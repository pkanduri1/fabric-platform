package com.truist.batch.validation;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Result object for individual record validation containing field-level errors and warnings.
 */
@Data
@NoArgsConstructor
public class RecordValidationResult {
    
    private Long recordNumber;
    private Map<String, String> recordData;
    private boolean valid = true;
    private LocalDateTime validationTime;
    
    private List<FieldValidationError> fieldErrors = new ArrayList<>();
    private List<FieldValidationWarning> fieldWarnings = new ArrayList<>();
    private List<String> recordLevelErrors = new ArrayList<>();
    private List<String> recordLevelWarnings = new ArrayList<>();
    
    private String recordHash;  // For duplicate detection
    private Map<String, Object> metadata;
    
    /**
     * Add a field validation error.
     */
    public void addFieldError(FieldValidationError error) {
        fieldErrors.add(error);
        valid = false;
    }
    
    /**
     * Add a field validation warning.
     */
    public void addFieldWarning(FieldValidationWarning warning) {
        fieldWarnings.add(warning);
    }
    
    /**
     * Add a record-level error.
     */
    public void addRecordError(String errorMessage) {
        recordLevelErrors.add(errorMessage);
        valid = false;
    }
    
    /**
     * Add a record-level warning.
     */
    public void addRecordWarning(String warningMessage) {
        recordLevelWarnings.add(warningMessage);
    }
    
    /**
     * Get total error count for this record.
     */
    public int getErrorCount() {
        return fieldErrors.size() + recordLevelErrors.size();
    }
    
    /**
     * Get total warning count for this record.
     */
    public int getWarningCount() {
        return fieldWarnings.size() + recordLevelWarnings.size();
    }
    
    /**
     * Check if record has critical errors.
     */
    public boolean hasCriticalErrors() {
        return fieldErrors.stream().anyMatch(error -> "CRITICAL".equals(error.getSeverity()));
    }
    
    /**
     * Get validation summary for this record.
     */
    public String getValidationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Record ").append(recordNumber).append(" Validation:\n");
        summary.append("- Status: ").append(valid ? "VALID" : "INVALID").append("\n");
        summary.append("- Field Errors: ").append(fieldErrors.size()).append("\n");
        summary.append("- Field Warnings: ").append(fieldWarnings.size()).append("\n");
        summary.append("- Record Errors: ").append(recordLevelErrors.size()).append("\n");
        summary.append("- Record Warnings: ").append(recordLevelWarnings.size()).append("\n");
        
        if (!fieldErrors.isEmpty()) {
            summary.append("- Field Error Details:\n");
            for (FieldValidationError error : fieldErrors) {
                summary.append("  * ").append(error.getFieldName()).append(": ").append(error.getErrorMessage()).append("\n");
            }
        }
        
        return summary.toString();
    }
}