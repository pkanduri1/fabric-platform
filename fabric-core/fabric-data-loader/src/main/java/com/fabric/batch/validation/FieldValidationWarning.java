package com.fabric.batch.validation;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a field-level validation warning with detailed context information.
 */
@Data
@NoArgsConstructor
public class FieldValidationWarning {
    
    private String fieldName;
    private String fieldValue;
    private Long ruleId;
    private String ruleType;
    private String warningMessage;
    private String warningCode;
    private LocalDateTime timestamp = LocalDateTime.now();
    
    // Additional context
    private Long recordNumber;
    private String suggestedValue;
    private String validationExpression;
    private Map<String, Object> additionalContext = new HashMap<>();
    
    public FieldValidationWarning(String fieldName, String fieldValue, String warningMessage) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.warningMessage = warningMessage;
    }
    
    /**
     * Add additional context information.
     */
    public void addContext(String key, Object value) {
        additionalContext.put(key, value);
    }
    
    /**
     * Get formatted warning message with context.
     */
    public String getFormattedWarningMessage() {
        StringBuilder formatted = new StringBuilder();
        formatted.append("[WARNING] ");
        formatted.append("Field '").append(fieldName).append("' ");
        
        if (fieldValue != null) {
            formatted.append("(value: '").append(fieldValue).append("') ");
        }
        
        formatted.append("- ").append(warningMessage);
        
        if (ruleType != null) {
            formatted.append(" [Rule: ").append(ruleType).append("]");
        }
        
        return formatted.toString();
    }
}