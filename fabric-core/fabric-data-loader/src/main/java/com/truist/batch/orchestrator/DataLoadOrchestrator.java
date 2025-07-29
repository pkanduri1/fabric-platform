package com.truist.batch.orchestrator;

import com.truist.batch.audit.AuditTrailManager;
import com.truist.batch.config.DataLoadConfigurationService;
import com.truist.batch.entity.DataLoadConfigEntity;
import com.truist.batch.entity.ProcessingJobEntity;
import com.truist.batch.entity.ValidationRuleEntity;
import com.truist.batch.repository.ProcessingJobRepository;
import com.truist.batch.sqlloader.SqlLoaderExecutor;
import com.truist.batch.threshold.ErrorThresholdManager;
import com.truist.batch.validation.ComprehensiveValidationEngine;
import com.truist.batch.validation.ValidationSummary;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Main orchestrator for data loading operations.
 * Integrates all components: configuration, validation, SQL*Loader, audit, and thresholds.
 */
@Slf4j
@Service
public class DataLoadOrchestrator {
    
    @Autowired
    private DataLoadConfigurationService configurationService;
    
    @Autowired
    private ComprehensiveValidationEngine validationEngine;
    
    @Autowired
    private ErrorThresholdManager thresholdManager;
    
    @Autowired
    private AuditTrailManager auditTrailManager;
    
    @Autowired
    private SqlLoaderExecutor sqlLoaderExecutor;
    
    @Autowired
    private ProcessingJobRepository processingJobRepository;
    
    /**
     * Execute complete data loading process for a file.
     */
    @Transactional
    public DataLoadResult executeDataLoad(String configId, String fileName, String filePath) {
        log.info("Starting data load execution - Config: {}, File: {}", configId, fileName);
        
        DataLoadResult result = new DataLoadResult();
        result.setConfigId(configId);
        result.setFileName(fileName);
        result.setFilePath(filePath);
        result.setStartTime(LocalDateTime.now());
        
        String correlationId = null;
        String jobExecutionId = UUID.randomUUID().toString();
        
        try {
            // 1. Load configuration
            log.debug("Step 1: Loading configuration for {}", configId);
            Optional<DataLoadConfigEntity> configOpt = configurationService.getConfiguration(configId);
            if (configOpt.isEmpty()) {
                throw new RuntimeException("Configuration not found: " + configId);
            }
            
            DataLoadConfigEntity config = configOpt.get();
            result.setConfiguration(config);
            
            // 2. Initialize audit trail
            log.debug("Step 2: Initializing audit trail");
            correlationId = auditTrailManager.auditDataLoadStart(
                configId, jobExecutionId, fileName, 
                config.getSourceSystem(), config.getTargetTable());
            result.setCorrelationId(correlationId);
            
            // 3. Create processing job record
            log.debug("Step 3: Creating processing job record");
            ProcessingJobEntity job = createProcessingJob(config, jobExecutionId, correlationId, fileName, filePath);
            processingJobRepository.save(job);
            result.setJobExecutionId(jobExecutionId);
            
            // 4. Validate file exists and is readable
            log.debug("Step 4: Validating file access");
            validateFileAccess(filePath);
            
            // 5. Load validation rules
            log.debug("Step 5: Loading validation rules");
            List<ValidationRuleEntity> validationRules = configurationService.getValidationRules(configId);
            result.setValidationRulesCount(validationRules.size());
            
            // 6. Process file with validation
            log.debug("Step 6: Processing file with validation");
            FileProcessingResult processingResult = processFileWithValidation(
                config, validationRules, filePath, correlationId);
            result.setProcessingResult(processingResult);
            
            // 7. Check error thresholds
            log.debug("Step 7: Checking error thresholds");
            ErrorThresholdManager.ThresholdCheckResult thresholdCheck = 
                thresholdManager.checkThreshold(configId, config, processingResult.getValidationSummary());
            result.setThresholdCheck(thresholdCheck);
            
            // 8. Execute SQL*Loader if validation passed and threshold not exceeded
            if (!thresholdCheck.isThresholdExceeded() && processingResult.canProceedToLoad()) {
                log.debug("Step 8: Executing SQL*Loader");
                SqlLoaderResult loaderResult = executeSqlLoader(config, filePath, correlationId);
                result.setLoaderResult(loaderResult);
                
                // 9. Post-load validation if configured
                if ("Y".equals(config.getPostLoadValidation())) {
                    log.debug("Step 9: Post-load validation");
                    performPostLoadValidation(config, loaderResult, correlationId);
                }
            } else {
                log.warn("Skipping SQL*Loader execution due to validation errors or threshold exceeded");
                result.setSkippedLoad(true);
                result.setSkipReason(thresholdCheck.getMessage());
            }
            
            // 10. Update job status and audit completion
            log.debug("Step 10: Completing audit trail");
            completeDataLoadAudit(job, result, correlationId);
            
            result.setSuccess(true);
            result.setEndTime(LocalDateTime.now());
            
            log.info("Data load execution completed successfully - Config: {}, Correlation: {}", 
                configId, correlationId);
            
        } catch (Exception e) {
            log.error("Data load execution failed - Config: {}, Error: {}", configId, e.getMessage(), e);
            
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setEndTime(LocalDateTime.now());
            
            // Audit error event
            if (correlationId != null) {
                auditTrailManager.auditErrorEvent(correlationId, "DATA_LOAD_ERROR", 
                    e.getMessage(), "SYS_ERROR", getStackTrace(e), 0);
                auditTrailManager.auditDataLoadComplete(correlationId, false, 
                    result.getTotalRecords(), result.getSuccessfulRecords(), 
                    result.getFailedRecords(), result.getDurationMs());
            }
        }
        
        return result;
    }
    
