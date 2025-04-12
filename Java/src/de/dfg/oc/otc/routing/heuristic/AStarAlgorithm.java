package de.dfg.oc.otc.routing.heuristic;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.Centroid;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.routing.RouteEntry;
import de.dfg.oc.otc.routing.RoutingComponent;
import de.dfg.oc.otc.routing.RoutingTable;
import org.apache.commons.math3.util.FastMath;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * Class implementing A*-algorithm for use on a HeuristicStreetGraph.
 *
 * @author Johannes
 */
abstract class AStarAlgorithm {
    // Initialize priority queue
    private static final LinkedList<PriorityEntry> openList = new LinkedList<>();

    private static float determineDistanceFromRCtoCentroid(final RoutingComponent rc, final Centroid destination) {
        final Point2D rcCoords = rc.getJunction().getCoordinates();
        final Point2D centroidCoords = destination.getCoordinates();

        return (float) Math.sqrt(FastMath.pow(rcCoords.getX() - centroidCoords.getX(), 2)
                + FastMath.pow(rcCoords.getY() - centroidCoords.getY(), 2));
    }

    /**
     * Perform the A* Algorithm on a HeuristicStreetGraph from a HeuristicRC to
     * all destinations in the network for each inSection.
     *
     * @param sourceRC source HeuristicRC
     */
    static void performAStar(final HeuristicRC sourceRC) {
        sortCentroidsByDistance(sourceRC);

        final List<Centroid> destinations = sourceRC.getCentroidsSortedByDistance();

        // Total costs to Centroids
        Map<Centroid, Float> centroidToDistances = new HashMap<>(5);
        Map<HeuristicRC, HeuristicRC> rcToPredecessor = new HashMap<>(5);
        Map<Centroid, HeuristicRC> centroidToPredecessor = new HashMap<>(5);
        // Maps the RoutingComponents and Centroids to the StreetGraphEntries in the openList
        Map<HeuristicRC, PriorityEntry> rcToPriorityEntryMap = new HashMap<>(5);
        Map<HeuristicRC, Float> distancesOverRCtoDestination = new HashMap<>(5);
        Map<HeuristicRC, Float> rcDistances = new HashMap<>(5);

        // Run over all destinations and determine best path
        for (Section inSection : sourceRC.getJunction().getInSections()) {
            for (Centroid destination : destinations) {
                centroidToDistances.clear();
                rcToPredecessor.clear();
                centroidToPredecessor.clear();
                rcToPriorityEntryMap.clear();
                distancesOverRCtoDestination.clear();
                rcDistances.clear();

                // HeuristicStreetGraphEntry for source to source
                HeuristicRouteEntry sourceEntry = new HeuristicRouteEntry(sourceRC, sourceRC, null,
                        inSection, inSection, 0);

                distancesOverRCtoDestination.put(sourceRC, (float) 0);
                openList.add(new PriorityEntry(sourceEntry, 0));
                rcDistances.put(sourceRC, sourceEntry.getCosts());

                while (!openList.isEmpty()) {
                    PriorityEntry firstPriorityEntry = openList.removeFirst();
                    HeuristicRouteEntry toExpandEntry = firstPriorityEntry.getGraphEntry();

                    // Check if destination is Centroid or RoutingComponent
                    if (toExpandEntry.getDestinationRC() != null) {
                        // Expand
                        updateOrInsertCostsForRCsInPriorityQueue(rcDistances, distancesOverRCtoDestination, rcToPredecessor, rcToPriorityEntryMap, sourceRC, destination, toExpandEntry);
                        updateOrInsertCostsForCentroidsInPriorityQueue(sourceRC, firstPriorityEntry, centroidToDistances, centroidToPredecessor);

                        // And sort list ascending in costs
                        Collections.sort(openList);
                    } else if (toExpandEntry.getDestinationCentroid() != null) {
                        // Stop if the found destination is the searched one
                        if (toExpandEntry.getDestinationCentroid() == destination) {
                            break;
                        }
                    }
                }

                openList.clear();

                // For all Destinations dijkstra shortest Path
                HeuristicRC rcLastOne = centroidToPredecessor.get(destination);
                if (rcLastOne != null) {
                    HeuristicRC successor = null;

                    // Determine paths from centroids to source
                    while (rcLastOne != sourceRC) {
                        // Increase routeCount on Entries
                        PriorityEntry rcLastOneEntry = rcToPriorityEntryMap.get(rcLastOne);
                        if (rcLastOneEntry != null) {
                            rcLastOneEntry.getGraphEntry().incrementRoutesCount();
                        }
                        successor = rcLastOne;
                        rcLastOne = rcToPredecessor.get(rcLastOne);
                    }

                    // Check if is RoutingComponent->RoutingComponent or RoutingComponents->Centroid path
                    RouteEntry nextHop;
                    if (successor != null) {
                        nextHop = sourceRC.getStreetMap().getNextHop(sourceRC, successor, inSection);
                    } else {
                        nextHop = sourceRC.getStreetMap().getNextHop(sourceRC, destination, inSection);
                    }

                    // Fill RoutingTable
                    RoutingTable routingTable = sourceRC.getRoutingTableForSection(inSection.getId());
                    routingTable.updateRoutingData(destination.getId(), nextHop.getOutSection().getId(),
                            centroidToDistances.get(destination));
                }
            }
        }
    }

