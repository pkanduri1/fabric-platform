package com.fabric.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for SQL*Loader configuration creation and updates.
 * Contains comprehensive validation rules for enterprise fintech security
 * and compliance requirements.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"preExecutionSql", "postExecutionSql", "customOptions"})
public class SqlLoaderConfigRequest {
    
    // Basic Configuration
    @NotBlank(message = "Job name is required")
    @Size(max = 50, message = "Job name cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Job name can only contain alphanumeric characters, underscores, and hyphens")
    private String jobName;
    
    @NotBlank(message = "Source system is required")
    @Size(max = 50, message = "Source system cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Source system can only contain alphanumeric characters, underscores, and hyphens")
    private String sourceSystem;
    
    @NotBlank(message = "Target table is required")
    @Size(max = 100, message = "Target table name cannot exceed 100 characters")
    private String targetTable;
    
    // Load Configuration
    @Builder.Default
    private String loadMethod = "INSERT"; // INSERT, APPEND, REPLACE, TRUNCATE
    
    @Builder.Default
    private Boolean directPath = true;
    
    @Min(value = 1, message = "Parallel degree must be at least 1")
    @Max(value = 32, message = "Parallel degree cannot exceed 32")
    @Builder.Default
    private Integer parallelDegree = 1;
    
    @Positive(message = "Bind size must be positive")
    @Builder.Default
    private Long bindSize = 256000L;
    
    @Positive(message = "Read size must be positive")
    @Builder.Default
    private Long readSize = 1048576L;
    
    @Min(value = 0, message = "Max errors cannot be negative")
    @Builder.Default
    private Integer maxErrors = 1000;
    
    @Min(value = 0, message = "Skip rows cannot be negative")
    @Builder.Default
    private Integer skipRows = 1;
    
    @Min(value = 1, message = "Rows per commit must be at least 1")
    @Builder.Default
    private Integer rowsPerCommit = 64;
    
    // File Format Configuration
    @Size(max = 10, message = "Field delimiter cannot exceed 10 characters")
    @Builder.Default
    private String fieldDelimiter = "|";
    
    @Size(max = 10, message = "Record delimiter cannot exceed 10 characters")
    @Builder.Default
    private String recordDelimiter = "\n";
    
    @Size(max = 5, message = "String delimiter cannot exceed 5 characters")
    @Builder.Default
    private String stringDelimiter = "\"";
    
    @Size(max = 20, message = "Character set cannot exceed 20 characters")
    @Builder.Default
    private String characterSet = "UTF8";
    
    @Size(max = 50, message = "Date format cannot exceed 50 characters")
    @Builder.Default
    private String dateFormat = "YYYY-MM-DD HH24:MI:SS";
    
    @Size(max = 50, message = "Timestamp format cannot exceed 50 characters")
    @Builder.Default
    private String timestampFormat = "YYYY-MM-DD HH24:MI:SS.FF";
    
    @Size(max = 20, message = "Null if condition cannot exceed 20 characters")
    @Builder.Default
    private String nullIf = "BLANKS";
    
    @Builder.Default
    private Boolean trimWhitespace = true;
    
    @Builder.Default
    private Boolean optionalEnclosures = true;
    
    // Performance Configuration
    @Builder.Default
    private Boolean resumable = true;
    
    @Min(value = 1, message = "Resumable timeout must be at least 1 second")
    @Max(value = 86400, message = "Resumable timeout cannot exceed 24 hours")
    @Builder.Default
    private Integer resumableTimeout = 7200;
    
    @Size(max = 100, message = "Resumable name cannot exceed 100 characters")
    private String resumableName;
    
    @Positive(message = "Stream size must be positive")
    @Builder.Default
    private Long streamSize = 256000L;
    
    @Builder.Default
    private Boolean silentMode = false;
    
    @Builder.Default
    private Boolean continueLoad = false;
    
    // Security Configuration
    @Builder.Default
    private Boolean encryptionRequired = false;
    
    @Size(max = 20, message = "Encryption algorithm cannot exceed 20 characters")
    @Builder.Default
    private String encryptionAlgorithm = "AES256";
    
    @Size(max = 100, message = "Encryption key ID cannot exceed 100 characters")
    private String encryptionKeyId;
    
    @Builder.Default
    private Boolean auditTrailRequired = true;
    
    @Size(max = 2000, message = "Pre-execution SQL cannot exceed 2000 characters")
    private String preExecutionSql;
    
    @Size(max = 2000, message = "Post-execution SQL cannot exceed 2000 characters")
    private String postExecutionSql;
    
    @Size(max = 2000, message = "Custom options cannot exceed 2000 characters")
    private String customOptions;
    
    @Builder.Default
    private Boolean validationEnabled = true;
    
    // Compliance Configuration
    @NotNull(message = "Data classification is required")
    @Builder.Default
    private String dataClassification = "INTERNAL"; // PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED, TOP_SECRET
    
    @Size(max = 1000, message = "PII fields list cannot exceed 1000 characters")
    private String piiFields; // Comma-separated list of PII field names
    
