package tests.evaluation.aid;

import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.aid.algorithms.svm.IncidentStorage;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.tools.FileUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See folder TrafficData/AIDTrafficData
 * Each file has 288 situation entries.
 * 3 Folders *(30+31)+3 Folders *(31+30) = 366 files are available
 * 366*288=105408 entries available
 * <p>
 * Created by oc6admin on 07.01.2016.
 */
public abstract class AIDTrafficDataReader {
    public static final int NUMBER_TRAINING_DAYS = 21;
    public static final int NUMBER_TEST_DAYS = 7;
    public static final String[] FOLDER_NAMES = {
            "I35E - 2442\\201306", "I35E - 2442\\201307",
            "I35E - 2443\\201306", "I35E - 2443\\201307",
            "I35E - 2444\\201306", "I35E - 2444\\201307",
            "I35E - 2447\\201306", "I35E - 2447\\201307",
            "I35E - 2448\\201306", "I35E - 2448\\201307",
            "I94 - 569\\201505", "I94 - 569\\201506",
            "I94 - 365\\201505", "I94 - 365\\201506",
            "I94 - 366\\201505", "I94 - 366\\201506",
            "I94 - 367\\201505", "I94 - 367\\201506",
            "TH5 - 1577\\201512", "TH5 - 1577\\201601"
    };
    /**
     * Length of the input vector from the files. Last entry is the class label.
     */
    public static final int SITUATION_LENGTH = 7;
    /**
     * Positions of the sensor values in the input vector from the data files.
     */
    public static final int DENSITY = 0;
    public static final int FLOW = 1;
    public static final int HEADWAY = 2;
    public static final int OCCUPANCY = 3;
    public static final int SPEED = 4;
    public static final int VOLUME = 5;
    /**
     * Folder where the evaluation data sets are stored.
     */
    public static final String AID_DATA_FOLDER = "..\\TrafficData\\AID Traffic Data\\";
    public static final int NUMBER_DATA_PER_DAY = 288;
    public static final int NUMBER_OF_EVALUATION_RUNS = 10;

