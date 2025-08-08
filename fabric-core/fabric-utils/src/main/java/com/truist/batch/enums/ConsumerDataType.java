package com.truist.batch.enums;

import java.util.regex.Pattern;

/**
 * Consumer Default Data Types Enumeration
 * 
 * Defines specialized data types for consumer lending and default operations
 * with built-in validation patterns and formatting rules.
 * 
 * @author Claude Code - Senior Full Stack Developer
 * @version 1.0
 * @since 2025-08-02
 */
public enum ConsumerDataType {
    
    // Standard data types (inherited from existing system)
    STRING("String", "^.{1,255}$", "Standard text field", PIIClassification.NONE, false),
    INTEGER("Integer", "^-?\\d{1,10}$", "Whole number", PIIClassification.NONE, false),
    BIGDECIMAL("BigDecimal", "^-?\\d{1,15}(\\.\\d{1,4})?$", "Decimal number with precision", PIIClassification.NONE, false),
    DATE("Date", "^\\d{4}-\\d{2}-\\d{2}$|^\\d{8}$", "Date in YYYY-MM-DD or YYYYMMDD format", PIIClassification.NONE, false),
    LONG("Long", "^-?\\d{1,19}$", "Long integer", PIIClassification.NONE, false),
    
    // Consumer-specific data types (enhanced for lending operations)
    CONSUMER_SSN("Consumer SSN", "^\\d{3}-?\\d{2}-?\\d{4}$", "Social Security Number with masking", PIIClassification.HIGH, true),
    CONSUMER_PHONE("Consumer Phone", "^\\+?1?[-.\\s]?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}$", "Phone number with formatting", PIIClassification.MEDIUM, false),
    CONSUMER_EMAIL("Consumer Email", "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", "Email address with validation", PIIClassification.MEDIUM, false),
    CONSUMER_ADDRESS("Consumer Address", "^.{5,255}$", "Physical address", PIIClassification.MEDIUM, false),
    CONSUMER_INCOME("Consumer Income", "^\\d{1,8}(\\.\\d{2})?$", "Annual income in dollars", PIIClassification.MEDIUM, false),
    CONSUMER_CREDIT_SCORE("Consumer Credit Score", "^[3-8]\\d{2}$", "Credit score (300-850)", PIIClassification.HIGH, false),
    
    // Lending-specific data types
    LOAN_AMOUNT("Loan Amount", "^\\d{1,10}(\\.\\d{2})?$", "Loan amount in dollars", PIIClassification.LOW, false),
    INTEREST_RATE("Interest Rate", "^\\d{1,2}(\\.\\d{3})?%?$", "Interest rate percentage", PIIClassification.NONE, false),
    DTI_RATIO("DTI Ratio", "^\\d{1,2}(\\.\\d{2})?%?$", "Debt-to-Income ratio percentage", PIIClassification.LOW, false),
    LTV_RATIO("LTV Ratio", "^\\d{1,3}(\\.\\d{2})?%?$", "Loan-to-Value ratio percentage", PIIClassification.LOW, false),
    PROPERTY_VALUE("Property Value", "^\\d{1,10}(\\.\\d{2})?$", "Property value in dollars", PIIClassification.LOW, false),
    
    // Default management data types
    DEFAULT_STATUS("Default Status", "^(CURRENT|30_DAYS|60_DAYS|90_DAYS|120_PLUS|CHARGED_OFF)$", "Loan default status", PIIClassification.LOW, false),
    PAYMENT_HISTORY("Payment History", "^[01C]{1,24}$", "Payment history (0=current, 1=late, C=charged off)", PIIClassification.MEDIUM, false),
    RECOVERY_AMOUNT("Recovery Amount", "^\\d{1,10}(\\.\\d{2})?$", "Amount recovered from default", PIIClassification.LOW, false),
    
    // Employment and income verification
    EMPLOYMENT_STATUS("Employment Status", "^(EMPLOYED|UNEMPLOYED|SELF_EMPLOYED|RETIRED|STUDENT|OTHER)$", "Employment status", PIIClassification.LOW, false),
    EMPLOYER_NAME("Employer Name", "^.{2,100}$", "Employer name", PIIClassification.MEDIUM, false),
    INCOME_TYPE("Income Type", "^(W2|1099|BANK_STATEMENT|OTHER)$", "Type of income verification", PIIClassification.LOW, false),
    
    // Regulatory compliance fields
    HMDA_ETHNICITY("HMDA Ethnicity", "^[1-5]$", "HMDA ethnicity code", PIIClassification.HIGH, false),
    HMDA_RACE("HMDA Race", "^[1-7]$", "HMDA race code", PIIClassification.HIGH, false),
    HMDA_SEX("HMDA Sex", "^[1-4]$", "HMDA sex code", PIIClassification.HIGH, false);
    
    private final String displayName;
    private final String validationPattern;
    private final String description;
    private final PIIClassification piiClassification;
    private final boolean requiresMasking;
    private final Pattern compiledPattern;
    
    ConsumerDataType(String displayName, String validationPattern, String description, 
                    PIIClassification piiClassification, boolean requiresMasking) {
        this.displayName = displayName;
        this.validationPattern = validationPattern;
        this.description = description;
        this.piiClassification = piiClassification;
        this.requiresMasking = requiresMasking;
        this.compiledPattern = Pattern.compile(validationPattern);
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getValidationPattern() {
        return validationPattern;
    }
    
    public String getDescription() {
        return description;
    }
    
    public PIIClassification getPiiClassification() {
        return piiClassification;
    }
    
    public boolean requiresMasking() {
        return requiresMasking;
    }
    
    /**
     * Validate a value against this data type's pattern
     */
    public boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        return compiledPattern.matcher(value.trim()).matches();
    }
    
