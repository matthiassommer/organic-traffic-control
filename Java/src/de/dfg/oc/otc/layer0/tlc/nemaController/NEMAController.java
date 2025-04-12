package de.dfg.oc.otc.layer0.tlc.nemaController;

import de.dfg.oc.otc.layer0.tlc.*;
import de.dfg.oc.otc.layer1.observer.monitoring.SubDetectorValue;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.Phase;
import de.dfg.oc.otc.manager.aimsun.detectors.SubDetector;

import org.apache.log4j.Logger;

import java.util.List;

/**
 * Implementation is done with respect to information provided by NEMA Standards
 * Publication NO. TS 2 pages 127ff and 29ff.
 * <p/>
 * http://ops.fhwa.dot.gov/trafficanalysistools/tat_vol4/app_f.htm
 * 
 * @author tomforde
 */
public class NEMAController extends AbstractTLC {
	private static final Logger log = Logger.getLogger(NEMAController.class);
	/**
	 * Parameter defines default extend step size in seconds.
	 */
	private final float defaultExtensionStep = 1;
	/**
	 * Parameter defines the 'age' of detector information.
	 */
	private final float MAX_VALUE_AGE = 5;
	/**
	 * Flag indicating, whether a concurring green call exists or not (from
	 * other phase).
	 */
	private boolean changeRequest;
	/**
	 * Value depending on duration of first dynamic part (might differ from
	 * phase.getMaxOut).
	 */
	private float dynamicMaxOut;
	/**
	 * The phase which will be executed after leaving the current one.
	 */
	private int nextPhase;

	/**
	 * The next phase for which a synchronisation is necessary.
	 */
	private int nextSynchPhase;
	/**
	 * The system time at which the next synchronisation takes place.
	 */
	private float nextSynchronisationTime;
	/**
	 * Flag indicating if current phase is within the second dynamic part of the
	 * the phase.
	 */
	private boolean isPhaseInGap;
	/**
	 * Value is calculated depending on the queue - Duration of first dynamic
	 * part.
	 */
	private float queueBasedDuration;
	/**
	 * Flag indicating if the duration of the first dynamic part has been
	 * calculated.
	 */
	private boolean queueCalculated;
	/**
	 * Flag indicating action for no-traffic-situation: {@code true} -> all red;
	 * {@code false} -> last active phase remains green.
	 */
	private boolean restInRed;
	/**
	 * Flag indicating, whether a new synchronisation request exists or not.
	 */
	private boolean synchWaiting;
	/**
	 * The (first) system time at which concurring green call has been detected.
	 */
	private float timeFirstDemand = -1;
	private Recall recall;

