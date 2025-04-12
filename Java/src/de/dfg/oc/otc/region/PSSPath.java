package de.dfg.oc.otc.region;

import de.dfg.oc.otc.manager.OTCManagerException;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Class describes a path within a Progressive Signal System.
 *
 * @author tomforde
 */
class PSSPath implements Comparable<PSSPath> {
    private static final Logger log = Logger.getLogger(PSSPath.class);
    /**
     * The cost/weight of this path.
     */
    private float cost;
    private Map<Integer, Float> costs;
    /**
     * Initial cost of this path before inverting.
     */
    private float initialCost = -1;
    /**
     * The path defined as a sequence of nodes.
     */
    private final List<Vertex> interNodes;
    /**
     * Flag indicating, whether cost are inverted or not.
     */
    private boolean isInverted;
    /**
     * Flag indicates if path represents 8-node path or OTCNode path.
     */
    private boolean isOTCPath;
    /**
     * Flag used for the PSS system process when splitting conflicting streams.
     */
    private boolean isSplitted;
    /**
     * Origin node of this path.
     */
    private final Vertex origin;
    /**
     * The path defined as a sequence of edges.
     */
    private List<Edge> path;

    /**
     * Constructor.
     *
     * @param start The origin node of this path
     * @param path  The path defined as a sequence of vertices
     */
    PSSPath(final Vertex start, final LinkedList<Vertex> path) {
        this.origin = start;
        this.interNodes = path;
        generateEdgePath();
        this.costs = new HashMap<>(5);
    }

    final void addCost(final Map<Integer, Float> costs) {
        costs.entrySet().stream().filter(entry -> containsVertex(entry.getKey())).forEach(entry -> this.costs.put(entry.getKey(), entry.getValue()));
    }

    /**
     * Method used to check whether this path contains at least one identical
     * vertex as the given candidate (true), or not (false).
     *
     * @param extension The other path
     * @return Answer
     */
    final boolean checkConflicts(final PSSPath extension) {
        if (extension.interNodes.isEmpty()) {
            return true;
        }

        for (Vertex v : extension.interNodes) {
            // Fehlerueberpruefung
            if (v.getID() >= 0) {
                // Konflikt?
                if (this.containsVertex(v.getID())) {
                    return false;
                }
            }
        }
        // Kein Konflikt gefunden: Alles okay.
        return true;
    }

    /**
     * Method compares this path with another one by using the cost.
     *
     * @return {@code -1} if this.cost < other.cost {@code 0} if
     * this.cost = other.cost {@code 1} if this.cost > other.cost
     */
    public final int compareTo(final PSSPath path) {
        if (Float.isNaN(path.cost) || Float.isNaN(cost) || path.cost < 0 || cost < 0) {
            throw new RegionalManagerException("Error: Invalid path cost! Not able to compare PSS-paths!");
        } else if (this.cost < path.cost) {
            return -1;
        } else if (this.cost > path.cost) {
            return 1;
        }
        return 0;
    }

