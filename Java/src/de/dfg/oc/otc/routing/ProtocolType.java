package de.dfg.oc.otc.routing;

import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.routing.distanceVector.*;
import de.dfg.oc.otc.routing.greedy.GreedyRC;
import de.dfg.oc.otc.routing.heuristic.HeuristicRC;
import de.dfg.oc.otc.routing.linkState.LinkStateRC;
import de.dfg.oc.otc.routing.linkState.RegionalLinkStateRC;
import de.dfg.oc.otc.routing.linkState.temporal.RegionalTemporalLSRC;
import de.dfg.oc.otc.routing.linkState.temporal.TemporalLinkStateRC;
import forecasting.DefaultForecastParameters;

/**
 * Enum specifies the different available protocol types.
 */
public enum ProtocolType {
    NONE,
    DVR, REGIONALDVR, TDVR, TEMPORALDVR, REGIONALTDVR,
    LSR, REGIONALLSR, TLSR, REGIONALTLSR,
    GREEDY, HEURISTIC, TEMPORALDS;

    /**
     * Method creates a new instance of a RoutingComponent based on the given
     * ProtocolType.
     *
     * @param node        the intersection
     * @param name        of the routing component
     * @param useForecast
     * @return routing component
     */
    public static RoutingComponent initRoutingComponent(final OTCNode node, final String name, boolean useForecast) {
        ProtocolType protocol = ProtocolType.valueOf(name);
        switch (protocol) {
            case NONE:
                return null;
            case DVR:
                return new DistanceVectorRC(protocol, node);
            case LSR:
                return new LinkStateRC(protocol, node);
            case REGIONALLSR:
                return new RegionalLinkStateRC(protocol, node);
            case GREEDY:
                return new GreedyRC(protocol, node);
            case HEURISTIC:
                return new HeuristicRC(protocol, node);
            case REGIONALDVR:
                return new RegionalDistanceVectorRC(protocol, node);
            case TEMPORALDVR:
                DefaultForecastParameters.IS_FORECAST_MODULE_ACTIVE = true;
                return new TemporalDistanceVectorRC(protocol, node);
            case REGIONALTDVR:
                if (useForecast) {
                    return new RegionalTDVRRC(protocol, node);
                }
                throw new IllegalArgumentException("Activate forecast module!");
            case TLSR:
                DefaultForecastParameters.IS_FORECAST_MODULE_ACTIVE = true;
                return new TemporalLinkStateRC(protocol, node);
            case REGIONALTLSR:
                if (useForecast) {
                    return new RegionalTemporalLSRC(protocol, node);
                }
                throw new IllegalArgumentException("Activate forecast module!");
            case TEMPORALDS:
                DefaultForecastParameters.IS_FORECAST_MODULE_ACTIVE = true;
                return new TemporalDynamicSourceRC(protocol, node);
            case TDVR:
                DefaultForecastParameters.IS_FORECAST_MODULE_ACTIVE = true;
                return new TDVR(protocol, node);
            default:
                throw new IllegalArgumentException("Unknown protocol type");
        }
    }
}
