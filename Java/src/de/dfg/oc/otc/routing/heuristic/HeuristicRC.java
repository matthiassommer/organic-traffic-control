package de.dfg.oc.otc.routing.heuristic;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.Centroid;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.routing.ProtocolType;
import de.dfg.oc.otc.routing.RoutingComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Class implements a heuristic routing protocol for decentralised routing in
 * urban road networks. Orientated on the GPSR routing protocol.
 * <p>
 * Each node in the network determines the routes in the routing tables locally
 * decided with the knowledge of the whole network calculated with the
 * A*-algorithm (optimal and complete, finds the shortest route if one exists).
 * <p>
 * There is not communication. Each RC has the knowledge about the whole network
 * topology.
 *
 * @author lyda, tomforde
 */
public class HeuristicRC extends RoutingComponent {
    private final List<Centroid> centroidsSortedByDistance = new ArrayList<>();
    /**
     * Represents a graph of the road network annotated with the respective costs for routes between different targets.
     */
    private HeuristicNetworkGraph streetMap;

    public HeuristicRC(final ProtocolType type, final OTCNode theNode) {
        super(type, theNode);
    }

    /**
     * Initialize streetMap with driving time in between all nodes and destinations
     * <p>
     * Determines the local StreetGraphEntries and adds them to the HeuristicStreetGraph.
     * Update streetMap with local traffic situation.
     */
    private void determineLocalStreetGraphEntries() {
        // Determine link costs for all turnings to all neighbouring RC
        for (Section inSection : this.getJunction().getInSections()) {
            for (Section outSection : this.getJunction().getOutSections()) {

                float turningCost = determineTurningCost(inSection.getId(), outSection.getId());
                if (turningCost > 0) {
                    HeuristicRouteEntry newEntry = null;
                    // Check destination of outsection
                    // First if its a centroid
                    AimsunJunction nextJunction = outSection.getNextJunction();
                    if (nextJunction != null) {
                        HeuristicRC neighbourRC = (HeuristicRC) nextJunction.getNode().getRoutingComponent();
                        newEntry = new HeuristicRouteEntry(this, neighbourRC, null, inSection, outSection,
                                turningCost);
                    } else {
                        Integer centroidId = this.outSectionDestinations.get(outSection.getId());
                        if (centroidId != null) {
                            Centroid centroid = OTCManager.getInstance().getNetwork().getCentroid(centroidId);
                            newEntry = new HeuristicRouteEntry(this, null, centroid, inSection,
                                    outSection, turningCost);
                        }
                    }
                    if (newEntry != null) {
                        streetMap.updateLinkCost(newEntry);
                    }
                }
            }
        }
    }

    List<Centroid> getCentroidsSortedByDistance() {
        return centroidsSortedByDistance;
    }

    void addCentroid(int pos, Centroid centroid) {
        this.centroidsSortedByDistance.add(pos, centroid);
    }

    HeuristicNetworkGraph getStreetMap() {
        return streetMap;
    }

    @Override
    public void performProtocol() {
        if (streetMap == null) {
            streetMap = new HeuristicNetworkGraph();
        }

        determineLocalStreetGraphEntries();
        AStarAlgorithm.performAStar(this);

        streetMap.resetRoutesCount();
    }
}
