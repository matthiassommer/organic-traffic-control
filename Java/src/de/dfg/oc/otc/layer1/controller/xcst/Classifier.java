package de.dfg.oc.otc.layer1.controller.xcst;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer1.controller.ClassifierException;
import de.dfg.oc.otc.layer1.controller.Interval;
import de.dfg.oc.otc.layer1.controller.LCSConstants;
import org.apache.commons.math3.util.FastMath;
import org.apache.log4j.Logger;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

/**
 * Represents a classifier / rule in the classifier system.
 *
 * @author hpr
 */
public class Classifier implements Cloneable, Comparable<Classifier> {
    private static final Logger log = Logger.getLogger(Classifier.class);
    private final ClassifierCondition condition;
    private TrafficLightControllerParameters action;

    /**
     * The action set size estimate of the classifier.
     */
    private float actionSetSize = 1;

    /**
     * An id number that helps to identify this classifier. (This id set when
     * adding this classifier to the population.)
     */
    private int classifierId;
    private LCSConstants constants;
    /**
     * Distinguish if the classifier was created by EA or LCS.
     */
    private Creator createdBy = Creator.LCS;
    /**
     * Distance between this classifier and the current situation. (This
     * attribute is updated each time {@code calculateDistance()} is
     * called.
     */
    private float distanceToCurrentSituation = Float.NaN;
    /**
     * Experience of this classifier, i.e. the number of times it has been
     * included in an action set.
     */
    private int experience;
    /**
     * Based on the inverse function of the prediction error.
     */
    private float fitness;
    /**
     * The reward prediction as initially set as the Classifier was generated
     * (by Layer 2 or via covering).
     */
    private float initialPrediction;
    /**
     * Traffic demand for which this classifier was initially created.
     */
    private float[] initialSituation;
    /**
     * Maximal traffic flow for this classifier.
     */
    private float[] maxFlow;
    /**
     * Counts the number of widenings performed on this classifier.
     */
    private int numberOfWidenings;
    /**
     * Specifies the number of identical classifiers copies that a classifier
     * represents.
     */
    private int numerosity = 1;
    /**
     * The reward prediction value of this classifier.
     */
    private float rewardPrediction;
    /**
     * The mean absolute deviation between the prediction value and the actual
     * payoff.
     */
    private float predictionError;
    /**
     * Flag used to configure the distance calculation method: - true: Distance
     * calculated by using the nearest interval point - false: Distance
     * calculated by using the centroid of the interval.
     */
    private boolean useInterval;

    /**
     * Creates a classifier from the given information.
     *
     * @param condition  the classifier condition
     * @param tlcParam   the classifier action
     * @param prediction the prediction
     */
    Classifier(final ClassifierCondition condition,
               final TrafficLightControllerParameters tlcParam,
               final float prediction) {
        initConstants();
        this.condition = condition;
        this.action = tlcParam;
        this.rewardPrediction = prediction;
        this.initialPrediction = prediction;
    }

    /**
     * Creates a classifier from the given information.
     *
     * @param situation  the traffic situation
     * @param tlcParam   the classifier action
     * @param prediction the prediction
     */
    Classifier(final float[] situation,
               final TrafficLightControllerParameters tlcParam,
               final float prediction) {
        initConstants();
        this.maxFlow = tlcParam.getMaxSignalGroupFlowForDegreeOfSaturation(1);

        this.condition = new ClassifierCondition(situation,
                tlcParam.getNbOfLanesForSignalGroups(), maxFlow);
        this.action = tlcParam;
        this.rewardPrediction = prediction;

        this.initialSituation = situation;
        this.initialPrediction = prediction;
    }

    /**
     * Calculates the distance of the situation given as parameter to this
     * classifier. The configuration parameter {@code useInterval} switches
     * between calculating the distance to the interval boundaries or to the
     * initial traffic demand for which this classifier has been created.
     *
     * @param situation the situation for which the distance is calculated
     * @return the calculated distance (or {@code NaN} in case of errors)
     */
    final float calculateDistance(final float[] situation) {
        if (condition.getLength() != situation.length) {
            log.warn("calculatedDistance(): Situation has wrong length!");
            return Float.NaN;
        }

        if (useInterval) {
            return calculateDistanceInterval(situation);
        } else {
            return calculateDistanceToInitialDemand(situation);
        }
    }

