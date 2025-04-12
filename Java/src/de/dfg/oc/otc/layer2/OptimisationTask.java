package de.dfg.oc.otc.layer2;

import de.dfg.oc.otc.layer1.observer.Attribute;
import de.dfg.oc.otc.layer2.ea.EAConfig;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.AimsunNetwork;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Die Klasse kapselt alle f�r eine Optimierungsaufgabe notwendigen Daten. Sie
 * wird von Ebene 1 erzeugt und per {@code RMI} an Ebene 2 �bergeben.
 *
 * @author hpr
 */
@SuppressWarnings("serial")
public class OptimisationTask implements Serializable {
    private Attribute optimisationCriterion;

    /**
     * Umlaufzeit, die als Nebenbedingung vom EA ber�cksichtigt werden soll
     * (Verwende {@code 0}, falls keine Bedingung an die Umlaufzeit gestellt werden soll.).
     */
    private int cycleTimeConstraint;

    /**
     * Konfigurationsobjekt f�r den EA.
     */
    private EAConfig eaConfiguration;

    /**
     * das zu simulierende Netz (der Inhalt der {@code .ang}-Datei)
     */
    /* http://www.javaworld.com/javaworld/jw-05-1997/javadev/RMISendFile.html */
    private byte[] fileData;

    /**
     * ID des zu optimierenden Knotens.
     */
    private int nodeID;

    /**
     * ID der zu simulierenden Replikation.
     */
    private int replicationId;

    /**
     * IDs der Eingangs- und Ausgangssections, auf die sich die Werte in
     * {@code situation} beziehen.
     */
    private int[] sectionIDsForSituation;

    /**
     * Die Verkehrssituation, die zum Zeitpunkt {@code time} vorlag.
     */
    private float[] situation;

    /**
     * Fortlaufende Nummer der Optimierungsaufgabe.
     */
    private int taskID;

    /**
     * Zeitpunkt, f�r den die Optimierung durchgef�hrt werden soll.
     */
    private float timeForOptimisation;

    /**
     * Das Verkehrsaufkommen, das zum Zeitpunkt {@code time} vorlag (Summe
     * der einzelnen {@code situation}-Eintr�ge).
     */
    private float totalDemand;

    /**
     * Datensatz, der die Abbiegebeziehungen am Knoten beschreibt (wird f�r die
     * Wartezeitberechnung nach Webster ben�tigt).
     */
    private Map<String, TurningData> turningData = new HashMap<>();

    /**
     * Der Konstruktor erzeugt eine neue Optimierungsaufgabe f�r Ebene 2. Die
     * Aufgabe beeinhaltet die Netz-Datei, die ID des zu optimierenden
     * Controllers, den Simulationszeitpunkt, die zu diesem Zeitpunkt
     * vorliegende Verkehrssituation und die zu simulierende Replikation.
     * Zus�tzlich ist ein vom EA zu verwendendes Konfigurationsobjekt anzugeben.
     *
     * @param angFile                ein Dateiverweis auf das zu verwendende Netzwerk (kann
     *                               {@code null} sein; ggf. wird die Datei eingelesen und
     *                               kann sp�ter per RMI �bertragen werden)
     * @param nodeID                 die ID des zu optimierenden Knotens
     * @param time                   der Zeitpunkt, f�r den die Optimierung durchgef�hrt werden
     *                               soll
     * @param situation              die zum Simulationszeitpunkt vorliegende Verkehrssituation
     * @param sectionIDsForSituation IDs der Eingangs- und Ausgangssections, auf die sich die Werte
     *                               in {@code _situation} beziehen
     * @param replicationId          die ID der zu simulierenden Replikation
     * @param eaConf                 Konfigurationsobjekt f�r den EA (falls {@code null} wird
     *                               die Standardkonfiguration verwendet)
     * @param attribute              das zu verwendende Optimierungskriterium
     * @param cycleTimeConstraint    Umlaufzeit, die als Nebenbedingung vom EA ber�cksichtigt
     *                               werden soll (Verwende {@code 0}, falls keine Bedingung an
     *                               die Umlaufzeit gestellt werden soll.)
     * @throws IOException
     */
    public OptimisationTask(final File angFile, final int nodeID, final float time, final float[] situation, final int[] sectionIDsForSituation,
                            final int replicationId, final EAConfig eaConf, final Attribute attribute, final int cycleTimeConstraint) throws IOException {
        // Ggf. Modell laden
        if (angFile != null) {
            this.fileData = readIn(angFile);
        }

        this.nodeID = nodeID;
        this.timeForOptimisation = time;
        this.situation = situation;
        this.sectionIDsForSituation = sectionIDsForSituation;
        this.replicationId = replicationId;

        // EA-Config
        if (eaConf != null) {
            eaConfiguration = eaConf;
        } else {
            eaConfiguration = new EAConfig();
        }

        this.optimisationCriterion = attribute;
        this.cycleTimeConstraint = cycleTimeConstraint;

        final AimsunNetwork network = OTCManager.getInstance().getNetwork();
        if (network != null) {
            this.turningData = network.getNode(nodeID).getJunction().exportTurningData();
        }

        calculateTotalDemand();
    }

