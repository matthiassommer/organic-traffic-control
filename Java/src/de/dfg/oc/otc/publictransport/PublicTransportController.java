package de.dfg.oc.otc.publictransport;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer0.tlc.AbstractTLC;
import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer0.tlc.fixedTimeController.FixedTimeController;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.Phase;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class manages the signal change for a public transport vehicle.
 * It holds the necessary information and computes the signal change.
 */
public class PublicTransportController {
    /**
     * The offset added to the estimated arrival time at junction (in seconds).
     */
    private final float offsetTime;
    /**
     * The assigned detector.
     */
    private final Detector detector;
    /**
     * The assigned junction.
     */
    private final AimsunJunction junction;
    /**
     * The section that leads to the junction.
     */
    private final Section section;
    /**
     * The ID of public transport line. This is needed for routing.
     */
    private final int lineID;
    /**
     * This contains all the phases for green times for this turning.
     */
    private final List<Phase> greenPhases;
    /**
     * The distance of the Detector to the End of the section.
     */
    private final float detectorDistanceToExit;
    private final PublicTransportComponent publicTransportComponent;
    /**
     * This contains all phases of this junction.
     */
    private List<Phase> allPhases;
    /**
     * The standard Traffic Light Controller.
     */
    private AbstractTLC defaultTLC;
    /**
     * The total number of phases.
     */
    private int numberPhases;
    /**
     * The total number of phases that are not Interphases.
     */
    private int numberOfNonInterphases;
    /**
     * The duration in seconds for all phases.
     */
    private List<Float> durations;
    /**
     * List of the IDs of the phases.
     */
    private List<Integer> phaseIDs;
    /**
     * True if the TLC was changed.
     */
    private boolean tempTLCActive;
    /**
     * The ID of the current active phase.
     */
    private int currentPhaseID;
    /**
     * True, if phase switches are permitted.
     */
    private boolean phaseRotationEnabled;
    /**
     * True, if Early Red and Extended Green operations are permitted.
     */
    private boolean phaseLengthChangeEnabled;
    /**
     * True if the phases are switched instead of moved.
     */
    private boolean switching;
    /**
     * Number of signal changes at this PublicTransportController.
     */
    private int numberOfPhaseChanges;
    /**
     * Number of PhaseChanges, that were denied because of restrictions
     * to periodic changes of TLCs.
     */
    private int deniedPhaseChanges;

    /**
     * This class controls the traffic light signal change for public transport vehicels.
     *
     * @param detector    the detector
     * @param junction    the to be controlled junction
     * @param greenPhases the greenphases
     * @param section     the section leading to the junction
     * @param lineID      the ID of the public transport line
     */
    PublicTransportController(Detector detector, AimsunJunction junction, List<Phase> greenPhases, Section section, int lineID) {
        this.detector = detector;
        this.junction = junction;
        this.greenPhases = greenPhases;
        this.section = section;
        this.lineID = lineID;
        this.publicTransportComponent = new PublicTransportComponent(this);
        this.detectorDistanceToExit = this.section.getLength() - this.detector.getPositionBegin();
        this.deniedPhaseChanges = 0;
        this.numberOfPhaseChanges = 0;
        this.offsetTime = DefaultParams.PT_OFFSET;

        setDefaultTLC();

        changeFeatureSetting(DefaultParams.PT_FEATURE_SETTING);
        setPhaseRotationMethod(DefaultParams.PT_PHASE_CHANGE_METHOD);
    }

