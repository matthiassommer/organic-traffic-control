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
import de.dfg.oc.otc.logfileanalyzer.DataMemory;
import de.dfg.oc.otc.logfileanalyzer.LogFileAnalyzer;
import org.apache.commons.math3.util.FastMath;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Provides the run window containing the menu, buttons to step through the
 * iterations, and tables displaying the classifier sets. It also manages the
 * {@code ChartFrame}s and provides them with the current data.
 *
 * @author Clemens Gersbacher, Holger Prothmann
 */
@SuppressWarnings("serial")
public class TableFrame extends JFrame {
    /**
     * Contains all opened {@code ChartFrames} displaying histograms.
     */
    private final List<ChartFrame> chartFrames;

    /**
     * Containing the classifier sets of the current iteration.
     */
    private DataElement currentElement;

    /**
     * Contains all classifier sets.
     */
    private DataMemory dataMemory;

    /**
     * Folder containing the last opened ea log-files.
     */
    private File eaStatDirPath;

    /**
     * Gets the fist {@code DataElement} and updates the tables.
     */
    private JButton jButtonFirst;

    /**
     * Gets the last {@code DataElement} and updates the tables.
     */
    private JButton jButtonLast;

    /**
     * Gets the next {@code DataElement} and updates the tables.
     */
    private JButton jButtonNext;

    /**
     * Gets the previous {@code DataElement} and updates the tables.
     */
    private JButton jButtonPrevious;

    /**
     * {@code JCheckBoxItem} for selecting all table columns.
     */
    private JCheckBoxMenuItem jCheckBoxMenuItemAll;

    /**
     * {@code JCheckBoxItem} for deselecting all table columns.
     */
    private JCheckBoxMenuItem jCheckBoxMenuItemNone;

    /**
     * Contains the {@code jPanelControlPane} and {@code jTabbedPane}.
     */
    private JPanel jContentPane;

    /**
     * MenuBar.
     */
    private JMenuBar jJMenuBar;

    /**
     * File menu.
     */
    private JMenu jMenuFile;

    /**
     * Menu item "EA statistics".
     */
    private JMenuItem jMenuItemEAStatistics;

    /**
     * File menu item "exit".
     */
    private JMenuItem jMenuItemExit;

    /**
     * File menu item "open".
     */
    private JMenuItem jMenuItemOpen;

    /**
     * Menu item "New chart".
     */
    private JMenuItem jMenuNewChart;

    /**
     * Menu item to hide or show columns.
     */
    private JMenu jMenuSelectColumns;

    /**
     * View menu.
     */
    private JMenu jMenuView;

    /**
     * Contains the {@code jTextFieldIteration, jButtonNext,
     * jButtonPrevious, jButtonFirst} and {@code jButtonLast}.
     */
    private JPanel jPanelControlpane;

    /**
     * Contains a table for the action set.
     */
    private JScrollPane jScrollPaneActionSet;

    /**
     * Contains a table for the match set.
     */
    private JScrollPane jScrollPaneMatchSet;

    /**
     * Contains the population table.
     */
    private JScrollPane jScrollPanePopulation;

    /**
     * Tabs for switching among population, match and action sets.
     */
    private JTabbedPane jTabbedPane;

    /**
     * Contains the match set.
     */
    private JTable jTableActionSet;

    /**
     * Contains the match set.
     */
    private JTable jTableMatchSet;

    /**
     * Contains a table for the classifier population.
     */
    private JTable jTablePopulation;

    /**
     * Textfield "LCS input".
     */
    private JTextField jTextFieldInput;

    /**
     * Shows the current iteration.
     */
    private JTextField jTextFieldIteration;

    /**
     * Path to the last opened file.
     */
    private File logFilePath;

    /**
     * Array creating a {@code JCheckBoxItem}-object for every column of
     * the table. With these checkboxes you can hide or show a specific column.
     */
    private JCheckBoxMenuItem[] viewItems;

    // ---------------------------------------------------------------

    /**
     * Constructor. Starts the GUI.
     */
    public TableFrame() {
        super();
        chartFrames = new ArrayList<>();
        initialize();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    /**
     * Stores a new {@code ChartFrame} into this class' vector of
     * {@code ChartFrame}s.
     *
     * @param newChartFrame a new {@code ChartFrame}
     */
    final void addChartFrame(final ChartFrame newChartFrame) {
        chartFrames.add(newChartFrame);
    }

    /**
     * Adds {@code RowSorter}s to every column of the given table.
     *
     * @param table table that will equipped with {@code RowSorter}s
     */
    private void addTabellenRowSort(final JTable table) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(
                table.getModel());
        for (int i = 0; i < table.getModel().getColumnCount(); i++) {
            sorter.setComparator(i, new TableElementComparator());
        }
        table.setRowSorter(sorter);
    }

