<p align="center">
<img src="https://capsule-render.vercel.app/api?type=waving&height=300&color=0:1f4037,50:2c7744,100:11998e&text=True%20Combat%20Manager&fontSize=56&fontColor=ffffff&fontAlignY=38&desc=High%20Performance%20Combat%20Management%20for%20Minecraft%20Servers&descSize=18&descAlignY=55"/>

# True Combat Manager

Professional combat tracking with interactive GUIs, persistent statistics, dual grace period protection, and modern Minecraft server optimization.

<img src="https://img.shields.io/badge/Version-1.2.1-brightgreen?style=for-the-badge"/>
<img src="https://img.shields.io/badge/Minecraft-1.18--1.21-3fb950?style=for-the-badge&logo=minecraft"/>
<img src="https://img.shields.io/badge/Java-17+-f39c12?style=for-the-badge&logo=openjdk"/>
<img src="https://img.shields.io/badge/Platform-Paper%20%7C%20Spigot-3498db?style=for-the-badge"/>
<img src="https://img.shields.io/badge/Status-Stable-success?style=for-the-badge"/>

</p>

---

## Overview

True Combat Manager is a feature-rich combat management system designed for competitive Minecraft PvP servers. It focuses on accurate combat detection, preventing PvP abuse, providing clear player feedback, interactive statistics GUIs, and persistent data tracking.

---

## ✨ What's New in v1.2.1

### Dual Grace Period System

The disconnect protection system has been completely overhauled.

- **Bad-internet grace period** — longer, more lenient window for players who lost connection unexpectedly. Detected automatically from the quit reason.
- **Intentional grace period** — shorter window for players who pressed Disconnect or closed the game.
- Each type has its own `grace-seconds`, messages, and can be enabled/disabled independently.

### Repeat-Logout Abuse Prevention

- Tracks each player's disconnect history in a rolling time window.
- If a player disconnects too many times within the window, they are killed immediately on the next logout — no grace period.
- Fully configurable: `max-count`, `window-seconds`, messages.

### Bypass-Totem Config

- `bypass-totem: true` (default) — punishment kills bypass the Totem of Undying.
- `bypass-totem: false` — a held totem can proc and save the player.

### Per-Player Theme Persistence

- Theme selections saved to the database and restored across sessions.
- `/combat toggle-style` works both in and out of combat.

### Sound Profile Hot-Reload

- `/combat reload` now correctly updates sound profiles without a restart.

### Bug Fixes

- Fixed inventory duplication (armor dropped twice due to `getContents()` including armor slots)
- Fixed double bossbar on reconnect
- Fixed player not killed after grace period (config loaded before ConfigManager was ready)
- Fixed concurrent update race in player data
- Fixed `plugin.getConfig()` stale after reload

---

## Core Features

### Combat System

- Real-time combat detection
- Dual grace period disconnect protection (bad-internet vs intentional)
- Repeat-logout abuse prevention with kill-on-login
- Lag compensation using server TPS
- Thread-safe combat architecture

### Interactive GUI System

- **Player Stats GUI** — overall record, damage stats, combat stats
- **Weapon Stats GUI** — per-weapon breakdown (swords, axes, ranged)
- **Server Overview GUI** — admin panel with network-wide statistics
- Fully configurable via `gui.yml`
- Back buttons and seamless navigation

### Database System

- SQLite (default, no setup required)
- MySQL for larger networks
- Persistent player statistics and theme preferences
- Automatic database migration

### Smart Restrictions

- Ender pearl cooldown control
- Trident usage restrictions
- Elytra combat limitations
- Golden apple cooldown system
- Teleport command blocking
- Safezone protection support

### Visual Interface

- 6 built-in themes with per-player persistence
- BossBar combat indicators
- ActionBar status display
- 6 sound profiles with hot-reload
- HEX color customization

### Protection Systems

- Newbie protection for unarmored players
- Timed immunity system with admin commands
- WorldGuard safe zone integration
- Configurable bypass permissions

---

## Disconnect Protection Flow

**Grace period expires, player still offline:**
1. Inventory dropped at disconnect location immediately
2. Win/loss recorded, opponent notified and rewarded
3. `pendingPunishments` entry stored — player killed on next login

**Player rejoins with pending punishment:**
1. Live inventory cleared
2. Player killed (totem bypass configurable)
3. Opponent receives reminder message

---

## Commands

### Player Commands

```
/combat status              - View your combat status
/combat stats               - Open your combat statistics GUI
/combat summary             - View latest fight summary
/combat toggle-style        - Change visual theme (works anytime)
```

### Admin Commands

```
/combat reload              - Reload configuration
/combat inspect <player>    - View real-time combat info
/combat summary <player>    - Open player's stats GUI
/combatadmin stats          - Open server statistics GUI
/combat clear <player>      - Force-end combat
/combat protection <player> <seconds> - Grant immunity
/combat debug               - Toggle debug mode
/combat logging <on|off>    - Toggle console logging
```

---

## Configuration Example

```yaml
combat:
  duration: 10
  disconnect-protection:
    enabled: true
    bad-internet:
      grace-seconds: 30
    intentional:
      grace-seconds: 10
    repeat-logout:
      enabled: true
      max-count: 2
      window-seconds: 240
    punishment:
      drop-inventory: true
      bypass-totem: true

visual:
  themes:
    default-theme: "clean"
  sounds:
    profile: "default"

database:
  type: "sqlite"

logging:
  console-enabled: false
```

All settings reload instantly with `/combat reload`.

---

## PlaceholderAPI Support

```
%pvpcombat_in_combat%
%pvpcombat_time_left%
%pvpcombat_opponent%
%pvpcombat_wins%
%pvpcombat_losses%
%pvpcombat_kd_ratio%
%pvpcombat_win_rate%
%pvpcombat_total_damage_dealt%
%pvpcombat_total_damage_received%
```

---

## Installation

1. Download the plugin JAR
2. Place it inside the `plugins` folder
3. Restart the server
4. Configure settings if needed
5. Run `/combat reload`

**No additional setup required** — SQLite works out of the box.

---

## Requirements

Minecraft **1.18+** · Java **17+** (21 recommended) · Paper or Spigot

**Optional:** PlaceholderAPI · WorldGuard · ProtocolLib

---

## Support

https://github.com/muzlik-gm/True-Combat-Manager/issues

---

<p align="center">
<img src="https://capsule-render.vercel.app/api?type=waving&section=footer&height=120&color=0:11998e,100:1f4037"/>

© 2026 muzlik · Made for the Minecraft PvP community

</p>
