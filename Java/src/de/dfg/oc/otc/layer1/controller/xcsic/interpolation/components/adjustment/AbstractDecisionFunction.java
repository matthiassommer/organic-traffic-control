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
import de.dfg.oc.otc.layer1.observer.Attribute;

/**
 * Created by Anthony Stein on 30.05.2014.
 *
 * Eine abstrakte Klasse für die Entscheidungsfunktion,
 * ob eine neue Stützstelle hinzugefügt werden soll oder nicht.
 *
 * Jede implementierte Entscheidungsfunktion sollte von dieser Klasse erben, damit sie in Kombination
 * mit anderen Instanzen innerhalb der <code>AdjustmentComponent</code> verwendet werden kann.
 */
public abstract class AbstractDecisionFunction {

    // Ein Identifier für die HashMap die alle eingesetzten Entscheidungsfunktionen enthält.
    protected AdjustmentComponent.DecisionFunctions id;
    // Ein aussagekräftiger String, der die Entscheidungsregeln beschreibt.
    protected String description;

    protected InterpolationComponent interpolationComponent;

    protected AbstractDecisionFunction (InterpolationComponent interpolationComponent)
    {
        this.interpolationComponent = interpolationComponent;
    }

    /**
     * Diese Methode muss von jeder Kind-Klasse überschrieben werden und mit den eigentlichen
     * Entscheidungsregeln befüllt werden.
     *
     * @param situation Die Situation unter der der Reward erzielt wurde
     * @param reward Der tatsächliche Reward vom SuOC
     * @param a_exec Die tatsächlich ausgeführte Aktion a_exec
     * @param a_int Die von der Interpolante vorgeschlagene Aktion a_int
     * @param maxReward Der maximale reward, der erhalten werden kann
     * @return Ein boolscher Wert, der aussagt, ob eine neue Stützstelle hinzugefügt werden soll oder nicht.
     */
    public abstract boolean decide(Situation situation, Object a_exec, Object a_int, double reward, double maxReward);

    public String getDescription() {
        return description;
    }

}
