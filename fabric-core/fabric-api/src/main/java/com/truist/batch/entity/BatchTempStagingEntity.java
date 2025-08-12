package com.truist.batch.entity;

/**
 * Entity for batch temporary staging data
 * Stub implementation to satisfy dependency injection
 */
public class BatchTempStagingEntity {
    
    private String id;
    private String batchId;
    private String transactionType;
    private String status;
    private String data;
    private String createdBy;
    private String createdDate;
    
    // Default constructor
    public BatchTempStagingEntity() {}
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
}