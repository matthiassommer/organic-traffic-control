package de.dfg.oc.otc.aid;

import de.dfg.oc.otc.aid.algorithms.AIDAlgorithmFactory;
import de.dfg.oc.otc.aid.algorithms.AbstractAIDAlgorithm;
import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.Link;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.detectors.*;
import de.dfg.oc.otc.manager.gui.AIDComponentPanel;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import java.io.IOException;
import java.util.*;

/**
 * AID component for a junction containing the associated monitoring zones and
 * the incident handler. Has the functionality to build the overlay structure
 * with links and detector pairs.
 *
 * @author Matthias Sommer
 */
public class AIDModule {
    private static final Logger log = Logger.getLogger(AIDModule.class);
    /**
     * Corresponding junction of this component.
     */
    private final AimsunJunction junction;
    /**
     * List of algorithms used in every monitoring zone of the junction.
     */
    private final List<AbstractAIDAlgorithm> algorithms;
    /**
     * List of links which are created for the junction.
     */
    private final List<Link> links;
    /**
     * List of monitoring zones which are managed by the junction.
     */
    private final List<AIDMonitoringZone> monitoringZones;
    /**
     * List of detector pairs which are monitored by the junction.
     */
    private final List<AbstractDetectorGroup> pairs;
    /**
     * List of detector pairs which are received from neighbour junctions.
     */
    private final List<AbstractDetectorGroup> receivedPairsFromNeighbours;
    /**
     * Reference to the gui component which is used as observer for the aid
     * algorithms.
     */
    private AIDComponentPanel gui;
    /**
     * ID of the newest monitoring zone.
     */
    private int nextZoneID = 1;

    public AIDModule(AimsunJunction junction) {
        this.junction = junction;
        this.links = new ArrayList<>();
        this.pairs = new ArrayList<>();
        this.receivedPairsFromNeighbours = new ArrayList<>();
        this.monitoringZones = new ArrayList<>();
        this.algorithms = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "Junction " + this.junction.getId() + " (#MZ = " + this.monitoringZones.size() + ")";
    }

    /**
     * Initializes the component, called by each OTCNode after the network has
     * been initialized.
     */
    public void initialiseComponent() {
        buildLinks();
        buildDetectorPairs(this.links);
        informNeighboursOfPairs();
    }

    /**
     * Reads the required detector capabilities from the configuration file.
     */
    private List<Integer> getRequiredDetectorCapabilities() {
        String detectorCapabilities = DefaultParams.AID_DETECTOR_CAPABILITIES;
        DetectorCapabilities capabilities = new DetectorCapabilities();

        List<Integer> requiredDetectorCapabilities = new ArrayList<>();
        for (String capability : detectorCapabilities.split(" ")) {
            requiredDetectorCapabilities.add(capabilities.getFeatureNumber(capability));
        }
        return requiredDetectorCapabilities;
    }

    /**
     * Creates logical links which are composed of the sections between nodes.
     * Only has to be called once at the beginning of the simulation (even if
     * the availability of detectors changes).
     */
    private void buildLinks() {
        final List<Section> outgoingSections = getOutSectionsFromNode();

        // find EDGE_TO_EDGE networkEntry sections
        for (Section section : this.junction.getOutSections()) {
            findEdgeToEdgeEntrySections(outgoingSections, section);
        }

        Map<Section, List<List<Section>>> forks = buildLinksStartAtOutgoingSections(outgoingSections);
        buildLinksForForkingPaths(forks);
        checkAndNotifyNeighbourJunctionsAboutLinks();
    }

    /**
     * Build detector pairs lying on this link and forking detector groups lying
     * on different links.
     */
    private void buildDetectorPairs(final Iterable<Link> links) {
        // build detector pairs for these links
        for (Link link : links) {
            if (link.hasMonitoredDetectors()) {
                buildDetectorPairs(link);
                buildBranchingDetectorGroups(link);
            }
        }

        // build detector groups lying on different links
        final Collection<AbstractDetectorGroup> markedForRemoval = new ArrayList<>();
        this.pairs.stream().filter(pair -> pair.getType() != AbstractDetectorGroup.Type.SIMPLE_DETECTOR_PAIR).
                forEach(pair -> markedForRemoval.addAll(buildSuperpairs((DetectorForkGroup) pair, this.pairs)));
        this.pairs.removeAll(markedForRemoval);
    }

