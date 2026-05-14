package com.muzlik.pvpcombat.combat;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import com.muzlik.pvpcombat.data.CombatSession;
import com.muzlik.pvpcombat.data.CombatState;
import com.muzlik.pvpcombat.data.TimerData;
import com.muzlik.pvpcombat.events.CombatEndEvent;
import com.muzlik.pvpcombat.events.CombatStartEvent;
import com.muzlik.pvpcombat.integration.crossserver.NetworkSyncManager;
import com.muzlik.pvpcombat.interfaces.ICombatManager;
import com.muzlik.pvpcombat.interfaces.IConfigManager;
import com.muzlik.pvpcombat.logging.CombatLogger;
import com.muzlik.pvpcombat.performance.LagManager;
import com.muzlik.pvpcombat.performance.PerformanceMonitor;
import com.muzlik.pvpcombat.utils.AsyncUtils;
import com.muzlik.pvpcombat.utils.CacheManager;
import com.muzlik.pvpcombat.visual.VisualManager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Thread-safe singleton managing all active combat sessions.
 * Uses ReadWriteLock for optimal concurrent access and ConcurrentHashMap for session storage.
 */
public class CombatManager implements ICombatManager {

    private final PvPCombatPlugin plugin;
    private final ConcurrentHashMap<UUID, CombatSession> activeSessions;
    private final ConcurrentHashMap<UUID, BukkitTask> sessionTimers;
    private final ReadWriteLock sessionLock;
    private final VisualManager visualManager;
    private final CombatLogger combatLogger;
    private final LagManager lagManager;
    private final NetworkSyncManager networkSyncManager;
    private final PerformanceMonitor performanceMonitor;
    private final CacheManager cacheManager;
    private int defaultTimerSeconds;

    private final CombatTracker combatTracker;
    private final DisconnectTracker disconnectTracker;

