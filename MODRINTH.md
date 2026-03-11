# True Combat Manager

**Professional PvP Combat Management for Minecraft Servers**

A lightweight, high-performance combat plugin with persistent statistics, real-time tracking, and comprehensive admin tools. Built for modern Minecraft servers running Spigot, Paper, or Purpur.

---

## ⚡ Why Choose True Combat Manager?

▸ **Zero Lag** - Optimized for high-performance servers with async operations  
▸ **Database Support** - SQLite (default) and MySQL for persistent statistics  
▸ **Real-Time Reload** - Update config without restart using `/combat reload`  
▸ **Newbie Protection** - Automatically protects players without armor  
▸ **Combat Logging Protection** - Instant death with full inventory drop  
▸ **Smart Restrictions** - Block tridents, pearls, gapples, elytra, teleports  
▸ **Visual Feedback** - 6 themes, BossBar, ActionBar, 5 sound profiles  
▸ **Comprehensive Statistics** - Wins, losses, K/D, damage, accuracy  
▸ **PlaceholderAPI** - 15+ placeholders for scoreboards  
▸ **Cross-Server Support** - BungeeCord/Velocity compatibility  
▸ **Optimized Size** - Only 2.4MB JAR file

---

## 🎮 Core Features

### Combat Management
▸ Real-time combat detection and tracking  
▸ Configurable combat duration (default 30s)  
▸ Automatic timer reset on damage  
▸ Lag compensation system (TPS monitoring)  
▸ Combat logging protection (instant death on logout)  
▸ Automatic combat end on death  
▸ Thread-safe architecture

### Newbie Protection System
▸ Protects players without armor from PvP  
▸ XP level threshold (configurable, default level 3)  
▸ Prevents damage dealing by newbies  
▸ Prevents damage receiving by newbies  
▸ Requires at least 1 armor piece (configurable)  
▸ Bypass permission support  
▸ Custom messages for attackers and defenders

### Smart Restrictions (All Configurable)
▸ **Tridents** - Block throwing and riptide enchantment  
▸ **Ender Pearls** - Block usage with cooldowns (10s base, 20s combat)  
▸ **Respawn Anchors** - Block usage during combat  
▸ **Elytra** - Block gliding and firework boosts  
▸ **End Crystals** - Block placement/breaking  
▸ **Golden Apples** - Configurable cooldowns (3s base, 4.5s combat)  
▸ **Enchanted Golden Apples** - Separate cooldowns (4s base, 8s combat)  
▸ **Commands** - Block teleport commands (/tp, /home, /spawn, /warp, /tpa, /back)  
▸ **Safezones** - Prevent entry with visual glass barriers  
▸ **Creative Mode** - Auto-switch to survival during combat  
▸ **Block Breaking/Placing** - Optional restrictions

### Visual System
▸ **6 Built-in Themes:** default, minimal, intense, elegant, neon, retro  
▸ **BossBar Timer** - Dynamic countdown with smooth animations  
▸ **ActionBar Updates** - Real-time opponent and timer information  
▸ **Color-coded Urgency** - Green → yellow → red  
▸ **Live Theme Switching** - Change style mid-combat with `/combat toggle-style`  
▸ **HEX Color Support** - Full RGB color customization  
▸ **5 Sound Profiles** - default, subtle, intense, calm, electronic  
▸ **Event Sounds** - Combat start/end, timer warning, timer reset

### Statistics & Tracking
▸ Wins/Losses tracking  
▸ K/D ratio calculation  
▸ Win rate percentage  
▸ Damage dealt/received (precise to 0.1 hearts)  
▸ Hits landed vs total hits  
▸ Accuracy percentage  
▸ Combat duration tracking  
▸ Total combat time  
▸ Last combat timestamp  
▸ PlaceholderAPI integration (15+ placeholders)

### Database Support
▸ **SQLite** - Local database for single-server setups (default)  
▸ **MySQL** - Network-wide statistics for multi-server networks  
▸ Automatic data migration  
▸ Optimized for Windows, Linux, and Docker environments  
▸ Persistent player statistics across restarts

