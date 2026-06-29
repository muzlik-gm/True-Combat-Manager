package com.muzlik.pvpcombat.database;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.*;

import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MySQLDatabaseManagerTest {

    private static final Logger LOGGER = Logger.getLogger(MySQLDatabaseManagerTest.class.getName());

    private MySQLDatabaseManager manager;

    @BeforeEach
    void setUp() {
        manager = new MySQLDatabaseManager(LOGGER, "localhost", 3306, "pvp_combat", "testuser", "testpass");
    }

    @Test
    @Order(1)
    @DisplayName("databaseType should be MySQL")
    void databaseTypeIsMySQL() {
        assertEquals("MySQL", manager.databaseType);
    }

    @Test
    @Order(2)
    @DisplayName("createHikariConfig sets correct JDBC URL")
    void jdbcUrlIsCorrect() {
        HikariConfig config = manager.createHikariConfig();
        assertEquals("jdbc:mysql://localhost:3306/pvp_combat", config.getJdbcUrl());
    }

    @Test
    @Order(3)
    @DisplayName("createHikariConfig sets correct driver class name")
    void driverClassNameIsCorrect() {
        HikariConfig config = manager.createHikariConfig();
        assertEquals("com.mysql.cj.jdbc.Driver", config.getDriverClassName());
    }

    @Test
    @Order(4)
    @DisplayName("createHikariConfig sets credentials")
    void credentialsAreSet() {
        HikariConfig config = manager.createHikariConfig();
        assertEquals("testuser", config.getUsername());
        assertEquals("testpass", config.getPassword());
    }

    @Test
    @Order(5)
    @DisplayName("createHikariConfig sets maximum pool size to 10")
    void maximumPoolSizeIsTen() {
        HikariConfig config = manager.createHikariConfig();
        assertEquals(10, config.getMaximumPoolSize());
    }

    @Test
    @Order(6)
    @DisplayName("createHikariConfig sets minimum idle to 2")
    void minimumIdleIsTwo() {
        HikariConfig config = manager.createHikariConfig();
        assertEquals(2, config.getMinimumIdle());
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
    @DisplayName("createHikariConfig sets leak detection threshold to 60000")
    void leakDetectionThresholdIs60000() {
        HikariConfig config = manager.createHikariConfig();
        assertEquals(60000L, config.getLeakDetectionThreshold());
    }

    @Test
    @Order(11)
    @DisplayName("createHikariConfig sets connection test query to SELECT 1")
    void connectionTestQueryIsSelect1() {
        HikariConfig config = manager.createHikariConfig();
        assertEquals("SELECT 1", config.getConnectionTestQuery());
    }

    @Test
    @Order(12)
    @DisplayName("createHikariConfig sets data source properties")
    void dataSourcePropertiesAreSet() {
        HikariConfig config = manager.createHikariConfig();
        Properties props = config.getDataSourceProperties();

        assertNotNull(props);
        assertEquals("true", props.getProperty("cachePrepStmts"));
        assertEquals("250", props.getProperty("prepStmtCacheSize"));
        assertEquals("2048", props.getProperty("prepStmtCacheSqlLimit"));
        assertEquals("true", props.getProperty("useServerPrepStmts"));
        assertEquals("true", props.getProperty("useLocalSessionState"));
        assertEquals("true", props.getProperty("rewriteBatchedStatements"));
        assertEquals("true", props.getProperty("cacheResultSetMetadata"));
        assertEquals("true", props.getProperty("cacheServerConfiguration"));
        assertEquals("true", props.getProperty("elideSetAutoCommits"));
        assertEquals("false", props.getProperty("maintainTimeStats"));
    }

    @Test
    @Order(13)
    @DisplayName("createHikariConfig adds all performance optimizations")
    void allPerformancePropertiesPresent() {
        HikariConfig config = manager.createHikariConfig();
        Properties props = config.getDataSourceProperties();
        assertEquals(10, props.size());
    }

    @Test
    @Order(14)
    @DisplayName("JDBC URL includes custom host and port")
    void jdbcUrlWithCustomHostPort() {
        MySQLDatabaseManager customManager = new MySQLDatabaseManager(LOGGER, "db.example.com", 3307, "mydb", "u", "p");
        HikariConfig config = customManager.createHikariConfig();
        assertEquals("jdbc:mysql://db.example.com:3307/mydb", config.getJdbcUrl());
    }
}
