package tests.evaluation.aid.xcsrUrban;

import de.dfg.oc.otc.aid.algorithms.xcsrUrban.XCSRUrbanParameters;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dietmar on 17.04.2017.
 */
public class LoadDataXCSRU
{
    /**
     * Positions of the sensor values in the input vector from the data files.
     */
    public static final int TIME = 0;
    public static final int DENSITY1 = 1;
    public static final int DENSITY2 = 2;
    public static final int HEADWAY1 = 3;
    public static final int HEADWAY2 = 4;
    public static final int OCCUPANCY1 = 5;
    public static final int OCCUPANCY2 = 6;
    public static final int SPEED1 = 7;
    public static final int SPEED2 = 8;
    public static final int VOLUME1 = 9;
    public static final int VOLUME2 = 10;


    public static int NUMBER_TRAINING_DAYS = 9;
    public static int NUMBER_TEST_DAYS = 1;
    public static final int NUMBER_DATA_PER_DAY = 40723;

    /**
     * Length of the input vector from the files. Last entry is the class label.
     */
    public static final int SITUATION_LENGTH = 12;

    static String pathTrafficData = "..\\Studentische Arbeiten\\PM Sach\\Evaluation\\1 Aimsun Traffic Data\\";
    static String pathResults = "..\\Studentische Arbeiten\\PM Sach\\Evaluation\\2 Parameterstudie\\AID\\";

    /**
     * Folder where the evaluation data sets are stored.
     * Syntax is detectorPairID\\replicationID(first 6 digits)
     */
    public static final String[] FOLDER_NAMES = {
            "977978979980\\100006"
    };

    /**
     * makes "10000661.csv" the first data file
     */
    private static int firstReplicationSuffix = 61;
    public static final int NUMBER_OF_EVALUATION_FOLDERS = FOLDER_NAMES.length;
    static String delimiter = ";";


    /**
     * Load data for training and testing into a double array.
     * Loads each day for the specified detector and policy
     * @param evalFolderIdx the evalFolderIdx determines the folder where the data is taken from
     * @return 1D array with sequential detector data from all day files
     */
    public static double[] loadSequentialData(int evalFolderIdx) {
        String folder = FOLDER_NAMES[evalFolderIdx];
        String fileType = ".csv";

        // evalFolderIdx = detector/policy, j = day
        double[] evaluationData = new double[(NUMBER_TEST_DAYS + NUMBER_TRAINING_DAYS) * NUMBER_DATA_PER_DAY * SITUATION_LENGTH];
        int i = 0;
        for (int j = firstReplicationSuffix; j < firstReplicationSuffix + NUMBER_TEST_DAYS + NUMBER_TRAINING_DAYS; j++) {
            String fileName = j + fileType;
            double[] data = readDataFromFile(pathTrafficData + folder + fileName);
            for (int k = 0; k < data.length; k++)
            {
                evaluationData[i++] = data[k];
            }
        }
        return evaluationData;
    }

    /**
     * Formats the data for 10-fold Crossvalidation
     * Moves the test day data to the end of the data array as the last day is taken for testing
     * @param testDataDay day index 0 to 9 for 10 days in total and NUMBER_TRAINING_DAYS=1
     * @return repartitioned data array
     */
    public static double[] partitionDataForCrossvalidation(double[] data, int testDataDay)
    {
        double[] trainingData = new double[(NUMBER_TRAINING_DAYS) * NUMBER_DATA_PER_DAY * SITUATION_LENGTH];
        double[] testData = new double[(NUMBER_TEST_DAYS) * NUMBER_DATA_PER_DAY * SITUATION_LENGTH];

        int testDataStart = testDataDay * NUMBER_DATA_PER_DAY * SITUATION_LENGTH;
        int testDataEnd = (testDataDay + 1) * NUMBER_DATA_PER_DAY * SITUATION_LENGTH;

        int j = 0;
        int k = 0;

        for (int i = 0; i < data.length; i++)
        {
            if (testDataStart <= i && i < testDataEnd)
            {
                testData[j++] = data[i];
            }
            else
            {
                trainingData[k++] = data[i];
            }
        }

        return ArrayUtils.addAll(trainingData, testData);
    }

    /**
     * @param filename
     * @return list of traffic sensor feature vectors
     */
    public static double[] readDataFromFile(@NotNull String filename) {
        List<Float> data = new ArrayList<>(NUMBER_DATA_PER_DAY * SITUATION_LENGTH);
        try {
            String line;

            BufferedReader br = new BufferedReader(new FileReader(filename));

            while ((line = br.readLine()) != null) {
                String[] arguments = line.split(delimiter);

                for (int i = 0; i < arguments.length; i++)
                {
                    data.add(Float.parseFloat(arguments[i]));
                }
            }
            br.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        double[] d = new double[data.size()];
        for (int i = 0; i < data.size(); i++)
            d[i] = data.get(i);
        return d;
    }
}
