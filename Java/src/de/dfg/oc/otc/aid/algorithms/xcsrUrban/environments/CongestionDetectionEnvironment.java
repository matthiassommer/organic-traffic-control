package de.dfg.oc.otc.aid.algorithms.xcsrUrban.environments;

import de.dfg.oc.otc.aid.AimsunPolicyStatus;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.OperationMode;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.XCSRUrban;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.XCSRUrbanParameters;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;
import de.dfg.oc.otc.tools.FileUtilities;
import de.dfg.oc.otc.tools.HelperFunctions;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import tests.evaluation.aid.AIDTrafficDataReader;
import tests.evaluation.aid.xcsrUrban.LoadDataXCSRU;

import java.io.*;
import java.util.Arrays;

/**
 * Created by oc6admin on 03.02.2016.
 * Edited by Dietmar on 24.04.2017
 */
public class CongestionDetectionEnvironment implements ContinuousEnvironment, Runnable {

    public String evalFolder = XCSRUrbanParameters.EVALUATION_FOLDER;
    /**
     * Flags if the specified sensor values should be included in the feature vector.
     */
    private boolean VOLUME;
    private boolean SPEED;
    private boolean OCCUPANCY;
    private boolean DENSITY;
    /**
     *
     */
    /**
     * Exports the classification and the actual label for plotting the ROC curve.
     */
    private PrintStream psPredictionActualLabel = null;

    public PrintStream psParameters = null;
//    private PrintStream psRanking = null;

    /**
     * Minimum and maximum values for the normalisation to [0;1].
     */
    private double MAXCOUNT1;
    private double MAXCOUNT2;
    private double MAXSPEED1;
    private double MAXSPEED2;
    private double MAXOCCUPANCY1;
    private double MAXOCCUPANCY2;
    private double MAXDENSITY1;
    private double MAXDENSITY2;
    /**
     * Last feature vector that was passed to the XCSRUrban.
     */
    private double[] lastFeatureVector;
    /**
     * Flag if the last prediction of the XCSRUrban was correct.
     */
    private boolean lastPredictionWasCorrect = false;
    /**
     * Last full situation vector that was taken from the data set.
     */
    private double[] lastSituation;
    /**
     * After training, the metrics are evaluated.
     */
    private boolean trainingPhaseFinished = false;
    /**
     * Use the last classification of the XCSRUrban as an additional feature.
     */
    private boolean useLastPrediction = false;
    /**
     * offset == 0 => congestion detection, offset > 0 => congestion forecast.
     * Increase by 1 leads to increasing the forecast horizon by 5 minutes.
     */
    private int timeStepOffset = 6;

    /**
     * Data set for evaluation.
     */
    public double[][] dataSet;
    private double[] currentData;

    /**
     * Aimsun live data
     */
    public double[] currentSituationAimsun;

    private XCSRUrban xcsru;
    private long time;
    private int seed;
    public OperationMode operationMode;


    public XCSRUrbanParameters parameters = new XCSRUrbanParameters();

    public void setUseLastPrediction(boolean useLastPrediction) {
        this.useLastPrediction = useLastPrediction;
    }

    //ParameterEvaluation
    public void init(boolean count, boolean speed, boolean occupancy, boolean density, double[][] data, int seed, String evalFolder, OperationMode operationMode, boolean classificationOnly)
    {
        // crossValidationIndex is used in the paramter study only
        this.evalFolder = evalFolder + "\\";
        this.seed = seed;
        this.dataSet = data;
        setConfig(count, speed, occupancy, density);
        time = System.currentTimeMillis();
        this.operationMode = operationMode;
        this.trainingPhaseFinished = classificationOnly;
        this.xcsru = new XCSRUrban(this, this.evalFolder, parameters);
        this.setDataSet(data);
    }
    public void setDataSet(double[][] data)
    {
        this.dataSet = data.clone();
    }

    // APIDEvaluation, Aimsun Mode
    public void init(XCSRUrban xcsru, boolean count, boolean speed, boolean occupancy, boolean density, double[][] data, boolean classificationOnly, String evalFolder)
    {
        this.evalFolder = evalFolder;
        this.seed = 0;
        this.dataSet = data;
        setConfig(count, speed, occupancy, density);
        time = System.currentTimeMillis();
        this.trainingPhaseFinished = classificationOnly;
        operationMode = OperationMode.AIMSUN_CLASSIFICATION;

        if (xcsru != null)
        {
            this.xcsru = xcsru;
        }
        else
        {
            this.xcsru = new XCSRUrban(this, this.evalFolder, parameters);
        }

    }

