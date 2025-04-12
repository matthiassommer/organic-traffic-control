/**
 * This file is part of JkernelMachines.
 * <p>
 * JkernelMachines is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * JkernelMachines is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with JkernelMachines.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Copyright David Picard - 2013
 */
package de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.evaluation;

import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.classifier.Classifier;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.type.TrainingSample;
import de.dfg.oc.otc.aid.evaluation.CongestionMetrics;

import java.io.PrintStream;
import java.util.List;

/**
 * AIDEvaluator computing the F-score (by default F1).
 *
 * @author picard
 */
public class FScoreEvaluator<T> implements Evaluator<T> {
    private PrintStream psROCData;
    /**
     * Exports values for the confusions matrix (TP,FP,FN,TN).
     */
    private PrintStream psConfusionMatrix = null;
    private Classifier<T> cls;
    private List<TrainingSample<T>> trainList;
    private List<TrainingSample<T>> testList;
    private double beta = 1;
    private float recall;
    private float precision;
    private float accuracy;
    private float sensitivity;
    private float specificity;

    public void setPsConfusionMatrix(PrintStream psConfusionMatrix) {
        this.psConfusionMatrix = psConfusionMatrix;
    }

    public float getSpecificity() {
        return specificity;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public float getRecall() {
        return recall;
    }

    public float getPrecision() {
        return precision;
    }

    /* (non-Javadoc)
     * @see fr.lip6.jkernelmachines.evaluation.AIDEvaluator#setClassifier(fr.lip6.jkernelmachines.classifier.Classifier)
     */
    @Override
    public void setClassifier(Classifier<T> cls) {
        this.cls = cls;
    }

    /* (non-Javadoc)
     * @see fr.lip6.jkernelmachines.evaluation.AIDEvaluator#setTrainingSet(java.util.List)
     */
    @Override
    public void setTrainingSet(List<TrainingSample<T>> trainlist) {
        this.trainList = trainlist;
    }

    /* (non-Javadoc)
     * @see fr.lip6.jkernelmachines.evaluation.AIDEvaluator#setTestingSet(java.util.List)
     */
    @Override
    public void setTestingSet(List<TrainingSample<T>> testlist) {
        this.testList = testlist;
    }

    /* (non-Javadoc)
     * @see fr.lip6.jkernelmachines.evaluation.AIDEvaluator#evaluate()
     */
    @Override
    public void evaluate() {
        if (trainList != null) {
            cls.train(trainList);
        }
    }

    public double getSensitivity() {
        return sensitivity;
    }

    /* (non-Javadoc)
         * @see fr.lip6.jkernelmachines.evaluation.AIDEvaluator#getScore()
         */
    @Override
    public double getScore() {
        int tp = 0, fp = 0, fn = 0;
        int good = 0;

        for (TrainingSample<T> t : testList) {
            double classification = cls.valueOf(t.sample);

            if (t.label < 0) {
                psROCData.println((float) classification + "\t 0");
            } else {
                psROCData.println((float) classification + "\t 1");
            }

            if (classification * t.label > 0) {
                good++;
            }

            if (classification < 0) {
                if (t.label < 0) {
                    tp++;
                } else {
                    fp++;
                }
            } else {
                if (t.label < 0) {
                    fn++;
                }
            }
        }

        int tn = testList.size() - tp - fp - fn;

        psConfusionMatrix.println(tp + "\t" + fp + "\t" + tn + "\t" + fn);

        precision = 0;
        if (tp + fp > 0) {
            precision = tp / ((float) tp + fp);
        }
        recall = tp / ((float) tp + fn);
        accuracy = good / (float) testList.size();
        sensitivity = recall;
        specificity = CongestionMetrics.specificity(tn, fp);

        if (precision + recall == 0) {
            return 0;
        }
        return (1 + beta * beta) * precision * recall / (beta * beta * precision + recall);
    }

    /**
     * Get the beta value of the F-score
     *
     * @return the beta value
     */
    public double getBeta() {
        return beta;
    }

    /**
     * Sets the beta value for the F-score
     *
     * @param beta the beta value
     */
    public void setBeta(double beta) {
        this.beta = beta;
    }

    public void setROCPrinter(PrintStream psROCData) {
        this.psROCData = psROCData;
    }
}
