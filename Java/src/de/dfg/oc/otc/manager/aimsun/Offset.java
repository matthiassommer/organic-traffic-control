package de.dfg.oc.otc.manager.aimsun;

import java.util.List;

/**
 * Datastructure representing offsets between junctions. Difference between a
 * reference time and the start time of the phase serving the coordinated
 * traffic movement.
 *
 * @author hpr
 */
public class Offset {
    /**
     * 1st node of offset relationship.
     */
    private final AimsunJunction originNode;
    /**
     * 2nd node of offset relationship.
     */
    private AimsunJunction destinationNode;
    /**
     * Alternative destination: sink.
     */
    private Centroid destinationSink;
    /**
     * Offset in seconds (i.e. time needed to travel the path).
     */
    private int offset;
    /**
     * Path between origin and destination.
     */
    private List<Section> path;

    /**
     * Creates an offset object between two junctions.
     *
     * @param origin the origin node
     * @param dest   the destination node
     * @param path   a path (i.e. a sequence of sections) between origin and
     *               destination
     * @param offset time needed to travel the path
     */
    Offset(final AimsunJunction origin, final AimsunJunction dest, final List<Section> path, final int offset) {
        this.originNode = origin;
        this.destinationNode = dest;
        this.path = path;
        this.offset = offset;
    }

    /**
     * Creates an offset object from a junction to a sink.
     *
     * @param origin the origin node
     * @param sink   destination is a centroid
     * @param path   a path (i.e. a sequence of sections) between origin and
     *               destination
     * @param offset time needed to travel the path
     */
    Offset(final AimsunJunction origin, final Centroid sink, final List<Section> path, final int offset) {
        this.originNode = origin;
        this.destinationSink = sink;
        this.path = path;
        this.offset = offset;
    }

    public final AimsunJunction getDestinationJunction() {
        return destinationNode;
    }

    /**
     * Returns the offset in seconds stored in this object.
     *
     * @return the offset in seconds
     */
    public final int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public final AimsunJunction getOriginJunction() {
        return originNode;
    }

    /**
     * Returns a list of sections that connect the origin and destination node
     * of this object.
     *
     * @return a list of sections that connect the origin and destination node
     */
    public final List<Section> getPath() {
        return path;
    }

    public void setPath(List<Section> path) {
        this.path = path;
    }

    public final boolean isDestinationJunction() {
        return this.destinationSink == null;
    }

    @Override
    public final String toString() {
        String dest;
        if (isDestinationJunction()) {
            dest = destinationNode.getId() + " (Node)";
        } else {
            dest = destinationSink.getId() + " (Centroid)";
        }

        String output = originNode.getId() + " --> " + dest + ", section ";
        for (Section section : path) {
            output += section.getId() + ", ";
        }
        output += "offset " + offset + " sec.";

        return output;
    }
}
