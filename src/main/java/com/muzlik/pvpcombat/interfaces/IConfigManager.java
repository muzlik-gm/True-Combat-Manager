package com.muzlik.pvpcombat.interfaces;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Interface for managing plugin configuration.
 */
public interface IConfigManager {

    /**
     * Loads the configuration from files.
     */
    void loadConfig();

    /**
     * Reloads the configuration.
     */
    void reloadConfig();

    /**
     * Validates the current configuration.
     *
     * @return true if valid, false otherwise
     */
    boolean validateConfig();

    /**
     * Gets the main configuration.
     *
     * @return The main config
     */
    FileConfiguration getMainConfig();

    /**
     * Gets the messages configuration.
     *
     * @return The messages config
     */
    FileConfiguration getMessagesConfig();

    /**
     * Saves the current configuration.
     */
    void saveConfig();

    /**
     * Gets a configuration value by path.
     *
     * @param path The configuration path
     * @return The value, or null if not found
     */
    Object getConfigValue(String path);

    /**
     * Sets a configuration value.
     *
     * @param path The configuration path
     * @param value The value to set
     */
    void setConfigValue(String path, Object value);
}