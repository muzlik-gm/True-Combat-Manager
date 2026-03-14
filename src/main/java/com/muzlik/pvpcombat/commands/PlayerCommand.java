package com.muzlik.pvpcombat.commands;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import com.muzlik.pvpcombat.combat.CombatManager;
import com.muzlik.pvpcombat.data.CombatSession;
import com.muzlik.pvpcombat.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.attribute.Attribute;

/**
 * Handles player-facing combat commands with enhanced error handling and user feedback.
 *
 * @author PvPCombat Plugin Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class PlayerCommand {

    private final PvPCombatPlugin plugin;

    /**
     * Constructs a new PlayerCommand handler.
     *
     * @param plugin The main plugin instance
     * @throws IllegalArgumentException if plugin is null
     */
    public PlayerCommand(PvPCombatPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        this.plugin = plugin;
    }

    /**
     * Handles player command execution with comprehensive error handling.
     *
     * @param player The player executing the command
     * @param args The command arguments
     * @return true if command was handled, false if usage should be shown
     * @throws IllegalStateException if plugin components are not properly initialized
     */
    public boolean handleCommand(Player player, String[] args) {
        try {
            if (args.length < 1) {
                return false; // Show usage
            }

            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "status":
                    return handleStatusCommand(player);
                case "summary":
                    return handleSummaryCommand(player);
                case "toggle-style":
                    return handleToggleStyleCommand(player);
                default:
                    return false; // Unknown subcommand
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling player command: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cAn error occurred while processing your command. Please try again.");
            return true;
        }
    }

    /**
     * Shows player's combat state with enhanced error handling and detailed information.
     *
     * @param player The player to show status for
     * @return true if command executed successfully
     */
    private boolean handleStatusCommand(Player player) {
        try {
            if (plugin.getCombatManager() == null) {
                player.sendMessage("§cCombat system is not available. Please contact an administrator.");
                return true;
            }

            CombatManager combatManager = (CombatManager) plugin.getCombatManager();
            if (!combatManager.isInCombat(player)) {
                player.sendMessage("§aYou are not in combat.");
                player.sendMessage("§7You can engage in PvP combat or wait for the combat timer to expire.");
                return true;
            }

            CombatSession session = combatManager.getActiveSessions().get(player.getUniqueId());
            if (session == null) {
                player.sendMessage("§cCould not retrieve combat session data.");
                return true;
            }

            Player opponent = session.getOpponent(player);

            // Display combat status header
            player.sendMessage("§6=== Combat Status ===");
            player.sendMessage("§eStatus: §cIn Combat");

            if (opponent != null) {
                player.sendMessage("§eOpponent: §f" + opponent.getName());

                // Show opponent health with progress bar
                double opponentHealth = opponent.getHealth();
                double opponentMaxHealth = ((LivingEntity) opponent).getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                double opponentHealthProgress = opponentHealth / opponentMaxHealth;
                String opponentHealthBar = MessageUtils.createProgressBar(opponentHealthProgress, 10);
                player.sendMessage(String.format("§eOpponent Health: §f%s §7(%.1f/%.1f)", opponentHealthBar, opponentHealth, opponentMaxHealth));
            } else {
                player.sendMessage("§eOpponent: §fUnknown");
            }

            // Show player's own health with progress bar
            double playerHealth = player.getHealth();
            double playerMaxHealth = ((LivingEntity) player).getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double playerHealthProgress = playerHealth / playerMaxHealth;
            String playerHealthBar = MessageUtils.createProgressBar(playerHealthProgress, 10);
            player.sendMessage(String.format("§eYour Health: §f%s §7(%.1f/%.1f)", playerHealthBar, playerHealth, playerMaxHealth));

            // Show combat timer with progress bar
            int timeLeft = session.getRemainingTime();
            double timerProgress = session.getTimerData().getProgress();
            String timerBar = MessageUtils.createProgressBar(timerProgress, 10);
            player.sendMessage(String.format("§eTime Remaining: §f%s §f%ds", timerBar, timeLeft));

            player.sendMessage("§7§oUse /combat summary to view detailed fight statistics.");

            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Error showing combat status: " + e.getMessage());
            player.sendMessage("§cFailed to show combat status. The system may be temporarily unavailable.");
            return true;
        }
    }

    /**
     * Formats time in MM:SS format with null safety.
     *
     * @param milliseconds The time in milliseconds to format
     * @return Formatted time string, or "Unknown" if invalid
     */
    private String formatTime(long milliseconds) {
        try {
            if (milliseconds < 0) {
                return "Unknown";
            }
            long seconds = milliseconds / 1000;
            long minutes = seconds / 60;
            seconds %= 60;
            return String.format("%02d:%02d", minutes, seconds);
        } catch (Exception e) {
            plugin.getLogger().warning("Error formatting time: " + e.getMessage());
            return "Unknown";
        }
    }

    /**
     * Handles the summary subcommand.
     */
    private boolean handleSummaryCommand(Player player) {
        try {
            // Get player's combat data
            com.muzlik.pvpcombat.data.PlayerCombatData combatData = 
                ((com.muzlik.pvpcombat.combat.CombatManager) plugin.getCombatManager())
                    .getCombatTracker().getPlayerData(player.getUniqueId());

            if (combatData == null) {
                player.sendMessage("§cNo combat data found. Engage in combat first!");
                return true;
            }

            // Check if player has any combat data
            if (combatData.getTotalCombats() == 0 && combatData.getTotalDamageDealt() == 0) {
                player.sendMessage("§eNo combat statistics yet. Fight someone to generate data!");
                return true;
            }

            // Display summary
            player.sendMessage("§6=== Combat Summary for " + player.getName() + " ===");
            player.sendMessage(String.format("§eTotal Combats: §f%d", combatData.getTotalCombats()));
            
            if (combatData.getTotalCombats() > 0) {
                player.sendMessage(String.format("§eWins: §a%d §7| §eLosses: §c%d", 
                    combatData.getWins(), combatData.getLosses()));
                
                // Calculate win rate
                double winRate = (double) combatData.getWins() / combatData.getTotalCombats() * 100.0;
                player.sendMessage(String.format("§eWin Rate: §f%.1f%%", winRate));
                
                // Calculate K/D ratio
                double kdRatio = combatData.getLosses() > 0 ? 
                    (double) combatData.getWins() / combatData.getLosses() : combatData.getWins();
                player.sendMessage(String.format("§eK/D Ratio: §f%.2f", kdRatio));
            }
            
            player.sendMessage(String.format("§eDamage Dealt: §c%.1f ❤", combatData.getTotalDamageDealt()));
            player.sendMessage(String.format("§eDamage Received: §c%.1f ❤", combatData.getTotalDamageReceived()));
            
            // Calculate damage ratio
            if (combatData.getTotalDamageReceived() > 0) {
                double damageRatio = combatData.getTotalDamageDealt() / combatData.getTotalDamageReceived();
                player.sendMessage(String.format("§eDamage Ratio: §f%.2f", damageRatio));
            }
            
            // Show combat time
            if (combatData.getTotalCombatTime() > 0) {
                long totalMinutes = combatData.getTotalCombatTime() / 60000;
                long totalSeconds = (combatData.getTotalCombatTime() % 60000) / 1000;
                player.sendMessage(String.format("§eTotal Combat Time: §f%dm %ds", totalMinutes, totalSeconds));
            }
            
            // Show last combat time
            if (combatData.getLastCombat() != null) {
                player.sendMessage("§7Last Combat: §f" + combatData.getLastCombat().toString());
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error showing combat summary: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cFailed to show combat summary.");
            return true;
        }
    }

    /**
     * Handles the toggle-style subcommand.
     */
    private boolean handleToggleStyleCommand(Player player) {
        try {
            // Get available themes from config
            java.util.List<String> availableThemes = plugin.getConfig().getStringList("visual.themes.available");
            
            if (availableThemes.isEmpty()) {
                availableThemes = java.util.Arrays.asList("minimal", "fire", "ice", "neon", "dark", "clean");
            }

            // Check if player is in combat
            if (!plugin.getCombatManager().isInCombat(player)) {
                player.sendMessage("§eYou are not in combat. Theme will apply when you enter combat.");
                player.sendMessage("§7Available styles: §f" + String.join(", ", availableThemes));
                
                // Show theme preview
                String defaultTheme = plugin.getConfig().getString("visual.themes.default-theme", "clean");
                int currentIndex = availableThemes.indexOf(defaultTheme);
                if (currentIndex == -1) currentIndex = 0;
                int nextIndex = (currentIndex + 1) % availableThemes.size();
                String nextTheme = availableThemes.get(nextIndex);
                
                // Update default theme in config for next combat
                plugin.getConfig().set("visual.themes.default-theme", nextTheme);
                
                player.sendMessage("§aNext combat will use theme: §e" + nextTheme);
                return true;
            }

            // Get player's current session
            com.muzlik.pvpcombat.data.CombatSession session = null;
            for (com.muzlik.pvpcombat.data.CombatSession s : ((com.muzlik.pvpcombat.combat.CombatManager) plugin.getCombatManager()).getActiveSessions().values()) {
                if (s.involvesPlayer(player)) {
                    session = s;
                    break;
                }
            }
            
            if (session == null) {
                player.sendMessage("§cCould not find your combat session!");
                return true;
            }

            // Get current theme from session
            String currentTheme = session.getCurrentTheme();
            if (currentTheme == null || currentTheme.isEmpty()) {
                currentTheme = plugin.getConfig().getString("visual.themes.default-theme", "clean");
            }
            
            // Find next theme in the list
            int currentIndex = availableThemes.indexOf(currentTheme);
            if (currentIndex == -1) {
                currentIndex = 0; // Default to first theme if current not found
            }
            int nextIndex = (currentIndex + 1) % availableThemes.size();
            String nextTheme = availableThemes.get(nextIndex);

            // Apply the new theme
            com.muzlik.pvpcombat.visual.BossBarManager bossBarManager = 
                ((com.muzlik.pvpcombat.combat.CombatManager) plugin.getCombatManager()).getVisualManager().getBossBarManager();
            bossBarManager.applyTheme(session.getSessionId().toString(), nextTheme, true);
            session.setCurrentTheme(nextTheme);

            // Get theme details to show what changed
            com.muzlik.pvpcombat.visual.ThemeManager.Theme theme = 
                ((com.muzlik.pvpcombat.combat.CombatManager) plugin.getCombatManager()).getVisualManager()
                    .getThemeManager().getTheme(nextTheme);
            
            // Send confirmation message with theme details
            player.sendMessage("§6=== Theme Changed ===");
            player.sendMessage("§aNew Theme: §e" + nextTheme);
            if (theme != null) {
                player.sendMessage("§7Color: §f" + theme.getBossBarColor().name());
                player.sendMessage("§7Style: §f" + theme.getBossBarStyle().name());
            }
            player.sendMessage("§7Available: §f" + String.join(", ", availableThemes));
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error toggling style: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cFailed to toggle style.");
            return true;
        }
    }
}
