package com.truist.batch.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Placeholder data lineage report class for fabric-api module.
 * Provides basic structure without dependencies on fabric-data-loader.
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
    private double dataQualityScore = 100.0;
    private String complianceStatus = "COMPLIANT";
    private List<DataLineageStep> lineageSteps;
    private Map<String, Object> metadata;
    
    public DataLineageReport(String correlationId, List<?> auditEntries) {
        this.correlationId = correlationId;
        this.reportGeneratedAt = LocalDateTime.now();
        this.lineageSteps = Collections.emptyList();
        this.metadata = Collections.emptyMap();
        this.complianceStatus = "COMPLIANT";
        this.dataQualityScore = 100.0;
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