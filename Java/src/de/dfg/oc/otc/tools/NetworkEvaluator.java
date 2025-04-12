package de.dfg.oc.otc.tools;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Copies AIMSUNs network statistics from a .mdb-file into text files.
 *
 * @author hpr
 *
 */
class NetworkEvaluator {
    public static void main(final String[] args) {
        final NetworkEvaluator ne = new NetworkEvaluator();
        ne.copyNetworkDatatoCSV();
        ne.copyStreamDatatoCSV();
        ne.copyPollutionDatatoCSV();
        ne.copyResultFiles();
    }

    /** The database connection. */
    private Connection connection;
    /** Database filename. */
    private final String databaseFilename;
    /** Simulation start time. */
    private int startTime = -1;
    /** Simulation stop time. */
    private int stopTime = -1;

     private NetworkEvaluator() {
        this.databaseFilename = "Layer1.mdb";
    }

    private void closeDBConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Copies the network-wide travel times and number of stops for all
     * replications from the database to a text file.
     */
    private void copyNetworkDatatoCSV() {
        // Get replications stored in DB
        final List<Integer> rids = getReplicationsInDB();

        openDB();

        getStartTime();
        getStopTime();

        for (Integer id : rids) {
            try {
                Statement stmt = connection.createStatement();
                // + "AND tfrom <> 0");
                ResultSet rs = stmt.executeQuery("SELECT * FROM SysSta WHERE rid=" + id);

                // Travel time and stops ([s/km] and [#/veh/km], respectively)
                FileOutputStream fout = new FileOutputStream(id + "_Performance_NET.csv", false);
                PrintStream ps = new PrintStream(fout);

                // Fuel [l/km]
                FileOutputStream fout3 = new FileOutputStream(id + "_Fuel-km_NET.csv", false);
                PrintStream ps3 = new PrintStream(fout3);

                // Travel time in [s] (not [s/km]), stops in [#/veh] (not
                // [#/veh/km])
                FileOutputStream fout4 = new FileOutputStream(id + "_Performance_NET_ABS.csv", false);
                PrintStream ps4 = new PrintStream(fout4);

                while (rs.next()) {
                    int tfrom = rs.getInt("tfrom");
                    int tto = rs.getInt("tto");
                    // s/km
                    double ttime = rs.getDouble("ttime1");
                    // #/veh/km
                    double stops = rs.getDouble("nstops");
                    // l
                    double fuelc = rs.getDouble("fuelc");
                    // km
                    double km = rs.getDouble("travel");
                    // s
                    double totalTTime = rs.getDouble("traveltime");
                    // veh/h
                    double flow = rs.getDouble("flow");

                    if (!(tfrom == startTime && tto == stopTime)) {
                        // Durchschnitt aus DB nicht übernehmen
                        String rsLine = tto + "; " + ttime + "; " + stops + ";";
                        ps.println(rsLine);
                        System.out.println(rsLine);

                        rsLine = tto + "; " + fuelc / km * 100;
                        ps3.println(rsLine);

                        double noOfVeh = flow * (tto - tfrom) / 3600;
                        rsLine = tto + "; " + totalTTime / noOfVeh + "; " + stops * km / noOfVeh;
                        // + "; " + km/noOfVeh;
                        ps4.println(rsLine);
                    }
                }

                stmt.close();
                fout.close();
                fout3.close();
                fout4.close();
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
        closeDBConnection();
    }

    /**
     * Copies the network-wide emissions from the database to a text file.
     */
    private void copyPollutionDatatoCSV() {
        // Get replications stored in DB
        final List<Integer> rids = getReplicationsInDB();

        openDB();

        getStartTime();
        getStopTime();

        for (Integer id : rids) {
            try {
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM SysPoll WHERE rid=" + id
                        + " ORDER BY tfrom, tto, npollutant");

                FileOutputStream fout = new FileOutputStream(id + "_Pollution_NET.csv", false);
                PrintStream ps = new PrintStream(fout);

                FileOutputStream fout2 = new FileOutputStream(id + "_Pollution-km_NET.csv", false);
                PrintStream ps2 = new PrintStream(fout2);

                double[] vpollutant = new double[3];

                while (rs.next()) {
                    int tfrom = rs.getInt("tfrom");
                    int tto = rs.getInt("tto");
                    // kg
                    vpollutant[rs.getRow() % 3] = rs.getDouble("vpollutant");

                    if (!(tfrom == startTime && tto == stopTime) && rs.getRow() % 3 == 0) {
                        String rsLine = tto + "; " + vpollutant[1] + "; " + vpollutant[2] + "; " + vpollutant[0];
                        ps.println(rsLine);
                        System.out.println(rsLine);

                        // Gefahrene Kilometer bestimmen
                        Statement stmt2 = connection.createStatement();
                        ResultSet rs2 = stmt2.executeQuery("SELECT * FROM SysSta WHERE rid=" + id + " AND tfrom="
                                + tfrom);

                        rs2.next();
                        final double km = rs2.getDouble("travel");

                        // g/km
                        String rsLine2 = tto + "; " + vpollutant[1] / km * 1000 + "; " + vpollutant[2] / km * 1000
                                + "; " + vpollutant[0] / km * 1000;
                        ps2.println(rsLine2);

                        stmt2.close();
                    }
                }

                stmt.close();
                fout.close();
                fout2.close();
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
        closeDBConnection();
    }

    private void copyResultFiles() {
        final File dir = new File(System.getProperty("user.dir"));
        final List<Integer> rids = getReplicationsInDB();

        openDB();

        for (Integer id : rids) {
            // Save log-files
            FilenameFilter filter = (dir1, name) -> name.contains("_Performance_") || name.contains("_Fuel") || name.contains("_Stream")
                    || name.contains("_Pollution");
            String[] filenames = dir.list(filter);

            String subDir = "Rep" + id;
            new File(subDir).mkdir();

            try {
                for (String filename : filenames) {
                    if (filename.contains(id + "_")) {
                        FileUtilities.copyFile(new File(filename), new File(subDir + "\\" + filename), true);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        closeDBConnection();
    }

    /**
     * Copies the travel times and number of stops for all streams from the
     * database to a text file.
     */
    private void copyStreamDatatoCSV() {
        // Get replications stored in DB
        final List<Integer> rids = getReplicationsInDB();

        openDB();

        getStartTime();
        getStopTime();

        for (Integer id : rids) {
            List<Integer> strms = getStreamIdsForRep();

            for (Integer streamID : strms) {
                try {
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM StrmSta WHERE rid=" + id + " AND id=" + streamID
                            + " ORDER BY tto");

                    FileOutputStream fout = new FileOutputStream(id + "_Performance_Stream" + streamID + ".csv", false);
                    PrintStream ps = new PrintStream(fout);

                    while (rs.next()) {
                        int tfrom = rs.getInt("tfrom");
                        int tto = rs.getInt("tto");
                        double ttime = rs.getDouble("ttime1");
                        double stops = rs.getDouble("nstops");

                        if (!(tfrom == startTime && tto == stopTime)) {
                            // Durchschnitt aus DB nicht übernehmen
                            String rsLine = tto + "; " + ttime + "; " + stops;
                            ps.println(rsLine);
                            System.out.println(rsLine);
                        }
                    }

                    stmt.close();
                    fout.close();
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
        closeDBConnection();
    }

    /**
     * Determines the replication ids stored in an AIMSUN database.
     *
     * @return the replication ids stored in an AIMSUN database
     */
    private List<Integer> getReplicationsInDB() {
        openDB();

        final List<Integer> rids = new ArrayList<>();
        try {
            final Statement stmt = connection.createStatement();
            final ResultSet rs = stmt.executeQuery("SELECT DISTINCT rid FROM SysSta");
            while (rs.next()) {
                final int rid = rs.getInt("rid");
                rids.add(rid);
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDBConnection();
        }

        return rids;
    }

    /**
     * Sets the stop time of the simulation run.
     *
     * @see #stopTime
     */
    private void getStartTime() {
        try {
            final Statement stmt1 = connection.createStatement();
            final ResultSet rs1 = stmt1.executeQuery("SELECT MIN(tfrom) FROM SysSta");
            while (rs1.next()) {
                startTime = rs1.getInt(1);
                System.out.println("START " + startTime);
            }
            rs1.close();
            stmt1.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the stop time of the simulation run.
     *
     * @see #stopTime
     */
    private void getStopTime() {
        try {
            final Statement stmt1 = connection.createStatement();
            final ResultSet rs1 = stmt1.executeQuery("SELECT MAX(tto) FROM SysSta");
            while (rs1.next()) {
                stopTime = rs1.getInt(1);
                System.out.println("STOP " + stopTime);
            }
            rs1.close();
            stmt1.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Determines the available streams for a given replication ID.
     *
     * @return the available streams
     */
    private List<Integer> getStreamIdsForRep() {
        final List<Integer> strms = new ArrayList<>();

        try {
            final Statement stmt = connection.createStatement();
            final ResultSet rs = stmt.executeQuery("SELECT DISTINCT id FROM StrmSta");
            while (rs.next()) {
                final int strm = rs.getInt("id");
                strms.add(strm);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return strms;
    }

    /**
     * Opens a database connection.
     */
    private void openDB() {
        final String database = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=" + databaseFilename
                + ";DriverID=22;READONLY=false}";

        try {
            connection = DriverManager.getConnection(database);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
