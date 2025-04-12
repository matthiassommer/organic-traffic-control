/*
 * Copyright (c) 2015 by
 * Anthony Stein, M.Sc.
 * University of Augsburg
 * Department of Computer Science
 * Chair of Organic Computing
 * All rights reserved. Distribution without approval by the copyright holder is explicitly prohibited.
 * Sources are only for non-commercial and academic use
 * in the scope of student theses and courses of the University of Augsburg.
 *
 * THE SOFTWAREPARTS ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package de.dfg.oc.otc.layer1.controller.xcscic.interpolation.components.interpolants.values;

import de.dfg.oc.otc.layer1.controller.xcscic.Classifier;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.components.interpolants.IDWInterpolant;

import java.util.Map;

/**
 * Created by Anthony on 02.03.2015.
 *
 * Encapsulates the result of an {@link IDWInterpolant}-based interpolation.
 * In addition to the results of an {@link OTCValue}, the idw-weights for the classifiers are stored.
 *
 * @author Anthony Stein
 * @author Dominik Rauh
 */
public class AccumulatedIDWOTCValue extends OTCValue
{
    private Map<Classifier, Double> weightsForClassifiers;

    public AccumulatedIDWOTCValue(Classifier neNeClassifier,
                                  Classifier interpolatedClassifier,
                                  Map<Classifier, Double> weightsForClassifiers) {
        super(neNeClassifier, interpolatedClassifier);
        this.weightsForClassifiers = weightsForClassifiers;
    }

    public Map<Classifier, Double> getWeightsForClassifiers() {
        return weightsForClassifiers;
    }

    /**
     * Returns the idw-weight for a specific action (denoted by its hash) of a classifier
     * @param actionHash The action hash to get the weight for
     * @return The weight for an action. When no classifier whith the passed action exists, zero is returned
     */
    public double getAccumulatedIDWeightForActionHash(int actionHash) {

        for(Map.Entry<Classifier, Double> entry : this.weightsForClassifiers.entrySet())
        {
            if(entry.getKey().getAction().hashCode() == actionHash)
            {
                return  entry.getValue();
            }
        }
        //No matching action from classifier found; return default weight
        return 0;
    }
}
