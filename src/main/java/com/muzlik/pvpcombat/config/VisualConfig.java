package com.muzlik.pvpcombat.config;

import org.bukkit.configuration.ConfigurationSection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Visual configuration settings.
 * Handles themes, bossbar formats, actionbar messages, sounds, and animations.
 */
public class VisualConfig extends SubConfig {

    // Theme settings
    private boolean themesEnabled;
    private String defaultTheme;
    private boolean allowCustomThemes;
    private List<String> availableThemes;

    // Animation settings
    private boolean animationsEnabled;
    private int themeTransitionDuration;
    private int themeTransitionSteps;

    // Bossbar settings
    private boolean bossbarEnabled;
    private int bossbarUpdateInterval;
    private Map<String, String> bossbarFormats;
    private Map<String, Map<String, Object>> bossbarCustom;

    // Actionbar settings
    private boolean actionbarEnabled;
    private boolean actionbarShowOpponent;
    private int actionbarUpdateInterval;
    private Map<String, String> actionbarFormats;

    // Sound settings
    private boolean soundsEnabled;
    private String defaultSoundProfile;
    private Map<String, SoundProfile> soundProfiles;

    // Message settings
    private String defaultMessageStyle;
    private Map<String, String> messageFormats;

    /**
     * Creates a new visual configuration instance.
     *
     * @param validator The configuration validator
     * @param config The configuration section
     */
    public VisualConfig(ConfigurationValidator validator, ConfigurationSection config) {
        super(validator, config, "visual");
    }

