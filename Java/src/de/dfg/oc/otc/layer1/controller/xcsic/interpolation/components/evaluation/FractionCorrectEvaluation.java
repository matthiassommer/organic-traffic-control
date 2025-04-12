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

package de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.evaluation;

import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.Situation;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationConstants;

import java.util.ArrayList;

/**
 * Created by Anthony Stein on 30.05.2014.
 *
 * Eine simple Evaluationsmetrik, die an das "Fraction Correct" System-Performance-Maß eines XCS(-R) angelehnt ist.
 *
 */
public class FractionCorrectEvaluation extends AbstractEvaluationMetric {

    private static final double DECISION_THRESHOLD = 0.5;

    // Das Menge aller Aussagen, ob die von der IC vorgeschlagene Aktion a_int korrekt war/gewesen wäre.
    private ArrayList<Integer> evaluationWindow = new ArrayList<Integer>(InterpolationConstants.t_window);

    /**
     * Standardkonstruktor; weist den Instanzvariablen der Basisklasse entsprechende Werte zu.
     * @see de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.evaluation.AbstractEvaluationMetric
     */
    public FractionCorrectEvaluation() {
        this.id = EvaluationComponent.EvaluationMetrics.FRACTION_CORRECT;
        this.description = "A simple fraction of correct proposals over the last n (see t_window) guesses from the IC!";
    }

    @Override
    /**
     * Siehe Kommentar zur Deklaration innerhalb der Basisklasse.
     * @see xcsr_ic.interpolation.components.evaluation.AbstractEvaluationMetric
     */
    public void evaluate(Situation situation, Object a_exec, Object a_int, double reward, double maxReward) {

        boolean wasCorrect = (reward > DECISION_THRESHOLD * maxReward);

        // Lag die IC richtig, wird eine 1 (richtig) hinzugefügt, ansonsten eine 0 (falsch)
        // Richtig lag die IC immer dann, wenn die ausgeführte Aktion korrekt war und mit der
        // von der IC vorgeschlagenen Aktion übereinstimmt, oder keine Übereinstimmung vorliegt,
        // und gleichzeitig die tatsächlich ausgeführte Aktion falsch war.
        if(wasCorrect && a_exec == a_int) {
            this.evaluationWindow.add(1);
        } else if(!wasCorrect && a_exec != a_int) {
            this.evaluationWindow.add(1);
        } else {
            this.evaluationWindow.add(0);
        }

        // Nach Erreichen der Evaluationsfenstergröße t_window wird der Evaluationswert neu berechnet
        // und das Evaluationsfenster geleert.
        if(evaluationWindow.size() >= InterpolationConstants.t_window) {
            int sumOfFractionCorrect = 0;
            for (Integer correct : this.evaluationWindow) {
                sumOfFractionCorrect += correct;
            }
            this.setEvaluationValue((double) sumOfFractionCorrect / (double) this.evaluationWindow.size());
            this.evaluationWindow.clear();
        }
    }
}
