package de.dfg.oc.otc.routing.distanceVector;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.Turning;
import de.dfg.oc.otc.routing.ProtocolType;
import de.dfg.oc.otc.routing.RoutingComponent;
import de.dfg.oc.otc.routing.RoutingTable;

import java.util.*;

/**
 * Extension of the temporal distance vector routing protocol by by grouping
 * nodes into different regions and applying ideas from the
 * Border-Gateway-Protocol for communication between different regions, and
 * Interior-Regional-Protocol for communication within the same region.
 * <p>
 * Therefore reduces the overall number of sent messages in the network.
 *
 * @author Matthias Sommer
 */
public class RegionalTDVRRC extends RegionalDistanceVectorRC {
    private final ForecastUtilities forecasts;

    public RegionalTDVRRC(ProtocolType type, OTCNode node) {
        super(type, node);
        this.forecasts = new ForecastUtilities(node);
    }

    private void calculateCostsAndNotifyNeighbours(final int destinationID,
                                                   List<Integer> path,
                                                   List<ForecastUtilities.SectionForecastMessage> sectionMessages,
                                                   List<ForecastUtilities.TurningForecastMessage> turningMessages,
                                                   final int callingNodeID, final boolean isRegionalInformation) {
        int outSectionID = path.get(path.size() - 1);

        Collection<RoutingTable> routingTables = this.inSectionToRoutingTable.values();
        if (isRegionalInformation) {
            routingTables = regionalRoutingTables.values();
        }

        for (RoutingTable routingTable : routingTables) {
            final int inSectionID = routingTable.getInSectionID();

            Turning turning = this.getJunction().getTurning(inSectionID, outSectionID);
            if (turning != null) {
                List<Integer> copyPath = new ArrayList<>(path);
                copyPath.add(inSectionID);

                float pathTraveltime = forecasts.calculateTravelTimeForPath(copyPath, sectionMessages, turningMessages);

                final boolean updatePerformed = routingTable.updateRoutingData(destinationID, outSectionID, pathTraveltime);
                if (updatePerformed) {
                    informFurtherRCsAboutTableUpdate(inSectionID, copyPath, sectionMessages, turningMessages, destinationID, isRegionalInformation);
                }
            }
        }
    }

    private void informFurtherRCsAboutTableUpdate(int inSectionID, List<Integer> path,
                                                  List<ForecastUtilities.SectionForecastMessage> sectionMessages,
                                                  List<ForecastUtilities.TurningForecastMessage> turningMessages,
                                                  int destinationID, boolean isRegionalInformation) {
        List<Integer> idList = getPredeccesorsForInSection(inSectionID);

        if (!isRegionalInformation) {
            for (RoutingComponent borderNeighbour : borderNeighbours) {
                final int neighbourID = borderNeighbour.getNodeID();
                idList.remove(new Integer(neighbourID));
            }
        }

        if (!idList.isEmpty()) {
            informNeighbours(idList, path, sectionMessages, turningMessages, destinationID, isRegionalInformation);
        }
    }

    private void notifyNeighbours(final int destinationID, List<Integer> path,
                                  List<ForecastUtilities.SectionForecastMessage> sectionMessages,
                                  List<ForecastUtilities.TurningForecastMessage> turningMessages,
                                  final int callingNodeID, final boolean isRegionalInformation) {
        increaseCommunicationCounterByOne();

        if (!isRegionalInformation) {
            calculateCostsAndNotifyNeighbours(destinationID, path, sectionMessages, turningMessages, callingNodeID, false);
        } else if (destinationID != getRegionId()) {
            calculateCostsAndNotifyNeighbours(destinationID, path, sectionMessages, turningMessages, callingNodeID, true);
        }
    }

    private void distributeDestinations(final Set<RegionalTDVRRC> neighbours,
                                        final Map<Integer, RoutingTable> routingTables, final boolean isRegionalInformation) {
        for (RegionalTDVRRC neighbour : neighbours) {
            final int nodeID = neighbour.getNodeID();

            // Determine the InComingSection for each of these nodes
            List<Integer> incomingSectionsIDs = neighbourToInSections.get(nodeID);
            for (Integer insectionID : incomingSectionsIDs) {
                RoutingTable routingTable = routingTables.get(insectionID);
                Set<Integer> destinations = routingTable.getDestinationIDs();

                // only notify neighbours if destination is in the same region
                destinations.stream().filter(routingTable::isDestinationInSameRegion).forEach(destinationID -> {
                    Section outsection = forecasts.findOutsection(insectionID, destinationID);

                    List<ForecastUtilities.TurningForecastMessage> turningMessages = new ArrayList<>();
                    ForecastUtilities.TurningForecastMessage turningMessage = forecasts.generateTurningForecasts(insectionID, outsection.getId());
                    turningMessages.add(turningMessage);

                    List<ForecastUtilities.SectionForecastMessage> sectionMessages = new ArrayList<>();
                    ForecastUtilities.SectionForecastMessage outsectionForecast = forecasts.generateSectionForecasts(outsection);
                    sectionMessages.add(outsectionForecast);

                    Section insection = OTCManager.getInstance().getNetwork().getSection(insectionID);
                    ForecastUtilities.SectionForecastMessage insectionForecast = forecasts.generateSectionForecasts(insection);
                    sectionMessages.add(insectionForecast);

                    List<Integer> path = new ArrayList<>();
                    path.add(outsection.getId());
                    path.add(insectionID);

                    neighbour.notifyNeighbours(destinationID, path, sectionMessages, turningMessages, getNodeID(), isRegionalInformation);
                });
            }
        }
    }

    private void informNeighbours(final List<Integer> neighboursToBeInformed, List<Integer> path,
                                  List<ForecastUtilities.SectionForecastMessage> sectionMessages,
                                  List<ForecastUtilities.TurningForecastMessage> turningMessages,
                                  final int targetId, final boolean isRegionalInformation) {
        neighboursToBeInformed.stream().filter(neighbourRCs::containsKey).forEach(predecessor -> {
            // notify predecessor
            RegionalTDVRRC rc = (RegionalTDVRRC) neighbourRCs.get(predecessor);
            rc.notifyNeighbours(targetId, path, sectionMessages, turningMessages, getNodeID(), isRegionalInformation);
        });
    }
}
