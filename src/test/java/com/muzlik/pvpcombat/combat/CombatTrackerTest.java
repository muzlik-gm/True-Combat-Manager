package com.muzlik.pvpcombat.combat;

import com.muzlik.pvpcombat.data.CombatEvent;
import com.muzlik.pvpcombat.data.PlayerCombatData;
import com.muzlik.pvpcombat.interfaces.IDatabaseManager;
import com.muzlik.pvpcombat.performance.LagManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CombatTrackerTest {

    @Mock
    private Plugin plugin;

    @Mock
    private LagManager lagManager;

    @Mock
    private IDatabaseManager databaseManager;

    @Mock
    private Player player;

    private UUID playerId;
    private CombatTracker tracker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("Test"));
        playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        tracker = new CombatTracker(plugin);
    }

    @Test
    @Order(1)
    @DisplayName("constructor initializes empty player data map")
    void constructorInitializesEmptyMaps() {
        assertTrue(tracker.getAllPlayerData().isEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("getPlayerData creates new data for unknown player")
    void getPlayerDataCreatesNewData() {
        PlayerCombatData data = tracker.getPlayerData(playerId);
        assertNotNull(data);
        assertEquals(playerId, data.getPlayerId());
        assertEquals(0, data.getWins());
        assertEquals(0, data.getLosses());
    }

    @Test
    @Order(3)
    @DisplayName("getPlayerData returns same data for repeated calls")
    void getPlayerDataReturnsSameInstance() {
        PlayerCombatData first = tracker.getPlayerData(playerId);
        PlayerCombatData second = tracker.getPlayerData(playerId);
        assertSame(first, second);
    }

    @Test
    @Order(4)
    @DisplayName("getAllPlayerData returns all tracked data")
    void getAllPlayerDataReturnsAllData() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        tracker.getPlayerData(id1);
        tracker.getPlayerData(id2);
        assertEquals(2, tracker.getAllPlayerData().size());
    }

    @Test
    @Order(5)
    @DisplayName("getAllPlayerData returns a defensive copy")
    void getAllPlayerDataIsDefensiveCopy() {
        tracker.getPlayerData(playerId);
        Map<UUID, PlayerCombatData> data = tracker.getAllPlayerData();
        data.clear();
        assertFalse(tracker.getAllPlayerData().isEmpty());
    }

    @Test
    @Order(6)
    @DisplayName("setLagManager stores reference")
    void setLagManagerStoresReference() {
        tracker.setLagManager(lagManager);
        tracker.recordDamageDealt(player, 10.0);
        verify(lagManager).updatePlayerPing(player);
    }

    @Test
    @Order(7)
    @DisplayName("setDatabaseManager stores reference")
    void setDatabaseManagerStoresReference() {
        tracker.setDatabaseManager(databaseManager);
        tracker.saveAllData();
        verify(databaseManager).saveBatch(any());
    }

    @Test
    @Order(8)
    @DisplayName("saveAllData does nothing when databaseManager is null")
    void saveAllDataNoOpWithoutDatabase() {
        tracker.saveAllData();
        assertTrue(true);
    }

    @Test
    @Order(9)
    @DisplayName("recordDamageDealt updates damage and combo")
    void recordDamageDealtUpdatesDamageAndCombo() {
        tracker.recordDamageDealt(player, 15.5);
        PlayerCombatData data = tracker.getPlayerData(playerId);
        assertEquals(15.5, data.getTotalDamageDealt(), 0.001);
        assertTrue(data.getLastActivity() > 0);
    }

    @Test
    @Order(10)
    @DisplayName("recordDamageReceived updates damage and resets combo")
    void recordDamageReceivedUpdatesDamageAndResetsCombo() {
        tracker.recordDamageDealt(player, 10.0);
        tracker.recordDamageReceived(player, 5.0);
        PlayerCombatData data = tracker.getPlayerData(playerId);
        assertEquals(5.0, data.getTotalDamageReceived(), 0.001);
        assertEquals(0, tracker.getAndResetCombo(playerId));
    }

    @Test
    @Order(11)
    @DisplayName("getAndResetCombo returns 0 for unknown player")
    void getAndResetComboReturnsZeroForUnknown() {
        assertEquals(0, tracker.getAndResetCombo(UUID.randomUUID()));
    }

    @Test
    @Order(12)
    @DisplayName("getAndResetCombo returns current streak and clears it")
    void getAndResetComboReturnsAndClears() {
        tracker.recordDamageDealt(player, 5.0);
        tracker.recordDamageDealt(player, 5.0);
        tracker.recordDamageDealt(player, 5.0);
        assertEquals(3, tracker.getAndResetCombo(playerId));
        assertEquals(0, tracker.getAndResetCombo(playerId));
    }

    @Test
    @Order(13)
    @DisplayName("recordWinByUUID increments wins and combats")
    void recordWinByUUIDIncrementsStats() {
        tracker.recordWinByUUID(playerId);
        PlayerCombatData data = tracker.getPlayerData(playerId);
        assertEquals(1, data.getWins());
        assertEquals(1, data.getTotalCombats());
    }

    @Test
    @Order(14)
    @DisplayName("recordLossByUUID increments losses and combats")
    void recordLossByUUIDIncrementsStats() {
        tracker.recordLossByUUID(playerId);
        PlayerCombatData data = tracker.getPlayerData(playerId);
        assertEquals(1, data.getLosses());
        assertEquals(1, data.getTotalCombats());
    }

    @Test
    @Order(15)
    @DisplayName("cleanupInactiveData removes old entries")
    void cleanupInactiveDataRemovesOldEntries() {
        UUID oldId = UUID.randomUUID();
        tracker.getPlayerData(oldId);
        tracker.cleanupInactiveData();
        assertTrue(tracker.getAllPlayerData().containsKey(oldId));
        PlayerCombatData oldData = tracker.getPlayerData(oldId);
        oldData.updateLastActivity(System.currentTimeMillis() - 25L * 60 * 60 * 1000);
        tracker.cleanupInactiveData();
        assertFalse(tracker.getAllPlayerData().containsKey(oldId));
    }

    @Test
    @Order(16)
    @DisplayName("recordEvent adds event to player data")
    void recordEventAddsEvent() {
        CombatEvent event = new CombatEvent(UUID.randomUUID(), playerId, "test") {};
        tracker.recordEvent(event);
        PlayerCombatData data = tracker.getPlayerData(playerId);
        assertTrue(data.getEvents().contains(event));
    }

    @Test
    @Order(17)
    @DisplayName("constructor uses plugin logger")
    void constructorUsesPluginLogger() {
        verify(plugin, atLeastOnce()).getLogger();
    }

    @Test
    @Order(18)
    @DisplayName("recordDamageDealt with lagManager calls update")
    void recordDamageDealtWithLagManager() {
        tracker.setLagManager(lagManager);
        tracker.recordDamageDealt(player, 10.0);
        verify(lagManager).updatePlayerPing(player);
    }

    @Test
    @Order(19)
    @DisplayName("recordDamageReceived with lagManager calls update")
    void recordDamageReceivedWithLagManager() {
        tracker.setLagManager(lagManager);
        tracker.recordDamageReceived(player, 10.0);
        verify(lagManager).updatePlayerPing(player);
    }
}
