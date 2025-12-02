package com.fabric.batch.entity;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * POJO class for SQL*Loader configuration.
 * This would be implemented in the fabric-data-loader module.
 * For Phase 1.2 testing purposes, we provide stub implementation.
 */
@Data
@Builder(toBuilder = true)
public class SqlLoaderConfigEntity {
    
    private String configId;
    private String jobName;
    private String sourceSystem;
    private String targetTable;
    private LoadMethod loadMethod;
    private String directPath;
    private Integer parallelDegree;
    private Long bindSize;
    private Long readSize;
    private Integer maxErrors;
    private Integer skipRows;
    private Integer rowsPerCommit;
    private String fieldDelimiter;
    private String recordDelimiter;
    private String stringDelimiter;
    private String characterSet;
    private DataClassification dataClassification;
    private String encryptionRequired;
    private String auditTrailRequired;
    private String piiFields;
    private String regulatoryCompliance;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
    private Integer version;
    private Boolean enabled;
    private List<Object> fieldConfigs;
    
    public enum LoadMethod {
        INSERT, APPEND, REPLACE, TRUNCATE
    }
    
    public enum DataClassification {
        PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED, TOP_SECRET
    }
    
    public boolean isDirectPathEnabled() {
        return "Y".equals(directPath);
    }
    
    public boolean isEncryptionRequired() {
        return "Y".equals(encryptionRequired);
    }
    
    public boolean isAuditTrailRequired() {
        return "Y".equals(auditTrailRequired);
    }
    
    public boolean containsPiiData() {
        return piiFields != null && !piiFields.trim().isEmpty();
    }
    
    public boolean isEnabled() {
        return enabled != null ? enabled : true;
    }
}