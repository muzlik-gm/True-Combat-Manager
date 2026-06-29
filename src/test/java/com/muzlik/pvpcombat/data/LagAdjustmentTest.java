package com.muzlik.pvpcombat.data;

import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LagAdjustmentTest {

    private UUID sessionId;
    private LagAdjustment adjustment;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        adjustment = new LagAdjustment(sessionId);
    }

    @Test
    @Order(1)
    @DisplayName("Constructor should initialize correctly")
    void testInitialization() {
        assertEquals(sessionId, adjustment.getSessionId());
        assertEquals(0, adjustment.getExtraSecondsGranted());
        assertEquals(1.0, adjustment.getAdjustmentMultiplier(), 0.001);
        assertFalse(adjustment.isActive());
        assertEquals(0L, adjustment.getTotalAdjustedSeconds());
        assertEquals(0, adjustment.getAdjustmentCount());
        assertTrue(adjustment.getAdjustmentStartTime() > 0);
    }

    @Test
    @Order(2)
    @DisplayName("addExtraSeconds should accumulate")
    void testAddExtraSeconds() {
        adjustment.addExtraSeconds(5);
        assertEquals(5, adjustment.getExtraSecondsGranted());
        assertEquals(5L, adjustment.getTotalAdjustedSeconds());
        assertEquals(1, adjustment.getAdjustmentCount());

        adjustment.addExtraSeconds(3);
        assertEquals(8, adjustment.getExtraSecondsGranted());
        assertEquals(8L, adjustment.getTotalAdjustedSeconds());
        assertEquals(2, adjustment.getAdjustmentCount());
    }

    @Test
    @Order(3)
    @DisplayName("addExtraSeconds should update lastAdjustmentTime")
    void testAddExtraSecondsUpdatesTime() {
        long before = System.currentTimeMillis();
        adjustment.addExtraSeconds(1);
        assertTrue(adjustment.getLastAdjustmentTime() >= before);
    }

    @Test
    @Order(4)
    @DisplayName("setAdjustmentMultiplier should update multiplier")
    void testSetAdjustmentMultiplier() {
        adjustment.setAdjustmentMultiplier(1.5);
        assertEquals(1.5, adjustment.getAdjustmentMultiplier(), 0.001);
    }

    @Test
    @Order(5)
    @DisplayName("setActive should change active state")
    void testSetActive() {
        assertFalse(adjustment.isActive());
        adjustment.setActive(true);
        assertTrue(adjustment.isActive());
        adjustment.setActive(false);
        assertFalse(adjustment.isActive());
    }

    @Test
    @Order(6)
    @DisplayName("setActive(true) should update lastAdjustmentTime")
    void testSetActiveUpdatesTime() {
        long before = System.currentTimeMillis();
        adjustment.setActive(true);
        assertTrue(adjustment.getLastAdjustmentTime() >= before);
    }

    @Test
    @Order(7)
    @DisplayName("calculateExtension should return 0 when inactive")
    void testCalculateExtensionInactive() {
        int result = adjustment.calculateExtension(0.5, 10, 2.0);
        assertEquals(0, result);
        assertEquals(0, adjustment.getExtraSecondsGranted());
    }

    @Test
    @Order(8)
    @DisplayName("calculateExtension should return 0 when severity is zero")
    void testCalculateExtensionZeroSeverity() {
        adjustment.setActive(true);
        int result = adjustment.calculateExtension(0.0, 10, 2.0);
        assertEquals(0, result);
    }

    @Test
    @Order(9)
    @DisplayName("calculateExtension should return 0 when severity is negative")
    void testCalculateExtensionNegativeSeverity() {
        adjustment.setActive(true);
        int result = adjustment.calculateExtension(-0.5, 10, 2.0);
        assertEquals(0, result);
    }

    @Test
    @Order(10)
    @DisplayName("calculateExtension should compute and add extra seconds")
    void testCalculateExtensionActive() {
        adjustment.setActive(true);
        int result = adjustment.calculateExtension(0.5, 10, 2.0);
        assertEquals(10, result);
        assertEquals(10, adjustment.getExtraSecondsGranted());
        assertEquals(1, adjustment.getAdjustmentCount());
    }

    @Test
    @Order(11)
    @DisplayName("calculateExtension should handle fractional results with ceil")
    void testCalculateExtensionCeil() {
        adjustment.setActive(true);
        int result = adjustment.calculateExtension(0.3, 10, 1.0);
        assertEquals(3, result);
        assertEquals(3, adjustment.getExtraSecondsGranted());
    }

    @Test
    @Order(12)
    @DisplayName("reset should clear state but keep total stats")
    void testReset() {
        adjustment.setActive(true);
        adjustment.addExtraSeconds(10);
        adjustment.setAdjustmentMultiplier(2.0);

        adjustment.reset();

        assertEquals(0, adjustment.getExtraSecondsGranted());
        assertEquals(1.0, adjustment.getAdjustmentMultiplier(), 0.001);
        assertFalse(adjustment.isActive());
        assertEquals(0L, adjustment.getLastAdjustmentTime());
    }

    @Test
    @Order(13)
    @DisplayName("shouldCleanup should return false when active")
    void testShouldCleanupActive() {
        adjustment.setActive(true);
        assertFalse(adjustment.shouldCleanup(1000));
    }

    @Test
    @Order(14)
    @DisplayName("shouldCleanup should return true when inactive and past timeout")
    void testShouldCleanupInactive() throws InterruptedException {
        // When inactive and no recent activity, shouldCleanup depends on lastAdjustmentTime
        // lastAdjustmentTime starts at 0 when not yet activated
        assertTrue(adjustment.shouldCleanup(0));
    }

    @Test
    @Order(15)
    @DisplayName("getAdjustmentSummary should format correctly")
    void testGetAdjustmentSummary() {
        adjustment.setActive(true);
        adjustment.addExtraSeconds(5);
        String summary = adjustment.getAdjustmentSummary();
        assertTrue(summary.contains(sessionId.toString()));
        assertTrue(summary.contains("active=true"));
        assertTrue(summary.contains("extraSeconds=5"));
    }

    @Test
    @Order(16)
    @DisplayName("toString should format correctly")
    void testToString() {
        String s = adjustment.toString();
        assertTrue(s.contains(sessionId.toString()));
        assertTrue(s.contains("extraSeconds=0"));
        assertTrue(s.contains("active=false"));

        adjustment.addExtraSeconds(3);
        s = adjustment.toString();
        assertTrue(s.contains("extraSeconds=3"));
    }
}
