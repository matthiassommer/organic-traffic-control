package tests.evaluation.aid.xcsrUrban;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.OperationMode;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.XCSRUrbanParameters;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.environments.CongestionDetectionEnvironment;
//import tests.evaluation.aid.AIDTrafficDataReader;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by oc6admin on 03.02.2016.
 */
public class XCSRUrbanParameterEvaluation
{
    static int[] maxPopSize_values = {500, 2500, 4500};
    static double[] beta_values = {0.1, 0.2};
    static double[] theta_GA_values = {25, 50};
    static double[] pX_values = {0.5, 1.0};
    static double[] pM_values = {0.01, 0.05};
    static double[] predictionErrorReduction_values = {0.1, 1.0};

    static boolean crossValidation = true;
    static int numThreads = 8;
    static Thread[] threads = new Thread[numThreads];


    public static void main(String[] args)
    {
        double[][] data = new double[LoadDataXCSRU.NUMBER_OF_EVALUATION_FOLDERS][];
        for (int i = 0; i < (LoadDataXCSRU.NUMBER_TRAINING_DAYS + LoadDataXCSRU.NUMBER_TEST_DAYS); i++)
        {
            try
            {
                for (int j = 0; j < LoadDataXCSRU.NUMBER_OF_EVALUATION_FOLDERS; j++)
                {
                    data[j] = null;
                    if (crossValidation) {
                        data[j] = LoadDataXCSRU.partitionDataForCrossvalidation(LoadDataXCSRU.loadSequentialData(j), i);
                    } else {
                        data[j] = LoadDataXCSRU.loadSequentialData(j);
                    }
                }
                System.out.println("Evaluating " + i);
                evaluate(data, i);

                if (!crossValidation)
                    break;
            }
            catch (OutOfMemoryError e)
            {
                System.out.println(e.toString());
                System.out.println("Exception bei " + i);
            }
        }

        System.out.println("generating optimal population");
//        generateXCSRUPopulation(data);

        System.out.println("end");

    }

    private static void evaluate(double[][] data, int crossValidationIndex)
    {
        int count = 0;

        for (int i = 0; i < maxPopSize_values.length; i++)
            for (int j = 0; j < beta_values.length; j++)
                for (int k = 0; k < theta_GA_values.length; k++)
                    for (int l = 0; l < pX_values.length; l++)
                        for (int m = 0; m < pM_values.length; m++)
                            for (int n = 0; n < predictionErrorReduction_values.length; n++)
                            {
                                int identifier = n * 1 + m * 10 + l * 100 + k * 1000 + j * 10000 + i * 100000;
                                String evalFolder = LoadDataXCSRU.pathResults + crossValidationIndex + "\\" + String.format("%06d", identifier);
                                if (Files.exists(Paths.get(evalFolder)))
                                {
                                    continue;
                                }

                                // block here if no free thread slot is available
                                boolean allAlive = true;
                                int freeThread = -1;
                                while (allAlive)
                                {
                                    for (int x = 0; x < threads.length; x++)
                                    {
                                        if (threads[x] == null || !threads[x].isAlive())
                                        {
                                            allAlive = false;
                                            freeThread = x;
                                        }
                                    }

                                    // avoid high CPU load caused by busy-wait while waiting for a free thread slot
                                    if (allAlive)
                                    {
                                        try
                                        {
                                            Thread.sleep(100);
                                        }
                                        catch (Exception e)
                                        {
                                            System.err.println(e.toString());
                                        }
                                    }
                                }

                                CongestionDetectionEnvironment env = new CongestionDetectionEnvironment();
                                env.setParameters(maxPopSize_values[i], beta_values[j], theta_GA_values[k], pX_values[l], pM_values[m], predictionErrorReduction_values[n]);
                                env.setUseLastPrediction(false);
                                env.init(false, true, true, false, data, count++, evalFolder, OperationMode.OFFLINE_EVALUATION, false);
                                threads[freeThread] = null;
                                threads[freeThread] = new Thread(env);
                                env.setDataSet(data);
                                threads[freeThread].start();
                            }
    }

    private static void generateXCSRUPopulation(double[][] data)
    {
        final int chosenMaxPopSize = 4500;
        final double chosenBeta = 0.2;
        final double chosenThetaGA = 50;
        final double chosenPX = 0.5;
        final double chosenPM = 0.05;
        final double chosenPredictionErrorReduction = 1.0;

        // cache the number of days to change them for this method only
        int NUMBER_TRAINING_DAYS = LoadDataXCSRU.NUMBER_TRAINING_DAYS;
        int NUMBER_TEST_DAYS = LoadDataXCSRU.NUMBER_TEST_DAYS;
        LoadDataXCSRU.NUMBER_TRAINING_DAYS = 10;
        LoadDataXCSRU.NUMBER_TEST_DAYS = 0;

        CongestionDetectionEnvironment env = new CongestionDetectionEnvironment();
        env.setParameters(chosenMaxPopSize, chosenBeta, chosenThetaGA, chosenPX, chosenPM, chosenPredictionErrorReduction);
        env.setUseLastPrediction(false);
        String evalFolder = LoadDataXCSRU.pathResults + "optimal\\0\\";
        env.init(false, true, true, false, data, 99, evalFolder, OperationMode.OFFLINE_TRAINING_ONLY, true);
        env.run();

        LoadDataXCSRU.NUMBER_TRAINING_DAYS = NUMBER_TRAINING_DAYS;
        LoadDataXCSRU.NUMBER_TEST_DAYS = NUMBER_TEST_DAYS;
    }
}
