package com.truist.batch.enums;

/**
 * Template Category Enumeration for Consumer Default Operations
 * 
 * Defines the various categories of templates supported by the enhanced
 * template management system for consumer lending and default operations.
 * 
 * @author Claude Code - Senior Full Stack Developer
 * @version 1.0
 * @since 2025-08-02
 */
public enum TemplateCategory {
    
    /**
     * General purpose templates (default/legacy category)
     */
    GENERAL("General Purpose", "Standard templates for general use"),
    
    /**
     * Consumer default management templates
     */
    CONSUMER_DEFAULT("Consumer Default", "Templates for consumer default processing and management"),
    
    /**
     * Mortgage application and processing templates
     */
    MORTGAGE_APPLICATION("Mortgage Application", "Templates for mortgage loan applications and processing"),
    
    /**
     * Personal loan templates
     */
    PERSONAL_LOAN("Personal Loan", "Templates for personal loan applications and processing"),
    
    /**
     * Credit card application templates
     */
    CREDIT_CARD_APPLICATION("Credit Card Application", "Templates for credit card applications and processing"),
    
    /**
     * Default management and recovery templates
     */
    DEFAULT_MANAGEMENT("Default Management", "Templates for default management and recovery processes"),
    
    /**
     * Commercial lending templates
     */
    COMMERCIAL_LENDING("Commercial Lending", "Templates for commercial and business lending"),
    
    /**
     * Regulatory reporting templates
     */
    REGULATORY_REPORTING("Regulatory Reporting", "Templates for regulatory compliance and reporting");
    
    private final String displayName;
    private final String description;
    
    TemplateCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this category is consumer-related
     */
    public boolean isConsumerCategory() {
        return this == CONSUMER_DEFAULT || 
               this == MORTGAGE_APPLICATION || 
               this == PERSONAL_LOAN || 
               this == CREDIT_CARD_APPLICATION ||
               this == DEFAULT_MANAGEMENT;
    }
    
    /**
     * Check if this category requires enhanced PII protection
     */
    public boolean requiresEnhancedPIIProtection() {
        return isConsumerCategory();
    }
    
    /**
     * Get template category by name (case-insensitive)
     */
    public static TemplateCategory fromString(String name) {
        if (name == null || name.trim().isEmpty()) {
            return GENERAL;
        }
        
        try {
            return TemplateCategory.valueOf(name.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            // Try to match by display name
            for (TemplateCategory category : values()) {
                if (category.displayName.equalsIgnoreCase(name.trim())) {
                    return category;
                }
            }
            return GENERAL;
        }
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}