package de.dfg.oc.otc.layer1.observer;

import de.dfg.oc.otc.layer1.observer.Layer1Observer.DataSource;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.TrafficType;
import de.dfg.oc.otc.manager.aimsun.Turning;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Returns situation descriptions for a node containing attribute values for all its turnings.
 *
 * @author Matthias Sommer
 */
public class SituationAnalyser {
    private float lastExportTime = 0;
    private float[] lastForecasts;
    private final OTCNode node;

    public SituationAnalyser(OTCNode node) {
        this.lastForecasts = new float[]{};
        this.node = node;
    }

    /**
     * Build a situation description combining the forecasts and the current situation.
     * The forecast is only considered if the MASE is below 1.
     *
     * @param situation of intersection
     * @param forecasts for all turnings
     * @param errors    of predictions
     * @return adjustedSituation
     */
    private float[] adjustFlowsByForecasts(float[] situation, float[] forecasts, float[] errors) {
        float[] adjustedSituation = new float[situation.length];

        for (int i = 0; i < situation.length; i++) {
            float error = Math.abs(errors[i]);
            float forecast = forecasts[i];

            // Higher error values indicate inaccurate forecasts => Don't consider forecast.
            final float maxError = 1.0f;
            if (!Double.isNaN(error) && !Float.isNaN(forecast) && error <= maxError) {
                float alpha = error / maxError;
                adjustedSituation[i] = alpha * situation[i] + (1 - alpha) * forecast;
            } else {
                adjustedSituation[i] = situation[i];
            }
        }
        return adjustedSituation;
    }

    /**
     * Get a list of forecast errors of all {@link Turning}s of a{@link OTCNode}.
     */
    private float[] getForecastErrors() {
        List<Turning> turnings = node.getJunction().getTurnings(TrafficType.INDIVIDUAL_TRAFFIC);

        float[] errors = new float[turnings.size()];
        for (int i = 0; i < turnings.size(); i++) {
            errors[i] = turnings.get(i).getFlowForecaster().getForecastError();
        }
        return errors;
    }

    /**
     * Get the one-step forecast (the cycle time of the junction) for
     * each turning of an intersection.
     *
     * @return forecasted situation (size = number of turnings)
     */
    private float[] getForecastedSituation() {
        List<Turning> turnings = node.getJunction().getTurnings(TrafficType.INDIVIDUAL_TRAFFIC);

        float[] situation = new float[turnings.size()];
        for (int i = 0; i < turnings.size(); i++) {
            situation[i] = turnings.get(i).getFlowForecaster().getForecast(1);
        }
        return situation;
    }

    /**
     * Get the current situation for this intersection.
     *
     * @param source    data source (Statistik oder Detektoren).
     * @param attribute Attribut bezüglich dessen die Situationsbeschreibung erstellt
     *                  werden soll.
     * @param interval  Intervall (in Sekunden), über das die Eingangswerte gemittelt
     *                  werden sollen.
     * @return Array mit Situationsbeschreibung
     * @throws IllegalArgumentException wenn eine ungültige Datenquelle angegeben wurde.
     * @throws OTCManagerException      wenn keine gültige Situation bestimmt werden konnte.
     */
    public float[] getSituation(DataSource source, int attribute, float interval) throws IllegalArgumentException {
        switch (source) {
            case STATISTICS:
                return node.getLayer1Observer().getStatisticsObserver(TrafficType.INDIVIDUAL_TRAFFIC)
                        .getSituation(interval, attribute);
            case DETECTOR:
                return node.getL1DetectorObserver().getSituation(interval, attribute);
            default:
                throw new IllegalArgumentException("Invalid data source");
        }
    }

    /**
     * Returns a situation description for the node for the given attribute.
     *
     * @param source    data source (Statistik oder Detektoren).
     * @param attribute Attribut bez�glich dessen die Situationsbeschreibung erstellt
     *                  werden soll.
     * @return description
     * @throws IllegalArgumentException wenn eine ungültige Datenquelle angegeben wurde
     * @throws OTCManagerException      wenn keine gültige Situation bestimmt werden konnte.
     */
    public String[] getSituationDescription(DataSource source, int attribute)
            throws IllegalArgumentException, OTCManagerException {
        switch (source) {
            case STATISTICS:
                return node.getLayer1Observer().getStatisticsObserver(TrafficType.INDIVIDUAL_TRAFFIC)
                        .getSituationDescription(attribute);
            case DETECTOR:
                return new String[0];
            default:
                throw new IllegalArgumentException("Invalid data source");
        }
    }

    /**
     * Combine forecasts and current flow values for a situation description.
     *
     * @param source    for situation description
     * @param attribute for description
     * @param interval  to aggregate attribute
     * @return situation description
     * @throws IllegalArgumentException
     * @throws OTCManagerException
     */
    public float[] getSituationWithFlowForecast(DataSource source, int attribute, float interval) throws IllegalArgumentException, OTCManagerException {
        // this situation description represents the actual monitored traffic demands
        float[] situation = getSituation(source, attribute, interval);
        // this vector contains one-step forecasts of future traffic demands for each turning movement
        float[] forecastedSituation = getForecastedSituation();
        float[] forecastErrors = getForecastErrors();

        // this situation description goes to the controller
        float[] adjustedSituation = adjustFlowsByForecasts(situation, forecastedSituation, forecastErrors);

        exportLog(node.getId(), situation, forecastedSituation, adjustedSituation);

        return adjustedSituation;
    }

    /**
     * Write data to a file containing the situation, forecasted and adjusted situation.
     * <p>
     * The forecasts stored are the one-step forecasts from the last time step.
     * Therefore, the actual and the forecast values are stored on the same line.
     * <p>
     * Time, Actual Situation (t=Time), Forecasted Situation (t=Time+1), Adjusted Situation (t=Time)
     */
    private void exportLog(int nodeID, float[] situation, float[] forecasts, float[] adjustedSituation) {
        try {
            float timeStep = OTCManager.getInstance().getTime();
            if (lastExportTime != timeStep) {
                int replicationID = OTCManager.getInstance().getReplicationID();

                for (int i = 0; i < lastForecasts.length; i++) {
                    File file = new File("logs/repl" + replicationID + "_flow_forecast_node" + nodeID + "_turn" + i + ".txt");

                    if (!Float.isNaN(lastForecasts[i])) {
                        String output = timeStep + "\t" + situation[i] + "\t" + lastForecasts[i] + "\t" + adjustedSituation[i] + "\n";
                        FileUtils.writeStringToFile(file, output, true);
                    }
                }

                lastExportTime = timeStep;
                lastForecasts = forecasts;
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
