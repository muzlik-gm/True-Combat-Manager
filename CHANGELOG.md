# Changelog

All notable changes to True Combat Manager will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.2.1] - 2026-05-16

### 🛡️ Dual Grace Period System & Major Bug Fixes

This release overhauls the disconnect protection system with a fully configurable dual grace period, fixes a series of inventory duplication bugs, and resolves a long-standing concurrency issue in player data tracking.

---

### ✨ New Features

#### Dual Grace Period (Disconnect Protection)
- **Bad-internet grace period** — longer, more lenient window for players who lost connection unexpectedly (network drop, timeout, server kick). Detected automatically from the quit reason.
- **Intentional grace period** — shorter window for players who pressed Disconnect or closed the game.
- Each type has its own `grace-seconds`, `disconnect-message`, and `reconnect-message` — all configurable independently.
- Grace period countdown shown to the opponent via action bar, boss bar, or title (configurable).

#### Repeat-Logout Abuse Prevention
- Tracks each player's disconnect history in a rolling time window.
- If a player disconnects more than `max-count` times within `window-seconds`, they are killed immediately on the next logout — no grace period granted.
- Fully configurable: `enabled`, `max-count`, `window-seconds`, `kill-message`, `opponent-message`.

#### Bypass-Totem Config Option
- `punishment.bypass-totem: true` (default) — uses `setHealth(0)` to kill the combat logger, bypassing the Totem of Undying. Recommended for anti-abuse.
- `punishment.bypass-totem: false` — deals lethal damage through the normal pipeline; a held totem can proc and save the player.

#### Per-Player Theme Persistence
- Theme selections are now saved per-player to the database and restored across sessions.
- `/combat toggle-style` works both in and out of combat — no longer requires an active fight.
- The chosen theme is applied automatically when the player enters combat.

#### Sound Profile Hot-Reload
- Sound profiles now read from `ConfigManager`'s reloadable `FileConfiguration` instead of Bukkit's stale cached config.
- `/combat reload` now correctly updates sound profiles without a server restart.
- Built-in profiles (default, subtle, intense, calm, electronic, clean) are always registered as fallbacks; config profiles overlay them.

---

### 🔧 Disconnect Protection — Full Behaviour

**Grace period expires, player still offline:**
1. Inventory snapshot dropped at disconnect location immediately
2. Win/loss recorded, opponent notified and rewarded
3. Punishment broadcast sent
4. `pendingPunishments` entry stored — player will be killed on next login

**Player rejoins with pending punishment:**
1. Live inventory cleared (prevents death-drop duplication)
2. Player killed (`setHealth(0)` or `damage(10000)` depending on `bypass-totem`)
3. Opponent receives a reminder message

**Grace period expires, player already online (rejoined before timer fired):**
1. Live inventory cleared
2. Snapshot dropped at original disconnect location
3. Player killed immediately

---

### 🐛 Bug Fixes

#### Inventory Duplication (Triple Drop)
- **Root cause:** `getContents()` returns all 41 inventory slots including armor (36–39) and offhand (40). Passing it alongside `getArmorContents()` caused armor to be dropped twice.
- **Fix:** Changed all snapshot calls from `getContents()` to `getStorageContents()` (slots 0–35 only). Armor is captured separately via `getArmorContents()` with no overlap.

#### Armor Clearing Before Kill
- **Root cause:** `setArmorContents(new ItemStack[4])` passes null references; Bukkit may not clear slots reliably.
- **Fix:** New `clearAllInventory()` helper explicitly sets each slot to `new ItemStack(Material.AIR)` using individual setters (`setHelmet`, `setChestplate`, `setLeggings`, `setBoots`, `setItemInOffHand`).

#### Double Bossbar on Reconnect
- **Root cause:** `silentlyRemovePlayer` cancelled the timer task but left the old session's `BossBar` in `BossBarManager.activeBossBars`. When combat restarted, a second bossbar was created and stacked.
- **Fix:** `silentlyRemovePlayer` now calls `bossBarManager.clearBossBar(sessionId)` to remove the old bar before the new session creates one.

#### `endCombat` Crashing on Offline Players
- `clearVisuals(player)` and `generateSummary(player, ...)` were called unconditionally even when one player was offline (e.g. after grace period expiry).
- Both calls are now guarded with `player.isOnline()`.

#### Config Loaded Before `ConfigManager` Ready
- `DisconnectTracker` was constructed inside `CombatManager`'s constructor, before `ConfigManager.loadConfig()` ran. `getDisconnectConfig()` returned null, the fallback used a no-op `load()`, and all fields defaulted to `false`/`0` — so the grace period never ran.
- **Fix:** `cfg` is now resolved lazily via a `cfg()` method on every call. By the time any player disconnects, `ConfigManager` has finished loading.

#### Concurrent Update Race in `PlayerCombatData`
- Plain `int` and `double` fields were updated from multiple threads (damage events, win/loss recording), causing lost updates under concurrent access.
- **Fix:** All concurrently-updated fields replaced with atomic types: `AtomicInteger` for counts, `AtomicLong` with `Double.doubleToLongBits` CAS loop for damage values.
- Fixes the flaky `testConcurrentUpdates` test (expected 1000, got ~925).

#### `plugin.getConfig()` Stale After Reload
- `ConfigManager.reloadConfig()` reloaded its own `FileConfiguration` but never called `plugin.reloadConfig()`, so `plugin.getConfig()` returned the startup snapshot forever.
- **Fix:** `ConfigManager.reloadConfig()` now calls `plugin.reloadConfig()` first.

