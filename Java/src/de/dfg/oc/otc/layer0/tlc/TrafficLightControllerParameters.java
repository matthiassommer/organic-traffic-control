package de.dfg.oc.otc.layer0.tlc;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.Phase;
import de.dfg.oc.otc.manager.aimsun.SignalGroup;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.*;

/**
 * Generischer Container, der die Parameter von
 * {@code TrafficLightController}-Objekten aufnimmt. An action specifies
 * the classifier's proposed reaction to an input.
 *
 * @author rochner
 */
@SuppressWarnings("serial")
public class TrafficLightControllerParameters implements Serializable, Cloneable {
    private static final Logger log = Logger.getLogger(TrafficLightControllerParameters.class);
    /**
     * Saturation flow.
     */
    private final int maxFlow = 1800;
    /**
     * Is phase interphase or non-interphase? true if interphase.
     */
    private boolean[] isInterPhase = new boolean[0];
    /**
     * Cycle time: time available for one iteration through the signal plan.
     */
    private float cycleTime = -1;
    /**
     * The maximal green times for each phase in seconds.
     */
    private float[] greenTimes = new float[0];
    /**
     * Green time (per signal group).
     */
    private float[] greenTimeForSignalGroups;
    /**
     * IDs of phases. First entry is id of junction.
     */
    private int[] ids = new int[0];
    /**
     * Number of lanes (per signal group).
     */
    private int[] numberOfLanesForSignalGroups;
    /**
     * ???
     */
    private String[] strings = new String[0];
    /**
     * Type of the TLC.
     *
     * @see de.dfg.oc.otc.layer0.tlc.TLCTypes
     */
    private int tlcType;

    /**
     * Erzeugt einen Parametersatz, der einen Traffic Light Controller
     * beschreibt. Das Mapping der Werte aus den Arrays zu tats�chlichen
     * Eigenschaften geschieht in den Klassen, in denen die konkreten Traffic
     * Light Controller implementiert werden.
     *
     * @param type         Typ des TLC, see {@code TLCTypes}
     * @param ints         ids
     * @param greenTimes   green times
     * @param strings
     * @param isInterPhase is interphase
     */
    public TrafficLightControllerParameters(final int type, final int[] ints, final float[] greenTimes,
                                            final String[] strings, final boolean[] isInterPhase) {
        this.setType(type);
        this.ids = ints;
        this.greenTimes = greenTimes;
        this.strings = strings;
        this.isInterPhase = isInterPhase;
    }

    /**
     * Erzeugt TLC-Parameter aus einem übergebenen String.
     *
     * @param paramString String aus dem das Parameterobjekt erzeugt wird
     */
    public TrafficLightControllerParameters(final String paramString) {
        // Determine first occurrence of 'true' or 'false'
        int cutoffIndex;
        final int cutoffIndex1 = paramString.indexOf(", false");
        final int cutoffIndex2 = paramString.indexOf(", true");

        if (cutoffIndex1 != -1 && cutoffIndex2 != -1) {
            cutoffIndex = Math.min(cutoffIndex1, cutoffIndex2);
        } else {
            cutoffIndex = Math.max(cutoffIndex1, cutoffIndex2);
            if (cutoffIndex == -1) {
                cutoffIndex = paramString.length();
            }
        }

        // Contains ints and greenTimes
        final String paramString1 = paramString.substring(0, cutoffIndex);
        final String[] actionParts = paramString1.split(", ");

        setType(actionParts[0]);
        setIntsAndFloats(actionParts);
        setBooleans(paramString, cutoffIndex1);
    }

    private void setBooleans(String paramString, int cutoffIndex) {
        final String booleanString = paramString.substring(Math.min(cutoffIndex + 1, paramString.length()),
                paramString.length());
        if (!booleanString.isEmpty()) {
            final String[] boolStr = booleanString.split(", ");
            final boolean[] booleans = new boolean[boolStr.length];
            for (int i = 0; i < boolStr.length; i++) {
                final String trimmed = boolStr[i].trim();
                booleans[i] = trimmed.equals("true");
            }
            this.isInterPhase = booleans;
        }
    }

    private void setIntsAndFloats(String[] actionParts) {
        final int[] intValues = new int[(actionParts.length - 2) / 2 + 1];
        for (int i = 1; i <= (actionParts.length - 2) / 2 + 1; i++) {
            intValues[i - 1] = new Integer(actionParts[i]);
        }
        this.ids = intValues;

        final float[] floatValues = new float[(actionParts.length - 2) / 2];
        for (int i = (actionParts.length - 2) / 2 + 2; i < actionParts.length; i++) {
            floatValues[i - ((actionParts.length - 2) / 2 + 2)] = new Float(actionParts[i]);
        }
        this.greenTimes = floatValues;
    }

