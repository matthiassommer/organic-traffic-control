package de.dfg.oc.otc.layer2;

import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;

import java.io.Serializable;

/**
 * Die Klasse kapselt das Ergebnis eines Optimierungslaufs. Ein
 * {@code OptResult} wird auf Ebene 2 erzeugt und per RMI an Ebene 1
 * zur�ckgeliefert.
 *
 * @author hpr
 */
@SuppressWarnings("serial")
public class OptimisationResult implements Serializable {
    /**
     * ID des Knotens, für den die Optimierung durchgeführt wurde.
     */
    private final int nodeID;

    /**
     * Aktionsteil der Regel für das LCS.
     */
    private final TrafficLightControllerParameters parameters;

    /**
     * Die Verkehrssituation, die zum Zeitpunkt {@code time} vorlag.
     */
    private final float[] situation;

    /**
     * Simulationszeitpunkt, für den die Optimierung durchgeführt wurde.
     */
    private final float time;

    /**
     * Bewertung der Regel.
     */
    private final float ratingOfRule;

    /**
     * Der Konstruktor erzeugt ein neues Optimierungsergebnis, das auf Ebene 2
     * erzeugt und per RMI an Ebene 1 transportiert wird. Das Ergebnis besteht
     * aus der ID des betrachteten Knotens, dem betrachteten
     * Simulationszeitpunkt, der zu diesem Zeitpunkt vorliegenden
     * Verkehrssituation, den erzeugten Steuerungsparametern und einer Bewertung
     * der Steuerung durch Ebene 2.
     *
     * @param nodeID    die ID des betrachteten Knotens
     * @param time      der betrachtete Simulationszeitpunkt
     * @param situation die zum Simulationszeitpunkt vorliegende Verkehrssituation
     * @param tlcParams die erzeugten Steuerungsparameter
     * @param value     die Bewertung der Steuerung durch Ebene 2
     */
    public OptimisationResult(final int nodeID, final float time, final float[] situation,
                              final TrafficLightControllerParameters tlcParams, final float value) {
        this.nodeID = nodeID;
        this.time = time;
        this.situation = situation;
        this.parameters = tlcParams;
        this.ratingOfRule = value;
    }

    /**
     * Gibt die ID des Knotens zur�ck, f�r den die Optimierung durchgef�hrt
     * wurde.
     *
     * @return ID des Knotens
     */
    public final int getNodeID() {
        return nodeID;
    }

    /**
     * Die Methode gibt die bei der Optimierung gefundenen Steuerungsparameter
     * zur�ck. (Aktionsteil der LCS-Regel)
     *
     * @return die bei der Optimierung gefundenen Steuerungsparameter
     */
    public final TrafficLightControllerParameters getParameters() {
        return parameters;
    }

    /**
     * Gibt die Verkehrssituation zur�ck, die der Optimierung zugrunde lag.
     *
     * @return die Verkehrssituation, die der Optimierung zugrunde lag.
     */
    public final float[] getSituation() {
        return situation;
    }

    /**
     * Die Methode gibt die Bewertung der Steuerungsparameter f�r die
     * zugrundeliegende Situation zur�ck. (Bewertung der LCS-Regel).
     *
     * @return die Bewertung der Steuerungsparameter f�r die zugrundeliegende
     * Situation
     */
    public final float getValue() {
        return ratingOfRule;
    }

    @Override
    public final String toString() {
        String situationString = "";
        if (situation != null) {
            for (float aSituation : situation) {
                situationString += " " + aSituation;
            }
        }

        return "[OptimisationResult] NodeID " + nodeID + ", time " + time + ", situation"
                + situationString + ", TLCParameters " + parameters + ", rating " + ratingOfRule;
    }
}
