# True Combat Manager

[![Version](https://img.shields.io/badge/version-1.2.1-blue.svg)](https://github.com/muzlik-gm/True-Combat-Manager)
[![Minecraft](https://img.shields.io/badge/minecraft-1.18--1.21-green.svg)](https://www.spigotmc.org/)
[![Java](https://img.shields.io/badge/java-17+-orange.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

**Professional PvP Combat Management for Minecraft Servers**

A lightweight, high-performance combat plugin with persistent statistics, real-time tracking, interactive GUIs, and comprehensive admin tools. Built for modern Minecraft servers running Spigot, Paper, or Purpur.

---

## ⚡ Key Features

### ▸ Combat System
Real-time combat tracking with configurable duration • Automatic combat tagging on player damage • Dual grace period disconnect protection (bad-internet vs intentional) • Repeat-logout abuse prevention • Timer reset on new damage • Lag compensation system

### ▸ Database & Statistics
SQLite database (default, zero setup) • MySQL support for multi-server networks • Persistent player statistics across restarts • Weapon-specific damage tracking • Per-player theme preferences saved to DB

### ▸ Interactive GUI System
Player stats GUI with overall record, damage stats, and combat stats • Weapon stats GUI with per-weapon breakdown (swords, axes, ranged) • Server overview GUI for admins • Back buttons and navigation • Fully configurable via `gui.yml`

### ▸ Protection Systems
Dual grace period for disconnects (bad-internet / intentional, separately configurable) • Repeat-logout kill-on-login prevention • Newbie protection for unarmored players • Timed immunity system • WorldGuard safe zone integration • Bypass-totem option for punishment kills

### ▸ Visual Feedback
6 built-in themes with per-player persistence • Theme change works in and out of combat • Customizable BossBar • ActionBar timer display • 6 sound profiles with hot-reload support • HEX color support

### ▸ Restrictions
Block ender pearls during combat • Prevent elytra usage • Restrict trident throwing • Block golden apple consumption • Prevent command usage • Disable teleportation • Respawn anchor blocking

### ▸ Admin Tools
`/combat reload` — instant config reload without restart • `/combat protection <player> <seconds>` — grant immunity • `/combat clear <player>` — force-end combat • `/combatadmin stats` — server-wide statistics GUI • `/combat logging <on|off>` — toggle console logging • `/combat inspect <player>` — real-time combat status

### ▸ Cross-Server Support
BungeeCord/Velocity network sync • Prevent server-hopping during combat • Shared combat state across backend servers

---

## 📋 Requirements

**Server Software:** Spigot, Paper, or Purpur
**Minecraft Version:** 1.18 – 1.21+
**Java Version:** 17 or higher (21 recommended)

**Optional Dependencies:**
▸ PlaceholderAPI 2.11+ — placeholder support
▸ WorldGuard 7.0+ — safe zone protection
▸ ProtocolLib 5.0+ — client-side barriers

---

## 🚀 Installation

1. Download `truecombatmanager-1.2.1.jar`
2. Place in your server's `plugins` folder
3. Restart your server
4. Configure in `plugins/TrueCombatManager/config.yml`
5. Use `/combat reload` to apply changes without restarting

**No additional setup required** — SQLite database works out of the box.

For detailed setup instructions, see [INSTALLATION.md](INSTALLATION.md)

---

## 📊 What's New in v1.2.1

▸ **NEW: Dual Grace Period** — separate timers for bad-internet vs intentional disconnects
▸ **NEW: Repeat-Logout Prevention** — players who disconnect too often are killed on next login
▸ **NEW: Bypass-Totem Config** — choose whether punishment kills bypass the Totem of Undying
▸ **NEW: Per-Player Theme Persistence** — theme saved to DB, restored across sessions
▸ **NEW: Theme Change Out of Combat** — `/combat toggle-style` works anytime
▸ **NEW: Sound Profile Hot-Reload** — `/combat reload` now correctly updates sound profiles
▸ **FIXED: Inventory duplication** — armor was dropped twice due to `getContents()` including armor slots
▸ **FIXED: Double bossbar on reconnect** — old session bossbar now cleared before new one is created
▸ **FIXED: Player not killed after grace period** — config was loaded before ConfigManager was ready
▸ **FIXED: Concurrent update race** — `PlayerCombatData` now uses atomic fields
▸ **FIXED: `plugin.getConfig()` stale after reload** — `plugin.reloadConfig()` now called on `/combat reload`

See [CHANGELOG.md](CHANGELOG.md) for full version history.

---

## 🎮 Commands

### Player Commands
```
/combat status          - View your combat status
/combat stats           - Open your combat statistics GUI
/combat summary         - View your combat statistics
/combat toggle-style    - Change visual theme (works in and out of combat)
```

### Admin Commands
```
/combat reload                      - Reload configuration in real-time
/combat inspect <player>            - Inspect player combat status
/combat clear <player>              - Force-end combat
/combatadmin stats                  - View server statistics GUI
/combat logging <on|off>            - Toggle console logging
/combat protection <player> <time>  - Grant timed immunity
/combat summary <player>            - Open player's stats GUI
```

---

## 🔐 Permissions

### Player Permissions (default: true)
▸ `pvpcombat.command.status` — use /combat status
▸ `pvpcombat.command.summary` — view statistics
▸ `pvpcombat.command.toggle-style` — change theme

### Admin Permissions (default: op)
▸ `pvpcombat.admin` — all admin commands
▸ `pvpcombat.admin.inspect` — inspect players
▸ `pvpcombat.admin.debug` — debug mode

### Bypass Permissions (default: op)
▸ `pvpcombat.bypass.combatlog` — bypass combat logging
▸ `pvpcombat.bypass.restrictions` — bypass all restrictions
▸ `pvpcombat.bypass.newbie` — bypass newbie protection
▸ `pvpcombat.bypass.server-switch` — bypass server switch prevention

---

## 🔧 Configuration Highlights

### Dual Grace Period
```yaml
combat:
  disconnect-protection:
    enabled: true
    display-mode: "actionbar"   # actionbar | bossbar | scoreboard

    bad-internet:
      enabled: true
      grace-seconds: 30         # generous — not their fault

    intentional:
      enabled: true
      grace-seconds: 10         # shorter — deliberate disconnect

    repeat-logout:
      enabled: true
      max-count: 2              # 3rd disconnect in window = instant kill
      window-seconds: 240       # 4-minute rolling window

    punishment:
      drop-inventory: true
      bypass-totem: true        # true = setHealth(0), totem can't save them
      broadcast: "&c{player} &ecombat-logged and was punished!"
```

### Visual Themes
```yaml
visual:
  themes:
    default-theme: "clean"      # minimal, fire, ice, neon, dark, clean
  sounds:
    profile: "default"          # default, subtle, intense, calm, electronic, clean
```

### Database
```yaml
database:
  type: "sqlite"                # sqlite or mysql
  mysql:
    host: "localhost"
    port: 3306
    database: "pvpcombat"
    username: "root"
    password: ""
```

All settings reload instantly with `/combat reload` — no restart needed.

---

## 🎨 Visual Themes

Players can switch between 6 built-in themes with `/combat toggle-style`. The choice is saved and restored automatically:

| Theme | BossBar Color | Sound Profile |
|-------|--------------|---------------|
| minimal | White | subtle |
| fire | Red | intense |
| ice | Blue | calm |
| neon | Pink | electronic |
| dark | White | subtle |
| clean | Green | clean |

---

## 📊 PlaceholderAPI

```
%pvpcombat_in_combat%           - true/false
%pvpcombat_time_left%           - remaining seconds
%pvpcombat_opponent%            - opponent name
%pvpcombat_wins%                - total wins
%pvpcombat_losses%              - total losses
%pvpcombat_kd_ratio%            - K/D ratio
%pvpcombat_win_rate%            - win rate percentage
%pvpcombat_total_damage_dealt%  - total damage dealt
%pvpcombat_total_damage_received% - total damage received
```

---

## 🌐 Network Setup

For multi-server networks:

1. Install plugin on each backend Spigot/Paper server
2. Configure MySQL database (shared across network)
3. Enable cross-server sync in config:
   ```yaml
   integration:
     cross-server-sync:
       enabled: true
   ```
4. Plugin automatically syncs through BungeeCord/Velocity

---

## 🐛 Troubleshooting

**Grace period not working?**
→ Delete `config.yml` and let it regenerate — the new `bad-internet`/`intentional` sections must be present
→ Check console for `[DisconnectTracker]` log lines

**Double bossbar after reconnect?**
→ Update to 1.2.1 — this was a known bug fixed in this release

**Sound profile changes not applying?**
→ Use `/combat reload` — sound profiles now hot-reload correctly in 1.2.1

**Server lag?**
→ Disable console logging: `/combat logging disabled`

**Config changes not applying?**
→ Use `/combat reload` — all settings reload without restart

---

## 🔗 Links

**GitHub:** https://github.com/muzlik-gm/True-Combat-Manager
**Issues:** https://github.com/muzlik-gm/True-Combat-Manager/issues
**Changelog:** [CHANGELOG.md](CHANGELOG.md)
**Installation Guide:** [INSTALLATION.md](INSTALLATION.md)

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**. See the [LICENSE](LICENSE) file for the full license text.

### Why GPLv3?
We chose the **GNU General Public License v3.0** for **True Combat Manager** because it is the industry standard for open-source Minecraft plugin development, aligning with the licensing of major platforms like SpigotMC and PaperMC. GPLv3 is a strong copyleft license that ensures:
1. **End-User Freedom:** Players and server administrators have the freedom to run, study, share, and modify the software.
2. **Copyleft Protection:** Any modified versions or derivatives of this plugin must also be open-sourced under the GPLv3. This prevents third parties from taking our work, making minor proprietary modifications, and selling it under a closed-source license.
3. **Patent Protections:** GPLv3 contains explicit patent grants, protecting users and contributors from patent litigation.

---

**Made with care for the Minecraft community**
