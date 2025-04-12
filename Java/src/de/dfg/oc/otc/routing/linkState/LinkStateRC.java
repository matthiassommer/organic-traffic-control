package de.dfg.oc.otc.routing.linkState;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.*;
import de.dfg.oc.otc.routing.ProtocolType;
import de.dfg.oc.otc.routing.RoutingComponent;

import java.util.*;

/**
 * Class implements the LinkState protocol for decentralised routing in urban
 * road networks. Based on the Open Shortest Path First(OSPF) routing protocol.
 * <p>
 * Nodes only know their local delays and their neighbours. Communicate these to
 * direct neighbours (flooding). Shortest path is then calculated with
 * Dijkstra's algorithm.
 * <p>
 * Link-state algorithms require more CPU power and memory than distance vector
 * algorithms and are generally more scalable than distance vector protocols
 *
 * @author lyda
 */
public class LinkStateRC extends RoutingComponent {
    /**
     * Queue of received advertisements to process and send further on.
     */
    protected final List<Advertisement> interiorAdvertisementQueue;
    /**
     * The network database for this node.
     */
    protected final Database interiorLinkstateDatabase;
    /**
     * Maps the RoutingComponents to the last received sequence number.
     */
    protected final Map<Integer, Integer> rcToSequenceNumber;
    /**
     * Were local advertisements calculated?
     */
    protected boolean determinedLocalAdvertisements;
    /**
     * The last used sequence number for advertisements.
     */
    public int sequenceNumberCounter;

    public LinkStateRC(final ProtocolType type, final OTCNode node) {
        super(type, node);
        this.interiorLinkstateDatabase = new Database();
        this.rcToSequenceNumber = new HashMap<>();
        this.interiorAdvertisementQueue = new ArrayList<>();
    }

    /**
     * Determines the local LinkStateDataDaseEntries with the current traffic situation.
     * Create an advertisement with all the database entries for all turnings of this node to
     * neighbour nodes and centroids.
     *
     * @return LinkStateAdvert with the local LinkStateDataDaseEntries
     */
    protected Advertisement determineLocalAdvertisement() {
        sequenceNumberCounter++;

        final Advertisement advertisement = new Advertisement(this, sequenceNumberCounter);
        final AimsunJunction junction = this.getJunction();

        // Determine link costs for all turnings to all neighbouring RCs and centroids
        for (Turning turning : junction.getTurnings(TrafficType.INDIVIDUAL_TRAFFIC)) {
            float turningCost = determineTurningCost(turning.getInSection().getId(), turning.getOutSection().getId());
            final float linkCost = getDynamicDelayForSection(turning.getOutSection().getId());

            try {
                DatabaseEntry databaseEntry = createDatabaseEntry(turning.getInSection(), turning.getOutSection(), turningCost + linkCost);
                advertisement.addLinkStateDataBaseEntry(databaseEntry);
            } catch (OTCManagerException e) {
                log.warn(e.getMessage());
            }
        }

        return advertisement;
    }

    private DatabaseEntry createDatabaseEntry(final Section inSection, final Section outSection, final float costs) {
        final AimsunJunction nextJunction = outSection.getNextJunction();
        if (nextJunction != null) {
            // next node is junction
            final LinkStateRC neighbourRC = (LinkStateRC) nextJunction.getNode().getRoutingComponent();
            return new DatabaseEntry(this, neighbourRC, null, inSection, outSection, costs, -1, sequenceNumberCounter);
        } else {
            // next node is centroid
            final Integer destination = this.outSectionDestinations.get(outSection.getId());
            if (destination != null) {
                final Centroid centroid = OTCManager.getInstance().getNetwork().getCentroid(destination);
                return new DatabaseEntry(this, null, centroid, inSection, outSection, costs, -1, sequenceNumberCounter);
            }
        }
        throw new OTCManagerException("Section neither leads to a node nor a centroid");
    }

