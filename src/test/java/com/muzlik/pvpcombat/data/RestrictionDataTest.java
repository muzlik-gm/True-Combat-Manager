package com.muzlik.pvpcombat.data;

import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RestrictionDataTest {

    private UUID playerId;
    private RestrictionData restriction;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        restriction = new RestrictionData(playerId);
    }

    @Test
    @Order(1)
    @DisplayName("Constructor should initialize correctly")
    void testInitialization() {
        assertEquals(playerId, restriction.getPlayerId());
        assertFalse(restriction.isElytraGlideBlocked());
        assertTrue(restriction.getActiveCooldowns().isEmpty());
        assertNull(restriction.getLastEnderPearlUse());
        assertNull(restriction.getLastElytraBoost());
        assertNull(restriction.getLastGlideStart());
        assertNull(restriction.getLastGoldenAppleUse());
        assertNull(restriction.getLastEnchantedGoldenAppleUse());
        assertEquals(0.0, restriction.getAltitudeAtGlideStart(), 0.001);
    }

    @Test
    @Order(2)
    @DisplayName("setCooldown should register active cooldown")
    void testSetCooldown() {
        restriction.setCooldown("ENDER_PEARL", 5);
        assertTrue(restriction.isOnCooldown("ENDER_PEARL"));
    }

    @Test
    @Order(3)
    @DisplayName("isOnCooldown should return false for unknown type")
    void testIsOnCooldownUnknown() {
        assertFalse(restriction.isOnCooldown("NONEXISTENT"));
    }

    @Test
    @Order(4)
    @DisplayName("removeCooldown should clear cooldown")
    void testRemoveCooldown() {
        restriction.setCooldown("GOLDEN_APPLE", 30);
        assertTrue(restriction.isOnCooldown("GOLDEN_APPLE"));
        restriction.removeCooldown("GOLDEN_APPLE");
        assertFalse(restriction.isOnCooldown("GOLDEN_APPLE"));
    }

    @Test
    @Order(5)
    @DisplayName("setElytraGlideBlocked should toggle")
    void testElytraGlideBlocked() {
        assertFalse(restriction.isElytraGlideBlocked());
        restriction.setElytraGlideBlocked(true);
        assertTrue(restriction.isElytraGlideBlocked());
        restriction.setElytraGlideBlocked(false);
        assertFalse(restriction.isElytraGlideBlocked());
    }

    @Test
    @Order(6)
    @DisplayName("setLastEnderPearlUse should store timestamp")
    void testLastEnderPearlUse() {
        LocalDateTime now = LocalDateTime.now();
        restriction.setLastEnderPearlUse(now);
        assertEquals(now, restriction.getLastEnderPearlUse());
    }

    @Test
    @Order(7)
    @DisplayName("setLastElytraBoost should store timestamp")
    void testLastElytraBoost() {
        LocalDateTime now = LocalDateTime.now();
        restriction.setLastElytraBoost(now);
        assertEquals(now, restriction.getLastElytraBoost());
    }

    @Test
    @Order(8)
    @DisplayName("setLastGlideStart should store timestamp")
    void testLastGlideStart() {
        LocalDateTime now = LocalDateTime.now();
        restriction.setLastGlideStart(now);
        assertEquals(now, restriction.getLastGlideStart());
    }

    @Test
    @Order(9)
    @DisplayName("setAltitudeAtGlideStart should store value")
    void testAltitudeAtGlideStart() {
        restriction.setAltitudeAtGlideStart(120.5);
        assertEquals(120.5, restriction.getAltitudeAtGlideStart(), 0.001);
    }

    @Test
    @Order(10)
    @DisplayName("setLastGoldenAppleUse should store timestamp")
    void testLastGoldenAppleUse() {
        LocalDateTime now = LocalDateTime.now();
        restriction.setLastGoldenAppleUse(now);
        assertEquals(now, restriction.getLastGoldenAppleUse());
    }

    @Test
    @Order(11)
    @DisplayName("setLastEnchantedGoldenAppleUse should store timestamp")
    void testLastEnchantedGoldenAppleUse() {
        LocalDateTime now = LocalDateTime.now();
        restriction.setLastEnchantedGoldenAppleUse(now);
        assertEquals(now, restriction.getLastEnchantedGoldenAppleUse());
    }

    @Test
    @Order(12)
    @DisplayName("clearAllRestrictions should reset all fields")
    void testClearAllRestrictions() {
        restriction.setCooldown("ENDER_PEARL", 30);
        restriction.setElytraGlideBlocked(true);
        restriction.setLastEnderPearlUse(LocalDateTime.now());
        restriction.setLastElytraBoost(LocalDateTime.now());
        restriction.setLastGlideStart(LocalDateTime.now());
        restriction.setLastGoldenAppleUse(LocalDateTime.now());
        restriction.setLastEnchantedGoldenAppleUse(LocalDateTime.now());
        restriction.setAltitudeAtGlideStart(45.0);

        restriction.clearAllRestrictions();

        assertTrue(restriction.getActiveCooldowns().isEmpty());
        assertFalse(restriction.isElytraGlideBlocked());
        assertNull(restriction.getLastEnderPearlUse());
        assertNull(restriction.getLastElytraBoost());
        assertNull(restriction.getLastGlideStart());
        assertNull(restriction.getLastGoldenAppleUse());
        assertNull(restriction.getLastEnchantedGoldenAppleUse());
        assertEquals(0.0, restriction.getAltitudeAtGlideStart(), 0.001);
    }

    @Test
    @Order(13)
    @DisplayName("Multiple cooldowns should be tracked independently")
    void testMultipleCooldowns() {
        restriction.setCooldown("ENDER_PEARL", 5);
        restriction.setCooldown("GOLDEN_APPLE", 10);
        assertTrue(restriction.isOnCooldown("ENDER_PEARL"));
        assertTrue(restriction.isOnCooldown("GOLDEN_APPLE"));
        restriction.removeCooldown("ENDER_PEARL");
        assertFalse(restriction.isOnCooldown("ENDER_PEARL"));
        assertTrue(restriction.isOnCooldown("GOLDEN_APPLE"));
    }

    @Test
    @Order(14)
    @DisplayName("getActiveCooldowns returns mutable map")
    void testActiveCooldownsMap() {
        restriction.setCooldown("TEST", 10);
        assertEquals(1, restriction.getActiveCooldowns().size());
    }
}
