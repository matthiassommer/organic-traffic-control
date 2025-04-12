package de.dfg.oc.otc.routing;

import de.dfg.oc.otc.manager.aimsun.Section;

import java.util.*;

/**
 * Class defines a RoutingTable to be used for each incoming section of an
 * OTC-controlled junction. Stores an entry for each destination (Centroid or
 * region) containing the next hop (here: outgoing section to the next junction) and the corresponding weight value.
 * <p>
 * Each routing table stands for one dynamic traffic sign. This would be shown
 * on a sign like "For region X turn left, 10 min."
 *
 * @author tomforde
 */
public class RoutingTable extends Observable {
    /**
     * The current aggregated delay times in seconds on the way to the target (Centroid or region).
     * Mapping: Centroid ID / Delay.
     */
    private final Map<Integer, Float> delays;
    /**
     * Incoming {@link Section} of a node for which this RoutingTable is applied.
     */
    private final int inSectionID;
    /**
     * Stores the IDs of directly connected destinations. Used by regional routing protocols.
     */
    private final List<Integer> destinationsInSameRegion;
    /**
     * Mapping of target-IDs (Centroid or region) to the ID of the next hop
     * (here: outgoing section).
     */
    private final Map<Integer, Integer> nextHops;

    /**
     * Constructor.
     *
     * @param sectionID The incoming {@link Section} for which this table is applied
     */
    public RoutingTable(final int sectionID) {
        this.inSectionID = sectionID;
        this.nextHops = new HashMap<>(5);
        this.delays = new HashMap<>(5);
        this.destinationsInSameRegion = new ArrayList<>(5);
    }

    /**
     * Reset all routing information.
     */
    public final void reset() {
        this.nextHops.clear();
        this.delays.clear();
    }

    /**
     * Method generates the API-readable representation of routing information.
     * [incoming section, next hop, target]
     *
     * @return result
     */
    final int[][] generateRepresentation() {
        final int[][] result = new int[nextHops.size()][3];
        final Iterator<Integer> nextHopsIt = nextHops.keySet().iterator();

        for (int i = 0; i < nextHops.size(); i++) {
            final Integer targetID = nextHopsIt.next();
            result[i][0] = inSectionID;
            result[i][1] = nextHops.get(targetID);
            result[i][2] = targetID;
        }

        return result;
    }

    /**
     * Method returns the current delay for a given target.
     *
     * @param targetID id of target
     * @return delay cost to target
     */
    public final float getDelayForTarget(final int targetID) {
        if (delays.containsKey(targetID)) {
            return delays.get(targetID);
        }
        return Float.NaN;
    }

    /**
     * Method used to get IDs of all known destinations (e.g. a Centroid).
     *
     * @return centroid IDs
     */
    public final Set<Integer> getDestinationIDs() {
        return nextHops.keySet();
    }

    public final int getInSectionID() {
        return inSectionID;
    }

    /**
     * Returns next hop (node) for a destination.
     *
     * @param targetID id of destination
     * @return next hop id
     */
    public final int getNextHopForTarget(final int targetID) {
        if (nextHops.containsKey(targetID)) {
            return nextHops.get(targetID);
        }
        return -1;
    }

    /**
     * Send all routing entries of this table to the observers.
     */
    void informObservers() {
        final Iterator<Integer> nextHopsIt = nextHops.keySet().iterator();
        for (int i = 0; i < nextHops.size(); i++) {
            final int targetID = nextHopsIt.next();

            this.setChanged();
            this.notifyObservers(new String[]{String.valueOf(inSectionID), String.valueOf(nextHops.get(targetID)), String.valueOf(targetID), String.valueOf(Math.round(this.delays.get(targetID)))});
        }
    }

