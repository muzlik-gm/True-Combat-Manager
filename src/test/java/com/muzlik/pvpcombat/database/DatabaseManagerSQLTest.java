package com.muzlik.pvpcombat.database;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerSQLTest {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManagerSQLTest.class.getName());

    private DatabaseManager createManager(String databaseType) {
        return new DatabaseManager(LOGGER, databaseType) {
            @Override
            protected HikariConfig createHikariConfig() {
                return new HikariConfig();
            }
        };
    }

    @Test
    @DisplayName("insertOrIgnore uses INSERT OR IGNORE for SQLite")
    void insertOrIgnoreSQLite() {
        DatabaseManager mgr = createManager("SQLite");
        String sql = mgr.insertOrIgnore("schema_version", "(version)", "(1)");
        assertEquals("INSERT OR IGNORE INTO schema_version (version) VALUES (1)", sql);
    }

    @Test
    @DisplayName("insertOrIgnore uses INSERT IGNORE for MySQL")
    void insertOrIgnoreMySQL() {
        DatabaseManager mgr = createManager("MySQL");
        String sql = mgr.insertOrIgnore("schema_version", "(version)", "(1)");
        assertEquals("INSERT IGNORE INTO schema_version (version) VALUES (1)", sql);
    }

    @Test
    @DisplayName("insertOrReplace uses INSERT OR REPLACE for SQLite")
    void insertOrReplaceSQLite() {
        DatabaseManager mgr = createManager("SQLite");
        String sql = mgr.insertOrReplace("player_stats",
            "(player_id, wins, losses)",
            "(?, ?, ?)");
        assertEquals("INSERT OR REPLACE INTO player_stats (player_id, wins, losses) VALUES (?, ?, ?)", sql);
    }

    @Test
    @DisplayName("insertOrReplace uses REPLACE INTO for MySQL")
    void insertOrReplaceMySQL() {
        DatabaseManager mgr = createManager("MySQL");
        String sql = mgr.insertOrReplace("player_stats",
            "(player_id, wins, losses)",
            "(?, ?, ?)");
        assertEquals("REPLACE INTO player_stats (player_id, wins, losses) VALUES (?, ?, ?)", sql);
    }

    @Test
    @DisplayName("insertOrIgnore produces executable SQL for both databases")
    void insertOrIgnoreProducesValidSQL() {
        DatabaseManager sqlite = createManager("SQLite");
        DatabaseManager mysql = createManager("MySQL");

        String table = "schema_version";
        String columns = "(version)";
        String values = "(1)";

        String sqliteSQL = sqlite.insertOrIgnore(table, columns, values);
        String mysqlSQL = mysql.insertOrIgnore(table, columns, values);

        assertTrue(sqliteSQL.startsWith("INSERT OR IGNORE INTO"));
        assertTrue(mysqlSQL.startsWith("INSERT IGNORE INTO"));
        assertTrue(sqliteSQL.contains("schema_version"));
        assertTrue(mysqlSQL.contains("schema_version"));
    }

    @Test
    @DisplayName("insertOrReplace produces executable SQL for both databases")
    void insertOrReplaceProducesValidSQL() {
        DatabaseManager sqlite = createManager("SQLite");
        DatabaseManager mysql = createManager("MySQL");

        String table = "player_stats";
        String columns = "(player_id, wins, losses, total_damage_dealt)";
        String values = "(?, ?, ?, ?)";

        String sqliteSQL = sqlite.insertOrReplace(table, columns, values);
        String mysqlSQL = mysql.insertOrReplace(table, columns, values);

        assertTrue(sqliteSQL.startsWith("INSERT OR REPLACE INTO"));
        assertTrue(mysqlSQL.startsWith("REPLACE INTO"));
        assertTrue(sqliteSQL.contains("player_stats"));
        assertTrue(mysqlSQL.contains("player_stats"));
    }
}
