package de.dfg.oc.otc.layer1.observer;

import de.dfg.oc.otc.layer1.observer.monitoring.*;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;

import java.util.Observable;

/**
 * Diese Klasse implementiert einen Observer f�r eine beliebige Anzahl von
 * {@link de.dfg.oc.otc.manager.aimsun.detectors.SubDetector}.
 * Jeder {@link AbstractObserver} ist als Observer bei den
 * beobachteten {@link de.dfg.oc.otc.manager.aimsun.detectors.SubDetector} eingetragen.
 *
 * @author rochner
 */
public class DetectorObserver extends AbstractObserver {
    public DetectorObserver() {
        super();
    }

    /**
     * Create new Observer for a list of detectors.
     */
    DetectorObserver(final Iterable<Detector> detectors) {
        super();
        for (Detector detector : detectors) {
            DetectorDataStorage storage = new DetectorDataStorage(detector);
            observedObjectsMap.put(detector.getId(), storage);
            detector.addObserver(this);
        }
    }

    /**
     * Add a new detector to DataStorage.
     */
    public final void addDetector(final Detector detector) {
        final DetectorDataStorage detectorStorage = new DetectorDataStorage(detector);
        observedObjectsMap.put(detector.getId(), detectorStorage);
        detector.addObserver(this);
    }

    @Override
    public final float getAverageValue(final int detectorId, final int feature, final float timeInterval)
            throws OTCManagerException {
        final DetectorDataStorage storage = (DetectorDataStorage) observedObjectsMap.get(detectorId);
        if (storage != null) {
            return storage.getAverage(feature, timeInterval);
        } else {
            throw new OTCManagerException("No detector with ID " + detectorId);
        }
    }

    public final void update(final Observable o, final Object arg) {
        final Detector obsDetector = (Detector) o;
        if (arg == null) {
            // Es ist ein Neustart erfolgt, History l�schen.
            observedObjectsMap.values().forEach(DataStorage::reset);
            return;
        }
        final DataEntry value = (DetectorDataValue) arg;
        final DetectorDataStorage storage = (DetectorDataStorage) observedObjectsMap.get(obsDetector.getId());
        if (storage != null) {
            storage.addDatum(value);
        }
    }
}
