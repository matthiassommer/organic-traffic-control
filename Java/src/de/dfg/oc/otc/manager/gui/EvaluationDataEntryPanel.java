package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.layer1.observer.Attribute;
import de.dfg.oc.otc.layer1.observer.Layer1Observer.DataSource;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.TrafficType;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Observable;
import java.util.Observer;

/**
 * This class is used to create a Panel for each evaluation indicator.
 * <p>
 * It is used for all specific implementations of evaluation criteria to display
 * the evaluation value and classification
 *
 * @author tomforde
 */
class EvaluationDataEntryPanel extends JPanel implements Observer {
    /**
     * Das Attribut 'criterion' gibt an, welche Evaluierungsgr��e vom aktuellen
     * Panel angezeigt wird.
     */
    private final Attribute criterion;
    private JLabel classification;
    private String labelText = "Current value";
    private JLabel value;
    private JSlider valueVis;
    private String descriptionText = "Classification";
    private String featureName = "";
    private final DecimalFormat formatter = new DecimalFormat("###.#");
    private int majTickSpacing;
    private int maxSliderValue;
    private int minTickSpacing;
    /**
     * Object Id speichert die ID des aktuellen Knotens, f�r den die Evaluierung
     * durchgef�hrt wird.
     */
    private int objID;
    private JLabel observedObjectLabel;

    EvaluationDataEntryPanel(final Attribute criterion) {
        super();
        this.criterion = criterion;
        if (this.criterion == null) {
            OTCManager.getInstance().newWarning("Attribute criterion is null");
        }

        initialize();
    }

    private void adaptPanelToCriterion() {
        if (criterion == Attribute.AVSTOPS) {
            featureName = " Averaged Stops ";
            minTickSpacing = 1;
            majTickSpacing = 5;
            maxSliderValue = 10;
        } else if (criterion == Attribute.LOS) {
            featureName = " Level of Service ";
            minTickSpacing = 5;
            majTickSpacing = 25;
            maxSliderValue = 150;
        } else if (criterion == Attribute.MAXSTOPS) {
            featureName = " Maximum Stops ";
            minTickSpacing = 1;
            majTickSpacing = 5;
            maxSliderValue = 10;
        } else if (criterion == Attribute.QUEUELENGTH) {
            featureName = " Averaged Queue Length ";
            minTickSpacing = 1;
            majTickSpacing = 5;
            maxSliderValue = 10;
        } else if (criterion == Attribute.UTILISATION) {
            featureName = " Degree of Utilisation ";
            labelText = "Current value";
            minTickSpacing = 5;
            majTickSpacing = 25;
            maxSliderValue = 200;
        } else {
            // Default initialisation for unknown features
            featureName = " Unknown Evaluation Criterion ";
            descriptionText = "Unknown Classification: ";
            minTickSpacing = 5;
            majTickSpacing = 25;
            maxSliderValue = 150;
        }
    }

    private JLabel createCurrentClassification() {
        this.classification = new JLabel();
        Border paddingBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        Border border = BorderFactory.createLineBorder(Color.BLACK);
        this.classification.setBorder(BorderFactory.createCompoundBorder(border, paddingBorder));
        this.classification.setToolTipText("Current Classification");
        // Necessary to reach an appropriate preferred size value
        this.classification.setText("?");
        this.classification.setHorizontalAlignment(SwingConstants.CENTER);

        // Set a fixed size
        Dimension fixedSize = this.classification.getPreferredSize();
        fixedSize.setSize(fixedSize.width + 5, fixedSize.height);
        this.classification.setPreferredSize(fixedSize);
        this.classification.setMinimumSize(fixedSize);
        this.classification.setMaximumSize(fixedSize);

        return classification;
    }

    private JLabel createCurrentValueLabel() {
        this.value = new JLabel();
        Border paddingBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        Border border = BorderFactory.createLineBorder(Color.BLACK);
        this.value.setBorder(BorderFactory.createCompoundBorder(border, paddingBorder));
        this.value.setText("0.0");
        this.value.setHorizontalAlignment(SwingConstants.CENTER);

        // Setting a fixed size of the currentValue Label
        Dimension dim = this.value.getPreferredSize();
        dim.setSize(60, dim.height);
        this.value.setPreferredSize(dim);
        this.value.setMinimumSize(dim);
        this.value.setMaximumSize(dim);
        return this.value;
    }

