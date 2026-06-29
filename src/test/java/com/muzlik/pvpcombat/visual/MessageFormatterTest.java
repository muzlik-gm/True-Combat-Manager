package com.muzlik.pvpcombat.visual;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MessageFormatterTest {

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @DisplayName("MessageStyle inner class")
    class MessageStyleTest {

        @Test
        @Order(1)
        @DisplayName("constructor stores all fields")
        void constructorStoresAllFields() {
            MessageFormatter.MessageStyle style = new MessageFormatter.MessageStyle(
                "test", "&a[PREFIX] &f", "&r &a[END]", true, true
            );

            assertEquals("test", style.getName());
            assertEquals("&a[PREFIX] &f", style.getPrefix());
            assertEquals("&r &a[END]", style.getSuffix());
            assertTrue(style.shouldShowHealth());
            assertTrue(style.shouldShowOpponent());
        }

        @Test
        @Order(2)
        @DisplayName("applyStyle adds prefix and suffix to message")
        void applyStyleAddsPrefixAndSuffix() {
            MessageFormatter.MessageStyle style = new MessageFormatter.MessageStyle(
                "test", "&7[COMBAT] ", " &7[END]", false, false
            );

            String result = style.applyStyle("Fight!");
            assertEquals("&7[COMBAT] Fight! &7[END]", result);
        }

        @Test
        @Order(3)
        @DisplayName("applyStyle with empty prefix only adds suffix")
        void applyStyleWithEmptyPrefix() {
            MessageFormatter.MessageStyle style = new MessageFormatter.MessageStyle(
                "test", "", " &c[OVER]", false, false
            );

            String result = style.applyStyle("Battle");
            assertEquals("Battle &c[OVER]", result);
        }

        @Test
        @Order(4)
        @DisplayName("applyStyle with empty suffix only adds prefix")
        void applyStyleWithEmptySuffix() {
            MessageFormatter.MessageStyle style = new MessageFormatter.MessageStyle(
                "test", "&a[P] ", "", false, false
            );

            String result = style.applyStyle("Engaged");
            assertEquals("&a[P] Engaged", result);
        }

        @Test
        @Order(5)
        @DisplayName("applyStyle with null prefix does not add null text")
        void applyStyleWithNullPrefix() {
            MessageFormatter.MessageStyle style = new MessageFormatter.MessageStyle(
                "test", null, " &c[END]", false, false
            );

            String result = style.applyStyle("Combat");
            assertEquals("Combat &c[END]", result);
        }

        @Test
        @Order(6)
        @DisplayName("applyStyle with null suffix does not add null text")
        void applyStyleWithNullSuffix() {
            MessageFormatter.MessageStyle style = new MessageFormatter.MessageStyle(
                "test", "&a[START] ", null, false, false
            );

            String result = style.applyStyle("Duel");
            assertEquals("&a[START] Duel", result);
        }

        @Test
        @Order(7)
        @DisplayName("applyStyle with empty prefix and suffix returns message unchanged")
        void applyStyleWithBothEmpty() {
            MessageFormatter.MessageStyle style = new MessageFormatter.MessageStyle(
                "test", "", "", false, false
            );

            String result = style.applyStyle("RawMessage");
            assertEquals("RawMessage", result);
        }

        @Test
        @Order(8)
        @DisplayName("showHealth and showOpponent are independent")
        void showHealthAndOpponentIndependent() {
            MessageFormatter.MessageStyle style1 = new MessageFormatter.MessageStyle("a", "", "", true, false);
            assertTrue(style1.shouldShowHealth());
            assertFalse(style1.shouldShowOpponent());

            MessageFormatter.MessageStyle style2 = new MessageFormatter.MessageStyle("b", "", "", false, true);
            assertFalse(style2.shouldShowHealth());
            assertTrue(style2.shouldShowOpponent());
        }

        @Test
        @Order(9)
        @DisplayName("getName returns correct name")
        void getNameReturnsName() {
            MessageFormatter.MessageStyle style = new MessageFormatter.MessageStyle(
                "minimal", "&7", "&r", false, false
            );
            assertEquals("minimal", style.getName());
        }
    }
}
