package de.dfg.oc.otc.region;

/**
 * Datastructure used to represent traffic streams.
 *
 * @author tomforde
 */
class TrafficStream {
    /**
     * The ID of the node where the stream starts.
     */
    private final int originNodeID;
    /**
     * The strength of the stream in vehicles/hour.
     */
    private final float streamStrength;
    /**
     * The synchronisation time provided by the sending node.
     */
    private float synchTime;
    /**
     * The ID of the node where the stream continues.
     */
    private final int targetNodeID;

    /**
     * Constructor for Traffic Stream data structure.
     *
     * @param originNodeID The ID of the node where the stream starts
     * @param targetNodeID The ID of the node where the stream continues
     * @param strength     The strength of the stream in vehicles/hour
     * @param time         The synchronisation time provided by the sending node
     */
    TrafficStream(final int originNodeID, final int targetNodeID, final float strength, final float time) {
        this.originNodeID = originNodeID;
        this.targetNodeID = targetNodeID;
        this.streamStrength = strength;
        this.synchTime = time;
    }

    final int getOriginNodeID() {
        return originNodeID;
    }

    final float getStreamStrength() {
        return streamStrength;
    }

    final int getTargetNodeID() {
        return targetNodeID;
    }

    final void setSynchTime(final float synchTime) {
        this.synchTime = synchTime;
    }

    @Override
    public final String toString() {
        return "Traffic stream from node " + originNodeID + " to node " + targetNodeID + " has strength of "
                + streamStrength + " veh/hour and synchronisation time " + synchTime;
    }
}
