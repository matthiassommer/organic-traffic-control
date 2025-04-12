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

/**
 * Contains the minimum, maximum, and the currently selected value of a table
 * column. This data can be displayed in a {@code ComparisonFrame}.
 *
 * @author Holger Prothmann
 */
public class ComparisonDataSet {
    /**
     * The name of the table column.
     */
    private final String columnName;

    /**
     * The maximum value contained in the column.
     */
    private final double max;

    /**
     * The minimum value contained in the column.
     */
    private final double min;

    /**
     * The value of this currently selected classifier.
     */
    private final double selected;

    public ComparisonDataSet(final String _columnName, final double _min, final double _max, final double _selected) {
        columnName = _columnName;
        min = _min;
        max = _max;
        selected = _selected;
    }

    /**
     * Returns the name of the table column.
     *
     * @return the name of the table column
     */
    public final Comparable getColumnName() {
        return columnName;
    }

    /**
     * Returns the maximum value contained in the column.
     *
     * @return the maximum value contained in the column
     */
    public final double getMax() {
        return max;
    }

    /**
     * Returns the minimum value contained in the column.
     *
     * @return the minimum value contained in the column
     */
    public final double getMin() {
        return min;
    }

    /**
     * Returns the value of this currently selected classifier.
     *
     * @return the value of this currently selected classifier
     */
    public final double getSelected() {
        return selected;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public final String toString() {
        return columnName + ": min " + min + ", max " + max + ", selected " + selected;
    }
}
