package de.dfg.oc.otc.layer1.observer.monitoring;

/**
 * Erledigt das Mapping der Detector-Capabilities.
 */
public final class DetectorCapabilities extends MeasurementCapabilities {
    public static final int COUNT = 0;
    public static final int PRESENCE = 1;
    public static final int SPEED = 2;
    public static final int OCCUPANCY = 3;
    public static final int HEADWAY = 4;
    public static final int DENSITY = 5;
    public static final int EQUIPPEDVEHICLE = 6;
    private static final int NUMOCCUPIED = 7;
    public static final int NUM = 8;

    public DetectorCapabilities() {
        numFeatures = NUM;
    }

    @Override
    public String getDescription(final int feature) {
        switch (feature) {
            case 0:
                return "COUNT";
            case 1:
                return "PRESENCE";
            case 2:
                return "SPEED";
            case 3:
                return "OCCUPANCY";
            case 4:
                return "HEADWAY";
            case 5:
                return "DENSITY";
            case 6:
                return "EQUIPPED vehicles";
            case 7:
                return "Occupied INTERVALS";
            default:
                return "Unknown feature";
        }
    }

    @Override
    public int getFeatureNumber(String description) {
        description = description.toUpperCase();
        if (description.contains("COUNT")) {
            return COUNT;
        } else if (description.contains("PRESENCE")) {
            return PRESENCE;
        } else if (description.contains("SPEED")) {
            return SPEED;
        } else if (description.contains("OCCUPANCY")) {
            return OCCUPANCY;
        } else if (description.contains("HEADWAY")) {
            return HEADWAY;
        } else if (description.contains("DENSITY")) {
            return DENSITY;
        } else if (description.contains("EQUIPPED")) {
            return EQUIPPEDVEHICLE;
        } else if (description.contains("INTERVALS")) {
            return NUMOCCUPIED;
        }
        return -1;
    }

    @Override
    public boolean isVehicleBased(final int feature) {
        return false;
    }
}
