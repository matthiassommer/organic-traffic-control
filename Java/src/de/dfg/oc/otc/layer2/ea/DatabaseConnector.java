package de.dfg.oc.otc.layer2.ea;

import de.dfg.oc.otc.config.DefaultParams;

/**
 * @author Matthias Sommer.
 */
class DatabaseConnector {
    /**
     * Path to the database.
     */
    private String databasePath;
    private final int eaId;

    public Database getDatabase() {
        return database;
    }

    private Database database;

    DatabaseConnector(int eaId) {
        this.eaId = eaId;

        setDBPath();
        connectToDatabase();
    }

    /**
     * Determines the locations of the database file.
     */
    private void setDBPath() {
        // Get location of db-file
        databasePath = DefaultParams.PATH;
        databasePath = databasePath.substring(0, databasePath.lastIndexOf("java"));
        databasePath = databasePath.concat("Layer2_" + eaId + ".mdb");
        databasePath = databasePath.replace("/", "\\");
    }

    private void connectToDatabase() {
        database = new Database(databasePath, "DB_L2_" + eaId, DefaultParams.DB_TABLE_NAME, DefaultParams.DB_USER, DefaultParams.DB_PASSWORD);
    }
}
