package com.truist.batch.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for CM3INT.TEMPLATE_SOURCE_MAPPINGS table
 * Used for US002 Template Configuration Enhancement - Field-level mappings between templates and source systems
 */
@Entity
@Table(name = "TEMPLATE_SOURCE_MAPPINGS", schema = "CM3INT")
public class TemplateSourceMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "FILE_TYPE", length = 50, nullable = false)
    private String fileType;

    @Column(name = "TRANSACTION_TYPE", length = 10, nullable = false)
    private String transactionType;

    @Column(name = "SOURCE_SYSTEM_ID", length = 50, nullable = false)
    private String sourceSystemId;

    @Column(name = "JOB_NAME", length = 100, nullable = false)
    private String jobName;

    @Column(name = "TARGET_FIELD_NAME", length = 50, nullable = false)
    private String targetFieldName;

    @Column(name = "SOURCE_FIELD_NAME", length = 100)
    private String sourceFieldName;

    @Column(name = "TRANSFORMATION_TYPE", length = 20)
    private String transformationType = "source";

    @Column(name = "TRANSFORMATION_CONFIG", length = 1000)
    private String transformationConfig;

    @Column(name = "TARGET_POSITION")
    private Integer targetPosition;

    @Column(name = "LENGTH")
    private Integer length;

    @Column(name = "DATA_TYPE", length = 20)
    private String dataType;

    @Column(name = "CREATED_BY", length = 50, nullable = false)
    private String createdBy;

    @Column(name = "CREATED_DATE", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "MODIFIED_BY", length = 50)
    private String modifiedBy;

    @Column(name = "MODIFIED_DATE")
    private LocalDateTime modifiedDate;

    @Column(name = "VERSION")
    private Integer version = 1;

    @Column(name = "ENABLED", length = 1)
    private String enabled = "Y";

    // Foreign key relationships (optional, for reference)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SOURCE_SYSTEM_ID", referencedColumnName = "ID", insertable = false, updatable = false)
    private SourceSystemEntity sourceSystem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_TYPE", referencedColumnName = "FILE_TYPE", insertable = false, updatable = false)
    private FileTypeTemplateEntity fileTypeTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "FILE_TYPE", referencedColumnName = "FILE_TYPE", insertable = false, updatable = false),
        @JoinColumn(name = "TRANSACTION_TYPE", referencedColumnName = "TRANSACTION_TYPE", insertable = false, updatable = false),
        @JoinColumn(name = "TARGET_FIELD_NAME", referencedColumnName = "FIELD_NAME", insertable = false, updatable = false)
    })
    private FieldTemplateEntity fieldTemplate;

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

    // JPA lifecycle methods
    @PreUpdate
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

    public SourceSystemEntity getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(SourceSystemEntity sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public FileTypeTemplateEntity getFileTypeTemplate() {
        return fileTypeTemplate;
    }

    public void setFileTypeTemplate(FileTypeTemplateEntity fileTypeTemplate) {
        this.fileTypeTemplate = fileTypeTemplate;
    }

    public FieldTemplateEntity getFieldTemplate() {
        return fieldTemplate;
    }

    public void setFieldTemplate(FieldTemplateEntity fieldTemplate) {
        this.fieldTemplate = fieldTemplate;
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
        return "TemplateSourceMappingEntity{" +
                "id=" + id +
                ", fileType='" + fileType + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", sourceSystemId='" + sourceSystemId + '\'' +
                ", jobName='" + jobName + '\'' +
                ", targetFieldName='" + targetFieldName + '\'' +
                ", sourceFieldName='" + sourceFieldName + '\'' +
                ", transformationType='" + transformationType + '\'' +
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