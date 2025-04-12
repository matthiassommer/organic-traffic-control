package de.dfg.oc.otc.routing.linkState;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.*;
import de.dfg.oc.otc.routing.ProtocolType;
import de.dfg.oc.otc.routing.RoutingTable;

import java.util.*;

/**
 * Class implements the RegionalLinkState protocol for decentralised routing in
 * urban road networks.
 * <p>
 * Applying ideas from the Border-Gateway-Protocol for communication between
 * different regions, and Interior-Regional-Protocol for communication within
 * the same region.
 *
 * @author lyda
 */
public class RegionalLinkStateRC extends LinkStateRC {
    protected final Database borderLinkstateDatabase;
    /**
     * Border Neighbours in neighbouring region.
     */
    protected final Set<RegionalLinkStateRC> borderNeighbours;
    /**
     * Neighbours in interior Network.
     */
    protected final Set<RegionalLinkStateRC> interiorNeighbours;
    protected final List<Advertisement> borderAdvertisementQueue;
    /**
     * Border Neighbours in same region.
     */
    private final Set<RegionalLinkStateRC> borderRCsInRegion;
    /**
     * Neighbouring nodes within another region to cooperate with.
     */
    private final Set<RegionalLinkStateRC> toAskBorderNeighbours;
    /**
     * Neighbouring nodes within the same region to cooperate with.
     */
    private final Set<RegionalLinkStateRC> toAskInteriorNeighbours;
    protected Advertisement dijkstraAdvertisement;
    private boolean determinedBorderAdvert;
    private Advertisement localBorderAdvert;

    public RegionalLinkStateRC(final ProtocolType type, final OTCNode node) {
        super(type, node);
        this.interiorNeighbours = new HashSet<>(5);
        this.toAskInteriorNeighbours = new HashSet<>(5);
        this.borderNeighbours = new HashSet<>(3);
        this.toAskBorderNeighbours = new HashSet<>(3);
        this.borderAdvertisementQueue = new ArrayList<>();
        this.borderRCsInRegion = new HashSet<>(3);
        this.borderLinkstateDatabase = new Database();
    }

    /**
     * Asks for cooperation with possible interior and border candidates and
     * determines their regional status.
     */
    public final void askCooperations() {
        this.toAskInteriorNeighbours.stream().filter(rc -> !interiorNeighbours.contains(rc)).forEach(rc -> rc.notifyCooperation(this, RegionalType.INTERIOR, RegionalContactType.ASK));
        this.toAskInteriorNeighbours.clear();

        this.toAskBorderNeighbours.stream().filter(rc -> !borderNeighbours.contains(rc)).forEach(rc -> rc.notifyCooperation(this, RegionalType.BORDER, RegionalContactType.ASK));
        this.toAskBorderNeighbours.clear();
    }

    /**
     * Clear link state database.
     */
    public final void clearLinkstateDatabase() {
        this.interiorLinkstateDatabase.reset();
    }

    /**
     * Calculate costs to all border nodes in nearby traffic networks depending
     * on the link state database of the component.
     */
    public void computeBorderDijkstra() {
        this.borderAdvertisementQueue.forEach(this.borderLinkstateDatabase::updateLinkCost);
        this.borderAdvertisementQueue.clear();
        new DijkstraAlgorithm().runAlgorithm(this, this.borderLinkstateDatabase, true);
    }

    /**
     * Calculate all costs to all destinations in the regional sub-network depending
     * on the link state database of the component.
     */
    public void computeInteriorDijkstra() {
        this.interiorAdvertisementQueue.forEach(this.interiorLinkstateDatabase::updateLinkCost);
        this.interiorAdvertisementQueue.clear();
        new DijkstraAlgorithm().runAlgorithm(this, this.interiorLinkstateDatabase, false);
    }

