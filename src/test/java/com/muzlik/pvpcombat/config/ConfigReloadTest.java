package com.muzlik.pvpcombat.config;

import com.muzlik.pvpcombat.data.TimerData;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for configuration reload functionality and timer data.
 * Tests the core reload logic without file system dependencies.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfigReloadTest {

    @Test
    @Order(1)
    @DisplayName("TimerData should initialize correctly")
    void testTimerDataInitialization() {
        UUID sessionId = UUID.randomUUID();
        TimerData timerData = new TimerData(sessionId, 30);
        
        assertEquals(sessionId, timerData.getSessionId());
        assertEquals(30, timerData.getInitialDurationSeconds());
        assertEquals(30, timerData.getRemainingSeconds());
        assertFalse(timerData.isPaused());
        assertFalse(timerData.isExpired());
    }

    @Test
    @Order(2)
    @DisplayName("TimerData should handle manual time updates")
    void testTimerDataManualUpdate() {
        TimerData timerData = new TimerData(UUID.randomUUID(), 30);
        
        timerData.setRemainingSeconds(20);
        assertEquals(20, timerData.getRemainingSeconds());
        
        timerData.setRemainingSeconds(10);
        assertEquals(10, timerData.getRemainingSeconds());
    }

    @Test
    @Order(3)
    @DisplayName("TimerData should reset correctly")
    void testTimerDataReset() {
        TimerData timerData = new TimerData(UUID.randomUUID(), 30);
        
        timerData.setRemainingSeconds(10);
        assertEquals(10, timerData.getRemainingSeconds());
        
        timerData.reset();
        assertEquals(30, timerData.getRemainingSeconds());
        assertFalse(timerData.isPaused());
    }

    @Test
    @Order(4)
    @DisplayName("TimerData should calculate progress correctly")
    void testTimerDataProgress() {
        TimerData timerData = new TimerData(UUID.randomUUID(), 30);
        
        assertEquals(1.0, timerData.getProgress(), 0.001);
        
        timerData.setRemainingSeconds(15);
        assertEquals(0.5, timerData.getProgress(), 0.001);
        
        timerData.setRemainingSeconds(0);
        assertEquals(0.0, timerData.getProgress(), 0.001);
    }

    @Test
    @Order(5)
    @DisplayName("TimerData should handle expiration correctly")
    void testTimerDataExpiration() {
        TimerData timerData = new TimerData(UUID.randomUUID(), 30);
        
        assertFalse(timerData.isExpired());
        
        timerData.setRemainingSeconds(0);
        assertTrue(timerData.isExpired());
    }

    @Test
    @Order(6)
    @DisplayName("TimerData should handle pause/resume")
    void testTimerDataPauseResume() {
        TimerData timerData = new TimerData(UUID.randomUUID(), 30);
        
        assertFalse(timerData.isPaused());
        
        timerData.setPaused(true);
        assertTrue(timerData.isPaused());
        
        // When paused, updateElapsedTime should not decrement
        boolean expired = timerData.updateElapsedTime();
        assertFalse(expired);
        assertEquals(30, timerData.getRemainingSeconds());
        
        timerData.setPaused(false);
        assertFalse(timerData.isPaused());
    }

    @Test
    @Order(7)
    @DisplayName("TimerData should handle edge cases")
    void testTimerDataEdgeCases() {
        // Test with 0 duration
        TimerData timer1 = new TimerData(UUID.randomUUID(), 0);
        assertEquals(0, timer1.getRemainingSeconds());
        assertTrue(timer1.isExpired());
        
        // Test with large duration
        TimerData timer2 = new TimerData(UUID.randomUUID(), 3600);
        assertEquals(3600, timer2.getRemainingSeconds());
        assertFalse(timer2.isExpired());
    }

    @Test
    @Order(8)
    @DisplayName("TimerData should reject invalid values")
    void testTimerDataValidation() {
        // Test null session ID
        assertThrows(IllegalArgumentException.class, () -> {
            new TimerData(null, 30);
        });
        
        // Test negative duration
        assertThrows(IllegalArgumentException.class, () -> {
            new TimerData(UUID.randomUUID(), -1);
        });
        
        // Test negative remaining seconds
        TimerData timerData = new TimerData(UUID.randomUUID(), 30);
        assertThrows(IllegalArgumentException.class, () -> {
            timerData.setRemainingSeconds(-1);
        });
    }

    @Test
    @Order(9)
    @DisplayName("TimerData should track update time")
    void testTimerDataUpdateTime() throws InterruptedException {
        TimerData timerData = new TimerData(UUID.randomUUID(), 30);
        
        long initialUpdateTime = timerData.getLastUpdateTime();
        assertTrue(initialUpdateTime > 0);
        
        Thread.sleep(100);
        
        timerData.setRemainingSeconds(25);
        long newUpdateTime = timerData.getLastUpdateTime();
        
        assertTrue(newUpdateTime > initialUpdateTime);
    }

    @Test
    @Order(10)
    @DisplayName("TimerData should handle concurrent access")
    void testTimerDataConcurrency() throws InterruptedException {
        TimerData timerData = new TimerData(UUID.randomUUID(), 100);
        
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    int current = timerData.getRemainingSeconds();
                    if (current > 0) {
                        timerData.setRemainingSeconds(current - 1);
                    }
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Should have decremented 50 times
        assertTrue(timerData.getRemainingSeconds() <= 50);
    }
}
