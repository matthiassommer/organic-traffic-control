package de.dfg.oc.otc.routing.distanceVector;

import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.routing.ProtocolType;
import de.dfg.oc.otc.routing.RoutingComponent;
import de.dfg.oc.otc.routing.RoutingTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

/**
 * Based on the distance vector routing protocol. Uses temporal data with
 * respect to current traffic flows as well as forecasts to determine the best
 * routes through the network to prominent destinations.
 * Proactive route guidance protocol.
 *
 * Too many messages and sub-calls. Simulation does not run...
 *
 * @author Matthias Sommer
 */
public class TemporalDistanceVectorRC extends RoutingComponent {
    public TemporalDistanceVectorRC(final ProtocolType type, final OTCNode node) {
        super(type, node);
    }

    /**
     * Forward the request to not visited neighbours.
     *
     * @param senders nodes on the path
     * @param path    the path from the sender to myself
     * @param delay   the time to take the path in seconds
     */
    private void forwardRequest(final List<Integer> senders, final List<Integer> path, final float delay) {
        final int lastInSection = path.get(path.size() - 1);

        // Forward request to further not visited neighbours
        for (Entry<Integer, RoutingComponent> entry : this.neighbourRCs.entrySet()) {
            final int junctionID = entry.getKey();
            float bestCost = Float.MAX_VALUE;
            int bestSection = -1;

            // Find outSection from this rc to the neighbourRC
            for (int outSection : this.neighbourToOutSections.get(junctionID)) {
                // avoid cycles
                if (!path.contains(outSection)) {
                    final float turningCosts = getTurningForecast(delay, lastInSection, outSection);
                    if (areCostsValid(turningCosts)) {
                        final float sectionCosts = getSectionForecast(delay + turningCosts, outSection, junctionID, true);
                        final boolean fasterPath = turningCosts + sectionCosts < bestCost;

                        if (areCostsValid(sectionCosts) && fasterPath) {
                            bestCost = turningCosts + sectionCosts;
                            bestSection = outSection;
                        }
                    }
                }
            }

            if (bestSection != -1) {
                final float totalCost = delay + bestCost;

                List<Integer> copySenders = new ArrayList<>(senders);
                copySenders.add(this.getNodeID());

                List<Integer> copyPath = new ArrayList<>(path);
                copyPath.add(bestSection);

                increaseCommunicationCounterByOne();
                ((TemporalDistanceVectorRC) entry.getValue()).request(copySenders, copyPath, totalCost);
            }
        }

    }

    /**
     * Send requests to all neighbours for all my incoming sections to get the
     * delays from myself to all reachable {@link de.dfg.oc.otc.manager.aimsun.Centroid}s in the network.
     * <p>
     * The total number of sent requests per protocol run is the number of
     * neighbour nodes multiplied with the number of incoming sections.
     */
    private void initRequest() {
        for (Section inSection : this.getJunction().getInSections()) {
            for (Entry<Integer, RoutingComponent> entry : this.neighbourRCs.entrySet()) {
                final int junctionID = entry.getKey();
                final int outSection = this.neighbourToOutSections.get(junctionID).get(0);

                final float turningCost = determineTurningCost(inSection.getId(), outSection);
                if (areCostsValid(turningCost)) {
                    float sectionCosts = getDynamicDelayForSection(outSection);
                    if (!areCostsValid(sectionCosts)) {
                        // Add static costs from the section to the calling neighbour to the delay
                        sectionCosts = costToNeighbour.get(junctionID);
                    }

                    List<Integer> senders = new ArrayList<>();
                    senders.add(this.getNodeID());

                    List<Integer> path = new ArrayList<>();
                    path.add(inSection.getId());
                    path.add(outSection);

                    increaseCommunicationCounterByOne();
                    ((TemporalDistanceVectorRC) entry.getValue()).request(senders, path, sectionCosts + turningCost);
                }
            }
        }
    }

