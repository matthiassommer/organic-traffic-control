package de.dfg.oc.otc.routing.linkState;

import de.dfg.oc.otc.manager.aimsun.Centroid;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.routing.RouteEntry;
import de.dfg.oc.otc.routing.RoutePriorityEntry;
import de.dfg.oc.otc.routing.RoutingComponent;
import de.dfg.oc.otc.routing.RoutingComponent.RegionalType;
import de.dfg.oc.otc.routing.RoutingTable;

import java.util.*;

/**
 * Implements Dijkstra's algorithm to use on a {@link de.dfg.oc.otc.routing.NetworkGraph} to determine
 * the shortest paths to centroids.
 *
 * @author Johannes
 */
public class DijkstraAlgorithm {
    /**
     * When the algorithm terminates, this lists holds the costs from the source RC to all reachable Centroids.
     */
    protected final Map<Centroid, Float> centroidToCost = new HashMap<>(10);
    /**
     * Mapping of a Centroid to a direct predecessing RoutingComponent.
     */
    protected final Map<Centroid, RoutingComponent> centroidToPredecessor = new HashMap<>(3);
    /**
     * Mapping of a Centroid to a NetworkGraphPriorityEntry (neigbour RC, insection of RC, outsection from RC to Centroid).
     */
    protected final Map<Centroid, RoutePriorityEntry> centroidToPriorityEntry = new HashMap<>(3);
    /**
     * Priority queue for entry processing (entries get sorted by costs ascending).
     */
    protected final List<RoutePriorityEntry> entryPriorityQueue = new LinkedList<>();
    /**
     * When the algorithm terminates, this lists holds the costs from the source RC to all reachable RoutingComponents.
     */
    protected final Map<RoutingComponent, Float> rcToCost = new HashMap<>();
    /**
     * Mapping of a RoutingComponent to its direct predecessor.
     */
    protected final Map<RoutingComponent, RoutingComponent> rcToPredecessor = new HashMap<>(3);
    /**
     * Mapping of the source RC to a NetworkGraphPriorityEntry (neigbour RC, insection of RC, outsection from RC to RC).
     */
    protected final Map<RoutingComponent, RoutePriorityEntry> rcToPriorityEntry = new HashMap<>(3);

    /**
     * For {@link RegionalLinkStateRC}s check the costs to border components in
     * the interior network and generate a {@link DatabaseEntry} for
     * their path.
     *
     * @param sourceRC                sender
     * @param entryToExpand           database entry
     * @param database                with LinkStateAdvertisements for interior or border
     * @param inSection               an incoming section of the sender
     * @param virtualBorderLinkAdvert Advertisement for a border link
     */
    private void determineCostsToBorder(final LinkStateRC sourceRC, final RoutePriorityEntry entryToExpand,
                                        final Database database, final Section inSection,
                                        final Advertisement virtualBorderLinkAdvert) {
        if (sourceRC instanceof RegionalLinkStateRC) {
            RegionalLinkStateRC regionalSourceRC = (RegionalLinkStateRC) sourceRC;

            if (regionalSourceRC.getRegionalRCType() == RegionalType.BORDER
                    && regionalSourceRC.getBorderRCsInRegion().contains(entryToExpand.getDestinationRC())) {
                // Create virtual database entry for this interior connection
                RoutingComponent rcLastOne = entryToExpand.getSourceRC();
                RoutingComponent rcSuccessor = null;
                if (rcLastOne != null) {
                    // Determine paths from this rc to sourceRC to find outSection for virtual link
                    while (rcLastOne != sourceRC) {
                        rcSuccessor = rcLastOne;
                        rcLastOne = rcToPredecessor.get(rcLastOne);
                    }

                    DatabaseEntry nextVirtualHopDBEntry;
                    if (rcSuccessor != null) {
                        nextVirtualHopDBEntry = (DatabaseEntry) database.getNextHop(sourceRC, rcSuccessor, inSection);

                        if (nextVirtualHopDBEntry != null) {
                            // Create virtual entry
                            DatabaseEntry virtualDatabaseEntry = new DatabaseEntry(
                                    sourceRC, (LinkStateRC) entryToExpand.getDestinationRC(), null, inSection,
                                    entryToExpand.getOutSection(), rcToCost.get(entryToExpand.getDestinationRC()), nextVirtualHopDBEntry.getOutSection().getId(), sourceRC.sequenceNumberCounter);
                            virtualBorderLinkAdvert.addLinkStateDataBaseEntry(virtualDatabaseEntry);
                        }
                    }
                }
            }
        }
    }

