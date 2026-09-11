package com.muzlik.pvpcombat.logging;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import com.muzlik.pvpcombat.data.ReplayEvent;
import com.muzlik.pvpcombat.logging.CombatLogEntry.EventType;
import com.muzlik.pvpcombat.utils.AsyncUtils;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;

/**
 * Thread-safe combat logging system with async processing and configurable storage.
 */
public class CombatLogger {

    public enum StorageType {
        FILE,
        MEMORY,
        BOTH
    }

    public enum SummaryDelivery {
        CHAT,
        GUI,
        STORAGE,
        NONE
    }

    private final PvPCombatPlugin plugin;
    private final Logger logger;
    private final Map<UUID, List<CombatLogEntry>> memoryLogs;
    private final BlockingQueue<CombatLogEntry> logQueue;
    private final ScheduledExecutorService executor;
    private final Path logDirectory;
    private final SimpleDateFormat dateFormat;

    // Replay integration
    private CombatReplayManager replayManager;

    // Configuration
    private StorageType storageType;
    private SummaryDelivery summaryDelivery;
    private boolean enableDetailedLogging;
    private int logRetentionDays;
    private int maxMemoryEntries;

    public CombatLogger(PvPCombatPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.memoryLogs = new ConcurrentHashMap<>();
        this.logQueue = new LinkedBlockingQueue<>();
        this.executor = Executors.newScheduledThreadPool(2);

        this.logDirectory = Paths.get(plugin.getDataFolder().getAbsolutePath(), "combat_logs");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // Load configuration
        loadConfiguration();

        // Initialize replay manager
        initializeReplayManager();

        // Start async processing
        startAsyncProcessing();

        // Schedule cleanup task
        scheduleCleanupTask();

        logger.info("CombatLogger initialized with storage type: " + storageType);
    }

    /**
     * Loads configuration settings from plugin config.
     */
    private void loadConfiguration() {
        this.storageType = StorageType.valueOf(
            plugin.getConfig().getString("logging.storage.type", "BOTH").toUpperCase()
        );
        this.summaryDelivery = SummaryDelivery.valueOf(
            plugin.getConfig().getString("logging.summary.delivery", "CHAT").toUpperCase()
        );
        this.enableDetailedLogging = plugin.getConfig().getBoolean("logging.detailed.enabled", true);
        this.logRetentionDays = plugin.getConfig().getInt("logging.retention.days", 30);
        this.maxMemoryEntries = plugin.getConfig().getInt("logging.memory.max_entries", 10000);
    }

    /**
     * Initializes the replay manager if replay system is enabled.
     */
    private void initializeReplayManager() {
        boolean replayEnabled = plugin.getConfig().getBoolean("replay.enabled", false);
        if (replayEnabled) {
            this.replayManager = new CombatReplayManager(plugin);
            logger.info("Replay system initialized");
        } else {
            logger.info("Replay system is disabled");
        }
    }

    /**
     * Records a replay event if replay system is active.
     */
    private void recordReplayEvent(UUID sessionId, ReplayEvent event) {
        if (replayManager != null) {
            replayManager.recordEvent(sessionId, event);
        }
    }

    /**
     * Logs a combat event asynchronously.
     */
    public void logEvent(CombatLogEntry entry) {
        if (!enableDetailedLogging && entry.getEventType() != EventType.COMBAT_START &&
            entry.getEventType() != EventType.COMBAT_END) {
            return;
        }

        try {
            logQueue.put(entry);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Failed to queue combat log entry: " + e.getMessage());
        }
    }

    /**
     * Logs damage dealt.
     */
    public void logDamageDealt(UUID sessionId, Player attacker, Player defender, double damage,
                                boolean hitLanded, double distance, String weaponType) {
        CombatLogEntry entry = new CombatLogEntry.Builder()
            .sessionId(sessionId)
            .playerId(attacker.getUniqueId())
            .targetId(defender.getUniqueId())
            .eventType(hitLanded ? EventType.HIT_LANDED : EventType.HIT_MISSED)
            .damage(damage)
            .hitLanded(hitLanded)
            .distance(distance)
            .weaponType(weaponType)
            .location(String.format("%.1f,%.1f,%.1f", attacker.getLocation().getX(),
                                  attacker.getLocation().getY(), attacker.getLocation().getZ()))
            .additionalData(String.format("Target: %s, Weapon: %s", defender.getName(), weaponType))
            .build();

        logEvent(entry);

        // Record for replay system
        if (replayManager != null) {
            ReplayEvent.ReplayEventType replayType = hitLanded ?
                ReplayEvent.ReplayEventType.HIT_LANDED : ReplayEvent.ReplayEventType.HIT_MISSED;
            ReplayEvent replayEvent = new ReplayEvent.Builder()
                .playerId(attacker.getUniqueId())
                .targetId(defender.getUniqueId())
                .eventType(replayType)
                .damage(damage)
                .location(entry.getLocation())
                .weaponType(weaponType)
                .build();
            recordReplayEvent(sessionId, replayEvent);
        }
    }

