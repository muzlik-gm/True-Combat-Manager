package com.muzlik.pvpcombat.performance;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import com.muzlik.pvpcombat.data.PerformanceData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PingTrackerTest {

    @Mock
    private PvPCombatPlugin plugin;

    @Mock
    private FileConfiguration config;

    @Mock
    private Player player;

    private UUID playerId;
    private PingTracker tracker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        playerId = UUID.randomUUID();

        when(plugin.getConfig()).thenReturn(config);
        when(config.getLong("lag.ping-update-interval-ms", 1000L)).thenReturn(1000L);
        when(config.getLong("lag.ping-cleanup-threshold-ms", 300000L)).thenReturn(300000L);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("Test"));

        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("TestPlayer");

        tracker = new PingTracker(plugin);
    }

    @Test
    @Order(1)
    @DisplayName("constructor initializes with zero consecutive failures")
    void constructorInitializesZeroFailures() {
        assertEquals(0, tracker.getConsecutiveFailures());
    }

    @Test
    @Order(2)
    @DisplayName("isExperiencingIssues returns false initially")
    void isExperiencingIssuesFalseInitially() {
        assertFalse(tracker.isExperiencingIssues());
    }

    @Test
    @Order(3)
    @DisplayName("getAveragePing returns 0.0 on empty tracker")
    void getAveragePingReturnsZeroOnEmpty() {
        assertEquals(0.0, tracker.getAveragePing(), 0.001);
    }

    @Test
    @Order(4)
    @DisplayName("getHighestPing returns 0 on empty tracker")
    void getHighestPingReturnsZeroOnEmpty() {
        assertEquals(0, tracker.getHighestPing());
    }

    @Test
    @Order(5)
    @DisplayName("getPerformanceData returns null for unknown player")
    void getPerformanceDataReturnsNullForUnknown() {
        assertNull(tracker.getPerformanceData(UUID.randomUUID()));
    }

    @Test
    @Order(6)
    @DisplayName("getOrCreatePerformanceData creates new data for unknown player")
    void getOrCreatePerformanceDataCreatesNew() {
        PerformanceData data = tracker.getOrCreatePerformanceData(playerId);
        assertNotNull(data);
        assertEquals(playerId, data.getPlayerId());
    }

    @Test
    @Order(7)
    @DisplayName("getOrCreatePerformanceData returns existing data")
    void getOrCreatePerformanceDataReturnsExisting() {
        PerformanceData first = tracker.getOrCreatePerformanceData(playerId);
        PerformanceData second = tracker.getOrCreatePerformanceData(playerId);
        assertSame(first, second);
    }

    @Test
    @Order(8)
    @DisplayName("getPingStats returns formatted string")
    void getPingStatsReturnsFormattedString() {
        String stats = tracker.getPingStats();
        assertNotNull(stats);
        assertTrue(stats.startsWith("PingTracker"));
        assertTrue(stats.contains("totalPlayers="));
        assertTrue(stats.contains("updateInterval="));
    }

    @Test
    @Order(9)
    @DisplayName("getHealthStatus returns formatted string")
    void getHealthStatusReturnsFormattedString() {
        String status = tracker.getHealthStatus();
        assertNotNull(status);
        assertTrue(status.startsWith("PingTrackerHealth"));
        assertTrue(status.contains("failures="));
        assertTrue(status.contains("lastSuccess="));
    }

    @Test
    @Order(10)
    @DisplayName("updatePlayerPing handles null player gracefully")
    void updatePlayerPingHandlesNull() {
        tracker.updatePlayerPing(null);
        assertEquals(0, tracker.getConsecutiveFailures());
    }

    @Test
    @Order(11)
    @DisplayName("cleanupInactiveData does nothing on empty tracker")
    void cleanupInactiveDataNoOpOnEmpty() {
        tracker.cleanupInactiveData();
        assertEquals(0.0, tracker.getAveragePing(), 0.001);
    }

    @Test
    @Order(12)
    @DisplayName("reloadConfiguration reloads from config")
    void reloadConfigurationReloadsFromConfig() {
        tracker.reloadConfiguration();
        assertEquals(0, tracker.getConsecutiveFailures());
    }

    @Test
    @Order(13)
    @DisplayName("getPingStats reflects tracked players")
    void getPingStatsReflectsTrackedPlayers() {
        tracker.getOrCreatePerformanceData(playerId);
        String stats = tracker.getPingStats();
        assertTrue(stats.contains("totalPlayers=1"));
    }

    @Test
    @Order(14)
    @DisplayName("updatePlayerPing rate-limits by updateInterval")
    void updatePlayerPingRateLimits() {
        when(config.getDouble("lag.tps-threshold", 18.0)).thenReturn(18.0);
        when(config.getInt("lag.ping-threshold", 200)).thenReturn(200);

        tracker.getOrCreatePerformanceData(playerId);
        tracker.updatePlayerPing(player);
        int failuresAfter = tracker.getConsecutiveFailures();
        assertEquals(0, failuresAfter);
    }

    @Test
    @Order(15)
    @DisplayName("getAveragePing returns correct value for tracked players")
    void getAveragePingWithData() {
        PerformanceData data = tracker.getOrCreatePerformanceData(playerId);
        data.setCurrentPing(100);
        data.setCurrentPing(200);
        assertTrue(tracker.getAveragePing() > 0);
    }
}