    /**
     * Notifies all neighbor nodes about the created detector pairs. Therefore
     * they can complete their incomplete divided pairs.
     */
    private void informNeighboursOfPairs() {
        for (OTCNode neighbour : this.junction.getNeighbouringNodes().values()) {
            for (AbstractDetectorGroup pair : this.pairs) {
                try {
                    neighbour.sendDetectorPairToNeighborNode(pair.getClone());
                } catch (CloneNotSupportedException e) {
                    log.error(e);
                }
            }
        }
    }

    /**
     * Find the sections directly outgoing from the junction and the ones coming
     * from a centroid entering the junction.
     *
     * @return list of sections
     */
    private List<Section> getOutSectionsFromNode() {
        // Take junction outgoing sections
        final List<Section> outgoingSections = new ArrayList<>(this.junction.getOutSections());

        // Add centroid outgoing sections entering the junction
        for (Section inSection : this.junction.getInSections()) {
            List<Section> entrySections = new ArrayList<>();
            findEntrySections(entrySections, inSection);

            entrySections.stream().filter(entrySection -> !outgoingSections.contains(entrySection)).forEach(outgoingSections::add);
        }

        return outgoingSections;
    }

    /**
     * Find sections entering the network.
     */
    private void findEntrySections(final List<Section> entrySections, final Section section) {
        // Section belongs to a centroid
        if (section.isEntry()) {
            if (!entrySections.contains(section)) {
                entrySections.add(section);
                // Section belongs to a junction
            }
        } else if (!section.isJunctionExit()) {
            for (Section origin : section.getOriginSections()) {
                findEntrySections(entrySections, origin);
            }
        }
    }

    /**
     * This method is used to add detector pairs to the responsibility of this
     * node. Thereby it is made sure that no detector pairs are added twice.
     */
    private void addDetectorPair(final AbstractDetectorGroup candidatePair) {
        boolean alreadyContained = false;

        for (AbstractDetectorGroup dPair : this.pairs) {
            if (candidatePair.getType() == dPair.getType()) {
                if (candidatePair.isEquallyComposed(dPair)) {
                    alreadyContained = true;
                    break;
                }
            }
        }

        if (!alreadyContained) {
            this.pairs.add(candidatePair);
        }
    }

    /**
     * Through this method detector pairs are received from neighbor nodes.
     */
    public void addReceivedDetectorPair(final AbstractDetectorGroup pair) {
        this.receivedPairsFromNeighbours.add(pair);
    }

    /**
     * This method controls the assignment of algorithms to monitoring zones.
     */
    private AbstractAIDAlgorithm assignMZToAIDAlgorithm(final AIDMonitoringZone monitoringZone) throws AIDException {
        int replication = OTCManager.getInstance().getReplicationID();

        String algo;
        if (10002962 <= replication && replication < 10003002)
        {
            algo = "APIDAutoIncidentDetection";
        }
        else if (10003002 <= replication && replication < 10003042)
        {
            algo = "XCSRURBAN";
        }
        else
        {
            algo = DefaultParams.AID_DEFAULT_ALGORITHM;
        }
        System.out.println("Algorithm is " + algo);

        final AbstractAIDAlgorithm algorithm = AIDAlgorithmFactory.getAlgorithm(algo);

        if (monitoringZone.getMonitoredDetectorPairs().size() < algorithm.getRequiredDetectorPairCount()) {
            throw new AIDException(monitoringZone.getId() + ": not enough detector pairs: " +
                    algorithm.getRequiredDetectorPairCount() + "/" + monitoringZone.getMonitoredDetectorPairs().size());
        }

        algorithm.setNode(this.junction.getNode());
        algorithm.setMonitoringZone(monitoringZone);
        algorithm.addObserver(this.gui);

        algorithm.finalizeInitialization();

        initialiseLogger(algorithm);

        this.nextZoneID++;
        return algorithm;
    }

