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

package de.dfg.oc.otc.layer1.controller.xcscic.interpolation.components.evaluation;

import de.dfg.oc.otc.layer1.controller.xcscic.XCSCICConstants;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.Situation;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.components.interpolants.values.OTCValue;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.components.interpolants.AbstractInterpolationTechnique;

import java.io.*;
import java.util.HashMap;

/**
 * Created by Anthony Stein on 28.03.14.
 *
 * Die Implementierung der Logik der IC-Subkomponente "Evaluation Component".
 */
public class EvaluationComponent {

    // Eine Aufzählung aller implementierter Evaluationsmetriken (Einträge fungieren gleichzeit als Identifier)
    public enum EvaluationMetrics {
        FRACTION_CORRECT,
        WEBSTER_FIRST_VARIANT
    };

    // Ermöglicht die Kommunikation mit der IC-Subkomponente "Interpolant"
    private AbstractInterpolationTechnique correspondingInterpolant;

    private double maxReward;

    private final String outFilePath;

    // Für die Ausgabe des Trust-Levels in eine Textdatei
    private PrintWriter pW = null;
    private FileOutputStream fOS = null;
    private OutputStreamWriter oSW = null;

    // Enthält alle implementierten Evaluationsmetriken die in Kombination oder einzeln benötigt werden.
    private HashMap<EvaluationMetrics, AbstractEvaluationMetric> evalMetrics = new HashMap<EvaluationMetrics, AbstractEvaluationMetric>();

    /**
     * Konstruktor der <code>EvaluationComponent</code> Klasse
     * @param interpolant Eine Referenz auf die IC-Subkomponente "Interpolant"
     * @param outFilePath Der Pfad an dem die Textdatei zur Auswertung erstellt werden soll
     */
    public EvaluationComponent(AbstractInterpolationTechnique interpolant, String outFilePath, double maxReward)
    {
        this.maxReward = maxReward;
        this.outFilePath = outFilePath;

        this.correspondingInterpolant = interpolant;
        this.evalMetrics.put(EvaluationMetrics.WEBSTER_FIRST_VARIANT, new WebstersEvaluationFirstVariant());

        try
        {
            fOS = new FileOutputStream(outFilePath);
            oSW = new OutputStreamWriter(fOS, XCSCICConstants.OUT_FILE_CHARSET);
            pW = new PrintWriter(oSW);

            pW.println("trust");
        }
        catch (Exception e) { e.printStackTrace(); }

        try
        {
            this.pW.flush();
            this.pW.close();
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Berechnet den Trust-Level T_IC, wenn er benötigt wird. Hier können die unterschiedlichen Evaluationsmetriken nach
     * Bedarf kombiniert werden.
     * @return Der reellwertige Trust-Level; Muss zwischen 0 und 1 liegen!!!
     */
    public double obtainTrustLevel() {
        AbstractEvaluationMetric fracCorrect = this.evalMetrics.get(EvaluationMetrics.WEBSTER_FIRST_VARIANT);
        return fracCorrect.getEvaluationValue();
    }

    /**
     * Nimmt den zuletzt erhaltenen Zustand des betrachteten SuOCs entgegen und speichert benötigte Werte zwischen
     * bzw. leitet diese an die entsprechenden Stellen weiter.
     *
     * @param reward Der tatsächlich vom SuOC erhaltene Reward/Payoff r
     * @param executedAction Die tatsächlich im SuOC ausgeführte Aktion a_exec
     */
    public void putLastReceivedEnvironmentState(Situation situation, Object executedAction, double reward) {
        Object a_exec = executedAction;
        Object a_int = null;

        if((this.correspondingInterpolant.getInterpolatedValue() != null)) {
            OTCValue interpolatedValue = ((OTCValue) this.correspondingInterpolant.getInterpolatedValue());
            if(interpolatedValue.getInterpolatedClassifier() != null) {
                a_int = interpolatedValue.getInterpolatedClassifier().getAction();
            }
        }

        // Aktualisiert die Datenstrukturen zur Berechnung der jeweiligen Evaluationsmetriken.
        updateEvaluationMetrics(situation, a_exec, a_int, reward);
    }

    /**
     * Aktualisiert die Datenstrukturen zur Berechnung der jeweiligen Evaluationsmetriken.
     *
     * @param a_exec Die tatsächlich im SuOC ausgeführte Aktion a_exec
     * @param a_int Die von der IC vorgeschlagene Aktion a_int
     * @param reward Der tatsächlich vom SuOC erhaltene Reward/Payoff r
     */
    private void updateEvaluationMetrics(Situation situation, Object a_exec, Object a_int, double reward) {
        AbstractEvaluationMetric evalMetric = this.evalMetrics.get(EvaluationMetrics.WEBSTER_FIRST_VARIANT);
        evalMetric.evaluate(situation, a_exec, a_int, reward, maxReward);
    }

    /**
     * Schreibt den aktuellen Trust-Level in den entsprechenden Datei-Stream und
     * gibt diesen außerdem auf der Debug-Konsole aus.
     */
    public void printPerformanceMeasures()
    {
        try
        {
            fOS = new FileOutputStream(outFilePath, true);
            oSW = new OutputStreamWriter(fOS, XCSCICConstants.OUT_FILE_CHARSET);
            pW = new PrintWriter(oSW);

            this.pW.println(obtainTrustLevel());
        }
        catch (Exception e) { e.printStackTrace(); }

        try
        {
            this.pW.flush();
            this.pW.close();
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Schreibt die gepufferten Daten auf die Festplatte und schließt die Streams.
     */
    public void flushOutputFile() {
        try {
            this.pW.flush();
            this.pW.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