    /**
     * Format a value according to this data type's formatting rules
     */
    public String format(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        
        String trimmedValue = value.trim();
        
        switch (this) {
            case CONSUMER_SSN:
                // Format SSN as XXX-XX-XXXX
                String cleanSSN = trimmedValue.replaceAll("[^\\d]", "");
                if (cleanSSN.length() == 9) {
                    return cleanSSN.substring(0, 3) + "-" + cleanSSN.substring(3, 5) + "-" + cleanSSN.substring(5);
                }
                return trimmedValue;
                
            case CONSUMER_PHONE:
                // Format phone as (XXX) XXX-XXXX
                String cleanPhone = trimmedValue.replaceAll("[^\\d]", "");
                if (cleanPhone.length() == 10) {
                    return "(" + cleanPhone.substring(0, 3) + ") " + cleanPhone.substring(3, 6) + "-" + cleanPhone.substring(6);
                } else if (cleanPhone.length() == 11 && cleanPhone.startsWith("1")) {
                    return "(" + cleanPhone.substring(1, 4) + ") " + cleanPhone.substring(4, 7) + "-" + cleanPhone.substring(7);
                }
                return trimmedValue;
                
            case CONSUMER_INCOME:
            case LOAN_AMOUNT:
            case PROPERTY_VALUE:
            case RECOVERY_AMOUNT:
                // Format currency amounts
                try {
                    double amount = Double.parseDouble(trimmedValue.replaceAll("[^\\d.]", ""));
                    return String.format("%.2f", amount);
                } catch (NumberFormatException e) {
                    return trimmedValue;
                }
                
            case INTEREST_RATE:
            case DTI_RATIO:
            case LTV_RATIO:
                // Format percentages
                String cleanPercent = trimmedValue.replaceAll("[^\\d.]", "");
                if (!cleanPercent.isEmpty()) {
                    return cleanPercent + (trimmedValue.contains("%") ? "%" : "");
                }
                return trimmedValue;
                
            default:
                return trimmedValue;
        }
    }
    
    /**
     * Mask a value for display in non-production environments or to unauthorized users
     */
    public String mask(String value) {
        if (value == null || value.trim().isEmpty() || !requiresMasking) {
            return value;
        }
        
        String trimmedValue = value.trim();
        
        switch (this) {
            case CONSUMER_SSN:
                // Mask SSN as XXX-XX-1234 (show last 4 digits)
                String cleanSSN = trimmedValue.replaceAll("[^\\d]", "");
                if (cleanSSN.length() == 9) {
                    return "XXX-XX-" + cleanSSN.substring(5);
                }
                return "XXX-XX-XXXX";
                
            default:
                // Generic masking - show first and last characters
                if (trimmedValue.length() <= 2) {
                    return "X".repeat(trimmedValue.length());
                } else if (trimmedValue.length() <= 6) {
                    return trimmedValue.charAt(0) + "X".repeat(trimmedValue.length() - 2) + trimmedValue.charAt(trimmedValue.length() - 1);
                } else {
                    return trimmedValue.substring(0, 2) + "X".repeat(trimmedValue.length() - 4) + trimmedValue.substring(trimmedValue.length() - 2);
                }
        }
    }
    
    /**
     * Get maximum recommended field length for this data type
     */
    public int getMaxLength() {
        switch (this) {
            case CONSUMER_SSN:
                return 11; // XXX-XX-XXXX
            case CONSUMER_PHONE:
                return 14; // (XXX) XXX-XXXX
            case CONSUMER_EMAIL:
                return 255;
            case CONSUMER_ADDRESS:
                return 255;
            case CONSUMER_INCOME:
            case LOAN_AMOUNT:
            case PROPERTY_VALUE:
            case RECOVERY_AMOUNT:
                return 12; // Up to 999,999,999.99
            case CONSUMER_CREDIT_SCORE:
                return 3; // 300-850
            case INTEREST_RATE:
            case DTI_RATIO:
            case LTV_RATIO:
                return 6; // XX.XXX%
            case PAYMENT_HISTORY:
                return 24; // 24 months of history
            case EMPLOYER_NAME:
                return 100;
            default:
                return 50;
        }
    }
    
    /**
     * Check if this data type is consumer-specific
     */
    public boolean isConsumerSpecific() {
        return name().startsWith("CONSUMER_") || 
               name().startsWith("LOAN_") || 
               name().startsWith("DTI_") || 
               name().startsWith("LTV_") || 
               name().startsWith("DEFAULT_") || 
               name().startsWith("PAYMENT_") || 
               name().startsWith("RECOVERY_") || 
               name().startsWith("EMPLOYMENT_") || 
               name().startsWith("EMPLOYER_") || 
               name().startsWith("INCOME_") || 
               name().startsWith("HMDA_") ||
               name().startsWith("PROPERTY_");
    }
    
    /**
     * Get data type by name (case-insensitive)
     */
    public static ConsumerDataType fromString(String name) {
        if (name == null || name.trim().isEmpty()) {
            return STRING;
        }
        
        try {
            return ConsumerDataType.valueOf(name.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            // Try to match by display name
            for (ConsumerDataType dataType : values()) {
                if (dataType.displayName.equalsIgnoreCase(name.trim())) {
                    return dataType;
                }
            }
            // Default to STRING for unknown types
            return STRING;
        }
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}