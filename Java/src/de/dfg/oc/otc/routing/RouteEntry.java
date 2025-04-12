package de.dfg.oc.otc.routing;

import de.dfg.oc.otc.manager.aimsun.Centroid;
import de.dfg.oc.otc.manager.aimsun.Section;

/**
 * Keeps the information of the state of a route between two routing components.
 * This includes the total costs for the whole route as well as the in- and outgoing {@link Section} of the source node,
 * the source {@link RoutingComponent} itself and the destination, either a {@link RoutingComponent} or {@link Centroid}.
 *
 * @author lyda
 */
public abstract class RouteEntry {
    /**
     * Delay (in sec.) for route from the source to the destination (centroid or RC).
     */
    protected float costs;
    /**
     * Destination centroid of this route. If this is not null, destinationRC has to be null.
     */
    protected Centroid destinationCentroid;
    /**
     * Destination RC of this route. If this is not null, destinationCentroid has to be null.
     */
    protected RoutingComponent destinationRC;
    /**
     * Incoming section of the sourceRC.
     */
    protected Section inSection;
    /**
     * Outgoing section of the sourceRC.
     */
    protected Section outSection;
    /**
     * First routing component of this route.
     */
    protected RoutingComponent sourceRC;

    public float getCosts() {
        return costs;
    }

    public final Centroid getDestinationCentroid() {
        return destinationCentroid;
    }

    public RoutingComponent getDestinationRC() {
        return destinationRC;
    }

    public final Section getInSection() {
        return inSection;
    }

    public final Section getOutSection() {
        return outSection;
    }

    public RoutingComponent getSourceRC() {
        return sourceRC;
    }
}
