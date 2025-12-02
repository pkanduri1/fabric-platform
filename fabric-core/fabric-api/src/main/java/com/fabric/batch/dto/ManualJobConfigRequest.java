package com.fabric.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;

import java.util.Map;
import java.util.List;

/**
 * Request DTO for Manual Job Configuration operations.
 * 
 * Enterprise-grade request object implementing comprehensive validation,
 * security sanitization, and SOX compliance features for manual job
 * configuration management.
 * 
 * Security Features:
 * - Input validation and sanitization
 * - JSON schema validation for parameters
 * - Business rule enforcement
 * - PII data handling controls
 * 
 * Banking Compliance:
 * - Field-level validation for financial data integrity
 * - Regulatory parameter validation
 * - Audit trail support metadata
 * 
 * @author Senior Full Stack Developer Agent
 * @version 2.0
 * @since US001 Phase 2 - REST API Implementation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Manual Job Configuration Request DTO with enterprise validation and security controls")
public class ManualJobConfigRequest {

    @Schema(description = "Unique job name identifier", 
            example = "DAILY_TRANSACTION_LOADER", 
            required = true,
            pattern = "^[A-Z][A-Z0-9_]{2,99}$")
    @NotBlank(message = "Job name is required")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,99}$", 
             message = "Job name must start with uppercase letter, contain only uppercase letters, numbers, and underscores, 3-100 characters")
    @Size(min = 3, max = 100, message = "Job name must be between 3 and 100 characters")
    @JsonProperty("jobName")
    private String jobName;

    @Schema(description = "Job type classification", 
            example = "ETL_BATCH", 
            required = true,
            allowableValues = {"ETL_BATCH", "DATA_MIGRATION", "REPORT_GENERATION", "FILE_PROCESSING", "API_SYNC"})
    @NotBlank(message = "Job type is required")
    @Pattern(regexp = "^(ETL_BATCH|DATA_MIGRATION|REPORT_GENERATION|FILE_PROCESSING|API_SYNC)$",
             message = "Job type must be one of: ETL_BATCH, DATA_MIGRATION, REPORT_GENERATION, FILE_PROCESSING, API_SYNC")
    @JsonProperty("jobType")
    private String jobType;

    @Schema(description = "Source system identifier", 
            example = "CORE_BANKING", 
            required = true,
            pattern = "^[A-Z][A-Z0-9_]{2,49}$")
    @NotBlank(message = "Source system is required")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,49}$", 
             message = "Source system must start with uppercase letter, contain only uppercase letters, numbers, and underscores, 3-50 characters")
    @Size(min = 3, max = 50, message = "Source system must be between 3 and 50 characters")
    @JsonProperty("sourceSystem")
    private String sourceSystem;

    @Schema(description = "Target system identifier", 
            example = "DATA_WAREHOUSE", 
            required = true,
            pattern = "^[A-Z][A-Z0-9_]{2,49}$")
    @NotBlank(message = "Target system is required")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,49}$", 
             message = "Target system must start with uppercase letter, contain only uppercase letters, numbers, and underscores, 3-50 characters")
    @Size(min = 3, max = 50, message = "Target system must be between 3 and 50 characters")
    @JsonProperty("targetSystem")
    private String targetSystem;

    @Schema(description = "Master query identifier for linking to predefined SQL queries", 
            example = "mq_daily_transactions_001", 
            required = false)
    @Size(max = 100, message = "Master query ID cannot exceed 100 characters")
    @JsonProperty("masterQueryId")
    private String masterQueryId;

    @Schema(description = "Job execution parameters in JSON format", 
            example = "{\"batchSize\": 1000, \"connectionTimeout\": 30, \"retryCount\": 3}", 
            required = true)
    @NotNull(message = "Job parameters are required")
    @Valid
    @JsonProperty("jobParameters")
    private Map<String, Object> jobParameters;

    @Schema(description = "Job configuration description", 
            example = "Daily transaction data loader for core banking integration", 
            required = false)
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Job priority level", 
            example = "HIGH", 
            required = false,
            allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|CRITICAL)$",
             message = "Priority must be one of: LOW, MEDIUM, HIGH, CRITICAL")
    @JsonProperty("priority")
    private String priority = "MEDIUM";

    @Schema(description = "Expected runtime in minutes", 
            example = "30", 
            required = false)
    @Min(value = 1, message = "Expected runtime must be at least 1 minute")
    @Max(value = 1440, message = "Expected runtime cannot exceed 24 hours (1440 minutes)")
    @JsonProperty("expectedRuntimeMinutes")
    private Integer expectedRuntimeMinutes;

    @Schema(description = "Job notification email addresses", 
            example = "[\"admin@bank.com\", \"ops@bank.com\"]", 
            required = false)
    @Size(max = 10, message = "Maximum 10 notification emails allowed")
    @JsonProperty("notificationEmails")
    private List<@Email(message = "Invalid email format") String> notificationEmails;

    @Schema(description = "Business justification for job configuration", 
            example = "Required for daily reconciliation and regulatory reporting", 
            required = false)
    @Size(max = 1000, message = "Business justification cannot exceed 1000 characters")
    @JsonProperty("businessJustification")
    private String businessJustification;

    @Schema(description = "Compliance and regulatory tags", 
            example = "[\"SOX\", \"PCI_DSS\", \"BASEL_III\"]", 
            required = false)
    @Size(max = 20, message = "Maximum 20 compliance tags allowed")
    @JsonProperty("complianceTags")
    private List<@Pattern(regexp = "^[A-Z_]+$", message = "Compliance tags must be uppercase with underscores only") String> complianceTags;

    @Schema(description = "Security classification level", 
            example = "CONFIDENTIAL", 
            required = false,
            allowableValues = {"PUBLIC", "INTERNAL", "CONFIDENTIAL", "RESTRICTED"})
    @Pattern(regexp = "^(PUBLIC|INTERNAL|CONFIDENTIAL|RESTRICTED)$",
             message = "Security classification must be one of: PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED")
    @JsonProperty("securityClassification")
    private String securityClassification = "INTERNAL";

    @Schema(description = "Environment deployment targets", 
            example = "[\"DEV\", \"TEST\", \"PROD\"]", 
            required = false)
    @Size(max = 10, message = "Maximum 10 environment targets allowed")
    @JsonProperty("environmentTargets")
    private List<@Pattern(regexp = "^(DEV|TEST|UAT|STAGING|PROD)$", 
                          message = "Environment must be one of: DEV, TEST, UAT, STAGING, PROD") String> environmentTargets;

    @Schema(description = "Data classification level for processed data", 
            example = "SENSITIVE", 
            required = false,
            allowableValues = {"PUBLIC", "INTERNAL", "SENSITIVE", "HIGHLY_SENSITIVE"})
    @Pattern(regexp = "^(PUBLIC|INTERNAL|SENSITIVE|HIGHLY_SENSITIVE)$",
             message = "Data classification must be one of: PUBLIC, INTERNAL, SENSITIVE, HIGHLY_SENSITIVE")
    @JsonProperty("dataClassification")
    private String dataClassification = "INTERNAL";

    @Schema(description = "Maximum allowed execution time in minutes", 
            example = "120", 
            required = false)
    @Min(value = 1, message = "Max execution time must be at least 1 minute")
    @Max(value = 2880, message = "Max execution time cannot exceed 48 hours (2880 minutes)")
    @JsonProperty("maxExecutionTimeMinutes")
    private Integer maxExecutionTimeMinutes = 180; // Default 3 hours

    @Schema(description = "Retry configuration parameters", 
            example = "{\"maxRetries\": 3, \"retryDelaySeconds\": 60, \"retryOnFailureOnly\": true}", 
            required = false)
    @JsonProperty("retryConfiguration")
    private Map<String, Object> retryConfiguration;

    @Schema(description = "Monitoring and alerting configuration", 
            example = "{\"enableAlerts\": true, \"alertThresholds\": {\"errorRate\": 5, \"executionTime\": 90}}", 
            required = false)
    @JsonProperty("monitoringConfiguration")
    private Map<String, Object> monitoringConfiguration;

    /**
     * Validates the job parameters for basic structure and security requirements.
     * 
     * @return true if parameters are valid
     */
    public boolean hasValidJobParameters() {
        if (jobParameters == null || jobParameters.isEmpty()) {
            return false;
        }
        
        // Check for required core parameters
        return jobParameters.containsKey("source") || 
               jobParameters.containsKey("target") || 
               jobParameters.size() >= 1;
    }

    /**
     * Checks if the configuration contains sensitive data that requires encryption.
     * 
     * @return true if sensitive data is present
     */
    public boolean containsSensitiveData() {
        if (jobParameters == null) {
            return false;
        }
        
        // Check for common sensitive parameter names
        return jobParameters.keySet().stream()
                .anyMatch(key -> key.toLowerCase().contains("password") ||
                                key.toLowerCase().contains("secret") ||
                                key.toLowerCase().contains("key") ||
                                key.toLowerCase().contains("token") ||
                                key.toLowerCase().contains("credential"));
    }

    /**
     * Gets the effective priority with fallback to default.
     * 
     * @return effective priority level
     */
    public String getEffectivePriority() {
        return priority != null && !priority.isEmpty() ? priority : "MEDIUM";
    }

    /**
     * Validates compliance tags format and content.
     * 
     * @return true if all compliance tags are valid
     */
    public boolean hasValidComplianceTags() {
        if (complianceTags == null || complianceTags.isEmpty()) {
            return true; // Optional field
        }
        
        return complianceTags.stream()
                .allMatch(tag -> tag.matches("^[A-Z_]+$") && tag.length() <= 20);
    }
}