    private void setType(String actionPart) {
        final int type = new Integer(actionPart);

        if (type != TLCTypes.FIXEDTIME && type != TLCTypes.NEMA) {
            log.warn("Creating TLC parameters from a string is only supported for fixed time and NEMA TLCs.");
        }
        setType(type);
    }

    /**
     * Adapts the phase durations of this parameter set by proportionally
     * resizing the phase durations of non-interphases. The new parameter set's
     * cycle time equals the cycle time given as parameter (if
     * {@code _newCycle == 0}, no adaptation is performed).
     *
     * @param newCycleTime cycle time of the returned parameter set (if
     *                     {@code _newCycle == 0}, no adaptation is performed)
     * @return a new parameters set, its cycle time equals the cycle time given
     * as parameter
     * @throws TLCException if the conversion is not possible (e.g if the resulting phase
     *                         durations are negative)
     */
    public final TrafficLightControllerParameters adaptCycleTime(final int newCycleTime) throws TLCException {
        if (newCycleTime != 0) {
            // Check cycle time
            final float interPhaseDuration = getInterphaseDuration();
            if (newCycleTime <= interPhaseDuration) {
                throw new TLCException("Given cycleTime (" + newCycleTime
                        + ") is smaller than duration of interphases (" + interPhaseDuration + ")!");
            }

            if (this.tlcType == TLCTypes.FIXEDTIME) {
                float[] newDurations = adaptFTC(newCycleTime);
                return new TrafficLightControllerParameters(TLCTypes.FIXEDTIME, ids, newDurations, new String[0],
                        isInterPhase);
            } else if (this.tlcType == TLCTypes.NEMA) {
                float[] newNEMADurations = adaptNEMA(newCycleTime);
                return new TrafficLightControllerParameters(TLCTypes.NEMA, ids, newNEMADurations, new String[0],
                        isInterPhase);
            } else {
                throw new TLCException("TLC type not supported.");
            }
        } else {
            // No cycle time adaptation needed (newCycleTime == 0)
            return this;
        }
    }

    private float[] adaptNEMA(int newCycleTime) {
        //TODO Calculate cycle time by considering gap reduction?????
        //TODO adjust green time for current phase in container
        return null;
    }

    /**
     * Adapt phase durations to new cycle time by multiplying it with a factor to match the new cycle time
     * (sum of all phase durations equals cycle time).
     *
     * @return adapted phase durations
     */
    private float[] adaptFTC(int newCycleTime) {
        final float oldCycle = getCycleTime();
        final float interphaseDuration = getInterphaseDuration();
        final float factor = (newCycleTime - interphaseDuration) / (oldCycle - interphaseDuration);

        int newCycle = 0;
        final int size = greenTimes.length;
        final float[] newDurations = new float[size];

        for (int i = 0; i < size; i++) {
            if (isInterPhase[i]) {
                // Copy duration of interphases
                newDurations[i] = greenTimes[i];
                newCycle += greenTimes[i];
            } else {
                // Resize durations of other phases
                final int newDuration = Math.round(factor * greenTimes[i]);
                newDurations[i] = newDuration;
                newCycle += newDuration;
            }
        }

        // Correct differences that my have occurred by rounding
        if (newCycle != newCycleTime) {
            int diff = newCycleTime - newCycle;

            for (int i = 0; i < newDurations.length; i++) {
                // For non-interphases: Can difference be corrected by
                // changing this phase?
                if (!isInterPhase[i] && newDurations[i] + diff > 0) {
                    newDurations[i] += diff;
                    diff = 0;
                    break;
                }
            }

            // Correction was impossible
            if (diff != 0) {
                throw new TLCException("Negative phase duration was calculated!");
            }
        }

        for (int i = 0; i < newDurations.length; i++) {
            // For non-interphases: Is phase duration => 1?
            if (!isInterPhase[i] && newDurations[i] < 1) {
                throw new TLCException(OTCManager.getInstance().getTime()
                        + ": Calculated phase duration is < 1 when adapting node " + ids[0] + " from "
                        + oldCycle + " to " + newCycleTime + ".");
            }
        }
        return newDurations;
    }

