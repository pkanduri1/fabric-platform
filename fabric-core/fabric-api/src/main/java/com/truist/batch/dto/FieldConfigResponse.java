package com.truist.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for SQL*Loader field configuration with comprehensive metadata.
 * Contains all field details, validation status, and security information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldConfigResponse {
    
    // Field Identity
    private String fieldId;
    private String configId;
    private String fieldName;
    private String columnName;
    private String sourceField;
    
    // Field Positioning
    private Integer fieldOrder;
    private Integer fieldPosition; // For fixed-width files
    private Integer fieldLength; // For fixed-width files
    
    // Data Type Configuration
    private String dataType;
    private Integer maxLength;
    private String formatMask;
    private Boolean nullable;
    private String defaultValue;
    
    // Transformation Configuration
    private String sqlExpression;
    private String transformationApplied;
    private String dataLineage;
    
    // Security Configuration
    private Boolean encrypted;
    private String encryptionFunction;
    private Boolean containsPii;
    private String piiClassification;
    private Boolean maskingRequired;
    private String maskingFunction;
    
    // Validation Configuration
    private String validationRule;
    private String nullIfCondition;
    private Boolean trimEnabled;
    private String caseSensitive;
    private String characterSet;
    private String checkConstraint;
    
    // Lookup Configuration
    private String lookupTable;
    private String lookupColumn;
    
    // Constraint Configuration
    private Boolean uniqueConstraint;
    private Boolean primaryKey;
    
    // Business Rule Configuration
    private String businessRuleClass;
    private String businessRuleParameters;
    
    // Data Quality Configuration
    private Boolean dataQualityCheckEnabled;
    private String dataQualityRule;
    private Double errorThresholdPercentage;
    
    // Audit Configuration
    private Boolean auditField;
    
    // Performance Configuration
    private Boolean indexRecommended;
    private Boolean partitionKey;
    private String compressionHint;
    
    // Metadata
    private String description;
    
    // Audit Information
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
    private Integer version;
    private Boolean enabled;
    
    // Field Analysis
    private FieldAnalysis analysis;
    
    // Nested class for field analysis
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldAnalysis {
        private String securityLevel; // LOW, MEDIUM, HIGH
        private String complexityLevel; // SIMPLE, MODERATE, COMPLEX
        private Boolean hasCustomTransformation;
        private Boolean hasDataQualityRules;
        private Boolean hasBusinessRules;
        private Boolean requiresSpecialHandling;
        private String performanceImpact; // MINIMAL, MODERATE, SIGNIFICANT
        private java.util.List<String> warnings;
        private java.util.List<String> recommendations;
    }
    
    // Utility Methods
    
    /**
     * Check if field is for fixed-width format.
     */
    public boolean isFixedWidth() {
        return fieldPosition != null && fieldLength != null;
    }
    
    /**
     * Check if field requires security measures.
     */
    public boolean requiresSecurityMeasures() {
        return Boolean.TRUE.equals(containsPii) || 
               Boolean.TRUE.equals(encrypted) || 
               Boolean.TRUE.equals(maskingRequired);
    }
    
    /**
     * Check if field has data quality rules configured.
     */
    public boolean hasDataQualityRules() {
        return Boolean.TRUE.equals(dataQualityCheckEnabled) && 
               dataQualityRule != null && !dataQualityRule.trim().isEmpty();
    }
    
    /**
     * Check if field has custom transformation logic.
     */
    public boolean hasCustomTransformation() {
        return sqlExpression != null && !sqlExpression.trim().isEmpty();
    }
    
    /**
     * Check if field has business rule validation.
     */
    public boolean hasBusinessRules() {
        return businessRuleClass != null && !businessRuleClass.trim().isEmpty();
    }
    
    /**
     * Check if field has lookup functionality.
     */
    public boolean hasLookup() {
        return lookupTable != null && !lookupTable.trim().isEmpty();
    }
    
    /**
     * Check if field is a key field (primary or unique).
     */
    public boolean isKeyField() {
        return Boolean.TRUE.equals(primaryKey) || Boolean.TRUE.equals(uniqueConstraint);
    }
    
    /**
     * Get field security classification.
     */
    public String getSecurityClassification() {
        if (Boolean.TRUE.equals(containsPii)) {
            if (Boolean.TRUE.equals(encrypted)) {
                return "PII_ENCRYPTED";
            } else if (Boolean.TRUE.equals(maskingRequired)) {
                return "PII_MASKED";
            } else {
                return "PII_UNPROTECTED";
            }
        } else if (Boolean.TRUE.equals(encrypted)) {
            return "ENCRYPTED";
        } else {
            return "STANDARD";
        }
    }
    
    /**
     * Calculate field complexity score.
     */
    public int getComplexityScore() {
        int score = 0;
        
        // Base complexity
        if (hasCustomTransformation()) score += 3;
        if (hasDataQualityRules()) score += 2;
        if (hasBusinessRules()) score += 3;
        if (hasLookup()) score += 2;
        
        // Security complexity
        if (Boolean.TRUE.equals(encrypted)) score += 2;
        if (Boolean.TRUE.equals(maskingRequired)) score += 2;
        if (Boolean.TRUE.equals(containsPii)) score += 1;
        
        // Constraint complexity
        if (Boolean.TRUE.equals(primaryKey)) score += 1;
        if (Boolean.TRUE.equals(uniqueConstraint)) score += 1;
        if (checkConstraint != null && !checkConstraint.trim().isEmpty()) score += 2;
        
        // Format complexity
        if (formatMask != null && !formatMask.trim().isEmpty()) score += 1;
        if (isFixedWidth()) score += 1;
        
        return score;
    }
    
    /**
     * Get field complexity level based on score.
     */
    public String getComplexityLevel() {
        int score = getComplexityScore();
        if (score >= 8) return "COMPLEX";
        if (score >= 4) return "MODERATE";
        return "SIMPLE";
    }
    
    /**
     * Check if field requires special handling during load.
     */
    public boolean requiresSpecialHandling() {
        return hasCustomTransformation() || 
               hasBusinessRules() || 
               hasLookup() || 
               requiresSecurityMeasures() ||
               Boolean.TRUE.equals(auditField);
    }
    
    /**
     * Get estimated performance impact of field configuration.
     */
    public String getPerformanceImpact() {
        int impactScore = 0;
        
        if (hasCustomTransformation()) impactScore += 3;
        if (hasBusinessRules()) impactScore += 4;
        if (hasLookup()) impactScore += 3;
        if (hasDataQualityRules()) impactScore += 2;
        if (Boolean.TRUE.equals(encrypted)) impactScore += 2;
        if (Boolean.TRUE.equals(maskingRequired)) impactScore += 2;
        
        if (impactScore >= 6) return "SIGNIFICANT";
        if (impactScore >= 3) return "MODERATE";
        return "MINIMAL";
    }
    
    /**
     * Get field configuration warnings.
     */
    public java.util.List<String> getConfigurationWarnings() {
        java.util.List<String> warnings = new java.util.ArrayList<>();
        
        if (Boolean.TRUE.equals(containsPii) && !Boolean.TRUE.equals(encrypted) && !Boolean.TRUE.equals(maskingRequired)) {
            warnings.add("PII field is not encrypted or masked");
        }
        
        if (Boolean.TRUE.equals(primaryKey) && Boolean.TRUE.equals(nullable)) {
            warnings.add("Primary key field cannot be nullable");
        }
        
        if (dataType != null && ("CHAR".equals(dataType) || "VARCHAR".equals(dataType)) && maxLength == null) {
            warnings.add("Character field missing maximum length specification");
        }
        
        if (Boolean.TRUE.equals(encrypted) && (encryptionFunction == null || encryptionFunction.trim().isEmpty())) {
            warnings.add("Encryption enabled but no encryption function specified");
        }
        
        if (lookupTable != null && (lookupColumn == null || lookupColumn.trim().isEmpty())) {
            warnings.add("Lookup table specified but lookup column is missing");
        }
        
        return warnings;
    }
    
    /**
     * Get field configuration recommendations.
     */
    public java.util.List<String> getConfigurationRecommendations() {
        java.util.List<String> recommendations = new java.util.ArrayList<>();
        
        if (Boolean.TRUE.equals(containsPii) && !Boolean.TRUE.equals(auditField)) {
            recommendations.add("Consider enabling audit trail for PII field");
        }
        
        if (isKeyField() && !Boolean.TRUE.equals(indexRecommended)) {
            recommendations.add("Consider adding index for key field");
        }
        
        if (getComplexityScore() > 6) {
            recommendations.add("Consider simplifying field transformation logic");
        }
        
        if (Boolean.TRUE.equals(dataQualityCheckEnabled) && errorThresholdPercentage == null) {
            recommendations.add("Consider setting error threshold for data quality checks");
        }
        
        return recommendations;
    }
    
    /**
     * Generate field summary for logging and reporting.
     */
    public String getFieldSummary() {
        return String.format("Field[name=%s, type=%s, security=%s, complexity=%s, nullable=%s]",
                fieldName, dataType, getSecurityClassification(), 
                getComplexityLevel(), nullable);
    }
    
    /**
     * Check if field configuration is valid and complete.
     */
    public boolean isValid() {
        return fieldName != null && !fieldName.trim().isEmpty() &&
               columnName != null && !columnName.trim().isEmpty() &&
               dataType != null && !dataType.trim().isEmpty() &&
               getConfigurationWarnings().isEmpty();
    }
    
    /**
     * Get field age in days since creation.
     */
    public long getFieldAgeDays() {
        if (createdDate == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(
                createdDate.toLocalDate(), 
                LocalDateTime.now().toLocalDate());
    }
}