	/**
	 * Constructor for a NEMA - standard controller.
	 * 
	 * @param maximumGreenTimes
	 *            Maximum green times of all phases
	 * @param junction
	 *            the junction, which is controlled by the NEMA Controller
	 * @param phaseIds
	 *            IDs of the phases, which should be included by the TLC
	 * @param recalls
	 *            Array of Recall objects, indicating which recall type is
	 *            currently observed for the particular phase
	 * @param maxInitialGreenTimes
	 *            Specifies the maximum duration of the queue-based dynamic part
	 *            of the phase
	 * @param maximumGaps
	 *            Maximum gap between two cars for the second dynamic part of
	 *            the phase. Value applied before reduction function starts
	 * @param minimumGreenTimes
	 *            The shortest green time of a phase. If a time setting control
	 *            is designated as Minimum Green, the green time shall be not
	 *            less than that setting.
	 * @param extensionSteps
	 *            Values defining the steps to increase the duration of the
	 *            queue-based part of the phase (multiplied with #waitingCars)
	 * @param redDelays
	 *            Time before reduction function starts
	 * @throws TLCException
	 *             Will be thrown, if: - junction doesn't exist - the number of
	 *             phases is not equal to: length of all arrays
	 */
	public NEMAController(final float[] maximumGreenTimes,
			final AimsunJunction junction, final List<Integer> phaseIds,
			final Recall[] recalls, final float[] maxInitialGreenTimes,
			final float[] maximumGaps, final float[] minimumGreenTimes,
			final float[] extensionSteps, final float[] redDelays)
			throws TLCException {
		super(maximumGreenTimes, junction, phaseIds);

		if (recalls.length != numPhases
				|| maxInitialGreenTimes.length != numPhases
				|| maximumGaps.length != numPhases
				|| minimumGreenTimes.length != numPhases
				|| redDelays.length != numPhases) {
			throw new TLCException(
					"Number of elements not equal to number of phases");
		}

		for (int i = 0; i < numPhases; i++) {
			if (recalls[i] == Recall.no
					&& phases[i].getNumRecallDetectors() < 1) {
				throw new TLCException("Phase " + this.phaseIds.get(i)
						+ " has no recall detector.");
			}
			phases[i].setRecall(recalls[i]);

			phases[i].setMaximumGap(maximumGaps[i]);

			if (maxInitialGreenTimes[i] < 1 || maxInitialGreenTimes[i] > 99) {
				throw new TLCException(
						"Maximum green time out of defined range." + maxInitialGreenTimes[i] + "  i: " + i);
			}
			phases[i].setMaximimInitialGreenTime(maxInitialGreenTimes[i]);

			if (minimumGreenTimes[i] < 1 || minimumGreenTimes[i] > 255) {
				throw new TLCException(
						"Minimum green time out of defined range." + minimumGreenTimes[i] + "  i: " + i);
			}
			phases[i].setMinimumGreenTime(minimumGreenTimes[i]);

			phases[i].setExtensionStep(extensionSteps[i]);

			if (redDelays[i] < 1 || redDelays[i] > 255) {
				throw new TLCException(
						"Minimum red delay out of defined range." + redDelays[i] + "  i: " + i);
			}
			phases[i].setReductionDelay(redDelays[i]);
		}
	}

	/**
	 * Constructor for a NEMA - standard controller.
	 * 
	 * @param maximumGreenTimes
	 *            Maximum green times of all phases
	 * @param junction
	 *            the junction, which is controlled by the NEMA Controller
	 * @param recalls
	 *            Array of Recall objects, indicating which recall type is
	 *            currently observed for the particular phase
	 * @param maxInitialGreenTimes
	 *            Maximum initial green times - specifying the maximum duration
	 *            of the queue-based dynamic part of the phase
	 * @param allowableGaps
	 *            Maximum gap between two cars for the second dynamic part of
	 *            the phase. Value applied before reduction function starts
	 * @param minimumGreenTimes
	 *            The shortest green time of a phase. If a time setting control
	 *            is designated as Minimum Green, the green time shall be not
	 *            less than that setting.
	 * @param extensionSteps
	 *            Values defining the steps to increase the duration of the
	 *            queue-based part of the phase (multiplied with #waitingCars)
	 * @param redDelays
	 *            Time before reduction function starts
	 * @throws TLCException
	 *             Will be thrown, if: - junction doesn't exist - the number of
	 *             phases is not equal to: length of all arrays
	 */
	public NEMAController(final float[] maximumGreenTimes,
			final AimsunJunction junction, final Recall[] recalls,
			final float[] maxInitialGreenTimes, final float[] allowableGaps,
			final float[] minimumGreenTimes, final float[] extensionSteps,
			final float[] redDelays) throws TLCException {
		super(maximumGreenTimes, junction);

		if (recalls.length != numPhases
				|| maxInitialGreenTimes.length != numPhases
				|| allowableGaps.length != numPhases
				|| minimumGreenTimes.length != numPhases
				|| redDelays.length != numPhases) {
			throw new TLCException(
					"Anzahl der Elemente in �bergebenem Parameter stimmt nicht mit Anzahl der Phasendauern �berein!");
		}

		for (int i = 0; i < numPhases; i++) {
			if (recalls[i] == Recall.no
					&& phases[i].getNumRecallDetectors() < 1) {
				throw new TLCException("Phase " + phaseIds.get(i)
						+ " verf�gt �ber keinen Recall-Detector.");
			}
			phases[i].setRecall(recalls[i]);

			phases[i].setMaximumGap(allowableGaps[i]);

			if (maxInitialGreenTimes[i] < 1 || maxInitialGreenTimes[i] > 99) {
				throw new TLCException(
						"Maximum green time out of defined range." + maxInitialGreenTimes[i] + "  i: " + i);
			}
			phases[i].setMaximimInitialGreenTime(maxInitialGreenTimes[i]);

			if (minimumGreenTimes[i] < 1 || minimumGreenTimes[i] > 255) {
				throw new TLCException(
						"Minimum green time out of defined range." + minimumGreenTimes[i] + "  i: " + i);
			}
			phases[i].setMinimumGreenTime(minimumGreenTimes[i]);

			phases[i].setExtensionStep(extensionSteps[i]);

			if (redDelays[i] < 1 || redDelays[i] > 255) {
				throw new TLCException(
						"Minimum red delay out of defined range." + redDelays[i] + "  i: " + i);
			}
			phases[i].setReductionDelay(redDelays[i]);
		}
	}

