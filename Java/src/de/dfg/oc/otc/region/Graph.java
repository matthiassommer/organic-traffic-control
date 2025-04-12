package de.dfg.oc.otc.region;

import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class represents the graph consisting of vertices and edges.
 *
 * @author tomforde
 */
class Graph {
    private static final Logger log = Logger.getLogger(Graph.class);
    /**
     * The graph as set of vertices.
     */
    private final Map<Integer, Vertex> graph;
    /**
     * All subnode-IDs representing approaches.
     */
    private final List<Integer> inSubNodeIDs;
    /**
     * All subnode-IDs representing exits.
     */
    private final List<Integer> outSubNodeIDs;
    /**
     * Sorted list of all edges contained by this graph.
     */
    private final PriorityQueue<Edge> edges;

    Graph() {
        this.graph = new HashMap<>(10);
        this.inSubNodeIDs = new ArrayList<>(4);
        this.outSubNodeIDs = new ArrayList<>(4);
        this.edges = new PriorityQueue<>();
    }

    /**
     * Method adds an edge to this graph and creates vertices if they are
     * missing.
     *
     * @param sourceID       ID of the origin vertex
     * @param destID         ID of the target vertex
     * @param cost           The cost of the edge
     * @param isIntermediate Flag, if edge is section or turning (false)
     */
    final void addEdge(final Integer sourceID, final Integer destID, final float cost,
                       final boolean isIntermediate) {
        // finde Knoten v zum Startnamen
        final Vertex startVertex = getVertexWithUpdate(sourceID);

        // finde Knoten w zum Zielnamen
        final Vertex destinationVertex = getVertexWithUpdate(destID);

        // fuege Kante (v,w) mit Kosten cost ein
        final Edge edge = new Edge(destinationVertex, cost);
        edge.setIntermediate(isIntermediate);
        edge.setStartVertexID(sourceID);
        startVertex.addEdge(edge);
        edges.add(edge);
    }

    /**
     * Method used to add the IDs of subnodes (for approaches).
     *
     * @param subIns
     */
    final void addInSubNodeIDs(final List<Integer> subIns) {
        inSubNodeIDs.addAll(subIns.stream().filter(tmp -> !inSubNodeIDs.contains(tmp)).map(tmp -> tmp).collect(Collectors.toList()));
    }

    /**
     * Method used to add the IDs of subnodes (for exits).
     *
     * @param subOuts Subnode-IDs (exits)
     */
    final void addOutSubNodeIDs(final List<Integer> subOuts) {
        outSubNodeIDs.addAll(subOuts.stream().filter(tmp -> !outSubNodeIDs.contains(tmp)).map(tmp -> tmp).collect(Collectors.toList()));
    }

    /**
     * Method adds a new vertex to the graph.
     *
     * @param id   The ID of the vertex
     * @param vert The vertex to be added
     */
    private void addVertex(final int id, final Vertex vert) {
        if (graph.containsKey(id)) {
            log.warn("Adding of vertex not possible - already existing");
            return;
        }

        graph.put(id, vert);

        // Update of edge information
        edges.addAll(vert.getEdges());
    }

    /**
     * Delivers an identical copy of this graph.
     *
     * @return clone
     */
    public final Graph clone() {
        final Graph returnGraph = new Graph();

        // Add vertices
        for (Map.Entry<Integer, Vertex> integerVertexEntry : graph.entrySet()) {
            returnGraph.addVertex(integerVertexEntry.getKey(), integerVertexEntry.getValue());
        }

        // Add InSubNodes
        final List<Integer> in = new ArrayList<>();
        in.addAll(inSubNodeIDs);
        returnGraph.addInSubNodeIDs(in);

        // Add OutSubNodes
        final List<Integer> out = new ArrayList<>();
        out.addAll(outSubNodeIDs);
        returnGraph.addOutSubNodeIDs(out);

        return returnGraph;
    }

    /**
     * Method used to determine all edges representing turnings of a given
     * OTCNode.
     *
     * @param otcNodeId The identifier of the OTCNode
     * @return All edges representing turnings of that node
     */
    final List<Edge> getAllEdgesForOTCNode(final int otcNodeId) {
        final List<Edge> edges = new ArrayList<>();

        if (otcNodeId <= 0) {
            log.debug("Invald arguments - cannot determine edges!");
        }

        for (Edge tmp : this.edges) {
            // Fehler abfangen
            if (tmp.getStartVertexID() <= 0 || tmp.destination.getID() <= 0) {

                log.debug("Invalid edge found, ignoring: " + "Edge is okay: " + tmp.getEdgeDescription());
                continue;
            }

            // OTC-Start und EndknotenID der Kante bestimmen
            int tmpIDStart = tmp.getStartVertexID() / 100;
            int tmpIDEnd = tmp.destination.getID() / 100;

            // Wenn Turning des gesuchten Knotens, hinzufuegen
            if (tmpIDStart == tmpIDEnd && tmpIDEnd == otcNodeId) {
                edges.add(tmp);
            }
        }

        return edges;
    }

    /**
     * Method delivers all edges leading to this vertex.
     *
     * @return
     */
    final List<Edge> getAllEdgesLeadingToVertex(final Vertex target) {
        final List<Edge> edgeList = new ArrayList<>();

        for (Edge edge : edges) {
            boolean sameTarget = edge.destination.getID() == target.getID();
            boolean newEdge = !edgeList.contains(edge);
            if (sameTarget && newEdge) {
                edgeList.add(edge);
            }
        }

        return edgeList;
    }

    /**
     * Method creates a PriorityQueue out of all edges contained by this graph.
     * Queue is a clone of the internal structure to avoid removing at the
     * graph.
     *
     * @return The cloned Queue of all edges
     */
    final PriorityQueue<Edge> getCloneOfEdgeQueue() {
        final PriorityQueue<Edge> clonedQueue = new PriorityQueue<>();

        if (edges.isEmpty()) {
            log.warn("Invalid edge list!");
            return clonedQueue;
        }

        clonedQueue.addAll(edges.stream().map(edge -> edge).collect(Collectors.toList()));
        return clonedQueue;
    }

    /**
     * Method gets a specific vertex by its ID.
     *
     * @param id The identifier
     * @return The vertex, null otherwise
     */
    final Vertex getVertex(final Integer id) {
        return graph.get(id);
    }

    /**
     * Method gets a specific vertex by its ID, creates a new one if ID is
     * unknown.
     *
     * @param id The ID of the vertex
     * @return The vertex
     */
    private Vertex getVertexWithUpdate(final Integer id) {
        Vertex vertex = graph.get(id);
        if (vertex == null) {
            // Falls Vertex noch nicht vorhanden: neu anlegen!
            vertex = new Vertex(id);
            graph.put(id, vertex);
        }
        return vertex;
    }

    /**
     * Method used to invert the cost of all edges.
     *
     * @param maxCost The max. cost. Calculation: costOfEdge = maxCost - costOfEdge
     */
    final void invertCosts(final float maxCost) {
        for (Vertex vertex : graph.values()) {
            for (Edge edge : vertex.getEdges()) {
                edge.invertCost(maxCost);
            }
        }
    }

    /**
     * Method used to remove a given edge from the graph.
     *
     * @param edge The edge to be removed
     */
    final void removeEdge(final Edge edge) {
        if (edge.getStartVertexID() > 0) {
            final Vertex vertex = graph.get(edge.getStartVertexID());
            vertex.removeEdge(edge);
            edges.remove(edge);
        } else {
            log.warn("Cannot remove edge - edge is null or has no start!");
        }
    }
}