    /**
     * Called when {@Link Detector} detects a public transport vehicle.
     *
     * @param speed the current speed of the detected vehicle
     */
    public void detectedPublicTransport(float speed) throws OTCManagerException {
        if (this.tempTLCActive || OTCManager.getInstance().getTime() < this.junction.getNode().getTLCChangeDelay()) {
            if (phaseLengthChangeEnabled || phaseRotationEnabled) {
                deniedPhaseChanges++;

                String message = "<html><b><font color='red'>Phase Change denied, not ready!</font></b></html>";
                publicTransportComponent.sendMessageToGUI(message);
            }
            return;
        }

        if (speed <= 0) {
            throw new OTCManagerException("Invalid measured speed.");
        }

        setDefaultTLC();
        this.currentPhaseID = this.junction.getCurrentPhase();

        // The time the vehicle needs to reach the junction.
        float drivingTimeToTrafficLight = detectorDistanceToExit / speed + this.offsetTime;

        // The remaining time of the phase.
        float phaseTimeRemaining = this.junction.getPhaseById(this.junction.getCurrentPhase()).getDefaultDuration() - (OTCManager.getInstance().getTime() - this.junction.getActiveTLC().getTimeOfLastChange());

        // The difference between the time the vehicle needs to reach the signal and the remaining signal time. Positive means vehicle will reach junction after signal change, Negative means vehicle will reach junction before signal change.
        float adjustmentTime = Math.round(drivingTimeToTrafficLight - phaseTimeRemaining);

        boolean signalIsGreen = this.greenPhases.contains(this.junction.getPhaseById(this.currentPhaseID));
        boolean isInterphase = this.junction.getPhaseById(this.currentPhaseID).isInterphase();
        if (signalIsGreen) {
            /**
             * At time of vehicle arrival, phase lasts longer than offset,
             * so adjustmentTime must be added to phase duration
             */
            if (adjustmentTime > 0) {
                phaseLengthAdjustment(adjustmentTime);
            }
        } else {
            if (adjustmentTime >= 0) {
                /**
                 * If vehicle reaches junction after signal change,
                 * check for phase switch.
                 * The if statement is necessary so that the TLC is only changed when there was actually a phase switch.
                 */
                if (phaseRotation()) {
                    activateTempTLC();
                }
            } else {
                /**
                 * If vehicle reaches junction before signal change,
                 * phase duration must be shortened
                 */
                if (!isInterphase) {
                    phaseLengthAdjustment(adjustmentTime); // adjustmentTime is negative!
                }
            }
        }
    }

    /**
     * This function switches the individuell Transit Signal Priority Techniques on and off.
     *
     * @param setting 0: deactivated, 1: activated, 2: just phase duration change, 3: just phase rotation
     */
    private void changeFeatureSetting(int setting) {
        switch (setting) {
            case 0:
                this.phaseLengthChangeEnabled = false;
                this.phaseRotationEnabled = false;
                break;
            case 1:
                this.phaseLengthChangeEnabled = true;
                this.phaseRotationEnabled = true;
                break;
            case 2:
                this.phaseLengthChangeEnabled = true;
                this.phaseRotationEnabled = false;
                break;
            case 3:
                this.phaseLengthChangeEnabled = false;
                this.phaseRotationEnabled = true;
                break;
            default:
                this.phaseLengthChangeEnabled = false;
                this.phaseRotationEnabled = false;
        }
    }

    /**
     * Sets the mode of PhaseRotation
     *
     * @param setting 0: switch the phases, 1: move the phase
     */
    private void setPhaseRotationMethod(int setting) {
        switch (setting) {
            case 0:
                this.switching = true;
                break;
            case 1:
                this.switching = false;
                break;
            default:
                this.switching = false;
        }
    }

    /**
     * This method extends the current phase for the given float value
     * and decreases the other phases so that the total cycle time remains unchanged.
     *
     * @param adjustment the amount of time the phase is extended
     * @return the result state: -1= a temporary TLC is currently active, 1:no change was necessary, 2= Green Extension
     */
    private void phaseLengthAdjustment(float adjustment) {
        /**
         * If adjustment is longer than half the phase do nothing (or switch phases).
         */
        if (Math.abs(adjustment) > this.junction.getPhaseById(this.junction.getCurrentPhase()).getDefaultDuration() / 2) {
            if (phaseRotation()) {
                activateTempTLC();
                return;
            }
        }

        if (this.phaseLengthChangeEnabled) {
            float oldDuration = -1;

            /**
             * Add adjustment to the duration of current phase
             * and remove it from the other phases in equal parts
             * so that the overall cycle time remains the same
             */
            float temp = Math.round(adjustment / (this.numberOfNonInterphases - 1));
            if (temp == 0) {
                return;
            }

            for (int i = 0; i < this.numberPhases; i++) {
                float phaseDuration = this.durations.get(i);

                if (this.allPhases.get(i).getId() == this.currentPhaseID) {
                    oldDuration = phaseDuration;

                    phaseDuration += adjustment;
                    if (phaseDuration < 1) {
                        phaseDuration = 1;
                    }
                    this.durations.set(i, phaseDuration);
                } else if (!this.allPhases.get(i).isInterphase()) {
                    phaseDuration -= temp;
                    if (phaseDuration < 1) {
                        phaseDuration = 1;
                    }
                    this.durations.set(i, phaseDuration);
                }
            }

            checkCycleTime();

            sendPhaseAdjustmentToGUI(adjustment, oldDuration);

            phaseRotation();
            activateTempTLC();
        }
    }

