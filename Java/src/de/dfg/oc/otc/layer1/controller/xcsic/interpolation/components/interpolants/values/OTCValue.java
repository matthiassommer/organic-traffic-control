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

/**
 * Created by Dominik on 10.03.2015.
 *
 * Represents the Interpolation Result for a given situation of the OTC-System
 *
 * It consists of the following objects:
 *
 * - The situation of the nearest neighbor of the formerly passed situation
 * - The action of the nearest neighbor of the formerly passed situation
 * - The interpolated action based on the weights of all sites for the formerly passed situation
 *
 * @author rauhdomi
 */
public class OTCValue implements InterpolationValue
{
    Situation nearestNeighborSituation;
    TrafficLightControllerParameters nearestNeighborAction;
    TrafficLightControllerParameters interpolatedAction;

    public OTCValue(Situation nearestNeighborSituation,
                    TrafficLightControllerParameters nearestNeighborAction,
                    TrafficLightControllerParameters interpolatedAction)
    {
        this.nearestNeighborSituation = nearestNeighborSituation;
        this.nearestNeighborAction = nearestNeighborAction;
        this.interpolatedAction = interpolatedAction;
    }

    public Situation getSituation() { return this.nearestNeighborSituation; }

    public TrafficLightControllerParameters getNearestNeighborAction() {return this.nearestNeighborAction; }

    public TrafficLightControllerParameters getInterpolatedAction()
    {
        return this.interpolatedAction;
    }
}
