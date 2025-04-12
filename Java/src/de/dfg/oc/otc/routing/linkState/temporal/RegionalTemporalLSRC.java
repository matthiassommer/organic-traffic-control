package de.dfg.oc.otc.routing.linkState.temporal;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.*;
import de.dfg.oc.otc.routing.ProtocolType;
import de.dfg.oc.otc.routing.RoutingTable;
import de.dfg.oc.otc.routing.linkState.Advertisement;
import de.dfg.oc.otc.routing.linkState.RegionalLinkStateRC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * This protocol xtends the RegionalLinkStateRC using forecasts of travel times instead of static travel times.
 *
 * @author Kamuran Isik
 */
public class RegionalTemporalLSRC extends RegionalLinkStateRC {
    public RegionalTemporalLSRC(ProtocolType type, OTCNode node) {
        super(type, node);
    }

    @Override
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
                final Centroid centroid = OTCManager.getInstance().getNetwork().getCentroid(destinationId);

                TemporalDatabaseEntry entry = createDatabaseEntry(inSection, outSection, centroid, null);
                advertisement.addLinkStateDataBaseEntry(entry);
            }
        }

        if (this.dijkstraAdvertisement != null) {
            this.dijkstraAdvertisement.getDatabaseEntries().forEach(advertisement::addLinkStateDataBaseEntry);
            this.dijkstraAdvertisement = null;
        }

        return advertisement;
    }

    private TemporalDatabaseEntry createDatabaseEntry(Section inSection, Section outSection, Centroid centroid, RegionalLinkStateRC rc) {
        List<HashMap.SimpleEntry<Integer, Float>> timeTurningForecastMappings = new ArrayList<>();
        List<HashMap.SimpleEntry<Integer, Float>> timeLinkForecastMappings = new ArrayList<>();

        determineForecastMappings(inSection, outSection, timeTurningForecastMappings, timeLinkForecastMappings);

        final float turningCost = determineTurningCost(inSection.getId(), outSection.getId());
        final float linkCost = getDynamicDelayForSection(outSection.getId());

        if (centroid != null) {
            return new TemporalDatabaseEntry(this, null, centroid, inSection, outSection, timeTurningForecastMappings,
                    timeLinkForecastMappings, sequenceNumberCounter, turningCost, linkCost);
        } else if (rc != null) {
            return new TemporalDatabaseEntry(this, rc, null, inSection, outSection, timeTurningForecastMappings,
                    timeLinkForecastMappings, sequenceNumberCounter, turningCost, linkCost);
        }
        throw new IllegalArgumentException("Centroid or RC must not be null");
    }

    @Override
    protected Advertisement[] determineLocalAdvertisements() {
        sequenceNumberCounter++;

        final Advertisement interiorAdvert = new Advertisement(this, sequenceNumberCounter);
        final Advertisement borderAdvert = new Advertisement(this, sequenceNumberCounter);

        // Determine link costs for all turnings to all neighbouring RC
        final AimsunJunction junction = this.getJunction();
        for (Turning turning : junction.getTurnings(TrafficType.INDIVIDUAL_TRAFFIC)) {
            Section inSection = turning.getInSection();
            Section outSection = turning.getOutSection();

            // Check destination of outSection
            // If its a Junction
            AimsunJunction nextJunction = outSection.getNextJunction();
            if (nextJunction != null) {
                RegionalLinkStateRC nextRC = (RegionalLinkStateRC) nextJunction.getNode().getRoutingComponent();

                if (interiorNeighbours.contains(nextRC)) {
                    TemporalDatabaseEntry interiorDBEntry = createDatabaseEntry(inSection, outSection, null, nextRC);
                    interiorAdvert.addLinkStateDataBaseEntry(interiorDBEntry);
                } else if (borderNeighbours.contains(nextRC)) {
                    TemporalDatabaseEntry borderDBEntry = createDatabaseEntry(inSection, outSection, null, nextRC);
                    borderAdvert.addLinkStateDataBaseEntry(borderDBEntry);
                }
            }
            // if its a Centroid
            else {
                Integer centroidId = this.outSectionDestinations.get(outSection.getId());
                if (centroidId != null) {
                    Centroid centroid = OTCManager.getInstance().getNetwork().getCentroid(centroidId);
                    TemporalDatabaseEntry interiorDBEntry = createDatabaseEntry(inSection, outSection, centroid, null);
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
     * Combine forecasts if they are nearly the same to reduce communication overhead.
     *
     * @param time
     * @param oldCost
     * @param currentCost
     * @param timeForecastMappings
     * @return
     */
    private float reduceCommunicationOverhead(float time, float oldCost, float currentCost,
                                              List<HashMap.SimpleEntry<Integer, Float>> timeForecastMappings) {
        // minimal difference in seconds
        final float threshold = 0.1f;

        if (time == 0) {
            oldCost = currentCost;
            // add first entry
            timeForecastMappings.add(new HashMap.SimpleEntry<>((int) time, currentCost));
        }

        if (time > 0) {
            if (Math.abs(oldCost - currentCost) > threshold) {
                // add new entry
                timeForecastMappings.add(new HashMap.SimpleEntry<>((int) time, currentCost));
            } else {
                // Adapt last old entry: Take average of new and old value
                HashMap.SimpleEntry<Integer, Float> oldEntry = timeForecastMappings.get(timeForecastMappings.size() - 1);
                oldEntry.setValue((oldEntry.getValue() + currentCost) / 2);
            }

            oldCost = currentCost;
        }
        return oldCost;
    }

    /**
     * Determines the forecasts list for a {@link de.dfg.oc.otc.manager.aimsun.Turning} (specified by its in- and outbound section)
     * and for its outbound {@link de.dfg.oc.otc.manager.aimsun.Section}.
     *
     * @param inSection  inbound section of the {@link de.dfg.oc.otc.manager.aimsun.Turning}
     * @param outSection outbound section of the {@link de.dfg.oc.otc.manager.aimsun.Turning}
     */
    private void determineForecastMappings(final Section inSection, final Section outSection,
                                           List<HashMap.SimpleEntry<Integer, Float>> turningTimeForecastMappings,
                                           List<HashMap.SimpleEntry<Integer, Float>> linkTimeForecastMappings) {
        float oldTurningCost = 0;
        float oldLinkCost = 0;

        // Send current flow and forecasts for different future points in time (e.g. 10 minutes)
        for (int time = 0; time < 900; time += 90) {
            float turningCost = getTurningForecast(time, inSection.getId(), outSection.getId());
            oldTurningCost = reduceCommunicationOverhead(time, oldTurningCost, turningCost, turningTimeForecastMappings);

            float forecastTime = time + turningCost;

            float sectionCost = 0;
            if (outSection.getNextJunction() != null) {
                final int targetJunctionID = outSection.getNextJunction().getId();
                sectionCost = getSectionDelay(forecastTime, outSection, targetJunctionID, true);
            } else if (!outSection.getConnectedCentroids().isEmpty()) {
                final int targetCentroidID = outSection.getConnectedCentroids().get(0).getId();
                sectionCost = getSectionDelay(forecastTime, outSection, targetCentroidID, false);
            }

            oldLinkCost = reduceCommunicationOverhead(time, oldLinkCost, sectionCost, linkTimeForecastMappings);
        }
    }

    @Override
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
                    TemporalDatabaseEntry entry = createDatabaseEntry(inSection, outSection, centroid, null);
                    advert.addLinkStateDataBaseEntry(entry);
                }
            }
        }
        return advert;
    }

    @Override
    public final void computeBorderDijkstra() {
        this.borderAdvertisementQueue.forEach(this.borderLinkstateDatabase::updateLinkCost);
        this.borderAdvertisementQueue.clear();
        new TemporalDijkstraAlgorithm().runAlgorithm(this, this.borderLinkstateDatabase, true);
    }

    @Override
    public final void computeInteriorDijkstra() {
        this.interiorAdvertisementQueue.forEach(this.interiorLinkstateDatabase::updateLinkCost);
        this.interiorAdvertisementQueue.clear();
        new TemporalDijkstraAlgorithm().runAlgorithm(this, this.interiorLinkstateDatabase, false);
    }
}

