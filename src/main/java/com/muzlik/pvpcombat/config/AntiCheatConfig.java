package com.muzlik.pvpcombat.config;

import org.bukkit.configuration.ConfigurationSection;
import java.util.HashMap;
import java.util.Map;

/**
 * Anti-cheat configuration settings.
 * Handles interference detection, third-party damage prevention, and world-specific rules.
 */
public class AntiCheatConfig extends SubConfig {

    // Interference detection settings
    private boolean interferenceEnabled;
    private double interferenceMaxPercentage;
    private int interferenceWindow;
    private boolean interferenceBlockHits;
    private String interferenceMessage;

    // Sound settings for interference
    private boolean interferenceSoundEnabled;
    private String interferenceSound;
    private double interferenceSoundVolume;
    private double interferenceSoundPitch;

    // World-specific settings
    private Map<String, WorldInterferenceSettings> worldSettings;

    /**
     * Creates a new anti-cheat configuration instance.
     *
     * @param validator The configuration validator
     * @param config The configuration section
     */
    public AntiCheatConfig(ConfigurationValidator validator, ConfigurationSection config) {
        super(validator, config, "anticheat");
    }

    @Override
    public void load() {
        // Interference detection settings
        ConfigurationSection interferenceSection = getSection("interference");
        if (interferenceSection != null) {
            interferenceEnabled = interferenceSection.getBoolean("enabled", true);
            interferenceMaxPercentage = interferenceSection.getDouble("max-interference-percentage", 10.0);
            interferenceWindow = interferenceSection.getInt("interference-window", 5);
            interferenceBlockHits = interferenceSection.getBoolean("block-hits", false);
            interferenceMessage = interferenceSection.getString("message",
                "&c{interferer} cannot interfere: &f{target} &cis already in combat with &f{opponent}!");

            // Sound settings
            ConfigurationSection soundSection = interferenceSection.getConfigurationSection("sound");
            if (soundSection != null) {
                interferenceSoundEnabled = soundSection.getBoolean("enabled", true);
                interferenceSound = soundSection.getString("sound", "ENTITY_VILLAGER_NO");
                interferenceSoundVolume = soundSection.getDouble("volume", 1.0);
                interferenceSoundPitch = soundSection.getDouble("pitch", 1.0);
            } else {
                interferenceSoundEnabled = true;
                interferenceSound = "ENTITY_VILLAGER_NO";
                interferenceSoundVolume = 1.0;
                interferenceSoundPitch = 1.0;
            }

            // World-specific settings
            worldSettings = new HashMap<>();
            ConfigurationSection worldsSection = interferenceSection.getConfigurationSection("worlds");
            if (worldsSection != null) {
                for (String worldName : worldsSection.getKeys(false)) {
                    ConfigurationSection worldSection = worldsSection.getConfigurationSection(worldName);
                    if (worldSection != null) {
                        WorldInterferenceSettings settings = new WorldInterferenceSettings(
                            worldSection.getBoolean("enabled", interferenceEnabled),
                            worldSection.getDouble("max-interference-percentage", interferenceMaxPercentage),
                            worldSection.getInt("interference-window", interferenceWindow),
                            worldSection.getBoolean("block-hits", interferenceBlockHits)
                        );
                        worldSettings.put(worldName, settings);
                    }
                }
            }
        } else {
            interferenceEnabled = true;
            interferenceMaxPercentage = 10.0;
            interferenceWindow = 5;
            interferenceBlockHits = false;
            interferenceMessage = "&c{interferer} cannot interfere: &f{target} &cis already in combat with &f{opponent}!";
            interferenceSoundEnabled = true;
            interferenceSound = "ENTITY_VILLAGER_NO";
            interferenceSoundVolume = 1.0;
            interferenceSoundPitch = 1.0;
            worldSettings = new HashMap<>();
        }
    }

    @Override
    public void reload() {
        load();
    }

