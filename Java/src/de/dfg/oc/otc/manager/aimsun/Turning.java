package de.dfg.oc.otc.manager.aimsun;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer1.observer.ForecastAdapter;
import de.dfg.oc.otc.layer1.observer.monitoring.AbstractObservableStatistics;
import de.dfg.oc.otc.layer1.observer.monitoring.StatisticsCapabilities;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;
import de.dfg.oc.otc.tools.LimitedQueue;
import forecasting.DefaultForecastParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a turning of an intersections between two {@link Section}s.
 *
 * @author Matthias Sommer
 */
public class Turning extends AbstractObservableStatistics {
    /**
     * List of detectors lying on this turning.
     */
    private final List<Detector> detectors;
    /**
     * Identifier of this turning.
     */
    private final int id;
    /**
     * Enthält die durchschnittliche Schlangenlänge für diese Abbiegebeziegung
     * bei den letzten {@link de.dfg.oc.otc.layer1.controller.lcs.LCS}-Aufrufen.
     */
    private final LimitedQueue<Float> queueAvgHistory;
    /**
     * Enthält die maximale Schlangenlänge für diese Abbiegebeziegung bei den
     * letzten {@link de.dfg.oc.otc.layer1.controller.lcs.LCS}-Aufrufen.
     */
    private final LimitedQueue<Float> queueMaxHistory;
    /**
     * Incoming section of this turning.
     */
    private final Section sectionIn;
    /**
     * Outgoing section of this turning.
     */
    private final Section sectionOut;
    /**
     * Variables for start and end of the lanes of this turning.
     */
    private int firstLaneOrigin, lastLaneOrigin, firstLaneDestination, lastLaneDestination;
    /**
     * Gibt die Verkehrsart an, die über dieses {@link Turning} abgewickelt
     * wird.
     */
    private TrafficType trafficType;
    /**
     * Adapter for extending turnings with flow forecast abilities.
     */
    private ForecastAdapter flowForecaster;

    /**
     * Creates a turning.
     *
     * @param id         Id des {@link Turning}
     * @param sectionIn  {@link Section}, die in das {@link Turning} führt
     * @param sectionOut {@link Section}, die aus dem {@link Turning} führt
     */
    Turning(final int id, final Section sectionIn, final Section sectionOut) {
        this.id = id;
        this.sectionIn = sectionIn;
        this.sectionOut = sectionOut;
        this.detectors = new ArrayList<>(3);
        this.trafficType = TrafficType.UNDEFINED;
        this.queueMaxHistory = new LimitedQueue<>(3);
        this.queueAvgHistory = new LimitedQueue<>(3);

        if (DefaultForecastParameters.IS_FORECAST_MODULE_ACTIVE) {
            this.flowForecaster = new ForecastAdapter((int) (DefaultParams.L0_MIN_CYCLES_DELAY * 90 / OTCManager.getSimulationStepSize()));
        }
    }

    public ForecastAdapter getFlowForecaster() {
        return this.flowForecaster;
    }

    final void addDetector(final Detector detector) {
        this.detectors.add(detector);
    }

