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

package de.dfg.oc.otc.layer1.controller.xcscic.interpolation.components.interpolants;

import de.dfg.oc.otc.layer1.controller.xcscic.ClassifierSet;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.InterpolationComponentException;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.Situation;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.components.interpolants.values.InterpolationValue;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by Anthony Stein on 28.03.14.
 *
 * Diese abstrakte Klasse stellt eine Schnittstelle für eine einheitliche Verwendung
 * unterschiedlicher Interpolationstechniken zur Verfügung.
 * Dadurch kann die Interpolationstechnik (Subkomponente der IC "Interpolant") ohne Weiteres ausgetauscht werden.
 */
public abstract class AbstractInterpolationTechnique
{
    // Der interpolierte Funktionswert f(x_q)
    protected InterpolationValue interpolatedValue;
    // Der angefragte unbekannte Punkt x_q bzw. dessen Koordinaten
    protected Situation lastQueryPoint;

    /**
     * Führt eine Interpolation durch. Muss für jede Interpolationstechnik separat implementiert werden!
     *
     * @param situation Der angefragte, zu interpolierende Punkt im Problemraum x_q
     * @param population Die Klassifkatorpopulation, in der interpoliert werden soll
     */
    public abstract InterpolationValue interpolate(Situation situation, ClassifierSet population)
            throws InterpolationComponentException;

    /**
     * Liefert den (zuletzt) interpolierten Funktionswert f(x_q) an die anfragende Instanz zurück.
     *
     * @return Der interpolierte Funktionswert in der Object-Repräsentation (entsprechender Cast notwendig)
     */
    public InterpolationValue getInterpolatedValue() {
        return this.interpolatedValue;
    }

    /**
     * Fügt der Interpolante eine neue sichere Stützstelle p_i hinzu.
     * @param situation Die Koordinaten der neuen sicheren Stützstelle p_i
     * @param attributes Die Attribute, die der Stützstelle zugewiesen werden sollen
     *                   (i.d.R. numerischer Wert, aber auch alles andere z.B. Farbe möglich)
     */
    public abstract void addNewSite(Situation situation, Object[] attributes) throws InterpolationComponentException;

    /**
     * Entfernt eine Stützstelle von der Interpolante
     *
     * @param siteToRemove Die zu entfernende Stützstelle
     */
    public abstract void removeSite(Object siteToRemove) throws InterpolationComponentException;

    /**
     * Liefert den (zuletzt) angefragten unbekannten Punkt x_q an die anfragende Instanz zurück.
     *
     * @return Der angefragte unsichere Punkt x_q bzw. dessen Koordinaten in Form eines <code>double[]</code>
     */
    public Situation getLastQueryPoint() {return lastQueryPoint;}
}
