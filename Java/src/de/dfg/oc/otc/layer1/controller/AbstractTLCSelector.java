package de.dfg.oc.otc.layer1.controller;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer1.observer.Attribute;
import de.dfg.oc.otc.layer2.OptimisationResult;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.SignalGroup;
import de.dfg.oc.otc.manager.aimsun.Turning;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract parent class for selection mechanism on Layer 1. Selection
 * mechanisms (like LCS) need to extend this class.
 *
 * @author hpr
 */
public abstract class AbstractTLCSelector {
    /**
     * The attribute specifies the objective that is handled on Layer 1 (Level
     * of Service, queue length, ...). The attribute specifies whether the
     * objective should be minimized or maximized.
     */
    private Attribute attribute;

    /**
     * Traffic light controller that is set in the AIMSUN model.
     */
    private TrafficLightControllerParameters defaultTLCParams;

    private String logFileName = "";

    /**
     * The traffic node handled by this selector.
     */
    protected OTCNode managedNode;

    public abstract void addOptimisationResult(OptimisationResult result);

    /**
     * Returns {@code true} if the warm up time specified in the
     * configuration file has passed, so the statistics provide reasonable
     * traffic situations. When the warm up time has passed, the LCS can be
     * activated.
     *
     * @return {@code true}, iff the warm up time has passed
     */
    protected final boolean afterWarmUp() {
        final int warmUpDuration = DefaultParams.L1_WARMUP_TIME;
        final float currentTime = OTCManager.getInstance().getTime();

        return currentTime >= warmUpDuration;
    }

    /**
     * Adds the given string to the log-file of this LCS.
     *
     * @param message string that is added to the log-file
     */
    protected final void appendToLog(final String message) {
        if (DefaultParams.LOG_LCS_DATA) {
            setupLogging();

            try {
                final float time = OTCManager.getInstance().getTime();

                final FileOutputStream fout = new FileOutputStream(logFileName, true);
                new PrintStream(fout).println(time + ": " + message);
                fout.close();
            } catch (IOException e) {
                OTCManager.getInstance().newWarning("Unable to write to file " + logFileName);
            }
        }
    }

    public abstract int determineDesiredCycleTime(float[] situation) throws ClassifierException;

    public abstract TrafficLightControllerParameters determineDesiredTLC(float[] situation, int agreedCycleTime)
            throws ClassifierException;

    public abstract float determinePredictionForAction(int hashCode, float[] situation, int agreedCycleTime);

    public abstract float determinePredictionForActiveAction(int hashCode);

    public abstract void distributeReward(float evaluation);

    public abstract void generateMappingForSituation(float[] situation);

    public final Attribute getAttribute() {
        return attribute;
    }

    /**
     * Returns the configuration of the {@code TLCSelector}.
     */
    protected abstract String getConfig();

    /**
     * Returns the default traffic light controller.
     */
    protected final TrafficLightControllerParameters getDefaultTLCParams() {
        return defaultTLCParams;
    }

    /**
     * Converts a traffic situation that is defined based the flow of each
     * turning to a traffic situation that is defined based on the flow for each
     * signal group.
     *
     * @param situationByTurnings a traffic situation that is defined based the flow of each
     *                            turning
     * @return a traffic situation that is defined based on the flow for each
     * signal group
     */
    protected final float[] getSituationBySignalGroups(final float[] situationByTurnings) {
        // Signalgruppen des gesteuerten Knotens
        final List<SignalGroup> signalGrps = managedNode.getJunction().getSignalGroups();

        // SectionIds für Turning-basierte Situationsbeschreibung
        final int[] sectionIdsForTurnings = managedNode.getSectionIDsForTurnings();

        // Signalgruppen-basierte Situationsbeschreibung
        final List<Float> situationBySignalGroups = new ArrayList<>();

        // Durchlaufe alle Signalgruppen
        for (SignalGroup signalGroup : signalGrps) {
            // Signalgruppen können leer sein
            if (signalGroup.getNumberOfTurnings() > 0) {
                float signalGroupFlow = 0;

                // Durchlaufe alle Abbiegebeziehungen der Signalgruppe
                List<Turning> turnings = signalGroup.getTurnings();

                for (Turning turning : turnings) {
                    int inSectionId = turning.getInSection().getId();
                    int outSectionId = turning.getOutSection().getId();

                    // Bestimme Verkehrsfluss f�r die Signalgruppe
                    for (int k = 0; k < sectionIdsForTurnings.length; k += 2) {
                        if (sectionIdsForTurnings[k] == inSectionId && sectionIdsForTurnings[k + 1] == outSectionId) {
                            signalGroupFlow += situationByTurnings[k / 2];
                            // Nach dem ersten Treffer kann gestoppt werden
                            break;
                        }
                    }
                }
                situationBySignalGroups.add(signalGroupFlow);
            }
        }

        // Ergebnis umkopieren
        final int size = situationBySignalGroups.size();
        final float[] result = new float[size];
        for (int i = 0; i < size; i++) {
            result[i] = situationBySignalGroups.get(i);
        }

        return result;
    }

    public abstract boolean loadMappingFromFile(String popfile);

    public abstract void saveMappingToFile(String filename);

    public abstract TrafficLightControllerParameters selectAction(float[] situation, int agreedCycleTime)
            throws ClassifierException;

    protected final void setAttribute(final Attribute attribute) {
        this.attribute = attribute;
    }

    /**
     * Sets the default traffic light controller.
     *
     * @param defaultTLCParams the traffic light controller parameters used in the AIMSUN
     *                         model
     */
    public final void setDefaultTLCParams(final TrafficLightControllerParameters defaultTLCParams) {
        this.defaultTLCParams = defaultTLCParams;
    }

    private void setupLogging() {
        if (this.logFileName.isEmpty()) {
            String nodeID = "_";

            if (this.managedNode != null) {
                nodeID += this.managedNode.getId();
            }

            String selectorType = this.getClass().getName();
            selectorType = selectorType.substring(selectorType.lastIndexOf(".") + 1);
            this.logFileName = "logs/" + OTCManager.getInstance().getFilenamePrefix() + nodeID + "_" + selectorType + ".log";

            appendToLog(getConfig());
        }
    }
}
