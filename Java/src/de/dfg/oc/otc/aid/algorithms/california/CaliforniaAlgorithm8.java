package de.dfg.oc.otc.aid.algorithms.california;

import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.aid.algorithms.AbstractAIDAlgorithm;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;
import de.dfg.oc.otc.aid.disturbance.DisturbanceManager;

import java.util.HashMap;
import java.util.Map;

/**
 * The california algorithm variant 8.
 */
public class CaliforniaAlgorithm8 extends AbstractAIDAlgorithm {
    /**
     * Threshold for the difference between the occupancies of two succeeding
     * detectors (OCCDF).
     */
    private float THRESHOLD1 = 8;
    /**
     * Threshold for the relative temporal difference in downstream occupancy
     * (DOCCTD).
     */
    private float THRESHOLD2 = -0.4f;
    /**
     * Threshold for the relative difference between the occupancies of two
     * succeeding detectors (OCCRDF).
     */
    private float THRESHOLD3 = 0.5f;
    /**
     * Threshold for the occupancy of the 2nd detector (DOCC).
     */
    private float THRESHOLD4 = 0.3f;
    /**
     * Threshold for the occupancy of the 2nd detector (DOCC).
     */
    private float THRESHOLD5 = 30f;

    @Override
    public String getName() {
        return "California 8";
    }

    /**
     * Prepares the algorithm execution. Thereby the algorithm is executed for
     * each detector pair combination.
     *
     * @param time of the algorithm execution
     */
    protected void prepareAndRunAlgorithm(float time) {
        // Only execute the algorithm if past data is available
        if (pastOccupancyValues.isFull()) {
            String previousDetectorPairID = null;

            // Loop through each detector pair and execute the algorithm
            for (AbstractDetectorGroup detectorPair : monitoringZone.getMonitoredDetectorPairs()) {
                String detectorPairID = detectorPair.getId();
                // Skip first detector since it has no predecessor
                if (previousDetectorPairID != null) {
                    // detectorPairID = upstream
                    // previousDetectorPairID = downstream
                    String pairIdentifier = getPairIdentifier(detectorPairID, previousDetectorPairID);

                    // Set state to incident free if it does not exist yet
                    algorithmStates.putIfAbsent(pairIdentifier, CaliforniaState8.INCIDENT_FREE.ordinal());

                    // Actual execution of the algorithm
                    executeAlgorithm(time, pairIdentifier, detectorPairID, previousDetectorPairID);
                }

                previousDetectorPairID = detectorPairID;
            }
        }
    }

