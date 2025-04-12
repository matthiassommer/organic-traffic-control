package tests.evaluation.aid.laSVM;

import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.aid.algorithms.svm.CongestionClassificator;
import de.dfg.oc.otc.aid.algorithms.svm.IncidentStorage;
import de.dfg.oc.otc.aid.algorithms.svm.SVM;
import de.dfg.oc.otc.aid.evaluation.CongestionMetrics;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.tools.FileUtilities;
import tests.evaluation.aid.AIDTrafficDataReader;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Matthias Sommer.
 */
public abstract class EvaluationUtilities {
    public static final String EVALUATION_FOLDER = "..\\Auswertung\\AID\\LASVM\\";
    public static final int numberSamplesPerDay = 288;
    /**
     * Date in the first Month, where the learning starts
     */
    private static final int startDateLearning = 17;
    /**
     * Date in the first Month, where the learning ends
     */
    private static final int endDateLearning = 30;
    /**
     * Date in the second Month, where the testing starts
     */
    private static final int startDateTesting = 1;
    /**
     * Date in the second Month, where the testing ends
     */
    public static int endDateTesting = 14;

    public static Map<String, Object> getParameter() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("C", 10.0);
        parameters.put("gamma", 2.0);
        parameters.put("timeStepSize", (float) 300);
        parameters.put("timeStepsBackward", 5);
        parameters.put("timeStepsForward", 1);
        parameters.put("detectorCapabilities", new int[]{DetectorCapabilities.DENSITY, DetectorCapabilities.OCCUPANCY});
        parameters.put("forecastMethods", new ArrayList<>());
        parameters.put("forecastCapabilities", new int[]{});
        parameters.put("onlineLearning", false);
        parameters.put("reportCongestion", true);
        parameters.put("reportCongestionFile", null);
        parameters.put("congestionDefinition", CongestionClassificator.Definition.ANNOTATED_DATA);
        return parameters;
    }

    static void setEndDateTesting(int endDateTesting) {
        EvaluationUtilities.endDateTesting = endDateTesting;
    }

    /**
     * This is the main routing for the evaluation.
     *
     * @param station
     * @param detector  Detector where LASVM is executed
     * @param parameter parameter, which used for initializing
     * @return
     */
    static IncidentStorage runEvaluation(@NotNull String station, @NotNull String detector, @NotNull Map<String, Object> parameter) {
        SVM svm = new SVM(parameter);

        float timeStepSize = (float) parameter.get("timeStepSize");
        testSVM(svm, station, detector, timeStepSize);

        return svm.getIncidentStorage();
    }

    private static void testSVM(SVM svm, String station, String detector, float timeStepSize) {
        System.out.print(" Testing (");
        float startTime = System.currentTimeMillis();
        float timestepStart = 0;

        svm.setReportCongestion(true);

        List<DetectorDataValue> allDetectorValues = new ArrayList<>();

        for (int i = startDateTesting; i <= endDateTesting; i++) {
            String filename = "";
            if (station.equals("S637")) {
                filename = getFileNameForDetectorStation("I35E", detector, i, "2013070", "201307");
            } else if (station.equals("S86")) {
                filename = getFileNameForDetectorStation("I94", detector, i, "2015060", "201506");
            }

            List<DetectorDataValue> updateValues = AIDTrafficDataReader.readDetectorDataFromFile(filename, timestepStart);
            allDetectorValues.addAll(updateValues.stream().collect(Collectors.toList()));

            timestepStart = updateValues.get(updateValues.size() - 1).getTime() + timeStepSize;
        }

        for (DetectorDataValue value : allDetectorValues) {
            OTCManager.getInstance().setTimeForTests(value.getTime());
            svm.update(null, value);
        }
        System.out.print((System.currentTimeMillis() - startTime) / 1000f + " sec.)");
    }

    private static float trainSVM(SVM svm, String station, String detector, float timeStepSize) {
        long startTime = System.currentTimeMillis();
        System.out.print("\nSTART: Training (");

        List<DetectorDataValue> allDetectorValues = new ArrayList<>();
        float timestepStart = 0;

        svm.setReportCongestion(false);

        for (int i = startDateLearning; i <= endDateLearning; i++) {
            String filename = "";
            if (station.equals("S637")) {
                filename = getFileNameForDetectorStation("I35E", detector, i, "2013060", "201306");
            } else if (station.equals("S86")) {
                filename = getFileNameForDetectorStation("I94", detector, i, "2015050", "201505");
            }

            List<DetectorDataValue> detectorValues = AIDTrafficDataReader.readDetectorDataFromFile(filename, timestepStart);
            allDetectorValues.addAll(detectorValues.stream().collect(Collectors.toList()));

            timestepStart = detectorValues.get(detectorValues.size() - 1).getTime() + timeStepSize;
        }

        svm.offlineLearning(allDetectorValues);

        System.out.print((System.currentTimeMillis() - startTime) / 1000f + " sec.)");

        return timestepStart;
    }

    static void appendLineToFile(String line, @NotNull String filename) {
        try {
            File file = new File(filename);
            BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile(), true));
            bw.append(line).append("\n");
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void appendLineToFile(@NotNull float[] array, @NotNull String filename) {
        StringBuilder lineBuffer = new StringBuilder();
        for (float value : array) {
            if (Float.isNaN(value)) {
                lineBuffer.append(0 + " ");
            } else {
                lineBuffer.append(value).append(" ");
            }
        }

        appendLineToFile(lineBuffer.toString(), filename);
    }

    /**
     * @param incidentStorage
     * @return Metrics {TP, TN, FP, FN, precision, recall, accuracy, fmeasure, specificity, sensitivity}
     */
    static float[] getMetrics(@NotNull IncidentStorage incidentStorage) {
        List<Incident> incidents = new ArrayList<>();
        incidents.addAll(incidentStorage.getNotDetectedIncidents());
        incidents.addAll(incidentStorage.getRaisedIncidents());
        return getMetrics(incidents);
    }

    /**
     * @param incidentList
     * @return Metrics {TP, TN, FP, FN, precision, recall, accuracy, fmeasure, specificity, sensitivity}
     */
    @NotNull
    private static float[] getMetrics(@NotNull List<Incident> incidentList) {
        float FN = 0;
        float FP = 0;
        float TP = 0;
        float TN = 0;

        for (Incident incident : incidentList) {
            switch (incident.getEvaluationStatus()) {
                case TRUE_POSITIVE:
                    TP++;
                    break;
                case FALSE_NEGATIVE:
                    FN++;
                    break;
                case FALSE_POSITIVE:
                    FP++;
                    break;
                default:
                    TN++;
            }
        }

        return new float[]{TP, TN, FP, FN,
                CongestionMetrics.precision(TP, FP),
                CongestionMetrics.recall(TP, FN),
                CongestionMetrics.accuracy(TP, TN, FP, FN),
                CongestionMetrics.fmeasure(TP, FP, FN),
                CongestionMetrics.specificity(TN, FP),
                CongestionMetrics.sensitivity(TP, FN)};
    }

    @NotNull
    private static List<Incident> collectIncidentsOfOneDay(@NotNull IncidentStorage incidentStorage, float sampleStartTime, int day) {
        List<Incident> incidentsOfOneDay = new ArrayList<>();

        for (Incident incident : incidentStorage.getRaisedIncidents()) {
            float timeInSeconds = incident.getStartTime() * 60;
            int incidentDay = (int) ((timeInSeconds - sampleStartTime) / (60 * 60 * 24));
            if (incidentDay == day) {
                incidentsOfOneDay.add(incident);
            }
        }

        for (Incident incident : incidentStorage.getNotDetectedIncidents()) {
            float timeinSeconds = incident.getStartTime() * 60;
            int incidentDay = (int) ((timeinSeconds - sampleStartTime) / (60 * 60 * 24));
            if (incidentDay == day) {
                incidentsOfOneDay.add(incident);
            }
        }
        return incidentsOfOneDay;
    }

    /**
     * Combines results of one or more incidentstorages. Normally each Detector creates one Incidentstorage
     *
     * @param incidentstorage The IncidentStorage of one Test. Normally 3 Detector are combined
     * @param sampleStartTime
     * @param filename
     * @return array in form of [day][metric]
     */
    @NotNull
    public static float[][] evaluateTestResultPerDay(@NotNull IncidentStorage incidentstorage, float sampleStartTime, @NotNull String filename) {
        FileUtilities.createNewFile(filename);

        // Für jeden Tag pro Detektor eine eigene Liste
        // Incident [Day][Detector][Incidents]
        List<List<Incident>> incidentsPerDayPerDetector = new ArrayList<>();

        // Collect for each day all incidents of one detector in a list.
        for (int i = 0; i < endDateTesting; i++) {
            incidentsPerDayPerDetector.add(collectIncidentsOfOneDay(incidentstorage, sampleStartTime, i));
        }

        float[][] combinedResults = new float[endDateTesting][10];

        for (int i = 0; i < endDateTesting; i++) {
            float[] temp = getMetrics(incidentsPerDayPerDetector.get(i));
            combinedResults[i][0] += temp[0]; // TP
            combinedResults[i][1] += temp[1]; // TN
            combinedResults[i][2] += temp[2]; // FP
            combinedResults[i][3] += temp[3]; // FN
            combinedResults[i][4] += temp[4]; // precision
            combinedResults[i][5] += temp[5]; // recall
            combinedResults[i][6] += temp[6]; // accuracy
            combinedResults[i][7] += temp[7]; // fmeasure
            combinedResults[i][8] += temp[8]; // specificity
            combinedResults[i][9] += temp[9]; // sensitivity

            EvaluationUtilities.appendLineToFile(combinedResults[i], filename);
        }

        return combinedResults;
    }

    @NotNull
    public static String getFileNameForDetectorStation(String roadName, String detector, int i, String id1, String id2) {
        return AIDTrafficDataReader.AID_DATA_FOLDER + "\\" + roadName + " - " + detector + (i < 10 ? "\\" + id1 + i : "\\" + id2 + i) + ".txt";
    }
}
