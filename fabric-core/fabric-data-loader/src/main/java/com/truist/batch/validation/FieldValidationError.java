package com.truist.batch.validation;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a field-level validation error with detailed context information.
 */
@Data
@NoArgsConstructor
public class FieldValidationError {
    
    private String fieldName;
    private String fieldValue;
    private Long ruleId;
    private String ruleType;
    private String errorMessage;
    private String severity = "ERROR";  // INFO, WARNING, ERROR, CRITICAL
    private String errorCode;
    private LocalDateTime timestamp = LocalDateTime.now();
    
    // Additional context
    private Long recordNumber;
    private String expectedValue;
    private String validationExpression;
    private Map<String, Object> additionalContext = new HashMap<>();
    
    public FieldValidationError(String fieldName, String fieldValue, String errorMessage) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.errorMessage = errorMessage;
    }
    
    public FieldValidationError(String fieldName, String fieldValue, String errorMessage, String severity) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.errorMessage = errorMessage;
        this.severity = severity;
    }
    
    /**
     * Add additional context information.
     */
    public void addContext(String key, Object value) {
        additionalContext.put(key, value);
    }
    
    /**
     * Check if this is a critical error.
     */
    public boolean isCritical() {
        return "CRITICAL".equals(severity);
    }
    
    /**
     * Get formatted error message with context.
     */
    public String getFormattedErrorMessage() {
        StringBuilder formatted = new StringBuilder();
        formatted.append("[").append(severity).append("] ");
        formatted.append("Field '").append(fieldName).append("' ");
        
        if (fieldValue != null) {
            formatted.append("(value: '").append(fieldValue).append("') ");
        }
        
        formatted.append("- ").append(errorMessage);
        
        if (ruleType != null) {
            formatted.append(" [Rule: ").append(ruleType).append("]");
        }
        
        return formatted.toString();
    }
}