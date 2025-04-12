package de.dfg.oc.otc.routing.distanceVector;

import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.routing.ProtocolType;
import de.dfg.oc.otc.routing.RegionalHelper;
import de.dfg.oc.otc.routing.RoutingComponent;
import de.dfg.oc.otc.routing.RoutingTable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class implements the regional distance vector routing protocol.
 * It extends the standard implementation of the DVR protocol based on ideas from the Border-Gateway-Protocol.
 */
public class RegionalDistanceVectorRC extends DistanceVectorRC {
    /**
     * Neighbouring RCs with the another region id.
     */
    final Set<RegionalDistanceVectorRC> borderNeighbours;
    /**
     * Neighbouring RCs with the same region id.
     */
    private final Set<RegionalDistanceVectorRC> interiorNeighbours;
    /**
     * Mapping of inSection of a junction --> regional Routingtable.
     */
    final Map<Integer, RoutingTable> regionalRoutingTables;

    public RegionalDistanceVectorRC(final ProtocolType type, final OTCNode node) {
        super(type, node);

        this.interiorNeighbours = new HashSet<>();
        this.borderNeighbours = new HashSet<>();
        this.regionalRoutingTables = new HashMap<>();

        initialiseRegionalRoutingTables();
    }

    /**
     * Communication method for notifying the local interior neighbours using
     * the DistanceVector protocol.
     *
     * @param destinationID         centroid or region id of the target
     * @param delayToDestination    delay till here till the target
     * @param callingNodeID         id of the neighbour which is the source of information
     * @param isRegionalInformation flag to decide to treat as interior or regional distribution
     */
    private void calculateCostsAndNotifyNeighbours(final int destinationID, final float delayToDestination,
                                                   final int callingNodeID, final boolean isRegionalInformation) {
        // Get all belonging sections which go to the callingNodeID
        final List<Integer> sectionsToTarget = neighbourToOutSections.get(callingNodeID);
        for (int outSectionID : sectionsToTarget) {
            // Get the RoutingTables, for each section leading to the neighbour
            // -> add the current delay of the according turning
            // Run over RoutingTables depending on regional or interior routing information
            Collection<RoutingTable> routingTables = this.inSectionToRoutingTable.values();
            if (isRegionalInformation) {
                routingTables = regionalRoutingTables.values();
            }

            // Add the costs of the Link to this neighbour to the delay
            float totalDelay = costToNeighbour.get(callingNodeID) + delayToDestination;
            for (RoutingTable routingTable : routingTables) {
                final int inSectionID = routingTable.getInSectionID();

                final float turningCost = determineTurningCost(inSectionID, outSectionID);
                if (areCostsValid(turningCost)) {
                    totalDelay += turningCost;

                    final boolean updatePerformed = routingTable.updateRoutingData(destinationID, outSectionID, totalDelay);
                    if (updatePerformed) {
                        informFurtherRCsAboutTableUpdate(inSectionID, totalDelay, destinationID, isRegionalInformation);
                    }
                }
            }
        }
    }

    private void informFurtherRCsAboutTableUpdate(int inSectionID, float delayToDestination, int destinationID, boolean isRegionalInformation) {
        List<Integer> idList = getPredeccesorsForInSection(inSectionID);

        if (!isRegionalInformation) {
            for (RoutingComponent borderNeighbour : borderNeighbours) {
                final int neighbourID = borderNeighbour.getNodeID();
                idList.remove(new Integer(neighbourID));
            }
        }

        if (!idList.isEmpty()) {
            informNeighbours(idList, delayToDestination, destinationID, isRegionalInformation);
        }
    }

    /**
     * Sorts neighbours into interior and border candidates by checking their IDs.
     */
    public final void determineRegionalRCType() {
        for (Map.Entry<Integer, RoutingComponent> neighbour : neighbourRCs.entrySet()) {
            RoutingComponent rc = neighbour.getValue();
            final Integer regionID = rc.getRegionId();
            final Integer ownRegionID = getRegionId();

            if (ownRegionID.equals(regionID)) {
                interiorNeighbours.add((RegionalDistanceVectorRC) rc);
            } else {
                borderNeighbours.add((RegionalDistanceVectorRC) rc);
                setRegionalType(RegionalType.BORDER);
            }
        }
    }

    /**
     * Distribute destinations: depending wether it is an interior or a regional
     * destination (local centroid or regional destination) to the RCs neighbours.
     *
     * @param neighbours            to be informed neighbours
     * @param routingTables         belonging RoutingTables
     * @param isRegionalInformation flag to decide to treat as interior or regional distribution
     */
    private void distributeDestinations(final Set<RegionalDistanceVectorRC> neighbours,
                                        final Map<Integer, RoutingTable> routingTables, final boolean isRegionalInformation) {
        for (RegionalDistanceVectorRC neighbour : neighbours) {
            final int nodeID = neighbour.getNodeID();

            // Determine the InComingSection for each of these nodes
            List<Integer> incomingSectionsIDs = neighbourToInSections.get(nodeID);
            for (Integer sectionID : incomingSectionsIDs) {
                RoutingTable routingTable = routingTables.get(sectionID);
                Set<Integer> destinations = routingTable.getDestinationIDs();

                // only notify neighbours if destination is in the same region
                destinations.stream().filter(routingTable::isDestinationInSameRegion).forEach(destinationID ->
                        neighbour.notifyNeighbours(destinationID, routingTable.getDelayForTarget(destinationID),
                                getNodeID(), isRegionalInformation));
            }
        }
    }

