package de.dfg.oc.otc.aid.algorithms;

import de.dfg.oc.otc.aid.AIDModule;
import de.dfg.oc.otc.aid.AIDMonitoringZone;
import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.aid.evaluation.AIDEvaluator;
import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup.Type;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;
import de.dfg.oc.otc.manager.aimsun.detectors.DetectorForkGroup;
import de.dfg.oc.otc.manager.aimsun.detectors.DetectorPair;
import de.dfg.oc.otc.aid.disturbance.DisturbanceManager;
import de.dfg.oc.otc.tools.LimitedQueue;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class has to implemented by each AID algorithm.
 */
public abstract class AbstractAIDAlgorithm extends Observable implements Observer {
    private static Logger log;
    /**
     * Contains the past occupancy values of all observed detector pairs. <detector pair id, occupancy value>
     */
    protected final LimitedQueue<Map<String, Float>> pastOccupancyValues;
    /**
     * List of tentative incidents for each detector pair identifier. <detector pair id, Incident object>
     */
    protected final Map<String, Incident> tentativeIncidents;
    /**
     * AIDEvaluator which determines metrics such as FAR and CAR for the algorithm.
     */
    private final AIDEvaluator evaluator;
    /**
     * Contains the current occupancy values of all observed detector pairs. Key
     * = Pair ID, Value = List of occupancy values for one detector pair
     * measured in the current iteration.
     */
    private final Map<String, List<Float>> currentOccupancyValues;
    /**
     * Node to which the monitoring zone of this algorithm is assigned to.
     */
    protected OTCNode node;
    /**
     * Monitoring zone to which this algorithm is assigned to.
     */
    protected AIDMonitoringZone monitoringZone;
    /**
     * Number of detectors which are observed by this algorithm.
     */
    protected int observedDetectorCount;
    /**
     * Determines if the direction of the detector pairs which are monitored by
     * this algorithm is incoming to the junction or outgoing from it.
     */
    protected boolean incomingToJunction;
    /**
     * Scanning interval of the algorithm (in ticks).
     */
    protected final int executionInterval;
    /**
     * Duration of a single simulation step.
     */
    protected float simulationStepSize;
    /**
     * Time in milliseconds after which the algorithm is executed.
     */
    protected final float warmupTime;
    /**
     * Contains the current algorithm states for each detector pair combination.
     */
    public final Map<String, Integer> algorithmStates;
    /**
     * Defines whether the algorithm is currently in use by weighted combination.
     * If set the algorithm shouldn't report incidents, thats the job of the weighted combination.
     */
    private boolean usedInCombination;
    /**
     * Identifies an AID instance, is composed of all detector pair IDs
     */
    protected String identifier;
    /**
     * List of all instances of this class
     */
    private static ArrayList<AbstractAIDAlgorithm> instances = new ArrayList<>();


    protected AbstractAIDAlgorithm() {
        this.evaluator = new AIDEvaluator();
        this.algorithmStates = new HashMap<>();

        this.observedDetectorCount = 0;
        this.incomingToJunction = false;

        this.executionInterval = DefaultParams.AID_SCANNING_INTERVAL;
        this.warmupTime = DefaultParams.AID_WARUM_UP_TIME;
        this.simulationStepSize = OTCManager.getSimulationStepSize();
        this.currentOccupancyValues = new HashMap<>();
        this.pastOccupancyValues = new LimitedQueue<>(3);
        this.tentativeIncidents = new HashMap<>();
        AbstractAIDAlgorithm.instances.add(this);
    }

    public void setUsedInCombination(boolean value) {
        this.usedInCombination = value;
    }

