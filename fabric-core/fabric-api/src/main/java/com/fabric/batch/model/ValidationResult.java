package com.fabric.batch.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of a validation operation.
 * 
 * Contains validation status, error messages, and warning messages.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    
    /**
     * Whether the validation passed (true) or failed (false).
     */
    private boolean valid;
    
    /**
     * List of error messages (causes validation to fail).
     */
    private List<String> errors;
    
    /**
     * List of warning messages (doesn't cause validation to fail).
     */
    private List<String> warnings;
    
    /**
     * Constructor that initializes with validation status.
     * 
     * @param valid Initial validation status
     */
    public ValidationResult(boolean valid) {
        this.valid = valid;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }
    
    /**
     * Constructor that initializes with validation status and errors.
     * 
     * @param valid Initial validation status
     * @param errors List of error messages
     */
    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.warnings = new ArrayList<>();
    }
    
    /**
     * Constructor for legacy compatibility with multiple parameters.
     * Note: This appears to be a legacy constructor - some parameters are ignored.
     * 
     * @param valid Initial validation status
     * @param errors List of error messages
     * @param warnings List of warning messages
     * @param unused1 Unused parameter (for compatibility)
     * @param unused2 Unused parameter (for compatibility)
     * @param unused3 Unused parameter (for compatibility)
     * @param unused4 Unused parameter (for compatibility)
     */
    public ValidationResult(boolean valid, List<String> errors, List<String> warnings, 
                           List<String> unused1, List<String> unused2, List<String> unused3, List<String> unused4) {
        this.valid = valid;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }
    
    /**
     * Adds an error message and sets validation status to false.
     * 
     * @param error Error message to add
     */
    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
        this.valid = false; // Any error makes validation fail
    }
    
    /**
     * Adds a warning message (doesn't affect validation status).
     * 
     * @param warning Warning message to add
     */
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }
    
    /**
     * Checks if there are any errors.
     * 
     * @return true if there are errors
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    /**
     * Checks if there are any warnings.
     * 
     * @return true if there are warnings
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
    
    /**
     * Gets the total count of errors.
     * 
     * @return Number of errors
     */
    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }
    
    /**
     * Gets the total count of warnings.
     * 
     * @return Number of warnings
     */
    public int getWarningCount() {
        return warnings != null ? warnings.size() : 0;
    }
    
    /**
     * Gets a summary of the validation result.
     * 
     * @return Summary string containing error and warning counts
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        if (valid) {
            summary.append("Validation passed");
        } else {
            summary.append("Validation failed");
        }
        
        int errorCount = getErrorCount();
        int warningCount = getWarningCount();
        
        if (errorCount > 0 || warningCount > 0) {
            summary.append(" (");
            if (errorCount > 0) {
                summary.append(errorCount).append(" error").append(errorCount > 1 ? "s" : "");
                if (warningCount > 0) {
                    summary.append(", ");
                }
            }
            if (warningCount > 0) {
                summary.append(warningCount).append(" warning").append(warningCount > 1 ? "s" : "");
            }
            summary.append(")");
        }
        
        return summary.toString();
    }
}