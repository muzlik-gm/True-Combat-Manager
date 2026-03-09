package com.muzlik.pvpcombat.utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Circuit breaker pattern implementation for handling failing operations.
 * Prevents cascading failures by temporarily disabling operations that are failing repeatedly.
 */
public class CircuitBreaker {
    
    private final String name;
    private final int failureThreshold;
    private final long timeoutMillis;
    private final AtomicInteger failureCount;
    private final AtomicLong lastFailureTime;
    private volatile CircuitState state;
    
    /**
     * Circuit breaker states.
     */
    public enum CircuitState {
        CLOSED,      // Normal operation
        OPEN,        // Circuit is open, operations are blocked
        HALF_OPEN    // Testing if the circuit can be closed again
    }
    
    /**
     * Creates a new circuit breaker.
     * 
     * @param name Name of the circuit breaker for logging
     * @param failureThreshold Number of failures before opening the circuit
     * @param timeoutMillis Time in milliseconds before attempting to close the circuit
     */
    public CircuitBreaker(String name, int failureThreshold, long timeoutMillis) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.timeoutMillis = timeoutMillis;
        this.failureCount = new AtomicInteger(0);
        this.lastFailureTime = new AtomicLong(0);
        this.state = CircuitState.CLOSED;
    }
    
    /**
     * Checks if the circuit is open (operations should be blocked).
     * 
     * @return true if circuit is open, false otherwise
     */
    public boolean isOpen() {
        if (state == CircuitState.OPEN) {
            // Check if timeout has elapsed
            long currentTime = System.currentTimeMillis();
            long timeSinceLastFailure = currentTime - lastFailureTime.get();
            
            if (timeSinceLastFailure >= timeoutMillis) {
                // Try to transition to half-open state
                state = CircuitState.HALF_OPEN;
                return false;
            }
            return true;
        }
        return false;
    }
    
    /**
     * Records a successful operation.
     * Resets the failure count and closes the circuit if it was half-open.
     */
    public void recordSuccess() {
        failureCount.set(0);
        if (state == CircuitState.HALF_OPEN) {
            state = CircuitState.CLOSED;
        }
    }
    
    /**
     * Records a failed operation.
     * Increments the failure count and opens the circuit if threshold is reached.
     */
    public void recordFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        if (failures >= failureThreshold) {
            state = CircuitState.OPEN;
        }
    }
    
    /**
     * Manually resets the circuit breaker to closed state.
     */
    public void reset() {
        failureCount.set(0);
        state = CircuitState.CLOSED;
    }
    
    /**
     * Gets the current state of the circuit.
     * 
     * @return Current circuit state
     */
    public CircuitState getState() {
        return state;
    }
    
    /**
     * Gets the current failure count.
     * 
     * @return Number of consecutive failures
     */
    public int getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * Gets the name of this circuit breaker.
     * 
     * @return Circuit breaker name
     */
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return String.format("CircuitBreaker[name=%s, state=%s, failures=%d/%d]",
            name, state, failureCount.get(), failureThreshold);
    }
}
