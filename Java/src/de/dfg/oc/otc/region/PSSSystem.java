package de.dfg.oc.otc.region;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A PSSSystem is a set of PSS-Streams representing a possible configuration of
 * PSSs for the sub-net.
 *
 * @author tomforde
 */
@SuppressWarnings("serial")
class PSSSystem extends ArrayList<PSSPath> {
    private static final Logger log = Logger.getLogger(PSSSystem.class);
    private List<Integer> containedNodeIDs;
    /**
     * List of nodes being part of the streams.
     */
    private List<Vertex> containedNodes;

    /**
     * Constructor.
     *
     * @param theId The Identifier of this PSSSystem
     */
    PSSSystem(final int theId) {
        this.containedNodes = new ArrayList<>(5);
        this.containedNodeIDs = new ArrayList<>(5);
        log.debug("Created PSSSystem using ID: " + theId);
    }

    /**
     * Constructor.
     *
     * @param id
     * @param path
     */
    PSSSystem(final int id, final PSSPath path) {
        super(id);
        this.add(path);
    }

    /**
     * Method used to calculate the nb of benefiting cars for the whole PSS
     * system.
     *
     * @return The nb of cars
     */
    final float calculateBenefit() {
        float benefit = 0;
        log.debug("System consists of " + size() + " paths");

        for (PSSPath path : this) {
            final float costs = path.getCost();
            if (Float.isNaN(costs) || Float.isInfinite(costs)) {
                log.debug("Invalid path is part of PSS system - cannot determine its benefit!");
                continue;
            }

            if (path.isInverted()) {
                benefit += path.getInitialCost();
            } else {
                benefit += costs;
            }
        }

        log.debug("Benefit for system is: " + benefit);
        return benefit;
    }

    /**
     * Method returns a list of IDs of all vertices being already part of this
     * PSS system and are also contained by the candidate path.
     *
     * @param path The path to be verified
     * @return List of all vertex IDs
     */
    final Collection<Integer> getConflictingVertexIDs(final PSSPath path) {
        final Collection<Integer> vertices = new ArrayList<>();
        updateContainedNodes();

        for (Vertex vertex : path.getInterNodes()) {
            if (vertex.getID() <= 0) {
                log.debug("Invalid vertex received as argument.");
                continue;
            }

            if (this.containedNodeIDs.contains(vertex.getID())) {
                vertices.add(vertex.getID());
            }
        }

        return vertices;
    }

    /**
     * Method used to update the list of contained nodes for this PSS system.
     */
    private void updateContainedNodes() {
        this.containedNodes = new ArrayList<>(5);
        this.containedNodeIDs = new ArrayList<>(5);

        this.forEach(this::addNodesOfPath);
    }

    private void addNodesOfPath(PSSPath path) {
        path.getInterNodes().stream().filter(vertex -> !containedNodes.contains(vertex)).forEach(v -> {
            this.containedNodes.add(v);
            this.containedNodeIDs.add(v.getID());
        });
    }

    /**
     * Method used check, if a given stream could be added to this PSS system
     * without conflicts.
     *
     * @param extension The candidate to be added
     * @return Answer
     */
    final boolean verifyAdditionalStream(final PSSPath extension) {
        for (PSSPath path : this) {
            boolean check = path.checkConflicts(extension);
            if (!check) {
                return false;
            }
        }

        return true;
    }
}
