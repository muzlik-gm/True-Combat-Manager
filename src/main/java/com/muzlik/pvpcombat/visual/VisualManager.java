package com.muzlik.pvpcombat.visual;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import com.muzlik.pvpcombat.data.VisualPreferences;
import com.muzlik.pvpcombat.interfaces.IConfigManager;
import com.muzlik.pvpcombat.interfaces.IDatabaseManager;
import com.muzlik.pvpcombat.interfaces.IVisualManager;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main visual manager implementing IVisualManager interface.
 * Coordinates bossbar, action bar, and sound management.
 *
 * <p>Also owns the per-player {@link VisualPreferences} cache so that theme
 * selections persist across sessions and work even when the player is not
 * currently in combat.</p>
 */
public class VisualManager implements IVisualManager {

    private final PvPCombatPlugin plugin;
    private final BossBarManager bossBarManager;
    private final ActionBarManager actionBarManager;
    private final SoundManager soundManager;
    private final ThemeManager themeManager;
    private volatile MessageFormatter messageFormatter;

    /** In-memory cache of per-player visual preferences. */
    private final Map<UUID, VisualPreferences> preferencesCache = new ConcurrentHashMap<>();

    public VisualManager(PvPCombatPlugin plugin, IConfigManager configManager) {
        this.plugin = plugin;
        this.bossBarManager = new BossBarManager(plugin, configManager);
        this.actionBarManager = new ActionBarManager(plugin);
        this.soundManager = new SoundManager(plugin);
        this.themeManager = new ThemeManager(configManager, plugin.getLogger());
    }

    @Override
    public void displayBossBar(String sessionId) {
        bossBarManager.displayBossBar(sessionId);
    }

    @Override
    public void sendActionBar(Player player, String message) {
        actionBarManager.sendActionBar(player, message);
    }

    @Override
    public void playSound(Player player, Sound sound) {
        soundManager.playSound(player, sound);
    }

    @Override
    public void clearVisuals(Player player) {
        bossBarManager.clearBossBar(player);
        actionBarManager.clearActionBar(player);
    }

    @Override
    public void updateBossBarProgress(String sessionId, double progress) {
        bossBarManager.updateProgress(sessionId, progress);
    }

    /**
     * Updates the bossbar title.
     */
    public void updateBossBarTitle(String sessionId, String title) {
        bossBarManager.updateTitle(sessionId, title);
    }

    /**
     * Gets the bossbar manager for direct access.
     */
    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    /**
     * Gets the action bar manager for direct access.
     */
    public ActionBarManager getActionBarManager() {
        return actionBarManager;
    }

    /**
     * Gets the sound manager for direct access.
     */
    public SoundManager getSoundManager() {
        return soundManager;
    }

    /**
     * Gets the theme manager for direct access.
     */
    public ThemeManager getThemeManager() {
        return themeManager;
    }

    /**
     * Gets the message formatter for advanced message processing.
     */
    public MessageFormatter getMessageFormatter() {
        if (messageFormatter == null) {
            synchronized (this) {
                if (messageFormatter == null) {
                    messageFormatter = new MessageFormatter(plugin);
                }
            }
        }
        return messageFormatter;
    }

    // ── Per-player preferences ────────────────────────────────────────────

    /**
     * Returns the cached preferences for a player, loading from DB if needed.
     * Never returns null – falls back to defaults.
     */
    public VisualPreferences getPreferences(UUID playerId) {
        return preferencesCache.computeIfAbsent(playerId, id -> {
            IDatabaseManager db = getDatabase();
            if (db != null) {
                try {
                    return db.loadVisualPreferences(id);
                } catch (Exception e) {
                    plugin.getLogger().warning("[VisualManager] Could not load preferences for "
                        + id + ": " + e.getMessage());
                }
            }
            return new VisualPreferences(id);
        });
    }

    /**
     * Saves preferences to the cache and asynchronously persists to DB.
     */
    public void savePreferences(UUID playerId, VisualPreferences prefs) {
        preferencesCache.put(playerId, prefs);
        IDatabaseManager db = getDatabase();
        if (db != null) {
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    db.saveVisualPreferences(playerId, prefs);
                } catch (Exception e) {
                    plugin.getLogger().warning("[VisualManager] Could not save preferences for "
                        + playerId + ": " + e.getMessage());
                }
            });
        }
    }

    /**
     * Sets the player's preferred theme, persists it, and applies it to any
     * active combat session immediately.
     *
     * @param player    the player
     * @param themeName the theme to apply
     * @return true if the theme exists and was applied
     */
    public boolean setPlayerTheme(Player player, String themeName) {
        // Validate theme exists
        if (themeManager.getTheme(themeName) == null) {
            return false;
        }

        UUID id = player.getUniqueId();
        VisualPreferences prefs = getPreferences(id);
        prefs.setSelectedTheme(themeName);
        savePreferences(id, prefs);

        // Apply to active session if the player is in combat
        if (plugin.getCombatManager() != null && plugin.getCombatManager().isInCombat(player)) {
            com.muzlik.pvpcombat.combat.CombatManager cm =
                (com.muzlik.pvpcombat.combat.CombatManager) plugin.getCombatManager();
            for (com.muzlik.pvpcombat.data.CombatSession s : cm.getActiveSessions().values()) {
                if (s.involvesPlayer(player)) {
                    bossBarManager.applyTheme(s.getSessionId().toString(), themeName, true);
                    s.setCurrentTheme(themeName);
                    break;
                }
            }
        }

        return true;
    }

    /**
     * Returns the list of available themes from config, with a sensible default.
     */
    public List<String> getAvailableThemes() {
        List<String> themes = plugin.getConfig().getStringList("visual.themes.available");
        if (themes == null || themes.isEmpty()) {
            return java.util.Arrays.asList("minimal", "fire", "ice", "neon", "dark", "clean");
        }
        return themes;
    }

    /**
     * Returns the player's currently selected theme name.
     * Falls back to the server default theme if the player has no preference set.
     */
    public String getPlayerTheme(UUID playerId) {
        VisualPreferences prefs = preferencesCache.get(playerId);
        if (prefs != null && prefs.getSelectedTheme() != null
                && !prefs.getSelectedTheme().equals("default")) {
            return prefs.getSelectedTheme();
        }
        return plugin.getConfig().getString("visual.themes.default-theme", "clean");
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    /**
     * Initializes all visual managers.
     */
    public void initialize() {
        themeManager.loadThemes();
    }

    /**
     * Shuts down all visual managers.
     */
    public void shutdown() {
        bossBarManager.clearAllBossBars();
        actionBarManager.clearAllActionBars();
    }

    /**
     * Reloads configuration for all visual components.
     */
    public void reloadConfig() {
        themeManager.loadThemes();
        bossBarManager.reloadConfig();
        soundManager.reloadConfig();
        plugin.getLogger().info("VisualManager configuration reloaded");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private IDatabaseManager getDatabase() {
        if (plugin.getPluginManager() != null) {
            return plugin.getPluginManager().getDatabaseManager();
        }
        return null;
    }
}