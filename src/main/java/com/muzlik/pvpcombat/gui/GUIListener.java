package com.muzlik.pvpcombat.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    private final GUIManager guiManager;

    public GUIListener(GUIManager guiManager) {
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof StatsGuiHolder) && !(holder instanceof ServerStatsGuiHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        int raw = event.getRawSlot();

        // ── Server stats GUI ──────────────────────────────────────────────
        if (holder instanceof ServerStatsGuiHolder) {
            if (raw == guiManager.getServerWeaponButtonSlot()) {
                // Open server-wide weapon breakdown (first online player as proxy viewer)
                guiManager.openWeaponStatsGUI(player, player.getUniqueId());
            } else if (raw == guiManager.getServerCloseButtonSlot()) {
                player.closeInventory();
            }
            return;
        }

        // ── Player stats GUIs ─────────────────────────────────────────────
        StatsGuiHolder statsHolder = (StatsGuiHolder) holder;

        if (statsHolder.getView() == StatsGuiHolder.View.MAIN
                && raw == guiManager.getWeaponStatsButtonSlot()) {
            guiManager.openWeaponStatsGUI(player, statsHolder.getStatsTargetUuid());
            return;
        }

        if (statsHolder.getView() == StatsGuiHolder.View.WEAPON
                && raw == guiManager.getWeaponBackButtonSlot()) {
            guiManager.openMainStatsGUI(player, statsHolder.getStatsTargetUuid());
        }
    }
}