    @Size(max = 100, message = "Regulatory compliance cannot exceed 100 characters")
    private String regulatoryCompliance; // SOX, PCI-DSS, GDPR, etc.
    
    @Min(value = 1, message = "Retention days must be at least 1")
    @Max(value = 9999, message = "Retention days cannot exceed 9999")
    @Builder.Default
    private Integer retentionDays = 2555; // 7 years default for financial data
    
    @Size(max = 1000, message = "Notification emails cannot exceed 1000 characters")
    private String notificationEmails;
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    // Field Configurations
    @Valid
    @NotNull(message = "Field configurations are required")
    @Size(min = 1, message = "At least one field configuration is required")
    private List<FieldConfigRequest> fieldConfigurations;
    
    // Additional Metadata
    private Map<String, Object> additionalMetadata;
    
    // Control File Template Override
    private String controlFileTemplate;
    
    // Audit Information
    @NotBlank(message = "Created by is required")
    @Size(max = 50, message = "Created by cannot exceed 50 characters")
    private String createdBy;
    
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason; // Reason for creation/update
    
    // Validation Methods
    
    /**
     * Validate security requirements based on data classification and PII presence.
     */
    public void validateSecurityRequirements() {
        if (containsPiiData() && !encryptionRequired && isHighSecurityClassification()) {
            throw new IllegalArgumentException("PII data with high classification requires encryption");
        }
        
        if (regulatoryCompliance != null && regulatoryCompliance.contains("PCI") && !auditTrailRequired) {
            throw new IllegalArgumentException("PCI compliance requires audit trail");
        }
    }
    
    /**
     * Validate performance settings for consistency.
     */
    public void validatePerformanceSettings() {
        if (directPath && rowsPerCommit != null && rowsPerCommit > 0) {
            throw new IllegalArgumentException("Direct path load does not support ROWS parameter");
        }
        
        if (parallelDegree > 1 && !directPath) {
            throw new IllegalArgumentException("Parallel processing requires direct path loading");
        }
        
        if (bindSize > 10485760) { // 10MB
            throw new IllegalArgumentException("Bind size too large, may cause memory issues");
        }
    }
    
    /**
     * Validate field configurations for completeness and consistency.
     */
    public void validateFieldConfigurations() {
        if (fieldConfigurations == null || fieldConfigurations.isEmpty()) {
            throw new IllegalArgumentException("At least one field configuration is required");
        }
        
        // Check for duplicate field names
        long uniqueFieldNames = fieldConfigurations.stream()
                .map(FieldConfigRequest::getFieldName)
                .distinct()
                .count();
        
        if (uniqueFieldNames != fieldConfigurations.size()) {
            throw new IllegalArgumentException("Duplicate field names are not allowed");
        }
        
        // Validate field order consistency
        boolean hasPositions = fieldConfigurations.stream()
                .anyMatch(f -> f.getFieldOrder() != null);
        
        if (hasPositions) {
            boolean allHavePositions = fieldConfigurations.stream()
                    .allMatch(f -> f.getFieldOrder() != null);
            
            if (!allHavePositions) {
                throw new IllegalArgumentException("Either all fields must have field order or none");
            }
        }
    }
    
    /**
     * Check if configuration contains PII data.
     */
    public boolean containsPiiData() {
        return piiFields != null && !piiFields.trim().isEmpty();
    }
    
    /**
     * Check if data classification is high security.
     */
    public boolean isHighSecurityClassification() {
        return dataClassification != null && 
               (dataClassification.equals("CONFIDENTIAL") || 
                dataClassification.equals("RESTRICTED") || 
                dataClassification.equals("TOP_SECRET"));
    }
    
    /**
     * Check if parallel processing is configured.
     */
    public boolean hasParallelProcessing() {
        return parallelDegree != null && parallelDegree > 1;
    }
    
    /**
     * Get estimated memory usage in MB based on configuration.
     */
    public double getEstimatedMemoryUsageMB() {
        double baseMemory = 50.0; // Base memory overhead
        
        if (bindSize != null) {
            baseMemory += (bindSize / 1024.0 / 1024.0); // Convert bytes to MB
        }
        
        if (readSize != null) {
            baseMemory += (readSize / 1024.0 / 1024.0); // Convert bytes to MB
        }
        
        if (hasParallelProcessing()) {
            baseMemory *= parallelDegree; // Multiply by parallel degree
        }
        
        return baseMemory;
    }
    
    /**
     * Get configuration risk level based on security settings and data classification.
     */
    public String getRiskLevel() {
        if (isHighSecurityClassification() && !encryptionRequired) {
            return "HIGH";
        }
        
        if (containsPiiData() && !auditTrailRequired) {
            return "MEDIUM";
        }
        
        if (maxErrors > 10000) {
            return "MEDIUM";
        }
        
        return "LOW";
    }
    
    /**
     * Generate configuration summary for logging and audit purposes.
     */
    public String getConfigurationSummary() {
        return String.format("SqlLoaderConfig[job=%s, source=%s, table=%s, classification=%s, pii=%s, encryption=%s]",
                jobName, sourceSystem, targetTable, dataClassification, 
                containsPiiData(), encryptionRequired);
    }
}