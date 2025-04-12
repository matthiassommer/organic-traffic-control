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

package de.dfg.oc.otc.logfileanalyzer.histogram;

import de.dfg.oc.otc.logfileanalyzer.DataElement;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract class for histograms. Inherit from this abstract class when creating
 * new histograms. The {@code calculateHistogramData()}-method has to
 * provide the data displayed in the histogram.
 *
 * @author Clemens Gersbacher, Holger Prothmann
 */
public abstract class AbstractHistogram {
    /**
     * Describes this histogram. The description is used for identification in
     * the histogram selection and as histogram title.
     */
    final String description;

    /**
     * Number of classifiers shown in the histogram.
     */
    private int numberOfVisibleClassifiers;

    /**
     * Constructor. Requires a description that is used for identification in
     * the histogram selection and as histogram title.
     *
     * @param description a description that is used for identification in the histogram
     *                    selection and as histogram title
     */
    AbstractHistogram(final String description) {
        this.description = description;
        this.numberOfVisibleClassifiers = 0;
    }

    /**
     * Abstract method. Determines the data displayed in the histogram.
     *
     * @param selectedTable the table that was selected by the user as basis for the
     *                      histogram
     * @return the data that will be displayed in the histogram
     */
    protected abstract List<Double> calculateHistogramData(DefaultTableModel selectedTable);

    /**
     * Creates a panel for error messages. The error message is given as
     * parameter.
     *
     * @param text the error message
     * @return a panel for error messages
     */
    private JPanel createErrorPanel(final String text) {
        JPanel jPanelChart = new JPanel();
        // ...display error notice instead of chart.
        JTextArea jTextAreaAnzeige = new JTextArea(text);
        jTextAreaAnzeige.setVisible(true);
        jTextAreaAnzeige.setBackground(new Color(238, 238, 238));
        jPanelChart.add(jTextAreaAnzeige);
        jPanelChart.setVisible(true);
        return jPanelChart;
    }

    /**
     * Returns a panel containing a histogram. The data displayed in the
     * histogram is given as parameter. Data not inside the given limits is
     * discarded.
     *
     * @param dataElement     a {@code DataElement} containing the classifier sets of
     *                        an iteration
     * @param selectedTableId identifier of the selected table ({@code 0} for
     *                        population, {@code 1} for match set, {@code 2} for
     *                        action set)
     * @param lowerLimit      the lower limit that was entered by the user
     * @param upperLimit      the upper limit that was entered by the user
     * @return a {@code JPanel} containing the histogram
     */

    public final JPanel createHistogram(final DataElement dataElement, final int selectedTableId,
                                        final double lowerLimit, final double upperLimit) {
        DefaultTableModel selectedTable = dataElement.getPopulation();

        if (selectedTableId == 1) {
            selectedTable = dataElement.getMatchSet();
        }
        if (selectedTableId == 2) {
            selectedTable = dataElement.getActionSet();
        }

        List<Double> histogramData;
        try {
            // This abstract method must be implemented according to your needs!
            histogramData = calculateHistogramData(selectedTable);
        } catch (NumberFormatException nfe) {
            return createErrorPanel(this + " cannot be displayed as histogram.");
        }

        // Create the histogram based on the data you calculated
        return createHistogram(histogramData, lowerLimit, upperLimit);
    }

    /**
     * Returns a panel containing a histogram. The data displayed in the
     * histogram is given as parameter. Data not inside the given limits is
     * discarded.
     *
     * @param histogramData the data displayed in the histogram
     * @param lowerLimit    the lower limit that was entered by the user
     * @param upperLimit    the upper limit that was entered by the user
     * @return a {@code JPanel} containing the histogram
     */
    private JPanel createHistogram(final List<Double> histogramData, final double lowerLimit, final double upperLimit) {
        // Remove values outside the given limits
        List<Double> vectorHistogramDataWithinLimits = histogramData.stream().filter(d -> valueWithinLimits(d, lowerLimit, upperLimit)).collect(Collectors.toList());

        // Store number of elements shown in histogram
        this.numberOfVisibleClassifiers = vectorHistogramDataWithinLimits.size();

        // Convert vector to array
        double[] arrayHistogramDataWithinLimits = new double[vectorHistogramDataWithinLimits.size()];
        for (int i = 0; i < vectorHistogramDataWithinLimits.size(); i++) {
            double d = vectorHistogramDataWithinLimits.get(i);
            arrayHistogramDataWithinLimits[i] = d;
        }

        if (arrayHistogramDataWithinLimits.length > 0) {
            // Create histogram
            HistogramDataset data = new HistogramDataset();
            // key, data, #bins
            data.addSeries("Suchwert", arrayHistogramDataWithinLimits,
                    Math.max(100, arrayHistogramDataWithinLimits.length));

            // title, x axis label, y axis label, data, orientation, legend,
            // tooltips, URL
            JFreeChart chart = ChartFactory.createHistogram(description, description, "frequency", data,
                    PlotOrientation.VERTICAL, false, true, false);

            return new ChartPanel(chart);
        }
        return createErrorPanel("No data available (within the given limits).");
    }

    /**
     * Return the number of classifiers shown in the histogram.
     *
     * @return the number of classifiers shown in the histogram
     */
    public final int getNumberOfVisibleClassifiers() {
        return this.numberOfVisibleClassifiers;
    }

    /**
     * Returns a description for this histogram. The description is used for
     * identification in the histogram selection and as histogram title.
     *
     * @return a description for this histogram
     */
    @Override
    public final String toString() {
        return description;
    }

    /**
     * Tests if a given value lies within two limits.
     *
     * @param value      the value that will be tested
     * @param lowerLimit the lower limit for the testWithUpper
     * @param upperLimit the upper limit for the testWithUpper
     * @return {@code true} iff
     * {@code _lowerlimit <= value <= _upperlimit}
     */
    final boolean valueWithinLimits(final double value, final double lowerLimit, final double upperLimit) {
        boolean result = true;

        if (!Double.isNaN(lowerLimit)) {
            result = lowerLimit <= value;
        }

        if (!Double.isNaN(upperLimit)) {
            result = result && upperLimit >= value;
        }

        return result;
    }
}
