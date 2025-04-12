package de.dfg.oc.otc.aid.disturbance;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer0.tlc.AbstractTLC;
import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.Phase;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.SignalGroup;
import de.dfg.oc.otc.manager.aimsun.Turning;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class is used to create a new TLC for a disturbed intersection
 * and to reset it after the disturbance is gone.
 *
 * @author leinsinger
 */
public class DisturbanceTLCCreator {
    /**
     * The fractal of the available greentime
     */
    private float lambda;
    /**
     * The TLC before the change
     */
    private AbstractTLC oldTLC;
    /**
     * The TLC after the change
     * used to check if TLC is still in use before resetting it.
     */
    private AbstractTLC agreedTLC;
    /**
     * The OTCNode that initiates the TLC-Change
     */
    private OTCNode node;
    /**
     * Shows whether the TLC is changed or not
     */
    private boolean alreadyChanged;
    /**
     * The disturbance that caused the actual TLC-Change
     */
    private Disturbance causedDisturbance;
    /**
     * Shows all detected disturbances on this node
     */
    private List<Disturbance> detectedDisturbances;
    /**
     * Minimum green time in seconds according to RiLSA
     */
    private final int minimumGreenTime = 5;

    private enum Code {
        extend, shorten, nothing
    }

    private DisturbanceTLCCreator() {
    }

    /**
     * @param node that initiates the TLC-Change
     */
    public DisturbanceTLCCreator(final OTCNode node) {
        this.node = node;
        this.detectedDisturbances = new ArrayList<>(5);
        this.lambda = DefaultParams.DIST_TLC_LAMBDA;
    }

    /**
     * Check which of the active disturbances is the worst on the section.
     *
     * @return worstDisturbance
     */
    private Disturbance checkForWorstDisturbance() {
        float maxDegree = -0.1f;
        float minStartTime = 0;
        Disturbance worstDisturbance = null;

        for (Disturbance disturbance : detectedDisturbances) {
            // The higher the degree the worse the disturbance
            if (disturbance.getDegree() > maxDegree) {
                maxDegree = disturbance.getDegree();
                minStartTime = disturbance.getStart();
                worstDisturbance = disturbance;
            }
            // if same degree, the earlier disturbance is worse
            else if (disturbance.getDegree() == maxDegree) {
                if (disturbance.getStart() < minStartTime) {
                    maxDegree = disturbance.getDegree();
                    minStartTime = disturbance.getStart();
                    worstDisturbance = disturbance;
                }
            }
        }
        return worstDisturbance;
    }

    /**
     * Method to change the green times of a TLC due to a disturbance.
     * Shorten green times for section incoming turnings, extend green times for other turnings.
     * Cycle time stays the same.
     * called by OTCNode every time a new disturbance is built.
     * called by resetTLC if there is still a disturbance.
     *
     * @param disturbance the disturbance that called the Method
     * @param recall             shows if method is called from a new disturbance (false) or an old one (true)
     */
    public void adjustGreenTimesForIncomingIntersection(Disturbance disturbance, boolean recall) {
        if (node.getJunction().isControlled()) {
            if (!detectedDisturbances.contains(disturbance)) {
                detectedDisturbances.add(disturbance);
            }

            // if there is no change, the new greentimes can be calculated
            if (!alreadyChanged) {
                // if it is a new call, the old TLC is saved
                if (!recall) {
                    oldTLC = node.getJunction().getActiveTLC();
                }
                TrafficLightControllerParameters oldParams = oldTLC.getParameters();

                int affectedSectionID = disturbance.getLinkIncident().getSectionID();
                float[] newGreentimes = calculateNewGreentimesIncomingIntersection(oldParams.clone(), affectedSectionID, disturbance.getDegree());

                logMessage("Disturbance on Section  " + affectedSectionID + " forces TLC change on node " + node.getId());

                this.causedDisturbance = disturbance;

                // build new TLC
                TrafficLightControllerParameters newParams = new TrafficLightControllerParameters(oldParams.getType(),
                        oldParams.getIds(), newGreentimes, oldParams.getStrings(), oldParams.getIsInterPhase());

                // force immediate TLC change
                node.setTLCChangeDelay(1);
                node.changeTLC(newParams);

                // save new TLC for later check
                this.agreedTLC = node.getJunction().getActiveTLC();
                this.alreadyChanged = true;
            }
        }
    }

