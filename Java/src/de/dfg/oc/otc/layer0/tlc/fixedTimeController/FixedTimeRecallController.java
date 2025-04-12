package de.dfg.oc.otc.layer0.tlc.fixedTimeController;

import de.dfg.oc.otc.layer0.tlc.*;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;

import java.util.List;

/**
 * This controller has the ability to extend phases until another phase requests
 * a phase change. Useful if some signal groups of a section have no waiting
 * traffic.
 *
 * @author rochner
 */
@SuppressWarnings("serial")
public class FixedTimeRecallController extends AbstractTLC {

    /**
     * Erzeugt einen Festzeitcontroller mit Recall-M�glichkeit f�r alle
     * angegebenen Phasen.
     *
     * @param maxGreenTimes Freigabezeiten der Phasen (Reihenfolge wie von
     *                      AimsunJunction.getPhaseIds() zur�ckgegeben
     * @param junction      Id der Junction, f�r die der TLC erzeugt wird.
     * @param phaseIds      Ids der Phasen, die von diesem TLC ber�cksichtigt werden
     *                      sollen.
     * @param recalls       Array mit Recall-Objekten, die f�r jede Phase angeben, ob no
     *                      recall, min recall oder max recall. Derzeit hat nur no recall
     *                      Auswirkungen.
     * @throws TLCException wenn die Junction nicht existiert, zu wenig Phasen hat, die
     *                         Anzahl der Phasen oder Recalls nicht mit der Anzahl der
     *                         �bergebenen Phasendauern zusammenpasst oder eine �bergebene
     *                         Phasen-Id nicht existiert. Weiterhin wenn eine Phase, f�r die
     *                         Recall.no gesetzt ist, keinen Recall-Detector hat.
     */
    public FixedTimeRecallController(final float[] maxGreenTimes, final AimsunJunction junction,
                                     final List<Integer> phaseIds, final Recall[] recalls) throws TLCException {
        super(maxGreenTimes, junction, phaseIds);
        setRecalls(recalls);
    }

    /**
     * Erzeugt einen Festzeitcontroller mit Recall-M�glichkeit f�r alle in der
     * angegebenen Junction vorhandenen Phasen.
     *
     * @param maxGreenTimes Freigabezeiten der Phasen (Reihenfolge wie von
     *                      AimsunJunction.getPhaseIds() zur�ckgegeben
     * @param junction      Id der Junction, f�r die der TLC erzeugt wird.
     * @param recalls       Array mit Recall-Objekten, die f�r jede Phase angeben, ob no
     *                      recall, min recall oder max recall. Derzeit hat nur no recall
     *                      Auswirkungen.
     * @throws TLCException wenn die Junction nicht existiert, zu wenig Phasen hat, die
     *                         Anzahl der Phasen oder Recalls nicht mit der Anzahl der
     *                         �bergebenen Phasendauern zusammenpasst oder eine �bergebene
     *                         Phasen-Id nicht existiert. Weiterhin wenn eine Phase, f�r die
     *                         Recall.no gesetzt ist, keinen Recall-Detector hat.
     */
    public FixedTimeRecallController(final float[] maxGreenTimes, final AimsunJunction junction, final Recall[] recalls)
            throws TLCException {
        super(maxGreenTimes, junction);
        setRecalls(recalls);
    }

    @Override
    public final TrafficLightControllerParameters getParameters() {
        /*
         * integers[0]: junctionId integers[1...numPhases]: phaseIds
		 * integers[numPhases+1...numPhases*2]: recalls floats[0...numPhases-1]:
		 * maxOuts
		 */
        final int[] ints = new int[numPhases * 2 + 1];
        final float[] floats = new float[numPhases];

        ints[0] = junction.getId();
        for (int i = 0; i < numPhases; i++) {
            floats[i] = phases[i].getMaxGreenTime();
            ints[i + 1] = phaseIds.get(i);
            ints[numPhases + 1 + i] = phases[i].getRecall().ordinal();
        }

        return new TrafficLightControllerParameters(
                TLCTypes.FIXEDTIMERECALL, ints, floats, new String[0], new boolean[0]);
    }

    private void setRecalls(final Recall[] recalls) {
        if (recalls.length != numPhases) {
            throw new TLCException("Number of Recalls doesn't match with number of phase durations");
        }

        for (int i = 0; i < numPhases; i++) {
            if (recalls[i] == Recall.no && phases[i].getNumRecallDetectors() < 1) {
                throw new TLCException("Phase " + phaseIds.get(i) + " has no Recall-Detector.");
            }
            phases[i].setRecall(recalls[i]);
        }
    }

    @Override
    public final void step(final float time) throws TLCException {
        if (time < getTimeOfLastChange()) {
            throw new TLCException("Time is moving backwards!");
        }

        final float currentPhaseLasts = time - getTimeOfLastChange();
        if (currentPhaseLasts >= phases[currentPhase].getMaxGreenTime()) {
            final int startPhase = currentPhase;
            final float timeCorrection = currentPhaseLasts - phases[currentPhase].getMaxGreenTime();

            do {
                currentPhase++;
                if (currentPhase >= numPhases) {
                    currentPhase = 0;
                }

                if (phases[currentPhase].getRecall() == Recall.no
                        && phases[currentPhase].getNumActiveRecallDetectors() > 0) {
                    setTimeOfLastPhaseChange(time - timeCorrection);
                    break;
                } else if (phases[currentPhase].getRecall() == Recall.max) {
                    setTimeOfLastPhaseChange(time - timeCorrection);
                    break;
                } else if (phases[currentPhase].getRecall() == Recall.min) {
                    setTimeOfLastPhaseChange(time - timeCorrection);
                    break;
                } else if (phases[currentPhase].getRecall() == Recall.disable) {
                    setTimeOfLastPhaseChange(time - timeCorrection);
                    break;
                }
            } while (startPhase != currentPhase);
        }
    }
}
