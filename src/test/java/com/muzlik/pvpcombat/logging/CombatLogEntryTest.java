package com.muzlik.pvpcombat.logging;

import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CombatLogEntryTest {

    private UUID sessionId;
    private UUID playerId;
    private UUID targetId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        targetId = UUID.randomUUID();
    }

    @Test
    @Order(1)
    @DisplayName("Builder creates entry with all fields")
    void builderCreatesEntryWithAllFields() {
        LocalDateTime now = LocalDateTime.now();
        CombatLogEntry entry = new CombatLogEntry.Builder()
            .sessionId(sessionId)
            .playerId(playerId)
            .targetId(targetId)
            .eventType(CombatLogEntry.EventType.DAMAGE_DEALT)
            .timestamp(now)
            .damage(15.5)
            .hitLanded(true)
            .distance(5.0)
            .knockbackForce(0.5)
            .weaponType("DIAMOND_SWORD")
            .location("world,100,64,200")
            .additionalData("critical=true")
            .build();

        assertEquals(sessionId, entry.getSessionId());
        assertEquals(playerId, entry.getPlayerId());
        assertEquals(targetId, entry.getTargetId());
        assertEquals(CombatLogEntry.EventType.DAMAGE_DEALT, entry.getEventType());
        assertEquals(now, entry.getTimestamp());
        assertEquals(15.5, entry.getDamage(), 0.001);
        assertTrue(entry.isHitLanded());
        assertEquals(5.0, entry.getDistance(), 0.001);
        assertEquals(0.5, entry.getKnockbackForce(), 0.001);
        assertEquals("DIAMOND_SWORD", entry.getWeaponType());
        assertEquals("world,100,64,200", entry.getLocation());
        assertEquals("critical=true", entry.getAdditionalData());
    }

    @Test
    @Order(2)
    @DisplayName("Builder uses current timestamp by default")
    void builderUsesCurrentTimestampByDefault() {
        LocalDateTime before = LocalDateTime.now();
        CombatLogEntry entry = new CombatLogEntry.Builder()
            .sessionId(sessionId)
            .playerId(playerId)
            .eventType(CombatLogEntry.EventType.COMBAT_START)
            .build();
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(entry.getTimestamp());
        assertFalse(entry.getTimestamp().isBefore(before));
        assertFalse(entry.getTimestamp().isAfter(after));
    }

    @Test
    @Order(3)
    @DisplayName("Builder uses default values for optional fields")
    void builderUsesDefaultValues() {
        CombatLogEntry entry = new CombatLogEntry.Builder()
            .sessionId(sessionId)
            .playerId(playerId)
            .eventType(CombatLogEntry.EventType.COMBAT_END)
            .build();

        assertNull(entry.getTargetId());
        assertEquals(0.0, entry.getDamage(), 0.001);
        assertFalse(entry.isHitLanded());
        assertEquals(0.0, entry.getDistance(), 0.001);
        assertEquals(0.0, entry.getKnockbackForce(), 0.001);
        assertEquals("", entry.getWeaponType());
        assertEquals("", entry.getLocation());
        assertEquals("", entry.getAdditionalData());
    }

    @Test
    @Order(4)
    @DisplayName("Builder throws when sessionId is null")
    void builderThrowsOnNullSessionId() {
        assertThrows(IllegalStateException.class, () ->
            new CombatLogEntry.Builder()
                .playerId(playerId)
                .eventType(CombatLogEntry.EventType.COMBAT_START)
                .build()
        );
    }

    @Test
    @Order(5)
    @DisplayName("Builder throws when playerId is null")
    void builderThrowsOnNullPlayerId() {
        assertThrows(IllegalStateException.class, () ->
            new CombatLogEntry.Builder()
                .sessionId(sessionId)
                .eventType(CombatLogEntry.EventType.COMBAT_START)
                .build()
        );
    }

    @Test
    @Order(6)
    @DisplayName("Builder throws when eventType is null")
    void builderThrowsOnNullEventType() {
        assertThrows(IllegalStateException.class, () ->
            new CombatLogEntry.Builder()
                .sessionId(sessionId)
                .playerId(playerId)
                .build()
        );
    }

    @Test
    @Order(7)
    @DisplayName("Builder throws when all three required fields are missing")
    void builderThrowsOnAllRequiredMissing() {
        assertThrows(IllegalStateException.class, () ->
            new CombatLogEntry.Builder().build()
        );
    }

    @Test
    @Order(8)
    @DisplayName("EventType enum has all expected values")
    void eventTypeHasAllValues() {
        assertEquals(11, CombatLogEntry.EventType.values().length);
        assertNotNull(CombatLogEntry.EventType.valueOf("COMBAT_START"));
        assertNotNull(CombatLogEntry.EventType.valueOf("DAMAGE_DEALT"));
        assertNotNull(CombatLogEntry.EventType.valueOf("DAMAGE_RECEIVED"));
        assertNotNull(CombatLogEntry.EventType.valueOf("HIT_LANDED"));
        assertNotNull(CombatLogEntry.EventType.valueOf("HIT_MISSED"));
        assertNotNull(CombatLogEntry.EventType.valueOf("KNOCKBACK_GIVEN"));
        assertNotNull(CombatLogEntry.EventType.valueOf("KNOCKBACK_RECEIVED"));
        assertNotNull(CombatLogEntry.EventType.valueOf("MOVEMENT"));
        assertNotNull(CombatLogEntry.EventType.valueOf("COMBAT_END"));
        assertNotNull(CombatLogEntry.EventType.valueOf("TIMER_RESET"));
        assertNotNull(CombatLogEntry.EventType.valueOf("INTERFERENCE_DETECTED"));
    }

    @Test
    @Order(9)
    @DisplayName("toString returns formatted string")
    void toStringReturnsFormattedString() {
        CombatLogEntry entry = new CombatLogEntry.Builder()
            .sessionId(sessionId)
            .playerId(playerId)
            .eventType(CombatLogEntry.EventType.COMBAT_START)
            .additionalData("Combat started")
            .build();

        String str = entry.toString();
        assertNotNull(str);
        assertTrue(str.contains(playerId.toString()));
        assertTrue(str.contains("COMBAT_START"));
        assertTrue(str.contains(sessionId.toString()));
        assertTrue(str.contains("Combat started"));
    }

    @Test
    @Order(10)
    @DisplayName("Builder is chainable")
    void builderIsChainable() {
        CombatLogEntry entry = new CombatLogEntry.Builder()
            .sessionId(sessionId)
            .playerId(playerId)
            .targetId(targetId)
            .eventType(CombatLogEntry.EventType.KNOCKBACK_GIVEN)
            .damage(0.0)
            .hitLanded(false)
            .distance(3.0)
            .knockbackForce(0.8)
            .weaponType("STICK")
            .location("nether")
            .additionalData("potion_effect")
            .build();

        assertEquals(0.8, entry.getKnockbackForce(), 0.001);
        assertEquals("STICK", entry.getWeaponType());
        assertEquals("nether", entry.getLocation());
    }

    @Test
    @Order(11)
    @DisplayName("Builder allows null targetId")
    void builderAllowsNullTargetId() {
        CombatLogEntry entry = new CombatLogEntry.Builder()
            .sessionId(sessionId)
            .playerId(playerId)
            .eventType(CombatLogEntry.EventType.MOVEMENT)
            .build();

        assertNull(entry.getTargetId());
    }

    @Test
    @Order(12)
    @DisplayName("Multiple entries have unique instances")
    void multipleEntriesAreUnique() {
        CombatLogEntry e1 = new CombatLogEntry.Builder()
            .sessionId(sessionId).playerId(playerId)
            .eventType(CombatLogEntry.EventType.COMBAT_START).build();

        CombatLogEntry e2 = new CombatLogEntry.Builder()
            .sessionId(sessionId).playerId(playerId)
            .eventType(CombatLogEntry.EventType.COMBAT_END).build();

        assertNotSame(e1, e2);
    }

    @Test
    @Order(13)
    @DisplayName("Damage field accepts zero and negative values")
    void damageAcceptsZeroAndNegative() {
        CombatLogEntry zeroDamage = new CombatLogEntry.Builder()
            .sessionId(sessionId).playerId(playerId)
            .eventType(CombatLogEntry.EventType.DAMAGE_DEALT)
            .damage(0.0).build();
        assertEquals(0.0, zeroDamage.getDamage(), 0.001);

        CombatLogEntry negativeDamage = new CombatLogEntry.Builder()
            .sessionId(sessionId).playerId(playerId)
            .eventType(CombatLogEntry.EventType.DAMAGE_DEALT)
            .damage(-5.0).build();
        assertEquals(-5.0, negativeDamage.getDamage(), 0.001);
    }
}
