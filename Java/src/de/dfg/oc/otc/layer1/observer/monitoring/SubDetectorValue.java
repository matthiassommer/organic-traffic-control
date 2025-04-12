package de.dfg.oc.otc.layer1.observer.monitoring;

/**
 * Kapselt einen Wert mit dem zugeh�rigen Zeitpunkt.
 *
 * @author rochner
 */
public class SubDetectorValue implements DataEntry {
    /**
     * Time of subdetector value.
     */
    private final float time;
    /**
     * All stored values coming from the detector at this time step.
     */
    private final float[] value;

    public SubDetectorValue(final float time, final float value) {
        this.time = time;
        this.value = new float[1];
        this.value[0] = value;
    }

    private SubDetectorValue(final float time, final float[] value) {
        this.time = time;
        this.value = value.clone();
    }

    @Override
    public final SubDetectorValue clone() {
        return new SubDetectorValue(time, value);
    }

    @Override
    public final float getTime() {
        return time;
    }

    public final float getValue() {
        return value[0];
    }

    @Override
    public final float[] getValues() {
        return value;
    }
}