    /**
     * Logs combat start.
     */
    public void logCombatStart(UUID sessionId, Player attacker, Player defender) {
        CombatLogEntry startEntry = new CombatLogEntry.Builder()
            .sessionId(sessionId)
            .playerId(attacker.getUniqueId())
            .targetId(defender.getUniqueId())
            .eventType(EventType.COMBAT_START)
            .additionalData(String.format("Started combat with %s", defender.getName()))
            .build();

        logEvent(startEntry);
    }

    /**
     * Logs combat end.
     */
    public void logCombatEnd(UUID sessionId, Player player1, Player player2, String reason) {
        CombatLogEntry endEntry = new CombatLogEntry.Builder()
            .sessionId(sessionId)
            .playerId(player1.getUniqueId())
            .eventType(EventType.COMBAT_END)
            .additionalData(String.format("Combat ended: %s (Opponent: %s)", reason, player2.getName()))
            .build();

        logEvent(endEntry);

        // Record for replay system
        if (replayManager != null) {
            ReplayEvent replayEvent = new ReplayEvent.Builder()
                .playerId(player1.getUniqueId())
                .targetId(player2.getUniqueId())
                .eventType(ReplayEvent.ReplayEventType.COMBAT_END)
                .additionalData(reason)
                .build();
            recordReplayEvent(sessionId, replayEvent);
        }
    }

    /**
     * Generates and delivers a combat summary.
     */
    public void generateSummary(UUID sessionId, Player player, com.muzlik.pvpcombat.data.CombatSession session) {
        // Re-read configuration in case it was reloaded
        loadConfiguration();

        if (summaryDelivery == SummaryDelivery.NONE) {
            return;
        }

        AsyncUtils.runAsync(plugin, () -> {
            List<CombatLogEntry> sessionLogs = getSessionLogs(sessionId);
            CombatSummary summary;
            
            if (session != null) {
                // Create summary with session damage data
                summary = new CombatSummary(sessionLogs, session, player);
            } else {
                // Fallback to log-based summary
                summary = new CombatSummary(sessionLogs);
            }

            AsyncUtils.runSync(plugin, () -> {
                switch (summaryDelivery) {
                    case CHAT:
                        sendChatSummary(player, summary);
                        break;
                    case GUI:
                        openCombatSummaryGUI(player, summary);
                        break;
                    case STORAGE:
                        // Summary stored for admin access
                        logger.info("Combat summary stored for session " + sessionId);
                        break;
                }
            });
        });
    }

    /**
     * Gets all logs for a combat session.
     */
    public List<CombatLogEntry> getSessionLogs(UUID sessionId) {
        List<CombatLogEntry> sessionLogs = new ArrayList<>();

        // Get from memory
        if (storageType == StorageType.MEMORY || storageType == StorageType.BOTH) {
            List<CombatLogEntry> entries = memoryLogs.get(sessionId);
            if (entries != null) {
                sessionLogs.addAll(entries);
            }
        }

        // Get from files if needed
        if (storageType == StorageType.FILE || storageType == StorageType.BOTH) {
            sessionLogs.addAll(loadSessionLogsFromFile(sessionId));
        }

        if (!sessionLogs.isEmpty()) {
            sessionLogs.sort(Comparator.comparing(CombatLogEntry::getTimestamp));
        }
        
        return sessionLogs;
    }

