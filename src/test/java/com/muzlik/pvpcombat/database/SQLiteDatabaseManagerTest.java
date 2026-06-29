package com.muzlik.pvpcombat.database;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SQLiteDatabaseManagerTest {

    private static final Logger LOGGER = Logger.getLogger(SQLiteDatabaseManagerTest.class.getName());

    @TempDir
    File tempDir;

    private SQLiteDatabaseManager manager;

    @BeforeEach
    void setUp() {
        manager = new SQLiteDatabaseManager(LOGGER, tempDir);
    }

    @Test
    @Order(1)
    @DisplayName("databaseType should be SQLite")
    void databaseTypeIsSQLite() {
        assertEquals("SQLite", manager.databaseType);
    }

    @Test
    @Order(2)
    @DisplayName("createHikariConfig sets JDBC URL with sqlite prefix")
    void jdbcUrlHasSqlitePrefix() {
        HikariConfig config = manager.createHikariConfig();
        assertTrue(config.getJdbcUrl().startsWith("jdbc:sqlite:"));
    }

    @Test
    @Order(3)
    @DisplayName("createHikariConfig sets JDBC URL pointing to combat_data.db")
    void jdbcUrlPointsToCombatDataDb() {
        HikariConfig config = manager.createHikariConfig();
        assertTrue(config.getJdbcUrl().endsWith("combat_data.db"));
    }

    @Test
    @Order(4)
    @DisplayName("createHikariConfig sets JDBC URL with absolute path")
    void jdbcUrlUsesAbsolutePath() {
        HikariConfig config = manager.createHikariConfig();
        String url = config.getJdbcUrl();
        String path = url.substring("jdbc:sqlite:".length());
        assertTrue(new File(path).isAbsolute());
    }

    @Test
    @Order(5)
    @DisplayName("createHikariConfig sets correct driver class name")
    void driverClassNameIsCorrect() {
        HikariConfig config = manager.createHikariConfig();
        assertEquals("org.sqlite.JDBC", config.getDriverClassName());
    }

    @Test
    @Order(6)
    @DisplayName("createHikariConfig sets maximum pool size to 1")
    void maximumPoolSizeIsOne() {
        HikariConfig config = manager.createHikariConfig();
        assertEquals(1, config.getMaximumPoolSize());
    }

    @Test
    @Order(7)
    @DisplayName("createHikariConfig sets connection timeout to 30000")
    void connectionTimeoutIs30000() {
        HikariConfig config = manager.createHikariConfig();
        assertEquals(30000L, config.getConnectionTimeout());
    }

    @Test
    @Order(8)
    @DisplayName("createHikariConfig sets idle timeout to 600000")
    void idleTimeoutIs600000() {
        HikariConfig config = manager.createHikariConfig();
        assertEquals(600000L, config.getIdleTimeout());
    }

    @Test
    @Order(9)
    @DisplayName("createHikariConfig sets max lifetime to 1800000")
    void maxLifetimeIs1800000() {
        HikariConfig config = manager.createHikariConfig();
        assertEquals(1800000L, config.getMaxLifetime());
    }

    @Test
    @Order(10)
    @DisplayName("createHikariConfig sets connection init SQL with pragmas")
    void connectionInitSqlHasPragmas() {
        HikariConfig config = manager.createHikariConfig();
        String initSql = config.getConnectionInitSql();
        assertNotNull(initSql);
        assertTrue(initSql.contains("PRAGMA journal_mode=WAL"));
        assertTrue(initSql.contains("PRAGMA synchronous=NORMAL"));
        assertTrue(initSql.contains("PRAGMA temp_store=MEMORY"));
        assertTrue(initSql.contains("PRAGMA mmap_size=30000000000"));
    }

    @Test
    @Order(11)
    @DisplayName("createHikariConfig sets cachePrepStmts data source property")
    void cachePrepStmtsEnabled() {
        HikariConfig config = manager.createHikariConfig();
        assertEquals("true", config.getDataSourceProperties().getProperty("cachePrepStmts"));
    }

    @Test
    @Order(12)
    @DisplayName("createHikariConfig sets prepStmtCacheSize to 250")
    void prepStmtCacheSizeIs250() {
        HikariConfig config = manager.createHikariConfig();
        assertEquals("250", config.getDataSourceProperties().getProperty("prepStmtCacheSize"));
    }

    @Test
    @Order(13)
    @DisplayName("createHikariConfig sets prepStmtCacheSqlLimit to 2048")
    void prepStmtCacheSqlLimitIs2048() {
        HikariConfig config = manager.createHikariConfig();
        assertEquals("2048", config.getDataSourceProperties().getProperty("prepStmtCacheSqlLimit"));
    }

    @Test
    @Order(14)
    @DisplayName("createHikariConfig creates data folder if not exists")
    void dataFolderCreated() {
        File nestedDir = new File(tempDir, "nested/subdir");
        SQLiteDatabaseManager nestedManager = new SQLiteDatabaseManager(LOGGER, nestedDir);
        HikariConfig config = nestedManager.createHikariConfig();
        assertTrue(nestedDir.exists());
        assertTrue(config.getJdbcUrl().contains("combat_data.db"));
    }
}