    /**
     * Fügt eine neue durchschnittliche oder maximale Schlangenlänge zu
     * {@code queueAvgHistory} bzw. {@code queueMaxHistory} hinzu.
     *
     * @param queueType   durchschnittliche oder maximale Länge (
     *                    {@code StatisticsCapabilities.QUEUELENGH} oder
     *                    {@code StatisticsCapabilities.QUEUEMAX}
     * @param queueLength eine neue Schlangenlänge
     */
    public void addQueueData(final int queueType, final float queueLength) {
        switch (queueType) {
            case StatisticsCapabilities.QUEUEMAX:
                if (!Float.isNaN(queueLength)) {
                    this.queueMaxHistory.add(queueLength);
                }
                break;
            case StatisticsCapabilities.QUEUELENGTH:
                if (!Float.isNaN(queueLength)) {
                    this.queueAvgHistory.add(queueLength);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Liefert eine html-formatierte Beschreibung des Turnings in Form einer
     * Tabellenzeile mit 5 Spalten: Id, Eingangssektion, Ausgangssektion, Typ
     * der über dieses Turning abgearbeiteten Verkehrsart, zugeordnete
     * Detektoren.
     *
     * @return String der formatierten Beschreibung.
     */
    public final String getDescription() {
        String output = "<tr><td>" + id + "</td><td>" + sectionIn.getId() + " (Lanes " + firstLaneOrigin + " to "
                + lastLaneOrigin + ")</td><td>" + sectionOut.getId() + " (Lanes " + firstLaneDestination + " to "
                + lastLaneDestination + ")</td><td>" + trafficType.name() + "</td><td>";

        for (Detector detector : detectors) {
            output = output.concat(detector.getId() + " ");
        }

        return output.concat("</td></tr>");
    }

    public final List<Detector> getDetectors() {
        return this.detectors;
    }

    final int getFirstLaneOrigin() {
        return this.firstLaneOrigin;
    }

    @Override
    public final int getId() {
        return this.id;
    }

    public final Section getInSection() {
        return this.sectionIn;
    }

    final int getLastLaneOrigin() {
        return this.lastLaneOrigin;
    }

    public final Section getOutSection() {
        return this.sectionOut;
    }

    final RoadType getRoadType() {
        return this.sectionIn.getRoadType();
    }

    /**
     * Gibt den Typ der Verkehrsart zurück, die über dieses {@link Turning}
     * abgewickelt wird.
     *
     * @return Verkehrsart
     * @see TrafficType
     */
    final TrafficType getTrafficType() {
        return this.trafficType;
    }

    /**
     * Setzt den Typ der Verkehrsart, die über dieses {@link Turning}
     * abgewickelt wird.
     *
     * @param trafficType Verkehrsart
     * @see TrafficType
     */
    final void setTrafficType(final TrafficType trafficType) {
        this.trafficType = trafficType;
    }

    /**
     * Gibt {@code true} zurück, falls sich für diese Abbiegebeziehung eine
     * kritische Schlange gebildet hat.
     *
     * @return {@code true}, falls sich für diese Abbiegebeziehung eine
     * kritische Schlange gebildet hat
     */
    public final boolean isCriticalQueue() {
        if (!this.queueMaxHistory.isFull() || !this.queueAvgHistory.isFull()
                || this.queueMaxHistory.size() != this.queueAvgHistory.size()) {
            // Zu wenig Daten
            return false;
        }

        boolean criticalQueue = true;
        for (int i = 0; i < this.queueMaxHistory.size(); i++) {
            if (this.queueAvgHistory.get(i) < 0.65 * this.queueMaxHistory.get(i)) {
                criticalQueue = false;
            }
        }
        return criticalQueue;
    }

    @Override
    public final String printStatisticalData() {
        final String linesep = System.getProperty("line.separator");
        String output = "Statistical data for turning " + id + " from section " + sectionIn.getId() + " to "
                + sectionOut.getId() + linesep;

        if (statisticalValue != null) {
            output = output.concat(statisticalValue.toString());
        } else {
            output = output.concat("No data available");
        }

        return output;
    }

    /**
     * @param firstLaneOrigin      Erste Spur der Quellsection, die zu diesem {@link Turning}
     *                             gehört
     * @param lastLaneOrigin       Letzte Spur der Quellsection, die zu diesem {@link Turning}
     *                             gehört
     * @param firstLaneDestination Erste Spur der Zielsection, die zu diesem {@link Turning}
     *                             gehört
     * @param lastLaneDestination  Letzte Spur der Zielsection, die zu diesem {@link Turning}
     *                             gehört
     */
    public final void setLanes(final int firstLaneOrigin, final int lastLaneOrigin, final int firstLaneDestination,
                               final int lastLaneDestination) {
        this.firstLaneOrigin = firstLaneOrigin;
        this.lastLaneOrigin = lastLaneOrigin;
        this.firstLaneDestination = firstLaneDestination;
        this.lastLaneDestination = lastLaneDestination;
    }

    @Override
    public final String toString() {
        final String linesep = System.getProperty("line.separator");
        String output = "Turning " + id + ": Section " + sectionIn.getId() + " (Lanes " + firstLaneOrigin + " to "
                + lastLaneOrigin + ") to section " + sectionOut.getId() + " (Lanes " + firstLaneDestination + " to "
                + lastLaneDestination + "). Traffic type: " + trafficType.name();

        if (!this.detectors.isEmpty()) {
            output = output.concat(linesep + "Relevant Detector(s): ");
            for (Detector detector : this.detectors) {
                output = output.concat(detector.getId() + " ");
            }
        }

        return output.concat(linesep);
    }
}