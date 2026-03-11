# TrueCombatManager

[![Version](https://img.shields.io/badge/version-1.1.0-blue.svg)](https://github.com/muzlik-gm/Combat-Manager)
[![Minecraft](https://img.shields.io/badge/minecraft-1.19.4--1.21+-green.svg)](https://www.spigotmc.org/)
[![Java](https://img.shields.io/badge/java-21-orange.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-MIT-yellow.svg)](LICENSE)

**Professional PvP Combat Management for Minecraft Servers**

A lightweight, high-performance combat plugin with persistent statistics, real-time tracking, and comprehensive admin tools. Built for modern Minecraft servers running Spigot, Paper, or Purpur.

---

## ⚡ Key Features

### Combat System
▸ Real-time combat tracking with configurable duration  
▸ Automatic combat tagging on player damage  
▸ Combat logging protection (instant death on logout)  
▸ Timer reset on new damage  
▸ Lag compensation system  

### Database & Statistics
▸ SQLite database (default, zero setup)  
▸ MySQL support for multi-server networks  
▸ Persistent player statistics across restarts  
▸ Weapon-specific damage tracking  
▸ Combat history and session replays  

### Protection Systems
▸ Newbie protection for unarmored players  
▸ Timed immunity system with admin commands  
▸ WorldGuard safe zone integration  
▸ Configurable bypass permissions  

### Visual Feedback
▸ Customizable BossBar with 6 themes  
▸ ActionBar timer display  
▸ Sound effects for combat events  
▸ Progress indicators  

### Restrictions
▸ Block ender pearls during combat  
▸ Prevent elytra usage  
▸ Restrict trident throwing  
▸ Block golden apple consumption  
▸ Prevent command usage  
▸ Disable teleportation  

### Admin Tools
▸ `/combat reload` - Instant config reload without restart  
▸ `/combat protection <player> <seconds>` - Grant immunity  
▸ `/combat clear <player>` - Force-end combat  
▸ `/combat stats` - Server-wide statistics  
▸ `/combat logging <on|off>` - Toggle console logging  
▸ `/combat inspect <player>` - Real-time combat status  

### Cross-Server Support
▸ BungeeCord/Velocity network sync  
▸ Prevent server-hopping during combat  
▸ Shared combat state across network  
▸ Automatic plugin messaging  

---

## 📋 Requirements

**Server Software:** Spigot, Paper, or Purpur  
**Minecraft Version:** 1.19.4 - 1.21+  
**Java Version:** 21 or higher  

**Optional Dependencies:**  
▸ PlaceholderAPI - For placeholder support  
▸ WorldGuard - For safe zone protection  

---

## 🚀 Installation

1. Download `TrueCombatManager-1.1.0.jar`
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
▸ Added newbie protection system with admin commands  
▸ Optimized JAR size by 83% (14.6MB → 2.4MB)  
▸ Added WorldGuard safe zone integration  
▸ Enhanced admin tools and commands  
▸ Improved default configuration  
▸ Fixed memory leaks and performance issues  

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
/combat reload                      - Reload configuration
/combat inspect <player>            - Inspect player combat status
/combat clear <player>              - Force-end combat
/combat stats                       - View server statistics
/combat logging <on|off>            - Toggle console logging
/combat protection <player> <time>  - Grant timed immunity
/replay view <session-id>           - View combat replay
```

---

## 🔧 Configuration

The plugin includes three preset configurations:

▸ **Casual Survival** - Relaxed PvP settings  
▸ **Balanced PvP** - Default competitive settings  
▸ **Hardcore PvP** - Strict combat rules  

All settings reload instantly with `/combat reload` - no restart needed!

---

## 🌐 Network Setup

For multi-server networks:

1. Install plugin on each backend server
2. Enable cross-server sync in config:
   ```yaml
   integration:
     cross-server:
       enabled: true
   ```
3. Plugin automatically syncs through BungeeCord/Velocity

---

## 📈 Performance

▸ Optimized JAR size: 2.4 MB  
▸ Zero lag with intelligent caching  
▸ Thread-safe concurrent operations  
▸ Efficient database connection pooling  
▸ Minimal memory footprint  

---

## 🔗 Links

**GitHub:** https://github.com/muzlik-gm/Combat-Manager  
**Issues:** https://github.com/muzlik-gm/Combat-Manager/issues  
**Author:** muzlik  

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Made with ❤️ for the Minecraft community**

- [Installation](#-installation)
- [Configuration](#-configuration)
- [Commands](#-commands)
- [Permissions](#-permissions)
- [PlaceholderAPI](#-placeholderapi)
- [Dependencies](#-dependencies)
- [Support](#-support)

---

## 🎮 Features

### Combat Management
- ✅ Real-time combat tracking
- ✅ Configurable combat duration (default: 30s)
- ✅ Lag compensation system
- ✅ Combat logging protection (instant death on logout)
- ✅ Automatic combat end on death
- ✅ Combat forfeit system

### Newbie Protection
- ✅ Protects players without armor
- ✅ XP level threshold (configurable)
- ✅ Prevents damage dealing AND receiving
- ✅ Bypass permission support
- ✅ Customizable messages

### Restriction Systems
- ✅ **Tridents** - Block throwing and riptide in combat
- ✅ **Ender Pearls** - Block usage and safezone teleportation
- ✅ **Respawn Anchors** - Block usage during combat
- ✅ **Elytra** - Block gliding and firework boosts
- ✅ **End Crystals** - Block placement/breaking
- ✅ **Golden Apples** - Configurable cooldowns
- ✅ **Commands** - Block teleport commands
- ✅ **Safezones** - Prevent entry during combat with visual barriers

### Visual System
- ✅ 6 Built-in Themes (Default, Minimal, Intense, Elegant, Neon, Retro)
- ✅ BossBar display with timer
- ✅ ActionBar notifications
- ✅ Sound effects (configurable)
- ✅ Client-side glass barriers (ProtocolLib)
- ✅ Per-player style preferences

### Statistics & Tracking
- ✅ Wins/Losses tracking
- ✅ K/D ratio calculation
- ✅ Damage dealt/received
- ✅ Combat time tracking
- ✅ Knockback exchanges
- ✅ PlaceholderAPI integration

### Performance
- ✅ Optimized for zero lag
- ✅ Intelligent logging system
- ✅ Async operations
- ✅ Efficient caching
- ✅ Minimal memory footprint

---

## 📦 Installation

### Requirements
- **Minecraft:** 1.19.4 - 1.21+ (Paper/Spigot)
- **Java:** 21+
- **Platform:** Windows x64, Linux x64, or Linux ARM64 (Docker)

### What's Included
The plugin is fully self-contained with all essential dependencies:
- ✅ SQLite database support (Windows/Linux natives included)
- ✅ HikariCP connection pooling
- ✅ High-performance caching
- ✅ JSON configuration handling

### Optional Dependencies
- **PlaceholderAPI** (2.11+) - For placeholders
- **WorldGuard** (7.0+) - For safezone protection
- **MySQL Connector** (8.2.0+) - If using MySQL instead of SQLite

### Version Compatibility
TrueCombatManager automatically detects your Minecraft version and adapts accordingly:
- ✅ **1.19.4 - 1.20.6** - Fully supported
- ✅ **1.21+** - Fully supported
- ⚠️ **Older versions** - Warning displayed, may have issues
- ⚠️ **Newer versions** - Warning displayed, untested

### Installation Steps
1. Download the latest release (2.4 MB)
2. Place `TrueCombatManager-1.0.2.jar` in your `plugins/` folder
3. Restart your server
4. Configure `plugins/TrueCombatManager/config.yml`
5. Reload with `/combat reload`

No additional downloads needed! See [INSTALLATION.md](INSTALLATION.md) for details.

### Testing
TrueCombatManager includes comprehensive property-based tests:
- ✅ 8 property tests with 1000+ iterations each
- ✅ Combat session uniqueness and consistency
- ✅ Timer reset accuracy
- ✅ Thread-safe concurrent access
- ✅ Damage tracking accuracy
- ✅ K/D ratio calculations
- ✅ Win rate validation
- ✅ Combat time accumulation

---

## ⚙️ Configuration

### Quick Start

```yaml
# Enable/disable the plugin
general:
  enabled: true

# Combat duration in seconds
combat:
  duration: 30

# Newbie protection
newbie-protection:
  enabled: true
  xp-level-threshold: 3
  require-any-armor: true

# Console logging (disable for best performance)
logging:
  console-enabled: false
```

### Key Configuration Sections

#### Combat Settings
```yaml
combat:
  duration: 30                    # Combat duration in seconds
  allow-flight: false             # Allow flight during combat
  cancel-on-death: true           # End combat on death
```

#### Newbie Protection
```yaml
newbie-protection:
  enabled: true
  prevent-damage-dealing: true    # Newbies can't attack
  prevent-damage-receiving: true  # Newbies can't be attacked
  xp-level-threshold: 3           # Players with >3 XP not protected
  require-any-armor: true         # Need at least 1 armor piece
```

#### Restrictions
```yaml
restrictions:
  trident:
    enabled: true                 # Block tridents in combat
  
  enderpearl:
    enabled: true
    block-usage: true             # Block ender pearls
  
  respawn-anchor:
    enabled: true                 # Block respawn anchors
  
  elytra:
    enabled: true
    block-glide: true             # Block elytra gliding
  
  teleport:
    enabled: true
    blocked-commands:             # Commands to block
      - "tp"
      - "home"
      - "spawn"
```

#### Safezone Protection
```yaml
restrictions:
  safezone:
    enabled: true
    block-entry: true             # Block safezone entry
    protected-regions:            # WorldGuard regions
      - "spawn"
      - "safezone"
    barrier:
      material: "GLASS"           # Barrier block type
      height: 4                   # Barrier height
```

---

## 🎮 Commands

### Player Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/combat status` | Check your combat status | `pvpcombat.command.status` |
| `/combat summary` | View your combat statistics | `pvpcombat.command.summary` |
| `/combat toggle-style` | Change visual theme | `pvpcombat.command.toggle-style` |

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/combat inspect <player>` | Inspect player's combat status | `pvpcombat.admin.inspect` |
| `/combat reload` | Reload configuration | `pvpcombat.admin` |
| `/combat debug` | Toggle debug mode | `pvpcombat.admin.debug` |
| `/combat logging <enabled\|disabled>` | Control console logging | `pvpcombat.admin` |
| `/combat stats` | View server-wide statistics | `pvpcombat.admin` |
| `/combat clear <player>` | Force-end combat for a player | `pvpcombat.admin` |
| `/combat protection <player> <seconds>` | Grant temporary protection | `pvpcombat.admin` |

---

## 🔐 Permissions

### Player Permissions
- `pvpcombat.command.status` - Use /combat status (default: true)
- `pvpcombat.command.summary` - Use /combat summary (default: true)
- `pvpcombat.command.toggle-style` - Change visual theme (default: true)

### Admin Permissions
- `pvpcombat.admin` - Access all admin commands (default: op)
- `pvpcombat.admin.inspect` - Inspect players (default: op)
- `pvpcombat.admin.debug` - Toggle debug mode (default: op)

### Bypass Permissions
- `pvpcombat.bypass.combatlog` - Bypass combat logging restrictions (default: op)
- `pvpcombat.bypass.restrictions` - Bypass all restrictions (default: op)
- `pvpcombat.bypass.newbie` - Bypass newbie protection (default: op)

---

## 📊 PlaceholderAPI

### Combat Status
- `%pvpcombat_in_combat%` - true/false
- `%pvpcombat_time_left%` - Remaining seconds
- `%pvpcombat_opponent%` - Opponent name

### Lifetime Statistics
- `%pvpcombat_wins%` - Total wins
- `%pvpcombat_losses%` - Total losses
- `%pvpcombat_total_combats%` - Total combats
- `%pvpcombat_kd_ratio%` - K/D ratio
- `%pvpcombat_win_rate%` - Win rate percentage
- `%pvpcombat_total_damage_dealt%` - Total damage dealt
- `%pvpcombat_total_damage_received%` - Total damage received

### Session Statistics
- `%pvpcombat_session_damage_dealt%` - Damage in current fight
- `%pvpcombat_session_damage_received%` - Damage received in current fight
- `%pvpcombat_knockback_exchanges%` - Knockback exchanges

---

## 🔌 Dependencies

### Required
- **Spigot/Paper** 1.20.4+
- **Java** 21+

### Optional (Recommended)
- **ProtocolLib** 5.0+ - For client-side barriers
- **WorldGuard** 7.0+ - For safezone protection
- **PlaceholderAPI** 2.11+ - For placeholders

### Optional (Integrations)
- **Citizens** - NPC combat support
- **CombatLogX** - Integration support
- **MythicMobs** - Mob combat support

---

## 🎨 Visual Themes

### Available Themes
1. **Default** - Classic red/yellow theme
2. **Minimal** - Clean gray theme
3. **Intense** - Bold red/orange theme
4. **Elegant** - Sophisticated purple theme
5. **Neon** - Bright cyan/pink theme
6. **Retro** - Vintage green/yellow theme

### Customization
Players can switch themes with `/combat toggle-style`

---

## 🚀 Performance

### Optimizations
- ✅ **Zero Lag** - Optimized for high-performance servers
- ✅ **Async Operations** - Non-blocking database operations
- ✅ **Smart Caching** - Efficient data caching
- ✅ **Intelligent Logging** - No performance impact when disabled
- ✅ **Minimal Memory** - Low memory footprint

### Logging Control
```bash
# Disable logging for best performance (default)
/combat logging disabled

# Enable logging for debugging
/combat logging enabled
```

---

## 🛠️ Advanced Features

### Combat Logging Protection
- Players who logout during combat are instantly killed
- Inventory drops on death
- Opponent receives win credit
- Broadcast message to server

### Lag Compensation
- Automatic TPS monitoring
- Combat timer adjustment based on lag
- Fair combat duration regardless of server performance

### Cross-Server Support
- BungeeCord/Velocity integration
- Combat state synchronization
- Cross-server combat tracking

---

## 📝 Configuration Examples

### Hardcore PvP Server
```yaml
combat:
  duration: 60                    # Longer combat
newbie-protection:
  enabled: false                  # No protection
restrictions:
  trident:
    enabled: true                 # Block all items
  enderpearl:
    enabled: true
  elytra:
    enabled: true
```

### Casual/Friendly Server
```yaml
combat:
  duration: 15                    # Shorter combat
newbie-protection:
  enabled: true                   # Protect newbies
  xp-level-threshold: 10          # Higher threshold
restrictions:
  trident:
    enabled: false                # Allow items
  enderpearl:
    enabled: false
```

---

## 🐛 Troubleshooting

### Common Issues

**Issue:** Newbie protection not working
- Check `newbie-protection.enabled: true`
- Verify player has no armor equipped
- Check player XP level is below threshold
- Ensure player doesn't have bypass permission

**Issue:** Barriers not showing
- Install ProtocolLib
- Check `restrictions.safezone.barrier.enabled: true`
- Verify WorldGuard regions are configured

**Issue:** Server lag
- Disable console logging: `/combat logging disabled`
- Check `logging.console-enabled: false` in config
- Reduce combat duration if needed

---

## 📞 Support

### Getting Help
- **Discord:** [Join our Discord](https://discord.gg/yourserver)
- **Issues:** [GitHub Issues](https://github.com/yourusername/TrueCombatManager/issues)
- **Wiki:** [Documentation](https://github.com/yourusername/TrueCombatManager/wiki)

### Reporting Bugs
1. Check if issue already exists
2. Provide server version and plugin version
3. Include relevant config sections
4. Attach console errors (if any)
5. Describe steps to reproduce

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Credits

**Developer:** muzlik  
**Contributors:** [List contributors]  
**Special Thanks:** Paper team, ProtocolLib, WorldGuard

---

## 🔄 Changelog

### v1.0.2 (2025-12-24)
- ✅ Enhanced admin commands (stats, clear, protection)
- ✅ Version compatibility layer (1.19.4 - 1.21+)
- ✅ Comprehensive property-based testing (8 tests, 1000+ iterations)
- ✅ Configuration documentation with examples
- ✅ Performance optimizations verified
- ✅ Production-ready release

### v1.0.0 (2025-11-30)
- ✅ Initial release
- ✅ Complete combat management system
- ✅ Newbie protection
- ✅ All restriction systems
- ✅ Visual themes
- ✅ Statistics tracking
- ✅ Performance optimizations
- ✅ Console logging control

---

**Made with ❤️ for the Minecraft community**
