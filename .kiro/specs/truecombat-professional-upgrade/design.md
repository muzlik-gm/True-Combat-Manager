# Design Document: TrueCombatManager Professional Upgrade

## Overview

This design document outlines the architecture and implementation approach for upgrading TrueCombatManager to professional-grade quality. The upgrade focuses on reliability, testability, performance, and feature completeness while maintaining backward compatibility with existing configurations.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     PvPCombatPlugin                         │
│                   (Main Plugin Class)                       │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                     PluginManager                           │
│              (Lifecycle Coordinator)                        │
└─┬───────┬───────┬───────┬───────┬───────┬───────┬──────────┘
  │       │       │       │       │       │       │
  ▼       ▼       ▼       ▼       ▼       ▼       ▼
┌───┐   ┌───┐   ┌───┐   ┌───┐   ┌───┐   ┌───┐   ┌───┐
│CM │   │RM │   │VM │   │NP │   │CT │   │DT │   │DB │
└───┘   └───┘   └───┘   └───┘   └───┘   └───┘   └───┘
Combat  Rest.   Visual  Newbie  Combat  Disc.   Database
Manager Manager Manager Protect Tracker Tracker Manager

Legend:
CM = CombatManager
RM = RestrictionManager  
VM = VisualManager
NP = NewbieProtection
CT = CombatTracker
DT = DisconnectTracker
DB = DatabaseManager
```

### Component Responsibilities

**CombatManager**
- Manages active combat sessions
- Handles combat start/end logic
- Coordinates timer updates
- Thread-safe session operations

**RestrictionManager**
- Enforces item/command restrictions
- Manages cooldowns
- Validates restriction rules

**VisualManager**
- Handles BossBar/ActionBar display
- Manages sound effects
- Supports theme customization

**NewbieProtection**
- Checks armor/XP requirements
- Manages timed protection
- Handles protection messages

**CombatTracker**
- Tracks combat statistics
- Calculates win/loss records
- Provides damage analytics

**DisconnectTracker**
- Handles combat logging
- Manages grace periods
- Applies punishments

**DatabaseManager** (NEW)
- Handles persistent storage
- Manages connections
- Provides migration system

## Components and Interfaces

### 1. DatabaseManager (NEW Component)

```java
public interface IDatabaseManager {
    // Connection management
    void initialize();
    void shutdown();
    Connection getConnection() throws SQLException;
    
    // Player data operations
    void savePlayerData(UUID playerId, PlayerCombatData data);
    PlayerCombatData loadPlayerData(UUID playerId);
    
    // Batch operations
    void saveBatch(Map<UUID, PlayerCombatData> dataMap);
    
    // Migration
    void migrateSchema(int fromVersion, int toVersion);
    
    // Health check
    boolean isHealthy();
}
```

**Implementation Details:**
- Support both SQLite (default) and MySQL
- Use HikariCP for connection pooling
- Implement automatic reconnection
- Batch writes every 5 minutes
- Schema versioning system

### 2. Enhanced CombatManager

```java
public class CombatManager implements ICombatManager {
    // Thread-safe session storage
    private final ConcurrentHashMap<UUID, CombatSession> activeSessions;
    private final ReadWriteLock sessionLock;
    
    // Multi-combat support
    private final ConcurrentHashMap<UUID, Set<UUID>> playerOpponents;
    
    // Enhanced methods
    UUID startCombat(Player attacker, Player defender);
    boolean endCombat(UUID playerId, CombatEndReason reason);
    Set<Player> getOpponents(Player player);
    boolean isInCombatWith(Player player1, Player player2);
    
    // Thread-safe operations
    void withSessionLock(Runnable operation);
    <T> T withSessionLock(Supplier<T> operation);
}
```

**Key Improvements:**
- ReadWriteLock for better concurrency
- Support for multiple simultaneous combats
- Explicit end reasons for better tracking
- Atomic session operations

### 3. ConfigValidator (NEW Component)

```java
public class ConfigValidator {
    // Validation results
    public static class ValidationResult {
        boolean isValid;
        List<String> errors;
        List<String> warnings;
    }
    
    // Validation methods
    ValidationResult validateConfig(FileConfiguration config);
    ValidationResult validateCombatSettings(ConfigurationSection section);
    ValidationResult validateRestrictions(ConfigurationSection section);
    ValidationResult validateVisualSettings(ConfigurationSection section);
    
