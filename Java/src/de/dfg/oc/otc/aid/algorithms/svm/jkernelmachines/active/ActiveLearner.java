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

    Copyright David Picard - 2014

*/
package de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.active;

import java.util.List;

import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.classifier.Classifier;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.type.TrainingSample;

/**
 * <p>
 * Base abstract class for active learning strategies. 
 * </p>
 * <p>
 * This class contains an instance
 * of a classifier and a training set of samples. The classifier can be updated externaly, 
 * or thanks to helpers present in the class 
 * </p>
 * @author picard
 *
 */
public abstract class ActiveLearner<T> {
		
	protected Classifier<T> classifier;
	protected List<TrainingSample<T>> train;
	
	/**
	 * Method returning the best training sample in the list with respect to the active strategy
	 * @return the best sample
	 */
	public abstract TrainingSample<T> getActiveSample(List<TrainingSample<T>> l);
	
	/**
	 * perform nbSample updates of the classifier using the active strategy
	 * @param nbSamples
	 */
	public abstract void updateClassifier(int nbSamples);
	
	/**
	 * Setter for the classifier
	 * @param cls
	 */
	public void setClassifier(Classifier<T> cls) {
		this.classifier = cls;
	}
	
	/**
	 * Getter for the classifier
	 * @return the classifier
	 */
	public Classifier<T> getClassifier() {
		return this.classifier;
	}

	/**
	 * Return the list of training samples
	 * @return the list of training samples
	 */
	public List<TrainingSample<T>> getTrain() {
		return train;
	}

	/**
	 * Sets the list of training samples
	 * @param train the list of training samples
	 */
	public void setTrain(List<TrainingSample<T>> train) {
		this.train = train;
	}

}
