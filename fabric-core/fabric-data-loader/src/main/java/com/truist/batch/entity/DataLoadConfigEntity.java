package com.truist.batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing data loading configuration with SQL*Loader integration.
 * This entity stores configuration for file-based data loading operations
 * with comprehensive validation, audit, and error handling capabilities.
 */
@Entity
@Table(name = "data_load_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"validationRules", "auditTrails"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DataLoadConfigEntity {
    
    @Id
    @Column(name = "config_id", length = 100)
    private String configId;
    
    @Column(name = "job_name", nullable = false, length = 50)
    private String jobName;
    
    @Column(name = "source_system", nullable = false, length = 50)
    private String sourceSystem;
    
    @Column(name = "target_table", nullable = false, length = 100)
    private String targetTable;
    
    @Column(name = "file_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FileType fileType = FileType.PIPE_DELIMITED;
    
    @Column(name = "file_path_pattern", length = 500)
    private String filePathPattern;
    
    @Column(name = "control_file_template", length = 100)
    private String controlFileTemplate;
    
    @Column(name = "sql_loader_options", length = 1000)
    private String sqlLoaderOptions = "DIRECT=TRUE, ERRORS=1000, SKIP=1";
    
    @Column(name = "field_delimiter", length = 10)
    private String fieldDelimiter = "|";
    
    @Column(name = "record_delimiter", length = 10)
    private String recordDelimiter = "\n";
    
    @Column(name = "header_rows")
    private Integer headerRows = 1;
    
    @Column(name = "trailer_rows")
    private Integer trailerRows = 0;
    
    @Column(name = "max_errors")
    private Integer maxErrors = 1000;
    
    @Column(name = "parallel_degree")
    private Integer parallelDegree = 1;
    
    @Column(name = "validation_enabled", length = 1)
    private String validationEnabled = "Y";
    
    @Column(name = "pre_load_validation", length = 1)
    private String preLoadValidation = "Y";
    
    @Column(name = "post_load_validation", length = 1)
    private String postLoadValidation = "Y";
    
    @Column(name = "backup_enabled", length = 1)
    private String backupEnabled = "Y";
    
    @Column(name = "backup_path", length = 500)
    private String backupPath;
    
    @Column(name = "archive_enabled", length = 1)
    private String archiveEnabled = "Y";
    
    @Column(name = "archive_path", length = 500)
    private String archivePath;
    
    @Column(name = "encryption_required", length = 1)
    private String encryptionRequired = "N";
    
    @Column(name = "pii_data", length = 1)
    private String piiData = "N";
    
    @Column(name = "data_classification", length = 20)
    @Enumerated(EnumType.STRING)
    private DataClassification dataClassification = DataClassification.INTERNAL;
    
    @Column(name = "retention_days")
    private Integer retentionDays = 2555; // 7 years default for financial data
    
    @Column(name = "notification_emails", length = 1000)
    private String notificationEmails;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @Column(name = "modified_by", length = 50)
    private String modifiedBy;
    
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
    
    @Column(name = "version")
    private Integer version = 1;
    
    @Column(name = "enabled", length = 1)
    private String enabled = "Y";
    
    // Relationships
    @OneToMany(mappedBy = "dataLoadConfig", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ValidationRuleEntity> validationRules;
    
    @OneToMany(mappedBy = "dataLoadConfig", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProcessingJobEntity> processingJobs;
    
    @OneToMany(mappedBy = "configId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DataLoadAuditEntity> auditTrails;
    
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (configId == null) {
            configId = sourceSystem + "-" + jobName + "-" + System.currentTimeMillis();
        }
        if (version == null) {
            version = 1;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
        if (version != null) {
            version++;
        }
    }
    
    // Enums
    public enum FileType {
        PIPE_DELIMITED,
        COMMA_DELIMITED,
        TAB_DELIMITED,
        FIXED_WIDTH,
        JSON,
        XML,
        EXCEL
    }
    
    public enum DataClassification {
        PUBLIC,
        INTERNAL,
        CONFIDENTIAL,
        RESTRICTED,
        TOP_SECRET
    }
    
    // Utility methods
    public boolean isValidationEnabled() {
        return "Y".equalsIgnoreCase(validationEnabled);
    }
    
    public boolean isPreLoadValidationEnabled() {
        return "Y".equalsIgnoreCase(preLoadValidation);
    }
    
    public boolean isPostLoadValidationEnabled() {
        return "Y".equalsIgnoreCase(postLoadValidation);
    }
    
    public boolean isBackupEnabled() {
        return "Y".equalsIgnoreCase(backupEnabled);
    }
    
    public boolean isArchiveEnabled() {
        return "Y".equalsIgnoreCase(archiveEnabled);
    }
    
    public boolean isEncryptionRequired() {
        return "Y".equalsIgnoreCase(encryptionRequired);
    }
    
    public boolean containsPiiData() {
        return "Y".equalsIgnoreCase(piiData);
    }
    
    public boolean isEnabled() {
        return "Y".equalsIgnoreCase(enabled);
    }
}