    private void sendPhaseAdjustmentToGUI(float adjustment, float oldDuration) {
        String value = "";
        if (adjustment >= 0) {
            value += "+";
        }
        value += Math.round(adjustment * 100) / 100f + " sec.";

        String message = "<html><b><font color='green'>Phase " + (getIndexOfPhase(this.currentPhaseID) + 1) + " changed duration by " + value + " (old value: " + oldDuration + " sec.)</font></b></html>";
        this.publicTransportComponent.sendMessageToGUI(message);
    }

    /**
     * This method checks if the total cycleTime is still correct and adjusts the phases if necessary
     */
    private void checkCycleTime() {
        int size = durations.size();
        int indexOfLastPhase = (getIndexOfPhase(this.currentPhaseID) + size - 1) % size;

        if (allPhases.get(indexOfLastPhase).isInterphase()) {
            indexOfLastPhase = (indexOfLastPhase + size - 1) % size;
        }

        float durationsTotal = 0;
        for (float duration : this.durations) {
            durationsTotal += duration;
        }

        float phaseDuration = this.durations.get(indexOfLastPhase);
        phaseDuration += (float) this.junction.getActiveTLC().getCycleTime();
        phaseDuration -= durationsTotal;

        if (phaseDuration < 1) {
            phaseDuration = 1;
        }

        this.durations.set(indexOfLastPhase, phaseDuration);
    }