    /**
     * Receive a package of advertisements from a neighbouring RC.
     * Update local database with these advertisements and advertises it to neighbours.
     *
     * @param sender         neighbouring node who sent a list of advertisements
     * @param advertisements
     */
    private void notifyLink(final LinkStateRC sender, final Collection<Advertisement> advertisements) {
        Advertisement localAdvert = null;

        if (!this.determinedLocalAdvertisements) {
            localAdvert = determineLocalAdvertisement();
            advertisements.add(localAdvert);
            this.determinedLocalAdvertisements = true;
        }

        if (!advertisements.isEmpty()) {
            // Check all advertisements if they offer new information
            final Collection<Advertisement> newAdvertisements = new ArrayList<>();

            for (Advertisement advertisement : advertisements) {
                final boolean isNewSequenceNumber = checkAndUpdateSequenceNumber(advertisement);
                if (isNewSequenceNumber) {
                    newAdvertisements.add(advertisement);
                }
            }

            if (localAdvert != null) {
                // Update Sequence number if localAdvert was determined
                rcToSequenceNumber.put(getNodeID(), localAdvert.getSequenceNumber());
            }

            if (!newAdvertisements.isEmpty()) {
                putAdvertisementInQueue(sender, newAdvertisements);
            }

            if (localAdvert != null && sender != this) {
                informSender(sender, localAdvert);
            }
        }
    }

    /**
     * Check the sequence number of the advertisement and save the newest sequence number of the source.
     *
     * @param advertisement to check if outdated or new
     */
    private boolean checkAndUpdateSequenceNumber(final Advertisement advertisement) {
        final Integer lastSequenceNumber = rcToSequenceNumber.get(advertisement.getSource().getNodeID());

        if (lastSequenceNumber == null || lastSequenceNumber < advertisement.getSequenceNumber()) {
            rcToSequenceNumber.put(advertisement.getSource().getNodeID(), advertisement.getSequenceNumber());
            return true;
        }
        return false;
    }

    /**
     * If advert has new information put it in queue.
     *
     * @param sender         the origin sender
     * @param advertisements new advertisements to send
     */
    private void putAdvertisementInQueue(final LinkStateRC sender, final Collection<Advertisement> advertisements) {
        interiorAdvertisementQueue.addAll(advertisements);
        notifyNeighbours(sender, advertisements);
    }

    /**
     * Inform sender if localAdvert was determined.
     *
     * @param sender      the origin sender
     * @param localAdvert
     */
    private void informSender(final LinkStateRC sender, final Advertisement localAdvert) {
        final Collection<Advertisement> advertForSender = new ArrayList<>();
        advertForSender.add(localAdvert);

        increaseCommunicationCounterByOne();
        sender.notifyLink(this, advertForSender);
    }

    /**
     * Send advertisements to direct neighbours, but not to the origin sender.
     *
     * @param sender         the origin sender
     * @param advertisements list of advertisements
     */
    private void notifyNeighbours(final LinkStateRC sender, final Collection<Advertisement> advertisements) {
        if (!advertisements.isEmpty()) {
            for (Map.Entry<Integer, RoutingComponent> neighbourEntry : neighbourRCs.entrySet()) {
                if (sender.getNodeID() != neighbourEntry.getKey()) {
                    increaseCommunicationCounterByOne();
                    ((LinkStateRC) neighbourEntry.getValue()).notifyLink(this, advertisements);
                }
            }
        }
    }

    /**
     * 1. Determine advertisements for outgoing sections and turnings for each node
     * 2. Send these advertisements to direct node neighbours
     * 3. Check and update sequence numbers of advertisements
     * 4. Repeat with 1. until no neighbours are found or all messages are outdated
     * 5. Update Database with received advertisements
     * 6. Calculate shortest paths from self to all centroids with dijkstra algorithm
     */
    @Override
    public void performProtocol() {
        if (!determinedLocalAdvertisements) {
            final Advertisement localAdvert = determineLocalAdvertisement();
            interiorAdvertisementQueue.add(localAdvert);

            rcToSequenceNumber.put(getNodeID(), localAdvert.getSequenceNumber());
            determinedLocalAdvertisements = true;
        }

        sendAdvertisementToNeighbours(interiorAdvertisementQueue);

        // Update LinksStateDatabase with queue
        interiorAdvertisementQueue.forEach(interiorLinkstateDatabase::updateLinkCost);
        interiorAdvertisementQueue.clear();

        new DijkstraAlgorithm().runAlgorithm(this, interiorLinkstateDatabase, false);
    }

    @Override
    public void resetRoutingData() {
        this.determinedLocalAdvertisements = false;
        super.resetRoutingData();
    }

    /**
     * Update own Database and notify neighbours. Prepare to be sent
     * advertisements.
     *
     * @param advertisements to send
     */
    protected void sendAdvertisementToNeighbours(final Collection<Advertisement> advertisements) {
        final Collection<Advertisement> advertisementsToSend = new ArrayList<>();
        advertisementsToSend.addAll(advertisements);
        notifyNeighbours(this, advertisementsToSend);
    }
}
