package com.truist.batch.entity;

/**
 * Entity for batch processing status
 * Stub implementation to satisfy dependency injection
 */
public class BatchProcessingStatusEntity {
    
    private String id;
    private String batchId;
    private String status;
    private String message;
    private String createdBy;
    private String createdDate;
    
    // Default constructor
    public BatchProcessingStatusEntity() {}
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
}