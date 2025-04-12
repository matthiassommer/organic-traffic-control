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

package de.dfg.oc.otc.layer1.controller.xcsic.interpolation;

import de.dfg.oc.otc.layer1.controller.AbstractTLCSelector;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants.values.InterpolationValue;
import org.apache.commons.io.FilenameUtils;
import de.dfg.oc.otc.layer1.controller.xcsic.XCSICConstants;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.adjustment.AdjustmentComponent;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.evaluation.EvaluationComponent;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants.*;
import org.apache.log4j.Logger;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Created by Anthony Stein on 28.03.14.
 *
 * Die Implementierung der neuen Interpolations-Komponente IC.
 *
 * @author Anthony Stein (edited by rauhdomi)
 *
 */
public class InterpolationComponent {

    private InterpolationConstants constants;

    // Das XCS-R dessen Struktur um die IC erweitert werden soll.
    private AbstractTLCSelector correspondingXCSIC;

    private Logger log = Logger.getLogger(InterpolationComponent.class);

    // Die Subkomponenten der IC
    private AbstractInterpolationTechnique interpolant;
    private EvaluationComponent evaluationComp;
    private AdjustmentComponent adjustmentComp;

    // Der Pfad für die Datei, die die Evaluationsergebnisse beinhalten soll.
    private String evaluationFilePath;

    public InterpolationComponent() { }

    /**
     * Initialisiert die Subkomponenten der IC.
     */
    public void initInterpolationComponent()
    {
        initInterpolant();
        writeConstants();

        this.evaluationComp = new EvaluationComponent(this.interpolant, this.evaluationFilePath,
                correspondingXCSIC.getAttribute().getMaximalValue());

        this.adjustmentComp = new AdjustmentComponent(this, this.interpolant,
                correspondingXCSIC.getAttribute().getMaximalValue());
    }

    /**
     * Initialisiert die IC-Subkomponente "Interpolant"
     * je nach Konfiguration in der Datei <code>InterpolationConstants</code>
     */
    private void initInterpolant() {
       this.interpolant = InterpolantFactory.newInterpolant(InterpolationConstants.interpolationMethod);
    }

