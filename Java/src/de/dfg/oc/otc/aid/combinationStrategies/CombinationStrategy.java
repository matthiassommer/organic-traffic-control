package de.dfg.oc.otc.aid.combinationStrategies;

import de.dfg.oc.otc.aid.AIDMonitoringZone;
import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.aid.algorithms.AIDAlgorithmFactory;
import de.dfg.oc.otc.aid.algorithms.AbstractAIDAlgorithm;
import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;
import de.dfg.oc.otc.aid.disturbance.DisturbanceManager;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.Map.Entry;

/**
 * Combines the computational result of several incident algorithms.
 * The combination result has to exceed a certain threshold to be classified as incident.
 */
public class CombinationStrategy extends AbstractAIDAlgorithm {
    /**
     * The list of enabled AID algorithms.
     */
    private List<AbstractAIDAlgorithm> algorithms;
    /**
     * An extended version of {@link AbstractAIDAlgorithm#algorithmStates}<br/>
     * The key is still the pairIdentifier of two AbstractDetectorGroups <br/>
     * The Value is another map with key algorithm name and its value is the state the
     * algorithm calculated for this pairIdentifier
     */
    private final Map<String, Map<String, Integer>> allAlgorithmStates;
    /**
     * The selected combination strategy.
     */
    private Strategy strategy;
    private MajorityVoting majorityVoting;

    public CombinationStrategy() {
        this.allAlgorithmStates = new HashMap<>();
        readSelectedAIDAlgorithms();
        readSelectedCombinationStrategy();
    }

    private void readSelectedCombinationStrategy() {
        String strategy = DefaultParams.AID_COMBINATION_STRATEGY;
        if (strategy.equals("MAJORITY_VOTE")) {
            this.strategy = Strategy.MAJORITY_VOTE;
            this.majorityVoting = new MajorityVoting();
        }
    }

    private void readSelectedAIDAlgorithms() {
        String algorithms = DefaultParams.AID_COMBINED_ALGORITHMS;
        if (algorithms.trim().isEmpty()) {
            this.algorithms = AIDAlgorithmFactory.getAllAlgorithms();
        } else {
            String[] selectedAIDAlgorithms = algorithms.split(",");
            this.algorithms = new ArrayList<>();
            for (String algorithm : selectedAIDAlgorithms) {
                //toUpper as factory compares this way
                this.algorithms.add(AIDAlgorithmFactory.getAlgorithm(algorithm.toUpperCase()));
            }
        }
    }

    /**
     * see
     * {@link de.dfg.oc.otc.aid.algorithms.AbstractAIDAlgorithm#prepareAndRunAlgorithm(float)}
     * <br/>
     * 1) Calling the method for each algorithm (except APID, which handles this itself in newDetectorData)<br/>
     * 2) Fill the combined stateMap {@link CombinationStrategy#allAlgorithmStates} <br/>
     * 3) Execute weighting method
     */
    protected void prepareAndRunAlgorithm(float time) {
        initialiseAlgorithmStates();
        initialiseIndividualAlgorithms();
        executeCombination(time);
    }

    private void initialiseIndividualAlgorithms() {
        for (AbstractAIDAlgorithm algorithm : algorithms) {
            String name = algorithm.getName();
            for (Entry<String, Integer> entry : algorithm.algorithmStates.entrySet()) {
                Map<String, Integer> algorithmStateMap = allAlgorithmStates.get(entry.getKey());

                if (algorithmStateMap != null) {
                    boolean isIncident = algorithm.isStateMappedToIncident(entry.getValue());
                    int state = isIncident ? CombinationState.INCIDENT_CONFIRMED.ordinal() : CombinationState.INCIDENT_FREE.ordinal();
                    algorithmStateMap.put(name, state);
                }
            }
        }
    }

    private void initialiseAlgorithmStates() {
        String previousDetectorPairID = null;

        // Loop through each detector pair
        for (AbstractDetectorGroup detectorPair : monitoringZone.getMonitoredDetectorPairs()) {
            String detectorPairID = detectorPair.getId();
            // Skip first detector since it has no predecessor
            if (previousDetectorPairID != null) {
                // detectorPairID = upstream
                // previousDetectorPairID = downstream
                String pairIdentifier = getPairIdentifier(detectorPairID, previousDetectorPairID);

                // Set initial state to incident free if it does not exist yet
                algorithmStates.putIfAbsent(pairIdentifier, CombinationState.INCIDENT_FREE.ordinal());
                allAlgorithmStates.putIfAbsent(pairIdentifier, new HashMap<>());
            }
            previousDetectorPairID = detectorPairID;
        }
    }

