package com.muzlik.pvpcombat.visual;

import com.muzlik.pvpcombat.interfaces.IConfigManager;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ThemeManagerTest {

    private static final Logger LOGGER = Logger.getLogger(ThemeManagerTest.class.getName());

    @Mock
    private IConfigManager configManager;

    private ThemeManager themeManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        themeManager = new ThemeManager(configManager, LOGGER);
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @DisplayName("Theme inner class")
    class ThemeTest {

        @Test
        @Order(1)
        @DisplayName("constructor with all 8 parameters stores values")
        void fullConstructorStoresAllValues() {
            ThemeManager.Theme theme = new ThemeManager.Theme(
                "test", "&c{time_left}s", BarColor.RED, BarStyle.SEGMENTED_10,
                "&cCombat &f{time_left}s", true, "funny", "intense"
            );

            assertEquals("test", theme.getName());
            assertEquals("&c{time_left}s", theme.getBossBarTitle());
            assertEquals(BarColor.RED, theme.getBossBarColor());
            assertEquals(BarStyle.SEGMENTED_10, theme.getBossBarStyle());
            assertEquals("&cCombat &f{time_left}s", theme.getActionBarFormat());
            assertTrue(theme.hasAnimatedTransitions());
            assertEquals("funny", theme.getMessageStyle());
            assertEquals("intense", theme.getSoundProfile());
        }

        @Test
        @Order(2)
        @DisplayName("backward-compatible constructor defaults animated, message style, sound profile")
        void backwardCompatibleConstructorDefaults() {
            ThemeManager.Theme theme = new ThemeManager.Theme(
                "simple", "&7{time_left}", BarColor.WHITE, BarStyle.SOLID,
                "&7{time_left}s"
            );

            assertEquals("simple", theme.getName());
            assertEquals(BarColor.WHITE, theme.getBossBarColor());
            assertEquals(BarStyle.SOLID, theme.getBossBarStyle());
            assertFalse(theme.hasAnimatedTransitions());
            assertEquals("minimal", theme.getMessageStyle());
            assertEquals("default", theme.getSoundProfile());
        }

        @Test
        @Order(3)
        @DisplayName("getDefaultTheme returns expected built-in theme")
        void defaultThemeHasExpectedValues() {
            ThemeManager.Theme defaultTheme = themeManager.getDefaultTheme();
            assertNotNull(defaultTheme);
            assertEquals("default", defaultTheme.getName());
            assertEquals(BarColor.RED, defaultTheme.getBossBarColor());
            assertEquals(BarStyle.SOLID, defaultTheme.getBossBarStyle());
            assertFalse(defaultTheme.hasAnimatedTransitions());
            assertEquals("minimal", defaultTheme.getMessageStyle());
            assertEquals("default", defaultTheme.getSoundProfile());
        }

        @Test
        @Order(4)
        @DisplayName("getTheme returns null for unknown theme")
        void getThemeReturnsNullForUnknown() {
            assertNull(themeManager.getTheme("nonexistent_theme"));
        }

        @Test
        @Order(5)
        @DisplayName("getDefaultTheme never returns null")
        void getDefaultThemeNeverNull() {
            assertNotNull(themeManager.getDefaultTheme());
        }

        @Test
        @Order(6)
        @DisplayName("all built-in themes have names")
        void allBuiltInThemesHaveNames() {
            for (String name : new String[]{"minimal", "fire", "ice", "neon", "dark", "clean"}) {
                ThemeManager.Theme theme = themeManager.getTheme(name);
                assertNotNull(theme, "Theme " + name + " should exist");
                assertEquals(name, theme.getName());
            }
        }

        @Test
        @Order(7)
        @DisplayName("default theme is returned when 'default' key is missing")
        void defaultThemeFallback() {
            ThemeManager.Theme defaultTheme = themeManager.getDefaultTheme();
            assertNotNull(defaultTheme);
        }
    }
}
