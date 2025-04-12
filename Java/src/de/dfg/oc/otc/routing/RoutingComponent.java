package de.dfg.oc.otc.routing;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer1.observer.monitoring.StatisticsCapabilities;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.*;
import de.dfg.oc.otc.aid.disturbance.Disturbance;
import de.dfg.oc.otc.aid.disturbance.DisturbanceManager;
import org.apache.commons.math3.util.FastMath;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Abstract representation of a routing entity. Contains and manages the
 * {@link RoutingTable}.
 *
 * @author tomforde
 */
public abstract class RoutingComponent {
    protected static final Logger log = Logger.getLogger(RoutingComponent.class);
    /**
     * Counter for the number of sent messages.
     */
    private static int communicationCounter;
    /**
     * All Routing-Tables for the {@link OTCNode} mapped with the id of the
     * incoming {@link Section}s of this node. Section-ID-->RoutingTable
     */
    protected final Map<Integer, RoutingTable> inSectionToRoutingTable;
    /**
     * Mapping junctionID with the RoutingComponents of the direct neighbours
     * performing the same protocol. junctionID-->RC
     */
    protected final Map<Integer, RoutingComponent> neighbourRCs;
    /**
     * This component is responsible for routing at the following node.
     */
    private final OTCNode node;
    /**
     * Delay calculation based on (1) Webster or (2) statistics.
     */
    private final int delayCalculationMethod;
    /**
     * Which protocol is used for routing?
     */
    private final ProtocolType protocolType;
    /**
     * Mapping of static offsets from myself to my neighbouring {@link Centroid}s.
     */
    public Map<Integer, Offset> localDestinationOffsets;
    /**
     * Stores static costs for links from myself to my neighbouring {@link OTCNode}s
     * (How long does it approx. take to go there?). Node-ID/Cost - mapping.
     * The delay for the whole path from this junction to a neighboring one.
     */
    public final Map<Integer, Integer> costToNeighbour;
    /**
     * Neighbouring Junction-ID -> incoming sections-ID mapping. Which sections leads from
     * my neighbour to myself. (DVR)
     */
    protected Map<Integer, List<Integer>> neighbourToInSections;
    /**
     * Junction-ID -> outgoing sections-ID mapping. Which section leads from
     * myself to my neighbouring junctions. (DVR)
     */
    protected Map<Integer, List<Integer>> neighbourToOutSections;
    /**
     * {@link Section}-ID -> Centroid-ID Mapping. Which {@link Section} leads to
     * my neighbouring centroids.
     */
    protected Map<Integer, Integer> outSectionDestinations;
    /**
     * Regional type of this routing component.
     */
    private RegionalType regionalType = RegionalType.INTERIOR;
    /**
     * The region ID of the associated junction.
     */
    private Integer regionId;

    protected RoutingComponent(final ProtocolType type, final OTCNode node) {
        this.inSectionToRoutingTable = new HashMap<>(5);
        this.protocolType = type;
        this.node = node;
        this.neighbourRCs = new HashMap<>(5);
        this.costToNeighbour = new HashMap<>(5);
        this.delayCalculationMethod = DefaultParams.ROUTING_DELAY_CALCULATION;

        initialiseRoutingTables();
        initialiseLocalDestinations();
    }

    /**
     * Calculate disturbances to the estimated delay for a {@link Section}.
     *
     * @param delay     without disturbances
     * @param sectionID the {@link Section}
     * @return delay with disturbances
     */
    private float addDisturbancesToDelay(float delay, final int sectionID) {
        final List<Disturbance> disturbances = DisturbanceManager.getInstance().getDisturbancesForLink(sectionID);
        for (Disturbance disturbance : disturbances) {
            delay = delay * (1 + disturbance.getDegree());
        }
        return delay;
    }

    public Collection<RoutingComponent> getNeighbourRCs() {
        return neighbourRCs.values();
    }

