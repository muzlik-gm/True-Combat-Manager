package com.muzlik.pvpcombat.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration for the dual-grace-period disconnect protection system.
 *
 * Supports two separate grace periods:
 *  - "bad-internet"  : player lost connection involuntarily (short ping spike / timeout)
 *  - "intentional"   : player pressed Disconnect / closed the game window
 *
 * Repeat-logout abuse prevention: if a player disconnects more than
 * {@code repeatLogoutMaxCount} times within {@code repeatLogoutWindowSeconds},
 * they are killed immediately on the next logout (no grace period).
 */
public class DisconnectConfig extends SubConfig {

    // Master switch
    private boolean enabled;

    // ── Bad-internet grace period ──────────────────────────────────────────
    private boolean badInternetEnabled;
    private int     badInternetGraceSeconds;
    private String  badInternetDisconnectMessage;
    private String  badInternetReconnectMessage;

    // ── Intentional logout grace period ───────────────────────────────────
    private boolean intentionalEnabled;
    private int     intentionalGraceSeconds;
    private String  intentionalDisconnectMessage;
    private String  intentionalReconnectMessage;

    // ── Repeat-logout abuse prevention ────────────────────────────────────
    private boolean repeatLogoutEnabled;
    private int     repeatLogoutMaxCount;       // max disconnects before kill
    private int     repeatLogoutWindowSeconds;  // rolling window (seconds)
    private String  repeatLogoutKillMessage;    // message to player on kill
    private String  repeatLogoutOpponentMessage;// message to opponent

    // ── Punishment (applies when grace period expires without reconnect) ──
    private boolean dropInventoryOnPunish;
    private boolean killOnPunish;
    private boolean bypassTotem;
    private String  punishmentBroadcast;

    // ── Display ───────────────────────────────────────────────────────────
    private String  displayMode;               // actionbar | bossbar | scoreboard
    private String  gracePeriodFormat;         // {player} {time} {type}

    public DisconnectConfig(ConfigurationValidator validator, ConfigurationSection config) {
        super(validator, config, "combat.disconnect-protection");
    }

    @Override
    public void load() {
        // NOTE: getBoolean/getInt/getString prepend sectionName + "." automatically (relative keys).
        // getSection() does NOT prepend — pass the full absolute path for sub-sections.
        // sectionName = "combat.disconnect-protection"
        enabled = getBoolean("enabled", true);

        // Bad-internet section — full absolute path for getSection()
        ConfigurationSection bi = getSection("combat.disconnect-protection.bad-internet");
        if (bi != null) {
            badInternetEnabled           = bi.getBoolean("enabled", true);
            badInternetGraceSeconds      = bi.getInt("grace-seconds", 30);
            badInternetDisconnectMessage = bi.getString("disconnect-message",
                "&e{player} &clost connection during combat! They have &e{time}s &cto reconnect.");
            badInternetReconnectMessage  = bi.getString("reconnect-message",
                "&aYou reconnected in time! No penalty applied.");
        } else {
            badInternetEnabled           = true;
            badInternetGraceSeconds      = 30;
            badInternetDisconnectMessage = "&e{player} &clost connection during combat! They have &e{time}s &cto reconnect.";
            badInternetReconnectMessage  = "&aYou reconnected in time! No penalty applied.";
        }

        // Intentional section
        ConfigurationSection it = getSection("combat.disconnect-protection.intentional");
        if (it != null) {
            intentionalEnabled           = it.getBoolean("enabled", true);
            intentionalGraceSeconds      = it.getInt("grace-seconds", 10);
            intentionalDisconnectMessage = it.getString("disconnect-message",
                "&e{player} &cdisconnected during combat! They have &e{time}s &cto reconnect or be punished.");
            intentionalReconnectMessage  = it.getString("reconnect-message",
                "&aYou reconnected in time! No penalty applied.");
        } else {
            intentionalEnabled           = true;
            intentionalGraceSeconds      = 10;
            intentionalDisconnectMessage = "&e{player} &cdisconnected during combat! They have &e{time}s &cto reconnect or be punished.";
            intentionalReconnectMessage  = "&aYou reconnected in time! No penalty applied.";
        }

        // Repeat-logout section
        ConfigurationSection rl = getSection("combat.disconnect-protection.repeat-logout");
        if (rl != null) {
            repeatLogoutEnabled         = rl.getBoolean("enabled", true);
            repeatLogoutMaxCount        = rl.getInt("max-count", 2);
            repeatLogoutWindowSeconds   = rl.getInt("window-seconds", 240);
            repeatLogoutKillMessage     = rl.getString("kill-message",
                "&cYou were killed for repeatedly combat-logging!");
            repeatLogoutOpponentMessage = rl.getString("opponent-message",
                "&a{player} &ewas killed for repeatedly combat-logging!");
        } else {
            repeatLogoutEnabled         = true;
            repeatLogoutMaxCount        = 2;
            repeatLogoutWindowSeconds   = 240;
            repeatLogoutKillMessage     = "&cYou were killed for repeatedly combat-logging!";
            repeatLogoutOpponentMessage = "&a{player} &ewas killed for repeatedly combat-logging!";
        }

        // Punishment section
        ConfigurationSection pun = getSection("combat.disconnect-protection.punishment");
        if (pun != null) {
            dropInventoryOnPunish = pun.getBoolean("drop-inventory", true);
            killOnPunish          = pun.getBoolean("kill-on-punish", false);
            bypassTotem           = pun.getBoolean("bypass-totem", true);
            punishmentBroadcast   = pun.getString("broadcast",
                "&c{player} &ecombat-logged and was punished!");
        } else {
            dropInventoryOnPunish = true;
            killOnPunish          = false;
            bypassTotem           = true;
            punishmentBroadcast   = "&c{player} &ecombat-logged and was punished!";
        }

        // Flat keys — relative, SubConfig prepends "combat.disconnect-protection." automatically
        displayMode       = getString("display-mode", "actionbar");
        gracePeriodFormat = getString("grace-period-format",
            "&e{player} &7has &c{time}s &7to reconnect &8({type})");
    }

