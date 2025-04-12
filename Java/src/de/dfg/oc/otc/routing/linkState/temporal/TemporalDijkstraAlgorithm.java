package de.dfg.oc.otc.routing.linkState.temporal;

import de.dfg.oc.otc.manager.aimsun.Centroid;
import de.dfg.oc.otc.manager.aimsun.Turning;
import de.dfg.oc.otc.routing.RouteEntry;
import de.dfg.oc.otc.routing.RoutePriorityEntry;
import de.dfg.oc.otc.routing.RoutingComponent;
import de.dfg.oc.otc.routing.linkState.Advertisement;
import de.dfg.oc.otc.routing.linkState.DijkstraAlgorithm;
import de.dfg.oc.otc.routing.linkState.LinkStateRC;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;

/**
 * Implements Dijkstra's algorithm to use on a {@link de.dfg.oc.otc.routing.NetworkGraph} to determine
 * the shortest paths to centroids.
 * <p>
 * Matthias Sommer, Kamuran Isik
 */
public class TemporalDijkstraAlgorithm extends DijkstraAlgorithm {
    @Override
    protected void updateOrInsertCostsForCentroids(final List<RouteEntry> localCentroidEntries,
                                                   final RoutePriorityEntry priorityEntry) {
        for (RouteEntry entry : localCentroidEntries) {
            final RoutingComponent node = priorityEntry.getDestinationRC();
            final Centroid target = entry.getDestinationCentroid();

            final Float oldCostsToTarget = centroidToCost.get(target);
            final float costs = priorityEntry.getCosts();

            // TurningCost
            final List<HashMap.SimpleEntry<Integer, Float>> turningForecastMappings = ((TemporalDatabaseEntry) entry).getTimeTurningForecastMappings();
            float turningForecast = 0;
            if (turningForecastMappings != null) {
                turningForecast = timeForecastMapForEntry(costs, turningForecastMappings);
            }

            // SectionCost
            final List<HashMap.SimpleEntry<Integer, Float>> linkForecastMappings =
                    ((TemporalDatabaseEntry) entry).getTimeSectionForecastMappings();
            float linkForecast = 0;
            if (linkForecastMappings != null) {
                linkForecast = timeForecastMapForEntry(costs, linkForecastMappings);
            }

            Turning turning = node.getJunction().getTurning(entry.getInSection().getId(), entry.getOutSection().getId());
            final Float newCostsToTarget = determineNewCostToNode(turning, entry, turningForecast, linkForecast, costs);

            if (oldCostsToTarget != null) {
                if (newCostsToTarget < oldCostsToTarget) {
                    // Exchange old priority entry with the new one with lower costs
                    final RoutePriorityEntry oldPriorityEntry = centroidToPriorityEntry.get(target);
                    entryPriorityQueue.remove(oldPriorityEntry);
                } else {
                    continue;
                }
            }

            centroidToCost.put(target, newCostsToTarget);

            final RoutePriorityEntry newPriorityEntry = new RoutePriorityEntry(entry, newCostsToTarget);
            entryPriorityQueue.add(newPriorityEntry);

            centroidToPriorityEntry.put(target, newPriorityEntry);
            centroidToPredecessor.put(target, entry.getSourceRC());
        }
    }

    @Override
    protected void updateOrInsertCostsForRC(final List<RouteEntry> localRcEntries,
                                            final RoutePriorityEntry priorityEntry) {
        for (RouteEntry entry : localRcEntries) {
            final RoutingComponent destinationRC = entry.getDestinationRC();

            final Float oldCostsToNode = rcToCost.get(destinationRC);
            final float costs = priorityEntry.getCosts();
            final Float newCostsToNode;

            // Workaround for initial source entries
            if (!(entry instanceof TemporalDatabaseEntry)) {
                newCostsToNode = priorityEntry.getCosts() + entry.getCosts();
            } else {
                // Turning Cost
                final List<HashMap.SimpleEntry<Integer, Float>> turningForecastMappings =
                        ((TemporalDatabaseEntry) entry).getTimeTurningForecastMappings();
                float turningForecast = timeForecastMapForEntry(costs, turningForecastMappings);

                // Section Cost
                final List<HashMap.SimpleEntry<Integer, Float>> linkForecastMappings =
                        ((TemporalDatabaseEntry) entry).getTimeSectionForecastMappings();
                float linkForecast = timeForecastMapForEntry(costs, linkForecastMappings);

                Turning turning = priorityEntry.getDestinationRC().getJunction().getTurning(entry.getInSection().getId(), entry.getOutSection().getId());
                newCostsToNode = determineNewCostToNode(turning, entry, turningForecast, linkForecast, costs);
            }

            if (oldCostsToNode != null) {
                if (newCostsToNode < oldCostsToNode) {
                    // Exchange old priority entry with the new one with lower costs
                    final RoutePriorityEntry oldPriorityEntry = rcToPriorityEntry.get(destinationRC);
                    entryPriorityQueue.remove(oldPriorityEntry);
                } else {
                    continue;
                }
            }

            rcToCost.put(destinationRC, newCostsToNode);

            final RoutePriorityEntry newPriorityEntry = new RoutePriorityEntry(entry, newCostsToNode);
            entryPriorityQueue.add(newPriorityEntry);

            rcToPriorityEntry.put(destinationRC, newPriorityEntry);
            rcToPredecessor.put(destinationRC, entry.getSourceRC());
        }
    }