    public void run()
    {
        try {
            for (int i = 0; i < LoadDataXCSRU.NUMBER_OF_EVALUATION_FOLDERS; i++) {
                System.out.println("Starting evaluation " + evalFolder);

                this.evalFolder += i + "_";         // a prefix for each evaluation data folder
                this.initFileWriters();
                this.currentData = this.dataSet[i];

                // Use this if you only train from CSV files and if you don't re-use the population
                //            this.determineMaxValues(this.currentData);

                // When you need to use the trained data and save the population for use in live systems, where higher values
                // than measured in training could occur, set higher values for safety:
                // in previously exported csv traffic data no higher values than 65 for speed and 101 for occupancy were recorded. Based on this experience, arbitrary values are selected here
                setMaxValues(0.0, 0.0, 100, 100, 200, 200, 0.0, 0.0);


                xcsru.setSeedForExperiment(i + 1 + seed);
                xcsru.startExperiment(i, (this.currentData.length - this.timeStepOffset * LoadDataXCSRU.SITUATION_LENGTH) / LoadDataXCSRU.SITUATION_LENGTH);   //trialsPerExperiment = Number of feature fectors per run

                // print the metrics and which xcs parameter seed was used
                //            psRanking.println(String.format("%06d", xcsParamSeed) + ";" + tpfp);

                // print the parameters used
                psParameters.println(this.parameters.printParameters());

                xcsru.serializePopulation(this.evalFolder);

                this.trainingPhaseFinished = false;
            }

            System.out.println("TIME " + (System.currentTimeMillis() - time) / 1000.0 + " s.");

            psPredictionActualLabel.close();
            psParameters.close();
            //        psRanking.close();

            System.out.println("Finished parameter configuration " + evalFolder);
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
            System.out.println("Exception in " + this.evalFolder);
        }
    }


