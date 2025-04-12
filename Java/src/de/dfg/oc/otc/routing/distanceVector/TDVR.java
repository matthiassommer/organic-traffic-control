package de.dfg.oc.otc.routing.distanceVector;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.Turning;
import de.dfg.oc.otc.routing.ProtocolType;
import de.dfg.oc.otc.routing.RoutingComponent;
import de.dfg.oc.otc.routing.RoutingTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Extends the DVR protocol: uses forecasts of traffic flows two cover the time-dependant changes in the dynamic traffic conditions.
 *
 * @author Matthias Sommer.
 */
public class TDVR extends RoutingComponent {
    private final ForecastUtilities forecasts;

    public TDVR(final ProtocolType type, final OTCNode node) {
        super(type, node);
        this.forecasts = new ForecastUtilities(node);
    }

    /**
     * Method used to notify a set of neighbours reachable by incoming sections
     * about a reachable destination or a corresponding update.
     *
     * @param neighboursToInform list of neighbours to send new routing informations
     * @param destinationID      centroid id
     */
    private void informNeighbours(Iterable<Integer> neighboursToInform, List<ForecastUtilities.SectionForecastMessage> sectionMessages,
                                  List<ForecastUtilities.TurningForecastMessage> turningMessages, List<Integer> path, int destinationID) {
        for (Integer junctionID : neighboursToInform) {
            if (neighbourRCs.containsKey(junctionID)) {
                TDVR rc = (TDVR) neighbourRCs.get(junctionID);
                increaseCommunicationCounterByOne();
                rc.notify(path, sectionMessages, turningMessages, destinationID);
            }
        }
    }

    /**
     * Communication method for notifying the local neighbours about new
     * reachable destinations or cost updates.
     *
     * @param path
     * @param sectionMessages
     * @param turningMessages
     * @param destinationID   id of the destination (centroid)
     */
    private void notify(List<Integer> path, List<ForecastUtilities.SectionForecastMessage> sectionMessages,
                        List<ForecastUtilities.TurningForecastMessage> turningMessages, int destinationID) {
        int outsectionID = path.get(path.size() - 1);

        // Find RoutingTables, leading to this section
        for (RoutingTable routingTable : this.inSectionToRoutingTable.values()) {
            final int insectionID = routingTable.getInSectionID();

            // Check if inSection has a turning to the outSection
            Turning turning = this.getJunction().getTurning(insectionID, outsectionID);
            if (turning != null) {
                List<Integer> copyPath = new ArrayList<>(path);
                copyPath.add(insectionID);

                Section insection = OTCManager.getInstance().getNetwork().getSection(insectionID);
                ForecastUtilities.SectionForecastMessage insectionForecasts = forecasts.generateSectionForecasts(insection);
                sectionMessages.add(insectionForecasts);

                ForecastUtilities.TurningForecastMessage turningForecasts = forecasts.generateTurningForecasts(insectionID, outsectionID);
                turningMessages.add(turningForecasts);

                float pathTraveltime = forecasts.calculateTravelTimeForPath(copyPath, sectionMessages, turningMessages);

                boolean updated = routingTable.updateRoutingData(destinationID, outsectionID, pathTraveltime);
                if (updated) {
                    List<Integer> junctionIDs = getPredeccesorsForInSection(insectionID);
                    if (!junctionIDs.isEmpty()) {
                        informNeighbours(junctionIDs, sectionMessages, turningMessages, copyPath, destinationID);
                    }
                }
            }
        }
    }

    @Override
    public void performProtocol() {
        processLocalDestinations();

        for (RoutingComponent rc : neighbourRCs.values()) {
            TDVR neighbour = (TDVR) rc;
            final List<Integer> insectionIDs = neighbourToInSections.get(neighbour.getNodeID());
            for (Integer insectionID : insectionIDs) {
                RoutingTable routingTable = this.inSectionToRoutingTable.get(insectionID);

                for (int destinationID : routingTable.getDestinationIDs()) {
                    boolean isLocalDestination = routingTable.isDestinationInSameRegion(destinationID);
                    if (isLocalDestination) {
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

                        neighbour.notify(path, sectionMessages, turningMessages, destinationID);
                    }
                }
            }
        }
    }
}
