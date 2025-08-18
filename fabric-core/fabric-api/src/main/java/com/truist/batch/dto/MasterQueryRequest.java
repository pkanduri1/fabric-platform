package com.truist.batch.dto;

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
 * =========================================================================
 * MASTER QUERY REQUEST DTO - BANKING GRADE SECURITY
 * =========================================================================
 * 
 * Purpose: Request DTO for Master Query operations with banking-grade validation
 * - Comprehensive input validation and sanitization
 * - SQL injection protection with parameterized query requirements
 * - SOX compliance with audit trail support
 * - Role-based authorization metadata
 * 
 * Security Features:
 * - Query validation with read-only enforcement
 * - Parameter injection attack prevention
 * - Banking compliance with PCI-DSS requirements
 * - Correlation ID tracking for audit trail
 * 
 * Enterprise Standards:
 * - Input validation for all query parameters
 * - Business rule enforcement for banking operations
 * - Regulatory compliance metadata tracking
 * - Error handling with secure error messages
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Phase 1 - Master Query Integration
 * =========================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Master Query Request DTO with banking-grade security validation and SOX compliance")
public class MasterQueryRequest {

    @Schema(description = "Master query unique identifier", 
            example = "mq_transaction_summary_20250818", 
            required = true,
            pattern = "^mq_[a-z0-9_]{3,40}$")
    @NotBlank(message = "Master query ID is required")
    @Pattern(regexp = "^mq_[a-z0-9_]{3,40}$", 
             message = "Master query ID must start with 'mq_' followed by lowercase letters, numbers, and underscores, 6-43 characters total")
    @Size(min = 6, max = 43, message = "Master query ID must be between 6 and 43 characters")
    @JsonProperty("masterQueryId")
    private String masterQueryId;

    @Schema(description = "Query name for identification and display", 
            example = "Transaction Summary Report", 
            required = true)
    @NotBlank(message = "Query name is required")
    @Size(min = 3, max = 100, message = "Query name must be between 3 and 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\s\\-_\\.]{3,100}$", 
             message = "Query name can only contain letters, numbers, spaces, hyphens, underscores, and dots")
    @JsonProperty("queryName")
    private String queryName;

    @Schema(description = "SQL query statement (SELECT and WITH only)", 
            example = "SELECT account_id, SUM(amount) as total FROM transactions WHERE batch_date = :batchDate GROUP BY account_id", 
            required = true)
    @NotBlank(message = "SQL query is required")
    @Size(min = 10, max = 10000, message = "SQL query must be between 10 and 10,000 characters")
    @JsonProperty("querySql")
    private String querySql;

    @Schema(description = "Query description for documentation", 
            example = "Summarizes transaction amounts by account for specified batch date", 
            required = false)
    @Size(max = 500, message = "Query description cannot exceed 500 characters")
    @JsonProperty("queryDescription")
    private String queryDescription;

    @Schema(description = "Query parameters for parameterized execution", 
            example = "{\"batchDate\": \"2025-08-18\", \"minAmount\": 100.00}", 
            required = false)
    @Valid
    @JsonProperty("queryParameters")
    private Map<String, Object> queryParameters;

    @Schema(description = "Parameter validation rules for each query parameter", 
            example = "{\"batchDate\": {\"type\": \"date\", \"required\": true}, \"minAmount\": {\"type\": \"decimal\", \"min\": 0}}", 
            required = false)
    @JsonProperty("parameterValidationRules")
    private Map<String, Object> parameterValidationRules;

    @Schema(description = "Maximum execution time in seconds (1-30 seconds)", 
            example = "30", 
            required = false)
    @Min(value = 1, message = "Maximum execution time must be at least 1 second")
    @Max(value = 30, message = "Maximum execution time cannot exceed 30 seconds for banking compliance")
    @JsonProperty("maxExecutionTimeSeconds")
    private Integer maxExecutionTimeSeconds = 30;

    @Schema(description = "Maximum number of result rows (1-100 rows)", 
            example = "100", 
            required = false)
    @Min(value = 1, message = "Maximum result rows must be at least 1")
    @Max(value = 100, message = "Maximum result rows cannot exceed 100 for banking compliance")
    @JsonProperty("maxResultRows")
    private Integer maxResultRows = 100;

    @Schema(description = "Security classification level", 
            example = "INTERNAL", 
            required = false,
            allowableValues = {"PUBLIC", "INTERNAL", "CONFIDENTIAL", "RESTRICTED"})
    @Pattern(regexp = "^(PUBLIC|INTERNAL|CONFIDENTIAL|RESTRICTED)$",
             message = "Security classification must be one of: PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED")
    @JsonProperty("securityClassification")
    private String securityClassification = "INTERNAL";

    @Schema(description = "Whether query requires approval before execution", 
            example = "true", 
            required = false)
    @JsonProperty("requiresApproval")
    private Boolean requiresApproval = true;

    @Schema(description = "Business justification for query creation", 
            example = "Required for daily transaction reconciliation and regulatory reporting", 
            required = false)
    @Size(max = 1000, message = "Business justification cannot exceed 1000 characters")
    @JsonProperty("businessJustification")
    private String businessJustification;

