package com.muzlik.pvpcombat.combat;

import com.muzlik.pvpcombat.config.DisconnectConfig;
import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players who disconnect during combat and applies appropriate grace periods.
 *
 * <h3>Two disconnect types</h3>
 * <ul>
 *   <li><b>BAD_INTERNET</b> – connection timed out / kicked for "flying" / network drop.
 *       Detected by the quit reason containing "timed out", "lost connection", or similar.
 *       Gets a longer, more lenient grace period.</li>
 *   <li><b>INTENTIONAL</b> – player pressed Disconnect or closed the game window.
 *       Gets a shorter grace period.</li>
 * </ul>
 *
 * <h3>Repeat-logout abuse prevention</h3>
 * If a player disconnects more than {@code max-count} times within
 * {@code window-seconds}, they are killed immediately on the next logout
 * (no grace period granted at all).
 *
 * <h3>Reload support</h3>
 * Call {@link #reloadConfig()} after the plugin config has been refreshed.
 * All in-flight timers keep their original grace period; only new disconnects
 * use the updated values.
 */
public class DisconnectTracker {

    /** Categorises why a player disconnected. */
    public enum DisconnectType {
        BAD_INTERNET,
        INTENTIONAL
    }

    // ── Dependencies ──────────────────────────────────────────────────────
    private final PvPCombatPlugin plugin;
    private final CombatManager   combatManager;

    // ── Live config (refreshed on reload) ─────────────────────────────────
    private volatile DisconnectConfig cfg;

    // ── State maps ────────────────────────────────────────────────────────
    /** Players currently in a grace period (offline, waiting to reconnect). */
    private final Map<UUID, DisconnectData>  disconnectedPlayers = new ConcurrentHashMap<>();

    /** Active countdown display tasks keyed by the *opponent's* UUID. */
    private final Map<UUID, BukkitTask>      displayTasks        = new ConcurrentHashMap<>();

    /** Boss-bars shown to opponents during a grace period. */
    private final Map<UUID, org.bukkit.boss.BossBar> bossBars    = new ConcurrentHashMap<>();

    /**
     * Pending punishments for players who did not reconnect in time.
     * Stored in memory; applied when the player next logs in.
     * Key = offending player UUID, Value = opponent UUID (for win credit).
     */
    private final Map<UUID, UUID> pendingPunishments = new ConcurrentHashMap<>();

    /**
     * Snapshot data for pending punishments so we can drop the correct inventory on login.
     * Key = offending player UUID, Value = their DisconnectData at punishment time.
     */
    private final Map<UUID, DisconnectData> pendingPunishmentData = new ConcurrentHashMap<>();

    /**
     * Rolling disconnect history per player.
     * Used to detect repeat-logout abuse.
     * Each entry is the epoch-millisecond timestamp of a disconnect.
     */
    private final Map<UUID, Deque<Long>> disconnectHistory = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────
    public DisconnectTracker(PvPCombatPlugin plugin, CombatManager combatManager) {
        this.plugin        = plugin;
        this.combatManager = combatManager;
        // cfg is resolved lazily on first use via cfg()
    }

    // ── Config helpers ────────────────────────────────────────────────────

    /**
     * Returns the live config, resolving it lazily on first access and on every
     * call so that it is always up-to-date after a reload.
     * Never returns null — falls back to hardcoded defaults.
     */
    private DisconnectConfig cfg() {
        // Always re-resolve so reloads are picked up automatically
        if (plugin.getConfigManager() instanceof com.muzlik.pvpcombat.config.ConfigManager) {
            com.muzlik.pvpcombat.config.ConfigManager cm =
                (com.muzlik.pvpcombat.config.ConfigManager) plugin.getConfigManager();
            DisconnectConfig dc = cm.getDisconnectConfig();
            if (dc != null) {
                this.cfg = dc;
                return dc;
            }
        }
        // ConfigManager not ready yet — return cached or fallback
        if (cfg != null) return cfg;
        cfg = buildFallbackConfig();
        return cfg;
    }

    private DisconnectConfig buildFallbackConfig() {
        // Hard-coded safe defaults used only before ConfigManager finishes loading.
        return new DisconnectConfig(
            new com.muzlik.pvpcombat.config.ConfigurationValidator(),
            plugin.getConfig()
        );
    }

    /** Called by ConfigManager.reloadConfig() after files are re-read. */
    public void reloadConfig() {
        this.cfg = null; // force re-resolve on next cfg() call
        plugin.getLogger().info("[DisconnectTracker] Config will be re-resolved on next use.");
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Called from {@code CombatEventListener.onPlayerQuit} when a player in combat disconnects.
     *
     * @param player            the disconnecting player
     * @param opponent          their combat opponent (may be null if session already ended)
     * @param remainingCombatTime remaining seconds on the combat timer
     * @param type              whether this was a bad-internet or intentional disconnect
     */
    public void onPlayerDisconnect(Player player, Player opponent,
                                   int remainingCombatTime, DisconnectType type) {
        if (!cfg().isEnabled()) {
            // Protection disabled – instant punishment path handled by caller
            return;
        }

        UUID playerId = player.getUniqueId();

        // ── Repeat-logout check ───────────────────────────────────────────
        if (cfg().isRepeatLogoutEnabled() && isRepeatOffender(playerId)) {
            plugin.getLogger().info(String.format(
                "[DISCONNECT] %s is a repeat offender – scheduling immediate kill.", player.getName()));

            recordDisconnectHistory(playerId);

            UUID opponentId = opponent != null ? opponent.getUniqueId() : null;

            // Snapshot inventory for the pending punishment
            org.bukkit.Location location  = player.getLocation().clone();
            org.bukkit.inventory.ItemStack[] inventory = player.getInventory().getStorageContents().clone();
            org.bukkit.inventory.ItemStack[] armor     = player.getInventory().getArmorContents().clone();

            DisconnectData repeatData = new DisconnectData(
                playerId, player.getName(), opponentId,
                opponent != null ? opponent.getName() : "Unknown",
                0, 0, type, System.currentTimeMillis(),
                location, inventory, armor
            );

            if (opponentId != null) {
                pendingPunishments.put(playerId, opponentId);
                pendingPunishmentData.put(playerId, repeatData);
                combatManager.getCombatTracker().recordWinByUUID(opponentId);
                combatManager.getCombatTracker().recordLossByUUID(playerId);
            }

            // Notify opponent
            if (opponent != null && opponent.isOnline()) {
                opponent.sendMessage(color(cfg().getRepeatLogoutOpponentMessage()
                    .replace("{player}", player.getName())));
            }

            broadcastPunishment(player.getName());
            return;
        }

        // ── Determine grace period ────────────────────────────────────────
        int graceSeconds;
        boolean graceEnabled;
        if (type == DisconnectType.BAD_INTERNET) {
            graceEnabled  = cfg().isBadInternetEnabled();
            graceSeconds  = cfg().getBadInternetGraceSeconds();
        } else {
            graceEnabled  = cfg().isIntentionalEnabled();
            graceSeconds  = cfg().getIntentionalGraceSeconds();
        }

        // Record this disconnect in the rolling history
        recordDisconnectHistory(playerId);

        if (!graceEnabled || graceSeconds <= 0) {
            // Grace disabled for this type – instant punishment
            applyImmediatePunishment(playerId, player.getName(),
                opponent != null ? opponent.getUniqueId() : null,
                opponent != null ? opponent.getName() : "Unknown",
                player.getLocation().clone(),
                player.getInventory().getStorageContents().clone(),
                player.getInventory().getArmorContents().clone());
            return;
        }

        plugin.getLogger().info(String.format(
            "[DISCONNECT] %s (%s) disconnected vs %s. Grace: %ds.",
            player.getName(), type.name(),
            opponent != null ? opponent.getName() : "none",
            graceSeconds));

        // Snapshot inventory before the player object becomes invalid
        org.bukkit.Location location  = player.getLocation().clone();
        org.bukkit.inventory.ItemStack[] inventory = player.getInventory().getStorageContents().clone();
        org.bukkit.inventory.ItemStack[] armor     = player.getInventory().getArmorContents().clone();

        UUID opponentId   = opponent != null ? opponent.getUniqueId() : null;
        String opponentName = opponent != null ? opponent.getName() : "Unknown";

        DisconnectData data = new DisconnectData(
            playerId, player.getName(),
            opponentId, opponentName,
            remainingCombatTime, graceSeconds,
            type, System.currentTimeMillis(),
            location, inventory, armor
        );
        disconnectedPlayers.put(playerId, data);

        // Notify opponent
        if (opponent != null && opponent.isOnline()) {
            String template = type == DisconnectType.BAD_INTERNET
                ? cfg().getBadInternetDisconnectMessage()
                : cfg().getIntentionalDisconnectMessage();
            opponent.sendMessage(color(template
                .replace("{player}", player.getName())
                .replace("{time}", String.valueOf(graceSeconds))));

            startGracePeriodDisplay(opponent, player.getName(), graceSeconds, type);
        }

        // Schedule punishment if player doesn't reconnect in time
        BukkitTask punishTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (disconnectedPlayers.containsKey(playerId)) {
                    applyPunishment(data);
                    disconnectedPlayers.remove(playerId);
                    stopGracePeriodDisplay(opponentId);
                    // Fully end the opponent's session now that grace period is over
                    if (opponentId != null) {
                        combatManager.endCombatAfterGracePeriod(opponentId);
                    }
                }
            }
        }.runTaskLater(plugin, graceSeconds * 20L);

        data.setPunishmentTask(punishTask);
    }

    /**
     * Called from {@code CombatEventListener.onPlayerJoin} when a player logs in.
     *
     * @return true if the player was being tracked (was in a grace period)
     */
    public boolean onPlayerReconnect(Player player) {
        UUID playerId = player.getUniqueId();
        DisconnectData data = disconnectedPlayers.remove(playerId);

        if (data == null) return false;

        // Cancel the punishment timer
        if (data.getPunishmentTask() != null) {
            data.getPunishmentTask().cancel();
        }

        long offlineMs = System.currentTimeMillis() - data.getDisconnectTime();
        plugin.getLogger().info(String.format(
            "[RECONNECT] %s reconnected after %ds. No punishment.",
            player.getName(), offlineMs / 1000));

        // Send reconnect message
        String msg = data.getType() == DisconnectType.BAD_INTERNET
            ? cfg().getBadInternetReconnectMessage()
            : cfg().getIntentionalReconnectMessage();
        player.sendMessage(color(msg));

        // Notify opponent and restart combat
        if (data.getOpponentId() != null) {
            Player opponent = plugin.getServer().getPlayer(data.getOpponentId());
            if (opponent != null && opponent.isOnline()) {
                opponent.sendMessage(color("&e" + player.getName() + " &areconnected! Combat resuming..."));
                stopGracePeriodDisplay(opponent.getUniqueId());

                // Silently clean up the opponent's lingering session entry so startCombat
                // can create a fresh one. We do NOT call endCombat() here because that
                // would send a combat summary and clear the opponent's visuals.
                combatManager.silentlyRemovePlayer(data.getOpponentId());

                // Restart combat on the main thread (1-tick delay to let the silent
                // removal complete before startCombat checks isInCombat)
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () ->
                    combatManager.startCombat(player, opponent), 1L);
            } else {
                // Opponent went offline during grace period — clean up their session entry
                combatManager.silentlyRemovePlayer(data.getOpponentId());
            }
        }

        return true;
    }

    /**
     * Called from {@code CombatEventListener.onPlayerJoin} to apply a pending kill punishment.
     *
     * The inventory was already dropped at grace-period expiry.
     * Here we only clear the live inventory (so death drop is empty) and kill the player.
     */
    public void applyPendingPunishment(Player player) {
        UUID playerId = player.getUniqueId();
        if (!pendingPunishments.containsKey(playerId)) return;

        UUID opponentId = pendingPunishments.remove(playerId);
        pendingPunishmentData.remove(playerId); // clean up, inventory already dropped

        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            // Wipe all inventory slots so Minecraft's death drop produces nothing
            clearAllInventory(player);

            // Kill the player — inventory was already dropped when grace period expired
            killPlayer(player);
            player.sendMessage(color("&cYou were killed for combat-logging!"));
            plugin.getLogger().info(String.format(
                "[PUNISHMENT] Killed %s on login (grace period expired while offline).", player.getName()));
        });

        // Opponent already received their reward at grace-period expiry.
        // Just send a reminder message if they're online.
        if (opponentId != null) {
            Player opponent = plugin.getServer().getPlayer(opponentId);
            if (opponent != null && opponent.isOnline()) {
                opponent.sendMessage(color("&e" + player.getName()
                    + " &ahas rejoined and been killed for combat-logging!"));
            }
        }
    }

    /** Returns true if this player has a pending kill punishment. */
    public boolean hasPendingPunishment(UUID playerId) {
        return pendingPunishments.containsKey(playerId);
    }

    /** Returns true if this player is currently in a grace period. */
    public boolean isTracked(UUID playerId) {
        return disconnectedPlayers.containsKey(playerId);
    }

    /** Returns the disconnect data for a tracked player, or null. */
    public DisconnectData getDisconnectData(UUID playerId) {
        return disconnectedPlayers.get(playerId);
    }

    /** Clears tracking for a player (e.g. when combat ends normally). */
    public void clearTracking(UUID playerId) {
        DisconnectData data = disconnectedPlayers.remove(playerId);
        if (data != null && data.getPunishmentTask() != null) {
            data.getPunishmentTask().cancel();
        }
    }

    /** Full cleanup on plugin disable. */
    public void cleanup() {
        disconnectedPlayers.values().forEach(d -> {
            if (d.getPunishmentTask() != null) d.getPunishmentTask().cancel();
        });
        disconnectedPlayers.clear();

        displayTasks.values().forEach(t -> { if (t != null) t.cancel(); });
        displayTasks.clear();

        bossBars.values().forEach(b -> { if (b != null) b.removeAll(); });
        bossBars.clear();

        pendingPunishments.clear();
        pendingPunishmentData.clear();
        disconnectHistory.clear();
    }

    // ── Repeat-logout helpers ─────────────────────────────────────────────

    /**
     * Returns true if the player has already disconnected {@code max-count} or more
     * times within the rolling {@code window-seconds} window.
     */
    private boolean isRepeatOffender(UUID playerId) {
        Deque<Long> history = disconnectHistory.get(playerId);
        if (history == null || history.isEmpty()) return false;

        long windowMs  = cfg().getRepeatLogoutWindowSeconds() * 1000L;
        long cutoff    = System.currentTimeMillis() - windowMs;
        int  maxCount  = cfg().getRepeatLogoutMaxCount();

        // Count entries within the window
        long count = history.stream().filter(t -> t >= cutoff).count();
        return count >= maxCount;
    }

    /** Appends the current timestamp to the player's disconnect history. */
    private void recordDisconnectHistory(UUID playerId) {
        Deque<Long> history = disconnectHistory.computeIfAbsent(playerId, k -> new ArrayDeque<>());
        history.addLast(System.currentTimeMillis());

        // Prune entries older than the window to keep memory bounded
        long windowMs = cfg().getRepeatLogoutWindowSeconds() * 1000L;
        long cutoff   = System.currentTimeMillis() - windowMs;
        history.removeIf(t -> t < cutoff);
    }

    // ── Punishment helpers ────────────────────────────────────────────────

    /** Applies punishment when the grace period expires without reconnect. */
    private void applyPunishment(DisconnectData data) {
        plugin.getLogger().info(String.format(
            "[PUNISHMENT] %s did not reconnect in time. Applying penalty.", data.getPlayerName()));

        // Record stats immediately regardless of online status
        combatManager.getCombatTracker().recordLossByUUID(data.getPlayerId());
        if (data.getOpponentId() != null) {
            combatManager.getCombatTracker().recordWinByUUID(data.getOpponentId());
        }

        // Notify opponent and give reward immediately
        if (data.getOpponentId() != null) {
            Player opponent = plugin.getServer().getPlayer(data.getOpponentId());
            if (opponent != null && opponent.isOnline()) {
                opponent.sendMessage(color("&aYou won! &e" + data.getPlayerName()
                    + " &acombat-logged and was punished!"));
            }
        }

        // Broadcast punishment announcement
        broadcastPunishment(data.getPlayerName());

        // Check if the player is currently online (rejoined before the timer fired)
        Player onlinePlayer = plugin.getServer().getPlayer(data.getPlayerId());

        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            // ── Player is online ──────────────────────────────────────────
            // Clear live inventory, drop snapshot at disconnect location, then kill.
            final Player target = onlinePlayer;
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                // Wipe all inventory slots so Minecraft's death drop produces nothing
                clearAllInventory(target);
                // Drop the snapshotted items at the original disconnect location
                if (cfg().isDropInventoryOnPunish()) {
                    dropItemsRaw(data.getDisconnectLocation(), data.getInventory(),
                        data.getArmor(), data.getPlayerName());
                }
                killPlayer(target);
                target.sendMessage(color("&cYou were killed for combat-logging!"));
                plugin.getLogger().info("[PUNISHMENT] Killed online player "
                    + data.getPlayerName() + " for not reconnecting in time.");
            });
        } else {
            // ── Player is offline ─────────────────────────────────────────
            // Drop inventory NOW at the disconnect location (visible to the opponent).
            if (cfg().isDropInventoryOnPunish()) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                    dropItemsRaw(data.getDisconnectLocation(), data.getInventory(),
                        data.getArmor(), data.getPlayerName()));
            }
            // Queue a kill-only punishment for when they next log in.
            // Inventory is already dropped — applyPendingPunishment will only
            // clear their live inventory and kill them, NOT drop again.
            pendingPunishments.put(data.getPlayerId(), data.getOpponentId());
            // Store data so we know NOT to re-drop (inventoryAlreadyDropped = true)
            pendingPunishmentData.put(data.getPlayerId(), data);
        }
    }

    /** Applies an immediate punishment (no grace period). */
    private void applyImmediatePunishment(UUID playerId, String playerName,
                                          UUID opponentId, String opponentName,
                                          org.bukkit.Location location,
                                          org.bukkit.inventory.ItemStack[] inventory,
                                          org.bukkit.inventory.ItemStack[] armor) {
        combatManager.getCombatTracker().recordLossByUUID(playerId);
        if (opponentId != null) combatManager.getCombatTracker().recordWinByUUID(opponentId);

        if (cfg().isDropInventoryOnPunish()) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                dropItemsRaw(location, inventory, armor, playerName));
        }

        // Store snapshot data so applyPendingPunishment can drop inventory on next login
        if (cfg().isKillOnPunish()) {
            DisconnectData snapData = new DisconnectData(
                playerId, playerName, opponentId, opponentName,
                0, 0, DisconnectType.INTENTIONAL, System.currentTimeMillis(),
                location, inventory, armor
            );
            pendingPunishments.put(playerId, opponentId);
            pendingPunishmentData.put(playerId, snapData);
        }

        if (opponentId != null) {
            Player opponent = plugin.getServer().getPlayer(opponentId);
            if (opponent != null && opponent.isOnline()) {
                opponent.sendMessage(color("&aYou won! &e" + playerName
                    + " &acombat-logged and was punished!"));
            }
        }

        broadcastPunishment(playerName);
    }

    private void dropItems(DisconnectData data) {
        dropItemsRaw(data.getDisconnectLocation(), data.getInventory(), data.getArmor(), data.getPlayerName());
    }

    private void dropItemsRaw(org.bukkit.Location loc,
                               org.bukkit.inventory.ItemStack[] inventory,
                               org.bukkit.inventory.ItemStack[] armor,
                               String playerName) {
        org.bukkit.World world = loc.getWorld();
        if (world == null) return;

        int dropped = 0;
        for (org.bukkit.inventory.ItemStack item : inventory) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                world.dropItemNaturally(loc, item);
                dropped++;
            }
        }
        for (org.bukkit.inventory.ItemStack item : armor) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                world.dropItemNaturally(loc, item);
                dropped++;
            }
        }
        plugin.getLogger().info(String.format(
            "[PUNISHMENT] Dropped %d item stacks for %s at %s (storage=%d, armor=%d)",
            dropped, playerName, loc,
            (int) java.util.Arrays.stream(inventory).filter(i -> i != null && i.getType() != org.bukkit.Material.AIR).count(),
            (int) java.util.Arrays.stream(armor).filter(i -> i != null && i.getType() != org.bukkit.Material.AIR).count()));
        // Log call site to detect double-drop
        plugin.getLogger().info("[PUNISHMENT] dropItemsRaw called from: "
            + Thread.currentThread().getStackTrace()[2].toString());
    }

    private void broadcastPunishment(String playerName) {
        String msg = color(cfg().getPunishmentBroadcast().replace("{player}", playerName));
        plugin.getServer().broadcastMessage(msg);
    }

    // ── Grace-period display ──────────────────────────────────────────────

    private void startGracePeriodDisplay(Player opponent, String disconnectedName,
                                         int totalSeconds, DisconnectType type) {
        String typeLabel = type == DisconnectType.BAD_INTERNET ? "bad internet" : "intentional";

        BukkitTask task = new BukkitRunnable() {
            int remaining = totalSeconds;

            @Override
            public void run() {
                if (!opponent.isOnline() || remaining <= 0) {
                    cancel();
                    displayTasks.remove(opponent.getUniqueId());
                    return;
                }

                String raw = cfg().getGracePeriodFormat()
                    .replace("{player}", disconnectedName)
                    .replace("{time}",   String.valueOf(remaining))
                    .replace("{type}",   typeLabel);
                String msg = color(raw);

                switch (cfg().getDisplayMode().toLowerCase()) {
                    case "bossbar":
                        showBossBar(opponent, msg, remaining, totalSeconds);
                        break;
                    case "scoreboard":
                        opponent.sendTitle(color("&eGrace Period"), msg, 5, 25, 5);
                        break;
                    default: // actionbar
                        opponent.spigot().sendMessage(
                            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(msg));
                        break;
                }
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        displayTasks.put(opponent.getUniqueId(), task);
    }

    private void stopGracePeriodDisplay(UUID opponentId) {
        BukkitTask task = displayTasks.remove(opponentId);
        if (task != null) task.cancel();

        org.bukkit.boss.BossBar bar = bossBars.remove(opponentId);
        if (bar != null) bar.removeAll();
    }

    private void showBossBar(Player player, String title, int remaining, int total) {
        org.bukkit.boss.BossBar bar = bossBars.computeIfAbsent(player.getUniqueId(), k ->
            org.bukkit.Bukkit.createBossBar(title,
                org.bukkit.boss.BarColor.YELLOW,
                org.bukkit.boss.BarStyle.SOLID));

        bar.setTitle(title);
        bar.setProgress(Math.max(0.0, Math.min(1.0, (double) remaining / total)));
        if (!bar.getPlayers().contains(player)) bar.addPlayer(player);
    }

    // ── Utility ───────────────────────────────────────────────────────────

    /**
     * Completely wipes a player's inventory so Minecraft's death-drop event
     * produces nothing. Must be called on the main thread before killing.
     */
    private void clearAllInventory(Player player) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        inv.clear();
        org.bukkit.inventory.ItemStack air = new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR);
        inv.setHelmet(air);
        inv.setChestplate(air);
        inv.setLeggings(air);
        inv.setBoots(air);
        inv.setItemInOffHand(air);
    }

    /**
     * Kills a player, optionally bypassing the Totem of Undying.
     *
     * <ul>
     *   <li>{@code bypass-totem: true} — {@code setHealth(0.0)} kills instantly,
     *       totem cannot proc.</li>
     *   <li>{@code bypass-totem: false} — deals 10000 void damage through the
     *       normal damage pipeline; a held totem will proc and save the player.</li>
     * </ul>
     */
    private void killPlayer(Player player) {
        if (cfg().isBypassTotem()) {
            player.setHealth(0.0);
        } else {
            player.damage(10000.0);
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    // ── Data class ────────────────────────────────────────────────────────

    public static final class DisconnectData {
        private final UUID   playerId;
        private final String playerName;
        private final UUID   opponentId;
        private final String opponentName;
        private final int    remainingCombatTime;
        private final int    graceSeconds;
        private final DisconnectType type;
        private final long   disconnectTime;
        private final org.bukkit.Location disconnectLocation;
        private final org.bukkit.inventory.ItemStack[] inventory;
        private final org.bukkit.inventory.ItemStack[] armor;
        private BukkitTask punishmentTask;

        public DisconnectData(UUID playerId, String playerName,
                              UUID opponentId, String opponentName,
                              int remainingCombatTime, int graceSeconds,
                              DisconnectType type, long disconnectTime,
                              org.bukkit.Location disconnectLocation,
                              org.bukkit.inventory.ItemStack[] inventory,
                              org.bukkit.inventory.ItemStack[] armor) {
            this.playerId            = playerId;
            this.playerName          = playerName;
            this.opponentId          = opponentId;
            this.opponentName        = opponentName;
            this.remainingCombatTime = remainingCombatTime;
            this.graceSeconds        = graceSeconds;
            this.type                = type;
            this.disconnectTime      = disconnectTime;
            this.disconnectLocation  = disconnectLocation;
            this.inventory           = inventory;
            this.armor               = armor;
        }

        public UUID     getPlayerId()            { return playerId; }
        public String   getPlayerName()          { return playerName; }
        public UUID     getOpponentId()          { return opponentId; }
        public String   getOpponentName()        { return opponentName; }
        public int      getRemainingCombatTime() { return remainingCombatTime; }
        public int      getGraceSeconds()        { return graceSeconds; }
        public DisconnectType getType()          { return type; }
        public long     getDisconnectTime()      { return disconnectTime; }
        public org.bukkit.Location getDisconnectLocation() { return disconnectLocation; }
        public org.bukkit.inventory.ItemStack[] getInventory() { return inventory; }
        public org.bukkit.inventory.ItemStack[] getArmor()     { return armor; }
        public BukkitTask getPunishmentTask()    { return punishmentTask; }
        public void setPunishmentTask(BukkitTask t) { this.punishmentTask = t; }
    }
}


