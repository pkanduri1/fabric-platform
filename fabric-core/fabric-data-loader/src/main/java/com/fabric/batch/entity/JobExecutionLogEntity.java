package com.fabric.batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Entity representing detailed execution logs for processing jobs.
 * Provides granular tracking of job execution steps and performance metrics.
 */
@Entity
@Table(name = "job_execution_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"processingJob"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class JobExecutionLogEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;
    
    @Column(name = "job_execution_id", nullable = false, length = 100)
    private String jobExecutionId;
    
    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;
    
    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;
    
    @Column(name = "step_type", length = 50)
    @Enumerated(EnumType.STRING)
    private StepType stepType;
    
    @Column(name = "step_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StepStatus stepStatus;
    
    @Column(name = "log_level", length = 10)
    @Enumerated(EnumType.STRING)
    private LogLevel logLevel = LogLevel.INFO;
    
    @Column(name = "log_message", length = 4000)
    private String logMessage;
    
    @Column(name = "step_start_time")
    private LocalDateTime stepStartTime;
    
    @Column(name = "step_end_time")
    private LocalDateTime stepEndTime;
    
    @Column(name = "step_duration_ms")
    private Long stepDurationMs;
    
    @Column(name = "records_processed")
    private Long recordsProcessed = 0L;
    
    @Column(name = "records_successful")
    private Long recordsSuccessful = 0L;
    
    @Column(name = "records_failed")
    private Long recordsFailed = 0L;
    
    @Column(name = "records_warning")
    private Long recordsWarning = 0L;
    
    @Column(name = "memory_usage_mb")
    private Double memoryUsageMb;
    
    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;
    
    @Column(name = "io_read_mb")
    private Double ioReadMb;
    
    @Column(name = "io_write_mb")
    private Double ioWriteMb;
    
    @Column(name = "throughput_records_per_sec")
    private Double throughputRecordsPerSec;
    
    @Column(name = "error_code", length = 20)
    private String errorCode;
    
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    
    @Column(name = "error_details", columnDefinition = "CLOB")
    private String errorDetails;
    
    @Column(name = "warning_message", length = 2000)
    private String warningMessage;
    
    @Column(name = "additional_data", columnDefinition = "CLOB")
    private String additionalData;
    
    @Column(name = "checkpoint_data", length = 1000)
    private String checkpointData;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_execution_id", referencedColumnName = "job_execution_id", insertable = false, updatable = false)
    private ProcessingJobEntity processingJob;
    
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (stepStartTime == null) {
            stepStartTime = LocalDateTime.now();
        }
        if (logLevel == null) {
            logLevel = LogLevel.INFO;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        // Calculate duration if step is completed
        if (stepStartTime != null && stepEndTime != null && stepDurationMs == null) {
            stepDurationMs = java.time.Duration.between(stepStartTime, stepEndTime).toMillis();
        }
        
        // Calculate throughput
        if (stepDurationMs != null && stepDurationMs > 0 && recordsProcessed != null && recordsProcessed > 0) {
            throughputRecordsPerSec = (double) recordsProcessed / (stepDurationMs / 1000.0);
        }
    }
    
    // Enums
    public enum StepType {
        INITIALIZATION,
        VALIDATION,
        PRE_PROCESSING,
        DATA_LOADING,
        SQL_LOADER_EXECUTION,
        POST_PROCESSING,
        CLEANUP,
        ERROR_HANDLING,
        ROLLBACK,
        NOTIFICATION,
        ARCHIVAL,
        BACKUP,
        CUSTOM
    }
    
    public enum StepStatus {
        STARTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        SKIPPED,
        CANCELLED,
        RETRYING
    }
    
    public enum LogLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL
    }
    
    // Utility methods
    public boolean isCompleted() {
        return stepStatus == StepStatus.COMPLETED;
    }
    
    public boolean isFailed() {
        return stepStatus == StepStatus.FAILED;
    }
    
    public boolean hasErrors() {
        return recordsFailed != null && recordsFailed > 0;
    }
    
    public boolean hasWarnings() {
        return recordsWarning != null && recordsWarning > 0;
    }
    
    public double getSuccessRate() {
        if (recordsProcessed == null || recordsProcessed == 0) return 0.0;
        if (recordsSuccessful == null) return 0.0;
        return (double) recordsSuccessful / recordsProcessed * 100.0;
    }
    
    public double getErrorRate() {
        if (recordsProcessed == null || recordsProcessed == 0) return 0.0;
        if (recordsFailed == null) return 0.0;
        return (double) recordsFailed / recordsProcessed * 100.0;
    }
    
    public void completeStep() {
        stepEndTime = LocalDateTime.now();
        stepStatus = StepStatus.COMPLETED;
        if (stepStartTime != null) {
            stepDurationMs = java.time.Duration.between(stepStartTime, stepEndTime).toMillis();
        }
    }
    
    public void failStep(String errorMessage) {
        stepEndTime = LocalDateTime.now();
        stepStatus = StepStatus.FAILED;
        logLevel = LogLevel.ERROR;
        this.errorMessage = errorMessage;
        if (stepStartTime != null) {
            stepDurationMs = java.time.Duration.between(stepStartTime, stepEndTime).toMillis();
        }
    }
    
    public void incrementRetryCount() {
        if (retryCount == null) {
            retryCount = 0;
        }
        retryCount++;
    }
}