package com.truist.batch.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Model class representing a batch configuration stored in the batch_configurations table.
 * This class corresponds to the CM3INT.BATCH_CONFIGURATIONS database table structure.
 * 
 * Database Table Structure:
 * - ID VARCHAR(100) PRIMARY KEY
 * - SOURCE_SYSTEM VARCHAR(50) NOT NULL
 * - JOB_NAME VARCHAR(50) NOT NULL
 * - TRANSACTION_TYPE VARCHAR(20) DEFAULT '200'
 * - DESCRIPTION VARCHAR(500)
 * - CONFIGURATION_JSON CLOB NOT NULL
 * - CREATED_BY VARCHAR(50) NOT NULL
 * - CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 * - MODIFIED_BY VARCHAR(50)
 * - MODIFIED_DATE TIMESTAMP
 * - VERSION INTEGER DEFAULT 1
 * - ENABLED VARCHAR2(1) DEFAULT 'Y' CHECK (enabled IN ('Y', 'N'))
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchConfiguration {
    
    /**
     * Unique identifier for the configuration.
     * Format: {sourceSystem}-{jobName}-{transactionType}-{timestamp}
     */
    private String id;
    
    /**
     * Source system identifier (e.g., "ENCORE", "HR", "SHAW")
     */
    private String sourceSystem;
    
    /**
     * Job name identifier (e.g., "atoctran", "p327")
     */
    private String jobName;
    
    /**
     * Transaction type code (e.g., "200", "300", "900")
     * Defaults to "200" if not specified
     */
    private String transactionType;
    
    /**
     * Human-readable description of the configuration
     */
    private String description;
    
    /**
     * JSON representation of the FieldMappingConfig
     * Stored as CLOB in database
     */
    private String configurationJson;
    
    /**
     * User who created this configuration
     */
    private String createdBy;
    
    /**
     * Timestamp when configuration was created
     */
    private LocalDateTime createdDate;
    
    /**
     * User who last modified this configuration
     */
    private String lastModifiedBy;
    
    /**
     * Timestamp when configuration was last modified
     */
    private LocalDateTime lastModifiedDate;
    
    /**
     * Version number for optimistic locking and change tracking
     * Defaults to "1"
     */
    private String version;
    
    /**
     * Whether this configuration is enabled
     * Values: "Y" (enabled) or "N" (disabled)
     * Defaults to "Y"
     */
    private String enabled;
    
    /**
     * Convenience constructor for creating a new configuration
     */
    public BatchConfiguration(String sourceSystem, String jobName, String transactionType, 
                            String configurationJson, String createdBy) {
        this.sourceSystem = sourceSystem;
        this.jobName = jobName;
        this.transactionType = transactionType != null ? transactionType : "200";
        this.configurationJson = configurationJson;
        this.createdBy = createdBy;
        this.createdDate = LocalDateTime.now();
        this.version = "1";
        this.enabled = "Y";
        this.description = "Configuration for " + sourceSystem + "/" + jobName;
    }
    
    /**
     * Generate a unique ID for this configuration
     */
    public void generateId() {
        if (this.id == null) {
            this.id = this.sourceSystem + "-" + this.jobName + "-" + 
                     this.transactionType + "-" + System.currentTimeMillis();
        }
    }
    
    /**
     * Check if this configuration is enabled
     */
    public boolean isEnabledFlag() {
        return "Y".equals(this.enabled);
    }
    
    /**
     * Set enabled status using boolean
     */
    public void setEnabledFlag(boolean enabled) {
        this.enabled = enabled ? "Y" : "N";
    }
    
    /**
     * Update modification timestamp and user
     */
    public void markAsModified(String modifiedBy) {
        this.lastModifiedBy = modifiedBy;
        this.lastModifiedDate = LocalDateTime.now();
    }
}