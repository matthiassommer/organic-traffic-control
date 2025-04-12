package de.dfg.oc.otc.layer1.observer.monitoring;

import java.util.Arrays;

/**
 * Stores measured statistics of a turning.
 */
public class StatisticalDataValue implements DataEntry {
    /**
     * Time when statistics entry was made.
     */
    private final float time;
    /**
     * A list of all measured statistics like flow, average queue size, etc.
     */
    private final float[] values;
    
    public StatisticalDataValue(final float time, final float[] data) {
        this.time = time;
        this.values = data.clone();
    }

    public final StatisticalDataValue clone() {
        return new StatisticalDataValue(time, values);
    }

    public final float getTime() {
        return time;
    }

    public final float[] getValues() {
        return values;
    }

    public final String toString() {
        return "Time: " + this.time + " values: " + Arrays.toString(this.values);
    }
}
