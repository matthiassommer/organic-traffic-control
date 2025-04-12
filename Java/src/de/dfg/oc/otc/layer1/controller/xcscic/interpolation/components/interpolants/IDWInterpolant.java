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

package de.dfg.oc.otc.layer1.controller.xcscic.interpolation.components.interpolants;

import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer1.controller.LCSConstants;
import de.dfg.oc.otc.layer1.controller.xcscic.Classifier;
import de.dfg.oc.otc.layer1.controller.xcscic.ClassifierSet;
import de.dfg.oc.otc.layer1.controller.xcscic.XCSCIC;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.InterpolationComponentException;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.InterpolationConstants;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.Situation;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.components.interpolants.values.AccumulatedIDWOTCValue;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.components.interpolants.values.InterpolationValue;
import de.dfg.oc.otc.manager.aimsun.SignalGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Anthony on 10.02.2015.
 * <p>
 * Interpolates the {@link Classifier}s of the {@link XCSCIC}'s population based on inverse distance weighting
 *
 * @author Anthony Stein
 * @author Dominik Rauh
 */
public class IDWInterpolant extends AbstractInterpolationTechnique {
    private static final int POWER = InterpolationConstants.IDW_EXPO;

    @Override
    public InterpolationValue interpolate(Situation queryPoint, ClassifierSet population) throws InterpolationComponentException {
        this.lastQueryPoint = queryPoint;

        Map<Classifier, Double> weightsForClassifiers = this.calculateWeights(queryPoint, population);

        double sum_weights = 0.0;

        for (Double weight : weightsForClassifiers.values()) {
            sum_weights += weight;
        }

        Classifier interpolatedClassifier = null;
        Classifier neNeClassifier = getNearestNeighborClassifier(weightsForClassifiers);

        if (population.size() > 0) {
            for (Map.Entry<Classifier, Double> entry : weightsForClassifiers.entrySet()) {
                double accWeightForActionHash = entry.getValue();
                accWeightForActionHash /= sum_weights;
                weightsForClassifiers.put(entry.getKey(), accWeightForActionHash);
            }

            interpolatedClassifier = createInterpolatedClassifier(weightsForClassifiers, queryPoint);
        }

        this.interpolatedValue = new AccumulatedIDWOTCValue(
                neNeClassifier,
                interpolatedClassifier,
                weightsForClassifiers);

        return this.interpolatedValue;
    }

    @Override
    public void addNewSite(Situation situation, Object[] attributes) throws InterpolationComponentException {
        throw new InterpolationComponentException("Method not supported by " + getClass());
    }

    @Override
    public void removeSite(Object siteToRemove) throws InterpolationComponentException {
        throw new InterpolationComponentException("Method not supported by " + getClass());
    }

    private Classifier getNearestNeighborClassifier(Map<Classifier, Double> weightsForClassifiers) {
        Classifier neNeClassifier = null;
        double weightOfNeNe = Double.MIN_VALUE;

        for (Map.Entry<Classifier, Double> entry : weightsForClassifiers.entrySet()) {
            if (entry.getValue() >= weightOfNeNe) {
                weightOfNeNe = entry.getValue();
                neNeClassifier = entry.getKey();
            }
        }

        return neNeClassifier;
    }

    private Map<Classifier, Double> calculateWeights(Situation queryPoint, ClassifierSet population) throws InterpolationComponentException {
        HashMap<Classifier, Double> distanceWeights = new HashMap<>(population.size());

        for (Classifier sample : population) {
            double w_i = Math.pow(1 / calculateEuclideanDistance(sample, queryPoint), POWER);
            distanceWeights.put(sample, w_i);
        }
        return distanceWeights;
    }

    private double calculateEuclideanDistance(Classifier x_i, Situation x_q) throws InterpolationComponentException {
        double sum = 0.0;
        int dimCount = 0;

        for (Map.Entry<SignalGroup, Double> x_qFlow : x_q.getFlowsOfSignalGroup().entrySet()) {
            sum += Math.pow(x_i.getCondition().getInterval(dimCount).getCenter() - x_qFlow.getValue(), 2);
            dimCount++;
        }

        if (dimCount != x_q.getFlowsOfSignalGroup().size()) {
            throw new InterpolationComponentException(
                    "Situation of query point doesn't have the same dimensions as the sampling points");
        }
        return Math.sqrt(sum);
    }