---

### 🛠️ Configuration Changes

New section under `combat.disconnect-protection`:

```yaml
combat:
  disconnect-protection:
    enabled: true
    display-mode: "actionbar"       # actionbar | bossbar | scoreboard
    grace-period-format: "&e{player} &7has &c{time}s &7to reconnect &8({type})"

    bad-internet:
      enabled: true
      grace-seconds: 30
      disconnect-message: "&e{player} &clost connection! They have &e{time}s &cto reconnect."
      reconnect-message: "&aYou reconnected in time! No penalty applied."

    intentional:
      enabled: true
      grace-seconds: 10
      disconnect-message: "&e{player} &cdisconnected! They have &e{time}s &cto reconnect or be punished."
      reconnect-message: "&aYou reconnected in time! No penalty applied."

    repeat-logout:
      enabled: true
      max-count: 2
      window-seconds: 240
      kill-message: "&cYou were killed for repeatedly combat-logging!"
      opponent-message: "&a{player} &ewas killed for repeatedly combat-logging!"

    punishment:
      drop-inventory: true
      kill-on-punish: false
      bypass-totem: true            # NEW
      broadcast: "&c{player} &ecombat-logged and was punished!"
```

---

### 🔮 Migration Guide

#### From 1.2.0

1. Stop your server
2. Replace the JAR with `truecombatmanager-1.2.1.jar`
3. Delete `plugins/TrueCombatManager/config.yml` to regenerate with the new disconnect-protection structure, **or** manually add the new sections (see above)
4. Start your server

No database changes required. All existing statistics are preserved.

---

**Current Version**: 1.2.1
**Released**: May 16, 2026
**Author**: muzlik
**Support**: https://github.com/muzlik-gm/True-Combat-Manager/issues

---

## [1.2.0] - 2026-05-15

### 🖥️ Interactive GUI System

This release introduces a full inventory-based GUI system for viewing combat statistics.

### ✨ New Features

#### Player Stats GUI
- `/combat stats` opens your personal combat statistics GUI
- Displays overall record (wins, losses, K/D ratio, win rate)
- Displays damage statistics (dealt, received, ratio, highest burst)
- Displays combat statistics (total combats, critical hits, longest combo)
- Weapon stats button navigates to per-weapon breakdown

#### Weapon Stats GUI
- Per-weapon breakdown organized by category
- **Swords:** Netherite, Diamond, Iron, Stone, Golden
- **Axes:** Netherite, Diamond, Iron
- **Ranged:** Bow, Crossbow, Trident
- Each weapon shows total damage, kills, uses, and average damage per hit
- Back button returns to main stats screen

#### Server Overview GUI (Admin)
- `/combatadmin stats` opens the server-wide statistics GUI
- Network Snapshot: tracked players, active sessions, total combats, global win rate
- Server Damage Totals: dealt, received, total combat time
- Per-Player Averages: combats per player, damage per player

#### GUI Configuration (gui.yml)
- Fully configurable layouts, materials, slot positions, and colors
- All item names and lore support `&` color codes
- Placeholder support in all lore lines

### 🐛 Bug Fixes
- Some minor bug fixes

### 🔮 Migration Guide

#### From 1.1.0 or Earlier
1. Stop your server
2. Replace the plugin JAR with `truecombatmanager-1.2.0.jar`
3. Delete `plugins/TrueCombatManager/gui.yml` if it exists (will regenerate)
4. Start your server

---

**Released**: May 15, 2026

---

## [1.0.2] - 2025-12-04

### 🛡️ Anti-Abuse & Combat Logging Fix

**CRITICAL UPDATE**: Fixes a major exploit where players could crash opponents to force combat logging.

### ✨ New Features
- Disconnect protection system with grace period
- Players tracked on disconnect instead of instant punishment
- Reconnection within timer = no penalty
- All messages configurable

### 🐛 Bug Fixes
- Fixed exploit where players could abuse crashes to win fights
- Fixed instant punishment not considering network issues

---

**Released**: December 4, 2025

---

## [1.0.1] - 2025-11-30

### ⚡ Performance & Bug Fix Release

**CRITICAL UPDATE**: Fixes severe performance issues causing server lag and resolves multiple critical bugs.

### ⚡ Performance
- Fixed server lag from excessive string creation in event handlers
- Server TPS improved from 15–18 to stable 20
- Zero performance impact when logging is disabled

### ✨ New Features
- `/combat logging <enabled|disabled>` — toggle console logging in-game
- Respawn anchor blocking during combat

### 🐛 Bug Fixes
- Fixed command blocking not working (/warp, /home, /spawn, etc.)
- Fixed async event errors causing console spam
- Fixed newbie protection not detecting naked players
- Fixed tridents usable in combat
- Fixed ender pearls entering safezones
- Fixed barriers appearing on non-air blocks

---

**Released**: November 30, 2025

---

## [1.0.0] - 2025-11-21

### 🎉 Initial Release

First stable release of True Combat Manager.

#### Core Features
- Real-time combat detection and management
- Comprehensive statistics tracking
- Visual feedback (BossBar, ActionBar, Sounds)
- Extensive restriction system
- PlaceholderAPI integration
- Cross-server support (experimental)

---

**Released**: November 21, 2025
