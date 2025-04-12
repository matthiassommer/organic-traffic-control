/**
    This file is part of JkernelMachines.

    JkernelMachines is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    JkernelMachines is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with JkernelMachines.  If not, see <http://www.gnu.org/licenses/>.

    Copyright David Picard - 2012

 */
package de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.evaluation;

import java.util.List;

import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.classifier.Classifier;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.classifier.multiclass.MulticlassClassifier;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.type.TrainingSample;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.util.DebugPrinter;

/**
 * Evaluation class for computing the multiclass accuracy on a testing set,
 * given a provided cmulticlass classifier.
 * 
 * @author picard
 * 
 */
public class MulticlassAccuracyEvaluator<T> implements Evaluator<T> {

	MulticlassClassifier<T> classifier;
	List<TrainingSample<T>> trainList;
	List<TrainingSample<T>> testList;
	double accuracy;

	DebugPrinter debug = new DebugPrinter();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.lip6.evaluation.AIDEvaluator#setClassifier(fr.lip6.classifier.Classifier)
	 */
	@Override
	public void setClassifier(Classifier<T> cls) {
		if (cls instanceof MulticlassClassifier<?>) {
			classifier = (MulticlassClassifier<T>) cls;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.lip6.evaluation.AIDEvaluator#setTrainingSet(java.util.List)
	 */
	@Override
	public void setTrainingSet(List<TrainingSample<T>> trainlist) {
		this.trainList = trainlist;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.lip6.evaluation.AIDEvaluator#setTestingSet(java.util.List)
	 */
	@Override
	public void setTestingSet(List<TrainingSample<T>> testlist) {
		this.testList = testlist;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.lip6.evaluation.AIDEvaluator#evaluate()
	 */
	@Override
	public void evaluate() {
		// train
		if(classifier != null && trainList != null) {
			classifier.train(trainList);
		}
		
		debug.println(1, "training done, testing begins");

		double top = 0;
		for (TrainingSample<T> t : testList) {
			if (classifier.valueOf(t.sample) == t.label)
				top++;
		}
		accuracy = top / testList.size();
		debug.println(2, "Multiclass accuracy: "+accuracy);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.lip6.evaluation.AIDEvaluator#getScore()
	 */
	@Override
	public double getScore() {
		return accuracy;
	}

}
