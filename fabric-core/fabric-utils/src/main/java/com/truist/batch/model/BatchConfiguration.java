package com.truist.batch.model;

import java.time.LocalDateTime;

public class BatchConfiguration {
    
    private String id;
    private String sourceSystem;
    private String jobName;
    private String transactionType;
    private String configurationJson;
    private String enabled;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String filePath;
    private String version;
    private String description;
    private String masterFileSchemaId; // Reference to master file schema
    
    public BatchConfiguration() {
        this.transactionType = "200";
        this.enabled = "Y";
        this.createdDate = LocalDateTime.now();
        this.version = "1";
        this.createdBy = "system";
        this.description = "Auto-generated configuration";
    }
    
    public BatchConfiguration(String sourceSystem, String jobName, String transactionType, String configurationJson) {
        this();
        this.sourceSystem = sourceSystem;
        this.jobName = jobName; 
        this.transactionType = transactionType != null ? transactionType : "200";
        this.configurationJson = configurationJson;
        generateId();
    }
    
    private void generateId() {
        if (sourceSystem != null && jobName != null && transactionType != null) {
            this.id = sourceSystem + "-" + jobName + "-" + transactionType + "-" + System.currentTimeMillis();
        }
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSourceSystem() {
        return sourceSystem;
    }
    
    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
        generateId();
    }
    
    public String getJobName() {
        return jobName;
    }
    
    public void setJobName(String jobName) {
        this.jobName = jobName;
        generateId();
    }
    
    public String getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType != null ? transactionType : "200";
        generateId();
    }
    
    public String getConfigurationJson() {
        return configurationJson;
    }
    
    public void setConfigurationJson(String configurationJson) {
        this.configurationJson = configurationJson;
    }
    
    public String getEnabled() {
        return enabled;
    }
    
    public void setEnabled(String enabled) {
        // Ensure enabled is always Y or N
        this.enabled = (enabled != null && enabled.equalsIgnoreCase("N")) ? "N" : "Y";
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        // Ensure createdBy is not null
        this.createdBy = (createdBy != null && !createdBy.trim().isEmpty()) ? createdBy : "system";
    }
    
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
    
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }
    
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
    
    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }
    
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        // Ensure version is not null and is a valid number string
        this.version = (version != null && !version.trim().isEmpty()) ? version : "1";
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        // Ensure description is not null 
        this.description = (description != null && !description.trim().isEmpty()) ? description : "Configuration for " + (sourceSystem != null ? sourceSystem : "unknown") + "/" + (jobName != null ? jobName : "unknown");
    }
    
    public String getMasterFileSchemaId() {
        return masterFileSchemaId;
    }
    
    public void setMasterFileSchemaId(String masterFileSchemaId) {
        this.masterFileSchemaId = masterFileSchemaId;
    }
}