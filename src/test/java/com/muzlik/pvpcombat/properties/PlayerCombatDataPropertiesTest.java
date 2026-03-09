package com.muzlik.pvpcombat.properties;

import com.muzlik.pvpcombat.data.PlayerCombatData;
import net.jqwik.api.*;

import java.util.UUID;

/**
 * Property-based tests for PlayerCombatData.
 * Feature: truecombat-professional-upgrade
 * 
 * Tests universal properties for player combat statistics.
 */
public class PlayerCombatDataPropertiesTest {

    /**
     * Property 11: Damage Tracking Accuracy
     * For any player, total damage dealt should equal the sum of all recorded damage events.
     * 
     * Validates: Requirements 17.1, 17.2, 17.3
     */
    @Property
    @Label("Property 11: Damage tracking is accurate")
    void damageTrackingIsAccurate(
            @ForAll("damageValues") double[] damageValues) {
        
        PlayerCombatData data = new PlayerCombatData(UUID.randomUUID());
        
        double expectedTotal = 0.0;
        for (double damage : damageValues) {
            data.addDamageDealt(damage);
            expectedTotal += damage;
        }
        
        // Property: Total damage should equal sum of all damage events
        double actualTotal = data.getTotalDamageDealt();
        assert Math.abs(actualTotal - expectedTotal) < 0.001 : 
            "Total damage should be " + expectedTotal + " but was " + actualTotal;
    }

    /**
     * Property: Win/Loss Ratio Calculation
     * For any player, K/D ratio should be correctly calculated.
     */
    @Property
    @Label("Property: K/D ratio is correctly calculated")
    void kdRatioIsCorrect(
            @ForAll("winLossCount") int wins,
            @ForAll("winLossCount") int losses) {
        
        PlayerCombatData data = new PlayerCombatData(UUID.randomUUID());
        
        for (int i = 0; i < wins; i++) {
            data.incrementWins();
        }
        for (int i = 0; i < losses; i++) {
            data.incrementLosses();
        }
        
        double expectedKD = losses == 0 ? wins : (double) wins / losses;
        double actualKD = data.getKDRatio();
        
        // Property: K/D ratio should match expected calculation
        assert Math.abs(actualKD - expectedKD) < 0.001 : 
            "K/D ratio should be " + expectedKD + " but was " + actualKD;
    }

    /**
     * Property: Win Rate Calculation
     * For any player, win rate should be between 0 and 100 percent.
     */
    @Property
    @Label("Property: Win rate is within valid range")
    void winRateIsValid(
            @ForAll("winLossCount") int wins,
            @ForAll("winLossCount") int losses) {
        
        PlayerCombatData data = new PlayerCombatData(UUID.randomUUID());
        
        for (int i = 0; i < wins; i++) {
            data.incrementWins();
            data.incrementCombats();
        }
        for (int i = 0; i < losses; i++) {
            data.incrementLosses();
            data.incrementCombats();
        }
        
        double winRate = data.getWinRate();
        
        // Property: Win rate should be between 0 and 100
        assert winRate >= 0.0 && winRate <= 100.0 : 
            "Win rate should be between 0 and 100, but was " + winRate;
    }

    /**
     * Property: Combat Time Accumulation
     * For any player, total combat time should equal sum of all combat durations.
     */
    @Property
    @Label("Property: Combat time accumulates correctly")
    void combatTimeAccumulates(@ForAll("combatDurations") long[] durations) {
        PlayerCombatData data = new PlayerCombatData(UUID.randomUUID());
        
        long expectedTotal = 0;
        for (long duration : durations) {
            data.addCombatTime(duration);
            expectedTotal += duration;
        }
        
        // Property: Total combat time should equal sum of all durations
        assert data.getTotalCombatTime() == expectedTotal : 
            "Total combat time should be " + expectedTotal + " but was " + data.getTotalCombatTime();
    }

    /**
     * Property: Damage Ratio Calculation
     * For any player, damage ratio should be non-negative.
     */
    @Property
    @Label("Property: Damage ratio is non-negative")
    void damageRatioIsNonNegative(
            @ForAll("damageValues") double[] dealtDamage,
            @ForAll("damageValues") double[] receivedDamage) {
        
        PlayerCombatData data = new PlayerCombatData(UUID.randomUUID());
        
        for (double damage : dealtDamage) {
            data.addDamageDealt(damage);
        }
        for (double damage : receivedDamage) {
            data.addDamageReceived(damage);
        }
        
        double ratio = data.getDamageRatio();
        
        // Property: Damage ratio should always be non-negative
        assert ratio >= 0.0 : "Damage ratio should be non-negative, but was " + ratio;
    }

    /**
     * Provides arrays of valid damage values.
     */
    @Provide
    Arbitrary<double[]> damageValues() {
        return Arbitraries.doubles()
            .between(0.0, 100.0)
            .array(double[].class)
            .ofMinSize(0)
            .ofMaxSize(50);
    }

    /**
     * Provides arrays of valid combat durations in milliseconds.
     */
    @Provide
    Arbitrary<long[]> combatDurations() {
        return Arbitraries.longs()
            .between(0L, 300000L) // 0 to 5 minutes
            .array(long[].class)
            .ofMinSize(0)
            .ofMaxSize(20);
    }

    /**
     * Provides win/loss counts.
     */
    @Provide
    Arbitrary<Integer> winLossCount() {
        return Arbitraries.integers().between(0, 100);
    }
}
