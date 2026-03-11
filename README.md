# True Combat Manager

[![Version](https://img.shields.io/badge/version-1.1.0-blue.svg)](https://github.com/muzlik-gm/True-Combat-Manager)
[![Minecraft](https://img.shields.io/badge/minecraft-1.18--1.21-green.svg)](https://www.spigotmc.org/)
[![Java](https://img.shields.io/badge/java-17+-orange.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-All%20Rights%20Reserved-red.svg)](LICENSE)

**Professional PvP Combat Management for Minecraft Servers**

A lightweight, high-performance combat plugin with persistent statistics, real-time tracking, and comprehensive admin tools. Built for modern Minecraft servers running Spigot, Paper, or Purpur.

---

## ⚡ Key Features

### ▸ Combat System
Real-time combat tracking with configurable duration • Automatic combat tagging on player damage • Combat logging protection (instant death on logout) • Timer reset on new damage • Lag compensation system

### ▸ Database & Statistics
SQLite database (default, zero setup) • MySQL support for multi-server networks • Persistent player statistics across restarts • Weapon-specific damage tracking • Combat history and session replays

### ▸ Protection Systems
Newbie protection for unarmored players • Timed immunity system with admin commands • WorldGuard safe zone integration • Configurable bypass permissions

### ▸ Visual Feedback
Customizable BossBar with 6 themes • ActionBar timer display • Sound effects for combat events • Progress indicators • HEX color support

### ▸ Restrictions
Block ender pearls during combat • Prevent elytra usage • Restrict trident throwing • Block golden apple consumption • Prevent command usage • Disable teleportation

### ▸ Admin Tools
`/combat reload` - Instant config reload without restart • `/combat protection <player> <seconds>` - Grant immunity • `/combat clear <player>` - Force-end combat • `/combat stats` - Server-wide statistics • `/combat logging <on|off>` - Toggle console logging • `/combat inspect <player>` - Real-time combat status

### ▸ Cross-Server Support
BungeeCord/Velocity network sync • Prevent server-hopping during combat • Shared combat state across backend servers • Automatic plugin messaging

---

## 📋 Requirements

**Server Software:** Spigot, Paper, or Purpur  
**Minecraft Version:** 1.18 - 1.21+  
**Java Version:** 17 or higher (21 recommended)

**Optional Dependencies:**  
▸ PlaceholderAPI 2.11+ - For placeholder support  
▸ WorldGuard 7.0+ - For safe zone protection  
▸ ProtocolLib 5.0+ - For client-side barriers

---

## 🚀 Installation

1. Download `TrueCombatManager-1.1.0.jar` (2.4 MB)
2. Place in your server's `plugins` folder
3. Restart your server
4. Configure in `plugins/TrueCombatManager/config.yml`
5. Use `/combat reload` to apply changes

**No additional setup required** - SQLite database works out of the box!

For detailed setup instructions, see [INSTALLATION.md](INSTALLATION.md)

---

## 📊 What's New in v1.1.0

▸ Added SQLite/MySQL database for persistent statistics  
▸ Fixed config reload bug - all settings update instantly  
▸ Optimized JAR size by 83.5% (14.6MB → 2.4MB)  
▸ Enhanced admin tools and commands  
▸ Improved default configuration  
▸ Fixed memory leaks and performance issues  
▸ Added more PvP settings and restrictions

See [CHANGELOG.md](CHANGELOG.md) for full version history.

---

## 🎮 Commands

### Player Commands
```
/combat status          - View your combat status
/combat summary         - View your combat statistics
/combat toggle-style    - Change visual theme
```

### Admin Commands
```
/combat reload                      - Reload configuration in real-time
/combat inspect <player>            - Inspect player combat status
/combat clear <player>              - Force-end combat
/combat stats                       - View server statistics
/combat logging <on|off>            - Toggle console logging
/combat protection <player> <time>  - Grant timed immunity
/replay view <session-id>           - View combat replay
```

---

## 🔐 Permissions

### Player Permissions (default: true)
▸ `pvpcombat.command.status` - Use /combat status  
▸ `pvpcombat.command.summary` - View statistics  
▸ `pvpcombat.command.toggle-style` - Change theme

### Admin Permissions (default: op)
▸ `pvpcombat.admin` - All admin commands  
▸ `pvpcombat.admin.inspect` - Inspect players  
▸ `pvpcombat.admin.debug` - Debug mode

### Bypass Permissions (default: op)
▸ `pvpcombat.bypass.combatlog` - Bypass combat logging  
▸ `pvpcombat.bypass.restrictions` - Bypass all restrictions  
▸ `pvpcombat.bypass.newbie` - Bypass newbie protection  
▸ `pvpcombat.bypass.server-switch` - Bypass server switch prevention

---

## 🔧 Configuration

The plugin includes comprehensive configuration with 100+ options:

### Combat Settings
```yaml
combat:
  duration: 30                    # Combat duration in seconds
  allow-flight: false             # Allow flight during combat
  cancel-on-death: true           # End combat on death
```

### Newbie Protection
```yaml
newbie-protection:
  enabled: true
  prevent-damage-dealing: true    # Newbies can't attack
  prevent-damage-receiving: true  # Newbies can't be attacked
  xp-level-threshold: 3           # Players with >3 XP not protected
  require-any-armor: true         # Need at least 1 armor piece
```

### Database Configuration
```yaml
database:
  type: "SQLITE"                  # SQLITE or MYSQL
  mysql:
    host: "localhost"
    port: 3306
    database: "pvpcombat"
    username: "root"
    password: "password"
```

### Restrictions
```yaml
restrictions:
  trident:
    enabled: true                 # Block tridents in combat
  enderpearl:
    enabled: true
    cooldown: 10                  # Base cooldown (seconds)
    combat-cooldown-multiplier: 2.0
  golden-apple:
    cooldown: 3
    combat-cooldown-multiplier: 1.5
  teleport:
    enabled: true
    blocked-commands:
      - "tp"
      - "home"
      - "spawn"
      - "warp"
```

### Visual System
```yaml
visual:
  themes:
    default-theme: "default"      # default, minimal, intense, elegant, neon, retro
  bossbar:
    enabled: true
  actionbar:
    enabled: true
  sounds:
    profile: "default"            # default, subtle, intense, calm, electronic
```

All settings reload instantly with `/combat reload` - no restart needed!

---

## 📊 PlaceholderAPI

### Combat Status
▸ `%pvpcombat_in_combat%` - true/false  
▸ `%pvpcombat_time_left%` - Remaining seconds  
▸ `%pvpcombat_opponent%` - Opponent name

### Lifetime Statistics
▸ `%pvpcombat_wins%` - Total wins  
▸ `%pvpcombat_losses%` - Total losses  
▸ `%pvpcombat_total_combats%` - Total combats  
▸ `%pvpcombat_kd_ratio%` - K/D ratio  
▸ `%pvpcombat_win_rate%` - Win rate percentage  
▸ `%pvpcombat_total_damage_dealt%` - Total damage dealt  
▸ `%pvpcombat_total_damage_received%` - Total damage received

### Session Statistics
▸ `%pvpcombat_session_damage_dealt%` - Damage in current fight  
▸ `%pvpcombat_session_damage_received%` - Damage received  
▸ `%pvpcombat_knockback_exchanges%` - Knockback exchanges

---

## 🌐 Network Setup

For multi-server networks:

1. Install plugin on each backend Spigot/Paper server
2. Configure MySQL database (shared across network)
3. Enable cross-server sync in config:
   ```yaml
   integration:
     cross-server:
       enabled: true
   ```
4. Plugin automatically syncs through BungeeCord/Velocity

**Note:** Plugin runs on backend servers, not on the proxy itself.

---

## 📈 Performance

▸ Optimized JAR size: 2.4 MB  
▸ Zero lag with intelligent caching  
▸ Thread-safe concurrent operations  
▸ Efficient database connection pooling  
▸ Minimal memory footprint  
▸ Async operations for heavy tasks  
▸ Lag compensation system (TPS monitoring)

---

## 🐛 Troubleshooting

### Server Lag?
→ Disable console logging: `/combat logging disabled`  
→ Check config: `logging.console-enabled: false`

### Newbie Protection Not Working?
→ Check player has NO armor equipped (all 4 slots empty)  
→ Verify player XP level ≤ threshold (default: 3)  
→ Check player doesn't have `pvpcombat.bypass.newbie` permission

### Barriers Not Showing?
→ Install ProtocolLib 5.0+  
→ Check config: `restrictions.safezone.barrier.enabled: true`  
→ Verify WorldGuard regions are configured

### Config Changes Not Applying?
→ Use `/combat reload` instead of server restart  
→ Check console for any config errors  
→ Verify YAML syntax is correct

---

## 🎨 Visual Themes

Players can switch between 6 built-in themes with `/combat toggle-style`:

1. **Default** - Classic red/yellow theme
2. **Minimal** - Clean gray theme
3. **Intense** - Bold red/orange theme
4. **Elegant** - Sophisticated purple theme
5. **Neon** - Bright cyan/pink theme
6. **Retro** - Vintage green/yellow theme

All themes support full HEX color customization in the config.

---

## 🔗 Links

**GitHub:** https://github.com/muzlik-gm/True-Combat-Manager  
**Issues:** https://github.com/muzlik-gm/True-Combat-Manager/issues  
**Changelog:** [CHANGELOG.md](CHANGELOG.md)  
**Installation Guide:** [INSTALLATION.md](INSTALLATION.md)

---

## 📞 Support

Need help? Found a bug? Have a feature request?

▸ Open an issue on [GitHub Issues](https://github.com/muzlik-gm/True-Combat-Manager/issues)  
▸ Provide server version, plugin version, and config  
▸ Include console errors if applicable  
▸ Describe steps to reproduce

---

## 📄 License

**All Rights Reserved © 2025 muzlik**

This is proprietary software. Unauthorized copying, distribution, modification, or use is strictly prohibited.

---

## 🙏 Credits

**Developer:** muzlik  
**Special Thanks:** Paper team, ProtocolLib, WorldGuard, PlaceholderAPI

---

**Made with care for the Minecraft community**
