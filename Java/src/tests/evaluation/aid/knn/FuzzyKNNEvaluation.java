package tests.evaluation.aid.knn;


import de.dfg.oc.otc.aid.algorithms.knn.FuzzykNN;
import de.dfg.oc.otc.aid.algorithms.knn.KNNDetectorDataValue;
import de.dfg.oc.otc.aid.algorithms.svm.IncidentStorage;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;
import tests.evaluation.aid.AIDTrafficDataReader;
import tests.evaluation.aid.laSVM.EvaluationUtilities;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FuzzyKNNEvaluation {
    @Test
    public void runS637() {
        run("2448", "S637");
        evaluateFuzzyKNNTest();
    }

    @Test
    public void runS86() {
        run("366", "S86");
        evaluateFuzzyKNNTest();
    }

    private void run(String detector, String station) {
        FuzzykNN fuzzyknn = new FuzzykNN();

        // train KNN
        float timeStepSize = (float) EvaluationUtilities.getParameter().get("timeStepSize");

        String evaluation_base = EvaluationUtilities.EVALUATION_FOLDER + "Andere Verfahren\\FuzzyKNN\\incidentFiles";
        IncidentStorage incidentStorage = new IncidentStorage(evaluation_base + "\\incidentArchiv" + station + "D" + detector + ".csv");

        List<DetectorDataValue> allDetectorValues = new ArrayList<>();
        float timestepStart = 0;

        for (int i = 17; i <= 30; i++) {
            String filename = "";
            if (station.equals("S637")) {
                filename = EvaluationUtilities.getFileNameForDetectorStation("I35E", detector, i, "2013070", "201307");
            } else if (station.equals("S86")) {
                filename = EvaluationUtilities.getFileNameForDetectorStation("I94", detector, i, "2015060", "201506");
            }

            List<DetectorDataValue> updateValues = AIDTrafficDataReader.readDetectorDataFromFile(filename, timestepStart);
            allDetectorValues.addAll(updateValues.stream().collect(Collectors.toList()));

            timestepStart = updateValues.get(updateValues.size() - 1).getTime() + timeStepSize;
        }

        List<KNNDetectorDataValue> fuzzyDetektorDataValue = new ArrayList<>();
        for (DetectorDataValue value : allDetectorValues) {
            KNNDetectorDataValue knnValue = new KNNDetectorDataValue(value, value.isCongested());
            fuzzyDetektorDataValue.add(knnValue);
        }
        AbstractDetectorGroup dummy = fuzzyknn.getMonitoringZone().getMonitoredDetectorPairs().get(0);
        fuzzyknn.addTrainingSet(dummy.getId(), fuzzyDetektorDataValue);

        allDetectorValues.clear();


        // test knn
        for (int i = 1; i <= EvaluationUtilities.endDateTesting; i++) {
            String filename = "";
            if (station.equals("S637")) {
                filename = EvaluationUtilities.getFileNameForDetectorStation("I35E", detector, i, "2013060", "201306");
            } else if (station.equals("S86")) {
                filename = EvaluationUtilities.getFileNameForDetectorStation("I94", detector, i, "2015050", "201505");
            }

            List<DetectorDataValue> detectorValues = AIDTrafficDataReader.readDetectorDataFromFile(filename, timestepStart);
            allDetectorValues.addAll(detectorValues.stream().collect(Collectors.toList()));

            timestepStart = detectorValues.get(detectorValues.size() - 1).getTime() + timeStepSize;
        }

        // register as Observer
        fuzzyknn.addObserver(incidentStorage);

        // run the algorithm
        for (DetectorDataValue value : allDetectorValues) {
            dummy = fuzzyknn.getMonitoringZone().getMonitoredDetectorPairs().get(0);
            fuzzyknn.update(dummy, value);
        }

        for (DetectorDataValue value : allDetectorValues) {
            incidentStorage.classifyIncident(value.getTime(), value.isCongested());
        }
    }

    public void evaluateFuzzyKNNTest() {
        String evaluation_base = EvaluationUtilities.EVALUATION_FOLDER + "Andere Verfahren\\FuzzyKNN";

        // 1. read incidents
        IncidentStorage incidentStorageS637 = AIDTrafficDataReader.readIncidentsFromFile(evaluation_base + "\\incidentFiles\\incidentArchivS637D2448.csv");
        IncidentStorage incidentStorageS86 = AIDTrafficDataReader.readIncidentsFromFile(evaluation_base + "\\incidentFiles\\incidentArchivS86D366.csv");

        // 2. compute results
        EvaluationUtilities.evaluateTestResultPerDay(incidentStorageS637, 60 * 60 * 24 * 14, evaluation_base + "\\S637ResultsFuzzyKNN.csv");
        EvaluationUtilities.evaluateTestResultPerDay(incidentStorageS86, 60 * 60 * 24 * 14, evaluation_base + "\\S86ResultsFuzzyKNN.csv");
    }
}
