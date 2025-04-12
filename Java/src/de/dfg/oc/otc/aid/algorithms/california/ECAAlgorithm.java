package de.dfg.oc.otc.aid.algorithms.california;

import de.dfg.oc.otc.aid.AIDException;
import de.dfg.oc.otc.aid.AIDMonitoringZone;
import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.aid.Incident.IncidentType;
import de.dfg.oc.otc.aid.algorithms.AbstractAIDAlgorithm;
import de.dfg.oc.otc.layer1.observer.monitoring.DataEntry;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.*;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;
import de.dfg.oc.otc.manager.aimsun.detectors.DetectorForkGroup;
import de.dfg.oc.otc.manager.aimsun.detectors.DetectorPair;
import de.dfg.oc.otc.aid.disturbance.DisturbanceManager;
import de.dfg.oc.otc.tools.LimitedQueue;

import java.util.*;

/**
 * Implementation of the Extended California Algorithm ECA which extends the
 * standard California algorithm 7.
 * <p>
 * TODO: low traffic check is missing
 *
 * @author klejnowski
 */
public class ECAAlgorithm extends AbstractAIDAlgorithm {
    /**
     * Contains lists of the past occupancy values of all observed divided
     * detector pairs.
     */
    private final LimitedQueue<List<Map<String, Float>>> occupancyHistory;
    /**
     * Contains the current algorithm states for each detector pair combination. <detector pair, State>
     */
    private final Map<String, Integer> algorithmStates;
    /**
     * Contains the number of iterations how long an incident has already been
     * continuing. <detector pair, counter></detector>
     */
    private final Map<String, Integer> persistenceCounter;
    /**
     * List of pending incidents which have not been reported yet for each
     * detector pair identifier. <detector pair, Incident object>
     */
    private final Map<String, Incident> pendingIncidents;
    /**
     * ECAAlgorithms which receive traffic from the junction of this algorithm.
     */
    private final List<ECAAlgorithm> receivers;
    /**
     * List of other ECAAlgorithm senders from the same junction.
     */
    private final List<ECAAlgorithm> otherJunctionSenders;
    /**
     * Contains the history of count values.
     */
    private final LimitedQueue<Float> countHistory;
    /**
     * Number of iterations how long an incident needs to have the state
     * 'incident continuing' before it is reported.
     */
    private final int minIterationsBeforeReport = 2;
    /**
     * Tolerance for the start time of an incident.
     */
    private final float timeTolerance = 300;
    /**
     * Contains the values of divided detector pairs.
     */
    private final Map<DetectorForkGroup, float[]> dividedPairValues;
    /**
     * List of the ids of the divided detector pairs.
     */
    private final List<String> dividedPairIds;
    /**
     * List of the monitored divided detector pairs.
     */
    private final List<Detector> dividedDetectors;
    /**
     * Contains the list of current occupancy values of all observed divided
     * detector pairs. Key = Pair ID, Value = List of occupancy values for one
     * detector pair measured in the current iteration.
     */
    private List<Map<String, Float>> currentOccupancyValues;
    /**
     * Contains the number of monitored divided detectors.
     */
    private int dividedDetectorCounter;
    /**
     * The algorithm is a sender or not. Monitoring a junction-incoming section.
     */
    private boolean sender;
    /**
     * The algorithm is a receiver or not. Linked to a sending junction via a turning.
     */
    private boolean receiver;
    /**
     * List of ECAAlgorithm senders from neighbour junctions.
     */
    private List<ECAAlgorithm> neighbourSenders;
    /**
     * Detector used for junction monitoring.
     */
    private Detector junctionDetector;
    /**
     * Sums up the values from a count detector over a certain time span.
     */
    private float currentCountSum;
    /**
     * Threshold for the difference between the occupancies of two succeeding
     * detectors (OCCDF).
     */
    private float TH_ID1 = 8;
    /**
     * Threshold for the relative difference between the occupancies of two
     * succeeding detectors (OCCRDF).
     */
    private float TH_ID2 = 0.5f;
    /**
     * Threshold for the relative temporal difference in downstream occupancy
     * (DOCCTD).
     */
    private float TH_ID3 = 0.3f;
    /**
     * Threshold for the first junction incident test.
     */
    private float TH_J1 = 8;
    /**
     * Threshold for the second junction incident test.
     */
    private float TH_J2 = 0.6f;
    /**
     * Threshold for the third junction incident test.
     */
    private float TH_J3 = 6;
    /**
     * Defines whether junction monitoring is enabled or not.
     */
    private boolean junctionMonitoringEnabled = true;

