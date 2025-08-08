package com.truist.batch.enums;

/**
 * PII Classification Enumeration for Data Protection
 * 
 * Defines the classification levels for Personally Identifiable Information (PII)
 * to ensure proper data protection and compliance with privacy regulations.
 * 
 * @author Claude Code - Senior Full Stack Developer  
 * @version 1.0
 * @since 2025-08-02
 */
public enum PIIClassification {
    
    /**
     * No PII - Public information
     */
    NONE("None", "No personally identifiable information", false, false),
    
    /**
     * Low sensitivity PII - General consumer information
     */
    LOW("Low", "Low sensitivity PII - general consumer data", false, true),
    
    /**
     * Medium sensitivity PII - Financial information
     */
    MEDIUM("Medium", "Medium sensitivity PII - financial and contact information", true, true),
    
    /**
     * High sensitivity PII - Critical identification data
     */
    HIGH("High", "High sensitivity PII - SSN, account numbers, critical identification", true, true);
    
    private final String displayName;
    private final String description;
    private final boolean requiresEncryption;
    private final boolean requiresAuditLogging;
    
    PIIClassification(String displayName, String description, boolean requiresEncryption, boolean requiresAuditLogging) {
        this.displayName = displayName;
        this.description = description;
        this.requiresEncryption = requiresEncryption;
        this.requiresAuditLogging = requiresAuditLogging;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean requiresEncryption() {
        return requiresEncryption;
    }
    
    public boolean requiresAuditLogging() {
        return requiresAuditLogging;
    }
    
    /**
     * Check if this classification level requires masking in non-production environments
     */
    public boolean requiresMasking() {
        return this == MEDIUM || this == HIGH;
    }
    
    /**
     * Check if this classification requires special handling
     */
    public boolean requiresSpecialHandling() {
        return this != NONE;
    }
    
    /**
     * Get minimum access level required to view this PII classification
     */
    public String getMinimumAccessLevel() {
        switch (this) {
            case HIGH:
                return "PII_HIGH_ACCESS";
            case MEDIUM:
                return "PII_MEDIUM_ACCESS";
            case LOW:
                return "PII_LOW_ACCESS";
            default:
                return "PUBLIC_ACCESS";
        }
    }
    
    /**
     * Get data retention period in months for this PII classification
     */
    public int getRetentionPeriodMonths() {
        switch (this) {
            case HIGH:
                return 84; // 7 years for high sensitivity data
            case MEDIUM:
                return 60; // 5 years for medium sensitivity data
            case LOW:
                return 36; // 3 years for low sensitivity data
            default:
                return 12; // 1 year for non-PII data
        }
    }
    
    /**
     * Get PII classification by name (case-insensitive)
     */
    public static PIIClassification fromString(String name) {
        if (name == null || name.trim().isEmpty()) {
            return NONE;
        }
        
        try {
            return PIIClassification.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try to match by display name
            for (PIIClassification classification : values()) {
                if (classification.displayName.equalsIgnoreCase(name.trim())) {
                    return classification;
                }
            }
            return NONE;
        }
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}