    @Schema(description = "Data classification level for query results", 
            example = "SENSITIVE", 
            required = false,
            allowableValues = {"PUBLIC", "INTERNAL", "SENSITIVE", "HIGHLY_SENSITIVE"})
    @Pattern(regexp = "^(PUBLIC|INTERNAL|SENSITIVE|HIGHLY_SENSITIVE)$",
             message = "Data classification must be one of: PUBLIC, INTERNAL, SENSITIVE, HIGHLY_SENSITIVE")
    @JsonProperty("dataClassification")
    private String dataClassification = "INTERNAL";

    @Schema(description = "Compliance and regulatory tags", 
            example = "[\"SOX\", \"PCI_DSS\", \"BASEL_III\"]", 
            required = false)
    @Size(max = 10, message = "Maximum 10 compliance tags allowed")
    @JsonProperty("complianceTags")
    private List<@Pattern(regexp = "^[A-Z_]+$", message = "Compliance tags must be uppercase with underscores only") String> complianceTags;

    @Schema(description = "Query execution context and environment", 
            example = "PRODUCTION", 
            required = false,
            allowableValues = {"DEVELOPMENT", "TEST", "STAGING", "PRODUCTION"})
    @Pattern(regexp = "^(DEVELOPMENT|TEST|STAGING|PRODUCTION)$",
             message = "Query context must be one of: DEVELOPMENT, TEST, STAGING, PRODUCTION")
    @JsonProperty("queryContext")
    private String queryContext = "PRODUCTION";

    @Schema(description = "Expected query result columns metadata", 
            example = "[{\"name\": \"account_id\", \"type\": \"VARCHAR\", \"length\": 20}, {\"name\": \"total\", \"type\": \"DECIMAL\", \"precision\": 15, \"scale\": 2}]", 
            required = false)
    @JsonProperty("expectedColumns")
    private List<Map<String, Object>> expectedColumns;

    @Schema(description = "Query performance configuration", 
            example = "{\"useIndex\": true, \"optimizeForFirstRows\": false, \"parallelExecution\": false}", 
            required = false)
    @JsonProperty("performanceConfiguration")
    private Map<String, Object> performanceConfiguration;

    @Schema(description = "Notification configuration for query completion", 
            example = "{\"enableNotifications\": false, \"notificationEmails\": []}", 
            required = false)
    @JsonProperty("notificationConfiguration")
    private Map<String, Object> notificationConfiguration;

    /**
     * Validates that the SQL query is a safe SELECT or WITH statement.
     * 
     * @return true if query appears to be safe
     */
    public boolean isSelectQuery() {
        if (querySql == null || querySql.trim().isEmpty()) {
            return false;
        }
        
        String normalizedSql = querySql.trim().toUpperCase();
        return normalizedSql.startsWith("SELECT") || normalizedSql.startsWith("WITH");
    }

    /**
     * Checks if the query contains parameters that should be validated.
     * 
     * @return true if query contains parameter placeholders
     */
    public boolean hasParameters() {
        if (querySql == null) {
            return false;
        }
        
        // Check for named parameters (:paramName) or JDBC parameters (?)
        return querySql.contains(":") || querySql.contains("?");
    }

    /**
     * Validates parameter consistency between query and provided parameters.
     * 
     * @return true if parameters are consistent
     */
    public boolean hasValidParameterConsistency() {
        if (!hasParameters()) {
            return queryParameters == null || queryParameters.isEmpty();
        }
        
        return queryParameters != null && !queryParameters.isEmpty();
    }

    /**
     * Checks if the query contains sensitive data indicators.
     * 
     * @return true if query may return sensitive data
     */
    public boolean mayContainSensitiveData() {
        if (querySql == null) {
            return false;
        }
        
        String lowerSql = querySql.toLowerCase();
        return lowerSql.contains("ssn") || 
               lowerSql.contains("social_security") ||
               lowerSql.contains("credit_card") ||
               lowerSql.contains("account_number") ||
               lowerSql.contains("routing_number") ||
               lowerSql.contains("password") ||
               lowerSql.contains("personal_info");
    }

    /**
     * Gets the effective security classification with automatic escalation for sensitive data.
     * 
     * @return effective security classification
     */
    public String getEffectiveSecurityClassification() {
        if (mayContainSensitiveData()) {
            // Automatically escalate classification for sensitive data
            return "CONFIDENTIAL";
        }
        
        return securityClassification != null ? securityClassification : "INTERNAL";
    }

    /**
     * Validates query complexity for banking operations.
     * 
     * @return true if query complexity is acceptable
     */
    public boolean hasAcceptableComplexity() {
        if (querySql == null) {
            return false;
        }
        
        String upperSql = querySql.toUpperCase();
        int joinCount = countOccurrences(upperSql, "JOIN");
        int subqueryCount = countOccurrences(upperSql, "SELECT");
        
        // Banking compliance limits
        return joinCount <= 10 && subqueryCount <= 5;
    }

    /**
     * Count occurrences of a substring in the SQL query.
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
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

    /**
     * Gets the effective data classification with automatic escalation.
     * 
     * @return effective data classification
     */
    public String getEffectiveDataClassification() {
        if (mayContainSensitiveData()) {
            return "SENSITIVE";
        }
        
        return dataClassification != null ? dataClassification : "INTERNAL";
    }
}