    /**
     * Process file with comprehensive validation.
     */
    private FileProcessingResult processFileWithValidation(DataLoadConfigEntity config, 
                                                         List<ValidationRuleEntity> validationRules,
                                                         String filePath, String correlationId) throws IOException {
        FileProcessingResult result = new FileProcessingResult();
        ValidationSummary overallSummary = new ValidationSummary();
        overallSummary.setCorrelationId(correlationId);
        
        // Group validation rules by field name
        Map<String, List<ValidationRuleEntity>> rulesByField = new HashMap<>();
        for (ValidationRuleEntity rule : validationRules) {
            rulesByField.computeIfAbsent(rule.getFieldName(), k -> new ArrayList<>()).add(rule);
        }
        
        long recordCount = 0;
        long validRecords = 0;
        long errorRecords = 0;
        long warningRecords = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            int headerRows = config.getHeaderRows() != null ? config.getHeaderRows() : 1;
            String delimiter = config.getFieldDelimiter();
            
            // Skip header rows
            for (int i = 0; i < headerRows && (line = reader.readLine()) != null; i++) {
                lineNumber++;
                log.debug("Skipped header row {}: {}", lineNumber, line);
            }
            
            // Process data rows
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                recordCount++;
                
                try {
                    // Parse record fields
                    String[] fields = line.split(Pattern.quote(delimiter), -1);
                    Map<String, String> fieldValues = parseFieldsFromRecord(fields, config);
                    
                    // Validate record
                    ValidationSummary recordSummary = validationEngine.validateFields(
                        fieldValues, rulesByField, config.getMaxErrors());
                    
                    if (recordSummary.isValid()) {
                        validRecords++;
                    } else {
                        errorRecords++;
                    }
                    
                    if (recordSummary.hasWarnings()) {
                        warningRecords++;
                    }
                    
                    overallSummary.merge(recordSummary);
                    
                    // Check if we should continue processing based on threshold
                    if (!thresholdManager.shouldContinueProcessing(config.getConfigId(), 
                            (int)errorRecords, config.getMaxErrors())) {
                        log.warn("Stopping file processing due to error threshold exceeded at record {}", recordCount);
                        break;
                    }
                    
                    // Audit file processing progress every 1000 records
                    if (recordCount % 1000 == 0) {
                        auditTrailManager.auditFileProcessing(correlationId, "RECORD_VALIDATION",
                            "File processing progress", recordCount, validRecords, errorRecords);
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing record {} in file {}: {}", lineNumber, filePath, e.getMessage());
                    errorRecords++;
                    
                    auditTrailManager.auditErrorEvent(correlationId, "RECORD_PROCESSING_ERROR",
                        e.getMessage(), "PARSE_ERROR", getStackTrace(e), 1);
                }
            }
        }
        
