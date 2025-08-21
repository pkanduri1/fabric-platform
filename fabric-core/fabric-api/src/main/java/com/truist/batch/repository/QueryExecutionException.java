package com.truist.batch.repository;

/**
 * =========================================================================
 * QUERY EXECUTION EXCEPTION - BANKING GRADE EXECUTION FAILURE
 * =========================================================================
 * 
 * Purpose: Specialized exception for master query execution failures
 * - Thrown when queries fail during execution
 * - Contains correlation ID for audit trail compliance
 * - Provides error classification for proper handling
 * - Supports secure error messaging patterns
 * 
 * Security Features:
 * - No sensitive data exposure in error messages
 * - Correlation ID tracking for audit compliance
 * - Error classification for monitoring and alerting
 * 
 * Enterprise Standards:
 * - SOX-compliant audit trail support
 * - Banking error handling patterns
 * - Performance monitoring integration
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Phase 1 - Master Query Execution Implementation
 * =========================================================================
 */
public class QueryExecutionException extends RuntimeException {

    private final String correlationId;
    private final String errorCode;
    private final String executionPhase;

    /**
     * Constructor with message, correlation ID, and error code.
     * 
     * @param message Error message (must not contain sensitive data)
     * @param correlationId Correlation ID for audit trail tracking
     * @param errorCode Error classification code
     */
    public QueryExecutionException(String message, String correlationId, String errorCode) {
        super(message);
        this.correlationId = correlationId;
        this.errorCode = errorCode;
        this.executionPhase = "EXECUTION";
    }

    /**
     * Constructor with message, correlation ID, error code, and cause.
     * 
     * @param message Error message
     * @param correlationId Correlation ID for audit trail tracking
     * @param errorCode Error classification code
     * @param cause Root cause exception
     */
    public QueryExecutionException(String message, String correlationId, String errorCode, Throwable cause) {
        super(message, cause);
        this.correlationId = correlationId;
        this.errorCode = errorCode;
        this.executionPhase = "EXECUTION";
    }

    /**
     * Constructor with message, correlation ID, error code, execution phase, and cause.
     * 
     * @param message Error message
     * @param correlationId Correlation ID for audit trail tracking
     * @param errorCode Error classification code
     * @param executionPhase Phase during which error occurred
     * @param cause Root cause exception
     */
    public QueryExecutionException(String message, String correlationId, String errorCode, String executionPhase, Throwable cause) {
        super(message, cause);
        this.correlationId = correlationId;
        this.errorCode = errorCode;
        this.executionPhase = executionPhase;
    }

    /**
     * Get correlation ID for audit trail tracking.
     * 
     * @return Correlation ID
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Get error code for classification and monitoring.
     * 
     * @return Error classification code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Get execution phase where error occurred.
     * 
     * @return Execution phase
     */
    public String getExecutionPhase() {
        return executionPhase;
    }
}