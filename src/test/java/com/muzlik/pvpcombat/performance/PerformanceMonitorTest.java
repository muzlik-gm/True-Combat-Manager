package com.muzlik.pvpcombat.performance;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import com.muzlik.pvpcombat.utils.AsyncUtils;
import com.muzlik.pvpcombat.utils.CacheManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PerformanceMonitorTest {

    @Mock
    private PvPCombatPlugin plugin;

    @Mock
    private TPSMonitor tpsMonitor;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private FileConfiguration config;

    private PerformanceMonitor monitor;

    @BeforeAll
    static void initAsyncUtils() {
        AsyncUtils.initialize();
    }

    @AfterAll
    static void shutdownAsyncUtils() {
        AsyncUtils.shutdown();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getConfig()).thenReturn(config);
        when(config.getBoolean("performance.monitoring-enabled", true)).thenReturn(false);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("Test"));

        when(tpsMonitor.getCurrentTPS()).thenReturn(20.0);
        when(tpsMonitor.getAverageTPS()).thenReturn(20.0);
        when(tpsMonitor.getTPSSeverity(18.0)).thenReturn(0.0);

        monitor = new PerformanceMonitor(plugin, tpsMonitor, cacheManager);
    }

    @Test
    @Order(1)
    @DisplayName("isUnderStress returns false with perfect TPS")
    void isUnderStressFalseWithPerfectTPS() {
        assertFalse(monitor.isUnderStress());
    }

    @Test
    @Order(2)
    @DisplayName("isUnderStress returns true with low TPS")
    void isUnderStressTrueWithLowTPS() {
        when(tpsMonitor.getCurrentTPS()).thenReturn(15.0);
        assertTrue(monitor.isUnderStress());
    }

    @Test
    @Order(3)
    @DisplayName("getTpsMonitor returns the injected TPSMonitor")
    void getTpsMonitorReturnsInjected() {
        assertSame(tpsMonitor, monitor.getTpsMonitor());
    }

    @Test
    @Order(4)
    @DisplayName("getCacheManager returns the injected CacheManager")
    void getCacheManagerReturnsInjected() {
        assertSame(cacheManager, monitor.getCacheManager());
    }

    @Test
    @Order(5)
    @DisplayName("getPerformanceStats returns formatted string with TPS")
    void getPerformanceStatsContainsTPS() {
        String stats = monitor.getPerformanceStats();
        assertNotNull(stats);
        assertTrue(stats.contains("TPS:"));
        assertTrue(stats.contains("20.00"));
    }

    @Test
    @Order(6)
    @DisplayName("getPerformanceStats contains memory info")
    void getPerformanceStatsContainsMemory() {
        String stats = monitor.getPerformanceStats();
        assertTrue(stats.contains("Memory:"));
    }

    @Test
    @Order(7)
    @DisplayName("getPerformanceStats contains cache info")
    void getPerformanceStatsContainsCache() {
        when(cacheManager.getTotalCacheSize()).thenReturn(42L);
        String stats = monitor.getPerformanceStats();
        assertTrue(stats.contains("Cache:"));
    }

    @Test
    @Order(8)
    @DisplayName("startOperation and endOperation record timing")
    void startEndOperationRecordsMetrics() {
        monitor.startOperation("test-op");
        monitor.endOperation("test-op");
        String stats = monitor.getPerformanceStats();
        assertNotNull(stats);
    }

    @Test
    @Order(9)
    @DisplayName("timeOperation runs the runnable and records timing")
    void timeOperationRunsRunnable() {
        boolean[] ran = {false};
        monitor.timeOperation("timed-op", () -> ran[0] = true);
        assertTrue(ran[0]);
    }

    @Test
    @Order(10)
    @DisplayName("startOperation with different names records separately")
    void startOperationDifferentNames() {
        monitor.startOperation("op-a");
        monitor.startOperation("op-b");
        monitor.endOperation("op-a");
        monitor.endOperation("op-b");
        String stats = monitor.getPerformanceStats();
        assertNotNull(stats);
    }

    @Test
    @Order(11)
    @DisplayName("endOperation handles missing start gracefully")
    void endOperationHandlesMissingStart() {
        monitor.endOperation("never-started");
        assertFalse(monitor.isUnderStress());
    }

    @Test
    @Order(12)
    @DisplayName("timeOperation handles exception in runnable")
    void timeOperationHandlesException() {
        assertThrows(RuntimeException.class, () ->
            monitor.timeOperation("failing-op", () -> {
                throw new RuntimeException("test failure");
            })
        );
    }

    @Test
    @Order(13)
    @DisplayName("forceGC does not throw")
    void forceGCDoesNotThrow() {
        assertDoesNotThrow(() -> monitor.forceGC());
    }

    @Test
    @Order(14)
    @DisplayName("isUnderStress uses TPS threshold 18.0")
    void isUnderStressUsesTPSTreshold18() {
        when(tpsMonitor.getCurrentTPS()).thenReturn(18.0);
        assertFalse(monitor.isUnderStress());
        when(tpsMonitor.getCurrentTPS()).thenReturn(17.9);
        assertTrue(monitor.isUnderStress());
    }

    @Test
    @Order(15)
    @DisplayName("getPerformanceStats returns consistent format")
    void getPerformanceStatsConsistentFormat() {
        String stats1 = monitor.getPerformanceStats();
        String stats2 = monitor.getPerformanceStats();
        assertEquals(stats1, stats2);
    }
}