    // Migration
    void migrateConfig(File configFile, int fromVersion, int toVersion);
}
```

**Validation Rules:**
- Numeric ranges (duration > 0, cooldowns >= 0)
- Enum values (colors, materials, sounds)
- Region existence (WorldGuard integration)
- Command list format
- Theme configuration completeness

### 4. Enhanced CombatEventListener

```java
public class CombatEventListener implements Listener {
    // Complete all truncated methods
    @EventHandler(priority = EventPriority.LOWEST)
    void onPlayerCommand(PlayerCommandPreprocessEvent event);
    
    // Add missing handlers
    @EventHandler
    void onRespawnAnchorUse(PlayerInteractEvent event);
    
    @EventHandler
    void onTridentRiptide(PlayerRiptideEvent event);
    
    @EventHandler
    void onCrystalPlace(BlockPlaceEvent event);
    
    @EventHandler
    void onCrystalBreak(BlockBreakEvent event);
    
    // Improved safe zone checking
    private boolean isInSafeZone(Location location);
    private boolean isInSafeZone(Player player);
}
```

### 5. WorldGuardIntegration (NEW Component)

```java
public class WorldGuardIntegration {
    private final WorldGuardPlugin worldGuard;
    private final RegionContainer regionContainer;
    private final LoadingCache<Location, Boolean> safeZoneCache;
    
    // Safe zone detection without reflection
    boolean isInSafeZone(Location location);
    boolean isInProtectedRegion(Location location, String regionName);
    Set<String> getRegionsAt(Location location);
    
    // Cache management
    void invalidateCache();
    void invalidateCache(Location location);
}
```

**Implementation:**
- Direct WorldGuard API usage (no reflection)
- Guava LoadingCache for performance
- 5-second cache TTL
- Graceful degradation if WorldGuard unavailable

### 6. Enhanced RestrictionManager

```java
public class RestrictionManager implements IRestrictionManager {
    // Complete implementations
    boolean canUseRespawnAnchor(Player player);
    boolean canUseRiptide(Player player);
    boolean canPlaceCrystal(Player player);
    boolean canBreakCrystal(Player player);
    
    // Cooldown management
    void applyCooldown(Player player, RestrictionType type, int seconds);
    int getRemainingCooldown(Player player, RestrictionType type);
    void clearAllCooldowns(Player player);
    
    // Restriction types enum
    enum RestrictionType {
        ENDER_PEARL, ELYTRA, GOLDEN_APPLE, ENCHANTED_GOLDEN_APPLE,
        TRIDENT, RIPTIDE, RESPAWN_ANCHOR, END_CRYSTAL, BLOCKS, TELEPORT
    }
}
```

### 7. Enhanced CombatTracker

```java
public class CombatTracker {
    private final IDatabaseManager database;
    private final Map<UUID, PlayerCombatData> cache;
    
    // Granular damage tracking
    void recordDamage(Player attacker, Player defender, DamageInfo info);
    
    // Damage info class
    public static class DamageInfo {
        double amount;
        WeaponType weapon;
        boolean isCritical;
        double distance;
        long timestamp;
    }
    
    // Statistics retrieval
    PlayerCombatData getPlayerData(UUID playerId);
    CombatSessionStats getSessionStats(UUID sessionId);
    Map<WeaponType, Double> getDamageByWeapon(UUID playerId);
    
    // Persistence
    void saveAll();
    void loadAll();
    void autoSave(); // Called every 5 minutes
}
```

## Data Models

### Enhanced CombatSession

```java
public class CombatSession {
    private final UUID sessionId;
    private final Player attacker;
    private final Player defender;
    private final long startTime;
    private final TimerData timerData;
    private final Map<UUID, SessionPlayerData> playerData;
    private CombatState state;
    private volatile boolean active;
    
    // New fields
    private final List<DamageEvent> damageHistory;
    private final AtomicInteger timerResets;
    private CombatEndReason endReason;
    
    // Thread-safe operations
    public synchronized void recordDamage(Player damager, DamageInfo info);
    public synchronized void resetTimer();
    public synchronized boolean updateTimer();
}
```

### SessionPlayerData

```java
public class SessionPlayerData {
    private double damageDealt;
    private double damageReceived;
    private int hitsLanded;
    private int hitsTaken;
    private int criticalHits;
    private Map<WeaponType, Integer> weaponUsage;
    private List<Long> hitTimestamps;
    