    /**
     * Determines LinkStateAdverts for border neighbours with information from
     * routing tables and costs to all destinations in the interior network.
     *
     * @return LinkStateAdvert
     */
    protected Advertisement determineBorderAdvertisements() {
        sequenceNumberCounter++;

        final Advertisement advertisement = new Advertisement(this, sequenceNumberCounter);

        // Determine for all routing tables
        for (Section inSection : this.getJunction().getInSections()) {
            final RoutingTable routingTable = this.getRoutingTableForSection(inSection.getId());

            // Run through each to create a direct LinkStateDatabaseEntry
            // from this component to the destination
            for (Integer destinationId : routingTable.getDestinationIDs()) {
                final int outSectionId = routingTable.getNextHopForTarget(destinationId);
                final Section outSection = OTCManager.getInstance().getNetwork().getSection(outSectionId);
                final float costs = routingTable.getDelayForTarget(destinationId);
                final Centroid centroid = OTCManager.getInstance().getNetwork().getCentroid(destinationId);

                DatabaseEntry entry = new DatabaseEntry(this, null, centroid, inSection, outSection,
                        costs, -1, sequenceNumberCounter);
                advertisement.addLinkStateDataBaseEntry(entry);
            }
        }

        if (this.dijkstraAdvertisement != null) {
            this.dijkstraAdvertisement.getDatabaseEntries().forEach(advertisement::addLinkStateDataBaseEntry);
            this.dijkstraAdvertisement = null;
        }

        return advertisement;
    }

    /**
     * Determines LinkStateAdverts for interior neighbours and own adverts for
     * costs to border neighbours.
     *
     * @return array of [0]:interior and [1]:border adverts
     */
    protected Advertisement[] determineLocalAdvertisements() {
        sequenceNumberCounter++;

        final Advertisement interiorAdvert = new Advertisement(this, sequenceNumberCounter);
        final Advertisement borderAdvert = new Advertisement(this, sequenceNumberCounter);

        // Determine link costs for all turnings to all neighbouring RC
        final AimsunJunction junction = this.getJunction();
        for (Turning turning : junction.getTurnings(TrafficType.INDIVIDUAL_TRAFFIC)) {
            Section inSection = turning.getInSection();
            Section outSection = turning.getOutSection();

            // Determine costs between sections
            float turningCosts = determineTurningCost(inSection.getId(), outSection.getId());
            float linkCost = getDynamicDelayForSection(outSection.getId());
            float totalCost = turningCosts + linkCost;

            // Check destination of outSection
            // If its a Junction
            AimsunJunction nextJunction = outSection.getNextJunction();
            if (nextJunction != null) {
                RegionalLinkStateRC nextRC = (RegionalLinkStateRC) nextJunction.getNode().getRoutingComponent();

                if (interiorNeighbours.contains(nextRC)) {
                    DatabaseEntry interiorDBEntry = new DatabaseEntry(this, nextRC, null, inSection, outSection,
                            totalCost, -1, sequenceNumberCounter);
                    interiorAdvert.addLinkStateDataBaseEntry(interiorDBEntry);
                } else if (borderNeighbours.contains(nextRC)) {
                    DatabaseEntry borderDBEntry = new DatabaseEntry(this, nextRC, null, inSection, outSection,
                            totalCost, -1, sequenceNumberCounter);
                    borderAdvert.addLinkStateDataBaseEntry(borderDBEntry);
                }
            }
            // if its a Centroid
            else {
                Integer centroidId = this.outSectionDestinations.get(outSection.getId());
                if (centroidId != null) {
                    Centroid centroid = OTCManager.getInstance().getNetwork().getCentroid(centroidId);
                    DatabaseEntry interiorDBEntry = new DatabaseEntry(this, null, centroid, inSection, outSection,
                            totalCost, -1, sequenceNumberCounter);
                    interiorAdvert.addLinkStateDataBaseEntry(interiorDBEntry);
                }
            }
        }

        final Advertisement[] resultAdverts = new Advertisement[2];
        resultAdverts[0] = interiorAdvert;
        resultAdverts[1] = borderAdvert;
        return resultAdverts;
    }

