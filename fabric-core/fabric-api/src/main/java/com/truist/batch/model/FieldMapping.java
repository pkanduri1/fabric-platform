package com.truist.batch.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a field mapping configuration for batch processing.
 * 
 * This model defines how a field should be transformed, formatted,
 * and positioned in the output file.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldMapping {
    
    /**
     * The name of the field.
     */
    @JsonProperty("fieldName")
    private String fieldName;
    
    /**
     * The target field name in the output.
     */
    @JsonProperty("targetField")
    private String targetField;
    
    /**
     * The source field name from input data.
     */
    @JsonProperty("sourceField")
    private String sourceField;
    
    /**
     * Alternative source field identifier.
     */
    @JsonProperty("from")
    private String from;
    
    /**
     * The position of this field in the output (1-based).
     */
    @JsonProperty("targetPosition")
    private int targetPosition;
    
    /**
     * The length of this field in the output.
     */
    @JsonProperty("length")
    private int length;
    
    /**
     * The data type of this field.
     */
    @JsonProperty("dataType")
    private String dataType;
    
    /**
     * The format string for this field.
     */
    @JsonProperty("format")
    private String format;
    
    /**
     * The source format string.
     */
    @JsonProperty("sourceFormat")
    private String sourceFormat;
    
    /**
     * The target format string.
     */
    @JsonProperty("targetFormat")
    private String targetFormat;
    
    /**
     * The type of transformation to apply.
     * Valid values: source, constant, conditional, composite
     */
    @JsonProperty("transformationType")
    private String transformationType;
    
    /**
     * The constant value for constant transformations.
     */
    @JsonProperty("value")
    private String value;
    
    /**
     * The default value to use if transformation fails.
     */
    @JsonProperty("defaultValue")
    private String defaultValue;
    
    /**
     * Custom transformation expression.
     */
    @JsonProperty("transform")
    private String transform;
    
    /**
     * Padding direction (left or right).
     */
    @JsonProperty("pad")
    private String pad;
    
    /**
     * Character to use for padding.
     */
    @JsonProperty("padChar")
    private String padChar;
    
    /**
     * List of conditions for conditional transformations.
     */
    @JsonProperty("conditions")
    private List<Condition> conditions;
    
    /**
     * Whether this is a composite field.
     */
    @JsonProperty("composite")
    private boolean composite;
    
    /**
     * List of source fields for composite transformations.
     */
    @JsonProperty("sources")
    private List<String> sources;
    
    /**
     * Delimiter for composite fields.
     */
    @JsonProperty("delimiter")
    private String delimiter;
    
    /**
     * DSL expression for advanced transformations.
     */
    @JsonProperty("expression")
    private String expression;

    /**
     * Encryption level for this field (e.g., NONE, AES256).
     */
    @JsonProperty("encryptionLevel")
    private String encryptionLevel;

    /**
     * PII classification level (e.g., NONE, SENSITIVE, HIGH).
     */
    @JsonProperty("piiClassification")
    private String piiClassification;

    /**
     * Whether validation is required for this field.
     */
    @JsonProperty("validationRequired")
    private boolean validationRequired;

    /**
     * Compliance level for this field (e.g., STANDARD, HIGH, CRITICAL).
     */
    @JsonProperty("complianceLevel")
    private String complianceLevel;

    /**
     * Business context or description for this field.
     */
    @JsonProperty("businessContext")
    private String businessContext;

    /**
     * Business rule identifier associated with this field.
     */
    @JsonProperty("businessRuleId")
    private String businessRuleId;

    /**
     * Whether this field requires high compliance monitoring.
     */
    @JsonProperty("highCompliance")
    private boolean highCompliance;

    /**
     * Checks if this field mapping has a DSL expression.
     *
     * @return true if expression is not null and not empty
     */
    public boolean hasDSLExpression() {
        return expression != null && !expression.trim().isEmpty();
    }
}