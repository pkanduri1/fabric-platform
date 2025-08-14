package com.truist.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity for US001 Manual Job Configuration table.
 * 
 * Represents the core job configuration data for manual job scheduling and management.
 * This entity supports the US001 Manual Job Configuration Interface implementation.
 * 
 * Enterprise Standards:
 * - Configuration-first approach for all job parameters
 * - Audit trail integration for SOX compliance
 * - Banking-grade data classification and handling
 * - Support for complex job parameter structures via JSON/CLOB storage
 * 
 * Security Considerations:
 * - Job parameters may contain sensitive configuration data
 * - Proper access control required for job management operations
 * - Audit trail maintained for all configuration changes
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since US001 - Manual Job Configuration Interface
 */
@Entity
@Table(name = "MANUAL_JOB_CONFIG")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ManualJobConfigEntity {

    /**
     * Unique configuration identifier.
     * Format: cfg_{system}_{sequence}_{date}
     */
    @Id
    @Column(name = "CONFIG_ID", length = 50, nullable = false)
    private String configId;

    /**
     * Human-readable job name for identification and management.
     * Must be unique within the system for active configurations.
     */
    @Column(name = "JOB_NAME", length = 100, nullable = false)
    private String jobName;

    /**
     * Job type classification (ETL, BATCH, VALIDATION, etc.).
     * Used for categorization and specialized processing logic.
     */
    @Column(name = "JOB_TYPE", length = 50, nullable = false)
    private String jobType;

    /**
     * Source system identifier for data lineage tracking.
     * Critical for regulatory compliance and audit trail.
     */
    @Column(name = "SOURCE_SYSTEM", length = 50, nullable = false)
    private String sourceSystem;

    /**
     * Target system identifier for data lineage tracking.
     * Essential for understanding data flow and dependencies.
     */
    @Column(name = "TARGET_SYSTEM", length = 50, nullable = false)
    private String targetSystem;

    /**
     * JSON-formatted job parameters configuration.
     * Contains all job-specific configuration including:
     * - Data source connections
     * - Transformation rules
     * - Validation criteria
     * - Output specifications
     * 
     * Security Note: Sensitive parameters should be encrypted at application layer.
     */
    @Lob
    @Column(name = "JOB_PARAMETERS", nullable = false)
    private String jobParameters;

    /**
     * Current status of the job configuration.
     * Valid values: ACTIVE, INACTIVE, DEPRECATED
     */
    @Column(name = "STATUS", length = 20, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    /**
     * User who created this configuration.
     * Required for audit trail and accountability.
     */
    @Column(name = "CREATED_BY", length = 50, nullable = false)
    private String createdBy;

    /**
     * Timestamp when this configuration was created.
     * Automatically populated by JPA audit framework.
     */
    @CreatedDate
    @Column(name = "CREATED_DATE", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    /**
     * User who last updated this configuration.
     * Required for audit trail and accountability.
     */
    @Column(name = "UPDATED_BY", length = 50)
    private String updatedBy;

    /**
     * Timestamp when this configuration was last updated.
     * Automatically populated during update operations.
     */
    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    /**
     * Configuration version number for change tracking.
     * Incremented with each modification for optimistic locking.
     */
    @Version
    @Column(name = "VERSION_DECIMAL", nullable = false)
    @Builder.Default
    private Long versionNumber = 1L;

    // Business Logic Methods

    /**
     * Check if this configuration is currently active.
     * 
     * @return true if status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }

    /**
     * Activate this job configuration.
     * Changes status to ACTIVE for job execution.
     */
    public void activate() {
        this.status = "ACTIVE";
    }

    /**
     * Deactivate this job configuration.
     * Changes status to INACTIVE to prevent job execution.
     */
    public void deactivate() {
        this.status = "INACTIVE";
    }

    /**
     * Mark this configuration as deprecated.
     * Used when configuration is replaced but needs to be retained for audit purposes.
     */
    public void deprecate() {
        this.status = "DEPRECATED";
    }

    /**
     * Generate a display name for this configuration.
     * Combines job name with source/target systems for clarity.
     * 
     * @return formatted display name
     */
    public String getDisplayName() {
        return String.format("%s (%s -> %s)", jobName, sourceSystem, targetSystem);
    }

    /**
     * Validate that this configuration has all required fields.
     * Used before persisting to database.
     * 
     * @return true if configuration is valid, false otherwise
     */
    public boolean isValidConfiguration() {
        return configId != null && !configId.trim().isEmpty() &&
               jobName != null && !jobName.trim().isEmpty() &&
               jobType != null && !jobType.trim().isEmpty() &&
               sourceSystem != null && !sourceSystem.trim().isEmpty() &&
               targetSystem != null && !targetSystem.trim().isEmpty() &&
               jobParameters != null && !jobParameters.trim().isEmpty() &&
               createdBy != null && !createdBy.trim().isEmpty();
    }

    /**
     * Update this configuration with new values.
     * Updates core configuration fields and sets modification metadata.
     * 
     * @param newJobName the new job name
     * @param newJobType the new job type
     * @param newSourceSystem the new source system
     * @param newTargetSystem the new target system
     * @param newJobParameters the new job parameters (JSON format)
     * @param updatedBy the user performing the update
     */
    public void updateConfiguration(String newJobName, String newJobType, 
                                  String newSourceSystem, String newTargetSystem, 
                                  String newJobParameters, String updatedBy) {
        this.jobName = newJobName;
        this.jobType = newJobType;
        this.sourceSystem = newSourceSystem;
        this.targetSystem = newTargetSystem;
        this.jobParameters = newJobParameters;
        this.updatedBy = updatedBy;
        this.updatedDate = LocalDateTime.now();
    }
}