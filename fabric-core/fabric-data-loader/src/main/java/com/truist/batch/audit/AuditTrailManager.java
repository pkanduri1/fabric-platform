package com.truist.batch.audit;

import com.truist.batch.entity.DataLoadAuditEntity;
import com.truist.batch.entity.ProcessingJobEntity;
import com.truist.batch.repository.DataLoadAuditRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive audit trail manager for data loading operations.
 * Provides complete data lineage tracking and compliance reporting capabilities.
 */
@Slf4j
@Component
public class AuditTrailManager {
    
    @Autowired
    private DataLoadAuditRepository auditRepository;
    
    @Value("${application.name:fabric-data-loader}")
    private String applicationName;
    
    @Value("${application.version:1.0.0}")
    private String applicationVersion;
    
    @Value("${spring.profiles.active:default}")
    private String environment;
    
    @Value("${audit.async.enabled:true}")
    private boolean asyncAuditEnabled;
    
    // Cache for correlation tracking
    private final Map<String, AuditContext> auditContextCache = new ConcurrentHashMap<>();
    
    // System information cache
    private String hostName;
    private long processId;
    
    public AuditTrailManager() {
        initializeSystemInfo();
    }
    
    /**
     * Create audit entry for data loading operation start.
     */
    public String auditDataLoadStart(String configId, String jobExecutionId, String fileName, 
                                   String sourceSystem, String targetTable) {
        String correlationId = generateCorrelationId();
        
        DataLoadAuditEntity audit = createBaseAuditEntity(configId, jobExecutionId, correlationId);
        audit.setAuditType(DataLoadAuditEntity.AuditType.DATA_LINEAGE);
        audit.setEventName("DATA_LOAD_START");
        audit.setEventDescription("Data loading operation started");
        audit.setSourceSystem(sourceSystem);
        audit.setTargetTable(targetTable);
        audit.setFileName(fileName);
        audit.setDataSource(fileName);
        audit.setDataDestination(targetTable);
        
        // Create audit context for correlation tracking
        AuditContext context = new AuditContext(correlationId, configId, jobExecutionId);
        context.setStartTime(LocalDateTime.now());
        auditContextCache.put(correlationId, context);
        
        saveAuditEntry(audit);
        
        log.info("Data load audit started - Correlation ID: {}, Job: {}", correlationId, jobExecutionId);
        return correlationId;
    }
    
    /**
     * Create audit entry for file processing step.
     */
    public void auditFileProcessing(String correlationId, String stepName, String stepDescription,
                                  long recordCount, long processedCount, long errorCount) {
        DataLoadAuditEntity audit = createAuditFromContext(correlationId);
        if (audit == null) return;
        
        audit.setAuditType(DataLoadAuditEntity.AuditType.DATA_TRANSFORMATION);
        audit.setEventName("FILE_PROCESSING_" + stepName.toUpperCase());
        audit.setEventDescription(stepDescription);
        audit.setRecordCount(recordCount);
        audit.setProcessedCount(processedCount);
        audit.setErrorCount(errorCount);
        
        // Calculate data quality score
        if (recordCount > 0) {
            double qualityScore = (double) (processedCount - errorCount) / recordCount * 100.0;
            audit.setDataQualityScore(qualityScore);
        }
        
        saveAuditEntry(audit);
        
        log.debug("File processing audit - Correlation: {}, Step: {}, Records: {}, Errors: {}", 
                 correlationId, stepName, recordCount, errorCount);
    }
    
    /**
     * Create audit entry for validation results.
     */
    public void auditValidationResult(String correlationId, String validationType, 
                                    boolean validationPassed, long totalRecords, long errorCount,
                                    String validationRules) {
        DataLoadAuditEntity audit = createAuditFromContext(correlationId);
        if (audit == null) return;
        
        audit.setAuditType(DataLoadAuditEntity.AuditType.VALIDATION_RESULT);
        audit.setEventName("VALIDATION_" + validationType.toUpperCase());
        audit.setEventDescription("Data validation " + (validationPassed ? "passed" : "failed"));
        audit.setRecordCount(totalRecords);
        audit.setErrorCount(errorCount);
        audit.setValidationRulesApplied(validationRules);
        
        // Set compliance status based on validation result
        audit.setComplianceStatus(validationPassed && errorCount == 0 ? 
            DataLoadAuditEntity.ComplianceStatus.COMPLIANT : 
            DataLoadAuditEntity.ComplianceStatus.NON_COMPLIANT);
        
        // Calculate data quality score
        if (totalRecords > 0) {
            double qualityScore = (double) (totalRecords - errorCount) / totalRecords * 100.0;
            audit.setDataQualityScore(qualityScore);
        }
        
        saveAuditEntry(audit);
        
        log.info("Validation audit - Correlation: {}, Type: {}, Passed: {}, Errors: {}", 
                correlationId, validationType, validationPassed, errorCount);
    }
    
