package com.fabric.batch.model;

import java.time.LocalDateTime;

public class ConfigurationAudit {
    
    private Long id;
    private String configId;
    private String action;
    private String oldValue;
    private String newValue;
    private String changedBy;
    private LocalDateTime changeDate;
    private String reason;
    
    public ConfigurationAudit() {
        this.changeDate = LocalDateTime.now();
    }
    
    public ConfigurationAudit(String configId, String action, String reason) {
        this();
        this.configId = configId;
        this.action = action;
        this.reason = reason;
    }
    
    public ConfigurationAudit(String configId, String action, String oldValue, String newValue, String changedBy, String reason) {
        this();
        this.configId = configId;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changedBy = changedBy;
        this.reason = reason;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getConfigId() {
        return configId;
    }
    
    public void setConfigId(String configId) {
        this.configId = configId;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getOldValue() {
        return oldValue;
    }
    
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }
    
    public String getNewValue() {
        return newValue;
    }
    
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
    
    public String getChangedBy() {
        return changedBy;
    }
    
    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
    
    public LocalDateTime getChangeDate() {
        return changeDate;
    }
    
    public void setChangeDate(LocalDateTime changeDate) {
        this.changeDate = changeDate;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}