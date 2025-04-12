package de.dfg.oc.otc.aid.evaluation;

/**
 * In dieser Klasse werden die Staumetriken berechnet.
 *
 * @see "Use of Density-based Cluster Analysis and Classification Techniques
 * for Traffic Congestion Prediction and Visualization",
 * T. Diamantopoulos, D. Kehagias, F. Konig, D. Tzovaras
 */
public abstract class CongestionMetrics {

    /**
     * Calculates the Matthews Correlation Coefficient which contains all four measures
     * @param TP true positive
     * @param TN true negative
     * @param FP false positive
     * @param FN false negative
     * @return MCC
     */
    public static float mcc(float TP, float TN, float FP, float FN)
    {
        float d = (TP+FP)*(TP+FN)*(TN+FP)*(TN+FN);
        if (d == 0.0f)
            return 0.0f;
        return (TP * TN - FP * FN) / (float) Math.sqrt(d);
    }

    /**
     * precision denotes the percentage of predicted jams that were correctly classified as jams.
     * predicting few jams may result in very high precision since false positives are minimized.
     *
     * @param TP true positive
     * @param FP false positive
     * @return precision level [0;1]
     */
    public static float precision(float TP, float FP) {
        if (TP + FP == 0) {
            return 0;
        }
        return TP / (TP + FP);
    }

    /**
     * recall denotes the percentage of jams that were successfully predicted. Achieving high recall is
     * trivial; an algorithm may just classify all roads as congested.
     * Number of true positive assessment/Number of all positive assessment
     *
     * @param TP true positive
     * @param FN false negative
     * @return recall level [0;1]
     */
    public static float recall(float TP, float FN) {
        if (TP + FN == 0) {
            return 0;
        }
        return TP / (TP + FN);
    }

    /**
     * Classifying all roads as free flow achieves quite high accuracy
     * since congested roads are generally much fewer.
     * Number of correct assessments/Number of all assessments
     *
     * @param TP true positive
     * @param TN true negative
     * @param FP false positive
     * @param FN false negative
     * @return accuracy level [0;1]
     */
    public static float accuracy(float TP, float TN, float FP, float FN) {
        if (TP + TN + FP + FN == 0) {
            return 0;
        }
        return (TP + TN) / (TP + TN + FP + FN);
    }

    /**
     * F1-Score with beta=1.
     *
     * @param TP true positive
     * @param FP false positive
     * @param FN false negative
     * @return F-measure level [0;1]
     */
    public static float fmeasure(float TP, float FP, float FN) {
        float precision = precision(TP, FP);
        float recall = recall(TP, FN);

        if (precision + recall == 0) {
            return 0;
        }
        return (2 * precision * recall) / (precision + recall);
    }

    /**
     * Number of true negative assessment/Number of all negative assessment
     *
     * @param TN true negative
     * @param FP false positive
     * @return specificity level [0;1]
     */
    public static float specificity(float TN, float FP) {
        if (FP + TN == 0) {
            return 0;
        }
        return TN / (FP + TN);
    }

    /**
     * Defined analogously to recall.
     *
     * @param TP true positive
     * @param FN false negative
     * @return sensitivity level [0;1]
     */
    public static float sensitivity(float TP, float FN) {
        return recall(TP, FN);
    }
}