    /**
     * Interpolates for the given situation based on the sites of the chosen Interpolant
     *
     * @param situation The situation to interpolate an action for
     * @return The interpolated action
     */
    public InterpolationValue interpolate(Situation situation)
    {
        try
        {
            return this.interpolant.interpolate(situation);
        }
        catch(InterpolationComponentException e)
        {
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * Löscht eine Stützstelle aus der Menge der Stützstellen der gewählten Interpolante
     * {@see de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants.AbstractInterpolationTechnique}
     *
     * @throws InterpolationComponentException Falls für die gegebene Situation keine zu löschende Stützstelle existiert
     * @param situation Die Situation, die die Stützstelle beschreibt sigma(t)
     */
    public void deleteSite(Situation situation) throws InterpolationComponentException
    {
        this.interpolant.removeSite(situation);
    }

    /**
     * Die Schnittstelle zum XCS damit dieses den aktuellen Zustand des SuOCs
     * übermitteln kann. Reicht die Daten an die entsprechenden Stellen weiter an denen Sie benötigt werden.
     *
     * @param situation Die situation in der die Aktion ausgeführt und der Reward erhalten wurde
     * @param reward Der tatsächlich erhaltenen Reward/Payoff r des SuOCs nach Ausführung der gewählten Aktion a_exec
     * @param executedAction Die tatsächlich ausgeführte Aktion a_exec.
     */
    public void putLastReceivedEnvironmentState(Situation situation, Object executedAction, double reward) {
        this.evaluationComp.putLastReceivedEnvironmentState(situation, executedAction, reward);
        this.adjustmentComp.setReceivedPayoff(reward);
    }

    /**
     * Fordert eine Entscheidung von der Adjustment Component bzw. der zugewiesenen Entscheidungsfunktion an,
     * ob eine neue Stützstelle hinzugefügt werden soll, oder nicht. Anhand der Entscheidung wird die Stützstelle hin-
     * zugefügt
     *
     * @param latestSituation Die Situation für die der Reward erhalten wurde
     * @param reward Der tatsächlich erhaltenen Reward/Payoff r des SuOCs nach Ausführung der gewählten Aktion a_exec,
     *               welcher auf die Korrektheit/Güte der ausgeführten Aktion schließen lässt.
     * @param a_exec Die tatsächlich ausgeführte Aktion a_exec.
     */
    public void addNewSamplingPointIfUseful(Situation latestSituation, Object a_exec, double reward) {

        if(this.adjustmentComp.decideOnAddingNewSamplingPoint(latestSituation,
                a_exec,
                this.interpolant.getInterpolatedValue(), reward))
        {

            try { this.addNewSamplingPoint(latestSituation, new Object[]{a_exec}); }
            catch (InterpolationComponentException e) { log.error(e.getMessage()); }
        }
    }

    /**
     * Fügt der IC-Subkomponente "Interpolant" eine neue Stützstelle p_i hinzu.
     *
     * @param situation Die Koordinaten der neuen Stützstelle p_i
     * @param attributes Die Attribute bzw. der Funktionswert f(p_i) der der neuen Stützstelle zugewiesen werden soll.
     */
    private void addNewSamplingPoint(Situation situation, Object[] attributes) throws InterpolationComponentException{
        this.interpolant.addNewSite(situation, attributes);
    }

    /**
     * Gibt die aktuellen Evaluations-Resultate, die von der IC-Subkomponente
     * "Evaluation Component" berechnet werden, aus.
     */
    public void printPerformanceMeasures() {
        this.evaluationComp.printPerformanceMeasures();
    }

    public void flushAllOutputFiles() {
        this.evaluationComp.flushOutputFile();
    }

    /**
     * Liefert den aktuellen Wert der Interpolation an das anfragende Objekt zurück.
     * Schnittstelle die vom XCS-R direkt aufgerufen werden kann.
     *
     * @return Der interpoliert Wert (hier ein Integer-Wert der die Farbe schwarz oder weiß darstellt)
     */
    public InterpolationValue getInterpolatedValue() {
        return this.interpolant.getInterpolatedValue();
    }

    /**
     * Liefert den aktuellen Trust-Level der IC an das anfragende Objekt zurück.
     * Schnittstelle die vom XCS-R direkt aufgerufen werden kann.
     *
     * @return Der interpoliert Wert (hier ein Integer-Wert der die Farbe schwarz oder weiß darstellt)
     */
    public double getTrustLevel() {
        if(InterpolationConstants.IGNORE_TRUST_LEVEL)
        {
            return 1.0;
        }
        else
        {
            return this.evaluationComp.obtainTrustLevel();
        }
    }

    /**
     * Reinitialisiert die Interpolations-Komponente IC.
     *
     * @param expCounter Das aktuelle Experiment das ausgeführt wird. Für die Dateibenennung benötigt.
     */
    public void resetInterpolationComponent(int expCounter)
    {
        this.initInterpolant();

        this.evaluationComp = new EvaluationComponent(this.interpolant,
                generateOutFilePathForExperiment(expCounter),
                correspondingXCSIC.getAttribute().getMaximalValue());

        this.adjustmentComp = new AdjustmentComponent(this, this.interpolant,
                correspondingXCSIC.getAttribute().getMaximalValue());
    }

    /**
     * Generiert einen entsprechenden String der den Pfad der Ausgabedatei enthält.
     *
     * @param expCounter Das aktuelle Experiment das ausgeführt wird. Für die Dateibenennung benötigt.
     * @return Einen String mit dem generierten Pfad.
     */
    private String generateOutFilePathForExperiment(int expCounter)
    {
        String evaluationFilePathCopy = new String(this.evaluationFilePath);
        String outFilePathExtension = FilenameUtils.getExtension(evaluationFilePathCopy);
        String outFilePathWithoutExtension = FilenameUtils.removeExtension(evaluationFilePathCopy);
        String expFilePath = outFilePathWithoutExtension + "_Exp" + expCounter + "." + outFilePathExtension;
        return expFilePath;
    }

    private void writeConstants()
    {
        PrintWriter pW = null;

        String constantsFileSuffix = "params";
        String evaluationFilePathCopy = new String(this.evaluationFilePath);
        String filePathWithoutExtension = FilenameUtils.removeExtension(evaluationFilePathCopy);
        String filePathExtension = FilenameUtils.getExtension(evaluationFilePathCopy);

        try
        {
            FileOutputStream fOS = new FileOutputStream(filePathWithoutExtension
                    + "_" + constantsFileSuffix
                    + "." + filePathExtension);
            OutputStreamWriter oSW = new OutputStreamWriter(fOS, XCSICConstants.OUT_FILE_CHARSET);
            pW = new PrintWriter(oSW);
        }
        catch(Exception e){ System.out.println("Mistake in create file Writers! " + e); }

        pW.println("iota" + ";" + "tau" + ";" + "p_max" + ";"
                + "t_window" + ";"+ "interpolant" + ";" + "asi" + ";" + "ic_covering" + ";" + "warm_up" + ";"
                + "ignore_trust_level" + ";" + "add_covered_to_pop" + ";" + "idw_expo");
        pW.println(InterpolationConstants.iota_incentive + ";"
                + InterpolationConstants.tau_tax + ";"
                + InterpolationConstants.p_max + ";"
                + InterpolationConstants.t_window + ";"
                + InterpolationConstants.interpolationMethod.name() + ";"
                + InterpolationConstants.ASI + ";"
                + InterpolationConstants.INTERPOLATION_BASED_COVERING + ";"
                + InterpolationConstants.WARM_UP_SAMPLING_POINTS + ";"
                + InterpolationConstants.IGNORE_TRUST_LEVEL + ";"
                + InterpolationConstants.ADD_COVERED_TO_POP + ";" +
                + InterpolationConstants.IDW_EXPO);

        try
        {
            pW.flush();
            pW.close();
        }catch(Exception e){ System.out.println("Mistake in closing the file writer! " + e); }
    }

    public void setEvaluationFilePath(String outFilePath) {
        this.evaluationFilePath = outFilePath;
    }

    public void setCorrespondingXCSIC(AbstractTLCSelector xcsic) {
        this.correspondingXCSIC = xcsic;
    }

    public AbstractInterpolationTechnique getInterpolant() {
        return this.interpolant;
    }
}
