package com.muzlik.pvpcombat.data;

import org.bukkit.Material;

/**
 * Detailed information about a damage event.
 * Tracks weapon type, critical hits, distance, and timing.
 */
public class DamageInfo {
    
    private final double amount;
    private final WeaponType weaponType;
    private final boolean isCritical;
    private final double distance;
    private final long timestamp;
    private final Material weaponMaterial;
    
    public DamageInfo(double amount, WeaponType weaponType, boolean isCritical, 
                     double distance, Material weaponMaterial) {
        this.amount = amount;
        this.weaponType = weaponType;
        this.isCritical = isCritical;
        this.distance = distance;
        this.weaponMaterial = weaponMaterial;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Simple constructor for basic damage
    public DamageInfo(double amount) {
        this(amount, WeaponType.UNKNOWN, false, 0.0, Material.AIR);
    }
    
    public double getAmount() {
        return amount;
    }
    
    public WeaponType getWeaponType() {
        return weaponType;
    }
    
    public boolean isCritical() {
        return isCritical;
    }
    
    public double getDistance() {
        return distance;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public Material getWeaponMaterial() {
        return weaponMaterial;
    }
    
    /**
     * Check if this is a melee attack (distance < 4 blocks).
     */
    public boolean isMelee() {
        return distance < 4.0;
    }
    
    /**
     * Check if this is a ranged attack (distance >= 4 blocks).
     */
    public boolean isRanged() {
        return distance >= 4.0;
    }
    
    /**
     * Weapon type enumeration.
     */
    public enum WeaponType {
        SWORD,
        AXE,
        BOW,
        CROSSBOW,
        TRIDENT,
        FIST,
        OTHER,
        UNKNOWN;
        
        /**
         * Determine weapon type from material.
         */
        public static WeaponType fromMaterial(Material material) {
            if (material == null) {
                return UNKNOWN;
            }
            
            String name = material.name();
            
            if (name.endsWith("_SWORD")) {
                return SWORD;
            } else if (name.endsWith("_AXE")) {
                return AXE;
            } else if (name.equals("BOW")) {
                return BOW;
            } else if (name.equals("CROSSBOW")) {
                return CROSSBOW;
            } else if (name.equals("TRIDENT")) {
                return TRIDENT;
            } else if (name.equals("AIR")) {
                return FIST;
            } else {
                return OTHER;
            }
        }
    }
    
    @Override
    public String toString() {
        return String.format("DamageInfo{amount=%.2f, weapon=%s, critical=%b, distance=%.2f, time=%d}",
                           amount, weaponType, isCritical, distance, timestamp);
    }
}
