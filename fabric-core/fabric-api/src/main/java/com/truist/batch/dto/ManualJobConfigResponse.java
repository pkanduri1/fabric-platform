package com.truist.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

/**
 * Response DTO for Manual Job Configuration operations.
 * 
 * Enterprise-grade response object providing comprehensive job configuration
 * details with security-filtered sensitive data, audit information, and
 * monitoring metadata for banking-grade applications.
 * 
 * Security Features:
 * - Sensitive parameter masking
 * - Role-based data filtering
 * - Audit trail information
 * - Compliance metadata
 * 
 * Banking Features:
 * - SOX compliance audit fields
 * - Regulatory metadata support
 * - Data lineage tracking
 * - Performance monitoring data
 * 
 * @author Senior Full Stack Developer Agent
 * @version 2.0
 * @since US001 Phase 2 - REST API Implementation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Manual Job Configuration Response DTO with comprehensive audit and monitoring metadata")
public class ManualJobConfigResponse {

    @Schema(description = "Unique configuration identifier", 
            example = "cfg_core_banking_1691234567890_a1b2c3d4", 
            required = true)
    @JsonProperty("configId")
    private String configId;

    @Schema(description = "Unique job name identifier", 
            example = "DAILY_TRANSACTION_LOADER", 
            required = true)
    @JsonProperty("jobName")
    private String jobName;

    @Schema(description = "Job type classification", 
            example = "ETL_BATCH", 
            required = true)
    @JsonProperty("jobType")
    private String jobType;

    @Schema(description = "Source system identifier", 
            example = "CORE_BANKING", 
            required = true)
    @JsonProperty("sourceSystem")
    private String sourceSystem;

    @Schema(description = "Target system identifier", 
            example = "DATA_WAREHOUSE", 
            required = true)
    @JsonProperty("targetSystem")
    private String targetSystem;

    @Schema(description = "Master query identifier for linked SQL queries", 
            example = "mq_daily_transactions_001")
    @JsonProperty("masterQueryId")
    private String masterQueryId;

    @Schema(description = "Job execution parameters (sensitive values masked)", 
            example = "{\"batchSize\": 1000, \"connectionTimeout\": 30, \"password\": \"***MASKED***\"}", 
            required = true)
    @JsonProperty("jobParameters")
    private Map<String, Object> jobParameters;

    @Schema(description = "Current configuration status", 
            example = "ACTIVE", 
            allowableValues = {"ACTIVE", "INACTIVE", "DEPRECATED", "PENDING_APPROVAL"})
    @JsonProperty("status")
    private String status;

    @Schema(description = "Job configuration description", 
            example = "Daily transaction data loader for core banking integration")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Job priority level", 
            example = "HIGH", 
            allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
    @JsonProperty("priority")
    private String priority;

    @Schema(description = "Expected runtime in minutes", 
            example = "30")
    @JsonProperty("expectedRuntimeMinutes")
    private Integer expectedRuntimeMinutes;

    @Schema(description = "Job notification email addresses", 
            example = "[\"admin@bank.com\", \"ops@bank.com\"]")
    @JsonProperty("notificationEmails")
    private List<String> notificationEmails;

    @Schema(description = "Business justification for job configuration", 
            example = "Required for daily reconciliation and regulatory reporting")
    @JsonProperty("businessJustification")
    private String businessJustification;

    @Schema(description = "Compliance and regulatory tags", 
            example = "[\"SOX\", \"PCI_DSS\", \"BASEL_III\"]")
    @JsonProperty("complianceTags")
    private List<String> complianceTags;

    @Schema(description = "Security classification level", 
            example = "CONFIDENTIAL", 
            allowableValues = {"PUBLIC", "INTERNAL", "CONFIDENTIAL", "RESTRICTED"})
    @JsonProperty("securityClassification")
    private String securityClassification;

    @Schema(description = "Environment deployment targets", 
            example = "[\"DEV\", \"TEST\", \"PROD\"]")
    @JsonProperty("environmentTargets")
    private List<String> environmentTargets;

    @Schema(description = "Data classification level for processed data", 
            example = "SENSITIVE", 
            allowableValues = {"PUBLIC", "INTERNAL", "SENSITIVE", "HIGHLY_SENSITIVE"})
    @JsonProperty("dataClassification")
    private String dataClassification;

    @Schema(description = "Maximum allowed execution time in minutes", 
            example = "120")
    @JsonProperty("maxExecutionTimeMinutes")
    private Integer maxExecutionTimeMinutes;

    @Schema(description = "Retry configuration parameters", 
            example = "{\"maxRetries\": 3, \"retryDelaySeconds\": 60}")
    @JsonProperty("retryConfiguration")
    private Map<String, Object> retryConfiguration;

    @Schema(description = "Monitoring and alerting configuration", 
            example = "{\"enableAlerts\": true, \"alertThresholds\": {\"errorRate\": 5}}")
    @JsonProperty("monitoringConfiguration")
    private Map<String, Object> monitoringConfiguration;

    // Audit and Versioning Information

    @Schema(description = "User who created the configuration", 
            example = "john.doe@bank.com")
    @JsonProperty("createdBy")
    private String createdBy;

    @Schema(description = "Configuration creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @JsonProperty("createdDate")
    private LocalDateTime createdDate;

    @Schema(description = "User who last updated the configuration", 
            example = "jane.smith@bank.com")
    @JsonProperty("updatedBy")
    private String updatedBy;

    @Schema(description = "Configuration last update timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @JsonProperty("updatedDate")
    private LocalDateTime updatedDate;

    @Schema(description = "Current configuration version number", 
            example = "5")
    @JsonProperty("versionNumber")
    private Long versionNumber;

    // Execution Statistics and Monitoring

    @Schema(description = "Total number of successful executions", 
            example = "42")
    @JsonProperty("successfulExecutions")
    private Long successfulExecutions;

    @Schema(description = "Total number of failed executions", 
            example = "3")
    @JsonProperty("failedExecutions")
    private Long failedExecutions;

    @Schema(description = "Last successful execution timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @JsonProperty("lastSuccessfulExecution")
    private LocalDateTime lastSuccessfulExecution;

    @Schema(description = "Last execution status", 
            example = "COMPLETED", 
            allowableValues = {"COMPLETED", "FAILED", "RUNNING", "CANCELLED", "NEVER_EXECUTED"})
    @JsonProperty("lastExecutionStatus")
    private String lastExecutionStatus;

    @Schema(description = "Average execution duration in seconds", 
            example = "1847.5")
    @JsonProperty("averageExecutionDurationSeconds")
    private Double averageExecutionDurationSeconds;

    @Schema(description = "Current execution success rate as percentage", 
            example = "93.33")
    @JsonProperty("successRate")
    private Double successRate;

    // Security and Compliance Metadata

    @Schema(description = "Indicates if configuration contains encrypted parameters", 
            example = "true")
    @JsonProperty("hasEncryptedParameters")
    private Boolean hasEncryptedParameters;

    @Schema(description = "Last SOX compliance audit date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @JsonProperty("lastComplianceAudit")
    private LocalDateTime lastComplianceAudit;

    @Schema(description = "Compliance audit status", 
            example = "COMPLIANT", 
            allowableValues = {"COMPLIANT", "NON_COMPLIANT", "PENDING_REVIEW", "EXEMPT"})
    @JsonProperty("complianceStatus")
    private String complianceStatus;

    @Schema(description = "Next scheduled compliance review date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @JsonProperty("nextComplianceReview")
    private LocalDateTime nextComplianceReview;

    // Risk Assessment and Approval Workflow

    @Schema(description = "Risk assessment level", 
            example = "MEDIUM", 
            allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
    @JsonProperty("riskAssessment")
    private String riskAssessment;

    @Schema(description = "Current approval workflow status", 
            example = "APPROVED", 
            allowableValues = {"DRAFT", "PENDING_APPROVAL", "APPROVED", "REJECTED", "EXPIRED"})
    @JsonProperty("approvalStatus")
    private String approvalStatus;

    @Schema(description = "Approver user identifier", 
            example = "manager@bank.com")
    @JsonProperty("approvedBy")
    private String approvedBy;

    @Schema(description = "Approval timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @JsonProperty("approvedDate")
    private LocalDateTime approvedDate;

    @Schema(description = "Approval comments or notes", 
            example = "Approved for production deployment with monitoring requirements")
    @JsonProperty("approvalComments")
    private String approvalComments;

    // System Metadata

    @Schema(description = "Configuration deployment environment", 
            example = "PRODUCTION")
    @JsonProperty("deploymentEnvironment")
    private String deploymentEnvironment;

    @Schema(description = "Application version that created this configuration", 
            example = "2.1.0")
    @JsonProperty("applicationVersion")
    private String applicationVersion;

    @Schema(description = "Unique correlation ID for distributed tracing", 
            example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("correlationId")
    private String correlationId;

    @Schema(description = "Configuration checksum for integrity validation", 
            example = "sha256:a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3")
    @JsonProperty("configurationChecksum")
    private String configurationChecksum;

    @Schema(description = "Data retention policy for this configuration", 
            example = "7_YEARS")
    @JsonProperty("dataRetentionPolicy")
    private String dataRetentionPolicy;

    @Schema(description = "External system integration metadata")
    @JsonProperty("integrationMetadata")
    private Map<String, Object> integrationMetadata;

    /**
     * Gets the success rate as a percentage with proper handling of division by zero.
     * 
     * @return success rate percentage or 0 if no executions
     */
    public Double getCalculatedSuccessRate() {
        if (successfulExecutions == null || failedExecutions == null) {
            return 0.0;
        }
        
        long totalExecutions = successfulExecutions + failedExecutions;
        if (totalExecutions == 0) {
            return 0.0;
        }
        
        return (successfulExecutions.doubleValue() / totalExecutions) * 100.0;
    }

    /**
     * Determines if the configuration requires elevated security handling.
     * 
     * @return true if configuration requires special security handling
     */
    public boolean requiresElevatedSecurity() {
        return "RESTRICTED".equals(securityClassification) || 
               "HIGHLY_SENSITIVE".equals(dataClassification) ||
               (hasEncryptedParameters != null && hasEncryptedParameters);
    }

    /**
     * Checks if the configuration is production-ready.
     * 
     * @return true if configuration meets production deployment criteria
     */
    public boolean isProductionReady() {
        return "ACTIVE".equals(status) && 
               "APPROVED".equals(approvalStatus) &&
               "COMPLIANT".equals(complianceStatus) &&
               versionNumber != null && versionNumber > 0;
    }

    /**
     * Gets the risk level indicator for monitoring dashboards.
     * 
     * @return numeric risk level (1-4, higher is riskier)
     */
    public int getRiskLevelIndicator() {
        if (riskAssessment == null) {
            return 2; // Default medium risk
        }
        
        return switch (riskAssessment) {
            case "LOW" -> 1;
            case "MEDIUM" -> 2;
            case "HIGH" -> 3;
            case "CRITICAL" -> 4;
            default -> 2;
        };
    }
}