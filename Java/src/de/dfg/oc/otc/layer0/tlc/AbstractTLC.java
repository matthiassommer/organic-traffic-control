package de.dfg.oc.otc.layer0.tlc;

import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.Phase;
import de.dfg.oc.otc.manager.aimsun.SignalGroup;

import java.io.Serializable;
import java.util.List;

/**
 * Die abstrakte Basisklasse für Traffic Light Controller.
 *
 * @author rochner
 */
public abstract class AbstractTLC implements Serializable {
    protected int currentPhase;
    protected final AimsunJunction junction;
    /**
     * Anzahl der Phasen, die dieser TLC ber�cksichtigt.
     */
    protected int numPhases;
    /**
     * Externe Ids der Phasen.
     */
    protected List<Integer> phaseIds;
    /**
     * List of phases.
     */
    protected TrafficLightControllerPhase[] phases;
    private float timeOfLastPhaseChange;

    /**
     * Erzeugt einen TrafficLightController, der alle in der Junction
     * vorhandenen Phasen abdeckt.
     *
     * @param maxgreens Array der maximalen Freigabezeiten f�r alle Phasen.
     *                  Reihenfolge entspricht der Reihenbfolge der Phasen im
     *                  zugeh�rigen Junction-Objekt, wie sie von
     *                  AimsunJunction.getPhaseIds() zur�ckgegeben wird.
     * @param junction  Die Junction, f�r die der TLC erzeugt wird.
     * @throws TLCException wenn die Junction nicht existiert, zu wenig Phasen hat oder
     *                         die Anzahl der Phasen nicht mit der Anzahl der �bergebenen
     *                         Phasendauern zusammenpasst.
     */
    protected AbstractTLC(final float[] maxgreens, final AimsunJunction junction) throws TLCException {
        this.junction = junction;

        if (this.junction.getNumPhases() < 2) {
            throw new TLCException("Junction invalid (Anzahl Phasen < 2)");
        }

        this.numPhases = this.junction.getNumPhases();
        if (numPhases != maxgreens.length) {
            throw new TLCException("Anzahl Phasen stimmt nicht mit Anzahl übergebener Phasendauern überein!");
        }

        this.phases = new TrafficLightControllerPhase[this.numPhases];
        this.phaseIds = this.junction.getPhaseIds();
        for (int i = 0; i < numPhases; i++) {
            phases[i] = new TrafficLightControllerPhase(maxgreens[i], this.junction.getPhaseById(phaseIds.get(i)));
        }
    }

    /**
     * Erzeugt einen TrafficLightController f�r alle �bergebenen Phasen.
     *
     * @param maxGreens Array der maximalen Freigabezeiten f�r alle Phasen.
     *                  Reihenfolge entspricht der Reihenbfolge der Phasen im
     *                  zugeh�rigen Junction-Objekt, wie sie von
     *                  AimsunJunction.getPhaseIds() zur�ckgegeben wird.
     * @param junction  Die Junction, f�r die der TLC erzeugt wird.
     * @param phaseIds  Ids der Phasen, die von diesem TLC ber�cksichtigt werden
     *                  sollen.
     * @throws TLCException wenn die Junction nicht existiert, zu wenig Phasen hat, die
     *                         Anzahl der Phasen nicht mit der Anzahl der �bergebenen
     *                         Phasendauern zusammenpasst oder eine �bergebene Phasen-Id
     *                         nicht existiert.
     */
    protected AbstractTLC(final float[] maxGreens, final AimsunJunction junction, final List<Integer> phaseIds)
            throws TLCException {
        this.junction = junction;

        if (this.junction.getNumPhases() < 2 || phaseIds.size() > this.junction.getNumPhases()) {
            throw new TLCException("Junction invalid (Anzahl Phasen falsch)");
        }

        this.numPhases = phaseIds.size();
        if (this.numPhases != maxGreens.length) {
            throw new TLCException("Anzahl Phasen stimmt nicht mit Anzahl der übergebenen Phasendauern überein");
        }

        this.phaseIds = phaseIds;
        this.phases = new TrafficLightControllerPhase[this.numPhases];
        for (int i = 0; i < this.numPhases; i++) {
            this.phases[i] = new TrafficLightControllerPhase(maxGreens[i], this.junction.getPhaseById(this.phaseIds.get(i)));
        }
    }

