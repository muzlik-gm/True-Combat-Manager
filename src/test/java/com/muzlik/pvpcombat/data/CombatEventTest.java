package com.muzlik.pvpcombat.data;

import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CombatEventTest {

    private UUID sessionId;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        playerId = UUID.randomUUID();
    }

    @Test
    @Order(1)
    @DisplayName("Concrete CombatEvent should initialize all fields")
    void testConstructor() {
        CombatEvent event = new CombatEvent(sessionId, playerId, "DAMAGE") {};
        assertEquals(sessionId, event.getSessionId());
        assertEquals(playerId, event.getPlayerId());
        assertEquals("DAMAGE", event.getEventType());
        assertTrue(event.getTimestamp() > 0);
    }

    @Test
    @Order(2)
    @DisplayName("getTimestamp should return current time at construction")
    void testTimestamp() {
        long before = System.currentTimeMillis();
        CombatEvent event = new CombatEvent(sessionId, playerId, "KILL") {};
        long after = System.currentTimeMillis();
        assertTrue(event.getTimestamp() >= before);
        assertTrue(event.getTimestamp() <= after);
    }

    @Test
    @Order(3)
    @DisplayName("Two events should have unique event types")
    void testDifferentEventTypes() {
        CombatEvent damage = new CombatEvent(sessionId, playerId, "DAMAGE") {};
        CombatEvent kill = new CombatEvent(sessionId, playerId, "KILL") {};
        assertEquals("DAMAGE", damage.getEventType());
        assertEquals("KILL", kill.getEventType());
    }

    @Test
    @Order(4)
    @DisplayName("Events with different sessions should have different session IDs")
    void testDifferentSessions() {
        UUID session2 = UUID.randomUUID();
        CombatEvent event1 = new CombatEvent(sessionId, playerId, "HIT") {};
        CombatEvent event2 = new CombatEvent(session2, playerId, "HIT") {};
        assertNotEquals(event1.getSessionId(), event2.getSessionId());
        assertEquals(event1.getPlayerId(), event2.getPlayerId());
    }
}
