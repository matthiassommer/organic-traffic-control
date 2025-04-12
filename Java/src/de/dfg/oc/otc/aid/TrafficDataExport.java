package de.dfg.oc.otc.aid;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.tools.FileUtilities;
import tests.evaluation.aid.AIDTrafficDataReader;
import java.nio.file.*;
import java.io.*;
import java.nio.file.Files;

/**
 * Created by Dietmar on 07.03.2017.
 * Is to be called from DetectorPair.update()
 * Exports raw traffic data to CSV
 */
public class TrafficDataExport {

    private static String pathTrafficData = "..\\Studentische Arbeiten\\PM Sach\\Evaluation\\1 Aimsun Traffic Data\\";
//    private static String pathTrafficData = "..\\Studentische Arbeiten\\PM Sach\\Evaluation\\3 Auswertung\\" + DefaultParams.AID_DEFAULT_ALGORITHM + "\\Aimsun Traffic Data\\";
    private static String detector1 = "977978";
    private static String detector2 = "979980";
    private static String csvDelimiter = ";";

    private static boolean enabled = true;


    public static void exportData(String detectorID, DetectorDataValue aggregatedValue)
    {
        if (!enabled)
            return;

        if (!detectorID.equals(detector1) && !detectorID.equals(detector2))
            return;

        float time = aggregatedValue.getTime();

        // Add the detector values from the detector pair
        // Maintain the existing I35E file format
        float[] values = aggregatedValue.getValues();
        String csv = time + csvDelimiter;
        csv += values[DetectorCapabilities.DENSITY] + csvDelimiter;
        csv += values[DetectorCapabilities.HEADWAY] + csvDelimiter;
        csv += values[DetectorCapabilities.OCCUPANCY] + csvDelimiter;
        csv += values[DetectorCapabilities.SPEED] + csvDelimiter;
        csv += values[DetectorCapabilities.COUNT];

        long replication = OTCManager.getInstance().getSystemSeed();

        String fn = pathTrafficData + "\\" + detector1 + detector2 + "\\" + detectorID + "_" + replication + "_data.csv";
//        String fn = pathTrafficData + "\\" + detector1 + detector2 + "\\" + detectorID + "_" + replication + "_data.csv";

        try {
            if (!Files.exists(Paths.get(fn)))
                FileUtilities.createNewFile(fn);
            PrintStream ps = new PrintStream(new FileOutputStream(fn, true), true);
            ps.println(csv);
            ps.close();
        }
        catch (FileNotFoundException e)
        {
            System.out.println(e.toString());
        }
    }
}
