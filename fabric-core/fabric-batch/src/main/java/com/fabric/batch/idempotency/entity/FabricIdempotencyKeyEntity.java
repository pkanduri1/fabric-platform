package com.fabric.batch.idempotency.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Entity representing idempotency keys for batch jobs and API requests.
 * Provides comprehensive tracking of request processing states and enables
 * safe restart capabilities with audit trail integration.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Entity
@Table(name = "fabric_idempotency_keys", schema = "CM3INT")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FabricIdempotencyKeyEntity {
    
    @Id
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;
    
    @Column(name = "source_system", length = 50, nullable = false)
    private String sourceSystem;
    
    @Column(name = "job_name", length = 50, nullable = false)
    private String jobName;
    
    @Column(name = "transaction_id", length = 100)
    private String transactionId;
    
    @Column(name = "file_hash", length = 64)
    private String fileHash;
    
    @Column(name = "request_hash", length = 64)
    private String requestHash;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_state", length = 20, nullable = false)
    private ProcessingState processingState = ProcessingState.STARTED;
    
    @Lob
    @Column(name = "request_payload")
    private String requestPayload;
    
    @Lob
    @Column(name = "response_payload")
    private String responsePayload;
    
    @Lob
    @Column(name = "error_details")
    private String errorDetails;
    
    @Column(name = "correlation_id", length = 100, nullable = false)
    private String correlationId;
    
    @Column(name = "ttl_seconds")
    private Integer ttlSeconds = 86400; // 24 hours default
    
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
    
    @Column(name = "completed_date")
    private LocalDateTime completedDate;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "max_retries")
    private Integer maxRetries = 3;
    
    @Column(name = "created_by", length = 50, nullable = false)
    private String createdBy;
    
    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;
    
    @Column(name = "processing_node", length = 50)
    private String processingNode;
    
    @Version
    @Column(name = "lock_version")
    private Integer lockVersion = 0;
    
    @PrePersist
    @PreUpdate
    private void updateTimestamps() {
        this.lastAccessed = LocalDateTime.now();
        if (this.createdDate == null) {
            this.createdDate = LocalDateTime.now();
        }
        if (this.expiresAt == null && this.ttlSeconds != null) {
            this.expiresAt = LocalDateTime.now().plusSeconds(this.ttlSeconds);
        }
    }
    
    /**
     * Processing states for idempotent operations
     */
    public enum ProcessingState {
        STARTED("Operation has been initiated"),
        IN_PROGRESS("Operation is currently being processed"),
        COMPLETED("Operation completed successfully"),
        FAILED("Operation failed and can be retried"),
        EXPIRED("Operation has expired and can be reprocessed");
        
        private final String description;
        
        ProcessingState(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // Utility methods
    
    /**
     * Checks if the operation is in a terminal state
     */
    public boolean isTerminalState() {
        return processingState == ProcessingState.COMPLETED || 
               processingState == ProcessingState.EXPIRED;
    }
    
    /**
     * Checks if the operation can be retried
     */
    public boolean canRetry() {
        return processingState == ProcessingState.FAILED && 
               retryCount < maxRetries;
    }
    
    /**
     * Checks if the operation has expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Checks if the operation is stale (in progress for too long)
     */
    public boolean isStale(int staleTimeoutMinutes) {
        if (processingState != ProcessingState.IN_PROGRESS || createdDate == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(createdDate.plusMinutes(staleTimeoutMinutes));
    }
    
    /**
     * Increments retry count and updates state
     */
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null) ? 1 : this.retryCount + 1;
        if (this.retryCount >= this.maxRetries) {
            this.processingState = ProcessingState.FAILED;
        }
    }
    
    /**
     * Marks the operation as completed
     */
    public void markCompleted(String responsePayload) {
        this.processingState = ProcessingState.COMPLETED;
        this.completedDate = LocalDateTime.now();
        this.responsePayload = responsePayload;
        this.errorDetails = null; // Clear any previous error details
    }
    
    /**
     * Marks the operation as failed
     */
    public void markFailed(String errorDetails) {
        this.processingState = ProcessingState.FAILED;
        this.completedDate = LocalDateTime.now();
        this.errorDetails = errorDetails;
        this.responsePayload = null; // Clear any previous response
        incrementRetryCount();
    }
    
    /**
     * Marks the operation as in progress
     */
    public void markInProgress() {
        this.processingState = ProcessingState.IN_PROGRESS;
        this.completedDate = null;
        this.errorDetails = null;
    }
    
    /**
     * Gets a summary string for logging
     */
    public String getSummary() {
        return String.format("IdempotencyKey[key=%s, system=%s, job=%s, state=%s, retries=%d/%d]",
                idempotencyKey, sourceSystem, jobName, processingState, 
                retryCount != null ? retryCount : 0, 
                maxRetries != null ? maxRetries : 0);
    }
    
    /**
     * Checks if request payload storage is enabled and payload exists
     */
    public boolean hasRequestPayload() {
        return requestPayload != null && !requestPayload.trim().isEmpty();
    }
    
    /**
     * Checks if response payload storage is enabled and payload exists
     */
    public boolean hasResponsePayload() {
        return responsePayload != null && !responsePayload.trim().isEmpty();
    }
    
    /**
     * Checks if error details exist
     */
    public boolean hasErrorDetails() {
        return errorDetails != null && !errorDetails.trim().isEmpty();
    }
}