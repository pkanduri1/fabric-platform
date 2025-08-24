package com.truist.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.truist.batch.validation.ValidSourceSystem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * =========================================================================
 * MASTER QUERY CREATE REQUEST DTO - BANKING GRADE SECURITY
 * =========================================================================
 * 
 * Purpose: Data Transfer Object for creating new master query configurations
 * - Validation rules for banking compliance
 * - Security classification and audit requirements
 * - Template-based creation support
 * 
 * Security Features:
 * - Input validation and sanitization
 * - Business justification requirement
 * - Data classification enforcement
 * - Audit trail integration
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Master Query CRUD Implementation
 * =========================================================================
 */
@Data
@Builder
@Schema(description = "Request to create a new master query configuration")
public class MasterQueryCreateRequest {

    /**
     * Source system identifier for data integration.
     * Validated dynamically against SOURCE_SYSTEMS database table.
     */
    @NotBlank(message = "Source system is required")
    @ValidSourceSystem(message = "Source system must exist in the database and be enabled")
    @JsonProperty("sourceSystem")
    @Schema(description = "Source system identifier - validated against SOURCE_SYSTEMS database table", 
            example = "ENCORE")
    private String sourceSystem;

    /**
     * Descriptive name for the master query.
     * Must be unique within the source system.
     */
    @NotBlank(message = "Query name is required")
    @Size(min = 3, max = 100, message = "Query name must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-\\.]+$", 
            message = "Query name can only contain letters, numbers, underscores, hyphens, and dots")
    @JsonProperty("queryName")
    @Schema(description = "Descriptive name for the master query", 
            example = "customer_account_summary")
    private String queryName;

    /**
     * Human-readable description of the query purpose.
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @JsonProperty("description")
    @Schema(description = "Human-readable description of the query purpose", 
            example = "Retrieves customer account summary including balance and status")
    private String description;

    /**
     * Type of query operation (SELECT, WITH).
     * Banking regulation: Only read-only operations allowed
     * Defaults to "SELECT" if not specified.
     */
    @Pattern(regexp = "^(SELECT|WITH)$", 
            message = "Query type must be SELECT or WITH")
    @JsonProperty("queryType")
    @Schema(description = "Type of query operation", 
            example = "SELECT",
            allowableValues = {"SELECT", "WITH"})
    private String queryType = "SELECT";

    /**
     * SQL query content with parameter placeholders.
     * Must follow banking security guidelines.
     */
    @NotBlank(message = "Query SQL is required")
    @Size(min = 10, max = 10000, message = "Query SQL must be between 10 and 10000 characters")
    @JsonProperty("querySql")
    @Schema(description = "SQL query content with parameter placeholders", 
            example = "SELECT account_id, balance FROM accounts WHERE customer_id = :customerId")
    private String querySql;

    /**
     * Data classification level for security compliance.
     */
    @Pattern(regexp = "^(PUBLIC|INTERNAL|SENSITIVE|CONFIDENTIAL)$", 
            message = "Data classification must be one of: PUBLIC, INTERNAL, SENSITIVE, CONFIDENTIAL")
    @JsonProperty("dataClassification")
    @Schema(description = "Data classification level", 
            example = "SENSITIVE",
            allowableValues = {"PUBLIC", "INTERNAL", "SENSITIVE", "CONFIDENTIAL"})
    private String dataClassification;

    /**
     * Security classification for access control.
     */
    @Pattern(regexp = "^(PUBLIC|INTERNAL|CONFIDENTIAL|RESTRICTED)$", 
            message = "Security classification must be one of: PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED")
    @JsonProperty("securityClassification")
    @Schema(description = "Security classification for access control", 
            example = "INTERNAL",
            allowableValues = {"PUBLIC", "INTERNAL", "CONFIDENTIAL", "RESTRICTED"})
    private String securityClassification;

    /**
     * Business justification for creating this query.
     * Required for SOX compliance and audit trail.
     */
    @NotBlank(message = "Business justification is required for audit compliance")
    @Size(min = 20, max = 1000, message = "Business justification must be between 20 and 1000 characters")
    @JsonProperty("businessJustification")
    @Schema(description = "Business justification for creating this query", 
            example = "Required for daily customer account reconciliation and regulatory reporting")
    private String businessJustification;

    /**
     * Compliance tags for regulatory requirements.
     */
    @JsonProperty("complianceTags")
    @Schema(description = "Compliance tags for regulatory requirements", 
            example = "[\"SOX\", \"PCI_DSS\", \"BASEL_III\"]")
    private List<String> complianceTags;

