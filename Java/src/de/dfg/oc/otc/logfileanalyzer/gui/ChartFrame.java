/*
 * LogFileAnalyzer for Learning Classifier Systems
 * 
 * Copyright (C) 2008 
 * Clemens Gersbacher <clgersbacher@web.de>, 
 * Holger Prothmann <holger.prothmann@kit.edu>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package de.dfg.oc.otc.logfileanalyzer.gui;

import de.dfg.oc.otc.logfileanalyzer.DataElement;
import de.dfg.oc.otc.logfileanalyzer.LogFileAnalyzer;
import de.dfg.oc.otc.logfileanalyzer.histogram.AbstractHistogram;
import de.dfg.oc.otc.logfileanalyzer.histogram.UniversalHistogram;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Provides a frame containing a histogram chart.
 *
 * @author Clemens Gersbacher, Holger Prothmann
 */
@SuppressWarnings("serial")
class ChartFrame extends JFrame {
    /**
     * {@code TableFrame} that created this {@code ChartFrame}.
     */
    private final TableFrame myTableFrame;
    /**
     * Names of the classifier sets displayed in the
     * {@code jComboBoxTableSelector}.
     */
    private final String[] tableSelection = {"Population", "Match Set", "Action Set"};
    /**
     * The current {@code DataElement} displayed in the
     * {@code TableFrame}.
     */
    private DataElement currentElement;
    /**
     * Selects the histogram displayed in this {@code ChartFrame}.
     */
    private JComboBox<AbstractHistogram> jComboBoxHistogramSelector;
    /**
     * Selects the classifier set displayed in this {@code ChartFrame}.
     */
    private JComboBox<String[]> jComboBoxTableSelector;
    /**
     * Contains {@code ComboBox}es to select the classifier set and the
     * histogram displayed in this {@code ChartFrame}.
     */
    private JPanel jPanelComboBoxes;
    /**
     * Contains all panels of this {@code ChartFrame}.
     */
    private JPanel jPanelContentpane;
    /**
     * Contains the histogram.
     */
    private JPanel jPanelHistogram;
    /**
     * Contains the {@code jPanelInformation} and the
     * {@code jPanelHistogram}.
     */
    private JPanel jPanelInfoAndHistogram;
    /**
     * Contains the iteration and the number of classifiers displayed in this
     * {@code ChartFrame}.
     */
    private JPanel jPanelInformation;
    /**
     * Contains panels that define lower and upper limits for values displayed
     * in this {@code ChartFrame}.
     */
    private JPanel jPanelLimits;
    /**
     * Contains a {@code TextField} to define a lower limit for values
     * displayed in this {@code ChartFrame}.
     */
    private JPanel jPanelLowerLimit;
    /**
     * Contains a {@code TextField} to define an upper limit for values
     * displayed in this {@code ChartFrame}.
     */
    private JPanel jPanelUpperLimit;
    /**
     * Contains the number of classifiers displayed in this
     * {@code ChartFrame}.
     */
    private JTextArea jTextAreaClassifiers;
    /**
     * Contains the iteration displayed in this {@code ChartFrame}.
     */
    private JTextArea jTextAreaIteration;
    /**
     * Provides a description for the corresponding
     * {@code jTextFieldUpperLimit}.
     */
    private JTextArea jTextAreaLowerLimit;
    /**
     * Provides a description for the corresponding
     * {@code jTextFieldLowerLimit}.
     */
    private JTextArea jTextAreaUpperLimit;
    /**
     * Defines a lower limit for values displayed in this
     * {@code ChartFrame}.
     */
    private JTextField jTextFieldLowerLimit;
    /**
     * Defines an upper limit for values displayed in this
     * {@code ChartFrame}.
     */
    private JTextField jTextFieldUpperLimit;

    /**
     * Creates a new {@code ChartFrame} that displays histograms. A
     * {@code ChartFrame} is opened by the {@code TableFrame}-class
     * when "View-> New chart" is selected in the menu.
     *
     * @param myTableFrame {@code TableFrame} that opened this
     *                     {@code ChartFrame}
     */
    ChartFrame(final TableFrame myTableFrame) {
        super();
        this.myTableFrame = myTableFrame;
        this.currentElement = this.myTableFrame.getCurrentElement();
        initialize();
        this.updateChart();
    }

