package de.dfg.oc.otc.aid.algorithms.xcsrUrban;

/**
 * Created by Dietmar on 02.02.2017.
 */

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class containing multiple occupancy and speed values from a detector
 * pair.
 */
public class XCSRUDetectorPairValue {
    /**
     * List of occupancy values.
     */
    private final List<Float> occupancyValues;
    private final List<Float> speedValues;
    private final List<Float> densityValues;
    private final List<Float> headwayValues;
    private final List<Float> countValues;

    XCSRUDetectorPairValue() {
        occupancyValues = new ArrayList<>();
        speedValues = new ArrayList<>();
        densityValues = new ArrayList<>();
        headwayValues = new ArrayList<>();
        countValues = new ArrayList<>();
//        numValues = new ArrayList<>();
    }

    void addOccupancyValue(Float value) {
        occupancyValues.add(value);
    }

    void addSpeedValue(Float value) {
        speedValues.add(value);
    }

    void addDensityValue(Float value) {
        densityValues.add(value);
    }

    void addHeadwayValue(Float value) {
        headwayValues.add(value);
    }

    void addCountValue(Float value) {
        countValues.add(value);
    }

//    void addNumValue(Float value) {
//        numValues.add(value);
//    }


    /**
     * Returns the average occupancy of all stored values.
     */
    public float getAverageOccupancy() {
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
    public float getAverageSpeed() {
        float speedSum = 0;
        for (float speed : speedValues) {
            speedSum += speed;
        }

        if (!speedValues.isEmpty()) {
            return speedSum / speedValues.size();
        }
        return 0;
    }

    /**
     * Returns the average density of all stored values.
     */
    public float getAverageDensity() {
        float sum = 0;
        for (float val : densityValues) {
            sum += val;
        }

        if (!densityValues.isEmpty()) {
            return sum / densityValues.size();
        }
        return 0;
    }

    /**
     * Returns the average density of all stored values.
     */
    public float getAverageHeadway() {
        float sum = 0;
        for (float val : headwayValues) {
            sum += val;
        }

        if (!headwayValues.isEmpty()) {
            return sum / headwayValues.size();
        }
        return 0;
    }

    /**
     * Returns the average density of all stored values.
     */
    public float getAverageCount() {
        float sum = 0;
        for (float val : countValues) {
            sum += val;
        }

        if (!countValues.isEmpty()) {
            return sum / countValues.size();
        }
        return 0;
    }
}
