package de.dfg.oc.otc.layer2.ea;

import org.apache.log4j.Logger;

import java.sql.*;

/**
 * This class implements all methods the EA needs to read data from an
 * MS-Access-file (*.mdb) or an SQL-Server.
 */
public class Database {
    private static final Logger log = Logger.getLogger(Database.class);
    /**
     * The absolute filename of the database (ACCESS db only!).
     */
    private final String databaseFile;
    /**
     * The name of the database (SQL server only!).
     */
    private final String databaseName;
    private final String password;
    /**
     * The table of interest of the database.
     */
    private final String databaseTable;
    /**
     * Type of database (ACCESS or SQL Server). In Python und ANG-File anpassen!
     */
    private final DBType databaseType = DBType.SQL;
    private final String username;
    /**
     * The connection to database.
     */
    private Connection connection;

    /**
     * Constructs an instance of {@code Database} to connect to and work on
     * the specified database.
     *
     * @param dbFile the absolute filename of the mdb file (ACCESS db only!)
     * @param dbName the name of the database (SQL Server only!)
     * @param table  the name of the database table
     * @param dbUser the user name for the database
     * @param dbPwd  the password of the database (use an empty string if no
     *               password is needed)
     */
    public Database(final String dbFile, final String dbName, final String table, final String dbUser,
                    final String dbPwd) {
        loadDriver();

        this.databaseFile = dbFile;
        this.databaseName = dbName.trim();
        this.databaseTable = table;
        this.username = dbUser;
        this.password = dbPwd;
    }

    private void loadDriver() {
        try {
            // Microsoft SQL Server
            if (databaseType == DBType.SQL) {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            }
            // Access (Driver is loaded automatically)
        } catch (ClassNotFoundException e) {
            log.error("ClassNotFound: ", e);
        }
    }

    /**
     * Calculates the level of service (LoS) for a time interval. The start and
     * end time of the interval are given by {@code startTime} and
     * {@code stopTime}. If {@code startTime} or {@code stopTime}
     * equal {@code -1}, the whole simulated time is used as interval. The
     * relevant replication is specified by {@code rid} and the turnings
     * given by the turning id array.
     *
     * @param rid        id of the relevant replication
     * @param turningIds array of turning ids
     * @param startTime  the start time of the relevant interval (or {@code -1} if
     *                   the entire simulated interval should be considered)
     * @param stopTime   the end time of the relevant interval (or {@code -1} if
     *                   the entire simulated interval should be considered)
     * @return the calculated LoS or {@code -1} in case of an error
     */
    public final float calculateLoS(final int rid, final int[] turningIds, final double startTime,
                                     final double stopTime) {
        float result = -1;

        // Check turning ids
        if (turningIds == null) {
            log.error("TurningIds is a null object.");
            return result;
        } else if (turningIds.length % 2 != 0) {
            // Check length of turning id array (should be even)
            log.error("Length of turning id array should be even!");
            return result;
        }

        try {
            openDBConnection();

            // Calculate LoS
            float nenner = 0;
            float zaehler = 0;

            for (int i = 0; i < turningIds.length; i += 2) {
                float delay = getDBEntryForTurning(rid, turningIds[i], turningIds[i + 1], startTime, stopTime,
                        "dtime1");
                float flow = getDBEntryForTurning(rid, turningIds[i], turningIds[i + 1], startTime, stopTime,
                        "flow");

                zaehler += delay * flow;
                nenner += flow;
            }

            result = zaehler / nenner;

            log.debug("LOS " + result);
        } catch (SQLException e) {
            log.error("", e);
        } finally {
            try {
                closeDBConnection();
            } catch (SQLException e) {
                log.error("", e);
            }
        }

        return result;
    }

    /**
     * Closes the database that was opened by {@code open()}-method.
     *
     * @return boolean {@code true}if an opened connection was closed, else
     * {@code false}
     * @throws SQLException
     */
    private boolean closeDBConnection() throws SQLException {
        if (connection == null) {
            return false;
        }
        connection.close();
        connection = null;
        return true;
    }

