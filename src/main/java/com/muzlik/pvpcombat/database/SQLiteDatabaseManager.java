package com.muzlik.pvpcombat.database;

import com.zaxxer.hikari.HikariConfig;

import java.io.File;
import java.util.logging.Logger;

/**
 * SQLite implementation of the database manager.
 * Uses a local file-based database with HikariCP connection pooling.
 */
public class SQLiteDatabaseManager extends DatabaseManager {
    
    private final File databaseFile;
    
    public SQLiteDatabaseManager(Logger logger, File dataFolder) {
        super(logger, "SQLite");
        this.databaseFile = new File(dataFolder, "combat_data.db");
    }
    
    @Override
    protected HikariConfig createHikariConfig() {
        // Ensure data folder exists
        File dataFolder = databaseFile.getParentFile();
        if (dataFolder != null && !dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        
        // SQLite-specific settings
        config.setMaximumPoolSize(1); // SQLite doesn't support multiple connections well
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        // SQLite pragmas for better performance
        config.setConnectionInitSql(
            "PRAGMA journal_mode=WAL;" +
            "PRAGMA synchronous=NORMAL;" +
            "PRAGMA temp_store=MEMORY;" +
            "PRAGMA mmap_size=30000000000;"
        );
        
        return config;
    }
}
