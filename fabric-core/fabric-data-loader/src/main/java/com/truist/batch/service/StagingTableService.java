package com.truist.batch.service;

import java.util.List;
import java.util.Map;

import com.truist.batch.entity.StagingTableConfigEntity;
import com.truist.batch.entity.TransformationConfigEntity;

/**
 * Service interface for managing staging table operations in the ETL pipeline.
 * 
 * Provides comprehensive staging table management including DDL generation,
 * data loading, transformation execution, and cleanup operations.
 */
public interface StagingTableService {
    
    /**
     * Creates or updates staging table based on transformation configuration
     * 
     * @param transformationConfig The transformation configuration
     * @return The staging table configuration
     */
    StagingTableConfigEntity createOrUpdateStagingTable(TransformationConfigEntity transformationConfig);
    
    /**
     * Generates DDL for staging table based on field transformation rules
     * 
     * @param configId The transformation configuration ID
     * @return DDL statement for creating the staging table
     */
    String generateStagingTableDDL(String configId);
    
    /**
     * Executes DDL to create or alter staging table
     * 
     * @param ddlStatement The DDL statement to execute
     * @param stagingTableName The staging table name
     * @return True if successful, false otherwise
     */
    boolean executeStagingTableDDL(String ddlStatement, String stagingTableName);
    
    /**
     * Loads raw data into staging table
     * 
     * @param configId The transformation configuration ID
     * @param sourceData The source data to load
     * @param correlationId The correlation ID for tracking
     * @return Number of records loaded
     */
    long loadDataToStaging(String configId, List<Map<String, Object>> sourceData, String correlationId);
    
    /**
     * Executes transformations on staging table data
     * 
     * @param configId The transformation configuration ID
     * @param correlationId The correlation ID for tracking
     * @return Transformation execution result
     */
    StagingTransformationResult executeTransformations(String configId, String correlationId);
    
    /**
     * Validates staging table data against transformation rules
     * 
     * @param configId The transformation configuration ID
     * @param correlationId The correlation ID for tracking
     * @return Validation result summary
     */
    StagingValidationResult validateStagingData(String configId, String correlationId);
    
    /**
     * Moves validated data from staging to target table
     * 
     * @param configId The transformation configuration ID
     * @param correlationId The correlation ID for tracking
     * @return Number of records transferred
     */
    long transferToTarget(String configId, String correlationId);
    
    /**
     * Cleans up staging table based on retention policy
     * 
     * @param configId The transformation configuration ID
     * @return Number of records cleaned up
     */
    long cleanupStagingData(String configId);
    
    /**
     * Gets staging table statistics
     * 
     * @param configId The transformation configuration ID
     * @return Staging table statistics
     */
    StagingTableStatistics getStagingTableStatistics(String configId);
    
    /**
     * Checks if staging table exists and is properly configured
     * 
     * @param configId The transformation configuration ID
     * @return True if staging table is ready, false otherwise
     */
    boolean isStagingTableReady(String configId);
    
    /**
     * Archives staging table data before cleanup
     * 
     * @param configId The transformation configuration ID
     * @param correlationId The correlation ID for tracking
     * @return Archive operation result
     */
    StagingArchiveResult archiveStagingData(String configId, String correlationId);
    
    /**
     * Truncates staging table for fresh load
     * 
     * @param configId The transformation configuration ID
     * @return True if successful, false otherwise
     */
    boolean truncateStagingTable(String configId);
    
    /**
     * Gets staging table configuration by config ID
     * 
     * @param configId The transformation configuration ID
     * @return Staging table configuration
     */
    StagingTableConfigEntity getStagingTableConfig(String configId);
    
    /**
     * Updates staging table configuration
     * 
     * @param stagingConfig The staging table configuration to update
     * @return Updated staging table configuration
     */
    StagingTableConfigEntity updateStagingTableConfig(StagingTableConfigEntity stagingConfig);
    
    /**
     * Drops staging table (permanent deletion)
     * 
     * @param configId The transformation configuration ID
     * @return True if successful, false otherwise
     */
    boolean dropStagingTable(String configId);
    
    /**
     * Result class for staging transformation operations
     */
    public static class StagingTransformationResult {
        private long recordsProcessed;
        private long recordsTransformed;
        private long recordsFailed;
        private List<String> errors;
        private List<String> warnings;
        private long executionTimeMs;
        
        // Constructors, getters, and setters
        public StagingTransformationResult() {}
        
        public StagingTransformationResult(long recordsProcessed, long recordsTransformed, 
                                         long recordsFailed, List<String> errors, 
                                         List<String> warnings, long executionTimeMs) {
            this.recordsProcessed = recordsProcessed;
            this.recordsTransformed = recordsTransformed;
            this.recordsFailed = recordsFailed;
            this.errors = errors;
            this.warnings = warnings;
            this.executionTimeMs = executionTimeMs;
        }
        
