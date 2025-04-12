package de.dfg.oc.otc.aid.algorithms.knn;

import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author alexandermartel
 * @see <a
 * href="http://en.wikipedia.org/wiki/K-nearest_neighbors_algorithm">http://en.wikipedia.org/wiki/K-nearest_neighbors_algorithm</a>
 */
public class KNNAlgorithm extends AbstractkNNAlgorithm {
    @Override
    protected void executekNNAlgorithm(String pairIdentifier, String groupIdentifier, DetectorDataValue detectorDataValue) {
        List<KNNNeighbor> neighbors = getKNearestNeighbors(groupIdentifier, detectorDataValue, this.k);

        if (neighbors != null) {
            algorithmStates.putIfAbsent(pairIdentifier, KNNState.INCIDENT_FREE.ordinal());
        } else {
            return;
        }

        algorithmApplied();
        KNNState resultState = majorityVote(neighbors);

        switch (resultState) {
            case INCIDENT_CONFIRMED:
                incidentOccurred(pairIdentifier, detectorDataValue.getTime());
                break;
            default:
                incidentFree(detectorDataValue.getTime(), pairIdentifier);
                break;
        }
    }

    /**
     * The nearest neighbors are put to vote. Each neighbor votes for it's own
     * classification. The classification with the most votes wins. <br/>
     * If there's a tie, the total distances of all neighbors of the tied
     * classes are compares. Lowest total distance wins. <br>
     * If there's another tie, phew.
     *
     * @param neighbors the list of retrieved nearest neighbors
     * @return the classification
     */
    private KNNState majorityVote(List<KNNNeighbor> neighbors) {
        Map<KNNState, Integer> votes = new HashMap<>();
        Map<KNNState, Float> summedUpDistances = new HashMap<>();

        for (KNNNeighbor entry : neighbors) {
            KNNState classification = entry.getkNNDetectorDataValue().isIncident() ? KNNState.INCIDENT_CONFIRMED : KNNState.INCIDENT_FREE;
            if (votes.containsKey(classification)) {
                votes.put(classification, votes.get(classification) + 1);
                summedUpDistances.put(classification, summedUpDistances.get(classification) + entry.getDistance());
            } else {
                votes.put(classification, 1);
                summedUpDistances.put(classification, entry.getDistance());
            }
        }

        int maxVotes = 0;
        KNNState maxVotesName = null;

        List<Entry<KNNState, Integer>> tiedClasses = new ArrayList<>();
        for (Entry<KNNState, Integer> entry : votes.entrySet()) {
            if (entry.getValue() >= maxVotes) {
                if (entry.getValue() > maxVotes) {
                    tiedClasses.clear();
                }
                tiedClasses.add(entry);
                maxVotesName = entry.getKey();
                maxVotes = entry.getValue();
            }
        }

        if (tiedClasses.size() > 1) {
            float lowestSummatedDistance = 0;
            boolean first = true;
            for (Entry<KNNState, Integer> entry : tiedClasses) {

                float distance = summedUpDistances.get(entry.getKey());
                if (distance < lowestSummatedDistance || first) {
                    first = false;
                    lowestSummatedDistance = distance;
                    maxVotesName = entry.getKey();
                }
            }

        }

        return maxVotesName;
    }

    @Override
    public String getName() {
        return "kNN";
    }

    @Override
    public Map<String, Object> getParameters() {
        return null;
    }
}
