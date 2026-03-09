package com.muzlik.pvpcombat.config;

import org.bukkit.configuration.ConfigurationSection;
import java.util.List;

/**
 * Logging configuration settings.
 * Handles combat logging, storage types, retention policies, and summary delivery.
 */
public class LoggingConfig extends SubConfig {

    // General logging settings
    private boolean loggingEnabled;
    private String loggingLevel;
    private int loggingMaxFiles;
    private int loggingMaxSizeMb;

    // Combat logging settings
    private boolean combatLoggingDetailedEnabled;
    private String combatLoggingStorageType;
    private String combatLoggingSummaryDelivery;
    private int combatLoggingRetentionDays;
    private int combatLoggingMemoryMaxEntries;
    private List<String> combatLoggingIncludeStats;

    /**
     * Creates a new logging configuration instance.
     *
     * @param validator The configuration validator
     * @param config The configuration section
     */
    public LoggingConfig(ConfigurationValidator validator, ConfigurationSection config) {
        super(validator, config, "logging");
    }

    @Override
    public void load() {
        loggingEnabled = getBoolean("enabled", true);
        loggingLevel = getString("level", "INFO");
        loggingMaxFiles = getInt("max-files", 5);
        loggingMaxSizeMb = getInt("max-size-mb", 10);

        // Combat logging settings
        ConfigurationSection combatSection = getSection("combat");
        if (combatSection != null) {
            ConfigurationSection detailedSection = combatSection.getConfigurationSection("detailed");
            if (detailedSection != null) {
                combatLoggingDetailedEnabled = detailedSection.getBoolean("enabled", true);
            } else {
                combatLoggingDetailedEnabled = true;
            }

            ConfigurationSection storageSection = combatSection.getConfigurationSection("storage");
            if (storageSection != null) {
                combatLoggingStorageType = storageSection.getString("type", "BOTH");
            } else {
                combatLoggingStorageType = "BOTH";
            }

            ConfigurationSection summarySection = combatSection.getConfigurationSection("summary");
            if (summarySection != null) {
                combatLoggingSummaryDelivery = summarySection.getString("delivery", "CHAT");
            } else {
                combatLoggingSummaryDelivery = "CHAT";
            }

            ConfigurationSection retentionSection = combatSection.getConfigurationSection("retention");
            if (retentionSection != null) {
                combatLoggingRetentionDays = retentionSection.getInt("days", 30);
            } else {
                combatLoggingRetentionDays = 30;
            }

            ConfigurationSection memorySection = combatSection.getConfigurationSection("memory");
            if (memorySection != null) {
                combatLoggingMemoryMaxEntries = memorySection.getInt("max-entries", 10000);
            } else {
                combatLoggingMemoryMaxEntries = 10000;
            }

            ConfigurationSection statsSection = combatSection.getConfigurationSection("include-stats");
            if (statsSection != null) {
                combatLoggingIncludeStats = statsSection.getStringList("stats");
            } else {
                combatLoggingIncludeStats = List.of(
                    "hits_landed", "damage_dealt", "accuracy", "knockback_exchanges", "combat_duration"
                );
            }
        } else {
            combatLoggingDetailedEnabled = true;
            combatLoggingStorageType = "BOTH";
            combatLoggingSummaryDelivery = "CHAT";
            combatLoggingRetentionDays = 30;
            combatLoggingMemoryMaxEntries = 10000;
            combatLoggingIncludeStats = List.of(
                "hits_landed", "damage_dealt", "accuracy", "knockback_exchanges", "combat_duration"
            );
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

        if (!loggingLevel.equals("INFO") && !loggingLevel.equals("DEBUG") &&
            !loggingLevel.equals("WARNING") && !loggingLevel.equals("ERROR")) {
            warnings.add(new ConfigurationValidator.ConfigValidationError("logging.level", 
                "Invalid log level: " + loggingLevel + ". Valid levels: INFO, DEBUG, WARNING, ERROR", 
                loggingLevel, "INFO", ConfigurationValidator.ConfigValidationError.Severity.WARNING));
        }

        if (loggingMaxFiles < 1) {
            errors.add(new ConfigurationValidator.ConfigValidationError("logging.max-files", 
                "Max files must be positive: " + loggingMaxFiles, 
                loggingMaxFiles, 5, ConfigurationValidator.ConfigValidationError.Severity.ERROR));
        }

        if (loggingMaxSizeMb < 1) {
            errors.add(new ConfigurationValidator.ConfigValidationError("logging.max-size-mb", 
                "Max size must be positive: " + loggingMaxSizeMb, 
                loggingMaxSizeMb, 10, ConfigurationValidator.ConfigValidationError.Severity.ERROR));
        }

        if (!combatLoggingStorageType.equals("FILE") &&
            !combatLoggingStorageType.equals("MEMORY") &&
            !combatLoggingStorageType.equals("BOTH")) {
            errors.add(new ConfigurationValidator.ConfigValidationError("logging.combat.storage.type", 
                "Invalid storage type: " + combatLoggingStorageType + ". Must be FILE, MEMORY, or BOTH", 
                combatLoggingStorageType, "BOTH", ConfigurationValidator.ConfigValidationError.Severity.ERROR));
        }

        if (!combatLoggingSummaryDelivery.equals("CHAT") &&
            !combatLoggingSummaryDelivery.equals("GUI") &&
            !combatLoggingSummaryDelivery.equals("STORAGE") &&
            !combatLoggingSummaryDelivery.equals("NONE")) {
            warnings.add(new ConfigurationValidator.ConfigValidationError("logging.combat.summary.delivery", 
                "Invalid summary delivery: " + combatLoggingSummaryDelivery + ". Valid options: CHAT, GUI, STORAGE, NONE", 
                combatLoggingSummaryDelivery, "CHAT", ConfigurationValidator.ConfigValidationError.Severity.WARNING));
        }

        if (combatLoggingRetentionDays < 1) {
            warnings.add(new ConfigurationValidator.ConfigValidationError("logging.combat.retention.days", 
                "Retention days too low: " + combatLoggingRetentionDays + ", recommended minimum: 1 day", 
                combatLoggingRetentionDays, 30, ConfigurationValidator.ConfigValidationError.Severity.WARNING));
        }

        if (combatLoggingMemoryMaxEntries < 100) {
            warnings.add(new ConfigurationValidator.ConfigValidationError("logging.combat.memory.max-entries", 
                "Max memory entries too low: " + combatLoggingMemoryMaxEntries + ", recommended minimum: 100", 
                combatLoggingMemoryMaxEntries, 10000, ConfigurationValidator.ConfigValidationError.Severity.WARNING));
        }

        return new ConfigurationValidator.ValidationResult(errors.isEmpty(), errors, warnings, info);
    }

    @Override
    public boolean isEnabled() {
        return loggingEnabled;
    }

    // Getters for general logging settings
    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public int getLoggingMaxFiles() {
        return loggingMaxFiles;
    }

    public int getLoggingMaxSizeMb() {
        return loggingMaxSizeMb;
    }

    // Getters for combat logging settings
    public boolean isCombatLoggingDetailedEnabled() {
        return combatLoggingDetailedEnabled;
    }

    public String getCombatLoggingStorageType() {
        return combatLoggingStorageType;
    }

    public String getCombatLoggingSummaryDelivery() {
        return combatLoggingSummaryDelivery;
    }

    public int getCombatLoggingRetentionDays() {
        return combatLoggingRetentionDays;
    }

    public int getCombatLoggingMemoryMaxEntries() {
        return combatLoggingMemoryMaxEntries;
    }

    public List<String> getCombatLoggingIncludeStats() {
        return combatLoggingIncludeStats;
    }

    @Override
    public int getLoadPriority() {
        return 6; // Load after other configs
    }
}