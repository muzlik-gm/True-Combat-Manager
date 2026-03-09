package com.muzlik.pvpcombat.interfaces;

import com.muzlik.pvpcombat.data.PlayerCombatData;
import com.muzlik.pvpcombat.data.VisualPreferences;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

/**
 * Interface for database management operations.
 * Handles persistent storage of combat statistics and player data.
 */
public interface IDatabaseManager {
    
    /**
     * Initialize the database connection and schema.
     * @throws SQLException if initialization fails
     */
    void initialize() throws SQLException;
    
    /**
     * Shutdown the database connection gracefully.
     */
    void shutdown();
    
    /**
     * Get a database connection from the pool.
     * @return active database connection
     * @throws SQLException if connection cannot be obtained
     */
    Connection getConnection() throws SQLException;
    
    /**
     * Save player combat data to the database.
     * @param playerId the player's UUID
     * @param data the combat data to save
     */
    void savePlayerData(UUID playerId, PlayerCombatData data);
    
    /**
     * Load player combat data from the database.
     * @param playerId the player's UUID
     * @return the player's combat data, or a new instance if not found
     */
    PlayerCombatData loadPlayerData(UUID playerId);
    
    /**
     * Save multiple player data entries in a batch operation.
     * @param dataMap map of player UUIDs to combat data
     */
    void saveBatch(Map<UUID, PlayerCombatData> dataMap);
    
    /**
     * Save player visual preferences to the database.
     * @param playerId the player's UUID
     * @param preferences the visual preferences to save
     */
    void saveVisualPreferences(UUID playerId, VisualPreferences preferences);
    
    /**
     * Load player visual preferences from the database.
     * @param playerId the player's UUID
     * @return the player's visual preferences, or default preferences if not found
     */
    VisualPreferences loadVisualPreferences(UUID playerId);
    
    /**
     * Migrate database schema from one version to another.
     * @param fromVersion the current schema version
     * @param toVersion the target schema version
     * @throws SQLException if migration fails
     */
    void migrateSchema(int fromVersion, int toVersion) throws SQLException;
    
    /**
     * Check if the database connection is healthy.
     * @return true if the database is accessible and functional
     */
    boolean isHealthy();
    
    /**
     * Get the current schema version.
     * @return the schema version number
     */
    int getSchemaVersion();
}