    protected void newDetectorData(AbstractDetectorGroup detectorGroup, DetectorDataValue detectorValue) {
        float time = detectorValue.getTime();

        // Only start calculations after warm up time
        if (time > this.warmupTime) {
            String pairID = detectorGroup.getId();

            // Create field for new detector pair
            this.currentOccupancyValues.putIfAbsent(pairID, new ArrayList<>());

            // Add the occupation value from the detector pair
            this.currentOccupancyValues.get(pairID).add(detectorValue.getValues()[DetectorCapabilities.OCCUPANCY]);

            boolean enoughOccupancyData = this.currentOccupancyValues.size() == this.observedDetectorCount;
            boolean iterationEnd = time % this.executionInterval + this.simulationStepSize >= this.executionInterval;
            // At the end of each iteration execute algorithm
            if (iterationEnd && enoughOccupancyData) {
                // Add the average occupancy values from the current iteration
                this.pastOccupancyValues.add(getCurrentOccupancyAverages());
                this.currentOccupancyValues.clear();

                prepareAndRunAlgorithm(time);
            }
        }
    }

    /**
     * Calculates the average occupancy values for each detector pair from the
     * current iteration data (in case some detectors have collected multiple
     * values for this iteration).
     *
     * @return Average occupancy values for each detector pair
     */
    private Map<String, Float> getCurrentOccupancyAverages() {
        Map<String, Float> iterationAverages = new HashMap<>();

        // Loop through each detector pair
        for (AbstractDetectorGroup detectorPair : monitoringZone.getMonitoredDetectorPairs()) {
            float occupancySum = 0;
            String detectorPairID = detectorPair.getId();
            List<Float> detectorValues = this.currentOccupancyValues.get(detectorPairID);

            if (detectorValues != null) {
                // Calculate the average occupancy for the current iteration
                for (float occupancyValue : detectorValues) {
                    occupancySum += occupancyValue;
                }

                float averageOccupancy = occupancySum / detectorValues.size();
                iterationAverages.put(detectorPairID, averageOccupancy);
            }
        }

        return iterationAverages;
    }

    /**
     * This method is triggered every time new detector data is available since
     * the algorithm is being registered as observer in the
     * {@link AIDMonitoringZone#setAIDAlgorithm(AbstractAIDAlgorithm)} method.
     *
     * @param group Detector pair
     * @param value Detector value
     */
    @Override
    public void update(Observable group, Object value) {
        newDetectorData((AbstractDetectorGroup) group, (DetectorDataValue) value);
    }

    /**
     * This method must be used for notifying the AID gui component about a new
     * incident.
     *
     * @param incident which will be reported
     * @see de.dfg.oc.otc.manager.gui.AIDComponentPanel#update(java.util.Observable, Object)
     */
    protected void reportIncident(Incident incident) {
        //if algorithm is in use by weighted combination, let it handle the incident reporting
        if (usedInCombination) {
            return;
        }

        evaluator.addDetectedIncident(incident);

        int junctionID = monitoringZone.getMonitoredLink().getStartJunction().getId();
        incident.setMonitoringZoneID(monitoringZone.getId());
        incident.setJunctionID(junctionID);
        incident.setReportTime(OTCManager.getInstance().getTime());

        this.setChanged();
        this.notifyObservers(incident);
    }

    /**
     * Sets the algorithm state to free and removes tentative incidents for a
     * given detector pair combination.
     *
     * @param time           of the algorithm execution
     * @param pairIdentifier String combining the ids of two detector pairs
     */
    protected void incidentFree(float time, String pairIdentifier) {
        // Set end time of incident
        Incident incident = tentativeIncidents.get(pairIdentifier);
        if (incident != null) {
            incident.setEndTime(time);
            // Notify the DisturbanceManager to stop the linked disturbance
            if (!usedInCombination) //if algorithm is in use by weighted combination, let it handle the incident reporting
                DisturbanceManager.getInstance().endDisturbance(incident, node);
        }

        // Remove false tentative incident
        tentativeIncidents.put(pairIdentifier, null);
    }

    public boolean isUsedInCombination() {
        return usedInCombination;
    }

