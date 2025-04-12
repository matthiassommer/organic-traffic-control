package de.dfg.oc.otc.layer0.tlc;

import de.dfg.oc.otc.manager.aimsun.Phase;
import de.dfg.oc.otc.manager.aimsun.detectors.SubDetector;

/**
 * Part of the signalisation where a certain basic state stays unchanged. The green times may start or end at different times.
 * An interphase is the time span between the end of the current green time for a signal group and the start of another one.
 */
public class TrafficLightControllerPhase {
    private final Phase aimsunPhase;
    /**
     * Value defining the step to increase the duration of the queue-based part of the phase.
     */
    private float extensionStep;
    /**
     * Maximum gap between two cars for the second dynamic part of the phase.
     */
    private float maximumGap;
    private float maximalInitialGreenTime;
    private final float maximalGreenTime;
    private float minimumGreenTime;
    private Recall recall;
    /**
     * Time before reduction function starts.
     */
    private float reductionDelay;

    public TrafficLightControllerPhase(final float maxgreens, final Phase aimsunPhase) {
        this.maximalGreenTime = maxgreens;
        this.recall = Recall.disable;
        this.aimsunPhase = aimsunPhase;
    }

    public final Phase getAimsunPhase() {
        return aimsunPhase;
    }

    public final float getExtensionStep() {
        return extensionStep;
    }

    public final float getMaximumGap() {
        return maximumGap;
    }

    public final float getMaximimGreenTime() {
        return maximalInitialGreenTime;
    }

    public final float getMaxGreenTime() {
        return maximalGreenTime;
    }

    public final float getMinimumGreenTime() {
        return minimumGreenTime;
    }

    /**
     * Gibt die Anzahl der aktiven Recall-Detektoren der Phase zur�ck.
     *
     * @return number of recall detectors
     */
    public final int getNumActiveRecallDetectors() {
        int counter = 0;
        for (SubDetector subDetector : aimsunPhase.getRecallDetectors()) {
            if (subDetector.getValue() > 0) {
                counter++;
            }
        }
        return counter;
    }

    public final int getNumRecallDetectors() {
        return aimsunPhase.getNumRecallDetectors();
    }

    public final Recall getRecall() {
        return recall;
    }

    public final float getReductionDelay() {
        return reductionDelay;
    }

    public final void setExtensionStep(final float extensionStep) {
        this.extensionStep = extensionStep;
    }

    public final void setMaximumGap(final float maximumGap) {
        this.maximumGap = maximumGap;
    }

    public final void setMaximimInitialGreenTime(final float maxInitGreen) {
        this.maximalInitialGreenTime = maxInitGreen;
    }

    public final void setMinimumGreenTime(final float minimumGreenTime) {
        if (minimumGreenTime >= maximalGreenTime) {
            this.minimumGreenTime = maximalGreenTime;
        } else {
            this.minimumGreenTime = minimumGreenTime;
        }
    }

    public final void setRecall(final Recall recall) throws TLCException {
        if (recall != Recall.no) {
            this.recall = recall;
        } else {
            if (aimsunPhase.getNumRecallDetectors() < 1) {
                throw new TLCException(
                        "Phase hat keinen Presence/Recall Detektor, soll aber auf no recall gesetzt werden!");
            }
            this.recall = recall;
        }
    }

    public final void setReductionDelay(final float reductionDelay) {
        this.reductionDelay = reductionDelay;
    }

}
