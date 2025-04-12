/*
 * Copyright (c) 2015 by
 * Anthony Stein, M.Sc.
 * University of Augsburg
 * Department of Computer Science
 * Chair of Organic Computing
 * All rights reserved. Distribution without approval by the copyright holder is explicitly prohibited.
 * Sources are only for non-commercial and academic use
 * in the scope of student theses and courses of the University of Augsburg.
 *
 * THE SOFTWAREPARTS ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package de.dfg.oc.otc.layer1.controller.xcsic;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer1.controller.ClassifierException;
import de.dfg.oc.otc.layer1.controller.LCSConstants;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationComponent;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationConstants;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants.AbstractInterpolationTechnique;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants.values.AccumulatedIDWOTCValue;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants.values.InterpolationValue;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants.values.OTCValue;
import de.dfg.oc.otc.manager.OTCManager;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a set of classifiers.
 * <p>
 * The interpolated signal plan influences the way the action set is determined.
 *
 * @author hpr
 * @author rauhdomi
 */
//Added the integration of the action weights from the interpolation component when building the action set
//Added interpolation based covering
@SuppressWarnings("serial")
class ClassifierSet extends ArrayList<de.dfg.oc.otc.layer1.controller.xcsic.Classifier> {
    private static final Logger log = Logger.getLogger(ClassifierSet.class);
    private final long SEED = OTCManager.getInstance().getSystemSeed();

    /**
     * Used by the interpolation based convering,
     * to cover with the probability of the current interpolant's trust value
     */
    private RandomDataGenerator randomGenerator = new RandomDataGenerator();

    /**
     * Provides random long values for the Roulette-Wheel based deletion of classifiers
     */
    private RandomDataGenerator rand;

    /**
     * Counter needed to assign a running id the new classifiers that are added
     * to the population.
     */
    private int idCounter;

    /**
     * Determines if classifier ids will be updated when adding new classifiers
     * to this set. If this set represents the population, ids should be
     * updated. If this set represents a match set or an action set, no update
     * is necessary.
     */
    private boolean updateIds;

    /**
     * Creates an empty set of classifiers.
     */
    ClassifierSet() {
        this.rand = new RandomDataGenerator();
        if (DefaultParams.L2_MASTERSEED == 1) {
            this.rand.reSeed(SEED);
        }
    }

    /**
     * Creates a set of classifiers that contains all classifiers given as
     * parameter.
     *
     * @param cl the initial classifier that will be contained in the set
     */
    private ClassifierSet(final Collection<de.dfg.oc.otc.layer1.controller.xcsic.Classifier> cl) {
        super(cl);

        this.rand = new RandomDataGenerator();
        if (DefaultParams.L2_MASTERSEED == 1) {
            this.rand.reSeed(SEED);
        }
    }

    /**
     * Adds a classifier to this set.
     *
     * @param cl the classifier that will be added
     * @return {@code false} if a problem occurred
     */
    @Override
    public final synchronized boolean add(final de.dfg.oc.otc.layer1.controller.xcsic.Classifier cl) {
        while (getNumerositySum() >= DefaultParams.MAX_POPULATION_SIZE) {
            log.debug("POPULATION EXCEEDS MAXIMUM SIZE -> DELETION");
            deleteSingleClassifierFromPopulation();
        }

        if (updateIds) {
            this.idCounter++;
            cl.setClassifierID(this.idCounter);
        }

        return super.add(cl);
    }

    /**
     * Build the action set, i.e. determine the best action considering all
     * classifiers in the match set. The action set contains all classifiers
     * that promote the best action.
     *
     * @return the action set
     */
    final synchronized ClassifierSet buildActionSet(InterpolationComponent interpolationComponent) {
        // Store classifiers in hashmap depending on their action
        final Map<Integer, List<de.dfg.oc.otc.layer1.controller.xcsic.Classifier>> classifierMap = buildClassifierMap();

        // Get distinct actions
        final Set<Integer> actionHashes = classifierMap.keySet();

        // Calculate prediction array
        final double[] predictionArray = new double[actionHashes.size()];
        int pos = 0;

        for (Map.Entry<Integer, List<de.dfg.oc.otc.layer1.controller.xcsic.Classifier>> integerListEntry : classifierMap.entrySet()) {
            List<de.dfg.oc.otc.layer1.controller.xcsic.Classifier> clWithCommonAction = integerListEntry.getValue();

            float sumRewardFitness = 0;
            float sumFitness = 0;
            for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier cl : clWithCommonAction) {
                sumRewardFitness += cl.getPrediction() * cl.getFitness();
                sumFitness += cl.getFitness();
            }

            double payoffEstimateForClassifier = sumRewardFitness / sumFitness;

            if (InterpolationConstants.ASI) {
                //Incorporates the interpolated weights for the respective actions into the prediction array
                payoffEstimateForClassifier *= incorporateInterpolatedValues(
                        interpolationComponent, integerListEntry.getKey());
            }

            predictionArray[pos] = payoffEstimateForClassifier;

            pos++;
        }