    /**
     * Returns the current {@code DataElement} with the currently shown
     * data.
     *
     * @return the current {@code DataElement}
     */
    public final DataElement getCurrentElement() {
        return this.currentElement;
    }

    /**
     * Initializes the {@code jButtonFirst}. On click the first
     * {@code DataElement} is loaded.
     *
     * @return the {@code jButtonFirst}
     */
    private JButton getJButtonFirst() {
        if (jButtonFirst == null) {
            jButtonFirst = new JButton();
            jButtonFirst.setText("First");
            jButtonFirst.setEnabled(false);
            jButtonFirst.addActionListener(e -> {
                currentElement = dataMemory.getFirstElement();
                refresh();
            });
        }
        return jButtonFirst;
    }

    /**
     * Initializes the {@code jButtonLast}. On click the last
     * {@code DataElement} is loaded.
     *
     * @return the {@code jButtonLast}
     */
    private JButton getJButtonLast() {
        if (jButtonLast == null) {
            jButtonLast = new JButton();
            jButtonLast.setText("Last");
            jButtonLast.setEnabled(false);
            jButtonLast.addActionListener(e -> {
                currentElement = dataMemory.getLastElement();
                refresh();
            });
        }
        return jButtonLast;
    }

    /**
     * Initializes the {@code jButtonNext}. On click the next
     * {@code DataElement} is loaded.
     *
     * @return the {@code jButtonNext}
     */
    private JButton getJButtonNext() {
        if (jButtonNext == null) {
            jButtonNext = new JButton();
            jButtonNext.setText("+");
            jButtonNext.setEnabled(false);
            jButtonNext.addActionListener(e -> {
                currentElement = currentElement.getNextElement();
                refresh();
            });
        }
        return jButtonNext;
    }

    /**
     * Initializes the {@code jButtonPrevious}. On click the previous
     * {@code DataElement} is loaded.
     *
     * @return the {@code jButtonPrevious}
     */
    private JButton getJButtonPrevious() {
        if (jButtonPrevious == null) {
            jButtonPrevious = new JButton();
            jButtonPrevious.setText("-");
            jButtonPrevious.setEnabled(false);
            jButtonPrevious
                    .addActionListener(e -> {
                        currentElement = currentElement
                                .getPreviousElement();
                        refresh();
                    });
        }
        return jButtonPrevious;
    }

    /**
     * Initializes the {@code jCheckBoxMenuItemAll}.
     *
     * @return {@code jCheckBoxMenuItemAll}
     */
    private JCheckBoxMenuItem getJCheckBoxMenuItemAll() {
        if (jCheckBoxMenuItemAll == null) {
            jCheckBoxMenuItemAll = new JCheckBoxMenuItem();
            jCheckBoxMenuItemAll.setText("Select all");
            jCheckBoxMenuItemAll.setSelected(true);
            jCheckBoxMenuItemAll
                    .addItemListener(e -> {
                        jCheckBoxMenuItemAll.setSelected(true);
                        for (JCheckBoxMenuItem viewItem : viewItems) {
                            viewItem.setSelected(true);
                        }
                    });
        }
        return jCheckBoxMenuItemAll;
    }

    /**
     * Initializes the {@code jCheckBoxMenuItemNone}.
     *
     * @return the {@code jCheckBoxMenuItemNone}
     */
    private JCheckBoxMenuItem getJCheckBoxMenuItemNone() {
        if (jCheckBoxMenuItemNone == null) {
            jCheckBoxMenuItemNone = new JCheckBoxMenuItem();
            jCheckBoxMenuItemNone.setText("Deselect all");
            jCheckBoxMenuItemNone
                    .addItemListener(e -> {

                        jCheckBoxMenuItemNone.setSelected(false);
                        for (JCheckBoxMenuItem viewItem : viewItems) {
                            viewItem.setSelected(false);
                        }
                    });
        }
        return jCheckBoxMenuItemNone;
    }