    private Classifier createInterpolatedClassifier(Map<Classifier, Double> weightsForClassifier, Situation queryPoint) {
        Classifier interpolatedClassifier = null;

        float[] classifierSituation = interpolationSituationToClassifierSituation(queryPoint);
        TrafficLightControllerParameters classifierAction = createInterpolatedAction(weightsForClassifier);
        float classifierPrediction = createInterpolatedPrediction(weightsForClassifier);
        float classifierPredictionError = createInterpolatedPredictionError(weightsForClassifier);
        float classifierFitness = createInterpolatedFitness(weightsForClassifier);

        interpolatedClassifier = new Classifier(classifierSituation, classifierAction, classifierPrediction);
        interpolatedClassifier.setPredictionError(classifierPredictionError);
        interpolatedClassifier.setFitness(classifierFitness);
        interpolatedClassifier.setExperience(LCSConstants.getInstance().getExperienceInit());
        interpolatedClassifier.setNumerosity(1);
        interpolatedClassifier.setCreatedBy(Classifier.Creator.IC);

        return interpolatedClassifier;
    }

    private TrafficLightControllerParameters createInterpolatedAction(
            Map<Classifier, Double> weightsForClassifier) {
        TrafficLightControllerParameters interpolatedAction = null;
        TrafficLightControllerParameters referenceAction = weightsForClassifier.keySet().iterator().next().getAction();

        int greenTimesCount = referenceAction.getGreenTimes().length;
        float[] interpolatedGreenTimes = new float[greenTimesCount];

        for (Map.Entry<Classifier, Double> entry : weightsForClassifier.entrySet()) {
            TrafficLightControllerParameters action = entry.getKey().getAction();
            double weight = entry.getValue();

            interpolateGreenTimes(interpolatedGreenTimes, action, weight);
        }

        interpolatedAction = new TrafficLightControllerParameters(referenceAction.getType(),
                referenceAction.getIds(),
                interpolatedGreenTimes,
                referenceAction.getStrings(),
                referenceAction.getIsInterPhase());

        interpolatedAction.getCycleTime();

        return interpolatedAction;
    }


    private void interpolateGreenTimes(float[] interpolatedGreenTimes, TrafficLightControllerParameters action, double weight) {
        for (int i = 0; i < interpolatedGreenTimes.length; i++) {
            double greenTimeFrac = weight * action.getGreenTimes()[i];
            interpolatedGreenTimes[i] += greenTimeFrac;
        }
    }

    private float[] interpolationSituationToClassifierSituation(Situation queryPoint) {
        float[] classifierSituation = new float[queryPoint.getFlowsOfSignalGroup().size()];
        int dimCount = 0;

        for (Map.Entry<SignalGroup, Double> entry : queryPoint.getFlowsOfSignalGroup().entrySet()) {
            classifierSituation[dimCount++] = entry.getValue().floatValue();
        }

        return classifierSituation;
    }

    private float createInterpolatedPrediction(Map<Classifier, Double> weightsForClassifier) {
        float interpolatedPrediction = 0.0f;

        for (Map.Entry<Classifier, Double> entry : weightsForClassifier.entrySet()) {
            interpolatedPrediction += entry.getKey().getPrediction() * entry.getValue();
        }

        return interpolatedPrediction;
    }

    private float createInterpolatedPredictionError(Map<Classifier, Double> weightsForClassifier) {
        float interpolatedPredictionError = 0.0f;

        for (Map.Entry<Classifier, Double> entry : weightsForClassifier.entrySet()) {
            interpolatedPredictionError += entry.getKey().getPredictionError() * entry.getValue();
        }

        return interpolatedPredictionError;
    }

    private float createInterpolatedFitness(Map<Classifier, Double> weightsForClassifier) {
        float interpolatedFitness = 0.0f;

        for (Map.Entry<Classifier, Double> entry : weightsForClassifier.entrySet()) {
            interpolatedFitness += entry.getKey().getFitness() * entry.getValue();
        }

        return interpolatedFitness;
    }
}
