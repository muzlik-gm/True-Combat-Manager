package com.muzlik.pvpcombat.utils;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VersionCompatibilityTest {

    private VersionCompatibility.MinecraftVersion v1_19_4;
    private VersionCompatibility.MinecraftVersion v1_20_0;
    private VersionCompatibility.MinecraftVersion v1_20_1;
    private VersionCompatibility.MinecraftVersion v1_21_0;
    private VersionCompatibility.MinecraftVersion v2_0_0;
    private VersionCompatibility.MinecraftVersion sameAs1_20_0;

    @BeforeEach
    void setUp() {
        v1_19_4 = new VersionCompatibility.MinecraftVersion(1, 19, 4);
        v1_20_0 = new VersionCompatibility.MinecraftVersion(1, 20, 0);
        v1_20_1 = new VersionCompatibility.MinecraftVersion(1, 20, 1);
        v1_21_0 = new VersionCompatibility.MinecraftVersion(1, 21, 0);
        v2_0_0 = new VersionCompatibility.MinecraftVersion(2, 0, 0);
        sameAs1_20_0 = new VersionCompatibility.MinecraftVersion(1, 20, 0);
    }

    @Test
    @Order(1)
    @DisplayName("Constructor and getters should work correctly")
    void testConstructorAndGetters() {
        VersionCompatibility.MinecraftVersion v = new VersionCompatibility.MinecraftVersion(1, 20, 4);
        assertEquals(1, v.getMajor());
        assertEquals(20, v.getMinor());
        assertEquals(4, v.getPatch());
    }

    @Test
    @Order(2)
    @DisplayName("toString should return major.minor.patch format")
    void testToString() {
        assertEquals("1.19.4", v1_19_4.toString());
        assertEquals("1.20.0", v1_20_0.toString());
        assertEquals("2.0.0", v2_0_0.toString());
    }

    @Test
    @Order(3)
    @DisplayName("compareTo should return 0 for equal versions")
    void testCompareToEqual() {
        assertEquals(0, v1_20_0.compareTo(sameAs1_20_0));
    }

    @Test
    @Order(4)
    @DisplayName("compareTo should compare major version first")
    void testCompareToMajor() {
        assertTrue(v1_20_0.compareTo(v2_0_0) < 0);
        assertTrue(v2_0_0.compareTo(v1_20_0) > 0);
    }

    @Test
    @Order(5)
    @DisplayName("compareTo should compare minor version when major is equal")
    void testCompareToMinor() {
        assertTrue(v1_19_4.compareTo(v1_20_0) < 0);
        assertTrue(v1_20_0.compareTo(v1_19_4) > 0);
    }

    @Test
    @Order(6)
    @DisplayName("compareTo should compare patch version when major and minor are equal")
    void testCompareToPatch() {
        assertTrue(v1_20_0.compareTo(v1_20_1) < 0);
        assertTrue(v1_20_1.compareTo(v1_20_0) > 0);
    }

    @Test
    @Order(7)
    @DisplayName("isAtLeast should return true for equal version")
    void testIsAtLeastEqual() {
        assertTrue(v1_20_0.isAtLeast(sameAs1_20_0));
    }

    @Test
    @Order(8)
    @DisplayName("isAtLeast should return true for greater version")
    void testIsAtLeastGreater() {
        assertTrue(v1_20_1.isAtLeast(v1_20_0));
        assertTrue(v1_21_0.isAtLeast(v1_20_1));
        assertTrue(v2_0_0.isAtLeast(v1_20_0));
    }

    @Test
    @Order(9)
    @DisplayName("isAtLeast should return false for lesser version")
    void testIsAtLeastLess() {
        assertFalse(v1_19_4.isAtLeast(v1_20_0));
        assertFalse(v1_20_0.isAtLeast(v1_20_1));
    }

    @Test
    @Order(10)
    @DisplayName("isAtMost should return true for equal version")
    void testIsAtMostEqual() {
        assertTrue(v1_20_0.isAtMost(sameAs1_20_0));
    }

    @Test
    @Order(11)
    @DisplayName("isAtMost should return true for lesser version")
    void testIsAtMostLess() {
        assertTrue(v1_19_4.isAtMost(v1_20_0));
        assertTrue(v1_20_0.isAtMost(v1_21_0));
    }

    @Test
    @Order(12)
    @DisplayName("isAtMost should return false for greater version")
    void testIsAtMostGreater() {
        assertFalse(v1_21_0.isAtMost(v1_20_1));
        assertFalse(v1_20_0.isAtMost(v1_19_4));
    }

    @Test
    @Order(13)
    @DisplayName("compareTo should handle zero patch differences")
    void testCompareToZeroPatch() {
        VersionCompatibility.MinecraftVersion a = new VersionCompatibility.MinecraftVersion(1, 20, 0);
        VersionCompatibility.MinecraftVersion b = new VersionCompatibility.MinecraftVersion(1, 20, 0);
        assertEquals(0, a.compareTo(b));
    }
}
