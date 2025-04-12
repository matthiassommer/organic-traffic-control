package de.dfg.oc.otc.aid.algorithms.apid;

import de.dfg.oc.otc.aid.AimsunPolicyStatus;
import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.aid.algorithms.AbstractAIDAlgorithm;
import de.dfg.oc.otc.aid.disturbance.DisturbanceManager;
import de.dfg.oc.otc.aid.evaluation.CongestionMetrics;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;
import de.dfg.oc.otc.tools.FileUtilities;
import de.dfg.oc.otc.tools.LimitedQueue;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the all purpose incident detection algorithm.
 * see Deniz - OVERVIEW TO SOME INCIDENT DETECTION ALGORITHMS, p.278, table 2
 * Incident detection algorithms of COMPASS
 */
public class APIDAlgorithm extends AbstractAIDAlgorithm {
    /**
     * Contains the latest occupancy and speed values of all observed detector
     * pairs. First element in the queue is the oldest, last is the newest
     */
    private final LimitedQueue<Map<String, APIDDetectorValue>> pastDetectorValues;
    /**
     * Algorithm states for each detector pair identifier.
     */
    protected final Map<String, APIDState> algorithmStates;
    /**
     * Start times of the persistence tests (0 if no test is started).
     */
    private final Map<String, Float> persistenceTestStartTime;
    /**
     * Start times of the compression wave tests (0 if no test is started).
     */
    private final Map<String, Float> compressionWaveTestStartTime;
    /**
     * Defines whether medium traffic detection should be enabled or not.
     */
    public boolean MEDIUM_TRAFFIC_DETECTION_ENABLED = true;
    /**
     * Defines whether compression wave tests should be enabled or not. Deniz, Compass = false
     */
    public boolean COMPRESSION_WAVE_TEST_ENABLED = true;
    /**
     * Defines whether persistence tests should be enabled or not.
     */
    public boolean PERSISTENCE_TEST_ENABLED = true;
    /**
     * Occupancy threshold for medium traffic tests. Deniz: 60
     */
    public float TH_MEDIUM_TRAFFIC = 60;
    /**
     * Occupancy threshold for incident clearance. Deniz: -0.4
     */
    public float TH_INC_CLR = -0.4f;
    /**
     * Occupancy threshold for persistence tests.
     */
    public float TH_PT = 0.1f;
    /**
     * Occupancy threshold 1 for compression wave test. Deniz: -1.3
     */
    public float TH_CW1 = -1.3f;
    /**
     * Occupancy threshold 2 for compression wave test. Deniz: -1.5
     */
    public float TH_CW2 = -1.5f;
    /**
     * Duration of persistence test in seconds.
     */
    public float PERSISTENCE_TEST_PERIOD = 300;
    /**
     * Duration of compression wave test in seconds.
     */
    public float COMPRESSION_WAVE_TEST_PERIOD = 300;
    /**
     * Threshold for the difference between the occupancies of two succeeding
     * detectors (OCCDF). Deniz: 10.2
     */
    public float TH_ID1 = 10.2f;
    /**
     * Threshold for the relative difference between the occupancies of two
     * succeeding detectors (OCCRDF). Deniz: 0.0, Compass: 0.4
     */
    public float TH_ID2 = 0.0f;
    /**
     * Threshold for the occupancy of the 2nd detector (DOCC). Deniz: 20.8, compass: 28.8
     */
    public float TH_ID3 = 20.8f;
    /**
     * Threshold for the relative difference between the occupancies of two
     * succeeding detectors for medium traffic (OCCRDF).
     */
    public float TH_MED_ID1 = 0.4f;
    /**
     * Threshold for the relative temporal difference in speed for medium
     * traffic (SPDTDF).
     */
    public float TH_MED_ID2 = 0.1f;
    /**
     * Contains the current occupancy and speed values of all observed detector
     * pairs. Key = Pair ID, Value = List of detector values measured in the current iteration.
     */
    private Map<String, APIDDetectorValue> currentDetectorValues;

    /**
     * Constructor, loads default algorithm parameter values.
     */
    public APIDAlgorithm() {
        this.currentDetectorValues = new HashMap<>();
        this.pastDetectorValues = new LimitedQueue<>(2);
        this.algorithmStates = new HashMap<>();
        this.persistenceTestStartTime = new HashMap<>();
        this.compressionWaveTestStartTime = new HashMap<>();
    }

