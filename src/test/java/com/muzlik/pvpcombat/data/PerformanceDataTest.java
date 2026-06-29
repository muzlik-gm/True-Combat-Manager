package com.muzlik.pvpcombat.data;

import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PerformanceDataTest {

    private UUID playerId;
    private PerformanceData perf;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        perf = new PerformanceData(playerId);
    }

    @Test
    @Order(1)
    @DisplayName("Constructor should initialize with defaults")
    void testInitialization() {
        assertEquals(playerId, perf.getPlayerId());
        assertEquals(20.0, perf.getCurrentTps(), 0.001);
        assertEquals(20.0, perf.getAverageTps(), 0.001);
        assertEquals(0, perf.getCurrentPing());
        assertEquals(0, perf.getAveragePing());
        assertFalse(perf.isExperiencingLag());
        assertEquals(0L, perf.getLastLagSpikeTime());
    }

    @Test
    @Order(2)
    @DisplayName("setCurrentTps should update current TPS and average")
    void testSetCurrentTps() {
        perf.setCurrentTps(18.5);
        assertEquals(18.5, perf.getCurrentTps(), 0.001);
        assertEquals(19.85, perf.getAverageTps(), 0.001);
    }

    @Test
    @Order(3)
    @DisplayName("setCurrentPing should update current ping and average")
    void testSetCurrentPing() {
        perf.setCurrentPing(50);
        assertEquals(50, perf.getCurrentPing());
        assertEquals(10, perf.getAveragePing());
    }

    @Test
    @Order(4)
    @DisplayName("setExperiencingLag should set flag and update time")
    void testSetExperiencingLag() {
        assertFalse(perf.isExperiencingLag());
        assertEquals(0L, perf.getLastLagSpikeTime());

        perf.setExperiencingLag(true);
        assertTrue(perf.isExperiencingLag());
        assertTrue(perf.getLastLagSpikeTime() > 0);

        perf.setExperiencingLag(false);
        assertFalse(perf.isExperiencingLag());
    }

    @Test
    @Order(5)
    @DisplayName("isLagging should detect low TPS")
    void testIsLaggingLowTps() {
        perf.setCurrentTps(15.0);
        assertTrue(perf.isLagging(18.0, 100));
    }

    @Test
    @Order(6)
    @DisplayName("isLagging should detect high ping")
    void testIsLaggingHighPing() {
        perf.setCurrentPing(200);
        assertTrue(perf.isLagging(18.0, 100));
    }

    @Test
    @Order(7)
    @DisplayName("isLagging should return false when values are normal")
    void testIsLaggingNormal() {
        perf.setCurrentTps(19.5);
        perf.setCurrentPing(30);
        assertFalse(perf.isLagging(18.0, 100));
    }

    @Test
    @Order(8)
    @DisplayName("getLagSeverity should return 0 for normal conditions")
    void testLagSeverityNormal() {
        perf.setCurrentTps(20.0);
        perf.setCurrentPing(0);
        double severity = perf.getLagSeverity(18.0, 100);
        assertEquals(0.0, severity, 0.001);
    }

    @Test
    @Order(9)
    @DisplayName("getLagSeverity should calculate severity for low TPS")
    void testLagSeverityLowTps() {
        perf.setCurrentTps(9.0);
        perf.setCurrentPing(0);
        double severity = perf.getLagSeverity(18.0, 100);
        assertEquals(0.25, severity, 0.001);
    }

    @Test
    @Order(10)
    @DisplayName("getLagSeverity should not exceed 1.0")
    void testLagSeverityMax() {
        perf.setCurrentTps(0.0);
        perf.setCurrentPing(1000);
        double severity = perf.getLagSeverity(18.0, 100);
        assertEquals(1.0, severity, 0.001);
    }

    @Test
    @Order(11)
    @DisplayName("getTimeSinceLastLagSpike should return large value when no spike (lastLagSpikeTime=0)")
    void testTimeSinceLastLagSpikeNone() {
        assertTrue(perf.getTimeSinceLastLagSpike() > 0);
    }

    @Test
    @Order(12)
    @DisplayName("getTimeSinceLastLagSpike should return positive after spike")
    void testTimeSinceLastLagSpikeAfter() {
        perf.setExperiencingLag(true);
        assertTrue(perf.getTimeSinceLastLagSpike() >= 0);
    }

    @Test
    @Order(13)
    @DisplayName("toString should format correctly")
    void testToString() {
        perf.setCurrentTps(19.5);
        perf.setCurrentPing(25);
        String s = perf.toString();
        assertTrue(s.contains(playerId.toString()));
        assertTrue(s.contains("tps=19.50"));
        assertTrue(s.contains("ping=25"));
    }

    @Test
    @Order(14)
    @DisplayName("Multiple TPS updates should produce moving average")
    void testTpsMovingAverage() {
        perf.setCurrentTps(20.0);
        perf.setCurrentTps(20.0);
        perf.setCurrentTps(20.0);
        perf.setCurrentTps(10.0);
        // After many 20.0 updates, average settles near 20
        // Then after one 10.0: avg = (20.0 * 0.9) + (10.0 * 0.1) = 19.0
        assertEquals(19.0, perf.getAverageTps(), 0.001);
    }
}
