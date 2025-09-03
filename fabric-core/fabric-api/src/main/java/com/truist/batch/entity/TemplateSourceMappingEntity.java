package com.truist.batch.entity;

import java.time.LocalDateTime;

/**
 * POJO for CM3INT.TEMPLATE_SOURCE_MAPPINGS table
 * Used for US002 Template Configuration Enhancement - Field-level mappings between templates and source systems
 * 
 * Converted from JPA Entity to simple POJO to eliminate JPA dependencies
 */
public class TemplateSourceMappingEntity {

    private Long id;
    private String fileType;
    private String transactionType;
    private String sourceSystemId;
    private String jobName;
    private String targetFieldName;
    private String sourceFieldName;
    private String transformationType;
    private String transformationConfig;
    private String value;
    private String defaultValue;
    private Integer targetPosition;
    private Integer length;
    private String dataType;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
    private Integer version = 1;
    private String enabled = "Y";

    // Foreign key relationships removed for POJO conversion
    // These can be handled by separate service methods when needed

    // Constructors
    public TemplateSourceMappingEntity() {
        this.createdDate = LocalDateTime.now();
    }

    public TemplateSourceMappingEntity(String fileType, String transactionType, String sourceSystemId, 
                                     String jobName, String targetFieldName, String sourceFieldName, 
                                     String createdBy) {
        this();
        this.fileType = fileType;
        this.transactionType = transactionType;
        this.sourceSystemId = sourceSystemId;
        this.jobName = jobName;
        this.targetFieldName = targetFieldName;
        this.sourceFieldName = sourceFieldName;
        this.createdBy = createdBy;
    }

    // Lifecycle methods (previously JPA callbacks)
    public void preUpdate() {
        this.modifiedDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getTargetFieldName() {
        return targetFieldName;
    }

    public void setTargetFieldName(String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    public String getSourceFieldName() {
        return sourceFieldName;
    }

    public void setSourceFieldName(String sourceFieldName) {
        this.sourceFieldName = sourceFieldName;
    }

    public String getTransformationType() {
        return transformationType;
    }

    public void setTransformationType(String transformationType) {
        this.transformationType = transformationType;
    }

    public String getTransformationConfig() {
        return transformationConfig;
    }

    public void setTransformationConfig(String transformationConfig) {
        this.transformationConfig = transformationConfig;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Integer getTargetPosition() {
        return targetPosition;
    }

    public void setTargetPosition(Integer targetPosition) {
        this.targetPosition = targetPosition;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    // Removed relationship getters/setters for POJO conversion

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
        return "TemplateSourceMappingEntity{" +
                "id=" + id +
                ", fileType='" + fileType + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", sourceSystemId='" + sourceSystemId + '\'' +
                ", jobName='" + jobName + '\'' +
                ", targetFieldName='" + targetFieldName + '\'' +
                ", sourceFieldName='" + sourceFieldName + '\'' +
                ", transformationType='" + transformationType + '\'' +
                ", value='" + value + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", enabled='" + enabled + '\'' +
                '}';
    }

    // equals and hashCode based on business key
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemplateSourceMappingEntity)) return false;
        TemplateSourceMappingEntity that = (TemplateSourceMappingEntity) o;
        return fileType != null && fileType.equals(that.fileType) &&
               transactionType != null && transactionType.equals(that.transactionType) &&
               sourceSystemId != null && sourceSystemId.equals(that.sourceSystemId) &&
               targetFieldName != null && targetFieldName.equals(that.targetFieldName);
    }

    @Override
    public int hashCode() {
        int result = fileType != null ? fileType.hashCode() : 0;
        result = 31 * result + (transactionType != null ? transactionType.hashCode() : 0);
        result = 31 * result + (sourceSystemId != null ? sourceSystemId.hashCode() : 0);
        result = 31 * result + (targetFieldName != null ? targetFieldName.hashCode() : 0);
        return result;
    }
}