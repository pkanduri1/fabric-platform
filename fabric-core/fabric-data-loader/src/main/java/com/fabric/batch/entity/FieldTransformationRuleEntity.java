package com.fabric.batch.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing field-level transformation rules migrated from Excel mapping documents.
 * 
 * This entity captures complex transformation logic including conditional processing,
 * data formatting, validation rules, and audit requirements for financial data processing.
 */
@Entity
@Table(name = "field_transformation_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FieldTransformationRuleEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "field_transform_rule_seq")
    @SequenceGenerator(name = "field_transform_rule_seq", sequenceName = "seq_field_transform_rule_id", allocationSize = 1)
    @Column(name = "rule_id")
    private Long ruleId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private TransformationConfigEntity transformationConfig;
    
    // Source field information
    @Column(name = "source_field_name", length = 100)
    private String sourceFieldName;
    
    @Column(name = "source_data_type", length = 50)
    private String sourceDataType;
    
    @Column(name = "source_format", length = 100)
    private String sourceFormat;
    
    @Column(name = "source_length")
    private Integer sourceLength;
    
    // Target field information
    @Column(name = "target_field_name", nullable = false, length = 100)
    private String targetFieldName;
    
    @Column(name = "target_position", nullable = false)
    private Integer targetPosition;
    
    @Column(name = "target_length", nullable = false)
    private Integer targetLength;
    
    @Column(name = "target_data_type", nullable = false, length = 50)
    private String targetDataType;
    
    @Column(name = "target_format", length = 100)
    private String targetFormat;
    
    // Transformation logic
    @Column(name = "transformation_type", nullable = false, length = 50)
    private String transformationType; // source, constant, blank, conditional, lookup, expression, etc.
    
    @Lob
    @Column(name = "transformation_expression")
    private String transformationExpression; // Complex transformation logic
    
    @Column(name = "default_value", length = 4000)
    private String defaultValue;
    
    @Column(name = "constant_value", length = 4000)
    private String constantValue;
    
    // Formatting and padding
    @Column(name = "pad_direction", length = 10)
    private String padDirection; // left, right, none
    
    @Column(name = "pad_character", length = 1)
    private String padCharacter = " ";
    
    // Conditional logic (simple if/else)
    @Column(name = "condition_expression", length = 2000)
    private String conditionExpression;
    
    @Column(name = "if_value", length = 1000)
    private String ifValue;
    
    @Lob
    @Column(name = "elseif_conditions")
    private String elseifConditions; // JSON array of elseif conditions
    
    @Column(name = "else_value", length = 1000)
    private String elseValue;
    
    // Lookup table configuration
    @Column(name = "lookup_table", length = 100)
    private String lookupTable;
    
    @Column(name = "lookup_key_column", length = 100)
    private String lookupKeyColumn;
    
    @Column(name = "lookup_value_column", length = 100)
    private String lookupValueColumn;
    
    @Column(name = "lookup_default_value", length = 1000)
    private String lookupDefaultValue;
    
    // Data quality and validation
    @Column(name = "required_field", length = 1)
    private String requiredField = "N";
    
    @Column(name = "validation_rule", length = 2000)
    private String validationRule;
    
    @Column(name = "error_handling", length = 50)
    private String errorHandling = "default"; // default, skip, fail, warning
    
    // Metadata and auditing
    @Column(name = "excel_row_reference")
    private Integer excelRowReference; // Row number from original Excel mapping
    
    @Column(name = "business_rule_description", length = 1000)
    private String businessRuleDescription;
    
    @Column(name = "execution_order")
    private Integer executionOrder = 1;
    
    @Column(name = "performance_notes", length = 500)
    private String performanceNotes;
    
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
    
    // Relationship to conditional expressions for complex if/elseif/else logic
    // TODO: Implement conditional expressions relationship in future iteration
    // @OneToMany(mappedBy = "fieldTransformationRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<ConditionalExpressionEntity> conditionalExpressions;
    
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
        version++;
    }
    
    /**
     * Checks if this field is required for validation
     */
    public boolean isRequiredField() {
        return "Y".equals(requiredField);
    }
    
    /**
     * Checks if this rule is currently enabled
     */
    public boolean isEnabled() {
        return "Y".equals(enabled);
    }
    
    /**
     * Determines if this is a source field transformation
     */
    public boolean isSourceTransformation() {
        return "source".equals(transformationType);
    }
    
    /**
     * Determines if this is a constant value transformation
     */
    public boolean isConstantTransformation() {
        return "constant".equals(transformationType);
    }
    
    /**
     * Determines if this is a conditional transformation
     */
    public boolean isConditionalTransformation() {
        return "conditional".equals(transformationType);
    }
    
    /**
     * Gets the configuration ID from the parent transformation config
     */
    public String getConfigId() {
        return transformationConfig != null ? transformationConfig.getConfigId() : null;
    }
    
    /**
     * Gets a human-readable rule identifier for logging and debugging
     */
    public String getRuleIdentifier() {
        return String.format("%s.%s[%d]", 
            getConfigId(), 
            targetFieldName, 
            targetPosition
        );
    }
}