    public ECAAlgorithm() {
        this.currentOccupancyValues = new ArrayList<>();
        this.occupancyHistory = new LimitedQueue<>(3);
        this.countHistory = new LimitedQueue<>(3);
        this.algorithmStates = new HashMap<>();
        this.persistenceCounter = new HashMap<>();
        this.pendingIncidents = new HashMap<>();

        this.dividedPairIds = new ArrayList<>(5);
        this.dividedPairValues = new HashMap<>();
        this.dividedDetectors = new ArrayList<>(5);

        this.receivers = new ArrayList<>(3);
        this.otherJunctionSenders = new ArrayList<>(3);
    }

    @Override
    public String getName() {
        return "Extended California Algorithm";
    }

    /**
     * Used as entrance point for independent calculations. Thereby the actual
     * calculation is triggered.
     */
    @Override
    public void update(Observable obs, Object value) {
        // only runs once at the start of the first iteration
        if (this.neighbourSenders == null) {
            findNeighbourSenders();
        }

        if (obs instanceof Detector) {
            Detector detector = (Detector) obs;
            if (detector.equals(this.junctionDetector) && this.junctionMonitoringEnabled) {
                runJunctionMonitoring((DetectorDataValue) value);
            }

            if (this.dividedDetectors.contains(detector)) {
                runDividedPairMonitoring(detector, (DetectorDataValue) value);
            }
        } else {
            super.update(obs, value);
        }
    }

    /**
     * Find sending algorithms from incoming junctions.
     */
    private void findNeighbourSenders() {
        this.neighbourSenders = new ArrayList<>();

        if (this.sender) {
            Link link = this.monitoringZone.getMonitoredLink();

            if (link.getType() == Link.Type.JUNCTION_TO_JUNCTION) {
                OTCNode neighbour = link.getEndJunction().getNode();

                for (AbstractAIDAlgorithm algorithm : neighbour.getAIDComponent().getAlgorithms()) {
                    if (this.getName().equals(algorithm.getName())) {
                        AIDMonitoringZone monitoringZone = algorithm.getMonitoringZone();

                        if (monitoringZone.isIncoming()) {
                            AimsunJunction startJunction = monitoringZone.getMonitoredLink().getStartJunction();

                            if (startJunction == null || startJunction.getId() != this.node.getJunction().getId()) {
                                this.neighbourSenders.add((ECAAlgorithm) algorithm);
                            }
                        }
                    }
                }
            }

            // Find local senders
            for (AbstractAIDAlgorithm algorithm : this.node.getAIDComponent().getAlgorithms()) {
                if (this.getName().equals(algorithm.getName())) {
                    AIDMonitoringZone monitoringZone = algorithm.getMonitoringZone();

                    if (monitoringZone.isIncoming() && !algorithm.equals(this)) {
                        this.otherJunctionSenders.add((ECAAlgorithm) algorithm);
                    }
                }
            }
        }
    }