    /**
     * Berechnet das Verkehrsaufkommen als Summe der einzelnen Ströme.
     */
    private void calculateTotalDemand() {
        float totalFlow = 0;

        for (TurningData data : turningData.values()) {
            totalFlow += data.getFlow();
        }

        this.totalDemand = totalFlow;
    }

    /**
     * Gibt das Optimierungskriterium zurück.
     */
    public final Attribute getAttribute() {
        return optimisationCriterion;
    }

    /**
     * Gibt die als Nebenbedingung zu verwendende Umlaufzeit zur�ck.
     *
     * @return die als Nebenbedingung zu verwendende Umlaufzeit oder
     * {@code 0}, falls keine Bedingung an die Umlaufzeit gestellt
     * wird
     */
    public final int getCycleTimeConstraint() {
        return cycleTimeConstraint;
    }

    /**
     * Gibt die eingestellte EA-Konfiguration zur�ck.
     */
    public final EAConfig getEAConfig() {
        return eaConfiguration;
    }

    /**
     * Die Methode gibt die ID des zu optimierenden Knotens zur�ck.
     */
    public final int getNodeID() {
        return nodeID;
    }

    /**
     * Die Methode gibt die ID der Replikation zur�ck.
     */
    public final int getReplicationId() {
        return replicationId;
    }

    /**
     * Gibt die IDs der Eingangs- und Ausgangssections, auf die sich die Werte
     * in {@code situation} beziehen, zur�ck.
     *
     * @return IDs der Eingangs- und Ausgangssections, auf die sich die Werte in
     * {@code situation} beziehen
     */
    public final int[] getSectionIDsForSituation() {
        return sectionIDsForSituation;
    }

    /**
     * Gibt die Verkehrssituation zur�ck, die der Optimierung zugrunde lag.
     */
    public final float[] getSituation() {
        return situation;
    }

    /**
     * Gibt die laufende Nummer der Optimierungsaufgabe zur�ck.
     */
    public final int getTaskID() {
        return taskID;
    }

    /**
     * Die Methode gibt den Simulationszeitpunkt zur�ck.
     */
    public final float getTime() {
        return timeForOptimisation;
    }

    public final float getTotalDemand() {
        return totalDemand;
    }

    public final Map<String, TurningData> getTurningData() {
        return turningData;
    }

    /**
     * Gibt {@code true} zur�ck, falls der OptTask ein AIMSUN-Modell
     * mitliefert ({@code false} sonst).
     */
    public final boolean hasFileData() {
        return this.fileData != null;
    }

    /**
     * Die Methode liest die �bergebene Datei aus und speichert sie in dem
     * zur�ckgegebenen {@code byte[]}.
     *
     * @param file die auszulesende Datei
     * @return ein {@code byte[]} mit dem Dateiinhalt
     */
    private byte[] readIn(final File file) throws IOException {
        final byte[] fileInData = new byte[(int) file.length()];
        final FileInputStream fis = new FileInputStream(file);
        fis.read(fileInData);
        fis.close();
        return fileInData;
    }

    /**
     * Setzt die laufende Nummer der Optimierungsaufgabe.
     */
    public final void setTaskID(final int taskID) {
        this.taskID = taskID;
    }

    public final void setTurningData(final Map<String, TurningData> turningData) {
        this.turningData = turningData;
        calculateTotalDemand();
    }

    @Override
    public final String toString() {
        return "TASK " + taskID + " - NODE_ID " + nodeID + ", TIME " + timeForOptimisation + ", SITUATION " + Arrays.toString(situation);
    }

    /**
     * Die Methode speichert den Inhalt von {@code fileData} in der
     * angegebenen Datei. Die Datei wird neu angelegt.
     *
     * @param location Dateiname der anzulegenden Datei (inkl. Verzeichnis)
     * @throws IOException
     */
    public final void writeTo(final File location) throws IOException {
        final FileOutputStream fos = new FileOutputStream(location);
        fos.write(this.fileData);
        fos.flush();
        fos.close();
    }
}