    private JSlider createCurrentValueVis() {
        JSlider sliderObject = new JSlider();
        sliderObject.setMinimum(0);
        sliderObject.setMaximum(maxSliderValue);
        sliderObject.setValue(0);
        sliderObject.setMajorTickSpacing(majTickSpacing);
        sliderObject.setMinorTickSpacing(minTickSpacing);
        sliderObject.setPaintTicks(true);
        sliderObject.setPaintLabels(true);
        sliderObject.setEnabled(false);

        this.valueVis = sliderObject;
        return this.valueVis;
    }

    private JLabel getObservedObject() {
        if (observedObjectLabel == null) {
            observedObjectLabel = new JLabel("");
            Border paddingBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            Border border = BorderFactory.createLineBorder(Color.BLACK);
            observedObjectLabel.setBorder(BorderFactory.createCompoundBorder(border, paddingBorder));
            observedObjectLabel.setToolTipText("Observed network component");
        }
        return observedObjectLabel;
    }

    /**
     * This method is needed to initialize this panel. All components are
     * created and assigned to their positions.
     */
    private void initialize() {
        adaptPanelToCriterion();

        LayoutManager flowLayout = new FlowLayout(FlowLayout.LEADING);
        setLayout(flowLayout);

        JLabel junctionLabel = getObservedObject();
        add(junctionLabel);

        JLabel featureLabel = new JLabel(featureName);
        featureLabel.setPreferredSize(new Dimension(150, 20));
        add(featureLabel);

        JLabel descriptionLabel = new JLabel(descriptionText);
        descriptionLabel.setPreferredSize(new Dimension(100, 20));
        add(descriptionLabel);

        JLabel classLabel = createCurrentClassification();
        classLabel.setMinimumSize(new Dimension(200, 25));
        add(classLabel);

        JLabel textLabel = new JLabel(labelText);
        textLabel.setMinimumSize(new Dimension(200, 25));
        add(textLabel);

        JLabel valueLabel = createCurrentValueLabel();
        add(valueLabel);

        add(createCurrentValueVis());
    }

    private int prepareValue(float value) {
        if (criterion != null) {
            final float maxValue = criterion.getMaximalValue();
            if (value < 0) {
                value = 0;
            } else if (value > maxValue) {
                value = maxValue;
            }
        }
        return Math.round(value);
    }

    final void setCurrentClassification(final String classification) {
        this.classification.setText(classification);
    }

    final void setCurrentValue(final float value) {
        float valueToSet = value;

        if (Float.isNaN(value)) {
            valueToSet = 0;
        }

        if (criterion == Attribute.UTILISATION) {
            this.value.setText(formatter.format(valueToSet) + " %");
        } else {
            this.value.setText(formatter.format(valueToSet));
        }

        valueVis.setValue(prepareValue(valueToSet));
    }

    final void setObjectID(final int id) {
        objID = id;
    }

    final void setObservedObject(final String objID) {
        getObservedObject().setText(objID);
    }

    public final void update(final Observable obs, final Object arg) {
        if (isShowing()) {
            // hole den node anhand der aktuellen id des panels
            final OTCNode node = OTCManager.getInstance().getNetwork().getNode(objID);

            // aktualisiere Werte
            final float value = node.getEvaluation(DataSource.STATISTICS, criterion, TrafficType.INDIVIDUAL_TRAFFIC,
                    false);
            setCurrentValue(value);

            if (criterion == Attribute.AVSTOPS) {
                setCurrentClassification(String.valueOf(LOS.getLevelName(LOS.getClassification(Attribute.AVSTOPS, value))));
            } else if (criterion == Attribute.LOS) {
                setCurrentClassification(String.valueOf(LOS.getLevelName(LOS.getLos(value, TrafficType.INDIVIDUAL_TRAFFIC))));
            } else if (criterion == Attribute.MAXSTOPS) {
                setCurrentClassification(String.valueOf(LOS.getLevelName(LOS.getClassification(Attribute.MAXSTOPS, value))));
            } else if (criterion == Attribute.QUEUELENGTH) {
                setCurrentClassification(String.valueOf(LOS.getLevelName(LOS.getClassification(Attribute.QUEUELENGTH, value))));
            } else if (criterion == Attribute.UTILISATION) {
                setCurrentClassification(String.valueOf(LOS.getLevelName(LOS.getClassification(Attribute.UTILISATION, value))));
            }

            // update view
            Component parent = this;
            if (parent != null) {
                do {
                    parent = parent.getParent();
                } while (!(parent instanceof JTabbedPane));

                parent.repaint();
            }
        }
    }
}
