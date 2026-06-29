package com.muzlik.pvpcombat.data;

import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReplayEventTest {

    private UUID playerId;
    private UUID targetId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        targetId = UUID.randomUUID();
        now = LocalDateTime.now();
    }

    @Test
    @Order(1)
    @DisplayName("Builder should create a valid ReplayEvent")
    void testBuilder() {
        ReplayEvent event = new ReplayEvent.Builder()
            .playerId(playerId)
            .targetId(targetId)
            .eventType(ReplayEvent.ReplayEventType.HIT_LANDED)
            .timestamp(now)
            .damage(12.5)
            .critical(true)
            .location("100,64,-200")
            .weaponType("DIAMOND_SWORD")
            .additionalData("combo:3")
            .build();

        assertEquals(playerId, event.getPlayerId());
        assertEquals(targetId, event.getTargetId());
        assertEquals(ReplayEvent.ReplayEventType.HIT_LANDED, event.getEventType());
        assertEquals(now, event.getTimestamp());
        assertEquals(12.5, event.getDamage(), 0.001);
        assertTrue(event.isCritical());
        assertEquals("100,64,-200", event.getLocation());
        assertEquals("DIAMOND_SWORD", event.getWeaponType());
        assertEquals("combo:3", event.getAdditionalData());
    }

    @Test
    @Order(2)
    @DisplayName("Builder should throw on missing playerId")
    void testBuilderMissingPlayerId() {
        assertThrows(IllegalStateException.class, () ->
            new ReplayEvent.Builder()
                .eventType(ReplayEvent.ReplayEventType.MOVEMENT)
                .build());
    }

    @Test
    @Order(3)
    @DisplayName("Builder should throw on missing eventType")
    void testBuilderMissingEventType() {
        assertThrows(IllegalStateException.class, () ->
            new ReplayEvent.Builder()
                .playerId(playerId)
                .build());
    }

    @Test
    @Order(4)
    @DisplayName("Builder should use defaults for optional fields")
    void testBuilderDefaults() {
        ReplayEvent event = new ReplayEvent.Builder()
            .playerId(playerId)
            .eventType(ReplayEvent.ReplayEventType.COMBAT_END)
            .build();

        assertEquals(playerId, event.getPlayerId());
        assertNull(event.getTargetId());
        assertEquals(ReplayEvent.ReplayEventType.COMBAT_END, event.getEventType());
        assertNotNull(event.getTimestamp());
        assertEquals(0.0, event.getDamage(), 0.001);
        assertFalse(event.isCritical());
        assertEquals("", event.getLocation());
        assertEquals("", event.getWeaponType());
        assertEquals("", event.getAdditionalData());
    }

    @Test
    @Order(5)
    @DisplayName("getMemoryFootprint should return positive value")
    void testMemoryFootprint() {
        ReplayEvent event = new ReplayEvent.Builder()
            .playerId(playerId)
            .eventType(ReplayEvent.ReplayEventType.HIT_LANDED)
            .build();
        assertTrue(event.getMemoryFootprint() > 0);
    }

    @Test
    @Order(6)
    @DisplayName("getMemoryFootprint should increase with more data")
    void testMemoryFootprintWithData() {
        ReplayEvent minimal = new ReplayEvent.Builder()
            .playerId(playerId)
            .eventType(ReplayEvent.ReplayEventType.HIT_LANDED)
            .build();

        ReplayEvent withData = new ReplayEvent.Builder()
            .playerId(playerId)
            .eventType(ReplayEvent.ReplayEventType.HIT_LANDED)
            .location("100,64,-200")
            .weaponType("DIAMOND_SWORD")
            .additionalData("combo:3")
            .build();

        assertTrue(withData.getMemoryFootprint() > minimal.getMemoryFootprint());
    }

    @Test
    @Order(7)
    @DisplayName("toString should format correctly")
    void testToString() {
        ReplayEvent event = new ReplayEvent.Builder()
            .playerId(playerId)
            .eventType(ReplayEvent.ReplayEventType.DAMAGE_DEALT)
            .damage(15.0)
            .critical(true)
            .build();

        String s = event.toString();
        assertTrue(s.contains(playerId.toString()));
        assertTrue(s.contains("DAMAGE_DEALT"));
        assertTrue(s.contains("15.0 dmg"));
        assertTrue(s.contains("(crit)"));
    }

    @Test
    @Order(8)
    @DisplayName("toString should handle non-critical events")
    void testToStringNonCritical() {
        ReplayEvent event = new ReplayEvent.Builder()
            .playerId(playerId)
            .eventType(ReplayEvent.ReplayEventType.HIT_MISSED)
            .damage(0.0)
            .critical(false)
            .build();

        String s = event.toString();
        assertTrue(s.contains("HIT_MISSED"));
        assertFalse(s.contains("(crit)"));
    }

    @Test
    @Order(9)
    @DisplayName("All ReplayEventType values should be usable")
    void testAllEventTypes() {
        for (ReplayEvent.ReplayEventType type : ReplayEvent.ReplayEventType.values()) {
            ReplayEvent event = new ReplayEvent.Builder()
                .playerId(playerId)
                .eventType(type)
                .build();
            assertEquals(type, event.getEventType());
        }
    }
}
