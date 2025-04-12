package de.dfg.oc.otc.manager.aimsun.detectors;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages CounterLoops of a section.
 */
public class CounterLoopContainer {
    /**
     * Die verwalteten CounterLoops für diese Section.
     */
    private final Map<Integer, CounterLoop> counterLoops;

    /**
     * Abbiegewahrscheinlichkeiten der zugeh�rigen Turnings.
     */
    private final Map<Integer, Float> turningProbabilities;

    public CounterLoopContainer() {
        this.counterLoops = new HashMap<>();
        this.turningProbabilities = new HashMap<>();
    }

    /**
     * Methode fügt dem Container eine neue CounterLoop hinzu.
     *
     * @param turningID
     * @param loop
     */
    public final void addCounterLoop(final int turningID, final CounterLoop loop) {
        this.counterLoops.put(turningID, loop);
    }

    /**
     * Methode wird genutzt, um die Abbiegewahrscheinlichkeiten anzupassen.
     *
     * @param flows
     */
    private void adjustTurnProbabilities(final Map<Integer, Float> flows) {
        flows.entrySet().stream().filter(entry -> this.turningProbabilities.containsKey(entry.getKey())).forEach(entry -> this.turningProbabilities.put(entry.getKey(), entry.getValue()));
    }

    /**
     * Methode liefert die aktuelle Schlange fuer ein Turning.
     *
     * @param turningID
     * @return flow value
     */
    public final float getCounterLoopValueForTurning(final int turningID) {
        if (this.counterLoops.containsKey(turningID)) {
            return this.counterLoops.get(turningID).getNumberOfCarsInLoop();
        }
        return Float.NaN;
    }

    /**
     * Methode schaetzt die Verteilung der zugeordneten Zaehlschleifen neu ab.
     * <p>
     * Aufruf nur am Beginn eines jeden Umlaufs, sonst werden falsche
     * Abschaetzungen erzeugt!
     */
    public final void updateCounters(final Map<Integer, Float> flows) {
        if (flows != null) {
            adjustTurnProbabilities(flows);
        }
    }
}