    // Calculated properties
    public double getAccuracy();
    public double getAverageDamagePerHit();
    public int getComboCount();
}
```

### PlayerCombatData (Enhanced)

```java
public class PlayerCombatData {
    private final UUID playerId;
    private int wins;
    private int losses;
    private double totalDamageDealt;
    private double totalDamageReceived;
    private long totalCombatTime;
    private LocalDateTime lastCombat;
    
    // New fields
    private Map<WeaponType, WeaponStats> weaponStats;
    private int criticalHits;
    private int longestCombo;
    private double highestDamageInSession;
    
    // Persistence
    public Map<String, Object> serialize();
    public static PlayerCombatData deserialize(Map<String, Object> data);
}
```

### WeaponStats

```java
public class WeaponStats {
    private final WeaponType type;
    private int uses;
    private double totalDamage;
    private int kills;
    
    public double getAverageDamage() {
        return uses > 0 ? totalDamage / uses : 0;
    }
}
```

### ConfigValidationError

```java
public class ConfigValidationError {
    private final String path;
    private final String message;
    private final Object invalidValue;
    private final Object suggestedValue;
    private final Severity severity;
    
    public enum Severity {
        ERROR,   // Plugin cannot start
        WARNING, // Plugin works but suboptimal
        INFO     // Informational message
    }
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Combat Session Uniqueness
*For any* two players, at most one active combat session should exist between them at any given time.
**Validates: Requirements 1.1, 9.2**

### Property 2: Timer Reset Consistency
*For any* combat session, when damage is dealt or received, the timer should reset to the configured duration.
**Validates: Requirements 8.1, 8.2**

### Property 3: Thread-Safe Session Access
*For any* concurrent operations on combat sessions, the final state should be consistent and no data should be corrupted.
**Validates: Requirements 3.1, 3.2, 3.3**

### Property 4: Config Validation Completeness
*For any* configuration file, all invalid values should be detected and reported with helpful messages.
**Validates: Requirements 4.1, 4.2, 4.3**

### Property 5: Database Persistence Round-Trip
*For any* PlayerCombatData object, saving then loading should produce an equivalent object.
**Validates: Requirements 5.1, 5.2**

### Property 6: Safe Zone Detection Accuracy
*For any* location within a configured safe zone region, the system should correctly identify it as a safe zone.
**Validates: Requirements 6.1, 6.3**

### Property 7: Restriction Enforcement
*For any* restricted action during combat, the system should block the action and display the appropriate message.
**Validates: Requirements 10.1, 10.2, 10.3, 10.4**

### Property 8: Newbie Protection Armor Check
*For any* player, the newbie status should correctly reflect their armor equipment and XP level.
**Validates: Requirements 11.1, 11.2**

### Property 9: Multi-Combat Session Independence
*For any* player with multiple active combat sessions, each session should have independent timers and state.
**Validates: Requirements 9.2, 9.3, 9.4**

### Property 10: Combat End Cleanup
*For any* combat session that ends, all visual elements, timers, and session data should be properly cleaned up.
**Validates: Requirements 18.1, 18.2, 18.6, 18.7**

### Property 11: Damage Tracking Accuracy
*For any* damage event, the recorded damage should match the actual damage dealt and be attributed to the correct weapon type.
**Validates: Requirements 17.1, 17.2, 17.3**

### Property 12: Cooldown Expiration
*For any* restriction cooldown, after the configured time has elapsed, the restriction should no longer apply.
**Validates: Requirements 10.5**

### Property 13: Disconnect Grace Period
*For any* player who disconnects during combat, if they reconnect within the grace period, no punishment should be applied.
**Validates: Requirements 18.3**

### Property 14: Visual Theme Persistence
*For any* player who selects a theme, the theme should persist across logout/login.
**Validates: Requirements 12.3**

### Property 15: Cross-Server Combat Sync
*For any* combat session on a networked server, the combat state should be synchronized across all servers within 30 seconds.
**Validates: Requirements 13.1, 13.2, 13.4**

### Property 16: Performance Async Operations
*For any* non-critical operation, it should execute asynchronously without blocking the main thread.
**Validates: Requirements 14.1, 14.5**

### Property 17: Error Recovery
*For any* error in a subsystem, the plugin should continue functioning with degraded capabilities rather than crashing.
**Validates: Requirements 7.3, 7.5**

### Property 18: Command Blocking
*For any* blocked command executed during combat, the command should be cancelled and the player notified.
**Validates: Requirements 1.2, 1.3**

### Property 19: Statistics Auto-Save
*For any* 5-minute interval, all modified statistics should be saved to the database.
**Validates: Requirements 5.4**

### Property 20: Version Compatibility Detection
*For any* Minecraft server version, the plugin should detect the version and log warnings for unsupported versions.
**Validates: Requirements 15.1, 15.3**

## Error Handling

### Error Categories

**1. Configuration Errors**
- Invalid config values
- Missing required fields
- Malformed YAML

**Strategy:** Validate on load, use defaults, log clear errors

**2. Database Errors**
- Connection failures
- Query timeouts
- Schema mismatches

**Strategy:** Retry with exponential backoff, fallback to in-memory, alert admins

**3. WorldGuard Integration Errors**
- Plugin not found
- API version mismatch
- Region not found

**Strategy:** Graceful degradation, disable safe zone features, log warnings

**4. Thread Safety Errors**
- Race conditions
- Deadlocks
- Concurrent modification

**Strategy:** Use proper synchronization, timeout locks, log stack traces

**5. Network Errors (Cross-Server)**
- Connection timeout
- Message loss
- Deserialization failure

**Strategy:** Retry mechanism, queue messages, fallback to local-only mode

### Error Recovery Patterns

```java
// Pattern 1: Retry with Exponential Backoff
public <T> T retryOperation(Supplier<T> operation, int maxRetries) {
    int attempt = 0;
    while (attempt < maxRetries) {
        try {
            return operation.get();
        } catch (Exception e) {
            attempt++;
            if (attempt >= maxRetries) throw e;
            Thread.sleep((long) Math.pow(2, attempt) * 1000);
        }
    }
    throw new RuntimeException("Max retries exceeded");
}

// Pattern 2: Circuit Breaker
public class CircuitBreaker {
    private int failureCount = 0;
    private long lastFailureTime = 0;
    private static final int THRESHOLD = 5;
    private static final long TIMEOUT = 60000; // 1 minute
    
    public boolean isOpen() {
        if (failureCount >= THRESHOLD) {
            if (System.currentTimeMillis() - lastFailureTime > TIMEOUT) {
                reset();
                return false;
            }
            return true;
        }
        return false;
    }
    
    public void recordFailure() {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();
    }
    
    public void reset() {
        failureCount = 0;
    }
}

// Pattern 3: Graceful Degradation
public void performOperation() {
    try {
        // Try primary method
        primaryMethod();
    } catch (Exception e) {
        logger.warning("Primary method failed, using fallback");
        try {
            // Try fallback method
            fallbackMethod();
        } catch (Exception e2) {
            logger.severe("Both methods failed, disabling feature");
            disableFeature();
        }
    }
}
```

## Testing Strategy

### Unit Testing

**Framework:** JUnit 5
**Mocking:** Mockito
**Coverage Target:** 80%

**Test Categories:**
1. Event handler tests (all events)
2. Manager tests (each manager class)
3. Data model tests (serialization/deserialization)
4. Utility tests (helper methods)
5. Config validation tests

**Example Unit Tests:**
- Test newbie protection with various armor combinations
- Test command blocking with different command formats
- Test timer reset on damage
- Test session cleanup on combat end
- Test database save/load operations

### Property-Based Testing

**Framework:** jqwik (Java QuickCheck)
**Iterations:** Minimum 100 per property

**Property Test Examples:**

```java
@Property
void combatSessionUniqueness(@ForAll UUID player1, @ForAll UUID player2) {
    // Setup
    Player p1 = mockPlayer(player1);
    Player p2 = mockPlayer(player2);
    
    // Start combat
    UUID session1 = combatManager.startCombat(p1, p2);
    UUID session2 = combatManager.startCombat(p1, p2);
    
    // Verify only one session exists
    assertThat(session2).isNull();
    assertThat(combatManager.getActiveSessions()).hasSize(1);
}

@Property
void timerResetConsistency(@ForAll @IntRange(min = 1, max = 300) int duration,
                          @ForAll @DoubleRange(min = 0.1, max = 20.0) double damage) {
    // Setup
    config.set("combat.duration", duration);
    CombatSession session = createSession();
    
    // Advance timer
    session.updateTimer();
    int timeBeforeReset = session.getRemainingTime();
    
    // Deal damage (should reset timer)
    session.recordDamage(attacker, new DamageInfo(damage));
    
    // Verify timer reset to full duration
    assertThat(session.getRemainingTime()).isEqualTo(duration);
}

@Property
void databaseRoundTrip(@ForAll PlayerCombatData data) {
    // Save to database
    database.savePlayerData(data.getPlayerId(), data);
    
    // Load from database
    PlayerCombatData loaded = database.loadPlayerData(data.getPlayerId());
    
    // Verify equivalence
    assertThat(loaded).isEqualTo(data);
}

@Property
void restrictionEnforcement(@ForAll RestrictionType type, 
                           @ForAll @IntRange(min = 0, max = 300) int cooldown) {
    // Setup
    Player player = mockPlayer();
    combatManager.startCombat(player, opponent);
    
    // Apply restriction
    restrictionManager.applyCooldown(player, type, cooldown);
    
    // Verify restriction is enforced
    assertThat(restrictionManager.canUse(player, type)).isFalse();
    
    // Advance time past cooldown
    advanceTime(cooldown + 1);
    
    // Verify restriction is lifted
    assertThat(restrictionManager.canUse(player, type)).isTrue();
}
```

### Integration Testing

**Test Scenarios:**
1. Full combat flow (start → damage → timer → end)
2. Multi-player combat scenarios
3. Disconnect and reconnect flow
4. Config reload without restart
5. Database migration
6. Cross-server sync (with mock proxy)

### Performance Testing

**Metrics to Track:**
- Combat session creation time (< 5ms)
- Timer update overhead (< 1ms)
- Database save time (< 50ms)
- Cache hit rate (> 90%)
- Memory usage (< 100MB for 1000 sessions)

**Load Testing:**
- 100 concurrent combat sessions
- 1000 damage events per second
- 50 players joining/leaving per minute

## Implementation Notes

### Phase 1: Critical Fixes (Week 1-2)
1. Complete CombatEventListener
2. Add thread safety to CombatManager
3. Implement ConfigValidator
4. Add basic error handling
5. Write core unit tests

### Phase 2: Database & Persistence (Week 3)
1. Implement DatabaseManager
2. Add SQLite support
3. Add MySQL support
4. Implement auto-save
5. Add migration system

### Phase 3: Feature Completion (Week 4-5)
1. Complete all restriction implementations
2. Improve newbie protection
3. Enhance visual system
4. Add WorldGuard integration
5. Implement multi-combat support

### Phase 4: Testing & Polish (Week 6)
1. Write all property tests
2. Write integration tests
3. Performance optimization
4. Documentation updates
5. Final bug fixes

### Phase 5: Advanced Features (Week 7-8)
1. Cross-server sync
2. Replay system
3. Enhanced admin tools
4. API for other plugins
5. Version compatibility layer

## Dependencies

**Required:**
- Spigot/Paper API 1.19.4+
- Java 21
- Gson (shaded)
- Caffeine (shaded)

**Optional:**
- WorldGuard 7.0+
- PlaceholderAPI 2.11+
- ProtocolLib 5.0+

**Testing:**
- JUnit 5
- Mockito
- jqwik
- Testcontainers (for database tests)

**Database:**
- HikariCP (connection pooling)
- SQLite JDBC
- MySQL Connector (optional)

## Migration Strategy

### Config Migration

```yaml
# Version 2 → Version 3 migration
migrations:
  v2_to_v3:
    - rename: "combat.timer" → "combat.duration"
    - add: "combat.disconnect-protection.enabled" (default: true)
    - remove: "combat.legacy-mode"
    - validate: "combat.duration" (min: 1, max: 600)
```

### Database Schema Migration

```sql
-- Version 1 → Version 2
ALTER TABLE player_stats ADD COLUMN critical_hits INTEGER DEFAULT 0;
ALTER TABLE player_stats ADD COLUMN longest_combo INTEGER DEFAULT 0;
CREATE INDEX idx_player_stats_last_combat ON player_stats(last_combat);

-- Version 2 → Version 3
CREATE TABLE weapon_stats (
    player_id VARCHAR(36),
    weapon_type VARCHAR(50),
    uses INTEGER,
    total_damage DOUBLE,
    kills INTEGER,
    PRIMARY KEY (player_id, weapon_type)
);
```

## Backward Compatibility

**Config Compatibility:**
- Support old config format
- Auto-migrate on first load
- Backup old config before migration

**API Compatibility:**
- Maintain existing public methods
- Deprecate old methods with warnings
- Provide migration guide for plugin developers

**Data Compatibility:**
- Support old data format
- Migrate data on first load
- No data loss during migration