    /**
     * Run the Dijkstra algorithm on the received LinkState advertisements to
     * find the fastest paths to all centroids from the sourceRC.
     *
     * @param sourceRC       node to find fastest paths from
     * @param database       with LinkStateAdvertisements
     * @param borderDijkstra run the regional or normale variant
     */
    public void runAlgorithm(final LinkStateRC sourceRC, final Database database,
                             final boolean borderDijkstra) {
        // Used for RegionalLinkState: determination of internal virtual
        // directions between other border links inside the same region
        final Advertisement virtualBorderLinkAdvert = new Advertisement(sourceRC, sourceRC.sequenceNumberCounter);

        for (Section inSection : sourceRC.getJunction().getInSections()) {
            createSourceEntry(sourceRC, inSection);

            final Set<Centroid> destinations = database.getCentroidSet();
            // Routes to all targets from the sourceRC found or priorityQueue is empty
            while (!destinations.isEmpty() && !entryPriorityQueue.isEmpty()) {
                calculateCosts(database, sourceRC, inSection, destinations, virtualBorderLinkAdvert, borderDijkstra);
            }
            entryPriorityQueue.clear();

            determineShortestPaths(database, sourceRC, inSection);
        }

        sendBorderAdvertisement(sourceRC, virtualBorderLinkAdvert);
    }

    /**
     * @param database                with all DatabaseEntries
     * @param sourceRC                node to find fastest paths from
     * @param inSection               of the sourceRC
     * @param destinations            all reachable centroids in the network from the sourceRC
     * @param virtualBorderLinkAdvert advertisement for regional routing
     * @param borderDijkstra          calculate costs for local or regional routing?
     */
    private void calculateCosts(final Database database, final LinkStateRC sourceRC, final Section inSection,
                                final Collection<Centroid> destinations, final Advertisement virtualBorderLinkAdvert,
                                final boolean borderDijkstra) {
        // Entry with the lowest costs will be expanded if destination is a node
        final RoutePriorityEntry entryToExpand = entryPriorityQueue.remove(0);

        // Check if destination is Centroid or RoutingComponent
        if (entryToExpand.getDestinationRC() != null) {
            // RoutingComponent: Expand
            List<RouteEntry> localRCEntries = database.getRCEntriesForSection(
                    entryToExpand.getDestinationRC(), entryToExpand.getOutSection());
            updateOrInsertCostsForRC(localRCEntries, entryToExpand);

            List<RouteEntry> localCentroidEntries = database.getCentroidEntriesForSection(
                    entryToExpand.getDestinationRC(), entryToExpand.getOutSection());
            updateOrInsertCostsForCentroids(localCentroidEntries, entryToExpand);

            // sort queue ascending by costs
            Collections.sort(entryPriorityQueue);

            if (!borderDijkstra) {
                determineCostsToBorder(sourceRC, entryToExpand, database, inSection, virtualBorderLinkAdvert);
            }
        } else if (entryToExpand.getDestinationCentroid() != null) {
            // Link State search can be stopped when all destinations were found.
            // RegionalLinkState needs to find other border nodes in the region when it is a border node
            final boolean isNotRegionalLinkStateRC = !(sourceRC instanceof RegionalLinkStateRC);
            final boolean isBorder = borderDijkstra && sourceRC.getRegionalRCType() == RegionalType.BORDER;
            final boolean isInterior = !borderDijkstra && sourceRC.getRegionalRCType() == RegionalType.INTERIOR;
            if (isNotRegionalLinkStateRC || isBorder || isInterior) {
                destinations.remove(entryToExpand.getDestinationCentroid());
            }
        }
    }

    /**
     * For each incoming section of this node determine possible outgoing links to be used as initial data structure.
     *
     * @param sourceRC  routing component
     * @param inSection incoming section of the sourceRC
     */
    private void createSourceEntry(final LinkStateRC sourceRC, final Section inSection) {
        final DatabaseEntry sourceEntry = new DatabaseEntry(sourceRC, sourceRC, null, inSection,
                inSection, 0, -1, 0);
        entryPriorityQueue.add(new RoutePriorityEntry(sourceEntry, 0));
        rcToCost.put(sourceRC, 0f);
    }

    /**
     * For all destinations run Dijkstra shortest path. Each node independently
     * runs an algorithm over the map to determine the shortest path from itself
     * to every destination in the network.
     *
     * @param database  with all DatabaseEntries
     * @param sourceRC  node to find fastest paths from
     * @param inSection of the sourceRC
     */
    private void determineShortestPaths(final Database database, final LinkStateRC sourceRC,
                                        final Section inSection) {
        for (Centroid destination : database.getCentroidSet()) {
            RoutingComponent lastRC = centroidToPredecessor.get(destination);
            if (lastRC != null) {
                RoutingComponent successor = null;

                // Determine paths from centroids to source
                while (lastRC != sourceRC) {
                    successor = lastRC;
                    lastRC = rcToPredecessor.get(lastRC);
                }

                RouteEntry nextHopDBEntry;
                if (successor != null) {
                    // RoutingComponent->RoutingComponent  path
                    nextHopDBEntry = database.getNextHop(sourceRC, successor, inSection);
                } else {
                    // RoutingComponents->Centroid path
                    nextHopDBEntry = database.getNextHop(sourceRC, destination, inSection);
                }

                if (nextHopDBEntry != null) {
                    updateRoutingTable(nextHopDBEntry, sourceRC, inSection, destination);
                }
            }
        }
    }

