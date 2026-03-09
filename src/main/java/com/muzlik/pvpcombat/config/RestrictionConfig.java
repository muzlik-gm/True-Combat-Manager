package com.muzlik.pvpcombat.config;

import org.bukkit.configuration.ConfigurationSection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Restriction configuration settings.
 * Handles ender pearl, elytra, and teleport restrictions during combat.
 */
public class RestrictionConfig extends SubConfig {

    // Ender pearl restrictions
    private boolean enderPearlEnabled;
    private int enderPearlCooldown;
    private double enderPearlCombatCooldownMultiplier;
    private boolean enderPearlBlockUsage;

    // Elytra restrictions
    private boolean elytraEnabled;
    private boolean elytraBlockGlide;
    private boolean elytraBlockBoosting;
    private boolean elytraBlockTakeoff;
    private int elytraMinSafeHeight;
    private int elytraBoostCooldown;
    private Map<String, ElytraWorldSettings> elytraWorldSettings;
    private boolean elytraBlockAtNight;
    private boolean elytraBlockAtDay;

    // Teleport restrictions
    private boolean teleportEnabled;
    private List<String> teleportBlockedCommands;

    /**
     * Creates a new restriction configuration instance.
     *
     * @param validator The configuration validator
     * @param config The configuration section
     */
    public RestrictionConfig(ConfigurationValidator validator, ConfigurationSection config) {
        super(validator, config, "restrictions");
    }