    /**
     * Initializes the {@code jComboBoxHistogramSelector} that selects the
     * histogram displayed in this {@code ChartFrame}.
     *
     * @return the {@code jComboBoxHistogramSelector}
     */
    private JComboBox<AbstractHistogram> getJComboBoxHistogramSelector() {
        if (jComboBoxHistogramSelector == null) {
            jComboBoxHistogramSelector = new JComboBox<>();
            jComboBoxHistogramSelector.setMaximumRowCount(10);

            // If autoChart is activated, create one UniversalHistogram per
            // column
            if (LogFileAnalyzer.getInstance().getAutoChart()) {
                String[] columnNames = LogFileAnalyzer.getInstance().getColumnNames();
                for (String columnName : columnNames) {
                    jComboBoxHistogramSelector.addItem(new UniversalHistogram(columnName));
                }
            }

            // Add special histogram to the ComboBox...
            ArrayList<AbstractHistogram> histograms = (ArrayList<AbstractHistogram>) LogFileAnalyzer.getInstance().getHistograms();
            histograms.forEach(jComboBoxHistogramSelector::addItem);

            jComboBoxHistogramSelector.addItemListener(e -> {
                // Delete lower and upper limit on change
                jTextFieldLowerLimit.setText("");
                jTextFieldUpperLimit.setText("");
                updateChart();
            });
        }
        return jComboBoxHistogramSelector;
    }

    /**
     * Initializes the {@code jComboBoxTableSelector} that selects the
     * table displayed in this {@code ChartFrame}.
     *
     * @return the {@code jComboBoxTableSelector}
     */
    @SuppressWarnings("unchecked")
    private JComboBox<String[]> getJComboBoxTableSelector() {
        if (jComboBoxTableSelector == null) {
            jComboBoxTableSelector = new JComboBox(tableSelection);
            jComboBoxTableSelector.addItemListener(e -> updateChart());
        }
        return jComboBoxTableSelector;
    }

    /**
     * Initializes the {@code jPanelComboBoxes}. The panel contains
     * ComboBoxes to select the classifier set and the histogram displayed in
     * this {@code ChartFrame}.
     *
     * @return the {@code jPanelComboBoxes}
     */
    private JPanel getJPanelComboBoxes() {
        if (jPanelComboBoxes == null) {
            GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.fill = GridBagConstraints.VERTICAL;
            gridBagConstraints3.weightx = 1.0;
            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            gridBagConstraints2.fill = GridBagConstraints.VERTICAL;
            gridBagConstraints2.weightx = 1.0;
            jPanelComboBoxes = new JPanel();
            jPanelComboBoxes.setLayout(new GridBagLayout());
            jPanelComboBoxes.add(getJComboBoxTableSelector(), gridBagConstraints2);
            jPanelComboBoxes.add(getJComboBoxHistogramSelector(), gridBagConstraints3);
        }
        return jPanelComboBoxes;
    }

    /**
     * Initializes the {@code jPanelContentpane} that contains all panels
     * used in this {@code ChartFrame}.
     *
     * @return the {@code jPanelContentpane}
     */
    private JPanel getJPanelContentpane() {
        if (jPanelContentpane == null) {
            jPanelContentpane = new JPanel();
            BorderLayout borderLayout = new BorderLayout();
            borderLayout.setHgap(1);
            borderLayout.setVgap(1);
            jPanelContentpane.setLayout(borderLayout);
            jTextAreaIteration = new JTextArea("iteration: <?>");
            jTextAreaIteration.setBackground(new Color(238, 238, 238));
            jTextAreaIteration.setEditable(false);
            jPanelContentpane.add(getJPanelComboBoxes(), BorderLayout.NORTH);
            jPanelContentpane.add(getJPanelInfoAndHistogram(), BorderLayout.CENTER);
            jPanelContentpane.add(getJPanelLimits(), BorderLayout.SOUTH);

        }
        return jPanelContentpane;
    }

    /**
     * Initializes the {@code jPanelHistogram} that contains the histogram.
     *
     * @return the {@code jPanelChart}
     */
    private JPanel getJPanelHistogram() {
        if (jPanelHistogram == null) {
            jPanelHistogram = new JPanel();
            jPanelHistogram.setLayout(new GridBagLayout());
        }
        return jPanelHistogram;
    }

