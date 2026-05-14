<p align="center">
<img src="https://capsule-render.vercel.app/api?type=waving&height=300&color=0:1f4037,50:2c7744,100:11998e&text=True%20Combat%20Manager&fontSize=56&fontColor=ffffff&fontAlignY=38&desc=High%20Performance%20Combat%20Management%20for%20Minecraft%20Servers&descSize=18&descAlignY=55"/>

# True Combat Manager

Professional combat tracking designed for performance, reliability, and modern Minecraft servers.

<img src="https://img.shields.io/badge/Minecraft-1.18--1.21-3fb950?style=for-the-badge&logo=minecraft"/>
<img src="https://img.shields.io/badge/Java-17+-f39c12?style=for-the-badge&logo=openjdk"/>
<img src="https://img.shields.io/badge/Platform-Paper%20%7C%20Spigot-3498db?style=for-the-badge"/>
<img src="https://img.shields.io/badge/Status-Stable-success?style=for-the-badge"/>

</p>

---

## Overview

True Combat Manager is a lightweight combat management system designed for competitive Minecraft PvP servers.

It focuses on:

• accurate combat detection  
• preventing PvP abuse  
• providing clear player feedback  

The plugin is optimized for performance and stability even on busy PvP servers.

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

- BossBar combat indicators  
- ActionBar status display  
- Multiple UI themes  
- HEX color customization  
- Configurable sound profiles  

---

## Commands

### Player Commands

```
/combat status
/combat summary
/combat toggle-style
```

### Admin Commands

```
/combat reload
/combat inspect <player>
/combat clear <player>
/combat stats
/combat protection <player> <seconds>
/combat debug
/combat logging <on|off>
```

---

## Configuration Example

```yaml
combat:
  duration: 10
  cooldown: 5

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
```

---

## Installation

1. Download the plugin JAR  
2. Place it inside the `plugins` folder  
3. Restart the server  
4. Configure settings if needed  
5. Run `/combat reload`  

---

## Requirements

Minecraft **1.18+**  
Java **17+**  
Server software: **Paper or Spigot**

---

## Optional Integrations

PlaceholderAPI  
WorldGuard  
ProtocolLib  

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