    /**
     * Calculates the distance of the situation given as parameter to the area
     * covered by the condition of this classifier. The distance is calculated
     * as sum of the distance between each interval and the corresponding
     * situation value. If the situation is covered by the condition,
     * {@code 0} is returned.
     *
     * @param situation the situation for which the distance is calculated
     * @return the calculated distance (or {@code NaN} in case of errors)
     */
    private float calculateDistanceInterval(final float[] situation) {
        float sum = 0;
        final int size = condition.getLength();

        for (int i = 0; i < size; i++) {
            final Interval interval = condition.getInterval(i);
            float dist = 0;

            if (interval.contains(situation[i]) == -1) {
                dist = interval.getLower() - situation[i];
            } else if (interval.contains(situation[i]) == 1) {
                dist = situation[i] - interval.getUpper();
            }
            sum += dist;
        }

        this.distanceToCurrentSituation = sum;
        return sum;
    }

    /**
     * Calculates the Euclidean distance of the traffic demand given as
     * parameter to the initial demand for which this classifier was initially
     * created.
     *
     * @param situation the traffic demand for which the distance is calculated
     * @return
     */
    private float calculateDistanceToInitialDemand(final float[] situation) {
        float sum = 0;
        final int size = initialSituation.length;

        for (int i = 0; i < size; i++) {
            final float dist = (float) FastMath.pow(initialSituation[i]
                    - situation[i], 2);
            sum += dist;
        }

        this.distanceToCurrentSituation = (float) Math.sqrt(sum);
        return distanceToCurrentSituation;
    }

    /**
     * Return {@code true} iff the classifier can be widened to match the
     * given traffic demand.
     *
     * @param situation a traffic demand
     * @return {@code true} iff the classifier can be widened to match the
     * given traffic demand
     * @see #maxFlow
     */
    final boolean canBeWidened(final float[] situation) {
        boolean isWidenedAble = true;

        for (int i = 0; i < situation.length; i++) {
            if (situation[i] > maxFlow[i]) {
                isWidenedAble = false;
                break;
            }
        }
        return isWidenedAble;
    }

    /**
     * Returns an exact copy of this classifier.
     *
     * @return an exact copy of this classifier
     */
    @Override
    public final Classifier clone() {
        final ClassifierCondition clonedCondition = this.condition.clone();
        final TrafficLightControllerParameters clonedTLCParams = this.action
                .clone();

        final Classifier clonedCl = new Classifier(clonedCondition,
                clonedTLCParams, this.rewardPrediction);
        clonedCl.classifierId = this.classifierId;
        clonedCl.predictionError = this.predictionError;
        clonedCl.fitness = this.fitness;
        clonedCl.experience = this.experience;
        clonedCl.numerosity = this.numerosity;
        clonedCl.createdBy = this.createdBy;
        clonedCl.numberOfWidenings = this.numberOfWidenings;

        // Initial situation, max flow
        final float[] clonedInitialSituation = new float[this.initialSituation.length];
        final float[] clonedMaxFlow = new float[this.maxFlow.length];

        for (int i = 0; i < clonedInitialSituation.length; i++) {
            clonedInitialSituation[i] = initialSituation[i];
            clonedMaxFlow[i] = maxFlow[i];
        }

        clonedCl.initialSituation = clonedInitialSituation;
        clonedCl.maxFlow = clonedMaxFlow;

        return clonedCl;
    }

    /**
     * Allows for sorting classifiers according to their distance to the current
     * situation. Returns {@code -1} if this classifier has a smaller
     * distance than {@code _cl}, {@code 0} if distances are the same,
     * and {@code 1} if this classifier has a larger distance than
     * {@code _cl}.
     *
     * @param classifier a classifier
     * @return {@code -1, 0, 1} depending on the comparison result
     */
    public final int compareTo(final Classifier classifier) {
        if (this.distanceToCurrentSituation < classifier.distanceToCurrentSituation) {
            return -1;
        } else if (this.distanceToCurrentSituation == classifier.distanceToCurrentSituation) {
            return 0;
        }
        return 1;
    }

    final TrafficLightControllerParameters getAction() {
        return action;
    }

