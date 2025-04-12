package tests.evaluation.aid.laSVM;

import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.aid.algorithms.svm.CongestionClassificator;
import de.dfg.oc.otc.aid.algorithms.svm.IncidentStorage;
import de.dfg.oc.otc.layer1.observer.ForecastAdapter;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.manager.OTCManager;
import org.junit.Test;
import tests.evaluation.aid.AIDTrafficDataReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Run test of ForecastAdapter, Baseline Algorithm and SVMs
 */
public class TestReferenceAlgorithms {
    private final String evaluation_base = EvaluationUtilities.EVALUATION_FOLDER + "Andere Verfahren\\";

    @Test
    public void runSimple() {
        runSimpleAlgorithm("2448", "S637");
        runSimpleAlgorithm("366", "S86");
    }

    @Test
    // TODO Kontrollieren und �berarbeiten
    public void runForecastModule() {
        runForecastModule("2448", "S637");
        runForecastModule("366", "S86");
    }

    /**
     * Works on the basis: If is congested now, it will be congested in the next time step and vice versa.
     *
     * @param detector
     * @param station
     * @return
     */
    private void runSimpleAlgorithm(String detector, String station) {
        float timeStepSize = (float) EvaluationUtilities.getParameter().get("timeStepSize");

        CongestionClassificator congestionClassificator = new CongestionClassificator(CongestionClassificator.Definition.ANNOTATED_DATA, 0);
        String incidentFiles = evaluation_base + "Baseline\\incidentFiles\\incidentArchiv" + station + "D" + detector + ".csv";
        IncidentStorage incidentStorage = new IncidentStorage(incidentFiles);

        List<DetectorDataValue> allValues = new ArrayList<>();
        float timestepStart = 0;

        for (int i = 1; i <= EvaluationUtilities.endDateTesting; i++) {
            String filename = "";
            if (station.equals("S637")) {
                filename = EvaluationUtilities.getFileNameForDetectorStation("I35E", detector, i, "2013070", "201307");
            }
            if (station.equals("S86")) {
                filename = EvaluationUtilities.getFileNameForDetectorStation("I94", detector, i, "2015060", "201506");
            }

            List<DetectorDataValue> updateValues = AIDTrafficDataReader.readDetectorDataFromFile(filename, timestepStart);
            allValues.addAll(updateValues.stream().collect(Collectors.toList()));

            timestepStart = updateValues.get(updateValues.size() - 1).getTime() + timeStepSize;
        }

        for (DetectorDataValue value : allValues) {
            if (congestionClassificator.analyze(value)) {
                incidentStorage.classifyIncident(value.getTime(), true);
                Incident incident = new Incident();
                incident.setStartTime(value.getTime() + timeStepSize);
                incident.setConfirmed(false);
                incidentStorage.storeIncident(incident);
            } else {
                incidentStorage.classifyIncident(value.getTime(), false);
            }
        }

        EvaluationUtilities.evaluateTestResultPerDay(incidentStorage, 0, evaluation_base + "Baseline\\S637ResultsBaseline.csv");
        EvaluationUtilities.evaluateTestResultPerDay(incidentStorage, 0, evaluation_base + "Baseline\\S86ResultsBaseline.csv");
    }

    private void runForecastModule(String detector, String station) {
        Map<String, Object> parameter = EvaluationUtilities.getParameter();
        int timeStepsForward = (int) parameter.get("timeStepsForward");
        float timeStepSize = (float) parameter.get("timeStepSize");

        ForecastAdapter forecastModule = new ForecastAdapter((int) timeStepSize);

        List<DetectorDataValue> allValues = new ArrayList<>();
        float timestepStart = 0;

        // training
        for (int i = 17; i <= 30; i++) {
            String filename = "";
            if (station.equals("S637")) {
                filename = EvaluationUtilities.getFileNameForDetectorStation("I35E", detector, i, "2013060", "201306");
            } else if (station.equals("S86")) {
                filename = EvaluationUtilities.getFileNameForDetectorStation("I94", detector, i, "2015050", "201505");
            }

            List<DetectorDataValue> updateValues = AIDTrafficDataReader.readDetectorDataFromFile(filename, timestepStart);
            allValues.addAll(updateValues);

            timestepStart = updateValues.get(updateValues.size() - 1).getTime() + timeStepSize;
        }

        for (DetectorDataValue value : allValues) {
            OTCManager.getInstance().setTimeForTests(value.getTime());
            forecastModule.addValueForForecast(value.getTime(), value.getValues()[DetectorCapabilities.SPEED]);
        }
        allValues.clear();

        // testing: use the ForecastModule
        for (int i = 1; i <= EvaluationUtilities.endDateTesting; i++) {
            String filename = "";
            if (station.equals("S637")) {
                filename = EvaluationUtilities.getFileNameForDetectorStation("I35E", detector, i, "2013070", "201307");
            } else if (station.equals("S86")) {
                filename = EvaluationUtilities.getFileNameForDetectorStation("I94", detector, i, "2015060", "201506");
            }

            List<DetectorDataValue> updateValues = AIDTrafficDataReader.readDetectorDataFromFile(filename, timestepStart);
            allValues.addAll(updateValues.stream().collect(Collectors.toList()));

            timestepStart = updateValues.get(updateValues.size() - 1).getTime() + timeStepSize;
        }

        String incidentFiles = evaluation_base + "ForecastModule\\incidentFiles\\incidentArchiv" + station + "D" + detector + ".csv";
        IncidentStorage incidentStorage = new IncidentStorage(incidentFiles);
        for (DetectorDataValue value : allValues) {
            OTCManager.getInstance().setTimeForTests(value.getTime());

            forecastModule.addValueForForecast(value.getTime(), value.getValues()[DetectorCapabilities.SPEED]);
            float forecast = forecastModule.getForecast(1);

            // congestion line: 45 mph (* 1,609 for km/h)
            if (forecast <= 45.0 * 1.609) {
                Incident incident = new Incident();
                incident.setStartTime(value.getTime() + timeStepsForward * timeStepSize);
                incidentStorage.storeIncident(incident);
            }
            incidentStorage.classifyIncident(value.getTime(), value.isCongested());
        }

        EvaluationUtilities.evaluateTestResultPerDay(incidentStorage, 0, evaluation_base + "ForecastModule\\S637ResultsForecastModule.csv");
        EvaluationUtilities.evaluateTestResultPerDay(incidentStorage, 0, evaluation_base + "ForecastModule\\S86ResultsForecastModule.csv");
    }
}


