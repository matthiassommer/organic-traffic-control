package de.dfg.oc.otc.routing.distanceVector;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.Centroid;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.Turning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Encapsulates forecast methods for anticipatory DVR protocols.
 *
 * @author Matthias Sommer.
 */
class ForecastUtilities {
    private final OTCNode node;

    ForecastUtilities(OTCNode node) {
        this.node = node;
    }

    private float findForecastForSection(float costs, Iterable<SectionForecastMessage> forecastMappings, int sectionID) {
        float bestMatch = Float.MAX_VALUE;
        float forecast = 0;

        for (SectionForecastMessage mapping : forecastMappings) {
            if (mapping.section.getId() == sectionID) {
                for (HashMap.SimpleEntry<Integer, Float> entry : mapping.forecastsMapping) {
                    int time = entry.getKey();
                    float diff = Math.abs(time - costs);

                    if (diff < bestMatch) {
                        forecast = entry.getValue();
                        bestMatch = diff;
                    } else if (diff > bestMatch) {
                        break;
                    }
                }
                break;
            }
        }
        return forecast;
    }

    SectionForecastMessage generateSectionForecasts(Section section) {
        SectionForecastMessage message = new SectionForecastMessage(section);
        for (int time = 0; time < 900; time += 90) {
            float forecast = generateSectionForecast(time, section);
            message.addEntry(new HashMap.SimpleEntry<>(time, forecast));
        }
        return message;
    }

    TurningForecastMessage generateTurningForecasts(int insectionID, int outsectionID) {
        Turning turning = node.getRoutingComponent().getJunction().getTurning(insectionID, outsectionID);
        TurningForecastMessage message = new TurningForecastMessage(turning);
        for (int time = 0; time < 900; time += 90) {
            float forecast = node.getRoutingComponent().getTurningForecast(time, insectionID, outsectionID);
            message.addEntry(new HashMap.SimpleEntry<>(time, forecast));
        }
        return message;
    }

    private float findForecastForTurning(float costs, Iterable<TurningForecastMessage> forecastMappings, int turningID) {
        float bestMatch = Float.MAX_VALUE;
        float forecast = 0;

        for (TurningForecastMessage mapping : forecastMappings) {
            if (mapping.turning.getId() == turningID) {
                for (HashMap.SimpleEntry<Integer, Float> entry : mapping.forecastsMapping) {
                    int time = entry.getKey();
                    float diff = Math.abs(time - costs);

                    if (diff < bestMatch) {
                        forecast = entry.getValue();
                        bestMatch = diff;
                    } else if (diff > bestMatch) {
                        break;
                    }
                }
                break;
            }
        }
        return forecast;
    }

    float calculateTravelTimeForPath(final List<Integer> path, List<ForecastUtilities.SectionForecastMessage> sectionMessages,
                                     List<ForecastUtilities.TurningForecastMessage> turningMessages) {
        float costs = 0;
        for (int i = path.size() - 1; i >= 0; i--) {
            int insectionID = path.get(i);
            costs += findForecastForSection(costs, sectionMessages, insectionID);

            if (i > 0) {
                int outsectionID = path.get(i - 1);
                Turning turning = OTCManager.getInstance().getNetwork().getTurning(insectionID, outsectionID);
                costs += findForecastForTurning(costs, turningMessages, turning.getId());
            }

        }
        return costs;
    }

    Section findOutsection(int insectionID, int centroidID) {
        Section insection = OTCManager.getInstance().getNetwork().getSection(insectionID);
        List<Section> outSections = insection.getDestinationSections();
        Centroid centroid = OTCManager.getInstance().getNetwork().getCentroid(centroidID);

        for (Section section : outSections) {
            if (section.getConnectedCentroids().contains(centroid)) {
                return section;
            }
        }
        return null;
    }

    private float generateSectionForecast(int time, Section section) {
        if (section.isJunctionApproach()) {
            int targetJunctionID = section.getNextJunction().getId();
            return node.getRoutingComponent().getSectionDelay(time, section, targetJunctionID, true);
        } else if (section.isExit()) {
            int targetCentroidID = section.getConnectedCentroids().get(0).getId();
            return node.getRoutingComponent().getSectionDelay(time, section, targetCentroidID, false);
        }
        return Float.NaN;
    }

    static class TurningForecastMessage {
        private final Collection<HashMap.SimpleEntry<Integer, Float>> forecastsMapping = new ArrayList<>();
        private final Turning turning;

        TurningForecastMessage(Turning turning) {
            this.turning = turning;
        }

        void addEntry(HashMap.SimpleEntry<Integer, Float> entry) {
            forecastsMapping.add(entry);
        }
    }

    static final class SectionForecastMessage {
        private final Collection<HashMap.SimpleEntry<Integer, Float>> forecastsMapping = new ArrayList<>();
        private final Section section;

        private SectionForecastMessage(Section section) {
            this.section = section;
        }

        private void addEntry(HashMap.SimpleEntry<Integer, Float> entry) {
            forecastsMapping.add(entry);
        }
    }
}
