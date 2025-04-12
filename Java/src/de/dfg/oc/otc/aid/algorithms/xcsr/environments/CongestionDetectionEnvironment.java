package de.dfg.oc.otc.aid.algorithms.xcsr.environments;

import de.dfg.oc.otc.aid.algorithms.xcsr.XCSR;
import de.dfg.oc.otc.aid.evaluation.CongestionMetrics;
import de.dfg.oc.otc.tools.FileUtilities;
import de.dfg.oc.otc.tools.HelperFunctions;
import org.apache.commons.lang3.ArrayUtils;
import tests.evaluation.aid.AIDTrafficDataReader;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by oc6admin on 03.02.2016.
 */
public class CongestionDetectionEnvironment implements ContinuousEnvironment {
    /**
     * Folder where the evaluation results are stored.
     */
    private final String EVALUATION_FOLDER = "..\\Auswertung\\AID\\XCSR\\";
    /**
     * Flags if the specified sensor values should be included in the feature vector.
     */
    private boolean VOLUME;
    private boolean SPEED;
    private boolean OCCUPANCY;
    private boolean DENSITY;
    /**
     * Exports the complex congestion/classification error metrics.
     */
    private PrintStream psAllMetrics = null;
    /**
     * Exports values for the confusions matrix (TP,FP,FN,TN).
     */
    private PrintStream psConfusionMatrix = null;
    /**
     * Exports the classification and the actual label for plotting the ROC curve.
     */
    private PrintStream psPredictionActualLabel = null;
    /**
     * Minimum and maximum values for the normalisation to [0;1].
     */
    private double MAXCOUNT;
    private double MAXSPEED;
    private double MAXOCCUPANCY;
    private double MAXDENSITY;
    /**
     * Last feature vector that was passed to the XCSR.
     */
    private double[] lastFeatureVector;
    /**
     * Flag if the last prediction of the XCSR was correct.
     */
    private boolean lastPredictionWasCorrect = false;
    /**
     * Last full situation vector that was taken from the data set.
     */
    private double[] lastSituation;
    /**
     * Data set for evaluation.
     */
    private double[] data;
    private XCSR xcsr;
    private int TP = 0;
    private int FP = 0;
    private int TN = 0;
    private int FN = 0;
    /**
     * After training, the metrics are evaluated.
     */
    private boolean trainingPhaseFinished = false;
    /**
     * Use the last classification of the XCSR as an additional feature.
     */
    private boolean useLastPrediction = false;
    /**
     * offset == 0 => congestion detection, offset > 0 => congestion forecast.
     * Increase by 1 leads to increasing the forecast horizon by 5 minutes.
     */
    private int timeStepOffset = 6;

    public void setUseLastPrediction(boolean useLastPrediction) {
        this.useLastPrediction = useLastPrediction;
    }

    public void run(boolean count, boolean speed, boolean occupancy, boolean density, String filename) {
        setConfig(count, speed, occupancy, density);

        String folderSuffix = "";
        if (useLastPrediction) {
            folderSuffix += "LastClassification\\";
        } else if (this.timeStepOffset > 0) {
            folderSuffix += "Forecast\\" + this.timeStepOffset * 5 + "Min\\";
        }

        initFileWriters(filename, folderSuffix);

        long time = System.currentTimeMillis();
        for (int i = 0; i < AIDTrafficDataReader.NUMBER_OF_EVALUATION_RUNS; i++) {
            TP = 0;
            FP = 0;
            TN = 0;
            FN = 0;

            data = loadData(i * 2);
            determineMaxValues(data);

            xcsr = new XCSR(this, EVALUATION_FOLDER + "XCSRPerformance\\" + folderSuffix + filename);
            xcsr.setSeedForExperiment(i + 1);
            xcsr.startExperiment(i + 1, (data.length - this.timeStepOffset * AIDTrafficDataReader.SITUATION_LENGTH) / AIDTrafficDataReader.SITUATION_LENGTH);

            //TODO: ausgabe einmal für testphase und einmal in andere datei für training!
            psConfusionMatrix.println(TP + "\t" + FP + "\t" + TN + "\t" + FN);
            psAllMetrics.println(CongestionMetrics.precision(TP, FP) +
                    "\t" + CongestionMetrics.accuracy(TP, TN, FP, FN) +
                    "\t" + CongestionMetrics.fmeasure(TP, FP, FN) +
                    "\t" + CongestionMetrics.specificity(TN, FP) +
                    "\t" + CongestionMetrics.sensitivity(TP, FN));
            this.trainingPhaseFinished = false;
        }
        System.out.println("TIME " + (System.currentTimeMillis() - time) / 1000.0 + " s.");

        psAllMetrics.close();
        psConfusionMatrix.close();
        psPredictionActualLabel.close();
    }

