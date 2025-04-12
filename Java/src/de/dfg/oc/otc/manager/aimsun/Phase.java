package de.dfg.oc.otc.manager.aimsun;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.aimsun.detectors.SubDetector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Beschreibt eine Phase, die durch einen TLC schaltbar ist.
 * Part of the signalisation where a certain basic state stays unchanged. The green times may start or end at different times.
 * <p>
 * An interphase is the time  span between the end of the current green time for a signal group
 * and the start of another one (Versatzzeit).
 */
public class Phase {
    private final List<SubDetector> counterDetectors;
    private final List<SubDetector> headwayDetectors;
    /**
     * ID corresponding to phase id used in Aimsun simulator.
     */
    private final int id;
    /**
     * An interphase is generally an all-red clearance period. All traffic-lights are switched to red to avoid accidents with
     * conflicting streams.
     */
    private final boolean interphase;
    private final List<SubDetector> recallDetectors;
    private final List<SignalGroup> signalGroups;
    /**
     * Mindestfreigabezeit (Clearing time?).
     */
    private float absoluteMinimalDuration;
    /**
     * amount of time available for each phase.
     */
    private float defaultDuration = -1;
    /**
     * Values defining the steps to increase the duration of the queue-based
     * part of the phase.
     */
    private float defaultExtensionStep = -1;
    /**
     * Maximal phase duration.
     */
    private float defaultMaximalDuration = -1;

    /**
     * Erzeugt eine neue Phase, default[,Max,Min]Duration = -1.
     *
     * @param id           Id der Phase zur Zuordnung zu einer im Simulator vorhandenen
     *                     Phase.
     * @param isInterphase Legt fest, ob diese Phase eine Zwischenphase ist (true) oder
     *                     nicht (false).
     */
    Phase(final int id, final boolean isInterphase) {
        this.id = id;
        this.interphase = isInterphase;
        this.signalGroups = new ArrayList<>(3);
        this.recallDetectors = new ArrayList<>(2);
        this.headwayDetectors = new ArrayList<>(2);
        this.counterDetectors = new ArrayList<>(2);

        this.absoluteMinimalDuration = DefaultParams.L0_ABSOLUTE_MIN_DURATION;
        this.defaultExtensionStep = DefaultParams.L0_DEFAULTEXTSTEP;
    }

    /**
     * Erzeugt eine neue Phase. Wenn die angegebene maximale Phasendauer kleiner
     * ist als die angegebene normale Phasendauer, wird die maximale Phasendauer
     * auf die normale Phasendauer verl�ngert.
     *
     * @param id                 Id der Phase zur Zuordnung zu einer im Simulator vorhandenen
     *                           Phase.
     * @param isInterphase       Legt fest, ob diese Phase eine Interphase/Zwischenphase ist
     *                           (true) oder nicht (false).
     * @param defaultDuration    Normale Phasendauer.
     * @param defaultMaxDuration Maximale Phasendauer.
     */
    Phase(final int id, final boolean isInterphase, final float defaultDuration,
          final float defaultMaxDuration) throws OTCManagerException {
        this(id, isInterphase);

        if (defaultDuration <= 0 && defaultMaxDuration <= 0) {
            throw new OTCManagerException("Phase durations are less or equal zero.");
        }

        this.defaultDuration = defaultDuration;
        if (defaultMaxDuration < defaultDuration) {
            this.defaultMaximalDuration = defaultDuration;
        } else {
            this.defaultMaximalDuration = defaultMaxDuration;
        }

        this.absoluteMinimalDuration = DefaultParams.L0_ABSOLUTE_MIN_DURATION;
    }

    final void addCounterDetector(final SubDetector detector) {
        counterDetectors.add(detector);
    }

    final void addHeadwayDetector(final SubDetector detector) {
        headwayDetectors.add(detector);
    }

    final void addRecallDetector(final SubDetector detector) {
        recallDetectors.add(detector);
    }

    final void addSignalGroup(final SignalGroup group) {
        this.signalGroups.add(group);
    }

    final float getAbsoluteMinDuration() {
        return absoluteMinimalDuration;
    }

    public final Iterable<SubDetector> getCounterDetectors() {
        return counterDetectors;
    }

    /**
     * Gibt die (von AIMSUN übergebene) Phasendauer zurück, oder Float.NaN, wenn
     * diese nicht definiert oder <=0 ist.
     *
     * @return Phasendauer.
     */
    public final float getDefaultDuration() {
        return defaultDuration > 0 ? defaultDuration : Float.NaN;
    }

    final float getDefaultExtensionStep() {
        return defaultExtensionStep;
    }

    /**
     * Gibt die (von AIMSUN übergebene) maximale Phasendauer zurück, oder
     * Float.NaN, wenn diese nicht definiert oder <=0 ist.
     *
     * @return Maximale Phasendauer.
     */
    public final float getDefaultMaximalDuration() {
        return defaultMaximalDuration > 0 ? defaultMaximalDuration : Float.NaN;
    }

    public final Collection<SubDetector> getHeadwayDetectors() {
        return headwayDetectors;
    }

    /**
     * @return Liefert die Id der Phase. Mit dieser Id erfolgt die Zuordnung zu
     * einer Phase im Aimsun-Modell.
     */
    public final int getId() {
        return id;
    }

    public final int getNumRecallDetectors() {
        return recallDetectors.size();
    }

    public final Iterable<SubDetector> getRecallDetectors() {
        return recallDetectors;
    }

    public final List<SignalGroup> getSignalGroups() {
        return signalGroups;
    }

    final String getSimpleDescription() {
        final String interphaseString = interphase ? "(IP)" : "";
        String description = "<h3>Phase " + id + " " + interphaseString + "</h3>";

        if (signalGroups.isEmpty()) {
            description = description.concat("No SignalGroups.<br>");
        } else {
            for (SignalGroup signal : signalGroups) {
                description = description.concat("SignalGroup " + signal.getId() + " (" + signal.getNumberOfTurnings()
                        + " Turnings)<br>");
            }
        }

        return description;
    }

    public final boolean isInterphase() {
        return interphase;
    }

    /**
     * L�scht alle schon eingetragenen Subdetektoren.
     */
    final void resetDetectors() {
        this.recallDetectors.clear();
        this.headwayDetectors.clear();
        this.counterDetectors.clear();
    }

    final void setAbsoluteMinimalDuration(final float absoluteMinDuration) {
        this.absoluteMinimalDuration = absoluteMinDuration;
    }

    public final String toString() {
        final String linesep = System.getProperty("line.separator");
        String output = "Phase " + id + ", " + signalGroups.size() + " Signalgroups:" + linesep;

        for (SignalGroup signal : signalGroups) {
            output = output.concat(signal + linesep);
        }
        return output;
    }
}
