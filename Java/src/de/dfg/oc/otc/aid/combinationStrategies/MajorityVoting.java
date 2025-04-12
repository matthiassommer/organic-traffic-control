package de.dfg.oc.otc.aid.combinationStrategies;

import de.dfg.oc.otc.aid.algorithms.AbstractAIDAlgorithm;

import java.util.List;
import java.util.Map;

/**
 * Counts how many algorithms voted for incident.
 * If the ratio is above a certain threshold, the combined result is categories as incident as well, otherwise incident free.
 *
 * @author Matthias Sommer.
 */
class MajorityVoting {
    /**
     * The weighted combination must return a positive value above the threshold to trigger a positive incident alarm.
     */
    private final float threshold = 0.5f;

    /**
     * Equally weights all algorithms result and counts the ones in favor of
     * 'incident'.<br/>
     * The ratio of algorithms calling 'incident' and all algorithms must be
     * above the defined threshold.
     *
     * @param pairIdentifier     of the detector pair
     * @param allAlgorithmStates the state each algorithm has
     * @param algorithms         @return the result state after all_equal is done
     */
    CombinationState executeStrategy(String pairIdentifier, Map<String, Map<String, Integer>> allAlgorithmStates, List<AbstractAIDAlgorithm> algorithms) {
        Map<String, Integer> algorithmStateMap = allAlgorithmStates.get(pairIdentifier);

        int incidentVotes = 0;
        for (Map.Entry<String, Integer> entry : algorithmStateMap.entrySet()) {
            if (entry.getValue() == CombinationState.INCIDENT_CONFIRMED.ordinal() || entry.getValue() == CombinationState.INCIDENT_CONTINUING.ordinal()) {
                incidentVotes++;
            }
        }

        if ((float) incidentVotes / algorithms.size() >= this.threshold) {
            return CombinationState.INCIDENT_CONFIRMED;
        }
        return CombinationState.INCIDENT_FREE;
    }
}
