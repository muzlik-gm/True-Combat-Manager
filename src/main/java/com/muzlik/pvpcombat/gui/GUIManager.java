package com.muzlik.pvpcombat.gui;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import com.muzlik.pvpcombat.data.PlayerCombatData;
import com.muzlik.pvpcombat.data.WeaponStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class GUIManager {

    private final PvPCombatPlugin plugin;
    private YamlConfiguration guiConfig;

    public GUIManager(PvPCombatPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!configFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    public void openMainStatsGUI(Player viewer) {
        openMainStatsGUI(viewer, viewer.getUniqueId());
    }

    public void openMainStatsGUI(Player viewer, UUID targetUuid) {
        PlayerCombatData data = ((com.muzlik.pvpcombat.combat.CombatManager)plugin.getCombatManager()).getCombatTracker().getPlayerData(targetUuid);
        String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();

        String title = color(guiConfig.getString("main-stats.title", "&6&lCombat Statistics"))
                .replace("{player}", targetName != null ? targetName : "Unknown");
        int size = guiConfig.getInt("main-stats.size", 54);
        Inventory inv = Bukkit.createInventory(null, size, title);

        ConfigurationSection items = guiConfig.getConfigurationSection("main-stats.items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection itemSection = items.getConfigurationSection(key);
                if (itemSection == null) continue;

                Material material;
                try {
                    material = Material.valueOf(itemSection.getString("material", "PAPER"));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in gui.yml: " + itemSection.getString("material"));
                    continue;
                }
                int slot = itemSection.getInt("slot");
                String name = color(itemSection.getString("name", ""));
                List<String> lore = itemSection.getStringList("lore").stream()
                        .map(line -> color(replacePlaceholders(line, data)))
                        .collect(Collectors.toList());

                inv.setItem(slot, createItem(material, name, lore));
            }
        }

        viewer.openInventory(inv);
    }

    public void openWeaponStatsGUI(Player viewer) {
        openWeaponStatsGUI(viewer, viewer.getUniqueId());
    }

    public void openWeaponStatsGUI(Player viewer, UUID targetUuid) {
        PlayerCombatData data = ((com.muzlik.pvpcombat.combat.CombatManager)plugin.getCombatManager()).getCombatTracker().getPlayerData(targetUuid);
        String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();

        String title = color(guiConfig.getString("weapon-stats.title", "&a&lWeapon Statistics"))
                .replace("{player}", targetName != null ? targetName : "Unknown");
        int size = guiConfig.getInt("weapon-stats.size", 54);
        Inventory inv = Bukkit.createInventory(null, size, title);

        ConfigurationSection weapons = guiConfig.getConfigurationSection("weapon-stats.weapons");
        List<String> defaultLore = guiConfig.getStringList("weapon-lore");

        if (weapons != null) {
            for (String key : weapons.getKeys(false)) {
                ConfigurationSection weaponSection = weapons.getConfigurationSection(key);
                if (weaponSection == null) continue;

                Material material;
                try {
                    material = Material.valueOf(weaponSection.getString("material", "STONE"));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid weapon material in gui.yml: " + weaponSection.getString("material"));
                    continue;
                }
                int slot = weaponSection.getInt("slot");
                String name = color(weaponSection.getString("name", key));

                WeaponStats stats = data.getWeaponStats(material.name());
                List<String> lore = defaultLore.stream()
                        .map(line -> color(replaceWeaponPlaceholders(line, stats)))
                        .collect(Collectors.toList());

                inv.setItem(slot, createItem(material, name, lore));
            }
        }

        viewer.openInventory(inv);
    }

    private String replacePlaceholders(String text, PlayerCombatData data) {
        return text.replace("{wins}", String.valueOf(data.getWins()))
                .replace("{losses}", String.valueOf(data.getLosses()))
                .replace("{kd}", String.format("%.2f", data.getKDRatio()))
                .replace("{win_rate}", String.format("%.1f", data.getWinRate()))
                .replace("{damage_dealt}", String.format("%.1f", data.getTotalDamageDealt()))
                .replace("{damage_received}", String.format("%.1f", data.getTotalDamageReceived()))
                .replace("{damage_ratio}", String.format("%.2f", data.getDamageRatio()))
                .replace("{total_combats}", String.valueOf(data.getTotalCombats()))
                .replace("{critical_hits}", String.valueOf(data.getCriticalHits()))
                .replace("{longest_combo}", String.valueOf(data.getLongestCombo()))
                .replace("{highest_damage}", String.format("%.1f", data.getHighestDamageInSession()));
    }

    private String replaceWeaponPlaceholders(String text, WeaponStats stats) {
        return text.replace("{damage}", String.format("%.1f", stats.getTotalDamage()))
                .replace("{kills}", String.valueOf(stats.getKills()))
                .replace("{uses}", String.valueOf(stats.getUses()))
                .replace("{avg_damage}", String.format("%.1f", stats.getAverageDamage()));
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public YamlConfiguration getGuiConfig() {
        return guiConfig;
    }
}
