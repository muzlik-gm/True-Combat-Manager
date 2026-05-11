package com.muzlik.pvpcombat.gui;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    private final PvPCombatPlugin plugin;
    private final GUIManager guiManager;

    public GUIListener(PvPCombatPlugin plugin, GUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        String mainTitle = guiManager.getGuiConfig().getString("main-stats.title", "&6&lCombat Statistics");
        String weaponTitle = guiManager.getGuiConfig().getString("weapon-stats.title", "&a&lWeapon Statistics");

        if (title.equals(color(mainTitle)) || title.equals(color(weaponTitle))) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            // Check if clicked the weapon stats button in main menu
            int weaponStatsSlot = guiManager.getGuiConfig().getInt("main-stats.items.weapon-stats-button.slot", -1);
            if (event.getRawSlot() == weaponStatsSlot && title.equals(color(mainTitle))) {
                guiManager.openWeaponStatsGUI(player);
            }
        }
    }

    private String color(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}