    /**
     * Determine a region for each node and sort neighbours into possible
     * interior and border candidates based on the costs between this node and
     * his neighbour.
     */
    public final void determineRegionalRCType() {
        // determine which neighbours are borderNeighbours by checking their travel costs
        if (!costToNeighbour.isEmpty()) {
            float costSum = 0;
            for (Map.Entry<Integer, Integer> neighbour : costToNeighbour.entrySet()) {
                costSum += neighbour.getValue();
            }

            // Cost Maximum to be interiorNeighbour
            final float avgCost = costSum / costToNeighbour.size();
            float maxCost = 1.1f * avgCost;

            for (Map.Entry<Integer, Integer> neighbour : costToNeighbour.entrySet()) {
                if (maxCost > neighbour.getValue()) {
                    // For neighbour under the maxCost belong to interior
                    toAskInteriorNeighbours.add((RegionalLinkStateRC) neighbourRCs.get(neighbour.getKey()));
                } else {
                    toAskBorderNeighbours.add((RegionalLinkStateRC) neighbourRCs.get(neighbour.getKey()));
                    setRegionalType(RegionalType.BORDER);
                }
            }
        }
    }

    /**
     * Create database entries to centroids which are not in the local region.
     *
     * @return advertisement
     */
    protected Advertisement determineVirtualAdvert() {
        sequenceNumberCounter++;

        final Set<Centroid> restOfCentroids = borderLinkstateDatabase.getCentroidSet();
        final Set<Centroid> localCentroids = interiorLinkstateDatabase.getCentroidSet();

        // remove all centroids from globalCentroids which exists in
        // localCentroids to get the resulting missing centroids
        restOfCentroids.removeAll(localCentroids);

        final Advertisement advert = new Advertisement(this, sequenceNumberCounter);

        // Determine for all routingtables
        for (Section inSection : this.getJunction().getInSections()) {
            // Get destinations from routingtable
            final RoutingTable routingTable = this.getRoutingTableForSection(inSection.getId());

            // and run through each to create a direct LinkStateDatabaseEntry
            // from this component to the destination
            for (Centroid centroid : restOfCentroids) {
                // Get parameters for LinkStateDatabaseEntry
                int outSectionId = routingTable.getNextHopForTarget(centroid.getId());
                if (outSectionId != -1) {
                    final Section outSection = OTCManager.getInstance().getNetwork().getSection(outSectionId);
                    final float costs = routingTable.getDelayForTarget(centroid.getId());

                    DatabaseEntry entry = new DatabaseEntry(this, null, centroid, inSection,
                            outSection, costs, -1, sequenceNumberCounter);
                    advert.addLinkStateDataBaseEntry(entry);
                }
            }
        }
        return advert;
    }

    /**
     * Determine border advertisements and send it to all neighbours.
     */
    public final void distributeBorderAdvertisements() {
        notifyLink(this, new ArrayList<>(5));
    }

    /**
     * Determine local advertisements and send it to all interior neighbours.
     */
    public final void distributeInteriorAdvertisements() {
        notifyInteriorLink(this, new ArrayList<>(5));
    }

    /**
     * Creates a virtual advertisements about links to all destinations in
     * foreign regions for a border component and sends them to interior links
     * in the same region.
     */
    public final void distributeVirtualInteriorAdverts() {
        final Collection<Advertisement> sendAdverts = new ArrayList<>(2);
        final Advertisement virtualAdvert = determineVirtualAdvert();
        sendAdverts.add(virtualAdvert);

        notifyInteriorLink(this, sendAdverts);
    }

    public final Collection<RegionalLinkStateRC> getBorderRCsInRegion() {
        return borderRCsInRegion;
    }

    /**
     * @param sender
     * @param adverts
     */
    private void notifyAllNeighbours(final RegionalLinkStateRC sender, final Collection<Advertisement> adverts) {
        if (!adverts.isEmpty()) {
            // Don't inform sender but all other neighbours
            neighbourRCs.entrySet().forEach(neighbourEntry -> {
                boolean isNotOrigin = sender.getNodeID() != neighbourEntry.getKey();
                // Don't inform sender but all other neighbours
                if (isNotOrigin) {
                    increaseCommunicationCounterByOne();
                    ((RegionalLinkStateRC) neighbourEntry.getValue()).notifyLink(this, adverts);
                }
            });
        }
    }

