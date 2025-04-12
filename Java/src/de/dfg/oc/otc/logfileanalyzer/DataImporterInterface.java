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

import java.io.BufferedReader;

/**
 * Classes that read LCS log-files need to implement this interface by providing
 * the {@code getNextDataElement()}-method specified here. The method
 * should read a log-file and return the classifier sets for one iteration.
 *
 * @author Clemens Gersbacher, Holger Prothmann
 */
@FunctionalInterface
public interface DataImporterInterface {

    /**
     * Returns a {@code DataElement}-object containing the classifier sets
     * of the next unprocessed iteration. Use the {@code BufferedReader}
     * given as parameter to read the classifier sets are read from the
     * log-file. If the last iteration was reached, the method returns
     * {@code null}.
     * <p>
     * IMPORTANT: Create exactly one {@code DataElement} per iteration.
     * DataElements should be created in an ascending order with respect to
     * their iteration number.
     *
     * @param _logFileReader {@code BufferedReader} for reading the log-file
     * @return the next unprocessed {@code DataElement} containing the
     * iteration number and all classifier sets of the respective
     * iteration or {@code null} if all iterations have been
     * processed
     */
    DataElement getNextDataElement(BufferedReader _logFileReader);
}