    /**
     * Builds branching detector groups from a given link.
     */
    private void buildBranchingDetectorGroups(final Link link) {
        final List<Link> associatedLinks = new ArrayList<>();
        associatedLinks.add(link);

        for (Detector[] detectorBranch : link.getDividedDetectorPairs()) {
            int[] streams = link.getInOutStreamsForPair(detectorBranch[0], detectorBranch[1]);
            int instreams = streams[0];
            int outstreams = streams[1];

            DetectorForkGroup dividedPair = new DetectorForkGroup(detectorBranch[0], detectorBranch[1],
                    instreams, outstreams, associatedLinks);
            /*
             * The type ins only temporary, the actual type will be determined
			 * when building the super pairs.
			 */
            dividedPair.setType(AbstractDetectorGroup.Type.DIVIDED_UNMONITORED_BOTH);
            addDetectorPair(dividedPair);
        }
    }

    /**
     * Builds detector pairs out of two nearby detectors for a given link.
     */
    private void buildDetectorPairs(final Link link) {
        for (Detector[] detectorPair : link.getSimpleDetectorPairs()) {
            DetectorPair pair = new DetectorPair(detectorPair[0], detectorPair[1], link);
            pair.setId(String.valueOf(detectorPair[0].getId()) + String.valueOf(detectorPair[1].getId()));
            addDetectorPair(pair);
        }
    }

    /**
     * Recursively finds the alternative paths for all turnings and create
     * links.
     */
    private void buildLinksForForkingPaths(Map<Section, List<List<Section>>> forks) {
        final Map<Section, List<List<Section>>> tempForkingSections = new HashMap<>();

        do {
            for (Map.Entry<Section, List<List<Section>>> sectionListEntry : forks.entrySet()) {
                // All found paths for this forking section
                List<List<Section>> forkingPaths = sectionListEntry.getValue();
                tempForkingSections.clear();

                for (List<Section> forkingPath : forkingPaths) {
                    // Start with index 1 since index 0 was already checked before (see buildLinksStartAtOutgoingSections)
                    for (int i = 1; i < sectionListEntry.getKey().getNumDestinationSections(); i++) {
                        List<Section> completeNewForkingPath = new ArrayList<>(forkingPath);
                        completeNewForkingPath.add(sectionListEntry.getKey());

                        findPathsAndForksForSectionRecursively(sectionListEntry.getKey().getDestinationSections().get(i),
                                completeNewForkingPath, tempForkingSections);

                        Link forkingLink = new Link(AIDUtilities.getInstance().computeID(this.junction.getId()), completeNewForkingPath);
                        if (!this.links.contains(forkingLink)) {
                            this.links.add(forkingLink);
                        }
                    }
                }
            }
            forks = new HashMap<>(tempForkingSections);
        }
        while (!tempForkingSections.isEmpty());
    }

    /**
     * Finds ways to junctions or to the border of the network and builds links
     * out of those.
     */
    private Map<Section, List<List<Section>>> buildLinksStartAtOutgoingSections(final Iterable<Section> nodeOutgoingSections) {
        final Map<Section, List<List<Section>>> forkingSections = new HashMap<>();

        for (Section outgoingSection : nodeOutgoingSections) {
            List<Section> path = findPathsAndForksForSectionRecursively(outgoingSection, new ArrayList<>(), forkingSections);

            Link link = new Link(AIDUtilities.getInstance().computeID(this.junction.getId()), path);
            if (!this.links.contains(link)) {
                this.links.add(link);
            }
        }
        return forkingSections;
    }

    /**
     * Create monitoring zones for the longest link between two detectors.
     */
    private void createMonitoringZonesForPairs() {
        Map<Integer, AIDMonitoringZone> monitoringZones = new HashMap<>();

        for (AbstractDetectorGroup pair : this.pairs) {
            List<Link> longestLinks = findLongestLinksForPair(pair);
            for (Link link : longestLinks) {
                // MZ-creation from previous information
                if (!monitoringZones.containsKey(link.getId())) {
                    AIDMonitoringZone zone = new AIDMonitoringZone(link);

                    if (link.getEndJunction() != null && link.getEndJunction().equals(this.junction)) {
                        zone.setIncoming(true);
                    } else if (link.getStartJunction() != null && link.getStartJunction().equals(this.junction)) {
                        zone.setIncoming(false);
                    }

                    monitoringZones.put(link.getId(), zone);
                    this.monitoringZones.add(zone);
                }

                AIDMonitoringZone zone = monitoringZones.get(link.getId());
                if (!zone.isDetectorPairContained(pair)) {
                    zone.addDetectorPair(pair);
                }
            }
        }
    }