    /**
     * This method moves the green phase to the next possible index,
     * so that the vehicle will get an Early Green
     * WARNING: phaseRotation doesn't trigger a TLC change!
     *
     * @return changed true if the phases were rotated
     */
    private boolean phaseRotation() {
        int indexOfCurrentPhase = getIndexOfPhase(this.currentPhaseID);
        if (this.phaseRotationEnabled) {
            this.phaseIDs.clear();
            for (int i = 0; i < this.numberPhases; i++) {
                this.phaseIDs.add(this.allPhases.get(i).getId());
            }

            int nextIndex = (indexOfCurrentPhase + 1) % this.numberPhases;
            if (this.allPhases.get(nextIndex).isInterphase()) {
                nextIndex = (nextIndex + 1) % this.numberPhases;
            }

            boolean signalIsGreen = this.greenPhases.contains(this.junction.getPhaseById(this.currentPhaseID));
            boolean nextSignalIsRed = !this.greenPhases.contains(this.allPhases.get(nextIndex));
            if (nextSignalIsRed && !signalIsGreen) {
                int indexOfGreenPhase = getIndexOfGreenPhase(indexOfCurrentPhase);

                if (switching) {
                    switchPhases(nextIndex, indexOfGreenPhase);
                } else {
                    movePhase(nextIndex, indexOfGreenPhase);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Moves the entries of PhaseIds and durations at Index indexOfGreenPhase to the Index of nextIndex
     *
     * @param nextIndex
     * @param indexOfGreenPhase
     */
    private void movePhase(int nextIndex, int indexOfGreenPhase) {
        int indexAfterGreenPhase = (indexOfGreenPhase + 1) % this.numberPhases;
        boolean afterGreenPhaseIsInterphase = this.allPhases.get(indexAfterGreenPhase).isInterphase();

        if (indexOfGreenPhase > nextIndex) {
            if (afterGreenPhaseIsInterphase) {
                this.phaseIDs.add(nextIndex, this.phaseIDs.remove(indexAfterGreenPhase));
                this.phaseIDs.add(nextIndex, this.phaseIDs.remove(indexAfterGreenPhase));

                this.durations.add(nextIndex, this.durations.remove(indexAfterGreenPhase));
                this.durations.add(nextIndex, this.durations.remove(indexAfterGreenPhase));
            } else {
                this.phaseIDs.add(nextIndex, this.phaseIDs.remove(indexOfGreenPhase));
                this.durations.add(nextIndex, this.durations.remove(indexOfGreenPhase));
            }
        } else {
            if (afterGreenPhaseIsInterphase) {
                this.phaseIDs.add(nextIndex, this.phaseIDs.get(indexAfterGreenPhase));
                this.phaseIDs.add(nextIndex, this.phaseIDs.get(indexOfGreenPhase));
                this.phaseIDs.remove(indexAfterGreenPhase);
                this.phaseIDs.remove(indexOfGreenPhase);
                this.phaseIDs.add(0, this.phaseIDs.remove(this.numberPhases - 1));
                this.phaseIDs.add(0, this.phaseIDs.remove(this.numberPhases - 1));

                this.durations.add(nextIndex, this.durations.get(indexAfterGreenPhase));
                this.durations.add(nextIndex, this.durations.get(indexOfGreenPhase));
                this.durations.remove(indexAfterGreenPhase);
                this.durations.remove(indexOfGreenPhase);
                this.durations.add(0, this.durations.remove(this.numberPhases - 1));
                this.durations.add(0, this.durations.remove(this.numberPhases - 1));
            } else {
                this.phaseIDs.add(nextIndex, this.phaseIDs.get(indexOfGreenPhase));
                this.phaseIDs.remove(indexOfGreenPhase);

                this.durations.add(nextIndex, this.durations.get(indexOfGreenPhase));
                this.durations.remove(indexOfGreenPhase);
            }
        }

        String message;
        if (afterGreenPhaseIsInterphase) {
            message = "<html><b><font color='green'>Phase " + (indexOfGreenPhase + 1) + " and its interphase moved to position " + (nextIndex + 1) + ".</font></b></html>";
        } else {
            message = "<html><b><font color='green'>Phase " + (indexOfGreenPhase + 1) + " moved to position " + (nextIndex + 1) + ".</font></b></html>";
        }
        this.publicTransportComponent.sendMessageToGUI(message);
    }

    /**
     * Switches the phases at nextIndex and IndexOfGreenphase
     *
     * @param nextIndex
     * @param indexOfGreenPhase
     */
    private void switchPhases(int nextIndex, int indexOfGreenPhase) {
        int nextNextIndex = (nextIndex + 1) % this.numberPhases;
        int indexAfterGreenPhase = (indexOfGreenPhase + 1) % this.numberPhases;
        boolean afterGreenPhaseIsInterphase = this.allPhases.get(indexAfterGreenPhase).isInterphase();

        int greenPhase = this.phaseIDs.get(indexOfGreenPhase);
        int afterGreenPhase = this.phaseIDs.get(indexAfterGreenPhase);
        float greenPhaseDuration = this.durations.get(indexOfGreenPhase);
        float afterGreenPhaseDuration = this.durations.get(indexAfterGreenPhase);

        this.phaseIDs.set(indexOfGreenPhase, this.phaseIDs.get(nextIndex));
        this.durations.set(indexOfGreenPhase, this.durations.get(nextIndex));

        if (afterGreenPhaseIsInterphase) {
            this.phaseIDs.set(indexAfterGreenPhase, this.phaseIDs.get(nextNextIndex));
            this.durations.set(indexAfterGreenPhase, this.durations.get(nextNextIndex));
        }

        this.phaseIDs.set(nextIndex, greenPhase);
        this.durations.set(nextIndex, greenPhaseDuration);

        if (afterGreenPhaseIsInterphase) {
            this.phaseIDs.set(nextNextIndex, afterGreenPhase);
            this.durations.set(nextNextIndex, afterGreenPhaseDuration);
        }

        String message;
        if (afterGreenPhaseIsInterphase) {
            message = "<html><b><font color='green'>Phase " + (indexOfGreenPhase + 1) + " and its interphase switched with phase " + (nextIndex + 1) + ".</font></b></html>";
        } else {
            message = "<html><b><font color='green'>Phase " + (indexOfGreenPhase + 1) + " switched with phase " + (nextIndex + 1) + ".</font></b></html>";
        }
        this.publicTransportComponent.sendMessageToGUI(message);
    }

    /**
     * Manages the transfer of the computed parameters to the TLC.
     */
    private void activateTempTLC() {
        AbstractTLC fixedTimeController = new FixedTimeController(getDurationsAsArray(), this.junction, this.phaseIDs);
        changeTLC(fixedTimeController);

        this.numberOfPhaseChanges++;
        this.tempTLCActive = true;

        this.publicTransportComponent.sendMessageToGUI("Activate temporary TLC.");
    }

    /**
     * Method for switching Traffic Light Controller.
     *
     * @param ftc the new {@Link TrafficLightController}
     */
    private void changeTLC(AbstractTLC ftc) {
        TrafficLightControllerParameters parameters = ftc.getParameters();
        this.junction.getNode().changeTLC_PT(parameters);
    }

    /**
     * This method switches back to the default TLC after one cycle turn of phases.
     */
    void resetTLC() {
        if (this.tempTLCActive && this.junction.getNode().getTLCChangeDelay() <= OTCManager.getInstance().getTime()) {
            changeTLC(this.defaultTLC);
            this.tempTLCActive = false;

            publicTransportComponent.sendMessageToGUI("Phases reset to Default TLC.");
        }
    }

    /**
     * Set the default Traffic Light Controller.
     */
    private void setDefaultTLC() {
        if (!this.junction.hasActiveTLC()) {
            this.junction.generateDefaultFTC();
        }

        this.defaultTLC = this.junction.getActiveTLC();
        this.allPhases = this.junction.getPhases();
        this.phaseIDs = this.junction.getPhaseIds();
        this.numberPhases = this.junction.getNumPhases();
        this.numberOfNonInterphases = this.junction.getNumNonInterphases();
        setDurations();
    }

    private float[] getDurationsAsArray() {
        float[] phaseDurations = new float[durations.size()];
        for (int i = 0; i < durations.size(); i++) {
            phaseDurations[i] = durations.get(i);
        }
        return phaseDurations;
    }

    private void setDurations() {
        this.durations = new ArrayList<>();
        float[] phaseDurations = this.defaultTLC.getParameters().getGreenTimes();
        for (float duration : phaseDurations) {
            this.durations.add(duration);
        }
    }

    public Collection<Integer> getIDsOfGreenPhases() {
        return greenPhases.stream().map(Phase::getId).collect(Collectors.toList());
    }

    /**
     * Get the index of the phase in the list of all phases.
     *
     * @param phaseID ID of the phase
     * @return the index of the phase
     */
    private int getIndexOfPhase(int phaseID) {
        for (int i = 0; i < this.numberPhases; i++) {
            if (this.junction.getPhases().get(i).getId() == phaseID) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get the Index of the next green phase.
     *
     * @param startIndex the index where the scearch is started
     * @return the index of the next green phase after the start index
     */
    private int getIndexOfGreenPhase(int startIndex) {
        int i = startIndex;
        while (!greenPhases.contains(allPhases.get(i))) {
            i = (i + 1) % this.numberPhases;
        }
        return i;
    }

    public AbstractTLC getDefaultTLC() {
        return this.defaultTLC;
    }

    public float getDetectorDistanceToExit() {
        return detectorDistanceToExit;
    }

    public boolean[] getFeatureSetting() {
        return new boolean[]{phaseLengthChangeEnabled, phaseRotationEnabled};
    }

    Detector getDetector() {
        return this.detector;
    }

    public AimsunJunction getJunction() {
        return this.junction;
    }

    public Section getSection() {
        return this.section;
    }

    int getLineID() {
        return this.lineID;
    }

    public PublicTransportComponent getPublicTransportComponent() {
        return publicTransportComponent;
    }

    public int getDeniedPhaseChanges() {
        return deniedPhaseChanges;
    }

    public int getNumberOfPhaseChanges() {
        return numberOfPhaseChanges;
    }
}
