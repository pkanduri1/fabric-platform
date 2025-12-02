package com.fabric.batch.validation;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Summary of validation results for a complete record or batch.
 */
@Data
@NoArgsConstructor
public class ValidationSummary {
    
    private LocalDateTime validationTimestamp = LocalDateTime.now();
    private int totalFields;
    private int totalErrors;
    private int totalWarnings;
    private int validFields;
    private boolean thresholdExceeded = false;
    private String batchId;
    private String correlationId;
    
    // Field-level validation results
    private Map<String, List<FieldValidationResult>> fieldResults = new HashMap<>();
    
    // Summary lists for quick access
    private List<FieldValidationResult> allErrors = new ArrayList<>();
    private List<FieldValidationResult> allWarnings = new ArrayList<>();
    
    /**
     * Add validation results for a field.
     */
    public void addFieldResults(String fieldName, List<FieldValidationResult> results) {
        fieldResults.put(fieldName, results);
        
        for (FieldValidationResult result : results) {
            if (!result.isValid()) {
                allErrors.add(result);
            } else if (result.hasWarnings()) {
                allWarnings.add(result);
            }
        }
        
        // Update counts
        updateCounts();
    }
    
    /**
     * Check if validation passed (no errors).
     */
    public boolean isValid() {
        return totalErrors == 0 && !thresholdExceeded;
    }
    
    /**
     * Check if there are any warnings.
     */
    public boolean hasWarnings() {
        return totalWarnings > 0;
    }
    
    /**
     * Get validation success rate as percentage.
     */
    public double getSuccessRate() {
        if (totalFields == 0) return 100.0;
        return ((double) validFields / totalFields) * 100.0;
    }
    
    /**
     * Get error rate as percentage.
     */
    public double getErrorRate() {
        if (totalFields == 0) return 0.0;
        return ((double) totalErrors / totalFields) * 100.0;
    }
    
    /**
     * Get warning rate as percentage.
     */
    public double getWarningRate() {
        if (totalFields == 0) return 0.0;
        return ((double) totalWarnings / totalFields) * 100.0;
    }
    
    /**
     * Get validation results for a specific field.
     */
    public List<FieldValidationResult> getFieldResults(String fieldName) {
        return fieldResults.getOrDefault(fieldName, new ArrayList<>());
    }
    
    /**
     * Get all field names that were validated.
     */
    public List<String> getValidatedFields() {
        return new ArrayList<>(fieldResults.keySet());
    }
    
    /**
     * Get field names that have errors.
     */
    public List<String> getFieldsWithErrors() {
        List<String> fieldsWithErrors = new ArrayList<>();
        for (Map.Entry<String, List<FieldValidationResult>> entry : fieldResults.entrySet()) {
            boolean hasErrors = entry.getValue().stream().anyMatch(result -> !result.isValid());
            if (hasErrors) {
                fieldsWithErrors.add(entry.getKey());
            }
        }
        return fieldsWithErrors;
    }
    
    /**
     * Get field names that have warnings.
     */
    public List<String> getFieldsWithWarnings() {
        List<String> fieldsWithWarnings = new ArrayList<>();
        for (Map.Entry<String, List<FieldValidationResult>> entry : fieldResults.entrySet()) {
            boolean hasWarnings = entry.getValue().stream().anyMatch(FieldValidationResult::hasWarnings);
            if (hasWarnings) {
                fieldsWithWarnings.add(entry.getKey());
            }
        }
        return fieldsWithWarnings;
    }
    
    /**
     * Get summary report as string.
     */
    public String getSummaryReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== VALIDATION SUMMARY ===\n");
        report.append(String.format("Validation Time: %s\n", validationTimestamp));
        report.append(String.format("Total Fields: %d\n", totalFields));
        report.append(String.format("Valid Fields: %d\n", validFields));
        report.append(String.format("Errors: %d\n", totalErrors));
        report.append(String.format("Warnings: %d\n", totalWarnings));
        report.append(String.format("Success Rate: %.2f%%\n", getSuccessRate()));
        report.append(String.format("Error Rate: %.2f%%\n", getErrorRate()));
        report.append(String.format("Threshold Exceeded: %s\n", thresholdExceeded ? "YES" : "NO"));
        report.append(String.format("Overall Status: %s\n", isValid() ? "PASSED" : "FAILED"));
        
        if (!allErrors.isEmpty()) {
            report.append("\n=== ERRORS ===\n");
            for (FieldValidationResult error : allErrors) {
                report.append(String.format("- %s: %s\n", error.getFieldName(), error.getErrorMessage()));
            }
        }
        
        if (!allWarnings.isEmpty()) {
            report.append("\n=== WARNINGS ===\n");
            for (FieldValidationResult warning : allWarnings) {
                report.append(String.format("- %s: %s\n", warning.getFieldName(), warning.getWarningMessage()));
            }
        }
        
        report.append("========================\n");
        return report.toString();
    }
    
    /**
     * Update internal counts based on current results.
     */
    private void updateCounts() {
        totalErrors = allErrors.size();
        totalWarnings = allWarnings.size();
        validFields = totalFields - getFieldsWithErrors().size();
    }
    
    /**
     * Merge another validation summary into this one.
     */
    public void merge(ValidationSummary other) {
        this.totalFields += other.totalFields;
        this.fieldResults.putAll(other.fieldResults);
        this.allErrors.addAll(other.allErrors);
        this.allWarnings.addAll(other.allWarnings);
        this.thresholdExceeded = this.thresholdExceeded || other.thresholdExceeded;
        
        updateCounts();
    }
    
    /**
     * Create a simple validation summary for quick reporting.
     */
    public static ValidationSummary createSimple(int totalFields, int errors, int warnings) {
        ValidationSummary summary = new ValidationSummary();
        summary.setTotalFields(totalFields);
        summary.setTotalErrors(errors);
        summary.setTotalWarnings(warnings);
        summary.setValidFields(totalFields - errors);
        return summary;
    }
}