    /**
     * Method used to check whether this path contains a given vertex or not.
     *
     * @param vertexID The identifier of the given vertex
     * @return Answer: true or false.
     */
    private boolean containsVertex(final int vertexID) {
        if (origin != null && origin.getID() == vertexID) {
            return true;
        }

        if (!interNodes.isEmpty()) {
            // A:) nutze Vertices
            for (Vertex vertex : interNodes) {
                if (vertex.getID() == vertexID) {
                    return true;
                }
            }
        } else {
            // B:) nutze Edges
            for (Edge edge : path) {
                if (edge.destination.getID() == vertexID) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Method generates the path as a sequence of edges by using the sequence of
     * vertices.
     */
    private void generateEdgePath() {
        this.path = new ArrayList<>(5);

        boolean continueRun = true;
        final Iterator<Vertex> verIt = interNodes.listIterator();
        Vertex nextVertex = verIt.next();
        do {
            final Vertex v = nextVertex;

            if (verIt.hasNext()) {
                nextVertex = verIt.next();

                try {
                    final Edge tmp = v.getEdge(nextVertex);
                    path.add(tmp);
                } catch (OTCManagerException e) {
                    log.debug("Error while generating path: " + "No edge found between two linked vertices!");

                    String debug = "";
                    for (Vertex vertex : interNodes) {
                        debug += vertex.getID() + ",";
                    }
                    log.debug("ERROR for path: " + debug);
                }
            } else {
                // Abbruchkriterium erf�llt
                continueRun = false;
            }
        } while (continueRun);
    }

    final float getCost() {
        return cost;
    }

    final void setCost(final float cost) {
        this.cost = cost;
    }

    final Map<Integer, Float> getCosts() {
        return costs;
    }

    final void setCosts(final Map<Integer, Float> costs) {
        this.costs = costs;
    }

    /**
     * Method returns the cost of this path, before it has been inverted.
     *
     * @return The initial cost
     */
    final float getInitialCost() {
        return initialCost;
    }

    /**
     * Method returns the whole path as a linked sequence of vertices.
     *
     * @return Sequence of vertices
     */
    final List<Vertex> getInterNodes() {
        return interNodes;
    }

    /**
     * How many vertices are contained by this path?
     *
     * @return The nb of vertices on this path
     */
    final int getNumberOfNodes() {
        return interNodes.size();
    }

    /**
     * Method returns the origin node of this path.
     *
     * @return The origin node of this path
     */
    final Vertex getOrigin() {
        return origin;
    }

    /**
     * Method returns the whole path as a sequence of edges.
     *
     * @return Sequence of linked edges representing the path
     */
    final Collection<Edge> getPath() {
        return path;
    }

    /**
     * Method returns the length of the path as number of participating nodes.
     * This is not equal to the length in meters or the number of OTCNodes!
     *
     * @return The number of involved vertices
     */
    final int getPathLength() {
        return interNodes.size();
    }

    /**
     * Get the predecessor node of the vertex.
     *
     * @param vertex
     * @return predecessor vertex
     */
    final Vertex getPredecessor(final Vertex vertex) {
        for (Vertex node : interNodes) {
            if (node.hasEdge(vertex)) {
                return node;
            }
        }

        throw new OTCManagerException("Couldn't find predecessor node for vertex.");
    }

    /**
     * Method used to invert the cost of this path.
     *
     * @param invertValue The value used to invert the cost (should be >= maximum path
     *                    cost). Calculation: cost = invertValue - cost; re-invertation
     *                    possible.
     */
    final void invertCost(final float invertValue) {
        if (isInverted) {
            log.debug("Re-inverted path cost");
            this.isInverted = false;
            this.cost = initialCost;
            this.initialCost = -1;
        } else {
            this.isInverted = true;
            this.initialCost = cost;
            this.cost = invertValue - cost;
        }
    }

    /**
     * Method used to check, whether the cost of this path are inverted or not.
     *
     * @return Flag indicating if cost are inverted {@code true} or not (
     * {@code false})
     */
    final boolean isInverted() {
        return isInverted;
    }

    /**
     * Method return status of flag: is this a path of OTC nodes (true) or
     * sub-nodes (false)?
     *
     * @return Answer
     */
    final boolean isOTCPath() {
        return isOTCPath;
    }

    /**
     * Method used to set flag: is this path a representation of OTC nodes
     * (true) or sub-nodes (false).
     *
     * @param isOTCPath
     */
    final void setOTCPath(final boolean isOTCPath) {
        this.isOTCPath = isOTCPath;
    }

    /**
     * Is this path part of a larger one?
     *
     * @return
     */
    final boolean isSplitted() {
        return isSplitted;
    }

    /**
     * Sets the splitted flag - is this path part of a larger one?
     *
     * @param isSplitted
     */
    final void setSplitted(final boolean isSplitted) {
        this.isSplitted = isSplitted;
    }

    /**
     * Method used to update the cost of an OTC path. If the HashMap with costs
     * for each node is set, the first one will be ignored and the sum for the
     * following ones calculated.
     */
    final void updateCost() {
        if (interNodes.isEmpty() || costs.isEmpty()) {
            // TODO noch mehr Problemfaelle?
            log.debug("Error updating cost for path - invalid values!");
        }

        interNodes.stream().filter(vertex -> !costs.containsKey(vertex.getID())).forEach(this::outputDebugInformations);
    }

    private void outputDebugInformations(Vertex vertex) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Looking for node ID: ").append(vertex.getID()).append(". Path has nodes: ");
        for (Vertex node : interNodes) {
            stringBuilder.append(node.getID()).append(", ");
        }

        stringBuilder.append(" and this path knows cost for nodes: ");
        for (Integer cost : costs.keySet()) {
            stringBuilder.append(cost).append(", ");
        }

        log.debug(stringBuilder.toString());
    }
}
