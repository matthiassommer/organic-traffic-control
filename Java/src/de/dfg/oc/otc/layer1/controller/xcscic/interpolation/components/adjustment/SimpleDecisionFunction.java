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

package de.dfg.oc.otc.layer1.controller.xcscic.interpolation.components.adjustment;

import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.InterpolationComponent;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.Situation;

/**
 * Created by Anthony Stein on 30.05.2014.
 *
 * Die Implementierung einer simplen Entscheidungsfunktion.
 * Wenn die ausgeführte Aktion a_exec korrekt war, soll eine neue Stützstelle hinzugefügt werden.
 */
public class SimpleDecisionFunction extends AbstractDecisionFunction {

    private static final double DECISION_THRESHOLD = 0.5;

    /**
     * Standardkonstruktor. Belegt die Instanzvariablen die von der Basisklasse
     * <code>AbstractDecisionFunction</code> geerbt werden.
     *
     * @see de.dfg.oc.otc.layer1.controller.xcscic.interpolation.components.adjustment.AbstractDecisionFunction
     */
    public SimpleDecisionFunction(InterpolationComponent interpolationComponent) {
        super(interpolationComponent);
        this.id = AdjustmentComponent.DecisionFunctions.SIMPLE_DECISION;
        this.description = "Simply decides on whether the overall XCSR-Approx guess was right or not!";
    }

    @Override
    /**
     * Siehe Kommentar zur Deklaration innerhalb der Basisklasse
     * @see xcsr_ic.interpolation.components.adjustment.AbstractDecisionFunction
     */
    public boolean decide(Situation situation, Object a_exec, Object a_int, double reward, double maxReward) {
        // In diesem Fall wird angenommen, dass die ausgeführte Aktion korrekt war
        // TODO It would be better to implement a more sophisticated decision function
        return reward > DECISION_THRESHOLD * maxReward;
    }
}
