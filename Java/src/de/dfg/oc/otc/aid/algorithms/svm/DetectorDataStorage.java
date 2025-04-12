package de.dfg.oc.otc.aid.algorithms.svm;

import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.tools.LimitedQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by Christoph Weiss on 04.03.2015.
 * Class stores DetectorDataValue by incoming sequence.
 * Creates vector of specified DetectorDataValues entries.
 * If no DetectorDataValue in one time slot (time step) available, a placeholder value is returned instead.
 */
public class DetectorDataStorage {
    /**
     * Stores the Detector Data Values. The newest entry is at position 0.
     */
    @NotNull
    final private LimitedQueue<DetectorDataValue> detectorDataValues;
    /**
     * Stores the additional forecastValues.
     * Elements are stored in the same order as in detectorDataValues.
     * Each entry in detectorDataValues has forecastValues at the same place in the forecastValues list
     */
    @NotNull
    final private LimitedQueue<float[]> forecastValues;
    /**
     * Count of the forecastValues at each timestep.
     */
    final private int numberForecasts;
    /**
     * Size of one simulation time step in seconds.
     */
    final private float timeStepSize;
    /**
     * This values are used, if no DetectorDataValue in the time slot is measured.
     */
    final private float[] standardCapabilityValue;
    /**
     * Mean computing is necessary, if more than one measurement in a time slot are performed.
     * If it occurred, the mean between this two measurements is computed.
     * currentMean = ( (mean+1)*a + b ) / (mean + 2)
     * a = mean over first x-Factor; b = (x+1)-Factor.
     */
    private int meanDivisor;

    /**
     * Constructor. Stores parameter and initialize the lists.
     *
     * @param storageSize             maximum number of entries to store
     * @param timeStepSize            Step size of one time slot.
     * @param standardCapabilityValue Used, if at the particular time no DetectorDataValue is measured
     */
    public DetectorDataStorage(int storageSize, float timeStepSize, float[] standardCapabilityValue, int numberForecasts) {
        this.detectorDataValues = new LimitedQueue<>(storageSize);
        this.forecastValues = new LimitedQueue<>(storageSize);
        this.numberForecasts = numberForecasts;
        this.timeStepSize = timeStepSize;
        this.standardCapabilityValue = standardCapabilityValue;
        this.meanDivisor = 0;
    }

    /**
     * 1. compute the time slot, when the DetectorDataValue is measured.
     * 2. compute the mean detectorValue over all measured DetectorDataValues in this time slot
     * 3. Store the mean in detectorDataValues.
     * <p>
     * For correct usage, DetectorDataValues have to be inserted in measured time order.
     * Otherwise, an exception is thrown.
     *
     * @param forecasts
     * @param detectorValue DetectorDataValue to store
     * @return true if new DetectorDataValue was added, false otherwise or if new mean was calculated
     */
    public boolean addValue(@NotNull DetectorDataValue detectorValue, float[] forecasts) throws IllegalArgumentException {
        // norm time in time step size
        float timeStep = detectorValue.getTime() - detectorValue.getTime() % timeStepSize;
        DetectorDataValue newDetectorValue = new DetectorDataValue(timeStep, detectorValue.getValues(), detectorValue.isCongested());
        int size = detectorDataValues.size() - 1;

        if (this.detectorDataValues.isEmpty()) {
            detectorDataValues.add(newDetectorValue);
            if (forecasts.length > 0) {
                forecastValues.add(forecasts);
            }
            return true;
        }

        // A detectorValue already exist in the same time slot -> update mean
        if (timeStep == detectorDataValues.get(size).getTime()) {
            DetectorDataValue meanValue = calculateMean(detectorValue, timeStep);

            this.detectorDataValues.removeLast();
            this.detectorDataValues.add(meanValue);
            return false;
        } else {
            // reset mean
            meanDivisor = 0;

            detectorDataValues.add(newDetectorValue);
            if (forecasts.length > 0) {
                forecastValues.add(forecasts);
            }
            return true;
        }
    }

    @NotNull
    private DetectorDataValue calculateMean(@NotNull DetectorDataValue detectorValue, float timeStep) {
        float[] newValues = detectorValue.getValues();
        float[] oldValues = this.detectorDataValues.getLast().getValues();
        for (int i = 0; i < newValues.length; i++) {
            newValues[i] = ((meanDivisor + 1) * newValues[i] + oldValues[i]) / (meanDivisor + 2);
        }
        meanDivisor++;

        return new DetectorDataValue(timeStep, newValues, detectorValue.isCongested());
    }

    /**
     * Attention: gives full access to the list. Only for direct access reading created. Necessary by tests
     *
     * @return the DetectorDataArchive list
     */
    @NotNull
    public List<DetectorDataValue> getValues() {
        return this.detectorDataValues;
    }

    /**
     * Creates an array and stores a period of one entry of the DetectorDataValue.
     * First entries are the forecasts. Then the values are stored in timestep descending order.
     * E.g. [forecasts, flow_t1, speed_t1, flow_t2, speed_t2]
     *
     * @param timeStepStart                        Time step from that moment the entries are copied
     * @param timestepsBack                        of timesteps back
     * @param detectorCapabilitiesForFeaturevector Positions in the DetectorDataValue array
     * @return the array respectively the entry vector
     */
    @NotNull
    public float[] getFeatureVector(float timeStepStart, int timestepsBack, @NotNull int[] detectorCapabilitiesForFeaturevector) {
        int size = timestepsBack * detectorCapabilitiesForFeaturevector.length + numberForecasts;
        float[] featurevector = new float[size];

        // fill the featurevector with standard values
        int position = numberForecasts;
        for (int i = 0; i < timestepsBack; i++) {
            for (int capabilityID : detectorCapabilitiesForFeaturevector) {
                featurevector[position++] = standardCapabilityValue[capabilityID];
            }
        }

        position = 0;
        for (int i = detectorDataValues.size() - 1; i >= 0; i--) {
            if (detectorDataValues.get(i).getTime() <= timeStepStart) {
                if (!forecastValues.isEmpty()) {
                    // copy forecast values
                    float timeSlot = timeStepStart - timeStepStart % timeStepSize;
                    if (detectorDataValues.get(i).getTime() == timeSlot) {
                        for (int j = 0; j < this.numberForecasts; j++) {
                            featurevector[position++] = forecastValues.get(i)[j];
                        }
                    }
                }

                // copy time series values
                int valueCounter = numberForecasts;
                for (int j = detectorDataValues.size() - 1; j >= 0; j--) {
                    if (valueCounter >= timestepsBack) {
                        break;
                    }
                    valueCounter++;

                    for (int capabilityID : detectorCapabilitiesForFeaturevector) {
                        featurevector[position++] = detectorDataValues.get(j).getValues()[capabilityID];
                    }
                }
                break;
            }
        }

        return featurevector;
    }

    /**
     * Clears the internal memory.
     */
    public void clear() {
        this.detectorDataValues.clear();
        this.forecastValues.clear();
        this.meanDivisor = 0;
    }

    /**
     * Get the second oldest value.
     * Used in the LaSVM class
     *
     * @return the second oldest stored value
     */
    @Nullable
    DetectorDataValue getSecondOldestValue() {
        if (detectorDataValues.size() > 1) {
            return detectorDataValues.get(detectorDataValues.size() - 2);
        }
        return null;
    }
}