    /**
     * ASK a neighbour for a cooperation as an interior or border neighbour.
     * Node replies with an ACK or reject.
     *
     * @param sender          is the neighbour sending the request
     * @param regionalType suggested cooperation
     * @param contactType     is the type of asking, confirming or rejecting
     */
    private void notifyCooperation(final RegionalLinkStateRC sender, final RegionalType regionalType,
                                   final RegionalContactType contactType) {
        Set<RegionalLinkStateRC> askNeighbours;
        Set<RegionalLinkStateRC> newNeighbours;

        switch (regionalType) {
            case BORDER:
                askNeighbours = toAskBorderNeighbours;
                newNeighbours = borderNeighbours;
                break;
            case INTERIOR:
                askNeighbours = toAskInteriorNeighbours;
                newNeighbours = interiorNeighbours;
                break;
            default:
                throw new IllegalArgumentException("Unknown regional type");
        }

        switch (contactType) {
            case ASK:
                if (askNeighbours.contains(sender)) {
                    sender.notifyCooperation(this, regionalType, RegionalContactType.ACK);
                    newNeighbours.add(sender);
                } else {
                    sender.notifyCooperation(this, regionalType, RegionalContactType.REJECT);
                }
                this.increaseCommunicationCounterByOne();
                break;
            case ACK:
                newNeighbours.add(sender);
                break;
            case REJECT:
                break;
            default:
                throw new IllegalArgumentException("Unknown regional type");
        }
    }

    /**
     * Updates local Database with LinkStateAdvert and advertises it to
     * neighbours.
     *
     * @param sender
     * @param adverts
     */
    private void notifyInteriorLink(final RegionalLinkStateRC sender, final Collection<Advertisement> adverts) {
        Advertisement localAdvert = null;

        if (!determinedLocalAdvertisements) {
            // Determine local advert
            final Advertisement[] localAdverts = determineLocalAdvertisements();

            // Update Database with borderNeighbour Info
            adverts.add(localAdverts[0]);
            interiorAdvertisementQueue.add(localAdverts[0]);
            localAdvert = localAdverts[0];

            localBorderAdvert = localAdverts[1];
            borderAdvertisementQueue.add(localAdverts[1]);

            this.determinedLocalAdvertisements = true;
        }

        if (!adverts.isEmpty()) {
            // Check all new adverts if you know them already
            final Collection<Advertisement> newAdverts = new ArrayList<>();
            for (Advertisement newAdvert : adverts) {
                Integer lastSequenceNumber = rcToSequenceNumber.get(newAdvert.getSource().getNodeID());

                if (lastSequenceNumber == null || lastSequenceNumber < newAdvert.getSequenceNumber()) {
                    newAdverts.add(newAdvert);

                    // Save the newest sequence number of the source
                    rcToSequenceNumber.put(newAdvert.getSource().getNodeID(), newAdvert.getSequenceNumber());

                    // If this and sender are border component, then save sender
                    // as border component in same region
                    if (this != newAdvert.getSource()
                            && this.getRegionalRCType() == RegionalType.BORDER
                            && newAdvert.getSource().getRegionalRCType() == RegionalType.BORDER) {
                        // put it in the borderRCsInRegion set
                        borderRCsInRegion.add((RegionalLinkStateRC) newAdvert.getSource());
                    }

                }
            }

            if (!newAdverts.isEmpty()) {
                // If advertisement has new information then put it in
                // advertqueue
                interiorAdvertisementQueue.addAll(newAdverts);
                notifyInteriorNeighbours(sender, newAdverts);
            }

            if (localAdvert != null && sender != this) {
                // Inform sender if localAdvert was determined and not sender is
                // itself
                final Collection<Advertisement> advertForSender = new ArrayList<>();
                advertForSender.add(localAdvert);

                increaseCommunicationCounterByOne();
                sender.notifyInteriorLink(this, advertForSender);
            }
        }
    }

