package com.muzlik.pvpcombat.admin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

import java.util.List;

/**
 * Real-time GUI viewer for active combats.
 * Shows active combat pairs and opponents.
 */
public class AdminGUI {

    private static final String GUI_TITLE = "Active Combat Sessions";

    public AdminGUI() {
    }

    /**
     * Opens the admin GUI for the player.
     */
    public void openGUI(Player player) {
        if (!player.hasPermission("pvpcombat.admin")) {
            player.sendMessage("§cYou don't have permission to view the admin GUI.");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Populate GUI with active combat sessions
        populateCombatSessions(gui);

        player.openInventory(gui);
    }

    /**
     * Populates the GUI with active combat session information.
     */
    private void populateCombatSessions(Inventory gui) {
        // For now, create placeholder items showing no active combats
        // In full implementation, this would query the CombatManager for active sessions

        ItemStack noCombatsItem = new ItemStack(Material.BARRIER, 1);
        ItemMeta meta = noCombatsItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cNo Active Combats");
            meta.setLore(List.of(
                "§7There are currently no active combat sessions.",
                "§7Active combats will appear here when players engage in PvP."
            ));
            noCombatsItem.setItemMeta(meta);
        }

        gui.setItem(22, noCombatsItem); // Center slot

        // Add navigation/info items
        addNavigationItems(gui);
    }

    /**
     * Adds navigation and info items to the GUI.
     */
    private void addNavigationItems(Inventory gui) {
        // Refresh button
        ItemStack refreshItem = new ItemStack(Material.CLOCK, 1);
        ItemMeta refreshMeta = refreshItem.getItemMeta();
        if (refreshMeta != null) {
            refreshMeta.setDisplayName("§aRefresh");
            refreshMeta.setLore(List.of("§7Click to refresh the combat session list."));
            refreshItem.setItemMeta(refreshMeta);
        }
        gui.setItem(49, refreshItem); // Bottom right

        // Stats button
        ItemStack statsItem = new ItemStack(Material.BOOK, 1);
        ItemMeta statsMeta = statsItem.getItemMeta();
        if (statsMeta != null) {
            statsMeta.setDisplayName("§eCombat Statistics");
            statsMeta.setLore(List.of(
                "§7View overall combat system statistics.",
                "§7Total sessions, average duration, etc."
            ));
            statsItem.setItemMeta(statsMeta);
        }
        gui.setItem(50, statsItem); // Bottom middle-right

        // Settings button
        ItemStack settingsItem = new ItemStack(Material.REDSTONE_TORCH, 1);
        ItemMeta settingsMeta = settingsItem.getItemMeta();
        if (settingsMeta != null) {
            settingsMeta.setDisplayName("§cAdmin Settings");
            settingsMeta.setLore(List.of("§7Access admin configuration options."));
            settingsItem.setItemMeta(settingsMeta);
        }
        gui.setItem(51, settingsItem); // Bottom right
    }

    /**
     * Updates the GUI with current combat data.
     * This would be called periodically or on refresh.
     */
    public void updateGUI(Inventory gui) {
        // Clear existing items except navigation
        for (int i = 0; i < 45; i++) { // Don't clear navigation items (45-53)
            gui.setItem(i, null);
        }

        // Re-populate with current data
        populateCombatSessions(gui);
    }
}