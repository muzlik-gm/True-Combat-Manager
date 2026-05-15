package com.muzlik.pvpcombat.data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import com.muzlik.pvpcombat.data.CombatEvent;
import com.muzlik.pvpcombat.data.CombatStatistics;
import com.muzlik.pvpcombat.data.DamageInfo;
import com.muzlik.pvpcombat.data.WeaponStats;

/**
 * Model class for storing player-specific combat data and statistics.
 *
 * <p>All numeric counters that are updated from multiple threads
 * (damage events, win/loss recording) use atomic types to prevent
 * lost-update races under concurrent access.</p>
 */
public class PlayerCombatData {
    private final UUID playerId;

    // ── Atomic counters (updated from multiple threads) ───────────────────
    private final AtomicInteger totalCombats       = new AtomicInteger(0);
    private final AtomicInteger wins               = new AtomicInteger(0);
    private final AtomicInteger losses             = new AtomicInteger(0);
    private final AtomicLong    totalCombatTimeMs  = new AtomicLong(0);
    // Doubles stored as long bits via AtomicLong for lock-free CAS updates
    private final AtomicLong    totalDamageDealtBits    = new AtomicLong(Double.doubleToLongBits(0.0));
    private final AtomicLong    totalDamageReceivedBits = new AtomicLong(Double.doubleToLongBits(0.0));
    private final AtomicInteger criticalHits       = new AtomicInteger(0);
    private final AtomicLong    lastActivityMs     = new AtomicLong(System.currentTimeMillis());

    // ── Non-atomic fields (accessed on main thread only) ──────────────────
    private final Map<String, Integer> weaponUsage;
    private LocalDateTime lastCombat;
    private RestrictionData restrictionData;
    private List<CombatEvent> events = new ArrayList<>();
    private CombatStatistics stats = new CombatStatistics(null);

    // Enhanced tracking (main-thread only)
    private int longestCombo;
    private double highestDamageInSession;
    private final Map<String, WeaponStats> weaponStats;

    public PlayerCombatData(UUID playerId) {
        this.playerId = playerId;
        this.weaponUsage = new HashMap<>();
        this.weaponStats = new HashMap<>();
        this.lastCombat = LocalDateTime.now();
        this.restrictionData = new RestrictionData(playerId);
        this.longestCombo = 0;
        this.highestDamageInSession = 0.0;
    }

    // ── Getters / setters ─────────────────────────────────────────────────

    public UUID getPlayerId() { return playerId; }

    // totalCombats
    public int  getTotalCombats()              { return totalCombats.get(); }
    public void setTotalCombats(int v)         { totalCombats.set(v); }
    public void incrementCombats()             { totalCombats.incrementAndGet(); }

    // wins
    public int  getWins()                      { return wins.get(); }
    public void setWins(int v)                 { wins.set(v); }
    public void incrementWins()                { wins.incrementAndGet(); }

    // losses
    public int  getLosses()                    { return losses.get(); }
    public void setLosses(int v)               { losses.set(v); }
    public void incrementLosses()              { losses.incrementAndGet(); }

    // totalCombatTime
    public long getTotalCombatTime()           { return totalCombatTimeMs.get(); }
    public void setTotalCombatTime(long v)     { totalCombatTimeMs.set(v); }
    public void addCombatTime(long ms)         { totalCombatTimeMs.addAndGet(ms); }

    // totalDamageDealt — CAS loop for atomic double add
    public double getTotalDamageDealt() {
        return Double.longBitsToDouble(totalDamageDealtBits.get());
    }
    public void setTotalDamageDealt(double v) {
        totalDamageDealtBits.set(Double.doubleToLongBits(v));
    }
    public void addDamageDealt(double damage) {
        totalDamageDealtBits.updateAndGet(bits ->
            Double.doubleToLongBits(Double.longBitsToDouble(bits) + damage));
    }

    // totalDamageReceived
    public double getTotalDamageReceived() {
        return Double.longBitsToDouble(totalDamageReceivedBits.get());
    }
    public void setTotalDamageReceived(double v) {
        totalDamageReceivedBits.set(Double.doubleToLongBits(v));
    }
    public void addDamageReceived(double damage) {
        totalDamageReceivedBits.updateAndGet(bits ->
            Double.doubleToLongBits(Double.longBitsToDouble(bits) + damage));
    }

    // criticalHits
    public int  getCriticalHits()              { return criticalHits.get(); }
    public void setCriticalHits(int v)         { criticalHits.set(v); }
    public void incrementCriticalHits()        { criticalHits.incrementAndGet(); }

    // lastActivity
    public long getLastActivity()              { return lastActivityMs.get(); }
    public void updateLastActivity(long time)  { lastActivityMs.set(time); }

    // ── Non-atomic fields ─────────────────────────────────────────────────

    public Map<String, Integer> getWeaponUsage() { return weaponUsage; }
    public void incrementWeaponUsage(String weaponType) {
        weaponUsage.merge(weaponType, 1, Integer::sum);
    }

    public LocalDateTime getLastCombat()                    { return lastCombat; }
    public void setLastCombat(LocalDateTime lastCombat)     { this.lastCombat = lastCombat; }

    public RestrictionData getRestrictionData()             { return restrictionData; }
    public void setRestrictionData(RestrictionData r)       { this.restrictionData = r; }

    public List<CombatEvent> getEvents()                    { return events; }
    public CombatStatistics  getStats()                     { return stats; }

    public void clearRestrictions() {
        if (restrictionData != null) restrictionData.clearAllRestrictions();
    }

    // longestCombo
    public int  getLongestCombo()              { return longestCombo; }
    public void setLongestCombo(int v)         { this.longestCombo = v; }
    public void updateLongestCombo(int combo) {
        if (combo > longestCombo) longestCombo = combo;
    }

    // highestDamageInSession
    public double getHighestDamageInSession()          { return highestDamageInSession; }
    public void   setHighestDamageInSession(double v)  { this.highestDamageInSession = v; }
    public void   updateHighestDamage(double damage) {
        if (damage > highestDamageInSession) highestDamageInSession = damage;
    }

    // weaponStats
    public Map<String, WeaponStats> getWeaponStats()           { return weaponStats; }
    public WeaponStats getWeaponStats(String weaponMaterial) {
        return weaponStats.computeIfAbsent(weaponMaterial, WeaponStats::new);
    }

    public void recordDamageWithWeapon(DamageInfo damageInfo) {
        String material = damageInfo.getWeaponMaterial() != null
            ? damageInfo.getWeaponMaterial().name() : "AIR";
        WeaponStats ws = getWeaponStats(material);
        ws.recordHit(damageInfo.getAmount(), damageInfo.isCritical());
        if (damageInfo.isCritical()) incrementCriticalHits();
        addDamageDealt(damageInfo.getAmount());
        updateHighestDamage(damageInfo.getAmount());
    }

    public void recordKillWithWeapon(String weaponMaterial) {
        getWeaponStats(weaponMaterial).recordKill();
    }

    // ── Computed stats ────────────────────────────────────────────────────

    public double getKDRatio() {
        int l = losses.get();
        return l == 0 ? wins.get() : (double) wins.get() / l;
    }

    public double getWinRate() {
        int tc = totalCombats.get();
        return tc == 0 ? 0.0 : ((double) wins.get() / tc) * 100.0;
    }

    public double getDamageRatio() {
        double recv = getTotalDamageReceived();
        double dealt = getTotalDamageDealt();
        return recv == 0 ? (dealt > 0 ? dealt : 0.0) : dealt / recv;
    }
}
