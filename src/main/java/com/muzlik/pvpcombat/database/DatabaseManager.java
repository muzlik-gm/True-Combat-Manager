package com.muzlik.pvpcombat.database;

import com.muzlik.pvpcombat.data.PlayerCombatData;
import com.muzlik.pvpcombat.data.VisualPreferences;
import com.muzlik.pvpcombat.interfaces.IDatabaseManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base database manager with connection pooling via HikariCP.
 * Provides common functionality for SQLite and MySQL implementations.
 */
public abstract class DatabaseManager implements IDatabaseManager {
    
    protected final Logger logger;
    protected HikariDataSource dataSource;
    protected final String databaseType;
    
    private static final int CURRENT_SCHEMA_VERSION = 2;
    
    public DatabaseManager(Logger logger, String databaseType) {
        this.logger = logger;
        this.databaseType = databaseType;
    }
    
    /**
     * Create HikariCP configuration for the specific database type.
     * @return configured HikariConfig
     */
    protected abstract HikariConfig createHikariConfig();
    
    @Override
    public void initialize() throws SQLException {
        try {
            HikariConfig config = createHikariConfig();
            dataSource = new HikariDataSource(config);
            
            // Create tables if they don't exist
            createTables();
            
            // Check and migrate schema if needed
            int currentVersion = getSchemaVersion();
            if (currentVersion < CURRENT_SCHEMA_VERSION) {
                migrateSchema(currentVersion, CURRENT_SCHEMA_VERSION);
            }
            
            logger.info("Database initialized successfully (" + databaseType + ")");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize database", e);
            throw new SQLException("Database initialization failed", e);
        }
    }
    
    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection closed");
        }
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database is not initialized");
        }
        return dataSource.getConnection();
    }
    
    @Override
    public boolean isHealthy() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            return true;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Database health check failed", e);
            return false;
        }
    }
    
    @Override
    public int getSchemaVersion() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version FROM schema_version ORDER BY version DESC LIMIT 1")) {
            if (rs.next()) {
                return rs.getInt("version");
            }
            return 0;
        } catch (SQLException e) {
            // Table doesn't exist yet
            return 0;
        }
    }
    
    /**
     * Create initial database tables.
     */
    protected void createTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Schema version table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS schema_version (" +
                "version INTEGER PRIMARY KEY, " +
                "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            
            // Player stats table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS player_stats (" +
                "player_id VARCHAR(36) PRIMARY KEY, " +
                "wins INTEGER DEFAULT 0, " +
                "losses INTEGER DEFAULT 0, " +
                "total_damage_dealt DOUBLE DEFAULT 0, " +
                "total_damage_received DOUBLE DEFAULT 0, " +
                "total_combat_time BIGINT DEFAULT 0, " +
                "last_combat TIMESTAMP, " +
                "critical_hits INTEGER DEFAULT 0, " +
                "longest_combo INTEGER DEFAULT 0, " +
                "highest_damage_in_session DOUBLE DEFAULT 0" +
                ")"
            );
            
            // Visual preferences table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS visual_preferences (" +
                "player_id VARCHAR(36) PRIMARY KEY, " +
                "selected_theme VARCHAR(50) DEFAULT 'default', " +
                "selected_sound_profile VARCHAR(50) DEFAULT 'default', " +
                "selected_message_style VARCHAR(50) DEFAULT 'minimal', " +
                "animations_enabled BOOLEAN DEFAULT TRUE, " +
                "sounds_enabled BOOLEAN DEFAULT TRUE, " +
                "bossbar_enabled BOOLEAN DEFAULT TRUE, " +
                "actionbar_enabled BOOLEAN DEFAULT TRUE" +
                ")"
            );
            
            // Weapon stats table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS player_weapon_stats (" +
                "player_id VARCHAR(36), " +
                "weapon_material VARCHAR(50), " +
                "uses INTEGER DEFAULT 0, " +
                "total_damage DOUBLE DEFAULT 0, " +
                "kills INTEGER DEFAULT 0, " +
                "critical_hits INTEGER DEFAULT 0, " +
                "PRIMARY KEY (player_id, weapon_material)" +
                ")"
            );

            // Insert initial schema version if not exists
            stmt.execute("INSERT OR IGNORE INTO schema_version (version) VALUES (1)");
            
            logger.info("Database tables created successfully");
        }
    }
    
    @Override
    public void savePlayerData(UUID playerId, PlayerCombatData data) {
        String sql = "INSERT OR REPLACE INTO player_stats " +
                    "(player_id, wins, losses, total_damage_dealt, total_damage_received, " +
                    "total_combat_time, last_combat, critical_hits, longest_combo, highest_damage_in_session) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerId.toString());
            stmt.setInt(2, data.getWins());
            stmt.setInt(3, data.getLosses());
            stmt.setDouble(4, data.getTotalDamageDealt());
            stmt.setDouble(5, data.getTotalDamageReceived());
            stmt.setLong(6, data.getTotalCombatTime());
            stmt.setTimestamp(7, data.getLastCombat() != null ? 
                Timestamp.valueOf(data.getLastCombat()) : null);
            stmt.setInt(8, data.getCriticalHits());
            stmt.setInt(9, data.getLongestCombo());
            stmt.setDouble(10, data.getHighestDamageInSession());
            
            stmt.executeUpdate();

            // Save weapon stats
            saveWeaponStats(conn, playerId, data.getWeaponStats());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save player data for " + playerId, e);
        }
    }

    private void saveWeaponStats(Connection conn, UUID playerId, Map<String, com.muzlik.pvpcombat.data.WeaponStats> weaponStats) throws SQLException {
        String sql = "INSERT OR REPLACE INTO player_weapon_stats " +
                    "(player_id, weapon_material, uses, total_damage, kills, critical_hits) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Map.Entry<String, com.muzlik.pvpcombat.data.WeaponStats> entry : weaponStats.entrySet()) {
                com.muzlik.pvpcombat.data.WeaponStats stats = entry.getValue();
                stmt.setString(1, playerId.toString());
                stmt.setString(2, entry.getKey());
                stmt.setInt(3, stats.getUses());
                stmt.setDouble(4, stats.getTotalDamage());
                stmt.setInt(5, stats.getKills());
                stmt.setInt(6, stats.getCriticalHits());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
    
    @Override
    public PlayerCombatData loadPlayerData(UUID playerId) {
        String sql = "SELECT * FROM player_stats WHERE player_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                PlayerCombatData data = new PlayerCombatData(playerId);
                data.setWins(rs.getInt("wins"));
                data.setLosses(rs.getInt("losses"));
                data.setTotalDamageDealt(rs.getDouble("total_damage_dealt"));
                data.setTotalDamageReceived(rs.getDouble("total_damage_received"));
                data.setTotalCombatTime(rs.getLong("total_combat_time"));
                
                Timestamp lastCombat = rs.getTimestamp("last_combat");
                if (lastCombat != null) {
                    data.setLastCombat(lastCombat.toLocalDateTime());
                }
                
                data.setCriticalHits(rs.getInt("critical_hits"));
                data.setLongestCombo(rs.getInt("longest_combo"));
                data.setHighestDamageInSession(rs.getDouble("highest_damage_in_session"));
                
                // Load weapon stats
                loadWeaponStats(conn, playerId, data);

                return data;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load player data for " + playerId, e);
        }
        
        // Return new data if not found
        return new PlayerCombatData(playerId);
    }
    
    @Override
    public void saveBatch(Map<UUID, PlayerCombatData> dataMap) {
        if (dataMap.isEmpty()) {
            return;
        }
        
        String sql = "INSERT OR REPLACE INTO player_stats " +
                    "(player_id, wins, losses, total_damage_dealt, total_damage_received, " +
                    "total_combat_time, last_combat, critical_hits, longest_combo, highest_damage_in_session) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false);
            
            for (Map.Entry<UUID, PlayerCombatData> entry : dataMap.entrySet()) {
                PlayerCombatData data = entry.getValue();
                
                stmt.setString(1, entry.getKey().toString());
                stmt.setInt(2, data.getWins());
                stmt.setInt(3, data.getLosses());
                stmt.setDouble(4, data.getTotalDamageDealt());
                stmt.setDouble(5, data.getTotalDamageReceived());
                stmt.setLong(6, data.getTotalCombatTime());
                stmt.setTimestamp(7, data.getLastCombat() != null ? 
                    Timestamp.valueOf(data.getLastCombat()) : null);
                stmt.setInt(8, data.getCriticalHits());
                stmt.setInt(9, data.getLongestCombo());
                stmt.setDouble(10, data.getHighestDamageInSession());
                
                stmt.addBatch();
            }
            
            stmt.executeBatch();

            // Save weapon stats for all players in batch
            for (Map.Entry<UUID, PlayerCombatData> entry : dataMap.entrySet()) {
                saveWeaponStats(conn, entry.getKey(), entry.getValue().getWeaponStats());
            }

            conn.commit();
            
            logger.info("Batch saved " + dataMap.size() + " player records");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save batch data", e);
        }
    }
    
    private void loadWeaponStats(Connection conn, UUID playerId, PlayerCombatData data) throws SQLException {
        String sql = "SELECT * FROM player_weapon_stats WHERE player_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String material = rs.getString("weapon_material");
                com.muzlik.pvpcombat.data.WeaponStats stats = data.getWeaponStats(material);
                stats.setUses(rs.getInt("uses"));
                stats.setTotalDamage(rs.getDouble("total_damage"));
                stats.setKills(rs.getInt("kills"));
                stats.setCriticalHits(rs.getInt("critical_hits"));
            }
        }
    }

    @Override
    public void migrateSchema(int fromVersion, int toVersion) throws SQLException {
        logger.info("Migrating database schema from version " + fromVersion + " to " + toVersion);
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            if (fromVersion < 2) {
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS player_weapon_stats (" +
                    "player_id VARCHAR(36), " +
                    "weapon_material VARCHAR(50), " +
                    "uses INTEGER DEFAULT 0, " +
                    "total_damage DOUBLE DEFAULT 0, " +
                    "kills INTEGER DEFAULT 0, " +
                    "critical_hits INTEGER DEFAULT 0, " +
                    "PRIMARY KEY (player_id, weapon_material)" +
                    ")"
                );
            }
            
            // Update schema version
            stmt.execute("INSERT INTO schema_version (version) VALUES (" + toVersion + ")");
            
            logger.info("Schema migration completed successfully");
        }
    }
    
    @Override
    public void saveVisualPreferences(UUID playerId, VisualPreferences preferences) {
        String sql = "INSERT OR REPLACE INTO visual_preferences " +
                    "(player_id, selected_theme, selected_sound_profile, selected_message_style, " +
                    "animations_enabled, sounds_enabled, bossbar_enabled, actionbar_enabled) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerId.toString());
            stmt.setString(2, preferences.getSelectedTheme());
            stmt.setString(3, preferences.getSelectedSoundProfile());
            stmt.setString(4, preferences.getSelectedMessageStyle());
            stmt.setBoolean(5, preferences.isAnimationsEnabled());
            stmt.setBoolean(6, preferences.isSoundsEnabled());
            stmt.setBoolean(7, preferences.isBossBarEnabled());
            stmt.setBoolean(8, preferences.isActionBarEnabled());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save visual preferences for " + playerId, e);
        }
    }
    
    @Override
    public VisualPreferences loadVisualPreferences(UUID playerId) {
        String sql = "SELECT * FROM visual_preferences WHERE player_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new VisualPreferences(
                    playerId,
                    rs.getString("selected_theme"),
                    rs.getString("selected_sound_profile"),
                    rs.getString("selected_message_style"),
                    rs.getBoolean("animations_enabled"),
                    rs.getBoolean("sounds_enabled"),
                    rs.getBoolean("bossbar_enabled"),
                    rs.getBoolean("actionbar_enabled")
                );
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load visual preferences for " + playerId, e);
        }
        
        // Return default preferences if not found
        return new VisualPreferences(playerId);
    }
}