    /**
     * Returns a clone of the parameters.
     */
    public final TrafficLightControllerParameters clone() {
        final int[] clonedInts = new int[this.ids.length];
        System.arraycopy(this.ids, 0, clonedInts, 0, this.ids.length);

        final float[] clonedFloats = new float[this.greenTimes.length];
        System.arraycopy(this.greenTimes, 0, clonedFloats, 0, this.greenTimes.length);

        final String[] clonedStrings = new String[this.strings.length];
        System.arraycopy(this.strings, 0, clonedStrings, 0, this.strings.length);

        final boolean[] clonedBools = new boolean[this.isInterPhase.length];
        System.arraycopy(this.isInterPhase, 0, clonedBools, 0, this.isInterPhase.length);

        return new TrafficLightControllerParameters(
                this.tlcType, clonedInts, clonedFloats, clonedStrings, clonedBools);
    }

    /**
     * Returns {@code true} iff the parameter sets represented by this
     * object and {@code _tlcparams} are identical.
     *
     * @param o object that that current object is compared to
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrafficLightControllerParameters tlcparams = (TrafficLightControllerParameters) o;

        // Different TCL types
        if (this.tlcType != tlcparams.tlcType) {
            return false;
        }

        // Compare array lengths
        if (this.ids.length != tlcparams.ids.length || this.greenTimes.length != tlcparams.greenTimes.length
                || this.strings.length != tlcparams.strings.length
                || this.isInterPhase.length != tlcparams.isInterPhase.length) {
            return false;
        }

        for (int i = 0; i < this.ids.length; i++) {
            if (this.ids[i] != tlcparams.ids[i]) {
                return false;
            }
        }

        for (int i = 0; i < this.greenTimes.length; i++) {
            if (this.greenTimes[i] != tlcparams.greenTimes[i]) {
                return false;
            }
        }

        // strings
        for (int i = 0; i < this.strings.length; i++) {
            if (!this.strings[i].equals(tlcparams.strings[i])) {
                return false;
            }
        }

        for (int i = 0; i < this.isInterPhase.length; i++) {
            if (this.isInterPhase[i] != tlcparams.isInterPhase[i]) {
                return false;
            }
        }

        return true;
    }

    public final boolean[] getIsInterPhase() {
        return isInterPhase;
    }

    /**
     * Returns the cycle time for this parameter set.
     */
    public final float getCycleTime() {
        if (this.tlcType == TLCTypes.FIXEDTIME || this.tlcType == TLCTypes.NEMA) {
            // cycle time was not yet calculated
            if (this.cycleTime == -1) {
                float cycleTime = 0;
                for (float greenTime : greenTimes) {
                    cycleTime += greenTime;
                }

                this.cycleTime = cycleTime;
            }

            return cycleTime;
        } else if (this.tlcType == TLCTypes.FIXEDTIMERECALL) {
            // TODO Implementierung für FIXEDTIMERECALL
            return 0;
        }
        return 0;
    }

    public final float[] getGreenTimes() {
        return greenTimes;
    }

    /**
     * Return an array containing the green times (per signal group) for the
     * parameter set given as argument.
     *
     * @return an array containing the green times (per signal group)
     */
    private float[] getGreenTimeForSignalGroups() {
        if (this.tlcType == TLCTypes.FIXEDTIME || this.tlcType == TLCTypes.NEMA) {
            if (greenTimeForSignalGroups == null) {
                final AimsunJunction junction = OTCManager.getInstance().getNetwork().getJunction(ids[0]);
                final List<Phase> phases = junction.getPhases();
                final float[] signalGroupDurations = new float[junction.getSignalGroups().size()];

                for (Phase phase : phases) {
                    List<SignalGroup> signalGroups = phase.getSignalGroups();

                    for (SignalGroup signalGroup : signalGroups) {
                        signalGroupDurations[signalGroup.getId() - 1] += greenTimes[phase.getId() - 1];
                    }
                }

                final List<Float> signalGroupDurationList = new ArrayList<>();
                for (float duration : signalGroupDurations) {
                    if (duration != 0) {
                        signalGroupDurationList.add(duration);
                    }
                }

                final float[] signalGroupDurations2 = new float[signalGroupDurationList.size()];
                for (int i = 0; i < signalGroupDurations2.length; i++) {
                    signalGroupDurations2[i] = signalGroupDurationList.get(i);
                }

                greenTimeForSignalGroups = signalGroupDurations2;
            }
        } else {
            log.warn("Method not available for this TLC type.");
        }

        return greenTimeForSignalGroups;
    }

