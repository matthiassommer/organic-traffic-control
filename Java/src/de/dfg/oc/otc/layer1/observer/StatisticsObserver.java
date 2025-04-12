package de.dfg.oc.otc.layer1.observer;

import de.dfg.oc.otc.layer1.observer.monitoring.*;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.Turning;

import java.util.Observable;

/**
 * Diese Klasse implementiert einen Observer f�r eine beliebige Anzahl von
 * Turnings. Jeder StatisticsObserver ist als Observer bei den beobachteten
 * Turnings eingetragen.
 *
 * @author rochner
 */
public class StatisticsObserver extends AbstractObserver {
    /**
     * Erzeugt einen neuen Observer.
     *
     * @param observables Array mit Objekten (Netzwerk, Section, Turning), die von
     *                    diesem Observer �berwacht werden sollen.
     */
    public StatisticsObserver(final Iterable<Turning> observables) {
        super();

        for (AbstractObservableStatistics statistics : observables) {
            StatisticalDataStorage storage = new StatisticalDataStorage(statistics);
            observedObjectsMap.put(statistics.getId(), storage);
            statistics.addObserver(this);
        }
    }

    @Override
    public final float getAverageValue(final int turningId, final int feature, final float timeInterval)
            throws OTCManagerException {
        final StatisticalDataStorage storage = (StatisticalDataStorage) observedObjectsMap.get(turningId);
        if (storage != null) {
            return storage.getAverage(feature, timeInterval);
        } else {
            throw new OTCManagerException("No turning with Id " + turningId + " exists.");
        }
    }

    final String[] getSituationDescription(final int attribute) {
        final String[] descriptions = new String[observedObjectsMap.size() + 1];
        descriptions[0] = new StatisticsCapabilities().getDescription(attribute);
        int i = 1;

        for (DataStorage store : observedObjectsMap.values()) {
            StatisticalDataStorage storage = (StatisticalDataStorage) store;
            if (storage.getObservedObject() instanceof Turning) {
                Turning turning = (Turning) storage.getObservedObject();
                descriptions[i] = "Turning " + turning.getInSection().getId() + " to "
                        + turning.getOutSection().getId();
            } else if (storage.getObservedObject() instanceof Section) {
                Section section = (Section) storage.getObservedObject();
                descriptions[i] = "Section " + section.getId();
            } else {
                descriptions[i] = "Unknown Object";
            }
            i++;
        }

        return descriptions;
    }

    /**
     * Beim Aufruf der {@code getSituation()}-Methode wird ein Feld mit
     * statistischen Werten (z.B. Verkehrsfl�ssen) zur�ckgegeben. Diese Methode
     * erm�glich die Zuordnung dieser Werte zu Abbiegebeziehungen indem ein Feld
     * von Section-IDs zur�ckgegeben wird, das zu jedem Wert die IDs der Ein-
     * und Ausgangssections enth�lt.
     *
     * @return ein Feld von Section-IDs, das zu jedem von der
     * {@code getStatistics}-Methode gelieferten Wert die IDs der
     * zugeh�rigen Ein- und Ausgangssections enth�lt (oder
     * {@code -1}, falls ein Problem aufgetreten ist)
     */
    public final int[] getSituationSectionIDs() {
        final int[] sectionIDs = new int[2 * observedObjectsMap.size()];
        int i = 0;

        for (DataStorage store : observedObjectsMap.values()) {
            StatisticalDataStorage statisticStorage = (StatisticalDataStorage) store;
            if (statisticStorage.getObservedObject() instanceof Turning) {
                Turning turning = (Turning) statisticStorage.getObservedObject();
                sectionIDs[i] = turning.getInSection().getId();
                sectionIDs[i + 1] = turning.getOutSection().getId();
            } else {
                // Invalid ID
                sectionIDs[i] = -1;
                sectionIDs[i + 1] = -1;
            }
            i += 2;
        }

        return sectionIDs;
    }

    public final void update(final Observable o, final Object arg) {
        if (o instanceof Turning) {
            if (arg == null) {
                // Reset, delete history.
                observedObjectsMap.values().forEach(DataStorage::reset);
                return;
            }

            StatisticalDataValue value;
            try {
                value = (StatisticalDataValue) arg;
            } catch (RuntimeException e) {
                throw new OTCManagerException("arg is no StatisticalDataValue");
            }

            final Turning turning = (Turning) o;
            final StatisticalDataStorage storage = (StatisticalDataStorage) observedObjectsMap.get(turning.getId());
            if (storage != null) {
                storage.addDatum(value);
            }
        }
    }
}
