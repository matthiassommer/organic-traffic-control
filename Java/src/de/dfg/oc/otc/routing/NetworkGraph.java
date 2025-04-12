package de.dfg.oc.otc.routing;

import de.dfg.oc.otc.manager.aimsun.Centroid;
import de.dfg.oc.otc.manager.aimsun.Section;

import java.util.*;
import java.util.Map.Entry;

/**
 * Graph representation of the road traffic network. Stores mappings of routes between routing components
 * and destinations (routing components or centroids).
 *
 * @author lyda
 */
public class NetworkGraph {
    /**
     * Depicts a route from a origin RC to a centroid with the insection of the origin RC.
     * Also maps the incoming section to the according RouteEntry.
     */
    protected final Map<RoutingComponent, Map<Centroid, Map<Section, RouteEntry>>> centroidMapping;
    /**
     * Depicts a route from a origin RC to a neighbouring RC with the insection of the origin RC.
     * Also maps the incoming section to the according RouteEntry.
     */
    protected final Map<RoutingComponent, Map<RoutingComponent, Map<Section, RouteEntry>>> rcMapping;

    protected NetworkGraph() {
        this.centroidMapping = new HashMap<>();
        this.rcMapping = new HashMap<>();
    }

    public final void reset() {
        this.centroidMapping.clear();
        this.rcMapping.clear();
    }

    /**
     * Creates a set with all centroids in the database.
     *
     * @return set of {@link Centroid}s
     */
    public final Set<Centroid> getCentroidSet() {
        final Set<Centroid> centroids = new HashSet<>();
        for (Map<Centroid, Map<Section, RouteEntry>> centroidEntries : centroidMapping.values()) {
            centroids.addAll(centroidEntries.keySet());
        }
        return centroids;
    }

    /**
     * Returns the RouteEntry for the incoming section to the destination.
     *
     * @param sourceRC     origin of the route
     * @param destCentroid target of the route
     * @param inSection    of the sourceRC
     * @return RouteEntry
     */
    public final RouteEntry getNextHop(final RoutingComponent sourceRC, final Centroid destCentroid,
                                       final Section inSection) {
        return centroidMapping.get(sourceRC).get(destCentroid).get(inSection);
    }

    /**
     * Selects the DatabaseEntry from the database which is between two
     * RoutingComponents with a special incoming section.
     *
     * @param sourceRC  origin of the route
     * @param successor target of the route
     * @param inSection of the sourceRC
     * @return RouteEntry
     */
    public final RouteEntry getNextHop(final RoutingComponent sourceRC, final RoutingComponent successor,
                                       final Section inSection) {
        return rcMapping.get(sourceRC).get(successor).get(inSection);
    }

    /**
     * Checks for neighboured Centroids which can be reached from a
     * RoutingComponent by entering with a specified InSection.
     *
     * @param sourceRC  RoutingComponent
     * @param inSection for which the destinations are determined
     * @return database entries
     */
    public final List<RouteEntry> getCentroidEntriesForSection(final RoutingComponent sourceRC,
                                                               final Section inSection) {
        final Map<Centroid, Map<Section, RouteEntry>> neighbourCentroids = centroidMapping.get(sourceRC);
        if (neighbourCentroids == null) {
            return new ArrayList<>();
        }

        final List<RouteEntry> dataBaseEntries = new ArrayList<>();
        for (Entry<Centroid, Map<Section, RouteEntry>> centroidEntries : neighbourCentroids.entrySet()) {
            final RouteEntry entry = centroidEntries.getValue().get(inSection);
            if (entry != null) {
                dataBaseEntries.add(entry);
            }
        }

        return dataBaseEntries;
    }

    /**
     * Checks for neighboured RoutingComponents which can be reached from the soureRC by taking the specified inSection.
     *
     * @param sourceRC  RoutingComponent to find neighbours for
     * @param inSection of the sourceRC for which the destinations are determined
     * @return database entries
     */
    public final List<RouteEntry> getRCEntriesForSection(final RoutingComponent sourceRC, final Section inSection) {
        final List<RouteEntry> dataBaseEntries = new ArrayList<>();
        final Map<RoutingComponent, Map<Section, RouteEntry>> neighbourRCs = rcMapping.get(sourceRC);

        if (neighbourRCs != null) {
            for (Entry<RoutingComponent, Map<Section, RouteEntry>> neighbourRCEntries : neighbourRCs.entrySet()) {
                RouteEntry entry = neighbourRCEntries.getValue().get(inSection);
                if (entry != null) {
                    dataBaseEntries.add(entry);
                }
            }
        }

        return dataBaseEntries;
    }

    /**
     * Updates the the graph with a {@link RouteEntry}.
     *
     * @param graphEntry
     */
    public final void updateLinkCost(final RouteEntry graphEntry) {
        if (graphEntry.getDestinationCentroid() != null && graphEntry.getDestinationRC() == null) {
            updateLinkcostToCentroid(graphEntry);
        } else if (graphEntry.getDestinationCentroid() == null && graphEntry.getDestinationRC() != null) {
            updateLinkcostToRC(graphEntry);
        }
    }

    /**
     * RoutingComponent is destination.
     *
     * @param entry
     */
    private void updateLinkcostToRC(RouteEntry entry) {
        // Get RoutingComponent->DatabaseEntry mapping for source RoutingComponent
        Map<RoutingComponent, Map<Section, RouteEntry>> linkCosts = rcMapping.get(entry
                .getSourceRC());
        if (linkCosts == null) {
            // create Mapping
            linkCosts = new HashMap<>();
            rcMapping.put(entry.getSourceRC(), linkCosts);
        }

        Map<Section, RouteEntry> entriesForRC = linkCosts.get(entry.getDestinationRC());
        if (entriesForRC == null) {
            // Create Section-> Entry Map
            entriesForRC = new HashMap<>();
        }
        entriesForRC.put(entry.getInSection(), entry);

        linkCosts.put(entry.getDestinationRC(), entriesForRC);
    }

    /**
     * Centroid is destination.
     *
     * @param entry
     */
    private void updateLinkcostToCentroid(RouteEntry entry) {
        // Get Centroid->DatabaseEntry mapping for source RoutingComponent
        Map<Centroid, Map<Section, RouteEntry>> linkCosts = centroidMapping.get(entry.getSourceRC());
        if (linkCosts == null) {
            // create Mapping
            linkCosts = new HashMap<>();
            centroidMapping.put(entry.getSourceRC(), linkCosts);
        }

        Map<Section, RouteEntry> entriesForCentroid = linkCosts.get(entry.getDestinationCentroid());
        if (entriesForCentroid == null) {
            // Create Section-> Entry Map
            entriesForCentroid = new HashMap<>();
        }
        entriesForCentroid.put(entry.getInSection(), entry);

        linkCosts.put(entry.getDestinationCentroid(), entriesForCentroid);
    }
}
