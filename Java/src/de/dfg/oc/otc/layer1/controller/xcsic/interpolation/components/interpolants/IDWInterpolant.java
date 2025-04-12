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

package de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants;

import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationComponentException;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.Situation;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants.values.InterpolationValue;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationConstants;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants.values.AccumulatedIDWOTCValue;
import de.dfg.oc.otc.manager.aimsun.SignalGroup;
import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Anthony on 10.02.2015.
 *
 * Interpolates TrafficLightControllerParameters based on inverse distance weighting
 *
 * @author Anthony Stein
 * @author Dominik Rauh
 */
public class IDWInterpolant extends AbstractInterpolationTechnique
{
    private Map<Situation, TrafficLightControllerParameters> actionsForSituations;
    private final int POWER = InterpolationConstants.IDW_EXPO;

    public IDWInterpolant() {
        actionsForSituations = new HashMap<>();
    }

    @Override
    public InterpolationValue interpolate(Situation queryPoint) throws InterpolationComponentException
    {
        this.lastQueryPoint = queryPoint;

        Map<TrafficLightControllerParameters, Double> weightsForAction = new HashMap<>();
        Map<Situation, Double> weightsForSituations = this.calculateWeights(queryPoint);

        double sum_weights = 0.0;

        for(Object sample : samplingPoints)
        {
            TrafficLightControllerParameters assignedAction = this.actionsForSituations.get(sample);

            if(!weightsForAction.containsKey(assignedAction))
            {
                weightsForAction.put(assignedAction, weightsForSituations.get(sample));
            }
            else
            {
                double accWeightForAction = weightsForAction.get(assignedAction);
                accWeightForAction += weightsForSituations.get(sample);
                weightsForAction.put(assignedAction, accWeightForAction);
            }
            sum_weights += weightsForSituations.get(sample);
        }

        TrafficLightControllerParameters interpolatedAction = null;
        Situation neNeSituation = getNearestNeighborSituation(weightsForSituations);
        TrafficLightControllerParameters neNeAction = null;

        if(samplingPoints.size() > 0)
        {
            for(TrafficLightControllerParameters action : weightsForAction.keySet())
            {
                double accWeightForActionHash = weightsForAction.get(action);
                accWeightForActionHash /= sum_weights;
                weightsForAction.put(action, accWeightForActionHash);
            }

            interpolatedAction = createInterpolatedAction(weightsForAction);
            neNeAction = getNearestNeighborAction(weightsForAction);
        }

        this.interpolatedValue = new AccumulatedIDWOTCValue(neNeSituation, neNeAction, interpolatedAction, weightsForAction);

        return this.interpolatedValue;
    }

    @Override
    public void addNewSite(Situation situation, Object[] attributes) throws InterpolationComponentException
    {
        if(this.samplingPoints.size() >= InterpolationConstants.p_max)
        {
            return;
        }

        TrafficLightControllerParameters assignedAction = null;

        if(attributes.length > 0)
        {
            assignedAction = (TrafficLightControllerParameters)attributes[0];
        }

        if(assignedAction == null) { throw new InterpolationComponentException("No action assigned to new site!"); }
        if(this.samplingPoints.contains(situation) || this.actionsForSituations.containsKey(situation))
        {
            throw new InterpolationComponentException("Situation already exists in the set of sampling points!");
        }

        this.actionsForSituations.put(situation, assignedAction);
        this.samplingPoints.add(situation);
    }

    @Override
    public void removeSite(Object siteToRemove) throws InterpolationComponentException
    {
        boolean siteWasInSamplingPoints = false;

        if(this.samplingPoints.remove(siteToRemove)) { siteWasInSamplingPoints = true; }
        if(this.actionsForSituations.remove(siteToRemove) != null) { siteWasInSamplingPoints = true; }

        if(!siteWasInSamplingPoints)
        {
            throw new InterpolationComponentException("Situation wasn't in the set of sampling points!");
        }
    }

