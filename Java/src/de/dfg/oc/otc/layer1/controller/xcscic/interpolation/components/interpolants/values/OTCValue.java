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

import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer1.controller.xcscic.Classifier;
import de.dfg.oc.otc.layer1.controller.xcscic.XCSCIC;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.Situation;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.components.interpolants.AbstractInterpolationTechnique;

/**
 * Created by Dominik on 10.03.2015.
 *
 * Encapsulates the result from an interpolation in the {@link XCSCIC}
 * This is the {@link Classifier} which is closest to the query point {@see Situation}
 * Also an interpolated classifier is returned based on the used {@link AbstractInterpolationTechnique}
 *
 * @author Dominik Rauh
 *
 */
public class OTCValue implements InterpolationValue
{
    protected Classifier nearestNeighborClassifier;
    protected Classifier interpolatedClassifier;

    public OTCValue(Classifier nearestNeighborClassifier,
                    Classifier interpolatedClassifier)
    {
        this.nearestNeighborClassifier = nearestNeighborClassifier;
        this.interpolatedClassifier = interpolatedClassifier;
    }

    public Classifier getNearestNeighborClassifier() {
        return nearestNeighborClassifier;
    }

    public Classifier getInterpolatedClassifier() {
        return interpolatedClassifier;
    }
}
