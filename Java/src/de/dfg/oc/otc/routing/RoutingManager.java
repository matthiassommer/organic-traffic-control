package de.dfg.oc.otc.routing;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.routing.distanceVector.RegionalDistanceVectorRC;
import de.dfg.oc.otc.routing.distanceVector.RegionalTDVRRC;
import de.dfg.oc.otc.routing.distanceVector.TemporalDynamicSourceRC;
import de.dfg.oc.otc.routing.linkState.RegionalLinkStateRC;
import de.dfg.oc.otc.routing.linkState.temporal.RegionalTemporalLSRC;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The RoutingManager is implemented as a singleton pattern. It serves as
 * INSTANCE for the routing mechanism.
 *
 * @author Matthias Sommer
 */
public final class RoutingManager {
    private static final RoutingManager INSTANCE = new RoutingManager();
    /**
     * Flag if regional DistanceVector or LinkState Routing is active.
     */
    private boolean isRoutingInitialized;
    /**
     * Flag, ob Routing durchgefuehrt werden soll.
     */
    private boolean isRoutingEnabled;
    /**
     * Time step when the protocols should be run the next time.
     */
    private float nextProtocolRun;

    private RoutingManager() {
        initParameters();
    }

    /**
     * RoutingManager is implemented with the singleton pattern.
     *
     * @return instance of RoutingManager
     */
    public static RoutingManager getInstance() {
        return INSTANCE;
    }

    /**
     * Execute the routing component of all nodes for link state routing.
     *
     * @param junctions
     */
    private void executeRegionalLinkStateRouting(final Collection<AimsunJunction> junctions) {
        Collection<RegionalLinkStateRC> rcs = junctions.stream().map(junction -> (RegionalLinkStateRC) junction.getNode().getRoutingComponent()).collect(Collectors.toList());

        if (!isRoutingInitialized) {
            this.isRoutingInitialized = true;

            rcs.stream().forEach(RegionalLinkStateRC::determineRegionalRCType);
            rcs.forEach(RegionalLinkStateRC::askCooperations);
        }

        rcs.stream().forEach(RegionalLinkStateRC::clearLinkstateDatabase);

        // Send interiorAdverts to interior neighbours
        rcs.forEach(RegionalLinkStateRC::distributeInteriorAdvertisements);

        rcs.stream().filter(rc -> rc.getRegionalRCType() == RoutingComponent.RegionalType.BORDER).forEach(RegionalLinkStateRC::computeInteriorDijkstra);
        // Send borderAdverts to border neighbours and exchange interior destinations with border components
        rcs.stream().filter(rc -> rc.getRegionalRCType() == RoutingComponent.RegionalType.BORDER).forEach(RegionalLinkStateRC::distributeBorderAdvertisements);
        rcs.stream().filter(rc -> rc.getRegionalRCType() == RoutingComponent.RegionalType.BORDER).forEach(RegionalLinkStateRC::computeBorderDijkstra);

        // Create new virtual LinkStateDatabase entries in border
        // neighbours and send as interiorAdverts with direct destinations
        rcs.stream().filter(rc -> rc.getRegionalRCType() == RoutingComponent.RegionalType.BORDER).forEach(RegionalLinkStateRC::distributeVirtualInteriorAdverts);

        // Compute all interior components interior Dijkstra
        rcs.stream().filter(rc -> rc.getRegionalRCType() == RoutingComponent.RegionalType.INTERIOR).forEach(RegionalLinkStateRC::computeInteriorDijkstra);
    }

    private void executeRegionalTemporalLinkStateRouting(final Collection<AimsunJunction> junctions) {
        Collection<RegionalTemporalLSRC> rcs = junctions.stream().map(junction -> (RegionalTemporalLSRC) junction.getNode().getRoutingComponent()).collect(Collectors.toList());

        if (!isRoutingInitialized) {
            this.isRoutingInitialized = true;

            rcs.stream().forEach(RegionalTemporalLSRC::determineRegionalRCType);
            rcs.forEach(RegionalTemporalLSRC::askCooperations);
        }

        rcs.stream().forEach(RegionalTemporalLSRC::clearLinkstateDatabase);

        // Send interiorAdverts to interior neighbours
        rcs.forEach(RegionalTemporalLSRC::distributeInteriorAdvertisements);

        rcs.stream().filter(rc -> rc.getRegionalRCType() == RoutingComponent.RegionalType.BORDER).forEach(RegionalTemporalLSRC::computeInteriorDijkstra);
        // Send borderAdverts to border neighbours and exchange interior destinations with border components
        rcs.stream().filter(rc -> rc.getRegionalRCType() == RoutingComponent.RegionalType.BORDER).forEach(RegionalTemporalLSRC::distributeBorderAdvertisements);
        rcs.stream().filter(rc -> rc.getRegionalRCType() == RoutingComponent.RegionalType.BORDER).forEach(RegionalTemporalLSRC::computeBorderDijkstra);

        // Create new virtual LinkStateDatabase entries in border
        // neighbours and send as interiorAdverts with direct destinations
        rcs.stream().filter(rc -> rc.getRegionalRCType() == RoutingComponent.RegionalType.BORDER).forEach(RegionalTemporalLSRC::distributeVirtualInteriorAdverts);

        // Compute all interior components interior Dijkstra
        rcs.stream().filter(rc -> rc.getRegionalRCType() == RoutingComponent.RegionalType.INTERIOR).forEach(RegionalTemporalLSRC::computeInteriorDijkstra);
    }

