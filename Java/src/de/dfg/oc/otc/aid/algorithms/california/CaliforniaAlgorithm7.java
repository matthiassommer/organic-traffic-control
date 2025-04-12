package de.dfg.oc.otc.aid.algorithms.california;

import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.aid.algorithms.AbstractAIDAlgorithm;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;
import de.dfg.oc.otc.aid.disturbance.DisturbanceManager;

import java.util.HashMap;
import java.util.Map;

/**
 * The modified California algorithm variant 7 (additional incident continuing state)
 * <p>
 * ALTERNATIVE APPROACHES TO CONDITION MONITORING IN FREEWAY MANAGEMENT SYSTEMS, Brian L. Smith, 2002
 * https://www.researchgate.net/figure/242081661_fig2_Figure-10-Decision-Tree-for-Modified-California-Algorithm-7
 */
public class CaliforniaAlgorithm7 extends AbstractAIDAlgorithm {
    /**
     * Threshold for the difference between the occupancies of two succeeding
     * detectors (OCCDF).
     */
    private float THRESHOLD1 = 8;
    /**
     * Threshold for the relative difference between the occupancies of two
     * succeeding detectors (OCCRDF).
     */
    private float THRESHOLD2 = 0.5f;
    /**
     * Threshold for the relative temporal difference in downstream occupancy
     * (DOCCTD).
     */
    private float THRESHOLD3 = 0.3f;

    @Override
    public String getName() {
        return "California 7";
    }

    /**
     * Prepares the algorithm execution. Thereby the algorithm is executed for
     * each detector pair combination.
     *
     * @param time of the algorithm execution
     */
    protected void prepareAndRunAlgorithm(float time) {
        String previousDetectorPairID = null;

        // Loop through each detector pair and execute algorithm checks
        for (AbstractDetectorGroup detectorPair : monitoringZone.getMonitoredDetectorPairs()) {
            String detectorPairID = detectorPair.getId();
            // Skip first detector since it has no predecessor
            if (previousDetectorPairID != null) {
                // detectorPairID = upstream
                // previousDetectorPairID = downstream
                String pairIdentifier = getPairIdentifier(detectorPairID, previousDetectorPairID);

                // Set state to incident free if it does not exist yet
                algorithmStates.putIfAbsent(pairIdentifier, CaliforniaState7.INCIDENT_FREE.ordinal());

                executeAlgorithm(time, pairIdentifier, detectorPairID, previousDetectorPairID);
            }

            previousDetectorPairID = detectorPairID;
        }
    }

    /**
     * Actual execution of the algorithm.
     *
     * @param time                 of the algorithm execution
     * @param pairIdentifier       String combining the ids of two detector pairs
     * @param upstreamDetectorID   Id of the upstream detector pair
     * @param downstreamDetectorID Id of the downstream detector pair
     */
    private void executeAlgorithm(float time, String pairIdentifier, String upstreamDetectorID, String downstreamDetectorID) {
        algorithmApplied();

        // First element in the queue is the oldest, last is the newest
        float OCC_i_t = pastOccupancyValues.getLast().get(upstreamDetectorID);
        float OCC_iplus1_t = pastOccupancyValues.getLast().get(downstreamDetectorID);
        float OCC_iplus1_tminus2 = pastOccupancyValues.getFirst().get(downstreamDetectorID);

        // Spatial difference in occupancy
        float OCCDF = OCC_i_t - OCC_iplus1_t;

        // Relative spatial difference in occupancies
        float OCCRDF = 0;
        if (OCC_i_t > 0) {
            OCCRDF = OCCDF / OCC_i_t;
        }

        float DOCCTD = 0;
        if (OCC_iplus1_tminus2 > 0) {
            DOCCTD = (OCC_iplus1_tminus2 - OCC_iplus1_t) / OCC_iplus1_tminus2;
        }

        int currentState = algorithmStates.get(pairIdentifier);

        log(time + ";" + pairIdentifier + ";" + OCCDF + ";" + OCCRDF + ";" + OCC_iplus1_t + ";"
                + CaliforniaState7.values()[currentState]);

		/*
         * Modified California 7 Decision Tree
		 */
        if (currentState >= CaliforniaState7.INCIDENT_TENTATIVE.ordinal()) {
            if (currentState >= CaliforniaState7.INCIDENT_CONFIRMED.ordinal()) {
                if (OCCRDF >= this.THRESHOLD2) {
                    incidentContinuing(pairIdentifier);
                } else {
                    incidentFree(time, pairIdentifier);
                }
            } else {
                if (OCCRDF >= this.THRESHOLD2) {
                    incidentConfirmed(pairIdentifier);
                } else {
                    incidentFree(time, pairIdentifier);
                }
            }
        } else {
            if (OCCDF >= this.THRESHOLD1 && OCCRDF >= this.THRESHOLD2 && DOCCTD >= this.THRESHOLD3) {
                incidentTentative(time, pairIdentifier);
            } else {
                incidentFree(time, pairIdentifier);
            }
        }
    }