    /**
     * Starts async log processing.
     */
    private void startAsyncProcessing() {
        executor.scheduleWithFixedDelay(() -> {
            List<CombatLogEntry> batch = new ArrayList<>();
            logQueue.drainTo(batch, 100); // Process in batches

            for (CombatLogEntry entry : batch) {
                processLogEntry(entry);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Processes a single log entry.
     */
    private void processLogEntry(CombatLogEntry entry) {
        // Store in memory
        if (storageType == StorageType.MEMORY || storageType == StorageType.BOTH) {
            memoryLogs.computeIfAbsent(entry.getSessionId(), k -> new CopyOnWriteArrayList<>())
                     .add(entry);

            // Cleanup old memory entries
            cleanupMemoryLogs();
        }

        // Store in file
        if (storageType == StorageType.FILE || storageType == StorageType.BOTH) {
            writeToFile(entry);
        }
    }

    /**
     * Writes log entry to file.
     */
    private void writeToFile(CombatLogEntry entry) {
        try {
            Files.createDirectories(logDirectory);
            String dateStr = dateFormat.format(java.sql.Date.valueOf(entry.getTimestamp().toLocalDate()));
            Path logFile = logDirectory.resolve("combat_" + dateStr + ".log");

            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile.toFile(), true))) {
                writer.println(formatLogEntry(entry));
            }
        } catch (IOException e) {
            logger.warning("Failed to write combat log entry to file: " + e.getMessage());
        }
    }

    /**
     * Formats log entry for file storage.
     */
    private String formatLogEntry(CombatLogEntry entry) {
        return String.format("%s [%s] Session:%s Player:%s Target:%s Type:%s Damage:%.2f Hit:%s Distance:%.2f Weapon:%s Location:%s Data:%s",
            entry.getTimestamp(),
            entry.getSessionId(),
            entry.getPlayerId(),
            entry.getTargetId() != null ? entry.getTargetId() : "N/A",
            entry.getEventType(),
            entry.getDamage(),
            entry.isHitLanded(),
            entry.getDistance(),
            entry.getWeaponType(),
            entry.getLocation(),
            entry.getAdditionalData()
        );
    }

    /**
     * Loads session logs from file storage.
     */
    private List<CombatLogEntry> loadSessionLogsFromFile(UUID sessionId) {
        List<CombatLogEntry> entries = new ArrayList<>();

        try {
            Files.walk(logDirectory)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".log"))
                .forEach(file -> {
                    try {
                        List<String> lines = Files.readAllLines(file);
                        for (String line : lines) {
                            if (line.contains("[" + sessionId + "]")) {
                                // Parse and add entry (simplified for now)
                                // In production, implement proper parsing
                            }
                        }
                    } catch (IOException e) {
                        logger.warning("Failed to read combat log file: " + file);
                    }
                });
        } catch (IOException e) {
            logger.warning("Failed to load session logs from files: " + e.getMessage());
        }

