package de.dfg.oc.otc.manager.aimsun;

/**
 * These class holds all section road types available in AIMSUN.
 * <p>
 * Internal sections are part of a junction.
 */
public enum RoadType {
    NOT_SPECIFIED, ARTERIAL, ROAD, FREEWAY, RINGROAD, ONOFFRAMP, URBANROAD, STREET, SIGNALIZEDSTREET, PUBLICTRANSPORT, FOOTPATH, INTERNAL, PEDESTRIANAREA, TRAM_ROUTE, THIRTY_ZONE;

    /**
     * Returns the RoadType-Object matching the ID, if one exists (otherwise null).
     *
     * @param roadTypeName Namens-String, zu der das passende RoadType-Objekt gesucht wird.
     * @return RoadType object.
     */
    static RoadType getTypeForId(final int roadTypeName) {
        switch (roadTypeName) {
            case 49:
                return RoadType.ARTERIAL;
            case 50:
                return RoadType.ROAD;
            case 51:
                return RoadType.FREEWAY;
            case 52:
                return RoadType.RINGROAD;
            case 53:
                return RoadType.ONOFFRAMP;
            case 54:
                return RoadType.URBANROAD;
            case 55:
                return RoadType.STREET;
            case 56:
                return RoadType.SIGNALIZEDSTREET;
            case 263:
                return RoadType.FOOTPATH;
            case 243:
            case 10000420:
                return RoadType.INTERNAL;
            case 36:
                return RoadType.PEDESTRIANAREA;
            case 2516:
                return RoadType.THIRTY_ZONE;
            case 2517:
                return RoadType.TRAM_ROUTE;
            default:
                return RoadType.ARTERIAL;
        }
    }
}
