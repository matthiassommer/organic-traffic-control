package de.dfg.oc.otc.aid.algorithms.knn;

import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author alexandermartel
 *         The Fuzzy kNN algorithm. See
 *         "A Fuzzy K-Nearest Neighbor Algorithm" by James M. Keller
 *         <br/>
 *         Neighbor retrival works like normal kNN
 *         <br/>
 *         Instead of a majority vote a fuzzy membership degree is calculated
 *         using the distances to the neighbors and a membership degree of the
 *         neighbor.
 */
public class FuzzykNN extends AbstractkNNAlgorithm {
    /**
     * indicating whether the algorithm uses fuzzyMembership for the
     * membership degree of the neighbors<br/>.
     * see
     * {@link FuzzykNN#getMembershipDeegre(List, KNNDetectorDataValue, boolean)}
     * for more info
     */
    private final boolean fuzzyMembership = true;
    /**
     * Calculated fuzzy membership must be above threshold to count as a match
     */
    private final float threshold = 0.5f;
    /**
     * Determining the fuzzy strength parameter, used when calculating
     * the membership to a certain class using the neighbors distance.
     * Fuzzy strength influences the euclidian distance, which usually uses a
     * power of 2, but here its 2/(fuzzyStrength - 1).
     * A fuzzy strength <b>equal 2</b>is the <b>usual euclidian distance</b>.
     * With a fuzzy strength <b>greater than 2</b> higher distances have less
     * effect and <b>neighbors are more evenly weighted</b> the more fuzzy
     * strength increases.
     * With a fuzzy strength <b>less than 2</b> higher distances have more
     * effect and <b>closer neighbors are weighted more heavily</b> the more
     * fuzzy strength approaches the value '1'.
     */
    private int fuzzyStrength = 2;

    @Override
    protected void executekNNAlgorithm(String pairIdentifier, String groupIdentifier, DetectorDataValue detectorDataValue) {
        List<KNNNeighbor> neighbors = getKNearestNeighbors(groupIdentifier, detectorDataValue, this.k);

        // completely new data? add it to the states map as incident_free
        if (neighbors != null) {
            algorithmStates.putIfAbsent(pairIdentifier, KNNState.INCIDENT_FREE.ordinal());
        } else {
            return;
        }

        algorithmApplied();

        float membershipToIncidentClass = fuzzyMembershipInIncidentClass(detectorDataValue, getTrainingListForDetectorGroupIdentifier(groupIdentifier), neighbors);
        if (membershipToIncidentClass > this.threshold) {
            // its an incident
            incidentOccurred(pairIdentifier, detectorDataValue.getTime());
        } else {
            incidentFree(detectorDataValue.getTime(), pairIdentifier);
        }
    }

    /**
     * Calculates the actual fuzzy membership to the 'incident' class
     *
     * @param instance  The DetectorDataValue we're trying to classify
     * @param training  The trainingSet
     */
    private float fuzzyMembershipInIncidentClass(DetectorDataValue instance, List<KNNDetectorDataValue> training, List<KNNNeighbor> neighbors) {
        // check the paper to get more in depth information on the algorithm
        float enumerator = 0;
        float denominator = 0;

        for (int i = 0; i < k; i++) {
            float membershipDegree = getMembershipDeegre(training, neighbors.get(i).getkNNDetectorDataValue(), this.fuzzyMembership);
            float distance = (float) euclidianDistance.compute(convertFloatsToDoubles(instance.getValues()), neighbors.get(i).getkNNDetectorDataValue().getFeatureArray());
            if (distance == 0) {
                // if the distance is zero we'd get NaN as result
                distance = 0.001f;
            }

            enumerator += membershipDegree * (1 / distance);
        }

        for (int i = 0; i < k; i++) {
            float distance = (float) euclidianDistance.compute(convertFloatsToDoubles(instance.getValues()), neighbors.get(i).getkNNDetectorDataValue().getFeatureArray());
            if (distance == 0) {
                distance = 0.001f;
            }
            denominator += 1 / distance;
        }

        return enumerator / denominator;
    }

    /**
     * Calculates the membership degree of a neighbor, either fuzzy or crisp.
     * <br/>
     * Crisp membership: if the neighbor is in the class we're checking for
     * (incident here), it returns 1, otherwise 0.
     * <br/>
     * Fuzzy membership: return value depends on the neighbor being in the class
     * we're checking for and the neighbors of the currentNeighbor being in the
     * same class we're checking for. If currentNeighbor is in the same class
     * the value ranges from 0.51 to 1, otherwise from 0 to 0.49.
     *
     * @param trainingSet     The trainingSet we're working on
     * @param currentNeighbor One neighbor of the datapoint we're currently classifying
     * @param fuzzyMembership boolean indicating whether we're working with fuzzy (or crisp)
     *                        membership
     */
    private float getMembershipDeegre(List<KNNDetectorDataValue> trainingSet, KNNDetectorDataValue currentNeighbor, boolean fuzzyMembership) {
        //TODO parameterized?
        int membershipDegreeK = 4;

        List<KNNNeighbor> newNeighbor = Collections.emptyList();
        if (fuzzyMembership) {
            List<KNNDetectorDataValue> tempTraining = new ArrayList<>(trainingSet);
            tempTraining.remove(currentNeighbor);

            newNeighbor = getNeighborsOfTrainingSet(tempTraining, currentNeighbor.getDetectorDataValue(), membershipDegreeK);
        }

        float membershipDegree;
        if (currentNeighbor.isIncident()) {
            if (!fuzzyMembership) {
                membershipDegree = 1;
            } else {
                // membershipDegree =
                int neighborsInSameClass = neighborsInSameClass(newNeighbor, true);
                membershipDegree = (float) neighborsInSameClass / membershipDegreeK;
                membershipDegree *= 0.49f;
                membershipDegree += 0.51f;
            }
        } else {
            if (!fuzzyMembership) {
                membershipDegree = 0;
            } else {
                int neighborsInSameClass = neighborsInSameClass(newNeighbor, true);
                membershipDegree = (float) neighborsInSameClass / membershipDegreeK;
                membershipDegree += 0.49f;
            }
        }

        return membershipDegree;
    }

    /**
     * @param neighbors  a list of neighbors
     * @param isIncident the classification to check for
     * @return the number of neighbors with belong to the class specified in
     * isIncident
     */
    private int neighborsInSameClass(List<KNNNeighbor> neighbors, boolean isIncident) {
        int belonging = 0;
        for (KNNNeighbor neighbor : neighbors) {
            if (neighbor.getkNNDetectorDataValue().isIncident() == isIncident) {
                belonging++;
            }
        }
        return belonging;
    }

    @Override
    public String getName() {
        return "Fuzzy kNN";
    }

    @Override
    public Map<String, Object> getParameters() {
        return null;
    }
}