    /**
     * Sort the detector pairs according to their distance and assign an
     * AID-algorithm.
     */
    private void sortDetectorPairsByDistance() {
        Iterator<AIDMonitoringZone> it = this.monitoringZones.iterator();
        while (it.hasNext()) {
            AIDMonitoringZone monitoringZone = it.next();
            try {
                final AbstractAIDAlgorithm algorithm = assignMZToAIDAlgorithm(monitoringZone);
                this.algorithms.add(algorithm);

                monitoringZone.setId(this.nextZoneID - 1);
                monitoringZone.initDetectorPairs();
                monitoringZone.setAIDAlgorithm(algorithm);
            } catch (AIDException e) {
                it.remove();
                log.error(e.getMessage());
            }
        }
    }

    /**
     * Builds the monitoring zones for the node and assigns the AID-algorithms
     * to them.
     */
    private void buildMonitoringZones() {
        createMonitoringZonesForPairs();
        sortDetectorPairsByDistance();

        // Finalize initialization of algorithms, first outgoing and then incoming.
        this.monitoringZones.stream().filter(monitoringZone -> !monitoringZone.isIncoming()).forEach(monitoringZone -> {
            monitoringZone.getAIDAlgorithm().finalizeInitialization();
        });
        this.monitoringZones.stream().filter(AIDMonitoringZone::isIncoming).forEach(monitoringZone -> {
            monitoringZone.getAIDAlgorithm().finalizeInitialization();
        });
    }

    /**
     * Combine different branching detector groups.
     *
     * @return list of detector groups to remove
     */
    private List<AbstractDetectorGroup> buildSuperpairs(final DetectorForkGroup pair, final Iterable<AbstractDetectorGroup> pairs) {
        /*
         * At this point in time there is only one upstream and one downstream
		 * detector.
		 */
        final List<AbstractDetectorGroup> markedForRemoving = new ArrayList<>();

        for (AbstractDetectorGroup pair2 : pairs) {
            if (pair2.getType() != AbstractDetectorGroup.Type.SIMPLE_DETECTOR_PAIR && !pair.isEquallyComposed(pair2)) {
                int upId2 = ((DetectorForkGroup) pair2).getUpstreamDetectors().get(0).getId();
                int downId2 = ((DetectorForkGroup) pair2).getDownstreamDetectors().get(0).getId();
                int upId = pair.getUpstreamDetectors().get(0).getId();
                int downId = pair.getDownstreamDetectors().get(0).getId();

                // Check inputs and outputs
                if (upId2 == upId || downId2 == downId) {
                    pair.mergeWithPair((DetectorForkGroup) pair2);
                    markedForRemoving.add(pair2);
                }

                // Set type of newly created DetectorBranchGroup
                int inSize = pair.getUpstreamDetectors().size() - 1;
                int outSize = pair.getDownstreamDetectors().size() - 1;

                if (inSize < pair.getInStreams() && outSize < pair.getOutStreams()) {
                    pair.setType(AbstractDetectorGroup.Type.DIVIDED_UNMONITORED_BOTH);
                } else if (inSize < pair.getInStreams() && outSize == pair.getOutStreams()) {
                    pair.setType(AbstractDetectorGroup.Type.DIVIDED_MONITORED_OUTPUTS);
                } else if (inSize == pair.getInStreams() && outSize < pair.getOutStreams()) {
                    pair.setType(AbstractDetectorGroup.Type.DIVIDED_MONITORED_INPUTS);
                } else {
                    pair.setType(AbstractDetectorGroup.Type.DIVIDED_MONITORED);
                }
            }
        }

        return markedForRemoving;
    }

