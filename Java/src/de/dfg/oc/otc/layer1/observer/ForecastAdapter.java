/*
 * Copyright (c) 2015. Matthias Sommer, All rights reserved.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package de.dfg.oc.otc.layer1.observer;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.tools.LimitedQueue;
import forecasting.DefaultForecastParameters;
import forecasting.ForecastModule;

/**
 * Interface for the forecast component. Enables to add time series data and to derive forecasts.
 *
 * @author Matthias Sommer.
 */
public class ForecastAdapter {
    /**
     * ForecastModule for the associated managed element.
     */
    private ForecastModule forecastModule;
    /**
     * Is forecasting active or not?
     */
    private boolean active = false;
    /**
     * Defines how many sensor values should be collected to calculate their average (equals the forecast interval).
     */
    private int forecastInterval = 1;
    /**
     * Stores the individual measurements from OTC, measured every 0.75 seconds in the simulation.
     * These sensor values are averaged in case the storage is full.
     * Moving window.
     */
    private LimitedQueue<Double> temporaryStorage;

    /**
     * Initialises the underlying forecast module.
     *
     * @param forecastInterval defines how many sensor values should be collected to calculate their average.
     */
    public ForecastAdapter(int forecastInterval) {
        this.active = DefaultForecastParameters.IS_FORECAST_MODULE_ACTIVE;
        if (this.active) {
            this.forecastInterval = forecastInterval;
            this.forecastModule = new ForecastModule();
            this.temporaryStorage = new LimitedQueue<>(forecastInterval);
        }
    }

    private double getAverageFromTemporaryStorage() {
        if (!this.temporaryStorage.isFull()) {
            return Double.NaN;
        }

        float sum = 0;
        for (double sensorValue : this.temporaryStorage) {
            sum += sensorValue;
        }
        return sum / this.temporaryStorage.size();
    }

    public float getForecastHorizon() {
        return forecastInterval * OTCManager.getSimulationStepSize();
    }

    /**
     * Pass the monitored sensor data for the evaluation and execution of forecasts to the forecast module.
     *
     * @param time  the sensor value was retrieved from Aimsun
     * @param value the sensor value
     */
    public final void addValueForForecast(float time, double value) {
        if (active) {
            this.temporaryStorage.add(value);
            this.forecastModule.addValueToEvaluators(Math.round(time), value);

            double average = getAverageFromTemporaryStorage();
            if (!Double.isNaN(average)) {
                this.forecastModule.addValue(time, average);
                this.temporaryStorage.clear();
            }
        }
    }

    /**
     * Receive the Mean Average Scaled Error (MASE) of the combined forecast.
     *
     * @return error value
     */
    public final float getForecastError() {
        if (active) {
            return (float) this.forecastModule.getCombinedForecastError();
        }
        return Float.NaN;
    }

    /**
     * Get the combined forecast for the defined step in the future.
     *
     * @param stepAhead 0 equals the current value, 1 the forecast for the next time step (1*forecast interval), etc.
     * @return forecast value
     */
    public final float getForecast(int stepAhead) {
        float time = OTCManager.getInstance().getTime();
        if (active) {
            float forecast = (float) this.forecastModule.getCombinedForecast(Math.round(time), stepAhead, time + stepAhead * this.forecastInterval);
            return forecast < 0 ? 0 : forecast;
        }
        return Float.NaN;
    }

    public final void setStorageSize(int size) {
        if (active) {
            this.temporaryStorage.setLimit(size);
            this.forecastInterval = size;
        }
    }
}
