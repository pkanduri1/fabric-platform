package com.fabric.batch.dto;

import java.time.LocalDateTime;

/**
 * DTO for Source System with usage information for template configuration
 * Used in US002 Template Configuration Enhancement
 */
public class SourceSystemWithUsage {
    private String id;
    private String name;
    private String type;
    private String description;
    private boolean enabled;
    private Integer jobCount;
    
    // Usage-specific fields
    private boolean hasExistingConfiguration;
    private String existingJobName;
    private LocalDateTime lastConfigured;
    private Integer templateCount;

    // Constructors
    public SourceSystemWithUsage() {}

    public SourceSystemWithUsage(String id, String name, String type, String description, 
                                boolean enabled, Integer jobCount) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.enabled = enabled;
        this.jobCount = jobCount;
    }

    // Static factory method for creating from SourceSystemInfo
    public static SourceSystemWithUsage fromSourceSystemInfo(SourceSystemInfo sourceSystem) {
        SourceSystemWithUsage usage = new SourceSystemWithUsage();
        usage.setId(sourceSystem.getId());
        usage.setName(sourceSystem.getName());
        usage.setType(sourceSystem.getType());
        usage.setDescription(sourceSystem.getDescription());
        usage.setEnabled(sourceSystem.isEnabled());
        usage.setJobCount(sourceSystem.getJobCount());
        return usage;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getJobCount() {
        return jobCount;
    }

    public void setJobCount(Integer jobCount) {
        this.jobCount = jobCount;
    }

    public boolean isHasExistingConfiguration() {
        return hasExistingConfiguration;
    }

    public void setHasExistingConfiguration(boolean hasExistingConfiguration) {
        this.hasExistingConfiguration = hasExistingConfiguration;
    }

    public String getExistingJobName() {
        return existingJobName;
    }

    public void setExistingJobName(String existingJobName) {
        this.existingJobName = existingJobName;
    }

    public LocalDateTime getLastConfigured() {
        return lastConfigured;
    }

    public void setLastConfigured(LocalDateTime lastConfigured) {
        this.lastConfigured = lastConfigured;
    }

    public Integer getTemplateCount() {
        return templateCount;
    }

    public void setTemplateCount(Integer templateCount) {
        this.templateCount = templateCount;
    }

    @Override
    public String toString() {
        return "SourceSystemWithUsage{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", enabled=" + enabled +
                ", hasExistingConfiguration=" + hasExistingConfiguration +
                ", existingJobName='" + existingJobName + '\'' +
                ", templateCount=" + templateCount +
                '}';
    }
}