    /**
     * Remove links which don't belong to the own responsibility.
     * <p>
     * Notifies neighbor-nodes about which links are outgoing to them so they
     * don't have to search those themselves. (JUNCTION_TO_JUNCTION, start node
     * is the node itself, target node is the neighbor).
     * <p>
     * Note: In this method it is assumed that all other nodes already exist.
     * Therefore the bootstrapping must be done after all nodes have been
     * created.
     */
    private void checkAndNotifyNeighbourJunctionsAboutLinks() {
        final Iterator<Link> it = this.links.iterator();
        while (it.hasNext()) {
            final Link link = it.next();
            if (link.getType() == Link.Type.EDGE_TO_JUNCTION) {
                // Possibility for other responsibility
                if (!this.junction.equals(link.getEndJunction())) {
                    it.remove();
                }
            } else if (link.getType() == Link.Type.JUNCTION_TO_JUNCTION) {
                /*
                 * Notify target nodes about those links (if the target node is
				 * not the current node)
				 */
                final OTCNode targetNode = link.getStartJunction().getNode();
                if (!targetNode.equals(this.junction.getNode())) {
                    targetNode.receiveLinkFromNeighbor(link);
                }
            }
        }
    }

    /**
     * Checks the capabilities of all detectors from a pair.
     */
    private boolean checkDetectorCapabilityConstraint(final AbstractDetectorGroup.Type type, final AbstractDetectorGroup pair) {
        final Collection<Detector> detectors = new ArrayList<>();
        if (type == AbstractDetectorGroup.Type.SIMPLE_DETECTOR_PAIR) {
            detectors.add(((DetectorPair) pair).getUpstreamDetector());
            detectors.add(((DetectorPair) pair).getDownstreamDetector());
        } else {
            detectors.addAll(((DetectorForkGroup) pair).getUpstreamDetectors());
            detectors.addAll(((DetectorForkGroup) pair).getDownstreamDetectors());
        }

        List<Integer> requiredDetectorCapabilities = getRequiredDetectorCapabilities();
        for (Detector detector : detectors) {
            // Check for required capabilities
            for (int detectorCapability : requiredDetectorCapabilities) {
                SubDetector subDetector = detector.getSubDetector(detectorCapability);

                if (!subDetector.isEnabled()) {
                    log.warn("Pair " + pair.getPairDescription() + ": Required detector feature "
                            + new DetectorCapabilities().getDescription(detectorCapability) + " not present");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks the distance in meters between to detectors since it must not be too big or too small.
     */
    private boolean checkDetectorDistanceConstraint(final AbstractDetectorGroup.Type type, final AbstractDetectorGroup pair) {
        float[] distances = new float[2];
        final float minDistance = 2;
        float maxDistance = 20;

        if (type == AbstractDetectorGroup.Type.SIMPLE_DETECTOR_PAIR) {
            distances[0] = ((DetectorPair) pair).getMonitoredDistance();
            distances[1] = distances[0];
        } else {
            distances = ((DetectorForkGroup) pair).getMinMaxDetectorDistance();
            maxDistance = 100;
        }

        if (distances[0] < minDistance || distances[1] > maxDistance) {
            log.warn("Pair " + pair.getPairDescription() + " rejected: wrong distance of detectors: " + distances[0]
                    + " (min: " + minDistance + "), " + distances[1] + " (max: " + maxDistance + ")");
            return false;
        }

        return true;
    }

    /**
     * The number of uncontrolled streams for detector fork groups with
     * unmonitored in- or outputs must not be too big because the estimations
     * might be too unprecise.
     */
    private boolean checkStreamConstraint(final AbstractDetectorGroup.Type type, final AbstractDetectorGroup pair) {
        if (type != AbstractDetectorGroup.Type.SIMPLE_DETECTOR_PAIR) {
            final int uncontrolledStreams = ((DetectorForkGroup) pair).getNumberUncontrolledStreams();
            final int maxUncontrolledStreams = 1;
            if (uncontrolledStreams > maxUncontrolledStreams) {
                log.warn("Pair " + pair.getPairDescription() + " rejected: Too many uncontrolled in- and/or outstreams");
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the position of the detector group on the containing link.
     */
    private void calculatePositionOnLink(Link link, AbstractDetectorGroup pair) {
        if (link.getType() != Link.Type.EDGE_TO_EDGE) {
            final AbstractDetectorGroup.Type type = pair.getType();
            float distance;

            if (link.getType() == Link.Type.JUNCTION_TO_EDGE) {
                if (type == AbstractDetectorGroup.Type.SIMPLE_DETECTOR_PAIR) {
                    distance = link.getDistancesToStartEnd(((DetectorPair) pair).getUpstreamDetector())[0];
                } else {
                    float minDistance = 0;

                    for (Detector detector : ((DetectorForkGroup) pair).getUpstreamDetectors()) {
                        float tempDistance = link.getDistancesToStartEnd(detector)[0];
                        if (tempDistance > -1 && (minDistance == 0 || tempDistance < minDistance)) {
                            minDistance = tempDistance;
                        }
                    }
                    distance = minDistance;
                }
                pair.setDistanceToStart(distance);
            } else {
                if (type == AbstractDetectorGroup.Type.SIMPLE_DETECTOR_PAIR) {
                    distance = link.getDistancesToStartEnd(((DetectorPair) pair).getDownstreamDetector())[1];
                } else {
                    float minDistance = 0;

                    for (Detector detector : ((DetectorForkGroup) pair).getDownstreamDetectors()) {
                        float tempDistance = link.getDistancesToStartEnd(detector)[1];
                        if (tempDistance > -1 && (minDistance == 0 || tempDistance < minDistance)) {
                            minDistance = tempDistance;
                        }
                    }
                    distance = minDistance;
                }
                pair.setDistanceToEnd(distance);
            }
        }
    }

    /**
     * This method is the last one called during the bootstrapping process of a
     * node. This happens after every node has done a simple local bootstrapping
     * and has notified its neighbours about the created links.
     * <p>
     * After that it is decided which pairs will actually belong to this node
     * since the constraints of every pair are checked here.
     * <p>
     * This is also the reason why the pairs are registered as observers to
     * their detectors in this method. Furthermore the pairs are also assigned
     * to algorithms.
     */
    public final void finalizeInitialisation() {
        Iterator<AbstractDetectorGroup> it = this.pairs.iterator();
        while (it.hasNext()) {
            final AbstractDetectorGroup detectorPair = it.next();
            // Build super pairs, consisting of own pairs and received pairs from neighbors
            if (detectorPair.getType() != AbstractDetectorGroup.Type.SIMPLE_DETECTOR_PAIR) {
                final List<AbstractDetectorGroup> pairsToRemove = buildSuperpairs((DetectorForkGroup) detectorPair,
                        this.receivedPairsFromNeighbours);
                this.pairs.removeAll(pairsToRemove);
            }

            // check pair constraints and finally register as observer
            if (satisfiesPairConstraints(detectorPair)) {
                detectorPair.registerAsObserver();
            } else {
                it.remove();
            }
        }

        addDetectorPairsToLinks();

        it = this.pairs.iterator();
        while (it.hasNext()) {
            final AbstractDetectorGroup detectorPair = it.next();
            try {
                final Link link = findLinkContainingPair(detectorPair);
                calculatePositionOnLink(link, detectorPair);
            } catch (AIDException e) {
                // Detector pair is not observed
                it.remove();
            }
        }

        buildMonitoringZones();
    }

    /**
     * Finds edge to edge entry sections to the junction.
     */
    private void findEdgeToEdgeEntrySections(final List<Section> edgeSections, final Section section) {
        for (Section origin : section.getOriginSections()) {
            findEntrySections(edgeSections, origin);
        }

        // check if section ends at a centroid
        if (!section.isExit() && !section.isJunctionApproach()) {
            for (Section destination : section.getDestinationSections()) {
                findEdgeToEdgeEntrySections(edgeSections, destination);
            }
        }
    }

    /**
     * Finds the link which contains the given detector pair.
     */
    private Link findLinkContainingPair(final AbstractDetectorGroup pair) throws AIDException {
        for (Link link : this.links) {
            if (link.getStartJunction() != null && link.getStartJunction().equals(this.junction) && link.isDetectorPairContained(pair)) {
                return link;
            } else if (link.getEndJunction() != null && link.getEndJunction().equals(this.junction) && link.isDetectorPairContained(pair)) {
                return link;
            }
        }
        throw new AIDException("No matching link for AbstractDetectorGroup found.");
    }

    /**
     * Finds the longest link for a given detector pair.
     */
    private List<Link> findLongestLinksForPair(final AbstractDetectorGroup pair) {
        final List<Link> longestLinks = new ArrayList<>();

        if (pair.getType() == AbstractDetectorGroup.Type.SIMPLE_DETECTOR_PAIR) {
            Link longestContainingLink = null;

            for (Link link : this.links) {
                if (link.isDetectorPairContained(pair) && link.getType() != Link.Type.EDGE_TO_EDGE) {
                    if (longestContainingLink == null) {
                        longestContainingLink = link;
                    } else if (longestContainingLink.getLength() < link.getLength()) {
                        longestContainingLink = link;
                    }
                }
            }
            if (longestContainingLink != null) {
                longestLinks.add(longestContainingLink);
            }
        } else {
            // The length depends on the direction
            final DetectorForkGroup dPair = (DetectorForkGroup) pair;
            final Collection<Detector> allDetectors = new ArrayList<>();
            allDetectors.addAll(dPair.getUpstreamDetectors());
            allDetectors.addAll(dPair.getDownstreamDetectors());

            for (Detector detector : allDetectors) {
                Link longestContainingLink = null;

                for (Link link : this.links) {
                    if (link.isDetectorContained(detector) && link.getType() != Link.Type.EDGE_TO_EDGE) {
                        if (longestContainingLink == null) {
                            longestContainingLink = link;
                        } else if (longestContainingLink.getLength() < link.getLength()) {
                            longestContainingLink = link;
                        }
                    }
                }
                if (longestContainingLink != null) {
                    longestLinks.add(longestContainingLink);
                }
            }
        }

        return longestLinks;
    }

    /**
     * Helper method for {@link #buildLinks()} to find a path from a source section to a
     * destination.
     */
    private List<Section> findPathsAndForksForSectionRecursively(final Section origin, final List<Section> path, final Map<Section, List<List<Section>>> forkingSections) {
        // check for forks
        if (!origin.isJunctionApproach() && origin.getNumDestinationSections() > 1) {
            if (!forkingSections.containsKey(origin)) {
                final List<List<Section>> newPaths = new ArrayList<>();
                forkingSections.put(origin, newPaths);
            }

            forkingSections.get(origin).add(new ArrayList<>(path));
        }

        path.add(origin);

        // Go to next destination section
        if (!origin.isJunctionApproach() && !origin.isExit()) {
            findPathsAndForksForSectionRecursively(origin.getDestinationSection(), path, forkingSections);
        }

        return path;
    }

    /**
     * Returns all algorithms used in monitoring zones from this node.
     */
    public final Iterable<AbstractAIDAlgorithm> getAlgorithms() {
        return this.algorithms;
    }

    /**
     * Sets the GUI component which is used as observer for the algorithms.
     */
    public final void setGUI(AIDComponentPanel gui) {
        this.gui = gui;
    }

    /**
     * Returns the monitoring zone with a given id.
     */
    public AIDMonitoringZone getMonitoringZone(final int id) throws AIDException {
        for (AIDMonitoringZone monitoringZone : this.monitoringZones) {
            if (monitoringZone.getId() == id) {
                return monitoringZone;
            }
        }
        throw new AIDException("Couldn't find monitoring zone with id " + id);
    }

    /**
     * Returns the monitoring zones which belong to this node.
     */
    public final List<AIDMonitoringZone> getMonitoringZones() {
        return this.monitoringZones;
    }

    /**
     * Returns the detector pair object with the given id or null if it doesn't
     * exist.
     */
    public AbstractDetectorGroup getPair(final String id) throws AIDException {
        for (AbstractDetectorGroup pair : this.pairs) {
            if (pair.getId().equals(id)) {
                return pair;
            }
        }
        throw new AIDException("Couldn't find AbstractDetectorGroup with  id " + id);
    }

    /**
     * This method checks if 2 detectors can be grouped to a detector pair.
     * Thereby the focus lies on high level features such as the distance
     * between each other. Low level features like the position on the link are
     * checked earlier.
     *
     * @return {@code true} if the pair satifies all conditions, otherwise
     * {@code false}
     */
    private boolean satisfiesPairConstraints(final AbstractDetectorGroup pair) {
        return checkDetectorCapabilityConstraint(pair.getType(), pair) &&
                checkDetectorDistanceConstraint(pair.getType(), pair) &&
                checkStreamConstraint(pair.getType(), pair);
    }

    /**
     * Inform links about their detector pairs.
     */
    private void addDetectorPairsToLinks() {
        for (AbstractDetectorGroup detectorPair : this.pairs) {
            Collection<Integer> sectionIDs = new ArrayList<>();

            if (detectorPair.getType() == AbstractDetectorGroup.Type.SIMPLE_DETECTOR_PAIR) {
                int sectionDownStream = ((DetectorPair) detectorPair).getDownstreamDetector().getSectionId();
                int sectionUpStream = ((DetectorPair) detectorPair).getUpstreamDetector().getSectionId();

                sectionIDs.add(sectionDownStream);
                if (sectionUpStream != sectionDownStream) {
                    sectionIDs.add(sectionUpStream);
                }
            } else {
                ((DetectorForkGroup) detectorPair).getDownstreamDetectors().stream().filter(detector ->
                        !sectionIDs.contains(detector.getSectionId())).forEach(detector -> sectionIDs.add(detector.getSectionId()));
                ((DetectorForkGroup) detectorPair).getUpstreamDetectors().stream().filter(detector ->
                        !sectionIDs.contains(detector.getSectionId())).forEach(detector -> sectionIDs.add(detector.getSectionId()));
            }

            for (Link link : this.links) {
                for (int id : sectionIDs) {
                    for (Section section : link.getMonitoredSections()) {
                        if (id == section.getId() && !link.isDetectorPairContained(detectorPair)) {
                            link.addDetectorPair(detectorPair);
                        }
                    }
                }
            }
        }
    }

    /**
     * This method is used to pass a new link to a node. Thereby it takes the
     * link, clones it, assigns a new id and stores it with the other links.
     * <p>
     * It is used in the {{@link #buildLinks()} method.
     */
    public void transferLink(Link link) {
        try {
            final Link linkClone = link.getClone();
            linkClone.setId(AIDUtilities.getInstance().computeID(this.junction.getId()));
            this.links.add(linkClone);

            final Collection<Link> temp = new ArrayList<>();
            temp.add(link);
            buildDetectorPairs(temp);
        } catch (CloneNotSupportedException e) {
            log.error(link.getId() + " couldn't be transfered to " + this.junction.getId());
        }
    }

    /**
     * Initializes the loggers for the algorithms and evaluators.
     */
    private void initialiseLogger(final AbstractAIDAlgorithm algorithm) {
        int junctionID = this.junction.getId();

        String loggerName = "aid.logger_" + junctionID + "_MZ" + String.valueOf(this.nextZoneID);
        Logger algorithmLogger = Logger.getLogger(loggerName);
        Logger evaluationLogger = Logger.getLogger(loggerName + "_Evaluation");

        try {
            String path = "./logs/AID/" + algorithm.getName().replaceAll(" ", "") + "_Node_" + junctionID + "_MZ" + String.valueOf(this.nextZoneID);

            Appender algorithmLogAppender = new RollingFileAppender(new PatternLayout("%m%n"), path + ".csv", false);
            algorithmLogger.addAppender(algorithmLogAppender);

            Appender evaluationAppender = new RollingFileAppender(new PatternLayout("%m%n"), path + "_Evaluation.log", true);
            evaluationLogger.addAppender(evaluationAppender);

            algorithm.setLogger(algorithmLogger);
            algorithm.getAIDEvaluator().setLogger(evaluationLogger);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
