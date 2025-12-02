package com.fabric.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * =========================================================================
 * MASTER QUERY RESPONSE DTO - BANKING GRADE SECURITY
 * =========================================================================
 * 
 * Purpose: Response DTO for Master Query operations with banking-grade security
 * - Comprehensive query execution results with metadata
 * - SOX-compliant audit trail information
 * - Security classification and data protection
 * - Performance metrics and execution statistics
 * 
 * Security Features:
 * - Secure error messaging without exposing sensitive information
 * - Data classification enforcement in responses
 * - Correlation ID tracking for audit compliance
 * - Result sanitization for sensitive data protection
 * 
 * Enterprise Standards:
 * - Complete execution metadata for monitoring
 * - Regulatory compliance information
 * - Performance metrics for optimization
 * - Error handling with secure error details
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
@Schema(description = "Master Query Response DTO with banking-grade security and SOX compliance")
public class MasterQueryResponse {

    @Schema(description = "Master query unique identifier", 
            example = "mq_transaction_summary_20250818")
    @JsonProperty("masterQueryId")
    private String masterQueryId;

    @Schema(description = "Query execution status", 
            example = "SUCCESS",
            allowableValues = {"SUCCESS", "FAILED", "TIMEOUT", "CANCELLED", "VALIDATION_FAILED"})
    @JsonProperty("executionStatus")
    private String executionStatus;

    @Schema(description = "Query name for identification", 
            example = "Transaction Summary Report")
    @JsonProperty("queryName")
    private String queryName;

    @Schema(description = "Query execution results", 
            example = "[{\"account_id\": \"ACC123\", \"total\": 1500.00}, {\"account_id\": \"ACC456\", \"total\": 2300.50}]")
    @JsonProperty("results")
    private List<Map<String, Object>> results;

    @Schema(description = "Result column metadata", 
            example = "[{\"name\": \"account_id\", \"type\": \"VARCHAR\", \"length\": 20}, {\"name\": \"total\", \"type\": \"DECIMAL\", \"precision\": 15, \"scale\": 2}]")
    @JsonProperty("columnMetadata")
    private List<Map<String, Object>> columnMetadata;

    @Schema(description = "Number of rows returned", 
            example = "25")
    @JsonProperty("rowCount")
    private Integer rowCount;

    @Schema(description = "Query execution time in milliseconds", 
            example = "1250")
    @JsonProperty("executionTimeMs")
    private Long executionTimeMs;

    @Schema(description = "Timestamp when query execution started", 
            example = "2025-08-18T10:30:45.123Z")
    @JsonProperty("executionStartTime")
    private Instant executionStartTime;

    @Schema(description = "Timestamp when query execution completed", 
            example = "2025-08-18T10:30:46.373Z")
    @JsonProperty("executionEndTime")
    private Instant executionEndTime;

    @Schema(description = "User who executed the query", 
            example = "john.analyst")
    @JsonProperty("executedBy")
    private String executedBy;

    @Schema(description = "User role at time of execution", 
            example = "JOB_VIEWER")
    @JsonProperty("userRole")
    private String userRole;

    @Schema(description = "Correlation ID for audit trail", 
            example = "corr_12345678-abcd-1234-5678-123456789012")
    @JsonProperty("correlationId")
    private String correlationId;

    @Schema(description = "Security classification of results", 
            example = "INTERNAL",
            allowableValues = {"PUBLIC", "INTERNAL", "CONFIDENTIAL", "RESTRICTED"})
    @JsonProperty("securityClassification")
    private String securityClassification;

    @Schema(description = "Data classification of results", 
            example = "SENSITIVE",
            allowableValues = {"PUBLIC", "INTERNAL", "SENSITIVE", "HIGHLY_SENSITIVE"})
    @JsonProperty("dataClassification")
    private String dataClassification;

    @Schema(description = "Query parameters used in execution", 
            example = "{\"batchDate\": \"2025-08-18\", \"minAmount\": 100.00}")
    @JsonProperty("queryParameters")
    private Map<String, Object> queryParameters;

    @Schema(description = "Query validation results", 
            example = "{\"validationPassed\": true, \"securityChecks\": \"PASSED\", \"parameterValidation\": \"PASSED\"}")
    @JsonProperty("validationResults")
    private Map<String, Object> validationResults;

    @Schema(description = "Performance metrics and statistics", 
            example = "{\"cpuTimeMs\": 850, \"ioReadBytes\": 1024000, \"cacheHitRatio\": 0.85}")
    @JsonProperty("performanceMetrics")
    private Map<String, Object> performanceMetrics;

    @Schema(description = "Error information if execution failed", 
            example = "{\"errorCode\": \"TIMEOUT\", \"errorMessage\": \"Query execution exceeded 30 second timeout\"}")
    @JsonProperty("errorInfo")
    private Map<String, Object> errorInfo;

    @Schema(description = "Warning messages during execution", 
            example = "[\"Query returned maximum allowed rows (100), results may be truncated\"]")
    @JsonProperty("warnings")
    private List<String> warnings;