    /**
     * Processes the detector count values for junction incidents.
     *
     * @param value Detector count value
     */
    private void runJunctionMonitoring(DataEntry value) {
        this.currentCountSum += value.getValues()[DetectorCapabilities.COUNT];

        float cycleTime = 90;
        if (value.getTime() % cycleTime < OTCManager.getSimulationStepSize()) {
            countHistory.add(this.currentCountSum);

            // Check if a junction incident exists
            if (runJunctionIncident()) {
                if (this.sender) {
                    // Time frame in which occurred incidents are checked.
                    float lookbackTime = 3 * cycleTime;
                    float timeOfOccurrence = value.getTime() - lookbackTime;

                    // Only report incident if it has not been reported yet
                    if (!incidentReported(timeOfOccurrence)) {
                        // Report junction incident
                        reportIncident(createJunctionIncident(timeOfOccurrence));
                    }
                } else if (this.receiver) {
                    Incident junctionIncident = new Incident();
                    junctionIncident.setStartTime(value.getTime() - 270);
                    junctionIncident.setType(IncidentType.JUNCTION_INCIDENT);
                    //TODO: throw incident?
                }
            }

            this.currentCountSum = 0;
        }
    }

    /**
     * Processes the detector values for divided detector pairs.
     *
     * @param detector Divided detector
     * @param value    Detector value
     */
    private void runDividedPairMonitoring(Detector detector, DataEntry value) {
        if (value.getTime() >= this.warmupTime) {
            for (Map.Entry<DetectorForkGroup, float[]> detectorForkGroupEntry : this.dividedPairValues.entrySet()) {
                if (detectorForkGroupEntry.getKey().isDetectorContained(detector)) {
                    float[] currentValues = this.dividedPairValues.get(detectorForkGroupEntry.getKey());
                    if (currentValues == null) {
                        currentValues = new float[]{0, 0};
                    }
                    if (detectorForkGroupEntry.getKey().isDetectorUpstream(detector)) {
                        currentValues[0] += value.getValues()[DetectorCapabilities.OCCUPANCY];
                    } else {
                        currentValues[1] += value.getValues()[DetectorCapabilities.OCCUPANCY];
                    }

                    this.dividedPairValues.put(detectorForkGroupEntry.getKey(), currentValues);
                }
            }
            this.dividedDetectorCounter++;
        }
    }

