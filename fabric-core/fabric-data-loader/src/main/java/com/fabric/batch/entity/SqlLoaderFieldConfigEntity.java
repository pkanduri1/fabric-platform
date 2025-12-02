package com.fabric.batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Entity representing individual field configuration for SQL*Loader control file generation.
 * This entity stores detailed field-level settings including data types, transformations,
 * validation rules, encryption settings, and audit requirements.
 */
@Entity
@Table(name = "sql_loader_field_configs", schema = "CM3INT")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"sqlLoaderConfig"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SqlLoaderFieldConfigEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "field_config_id")
    private Long fieldConfigId;
    
    @Column(name = "config_id", nullable = false, length = 100)
    private String configId;
    
    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;
    
    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;
    
    @Column(name = "field_position")
    private Integer fieldPosition; // For fixed-width files
    
    @Column(name = "field_length")
    private Integer fieldLength; // For fixed-width files
    
    @Column(name = "data_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SqlLoaderDataType dataType;
    
    @Column(name = "format_mask", length = 100)
    private String formatMask; // Format mask for dates/numbers
    
    @Column(name = "nullable", length = 1)
    @Builder.Default
    private String nullable = "Y";
    
    @Column(name = "default_value", length = 500)
    private String defaultValue;
    
    @Column(name = "sql_expression", columnDefinition = "CLOB")
    @Lob
    private String sqlExpression; // SQL expression for field transformation
    
    @Column(name = "encrypted", length = 1)
    @Builder.Default
    private String encrypted = "N";
    
    @Column(name = "encryption_function", length = 200)
    private String encryptionFunction;
    
    @Column(name = "validation_rule", length = 1000)
    private String validationRule;
    
    @Column(name = "null_if_condition", length = 100)
    @Builder.Default
    private String nullIfCondition = "BLANKS";
    
    @Column(name = "trim_field", length = 1)
    @Builder.Default
    private String trimField = "Y";
    
    @Column(name = "case_sensitive", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CaseSensitive caseSensitive = CaseSensitive.PRESERVE;
    
    @Column(name = "max_length")
    private Integer maxLength;
    
    @Column(name = "min_length")
    private Integer minLength;
    
    @Column(name = "character_set", length = 20)
    private String characterSet;
    
    @Column(name = "check_constraint", length = 1000)
    private String checkConstraint;
    
    @Column(name = "lookup_table", length = 100)
    private String lookupTable;
    
    @Column(name = "lookup_column", length = 100)
    private String lookupColumn;
    
    @Column(name = "unique_constraint", length = 1)
    @Builder.Default
    private String uniqueConstraint = "N";
    
    @Column(name = "primary_key", length = 1)
    @Builder.Default
    private String primaryKey = "N";
    
    @Column(name = "business_rule_class", length = 200)
    private String businessRuleClass;
    
    @Column(name = "business_rule_parameters", length = 2000)
    private String businessRuleParameters;
    
    @Column(name = "audit_field", length = 1)
    @Builder.Default
    private String auditField = "N";
    
    @Column(name = "source_field", length = 100)
    private String sourceField;
    
    @Column(name = "transformation_applied", length = 500)
    private String transformationApplied;
    
    @Column(name = "data_lineage", length = 1000)
    private String dataLineage;
    
    @Column(name = "field_order", nullable = false)
    private Integer fieldOrder;
    
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", referencedColumnName = "config_id", insertable = false, updatable = false)
    private SqlLoaderConfigEntity sqlLoaderConfig;
    
    // JPA lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
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
    public enum SqlLoaderDataType {
        CHAR,
        VARCHAR2,
        NUMBER,
        DATE,
        TIMESTAMP,
        CLOB,
        BLOB,
        RAW,
        LONG,
        ROWID,
        UROWID
    }
    
    public enum CaseSensitive {
        PRESERVE,
        UPPER,
        LOWER
    }
    
    // Utility methods
    public boolean isNullable() {
        return "Y".equalsIgnoreCase(nullable);
    }
    
    public boolean isEncrypted() {
        return "Y".equalsIgnoreCase(encrypted);
    }
    
    public boolean isTrimEnabled() {
        return "Y".equalsIgnoreCase(trimField);
    }
    
    public boolean hasUniqueConstraint() {
        return "Y".equalsIgnoreCase(uniqueConstraint);
    }
    
    public boolean isPrimaryKey() {
        return "Y".equalsIgnoreCase(primaryKey);
    }
    
    public boolean isAuditField() {
        return "Y".equalsIgnoreCase(auditField);
    }
    
    public boolean isEnabled() {
        return "Y".equalsIgnoreCase(enabled);
    }
    
    public boolean hasBusinessRule() {
        return businessRuleClass != null && !businessRuleClass.trim().isEmpty();
    }
    
    public boolean hasValidationRule() {
        return validationRule != null && !validationRule.trim().isEmpty();
    }
    
    public boolean hasLookupReference() {
        return lookupTable != null && !lookupTable.trim().isEmpty() &&
               lookupColumn != null && !lookupColumn.trim().isEmpty();
    }
    
    public boolean hasSqlExpression() {
        return sqlExpression != null && !sqlExpression.trim().isEmpty();
    }
    
    public boolean isFixedWidth() {
        return fieldPosition != null && fieldLength != null && fieldLength > 0;
    }
    
    /**
     * Generate SQL*Loader control file field specification for this field.
     */
    public String generateControlFileFieldSpec() {
        StringBuilder spec = new StringBuilder();
        
        // Field name
        spec.append(columnName);
        
        // Position and length for fixed-width files
        if (isFixedWidth()) {
            spec.append(" POSITION(").append(fieldPosition).append(":").append(fieldPosition + fieldLength - 1).append(")");
        }
        
        // Data type specification
        spec.append(" ").append(generateDataTypeSpec());
        
        // Null handling
        if (!isNullable() || !nullIfCondition.equals("BLANKS")) {
            spec.append(" NULLIF ").append(nullIfCondition);
        }
        
        // Trimming
        if (isTrimEnabled()) {
            spec.append(" \"TRIM(:").append(columnName).append(")\"");
        }
        
        // Case conversion
        if (caseSensitive != CaseSensitive.PRESERVE) {
            String caseFunction = caseSensitive == CaseSensitive.UPPER ? "UPPER" : "LOWER";
            spec.append(" \"").append(caseFunction).append("(:").append(columnName).append(")\"");
        }
        
        // SQL expression transformation
        if (hasSqlExpression()) {
            spec.append(" \"").append(sqlExpression).append("\"");
        }
        
        // Encryption function
        if (isEncrypted() && encryptionFunction != null) {
            spec.append(" \"").append(encryptionFunction).append("(:").append(columnName).append(")\"");
        }
        
        return spec.toString();
    }
    
    /**
     * Generate data type specification for SQL*Loader control file.
     */
    private String generateDataTypeSpec() {
        StringBuilder typeSpec = new StringBuilder();
        
        switch (dataType) {
            case CHAR:
                typeSpec.append("CHAR");
                if (maxLength != null) {
                    typeSpec.append("(").append(maxLength).append(")");
                }
                break;
            case VARCHAR2:
                typeSpec.append("VARCHAR2");
                if (maxLength != null) {
                    typeSpec.append("(").append(maxLength).append(")");
                }
                break;
            case NUMBER:
                typeSpec.append("DECIMAL EXTERNAL");
                if (formatMask != null) {
                    typeSpec.append(" \"").append(formatMask).append("\"");
                }
                break;
            case DATE:
                typeSpec.append("DATE");
                if (formatMask != null) {
                    typeSpec.append(" \"").append(formatMask).append("\"");
                } else {
                    typeSpec.append(" \"YYYY-MM-DD HH24:MI:SS\"");
                }
                break;
            case TIMESTAMP:
                typeSpec.append("TIMESTAMP");
                if (formatMask != null) {
                    typeSpec.append(" \"").append(formatMask).append("\"");
                } else {
                    typeSpec.append(" \"YYYY-MM-DD HH24:MI:SS.FF\"");
                }
                break;
            case CLOB:
                typeSpec.append("CHAR(").append(maxLength != null ? maxLength : 4000).append(")");
                break;
            case RAW:
                typeSpec.append("RAW");
                if (maxLength != null) {
                    typeSpec.append("(").append(maxLength).append(")");
                }
                break;
            default:
                typeSpec.append("CHAR");
                if (maxLength != null) {
                    typeSpec.append("(").append(maxLength).append(")");
                }
        }
        
        return typeSpec.toString();
    }
    
    /**
     * Validate field configuration for correctness and completeness.
     */
    public void validateFieldConfiguration() {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be empty");
        }
        
        if (columnName == null || columnName.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name cannot be empty for field: " + fieldName);
        }
        
        if (fieldOrder == null || fieldOrder < 1) {
            throw new IllegalArgumentException("Field order must be positive for field: " + fieldName);
        }
        
        if (dataType == null) {
            throw new IllegalArgumentException("Data type must be specified for field: " + fieldName);
        }
        
        // Fixed-width validation
        if (fieldPosition != null && (fieldLength == null || fieldLength < 1)) {
            throw new IllegalArgumentException("Field length must be positive when position is specified for field: " + fieldName);
        }
        
        // Length validation
        if (maxLength != null && minLength != null && maxLength < minLength) {
            throw new IllegalArgumentException("Max length cannot be less than min length for field: " + fieldName);
        }
        
        // PII and encryption validation
        if (isEncrypted() && encryptionFunction == null) {
            throw new IllegalArgumentException("Encryption function must be specified for encrypted field: " + fieldName);
        }
        
        // Lookup validation
        if (lookupTable != null && lookupColumn == null) {
            throw new IllegalArgumentException("Lookup column must be specified when lookup table is provided for field: " + fieldName);
        }
        
        // Business rule validation
        if (hasBusinessRule() && businessRuleParameters != null && !isValidJson(businessRuleParameters)) {
            throw new IllegalArgumentException("Business rule parameters must be valid JSON for field: " + fieldName);
        }
    }
    
    /**
     * Simple JSON validation for business rule parameters.
     */
    private boolean isValidJson(String json) {
        try {
            return json.trim().startsWith("{") && json.trim().endsWith("}");
        } catch (Exception e) {
            return false;
        }
    }
}