package com.muzlik.pvpcombat.visual;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import com.muzlik.pvpcombat.interfaces.IConfigManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles audio cues for combat events with theme-based sound profiles.
 *
 * <p>Reads sound configuration from {@link IConfigManager#getMainConfig()} so that
 * hot-reloads via {@code /combat reload} are reflected immediately without a restart.</p>
 */
public class SoundManager {

    private final PvPCombatPlugin plugin;
    private final IConfigManager configManager;
    private final Map<String, SoundProfile> soundProfiles;

    public SoundManager(PvPCombatPlugin plugin) {
        this.plugin = plugin;
        // Resolve ConfigManager so we read from the reloadable FileConfiguration
        IConfigManager cm = plugin.getConfigManager();
        this.configManager = cm;
        this.soundProfiles = new HashMap<>();
        loadSoundProfiles();
    }

    /**
     * Plays a combat start sound.
     */
    public void playCombatStartSound(Player player) {
        playSoundForEvent(player, "combat_start");
    }

    /**
     * Plays a sound for a specific event using the active profile.
     * Reads the active profile name from the reloadable config each call so
     * in-game profile changes take effect without a restart.
     */
    public void playSoundForEvent(Player player, String eventType) {
        if (plugin.getVisualManager() != null) {
            if (!((VisualManager) plugin.getVisualManager()).getPreferences(player.getUniqueId()).isSoundsEnabled()) {
                return;
            }
        }
        // Use ConfigManager's FileConfiguration so reloads are respected
        FileConfiguration cfg = (configManager != null && configManager.getMainConfig() != null)
            ? configManager.getMainConfig()
            : plugin.getConfig();

        if (!cfg.getBoolean("visual.sounds.enabled", true)) {
            return;
        }

        String profileName = cfg.getString("visual.sounds.profile", "default");
        SoundProfile profile = soundProfiles.get(profileName);
        if (profile == null) {
            profile = soundProfiles.get("default");
        }

        if (profile != null) {
            SoundEvent soundEvent = profile.getSoundEvent(eventType);
            if (soundEvent != null) {
                player.playSound(player.getLocation(), soundEvent.getSound(),
                    soundEvent.getVolume(), soundEvent.getPitch());
            }
        }
    }

    /**
     * Plays a combat end sound.
     */
    public void playCombatEndSound(Player player) {
        playSoundForEvent(player, "combat_end");
    }

    /**
     * Plays a timer warning sound (e.g., 5 seconds left).
     */
    public void playTimerWarningSound(Player player) {
        playSoundForEvent(player, "timer_warning");
    }

    /**
     * Plays a timer reset sound.
     */
    public void playTimerResetSound(Player player) {
        playSoundForEvent(player, "timer_reset");
    }

    /**
     * Plays a sound for interference detection.
     */
    public void playInterferenceSound(Player player) {
        playSoundForEvent(player, "interference");
    }

    /**
     * Generic sound playing method.
     */
    public void playSound(Player player, Sound sound) {
        if (plugin.getVisualManager() != null) {
            if (!((VisualManager) plugin.getVisualManager()).getPreferences(player.getUniqueId()).isSoundsEnabled()) {
                return;
            }
        }
        FileConfiguration cfg = (configManager != null && configManager.getMainConfig() != null)
            ? configManager.getMainConfig() : plugin.getConfig();
        if (!cfg.getBoolean("visual.sounds.enabled", true)) return;
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    /**
     * Plays sound with custom volume and pitch.
     */
    public void playSound(Player player, Sound sound, float volume, float pitch) {
        if (plugin.getVisualManager() != null) {
            if (!((VisualManager) plugin.getVisualManager()).getPreferences(player.getUniqueId()).isSoundsEnabled()) {
                return;
            }
        }
        FileConfiguration cfg = (configManager != null && configManager.getMainConfig() != null)
            ? configManager.getMainConfig() : plugin.getConfig();
        if (!cfg.getBoolean("visual.sounds.enabled", true)) return;
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * Loads sound profiles from configuration.
     * Always registers built-in profiles first so they are available as fallbacks,
     * then overlays any profiles defined in config (allowing overrides).
     */
    private void loadSoundProfiles() {
        soundProfiles.clear();

        // Register built-ins first so they are always available
        createBuiltInSoundProfiles();

        // Overlay with config-defined profiles (allows admins to override built-ins)
        FileConfiguration cfg = (configManager != null && configManager.getMainConfig() != null)
            ? configManager.getMainConfig()
            : plugin.getConfig();

        ConfigurationSection profilesSection = cfg.getConfigurationSection("visual.sounds.profiles");
        if (profilesSection != null) {
            for (String profileName : profilesSection.getKeys(false)) {
                ConfigurationSection profileConfig = profilesSection.getConfigurationSection(profileName);
                if (profileConfig != null) {
                    SoundProfile profile = loadSoundProfileFromConfig(profileName, profileConfig);
                    // Only replace built-in if the config profile has at least one valid event
                    if (!profile.getAllSoundEvents().isEmpty()) {
                        soundProfiles.put(profileName, profile);
                    } else {
                        plugin.getLogger().warning("[SoundManager] Profile '" + profileName
                            + "' has no valid sound events – keeping built-in fallback.");
                    }
                }
            }
        }

        plugin.getLogger().fine("[SoundManager] Loaded " + soundProfiles.size() + " sound profiles: "
            + soundProfiles.keySet());
    }

    /**
     * Creates built-in sound profiles.
     */
    private void createBuiltInSoundProfiles() {
        // Default profile - Standard sounds
        SoundProfile defaultProfile = new SoundProfile("default");
        defaultProfile.addSoundEvent("combat_start", Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
        defaultProfile.addSoundEvent("combat_end", Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        defaultProfile.addSoundEvent("timer_warning", Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        defaultProfile.addSoundEvent("timer_reset", Sound.BLOCK_LEVER_CLICK, 0.5f, 1.5f);
        defaultProfile.addSoundEvent("interference", Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        soundProfiles.put("default", defaultProfile);

        // Subtle profile - Quiet sounds
        SoundProfile subtleProfile = new SoundProfile("subtle");
        subtleProfile.addSoundEvent("combat_start", Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.7f, 1.2f);
        subtleProfile.addSoundEvent("combat_end", Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
        subtleProfile.addSoundEvent("timer_warning", Sound.BLOCK_BAMBOO_BREAK, 0.6f, 1.8f);
        subtleProfile.addSoundEvent("timer_reset", Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 0.4f, 1.0f);
        subtleProfile.addSoundEvent("interference", Sound.BLOCK_ANVIL_BREAK, 0.3f, 0.8f);
        soundProfiles.put("subtle", subtleProfile);

        // Intense profile - Loud aggressive sounds
        SoundProfile intenseProfile = new SoundProfile("intense");
        intenseProfile.addSoundEvent("combat_start", Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.8f);
        intenseProfile.addSoundEvent("combat_end", Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.0f);
        intenseProfile.addSoundEvent("timer_warning", Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
        intenseProfile.addSoundEvent("timer_reset", Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.7f);
        intenseProfile.addSoundEvent("interference", Sound.ENTITY_BLAZE_DEATH, 1.0f, 1.0f);
        soundProfiles.put("intense", intenseProfile);

        // Calm profile - Relaxing sounds
        SoundProfile calmProfile = new SoundProfile("calm");
        calmProfile.addSoundEvent("combat_start", Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.5f);
        calmProfile.addSoundEvent("combat_end", Sound.ENTITY_ITEM_PICKUP, 0.6f, 2.0f);
        calmProfile.addSoundEvent("timer_warning", Sound.BLOCK_BELL_USE, 0.7f, 1.2f);
        calmProfile.addSoundEvent("timer_reset", Sound.BLOCK_CONDUIT_ACTIVATE, 0.5f, 1.8f);
        calmProfile.addSoundEvent("interference", Sound.ENTITY_VILLAGER_TRADE, 0.4f, 0.6f);
        soundProfiles.put("calm", calmProfile);

        // Electronic profile - Future/tech sounds
        SoundProfile electronicProfile = new SoundProfile("electronic");
        electronicProfile.addSoundEvent("combat_start", Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        electronicProfile.addSoundEvent("combat_end", Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 0.8f);
        electronicProfile.addSoundEvent("timer_warning", Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1.0f, 2.0f);
        electronicProfile.addSoundEvent("timer_reset", Sound.BLOCK_COMPARATOR_CLICK, 0.6f, 1.0f);
        electronicProfile.addSoundEvent("interference", Sound.BLOCK_ANVIL_BREAK, 0.8f, 1.2f);
        soundProfiles.put("electronic", electronicProfile);

        // Clean profile - Minimalist sounds
        SoundProfile cleanProfile = new SoundProfile("clean");
        cleanProfile.addSoundEvent("combat_start", Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
        cleanProfile.addSoundEvent("combat_end", Sound.UI_TOAST_IN, 0.7f, 1.2f);
        cleanProfile.addSoundEvent("timer_warning", Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.5f);
        cleanProfile.addSoundEvent("timer_reset", Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
        cleanProfile.addSoundEvent("interference", Sound.UI_TOAST_OUT, 0.6f, 0.5f);
        soundProfiles.put("clean", cleanProfile);
    }

    /**
     * Loads a sound profile from configuration.
     */
    private SoundProfile loadSoundProfileFromConfig(String profileName, ConfigurationSection config) {
        SoundProfile profile = new SoundProfile(profileName);

        // Load sound events
        ConfigurationSection eventsSection = config.getConfigurationSection("events");
        if (eventsSection != null) {
            for (String eventName : eventsSection.getKeys(false)) {
                ConfigurationSection eventConfig = eventsSection.getConfigurationSection(eventName);
                if (eventConfig != null) {
                    String soundName = eventConfig.getString("sound");
                    float volume = (float) eventConfig.getDouble("volume", 1.0);
                    float pitch = (float) eventConfig.getDouble("pitch", 1.0);

                    try {
                        Sound sound = Sound.valueOf(soundName.toUpperCase());
                        profile.addSoundEvent(eventName, sound, volume, pitch);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid sound: " + soundName + " in profile " + profileName);
                    }
                }
            }
        }

        return profile;
    }

    /**
     * Gets Sound enum from config string.
     */
    private Sound getSoundFromConfig(String configPath, String defaultSound) {
        String soundName = plugin.getConfig().getString(configPath, defaultSound);
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound name in config: " + soundName + ". Using default: " + defaultSound);
            try {
                return Sound.valueOf(defaultSound.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return Sound.BLOCK_ANVIL_LAND; // Fallback
            }
        }
    }

    /**
     * Reloads sound profiles from configuration.
     */
    public void reloadConfig() {
        soundProfiles.clear();
        loadSoundProfiles();
        plugin.getLogger().fine("SoundManager configuration reloaded");
    }

    /**
     * SoundProfile class for managing sound sets.
     */
    public static class SoundProfile {
        private final String name;
        private final Map<String, SoundEvent> soundEvents;

        public SoundProfile(String name) {
            this.name = name;
            this.soundEvents = new HashMap<>();
        }

        public void addSoundEvent(String eventType, Sound sound, float volume, float pitch) {
            soundEvents.put(eventType, new SoundEvent(sound, volume, pitch));
        }

        public SoundEvent getSoundEvent(String eventType) {
            return soundEvents.get(eventType);
        }

        public String getName() {
            return name;
        }

        public Map<String, SoundEvent> getAllSoundEvents() {
            return new HashMap<>(soundEvents);
        }
    }

    /**
     * SoundEvent class for individual sound configurations.
     */
    public static class SoundEvent {
        private final Sound sound;
        private final float volume;
        private final float pitch;

        public SoundEvent(Sound sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        public Sound getSound() { return sound; }
        public float getVolume() { return volume; }
        public float getPitch() { return pitch; }
    }
}