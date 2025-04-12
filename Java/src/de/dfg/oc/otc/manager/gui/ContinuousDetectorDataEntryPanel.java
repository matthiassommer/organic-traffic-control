package de.dfg.oc.otc.manager.gui;

import javax.swing.*;

/**
 * @author rochner
 * @see DetectorDataEntryPanel
 */
class ContinuousDetectorDataEntryPanel extends DetectorDataEntryPanel {
    @Override
    protected final JSlider getAverageDetectorValueVisu() {
        if (averageDetectorValueVisu == null) {
            averageDetectorValueVisu = createSlider();
        }
        return (JSlider) averageDetectorValueVisu;
    }

    @Override
    protected final JSlider getCurrentDetectorValueVisu() {
        if (detectorValueVisu == null) {
            detectorValueVisu = createSlider();
        }
        return (JSlider) detectorValueVisu;
    }

    @Override
    protected final void setAverageDetectorValueVisu(final float value) {
        if (value > upperLimit) {
            adjustLimits(lowerLimit, value);
        }
        if (value < lowerLimit) {
            adjustLimits(value, upperLimit);
        }
        getAverageDetectorValueVisu().setValue((int) (100 * (value - lowerLimit) / (upperLimit - lowerLimit)));
    }

    @Override
    protected final void setCurrentDetectorValueVisu(final float value) {
        if (value > upperLimit) {
            adjustLimits(lowerLimit, value);
        }
        if (value < lowerLimit) {
            adjustLimits(value, upperLimit);
        }
        getCurrentDetectorValueVisu().setValue((int) (100 * (value - lowerLimit) / (upperLimit - lowerLimit)));
    }

    @Override
    protected final void updateLabelTable() {
        getAverageDetectorValueVisu().setLabelTable(labelTable);
        getCurrentDetectorValueVisu().setLabelTable(labelTable);
    }
}
