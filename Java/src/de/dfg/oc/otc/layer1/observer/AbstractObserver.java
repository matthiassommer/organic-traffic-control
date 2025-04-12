package de.dfg.oc.otc.layer1.observer;

import de.dfg.oc.otc.layer1.observer.monitoring.DataStorage;
import de.dfg.oc.otc.layer1.observer.monitoring.StatisticsCapabilities;
import de.dfg.oc.otc.manager.OTCManagerException;

import java.util.*;

/**
 * Abstrakte Basisklasse f�r Observer, die z.B. Detektordaten oder statistische
 * Daten vom Simulator beobachten.
 *
 * @author rochner
 */
public abstract class AbstractObserver implements Observer {
    /**
     * Map, um von der Id eines Netzwerk-Objekts einfach auf den Eintrag in der
     * DataStorage-Liste zu kommen (beschleunigt das Update).
     */
    final Map<Integer, DataStorage> observedObjectsMap;

    AbstractObserver() {
        this.observedObjectsMap = new HashMap<>();
    }

    public final void clearAllObservers() {
        observedObjectsMap.values().forEach(DataStorage::deleteObservers);
    }

    public float getAverageValue(final int objectId, final int feature, final float timeInterval)
            throws OTCManagerException {
        final DataStorage storage = observedObjectsMap.get(objectId);
        if (storage != null) {
            return storage.getAverage(feature, timeInterval);
        }
        throw new OTCManagerException("No object with ID " + objectId);
    }

    public final float getAverageValue(final int objectId, final int feature, final int numEntries)
            throws OTCManagerException {
        final DataStorage storage = observedObjectsMap.get(objectId);
        if (storage != null) {
            return storage.getAverage(feature, numEntries);
        }
        throw new OTCManagerException("No object with ID " + objectId);
    }

    public final float getCurrentValue(final int objectId, final int feature) throws OTCManagerException {
        final DataStorage storage = observedObjectsMap.get(objectId);
        if (storage != null) {
            return storage.getLastRelevantDatum(feature);
        }
        throw new OTCManagerException("No object with ID " + objectId);
    }

    /**
     * Gibt die Anzahl der von diesem Observer beobachteten Objekte zur�ck.
     *
     * @return Anzahl bobachteter Objekte.
     */
    public final int getNumObservedObjects() {
        return observedObjectsMap.size();
    }

    /**
     * Liefert eine Situationsbeschreibung aller Turnings als Array von floats.
     * Elemente, f�r die kein g�ltiger Durchschnitt ermittelt werden kann, sind
     * mit Float.NaN besetzt.
     *
     * @param timeInterval Intervall, �ber das die jeweiligen Werte gemittelt werden
     * @param feature      Feature, dessen Werte ausgegeben werden sollen
     * @return Array mit Mittelwerten des gew�hlten Features f�r alle
     * beobachteten Objekte oder Float.NaN, wenn der Mittelwert nicht
     * bestimmt werden konnte.
     * @throws OTCManagerException Wenn nicht mindestens ein g�ltiger Wert in der Situation
     *                             enthalten ist.
     * @see DataStorage#getAverage(int, float)
     */
    public final float[] getSituation(final float timeInterval, final int feature) {
        final float[] situation = new float[observedObjectsMap.size()];
        int i = 0;

        for (DataStorage store : observedObjectsMap.values()) {
            situation[i] = store.getAverage(feature, timeInterval);
            if (Float.isNaN(situation[i])) {
                switch (feature) {
                    case StatisticsCapabilities.FLOW:
                        situation[i] = 0;
                    default:
                        break;
                }
            }
            i++;
        }

        return situation;
    }

    public final DataStorage getStorage(final int id) {
        return observedObjectsMap.get(id);
    }

    public final Collection<DataStorage> getStorages() {
        return observedObjectsMap.values();
    }

    public abstract void update(final Observable o, final Object arg);
}
