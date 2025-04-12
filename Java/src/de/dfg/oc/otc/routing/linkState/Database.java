package de.dfg.oc.otc.routing.linkState;

import de.dfg.oc.otc.manager.aimsun.Centroid;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.routing.NetworkGraph;
import de.dfg.oc.otc.routing.RouteEntry;
import de.dfg.oc.otc.routing.RoutingComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * Saves link data between routing components in the network for computing
 * routing information over the network.
 *
 * @author lyda
 */
public class Database extends NetworkGraph {
    /**
     * Gets an LinkStateAdvert extracts the LinkStateDataBaseEntries and inserts
     * them in the database.
     *
     * @param advertisement to be inserted
     * @return {@code true} if advert is inserted, {@code false} otherwise
     */
    public final boolean updateLinkCost(final Advertisement advertisement) {
        for (DatabaseEntry databaseEntry : advertisement.getDatabaseEntries()) {
            if (!updateLinkCost(databaseEntry)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Updates the the database with a {@link DatabaseEntry} if this
     * entry does not exist yet or if it is newer then the current information.
     *
     * @param newEntry
     * @return if update performed or not
     */
    private boolean updateLinkCost(final DatabaseEntry newEntry) {
        if (newEntry.getDestinationCentroid() != null && newEntry.getDestinationRC() == null) {
            return checkEntryForCentroid(newEntry);
        } else if (newEntry.getDestinationCentroid() == null && newEntry.getDestinationRC() != null) {
            return checkEntryRoutingComponent(newEntry);
        }
        return false;
    }

    /**
     * RoutingComponent is destination.
     *
     * @param newEntry
     */
    private boolean checkEntryRoutingComponent(final DatabaseEntry newEntry) {
        // Get RoutingComponent->DatabaseEntry mapping for source RoutingComponent
        Map<RoutingComponent, Map<Section, RouteEntry>> rcLinkCosts = rcMapping.get(newEntry.getSourceRC());
        if (rcLinkCosts == null) {
            // Not existing -> create Mapping
            rcLinkCosts = new HashMap<>();
            rcMapping.put(newEntry.getSourceRC(), rcLinkCosts);
        }

        Map<Section, RouteEntry> entriesForRC = rcLinkCosts.get(newEntry.getDestinationRC());
        if (entriesForRC != null) {
            // Compare entries for actuality
            final DatabaseEntry entryForSection = (DatabaseEntry) entriesForRC.get(newEntry.getInSection());
            if (entryForSection != null && entryForSection.getSequenceNumber() > newEntry.getSequenceNumber()) {
                return false;
            }
        } else {
            // Create Section-> Entry Map
            entriesForRC = new HashMap<>();
        }

        entriesForRC.put(newEntry.getInSection(), newEntry);
        rcLinkCosts.put(newEntry.getDestinationRC(), entriesForRC);

        return true;
    }

    /**
     * Centroid is destination.
     *
     * @param newEntry
     * @return {@code true} if entry was used and not outdated, {@code false} otherwise
     */
    private boolean checkEntryForCentroid(final DatabaseEntry newEntry) {
        Map<Centroid, Map<Section, RouteEntry>> centroidLinkCosts = centroidMapping.get(newEntry.getSourceRC());
        if (centroidLinkCosts == null) {
            // Not existing -> create Mapping
            centroidLinkCosts = new HashMap<>();
            centroidMapping.put(newEntry.getSourceRC(), centroidLinkCosts);
        }

        Map<Section, RouteEntry> entriesForCentroid = centroidLinkCosts.get(newEntry.getDestinationCentroid());
        if (entriesForCentroid != null) {
            // Compare entries for actuality
            final DatabaseEntry entryForSection = (DatabaseEntry) entriesForCentroid.get(newEntry.getInSection());
            if (entryForSection != null && entryForSection.getSequenceNumber() > newEntry.getSequenceNumber()) {
                return false;
            }
        } else {
            // Create Section-> Entry Map
            entriesForCentroid = new HashMap<>();
        }

        entriesForCentroid.put(newEntry.getInSection(), newEntry);
        centroidLinkCosts.put(newEntry.getDestinationCentroid(), entriesForCentroid);

        return true;
    }
}
