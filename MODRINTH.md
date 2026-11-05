# True Combat Manager

Professional PvP combat management for Minecraft servers. Persistent statistics, interactive GUIs, dual grace period disconnect protection, and version-compatible text rendering (ChatColor / Adventure Component API / MajorRework adapter).

---

<img src="https://capsule-render.vercel.app/api?type=waving&height=260&color=0:1f4037,50:2c7744,100:11998e&text=True%20Combat%20Manager&fontSize=48&fontColor=ffffff&fontAlignY=38&desc=High%20Performance%20Combat%20Management&descSize=16&descAlignY=55" alt="True Combat Manager banner" width="100%" />

---

## Supported Minecraft versions & loaders

| Minecraft version | Adapter / tier | Status |
|-------------------|----------------|--------|
| **1.19.4 – 1.19.x** | `LegacyCompatibilityAdapter` (ChatColor) | ✅ Officially supported |
| **1.20.x – 1.21.x** | `ModernCompatibilityAdapter` (Adventure Component API) | ✅ Officially supported |
| **1.22.x – 1.23.x** | `ModernCompatibilityAdapter` (refined ActionBar) | ✅ Officially supported |
| **26.x (26.0.0+)** | `MajorReworkCompatibilityAdapter` | ⚠️ Loads and runs (verified on MC 26.2 Spigot), but **untested / not officially supported** |

**Server software:** Spigot, Paper, Purpur (Bukkit/Folia compatible)  
**Java:** 21 or higher  
**Modrinth loaders:** `paper`, `purpur`, `spigot`, `bukkit`, `folia`

**Not supported:** Sponge, BungeeCord, Velocity, Waterfall (proxy / non-Bukkit platforms).

---

## Requirements

- Minecraft **1.19.4** or newer (officially up to **1.23.x**; 26.x uses the MajorRework adapter)
- Java **21+**
- Server software: **Spigot**, **Paper**, or **Purpur**

### Optional dependencies
- [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) — `%pvpcombat_*%` placeholders
- [WorldGuard](https://modrinth.com/plugin/worldguard) — region-based safe zones
- [ProtocolLib](https://modrinth.com/plugin/protocollib) — client-side barrier rendering

---

## What's new in v1.3.1

- **MajorRework adapter (26.x):** Added `MajorReworkCompatibilityAdapter` and a `isAtLeast(26,0,0)` branch in `CompatibilityManager.create()`; tier reports `MAJOR_REWORK (26+)` with an explicit untested-version warning.
- **Chat color routing:** `DisconnectTracker.color()` now routes through `CompatibilityAPI.translateColor()`; `CompatibilityAPI` injected via constructor and exposed via `PvPCombatPlugin.getCompatibilityAPI()`.
- **Deprecated `ItemStack` fixed:** Replaced single-arg `new ItemStack(Material.X)` with `new ItemStack(Material.X, 1)` across `DisconnectTracker`, `AdminGUI`, `GUIManager`, and `CombatLogger`.
- **Validated:** 404 unit tests pass; live MC 26.2 Spigot server load + `/combat reload` verified with no TrueCombatManager exceptions.

---

## Core features

### Combat system
- Real-time combat detection
- Dual grace period disconnect protection (bad-internet vs intentional)
- Repeat-logout abuse prevention with kill-on-login
- Lag compensation using server TPS
- Thread-safe combat architecture

### Interactive GUI system
- Player Stats GUI — overall record, damage stats, combat stats
- Weapon Stats GUI — per-weapon breakdown (swords, axes, ranged)
- Server Overview GUI — admin panel with network-wide statistics
- Fully configurable via `gui.yml`

### Database system
- SQLite (default, zero setup)
- MySQL for larger networks
- Persistent player statistics and theme preferences
- Automatic database migration

### Smart restrictions
- Ender pearl cooldown control
- Trident usage restrictions
- Elytra combat limitations
- Golden apple cooldown system
- Teleport command blocking
- Safezone protection support

### Visual interface
- 6 built-in themes with per-player persistence
- BossBar combat indicators
- ActionBar status display
- 6 sound profiles with hot-reload
- HEX color customization

### Protection systems
- Newbie protection for unarmored players
- Timed immunity system with admin commands
- WorldGuard safe zone integration
- Configurable bypass permissions

---

## Commands

### Player commands

```
/combat status              - View your combat status
/combat stats               - Open your combat statistics GUI
/combat summary             - View latest fight summary
/combat toggle-style        - Change visual theme (works anytime)
```

### Admin commands

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

## PlaceholderAPI support

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

1. Download `truecombatmanager-1.3.1.jar` from the Files tab.
2. Place it in your server's `plugins/` folder.
3. Start or restart the server.
4. Configure settings in `plugins/TrueCombatManager/config.yml` if needed.
5. Run `/combat reload` to apply changes without restarting.

SQLite works out of the box — no additional setup required.

---

## Configuration example

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

## Support

- Issues: https://github.com/muzlik-gm/True-Combat-Manager/issues
- Modrinth: https://modrinth.com/plugin/true-combat-manager

© 2026 muzlik · Made for the Minecraft PvP community
