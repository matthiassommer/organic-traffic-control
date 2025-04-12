package de.dfg.oc.otc.layer1.observer;

import de.dfg.oc.otc.layer1.Layer1Exception;
import de.dfg.oc.otc.layer1.observer.monitoring.DataStorage;
import de.dfg.oc.otc.layer1.observer.monitoring.StatisticalDataStorage;
import de.dfg.oc.otc.layer1.observer.monitoring.StatisticsCapabilities;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.TrafficType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mainly used for statistical output to the GUI (as well as for the routing
 * component).
 *
 * @author rochner
 */
public class Layer1Observer {
    /**
     * Ein Observer f�r alle Daten, die �ber Statistikfunktionen ermittelt
     * werden.
     */
    private final Map<TrafficType, StatisticsObserver> statisticsObservers;
    /**
     * Ein Observer f�r alle Daten, die �ber Detektoren ermittelt werden.
     */
    private DetectorObserver detectorObserver;

    /**
     * Erzeugt einen neuen Layer1Observer. Der zugeh�rige StatisticsObserver
     * wird f�r alle Turnings angelegt, die motorisierte Fahrzeuge bedienen.
     *
     * @param junction Die Junction, f�r die dieser Layer1Observer zust�ndig ist.
     * @throws de.dfg.oc.otc.layer1.Layer1Exception wenn die Junction ueber keine Turnings verfuegt.
     * @see TrafficType
     */
    public Layer1Observer(final AimsunJunction junction) throws Layer1Exception {
        if (!junction.getDetectors().isEmpty()) {
            this.detectorObserver = new DetectorObserver(junction.getDetectors());
        }

        this.statisticsObservers = new EnumMap<>(TrafficType.class);

        for (TrafficType trafficType : TrafficType.values()) {
            if (!junction.getTurnings(trafficType).isEmpty() && junction.getTurnings(trafficType).get(0) != null) {
                StatisticsObserver statisticsObserver = new StatisticsObserver(junction.getTurnings(trafficType));
                this.statisticsObservers.put(trafficType, statisticsObserver);
            }
        }
    }

    /**
     * Gibt die durchschnittliche Anzahl der Stops pro Fahrzeug an dem
     * beobachteten Knoten zur�ck. Das Zeitintervall (in Sekunden), �ber das die
     * Werte gemittelt werden, wird als Parameter 'interval' �bergeben.
     *
     * @param interval Intervall, �ber das die Werte gemittelt werden.
     * @return durchschnittliche Anzahl der Stops pro Fahrzeug. Im Fehlerfall
     * wird Float.NaN zur�ckgegeben.
     */
    private float getAverageStopsPerVehicle(final TrafficType trafficType, final float interval) {
        // Part 1: we need ALL turnings of the node
        final List<Integer> turnIDs = getTurningsForNode(trafficType);

        // Part 2: Collect stop values
        int turnings = 0;
        float stops = 0;

        if (!turnIDs.isEmpty()) {
            StatisticsObserver statObserver;
            float curStops;

            for (int id : turnIDs) {
                try {
                    statObserver = statisticsObservers.get(trafficType);
                    if (statObserver == null) {
                        continue;
                    }

                    curStops = statObserver.getAverageValue(id, StatisticsCapabilities.NUMSTOPS, interval);
                    if (!Float.isNaN(curStops) && curStops >= 0) {
                        turnings++;
                        stops += curStops;
                    }
                } catch (OTCManagerException ome) {
                    // Turning ignoriert
                }
            }
        }

        if (turnings == 0) {
            return Float.NaN;
        }
        return stops / turnings;
    }

    public final DetectorObserver getDetectorObserver() {
        return detectorObserver;
    }

