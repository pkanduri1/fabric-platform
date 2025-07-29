package com.truist.batch.sqlloader;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Configuration class for SQL*Loader operations.
 * Encapsulates all parameters needed for SQL*Loader control file generation and execution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlLoaderConfig {
    
    // Basic configuration
    private String configId;
    private String jobName;
    private String targetTable;
    private String dataFileName;
    private String controlFileName;
    private String logFileName;
    private String badFileName;
    private String discardFileName;
    
    // SQL*Loader options
    private String userid;  // Will be encrypted/secure
    private String loadMethod = "INSERT";  // INSERT, APPEND, REPLACE, TRUNCATE
    private Boolean directPath = true;
    private Integer errors = 1000;
    private Integer skip = 1;  // Skip header rows
    private Integer rows = 64;  // Rows per commit for conventional path
    private Integer bindSize = 256000;  // Bind array size
    private Integer readSize = 1048576;  // Read buffer size
    private Boolean parallel = false;
    private String characterSet = "UTF8";
    private String dateFormat = "YYYY-MM-DD HH24:MI:SS";
    private String timestampFormat = "YYYY-MM-DD HH24:MI:SS.FF";
    
    // File format configuration
    private String fieldDelimiter = "|";
    private String recordDelimiter = "\n";
    private String stringDelimiter = "\"";
    private Boolean optionalEnclosures = true;
    private String nullIf = "BLANKS";
    private Boolean trimWhitespace = true;
    private String encoding = "UTF-8";
    
    // Field configurations
    private List<FieldConfig> fields;
    
    // Security configuration
    private Boolean encryptionRequired = false;
    private String encryptionAlgorithm = "AES256";  // AES256, 3DES
    private String encryptionKeyId;
    private Boolean auditTrailRequired = true;
    
    // Performance configuration
    private Integer parallelDegree = 1;
    private String streamSize = "256000";
    private Boolean resumable = true;
    private String resumableTimeout = "7200";  // 2 hours
    private String resumableName;
    
    // Error handling configuration
    private Integer maxErrors = 1000;
    private Boolean continueLoad = false;  // Continue on errors
    private Boolean silentMode = false;
    private String errorLogging = "TABLE";  // TABLE, FILE, BOTH
    
    // Advanced options
    private Map<String, String> customOptions;
    private List<String> preExecutionSql;
    private List<String> postExecutionSql;
    
    // Validation configuration
    private Boolean validateOnly = false;
    private Boolean dataValidation = true;
    private Boolean referentialIntegrityCheck = true;
    
    // Monitoring and logging
    private String correlationId;
    private String executionEnvironment;
    private Map<String, Object> additionalMetadata;
    
    /**
     * Field configuration for SQL*Loader control file generation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldConfig {
        private String fieldName;
        private String columnName;
        private Integer position;  // For fixed-width files
        private Integer length;    // For fixed-width files
        private String dataType;   // CHAR, NUMBER, DATE, TIMESTAMP, etc.
        private String format;     // Format mask for dates/numbers
        private Boolean nullable = true;
        private String defaultValue;
        private String expression;  // SQL expression for transformation
        private Boolean encrypted = false;
        private String encryptionFunction;
        private String validationRule;
        private String nullIf = "BLANKS";
        private Boolean trim = true;
        private String caseSensitive = "PRESERVE";  // PRESERVE, UPPER, LOWER
        private Integer maxLength;
        private String characterSet;
        
        // Validation constraints
        private String checkConstraint;
        private String lookupTable;
        private String lookupColumn;
        private Boolean uniqueConstraint = false;
        private Boolean primaryKey = false;
        
        // Business rule configuration
        private String businessRuleClass;
        private Map<String, Object> businessRuleParameters;
        
        // Audit and lineage
        private Boolean auditField = false;
        private String sourceField;
        private String transformation;
        private String dataLineage;
    }
    
    /**
     * Get SQL*Loader command line options as a formatted string.
     */
    public String getCommandLineOptions() {
        StringBuilder options = new StringBuilder();
        
        if (directPath != null && directPath) {
            options.append("DIRECT=TRUE ");
        }
        
        if (errors != null) {
            options.append("ERRORS=").append(errors).append(" ");
        }
        
        if (skip != null && skip > 0) {
            options.append("SKIP=").append(skip).append(" ");
        }
        
        if (rows != null && (directPath == null || !directPath)) {
            options.append("ROWS=").append(rows).append(" ");
        }
        
        if (bindSize != null) {
            options.append("BINDSIZE=").append(bindSize).append(" ");
        }
        
        if (readSize != null) {
            options.append("READSIZE=").append(readSize).append(" ");
        }
        
        if (parallel != null && parallel) {
            options.append("PARALLEL=TRUE ");
        }
        
        if (resumable != null && resumable) {
            options.append("RESUMABLE=TRUE ");
            if (resumableTimeout != null) {
                options.append("RESUMABLE_TIMEOUT=").append(resumableTimeout).append(" ");
            }
            if (resumableName != null) {
                options.append("RESUMABLE_NAME='").append(resumableName).append("' ");
            }
        }
        
        if (silentMode != null && silentMode) {
            options.append("SILENT=ALL ");
        }
        
        // Add custom options
        if (customOptions != null) {
            customOptions.forEach((key, value) -> 
                options.append(key).append("=").append(value).append(" "));
        }
        
        return options.toString().trim();
    }
    
    /**
     * Validate the configuration for completeness and correctness.
     */
    public void validate() {
        if (targetTable == null || targetTable.trim().isEmpty()) {
            throw new IllegalArgumentException("Target table must be specified");
        }
        
        if (dataFileName == null || dataFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Data file name must be specified");
        }
        
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("Field configurations must be provided");
        }
        
        if (maxErrors != null && maxErrors < 0) {
            throw new IllegalArgumentException("Max errors cannot be negative");
        }
        
        if (parallelDegree != null && parallelDegree < 1) {
            throw new IllegalArgumentException("Parallel degree must be at least 1");
        }
        
        // Validate field configurations
        for (FieldConfig field : fields) {
            if (field.getFieldName() == null || field.getFieldName().trim().isEmpty()) {
                throw new IllegalArgumentException("Field name cannot be empty");
            }
            if (field.getColumnName() == null || field.getColumnName().trim().isEmpty()) {
                throw new IllegalArgumentException("Column name cannot be empty for field: " + field.getFieldName());
            }
        }
    }
    
    /**
     * Get the load method for the control file.
     */
    public String getLoadMethodForControlFile() {
        if ("INSERT".equalsIgnoreCase(loadMethod)) {
            return "INSERT INTO TABLE " + targetTable;
        } else if ("APPEND".equalsIgnoreCase(loadMethod)) {
            return "APPEND INTO TABLE " + targetTable;
        } else if ("REPLACE".equalsIgnoreCase(loadMethod)) {
            return "REPLACE INTO TABLE " + targetTable;
        } else if ("TRUNCATE".equalsIgnoreCase(loadMethod)) {
            return "TRUNCATE INTO TABLE " + targetTable;
        } else {
            return "INSERT INTO TABLE " + targetTable;
        }
    }
    
    /**
     * Check if direct path loading is enabled and valid.
     */
    public boolean isDirectPathEnabled() {
        return directPath != null && directPath && 
               (loadMethod == null || !"INSERT".equalsIgnoreCase(loadMethod) || 
                preExecutionSql == null || preExecutionSql.isEmpty());
    }
    
    /**
     * Get field configuration by field name.
     */
    public FieldConfig getFieldConfig(String fieldName) {
        if (fields == null) return null;
        return fields.stream()
                .filter(f -> fieldName.equals(f.getFieldName()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Check if any field requires encryption.
     */
    public boolean hasEncryptedFields() {
        if (fields == null) return false;
        return fields.stream().anyMatch(f -> f.getEncrypted() != null && f.getEncrypted());
    }
    
    /**
     * Check if any field has business rules.
     */
    public boolean hasBusinessRules() {
        if (fields == null) return false;
        return fields.stream().anyMatch(f -> f.getBusinessRuleClass() != null && !f.getBusinessRuleClass().trim().isEmpty());
    }
}