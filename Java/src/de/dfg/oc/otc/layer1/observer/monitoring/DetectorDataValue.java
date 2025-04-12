package de.dfg.oc.otc.layer1.observer.monitoring;

/**
 * DetectorValue consists of these values:
 * 1: Count, 2: Presence, 3: Speed, 4: Occupied Time Percentage, 5: Headway, 6: Density, 7: EquippedVehicle
 * that are passed by an detector (Aimsun) to OTC marked with a time stamp.
 */
public class DetectorDataValue implements DataEntry {
    /**
     * Time of detector value.
     */
    private final float time;
    /**
     * All stored values coming from the detector at this time step.
     */
    private final float[] values;
    /**
     * Set during offline training if training set is annotated.
     */
    private boolean isCongested;

    public void setAsAnnotated() {
        this.annotatedData = true;
    }

    public boolean isAnnotatedData() {
        return annotatedData;
    }

    private boolean annotatedData = false;

    public DetectorDataValue(final float time, final float[] values) {
        this.time = time;
        this.values = values.clone();
    }

    public DetectorDataValue(final float time, final float[] values, boolean isCongested) {
        this.time = time;
        this.values = values.clone();
        this.isCongested = isCongested;
    }

    public boolean isCongested() {
        return isCongested;
    }

    @Override
    public final DetectorDataValue clone() {
        return new DetectorDataValue(time, values);
    }

    @Override
    public final float getTime() {
        return time;
    }

    @Override
    public final float[] getValues() {
        return values;
    }
}
