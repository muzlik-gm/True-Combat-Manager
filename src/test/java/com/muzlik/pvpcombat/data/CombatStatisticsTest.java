package com.muzlik.pvpcombat.data;

import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CombatStatisticsTest {

    private UUID sessionId;
    private CombatStatistics stats;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        stats = new CombatStatistics(sessionId);
    }

    @Test
    @Order(1)
    @DisplayName("Constructor should initialize all values to zero")
    void testInitialization() {
        assertEquals(sessionId, stats.getSessionId());
        assertEquals(0, stats.getTotalAttacks());
        assertEquals(0, stats.getHitsLanded());
        assertEquals(0L, stats.getTotalDamageDealt());
        assertEquals(0L, stats.getTotalDamageReceived());
        assertEquals(0, stats.getCriticalHits());
        assertEquals(0L, stats.getTotalDistance());
        assertEquals(0, stats.getKnockbackGiven());
        assertEquals(0, stats.getKnockbackReceived());
        assertEquals(0L, stats.getTotalMovementDistance());
        assertEquals(0L, stats.getTotalCombatTime());
        assertEquals(0, stats.getTimerResets());
        assertEquals(0, stats.getInterferenceAttempts());
        assertEquals(0, stats.getSuccessfulInterferences());
        assertEquals(0, stats.getRestrictionViolations());
        assertTrue(stats.getWeaponUsage().isEmpty());
        assertTrue(stats.getDamageByWeapon().isEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("recordAttack with hit should update all attack stats")
    void testRecordAttackHit() {
        stats.recordAttack(true, 10.0, 5.0, "DIAMOND_SWORD", true);
        assertEquals(1, stats.getTotalAttacks());
        assertEquals(1, stats.getHitsLanded());
        assertEquals(10L, stats.getTotalDamageDealt());
        assertEquals(1, stats.getCriticalHits());
        assertEquals(5L, stats.getTotalDistance());
    }

    @Test
    @Order(3)
    @DisplayName("recordAttack with miss should only increment total attacks")
    void testRecordAttackMiss() {
        stats.recordAttack(false, 0.0, 0.0, "FIST", false);
        assertEquals(1, stats.getTotalAttacks());
        assertEquals(0, stats.getHitsLanded());
        assertEquals(0L, stats.getTotalDamageDealt());
    }

    @Test
    @Order(4)
    @DisplayName("recordAttack should track weapon usage")
    void testWeaponUsage() {
        stats.recordAttack(true, 5.0, 2.0, "DIAMOND_SWORD", false);
        stats.recordAttack(true, 7.0, 3.0, "DIAMOND_SWORD", false);
        stats.recordAttack(true, 4.0, 1.0, "BOW", false);

        assertEquals(2, stats.getWeaponUsage().get("DIAMOND_SWORD").get());
        assertEquals(1, stats.getWeaponUsage().get("BOW").get());
        assertEquals(12L, stats.getDamageByWeapon().get("DIAMOND_SWORD").get());
        assertEquals(4L, stats.getDamageByWeapon().get("BOW").get());
    }

    @Test
    @Order(5)
    @DisplayName("recordDamageReceived should accumulate")
    void testRecordDamageReceived() {
        stats.recordDamageReceived(15.0);
        assertEquals(15L, stats.getTotalDamageReceived());
        stats.recordDamageReceived(5.5);
        assertEquals(20L, stats.getTotalDamageReceived());
    }

    @Test
    @Order(6)
    @DisplayName("recordKnockbackGiven should increment")
    void testRecordKnockbackGiven() {
        stats.recordKnockbackGiven();
        assertEquals(1, stats.getKnockbackGiven());
        stats.recordKnockbackGiven();
        assertEquals(2, stats.getKnockbackGiven());
    }

    @Test
    @Order(7)
    @DisplayName("recordKnockbackReceived should increment")
    void testRecordKnockbackReceived() {
        stats.recordKnockbackReceived();
        assertEquals(1, stats.getKnockbackReceived());
    }

    @Test
    @Order(8)
    @DisplayName("recordMovement should accumulate distance")
    void testRecordMovement() {
        stats.recordMovement(10.5);
        assertEquals(10L, stats.getTotalMovementDistance());
        stats.recordMovement(5.7);
        assertEquals(15L, stats.getTotalMovementDistance());
    }

    @Test
    @Order(9)
    @DisplayName("recordTimerReset should increment")
    void testRecordTimerReset() {
        stats.recordTimerReset();
        assertEquals(1, stats.getTimerResets());
        stats.recordTimerReset();
        assertEquals(2, stats.getTimerResets());
    }

    @Test
    @Order(10)
    @DisplayName("addCombatTime should accumulate seconds")
    void testAddCombatTime() {
        stats.addCombatTime(30L);
        assertEquals(30L, stats.getTotalCombatTime());
        stats.addCombatTime(45L);
        assertEquals(75L, stats.getTotalCombatTime());
    }

    @Test
    @Order(11)
    @DisplayName("recordInterferenceAttempt should track attempts and successes")
    void testRecordInterferenceAttempt() {
        stats.recordInterferenceAttempt(true);
        assertEquals(1, stats.getInterferenceAttempts());
        assertEquals(1, stats.getSuccessfulInterferences());

        stats.recordInterferenceAttempt(false);
        assertEquals(2, stats.getInterferenceAttempts());
        assertEquals(1, stats.getSuccessfulInterferences());
    }

    @Test
    @Order(12)
    @DisplayName("recordRestrictionViolation should increment")
    void testRecordRestrictionViolation() {
        stats.recordRestrictionViolation();
        assertEquals(1, stats.getRestrictionViolations());
    }

    @Test
    @Order(13)
    @DisplayName("getAccuracy should return 0 when no attacks")
    void testAccuracyZeroAttacks() {
        assertEquals(0.0, stats.getAccuracy(), 0.001);
    }

    @Test
    @Order(14)
    @DisplayName("getAccuracy should calculate percentage correctly")
    void testAccuracyCalculation() {
        stats.recordAttack(true, 5.0, 1.0, "SWORD", false);
        stats.recordAttack(true, 5.0, 1.0, "SWORD", false);
        stats.recordAttack(false, 0.0, 0.0, "SWORD", false);
        assertEquals(66.666, stats.getAccuracy(), 0.01);
    }

    @Test
    @Order(15)
    @DisplayName("getAverageDamagePerHit should return 0 when no hits")
    void testAverageDamageZeroHits() {
        assertEquals(0.0, stats.getAverageDamagePerHit(), 0.001);
    }

    @Test
    @Order(16)
    @DisplayName("getAverageDamagePerHit should calculate correctly")
    void testAverageDamageCalculation() {
        stats.recordAttack(true, 10.0, 2.0, "SWORD", false);
        stats.recordAttack(true, 20.0, 3.0, "SWORD", false);
        assertEquals(15.0, stats.getAverageDamagePerHit(), 0.001);
    }

    @Test
    @Order(17)
    @DisplayName("getAverageDistance should return 0 when no hits")
    void testAverageDistanceZeroHits() {
        assertEquals(0.0, stats.getAverageDistance(), 0.001);
    }

    @Test
    @Order(18)
    @DisplayName("getAverageDistance should calculate correctly")
    void testAverageDistanceCalculation() {
        stats.recordAttack(true, 5.0, 4.0, "BOW", false);
        stats.recordAttack(true, 5.0, 6.0, "BOW", false);
        assertEquals(5.0, stats.getAverageDistance(), 0.001);
    }

    @Test
    @Order(19)
    @DisplayName("getCriticalHitRate should return 0 when no hits")
    void testCriticalRateZeroHits() {
        assertEquals(0.0, stats.getCriticalHitRate(), 0.001);
    }

    @Test
    @Order(20)
    @DisplayName("getCriticalHitRate should calculate correctly")
    void testCriticalRateCalculation() {
        stats.recordAttack(true, 5.0, 1.0, "SWORD", true);
        stats.recordAttack(true, 5.0, 1.0, "SWORD", false);
        stats.recordAttack(true, 5.0, 1.0, "SWORD", true);
        assertEquals(66.666, stats.getCriticalHitRate(), 0.01);
    }

    @Test
    @Order(21)
    @DisplayName("increment should increase total attacks and weapon usage")
    void testIncrement() {
        stats.increment("BOW");
        assertEquals(1, stats.getTotalAttacks());
        assertEquals(1, stats.getWeaponUsage().get("BOW").get());
    }

    @Test
    @Order(22)
    @DisplayName("getStatisticsSummary should return formatted string")
    void testGetStatisticsSummary() {
        stats.recordAttack(true, 10.0, 5.0, "SWORD", true);
        stats.recordAttack(false, 0.0, 0.0, "SWORD", false);
        stats.recordDamageReceived(5.0);
        stats.recordKnockbackGiven();
        stats.recordMovement(20.0);

        String summary = stats.getStatisticsSummary();
        assertTrue(summary.contains(sessionId.toString()));
        assertTrue(summary.contains("Attacks: 2"));
        assertTrue(summary.contains("landed (50.0%)"));
        assertTrue(summary.contains("Damage: 10 dealt"));
        assertTrue(summary.contains("5 received"));
        assertTrue(summary.contains("Knockback: 1 given"));
        assertTrue(summary.contains("Movement: 20 blocks"));
    }

    @Test
    @Order(23)
    @DisplayName("getCombatDurationSeconds should return 0 when no hits recorded")
    void testCombatDurationNoHits() {
        assertEquals(0L, stats.getCombatDurationSeconds());
    }

    @Test
    @Order(24)
    @DisplayName("weaponUsage and damageByWeapon should return defensive copies")
    void testWeaponMapsAreDefensiveCopies() {
        stats.recordAttack(true, 10.0, 1.0, "SWORD", false);
        stats.getWeaponUsage().clear();
        assertEquals(1, stats.getWeaponUsage().get("SWORD").get());
    }
}