    public final int[] getIds() {
        return ids;
    }

    /**
     * Returns the sum of all interphase durations.
     */
    private float getInterphaseDuration() {
        float interphaseDuration = 0;

        if (this.tlcType == TLCTypes.FIXEDTIME || this.tlcType == TLCTypes.NEMA) {
            if (isInterPhase.length == greenTimes.length) {
                for (int i = 0; i < greenTimes.length; i++) {
                    if (isInterPhase[i]) {
                        interphaseDuration += greenTimes[i];
                    }
                }
            }
            return interphaseDuration;
        }
        return 0;
    }

    /**
     * Returns the traffic flow per signal group that does not exceed a given
     * degree of saturation.
     *
     * @param degreeOfSaturation a degree of saturation
     * @return the maximally acceptable flow per signal group
     */
    public final float[] getMaxSignalGroupFlowForDegreeOfSaturation(final float degreeOfSaturation) {
        if (this.tlcType == TLCTypes.FIXEDTIME || this.tlcType == TLCTypes.NEMA) {
            final float[] greenTimes = getGreenTimeForSignalGroups();
            final int[] lanes = getNbOfLanesForSignalGroups();
            final float cycleTime = getCycleTime();

            // Acceptable flow
            final float[] signalGroupFlow = new float[greenTimes.length];
            for (int i = 0; i < greenTimes.length; i++) {
                final float f = greenTimes[i] / cycleTime;
                // Anteil der Grünzeit am Umlauf
                signalGroupFlow[i] = degreeOfSaturation * f * lanes[i] * maxFlow;
            }
            return signalGroupFlow;
        } else {
            log.warn("Method not available for this TLC type.");
            return new float[0];
        }
    }

    /**
     * Returns the number of lanes for the intersection's non-empty signal
     * groups.
     *
     * @return the number of lanes for the intersection's non-empty signal
     * groups.
     */
    public final int[] getNbOfLanesForSignalGroups() {
        if (numberOfLanesForSignalGroups == null) {
            fillNumberOfLanesForSignalGroups();
        }

        return numberOfLanesForSignalGroups;
    }

    /**
     * Set the number of lanes refering to a signal group.
     */
    private void fillNumberOfLanesForSignalGroups() {
        final List<Integer> laneList = new ArrayList<>();
        final List<SignalGroup> signalGroups = OTCManager.getInstance().getNetwork().getJunction(ids[0])
                .getSignalGroups();

        for (SignalGroup signalGroup : signalGroups) {
            int lanes = signalGroup.getNumberOfLanes();
            if (lanes != 0) {
                // Consider "non-empty" signal groups, only.
                laneList.add(lanes);
            }
        }

        // Convert to array
        final int[] lanes = new int[laneList.size()];
        for (int i = 0; i < lanes.length; i++) {
            lanes[i] = laneList.get(i);
        }

        numberOfLanesForSignalGroups = lanes;
    }

    /**
     * Returns when the phase with the given phase id starts.
     *
     * @param phaseId id of phase
     * @return second when the phase given as parameter starts
     */
    public final int getStartOfPhase(final int phaseId) {
        int startOfPhase = 0;

        if (this.tlcType == TLCTypes.FIXEDTIME || this.tlcType == TLCTypes.NEMA) {
            // Determine duration of preceeding phases
            // (Usually, id x belongs to the x-th phase).
            for (int i = 0; i < phaseId - 1; i++) {
                startOfPhase += greenTimes[i];
            }
        }
        return startOfPhase;
    }

    public final String[] getStrings() {
        return strings;
    }

    public final int getType() {
        return tlcType;
    }

    private void setType(final int type) {
        if (type == TLCTypes.FIXEDTIME || type == TLCTypes.FIXEDTIMERECALL || type == TLCTypes.NEMA) {
            this.tlcType = type;
        } else {
            log.warn("Unknown TLC type.");
        }
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public final String toString() {
        String toString = "";

        if (greenTimes != null) {
            for (float f : greenTimes) {
                Formatter formatter = new Formatter(Locale.ENGLISH);
                toString += formatter.format(", %4.1f", f);
                formatter.close();
            }
        }

        if (strings != null) {
            for (String s : strings) {
                toString += ", " + s;
            }
        }

        if (isInterPhase != null) {
            for (boolean b : isInterPhase) {
                toString += ", " + b;
            }
        }

        return toString;
    }
}