    /**
     * Initializes the {@code jPanelInfoAndHistogram} that contains the
     * {@code jPanelInformation} and the {@code jPanelHistogram}.
     *
     * @return the {@code jPanelInfoAndHistogram}
     */
    private JPanel getJPanelInfoAndHistogram() {
        if (jPanelInfoAndHistogram == null) {
            BorderLayout borderLayout = new BorderLayout();
            borderLayout.setHgap(1);
            borderLayout.setVgap(1);
            jPanelInfoAndHistogram = new JPanel();
            jPanelInfoAndHistogram.setLayout(borderLayout);
            jPanelInfoAndHistogram.add(getJPanelInformation(), BorderLayout.NORTH);
            jPanelInfoAndHistogram.add(getJPanelHistogram(), BorderLayout.CENTER);
        }
        return jPanelInfoAndHistogram;
    }

    /**
     * Initializes the {@code jPanelInformation}. The panel contains the
     * iteration and the number of classifiers displayed in this
     * {@code ChartFrame}.
     *
     * @return the {@code jPanelInformation}
     */
    private JPanel getJPanelInformation() {
        if (jPanelInformation == null) {
            jPanelInformation = new JPanel();
            jPanelInformation.setLayout(new BorderLayout());
            jPanelInformation.add(jTextAreaIteration, BorderLayout.WEST);
            jPanelInformation.add(getJTextAreaClassifiers(), BorderLayout.EAST);
        }
        return jPanelInformation;
    }

    /**
     * Initializes the {@code jPanelLimits}. The panel contains subpanels
     * that contain {@code TextField}s to define lower and upper limits for
     * values displayed in this {@code ChartFrame}.
     *
     * @return the {@code jPanelLimits}
     */
    private JPanel getJPanelLimits() {
        if (jPanelLimits == null) {
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.insets = new Insets(0, 5, 0, 0);
            gridBagConstraints1.gridy = 0;
            gridBagConstraints1.gridx = 1;
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.insets = new Insets(0, 0, 0, 5);
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridx = 0;
            jPanelLimits = new JPanel();
            jPanelLimits.setLayout(new GridBagLayout());
            jPanelLimits.setVisible(true);
            jPanelLimits.add(getJPanelLowerLimit(), gridBagConstraints);
            jPanelLimits.add(getJPanelUpperLimit(), gridBagConstraints1);
        }
        return jPanelLimits;
    }

    /**
     * Initializes the {@code jPanelLowerLimit}. The panel contains a
     * {@code TextField} to define a lower limit for values displayed in
     * this {@code ChartFrame}.
     *
     * @return the {@code jPanelLowerLimit}
     */
    private JPanel getJPanelLowerLimit() {
        if (jPanelLowerLimit == null) {
            BorderLayout borderLayout2 = new BorderLayout();
            borderLayout2.setHgap(1);
            borderLayout2.setVgap(1);
            jPanelLowerLimit = new JPanel();
            jPanelLowerLimit.setLayout(borderLayout2);
            jPanelLowerLimit.add(getJTextAreaLowerLimit(), BorderLayout.WEST);
            jPanelLowerLimit.add(getJTextFieldLowerLimit(), BorderLayout.CENTER);
        }
        return jPanelLowerLimit;
    }

    /**
     * Initializes the {@code jPanelUpperLimit}. The panel contains a
     * {@code TextField} to define an upper limit for values displayed in
     * this {@code ChartFrame}.
     *
     * @return the {@code jPanelUpperLimit}
     */
    private JPanel getJPanelUpperLimit() {
        if (jPanelUpperLimit == null) {
            BorderLayout borderLayout1 = new BorderLayout();
            borderLayout1.setHgap(1);
            borderLayout1.setVgap(1);
            jPanelUpperLimit = new JPanel();
            jPanelUpperLimit.setLayout(borderLayout1);
            jPanelUpperLimit.add(getJTextAreaUpperLimit(), BorderLayout.WEST);
            jPanelUpperLimit.add(getJTextFieldUpperLimit(), BorderLayout.CENTER);
        }
        return jPanelUpperLimit;
    }

    /**
     * Initializes the {@code jTextAreaClassifiers} that contains the
     * number of classifiers displayed in this {@code ChartFrame}.
     *
     * @return the {@code jTextAreaClassifiers}
     */
    private JTextArea getJTextAreaClassifiers() {
        if (jTextAreaClassifiers == null) {
            jTextAreaClassifiers = new JTextArea();
            jTextAreaClassifiers.setBackground(new Color(238, 238, 238));
            jTextAreaClassifiers.setEditable(false);
        }
        return jTextAreaClassifiers;
    }

