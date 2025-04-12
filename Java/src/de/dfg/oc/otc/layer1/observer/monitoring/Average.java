package de.dfg.oc.otc.layer1.observer.monitoring;

/**
 * Speichert einen Durchschnittswert mit allen zur Zuordnung n�tigen
 * Attributen.
 *
 * @author rochner
 */
public class Average {
    /**
     * Feature, f�r das der Durchschnitt berechnet wurde.
     */
    private final int feature;
    /**
     * Number of valid entries used for average calculation.
     */
    private final int numberValidEntries;
    private final boolean timeBased;
    /**
     * Time when average value was calculated.
     */
    private final float timeCreated;
    private final float timeInterval;
    /**
     * Average value.
     */
    private final float value;

    /**
     * Erzeugt ein neues Objekt zur Speicherung eines Durchschnittswerts.
     *
     * @param timeCreated  Zeitpunkt, zu dem der Durchschnittswert berechnet wurde.
     *                     Bestimmt �ber den Zeitstempel des letzten Eintrags im
     *                     Storage.
     * @param feature      Feature, f�r das der Wert berechnet wurde. Beim Zugriff
     *                     muss die interne Map f�r inaktive Features genutzt werden
     *                     (getMappedFeature).
     * @param numEntries   Anzahl g�ltiger Messwerte, die bei der Berechnung
     *                     ber�cksichtigt wurden.
     * @param timeInterval Intervall, �ber das der Wert berechnet wurde (Zeitstempel
     *                     des ersten minus des letzten g�ltigen genutzten Werts).
     * @param value        Durchschnittswert.
     */
    public Average(final float timeCreated, final int feature, final int numEntries, final float timeInterval,
                   final float value, final boolean timeBased) {
        this.timeCreated = timeCreated;
        this.feature = feature;
        this.numberValidEntries = numEntries;
        this.timeInterval = timeInterval;
        this.value = value;
        this.timeBased = timeBased;
    }

    public final Average clone() {
        return new Average(this.timeCreated, this.feature, this.numberValidEntries, this.timeInterval, this.value,
                this.timeBased);
    }

    /**
     * Gibt die Anzahl g�ltiger Messwerte zur�ck, die bei der Berechnung
     * ber�cksichtigt wurden.
     *
     * @return the numEntries
     */
    final int getNumEntries() {
        return numberValidEntries;
    }

    /**
     * Gibt den Zeitpunkt zur�ck, zu dem der Durchschnittswert berechnet
     * wurde. Bestimmt �ber den Zeitstempel des letzten Eintrags im Storage.
     *
     * @return Zeitpunkt
     */
    final float getTimeCreated() {
        return timeCreated;
    }

    /**
     * Gibt das Intervall zur�ck, �ber das der Wert berechnet wurde
     * (Zeitstempel des ersten minus des letzten g�ltigen genutzten Werts).
     *
     * @return the timeInterval
     */
    final float getTimeInterval() {
        return timeInterval;
    }

    /**
     * Gibt den Durchschnittswert zur�ck.
     *
     * @return the value
     */
    final float getValue() {
        return value;
    }

    /**
     * Gibt zur�ck, ob dieser Durchschnitt sich auf das Zeitintrvall, in dem
     * die enthaltenen Werte liegen, oder auf die Anzahl der
     * ber�cksichtigten Werte bezieht.
     *
     * @return true, wenn zeitbasiert, false, wenn anzahlbasiert.
     */
    final boolean isTimeBased() {
        return timeBased;
    }
}