        return entries;
    }

    /**
     * Cleans up old memory log entries.
     */
    private void cleanupMemoryLogs() {
        int totalEntries = memoryLogs.values().stream().mapToInt(List::size).sum();
        if (totalEntries > maxMemoryEntries) {
            // Remove oldest sessions (simplified cleanup)
            UUID oldestSession = memoryLogs.entrySet().stream()
                .min(Comparator.comparing(e -> e.getValue().isEmpty() ? LocalDateTime.MAX :
                    e.getValue().get(0).getTimestamp()))
                .map(Map.Entry::getKey)
                .orElse(null);

            if (oldestSession != null) {
                memoryLogs.remove(oldestSession);
            }
        }
    }

    /**
     * Schedules periodic cleanup of old log files.
     */
    private void scheduleCleanupTask() {
        executor.scheduleAtFixedRate(() -> {
            try {
                cleanupOldLogFiles();
            } catch (Exception e) {
                logger.warning("Failed to cleanup old log files: " + e.getMessage());
            }
        }, 1, 24, TimeUnit.HOURS);
    }

    /**
     * Cleans up old log files based on retention policy.
     */
    private void cleanupOldLogFiles() throws IOException {
        if (!Files.exists(logDirectory)) return;

        LocalDateTime cutoff = LocalDateTime.now().minusDays(logRetentionDays);
        Files.walk(logDirectory)
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".log"))
            .forEach(file -> {
                try {
                    String filename = file.getFileName().toString();
                    String dateStr = filename.replace("combat_", "").replace(".log", "");
                    LocalDateTime fileDate = LocalDateTime.parse(dateStr + "T00:00:00");

                    if (fileDate.isBefore(cutoff)) {
                        Files.delete(file);
                        logger.info("Deleted old combat log file: " + filename);
                    }
                } catch (Exception e) {
                    logger.warning("Failed to process log file for cleanup: " + file);
                }
            });
    }

    /**
     * Sends combat summary via chat.
     */
    private void sendChatSummary(Player player, CombatSummary summary) {
        // Get the actual combat data from tracker for accurate per-session stats
        // Note: We need to get the damage BEFORE it's added to cumulative totals
        // So we use the session data from the summary
        player.sendMessage("§6=== Combat Summary ===");
        player.sendMessage(String.format("§eHits Landed: §f%d/%d (%.1f%%)",
            summary.getHitsLanded(), summary.getTotalAttacks(), summary.getAccuracy()));
        player.sendMessage(String.format("§eDamage Dealt: §c%.1f ❤", summary.getTotalDamageDealt()));
        player.sendMessage(String.format("§eDamage Received: §c%.1f ❤", summary.getTotalDamageReceived()));
        player.sendMessage(String.format("§eKnockback Exchanges: §f%d", summary.getKnockbackExchanges()));
        player.sendMessage(String.format("§eCombat Duration: §f%s", summary.getFormattedDuration()));
    }

    /**
     * Shuts down the logger.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Shutdown replay manager
        if (replayManager != null) {
            replayManager.shutdown();
        }
    }

    /**
     * Gets replay data for admin viewing.
     */
    public String getReplayTimelineJson(UUID sessionId, Player requester) {
        if (replayManager != null) {
            return replayManager.getReplayTimelineJson(sessionId, requester);
        }
        return "{\"error\": \"Replay system not available\"}";
    }

    /**
     * Gets replay manager for admin access.
     */
    public CombatReplayManager getReplayManager() {
        return replayManager;
    }

    // Getters for configuration
    public StorageType getStorageType() { return storageType; }
    public SummaryDelivery getSummaryDelivery() { return summaryDelivery; }
    public boolean isDetailedLoggingEnabled() { return enableDetailedLogging; }

    /**
     * Opens basic combat summary GUI.
     */
    private void openCombatSummaryGUI(Player player, CombatSummary summary) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Combat Summary");

        // Hits Landed
        ItemStack hitsItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta hitsMeta = hitsItem.getItemMeta();
        hitsMeta.setDisplayName(ChatColor.GREEN + "Hits Landed");
        hitsMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Hits: " + summary.getHitsLanded(),
            ChatColor.GRAY + "Attacks: " + summary.getTotalAttacks(),
            ChatColor.YELLOW + "Accuracy: " + String.format("%.1f%%", summary.getAccuracy())
        ));
        hitsMeta.addEnchant(Enchantment.DURABILITY, 1, true);
        hitsItem.setItemMeta(hitsMeta);
        inv.setItem(10, hitsItem);

        // Damage Dealt
        ItemStack dmgDealt = new ItemStack(Material.GOLD_INGOT);
        ItemMeta dmgMeta = dmgDealt.getItemMeta();
        dmgMeta.setDisplayName(ChatColor.GOLD + "Damage Dealt");
        dmgMeta.setLore(Arrays.asList(ChatColor.GRAY + String.format("%.1f", summary.getTotalDamageDealt())));
        dmgDealt.setItemMeta(dmgMeta);
        inv.setItem(12, dmgDealt);

        // Damage Received
        ItemStack dmgRecv = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta recvMeta = dmgRecv.getItemMeta();
        recvMeta.setDisplayName(ChatColor.RED + "Damage Received");
        recvMeta.setLore(Arrays.asList(ChatColor.GRAY + String.format("%.1f", summary.getTotalDamageReceived())));
        dmgRecv.setItemMeta(recvMeta);
        inv.setItem(14, dmgRecv);

        // Duration
        ItemStack durationItem = new ItemStack(Material.CLOCK);
        ItemMeta durMeta = durationItem.getItemMeta();
        durMeta.setDisplayName(ChatColor.AQUA + "Combat Duration");
        durMeta.setLore(Arrays.asList(ChatColor.GRAY + summary.getFormattedDuration()));
        durationItem.setItemMeta(durMeta);
        inv.setItem(16, durationItem);

        player.openInventory(inv);
    }
}