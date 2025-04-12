package de.dfg.oc.otc.aid.algorithms.knn;

import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;

/**
 * @author alexandermartel Wrapper class for DetectorDataValue adding the
 *         incident classification
 */
public class KNNDetectorDataValue {
    private final DetectorDataValue detectorDataValue;
    private final boolean incident;
    private final double[] featureArray;

    public KNNDetectorDataValue(DetectorDataValue detectorDataValue, boolean isIncident) {
        this.detectorDataValue = detectorDataValue;
        featureArray = AbstractkNNAlgorithm.getFeatureArrayOfDetectorData(this.detectorDataValue);
        this.incident = isIncident;
    }

    KNNDetectorDataValue(float time, float[] values, boolean isIncident) {
        this.detectorDataValue = new DetectorDataValue(time, values);
        featureArray = AbstractkNNAlgorithm.getFeatureArrayOfDetectorData(this.detectorDataValue);
        this.incident = isIncident;
    }

    DetectorDataValue getDetectorDataValue() {
        return detectorDataValue;
    }

    public boolean isIncident() {
        return incident;
    }

    double[] getFeatureArray() {
        return featureArray;
    }
}
