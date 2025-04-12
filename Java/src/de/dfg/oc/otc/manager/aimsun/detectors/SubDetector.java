package de.dfg.oc.otc.manager.aimsun.detectors;

import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.layer1.observer.monitoring.SubDetectorValue;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCManagerException;
import org.apache.log4j.Logger;

import java.text.DecimalFormat;
import java.util.Observable;

/**
 * Subdetectors can have different features like speed detection or presence detection.
 *
 * @author Matthias Sommer
 */
public class SubDetector extends Observable {
    /**
     * Associated detector.
     */
    private Detector detector;
    /**
     * Refers to a DetectorCapability.
     */
    private int detectorFeature = -1;
    /**
     * Detector Identifier.
     */
    private int detectorIdentifier = -1;
    /**
     * Indicates if subdetector is enabled.
     */
    private boolean enabled;
    /**
     * ID of subdetector.
     */
    private int id = -1;
    private SubDetectorValue value;
    private final DetectorCapabilities detectorCapabilities = new DetectorCapabilities();

    public SubDetector() {
        this.value = new SubDetectorValue(-1f, -1f);
    }

    public final Detector getDetector() {
        return detector;
    }

    public final void setDetector(final Detector detector) {
        this.detector = detector;
    }

    final int getDetectorFeature() {
        return detectorFeature;
    }

    public final int getDetectorIdentifier() {
        return detectorIdentifier;
    }

    public final void setDetectorIdentifier(final int detectorIdentifier) {
        this.detectorIdentifier = detectorIdentifier;
    }

    /**
     * Liefert den Wert des SubDetectors als DetectorValue-Objekt (enth�lt Zeit
     * und Wert).
     *
     * @return DetectorValue-Objekt (Zeit und Wert).
     * @see SubDetectorValue
     */
    public final SubDetectorValue getDetectorValue() throws OTCManagerException {
        if (!enabled) {
            throw new OTCManagerException("Subdetector not active!");
        }
        return value;
    }

    public final int getId() {
        return id;
    }

    public final void setId(final int id) {
        this.id = id;
    }

    final String getSimpleDescription(final boolean includeValue) {
        if (includeValue) {
            final DecimalFormat formatter = new DecimalFormat("#.##");
            return "<tr><td>" + id + "</td><td>"
                    + detectorCapabilities.getDescription(detectorFeature)
                    + "</td><td>" + formatter.format(value.getValue()) + "</td></tr>";
        } else {
            return "<tr><td>" + id + "</td><td>"
                    + detectorCapabilities.getDescription(detectorFeature)
                    + "</td></tr>";
        }
    }

    /**
     * Liefert den Wert des SubDetectors als float.
     *
     * @return Wert des SubDetectors.
     */
    public final float getValue() throws OTCManagerException {
        if (!enabled) {
            throw new OTCManagerException("Subdetector not active!");
        }
        return value.getValue();
    }

    public final void setValue(final SubDetectorValue value) throws OTCManagerException {
        if (!enabled) {
            throw new OTCManagerException("Subdetector not active!");
        }
        this.value = value;
        setChanged();
        notifyObservers(value.clone());
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final void setEnabled(final boolean enable) {
        this.enabled = enable;
    }

    public final void reset() throws OTCManagerException {
        if (enabled) {
            this.value = new SubDetectorValue(-1, -1);
            setChanged();
            notifyObservers(null);
        }
    }

    public final void setFeature(final int feature) throws OTCManagerException {
        if (feature < 0 || feature >= DetectorCapabilities.NUM) {
            throw new OTCManagerException("Invalid Feature!");
        }
        this.detectorFeature = feature;
    }

    public final String toString() {
        final DecimalFormat formatter = new DecimalFormat("#.##");
        final String linesep = System.getProperty("line.separator");

        return "Subdetector " + id + " ("
                + OTCManager.getInstance().getNetwork().getDetectorCapabilities().getDescription(detectorFeature)
                + "): " + formatter.format(value.getValue()) + linesep;
    }
}