    private Situation getNearestNeighborSituation(Map<Situation, Double> weightsForSituation)
    {
        Situation neNeSituation = null;
        double weightOfNeNe = Double.MIN_VALUE;

        for(Map.Entry<Situation, Double> entry : weightsForSituation.entrySet())
        {
            if(entry.getValue() >= weightOfNeNe)
            {
                weightOfNeNe = entry.getValue();
                neNeSituation = entry.getKey();
            }
        }

        return neNeSituation;
    }

    private TrafficLightControllerParameters getNearestNeighborAction(
            Map<TrafficLightControllerParameters, Double> weightsForAction)
    {
        TrafficLightControllerParameters neNeAction = null;
        double weightOfNeNe = Double.MIN_VALUE;

        for(Map.Entry<TrafficLightControllerParameters, Double> entry : weightsForAction.entrySet())
        {
            if(entry.getValue() >= weightOfNeNe)
            {
                weightOfNeNe = entry.getValue();
                neNeAction = entry.getKey();
            }
        }

        return neNeAction;
    }

    private HashMap<Situation, Double> calculateWeights(Situation queryPoint) throws InterpolationComponentException
    {
        HashMap<Situation, Double> distanceWeights = new HashMap<>(this.samplingPoints.size());

        for (Object sample : this.samplingPoints)
        {
            Situation x_i = (Situation)sample;
            double w_i = Math.pow(1 / calculateEuclideanDistance(x_i, queryPoint), POWER);
            distanceWeights.put(x_i, w_i);
        }
        return distanceWeights;
    }

   /* private double calculateEuclideanDistance(Situation x_i, Situation x_q) {
        double sum = 0.0;

        double[] flowsOfSamplingPoint = ArrayUtils.toPrimitive(
                x_i.getFlowsOfSignalGroup().values().toArray(new Double[x_i.getFlowsOfSignalGroup().size()]));
        double[] flowsOfQueryPoint = ArrayUtils.toPrimitive(
                x_q.getFlowsOfSignalGroup().values().toArray(new Double[x_q.getFlowsOfSignalGroup().size()]));

        for(int i = 0; i < flowsOfSamplingPoint.length; i++) {
            sum += Math.pow(flowsOfSamplingPoint[i] - flowsOfQueryPoint[i], 2);
        }

        return Math.sqrt(sum);
    }*/

    private double calculateEuclideanDistance(Situation x_i, Situation x_q) throws InterpolationComponentException
    {
        double sum = 0.0;
        int dimCount = 0;

        for(Map.Entry<SignalGroup, Double> x_iFlow : x_i.getFlowsOfSignalGroup().entrySet())
        {
            for(Map.Entry<SignalGroup, Double> x_qFlow : x_q.getFlowsOfSignalGroup().entrySet())
            {
                String x_iID = x_iFlow.getKey().toString();
                String x_qID = x_qFlow.getKey().toString();

                if(x_iID.equals(x_qID))
                {
                    sum += Math.pow(x_iFlow.getValue() - x_qFlow.getValue(), 2);
                    dimCount++;
                    break;
                }
            }
        }

        if(dimCount != x_i.getFlowsOfSignalGroup().size())
        {
            throw new InterpolationComponentException(
                    "Situation of query point doesn't have the same dimensions as the sampling points");
        }


        return Math.sqrt(sum);
    }

    private TrafficLightControllerParameters createInterpolatedAction(
            Map<TrafficLightControllerParameters, Double> weightsForAction)
    {
        TrafficLightControllerParameters interpolatedAction = null;
        TrafficLightControllerParameters referenceAction = weightsForAction.keySet().iterator().next();

        int greenTimesCount = referenceAction.getGreenTimes().length;
        float[] interpolatedGreenTimes = new float[greenTimesCount];

        for(Map.Entry<TrafficLightControllerParameters, Double> entry : weightsForAction.entrySet())
        {
            TrafficLightControllerParameters action = entry.getKey();
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

    private void interpolateGreenTimes(float[] interpolatedGreenTimes, TrafficLightControllerParameters action, double weight)
    {
        for(int i = 0; i < interpolatedGreenTimes.length; i++)
        {
            double greenTimeFrac = weight * action.getGreenTimes()[i];
            interpolatedGreenTimes[i] += greenTimeFrac;
        }
    }
}
