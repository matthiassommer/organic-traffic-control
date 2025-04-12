/*
 * Created on 14.02.2008
 * 
 * $Id: CondVolumeHistogram.java,v 1.2 2008/02/15 16:51:32 hpr Exp $
 * 
 * $Author: hpr $
 * 
 * $Log: CondVolumeHistogram.java,v $
 * Revision 1.2  2008/02/15 16:51:32  hpr
 * - Einige Exceptions vermeiden.
 * - CycleTimeHistogram kommt mit neuem FTCFormat klar.
 *
 * Revision 1.1  2008/02/14 18:01:05  hpr
 * LogFileAnalyzer auf aktuellen Stand gebracht.
 *
 *
 */

package de.dfg.oc.otc.logfileanalyzer.histogram;

import javax.swing.table.DefaultTableModel;
import java.util.Collection;
import java.util.Vector;

public class CondVolumeHistogram extends AbstractHistogram {
    public CondVolumeHistogram() {
        super("Condition volume");
    }

    public final Vector<Double> calculateHistogramData(final DefaultTableModel selectedTable) {
        int column = selectedTable.findColumn("Condition");
        Vector<Double> conditionVolumes = new Vector<>();

        if (column == -1) {
            System.err.println("No such column!");
        } else {
            Collection<String> conditionVolumeStrings = new Vector<>();
            for (int row = 0; row < selectedTable.getRowCount(); row++) {
                String text = (String) selectedTable.getValueAt(row, column);
                conditionVolumeStrings.add(text);
            }
            conditionVolumes = convertVector(conditionVolumeStrings);
        }
        return conditionVolumes;
    }

    private Vector<Double> convertVector(final Iterable<String> conditions) {
        Vector<Double> doubleValues = new Vector<>();

        for (String strIntervals : conditions) {
            double result = 1;

            if (strIntervals != null) {
                while (strIntervals.contains("]")) {
                    int left = strIntervals.indexOf("[");
                    int right = strIntervals.indexOf("]");

                    String strInterval = strIntervals.substring(left + 1, right);
                    strIntervals = strIntervals.substring(right + 1);

                    String[] intervalLimits = strInterval.split(",");
                    double d1 = Double.parseDouble(intervalLimits[0]);
                    double d2 = Double.parseDouble(intervalLimits[1]);

                    result *= (d2 - d1) / 100;
                }
                doubleValues.add(result);
            }
        }

        return doubleValues;
    }
}
