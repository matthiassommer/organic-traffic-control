package de.dfg.oc.otc.layer1.observer.monitoring;

/**
 * Interface class for a data entry.
 */
public interface DataEntry {
    DataEntry clone() throws CloneNotSupportedException;

    float getTime();

    float[] getValues();
}