    /**
     * Method used to notify a set of neighbours about a reachable destination
     * or a corresponding update.
     *
     * @param neighboursToBeInformed list of neighbours to be informed
     * @param delayToDestination     delay till here till the target
     * @param targetId               centroid or region id of the target
     * @param isRegionalInformation  flag to decide to treat as interior or regional distribution
     */
    private void informNeighbours(final List<Integer> neighboursToBeInformed, final float delayToDestination,
                                  final int targetId, final boolean isRegionalInformation) {
         neighboursToBeInformed.stream().filter(neighbourRCs::containsKey).forEach(predecessor -> {
            // notify predecessor
            RegionalDistanceVectorRC rc = (RegionalDistanceVectorRC) neighbourRCs.get(predecessor);
            rc.notifyNeighbours(targetId, delayToDestination, getNodeID(), isRegionalInformation);
        });
    }

    /**
     * Method builds regional RoutingTables for all incoming sections.
     */
    private void initialiseRegionalRoutingTables() {
        for (Section section : getJunction().getInSections()) {
            regionalRoutingTables.putIfAbsent(section.getId(), new RoutingTable(section.getId()));
        }
    }

    /**
     * This node gets notified by its neighbour about new routing information.
     *
     * @param destinationID         is the target
     * @param delayToDestination    is the delay estimated till the neighbour
     * @param callingNodeID         id of the neighbour which is the source of information
     * @param isRegionalInformation flag to decide to treat as interior or regional distribution
     */
    private void notifyNeighbours(final int destinationID, final float delayToDestination, final int callingNodeID,
                                  final boolean isRegionalInformation) {
        increaseCommunicationCounterByOne();

        if (!isRegionalInformation) {
            // Interior Distance Vector information notification
            calculateCostsAndNotifyNeighbours(destinationID, delayToDestination, callingNodeID, false);
        } else if (destinationID != getRegionId()) {
            // Check if the destination is in own region->ignore
            calculateCostsAndNotifyNeighbours(destinationID, delayToDestination, callingNodeID, true);
        }
    }

    @Override
    public void performProtocol() {
        // Update locally reachable destination
        processLocalDestinations();

        // Distribute these to nodes in interior region
        distributeDestinations(interiorNeighbours, this.inSectionToRoutingTable, false);

        // If this is a border neighbour, tell other border neighbours about
        // connection to this region
        if (getRegionalRCType() == RegionalType.BORDER) {
            // Update locally reachable regions
            processBorderNeighbours();
            // Distribute these to all neighbours and regions
            Set<RegionalDistanceVectorRC> rcs = neighbourRCs.values().stream().map(element -> (RegionalDistanceVectorRC) element).collect(Collectors.toSet());
            distributeDestinations(rcs, this.regionalRoutingTables, true);
        }
    }

    /**
     * Finds the local reachable regions and puts them as local destinations
     * into the regional RoutingTable.
     */
    private void processBorderNeighbours() {
        //TODO korrekt?
        final float linkCost = 0;

        for (RoutingComponent borderNeighbour : borderNeighbours) {
            List<Integer> outSections = neighbourToOutSections.get(borderNeighbour.getNodeID());

            for (RoutingTable regionalTable : regionalRoutingTables.values()) {
                for (Integer outSection : outSections) {
                    float turningCost = determineTurningCost(regionalTable.getInSectionID(), outSection);
                    if (areCostsValid(turningCost)) {
                        float cost = turningCost + linkCost;

                        // Insert information into table. Saves that this is a neighbouring region.
                        regionalTable.updateLocalRoutingData(borderNeighbour.getRegionId(), outSection, cost);
                    }
                }
            }
        }
    }

    @Override
    public final void resetRoutingData() {
        super.resetRoutingData();
        regionalRoutingTables.values().forEach(RoutingTable::reset);
    }

    /**
     * After determining accumulated the next hop for all centroids in a region,
     * the normal RoutingTable has to be filled with the real CentroidIds.
     */
    public final void translateRegionToCentroids() {
        // Run over all regional InSection-> RoutingTables
        for (Map.Entry<Integer, RoutingTable> entry : this.regionalRoutingTables.entrySet()) {
            final Integer inSectionId = entry.getKey();
            final RoutingTable regionalTable = entry.getValue();

            // Take all region destinations
            for (Integer regionId : regionalTable.getDestinationIDs()) {
                // except the own region
                if (!regionId.equals(getRegionId())) {
                    // update interior RoutingTable
                    RoutingTable interiorTable = this.inSectionToRoutingTable.get(inSectionId);
                    final int nextHopID = regionalTable.getNextHopForTarget(regionId);
                    final float delay = regionalTable.getDelayForTarget(regionId);

                    // with each centroid from that region
                    for (Integer centroidId : RegionalHelper.getInstance().getCentroidRegionMapping().get(regionId)) {
                        interiorTable.updateRoutingData(centroidId, nextHopID, delay);
                    }
                }
            }
        }
    }
}
