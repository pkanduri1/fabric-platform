package com.fabric.batch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for saving template-source field mappings in US002 Template Configuration Enhancement
 */
public class TemplateSourceMappingRequest {
    
    @NotBlank(message = "File type is required")
    @Size(max = 50, message = "File type must not exceed 50 characters")
    private String fileType;

    @NotBlank(message = "Transaction type is required")
    @Size(max = 10, message = "Transaction type must not exceed 10 characters")
    private String transactionType;

    @NotBlank(message = "Source system ID is required")
    @Size(max = 50, message = "Source system ID must not exceed 50 characters")
    private String sourceSystemId;

    @NotBlank(message = "Job name is required")
    @Size(max = 100, message = "Job name must not exceed 100 characters")
    private String jobName;

    @NotEmpty(message = "Field mappings are required")
    @Valid
    private List<FieldMappingDto> fieldMappings;

    @NotBlank(message = "Created by is required")
    @Size(max = 50, message = "Created by must not exceed 50 characters")
    private String createdBy;

    // Constructors
    public TemplateSourceMappingRequest() {}

    public TemplateSourceMappingRequest(String fileType, String transactionType, String sourceSystemId, 
                                      String jobName, List<FieldMappingDto> fieldMappings, String createdBy) {
        this.fileType = fileType;
        this.transactionType = transactionType;
        this.sourceSystemId = sourceSystemId;
        this.jobName = jobName;
        this.fieldMappings = fieldMappings;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getSourceSystemId() {
        return sourceSystemId;
    }

    public void setSourceSystemId(String sourceSystemId) {
        this.sourceSystemId = sourceSystemId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public List<FieldMappingDto> getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(List<FieldMappingDto> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public String toString() {
        return "TemplateSourceMappingRequest{" +
                "fileType='" + fileType + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", sourceSystemId='" + sourceSystemId + '\'' +
                ", jobName='" + jobName + '\'' +
                ", fieldMappings=" + (fieldMappings != null ? fieldMappings.size() + " mappings" : "null") +
                ", createdBy='" + createdBy + '\'' +
                '}';
    }
}