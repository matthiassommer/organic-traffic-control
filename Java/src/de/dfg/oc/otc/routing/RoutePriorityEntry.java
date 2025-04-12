package de.dfg.oc.otc.routing;

/**
 * Wrapper class for {@link RouteEntry} saving total costs and implements
 * {@link Comparable}.
 *
 * @author Johannes
 */
public class RoutePriorityEntry extends RouteEntry implements Comparable<RoutePriorityEntry> {
    protected float costs;

    protected RoutePriorityEntry(){
    }

    public RoutePriorityEntry(final RouteEntry entry, final float costs) {
        this.inSection = entry.inSection;
        this.outSection = entry.outSection;
        this.sourceRC = entry.sourceRC;
        this.destinationCentroid = entry.destinationCentroid;
        this.destinationRC = entry.destinationRC;
        super.costs = entry.costs;

        this.costs = costs;
    }

    /**
     * Compare to graph entries by comparing their costs.
     *
     * @param entry graph entry to compare this one to
     * @return compare value
     */
    public final int compareTo(final RoutePriorityEntry entry) {
        if (this.costs < entry.costs) {
            return -1;
        } else if (this.costs > entry.costs) {
            return 1;
        }
        return 0;
    }

    public final float getCosts() {
        return costs;
    }
}