    /**
     * Creates a new incident.
     *
     * @param time           of the incident occurrence
     * @param pairIdentifier String combining the ids of two detector pairs
     * @param isConfirmed    represent whether an incident is confirmed or not
     * @return Newly created incident
     */
    protected Incident createIncident(float time, String pairIdentifier, boolean isConfirmed) {
        String[] detectorPairIDs = getPairIDs(pairIdentifier);

        Incident incident = new Incident();
        incident.setDetectorID(pairIdentifier);
        incident.setUpstreamDetectorPair(detectorPairIDs[0]);
        incident.setDownstreamDetectorPair(detectorPairIDs[1]);
        incident.setStartTime(time);
        incident.setSectionID(getSectionID(detectorPairIDs));
        incident.setConfirmed(isConfirmed);

        // Notify the DisturbanceManager to start a new disturbance
        //if algorithm is in use by weighted combination, let it handle the incident reporting
        if (!usedInCombination) {
            DisturbanceManager.getInstance().startDisturbance(incident, node);
        }

        return incident;
    }

    /**
     * Creates a new junction incident.
     *
     * @param time of the incident occurrence
     * @return Newly created incident
     */
    protected Incident createJunctionIncident(float time) {
        Incident incident = new Incident();
        incident.setStartTime(time);
        incident.setType(Incident.IncidentType.JUNCTION_INCIDENT);
        return incident;
    }

    /**
     * Sets the observed detector count to a given value.
     */
    public void setObservedDetectorCount(int count) {
        this.observedDetectorCount = count;
    }

    /**
     * Returns the monitoring zone to which this algorithm is assigned to.
     */
    public AIDMonitoringZone getMonitoringZone() {
        return this.monitoringZone;
    }

    /**
     * Sets the monitoring zone to which this algorithm is assigned to.
     */
    public void setMonitoringZone(AIDMonitoringZone monitoringZone) {
        this.monitoringZone = monitoringZone;
    }

    /**
     * Returns a list of all incidents detected by this algorithm.
     */
    public Iterable<Incident> getDetectedIncidents() {
        return this.evaluator.getIncidentAlarms();
    }

    /**
     * Returns the AIDEvaluator which is assigned to this algorithm.
     */
    public AIDEvaluator getAIDEvaluator() {
        return evaluator;
    }

    /**
     * Returns the node to which this algorithm is assigned to.
     */
    protected OTCNode getNode() {
        return node;
    }

    /**
     * Sets the node to which this algorithm is assigned to.
     */
    public void setNode(final OTCNode node) {
        this.node = node;
    }

    /**
     * Should be called every time an algorithm is applied.
     */
    protected void algorithmApplied() {
        this.evaluator.algorithmApplied();
    }

    public void setLogger(Logger logger) {
        log = logger;
    }

    protected void log(String message) {
        log.info(message);
    }

    /**
     * Determines if the direction of the detector pairs which are monitored by
     * this algorithm is incoming to the junction or outgoing from it.
     *
     * @param incoming Direction of the detector pairs
     */
    public void setDirection(boolean incoming) {
        incomingToJunction = incoming;
    }

    /**
     * Returns the algorithm parameters as string for displaying purposes within
     * the aid gui component.
     *
     * @return Algorithm parameters for displaying purposes
     */
    public String getParametersAsString() {
        String parameters = "<html>";

        Map<String, Object> params = this.getParameters();

        // Sort parameter keys alphabetically
        Object[] keys = params.keySet().toArray();
        Arrays.sort(keys);

        for (Object key : keys) {
            parameters += key + " = " + params.get(key) + "<br>";
        }

        parameters += "</html>";

        return parameters;
    }

