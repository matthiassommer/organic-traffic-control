package de.dfg.oc.otc.layer1.controller.xcst;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer1.controller.ClassifierException;
import de.dfg.oc.otc.layer1.controller.LCSConstants;
import de.dfg.oc.otc.manager.OTCManager;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a set of classifiers.
 *
 * @author hpr
 */
@SuppressWarnings("serial")
class ClassifierSet extends ArrayList<Classifier> {
    private static final Logger log = Logger.getLogger(ClassifierSet.class);

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
     * Seed for the random generator. It is only used when the "masterSeed" property in the config file is set to 1
     */
    private final int SEED = OTCManager.getInstance().getSystemSeed();

    /**
     * Provides random long values for the Roulette-Wheel based deletion of classifiers
     */
    private RandomDataGenerator rand;

    /**
     * Creates an empty set of classifiers.
     */
    ClassifierSet() {
        super();

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
    private ClassifierSet(final Collection<Classifier> cl) {
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
    public final synchronized boolean add(final Classifier cl) {
        while (getNumerositySum() >= DefaultParams.MAX_POPULATION_SIZE) {
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
    final synchronized ClassifierSet buildActionSet() {
        // Store classifiers in hashmap depending on their action
        final Map<Integer, List<Classifier>> classifierMap = buildClassifierMap();

        // Get distinct actions
        final Set<Integer> actionHashes = classifierMap.keySet();

        // Calculate prediction array
        final double[] predictionArray = new double[actionHashes.size()];
        int pos = 0;

        for (Map.Entry<Integer, List<Classifier>> integerListEntry : classifierMap.entrySet()) {
            List<Classifier> clWithCommonAction = integerListEntry.getValue();

            float sumRewardFitness = 0;
            float sumFitness = 0;
            for (Classifier cl : clWithCommonAction) {
                sumRewardFitness += cl.getPrediction() * cl.getFitness();
                sumFitness += cl.getFitness();
            }

            double payoffEstimateForClassifier = sumRewardFitness / sumFitness;
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
        final List<Classifier> classifiers = classifierMap
                .get(actionHashes.toArray()[posOfBestClassifier]);
        return new ClassifierSet(classifiers);
    }

    private Map<Integer, List<Classifier>> buildClassifierMap() {
        final Map<Integer, List<Classifier>> classifiers = new HashMap<>();

        for (Classifier classifier : this) {
            int actionHash = classifier.getAction().hashCode();

            // New action?
            if (!classifiers.containsKey(actionHash)) {
                List<Classifier> clWithCommonAction = new ArrayList<>(2);
                clWithCommonAction.add(classifier);
                classifiers.put(actionHash, clWithCommonAction);
            }
            // Action already known?
            else {
                List<Classifier> clWithCommonAction = classifiers
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

        for (Classifier classifier : this) {
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
     * @throws ClassifierException
     */
    final synchronized boolean containsMatchingClassifier(final float[] situation)
            throws ClassifierException {
        for (Classifier cl : this) {
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
     * @throws ClassifierException
     */
    @Deprecated
    protected final synchronized boolean containsMatchingClassifier(
            final float[] situation, final int cycleConstraint)
            throws ClassifierException {
        for (Classifier cl : this) {
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

        for (Classifier classifier : this) {
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

        for (Classifier classifier : this) {
            sum += classifier.getFitness();
        }
        return sum;
    }

    /**
     * Returns the number of micro-classifiers in the set.
     */
    private int getNumerositySum() {
        int sum = 0;

        for (Classifier classifier : this) {
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
        final Collection<Classifier> actArray = new ArrayList<>();

        for (Classifier classifier1 : this) {
            int actionHash = classifier1.getAction().hashCode();
            if (actionHash == theActionHash) {
                actArray.add(classifier1);
            }
        }

        float sumRewardFitness = 0;
        float sumFitness = 0;
        for (Classifier classifier : actArray) {
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
     */
    final Classifier selectNonMatchingClassifier(final float[] situation) {
        // Calculate distances
        for (Classifier classifier : this) {
            classifier.calculateDistance(situation);
        }

        // sort by distance
        Collections.sort(this);

        Classifier selectedCl = null;

        for (Classifier classifier : this) {
            // Suitable classifier for widening found
            if (classifier.canBeWidened(situation)) {
                // Widen (not yet matching) classifier
                Classifier clonedCl = classifier.clone();
                clonedCl.widenCondition(situation);

                // Reset error, fitness, and experience
                clonedCl.setPredictionError(LCSConstants.getInstance().getEpsilonInit());
                clonedCl.setFitness(LCSConstants.getInstance().getFitnessInit());
                clonedCl.setExperience(LCSConstants.getInstance().getExperienceInit());
                clonedCl.setNumerosity(1);
                clonedCl.setCreatedBy(Classifier.Creator.LCS);

                selectedCl = clonedCl;
                break;
            }
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
        final Collection<Classifier> toBeRemoved = new ArrayList<>();

        // Iterate over classifiers in this set
        for (Classifier classifier : this) {
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
