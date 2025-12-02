package com.fabric.batch.idempotency.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Entity representing audit trail for idempotency state changes.
 * Provides comprehensive tracking of all idempotent operation state transitions
 * for compliance, debugging, and monitoring purposes.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Entity
@Table(name = "fabric_idempotency_audit", schema = "CM3INT")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FabricIdempotencyAuditEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;
    
    @Column(name = "idempotency_key", length = 128, nullable = false)
    private String idempotencyKey;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "old_state", length = 20)
    private FabricIdempotencyKeyEntity.ProcessingState oldState;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "new_state", length = 20)
    private FabricIdempotencyKeyEntity.ProcessingState newState;
    
    @Column(name = "state_change_reason", length = 500)
    private String stateChangeReason;
    
    @Column(name = "changed_by", length = 50, nullable = false)
    private String changedBy;
    
    @Column(name = "change_date", nullable = false)
    private LocalDateTime changeDate;
    
    @Lob
    @Column(name = "processing_context")
    private String processingContext; // JSON metadata
    
    @Column(name = "client_ip", length = 45)
    private String clientIp;
    
    @Column(name = "user_agent", length = 1000)
    private String userAgent;
    
    @Column(name = "device_fingerprint", length = 100)
    private String deviceFingerprint;
    
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    @Column(name = "business_context", length = 500)
    private String businessContext;
    
    @PrePersist
    protected void onCreate() {
        if (changeDate == null) {
            changeDate = LocalDateTime.now();
        }
    }
    
    // Utility methods
    
    /**
     * Checks if this audit entry represents a state transition
     */
    public boolean isStateTransition() {
        return oldState != null && newState != null && !oldState.equals(newState);
    }
    
    /**
     * Checks if this represents a failure transition
     */
    public boolean isFailureTransition() {
        return newState == FabricIdempotencyKeyEntity.ProcessingState.FAILED;
    }
    
    /**
     * Checks if this represents a completion transition
     */
    public boolean isCompletionTransition() {
        return newState == FabricIdempotencyKeyEntity.ProcessingState.COMPLETED;
    }
    
    /**
     * Checks if this represents a retry attempt
     */
    public boolean isRetryAttempt() {
        return oldState == FabricIdempotencyKeyEntity.ProcessingState.FAILED && 
               newState == FabricIdempotencyKeyEntity.ProcessingState.IN_PROGRESS;
    }
    
    /**
     * Gets a human-readable description of the state transition
     */
    public String getTransitionDescription() {
        if (!isStateTransition()) {
            return "No state transition";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("State changed from ").append(oldState.name())
          .append(" to ").append(newState.name());
        
        if (stateChangeReason != null && !stateChangeReason.trim().isEmpty()) {
            sb.append(" - ").append(stateChangeReason);
        }
        
        return sb.toString();
    }
    
    /**
     * Creates a summary for logging purposes
     */
    public String getSummary() {
        return String.format("AuditEntry[id=%d, key=%s, %s → %s, by=%s, at=%s]",
                auditId, idempotencyKey, 
                oldState != null ? oldState.name() : "NULL",
                newState != null ? newState.name() : "NULL",
                changedBy, changeDate);
    }
    
    /**
     * Checks if the audit entry contains client information
     */
    public boolean hasClientInfo() {
        return (clientIp != null && !clientIp.trim().isEmpty()) ||
               (userAgent != null && !userAgent.trim().isEmpty()) ||
               (deviceFingerprint != null && !deviceFingerprint.trim().isEmpty());
    }
    
    /**
     * Checks if the audit entry contains processing context
     */
    public boolean hasProcessingContext() {
        return processingContext != null && !processingContext.trim().isEmpty();
    }
    
    /**
     * Checks if the audit entry contains business context
     */
    public boolean hasBusinessContext() {
        return businessContext != null && !businessContext.trim().isEmpty();
    }
    
    /**
     * Gets the elapsed time since the audit entry was created (for recent entries)
     */
    public long getMinutesSinceCreated() {
        if (changeDate == null) return 0;
        return java.time.Duration.between(changeDate, LocalDateTime.now()).toMinutes();
    }
    
    /**
     * Checks if this audit entry is recent (within last hour)
     */
    public boolean isRecent() {
        return getMinutesSinceCreated() <= 60;
    }
    
    /**
     * Factory method to create audit entry for state transition
     */
    public static FabricIdempotencyAuditEntity createStateTransition(
            String idempotencyKey,
            FabricIdempotencyKeyEntity.ProcessingState oldState,
            FabricIdempotencyKeyEntity.ProcessingState newState,
            String reason,
            String changedBy) {
        
        return FabricIdempotencyAuditEntity.builder()
                .idempotencyKey(idempotencyKey)
                .oldState(oldState)
                .newState(newState)
                .stateChangeReason(reason)
                .changedBy(changedBy)
                .changeDate(LocalDateTime.now())
                .build();
    }
    
    /**
     * Factory method to create audit entry for system events
     */
    public static FabricIdempotencyAuditEntity createSystemEvent(
            String idempotencyKey,
            String eventDescription,
            String processingContext) {
        
        return FabricIdempotencyAuditEntity.builder()
                .idempotencyKey(idempotencyKey)
                .newState(null) // System events don't change state
                .stateChangeReason(eventDescription)
                .changedBy("SYSTEM")
                .changeDate(LocalDateTime.now())
                .processingContext(processingContext)
                .build();
    }
}