    /**
     * Initializes the {@code jContentPane}.
     *
     * @return the {@code jContentPane}
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
            jContentPane.add(getJPanelControlpane(), BorderLayout.NORTH);
            jContentPane.add(getJTabbedPane(), BorderLayout.CENTER);
        }
        return jContentPane;
    }

    /**
     * Initializes the {@code jJMenuBar} and adds {@code jMenuFile}
     * and {@code jMenuView}.
     *
     * @return the {@code jJMenuBar}.
     */
    private JMenuBar getJJMenuBar() {
        if (jJMenuBar == null) {
            jJMenuBar = new JMenuBar();
            jJMenuBar.add(getJMenuFile());
            jJMenuBar.add(getJMenuView());
        }
        return jJMenuBar;
    }

    /**
     * Initializes the {@code jMenuFile} and adds
     * {@code jMenuItemOpen} and {@code jMenuItemExit}.
     *
     * @return the {@code jMenuFile}
     */
    private JMenu getJMenuFile() {
        if (jMenuFile == null) {
            jMenuFile = new JMenu();
            jMenuFile.setText("File");
            jMenuFile.add(getJMenuItemOpen());
            jMenuFile.add(getJMenuItemEAStatistics());
            jMenuFile.add(getJMenuItemExit());
        }
        return jMenuFile;
    }

    /**
     * Initializes the {@code jMenuNewChart} and adds an
     * {@code actionListener}. On action a new {@code ChartFrame} is
     * created.
     *
     * @return the {@code jMenuNewChart}
     */
    private JMenuItem getJMenuItemChart() {
        if (jMenuNewChart == null) {
            jMenuNewChart = new JMenuItem();
            jMenuNewChart.setText("New histogram");
            jMenuNewChart.addActionListener(new ChartOpenedListener(this));
        }
        return jMenuNewChart;
    }

