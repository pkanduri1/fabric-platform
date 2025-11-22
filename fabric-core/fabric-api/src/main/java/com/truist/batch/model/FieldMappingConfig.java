package com.truist.batch.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a complete field mapping configuration for a batch job.
 * 
 * This model contains all the information needed to generate YAML
 * configuration files for the batch processing framework.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldMappingConfig {
    
    /**
     * Unique identifier for this configuration.
     */
    private String id;
    
    /**
     * The source system identifier (e.g., "hr", "dda", "shaw").
     */
    private String sourceSystem;
    
    /**
     * The job name identifier (e.g., "p327", "atoctran").
     */
    private String jobName;
    
    /**
     * The transaction type for this configuration (optional).
     * If null or "default", this is the default configuration.
     */
    private String transactionType;
    
    /**
     * List of field mappings for this configuration.
     */
    private List<FieldMapping> fieldMappings;
    
    /**
     * Additional configuration properties (optional).
     */
    private String description;
    
    /**
     * The template identifier to use for this configuration.
     */
    private String templateId;
    
    /**
     * The master query identifier associated with this configuration.
     * Links to MASTER_QUERY_CONFIG.ID for dynamic query execution.
     */
    private Long masterQueryId;

    /**
     * The actual SQL query text associated with this configuration.
     * Stores the SQL query entered by users in Template Studio.
     */
    private String masterQuery;

    /**
     * Whether this configuration is active.
     */
    private boolean active = true;
    
    /**
     * The version of this configuration.
     */
    private int version;
    
    /**
     * When this configuration was created.
     */
    private LocalDateTime createdDate;

    /**
     * Who created this configuration.
     */
    private String createdBy;

    /**
     * When this configuration was last modified.
     */
    private LocalDateTime lastModified;

    /**
     * Who last modified this configuration.
     */
    private String modifiedBy;
}