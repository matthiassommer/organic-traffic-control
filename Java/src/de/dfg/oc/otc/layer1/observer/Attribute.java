package de.dfg.oc.otc.layer1.observer;

import org.apache.log4j.Logger;

/**
 * Attribute sind hier Werte, die f�r die Evaluation ber�cksichtigt werden, sie
 * dienen der Qualit�tsmessung. Folgende Werte sind implementiert:
 * <ul>
 * <li>LOS: Level of Service</li>
 * <li>QUEUELENGTH: Durchschnittliche L�nge der Warteschlange</li>
 * <li>UTILISATION: Auslastungsgrad der Kreuzung</li>
 * <li>AVSTOPS: Durchschnittliche Anzahl der Halte pro Fahrzeug</li>
 * <li>MAXSTOPS: Anzahl der Stops pro Fahrzeug an der Turning mit dem gr�ssten
 * Wert.</li>
 * </ul>
 */
public enum Attribute {
    AVSTOPS(true, 10), LOS(true, 200), MAXSTOPS(true, 10), QUEUELENGTH(true, 20), UTILISATION(true, 200, 2400);

    /**
     * Gibt an, ob gr��ere Werte besser sind (im Hinblick auf die Bewertung im
     * LCS; false) oder kleinere Werte (true).
     */
    private final boolean inverted;
    /**
     * Gr��ter erwarteter Wert f�r dieses Attribut (f�r Umrechnung der Bewertung
     * f�r das LCS).
     */
    private float maximalValue;
    /**
     * Wird genutzt, um f�r das Attribut zus�tzlich n�tige Parameter zu
     * speichern; beispielsweise die Kapazit�t beim Auslastungsgrad. Nicht f�r
     * alle ben�tigt.
     */
    private float parameter;

    Attribute(final boolean inverted, final float maximalValue) {
        this.inverted = inverted;
        this.maximalValue = maximalValue;
    }

    Attribute(final boolean inverted, final float maximalValue, final float newParameter) {
        this.inverted = inverted;
        this.maximalValue = maximalValue;
        this.parameter = newParameter;
    }

    public float getMaximalValue() {
        return maximalValue;
    }

    public void setMaximalValue(final float maximalValue) {
        this.maximalValue = maximalValue;
    }

    public float getParameter() {
        return parameter;
    }

    public void setParameter(final float parameter) {
        this.parameter = parameter;
    }

    /**
     * Gibt zur�ck, ob bei diesem Attribut gr��ere Werte bei der Bewertung von
     * Classifiern besser sind (false) oder kleinere Werte besser sind (true).
     *
     * @return inverted
     */
    public boolean isInverted() {
        return inverted;
    }

    /**
     * Bildet den Wert des Attributs so ab, dass das LCS maximieren kann.
     * Ber�cksichtigt den maximal erwarteten Wert.
     *
     * @param value Urspr�nglicher Wert
     * @return Abgebildeter Wert
     */
    public float mapEvaluationForLCS(final float value) {
        float mappedValue = maximalValue - value;
        if (mappedValue < 0) {
            mappedValue = 0;
        }
        return mappedValue;
    }

    /**
     * Bildet die Bewertung des LCS wieder auf den urspr�nglichen Wertebereich
     * ab (sofern der durch maximalValue beschr�nkte Wertebereich nicht verlassen
     * wurde).
     *
     * @param value Urspr�nglicher Wert
     * @return Abgebildeter Wert
     */
    public float mapPredictionToEvaluation(final float value) {
        return maximalValue - value;
    }
}
