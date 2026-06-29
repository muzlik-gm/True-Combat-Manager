package com.muzlik.pvpcombat.data;

import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TimelineBufferTest {

    private static final int CAPACITY = 5;
    private static final long MAX_AGE_SECONDS = 3600;

    private TimelineBuffer buffer;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        buffer = new TimelineBuffer(CAPACITY, MAX_AGE_SECONDS);
    }

    private ReplayEvent createEvent(ReplayEvent.ReplayEventType type) {
        return new ReplayEvent.Builder()
            .playerId(playerId)
            .eventType(type)
            .build();
    }

    @Test
    @Order(1)
    @DisplayName("Constructor should throw on non-positive capacity")
    void testConstructorInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new TimelineBuffer(0, 100));
        assertThrows(IllegalArgumentException.class, () -> new TimelineBuffer(-1, 100));
    }

    @Test
    @Order(2)
    @DisplayName("Constructor should throw on non-positive max age")
    void testConstructorInvalidMaxAge() {
        assertThrows(IllegalArgumentException.class, () -> new TimelineBuffer(10, 0));
        assertThrows(IllegalArgumentException.class, () -> new TimelineBuffer(10, -1));
    }

    @Test
    @Order(3)
    @DisplayName("Empty buffer should return empty arrays")
    void testEmptyBuffer() {
        assertEquals(0, buffer.getRecentEvents(10).length);
        assertEquals(0, buffer.getEventsInWindow(LocalDateTime.now().minusHours(1)).length);
    }

    @Test
    @Order(4)
    @DisplayName("addEvent and getRecentEvents should work")
    void testAddAndGetRecent() {
        ReplayEvent e1 = createEvent(ReplayEvent.ReplayEventType.HIT_LANDED);
        ReplayEvent e2 = createEvent(ReplayEvent.ReplayEventType.HIT_MISSED);
        buffer.addEvent(e1);
        buffer.addEvent(e2);

        ReplayEvent[] recent = buffer.getRecentEvents(10);
        assertEquals(2, recent.length);
        assertEquals(ReplayEvent.ReplayEventType.HIT_MISSED, recent[0].getEventType());
        assertEquals(ReplayEvent.ReplayEventType.HIT_LANDED, recent[1].getEventType());
    }

    @Test
    @Order(5)
    @DisplayName("getRecentEvents should respect limit")
    void testGetRecentLimited() {
        for (int i = 0; i < 5; i++) {
            buffer.addEvent(createEvent(ReplayEvent.ReplayEventType.MOVEMENT));
        }

        ReplayEvent[] recent = buffer.getRecentEvents(3);
        assertEquals(3, recent.length);
    }

    @Test
    @Order(6)
    @DisplayName("Buffer should wrap around when full")
    void testBufferWrapAround() {
        for (int i = 0; i < CAPACITY; i++) {
            buffer.addEvent(createEvent(ReplayEvent.ReplayEventType.MOVEMENT));
        }

        ReplayEvent[] beforeWrap = buffer.getRecentEvents(CAPACITY);
        assertEquals(CAPACITY, beforeWrap.length);

        buffer.addEvent(createEvent(ReplayEvent.ReplayEventType.HIT_LANDED));
        ReplayEvent[] afterWrap = buffer.getRecentEvents(CAPACITY);
        assertEquals(CAPACITY, afterWrap.length);
        assertEquals(ReplayEvent.ReplayEventType.HIT_LANDED, afterWrap[0].getEventType());
    }

    @Test
    @Order(7)
    @DisplayName("getEventsInWindow should return events after fromTime")
    void testGetEventsInWindow() {
        ReplayEvent e1 = createEvent(ReplayEvent.ReplayEventType.HIT_LANDED);
        buffer.addEvent(e1);

        LocalDateTime past = LocalDateTime.now().minusSeconds(10);
        ReplayEvent[] events = buffer.getEventsInWindow(past);
        assertEquals(1, events.length);

        LocalDateTime future = LocalDateTime.now().plusSeconds(10);
        ReplayEvent[] noEvents = buffer.getEventsInWindow(future);
        assertEquals(0, noEvents.length);
    }

    @Test
    @Order(8)
    @DisplayName("clear should remove all events")
    void testClear() {
        buffer.addEvent(createEvent(ReplayEvent.ReplayEventType.HIT_LANDED));
        buffer.addEvent(createEvent(ReplayEvent.ReplayEventType.HIT_MISSED));
        assertEquals(2, buffer.getRecentEvents(10).length);

        buffer.clear();
        assertEquals(0, buffer.getRecentEvents(10).length);
        assertEquals(0, buffer.getRecentEvents(10).length);
    }

    @Test
    @Order(9)
    @DisplayName("getStats should return correct buffer stats")
    void testGetStats() {
        TimelineBuffer.BufferStats stats = buffer.getStats();
        assertEquals(0, stats.currentSize);
        assertEquals(CAPACITY, stats.capacity);
        assertEquals(0L, stats.totalEvents);
        assertTrue(stats.memoryUsageBytes >= 0);

        buffer.addEvent(createEvent(ReplayEvent.ReplayEventType.HIT_LANDED));
        stats = buffer.getStats();
        assertEquals(1, stats.currentSize);
        assertEquals(1L, stats.totalEvents);
    }

    @Test
    @Order(10)
    @DisplayName("BufferStats toString should format correctly")
    void testBufferStatsToString() {
        buffer.addEvent(createEvent(ReplayEvent.ReplayEventType.HIT_LANDED));
        TimelineBuffer.BufferStats stats = buffer.getStats();
        String s = stats.toString();
        assertTrue(s.contains("1/5 events"));
        assertTrue(s.contains("1 total"));
    }

    @Test
    @Order(11)
    @DisplayName("getRecentEvents should handle limit larger than size")
    void testGetRecentLimitGreaterThanSize() {
        buffer.addEvent(createEvent(ReplayEvent.ReplayEventType.HIT_LANDED));
        ReplayEvent[] events = buffer.getRecentEvents(100);
        assertEquals(1, events.length);
    }

    @Test
    @Order(12)
    @DisplayName("Multiple addEvent calls should maintain order")
    void testEventOrder() {
        ReplayEvent e1 = createEvent(ReplayEvent.ReplayEventType.HIT_LANDED);
        ReplayEvent e2 = createEvent(ReplayEvent.ReplayEventType.KNOCKBACK);
        ReplayEvent e3 = createEvent(ReplayEvent.ReplayEventType.COMBAT_END);

        buffer.addEvent(e1);
        buffer.addEvent(e2);
        buffer.addEvent(e3);

        ReplayEvent[] recent = buffer.getRecentEvents(3);
        assertEquals(ReplayEvent.ReplayEventType.COMBAT_END, recent[0].getEventType());
        assertEquals(ReplayEvent.ReplayEventType.KNOCKBACK, recent[1].getEventType());
        assertEquals(ReplayEvent.ReplayEventType.HIT_LANDED, recent[2].getEventType());
    }
}
