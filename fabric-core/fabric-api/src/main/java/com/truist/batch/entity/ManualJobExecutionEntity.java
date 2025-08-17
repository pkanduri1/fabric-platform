package com.truist.batch.entity;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * POJO for Manual Job Execution table.
 * 
 * Tracks complete execution lifecycle including performance metrics, error
 * handling, and monitoring integration for banking-grade batch processing
 * with SOX compliance and enterprise observability features.
 * 
 * Enterprise Standards:
 * - Comprehensive execution audit trail
 * - Performance metrics collection and SLA monitoring
 * - Error tracking with detailed diagnostics
 * - Regulatory compliance with data retention policies
 * - Correlation tracking for distributed processing
 * 
 * Security Considerations:
 * - Execution parameter encryption and masking
 * - Environment-based access control
 * - Secure error message handling (no sensitive data exposure)
 * - Audit trail for execution access and modifications
 * 
 * @author Senior Full Stack Developer Agent
 * @version 2.0
 * @since US001 Phase 2 - Job Execution Tracking
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ManualJobExecutionEntity {

    /**
     * Unique execution identifier with structured format: exec_{system}_{timestamp}_{hash}
     * Example: exec_core_1691234567890_a1b2c3d4
     */
    @EqualsAndHashCode.Include
    private String executionId;

    /**
     * Foreign key reference to the job configuration being executed.
     * Links execution to its configuration for audit and management.
     */
    private String configId;

    /**
     * Job name copied from configuration for fast query access.
     * Denormalized for performance in monitoring and reporting queries.
     */
    private String jobName;

    /**
     * Type of execution: MANUAL, SCHEDULED, TRIGGERED, RETRY.
     * Indicates how the execution was initiated for audit purposes.
     */
    private String executionType;

    /**
     * Source that triggered the execution (USER_INTERFACE, API, SCHEDULER, etc.).
     * Provides context for execution source tracking and security auditing.
     */
    private String triggerSource;

    /**
     * Current execution status: STARTED, RUNNING, COMPLETED, FAILED, CANCELLED, RETRY.
     * Real-time status for monitoring and operational control.
     */
    private String status;

    /**
     * Execution start timestamp.
     * Critical for performance monitoring, SLA tracking, and scheduling.
     */
    private LocalDateTime startTime;

    /**
     * Execution end timestamp.
     * Used for duration calculation and completion tracking.
     */
    private LocalDateTime endTime;

    /**
     * Execution duration in seconds for performance analysis.
     * Calculated field for reporting and SLA monitoring.
     */
    private BigDecimal durationSeconds;

    /**
     * Total number of records processed during execution.
     * Key performance metric for capacity planning and monitoring.
     */
    private Long recordsProcessed;

    /**
     * Number of records processed successfully.
     * Quality metric for data processing validation.
     */
    private Long recordsSuccess;

    /**
     * Number of records that failed processing.
     * Error tracking metric for quality assurance.
     */
    private Long recordsError;

    /**
     * Percentage of records with errors (0-100).
     * Calculated quality metric for threshold monitoring and alerting.
     */
    private BigDecimal errorPercentage;

    /**
     * Primary error message for execution failures.
     * Business-friendly error description without sensitive data.
     */
    private String errorMessage;

    /**
     * Detailed error stack trace for technical troubleshooting.
     * Technical diagnostic information for development and support teams.
     */
    private String errorStackTrace;

    /**
     * Number of retry attempts for this execution.
     * Retry tracking for resilience monitoring and configuration tuning.
     */
    private Integer retryCount;

    /**
     * JSON serialized execution parameters.
     * Contains job-specific parameters, may include encrypted sensitive data.
     */
    private String executionParameters;

    /**
     * Detailed execution log for troubleshooting and audit.
     * Comprehensive log data for analysis and compliance reporting.
     */
    private String executionLog;

    /**
     * Location of output files or results.
     * Reference to execution outputs for downstream processing and validation.
     */
    private String outputLocation;

    /**
     * Correlation ID for distributed tracing and monitoring.
     * Links execution across multiple systems and services.
     */
    private String correlationId;

    /**
     * Flag indicating if monitoring alerts have been sent.
     * Prevents duplicate alerting and tracks notification status.
     */
    private Character monitoringAlertsSent;

    /**
     * User who executed the job.
     * Required for audit trail and access control validation.
     */
    private String executedBy;

    /**
     * Host or server where execution occurred.
     * Infrastructure tracking for capacity planning and troubleshooting.
     */
    private String executionHost;

    /**
     * Execution environment (DEVELOPMENT, TEST, STAGING, PRODUCTION).
     * Environment context for security and compliance controls.
     */
    private String executionEnvironment;

    /**
     * Record creation timestamp.
     * Audit timestamp for record lifecycle tracking.
     */
    private LocalDateTime createdDate;

    /**
     * Check if execution is currently active (started or running).
     * 
     * @return true if execution is in progress
     */
    public boolean isActive() {
        return "STARTED".equals(status) || "RUNNING".equals(status);
    }

    /**
     * Check if execution completed successfully.
     * 
     * @return true if execution status is COMPLETED
     */
    public boolean isSuccessful() {
        return "COMPLETED".equals(status);
    }

    /**
     * Check if execution failed or was cancelled.
     * 
     * @return true if execution failed or was cancelled
     */
    public boolean isFailedOrCancelled() {
        return "FAILED".equals(status) || "CANCELLED".equals(status);
    }

    /**
     * Check if alerts have been sent for this execution.
     * 
     * @return true if monitoring alerts have been sent
     */
    public boolean areAlertsSent() {
        return Character.valueOf('Y').equals(monitoringAlertsSent);
    }

    /**
     * Calculate and update execution duration based on start and end times.
     * Used for real-time duration tracking during execution.
     */
    public void calculateDuration() {
        if (startTime != null && endTime != null) {
            long durationMillis = java.time.Duration.between(startTime, endTime).toMillis();
            this.durationSeconds = BigDecimal.valueOf(durationMillis / 1000.0).setScale(2, BigDecimal.ROUND_HALF_UP);
        }
    }

    /**
     * Calculate and update error percentage based on processed and error counts.
     * Used for quality metrics and threshold monitoring.
     */
    public void calculateErrorPercentage() {
        if (recordsProcessed != null && recordsProcessed > 0 && recordsError != null) {
            double errorRate = (recordsError.doubleValue() / recordsProcessed.doubleValue()) * 100.0;
            this.errorPercentage = BigDecimal.valueOf(errorRate).setScale(2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.errorPercentage = BigDecimal.ZERO;
        }
    }

    /**
     * Mark execution as completed with success metrics.
     * 
     * @param recordsProcessed total records processed
     * @param recordsSuccess successful records
     * @param recordsError failed records
     */
    public void markCompleted(Long recordsProcessed, Long recordsSuccess, Long recordsError) {
        this.status = "COMPLETED";
        this.endTime = LocalDateTime.now();
        this.recordsProcessed = recordsProcessed;
        this.recordsSuccess = recordsSuccess;
        this.recordsError = recordsError;
        calculateDuration();
        calculateErrorPercentage();
    }

    /**
     * Mark execution as failed with error information.
     * 
     * @param errorMessage the error message
     * @param errorStackTrace detailed stack trace
     */
    public void markFailed(String errorMessage, String errorStackTrace) {
        this.status = "FAILED";
        this.endTime = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.errorStackTrace = errorStackTrace;
        calculateDuration();
    }

    /**
     * Mark execution as cancelled with reason.
     * 
     * @param cancellationReason reason for cancellation
     */
    public void markCancelled(String cancellationReason) {
        this.status = "CANCELLED";
        this.endTime = LocalDateTime.now();
        this.errorMessage = "Execution cancelled: " + cancellationReason;
        calculateDuration();
    }

    /**
     * Update execution status to running with optional progress information.
     * 
     * @param recordsProcessedSoFar current progress count
     */
    public void updateProgress(Long recordsProcessedSoFar) {
        this.status = "RUNNING";
        this.recordsProcessed = recordsProcessedSoFar;
    }

    /**
     * Mark that monitoring alerts have been sent for this execution.
     * Prevents duplicate alerting and tracks notification status.
     */
    public void markAlertsSent() {
        this.monitoringAlertsSent = 'Y';
    }

    /**
     * Increment retry count for retry tracking.
     * Used when execution is retried after failure.
     */
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount != null ? this.retryCount : 0) + 1;
    }

    /**
     * Get execution success rate as percentage.
     * 
     * @return success rate percentage (0-100)
     */
    public BigDecimal getSuccessRate() {
        if (recordsProcessed != null && recordsProcessed > 0 && recordsSuccess != null) {
            double successRate = (recordsSuccess.doubleValue() / recordsProcessed.doubleValue()) * 100.0;
            return BigDecimal.valueOf(successRate).setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Check if execution exceeds error threshold.
     * 
     * @param thresholdPercentage error threshold percentage
     * @return true if error percentage exceeds threshold
     */
    public boolean exceedsErrorThreshold(BigDecimal thresholdPercentage) {
        return errorPercentage != null && 
               thresholdPercentage != null && 
               errorPercentage.compareTo(thresholdPercentage) > 0;
    }

    /**
     * Get execution duration in milliseconds.
     * 
     * @return duration in milliseconds
     */
    public Long getDurationMillis() {
        if (durationSeconds != null) {
            return durationSeconds.multiply(BigDecimal.valueOf(1000)).longValue();
        }
        return null;
    }

    /**
     * Check if execution is long-running based on threshold.
     * 
     * @param thresholdSeconds duration threshold in seconds
     * @return true if execution duration exceeds threshold
     */
    public boolean isLongRunning(Long thresholdSeconds) {
        if (durationSeconds != null && thresholdSeconds != null) {
            return durationSeconds.compareTo(BigDecimal.valueOf(thresholdSeconds)) > 0;
        }
        
        // For active executions, calculate current duration
        if (isActive() && startTime != null) {
            long currentDurationSeconds = java.time.Duration.between(startTime, LocalDateTime.now()).toSeconds();
            return currentDurationSeconds > thresholdSeconds;
        }
        
        return false;
    }
}