package de.dfg.oc.otc.region;

import de.dfg.oc.otc.layer1.observer.Attribute;
import de.dfg.oc.otc.layer1.observer.monitoring.StatisticsCapabilities;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.TrafficType;
import de.dfg.oc.otc.manager.aimsun.Turning;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * OTCNode variant used to add Regional Manger intelligence.
 *
 * @author tomforde
 */
public class OTCNodeRegion extends OTCNodeSynchronized {
    private static final Logger log = Logger.getLogger(OTCNodeRegion.class);
    /**
     * Representation of this node needed by RegionalManager.
     */
    private NodeDataStructure nodeDataStructure;

    public OTCNodeRegion(final AimsunJunction junction, final Attribute attribute) {
        super(junction, attribute);
        log.info("New regional node (ID " + getId() + ")");
    }

    /**
     * Method used to generate the representation of this node.
     */
    private void generateRepresentation() {
        this.nodeDataStructure = new NodeDataStructure(this.getId());

        for (Turning turning : getJunction().getTurnings(TrafficType.INDIVIDUAL_TRAFFIC)) {
            final Section inSection = turning.getInSection();
            final Section outSection = turning.getOutSection();

            List<Section> path = inSection.determineCompletePathToPreviousJunction(new ArrayList<>());

            OTCNode predecessor = findPredeccessorNode(path);
            OTCNode successor = findSuccessorNode(outSection);

            final float weight = getLayer1Observer().getTurningStatistic(TrafficType.INDIVIDUAL_TRAFFIC,
                    getEvaluationInterval(), turning.getId(), StatisticsCapabilities.FLOW);

            this.nodeDataStructure.addEntryUsingTurnings(predecessor, successor, weight, inSection.getId(), outSection.getId());
        }
    }

    private OTCNode findSuccessorNode(Section destSection) {
        Section lastSection = null;
        List<Section> path = destSection.determineCompletePathToNextJunction(new ArrayList<>());
        if (!path.isEmpty()) {
            lastSection = path.get(path.size() - 1);
        }

        OTCNode successor = null;
        if (lastSection != null && lastSection.getNextJunction() != null) {
            successor = lastSection.getNextJunction().getNode();
        }
        return successor;
    }

    private OTCNode findPredeccessorNode(List<Section> path) {
        Section lastSection = null;
        if (!path.isEmpty()) {
            lastSection = path.get(path.size() - 1);
        }
        OTCNode predecessor = null;
        if (lastSection != null && lastSection.getPreviousJunction() != null) {
            predecessor = lastSection.getPreviousJunction().getNode();
        }
        return predecessor;
    }

    /**
     * Method used to select the representation of this node.
     *
     * @return Representation of this node
     */
    final NodeDataStructure getCurrentNodeRepresentation() {
        generateRepresentation();
        return nodeDataStructure;
    }

    /**
     * Method used to create the neighbourhood.
     */
    private void initialiseNeighbours() {
        getJunction().getNeighbouringNodes();
    }

    /**
     * This method is called by the RegMan after calculating the PSS centrally.
     * All needed information is delivered to establish the PSS.
     *
     * @param id   The identifier of this OTCNode
     * @param info The Object containing all needed PSS information
     */
    final void receivePSSInformationFromRegionalManager(final int id, final PSSInfo info) {
        if (info == null || id != getId()) {
            log.error("Node " + getId() + " received invalid information from RegionalManager");
        }

        // Nimmt der Knoten an einer gruenen Welle teil?
        if (info != null && info.isActivePSS()) {
            setPartOfPSS(true);
            setBeginOfPSS(info.isStart());
            setEndOfPSS(info.isEnd());
            setPredecessorIsSecondChoice(false);
            setConfirmedPredecessor(true);

            if (!isBeginOfPSS()) {
                setPredecessor(info.getPreds().get(0));
            } else {
                setPredecessor(null);
            }

            if (!isEndOfPSS()) {
                setPrimarySuccessor(info.getSuccs().get(0));
            } else {
                setPrimarySuccessor(null);
            }
        } else {
            setPartOfPSS(false);
            setBeginOfPSS(false);
            setEndOfPSS(false);
            setPredecessorIsSecondChoice(false);
            setConfirmedPredecessor(false);
            setPredecessor(null);
            setPrimarySuccessor(null);
        }

        log.info("RM delivered PSS info for node " + getId());
    }

    /**
     * Method used to register this node at the RegionalManager.
     */
    final void registerAtRegionalManager() {
        initialiseNeighbours();
        RegionalManager.getInstance().registerNode(this);
    }
}