    /**
     * Returns the section id from a upstream-downstream detector group pair.
     *
     * @param detectorPairIDs Array of detector ids (upstream + downstream)
     * @return Section id which contains the detector pair
     */
    private int getSectionID(String[] detectorPairIDs) {
        int sectionID = 0;

        if (detectorPairIDs.length == 2) {
            AIDModule aidModule = node.getAIDComponent();
            AbstractDetectorGroup upstreamPair = aidModule.getPair(detectorPairIDs[0]);
            AbstractDetectorGroup downstreamPair = aidModule.getPair(detectorPairIDs[1]);

            if (upstreamPair.getType() == Type.SIMPLE_DETECTOR_PAIR) {
                // Use downstream detector from upstream detector pair
                DetectorPair detectorPair = (DetectorPair) upstreamPair;
                sectionID = detectorPair.getDownstreamDetector().getSectionId();
            } else {
                // Use upstream detector from downstream detector pair
                if (downstreamPair.getType() == Type.SIMPLE_DETECTOR_PAIR) {
                    DetectorPair detector = (DetectorPair) downstreamPair;
                    sectionID = detector.getUpstreamDetector().getSectionId();
                } else {
                    DetectorForkGroup upstream = (DetectorForkGroup) upstreamPair;
                    DetectorForkGroup downstream = (DetectorForkGroup) downstreamPair;

					/*
                     * If the section id from the downstream detector of the
					 * upstream detector pair matches the section id from the
					 * upstream detector from the downstream detector pair, then
					 * that is their common section.
					 */
                    for (Detector upstreamDetector : upstream.getDownstreamDetectors()) {
                        for (Detector downstreamDetector : downstream.getUpstreamDetectors()) {
                            if (upstreamDetector.getSectionId() == downstreamDetector.getSectionId()) {
                                sectionID = upstreamDetector.getSectionId();
                            }
                        }
                    }
                }
            }
        }

        return sectionID;
    }

    /**
     * Returns an unique identifier for a pair of detector ids.
     *
     * @param upstreamID   Id of the upstream detector
     * @param downstreamID Id of the downstream detector
     * @return Unique identifier which combines the ids of both detectors
     */
    protected String getPairIdentifier(String upstreamID, String downstreamID) {
        return upstreamID + ";" + downstreamID;
    }

    /**
     * Returns the original detector ids from the unique pair identifier.
     *
     * @param pairIdentifier String combining the ids of two detector pairs
     * @return Array of detector ids (upstream + downstream)
     */
    private String[] getPairIDs(String pairIdentifier) {
        String[] split = pairIdentifier.split(";");
        return new String[]{split[0], split[1]};
    }

    /**
     * This method is called after every algorithm of a node has been
     * initialized. It can be used for initializing cooperative algorithms.
     */
    public void finalizeInitialization() {
        // Provides a unique identifier for this instance in AID operation mode containing all monitored detector pair IDs
        if (this.monitoringZone != null) {
            this.identifier = this.monitoringZone.getMonitoredDetectorPairs().stream().map(AbstractDetectorGroup::getId).collect(Collectors.joining(""));
        }
    }

    /**
     * This instance method is called at the very end of the simulation
     * Override this method in your AID class
     */
    public void finish()
    {
        // finalizing stuff like waving goodbye or writing evaluation metrics
    }

    /**
     * This static method is called at the very end of the simulation to finish all instances
     */
    public static void finishAllInstances()
    {
        for (AbstractAIDAlgorithm aid : instances) {
            aid.finish();
        }
    }

    /**
     * Prepares the algorithm execution
     *
     * @param time of the algorithm execution
     */
    protected abstract void prepareAndRunAlgorithm(float time);

    /**
     * Returns the name of the algorithm (used by the {@link de.dfg.oc.otc.aid.algorithms.AIDAlgorithmFactory}
     * for creating instances).
     */
    public abstract String getName();

    public abstract Map<String, Object> getParameters();

    /**
     * Defines the minimal number of detector pairs which are required for this
     * algorithm to function properly.
     *
     * @return Required number of detector pairs
     */
    public abstract int getRequiredDetectorPairCount();

    /**
     * This is used by the weighted combination. Each algorithm should map its states to currently being an incident or not, with tentative incidents not counting as incident
     *
     * @param state The state value the algorithm calculated
     * @return boolean indicating whether the state maps to an incident state
     */
    public abstract boolean isStateMappedToIncident(int state);
}