### Combat Replay System
▸ Timeline recording of all combat events  
▸ Hybrid storage (memory + compressed files)  
▸ Replay playback with admin commands  
▸ Event filtering and search  
▸ Configurable retention (default 30 days)  
▸ Memory-efficient with auto cleanup

### Safezone Protection
▸ WorldGuard integration  
▸ Prevent entry during combat  
▸ Visual glass barriers at boundaries  
▸ Client-side rendering (no world modification)  
▸ Configurable barrier material  
▸ Sound and visual feedback

### Performance Optimized
▸ **Zero Lag** - Optimized for high-performance servers  
▸ **Intelligent Logging** - Disable for best performance  
▸ **Async Operations** - Combat logging, stats calculation off main thread  
▸ **Efficient Caching** - Player data, restrictions, combat state  
▸ **Minimal Memory** - Configurable thread pool (default 4 threads)  
▸ **Auto Cleanup** - Automatic cache cleanup  
▸ **Lag Compensation** - Auto-adjusts timers when TPS drops below 18.0  
▸ **Optimized JAR** - Only 2.4MB (83.5% smaller than v1.0)

### Cross-Server Support
▸ BungeeCord/Velocity compatibility  
▸ Combat state synchronization across backend servers  
▸ Server switch prevention during combat  
▸ Network-wide broadcasts  
▸ Works on Spigot/Paper backend servers (not on proxy directly)

---

## 📋 Commands

### Player Commands
```
/combat status          - Check your current combat status and opponent
/combat summary         - View your lifetime combat statistics
/combat toggle-style    - Cycle through visual themes
```

### Admin Commands
```
/combat inspect <player>            - Real-time combat data inspection
/combat summary <player>            - View any player's statistics
/combat reload                      - Reload configuration in real-time
/combat debug                       - Toggle debug mode with verbosity levels
/combat logging <enabled|disabled>  - Control console logging
/replay view <session-uuid>         - View combat replay timeline
```

---

## 🔐 Permissions

### Player Permissions (default: true)
▸ `pvpcombat.command.status`  
▸ `pvpcombat.command.summary`  
▸ `pvpcombat.command.toggle-style`

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

## ⚙️ Configuration Highlights

Highly configurable with 100+ options:

```yaml
# General Settings
general:
  enabled: true
  debug-mode: false

# Combat Settings
combat:
  duration: 30              # Combat timer (seconds)
  allow-flight: false       # Allow flight during combat
  cancel-on-death: true     # End combat on death

# Newbie Protection
newbie-protection:
  enabled: true
  prevent-damage-dealing: true
  prevent-damage-receiving: true
  xp-level-threshold: 3
  require-any-armor: true   # true = need 1 piece, false = need full set

# Database
database:
  type: "SQLITE"            # SQLITE or MYSQL
  mysql:
    host: "localhost"
    port: 3306
    database: "pvpcombat"
    username: "root"
    password: "password"

# Restrictions
restrictions:
  trident:
    enabled: true
  enderpearl:
    enabled: true
    cooldown: 10
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
  safezone:
    enabled: true
    block-entry: true
    barrier:
      enabled: true
      material: "GLASS"

# Visual System
visual:
  themes:
    default-theme: "default"
  bossbar:
    enabled: true
  actionbar:
    enabled: true
  sounds:
    profile: "default"

# Performance
performance:
  lag:
    enabled: true
    tps-threshold: 18.0
  async:
    enabled: true
    thread-pool-size: 4

# Logging (disable for best performance)
logging:
  console-enabled: false
```

---

## 🚀 Installation

1. Download TrueCombatManager-1.1.0.jar
2. Place in your server's `plugins` folder
3. Install ProtocolLib (recommended for barriers)
4. Install WorldGuard (optional for safezones)
5. Restart your server
6. Edit `plugins/TrueCombatManager/config.yml` to customize
7. Use `/combat reload` to apply changes