    /**
     * Initializes the {@code jTextAreaLowerLimit} that provides a
     * description for the corresponding {@code jTextFieldLowerLimit}.
     *
     * @return the {@code jTextAreaLowerLimit}
     */
    private JTextArea getJTextAreaLowerLimit() {
        if (jTextAreaLowerLimit == null) {
            jTextAreaLowerLimit = new JTextArea();
            jTextAreaLowerLimit.setText("Lower limit:");
            jTextAreaLowerLimit.setEditable(false);
            jTextAreaLowerLimit.setBackground(new Color(238, 238, 238));
        }
        return jTextAreaLowerLimit;
    }

    /**
     * Initializes the {@code jTextAreaUpperLimit} that provides a
     * description for the corresponding {@code jTextFieldUpperLimit}.
     *
     * @return the {@code jTextAreaUpperLimit}
     */
    private JTextArea getJTextAreaUpperLimit() {
        if (jTextAreaUpperLimit == null) {
            jTextAreaUpperLimit = new JTextArea();
            jTextAreaUpperLimit.setText("Upper limit:");
            jTextAreaUpperLimit.setEditable(false);
            jTextAreaUpperLimit.setBackground(new Color(238, 238, 238));
        }
        return jTextAreaUpperLimit;
    }

    /**
     * Initializes the {@code jTextFieldLowerLimit} that defines a lower
     * limit for values displayed in this {@code ChartFrame}.
     *
     * @return the {@code jTextFieldLowerLimit}
     */
    private JTextField getJTextFieldLowerLimit() {
        if (jTextFieldLowerLimit == null) {
            jTextFieldLowerLimit = new JTextField();
            jTextFieldLowerLimit.setPreferredSize(new Dimension(60, 20));
            jTextFieldLowerLimit.setMinimumSize(new Dimension(60, 20));
            jTextFieldLowerLimit.addActionListener(e -> {
                try {

                    String strLowerLimit = replaceCommas(jTextFieldLowerLimit.getText());
                    double lowerLimit = Double.parseDouble(strLowerLimit);

                    String strUpperLimit = replaceCommas(jTextFieldUpperLimit.getText());

                    double upperLimit;
                    try {
                        upperLimit = Double.parseDouble(strUpperLimit);
                    } catch (NumberFormatException nfeUupper) {
                        upperLimit = Double.NaN;
                    }
                    if (upperLimit < lowerLimit) {
                        jTextFieldUpperLimit.setText(String.valueOf(lowerLimit));
                    }
                    jTextFieldLowerLimit.setText(String.valueOf(lowerLimit));
                    updateChart();
                } catch (NumberFormatException nfeLower) {
                    String strLowerLimit = jTextFieldLowerLimit.getText();
                    if (strLowerLimit.isEmpty()) {
                        updateChart();
                    } else {
                        jTextFieldLowerLimit.setText("<?>");
                    }
                }
            });
        }
        return jTextFieldLowerLimit;
    }

    /**
     * Initializes the {@code jTextFieldUpperLimit} that defines an upper
     * limit for values displayed in this {@code ChartFrame}.
     *
     * @return the {@code jTextFieldUpperLimit}
     */
    private JTextField getJTextFieldUpperLimit() {
        if (jTextFieldUpperLimit == null) {
            jTextFieldUpperLimit = new JTextField();
            jTextFieldUpperLimit.setPreferredSize(new Dimension(60, 20));
            jTextFieldUpperLimit.setMinimumSize(new Dimension(60, 20));
            jTextFieldUpperLimit.addActionListener(e -> {
                try {

                    String strUpperLimit = replaceCommas(jTextFieldUpperLimit.getText());
                    double upperLimit = Double.parseDouble(strUpperLimit);

                    String strLowerLimit = replaceCommas(jTextFieldLowerLimit.getText());
                    double lowerLimit;

                    try {
                        lowerLimit = Double.parseDouble(strLowerLimit);
                    } catch (NumberFormatException nfeLower) {
                        lowerLimit = Double.NaN;
                    }

                    if (upperLimit < lowerLimit) {
                        jTextFieldLowerLimit.setText(String.valueOf(upperLimit));
                    }

                    jTextFieldUpperLimit.setText(String.valueOf(upperLimit));
                    updateChart();
                } catch (NumberFormatException nfeUpper) {
                    String strUpperLimit = jTextFieldUpperLimit.getText();
                    if (strUpperLimit.isEmpty()) {
                        updateChart();
                    } else {
                        jTextFieldUpperLimit.setText("<?>");
                    }
                }
            });
        }
        return jTextFieldUpperLimit;
    }

