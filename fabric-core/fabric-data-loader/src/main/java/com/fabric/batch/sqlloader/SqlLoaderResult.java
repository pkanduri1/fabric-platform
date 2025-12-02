package com.fabric.batch.sqlloader;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result object containing SQL*Loader execution details and statistics.
 * Provides comprehensive information about the loading operation for auditing and monitoring.
 */
@Data
@NoArgsConstructor
public class SqlLoaderResult {
    
    // Execution identifiers
    private String jobExecutionId;
    private String correlationId;
    private String configId;
    
    // Execution timing
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    
    // Execution status
    private boolean successful = false;
    private String executionStatus;
    private int returnCode;
    private int retryCount = 0;
    
    // File paths
    private String controlFilePath;
    private String logFilePath;
    private String badFilePath;
    private String discardFilePath;
    private String dataFilePath;
    
    // Record statistics
    private Long totalRecords;
    private Long successfulRecords;
    private Long rejectedRecords;
    private Long skippedRecords;
    private Long discardedRecords;
    
    // Performance metrics
    private Double throughputRecordsPerSec;
    private Double avgRecordProcessingTimeMs;
    private Long memoryUsageMb;
    private Double cpuUsagePercent;
    private Long ioReadMb;
    private Long ioWriteMb;
    
    // Error and warning information
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private String errorDetails;
    private String executionOutput;
    
    // SQL*Loader specific information
    private String sqlLoaderVersion;
    private String loadMethod;
    private boolean directPathUsed = false;
    private Integer parallelDegree;
    private Long bindArraySize;
    private Long readBufferSize;
    
    // Validation results
    private Integer validationErrors = 0;
    private Integer businessRuleErrors = 0;
    private Integer referentialIntegrityErrors = 0;
    private List<String> validationMessages = new ArrayList<>();
    
    // Security and compliance
    private boolean encryptionApplied = false;
    private boolean auditTrailGenerated = false;
    private String complianceStatus = "PENDING";
    private List<String> securityEvents = new ArrayList<>();
    
    // Additional metadata
    private Map<String, Object> additionalMetadata = new HashMap<>();
    private String executionEnvironment;
    private String hostName;
    private Long processId;
    
    /**
     * Calculate success rate as a percentage.
     */
    public double getSuccessRate() {
        if (totalRecords == null || totalRecords == 0) {
            return 0.0;
        }
        if (successfulRecords == null) {
            return 0.0;
        }
        return (double) successfulRecords / totalRecords * 100.0;
    }
    
    /**
     * Calculate error rate as a percentage.
     */
    public double getErrorRate() {
        if (totalRecords == null || totalRecords == 0) {
            return 0.0;
        }
        if (rejectedRecords == null) {
            return 0.0;
        }
        return (double) rejectedRecords / totalRecords * 100.0;
    }
    
    /**
     * Calculate throughput in records per second.
     */
    public void calculateThroughput() {
        if (durationMs != null && durationMs > 0 && successfulRecords != null && successfulRecords > 0) {
            throughputRecordsPerSec = (double) successfulRecords / (durationMs / 1000.0);
        }
    }
    
    /**
     * Calculate average record processing time in milliseconds.
     */
    public void calculateAvgProcessingTime() {
        if (durationMs != null && durationMs > 0 && totalRecords != null && totalRecords > 0) {
            avgRecordProcessingTimeMs = (double) durationMs / totalRecords;
        }
    }
    
    /**
     * Add an error message to the error list.
     */
    public void addError(String error) {
        if (error != null && !error.trim().isEmpty()) {
            errors.add(error.trim());
        }
    }
    
    /**
     * Add a warning message to the warning list.
     */
    public void addWarning(String warning) {
        if (warning != null && !warning.trim().isEmpty()) {
            warnings.add(warning.trim());
        }
    }
    
    /**
     * Add a validation message.
     */
    public void addValidationMessage(String message) {
        if (message != null && !message.trim().isEmpty()) {
            validationMessages.add(message.trim());
        }
    }
    
    /**
     * Add a security event.
     */
    public void addSecurityEvent(String event) {
        if (event != null && !event.trim().isEmpty()) {
            securityEvents.add(event.trim());
        }
    }
    
    /**
     * Add additional metadata.
     */
    public void addMetadata(String key, Object value) {
        if (key != null && value != null) {
            additionalMetadata.put(key, value);
        }
    }
    
