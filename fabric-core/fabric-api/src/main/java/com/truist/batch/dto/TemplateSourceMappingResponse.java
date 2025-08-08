package com.truist.batch.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for template-source field mappings in US002 Template Configuration Enhancement
 */
public class TemplateSourceMappingResponse {
    
    private String fileType;
    private String transactionType;
    private String sourceSystemId;
    private String sourceSystemName;
    private String jobName;
    private List<FieldMappingDto> fieldMappings;
    private int mappingCount;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
    private boolean success;
    private String message;

    // Constructors
    public TemplateSourceMappingResponse() {}

    public TemplateSourceMappingResponse(String fileType, String transactionType, String sourceSystemId, 
                                       String jobName, List<FieldMappingDto> fieldMappings) {
        this.fileType = fileType;
        this.transactionType = transactionType;
        this.sourceSystemId = sourceSystemId;
        this.jobName = jobName;
        this.fieldMappings = fieldMappings;
        this.mappingCount = fieldMappings != null ? fieldMappings.size() : 0;
        this.success = true;
    }

    // Static factory methods
    public static TemplateSourceMappingResponse success(String fileType, String transactionType, 
                                                       String sourceSystemId, String jobName, 
                                                       List<FieldMappingDto> fieldMappings, String message) {
        TemplateSourceMappingResponse response = new TemplateSourceMappingResponse(
                fileType, transactionType, sourceSystemId, jobName, fieldMappings);
        response.setMessage(message);
        return response;
    }

    public static TemplateSourceMappingResponse error(String message) {
        TemplateSourceMappingResponse response = new TemplateSourceMappingResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
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

    public String getSourceSystemName() {
        return sourceSystemName;
    }

    public void setSourceSystemName(String sourceSystemName) {
        this.sourceSystemName = sourceSystemName;
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
        this.mappingCount = fieldMappings != null ? fieldMappings.size() : 0;
    }

    public int getMappingCount() {
        return mappingCount;
    }

    public void setMappingCount(int mappingCount) {
        this.mappingCount = mappingCount;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "TemplateSourceMappingResponse{" +
                "fileType='" + fileType + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", sourceSystemId='" + sourceSystemId + '\'' +
                ", sourceSystemName='" + sourceSystemName + '\'' +
                ", jobName='" + jobName + '\'' +
                ", mappingCount=" + mappingCount +
                ", success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}