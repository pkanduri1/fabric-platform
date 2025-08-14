package com.truist.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.*;
import java.util.Map;
import java.util.List;

/**
 * Request DTO for Manual Job Execution operations.
 * 
 * Enterprise-grade execution request supporting secure job execution
 * with comprehensive parameter validation, security controls, and
 * audit trail requirements for banking applications.
 * 
 * Security Features:
 * - Runtime parameter validation
 * - Execution context security
 * - Audit trail correlation
 * - Resource limit controls
 * 
 * Banking Features:
 * - SOX compliance execution metadata
 * - Regulatory approval validation
 * - Risk assessment integration
 * - Performance monitoring setup
 * 
 * @author Senior Full Stack Developer Agent
 * @version 2.0
 * @since US001 Phase 2 - Job Execution API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Job Execution Request DTO with enterprise security and compliance controls")
public class JobExecutionRequest {

    @Schema(description = "Configuration ID to execute", 
            example = "cfg_core_banking_1691234567890_a1b2c3d4", 
            required = true)
    @NotBlank(message = "Configuration ID is required")
    @Size(min = 10, max = 50, message = "Configuration ID must be between 10 and 50 characters")
    @JsonProperty("configId")
    private String configId;

    @Schema(description = "Execution type", 
            example = "MANUAL", 
            required = true,
            allowableValues = {"MANUAL", "SCHEDULED", "TRIGGERED", "RETRY"})
    @NotBlank(message = "Execution type is required")
    @Pattern(regexp = "^(MANUAL|SCHEDULED|TRIGGERED|RETRY)$",
             message = "Execution type must be one of: MANUAL, SCHEDULED, TRIGGERED, RETRY")
    @JsonProperty("executionType")
    private String executionType = "MANUAL";

    @Schema(description = "Trigger source for audit trail", 
            example = "USER_INTERFACE", 
            required = true,
            allowableValues = {"USER_INTERFACE", "API_CALL", "SCHEDULER", "WEBHOOK", "ADMIN_CONSOLE"})
    @NotBlank(message = "Trigger source is required")
    @Pattern(regexp = "^(USER_INTERFACE|API_CALL|SCHEDULER|WEBHOOK|ADMIN_CONSOLE)$",
             message = "Trigger source must be one of: USER_INTERFACE, API_CALL, SCHEDULER, WEBHOOK, ADMIN_CONSOLE")
    @JsonProperty("triggerSource")
    private String triggerSource;

    @Schema(description = "Runtime execution parameters (override configuration defaults)", 
            example = "{\"batchSize\": 500, \"timeoutMinutes\": 60}")
    @JsonProperty("executionParameters")
    private Map<String, Object> executionParameters;

    @Schema(description = "Business justification for this execution", 
            example = "Emergency data sync required for regulatory reporting deadline", 
            required = true)
    @NotBlank(message = "Business justification is required for manual executions")
    @Size(min = 10, max = 1000, message = "Business justification must be between 10 and 1000 characters")
    @JsonProperty("businessJustification")
    private String businessJustification;

    @Schema(description = "Execution priority level", 
            example = "HIGH", 
            allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|CRITICAL)$",
             message = "Priority must be one of: LOW, MEDIUM, HIGH, CRITICAL")
    @JsonProperty("priority")
    private String priority = "MEDIUM";

    @Schema(description = "Maximum execution timeout in minutes", 
            example = "120")
    @Min(value = 1, message = "Timeout must be at least 1 minute")
    @Max(value = 2880, message = "Timeout cannot exceed 48 hours (2880 minutes)")
    @JsonProperty("timeoutMinutes")
    private Integer timeoutMinutes = 180; // Default 3 hours

    @Schema(description = "Notification email addresses for this execution", 
            example = "[\"admin@bank.com\", \"ops@bank.com\"]")
    @Size(max = 10, message = "Maximum 10 notification emails allowed")
    @JsonProperty("notificationEmails")
    private List<@Email(message = "Invalid email format") String> notificationEmails;

    @Schema(description = "Expected number of records to process", 
            example = "50000")
    @Min(value = 0, message = "Expected records cannot be negative")
    @Max(value = 10_000_000, message = "Expected records cannot exceed 10 million")
    @JsonProperty("expectedRecordCount")
    private Long expectedRecordCount;

    @Schema(description = "Environment context for execution", 
            example = "PRODUCTION",
            allowableValues = {"DEVELOPMENT", "TEST", "UAT", "STAGING", "PRODUCTION"})
    @NotBlank(message = "Environment is required")
    @Pattern(regexp = "^(DEVELOPMENT|TEST|UAT|STAGING|PRODUCTION)$",
             message = "Environment must be one of: DEVELOPMENT, TEST, UAT, STAGING, PRODUCTION")
    @JsonProperty("environment")
    private String environment;

    @Schema(description = "Correlation ID for distributed tracing", 
            example = "550e8400-e29b-41d4-a716-446655440000")
    @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$",
             message = "Correlation ID must be a valid UUID format")
    @JsonProperty("correlationId")
    private String correlationId;

    @Schema(description = "Retry configuration for this execution", 
            example = "{\"maxRetries\": 3, \"retryDelaySeconds\": 300, \"retryOnFailureTypes\": [\"TIMEOUT\", \"CONNECTION_ERROR\"]}")
    @JsonProperty("retryConfiguration")
    private Map<String, Object> retryConfiguration;

    @Schema(description = "Risk assessment level for this execution", 
            example = "MEDIUM", 
            allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|CRITICAL)$",
             message = "Risk assessment must be one of: LOW, MEDIUM, HIGH, CRITICAL")
    @JsonProperty("riskAssessment")
    private String riskAssessment = "MEDIUM";

    @Schema(description = "Approval ticket reference for high-risk executions", 
            example = "CHG-2024-001234")
    @Size(max = 50, message = "Ticket reference cannot exceed 50 characters")
    @JsonProperty("approvalTicketReference")
    private String approvalTicketReference;

    @Schema(description = "Resource limits for execution", 
            example = "{\"maxMemoryMB\": 2048, \"maxCpuCores\": 4, \"maxDiskSpaceMB\": 10240}")
    @JsonProperty("resourceLimits")
    private Map<String, Object> resourceLimits;

    @Schema(description = "Monitoring thresholds for this execution", 
            example = "{\"errorRateThreshold\": 5.0, \"performanceThreshold\": 1000}")
    @JsonProperty("monitoringThresholds")
    private Map<String, Object> monitoringThresholds;

    @Schema(description = "Data lineage tracking enabled", 
            example = "true")
    @JsonProperty("enableDataLineage")
    private Boolean enableDataLineage = true;

    @Schema(description = "Compliance tags for regulatory tracking", 
            example = "[\"SOX\", \"PCI_DSS\"]")
    @Size(max = 10, message = "Maximum 10 compliance tags allowed")
    @JsonProperty("complianceTags")
    private List<@Pattern(regexp = "^[A-Z_]+$", message = "Compliance tags must be uppercase with underscores") String> complianceTags;

    @Schema(description = "Dry run mode - validate execution without running", 
            example = "false")
    @JsonProperty("dryRun")
    private Boolean dryRun = false;

    /**
     * Validates that critical execution requirements are met.
     * 
     * @return true if execution request is valid
     */
    public boolean isValidExecutionRequest() {
        return configId != null && !configId.trim().isEmpty() &&
               businessJustification != null && !businessJustification.trim().isEmpty() &&
               environment != null && !environment.trim().isEmpty() &&
               (timeoutMinutes == null || timeoutMinutes > 0);
    }

    /**
     * Determines if this execution requires elevated approval.
     * 
     * @return true if elevated approval is required
     */
    public boolean requiresElevatedApproval() {
        return "CRITICAL".equals(priority) ||
               "CRITICAL".equals(riskAssessment) ||
               "PRODUCTION".equals(environment) && "CRITICAL".equals(priority);
    }

    /**
     * Gets the effective timeout with fallback to default.
     * 
     * @return effective timeout in minutes
     */
    public Integer getEffectiveTimeoutMinutes() {
        return timeoutMinutes != null ? timeoutMinutes : 180;
    }

    /**
     * Checks if execution parameters contain sensitive data.
     * 
     * @return true if sensitive data is present
     */
    public boolean containsSensitiveExecutionParameters() {
        if (executionParameters == null) {
            return false;
        }
        
        return executionParameters.keySet().stream()
                .anyMatch(key -> key.toLowerCase().contains("password") ||
                                key.toLowerCase().contains("secret") ||
                                key.toLowerCase().contains("key") ||
                                key.toLowerCase().contains("token"));
    }

    /**
     * Gets the effective priority with business logic.
     * 
     * @return effective priority level
     */
    public String getEffectivePriority() {
        if ("PRODUCTION".equals(environment) && ("HIGH".equals(priority) || "CRITICAL".equals(priority))) {
            return priority;
        }
        return priority != null ? priority : "MEDIUM";
    }
}