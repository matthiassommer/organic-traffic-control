package de.dfg.oc.otc.aid.disturbance;

/**
 * Defines the type of the disturbance.
 */
public enum DisturbanceType {
    /**
     * There was an accident.
     */
    INCIDENT,
    /**
     * A lane of the section is closed or blocked.
     */
    LANE_CLOSURE,
    /**
     * The speed of the section has changed.
     */
    SPEED_CHANGE
}
