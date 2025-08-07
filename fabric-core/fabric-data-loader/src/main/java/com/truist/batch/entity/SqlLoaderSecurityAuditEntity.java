package com.truist.batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Entity representing SQL*Loader security audit events with comprehensive tracking
 * of access attempts, privilege escalations, data exposure, and compliance violations
 * for enterprise fintech regulatory requirements.
 */
@Entity
@Table(name = "sql_loader_security_audit", schema = "CM3INT")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"sqlLoaderExecution", "sqlLoaderConfig"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SqlLoaderSecurityAuditEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;
    
    @Column(name = "execution_id", length = 100)
    private String executionId;
    
    @Column(name = "config_id", length = 100)
    private String configId;
    
    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;
    
    @Column(name = "security_event_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SecurityEventType securityEventType;
    
    @Column(name = "event_description", nullable = false, length = 1000)
    private String eventDescription;
    
    @Column(name = "severity", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Severity severity = Severity.MEDIUM;
    
    @Column(name = "risk_score")
    @Builder.Default
    private Integer riskScore = 50; // Risk score 0-100
    
    @Column(name = "affected_tables", length = 1000)
    private String affectedTables; // Comma-separated list
    
    @Column(name = "affected_fields", length = 2000)
    private String affectedFields; // Comma-separated list
    
    @Column(name = "pii_exposed", length = 1000)
    private String piiExposed; // Comma-separated list of PII fields exposed
    
    @Column(name = "sensitive_data_hash", length = 256)
    private String sensitiveDataHash; // Hash of sensitive data for tracking
    
    @Column(name = "regulatory_impact", length = 500)
    private String regulatoryImpact; // Impact on regulatory compliance
    
    @Column(name = "compliance_status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ComplianceStatus complianceStatus = ComplianceStatus.REQUIRES_REVIEW;
    
    @Column(name = "remediation_required", length = 1)
    @Builder.Default
    private String remediationRequired = "N";
    
    @Column(name = "remediation_action", length = 1000)
    private String remediationAction;
    
    @Column(name = "remediation_date")
    private LocalDateTime remediationDate;
    
    // User and Session Information
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;
    
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "host_name", length = 100)
    private String hostName;
    
    @Column(name = "application_context", length = 500)
    private String applicationContext;
    
    @Column(name = "additional_metadata", columnDefinition = "CLOB")
    @Lob
    private String additionalMetadata; // JSON formatted additional data
    
    // Security Team Notification
    @Column(name = "reported_to_security", length = 1)
    @Builder.Default
    private String reportedToSecurity = "N";
    
    @Column(name = "security_team_notified", length = 1)
    @Builder.Default
    private String securityTeamNotified = "N";
    
    @Column(name = "incident_number", length = 50)
    private String incidentNumber;
    
    // Timestamps
    @Column(name = "audit_timestamp", nullable = false)
    private LocalDateTime auditTimestamp;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", referencedColumnName = "execution_id", insertable = false, updatable = false)
    private SqlLoaderExecutionEntity sqlLoaderExecution;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", referencedColumnName = "config_id", insertable = false, updatable = false)
    private SqlLoaderConfigEntity sqlLoaderConfig;
    
    // JPA lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (auditTimestamp == null) {
            auditTimestamp = LocalDateTime.now();
        }
        
        // Auto-determine some security properties based on event type and severity
        autoConfigureSecurityProperties();
    }
    
    // Enums
    public enum SecurityEventType {
        ACCESS_ATTEMPT,
        PRIVILEGE_ESCALATION,
        DATA_EXPOSURE,
        ENCRYPTION_EVENT,
        PII_ACCESS,
        COMPLIANCE_VIOLATION,
        AUTHENTICATION_FAILURE,
        AUTHORIZATION_FAILURE,
        SUSPICIOUS_ACTIVITY,
        REGULATORY_EVENT
    }
    
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    public enum ComplianceStatus {
        COMPLIANT,
        NON_COMPLIANT,
        REQUIRES_REVIEW,
        EXCEPTION_APPROVED,
        REMEDIATED
    }
    
    // Utility methods
    public boolean isRemediationRequired() {
        return "Y".equalsIgnoreCase(remediationRequired);
    }
    
    public boolean isReportedToSecurity() {
        return "Y".equalsIgnoreCase(reportedToSecurity);
    }
    
    public boolean isSecurityTeamNotified() {
        return "Y".equalsIgnoreCase(securityTeamNotified);
    }
    
    public boolean isCriticalSeverity() {
        return severity == Severity.CRITICAL;
    }
    
    public boolean isHighRisk() {
        return riskScore >= 75;
    }
    
    public boolean requiresImmediateAttention() {
        return isCriticalSeverity() || isHighRisk() || 
               securityEventType == SecurityEventType.PRIVILEGE_ESCALATION ||
               securityEventType == SecurityEventType.DATA_EXPOSURE ||
               complianceStatus == ComplianceStatus.NON_COMPLIANT;
    }
    
    public boolean hasPiiExposure() {
        return piiExposed != null && !piiExposed.trim().isEmpty();
    }
    
    public boolean hasRegulatoryImpact() {
        return regulatoryImpact != null && !regulatoryImpact.trim().isEmpty();
    }
    
    public boolean isRemediated() {
        return complianceStatus == ComplianceStatus.REMEDIATED ||
               complianceStatus == ComplianceStatus.EXCEPTION_APPROVED;
    }
    
    /**
     * Auto-configure security properties based on event type and context.
     */
    private void autoConfigureSecurityProperties() {
        // Set remediation required flag based on event type and severity
        if (securityEventType == SecurityEventType.DATA_EXPOSURE ||
            securityEventType == SecurityEventType.PRIVILEGE_ESCALATION ||
            securityEventType == SecurityEventType.COMPLIANCE_VIOLATION ||
            severity == Severity.CRITICAL || severity == Severity.HIGH) {
            remediationRequired = "Y";
        }
        
        // Auto-escalate high-risk events to security team
        if (severity == Severity.CRITICAL || riskScore >= 80) {
            reportedToSecurity = "Y";
        }
        
        // Set compliance status based on event type
        if (securityEventType == SecurityEventType.COMPLIANCE_VIOLATION) {
            complianceStatus = ComplianceStatus.NON_COMPLIANT;
        } else if (securityEventType == SecurityEventType.PII_ACCESS && hasPiiExposure()) {
            complianceStatus = ComplianceStatus.REQUIRES_REVIEW;
        }
        
        // Calculate risk score based on event type if not already set
        if (riskScore == 50) { // Default value
            riskScore = calculateRiskScore();
        }
    }
    
    /**
     * Calculate risk score based on event type, severity, and context.
     */
    private Integer calculateRiskScore() {
        int baseScore = 30;
        
        // Event type scoring
        switch (securityEventType) {
            case PRIVILEGE_ESCALATION:
                baseScore += 40;
                break;
            case DATA_EXPOSURE:
                baseScore += 35;
                break;
            case COMPLIANCE_VIOLATION:
                baseScore += 30;
                break;
            case PII_ACCESS:
                baseScore += 25;
                break;
            case AUTHENTICATION_FAILURE:
                baseScore += 20;
                break;
            case AUTHORIZATION_FAILURE:
                baseScore += 15;
                break;
            case SUSPICIOUS_ACTIVITY:
                baseScore += 20;
                break;
            case ENCRYPTION_EVENT:
                baseScore += 10;
                break;
            case REGULATORY_EVENT:
                baseScore += 25;
                break;
            case ACCESS_ATTEMPT:
            default:
                baseScore += 5;
                break;
        }
        
        // Severity scoring
        switch (severity) {
            case CRITICAL:
                baseScore += 30;
                break;
            case HIGH:
                baseScore += 20;
                break;
            case MEDIUM:
                baseScore += 10;
                break;
            case LOW:
                baseScore += 5;
                break;
        }
        
        // PII exposure increases risk
        if (hasPiiExposure()) {
            baseScore += 15;
        }
        
        // Regulatory impact increases risk
        if (hasRegulatoryImpact()) {
            baseScore += 10;
        }
        
        // Cap at 100
        return Math.min(baseScore, 100);
    }
    
    /**
     * Generate security alert summary for monitoring systems.
     */
    public String getSecurityAlertSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Security Event: ").append(securityEventType)
               .append(", Severity: ").append(severity)
               .append(", Risk Score: ").append(riskScore)
               .append(", User: ").append(userId);
        
        if (hasPiiExposure()) {
            summary.append(", PII Exposed: ").append(piiExposed);
        }
        
        if (hasRegulatoryImpact()) {
            summary.append(", Regulatory Impact: ").append(regulatoryImpact);
        }
        
        if (isRemediationRequired()) {
            summary.append(", Remediation Required");
        }
        
        return summary.toString();
    }
    
    /**
     * Mark security event as remediated.
     */
    public void markAsRemediated(String action, String remediatedBy) {
        this.complianceStatus = ComplianceStatus.REMEDIATED;
        this.remediationAction = action;
        this.remediationDate = LocalDateTime.now();
        this.remediationRequired = "N";
        
        // Update metadata with remediation info
        if (additionalMetadata == null) {
            additionalMetadata = "{}";
        }
        
        String remediationInfo = String.format(
            ",\"remediation\":{\"date\":\"%s\",\"by\":\"%s\",\"action\":\"%s\"}", 
            remediationDate, remediatedBy, action);
        
        if (additionalMetadata.endsWith("}")) {
            additionalMetadata = additionalMetadata.substring(0, additionalMetadata.length() - 1) + remediationInfo + "}";
        }
    }
    
    /**
     * Report to security team with incident number.
     */
    public void reportToSecurityTeam(String incidentNum) {
        this.reportedToSecurity = "Y";
        this.securityTeamNotified = "Y";
        this.incidentNumber = incidentNum;
        
        // Update metadata with security reporting info
        if (additionalMetadata == null) {
            additionalMetadata = "{}";
        }
        
        String reportingInfo = String.format(
            ",\"security_reporting\":{\"incident_number\":\"%s\",\"reported_date\":\"%s\"}", 
            incidentNum, LocalDateTime.now());
        
        if (additionalMetadata.endsWith("}")) {
            additionalMetadata = additionalMetadata.substring(0, additionalMetadata.length() - 1) + reportingInfo + "}";
        }
    }
    
    /**
     * Validate security audit entity for completeness and business rules.
     */
    public void validateSecurityAudit() {
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID must be specified");
        }
        
        if (securityEventType == null) {
            throw new IllegalArgumentException("Security event type must be specified");
        }
        
        if (eventDescription == null || eventDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Event description must be provided");
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID must be specified");
        }
        
        if (riskScore < 0 || riskScore > 100) {
            throw new IllegalArgumentException("Risk score must be between 0 and 100");
        }
        
        if (severity == null) {
            throw new IllegalArgumentException("Severity must be specified");
        }
        
        if (complianceStatus == null) {
            throw new IllegalArgumentException("Compliance status must be specified");
        }
        
        // Validate that PII exposure is tracked for relevant events
        if (securityEventType == SecurityEventType.PII_ACCESS && 
            (piiExposed == null || piiExposed.trim().isEmpty())) {
            throw new IllegalArgumentException("PII exposed fields must be documented for PII access events");
        }
        
        // Validate that remediation action is provided when required
        if (isRemediationRequired() && complianceStatus == ComplianceStatus.REMEDIATED &&
            (remediationAction == null || remediationAction.trim().isEmpty())) {
            throw new IllegalArgumentException("Remediation action must be provided for remediated events");
        }
    }
}