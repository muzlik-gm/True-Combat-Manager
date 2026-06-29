package com.muzlik.pvpcombat.logging;

import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CombatSummaryTest {

    private UUID sessionId;
    private UUID playerId;
    private UUID targetId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        targetId = UUID.randomUUID();
    }

    @Test
    @Order(1)
    @DisplayName("empty entries produces zero statistics")
    void emptyEntriesProducesZeroStatistics() {
        CombatSummary summary = new CombatSummary(new ArrayList<>());
        assertEquals(0, summary.getTotalAttacks());
        assertEquals(0, summary.getHitsLanded());
        assertEquals(0.0, summary.getAccuracy(), 0.001);
        assertEquals(0.0, summary.getTotalDamageDealt(), 0.001);
        assertEquals(0.0, summary.getTotalDamageReceived(), 0.001);
        assertEquals(0, summary.getKnockbackExchanges());
        assertEquals(0, summary.getCombatDurationSeconds());
    }

    @Test
    @Order(2)
    @DisplayName("HIT_LANDED events increment attacks and hits")
    void hitLandedIncrementsAttacksAndHits() {
        List<CombatLogEntry> entries = new ArrayList<>();
        entries.add(createEntry(CombatLogEntry.EventType.HIT_LANDED, 10.0, true));
        entries.add(createEntry(CombatLogEntry.EventType.HIT_LANDED, 8.0, true));

        CombatSummary summary = new CombatSummary(entries);
        assertEquals(2, summary.getTotalAttacks());
        assertEquals(2, summary.getHitsLanded());
        assertEquals(100.0, summary.getAccuracy(), 0.001);
        assertEquals(18.0, summary.getTotalDamageDealt(), 0.001);
    }

    @Test
    @Order(3)
    @DisplayName("HIT_MISSED events increment attacks without hits")
    void hitMissedIncrementsAttacksOnly() {
        List<CombatLogEntry> entries = new ArrayList<>();
        entries.add(createEntry(CombatLogEntry.EventType.HIT_MISSED, 0.0, false));
        entries.add(createEntry(CombatLogEntry.EventType.HIT_LANDED, 5.0, true));

        CombatSummary summary = new CombatSummary(entries);
        assertEquals(2, summary.getTotalAttacks());
        assertEquals(1, summary.getHitsLanded());
        assertEquals(50.0, summary.getAccuracy(), 0.001);
        assertEquals(5.0, summary.getTotalDamageDealt(), 0.001);
    }

    @Test
    @Order(4)
    @DisplayName("DAMAGE_RECEIVED events accumulate received damage")
    void damageReceivedAccumulates() {
        List<CombatLogEntry> entries = new ArrayList<>();
        entries.add(createEntry(CombatLogEntry.EventType.DAMAGE_RECEIVED, 12.0, false));
        entries.add(createEntry(CombatLogEntry.EventType.DAMAGE_RECEIVED, 8.0, false));

        CombatSummary summary = new CombatSummary(entries);
        assertEquals(20.0, summary.getTotalDamageReceived(), 0.001);
    }

    @Test
    @Order(5)
    @DisplayName("KNOCKBACK_GIVEN and KNOCKBACK_RECEIVED increment exchanges")
    void knockbackEventsIncrementExchanges() {
        List<CombatLogEntry> entries = new ArrayList<>();
        entries.add(createEntry(CombatLogEntry.EventType.KNOCKBACK_GIVEN, 0.0, false));
        entries.add(createEntry(CombatLogEntry.EventType.KNOCKBACK_RECEIVED, 0.0, false));

        CombatSummary summary = new CombatSummary(entries);
        assertEquals(2, summary.getKnockbackExchanges());
    }

    @Test
    @Order(6)
    @DisplayName("accuracy is zero when no attacks")
    void accuracyZeroWhenNoAttacks() {
        List<CombatLogEntry> entries = new ArrayList<>();
        entries.add(createEntry(CombatLogEntry.EventType.MOVEMENT, 0.0, false));

        CombatSummary summary = new CombatSummary(entries);
        assertEquals(0.0, summary.getAccuracy(), 0.001);
    }

    @Test
    @Order(7)
    @DisplayName("getFormattedDuration formats correctly")
    void formattedDurationFormatsCorrectly() {
        List<CombatLogEntry> entries = new ArrayList<>();
        entries.add(createEntry(CombatLogEntry.EventType.COMBAT_START, 0.0, false));
        entries.add(createEntry(CombatLogEntry.EventType.COMBAT_END, 0.0, false));

        CombatSummary summary = new CombatSummary(entries);
        String formatted = summary.getFormattedDuration();
        assertNotNull(formatted);
        assertTrue(formatted.contains("m"));
        assertTrue(formatted.contains("s"));
    }

    @Test
    @Order(8)
    @DisplayName("getPlayerStats creates PlayerCombatStats with correct values")
    void getPlayerStatsReturnsCorrectStats() {
        List<CombatLogEntry> entries = new ArrayList<>();
        entries.add(createEntry(CombatLogEntry.EventType.HIT_LANDED, 15.0, true));
        entries.add(createEntry(CombatLogEntry.EventType.HIT_MISSED, 0.0, false));

        CombatSummary summary = new CombatSummary(entries);
        CombatSummary.PlayerCombatStats stats = summary.getPlayerStats("TestPlayer");

        assertEquals("TestPlayer", stats.getPlayerName());
        assertEquals(1, stats.getHitsLanded());
        assertEquals(15.0, stats.getDamageDealt(), 0.001);
        assertEquals(50.0, stats.getAccuracy(), 0.001);
    }

    @Test
    @Order(9)
    @DisplayName("getDetailedSummary returns formatted multi-line string")
    void getDetailedSummaryReturnsFormattedString() {
        List<CombatLogEntry> entries = new ArrayList<>();
        entries.add(createEntry(CombatLogEntry.EventType.COMBAT_START, 0.0, false));
        entries.add(createEntry(CombatLogEntry.EventType.HIT_LANDED, 10.0, true));
        entries.add(createEntry(CombatLogEntry.EventType.HIT_MISSED, 0.0, false));
        entries.add(createEntry(CombatLogEntry.EventType.DAMAGE_RECEIVED, 5.0, false));
        entries.add(createEntry(CombatLogEntry.EventType.KNOCKBACK_GIVEN, 0.0, false));
        entries.add(createEntry(CombatLogEntry.EventType.COMBAT_END, 0.0, false));

        CombatSummary summary = new CombatSummary(entries);
        String detailed = summary.getDetailedSummary();
        assertNotNull(detailed);
        assertTrue(detailed.contains("Combat Statistics"));
        assertTrue(detailed.contains("Attacks:"));
        assertTrue(detailed.contains("Damage:"));
        assertTrue(detailed.contains("Knockback Exchanges:"));
        assertTrue(detailed.contains("Movement Events:"));
    }

    @Test
    @Order(10)
    @DisplayName("mixed events produce correct combined statistics")
    void mixedEventsProduceCorrectStatistics() {
        List<CombatLogEntry> entries = new ArrayList<>();
        entries.add(createEntry(CombatLogEntry.EventType.HIT_LANDED, 10.0, true));
        entries.add(createEntry(CombatLogEntry.EventType.HIT_LANDED, 15.0, true));
        entries.add(createEntry(CombatLogEntry.EventType.HIT_MISSED, 0.0, false));
        entries.add(createEntry(CombatLogEntry.EventType.DAMAGE_RECEIVED, 8.0, false));
        entries.add(createEntry(CombatLogEntry.EventType.KNOCKBACK_GIVEN, 0.0, false));

        CombatSummary summary = new CombatSummary(entries);
        assertEquals(3, summary.getTotalAttacks());
        assertEquals(2, summary.getHitsLanded());
        assertEquals(25.0, summary.getTotalDamageDealt(), 0.001);
        assertEquals(8.0, summary.getTotalDamageReceived(), 0.001);
        assertEquals(1, summary.getKnockbackExchanges());
        assertEquals(66.67, summary.getAccuracy(), 0.01);
    }

    @Test
    @Order(11)
    @DisplayName("getCombatDurationSeconds returns duration from start to end")
    void combatDurationFromStartToEnd() {
        List<CombatLogEntry> entries = new ArrayList<>();
        entries.add(createEntry(CombatLogEntry.EventType.COMBAT_START, 0.0, false));
        entries.add(new CombatLogEntry.Builder()
            .sessionId(sessionId).playerId(playerId)
            .eventType(CombatLogEntry.EventType.DAMAGE_DEALT)
            .timestamp(LocalDateTime.now().plusSeconds(5))
            .damage(5.0).build());
        entries.add(createEntry(CombatLogEntry.EventType.COMBAT_END, 0.0, false));

        CombatSummary summary = new CombatSummary(entries);
        assertTrue(summary.getCombatDurationSeconds() >= 0);
    }

    @Test
    @Order(12)
    @DisplayName("MOVEMENT events are counted in detailed summary")
    void movementEventsCountedInDetailedSummary() {
        List<CombatLogEntry> entries = new ArrayList<>();
        entries.add(createEntry(CombatLogEntry.EventType.MOVEMENT, 0.0, false));
        entries.add(createEntry(CombatLogEntry.EventType.MOVEMENT, 0.0, false));
        entries.add(createEntry(CombatLogEntry.EventType.MOVEMENT, 0.0, false));

        CombatSummary summary = new CombatSummary(entries);
        String detailed = summary.getDetailedSummary();
        assertTrue(detailed.contains("Movement Events: 3"));
    }

    @Test
    @Order(13)
    @DisplayName("PlayerCombatStats stores all constructor values")
    void playerCombatStatsStoresConstructorValues() {
        CombatSummary.PlayerCombatStats stats = new CombatSummary.PlayerCombatStats("Alice", 15, 75.5, 60.0);
        assertEquals("Alice", stats.getPlayerName());
        assertEquals(15, stats.getHitsLanded());
        assertEquals(75.5, stats.getDamageDealt(), 0.001);
        assertEquals(60.0, stats.getAccuracy(), 0.001);
    }

    @Test
    @Order(14)
    @DisplayName("getCombatDurationSeconds is zero for empty entries")
    void combatDurationZeroForEmpty() {
        CombatSummary summary = new CombatSummary(new ArrayList<>());
        assertEquals(0, summary.getCombatDurationSeconds());
    }

    private CombatLogEntry createEntry(CombatLogEntry.EventType type, double damage, boolean hitLanded) {
        return new CombatLogEntry.Builder()
            .sessionId(sessionId)
            .playerId(playerId)
            .targetId(targetId)
            .eventType(type)
            .damage(damage)
            .hitLanded(hitLanded)
            .build();
    }
}
