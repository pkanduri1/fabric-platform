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
 * MASTER QUERY UPDATE REQUEST DTO - BANKING GRADE SECURITY
 * =========================================================================
 * 
 * Purpose: Data Transfer Object for updating existing master query configurations
 * - Version control and change tracking
 * - Security classification validation
 * - Business justification requirement for changes
 * 
 * Security Features:
 * - Change justification for audit compliance
 * - Version increment tracking
 * - Field-level change detection
 * - Approval workflow integration (future)
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Master Query CRUD Implementation
 * =========================================================================
 */
@Data
@Builder
@Schema(description = "Request to update an existing master query configuration")
public class MasterQueryUpdateRequest {

    /**
     * Unique identifier of the master query to update.
     * Required for identifying the target configuration.
     */
    @NotNull(message = "Master query ID is required for update")
    @Positive(message = "Master query ID must be positive")
    @JsonProperty("id")
    @Schema(description = "Unique identifier of the master query to update", 
            example = "123")
    private Long id;

    /**
     * Expected current version for optimistic locking.
     * Prevents concurrent modification conflicts.
     */
    @NotNull(message = "Current version is required for optimistic locking")
    @Positive(message = "Version must be positive")
    @JsonProperty("currentVersion")
    @Schema(description = "Expected current version for optimistic locking", 
            example = "2")
    private Integer currentVersion;

    /**
     * Source system identifier for data integration.
     * Can be updated if data source changes.
     * Validated dynamically against SOURCE_SYSTEMS database table.
     */
    @ValidSourceSystem(allowEmpty = true, message = "Source system must exist in the database and be enabled")
    @JsonProperty("sourceSystem")
    @Schema(description = "Source system identifier - validated against SOURCE_SYSTEMS database table", 
            example = "ENCORE")
    private String sourceSystem;

    /**
     * Descriptive name for the master query.
     * Can be updated for better clarity.
     */
    @Size(min = 3, max = 100, message = "Query name must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-\\.]+$", 
            message = "Query name can only contain letters, numbers, underscores, hyphens, and dots")
    @JsonProperty("queryName")
    @Schema(description = "Descriptive name for the master query", 
            example = "customer_account_summary_v2")
    private String queryName;

    /**
     * Human-readable description of the query purpose.
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @JsonProperty("description")
    @Schema(description = "Human-readable description of the query purpose", 
            example = "Enhanced customer account summary with risk indicators")
    private String description;

    /**
     * Type of query operation (SELECT, WITH).
     * Can be updated if query structure changes significantly.
     */
    @Pattern(regexp = "^(SELECT|WITH)$", 
            message = "Query type must be SELECT or WITH")
    @JsonProperty("queryType")
    @Schema(description = "Type of query operation", 
            example = "SELECT",
            allowableValues = {"SELECT", "WITH"})
    private String queryType;

    /**
     * SQL query content with parameter placeholders.
     * Core field that drives most updates.
     */
    @Size(min = 10, max = 10000, message = "Query SQL must be between 10 and 10000 characters")
    @JsonProperty("querySql")
    @Schema(description = "Updated SQL query content with parameter placeholders", 
            example = "SELECT account_id, balance, risk_score FROM accounts a JOIN risk_assessment r ON a.customer_id = r.customer_id WHERE a.customer_id = :customerId")
    private String querySql;

    /**
     * Active status indicator (Y/N).
     * Can be updated to activate/deactivate queries.
     */
    @Pattern(regexp = "^[YN]$", message = "Active status must be Y or N")
    @JsonProperty("isActive")
    @Schema(description = "Active status indicator", 
            example = "Y",
            allowableValues = {"Y", "N"})
    private String isActive;

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
     * Business justification for the update.
     * Required for SOX compliance and audit trail.
     */
    @NotBlank(message = "Change justification is required for audit compliance")
    @Size(min = 20, max = 1000, message = "Change justification must be between 20 and 1000 characters")
    @JsonProperty("changeJustification")
    @Schema(description = "Business justification for the update", 
            example = "Enhanced query to include risk assessment data for improved regulatory reporting accuracy")
    private String changeJustification;

