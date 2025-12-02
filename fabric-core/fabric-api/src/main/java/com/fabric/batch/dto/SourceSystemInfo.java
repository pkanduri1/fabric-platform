package com.fabric.batch.dto;

import java.time.LocalDateTime;

/**
 * DTO for Source System information used in US002 Template Configuration Enhancement
 */
public class SourceSystemInfo {
    private String id;
    private String name;
    private String type;
    private String description;
    private String connectionString;
    private boolean enabled;
    private LocalDateTime createdDate;
    private Integer jobCount;

    // Constructors
    public SourceSystemInfo() {}

    public SourceSystemInfo(String id, String name, String type, String description, 
                          boolean enabled, LocalDateTime createdDate, Integer jobCount) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.enabled = enabled;
        this.createdDate = createdDate;
        this.jobCount = jobCount;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Integer getJobCount() {
        return jobCount;
    }

    public void setJobCount(Integer jobCount) {
        this.jobCount = jobCount;
    }

    @Override
    public String toString() {
        return "SourceSystemInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", enabled=" + enabled +
                ", jobCount=" + jobCount +
                '}';
    }
}