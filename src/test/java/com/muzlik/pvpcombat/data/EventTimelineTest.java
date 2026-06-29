package com.muzlik.pvpcombat.data;

import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EventTimelineTest {

    private static final int CAPACITY = 10;
    private static final long MAX_AGE_SECONDS = 3600;

    private EventTimeline timeline;
    private UUID sessionId;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        timeline = new EventTimeline(CAPACITY, MAX_AGE_SECONDS);
        sessionId = UUID.randomUUID();
        playerId = UUID.randomUUID();
    }

    private ReplayEvent createEvent(ReplayEvent.ReplayEventType type) {
        return new ReplayEvent.Builder()
            .playerId(playerId)
            .eventType(type)
            .build();
    }

    @Test
    @Order(1)
    @DisplayName("Constructor should initialize empty timeline")
    void testInitialization() {
        Map<UUID, TimelineBuffer.BufferStats> stats = timeline.getSessionStats();
        assertTrue(stats.isEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("addEvent should create a buffer for the session")
    void testAddEvent() {
        timeline.addEvent(sessionId, createEvent(ReplayEvent.ReplayEventType.HIT_LANDED));
        assertTrue(timeline.hasReplayData(sessionId));
    }

    @Test
    @Order(3)
    @DisplayName("getEventsInWindow should return empty for unknown session")
    void testGetEventsInWindowUnknown() {
        LocalDateTime from = LocalDateTime.now().minusHours(1);
        ReplayEvent[] events = timeline.getEventsInWindow(UUID.randomUUID(), from);
        assertEquals(0, events.length);
    }

    @Test
    @Order(4)
    @DisplayName("getRecentEvents should return empty for unknown session")
    void testGetRecentEventsUnknown() {
        ReplayEvent[] events = timeline.getRecentEvents(UUID.randomUUID(), 10);
        assertEquals(0, events.length);
    }

    @Test
    @Order(5)
    @DisplayName("addEvent and getRecentEvents should work together")
    void testAddAndGetRecent() {
        timeline.addEvent(sessionId, createEvent(ReplayEvent.ReplayEventType.HIT_LANDED));
        timeline.addEvent(sessionId, createEvent(ReplayEvent.ReplayEventType.KNOCKBACK));

        ReplayEvent[] events = timeline.getRecentEvents(sessionId, 5);
        assertEquals(2, events.length);
    }

    @Test
    @Order(6)
    @DisplayName("getFullReplay should return events within max age")
    void testGetFullReplay() {
        timeline.addEvent(sessionId, createEvent(ReplayEvent.ReplayEventType.HIT_LANDED));
        ReplayEvent[] events = timeline.getFullReplay(sessionId);
        assertEquals(1, events.length);
    }

    @Test
    @Order(7)
    @DisplayName("getReplayData should return null for empty session")
    void testGetReplayDataEmpty() {
        assertNull(timeline.getReplayData(sessionId));
    }

    @Test
    @Order(8)
    @DisplayName("getReplayData should return ReplayData for session with events")
    void testGetReplayData() {
        timeline.addEvent(sessionId, createEvent(ReplayEvent.ReplayEventType.HIT_LANDED));
        ReplayData data = timeline.getReplayData(sessionId);
        assertNotNull(data);
        assertEquals(sessionId, data.getSessionId());
        assertEquals(1, data.getEventCount());
    }

    @Test
    @Order(9)
    @DisplayName("removeSession should clear and remove session")
    void testRemoveSession() {
        timeline.addEvent(sessionId, createEvent(ReplayEvent.ReplayEventType.HIT_LANDED));
        assertTrue(timeline.hasReplayData(sessionId));

        timeline.removeSession(sessionId);
        assertFalse(timeline.hasReplayData(sessionId));
    }

    @Test
    @Order(10)
    @DisplayName("clearAll should remove all sessions")
    void testClearAll() {
        UUID session2 = UUID.randomUUID();
        timeline.addEvent(sessionId, createEvent(ReplayEvent.ReplayEventType.HIT_LANDED));
        timeline.addEvent(session2, createEvent(ReplayEvent.ReplayEventType.MOVEMENT));
        assertEquals(2, timeline.getSessionStats().size());

        timeline.clearAll();
        assertEquals(0, timeline.getSessionStats().size());
        assertFalse(timeline.hasReplayData(sessionId));
        assertFalse(timeline.hasReplayData(session2));
    }

    @Test
    @Order(11)
    @DisplayName("hasReplayData should return false for unknown session")
    void testHasReplayDataUnknown() {
        assertFalse(timeline.hasReplayData(UUID.randomUUID()));
    }

    @Test
    @Order(12)
    @DisplayName("hasReplayData should return true for session with events")
    void testHasReplayDataTrue() {
        timeline.addEvent(sessionId, createEvent(ReplayEvent.ReplayEventType.HIT_LANDED));
        assertTrue(timeline.hasReplayData(sessionId));
    }

    @Test
    @Order(13)
    @DisplayName("getOverallStats should return aggregate stats")
    void testGetOverallStats() {
        EventTimeline.TimelineStats stats = timeline.getOverallStats();
        assertEquals(0, stats.activeSessions);
        assertEquals(0L, stats.totalEvents);
        assertTrue(stats.totalMemoryBytes >= 0);

        timeline.addEvent(sessionId, createEvent(ReplayEvent.ReplayEventType.HIT_LANDED));
        stats = timeline.getOverallStats();
        assertEquals(1, stats.activeSessions);
        assertEquals(1L, stats.totalEvents);
    }

    @Test
    @Order(14)
    @DisplayName("TimelineStats toString should format correctly")
    void testTimelineStatsToString() {
        timeline.addEvent(sessionId, createEvent(ReplayEvent.ReplayEventType.HIT_LANDED));
        EventTimeline.TimelineStats stats = timeline.getOverallStats();
        String s = stats.toString();
        assertTrue(s.contains("1 sessions"));
        assertTrue(s.contains("1 events"));
    }

    @Test
    @Order(15)
    @DisplayName("Multiple sessions should be tracked independently")
    void testMultipleSessions() {
        UUID session2 = UUID.randomUUID();
        timeline.addEvent(sessionId, createEvent(ReplayEvent.ReplayEventType.HIT_LANDED));
        timeline.addEvent(session2, createEvent(ReplayEvent.ReplayEventType.MOVEMENT));

        assertEquals(1, timeline.getRecentEvents(sessionId, 10).length);
        assertEquals(1, timeline.getRecentEvents(session2, 10).length);
        assertEquals(2, timeline.getSessionStats().size());
    }

    @Test
    @Order(16)
    @DisplayName("getSessionStats should return all sessions")
    void testGetSessionStats() {
        timeline.addEvent(sessionId, createEvent(ReplayEvent.ReplayEventType.HIT_LANDED));
        Map<UUID, TimelineBuffer.BufferStats> stats = timeline.getSessionStats();
        assertEquals(1, stats.size());
        assertTrue(stats.containsKey(sessionId));
    }
}