    /**
     * Returns the {@code TableFrame} that opened this
     * {@code ChartFrame}.
     *
     * @return the {@code TableFrame} that opened this
     * {@code ChartFrame}
     */
    public final TableFrame getMyTableFrame() {
        return this.myTableFrame;
    }

    /**
     * Initializes a new {@code ChartFrame}.
     */
    private void initialize() {
        this.setSize(323, 275);
        this.setLocation(myTableFrame.getLocation().x + 20, myTableFrame.getLocation().y + 20);

        this.setContentPane(getJPanelContentpane());
        this.setTitle("Chart");
        this.setVisible(true);

        addWindowListener(new ChartClosedListener(this));
    }

    /**
     * Updates this {@code ChartFrame} with the current
     * {@code DataElement} of the {@code TableFrame}.
     */
    final void loadNewDataSet() {
        this.currentElement = myTableFrame.getCurrentElement();
        this.updateChart();
    }

    /**
     * Updates this {@code ChartFrame} with the data set given as
     * parameter.
     *
     * @param _newDataSet the new data set that will be displayed
     */
    private void loadNewDataSet(final DataElement _newDataSet) {
        this.currentElement = _newDataSet;
        this.updateChart();
    }

    /**
     * Replaces all commas ({@code ','}) in the given {@code String}
     * by points ({@code '.'}). Furthermore, only the rightmost point is
     * kept.
     *
     * @param str a {@code String} for processing
     * @return the processed {@code String}
     */
    private String replaceCommas(String str) {
        // Replace "," by "."
        str = str.replaceAll(",", ".");

        // Delete all "." except of the last one
        while (str.lastIndexOf(".") != str.indexOf(".")) {
            str = str.substring(0, str.indexOf(".")) + str.substring(str.indexOf(".") + 1);
        }

        return str;
    }

    /**
     * Replaces the {@code jPanelHistogram} with the panel given as
     * parameter.
     *
     * @param newPanel the new panel that will replace the current
     *                 {@code jPanelHistogram}
     */
    private void setNewChartPanel(final JPanel newPanel) {
        jPanelInfoAndHistogram.remove(jPanelHistogram);
        jPanelHistogram = newPanel;
        jPanelInfoAndHistogram.add(newPanel);
    }

    /**
     * Updates this {@code ChartFrame} after changes.
     */
    private void updateChart() {
        this.setTitle("Chart: " + jComboBoxTableSelector.getSelectedItem() + " - "
                + jComboBoxHistogramSelector.getSelectedItem());

        int selectedTable = jComboBoxTableSelector.getSelectedIndex();
        AbstractHistogram selectedHistogram = (AbstractHistogram) jComboBoxHistogramSelector.getSelectedItem();

        if (this.currentElement != null && selectedHistogram != null) {
            int numberOfClassifiers = currentElement.getPopulation().getRowCount();
            if (selectedTable == 1) {
                numberOfClassifiers = currentElement.getMatchSet().getRowCount();
            }
            if (selectedTable == 2) {
                numberOfClassifiers = currentElement.getActionSet().getRowCount();
            }

            double upperLimit;
            double lowerLimit;

            try {
                upperLimit = Double.parseDouble(jTextFieldUpperLimit.getText());
            } catch (NumberFormatException nfe) {
                upperLimit = Double.NaN;
            }

            try {
                lowerLimit = Double.parseDouble(jTextFieldLowerLimit.getText());
            } catch (NumberFormatException nfe) {
                lowerLimit = Double.NaN;
            }

            JPanel chartPanel = selectedHistogram
                    .createHistogram(currentElement, selectedTable, lowerLimit, upperLimit);
            this.setNewChartPanel(chartPanel);

            String numberOfClassifiersString = "# classifiers: " + numberOfClassifiers;
            int shownElements = selectedHistogram.getNumberOfVisibleClassifiers();
            if (shownElements != numberOfClassifiers) {
                numberOfClassifiersString = "# classifiers: " + shownElements + "/" + numberOfClassifiers;
            }

            jTextAreaIteration.setText("iteration: " + currentElement.getIteration());
            jTextAreaClassifiers.setText(numberOfClassifiersString);

        } else {
            jTextAreaIteration.setText("iteration: <?>");
            jTextAreaClassifiers.setText("# classifiers: 0");
            this.setNewChartPanel(new JPanel());
        }
    }
} // @jve:decl-index=0:visual-constraint="10,10"
