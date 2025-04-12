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

package de.dfg.oc.otc.logfileanalyzer;

import de.dfg.oc.otc.logfileanalyzer.gui.ChoosableFileFilter;
import de.dfg.oc.otc.logfileanalyzer.gui.TableFrame;
import de.dfg.oc.otc.logfileanalyzer.histogram.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * The {@code LogFileAnalyzer} is the program's run class. It determines
 * how log-files are imported and how histograms are created. Furthermore, it
 * defines the column names used in the program's tables. A
 * {@code LogFileAnalyzer} is a singleton, its instance can be obtained by
 * using the static {@code getInstance()}-method of this class. To adapt
 * the program to your needs, use the {@code setDataImporter()}-,
 * {@code setHistograms()}, and {@code setColumnNames()}-methods. To
 * run the program, call its {@code startLogFileAnalyzer()}-method.
 *
 * @author Clemens Gersbacher, Holger Prothmann
 */
public final class LogFileAnalyzer {
    /**
     * Singleton-Instance of the LogFileAnalyzer.
     */
    private static LogFileAnalyzer lfa;
    private static final double VERSION = 1.1;

    /**
     * Returns the singleton instance of the {@code LogFileAnalyzer}.
     *
     * @return the singleton instance of the {@code LogFileAnalyzer}
     */
    public static LogFileAnalyzer getInstance() {
        if (lfa == null) {
            lfa = new LogFileAnalyzer();
        }
        return lfa;
    }

    /**
     * Returns the version number.
     *
     * @return the version number
     */
    public static double getVERSION() {
        return VERSION;
    }

    /**
     * Starts the program.
     *
     * @param args command line arguments are currently not supported
     */
    public static void main(final String[] args) {
        LogFileAnalyzer lfa = LogFileAnalyzer.getInstance();

        // Define which files are show in the "Open"-dialog
        lfa.addFileFilter(new ChoosableFileFilter("LCS Log-File (*LCS.log)", "*LCS.log", true));

        // Create histograms
        lfa.autoChart = false;
        for (int i = 4; i < lfa.columnNames.length; i++) {
            lfa.addHistogram(new UniversalHistogram(lfa.columnNames[i]));
        }

        // Special histogram
        lfa.addHistogram(new PredictionDifferenceHistogram());
        lfa.addHistogram(new CondVolumeHistogram());
        lfa.addHistogram(new CycleTimeHistogram());

        lfa.startLogFileAnalyzer();
    }

    /**
     * Determines if histograms are automatically created for each table column.
     */
    private boolean autoChart = true;

    /**
     * Controls the auto-resize behavior of all tables. If set to
     * {@code true} every column gets the size it needs; if set to
     * {@code false} the columns are sized to fit the window.
     */
    private boolean autoResize = true;

    /**
     * Column names used in the table header.
     */
    private String[] columnNames = {"ID", "Created By", "Condition", "Action", "Current Pred.", "Initial Pred.",
            "Pred. Err.", "Fitness", "AS size", "Num.", "Exp."};

    /**
     * Importer responsible for reading log-files.
     */
    private DataImporterInterface dataImporter;

    /**
     * A list of filters that are applied to mask files in the "Open"-dialog.
     */
    private List<ChoosableFileFilter> fileOpenFilters;

    /**
     * A list of classes that are available for displaying histograms.
     */
    private List<AbstractHistogram> histograms;

    /**
     * Constructor. Sets all class attributes to defaults. Their values can be
     * changed by using the respective {@code set()}-methods.
     */
    private LogFileAnalyzer() {
        this.dataImporter = new OTCDataImporter();
        this.histograms = new ArrayList<>();
        this.fileOpenFilters = new ArrayList<>();
    }

    /**
     * Adds a new file filter to the list of filters used in the Open-dialog.
     *
     * @param filter file filter that will be added to the list of filters
     */
    void addFileFilter(final ChoosableFileFilter filter) {
        this.fileOpenFilters.add(filter);
    }

    /**
     * Adds a new histogram to the list of available histograms.
     *
     * @param histogram a new histogram that is added to the list of available
     *                  histograms
     */
    void addHistogram(final AbstractHistogram histogram) {
        this.histograms.add(histogram);
    }

    /**
     * Returns {@code true} if histograms are automatically created for all
     * table columns.
     *
     * @return {@code true} if histograms are automatically created for all
     * table columns
     */
    public boolean getAutoChart() {
        return this.autoChart;
    }

    /**
     * Returns the auto-resize behavior of all tables.
     *
     * @return {@code true} if every column gets the size it needs;
     * {@code false} if the columns are sized to fit the window
     */
    public boolean getAutoResize() {
        return this.autoResize;
    }

    /**
     * Returns the column names of classifier tables.
     *
     * @return the column names of classifier tables
     */
    public String[] getColumnNames() {
        return this.columnNames;
    }

    /**
     * Returns the {@code dataImporter} used for reading log-files.
     *
     * @return the {@code dataImporter} used for reading log-files
     */
    public DataImporterInterface getDataImporter() {
        return this.dataImporter;
    }

    /**
     * Returns a list of file filters that are used in the "Open"-dialog.
     *
     * @return a list of file filters that are used in the "Open"-dialog.
     */
    public List<ChoosableFileFilter> getFileOpenFilters() {
        return this.fileOpenFilters;
    }

    /**
     * Returns the histograms that are available for visualization.
     *
     * @return the histograms that are available for visualization
     */
    public List<AbstractHistogram> getHistograms() {
        return this.histograms;
    }

    /**
     * Determines if histograms are automatically created for all table columns.
     *
     * @param autoChart {@code true} to automatically create histograms
     */
    public void setAutoChart(final boolean autoChart) {
        this.autoChart = autoChart;
    }

    /**
     * Sets the auto-resize behavior of all tables.
     *
     * @param autoResize if set to {@code true}, every column gets the size it
     *                   needs; if set to {@code false} the columns are sized to
     *                   fit the window
     */
    public void setAutoResize(final boolean autoResize) {
        this.autoResize = autoResize;
    }

    /**
     * Sets the column names for all tables. The column names are also displayed
     * in the "View->Columns"-menu and are used if histograms are automatically
     * created for each column.
     *
     * @param columnNames a {@code String[]} containing column-headers
     */
    public void setColumnNames(final String[] columnNames) {
        this.columnNames = columnNames;
    }

    /**
     * Sets the {@code dataImporter} used for reading log-files.
     *
     * @param dataImporter {@code dataImporter} that needs to implement the
     *                     {@code DataImporterInterface}
     */
    public void setDataImporter(final DataImporterInterface dataImporter) {
        this.dataImporter = dataImporter;
    }

    /**
     * Sets a list of file filters that will used in the "Open"-dialog.
     *
     * @param fileOpenFilters a list of file filters that will used in the "Open"-dialog
     */
    public void setFileOpenFilters(final Vector<ChoosableFileFilter> fileOpenFilters) {
        this.fileOpenFilters = fileOpenFilters;
    }

    /**
     * Sets the histograms that will be available for visualization.
     *
     * @param histograms a {@code Vector} containing the histograms
     */
    public void setHistograms(final Vector<AbstractHistogram> histograms) {
        this.histograms = histograms;
    }

    /**
     * Creates a new GUI.
     */
    void startLogFileAnalyzer() {
        new TableFrame();
    }
}
