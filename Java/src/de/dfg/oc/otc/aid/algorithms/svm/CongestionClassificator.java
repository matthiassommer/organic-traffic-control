package de.dfg.oc.otc.aid.algorithms.svm;

import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.tools.LimitedQueue;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Christoph Weiss on 14.04.2015.
 * This class includes some formula for congestion detection.
 * For further information about the formula, see the methods.
 */
public class CongestionClassificator {
    /**
     * Stores the last data. Removes automatically older values.
     */
    @NotNull
    private final LimitedQueue<Float> lastSpeedValues;
    /**
     * The Free Flow Speed of a monitoring zone to decide if street is congested or not.
     */
    private final float freeFlowSpeed;
    private float congestionSpeed;
    /**
     * Variables to store the probability distribution.
     */
    private float firstSumVariance = 0;
    private float secSumVariance = 0;
    private float newValueCount = 0;
    private float globalMean = 0;
    /**
     * Flag for storing the decision about the last value.
     */
    private boolean congested = false;
    private Definition definition = Definition.MnDOT;

    public CongestionClassificator(Definition definition, float freeFlowSpeed) {
        this.definition = definition;
        this.freeFlowSpeed = freeFlowSpeed;
        this.congestionSpeed = freeFlowSpeed * 0.7f;
        this.lastSpeedValues = new LimitedQueue<>(3);
    }

    /**
     * Function for congestion detection. Decides about the chosen Definition, if the traffic where this particular
     * DetectorDataValue was measured is congested or not.
     *
     * @param detectorValue the measured DetectorDataValue
     * @return if congested or not.
     */
    public boolean analyze(@NotNull DetectorDataValue detectorValue) {
        switch (definition) {
            case MnDOT:
                return analyzeWithMnDOT(detectorValue);
            case MnDOT_SOFT:
                return analyzeWithMnDOTSoft(detectorValue);
            case MEAN_VELOCITY:
                return analyzeWithMeanVelocity(detectorValue);
            case ANNOTATED_DATA:
                return detectorValue.isCongested();
        }
        return false;
    }

    /**
     * Formula of congestion decision in:
     * DIAMANTOPOULOS et al.
     * Use of Density-based Cluster Analysis and Classification Techniques for Traffic Congestion Prediction and Visualisation.
     * In: Transport Research Arena (TRA) 5th Conference: Transport Solutions from Research to Deployment. 2014.
     * <p>
     * This method computes the Expected Value and the standard deviation.
     * For recursive computing of expected value and variance found in
     *
     * @param detectorValue Detector value
     * @return if congested return true, false otherwise.
     * @link http://wwwex.physik.uni-ulm.de/lehre/fehlerrechnung/node17.html
     */
    private boolean analyzeWithMeanVelocity(@NotNull DetectorDataValue detectorValue) {
        float newSpeedValue = detectorValue.getValues()[DetectorCapabilities.SPEED];
        newValueCount++;

        // Store values in queue
        this.lastSpeedValues.add(newSpeedValue);

        // Expected value
        this.globalMean = (newValueCount - 1) / newValueCount * globalMean + 1 / newValueCount * newSpeedValue;

        this.firstSumVariance += newSpeedValue * newSpeedValue;
        this.secSumVariance += newSpeedValue;
        float globalVariance = 1f / newValueCount * firstSumVariance - 1f / (newValueCount * newValueCount) * secSumVariance * secSumVariance;
        double globalStandardDeviation = Math.sqrt(globalVariance);

        float localMean = 0;
        for (float value : this.lastSpeedValues) {
            localMean += value;
        }
        localMean /= this.lastSpeedValues.size();

        float localVariance = 0;
        for (float value : this.lastSpeedValues) {
            localVariance += (value - localMean) * (value - localMean);
        }
        localVariance /= (float) this.lastSpeedValues.size();
        localVariance -= localMean * localMean;

        double localStandardDeviation = Math.sqrt(Math.abs(localVariance));

        // formula for congestion decision
        float factor = 100f * localMean / this.freeFlowSpeed;
        return factor <= this.globalMean && localStandardDeviation <= globalStandardDeviation;
    }

    /**
     * Metro District Office of Operations and Maintenance - Congestion definition: equal or less than 45 Miles per hour.
     *
     * @link www.dot.state.mn.us/rtmc/reports/congestionreport2013.pdf
     */
    private boolean analyzeWithMnDOT(@NotNull DetectorDataValue detectorValue) {
        return detectorValue.getValues()[DetectorCapabilities.SPEED] <= this.congestionSpeed;
    }

    /**
     * MnDOT with persistence check. Showed to lower FAR on the cost of longer MTTD.
     * Congestion def.: if last value was congested and the current value is also congested and velocity < 45 mph
     *
     * @link www.dot.state.mn.us/rtmc/reports/congestionreport2013.pdf
     */
    private boolean analyzeWithMnDOTSoft(@NotNull DetectorDataValue detectorValue) {
        float speed = detectorValue.getValues()[DetectorCapabilities.SPEED];
        if (speed <= this.congestionSpeed && this.congested) {
            return true;
        } else if (speed <= this.congestionSpeed) {
            this.congested = true;
            return false;
        } else {
            this.congested = false;
            return false;
        }
    }

    public enum Definition {MnDOT, MnDOT_SOFT, MEAN_VELOCITY, ANNOTATED_DATA}
}