    /**
     * Send new advertisements to all interior neighbours.
     *
     * @param sender
     * @param adverts
     */
    private void notifyInteriorNeighbours(final RegionalLinkStateRC sender, final Collection<Advertisement> adverts) {
        if (!adverts.isEmpty()) {
            // Don't inform sender but all other neighbours
            interiorNeighbours.forEach(interiorNeighbour -> {
                boolean isNotOrigin = sender.getNodeID() != interiorNeighbour.getNodeID();
                // Don't inform sender but all other neighbours
                if (isNotOrigin) {
                    increaseCommunicationCounterByOne();
                    interiorNeighbour.notifyInteriorLink(this, adverts);
                }
            });
        }
    }

    /**
     * Border components save the adverts and both interior and border
     * components are forwarding them.
     *
     * @param sender
     * @param adverts
     */
    private void notifyLink(final RegionalLinkStateRC sender, final Collection<Advertisement> adverts) {
        // Prepare to be sent adverts
        Advertisement localAdvert = null;

        if (this.getRegionalRCType() == RegionalType.BORDER) {
            if (!determinedBorderAdvert) {
                // Determine local advert
                localAdvert = determineBorderAdvertisements();

                // Add localBorderAdvert Entries to localAdvert cause the
                // sequencenumber is old
                if (localBorderAdvert != null) {
                    for (DatabaseEntry borderDatabaseEntry : localBorderAdvert.getDatabaseEntries()) {
                        localAdvert.addLinkStateDataBaseEntry(borderDatabaseEntry);
                    }
                    localBorderAdvert = null;
                }

                // Add to AdverQueue
                adverts.add(localAdvert);
                borderAdvertisementQueue.add(localAdvert);
                determinedBorderAdvert = true;
            }
        }

        if (!adverts.isEmpty()) {
            // Check all new adverts if you know them already
            final Collection<Advertisement> newAdverts = new ArrayList<>();
            for (Advertisement newAdvert : adverts) {
                Integer lastSequenceNo = rcToSequenceNumber.get(newAdvert.getSource().getNodeID());
                if (lastSequenceNo == null || lastSequenceNo < newAdvert.getSequenceNumber()) {
                    newAdverts.add(newAdvert);

                    // Also add to recievedBorderAdvertQueue if its a border
                    // component
                    if (this.getRegionalRCType() == RegionalType.BORDER) {
                        borderAdvertisementQueue.add(newAdvert);
                    }

                    // Save the newest sequence number of the source
                    // routingcomponent
                    rcToSequenceNumber.put(newAdvert.getSource().getNodeID(), newAdvert.getSequenceNumber());
                }
            }

            if (!newAdverts.isEmpty()) {
                // Notify neighbours with new adverts
                notifyAllNeighbours(sender, newAdverts);
            }

            if (localAdvert != null && sender != this) {
                // Inform sender if localAdvert was determined and not sender is
                // itself
                final Collection<Advertisement> advertForSender = new ArrayList<>();
                advertForSender.add(localAdvert);

                increaseCommunicationCounterByOne();
                sender.notifyLink(this, advertForSender);
            }
        }
    }

    @Override
    public final void performProtocol() {
        this.interiorAdvertisementQueue.clear();
        this.interiorLinkstateDatabase.reset();
        this.borderLinkstateDatabase.reset();
        this.borderAdvertisementQueue.clear();
        this.determinedBorderAdvert = false;
    }

    @Override
    public final void resetRoutingData() {
        this.determinedBorderAdvert = false;
        super.resetRoutingData();
    }

    public final void setDijkstraAdvertisement(final Advertisement advertisement) {
        this.dijkstraAdvertisement = advertisement;
    }

    /**
     * Enum specifies the different available contact types for two neighbouring
     * RegionalLinkStateRC.
     */
    public enum RegionalContactType {
        ACK, ASK, REJECT
    }
}