    /**
     * Sets the algorithm state to tentative and creates an incident for a given
     * detector pair combination.
     *
     * @param time           of the algorithm execution
     * @param pairIdentifier String combining the ids of two detector pairs
     */
    private void incidentTentative(float time, String pairIdentifier) {
        algorithmStates.put(pairIdentifier, CaliforniaState7.INCIDENT_TENTATIVE.ordinal());
        tentativeIncidents.put(pairIdentifier, createIncident(time, pairIdentifier, false));
    }

    /**
     * Sets the algorithm state to confirmed and reports a new incident for a
     * given detector pair combination.
     *
     * @param pairIdentifier String combining the ids of two detector pairs
     */
    private void incidentConfirmed(String pairIdentifier) {
        algorithmStates.put(pairIdentifier, CaliforniaState7.INCIDENT_CONFIRMED.ordinal());

        // throw incident
        Incident incident = tentativeIncidents.get(pairIdentifier);
        if (incident != null) {
            incident.setConfirmed(true);
            DisturbanceManager.getInstance().confirmDisturbance(incident, getNode());
            reportIncident(incident);
        }
    }

    /**
     * Sets the algorithm state to continuing for a given detector pair
     * combination.
     *
     * @param pairIdentifier String combining the ids of two detector pairs
     */
    private void incidentContinuing(String pairIdentifier) {
        algorithmStates.put(pairIdentifier, CaliforniaState7.INCIDENT_CONTINUING.ordinal());
    }

    /**
     * Sets the algorithm state to free and removes tentative incidents for a
     * given detector pair combination.
     *
     * @param time           of the algorithm execution
     * @param pairIdentifier String combining the ids of two detector pairs
     */
    protected void incidentFree(float time, String pairIdentifier) {
        algorithmStates.put(pairIdentifier, CaliforniaState7.INCIDENT_FREE.ordinal());
        super.incidentFree(time, pairIdentifier);
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("THRESHOLD1", this.THRESHOLD1);
        parameters.put("THRESHOLD2", this.THRESHOLD2);
        parameters.put("THRESHOLD3", this.THRESHOLD3);
        return parameters;
    }

    @Override
    public int getRequiredDetectorPairCount() {
        return 2;
    }

    @Override
    public boolean isStateMappedToIncident(int state) {
        return state == CaliforniaState7.INCIDENT_CONFIRMED.ordinal() || state == CaliforniaState7.INCIDENT_CONTINUING.ordinal();
    }

    private enum CaliforniaState7 {
        /**
         * The algorithm has found no incident.
         */
        INCIDENT_FREE,
        /**
         * The algorithm has found a possible incident and will do further checks to
         * confirm or refute it.
         */
        INCIDENT_TENTATIVE,
        /**
         * The algorithm has confirmed a tentative incident.
         */
        INCIDENT_CONFIRMED,
        /**
         * The confirmed incident is still continuing.
         */
        INCIDENT_CONTINUING
    }
}
