package com.truist.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Epic 2: Entity representing real-time processing status for parallel transaction processing.
 * This entity tracks the status, performance metrics, and health information for each
 * processing thread or partition in the parallel processing architecture.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 2 - Simple Transaction Processing
 */
@Entity
@Table(name = "BATCH_PROCESSING_STATUS",
       indexes = {
           @Index(name = "idx_proc_status_execution", 
                  columnList = "execution_id, transaction_type_id, processing_status"),
           @Index(name = "idx_proc_status_heartbeat", 
                  columnList = "last_heartbeat, processing_status")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProcessingStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, 
                    generator = "seq_batch_processing_status")
    @SequenceGenerator(name = "seq_batch_processing_status", 
                       sequenceName = "seq_batch_processing_status", 
                       allocationSize = 1)
    @Column(name = "status_id")
    private Long statusId;

    @NotBlank(message = "Execution ID is required")
    @Column(name = "execution_id", nullable = false, length = 100)
    private String executionId;

    @NotNull(message = "Transaction type ID is required")
    @Column(name = "transaction_type_id", nullable = false)
    private Long transactionTypeId;

    @NotBlank(message = "Processing status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private ProcessingStatus processingStatus;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "records_processed")
    @Builder.Default
    private Long recordsProcessed = 0L;

    @Column(name = "records_failed")
    @Builder.Default
    private Long recordsFailed = 0L;

    @Column(name = "thread_id", length = 50)
    private String threadId;

    @Lob
    @Column(name = "performance_metrics")
    private String performanceMetrics;

    @Column(name = "memory_usage_mb")
    private Long memoryUsageMb;

    @DecimalMin(value = "0.00", message = "CPU usage cannot be negative")
    @DecimalMax(value = "100.00", message = "CPU usage cannot exceed 100%")
    @Column(name = "cpu_usage_percent", precision = 5, scale = 2)
    private BigDecimal cpuUsagePercent;

    @Column(name = "last_heartbeat")
    @Builder.Default
    private Instant lastHeartbeat = Instant.now();

    @Lob
    @Column(name = "error_details")
    private String errorDetails;

    @DecimalMin(value = "0.00", message = "Data quality score cannot be negative")
    @DecimalMax(value = "100.00", message = "Data quality score cannot exceed 100")
    @Column(name = "data_quality_score", precision = 5, scale = 2)
    private BigDecimal dataQualityScore;

    /**
     * Processing status enumeration
     */
    public enum ProcessingStatus {
        INITIALIZED("INITIALIZED"),
        RUNNING("RUNNING"),
        COMPLETED("COMPLETED"),
        FAILED("FAILED"),
        CANCELLED("CANCELLED");

        private final String value;

        ProcessingStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public boolean isTerminal() {
            return this == COMPLETED || this == FAILED || this == CANCELLED;
        }

        public boolean isActive() {
            return this == INITIALIZED || this == RUNNING;
        }
    }

    /**
     * Business logic methods
     */

    public boolean isRunning() {
        return ProcessingStatus.RUNNING.equals(processingStatus);
    }

    public boolean isCompleted() {
        return ProcessingStatus.COMPLETED.equals(processingStatus);
    }

    public boolean isFailed() {
        return ProcessingStatus.FAILED.equals(processingStatus);
    }

    public boolean isTerminal() {
        return processingStatus != null && processingStatus.isTerminal();
    }

    public void markAsRunning() {
        this.processingStatus = ProcessingStatus.RUNNING;
        this.lastHeartbeat = Instant.now();
    }

    public void markAsCompleted() {
        this.processingStatus = ProcessingStatus.COMPLETED;
        this.endTime = Instant.now();
        this.lastHeartbeat = Instant.now();
    }

    public void markAsFailed(String errorDetails) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.endTime = Instant.now();
        this.errorDetails = errorDetails;
        this.lastHeartbeat = Instant.now();
    }

    public void markAsCancelled() {
        this.processingStatus = ProcessingStatus.CANCELLED;
        this.endTime = Instant.now();
        this.lastHeartbeat = Instant.now();
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = Instant.now();
    }

    /**
     * Performance calculation methods
     */

    public long getProcessingDurationMs() {
        if (endTime == null || startTime == null) {
            return 0;
        }
        return endTime.toEpochMilli() - startTime.toEpochMilli();
    }

    public double getSuccessRate() {
        long totalRecords = recordsProcessed + recordsFailed;
        if (totalRecords == 0) {
            return 0.0;
        }
        return (recordsProcessed * 100.0) / totalRecords;
    }

    public double getThroughputPerSecond() {
        long durationMs = getProcessingDurationMs();
        if (durationMs == 0 || recordsProcessed == 0) {
            return 0.0;
        }
        return (recordsProcessed * 1000.0) / durationMs;
    }

    public boolean isStale(long maxHeartbeatIntervalMs) {
        if (lastHeartbeat == null || !processingStatus.isActive()) {
            return false;
        }
        
        Instant cutoff = Instant.now().minusMillis(maxHeartbeatIntervalMs);
        return lastHeartbeat.isBefore(cutoff);
    }

    /**
     * Resource utilization methods
     */

    public void updateResourceUsage(long memoryMb, double cpuPercent) {
        this.memoryUsageMb = memoryMb;
        this.cpuUsagePercent = BigDecimal.valueOf(cpuPercent);
        updateHeartbeat();
    }

    public boolean isResourceStressed() {
        if (memoryUsageMb == null || cpuUsagePercent == null) {
            return false;
        }
        
        // Consider stressed if memory > 80% of max heap or CPU > 90%
        return memoryUsageMb > 1000 || // 1GB+ memory usage
               cpuUsagePercent.compareTo(BigDecimal.valueOf(90.0)) > 0;
    }

    /**
     * Data quality methods
     */

    public void updateDataQualityScore(double score) {
        this.dataQualityScore = BigDecimal.valueOf(score);
    }

    public boolean hasGoodDataQuality() {
        return dataQualityScore != null && 
               dataQualityScore.compareTo(BigDecimal.valueOf(85.0)) >= 0;
    }

    /**
     * Progress tracking methods
     */

    public void incrementRecordsProcessed(long count) {
        this.recordsProcessed = (this.recordsProcessed == null ? 0 : this.recordsProcessed) + count;
        updateHeartbeat();
    }

    public void incrementRecordsFailed(long count) {
        this.recordsFailed = (this.recordsFailed == null ? 0 : this.recordsFailed) + count;
        updateHeartbeat();
    }

    public long getTotalRecords() {
        return (recordsProcessed == null ? 0 : recordsProcessed) + 
               (recordsFailed == null ? 0 : recordsFailed);
    }

    /**
     * Audit and compliance methods
     */

    public ProcessingStatusAudit getAuditInfo() {
        return ProcessingStatusAudit.builder()
                .statusId(statusId)
                .executionId(executionId)
                .transactionTypeId(transactionTypeId)
                .processingStatus(processingStatus.getValue())
                .startTime(startTime)
                .endTime(endTime)
                .recordsProcessed(recordsProcessed)
                .recordsFailed(recordsFailed)
                .successRate(getSuccessRate())
                .throughputPerSecond(getThroughputPerSecond())
                .processingDurationMs(getProcessingDurationMs())
                .dataQualityScore(dataQualityScore)
                .threadId(threadId)
                .lastHeartbeat(lastHeartbeat)
                .build();
    }

    /**
     * Nested class for audit information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingStatusAudit {
        private Long statusId;
        private String executionId;
        private Long transactionTypeId;
        private String processingStatus;
        private Instant startTime;
        private Instant endTime;
        private Long recordsProcessed;
        private Long recordsFailed;
        private Double successRate;
        private Double throughputPerSecond;
        private Long processingDurationMs;
        private BigDecimal dataQualityScore;
        private String threadId;
        private Instant lastHeartbeat;
    }

    /**
     * String representation for logging and debugging
     */
    @Override
    public String toString() {
        return String.format("BatchProcessingStatus[id=%d, executionId=%s, status=%s, processed=%d, failed=%d, thread=%s]",
                statusId, executionId, processingStatus, recordsProcessed, recordsFailed, threadId);
    }
}