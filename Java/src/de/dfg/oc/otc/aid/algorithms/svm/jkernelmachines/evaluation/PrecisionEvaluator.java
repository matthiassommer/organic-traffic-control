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

import java.util.List;

/**
 * AIDEvaluator computing the precision defined as (number of relevant)/(number of retrieved).
 * @author picard
 *
 */
public class PrecisionEvaluator<T> implements Evaluator<T> {

    private Classifier<T> cls;
    private List<TrainingSample<T>> trainList;
    private List<TrainingSample<T>> testList;


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

    /* (non-Javadoc)
     * @see fr.lip6.jkernelmachines.evaluation.AIDEvaluator#getScore()
     */
    @Override
    public double getScore() {
        double prec;
        int tp = 0, fp = 0;

        for (TrainingSample<T> t : testList) {
            double v = cls.valueOf(t.sample);

            if (v >= 0) {
                if (t.label >= 0) {
                    tp++;
                } else {
                    fp++;
                }
            }
        }
        prec = 0;
        if (tp + fp > 0) {
            prec = tp / ((double) tp + fp);
        }

        return prec;
    }
}
