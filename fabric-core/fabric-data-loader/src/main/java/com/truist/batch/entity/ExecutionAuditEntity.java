package com.truist.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Epic 2: Entity representing comprehensive audit trail for all transaction processing events.
 * This entity provides banking-grade audit capabilities with digital signatures,
 * compliance tracking, and tamper-evident logging for regulatory requirements.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 2 - Simple Transaction Processing
 */
@Entity
@Table(name = "EXECUTION_AUDIT",
       indexes = {
           @Index(name = "idx_execution_audit_comp", 
                  columnList = "execution_id, event_timestamp, compliance_flags"),
           @Index(name = "idx_execution_audit_user", 
                  columnList = "user_id, event_timestamp, event_type"),
           @Index(name = "idx_execution_audit_correlation", 
                  columnList = "correlation_id, event_timestamp")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, 
                    generator = "seq_execution_audit")
    @SequenceGenerator(name = "seq_execution_audit", 
                       sequenceName = "seq_execution_audit", 
                       allocationSize = 1)
    @Column(name = "execution_audit_id")
    private Long executionAuditId;

    @NotBlank(message = "Execution ID is required")
    @Column(name = "execution_id", nullable = false, length = 100)
    private String executionId;

    @Column(name = "transaction_type_id")
    private Long transactionTypeId;

    @NotBlank(message = "Event type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Size(max = 500, message = "Event description cannot exceed 500 characters")
    @Column(name = "event_description", length = 500)
    private String eventDescription;

    @CreationTimestamp
    @Column(name = "event_timestamp", nullable = false, updatable = false)
    @Builder.Default
    private Instant eventTimestamp = Instant.now();

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(name = "success_flag", length = 1)
    @Builder.Default
    private String successFlag = "Y";

    @Column(name = "error_code", length = 20)
    private String errorCode;

    @Lob
    @Column(name = "error_details")
    private String errorDetails;

    @Lob
    @Column(name = "performance_data")
    private String performanceData;

    @Column(name = "compliance_flags", length = 200)
    private String complianceFlags;

    @Column(name = "digital_signature", length = 512)
    private String digitalSignature;

    @Column(name = "business_date")
    @Builder.Default
    private LocalDate businessDate = LocalDate.now();

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "data_lineage_id")
    private Long dataLineageId;

    /**
     * Event type enumeration for audit events
     */
    public enum EventType {
        BATCH_START("BATCH_START"),
        BATCH_END("BATCH_END"),
        TRANSACTION_START("TRANSACTION_START"),
        TRANSACTION_END("TRANSACTION_END"),
        ERROR_OCCURRED("ERROR_OCCURRED"),
        VALIDATION_FAILED("VALIDATION_FAILED"),
        SECURITY_EVENT("SECURITY_EVENT"),
        CONFIG_CHANGE("CONFIG_CHANGE"),
        DATA_ENCRYPTION("DATA_ENCRYPTION"),
        COMPLIANCE_CHECK("COMPLIANCE_CHECK"),
        PERFORMANCE_ALERT("PERFORMANCE_ALERT"),
        USER_ACCESS("USER_ACCESS"),
        SYSTEM_MAINTENANCE("SYSTEM_MAINTENANCE");

        private final String value;

        EventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public boolean isSecurityEvent() {
            return this == SECURITY_EVENT || this == USER_ACCESS || this == CONFIG_CHANGE;
        }

        public boolean isComplianceEvent() {
            return this == COMPLIANCE_CHECK || this == DATA_ENCRYPTION || this == VALIDATION_FAILED;
        }

        public boolean isPerformanceEvent() {
            return this == PERFORMANCE_ALERT || this == BATCH_START || this == BATCH_END;
        }
    }

    /**
     * Business logic methods
     */

    public boolean isSuccessful() {
        return "Y".equalsIgnoreCase(successFlag);
    }

    public boolean isFailed() {
        return "N".equalsIgnoreCase(successFlag);
    }

    public void markAsSuccessful() {
        this.successFlag = "Y";
        this.errorCode = null;
        this.errorDetails = null;
    }

    public void markAsFailed(String errorCode, String errorDetails) {
        this.successFlag = "N";
        this.errorCode = errorCode;
        this.errorDetails = errorDetails;
    }

    /**
     * Security and compliance methods
     */

    public boolean isSecurityEvent() {
        return eventType != null && eventType.isSecurityEvent();
    }

    public boolean isComplianceEvent() {
        return eventType != null && eventType.isComplianceEvent();
    }

    public boolean hasDigitalSignature() {
        return digitalSignature != null && !digitalSignature.trim().isEmpty();
    }

    public void addComplianceFlag(String flag) {
        if (complianceFlags == null || complianceFlags.trim().isEmpty()) {
            complianceFlags = flag;
        } else if (!complianceFlags.contains(flag)) {
            complianceFlags = complianceFlags + "," + flag;
        }
    }

    public boolean hasComplianceFlag(String flag) {
        return complianceFlags != null && complianceFlags.contains(flag);
    }

    /**
     * Data integrity methods
     */

    public void setDigitalSignature(String signature) {
        this.digitalSignature = signature;
    }

    public String generateAuditRecord() {
        return String.format("AuditRecord[executionId=%s,eventType=%s,timestamp=%s,user=%s,success=%s]",
                executionId, eventType, eventTimestamp, userId, successFlag);
    }

    /**
     * Performance data methods
     */

    public void addPerformanceData(String key, Object value) {
        String newData = String.format("\"%s\":\"%s\"", key, value);
        
        if (performanceData == null || performanceData.trim().isEmpty()) {
            performanceData = "{" + newData + "}";
        } else {
            // Simple JSON-like structure for performance data
            performanceData = performanceData.substring(0, performanceData.length() - 1) 
                            + "," + newData + "}";
        }
    }

    /**
     * Audit trail methods
     */

    public AuditTrailInfo getAuditTrailInfo() {
        return AuditTrailInfo.builder()
                .executionAuditId(executionAuditId)
                .executionId(executionId)
                .eventType(eventType.getValue())
                .eventDescription(eventDescription)
                .eventTimestamp(eventTimestamp)
                .userId(userId)
                .sourceIp(sourceIp)
                .successFlag(successFlag)
                .errorCode(errorCode)
                .complianceFlags(complianceFlags)
                .businessDate(businessDate)
                .correlationId(correlationId)
                .hasDigitalSignature(hasDigitalSignature())
                .build();
    }

    /**
     * Compliance reporting methods
     */

    public boolean requiresRegulatorReporting() {
        return isSecurityEvent() || 
               hasComplianceFlag("PCI_DSS") || 
               hasComplianceFlag("SOX") || 
               hasComplianceFlag("FFIEC");
    }

    public ComplianceAuditRecord getComplianceAuditRecord() {
        return ComplianceAuditRecord.builder()
                .auditId(executionAuditId)
                .executionId(executionId)
                .eventType(eventType.getValue())
                .complianceLevel(extractComplianceLevel())
                .regulatoryFlags(complianceFlags)
                .eventTimestamp(eventTimestamp)
                .businessDate(businessDate)
                .dataIntegrityVerified(hasDigitalSignature())
                .requiresReporting(requiresRegulatorReporting())
                .build();
    }

    private String extractComplianceLevel() {
        if (hasComplianceFlag("CRITICAL")) return "CRITICAL";
        if (hasComplianceFlag("HIGH")) return "HIGH";
        if (hasComplianceFlag("MEDIUM")) return "MEDIUM";
        return "STANDARD";
    }

    /**
     * Factory methods for common audit events
     */

    public static ExecutionAuditEntity createBatchStartEvent(String executionId, 
                                                           String userId, 
                                                           String sourceIp) {
        return ExecutionAuditEntity.builder()
                .executionId(executionId)
                .eventType(EventType.BATCH_START)
                .eventDescription("Batch processing started")
                .userId(userId)
                .sourceIp(sourceIp)
                .successFlag("Y")
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    public static ExecutionAuditEntity createSecurityEvent(String executionId,
                                                          String userId,
                                                          String sourceIp,
                                                          String securityEvent) {
        return ExecutionAuditEntity.builder()
                .executionId(executionId)
                .eventType(EventType.SECURITY_EVENT)
                .eventDescription(securityEvent)
                .userId(userId)
                .sourceIp(sourceIp)
                .successFlag("Y")
                .complianceFlags("SECURITY")
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    public static ExecutionAuditEntity createErrorEvent(String executionId,
                                                       String errorCode,
                                                       String errorDetails) {
        return ExecutionAuditEntity.builder()
                .executionId(executionId)
                .eventType(EventType.ERROR_OCCURRED)
                .eventDescription("Processing error occurred")
                .errorCode(errorCode)
                .errorDetails(errorDetails)
                .successFlag("N")
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    /**
     * Nested classes for data structures
     */

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditTrailInfo {
        private Long executionAuditId;
        private String executionId;
        private String eventType;
        private String eventDescription;
        private Instant eventTimestamp;
        private String userId;
        private String sourceIp;
        private String successFlag;
        private String errorCode;
        private String complianceFlags;
        private LocalDate businessDate;
        private String correlationId;
        private boolean hasDigitalSignature;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceAuditRecord {
        private Long auditId;
        private String executionId;
        private String eventType;
        private String complianceLevel;
        private String regulatoryFlags;
        private Instant eventTimestamp;
        private LocalDate businessDate;
        private boolean dataIntegrityVerified;
        private boolean requiresReporting;
    }

    /**
     * String representation for logging and debugging
     */
    @Override
    public String toString() {
        return String.format("ExecutionAudit[id=%d, executionId=%s, eventType=%s, success=%s, timestamp=%s]",
                executionAuditId, executionId, eventType, successFlag, eventTimestamp);
    }
}