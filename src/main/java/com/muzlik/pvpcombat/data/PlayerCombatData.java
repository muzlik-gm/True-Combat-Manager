package com.muzlik.pvpcombat.data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import com.muzlik.pvpcombat.data.CombatEvent;
import com.muzlik.pvpcombat.data.CombatStatistics;
import com.muzlik.pvpcombat.data.DamageInfo;
import com.muzlik.pvpcombat.data.WeaponStats;

/**
 * Model class for storing player-specific combat data and statistics.
 */
public class PlayerCombatData {
    private final UUID playerId;
    private int totalCombats;
    private int wins;
    private int losses;
    private long totalCombatTime;
    private double totalDamageDealt;
    private double totalDamageReceived;
    private final Map<String, Integer> weaponUsage;
    private LocalDateTime lastCombat;
    private RestrictionData restrictionData;
    private List<CombatEvent> events = new ArrayList<>();
    private CombatStatistics stats = new CombatStatistics(null);
    private long lastActivity = System.currentTimeMillis();
    
    // Enhanced tracking fields
    private int criticalHits;
    private int longestCombo;
    private double highestDamageInSession;
    private final Map<String, WeaponStats> weaponStats;

    public PlayerCombatData(UUID playerId) {
        this.playerId = playerId;
        this.weaponUsage = new HashMap<>();
        this.weaponStats = new HashMap<>();
        this.lastCombat = LocalDateTime.now();
        this.restrictionData = new RestrictionData(playerId);
        this.criticalHits = 0;
        this.longestCombo = 0;
        this.highestDamageInSession = 0.0;
    }

    // Getters and setters
    public UUID getPlayerId() { return playerId; }

    public int getTotalCombats() { return totalCombats; }
    public void setTotalCombats(int totalCombats) { this.totalCombats = totalCombats; }
    public void incrementCombats() { this.totalCombats++; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public void incrementWins() { this.wins++; }

    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    public void incrementLosses() { this.losses++; }

    public long getTotalCombatTime() { return totalCombatTime; }
    public void setTotalCombatTime(long totalCombatTime) { this.totalCombatTime = totalCombatTime; }
    public void addCombatTime(long time) { this.totalCombatTime += time; }

    public double getTotalDamageDealt() { return totalDamageDealt; }
    public void setTotalDamageDealt(double totalDamageDealt) { this.totalDamageDealt = totalDamageDealt; }
    public void addDamageDealt(double damage) { this.totalDamageDealt += damage; }

    public double getTotalDamageReceived() { return totalDamageReceived; }
    public void setTotalDamageReceived(double totalDamageReceived) { this.totalDamageReceived = totalDamageReceived; }
    public void addDamageReceived(double damage) { this.totalDamageReceived += damage; }

    public Map<String, Integer> getWeaponUsage() { return weaponUsage; }
    public void incrementWeaponUsage(String weaponType) {
        weaponUsage.put(weaponType, weaponUsage.getOrDefault(weaponType, 0) + 1);
    }

    public LocalDateTime getLastCombat() { return lastCombat; }
    public void setLastCombat(LocalDateTime lastCombat) { this.lastCombat = lastCombat; }

    public RestrictionData getRestrictionData() { return restrictionData; }
    public void setRestrictionData(RestrictionData restrictionData) { this.restrictionData = restrictionData; }

    public List<CombatEvent> getEvents() { return events; }

    public CombatStatistics getStats() { return stats; }

    public long getLastActivity() { return lastActivity; }

    public void updateLastActivity(long time) { this.lastActivity = time; }

    public void clearRestrictions() {
        if (restrictionData != null) {
            restrictionData.clearAllRestrictions();
        }
    }
    
    // Enhanced tracking getters and setters
    public int getCriticalHits() { return criticalHits; }
    public void setCriticalHits(int criticalHits) { this.criticalHits = criticalHits; }
    public void incrementCriticalHits() { this.criticalHits++; }
    
    public int getLongestCombo() { return longestCombo; }
    public void setLongestCombo(int longestCombo) { this.longestCombo = longestCombo; }
    public void updateLongestCombo(int combo) {
        if (combo > this.longestCombo) {
            this.longestCombo = combo;
        }
    }
    
    public double getHighestDamageInSession() { return highestDamageInSession; }
    public void setHighestDamageInSession(double highestDamageInSession) { 
        this.highestDamageInSession = highestDamageInSession; 
    }
    public void updateHighestDamage(double damage) {
        if (damage > this.highestDamageInSession) {
            this.highestDamageInSession = damage;
        }
    }
    
    // Weapon stats methods
    public Map<String, WeaponStats> getWeaponStats() {
        return weaponStats;
    }
    
    public WeaponStats getWeaponStats(String weaponMaterial) {
        return weaponStats.computeIfAbsent(weaponMaterial, WeaponStats::new);
    }
    
    public void recordDamageWithWeapon(DamageInfo damageInfo) {
        String material = damageInfo.getWeaponMaterial() != null ? damageInfo.getWeaponMaterial().name() : "AIR";
        WeaponStats stats = getWeaponStats(material);
        stats.recordHit(damageInfo.getAmount(), damageInfo.isCritical());
        
        if (damageInfo.isCritical()) {
            incrementCriticalHits();
        }
        
        addDamageDealt(damageInfo.getAmount());
        updateHighestDamage(damageInfo.getAmount());
    }
    
    public void recordKillWithWeapon(String weaponMaterial) {
        WeaponStats stats = getWeaponStats(weaponMaterial);
        stats.recordKill();
    }
    
    /**
     * Calculates and returns the K/D ratio.
     */
    public double getKDRatio() {
        if (losses == 0) {
            return wins;
        }
        return (double) wins / losses;
    }
    
    /**
     * Calculates and returns the win rate percentage.
     */
    public double getWinRate() {
        if (totalCombats == 0) {
            return 0.0;
        }
        return ((double) wins / totalCombats) * 100.0;
    }
    
    /**
     * Calculates and returns the damage ratio (dealt/received).
     */
    public double getDamageRatio() {
        if (totalDamageReceived == 0) {
            return totalDamageDealt > 0 ? totalDamageDealt : 0.0;
        }
        return totalDamageDealt / totalDamageReceived;
    }
}