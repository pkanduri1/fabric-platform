package com.truist.batch.sqlloader;

import lombok.Builder;
import lombok.Data;

/**
 * Stub class for SqlLoaderConfig from fabric-data-loader module.
 * For Phase 1.2 testing purposes, we provide stub implementation.
 */
@Data
@Builder(toBuilder = true)
public class SqlLoaderConfig {
    private String configId;
    private String jobName;
    private String targetTable;
    private String dataFileName;
    private String controlFileName;
    private String loadMethod;
    private Boolean directPath;
    private Integer parallelDegree;
    private Long bindSize;
    private Long readSize;
    private Integer errors;
    private Integer skip;
    private Integer rows;
    private String fieldDelimiter;
    private String recordDelimiter;
    private String stringDelimiter;
    private String characterSet;
    private Boolean trimWhitespace;
    private Boolean optionalEnclosures;
    private Boolean resumable;
    private Integer resumableTimeout;
    private String correlationId;
    private Boolean encryptionRequired;
    private String encryptionKeyId;
    private Boolean auditTrailRequired;
    
    public void setDataFileName(String dataFileName) {
        this.dataFileName = dataFileName;
    }
    
    public void setFieldDelimiter(String delimiter) {
        this.fieldDelimiter = delimiter;
    }
    
    public void setRecordDelimiter(String delimiter) {
        this.recordDelimiter = delimiter;
    }
    
    public void setStringDelimiter(String delimiter) {
        this.stringDelimiter = delimiter;
    }
    
    public void setOptionalEnclosures(Boolean optional) {
        this.optionalEnclosures = optional;
    }
    
    public void setTrimWhitespace(Boolean trim) {
        this.trimWhitespace = trim;
    }
    
    public void setParallelDegree(Integer degree) {
        this.parallelDegree = degree;
    }
    
    public void setBindSize(Long size) {
        this.bindSize = size;
    }
    
    public void setDirectPath(Boolean direct) {
        this.directPath = direct;
    }
    
    public void setReadSize(Long size) {
        this.readSize = size;
    }
    
    public void setResumable(Boolean resumable) {
        this.resumable = resumable;
    }
    
    public void setResumableTimeout(Integer timeout) {
        this.resumableTimeout = timeout;
    }
    
    public void setEncryptionRequired(Boolean required) {
        this.encryptionRequired = required;
    }
    
    public void setEncryptionKeyId(String keyId) {
        this.encryptionKeyId = keyId;
    }
    
    public void setAuditTrailRequired(Boolean required) {
        this.auditTrailRequired = required;
    }
}