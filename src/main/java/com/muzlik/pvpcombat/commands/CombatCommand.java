package com.muzlik.pvpcombat.commands;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Main combat command executor that delegates to specific command handlers.
 * Supports both player and admin commands with enhanced error handling and permission checking.
 *
 * @author PvPCombat Plugin Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class CombatCommand implements CommandExecutor, TabCompleter {

    private final PvPCombatPlugin plugin;
    private final PlayerCommand playerCommand;
    private final AdminCommand adminCommand;

    /**
     * Constructs a new CombatCommand handler.
     *
     * @param plugin The main plugin instance
     * @throws IllegalArgumentException if plugin is null
     */
    public CombatCommand(PvPCombatPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        this.plugin = plugin;
        this.playerCommand = new PlayerCommand(plugin);
        this.adminCommand = new AdminCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players.");
                return true;
            }

            Player player = (Player) sender;

            if (args.length == 0) {
                showHelp(player);
                return true;
            }

            String subCommand = args[0].toLowerCase();

            // Delegate to appropriate handler based on permissions and command type
            if (player.hasPermission("pvpcombat.admin") && isAdminCommand(subCommand)) {
                return adminCommand.onCommand(player, null, label, args);
            } else if (isPlayerCommand(subCommand)) {
                return playerCommand.handleCommand(player, args);
            } else {
                showHelp(player);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling combat command: " + e.getMessage());
            e.printStackTrace();
            if (sender instanceof Player) {
                sender.sendMessage("§cAn error occurred while processing the command. Check console for details.");
            }
            return true;
        }
    }

    /**
     * Checks if a subcommand is an admin command.
     *
     * @param subCommand The subcommand to check
     * @return true if it's an admin command, false otherwise
     */
    private boolean isAdminCommand(String subCommand) {
        return Arrays.asList("inspect", "summary", "reload", "debug", "logging", "protection", "stats", "clear").contains(subCommand);
    }

    /**
     * Checks if a subcommand is a player command.
     *
     * @param subCommand The subcommand to check
     * @return true if it's a player command, false otherwise
     */
    private boolean isPlayerCommand(String subCommand) {
        return Arrays.asList("status", "summary", "toggle-style").contains(subCommand);
    }

    /**
     * Shows help message with command usage and permissions.
     *
     * @param player The player to show help to
     */
    private void showHelp(Player player) {
        try {
            player.sendMessage("§6=== Combat Commands ===");
            player.sendMessage("§e/combat status §7- Shows your combat state");
            player.sendMessage("§e/combat summary §7- Shows your latest PvP fight summary");
            player.sendMessage("§e/combat toggle-style §7- Choose message and theme styles");

            if (player.hasPermission("pvpcombat.admin")) {
                player.sendMessage("§c=== Admin Commands ===");
                player.sendMessage("§e/combat inspect <player> §7- View real-time combat info");
                player.sendMessage("§e/combat summary <player> §7- Access last combat stats");
                player.sendMessage("§e/combat stats §7- View server-wide statistics");
                player.sendMessage("§e/combat clear <player> §7- Force-end combat");
                player.sendMessage("§e/combat reload §7- Reload configuration");
                player.sendMessage("§e/combat debug §7- Toggle debug mode");
                player.sendMessage("§e/combat logging <enabled|disabled> §7- Toggle console logging");
                player.sendMessage("§e/combat protection <player> <seconds> §7- Grant timed protection");
            }

            player.sendMessage("§7§oUse /combat <command> for detailed help on each command.");
        } catch (Exception e) {
            plugin.getLogger().severe("Error showing help: " + e.getMessage());
            player.sendMessage("§cUnable to display help. Check console for details.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        try {
            if (!(sender instanceof Player)) {
                return Collections.emptyList();
            }

            Player player = (Player) sender;
            List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                // Complete subcommands based on permissions
                completions.add("status");
                completions.add("summary");
                completions.add("toggle-style");

                if (player.hasPermission("pvpcombat.admin")) {
                    completions.add("inspect");
                    completions.add("reload");
                    completions.add("debug");
                    completions.add("logging");
                    completions.add("protection");
                    completions.add("stats");
                    completions.add("clear");
                }

                // Filter by current input
                String input = args[0].toLowerCase();
                completions.removeIf(s -> !s.toLowerCase().startsWith(input));
            } else if (args.length == 2 && player.hasPermission("pvpcombat.admin")) {
                // Complete player names for admin commands that need them
                String subCmd = args[0].toLowerCase();
                if ("inspect".equals(subCmd) || "summary".equals(subCmd) || "clear".equals(subCmd)) {
                    String input = args[1].toLowerCase();
                    // Limit to reasonable number to prevent lag
                    plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.getName().toLowerCase().startsWith(input))
                        .limit(10) // Limit suggestions to prevent performance issues
                        .forEach(p -> completions.add(p.getName()));
                }
            }

            return completions;

        } catch (Exception e) {
            plugin.getLogger().warning("Error in tab completion: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}