    /**
     * Die Methode initialisiert jMenuItemEAStatistics und f�gt einen
     * ActionListener ein, der einen Datei�ffnen-Dialog samt Filter startet.
     * Anschlie�end werden die Daten des ausgew�hlten Verzeichnisses eingelesen.
     *
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getJMenuItemEAStatistics() {
        if (jMenuItemEAStatistics == null) {
            jMenuItemEAStatistics = new JMenuItem();
            jMenuItemEAStatistics.setText("Load EA statistics");
            jMenuItemEAStatistics
                    .addActionListener(e -> {
                        JFileChooser chooser = new JFileChooser(
                                eaStatDirPath);
                        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

                        // Filter f�r Datei�ffnen werden definiert.
                        FileFilter eaLogFilter = new ChoosableFileFilter(
                                "EA Log-File (*OptLog*.txt)",
                                "*OptLog*.txt", true);
                        chooser.addChoosableFileFilter(eaLogFilter);
                        FileNameExtensionFilter logFilter = new FileNameExtensionFilter(
                                "Log-File (*.log; *.txt)", "log", "txt");
                        chooser.addChoosableFileFilter(logFilter);
                        chooser.setFileFilter(eaLogFilter);

                        // Datei�ffnen-Dialog wird gestartet.
                        int returnVal = chooser.showOpenDialog(null);
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            // Wenn OK werden Daten durch das Erzeugen des
                            // Datenspeichers eingelesen.
                            File verzeichnis = chooser.getSelectedFile();
                            if (!verzeichnis.isDirectory()) {
                                verzeichnis = chooser.getCurrentDirectory();
                            }

                            java.io.FileFilter eaLogFilesFilter = new ChoosableFileFilter(
                                    "EA Log-File (*OptLog*.txt)",
                                    "*OptLog*.txt", false);

                            File[] logDateien = verzeichnis
                                    .listFiles(eaLogFilesFilter);
                            if (logDateien.length < 1) {
                                eaStatDirPath = verzeichnis;
                                JOptionPane
                                        .showMessageDialog(
                                                null,
                                                "Verzeichnis "
                                                        + verzeichnis
                                                        + " enth�lt keine EA-Log-Dateien",
                                                "Warnung",
                                                JOptionPane.WARNING_MESSAGE,
                                                null
                                        );
                                return;
                            }

                            // String alleDateien = "Dateinamen: \n";
                            int[] durations = new int[logDateien.length];

                            int numRuns = logDateien.length;
                            for (int i = 0; i < numRuns; i++) {
                                try {
                                    FileReader fr = new FileReader(
                                            logDateien[i]);
                                    BufferedReader br = new BufferedReader(
                                            fr);
                                    String clString;
                                    while ((clString = br.readLine()) != null) {
                                        if (!clString.contains("DURATION")) {
                                        } else {
                                            String duration = clString
                                                    .substring(clString
                                                            .indexOf(" "));
                                            duration = duration.trim();
                                            if (duration.length() < 8) {
                                                JOptionPane
                                                        .showMessageDialog(
                                                                null,
                                                                "Problem reading Duration from "
                                                                        + logDateien[i],
                                                                "Fehler",
                                                                JOptionPane.ERROR_MESSAGE,
                                                                null
                                                        );
                                            }
                                            int hours = Integer.valueOf(duration
                                                    .substring(0, 2));
                                            int minutes = Integer.valueOf(duration
                                                    .substring(3, 5));
                                            int seconds = Integer.valueOf(duration
                                                    .substring(6, 8));
                                            durations[i] = seconds
                                                    + minutes * 60 + hours
                                                    * 3600;
                                        }
                                    }
                                    br.close();
                                } catch (IOException ioexcept) {
                                    JOptionPane
                                            .showMessageDialog(
                                                    null,
                                                    "Problem reading "
                                                            + logDateien[i],
                                                    "Fehler",
                                                    JOptionPane.ERROR_MESSAGE,
                                                    null
                                            );
                                }
                            }

                            int min = durations[0];
                            int max = min;
                            double average = min;
                            for (int i = 1; i < numRuns; i++) {
                                average += durations[i];
                                if (durations[i] < min) {
                                    min = durations[i];
                                } else if (durations[i] > max) {
                                    max = durations[i];
                                }
                            }
                            average /= numRuns;

                            double variance = 0;
                            for (int i = 0; i < numRuns; i++) {
                                variance += FastMath.pow(
                                        durations[i] - average, 2);
                            }
                            variance /= numRuns - 1;
                            variance = Math.sqrt(variance);

                            Formatter f = new Formatter();
                            JOptionPane.showMessageDialog(
                                    null,
                                    numRuns
                                            + " L�ufe, durchschnittliche Dauer "
                                            + f.format(
                                            "%.2f Sekunden mit einer Standardabweichung von %.2f",
                                            average, variance)
                                            + " Sekunden.\n K�rzester Lauf: "
                                            + min
                                            + " Sekunden, l�ngster Lauf "
                                            + max + " Sekunden."
                            );
                            // Speichern des letzten Pfades
                            eaStatDirPath = chooser.getCurrentDirectory();
                            f.close();
                        }
                    });
        }
        return jMenuItemEAStatistics;
    }

    /**
     * Initializes the {@code jMenuItemExit} and adds an
     * {@code actionListener}. On click the program is closed.
     *
     * @return the {@code jMenuItemExit}
     */
    private JMenuItem getJMenuItemExit() {
        if (jMenuItemExit == null) {
            jMenuItemExit = new JMenuItem();
            jMenuItemExit.setText("Exit");
            jMenuItemExit
                    .addActionListener(e -> System.exit(0));
        }
        return jMenuItemExit;
    }

