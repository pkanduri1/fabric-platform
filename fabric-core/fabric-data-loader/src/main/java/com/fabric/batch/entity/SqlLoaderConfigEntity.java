package com.fabric.batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing SQL*Loader configuration with comprehensive security,
 * performance, and compliance features for enterprise fintech environments.
 * 
 * This entity manages SQL*Loader control file generation, execution parameters,
 * security settings, and regulatory compliance requirements.
 */
@Entity
@Table(name = "sql_loader_configs", schema = "CM3INT")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"fieldConfigs", "executions", "securityAudits"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SqlLoaderConfigEntity {
    
    @Id
    @Column(name = "config_id", length = 100)
    private String configId;
    
    @Column(name = "job_name", nullable = false, length = 50)
    private String jobName;
    
    @Column(name = "source_system", nullable = false, length = 50)
    private String sourceSystem;
    
    @Column(name = "target_table", nullable = false, length = 100)
    private String targetTable;
    
    @Column(name = "control_file_template", columnDefinition = "CLOB")
    @Lob
    private String controlFileTemplate;
    
    @Column(name = "load_method", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LoadMethod loadMethod = LoadMethod.INSERT;
    
    @Column(name = "direct_path", length = 1)
    @Builder.Default
    private String directPath = "Y";
    
    @Column(name = "parallel_degree")
    @Builder.Default
    private Integer parallelDegree = 1;
    
    @Column(name = "bind_size")
    @Builder.Default
    private Long bindSize = 256000L;
    
    @Column(name = "read_size")
    @Builder.Default
    private Long readSize = 1048576L;
    
    @Column(name = "max_errors")
    @Builder.Default
    private Integer maxErrors = 1000;
    
    @Column(name = "skip_rows")
    @Builder.Default
    private Integer skipRows = 1;
    
    @Column(name = "rows_per_commit")
    @Builder.Default
    private Integer rowsPerCommit = 64;
    
    @Column(name = "field_delimiter", length = 10)
    @Builder.Default
    private String fieldDelimiter = "|";
    
    @Column(name = "record_delimiter", length = 10)
    @Builder.Default
    private String recordDelimiter = "\n";
    
    @Column(name = "string_delimiter", length = 5)
    @Builder.Default
    private String stringDelimiter = "\"";
    
    @Column(name = "character_set", length = 20)
    @Builder.Default
    private String characterSet = "UTF8";
    
    @Column(name = "date_format", length = 50)
    @Builder.Default
    private String dateFormat = "YYYY-MM-DD HH24:MI:SS";
    
    @Column(name = "timestamp_format", length = 50)
    @Builder.Default
    private String timestampFormat = "YYYY-MM-DD HH24:MI:SS.FF";
    
    @Column(name = "null_if", length = 20)
    @Builder.Default
    private String nullIf = "BLANKS";
    
    @Column(name = "trim_whitespace", length = 1)
    @Builder.Default
    private String trimWhitespace = "Y";
    
    @Column(name = "optional_enclosures", length = 1)
    @Builder.Default
    private String optionalEnclosures = "Y";
    
    @Column(name = "resumable", length = 1)
    @Builder.Default
    private String resumable = "Y";
    
    @Column(name = "resumable_timeout")
    @Builder.Default
    private Integer resumableTimeout = 7200;
    
    @Column(name = "resumable_name", length = 100)
    private String resumableName;
    
    @Column(name = "stream_size")
    @Builder.Default
    private Long streamSize = 256000L;
    
    @Column(name = "silent_mode", length = 1)
    @Builder.Default
    private String silentMode = "N";
    
    @Column(name = "continue_load", length = 1)
    @Builder.Default
    private String continueLoad = "N";
    
    // Security and Compliance Fields
    @Column(name = "encryption_required", length = 1)
    @Builder.Default
    private String encryptionRequired = "N";
    
    @Column(name = "encryption_algorithm", length = 20)
    @Builder.Default
    private String encryptionAlgorithm = "AES256";
    
    @Column(name = "encryption_key_id", length = 100)
    private String encryptionKeyId;
    
    @Column(name = "audit_trail_required", length = 1)
    @Builder.Default
    private String auditTrailRequired = "Y";
    
    @Column(name = "pre_execution_sql", columnDefinition = "CLOB")
    @Lob
    private String preExecutionSql;
    
    @Column(name = "post_execution_sql", columnDefinition = "CLOB")
    @Lob
    private String postExecutionSql;
    
    @Column(name = "custom_options", length = 2000)
    private String customOptions;
    
    @Column(name = "validation_enabled", length = 1)
    @Builder.Default
    private String validationEnabled = "Y";
    
    @Column(name = "data_classification", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DataClassification dataClassification = DataClassification.INTERNAL;
    
    @Column(name = "pii_fields", length = 1000)
    private String piiFields; // Comma-separated list of PII field names
    
    @Column(name = "regulatory_compliance", length = 100)
    private String regulatoryCompliance; // SOX, PCI-DSS, GDPR, etc.
    
    @Column(name = "retention_days")
    @Builder.Default
    private Integer retentionDays = 2555; // 7 years default for financial data
    
    @Column(name = "notification_emails", length = 1000)
    private String notificationEmails;
    
    @Column(name = "description", length = 2000)
    private String description;
    
    // Standard audit fields
    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @Column(name = "modified_by", length = 50)
    private String modifiedBy;
    
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
    
    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;
    
    @Column(name = "enabled", length = 1)
    @Builder.Default
    private String enabled = "Y";
    
    // Relationships
    @OneToMany(mappedBy = "sqlLoaderConfig", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("fieldOrder ASC")
    private List<SqlLoaderFieldConfigEntity> fieldConfigs;
    
    @OneToMany(mappedBy = "sqlLoaderConfig", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SqlLoaderExecutionEntity> executions;
    
    @OneToMany(mappedBy = "sqlLoaderConfig", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SqlLoaderSecurityAuditEntity> securityAudits;
    
    // JPA lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (configId == null) {
            configId = sourceSystem.toUpperCase() + "-" + jobName.toUpperCase() + "-SQLLOADER-" + 
                      String.format("%03d", System.currentTimeMillis() % 1000);
        }
        if (version == null) {
            version = 1;
        }
        if (resumableName == null && resumable != null && "Y".equalsIgnoreCase(resumable)) {
            resumableName = "FABRIC_SQLLOADER_" + configId;
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
    public enum LoadMethod {
        INSERT,
        APPEND,
        REPLACE,
        TRUNCATE
    }
    
    public enum DataClassification {
        PUBLIC,
        INTERNAL,
        CONFIDENTIAL,
        RESTRICTED,
        TOP_SECRET
    }
    
    // Utility methods for boolean checks
    public boolean isDirectPathEnabled() {
        return "Y".equalsIgnoreCase(directPath);
    }
    
    public boolean isTrimWhitespaceEnabled() {
        return "Y".equalsIgnoreCase(trimWhitespace);
    }
    
    public boolean isOptionalEnclosuresEnabled() {
        return "Y".equalsIgnoreCase(optionalEnclosures);
    }
    
    public boolean isResumableEnabled() {
        return "Y".equalsIgnoreCase(resumable);
    }
    
    public boolean isSilentModeEnabled() {
        return "Y".equalsIgnoreCase(silentMode);
    }
    
    public boolean isContinueLoadEnabled() {
        return "Y".equalsIgnoreCase(continueLoad);
    }
    
    public boolean isEncryptionRequired() {
        return "Y".equalsIgnoreCase(encryptionRequired);
    }
    
    public boolean isAuditTrailRequired() {
        return "Y".equalsIgnoreCase(auditTrailRequired);
    }
    
    public boolean isValidationEnabled() {
        return "Y".equalsIgnoreCase(validationEnabled);
    }
    
    public boolean isEnabled() {
        return "Y".equalsIgnoreCase(enabled);
    }
    
    public boolean containsPiiData() {
        return piiFields != null && !piiFields.trim().isEmpty();
    }
    
    public boolean hasParallelProcessing() {
        return parallelDegree != null && parallelDegree > 1;
    }
    
    public boolean hasPreExecutionSql() {
        return preExecutionSql != null && !preExecutionSql.trim().isEmpty();
    }
    
    public boolean hasPostExecutionSql() {
        return postExecutionSql != null && !postExecutionSql.trim().isEmpty();
    }
    
    /**
     * Get SQL*Loader command line options as formatted string for control file generation.
     */
    public String getFormattedCommandLineOptions() {
        StringBuilder options = new StringBuilder();
        
        if (isDirectPathEnabled()) {
            options.append("DIRECT=TRUE ");
        }
        
        if (maxErrors != null) {
            options.append("ERRORS=").append(maxErrors).append(" ");
        }
        
        if (skipRows != null && skipRows > 0) {
            options.append("SKIP=").append(skipRows).append(" ");
        }
        
        if (rowsPerCommit != null && !isDirectPathEnabled()) {
            options.append("ROWS=").append(rowsPerCommit).append(" ");
        }
        
        if (bindSize != null) {
            options.append("BINDSIZE=").append(bindSize).append(" ");
        }
        
        if (readSize != null) {
            options.append("READSIZE=").append(readSize).append(" ");
        }
        
        if (hasParallelProcessing()) {
            options.append("PARALLEL=TRUE ");
        }
        
        if (isResumableEnabled()) {
            options.append("RESUMABLE=TRUE ");
            if (resumableTimeout != null) {
                options.append("RESUMABLE_TIMEOUT=").append(resumableTimeout).append(" ");
            }
            if (resumableName != null) {
                options.append("RESUMABLE_NAME='").append(resumableName).append("' ");
            }
        }
        
        if (isSilentModeEnabled()) {
            options.append("SILENT=ALL ");
        }
        
        if (customOptions != null && !customOptions.trim().isEmpty()) {
            options.append(customOptions.trim()).append(" ");
        }
        
        return options.toString().trim();
    }
    
    /**
     * Get the load method clause for control file generation.
     */
    public String getLoadMethodClause() {
        if (loadMethod == null) {
            return "INSERT INTO TABLE " + targetTable;
        }
        
        switch (loadMethod) {
            case APPEND:
                return "APPEND INTO TABLE " + targetTable;
            case REPLACE:
                return "REPLACE INTO TABLE " + targetTable;
            case TRUNCATE:
                return "TRUNCATE INTO TABLE " + targetTable;
            case INSERT:
            default:
                return "INSERT INTO TABLE " + targetTable;
        }
    }
    
    /**
     * Validate configuration for completeness and business rules.
     */
    public void validateConfiguration() {
        if (targetTable == null || targetTable.trim().isEmpty()) {
            throw new IllegalArgumentException("Target table must be specified");
        }
        
        if (maxErrors != null && maxErrors < 0) {
            throw new IllegalArgumentException("Max errors cannot be negative");
        }
        
        if (parallelDegree != null && parallelDegree < 1) {
            throw new IllegalArgumentException("Parallel degree must be at least 1");
        }
        
        if (bindSize != null && bindSize < 1) {
            throw new IllegalArgumentException("Bind size must be positive");
        }
        
        if (readSize != null && readSize < 1) {
            throw new IllegalArgumentException("Read size must be positive");
        }
        
        if (retentionDays != null && retentionDays < 1) {
            throw new IllegalArgumentException("Retention days must be positive");
        }
        
        // Validate PII handling for compliance
        if (containsPiiData() && !isEncryptionRequired() && 
            (dataClassification == DataClassification.CONFIDENTIAL || 
             dataClassification == DataClassification.RESTRICTED || 
             dataClassification == DataClassification.TOP_SECRET)) {
            throw new IllegalArgumentException("PII data with high classification requires encryption");
        }
        
        // Validate regulatory compliance requirements
        if (regulatoryCompliance != null && regulatoryCompliance.contains("PCI") && 
            !isAuditTrailRequired()) {
            throw new IllegalArgumentException("PCI compliance requires audit trail");
        }
    }
}