    /**
     * Bestimmt welche Sensorwerte verwendet werden.
     */
    private void setConfig(boolean count, boolean speed, boolean occupancy, boolean density) {
        VOLUME = count;
        SPEED = speed;
        OCCUPANCY = occupancy;
        DENSITY = density;
    }

    private void initFileWriters(String filename, String folderSuffix) {
        try {
            String metricFilename = EVALUATION_FOLDER + "All_Metrics\\" + folderSuffix + filename + ".txt";
            FileUtilities.createNewFile(metricFilename);
            psAllMetrics = new PrintStream(new FileOutputStream(metricFilename), true);
            psAllMetrics.println("Precision \t Accuracy \t F-measure \t Specificity \t Sensitivity");

            String confusionFilename = EVALUATION_FOLDER + "ConfusionMatrix\\" + folderSuffix + filename + ".txt";
            FileUtilities.createNewFile(confusionFilename);
            psConfusionMatrix = new PrintStream(new FileOutputStream(confusionFilename), true);
            psConfusionMatrix.println("TP \t FP \t TN \t FN");

            String rocFilename = EVALUATION_FOLDER + "ROCData\\" + folderSuffix + filename + ".txt";
            FileUtilities.createNewFile(rocFilename);
            psPredictionActualLabel = new PrintStream(new FileOutputStream(rocFilename), true);
            psPredictionActualLabel.println("Step \t Prediction \t Label");
        } catch (FileNotFoundException e) {
            System.out.print(e.getMessage());
        }
    }

    @Override
    public double[] resetState() {
        if (xcsr.getProblemCounter() > AIDTrafficDataReader.NUMBER_TRAINING_DAYS * AIDTrafficDataReader.NUMBER_DATA_PER_DAY) {
            this.trainingPhaseFinished = true;
        }

        return getFeaturevector();
    }

    /**
     * Read and create a feature vector for a given time step.
     * <p>
     * Increasing xcsr.getProblemCounter() by 1 is similar to increasing the forecast by 5 minutes.
     *
     * @return feature vector
     */
    private double[] getFeaturevector() {
        int i = (xcsr.getProblemCounter() + this.timeStepOffset) * AIDTrafficDataReader.SITUATION_LENGTH;
        double situation[] = Arrays.copyOfRange(data, i, i + AIDTrafficDataReader.SITUATION_LENGTH);
        this.lastSituation = situation;
        this.lastFeatureVector = generateSituationArray(situation);
        return this.lastFeatureVector;
    }

    /**
     * Load data for training and testing into a double array.
     *
     * @param run the current run determines the folder where the data is taken from
     * @return double array with data
     */
    private double[] loadData(int run) {
        String folder = AIDTrafficDataReader.FOLDER_NAMES[run];
        String fileType = ".csv";

        List<Float> evaluationData = new ArrayList<>();
        for (int j = 1; j <= AIDTrafficDataReader.NUMBER_TEST_DAYS + AIDTrafficDataReader.NUMBER_TRAINING_DAYS; j++) {
            String fileName = j + fileType;
            if (j < 10) {
                fileName = "0" + j + fileType;
            } else if (j > 30) {
                int day = j % 30;
                if (day < 10) {
                    if (day == 0) {
                        fileName = "30" + fileType;
                    } else {
                        fileName = "0" + String.valueOf(day) + fileType;
                    }
                } else {
                    fileName = String.valueOf(day) + fileType;
                }

                folder = AIDTrafficDataReader.FOLDER_NAMES[run + 1];
            }

            evaluationData.addAll(AIDTrafficDataReader.readDataFromFile(AIDTrafficDataReader.AID_DATA_FOLDER + folder + fileName));
        }

        double[] data = new double[evaluationData.size()];
        for (int i = 0; i < evaluationData.size(); i++) {
            data[i] = evaluationData.get(i);
        }

        return data;
    }