    /**
     * Get a forecast for a {@link Section} for a certain time in the future.
     *
     * @param time       time to get to the {@link Section}
     * @param outSection id of outgoing {@link Section}
     * @param targetID   id of next target ({@link OTCNode} or {@link de.dfg.oc.otc.manager.aimsun.Centroid})
     * @param isNode     is target a ({@link OTCNode} or {@link de.dfg.oc.otc.manager.aimsun.Centroid}).
     *                   <true>if targetID belongs to a node</true>
     * @return forecast for {@link Section} delay
     */
    protected final float getSectionForecast(final float time, final int outSection, final int targetID, final boolean isNode) {
        final List<Section> sections = getJunction().getOutSections();

        for (Section section : sections) {
            if (section.getId() == outSection) {
                int steps = Math.round(time / (section.getFlowForecaster().getForecastHorizon()));
                final float forecast = section.getFlowForecaster().getForecast(steps);
                if (areCostsValid(forecast)) {
                    float delay = section.calculateDelay(forecast);
                    return addDisturbancesToDelay(delay, section.getId());
                } else {
                    if (isNode) {
                        float delay = getDynamicDelayForSection(outSection);
                        if (!areCostsValid(delay)) {
                            // Add static costs from the section to the calling neighbour to the delay
                            return costToNeighbour.get(targetID);
                        }
                        return delay;
                    } else {
                        // Target is Centroid
                        return this.localDestinationOffsets.get(targetID).getOffset();
                    }
                }
            }
        }

        return Float.NaN;
    }

    /**
     * Check if costs for a {@link Turning} are valid.
     * Checks if the ingoing section can serve an outgoing section (if a turning exists).
     *
     * @param cost for the {@link Turning}
     * @return {@code true} if costs are valid, {@code false}
     * otherwise
     */
    protected final boolean areCostsValid(final float cost) {
        return !(Float.isNaN(cost) || cost <= 0);
    }

    /**
     * Calculate the delay with webster for a specific turning and a given
     * traffic flow.
     *
     * @param flow    of traffic in veh/h
     * @param turning
     * @return delay for {@link Turning}
     */
    private float calculateDelayForTurning(final Turning turning, final float flow) {
        final AimsunJunction junction = getJunction();

        final SignalGroup signalGroup = junction.getSignalGroupForTurning(turning);
        // Effective green time for turning
        final double tf = junction.getGreenTimeForSignalGroup(signalGroup);
        final double tu = junction.getActiveTLC().getCycleTime();
        // Number of available lanes for the turning
        final double lanes = junction.getNbOfLanesForSignalGroup(signalGroup);

        return websterAvgDelayPerTurn(flow, tf, tu, lanes);
    }

    /**
     * Method used to choose the best offset (shortest travel time) out of a given set of offsets
     * (ignores invalid values) from the origin to the destination node.
     *
     * @param offsets
     * @param origin      starting node
     * @param destination end node
     * @return lowest delay
     */
    private int chooseBestOffset(final Iterable<Offset> offsets, final OTCNode origin, final OTCNode destination) {
        int bestDelay = Integer.MAX_VALUE;

        // Search for best offset
        for (Offset offset : offsets) {
            if (offset.getOriginJunction() == origin.getJunction()
                    && offset.getDestinationJunction() == destination.getJunction()) {
                final int delay = offset.getOffset();
                if (delay < bestDelay) {
                    bestDelay = delay;
                }
            }
        }
        return bestDelay;
    }

    /**
     * Method determines the total number of entries/destinations in all {@link RoutingTable}.
     *
     * @return number of entries
     */
    private int determineEntries() {
        int sum = 0;
        for (RoutingTable routingTable : this.inSectionToRoutingTable.values()) {
            sum += routingTable.getDestinationIDs().size();
        }
        return sum;
    }

