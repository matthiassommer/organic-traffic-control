package de.dfg.oc.otc.aid.algorithms.apid;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class containing multiple occupancy and speed values from a detector
 * pair.
 */
class APIDDetectorValue {
    /**
     * List of occupancy values.
     */
    private final List<Float> occupancyValues;
    /**
     * List of speed values.
     */
    private final List<Float> speedValues;

    APIDDetectorValue() {
        occupancyValues = new ArrayList<>();
        speedValues = new ArrayList<>();
    }

    void addOccupancyValue(Float value) {
        occupancyValues.add(value);
    }

    void addSpeedValue(Float value) {
        speedValues.add(value);
    }

    /**
     * Returns the average occupancy of all stored values.
     */
    float getAverageOccupancy() {
        float occupancySum = 0;
        for (float occupancy : occupancyValues) {
            occupancySum += occupancy;
        }

        if (!occupancyValues.isEmpty()) {
            return occupancySum / occupancyValues.size();
        }
        return 0;
    }

    /**
     * Returns the average speed of all stored values.
     */
    float getAverageSpeed() {
        float speedSum = 0;
        for (float speed : speedValues) {
            speedSum += speed;
        }

        if (!speedValues.isEmpty()) {
            return speedSum / speedValues.size();
        }
        return 0;
    }
}
