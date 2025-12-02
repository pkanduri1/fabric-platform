package com.fabric.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Epic 2: Entity representing temporary staging data for parallel transaction processing.
 * This entity supports high-volume transaction processing with partitioning, correlation tracking,
 * and comprehensive audit capabilities for banking compliance.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 2 - Simple Transaction Processing
 */
@Entity
@Table(name = "BATCH_TEMP_STAGING",
       indexes = {
           @Index(name = "idx_staging_execution_type", 
                  columnList = "execution_id, transaction_type_id, processing_status"),
           @Index(name = "idx_staging_correlation", 
                  columnList = "correlation_id, created_timestamp"),
           @Index(name = "idx_staging_business_date", 
                  columnList = "business_date, processing_status")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTempStagingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, 
                    generator = "seq_batch_temp_staging")
    @SequenceGenerator(name = "seq_batch_temp_staging", 
                       sequenceName = "seq_batch_temp_staging", 
                       allocationSize = 1)
    @Column(name = "staging_id")
    private Long stagingId;

    @NotBlank(message = "Execution ID is required")
    @Column(name = "execution_id", nullable = false, length = 100)
    private String executionId;

    @NotNull(message = "Transaction type ID is required")
    @Column(name = "transaction_type_id", nullable = false)
    private Long transactionTypeId;

    @NotNull(message = "Sequence number is required")
    @Min(value = 1, message = "Sequence number must be positive")
    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @Lob
    @Column(name = "source_data")
    private String sourceData;

    @Lob
    @Column(name = "processed_data")
    private String processedData;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", length = 20)
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @NotBlank(message = "Correlation ID is required")
    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "processed_timestamp")
    private Instant processedTimestamp;

    @CreationTimestamp
    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private Instant createdTimestamp;

    @Column(name = "thread_id", length = 50)
    private String threadId;

    @Column(name = "partition_key", length = 100)
    private String partitionKey;

    @Column(name = "business_date")
    @Builder.Default
    private LocalDate businessDate = LocalDate.now();

    @Column(name = "data_hash", length = 64)
    private String dataHash;

    @Min(value = 0, message = "Retry count cannot be negative")
    @Max(value = 10, message = "Retry count cannot exceed 10")
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Processing status enumeration
     */
    public enum ProcessingStatus {
        PENDING("PENDING"),
        PROCESSING("PROCESSING"),
        COMPLETED("COMPLETED"),
        FAILED("FAILED"),
        RETRYING("RETRYING");

        private final String value;

        ProcessingStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public boolean isTerminal() {
            return this == COMPLETED || this == FAILED;
        }

        public boolean canRetry() {
            return this == FAILED || this == RETRYING;
        }
    }

    /**
     * Business logic methods
     */

    public boolean isCompleted() {
        return ProcessingStatus.COMPLETED.equals(processingStatus);
    }

    public boolean isFailed() {
        return ProcessingStatus.FAILED.equals(processingStatus);
    }

    public boolean isPending() {
        return ProcessingStatus.PENDING.equals(processingStatus);
    }

    public boolean isProcessing() {
        return ProcessingStatus.PROCESSING.equals(processingStatus);
    }

    public boolean canRetry() {
        return processingStatus.canRetry() && retryCount < 10;
    }

    public void markAsProcessing(String threadId) {
        this.processingStatus = ProcessingStatus.PROCESSING;
        this.threadId = threadId;
        this.processedTimestamp = Instant.now();
    }

    public void markAsCompleted(String processedData) {
        this.processingStatus = ProcessingStatus.COMPLETED;
        this.processedData = processedData;
        this.processedTimestamp = Instant.now();
        this.errorMessage = null;
    }

    public void markAsFailed(String errorMessage) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
        this.processedTimestamp = Instant.now();
    }

    public void markForRetry(String errorMessage) {
        this.processingStatus = ProcessingStatus.RETRYING;
        this.errorMessage = errorMessage;
        this.retryCount++;
        this.processedTimestamp = Instant.now();
    }

    public void resetForRetry() {
        this.processingStatus = ProcessingStatus.PENDING;
        this.threadId = null;
        this.processedTimestamp = null;
    }

    /**
     * Data integrity and validation methods
     */

    public boolean hasValidData() {
        return sourceData != null && !sourceData.trim().isEmpty();
    }

    public boolean isDataHashValid(String expectedHash) {
        return dataHash != null && dataHash.equals(expectedHash);
    }

    public void updateDataHash(String hash) {
        this.dataHash = hash;
    }

    /**
     * Performance and monitoring methods
     */

    public long getProcessingDurationMs() {
        if (processedTimestamp == null || createdTimestamp == null) {
            return 0;
        }
        return processedTimestamp.toEpochMilli() - createdTimestamp.toEpochMilli();
    }

    public boolean isStale(long maxProcessingTimeMs) {
        if (processingStatus != ProcessingStatus.PROCESSING) {
            return false;
        }
        
        Instant cutoff = Instant.now().minusMillis(maxProcessingTimeMs);
        return createdTimestamp.isBefore(cutoff);
    }

    /**
     * Correlation and tracing methods
     */

    public String generatePartitionKey() {
        return String.format("%s_%s_%d", 
                executionId, 
                businessDate.toString().replace("-", ""), 
                transactionTypeId);
    }

    public void updatePartitionKey() {
        this.partitionKey = generatePartitionKey();
    }

    /**
     * Audit and compliance methods
     */

    public AuditInfo getAuditInfo() {
        return AuditInfo.builder()
                .stagingId(stagingId)
                .executionId(executionId)
                .correlationId(correlationId)
                .processingStatus(processingStatus.getValue())
                .businessDate(businessDate)
                .createdTimestamp(createdTimestamp)
                .processedTimestamp(processedTimestamp)
                .retryCount(retryCount)
                .threadId(threadId)
                .build();
    }

    /**
     * Nested class for audit information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditInfo {
        private Long stagingId;
        private String executionId;
        private String correlationId;
        private String processingStatus;
        private LocalDate businessDate;
        private Instant createdTimestamp;
        private Instant processedTimestamp;
        private Integer retryCount;
        private String threadId;
    }

    /**
     * Pre-persist callback to ensure data integrity
     */
    @PrePersist
    public void prePersist() {
        if (partitionKey == null) {
            updatePartitionKey();
        }
        if (businessDate == null) {
            businessDate = LocalDate.now();
        }
    }

    /**
     * String representation for logging and debugging
     */
    @Override
    public String toString() {
        return String.format("BatchTempStaging[id=%d, executionId=%s, status=%s, sequence=%d, thread=%s]",
                stagingId, executionId, processingStatus, sequenceNumber, threadId);
    }
}