    @Override
    public void load() {
        // Ender pearl settings
        ConfigurationSection enderPearlSection = getSection("enderpearl");
        if (enderPearlSection != null) {
            enderPearlEnabled = enderPearlSection.getBoolean("enabled", true);
            enderPearlCooldown = enderPearlSection.getInt("cooldown", 10);
            enderPearlCombatCooldownMultiplier = enderPearlSection.getDouble("combat-cooldown-multiplier", 2.0);
            enderPearlBlockUsage = enderPearlSection.getBoolean("block-usage", false);
        } else {
            enderPearlEnabled = true;
            enderPearlCooldown = 10;
            enderPearlCombatCooldownMultiplier = 2.0;
            enderPearlBlockUsage = false;
        }

        // Elytra settings
        ConfigurationSection elytraSection = getSection("elytra");
        if (elytraSection != null) {
            elytraEnabled = elytraSection.getBoolean("enabled", true);
            elytraBlockGlide = elytraSection.getBoolean("block-glide", true);
            elytraBlockBoosting = elytraSection.getBoolean("block-boosting", true);
            elytraBlockTakeoff = elytraSection.getBoolean("block-takeoff", true);
            elytraMinSafeHeight = elytraSection.getInt("min-safe-height", 10);
            elytraBoostCooldown = elytraSection.getInt("boost-cooldown", 30);

            // World-specific settings
            elytraWorldSettings = new HashMap<>();
            ConfigurationSection worldsSection = elytraSection.getConfigurationSection("worlds");
            if (worldsSection != null) {
                for (String worldName : worldsSection.getKeys(false)) {
                    ConfigurationSection worldSection = worldsSection.getConfigurationSection(worldName);
                    if (worldSection != null) {
                        ElytraWorldSettings settings = new ElytraWorldSettings(
                            worldSection.getInt("min-safe-height", elytraMinSafeHeight),
                            worldSection.getBoolean("block-glide", elytraBlockGlide)
                        );
                        elytraWorldSettings.put(worldName, settings);
                    }
                }
            }

            // Time-based restrictions
            ConfigurationSection timeSection = elytraSection.getConfigurationSection("time-restrictions");
            if (timeSection != null) {
                elytraBlockAtNight = timeSection.getBoolean("block-at-night", false);
                elytraBlockAtDay = timeSection.getBoolean("block-at-day", false);
            } else {
                elytraBlockAtNight = false;
                elytraBlockAtDay = false;
            }
        } else {
            elytraEnabled = true;
            elytraBlockGlide = true;
            elytraBlockBoosting = true;
            elytraBlockTakeoff = true;
            elytraMinSafeHeight = 10;
            elytraBoostCooldown = 30;
            elytraWorldSettings = new HashMap<>();
            elytraBlockAtNight = false;
            elytraBlockAtDay = false;
        }

        // Teleport settings
        ConfigurationSection teleportSection = getSection("teleport");
        if (teleportSection != null) {
            teleportEnabled = teleportSection.getBoolean("enabled", true);
            teleportBlockedCommands = teleportSection.getStringList("blocked-commands");
            if (teleportBlockedCommands.isEmpty()) {
                teleportBlockedCommands = List.of("/tp", "/teleport", "/warp", "/home", "/spawn");
            }
        } else {
            teleportEnabled = true;
            teleportBlockedCommands = List.of("/tp", "/teleport", "/warp", "/home", "/spawn");
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

        if (enderPearlCooldown < 0) {
            errors.add(new ConfigurationValidator.ConfigValidationError("restrictions.enderpearl.cooldown", 
                "Ender pearl cooldown cannot be negative: " + enderPearlCooldown, 
                enderPearlCooldown, 0, ConfigurationValidator.ConfigValidationError.Severity.ERROR));
        }
        if (enderPearlCombatCooldownMultiplier <= 0) {
            errors.add(new ConfigurationValidator.ConfigValidationError("restrictions.enderpearl.combat-cooldown-multiplier",
                "Combat cooldown multiplier must be positive: " + enderPearlCombatCooldownMultiplier, 
                enderPearlCombatCooldownMultiplier, 1.0, ConfigurationValidator.ConfigValidationError.Severity.ERROR));
        }

        if (elytraMinSafeHeight < 0) {
            errors.add(new ConfigurationValidator.ConfigValidationError("restrictions.elytra.min-safe-height", 
                "Minimum safe height cannot be negative: " + elytraMinSafeHeight, 
                elytraMinSafeHeight, 0, ConfigurationValidator.ConfigValidationError.Severity.ERROR));
        }
        if (elytraBoostCooldown < 0) {
            errors.add(new ConfigurationValidator.ConfigValidationError("restrictions.elytra.boost-cooldown", 
                "Boost cooldown cannot be negative: " + elytraBoostCooldown, 
                elytraBoostCooldown, 0, ConfigurationValidator.ConfigValidationError.Severity.ERROR));
        }

        return new ConfigurationValidator.ValidationResult(errors.isEmpty(), errors, warnings, info);
    }

    @Override
    public boolean isEnabled() {
        return enderPearlEnabled || elytraEnabled || teleportEnabled;
    }

    // Getters for ender pearl settings
    public boolean isEnderPearlEnabled() {
        return enderPearlEnabled;
    }

    public int getEnderPearlCooldown() {
        return enderPearlCooldown;
    }

    public double getEnderPearlCombatCooldownMultiplier() {
        return enderPearlCombatCooldownMultiplier;
    }

    public boolean isEnderPearlBlockUsage() {
        return enderPearlBlockUsage;
    }

    // Getters for elytra settings
    public boolean isElytraEnabled() {
        return elytraEnabled;
    }

    public boolean isElytraBlockGlide() {
        return elytraBlockGlide;
    }

    public boolean isElytraBlockBoosting() {
        return elytraBlockBoosting;
    }

    public boolean isElytraBlockTakeoff() {
        return elytraBlockTakeoff;
    }

    public int getElytraMinSafeHeight() {
        return elytraMinSafeHeight;
    }

    public int getElytraBoostCooldown() {
        return elytraBoostCooldown;
    }

    public Map<String, ElytraWorldSettings> getElytraWorldSettings() {
        return elytraWorldSettings;
    }

    public boolean isElytraBlockAtNight() {
        return elytraBlockAtNight;
    }

    public boolean isElytraBlockAtDay() {
        return elytraBlockAtDay;
    }

    // Getters for teleport settings
    public boolean isTeleportEnabled() {
        return teleportEnabled;
    }

    public List<String> getTeleportBlockedCommands() {
        return teleportBlockedCommands;
    }

    /**
     * Checks if a command is blocked during combat.
     *
     * @param command The command to check
     * @return true if blocked, false otherwise
     */
    public boolean isCommandBlocked(String command) {
        return teleportBlockedCommands.contains(command.toLowerCase());
    }

    /**
     * Gets elytra settings for a specific world.
     *
     * @param worldName The world name
     * @return The world settings, or null if no specific settings exist
     */
    public ElytraWorldSettings getElytraSettingsForWorld(String worldName) {
        return elytraWorldSettings.get(worldName);
    }

    /**
     * World-specific elytra settings.
     */
    public static class ElytraWorldSettings {
        private final int minSafeHeight;
        private final boolean blockGlide;

        public ElytraWorldSettings(int minSafeHeight, boolean blockGlide) {
            this.minSafeHeight = minSafeHeight;
            this.blockGlide = blockGlide;
        }

        public int getMinSafeHeight() {
            return minSafeHeight;
        }

        public boolean isBlockGlide() {
            return blockGlide;
        }
    }

    @Override
    public int getLoadPriority() {
        return 3; // Load after combat and visual configs
    }
}