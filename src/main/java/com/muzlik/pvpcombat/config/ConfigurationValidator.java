package com.muzlik.pvpcombat.config;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates plugin configuration and provides helpful error messages.
 * Ensures all config values are valid before the plugin starts.
 */
public class ConfigurationValidator {

    /**
     * Validation result containing errors, warnings, and info messages.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<ConfigValidationError> errors;
        private final List<ConfigValidationError> warnings;
        private final List<ConfigValidationError> info;

        public ValidationResult(boolean valid, List<ConfigValidationError> errors,
                              List<ConfigValidationError> warnings, List<ConfigValidationError> info) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
            this.info = info;
        }

        public boolean isValid() {
            return valid;
        }

        public List<ConfigValidationError> getErrors() {
            return errors;
        }

        public List<ConfigValidationError> getWarnings() {
            return warnings;
        }

        public List<ConfigValidationError> getInfo() {
            return info;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }

    /**
     * Represents a configuration validation error with severity and suggested fix.
     */
    public static class ConfigValidationError {
        private final String path;
        private final String message;
        private final Object invalidValue;
        private final Object suggestedValue;
        private final Severity severity;

        public ConfigValidationError(String path, String message, Object invalidValue,
                                    Object suggestedValue, Severity severity) {
            this.path = path;
            this.message = message;
            this.invalidValue = invalidValue;
            this.suggestedValue = suggestedValue;
            this.severity = severity;
        }

        public String getPath() {
            return path;
        }

        public String getMessage() {
            return message;
        }

        public Object getInvalidValue() {
            return invalidValue;
        }

        public Object getSuggestedValue() {
            return suggestedValue;
        }

        public Severity getSeverity() {
            return severity;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s (Current: %s, Suggested: %s)",
                severity, path, message, invalidValue, suggestedValue);
        }

