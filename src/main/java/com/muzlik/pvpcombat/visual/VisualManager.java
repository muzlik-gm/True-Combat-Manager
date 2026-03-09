package com.muzlik.pvpcombat.visual;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import com.muzlik.pvpcombat.interfaces.IConfigManager;
import com.muzlik.pvpcombat.interfaces.IVisualManager;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

/**
 * Main visual manager implementing IVisualManager interface.
 * Coordinates bossbar, action bar, and sound management.
 */
public class VisualManager implements IVisualManager {

    private final PvPCombatPlugin plugin;
    private final BossBarManager bossBarManager;
    private final ActionBarManager actionBarManager;
    private final SoundManager soundManager;
    private final ThemeManager themeManager;
    private volatile MessageFormatter messageFormatter;

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
         // Lazy initialization for thread safety
         if (messageFormatter == null) {
             synchronized (this) {
                 if (messageFormatter == null) {
                     messageFormatter = new MessageFormatter(plugin);
                 }
             }
         }
         return messageFormatter;
     }

    /**
     * Initializes all visual managers.
     */
    public void initialize() {
        themeManager.loadThemes();
        // SoundManager loads profiles automatically in constructor
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
}