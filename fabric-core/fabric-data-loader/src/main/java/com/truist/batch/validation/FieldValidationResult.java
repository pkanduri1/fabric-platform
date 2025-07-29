package com.truist.batch.validation;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of validating a single field against a validation rule.
 */
@Data
@NoArgsConstructor
public class FieldValidationResult {
    
    private String fieldName;
    private String fieldValue;
    private Long ruleId;
    private boolean valid = true;
    private String errorMessage;
    private String warningMessage;
    private String transformedValue;  // If rule transforms the value
    
    public FieldValidationResult(String fieldName, String fieldValue, boolean valid) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.valid = valid;
    }
    
    public FieldValidationResult(String fieldName, String fieldValue, boolean valid, String errorMessage) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.valid = valid;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Create a successful validation result.
     */
    public static FieldValidationResult success(String fieldName, String fieldValue) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setValid(true);
        return result;
    }
    
    /**
     * Create a failed validation result.
     */
    public static FieldValidationResult failure(String fieldName, String fieldValue, String errorMessage) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setValid(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    /**
     * Create a validation result with warning.
     */
    public static FieldValidationResult warning(String fieldName, String fieldValue, String warningMessage) {
        FieldValidationResult result = new FieldValidationResult();
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setValid(true);
        result.setWarningMessage(warningMessage);
        return result;
    }
    
    /**
     * Check if validation has warnings.
     */
    public boolean hasWarnings() {
        return warningMessage != null && !warningMessage.trim().isEmpty();
    }
}