        // Final audit of file processing
        auditTrailManager.auditFileProcessing(correlationId, "FILE_PROCESSING_COMPLETE",
            "Complete file processing summary", recordCount, validRecords, errorRecords);
        
        result.setValidationSummary(overallSummary);
        result.setTotalRecords(recordCount);
        result.setValidRecords(validRecords);
        result.setErrorRecords(errorRecords);
        result.setWarningRecords(warningRecords);
        
        log.info("File processing completed - Total: {}, Valid: {}, Errors: {}, Warnings: {}", 
            recordCount, validRecords, errorRecords, warningRecords);
        
        return result;
    }
    
    /**
     * Parse fields from record based on configuration.
     */
    private Map<String, String> parseFieldsFromRecord(String[] fields, DataLoadConfigEntity config) {
        Map<String, String> fieldValues = new HashMap<>();
        
        // For now, create generic field names - in production, this would come from field mapping configuration
        for (int i = 0; i < fields.length; i++) {
            String fieldName = "FIELD_" + (i + 1);
            String fieldValue = fields[i].trim();
            
            // Handle optional string delimiters
            if (fieldValue.startsWith("\"") && fieldValue.endsWith("\"") && fieldValue.length() > 1) {
                fieldValue = fieldValue.substring(1, fieldValue.length() - 1);
            }
            
            fieldValues.put(fieldName, fieldValue);
        }
        
        return fieldValues;
    }
    
    /**
     * Execute SQL*Loader for data loading.
     */
    private SqlLoaderResult executeSqlLoader(DataLoadConfigEntity config, String filePath, String correlationId) {
        SqlLoaderResult result = new SqlLoaderResult();
        
        try {
            log.info("Executing SQL*Loader for config: {}", config.getConfigId());
            
            // Execute SQL*Loader
            result = sqlLoaderExecutor.executeLoad(config, filePath, correlationId);
            
            // Audit SQL*Loader execution
            auditTrailManager.auditSqlLoaderExecution(correlationId, result.getControlFilePath(),
                result.getReturnCode(), result.getTotalRecords(), result.getSuccessfulRecords(),
                result.getRejectedRecords(), result.getErrorMessage());
            
        } catch (Exception e) {
            log.error("SQL*Loader execution failed: {}", e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            
            auditTrailManager.auditErrorEvent(correlationId, "SQL_LOADER_ERROR",
                e.getMessage(), "LOADER_ERROR", getStackTrace(e), 0);
        }
        
        return result;
    }
    
    /**
     * Perform post-load validation.
     */
    private void performPostLoadValidation(DataLoadConfigEntity config, SqlLoaderResult loaderResult, String correlationId) {
        try {
            log.info("Performing post-load validation for config: {}", config.getConfigId());
            
            // Audit post-load validation
            auditTrailManager.auditValidationResult(correlationId, "POST_LOAD_VALIDATION",
                loaderResult.isSuccess(), loaderResult.getTotalRecords(), 
                loaderResult.getRejectedRecords(), "Post-load data integrity checks");
            
        } catch (Exception e) {
            log.error("Post-load validation failed: {}", e.getMessage());
            auditTrailManager.auditErrorEvent(correlationId, "POST_LOAD_VALIDATION_ERROR",
                e.getMessage(), "VALIDATION_ERROR", getStackTrace(e), 0);
        }
    }
    
    /**
     * Create processing job entity.
     */
    private ProcessingJobEntity createProcessingJob(DataLoadConfigEntity config, String jobExecutionId, 
                                                   String correlationId, String fileName, String filePath) {
        ProcessingJobEntity job = new ProcessingJobEntity();
        job.setJobExecutionId(jobExecutionId);
        job.setConfigId(config.getConfigId());
        job.setCorrelationId(correlationId);
        job.setFileName(fileName);
        job.setFilePath(filePath);
        job.setJobStatus(ProcessingJobEntity.JobStatus.SUBMITTED);
        job.setExecutionMode(ProcessingJobEntity.ExecutionMode.BATCH);
        job.setPriority(ProcessingJobEntity.Priority.NORMAL);
        job.setCreatedBy("SYSTEM");
        
        return job;
    }
    
    /**
     * Complete audit trail for data loading operation.
     */
    private void completeDataLoadAudit(ProcessingJobEntity job, DataLoadResult result, String correlationId) {
        try {
            // Update job status
            job.setCompletedDate(LocalDateTime.now());
            job.setJobStatus(result.isSuccess() ? 
                ProcessingJobEntity.JobStatus.COMPLETED : ProcessingJobEntity.JobStatus.FAILED);
            job.setTotalRecords((long) result.getTotalRecords());
            job.setSuccessfulRecords((long) result.getSuccessfulRecords());
            job.setFailedRecords((long) result.getFailedRecords());
            job.setDurationMs(result.getDurationMs());
            
            if (!result.isSuccess()) {
                job.setErrorMessage(result.getErrorMessage());
            }
            
            processingJobRepository.save(job);
            
            // Complete audit trail
            auditTrailManager.auditDataLoadComplete(correlationId, result.isSuccess(),
                result.getTotalRecords(), result.getSuccessfulRecords(), 
                result.getFailedRecords(), result.getDurationMs());
            
        } catch (Exception e) {
            log.error("Error completing audit trail: {}", e.getMessage());
        }
    }
    
    /**
     * Validate file access.
     */
    private void validateFileAccess(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        
        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + filePath);
        }
        
        if (!Files.isReadable(path)) {
            throw new IOException("File is not readable: " + filePath);
        }
        
        if (Files.size(path) == 0) {
            throw new IOException("File is empty: " + filePath);
        }
    }
    
    /**
     * Get stack trace as string.
     */
    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Data load result wrapper.
     */
    @Data
    public static class DataLoadResult {
        private String configId;
        private String correlationId;
        private String jobExecutionId;
        private String fileName;
        private String filePath;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean success;
        private String errorMessage;
        private DataLoadConfigEntity configuration;
        private int validationRulesCount;
        private FileProcessingResult processingResult;
        private ErrorThresholdManager.ThresholdCheckResult thresholdCheck;
        private SqlLoaderResult loaderResult;
        private boolean skippedLoad;
        private String skipReason;
        
        public long getDurationMs() {
            if (startTime != null && endTime != null) {
                return java.time.Duration.between(startTime, endTime).toMillis();
            }
            return 0;
        }
        
        public long getTotalRecords() {
            return processingResult != null ? processingResult.getTotalRecords() : 0;
        }
        
        public long getSuccessfulRecords() {
            return processingResult != null ? processingResult.getValidRecords() : 0;
        }
        
        public long getFailedRecords() {
            return processingResult != null ? processingResult.getErrorRecords() : 0;
        }
    }
    
    /**
     * File processing result.
     */
    @Data
    public static class FileProcessingResult {
        private ValidationSummary validationSummary;
        private long totalRecords;
        private long validRecords;
        private long errorRecords;
        private long warningRecords;
        
        public boolean canProceedToLoad() {
            return validationSummary != null && validationSummary.isValid() && !validationSummary.isThresholdExceeded();
        }
        
        public double getSuccessRate() {
            return totalRecords > 0 ? (double) validRecords / totalRecords * 100.0 : 0.0;
        }
    }
    
    /**
     * SQL*Loader result placeholder.
     */
    @Data
    public static class SqlLoaderResult {
        private boolean success;
        private int returnCode;
        private String controlFilePath;
        private String logFilePath;
        private String badFilePath;
        private long totalRecords;
        private long successfulRecords;
        private long rejectedRecords;
        private String errorMessage;
        private long executionTimeMs;
    }
}