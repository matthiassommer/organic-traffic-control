package de.dfg.oc.otc.region;

/**
 * Class represents an edge within the graph.
 *
 * @author tomforde
 */
class Edge implements Comparable<Edge> {
    /**
     * Target node of this edge.
     */
    public final Vertex destination;
    /**
     * Cost of this edge.
     */
    private float cost;
    /**
     * Flag indicating whether edge is section(true) or turning.
     */
    private boolean isIntermediate;
    /**
     * Flag indicating whether cost is inverted or not.
     */
    private boolean isInverted;
    /**
     * Initial cost - before inverting.
     */
    private float primaryCost = -1;
    private int startVertexID = -1;

    /**
     * Constructor.
     *
     * @param destination The {@code vertex} this Edge leads to
     * @param cost        The cost of this Edge
     */
    Edge(final Vertex destination, final float cost) {
        this.destination = destination;
        this.cost = cost;
    }

    /**
     * Method used to compare edges by using the cost.
     *
     * @return -1 if cost of this is less +1 if cost of this is larger 0 if cost
     * are equal
     */
    public final int compareTo(final Edge other) {
        // falls selbst kleiner - R�ckgabe negativ
        return cost < other.cost ? -1
                // falls selbst groesser - R�ckgabe negativ
                : cost > other.cost ? +1
                // bei Gleichheit - R�ckgabe 0
                : 0;
    }

    final float getCost() {
        return cost;
    }

    /**
     * Get destination vertex for this edge.
     *
     * @return The destination vertex
     */
    final Vertex getDestination() {
        return destination;
    }

    /**
     * Method used to generate a complete description of the edge.
     *
     * @return The description of the edge.
     */
    final String getEdgeDescription() {
        final StringBuilder sb = new StringBuilder();
        final String linesep = System.getProperty("line.separator");

        sb.append("Edge from Vertex ").append(startVertexID).append(" to ").append(destination.getID()).append(linesep);
        if (isInverted) {
            sb.append(" is inverted. Primary cost: ").append(primaryCost);
        } else {
            sb.append(" is NOT inverted. ");
        }
        sb.append("The edge has cost: ").append(cost);

        return sb.toString();
    }

    /**
     * If cost are inverted, this method returns the originally set value.
     *
     * @return The originally set cost
     */
    final float getPrimaryCost() {
        return primaryCost;
    }

    /**
     * Method returns the ID of the origin.
     *
     * @return The ID of the start vertex
     */
    final int getStartVertexID() {
        return startVertexID;
    }

    /**
     * Method used to set the ID of the start vertex.
     *
     * @param startVertexID
     */
    final void setStartVertexID(final int startVertexID) {
        this.startVertexID = startVertexID;
    }

    /**
     * Method used to invert the cost.
     *
     * @param maxCost Value needed to invert cost: cost will be substracted from
     *                this value
     */
    final void invertCost(final float maxCost) {
        if (!isInverted) {
            this.primaryCost = cost;
            this.cost = maxCost - cost;
            this.isInverted = true;
        } else {
            this.cost = primaryCost;
            this.primaryCost = -1;
            this.isInverted = false;
        }
    }

    /**
     * Method checks, whether this edge is representing a section or a turning.
     * Sections have no initial weight.
     *
     * @return {@code true} if edge is section, {@code false} if
     * turning
     */
    final boolean isIntermediate() {
        return isIntermediate;
    }

    /**
     * Method used to set flag: {@code true} if edge is section,
     * {@code false} if turning.
     *
     * @param isIntermediate Flag, if edge is section or turning
     */
    final void setIntermediate(final boolean isIntermediate) {
        this.isIntermediate = isIntermediate;
    }

    /**
     * Method returns, if cost of edge are inverted or not.
     *
     * @return Cost inverted (true) or not (false)
     */
    final boolean isInverted() {
        return isInverted;
    }
}
