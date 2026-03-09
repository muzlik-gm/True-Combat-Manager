package com.muzlik.pvpcombat.protection;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles newbie protection system.
 * Protects players without armor from PvP combat.
 * Also provides timed protection for new players.
 */
public class NewbieProtection {

    private final PvPCombatPlugin plugin;
    private final Map<UUID, Long> timedProtection; // UUID -> expiration timestamp
    private final Map<UUID, BukkitTask> protectionTasks; // UUID -> countdown task

    public NewbieProtection(PvPCombatPlugin plugin) {
        this.plugin = plugin;
        this.timedProtection = new ConcurrentHashMap<>();
        this.protectionTasks = new ConcurrentHashMap<>();
    }

    /**
     * Checks if newbie protection is enabled.
     */
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("newbie-protection.enabled", true);
    }

    /**
     * Checks if a player is considered a newbie (no armor and low XP OR has timed protection).
     */
    public boolean isNewbie(Player player) {
        // Don't check if protection is disabled
        if (!isEnabled()) {
            return false;
        }
        
        // Check if player has bypass permission
        if (player.hasPermission("pvpcombat.bypass.newbie")) {
            if (plugin.getConfig().getBoolean("logging.console-enabled", false)) {
                plugin.getLoggingManager().log("[NEWBIE CHECK] " + player.getName() + " has bypass permission");
            }
            return false;
        }

        // Check if player has active timed protection
        if (hasTimedProtection(player)) {
            if (plugin.getConfig().getBoolean("logging.console-enabled", false)) {
                plugin.getLoggingManager().log("[NEWBIE CHECK] " + player.getName() + " has timed protection");
            }
            return true;
        }

        // Check XP level threshold
        int xpThreshold = plugin.getConfig().getInt("newbie-protection.xp-level-threshold", 3);
        int playerLevel = player.getLevel();
        
        if (plugin.getConfig().getBoolean("logging.console-enabled", false)) {
            plugin.getLoggingManager().log("[NEWBIE CHECK] " + player.getName() + " - Level: " + playerLevel + ", Threshold: " + xpThreshold);
        }
        
        if (playerLevel > xpThreshold) {
            if (plugin.getConfig().getBoolean("logging.console-enabled", false)) {
                plugin.getLoggingManager().log("[NEWBIE CHECK] " + player.getName() + " has too much XP, not a newbie");
            }
            return false; // Player has enough XP, not a newbie
        }

        // Check if player has armor
        boolean hasArmorEquipped = hasArmor(player);
        if (plugin.getConfig().getBoolean("logging.console-enabled", false)) {
            plugin.getLoggingManager().log("[NEWBIE CHECK] " + player.getName() + " has armor: " + hasArmorEquipped);
        }
        
        boolean isNewbie = !hasArmorEquipped;
        if (plugin.getConfig().getBoolean("logging.console-enabled", false)) {
            plugin.getLoggingManager().log("[NEWBIE CHECK] " + player.getName() + " IS NEWBIE: " + isNewbie);
        }
        
        return isNewbie;
    }

    /**
     * Checks if a player has armor equipped.
     */
    private boolean hasArmor(Player player) {
        boolean requireAnyArmor = plugin.getConfig().getBoolean("newbie-protection.require-any-armor", true);

        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();
        
        // Check if armor pieces are not null AND not AIR
        boolean hasHelmet = helmet != null && helmet.getType() != org.bukkit.Material.AIR;
        boolean hasChestplate = chestplate != null && chestplate.getType() != org.bukkit.Material.AIR;
        boolean hasLeggings = leggings != null && leggings.getType() != org.bukkit.Material.AIR;
        boolean hasBoots = boots != null && boots.getType() != org.bukkit.Material.AIR;
        
        if (plugin.getConfig().getBoolean("logging.console-enabled", false)) {
            plugin.getLoggingManager().log("[ARMOR CHECK] " + player.getName() + " - Helmet:" + hasHelmet + " Chest:" + hasChestplate + " Legs:" + hasLeggings + " Boots:" + hasBoots);
        }

        if (requireAnyArmor) {
            // Player needs at least ONE armor piece
            return hasHelmet || hasChestplate || hasLeggings || hasBoots;
        } else {
            // Player needs FULL armor set
            return hasHelmet && hasChestplate && hasLeggings && hasBoots;
        }
    }

    /**
     * Checks if a newbie can deal damage.
     * Returns FALSE if newbie should be blocked from dealing damage.
     */
    public boolean canNewbieDealDamage(Player newbie) {
        if (!isEnabled()) {
            return true; // Protection disabled, allow damage
        }

        if (!isNewbie(newbie)) {
            return true; // Not a newbie, allow damage
        }

        // If prevent-damage-dealing is TRUE, newbie CANNOT deal damage (return FALSE)
        // If prevent-damage-dealing is FALSE, newbie CAN deal damage (return TRUE)
        boolean preventDealing = plugin.getConfig().getBoolean("newbie-protection.prevent-damage-dealing", true);
        boolean canDealDamage = !preventDealing;
        
        if (plugin.getConfig().getBoolean("logging.console-enabled", false)) {
            plugin.getLoggingManager().log("[NEWBIE DAMAGE] " + newbie.getName() + " prevent-dealing=" + preventDealing + ", can deal damage=" + canDealDamage);
        }
        
        return canDealDamage; // Invert: if prevent=true, return false (block damage)
    }

    /**
     * Checks if a newbie can receive damage.
     * Returns FALSE if newbie should be protected from receiving damage.
     */
    public boolean canNewbieReceiveDamage(Player newbie) {
        if (!isEnabled()) {
            return true; // Protection disabled, allow damage
        }

        if (!isNewbie(newbie)) {
            return true; // Not a newbie, allow damage
        }

        // If prevent-damage-receiving is TRUE, newbie CANNOT receive damage (return FALSE)
        // If prevent-damage-receiving is FALSE, newbie CAN receive damage (return TRUE)
        boolean preventReceiving = plugin.getConfig().getBoolean("newbie-protection.prevent-damage-receiving", true);
        return !preventReceiving; // Invert: if prevent=true, return false (block damage)
    }

    /**
     * Gets the message to send to a newbie trying to attack.
     */
    public String getNewbieAttackMessage() {
        return plugin.getConfig().getString("newbie-protection.newbie-attack-message",
            "&cYou need armor to attack other players!")
            .replace("&", "§");
    }

    /**
     * Gets the message to send when attacking a newbie.
     */
    public String getAttackingNewbieMessage() {
        return plugin.getConfig().getString("newbie-protection.attacking-newbie-message",
            "&cYou cannot attack players without armor!")
            .replace("&", "§");
    }

    /**
     * Checks if a player has active timed protection.
     */
    public boolean hasTimedProtection(Player player) {
        Long expirationTime = timedProtection.get(player.getUniqueId());
        if (expirationTime == null) {
            return false;
        }
        
        // Check if protection has expired
        if (System.currentTimeMillis() >= expirationTime) {
            removeTimedProtection(player.getUniqueId());
            return false;
        }
        
        return true;
    }

    /**
     * Gets remaining timed protection in seconds.
     */
    public int getRemainingProtectionTime(Player player) {
        Long expirationTime = timedProtection.get(player.getUniqueId());
        if (expirationTime == null) {
            return 0;
        }
        
        long remaining = (expirationTime - System.currentTimeMillis()) / 1000;
        return (int) Math.max(0, remaining);
    }

    /**
     * Gives timed protection to a player.
     * @param player The player to protect
     * @param seconds Duration in seconds
     */
    public void giveTimedProtection(Player player, int seconds) {
        // Remove existing protection if any
        removeTimedProtection(player.getUniqueId());
        
        // Calculate expiration time
        long expirationTime = System.currentTimeMillis() + (seconds * 1000L);
        timedProtection.put(player.getUniqueId(), expirationTime);
        
        // Start countdown task
        BukkitTask task = new BukkitRunnable() {
            int remaining = seconds;
            
            @Override
            public void run() {
                if (remaining <= 0 || !player.isOnline()) {
                    removeTimedProtection(player.getUniqueId());
                    this.cancel();
                    return;
                }
                
                // Send reminders at specific intervals
                if (remaining == 60 || remaining == 30 || remaining == 10 || remaining <= 5) {
                    String message = plugin.getConfig().getString("newbie-protection.timed-protection-reminder",
                        "&aYou have &e{time}s &aof newbie protection remaining!")
                        .replace("&", "§")
                        .replace("{time}", String.valueOf(remaining));
                    player.sendMessage(message);
                }
                
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second
        
        protectionTasks.put(player.getUniqueId(), task);
        
        // Send initial message
        String message = plugin.getConfig().getString("newbie-protection.timed-protection-granted",
            "&aYou have been granted &e{time}s &aof newbie protection!")
            .replace("&", "§")
            .replace("{time}", String.valueOf(seconds));
        player.sendMessage(message);
        
        if (plugin.getConfig().getBoolean("logging.console-enabled", false)) {
            plugin.getLoggingManager().log("[TIMED PROTECTION] Granted " + seconds + "s protection to " + player.getName());
        }
    }

    /**
     * Removes timed protection from a player.
     */
    public void removeTimedProtection(UUID playerId) {
        timedProtection.remove(playerId);
        
        BukkitTask task = protectionTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null && player.isOnline()) {
            String message = plugin.getConfig().getString("newbie-protection.timed-protection-expired",
                "&cYour newbie protection has expired!")
                .replace("&", "§");
            player.sendMessage(message);
        }
    }

    /**
     * Handles player join for automatic timed protection.
     */
    public void onPlayerJoin(Player player) {
        // Check if timed protection on join is enabled
        if (!plugin.getConfig().getBoolean("newbie-protection.timed-protection-on-join.enabled", false)) {
            return;
        }
        
        // Check if player has bypass permission
        if (player.hasPermission("pvpcombat.bypass.newbie")) {
            return;
        }
        
        // Check if player has played before
        if (player.hasPlayedBefore()) {
            return;
        }
        
        // Give timed protection
        int duration = plugin.getConfig().getInt("newbie-protection.timed-protection-on-join.duration-seconds", 900); // Default 15 minutes
        giveTimedProtection(player, duration);
    }

    /**
     * Cleans up protection data for offline players.
     */
    public void cleanup() {
        timedProtection.entrySet().removeIf(entry -> {
            if (System.currentTimeMillis() >= entry.getValue()) {
                BukkitTask task = protectionTasks.remove(entry.getKey());
                if (task != null) {
                    task.cancel();
                }
                return true;
            }
            return false;
        });
    }
}