    /**
     * Sensor values are average over five minutes intervals.
     * 2304 entries per file
     * situation[] ={TIME, VOLUME, PRESENCE, SPEED, OCCUPANCY, HEADWAY, DENSITY, EQUIPPED VEHICLE, actualCongestionState}
     * Adjusts the class labels: -1 is converted to 0 for XCSR to work properly!
     *
     * @param filename
     * @return list of traffic sensor feature vectors
     */
    public static List<Float> readDataFromFile(@NotNull String filename) {
        List<Float> data = new ArrayList<>();
        try {
            String line;

            BufferedReader br = new BufferedReader(new FileReader(filename));

            while ((line = br.readLine()) != null) {
                String[] arguments = line.split(",");

                for (int i = 2; i < arguments.length - 1; i++) {
                    data.add(Float.parseFloat(arguments[i]));
                }
                try {
                    float label = Float.parseFloat(arguments[arguments.length - 1]);
                    data.add(label < 0 ? 0f : 1f);
                } catch (NumberFormatException e) {
                    e.getMessage();
                }
            }
            br.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return data;
    }

    public static void addLabelsToCSVFile() {
        for (int i = 10; i <= 30; i++) {
            String filename = "..//TrafficData//Minneapolis Freeway Traffic Data//I94 - 569//201505" + i + ".csv";
            try {
                String line;

                BufferedReader br = new BufferedReader(new FileReader(filename));

                //remove headers
                br.readLine();
                br.readLine();
                br.readLine();

                FileUtilities.createNewFile("..//TrafficData//AID Traffic Data//I94 - 569//201505" + i + ".csv");
                PrintStream writer = new PrintStream(new FileOutputStream("..//TrafficData//AID Traffic Data//I94 - 569//201505" + i + ".csv"), true);
                while ((line = br.readLine()) != null) {
                    String[] arguments = line.split(",");
                    float speed = Float.parseFloat(arguments[6]);
                    if (speed > 0 && speed < 20) {
                        writer.println(line + "-1");
                    } else {
                        writer.println(line + "1");
                    }

                }
                br.close();
                writer.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * Reads n days of files from the AID data folders, e.g. 288*8*30days=69120 entries
     *
     * @return list of float data
     */
    public static List<Float> readFilesFromFolders(String folderName) {
        List<Float> evaluationData = new ArrayList<>();
        for (int j = 1; j <= NUMBER_TRAINING_DAYS + NUMBER_TEST_DAYS; j++) {
            String fileName = j + ".txt";
            if (j < 10) {
                fileName = "0" + j + ".txt";
            }
            evaluationData.addAll(readDataFromFile(AID_DATA_FOLDER + folderName + fileName));
        }
        return evaluationData;
    }

    /**
     * Read n days of evaluation data from the specified input folder.
     *
     * @param inputFolder parh to the data sets
     * @return list of DetectorDataValue objects
     */
    public static List<DetectorDataValue> getEvaluationData(String inputFolder, int lastDay) {
        List<DetectorDataValue> evaluationData = new ArrayList<>();
        for (int j = 1; j <= lastDay; j++) {
            String fileName = j + ".txt";
            if (j < 10) {
                fileName = "0" + j + ".txt";
            }
            List<DetectorDataValue> values = AIDTrafficDataReader.readDetectorDataFromFile(inputFolder + fileName, 0);
            evaluationData.addAll(values.stream().collect(Collectors.toList()));
        }
        return evaluationData;
    }

    /**
     * Read annotated detector data from a CSV file, starting a a defined timestep.
     * 0 for all data in the file.
     * The 8. entry of the file has to be 1 or 0, defining if there is a congestion or none.
     * Used for the evaluation of the SVM approach.
     *
     * @param filename  complete path to the txt file
     * @param timeShift 0 for whole file
     * @return list of object of type DetectorDataValue
     */
    @NotNull
    public static List<DetectorDataValue> readDetectorDataFromFile(@NotNull String filename, float timeShift) {
        List<DetectorDataValue> detectorDataValues = new ArrayList<>();

        try {
            String line;
            BufferedReader br = new BufferedReader(new FileReader(filename));

            while ((line = br.readLine()) != null) {
                String[] arguments = line.split(" ");

                float[] data = new float[arguments.length];
                for (int i = 0; i < arguments.length - 1; i++) {
                    data[i] = Float.parseFloat(arguments[i + 1]);
                }

                DetectorDataValue dataValue;
                if (data.length <= 7) {
                    dataValue = new DetectorDataValue(Float.parseFloat(arguments[0]) + timeShift, data);
                } else {
                    boolean isCongested = data[7] == ClassLabels.CONGESTION.getLabel();
                    dataValue = new DetectorDataValue(Float.parseFloat(arguments[0]) + timeShift, data, isCongested);
                    dataValue.setAsAnnotated();
                }
                detectorDataValues.add(dataValue);
            }
            br.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return detectorDataValues;
    }

    /**
     * Read a CSV file with incidents for the evaluation of the Fuzzy KNN.
     *
     * @param filename complete path to file
     * @return Incident storage with loaded incidents from the file
     */
    @Nullable
    public static IncidentStorage readIncidentsFromFile(@NotNull String filename) {
        IncidentStorage archive = new IncidentStorage("");

        try {
            String line;
            BufferedReader br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null) {
                String[] arguments = line.split(" ");

                boolean congestion = arguments[2].equalsIgnoreCase("TRUE_POSITIVE");
                float timestep = Float.parseFloat(arguments[0]);

                Incident incident = new Incident();
                incident.setStartTime(timestep);
                incident.setEvaluationStatus(congestion ? Incident.EvaluationStatus.TRUE_POSITIVE : Incident.EvaluationStatus.FALSE_POSITIVE);

                archive.storeIncident(incident);
            }
            br.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return archive;
    }

    public enum ClassLabels {
        CONGESTION(-1), NO_CONGESTION(1), FALSE_DETECTOR_DATA(-100);

        private int label;

        ClassLabels(int label) {
            this.label = label;
        }

        public int getLabel() {
            return label;
        }
    }
}
