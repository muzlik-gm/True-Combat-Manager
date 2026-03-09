package com.muzlik.pvpcombat.integration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * WorldGuard integration without reflection.
 * Uses direct WorldGuard API with caching for performance.
 */
public class WorldGuardIntegration {
    
    private final Plugin plugin;
    private final Logger logger;
    private final boolean worldGuardAvailable;
    private final RegionContainer regionContainer;
    
    // Cache for safe zone checks (5-second TTL)
    private final Cache<String, Boolean> safeZoneCache;
    
    // Safe zone region names from config
    private final Set<String> safeZoneRegions;
    
    public WorldGuardIntegration(Plugin plugin, Set<String> safeZoneRegions) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.safeZoneRegions = new HashSet<>(safeZoneRegions);
        
        // Check if WorldGuard is available
        boolean available = false;
        RegionContainer container = null;
        
        try {
            if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                available = true;
                logger.info("WorldGuard integration enabled");
            } else {
                logger.info("WorldGuard not found - safe zone features disabled");
            }
        } catch (Exception e) {
            logger.warning("Failed to initialize WorldGuard integration: " + e.getMessage());
        }
        
        this.worldGuardAvailable = available;
        this.regionContainer = container;
        
        // Initialize cache
        this.safeZoneCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .maximumSize(1000)
            .build();
    }
    
    /**
     * Check if WorldGuard is available and functional.
     */
    public boolean isAvailable() {
        return worldGuardAvailable;
    }
    
    /**
     * Check if a location is in a safe zone.
     * Uses caching for performance.
     */
    public boolean isInSafeZone(org.bukkit.Location location) {
        if (!worldGuardAvailable || safeZoneRegions.isEmpty()) {
            return false;
        }
        
        // Create cache key
        String cacheKey = location.getWorld().getName() + ":" + 
                         location.getBlockX() + ":" + 
                         location.getBlockY() + ":" + 
                         location.getBlockZ();
        
        // Check cache first
        Boolean cached = safeZoneCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Check WorldGuard regions
        boolean inSafeZone = checkSafeZone(location);
        
        // Cache result
        safeZoneCache.put(cacheKey, inSafeZone);
        
        return inSafeZone;
    }
    
    /**
     * Check if a player is in a safe zone.
     */
    public boolean isInSafeZone(Player player) {
        return isInSafeZone(player.getLocation());
    }
    
    /**
     * Check if a location is in a specific protected region.
     */
    public boolean isInProtectedRegion(org.bukkit.Location location, String regionName) {
        if (!worldGuardAvailable) {
            return false;
        }
        
        try {
            Location wgLocation = BukkitAdapter.adapt(location);
            RegionQuery query = regionContainer.createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(wgLocation);
            
            for (ProtectedRegion region : regions) {
                if (region.getId().equalsIgnoreCase(regionName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warning("Error checking protected region: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get all region names at a location.
     */
    public Set<String> getRegionsAt(org.bukkit.Location location) {
        Set<String> regionNames = new HashSet<>();
        
        if (!worldGuardAvailable) {
            return regionNames;
        }
        
        try {
            Location wgLocation = BukkitAdapter.adapt(location);
            RegionQuery query = regionContainer.createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(wgLocation);
            
            for (ProtectedRegion region : regions) {
                regionNames.add(region.getId());
            }
        } catch (Exception e) {
            logger.warning("Error getting regions: " + e.getMessage());
        }
        
        return regionNames;
    }
    
    /**
     * Invalidate the entire cache.
     */
    public void invalidateCache() {
        safeZoneCache.invalidateAll();
    }
    
    /**
     * Invalidate cache for a specific location.
     */
    public void invalidateCache(org.bukkit.Location location) {
        String cacheKey = location.getWorld().getName() + ":" + 
                         location.getBlockX() + ":" + 
                         location.getBlockY() + ":" + 
                         location.getBlockZ();
        safeZoneCache.invalidate(cacheKey);
    }
    
    /**
     * Add a safe zone region name.
     */
    public void addSafeZoneRegion(String regionName) {
        safeZoneRegions.add(regionName);
        invalidateCache();
    }
    
    /**
     * Remove a safe zone region name.
     */
    public void removeSafeZoneRegion(String regionName) {
        safeZoneRegions.remove(regionName);
        invalidateCache();
    }
    
    /**
     * Get all configured safe zone region names.
     */
    public Set<String> getSafeZoneRegions() {
        return new HashSet<>(safeZoneRegions);
    }
    
    /**
     * Internal method to check if location is in a safe zone.
     */
    private boolean checkSafeZone(org.bukkit.Location location) {
        try {
            Location wgLocation = BukkitAdapter.adapt(location);
            RegionQuery query = regionContainer.createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(wgLocation);
            
            for (ProtectedRegion region : regions) {
                if (safeZoneRegions.contains(region.getId())) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warning("Error checking safe zone: " + e.getMessage());
        }
        
        return false;
    }
}
