package de.dfg.oc.otc.layer1.observer.monitoring;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.tools.LimitedQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * Speichert Messwerte, z. B. von Detektoren oder statistischen Funktionen, und
 * wertet diese aus (Durchschnitt). H�lt die jeweils zuletzt berechneten
 * Durchschnittswerte in einem Zwischenspeicher vor, um bei wiederholtem Zugriff
 * den Berechnungsaufwand zu reduzieren.
 *
 * @author rochner
 */
public abstract class DataStorage extends Observable {
    /**
     * Anzahl von g�ltigen Me�werten, die mindestens in einen Durchschnitt
     * eingehen m�ssen, damit dieser zur�ckgegeben wird. Sonst NaN oder
     * vergleichbar.
     */
    private static int MIN_NUM_ENTRIES_AVERAGE;
    /**
     * Gr��e des Intervalls, in dem am Anfang und Ende einer
     * Durchschnittsberechnung f�r fahrzeugbasierte Gr��en das Gewicht einzelner
     * Me�werte linear reduziert wird, um Oszillationen zu d�mpfen.
     */
    private static float smoothInterval;
    /**
     * Liste mit allen relevanten DataEntries. Relevant ist ein Eintrag,
     * wenn er g�ltige Daten enth�lt und sich mindestens eine Komponente dieser
     * Daten vom vorhergehenden Eintrag unterscheidet.
     */
    final LimitedQueue<DataEntry> entries;
    /**
     * Wird verwendet, um bei nicht vorhandenen Features (abgeschaltet �ber
     * disableFeature) das Mapping anzupassen.
     */
    final int[] featureMap;
    final int numFeatures;
    /**
     * Speichert für jedes "Feature" den zuletzt berechneten Durchschnittswert.
     */
    private final Average[] averages;
    private final MeasurementCapabilities capabilities;
    /**
     * Letzter gültiger Eintrag (unabhängig davon, ob eine Veränderung
     * eingetreten ist).
     */
    DataEntry lastEntry;
    int numDisabledFeatures;

    /**
     * Erzeugt ein neues Objekt.
     *
     * @param capabilities Anzahl der maximal zu berücksichtigenden Features. Mapping
     *                     erfolgt in den erweiternden Klassen.
     */
    DataStorage(final MeasurementCapabilities capabilities) {
        this.entries = new LimitedQueue<>(500);
        initialiseDefaultParams();
        this.capabilities = capabilities;
        this.numFeatures = capabilities.getNumFeatures();
        this.featureMap = new int[numFeatures];
        this.averages = new Average[numFeatures];

        for (int i = 0; i < numFeatures; i++) {
            this.featureMap[i] = i;
            this.averages[i] = new Average(-1, -1, -1, -1, -1, false);
        }
    }

    /**
     * F�gt einen neuen Messwert hinzu.
     *
     * @param datum Der einzuf�gende Messwert.
     */
    public abstract void addDatum(DataEntry datum);

    /**
     * Werden Features nicht genutzt, k�nnen sie deaktiviert werden, um
     * Speicherplatz zu sparen.
     *
     * @param feature Das zu deaktivierende Feature.
     * @throws OTCManagerException wenn ein ung�ltiges Feature angegeben wurde.
     */
    final void disableFeature(final int feature) throws OTCManagerException {
        if (feature >= numFeatures) {
            throw new OTCManagerException("Unknown feature");
        }

        this.featureMap[feature] = -1;
        for (int i = feature + 1; i < numFeatures; i++) {
            this.featureMap[i]--;
        }
        this.numDisabledFeatures++;
    }

    public abstract float getAverage(int feature, float timeInterval);

    public abstract float getAverage(int feature, int numEntries);

    private String getDescriptions(final String separator) {
        String output = "Time" + separator;
        for (int i = 0; i < numFeatures; i++) {
            if (featureMap[i] == -1) {
                // Feature is disabled
                continue;
            }
            output = output.concat(capabilities.getDescription(i) + separator);
        }
        return output;
    }

    /**
     * Gibt den letzten Eintrag zur�ck.
     *
     * @return Letzter gespeicherter Eintrag.
     */
    final DataEntry getLastRelevantDatum() {
        if (entries.isEmpty()) {
            throw new OTCManagerException("No data!");
        }
        return entries.get(entries.size() - 1);
    }

    /**
     * Gibt den letzten Eintrag f�r ein bestimmtes Feature zur�ck.
     *
     * @return Letzter gespeicherter Eintrag.
     * @throws OTCManagerException
     */
    public final float getLastRelevantDatum(final int feature) throws OTCManagerException {
        if (entries.isEmpty()) {
            throw new OTCManagerException("No entries available");
        }
        return entries.get(entries.size() - 1).getValues()[getMappedFeature(feature)];
    }