    /**
     * Method collects {@link RoutingComponent}s from all known direct neighbors
     * and stores them. Those neighbours, who are performing a different routing
     * protocol, are ignored.
     */
    protected final void determineNeighbourRCs() {
        Map<Integer, OTCNode> neighbourNodes = getJunction().getNeighbouringNodes();
        for (OTCNode neighbour : neighbourNodes.values()) {
            RoutingComponent rc = neighbour.getRoutingComponent();

            if (rc != null) {
                if (rc.protocolType == this.protocolType) {
                    // Add known neighbour
                    neighbourRCs.put(rc.getNodeID(), rc);
                }
            } else {
                log.warn("Neighbor has no RoutingComponent!");
            }
        }
    }

    /**
     * Determines the cost (avg. delay) for a {@link Turning} (specified by its
     * in- and outbound section) using Websters formula. Used by the routing
     * algorithms.
     *
     * @param inSectionID  inbound section of the {@link Turning}
     * @param outSectionID outbound section of the {@link Turning}
     * @return estimated delay in seconds (or {@code -1} if no
     * {@link Turning} exists)
     */
    protected final float determineTurningCost(final int inSectionID, final int outSectionID) {
        // -1, if no turning exists
        float tw;

        final AimsunJunction junction = getJunction();
        final Turning turning = junction.getTurning(inSectionID, outSectionID);
        if (turning == null) {
            return -1;
        }

        if (delayCalculationMethod == 1) {
            // Use Webster
            // TODO More efficient update!
            node.updateTurnToFlowMap();
            final SignalGroup signalGroup = junction.getSignalGroupForTurning(turning);
            float flow = node.getFlowForSignalGroup(signalGroup);
            if (Float.isNaN(flow)) {
                return -1;
            }

            return calculateDelayForTurning(turning, flow);
        } else {
            // Use statistics
            return node.getL1StatObserver().getAverageValue(turning.getId(), StatisticsCapabilities.DELAYTIME,
                    node.getEvaluationInterval());
        }
    }

    /**
     * Get a forecast for a {@link Turning} for a certain time in the future.
     *
     * @param seconds    time to get to the {@link Turning}
     * @param inSection  incoming {@link Section} of the {@link Turning}
     * @param outSection outgoing {@link Section} of the {@link Turning}
     * @return forecast for delay of the {@link Turning}
     */
    public final float getTurningForecast(final float seconds, final int inSection, final int outSection) {
        if (seconds == 0) {
            return determineTurningCost(inSection, outSection);
        }

        final Turning turning = getJunction().getTurning(inSection, outSection);
        if (turning == null) {
            return -1;
        }

        int steps = Math.round(seconds / (turning.getFlowForecaster().getForecastHorizon()));
        final float forecast = turning.getFlowForecaster().getForecast(steps);
        if (areCostsValid(forecast)) {
            return calculateDelayForTurning(turning, forecast);
        }
        return determineTurningCost(inSection, outSection);
    }

    /**
     * Get a forecast for a {@link Section} for a certain time in the future or the static travel costs.
     *
     * @param time     time to get to the {@link de.dfg.oc.otc.manager.aimsun.Section}
     * @param section  id of outgoing {@link de.dfg.oc.otc.manager.aimsun.Section}
     * @param targetID id of next target ({@link de.dfg.oc.otc.manager.OTCNode} or {@link de.dfg.oc.otc.manager.aimsun.Centroid})
     * @param isNode   is target a ({@link de.dfg.oc.otc.manager.OTCNode} or {@link de.dfg.oc.otc.manager.aimsun.Centroid}). <true>if targetID belongs to a node</true>
     * @return forecast for {@link Section} delay
     */
    public float getSectionDelay(final float time, final Section section, final int targetID, final boolean isNode) {
        int steps = Math.round(time / (section.getFlowForecaster().getForecastHorizon()));
        final float forecast = section.getFlowForecaster().getForecast(steps);
        if (areCostsValid(forecast)) {
            float delay = section.calculateDelay(forecast);
            return addDisturbancesToDelay(delay, section.getId());
        } else {
            if (isNode) {
                // Target is Node
                float linkCost = getDynamicDelayForSection(section.getId());
                if (Float.isNaN(linkCost)) {
                    // Add static costs from the section to the calling neighbour to the delay
                    return costToNeighbour.get(targetID);
                }
                return linkCost;
            } else {
                // Target is Centroid
                return localDestinationOffsets.get(targetID).getOffset();
            }
        }
    }