### Requirements
**Required:**  
▸ Minecraft 1.18+ (Paper/Spigot/Purpur)  
▸ Java 17+ (Java 21 recommended)

**Recommended:**  
▸ ProtocolLib 5.0+ (for client-side barriers)  
▸ WorldGuard 7.0+ (for safezone protection)  
▸ PlaceholderAPI 2.11+ (for placeholders)

Works out of the box with default settings!

---

## 📊 PlaceholderAPI Support

Full PlaceholderAPI integration for scoreboard, tab list, and chat plugins:

### Combat Status
▸ `%pvpcombat_in_combat%` - true/false  
▸ `%pvpcombat_time_left%` - Remaining seconds  
▸ `%pvpcombat_opponent%` - Opponent name

### Lifetime Statistics
▸ `%pvpcombat_wins%` - Total wins  
▸ `%pvpcombat_losses%` - Total losses  
▸ `%pvpcombat_kd_ratio%` - K/D ratio  
▸ `%pvpcombat_win_rate%` - Win rate percentage  
▸ `%pvpcombat_total_damage_dealt%` - Total damage dealt  
▸ `%pvpcombat_total_damage_received%` - Total damage received

---

## 📊 Combat Statistics Example

Players can view their statistics with `/combat summary`:

```
=== Combat Summary for PlayerName ===
Total Combats: 47
Wins: 32 | Losses: 15
Win Rate: 68.1%
K/D Ratio: 2.13
Damage Dealt: 587.5 ❤
Damage Received: 275.0 ❤
Damage Ratio: 2.14
Total Combat Time: 23m 45s
Last Combat: 2025-11-30 15:30:00
```

---

## 🐛 Troubleshooting

### Server Lag?
→ Disable console logging: `/combat logging disabled`  
→ Check config: `logging.console-enabled: false`

### Newbie Protection Not Working?
→ Check player has NO armor equipped (all 4 slots empty)  
→ Verify player XP level ≤ threshold (default 3)  
→ Check player doesn't have `pvpcombat.bypass.newbie` permission

### Barriers Not Showing?
→ Install ProtocolLib 5.0+  
→ Check config: `restrictions.safezone.barrier.enabled: true`  
→ Verify WorldGuard regions are configured

---

## ✅ Tested Versions

▸ Minecraft 1.21.x - Fully Tested  
▸ Minecraft 1.20.x - Compatible  
▸ Minecraft 1.19.x - Compatible  
▸ Minecraft 1.18.x - Basic Features

▸ Paper 1.18+ - Recommended  
▸ Spigot 1.18+ - Supported  
▸ Purpur 1.18+ - Supported

▸ Java 21 - Recommended  
▸ Java 17+ - Minimum

---

## 📝 Changelog - v1.1.0

### New Features
▸ SQLite database for data persistence  
▸ MySQL support for network-wide statistics  
▸ Real-time config reload without restart  
▸ Enhanced PvP settings and restrictions  
▸ Improved default configuration

### Improvements
▸ Optimized JAR size (83.5% reduction to 2.4MB)  
▸ Better performance and memory usage  
▸ Enhanced cross-server synchronization

### Bug Fixes
▸ Fixed config reload requiring restart  
▸ Fixed various known issues  
▸ Improved stability and reliability

---

## 📞 Support

Need help? Contact through:

▸ GitHub Issues - Bug reports and feature requests  
▸ Modrinth Comments - General questions  
▸ Discord - Community support (if available)

### Reporting Bugs
1. Check if issue already exists
2. Provide server version and plugin version
3. Include relevant config sections
4. Attach console errors (if any)
5. Describe steps to reproduce

### Feature Requests
All feature requests are welcome and considered!

---

## 📄 License

**All Rights Reserved © 2025 muzlik**

This is proprietary software. Modification or redistribution is prohibited.

---

**Made with care for the Minecraft community**