    /**
     * Sucht nach einer Phase, die m�glichst viele Signalgruppen mit der als
     * Parameter �bergebenen Phase teilt.
     *
     * @param phase Phase, zu der eine �hnliche Phase im Bestand des TLC gesucht
     *              werden soll.
     * @return Am besten passende Phase oder die erste Phase des Controllers,
     * wenn es keine Phase gibt, die mindestens eine Signalgruppe mit
     * der angegebenen Phase teilt.
     */
    public final int findSimilarPhase(final Phase phase) {
        final List<SignalGroup> signalGroups = phase.getSignalGroups();
        int matchingSigGroups = 0;
        int bestPhase = phases[0].getAimsunPhase().getId();

        for (TrafficLightControllerPhase phase1 : phases) {
            int sigGroupsCounter = 0;
            final List<SignalGroup> signalGroupsCompare = phase1.getAimsunPhase().getSignalGroups();

            for (SignalGroup signalgroup : signalGroups) {
                for (SignalGroup signalGroup : signalGroupsCompare) {
                    if (signalgroup == signalGroup) {
                        sigGroupsCounter++;
                    }
                }
            }

            if (sigGroupsCounter > matchingSigGroups) {
                matchingSigGroups = sigGroupsCounter;
                bestPhase = phase1.getAimsunPhase().getId();
            }
        }
        return bestPhase;
    }

    /**
     * Liefert die (externe) Id der Phase, die momentan aktiv ist.
     *
     * @return Aimsuns Phasen-Id.
     */
    public final int getCurrentPhaseID() {
        return phaseIds.get(currentPhase);
    }

    /**
     * Liefert die (ungef�hre) Umlaufzeit. Diese kann beispielsweise verwendet
     * werden, um ein sinnvolles Zeitintervall f�r die Situationserkennung oder
     * Bewertung an einem Knoten festzulegen (minimum zwei- bis dreifache
     * Umlaufzeit).
     *
     * @return Umlaufzeit in Sekunden
     */
    public final int getCycleTime() {
        float cycleTime = 0;

        for (int i = 0; i < numPhases; i++) {
            cycleTime += phases[i].getMaxGreenTime();
        }
        return new Float(cycleTime).intValue();
    }

    public abstract TrafficLightControllerParameters getParameters();

    public final float getTimeOfLastChange() {
        return timeOfLastPhaseChange;
    }

    /**
     * Initialisiert den Traffic Light Controller. Die aktuelle Phase wird
     * entsprechend Parameter gesetzt, timeLastChange auf 0.
     *
     * @param phaseID Die externe Id der Phase, die als aktiv gesetzt werden soll.
     * @throws TLCException wenn die angegebene Phase ung�ltig ist.
     */
    public final void init(final int phaseID) throws TLCException {
        this.timeOfLastPhaseChange = 0;

        for (int i = 0; i < numPhases; i++) {
            if (phaseIds.get(i) == phaseID) {
                currentPhase = i;
                return;
            }
        }

        this.currentPhase = 0;
        throw new TLCException("Phase id " + phaseID + "invalid.");
    }

    /**
     * Initialisiert den Traffic Light Controller. Die aktuelle Phase wird
     * entsprechend Parameter gesetzt, timeLastChange auf den angegebenen Wert.
     *
     * @param phase      Die externe Id der Phase, die als aktiv gesetzt werden soll.
     * @param lastChange Die Dauer, die diese Phase schon aktiv gewesen ist.
     * @throws TLCException wenn die angegebene Phase ung�ltig ist.
     */
    public final void init(final int phase, final float lastChange) throws TLCException {
        init(phase);
        this.timeOfLastPhaseChange = lastChange;
    }

    /**
     * Initialisiert den Traffic Light Controller. Die aktuelle Phase wird auf 0
     * gesetzt (interner Index der Phase, nicht externe Id), timeLastChange auf
     * 0.
     */
    public final void reset() {
        this.timeOfLastPhaseChange = 0;
        this.currentPhase = 0;
    }

    protected final void setTimeOfLastPhaseChange(final float timeOfLastPhaseChange) {
        this.timeOfLastPhaseChange = timeOfLastPhaseChange;
    }

    /**
     * Berechne den n�chsten TLC-Schritt f�r alle Knoten
     */
    public abstract void step(float time) throws TLCException;

    public final String toString() {
        return getParameters().toString();
    }
}
