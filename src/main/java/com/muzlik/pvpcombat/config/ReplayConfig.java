package com.muzlik.pvpcombat.config;

import org.bukkit.configuration.ConfigurationSection;
import java.util.List;

/**
 * Replay configuration settings.
 * Handles combat replay system, storage formats, timeline settings, and access control.
 */
public class ReplayConfig extends SubConfig {

    // General replay settings
    private boolean replayEnabled;
    private String replayStorageFormat;
    private int replayTimelineCapacity;
    private int replayTimelineMaxAgeSeconds;
    private int replayCacheMaxAgeMinutes;
    private boolean replayAccessAdminOnly;
    private List<String> replayAllowedAdmins;

    // GUI settings
    private boolean replayGuiEnabled;
    private int replayGuiPageSize;
    private boolean replayGuiAutoplayEnabled;
    private double replayGuiAutoplaySpeed;
    private int replayGuiAutoplayInterval;

    /**
     * Creates a new replay configuration instance.
     *
     * @param validator The configuration validator
     * @param config The configuration section
     */
    public ReplayConfig(ConfigurationValidator validator, ConfigurationSection config) {
        super(validator, config, "replay");
    }

    @Override
    public void load() {
        replayEnabled = getBoolean("enabled", true);
        replayStorageFormat = getString("storage.format", "HYBRID");
        replayTimelineCapacity = getInt("timeline.capacity", 1000);
        replayTimelineMaxAgeSeconds = getInt("timeline.max_age_seconds", 600);
        replayCacheMaxAgeMinutes = getInt("cache.max_age_minutes", 30);
        replayAccessAdminOnly = getBoolean("access.admin_only", true);
        replayAllowedAdmins = getSection("access") != null ?
            getSection("access").getStringList("allowed_admins") : List.of();

        // GUI settings
        ConfigurationSection guiSection = getSection("gui");
        if (guiSection != null) {
            replayGuiEnabled = guiSection.getBoolean("enabled", true);
            replayGuiPageSize = guiSection.getInt("page_size", 50);

            ConfigurationSection autoplaySection = guiSection.getConfigurationSection("autoplay");
            if (autoplaySection != null) {
                replayGuiAutoplayEnabled = autoplaySection.getBoolean("enabled", false);
                replayGuiAutoplaySpeed = autoplaySection.getDouble("speed", 1.0);
                replayGuiAutoplayInterval = autoplaySection.getInt("interval", 4);
            } else {
                replayGuiAutoplayEnabled = false;
                replayGuiAutoplaySpeed = 1.0;
                replayGuiAutoplayInterval = 4;
            }
        } else {
            replayGuiEnabled = true;
            replayGuiPageSize = 50;
            replayGuiAutoplayEnabled = false;
            replayGuiAutoplaySpeed = 1.0;
            replayGuiAutoplayInterval = 4;
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

        if (!replayStorageFormat.equals("MEMORY") &&
            !replayStorageFormat.equals("COMPRESSED_FILE") &&
            !replayStorageFormat.equals("HYBRID")) {
            errors.add(new ConfigurationValidator.ConfigValidationError("replay.storage.format", 
                "Invalid storage format: " + replayStorageFormat + ". Must be MEMORY, COMPRESSED_FILE, or HYBRID", 
                replayStorageFormat, "HYBRID", ConfigurationValidator.ConfigValidationError.Severity.ERROR));
        }

        if (replayTimelineCapacity < 100) {
            warnings.add(new ConfigurationValidator.ConfigValidationError("replay.timeline.capacity", 
                "Timeline capacity too small: " + replayTimelineCapacity + ", recommended minimum: 100", 
                replayTimelineCapacity, 1000, ConfigurationValidator.ConfigValidationError.Severity.WARNING));
        }

        if (replayTimelineMaxAgeSeconds < 60) {
            warnings.add(new ConfigurationValidator.ConfigValidationError("replay.timeline.max_age_seconds", 
                "Timeline max age too short: " + replayTimelineMaxAgeSeconds + "s, recommended minimum: 60s", 
                replayTimelineMaxAgeSeconds, 600, ConfigurationValidator.ConfigValidationError.Severity.WARNING));
        }

        if (replayCacheMaxAgeMinutes < 5) {
            warnings.add(new ConfigurationValidator.ConfigValidationError("replay.cache.max_age_minutes", 
                "Cache max age too short: " + replayCacheMaxAgeMinutes + "min, recommended minimum: 5min", 
                replayCacheMaxAgeMinutes, 30, ConfigurationValidator.ConfigValidationError.Severity.WARNING));
        }

        if (replayGuiPageSize < 10) {
            warnings.add(new ConfigurationValidator.ConfigValidationError("replay.gui.page_size", 
                "GUI page size too small: " + replayGuiPageSize + ", recommended minimum: 10", 
                replayGuiPageSize, 50, ConfigurationValidator.ConfigValidationError.Severity.WARNING));
        }

        if (replayGuiAutoplaySpeed <= 0) {
            errors.add(new ConfigurationValidator.ConfigValidationError("replay.gui.autoplay.speed", 
                "Autoplay speed must be positive: " + replayGuiAutoplaySpeed, 
                replayGuiAutoplaySpeed, 1.0, ConfigurationValidator.ConfigValidationError.Severity.ERROR));
        }

        if (replayGuiAutoplayInterval < 1) {
            errors.add(new ConfigurationValidator.ConfigValidationError("replay.gui.autoplay.interval", 
                "Autoplay interval must be positive: " + replayGuiAutoplayInterval, 
                replayGuiAutoplayInterval, 4, ConfigurationValidator.ConfigValidationError.Severity.ERROR));
        }

        return new ConfigurationValidator.ValidationResult(errors.isEmpty(), errors, warnings, info);
    }

    @Override
    public boolean isEnabled() {
        return replayEnabled;
    }

    // Getters for general replay settings
    public boolean isReplayEnabled() {
        return replayEnabled;
    }

    public String getReplayStorageFormat() {
        return replayStorageFormat;
    }

    public int getReplayTimelineCapacity() {
        return replayTimelineCapacity;
    }

    public int getReplayTimelineMaxAgeSeconds() {
        return replayTimelineMaxAgeSeconds;
    }

    public int getReplayCacheMaxAgeMinutes() {
        return replayCacheMaxAgeMinutes;
    }

    public boolean isReplayAccessAdminOnly() {
        return replayAccessAdminOnly;
    }

    public List<String> getReplayAllowedAdmins() {
        return replayAllowedAdmins;
    }

    // Getters for GUI settings
    public boolean isReplayGuiEnabled() {
        return replayGuiEnabled;
    }

    public int getReplayGuiPageSize() {
        return replayGuiPageSize;
    }

    public boolean isReplayGuiAutoplayEnabled() {
        return replayGuiAutoplayEnabled;
    }

    public double getReplayGuiAutoplaySpeed() {
        return replayGuiAutoplaySpeed;
    }

    public int getReplayGuiAutoplayInterval() {
        return replayGuiAutoplayInterval;
    }

    /**
     * Checks if a player has admin access to replays.
     *
     * @param playerName The player name
     * @param uuid The player UUID
     * @return true if access is granted, false otherwise
     */
    public boolean hasReplayAccess(String playerName, String uuid) {
        if (!replayAccessAdminOnly) {
            return true;
        }
        return replayAllowedAdmins.contains(playerName) || replayAllowedAdmins.contains(uuid);
    }

    @Override
    public int getLoadPriority() {
        return 8; // Load last as it depends on other systems
    }
}