    /**
     * Wenn Features deaktiviert wurden, muss die Feature-Id entsprechend
     * angepasst werden.
     *
     * @param feature Feature ohne Ber�cksichtigung inaktiver Features.
     * @return Feature mit Ber�cksichtigung inaktiver Features.
     * @throws OTCManagerException wenn ein ung�ltiges Feature angegeben wurde.
     */
    public final int getMappedFeature(final int feature) throws OTCManagerException {
        if (feature >= numFeatures) {
            throw new OTCManagerException("Unknown feature");
        }
        return featureMap[feature];
    }

    /**
     * Erzeugt einen Mittelwert f�r alle Werte, die in das angegebene
     * Zeitintervall (beginnend mit dem aktuellsten Eintrag) fallen.
     *
     * @param feature      Das Feature, f�r das der Durchschnitt berechnet werden soll
     *                     (int, Mapping �ber passend gesetzte static final Attribute der
     *                     implementierenden Klassen oder entsprechende Mapping-Klassen).
     * @param timeInterval Zeitintervall, in dem alle eingetragenen Werte ber�cksichtigt
     *                     werden sollen.
     * @return Zeitbezogener Mittelwert (pro Stunde, unter der Annahme dass alle
     * Zeitstempel der Messwerte in Sekunden sind). Falls nicht gen�gend
     * Eintr�ge f�r die Durchschnittsberechnung vorhanden sind, wird
     * Float.NaN zur�ckgegeben. Grenzwert: MIN_NUM_ENTRIES_AVERAGE.
     * @throws OTCManagerException <ul>
     *                             <li>Falls �berhaupt noch keine Eintr�ge vorhanden sind.</li>
     *                             <li>Falls ein ung�ltiges Feature angegeben wurde.</li>
     *                             </ul>
     */
    final Average getRawAverage(final int feature, final float timeInterval) throws OTCManagerException {
        if (feature < 0 || feature >= numFeatures || featureMap[feature] < 0) {
            throw new OTCManagerException("Unknown feature");
        }

        if (getLastRelevantDatum() == null) {
            throw new OTCManagerException("No data!");
        }

        final boolean exists = checkIfRequestedAverageExists(feature, timeInterval);
        if (exists) {
            return averages[feature].clone();
        }

        final int mappedFeature = featureMap[feature];

        final float currentTime = lastEntry.getTime();
        DataEntry sdv = null;
        float average = 0;
        float time = 0;
        int index = entries.size();
        int validEntries = 0;

        if (index > 3) {
            if (isVehicleBased(feature)) {
                // F�r diskrete Messwerte
                // Summe der g�ltigen Messwerte bilden
                do {
                    index--;
                    sdv = entries.get(index);
                    time = currentTime - sdv.getTime();

                    if (index == 0 || time >= timeInterval + smoothInterval) {
                        break;
                    }

                    if (!Float.isNaN(sdv.getValues()[mappedFeature])) {
                        float value = sdv.getValues()[mappedFeature];

                        if (time < smoothInterval) {
                            value = value * time / smoothInterval;
                        } else if (time > timeInterval) {
                            value = value * (timeInterval + smoothInterval - time) / smoothInterval;
                            if (value < 0) {
                                value = 0;
                            }
                        }

                        average += value;
                        validEntries++;
                    }
                } while (time < timeInterval + smoothInterval);
            } else {
                // F�r kontinuierliche Messwerte
                // Einstieg: Letzten g�ltigen Wert suchen
                DataEntry prevSdv;
                do {
                    index--;
                    prevSdv = entries.get(index);
                } while (Float.isNaN(prevSdv.getValues()[mappedFeature]) && index >= 0);

                // Summative Integration
                if (index > 3) {
                    do {
                        index--;
                        sdv = entries.get(index);
                        time = currentTime - sdv.getTime();
                        final float deltaTime = prevSdv.getTime() - sdv.getTime();
                        final float previousValue = prevSdv.getValues()[mappedFeature];

                        if (!Float.isNaN(sdv.getValues()[mappedFeature])) {
                            average += (sdv.getValues()[mappedFeature] + previousValue) * deltaTime * 0.5;
                            validEntries++;
                            prevSdv = sdv;
                        }
                    } while (time < timeInterval && index > 0);

                    if (index == 0) {
                        average /= time;
                    } else {
                        average /= timeInterval;
                    }
                }
            }
        }

        if (validEntries <= DefaultParams.L0_MIN_ENTRIES_AVERAGE || sdv == null) {
            averages[feature] = new Average(currentTime, feature, validEntries, time, Float.NaN, true);
        } else {
            averages[feature] = new Average(currentTime, feature, validEntries, time, average, true);
        }

        return averages[feature].clone();
    }

