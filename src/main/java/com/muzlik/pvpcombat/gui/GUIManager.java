package com.muzlik.pvpcombat.gui;

import com.muzlik.pvpcombat.combat.CombatManager;
import com.muzlik.pvpcombat.combat.CombatTracker;
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
        CombatManager combatManager = (CombatManager) plugin.getCombatManager();
        PlayerCombatData data = combatManager.getCombatTracker().getPlayerData(targetUuid);
        String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();

        String title = color(replacePlayerToken(guiConfig.getString("main-stats.title", "&6&lCombat Statistics"), targetName));
        int size = guiConfig.getInt("main-stats.size", 54);
        StatsGuiHolder holder = new StatsGuiHolder(targetUuid, StatsGuiHolder.View.MAIN);
        Inventory inv = Bukkit.createInventory(holder, size, title);

        placeConfiguredItems(inv, guiConfig.getConfigurationSection("main-stats.items"), data, targetName);
        applyFiller(inv);
        viewer.openInventory(inv);
    }

    public void openWeaponStatsGUI(Player viewer) {
        openWeaponStatsGUI(viewer, viewer.getUniqueId());
    }

    public void openWeaponStatsGUI(Player viewer, UUID targetUuid) {
        CombatManager combatManager = (CombatManager) plugin.getCombatManager();
        PlayerCombatData data = combatManager.getCombatTracker().getPlayerData(targetUuid);
        String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();

        String title = color(replacePlayerToken(guiConfig.getString("weapon-stats.title", "&a&lWeapon Statistics"), targetName));
        int size = guiConfig.getInt("weapon-stats.size", 54);
        StatsGuiHolder holder = new StatsGuiHolder(targetUuid, StatsGuiHolder.View.WEAPON);
        Inventory inv = Bukkit.createInventory(holder, size, title);

        ConfigurationSection weapons = guiConfig.getConfigurationSection("weapon-stats.weapons");
        List<String> defaultLore = guiConfig.getStringList("weapon-lore");

        if (weapons != null) {
            for (String key : weapons.getKeys(false)) {
                ConfigurationSection weaponSection = weapons.getConfigurationSection(key);
                if (weaponSection == null) {
                    continue;
                }

                Material material;
                try {
                    material = Material.valueOf(weaponSection.getString("material", "STONE"));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid weapon material in gui.yml: " + weaponSection.getString("material"));
                    continue;
                }
                int slot = weaponSection.getInt("slot");
                String name = color(replacePlaceholders(weaponSection.getString("name", key), data, targetName));

                WeaponStats stats = data.getWeaponStats(material.name());
                List<String> lore = defaultLore.stream()
                        .map(line -> color(replaceWeaponPlaceholders(line, stats)))
                        .collect(Collectors.toList());

                inv.setItem(slot, createItem(material, name, lore));
            }
        }

        ConfigurationSection backSection = guiConfig.getConfigurationSection("weapon-stats.back-button");
        if (backSection != null) {
            placeBackButton(inv, backSection);
        }

        applyFiller(inv);
        viewer.openInventory(inv);
    }

    /**
     * Server-wide aggregate statistics (admin).
     */
    public void openServerStatsGUI(Player viewer) {
        if (!(plugin.getCombatManager() instanceof CombatManager)) {
            viewer.sendMessage(ChatColor.RED + "Combat system is not available.");
            return;
        }
        CombatManager combatManager = (CombatManager) plugin.getCombatManager();
        CombatTracker tracker = combatManager.getCombatTracker();
        if (tracker == null) {
            viewer.sendMessage(ChatColor.RED + "Combat tracker is not available.");
            return;
        }

        ServerAggregate agg = computeServerAggregate(combatManager, tracker);

        String title = color(guiConfig.getString("server-stats.title", "&5&lServer Combat Overview"));
        int size = guiConfig.getInt("server-stats.size", 54);
        ServerStatsGuiHolder holder = new ServerStatsGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, size, title);

        ConfigurationSection items = guiConfig.getConfigurationSection("server-stats.items");
        int placed = 0;
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection itemSection = items.getConfigurationSection(key);
                if (itemSection == null) {
                    continue;
                }

                Material material;
                try {
                    material = Material.valueOf(itemSection.getString("material", "PAPER"));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in gui.yml (server-stats): " + itemSection.getString("material"));
                    continue;
                }
                int slot = itemSection.getInt("slot");
                String name = color(itemSection.getString("name", ""));
                List<String> lore = itemSection.getStringList("lore").stream()
                        .map(line -> color(replaceServerPlaceholders(line, agg)))
                        .collect(Collectors.toList());

                inv.setItem(slot, createItem(material, name, lore));
                placed++;
            }
        }
        if (placed == 0) {
            populateServerStatsFallback(inv, agg);
        }

        // Weapon stats button
        ConfigurationSection weaponBtn = guiConfig.getConfigurationSection("server-stats.weapon-stats-button");
        if (weaponBtn != null) {
            placeBackButton(inv, weaponBtn);
        } else {
            inv.setItem(31, createItem(Material.DIAMOND_SWORD, color("&a&lWeapon Statistics"),
                    List.of(color("&e» Click to open"))));
        }

        // Close button
        ConfigurationSection closeBtn = guiConfig.getConfigurationSection("server-stats.close-button");
        if (closeBtn != null) {
            placeBackButton(inv, closeBtn);
        } else {
            inv.setItem(49, createItem(Material.BARRIER, color("&c&lClose"), List.of()));
        }

        applyFiller(inv);
        viewer.openInventory(inv);
    }

    private void placeConfiguredItems(Inventory inv, ConfigurationSection items, PlayerCombatData data, String playerName) {
        if (items == null) {
            return;
        }
        for (String key : items.getKeys(false)) {
            ConfigurationSection itemSection = items.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }

            Material material;
            try {
                material = Material.valueOf(itemSection.getString("material", "PAPER"));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in gui.yml: " + itemSection.getString("material"));
                continue;
            }
            int slot = itemSection.getInt("slot");
            String name = color(replacePlaceholders(itemSection.getString("name", ""), data, playerName));
            List<String> lore = itemSection.getStringList("lore").stream()
                    .map(line -> color(replacePlaceholders(line, data, playerName)))
                    .collect(Collectors.toList());

            inv.setItem(slot, createItem(material, name, lore));
        }
    }

    private void placeBackButton(Inventory inv, ConfigurationSection backSection) {
        try {
            Material material = Material.valueOf(backSection.getString("material", "ARROW"));
            int slot = backSection.getInt("slot", 49);
            String name = color(backSection.getString("name", "&7Back"));
            List<String> lore = backSection.getStringList("lore").stream().map(this::color).collect(Collectors.toList());
            inv.setItem(slot, createItem(material, name, lore));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid back-button material in gui.yml");
        }
    }

    private void applyFiller(Inventory inv) {
        if (!guiConfig.getBoolean("gui-ux.filler-enabled", true)) {
            return;
        }
        Material pane;
        try {
            pane = Material.valueOf(guiConfig.getString("gui-ux.filler-material", "GRAY_STAINED_GLASS_PANE"));
        } catch (IllegalArgumentException e) {
            pane = Material.GRAY_STAINED_GLASS_PANE;
        }
        String label = color(guiConfig.getString("gui-ux.filler-name", " "));
        ItemStack filler = createItem(pane, label, List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler.clone());
            }
        }
    }

    private ServerAggregate computeServerAggregate(CombatManager combatManager, CombatTracker tracker) {
        Map<UUID, PlayerCombatData> allData = tracker.getAllPlayerData();

        int totalPlayers = allData.size();
        int totalCombats = 0;
        int totalWins = 0;
        int totalLosses = 0;
        double totalDamageDealt = 0;
        double totalDamageReceived = 0;
        long totalCombatTime = 0;

        for (PlayerCombatData d : allData.values()) {
            totalCombats += d.getTotalCombats();
            totalWins += d.getWins();
            totalLosses += d.getLosses();
            totalDamageDealt += d.getTotalDamageDealt();
            totalDamageReceived += d.getTotalDamageReceived();
            totalCombatTime += d.getTotalCombatTime();
        }

        int activeSessions = combatManager.getActiveSessions().size() / 2;
        int denom = totalWins + totalLosses;
        double winRateGlobal = denom > 0 ? (double) totalWins / denom * 100.0 : 0.0;
        long hours = totalCombatTime / (1000 * 60 * 60);
        long minutes = (totalCombatTime / (1000 * 60)) % 60;
        long seconds = (totalCombatTime / 1000) % 60;

        return new ServerAggregate(totalPlayers, activeSessions, totalCombats, totalWins, totalLosses,
                totalDamageDealt, totalDamageReceived, hours, minutes, seconds, winRateGlobal,
                totalPlayers > 0 ? (double) totalCombats / totalPlayers : 0,
                totalPlayers > 0 ? totalDamageDealt / totalPlayers : 0);
    }

    private void populateServerStatsFallback(Inventory inv, ServerAggregate agg) {
        inv.setItem(13, createItem(Material.NETHER_STAR, color("&d&lNetwork Snapshot"), List.of(
                color("&7Players: &f" + agg.totalPlayers),
                color("&7Active fights: &f" + agg.activeSessions),
                color("&7Combats: &f" + agg.totalCombats),
                color("&7Win Rate: &f" + String.format("%.1f", agg.winRateGlobal) + "%")
        )));
        inv.setItem(20, createItem(Material.FIRE_CHARGE, color("&c&lServer Damage Totals"), List.of(
                color("&7Dealt: &f" + String.format("%.1f", agg.totalDamageDealt)),
                color("&7Received: &f" + String.format("%.1f", agg.totalDamageReceived)),
                color("&7Time: &f" + agg.hours + "h " + agg.minutes + "m " + agg.seconds + "s")
        )));
        inv.setItem(22, createItem(Material.BEACON, color("&a&lWins &7/ &c&lLosses"), List.of(
                color("&aWins:   &f" + agg.totalWins),
                color("&cLosses: &f" + agg.totalLosses)
        )));
        inv.setItem(24, createItem(Material.WRITABLE_BOOK, color("&e&lPer-Player Averages"), List.of(
                color("&7Combats/player: &f" + String.format("%.1f", agg.avgCombatsPerPlayer)),
                color("&7Damage/player:  &f" + String.format("%.1f", agg.avgDamageDealtPerPlayer))
        )));
    }

    private String replaceServerPlaceholders(String text, ServerAggregate agg) {
        return text
                .replace("{total_players}", String.valueOf(agg.totalPlayers))
                .replace("{active_sessions}", String.valueOf(agg.activeSessions))
                .replace("{total_combats}", String.valueOf(agg.totalCombats))
                .replace("{total_wins}", String.valueOf(agg.totalWins))
                .replace("{total_losses}", String.valueOf(agg.totalLosses))
                .replace("{total_damage_dealt}", String.format("%.1f", agg.totalDamageDealt))
                .replace("{total_damage_received}", String.format("%.1f", agg.totalDamageReceived))
                .replace("{combat_time}", agg.hours + "h " + agg.minutes + "m " + agg.seconds + "s")
                .replace("{win_rate_global}", String.format("%.1f", agg.winRateGlobal))
                .replace("{avg_combats}", String.format("%.1f", agg.avgCombatsPerPlayer))
                .replace("{avg_damage}", String.format("%.1f", agg.avgDamageDealtPerPlayer));
    }

    private static String replacePlayerToken(String text, String playerName) {
        if (text == null) {
            return "";
        }
        return text.replace("{player}", playerName != null ? playerName : "Unknown");
    }

    private String replacePlaceholders(String text, PlayerCombatData data, String playerName) {
        if (text == null) {
            text = "";
        }
        return replacePlayerToken(text
                .replace("{wins}", String.valueOf(data.getWins()))
                .replace("{losses}", String.valueOf(data.getLosses()))
                .replace("{kd}", String.format("%.2f", data.getKDRatio()))
                .replace("{win_rate}", String.format("%.1f", data.getWinRate()))
                .replace("{damage_dealt}", String.format("%.1f", data.getTotalDamageDealt()))
                .replace("{damage_received}", String.format("%.1f", data.getTotalDamageReceived()))
                .replace("{damage_ratio}", String.format("%.2f", data.getDamageRatio()))
                .replace("{total_combats}", String.valueOf(data.getTotalCombats()))
                .replace("{critical_hits}", String.valueOf(data.getCriticalHits()))
                .replace("{longest_combo}", String.valueOf(data.getLongestCombo()))
                .replace("{highest_damage}", String.format("%.1f", data.getHighestDamageInSession())),
                playerName);
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

    public int getWeaponStatsButtonSlot() {
        return guiConfig.getInt("main-stats.items.weapon-stats-button.slot", 31);
    }

    public int getWeaponBackButtonSlot() {
        ConfigurationSection back = guiConfig.getConfigurationSection("weapon-stats.back-button");
        return back != null ? back.getInt("slot", 49) : 49;
    }

    public int getServerWeaponButtonSlot() {
        ConfigurationSection btn = guiConfig.getConfigurationSection("server-stats.weapon-stats-button");
        return btn != null ? btn.getInt("slot", 31) : 31;
    }

    public int getServerCloseButtonSlot() {
        ConfigurationSection btn = guiConfig.getConfigurationSection("server-stats.close-button");
        return btn != null ? btn.getInt("slot", 49) : 49;
    }

    private static final class ServerAggregate {
        final int totalPlayers;
        final int activeSessions;
        final int totalCombats;
        final int totalWins;
        final int totalLosses;
        final double totalDamageDealt;
        final double totalDamageReceived;
        final long hours;
        final long minutes;
        final long seconds;
        final double winRateGlobal;
        final double avgCombatsPerPlayer;
        final double avgDamageDealtPerPlayer;

        ServerAggregate(int totalPlayers, int activeSessions, int totalCombats, int totalWins, int totalLosses,
                        double totalDamageDealt, double totalDamageReceived,
                        long hours, long minutes, long seconds, double winRateGlobal,
                        double avgCombatsPerPlayer, double avgDamageDealtPerPlayer) {
            this.totalPlayers = totalPlayers;
            this.activeSessions = activeSessions;
            this.totalCombats = totalCombats;
            this.totalWins = totalWins;
            this.totalLosses = totalLosses;
            this.totalDamageDealt = totalDamageDealt;
            this.totalDamageReceived = totalDamageReceived;
            this.hours = hours;
            this.minutes = minutes;
            this.seconds = seconds;
            this.winRateGlobal = winRateGlobal;
            this.avgCombatsPerPlayer = avgCombatsPerPlayer;
            this.avgDamageDealtPerPlayer = avgDamageDealtPerPlayer;
        }
    }
}
