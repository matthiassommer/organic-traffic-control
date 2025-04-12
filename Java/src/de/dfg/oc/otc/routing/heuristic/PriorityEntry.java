package de.dfg.oc.otc.routing.heuristic;

import de.dfg.oc.otc.routing.RoutePriorityEntry;

/**
 * Wrapper class for {@link HeuristicRouteEntry} saving total costs and implements
 * Comparable sorting total costs and number of routes.
 *
 * @author Johannes
 */
class PriorityEntry extends RoutePriorityEntry {
    private final HeuristicRouteEntry graphEntry;

    PriorityEntry(final HeuristicRouteEntry graphEntry, final float costs) {
        this.graphEntry = graphEntry;
        this.costs = costs;
    }

    /**
     * Compare to graph entries by comparing their costs.
     *
     * @param entry graph entry to compare this one to
     * @return compare value
     */
    protected int compareTo(final PriorityEntry entry) {
        if (this.graphEntry.getRoutesCount() < entry.graphEntry.getRoutesCount()) {
            return -1;
        } else if (this.graphEntry.getRoutesCount() > entry.graphEntry.getRoutesCount()) {
            return 1;
        } else {
            if (this.costs < entry.getCosts()) {
                return -1;
            } else if (this.costs > entry.getCosts()) {
                return 1;
            }
            return 0;
        }
    }

    public HeuristicRouteEntry getGraphEntry() {
        return graphEntry;
    }
}
