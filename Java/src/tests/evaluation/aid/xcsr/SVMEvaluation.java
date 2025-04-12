package tests.evaluation.aid.xcsr;

import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.classifier.LaSVM;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.classifier.LaSVMI;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.classifier.SDCA;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.evaluation.FScoreEvaluator;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.io.CsvImporter;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.kernel.typed.DoubleGaussChi2;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.type.TrainingSample;
import de.dfg.oc.otc.tools.FileUtilities;
import tests.evaluation.aid.AIDTrafficDataReader;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates the support vector machine for congestion detection.
 * Uses the same input data as the XCSR evaluation.
 * Executes 10 runs with different data sets. The data sets span 28 days of evaluation data.
 * Automatically exports the evaluation results to text files.
 * <p>
 * Created by oc6admin on 15.01.2016.
 */
class SVMEvaluation {
    private PrintStream psAllMetrics;
    /**
     * Exports the classification and the actual label for plotting the ROC curve.
     */
    private PrintStream psROCData = null;
    /**
     * Exports the classification and the actual label for plotting the ROC curve.
     */
    private PrintStream psConfusionMatrix = null;
    /**
     * Folder where the evaluation results are stored.
     */
    private String EVALUATION_FOLDER = "..\\Auswertung\\AID\\SVM\\LaSVMI\\";

    private LaSVMI<double[]> svm;
    /**
     * Kernel hyper-parameter.
     */
    private double gamma = 0.01;
    /**
     * Class margin (C*A = B).
     */
    private double C = 10.0;

    public static void main(String[] args) {
        SVMEvaluation evaluation = new SVMEvaluation();
        //evaluation.parameterStudyCandGamma();

        // analog zu XCSR Eval
        evaluation.run("COUNT", new int[]{AIDTrafficDataReader.VOLUME});
        evaluation.run("COUNT+DENSITY", new int[]{AIDTrafficDataReader.VOLUME, AIDTrafficDataReader.DENSITY});
        evaluation.run("COUNT+DENSITY+OCCUPANCY", new int[]{AIDTrafficDataReader.VOLUME, AIDTrafficDataReader.DENSITY, AIDTrafficDataReader.OCCUPANCY});
        evaluation.run("COUNT+DENSITY+OCCUPANCY+SPEED", new int[]{AIDTrafficDataReader.VOLUME, AIDTrafficDataReader.DENSITY, AIDTrafficDataReader.OCCUPANCY, AIDTrafficDataReader.SPEED});
        evaluation.run("COUNT+DENSITY+SPEED", new int[]{AIDTrafficDataReader.VOLUME, AIDTrafficDataReader.DENSITY, AIDTrafficDataReader.SPEED});
        evaluation.run("COUNT+OCCUPANCY", new int[]{AIDTrafficDataReader.VOLUME, AIDTrafficDataReader.OCCUPANCY});
        evaluation.run("COUNT+OCCUPANCY+SPEED", new int[]{AIDTrafficDataReader.VOLUME, AIDTrafficDataReader.OCCUPANCY, AIDTrafficDataReader.SPEED});
        evaluation.run("COUNT+SPEED", new int[]{AIDTrafficDataReader.VOLUME, AIDTrafficDataReader.SPEED});
        evaluation.run("DENSITY", new int[]{AIDTrafficDataReader.DENSITY});
        evaluation.run("DENSITY+OCCUPANCY", new int[]{AIDTrafficDataReader.DENSITY, AIDTrafficDataReader.OCCUPANCY});
        evaluation.run("DENSITY+OCCUPANCY+SPEED", new int[]{AIDTrafficDataReader.DENSITY, AIDTrafficDataReader.OCCUPANCY, AIDTrafficDataReader.SPEED});
        evaluation.run("DENSITY+SPEED", new int[]{AIDTrafficDataReader.DENSITY, AIDTrafficDataReader.SPEED});
        evaluation.run("OCCUPANCY", new int[]{AIDTrafficDataReader.OCCUPANCY});
        evaluation.run("OCCUPANCY+SPEED", new int[]{AIDTrafficDataReader.OCCUPANCY, AIDTrafficDataReader.SPEED});
        evaluation.run("SPEED", new int[]{AIDTrafficDataReader.SPEED});
    }

    /**
     * Run a parameter study for C and gamma.
     */
    private void parameterStudyCandGamma() {
        final int[] features = new int[]{AIDTrafficDataReader.OCCUPANCY, AIDTrafficDataReader.SPEED};

        double[] c = {1, 10, 100, 500, 1000};
        double[] gamma = {0.01, 0.1, 0.5, 1};

        for (double valueC : c) {
            this.C = valueC;
            for (double valueGamma : gamma) {
                this.gamma = valueGamma;
                run("parameterStudy\\OCCUPANCY+SPEED-" + valueC + "_" + valueGamma, features);
            }
        }
    }

