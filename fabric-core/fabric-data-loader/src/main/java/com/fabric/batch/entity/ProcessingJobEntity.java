package com.fabric.batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing individual data loading job executions.
 * Tracks job status, statistics, and provides comprehensive audit trail.
 */
@Entity
@Table(name = "processing_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"dataLoadConfig", "executionLogs"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProcessingJobEntity {
    
    @Id
    @Column(name = "job_execution_id", length = 100)
    private String jobExecutionId;
    
    @Column(name = "config_id", nullable = false, length = 100)
    private String configId;
    
    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;
    
    @Column(name = "batch_id", length = 50)
    private String batchId;
    
    @Column(name = "file_name", length = 500)
    private String fileName;
    
    @Column(name = "file_path", length = 1000)
    private String filePath;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_checksum", length = 100)
    private String fileChecksum;
    
    @Column(name = "job_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private JobStatus jobStatus = JobStatus.SUBMITTED;
    
    @Column(name = "execution_mode", length = 20)
    @Enumerated(EnumType.STRING)
    private ExecutionMode executionMode = ExecutionMode.BATCH;
    
    @Column(name = "priority", length = 10)
    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.NORMAL;
    
    @Column(name = "submitted_date")
    private LocalDateTime submittedDate;
    
    @Column(name = "started_date")
    private LocalDateTime startedDate;
    
    @Column(name = "completed_date")
    private LocalDateTime completedDate;
    
    @Column(name = "duration_ms")
    private Long durationMs;
    
    @Column(name = "total_records")
    private Long totalRecords = 0L;
    
    @Column(name = "processed_records")
    private Long processedRecords = 0L;
    
    @Column(name = "successful_records")
    private Long successfulRecords = 0L;
    
    @Column(name = "failed_records")
    private Long failedRecords = 0L;
    
    @Column(name = "warning_records")
    private Long warningRecords = 0L;
    
    @Column(name = "duplicate_records")
    private Long duplicateRecords = 0L;
    
    @Column(name = "validation_errors")
    private Integer validationErrors = 0;
    
    @Column(name = "business_rule_errors")
    private Integer businessRuleErrors = 0;
    
    @Column(name = "referential_errors")
    private Integer referentialErrors = 0;
    
    @Column(name = "sql_loader_return_code")
    private Integer sqlLoaderReturnCode;
    
    @Column(name = "control_file_path", length = 500)
    private String controlFilePath;
    
    @Column(name = "log_file_path", length = 500)
    private String logFilePath;
    
    @Column(name = "bad_file_path", length = 500)
    private String badFilePath;
    
    @Column(name = "discard_file_path", length = 500)
    private String discardFilePath;
    
    @Column(name = "backup_file_path", length = 500)
    private String backupFilePath;
    
    @Column(name = "archive_file_path", length = 500)
    private String archiveFilePath;
    
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    
    @Column(name = "warning_message", length = 2000)
    private String warningMessage;
    
    @Column(name = "rollback_required", length = 1)
    private String rollbackRequired = "N";
    
    @Column(name = "rollback_completed", length = 1)
    private String rollbackCompleted = "N";
    
    @Column(name = "rollback_date")
    private LocalDateTime rollbackDate;
    
    @Column(name = "notification_sent", length = 1)
    private String notificationSent = "N";
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "max_retries")
    private Integer maxRetries = 3;
    
    @Column(name = "next_retry_date")
    private LocalDateTime nextRetryDate;
    
    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @Column(name = "modified_by", length = 50)
    private String modifiedBy;
    
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", referencedColumnName = "config_id", insertable = false, updatable = false)
    private DataLoadConfigEntity dataLoadConfig;
    
    @OneToMany(mappedBy = "jobExecutionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<JobExecutionLogEntity> executionLogs;
    
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (submittedDate == null) {
            submittedDate = LocalDateTime.now();
        }
        if (jobExecutionId == null) {
            jobExecutionId = configId + "-" + System.currentTimeMillis();
        }
        if (correlationId == null) {
            correlationId = "CORR-" + System.currentTimeMillis();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
        
        // Calculate duration if job is completed
        if (startedDate != null && completedDate != null && durationMs == null) {
            durationMs = java.time.Duration.between(startedDate, completedDate).toMillis();
        }
    }
    
    // Enums
    public enum JobStatus {
        SUBMITTED,
        QUEUED,
        VALIDATING,
        PROCESSING,
        LOADING,
        POST_PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        RETRYING,
        ROLLBACK_REQUIRED,
        ROLLBACK_COMPLETED
    }
    
    public enum ExecutionMode {
        BATCH,
        REAL_TIME,
        SCHEDULED,
        MANUAL
    }
    
    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }
    
    // Utility methods
    public boolean isCompleted() {
        return jobStatus == JobStatus.COMPLETED;
    }
    
    public boolean isFailed() {
        return jobStatus == JobStatus.FAILED;
    }
    
    public boolean requiresRollback() {
        return "Y".equalsIgnoreCase(rollbackRequired);
    }
    
    public boolean isRollbackCompleted() {
        return "Y".equalsIgnoreCase(rollbackCompleted);
    }
    
    public boolean isNotificationSent() {
        return "Y".equalsIgnoreCase(notificationSent);
    }
    
    public boolean canRetry() {
        return retryCount < maxRetries && isFailed();
    }
    
    public double getSuccessRate() {
        if (totalRecords == 0) return 0.0;
        return (double) successfulRecords / totalRecords * 100.0;
    }
    
    public double getErrorRate() {
        if (totalRecords == 0) return 0.0;
        return (double) failedRecords / totalRecords * 100.0;
    }
    
    public boolean hasErrors() {
        return failedRecords > 0 || validationErrors > 0 || businessRuleErrors > 0 || referentialErrors > 0;
    }
    
    public boolean hasWarnings() {
        return warningRecords > 0;
    }
    
    public void incrementRetryCount() {
        if (retryCount == null) {
            retryCount = 0;
        }
        retryCount++;
    }
}