	private float calculateCurrentGap(final float time) {
		// Gap reduction function nutzen
		final float beginReduction = getTimeOfLastChange() + dynamicMaxOut
				+ phases[currentPhase].getReductionDelay();

		// Check des Anfangszeitpunktes der GAP-Reduktionsfunktion
		if (timeFirstDemand < beginReduction) {
			return calculateCurrentGapSize(beginReduction, time);
		}
		return calculateCurrentGapSize(timeFirstDemand, time);
	}

	/**
	 * Method used to calculate the current allowed gap size depending on a
	 * decreasing function and the amount of time elapsed since the first
	 * green-demand detection of an other phase.
	 * 
	 * @param timeFirstDemand
	 *            The point in time where the first other phase announced demand
	 *            of green time
	 * @param time
	 *            The current system time
	 * @return The current allowed maximum gap size
	 */
	private float calculateCurrentGapSize(final float timeFirstDemand,
			final float time) {
		final float k = (time - timeFirstDemand)
				/ (dynamicMaxOut - timeFirstDemand);

		// Parameter defines the safe distance by calculating the GAP.
		final float MIN_SAFE_DISTANCE = 5;

		float gap = phases[currentPhase].getMaximumGap()
				- k * (phases[currentPhase].getMaximumGap() - MIN_SAFE_DISTANCE);

		if (Float.isInfinite(gap)) {
			gap = MIN_SAFE_DISTANCE;
		}

		// Normalise gap
		if (gap > phases[currentPhase].getMaximumGap()) {
			gap = phases[currentPhase].getMaximumGap();
		}

		if (gap < MIN_SAFE_DISTANCE) {
			gap = MIN_SAFE_DISTANCE;
		}

		return gap;
	}

	/**
	 * Method used to calculate the current GAP-size between the arriving cars.
	 * 
	 * @param time
	 *            The current system time
	 * @return The current GAP-size (lowest, if #detectors > 1)
	 */
	private float calculateCurrentHeadway(final float time) {
		float currentHeadway = -1;
		// Setzen von currentHeadway auf den kleinsten verf�gbaren Wert
		for (SubDetector detector : phases[currentPhase].getAimsunPhase()
				.getHeadwayDetectors()) {
			SubDetectorValue subdetectorValue = detector.getDetectorValue();

			if (subdetectorValue != null) {
				// �berpr�fen der zeitangabe.
				float diff = time - subdetectorValue.getTime();

				if (diff < MAX_VALUE_AGE) {
					float value = subdetectorValue.getValue();

					if (!Float.isNaN(value)) {
						if (currentHeadway < 0
								|| value < currentHeadway && value >= 0) {
							currentHeadway = value;
						}
					}
				}
			}
		}

		if (phases[currentPhase].getAimsunPhase().getHeadwayDetectors()
				.isEmpty()) {
			log.debug("No detectors for headway calculation found!");
		}

		return currentHeadway;
	}

