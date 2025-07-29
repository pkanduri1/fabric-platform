package com.truist.batch.audit;

import com.truist.batch.repository.DataLoadAuditRepository;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive audit statistics for monitoring and reporting.
 */
@Data
@NoArgsConstructor
public class AuditStatistics {
    
    private LocalDateTime fromDate;
    private LocalDateTime generatedAt;
    private long totalAuditEntries;
    private long dataLineageEntries;
    private long securityEventEntries;
    private long complianceCheckEntries;
    private long errorEventEntries;
    private long performanceMetricEntries;
    private long validationResultEntries;
    private long businessRuleExecutionEntries;
    private long compliantEntries;
    private long nonCompliantEntries;
    private long entriesRequiringReview;
    private double averageDataQualityScore;
    private long totalRecordsProcessed;
    private long totalRecordsFailed;
    private double overallSuccessRate;
    private long averageExecutionTimeMs;
    private Map<String, Long> auditTypeBreakdown;
    private Map<String, Long> complianceStatusBreakdown;
    private Map<String, Long> errorCodeBreakdown;
    private Map<String, Double> qualityScoreBySource;
    
    public AuditStatistics(LocalDateTime fromDate, DataLoadAuditRepository auditRepository) {
        this.fromDate = fromDate;
        this.generatedAt = LocalDateTime.now();
        this.auditTypeBreakdown = new HashMap<>();
        this.complianceStatusBreakdown = new HashMap<>();
        this.errorCodeBreakdown = new HashMap<>();
        this.qualityScoreBySource = new HashMap<>();
        
        calculateStatistics(auditRepository);
    }
    
    private void calculateStatistics(DataLoadAuditRepository auditRepository) {
        try {
            // Total audit entries
            this.totalAuditEntries = auditRepository.countAuditEntriesSince(fromDate);
            
            // Audit type counts
            this.dataLineageEntries = auditRepository.countByAuditTypeAndAuditTimestampAfter("DATA_LINEAGE", fromDate);
            this.securityEventEntries = auditRepository.countByAuditTypeAndAuditTimestampAfter("SECURITY_EVENT", fromDate);
            this.complianceCheckEntries = auditRepository.countByAuditTypeAndAuditTimestampAfter("COMPLIANCE_CHECK", fromDate);
            this.errorEventEntries = auditRepository.countByAuditTypeAndAuditTimestampAfter("ERROR_EVENT", fromDate);
            this.performanceMetricEntries = auditRepository.countByAuditTypeAndAuditTimestampAfter("PERFORMANCE_METRIC", fromDate);
            this.validationResultEntries = auditRepository.countByAuditTypeAndAuditTimestampAfter("VALIDATION_RESULT", fromDate);
            this.businessRuleExecutionEntries = auditRepository.countByAuditTypeAndAuditTimestampAfter("BUSINESS_RULE_EXECUTION", fromDate);
            
            // Compliance status counts
            this.compliantEntries = auditRepository.countByComplianceStatusAndAuditTimestampAfter("COMPLIANT", fromDate);
            this.nonCompliantEntries = auditRepository.countByComplianceStatusAndAuditTimestampAfter("NON_COMPLIANT", fromDate);
            this.entriesRequiringReview = auditRepository.countByComplianceStatusAndAuditTimestampAfter("REQUIRES_REVIEW", fromDate);
            
            // Data quality metrics
            Double avgQuality = auditRepository.getAverageDataQualityScore(fromDate);
            this.averageDataQualityScore = avgQuality != null ? avgQuality : 0.0;
            
            // Record processing metrics
            Long totalProcessed = auditRepository.getTotalRecordsProcessed(fromDate);
            this.totalRecordsProcessed = totalProcessed != null ? totalProcessed : 0;
            
            Long totalFailed = auditRepository.getTotalRecordsFailed(fromDate);
            this.totalRecordsFailed = totalFailed != null ? totalFailed : 0;
            
            // Calculate success rate
            if (totalRecordsProcessed > 0) {
                this.overallSuccessRate = ((double) (totalRecordsProcessed - totalRecordsFailed) / totalRecordsProcessed) * 100.0;
            }
            
            // Execution time metrics
            Long avgExecutionTime = auditRepository.getAverageExecutionTime(fromDate);
            this.averageExecutionTimeMs = avgExecutionTime != null ? avgExecutionTime : 0;
            
            // Build breakdown maps
            buildAuditTypeBreakdown();
            buildComplianceStatusBreakdown();
            buildErrorCodeBreakdown(auditRepository);
            buildQualityScoreBySource(auditRepository);
            
        } catch (Exception e) {
            // Handle errors gracefully - set default values
            this.totalAuditEntries = 0;
            this.averageDataQualityScore = 0.0;
            this.overallSuccessRate = 0.0;
        }
    }
    