    @Override
    public void finalizeInitialization() {
        algorithms.forEach(AbstractAIDAlgorithm::finalizeInitialization);
    }

    /**
     * Execute weighting and handle the result.
     * Already reported incidents won't be reported twice, but set in state incident_continuing.
     */
    private void executeCombination(float time) {
        for (String detectorPairID : allAlgorithmStates.keySet()) {
            CombinationState state = getWeightedState(detectorPairID);

            if (state == CombinationState.INCIDENT_CONFIRMED
                    && algorithmStates.get(detectorPairID) == CombinationState.INCIDENT_CONFIRMED.ordinal()) {
                algorithmStates.put(detectorPairID, CombinationState.INCIDENT_CONTINUING.ordinal());
            } else if (state == CombinationState.INCIDENT_CONFIRMED
                    && algorithmStates.get(detectorPairID) == CombinationState.INCIDENT_FREE.ordinal()) {
                algorithmStates.put(detectorPairID, CombinationState.INCIDENT_CONFIRMED.ordinal());

                Incident incident = createIncident(time, detectorPairID, true);
                tentativeIncidents.put(detectorPairID, incident);

                DisturbanceManager.getInstance().confirmDisturbance(incident, getNode());
                reportIncident(incident);
            } else if (state == CombinationState.INCIDENT_FREE
                    && algorithmStates.get(detectorPairID) != CombinationState.INCIDENT_FREE.ordinal()) {
                algorithmStates.put(detectorPairID, CombinationState.INCIDENT_FREE.ordinal());
                incidentFree(time, detectorPairID);
            }
        }
    }

    /**
     * Executes the actual weighted combination
     *
     * @return The result state after the weighting combination is processed
     */
    private CombinationState getWeightedState(String pairIdentifier) {
        switch (this.strategy) {
            case MAJORITY_VOTE:
                return majorityVoting.executeStrategy(pairIdentifier, allAlgorithmStates, algorithms);
        }
        throw new IllegalArgumentException("Unknown AID strategy");
    }

    @Override
    public int getRequiredDetectorPairCount() {
        // get max of algorithms required detector count
        return algorithms.stream()
                .max((alg1, alg2) -> Integer.compare(
                        alg1.getRequiredDetectorPairCount(),
                        alg2.getRequiredDetectorPairCount())).get().getRequiredDetectorPairCount();
    }

    @Override
    public void setNode(OTCNode node) {
        super.setNode(node);
        for (AbstractAIDAlgorithm algorithm : algorithms) {
            algorithm.setNode(node);
        }
    }

    @Override
    public void setMonitoringZone(AIDMonitoringZone monitoringZone) {
        super.setMonitoringZone(monitoringZone);
        for (AbstractAIDAlgorithm algorithm : algorithms) {
            algorithm.setMonitoringZone(monitoringZone);
        }
    }

    @Override
    public void setObservedDetectorCount(int count) {
        super.setObservedDetectorCount(count);
        for (AbstractAIDAlgorithm algorithm : algorithms) {
            algorithm.setObservedDetectorCount(count);
        }
    }

    @Override
    public void setLogger(Logger logger) {
        super.setLogger(logger);
        for (AbstractAIDAlgorithm algorithm : algorithms) {
            algorithm.setLogger(logger);
        }
    }

    @Override
    public void setDirection(boolean incoming) {
        super.setDirection(incoming);
        for (AbstractAIDAlgorithm algorithm : algorithms) {
            algorithm.setDirection(incoming);
        }
    }

    @Override
    public void update(Observable obs, Object obj) {
        // update calls newDetectorData + special handling in ECA
        for (AbstractAIDAlgorithm algorithm : algorithms) {
            algorithm.update(obs, obj);
        }
        DetectorDataValue value = (DetectorDataValue) obj;
        prepareAndRunAlgorithm(value.getTime());
    }

    @Override
    public String getName() {
        return "Ensemble";
    }

    @Override
    public Map<String, Object> getParameters() {
        return null;
    }

    @Override
    public boolean isStateMappedToIncident(int state) {
        // don't need it in the weighted combination
        return false;
    }

    @Override
    public boolean isUsedInCombination() {
        return false; // this IS the combination
    }

    private enum Strategy {
        MAJORITY_VOTE, XCSR
    }
}
