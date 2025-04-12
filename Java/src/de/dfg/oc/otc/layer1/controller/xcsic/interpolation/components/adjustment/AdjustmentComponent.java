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

package de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.adjustment;

import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationComponent;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.Situation;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants.AbstractInterpolationTechnique;

import java.util.HashMap;

/**
 * Created by Anthony Stein on 28.03.14.
 *
 * Diese Klasse implementiert die Logik der Subkomponente "Adjustment Component" der Interpolations-Komponente IC
 */
public class AdjustmentComponent {

    private double maxReward;

    // Speichert den zuletzt erhaltenen Reward vom SuOC zwischen.
    double lastReceivedPayoff;

    // Eine Aufzählung aller implementierten Entscheidungsfunktionen (die Einträge entsprechen den Identifiern)
    public enum DecisionFunctions {
        SIMPLE_DECISION,
        WEBSTERS_DECISION
    }

    // Ermöglicht die Kommunikation mit der IC-Subkomponente "Interpolant"
    private AbstractInterpolationTechnique correspondingInterpolant;

    // Hält alle parallel zum Einsatz kommenden Entscheidungsfunktionen
    private HashMap<DecisionFunctions, AbstractDecisionFunction> decFunc = new HashMap<DecisionFunctions, AbstractDecisionFunction>();

    /**
     * Der Konstruktor der <code>AdjustmentComponent</code>
     *
     * @param interpolant Die entsprechende Instanz der IC-Subkomponente "Interpolant"
     */
    public AdjustmentComponent(InterpolationComponent interpolationComponent,
                               AbstractInterpolationTechnique interpolant,
                               double maxReward) {
        this.correspondingInterpolant = interpolant;
        this.decFunc.put(DecisionFunctions.WEBSTERS_DECISION, new WebstersDecisionFunction(interpolationComponent));
        this.maxReward = maxReward;
    }

    public void setReceivedPayoff(double payoff) {
        this.lastReceivedPayoff = payoff;
    }

    /**
     * Wird von der IC direkt aufgerufen sobald eine Entscheidung benötigt wird.
     * Hier kann die kombinierte Entscheidung aus allen Entscheidungsfunktionen getroffen werden.
     *
     * @param situation Die Situation unter der der Reward erzielt wurde
     * @param reward Der vom SuOC zurückgelieferte Payoff/Reward r
     * @param a_exec Die tatsächlich ausgeführte Aktion a_exec
     * @param a_int Die von der Interpolante vorgeschlagene Aktion a_int
     * @return Ein boolscher Wert, der aussagt, ob eine neue Stützstelle hinzugefügt werden soll, oder nicht.
     */
    public boolean decideOnAddingNewSamplingPoint(Situation situation, Object a_exec, Object a_int, double reward) {
        AbstractDecisionFunction decisionFunction = this.decFunc.get(DecisionFunctions.WEBSTERS_DECISION);
        return decisionFunction.decide(situation, a_exec, a_int, reward, maxReward);
    }
}
