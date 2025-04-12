package de.dfg.oc.otc.layer2;

import de.dfg.oc.otc.tools.AbstractArrayUtilities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data set that represents a turning. (Used to communicate turning data to
 * Layer 2 as part of an {@code OptimisationTask}.)
 *
 * @author hpr
 */
public class TurningData implements Serializable {
    /**
     * Flow for the turning.
     */
    private final float flow;
    /**
     * Inbound section of the turning.
     */
    private final int inSectionId;

    private final int numberOfInboundLanes;
    /**
     * Outbound section of the turning.
     */
    private final int outSectionId;
    /**
     * Phases that include this turning.
     */
    private final List<Integer> phases;

    private boolean shared;

    public TurningData(final int inSection, final int outSection, final int numberOfLanes, final float flow) {
        this.inSectionId = inSection;
        this.outSectionId = outSection;
        this.numberOfInboundLanes = numberOfLanes;
        this.phases = new ArrayList<>();
        this.flow = flow;
    }

    public final void addPhase(final int phaseId) {
        if (!phases.contains(phaseId)) {
            phases.add(phaseId);
        }
    }

    public final float getFlow() {
        return flow;
    }

    public final int getNumberOfLanes() {
        return numberOfInboundLanes;
    }

    public final Iterable<Integer> getPhases() {
        return phases;
    }

    public final void setShared(final boolean shared) {
        this.shared = shared;
    }

    @Override
    public final String toString() {
        return inSectionId + " -> " + outSectionId + ", SHARED " + shared + ", #LANES  " + numberOfInboundLanes
                + ", PHASES " + AbstractArrayUtilities.arrayToString(phases) + ", FLOW " + flow + "\n";
    }
}
