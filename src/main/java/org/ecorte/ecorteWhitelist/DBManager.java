package org.ecorte.ecorteWhitelist;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBManager {
    public EcorteWhitelist plugin;
    public Config config;

    public HikariDataSource ds;

    private static DBManager instance = null;

    public DBManager(EcorteWhitelist plugin) {
        this.plugin = plugin;
        this.config = Config.getInstance(plugin);
    }

    public void connectToSQL() {
        if (config.dbUrl == null || config.username == null || config.password == null) {
            plugin.getLogger().severe("Database connection not configured!");
            return;
        }

        HikariConfig dbConfig = new HikariConfig();
        dbConfig.setJdbcUrl(config.dbUrl);
        dbConfig.setUsername(config.username);
        dbConfig.setPassword(config.password);
        dbConfig.setMaximumPoolSize(5);

        this.ds = new HikariDataSource(dbConfig);

        try (Connection conn = this.ds.getConnection()) {
            conn.setAutoCommit(false);
            try {
                initializeVersionTable(conn);

                int currentVersion = getCurrentVersion(conn);
                int newVersion = performMigrations(conn, currentVersion);
                updateVersion(conn, newVersion);

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                plugin.getLogger().severe("Database migration failed: " + e.getMessage());
                e.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error connecting to the database:");
            e.printStackTrace();
        }
    }

    private int performMigrations(Connection conn, int currentVersion) {
        int newVersion = currentVersion;

        if (currentVersion == 0) {
            try (PreparedStatement createTable = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS ecortewhitelist_whitelist (" +
                            "    uuid VARCHAR(36) PRIMARY KEY NOT NULL," +
                            "    author VARCHAR(36) NOT NULL" +
                            ")"
            )) {
                createTable.executeUpdate();
                newVersion = 1;
                updateVersion(conn, newVersion);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return newVersion;
    }

    private void initializeVersionTable(Connection conn) throws SQLException {
        try (PreparedStatement createTable = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS ecortewhitelist_version (" +
                        "    version INT NOT NULL PRIMARY KEY," +
                        "    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                        ")"
        )) {
            createTable.executeUpdate();
        }
    }

    private int getCurrentVersion(Connection conn) throws SQLException {
        try (PreparedStatement selectVersion = conn.prepareStatement("SELECT version FROM ecortewhitelist_version")) {
            try (ResultSet resultSet = selectVersion.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("version");
                }
            }
        }

        try (PreparedStatement insertVersion = conn.prepareStatement(
                "INSERT INTO ecortewhitelist_version (version) VALUES (0)"
        )) {
            insertVersion.executeUpdate();
        }
        return 0;
    }

    private void updateVersion(Connection conn, int version) throws SQLException {
        try (PreparedStatement updateVersion = conn.prepareStatement(
                "UPDATE ecortewhitelist_version SET version = ?, last_updated = CURRENT_TIMESTAMP"
        )) {
            updateVersion.setInt(1, version);
            updateVersion.executeUpdate();
        }
    }

    public void close() {
        if (ds != null) {
            ds.close();
        }
    }

    public static DBManager getInstance(EcorteWhitelist plugin) {
        if (instance == null) {
            instance = new DBManager(plugin);
        }
        return instance;
    }
}