    private void buildAuditTypeBreakdown() {
        auditTypeBreakdown.put("DATA_LINEAGE", dataLineageEntries);
        auditTypeBreakdown.put("SECURITY_EVENT", securityEventEntries);
        auditTypeBreakdown.put("COMPLIANCE_CHECK", complianceCheckEntries);
        auditTypeBreakdown.put("ERROR_EVENT", errorEventEntries);
        auditTypeBreakdown.put("PERFORMANCE_METRIC", performanceMetricEntries);
        auditTypeBreakdown.put("VALIDATION_RESULT", validationResultEntries);
        auditTypeBreakdown.put("BUSINESS_RULE_EXECUTION", businessRuleExecutionEntries);
    }
    
    private void buildComplianceStatusBreakdown() {
        complianceStatusBreakdown.put("COMPLIANT", compliantEntries);
        complianceStatusBreakdown.put("NON_COMPLIANT", nonCompliantEntries);
        complianceStatusBreakdown.put("REQUIRES_REVIEW", entriesRequiringReview);
    }
    
    private void buildErrorCodeBreakdown(DataLoadAuditRepository auditRepository) {
        try {
            // This would need a custom repository method to get error code counts
            // For now, we'll leave it empty or implement basic logic
            errorCodeBreakdown.put("VALIDATION_ERROR", validationResultEntries);
            errorCodeBreakdown.put("SYSTEM_ERROR", errorEventEntries);
        } catch (Exception e) {
            // Handle gracefully
        }
    }
    
    private void buildQualityScoreBySource(DataLoadAuditRepository auditRepository) {
        try {
            // This would need a custom repository method to get quality scores by source
            // For now, we'll set a default
            qualityScoreBySource.put("OVERALL", averageDataQualityScore);
        } catch (Exception e) {
            // Handle gracefully
        }
    }
    
    /**
     * Get compliance percentage.
     */
    public double getCompliancePercentage() {
        long totalComplianceEntries = compliantEntries + nonCompliantEntries + entriesRequiringReview;
        if (totalComplianceEntries == 0) return 100.0;
        return ((double) compliantEntries / totalComplianceEntries) * 100.0;
    }
    
    /**
     * Get error rate percentage.
     */
    public double getErrorRate() {
        if (totalRecordsProcessed == 0) return 0.0;
        return ((double) totalRecordsFailed / totalRecordsProcessed) * 100.0;
    }
    
    /**
     * Check if system is healthy based on thresholds.
     */
    public boolean isSystemHealthy() {
        return getCompliancePercentage() >= 95.0 && 
               getErrorRate() <= 5.0 && 
               averageDataQualityScore >= 90.0;
    }
    
    /**
     * Get summary report as string.
     */
    public String getSummaryReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== AUDIT STATISTICS SUMMARY ===\n");
        report.append(String.format("Report Period: %s to %s\n", fromDate, generatedAt));
        report.append(String.format("Total Audit Entries: %d\n", totalAuditEntries));
        report.append(String.format("Records Processed: %d\n", totalRecordsProcessed));
        report.append(String.format("Records Failed: %d\n", totalRecordsFailed));
        report.append(String.format("Success Rate: %.2f%%\n", overallSuccessRate));
        report.append(String.format("Average Data Quality: %.2f\n", averageDataQualityScore));
        report.append(String.format("Compliance Rate: %.2f%%\n", getCompliancePercentage()));
        report.append(String.format("System Health: %s\n", isSystemHealthy() ? "HEALTHY" : "NEEDS ATTENTION"));
        report.append("================================\n");
        return report.toString();
    }
}