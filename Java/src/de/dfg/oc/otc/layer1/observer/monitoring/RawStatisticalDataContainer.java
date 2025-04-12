package de.dfg.oc.otc.layer1.observer.monitoring;

/**
 * Diese Klasse ist ein Container f�r die noch unbearbeiteten statistischen
 * Daten, wie sie von Aimsun kommen.
 *
 * @author rochner
 */
public class RawStatisticalDataContainer {
	/**
     * Average speed of vehicles.
     */
    private float averageSpeed;
    /**
     * Index der Datens�tze seit Reset. Z�hlung beginnt bei 1;
     */
    private int datasetIndex = -1;
    private float delayTime;
    /**
     * Number of cars.
     */
    private int flow;
    private float lastStatResetTime;
    /**
     * Longest queue in number of waiting cars.
     */
    private float maxQueue;
    /**
     * Average number of stops until the turning is passed.
     */
    private float numStops;
    /**
     * Average queue length.
     */
    private float queueLength;
    /**
     * The deviation of speed measurements.
     */
    private float speedDeviation;
    /**
     * Stop time in seconds in front of red lights.
     */
    private float stopTime;
    private float time;
    private float travelTime;

    /**
     * Save unprocessed data from aimsun.
     *
     * @param time           Die aktuelle Zeit
     * @param flow           Verkehrsst�rke veh/h
     * @param travelTime     Travel Time : Average sec
     * @param delayTime      Delay Time : Average sec
     * @param stopTime       Stop Time : Average sec
     * @param queueLength    Average Queue Length (veh)
     * @param numStops       Number of Stops (#/Veh)
     * @param averageSpeed   Speed : Average km/h
     * @param speedDeviation Speed : Deviation km/h
     * @param maxQueue       Maximum Queue Length (veh)
     */
    public RawStatisticalDataContainer(final float time, final float resetTime, final int flow, final float travelTime,
                                       final float delayTime, final float stopTime, final float queueLength, final float numStops,
                                       final float averageSpeed, final float speedDeviation, final float maxQueue) {
        this.time = time;
        this.lastStatResetTime = resetTime;
        this.flow = flow;
        this.travelTime = travelTime;
        this.delayTime = delayTime;
        this.stopTime = stopTime;
        this.queueLength = queueLength;
        this.numStops = numStops;
        this.averageSpeed = averageSpeed;
        this.speedDeviation = speedDeviation;
        this.maxQueue = maxQueue;
    }

    final float getAverageSpeed() {
        return averageSpeed;
    }

    /**
     * Gibt den Index des Datensatzes zur�ck. Der Index wird bei einem Reset auf
     * 1 zur�ckgesetzt.
     *
     * @return Index.
     */
    final int getDatasetIndex() {
        return datasetIndex;
    }

    final float getDelayTime() {
        return delayTime;
    }

    final int getFlow() {
        return flow;
    }

    /**
     * @return Gibt den zeitpunkt des letzten Resets vor Erzeugung dieses
     * Datensatzes zur�ck.
     */
    final float getLastReset() {
        return lastStatResetTime;
    }

    /**
     * @return the maximum number of waiting cars
     */
    final float getMaxQueueLength() {
        return maxQueue;
    }

    /**
     * @return the number of stops
     */
    final float getNumStops() {
        return numStops;
    }

    final float getQueueLength() {
        return queueLength;
    }

    /**
     * @return the deviation of speed measurements
     */
    final float getSpeedDeviation() {
        return speedDeviation;
    }

    final float getStopTime() {
        return stopTime;
    }

    final float getTime() {
        return time;
    }

    final float getTravelTime() {
        return travelTime;
    }

    /**
     * Reset des Containers, alle Messwerte auf 0.
     *
     * @param resetTime Zeitpunkt des Resets.
     */
    final void reset(final float resetTime) {
        this.time = resetTime;
        this.lastStatResetTime = resetTime;
        this.datasetIndex = 1;
        this.flow = 0;
        this.travelTime = 0;
        this.delayTime = 0;
        this.stopTime = 0;
        this.queueLength = 0;
        this.numStops = 0;
        this.averageSpeed = 0;
        this.speedDeviation = 0;
        this.maxQueue = 0;
    }

    final void setDatasetIndex(final int theIndex) {
        this.datasetIndex = theIndex;
    }
}