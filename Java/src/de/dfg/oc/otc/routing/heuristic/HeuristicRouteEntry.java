package de.dfg.oc.otc.routing.heuristic;

import de.dfg.oc.otc.manager.aimsun.Centroid;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.routing.RouteEntry;
import de.dfg.oc.otc.routing.RoutingComponent;

/**
 * Keeps the information of the state of a link and the number of routes this
 * entry is used for. Including cost regarding the specific {@link de.dfg.oc.otc.manager.aimsun.Turning}, the In- and
 * OutSection, the source {@link de.dfg.oc.otc.routing.RoutingComponent} and the destination, either a
 * {@link de.dfg.oc.otc.routing.RoutingComponent} or {@link Centroid}
 *
 * @author lyda
 */
public class HeuristicRouteEntry extends RouteEntry {
    /**
     * Equal to costs for this route.
     */
    private int routesCount;

    HeuristicRouteEntry(final RoutingComponent sourceRC, final RoutingComponent destRC,
                        final Centroid destCentroid, final Section inSection, final Section outSection, final float costs) {
        this.sourceRC = sourceRC;
        this.destinationRC = destRC;
        this.destinationCentroid = destCentroid;
        this.inSection = inSection;
        this.outSection = outSection;
        this.costs = costs;
    }

    final int getRoutesCount() {
        return routesCount;
    }

    final void incrementRoutesCount() {
        routesCount++;
    }

    final void resetRoutesCount() {
        routesCount = 0;
    }
}
