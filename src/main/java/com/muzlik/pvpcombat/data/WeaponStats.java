package com.muzlik.pvpcombat.data;

import com.muzlik.pvpcombat.data.DamageInfo.WeaponType;

/**
 * Statistics for a specific weapon type.
 * Tracks usage, damage, and kills.
 */
public class WeaponStats {
    
    private final WeaponType type;
    private int uses;
    private double totalDamage;
    private int kills;
    private int criticalHits;
    
    public WeaponStats(WeaponType type) {
        this.type = type;
        this.uses = 0;
        this.totalDamage = 0.0;
        this.kills = 0;
        this.criticalHits = 0;
    }
    
    /**
     * Record a hit with this weapon.
     */
    public void recordHit(double damage, boolean isCritical) {
        uses++;
        totalDamage += damage;
        if (isCritical) {
            criticalHits++;
        }
    }
    
    /**
     * Record a kill with this weapon.
     */
    public void recordKill() {
        kills++;
    }
    
    /**
     * Get average damage per hit.
     */
    public double getAverageDamage() {
        return uses > 0 ? totalDamage / uses : 0.0;
    }
    
    /**
     * Get critical hit rate as a percentage.
     */
    public double getCriticalRate() {
        return uses > 0 ? (criticalHits * 100.0) / uses : 0.0;
    }
    
    // Getters
    public WeaponType getType() {
        return type;
    }
    
    public int getUses() {
        return uses;
    }
    
    public double getTotalDamage() {
        return totalDamage;
    }
    
    public int getKills() {
        return kills;
    }
    
    public int getCriticalHits() {
        return criticalHits;
    }
    
    // Setters for loading from database
    public void setUses(int uses) {
        this.uses = uses;
    }
    
    public void setTotalDamage(double totalDamage) {
        this.totalDamage = totalDamage;
    }
    
    public void setKills(int kills) {
        this.kills = kills;
    }
    
    public void setCriticalHits(int criticalHits) {
        this.criticalHits = criticalHits;
    }
    
    @Override
    public String toString() {
        return String.format("WeaponStats{type=%s, uses=%d, damage=%.2f, kills=%d, crits=%d}",
                           type, uses, totalDamage, kills, criticalHits);
    }
}