    private static void sortCentroidsByDistance(HeuristicRC sourceRC) {
        if (sourceRC.getCentroidsSortedByDistance().isEmpty()) {
            // Map for distances from sourceRC to centroids
            final Map<Centroid, Float> distanceToCentroids = new HashMap<>();

            for (Map.Entry<Integer, Centroid> entry : OTCManager.getInstance().getNetwork().getCentroidMap().entrySet()) {
                float distance = determineDistanceFromRCtoCentroid(sourceRC, entry.getValue());
                distanceToCentroids.put(entry.getValue(), distance);

                // Run through centroidsSortedByDistance List to find insert point
                int pos = 0;

                for (Centroid sortedCentroid : sourceRC.getCentroidsSortedByDistance()) {
                    float sortedCentroidDistance = distanceToCentroids.get(sortedCentroid);
                    if (sortedCentroidDistance >= distance) {
                        break;
                    } else {
                        pos++;
                    }
                }

                // Insert centroid at determined point
                sourceRC.addCentroid(pos, entry.getValue());
            }
        }
    }

    private static void updateOrInsertCostsForCentroidsInPriorityQueue(HeuristicRC sourceRC,
                                                                       PriorityEntry firstPriorityEntry,
                                                                       Map<Centroid, Float> centroidToDistances,
                                                                       Map<Centroid, HeuristicRC> centroidToPredecessor) {
        HeuristicRouteEntry toExpandEntry = firstPriorityEntry.getGraphEntry();

        List<RouteEntry> localCentroidEntries = sourceRC.getStreetMap()
                .getCentroidEntriesForSection(toExpandEntry.getDestinationRC(),
                        toExpandEntry.getOutSection());

        Map<Centroid, PriorityEntry> centroidToPriorityEntryMap = new HashMap<>();

        // Update or insert costs in priority queue for Centroids
        for (RouteEntry localStreetGraphEntry : localCentroidEntries) {
            HeuristicRouteEntry localCentroidEntry = (HeuristicRouteEntry) localStreetGraphEntry;
            // Costs till here compared to till now found costs
            Float costsTillNow = centroidToDistances.get(localCentroidEntry.getDestinationCentroid());
            Float newCosts = firstPriorityEntry.getCosts() + localCentroidEntry.getCosts();

            boolean insertNew = true;

            if (costsTillNow != null) {
                if (newCosts < costsTillNow) {
                    // Find old priority entry, remove it and
                    // put
                    // the new
                    PriorityEntry oldPriorityEntry = centroidToPriorityEntryMap
                            .get(localCentroidEntry
                                    .getDestinationCentroid());
                    openList.remove(oldPriorityEntry);
                } else {
                    insertNew = false;
                }
            }
            if (insertNew) {
                centroidToDistances.put(localCentroidEntry
                        .getDestinationCentroid(), newCosts);
                PriorityEntry newPriorityEntry = new PriorityEntry(
                        localCentroidEntry, newCosts);
                openList.add(newPriorityEntry);
                centroidToPriorityEntryMap.put(
                        localCentroidEntry.getDestinationCentroid(),
                        newPriorityEntry);
                centroidToPredecessor.put(localCentroidEntry
                        .getDestinationCentroid(), (HeuristicRC) localCentroidEntry
                        .getSourceRC());
            }
        }
    }