    /**
     * Expected parameters for the query.
     * Used for validation and UI generation.
     */
    @JsonProperty("expectedParameters")
    @Schema(description = "Expected parameters for the query with metadata")
    private List<QueryParameterMetadata> expectedParameters;

    /**
     * Template category if created from template.
     */
    @JsonProperty("templateCategory")
    @Schema(description = "Template category if created from template", 
            example = "Account Management")
    private String templateCategory;

    /**
     * Template name if created from template.
     */
    @JsonProperty("templateName")
    @Schema(description = "Template name if created from template", 
            example = "Account Summary")
    private String templateName;

    /**
     * Additional metadata for the query.
     */
    @JsonProperty("metadata")
    @Schema(description = "Additional metadata for the query")
    private Map<String, Object> metadata;

    /**
     * Nested class for parameter metadata.
     */
    @Data
    @Builder
    @Schema(description = "Metadata for query parameters")
    public static class QueryParameterMetadata {
        
        @NotBlank(message = "Parameter name is required")
        @JsonProperty("name")
        @Schema(description = "Parameter name", example = "customerId")
        private String name;
        
        @NotBlank(message = "Parameter type is required")
        @JsonProperty("type")
        @Schema(description = "Parameter data type", example = "STRING")
        private String type;
        
        @JsonProperty("required")
        @Schema(description = "Whether parameter is required", example = "true")
        private Boolean required;
        
        @JsonProperty("description")
        @Schema(description = "Parameter description", example = "Unique customer identifier")
        private String description;
        
        @JsonProperty("defaultValue")
        @Schema(description = "Default value for parameter")
        private String defaultValue;
        
        @JsonProperty("validationPattern")
        @Schema(description = "Regex pattern for parameter validation")
        private String validationPattern;
    }

    /**
     * Validate the create request for business rules.
     */
    public boolean isValidForCreation() {
        return sourceSystem != null && !sourceSystem.trim().isEmpty() &&
               queryName != null && !queryName.trim().isEmpty() &&
               queryType != null && !queryType.trim().isEmpty() &&
               querySql != null && !querySql.trim().isEmpty() &&
               businessJustification != null && !businessJustification.trim().isEmpty() &&
               isValidQueryType() &&
               isValidSourceSystem();
    }

    /**
     * Check if query type is allowed (read-only operations only).
     */
    public boolean isValidQueryType() {
        return "SELECT".equalsIgnoreCase(this.queryType) || 
               "WITH".equalsIgnoreCase(this.queryType);
    }

    /**
     * Check if source system is recognized.
     * NOTE: This method is deprecated - validation is now handled by @ValidSourceSystem annotation
     */
    @Deprecated
    public boolean isValidSourceSystem() {
        // Validation is now handled by @ValidSourceSystem annotation
        // which checks against the actual SOURCE_SYSTEMS database table
        return sourceSystem != null && !sourceSystem.trim().isEmpty();
    }

    /**
     * Get effective data classification based on content analysis.
     */
    public String getEffectiveDataClassification() {
        if (dataClassification != null && !dataClassification.trim().isEmpty()) {
            return dataClassification;
        }
        
        // Auto-classify based on query content
        if (querySql != null) {
            String sql = querySql.toLowerCase();
            if (sql.contains("account") || sql.contains("customer") || sql.contains("ssn")) {
                return "CONFIDENTIAL";
            } else if (sql.contains("transaction") || sql.contains("payment") || sql.contains("amount")) {
                return "SENSITIVE";
            }
        }
        
        return "INTERNAL"; // Default classification
    }

    /**
     * Get effective security classification based on data content.
     */
    public String getEffectiveSecurityClassification() {
        if (securityClassification != null && !securityClassification.trim().isEmpty()) {
            return securityClassification;
        }
        
        // Map from data classification
        String dataClass = getEffectiveDataClassification();
        switch (dataClass) {
            case "CONFIDENTIAL":
                return "CONFIDENTIAL";
            case "SENSITIVE":
                return "INTERNAL";
            default:
                return "INTERNAL";
        }
    }

    /**
     * Extract parameter names from SQL query.
     */
    public List<String> extractParameterNames() {
        if (querySql == null) {
            return List.of();
        }
        
        return java.util.Arrays.stream(querySql.split("\\s+"))
                .filter(word -> word.startsWith(":"))
                .map(word -> word.substring(1).replaceAll("[^a-zA-Z0-9_]", ""))
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }
}