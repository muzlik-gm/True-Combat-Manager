# Installation Guide — True Combat Manager v1.2.1

## Quick Start

Drop the JAR in your plugins folder and restart. That's it — SQLite works out of the box with zero configuration.

---

## Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| Minecraft | 1.18.x | 1.21.x |
| Java | 17 | 21 |
| Server software | Spigot | Paper |

### Included (no download needed)
- ✅ SQLite JDBC — built-in database (Windows x64, Linux x64, Linux ARM64)
- ✅ HikariCP — connection pooling
- ✅ Gson — JSON handling
- ✅ Caffeine — high-performance caching

### Optional
- **PlaceholderAPI** — for `%pvpcombat_*%` placeholders
- **WorldGuard 7.0+** — for region-based safe zones
- **ProtocolLib 5.0+** — for client-side barrier rendering at safe zone edges

---

## Installation Steps

1. Download `truecombatmanager-1.2.1.jar`
2. Place it in your server's `plugins/` folder
3. Start or restart your server
4. The plugin generates its config files automatically:
   - `plugins/TrueCombatManager/config.yml`
   - `plugins/TrueCombatManager/gui.yml`
5. Edit settings as needed
6. Run `/combat reload` to apply changes without restarting

---

## First-Time Configuration

The defaults work for most servers. The settings you're most likely to want to change:

```yaml
# How long combat lasts after the last hit (seconds)
combat:
  duration: 10

# Grace period when a player disconnects during combat
combat:
  disconnect-protection:
    bad-internet:
      grace-seconds: 30     # generous for network drops
    intentional:
      grace-seconds: 10     # shorter for deliberate disconnects
    punishment:
      drop-inventory: true
      bypass-totem: true    # true = totem can't save a combat logger

# Newbie protection (off by default)
newbie-protection:
  enabled: false

# Database (SQLite by default, no setup needed)
database:
  type: "sqlite"
```

---

## MySQL Setup (Optional)

Only needed for multi-server networks or if you prefer MySQL over SQLite.

1. Create a database on your MySQL server:
   ```sql
   CREATE DATABASE pvpcombat CHARACTER SET utf8mb4;
   CREATE USER 'pvpcombat'@'%' IDENTIFIED BY 'yourpassword';
   GRANT ALL PRIVILEGES ON pvpcombat.* TO 'pvpcombat'@'%';
   ```

2. Update `config.yml`:
   ```yaml
   database:
     type: "mysql"
     mysql:
       host: "your-mysql-host"
       port: 3306
       database: "pvpcombat"
       username: "pvpcombat"
       password: "yourpassword"
   ```

3. Run `/combat reload` or restart the server.

The plugin handles schema creation and migrations automatically.

---

## Multi-Server (BungeeCord / Velocity) Setup

1. Install the plugin on **each backend Spigot/Paper server** (not on the proxy)
2. Set up a shared MySQL database (see above)
3. Enable cross-server sync:
   ```yaml
   integration:
     cross-server-sync:
       enabled: true
       platform: "AUTO"    # AUTO detects BungeeCord or Velocity
   ```
4. Restart all backend servers

Players' combat state and statistics are shared across the network.

---

## Upgrading from v1.2.0

1. Stop your server
2. Replace the JAR with `truecombatmanager-1.2.1.jar`
3. **Recommended:** Delete `plugins/TrueCombatManager/config.yml` so it regenerates with the new `disconnect-protection` structure. Your database and statistics are unaffected.
   - Alternatively, manually add the new sections — see [CHANGELOG.md](CHANGELOG.md) for the exact YAML.
4. Start your server

No database migration required.

---

## Upgrading from v1.1.0 or Earlier

1. Stop your server
2. Replace the JAR
3. Delete `config.yml` and `gui.yml` (both will regenerate)
4. Start your server
5. Re-apply any custom settings

---

## Platform Notes

### Windows
Fully supported. SQLite native library for Windows x64 is bundled.

### Linux x64
Fully supported. SQLite native library for Linux x64 is bundled.

### Linux ARM64 (Docker, Raspberry Pi, etc.)
Fully supported. SQLite native library for Linux ARM64 is bundled.

### Other platforms (macOS, FreeBSD, 32-bit)
The bundled SQLite natives won't load. Add the full `sqlite-jdbc` library to your server's classpath, or switch to MySQL.

---

## Permissions Quick Reference

```yaml
# Grant all player commands (default: true for all players)
pvpcombat.command.status
pvpcombat.command.summary
pvpcombat.command.toggle-style

# Grant admin access (default: op)
pvpcombat.admin

# Bypass permissions (default: op)
pvpcombat.bypass.combatlog
pvpcombat.bypass.restrictions
pvpcombat.bypass.newbie
pvpcombat.bypass.server-switch
```

---

## Troubleshooting

**Plugin not loading?**
→ Check Java version: `java -version` — must be 17+
→ Check server version — must be 1.18+
→ Check console for error messages on startup

**Grace period not working after upgrade?**
→ Delete `config.yml` and let it regenerate — the new `bad-internet`/`intentional` sub-sections must be present

**SQLite errors on startup?**
→ Your platform may not be supported by the bundled natives — switch to MySQL

**Config changes not applying?**
→ Use `/combat reload` — all settings reload without restart
→ Check console for YAML syntax errors

**Sound profiles not changing after reload?**
→ Update to 1.2.1 — this was fixed in this release

**Double bossbar showing?**
→ Update to 1.2.1 — this was fixed in this release

---

## Support

Open an issue at: https://github.com/muzlik-gm/True-Combat-Manager/issues

Include:
- Server software and version (e.g. Paper 1.21.1)
- Java version
- Plugin version
- Relevant console errors
- Steps to reproduce
