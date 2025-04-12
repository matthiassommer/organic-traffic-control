package de.dfg.oc.otc.aid.combinationStrategies;

/**
 * The states the combined algorithm can obtain.
 *
 * @author Matthias Sommer.
 */
enum CombinationState {
    /**
     * The algorithm has found no incident.
     */
    INCIDENT_FREE,

    /**
     * The algorithm has found an incident.
     */
    INCIDENT_CONFIRMED,

    /**
     * The algorithm has found confirmed an already existing incident.
     */
    INCIDENT_CONTINUING
}