    @Override
    public void load() {
        // Theme settings
        ConfigurationSection themesSection = getSection("themes");
        if (themesSection != null) {
            themesEnabled = themesSection.getBoolean("enabled", true);
            defaultTheme = themesSection.getString("default-theme", "clean");
            allowCustomThemes = themesSection.getBoolean("allow-custom", true);
            availableThemes = themesSection.getStringList("available");
        } else {
            themesEnabled = true;
            defaultTheme = "clean";
            allowCustomThemes = true;
            availableThemes = List.of("minimal", "fire", "ice", "neon", "dark", "clean");
        }

        // Animation settings
        ConfigurationSection animationsSection = getSection("animations");
        if (animationsSection != null) {
            animationsEnabled = animationsSection.getBoolean("enabled", true);
            themeTransitionDuration = animationsSection.getInt("theme-transition-duration", 20);
            themeTransitionSteps = animationsSection.getInt("theme-transition-steps", 5);
        } else {
            animationsEnabled = true;
            themeTransitionDuration = 20;
            themeTransitionSteps = 5;
        }

        // Bossbar settings
        ConfigurationSection bossbarSection = getSection("bossbar");
        if (bossbarSection != null) {
            bossbarEnabled = bossbarSection.getBoolean("enabled", true);
            bossbarUpdateInterval = bossbarSection.getInt("update-interval", 1);
            bossbarFormats = new HashMap<>();
            ConfigurationSection formatsSection = bossbarSection.getConfigurationSection("formats");
            if (formatsSection != null) {
                for (String key : formatsSection.getKeys(false)) {
                    bossbarFormats.put(key, formatsSection.getString(key, ""));
                }
            }
            bossbarCustom = new HashMap<>();
            ConfigurationSection customSection = bossbarSection.getConfigurationSection("custom");
            if (customSection != null) {
                for (String key : customSection.getKeys(false)) {
                    Map<String, Object> customMap = new HashMap<>();
                    ConfigurationSection themeSection = customSection.getConfigurationSection(key);
                    if (themeSection != null) {
                        customMap.put("color", themeSection.getString("color", "RED"));
                        customMap.put("style", themeSection.getString("style", "SOLID"));
                    }
                    bossbarCustom.put(key, customMap);
                }
            }
        } else {
            bossbarEnabled = true;
            bossbarUpdateInterval = 1;
            bossbarFormats = createDefaultBossbarFormats();
            bossbarCustom = new HashMap<>();
        }

        // Actionbar settings
        ConfigurationSection actionbarSection = getSection("actionbar");
        if (actionbarSection != null) {
            actionbarEnabled = actionbarSection.getBoolean("enabled", true);
            actionbarShowOpponent = actionbarSection.getBoolean("show-opponent", true);
            actionbarUpdateInterval = actionbarSection.getInt("update-interval", 20);
            actionbarFormats = new HashMap<>();
            ConfigurationSection formatsSection = actionbarSection.getConfigurationSection("formats");
            if (formatsSection != null) {
                for (String key : formatsSection.getKeys(false)) {
                    actionbarFormats.put(key, formatsSection.getString(key, ""));
                }
            }
        } else {
            actionbarEnabled = true;
            actionbarShowOpponent = true;
            actionbarUpdateInterval = 20;
            actionbarFormats = createDefaultActionbarFormats();
        }

        // Sound settings
        ConfigurationSection soundsSection = getSection("sounds");
        if (soundsSection != null) {
            soundsEnabled = soundsSection.getBoolean("enabled", true);
            defaultSoundProfile = soundsSection.getString("profile", "default");
            soundProfiles = new HashMap<>();
            ConfigurationSection profilesSection = soundsSection.getConfigurationSection("profiles");
            if (profilesSection != null) {
                for (String profileName : profilesSection.getKeys(false)) {
                    ConfigurationSection profileSection = profilesSection.getConfigurationSection(profileName);
                    if (profileSection != null) {
                        SoundProfile profile = new SoundProfile();
                        ConfigurationSection eventsSection = profileSection.getConfigurationSection("events");
                        if (eventsSection != null) {
                            for (String eventName : eventsSection.getKeys(false)) {
                                ConfigurationSection eventSection = eventsSection.getConfigurationSection(eventName);
                                if (eventSection != null) {
                                    SoundEvent soundEvent = new SoundEvent(
                                        eventSection.getString("sound", "BLOCK_ANVIL_LAND"),
                                        eventSection.getDouble("volume", 1.0),
                                        eventSection.getDouble("pitch", 1.0)
                                    );
                                    profile.addEvent(eventName, soundEvent);
                                }
                            }
                        }
                        soundProfiles.put(profileName, profile);
                    }
                }
            }
        } else {
            soundsEnabled = true;
            defaultSoundProfile = "default";
            soundProfiles = createDefaultSoundProfiles();
        }

        // Message settings
        ConfigurationSection messagesSection = getSection("messages");
        if (messagesSection != null) {
            defaultMessageStyle = messagesSection.getString("default-style", "minimal");
            messageFormats = new HashMap<>();
            ConfigurationSection formatsSection = messagesSection.getConfigurationSection("formats");
            if (formatsSection != null) {
                for (String key : formatsSection.getKeys(false)) {
                    messageFormats.put(key, formatsSection.getString(key, ""));
                }
            }
        } else {
            defaultMessageStyle = "minimal";
            messageFormats = createDefaultMessageFormats();
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

        if (defaultTheme != null && !availableThemes.contains(defaultTheme)) {
            warnings.add(new ConfigurationValidator.ConfigValidationError("visual.themes.default-theme", 
                "Default theme '" + defaultTheme + "' not in available themes list: " + availableThemes, 
                defaultTheme, availableThemes.isEmpty() ? "default" : availableThemes.get(0), 
                ConfigurationValidator.ConfigValidationError.Severity.WARNING));
        }

        if (defaultSoundProfile != null && !soundProfiles.containsKey(defaultSoundProfile)) {
            warnings.add(new ConfigurationValidator.ConfigValidationError("visual.sounds.profile", 
                "Default sound profile '" + defaultSoundProfile + "' not found in sound profiles", 
                defaultSoundProfile, "default", ConfigurationValidator.ConfigValidationError.Severity.WARNING));
        }

        if (defaultMessageStyle != null && !messageFormats.containsKey(defaultMessageStyle)) {
            warnings.add(new ConfigurationValidator.ConfigValidationError("visual.messages.default-style", 
                "Default message style '" + defaultMessageStyle + "' not found in message formats", 
                defaultMessageStyle, "default", ConfigurationValidator.ConfigValidationError.Severity.WARNING));
        }

        return new ConfigurationValidator.ValidationResult(errors.isEmpty(), errors, warnings, info);
    }

    @Override
    public boolean isEnabled() {
        return themesEnabled || bossbarEnabled || actionbarEnabled || soundsEnabled;
    }

    // Helper methods for default configurations
    private Map<String, String> createDefaultBossbarFormats() {
        Map<String, String> formats = new HashMap<>();
        formats.put("default", "&cCombat Timer: &f{time_left}s");
        formats.put("minimal", "&7{time_left}");
        formats.put("fire", "&c🔥 &f{time_left}s &c🔥");
        formats.put("ice", "&b❄ &f{time_left}s &b❄");
        formats.put("neon", "&d✨ &f{time_left}s &d✨");
        formats.put("dark", "&8{time_left}s");
        formats.put("clean", "&aCombat: &f{time_left}s");
        return formats;
    }

    private Map<String, String> createDefaultActionbarFormats() {
        Map<String, String> formats = new HashMap<>();
        formats.put("default", "&cCombat with &f{opponent} &c- &f{time_left}s");
        formats.put("minimal", "&7Combat - {time_left}s");
        formats.put("detailed", "&c[COMBAT] &fFighting &e{opponent} &f- &a{time_left}s &fremaining");
        formats.put("funny", "&d⚔ &eLOL! &fFighting &b{opponent} &f- &c{time_left}s ⚔");
        formats.put("medieval", "&6[&4BATTLE&6] &fvs &c{opponent} &f- &e{time_left}s");
        formats.put("competitive", "&c[&4RANKED&c] &fvs &4{opponent} &f- &c{time_left}s");
        return formats;
    }

    private Map<String, String> createDefaultMessageFormats() {
        Map<String, String> formats = new HashMap<>();
        formats.put("minimal", "&7{time_left}s remaining");
        formats.put("detailed", "&cCombat with &f{opponent}&c - &f{time_left}s &cleft &f(Health: &a{health}&f/&a{max_health}&f)");
        formats.put("funny", "&d⚔ &eGet rekt &b{opponent}&e! &c{time_left}s &eleft! ⚔");
        formats.put("medieval", "&6[&4BATTLE&6] &fThou fighteth &c{opponent} &f- &e{time_left} &fseconds remaineth");
        formats.put("competitive", "&c[&4RANKED&c] &fElite duel vs &4{opponent} &f- &c{time_left}s");
        return formats;
    }

    private Map<String, SoundProfile> createDefaultSoundProfiles() {
        Map<String, SoundProfile> profiles = new HashMap<>();

        // Default profile
        SoundProfile defaultProfile = new SoundProfile();
        defaultProfile.addEvent("combat_start", new SoundEvent("BLOCK_ANVIL_LAND", 1.0f, 1.0f));
        defaultProfile.addEvent("combat_end", new SoundEvent("ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.0f));
        defaultProfile.addEvent("timer_warning", new SoundEvent("BLOCK_NOTE_BLOCK_PLING", 1.0f, 2.0f));
        defaultProfile.addEvent("timer_reset", new SoundEvent("BLOCK_LEVER_CLICK", 0.5f, 1.5f));
        defaultProfile.addEvent("interference", new SoundEvent("ENTITY_VILLAGER_NO", 1.0f, 1.0f));
        profiles.put("default", defaultProfile);

        // Subtle profile
        SoundProfile subtleProfile = new SoundProfile();
        subtleProfile.addEvent("combat_start", new SoundEvent("BLOCK_STONE_BUTTON_CLICK_ON", 0.7f, 1.2f));
        subtleProfile.addEvent("combat_end", new SoundEvent("ENTITY_ITEM_PICKUP", 0.5f, 1.0f));
        subtleProfile.addEvent("timer_warning", new SoundEvent("BLOCK_BAMBOO_BREAK", 0.6f, 1.8f));
        subtleProfile.addEvent("timer_reset", new SoundEvent("BLOCK_STONE_BUTTON_CLICK_OFF", 0.4f, 1.0f));
        subtleProfile.addEvent("interference", new SoundEvent("BLOCK_ANVIL_BREAK", 0.3f, 0.8f));
        profiles.put("subtle", subtleProfile);

        // And more profiles...

        return profiles;
    }

    // Getters
    public boolean isThemesEnabled() {
        return themesEnabled;
    }

    public String getDefaultTheme() {
        return defaultTheme;
    }

    public boolean isAllowCustomThemes() {
        return allowCustomThemes;
    }

    public List<String> getAvailableThemes() {
        return availableThemes;
    }

    public boolean isAnimationsEnabled() {
        return animationsEnabled;
    }

    public int getThemeTransitionDuration() {
        return themeTransitionDuration;
    }

    public int getThemeTransitionSteps() {
        return themeTransitionSteps;
    }

    public boolean isBossbarEnabled() {
        return bossbarEnabled;
    }

    public int getBossbarUpdateInterval() {
        return bossbarUpdateInterval;
    }

    public Map<String, String> getBossbarFormats() {
        return bossbarFormats;
    }

    public Map<String, Map<String, Object>> getBossbarCustom() {
        return bossbarCustom;
    }

    public boolean isActionbarEnabled() {
        return actionbarEnabled;
    }

    public boolean isActionbarShowOpponent() {
        return actionbarShowOpponent;
    }

    public int getActionbarUpdateInterval() {
        return actionbarUpdateInterval;
    }

    public Map<String, String> getActionbarFormats() {
        return actionbarFormats;
    }

    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }

    public String getDefaultSoundProfile() {
        return defaultSoundProfile;
    }

    public Map<String, SoundProfile> getSoundProfiles() {
        return soundProfiles;
    }

    public String getDefaultMessageStyle() {
        return defaultMessageStyle;
    }

    public Map<String, String> getMessageFormats() {
        return messageFormats;
    }

    // Inner classes for sound configuration
    public static class SoundProfile {
        private final Map<String, SoundEvent> events = new HashMap<>();

        public void addEvent(String eventName, SoundEvent event) {
            events.put(eventName, event);
        }

        public SoundEvent getEvent(String eventName) {
            return events.get(eventName);
        }

        public Map<String, SoundEvent> getEvents() {
            return events;
        }
    }

    public static class SoundEvent {
        private final String sound;
        private final double volume;
        private final double pitch;

        public SoundEvent(String sound, double volume, double pitch) {
            this.sound = sound;
            this.volume = (float) volume;
            this.pitch = (float) pitch;
        }

        public String getSound() {
            return sound;
        }

        public double getVolume() {
            return volume;
        }

        public double getPitch() {
            return pitch;
        }
    }

    @Override
    public int getLoadPriority() {
        return 2; // Load after combat config
    }
}