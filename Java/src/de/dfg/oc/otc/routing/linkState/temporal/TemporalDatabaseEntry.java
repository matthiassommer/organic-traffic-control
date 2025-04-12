package de.dfg.oc.otc.routing.linkState.temporal;

import de.dfg.oc.otc.manager.aimsun.Centroid;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.routing.linkState.DatabaseEntry;
import de.dfg.oc.otc.routing.linkState.LinkStateRC;

import java.util.HashMap;
import java.util.List;

/**
 * This class extends the normal DataBaseEntry which is used with Advertisements sent to other routing components.
 * It holds a mapping of forecasts and the respective time steps these forecasts were calculated.
 *
 * @author Matthias Sommer, Kamuran Isik
 */
class TemporalDatabaseEntry extends DatabaseEntry {
    /**
     * A list of mappings between a flow forecast value and the time the forecast was made.
     */
    private final List<HashMap.SimpleEntry<Integer, Float>> timeTurningForecastMappings;
    private final List<HashMap.SimpleEntry<Integer, Float>> timeSectionForecastMappings;
    private final float turningCost;
    private final float sectionCost;

    public TemporalDatabaseEntry(final LinkStateRC sourceRC, final LinkStateRC destRC,
                                 final Centroid destCentroid,
                                 final Section inSection, final Section outSection,
                                 final List<HashMap.SimpleEntry<Integer, Float>> timeTurningForecastMappings,
                                 final List<HashMap.SimpleEntry<Integer, Float>> timeSectionForecastMappings,
                                 final int sequenceNumber,
                                 final float turningCost,
                                 final float sectionCost) {
        // costs jetzt einfach auf 0 gesetzt
        super(sourceRC, destRC, destCentroid, inSection, outSection, 0, -1, sequenceNumber);
        this.timeTurningForecastMappings = timeTurningForecastMappings;
        this.timeSectionForecastMappings = timeSectionForecastMappings;
        this.turningCost = turningCost;
        this.sectionCost = sectionCost;
    }

    public float getTurningCost() {
        return turningCost;
    }

    public float getSectionCost() {
        return sectionCost;
    }

    public List<HashMap.SimpleEntry<Integer, Float>> getTimeTurningForecastMappings() {
        return timeTurningForecastMappings;
    }

    public List<HashMap.SimpleEntry<Integer, Float>> getTimeSectionForecastMappings() {
        return timeSectionForecastMappings;
    }
}