    @Schema(description = "Compliance and audit information", 
            example = "{\"auditTrailId\": \"audit_12345\", \"complianceTags\": [\"SOX\", \"PCI_DSS\"], \"dataRetentionDays\": 2555}")
    @JsonProperty("complianceInfo")
    private Map<String, Object> complianceInfo;

    @Schema(description = "Query execution context", 
            example = "PRODUCTION",
            allowableValues = {"DEVELOPMENT", "TEST", "STAGING", "PRODUCTION"})
    @JsonProperty("executionContext")
    private String executionContext;

    @Schema(description = "Result pagination information", 
            example = "{\"totalRows\": 150, \"returnedRows\": 100, \"hasMoreResults\": true}")
    @JsonProperty("paginationInfo")
    private Map<String, Object> paginationInfo;

    @Schema(description = "Data lineage tracking information", 
            example = "{\"sourceSystem\": \"CORE_BANKING\", \"dataAsOfDate\": \"2025-08-18\", \"lineageId\": \"lineage_789\"}")
    @JsonProperty("dataLineage")
    private Map<String, Object> dataLineage;

    @Schema(description = "Query optimization recommendations", 
            example = "[\"Consider adding index on account_id column\", \"Query could benefit from date partitioning\"]")
    @JsonProperty("optimizationRecommendations")
    private List<String> optimizationRecommendations;

    @Schema(description = "Next execution allowable time for rate limiting", 
            example = "2025-08-18T10:35:45.123Z")
    @JsonProperty("nextExecutionAllowedAt")
    private Instant nextExecutionAllowedAt;

    @Schema(description = "Query result hash for change detection", 
            example = "sha256:a1b2c3d4e5f6...")
    @JsonProperty("resultHash")
    private String resultHash;

    /**
     * Checks if the query execution was successful.
     * 
     * @return true if execution was successful
     */
    public boolean isSuccessful() {
        return "SUCCESS".equals(executionStatus);
    }

    /**
     * Checks if the query execution failed.
     * 
     * @return true if execution failed
     */
    public boolean isFailed() {
        return "FAILED".equals(executionStatus) || 
               "TIMEOUT".equals(executionStatus) || 
               "CANCELLED".equals(executionStatus) ||
               "VALIDATION_FAILED".equals(executionStatus);
    }

    /**
     * Gets the execution duration in seconds.
     * 
     * @return execution duration in seconds
     */
    public Double getExecutionTimeSeconds() {
        return executionTimeMs != null ? executionTimeMs / 1000.0 : null;
    }

    /**
     * Checks if the results contain sensitive data.
     * 
     * @return true if results contain sensitive data
     */
    public boolean containsSensitiveData() {
        return "SENSITIVE".equals(dataClassification) || 
               "HIGHLY_SENSITIVE".equals(dataClassification);
    }

    /**
     * Checks if the results are classified as confidential or higher.
     * 
     * @return true if results are confidential
     */
    public boolean isConfidential() {
        return "CONFIDENTIAL".equals(securityClassification) || 
               "RESTRICTED".equals(securityClassification);
    }

    /**
     * Gets the effective number of results returned.
     * 
     * @return number of result rows
     */
    public int getEffectiveRowCount() {
        if (rowCount != null) {
            return rowCount;
        }
        return results != null ? results.size() : 0;
    }

    /**
     * Checks if the query hit the maximum row limit.
     * 
     * @return true if results were truncated due to row limit
     */
    public boolean isResultsTruncated() {
        if (paginationInfo != null) {
            Object hasMore = paginationInfo.get("hasMoreResults");
            return Boolean.TRUE.equals(hasMore);
        }
        return getEffectiveRowCount() >= 100; // Default max rows
    }

    /**
     * Gets a summary of the query execution for logging.
     * 
     * @return execution summary string
     */
    public String getExecutionSummary() {
        return String.format("Query: %s, Status: %s, Rows: %d, Time: %dms, User: %s, Correlation: %s",
                           queryName != null ? queryName : masterQueryId,
                           executionStatus,
                           getEffectiveRowCount(),
                           executionTimeMs != null ? executionTimeMs : 0,
                           executedBy,
                           correlationId);
    }

    /**
     * Checks if the execution has any warnings.
     * 
     * @return true if warnings are present
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    /**
     * Checks if the execution has error information.
     * 
     * @return true if error information is present
     */
    public boolean hasErrors() {
        return errorInfo != null && !errorInfo.isEmpty();
    }

    /**
     * Gets the primary error message if execution failed.
     * 
     * @return error message or null if no errors
     */
    public String getPrimaryErrorMessage() {
        if (errorInfo != null) {
            Object message = errorInfo.get("errorMessage");
            return message != null ? message.toString() : null;
        }
        return null;
    }

    /**
     * Gets the error code if execution failed.
     * 
     * @return error code or null if no errors
     */
    public String getErrorCode() {
        if (errorInfo != null) {
            Object code = errorInfo.get("errorCode");
            return code != null ? code.toString() : null;
        }
        return null;
    }
}