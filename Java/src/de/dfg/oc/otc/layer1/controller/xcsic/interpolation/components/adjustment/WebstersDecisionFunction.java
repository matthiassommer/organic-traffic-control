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

package de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.adjustment;

import de.dfg.oc.otc.layer0.tlc.TLCTypes;
import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationComponent;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationComponentException;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationConstants;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.Situation;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants.values.OTCValue;
import de.dfg.oc.otc.layer2.TurningData;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.Phase;
import de.dfg.oc.otc.manager.aimsun.SignalGroup;
import de.dfg.oc.otc.manager.aimsun.Turning;
import org.apache.commons.math3.util.FastMath;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by Dominik on 13.03.2015.
 *
 * Decides if a new site shall be added.
 * In the OTC-System this is always the case, so the decide-Method will always retun true.
 * Here the decide-Method is mainly used to decide if a already existant site of the chosen interpolant
 * shall be superseded by a newer and better performing (calculated through Webster's formula) one.
 *
 * @author rauhdomi
 */
public class WebstersDecisionFunction extends AbstractDecisionFunction {

    private static final Logger log = Logger.getLogger(WebstersDecisionFunction.class);
    private double SUPERSEDE_THRESHOLD_SCALING_FACTOR = 1.0;

    public WebstersDecisionFunction(InterpolationComponent interpolationComponent)
    {
        super(interpolationComponent);
    }

    /**
     * Decides if a new site shall be added.
     * In the OTC-System this is always the case, so the decide-Method will always return true.
     * Here the decide-Method is mainly used to decide if a already existant site of the chosen interpolant
     * shall be superseded by a newer and better performing (calculated through Webster's formula) one.
     *
     * @param situation Die Situation unter der der Reward erzielt wurde
     * @param a_exec Die tatsächlich ausgeführte Aktion a_exec
     * @param a_int Die von der Interpolante vorgeschlagene Aktion a_int
     * @param reward Der tatsächliche Reward vom SuOC
     * @param maxReward Der maximale reward, der erhalten werden kann
     * @return
     */
    @Override
    public boolean decide(Situation situation, Object a_exec, Object a_int, double reward, double maxReward) {

        boolean sitesMaximumReached = this.interpolationComponent.getInterpolant().getSamplingPoints().size()
                >= InterpolationConstants.p_max;

        //Get NeNe of current situation
        OTCValue nearestNeighbor = (OTCValue)a_int;

        if(!sitesMaximumReached
                || (nearestNeighbor.getNearestNeighborAction() == null)
                || (nearestNeighbor.getSituation() == null)) { return true; }

        Situation situationOfNearestSite = nearestNeighbor.getSituation();
        TrafficLightControllerParameters actionOfNearestSite = nearestNeighbor.getNearestNeighborAction();

        double distanceBetweenSituations = calculateEuclideanDistance(situation, situationOfNearestSite);
        boolean shallSupersede = distanceBetweenSituations < calculateSupersedeThreshold();

        if(shallSupersede)
        {
            double avgDelayNewSite = websterAverageDelay(situation, (TrafficLightControllerParameters)a_exec);
            double avgDelayNearestToNewSite = websterAverageDelay(situationOfNearestSite, actionOfNearestSite);

            if(avgDelayNewSite < avgDelayNearestToNewSite)
            {
                try
                {
                    interpolationComponent.deleteSite(situationOfNearestSite);
                }
                catch (InterpolationComponentException e)
                {
                    log.error(e.getMessage());
                }
            }
        }
        //This method returns true so the new site will be added anyway (because it's a valid signal plan from layer 2)
        return true;
    }

    private double websterAverageDelay(Situation situation, TrafficLightControllerParameters action)
    {
        double waitingTime = 0.0;
        double totalDemand = 0.0;

        totalDemand = calculateTotalDemand(situation);

        for(Map.Entry<String, TurningData> turningDataEntry : situation.getTurningDataMap().entrySet())
        {
            String id = turningDataEntry.getKey();
            TurningData turningData = turningDataEntry.getValue();

            double delay = websterAverageDelayPerTurn(turningData.getFlow(),
                    calculateGreenTime(id, action),
                    action.getCycleTime(),
                    turningData.getNumberOfLanes());
            waitingTime += turningData.getFlow() / (totalDemand * delay);
        }

        return waitingTime;
    }

    /**
     * Calculates the average waiting time for a turning according to Webster's
     * formula.
     *
     * @param trafficFlow current flow (veh/h)
     * @param greenTime   effective green time for turning in seconds
     * @param cycleTime   cycle time in seconds
     * @param lanes       number of available lanes for the turning
     * @return average waiting time (in sec)
     */
    private double websterAverageDelayPerTurn(double trafficFlow, final double greenTime, final double cycleTime, final int lanes) {
        // Avoid division by zero
        if (trafficFlow == 0) {
            trafficFlow = 1;
        }

        final double saturationFlow = lanes * 1800;
        // Anteil der effektiven Grünzeit am Umlauf
        final double f = greenTime / cycleTime;
        double degreeOfSaturation = trafficFlow / (f * saturationFlow);

        // Prerequisite: degreeOfSaturation < 1
        if (degreeOfSaturation > 1) {
            degreeOfSaturation = .99;
        }

        final double td = cycleTime * FastMath.pow(1 - f, 2) / (2 * (1 - trafficFlow / saturationFlow));

        return 0.9 * (td + 1800 * FastMath.pow(degreeOfSaturation, 2) / (trafficFlow * (1 - degreeOfSaturation)));
    }

    private double calculateTotalDemand(Situation situation)
    {
        double totalDemand = 0.0;

        for(TurningData td : situation.getTurningDataMap().values())
        {
            totalDemand += td.getFlow();
        }
        return totalDemand;
    }

    private double calculateGreenTime(String turnDataId, TrafficLightControllerParameters action)
    {
        Map<SignalGroup, Double> greenTimesForSignalGroups = getGreenTimeForSignalGroups(action.getGreenTimes(),
                action.getType(),
                action.getIds());
        double greenTimeForTurningOfSignalGroup = 0;

        boolean signalGroupForTurningFound = false;

        //Determine to which signal group and thus green time the turning belongs to
        for(Map.Entry<SignalGroup, Double> greenTimeForSignalGroup : greenTimesForSignalGroups.entrySet())
        {
            for(Turning t : greenTimeForSignalGroup.getKey().getTurnings())
            {
                String turningId = t.getInSection().getId() + ";" + t.getOutSection().getId();

                if(turnDataId.equals(turningId))
                {
                    greenTimeForTurningOfSignalGroup = greenTimeForSignalGroup.getValue();
                    signalGroupForTurningFound = true;
                    break;
                }
            }
            if(signalGroupForTurningFound) { break; }
        }

        return greenTimeForTurningOfSignalGroup;
    }

    private Map<SignalGroup, Double> getGreenTimeForSignalGroups(float[] greenTimes, int tlcType, int[] ids) {

        Map<SignalGroup, Double> greenTimeForSignalGroupsAll = new HashMap<>();
        Map<SignalGroup, Double> greenTimeForSignalGroupsCleaned = new HashMap<>();

        if (tlcType == TLCTypes.FIXEDTIME || tlcType == TLCTypes.NEMA)
        {
            final AimsunJunction junction = OTCManager.getInstance().getNetwork().getJunction(ids[0]);
            final List<Phase> phases = junction.getPhases();

            for (Phase phase : phases)
            {
                List<SignalGroup> signalGroups = phase.getSignalGroups();

                for (SignalGroup signalGroup : signalGroups) {

                    if(!greenTimeForSignalGroupsAll.containsKey(signalGroup))
                    {
                        greenTimeForSignalGroupsAll.put(signalGroup, (double)greenTimes[phase.getId() - 1]);
                    }
                    else
                    {
                        double greenTime = greenTimeForSignalGroupsAll.get(signalGroup);
                        greenTime += greenTimes[phase.getId() - 1];
                        greenTimeForSignalGroupsAll.put(signalGroup, greenTime);
                    }

                }
            }

            for(Map.Entry<SignalGroup, Double> greenTimeForSignalGroup : greenTimeForSignalGroupsAll.entrySet())
            {
                if(greenTimeForSignalGroup.getValue() != 0)
                {
                    greenTimeForSignalGroupsCleaned.put(greenTimeForSignalGroup.getKey(), greenTimeForSignalGroup.getValue());
                }
            }
        }
        else
        {
            log.warn("Method not available for this TLC type.");
        }

        return greenTimeForSignalGroupsCleaned;
    }

    private double calculateEuclideanDistance(Situation x_i, Situation x_q)
    {
        double sum = 0.0;

        for(Map.Entry<SignalGroup, Double> x_iFlow : x_i.getFlowsOfSignalGroup().entrySet())
        {
            for(Map.Entry<SignalGroup, Double> x_qFlow : x_q.getFlowsOfSignalGroup().entrySet())
            {
                String x_iID = x_iFlow.getKey().toString();
                String x_qID = x_qFlow.getKey().toString();

                if(x_iID.equals(x_qID))
                {
                    sum += Math.pow(x_iFlow.getValue() - x_qFlow.getValue(), 2);
                    break;
                }
            }
        }

        return Math.sqrt(sum);
    }

    private double calculateSupersedeThreshold()
    {
        Set<Situation> setOfSamplingPoints = this.interpolationComponent.getInterpolant().getSamplingPoints();

        //Calculate the average distance between the Situations used as sites of the interpolant
        Situation[] samplingPoints = setOfSamplingPoints.toArray(new Situation[setOfSamplingPoints.size()]);
        int possibleSiteCombinations = (samplingPoints.length * (samplingPoints.length - 1)) / 2;
        double averageDistance = 0.0;
        double sumOfDistances = 0.0;

        for(int i = 0; i < samplingPoints.length - 1; i++)
        {
            for(int u = i + 1; u < samplingPoints.length; u++)
            {
                sumOfDistances += calculateEuclideanDistance(samplingPoints[i], samplingPoints[u]);
            }
        }

        averageDistance = sumOfDistances / possibleSiteCombinations;

        return averageDistance * SUPERSEDE_THRESHOLD_SCALING_FACTOR;
    }
}
