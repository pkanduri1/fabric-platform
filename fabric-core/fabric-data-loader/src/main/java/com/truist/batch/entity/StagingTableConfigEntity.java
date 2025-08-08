package com.truist.batch.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing staging table configuration for ETL pipeline data transformation.
 * 
 * This entity supports database-driven transformation by defining staging area
 * characteristics, processing parameters, and data governance requirements.
 */
@Entity
@Table(name = "staging_table_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StagingTableConfigEntity {
    
    @Id
    @Column(name = "staging_config_id", length = 100)
    private String stagingConfigId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private TransformationConfigEntity transformationConfig;
    
    @Column(name = "staging_table_name", nullable = false, length = 100)
    private String stagingTableName;
    
    @Column(name = "staging_schema", length = 50)
    private String stagingSchema = "STAGING";
    
    @Column(name = "target_table_name", nullable = false, length = 100)
    private String targetTableName;
    
    @Column(name = "target_schema", length = 50)
    private String targetSchema = "PROD";
    
    // Staging table characteristics
    @Column(name = "staging_type", length = 50)
    private String stagingType = "TEMPORARY"; // TEMPORARY, PERSISTENT, EXTERNAL
    
    @Column(name = "retention_days")
    private Integer retentionDays = 7;
    
    @Column(name = "partition_strategy", length = 50)
    private String partitionStrategy; // DATE, HASH, RANGE, etc.
    
    @Column(name = "partition_column", length = 100)
    private String partitionColumn;
    
    // ETL processing configuration
    @Column(name = "batch_size")
    private Integer batchSize = 10000;
    
    @Column(name = "parallel_degree")
    private Integer parallelDegree = 4;
    
    @Column(name = "validation_level", length = 20)
    private String validationLevel = "STANDARD"; // NONE, BASIC, STANDARD, COMPREHENSIVE
    
    @Column(name = "error_threshold_percent")
    private Integer errorThresholdPercent = 5;
    
    // Data lineage and audit
    @Column(name = "source_system", length = 50)
    private String sourceSystem;
    
    @Column(name = "data_classification", length = 20)
    private String dataClassification = "INTERNAL"; // PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED
    
    @Column(name = "pii_fields", length = 2000)
    private String piiFields; // Comma-separated list of PII field names
    
    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @Column(name = "modified_by", length = 50)
    private String modifiedBy;
    
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
    
    @Column(name = "enabled", length = 1)
    private String enabled = "Y";
    
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (stagingConfigId == null) {
            stagingConfigId = generateStagingConfigId();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
    }
    
    /**
     * Generates a unique staging configuration ID
     */
    private String generateStagingConfigId() {
        return String.format("%s-STAGING", 
            transformationConfig != null ? transformationConfig.getConfigId() : "UNKNOWN"
        );
    }
    
    /**
     * Checks if this staging configuration is currently enabled
     */
    public boolean isEnabled() {
        return "Y".equals(enabled);
    }
    
    /**
     * Determines if this is a temporary staging table
     */
    public boolean isTemporaryStaging() {
        return "TEMPORARY".equals(stagingType);
    }
    
    /**
     * Determines if this is a persistent staging table
     */
    public boolean isPersistentStaging() {
        return "PERSISTENT".equals(stagingType);
    }
    
    /**
     * Determines if this staging table is partitioned
     */
    public boolean isPartitioned() {
        return partitionStrategy != null && !partitionStrategy.trim().isEmpty();
    }
    
    /**
     * Checks if PII fields are configured for this staging table
     */
    public boolean hasPiiFields() {
        return piiFields != null && !piiFields.trim().isEmpty();
    }
    
    /**
     * Gets the full staging table name including schema
     */
    public String getFullStagingTableName() {
        return String.format("%s.%s", stagingSchema, stagingTableName);
    }
    
    /**
     * Gets the full target table name including schema
     */
    public String getFullTargetTableName() {
        return String.format("%s.%s", targetSchema, targetTableName);
    }
    
    /**
     * Gets the configuration ID from the parent transformation config
     */
    public String getConfigId() {
        return transformationConfig != null ? transformationConfig.getConfigId() : null;
    }
}