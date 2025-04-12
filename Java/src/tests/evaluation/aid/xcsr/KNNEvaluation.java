package tests.evaluation.aid.xcsr;

import de.dfg.oc.otc.aid.algorithms.knn.FuzzykNN;
import de.dfg.oc.otc.aid.algorithms.knn.KNNDetectorDataValue;
import de.dfg.oc.otc.aid.algorithms.svm.IncidentStorage;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;
import tests.evaluation.aid.AIDTrafficDataReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by oc6admin on 19.01.2016.
 */
class KNNEvaluation {
    private final String outputDataFolder = "..\\Auswertung\\AID\\FuzzyKNN\\";

    public static void main(String[] args) throws IOException {
        KNNEvaluation evaluation = new KNNEvaluation();
        evaluation.run();
    }

    private void run() {
        for (int run = 0; run < AIDTrafficDataReader.NUMBER_OF_EVALUATION_RUNS; run++) {
            runAlgorithm(AIDTrafficDataReader.AID_DATA_FOLDER + AIDTrafficDataReader.FOLDER_NAMES[run], outputDataFolder + run + ".txt");
        }
        writeMetricsToFile();
    }

    private void runAlgorithm(String inputFolder, String outputFilePath) {
        FuzzykNN fuzzyknn = new FuzzykNN();

        List<DetectorDataValue> evaluationData = AIDTrafficDataReader.getEvaluationData(inputFolder, 30);
        IncidentStorage incidentStorage = new IncidentStorage(outputDataFolder + "\\incidentArchiv" + ".csv");

        List<KNNDetectorDataValue> fuzzyDetektorDataValue = new ArrayList<>();
        for (DetectorDataValue value : evaluationData) {
            KNNDetectorDataValue knnValue = new KNNDetectorDataValue(value, value.isCongested());
            fuzzyDetektorDataValue.add(knnValue);
        }
        AbstractDetectorGroup dummy = fuzzyknn.getMonitoringZone().getMonitoredDetectorPairs().get(0);
        fuzzyknn.addTrainingSet(dummy.getId(), fuzzyDetektorDataValue);



        fuzzyknn.addObserver(incidentStorage);

        // run the algorithm
        for (DetectorDataValue value : evaluationData) {
            dummy = fuzzyknn.getMonitoringZone().getMonitoredDetectorPairs().get(0);
            fuzzyknn.update(dummy, value);
        }

        for (DetectorDataValue value : evaluationData) {
            incidentStorage.classifyIncident(value.getTime(), value.isCongested());
        }
    }

    private void writeMetricsToFile() {
    }
}