    public CombatManager(PvPCombatPlugin plugin, CombatLogger combatLogger, NetworkSyncManager networkSyncManager,
                        PerformanceMonitor performanceMonitor, CacheManager cacheManager, IConfigManager configManager) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        this.sessionTimers = new ConcurrentHashMap<>();
        this.sessionLock = new ReentrantReadWriteLock();
        this.visualManager = new VisualManager(plugin, configManager);
        this.combatLogger = combatLogger;
        this.combatTracker = new CombatTracker(plugin);
        this.disconnectTracker = new DisconnectTracker(plugin, this);
        this.lagManager = new LagManager(plugin, performanceMonitor.getTpsMonitor(), performanceMonitor);
        this.combatTracker.setLagManager(this.lagManager);
        this.networkSyncManager = networkSyncManager;
        this.performanceMonitor = performanceMonitor;
        this.cacheManager = cacheManager;
        this.defaultTimerSeconds = plugin.getConfig().getInt("combat.duration", 30);
    }

    /**
     * Executes an operation with a read lock.
     * Use for operations that only read session data.
     * 
     * @param operation The operation to execute
     */
    private void withReadLock(Runnable operation) {
        sessionLock.readLock().lock();
        try {
            operation.run();
        } finally {
            sessionLock.readLock().unlock();
        }
    }

    /**
     * Executes an operation with a read lock and returns a value.
     * Use for operations that only read session data.
     * 
     * @param operation The operation to execute
     * @return The result of the operation
     */
    private <T> T withReadLock(Supplier<T> operation) {
        sessionLock.readLock().lock();
        try {
            return operation.get();
        } finally {
            sessionLock.readLock().unlock();
        }
    }

    /**
     * Executes an operation with a write lock.
     * Use for operations that modify session data.
     * 
     * @param operation The operation to execute
     */
    private void withWriteLock(Runnable operation) {
        sessionLock.writeLock().lock();
        try {
            operation.run();
        } finally {
            sessionLock.writeLock().unlock();
        }
    }

    /**
     * Executes an operation with a write lock and returns a value.
     * Use for operations that modify session data.
     * 
     * @param operation The operation to execute
     * @return The result of the operation
     */
    private <T> T withWriteLock(Supplier<T> operation) {
        sessionLock.writeLock().lock();
        try {
            return operation.get();
        } finally {
            sessionLock.writeLock().unlock();
        }
    }

    @Override
    public UUID startCombat(Player attacker, Player defender) {
        performanceMonitor.startOperation("combat-start");

        try {
            // Use write lock for session creation (atomic operation)
            return withWriteLock(() -> {
                // Check cache first for existing combat state
                String cacheKey = attacker.getUniqueId() + ":" + defender.getUniqueId();
                CombatSession cachedSession = (CombatSession) cacheManager.get("combat-states", cacheKey);

                if (cachedSession != null && cachedSession.isActive()) {
                    // #region agent log
                    com.muzlik.pvpcombat.debug.AgentDebugLog.log("pre", "H2", "CombatManager.java:startCombat",
                            "blocked_cache_active", java.util.Map.of("cacheKey", cacheKey));
                    // #endregion
                    return null; // Already in combat
                }

                // Check if either player is already in combat
                if (isInCombatUnsafe(attacker) || isInCombatUnsafe(defender)) {
                    // #region agent log
                    com.muzlik.pvpcombat.debug.AgentDebugLog.log("pre", "H2", "CombatManager.java:startCombat",
                            "blocked_already_in_combat", java.util.Map.of(
                                    "attackerIn", isInCombatUnsafe(attacker),
                                    "defenderIn", isInCombatUnsafe(defender)));
                    // #endregion
                    return null; // Cannot start new combat
                }
                
                // Check if either player is in a safe zone
                if (isInSafeZone(attacker) || isInSafeZone(defender)) {
                    plugin.getLogger().info("Combat prevented: One or both players are in a safe zone");
                    // #region agent log
                    com.muzlik.pvpcombat.debug.AgentDebugLog.log("pre", "H2", "CombatManager.java:startCombat",
                            "blocked_safezone", java.util.Map.of(
                                    "attackerSz", isInSafeZone(attacker),
                                    "defenderSz", isInSafeZone(defender)));
                    // #endregion
                    return null; // Cannot start combat in safe zone
                }

                UUID sessionId = UUID.randomUUID();
                CombatSession session = new CombatSession(sessionId, attacker, defender, defaultTimerSeconds);

                activeSessions.put(attacker.getUniqueId(), session);
                activeSessions.put(defender.getUniqueId(), session);

                // Cache the combat state
                cacheManager.put("combat-states", cacheKey, session);

                // Register session with lag manager for performance monitoring
                lagManager.registerSession(sessionId);

                // Start timer task asynchronously
                AsyncUtils.runAsync(plugin, () -> startTimerTask(session), "combat-processing");

                // Initialize visual elements (keep on main thread for thread safety)
                AsyncUtils.runSync(plugin, () -> {
                    visualManager.displayBossBar(sessionId.toString());
                    visualManager.getActionBarManager().startActionBarUpdates(sessionId.toString(), attacker, defender);
                    visualManager.getSoundManager().playCombatStartSound(attacker);
                    visualManager.getSoundManager().playCombatStartSound(defender);
                });

                // Fire CombatStartEvent
                plugin.getServer().getPluginManager().callEvent(new CombatStartEvent(session, attacker, defender));

                // Broadcast combat start across network asynchronously if sync is enabled
                if (networkSyncManager != null && networkSyncManager.isEnabled()) {
                    AsyncUtils.runAsync(plugin, () -> {
                        networkSyncManager.broadcastCombatStart(session).whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                plugin.getLogger().warning("Failed to broadcast combat start: " + throwable.getMessage());
                            }
                        });
                    }, "combat-processing");
                }

                // Log combat start asynchronously
                AsyncUtils.runAsync(plugin, () ->
                    combatLogger.logCombatStart(sessionId, attacker, defender), "combat-processing");

                plugin.getLogger().info("Combat started between " + attacker.getName() + " and " + defender.getName());
                // #region agent log
                com.muzlik.pvpcombat.debug.AgentDebugLog.log("pre", "H2", "CombatManager.java:startCombat",
                        "combat_started", java.util.Map.of("sessionId", sessionId.toString()));
                // #endregion
                return sessionId;
            });
        } finally {
            performanceMonitor.endOperation("combat-start");
        }
    }

    /**
     * Internal method to check combat status without acquiring lock.
     * MUST be called within a lock context.
     */
    private boolean isInCombatUnsafe(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    @Override
    public boolean endCombat(UUID playerId) {
        performanceMonitor.startOperation("combat-end");

        try {
            // Use write lock for session removal (atomic operation)
            return withWriteLock(() -> {
                CombatSession session = activeSessions.remove(playerId);
                if (session != null) {
                    UUID sessionId = session.getSessionId();

                    // Calculate combat duration
                    long combatDuration = System.currentTimeMillis() - session.getStartTime();
                    
                    // Update combat tracker with combat time for both players
                    com.muzlik.pvpcombat.data.PlayerCombatData attackerData = combatTracker.getPlayerData(session.getAttacker().getUniqueId());
                    com.muzlik.pvpcombat.data.PlayerCombatData defenderData = combatTracker.getPlayerData(session.getDefender().getUniqueId());
                    
                    attackerData.addCombatTime(combatDuration);
                    defenderData.addCombatTime(combatDuration);

                    // Merge weapon stats from session to global data
                    mergeWeaponStats(session.getAttacker(), session);
                    mergeWeaponStats(session.getDefender(), session);
                    
                    // Update last combat timestamp
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    attackerData.setLastCombat(now);
                    defenderData.setLastCombat(now);
                    
                    // Log the combat data for debugging
                    plugin.getLogger().info(String.format("[COMBAT END] %s: %.1f dmg dealt, %d wins, %d losses | %s: %.1f dmg dealt, %d wins, %d losses",
                        session.getAttacker().getName(), attackerData.getTotalDamageDealt(), attackerData.getWins(), attackerData.getLosses(),
                        session.getDefender().getName(), defenderData.getTotalDamageDealt(), defenderData.getWins(), defenderData.getLosses()));

                    // Remove from cache
                    String cacheKey = session.getAttacker().getUniqueId() + ":" + session.getDefender().getUniqueId();
                    cacheManager.remove("combat-states", cacheKey);

                    // Remove both players from the session
                    activeSessions.remove(session.getAttacker().getUniqueId());
                    activeSessions.remove(session.getDefender().getUniqueId());

                    session.setActive(false);
                    session.setState(CombatState.NOT_IN_COMBAT);

                    // Unregister session from lag manager
                    lagManager.unregisterSession(sessionId);

                    // Cancel timer task
                    BukkitTask timerTask = sessionTimers.remove(sessionId);
                    if (timerTask != null) {
                        timerTask.cancel();
                    }

                    // Clear visual elements (keep on main thread)
                    AsyncUtils.runSync(plugin, () -> {
                        visualManager.clearVisuals(session.getAttacker());
                        visualManager.clearVisuals(session.getDefender());
                        visualManager.getSoundManager().playCombatEndSound(session.getAttacker());
                        visualManager.getSoundManager().playCombatEndSound(session.getDefender());
                    });

                    // Fire CombatEndEvent
                    Player winner = session.getAttacker().getUniqueId().equals(playerId) ? session.getDefender() : session.getAttacker();
                    Player loser = session.getAttacker().getUniqueId().equals(playerId) ? session.getAttacker() : session.getDefender();
                    plugin.getServer().getPluginManager().callEvent(new CombatEndEvent(session, winner, loser, CombatEndEvent.CombatEndReason.FORCE_END));

                    // Broadcast combat end across network asynchronously if sync is enabled
                    if (networkSyncManager != null && networkSyncManager.isEnabled()) {
                        AsyncUtils.runAsync(plugin, () -> {
                            networkSyncManager.broadcastCombatEnd(session.getSessionId(), "Combat ended").whenComplete((result, throwable) -> {
                                if (throwable != null) {
                                    plugin.getLogger().warning("Failed to broadcast combat end: " + throwable.getMessage());
                                }
                            });
                        }, "combat-processing");
                    }

                    // Log combat end and generate summaries asynchronously
                    // Pass session data before it's removed
                    final CombatSession finalSession = session;
                    AsyncUtils.runAsync(plugin, () -> {
                        combatLogger.logCombatEnd(finalSession.getSessionId(), finalSession.getAttacker(),
                                                  finalSession.getDefender(), "Combat ended");
                        combatLogger.generateSummary(finalSession.getSessionId(), finalSession.getAttacker(), finalSession);
                        combatLogger.generateSummary(finalSession.getSessionId(), finalSession.getDefender(), finalSession);
                    }, "combat-processing");

                    plugin.getLogger().info("Combat ended for player " + playerId + " (Duration: " + (combatDuration / 1000) + "s)");
                    return true;
                }
                return false;
            });
        } finally {
            performanceMonitor.endOperation("combat-end");
        }
    }

    @Override
    public boolean isInCombat(Player player) {
        // Use read lock for safe concurrent access
        return withReadLock(() -> activeSessions.containsKey(player.getUniqueId()));
    }

    @Override
    public boolean resetTimer(UUID sessionId) {
        // Use read lock since we're only reading and updating session state
        return withReadLock(() -> {
            // Find session by sessionId (need to iterate since map is keyed by player UUID)
            for (CombatSession session : activeSessions.values()) {
                if (session.getSessionId().equals(sessionId)) {
                    session.resetTimer();

                    // Update bossbar progress
                    double progress = session.getTimerData().getProgress();
                    visualManager.updateBossBarProgress(sessionId.toString(), progress);

                    // Play timer reset sound
                    visualManager.getSoundManager().playTimerResetSound(session.getAttacker());
                    visualManager.getSoundManager().playTimerResetSound(session.getDefender());

                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public Player getOpponent(Player player) {
        // Use read lock for safe concurrent access
        return withReadLock(() -> {
            CombatSession session = activeSessions.get(player.getUniqueId());
            return session != null ? session.getOpponent(player) : null;
        });
    }

    /**
     * Gets all active sessions (for cleanup).
     * Returns a defensive copy to prevent concurrent modification.
     */
    public Map<UUID, CombatSession> getActiveSessions() {
        return withReadLock(() -> new ConcurrentHashMap<>(activeSessions));
    }

    /**
     * Starts a timer task for a combat session.
     */
    private void startTimerTask(CombatSession session) {
        UUID sessionId = session.getSessionId();

        // #region agent log
        com.muzlik.pvpcombat.debug.AgentDebugLog.log("pre", "H1", "CombatManager.java:startTimerTask",
                "scheduling_timer", java.util.Map.of(
                        "thread", Thread.currentThread().getName(),
                        "sessionId", sessionId.toString()));
        // #endregion

        BukkitRunnable timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!session.isActive()) {
                    cancel();
                    return;
                }

                // Update player ping data for lag detection
                lagManager.updatePlayerPing(session.getAttacker());
                lagManager.updatePlayerPing(session.getDefender());

                // Check for lag adjustments
                int lagExtension = lagManager.checkAndApplyLagAdjustment(sessionId,
                        session.getAttacker(), session.getDefender());

                if (lagExtension > 0) {
                    // Apply lag extension to timer
                    TimerData timerData = session.getTimerData();
                    int newRemaining = timerData.getRemainingSeconds() + lagExtension;
                    timerData.setRemainingSeconds(newRemaining);
                    session.setTimerSeconds(newRemaining);

                    plugin.getLogger().fine(String.format("Extended combat timer for session %s by %d seconds due to lag",
                            sessionId, lagExtension));
                }

                // Update timer and check if expired
                boolean expired = session.updateTimer();

                if (expired) {
                    // Combat timer expired - end combat
                    endCombat(session.getAttacker().getUniqueId());
                } else {
                    // Periodic sync of combat state across network
                    if (networkSyncManager != null && networkSyncManager.isEnabled()) {
                        // Sync every 30 seconds or when significant changes occur
                        if (session.getTimerData().getRemainingSeconds() % 30 == 0) {
                            networkSyncManager.broadcastCombatStart(session).whenComplete((result, throwable) -> {
                                if (throwable != null) {
                                    plugin.getLogger().fine("Failed to sync combat state: " + throwable.getMessage());
                                }
                            });
                        }
                    }

                    // Update visual elements with current time
                    int remainingTime = session.getRemainingTime();
                    double progress = session.getTimerData().getProgress();
                    
                    // Update bossbar progress and title
                    visualManager.updateBossBarProgress(sessionId.toString(), progress);
                    String title = plugin.getConfig().getString("combat.bossbar.title", "&cCombat: &f{time_left}s")
                        .replace("{time_left}", String.valueOf(remainingTime))
                        .replace("&", "§");
                    visualManager.updateBossBarTitle(sessionId.toString(), title);

                    // Play warning sound at 5 seconds
                    if (remainingTime == 5) {
                        visualManager.getSoundManager().playTimerWarningSound(session.getAttacker());
                        visualManager.getSoundManager().playTimerWarningSound(session.getDefender());
                    }
                }
            }
        };

        // Run every second (20 ticks)
        BukkitTask task = timerTask.runTaskTimer(plugin, 0L, 20L);
        sessionTimers.put(sessionId, task);
    }

    /**
     * Gets a session by its ID.
     * Thread-safe with read lock.
     */
    public CombatSession getSessionById(String sessionId) {
        return withReadLock(() -> {
            try {
                UUID id = UUID.fromString(sessionId);
                for (CombatSession session : activeSessions.values()) {
                    if (session.getSessionId().equals(id)) {
                        return session;
                    }
                }
            } catch (IllegalArgumentException e) {
                // Invalid UUID format
            }
            return null;
        });
    }

    /**
     * Gets the visual manager.
     */
    public VisualManager getVisualManager() {
        return visualManager;
    }

    /**
     * Gets the lag manager for performance monitoring.
     */
    public LagManager getLagManager() {
        return lagManager;
    }

    /**
     * Cleans up expired sessions.
     * Thread-safe with write lock for session removal.
     */
    public void cleanupExpiredSessions() {
        performanceMonitor.startOperation("cleanup-expired-sessions");

        try {
            // Run cleanup asynchronously to avoid blocking main thread
            AsyncUtils.runAsync(plugin, () -> {
                withWriteLock(() -> {
                    activeSessions.entrySet().removeIf(entry -> {
                        CombatSession session = entry.getValue();
                        if (session.isExpired()) {
                            // End combat synchronously as it needs to interact with main thread
                            AsyncUtils.runSync(plugin, () -> endCombat(entry.getKey()));
                            return true;
                        }
                        return false;
                    });
                });
            }, "cleanup-tasks");
        } finally {
            performanceMonitor.endOperation("cleanup-expired-sessions");
        }
    }
    /**
     * Reloads configuration values from the plugin config.
     * Called when /combat reload is executed.
     */
    public void reloadConfig() {
        this.defaultTimerSeconds = plugin.getConfig().getInt("combat.duration", 30);

        // Reload visual manager config
        if (visualManager != null) {
            visualManager.reloadConfig();
        }

        plugin.getLogger().info("CombatManager configuration reloaded (duration: " + defaultTimerSeconds + "s)");
    }

    /**
     * Merges weapon stats from a session into a player's global statistics.
     */
    private void mergeWeaponStats(Player player, CombatSession session) {
        com.muzlik.pvpcombat.data.PlayerCombatData globalData = combatTracker.getPlayerData(player.getUniqueId());
        Map<String, com.muzlik.pvpcombat.data.WeaponStats> sessionWeaponStats = session.getWeaponStats(player);

        for (Map.Entry<String, com.muzlik.pvpcombat.data.WeaponStats> entry : sessionWeaponStats.entrySet()) {
            String material = entry.getKey();
            com.muzlik.pvpcombat.data.WeaponStats sessionStats = entry.getValue();
            com.muzlik.pvpcombat.data.WeaponStats globalStats = globalData.getWeaponStats(material);

            globalStats.setUses(globalStats.getUses() + sessionStats.getUses());
            globalStats.setTotalDamage(globalStats.getTotalDamage() + sessionStats.getTotalDamage());
            globalStats.setKills(globalStats.getKills() + sessionStats.getKills());
            globalStats.setCriticalHits(globalStats.getCriticalHits() + sessionStats.getCriticalHits());
        }
    }

    /**
     * Gets the network sync manager for cross-server functionality.
     */
    public NetworkSyncManager getNetworkSyncManager() {
        return networkSyncManager;
    }
    
    /**
     * Checks if a player is currently in a safe zone.
     */
    private boolean isInSafeZone(Player player) {
        // Check if safezone protection is enabled
        if (!plugin.getConfig().getBoolean("restrictions.safezone.enabled", true)) {
            return false;
        }
        
        // Check if WorldGuard is available
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            return false;
        }
        
        try {
            // Get protected regions list
            java.util.List<String> protectedRegions = plugin.getConfig().getStringList("restrictions.safezone.protected-regions");
            if (protectedRegions.isEmpty()) {
                return false;
            }
            
            org.bukkit.Location location = player.getLocation();
            
            // Use reflection to check WorldGuard regions
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object worldGuard = worldGuardClass.getMethod("getInstance").invoke(null);
            Object platform = worldGuardClass.getMethod("getPlatform").invoke(worldGuard);
            Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);
            
            // Get BukkitAdapter
            Class<?> adapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object adaptedWorld = adapterClass.getMethod("adapt", org.bukkit.World.class).invoke(null, location.getWorld());
            
            // Get RegionManager
            Object regionManager = regionContainer.getClass().getMethod("get", 
                Class.forName("com.sk89q.worldedit.world.World")).invoke(regionContainer, adaptedWorld);
            
            if (regionManager != null) {
                // Check each protected region
                for (String regionName : protectedRegions) {
                    Object region = regionManager.getClass().getMethod("getRegion", String.class)
                        .invoke(regionManager, regionName);
                    
                    if (region != null) {
                        // Check if location is in region
                        Boolean contains = (Boolean) region.getClass().getMethod("contains", int.class, int.class, int.class)
                            .invoke(region, location.getBlockX(), location.getBlockY(), location.getBlockZ());
                        
                        if (contains != null && contains) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // WorldGuard not available or error checking - assume not in safezone
            plugin.getLogger().fine("Could not check safezone status: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Gets the combat tracker for statistics.
     */
    public CombatTracker getCombatTracker() {
        return combatTracker;
    }

    /**
     * Gets the disconnect tracker for handling combat logging.
     */
    public DisconnectTracker getDisconnectTracker() {
        return disconnectTracker;
    }

    /**
     * Cleanup method called on plugin disable.
     */
    public void cleanup() {
        // Cancel all active timers
        for (BukkitTask task : sessionTimers.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        sessionTimers.clear();
        
        // Cleanup disconnect tracker
        if (disconnectTracker != null) {
            disconnectTracker.cleanup();
        }
        
        // Clear all sessions
        activeSessions.clear();
    }
}