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

/**
 * Standard interface for all cross-validation methods
 * 
 * @author picard
 *
 */
public interface CrossValidation {
	
	/**
	 * perform learning and evaluations
	 */
	public void run();
	
	/**
	 * Tells the average score of the test
	 * @return the average score
	 */
	public double getAverageScore();
	
	/**
	 * Tells the standard deviation of the test
	 * @return the standard deviation
	 */
	public double getStdDevScore();
	
	/**
	 * Tells the scores of the tests, in order of evaluation
	 * @return an array with the scores in order
	 */
	public double[] getScores(); 

}