    /**
     * Iterate over decision tree of the algorithm and find current state of
     * monitored road.
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

        // Relative temporal difference in downstream occupancy
        float DOCCTD = 0;
        if (OCC_iplus1_tminus2 > 0) {
            DOCCTD = (OCC_iplus1_tminus2 - OCC_iplus1_t) / OCC_iplus1_tminus2;
        }

        int currentState = algorithmStates.get(pairIdentifier);

        log(time + ";" + pairIdentifier + ";" + OCCDF + ";" + OCCRDF + ";" + DOCCTD + ";" + OCC_iplus1_t + ";"
                + CaliforniaState8.values()[currentState]);

		/*
         * California 8 Decision Tree
		 */
        if (currentState >= CaliforniaState8.TENTATIVE_INCIDENT.ordinal()) {
            if (OCCRDF >= this.THRESHOLD3) {
                if (currentState >= CaliforniaState8.INCIDENT_CONFIRMED.ordinal()) {
                    incidentState(pairIdentifier, CaliforniaState8.INCIDENT_CONTINUING);
                } else {
                    incidentConfirmed(pairIdentifier);
                }
            } else {
                if (DOCCTD >= this.THRESHOLD2) {
                    incidentFree(time, pairIdentifier);
                } else {
                    if (OCC_iplus1_t >= this.THRESHOLD5) {
                        if (currentState >= CaliforniaState8.INCIDENT_CONFIRMED.ordinal()) {
                            incidentFree(time, pairIdentifier);
                        } else {
                            incidentState(pairIdentifier, CaliforniaState8.COMPRESSION_WAVE_1);
                        }
                    } else {
                        incidentFree(time, pairIdentifier);
                    }
                }

            }
        } else {
            if (OCC_iplus1_t >= this.THRESHOLD5) {
                if (DOCCTD >= this.THRESHOLD2) {
                    if (currentState >= CaliforniaState8.COMPRESSION_WAVE_1.ordinal()) {
                        if (currentState >= CaliforniaState8.COMPRESSION_WAVE_2.ordinal()) {
                            if (currentState >= CaliforniaState8.COMPRESSION_WAVE_3.ordinal()) {
                                if (currentState >= CaliforniaState8.COMPRESSION_WAVE_4.ordinal()) {
                                    if (currentState >= CaliforniaState8.COMPRESSION_WAVE_5.ordinal()) {
                                        incidentFree(time, pairIdentifier);
                                    } else {
                                        incidentState(pairIdentifier, CaliforniaState8.COMPRESSION_WAVE_5);
                                    }
                                } else {
                                    incidentState(pairIdentifier, CaliforniaState8.COMPRESSION_WAVE_4);
                                }
                            } else {
                                incidentState(pairIdentifier, CaliforniaState8.COMPRESSION_WAVE_3);
                            }
                        } else {
                            incidentState(pairIdentifier, CaliforniaState8.COMPRESSION_WAVE_2);
                        }
                    } else {
                        incidentFree(time, pairIdentifier);
                    }
                } else {
                    incidentState(pairIdentifier, CaliforniaState8.COMPRESSION_WAVE_1);
                }
            } else {
                if (currentState >= CaliforniaState8.COMPRESSION_WAVE_1.ordinal()) {
                    if (currentState >= CaliforniaState8.COMPRESSION_WAVE_2.ordinal()) {
                        if (currentState >= CaliforniaState8.COMPRESSION_WAVE_3.ordinal()) {
                            if (currentState >= CaliforniaState8.COMPRESSION_WAVE_4.ordinal()) {
                                if (currentState >= CaliforniaState8.COMPRESSION_WAVE_5.ordinal()) {
                                    incidentFree(time, pairIdentifier);
                                } else {
                                    incidentState(pairIdentifier, CaliforniaState8.COMPRESSION_WAVE_5);
                                }
                            } else {
                                incidentState(pairIdentifier, CaliforniaState8.COMPRESSION_WAVE_4);
                            }
                        } else {
                            incidentState(pairIdentifier, CaliforniaState8.COMPRESSION_WAVE_3);
                        }
                    } else {
                        incidentState(pairIdentifier, CaliforniaState8.COMPRESSION_WAVE_2);
                    }
                } else {
                    // CA 7 routine
                    if (OCCDF >= this.THRESHOLD1 && OCCRDF >= this.THRESHOLD3 && DOCCTD >= this.THRESHOLD4) {
                        incidentTentative(time, pairIdentifier);
                    } else {
                        incidentFree(time, pairIdentifier);
                    }
                }
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
        algorithmStates.put(pairIdentifier, CaliforniaState8.TENTATIVE_INCIDENT.ordinal());
        tentativeIncidents.put(pairIdentifier, createIncident(time, pairIdentifier, false));
    }

    /**
     * Sets the algorithm state to confirmed and reports a new incident for a
     * given detector pair combination.
     *
     * @param pairIdentifier String combining the ids of two detector pairs
     */
    private void incidentConfirmed(String pairIdentifier) {
        algorithmStates.put(pairIdentifier, CaliforniaState8.INCIDENT_CONFIRMED.ordinal());

        // Throw incident
        Incident incident = tentativeIncidents.get(pairIdentifier);
        if (incident != null) {
            incident.setConfirmed(true);
            DisturbanceManager.getInstance().confirmDisturbance(incident, getNode());
            reportIncident(incident);
        }
    }

    /**
     * Sets the algorithm state the given state.
     *
     * @param pairIdentifier String combining the ids of two detector pairs
     * @param state          of the algorithm for a given pair
     */
    private void incidentState(String pairIdentifier, CaliforniaState8 state) {
        algorithmStates.put(pairIdentifier, state.ordinal());
    }

    /**
     * Sets the algorithm state to free and removes tentative incidents for a
     * given detector pair combination.
     *
     * @param time           of the algorithm execution
     * @param pairIdentifier String combining the ids of two detector pairs
     */
    protected void incidentFree(float time, String pairIdentifier) {
        algorithmStates.put(pairIdentifier, CaliforniaState8.INCIDENT_FREE.ordinal());
        super.incidentFree(time, pairIdentifier);
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("THRESHOLD1", this.THRESHOLD1);
        parameters.put("THRESHOLD2", this.THRESHOLD2);
        parameters.put("THRESHOLD3", this.THRESHOLD3);
        parameters.put("THRESHOLD4", this.THRESHOLD4);
        parameters.put("THRESHOLD5", this.THRESHOLD5);
        return parameters;
    }

    @Override
    public final int getRequiredDetectorPairCount() {
        return 2;
    }
    
    @Override
	public boolean isStateMappedToIncident(int state) {
        return state == CaliforniaState8.INCIDENT_CONFIRMED.ordinal() || state == CaliforniaState8.INCIDENT_CONTINUING.ordinal();
	}

    /**
     * States used by the california 8 algorithm.
     */
    private enum CaliforniaState8 {
        /**
         * The algorithm has found no incident.
         */
        INCIDENT_FREE,
        /**
         * Compression wave downstream 1 minute ago.
         */
        COMPRESSION_WAVE_1,
        /**
         * Compression wave downstream 2 minutes ago.
         */
        COMPRESSION_WAVE_2,
        /**
         * Compression wave downstream 3 minutes ago.
         */
        COMPRESSION_WAVE_3,
        /**
         * Compression wave downstream 4 minutes ago.
         */
        COMPRESSION_WAVE_4,
        /**
         * Compression wave downstream 5 minutes ago.
         */
        COMPRESSION_WAVE_5,
        /**
         * The algorithm has found a possible incident and will do further checks to
         * confirm or refute it.
         */
        TENTATIVE_INCIDENT,
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