    /**
     * Sets the {@code TrafficLightControllerParameters}-object given as
     * parameter as action of this classifier.
     *
     * @param tlcParam the {@code TrafficLightControllerParameters}-object to be
     *                 set as action
     */
    final void setAction(
            final TrafficLightControllerParameters tlcParam) {
        this.action = tlcParam;
        this.maxFlow = tlcParam.getMaxSignalGroupFlowForDegreeOfSaturation(1);
    }

    /**
     * Returns the deletion vote for this classifier.
     *
     * @param avgFitnessInPop the average fitness in the population
     * @return the deletion vote for this classifier
     */
    final double getDeletionVote(final double avgFitnessInPop) {
        double vote = this.actionSetSize * this.numerosity;
        final double ownFit = this.fitness / (double) this.numerosity;

        if (this.experience > DefaultParams.THETA_DEL && ownFit < DefaultParams.DELTA * avgFitnessInPop) {
            vote *= avgFitnessInPop / ownFit;
        }

        return vote;
    }

    final float getFitness() {
        return fitness;
    }

    final void setFitness(final float fitness) {
        this.fitness = fitness;
    }

    /**
     * Returns the kappa-value for this classifier. (Needed by the LCS for the
     * fitness calculations that are performed for all classifiers in an action
     * set.)
     *
     * @return
     */
    final float getKappa() {
        if (this.predictionError < constants.getEpsilonZero()) {
            return 1;
        }
        return new Float(constants.getAlpha()
                * FastMath.pow(predictionError / constants.getEpsilonZero(), -1
                * constants.getNy()));
    }

    final int getNumerosity() {
        return numerosity;
    }

    final void setNumerosity(final int numerosity) {
        this.numerosity = numerosity;
    }

    final float getPrediction() {
        return rewardPrediction;
    }

    /**
     * Initializes the LCSConstants needed for this class as well as the initial
     * values for predictionError,fitness, and experience.
     */
    private void initConstants() {
        this.constants = LCSConstants.getInstance();
        this.predictionError = constants.getEpsilonInit();
        this.fitness = constants.getFitnessInit();
        this.experience = constants.getExperienceInit();
        this.useInterval = DefaultParams.L1_USE_INTERVAL;
    }

    /**
     * Returns {@code true} if the classifier condition matches the
     * situation given as parameter.
     *
     * @param situation the situation that will be checked
     * @return {@code true} if the classifier condition matches the
     * situation
     */
    final boolean matches(final float[] situation)
            throws ClassifierException {
        return this.condition.matches(situation);
    }

    /**
     * Returns {@code true} if the classifier condition matches the
     * situation given as parameter and if its action matches the given cycle
     * time.
     *
     * @param situation       the situation that will be checked
     * @param cycleConstraint time constrained that will be checked
     * @return {@code true} if the classifier condition matches the
     * situation
     */
    @Deprecated
    final boolean matches(final float[] situation,
                          final int cycleConstraint) throws ClassifierException {
        // Relevant cycle constraint given?
        boolean cycleIrrelevant = false;
        if (cycleConstraint == 0) {
            cycleIrrelevant = true;
        }

        // TODO Exakte �bereinstimmung bei Umlaufzeit sinnvoll?
        return this.condition.matches(situation)
                && (cycleIrrelevant || cycleConstraint == action.getCycleTime());
    }

    /**
     * Sets the action set size estimate of this classifier to the value given
     * as parameter.
     *
     * @param actionSetSize the action set size estimate that will be set
     */
    final void setActionSetSize(final float actionSetSize) {
        this.actionSetSize = actionSetSize;
    }

    final void setClassifierID(final int clId) {
        this.classifierId = clId;
    }

    final void setCreatedBy(final Creator createdBy) {
        this.createdBy = createdBy;
    }

    final void setExperience(final int experience) {
        this.experience = experience;
    }

    final void setInitialPrediction(final float initPrediction) {
        this.initialPrediction = initPrediction;
    }

    final void setInitialSituation(final float[] initialSituation) {
        this.initialSituation = initialSituation;
    }

    final void setMaxFlow(final float[] maxFlow) {
        this.maxFlow = maxFlow;
    }

    final void setNumberOfWidenings(final int numberOfWidenings) {
        this.numberOfWidenings = numberOfWidenings;
    }