    @Override
    public void reload() {
        load();
    }

    @Override
    public ConfigurationValidator.ValidationResult validate() {
        java.util.List<ConfigurationValidator.ConfigValidationError> errors   = new java.util.ArrayList<>();
        java.util.List<ConfigurationValidator.ConfigValidationError> warnings = new java.util.ArrayList<>();
        java.util.List<ConfigurationValidator.ConfigValidationError> info     = new java.util.ArrayList<>();

        if (badInternetGraceSeconds < 0) {
            errors.add(new ConfigurationValidator.ConfigValidationError(
                "combat.disconnect-protection.bad-internet.grace-seconds",
                "Grace seconds cannot be negative", badInternetGraceSeconds, 30,
                ConfigurationValidator.ConfigValidationError.Severity.ERROR));
        }
        if (intentionalGraceSeconds < 0) {
            errors.add(new ConfigurationValidator.ConfigValidationError(
                "combat.disconnect-protection.intentional.grace-seconds",
                "Grace seconds cannot be negative", intentionalGraceSeconds, 10,
                ConfigurationValidator.ConfigValidationError.Severity.ERROR));
        }
        if (repeatLogoutMaxCount < 1) {
            warnings.add(new ConfigurationValidator.ConfigValidationError(
                "combat.disconnect-protection.repeat-logout.max-count",
                "max-count should be at least 1", repeatLogoutMaxCount, 2,
                ConfigurationValidator.ConfigValidationError.Severity.WARNING));
        }
        if (repeatLogoutWindowSeconds < 30) {
            warnings.add(new ConfigurationValidator.ConfigValidationError(
                "combat.disconnect-protection.repeat-logout.window-seconds",
                "window-seconds is very short (< 30s)", repeatLogoutWindowSeconds, 240,
                ConfigurationValidator.ConfigValidationError.Severity.WARNING));
        }

        return new ConfigurationValidator.ValidationResult(errors.isEmpty(), errors, warnings, info);
    }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public int getLoadPriority() { return 2; }

    // ── Getters ────────────────────────────────────────────────────────────

    public boolean isBadInternetEnabled()          { return badInternetEnabled; }
    public int     getBadInternetGraceSeconds()     { return badInternetGraceSeconds; }
    public String  getBadInternetDisconnectMessage(){ return badInternetDisconnectMessage; }
    public String  getBadInternetReconnectMessage() { return badInternetReconnectMessage; }

    public boolean isIntentionalEnabled()           { return intentionalEnabled; }
    public int     getIntentionalGraceSeconds()     { return intentionalGraceSeconds; }
    public String  getIntentionalDisconnectMessage(){ return intentionalDisconnectMessage; }
    public String  getIntentionalReconnectMessage() { return intentionalReconnectMessage; }

    public boolean isRepeatLogoutEnabled()          { return repeatLogoutEnabled; }
    public int     getRepeatLogoutMaxCount()        { return repeatLogoutMaxCount; }
    public int     getRepeatLogoutWindowSeconds()   { return repeatLogoutWindowSeconds; }
    public String  getRepeatLogoutKillMessage()     { return repeatLogoutKillMessage; }
    public String  getRepeatLogoutOpponentMessage() { return repeatLogoutOpponentMessage; }

    public boolean isDropInventoryOnPunish()        { return dropInventoryOnPunish; }
    public boolean isKillOnPunish()                 { return killOnPunish; }
    public boolean isBypassTotem()                  { return bypassTotem; }
    public String  getPunishmentBroadcast()         { return punishmentBroadcast; }

    public String  getDisplayMode()                 { return displayMode; }
    public String  getGracePeriodFormat()           { return gracePeriodFormat; }
}
