package de.dfg.oc.otc.manager.aimsun;

/**
 * Der Traffic Type unterscheidet die verschiedenen Verkehrsformen:
 * <ul>
 * <li>PUBLIC_TRANSPORT: öffentlicher Personennahverkehr</li>
 * <li>INDIVIDUAL_TRAFFIC: Individualverkehr (Kraftfahrzeuge, sofern nicht
 * ausschließlich ÖV)</li>
 * <li>PEDESTRIANS: Fussgänger</li>
 * <li>CYCLISTS: Fahrradfahrer</li>
 * <li>ALL: alles zusammen</li>
 * </ul>.
 *
 * @author rochner
 */
public enum TrafficType {
    ALL, CYCLISTS, INDIVIDUAL_TRAFFIC, PEDESTRIANS, PUBLIC_TRANSPORT, UNDEFINED
}
