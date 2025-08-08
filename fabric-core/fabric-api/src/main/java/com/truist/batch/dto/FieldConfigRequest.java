package com.truist.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for SQL*Loader field configuration.
 * Contains validation rules for field-level security, data types, and transformations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldConfigRequest {
    
    @NotBlank(message = "Field name is required")
    @Size(max = 100, message = "Field name cannot exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "Field name must start with a letter and contain only alphanumeric characters and underscores")
    private String fieldName;
    
    @NotBlank(message = "Column name is required")
    @Size(max = 100, message = "Column name cannot exceed 100 characters")
    private String columnName;
    
    @Size(max = 100, message = "Source field cannot exceed 100 characters")
    private String sourceField;
    
    @Min(value = 1, message = "Field order must be at least 1")
    private Integer fieldOrder;
    
    @Min(value = 1, message = "Field position must be at least 1")
    private Integer fieldPosition; // For fixed-width files
    
    @Min(value = 1, message = "Field length must be at least 1")
    private Integer fieldLength; // For fixed-width files
    
    @NotNull(message = "Data type is required")
    @Builder.Default
    private String dataType = "CHAR"; // CHAR, VARCHAR, NUMBER, DATE, TIMESTAMP
    
    @Min(value = 1, message = "Max length must be at least 1")
    private Integer maxLength;
    
    @Size(max = 50, message = "Format mask cannot exceed 50 characters")
    private String formatMask; // Date/number format
    
    @Builder.Default
    private Boolean nullable = true;
    
    @Size(max = 100, message = "Default value cannot exceed 100 characters")
    private String defaultValue;
    
    @Size(max = 500, message = "SQL expression cannot exceed 500 characters")
    private String sqlExpression; // Custom SQL expression
    
    @Builder.Default
    private Boolean encrypted = false;
    
    @Size(max = 100, message = "Encryption function cannot exceed 100 characters")
    private String encryptionFunction;
    
    @Size(max = 500, message = "Validation rule cannot exceed 500 characters")
    private String validationRule; // Custom validation SQL
    
    @Size(max = 50, message = "Null if condition cannot exceed 50 characters")
    private String nullIfCondition;
    
    @Builder.Default
    private Boolean trimEnabled = false;
    
    @Builder.Default
    private String caseSensitive = "PRESERVE"; // PRESERVE, UPPER, LOWER
    
    @Size(max = 20, message = "Character set cannot exceed 20 characters")
    private String characterSet;
    
    @Size(max = 500, message = "Check constraint cannot exceed 500 characters")
    private String checkConstraint;
    
    @Size(max = 100, message = "Lookup table cannot exceed 100 characters")
    private String lookupTable;
    
    @Size(max = 100, message = "Lookup column cannot exceed 100 characters")
    private String lookupColumn;
    
    @Builder.Default
    private Boolean uniqueConstraint = false;
    
    @Builder.Default
    private Boolean primaryKey = false;
    
    @Size(max = 200, message = "Business rule class cannot exceed 200 characters")
    private String businessRuleClass;
    
    @Size(max = 1000, message = "Business rule parameters cannot exceed 1000 characters")
    private String businessRuleParameters; // JSON format
    
    @Builder.Default
    private Boolean auditField = false;
    
    @Size(max = 100, message = "Transformation applied cannot exceed 100 characters")
    private String transformationApplied;
    
    @Size(max = 500, message = "Data lineage cannot exceed 500 characters")
    private String dataLineage;
    
    @Size(max = 1000, message = "Field description cannot exceed 1000 characters")
    private String description;
    
    // Data Quality Configuration
    @Builder.Default
    private Boolean dataQualityCheckEnabled = false;
    
    @Size(max = 500, message = "Data quality rule cannot exceed 500 characters")
    private String dataQualityRule;
    
    @Min(value = 0, message = "Error threshold cannot be negative")
    @Max(value = 100, message = "Error threshold cannot exceed 100")
    private Double errorThresholdPercentage;
    
    // PII and Security
    @Builder.Default
    private Boolean containsPii = false;
    
    @Size(max = 50, message = "PII classification cannot exceed 50 characters")
    private String piiClassification; // SSN, CREDIT_CARD, PHONE, EMAIL, etc.
    
    @Builder.Default
    private Boolean maskingRequired = false;
    
    @Size(max = 100, message = "Masking function cannot exceed 100 characters")
    private String maskingFunction;
    
    // Performance Hints
    @Builder.Default
    private Boolean indexRecommended = false;
    
    @Builder.Default
    private Boolean partitionKey = false;
    
    @Size(max = 100, message = "Compression hint cannot exceed 100 characters")
    private String compressionHint;
    
    /**
     * Validate field configuration for consistency and business rules.
     */
    public void validateFieldConfiguration() {
        // Validate fixed-width configuration
        if (fieldPosition != null || fieldLength != null) {
            if (fieldPosition == null || fieldLength == null) {
                throw new IllegalArgumentException("Both position and length must be specified for fixed-width fields");
            }
        }
        
        // Validate encryption requirements
        if (encrypted && (encryptionFunction == null || encryptionFunction.trim().isEmpty())) {
            throw new IllegalArgumentException("Encryption function is required when encryption is enabled");
        }
        
        // Validate PII requirements
        if (containsPii && !encrypted && !maskingRequired) {
            throw new IllegalArgumentException("PII fields must be either encrypted or masked");
        }
        
        // Validate primary key constraints
        if (primaryKey && nullable) {
            throw new IllegalArgumentException("Primary key fields cannot be nullable");
        }
        
        // Validate lookup configuration
        if (lookupTable != null && (lookupColumn == null || lookupColumn.trim().isEmpty())) {
            throw new IllegalArgumentException("Lookup column is required when lookup table is specified");
        }
        
        // Validate data type specific rules
        validateDataTypeRules();
        
        // Validate business rule configuration
        if (businessRuleClass != null && !businessRuleClass.trim().isEmpty()) {
            validateBusinessRuleClass();
        }
    }
    
    /**
     * Validate data type specific rules and format masks.
     */
    private void validateDataTypeRules() {
        if (dataType == null) return;
        
        switch (dataType.toUpperCase()) {
            case "NUMBER":
                if (formatMask != null && !isValidNumberFormat(formatMask)) {
                    throw new IllegalArgumentException("Invalid number format mask: " + formatMask);
                }
                break;
                
            case "DATE":
                if (formatMask != null && !isValidDateFormat(formatMask)) {
                    throw new IllegalArgumentException("Invalid date format mask: " + formatMask);
                }
                break;
                
            case "TIMESTAMP":
                if (formatMask != null && !isValidTimestampFormat(formatMask)) {
                    throw new IllegalArgumentException("Invalid timestamp format mask: " + formatMask);
                }
                break;
                
            case "CHAR":
            case "VARCHAR":
                if (maxLength == null || maxLength <= 0) {
                    throw new IllegalArgumentException("Max length is required for character fields");
                }
                break;
        }
    }
    
    /**
     * Validate business rule class name.
     */
    private void validateBusinessRuleClass() {
        // Check if it's a valid Java class name pattern
        if (!businessRuleClass.matches("^[a-zA-Z_$][a-zA-Z\\d_$]*(\\.([a-zA-Z_$][a-zA-Z\\d_$]*))*$")) {
            throw new IllegalArgumentException("Invalid business rule class name: " + businessRuleClass);
        }
    }
    
    /**
     * Check if format mask is valid for number fields.
     */
    private boolean isValidNumberFormat(String format) {
        // Basic validation for Oracle number formats
        return format.matches("^[0-9.,\\-$]+$");
    }
    
    /**
     * Check if format mask is valid for date fields.
     */
    private boolean isValidDateFormat(String format) {
        // Basic validation for Oracle date formats
        return format.matches(".*[YMD].*");
    }
    
    /**
     * Check if format mask is valid for timestamp fields.
     */
    private boolean isValidTimestampFormat(String format) {
        // Basic validation for Oracle timestamp formats
        return format.matches(".*[YMDHF].*");
    }
    
    /**
     * Check if field is for fixed-width file format.
     */
    public boolean isFixedWidth() {
        return fieldPosition != null && fieldLength != null;
    }
    
    /**
     * Check if field requires security measures.
     */
    public boolean requiresSecurityMeasures() {
        return containsPii || encrypted || maskingRequired;
    }
    
    /**
     * Check if field has data quality rules.
     */
    public boolean hasDataQualityRules() {
        return dataQualityCheckEnabled && dataQualityRule != null && !dataQualityRule.trim().isEmpty();
    }
    
    /**
     * Check if field has custom transformation.
     */
    public boolean hasCustomTransformation() {
        return sqlExpression != null && !sqlExpression.trim().isEmpty();
    }
    
    /**
     * Get field configuration summary for logging.
     */
    public String getFieldSummary() {
        return String.format("Field[name=%s, type=%s, pii=%s, encrypted=%s, nullable=%s]",
                fieldName, dataType, containsPii, encrypted, nullable);
    }
}