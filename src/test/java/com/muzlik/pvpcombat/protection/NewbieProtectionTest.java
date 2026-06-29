package com.muzlik.pvpcombat.protection;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NewbieProtectionTest {

    @Mock
    private PvPCombatPlugin plugin;

    @Mock
    private FileConfiguration config;

    private NewbieProtection protection;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getConfig()).thenReturn(config);
        when(config.getBoolean("newbie-protection.enabled", true)).thenReturn(true);
        when(config.getBoolean("logging.console-enabled", false)).thenReturn(false);

        protection = new NewbieProtection(plugin);
    }

    @Test
    @Order(1)
    @DisplayName("constructor initializes protection maps")
    void constructorInitializesEmptyMaps() {
        assertNotNull(protection);
    }

    @Test
    @Order(2)
    @DisplayName("isEnabled reads from config")
    void isEnabledReadsFromConfig() {
        when(config.getBoolean("newbie-protection.enabled", true)).thenReturn(true);
        assertTrue(protection.isEnabled());

        when(config.getBoolean("newbie-protection.enabled", true)).thenReturn(false);
        assertFalse(protection.isEnabled());
    }

    @Test
    @Order(3)
    @DisplayName("getNewbieAttackMessage returns formatted message")
    void getNewbieAttackMessageReturnsFormattedMessage() {
        when(config.getString("newbie-protection.newbie-attack-message",
            "&cYou need armor to attack other players!"))
            .thenReturn("&cCustom message");
        assertEquals("§cCustom message", protection.getNewbieAttackMessage());
    }

    @Test
    @Order(4)
    @DisplayName("getAttackingNewbieMessage returns formatted message")
    void getAttackingNewbieMessageReturnsFormattedMessage() {
        when(config.getString("newbie-protection.attacking-newbie-message",
            "&cYou cannot attack players without armor!"))
            .thenReturn("&aCustom message");
        assertEquals("§aCustom message", protection.getAttackingNewbieMessage());
    }

    @Test
    @Order(5)
    @DisplayName("cleanup does not throw")
    void cleanupDoesNotThrow() {
        assertDoesNotThrow(() -> protection.cleanup());
    }
}