    /**
     * Insert into Routingtable the reconstructed Graphentry
     *
     * @param nextHopDBEntry
     * @param sourceRC       starting node for this route to the destination
     * @param inSection      of the sourceRC
     * @param destination    a centroid
     */
    private void updateRoutingTable(RouteEntry nextHopDBEntry, LinkStateRC sourceRC, Section inSection,
                                    Centroid destination) {
        Integer nextSectionID;

        DatabaseEntry virtualNextHopDBEntry = (DatabaseEntry) nextHopDBEntry;
        if (virtualNextHopDBEntry.getRealOutSectionID() >= 0) {
            // insert real outSection
            nextSectionID = virtualNextHopDBEntry.getRealOutSectionID();
        } else {
            nextSectionID = nextHopDBEntry.getOutSection().getId();
        }

        final RoutingTable routingTable = sourceRC.getRoutingTableForSection(inSection.getId());
        routingTable.updateRoutingData(destination.getId(), nextSectionID, centroidToCost.get(destination));
    }

    /**
     * Send advertisements to border nodes.
     *
     * @param sourceRC                starting node
     * @param virtualBorderLinkAdvert
     */
    protected void sendBorderAdvertisement(final LinkStateRC sourceRC,
                                         final Advertisement virtualBorderLinkAdvert) {
        if (sourceRC instanceof RegionalLinkStateRC) {
            final RegionalLinkStateRC routingComponent = (RegionalLinkStateRC) sourceRC;
            if (routingComponent.getRegionalRCType() == RegionalType.BORDER) {
                routingComponent.setDijkstraAdvertisement(virtualBorderLinkAdvert);
            }
        }
    }

    /**
     * Update or insert costs in priority queue for routes from the sourceRC to all Centroids.
     *
     * @param localCentroidEntries
     * @param firstPriorityEntry
     */
    protected void updateOrInsertCostsForCentroids(final List<RouteEntry> localCentroidEntries,
                                                   final RoutePriorityEntry firstPriorityEntry) {
        for (RouteEntry entry : localCentroidEntries) {
            final Centroid target = entry.getDestinationCentroid();

            final Float oldCostsToTarget = centroidToCost.get(target);
            final Float newCostsToTarget = firstPriorityEntry.getCosts() + entry.getCosts();

            if (oldCostsToTarget != null) {
                if (newCostsToTarget < oldCostsToTarget) {
                    // Exchange old priority entry with the new one with lower costs
                    final RoutePriorityEntry oldPriorityEntry = centroidToPriorityEntry.get(target);
                    entryPriorityQueue.remove(oldPriorityEntry);
                } else {
                    continue;
                }
            }

            centroidToCost.put(target, newCostsToTarget);

            final RoutePriorityEntry newPriorityEntry = new RoutePriorityEntry(entry, newCostsToTarget);
            entryPriorityQueue.add(newPriorityEntry);

            centroidToPriorityEntry.put(target, newPriorityEntry);
            centroidToPredecessor.put(target, entry.getSourceRC());
        }
    }

    /**
     * Update or insert costs in priority queue for routes from the sourceRC to other RoutingComponents.
     *
     * @param localRcEntries routes from a RC (priorityEntry) to a neighbouring RC
     * @param priorityEntry  the current considered RC
     */
    protected void updateOrInsertCostsForRC(final List<RouteEntry> localRcEntries,
                                            final RoutePriorityEntry priorityEntry) {
        for (RouteEntry entry : localRcEntries) {
            final RoutingComponent destinationRC = entry.getDestinationRC();

            final Float oldCostsToNode = rcToCost.get(destinationRC);
            final Float newCostsToNode = priorityEntry.getCosts() + entry.getCosts();

            if (oldCostsToNode != null) {
                if (newCostsToNode < oldCostsToNode) {
                    // Exchange old priority entry with the new one with lower costs
                    final RoutePriorityEntry oldPriorityEntry = rcToPriorityEntry.get(destinationRC);
                    entryPriorityQueue.remove(oldPriorityEntry);
                } else {
                    continue;
                }
            }

            rcToCost.put(destinationRC, newCostsToNode);

            final RoutePriorityEntry newPriorityEntry = new RoutePriorityEntry(entry, newCostsToNode);
            entryPriorityQueue.add(newPriorityEntry);

            rcToPriorityEntry.put(destinationRC, newPriorityEntry);
            rcToPredecessor.put(destinationRC, entry.getSourceRC());
        }
    }
}