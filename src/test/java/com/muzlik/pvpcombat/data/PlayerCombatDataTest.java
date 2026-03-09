package com.muzlik.pvpcombat.data;

import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PlayerCombatData statistics tracking.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlayerCombatDataTest {

    private PlayerCombatData data;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        data = new PlayerCombatData(playerId);
    }

    @Test
    @Order(1)
    @DisplayName("Player combat data should initialize with zero values")
    void testInitialization() {
        assertEquals(playerId, data.getPlayerId());
        assertEquals(0, data.getTotalCombats());
        assertEquals(0, data.getWins());
        assertEquals(0, data.getLosses());
        assertEquals(0.0, data.getTotalDamageDealt(), 0.001);
        assertEquals(0.0, data.getTotalDamageReceived(), 0.001);
        assertEquals(0L, data.getTotalCombatTime());
    }

    @Test
    @Order(2)
    @DisplayName("Damage dealt should accumulate correctly")
    void testDamageDealtAccumulation() {
        data.addDamageDealt(10.5);
        assertEquals(10.5, data.getTotalDamageDealt(), 0.001);
        
        data.addDamageDealt(5.25);
        assertEquals(15.75, data.getTotalDamageDealt(), 0.001);
        
        data.addDamageDealt(20.0);
        assertEquals(35.75, data.getTotalDamageDealt(), 0.001);
    }

    @Test
    @Order(3)
    @DisplayName("Damage received should accumulate correctly")
    void testDamageReceivedAccumulation() {
        data.addDamageReceived(8.0);
        assertEquals(8.0, data.getTotalDamageReceived(), 0.001);
        
        data.addDamageReceived(12.5);
        assertEquals(20.5, data.getTotalDamageReceived(), 0.001);
    }

    @Test
    @Order(4)
    @DisplayName("Win count should increment correctly")
    void testWinIncrement() {
        assertEquals(0, data.getWins());
        
        data.incrementWins();
        assertEquals(1, data.getWins());
        
        data.incrementWins();
        data.incrementWins();
        assertEquals(3, data.getWins());
    }

    @Test
    @Order(5)
    @DisplayName("Loss count should increment correctly")
    void testLossIncrement() {
        assertEquals(0, data.getLosses());
        
        data.incrementLosses();
        assertEquals(1, data.getLosses());
        
        data.incrementLosses();
        assertEquals(2, data.getLosses());
    }

    @Test
    @Order(6)
    @DisplayName("Combat count should increment correctly")
    void testCombatIncrement() {
        assertEquals(0, data.getTotalCombats());
        
        data.incrementCombats();
        assertEquals(1, data.getTotalCombats());
        
        for (int i = 0; i < 10; i++) {
            data.incrementCombats();
        }
        assertEquals(11, data.getTotalCombats());
    }

    @Test
    @Order(7)
    @DisplayName("K/D ratio should calculate correctly with losses")
    void testKDRatioWithLosses() {
        data.incrementWins();
        data.incrementWins();
        data.incrementWins();
        data.incrementLosses();
        
        double expectedKD = 3.0 / 1.0;
        assertEquals(expectedKD, data.getKDRatio(), 0.001);
    }

    @Test
    @Order(8)
    @DisplayName("K/D ratio should handle zero losses")
    void testKDRatioWithZeroLosses() {
        data.incrementWins();
        data.incrementWins();
        data.incrementWins();
        
        // With zero losses, K/D should equal wins
        assertEquals(3.0, data.getKDRatio(), 0.001);
    }

    @Test
    @Order(9)
    @DisplayName("K/D ratio should handle zero wins")
    void testKDRatioWithZeroWins() {
        data.incrementLosses();
        data.incrementLosses();
        
        assertEquals(0.0, data.getKDRatio(), 0.001);
    }

    @Test
    @Order(10)
    @DisplayName("Win rate should calculate correctly")
    void testWinRateCalculation() {
        data.incrementWins();
        data.incrementCombats();
        data.incrementWins();
        data.incrementCombats();
        data.incrementLosses();
        data.incrementCombats();
        data.incrementLosses();
        data.incrementCombats();
        
        // 2 wins out of 4 combats = 50%
        assertEquals(50.0, data.getWinRate(), 0.001);
    }

    @Test
    @Order(11)
    @DisplayName("Win rate should handle zero combats")
    void testWinRateWithZeroCombats() {
        assertEquals(0.0, data.getWinRate(), 0.001);
    }

    @Test
    @Order(12)
    @DisplayName("Win rate should handle 100% wins")
    void testWinRateWithAllWins() {
        for (int i = 0; i < 10; i++) {
            data.incrementWins();
            data.incrementCombats();
        }
        
        assertEquals(100.0, data.getWinRate(), 0.001);
    }

    @Test
    @Order(13)
    @DisplayName("Combat time should accumulate correctly")
    void testCombatTimeAccumulation() {
        data.addCombatTime(30000L); // 30 seconds
        assertEquals(30000L, data.getTotalCombatTime());
        
        data.addCombatTime(45000L); // 45 seconds
        assertEquals(75000L, data.getTotalCombatTime());
    }

    @Test
    @Order(14)
    @DisplayName("Damage ratio should calculate correctly")
    void testDamageRatioCalculation() {
        data.addDamageDealt(100.0);
        data.addDamageReceived(50.0);
        
        double expectedRatio = 100.0 / 50.0;
        assertEquals(expectedRatio, data.getDamageRatio(), 0.001);
    }

    @Test
    @Order(15)
    @DisplayName("Damage ratio should handle zero damage received")
    void testDamageRatioWithZeroReceived() {
        data.addDamageDealt(100.0);
        
        // With zero damage received, ratio should equal damage dealt
        assertEquals(100.0, data.getDamageRatio(), 0.001);
    }

    @Test
    @Order(16)
    @DisplayName("Damage ratio should handle zero damage dealt")
    void testDamageRatioWithZeroDealt() {
        data.addDamageReceived(50.0);
        
        assertEquals(0.0, data.getDamageRatio(), 0.001);
    }

    @Test
    @Order(17)
    @DisplayName("Statistics should handle large numbers")
    void testLargeNumbers() {
        for (int i = 0; i < 1000; i++) {
            data.addDamageDealt(10.5);
            data.incrementCombats();
        }
        
        assertEquals(10500.0, data.getTotalDamageDealt(), 0.001);
        assertEquals(1000, data.getTotalCombats());
    }

    @Test
    @Order(18)
    @DisplayName("Statistics should handle concurrent updates")
    void testConcurrentUpdates() throws InterruptedException {
        Thread[] threads = new Thread[10];
        
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    data.addDamageDealt(1.0);
                    data.incrementCombats();
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 10 threads * 100 iterations = 1000
        assertEquals(1000.0, data.getTotalDamageDealt(), 0.001);
        assertEquals(1000, data.getTotalCombats());
    }
}
