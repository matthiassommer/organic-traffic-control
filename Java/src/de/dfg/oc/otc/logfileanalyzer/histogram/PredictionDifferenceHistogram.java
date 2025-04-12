/*
 * Created on 14.02.2008
 * 
 * $Id: PredictionDifferenceHistogram.java,v 1.2 2008/02/15 16:51:32 hpr Exp $
 * 
 * $Author: hpr $
 * 
 * $Log: PredictionDifferenceHistogram.java,v $
 * Revision 1.2  2008/02/15 16:51:32  hpr
 * - Einige Exceptions vermeiden.
 * - CycleTimeHistogram kommt mit neuem FTCFormat klar.
 *
 * Revision 1.1  2008/02/14 18:01:04  hpr
 * LogFileAnalyzer auf aktuellen Stand gebracht.
 *
 *
 */

package de.dfg.oc.otc.logfileanalyzer.histogram;

import javax.swing.table.DefaultTableModel;
import java.util.Vector;

public class PredictionDifferenceHistogram extends AbstractHistogram {

    public PredictionDifferenceHistogram() {
        super("Prediction difference");
    }

    public final Vector<Double> calculateHistogramData(final DefaultTableModel selectedTable) {
        // Determine prediction columns
        int columnInitialPrediction = selectedTable.findColumn("Initial Pred.");
        int columnCurrentPrediction = selectedTable.findColumn("Current Pred.");

        Vector<Double> differences = new Vector<>();

        if (columnInitialPrediction == -1 || columnCurrentPrediction == -1) {
            System.err.println("No such column!");
        } else {

            for (int row = 0; row < selectedTable.getRowCount(); row++) {
                try {
                    String strInitial = (String) selectedTable.getValueAt(row, columnInitialPrediction);
                    if (strInitial != null) {
                        double doubleInitial = Double.parseDouble(strInitial);
                        String strCurrent = (String) selectedTable.getValueAt(row, columnCurrentPrediction);
                        double doubleCurrent = Double.parseDouble(strCurrent);

                        double difference = doubleInitial - doubleCurrent;
                        differences.add(difference);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return differences;
    }
}
