package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataStorage;
import de.dfg.oc.otc.manager.OTCManagerException;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;

/**
 * Erzeugt ein Panel f�r einen SubDetector, das dessen aktuellen Wert und
 * Durchschnittswert anzeigt. Je nach Typ des SubDetectors werden
 * unterschiedliche Elemente zur Visualisierung verwendet.
 *
 * @author rochner
 */
abstract class DetectorDataEntryPanel extends JPanel implements Observer {
    private final int MAX_SLIDER_VALUE = 100;
    private final int MAX_TICK_SPACING = 25;
    private final int MIN_SLIDER_VALUE = 0;
    private final DecimalFormat formatter1 = new DecimalFormat("#.##");
    private final DecimalFormat formatter2 = new DecimalFormat("##.#");
    private final DecimalFormat formatter3 = new DecimalFormat("###");
    JComponent averageDetectorValueVisu;
    JComponent detectorValueVisu;
    Hashtable<Integer, JLabel> labelTable;
    float lowerLimit;
    float upperLimit;
    private JLabel averageDetectorValue;
    private int detectorFeatureId;
    private JLabel detectorId;
    private JLabel detectorValue;
    private int ticks;
    private JLabel subDetectorFeature;
    private JLabel subdetectorId;
    private float timeInterval;

    DetectorDataEntryPanel() {
        super();
        initialize();
    }

    final void adjustLimits(final float lower, final float upper) {
        upperLimit = upper;
        lowerLimit = lower;
        float increment = (upperLimit - lowerLimit) / (ticks - 1);
        labelTable.clear();
        for (int i = 0; i < ticks; i++) {
            if (upperLimit < 10) {
                labelTable.put(MIN_SLIDER_VALUE + i * MAX_TICK_SPACING,
                        new JLabel(formatter2.format(lowerLimit + i * increment)));
            } else {
                labelTable.put(MIN_SLIDER_VALUE + i * MAX_TICK_SPACING,
                        new JLabel(formatter3.format(lowerLimit + i * increment)));
            }
        }
        updateLabelTable();
    }

    /**
     * This method initializes averageDetectorValue.
     *
     * @return javax.swing.JTextField
     */
    private JLabel createAverageDetectorValue() {
        averageDetectorValue = new JLabel("");
        averageDetectorValue.setToolTipText("Average value of the subdetector");
        return averageDetectorValue;
    }

    /**
     * This method initializes currentDetectorValue.
     *
     * @return javax.swing.JTextField
     */
    private JLabel createCurrentDetectorValue() {
        detectorValue = new JLabel("");
        detectorValue.setToolTipText("Current value of the subdetector");
        return detectorValue;
    }

    final JSlider createSlider() {
        final JSlider slider = new JSlider(MIN_SLIDER_VALUE, MAX_SLIDER_VALUE);
        slider.setValue(0);
        slider.setMajorTickSpacing(MAX_TICK_SPACING);
        slider.setMinorTickSpacing(5);
        slider.setLabelTable(labelTable);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setEnabled(false);
        return slider;
    }

    private JLabel createSubDetectorFeature() {
        subDetectorFeature = new JLabel("Presence Detector");
        subDetectorFeature.setToolTipText("Feature of the subdetector.");
        subDetectorFeature.setMinimumSize(subDetectorFeature.getPreferredSize());
        subDetectorFeature.setPreferredSize(subDetectorFeature.getPreferredSize());
        subDetectorFeature.setText("");

        return subDetectorFeature;
    }

    protected abstract JComponent getAverageDetectorValueVisu();

    protected abstract void setAverageDetectorValueVisu(float value);

    protected abstract JComponent getCurrentDetectorValueVisu();

    protected abstract void setCurrentDetectorValueVisu(float value);

    /**
     * This method initializes detectorId.
     *
     * @return javax.swing.JTextField
     */
    private JLabel getDetectorId() {
        if (detectorId == null) {
            detectorId = new JLabel("");
            detectorId.setToolTipText("ID of the associated detector.");
        }

        return detectorId;
    }

    final void setDetectorId(final int id) {
        getDetectorId().setText(String.valueOf(id));
    }

    /**
     * This method initializes subdetectorId.
     *
     * @return javax.swing.JTextField
     */
    private JLabel getSubdetectorId() {
        subdetectorId = new JLabel("");
        subdetectorId.setToolTipText("ID of the subdetector");

        return subdetectorId;
    }

    private void initialize() {
        ticks = (MAX_SLIDER_VALUE - MIN_SLIDER_VALUE) / MAX_TICK_SPACING + 1;
        timeInterval = 30;
        labelTable = new Hashtable<>();
        adjustLimits(0, 1);

        final LayoutManager flowLayout = new FlowLayout(FlowLayout.LEFT, 15, 5);
        this.setLayout(flowLayout);

        this.add(getSubdetectorId());
        this.add(getDetectorId());
        this.add(createSubDetectorFeature());
        this.add(createCurrentDetectorValue());
        this.add(getCurrentDetectorValueVisu());
        this.add(createAverageDetectorValue());
        this.add(getAverageDetectorValueVisu());
    }

    final void setAverageDetectorValue(final float value) {
        if (value >= 0) {
            if (value < 10) {
                averageDetectorValue.setText(formatter1.format(value));
            } else if (value < 100) {
                averageDetectorValue.setText(formatter2.format(value));
            } else {
                averageDetectorValue.setText(formatter3.format(value));
            }
            setAverageDetectorValueVisu(value);
        } else {
            averageDetectorValue.setText("n/a");
            setAverageDetectorValueVisu(0);
        }
    }

    final void setCurrentDetectorValue(final float value) {
        if (value >= 0) {
            if (value < 10) {
                detectorValue.setText(formatter1.format(value));
            } else if (value < 100) {
                detectorValue.setText(formatter2.format(value));
            } else {
                detectorValue.setText(formatter3.format(value));
            }
            setCurrentDetectorValueVisu(value);
        } else {
            detectorValue.setText("n/a");
            setCurrentDetectorValueVisu(0);
        }
    }

    final void setId(final int id) {
        subdetectorId.setText(String.valueOf(id));
    }

    final void setSubDetectorFeature(final int feature) {
        subDetectorFeature.setText(new DetectorCapabilities().getDescription(feature));
        this.detectorFeatureId = feature;
    }

    @Override
    public final void update(final Observable o, final Object arg) {
        if (isShowing()) {
            final DetectorDataStorage storage = (DetectorDataStorage) o;
            float averageValue;

            try {
                averageValue = storage.getAverage(detectorFeatureId, timeInterval);
            } catch (OTCManagerException ome) {
                averageValue = 0;
            }

            setAverageDetectorValue(averageValue);
            setCurrentDetectorValue(storage.getLastRelevantDatum(detectorFeatureId));

            Component parent = this;
            do {
                parent = parent.getParent();
            } while (!(parent instanceof JTabbedPane));
            parent.repaint();
        }
    }

    protected abstract void updateLabelTable();
}
