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

import de.dfg.oc.otc.logfileanalyzer.ComparisonDataSet;
import de.dfg.oc.otc.logfileanalyzer.LogFileAnalyzer;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

/**
 * Listens to mouse events on a table.
 *
 * @author Holger Prothmann
 */
class TableClickMouseListener extends MouseAdapter {
    /**
     * The table associated with this {@code TableClickMouseListener}.
     */
    private final JTable table;

    /**
     * Creates a new {@code TableClickMouseListener}.
     *
     * @param table the table associated with this
     *              {@code TableClickMouseListener}
     */
    public TableClickMouseListener(final JTable table) {
        // It is necessary to keep the table since it is not possible
        // to determine the table from the event's source.
        this.table = table;
    }

    /**
     * Returns {@code true} iff the contents of the given table column can
     * be converted to {@code double}.
     *
     * @param columnId id of relevant table column
     * @return {@code true} iff the contents of the given column can be
     * converted to {@code double}
     */
    private boolean columnConvertible(final int columnId) {
        TableModel tm = table.getModel();

        for (int row = 0; row < tm.getRowCount(); row++) {
            try {
                new Double((String) tm.getValueAt(row, columnId));
            } catch (Exception e) {
                // Contents of column could not be converted to double, return
                // false!
                return false;
            }
        }
        return true;
    }

    /**
     * Determines the minimum and maximum value of a table column.
     *
     * @param column   the id of the table column
     * @param selected the value of the currently selected row
     * @return a {@code ComparisonDataSet} that can be displayed in a
     * {@code ComparisionFrame}
     */
    private ComparisonDataSet determineMinMax(final int column, final double selected) {
        TableModel tm = table.getModel();
        String[] columnNames = LogFileAnalyzer.getInstance().getColumnNames();

        double min = selected;
        double max = selected;

        // TODO More efficient implementation...
        for (int row = 0; row < tm.getRowCount(); row++) {
            double current = new Double((String) tm.getValueAt(row, column));
            if (current < min) {
                min = current;
            } else if (current > max) {
                max = current;
            }
        }

        String columnName = columnNames[column];

        return new ComparisonDataSet(columnName, min, max, selected);
    }

    /**
     * Reacts on a mouse event that occurred in the table. In case of a double
     * click, a new {@code ComparisonFrame} is created that compares the
     * classifier currently selected in the table with the table's other
     * classifiers.
     *
     * @param e
     */
    public final void mouseClicked(final MouseEvent e) {
        if (e.getClickCount() == 2) {
            TableModel tm = table.getModel();
            int rowIdView = table.getSelectionModel().getLeadSelectionIndex();

            // Has a row been selected?
            if (rowIdView > -1) {
                // Get id of selected row in table model
                int rowIdModel = table.convertRowIndexToModel(rowIdView);

                // Get available columns
                String[] columnNames = LogFileAnalyzer.getInstance().getColumnNames();

                Vector<ComparisonDataSet> mmsForColumns = new Vector<>();
                for (int i = 0; i < columnNames.length; i++) {
                    if (columnConvertible(i)) {
                        ComparisonDataSet mms = determineMinMax(i,
                                new Double((String) tm.getValueAt(rowIdModel, i)));
                        mmsForColumns.add(mms);
                    }
                }

                // Get string representation of selected classifier
                String selectedClassifier = "Classifier " + tm.getValueAt(rowIdModel, 0);
                String title = "- " + table.getName() + " -";

                new ComparisonFrame(selectedClassifier, title, mmsForColumns).display();
            }
        }
    }
}
