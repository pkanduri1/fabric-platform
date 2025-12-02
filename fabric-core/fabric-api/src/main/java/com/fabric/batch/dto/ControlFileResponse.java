package com.fabric.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for SQL*Loader control file generation operations.
 * Contains control file content, metadata, and validation information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlFileResponse {
    
    // Control File Content
    private String controlFileContent;
    private String controlFileName;
    private String controlFilePath;
    
    // Generation Metadata
    private String configId;
    private String jobName;
    private String sourceSystem;
    private String targetTable;
    private String dataFilePath;
    private LocalDateTime generatedAt;
    private String generatedBy;
    private String correlationId;
    
    // Control File Statistics
    private Integer totalFields;
    private Integer encryptedFields;
    private Integer validationFields;
    private Integer transformationFields;
    private Long estimatedFileSize;
    
    // Performance Estimates
    private String performanceProfile;
    private Long estimatedLoadTimeMinutes;
    private Double estimatedThroughputRecordsPerSecond;
    private String memoryUsageProfile;
    
    // Validation Results
    private Boolean syntaxValid;
    private List<String> validationErrors;
    private List<String> validationWarnings;
    private List<String> optimizationRecommendations;
    
    // Security Information
    private Boolean containsEncryption;
    private Boolean containsPiiHandling;
    private String securityLevel;
    private List<String> securityFeatures;
    
    // Additional Options
    private Map<String, String> controlFileOptions;
    private List<String> preExecutionCommands;
    private List<String> postExecutionCommands;
    
    // File Metadata
    private String characterSet;
    private String fileFormat; // DELIMITED, FIXED_WIDTH
    private String loadMethod; // INSERT, APPEND, REPLACE, TRUNCATE
    
    // Utility Methods
    
    /**
     * Check if control file is ready for execution.
     */
    public boolean isReadyForExecution() {
        return Boolean.TRUE.equals(syntaxValid) && 
               (validationErrors == null || validationErrors.isEmpty()) &&
               controlFileContent != null && !controlFileContent.trim().isEmpty();
    }
    
    /**
     * Get total issue count (errors + warnings).
     */
    public int getTotalIssueCount() {
        int count = 0;
        if (validationErrors != null) count += validationErrors.size();
        if (validationWarnings != null) count += validationWarnings.size();
        return count;
    }
    
    /**
     * Check if control file has performance concerns.
     */
    public boolean hasPerformanceConcerns() {
        return optimizationRecommendations != null && !optimizationRecommendations.isEmpty();
    }
    
    /**
     * Get control file complexity level.
     */
    public String getComplexityLevel() {
        int complexityScore = 0;
        
        if (encryptedFields != null && encryptedFields > 0) complexityScore += 2;
        if (validationFields != null && validationFields > 0) complexityScore += 2;
        if (transformationFields != null && transformationFields > 0) complexityScore += 3;
        if (preExecutionCommands != null && !preExecutionCommands.isEmpty()) complexityScore += 2;
        if (postExecutionCommands != null && !postExecutionCommands.isEmpty()) complexityScore += 2;
        
        if (complexityScore >= 7) return "HIGH";
        if (complexityScore >= 4) return "MEDIUM";
        return "LOW";
    }
    
    /**
     * Generate summary for logging.
     */
    public String getSummary() {
        return String.format("ControlFile[config=%s, fields=%d, valid=%s, complexity=%s]",
                configId, totalFields, syntaxValid, getComplexityLevel());
    }
}

/**
 * DTO for control file validation results.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ControlFileValidationResult {
    private Boolean valid;
    private List<String> errors;
    private List<String> warnings;
    private List<String> recommendations;
    private String validationSummary;
    private LocalDateTime validatedAt;
    private String validatedBy;
}

/**
 * DTO for control file generation history.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ControlFileHistory {
    private String historyId;
    private String configId;
    private String controlFileName;
    private LocalDateTime generatedAt;
    private String generatedBy;
    private String correlationId;
    private Boolean successful;
    private String errorMessage;
    private Map<String, Object> metadata;
}