    /**
     * Create audit entry for business rule execution.
     */
    public void auditBusinessRuleExecution(String correlationId, String ruleName, String ruleDescription,
                                         boolean rulePassed, long recordsAffected, String businessRules) {
        DataLoadAuditEntity audit = createAuditFromContext(correlationId);
        if (audit == null) return;
        
        audit.setAuditType(DataLoadAuditEntity.AuditType.BUSINESS_RULE_EXECUTION);
        audit.setEventName("BUSINESS_RULE_" + ruleName.toUpperCase().replaceAll("\\s+", "_"));
        audit.setEventDescription(ruleDescription);
        audit.setRecordCount(recordsAffected);
        audit.setBusinessRulesApplied(businessRules);
        
        if (!rulePassed) {
            audit.setErrorCount(recordsAffected);
            audit.setComplianceStatus(DataLoadAuditEntity.ComplianceStatus.NON_COMPLIANT);
        }
        
        saveAuditEntry(audit);
        
        log.info("Business rule audit - Correlation: {}, Rule: {}, Passed: {}, Records: {}", 
                correlationId, ruleName, rulePassed, recordsAffected);
    }
    
    /**
     * Create audit entry for SQL*Loader execution.
     */
    public void auditSqlLoaderExecution(String correlationId, String controlFile, 
                                      int returnCode, long totalRecords, long successfulRecords,
                                      long rejectedRecords, String errorMessage) {
        DataLoadAuditEntity audit = createAuditFromContext(correlationId);
        if (audit == null) return;
        
        audit.setAuditType(DataLoadAuditEntity.AuditType.DATABASE_OPERATION);
        audit.setEventName("SQL_LOADER_EXECUTION");
        audit.setEventDescription("SQL*Loader data loading operation");
        audit.setRecordCount(totalRecords);
        audit.setProcessedCount(successfulRecords);
        audit.setErrorCount(rejectedRecords);
        
        if (returnCode != 0) {
            audit.setErrorCode(String.valueOf(returnCode));
            audit.setErrorMessage(errorMessage);
            audit.setComplianceStatus(DataLoadAuditEntity.ComplianceStatus.NON_COMPLIANT);
        }
        
        // Add SQL*Loader specific metadata
        audit.setAdditionalMetadata(String.format(
            "{\"controlFile\":\"%s\",\"returnCode\":%d,\"successfulRecords\":%d,\"rejectedRecords\":%d}",
            controlFile, returnCode, successfulRecords, rejectedRecords));
        
        saveAuditEntry(audit);
        
        log.info("SQL*Loader audit - Correlation: {}, Return Code: {}, Total: {}, Success: {}, Rejected: {}", 
                correlationId, returnCode, totalRecords, successfulRecords, rejectedRecords);
    }
    
    /**
     * Create audit entry for data transformation.
     */
    public void auditDataTransformation(String correlationId, String transformationType,
                                      String transformationDescription, long recordsTransformed,
                                      String transformationRules) {
        DataLoadAuditEntity audit = createAuditFromContext(correlationId);
        if (audit == null) return;
        
        audit.setAuditType(DataLoadAuditEntity.AuditType.DATA_TRANSFORMATION);
        audit.setEventName("DATA_TRANSFORMATION_" + transformationType.toUpperCase());
        audit.setEventDescription(transformationDescription);
        audit.setRecordCount(recordsTransformed);
        audit.setProcessedCount(recordsTransformed);
        audit.setTransformationApplied(transformationRules);
        
        saveAuditEntry(audit);
        
        log.debug("Data transformation audit - Correlation: {}, Type: {}, Records: {}", 
                 correlationId, transformationType, recordsTransformed);
    }
    
    /**
     * Create security audit entry.
     */
    public void auditSecurityEvent(String correlationId, String eventType, String eventDescription,
                                 String userId, String ipAddress, boolean encryptionApplied,
                                 String piiFields) {
        DataLoadAuditEntity audit = createAuditFromContext(correlationId);
        if (audit == null) return;
        
        audit.setAuditType(DataLoadAuditEntity.AuditType.SECURITY_EVENT);
        audit.setEventName("SECURITY_" + eventType.toUpperCase());
        audit.setEventDescription(eventDescription);
        audit.setUserId(userId);
        audit.setIpAddress(ipAddress);
        audit.setEncryptionApplied(encryptionApplied ? "Y" : "N");
        audit.setPiiFields(piiFields);
        
        // Security events are always compliance-relevant
        audit.setComplianceStatus(DataLoadAuditEntity.ComplianceStatus.REQUIRES_REVIEW);
        audit.setRegulatoryRequirement("PCI-DSS, SOX, GDPR");
        
        saveAuditEntry(audit);
        
        log.warn("Security audit - Correlation: {}, Event: {}, User: {}, IP: {}", 
                correlationId, eventType, userId, ipAddress);
    }
    
