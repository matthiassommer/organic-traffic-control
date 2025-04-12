package de.dfg.oc.otc.region;

import de.dfg.oc.otc.manager.OTCNode;

import java.util.*;

/**
 * Class used to store the complete representation of this node needed by the
 * Regional Manager.
 *
 * @author tomforde
 */
class NodeDataStructure {
    private final List<Integer> entrySubNodes;
    private final List<Integer> exitSubNodes;
    /**
     * Edges between sub-nodes.
     */
    private final Map<Integer, TurningDataStructure> innerEdges;
    /**
     * sub-nodes responsible for incoming neighbours.
     */
    private final Map<Integer, Integer> inNodes;
    // int 1: otcnode-id, int 2: subnode-id
    /**
     * ID of this OTCNode.
     */
    private final int nodeID;
    /**
     * Sub-nodes responsible for outgoing neighbours.
     */
    private final Map<Integer, Integer> outNodes;
    /**
     * All known neighbours.
     */
    private final Map<Integer, OTCNode> regionalIncomingNeighbours;
    private final Map<Integer, OTCNode> regionalOutgoingNeighbours;
    /**
     * Currently max. subnode-ID for incoming sections.
     */
    private int maxInNodeID;
    /**
     * Currently max. subnode-ID for outgoing sections.
     */
    private int maxOutNodeID = 10;
    /**
     * Flag indication, if calculation is done by using turnings or sections
     * (false).
     */
    private boolean usingTurnings = true;

    /**
     * Constructor.
     *
     * @param nodeID ID of the corresponding OTCNode
     */
    NodeDataStructure(final int nodeID) {
        this.nodeID = nodeID;
        this.inNodes = new HashMap<>(4);
        this.outNodes = new HashMap<>(4);
        this.regionalIncomingNeighbours = new HashMap<>(3);
        this.regionalOutgoingNeighbours = new HashMap<>(3);
        this.innerEdges = new HashMap<>(10);
        this.entrySubNodes = new ArrayList<>(3);
        this.exitSubNodes = new ArrayList<>(3);
    }

    /**
     * Adds a new Entry.
     *
     * @param origin
     * @param target
     * @param weight
     * @param inSectionID
     * @param outSectionID
     */
    final void addEntryUsingTurnings(final OTCNode origin, final OTCNode target, final float weight,
                                     final int inSectionID, final int outSectionID) {
        if (!usingTurnings && !innerEdges.isEmpty()) {
            throw new RegionalManagerException(
                    "Not possible to use edges based on turning (addEntryUsingTurnings) AND section (addEntryUsingSections) information in one calculation!");
        } else if (!usingTurnings && innerEdges.isEmpty()) {
            usingTurnings = true;
        }

        // TODO bei einkommenden und ausgehenden Knoten die Info speichern!

        // Speichere neue Nachbarn
        if (origin == null) {
            // Ursprung ist Centroid
            // TODO was machen?
        } else if (!regionalIncomingNeighbours.containsKey(origin.getId())) {
            regionalIncomingNeighbours.put(origin.getId(), origin);
        }

        if (target == null) {
            // Senke ist Centroid
            // TODO was machen?
        } else if (!regionalOutgoingNeighbours.containsKey(target.getId())) {
            regionalOutgoingNeighbours.put(target.getId(), target);
        }

        int inNodeID = getInNodeID(origin, inSectionID);
        int outNodeID = getOutNodeID(target, outSectionID);

        // Erstelle/ersetze das TDS Objekt, f�ge der Liste hinzu
        TurningDataStructure tds;
        // turID = AAAABBBBCCCC, mit A=realNodeID, B=originNodeID, C=targetNodeID
        final int turningIdentifier = 1000000 * nodeID + 1000 * inSectionID + outSectionID;

        if (innerEdges.containsKey(turningIdentifier)) {
            tds = innerEdges.get(turningIdentifier);
            // aktualisieren der Werte, verhindern von doppelten
            tds.setWeight(weight);
        } else {
            tds = new TurningDataStructure(inNodeID, outNodeID, weight);
        }

        innerEdges.put(turningIdentifier, tds);
    }

    /**
     * Hole die passenden sub-node IDs.
     *
     * @param target
     * @param outSectionID
     */
    private int getOutNodeID(OTCNode target, int outSectionID) {
        int outNodeID;
        if (target == null) {
            // Wenn target == centroid, nutze sectionID als successor
            outNodeID = getSubNodeID(outSectionID, false);
            if (!exitSubNodes.contains(outNodeID)) {
                exitSubNodes.add(outNodeID);
            }
        } else {
            outNodeID = getSubNodeID(target.getId(), false);
        }
        return outNodeID;
    }

    private int getInNodeID(OTCNode origin, int inSectionID) {
        int inNodeID;
        if (origin == null) {
            // Wenn origin == centroid, nutze sectionID als predecessor
            inNodeID = getSubNodeID(inSectionID, true);
            if (!entrySubNodes.contains(inNodeID)) {
                entrySubNodes.add(inNodeID);
            }
        } else {
            inNodeID = getSubNodeID(origin.getId(), true);
        }
        return inNodeID;
    }

    final int getInNodeIDForNeighbour(final int nodeID) {
        return inNodes.get(nodeID);
    }

    final List<Integer> getInSubNodeIDs() {
        return entrySubNodes;
    }

    final int getNodeID() {
        return nodeID;
    }

    final int getOutNodeIDForNeighbour(final int nodeID) {
        return outNodes.get(nodeID);
    }

    final List<Integer> getOutSubNodeIDs() {
        return exitSubNodes;
    }

    final Iterable<OTCNode> getPredecessorNodes() {
        return regionalIncomingNeighbours.values();
    }

    private int getSubNodeID(final int nodeId, final boolean isInNode) {
        if (nodeId < 0) {
            return -1;
        }

        int id;
        final int nodeID = this.nodeID * 100;

        if (isInNode) {
            // subnode responsible for incoming sections
            if (inNodes.containsKey(nodeId)) {
                id = inNodes.get(nodeId);
            } else {
                id = nodeID + maxInNodeID;
                inNodes.put(nodeId, id);
                maxInNodeID++;
            }
        } else {
            // subnode responsible for outgoing sections
            if (outNodes.containsKey(nodeId)) {
                id = outNodes.get(nodeId);
            } else {
                id = maxOutNodeID + nodeID;
                outNodes.put(nodeId, id);
                maxOutNodeID++;
            }
        }
        return id;
    }

    final Iterable<OTCNode> getSuccessorNodes() {
        return regionalOutgoingNeighbours.values();
    }

    final Collection<TurningDataStructure> getTDS() {
        return innerEdges.values();
    }
}