    /*
     * Check if requested average has already been computed. If so, return
     * precomputed value.
     */
    // TODO: Delta für die �berpr�fung des Intervalls an tats�chliche
    // Bedingungen anpassen.
    private boolean checkIfRequestedAverageExists(final int feature, final float timeInterval) {
        boolean isUpper = averages[feature].getTimeInterval() < timeInterval + 5;
        boolean isLower = averages[feature].getTimeInterval() > timeInterval - 5;
        boolean isSameTime = averages[feature].getTimeCreated() == lastEntry.getTime();
        return isUpper && isLower && isSameTime && averages[feature].isTimeBased();
    }

    /**
     * Erzeugt einen Mittelwert der letzten numEntries Werte. Nur g�ltige Werte
     * werden ber�cksichtigt.
     *
     * @param feature    Das Attribut, für das der Durchschnitt berechnet werden soll.
     * @param numEntries Anzahl Eintr�ge, beginnend mit dem aktuellsten, die
     *                   ber�cksichtigt werden sollen.
     * @return Mittelwert, bezogen auf die <b>Anzahl der g�ltigen Werte</b>.
     * @throws OTCManagerException <ul>
     *                             <li>Falls �berhaupt noch keine Eintr�ge vorhanden sind.</li>
     *                             <li>Falls nicht gen�gend Eintr�ge f�r die
     *                             Durchschnittsberechnung vorhanden sind. Grenzwert:
     *                             MIN_NUM_ENTRIES_AVERAGE.</li>
     *                             <li>Falls ein ung�ltiges Feature angegeben wurde</li>
     *                             </ul>
     */
    final Average getRawAverage(final int feature, final int numEntries) throws OTCManagerException {
        if (feature < 0 || feature >= numFeatures || featureMap[feature] < 0) {
            throw new OTCManagerException("Unknown feature");
        }

		/*
         * Check if requested average has already been computed. If so, return
		 * precomputed value.
		 */
        if (averages[feature].getNumEntries() == numEntries
                && averages[feature].getTimeCreated() == lastEntry.getTime() && !averages[feature].isTimeBased()) {
            return averages[feature];
        }

        int validEntries = 0;
        int index = 0;
        final int lastIndex = entries.size();
        DataEntry sdv = null;
        float average = 0;

        final int mappedFeature = featureMap[feature];

        while (validEntries < numEntries) {
            index++;
            if (index > lastIndex) {
                break;
            }

            sdv = entries.get(lastIndex - index);

            if (!Float.isNaN(sdv.getValues()[mappedFeature])) {
                average += sdv.getValues()[mappedFeature];
                validEntries++;
            }
        }

        if (validEntries <= DefaultParams.L0_MIN_ENTRIES_AVERAGE || sdv == null) {
            throw new OTCManagerException("Average konnte nicht berechnet werden.");
        } else {
            // average = average / validEntries;
            averages[feature] = new Average(lastEntry.getTime(), feature, validEntries, sdv.getTime()
                    - lastEntry.getTime(), average, false);
            return averages[feature];
        }
    }

    /**
     * Setzt die Werte f�r das smoothInterval und MIN_NUM_ENTRIES_AVERAGE anhand
     * der in der XML Konfiguration des PropertyManagers definierten Werte.
     */
    private void initialiseDefaultParams() {
        smoothInterval = DefaultParams.L0_SMOOTH_INTERVAL;
    }

    private boolean isVehicleBased(final int feature) {
        return capabilities.isVehicleBased(feature);
    }

    /**
     * Erledigt die Aufbereitung: Ung�ltige Werte werden aussortiert, das Array
     * auf die Gr��e der tats�chlich aktiven Features reduziert.
     *
     * @param entry DataEntry-Objekt mit Werten f�r alle Features, Reihenfolge
     *              entsprechend Mapping-Klasse
     * @return Aufbereitetes DataEntry-Objekt.
     */
    public abstract DataEntry preprocessEntry(DataEntry entry);

    /**
     * Setzt die gespeicherten Datenwerte und Durchschnitte bei einem Neustart
     * zur�ck.
     *
     * @see #entries
     * @see #averages
     */
    public final void reset() {
        this.entries.clear();
        for (int i = 0; i < numFeatures; i++) {
            this.averages[i] = new Average(-1, -1, -1, -1, -1, false);
        }
    }

    public final String toString() {
        return toString("; ");
    }

    public final String toString(final String separator) {
        if (entries.isEmpty()) {
            return "No data";
        }

        final String linesep = System.getProperty("line.separator");
        String output = "";

        for (int i = 0; i < entries.size(); i++) {
            DataEntry entry = entries.get(i);
            output = output.concat(entry.getTime() + separator);
            float[] values = entry.getValues();

            for (int j = 0; j < numFeatures - numDisabledFeatures; j++) {
                output = output.concat(values[j] + separator);
            }

            output = output.concat(linesep);
        }

        final String description = getDescriptions(separator) + linesep;
        output = description.concat(output + linesep);

        return output;
    }
}
