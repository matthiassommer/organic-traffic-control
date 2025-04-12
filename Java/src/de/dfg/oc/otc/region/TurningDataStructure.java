package de.dfg.oc.otc.region;

/**
 * Class describes a turning - used for the NDS.
 *
 * @author tomforde
 */
class TurningDataStructure {
    /**
     * ID of the sub-node responsible for outgoing sections.
     */
    private final int destinationNodeID;
    /**
     * ID of the sub-node responsible for incoming section.
     */
    private final int originNodeID;
    /**
     * The flow of this turning.
     */
    private float weight;

    /**
     * Constructor.
     *
     * @param origin      The origin subnode-ID
     * @param destination The target subnode-ID
     * @param weight      The flow of this turning
     */
    TurningDataStructure(final int origin, final int destination, final float weight) {
        this.originNodeID = origin;
        this.destinationNodeID = destination;
        this.weight = weight;
    }

    final int getDestinationNodeID() {
        return destinationNodeID;
    }

    final int getOriginNodeID() {
        return originNodeID;
    }

    final float getWeight() {
        return weight;
    }

    final void setWeight(final float weight) {
        this.weight = weight;
    }
}
