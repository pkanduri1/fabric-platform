package com.fabric.batch.entity;

/**
 * Entity for execution audit records
 * Stub implementation to satisfy dependency injection
 */
public class ExecutionAuditEntity {
    
    private String id;
    private String executionId;
    private String action;
    private String status;
    private String details;
    private String executedBy;
    private String executionDate;
    
    // Default constructor
    public ExecutionAuditEntity() {}
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    
    public String getExecutedBy() { return executedBy; }
    public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }
    
    public String getExecutionDate() { return executionDate; }
    public void setExecutionDate(String executionDate) { this.executionDate = executionDate; }
}