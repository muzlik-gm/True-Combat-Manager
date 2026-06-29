package com.muzlik.pvpcombat.combat;

import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DisconnectTrackerTest {

    @Test
    @Order(1)
    @DisplayName("DisconnectType enum has BAD_INTERNET and INTENTIONAL values")
    void enumHasExpectedValues() {
        assertEquals(2, DisconnectTracker.DisconnectType.values().length);
        assertNotNull(DisconnectTracker.DisconnectType.valueOf("BAD_INTERNET"));
        assertNotNull(DisconnectTracker.DisconnectType.valueOf("INTENTIONAL"));
    }

    @Test
    @Order(2)
    @DisplayName("DisconnectData stores all constructor parameters")
    void disconnectDataStoresAllFields() {
        UUID playerId = UUID.randomUUID();
        UUID opponentId = UUID.randomUUID();
        long now = System.currentTimeMillis();
        DisconnectTracker.DisconnectType type = DisconnectTracker.DisconnectType.BAD_INTERNET;

        DisconnectTracker.DisconnectData data = new DisconnectTracker.DisconnectData(
            playerId, "TestPlayer", opponentId, "Opponent",
            10, 30, type, now, null, null, null
        );

        assertEquals(playerId, data.getPlayerId());
        assertEquals("TestPlayer", data.getPlayerName());
        assertEquals(opponentId, data.getOpponentId());
        assertEquals("Opponent", data.getOpponentName());
        assertEquals(10, data.getRemainingCombatTime());
        assertEquals(30, data.getGraceSeconds());
        assertEquals(type, data.getType());
        assertEquals(now, data.getDisconnectTime());
    }

    @Test
    @Order(3)
    @DisplayName("DisconnectData allows null opponent fields")
    void disconnectDataAllowsNullOpponent() {
        UUID playerId = UUID.randomUUID();
        DisconnectTracker.DisconnectData data = new DisconnectTracker.DisconnectData(
            playerId, "SoloPlayer", null, "Unknown",
            0, 0, DisconnectTracker.DisconnectType.INTENTIONAL, System.currentTimeMillis(),
            null, null, null
        );

        assertNull(data.getOpponentId());
        assertEquals("Unknown", data.getOpponentName());
    }

    @Test
    @Order(4)
    @DisplayName("DisconnectData allows null location and inventory")
    void disconnectDataAllowsNullLocation() {
        UUID playerId = UUID.randomUUID();
        DisconnectTracker.DisconnectData data = new DisconnectTracker.DisconnectData(
            playerId, "Test", null, "Unknown",
            5, 15, DisconnectTracker.DisconnectType.BAD_INTERNET, System.currentTimeMillis(),
            null, null, null
        );

        assertNull(data.getDisconnectLocation());
        assertNull(data.getInventory());
        assertNull(data.getArmor());
    }

    @Test
    @Order(5)
    @DisplayName("DisconnectData getPunishmentTask returns null initially")
    void disconnectDataPunishmentTaskNullInitially() {
        DisconnectTracker.DisconnectData data = new DisconnectTracker.DisconnectData(
            UUID.randomUUID(), "Test", null, "Unknown",
            0, 0, DisconnectTracker.DisconnectType.INTENTIONAL, System.currentTimeMillis(),
            null, null, null
        );

        assertNull(data.getPunishmentTask());
    }

    @Test
    @Order(6)
    @DisplayName("DisconnectData setPunishmentTask stores reference")
    void disconnectDataSetPunishmentTask() {
        DisconnectTracker.DisconnectData data = new DisconnectTracker.DisconnectData(
            UUID.randomUUID(), "Test", null, "Unknown",
            0, 0, DisconnectTracker.DisconnectType.INTENTIONAL, System.currentTimeMillis(),
            null, null, null
        );

        org.bukkit.scheduler.BukkitTask mockTask = org.mockito.Mockito.mock(org.bukkit.scheduler.BukkitTask.class);
        data.setPunishmentTask(mockTask);
        assertSame(mockTask, data.getPunishmentTask());
    }

    @Test
    @Order(7)
    @DisplayName("DisconnectData stores remaining combat time zero")
    void disconnectDataRemainingCombatTimeZero() {
        DisconnectTracker.DisconnectData data = new DisconnectTracker.DisconnectData(
            UUID.randomUUID(), "Test", null, "Unknown",
            0, 0, DisconnectTracker.DisconnectType.INTENTIONAL, System.currentTimeMillis(),
            null, null, null
        );

        assertEquals(0, data.getRemainingCombatTime());
        assertEquals(0, data.getGraceSeconds());
    }

    @Test
    @Order(8)
    @DisplayName("DisconnectType BAD_INTERNET is not INTENTIONAL")
    void badInternetIsNotIntentional() {
        assertNotEquals(
            DisconnectTracker.DisconnectType.BAD_INTERNET,
            DisconnectTracker.DisconnectType.INTENTIONAL
        );
    }
}
