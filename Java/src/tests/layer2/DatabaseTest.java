package tests.layer2;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer2.ea.Database;
import org.junit.Test;

import java.io.*;

/**
 * Simple class for testing database operations performed by the
 * {@code de.dfg.oc.otc.layer2.ea.Database}-class.
 *
 * @author hpr
 * @see de.dfg.oc.otc.layer2.ea.Database
 */
public class DatabaseTest {
    @Test
    public void run() {
        String filePath = "OTC\\Aimsun_Modell";

        // Erster Knoten
        // int[] turns =
        // {100,102,100,97,100,99,98,102,96,102,96,101,94,101,94,99,94,97};

        // K7
        // int[] turns = { 155, 115, 99, 159, 99, 620, 206, 207, 272, 621, 211,
        // 212 };

        // K3
        int[] turns = {130, 127, 130, 529, 130, 246, 122, 123, 122, 534, 534, 129, 126, 128, 125, 127, 125, 529, 120,
                121, 120, 123, 120, 534, 529, 121};

        int[] warmUps = {0, 900, 1800};
        int[] rids = {240};
        // Statistikintervall in Sekunden
        int statInterval = 300;

        for (int rid : rids) {
            for (int warmup : warmUps) {
                // Startzeitpunkt des Auswertungsintervalls in Sekunden
                double stopTime = 24400;

                // Datenbank holen
                FilenameFilter filter = (dir, name) -> name.endsWith(".mdb") && name.startsWith("Layer");

                String[] filenames = new File(filePath).list(filter);

                try {
                    for (String filename : filenames) {
                        // Open db
                        Database db = new Database(filePath + filename, "", DefaultParams.DB_TABLE_NAME, "", "");

                        // Open file
                        // / FileWriter fstream = new FileWriter(
                        // "K3_morning-peak_00-00-00-00_warmup-" + warmup
                        // + "_rid-" + rid + ".csv");

                        filename = filename.substring(0, filename.length() - 4);
                        FileWriter fstream = new FileWriter(filePath + filename + "_WARMUP-" + warmup + ".csv");
                        BufferedWriter out = new BufferedWriter(fstream);

                        double result = 0;
                        int i = 0;
                        double tmpStart = warmup;
                        do {
                            i++;
                            double tmpStop = tmpStart + statInterval;

                            result += db.calculateLoS(rid, turns, tmpStart, tmpStop);

                            // Logging
                            String str = tmpStop + "; " + result / i;
                            System.out.println(str);
                            out.write(str + System.getProperty("line.separator"));

                            tmpStart += statInterval;
                        } while (tmpStart <= stopTime - statInterval);
                        out.close();
                    }
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }
}
