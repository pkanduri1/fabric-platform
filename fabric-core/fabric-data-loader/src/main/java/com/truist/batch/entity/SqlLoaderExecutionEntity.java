package com.truist.batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

/**
 * Entity representing SQL*Loader execution tracking with comprehensive performance metrics,
 * security audit, and compliance monitoring for enterprise fintech environments.
 */
@Entity
@Table(name = "sql_loader_executions", schema = "CM3INT")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"sqlLoaderConfig", "securityAudits"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SqlLoaderExecutionEntity {
    
    @Id
    @Column(name = "execution_id", length = 100)
    private String executionId;
    
    @Column(name = "config_id", nullable = false, length = 100)
    private String configId;
    
    @Column(name = "job_execution_id", length = 100)
    private String jobExecutionId; // Links to processing_jobs table
    
    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;
    
    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;
    
    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_checksum", length = 100)
    private String fileChecksum;
    
    @Column(name = "control_file_path", length = 500)
    private String controlFilePath;
    
    @Column(name = "control_file_content", columnDefinition = "CLOB")
    @Lob
    private String controlFileContent;
    
    @Column(name = "execution_command", columnDefinition = "CLOB")
    @Lob
    private String executionCommand;
    
    @Column(name = "execution_environment", length = 100)
    private String executionEnvironment;
    
    @Column(name = "execution_status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ExecutionStatus executionStatus = ExecutionStatus.SUBMITTED;
    
    @Column(name = "sql_loader_return_code")
    private Integer sqlLoaderReturnCode;
    
    @Column(name = "started_date")
    private LocalDateTime startedDate;
    
    @Column(name = "completed_date")
    private LocalDateTime completedDate;
    
    @Column(name = "duration_ms")
    private Long durationMs;
    
    // Record Processing Metrics
    @Column(name = "total_records")
    @Builder.Default
    private Long totalRecords = 0L;
    
    @Column(name = "successful_records")
    @Builder.Default
    private Long successfulRecords = 0L;
    
    @Column(name = "rejected_records")
    @Builder.Default
    private Long rejectedRecords = 0L;
    
    @Column(name = "skipped_records")
    @Builder.Default
    private Long skippedRecords = 0L;
    
    @Column(name = "warning_records")
    @Builder.Default
    private Long warningRecords = 0L;
    
    @Column(name = "throughput_records_per_sec")
    private Double throughputRecordsPerSec;
    
    // File Paths for SQL*Loader outputs
    @Column(name = "log_file_path", length = 500)
    private String logFilePath;
    
    @Column(name = "bad_file_path", length = 500)
    private String badFilePath;
    
    @Column(name = "discard_file_path", length = 500)
    private String discardFilePath;
    
    @Column(name = "log_file_content", columnDefinition = "CLOB")
    @Lob
    private String logFileContent;
    
    @Column(name = "execution_output", columnDefinition = "CLOB")
    @Lob
    private String executionOutput;
    
    // Error and Warning Information
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    
    @Column(name = "error_details", columnDefinition = "CLOB")
    @Lob
    private String errorDetails;
    
    @Column(name = "warning_messages", columnDefinition = "CLOB")
    @Lob
    private String warningMessages;
    
    // Performance Metrics (JSON formatted)
    @Column(name = "performance_metrics", columnDefinition = "CLOB")
    @Lob
    private String performanceMetrics;
    
    @Column(name = "memory_usage_mb")
    private Double memoryUsageMb;
    
    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;
    
    @Column(name = "io_read_mb")
    private Double ioReadMb;
    
    @Column(name = "io_write_mb")
    private Double ioWriteMb;
    
    // Retry and Recovery
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;
    
    @Column(name = "next_retry_date")
    private LocalDateTime nextRetryDate;
    
    // Notification and Cleanup
    @Column(name = "notification_sent", length = 1)
    @Builder.Default
    private String notificationSent = "N";
    
    @Column(name = "cleanup_completed", length = 1)
    @Builder.Default
    private String cleanupCompleted = "N";
    
    @Column(name = "archived", length = 1)
    @Builder.Default
    private String archived = "N";
    
    @Column(name = "archive_path", length = 500)
    private String archivePath;
    
    // Standard audit fields
    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", referencedColumnName = "config_id", insertable = false, updatable = false)
    private SqlLoaderConfigEntity sqlLoaderConfig;
    
    @OneToMany(mappedBy = "sqlLoaderExecution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SqlLoaderSecurityAuditEntity> securityAudits;
    
    // JPA lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (executionId == null) {
            executionId = "EXEC-" + correlationId + "-" + System.currentTimeMillis();
        }
        if (executionStatus == null) {
            executionStatus = ExecutionStatus.SUBMITTED;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        // Calculate duration if both start and end dates are available
        if (startedDate != null && completedDate != null && durationMs == null) {
            durationMs = Duration.between(startedDate, completedDate).toMillis();
        }
        
        // Calculate throughput if duration and total records are available
        if (durationMs != null && durationMs > 0 && totalRecords != null && totalRecords > 0) {
            throughputRecordsPerSec = (totalRecords.doubleValue() * 1000.0) / durationMs.doubleValue();
        }
    }
    
    // Enums
    public enum ExecutionStatus {
        SUBMITTED,
        GENERATING_CONTROL,
        EXECUTING,
        SUCCESS,
        SUCCESS_WITH_WARNINGS,
        FAILED,
        CANCELLED,
        TIMEOUT
    }
    
    // Utility methods
    public boolean isNotificationSent() {
        return "Y".equalsIgnoreCase(notificationSent);
    }
    
    public boolean isCleanupCompleted() {
        return "Y".equalsIgnoreCase(cleanupCompleted);
    }
    
    public boolean isArchived() {
        return "Y".equalsIgnoreCase(archived);
    }
    
    public boolean isSuccessful() {
        return executionStatus == ExecutionStatus.SUCCESS || 
               executionStatus == ExecutionStatus.SUCCESS_WITH_WARNINGS;
    }
    
    public boolean isFailed() {
        return executionStatus == ExecutionStatus.FAILED ||
               executionStatus == ExecutionStatus.CANCELLED ||
               executionStatus == ExecutionStatus.TIMEOUT;
    }
    
    public boolean hasWarnings() {
        return executionStatus == ExecutionStatus.SUCCESS_WITH_WARNINGS ||
               (warningRecords != null && warningRecords > 0) ||
               (warningMessages != null && !warningMessages.trim().isEmpty());
    }
    
    public boolean hasErrors() {
        return isFailed() || 
               (rejectedRecords != null && rejectedRecords > 0) ||
               (errorMessage != null && !errorMessage.trim().isEmpty());
    }
    
    public boolean isRetryable() {
        return isFailed() && 
               retryCount < maxRetries && 
               executionStatus != ExecutionStatus.CANCELLED;
    }
    
    public boolean isCompleted() {
        return completedDate != null;
    }
    
    public boolean isInProgress() {
        return executionStatus == ExecutionStatus.EXECUTING ||
               executionStatus == ExecutionStatus.GENERATING_CONTROL;
    }
    
    /**
     * Calculate success rate as percentage.
     */
    public double getSuccessRate() {
        if (totalRecords == null || totalRecords == 0) {
            return 0.0;
        }
        return (successfulRecords != null ? successfulRecords.doubleValue() : 0.0) / totalRecords.doubleValue() * 100.0;
    }
    
    /**
     * Calculate error rate as percentage.
     */
    public double getErrorRate() {
        if (totalRecords == null || totalRecords == 0) {
            return 0.0;
        }
        return (rejectedRecords != null ? rejectedRecords.doubleValue() : 0.0) / totalRecords.doubleValue() * 100.0;
    }
    
    /**
     * Get execution duration in human readable format.
     */
    public String getFormattedDuration() {
        if (durationMs == null) {
            return "N/A";
        }
        
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds % 60);
        } else {
            return String.format("%d.%03ds", seconds, durationMs % 1000);
        }
    }
    
    /**
     * Get formatted throughput display.
     */
    public String getFormattedThroughput() {
        if (throughputRecordsPerSec == null) {
            return "N/A";
        }
        
        if (throughputRecordsPerSec > 1000000) {
            return String.format("%.2fM records/sec", throughputRecordsPerSec / 1000000.0);
        } else if (throughputRecordsPerSec > 1000) {
            return String.format("%.2fK records/sec", throughputRecordsPerSec / 1000.0);
        } else {
            return String.format("%.2f records/sec", throughputRecordsPerSec);
        }
    }
    
    /**
     * Get execution summary for monitoring and reporting.
     */
    public String getExecutionSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("File: ").append(fileName)
               .append(", Status: ").append(executionStatus)
               .append(", Records: ").append(totalRecords)
               .append(", Success: ").append(successfulRecords)
               .append(", Errors: ").append(rejectedRecords)
               .append(", Duration: ").append(getFormattedDuration())
               .append(", Throughput: ").append(getFormattedThroughput());
        
        if (sqlLoaderReturnCode != null) {
            summary.append(", Return Code: ").append(sqlLoaderReturnCode);
        }
        
        return summary.toString();
    }
    
    /**
     * Set execution as started with current timestamp.
     */
    public void markAsStarted() {
        this.startedDate = LocalDateTime.now();
        this.executionStatus = ExecutionStatus.EXECUTING;
    }
    
    /**
     * Set execution as completed with current timestamp and final status.
     */
    public void markAsCompleted(ExecutionStatus finalStatus) {
        this.completedDate = LocalDateTime.now();
        this.executionStatus = finalStatus;
        if (startedDate != null) {
            this.durationMs = Duration.between(startedDate, completedDate).toMillis();
        }
        
        // Calculate throughput
        if (durationMs != null && durationMs > 0 && totalRecords != null && totalRecords > 0) {
            this.throughputRecordsPerSec = (totalRecords.doubleValue() * 1000.0) / durationMs.doubleValue();
        }
    }
    
    /**
     * Increment retry count and set next retry date.
     */
    public void incrementRetryCount(int delayMinutes) {
        this.retryCount++;
        this.nextRetryDate = LocalDateTime.now().plusMinutes(delayMinutes);
        this.executionStatus = ExecutionStatus.SUBMITTED;
    }
    
    /**
     * Validate execution entity for business rules.
     */
    public void validateExecution() {
        if (configId == null || configId.trim().isEmpty()) {
            throw new IllegalArgumentException("Config ID must be specified");
        }
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID must be specified");
        }
        
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name must be specified");
        }
        
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path must be specified");
        }
        
        if (retryCount < 0) {
            throw new IllegalArgumentException("Retry count cannot be negative");
        }
        
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative");
        }
        
        if (totalRecords != null && totalRecords < 0) {
            throw new IllegalArgumentException("Total records cannot be negative");
        }
        
        if (successfulRecords != null && successfulRecords < 0) {
            throw new IllegalArgumentException("Successful records cannot be negative");
        }
        
        if (rejectedRecords != null && rejectedRecords < 0) {
            throw new IllegalArgumentException("Rejected records cannot be negative");
        }
    }
}