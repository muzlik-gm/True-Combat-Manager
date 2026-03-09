# Installation Guide

## Quick Start

The plugin is now **fully self-contained** and ready to use! Just drop it in your plugins folder.

## Requirements

### Server Requirements
- **Minecraft:** 1.19.4 - 1.21+ (Paper/Spigot)
- **Java:** 21+
- **Platform:** Windows x64, Linux x64, or Linux ARM64 (Docker)

### Included Dependencies
The plugin includes all essential dependencies:
- ✅ **SQLite JDBC** - Built-in database support (Windows x64, Linux x64, Linux ARM64)
- ✅ **HikariCP** - Connection pooling
- ✅ **Gson** - JSON handling
- ✅ **Caffeine** - High-performance caching

### Optional Dependencies
- **PlaceholderAPI** (for placeholder support)
- **WorldGuard** (for region-based combat restrictions)
- **MySQL Connector** (only if using MySQL instead of SQLite - must be added separately)

## Installation Steps

1. Download `truecombatmanager-1.0.2.jar`
2. Place it in your server's `plugins` folder
3. Start/restart your server
4. Configure the plugin in `plugins/TrueCombatManager/config.yml`
5. Reload with `/combat reload`

That's it! No additional downloads needed.

## Platform Support

The plugin includes native SQLite libraries for:
- ✅ **Windows x64** - Most Windows servers
- ✅ **Linux x64** - Most Linux servers
- ✅ **Linux ARM64** - Docker containers and ARM-based servers

If you're running on Mac or other platforms, you'll need to add the full sqlite-jdbc library separately.

## Size Optimization

The plugin JAR is **2.4 MB** instead of 14+ MB by:
- Including only essential platform natives (Windows x64, Linux x64, Linux ARM64)
- Excluding Mac, FreeBSD, 32-bit systems, and other uncommon platforms
- Using JAR minimization to remove unused classes
- Bundling only what 99% of servers actually need