    @Override
    public void performProtocol() {
        // Find direct connected centroids
        processLocalDestinations();

        // 1. Initialise requests to neighbour junctions
        initRequest();

        // receiver gets request

        // if receiver has direct connected centroids:
        // 2. he calculates the cost to each centroid from the sender to the
        // centroid
        // receiver sends a reply([senders], [pathToCentroid], centroidID,
        // costs) to sender
        // goto 4

        // 3. Node receives reply
        // if he is the origin sender of the request (first entry in the
        // nodeIDList of the request):
        // adds new entry to routing table if he has no entry to centroid
        // or costs for new path are lower than previous entry

        // if he is NOT the origin sender of the request:
        // send reply to next previous node (goto 3)
        // UNTIL: origin sender of request is reached

        // if receiver has NO direct connected centroids:
        // 4. he sends the received request forward to all neighbours
        // list of receiverIDs, path and cost of request have to be updated
        // goto 2

        // UNTIL: request is passed forward until there is no neighbour which is
        // not in the nodeID List of the request
    }

    /**
     * Receiver replies to a request sending back a message containing a
     * {@link de.dfg.oc.otc.manager.aimsun.Centroid} as well as the path from the sender to the
     * {@link de.dfg.oc.otc.manager.aimsun.Centroid} and the costs for the path.
     * <p>
     * If sender is found, he updates his RoutingTable, otherwise reply is send
     * to next node in the list of senders.
     *
     * @param senders  list of senders
     * @param path     the path from a incoming section of the origin sender to the
     *                 {@link de.dfg.oc.otc.manager.aimsun.Centroid}
     * @param centroid the found {@link de.dfg.oc.otc.manager.aimsun.Centroid}
     * @param cost     cost from the origin sender to the {@link de.dfg.oc.otc.manager.aimsun.Centroid} in seconds
     */
    private void reply(final List<Integer> senders, final List<Integer> path, final int centroid,
                       final float cost) {
        // Origin sender found, update routing table
        if (senders.isEmpty()) {
            final int inSection = path.get(0);
            final int outSection = path.get(1);
            RoutingTable table = this.inSectionToRoutingTable.get(inSection);

            if (table == null) {
                RoutingTable newTable = new RoutingTable(inSection);
                this.inSectionToRoutingTable.put(inSection, newTable);
                table = newTable;
            }

            table.updateRoutingData(centroid, outSection, cost);
        } else {
            increaseCommunicationCounterByOne();

            int junctionID = senders.remove(senders.size() - 1);
            final TemporalDistanceVectorRC nextSender = (TemporalDistanceVectorRC) neighbourRCs.get(junctionID);
            nextSender.reply(senders, path, centroid, cost);
        }
    }

    /**
     * Send replies to the sender for each neighbouring destination from the
     * current node. Skips if the node has no centroids.
     *
     * @param senders        node on the path
     * @param path           from the origin sender to myself
     * @param delay          the time to take the path in seconds
     * @param circleDetected
     */
    private void findTargetsAndReply(final List<Integer> senders, final List<Integer> path,
                                     final float delay, boolean circleDetected) {
        final int lastInSection = path.get(path.size() - 1);

        for (Entry<Integer, Integer> entry : this.outSectionDestinations.entrySet()) {
            final int outSection = entry.getKey();
            final float turningCost = getTurningForecast(delay, lastInSection, outSection);

            if (areCostsValid(turningCost)) {
                final int centroidID = entry.getValue();
                final float linkCost = getSectionForecast(delay + turningCost, outSection, centroidID, false);
                final float totalCost = delay + turningCost + linkCost;

                // If sender is direct neighbour, send directly to him
                final int startID = senders.get(0);
                if (this.neighbourRCs.containsValue(startID)) {
                    senders.clear();
                    senders.add(startID);
                    reply(senders, path, centroidID, totalCost);
                } else if (circleDetected) {
                    reply(Collections.emptyList(), path, centroidID, totalCost);
                } else {
                    reply(new ArrayList<>(senders), path, centroidID, totalCost);
                }
            }
        }
    }

    /**
     * This method is called by neighbouring nodes.
     *
     * @param senders sorted list of previous senders of this request. First entry
     *                is origin sender.
     * @param path    list of section IDs which form the path from the origin sender
     *                to the current receiver.
     * @param delay   the costs from the origin sender to this node in seconds
     */
    private void request(final List<Integer> senders, final List<Integer> path, final float delay) {
        final int sendingNodeID = senders.get(0);
        final boolean reachedSendingNode = sendingNodeID == this.getNodeID();

        findTargetsAndReply(senders, path, delay, reachedSendingNode);

        if (!reachedSendingNode) {
            forwardRequest(senders, path, delay);
        }
    }
}
