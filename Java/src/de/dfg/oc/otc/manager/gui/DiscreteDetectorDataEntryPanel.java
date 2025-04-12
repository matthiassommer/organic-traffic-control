package de.dfg.oc.otc.manager.gui;

import javax.swing.*;

/**
 * @author rochner
 */
class DiscreteDetectorDataEntryPanel extends DetectorDataEntryPanel {
    @Override
    protected final JSlider getAverageDetectorValueVisu() {
        if (averageDetectorValueVisu == null) {
            averageDetectorValueVisu = createSlider();
        }
        return (JSlider) averageDetectorValueVisu;
    }

    @Override
    protected final JRadioButton getCurrentDetectorValueVisu() {
        if (detectorValueVisu == null) {
            detectorValueVisu = new JRadioButton();
        }
        return (JRadioButton) detectorValueVisu;
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
        getCurrentDetectorValueVisu().setSelected(value > 0.1);
    }

    @Override
    protected final void updateLabelTable() {
        getAverageDetectorValueVisu().setLabelTable(labelTable);
    }
}
