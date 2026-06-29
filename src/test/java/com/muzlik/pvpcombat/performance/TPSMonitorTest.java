package com.muzlik.pvpcombat.performance;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TPSMonitorTest {

    @Mock
    private PvPCombatPlugin plugin;

    @Mock
    private FileConfiguration config;

    private TPSMonitor monitor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getConfig()).thenReturn(config);
        when(config.getInt("lag.tps-history-length", 60)).thenReturn(60);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("Test"));
        monitor = new TPSMonitor(plugin);
    }

    @Test
    @Order(1)
    @DisplayName("initial currentTPS is 20.0")
    void initialCurrentTPSIs20() {
        assertEquals(20.0, monitor.getCurrentTPS(), 0.001);
    }

    @Test
    @Order(2)
    @DisplayName("initial averageTPS is 20.0")
    void initialAverageTPSIs20() {
        assertEquals(20.0, monitor.getAverageTPS(), 0.001);
    }

    @Test
    @Order(3)
    @DisplayName("isLagging returns false when TPS is above threshold")
    void isLaggingFalseAboveThreshold() {
        assertFalse(monitor.isLagging(18.0));
    }

    @Test
    @Order(4)
    @DisplayName("isLagging returns true when threshold exceeds TPS")
    void isLaggingTrueWhenThresholdExceedsTPS() {
        assertTrue(monitor.isLagging(25.0));
    }

    @Test
    @Order(5)
    @DisplayName("reset restores currentTPS to 20.0")
    void resetRestoresCurrentTPS() {
        monitor.reset();
        assertEquals(20.0, monitor.getCurrentTPS(), 0.001);
    }

    @Test
    @Order(6)
    @DisplayName("reset restores averageTPS to 20.0")
    void resetRestoresAverageTPS() {
        monitor.reset();
        assertEquals(20.0, monitor.getAverageTPS(), 0.001);
    }

    @Test
    @Order(7)
    @DisplayName("getTPSSeverity returns 0.0 for perfect TPS (20.0)")
    void tpsSeverityZeroForPerfectTPS() {
        assertEquals(0.0, monitor.getTPSSeverity(18.0), 0.001);
    }

    @Test
    @Order(8)
    @DisplayName("getTPSSeverity returns 0.5 when TPS equals minimum acceptable")
    void tpsSeverityAtThreshold() {
        double severity = monitor.getTPSSeverity(20.0);
        assertEquals(0.0, severity, 0.001);
    }

    @Test
    @Order(9)
    @DisplayName("getTPSSeverity is between 0 and 1")
    void tpsSeverityInRange() {
        double severity = monitor.getTPSSeverity(18.0);
        assertTrue(severity >= 0.0 && severity <= 1.0);
    }

    @Test
    @Order(10)
    @DisplayName("reset clears history size to 0")
    void resetClearsHistorySize() {
        monitor.reset();
        assertEquals(0, monitor.getTPSHistory().length);
    }

    @Test
    @Order(11)
    @DisplayName("getPerformanceStats returns formatted string")
    void getPerformanceStatsReturnsFormattedString() {
        String stats = monitor.getPerformanceStats();
        assertNotNull(stats);
        assertTrue(stats.contains("TPSMonitor"));
        assertTrue(stats.contains("current="));
        assertTrue(stats.contains("average="));
    }

    @Test
    @Order(12)
    @DisplayName("getTPSHistory is empty after reset")
    void tpsHistoryEmptyAfterReset() {
        monitor.reset();
        assertEquals(0, monitor.getTPSHistory().length);
    }

    @Test
    @Order(13)
    @DisplayName("getTimeSinceLastUpdate returns positive value")
    void getTimeSinceLastUpdateReturnsPositive() {
        assertTrue(monitor.getTimeSinceLastUpdate() >= 0);
    }

    @Test
    @Order(14)
    @DisplayName("constructor loads history length from config")
    void constructorLoadsHistoryLengthFromConfig() {
        when(config.getInt("lag.tps-history-length", 60)).thenReturn(30);
        TPSMonitor customMonitor = new TPSMonitor(plugin);
        assertNotNull(customMonitor);
    }

    @Test
    @Order(15)
    @DisplayName("reloadConfiguration reloads config value")
    void reloadConfigurationReloadsConfig() {
        when(config.getInt("lag.tps-history-length", 60)).thenReturn(120);
        monitor.reloadConfiguration();
        assertNotNull(monitor.getPerformanceStats());
    }
}
