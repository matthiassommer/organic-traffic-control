package de.dfg.oc.otc.layer1.observer.monitoring;

import de.dfg.oc.otc.manager.OTCManagerException;

/**
 * @author rochner
 */
public class StatisticalDataStorage extends DataStorage {
    private final AbstractObservableStatistics observedStatistics;

    public StatisticalDataStorage(final AbstractObservableStatistics observedStatistics) {
        super(new StatisticsCapabilities());
        this.observedStatistics = observedStatistics;
    }

    @Override
    public final void addDatum(DataEntry datum) {
        boolean noChange = true;
        datum = preprocessEntry(datum);

        try {
            final DataEntry previousEntry = getLastRelevantDatum();

            if (datum.getValues()[featureMap[StatisticsCapabilities.FLOW]] > 0) {
                noChange = false;
            } else {
                for (int i = 1; i < numFeatures - numDisabledFeatures; i++) {
                    // Vergleiche mit == oder != bei NaN verhalten sich teilweise
                    // nicht ganz intuitiv. Java-Doku beachten!
                    if (!Float.isNaN(datum.getValues()[i]) && datum.getValues()[i] != previousEntry.getValues()[i]) {
                        noChange = false;
                        break;
                    }
                }
            }
        } catch (OTCManagerException e) {
            noChange = false;
        }

        if (!noChange) {
            entries.add(datum);
        }

        lastEntry = datum;
        setChanged();
        notifyObservers();
    }

    /**
     * Get the average value of a feature for a specified time interval.
     *
     * @return
     */
    public final float getAverage(final int feature, final float timeInterval) throws OTCManagerException {
        Average rawAverage;
        try {
            rawAverage = getRawAverage(feature, timeInterval);
        } catch (OTCManagerException ome) {
            return Float.NaN;
        }

        switch (feature) {
            case StatisticsCapabilities.FLOW:
                return rawAverage.getValue() / timeInterval * 3600f;
            case StatisticsCapabilities.DELAYTIME:
                return rawAverage.getValue() / getRawAverage(StatisticsCapabilities.FLOW, timeInterval).getValue();
            case StatisticsCapabilities.STOPTIME:
                return rawAverage.getValue() / getRawAverage(StatisticsCapabilities.FLOW, timeInterval).getValue();
            case StatisticsCapabilities.TRAVELTIME:
                return rawAverage.getValue() / getRawAverage(StatisticsCapabilities.FLOW, timeInterval).getValue();
            case StatisticsCapabilities.QUEUELENGTH:
                return rawAverage.getValue();
            case StatisticsCapabilities.QUEUEMAX:
                return rawAverage.getValue();
            case StatisticsCapabilities.NUMSTOPS:
                return rawAverage.getValue() / getRawAverage(StatisticsCapabilities.FLOW, timeInterval).getValue();
            case StatisticsCapabilities.SPEED:
                return rawAverage.getValue() / getRawAverage(StatisticsCapabilities.FLOW, timeInterval).getValue();
            case StatisticsCapabilities.SPEEDDEVIATION:
                return rawAverage.getValue() / getRawAverage(StatisticsCapabilities.FLOW, timeInterval).getValue();
            default:
                throw new OTCManagerException("Unknown Feature");
        }
    }

    public final float getAverage(final int feature, final int numEntries) throws OTCManagerException {
        Average rawAverage;
        try {
            rawAverage = getRawAverage(feature, numEntries);
        } catch (OTCManagerException ome) {
            return Float.NaN;
        }

        switch (feature) {
            case StatisticsCapabilities.FLOW:
                return rawAverage.getValue() / rawAverage.getNumEntries();
            case StatisticsCapabilities.DELAYTIME:
                return rawAverage.getValue() / getRawAverage(StatisticsCapabilities.FLOW, numEntries).getValue();
            case StatisticsCapabilities.STOPTIME:
                return rawAverage.getValue() / getRawAverage(StatisticsCapabilities.FLOW, numEntries).getValue();
            case StatisticsCapabilities.TRAVELTIME:
                return rawAverage.getValue() / getRawAverage(StatisticsCapabilities.FLOW, numEntries).getValue();
            case StatisticsCapabilities.QUEUELENGTH:
                return rawAverage.getValue() / rawAverage.getNumEntries();
            case StatisticsCapabilities.QUEUEMAX:
                return rawAverage.getValue() / rawAverage.getNumEntries();
            default:
                throw new OTCManagerException("Unknown Feature");
        }
    }

    public final AbstractObservableStatistics getObservedObject() {
        return observedStatistics;
    }

    @Override
    public final StatisticalDataValue preprocessEntry(final DataEntry entry) {
        final float[] clearedValues = new float[numFeatures - numDisabledFeatures];

        for (int i = 0; i < numFeatures; i++) {
            if (featureMap[i] < 0) {
                continue;
            }
            final float originalValue = entry.getValues()[i];
            clearedValues[featureMap[i]] = originalValue >= 0 ? originalValue : Float.NaN;
        }

        return new StatisticalDataValue(entry.getTime(), clearedValues);
    }
}
