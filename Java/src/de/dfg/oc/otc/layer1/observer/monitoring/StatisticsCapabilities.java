package de.dfg.oc.otc.layer1.observer.monitoring;

import de.dfg.oc.otc.manager.OTCManagerException;

/**
 * @author rochner
 */
public final class StatisticsCapabilities extends MeasurementCapabilities {
    public static final int DELAYTIME = 2;
    public static final int FLOW = 0;
    public static final int NUM = 9;
    public static final int NUMSTOPS = 5;
    public static final int QUEUELENGTH = 4;
    public static final int QUEUEMAX = 8;
    public static final int SPEED = 6;
    public static final int SPEEDDEVIATION = 7;
    public static final int STOPTIME = 3;
    public static final int TRAVELTIME = 1;

    public StatisticsCapabilities() {
        numFeatures = NUM;
    }

    @Override
    public String getDescription(final int feature) {
        switch (feature) {
            case 0:
                return "Traffic Flow";
            case 1:
                return "Travel Time";
            case 2:
                return "Delay Time";
            case 3:
                return "Stop Time";
            case 4:
                return "Queue Length";
            case 5:
                return "Number of Stops";
            case 6:
                return "Average Speed";
            case 7:
                return "Deviation of Speed Measurements";
            case 8:
                return "Maximum Queue Length";
            default:
                return "Unknown feature.";
        }
    }

    @Override
    public int getFeatureNumber(final String description) {
        if (description.contains("Flow")) {
            return FLOW;
        } else if (description.contains("Delay")) {
            return DELAYTIME;
        } else if (description.contains("Travel")) {
            return TRAVELTIME;
        } else if (description.contains("Queue")) {
            return QUEUELENGTH;
        } else if (description.contains("Stop")) {
            return STOPTIME;
        } else if (description.contains("Number")) {
            return NUMSTOPS;
        } else if (description.contains("Speed")) {
            return SPEED;
        } else if (description.contains("Deviation")) {
            return SPEEDDEVIATION;
        } else if (description.contains("Max Waiting")) {
            return QUEUEMAX;
        }
        return -1;
    }

    @Override
    public boolean isVehicleBased(final int feature) {
        switch (feature) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 5:
            case 6:
            case 7:
                return true;
            case 4:
            case 8:
                return false;
            default:
                throw new OTCManagerException("Unknown feature");
        }
    }
}