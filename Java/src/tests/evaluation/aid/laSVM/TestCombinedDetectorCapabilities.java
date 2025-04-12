package tests.evaluation.aid.laSVM;

import de.dfg.oc.otc.aid.algorithms.svm.IncidentStorage;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.tools.FileUtilities;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

/**
 * Test the SVM with different types of detector input such as SPEED+DENSITY values.
 *
 * @author Matthias Sommer.
 */
public class TestCombinedDetectorCapabilities {
    private int[][] combination = {{DetectorCapabilities.SPEED},
            {DetectorCapabilities.SPEED, DetectorCapabilities.COUNT},
            {DetectorCapabilities.SPEED, DetectorCapabilities.DENSITY},
            {DetectorCapabilities.COUNT, DetectorCapabilities.DENSITY}};

    @Test
    public void runS637() {
        run("S637", "2448");
    }

    @Test
    public void runS86() {
        run("S86", "366");
    }

    private void run(String station, String detector) {
        // incidentarchive[detector][DetectorCapacityValue]
        IncidentStorage[] incidents = new IncidentStorage[combination.length];
        Map<String, Object> parameter = EvaluationUtilities.getParameter();

        int sampleStartTime = EvaluationUtilities.endDateTesting * 24 * 60 * 60;
        // [#Test][#Day][Metrics]
        float[][][] allResult = new float[combination.length][30][9];

        for (int i = 0; i < combination.length; i++) {
            parameter.replace("detectorCapabilities", combination[i]);

            incidents[i] = EvaluationUtilities.runEvaluation(station, detector, parameter);
            float[] result = EvaluationUtilities.getMetrics(incidents[i]);

            String filename = EvaluationUtilities.EVALUATION_FOLDER + "CombinedDetectorCapabilities\\" + station + "Results";
            if (i == 0) {
                filename += combination[i][0];
                FileUtilities.createNewFile(filename + ".csv");
                EvaluationUtilities.appendLineToFile(combination[i][0] + " " + Arrays.toString(result), filename + ".csv");
            } else {
                filename += combination[i][0] + "-" + combination[i][1];
                FileUtilities.createNewFile(filename + ".csv");
                EvaluationUtilities.appendLineToFile(combination[i][0] + " " + combination[i][1] + " " + Arrays.toString(result), filename + ".csv");
            }

            allResult[i] = EvaluationUtilities.evaluateTestResultPerDay(incidents[i], sampleStartTime, filename + "perDay.csv");
        }

        printLatexTableMeanStddev(allResult);
    }

    /**
     * allResult have to be the size allResult [8][30][8]
     * with [Test][Day][Values] (Values = TruePositive, TrueNegative, ...., Precision, Recall, etc
     * First 2 Tests are not regarded in table!
     *
     * @param allResult
     */
    private void printLatexTableMeanStddev(float[][][] allResult) {
        int numberCombinations = combination.length;
        int days = EvaluationUtilities.endDateTesting;
        int numberMetrics = 9;

        float[][] meanResult = new float[numberCombinations][numberMetrics];
        for (int day = 0; day < days; day++) {
            for (int combi = 0; combi < numberCombinations; combi++) {
                for (int metric = 0; metric < numberMetrics; metric++) {
                    meanResult[combi][metric] += Float.isNaN(allResult[combi][day][metric]) ? 0 : allResult[combi][day][metric];
                }
            }
        }

        for (int combi = 0; combi < numberCombinations; combi++) {
            for (int metric = 4; metric < numberMetrics; metric++) {
                meanResult[combi][metric] /= (float) days;
            }
        }

        // compute variances
        float[][] varianceResult = new float[numberCombinations][numberMetrics - 4];

        // sum over all days
        for (int day = 0; day < days; day++) {
            for (int combi = 0; combi < numberCombinations; combi++) {
                for (int metric = 4; metric < numberMetrics; metric++) {
                    float value = Float.isNaN(allResult[combi][day][metric - 4]) ? 0 : allResult[combi][day][metric - 4];
                    varianceResult[combi][metric - 4] += (value - meanResult[combi][metric - 4]) * (value - meanResult[combi][metric - 4]);
                }
            }
        }

        // average variances
        for (int combi = 0; combi < numberCombinations; combi++) {
            for (int metric = 4; metric < numberMetrics; metric++) {
                varianceResult[combi][metric - 4] /= (float) days;
            }
        }

        // compute standard deviation
        for (int combi = 0; combi < numberCombinations; combi++) {
            for (int metric = 4; metric < numberMetrics; metric++) {
                varianceResult[combi][metric - 4] = (float) Math.sqrt(varianceResult[combi][metric - 4]);
            }
        }

        System.out.println("\\begin{tabular}{|l||l|l|l||l|l|l|}");
        System.out.println(" & \\multicolumn{3}{|l|}{S86} & \\multicolumn{3}{|l|}{S637} \\\\");
        System.out.println(" & Speed & Speed+Flow & Speed+Density & Flow+Density & Speed & Speed+Flow & Speed+Density & Flow+Density \\\\");

        System.out.print("Precision $\\mu$:");
        for (int combi = 0; combi < numberCombinations; combi++) {
            System.out.print(" & " + Math.round(meanResult[combi][4] * 100) / 100f);
        }
        System.out.println("\\\\");

        System.out.print("Precision $\\sigma$:");
        for (int k = 0; k < numberCombinations; k++) {
            System.out.print(" & " + Math.round(varianceResult[k][0] * 100) / 100f);
        }
        System.out.println("\\\\");

        System.out.print("Recall $\\mu$:");
        for (int k = 0; k < numberCombinations; k++) {
            System.out.print(" & " + Math.round(meanResult[k][5] * 100) / 100f);
        }
        System.out.println("\\\\");

        System.out.print("Recall $\\sigma$:");
        for (int k = 0; k < numberCombinations; k++) {
            System.out.print(" & " + Math.round(varianceResult[k][1] * 100) / 100f);
        }
        System.out.println("\\\\");

        System.out.print("F-measure $\\mu$:");
        for (int k = 0; k < numberCombinations; k++) {
            System.out.print(" & " + Math.round(meanResult[k][7] * 100) / 100f);
        }
        System.out.println("\\\\");

        System.out.print("F-measure $\\sigma$:");
        for (int k = 0; k < numberCombinations; k++) {
            System.out.print(" & " + Math.round(varianceResult[k][3] * 100) / 100f);
        }
        System.out.println("\\\\");

        System.out.print("Accuracy $\\mu$:");
        for (int k = 0; k < numberCombinations; k++) {
            System.out.print(" & " + Math.round(meanResult[k][6] * 100) / 100f);
        }
        System.out.println("\\\\");

        System.out.print("Accuracy $\\sigma$:");
        for (int k = 0; k < numberCombinations; k++) {
            System.out.print(" & " + Math.round(varianceResult[k][2] * 100) / 100f);
        }
        System.out.println("\\\\");
        System.out.println("\\end{tabular}");

        for (int i = 0; i < numberCombinations; i++) {
            System.out.println(Arrays.toString(meanResult[i]));
        }
        System.out.println();

        for (int i = 0; i < numberCombinations; i++) {
            System.out.println(Arrays.toString(varianceResult[i]));
        }
    }
}