    private void run(String exportFilename, int[] features) {
        initExporter(exportFilename);

        long trainTime = 0;
        long testTime = 0;

        for (int run = 0; run < AIDTrafficDataReader.NUMBER_OF_EVALUATION_RUNS; run++) {
            long time = System.currentTimeMillis();

            List<TrainingSample<double[]>> train = loadTrainData(2 * run, features);
            List<TrainingSample<double[]>> test = loadTestData(2 * run, features);

            // Feature scaling bzw. normalisation notwendig, wenn die Wertebereiche der Features zu unterschiedlich sind!

            svm = new LaSVMI<>(new DoubleGaussChi2(this.gamma));
            svm.setC(this.C);
            svm.setE(5);

            FScoreEvaluator<double[]> fs = new FScoreEvaluator<>();
            fs.setClassifier(svm);
            fs.setTrainingSet(train);
            fs.setTestingSet(test);
            fs.evaluate();
            fs.setROCPrinter(this.psROCData);
            fs.setPsConfusionMatrix(this.psConfusionMatrix);

            trainTime += System.currentTimeMillis() - time;
            time = System.currentTimeMillis();

            // calculates all metric values!
            float fmeasure = (float) fs.getScore();

            psAllMetrics.println(this.C + "\t" + this.gamma + "\t" + fs.getPrecision() + "\t" + fs.getAccuracy() + "\t" + fmeasure + "\t" + fs.getSpecificity() + "\t" + fs.getSensitivity()); //+ "\t" + fs.getRecall()
            testTime += System.currentTimeMillis() - time;
        }

        System.out.println("TRAIN " + trainTime / 1000.0 + " s., TEST " + testTime / 1000.0 + " s.");
    }

    private List<TrainingSample<double[]>> loadTestData(int run, int[] features) {
        String folder = AIDTrafficDataReader.FOLDER_NAMES[run];
        List<TrainingSample<double[]>> test = new ArrayList<>();
        try {
            for (int day = AIDTrafficDataReader.NUMBER_TRAINING_DAYS + 1; day <= AIDTrafficDataReader.NUMBER_TEST_DAYS + AIDTrafficDataReader.NUMBER_TRAINING_DAYS; day++) {
                String fileName = day + ".csv";
                if (day < 10) {
                    fileName = "0" + day + ".csv";
                } else if (day > 30) {
                    if (day % 30 < 10) {
                        fileName = "0" + String.valueOf(day % 30) + ".csv";
                    } else {
                        fileName = String.valueOf(day % 30) + ".csv";
                    }

                    folder = AIDTrafficDataReader.FOLDER_NAMES[run + 1];
                }

                List<TrainingSample<double[]>> data = CsvImporter.importFromFile(AIDTrafficDataReader.AID_DATA_FOLDER + folder + fileName, features);
                test.addAll(data);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        return test;
    }

    private List<TrainingSample<double[]>> loadTrainData(int run, int[] features) {
        String folder = AIDTrafficDataReader.FOLDER_NAMES[run];
        String fileType = ".csv";

        List<TrainingSample<double[]>> train = new ArrayList<>();
        try {
            for (int j = 1; j <= AIDTrafficDataReader.NUMBER_TRAINING_DAYS; j++) {
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
                List<TrainingSample<double[]>> data = CsvImporter.importFromFile(AIDTrafficDataReader.AID_DATA_FOLDER + folder + fileName, features);
                train.addAll(data);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        return train;
    }

    private void initExporter(String filename) {
        try {
            FileUtilities.createNewFile(EVALUATION_FOLDER + filename + ".txt");
            psAllMetrics = new PrintStream(new FileOutputStream(EVALUATION_FOLDER + filename + ".txt"), true);
            psAllMetrics.println("C \t gamma \t Precision \t Accuracy \t F-measure \t Specificity \t Sensitivity");

            FileUtilities.createNewFile(EVALUATION_FOLDER + "ROCData\\" + filename + ".txt");
            psROCData = new PrintStream(new FileOutputStream(EVALUATION_FOLDER + "ROCData\\" + filename + ".txt"), true);
            psROCData.println("Class \t Label");

            FileUtilities.createNewFile(EVALUATION_FOLDER + "ConfusionMatrix\\" + filename + ".txt");
            psConfusionMatrix = new PrintStream(new FileOutputStream(EVALUATION_FOLDER + "ConfusionMatrix\\" + filename + ".txt"), true);
            psConfusionMatrix.println("TP \t FP \t TN \t FN");
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }
}


