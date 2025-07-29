package com.truist.batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Entity representing validation rules for data loading operations.
 * Supports configurable data type validation, business rules, and referential integrity checks.
 */
@Entity
@Table(name = "validation_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"dataLoadConfig"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ValidationRuleEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long ruleId;
    
    @Column(name = "config_id", nullable = false, length = 100)
    private String configId;
    
    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;
    
    @Column(name = "rule_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;
    
    @Column(name = "validation_expression", length = 2000)
    private String validationExpression;
    
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    
    @Column(name = "severity", length = 20)
    @Enumerated(EnumType.STRING)
    private Severity severity = Severity.ERROR;
    
    @Column(name = "execution_order")
    private Integer executionOrder = 1;
    
    @Column(name = "data_type", length = 50)
    private String dataType;
    
    @Column(name = "max_length")
    private Integer maxLength;
    
    @Column(name = "min_length")
    private Integer minLength;
    
    @Column(name = "pattern", length = 500)
    private String pattern;
    
    @Column(name = "required_field", length = 1)
    private String requiredField = "N";
    
    @Column(name = "unique_field", length = 1)
    private String uniqueField = "N";
    
    @Column(name = "reference_table", length = 100)
    private String referenceTable;
    
    @Column(name = "reference_column", length = 100)
    private String referenceColumn;
    
    @Column(name = "lookup_sql", length = 2000)
    private String lookupSql;
    
    @Column(name = "business_rule_class", length = 200)
    private String businessRuleClass;
    
    @Column(name = "error_threshold")
    private Integer errorThreshold = 0;
    
    @Column(name = "warning_threshold")  
    private Integer warningThreshold = 0;
    
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", referencedColumnName = "config_id", insertable = false, updatable = false)
    private DataLoadConfigEntity dataLoadConfig;
    
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (version == null) {
            version = 1;
        }
        if (executionOrder == null) {
            executionOrder = 1;
        }
        if (severity == null) {
            severity = Severity.ERROR;
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
    public enum RuleType {
        DATA_TYPE_VALIDATION,
        LENGTH_VALIDATION,
        PATTERN_VALIDATION,
        REQUIRED_FIELD_VALIDATION,
        UNIQUE_FIELD_VALIDATION,
        REFERENTIAL_INTEGRITY,
        BUSINESS_RULE,
        CUSTOM_SQL_VALIDATION,
        RANGE_VALIDATION,
        DATE_FORMAT_VALIDATION,
        NUMERIC_VALIDATION,
        EMAIL_VALIDATION,
        PHONE_VALIDATION,
        SSN_VALIDATION,
        ACCOUNT_NUMBER_VALIDATION
    }
    
    public enum Severity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
    
    // Utility methods
    public boolean isRequired() {
        return "Y".equalsIgnoreCase(requiredField);
    }
    
    public boolean isUnique() {
        return "Y".equalsIgnoreCase(uniqueField);
    }
    
    public boolean isEnabled() {
        return "Y".equalsIgnoreCase(enabled);
    }
    
    public boolean isCritical() {
        return severity == Severity.CRITICAL || severity == Severity.ERROR;
    }
    
    public boolean hasReferentialIntegrity() {
        return referenceTable != null && !referenceTable.trim().isEmpty();
    }
    
    public boolean hasCustomBusinessRule() {
        return businessRuleClass != null && !businessRuleClass.trim().isEmpty();
    }
}