    /**
     * Calculate dynamic delay based on the current flow etc. in seconds.
     *
     * @return delay for section
     */
    protected float getDynamicDelayForSection(int sectionID) {
        for (Section section : getJunction().getOutSections()) {
            if (section.getId() == sectionID) {
                // target is centroid or junction
                if (section.isJunctionApproach() || section.isExit()) {
                    // Multiply Disturbance-degree to delay
                    float delay = section.calculateDelay(section.getFlow());
                    return addDisturbancesToDelay(delay, section.getId());
                }
            }
        }

        for (Section section : getJunction().getInSections()) {
            if (section.getId() == sectionID) {
                // target is centroid or junction
                if (section.isJunctionApproach() || section.isExit()) {
                    // Multiply Disturbance-degree to delay
                    float delay = section.calculateDelay(section.getFlow());
                    return addDisturbancesToDelay(delay, section.getId());
                }
            }
        }

        return Float.NaN;
    }

    /**
     * Method used to estimate the time a car needs to cover the distance
     * between this junction and a neighbouring junction.
     * <p>
     * Calculation does not consider dynamic travel times depending on density and capacity of the path!
     */
    protected void calculateOffsetToNeighbor() {
        for (Map.Entry<Integer, RoutingComponent> entry : neighbourRCs.entrySet()) {
            List<Offset> offsets = getJunction().getOffsetForNode(entry.getKey());

            final int offset = chooseBestOffset(offsets, node, entry.getValue().node);
            if (offset > 0) {
                this.costToNeighbour.put(entry.getKey(), offset);
            }
        }
    }

    public final int getCommunicationCounter() {
        return communicationCounter;
    }

    public final int getDelayCalculationMethod() {
        return delayCalculationMethod;
    }

    /**
     * Returns all IDs for available inSections.
     *
     * @return A set of integers representing the In-Section IDs
     */
    public final Set<Integer> getInSectionIDs() {
        return this.inSectionToRoutingTable.keySet();
    }

    /**
     * Method used to determine sending neighbours (their IDs) for a given
     * incoming {@link Section} (ID).
     *
     * @param section the incoming {@link Section}
     * @return id list of neighbours
     */
    protected final List<Integer> getPredeccesorsForInSection(final Integer section) {
        final List<Integer> neighbours = new ArrayList<>();

        for (Map.Entry<Integer, List<Integer>> entry : neighbourToInSections.entrySet()) {
            final List<Integer> sections = entry.getValue();
            if (sections.contains(section)) {
                neighbours.add(entry.getKey());
            }
        }
        return neighbours;
    }

    public final AimsunJunction getJunction() {
        return node.getJunction();
    }

    public final int getNodeID() {
        return node.getId();
    }

    public final Map<Integer, Integer> getOutSectionDestinations() {
        return outSectionDestinations;
    }

    public final ProtocolType getProtocolType() {
        return protocolType;
    }

    public final RegionalType getRegionalRCType() {
        return this.regionalType;
    }

    /**
     * Returns the regionId of a specified node. If the node doesn't know its
     * regionId yet, the RegionalHelper is used to complete its information.
     *
     * @return regionId of that node
     */
    public final Integer getRegionId() {
        if (this.regionId == null) {
            this.regionId = RegionalHelper.getInstance().getJunctionRegionMapping().get(getNodeID());

            if (this.regionId == null) {
                log.error("RegionId for junction " + getNodeID() + " not available!");
            }
        }
        return this.regionId;
    }

    /**
     * Returns a representation of the {@link RoutingTable} to be transferred to
     * the API.
     *
     * @return {@link RoutingTable}
     */
    public final int[][] getRoutingTable() {
        if (this.inSectionToRoutingTable.isEmpty()) {
            return new int[0][0];
        }

        final int entriesCount = determineEntries();
        int j = 0;
        final int[][] result = new int[entriesCount][3];

        for (RoutingTable table : this.inSectionToRoutingTable.values()) {
            int[][] entries = table.generateRepresentation();
            for (int[] entry : entries) {
                result[j] = entry;
                j++;
            }
        }

        return result;
    }

