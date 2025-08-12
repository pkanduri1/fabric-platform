package com.truist.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Container class for all SQL*Loader reporting and analytics DTOs.
 * Contains comprehensive reporting structures for compliance, performance,
 * security, and operational analytics.
 */
public class SqlLoaderReports {

    // ==================== VALIDATION RESULTS ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ControlFileValidationResult {
        private Boolean valid;
        private List<String> errors;
        private List<String> warnings;
        private List<String> recommendations;
        private String validationSummary;
        private LocalDateTime validatedAt;
        private String validatedBy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfigurationValidationResult {
        private Boolean valid;
        private String jobName;
        private String sourceSystem;
        private List<String> errors;
        private List<String> warnings;
        private List<String> recommendations;
        private String validationSummary;
        private LocalDateTime validatedAt;
        private String validatedBy;
    }

    // ==================== COMPLIANCE REPORTING ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComplianceReport {
        private String configId;
        private String jobName;
        private String sourceSystem;
        private Boolean compliant;
        private String dataClassification;
        private String regulatoryCompliance;
        private List<String> complianceIssues;
        private List<String> complianceRecommendations;
        private String riskLevel;
        private Boolean piiDataPresent;
        private Boolean encryptionCompliant;
        private Boolean auditCompliant;
        private Boolean retentionCompliant;
        private LocalDateTime assessedAt;
        private String assessedBy;
        private Map<String, Object> complianceMetrics;
        
        /**
         * Convenience method for accessing compliant field
         */
        public Boolean isCompliant() {
            return this.compliant;
        }
        