    private static void updateOrInsertCostsForRCsInPriorityQueue(Map<HeuristicRC, Float> rcDistances,
                                                                 Map<HeuristicRC, Float> distancesOverRCtoDestination,
                                                                 Map<HeuristicRC, HeuristicRC> rcToPredecessor,
                                                                 Map<HeuristicRC, PriorityEntry> rcToPriorityEntryMap,
                                                                 HeuristicRC sourceRC,
                                                                 Centroid destination,
                                                                 HeuristicRouteEntry toExpandEntry) {
        List<RouteEntry> localRcEntries = sourceRC.getStreetMap()
                .getRCEntriesForSection(toExpandEntry.getDestinationRC(),
                        toExpandEntry.getOutSection());
        // Update or insert costs in priority queue for
        // RoutingComponents
        for (RouteEntry localStreetGraphEntry : localRcEntries) {
            HeuristicRouteEntry localRcEntry = (HeuristicRouteEntry) localStreetGraphEntry;
            // Costs till here compared to till now found costs
            Float costsTillNow = rcDistances.get(localRcEntry.getDestinationRC());
            // For new Costs use heuristic part which is the
            // distance from RoutingComponent of the
            // StreetGraphEntry to the actual destination
            // calculated in time with the speedlimit on the
            // outSection

            float heuristic = 0;

            Map<Centroid, Float> rcWithDistances = sourceRC.getStreetMap().getRcDistanceToDestination()
                    .get(localRcEntry.getDestinationRC());
            // If value not exists then create new HashMap
            if (rcWithDistances == null) {
                rcWithDistances = new HashMap<>();
                sourceRC.getStreetMap().getRcDistanceToDestination()
                        .put(localRcEntry.getDestinationRC(),
                                rcWithDistances);
            }

            Float distance = rcWithDistances.get(destination);
            // If heuristic not exists, calculate it
            if (distance == null) {
                distance = determineDistanceFromRCtoCentroid(
                        localRcEntry.getDestinationRC(), destination);
                rcWithDistances.put(destination, distance);
            }

            heuristic = (float) (distance / localRcEntry.getOutSection().getSpeedlimit() * 3.6);

            Float distanceTillDestRC = distancesOverRCtoDestination
                    .get(localRcEntry.getSourceRC())
                    + localRcEntry.getCosts();

            Float newCosts = distanceTillDestRC + heuristic;
            boolean insertNew = true;

            if (costsTillNow != null) {
                if (newCosts < costsTillNow) {
                    // Find old priority entry, remove it and
                    // put the new
                    PriorityEntry oldPriorityEntry = rcToPriorityEntryMap
                            .get(localRcEntry.getDestinationRC());
                    openList.remove(oldPriorityEntry);
                } else {
                    insertNew = false;
                }
            }
            if (insertNew) {
                rcDistances.put((HeuristicRC) localRcEntry.getDestinationRC(),
                        newCosts);
                distancesOverRCtoDestination.put((HeuristicRC) localRcEntry
                        .getDestinationRC(), distanceTillDestRC);
                PriorityEntry newPriorityEntry = new PriorityEntry(
                        localRcEntry, newCosts);
                openList.add(newPriorityEntry);
                rcToPriorityEntryMap.put((HeuristicRC) localRcEntry
                        .getDestinationRC(), newPriorityEntry);
                rcToPredecessor.put((HeuristicRC) localRcEntry.getDestinationRC(),
                        (HeuristicRC) localRcEntry.getSourceRC());
            }
        }
    }
}
