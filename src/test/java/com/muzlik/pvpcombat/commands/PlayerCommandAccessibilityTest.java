package com.muzlik.pvpcombat.commands;

import com.muzlik.pvpcombat.combat.CombatManager;
import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import com.muzlik.pvpcombat.data.CombatSession;
import com.muzlik.pvpcombat.data.VisualPreferences;
import com.muzlik.pvpcombat.visual.BossBarManager;
import com.muzlik.pvpcombat.visual.VisualManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class PlayerCommandAccessibilityTest {

    @Mock
    private PvPCombatPlugin plugin;

    @Mock
    private VisualManager visualManager;

    @Mock
    private BossBarManager bossBarManager;

    @Mock
    private CombatManager combatManager;

    @Mock
    private Player player;

    private PlayerCommand playerCommand;
    private final UUID playerId = UUID.randomUUID();
    private VisualPreferences preferences;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(plugin.getVisualManager()).thenReturn(visualManager);
        when(plugin.getCombatManager()).thenReturn(combatManager);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("PvPCombat"));

        when(player.getUniqueId()).thenReturn(playerId);

        preferences = new VisualPreferences(playerId);
        when(visualManager.getPreferences(playerId)).thenReturn(preferences);
        when(visualManager.getBossBarManager()).thenReturn(bossBarManager);

        playerCommand = new PlayerCommand(plugin);
    }

    @Test
    public void testToggleSoundsSubcommand() {
        // Sounds are enabled by default
        assertTrue(preferences.isSoundsEnabled());

        String[] args = new String[]{"toggle-sounds"};
        boolean result = playerCommand.handleCommand(player, args);

        assertTrue(result);
        // Assert sounds were toggled off
        assertTrue(!preferences.isSoundsEnabled());
        verify(visualManager, times(1)).savePreferences(playerId, preferences);
        verify(player, times(1)).sendMessage(contains("sounds have been disabled"));

        // Toggle again
        result = playerCommand.handleCommand(player, args);
        assertTrue(result);
        assertTrue(preferences.isSoundsEnabled());
        verify(visualManager, times(2)).savePreferences(playerId, preferences);
        verify(player, times(1)).sendMessage(contains("sounds have been enabled"));
    }

    @Test
    public void testToggleBossBarSubcommand() {
        // BossBar is enabled by default
        assertTrue(preferences.isBossBarEnabled());

        String[] args = new String[]{"toggle-bossbar"};
        boolean result = playerCommand.handleCommand(player, args);

        assertTrue(result);
        // Assert bossbar was toggled off
        assertTrue(!preferences.isBossBarEnabled());
        verify(visualManager, times(1)).savePreferences(playerId, preferences);
        verify(player, times(1)).sendMessage(contains("bossbar has been disabled"));

        // Toggle again
        result = playerCommand.handleCommand(player, args);
        assertTrue(result);
        assertTrue(preferences.isBossBarEnabled());
        verify(visualManager, times(2)).savePreferences(playerId, preferences);
        verify(player, times(1)).sendMessage(contains("bossbar has been enabled"));
    }

    @Test
    public void testToggleBossBarInstantRefreshWhenInCombat() {
        // Player is in active combat
        when(combatManager.isInCombat(player)).thenReturn(true);

        UUID sessionId = UUID.randomUUID();
        CombatSession session = mock(CombatSession.class);
        when(session.getSessionId()).thenReturn(sessionId);
        when(session.involvesPlayer(player)).thenReturn(true);

        Map<UUID, CombatSession> activeSessions = new HashMap<>();
        activeSessions.put(playerId, session);
        when(combatManager.getActiveSessions()).thenReturn(activeSessions);

        String[] args = new String[]{"toggle-bossbar"};
        boolean result = playerCommand.handleCommand(player, args);

        assertTrue(result);
        // Assert immediate visual refresh was called
        verify(bossBarManager, times(1)).refreshPlayerParticipation(sessionId.toString());
    }

    @Test
    public void testToggleActionBarSubcommand() {
        // ActionBar is enabled by default
        assertTrue(preferences.isActionBarEnabled());

        String[] args = new String[]{"toggle-actionbar"};
        boolean result = playerCommand.handleCommand(player, args);

        assertTrue(result);
        // Assert actionbar was toggled off
        assertTrue(!preferences.isActionBarEnabled());
        verify(visualManager, times(1)).savePreferences(playerId, preferences);
        verify(player, times(1)).sendMessage(contains("action bar has been disabled"));

        // Toggle again
        result = playerCommand.handleCommand(player, args);
        assertTrue(result);
        assertTrue(preferences.isActionBarEnabled());
        verify(visualManager, times(2)).savePreferences(playerId, preferences);
        verify(player, times(1)).sendMessage(contains("action bar has been enabled"));
    }
}