    /**
     * Increase the green time for phases of outgoing turnings of the congested section.
     */
    public void adjustGreenTimesForOutgoingIntersection(Disturbance disturbance, Section section, boolean recall) {
        if (node.getJunction().isControlled()) {
            if (!detectedDisturbances.contains(disturbance)) {
                detectedDisturbances.add(disturbance);
            }

            // if there is no change, the new greentimes can be calculated
            if (!alreadyChanged) {
                // if it is a new call, the old TLC is saved
                if (!recall) {
                    oldTLC = node.getJunction().getActiveTLC();
                }
                TrafficLightControllerParameters oldParams = oldTLC.getParameters();

                float[] newGreentimes = calculateNewGreenTimesOutgoingIntersection(oldParams.clone(), section.getId(), disturbance.getDegree());

                logMessage("Disturbance on Section  " + section.getId() + " forces TLC change on node " + node.getId());

                this.causedDisturbance = disturbance;

                // build new TLC
                TrafficLightControllerParameters newParams = new TrafficLightControllerParameters(oldParams.getType(),
                        oldParams.getIds(), newGreentimes, oldParams.getStrings(), oldParams.getIsInterPhase());

                node.setTLCChangeDelay(1);
                node.changeTLC(newParams);

                // save new TLC for later check
                this.agreedTLC = node.getJunction().getActiveTLC();
                this.alreadyChanged = true;
            }
        }
    }

    private void logMessage(String message) {
        OTCManager.getInstance().newInfo(OTCManager.getInstance().getTime() + message);
        System.out.println(message);
    }

    /**
     * Method to reset TLC
     * called by OTCNode every time a disturbance is stopped
     *
     * @param endingDisturbance the disturbance that has been stopped
     */
    public void resetTLC(Disturbance endingDisturbance) {
        if (node.getJunction().isControlled()) {
            if (detectedDisturbances.contains(endingDisturbance)) {
                detectedDisturbances.remove(endingDisturbance);
            }

            if (alreadyChanged) {
                // if last change was caused by this disturbance it will be resetted
                if (endingDisturbance.equals(causedDisturbance)) {
                    AbstractTLC tempTLC = node.getJunction().getActiveTLC();

                    // if the plan has changed by DPSS, it will be still used
                    if (!oldTLC.equals(tempTLC) && !agreedTLC.equals(tempTLC)) {
                        oldTLC = tempTLC;
                    }

                    alreadyChanged = false;

                    // if there are other disturbances, a new change will be initiated
                    if (!detectedDisturbances.isEmpty()) {
                        adjustGreenTimesForIncomingIntersection(checkForWorstDisturbance(), true);
                    } else {
                        // enable TLC-changing (well it's cheating)
                        float temp = node.getTLCChangeDelay();

                        node.setTLCChangeDelay(1);
                        node.changeTLC(oldTLC.getParameters());

                        logMessage(" TLC on node " + node.getId() + " resetted");

                        node.setTLCChangeDelay(temp);
                    }
                }
            }
        }
    }

    /**
     * Method to calculate new green times. keep number of phases and interphases.
     * Shorten phases going into the congested section.
     *
     * @param oldParams old Parameters as base
     * @param sectionID the disturbed section
     * @param degree
     * @return new greentimes
     */
    private float[] calculateNewGreentimesIncomingIntersection(TrafficLightControllerParameters oldParams, int sectionID, float degree) {
        final int numPhases = node.getJunction().getNumPhases();
        boolean[] interphases = oldParams.getIsInterPhase();
        float[] greenTimes = oldParams.getGreenTimes();

        // array to show the type of change
        int[] code = new int[numPhases];

        List<Integer> affectedPhases = new ArrayList<>();
        affectedPhases.addAll(getPhasesForOutgoingSection(sectionID));

        int phasesToExtend = numPhases;
        int phasesToShorten = 0;

        for (int i = 0; i < numPhases; ++i) {
            code[i] = Code.extend.ordinal();

            if (interphases[i]) {
                code[i] = Code.nothing.ordinal();
                phasesToExtend--;
            }

            // affected phases will be shortened
            if (affectedPhases.contains(i + 1)) {
                if (greenTimes[i] > minimumGreenTime + 1) {
                    code[i] = Code.shorten.ordinal();
                    phasesToShorten++;
                } else {
                    code[i] = Code.nothing.ordinal();
                }
                phasesToExtend--;
            }
        }

        int bonus = (int) (lambda * getMaximalChangeTime(greenTimes, code));

        if (bonus == 0 || phasesToShorten == 0 || phasesToExtend == 0) {
            return oldParams.getGreenTimes();
        }
        return adjustGreenTimes(code, bonus, numPhases, phasesToShorten, phasesToExtend, greenTimes, degree);
    }