    /**
     * Get a forecast or the actual value out of the list of forecastMappings to the next centroid or RC.
     *
     * @param forecastMappings list of forecasts
     * @param costs            costs of priorityEntry
     * @return forecast of the {@link de.dfg.oc.otc.manager.aimsun.Turning} and {@link de.dfg.oc.otc.manager.aimsun.Section}
     */

    private float timeForecastMapForEntry(float costs, List<HashMap.SimpleEntry<Integer, Float>> forecastMappings) {
        float bestDifference = Float.MAX_VALUE;

        float forecast = 0;
        for (AbstractMap.SimpleEntry<Integer, Float> mapping : forecastMappings) {
            if (mapping.getKey() > -1 && mapping.getValue() > -1) {
                final int time = mapping.getKey();
                float diff = Math.abs(time - costs);

                if (diff < bestDifference) {
                    forecast = mapping.getValue();
                    bestDifference = diff;
                }
            }
        }
        return forecast;
    }

    private float determineNewCostToNode(Turning turning, RouteEntry entry, float turningForecast,
                                         float sectionForecast, float costs) {
        if (turning != null) {
            // turning Values
            double turningError = turning.getFlowForecaster().getForecastError();
            float turningCost = ((TemporalDatabaseEntry) entry).getTurningCost();

            // section values
            double linkError = entry.getOutSection().getFlowForecaster().getForecastError();
            float linkCost = ((TemporalDatabaseEntry) entry).getSectionCost();

            final double forecast = weigthForecast(turningCost, turningError, linkCost, linkError, turningForecast, sectionForecast);

            return (float) (costs + forecast);
        }
        return costs + turningForecast + sectionForecast;
    }

    /**
     * Exponential smoothing allows to determine weighting of statically estimated value and forecast value.
     * The lower the forecast error MASE the higher weight the forecast.
     *
     * @param turningCost          statically estimated value of turning
     * @param turningForecastError error value of turning
     * @param sectionCost          statically estimated value of outSection
     * @param sectionForecastError error of outSection
     * @param turningForecast      forecast value of turning
     * @param sectionForecast      forecast value of outSection
     * @return forecast for considered entry
     */
    private double weigthForecast(final float turningCost, final double turningForecastError,
                                 final float sectionCost, final double sectionForecastError,
                                 final float turningForecast, final float sectionForecast) {
        double smoothedTravelTimeTurning;
        double smoothedTravelTimeSection;

        final float maxError = 0.9f;

        // determine turning Cost
        if (turningForecastError < maxError) {
            smoothedTravelTimeTurning = (1 - turningForecastError) * turningForecast + turningForecastError * turningCost;
        } else {
            smoothedTravelTimeTurning = 0.1 * turningForecast + 0.9 * turningCost;
        }

        // determine section Cost
        if (sectionForecastError < maxError) {
            smoothedTravelTimeSection = (1 - sectionForecastError) * sectionForecast + sectionForecastError * sectionCost;
        } else {
            smoothedTravelTimeSection = 0.1 * sectionForecast + 0.9 * sectionCost;
        }

        return smoothedTravelTimeTurning + smoothedTravelTimeSection;
    }

    @Override
    protected void sendBorderAdvertisement(final LinkStateRC sourceRC, final Advertisement virtualBorderLinkAdvert) {
        if (sourceRC instanceof RegionalTemporalLSRC) {
            final RegionalTemporalLSRC routingComponent = (RegionalTemporalLSRC) sourceRC;
            if (routingComponent.getRegionalRCType() == RoutingComponent.RegionalType.BORDER) {
                routingComponent.setDijkstraAdvertisement(virtualBorderLinkAdvert);
            }
        }
    }
}
