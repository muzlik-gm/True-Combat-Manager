package com.muzlik.pvpcombat.data;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WeaponStatsTest {

    private WeaponStats stats;

    @BeforeEach
    void setUp() {
        stats = new WeaponStats("DIAMOND_SWORD");
    }

    @Test
    @Order(1)
    @DisplayName("Constructor should initialize with zero values")
    void testInitialization() {
        assertEquals("DIAMOND_SWORD", stats.getType());
        assertEquals(0, stats.getUses());
        assertEquals(0.0, stats.getTotalDamage(), 0.001);
        assertEquals(0, stats.getKills());
        assertEquals(0, stats.getCriticalHits());
    }

    @Test
    @Order(2)
    @DisplayName("recordHit should increment uses and damage")
    void testRecordHit() {
        stats.recordHit(10.0, false);
        assertEquals(1, stats.getUses());
        assertEquals(10.0, stats.getTotalDamage(), 0.001);
        assertEquals(0, stats.getCriticalHits());
    }

    @Test
    @Order(3)
    @DisplayName("recordHit with critical should increment critical hits")
    void testRecordHitCritical() {
        stats.recordHit(15.0, true);
        assertEquals(1, stats.getUses());
        assertEquals(15.0, stats.getTotalDamage(), 0.001);
        assertEquals(1, stats.getCriticalHits());
    }

    @Test
    @Order(4)
    @DisplayName("recordKill should increment kills")
    void testRecordKill() {
        stats.recordKill();
        assertEquals(1, stats.getKills());
        stats.recordKill();
        assertEquals(2, stats.getKills());
    }

    @Test
    @Order(5)
    @DisplayName("getAverageDamage should return 0 when no uses")
    void testAverageDamageNoUses() {
        assertEquals(0.0, stats.getAverageDamage(), 0.001);
    }

    @Test
    @Order(6)
    @DisplayName("getAverageDamage should calculate correctly")
    void testAverageDamage() {
        stats.recordHit(10.0, false);
        stats.recordHit(20.0, false);
        assertEquals(15.0, stats.getAverageDamage(), 0.001);
    }

    @Test
    @Order(7)
    @DisplayName("getCriticalRate should return 0 when no uses")
    void testCriticalRateNoUses() {
        assertEquals(0.0, stats.getCriticalRate(), 0.001);
    }

    @Test
    @Order(8)
    @DisplayName("getCriticalRate should calculate correctly")
    void testCriticalRate() {
        stats.recordHit(10.0, true);
        stats.recordHit(10.0, false);
        stats.recordHit(10.0, true);
        assertEquals(66.666, stats.getCriticalRate(), 0.01);
    }

    @Test
    @Order(9)
    @DisplayName("Setters should update values")
    void testSetters() {
        stats.setUses(10);
        stats.setTotalDamage(150.0);
        stats.setKills(5);
        stats.setCriticalHits(3);

        assertEquals(10, stats.getUses());
        assertEquals(150.0, stats.getTotalDamage(), 0.001);
        assertEquals(5, stats.getKills());
        assertEquals(3, stats.getCriticalHits());
    }

    @Test
    @Order(10)
    @DisplayName("toString should format correctly")
    void testToString() {
        stats.recordHit(15.5, true);
        stats.recordKill();
        String s = stats.toString();
        assertTrue(s.contains("DIAMOND_SWORD"));
        assertTrue(s.contains("uses=1"));
        assertTrue(s.contains("damage=15.50"));
        assertTrue(s.contains("kills=1"));
        assertTrue(s.contains("crits=1"));
    }

    @Test
    @Order(11)
    @DisplayName("Multiple hits should accumulate correctly")
    void testMultipleHits() {
        for (int i = 0; i < 10; i++) {
            stats.recordHit(5.0, i % 2 == 0);
        }
        assertEquals(10, stats.getUses());
        assertEquals(50.0, stats.getTotalDamage(), 0.001);
        assertEquals(5, stats.getCriticalHits());
    }
}