        // Determine best action (i.e. highest payoff prediction)
        int posOfBestClassifier = 0;
        final int size = predictionArray.length;

        for (int i = 1; i < size; i++) {
            if (predictionArray[posOfBestClassifier] < predictionArray[i]) {
                posOfBestClassifier = i;
            }
        }

        // Return set of classifiers with best action
        final List<de.dfg.oc.otc.layer1.controller.xcsic.Classifier> classifiers = classifierMap
                .get(actionHashes.toArray()[posOfBestClassifier]);
        return new ClassifierSet(classifiers);
    }

    /**
     * Returns a factor determined by the results of an interpolation which can be mulitplied with an action of the
     * prediction array. This factor is mainly made up from the sibson weight of a specific action declared by its
     * action hash. A discount is then applied which depends on the interpolant's current trust level.
     * <p>
     * Currently only for InverseDistanceWeighting-Interpolation
     *
     * @param interpolationComponent The {@link InterpolationComponent} to get the weights from
     * @param actionHash             The action hash to get the factor from
     * @return The factor to be multiplied with the specific action from the prediction array
     */
    private double incorporateInterpolatedValues(InterpolationComponent interpolationComponent, int actionHash) {
        AbstractInterpolationTechnique interpolant = interpolationComponent.getInterpolant();
        AccumulatedIDWOTCValue accWeights = (AccumulatedIDWOTCValue) interpolant.getInterpolatedValue();

        double currentTrustLevel = interpolationComponent.getTrustLevel();
        double weight = accWeights.getAccumulatedIDWeightForActionHash(actionHash);

        return 1.0 + weight * currentTrustLevel;
    }

    private Map<Integer, List<de.dfg.oc.otc.layer1.controller.xcsic.Classifier>> buildClassifierMap() {
        final Map<Integer, List<de.dfg.oc.otc.layer1.controller.xcsic.Classifier>> classifiers = new HashMap<>();

        for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier : this) {
            int actionHash = classifier.getAction().hashCode();

            // New action?
            if (!classifiers.containsKey(actionHash)) {
                List<de.dfg.oc.otc.layer1.controller.xcsic.Classifier> clWithCommonAction = new ArrayList<>(2);
                clWithCommonAction.add(classifier);
                classifiers.put(actionHash, clWithCommonAction);
            }
            // Action already known?
            else {
                List<de.dfg.oc.otc.layer1.controller.xcsic.Classifier> clWithCommonAction = classifiers
                        .get(actionHash);
                clWithCommonAction.add(classifier);
                classifiers.put(actionHash, clWithCommonAction);
            }
        }

        return classifiers;
    }

    /**
     * Returns the subset of all classifiers in the set that match the situation
     * given as parameter. The match set is empty if no matching classifiers are
     * available.
     * <p>
     * Classifiers are included in the returned set by reference, i.e. they are
     * not cloned. Therefore, changes to the classifiers will also affect the
     * original set.
     *
     * @param situation the situation to be matched
     * @return a (possibly) empty set of classifiers
     */
    final synchronized ClassifierSet buildMatchSet(final float[] situation)
            throws ClassifierException {
        final ClassifierSet matchSet = new ClassifierSet();

        for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier : this) {
            // Matching classifier found?
            if (classifier.matches(situation)) {
                matchSet.add(classifier);
            }
        }

        return matchSet;
    }

    /**
     * Return {@code true} if the set contains a classifier that matches
     * the given situation.
     *
     * @param situation situation to be matched
     * @return {@code true} if the set contains a classifier that matches
     * the given situation
     * @throws de.dfg.oc.otc.layer1.controller.ClassifierException
     */
    final synchronized boolean containsMatchingClassifier(final float[] situation)
            throws ClassifierException {
        for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier cl : this) {
            if (cl.matches(situation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return {@code true} if the set contains a classifier that matches
     * the given situation.
     *
     * @param situation       situation to be matched
     * @param cycleConstraint cycle time (of action) to be matched ({@code 0}, if
     *                        constraint can be ignored)
     * @return {@code true} if the set contains a classifier that matches
     * the given situation
     * @throws de.dfg.oc.otc.layer1.controller.ClassifierException
     */
    @Deprecated
    protected final synchronized boolean containsMatchingClassifier(
            final float[] situation, final int cycleConstraint)
            throws ClassifierException {
        for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier cl : this) {
            if (cl.matches(situation, cycleConstraint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes one classifier in the population. The classifier that will be
     * deleted is chosen by roulette wheel selection considering the deletion
     * vote. Returns the macro-classifier which got decreased by one
     * micro-classifier.
     *
     * @return the deleted classifier
     */
    private void deleteSingleClassifierFromPopulation() {
        final double meanFitness = getFitnessSum() / (double) getNumerositySum();
        final int size = this.size();
        double sum = 0;

        for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier : this) {
            sum += classifier.getDeletionVote(meanFitness);
        }

        final double choicePoint = sum * this.rand.nextUniform(0, 1);
        sum = 0;

        Iterator<Classifier> classifierIterator = this.iterator();

        while (classifierIterator.hasNext()) {
            Classifier classifier = classifierIterator.next();

            sum += classifier.getDeletionVote(meanFitness);

            if (sum > choicePoint) {
                classifier.setNumerosity(classifier.getNumerosity() - 1);

                if (classifier.getNumerosity() == 0) {
                    log.debug("REMOVING " + classifier);
                    classifierIterator.remove();
                    return;
                }
            }
        }
    }

    /**
     * Returns a set of hash codes of the distinct actions contained in this
     * set.
     *
     * @return a set of hash codes of the distinct actions contained in this set
     */
    final synchronized Collection<Integer> getDistinctActions() {
        return this.stream().map(cl -> cl.getAction().hashCode()).collect(Collectors.toSet());
    }

    /**
     * Sum the fitness of all classifiers in this set.
     *
     * @return the fitness sum for this set
     */
    private double getFitnessSum() {
        double sum = 0;

        for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier : this) {
            sum += classifier.getFitness();
        }
        return sum;
    }

    /**
     * Returns the number of micro-classifiers in the set.
     *
     * @return the number of micro-classifiers in the set
     */
    private int getNumerositySum() {
        int sum = 0;

        for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier : this) {
            sum += classifier.getNumerosity();
        }
        return sum;
    }

    /**
     * Method used to estimate the prediction payoff for a specified action.
     * Note: Should only be used on action sets!
     *
     * @param theActionHash HashCode identifying the specific action
     * @return the estimated payoff
     */
    final synchronized float getPredictionForAction(final int theActionHash) {
        // Sammle alle Classifier f�r best. Aktion
        final Collection<de.dfg.oc.otc.layer1.controller.xcsic.Classifier> actArray = new ArrayList<>();

        for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier1 : this) {
            int actionHash = classifier1.getAction().hashCode();
            if (actionHash == theActionHash) {
                actArray.add(classifier1);
            }
        }

        float sumRewardFitness = 0;
        float sumFitness = 0;
        for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier : actArray) {
            sumRewardFitness += classifier.getPrediction() * classifier.getFitness();
            sumFitness += classifier.getFitness();
        }

        final float payoffEstimateForCl = sumRewardFitness / sumFitness;

        if (log.isDebugEnabled()) {
            log.debug(OTCManager.getInstance().getTime() + ": Set size "
                    + this.size() + " - #actArray " + actArray.size());
            log.debug("sumRewardFitness: " + sumRewardFitness);
            log.debug("sumFitness: " + sumFitness);
        }

        return payoffEstimateForCl;
    }

    /**
     * Selects a (non-matching) classifier that is (i) closest to the given
     * traffic demand and (ii) can be widened. The selected classifier is
     * widened and returned. If no classifier is selected, {@code null} is
     * returned.
     *
     * @param situation an unmatched traffic demand
     * @return
     */
    final de.dfg.oc.otc.layer1.controller.xcsic.Classifier selectNonMatchingClassifier(final float[] situation) {
        // Calculate distances
        for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier : this) {
            classifier.calculateDistance(situation);
        }

        // sort by distance
        Collections.sort(this);

        de.dfg.oc.otc.layer1.controller.xcsic.Classifier selectedCl = null;

        for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier : this) {
            // Suitable classifier for widening found
            if (classifier.canBeWidened(situation)) {
                // Widen (not yet matching) classifier
                de.dfg.oc.otc.layer1.controller.xcsic.Classifier clonedCl = classifier.clone();
                clonedCl.widenCondition(situation);

                // Reset error, fitness, and experience
                clonedCl.setPredictionError(LCSConstants.getInstance().getEpsilonInit());
                clonedCl.setFitness(LCSConstants.getInstance().getFitnessInit());
                clonedCl.setExperience(LCSConstants.getInstance().getExperienceInit());
                clonedCl.setNumerosity(1);
                clonedCl.setCreatedBy(de.dfg.oc.otc.layer1.controller.xcsic.Classifier.Creator.LCS);

                selectedCl = clonedCl;
                break;
            }
        }

        return selectedCl;
    }

    /**
     * Selects a (non-matching) classifier that is closest to the given
     * traffic demand. This classifier is then cloned and an interpolated {@link TrafficLightControllerParameters} object
     * will be set as its action. This method executed with the probability of the passed trust-level
     *
     * @param situation         The situation to cover
     * @param interpolatedValue The encapsulated interpolated action to set
     * @param trustLevel        The interpolant's current trust level
     * @return The classifier which matches the formerly uncovered situation
     */
    final de.dfg.oc.otc.layer1.controller.xcsic.Classifier selectNonMatchingClassifierByInterpolation
    (final float[] situation, InterpolationValue interpolatedValue, double trustLevel) {
        OTCValue nearestNeighbor = (OTCValue) interpolatedValue;
        if (nearestNeighbor.getInterpolatedAction() == null) {
            return null;
        }

        //Covering is conducted  by using the trust level as a probability
        if (randomGenerator.nextUniform(0, 1) > trustLevel) {
            return null;
        }

        // Calculate distances
        for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier : this) {
            classifier.calculateDistance(situation);
        }

        // sort by distance
        Collections.sort(this);

        de.dfg.oc.otc.layer1.controller.xcsic.Classifier selectedCl = null;

        if (this.iterator().hasNext()) {
            selectedCl = this.iterator().next().clone();

            selectedCl.setAction(nearestNeighbor.getInterpolatedAction());

            // Reset error, fitness, and experience
            selectedCl.setPredictionError(LCSConstants.getInstance().getEpsilonInit());
            selectedCl.setFitness(LCSConstants.getInstance().getFitnessInit());
            selectedCl.setExperience(LCSConstants.getInstance().getExperienceInit());
            selectedCl.setNumerosity(1);
            selectedCl.setCreatedBy(de.dfg.oc.otc.layer1.controller.xcsic.Classifier.Creator.LCS);
        }

        return selectedCl;
    }

    /**
     * Sets the id counter to the value given as parameter.
     *
     * @param idCounter the new value for {@code idCounter}
     */
    final void setIdCounter(final int idCounter) {
        this.idCounter = idCounter;
    }

    /**
     * Determines if classifier ids will be updated when adding new classifiers
     * to this set. If this set represents the population, ids should be
     * updated. If this set represents a match set or an action set, no update
     * is necessary.
     *
     * @param updateIds {@code true} when ids should be updated
     */
    final void setUpdateIds(final boolean updateIds) {
        this.updateIds = updateIds;
    }

    /**
     * Checks if classifiers in this set can be subsumed by other classifiers.
     */
    protected final synchronized void subsumeClassifiers() {
        final Collection<de.dfg.oc.otc.layer1.controller.xcsic.Classifier> toBeRemoved = new ArrayList<>();

        // Iterate over classifiers in this set
        for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier : this) {
            // Compare them to all other classifiers in this set
            this.forEach(classifier1 -> {
                boolean notContains = !toBeRemoved.contains(classifier1);
                boolean subsumes = classifier.subsumes(classifier1);
                // If classifier1 is not already subsumed and cl1 subsumes classifier1
                if (notContains && subsumes) {
                    // subsume classifier1
                    classifier.setNumerosity(classifier.getNumerosity() + classifier1.getNumerosity());
                    toBeRemoved.add(classifier1);
                }
            });
        }

        toBeRemoved.forEach(this::remove);
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object
     */
    @Override
    public final synchronized String toString() {
        final StringJoiner lineSepJoiner = new StringJoiner(System.lineSeparator());

        this.stream()
                .map(classifier -> classifier.toString())
                .forEach(classifierStr -> lineSepJoiner.add(classifierStr));

        final String exportedClassifierSet = lineSepJoiner.toString();

        if (exportedClassifierSet.isEmpty()) {
            return "NO CLASSIFIERS AVAILABLE";
        }

        return exportedClassifierSet;
    }
}
