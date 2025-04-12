/*
 * Created on 14.02.2008
 * 
 * $Id: CycleTimeHistogram.java,v 1.2 2008/02/15 16:51:32 hpr Exp $
 * 
 * $Author: hpr $
 * 
 * $Log: CycleTimeHistogram.java,v $
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
import java.util.Vector;

public class CycleTimeHistogram extends AbstractHistogram {
    public CycleTimeHistogram() {
        super("Cycle time (FTC)");
    }

    private double calculateCycleTime(String action) {
        if (action == null) {
            return -1;
        }

        // Determine first occurrence of 'true' or 'false'
        int cutoffIndex;
        int cutoffIndex1 = action.indexOf(", false");
        int cutoffIndex2 = action.indexOf(", true");

        if (cutoffIndex1 != -1 && cutoffIndex2 != -1) {
            cutoffIndex = Math.min(cutoffIndex1, cutoffIndex2);
        } else {
            cutoffIndex = Math.max(cutoffIndex1, cutoffIndex2);
            if (cutoffIndex == -1) {
                cutoffIndex = action.length();
            }
        }

        action = action.substring(0, cutoffIndex);

        String[] durations = action.split(",");
        int start = durations.length / 2 + 1;
        double result = 0;
        for (int i = start; i < durations.length; i++) {
            result += Double.parseDouble(durations[i].trim());
        }
        return result;
    }

    public final Vector<Double> calculateHistogramData(final DefaultTableModel selectedTable) {
        int column = selectedTable.findColumn("Action");

        Vector<Double> cycleTimes = new Vector<>();

        if (column == -1) {
            System.err.println("No such column");
        } else {
            for (int row = 0; row < selectedTable.getRowCount(); row++) {
                try {
                    String action = (String) selectedTable.getValueAt(row, column);
                    cycleTimes.add(calculateCycleTime(action));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return cycleTimes;
    }
}
