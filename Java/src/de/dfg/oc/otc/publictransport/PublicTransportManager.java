package de.dfg.oc.otc.publictransport;

import de.dfg.oc.otc.config.DefaultParams;

import java.util.HashMap;
import java.util.Map;

/**
 * This class assembels all the necessary data for public transport signal priority change
 * and holds them in a List of {@Link PublicTransportDetector}s for easy access.
 */
public class PublicTransportManager {
    private static final PublicTransportManager INSTANCE = new PublicTransportManager();
    private final Map<Integer, PublicTransportLine> lineMap;
    private final int active;

    private PublicTransportManager() {
        this.active = DefaultParams.PT_FEATURE_SETTING;
        this.lineMap = new HashMap<>();
    }

    public static PublicTransportManager getInstance() {
        return INSTANCE;
    }

    public void addPublicTransportLine(int id, PublicTransportLine line) {
        this.lineMap.put(id, line);
    }

    public Map<Integer, PublicTransportLine> getPublicTransportLines() {
        return lineMap;
    }

    public void reset() {
        if (active != 0) {
            lineMap.values().forEach(PublicTransportLine::manageTLCReset);
        }
    }
}