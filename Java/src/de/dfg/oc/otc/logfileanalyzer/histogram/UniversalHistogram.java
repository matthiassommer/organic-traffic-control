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

import javax.swing.table.DefaultTableModel;
import java.util.Vector;

/**
 * Creates a histogram for any table column containing numbers. Simply pass the
 * column name to the constructor.
 *
 * @author Clemens Gersbacher, Holger Prothmann
 */
public class UniversalHistogram extends AbstractHistogram {
    /**
     * Constructor. Requires the name of the column that should be displayed.
     *
     * @param columnName name of the column that is displayed in this histogram
     */
    public UniversalHistogram(final String columnName) {
        super(columnName);
    }

    /*
     * Comment available in superclass.
     */
    @Override
    public final Vector<Double> calculateHistogramData(final DefaultTableModel selectedTable) {
        Vector<Double> dataVector = new Vector<>();

        int column = selectedTable.findColumn(description);
        if (column == -1) {
            // Column not found
            System.err.println("No data for column '" + description + "'.");
        } else {
            // Column found
            // Read the column to a vector
            for (int row = 0; row < selectedTable.getRowCount(); row++) {
                try {
                    String strCellEntry = (String) selectedTable.getValueAt(row, column);
                    if (strCellEntry != null) {
                        double cellEntry = Double.parseDouble(strCellEntry);
                        dataVector.add(cellEntry);
                    }
                } catch (NumberFormatException nfe) {
                    nfe.getStackTrace();
                    // Content of cell cannot be converted to double. Ignore.
                }
            }
        }
        return dataVector;
    }
}