	/**
	 * This method is used to calculate the time demand of the current phase for
	 * the first dynamic part. Therefore, the number of vehicles entering the
	 * waiting area during yellow and red time are needed. The value should be
	 * calculated once as it doesn't change during green time.
	 * <p/>
	 * The method sets two parameters: - queueBasedDuration: The calculated time
	 * demand - queueCalculated: Flag, if time demand has been calculated
	 * 
	 * @param time
	 *            the current system time
	 */
	private void calculateQueueBasedDuration(final float time) {
		// Hole detector Informationen
		float maxQueue = 0;
		float durationStep = phases[currentPhase].getExtensionStep();

		// Check consistency of parameter
		if (durationStep <= 0) {
			durationStep = defaultExtensionStep;
		}

		// TODO Layer1Observer.DataSource.STATISTICS
		// Layer1Observer.DataSource.DETECTOR
		for (SubDetector subDetector : phases[currentPhase].getAimsunPhase()
				.getCounterDetectors()) {
			SubDetectorValue detectorValue = subDetector.getDetectorValue();
			if (detectorValue != null) {
				// Check der zeitangabe.
				float detectorDiff = time - detectorValue.getTime();
				if (detectorDiff < MAX_VALUE_AGE) {
					float queue = detectorValue.getValue();
					if (!Float.isNaN(queue) && queue > maxQueue) {
						maxQueue = queue;
					}
				}

			}
		}

		final int numberOfSteps = Math.round(maxQueue);

		// Berechnen der Dauer
		queueBasedDuration = numberOfSteps * durationStep;

		// Normalisieren der Dauer (Beachten von Grenzwerten)
		if (queueBasedDuration < phases[currentPhase].getMinimumGreenTime()) {
			queueBasedDuration = phases[currentPhase].getMinimumGreenTime();
		} else if (queueBasedDuration > phases[currentPhase]
				.getMaximimGreenTime()) {
			queueBasedDuration = phases[currentPhase].getMaximimGreenTime();
		}

		queueCalculated = true;
	}

	private void calculcateFirstPart(final float time) {
		if (!queueCalculated) {
			calculateQueueBasedDuration(time);
			// Anpassen der Maximall�nge der Phase:
			// Ber�cksichtigung einer eventuellen Verk�rzung durch den
			// ersten dynamischen Teil
			dynamicMaxOut = phases[currentPhase].getMaxGreenTime()
					- (phases[currentPhase].getMaximimGreenTime() - queueBasedDuration);
		}
	}

	/**
	 * Method used to check whether another phase has detected waiting cars and
	 * therefore needs green time or not.
	 * 
	 * @return {@code True} (if other phase needs green), otherwise
	 *         {@code false}
	 */
	private boolean checkDemandOfPhases() {
		nextPhase = chooseNextNonInterPhase();

		return nextPhase != currentPhase;
	}

	private void checkIfPhaseIsInGap(final float phaseDuration) {
		// Erster Teil: Beachten der eigenen Warteschlange
		if (phaseDuration < phases[currentPhase].getMaximimGreenTime()
				&& !isPhaseInGap) {
			// Check ob Teil eins abgelaufen
			if (phaseDuration <= queueBasedDuration) {
				// Phase im initialen Bereich, keine Veränderung durchführen
			} else {
				// Check, ob Anforderungen anderer Phasen vorliegen
				isPhaseInGap = true;
			}
		} else if (phaseDuration >= phases[currentPhase].getMaximimGreenTime()
				&& !isPhaseInGap) {
			isPhaseInGap = true;
		}
	}

	/**
	 * Method used to select the next phase (which is not an interphase) by
	 * choosing the first one in the phase ordering currently having waiting
	 * cars.
	 * 
	 * @return The ID of the (non inter-)phase
	 */
	private int chooseNextNonInterPhase() {
		int phaseID = currentPhase;

		// Choose next phase, which is not an interphase and has no demand
		do {
			phaseID++;
			if (phaseID >= numPhases) {
				phaseID = 0;
			}

			final Phase currentPhase = phases[phaseID].getAimsunPhase();
			if (!currentPhase.isInterphase()) {
				// Aktuelle Phase ist keine Interphase
				for (SubDetector subDetector : currentPhase
						.getRecallDetectors()) {
					if (subDetector.getValue() > 0) {
						// Fertig, sobald das erste anfordernde Turning gefunden
						return phaseID;
					}
				}
			}
		} while (phaseID != currentPhase);

		// Keine Phase gefunden: Wähle nächste geplante
		phaseID++;

		// Normalisieren auf Wertebereich
		if (phaseID >= numPhases) {
			phaseID = 0;
		}

		// TODO war currentPhase, falsch?
		return phaseID;
	}