    /**
     * Liefert eine Bewertung des Knotens.
     *
     * @param source      Datenquelle, entweder DETECTOR oder STATISTICS.
     * @param attribute   Das Attribut, das Basis f�r die Bewertung sein soll, z.B. LOS
     * @param interval    Zeitintervall, �ber das die Eingangswerte f�r die Bewertung
     *                    gemittelt werden sollen.
     * @param trafficType
     * @return Bewertung
     * @throws Layer1Exception wenn source oder attribute null sind, wenn ein ung�ltiger Datenquellentyp angegeben wird
     */
    final float getEvaluation(final DataSource source, final Attribute attribute, final TrafficType trafficType,
                              final float interval) throws Layer1Exception {
        switch (source) {
            case DETECTOR:
                return detectorObserver.getAverageValue(1, 1, interval);
            case STATISTICS:
                if (attribute == Attribute.LOS) {
                    return getLOSValue(trafficType, interval);
                } else if (attribute == Attribute.QUEUELENGTH) {
                    return getQueueValue(trafficType, interval);
                } else if (attribute == Attribute.UTILISATION) {
                    return getUtilisationValue(trafficType, interval, attribute.getParameter());
                } else if (attribute == Attribute.AVSTOPS) {
                    return getAverageStopsPerVehicle(trafficType, interval);
                } else if (attribute == Attribute.MAXSTOPS) {
                    return getMaxStopsValue(trafficType, interval);
                } else {
                    throw new Layer1Exception("Unknown criteria");
                }
            default:
                throw new Layer1Exception("Ung�ltiger Datenquellentyp angegeben.");
        }
    }

    /**
     * Gibt den LOS-Wert f�r den beobachteten Knoten zur�ck. Das Zeitintervall
     * (in Sekunden), �ber das die Werte gemittelt werden, wird als Parameter
     * interval �bergeben. https://en.wikipedia.org/wiki/Level_of_service
     *
     * @param trafficType
     * @param interval    Intervall, �ber das die Werte gemittelt werden.
     * @return LOS-Wert.
     */
    private float getLOSValue(final TrafficType trafficType, final float interval) {
        // Part 1: we need all relevant turnings of the node
        final List<Integer> turnIDs = getTurningsForNode(trafficType);
        if (turnIDs.isEmpty()) {
            return Float.NaN;
        }

        // Part 2: Calculate LOS
        float flow = 0;
        float delay = 0;
        float curFlow, curDelay;
        StatisticsObserver statObserver;

        for (int turning : turnIDs) {
            try {
                statObserver = statisticsObservers.get(trafficType);
                if (statObserver == null) {
                    continue;
                }
                curFlow = statObserver.getAverageValue(turning, StatisticsCapabilities.FLOW, interval);
                curDelay = statObserver.getAverageValue(turning, StatisticsCapabilities.DELAYTIME, interval);
            } catch (OTCManagerException ome) {
                curFlow = 0;
                curDelay = 0;
            }
            if (!Float.isNaN(curFlow) && !Float.isNaN(curDelay)) {
                flow += curFlow;
                delay += curDelay * curFlow;
            }
        }

        // Return the result - avoid division by zero
        if (flow <= 0) {
            return Float.NaN;
        }
        return delay / flow;
    }

    /**
     * Gibt die durchschnittliche Anzahl der Stops pro Fahrzeug an der des
     * beobachteten Knotens mit dem h�chsten Wert zur�ckk. Das Zeitintervall (in
     * Sekunden), �ber das die Werte gemittelt werden, wird als Parameter
     * 'interval' �bergeben.
     *
     * @param trafficType
     * @param interval    Intervall, �ber das die Werte gemittelt werden.
     * @return durchschnittliche Anzahl der Stops pro Fahrzeug. Im Fehlerfall
     * wird Float.NaN zur�ckgegeben.
     */
    private float getMaxStopsValue(final TrafficType trafficType, final float interval) {
        // Part 1: we need ALL turnings of the node
        final List<Integer> turnIDs = getTurningsForNode(trafficType);

        // Part 2: Collect stop values
        float maxStops = 0;

        StatisticsObserver statObserver;
        for (int curID : turnIDs) {
            try {
                statObserver = statisticsObservers.get(trafficType);
                if (statObserver == null) {
                    continue;
                }
                float curStops = statObserver.getAverageValue(curID, StatisticsCapabilities.NUMSTOPS, interval);
                if (!Float.isNaN(curStops) && curStops > maxStops) {
                    maxStops = curStops;
                }
            } catch (OTCManagerException ome) {
                // Turning ignoriert
            }
        }

        if (maxStops == 0) {
            return Float.NaN;
        }
        return maxStops;
    }