    /**
     * Removes all entries in the {@code databaseTable} of this class.
     */
    final void deleteTableEntries() {
        try {
            // Close db connection
            openDBConnection();
            final Statement statement = connection.createStatement();

            if (databaseType == DBType.SQL) {
                // SQL server
                statement.executeUpdate("DELETE FROM " + this.databaseTable);
            } else {
                // Access
                statement.executeUpdate("DELETE * FROM " + this.databaseTable);
            }

            statement.close();
        } catch (SQLException e) {
            log.error("deleteTableEntries()", e);
        } finally {
            try {
                closeDBConnection();
            } catch (SQLException e) {
                log.error("", e);
            }
        }
    }

    /**
     * Reads a database entry for a turning from the database. The parameter
     * {@code rid} specifies the relevant replication, {@code fromID}
     * and {@code toID} specify the turning under consideration. The start
     * and end time of the relevant interval are given by {@code startTime}
     * and {@code stopTime}. If {@code startTime} or
     * {@code stopTime} equal {@code -1}, the whole simulated time is
     * used as interval. The column which will be read is specified by the
     * {@code column} string.
     *
     * @param rid       the relevant replication id
     * @param fromID    the id of the start section of a turning
     * @param toID      the id of the end section of a turning
     * @param startTime the start time of the relevant interval (or {@code -1} if
     *                  the entire simulated interval should be considered)
     * @param stopTime  the end time of the relevant interval (or {@code -1} if
     *                  the entire simulated interval should be considered)
     * @param column    the column ("flow", "dtime1", ...) to be read
     * @return the entry obtained from the db
     */
    private float getDBEntryForTurning(final int rid, final int fromID, final int toID, double startTime,
                                        double stopTime, final String column) {
        float entry = -1;

        try {
            final Statement statement = connection.createStatement();

            if (startTime == -1 || stopTime == -1) {
                // Determine start time of simulation
                ResultSet resultSet = statement.executeQuery("SELECT MIN(tfrom) FROM " + this.databaseTable + " WHERE rid =" + rid
                        + " AND fromsect =" + fromID + " AND tosect = " + toID);
                resultSet.next();
                startTime = resultSet.getDouble(1);
                resultSet.close();

                // Determine stop time of simulation
                resultSet = statement.executeQuery("SELECT MAX(tto) FROM " + this.databaseTable + " WHERE rid =" + rid
                        + " AND fromsect =" + fromID + " AND tosect = " + toID);
                resultSet.next();
                stopTime = resultSet.getDouble(1);
                resultSet.close();
            }

            // Read average entry for "startTime" to "stopTime"
            final ResultSet rs = statement.executeQuery("SELECT " + column + " FROM " + this.databaseTable + " WHERE rid ="
                    + rid + " AND fromsect =" + fromID + " AND tosect = " + toID + " AND tfrom =" + startTime
                    + " AND tto =" + stopTime);

            if (rs.next()) {
                entry = (float) rs.getDouble(1);
            }
            rs.close();
            statement.close();

        } catch (SQLException e) {
            log.error("RID " + rid + ", FROMID " + fromID + ", TOID " + toID + ", STARTTIME " + startTime
                    + ", STOPTIME " + stopTime + ", COLUMN " + column, e);
        }

        return entry;
    }

    /**
     * Opens the database. After all work is done use close method to close the
     * database.
     *
     * @return
     * @throws SQLException if there is an error in SQL expression used by DriverManager
     *                      return success
     */
    private boolean openDBConnection() throws SQLException {
        if (connection != null) {
            return false;
        }

        String database;
        if (databaseType == DBType.SQL) {
            database = "jdbc:sqlserver://localhost:1433;databaseName=" + databaseName + ";user=" + username
                    + ";password=" + password + ";";
        } else {
            database = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=" + databaseFile
                    + ";DriverID=22;READONLY=false}";
        }

        connection = DriverManager.getConnection(database);

        SQLWarning warning = null;
        try {
            warning = connection.getWarnings();
        } catch (SQLException ex) {
            ex.getErrorCode();
        }
        while (warning != null) {
            log.warn("SQL-Status: " + warning.getSQLState());
            log.warn("SQL-Nachricht: " + warning.getMessage());
            log.warn("SQL-Fehlercode: " + warning.getErrorCode());
            warning = warning.getNextWarning();
        }

        return true;
    }

    /**
     * Type of database (SQL server or Access file).
     */
    private enum DBType {
        ACCESS, SQL
    }
}
