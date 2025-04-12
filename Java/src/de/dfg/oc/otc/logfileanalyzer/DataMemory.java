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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Vector;

/**
 * The {@code DataMemory} stores the {@code DataElement}s of all
 * iterations of an experiment. It relies on the
 * {@code DataImporterInterface} to read complete log-files and provides a
 * method to search for {@code DataElement}s by their iteration number.
 *
 * @author Clemens Gersbacher, Holger Prothmann
 */
public class DataMemory {
    /**
     * Contains all {@code DataElement}s of an experiment.
     */
    private final Vector<DataElement> data;

    /**
     * The log-file that will be read.
     */
    private final File logFile;

    /**
     * Creates a new {@code DataMemory} that stores all iterations of an
     * experiment.
     *
     * @param logFile The log-file that will be read.
     */
    public DataMemory(final File logFile) {
        this.data = new Vector<>();
        this.logFile = logFile;
    }

    /**
     * Returns the {@code DataElement} of the first iteration. Returns
     * {@code null} if no {@code DataElement}s are stored in this
     * memory.
     *
     * @return the {@code DataElement} of the first iteration
     */
    public final DataElement getFirstElement() {
        if (data.isEmpty()) {
            return null;
        }
        return data.firstElement();
    }

    /**
     * Returns the {@code DataElement} of the last iteration. Returns
     * {@code null} if no {@code DataElement}s are stored in this
     * memory.
     *
     * @return the {@code DataElement} of the last iteration
     */
    public final DataElement getLastElement() {
        if (data.isEmpty()) {
            return null;
        }
        return data.lastElement();
    }

    /**
     * Reads a complete log-file and stores the contained classifier sets
     * iterationwise.
     */
    public final void readData() {
        BufferedReader bR = null;
        try {
            bR = new BufferedReader(new FileReader(logFile));
        } catch (FileNotFoundException e) {
            System.err.println("Could not access log-file: " + logFile.getAbsolutePath());
        }

        // Obtain dataImporter from LogFileAnalyzer.
        DataImporterInterface dataImporter = LogFileAnalyzer.getInstance().getDataImporter();

        // Read data for next iteration.
        DataElement currentElement = dataImporter.getNextDataElement(bR);
        while (currentElement != null) {
            saveElement(currentElement);
            currentElement = dataImporter.getNextDataElement(bR);
        }
    }

    /**
     * Stores a {@code DataElement} in the memory and updates the object's
     * next- and previous-references.
     *
     * @param element {@code DataElement} that will be stored
     */
    private void saveElement(final DataElement element) {
        if (!data.isEmpty()) {
            DataElement letzter = data.lastElement();
            letzter.setNextElement(element);
            element.setPreviousElement(letzter);
            element.setNextElement(element);
        } else {
            element.setPreviousElement(element);
            element.setNextElement(element);
        }
        data.add(element);
    }

    /**
     * Searches for a {@code DataElement} by its iteration number. If the
     * iteration number is not present in this memory, the method returns the
     * {@code DataElement} whose iteration number is closest to the given
     * iteration. If no elements are stored in this memory, the method returns
     * {@code null}.
     *
     * @param iteration an iteration number
     * @return {@code DataElement} whose iteration number is closest to
     * {@code _iteration}
     */
    public final DataElement searchElement(final double iteration) {
        if (data.isEmpty()) {
            return null;
        }

        // Compare to first/last element...
        if (data.firstElement().getIteration() >= iteration) {
            return data.firstElement();
        }
        if (data.lastElement().getIteration() <= iteration) {
            return data.lastElement();
        }


        int lowerLimit = 0;
        int upperLimit = data.size() - 1;
        boolean loop = true;
        int testValue = 1;

        // Binary search
        while (loop) {
            // Get middle position
            testValue = (int) Math.ceil((double) (upperLimit + lowerLimit) / 2);

            if (data.elementAt(testValue).getIteration() >= iteration
                    && data.elementAt(testValue - 1).getIteration() <= iteration) {
                loop = false;
            } else {
                if (data.elementAt(testValue).getIteration() <= iteration) {
                    lowerLimit = testValue;
                } else {
                    upperLimit = testValue;
                }
            }
        }

        // Searching the closest element
        double dif1 = data.elementAt(testValue).getIteration() - iteration;
        double dif2 = iteration - data.elementAt(testValue - 1).getIteration();

        if (dif1 < dif2) {
            return data.elementAt(testValue);
        } else {
            return data.elementAt(testValue - 1);
        }
    }
}
