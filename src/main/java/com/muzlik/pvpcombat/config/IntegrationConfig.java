package com.muzlik.pvpcombat.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Integration configuration settings.
 * Handles cross-server sync, PlaceholderAPI, and other plugin integrations.
 */
public class IntegrationConfig extends SubConfig {

    // PlaceholderAPI settings
    private boolean placeholderApiEnabled;

    // Cross-server sync settings
    private boolean crossServerSyncEnabled;
    private String crossServerSyncPlatform;
    private String crossServerSyncChannel;
    private boolean crossServerSyncBroadcastEnabled;
    private String crossServerSyncBroadcastFormat;
    private boolean crossServerSyncPreventServerSwitch;
    private String crossServerSyncPreventServerSwitchMessage;
    private int crossServerSyncInterval;
    private int crossServerSyncTimeout;
    private boolean crossServerSyncConnectionPoolEnabled;
    private int crossServerSyncConnectionPoolMaxConnections;
    private int crossServerSyncConnectionPoolIdleTimeout;

    // Legacy network settings (deprecated)
    private boolean legacyNetworkEnabled;
    private String legacyNetworkServerId;

    /**
     * Creates a new integration configuration instance.
     *
     * @param validator The configuration validator
     * @param config The configuration section
     */
    public IntegrationConfig(ConfigurationValidator validator, ConfigurationSection config) {
        super(validator, config, "integration");
    }

    @Override
    public void load() {
        // PlaceholderAPI settings
        ConfigurationSection placeholderApiSection = getSection("placeholderapi");
        if (placeholderApiSection != null) {
            placeholderApiEnabled = placeholderApiSection.getBoolean("enabled", true);
        } else {
            placeholderApiEnabled = true;
        }

        // Cross-server sync settings
        ConfigurationSection crossServerSection = getSection("cross-server-sync");
        if (crossServerSection != null) {
            crossServerSyncEnabled = crossServerSection.getBoolean("enabled", false);
            crossServerSyncPlatform = crossServerSection.getString("platform", "AUTO");
            crossServerSyncChannel = crossServerSection.getString("channel", "pvpcombat:sync");

            // Broadcast settings
            ConfigurationSection broadcastSection = crossServerSection.getConfigurationSection("broadcast");
            if (broadcastSection != null) {
                crossServerSyncBroadcastEnabled = broadcastSection.getBoolean("enabled", false);
                crossServerSyncBroadcastFormat = broadcastSection.getString("format",
                    "&6[Network] &e{attacker} &fis now in combat with &e{defender} &fon &b{server}");
            } else {
                crossServerSyncBroadcastEnabled = false;
                crossServerSyncBroadcastFormat = "&6[Network] &e{attacker} &fis now in combat with &e{defender} &fon &b{server}";
            }

            // Server switching prevention
            ConfigurationSection preventSwitchSection = crossServerSection.getConfigurationSection("prevent-server-switch");
            if (preventSwitchSection != null) {
                crossServerSyncPreventServerSwitch = preventSwitchSection.getBoolean("enabled", true);
                crossServerSyncPreventServerSwitchMessage = preventSwitchSection.getString("message",
                    "&cCannot switch servers while in combat! Time remaining: {time}s");
            } else {
                crossServerSyncPreventServerSwitch = true;
                crossServerSyncPreventServerSwitchMessage = "&cCannot switch servers while in combat! Time remaining: {time}s";
            }

            crossServerSyncInterval = crossServerSection.getInt("sync-interval", 30);
            crossServerSyncTimeout = crossServerSection.getInt("timeout", 5);

            // Connection pooling
            ConfigurationSection poolSection = crossServerSection.getConfigurationSection("connection-pool");
            if (poolSection != null) {
                crossServerSyncConnectionPoolEnabled = poolSection.getBoolean("enabled", true);
                crossServerSyncConnectionPoolMaxConnections = poolSection.getInt("max-connections", 10);
                crossServerSyncConnectionPoolIdleTimeout = poolSection.getInt("idle-timeout", 300);
            } else {
                crossServerSyncConnectionPoolEnabled = true;
                crossServerSyncConnectionPoolMaxConnections = 10;
                crossServerSyncConnectionPoolIdleTimeout = 300;
            }
        } else {
            crossServerSyncEnabled = false;
            crossServerSyncPlatform = "AUTO";
            crossServerSyncChannel = "pvpcombat:sync";
            crossServerSyncBroadcastEnabled = false;
            crossServerSyncBroadcastFormat = "&6[Network] &e{attacker} &fis now in combat with &e{defender} &fon &b{server}";
            crossServerSyncPreventServerSwitch = true;
            crossServerSyncPreventServerSwitchMessage = "&cCannot switch servers while in combat! Time remaining: {time}s";
            crossServerSyncInterval = 30;
            crossServerSyncTimeout = 5;
            crossServerSyncConnectionPoolEnabled = true;
            crossServerSyncConnectionPoolMaxConnections = 10;
            crossServerSyncConnectionPoolIdleTimeout = 300;
        }

        // Legacy network settings (deprecated)
        ConfigurationSection networkSection = getSection("network");
        if (networkSection != null) {
            legacyNetworkEnabled = networkSection.getBoolean("enabled", false);
            legacyNetworkServerId = networkSection.getString("server-id", "server1");
        } else {
            legacyNetworkEnabled = false;
            legacyNetworkServerId = "server1";
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

        if (crossServerSyncEnabled) {
            if (!crossServerSyncPlatform.equals("AUTO") &&
                !crossServerSyncPlatform.equals("BUNGEE") &&
                !crossServerSyncPlatform.equals("VELOCITY")) {
                errors.add(new ConfigurationValidator.ConfigValidationError("integration.cross-server-sync.platform", 
                    "Invalid platform: " + crossServerSyncPlatform + ". Must be AUTO, BUNGEE, or VELOCITY", 
                    crossServerSyncPlatform, "AUTO", ConfigurationValidator.ConfigValidationError.Severity.ERROR));
            }

            if (crossServerSyncInterval < 5) {
                warnings.add(new ConfigurationValidator.ConfigValidationError("integration.cross-server-sync.sync-interval", 
                    "Sync interval too low: " + crossServerSyncInterval + "s, recommended minimum: 5s", 
                    crossServerSyncInterval, 5, ConfigurationValidator.ConfigValidationError.Severity.WARNING));
            }

            if (crossServerSyncTimeout < 1) {
                errors.add(new ConfigurationValidator.ConfigValidationError("integration.cross-server-sync.timeout", 
                    "Timeout must be positive: " + crossServerSyncTimeout, 
                    crossServerSyncTimeout, 5, ConfigurationValidator.ConfigValidationError.Severity.ERROR));
            }

            if (crossServerSyncConnectionPoolEnabled) {
                if (crossServerSyncConnectionPoolMaxConnections < 1) {
                    errors.add(new ConfigurationValidator.ConfigValidationError("integration.cross-server-sync.connection-pool.max-connections",
                        "Max connections must be positive: " + crossServerSyncConnectionPoolMaxConnections, 
                        crossServerSyncConnectionPoolMaxConnections, 10, ConfigurationValidator.ConfigValidationError.Severity.ERROR));
                }
                if (crossServerSyncConnectionPoolIdleTimeout < 30) {
                    warnings.add(new ConfigurationValidator.ConfigValidationError("integration.cross-server-sync.connection-pool.idle-timeout",
                        "Idle timeout too low: " + crossServerSyncConnectionPoolIdleTimeout + "s, recommended minimum: 30s", 
                        crossServerSyncConnectionPoolIdleTimeout, 300, ConfigurationValidator.ConfigValidationError.Severity.WARNING));
                }
            }
        }

        if (legacyNetworkEnabled) {
            warnings.add(new ConfigurationValidator.ConfigValidationError("integration.network.enabled", 
                "Legacy network settings are deprecated. Use cross-server-sync instead.", 
                true, false, ConfigurationValidator.ConfigValidationError.Severity.WARNING));
        }

        return new ConfigurationValidator.ValidationResult(errors.isEmpty(), errors, warnings, info);
    }

    @Override
    public boolean isEnabled() {
        return placeholderApiEnabled || crossServerSyncEnabled || legacyNetworkEnabled;
    }

    // Getters for PlaceholderAPI settings
    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }

    // Getters for cross-server sync settings
    public boolean isCrossServerSyncEnabled() {
        return crossServerSyncEnabled;
    }

    public String getCrossServerSyncPlatform() {
        return crossServerSyncPlatform;
    }

    public String getCrossServerSyncChannel() {
        return crossServerSyncChannel;
    }

    public boolean isCrossServerSyncBroadcastEnabled() {
        return crossServerSyncBroadcastEnabled;
    }

    public String getCrossServerSyncBroadcastFormat() {
        return crossServerSyncBroadcastFormat;
    }

    public boolean isCrossServerSyncPreventServerSwitch() {
        return crossServerSyncPreventServerSwitch;
    }

    public String getCrossServerSyncPreventServerSwitchMessage() {
        return crossServerSyncPreventServerSwitchMessage;
    }

    public int getCrossServerSyncInterval() {
        return crossServerSyncInterval;
    }

    public int getCrossServerSyncTimeout() {
        return crossServerSyncTimeout;
    }

    public boolean isCrossServerSyncConnectionPoolEnabled() {
        return crossServerSyncConnectionPoolEnabled;
    }

    public int getCrossServerSyncConnectionPoolMaxConnections() {
        return crossServerSyncConnectionPoolMaxConnections;
    }

    public int getCrossServerSyncConnectionPoolIdleTimeout() {
        return crossServerSyncConnectionPoolIdleTimeout;
    }

    // Getters for legacy network settings
    public boolean isLegacyNetworkEnabled() {
        return legacyNetworkEnabled;
    }

    public String getLegacyNetworkServerId() {
        return legacyNetworkServerId;
    }

    @Override
    public int getLoadPriority() {
        return 5; // Load after other configs
    }
}