    /**
     * Returns complete RoutingTable for the underlying node.
     *
     * @param sectionID the incoming {@link Section}
     * @return {@link RoutingTable}
     */
    public final RoutingTable getRoutingTableForSection(final int sectionID) {
        return this.inSectionToRoutingTable.get(sectionID);
    }

    protected final void increaseCommunicationCounterByOne() {
        communicationCounter++;
    }

    /**
     * Method determines mapping from neighbours to {@link Section}s.
     */
    protected final void initialiseSectionMappings() {
        initialiseNeighBourToInSectionMapping(getJunction().getInSections());
        initialiseNeighBourToOutSectionMapping(getJunction().getOutSections());
    }

    /**
     * Iteriere ueber alle outSections und speicher das Mapping von Knoten zu Section
     */
    private void initialiseNeighBourToOutSectionMapping(final Iterable<Section> outSections) {
        this.neighbourToOutSections = new HashMap<>(8);

        for (Section outSection : outSections) {
            // Nachfolger bestimmen
            List<Integer> neighbourIDs = outSection.determineReceivingNodes(new ArrayList<>(),
                    new ArrayList<>());

            for (Integer nodeId : neighbourIDs) {
                // Check, ob Centroid oder invalid (-1)
                if (nodeId > 0) {
                    // Hole Dir die eventuell bestehende Liste
                    List<Integer> list = neighbourToOutSections.get(nodeId);
                    if (list == null) {
                        list = new ArrayList<>();
                        neighbourToOutSections.put(nodeId, list);
                    }

                    // Neue section speichern
                    if (!list.contains(outSection.getId())) {
                        list.add(outSection.getId());
                        neighbourToOutSections.put(nodeId, list);
                    }
                }
            }
        }
    }

    /**
     * Iteriere ueber alle inSections und speicher das Mapping von Knoten zu Section
     */
    private void initialiseNeighBourToInSectionMapping(final Iterable<Section> inSections) {
        this.neighbourToInSections = new HashMap<>(8);

        for (Section inSection : inSections) {
            // Vorgaenger
            List<Integer> neighbourIDs = inSection.determineSendingNodes(new ArrayList<>(),
                    new ArrayList<>());

            for (Integer nodeID : neighbourIDs) {
                // Check, ob Centroid oder invalid
                if (nodeID > 0) {
                    List<Integer> list = neighbourToInSections.get(nodeID);
                    if (list == null) {
                        list = new ArrayList<>();
                        neighbourToInSections.put(nodeID, list);
                    }

                    // Neue section speichern
                    if (!list.contains(inSection.getId())) {
                        list.add(inSection.getId());
                        neighbourToInSections.put(nodeID, list);
                    }
                }
            }
        }
    }

    /**
     * Update costs to connected centroids.
     * <p>
     * Die RoutingKomponente muss die lokal erreichbaren Ziele (Centroids)
     * kennen und in die RoutingTabellen eintragen. Dazu sind a) die Pfadkosten
     * (Offset) zum Ziel und b) die Kosten fuer das zugehoerige Turning
     * notwendig.
     * <p>
     * Lokale Ziele werden initial in RoutingComponent initialisiert.
     */
    protected final void processLocalDestinations() {
        for (Map.Entry<Integer, Integer> entry : outSectionDestinations.entrySet()) {
            final Integer centroidID = entry.getValue();
            final int outSection = entry.getKey();

            // Kosten des Links zum Centroid bestimmen
            // 0 entspricht: Kein gueltiger Offset zum Ziel -> Ignorieren
            float linkCost = 0;
            if (localDestinationOffsets.containsKey(centroidID) && localDestinationOffsets.get(centroidID).getOffset() > 0) {
                // Valid Offset to target
                linkCost = localDestinationOffsets.get(centroidID).getOffset();
            }

            for (RoutingTable table : this.inSectionToRoutingTable.values()) {
                float turningCost = determineTurningCost(table.getInSectionID(), outSection);
                // turnings to centroid exists
                if (areCostsValid(turningCost)) {
                    // Add new entry to RoutingTable, new local target
                    table.insertRoutingData(centroidID, outSection, turningCost + linkCost);
                }
            }
        }
    }

