package com.muzlik.pvpcombat.database;

import com.zaxxer.hikari.HikariConfig;

import java.util.logging.Logger;

/**
 * MySQL implementation of the database manager.
 * Uses a remote MySQL server with HikariCP connection pooling.
 */
public class MySQLDatabaseManager extends DatabaseManager {
    
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    
    public MySQLDatabaseManager(Logger logger, String host, int port, 
                               String database, String username, String password) {
        super(logger, "MySQL");
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }
    
    @Override
    protected HikariConfig createHikariConfig() {
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setUsername(username);
        config.setPassword(password);
        
        // MySQL-specific settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        
        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        // Connection test query
        config.setConnectionTestQuery("SELECT 1");
        
        return config;
    }
}
