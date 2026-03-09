package com.muzlik.pvpcombat.utils;

import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Utility class for retrying operations with exponential backoff.
 * Helps handle transient failures gracefully.
 */
public class RetryUtils {
    
    private static final Logger LOGGER = Logger.getLogger(RetryUtils.class.getName());
    
    /**
     * Retries an operation with exponential backoff.
     * 
     * @param operation The operation to retry
     * @param maxRetries Maximum number of retry attempts
     * @param initialDelayMs Initial delay in milliseconds
     * @param operationName Name of the operation for logging
     * @param <T> Return type of the operation
     * @return Result of the operation
     * @throws RuntimeException if all retries are exhausted
     */
    public static <T> T retryWithExponentialBackoff(Supplier<T> operation, int maxRetries,
                                                     long initialDelayMs, String operationName) {
        int attempt = 0;
        long delay = initialDelayMs;
        
        while (attempt < maxRetries) {
            try {
                return operation.get();
            } catch (Exception e) {
                attempt++;
                
                if (attempt >= maxRetries) {
                    LOGGER.severe(String.format("Operation '%s' failed after %d attempts: %s",
                        operationName, maxRetries, e.getMessage()));
                    throw new RuntimeException("Max retries exceeded for " + operationName, e);
                }
                
                LOGGER.warning(String.format("Operation '%s' failed (attempt %d/%d): %s. Retrying in %dms...",
                    operationName, attempt, maxRetries, e.getMessage(), delay));
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
                
                // Exponential backoff: double the delay for next attempt
                delay *= 2;
            }
        }
        
        throw new RuntimeException("Unexpected retry loop exit");
    }
    
    /**
     * Retries an operation with exponential backoff, using default parameters.
     * 
     * @param operation The operation to retry
     * @param operationName Name of the operation for logging
     * @param <T> Return type of the operation
     * @return Result of the operation
     */
    public static <T> T retry(Supplier<T> operation, String operationName) {
        return retryWithExponentialBackoff(operation, 3, 1000, operationName);
    }
    
    /**
     * Retries a void operation with exponential backoff.
     * 
     * @param operation The operation to retry
     * @param maxRetries Maximum number of retry attempts
     * @param initialDelayMs Initial delay in milliseconds
     * @param operationName Name of the operation for logging
     */
    public static void retryVoid(Runnable operation, int maxRetries,
                                 long initialDelayMs, String operationName) {
        retryWithExponentialBackoff(() -> {
            operation.run();
            return null;
        }, maxRetries, initialDelayMs, operationName);
    }
    
    /**
     * Retries a void operation with default parameters.
     * 
     * @param operation The operation to retry
     * @param operationName Name of the operation for logging
     */
    public static void retryVoid(Runnable operation, String operationName) {
        retryVoid(operation, 3, 1000, operationName);
    }
}
