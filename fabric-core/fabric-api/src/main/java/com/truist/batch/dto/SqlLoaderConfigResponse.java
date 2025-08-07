package com.truist.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for SQL*Loader configuration operations.
 * Contains complete configuration details with analysis and metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SqlLoaderConfigResponse {
    
    // Basic Configuration
    private String configId;
    private String jobName;
    private String sourceSystem;
    private String targetTable;
    
    // Load Configuration
    private String loadMethod;
    private Boolean directPath;
    private Integer parallelDegree;
    private Long bindSize;
    private Long readSize;
    private Integer maxErrors;
    private Integer skipRows;
    private Integer rowsPerCommit;
    
    // File Format Configuration
    private String fieldDelimiter;
    private String recordDelimiter;
    private String stringDelimiter;
    private String characterSet;
    private String dateFormat;
    private String timestampFormat;
    private String nullIf;
    private Boolean trimWhitespace;
    private Boolean optionalEnclosures;
    
    // Performance Configuration
    private Boolean resumable;
    private Integer resumableTimeout;
    private String resumableName;
    private Long streamSize;
    private Boolean silentMode;
    private Boolean continueLoad;
    
    // Security Configuration
    private Boolean encryptionRequired;
    private String encryptionAlgorithm;
    private String encryptionKeyId;
    private Boolean auditTrailRequired;
    private String preExecutionSql;
    private String postExecutionSql;
    private String customOptions;
    private Boolean validationEnabled;
    
    // Compliance Configuration
    private String dataClassification;
    private String piiFields;
    private String regulatoryCompliance;
    private Integer retentionDays;
    private String notificationEmails;
    private String description;
    
    // Field Configurations
    private List<FieldConfigResponse> fieldConfigurations;
    
    // Additional Metadata
    private Map<String, Object> additionalMetadata;
    
    // Control File Template
    private String controlFileTemplate;
    
    // Audit Information
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
    private String reason;
    
    // Version and Status
    private Integer version;
    private Boolean enabled;
    private String status;
    
    // Analysis Information (enriched during retrieval)
    private Double estimatedMemoryUsageMB;
    private String riskLevel;
    private String performanceProfile;
    private Boolean needsOptimization;
    private List<String> optimizationRecommendations;
    private List<String> complianceIssues;
    private List<String> securityWarnings;
    
    // Execution Statistics (optional)
    private Integer totalExecutions;
    private LocalDateTime lastExecutionDate;
    private String lastExecutionStatus;
    private Double averageExecutionTimeSeconds;
    
    // Configuration Summary
    private String configurationSummary;
    private LocalDateTime retrievedAt;
}