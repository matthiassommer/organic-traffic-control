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

package de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.evaluation;

import de.dfg.oc.otc.layer0.tlc.TLCTypes;
import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationConstants;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.Situation;
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
 * Created by Dominik on 11.03.2015.
 *
 * Evaluation metric based on webster's formula.
 * Decides if the Interpolation Component's interpolated action will perform better than the action that
 * was actually executed by the SuOC.
 * Then calculates the interpolant's trust level based on this information
 *
 * @author rauhdomi
 *
 */
public class WebstersEvaluationFirstVariant extends AbstractEvaluationMetric
{
    private static final Logger log = Logger.getLogger(WebstersEvaluationFirstVariant.class);

    public static final String RANKING_SUPERIOR = "SUPERIOR";
    public static final String RANKING_EVEN = "EVEN";
    public static final String RANKING_INFERIOR = "INFERIOR";

    // Die Menge aller Aussagen, ob die von der IC vorgeschlagene Aktion besser, gleich oder schlechter gewesen wäre.
    private Map<String, Integer> interpolatedActionRanking;

    /**
     * Standardkonstruktor; weist den Instanzvariablen der Basisklasse entsprechende Werte zu.
     * @see de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.evaluation.AbstractEvaluationMetric
     */
    public WebstersEvaluationFirstVariant() {
        this.id = EvaluationComponent.EvaluationMetrics.WEBSTER_FIRST_VARIANT;
        this.description = "Uses WebsterAVGDelay to compare if the Action from the interpolant is better.";
        this.evaluationValue = 0.5;

        this.interpolatedActionRanking = new HashMap<>();
        resetInterpolatedActionRankings();
    }

    @Override
    /**
     * Siehe Kommentar zur Deklaration innerhalb der Basisklasse.
     * @see xcsr_ic.interpolation.components.evaluation.AbstractEvaluationMetric
     */
    public void evaluate(Situation situation, Object a_exec, Object a_int, double reward, double maxReward)
    {
        double averageDelayTimeIC = 0.0;
        double averageDelayTimeExec = 0.0;

        if((!a_exec.equals(a_int)) && (a_int != null))
        {
            averageDelayTimeIC = websterAverageDelay(situation, (TrafficLightControllerParameters)a_int);
            averageDelayTimeExec = websterAverageDelay(situation, (TrafficLightControllerParameters)a_exec);

            if(averageDelayTimeIC < averageDelayTimeExec)
            {
                int ranking = this.interpolatedActionRanking.get(RANKING_SUPERIOR);
                ranking ++;
                this.interpolatedActionRanking.put(RANKING_SUPERIOR, ranking);
            }
            else if(averageDelayTimeIC > averageDelayTimeExec)
            {
                int ranking = this.interpolatedActionRanking.get(RANKING_INFERIOR);
                ranking ++;
                this.interpolatedActionRanking.put(RANKING_INFERIOR, ranking);
            }
            else
            {
                int ranking = this.interpolatedActionRanking.get(RANKING_EVEN);
                ranking ++;
                this.interpolatedActionRanking.put(RANKING_EVEN, ranking);
            }
        }
        else
        {
            int ranking = this.interpolatedActionRanking.get(RANKING_EVEN);
            ranking ++;
            this.interpolatedActionRanking.put(RANKING_EVEN, ranking);
        }

        calculateTrustValueIfWindowFull();
    }

    private void calculateTrustValueIfWindowFull()
    {
        int overallRankCount = 0;

        for(Map.Entry<String, Integer> entry : interpolatedActionRanking.entrySet())
        {
            overallRankCount += entry.getValue();
        }

        // Nach Erreichen der Evaluationsfenstergröße t_window wird der Evaluationswert neu berechnet
        // und das Evaluationsfenster geleert.
        if(overallRankCount >= InterpolationConstants.t_window) {
            double superiorRankings = this.interpolatedActionRanking.get(RANKING_SUPERIOR);
            int evenRankings = this.interpolatedActionRanking.get(RANKING_EVEN);

            this.evaluationValue = superiorRankings / (overallRankCount - evenRankings);

            if(Double.isNaN(this.evaluationValue)) { this.evaluationValue = 0.0; }

            resetInterpolatedActionRankings();
        }
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

    private void resetInterpolatedActionRankings()
    {
        this.interpolatedActionRanking.clear();
        this.interpolatedActionRanking.put(RANKING_SUPERIOR, 0);
        this.interpolatedActionRanking.put(RANKING_EVEN, 0);
        this.interpolatedActionRanking.put(RANKING_INFERIOR, 0);
    }

    public Map<String, Integer> getInterpolatedActionRanking() {
        return this.interpolatedActionRanking;
    }
}
