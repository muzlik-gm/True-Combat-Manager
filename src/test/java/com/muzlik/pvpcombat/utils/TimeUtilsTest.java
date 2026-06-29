package com.muzlik.pvpcombat.utils;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TimeUtilsTest {

    @Test
    @Order(1)
    @DisplayName("secondsToTicks should convert seconds correctly")
    void testSecondsToTicks() {
        assertEquals(0L, TimeUtils.secondsToTicks(0));
        assertEquals(20L, TimeUtils.secondsToTicks(1));
        assertEquals(100L, TimeUtils.secondsToTicks(5));
        assertEquals(1200L, TimeUtils.secondsToTicks(60));
    }

    @Test
    @Order(2)
    @DisplayName("ticksToSeconds should convert ticks correctly")
    void testTicksToSeconds() {
        assertEquals(0L, TimeUtils.ticksToSeconds(0));
        assertEquals(1L, TimeUtils.ticksToSeconds(20));
        assertEquals(5L, TimeUtils.ticksToSeconds(100));
        assertEquals(60L, TimeUtils.ticksToSeconds(1200));
    }

    @Test
    @Order(3)
    @DisplayName("ticksToSeconds should perform integer division")
    void testTicksToSecondsIntegerDivision() {
        assertEquals(0L, TimeUtils.ticksToSeconds(19));
        assertEquals(0L, TimeUtils.ticksToSeconds(1));
        assertEquals(3L, TimeUtils.ticksToSeconds(79));
    }

    @Test
    @Order(4)
    @DisplayName("formatTime should format MM:SS correctly")
    void testFormatTime() {
        assertEquals("00:00", TimeUtils.formatTime(0));
        assertEquals("00:59", TimeUtils.formatTime(59));
        assertEquals("01:00", TimeUtils.formatTime(60));
        assertEquals("01:30", TimeUtils.formatTime(90));
        assertEquals("10:00", TimeUtils.formatTime(600));
        assertEquals("61:01", TimeUtils.formatTime(3661));
    }

    @Test
    @Order(5)
    @DisplayName("currentTimeMillis should return current system time")
    void testCurrentTimeMillis() {
        long before = System.currentTimeMillis();
        long result = TimeUtils.currentTimeMillis();
        long after = System.currentTimeMillis();
        assertTrue(result >= before && result <= after);
    }

    @Test
    @Order(6)
    @DisplayName("isExpired should return true when duration has passed")
    void testIsExpiredTrue() {
        long past = System.currentTimeMillis() - 2000;
        assertTrue(TimeUtils.isExpired(past, 500));
    }

    @Test
    @Order(7)
    @DisplayName("isExpired should return false when duration has not passed")
    void testIsExpiredFalse() {
        long now = System.currentTimeMillis();
        assertFalse(TimeUtils.isExpired(now, 100000));
    }

    @Test
    @Order(8)
    @DisplayName("isExpired should return true for exactly expired timestamps")
    void testIsExpiredExactly() {
        long past = System.currentTimeMillis() - 500;
        assertTrue(TimeUtils.isExpired(past, 500));
    }
}
