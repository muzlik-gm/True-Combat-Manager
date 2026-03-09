package com.muzlik.pvpcombat.core;

import com.muzlik.pvpcombat.admin.LoggingManager;
import com.muzlik.pvpcombat.interfaces.ICombatManager;
import com.muzlik.pvpcombat.interfaces.IConfigManager;
import com.muzlik.pvpcombat.interfaces.IRestrictionManager;
import com.muzlik.pvpcombat.interfaces.IVisualManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class extending JavaPlugin.
 */
public class PvPCombatPlugin extends JavaPlugin {

    private static PvPCombatPlugin instance;

    private PluginManager pluginManager;
    private ICombatManager combatManager;
    private IVisualManager visualManager;
    private IRestrictionManager restrictionManager;
    private IConfigManager configManager;
    private LoggingManager loggingManager;

    @Override
    public void onEnable() {
        instance = this;

        try {
            // Initialize plugin manager
            pluginManager = new PluginManager(this);

            // Load configurations with validation
            try {
                configManager = pluginManager.getConfigManager();
                configManager.loadConfig();
                getLogger().info("Configuration loaded successfully");
            } catch (Exception e) {
                getLogger().severe("Failed to load configuration: " + e.getMessage());
                e.printStackTrace();
                getLogger().severe("Using default configuration values");
                // Continue with defaults rather than disabling plugin
            }

            // Initialize logging manager
            try {
                loggingManager = new LoggingManager(this);
            } catch (Exception e) {
                getLogger().severe("Failed to initialize logging manager: " + e.getMessage());
                e.printStackTrace();
                // Create a minimal logging manager
                loggingManager = new LoggingManager(this);
            }

            // Initialize subsystems with error handling
            try {
                combatManager = pluginManager.getCombatManager();
                getLogger().info("Combat manager initialized");
            } catch (Exception e) {
                getLogger().severe("CRITICAL: Failed to initialize combat manager: " + e.getMessage());
                e.printStackTrace();
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            try {
                visualManager = pluginManager.getVisualManager();
                getLogger().info("Visual manager initialized");
            } catch (Exception e) {
                getLogger().severe("Failed to initialize visual manager: " + e.getMessage());
                e.printStackTrace();
                getLogger().warning("Visual features will be disabled");
            }

            try {
                restrictionManager = pluginManager.getRestrictionManager();
                getLogger().info("Restriction manager initialized");
            } catch (Exception e) {
                getLogger().severe("Failed to initialize restriction manager: " + e.getMessage());
                e.printStackTrace();
                getLogger().warning("Restriction features will be disabled");
            }

            // Register events and commands with error handling
            try {
                pluginManager.registerEvents();
                getLogger().info("Events registered successfully");
            } catch (Exception e) {
                getLogger().severe("Failed to register events: " + e.getMessage());
                e.printStackTrace();
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            try {
                pluginManager.registerCommands();
                getLogger().info("Commands registered successfully");
            } catch (Exception e) {
                getLogger().severe("Failed to register commands: " + e.getMessage());
                e.printStackTrace();
                getLogger().warning("Commands will not be available");
            }
            
            // Register PlaceholderAPI expansion if available
            try {
                if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    new com.muzlik.pvpcombat.integration.PvPCombatExpansion(this).register();
                    getLogger().info("PlaceholderAPI expansion registered!");
                }
            } catch (Exception e) {
                getLogger().warning("Failed to register PlaceholderAPI expansion: " + e.getMessage());
                e.printStackTrace();
                // Non-critical, continue without PlaceholderAPI
            }

            getLogger().info("PvPCombat plugin has been enabled successfully!");
            
        } catch (Exception e) {
            getLogger().severe("CRITICAL ERROR during plugin initialization: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (pluginManager != null) {
                pluginManager.shutdown();
                getLogger().info("Plugin manager shut down successfully");
            }
        } catch (Exception e) {
            getLogger().severe("Error during plugin shutdown: " + e.getMessage());
            e.printStackTrace();
        } finally {
            instance = null;
            getLogger().info("PvPCombat plugin has been disabled!");
        }
    }

    // Getters for accessing managers
    public static PvPCombatPlugin getInstance() {
        return instance;
    }

    public ICombatManager getCombatManager() {
        return combatManager;
    }

    public IVisualManager getVisualManager() {
        return visualManager;
    }

    public IRestrictionManager getRestrictionManager() {
        return restrictionManager;
    }

    public IConfigManager getConfigManager() {
        return configManager;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public LoggingManager getLoggingManager() {
        return loggingManager;
    }
    
    public com.muzlik.pvpcombat.protection.NewbieProtection getNewbieProtection() {
        return pluginManager != null ? pluginManager.getNewbieProtection() : null;
    }
}
