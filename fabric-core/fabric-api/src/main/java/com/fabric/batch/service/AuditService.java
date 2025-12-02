package com.fabric.batch.service;

import java.util.Map;

/**
 * Service interface for audit logging operations.
 * Provides SOX-compliant audit trail functionality for configuration changes,
 * user actions, and security events.
 */
public interface AuditService {
    
    /**
     * Log configuration creation event
     * @param configId Unique identifier for the configuration
     * @param newValue The new configuration value
     * @param changedBy User who made the change
     * @param reason Business reason for the change
     */
    void logCreate(String configId, Object newValue, String changedBy, String reason);
    
    /**
     * Log configuration update event
     * @param configId Unique identifier for the configuration
     * @param oldValue The previous configuration value
     * @param newValue The new configuration value
     * @param changedBy User who made the change
     * @param reason Business reason for the change
     */
    void logUpdate(String configId, Object oldValue, Object newValue, String changedBy, String reason);
    
    /**
     * Log configuration deletion event
     * @param configId Unique identifier for the configuration
     * @param oldValue The deleted configuration value
     * @param changedBy User who made the change
     * @param reason Business reason for the change
     */
    void logDelete(String configId, Object oldValue, String changedBy, String reason);
    
    /**
     * Log security event with String parameters
     * @param eventType Type of security event
     * @param description Description of the event
     * @param changedBy User associated with the event
     * @param additionalData Additional security context data
     */
    void logSecurityEvent(String eventType, String description, String changedBy, Map<String, String> additionalData);
    
    /**
     * Log security event with Object parameters
     * @param eventType Type of security event
     * @param description Description of the event
     * @param changedBy User associated with the event
     * @param additionalData Additional security context data
     */
    void logSecurityEventWithObjects(String eventType, String description, String changedBy, Map<String, ? extends Object> additionalData);
}
