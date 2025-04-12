package de.dfg.oc.otc.region;

import de.dfg.oc.otc.manager.OTCManagerException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class represents a vertex within the graph.
 *
 * @author tomforde
 */
class Vertex {
    /**
     * All neighbours of the node as list of edges.
     */
    private final List<Edge> edges = new ArrayList<>(5);
    /**
     * ID of the node.
     */
    private final int id;


    /**
     * Constructor for node.
     *
     * @param id The Identifier for this vertex
     */
    Vertex(final int id) {
        this.id = id;
    }

    /**
     * Method used to add an edge for this vertex.
     *
     * @param edge The new edge to be added
     */
    final void addEdge(final Edge edge) {
        edges.add(edge);
    }

    /**
     * Method returns the edge leading to the specified vertex.
     *
     * @param destination The vertex
     * @return The edge leading to the destination vertex, null otherwise
     */
    final Edge getEdge(final Vertex destination) throws OTCManagerException {
        for (Edge edge : edges) {
            if (edge.getDestination() == destination) {
                return edge;
            }
        }
        throw new OTCManagerException("Vertex not reachable from edge");
    }

    /**
     * Method returns all edges of this node.
     *
     * @return {@code List} of all edges
     */
    final List<Edge> getEdges() {
        return edges;
    }

    /**
     * Method returns the name of this vertex (the id).
     *
     * @return The name of this vertex (eq to ID)
     */
    final int getID() {
        return id;
    }

    /**
     * Method checks whether there is an edge to the vertex or not.
     *
     * @param destination the target node
     * @return
     */
    final boolean hasEdge(final Vertex destination) {
        for (Edge edge : edges) {
            if (edge.getDestination() == destination) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method used to remove an edge.
     *
     * @param edge The edge to be removed
     */
    final void removeEdge(final Edge edge) {
        if (edges.contains(edge)) {
            edges.remove(edge);
        } else if (edge.destination != null && edge.destination.id >= 0) {
            Iterator<Edge> it = this.edges.iterator();
            while (it.hasNext()) {
                Edge check = it.next();
                if (check.destination != null && check.destination.id >= 0
                        && check.destination.id == edge.destination.id) {
                    it.remove();
                }
            }
        }
    }
}
