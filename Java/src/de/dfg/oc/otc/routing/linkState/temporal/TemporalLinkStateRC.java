package de.dfg.oc.otc.routing.linkState.temporal;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.*;
import de.dfg.oc.otc.routing.ProtocolType;
import de.dfg.oc.otc.routing.linkState.Advertisement;
import de.dfg.oc.otc.routing.linkState.LinkStateRC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class extends the LinkStateRC with forecasts.
 *
 * @author Matthias Sommer, Kamuran Isik
 */
public class TemporalLinkStateRC extends LinkStateRC {
    public TemporalLinkStateRC(final ProtocolType type, final OTCNode node) {
        super(type, node);
    }

    /**
     * Determines the local LinkStateDataDaseEntries with the current traffic situation and forecast.
     * Create an advertisement with database entries for the current and future flow of all turnings
     * and outgoing sections of this node.
     *
     * @return LinkStateAdvert with the local LinkStateDataDaseEntries
     */
    @Override
    protected Advertisement determineLocalAdvertisement() {
        sequenceNumberCounter++;

        final Advertisement advertisement = new Advertisement(this, sequenceNumberCounter);
        final AimsunJunction junction = this.getJunction();

        // Determine link costs for all turnings to all neighbouring RCs and centroids
        for (Turning turning : junction.getTurnings(TrafficType.INDIVIDUAL_TRAFFIC)) {
            TemporalDatabaseEntry entry = createDatabaseEntry(turning.getInSection(), turning.getOutSection());
            advertisement.addLinkStateDataBaseEntry(entry);
        }

        return advertisement;
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
        final float threshold = 2;

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
     * Determines the forecasts list for a {@link Turning} (specified by its in- and outbound section)
     * and for its outbound {@link Section}.
     *
     * @param inSection  inbound section of the {@link Turning}
     * @param outSection outbound section of the {@link Turning}
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

    /**
     * Create a new database entry.
     *
     * @param inSection
     * @param outSection
     * @return database entry
     */
    private TemporalDatabaseEntry createDatabaseEntry(final Section inSection, final Section outSection) {
        List<HashMap.SimpleEntry<Integer, Float>> timeTurningForecastMappings = new ArrayList<>();
        List<HashMap.SimpleEntry<Integer, Float>> timeLinkForecastMappings = new ArrayList<>();

        determineForecastMappings(inSection, outSection, timeTurningForecastMappings, timeLinkForecastMappings);

        final float turningCost = determineTurningCost(inSection.getId(), outSection.getId());
        final float linkCost = getDynamicDelayForSection(outSection.getId());

        final AimsunJunction nextJunction = outSection.getNextJunction();
        if (nextJunction != null) {
            // next node is junction
            final TemporalLinkStateRC neighbourRC = (TemporalLinkStateRC) nextJunction.getNode().getRoutingComponent();
            return new TemporalDatabaseEntry(this, neighbourRC, null, inSection, outSection, timeTurningForecastMappings,
                    timeLinkForecastMappings, sequenceNumberCounter, turningCost, linkCost);
        } else {
            // next node is centroid
            final Integer destination = this.outSectionDestinations.get(outSection.getId());
            if (destination != null) {
                final Centroid centroid = OTCManager.getInstance().getNetwork().getCentroid(destination);
                return new TemporalDatabaseEntry(this, null, centroid, inSection, outSection, timeTurningForecastMappings,
                        timeLinkForecastMappings, sequenceNumberCounter, turningCost, linkCost);
            }
        }
        throw new IllegalArgumentException("Centroid or RC must not be null");
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

        //Update LinksStateDatabase with queue
        interiorAdvertisementQueue.forEach(interiorLinkstateDatabase::updateLinkCost);
        interiorAdvertisementQueue.clear();

        new TemporalDijkstraAlgorithm().runAlgorithm(this, interiorLinkstateDatabase, false);
    }
}