    /**
     * Initializes the {@code jMenuItemOpen} and adds an
     * {@code actionListener}. On click an OpenFile-dialogue is opened and
     * the {@code DataMemory} is created.
     *
     * @return the {@code jMenuItemOpen}
     */
    private JMenuItem getJMenuItemOpen() {
        if (jMenuItemOpen == null) {
            jMenuItemOpen = new JMenuItem();
            jMenuItemOpen.setText("Open log-File");
            jMenuItemOpen
                    .addActionListener(e -> {
                        JFileChooser chooser = new JFileChooser(logFilePath);
                        // own filters are added
                        List<ChoosableFileFilter> fileOpenFilters = LogFileAnalyzer
                                .getInstance().getFileOpenFilters();
                        if (!fileOpenFilters.isEmpty()) {
                            fileOpenFilters.forEach(chooser::addChoosableFileFilter);
                            chooser.setFileFilter(fileOpenFilters.get(0));
                        }
                        // if no own filters were specified, a default
                        // filter is created
                        else {
                            FileNameExtensionFilter logFilter = new FileNameExtensionFilter(
                                    "Log-File (*.log; *.txt)", "log", "txt");
                            chooser.addChoosableFileFilter(logFilter);
                            chooser.setFileFilter(logFilter);
                        }
                        // OpenFile-Dialogue is started
                        int returnVal = chooser.showOpenDialog(null);

                        // on OK
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            File logFile = chooser.getSelectedFile();
                            // dataManager is created
                            dataMemory = new DataMemory(logFile);
                            // data are read in
                            dataMemory.readData();
                            // get first element
                            currentElement = dataMemory.getFirstElement();
                            refresh();
                            // save directory
                            logFilePath = chooser.getCurrentDirectory();
                            // set file name in title
                            setTitle("LogFileAnalyzer "
                                    + LogFileAnalyzer.getVERSION() + " - "
                                    + logFile.getName());
                        }
                    });
        }
        return jMenuItemOpen;
    }

    /**
     * Initializes the {@code jMenuSelectColumns}. For each column of the
     * table a checkbox-item is created.
     *
     * @return the {@code jMenuSelectColumns}
     */
    private JMenu getJMenuTable() {
        if (jMenuSelectColumns == null) {
            jMenuSelectColumns = new JMenu();
            jMenuSelectColumns.setText("Columns");
            jMenuSelectColumns.setVisible(true);

            // add "Select All" and "Deselect All"
            jMenuSelectColumns.add(getJCheckBoxMenuItemAll());
            jMenuSelectColumns.add(getJCheckBoxMenuItemNone());

            jMenuSelectColumns.addSeparator();

            // get column-headers
            String[] columnNames = LogFileAnalyzer.getInstance()
                    .getColumnNames();
            int numberOfColumns = columnNames.length;

            // create checkbox-array and add actionListener
            viewItems = new JCheckBoxMenuItem[numberOfColumns];
            for (int i = 0; i < viewItems.length; i++) {
                if (viewItems[i] == null) {
                    viewItems[i] = new JCheckBoxMenuItem(columnNames[i], true);
                    viewItems[i].setName(columnNames[i]);
                    viewItems[i]
                            .addItemListener(e -> refresh());
                    jMenuSelectColumns.add(viewItems[i]);
                }
            }
        }
        return jMenuSelectColumns;
    }

    /**
     * Initializes the {@code jMenuView}.
     *
     * @return the {@code jMenuView}
     */
    private JMenu getJMenuView() {
        if (jMenuView == null) {
            jMenuView = new JMenu();
            jMenuView.setText("View");
            jMenuView.add(getJMenuTable());
            jMenuView.add(getJMenuItemChart());
        }
        return jMenuView;
    }

    /**
     * Initializes the {@code jPanelControlpane} and adds
     * {@code jTextFieldIteration}, {@code jButtonPrevious}
     * {@code jButtonNext}, {@code jButtonFirst} and
     * {@code jButtonLast}.
     *
     * @return the {@code jPanelControlpane}
     */
    private JPanel getJPanelControlpane() {
        if (jPanelControlpane == null) {
            /*
      Label "iteration".
	 */
            JLabel jLabel_iteration = new JLabel();
            jLabel_iteration.setText("iteration");
            jLabel_iteration.setHorizontalAlignment(SwingConstants.CENTER);
            jLabel_iteration.setPreferredSize(new Dimension(75, 16));
            /*
	  Label "LCS input".
	 */
            JLabel jLabel_input = new JLabel();
            jLabel_input.setText("LCS input");
            jLabel_input.setHorizontalTextPosition(SwingConstants.TRAILING);
            jLabel_input.setHorizontalAlignment(SwingConstants.CENTER);
            jLabel_input.setPreferredSize(new Dimension(75, 16));
            jPanelControlpane = new JPanel();
            jPanelControlpane.setLayout(new BoxLayout(getJPanelControlpane(),
                    BoxLayout.X_AXIS));
            jPanelControlpane.add(jLabel_iteration, null);
            jPanelControlpane.add(getJTextFieldIteration(), null);
            jPanelControlpane.add(jLabel_input, null);
            jPanelControlpane.add(getJTextField_input(), null);
            jPanelControlpane.add(getJButtonPrevious(), null);
            jPanelControlpane.add(getJButtonNext(), null);
            jPanelControlpane.add(getJButtonFirst(), null);
            jPanelControlpane.add(getJButtonLast(), null);

        }
        return jPanelControlpane;
    }

    /**
     * Initializes the {@code jScrollPaneActionSet}.
     *
     * @return the {@code jScrollPaneActionSet}
     */
    private JScrollPane getJScrollPaneActionSet() {
        if (jScrollPaneActionSet == null) {
            jScrollPaneActionSet = new JScrollPane();
            jScrollPaneActionSet.setViewportView(getJTableActionSet());
        }
        return jScrollPaneActionSet;
    }

    /**
     * Initializes the {@code jScrollPaneMatchSet}.
     *
     * @return the {@code jScrollPaneMatchSet}
     */
    private JScrollPane getJScrollPaneMatchSet() {
        if (jScrollPaneMatchSet == null) {
            jScrollPaneMatchSet = new JScrollPane();
            jScrollPaneMatchSet.setViewportView(getJTableMatchSet());
        }
        return jScrollPaneMatchSet;
    }

    /**
     * Initializes the {@code jScrollPanePopulation}.
     *
     * @return the {@code jScrollPanePopulation}
     */
    private JScrollPane getJScrollPanePopulation() {
        if (jScrollPanePopulation == null) {
            jScrollPanePopulation = new JScrollPane();
            jScrollPanePopulation
                    .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            jScrollPanePopulation
                    .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            jScrollPanePopulation.setViewportView(getJTablePopulation());
        }
        return jScrollPanePopulation;
    }

    /**
     * Initializes the {@code jTabbedPane}.
     *
     * @return the {@code jTabbedPane}
     */
    private JTabbedPane getJTabbedPane() {
        if (jTabbedPane == null) {
            jTabbedPane = new JTabbedPane();
            jTabbedPane.addTab("Population", null, getJScrollPanePopulation(),
                    null);
            jTabbedPane.addTab("Match Set", null, getJScrollPaneMatchSet(),
                    null);
            jTabbedPane.addTab("Action Set", null, getJScrollPaneActionSet(),
                    null);
        }
        return jTabbedPane;
    }

    /**
     * Initializes the {@code jTableActionSet}.
     *
     * @return the {@code jTableActionSet}
     */
    private JTable getJTableActionSet() {
        if (jTableActionSet == null) {
            jTableActionSet = new JTable() {
                // Cells are not editable...
                public boolean isCellEditable(final int row, final int column) {
                    return false;
                }
            };

            jTableActionSet.setName("ActionSet");
            // if autoResize true, disable AutoResizeMode
            if (LogFileAnalyzer.getInstance().getAutoResize()) {
                jTableActionSet.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                // create "empty" table
            }

            DefaultTableModel dtm = new DefaultTableModel(1, 1);
            dtm.setValueAt("Import Log-File", 0, 0);
            String[] name = {"Information"};
            dtm.setColumnIdentifiers(name);
            jTableActionSet.setModel(dtm);

            // if autoResize true, use TableResize-method
            if (LogFileAnalyzer.getInstance().getAutoResize()) {
                resizeTable(jTableActionSet);
            }
            // add RowSort
            addTabellenRowSort(jTableActionSet);

            // Add selection listener
            jTableActionSet
                    .setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jTableActionSet.setRowSelectionAllowed(true);
            jTableActionSet.setColumnSelectionAllowed(false);
            jTableActionSet.addMouseListener(new TableClickMouseListener(
                    jTableActionSet));
        }
        return jTableActionSet;
    }

    /**
     * Initializes the {@code jTableMatchSet}.
     *
     * @return the {@code jTableMatchSet}
     */
    private JTable getJTableMatchSet() {
        if (jTableMatchSet == null) {
            jTableMatchSet = new JTable() {
                // Cells are not editable...
                public boolean isCellEditable(final int row, final int column) {
                    return false;
                }
            };
            jTableMatchSet.setName("MatchSet");

            // if autoResize true, disable AutoResizeMode
            if (LogFileAnalyzer.getInstance().getAutoResize()) {
                jTableMatchSet.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            }

            // create "empty" table
            DefaultTableModel dtm = new DefaultTableModel(1, 1);
            dtm.setValueAt("Import Log-File", 0, 0);
            String[] name = {"Information"};
            dtm.setColumnIdentifiers(name);
            jTableMatchSet.setModel(dtm);

            // if autoResize true, use TableResize-method
            if (LogFileAnalyzer.getInstance().getAutoResize()) {
                resizeTable(jTableMatchSet);
            }

            // add RowSort
            addTabellenRowSort(jTableMatchSet);

            // Add mouse listener
            jTableMatchSet
                    .setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jTableMatchSet.setRowSelectionAllowed(true);
            jTableMatchSet.setColumnSelectionAllowed(false);
            jTableMatchSet.addMouseListener(new TableClickMouseListener(
                    jTableMatchSet));
        }
        return jTableMatchSet;
    }

    /**
     * Initializes the {@code jTablePopulation}.
     *
     * @return the {@code jTablePopulation}
     */
    private JTable getJTablePopulation() {
        if (jTablePopulation == null) {
            jTablePopulation = new JTable() {
                // Cells are not editable...
                public boolean isCellEditable(final int row, final int column) {
                    return false;
                }
            };
            jTablePopulation.setName("Population");

            // if autoResize true, disable AutoResizeMode
            if (LogFileAnalyzer.getInstance().getAutoResize()) {
                jTablePopulation.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            }

            // create "empty" table
            DefaultTableModel dtm = new DefaultTableModel(1, 1);
            dtm.setValueAt("Import Log-File", 0, 0);
            String[] name = {"Information"};
            dtm.setColumnIdentifiers(name);
            jTablePopulation.setModel(dtm);

            // if autoResize true, use TableResize-method
            if (LogFileAnalyzer.getInstance().getAutoResize()) {
                resizeTable(jTablePopulation);
            }

            // add RowSort
            addTabellenRowSort(jTablePopulation);

            // Add mouse listener
            jTablePopulation
                    .setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jTablePopulation.setRowSelectionAllowed(true);
            jTablePopulation.setColumnSelectionAllowed(false);
            jTablePopulation.addMouseListener(new TableClickMouseListener(
                    jTablePopulation));
        }
        return jTablePopulation;
    }

    /**
     * Initializes {@code jTextField_input}.
     *
     * @return javax.swing.JTextField
     */
    private JTextField getJTextField_input() {
        if (jTextFieldInput == null) {
            jTextFieldInput = new JTextField();
            jTextFieldInput.setEditable(false);
        }
        return jTextFieldInput;
    }

    /**
     * Initializes the {@code jTextFieldIteration} and adds an
     * {@code actionListener} to search for a specific iteration.
     *
     * @return the {@code jTextFieldIteration}
     */
    private JTextField getJTextFieldIteration() {
        if (jTextFieldIteration == null) {
            jTextFieldIteration = new JTextField();
            jTextFieldIteration.setHorizontalAlignment(JTextField.LEADING);
            jTextFieldIteration.setText("No valid value");
            jTextFieldIteration.setPreferredSize(new Dimension(4, 20));
            jTextFieldIteration.setEnabled(false);
            jTextFieldIteration
                    .addActionListener(e -> {
                        String searchString = jTextFieldIteration.getText();
                        try {
                            double searchValue = Double
                                    .parseDouble(searchString);
                            currentElement = dataMemory
                                    .searchElement(searchValue);
                            refresh();
                        } catch (NumberFormatException nfe) {
                            // Conversion to double fails.
                            jTextFieldIteration.setText("No valid value");
                        }
                    });
        }
        return jTextFieldIteration;
    }

    /**
     * Initializes the GUI and all contents.
     */
    private void initialize() {
        this.setSize(640, 380);
        this.setLocation(10, 10);
        this.setJMenuBar(getJJMenuBar());
        this.setContentPane(getJContentPane());
        this.setTitle("LogFileAnalyzer " + LogFileAnalyzer.getVERSION());
    }

    /**
     * Refreshes the tables and all open {@code ChartFrame}s. The method
     * removes columns deselected in the View->Columns-Menu, resizes the column
     * width and sets {@code RowSorter}s for the tables. If the
     * currentElement is {@code null}, the controls are disabled and a
     * notification is shown.
     */
    private void refresh() {
        refreshCharts();
        if (this.currentElement != null) {
            // enable controls
            jTextFieldIteration.setEnabled(true);
            jButtonNext.setEnabled(true);
            jButtonPrevious.setEnabled(true);
            jButtonFirst.setEnabled(true);
            jButtonLast.setEnabled(true);

			/*
			 * Delete contents of the tables and show notification. This is
			 * necessary to ensure, that the new data is shown correctly.
			 */
            DefaultTableModel dtm = new DefaultTableModel(1, 1);
            dtm.setValueAt("Daten werden aktualisiert", 0, 0);
            String[] header = {"Information"};
            dtm.setColumnIdentifiers(header);
            jTablePopulation.setModel(dtm);
            jTableMatchSet.setModel(dtm);
            jTableActionSet.setModel(dtm);

            // Show data of the current element.
            jTextFieldIteration.setText(String.valueOf(currentElement.getIteration()));
            jTextFieldInput.setText(currentElement.getInput());
            jTablePopulation.setModel(currentElement.getPopulation());
            jTableMatchSet.setModel(currentElement.getMatchSet());
            jTableActionSet.setModel(currentElement.getActionSet());

            // Remove deselected columns
            for (JCheckBoxMenuItem viewItem : viewItems) {
                if (!viewItem.getState()) {
                    try {
                        jTablePopulation.removeColumn(jTablePopulation
                                .getColumn(viewItem.getName()));
                        jTableMatchSet.removeColumn(jTableMatchSet
                                .getColumn(viewItem.getName()));
                        jTableActionSet.removeColumn(jTableActionSet
                                .getColumn(viewItem.getName()));
                    } catch (IllegalArgumentException e) {
                        e.getStackTrace();
                    }

                }
            }

            // If autoResize is true, use resizeTable-method.
            if (LogFileAnalyzer.getInstance().getAutoResize()) {
                resizeTable(jTablePopulation);
                resizeTable(jTableMatchSet);
                resizeTable(jTableActionSet);
            }

            // add RowSorter
            addTabellenRowSort(jTablePopulation);
            addTabellenRowSort(jTableMatchSet);
            addTabellenRowSort(jTableActionSet);

            // Update title
            jTablePopulation.setName("Population @"
                    + currentElement.getIteration());
            jTableMatchSet
                    .setName("MatchSet @" + currentElement.getIteration());
            jTableActionSet.setName("ActionSet @"
                    + currentElement.getIteration());
        } else {
            // disable controls
            jTextFieldIteration.setText("File contains no information");
            jTextFieldIteration.setEnabled(false);
            jButtonNext.setEnabled(false);
            jButtonPrevious.setEnabled(false);
            jButtonFirst.setEnabled(false);
            jButtonLast.setEnabled(false);

            DefaultTableModel dtm = new DefaultTableModel(1, 1);
            dtm.setValueAt("No Data", 0, 0);
            String[] name = {"Information"};
            dtm.setColumnIdentifiers(name);

            jTablePopulation.setModel(dtm);
            jTableMatchSet.setModel(dtm);
            jTableActionSet.setModel(dtm);

            // If autoResize is true, use resizeTable-method
            if (LogFileAnalyzer.getInstance().getAutoResize()) {
                resizeTable(jTablePopulation);
                resizeTable(jTableMatchSet);
                resizeTable(jTableActionSet);
            }

            // Update title
            jTablePopulation.setName("Population");
            jTableMatchSet.setName("MatchSet");
            jTableActionSet.setName("ActionSet");
        }

    }

    /**
     * Refreshes all open {@code ChartFrame}s.
     */
    private void refreshCharts() {
        for (ChartFrame chartFrame : this.chartFrames) {
            chartFrame.loadNewDataSet();
        }
    }

    /**
     * Deletes a {@code ChartFrame} out of this class' vector of
     * {@code ChartFrame}s.
     *
     * @param deleteChartFrame {@code ChartFrame} to delete
     */
    final void removeChartFrame(final ChartFrame deleteChartFrame) {
        chartFrames.remove(deleteChartFrame);
    }

    /**
     * Resizes the column widths of a table to fit the size of the contents and
     * the header.
     *
     * @param table table used for resize
     */
    private void resizeTable(final JTable table) {
        // resize every column i
        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumnModel columnModel = table.getColumnModel();
            TableColumn column = columnModel.getColumn(i);

            // Use headerRenderer to calculate header-size
            TableCellRenderer headerRenderer = table.getTableHeader()
                    .getDefaultRenderer();
            int headerWidth = headerRenderer.getTableCellRendererComponent(
                    null, column.getHeaderValue(), false, false, 0, 0)
                    .getPreferredSize().width;

            // for every row j
            int cellWidth = 0;
            for (int j = 0; j < table.getModel().getRowCount(); j++) {
                // convert Table-column to Model-column
                int modelColumn = table.convertColumnIndexToModel(i);

                Object inhalt = table.getModel().getValueAt(j, modelColumn);
                int width = table
                        .getDefaultRenderer(table.getColumnClass(i))
                        .getTableCellRendererComponent(table, inhalt, false,
                                false, 0, i).getPreferredSize().width;
                cellWidth = Math.max(cellWidth, width);
            }
            // set column-with to maximum of header- and content-width
            column.setPreferredWidth(Math.max(headerWidth, cellWidth) + 3);
        }
    }
} // @jve:decl-index=0:visual-constraint="23,35"