    /**
     * Set initial parameters for routing configuration.
     */
    private void initParameters() {
        String protocolType = DefaultParams.ROUTING_PROTOCOL;
        if (!protocolType.isEmpty() && !protocolType.equals("NONE")) {
            this.isRoutingEnabled = true;
        }

        // TODO 600 für 10-Min.-Statistiken, abhängig von AIMSUN-Netz Setting
        this.nextProtocolRun = 300 + DefaultParams.ROUTING_INTERVAL;
    }

    public float getNextProtocolRun() {
        return nextProtocolRun;
    }

    /**
     * Method used to determine the routing data by running the active routing
     * protocol.
     *
     * @param time
     */
    public void processRoutingProtocol(final float time) {
        if (!isRoutingEnabled) {
            return;
        }

        final List<AimsunJunction> junctions = OTCManager.getInstance().getNetwork().getControlledJunctions();

        if (time > nextProtocolRun) {
            this.nextProtocolRun = time + DefaultParams.ROUTING_INTERVAL;

            resetRoutingData(junctions);
            initProtocolsAndFillRoutingTables(junctions);
            performRoutingProtocols(junctions);
            mapCentroidsToRegions(junctions);
        }
    }

    /**
     * Prints the routing tables.
     *
     * @return a String representing the routing tables
     */
    private String logRoutingData() {
        final String linesep = System.getProperty("line.separator");
        final StringBuilder log = new StringBuilder();
        log.append("Time: ").append(OTCManager.getInstance().getTime()).append(linesep);

        for (AimsunJunction junction : OTCManager.getInstance().getNetwork().getControlledJunctions()) {
            final RoutingComponent rc = junction.getNode().getRoutingComponent();

            // Sortiertes Logging
            final List<Section> inSections = junction.getInSections();
            log.append("Junction ").append(junction.getId()).append(linesep);

            for (Section section : inSections) {
                RoutingTable routingTable = rc.getRoutingTableForSection(section.getId());
                log.append(routingTable);
            }
        }

        return log.toString();
    }

    /**
     * Map centroids to regions for regional protocols.
     *
     * @param junctions
     */
    private void mapCentroidsToRegions(final List<AimsunJunction> junctions) {
        for (AimsunJunction junction : junctions) {
            RoutingComponent routingComponent = junction.getNode().getRoutingComponent();
            if (routingComponent != null) {
                ProtocolType protocolType = routingComponent.getProtocolType();

                if (protocolType == ProtocolType.REGIONALDVR) {
                    junctions.stream().forEach(junction2 -> {
                        RegionalDistanceVectorRC regionalRC = (RegionalDistanceVectorRC) junction2.getNode()
                                .getRoutingComponent();
                        regionalRC.translateRegionToCentroids();
                    });
                } else if (protocolType == ProtocolType.REGIONALTDVR) {
                    junctions.stream().forEach(junction2 -> {
                        RegionalTDVRRC regionalRC = (RegionalTDVRRC) junction2.getNode().getRoutingComponent();
                        regionalRC.translateRegionToCentroids();
                    });
                }
            }
        }
    }

    private void performRoutingProtocols(final Collection<AimsunJunction> junctions) {
        for (AimsunJunction junction : junctions) {
            RoutingComponent routingComponent = junction.getNode().getRoutingComponent();
            if (routingComponent != null) {
                routingComponent.performProtocol();
            }
        }

        junctions.stream().filter(junction -> junction.getNode().getRoutingComponent() != null).forEach(junction -> {
            RoutingComponent rc = junction.getNode().getRoutingComponent();
            rc.getInSectionIDs().stream().forEach(sectionID -> rc.getRoutingTableForSection(sectionID).informObservers());
        });
    }

    private void initProtocolsAndFillRoutingTables(final Collection<AimsunJunction> junctions) {
        for (AimsunJunction junction : junctions) {
            RoutingComponent routingComponent = junction.getNode().getRoutingComponent();
            if (routingComponent != null) {
                ProtocolType protocolType = routingComponent.getProtocolType();

                if (protocolType == ProtocolType.GREEDY || protocolType == ProtocolType.HEURISTIC) {
                    if (!isRoutingInitialized) {
                        CoordinateHelper.loadCoordinates();
                        break;
                    }
                } else if (protocolType == ProtocolType.REGIONALDVR) {
                    if (!isRoutingInitialized) {
                        ((RegionalDistanceVectorRC) routingComponent).determineRegionalRCType();
                    }
                } else if (protocolType == ProtocolType.REGIONALTDVR) {
                    if (!isRoutingInitialized) {
                        ((RegionalTDVRRC) routingComponent).determineRegionalRCType();
                    }
                } else if (protocolType == ProtocolType.REGIONALLSR) {
                    executeRegionalLinkStateRouting(junctions);
                    return;
                } else if (protocolType == ProtocolType.REGIONALTLSR) {
                    executeRegionalTemporalLinkStateRouting(junctions);
                    return;
                } else if (protocolType == ProtocolType.TEMPORALDS) {
                    TemporalDynamicSourceRC rc = (TemporalDynamicSourceRC) routingComponent;
                    rc.initSearchForCentroid();
                }
            }
        }
        this.isRoutingInitialized = true;
    }

    private void resetRoutingData(final List<AimsunJunction> junctions) {
        junctions.stream().filter(junction -> junction.getNode().getRoutingComponent() != null).forEach(junction -> junction.getNode().getRoutingComponent().resetRoutingData());
    }
}
