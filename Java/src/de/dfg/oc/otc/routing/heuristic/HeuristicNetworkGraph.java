package de.dfg.oc.otc.routing.heuristic;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.Centroid;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.Turning;
import de.dfg.oc.otc.routing.NetworkGraph;
import de.dfg.oc.otc.routing.RouteEntry;
import de.dfg.oc.otc.routing.RoutingComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * Advanced graph representation of a traffic network with equal capacity usage.
 * The graph contains {@link HeuristicRouteEntry} which are saving the routes
 * which were put over them. This class is an enhancement of the {@link de.dfg.oc.otc.routing.NetworkGraph} and
 * reinitiates the routeNumber to 0 for new calculations.
 *
 * @author Johannes
 */
class HeuristicNetworkGraph extends NetworkGraph {
    /**
     * Maps routing components to centroids with the respective costs (delay in sec.).
     */
    private final Map<RoutingComponent, Map<Centroid, Float>> rcDistanceToDestination;

    /**
     * Creates a HeuristicStreetGraph for this network.
     */
    HeuristicNetworkGraph() {
        super();
        this.rcDistanceToDestination = new HashMap<>();
        fillWithNetwork();
    }

    /**
     * Fills the HeuristicStreetGraph with the average driving time between the
     * junctions and {@link HeuristicRouteEntry}.
     */
    private void fillWithNetwork() {
        // Run over all RoutingComponents
        for (AimsunJunction junction : OTCManager.getInstance().getNetwork().getControlledJunctions()) {
            HeuristicRC rc = (HeuristicRC) junction.getNode().getRoutingComponent();

            // Get the average driving time to neighbour RoutingComponents and
            // Centroids depending on in- and outSection combinations
            for (Section inSection : junction.getInSections()) {
                for (Section outSection : junction.getOutSections()) {
                    // Check if in- outSection combination exists
                    Turning turning = junction.getTurning(inSection.getId(), outSection.getId());
                    if (turning != null) {
                        float costs;
                        RoutingComponent destRC = null;
                        Centroid destCentroid = null;

                        // Follow outSection
                        if (outSection.getNextJunction() != null) {
                            // Get the average driving time to neighbour RoutingComponents
                            destRC = outSection.getNextJunction().getNode().getRoutingComponent();
                            costs = rc.costToNeighbour.get(destRC.getNodeID());
                        } else {
                            // Get the average driving time to neighbour Centroids
                            Integer destCentroidId = rc.getOutSectionDestinations().get(outSection.getId());
                            destCentroid = OTCManager.getInstance().getNetwork().getCentroid(destCentroidId);
                            costs = rc.localDestinationOffsets.get(destCentroidId).getOffset();
                        }

                        HeuristicRouteEntry newEntry = new HeuristicRouteEntry(rc, destRC, destCentroid,
                                inSection, outSection, costs);
                        updateLinkCost(newEntry);
                    }
                }
            }
        }
    }

    final Map<RoutingComponent, Map<Centroid, Float>> getRcDistanceToDestination() {
        return rcDistanceToDestination;
    }

    /**
     * Resets the route counter for each HeuristicStreetGraphEntry in the
     * {@link de.dfg.oc.otc.routing.NetworkGraph} for new route calculation.
     */
    final void resetRoutesCount() {
        for (Map.Entry<RoutingComponent, Map<RoutingComponent, Map<Section, RouteEntry>>> rcSource : rcMapping.entrySet()) {
            for (Map.Entry<RoutingComponent, Map<Section, RouteEntry>> rcEntries : rcSource.getValue().entrySet()) {
                for (Map.Entry<Section, RouteEntry> sectionEntry : rcEntries.getValue().entrySet()) {
                    HeuristicRouteEntry heuristicSectionEntry = (HeuristicRouteEntry) sectionEntry.getValue();
                    heuristicSectionEntry.resetRoutesCount();
                }
            }
        }

        for (Map.Entry<RoutingComponent, Map<Centroid, Map<Section, RouteEntry>>> rcSource : centroidMapping.entrySet()) {
            for (Map.Entry<Centroid, Map<Section, RouteEntry>> centroidEntries : rcSource.getValue().entrySet()) {
                for (Map.Entry<Section, RouteEntry> sectionEntry : centroidEntries.getValue().entrySet()) {
                    HeuristicRouteEntry heuristicSectionEntry = (HeuristicRouteEntry) sectionEntry.getValue();
                    heuristicSectionEntry.resetRoutesCount();
                }
            }
        }
    }
}
