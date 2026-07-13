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
                case "stats":
                    return handleStatsCommand(player);
                case "toggle-sounds":
                    return handleToggleSoundsCommand(player);
                case "toggle-bossbar":
                    return handleToggleBossBarCommand(player);
                case "toggle-actionbar":
                    return handleToggleActionBarCommand(player);
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
            if (plugin.getGuiManager() != null) {
                plugin.getGuiManager().openMainStatsGUI(player);
                return true;
            }

            player.sendMessage("§cGUI system is not available.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error showing combat summary GUI: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cFailed to show combat summary.");
            return true;
        }
    }

    /**
     * Opens the player's own combat stats GUI.
     */
    private boolean handleStatsCommand(Player player) {
        if (plugin.getGuiManager() != null) {
            plugin.getGuiManager().openMainStatsGUI(player);
        } else {
            player.sendMessage("§cStats GUI is not available.");
        }
        return true;
    }

    /**
     * Handles the toggle-style subcommand.
     * Works both in and out of combat. The chosen theme is persisted per-player.
     */
    private boolean handleToggleStyleCommand(Player player) {
        try {
            com.muzlik.pvpcombat.visual.VisualManager vm = getVisualManager();
            if (vm == null) {
                player.sendMessage("§cVisual system is not available.");
                return true;
            }

            java.util.List<String> availableThemes = vm.getAvailableThemes();

            // Determine current theme for this player
            String currentTheme = vm.getPlayerTheme(player.getUniqueId());
            int currentIndex = availableThemes.indexOf(currentTheme);
            if (currentIndex == -1) currentIndex = 0;

            // Cycle to next theme
            int nextIndex = (currentIndex + 1) % availableThemes.size();
            String nextTheme = availableThemes.get(nextIndex);

            // Apply + persist via VisualManager
            boolean applied = vm.setPlayerTheme(player, nextTheme);
            if (!applied) {
                player.sendMessage("§cTheme '" + nextTheme + "' is not available.");
                return true;
            }

            // Get theme details for the confirmation message
            com.muzlik.pvpcombat.visual.ThemeManager.Theme theme = vm.getThemeManager().getTheme(nextTheme);

            boolean inCombat = plugin.getCombatManager() != null
                && plugin.getCombatManager().isInCombat(player);

            player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            player.sendMessage("§e§lTheme Changed");
            player.sendMessage("");
            player.sendMessage("§7Theme:  §f" + nextTheme);
            if (theme != null) {
                player.sendMessage("§7Color:  §f" + theme.getBossBarColor().name());
                player.sendMessage("§7Style:  §f" + theme.getBossBarStyle().name());
                player.sendMessage("§7Sound:  §f" + theme.getSoundProfile());
            }
            player.sendMessage("");
            if (inCombat) {
                player.sendMessage("§a✓ Applied to your active combat session.");
            } else {
                player.sendMessage("§a✓ Saved. Will apply when you enter combat.");
            }
            player.sendMessage("§7Available: §f" + String.join("§7, §f", availableThemes));
            player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error toggling style: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cFailed to toggle style.");
            return true;
        }
    }

    /**
     * Handles the toggle-sounds subcommand.
     */
    private boolean handleToggleSoundsCommand(Player player) {
        try {
            com.muzlik.pvpcombat.visual.VisualManager vm = getVisualManager();
            if (vm == null) {
                player.sendMessage("§cVisual system is not available.");
                return true;
            }

            java.util.UUID id = player.getUniqueId();
            com.muzlik.pvpcombat.data.VisualPreferences prefs = vm.getPreferences(id);
            boolean newVal = !prefs.isSoundsEnabled();
            prefs.setSoundsEnabled(newVal);
            vm.savePreferences(id, prefs);

            if (newVal) {
                player.sendMessage("§aCombat sounds have been enabled.");
            } else {
                player.sendMessage("§cCombat sounds have been disabled.");
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error toggling sounds: " + e.getMessage());
            player.sendMessage("§cFailed to toggle sounds.");
            return true;
        }
    }

    /**
     * Handles the toggle-bossbar subcommand.
     */
    private boolean handleToggleBossBarCommand(Player player) {
        try {
            com.muzlik.pvpcombat.visual.VisualManager vm = getVisualManager();
            if (vm == null) {
                player.sendMessage("§cVisual system is not available.");
                return true;
            }

            java.util.UUID id = player.getUniqueId();
            com.muzlik.pvpcombat.data.VisualPreferences prefs = vm.getPreferences(id);
            boolean newVal = !prefs.isBossBarEnabled();
            prefs.setBossBarEnabled(newVal);
            vm.savePreferences(id, prefs);

            // If player is in active combat, we must apply this instantly by adding or removing them
            if (plugin.getCombatManager() != null && plugin.getCombatManager().isInCombat(player)) {
                com.muzlik.pvpcombat.combat.CombatManager cm =
                    (com.muzlik.pvpcombat.combat.CombatManager) plugin.getCombatManager();
                for (com.muzlik.pvpcombat.data.CombatSession s : cm.getActiveSessions().values()) {
                    if (s.involvesPlayer(player)) {
                        vm.getBossBarManager().refreshPlayerParticipation(s.getSessionId().toString());
                        break;
                    }
                }
            }

            if (newVal) {
                player.sendMessage("§aCombat bossbar has been enabled.");
            } else {
                player.sendMessage("§cCombat bossbar has been disabled.");
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error toggling bossbar: " + e.getMessage());
            player.sendMessage("§cFailed to toggle bossbar.");
            return true;
        }
    }

    /**
     * Handles the toggle-actionbar subcommand.
     */
    private boolean handleToggleActionBarCommand(Player player) {
        try {
            com.muzlik.pvpcombat.visual.VisualManager vm = getVisualManager();
            if (vm == null) {
                player.sendMessage("§cVisual system is not available.");
                return true;
            }

            java.util.UUID id = player.getUniqueId();
            com.muzlik.pvpcombat.data.VisualPreferences prefs = vm.getPreferences(id);
            boolean newVal = !prefs.isActionBarEnabled();
            prefs.setActionBarEnabled(newVal);
            vm.savePreferences(id, prefs);

            if (newVal) {
                player.sendMessage("§aCombat action bar has been enabled.");
            } else {
                player.sendMessage("§cCombat action bar has been disabled.");
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error toggling actionbar: " + e.getMessage());
            player.sendMessage("§cFailed to toggle actionbar.");
            return true;
        }
    }

    /** Safely retrieves the VisualManager, returning null if unavailable. */
    private com.muzlik.pvpcombat.visual.VisualManager getVisualManager() {
        if (plugin.getVisualManager() instanceof com.muzlik.pvpcombat.visual.VisualManager) {
            return (com.muzlik.pvpcombat.visual.VisualManager) plugin.getVisualManager();
        }
        return null;
    }
}