    @Override
    public String getName() {
        return "APID Algorithm";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void newDetectorData(AbstractDetectorGroup detectorGroup, DetectorDataValue detectorValue) {
        float time = detectorValue.getTime();
        if (time > this.warmupTime) {
            String pairID = detectorGroup.getId();
            // Create field for new detector pair values
            this.currentDetectorValues.putIfAbsent(pairID, new APIDDetectorValue());

            // Add the detector values from the detector pair
            this.currentDetectorValues.get(pairID).addOccupancyValue(detectorValue.getValues()[DetectorCapabilities.OCCUPANCY]);
            this.currentDetectorValues.get(pairID).addSpeedValue(detectorValue.getValues()[DetectorCapabilities.SPEED]);

            // At the end of each iteration execute algorithm
            boolean enoughMeasurements = this.currentDetectorValues.size() == this.observedDetectorCount;
            boolean nextIteration = time % this.executionInterval + this.simulationStepSize >= this.executionInterval;
            if (nextIteration && enoughMeasurements) {
                // Add the values from the current iteration
                this.pastDetectorValues.add(currentDetectorValues);
                prepareAndRunAlgorithm(time);
                this.currentDetectorValues = new HashMap<>();
            }
        }
    }

    /**
     * Prepares the algorithm execution. Thereby the algorithm is executed for
     * each detector pair combination.
     *
     * @param time of algorithm execution
     */
    protected void prepareAndRunAlgorithm(float time) {
        // Only execute the algorithm if past data is available
        if (pastDetectorValues.isFull()) {
            String previousDetectorPairID = null;

            // Loop through each detector pair and execute the algorithm
            for (AbstractDetectorGroup detectorPair : monitoringZone.getMonitoredDetectorPairs()) {
                String detectorPairID = detectorPair.getId();
                // Skip first detector since it has no predecessor
                if (previousDetectorPairID != null) {
                    // detectorPairID = upstream
                    // previousDetectorPairID = downstream
                    String pairIdentifier = getPairIdentifier(detectorPairID, previousDetectorPairID);

                    initializeVariables(pairIdentifier);
                    executeAlgorithm(time, pairIdentifier, detectorPairID, previousDetectorPairID);
                }

                previousDetectorPairID = detectorPairID;
            }
        }
    }

    /**
     * Initializes the required algorithm variables.
     *
     * @param pairIdentifier String combining the ids of two detector pairs
     */
    private void initializeVariables(String pairIdentifier) {
        // Set state to incident free if it does not exist yet
        algorithmStates.putIfAbsent(pairIdentifier, APIDState.INCIDENT_FREE);
        persistenceTestStartTime.putIfAbsent(pairIdentifier, 0f);
        compressionWaveTestStartTime.putIfAbsent(pairIdentifier, 0f);
    }

    /**
     * Actual execution of the APID algorithm.
     *
     * @param time                 of the algorithm execution
     * @param pairIdentifier       String combining the ids of two detector pairs
     * @param upstreamDetectorID   Id of the upstream detector pair
     * @param downstreamDetectorID Id of the downstream detector pair
     */
    protected void executeAlgorithm(float time, String pairIdentifier, String upstreamDetectorID, String downstreamDetectorID) {
        algorithmApplied();

        Map<String, APIDDetectorValue> pastValues = pastDetectorValues.getFirst();
        Map<String, APIDDetectorValue> currentValues = pastDetectorValues.getLast();

        // Occupancy values
        float OCC_i_t = currentValues.get(upstreamDetectorID).getAverageOccupancy();
        float OCC_iplus1_t = currentValues.get(downstreamDetectorID).getAverageOccupancy();
        float OCC_iplus1_tminus2 = pastValues.get(downstreamDetectorID).getAverageOccupancy();

        // Speed values
        float SPD_i_t = currentValues.get(upstreamDetectorID).getAverageSpeed();
        float SPD_i_tminus2 = pastValues.get(upstreamDetectorID).getAverageSpeed();

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

        // Downstream occupancy
        float DOCC = OCC_iplus1_t;

        // Relative temporal difference in Speed
        float SPDTDF = 0;
        if (SPD_i_tminus2 > 0) {
            SPDTDF = (SPD_i_tminus2 - SPD_i_t) / SPD_i_tminus2;
        }

        APIDState currentState = algorithmStates.get(pairIdentifier);

        log(time + ";" + pairIdentifier + ";" + OCCDF + ";" + OCCRDF + ";" + DOCCTD + ";" + DOCC + ";" + SPDTDF + ";" + currentState);

        executeDecisionTree(time, pairIdentifier, currentState, OCC_i_t, DOCCTD, OCCDF, OCCRDF, DOCC, SPDTDF);
    }

    private void executeDecisionTree(float time, String pairIdentifier, APIDState currentState, float OCC_i_t, float DOCCTD, float OCCDF, float OCCRDF, float DOCC, float SPDTDF) {
        if (currentState != APIDState.INCIDENT_TENTATIVE) {
            if (currentState != APIDState.INCIDENT_CONFIRMED) {
                // Incident free
                if (MEDIUM_TRAFFIC_DETECTION_ENABLED) {
                    // Medium volume traffic flow check required
                    if (OCC_i_t > TH_MEDIUM_TRAFFIC) {
                        // High volume traffic
                        generalIncidentDetection(time, pairIdentifier, DOCCTD, OCCDF, OCCRDF, DOCC);
                    } else {
                        // Medium volume traffic
                        mediumTrafficIncidentDetection(time, pairIdentifier, OCCRDF, SPDTDF);
                    }
                } else {
                    // Low volume traffic: No traffic flow check required
                    generalIncidentDetection(time, pairIdentifier, DOCCTD, OCCDF, OCCRDF, DOCC);
                }
            } else {
                if (OCCRDF < TH_INC_CLR) {
                    incidentFree(time, pairIdentifier);
                }
            }
        } else {
            // Tentative Incident
            if (OCCRDF > TH_PT) {
                // Tentative incident valid, PERSISTENCE_TEST_PERIOD expired
                boolean persistencePeriodExpired = time > persistenceTestStartTime.get(pairIdentifier) + PERSISTENCE_TEST_PERIOD;
                if (persistenceTestStartTime.get(pairIdentifier) > 0 && persistencePeriodExpired) {
                  incidentConfirmed(time, pairIdentifier);
                }
            } else {
                // Tentative incident invalid, incident free
                incidentFree(time, pairIdentifier);

                // Reset persistence test
                persistenceTestStartTime.put(pairIdentifier, 0f);

                if (COMPRESSION_WAVE_TEST_ENABLED) {
                    // Test for compression wave
                    if (DOCC > TH_CW1 && DOCCTD <= TH_CW2) {
                        // Compression wave detected
                        compressionWaveTestStartTime.put(pairIdentifier, time);
                    }
                }
            }
        }
    }

    /**
     * General incident detection method of the APID algorithm.
     *
     * @param time           of the algorithm execution
     * @param pairIdentifier String combining the ids of two detector pairs
     * @param DOCCTD         Temporal difference in downstream occupancy
     * @param OCCDF          Spatial difference in occupancy
     * @param OCCRDF         Relative spatial difference in occupancies
     * @param DOCC           Downstream occupancy
     */
    private void generalIncidentDetection(float time, String pairIdentifier, float DOCCTD, float OCCDF, float OCCRDF, float DOCC) {
        if (COMPRESSION_WAVE_TEST_ENABLED) {
            // Compression wave test
            if (DOCCTD > TH_CW1) {
                if (DOCCTD > TH_CW2) {
                    // No compression wave detected
                    if (compressionWaveTestStartTime.get(pairIdentifier) == 0) {
                        incidentFree(time, pairIdentifier);
                    } else {
                        // Compression wave detected before
                        updateCompressionWaveHistory(time, pairIdentifier);
                    }
                } else {
                    // Compression wave at the start of a new CW_TEST_PERIOD
                    compressionWaveTestStartTime.put(pairIdentifier, time);
                }
            } else {
                // DOCC low, check if due to compression wave
                if (compressionWaveTestStartTime.get(pairIdentifier) == 0) {
                    // Perform incident detection
                    californiaIncidentDetection(time, pairIdentifier, OCCDF, OCCRDF, DOCC);
                } else {
                    // Compression wave detected before
                    updateCompressionWaveHistory(time, pairIdentifier);
                }
            }
        } else {
            // No compression wave test required, perform incident detection
            californiaIncidentDetection(time, pairIdentifier, OCCDF, OCCRDF, DOCC);
        }
    }

    /**
     * Medium traffic incident detection method of the APID algorithm.
     *
     * @param time           of the algorithm execution
     * @param pairIdentifier String combining the ids of two detector pairs
     * @param OCCRDF         Relative spatial difference in occupancies
     * @param SPDTDF         Relative temporal difference in Speed
     */
    private void mediumTrafficIncidentDetection(float time, String pairIdentifier, float OCCRDF, float SPDTDF) {
        updateCompressionWaveHistory(time, pairIdentifier);

        if (MEDIUM_TRAFFIC_DETECTION_ENABLED) {
            // Medium traffic incident detection
            if (OCCRDF > TH_MED_ID1 && SPDTDF > TH_MED_ID2) {
                // Persistence test required?
                if (PERSISTENCE_TEST_ENABLED) {
                    // Persistence test required, declare tentative incident
                    incidentTentative(time, pairIdentifier);
                } else {
                    incidentConfirmed(time, pairIdentifier);
                }
            } else {
                incidentFree(time, pairIdentifier);
            }
        } else {
            incidentFree(time, pairIdentifier);
        }
    }

    /**
     * Updates the compression wave history.
     *
     * @param time           of the algorithm execution
     * @param pairIdentifier String combining the ids of two detector pairs
     */
    private void updateCompressionWaveHistory(float time, String pairIdentifier) {
        // Compression wave expired
        if (compressionWaveTestStartTime.get(pairIdentifier) > 0
                && time > compressionWaveTestStartTime.get(pairIdentifier) + COMPRESSION_WAVE_TEST_PERIOD) {
            // No compression wave present
            compressionWaveTestStartTime.put(pairIdentifier, 0f);
        }
    }

    /**
     * Standard California 7 incident detection method.
     *
     * @param time           of the algorithm execution
     * @param pairIdentifier String combining the ids of two detector pairs
     * @param OCCDF          Spatial difference in occupancy
     * @param OCCRDF         Relative spatial difference in occupancies
     * @param DOCC           Downstream occupancy
     */
    private void californiaIncidentDetection(float time, String pairIdentifier, float OCCDF, float OCCRDF, float DOCC) {
        if (OCCDF > TH_ID1 && OCCRDF > TH_ID2 && DOCC < TH_ID3) {
            if (PERSISTENCE_TEST_ENABLED) {
                incidentTentative(time, pairIdentifier);
            } else {
                incidentConfirmed(time, pairIdentifier);
            }
        } else {
            incidentFree(time, pairIdentifier);
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
        // Start persistence test
        persistenceTestStartTime.put(pairIdentifier, time);

        algorithmStates.put(pairIdentifier, APIDState.INCIDENT_TENTATIVE);
        tentativeIncidents.put(pairIdentifier, createIncident(time, pairIdentifier, false));
    }

    /**
     * Sets the algorithm state to confirmed and reports a new incident for a
     * given detector pair combination.
     *
     * @param time           of the algorithm execution
     * @param pairIdentifier String combining the ids of two detector pairs
     */
    protected void incidentConfirmed(float time, String pairIdentifier) {
        algorithmStates.put(pairIdentifier, APIDState.INCIDENT_CONFIRMED);

        Incident incident;
        if (PERSISTENCE_TEST_ENABLED) {
            incident = tentativeIncidents.get(pairIdentifier);
            incident.setConfirmed(true);
            DisturbanceManager.getInstance().confirmDisturbance(incident, getNode());
        } else {
            incident = createIncident(time, pairIdentifier, true);
            tentativeIncidents.put(pairIdentifier, incident);
        }

        reportIncident(incident);
    }

    /**
     * Sets the algorithm state to free and removes tentative incidents for a
     * given detector pair combination.
     *
     * @param time           of the algorithm execution
     * @param pairIdentifier String combining the ids of two detector pairs
     */
    protected void incidentFree(float time, String pairIdentifier) {
        algorithmStates.put(pairIdentifier, APIDState.INCIDENT_FREE);
        super.incidentFree(time, pairIdentifier);
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("MEDIUM_TRAFFIC_DETECTION_ENABLED", this.MEDIUM_TRAFFIC_DETECTION_ENABLED);
        parameters.put("COMPRESSION_WAVE_TEST_ENABLED", this.COMPRESSION_WAVE_TEST_ENABLED);
        parameters.put("PERSISTENCE_TEST_ENABLED", this.PERSISTENCE_TEST_ENABLED);
        parameters.put("TH_MEDIUM_TRAFFIC", this.TH_MEDIUM_TRAFFIC);
        parameters.put("TH_INC_CLR", this.TH_INC_CLR);
        parameters.put("TH_PT", this.TH_PT);
        parameters.put("TH_CW1", this.TH_CW1);
        parameters.put("TH_CW2", this.TH_CW2);
        parameters.put("PERSISTENCE_TEST_PERIOD", this.PERSISTENCE_TEST_PERIOD);
        parameters.put("COMPRESSION_WAVE_TEST_PERIOD", this.COMPRESSION_WAVE_TEST_PERIOD);
        parameters.put("TH_ID1", this.TH_ID1);
        parameters.put("TH_ID2", this.TH_ID2);
        parameters.put("TH_ID3", this.TH_ID3);
        parameters.put("TH_MED_ID1", this.TH_MED_ID1);
        parameters.put("TH_MED_ID2", this.TH_MED_ID2);

        return parameters;
    }

    @Override
    public int getRequiredDetectorPairCount() {
        return 2;
    }

    @Override
    public boolean isStateMappedToIncident(int state) {
        return state == APIDState.INCIDENT_CONFIRMED.ordinal();
    }

    /**
     * States used by the APID algorithm.
     */
    protected enum APIDState {
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
        INCIDENT_CONFIRMED
    }
}
