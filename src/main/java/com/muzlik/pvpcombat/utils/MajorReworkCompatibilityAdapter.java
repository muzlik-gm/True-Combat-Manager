package com.muzlik.pvpcombat.utils;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

/**
 * Compatibility adapter for Minecraft 26.x (major rework).
 *
 * <p>Extends the Modern adapter's Adventure Component API usage, since 26.x
 * is newer than 1.20 and retains the Adventure-based text model. Overrides
 * version-tier reporting so the plugin can identify the major rework branch
 * and apply any 26.x-specific handling as needed.</p>
 */
public class MajorReworkCompatibilityAdapter extends CompatibilityAdapter {

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.legacySection();

    public MajorReworkCompatibilityAdapter(PvPCombatPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getTier() {
        return "MAJOR_REWORK (26+)";
    }

    @Override
    public boolean usesComponentAPI() {
        return true;
    }

    @Override
    public boolean usesRefinedActionBar() {
        return true;
    }

    @Override
    public boolean isMajorRework() {
        return true;
    }

    @Override
    public String translateColor(String message) {
        if (message == null) return "";
        // LegacyComponentSerializer uses '§' as the color code character,
        // but the plugin uses '&'. Convert '&' to '§' before deserializing.
        String legacySection = message.replace('&', '§');
        Component component = SERIALIZER.deserialize(legacySection);
        return SERIALIZER.serialize(component);
    }

    @Override
    public void sendActionBar(Player player, String message) {
        if (player == null || !player.isOnline()) return;
        String formatted = translateColor(message);
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent(formatted)
        );
    }

    @Override
    public BossBar createBossBar(String title, BarColor color, BarStyle style) {
        return org.bukkit.Bukkit.createBossBar(
                translateColor(title), color, style);
    }
}