    /**
     * Summary of specific changes made.
     * Helps reviewers understand modifications.
     */
    @Size(max = 500, message = "Change summary cannot exceed 500 characters")
    @JsonProperty("changeSummary")
    @Schema(description = "Summary of specific changes made", 
            example = "Added JOIN with risk_assessment table, included risk_score column")
    private String changeSummary;

    /**
     * Compliance tags for regulatory requirements.
     */
    @JsonProperty("complianceTags")
    @Schema(description = "Updated compliance tags for regulatory requirements", 
            example = "[\"SOX\", \"PCI_DSS\", \"BASEL_III\", \"GDPR\"]")
    private List<String> complianceTags;

    /**
     * Expected parameters for the updated query.
     */
    @JsonProperty("expectedParameters")
    @Schema(description = "Updated expected parameters for the query")
    private List<MasterQueryCreateRequest.QueryParameterMetadata> expectedParameters;

    /**
     * Additional metadata for the query.
     */
    @JsonProperty("metadata")
    @Schema(description = "Updated additional metadata for the query")
    private Map<String, Object> metadata;

    /**
     * Whether this update should create a new version.
     * Major changes should increment version.
     */
    @JsonProperty("createNewVersion")
    @Schema(description = "Whether this update should create a new version", 
            example = "true")
    private Boolean createNewVersion;

    /**
     * Whether to preserve the old version as inactive.
     * Supports rollback scenarios.
     */
    @JsonProperty("preserveOldVersion")
    @Schema(description = "Whether to preserve the old version as inactive", 
            example = "true")
    private Boolean preserveOldVersion;

    /**
     * Validate the update request for business rules.
     */
    public boolean isValidForUpdate() {
        return id != null && id > 0 &&
               currentVersion != null && currentVersion > 0 &&
               changeJustification != null && !changeJustification.trim().isEmpty() &&
               hasAtLeastOneUpdateField();
    }

    /**
     * Check if at least one field is being updated.
     */
    private boolean hasAtLeastOneUpdateField() {
        return sourceSystem != null ||
               queryName != null ||
               description != null ||
               queryType != null ||
               querySql != null ||
               isActive != null ||
               dataClassification != null ||
               securityClassification != null ||
               complianceTags != null ||
               expectedParameters != null ||
               metadata != null;
    }

    /**
     * Check if query type is allowed (read-only operations only).
     */
    public boolean isValidQueryType() {
        if (queryType == null) return true; // Not being updated
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
        return true; // Annotation handles the validation
    }

    /**
     * Determine if this is a major change requiring version increment.
     */
    public boolean isMajorChange() {
        return createNewVersion != null && createNewVersion ||
               querySql != null ||  // SQL changes are always major
               queryType != null || // Type changes are major
               (isActive != null && "N".equals(isActive)); // Deactivation is major
    }

    /**
     * Determine if this is a minor change (metadata only).
     */
    public boolean isMinorChange() {
        return !isMajorChange() && 
               (description != null || 
                dataClassification != null ||
                securityClassification != null ||
                complianceTags != null ||
                metadata != null);
    }

    /**
     * Get fields that are being updated.
     */
    public List<String> getUpdatedFields() {
        List<String> updatedFields = new java.util.ArrayList<>();
        
        if (sourceSystem != null) updatedFields.add("sourceSystem");
        if (queryName != null) updatedFields.add("queryName");
        if (description != null) updatedFields.add("description");
        if (queryType != null) updatedFields.add("queryType");
        if (querySql != null) updatedFields.add("querySql");
        if (isActive != null) updatedFields.add("isActive");
        if (dataClassification != null) updatedFields.add("dataClassification");
        if (securityClassification != null) updatedFields.add("securityClassification");
        if (complianceTags != null) updatedFields.add("complianceTags");
        if (expectedParameters != null) updatedFields.add("expectedParameters");
        if (metadata != null) updatedFields.add("metadata");
        
        return updatedFields;
    }

    /**
     * Extract parameter names from SQL query if being updated.
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