package com.fabric.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * POJO representing comprehensive audit trail for data loading operations.
 * Provides complete data lineage tracking and change history for compliance requirements.
 * Converted from JPA Entity to work with JdbcTemplate.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DataLoadAuditEntity {
    
    private Long auditId;
    
    private String configId;
    
    private String jobExecutionId;
    
    private String correlationId;
    
    private AuditType auditType;
    
    private String eventName;
    
    private String eventDescription;
    
    private String sourceSystem;
    
    private String targetTable;
    
    private String fileName;
    
    private String filePath;
    
    private Long recordCount;
    
    private Long processedCount;
    
    private Long errorCount;
    
    private String dataSource;
    
    private String dataDestination;
    
    private String transformationApplied;
    
    private String validationRulesApplied;
    
    private String businessRulesApplied;
    
    private Double dataQualityScore;
    
    private ComplianceStatus complianceStatus = ComplianceStatus.COMPLIANT;
    
    private String regulatoryRequirement;
    
    private Integer retentionPeriodDays;
    
    private String piiFields;
    
    private String sensitiveDataHash;
    
    private String encryptionApplied = "N";
    
    private String maskingApplied = "N";
    
    private String accessControlApplied = "N";
    
    private String userId;
    
    private String sessionId;
    
    private String ipAddress;
    
    private String userAgent;
    
    private String applicationName = "fabric-data-loader";
    
    private String applicationVersion;
    
    private String environment;
    
    private String hostName;
    
    private Long processId;
    
    private Long threadId;
    
    private Long executionTimeMs;
    
    private Double memoryUsageMb;
    
    private Double cpuUsagePercent;
    
    private String errorCode;
    
    private String errorMessage;
    
    private String stackTrace;
    
    private String additionalMetadata;
    
    private Long parentAuditId;
    
    private LocalDateTime auditTimestamp;
    
    private LocalDateTime createdDate;
    
    /**
     * Initialize timestamps and default values before saving.
     * Called manually by repository implementation.
     */
    public void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (auditTimestamp == null) {
            auditTimestamp = LocalDateTime.now();
        }
        if (complianceStatus == null) {
            complianceStatus = ComplianceStatus.COMPLIANT;
        }
    }
    
    // Enums
    public enum AuditType {
        DATA_LINEAGE,
        SECURITY_EVENT,
        COMPLIANCE_CHECK,
        PERFORMANCE_METRIC,
        ERROR_EVENT,
        CONFIGURATION_CHANGE,
        DATA_TRANSFORMATION,
        VALIDATION_RESULT,
        BUSINESS_RULE_EXECUTION,
        FILE_OPERATION,
        DATABASE_OPERATION,
        SYSTEM_EVENT
    }
    
    public enum ComplianceStatus {
        COMPLIANT,
        NON_COMPLIANT,
        REQUIRES_REVIEW,
        EXCEPTION_APPROVED,
        PENDING_APPROVAL
    }
    
    // Utility methods
    public boolean isEncryptionApplied() {
        return "Y".equalsIgnoreCase(encryptionApplied);
    }
    
    public boolean isMaskingApplied() {
        return "Y".equalsIgnoreCase(maskingApplied);
    }
    
    public boolean isAccessControlApplied() {
        return "Y".equalsIgnoreCase(accessControlApplied);
    }
    
    public boolean isCompliant() {
        return complianceStatus == ComplianceStatus.COMPLIANT;
    }
    
    public boolean hasErrors() {
        return errorCount != null && errorCount > 0;
    }
    
    public boolean containsPii() {
        return piiFields != null && !piiFields.trim().isEmpty();
    }
    
    public double getProcessingEfficiency() {
        if (recordCount == null || recordCount == 0) return 0.0;
        if (processedCount == null) return 0.0;
        return (double) processedCount / recordCount * 100.0;
    }
    
    public double getErrorRate() {
        if (recordCount == null || recordCount == 0) return 0.0;
        if (errorCount == null) return 0.0;
        return (double) errorCount / recordCount * 100.0;
    }
}