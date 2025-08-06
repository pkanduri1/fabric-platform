package com.truist.batch.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for existing CM3INT.SOURCE_SYSTEMS table
 * Used for US002 Template Configuration Enhancement - Source System Management
 */
@Entity
@Table(name = "SOURCE_SYSTEMS", schema = "CM3INT")
public class SourceSystemEntity {

    @Id
    @Column(name = "ID", length = 50)
    private String id;

    @Column(name = "NAME", length = 100, nullable = false)
    private String name;

    @Column(name = "TYPE", length = 20, nullable = false)
    private String type;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "CONNECTION_STRING", length = 1000)
    private String connectionString;

    @Column(name = "ENABLED", length = 1)
    private String enabled = "Y";

    @Column(name = "CREATED_DATE", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "JOB_COUNT")
    private Integer jobCount = 0;

    // Constructors
    public SourceSystemEntity() {
        this.createdDate = LocalDateTime.now();
    }

    public SourceSystemEntity(String id, String name, String type, String description) {
        this();
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
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

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
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

    // Utility methods
    public boolean isEnabled() {
        return "Y".equals(this.enabled);
    }

    public void enable() {
        this.enabled = "Y";
    }

    public void disable() {
        this.enabled = "N";
    }

    // toString for debugging
    @Override
    public String toString() {
        return "SourceSystemEntity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", description='" + description + '\'' +
                ", enabled='" + enabled + '\'' +
                ", jobCount=" + jobCount +
                '}';
    }

    // equals and hashCode based on ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SourceSystemEntity)) return false;
        SourceSystemEntity that = (SourceSystemEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}