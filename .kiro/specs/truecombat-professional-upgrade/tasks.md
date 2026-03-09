# Implementation Plan: TrueCombatManager Professional Upgrade

## Overview

This implementation plan breaks down the professional upgrade into discrete, testable tasks. Each task builds on previous work and includes specific requirements references. Tasks are organized into phases for systematic implementation.

## Tasks

### Phase 1: Critical Fixes & Foundation

- [x] 0. Fix SQLite Native Library Loading (CRITICAL PRODUCTION BUG)
  - Modify pom.xml Maven Shade Plugin configuration to properly handle SQLite native libraries
  - Remove the blanket META-INF/* exclusion that breaks SQLite native library loading
  - Add specific exclusions for only unnecessary META-INF files (signatures, etc.)
  - Keep META-INF/native/** files that SQLite JDBC needs for native library extraction
  - Test the shaded JAR to ensure SQLite can load native libraries on Windows, Linux, and macOS
  - Verify the plugin starts successfully with SQLite database
  - _Requirements: 5.1, 5.7 (Database functionality depends on this fix)_

- [x] 1. Complete CombatEventListener Implementation
  - Read the complete CombatEventListener.java file (all 866 lines)
  - Complete the truncated onPlayerCommand method
  - Verify all event handlers are present and functional
  - Add missing event handlers (respawn anchor, trident riptide, crystal placement/breaking)
  - Test command blocking with various formats (aliases, plugin prefixes, case sensitivity)
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ]* 1.1 Write property test for command blocking
  - **Property 18: Command Blocking**
  - **Validates: Requirements 1.2, 1.3**

- [x] 2. Implement Thread-Safe Combat Session Management
  - Add ReadWriteLock to CombatManager for session operations
  - Implement withSessionLock helper methods for atomic operations
  - Replace HashMap with ConcurrentHashMap for activeSessions
  - Add synchronization to all session state transitions
  - Document thread safety guarantees in JavaDoc
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [ ]* 2.1 Write property test for thread-safe session access
  - **Property 3: Thread-Safe Session Access**
  - **Validates: Requirements 3.1, 3.2, 3.3**

- [x] 3. Create ConfigValidator Component
  - Create ConfigValidator class with validation methods
  - Implement validateConfig method for full config validation
  - Implement validateCombatSettings for combat section
  - Implement validateRestrictions for restrictions section
  - Implement validateVisualSettings for visual section
  - Add validation for numeric ranges, enums, and region names
  - Create ConfigValidationError class for error reporting
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

- [ ]* 3.1 Write property test for config validation
  - **Property 4: Config Validation Completeness**
  - **Validates: Requirements 4.1, 4.2, 4.3**

- [x] 4. Implement Enhanced Error Handling
  - Wrap all critical operations in try-catch blocks
  - Add error logging with full stack traces
  - Implement graceful recovery for common errors
  - Add circuit breaker pattern for failing operations
  - Create error notification system for admins
  - Ensure no unhandled exceptions reach Bukkit
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7_

- [ ]* 4.1 Write unit tests for error handling
  - Test error recovery patterns
  - Test circuit breaker functionality
  - Test graceful degradation
  - _Requirements: 7.3, 7.5_

- [x] 5. Checkpoint - Verify Phase 1 Completion
  - Ensure all tests pass
  - Verify no compilation errors
  - Check code coverage (target: 60%+)
  - Ask user if questions arise


### Phase 2: Database & Persistence

- [x] 6. Create DatabaseManager Interface and Implementation
  - Create IDatabaseManager interface with all required methods
  - Implement SQLiteDatabaseManager for SQLite support
  - Implement MySQLDatabaseManager for MySQL support
  - Add HikariCP for connection pooling
  - Implement connection health checks
  - Add automatic reconnection logic
  - _Requirements: 5.1, 5.2, 5.5, 5.7_

- [ ]* 6.1 Write property test for database round-trip
  - **Property 5: Database Persistence Round-Trip**
  - **Validates: Requirements 5.1, 5.2**

- [x] 7. Implement Database Schema and Migration System
  - Create initial database schema (version 1)
  - Implement schema versioning system
  - Create migration scripts for schema updates
  - Add migrateSchema method to DatabaseManager
  - Test migrations from v1 to v2, v2 to v3
  - _Requirements: 5.6_

- [ ]* 7.1 Write unit tests for database migrations
  - Test schema version detection
  - Test migration execution
  - Test rollback on migration failure
  - _Requirements: 5.6_

- [x] 8. Integrate Database with CombatTracker
  - Add DatabaseManager dependency to CombatTracker
  - Implement savePlayerData method
  - Implement loadPlayerData method
  - Implement saveBatch for bulk operations
  - Add auto-save task (every 5 minutes)
  - Load existing data on plugin startup
  - Save all data on plugin shutdown
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ]* 8.1 Write integration tests for CombatTracker persistence
  - Test save and load operations
  - Test auto-save functionality
  - Test data integrity across restarts
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 9. Checkpoint - Verify Phase 2 Completion
  - Ensure all database tests pass
  - Verify data persists across server restarts
  - Check for memory leaks in connection pooling
  - Test both SQLite and MySQL implementations
  - Ask user if questions arise

### Phase 3: Feature Completion

- [x] 10. Implement WorldGuard Integration Without Reflection
  - Create WorldGuardIntegration class
  - Use WorldGuard API directly (no reflection)
  - Implement isInSafeZone method with caching
  - Implement isInProtectedRegion method
  - Add Guava LoadingCache with 5-second TTL
  - Handle WorldGuard not installed gracefully
  - _Requirements: 6.1, 6.2, 6.3, 6.5, 6.6_

- [ ]* 10.1 Write property test for safe zone detection
  - **Property 6: Safe Zone Detection Accuracy**
  - **Validates: Requirements 6.1, 6.3**

- [x] 11. Complete All Restriction Implementations
  - Implement respawn anchor blocking in CombatEventListener
  - Implement trident riptide blocking
  - Implement end crystal placement/breaking blocking
  - Implement block breaking/placing restrictions
  - Add proper cooldown management for all restrictions
  - Display appropriate messages for each restriction type
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7_

- [ ]* 11.1 Write property test for restriction enforcement
  - **Property 7: Restriction Enforcement**
  - **Validates: Requirements 10.1, 10.2, 10.3, 10.4**

- [ ]* 11.2 Write property test for cooldown expiration
  - **Property 12: Cooldown Expiration**
  - **Validates: Requirements 10.5**

- [x] 12. Improve Newbie Protection System
  - Fix armor checking (handle null vs AIR material correctly)
  - Fix XP level threshold checking
  - Reduce timed protection reminder frequency (60s, 30s, 10s, 5s only)
  - Add cleanup task for expired protection data
  - Handle player logout/login correctly for timed protection
  - Add admin commands: /combat protect <player> <seconds>, /combat unprotect <player>
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7_

- [ ]* 12.1 Write property test for newbie protection armor check
  - **Property 8: Newbie Protection Armor Check**
  - **Validates: Requirements 11.1, 11.2**

- [x] 13. Implement Combat Timer Reset Logic
  - Modify onEntityDamage to reset timer on any damage
  - Apply lag compensation to timer resets
  - Update visual elements immediately after reset
  - Play timer reset sound when configured
  - Log timer resets when console logging enabled
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_

- [ ]* 13.1 Write property test for timer reset consistency
  - **Property 2: Timer Reset Consistency**
  - **Validates: Requirements 8.1, 8.2**

- [x] 14. Implement Multi-Attacker Combat Handling
  - Add playerOpponents map to track multiple opponents per player
  - Modify startCombat to support multiple simultaneous combats
  - Add getOpponents method returning Set<Player>
  - Add isInCombatWith method for specific opponent checking
  - Implement max concurrent sessions limit (configurable)
  - Display most recent opponent in visual elements
  - End each combat session independently
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

- [ ]* 14.1 Write property test for multi-combat session independence
  - **Property 9: Multi-Combat Session Independence**
  - **Validates: Requirements 9.2, 9.3, 9.4**

- [x] 15. Checkpoint - Verify Phase 3 Completion
  - Ensure all feature tests pass
  - Test multi-combat scenarios manually
  - Verify safe zone detection works correctly
  - Test all restriction types
  - Check newbie protection improvements
  - Ask user if questions arise


### Phase 4: Visual System & UX

- [x] 16. Implement Complete Visual Theme System
  - Create Theme enum with all 6 themes (Default, Minimal, Intense, Elegant, Neon, Retro)
  - Implement theme switching in /combat toggle-style command
  - Add theme persistence to database
  - Load player theme preferences on join
  - Ensure consistent color code handling (& and § both supported)
  - Sync BossBar and ActionBar updates
  - Apply sound profile based on selected theme
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7_

- [ ]* 16.1 Write property test for visual theme persistence
  - **Property 14: Visual Theme Persistence**
  - **Validates: Requirements 12.3**

- [x] 17. Enhance Combat Damage Tracking
  - Create DamageInfo class with weapon type, critical hit, distance, timestamp
  - Modify recordDamage to accept DamageInfo parameter
  - Track damage by weapon type in SessionPlayerData
  - Track critical hits separately
  - Track damage by distance (melee vs ranged)
  - Calculate combo hits from hit timestamps
  - Add damage breakdown to combat summaries
  - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5, 17.6_

- [ ]* 17.1 Write property test for damage tracking accuracy
  - **Property 11: Damage Tracking Accuracy**
  - **Validates: Requirements 17.1, 17.2, 17.3**

- [x] 18. Implement Enhanced Combat End Conditions
  - Add CombatEndReason enum (TIMER_EXPIRED, DEATH, DISCONNECT, FORFEIT, ADMIN_CLEAR)
  - Modify endCombat to accept CombatEndReason parameter
  - Fire appropriate events for each end condition
  - Clean up all visual elements on combat end
  - Clean up all timers on combat end
  - Clean up all session data on combat end
  - Record appropriate statistics based on end reason
  - _Requirements: 18.1, 18.2, 18.3, 18.4, 18.5, 18.6, 18.7_

- [ ]* 18.1 Write property test for combat end cleanup
  - **Property 10: Combat End Cleanup**
  - **Validates: Requirements 18.1, 18.2, 18.6, 18.7**

- [ ]* 18.2 Write property test for disconnect grace period
  - **Property 13: Disconnect Grace Period**
  - **Validates: Requirements 18.3**

- [x] 19. Checkpoint - Verify Phase 4 Completion
  - Test all 6 visual themes
  - Verify theme persistence works
  - Test damage tracking with various weapons
  - Test all combat end conditions
  - Check visual cleanup on combat end
  - Ask user if questions arise

### Phase 5: Admin Tools & Polish

- [x] 20. Implement Enhanced Admin Commands
  - Implement /combat inspect <player> with detailed status
  - Implement /combat debug with real-time performance metrics
  - Implement /combat stats with server-wide statistics
  - Implement /combat reload without server restart
  - Implement /combat clear <player> to force-end combat
  - Implement /combat protect <player> <seconds> for manual protection
  - Implement /combat unprotect <player> to remove protection
  - Add tab completion for all admin commands
  - Log all admin commands with timestamps
  - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5, 16.6, 16.7_

- [ ]* 20.1 Write unit tests for admin commands
  - Test each command with valid inputs
  - Test each command with invalid inputs
  - Test permission checking
  - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5_

- [x] 21. Implement Configuration Documentation
  - Add inline comments for every config option
  - Document valid value ranges for numeric options
  - Document valid enum values for choice options
  - Create example configurations for common server types (hardcore, casual, balanced)
  - Create config migration guide
  - Implement config validation on load with suggestions
  - Generate default config with all options documented
  - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5, 19.6, 19.7_

- [ ]* 21.1 Write unit tests for config documentation
  - Verify all options have comments
  - Verify all examples are valid
  - Test config generation
  - _Requirements: 19.1, 19.7_

- [x] 22. Implement Performance Optimizations
  - Move all non-critical operations to async threads
  - Implement connection pooling for database with HikariCP
  - Add caching with proper TTL management (Caffeine)
  - Implement batch database writes
  - Ensure main thread is never blocked
  - Add performance metrics to /combat debug
  - Respect console-enabled config for logging overhead
  - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7_

- [ ]* 22.1 Write property test for async operations
  - **Property 16: Performance Async Operations**
  - **Validates: Requirements 14.1, 14.5**

- [ ]* 22.2 Write property test for statistics auto-save
  - **Property 19: Statistics Auto-Save**
  - **Validates: Requirements 5.4**

- [x] 23. Implement Version Compatibility Layer
  - Add server version detection on startup
  - Support Minecraft 1.19.4 through 1.21+
  - Log clear warning for unsupported versions
  - Use version-compatible API calls
  - Handle deprecated API methods gracefully
  - Test against multiple Minecraft versions
  - Document supported versions in README
  - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5, 15.6, 15.7_

- [ ]* 23.1 Write property test for version compatibility detection
  - **Property 20: Version Compatibility Detection**
  - **Validates: Requirements 15.1, 15.3**

- [x] 24. Checkpoint - Verify Phase 5 Completion
  - Test all admin commands
  - Verify config documentation is complete
  - Run performance benchmarks
  - Test on multiple Minecraft versions
  - Check code coverage (target: 80%+)
  - Ask user if questions arise


### Phase 6: Advanced Features

- [ ] 25. Implement Cross-Server Combat Synchronization
  - Create NetworkSyncManager for BungeeCord/Velocity
  - Implement combat start event broadcasting
  - Implement combat end event broadcasting
  - Implement server switch prevention during combat
  - Add periodic combat state sync (every 30 seconds)
  - Handle network timeouts gracefully
  - Support both BungeeCord and Velocity
  - Auto-detect proxy platform
  - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 13.6, 13.7_

- [ ]* 25.1 Write property test for cross-server combat sync
  - **Property 15: Cross-Server Combat Sync**
  - **Validates: Requirements 13.1, 13.2, 13.4**

- [ ] 26. Implement Combat Replay System
  - Create ReplayManager for recording combat events
  - Record all damage events with timestamps
  - Record movement events during combat
  - Record item usage events during combat
  - Store replays in compressed format
  - Implement /replay view <session-id> command
  - Implement /replay list <player> command
  - Auto-delete replays older than retention period
  - Limit replay storage size
  - Allow exporting replays to JSON format
  - _Requirements: 20.1, 20.2, 20.3, 20.4, 20.5, 20.6, 20.7_

- [ ]* 26.1 Write unit tests for replay system
  - Test replay recording
  - Test replay playback
  - Test replay storage limits
  - Test replay auto-deletion
  - _Requirements: 20.1, 20.2, 20.5, 20.6_

- [ ] 27. Create Plugin API for Third-Party Integration
  - Create public API interfaces for other plugins
  - Expose combat state queries
  - Expose combat events
  - Expose statistics access
  - Create API documentation
  - Provide example integration code
  - Version the API for stability
  - _Requirements: Not explicitly in requirements, but valuable for professional plugin_

- [ ]* 27.1 Write integration tests for plugin API
  - Test API usage from external plugin
  - Test event listening
  - Test data access
  - _Requirements: API functionality_

- [ ] 28. Checkpoint - Verify Phase 6 Completion
  - Test cross-server sync on test network
  - Test replay system functionality
  - Verify API works for third-party plugins
  - Check all advanced features work together
  - Ask user if questions arise

### Phase 7: Comprehensive Testing & Documentation

- [x] 29. Write Comprehensive Property-Based Tests
  - Write property test for combat session uniqueness (Property 1)
  - Write property test for error recovery (Property 17)
  - Ensure all 20 properties have corresponding tests
  - Run all property tests with 100+ iterations
  - Fix any failures discovered by property tests
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

- [ ] 30. Write Integration Tests
  - Test full combat flow (start → damage → timer → end)
  - Test multi-player combat scenarios
  - Test disconnect and reconnect flow
  - Test config reload without restart
  - Test database migration
  - Test cross-server sync with mock proxy
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [ ] 31. Write Performance Tests
  - Benchmark combat session creation time (target: < 5ms)
  - Benchmark timer update overhead (target: < 1ms)
  - Benchmark database save time (target: < 50ms)
  - Measure cache hit rate (target: > 90%)
  - Measure memory usage (target: < 100MB for 1000 sessions)
  - Load test with 100 concurrent combat sessions
  - Load test with 1000 damage events per second
  - Load test with 50 players joining/leaving per minute
  - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7_

- [ ] 32. Update Documentation
  - Update README with all new features
  - Document all configuration options
  - Create admin guide
  - Create player guide
  - Create developer API guide
  - Add troubleshooting section
  - Add FAQ section
  - Create video tutorials (optional)
  - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5_

- [ ] 33. Final Code Review and Cleanup
  - Review all code for consistency
  - Remove debug code and comments
  - Optimize imports
  - Format code consistently
  - Update JavaDoc for all public methods
  - Check for code smells
  - Run static analysis tools (SpotBugs, PMD)
  - _Requirements: All requirements_

- [ ] 34. Final Checkpoint - Production Readiness
  - Verify all 20 requirements are met
  - Ensure all tests pass (unit, property, integration, performance)
  - Check code coverage is 80%+
  - Verify no critical bugs remain
  - Test on production-like environment
  - Get user approval for release
  - Ask user if questions arise

## Notes

- Tasks marked with `*` are optional test tasks that can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Integration tests validate end-to-end flows
- Performance tests validate scalability and efficiency

## Estimated Timeline

- **Phase 1 (Critical Fixes):** 2 weeks
- **Phase 2 (Database):** 1 week
- **Phase 3 (Features):** 2 weeks
- **Phase 4 (Visual/UX):** 1 week
- **Phase 5 (Admin Tools):** 1 week
- **Phase 6 (Advanced):** 2 weeks
- **Phase 7 (Testing/Docs):** 1 week

**Total:** 10 weeks for complete professional upgrade

## Success Criteria

- ✅ All 20 requirements fully implemented
- ✅ 80%+ code coverage with tests
- ✅ All property tests passing (100+ iterations each)
- ✅ All integration tests passing
- ✅ Performance benchmarks met
- ✅ Zero critical bugs
- ✅ Complete documentation
- ✅ Production-ready quality
