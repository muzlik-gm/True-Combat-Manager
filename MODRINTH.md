<p align="center">
<img src="https://capsule-render.vercel.app/api?type=waving&height=300&color=0:1f4037,50:2c7744,100:11998e&text=True%20Combat%20Manager&fontSize=56&fontColor=ffffff&fontAlignY=38&desc=High%20Performance%20Combat%20Management%20for%20Minecraft%20Servers&descSize=18&descAlignY=55"/>

# True Combat Manager

Professional combat tracking with interactive GUIs, persistent statistics, and modern Minecraft server optimization.

<img src="https://img.shields.io/badge/Version-1.2.0-brightgreen?style=for-the-badge"/>
<img src="https://img.shields.io/badge/Minecraft-1.18--1.21-3fb950?style=for-the-badge&logo=minecraft"/>
<img src="https://img.shields.io/badge/Java-17+-f39c12?style=for-the-badge&logo=openjdk"/>
<img src="https://img.shields.io/badge/Platform-Paper%20%7C%20Spigot-3498db?style=for-the-badge"/>
<img src="https://img.shields.io/badge/Status-Stable-success?style=for-the-badge"/>

</p>

---

## Overview

True Combat Manager is a feature-rich combat management system designed for competitive Minecraft PvP servers.

It focuses on:

• accurate combat detection  
• preventing PvP abuse  
• providing clear player feedback  
• interactive statistics GUIs  
• persistent data tracking  

The plugin is optimized for performance and stability even on busy PvP servers.

---

## ✨ What's New in v1.2.0

### Interactive GUI System

- **Player Stats GUI** - Beautiful inventory interface showing combat statistics  
- **Weapon Stats GUI** - Per-weapon breakdown with damage, kills, and usage  
- **Server Overview GUI** - Admin panel with network-wide combat statistics  
- **Fully Customizable** - Configure colors, materials, layouts via gui.yml  
- **Click Navigation** - Seamless navigation between stat screens with back buttons  
- **Real-time Updates** - Live data from database  

### Enhanced Commands

- `/combat stats` - Opens your personal combat stats GUI  
- `/combatadmin stats` - Opens server-wide statistics GUI (admins)  
- Weapon stats button in main GUI  
- Back buttons for easy navigation  

### Improved UX

- Clean, modern GUI layouts with proper spacing  
- Color-coded statistics (wins = green, losses = red)  
- Organized weapon categories (swords, axes, ranged)  
- Glass pane borders for visual clarity  
- Timestamped backup system for deployments  

---

## Core Features

### Combat System

- Real-time combat detection  
- Combat logging prevention with disconnect protection  
- Grace period for reconnecting players  
- Lag compensation using server TPS  
- Thread-safe combat architecture  

---

### Database System

- SQLite support (default, no setup required)  
- MySQL support for larger networks  
- Persistent player combat statistics  
- Weapon-specific combat tracking  
- Automatic database migration  

---

### Smart Restrictions

- Ender pearl cooldown control  
- Trident usage restrictions  
- Elytra combat limitations  
- Golden apple cooldown system  
- Teleport command blocking during combat  
- Safezone protection support  

---

### Visual Interface

- **NEW:** Interactive statistics GUIs  
- BossBar combat indicators  
- ActionBar status display  
- Multiple UI themes (6 built-in)  
- HEX color customization  
- Configurable sound profiles  

---

### Protection Systems

- Newbie protection for unarmored players  
- Timed immunity system with admin commands  
- WorldGuard safe zone integration  
- Configurable bypass permissions  

---

## GUI System

### Player Stats GUI

Shows comprehensive combat statistics:
- Overall record (wins, losses, K/D ratio, win rate)  
- Damage statistics (dealt, received, ratio, highest burst)  
- Combat statistics (total combats, critical hits, longest combo)  
- Weapon stats button to view detailed breakdown  

### Weapon Stats GUI

Per-weapon breakdown organized by category:
- **Swords:** Netherite, Diamond, Iron, Stone, Golden  
- **Axes:** Netherite, Diamond, Iron  
- **Ranged:** Bow, Crossbow, Trident  

Each weapon shows:
- Total damage dealt  
- Kills with that weapon  
- Times used  
- Average damage per hit  

### Server Overview GUI (Admin)

Network-wide statistics:
- Total tracked players and active sessions  
- Total combats across all players  
- Global win/loss ratio and win rate  
- Server damage totals and combat time  
- Per-player averages  
- Weapon stats button for server-wide weapon data  
- Close button  

---

## Commands

### Player Commands

```
/combat status              - View your combat status
/combat stats               - Open your combat statistics GUI
/combat summary             - View latest fight summary
/combat toggle-style        - Change visual theme
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
  cooldown: 5
  disconnect-protection:
    enabled: true

database:
  type: "SQLITE"

restrictions:
  enderpearl:
    cooldown: 6
  trident:
    cooldown: 5
  golden-apple:
    cooldown: 3

visual:
  themes:
    default-theme: "minimal"

logging:
  console-enabled: false
```

All settings reload instantly using:

```
/combat reload
```

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

**No additional setup required** - SQLite database works out of the box!

---

## Requirements

Minecraft **1.18+**  
Java **17+** (21 recommended)  
Server software: **Paper or Spigot**

---

## Optional Integrations

PlaceholderAPI - For placeholder support  
WorldGuard - For safe zone protection  
ProtocolLib - For enhanced visual effects  

---

## Performance

- Optimized JAR size: 2.4 MB  
- Zero lag with intelligent caching  
- Thread-safe concurrent operations  
- Efficient database connection pooling  
- Minimal memory footprint  
- Async operations for heavy tasks  

---

## Support

Issues and bug reports:  
https://github.com/muzlik-gm/True-Combat-Manager/issues

---

<p align="center">
<img src="https://capsule-render.vercel.app/api?type=waving&section=footer&height=120&color=0:11998e,100:1f4037"/>

© 2025 muzlik  
Made for the Minecraft PvP community

</p>
