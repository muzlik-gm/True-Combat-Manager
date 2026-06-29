package com.muzlik.pvpcombat.data;

import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TimerDataTest {

    private UUID sessionId;
    private TimerData timer;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        timer = new TimerData(sessionId, 30);
    }

    @Test
    @Order(1)
    @DisplayName("Constructor should initialize correctly")
    void testInitialization() {
        assertEquals(sessionId, timer.getSessionId());
        assertEquals(30, timer.getRemainingSeconds());
        assertEquals(30, timer.getInitialDurationSeconds());
        assertFalse(timer.isPaused());
        assertFalse(timer.isExpired());
        assertTrue(timer.getStartTime() > 0);
    }

    @Test
    @Order(2)
    @DisplayName("Constructor should throw on null UUID")
    void testConstructorNullUUID() {
        assertThrows(IllegalArgumentException.class, () -> new TimerData(null, 30));
    }

    @Test
    @Order(3)
    @DisplayName("Constructor should throw on negative duration")
    void testConstructorNegativeDuration() {
        assertThrows(IllegalArgumentException.class, () -> new TimerData(sessionId, -1));
    }

    @Test
    @Order(4)
    @DisplayName("Constructor should accept zero duration")
    void testConstructorZeroDuration() {
        TimerData zeroTimer = new TimerData(sessionId, 0);
        assertEquals(0, zeroTimer.getRemainingSeconds());
        assertTrue(zeroTimer.isExpired());
    }

    @Test
    @Order(5)
    @DisplayName("setRemainingSeconds should update time and lastUpdateTime")
    void testSetRemainingSeconds() {
        timer.setRemainingSeconds(15);
        assertEquals(15, timer.getRemainingSeconds());
        assertTrue(timer.getLastUpdateTime() >= timer.getStartTime());
    }

    @Test
    @Order(6)
    @DisplayName("setRemainingSeconds should throw on negative")
    void testSetRemainingSecondsNegative() {
        assertThrows(IllegalArgumentException.class, () -> timer.setRemainingSeconds(-5));
    }

    @Test
    @Order(7)
    @DisplayName("setRemainingSeconds should accept zero")
    void testSetRemainingSecondsZero() {
        timer.setRemainingSeconds(0);
        assertEquals(0, timer.getRemainingSeconds());
        assertTrue(timer.isExpired());
    }

    @Test
    @Order(8)
    @DisplayName("pause and resume should work")
    void testPauseResume() {
        assertFalse(timer.isPaused());
        timer.setPaused(true);
        assertTrue(timer.isPaused());
        timer.setPaused(false);
        assertFalse(timer.isPaused());
    }

    @Test
    @Order(9)
    @DisplayName("reset should restore initial duration and unpause")
    void testReset() {
        timer.setRemainingSeconds(10);
        timer.setPaused(true);
        timer.reset();
        assertEquals(30, timer.getRemainingSeconds());
        assertFalse(timer.isPaused());
    }

    @Test
    @Order(10)
    @DisplayName("updateElapsedTime should not decrement when paused")
    void testUpdateElapsedTimePaused() {
        timer.setPaused(true);
        assertFalse(timer.updateElapsedTime());
        assertEquals(30, timer.getRemainingSeconds());
    }

    @Test
    @Order(11)
    @DisplayName("isExpired should return true when remaining is zero")
    void testIsExpired() {
        assertFalse(timer.isExpired());
        timer.setRemainingSeconds(0);
        assertTrue(timer.isExpired());
    }

    @Test
    @Order(12)
    @DisplayName("getProgress should return correct ratio")
    void testGetProgress() {
        assertEquals(1.0, timer.getProgress(), 0.001);
        timer.setRemainingSeconds(15);
        assertEquals(0.5, timer.getProgress(), 0.001);
        timer.setRemainingSeconds(0);
        assertEquals(0.0, timer.getProgress(), 0.001);
    }

    @Test
    @Order(13)
    @DisplayName("updateElapsedTime should decrement correctly over time")
    void testUpdateElapsedTimeDecrement() throws InterruptedException {
        Thread.sleep(1100);
        boolean expired = timer.updateElapsedTime();
        assertEquals(28, timer.getRemainingSeconds(), 1);
        assertFalse(expired);
    }

    @Test
    @Order(14)
    @DisplayName("getLastUpdateTime should be set initially")
    void testLastUpdateTime() {
        assertTrue(timer.getLastUpdateTime() >= timer.getStartTime());
        assertTrue(timer.getLastUpdateTime() <= System.currentTimeMillis());
    }
}
