package de.dfg.oc.otc.layer1.observer.monitoring;

import de.dfg.oc.otc.manager.OTCManagerException;

import java.io.Serializable;
import java.util.Observable;

/**
 * @author rochner
 */
public abstract class AbstractObservableStatistics extends Observable implements Serializable {
    protected StatisticalDataValue statisticalValue;
    /**
     * Unprocess raw data from Aimsun.
     */
    private RawStatisticalDataContainer previousRawData;

    /**
     * Pre-processed data from AIMSUN.
     */
    public final void addRawStatisticalDataEntry(final RawStatisticalDataContainer rawData)
            throws OTCManagerException {
        boolean reset = false;

        // Falls Zeitpunkt gleich Reset -> previousData zur�cksetzen
        if (rawData.getTime() == rawData.getLastReset()) {
            reset = true;
        }

        if (this.previousRawData == null) {
            this.previousRawData = rawData;
            this.previousRawData.setDatasetIndex(1);

            if (reset) {
                this.previousRawData.reset(rawData.getTime());
            }
            return;
        }

        if (!reset && previousRawData.getLastReset() != rawData.getLastReset()) {
            throw new OTCManagerException("Reset Zeitpunkte passen nicht!");
        }

        rawData.setDatasetIndex(previousRawData.getDatasetIndex() + 1);

        setStatisticalValue(rawData);

        setChanged();
        notifyObservers(statisticalValue.clone());

        // Falls Zeitpunkt gleich Reset -> previousData zur�cksetzen.
        if (reset) {
            previousRawData.reset(rawData.getTime());
        } else {
            previousRawData = rawData;
        }
    }

    private void setStatisticalValue(RawStatisticalDataContainer rawData) {
        final int vehiclesSinceLastTimeStep = Math.round(rawData.getFlow() / 3600f
                * (rawData.getTime() - previousRawData.getLastReset()));
        final int vehiclesInThePreviousTimeStep = Math.round(previousRawData.getFlow() / 3600f
                * (previousRawData.getTime() - previousRawData.getLastReset()));
        final int flow = vehiclesSinceLastTimeStep - vehiclesInThePreviousTimeStep;

        final int queueLength = Math.round(rawData.getQueueLength() * rawData.getDatasetIndex()
                - previousRawData.getQueueLength() * previousRawData.getDatasetIndex());

        // TODO: Muss der Wert noch umgerechnet werden?
        final int maxQueueLength = Math.round(rawData.getMaxQueueLength());

        float travelTime = Float.NaN;
        float delayTime = Float.NaN;
        float stopTime = Float.NaN;
        float numStops = Float.NaN;
        float averageSpeed = Float.NaN;
        float speedDeviation = Float.NaN;

        if (flow > 0) {
            travelTime = rawData.getTravelTime() * vehiclesSinceLastTimeStep - previousRawData.getTravelTime()
                    * vehiclesInThePreviousTimeStep;
            delayTime = rawData.getDelayTime() * vehiclesSinceLastTimeStep - previousRawData.getDelayTime()
                    * vehiclesInThePreviousTimeStep;
            stopTime = rawData.getStopTime() * vehiclesSinceLastTimeStep - previousRawData.getStopTime()
                    * vehiclesInThePreviousTimeStep;
            numStops = rawData.getNumStops() * vehiclesSinceLastTimeStep - previousRawData.getNumStops()
                    * vehiclesInThePreviousTimeStep;
            averageSpeed = rawData.getAverageSpeed() * vehiclesSinceLastTimeStep - previousRawData.getAverageSpeed()
                    * vehiclesInThePreviousTimeStep;
            speedDeviation = rawData.getSpeedDeviation() * vehiclesSinceLastTimeStep
                    - previousRawData.getSpeedDeviation() * vehiclesInThePreviousTimeStep;
        }

        this.statisticalValue = new StatisticalDataValue(rawData.getTime(), new float[]{flow, travelTime,
                delayTime, stopTime, queueLength, numStops, averageSpeed, speedDeviation, maxQueueLength});
    }

    public abstract int getId();

    public abstract String printStatisticalData();
}
