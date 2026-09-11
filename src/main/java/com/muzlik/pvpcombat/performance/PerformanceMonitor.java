package com.muzlik.pvpcombat.performance;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import com.muzlik.pvpcombat.utils.AsyncUtils;
import com.muzlik.pvpcombat.utils.CacheManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive performance monitoring system for the PvP Combat plugin.
 * Tracks TPS, memory usage, cache performance, and operation timings.
 */
public class PerformanceMonitor {

    private final PvPCombatPlugin plugin;
    private final TPSMonitor tpsMonitor;
    private final CacheManager cacheManager;

    // Performance metrics
    private final Map<String, AtomicLong> operationCounts;
    private final Map<String, AtomicLong> operationTimes;
    private final Map<String, Long> operationStartTimes;

    // Memory tracking
    private volatile long peakMemoryUsage;

    public PerformanceMonitor(PvPCombatPlugin plugin, TPSMonitor tpsMonitor, CacheManager cacheManager) {
        this.plugin = plugin;
        this.tpsMonitor = tpsMonitor;
        this.cacheManager = cacheManager;

        this.operationCounts = new ConcurrentHashMap<>();
        this.operationTimes = new ConcurrentHashMap<>();
        this.operationStartTimes = new ConcurrentHashMap<>();

        this.peakMemoryUsage = 0;

        // Start periodic monitoring
        startMonitoring();
    }

    /**
     * Starts periodic performance monitoring tasks.
     */
    private void startMonitoring() {
        // Monitor every 30 seconds
        AsyncUtils.scheduleMonitoringTask(this::collectMetrics, 30, 30, java.util.concurrent.TimeUnit.SECONDS);

        // Memory check every 5 minutes
        AsyncUtils.scheduleMonitoringTask(this::checkMemoryUsage, 5, 5, java.util.concurrent.TimeUnit.MINUTES);
    }

    /**
     * Starts timing an operation.
     */
    public void startOperation(String operationName) {
        operationStartTimes.put(operationName + Thread.currentThread().getId(),
                               System.nanoTime());
    }

    /**
     * Ends timing an operation and records the metrics.
     */
    public void endOperation(String operationName) {
        String key = operationName + Thread.currentThread().getId();
        Long startTime = operationStartTimes.remove(key);

        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            operationCounts.computeIfAbsent(operationName, k -> new AtomicLong()).incrementAndGet();
            operationTimes.computeIfAbsent(operationName, k -> new AtomicLong()).addAndGet(duration);
        }
    }

    /**
     * Times a complete operation.
     */
    public void timeOperation(String operationName, Runnable operation) {
        startOperation(operationName);
        try {
            operation.run();
        } finally {
            endOperation(operationName);
        }
    }

    /**
     * Collects and logs current performance metrics.
     */
    private void collectMetrics() {
        if (!plugin.getConfig().getBoolean("performance.monitoring-enabled", true)) {
            return;
        }

        StringBuilder metrics = new StringBuilder();
        metrics.append("=== PvP Combat Performance Metrics ===\n");

        // TPS metrics
        metrics.append(String.format("TPS: Current=%.2f, Average=%.2f, Severity=%.2f\n",
                tpsMonitor.getCurrentTPS(),
                tpsMonitor.getAverageTPS(),
                tpsMonitor.getTPSSeverity(18.0)));

        // Memory metrics
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        metrics.append(String.format("Memory: Used=%dMB/%dMB (%.1f%%), Peak=%dMB\n",
                usedMemory / 1024 / 1024,
                maxMemory / 1024 / 1024,
                (double) usedMemory / maxMemory * 100,
                peakMemoryUsage / 1024 / 1024));

        // Cache metrics
        if (cacheManager != null) {
            metrics.append("Cache: ").append(cacheManager.getTotalCacheSize()).append(" items\n");
            metrics.append(cacheManager.getAllCacheStats());
        }

        // Operation metrics
        metrics.append("Operations:\n");
        operationCounts.forEach((op, count) -> {
            AtomicLong time = operationTimes.get(op);
            if (time != null && count.get() > 0) {
                double avgTime = time.get() / (double) count.get() / 1_000_000; // Convert to milliseconds
                metrics.append(String.format("- %s: count=%d, avg=%.2fms\n", op, count.get(), avgTime));
            }
        });

        // Thread pool status
        metrics.append("Thread Pools:\n").append(AsyncUtils.getThreadPoolStatus());

        plugin.getLogger().info(metrics.toString());

        // Reset counters for next period
        resetCounters();
    }

    /**
     * Checks memory usage and updates peak if necessary.
     */
    private void checkMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();

        if (usedMemory > peakMemoryUsage) {
            peakMemoryUsage = usedMemory;
        }

        // Warn if memory usage is high
        long maxMemory = runtime.maxMemory();
        double usagePercent = (double) usedMemory / maxMemory * 100;

        if (usagePercent > 85) {
            plugin.getLogger().warning(String.format("High memory usage detected: %.1f%% (%dMB/%dMB)",
                    usagePercent, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024));
        }
    }

    /**
     * Resets operation counters for the next monitoring period.
     */
    private void resetCounters() {
        operationCounts.clear();
        operationTimes.clear();
    }

    /**
     * Gets performance statistics as a formatted string.
     */
    public String getPerformanceStats() {
        StringBuilder stats = new StringBuilder();

        stats.append(String.format("TPS: %.2f/%.2f (severity: %.2f)\n",
                tpsMonitor.getCurrentTPS(), tpsMonitor.getAverageTPS(),
                tpsMonitor.getTPSSeverity(18.0)));

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();

        stats.append(String.format("Memory: %dMB/%dMB (%.1f%%)\n",
                usedMemory / 1024 / 1024, maxMemory / 1024 / 1024,
                (double) usedMemory / maxMemory * 100));

        if (cacheManager != null) {
            stats.append("Cache: ").append(cacheManager.getTotalCacheSize()).append(" items cached\n");
        }

        return stats.toString();
    }

    /**
     * Checks if the system is under performance stress.
     */
    public boolean isUnderStress() {
        return tpsMonitor.getCurrentTPS() < 18.0 ||
               (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) /
               (double) Runtime.getRuntime().maxMemory() > 0.9;
    }

    /**
     * Gets the current TPS monitor.
     */
    public TPSMonitor getTpsMonitor() {
        return tpsMonitor;
    }

    /**
     * Gets the cache manager.
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * Forces a garbage collection (use sparingly for debugging).
     */
    public void forceGC() {
        System.gc();
        plugin.getLogger().info("Forced garbage collection");
    }
}