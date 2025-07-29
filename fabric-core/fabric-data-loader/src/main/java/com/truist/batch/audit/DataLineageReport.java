package com.truist.batch.audit;

import com.truist.batch.entity.DataLoadAuditEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Comprehensive data lineage report for tracking data flow and transformations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataLineageReport {
    
    private String correlationId;
    private LocalDateTime reportGeneratedAt;
    private String sourceSystem;
    private String targetTable;
    private String fileName;
    private LocalDateTime dataLoadStartTime;
    private LocalDateTime dataLoadEndTime;
    private long totalExecutionTimeMs;
    private long totalRecords;
    private long successfulRecords;
    private long failedRecords;
    private double dataQualityScore;
    private String complianceStatus;
    private List<DataLineageStep> lineageSteps;
    private Map<String, Object> metadata;
    
    public DataLineageReport(String correlationId, List<DataLoadAuditEntity> auditEntries) {
        this.correlationId = correlationId;
        this.reportGeneratedAt = LocalDateTime.now();
        
        if (auditEntries != null && !auditEntries.isEmpty()) {
            processAuditEntries(auditEntries);
        }
    }
    
    private void processAuditEntries(List<DataLoadAuditEntity> auditEntries) {
        // Extract basic information from first entry
        DataLoadAuditEntity firstEntry = auditEntries.get(0);
        this.sourceSystem = firstEntry.getSourceSystem();
        this.targetTable = firstEntry.getTargetTable();
        this.fileName = firstEntry.getFileName();
        
        // Find start and end times
        this.dataLoadStartTime = auditEntries.stream()
            .map(DataLoadAuditEntity::getAuditTimestamp)
            .min(LocalDateTime::compareTo)
            .orElse(null);
            
        this.dataLoadEndTime = auditEntries.stream()
            .map(DataLoadAuditEntity::getAuditTimestamp)
            .max(LocalDateTime::compareTo)
            .orElse(null);
            
        if (dataLoadStartTime != null && dataLoadEndTime != null) {
            this.totalExecutionTimeMs = java.time.Duration.between(dataLoadStartTime, dataLoadEndTime).toMillis();
        }
        
        // Calculate aggregated metrics
        this.totalRecords = auditEntries.stream()
            .mapToLong(entry -> entry.getRecordCount() != null ? entry.getRecordCount() : 0)
            .max()
            .orElse(0);
            
        this.successfulRecords = auditEntries.stream()
            .mapToLong(entry -> entry.getProcessedCount() != null ? entry.getProcessedCount() : 0)
            .max()
            .orElse(0);
            
        this.failedRecords = auditEntries.stream()
            .mapToLong(entry -> entry.getErrorCount() != null ? entry.getErrorCount() : 0)
            .sum();
            
        this.dataQualityScore = auditEntries.stream()
            .mapToDouble(entry -> entry.getDataQualityScore() != null ? entry.getDataQualityScore() : 100.0)
            .average()
            .orElse(100.0);
            
        // Get compliance status from latest entry
        this.complianceStatus = auditEntries.stream()
            .filter(entry -> entry.getComplianceStatus() != null)
            .reduce((first, second) -> second) // Get last one
            .map(entry -> entry.getComplianceStatus().toString())
            .orElse("UNKNOWN");
        
        // Create lineage steps
        this.lineageSteps = auditEntries.stream()
            .map(this::createLineageStep)
            .collect(Collectors.toList());
        
        // Create metadata map
        this.metadata = createMetadata(auditEntries);
    }
    
    private DataLineageStep createLineageStep(DataLoadAuditEntity auditEntry) {
        DataLineageStep step = new DataLineageStep();
        step.setStepName(auditEntry.getEventName());
        step.setStepType(auditEntry.getAuditType().toString());
        step.setDescription(auditEntry.getEventDescription());
        step.setTimestamp(auditEntry.getAuditTimestamp());
        step.setRecordsProcessed(auditEntry.getRecordCount());
        step.setRecordsSuccessful(auditEntry.getProcessedCount());
        step.setRecordsFailed(auditEntry.getErrorCount());
        step.setDataSource(auditEntry.getDataSource());
        step.setDataDestination(auditEntry.getDataDestination());
        step.setTransformationApplied(auditEntry.getTransformationApplied());
        step.setValidationRules(auditEntry.getValidationRulesApplied());
        step.setBusinessRules(auditEntry.getBusinessRulesApplied());
        step.setErrorMessage(auditEntry.getErrorMessage());
        step.setExecutionTimeMs(auditEntry.getExecutionTimeMs());
        return step;
    }
    
    private Map<String, Object> createMetadata(List<DataLoadAuditEntity> auditEntries) {
        return auditEntries.stream()
            .filter(entry -> entry.getAdditionalMetadata() != null)
            .collect(Collectors.toMap(
                entry -> entry.getEventName(),
                entry -> entry.getAdditionalMetadata(),
                (existing, replacement) -> replacement
            ));
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataLineageStep {
        private String stepName;
        private String stepType;
        private String description;
        private LocalDateTime timestamp;
        private Long recordsProcessed;
        private Long recordsSuccessful;
        private Long recordsFailed;
        private String dataSource;
        private String dataDestination;
        private String transformationApplied;
        private String validationRules;
        private String businessRules;
        private String errorMessage;
        private Long executionTimeMs;
    }
}