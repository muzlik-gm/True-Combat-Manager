package com.muzlik.pvpcombat.combat;

import com.muzlik.pvpcombat.data.CombatEvent;
import com.muzlik.pvpcombat.data.PlayerCombatData;
import com.muzlik.pvpcombat.interfaces.IDatabaseManager;
import com.muzlik.pvpcombat.performance.LagManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles combat detection and tracking of combat events.
 * Integrates with LagManager for performance-aware combat decisions.
 * Integrates with DatabaseManager for persistent storage.
 */
public class CombatTracker {

    private final Map<UUID, PlayerCombatData> playerData;
    private LagManager lagManager;
    private IDatabaseManager databaseManager;
    private final Plugin plugin;
    private final Logger logger;
    private BukkitTask autoSaveTask;
    
    private static final long AUTO_SAVE_INTERVAL = 5 * 60 * 20L; // 5 minutes in ticks

    public CombatTracker(Plugin plugin) {
        this.playerData = new ConcurrentHashMap<>();
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Sets the lag manager for performance monitoring integration.
     */
    public void setLagManager(LagManager lagManager) {
        this.lagManager = lagManager;
    }
    
    /**
     * Sets the database manager for persistent storage.
     */
    public void setDatabaseManager(IDatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    /**
     * Initialize the tracker and load existing data.
     */
    public void initialize() {
        if (databaseManager != null) {
            logger.info("Loading player combat data from database...");
            loadAllData();
            startAutoSave();
        }
    }
    
    /**
     * Shutdown the tracker and save all data.
     */
    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        if (databaseManager != null) {
            logger.info("Saving all player combat data...");
            saveAllData();
        }
    }
    
    /**
     * Start the auto-save task.
     */
    private void startAutoSave() {
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                saveAllData();
                logger.info("Auto-saved combat data for " + playerData.size() + " players");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to auto-save combat data", e);
            }
        }, AUTO_SAVE_INTERVAL, AUTO_SAVE_INTERVAL);
    }
    
    /**
     * Save all player data to the database.
     */
    public void saveAllData() {
        if (databaseManager == null) {
            return;
        }
        
        try {
            databaseManager.saveBatch(new ConcurrentHashMap<>(playerData));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save player data batch", e);
        }
    }
    
    /**
     * Load all player data from the database.
     */
    private void loadAllData() {
        // Data is loaded on-demand when players join
        // This method is here for future bulk loading if needed
    }
    
    /**
     * Save a specific player's data to the database.
     */
    public void savePlayerData(UUID playerId) {
        if (databaseManager == null) {
            return;
        }
        
        PlayerCombatData data = playerData.get(playerId);
        if (data != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    databaseManager.savePlayerData(playerId, data);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to save data for player " + playerId, e);
                }
            });
        }
    }
    
    /**
     * Load a specific player's data from the database.
     */
    public void loadPlayerData(UUID playerId) {
        if (databaseManager == null) {
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerCombatData data = databaseManager.loadPlayerData(playerId);
                playerData.put(playerId, data);
                logger.fine("Loaded combat data for player " + playerId);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to load data for player " + playerId, e);
                // Create new data if loading fails
                playerData.put(playerId, new PlayerCombatData(playerId));
            }
        });
    }

    /**
     * Records a combat event.
     */
    public void recordEvent(CombatEvent event) {
        PlayerCombatData data = getPlayerData(event.getPlayerId());
        data.getEvents().add(event);
        data.updateLastActivity(System.currentTimeMillis());
        data.getStats().increment(event.getEventType());
    }

    /**
     * Gets or creates player combat data.
     */
    public PlayerCombatData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, PlayerCombatData::new);
    }

    /**
     * Records damage dealt in combat.
     */
    public void recordDamageDealt(Player attacker, double damage) {
        PlayerCombatData data = getPlayerData(attacker.getUniqueId());
        data.addDamageDealt(damage);
        data.updateLastActivity(System.currentTimeMillis());

        // Update performance data for lag detection
        if (lagManager != null) {
            lagManager.updatePlayerPing(attacker);
        }
    }

    /**
     * Records damage received in combat.
     */
    public void recordDamageReceived(Player defender, double damage) {
        PlayerCombatData data = getPlayerData(defender.getUniqueId());
        data.addDamageReceived(damage);
        data.updateLastActivity(System.currentTimeMillis());

        // Update performance data for lag detection
        if (lagManager != null) {
            lagManager.updatePlayerPing(defender);
        }
    }

    /**
     * Records a combat win.
     */
    public void recordWin(Player winner) {
        PlayerCombatData data = getPlayerData(winner.getUniqueId());
        data.incrementWins();
        data.incrementCombats();
        data.updateLastActivity(System.currentTimeMillis());
        
        // Log for debugging
        System.out.println("[COMBAT] " + winner.getName() + " won! Total wins: " + data.getWins());
    }

    /**
     * Records a combat loss.
     */
    public void recordLoss(Player loser) {
        PlayerCombatData data = getPlayerData(loser.getUniqueId());
        data.incrementLosses();
        data.incrementCombats();
        data.updateLastActivity(System.currentTimeMillis());
        
        // Log for debugging
        System.out.println("[COMBAT] " + loser.getName() + " lost! Total losses: " + data.getLosses());
    }

    /**
     * Gets all player data (for persistence).
     */
    public Map<UUID, PlayerCombatData> getAllPlayerData() {
        return new ConcurrentHashMap<>(playerData);
    }

    /**
     * Records a combat win by UUID (for offline players).
     */
    public void recordWinByUUID(UUID winnerId) {
        PlayerCombatData data = getPlayerData(winnerId);
        data.incrementWins();
        data.incrementCombats();
        data.updateLastActivity(System.currentTimeMillis());
        
        // Log for debugging
        System.out.println("[COMBAT] Player " + winnerId + " won! Total wins: " + data.getWins());
    }

    /**
     * Records a combat loss by UUID (for offline players).
     */
    public void recordLossByUUID(UUID loserId) {
        PlayerCombatData data = getPlayerData(loserId);
        data.incrementLosses();
        data.incrementCombats();
        data.updateLastActivity(System.currentTimeMillis());
        
        // Log for debugging
        System.out.println("[COMBAT] Player " + loserId + " lost! Total losses: " + data.getLosses());
    }

    /**
     * Clears old/inactive player data.
     */
    public void cleanupInactiveData() {
        long cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000; // 24 hours
        playerData.entrySet().removeIf(entry -> entry.getValue().getLastActivity() < cutoff);
    }
}