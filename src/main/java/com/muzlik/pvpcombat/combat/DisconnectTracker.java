package com.muzlik.pvpcombat.combat;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players who disconnect during combat to prevent abuse.
 * Only punishes if they don't reconnect before combat timer expires.
 */
public class DisconnectTracker {

    private final PvPCombatPlugin plugin;
    private final CombatManager combatManager;
    
    // Tracks disconnected players and their remaining combat time
    private final Map<UUID, DisconnectData> disconnectedPlayers;
    
    // Tracks grace period display tasks for opponents
    private final Map<UUID, BukkitTask> displayTasks;
    
    // Tracks pending punishments (in memory, not in config)
    private final Map<UUID, Boolean> pendingPunishments;
    
    public DisconnectTracker(PvPCombatPlugin plugin, CombatManager combatManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.disconnectedPlayers = new ConcurrentHashMap<>();
        this.displayTasks = new ConcurrentHashMap<>();
        this.pendingPunishments = new ConcurrentHashMap<>();
    }
    
    /**
     * Called when a player disconnects during combat.
     * Starts tracking them instead of instantly punishing.
     */
    public void onPlayerDisconnect(Player player, Player opponent, int remainingCombatTime) {
        UUID playerId = player.getUniqueId();
        
        plugin.getLogger().info(String.format("[DISCONNECT] %s disconnected during combat with %s. Tracking for %d seconds.",
            player.getName(), opponent.getName(), remainingCombatTime));
        
        // Capture player's inventory and location before they disconnect
        org.bukkit.Location location = player.getLocation().clone();
        org.bukkit.inventory.ItemStack[] inventory = player.getInventory().getContents().clone();
        org.bukkit.inventory.ItemStack[] armor = player.getInventory().getArmorContents().clone();
        
        // Create disconnect data
        DisconnectData data = new DisconnectData(
            playerId,
            player.getName(),
            opponent.getUniqueId(),
            opponent.getName(),
            remainingCombatTime,
            System.currentTimeMillis(),
            location,
            inventory,
            armor
        );
        
        disconnectedPlayers.put(playerId, data);
        
        // Start grace period display for opponent
        if (opponent != null && opponent.isOnline()) {
            startGracePeriodDisplay(opponent, player.getName(), remainingCombatTime);
        }
        
        // Start punishment timer - only punish if they don't reconnect in time
        BukkitTask punishmentTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if player is still offline
                if (disconnectedPlayers.containsKey(playerId)) {
                    // Player didn't reconnect in time - apply punishment
                    applyPunishment(data);
                    disconnectedPlayers.remove(playerId);
                    
                    // Stop grace period display for opponent
                    if (opponent != null && opponent.isOnline()) {
                        stopGracePeriodDisplay(opponent.getUniqueId());
                    }
                }
            }
        }.runTaskLater(plugin, remainingCombatTime * 20L); // Convert seconds to ticks
        
        data.setPunishmentTask(punishmentTask);
    }
    
    /**
     * Called when a player reconnects.
     * Checks if they were tracked and handles accordingly.
     */
    public boolean onPlayerReconnect(Player player) {
        UUID playerId = player.getUniqueId();
        DisconnectData data = disconnectedPlayers.remove(playerId);
        
        if (data != null) {
            // Cancel punishment task
            if (data.getPunishmentTask() != null) {
                data.getPunishmentTask().cancel();
            }
            
            long disconnectDuration = (System.currentTimeMillis() - data.getDisconnectTime()) / 1000;
            
            plugin.getLogger().info(String.format("[RECONNECT] %s reconnected after %d seconds. No punishment applied.",
                player.getName(), disconnectDuration));
            
            // Get message from config
            String message = plugin.getConfig().getString("combat.disconnect-protection.reconnect-success-message",
                "&aYou reconnected in time! No combat logging penalty applied.");
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            
            // Notify opponent and stop grace period display
            Player opponent = plugin.getServer().getPlayer(data.getOpponentId());
            if (opponent != null && opponent.isOnline()) {
                opponent.sendMessage(String.format("§e%s reconnected to the server.", player.getName()));
                stopGracePeriodDisplay(opponent.getUniqueId());
                
                // Restart combat between them
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    combatManager.startCombat(player, opponent);
                });
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Applies punishment to a player who didn't reconnect in time.
     */
    private void applyPunishment(DisconnectData data) {
        plugin.getLogger().info(String.format("[PUNISHMENT] %s did not reconnect in time. Applying combat log penalty.",
            data.getPlayerName()));
        
        // Record loss for the disconnected player
        combatManager.getCombatTracker().recordLossByUUID(data.getPlayerId());
        
        // Record win for opponent
        combatManager.getCombatTracker().recordWinByUUID(data.getOpponentId());
        
        // Drop inventory at disconnect location immediately
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.Location dropLocation = data.getDisconnectLocation();
            org.bukkit.World world = dropLocation.getWorld();
            
            if (world != null) {
                // Drop all inventory items
                for (org.bukkit.inventory.ItemStack item : data.getInventory()) {
                    if (item != null && item.getType() != org.bukkit.Material.AIR) {
                        world.dropItemNaturally(dropLocation, item);
                    }
                }
                
                // Drop all armor items
                for (org.bukkit.inventory.ItemStack item : data.getArmor()) {
                    if (item != null && item.getType() != org.bukkit.Material.AIR) {
                        world.dropItemNaturally(dropLocation, item);
                    }
                }
                
                plugin.getLogger().info(String.format("[PUNISHMENT] Dropped inventory for %s at %s",
                    data.getPlayerName(), dropLocation.toString()));
            }
        });
        
        // Notify opponent if online
        Player opponent = plugin.getServer().getPlayer(data.getOpponentId());
        if (opponent != null && opponent.isOnline()) {
            opponent.sendMessage(String.format("§aYou won! §e%s §acombat logged and was punished. Their items have been dropped!", data.getPlayerName()));
            combatManager.getCombatTracker().recordWin(opponent);
        }
        
        // Broadcast message from config
        String broadcastMessage = plugin.getConfig().getString("combat.disconnect-protection.punishment-broadcast",
            "&c{player} &ecombat logged and was punished for not reconnecting in time!");
        broadcastMessage = org.bukkit.ChatColor.translateAlternateColorCodes('&', 
            broadcastMessage.replace("{player}", data.getPlayerName()));
        plugin.getServer().broadcastMessage(broadcastMessage);
        
        // Store punishment in memory (not in config to avoid corruption)
        pendingPunishments.put(data.getPlayerId(), true);
        
        plugin.getLogger().info(String.format("[PUNISHMENT] Combat log penalty applied to %s. Opponent %s awarded win. Items dropped at disconnect location.",
            data.getPlayerName(), data.getOpponentName()));
    }
    
    /**
     * Starts displaying grace period countdown to opponent.
     */
    private void startGracePeriodDisplay(Player opponent, String disconnectedPlayerName, int totalSeconds) {
        String displayMode = plugin.getConfig().getString("combat.disconnect-protection.display-mode", "actionbar");
        
        BukkitTask displayTask = new BukkitRunnable() {
            int remainingSeconds = totalSeconds;
            
            @Override
            public void run() {
                if (!opponent.isOnline() || remainingSeconds <= 0) {
                    cancel();
                    displayTasks.remove(opponent.getUniqueId());
                    return;
                }
                
                String message = plugin.getConfig().getString("combat.disconnect-protection.grace-period-format",
                    "&e{player} &7has &c{time}s &7to reconnect")
                    .replace("{player}", disconnectedPlayerName)
                    .replace("{time}", String.valueOf(remainingSeconds));
                message = org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
                
                switch (displayMode.toLowerCase()) {
                    case "bossbar":
                        displayBossBar(opponent, message, remainingSeconds, totalSeconds);
                        break;
                    case "scoreboard":
                        displayScoreboard(opponent, message, remainingSeconds);
                        break;
                    case "actionbar":
                    default:
                        opponent.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(message));
                        break;
                }
                
                remainingSeconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        
        displayTasks.put(opponent.getUniqueId(), displayTask);
    }
    
    // Store boss bars for each player
    private final Map<UUID, org.bukkit.boss.BossBar> bossBars = new ConcurrentHashMap<>();
    
    /**
     * Stops grace period display for a player.
     */
    private void stopGracePeriodDisplay(UUID playerId) {
        BukkitTask task = displayTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        // Clear bossbar if using that mode
        org.bukkit.boss.BossBar bossBar = bossBars.remove(playerId);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }
    
    /**
     * Displays grace period on boss bar.
     */
    private void displayBossBar(Player player, String message, int remaining, int total) {
        org.bukkit.boss.BossBar bossBar = bossBars.computeIfAbsent(player.getUniqueId(), 
            k -> org.bukkit.Bukkit.createBossBar(
                message,
                org.bukkit.boss.BarColor.YELLOW,
                org.bukkit.boss.BarStyle.SOLID
            ));
        
        bossBar.setTitle(message);
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, (double) remaining / total)));
        
        if (!bossBar.getPlayers().contains(player)) {
            bossBar.addPlayer(player);
        }
    }
    
    /**
     * Displays grace period on scoreboard.
     */
    private void displayScoreboard(Player player, String message, int remaining) {
        // Send as title for visibility
        player.sendTitle(
            org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eGrace Period"),
            message,
            10, 20, 10
        );
    }
    
    /**
     * Checks if a player is currently tracked as disconnected.
     */
    public boolean isTracked(UUID playerId) {
        return disconnectedPlayers.containsKey(playerId);
    }
    
    /**
     * Gets disconnect data for a player.
     */
    public DisconnectData getDisconnectData(UUID playerId) {
        return disconnectedPlayers.get(playerId);
    }
    
    /**
     * Clears tracking for a player (used when combat ends normally).
     */
    public void clearTracking(UUID playerId) {
        DisconnectData data = disconnectedPlayers.remove(playerId);
        if (data != null && data.getPunishmentTask() != null) {
            data.getPunishmentTask().cancel();
        }
    }
    
    /**
     * Cleans up all tracking data (called on plugin disable).
     */
    public void cleanup() {
        for (DisconnectData data : disconnectedPlayers.values()) {
            if (data.getPunishmentTask() != null) {
                data.getPunishmentTask().cancel();
            }
        }
        disconnectedPlayers.clear();
        
        // Cancel all display tasks
        for (BukkitTask task : displayTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        displayTasks.clear();
        
        // Remove all boss bars
        for (org.bukkit.boss.BossBar bossBar : bossBars.values()) {
            if (bossBar != null) {
                bossBar.removeAll();
            }
        }
        bossBars.clear();
        
        // Clear pending punishments
        pendingPunishments.clear();
    }
    
    /**
     * Checks if a player has a pending punishment.
     */
    public boolean hasPendingPunishment(UUID playerId) {
        return pendingPunishments.containsKey(playerId);
    }
    
    /**
     * Applies pending punishment to a player who just logged in.
     */
    public void applyPendingPunishment(Player player) {
        UUID playerId = player.getUniqueId();
        if (hasPendingPunishment(playerId)) {
            // Clear the pending punishment from memory
            pendingPunishments.remove(playerId);
            
            // Kill the player
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                player.setHealth(0.0);
                player.sendMessage("§cYou were killed for combat logging!");
                plugin.getLogger().info(String.format("[PUNISHMENT] Applied pending punishment to %s (killed on login)",
                    player.getName()));
            });
        }
    }
    
    /**
     * Data class to track disconnected players.
     */
    public static class DisconnectData {
        private final UUID playerId;
        private final String playerName;
        private final UUID opponentId;
        private final String opponentName;
        private final int remainingCombatTime;
        private final long disconnectTime;
        private final org.bukkit.Location disconnectLocation;
        private final org.bukkit.inventory.ItemStack[] inventory;
        private final org.bukkit.inventory.ItemStack[] armor;
        private BukkitTask punishmentTask;
        
        public DisconnectData(UUID playerId, String playerName, UUID opponentId, String opponentName,
                            int remainingCombatTime, long disconnectTime, org.bukkit.Location location,
                            org.bukkit.inventory.ItemStack[] inventory, org.bukkit.inventory.ItemStack[] armor) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.opponentId = opponentId;
            this.opponentName = opponentName;
            this.remainingCombatTime = remainingCombatTime;
            this.disconnectTime = disconnectTime;
            this.disconnectLocation = location;
            this.inventory = inventory;
            this.armor = armor;
        }
        
        public UUID getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public UUID getOpponentId() { return opponentId; }
        public String getOpponentName() { return opponentName; }
        public int getRemainingCombatTime() { return remainingCombatTime; }
        public long getDisconnectTime() { return disconnectTime; }
        public org.bukkit.Location getDisconnectLocation() { return disconnectLocation; }
        public org.bukkit.inventory.ItemStack[] getInventory() { return inventory; }
        public org.bukkit.inventory.ItemStack[] getArmor() { return armor; }
        public BukkitTask getPunishmentTask() { return punishmentTask; }
        public void setPunishmentTask(BukkitTask task) { this.punishmentTask = task; }
    }
}