        /**
         * Convenience method for accessing complianceRecommendations field
         */
        public List<String> getRecommendations() {
            return this.complianceRecommendations;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataClassificationReport {
        private Integer totalConfigurations;
        private Integer totalPiiConfigurations;
        private Integer encryptedPiiConfigurations;
        private Integer unencryptedPiiConfigurations;
        private Double piiComplianceRate;
        private Map<String, Integer> classificationBreakdown;
        private Map<String, Integer> regulatoryComplianceBreakdown;
        private List<String> topComplianceIssues;
        private List<String> recommendations;
        private LocalDateTime reportGeneratedAt;
    }

    // ==================== PERFORMANCE REPORTING ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PerformanceReport {
        private String configId;
        private String jobName;
        private String sourceSystem;
        private String performanceProfile;
        private Boolean directPathEnabled;
        private Integer parallelDegree;
        private Long bindSize;
        private Long readSize;
        private Boolean needsOptimization;
        private List<String> performanceRecommendations;
        private Double estimatedThroughputMBps;
        private Double estimatedMemoryUsageMB;
        private Map<String, Object> performanceMetrics;
        private LocalDateTime analyzedAt;
        private String analyzedBy;
        
        /**
         * Convenience method for accessing needsOptimization field
         */
        public Boolean needsOptimization() {
            return this.needsOptimization;
        }
        
        /**
         * Convenience method for accessing performanceRecommendations field
         */
        public List<String> getOptimizationRecommendations() {
            return this.performanceRecommendations;
        }
        
        /**
         * Convenience method for accessing directPathEnabled field
         */
        public Boolean isDirectPathEnabled() {
            return this.directPathEnabled;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExecutionStatistics {
        private String configId;
        private String jobName;
        private String sourceSystem;
        private Integer totalExecutions;
        private Integer successfulExecutions;
        private Integer failedExecutions;
        private Double successRate;
        private Double averageExecutionTimeSeconds;
        private Double averageDataVolumeMB;
        private Double averageThroughputMBps;
        private LocalDateTime firstExecutionDate;
        private LocalDateTime lastExecutionDate;
        private Map<String, Object> executionTrends;
        private LocalDateTime reportGeneratedAt;
    }

    // ==================== SECURITY REPORTING ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SecurityAssessment {
        private String configId;
        private String jobName;
        private String sourceSystem;
        private String riskLevel;
        private Boolean containsPiiData;
        private Boolean encryptionConfigured;
        private Boolean auditingEnabled;
        private Boolean complianceRequired;
        private List<String> securityWarnings;
        private List<String> securityRecommendations;
        private Map<String, Object> securityMetrics;
        private LocalDateTime assessedAt;
        private String assessedBy;
    }

    // ==================== OPERATIONAL REPORTING ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfigurationUsageReport {
        private Integer totalConfigurations;
        private Integer activeConfigurations;
        private Integer inactiveConfigurations;
        private Integer configurationsWithPii;
        private Integer encryptedConfigurations;
        private Map<String, Integer> sourceSystemBreakdown;
        private Map<String, Integer> dataClassificationBreakdown;
        private Map<String, Integer> loadMethodBreakdown;
        private List<String> mostUsedTables;
        private List<String> topPerformingConfigurations;
        private LocalDateTime reportGeneratedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfigurationAlert {
        private String configId;
        private String jobName;
        private String sourceSystem;
        private String alertType;
        private String severity;
        private String message;
        private String recommendedAction;
        private LocalDateTime alertGeneratedAt;
        private Boolean acknowledged;
        private String acknowledgedBy;
        private LocalDateTime acknowledgedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuditTrailReport {
        private String configId;
        private String jobName;
        private String sourceSystem;
        private Integer totalChanges;
        private List<AuditEntry> auditEntries;
        private LocalDateTime reportGeneratedAt;
        private LocalDateTime reportPeriodStart;
        private LocalDateTime reportPeriodEnd;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuditEntry {
        private String auditId;
        private String configId;
        private String actionType;
        private String actionDescription;
        private String performedBy;
        private LocalDateTime performedAt;
        private Map<String, Object> changeDetails;
        private String reason;
    }

    // ==================== EXECUTION AND CONTROL FILE MANAGEMENT ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExecutionConfig {
        private String configId;
        private String executionId;
        private String correlationId;
        private String dataFilePath;
        private String controlFilePath;
        private Map<String, Object> executionParameters;
        private Boolean readyForExecution;
        private List<String> validationIssues;
        private LocalDateTime preparedAt;
        private String preparedBy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExecutionHistory {
        private String executionId;
        private String configId;
        private String jobName;
        private String sourceSystem;
        private String dataFilePath;
        private String executionStatus;
        private LocalDateTime executionStartTime;
        private LocalDateTime executionEndTime;
        private Long durationSeconds;
        private Long recordsProcessed;
        private Long recordsLoaded;
        private Long recordsRejected;
        private String errorMessage;
        private String correlationId;
        private String executedBy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ControlFileHistory {
        private String controlFileId;
        private String configId;
        private String jobName;
        private String sourceSystem;
        private String dataFilePath;
        private LocalDateTime generatedAt;
        private String generatedBy;
        private String correlationId;
        private Boolean syntaxValid;
        private String controlFileName;
        private Long fileSizeBytes;
        private String checksum;
    }

    // ==================== TEMPLATE AND BULK OPERATIONS ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfigurationTemplate {
        private String templateId;
        private String templateName;
        private String description;
        private String sourceConfigId;
        private String category;
        private Map<String, Object> templateParameters;
        private List<String> applicableSourceSystems;
        private String createdBy;
        private LocalDateTime createdDate;
        private Integer usageCount;
        private Boolean active;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BulkImportResult {
        private Integer totalRecords;
        private Integer successfulImports;
        private Integer failedImports;
        private Integer skippedRecords;
        private List<String> importErrors;
        private List<String> importWarnings;
        private List<String> createdConfigIds;
        private String importBatchId;
        private LocalDateTime importStartTime;
        private LocalDateTime importEndTime;
        private String importedBy;
    }

    // ==================== INTEGRATION AND COMPATIBILITY ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfigurationTestResult {
        private String configId;
        private Boolean connectionSuccessful;
        private Boolean permissionsValid;
        private Boolean tableExists;
        private Boolean tableAccessible;
        private List<String> testErrors;
        private List<String> testWarnings;
        private Map<String, Object> testMetrics;
        private LocalDateTime testedAt;
        private String testedBy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompatibilityResult {
        private String sourceConfigId;
        private String targetConfigId;
        private String sourceJobName;
        private String targetJobName;
        private Double compatibilityScore;
        private List<String> compatibleFields;
        private List<String> incompatibleFields;
        private List<String> recommendations;
        private LocalDateTime analyzedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MappingRecommendations {
        private String sourceConfigId;
        private String targetConfigId;
        private List<FieldMapping> fieldMappings;
        private List<String> transformationRecommendations;
        private List<String> validationRules;
        private Double mappingConfidence;
        private LocalDateTime generatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldMapping {
        private String sourceField;
        private String targetField;
        private String transformationFunction;
        private Double confidence;
        private String rationale;
        private List<String> validationRules;
    }
}