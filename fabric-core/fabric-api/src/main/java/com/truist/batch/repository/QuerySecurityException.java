package com.truist.batch.repository;

/**
 * =========================================================================
 * QUERY SECURITY EXCEPTION - BANKING GRADE SECURITY VIOLATION
 * =========================================================================
 * 
 * Purpose: Specialized exception for master query security violations
 * - Thrown when queries violate security policies
 * - Contains correlation ID for audit trail compliance
 * - Provides secure error messaging without exposing sensitive data
 * 
 * Security Features:
 * - No sensitive data exposure in error messages
 * - Correlation ID tracking for audit compliance
 * - Classification of security violation types
 * 
 * Enterprise Standards:
 * - SOX-compliant audit trail support
 * - Banking security policy enforcement
 * - Secure exception handling patterns
 * 
 * @author Senior Full Stack Developer Agent  
 * @version 1.0
 * @since Phase 1 - Master Query Security Implementation
 * =========================================================================
 */
public class QuerySecurityException extends RuntimeException {

    private final String correlationId;
    private final String securityViolationType;

    /**
     * Constructor with message and correlation ID.
     * 
     * @param message Security violation message (must not contain sensitive data)
     * @param correlationId Correlation ID for audit trail tracking
     */
    public QuerySecurityException(String message, String correlationId) {
        super(message);
        this.correlationId = correlationId;
        this.securityViolationType = "SECURITY_POLICY_VIOLATION";
    }

    /**
     * Constructor with message, correlation ID, and violation type.
     * 
     * @param message Security violation message
     * @param correlationId Correlation ID for audit trail tracking  
     * @param securityViolationType Type of security violation
     */
    public QuerySecurityException(String message, String correlationId, String securityViolationType) {
        super(message);
        this.correlationId = correlationId;
        this.securityViolationType = securityViolationType;
    }

    /**
     * Constructor with message, correlation ID, violation type, and cause.
     * 
     * @param message Security violation message
     * @param correlationId Correlation ID for audit trail tracking
     * @param securityViolationType Type of security violation
     * @param cause Root cause exception
     */
    public QuerySecurityException(String message, String correlationId, String securityViolationType, Throwable cause) {
        super(message, cause);
        this.correlationId = correlationId;
        this.securityViolationType = securityViolationType;
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
     * Get security violation type for classification.
     * 
     * @return Security violation type
     */
    public String getSecurityViolationType() {
        return securityViolationType;
    }
}