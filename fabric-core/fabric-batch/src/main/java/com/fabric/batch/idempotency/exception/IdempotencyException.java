package com.fabric.batch.idempotency.exception;

/**
 * Exception thrown when idempotency operations fail.
 * Provides comprehensive error information for debugging and monitoring
 * of idempotent operation failures.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
public class IdempotencyException extends RuntimeException {
    
    private final String correlationId;
    private final String idempotencyKey;
    private final String errorCode;
    private final boolean retryable;
    
    public IdempotencyException(String message) {
        super(message);
        this.correlationId = null;
        this.idempotencyKey = null;
        this.errorCode = null;
        this.retryable = false;
    }
    
    public IdempotencyException(String message, String correlationId) {
        super(message);
        this.correlationId = correlationId;
        this.idempotencyKey = null;
        this.errorCode = null;
        this.retryable = false;
    }
    
    public IdempotencyException(String message, String correlationId, Throwable cause) {
        super(message, cause);
        this.correlationId = correlationId;
        this.idempotencyKey = null;
        this.errorCode = null;
        this.retryable = false;
    }
    
    public IdempotencyException(String message, String correlationId, String idempotencyKey, 
                               String errorCode, boolean retryable) {
        super(message);
        this.correlationId = correlationId;
        this.idempotencyKey = idempotencyKey;
        this.errorCode = errorCode;
        this.retryable = retryable;
    }
    
    public IdempotencyException(String message, String correlationId, String idempotencyKey, 
                               String errorCode, boolean retryable, Throwable cause) {
        super(message, cause);
        this.correlationId = correlationId;
        this.idempotencyKey = idempotencyKey;
        this.errorCode = errorCode;
        this.retryable = retryable;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("[message=").append(getMessage());
        if (correlationId != null) sb.append(", correlationId=").append(correlationId);
        if (idempotencyKey != null) sb.append(", idempotencyKey=").append(idempotencyKey);
        if (errorCode != null) sb.append(", errorCode=").append(errorCode);
        sb.append(", retryable=").append(retryable);
        sb.append("]");
        return sb.toString();
    }
}