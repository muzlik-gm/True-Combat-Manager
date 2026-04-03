package com.muzlik.pvpcombat.admin;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;

/**
 * Manages console logging settings for the plugin.
 * Controls what gets logged to console based on admin preferences.
 */
public class LoggingManager {

    private final PvPCombatPlugin plugin;
    private boolean consoleLoggingEnabled;

    public LoggingManager(PvPCombatPlugin plugin) {
        this.plugin = plugin;
        // Load from config
        this.consoleLoggingEnabled = plugin.getConfig().getBoolean("logging.console-enabled", false);
    }

    /**
     * Checks if console logging is enabled.
     */
    public boolean isConsoleLoggingEnabled() {
        return consoleLoggingEnabled;
    }

    /**
     * Enables console logging.
     */
    public void enableConsoleLogging() {
        this.consoleLoggingEnabled = true;
        plugin.getConfig().set("logging.console-enabled", true);
        plugin.saveConfig();
    }

    /**
     * Disables console logging.
     */
    public void disableConsoleLogging() {
        this.consoleLoggingEnabled = false;
        plugin.getConfig().set("logging.console-enabled", false);
        plugin.saveConfig();
    }

    /**
     * Toggles console logging and returns the new state.
     */
    public boolean toggleConsoleLogging() {
        this.consoleLoggingEnabled = !this.consoleLoggingEnabled;
        plugin.getConfig().set("logging.console-enabled", this.consoleLoggingEnabled);
        plugin.saveConfig();
        return this.consoleLoggingEnabled;
    }

    /**
     * Logs a message to console only if logging is enabled.
     */
    public void log(String message) {
        if (consoleLoggingEnabled) {
            plugin.getLogger().info(message);
        }
    }

    /**
     * Logs a warning to console only if logging is enabled.
     */
    public void logWarning(String message) {
        if (consoleLoggingEnabled) {
            plugin.getLogger().warning(message);
        }
    }

    /**
     * Always logs errors regardless of logging setting.
     */
    public void logError(String message) {
        plugin.getLogger().severe(message);
    }

    /**
     * Reloads configuration for logging manager.
     */
    public void reloadConfig() {
        this.consoleLoggingEnabled = plugin.getConfig().getBoolean("logging.console-enabled", false);
        plugin.getLogger().info("LoggingManager configuration reloaded");
    }
}
