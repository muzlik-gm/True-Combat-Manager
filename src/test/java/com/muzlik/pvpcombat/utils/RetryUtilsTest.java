package com.muzlik.pvpcombat.utils;

import org.junit.jupiter.api.*;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RetryUtilsTest {

    @Test
    @Order(1)
    @DisplayName("retryWithExponentialBackoff should return result on first attempt")
    void testSuccessOnFirstAttempt() {
        String result = RetryUtils.retryWithExponentialBackoff(
            () -> "success", 3, 1, "test-op");
        assertEquals("success", result);
    }

    @Test
    @Order(2)
    @DisplayName("retryWithExponentialBackoff should succeed after multiple failures")
    void testSuccessAfterRetries() {
        AtomicInteger attempts = new AtomicInteger(0);
        String result = RetryUtils.retryWithExponentialBackoff(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RuntimeException("transient error");
            }
            return "success";
        }, 4, 1, "test-retry");
        assertEquals("success", result);
        assertEquals(3, attempts.get());
    }

    @Test
    @Order(3)
    @DisplayName("retryWithExponentialBackoff should throw when all retries exhausted")
    void testExhaustion() {
        assertThrows(RuntimeException.class, () ->
            RetryUtils.retryWithExponentialBackoff(
                () -> { throw new RuntimeException("persistent error"); },
                2, 1, "failing-op"));
    }

    @Test
    @Order(4)
    @DisplayName("retry should use default parameters")
    void testRetryDefault() {
        String result = RetryUtils.retry(() -> "ok", "default-op");
        assertEquals("ok", result);
    }

    @Test
    @Order(5)
    @DisplayName("retryVoid should work with explicit parameters")
    void testRetryVoidExplicit() {
        AtomicInteger counter = new AtomicInteger(0);
        RetryUtils.retryVoid(() -> {
            if (counter.incrementAndGet() < 2) {
                throw new RuntimeException("fail");
            }
        }, 3, 1, "void-op");
        assertEquals(2, counter.get());
    }

    @Test
    @Order(6)
    @DisplayName("retryVoid should work with default parameters")
    void testRetryVoidDefault() {
        AtomicInteger counter = new AtomicInteger(0);
        RetryUtils.retryVoid(() -> counter.incrementAndGet(), "default-void");
        assertEquals(1, counter.get());
    }

    @Test
    @Order(7)
    @DisplayName("retryWithExponentialBackoff should throw on interrupt")
    void testInterrupt() {
        Thread.currentThread().interrupt();
        assertThrows(RuntimeException.class, () ->
            RetryUtils.retryWithExponentialBackoff(
                () -> { throw new RuntimeException("fail"); },
                3, 100, "interrupt-op"));
        assertTrue(Thread.interrupted());
    }
}