    /**
     * Turnings outgoing from the disturbed section have to be lenghtend in terms of their green times.
     *
     * @return adjusted green times
     */
    private float[] calculateNewGreenTimesOutgoingIntersection(TrafficLightControllerParameters oldParams, int sectionID, float degree) {
        final int numPhases = node.getJunction().getNumPhases();
        boolean[] interphases = oldParams.getIsInterPhase();
        float[] greenTimes = oldParams.getGreenTimes();

        // array to show the type of change
        int[] code = new int[numPhases];

        List<Integer> affectedPhases = new ArrayList<>();
        affectedPhases.addAll(getPhasesForIncomingSection(sectionID));

        int phasesToExtend = 0;
        int phasesToShorten = numPhases;

        for (int i = 0; i < numPhases; ++i) {
            code[i] = Code.nothing.ordinal();

            if (affectedPhases.contains(i + 1)) {
                // extend outgoing phases
                code[i] = Code.extend.ordinal();
                phasesToExtend++;
            } else if (greenTimes[i] > minimumGreenTime + 1) {
                // shorten other phases
                code[i] = Code.shorten.ordinal();
                phasesToShorten--;
            } else if (interphases[i]) {
                phasesToShorten--;
            }
        }

        int bonus = (int) (lambda * getMaximalChangeTime(greenTimes, code));

        if (bonus == 0 || phasesToShorten == 0 || phasesToExtend == 0) {
            return oldParams.getGreenTimes();
        }
        return adjustGreenTimes(code, bonus, numPhases, phasesToShorten, phasesToExtend, greenTimes, degree);
    }

    private float[] adjustGreenTimes(int[] code, int bonus, int numPhases, int phasesToShorten, int phasesToExtend, float[] greenTimes, float degree) {
        logMessage("Disturbance: Change time for TLC (sec): " + bonus);

        // changing time: bonus = malus, to keep cycle time
        int malus = bonus;

        //bonus > malus: extend cycle time in accordance to degree (high degree--> more bonus)
        if(!Float.isNaN(degree))
            bonus += 10 * degree;

        int malusPerPhase = malus < phasesToShorten ? 1 : malus / phasesToShorten;
        int bonusPerPhase = bonus < phasesToExtend ? 1 : bonus / phasesToExtend;

        for (int i = 0; i < numPhases; i++) {
            // extending
            if (code[i] == Code.extend.ordinal() && bonus > 0) {
                greenTimes[i] += bonusPerPhase;
                bonus -= bonusPerPhase;
            }
            // shorten phase
            else if (code[i] == Code.shorten.ordinal() && malus > 0) {
                if (greenTimes[i] - malusPerPhase >= minimumGreenTime) {
                    greenTimes[i] -= malusPerPhase;
                    malus -= malusPerPhase;
                } else {
                    greenTimes[i] = minimumGreenTime;
                }
            }
        }
        return greenTimes;
    }

    /**
     * Method to calculate the possible time to shorten phases.
     *
     * @param greenTimes the actual green times
     * @param code       the type of changing
     * @return possible change time
     */
    private float getMaximalChangeTime(float[] greenTimes, int[] code) {
        float sum = 0;
        for (int i = 0; i < greenTimes.length; i++) {
            if (code[i] == Code.shorten.ordinal()) {
                sum += greenTimes[i] - minimumGreenTime;
            }
        }
        return sum;
    }

    /**
     * Method to get the associated phases to a given outgoing section.
     *
     * @param sectionID destination section
     * @return associated phases
     */
    private List<Integer> getPhasesForOutgoingSection(final int sectionID) {
        final List<Integer> phasesForSection = new ArrayList<>();
        final List<Phase> phases = node.getJunction().getPhases();

        for (Phase phase : phases) {
            List<SignalGroup> signalGroups = phase.getSignalGroups();
            for (SignalGroup signalGroup : signalGroups) {
                List<Turning> turnings = signalGroup.getTurnings();
                phasesForSection.addAll(turnings.stream().filter(turning -> turning.getOutSection().getId() == sectionID).map(turning -> phase.getId()).collect(Collectors.toList()));
            }
        }

        return phasesForSection;
    }

    /**
     * Method to get the associated phases to a given incoming section.
     *
     * @param sectionID destination section
     * @return associated phases
     */
    private List<Integer> getPhasesForIncomingSection(final int sectionID) {
        final List<Integer> phasesForSection = new ArrayList<>();
        final List<Phase> phases = node.getJunction().getPhases();

        for (Phase phase : phases) {
            List<SignalGroup> signalGroups = phase.getSignalGroups();
            for (SignalGroup signalGroup : signalGroups) {
                List<Turning> turnings = signalGroup.getTurnings();
                phasesForSection.addAll(turnings.stream().filter(turning -> turning.getInSection().getId() == sectionID).map(turning -> phase.getId()).collect(Collectors.toList()));
            }
        }

        return phasesForSection;
    }
}
