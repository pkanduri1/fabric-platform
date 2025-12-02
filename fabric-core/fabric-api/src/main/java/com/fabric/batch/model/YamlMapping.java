package com.fabric.batch.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

/**
 * Represents a YAML mapping configuration for batch processing.
 * 
 * This model corresponds to the structure of YAML files used by the
 * GenericProcessor and batch job execution framework.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YamlMapping {
    
    /**
     * The file type identifier (typically the job name).
     */
    private String fileType;
    
    /**
     * The transaction type for this mapping (optional).
     */
    private String transactionType;
    
    /**
     * Map of field names to their corresponding field mapping configurations.
     * Key: field name (lowercase, underscore-separated)
     * Value: FieldMapping configuration
     */
    private Map<String, FieldMapping> fields;

    /**
     * Template reference for this mapping (optional).
     */
    private String template;

    /**
     * Overall compliance level for this mapping (optional).
     */
    private String complianceLevel;

    /**
     * Version of this mapping configuration (optional).
     */
    private String version;

    /**
     * Last modified timestamp for this mapping (optional).
     */
    private String lastModified;
}