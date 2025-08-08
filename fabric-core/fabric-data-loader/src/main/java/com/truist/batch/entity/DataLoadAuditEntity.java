package com.truist.batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Entity representing comprehensive audit trail for data loading operations.
 * Provides complete data lineage tracking and change history for compliance requirements.
 */
@Entity
@Table(name = "data_load_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DataLoadAuditEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;
    
    @Column(name = "config_id", nullable = false, length = 100)
    private String configId;
    
    @Column(name = "job_execution_id", length = 100)
    private String jobExecutionId;
    
    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;
    
    @Column(name = "audit_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AuditType auditType;
    
    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;
    
    @Column(name = "event_description", length = 1000)
    private String eventDescription;
    
    @Column(name = "source_system", length = 50)
    private String sourceSystem;
    
    @Column(name = "target_table", length = 100)
    private String targetTable;
    
    @Column(name = "file_name", length = 500)
    private String fileName;
    
    @Column(name = "file_path", length = 1000)
    private String filePath;
    
    @Column(name = "record_count")
    private Long recordCount;
    
    @Column(name = "processed_count")
    private Long processedCount;
    
    @Column(name = "error_count")
    private Long errorCount;
    
    @Column(name = "data_source", length = 200)
    private String dataSource;
    
    @Column(name = "data_destination", length = 200)
    private String dataDestination;
    
    @Column(name = "transformation_applied", length = 1000)
    private String transformationApplied;
    
    @Column(name = "validation_rules_applied", length = 2000)
    private String validationRulesApplied;
    
    @Column(name = "business_rules_applied", length = 2000)
    private String businessRulesApplied;
    
    @Column(name = "data_quality_score")
    private Double dataQualityScore;
    
    @Column(name = "compliance_status", length = 20)
    @Enumerated(EnumType.STRING)
    private ComplianceStatus complianceStatus = ComplianceStatus.COMPLIANT;
    
    @Column(name = "regulatory_requirement", length = 100)
    private String regulatoryRequirement;
    
    @Column(name = "retention_period_days")
    private Integer retentionPeriodDays;
    
    @Column(name = "pii_fields", length = 1000)
    private String piiFields;
    
    @Column(name = "sensitive_data_hash", length = 256)
    private String sensitiveDataHash;
    
    @Column(name = "encryption_applied", length = 1)
    private String encryptionApplied = "N";
    
    @Column(name = "masking_applied", length = 1)
    private String maskingApplied = "N";
    
    @Column(name = "access_control_applied", length = 1)
    private String accessControlApplied = "N";
    
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;
    
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "application_name", length = 100)
    private String applicationName = "fabric-data-loader";
    
    @Column(name = "application_version", length = 20)
    private String applicationVersion;
    
    @Column(name = "environment", length = 20)
    private String environment;
    
    @Column(name = "host_name", length = 100)
    private String hostName;
    
    @Column(name = "process_id")
    private Long processId;
    
    @Column(name = "thread_id")
    private Long threadId;
    
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
    
    @Column(name = "memory_usage_mb")
    private Double memoryUsageMb;
    
    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;
    
    @Column(name = "error_code", length = 20)
    private String errorCode;
    
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    
    @Column(name = "stack_trace", columnDefinition = "CLOB")
    private String stackTrace;
    
    @Column(name = "additional_metadata", columnDefinition = "CLOB")
    private String additionalMetadata;
    
    @Column(name = "parent_audit_id")
    private Long parentAuditId;
    
    @Column(name = "audit_timestamp", nullable = false)
    private LocalDateTime auditTimestamp;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @PrePersist
    protected void onCreate() {
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