    /**
     * Create compliance audit entry.
     */
    public void auditComplianceCheck(String correlationId, String complianceType,
                                   String regulatoryRequirement, boolean compliant,
                                   String complianceDescription) {
        DataLoadAuditEntity audit = createAuditFromContext(correlationId);
        if (audit == null) return;
        
        audit.setAuditType(DataLoadAuditEntity.AuditType.COMPLIANCE_CHECK);
        audit.setEventName("COMPLIANCE_" + complianceType.toUpperCase());
        audit.setEventDescription(complianceDescription);
        audit.setRegulatoryRequirement(regulatoryRequirement);
        audit.setComplianceStatus(compliant ? 
            DataLoadAuditEntity.ComplianceStatus.COMPLIANT : 
            DataLoadAuditEntity.ComplianceStatus.NON_COMPLIANT);
        
        saveAuditEntry(audit);
        
        log.info("Compliance audit - Correlation: {}, Type: {}, Requirement: {}, Compliant: {}", 
                correlationId, complianceType, regulatoryRequirement, compliant);
    }
    
    /**
     * Create performance metrics audit entry.
     */
    public void auditPerformanceMetrics(String correlationId, String operationType,
                                      long executionTimeMs, double memoryUsageMb,
                                      double cpuUsagePercent, long recordsProcessed) {
        DataLoadAuditEntity audit = createAuditFromContext(correlationId);
        if (audit == null) return;
        
        audit.setAuditType(DataLoadAuditEntity.AuditType.PERFORMANCE_METRIC);
        audit.setEventName("PERFORMANCE_" + operationType.toUpperCase());
        audit.setEventDescription("Performance metrics for " + operationType);
        audit.setExecutionTimeMs(executionTimeMs);
        audit.setMemoryUsageMb(memoryUsageMb);
        audit.setCpuUsagePercent(cpuUsagePercent);
        audit.setRecordCount(recordsProcessed);
        audit.setProcessedCount(recordsProcessed);
        
        saveAuditEntry(audit);
        
        log.debug("Performance audit - Correlation: {}, Operation: {}, Time: {}ms, Memory: {}MB", 
                 correlationId, operationType, executionTimeMs, memoryUsageMb);
    }
    
    /**
     * Create audit entry for error events.
     */
    public void auditErrorEvent(String correlationId, String errorType, String errorMessage,
                              String errorCode, String stackTrace, long recordsAffected) {
        DataLoadAuditEntity audit = createAuditFromContext(correlationId);
        if (audit == null) return;
        
        audit.setAuditType(DataLoadAuditEntity.AuditType.ERROR_EVENT);
        audit.setEventName("ERROR_" + errorType.toUpperCase());
        audit.setEventDescription("Error occurred during data loading");
        audit.setErrorCode(errorCode);
        audit.setErrorMessage(errorMessage);
        audit.setStackTrace(stackTrace);
        audit.setRecordCount(recordsAffected);
        audit.setErrorCount(recordsAffected);
        audit.setComplianceStatus(DataLoadAuditEntity.ComplianceStatus.NON_COMPLIANT);
        
        saveAuditEntry(audit);
        
        log.error("Error audit - Correlation: {}, Type: {}, Code: {}, Records: {}", 
                 correlationId, errorType, errorCode, recordsAffected);
    }
    
    /**
     * Complete audit trail for data loading operation.
     */
    public void auditDataLoadComplete(String correlationId, boolean successful, 
                                    long totalRecords, long successfulRecords, long failedRecords,
                                    long durationMs) {
        DataLoadAuditEntity audit = createAuditFromContext(correlationId);
        if (audit == null) return;
        
        audit.setAuditType(DataLoadAuditEntity.AuditType.DATA_LINEAGE);
        audit.setEventName("DATA_LOAD_COMPLETE");
        audit.setEventDescription("Data loading operation " + (successful ? "completed successfully" : "failed"));
        audit.setRecordCount(totalRecords);
        audit.setProcessedCount(successfulRecords);
        audit.setErrorCount(failedRecords);
        audit.setExecutionTimeMs(durationMs);
        
        // Set compliance status based on success
        audit.setComplianceStatus(successful && failedRecords == 0 ? 
            DataLoadAuditEntity.ComplianceStatus.COMPLIANT : 
            DataLoadAuditEntity.ComplianceStatus.NON_COMPLIANT);
        
        // Calculate final data quality score
        if (totalRecords > 0) {
            double qualityScore = (double) successfulRecords / totalRecords * 100.0;
            audit.setDataQualityScore(qualityScore);
        }
        
        saveAuditEntry(audit);
        
        // Clean up context cache
        auditContextCache.remove(correlationId);
        
        log.info("Data load audit completed - Correlation: {}, Success: {}, Total: {}, Duration: {}ms", 
                correlationId, successful, totalRecords, durationMs);
    }
    
