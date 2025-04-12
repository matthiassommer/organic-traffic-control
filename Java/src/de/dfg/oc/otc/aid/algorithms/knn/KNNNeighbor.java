package de.dfg.oc.otc.aid.algorithms.knn;

/**
 * @author alexandermartel Wrapper class for kNNDetectorDataValue for use as one
 *         of the k-nearest neigbors adding the calculated distance as neighbor
 */
class KNNNeighbor {
    private final KNNDetectorDataValue kNNDetectorDataValue;
    private final float distance;

    KNNNeighbor(KNNDetectorDataValue kNNDetectorDataValue, float distance) {
        this.kNNDetectorDataValue = kNNDetectorDataValue;
        this.distance = distance;
    }

    KNNDetectorDataValue getkNNDetectorDataValue() {
        return kNNDetectorDataValue;
    }

    public float getDistance() {
        return distance;
    }
}