    /**
     * Derive the max. values of the used features.
     */
    private void determineMaxValues(double[] data) {
        if (VOLUME) {
            MAXCOUNT = HelperFunctions.getMaxValue(data, AIDTrafficDataReader.VOLUME) + 1;
        }

        if (SPEED) {
            MAXSPEED = HelperFunctions.getMaxValue(data, AIDTrafficDataReader.SPEED) + 1;
        }

        if (OCCUPANCY) {
            MAXOCCUPANCY = HelperFunctions.getMaxValue(data, AIDTrafficDataReader.OCCUPANCY) + 1;
        }

        if (DENSITY) {
            MAXDENSITY = HelperFunctions.getMaxValue(data, AIDTrafficDataReader.DENSITY) + 1;
        }
    }

    @Override
    public double[] getCurrentState() {
        return this.lastFeatureVector;
    }

    /**
     * Normiert die Verkehrsdaten auf einen Wert auf [0;1[.
     * @return normalised situation vector
     */
    private double[] generateSituationArray(double[] situation) {
        double[] adjustedSituation = new double[6];

        final int UNUSED_PARAMETER = -100;
        adjustedSituation[0] = !DENSITY ? UNUSED_PARAMETER : HelperFunctions.normalize(situation[AIDTrafficDataReader.DENSITY], 0, MAXDENSITY);
        float lastPrediction = 0.99f;
        if (this.lastSituation[AIDTrafficDataReader.SITUATION_LENGTH - 1] == 0) {
            lastPrediction = 0;
        }
        adjustedSituation[1] = !this.useLastPrediction ? UNUSED_PARAMETER : lastPrediction;
        adjustedSituation[2] = UNUSED_PARAMETER;
        adjustedSituation[3] = !OCCUPANCY ? UNUSED_PARAMETER : HelperFunctions.normalize(situation[AIDTrafficDataReader.OCCUPANCY], 0, MAXOCCUPANCY);
        adjustedSituation[4] = !SPEED ? UNUSED_PARAMETER : HelperFunctions.normalize(situation[AIDTrafficDataReader.SPEED], 0, MAXSPEED);
        adjustedSituation[5] = !VOLUME ? UNUSED_PARAMETER : HelperFunctions.normalize(situation[AIDTrafficDataReader.VOLUME], 0, MAXCOUNT);

        // remove unused parameters
        for (int j = 0; j < adjustedSituation.length; j++) {
            if (adjustedSituation[j] == UNUSED_PARAMETER) {
                adjustedSituation = ArrayUtils.remove(adjustedSituation, j);
                j--;
            } else if (adjustedSituation[j] < 0) { // sensor values are faulty
                adjustedSituation[j] = 0;
            }
        }
        return adjustedSituation;
    }

    @Override
    public double executeAction(int action) {
        double actualCongestionState = this.lastSituation[AIDTrafficDataReader.SITUATION_LENGTH - 1];
        psPredictionActualLabel.println(xcsr.getProblemCounter() + "\t" + action + "\t" + actualCongestionState);

        if (action == 0 && actualCongestionState == 0) {
            this.lastPredictionWasCorrect = true;
            if (trainingPhaseFinished) {
                TP++;
            }
            return getMaxPayoff();
        } else if (action == 0 && actualCongestionState == AIDTrafficDataReader.ClassLabels.NO_CONGESTION.getLabel()) {
            this.lastPredictionWasCorrect = false;
            if (trainingPhaseFinished) {
                FP++;
                //  System.out.println("False congested " + Arrays.toString(lastSituation));
            }
        } else if (action == AIDTrafficDataReader.ClassLabels.NO_CONGESTION.getLabel() && actualCongestionState == AIDTrafficDataReader.ClassLabels.NO_CONGESTION.getLabel()) {
            this.lastPredictionWasCorrect = true;
            if (trainingPhaseFinished) {
                TN++;
            }
            return getMaxPayoff();
        } else if (action == AIDTrafficDataReader.ClassLabels.NO_CONGESTION.getLabel() && actualCongestionState == 0) {
            this.lastPredictionWasCorrect = false;
            if (trainingPhaseFinished) {
                FN++;
                //  System.out.println("False free " + Arrays.toString(lastSituation));
            }
        }

        return 0;
    }

    @Override
    public boolean wasCorrect() {
        return this.lastPredictionWasCorrect;
    }

    @Override
    public boolean doReset() {
        return false;
    }

    @Override
    public int getConditionLength() {
        return 4;
    }

    @Override
    public int getMaxPayoff() {
        return 1000;
    }

    @Override
    public boolean isMultiStepProblem() {
        return false;
    }

    @Override
    public int getNrActions() {
        return 2;
    }

    @Override
    public double[] getProblemSpaceBounds(int dimension) {
        return new double[]{0.0, 1.0};
    }
}
