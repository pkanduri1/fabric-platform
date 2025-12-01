package com.truist.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entity for Source Configuration table (SOURCE_CONFIG).
 *
 * Represents database-driven configuration properties for different source systems
 * (HR, ENCORE, PAYROLL, etc.). This entity supports the Phase 2 database-driven
 * configuration system that enables dynamic property resolution based on source context.
 *
 * Enterprise Standards:
 * - Configuration-first approach for all source-specific parameters
 * - Database-driven property resolution replaces hardcoded values
 * - Support for multiple source systems with isolated configurations
 * - Audit trail integration for SOX compliance
 *
 * Security Considerations:
 * - Configuration values may contain sensitive path information
 * - Proper access control required for configuration management operations
 * - Audit trail maintained for all configuration changes
 * - Active/inactive flag supports configuration lifecycle management
 *
 * Usage Example:
 * <pre>
 * SourceConfigEntity config = SourceConfigEntity.builder()
 *     .configId("cfg_hr_001")
 *     .sourceCode("HR")
 *     .configKey("batch.defaults.outputBasePath")
 *     .configValue("/data/output/hr")
 *     .description("HR system output base path")
 *     .active("Y")
 *     .modifiedBy("admin")
 *     .build();
 * </pre>
 *
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Phase 2 - Database-Driven Source Configuration System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceConfigEntity {

    /**
     * Unique configuration identifier.
     * Format: cfg_{sourceCode}_{sequence}
     * Example: cfg_hr_001, cfg_encore_001
     */
    private String configId;

    /**
     * Source system identifier.
     * Identifies which source system this configuration belongs to.
     * Examples: HR, ENCORE, PAYROLL, LENDING
     * Used to isolate configurations between different source systems.
     */
    private String sourceCode;

    /**
     * Configuration property key.
     * Defines the property key that will be resolved at runtime.
     * Examples:
     * - batch.defaults.outputBasePath
     * - batch.defaults.inputBasePath
     * - batch.defaults.archivePath
     * - batch.defaults.errorPath
     *
     * These keys can be referenced in YAML configurations using ${} syntax:
     * outputPath: ${batch.defaults.outputBasePath}/atoctran
     */
    private String configKey;

    /**
     * Configuration property value.
     * The actual value that will be used when the property is resolved.
     * Examples:
     * - /data/output/hr
     * - /data/input/encore
     * - /archive/payroll
     *
     * Security Note: Sensitive values should be encrypted at application layer.
     */
    private String configValue;

    /**
     * Human-readable description of the configuration property.
     * Helps administrators understand the purpose and usage of the property.
     */
    private String description;

    /**
     * Timestamp when this configuration was created.
     * Automatically populated during save operations.
     * Required for audit trail and SOX compliance.
     */
    private LocalDateTime createdDate;

    /**
     * Timestamp when this configuration was last modified.
     * Automatically populated during update operations.
     * Required for audit trail and SOX compliance.
     */
    private LocalDateTime modifiedDate;

    /**
     * User who last modified this configuration.
     * Required for audit trail and accountability.
     * Tracks who made the configuration change for SOX compliance.
     */
    private String modifiedBy;

    /**
     * Active flag indicating whether this configuration is active.
     * Valid values: 'Y' (active), 'N' (inactive)
     * Inactive configurations are ignored during property resolution.
     * Default value: 'Y'
     */
    @Builder.Default
    private String active = "Y";

    // Business Logic Methods

    /**
     * Check if this configuration is currently active.
     *
     * @return true if active flag is 'Y', false otherwise
     */
    public boolean isActive() {
        return "Y".equals(this.active);
    }

    /**
     * Activate this configuration.
     * Changes active flag to 'Y' to enable property resolution.
     */
    public void activate() {
        this.active = "Y";
    }

    /**
     * Deactivate this configuration.
     * Changes active flag to 'N' to disable property resolution.
     */
    public void deactivate() {
        this.active = "N";
    }

    /**
     * Validate that this configuration has all required fields.
     * Used before persisting to database.
     *
     * @return true if configuration is valid, false otherwise
     */
    public boolean isValidConfiguration() {
        return configId != null && !configId.trim().isEmpty() &&
               sourceCode != null && !sourceCode.trim().isEmpty() &&
               configKey != null && !configKey.trim().isEmpty() &&
               configValue != null && !configValue.trim().isEmpty() &&
               modifiedBy != null && !modifiedBy.trim().isEmpty();
    }

    /**
     * Update this configuration with new value and description.
     * Updates the configuration value and metadata fields.
     *
     * @param newConfigValue the new configuration value
     * @param newDescription the new description
     * @param modifiedBy the user performing the update
     */
    public void updateConfiguration(String newConfigValue, String newDescription, String modifiedBy) {
        this.configValue = newConfigValue;
        this.description = newDescription;
        this.modifiedBy = modifiedBy;
        this.modifiedDate = LocalDateTime.now();
    }

    /**
     * Generate a display name for this configuration.
     * Combines source code and config key for clarity.
     *
     * @return formatted display name
     */
    public String getDisplayName() {
        return String.format("%s: %s", sourceCode, configKey);
    }

    /**
     * Get the full property path for Spring property resolution.
     * Returns the config key as-is since it already contains the full path.
     *
     * @return the configuration key (full property path)
     */
    public String getPropertyPath() {
        return this.configKey;
    }

    /**
     * Check if this configuration belongs to a specific source system.
     *
     * @param sourceCode the source code to check
     * @return true if this configuration belongs to the specified source, false otherwise
     */
    public boolean belongsToSource(String sourceCode) {
        return this.sourceCode != null && this.sourceCode.equalsIgnoreCase(sourceCode);
    }

    /**
     * Create a copy of this configuration for a different source system.
     * Useful for cloning configurations across source systems.
     *
     * @param newSourceCode the source code for the new configuration
     * @param newConfigId the new configuration ID
     * @param modifiedBy the user creating the copy
     * @return a new SourceConfigEntity with the specified source code
     */
    public SourceConfigEntity copyForSource(String newSourceCode, String newConfigId, String modifiedBy) {
        return SourceConfigEntity.builder()
            .configId(newConfigId)
            .sourceCode(newSourceCode)
            .configKey(this.configKey)
            .configValue(this.configValue)
            .description(this.description + " (Copied from " + this.sourceCode + ")")
            .createdDate(LocalDateTime.now())
            .modifiedDate(LocalDateTime.now())
            .modifiedBy(modifiedBy)
            .active(this.active)
            .build();
    }
}
