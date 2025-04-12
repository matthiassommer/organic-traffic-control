package de.dfg.oc.otc.region;

/**
 * A MatchingObjekt is needed when creating the graph out of the nodes
 * descriptions, because edges between OTCNodes are not stored directly.
 * <p>
 * The reason is, that one OTCNode is represented by 8 sub-nodes (for two orthogonal streets)
 */
class JunctionDataStructure {
    /**
     * Subnode ID of the target node (OTCNodeID).
     */
    private int destinationSubNodeID;
    /**
     * Subnode ID of the origin node (OTCNodeID).
     */
    private int originSubNodeID;

    final int getDestinationSubNodeID() {
        return destinationSubNodeID;
    }

    final void setDestinationSubNodeID(final int destinationSubNodeID) {
        this.destinationSubNodeID = destinationSubNodeID;
    }

    final int getOriginSubNodeID() {
        return originSubNodeID;
    }

    final void setOriginSubNodeID(final int originSubNodeID) {
        this.originSubNodeID = originSubNodeID;
    }
}