    public void setParameters(int maxPopSize, double beta, double theta_GA, double pX, double pM, double predictionErrorReduction)
    {
        parameters.setParameters(maxPopSize, beta, theta_GA, pX, pM, predictionErrorReduction);
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

    public void initFileWriters() {
        try {
            String rocFilename = this.evalFolder + "ROCData.csv";
            FileUtilities.createNewFile(rocFilename);
            psPredictionActualLabel = new PrintStream(new FileOutputStream(rocFilename), true);
            psPredictionActualLabel.println("Step;Time;CongestionDetected;IsCongested");

            String parFilename = this.evalFolder + "XCS_Parameters.txt";
            FileUtilities.createNewFile(parFilename);
            psParameters = new PrintStream(new FileOutputStream(parFilename), true);

//            String rankFilename = evalFolder + "XCS_ParamsRanking.txt";
//            if (!Files.exists(Paths.get(rankFilename)))
//                FileUtilities.createNewFile(rankFilename);
//            psRanking = new PrintStream(new FileOutputStream(rankFilename, true), true);

        } catch (FileNotFoundException e) {
            System.out.print(e.getMessage());
        }
    }

    @Override
    public double[] resetState() {
        if (xcsru.getProblemCounter() > LoadDataXCSRU.NUMBER_TRAINING_DAYS * LoadDataXCSRU.NUMBER_DATA_PER_DAY) {
//            if (!trainingPhaseFinished)
//                System.out.println("finished training phase at " + xcsru.getProblemCounter());
            this.trainingPhaseFinished = true;
        }

        return getFeaturevector();
    }

    /**
     * Read and create a feature vector for a given time step.
     * <p>
     * Increasing xcsru.getProblemCounter() by 1 is similar to increasing the forecast by 5 minutes.
     *
     * @return feature vector
     */
    private double[] getFeaturevector()
    {
        if (operationMode == OperationMode.OFFLINE_EVALUATION || operationMode == OperationMode.OFFLINE_TRAINING_ONLY)
        {
            int i = (xcsru.getProblemCounter() + this.timeStepOffset) * LoadDataXCSRU.SITUATION_LENGTH;
            double situation[] = Arrays.copyOfRange(currentData, i, i + LoadDataXCSRU.SITUATION_LENGTH);
            this.lastSituation = situation;
            this.lastFeatureVector = generateSituationArray(situation);
            return this.lastFeatureVector;
        }
        else if (operationMode == OperationMode.AIMSUN_CLASSIFICATION)
        {
            this.lastSituation = currentSituationAimsun;
            this.lastFeatureVector = generateSituationArray(this.currentSituationAimsun);
            return this.lastFeatureVector;
        }
        else
        {
            throw new NotImplementedException("Unsupported OperationMode: " + operationMode);
        }
    }

    /**
     * Normiert die Verkehrsdaten auf einen Wert auf [0;1[.
     * @return normalised situation vector
     */
    private double[] generateSituationArray(double[] situation)
    {
        float lastPrediction = 0.99f;
        if (this.lastSituation[LoadDataXCSRU.SITUATION_LENGTH - 1] == 0) {
            lastPrediction = 0;
        }

        double[] adjustedSituation = new double[9];
        final int UNUSED_PARAMETER = -100;

        adjustedSituation[0] = !DENSITY ? UNUSED_PARAMETER : HelperFunctions.normalize(situation[LoadDataXCSRU.DENSITY1], 0, MAXDENSITY1);
        adjustedSituation[1] = !DENSITY ? UNUSED_PARAMETER : HelperFunctions.normalize(situation[LoadDataXCSRU.DENSITY2], 0, MAXDENSITY2);
        adjustedSituation[2] = !OCCUPANCY ? UNUSED_PARAMETER : HelperFunctions.normalize(situation[LoadDataXCSRU.OCCUPANCY1], 0, MAXOCCUPANCY1);
        adjustedSituation[3] = !OCCUPANCY ? UNUSED_PARAMETER : HelperFunctions.normalize(situation[LoadDataXCSRU.OCCUPANCY2], 0, MAXOCCUPANCY2);
        adjustedSituation[4] = !SPEED ? UNUSED_PARAMETER : HelperFunctions.normalize(situation[LoadDataXCSRU.SPEED1], 0, MAXSPEED1);
        adjustedSituation[5] = !SPEED ? UNUSED_PARAMETER : HelperFunctions.normalize(situation[LoadDataXCSRU.SPEED2], 0, MAXSPEED2);
        adjustedSituation[6] = !VOLUME ? UNUSED_PARAMETER : HelperFunctions.normalize(situation[LoadDataXCSRU.VOLUME1], 0, MAXCOUNT1);
        adjustedSituation[7] = !VOLUME ? UNUSED_PARAMETER : HelperFunctions.normalize(situation[LoadDataXCSRU.VOLUME2], 0, MAXCOUNT2);
        adjustedSituation[8] = !this.useLastPrediction ? UNUSED_PARAMETER : lastPrediction;

        // remove unused parameters
        for (int j = 0; j < adjustedSituation.length; j++) {
            if (adjustedSituation[j] == UNUSED_PARAMETER) {
                adjustedSituation = ArrayUtils.remove(adjustedSituation, j);
                j--;
            } else if (adjustedSituation[j] < 0) { // sensor values are faulty
                adjustedSituation[j] = 0;
            } else if (adjustedSituation[j] > 1.0) { // sensor values are faulty
                xcsru.log("Situation value > 1.0: " + adjustedSituation[j] + " at j=" + j);
            }
        }
        return adjustedSituation;
    }

    /**
     * Derive the max. values of the used features.
     */
    private void determineMaxValues(double[] data) {
        if (VOLUME) {
            MAXCOUNT1 = getMaxValue(data, LoadDataXCSRU.VOLUME1) + 1;
            MAXCOUNT2 = getMaxValue(data, LoadDataXCSRU.VOLUME2) + 1;
        }

        if (SPEED) {
            MAXSPEED1 = getMaxValue(data, LoadDataXCSRU.SPEED1) + 1;
            MAXSPEED2 = getMaxValue(data, LoadDataXCSRU.SPEED2) + 1;
        }

        if (OCCUPANCY) {
            MAXOCCUPANCY1 = getMaxValue(data, LoadDataXCSRU.OCCUPANCY1) + 1;
            MAXOCCUPANCY2 = getMaxValue(data, LoadDataXCSRU.OCCUPANCY2) + 1;
        }

        if (DENSITY) {
            MAXDENSITY1 = getMaxValue(data, LoadDataXCSRU.DENSITY1) + 1;
            MAXDENSITY2 = getMaxValue(data, LoadDataXCSRU.DENSITY2) + 1;
        }
    }

    public void setMaxValues(double MAXCOUNT1, double MAXCOUNT2, double MAXSPEED1, double MAXSPEED2, double MAXOCCUPANCY1, double MAXOCCUPANCY2, double MAXDENSITY1, double MAXDENSITY2)
    {
        this.MAXCOUNT1 = MAXCOUNT1;
        this.MAXCOUNT2 = MAXCOUNT2;
        this.MAXSPEED1 = MAXSPEED1;
        this.MAXSPEED2 = MAXSPEED2;
        this.MAXOCCUPANCY1 = MAXOCCUPANCY1;
        this.MAXOCCUPANCY2 = MAXOCCUPANCY2;
        this.MAXDENSITY1 = MAXDENSITY1;
        this.MAXDENSITY2 = MAXDENSITY2;
    }

    private static double getMaxValue(double[] values, int type) {
        double[] array = new double[values.length / LoadDataXCSRU.SITUATION_LENGTH];
        int j = 0;
        for (int i = type; i < values.length; i += LoadDataXCSRU.SITUATION_LENGTH) {
            array[j] = values[i];
            j++;
        }
        return findMax(array);
    }

    private static double findMax(double[] numbers) {
        double highest = numbers[0];
        for (int index = 1; index < numbers.length; index++) {
            if (numbers[index] > highest) {
                highest = numbers[index];
            }
        }
        return highest;
    }

    @Override
    public double[] getCurrentState() {
        return this.lastFeatureVector;
    }

    @Override
    public double executeAction(int action) {

        boolean congestionDetected = action == 0;
        boolean isCongested;
        float time;

        if (operationMode == OperationMode.OFFLINE_EVALUATION || operationMode == OperationMode.OFFLINE_TRAINING_ONLY)
        {
            if (this.lastSituation[LoadDataXCSRU.SITUATION_LENGTH - 1] == 1)
            {
                isCongested = true;
            }
            else
            {
                isCongested = false;
            }
            time = Math.round((float)lastSituation[LoadDataXCSRU.TIME] * 10f) / 10f;;
        }
        else if (operationMode == OperationMode.AIMSUN_CLASSIFICATION)
        {
            isCongested = false;
            for (AbstractDetectorGroup pair : xcsru.getMonitoringZone().getMonitoredDetectorPairs())
            {
                if (AimsunPolicyStatus.isAnyPolicyActive(pair.getId()))
                {
                    isCongested = true;
                    break;
                }
            }
            time = Math.round(currentSituationAimsun[LoadDataXCSRU.TIME] * 10f) / 10f;
        }
        else
        {
            throw new NotImplementedException("Unsupported OperationMode: " + operationMode);
        }

        psPredictionActualLabel.println(xcsru.getProblemCounter() + ";" + time + ";" + (congestionDetected ? 1 : 0) + ";" + (isCongested ? 1 : 0));
//        xcsru.psIncidents.println(currentSituationAimsun[LoadDataXCSRU.TIME] + ";" + isCongested + ";" + congestionDetected);

        // action=0: CONGESTION
        if (congestionDetected && isCongested) {
            this.lastPredictionWasCorrect = true;
            if (trainingPhaseFinished) {
                xcsru.TP++;
            }
            return getMaxPayoff();
        }
        else if (congestionDetected && !isCongested) {
            this.lastPredictionWasCorrect = false;
            if (trainingPhaseFinished) {
                xcsru.FP++;
            }
        }
        else if (!congestionDetected && !isCongested) {
            this.lastPredictionWasCorrect = true;
            if (trainingPhaseFinished) {
                xcsru.TN++;
            }
            return getMaxPayoff();
        }
        else if (!congestionDetected && isCongested) {
            this.lastPredictionWasCorrect = false;
            if (trainingPhaseFinished) {
                xcsru.FN++;
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