        // Getters and setters
        public long getRecordsProcessed() { return recordsProcessed; }
        public void setRecordsProcessed(long recordsProcessed) { this.recordsProcessed = recordsProcessed; }
        
        public long getRecordsTransformed() { return recordsTransformed; }
        public void setRecordsTransformed(long recordsTransformed) { this.recordsTransformed = recordsTransformed; }
        
        public long getRecordsFailed() { return recordsFailed; }
        public void setRecordsFailed(long recordsFailed) { this.recordsFailed = recordsFailed; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
        
        public long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    }
    
    /**
     * Result class for staging validation operations
     */
    public static class StagingValidationResult {
        private long recordsValidated;
        private long recordsPassed;
        private long recordsFailed;
        private List<String> validationErrors;
        private double dataQualityScore;
        
        // Constructors, getters, and setters
        public StagingValidationResult() {}
        
        public StagingValidationResult(long recordsValidated, long recordsPassed, 
                                     long recordsFailed, List<String> validationErrors, 
                                     double dataQualityScore) {
            this.recordsValidated = recordsValidated;
            this.recordsPassed = recordsPassed;
            this.recordsFailed = recordsFailed;
            this.validationErrors = validationErrors;
            this.dataQualityScore = dataQualityScore;
        }
        
        // Getters and setters
        public long getRecordsValidated() { return recordsValidated; }
        public void setRecordsValidated(long recordsValidated) { this.recordsValidated = recordsValidated; }
        
        public long getRecordsPassed() { return recordsPassed; }
        public void setRecordsPassed(long recordsPassed) { this.recordsPassed = recordsPassed; }
        
        public long getRecordsFailed() { return recordsFailed; }
        public void setRecordsFailed(long recordsFailed) { this.recordsFailed = recordsFailed; }
        
        public List<String> getValidationErrors() { return validationErrors; }
        public void setValidationErrors(List<String> validationErrors) { this.validationErrors = validationErrors; }
        
        public double getDataQualityScore() { return dataQualityScore; }
        public void setDataQualityScore(double dataQualityScore) { this.dataQualityScore = dataQualityScore; }
    }
    
    /**
     * Statistics class for staging table monitoring
     */
    public static class StagingTableStatistics {
        private String configId;
        private String stagingTableName;
        private long totalRecords;
        private long validRecords;
        private long invalidRecords;
        private double tableSizeMB;
        private String lastUpdateTime;
        private int retentionDays;
        
        // Constructors, getters, and setters
        public StagingTableStatistics() {}
        
        // Getters and setters
        public String getConfigId() { return configId; }
        public void setConfigId(String configId) { this.configId = configId; }
        
        public String getStagingTableName() { return stagingTableName; }
        public void setStagingTableName(String stagingTableName) { this.stagingTableName = stagingTableName; }
        
        public long getTotalRecords() { return totalRecords; }
        public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }
        
        public long getValidRecords() { return validRecords; }
        public void setValidRecords(long validRecords) { this.validRecords = validRecords; }
        
        public long getInvalidRecords() { return invalidRecords; }
        public void setInvalidRecords(long invalidRecords) { this.invalidRecords = invalidRecords; }
        
        public double getTableSizeMB() { return tableSizeMB; }
        public void setTableSizeMB(double tableSizeMB) { this.tableSizeMB = tableSizeMB; }
        
        public String getLastUpdateTime() { return lastUpdateTime; }
        public void setLastUpdateTime(String lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
        
        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    }
    
    /**
     * Result class for staging archive operations
     */
    public static class StagingArchiveResult {
        private long recordsArchived;
        private String archiveLocation;
        private String archiveFormat;
        private boolean encryptionApplied;
        private long archiveSizeMB;
        
        // Constructors, getters, and setters
        public StagingArchiveResult() {}
        
        // Getters and setters
        public long getRecordsArchived() { return recordsArchived; }
        public void setRecordsArchived(long recordsArchived) { this.recordsArchived = recordsArchived; }
        
        public String getArchiveLocation() { return archiveLocation; }
        public void setArchiveLocation(String archiveLocation) { this.archiveLocation = archiveLocation; }
        
        public String getArchiveFormat() { return archiveFormat; }
        public void setArchiveFormat(String archiveFormat) { this.archiveFormat = archiveFormat; }
        
        public boolean isEncryptionApplied() { return encryptionApplied; }
        public void setEncryptionApplied(boolean encryptionApplied) { this.encryptionApplied = encryptionApplied; }
        
        public long getArchiveSizeMB() { return archiveSizeMB; }
        public void setArchiveSizeMB(long archiveSizeMB) { this.archiveSizeMB = archiveSizeMB; }
    }
}