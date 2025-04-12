package de.dfg.oc.otc.layer1.observer;

import de.dfg.oc.otc.layer1.observer.Layer1Observer.DataSource;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.TrafficType;

/**
 * Retrieve statistical information about a node like LOS and average flow.
 */
public abstract class PerformanceAnalyser {
    /**
     * Gibt die Bewertung eines Knotens zur�ck. Intervall wird auf
     * evaluationInterval gesetzt. Wenn der Knoten durch OTC gesteuert ist und
     * seit dem letzten Wechsel des TLC weniger Zeit vergangen ist als
     * evaluationInterval lang ist, wird das Intervall auf diesen Zeitraum
     * verk�rzt, um die Bewertung auf den Zeitraum zu beschr�nken, den der
     * aktuelle TLC beeinflusst hat.
     *
     * @param node
     * @param source      Datenquelle: Datasource.DETECTOR oder Datasource.STATISTICS
     * @param attribute   Das Attribut, das als Basis f�r die Bewertung dienen soll.
     * @param trafficType Der TrafficType, f�r den die Bewertung erfolgen soll.
     * @param setTimer    Gibt an, ob der Timer f�r die n�chste Evaluation gesetzt
     *                    werden soll (true) oder nicht (false). Wichtig, damit Abfragen
     *                    zur Aktualisierung der GUI, von Logfiles o.�. den Ablauf nicht
     *                    st�ren.
     * @return Bewertung gemäß Parametern.
     */
    public static float getEvaluationForNode(final OTCNode node, final DataSource source, final Attribute attribute,
                                             final TrafficType trafficType, final boolean setTimer) {
        float interval = node.getEvaluationInterval();

        // Anpassen Intervall an Zeit, die aktueller TLC aktiv war.
        if (node.getJunction().isControlled()) {
            final float timeSinceLastChange = OTCManager.getInstance().getTime() - node.getTimeLastTLCChange();

            if (timeSinceLastChange < interval) {
                interval = timeSinceLastChange;
            }
        }

        return getEvaluationForNode(node, source, attribute, trafficType, interval, setTimer);
    }

    /**
     * Gibt die Bewertung eines Knotens für ein bestimmtes Interval zur�ck.
     *
     * @param node
     * @param source      Datenquelle: Datasource.DETECTOR oder Datasource.STATISTICS
     * @param attribute   Das Attribut, das als Basis f�r die Bewertung dienen soll.
     * @param trafficType Der TrafficType, f�r den die Bewertung erfolgen soll.
     * @param interval    Das Intervall (in Sekunden) �ber das die Werte f�r die
     *                    Bewertung gemittelt werden sollen.
     * @param setTimer    Gibt an, ob der Timer f�r die n�chste Evaluation gesetzt
     *                    werden soll (true) oder nicht (false). Wichtig, damit Abfragen
     *                    zur Aktualisierung der GUI, von Logfiles o.�. den Ablauf nicht
     *                    st�ren.
     * @return Bewertung einer Kreuzung.
     */
    public static float getEvaluationForNode(final OTCNode node, final DataSource source, final Attribute attribute,
                                             final TrafficType trafficType, final float interval, final boolean setTimer) {
        final float currentTime = OTCManager.getInstance().getTime();

        if (setTimer && node.getJunction().isControlled()) {
            final float time = currentTime + node.getMinCyclesBetweenEvals()
                    * node.getJunction().getActiveTLC().getCycleTime();
            node.setTimeNextEvaluation(time);
        } else if (setTimer) {
            node.setTimeNextEvaluation(currentTime + node.getEvaluationInterval());
        }

        // Parameter f�r die Berechnung setzen
        if (attribute == Attribute.UTILISATION) {
            attribute.setParameter(node.getCapacity());
        }

        // Aufruf der eigentlichen Berechnung im Layer1Observer.
        return node.getLayer1Observer().getEvaluation(source, attribute, trafficType, interval);
    }
}
