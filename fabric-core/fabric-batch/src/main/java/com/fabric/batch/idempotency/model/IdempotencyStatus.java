package com.fabric.batch.idempotency.model;

/**
 * Enumeration of possible idempotency operation statuses.
 * Provides comprehensive status tracking for all idempotent operations
 * with clear descriptions for monitoring and debugging.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
public enum IdempotencyStatus {
    SUCCESS("Operation completed successfully"),
    CACHED_RESULT("Result retrieved from cache"),
    IN_PROGRESS("Operation is currently being processed"),
    FAILED("Operation failed but can be retried"),
    MAX_RETRIES_EXCEEDED("Maximum retry attempts exceeded"),
    EXPIRED("Idempotency key has expired"),
    INVALID_REQUEST("Request is invalid or malformed");
    
    private final String description;
    
    IdempotencyStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if this status represents a successful operation
     */
    public boolean isSuccessful() {
        return this == SUCCESS || this == CACHED_RESULT;
    }
    
    /**
     * Checks if this status represents a failed operation
     */
    public boolean isFailed() {
        return this == FAILED || this == MAX_RETRIES_EXCEEDED;
    }
    
    /**
     * Checks if this status represents an operation in progress
     */
    public boolean isInProgress() {
        return this == IN_PROGRESS;
    }
    
    /**
     * Checks if this status represents a terminal state
     */
    public boolean isTerminal() {
        return this != IN_PROGRESS;
    }
    
    /**
     * Checks if retry is possible for this status
     */
    public boolean canRetry() {
        return this == FAILED;
    }
}