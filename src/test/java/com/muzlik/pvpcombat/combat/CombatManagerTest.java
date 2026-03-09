package com.muzlik.pvpcombat.combat;

import com.muzlik.pvpcombat.core.PvPCombatPlugin;
import com.muzlik.pvpcombat.data.CombatSession;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CombatManager functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CombatManagerTest {

    @Mock
    private PvPCombatPlugin plugin;
    
    @Mock
    private Player player1;
    
    @Mock
    private Player player2;
    
    private UUID uuid1;
    private UUID uuid2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        uuid1 = UUID.randomUUID();
        uuid2 = UUID.randomUUID();
        
        when(player1.getUniqueId()).thenReturn(uuid1);
        when(player1.getName()).thenReturn("Player1");
        when(player1.isOnline()).thenReturn(true);
        
        when(player2.getUniqueId()).thenReturn(uuid2);
        when(player2.getName()).thenReturn("Player2");
        when(player2.isOnline()).thenReturn(true);
    }

    @Test
    @Order(1)
    @DisplayName("Combat session should be created with valid players")
    void testCombatSessionCreation() {
        CombatSession session = new CombatSession(UUID.randomUUID(), player1, player2, 30);
        
        assertNotNull(session, "Session should be created");
        assertTrue(session.isActive(), "Session should be active");
        assertEquals(30, session.getRemainingTime(), "Timer should be 30 seconds");
        assertEquals(player1, session.getAttacker(), "Attacker should be player1");
        assertEquals(player2, session.getDefender(), "Defender should be player2");
    }

    @Test
    @Order(2)
    @DisplayName("Combat session timer should decrement after time passes")
    void testTimerDecrement() throws InterruptedException {
        CombatSession session = new CombatSession(UUID.randomUUID(), player1, player2, 30);
        
        int initialTime = session.getRemainingTime();
        
        // Wait for at least 1 second
        Thread.sleep(1100);
        
        session.updateTimer();
        int afterUpdate = session.getRemainingTime();
        
        assertTrue(afterUpdate < initialTime, "Timer should decrement after time passes");
    }

    @Test
    @Order(3)
    @DisplayName("Combat session timer should reset correctly")
    void testTimerReset() throws InterruptedException {
        CombatSession session = new CombatSession(UUID.randomUUID(), player1, player2, 30);
        
        // Wait and decrement timer
        Thread.sleep(1100);
        session.updateTimer();
        
        assertTrue(session.getRemainingTime() < 30, "Timer should have decremented");
        
        // Reset timer
        session.resetTimer();
        
        assertEquals(30, session.getRemainingTime(), "Timer should reset to 30");
    }

    @Test
    @Order(4)
    @DisplayName("Combat session should expire when timer reaches zero")
    void testSessionExpiration() {
        CombatSession session = new CombatSession(UUID.randomUUID(), player1, player2, 3);
        
        assertFalse(session.isExpired(), "Session should not be expired initially");
        
        // Manually set remaining seconds to 0 to simulate expiration
        session.getTimerData().setRemainingSeconds(0);
        
        assertTrue(session.isExpired(), "Session should be expired after timer reaches zero");
    }

    @Test
    @Order(5)
    @DisplayName("Combat session should identify opponent correctly")
    void testOpponentIdentification() {
        CombatSession session = new CombatSession(UUID.randomUUID(), player1, player2, 30);
        
        assertEquals(player2, session.getOpponent(player1), "Player2 should be opponent of Player1");
        assertEquals(player1, session.getOpponent(player2), "Player1 should be opponent of Player2");
    }

    @Test
    @Order(6)
    @DisplayName("Combat session should handle null opponent check")
    void testNullOpponentCheck() {
        CombatSession session = new CombatSession(UUID.randomUUID(), player1, player2, 30);
        
        Player unknownPlayer = mock(Player.class);
        when(unknownPlayer.getUniqueId()).thenReturn(UUID.randomUUID());
        
        assertNull(session.getOpponent(unknownPlayer), "Unknown player should have no opponent");
    }

    @Test
    @Order(7)
    @DisplayName("Combat session progress should be calculated correctly")
    void testProgressCalculation() {
        CombatSession session = new CombatSession(UUID.randomUUID(), player1, player2, 30);
        
        double initialProgress = session.getTimerData().getProgress();
        assertEquals(1.0, initialProgress, 0.01, "Initial progress should be 100%");
        
        // Manually set to 50%
        session.getTimerData().setRemainingSeconds(15);
        
        double halfProgress = session.getTimerData().getProgress();
        assertEquals(0.5, halfProgress, 0.01, "Progress should be 50%");
    }

    @Test
    @Order(8)
    @DisplayName("Combat session should handle damage tracking")
    void testDamageTracking() {
        CombatSession session = new CombatSession(UUID.randomUUID(), player1, player2, 30);
        
        session.recordDamage(player1, 10.5);
        assertEquals(10.5, session.getDamageDealt(player1), 0.001);
        assertEquals(10.5, session.getDamageReceived(player2), 0.001);
        
        session.recordDamage(player2, 8.0);
        assertEquals(8.0, session.getDamageDealt(player2), 0.001);
        assertEquals(8.0, session.getDamageReceived(player1), 0.001);
    }

    @Test
    @Order(9)
    @DisplayName("Combat session should maintain state consistency")
    void testStateConsistency() {
        CombatSession session = new CombatSession(UUID.randomUUID(), player1, player2, 30);
        
        assertTrue(session.isActive(), "Session should be active");
        assertFalse(session.isExpired(), "Session should not be expired");
        
        session.setActive(false);
        
        assertFalse(session.isActive(), "Session should be inactive");
    }

    @Test
    @Order(10)
    @DisplayName("Combat session should handle edge case timers")
    void testEdgeCaseTimers() {
        // Test with 1 second timer
        CombatSession session1 = new CombatSession(UUID.randomUUID(), player1, player2, 1);
        assertEquals(1, session1.getRemainingTime());
        session1.getTimerData().setRemainingSeconds(0);
        assertTrue(session1.isExpired());
        
        // Test with large timer
        CombatSession session2 = new CombatSession(UUID.randomUUID(), player1, player2, 300);
        assertEquals(300, session2.getRemainingTime());
        assertFalse(session2.isExpired());
    }
}
