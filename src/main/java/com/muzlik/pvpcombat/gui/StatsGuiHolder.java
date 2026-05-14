package com.muzlik.pvpcombat.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Identifies per-player stats inventories so clicks resolve the correct target UUID.
 */
public final class StatsGuiHolder implements InventoryHolder {

    public enum View {
        MAIN,
        WEAPON
    }

    private final UUID statsTargetUuid;
    private final View view;

    public StatsGuiHolder(UUID statsTargetUuid, View view) {
        this.statsTargetUuid = statsTargetUuid;
        this.view = view;
    }

    public UUID getStatsTargetUuid() {
        return statsTargetUuid;
    }

    public View getView() {
        return view;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