	private void executeAction(final float time) {
		// Aktualisieren der Informationen für die nächste Phase
		checkDemandOfPhases();

		// Setzen der nächsten Phase
		if (phases[currentPhase].getAimsunPhase().isInterphase()) {
			currentPhase = nextPhase;
		} else {
			// Interphasen beachten
			final int followingPhase = (currentPhase + 1) % numPhases;

			if (phases[followingPhase].getAimsunPhase().isInterphase()) {
				currentPhase = followingPhase;
			} else {
				currentPhase = nextPhase;
			}
		}

		// Rest der Status Informationen
		isPhaseInGap = false;
		queueCalculated = false;
		dynamicMaxOut = -1;
		timeFirstDemand = -1;
		changeRequest = false;

		setTimeOfLastPhaseChange(time);
	}

	/**
	 * Method used to estimate the duration of the current phase.
	 * 
	 * @return The forecast value
	 */
	public final float getDurationForecastCurrentPhase() {
		if (queueCalculated) {
			return queueBasedDuration
					+ phases[currentPhase].getMaxGreenTime() - phases[currentPhase]
							.getMaximimGreenTime();
		}
		return phases[currentPhase].getMaxGreenTime();
	}

	@Override
	public final TrafficLightControllerParameters getParameters() {
		/*
		 * integers[0]: junctionId integers[1...numPhases]: phaseIds
		 * integers[numPhases+1...numPhases*2]: recalls floats[0...numPhases-1]:
		 * maxOuts floats[numPhases...(2*numPhases-1)]: maxInitGreens
		 * floats[2*numPhases...(3*numPhases-1)]: maxGaps
		 * floats[3*numPhases...(4*numPhases-1)]: minGreens
		 * floats[4*numPhases...(5*numPhases-1)]: extSteps
		 * floats[5*numPhases...(6*numPhases-1)]: reductionDelays
		 */
		final int numFloat = numPhases * 6;
		final int numInt = numPhases * 2 + 1;

		final int[] ints = new int[numInt];
		final float[] floats = new float[numFloat];

		ints[0] = junction.getId();
		for (int i = 0; i < numPhases; i++) {
			floats[i] = phases[i].getMaxGreenTime();
			floats[numPhases + i] = phases[i].getMaximimGreenTime();
			floats[2 * numPhases + i] = phases[i].getMaximumGap();
			floats[3 * numPhases + i] = phases[i].getMinimumGreenTime();
			floats[4 * numPhases + i] = phases[i].getExtensionStep();
			floats[5 * numPhases + i] = phases[i].getReductionDelay();
			ints[i + 1] = phaseIds.get(i);
			ints[numPhases + 1 + i] = phases[i].getRecall().ordinal();
		}

		return new TrafficLightControllerParameters(TLCTypes.NEMA, ints,
				floats, new String[0], new boolean[0]);
	}

	private void printCurrentDecision(final float duration,
			final boolean execution) {
		final StringBuilder output = new StringBuilder();
		final String linesep = System.getProperty("line.separator");
		output.append("Current active phase: ").append(currentPhase);

		if (phases[currentPhase].getAimsunPhase().isInterphase()) {
			output.append(" is an interphase").append(linesep);
		} else {
			output.append(" is NOT an interphase").append(linesep);
		}

		output.append("Phase duration until now: ").append(duration)
				.append(linesep);
		output.append("Change will be executed: ");

		if (execution) {
			output.append("next phase: ").append(nextPhase).append(linesep);
		} else {
			output.append("NO").append(linesep);
		}

		output.append("Decision motivated by:").append(linesep);
		output.append("ChangeRequest calculation: ").append(changeRequest)
				.append(linesep);
		output.append("Dynamic Max Out calculation: ").append(dynamicMaxOut)
				.append(linesep);
		output.append("PhaseInGap calculation: ").append(isPhaseInGap)
				.append(linesep);
		output.append("FirstTimeDemand calculation: ").append(timeFirstDemand)
				.append(linesep);
		output.append("Maximal duration: ")
				.append(phases[currentPhase].getMaxGreenTime()).append(linesep);
		output.append("Demand of other phases found: ")
				.append(checkDemandOfPhases()).append(linesep);

		log.debug(output.toString());
	}

	public final void setNextSynchronisation(final int newSynchPhase,
			final float newSynchTime) {
		this.nextSynchPhase = newSynchPhase;
		this.nextSynchronisationTime = newSynchTime;
		this.synchWaiting = true;
	}