    /**
     * Gibt die durchschnittliche L�nge der Warteschlangen an dem beobachteten
     * Knoten zur�ck. Das Zeitintervall (in Sekunden), �ber das die Werte
     * gemittelt werden, wird als Parameter 'interval' �bergeben.
     *
     * @param trafficType
     * @param interval    Intervall, �ber das die Werte gemittelt werden.
     * @return durchscnittliche L�nge der Warteschlangen. Im Fehlerfall wird
     * Float.NaN zur�ckgegeben.
     */
    private float getQueueValue(final TrafficType trafficType, final float interval) {
        // Part 1: we need ALL turnings of the node
        final List<Integer> turnIDs = getTurningsForNode(trafficType);

        // Part 2: Collect queuelength values
        float queue = 0;
        int turnings = 0;

        StatisticsObserver statObserver;
        for (int id : turnIDs) {
            try {
                statObserver = statisticsObservers.get(trafficType);
                if (statObserver == null) {
                    continue;
                }
                float curQueue = statObserver.getAverageValue(id, StatisticsCapabilities.QUEUELENGTH, interval);
                if (!Float.isNaN(curQueue) && curQueue >= 0) {
                    turnings++;
                    queue += curQueue;
                }
            } catch (OTCManagerException ome) {
                // Turning ignoriert
            }
        }

        if (turnings <= 0) {
            return Float.NaN;
        }
        return queue / turnings;
    }

    /**
     * Gibt den StatisticsObserver f�r den angegebenen Verkehrstyp zur�ck.
     *
     * @param trafficType Der Verkehrstyp, f�r den der StatisticsObserver zur�ckgegeben
     *                    werden soll.
     * @return Der statisticsObserver f�r motorisierten Verkehr.
     */
    public final StatisticsObserver getStatisticsObserver(final TrafficType trafficType) {
        return statisticsObservers.get(trafficType);
    }

    /**
     * Method used to collect all turning IDs of given TrafficType of the
     * current node. Returns {@code null} if no turnings of the given
     * TrafficType exist.
     *
     * @param trafficType TrafficType of the Turnings to be returned.
     * @return Vector with turning IDs (integer values) or {@code null} if
     * no turnings of the given TrafficType exist.
     * @see TrafficType
     */
    private List<Integer> getTurningsForNode(final TrafficType trafficType) {
        final StatisticsObserver statObserver = statisticsObservers.get(trafficType);

        if (statObserver == null) {
            return Collections.emptyList();
        }

        final List<Integer> turnIDs = new ArrayList<>();
        final Collection<DataStorage> storages = statObserver.getStorages();

        turnIDs.addAll(storages.stream().map(storage -> ((StatisticalDataStorage) storage).getObservedObject().getId()).collect(Collectors.toList()));

        return turnIDs;
    }

    /**
     * Methode benötigt für Stream Info.
     *
     * @param trafficType
     * @param interval
     * @param turningID
     * @param capability
     * @return
     */
    public final float getTurningStatistic(final TrafficType trafficType, final float interval, final int turningID,
                                           final int capability) {
        if (turningID <= 0) {
            return Float.NaN;
        }

        try {
            final StatisticsObserver statObserver = statisticsObservers.get(trafficType);
            if (statObserver == null) {
                return Float.NaN;
            } else {
                return statObserver.getAverageValue(turningID, capability, interval);
            }
        } catch (OTCManagerException ome) {
            return Float.NaN;
        }
    }

    /**
     * Gibt den Auslastungsgrad des beobachteten Knotens zur�ck. Das
     * Zeitintervall (in Sekunden), �ber das die Werte gemittelt werden, wird
     * als Parameter 'interval' �bergeben.
     *
     * @param trafficType
     * @param interval    Intervall, �ber das die Werte gemittelt werden.
     * @param capacity    Aktuelle Kapazit�t des beobachteten Knotens.
     * @return Auslastungsgrad des Knotens in Prozent. Im Fehlerfall wird
     * Float.NaN zur�ckgegeben.
     */
    private float getUtilisationValue(final TrafficType trafficType, final float interval, final float capacity) {
        // Part 1: we need ALL turnings of the node
        final List<Integer> turningIDs = getTurningsForNode(TrafficType.INDIVIDUAL_TRAFFIC);

        // Part 2: Collect Flow values
        float flow = 0;
        float currentFlow;

        for (int detector : turningIDs) {
            try {
                StatisticsObserver statObserver = statisticsObservers.get(trafficType);
                if (statObserver == null) {
                    continue;
                }
                currentFlow = statObserver.getAverageValue(detector, StatisticsCapabilities.FLOW, interval);
            } catch (OTCManagerException ome) {
                currentFlow = -1;
            }

            if (!Float.isNaN(currentFlow) && currentFlow > 0) {
                flow += currentFlow;
            }
        }

        if (flow == 0) {
            return Float.NaN;
        }
        return flow * 100 / capacity;
    }

    public enum DataSource {
        DETECTOR, STATISTICS
    }
}