    @Override
    public ConfigurationValidator.ValidationResult validate() {
        java.util.List<ConfigurationValidator.ConfigValidationError> errors = new java.util.ArrayList<>();
        java.util.List<ConfigurationValidator.ConfigValidationError> warnings = new java.util.ArrayList<>();
        java.util.List<ConfigurationValidator.ConfigValidationError> info = new java.util.ArrayList<>();

        if (interferenceMaxPercentage < 0 || interferenceMaxPercentage > 100) {
            errors.add(new ConfigurationValidator.ConfigValidationError("anticheat.interference.max-interference-percentage",
                "Max interference percentage must be between 0 and 100: " + interferenceMaxPercentage, 
                interferenceMaxPercentage, 10.0, ConfigurationValidator.ConfigValidationError.Severity.ERROR));
        }

        if (interferenceWindow < 1) {
            errors.add(new ConfigurationValidator.ConfigValidationError("anticheat.interference.interference-window",
                "Interference window must be positive: " + interferenceWindow, 
                interferenceWindow, 5, ConfigurationValidator.ConfigValidationError.Severity.ERROR));
        }

        if (interferenceSoundVolume < 0 || interferenceSoundVolume > 2) {
            warnings.add(new ConfigurationValidator.ConfigValidationError("anticheat.interference.sound.volume",
                "Sound volume should be between 0 and 2: " + interferenceSoundVolume, 
                interferenceSoundVolume, 1.0, ConfigurationValidator.ConfigValidationError.Severity.WARNING));
        }

        if (interferenceSoundPitch < 0.5 || interferenceSoundPitch > 2) {
            warnings.add(new ConfigurationValidator.ConfigValidationError("anticheat.interference.sound.pitch",
                "Sound pitch should be between 0.5 and 2: " + interferenceSoundPitch, 
                interferenceSoundPitch, 1.0, ConfigurationValidator.ConfigValidationError.Severity.WARNING));
        }

        return new ConfigurationValidator.ValidationResult(errors.isEmpty(), errors, warnings, info);
    }

    @Override
    public boolean isEnabled() {
        return interferenceEnabled;
    }

    // Getters for interference settings
    public boolean isInterferenceEnabled() {
        return interferenceEnabled;
    }

    public double getInterferenceMaxPercentage() {
        return interferenceMaxPercentage;
    }

    public int getInterferenceWindow() {
        return interferenceWindow;
    }

    public boolean isInterferenceBlockHits() {
        return interferenceBlockHits;
    }

    public String getInterferenceMessage() {
        return interferenceMessage;
    }

    public boolean isInterferenceSoundEnabled() {
        return interferenceSoundEnabled;
    }

    public String getInterferenceSound() {
        return interferenceSound;
    }

    public double getInterferenceSoundVolume() {
        return interferenceSoundVolume;
    }

    public double getInterferenceSoundPitch() {
        return interferenceSoundPitch;
    }

    public Map<String, WorldInterferenceSettings> getWorldSettings() {
        return worldSettings;
    }

    /**
     * Gets interference settings for a specific world.
     *
     * @param worldName The world name
     * @return The world settings, or global settings if no specific settings exist
     */
    public WorldInterferenceSettings getInterferenceSettingsForWorld(String worldName) {
        return worldSettings.getOrDefault(worldName, new WorldInterferenceSettings(
            interferenceEnabled, interferenceMaxPercentage, interferenceWindow, interferenceBlockHits
        ));
    }

    /**
     * World-specific interference settings that override global settings.
     */
    public static class WorldInterferenceSettings {
        private final boolean enabled;
        private final double maxPercentage;
        private final int window;
        private final boolean blockHits;

        public WorldInterferenceSettings(boolean enabled, double maxPercentage, int window, boolean blockHits) {
            this.enabled = enabled;
            this.maxPercentage = maxPercentage;
            this.window = window;
            this.blockHits = blockHits;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public double getMaxPercentage() {
            return maxPercentage;
        }

        public int getWindow() {
            return window;
        }

        public boolean isBlockHits() {
            return blockHits;
        }
    }

    @Override
    public int getLoadPriority() {
        return 7; // Load after other configs
    }
}