    /**
     * Check if the execution had any errors.
     */
    public boolean hasErrors() {
        return !errors.isEmpty() || 
               (rejectedRecords != null && rejectedRecords > 0) ||
               (validationErrors != null && validationErrors > 0) ||
               (businessRuleErrors != null && businessRuleErrors > 0) ||
               (referentialIntegrityErrors != null && referentialIntegrityErrors > 0);
    }
    
    /**
     * Check if the execution had any warnings.
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    /**
     * Get total error count including all types of errors.
     */
    public int getTotalErrorCount() {
        int count = 0;
        if (rejectedRecords != null) count += rejectedRecords.intValue();
        if (validationErrors != null) count += validationErrors;
        if (businessRuleErrors != null) count += businessRuleErrors;
        if (referentialIntegrityErrors != null) count += referentialIntegrityErrors;
        return count;
    }
    
    /**
     * Check if the result meets acceptable quality thresholds.
     */
    public boolean meetsQualityThreshold(double maxErrorRatePercent) {
        return getErrorRate() <= maxErrorRatePercent;
    }
    
    /**
     * Check if the performance meets acceptable thresholds.
     */
    public boolean meetsPerformanceThreshold(double minThroughputRecordsPerSec) {
        return throughputRecordsPerSec != null && throughputRecordsPerSec >= minThroughputRecordsPerSec;
    }
    
    /**
     * Get a summary of the execution result.
     */
    public String getExecutionSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("SQL*Loader Execution Summary:\n");
        summary.append("- Job ID: ").append(jobExecutionId).append("\n");
        summary.append("- Status: ").append(successful ? "SUCCESS" : "FAILED").append("\n");
        summary.append("- Duration: ").append(durationMs != null ? durationMs + "ms" : "N/A").append("\n");
        summary.append("- Total Records: ").append(totalRecords != null ? totalRecords : "N/A").append("\n");
        summary.append("- Successful Records: ").append(successfulRecords != null ? successfulRecords : "N/A").append("\n");
        summary.append("- Rejected Records: ").append(rejectedRecords != null ? rejectedRecords : "N/A").append("\n");
        summary.append("- Success Rate: ").append(String.format("%.2f%%", getSuccessRate())).append("\n");
        summary.append("- Error Rate: ").append(String.format("%.2f%%", getErrorRate())).append("\n");
        
        if (throughputRecordsPerSec != null) {
            summary.append("- Throughput: ").append(String.format("%.2f records/sec", throughputRecordsPerSec)).append("\n");
        }
        
        if (hasErrors()) {
            summary.append("- Errors: ").append(errors.size()).append(" error(s) occurred\n");
        }
        
        if (hasWarnings()) {
            summary.append("- Warnings: ").append(warnings.size()).append(" warning(s) occurred\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Mark execution as completed and calculate derived metrics.
     */
    public void completeExecution() {
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        
        if (startTime != null && endTime != null && durationMs == null) {
            durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        }
        
        calculateThroughput();
        calculateAvgProcessingTime();
        
        // Set compliance status based on results
        if (hasErrors()) {
            complianceStatus = "NON_COMPLIANT";
        } else if (hasWarnings()) {
            complianceStatus = "COMPLIANT_WITH_WARNINGS";
        } else {
            complianceStatus = "COMPLIANT";
        }
    }
    
    /**
     * Check if retry is recommended based on error type and count.
     */
    public boolean shouldRetry(int maxRetries) {
        if (retryCount >= maxRetries) {
            return false;
        }
        
        // Don't retry if successful
        if (successful) {
            return false;
        }
        
        // Don't retry for certain types of errors (configuration errors, permission issues)
        for (String error : errors) {
            if (error.contains("ORA-00942") ||  // Table doesn't exist
                error.contains("ORA-00955") ||  // Name already exists
                error.contains("ORA-01031") ||  // Insufficient privileges
                error.contains("SQL*Loader-350")) {  // Syntax error in control file
                return false;
            }
        }
        
        // Retry for transient errors (connection issues, resource problems)
        for (String error : errors) {
            if (error.contains("ORA-00054") ||  // Resource busy
                error.contains("ORA-12170") ||  // Connection timeout
                error.contains("ORA-03113") ||  // End-of-file on communication channel
                error.contains("SQL*Loader-925")) {  // Unable to allocate memory
                return true;
            }
        }
        
        // Default: retry if error rate is not too high (< 50%)
        return getErrorRate() < 50.0;
    }
}