package de.dfg.oc.otc.routing.distanceVector;

import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.routing.ProtocolType;
import de.dfg.oc.otc.routing.RoutingComponent;
import de.dfg.oc.otc.routing.RoutingTable;

import java.util.List;
import java.util.Set;

/**
 * Class implements the DistanceVector protocol for decentralised routing in
 * urban road networks. Has to know its neighbours and its local delays.
 * Communication is done with direct neighbours (global delays).
 *
 * @author tomforde
 */
public class DistanceVectorRC extends RoutingComponent {
    public DistanceVectorRC(final ProtocolType type, final OTCNode node) {
        super(type, node);
    }

    /**
     * Method used to notify a set of neighbours reachable by incoming sections
     * about a reachable destination or a corresponding update.
     *
     * @param neighboursToInform list of predecessing neighbours to send new routing informations
     * @param cost               delay in seconds from the calling node to the destination
     * @param target             centroid id
     */
    private void informNeighbours(final Iterable<Integer> neighboursToInform, final float cost, final int target) {
        for (Integer junctionID : neighboursToInform) {
            if (neighbourRCs.containsKey(junctionID)) {
                DistanceVectorRC dvr = (DistanceVectorRC) neighbourRCs.get(junctionID);
                dvr.notify(getNodeID(), cost, target);
            }
        }
    }

    /**
     * Communication method for notifying the local neighbours about new
     * reachable destinations or cost updates.
     *
     * @param callingNode id of the neighbouring node who called this method on this
     *                    node
     * @param delay       costs to a centroid from the calling node
     * @param targetID    id of the destination (centroid)
     */
    private void notify(final int callingNode, final float delay, final int targetID) {
        increaseCommunicationCounterByOne();

        // Hole die zugehoerige outSection fuer den aufrufenden Knoten
        final List<Integer> outSectionsToNeighbour = neighbourToOutSections.get(callingNode);
        if (outSectionsToNeighbour != null) {
            for (int outSectionID : outSectionsToNeighbour) {
                float sectionCosts = getDynamicDelayForSection(outSectionID);
                if (!areCostsValid(sectionCosts)) {
                    sectionCosts = costToNeighbour.get(callingNode);
                }

                // Find RoutingTables, leading to this section
                for (RoutingTable routingTable : this.inSectionToRoutingTable.values()) {
                    final int inSectionID = routingTable.getInSectionID();
                    float turningCosts = determineTurningCost(inSectionID, outSectionID);
                    float totalDelay = turningCosts + sectionCosts + delay;

                    if (areCostsValid(turningCosts)) {
                        // Add values to the RoutingTable
                        final boolean updatePerformed = routingTable.updateRoutingData(targetID, outSectionID, totalDelay);
                        // If routing table was updated, notify neighbours.
                        if (updatePerformed) {
                            // Alle Vorgaenger der InComingSection der RoutingTable
                            List<Integer> junctionIDs = getPredeccesorsForInSection(inSectionID);
                            if (!junctionIDs.isEmpty()) {
                                informNeighbours(junctionIDs, totalDelay, targetID);
                            }
                        }
                    }

                }
            }
        } else {
            log.warn("RC of node " + getNodeID() + " has been notified by an unknown neighbour!");
        }
    }

    @Override
    public void performProtocol() {
        // Lokal erreichbare Ziele aktualisieren
        processLocalDestinations();

        for (RoutingComponent rc : neighbourRCs.values()) {
            final DistanceVectorRC neighbour = (DistanceVectorRC) rc;

            // incoming section fuer den Nachbarknoten
            final List<Integer> incomingSectionIDs = neighbourToInSections.get(neighbour.getNodeID());
            for (Integer insectionID : incomingSectionIDs) {
                // RoutingTable of the incoming section
                final RoutingTable routingTable = this.inSectionToRoutingTable.get(insectionID);
                final Set<Integer> centroidIDs = routingTable.getDestinationIDs();

                for (Integer centroid : centroidIDs) {
                    boolean isLocalTarget = routingTable.isDestinationInSameRegion(centroid);
                    if (isLocalTarget) {
                        neighbour.notify(getNodeID(), routingTable.getDelayForTarget(centroid), centroid);
                    }
                }
            }
        }
    }
}