    final void setPredictionError(final float predictionError) {
        this.predictionError = predictionError;
    }

    /**
     * Returns {@code true} if this classifier can subsume the classifier
     * given as parameter. (See also: Butz' book, Appendix B, Subsumption)
     *
     * @param classifier the parameter that is compared to this one
     * @return {@code true} if this classifier can subsume {@code _cl}
     */
    final boolean subsumes(final Classifier classifier) {
        // Check if argument references a different object
        if (classifier == this) {
            return false;
        }

        // Check if this classifier is experienced and accurate enough to
        // subsume other classifiers
        if (this.experience <= constants.getThetaSub()
                || this.predictionError >= constants.getEpsilonZero()) {
            return false;
        }

        // Check if action is the same
        final boolean matchesHashCode = this.action.hashCode() == classifier.action.hashCode();
        final boolean containsCondition = this.condition.contains(classifier.condition);
        return matchesHashCode && containsCondition;

    }

    /**
     * Returns a string representation of the object, the output is formatted
     * for better readability (resulting in a loss of precision).
     *
     * @return a formatted string representation of the object
     */
    @Override
    public final String toString() {
        final DecimalFormat df = new DecimalFormat("000");
        final Formatter ff = new Formatter(Locale.ENGLISH);

        String createdBy = " ";
        if (this.createdBy == Creator.EvoAlg) {
            createdBy += Creator.EvoAlg;
        } else {
            createdBy += this.createdBy + df.format(this.numberOfWidenings);
        }

        final String output = df.format(this.classifierId)
                + createdBy
                + " --- "
                + this.condition
                + " --- "
                + this.action
                + " --- "
                + ff.format("%6.2f, %6.2f, %6.2f, %.2f, %5.2f, %2d, %2d",
                this.rewardPrediction, this.initialPrediction,
                this.predictionError, this.fitness, this.actionSetSize,
                this.numerosity, this.experience) + " --- "
                + Arrays.toString(initialSituation);
        ff.close();

        return output;
    }

    /**
     * Updates the action set size.
     *
     * @param numerositySum the numerosity sum for the action set this classifier was part
     *                      of
     */
    private void updateActionSetSize(final int numerositySum) {
        if (this.experience < 1 / constants.getBeta()) {
            this.actionSetSize += (numerositySum - actionSetSize) / experience;
        } else {
            this.actionSetSize += constants.getBeta()
                    * (numerositySum - actionSetSize);
        }
    }

    /**
     * Updates prediction, prediction error, experience, and action set size
     * estimates of this classifier depending on the obtained reward and the
     * action set size.
     *
     * @param reward        the reward
     * @param numerositySum the numerosity sum of the action set this classifier was part
     *                      of
     */
    final void updateEvaluation(final float reward,
                                final int numerositySum) {
        this.experience++;
        updatePredictionError(reward);
        updatePrediction(reward);
        updateActionSetSize(numerositySum);
    }

    /**
     * Updates the prediction based on the obtained reward.
     *
     * @param reward the obtained reward
     */
    private void updatePrediction(final float reward) {
        if (this.experience < 1 / constants.getBeta()) {
            this.rewardPrediction += (reward - rewardPrediction) / experience;
        } else {
            this.rewardPrediction += constants.getBeta()
                    * (reward - rewardPrediction);
        }
    }

    /**
     * Updates the prediction error of this classifier depending on the obtained
     * reward.
     *
     * @param reward the obtained reward
     */
    private void updatePredictionError(final float reward) {
        if (this.experience < 1 / constants.getBeta()) {
            this.predictionError += (Math.abs(reward - rewardPrediction) - predictionError)
                    / experience;
        } else {
            this.predictionError += constants.getBeta()
                    * (Math.abs(reward - rewardPrediction) - predictionError);
        }
    }

    /**
     * Widens the condition of the classifier as far as necessary to fit the
     * given situation.
     *
     * @param situation a situation that should be included in the condition
     */
    final void widenCondition(final float[] situation) {
        this.condition.widen(situation);
        this.numberOfWidenings++;
        this.createdBy = Creator.LCS;
    }

    /**
     * Enumerator. Distinguish if the classifier was created by the EA (trough
     * optimization) or by the LCS (through widening).
     */
    public enum Creator {
        EvoAlg, LCS, TMP
    }
}
