package com.fabric.batch.entity;

import lombok.*;

import java.time.LocalDateTime;

/**
 * POJO for Job Parameter Template table.
 * 
 * Represents reusable job parameter templates in the Fabric Platform with
 * enterprise-grade template inheritance, versioning, and compliance features.
 * 
 * Enterprise Standards:
 * - Template inheritance with parent/child relationships
 * - Comprehensive audit trail for SOX compliance
 * - Usage tracking for performance analytics
 * - Schema validation for parameter structures
 * - Compliance documentation and notes
 * 
 * Security Considerations:
 * - System template protection (read-only for business users)
 * - Access control through status management
 * - Template deprecation with replacement tracking
 * - Sensitive parameter identification
 * 
 * @author Senior Full Stack Developer Agent
 * @version 2.0
 * @since US001 Phase 2 - Parameter Template Management
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class JobParameterTemplateEntity {

    /**
     * Unique template identifier with structured format: tpl_{type}_{timestamp}_{hash}
     * Example: tpl_etl_1691234567890_a1b2c3d4
     */
    @EqualsAndHashCode.Include
    private String templateId;

    /**
     * Human-readable template name, must be unique across all templates.
     * Used for template discovery and selection in user interfaces.
     */
    private String templateName;

    /**
     * Job type this template applies to (ETL_BATCH, DATA_MIGRATION, etc.).
     * Links template to specific job execution patterns.
     */
    private String jobType;

    /**
     * Template version in semantic versioning format (1.0, 1.1, 2.0).
     * Supports template evolution and backward compatibility.
     */
    private String templateVersion;

    /**
     * Detailed description of template purpose and usage guidelines.
     * Supports business user understanding and template selection.
     */
    private String templateDescription;

    /**
     * JSON schema defining parameter structure, types, and validation rules.
     * Enables runtime parameter validation and UI form generation.
     */
    private String templateSchema;

    /**
     * JSON object containing default parameter values.
     * Applied when parameters are not explicitly provided by users.
     */
    private String defaultValues;

    /**
     * JSON object containing validation rules and constraints.
     * Defines business rules for parameter validation beyond schema.
     */
    private String validationRules;

    /**
     * Template category for organization and discovery.
     * Examples: BANKING, RISK, COMPLIANCE, DATA_MIGRATION
     */
    private String category;

    /**
     * Comma-separated tags for advanced search and filtering.
     * Supports metadata-driven template discovery.
     */
    private String tags;

    /**
     * Template status: ACTIVE, INACTIVE, DEPRECATED, DRAFT.
     * Controls template availability and lifecycle management.
     */
    private String status;

    /**
     * Indicates if template is managed by system (read-only for business users).
     * System templates require elevated privileges for modifications.
     */
    private Character isSystemTemplate;

    /**
     * Indicates if template is deprecated with migration path available.
     * Deprecated templates remain available but not recommended for new usage.
     */
    private Character isDeprecated;

    /**
     * Parent template ID for inheritance relationships.
     * Supports template hierarchies and parameter inheritance.
     */
    private String parentTemplateId;

    /**
     * Extended template ID for composition relationships.
     * Allows templates to extend functionality from other templates.
     */
    private String extendsTemplateId;

    /**
     * Count of template usage for analytics and optimization.
     * Tracks template popularity and adoption patterns.
     */
    private Long usageCount;

    /**
     * Timestamp of last template usage for cache management.
     * Supports intelligent caching and performance optimization.
     */
    private LocalDateTime lastUsedDate;

    /**
     * Compliance-related notes and documentation references.
     * Supports regulatory requirements and audit documentation.
     */
    private String complianceNotes;

    /**
     * URL to detailed template documentation and guidelines.
     * Provides link to comprehensive template usage documentation.
     */
    private String documentationUrl;

    /**
     * User who created the template.
     * Required for audit trail and ownership tracking.
     */
    private String createdBy;

    /**
     * Template creation timestamp.
     * Automatic timestamp for audit and lifecycle tracking.
     */
    private LocalDateTime createdDate;

    /**
     * User who last updated the template.
     * Tracks modification ownership for audit purposes.
     */
    private String updatedBy;

    /**
     * Last update timestamp.
     * Tracks modification timing for audit and change management.
     */
    private LocalDateTime updatedDate;

    /**
     * Version number for optimistic locking and change tracking.
     * Incremented on each update to prevent concurrent modification conflicts.
     */
    private Long versionDecimal;

    /**
     * Utility method to check if template is active and available for use.
     * 
     * @return true if template is active and not deprecated
     */
    public boolean isActive() {
        return "ACTIVE".equals(status) && !Character.valueOf('Y').equals(isDeprecated);
    }

    /**
     * Utility method to check if template is a system-managed template.
     * 
     * @return true if template is system-managed
     */
    public boolean isSystemManaged() {
        return Character.valueOf('Y').equals(isSystemTemplate);
    }

    /**
     * Utility method to check if template is deprecated.
     * 
     * @return true if template is marked as deprecated
     */
    public boolean isTemplateDeprecated() {
        return Character.valueOf('Y').equals(isDeprecated);
    }

    /**
     * Utility method to increment usage count and update last used date.
     * Used for analytics and performance optimization.
     */
    public void recordUsage() {
        this.usageCount = (this.usageCount != null ? this.usageCount : 0L) + 1L;
        this.lastUsedDate = LocalDateTime.now();
    }

    /**
     * Utility method to update template metadata during modifications.
     * 
     * @param updatedBy user performing the update
     */
    public void updateMetadata(String updatedBy) {
        this.updatedBy = updatedBy;
        this.updatedDate = LocalDateTime.now();
        this.versionDecimal = (this.versionDecimal != null ? this.versionDecimal : 0L) + 1L;
    }

    /**
     * Utility method to deprecate template with replacement information.
     * 
     * @param replacementTemplateId optional replacement template
     * @param deprecatedBy user performing deprecation
     * @param reason deprecation reason
     */
    public void deprecate(String replacementTemplateId, String deprecatedBy, String reason) {
        this.isDeprecated = 'Y';
        this.status = "DEPRECATED";
        this.extendsTemplateId = replacementTemplateId; // Repurpose field for replacement tracking
        this.complianceNotes = (this.complianceNotes != null ? this.complianceNotes + " | " : "") + 
                              "DEPRECATED: " + reason + " [" + deprecatedBy + "]";
        updateMetadata(deprecatedBy);
    }
}