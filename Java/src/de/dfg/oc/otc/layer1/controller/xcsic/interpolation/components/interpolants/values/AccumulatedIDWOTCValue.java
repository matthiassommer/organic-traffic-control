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

package de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants.values;

import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.Situation;

import java.util.Map;

/**
 * Created by Anthony on 02.03.2015.
 *
 * Holds the interpolation results of the IDWInterpolant which are:
 *
 * - The situation of the nearest neighbor
 * - The action (TrafficLightControllerParameters) of the nearest neighbor
 * - The interpolated action based on the weights of all sites for the formerly passed situation
 * - The weights of the actions which belong to the sites of the interpolant for the formerly passed situation
 *
 * @author Anthony Stein (edited by rauhdomi)
 */
public class AccumulatedIDWOTCValue extends OTCValue
{
    private Map<TrafficLightControllerParameters, Double> weightsForAction;

    public AccumulatedIDWOTCValue(Situation nearestNeighborSituation,
                                  TrafficLightControllerParameters nearestNeighborAction,
                                  TrafficLightControllerParameters interpolatedAction,
                                  Map<TrafficLightControllerParameters, Double> weightsForAction) {
        super(nearestNeighborSituation, nearestNeighborAction, interpolatedAction);
        this.weightsForAction = weightsForAction;
    }

    public Map<TrafficLightControllerParameters, Double> getWeightsForActions() {
        return weightsForAction;
    }

    public double getAccumulatedIDWeightForActionHash(int actionHash) {

        for(Map.Entry<TrafficLightControllerParameters, Double> entry : weightsForAction.entrySet())
        {
            if(entry.getKey().hashCode() == actionHash)
            {
                return  entry.getValue();
            }
        }
        //No Action found return default weight
        return 0;
    }
}