        public enum Severity {
            ERROR,   // Plugin cannot start
            WARNING, // Plugin works but suboptimal
            INFO     // Informational message
        }
    }

    /**
     * Validates the entire configuration.
     */
    public ValidationResult validateConfig(FileConfiguration config) {
        List<ConfigValidationError> errors = new ArrayList<>();
        List<ConfigValidationError> warnings = new ArrayList<>();
        List<ConfigValidationError> info = new ArrayList<>();

        // Validate combat settings
        ValidationResult combatResult = validateCombatSettings(config.getConfigurationSection("combat"));
        errors.addAll(combatResult.getErrors());
        warnings.addAll(combatResult.getWarnings());
        info.addAll(combatResult.getInfo());

        // Validate restrictions
        ValidationResult restrictionsResult = validateRestrictions(config.getConfigurationSection("restrictions"));
        errors.addAll(restrictionsResult.getErrors());
        warnings.addAll(restrictionsResult.getWarnings());
        info.addAll(restrictionsResult.getInfo());

        // Validate visual settings
        ValidationResult visualResult = validateVisualSettings(config.getConfigurationSection("visual"));
        errors.addAll(visualResult.getErrors());
        warnings.addAll(visualResult.getWarnings());
        info.addAll(visualResult.getInfo());

        // Validate logging settings
        ValidationResult loggingResult = validateLoggingSettings(config.getConfigurationSection("logging"));
        errors.addAll(loggingResult.getErrors());
        warnings.addAll(loggingResult.getWarnings());
        info.addAll(loggingResult.getInfo());

        boolean valid = errors.isEmpty();
        return new ValidationResult(valid, errors, warnings, info);
    }

    /**
     * Validates combat section of config.
     */
    public ValidationResult validateCombatSettings(ConfigurationSection section) {
        List<ConfigValidationError> errors = new ArrayList<>();
        List<ConfigValidationError> warnings = new ArrayList<>();
        List<ConfigValidationError> info = new ArrayList<>();

        if (section == null) {
            errors.add(new ConfigValidationError("combat", "Combat section is missing",
                null, "Add combat section to config", ConfigValidationError.Severity.ERROR));
            return new ValidationResult(false, errors, warnings, info);
        }

        // Validate duration
        int duration = section.getInt("duration", -1);
        if (duration <= 0) {
            errors.add(new ConfigValidationError("combat.duration",
                "Duration must be greater than 0", duration, 30,
                ConfigValidationError.Severity.ERROR));
        } else if (duration > 600) {
            warnings.add(new ConfigValidationError("combat.duration",
                "Duration is very high (>10 minutes)", duration, 30,
                ConfigValidationError.Severity.WARNING));
        }

        // Validate disconnect protection
        if (section.contains("disconnect-protection")) {
            ConfigurationSection dcSection = section.getConfigurationSection("disconnect-protection");
            if (dcSection != null) {
                boolean enabled = dcSection.getBoolean("enabled", true);
                int gracePeriod = dcSection.getInt("grace-period", -1);
                
                if (enabled && gracePeriod <= 0) {
                    errors.add(new ConfigValidationError("combat.disconnect-protection.grace-period",
                        "Grace period must be greater than 0 when disconnect protection is enabled",
                        gracePeriod, 10, ConfigValidationError.Severity.ERROR));
                }
            }
        }

        boolean valid = errors.isEmpty();
        return new ValidationResult(valid, errors, warnings, info);
    }

    /**
     * Validates restrictions section of config.
     */
    public ValidationResult validateRestrictions(ConfigurationSection section) {
        List<ConfigValidationError> errors = new ArrayList<>();
        List<ConfigValidationError> warnings = new ArrayList<>();
        List<ConfigValidationError> info = new ArrayList<>();

        if (section == null) {
            warnings.add(new ConfigValidationError("restrictions", "Restrictions section is missing",
                null, "Add restrictions section for item/command blocking",
                ConfigValidationError.Severity.WARNING));
            return new ValidationResult(true, errors, warnings, info);
        }

        // Validate ender pearl cooldown
        if (section.contains("ender-pearl")) {
            ConfigurationSection epSection = section.getConfigurationSection("ender-pearl");
            if (epSection != null) {
                int cooldown = epSection.getInt("cooldown", -1);
                if (cooldown < 0) {
                    errors.add(new ConfigValidationError("restrictions.ender-pearl.cooldown",
                        "Cooldown cannot be negative", cooldown, 0,
                        ConfigValidationError.Severity.ERROR));
                }
            }
        }

        // Validate golden apple cooldowns
        if (section.contains("golden-apple")) {
            ConfigurationSection gaSection = section.getConfigurationSection("golden-apple");
            if (gaSection != null) {
                int cooldown = gaSection.getInt("cooldown", -1);
                int enchantedCooldown = gaSection.getInt("enchanted-cooldown", -1);
                
                if (cooldown < 0) {
                    errors.add(new ConfigValidationError("restrictions.golden-apple.cooldown",
                        "Cooldown cannot be negative", cooldown, 0,
                        ConfigValidationError.Severity.ERROR));
                }
                
                if (enchantedCooldown < 0) {
                    errors.add(new ConfigValidationError("restrictions.golden-apple.enchanted-cooldown",
                        "Enchanted cooldown cannot be negative", enchantedCooldown, 0,
                        ConfigValidationError.Severity.ERROR));
                }
            }
        }

        // Validate teleport blocked commands
        if (section.contains("teleport")) {
            ConfigurationSection tpSection = section.getConfigurationSection("teleport");
            if (tpSection != null) {
                List<String> blockedCommands = tpSection.getStringList("blocked-commands");
                if (blockedCommands.isEmpty()) {
                    warnings.add(new ConfigValidationError("restrictions.teleport.blocked-commands",
                        "No blocked commands specified", null,
                        Arrays.asList("tp", "teleport", "home", "spawn"),
                        ConfigValidationError.Severity.WARNING));
                }
            }
        }

        boolean valid = errors.isEmpty();
        return new ValidationResult(valid, errors, warnings, info);
    }

    /**
     * Validates visual settings section of config.
     */
    public ValidationResult validateVisualSettings(ConfigurationSection section) {
        List<ConfigValidationError> errors = new ArrayList<>();
        List<ConfigValidationError> warnings = new ArrayList<>();
        List<ConfigValidationError> info = new ArrayList<>();

        if (section == null) {
            warnings.add(new ConfigValidationError("visual", "Visual section is missing",
                null, "Add visual section for BossBar/ActionBar customization",
                ConfigValidationError.Severity.WARNING));
            return new ValidationResult(true, errors, warnings, info);
        }

        // Validate bossbar settings
        if (section.contains("bossbar")) {
            ConfigurationSection bbSection = section.getConfigurationSection("bossbar");
            if (bbSection != null) {
                String color = bbSection.getString("color", "");
                String style = bbSection.getString("style", "");
                
                // Validate color
                if (!color.isEmpty() && !isValidBossBarColor(color)) {
                    errors.add(new ConfigValidationError("visual.bossbar.color",
                        "Invalid BossBar color", color, "RED",
                        ConfigValidationError.Severity.ERROR));
                }
                
                // Validate style
                if (!style.isEmpty() && !isValidBossBarStyle(style)) {
                    errors.add(new ConfigValidationError("visual.bossbar.style",
                        "Invalid BossBar style", style, "SOLID",
                        ConfigValidationError.Severity.ERROR));
                }
            }
        }

        // Validate sounds
        if (section.contains("sounds")) {
            ConfigurationSection soundSection = section.getConfigurationSection("sounds");
            if (soundSection != null) {
                validateSound(soundSection, "combat-start", errors);
                validateSound(soundSection, "combat-end", errors);
                validateSound(soundSection, "timer-warning", errors);
            }
        }

        boolean valid = errors.isEmpty();
        return new ValidationResult(valid, errors, warnings, info);
    }

    /**
     * Validates logging settings section of config.
     */
    public ValidationResult validateLoggingSettings(ConfigurationSection section) {
        List<ConfigValidationError> errors = new ArrayList<>();
        List<ConfigValidationError> warnings = new ArrayList<>();
        List<ConfigValidationError> info = new ArrayList<>();

        if (section == null) {
            info.add(new ConfigValidationError("logging", "Logging section is missing",
                null, "Add logging section for debug output",
                ConfigValidationError.Severity.INFO));
            return new ValidationResult(true, errors, warnings, info);
        }

        // All logging settings are optional with defaults, so no errors possible
        boolean consoleEnabled = section.getBoolean("console-enabled", false);
        if (consoleEnabled) {
            info.add(new ConfigValidationError("logging.console-enabled",
                "Console logging is enabled - may impact performance on busy servers",
                true, false, ConfigValidationError.Severity.INFO));
        }

        return new ValidationResult(true, errors, warnings, info);
    }

    /**
     * Validates a sound configuration.
     */
    private void validateSound(ConfigurationSection section, String key, List<ConfigValidationError> errors) {
        String soundName = section.getString(key, "");
        if (!soundName.isEmpty()) {
            try {
                Sound.valueOf(soundName.toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add(new ConfigValidationError("visual.sounds." + key,
                    "Invalid sound name", soundName, "ENTITY_EXPERIENCE_ORB_PICKUP",
                    ConfigValidationError.Severity.ERROR));
            }
        }
    }

    /**
     * Checks if a BossBar color is valid.
     */
    private boolean isValidBossBarColor(String color) {
        try {
            org.bukkit.boss.BarColor.valueOf(color.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Checks if a BossBar style is valid.
     */
    private boolean isValidBossBarStyle(String style) {
        try {
            org.bukkit.boss.BarStyle.valueOf(style.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates a material name.
     */
    private boolean isValidMaterial(String materialName) {
        try {
            Material.valueOf(materialName.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates a single value and returns it if valid, null otherwise.
     * Used by SubConfig classes for type-safe value retrieval.
     * 
     * @param path Configuration path
     * @param value Value to validate
     * @param expectedType Expected type class
     * @param <T> Expected type
     * @return Validated value or null if invalid
     */
    public <T> T validateValue(String path, Object value, Class<T> expectedType) {
        if (value == null) {
            return null;
        }

        try {
            if (expectedType.isInstance(value)) {
                return expectedType.cast(value);
            }
        } catch (ClassCastException e) {
            // Invalid type, return null
        }

        return null;
    }
}
