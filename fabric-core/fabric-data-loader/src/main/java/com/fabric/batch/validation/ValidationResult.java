package com.fabric.batch.validation;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Comprehensive validation result containing detailed statistics and error information.
 */
@Data
@NoArgsConstructor
public class ValidationResult {
    
    // Basic identification
    private String configId;
    private String correlationId;
    private String filePath;
    
    // Validation timing
    private LocalDateTime validationStartTime;
    private LocalDateTime validationEndTime;
    private Long validationDurationMs;
    
    // Validation status
    private ValidationStatus validationStatus;
    private boolean valid = true;
    
    // Record statistics
    private Long totalRecords = 0L;
    private Long validRecords = 0L;
    private Long invalidRecords = 0L;
    private Long skippedRecords = 0L;
    private Double successRatePercent = 0.0;
    
    // File information
    private Long fileSizeBytes;
    private String fileEncoding;
    private String fileFormat;
    
    // Error and warning information
    private List<ValidationError> errors = new ArrayList<>();
    private List<ValidationWarning> warnings = new ArrayList<>();
    private List<RecordValidationResult> recordErrors = new ArrayList<>();
    private String errorDetails;
    
    // Field-level error statistics
    private Map<String, Integer> fieldErrorCounts = new HashMap<>();
    private Map<String, Map<String, Integer>> fieldErrorCountsByType = new HashMap<>();
    
    // Rule execution statistics
    private Map<Long, Integer> ruleExecutionCounts = new HashMap<>();
    private Map<Long, Integer> ruleFailureCounts = new HashMap<>();
    private Map<String, Integer> ruleTypeFailureCounts = new HashMap<>();
    
    // Performance metrics
    private Double recordsPerSecond;
    private Long memoryUsageMb;
    private Integer threadsUsed = 1;
    
    // Business validation results
    private List<BusinessRuleResult> businessRuleResults = new ArrayList<>();
    
    // Data quality metrics
    private DataQualityMetrics dataQualityMetrics = new DataQualityMetrics();
    
    public enum ValidationStatus {
        PASSED,
        WARNING,
        FAILED,
        SKIPPED,
        IN_PROGRESS,
        CANCELLED
    }
    
    /**
     * Add a validation error.
     */
    public void addError(String errorCode, String errorMessage) {
        ValidationError error = new ValidationError();
        error.setErrorCode(errorCode);
        error.setErrorMessage(errorMessage);
        error.setTimestamp(LocalDateTime.now());
        errors.add(error);
        valid = false;
    }
    
    /**
     * Add a validation warning.
     */
    public void addWarning(String warningCode, String warningMessage) {
        ValidationWarning warning = new ValidationWarning();
        warning.setWarningCode(warningCode);
        warning.setWarningMessage(warningMessage);
        warning.setTimestamp(LocalDateTime.now());
        warnings.add(warning);
    }
    
    /**
     * Add a record validation error.
     */
    public void addRecordError(RecordValidationResult recordError) {
        recordErrors.add(recordError);
    }
    
    /**
     * Increment field error count.
     */
    public void incrementFieldErrorCount(String fieldName, String ruleType) {
        fieldErrorCounts.merge(fieldName, 1, Integer::sum);
        
        fieldErrorCountsByType.computeIfAbsent(fieldName, k -> new HashMap<>())
                              .merge(ruleType, 1, Integer::sum);
    }
    
    /**
     * Get total error count.
     */
    public int getErrorCount() {
        return errors.size() + recordErrors.size();
    }
    
    /**
     * Get total warning count.
     */
    public int getWarningCount() {
        return warnings.size();
    }
    
    /**
     * Check if validation is successful (no errors).
     */
    public boolean isValid() {
        return valid && getErrorCount() == 0;
    }
    
    /**
     * Get validation summary as string.
     */
    public String getValidationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Validation Summary:\n");
        summary.append("- Status: ").append(validationStatus).append("\n");
        summary.append("- Total Records: ").append(totalRecords).append("\n");
        summary.append("- Valid Records: ").append(validRecords).append("\n");
        summary.append("- Invalid Records: ").append(invalidRecords).append("\n");
        summary.append("- Success Rate: ").append(String.format("%.2f%%", successRatePercent)).append("\n");
        summary.append("- Errors: ").append(getErrorCount()).append("\n");
        summary.append("- Warnings: ").append(getWarningCount()).append("\n");
        
        if (validationDurationMs != null) {
            summary.append("- Duration: ").append(validationDurationMs).append("ms\n");
        }
        
        if (recordsPerSecond != null) {
            summary.append("- Processing Rate: ").append(String.format("%.2f records/sec", recordsPerSecond)).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Calculate processing rate.
     */
    public void calculateProcessingRate() {
        if (validationDurationMs != null && validationDurationMs > 0 && totalRecords > 0) {
            recordsPerSecond = (double) totalRecords / (validationDurationMs / 1000.0);
        }
    }
    
    /**
     * Get field with most errors.
     */
    public Map.Entry<String, Integer> getFieldWithMostErrors() {
        return fieldErrorCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
    }
    
    /**
     * Get most common error type.
     */
    public String getMostCommonErrorType() {
        return ruleTypeFailureCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
    
    /**
     * Check if error threshold is exceeded.
     */
    public boolean isErrorThresholdExceeded(double maxErrorRatePercent) {
        if (totalRecords == 0) return false;
        double errorRate = (double) invalidRecords / totalRecords * 100.0;
        return errorRate > maxErrorRatePercent;
    }
    
    @Data
    @NoArgsConstructor
    public static class ValidationError {
        private String errorCode;
        private String errorMessage;
        private String fieldName;
        private Long recordNumber;
        private String severity = "ERROR";
        private LocalDateTime timestamp;
        private Map<String, Object> additionalData = new HashMap<>();
    }
    
    @Data
    @NoArgsConstructor
    public static class ValidationWarning {
        private String warningCode;
        private String warningMessage;
        private String fieldName;
        private Long recordNumber;
        private LocalDateTime timestamp;
        private Map<String, Object> additionalData = new HashMap<>();
    }
    
    @Data
    @NoArgsConstructor
    public static class BusinessRuleResult {
        private String ruleName;
        private String ruleDescription;
        private boolean passed;
        private String resultMessage;
        private Map<String, Object> resultData = new HashMap<>();
        private LocalDateTime executionTime;
        private Long executionDurationMs;
    }
    
    @Data
    @NoArgsConstructor
    public static class DataQualityMetrics {
        private Double completenessScore = 0.0;  // % of non-null values
        private Double accuracyScore = 0.0;      // % of values passing validation
        private Double consistencyScore = 0.0;   // % of values consistent with patterns
        private Double validityScore = 0.0;      // % of values in valid format
        private Double uniquenessScore = 0.0;    // % of unique values where expected
        private Double overallQualityScore = 0.0; // Weighted average of all scores
        
        public void calculateOverallScore() {
            overallQualityScore = (completenessScore + accuracyScore + consistencyScore + 
                                 validityScore + uniquenessScore) / 5.0;
        }
    }
}