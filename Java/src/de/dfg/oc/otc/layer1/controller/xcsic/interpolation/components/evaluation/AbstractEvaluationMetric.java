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

/**
 * Created by Anthony Stein on 30.05.2014.
 *
 * Eine abstrakte Klasse für die Evaluationsmetriken,
 * die u.a. zur Berechnung des Trust-Levels T_IC benötigt werden.
 *
 * Jede implementierte Evaluationsmetrik sollte von dieser Klasse erben, damit sie in Kombination
 * mit anderen Instanzen innerhalb der <code>EvaluationComponent</code> verwendet werden kann.
 */
public abstract class AbstractEvaluationMetric {

    // Ein numerischer Wert, der für jede Instanz einer konkreten Metrik gesondert interpretiert werden muss
    protected double evaluationValue = 0.0;
    // Ein Identifier für die HashMap die alle eingesetzten Evaluationsmetriken enthält.
    protected EvaluationComponent.EvaluationMetrics id;
    // Ein aussagekräftiger String, der die Entscheidungsregeln beschreibt.
    protected String description;

    /**
     * Diese Methode muss von jeder Kind-Klasse überschrieben werden und mit den eigentlichen
     * Berechnungen für die Evaluation befüllt werden.
     *
     * @param situation Die Situation unter der die Aktion ausgeführt wurde
     * @param a_exec Die tatsächlich im SuOC ausgeführte Aktion a_exec
     * @param a_int Die von der Interpolante vorgeschlagene Aktion a_int
     * @param reward Der vom SuOC tatsächlich erhaltene Reward/Payoff r
     * @param maxReward Der maxmimal vom SuOC erhaltbare Reward/Payoff
     */
    public abstract void evaluate(Situation situation, Object a_exec, Object a_int, double reward, double maxReward);

    public double getEvaluationValue() {
        return this.evaluationValue;
    }

    public void setEvaluationValue(double evalValue) {
        this.evaluationValue = evalValue;
    }

    public String getDescription() {
        return description;
    }
}
