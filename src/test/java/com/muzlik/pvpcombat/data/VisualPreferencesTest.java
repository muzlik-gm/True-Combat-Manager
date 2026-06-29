package com.muzlik.pvpcombat.data;

import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VisualPreferencesTest {

    private UUID playerId;
    private VisualPreferences prefs;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        prefs = new VisualPreferences(playerId);
    }

    @Test
    @Order(1)
    @DisplayName("Default constructor should set default values")
    void testDefaultConstructor() {
        assertEquals(playerId, prefs.getPlayerId());
        assertEquals("default", prefs.getSelectedTheme());
        assertEquals("default", prefs.getSelectedSoundProfile());
        assertEquals("minimal", prefs.getSelectedMessageStyle());
        assertTrue(prefs.isAnimationsEnabled());
        assertTrue(prefs.isSoundsEnabled());
        assertTrue(prefs.isBossBarEnabled());
        assertTrue(prefs.isActionBarEnabled());
    }

    @Test
    @Order(2)
    @DisplayName("Full constructor should set all values with null fallbacks")
    void testFullConstructor() {
        VisualPreferences full = new VisualPreferences(
            playerId, "dark", "pvp", "detailed",
            false, true, false, true
        );
        assertEquals(playerId, full.getPlayerId());
        assertEquals("dark", full.getSelectedTheme());
        assertEquals("pvp", full.getSelectedSoundProfile());
        assertEquals("detailed", full.getSelectedMessageStyle());
        assertFalse(full.isAnimationsEnabled());
        assertTrue(full.isSoundsEnabled());
        assertFalse(full.isBossBarEnabled());
        assertTrue(full.isActionBarEnabled());
    }

    @Test
    @Order(3)
    @DisplayName("Full constructor should use defaults for null values")
    void testFullConstructorNullFallbacks() {
        VisualPreferences full = new VisualPreferences(
            playerId, null, null, null,
            false, false, false, false
        );
        assertEquals("default", full.getSelectedTheme());
        assertEquals("default", full.getSelectedSoundProfile());
        assertEquals("minimal", full.getSelectedMessageStyle());
    }

    @Test
    @Order(4)
    @DisplayName("Setters should update values")
    void testSetters() {
        prefs.setSelectedTheme("ocean");
        assertEquals("ocean", prefs.getSelectedTheme());

        prefs.setSelectedSoundProfile("battle");
        assertEquals("battle", prefs.getSelectedSoundProfile());

        prefs.setSelectedMessageStyle("chatty");
        assertEquals("chatty", prefs.getSelectedMessageStyle());

        prefs.setAnimationsEnabled(false);
        assertFalse(prefs.isAnimationsEnabled());

        prefs.setSoundsEnabled(false);
        assertFalse(prefs.isSoundsEnabled());

        prefs.setBossBarEnabled(false);
        assertFalse(prefs.isBossBarEnabled());

        prefs.setActionBarEnabled(false);
        assertFalse(prefs.isActionBarEnabled());
    }

    @Test
    @Order(5)
    @DisplayName("resetToDefaults should restore default values")
    void testResetToDefaults() {
        prefs.setSelectedTheme("ocean");
        prefs.setSelectedSoundProfile("battle");
        prefs.setSelectedMessageStyle("chatty");
        prefs.setAnimationsEnabled(false);
        prefs.setSoundsEnabled(false);
        prefs.setBossBarEnabled(false);
        prefs.setActionBarEnabled(false);

        prefs.resetToDefaults();

        assertEquals("default", prefs.getSelectedTheme());
        assertEquals("default", prefs.getSelectedSoundProfile());
        assertEquals("minimal", prefs.getSelectedMessageStyle());
        assertTrue(prefs.isAnimationsEnabled());
        assertTrue(prefs.isSoundsEnabled());
        assertTrue(prefs.isBossBarEnabled());
        assertTrue(prefs.isActionBarEnabled());
    }

    @Test
    @Order(6)
    @DisplayName("copy should create an independent copy")
    void testCopy() {
        VisualPreferences copy = prefs.copy();
        assertEquals(prefs.getPlayerId(), copy.getPlayerId());
        assertEquals(prefs.getSelectedTheme(), copy.getSelectedTheme());
        assertEquals(prefs.getSelectedSoundProfile(), copy.getSelectedSoundProfile());
        assertEquals(prefs.getSelectedMessageStyle(), copy.getSelectedMessageStyle());
        assertEquals(prefs.isAnimationsEnabled(), copy.isAnimationsEnabled());
        assertEquals(prefs.isSoundsEnabled(), copy.isSoundsEnabled());
        assertEquals(prefs.isBossBarEnabled(), copy.isBossBarEnabled());
        assertEquals(prefs.isActionBarEnabled(), copy.isActionBarEnabled());

        copy.setSelectedTheme("dark");
        assertNotEquals(prefs.getSelectedTheme(), copy.getSelectedTheme());
    }
}
