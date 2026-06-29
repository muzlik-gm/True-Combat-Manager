package com.muzlik.pvpcombat.utils;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CircuitBreakerTest {

    private static final String CB_NAME = "test-breaker";
    private static final int THRESHOLD = 3;
    private static final long TIMEOUT_MS = 50;

    private CircuitBreaker breaker;

    @BeforeEach
    void setUp() {
        breaker = new CircuitBreaker(CB_NAME, THRESHOLD, TIMEOUT_MS);
    }

    @Test
    @Order(1)
    @DisplayName("Circuit breaker should initialize in CLOSED state")
    void testInitialState() {
        assertEquals(CircuitBreaker.CircuitState.CLOSED, breaker.getState());
        assertEquals(0, breaker.getFailureCount());
        assertEquals(CB_NAME, breaker.getName());
    }

    @Test
    @Order(2)
    @DisplayName("recordSuccess should keep state CLOSED when already CLOSED")
    void testRecordSuccessKeepsClosed() {
        breaker.recordSuccess();
        assertEquals(CircuitBreaker.CircuitState.CLOSED, breaker.getState());
        assertEquals(0, breaker.getFailureCount());
    }

    @Test
    @Order(3)
    @DisplayName("Failure count should increment below threshold")
    void testFailuresBelowThreshold() {
        breaker.recordFailure();
        assertEquals(1, breaker.getFailureCount());
        assertEquals(CircuitBreaker.CircuitState.CLOSED, breaker.getState());

        breaker.recordFailure();
        assertEquals(2, breaker.getFailureCount());
        assertEquals(CircuitBreaker.CircuitState.CLOSED, breaker.getState());
    }

    @Test
    @Order(4)
    @DisplayName("Circuit should open when failure threshold is reached")
    void testCircuitOpensAtThreshold() {
        for (int i = 0; i < THRESHOLD; i++) {
            breaker.recordFailure();
        }
        assertEquals(THRESHOLD, breaker.getFailureCount());
        assertEquals(CircuitBreaker.CircuitState.OPEN, breaker.getState());
    }

    @Test
    @Order(5)
    @DisplayName("isOpen should return true when circuit is OPEN")
    void testIsOpenWhenOpen() {
        for (int i = 0; i < THRESHOLD; i++) {
            breaker.recordFailure();
        }
        assertTrue(breaker.isOpen());
    }

    @Test
    @Order(6)
    @DisplayName("isOpen should return false when circuit is CLOSED")
    void testIsOpenWhenClosed() {
        assertFalse(breaker.isOpen());
    }

    @Test
    @Order(7)
    @DisplayName("isOpen should transition OPEN to HALF_OPEN after timeout")
    void testTransitionToHalfOpen() throws InterruptedException {
        for (int i = 0; i < THRESHOLD; i++) {
            breaker.recordFailure();
        }
        assertEquals(CircuitBreaker.CircuitState.OPEN, breaker.getState());

        Thread.sleep(TIMEOUT_MS + 20);

        assertFalse(breaker.isOpen());
        assertEquals(CircuitBreaker.CircuitState.HALF_OPEN, breaker.getState());
    }

    @Test
    @Order(8)
    @DisplayName("recordSuccess should transition HALF_OPEN back to CLOSED")
    void testRecordSuccessFromHalfOpen() throws InterruptedException {
        for (int i = 0; i < THRESHOLD; i++) {
            breaker.recordFailure();
        }
        Thread.sleep(TIMEOUT_MS + 20);
        breaker.isOpen();
        assertEquals(CircuitBreaker.CircuitState.HALF_OPEN, breaker.getState());

        breaker.recordSuccess();
        assertEquals(CircuitBreaker.CircuitState.CLOSED, breaker.getState());
        assertEquals(0, breaker.getFailureCount());
    }

    @Test
    @Order(9)
    @DisplayName("reset should return circuit to CLOSED state")
    void testReset() {
        for (int i = 0; i < THRESHOLD; i++) {
            breaker.recordFailure();
        }
        assertEquals(CircuitBreaker.CircuitState.OPEN, breaker.getState());

        breaker.reset();
        assertEquals(CircuitBreaker.CircuitState.CLOSED, breaker.getState());
        assertEquals(0, breaker.getFailureCount());
    }

    @Test
    @Order(10)
    @DisplayName("toString should format correctly")
    void testToString() {
        String s = breaker.toString();
        assertTrue(s.contains(CB_NAME));
        assertTrue(s.contains("CLOSED"));
        assertTrue(s.contains("0/3"));

        breaker.recordFailure();
        s = breaker.toString();
        assertTrue(s.contains("1/3"));
    }

    @Test
    @Order(11)
    @DisplayName("getState should reflect current state")
    void testGetState() {
        assertEquals(CircuitBreaker.CircuitState.CLOSED, breaker.getState());
        for (int i = 0; i < THRESHOLD; i++) {
            breaker.recordFailure();
        }
        assertEquals(CircuitBreaker.CircuitState.OPEN, breaker.getState());
    }
}