    /**
     * Create base audit entity with common fields.
     */
    private DataLoadAuditEntity createBaseAuditEntity(String configId, String jobExecutionId, String correlationId) {
        DataLoadAuditEntity audit = new DataLoadAuditEntity();
        audit.setConfigId(configId);
        audit.setJobExecutionId(jobExecutionId);
        audit.setCorrelationId(correlationId);
        audit.setApplicationName(applicationName);
        audit.setApplicationVersion(applicationVersion);
        audit.setEnvironment(environment);
        audit.setHostName(hostName);
        audit.setProcessId(processId);
        audit.setThreadId(Thread.currentThread().getId());
        
        // Add current memory usage
        MemoryUsage heapUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        audit.setMemoryUsageMb((double) heapUsage.getUsed() / (1024 * 1024));
        
        return audit;
    }
    
    /**
     * Create audit entity from existing context.
     */
    private DataLoadAuditEntity createAuditFromContext(String correlationId) {
        AuditContext context = auditContextCache.get(correlationId);
        if (context == null) {
            log.warn("No audit context found for correlation ID: {}", correlationId);
            return null;
        }
        
        return createBaseAuditEntity(context.getConfigId(), context.getJobExecutionId(), correlationId);
    }
    
    /**
     * Save audit entry (async if enabled).
     */
    private void saveAuditEntry(DataLoadAuditEntity audit) {
        if (asyncAuditEnabled) {
            saveAuditEntryAsync(audit);
        } else {
            saveAuditEntrySync(audit);
        }
    }
    
    /**
     * Save audit entry asynchronously.
     */
    @Async
    public void saveAuditEntryAsync(DataLoadAuditEntity audit) {
        try {
            auditRepository.save(audit);
        } catch (Exception e) {
            log.error("Error saving audit entry asynchronously: {}", e.getMessage());
        }
    }
    
    /**
     * Save audit entry synchronously.
     */
    private void saveAuditEntrySync(DataLoadAuditEntity audit) {
        try {
            auditRepository.save(audit);
        } catch (Exception e) {
            log.error("Error saving audit entry: {}", e.getMessage());
            throw new RuntimeException("Failed to save audit entry", e);
        }
    }
    
    /**
     * Generate unique correlation ID.
     */
    private String generateCorrelationId() {
        return "AUDIT-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Initialize system information.
     */
    private void initializeSystemInfo() {
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostName = "unknown";
        }
        
        try {
            processId = ProcessHandle.current().pid();
        } catch (Exception e) {
            processId = -1;
        }
    }
    
    /**
     * Get complete data lineage for a correlation ID.
     */
    public DataLineageReport getDataLineage(String correlationId) {
        try {
            List<DataLoadAuditEntity> auditEntries = auditRepository.findByCorrelationIdOrderByAuditTimestamp(correlationId);
            return new DataLineageReport(correlationId, auditEntries);
        } catch (Exception e) {
            log.error("Error retrieving data lineage for correlation ID {}: {}", correlationId, e.getMessage());
            throw new RuntimeException("Failed to retrieve data lineage", e);
        }
    }
    
    /**
     * Get audit statistics for monitoring dashboard.
     */
    public AuditStatistics getAuditStatistics(LocalDateTime fromDate) {
        try {
            return new AuditStatistics(fromDate, auditRepository);
        } catch (Exception e) {
            log.error("Error retrieving audit statistics: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve audit statistics", e);
        }
    }
    
    /**
     * Clean up old audit entries based on retention policy.
     */
    public void cleanupOldAuditEntries(int retentionDays) {
        try {
            LocalDateTime retentionDate = LocalDateTime.now().minusDays(retentionDays);
            List<DataLoadAuditEntity> oldEntries = auditRepository.findAuditEntriesForRetention(retentionDate);
            
            if (!oldEntries.isEmpty()) {
                auditRepository.deleteAll(oldEntries);
                log.info("Cleaned up {} old audit entries older than {} days", oldEntries.size(), retentionDays);
            }
        } catch (Exception e) {
            log.error("Error cleaning up old audit entries: {}", e.getMessage());
        }
    }
    
    /**
     * Audit context for correlation tracking.
     */
    private static class AuditContext {
        private final String correlationId;
        private final String configId;
        private final String jobExecutionId;
        private LocalDateTime startTime;
        
        public AuditContext(String correlationId, String configId, String jobExecutionId) {
            this.correlationId = correlationId;
            this.configId = configId;
            this.jobExecutionId = jobExecutionId;
        }
        
        public String getCorrelationId() { return correlationId; }
        public String getConfigId() { return configId; }
        public String getJobExecutionId() { return jobExecutionId; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    }
}