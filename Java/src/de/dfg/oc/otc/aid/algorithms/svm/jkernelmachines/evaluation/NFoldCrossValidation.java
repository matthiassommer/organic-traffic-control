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

import java.util.ArrayList;
import java.util.List;

import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.classifier.Classifier;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.type.TrainingSample;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.util.DebugPrinter;

/**
 * <p>Class for performing N-Fold Cross-validation.</p> 
 * <p>The list of samples used is taken in order. Let us consider 10 folds.
 * For the first fold, the first 10% are used for testing, and the remaining 
 * 90% are used for training. For the second fold, the second 10% are used for
 * testing, and the remaining for training, and so on.
 * <b>Warning, no randomization is performed on the list, so be careful it is not
 * in the order of the classes which would bias the learning.</b>
 * This CV is balanced by default
 * </p>
 * @author picard
 *
 */
public class NFoldCrossValidation<T> implements CrossValidation, BalancedCrossValidation {
	
	boolean balanced = true;
	int N = 5;
	Classifier<T> classifier;
	List<TrainingSample<T>> list;
	Evaluator<T> evaluator;
	
	double[] results;
	
	DebugPrinter debug = new DebugPrinter();
	
	
	/**
	 * Default constructor with number of folds, classifier, full samples list and evaluation metric.
	 * @param n the number of folds
	 * @param cls the classifier to evaluate
	 * @param l the full list of sample
	 * @param eval the evaluation metric to compute on each fold
	 */
	public NFoldCrossValidation(int n, Classifier<T> cls, List<TrainingSample<T>> l, Evaluator<T> eval) {
		N = Math.max(n, 2); // avoid 1 fold or less cv ;)
		classifier = cls;
		evaluator = eval;
		list = new ArrayList<TrainingSample<T>>();
		list.addAll(l);
	}
	

	/* (non-Javadoc)
	 * @see fr.lip6.evaluation.CrossValidation#run()
	 */
	@Override
	public void run() {
		results = new double[N];
		
		List<TrainingSample<T>> pos = new ArrayList<TrainingSample<T>>();
		List<TrainingSample<T>> neg = new ArrayList<TrainingSample<T>>();
		for(TrainingSample<T> t : list) {
			if(t.label == 1) {
				pos.add(t);
			}
			else {
				neg.add(t);
			}
		}

		for (int n = 0 ; n < N ; n++) {
			
			//setting nth fold
			List<TrainingSample<T>> test = new ArrayList<TrainingSample<T>>();
			List<TrainingSample<T>> train = new ArrayList<TrainingSample<T>>();
			if(balanced) {
				int step = pos.size() / N;
				test.addAll(pos.subList(n*step, (n+1)*step));
				train.addAll(pos);
				step = neg.size() / N;
				test.addAll(neg.subList(n*step, (n+1)*step));
				train.addAll(neg);
				train.removeAll(test);
			}
			else {
				int step = list.size() / N;
				test.addAll(list.subList(n*step, (n+1)*step));
				train.addAll(list);
				train.removeAll(test);
			}
			
			debug.println(4, "train size: "+train.size());
			debug.println(4, "test size: "+test.size());
			
			//setting evaluator
			evaluator.setClassifier(classifier);
			evaluator.setTrainingSet(train);
			evaluator.setTestingSet(test);
			
			//train and compute results
			evaluator.evaluate();
			
			//get score
			results[n] = evaluator.getScore();			
		}
		

	}

	/* (non-Javadoc)
	 * @see fr.lip6.evaluation.CrossValidation#getAverageScore()
	 */
	@Override
	public double getAverageScore() {
		if(results == null)
			return Double.NaN;
		
		double ave = 0;
		
		for(double d : results)
			ave += d;
		
		return ave/results.length;
	}

	/* (non-Javadoc)
	 * @see fr.lip6.evaluation.CrossValidation#getStdDevScore()
	 */
	@Override
	public double getStdDevScore() {
		if(results == null)
			return Double.NaN;
		
		double std = 0;
		double ave = getAverageScore();
		
		for(double d : results)
			std += (d-ave)*(d-ave);
		
		return Math.sqrt(std/results.length);
	}

	/* (non-Javadoc)
	 * @see fr.lip6.evaluation.CrossValidation#getScores()
	 */
	@Override
	public double[] getScores() {
		return results;
	}


	/**
	 * Returns true if the splits are balanced between positive and negative
	 * @return
	 */
	public boolean isBalanced() {
		return balanced;
	}


	/**
	 * Set class balancing strategy when computing the splits
	 * @param balanced true if enables balancing
	 */
	public void setBalanced(boolean balanced) {
		this.balanced = balanced;
	}

}
