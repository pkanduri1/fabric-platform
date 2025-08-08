package com.truist.batch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating new source systems in US002 Template Configuration Enhancement
 */
public class CreateSourceSystemRequest {
    
    @NotBlank(message = "System ID is required")
    @Size(max = 50, message = "System ID must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "System ID must contain only uppercase letters, numbers, and underscores")
    private String id;

    @NotBlank(message = "System name is required")
    @Size(max = 100, message = "System name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "System type is required")
    @Pattern(regexp = "^(ORACLE|SQLSERVER|FILE|API)$", message = "System type must be one of: ORACLE, SQLSERVER, FILE, API")
    private String type;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 1000, message = "Connection string must not exceed 1000 characters")
    private String connectionString;

    private boolean enabled = true;

    // Constructors
    public CreateSourceSystemRequest() {}

    public CreateSourceSystemRequest(String id, String name, String type, String description) {
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
        this.id = id != null ? id.toUpperCase() : null;
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

    @Override
    public String toString() {
        return "CreateSourceSystemRequest{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", description='" + description + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}