    /**
     * Initialise mappings for local destinations.
     */
    protected final void initialiseLocalDestinations() {
        this.localDestinationOffsets = new HashMap<>(5);
        this.outSectionDestinations = new HashMap<>(5);

        final Set<Integer> localCentroidIDs = getJunction().getLocalCentroidIDs();

        // Dies ist mein aktuelles (lokales) Ziel
        for (Integer centroidID : localCentroidIDs) {
            final Offset offset = getJunction().getOffsetForCentroid(centroidID);

            if (offset != null && !offset.isDestinationJunction()) {
                // Bestimme die zugehoerige OutSection
                final int outSectionID = offset.getPath().get(0).getId();

                this.outSectionDestinations.put(outSectionID, centroidID);
                this.localDestinationOffsets.put(centroidID, offset);
            } else {
                log.warn("Shouldn't be here: Received invalid offset object from junction!");
            }
        }
    }

    /**
     * Method builds {@link RoutingTable}s for all incoming {@link Section}s.
     */
    private void initialiseRoutingTables() {
        final List<Section> inSections = getJunction().getInSections();
        inSections.forEach(this::addRoutingTableForSection);
    }

    private void addRoutingTableForSection(Section section) {
        this.inSectionToRoutingTable.putIfAbsent(section.getId(), new RoutingTable(section.getId()));
    }

    /**
     * Main routine for routing - to be called by {@link RoutingManager}.
     */
    public abstract void performProtocol();

    /**
     * PART 1 of the protocol's run routine: Reset all protocol tables before
     * performing the particular protocol logic.
     */
    public void resetRoutingData() {
        this.inSectionToRoutingTable.values().forEach(RoutingTable::reset);

        // Nachbarschaft bekannt?
        if (this.neighbourRCs.isEmpty()) {
            // Initialiseren
            determineNeighbourRCs();
            initialiseSectionMappings();
            calculateOffsetToNeighbor();
            initialiseLocalDestinations();
        }
    }

    protected final void setRegionalType(final RegionalType regionalType) {
        this.regionalType = regionalType;
    }

    /**
     * Calculates the average delay for a turning according to Webster's
     * formula.
     *
     * @param M     current flow (veh/h)
     * @param tf    effective green time for turning
     * @param tu    cycle time
     * @param lanes number of available lanes for the turning
     * @return average delay (in sec)
     */
    private float websterAvgDelayPerTurn(double M, final double tf, final double tu, final double lanes) {
        // Avoid division by zero
        if (M == 0) {
            M = 1;
        }

        // Saturation flow
        final double S = lanes * 1800;
        // TODO Different saturation flows for straight, left, and right turns?

        // Anteil der effektiven Gr�nzeit am Umlauf
        final double f = tf / tu;
        // Saturation degree
        double g = M / (f * S);
        // Prerequisite: g < 1
        if (g >= 1) {
            g = .99;
        }

        final double denominator1 = tu * FastMath.pow(1 - f, 2);
        final double numerator1 = 2 * (1 - M / S);
        final double summand1 = denominator1 / numerator1;

        final double denominator2 = 1800 * FastMath.pow(g, 2);
        final double numerator2 = M * (1 - g);
        final double summand2 = denominator2 / numerator2;

        final double onehundredPercent = summand1 + summand2;
        return (float) (.9 * onehundredPercent);
    }

    /**
     * A border node is at the border of two regions. An interior node has only
     * neighbouring nodes within the same region.
     */
    public enum RegionalType {
        BORDER, INTERIOR
    }
}
