package de.dfg.oc.otc.layer0.tlc.fixedTimeController;

import de.dfg.oc.otc.layer0.tlc.AbstractTLC;
import de.dfg.oc.otc.layer0.tlc.TLCException;
import de.dfg.oc.otc.layer0.tlc.TLCTypes;
import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;

import java.util.List;

/**
 * A simple fixed time controller. Has set of phases and iterates over them.
 *
 * @author rochner
 */
@SuppressWarnings("serial")
public class FixedTimeController extends AbstractTLC {
    /**
     * Erzeugt einen Festzeitcontroller für alle in der angegebenen Junction
     * vorhandenen Phasen.
     *
     * @param maxOuts  Freigabezeiten der Phasen (Reihenfolge wie von
     *                 AimsunJunction.getPhaseIds() zurückgegeben.
     * @param junction Die Junction, für die der TLC erzeugt wird.
     */
    public FixedTimeController(final float[] maxOuts, final AimsunJunction junction) throws TLCException {
        super(maxOuts, junction);
    }

    /**
     * Erzeugt einen Festzeitcontroller für alle angegebenen Phasen.
     *
     * @param maxOuts  Freigabezeiten der Phasen (Reihenfolge wie von
     *                 AimsunJunction.getPhaseIds() zurückgegeben
     * @param junction Die Junction, für die der TLC erzeugt wird.
     * @param phaseIds Ids der Phasen, die von diesem TLC berücksichtigt werden
     *                 sollen.
     */
    public FixedTimeController(final float[] maxOuts, final AimsunJunction junction, final List<Integer> phaseIds) throws TLCException {
        super(maxOuts, junction, phaseIds);
    }

    @Override
    public final TrafficLightControllerParameters getParameters() {
        /*
		 * integers[0]: junctionId integers[1...numPhases]: phaseIds
		 * 
		 * maxGreenTimes[0...numPhases-1]: maxGreenTimes
		 * 
		 * booleans[0...numPhases]: is interphase
		 */
        final int[] ids = new int[numPhases + 1];
        final float[] maxGreenTimes = new float[numPhases];
        final boolean[] isInterphase = new boolean[numPhases];

        ids[0] = junction.getId();

        for (int i = 0; i < numPhases; i++) {
            ids[i + 1] = phaseIds.get(i);
            maxGreenTimes[i] = phases[i].getMaxGreenTime();
            isInterphase[i] = phases[i].getAimsunPhase().isInterphase();
        }

        return new TrafficLightControllerParameters(TLCTypes.FIXEDTIME, ids, maxGreenTimes, new String[0], isInterphase);
    }

    @Override
    public final void step(final float time) throws TLCException {
        if (time < getTimeOfLastChange()) {
            throw new TLCException("Time moves backwards");
        }

        final float currentPhaseRuntime = time - getTimeOfLastChange();
        if (currentPhaseRuntime >= phases[currentPhase].getMaxGreenTime()) {
            final float timeCorrection = currentPhaseRuntime - phases[currentPhase].getMaxGreenTime();
            currentPhase++;

            if (currentPhase >= numPhases) {
                currentPhase = 0;
            }

            setTimeOfLastPhaseChange(time - timeCorrection);
        }
    }
}
