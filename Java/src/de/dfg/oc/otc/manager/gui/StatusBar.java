package de.dfg.oc.otc.manager.gui;

import javax.swing.*;

/**
 * This panel shows a small statusbar.
 */
class StatusBar extends JPanel {
    private final JLabel valueLabel = new JLabel("");

    /**
     * Constructs a new statusbar with the number of fields specified in size,
     * the descriptions specified in strings and the values in the value array.
     *
     */
    StatusBar() {
        this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        JLabel descriptionLabel = new JLabel("");
        descriptionLabel.setText("Simulation time: ");
        this.valueLabel.setText("0");

        this.add(descriptionLabel);
        this.add(valueLabel);
    }

    void changeText(final String value) {
        this.valueLabel.setText(value);
    }
}