	@Override
	public final void step(final float time) throws TLCException {
		log.info("Calculating new step at time: " + time);
		// TODO Synchronisation mit einbeziehen
		// TODO RestInRed erlauben/ berücksichtigen

		// Speichern der aktuellen Dauer
		final float phaseDuration = time - getTimeOfLastChange();

		// Parameter ist true, wenn Wechsel durchgeführt werden muss
		boolean executeChange = false;
		// Parameter ist true, wenn Wechsel durchgeführt werden kann
		boolean allowChange = false;

		// Parameter ist disabled falls das Programm keinen Recall zul�sst
		// Parameter ist no falls kein Recall vorhanden ist
		recall = phases[currentPhase].getRecall();

		// setze Parameter auf true, wenn keine Recalls bestehen/ zugelassen
		// werden
		// und das programm in der Interphase ist und keine Anfrage(Call)
		// existiert
		if ((this.recall == Recall.no || this.recall == Recall.disable)
				&& phases[currentPhase].getAimsunPhase().isInterphase()
				&& !changeRequest) {
			restInRed = true;
		} else {
            restInRed = false;
        }

		if (phases[currentPhase].getAimsunPhase().isInterphase()) {
			// Nur Check, ob Zeit bereits abgelaufen.
			if (phaseDuration >= phases[currentPhase].getMaxGreenTime()) {
				executeChange = true;
				// Bestimme neue phase
				nextPhase = chooseNextNonInterPhase();
			}
			// else behalte Interphase bei
		} else {
			// NEMA Logik

			// Berechne einmalig pro Phasenaufruf die Dauer des ersten Teils
			calculcateFirstPart(time);

			// Check, ob Phase noch in der Sperrzeit
			if (phaseDuration >= phases[currentPhase].getMinimumGreenTime()) {
				checkIfPhaseIsInGap(phaseDuration);

				// Zweiter Teil: GAP. Erst berücksichtigen, wenn
				// queue-basierter
				// Teil abgelaufen ist
				// queueCalculated &&
				if (isPhaseInGap) {
					if (phaseDuration <= dynamicMaxOut) {
						// Check, ob Anforderungen anderer Phasen vorliegen
						// (nur nötig, wenn vorher noch kein bedarf bestand)
						if (!changeRequest) {
							changeRequest = checkDemandOfPhases();

							// Check, ob erstmals Bedarf besteht
							if (changeRequest) {
								timeFirstDemand = time;
							}
						}

						// capability HEADWAY nutzen
						float currentHeadway = calculateCurrentHeadway(time);

						// Check des errechneten Wertes (wenn NaN || ungültig
						// -> kein headway vorhanden)
						if (Float.isNaN(currentHeadway) || currentHeadway <= 0) {
							currentHeadway = phases[currentPhase]
									.getMaximumGap() + 1;
						}

						if (changeRequest) {
							final float currentGap = calculateCurrentGap(time);

							if (currentHeadway < 0
									|| currentHeadway > currentGap) {
								executeChange = true;
							}
						} else {
							// Maximal vorgegebene GAP Größe nutzen
							if (currentHeadway < 0
									|| currentHeadway > phases[currentPhase]
											.getMaximumGap()) {
								allowChange = true;
							}
						}
					} else {
						if (!changeRequest) {
							changeRequest = checkDemandOfPhases();

							if (changeRequest) {
								timeFirstDemand = time;
								executeChange = true;
							}
							allowChange = true;
						} else {
							// TODO: Hier execute Change nicht etwa false, da
							// kein changeRequest
							// besteht und somit kein change ausgef�hrt werden
							// sollte?
							executeChange = true;
						}
					}
				}
			}
			// else: noch in Sperrzeit -> nichts tun
		}

		printCurrentDecision(phaseDuration, executeChange);

		// Handlung nötig / möglich?
			if (executeChange) {
				executeAction(time);
				executeChange = false;
			}
			// INFO: Zur Zeit wird nur ein Wechsel durchgeführt, wenn eine
			// andere Phase auf Grün schalten möchte, andernfalls bleibt
			// die
			// aktuelle Phase grün
		else {
			if(allowChange && restInRed){
				phases[nextPhase].getAimsunPhase().isInterphase();
			}
		}
	}
}
