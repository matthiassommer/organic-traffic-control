package de.dfg.oc.otc.layer1.observer.monitoring;

import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;

/**
 * Saves data from a detector.
 *
 * @author rochner
 */
public class DetectorDataStorage extends DataStorage {
    private final Detector observedDetector;

    public DetectorDataStorage(final Detector detector) {
        super(new DetectorCapabilities());
        this.observedDetector = detector;

        for (int i = 0; i < numFeatures; i++) {
            if (!detector.getSubDetector(i).isEnabled()) {
                disableFeature(i);
            }
        }
    }

    @Override
    public final void addDatum(DataEntry datum) {
        datum = preprocessEntry(datum);

        while (entries.size() > 400) {
            entries.remove(0);
        }

        // Nur Datens�tze, die an wenigstens einer Position einen g�ltigen Wert
        // enthalten (also nicht NaN), werden gespeichert.
        for (int i = 1; i < numFeatures - numDisabledFeatures; i++) {
            if (!Float.isNaN(datum.getValues()[i])) {
                entries.add(datum);
                break;
            }
        }
        this.lastEntry = datum;

        setChanged();
        notifyObservers();
    }

    @Override
    public final float getAverage(final int feature, final float timeInterval) {
        try {
            Average rawAverage = getRawAverage(feature, timeInterval);
            return rawAverage.getValue() / rawAverage.getTimeInterval();
        } catch (OTCManagerException ome) {
            return Float.NaN;
        }
    }

    @Override
    public final float getAverage(final int feature, final int numEntries) {
        try {
            Average rawAverage = getRawAverage(feature, numEntries);
            return rawAverage.getValue() / rawAverage.getTimeInterval();
        } catch (OTCManagerException ome) {
            return Float.NaN;
        }
    }

    public final Detector getDetector() {
        return observedDetector;
    }

    @Override
    public final DetectorDataValue preprocessEntry(final DataEntry entry) {
        final float[] clearedValues = new float[numFeatures - numDisabledFeatures];

        for (int i = 0; i < numFeatures; i++) {
            if (featureMap[i] < 0) {
                continue;
            }
            final float originalValue = entry.getValues()[i];
            clearedValues[featureMap[i]] = originalValue >= 0 ? originalValue : Float.NaN;
        }
        return new DetectorDataValue(entry.getTime(), clearedValues);
    }
}