    /**
     * Method used to insert a new or replace an existing routing entry,
     * additional info, if destination has direct a link to this node.
     *
     * @param targetID  id of a target, e.g. {@link de.dfg.oc.otc.manager.aimsun.Centroid}
     * @param nextHopID id of the next section
     * @param delay     costs
     */
    public final void insertRoutingData(final int targetID, final int nextHopID, final float delay) {
        if (isDataValid(targetID, nextHopID, delay)) {
            nextHops.put(targetID, nextHopID);
            delays.put(targetID, delay);

            if (!isDestinationInSameRegion(targetID)) {
                destinationsInSameRegion.add(targetID);
            }
        }
    }

    /**
     * Check if sent data is valid.
     *
     * @param targetID  id of a target
     * @param nextHopID id of the next section
     * @param delay     costs
     * @return {@code true} if data is valid, {@code false} otherwise
     */
    private boolean isDataValid(final int targetID, final int nextHopID, final float delay) {
        return targetID > 0 && nextHopID > 0 && delay > 0;
    }

    /**
     * Is the destination with this id in the same region as the node?
     *
     * @param targetID id of target, e.g. {@link de.dfg.oc.otc.manager.aimsun.Centroid}
     * @return {@code true} if target is local, otherwise
     * {@code false}
     */
    public final boolean isDestinationInSameRegion(final int targetID) {
        return destinationsInSameRegion.contains(targetID);
    }

    /**
     * Generates a description of the current {@link RoutingTable} containing
     * all entries.
     *
     * @return a description of the current RoutingTable
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        final String linesep = System.getProperty("line.separator");

        for (Map.Entry<Integer, Integer> integerIntegerEntry : nextHops.entrySet()) {
            sb.append("FROM ").append(inSectionID).append(" TO ").append(integerIntegerEntry.getKey()).append(", NEXT HOP ").append(integerIntegerEntry.getValue()).append(", DELAY ").append(delays.get(integerIntegerEntry.getKey())).append(linesep);
        }

        return sb.toString();
    }

    /**
     * Method used to insert a new or update an existing routing entry,
     * additional info, if destination has direct a link to this node.
     * <p>
     * IMPORTANT: Update is only performed, if the new delay value is less than
     * the old one. If you would like to replace an existing entry (don't care
     * about the previous delays) - use insertLocalRoutingData() instead!
     *
     * @param targetID  id of a target (e.g. Centroid or region)
     * @param nextHopID id of a (outgoing) section
     * @param delay     costs
     * @return {@code true} if routing data was updated, {@code false}
     * otherwise
     */
    public final void updateLocalRoutingData(final int targetID, final int nextHopID, final float delay) {
        if (isDataValid(targetID, nextHopID, delay)) {
            final boolean betterDelay = delays.containsKey(targetID) && delays.get(targetID) > delay;
            final boolean newTarget = !nextHops.containsKey(targetID);

            if (newTarget || betterDelay) {
                this.nextHops.put(targetID, nextHopID);
                this.delays.put(targetID, delay);

                if (!isDestinationInSameRegion(targetID)) {
                    this.destinationsInSameRegion.add(targetID);
                }
            }
        }
    }

    /**
     * Method used to update routing data - To target use nextHop. Link has a
     * current delay.
     * <p>
     * IMPORTANT: Update is only performed, if the new delay value is less than
     * the old one. If you would like to replace an existing entry (don't care
     * about the previous delays) - use insertRoutingData() instead!
     *
     * @param targetID  id of a target (e.g. {@link de.dfg.oc.otc.manager.aimsun.Centroid})
     * @param nextHopID id of a (outgoing) {@link Section}
     * @param delay     costs
     * @return true, if update was necessary, otherwise false
     */
    public final boolean updateRoutingData(final int targetID, final int nextHopID, final float delay) {
        if (isDataValid(targetID, nextHopID, delay)) {
            final boolean betterDelay = delays.containsKey(targetID) && delays.get(targetID) > delay;
            final boolean newTarget = !nextHops.containsKey(targetID);

            if (newTarget || betterDelay) {
                this.nextHops.put(targetID, nextHopID);
                this.delays.put(targetID, delay);
                return true;
            }
        }
        return false;
    }
}