    /**
     * Checks if the incident has already been reported.
     *
     * @param timeOfOccurrence Time of incident occurrence
     * @return {@code true} if incident has already been reported, {@code false}
     * otherwise
     */
    private boolean incidentReported(float timeOfOccurrence) {
        // Check for own incident
        if (linkIncidentPresent(timeOfOccurrence)) {
            return true;
        }

        // Check if receivers have incidents at this junction
        for (ECAAlgorithm receiver : this.receivers) {
            if (receiver.linkIncidentPresent(timeOfOccurrence)) {
                return true;
            }
        }

        // Check if other senders of this junction have incidents
        for (ECAAlgorithm junctionSender : this.otherJunctionSenders) {
            if (junctionSender.junctionIncidentPresent(timeOfOccurrence)) {
                return true;
            }
        }

		/*
         * Check if other senders from neighbour nodes have reported junction or
		 * link incidents.
		 */
        for (ECAAlgorithm neighbourSender : this.neighbourSenders) {
            boolean linkIncident = neighbourSender.linkIncidentPresent(timeOfOccurrence);
            boolean junctionIncident = neighbourSender.junctionIncidentPresent(timeOfOccurrence);

            if (linkIncident || junctionIncident) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a section incident has happened around the given time.
     *
     * @param time of incident occurrence
     * @return {@code true} if an incident is present at this link,
     * {@code false} otherwise
     */
    private boolean linkIncidentPresent(float time) {
        for (Incident incident : getAIDEvaluator().getIncidentAlarms()) {
            if (incident.getType() == IncidentType.SECTION_INCIDENT && incident.getStartTime() >= time - this.timeTolerance) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a junction incident has happened around the given time.
     *
     * @param time of incident occurrence
     * @return {@code true} if a junction incident is present, {@code false}
     * otherwise
     */
    private boolean junctionIncidentPresent(float time) {
        for (Incident incident : getAIDEvaluator().getIncidentAlarms()) {
            if (incident.getType() == IncidentType.JUNCTION_INCIDENT && incident.getStartTime() >= time - this.timeTolerance) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if an incident has happened at the junction.
     * Test works on vehicle counts only.
     *
     * @return {@code true} if an incident is assumed at the junction,
     * {@code false} otherwise
     */
    private boolean runJunctionIncident() {
        if (countHistory.isFull()) {
            // Current count sum
            float t_sum = countHistory.get(2);
            // Sum of previous iteration
            float tminus1_sum = countHistory.get(1);
            // Sum of previous previous iteration
            float tminus2_sum = countHistory.get(0);

            // TEST 1: count decrease in case of an incident
            if (tminus2_sum - tminus1_sum >= TH_J1) {
                // TEST 2: count difference comparison on percentage basis to avoid false alarms due to low traffic density
                if ((tminus2_sum - tminus1_sum) / tminus2_sum >= TH_J2) {
                    // TEST 3: estimates progress of the count value; count stays low -> incident assumed
                    if (t_sum - tminus1_sum <= TH_J3) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Prepares the algorithm execution. Thereby the algorithm is executed for
     * each detector pair combination.
     *
     * @param time of the algorithm execution
     */
    private void initStandardAlgorithmExecution(float time) {
        // Only execute the algorithm if past data is available
        if (pastOccupancyValues.isFull()) {
            String previousDetectorPairID = null;

            // Loop through each detector pair and execute the algorithm
            for (AbstractDetectorGroup detectorPair : monitoringZone.getMonitoredDetectorPairs()) {
                String detectorPairID = detectorPair.getId();

                if (pastOccupancyValues.getLast().keySet().contains(detectorPairID)) {
                    // Skip first detector since it has no predecessor
                    if (previousDetectorPairID != null) {
                        // detectorPairID = upstream
                        // previousDetectorPairID = downstream
                        String pairIdentifier = getPairIdentifier(detectorPairID, previousDetectorPairID);

                        // Set state to incident free if it does not exist yet
                        algorithmStates.putIfAbsent(pairIdentifier, ECAState.INCIDENT_FREE.ordinal());

                        // Set the persistence counter to 0
                        persistenceCounter.putIfAbsent(pairIdentifier, 0);

                        // Actual execution of the algorithm
                        runStandardAlgorithm(time, pairIdentifier, detectorPairID, previousDetectorPairID);
                    }

                    previousDetectorPairID = detectorPairID;
                }
            }
        }
    }

    /**
     * Actual execution of the standard california algorithm 7.
     *
     * @param time                 of the algorithm execution
     * @param pairIdentifier       String combining the ids of two detector pairs
     * @param upstreamDetectorID   Id of the upstream detector pair
     * @param downstreamDetectorID Id of the downstream detector pair
     */
    private void runStandardAlgorithm(float time, String pairIdentifier, String upstreamDetectorID, String downstreamDetectorID) {
        algorithmApplied();

        // First element in the queue is the oldest, last is the newest
        float OCC_i_t = pastOccupancyValues.getLast().get(upstreamDetectorID);
        float OCC_iplus1_t = pastOccupancyValues.getLast().get(downstreamDetectorID);
        float OCC_iplus1_tminus2 = pastOccupancyValues.getFirst().get(downstreamDetectorID);

        // Spatial difference in occupancy
        float OCCDF = OCC_i_t - OCC_iplus1_t;

        // Relative spatial difference in occupancies
        float OCCRDF = 0;
        if (OCC_i_t > 0) {
            OCCRDF = OCCDF / OCC_i_t;
        }

        // Relative temporal difference in downstream occupancy
        float DOCCTD = 0;
        if (OCC_iplus1_tminus2 > 0) {
            DOCCTD = (OCC_iplus1_tminus2 - OCC_iplus1_t) / OCC_iplus1_tminus2;
        }

        int currentState = algorithmStates.get(pairIdentifier);

        log(time + ";" + pairIdentifier + ";" + OCCDF + ";" + OCCRDF + ";" + DOCCTD + ";"
                + persistenceCounter.get(pairIdentifier) + ";" + ECAState.values()[currentState]);

		// Standard California 7 Decision Tree
        if (currentState >= ECAState.INCIDENT_OCCURED.ordinal()) {
             if (OCCRDF >= this.TH_ID2) {
                incidentContinuing(pairIdentifier);
            } else {
                incidentFree(time, pairIdentifier);
            }
        } else {
            if (OCCDF >= this.TH_ID1 && OCCRDF >= this.TH_ID2 && DOCCTD >= this.TH_ID3) {
                incidentOccured(time, pairIdentifier);
            } else {
                incidentFree(time, pairIdentifier);
            }
        }
    }

    /**
     * Create a tentative incident for a given detector pair combination.
     *
     * @param time           of the algorithm execution
     * @param pairIdentifier String combining the ids of two detector pairs
     */
    private void incidentOccured(float time, String pairIdentifier) {
        Incident incident = createIncident(time, pairIdentifier, false);
        tentativeIncidents.put(pairIdentifier, incident);
        pendingIncidents.put(pairIdentifier, incident);

        algorithmStates.put(pairIdentifier, ECAState.INCIDENT_OCCURED.ordinal());
        persistenceCounter.put(pairIdentifier, 1);
    }

    /**
     * Sets the algorithm state to continuing and reports a new incident for a
     * given detector pair combination if the persistence check is positive.
     *
     * @param pairIdentifier String combining the ids of two detector pairs
     */
    private void incidentContinuing(String pairIdentifier) {
        algorithmStates.put(pairIdentifier, ECAState.INCIDENT_CONTINUING.ordinal());
        persistenceCounter.put(pairIdentifier, persistenceCounter.get(pairIdentifier) + 1);

        // Throw incident alarm
        Incident incident = pendingIncidents.get(pairIdentifier);
        if (incident != null && persistenceCounter.get(pairIdentifier) >= minIterationsBeforeReport) {
            // a new disturbance has to be confirmed,
            // because tentative Incidents should not change the TLC,
            // if config-flag "allowTentativeIncidents" is 0
            incident.setConfirmed(true);
            DisturbanceManager.getInstance().confirmDisturbance(incident, getNode());
            reportIncident(incident);

            // Remove pending incident after reporting it
            pendingIncidents.remove(pairIdentifier);
        }
    }

    /**
     * Sets the algorithm state to free and removes tentative incidents for a
     * given detector pair combination.
     *
     * @param time           of the algorithm execution
     * @param pairIdentifier String combining the ids of two detector pairs
     */
    protected void incidentFree(float time, String pairIdentifier) {
        // Set algorithm state to free
        algorithmStates.put(pairIdentifier, ECAState.INCIDENT_FREE.ordinal());

        super.incidentFree(time, pairIdentifier);
        pendingIncidents.remove(pairIdentifier);

        // Reset persistence counter
        persistenceCounter.put(pairIdentifier, 0);
    }

    /**
     * Prepares the algorithm execution for the enhanced version.
     *
     * @param time of the algorithm execution
     */
    protected void prepareAndRunAlgorithm(float time) {
        /*
         * Normalerweise werden einfach die Listen mit Durchschnittswerten
		 * aller Detektoren der letzten 3 Iterationen verwendet. Diese werden
		 * einem Durchlauf beim Entscheidungsbaum unterzogen. Die Erweiterung
		 * des ECAs besteht darin, dass es fuer DividedPairs
		 * unterscheidliche Werte gibt, je nachdem ob sie als Up- oder
		 * Downstreamstation betrachtet werden. Die Listen werden in
		 * Laenge geteilt und einzeln durchlaufen, wobei die DividedPairs bei
		 * jedem Durchlauf den entsprechenden Wert annehmen.
		 * 
		 * Bei den Alternativwerten werden hier die Durchschnittswerte gebildet
		 * und die neuen Listen generiert.
		 */
        /*
      Defines whether monitoring of divided detector pairs is enabled or not.
     */
        if (this.dividedDetectors.isEmpty()) {
            initStandardAlgorithmExecution(time);
        } else {
            // New algorithm iteration
            calculateDividedPairValueAverages();

            occupancyHistory.add(currentOccupancyValues);
            currentOccupancyValues = new ArrayList<>();

            Map<String, Float> currentDetectorValues = new HashMap<>();

            boolean first = true;
            boolean last = false;

            Iterator<String> occupancyIterator = this.pastOccupancyValues.getLast().keySet().iterator();

            while (occupancyIterator.hasNext()) {
                String pairID = occupancyIterator.next();

                if (!occupancyIterator.hasNext()) {
                    last = true;
                }

                if (!this.dividedPairIds.contains(pairID)) {
                    float averageOccupancyValue = this.pastOccupancyValues.getLast().get(pairID);
                    currentDetectorValues.put(pairID, averageOccupancyValue);
                } else {
                    /*
                     * End old list with downstream values and begin new list
					 * with upstream values
					 */
                    if (first) {
                        currentDetectorValues.put(pairID, this.dividedPairValues.get(getPairForId(pairID))[0]);
                        currentDetectorValues = new HashMap<>();
                    } else if (last) {
                        currentDetectorValues.put(pairID, this.dividedPairValues.get(getPairForId(pairID))[1]);
                        currentDetectorValues = new HashMap<>();
                    } else {
                        currentDetectorValues.put(pairID, this.dividedPairValues.get(getPairForId(pairID))[1]);
                        this.currentOccupancyValues.add(currentDetectorValues);
                        currentDetectorValues = new HashMap<>();
                        currentDetectorValues.put(pairID, this.dividedPairValues.get(getPairForId(pairID))[0]);
                    }
                }

                if (last) {
                    this.currentOccupancyValues.add(currentDetectorValues);
                }

                if (first) {
                    first = false;
                }
            }

            // Execute standard algorithm decision tree for each list
            if (this.occupancyHistory.isFull()) {
                for (int i = 0; i < this.currentOccupancyValues.size(); i++) {
                    this.pastOccupancyValues.clear();

                    this.pastOccupancyValues.add(this.occupancyHistory.getFirst().get(i));
                    this.pastOccupancyValues.add(this.occupancyHistory.getLast().get(i));
                    this.pastOccupancyValues.add(this.currentOccupancyValues.get(i));

                    initStandardAlgorithmExecution(time);
                }
            }

            // Prepare next iteration
            this.dividedDetectorCounter = 0;
            for (DetectorForkGroup pair : this.dividedPairValues.keySet()) {
                this.dividedPairValues.put(pair, null);
            }
        }
    }

    /**
     * Calculates the average occupancy values for divided detector pairs.
     */
    private void calculateDividedPairValueAverages() {
        int factor = this.dividedDetectorCounter / this.dividedDetectors.size();

        for (Map.Entry<DetectorForkGroup, float[]> detectorForkGroupEntry : this.dividedPairValues.entrySet()) {
            float[] average = new float[2];

            float average1 = this.dividedPairValues.get(detectorForkGroupEntry.getKey())[0];
            float average2 = this.dividedPairValues.get(detectorForkGroupEntry.getKey())[1];

            average[0] = average1 / factor;
            average[1] = average2 / factor;
            ;

            this.dividedPairValues.put(detectorForkGroupEntry.getKey(), average);
        }
    }

    /**
     * Returns the divided detector pair for the given id.
     *
     * @param id Divided detector pair id
     * @return Divided detector pair object
     */
    private DetectorForkGroup getPairForId(String id) throws AIDException {
        for (AbstractDetectorGroup pair : this.monitoringZone.getMonitoredDetectorPairs()) {
            if (pair.getId().equals(id)) {
                return (DetectorForkGroup) pair;
            }
        }
        throw new AIDException("Couldn't find DetectorForkGroup with id " + id);
    }

    @Override
    public void finalizeInitialization() {
        initJunctionMonitoring();
        initDividedPairMonitoring();
    }

    /**
     * This method is used for initializing the infrastructure for junction monitoring.
     * If another AID algorithm is used in a required monitoring zone,
     * or if there is no outgoing monitoring zone for a turning, than junction
     * monitoring will be disabled.
     * The idea behind this method is that all senders find their receivers.
     */
    private void initJunctionMonitoring() {
        if (this.incomingToJunction) {
            AbstractDetectorGroup nearestPair = this.monitoringZone.getNearestPairToJunction();
            Detector countDetector;
            boolean junctionMonitoringPossible = true;

            // Algorithm acts as sender if junction monitoring is possible.
            this.sender = true;

            // Monitoring of the nearest detector
            if (nearestPair.getType() == AbstractDetectorGroup.Type.SIMPLE_DETECTOR_PAIR) {
                countDetector = ((DetectorPair) nearestPair).getDownstreamDetector();
            } else {
                countDetector = ((DetectorForkGroup) nearestPair).getDownstreamDetectors().get(0);
            }

            // Find target detector of this detector
            List<Turning> turnings = this.node.getJunction().getTurningsForIncomingSectionID(countDetector.getSectionId());
            for (Turning turning : turnings) {
                AIDMonitoringZone destinationMonitoringZone = null;
                Detector nearestDetector = null;
                Detector targetDetector = null;

                // Create local cooperation infrastructure
                List<Detector> detectors;
                if (turning.getOutSection().getRoadType() == RoadType.INTERNAL) {
                    detectors = turning.getOutSection().getDestinationSection().getDetectors();
                } else {
                    detectors = turning.getOutSection().getDetectors();
                }

                for (Detector detector : detectors) {
                    // Find out which algorithms are observing these detectors.
                    Iterator<AIDMonitoringZone> monitoringZones = this.node.getAIDComponent().getMonitoringZones()
                            .iterator();

                    while (monitoringZones.hasNext()) {
                        AIDMonitoringZone monitoringZone = monitoringZones.next();

                        if (monitoringZone.isIncoming()) {
                            if (monitoringZones.hasNext()) {
                                monitoringZone = monitoringZones.next();
                            } else {
                                break;
                            }
                        }

                        AbstractDetectorGroup detectorPair = monitoringZone.getNearestPairToJunction();

                        if (detectorPair.getType() == AbstractDetectorGroup.Type.SIMPLE_DETECTOR_PAIR) {
                            nearestDetector = ((DetectorPair) detectorPair).getUpstreamDetector();
                        } else {
                            for (Detector dividedDetector : ((DetectorForkGroup) detectorPair).getUpstreamDetectors()) {
                                if (dividedDetector.getSectionId() == detector.getSectionId()) {
                                    nearestDetector = dividedDetector;
                                    break;
                                }
                            }
                        }

                        if (nearestDetector != null && nearestDetector.getId() == detector.getId()) {
                            targetDetector = detector;
                            destinationMonitoringZone = monitoringZone;
                        }
                    }
                }

                // Target detector has been found
                if (destinationMonitoringZone != null) {
                    AbstractAIDAlgorithm algorithm = destinationMonitoringZone.getAIDAlgorithm();

                    if (algorithm instanceof ECAAlgorithm) {
                        // Initialize the received algorithm
                        ECAAlgorithm eca = (ECAAlgorithm) algorithm;
                        if (this.junctionDetector == null) {
                            this.junctionDetector = targetDetector;
                            this.junctionDetector.addObserver(this);
                        }
                        eca.receiver = true;
                        addReceiver(eca);
                    } else {
                        junctionMonitoringPossible = false;
                    }
                } else {
                    junctionMonitoringPossible = false;
                }
            }

			/*
             * Add the algorithm as observer to the detector if junction
			 * monitoring is possible.
			 */
            if (junctionMonitoringPossible) {
                this.junctionDetector = countDetector;
                this.junctionDetector.addObserver(this);
            } else {
                this.sender = false;
                this.junctionMonitoringEnabled = false;
            }
        }
    }

    /**
     * This method prepares the algorithm for separate handling of divided
     * detector pairs.
     */
    private void initDividedPairMonitoring() {
        for (AbstractDetectorGroup detectorGroup : this.monitoringZone.getMonitoredDetectorPairs()) {
            // Type: Divided detector pair
            if (detectorGroup.getType() != AbstractDetectorGroup.Type.SIMPLE_DETECTOR_PAIR) {
                DetectorForkGroup group = (DetectorForkGroup) detectorGroup;

                List<Detector> upstreamDetectorPair = group.getUpstreamDetectors();
                List<Detector> downstreamDetectorPair = group.getDownstreamDetectors();

                if (!upstreamDetectorPair.isEmpty() && !downstreamDetectorPair.isEmpty()) {
                    Detector upstream = null;
                    Detector downstream = null;

                    Link link = this.monitoringZone.getMonitoredLink();

                    for (Detector detector : upstreamDetectorPair) {
                        float distance = link.getDistancesToStartEnd(detector)[0];
                        if (distance > -1) {
                            upstream = detector;
                        }
                    }

                    for (Detector detector : downstreamDetectorPair) {
                        float distance = link.getDistancesToStartEnd(detector)[0];
                        if (distance > -1) {
                            downstream = detector;
                        }
                    }

                    if (upstream != null && downstream != null) {
                        this.dividedPairValues.put(group, new float[2]);
                        this.dividedPairIds.add(group.getId());

                        if (this.monitoringZone.getMonitoredDetectorPairs().get(0).equals(group)) {
                            this.dividedDetectors.add(upstream);
                            upstream.addObserver(this);
                        } else if (this.monitoringZone.getMonitoredDetectorPairs().get(monitoringZone.getMonitoredDetectorPairs().size()).equals(group)) {
                            this.dividedDetectors.add(downstream);
                            downstream.addObserver(this);
                        } else {
                            this.dividedDetectors.add(upstream);
                            upstream.addObserver(this);

                            this.dividedDetectors.add(downstream);
                            downstream.addObserver(this);
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds an algorithm as receiver.
     *
     * @param algorithm which acts as receiver
     */
    private void addReceiver(ECAAlgorithm algorithm) {
        this.receivers.add(algorithm);
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("TH_ID1", this.TH_ID1);
        parameters.put("TH_ID2", this.TH_ID2);
        parameters.put("TH_ID3", this.TH_ID3);

        parameters.put("TH_J1", this.TH_J1);
        parameters.put("TH_J2", this.TH_J2);
        parameters.put("TH_J3", this.TH_J3);

        parameters.put("SCANNING_INTERVAL", this.executionInterval);
        parameters.put("WARMUP_TIME", this.warmupTime);
        parameters.put("JUNCTION_MONITORING", this.junctionMonitoringEnabled);
        parameters.put("MINIMAL_DURATION", this.minIterationsBeforeReport);

        return parameters;
    }

    @Override
    public int getRequiredDetectorPairCount() {
        return 2;
    }

    @Override
    public boolean isStateMappedToIncident(int state) {
        return state == ECAState.INCIDENT_OCCURED.ordinal() || state == ECAState.INCIDENT_CONTINUING.ordinal();
    }

    /**
     * States used by the enhanced california algorithm.
     */
    private enum ECAState {
        /**
         * The algorithm has found no incident.
         */
        INCIDENT_FREE,
        /**
         * The algorithm has found an incident.
         */
        INCIDENT_OCCURED,
        /**
         * The confirmed incident is still continuing.
         */
        INCIDENT_CONTINUING
    }
}
