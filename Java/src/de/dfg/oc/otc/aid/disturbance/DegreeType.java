package de.dfg.oc.otc.aid.disturbance;

/**
 * Enum specifies the different available degree calculation types used to measure the influence of the disturbance.
 */
enum DegreeType {
    /*
     *  No Calculation (i.e. degree = 0).
     */
    NONE,
    /*
     *  Static degree (e.g. degree = 0.5).
     */
    STATIC,
    /*
     *  Calculation with average Speed.
     */
    SPEED,
    /*
     *  Calculation with Occupancy.
     */
    OCCUPANCY,
    /*
     *  Combined average Speed and Occupancy.
     */
    SPEEDOCCUPANCY
}
