package com.muzlik.pvpcombat.restrictions;

import com.muzlik.pvpcombat.combat.CombatManager;
import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import com.muzlik.pvpcombat.data.RestrictionData;
import com.muzlik.pvpcombat.interfaces.IRestrictionManager;
import com.muzlik.pvpcombat.utils.CacheManager;
import org.bukkit.entity.Player;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Main restriction manager implementing IRestrictionManager interface.
 * Handles all movement and ability restrictions during combat.
 */
public class RestrictionManager implements IRestrictionManager {

    private final CombatManager combatManager;
    private final EnderPearlRestriction enderPearlRestriction;
    private final ElytraRestriction elytraRestriction;
    private final GoldenAppleRestriction goldenAppleRestriction;
    private final TridentRestriction tridentRestriction;
    private final CrystalRestriction crystalRestriction;
    private final Map<Player, RestrictionData> playerRestrictions;
    private final CacheManager cacheManager;

    public RestrictionManager(CombatManager combatManager, CacheManager cacheManager) {
        this.combatManager = combatManager;
        this.cacheManager = cacheManager;
        this.enderPearlRestriction = new EnderPearlRestriction(this);
        this.elytraRestriction = new ElytraRestriction(this);
        this.goldenAppleRestriction = new GoldenAppleRestriction(this);
        this.tridentRestriction = new TridentRestriction(this);
        this.crystalRestriction = new CrystalRestriction(this);
        this.playerRestrictions = new ConcurrentHashMap<>();
    }

    @Override
    public boolean canUseEnderPearl(Player player) {
        if (!combatManager.isInCombat(player)) {
            return true;
        }

        // Check cache first
        String cacheKey = player.getUniqueId() + ":enderpearl";
        Boolean cached = (Boolean) cacheManager.get("restriction-data", cacheKey);
        if (cached != null) {
            return cached;
        }

        RestrictionData restrictionData = getOrCreateRestrictionData(player);
        boolean canUse = enderPearlRestriction.canUse(player, restrictionData);

        // Cache the result for short time
        cacheManager.put("restriction-data", cacheKey, canUse);
        return canUse;
    }

    @Override
    public boolean canUseElytra(Player player) {
        if (!combatManager.isInCombat(player)) {
            return true;
        }

        // Check cache first
        String cacheKey = player.getUniqueId() + ":elytra";
        Boolean cached = (Boolean) cacheManager.get("restriction-data", cacheKey);
        if (cached != null) {
            return cached;
        }

        RestrictionData restrictionData = getOrCreateRestrictionData(player);
        boolean canUse = elytraRestriction.canUse(player, restrictionData);

        // Cache the result for short time
        cacheManager.put("restriction-data", cacheKey, canUse);
        return canUse;
    }

    @Override
    public boolean canBreakBlocks(Player player) {
        if (!combatManager.isInCombat(player)) {
            return true;
        }
        // Check if block restrictions are enabled
        boolean restrictionsEnabled = PvPCombatPlugin.getInstance().getConfig().getBoolean("restrictions.blocks.enabled", false);
        if (!restrictionsEnabled) {
            return true;
        }
        // Block breaking restrictions
        return !PvPCombatPlugin.getInstance().getConfig().getBoolean("restrictions.blocks.block-breaking", false);
    }

    @Override
    public boolean canPlaceBlocks(Player player) {
        if (!combatManager.isInCombat(player)) {
            return true;
        }
        // Check if block restrictions are enabled
        boolean restrictionsEnabled = PvPCombatPlugin.getInstance().getConfig().getBoolean("restrictions.blocks.enabled", false);
        if (!restrictionsEnabled) {
            return true;
        }
        // Block placing restrictions
        return !PvPCombatPlugin.getInstance().getConfig().getBoolean("restrictions.blocks.block-placing", false);
    }
    public boolean canTeleport(Player player) {
        if (!combatManager.isInCombat(player)) {
            return true;
        }
        // Teleport restrictions.
        return !PvPCombatPlugin.getInstance().getConfig().getBoolean("restrictions.teleport", true);
    }

    @Override
    public void applyCooldown(Player player, String actionType, int cooldownSeconds) {
        RestrictionData restrictionData = getOrCreateRestrictionData(player);
        restrictionData.setCooldown(actionType, cooldownSeconds);
    }

    /**
     * Gets or creates restriction data for a player.
     */
    public RestrictionData getOrCreateRestrictionData(Player player) {
        return playerRestrictions.computeIfAbsent(player, p -> new RestrictionData(p.getUniqueId()));
    }

    /**
     * Removes restriction data for a player (when they leave combat).
     */
    public void removeRestrictionData(Player player) {
        RestrictionData removed = playerRestrictions.remove(player);
        if (removed != null) {
            removed.clearAllRestrictions();
        }
    }

    /**
     * Clears all restrictions for a player.
     */
    public void clearRestrictions(Player player) {
        RestrictionData restrictionData = playerRestrictions.get(player);
        if (restrictionData != null) {
            restrictionData.clearAllRestrictions();
        }
    }

    @Override
    public boolean canUseGoldenApple(Player player) {
        if (!combatManager.isInCombat(player)) {
            return true;
        }

        String cacheKey = player.getUniqueId() + ":goldenapple";
        Boolean cached = (Boolean) cacheManager.get("restriction-data", cacheKey);
        if (cached != null) {
            return cached;
        }

        boolean canUse = goldenAppleRestriction.canUseGoldenApple(player);
        cacheManager.put("restriction-data", cacheKey, canUse);
        return canUse;
    }

    @Override
    public boolean canUseEnchantedGoldenApple(Player player) {
        if (!combatManager.isInCombat(player)) {
            return true;
        }

        String cacheKey = player.getUniqueId() + ":enchantedgoldenapple";
        Boolean cached = (Boolean) cacheManager.get("restriction-data", cacheKey);
        if (cached != null) {
            return cached;
        }

        boolean canUse = goldenAppleRestriction.canUseEnchantedGoldenApple(player);
        cacheManager.put("restriction-data", cacheKey, canUse);
        return canUse;
    }

    // Getters for specific restrictions
    public EnderPearlRestriction getEnderPearlRestriction() { return enderPearlRestriction; }
    public ElytraRestriction getElytraRestriction() { return elytraRestriction; }
    public GoldenAppleRestriction getGoldenAppleRestriction() { return goldenAppleRestriction; }
    public TridentRestriction getTridentRestriction() { return tridentRestriction; }
    public CrystalRestriction getCrystalRestriction() { return crystalRestriction; }
    public CombatManager getCombatManager() { return combatManager; }

    /**
     * Reloads configuration for all restrictions.
     */
    public void reloadConfig() {
        // Clear cache to force re-reading config values
        cacheManager.clear("restriction-data");
        PvPCombatPlugin.getInstance().getLogger().info("RestrictionManager configuration reloaded");
    }
}