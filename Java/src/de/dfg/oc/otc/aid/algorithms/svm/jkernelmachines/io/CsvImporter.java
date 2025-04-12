/**
 * This file is part of JkernelMachines.
 * <p>
 * JkernelMachines is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * JkernelMachines is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with JkernelMachines.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Copyright David Picard - 2012
 */
package de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.io;

import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.type.TrainingSample;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple class to import data in csv format, with one sample per line:<br/>
 * attr1, attr2, ... , class
 * <p>
 * position of the class label can be arbitrary
 *
 * @author picard
 */
public class CsvImporter {
    /**
     * Importer with full settings.
     *
     * @param filename      the file containing the data
     * @param sep           the token which separates the values
     * @param labelPosition the position of the class label
     * @return the full list of TrainingSample
     * @throws IOException
     */
    public static List<TrainingSample<double[]>> importFromFile(String filename, String sep, int labelPosition, int[] positions) throws IOException {
        // the samples list
        List<TrainingSample<double[]>> list = new ArrayList<>();

        LineNumberReader line = new LineNumberReader(new FileReader(filename));
        String l;

        //remove headers
        line.readLine();
        line.readLine();
        line.readLine();

        // parse all lines
        while ((l = line.readLine()) != null) {
            String[] tok = l.split(sep);
            // attributes for feature vector
            double[] d = new double[tok.length - 1];
            // class label
            int y = 0;

            if (labelPosition == -1) {
                // first n-1 fields are attributes
                for (int i = 2; i < d.length; i++) {
                    d[i] = Double.parseDouble(tok[i]);
                }
                y = Integer.parseInt(tok[tok.length - 1]);
            } else if (labelPosition < d.length) {
                for (int i = 0; i < labelPosition; i++) {
                    d[i] = Double.parseDouble(tok[i]);
                }
                for (int i = labelPosition + 1; i < d.length; i++) {
                    d[i - 1] = Double.parseDouble(tok[i]);
                }
                y = Integer.parseInt(tok[labelPosition]);
            }

            double[] attributes = new double[positions.length];
            for (int i = 0; i < positions.length; i++) {
                attributes[i] = d[positions[i] + 1];
            }

            TrainingSample<double[]> t = new TrainingSample<>(attributes, y);
            list.add(t);
        }

        line.close();
        return list;

    }

    public static List<TrainingSample<double[]>> importFromFile(String filename, int[] positions) throws IOException {
        return importFromFile(filename, ",", -1, positions);
    }
}
