package de.dfg.oc.otc.routing.distanceVector;

import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.Turning;
import de.dfg.oc.otc.routing.ProtocolType;
import de.dfg.oc.otc.routing.RoutingComponent;
import de.dfg.oc.otc.routing.RoutingTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NOT FINISHED. Just a test
 *
 * @author Matthias Sommer.
 */
public class TemporalDynamicSourceRC extends RoutingComponent {
    /**
     * Stores all paths (sequence of section ids) to each reachable centroid from this node.
     * First entry of a path is an ingoing section of this node, last is outgoing section to centroid.
     */
    private final Map<Integer, List<List<Integer>>> centroidPathMapping = new HashMap<>();

    public TemporalDynamicSourceRC(ProtocolType type, OTCNode node) {
        super(type, node);
    }

    @Override
    public void performProtocol() {
        // find local centroids
        processLocalDestinations();

        // init calculation messages for costs to centroids
        initUpdateForPathCostsToCentroid();
    }

    /**
     * Inform all neighbors about own destinations.
     */
    public void initSearchForCentroid() {
        for (Map.Entry<Integer, Integer> entry : this.outSectionDestinations.entrySet()) {
            List<Integer> path = new ArrayList<>();
            path.add(entry.getKey());
            informNeighborsAboutCentroid(entry.getValue(), path);
        }
    }

    /**
     * Add new centroid to routing table if it was unknown.
     * Only forward if centroid was unknown.
     * Costs are unknown for the first request of each certain centroid of an interation.
     */
    private void informNeighborsAboutCentroid(int centroidID, List<Integer> path) {
        for (RoutingComponent neighbour : neighbourRCs.values()) {
            TemporalDynamicSourceRC rc = (TemporalDynamicSourceRC) neighbour;

            List<Integer> incomingSectionIDs = neighbourToInSections.get(neighbour.getNodeID());
            if (!incomingSectionIDs.isEmpty() && !path.contains(incomingSectionIDs.get(0))) {
                rc.receiveCentroidMessage(centroidID, path, incomingSectionIDs.get(0));
            }
        }
    }

    /**
     * Receive message about centroid from neighbouring nodes.
     * Store new entry if centroid was unknown and forward the new information to other neighbors.
     * Else, do nothing.
     *
     * @param centroidID
     * @param path
     * @param outSectionID
     */
    private void receiveCentroidMessage(int centroidID, List<Integer> path, Integer outSectionID) {
        for (Map.Entry<Integer, RoutingTable> entry : this.inSectionToRoutingTable.entrySet()) {
            RoutingTable routingTable = entry.getValue();
            int insectionID = entry.getKey();

            final Turning turning = getJunction().getTurning(insectionID, outSectionID);
            // forward if centroid was unknown
            if (turning != null && !routingTable.isDestinationInSameRegion(centroidID)) {
                // forward
                List<Integer> copyPath = new ArrayList<>(path);
                copyPath.add(0, outSectionID);
                informNeighborsAboutCentroid(centroidID, copyPath);

                // store new received path
                addNewCentroidEntryInRoutingTable(outSectionID, centroidID);

                List<Integer> copyPath2 = new ArrayList<>(copyPath);
                copyPath2.add(0, insectionID);
                List<List<Integer>> paths = centroidPathMapping.get(centroidID);
                paths.add(copyPath2);
                centroidPathMapping.put(centroidID, paths);
            }
        }
    }


    /**
     * Add a new entry for a found reachable centroid.
     */
    private void addNewCentroidEntryInRoutingTable(int outSectionID, int centroidId) {
        for (RoutingTable routingTable : this.inSectionToRoutingTable.values()) {
            routingTable.insertRoutingData(centroidId, outSectionID, Float.MAX_VALUE);
        }
    }

    /**
     * Send a request to calculate the current costs for a known route from myself to a centroid.
     */
    private void initUpdateForPathCostsToCentroid() {
        for (Map.Entry<Integer, List<List<Integer>>> entry : this.centroidPathMapping.entrySet()) {
            int centroidID = entry.getKey();

            for (List<Integer> path : entry.getValue()) {
                int insectionID = path.get(0);
                float bestKnownCosts = this.inSectionToRoutingTable.get(insectionID).getDelayForTarget(centroidID);

                float turningCosts = determineTurningCost(insectionID, path.get(1));
                float sectionCosts = getDynamicDelayForSection(path.get(1));
                float costs = turningCosts + sectionCosts;

                if (costs < bestKnownCosts) {
                    forwardRequestToNextNode(1, path, centroidID, bestKnownCosts, costs);
                }
            }
        }
    }

    /**
     * Forward reply until requesting node is reached.
     */
    private void forwardRequestToNextNode(int position, List<Integer> path, int centroidID, float bestCosts, float currentCosts) {
        if (position == path.size() - 1 && currentCosts < bestCosts) {
            // reached last node, return costs to sender
            returnToSender(path.size() - 1, path, centroidID, currentCosts);
        }

        int insectionID = path.get(position);
        int outsectionID = path.get(position + 1);

        // finde knoten object
        for (RoutingComponent neighbour : neighbourRCs.values()) {
            TemporalDynamicSourceRC rc = (TemporalDynamicSourceRC) neighbour;

            List<Integer> incomingSectionIDs = neighbourToInSections.get(neighbour.getNodeID());
            if (incomingSectionIDs.contains(insectionID)) {
                // berechne kosten bzw forecast für turning und section
                float turningCosts = getTurningForecast(currentCosts, insectionID, outsectionID);
                float sectionCosts = getSectionForecast(currentCosts + turningCosts, outsectionID, neighbour.getNodeID(), true);

                currentCosts += turningCosts + sectionCosts;

                if (currentCosts < bestCosts) {
                    rc.forwardRequestToNextNode(position + 1, path, centroidID, bestCosts, currentCosts);
                }
                break;
            }
        }
    }

    /**
     * Centroid reached and costs are lower then the best known ones.
     * Send reply to requesting node backwards along the path.
     */
    private void returnToSender(int position, List<Integer> path, int centroidID, float newCosts) {
        if (position == 1) {
            updateRoutingTableForReply(path, centroidID, newCosts);
        }

        int insectionID = path.get(position);
        for (RoutingComponent neighbour : neighbourRCs.values()) {
            TemporalDynamicSourceRC rc = (TemporalDynamicSourceRC) neighbour;

            List<Integer> incomingSectionIDs = neighbourToInSections.get(neighbour.getNodeID());
            if (incomingSectionIDs.contains(insectionID)) {
                rc.returnToSender(position - 1, path, centroidID, newCosts);
            }
        }
    }

    /**
     * Update routing table with new lowest cost.
     *
     * @param path
     * @param newCosts
     */
    private void updateRoutingTableForReply(List<Integer> path, int centroidID, float newCosts) {
        RoutingTable table = this.inSectionToRoutingTable.get(path.get(0));
        table.updateRoutingData(centroidID, path.get(1), newCosts);
    }
}
