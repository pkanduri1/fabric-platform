package com.fabric.batch.repository;

import com.fabric.batch.entity.DataLoadAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for DataLoadAuditEntity operations.
 * Provides comprehensive data access methods for audit trail and compliance reporting.
 */
@Repository
public interface DataLoadAuditRepository extends JpaRepository<DataLoadAuditEntity, Long> {
    
    /**
     * Find audit entries by correlation ID for complete data lineage tracking.
     */
    List<DataLoadAuditEntity> findByCorrelationIdOrderByAuditTimestamp(String correlationId);
    
    /**
     * Find audit entries by configuration ID.
     */
    List<DataLoadAuditEntity> findByConfigIdOrderByAuditTimestampDesc(String configId);
    
    /**
     * Find audit entries by job execution ID.
     */
    List<DataLoadAuditEntity> findByJobExecutionIdOrderByAuditTimestamp(String jobExecutionId);
    
    /**
     * Find audit entries by audit type.
     */
    List<DataLoadAuditEntity> findByAuditTypeOrderByAuditTimestampDesc(DataLoadAuditEntity.AuditType auditType);
    
    /**
     * Find audit entries by user ID for user activity tracking.
     */
    List<DataLoadAuditEntity> findByUserIdOrderByAuditTimestampDesc(String userId);
    
    /**
     * Find audit entries for specific date range.
     */
    @Query("SELECT a FROM DataLoadAuditEntity a WHERE a.auditTimestamp BETWEEN :startDate AND :endDate ORDER BY a.auditTimestamp DESC")
    List<DataLoadAuditEntity> findByDateRange(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find data lineage audit entries.
     */
    List<DataLoadAuditEntity> findByAuditTypeAndSourceSystemOrderByAuditTimestamp(
            DataLoadAuditEntity.AuditType auditType, String sourceSystem);
    
    /**
     * Find security audit events.
     */
    @Query("SELECT a FROM DataLoadAuditEntity a WHERE a.auditType = 'SECURITY_EVENT' ORDER BY a.auditTimestamp DESC")
    List<DataLoadAuditEntity> findSecurityAuditEvents();
    
    /**
     * Find compliance audit entries.
     */
    List<DataLoadAuditEntity> findByAuditTypeAndComplianceStatusOrderByAuditTimestampDesc(
            DataLoadAuditEntity.AuditType auditType, DataLoadAuditEntity.ComplianceStatus complianceStatus);
    
    /**
     * Find audit entries with errors.
     */
    @Query("SELECT a FROM DataLoadAuditEntity a WHERE a.errorCount > 0 ORDER BY a.auditTimestamp DESC")
    List<DataLoadAuditEntity> findErrorAuditEntries();
    
    /**
     * Find audit entries containing PII data.
     */
    @Query("SELECT a FROM DataLoadAuditEntity a WHERE a.piiFields IS NOT NULL AND a.piiFields != '' ORDER BY a.auditTimestamp DESC")
    List<DataLoadAuditEntity> findPiiDataAuditEntries();
    
    /**
     * Find audit entries with encryption applied.
     */
    @Query("SELECT a FROM DataLoadAuditEntity a WHERE a.encryptionApplied = 'Y' ORDER BY a.auditTimestamp DESC")
    List<DataLoadAuditEntity> findEncryptedDataAuditEntries();
    
    /**
     * Find audit entries with masking applied.
     */
    @Query("SELECT a FROM DataLoadAuditEntity a WHERE a.maskingApplied = 'Y' ORDER BY a.auditTimestamp DESC")
    List<DataLoadAuditEntity> findMaskedDataAuditEntries();
    
    /**
     * Find audit entries by target table for data lineage.
     */
    List<DataLoadAuditEntity> findByTargetTableOrderByAuditTimestampDesc(String targetTable);
    
    /**
     * Find audit entries by file name pattern.
     */
    @Query("SELECT a FROM DataLoadAuditEntity a WHERE a.fileName LIKE :fileNamePattern ORDER BY a.auditTimestamp DESC")
    List<DataLoadAuditEntity> findByFileNamePattern(@Param("fileNamePattern") String fileNamePattern);
    
    /**
     * Find non-compliant audit entries.
     */
    @Query("SELECT a FROM DataLoadAuditEntity a WHERE a.complianceStatus != 'COMPLIANT' ORDER BY a.auditTimestamp DESC")
    List<DataLoadAuditEntity> findNonCompliantAuditEntries();
    
    /**
     * Find audit entries requiring review.
     */
    List<DataLoadAuditEntity> findByComplianceStatusOrderByAuditTimestampDesc(
            DataLoadAuditEntity.ComplianceStatus complianceStatus);
    
    /**
     * Find audit entries by regulatory requirement.
     */
    List<DataLoadAuditEntity> findByRegulatoryRequirementOrderByAuditTimestampDesc(String regulatoryRequirement);
    
    /**
     * Find performance audit entries (execution time above threshold).
     */
    @Query("SELECT a FROM DataLoadAuditEntity a WHERE a.executionTimeMs > :thresholdMs ORDER BY a.executionTimeMs DESC")
    List<DataLoadAuditEntity> findSlowExecutionAuditEntries(@Param("thresholdMs") Long thresholdMs);
    
    /**
     * Find high memory usage audit entries.
     */
    @Query("SELECT a FROM DataLoadAuditEntity a WHERE a.memoryUsageMb > :thresholdMb ORDER BY a.memoryUsageMb DESC")
    List<DataLoadAuditEntity> findHighMemoryUsageAuditEntries(@Param("thresholdMb") Double thresholdMb);
    
    /**
     * Get data lineage for specific data source to destination.
     */
    @Query("SELECT a FROM DataLoadAuditEntity a WHERE a.dataSource = :dataSource AND a.dataDestination = :dataDestination AND a.auditType = 'DATA_LINEAGE' ORDER BY a.auditTimestamp")
    List<DataLoadAuditEntity> getDataLineage(
            @Param("dataSource") String dataSource, @Param("dataDestination") String dataDestination);
    
    /**
     * Get audit statistics by audit type.
     */
    @Query("SELECT a.auditType, COUNT(a), AVG(a.executionTimeMs), SUM(a.recordCount) FROM DataLoadAuditEntity a WHERE a.auditTimestamp >= :fromDate GROUP BY a.auditType")
    List<Object[]> getAuditStatisticsByType(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get audit statistics by user.
     */
    @Query("SELECT a.userId, COUNT(a), SUM(a.recordCount), COUNT(CASE WHEN a.errorCount > 0 THEN 1 END) FROM DataLoadAuditEntity a WHERE a.auditTimestamp >= :fromDate GROUP BY a.userId")
    List<Object[]> getAuditStatisticsByUser(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get daily audit volume.
     */
    @Query("SELECT DATE(a.auditTimestamp), COUNT(a), SUM(a.recordCount) FROM DataLoadAuditEntity a WHERE a.auditTimestamp >= :fromDate GROUP BY DATE(a.auditTimestamp) ORDER BY DATE(a.auditTimestamp)")
    List<Object[]> getDailyAuditVolume(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Find audit entries by environment for environment-specific reporting.
     */
    List<DataLoadAuditEntity> findByEnvironmentOrderByAuditTimestampDesc(String environment);
    
    /**
     * Find audit entries by application version.
     */
    List<DataLoadAuditEntity> findByApplicationVersionOrderByAuditTimestampDesc(String applicationVersion);
    
    /**
     * Get compliance status summary.
     */
    @Query("SELECT a.complianceStatus, COUNT(a) FROM DataLoadAuditEntity a WHERE a.auditTimestamp >= :fromDate GROUP BY a.complianceStatus")
    List<Object[]> getComplianceStatusSummary(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Find audit entries with parent-child relationships.
     */
    List<DataLoadAuditEntity> findByParentAuditIdOrderByAuditTimestamp(Long parentAuditId);
    
    /**
     * Get data quality metrics.
     */
    @Query("SELECT AVG(a.dataQualityScore), MIN(a.dataQualityScore), MAX(a.dataQualityScore), COUNT(a) FROM DataLoadAuditEntity a WHERE a.dataQualityScore IS NOT NULL AND a.auditTimestamp >= :fromDate")
    Object[] getDataQualityMetrics(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Find audit entries for retention policy enforcement.
     */
    @Query("SELECT a FROM DataLoadAuditEntity a WHERE a.auditTimestamp <= :retentionDate ORDER BY a.auditTimestamp")
    List<DataLoadAuditEntity> findAuditEntriesForRetention(@Param("retentionDate") LocalDateTime retentionDate);
    
    /**
     * Count audit entries since a specific date.
     */
    @Query("SELECT COUNT(a) FROM DataLoadAuditEntity a WHERE a.auditTimestamp >= :fromDate")
    long countAuditEntriesSince(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Count audit entries by audit type since a specific date.
     */
    @Query("SELECT COUNT(a) FROM DataLoadAuditEntity a WHERE a.auditType = :auditType AND a.auditTimestamp >= :fromDate")
    long countByAuditTypeAndAuditTimestampAfter(@Param("auditType") String auditType, @Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Count audit entries by compliance status since a specific date.
     */
    @Query("SELECT COUNT(a) FROM DataLoadAuditEntity a WHERE a.complianceStatus = :complianceStatus AND a.auditTimestamp >= :fromDate")
    long countByComplianceStatusAndAuditTimestampAfter(@Param("complianceStatus") String complianceStatus, @Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get average data quality score since a specific date.
     */
    @Query("SELECT AVG(a.dataQualityScore) FROM DataLoadAuditEntity a WHERE a.dataQualityScore IS NOT NULL AND a.auditTimestamp >= :fromDate")
    Double getAverageDataQualityScore(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get total records processed since a specific date.
     */
    @Query("SELECT SUM(a.recordCount) FROM DataLoadAuditEntity a WHERE a.recordCount IS NOT NULL AND a.auditTimestamp >= :fromDate")
    Long getTotalRecordsProcessed(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get total records failed since a specific date.
     */
    @Query("SELECT SUM(a.errorCount) FROM DataLoadAuditEntity a WHERE a.errorCount IS NOT NULL AND a.auditTimestamp >= :fromDate")
    Long getTotalRecordsFailed(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get average execution time since a specific date.
     */
    @Query("SELECT AVG(a.executionTimeMs) FROM DataLoadAuditEntity a WHERE a.executionTimeMs IS NOT NULL AND a.auditTimestamp >= :fromDate")
    Long getAverageExecutionTime(@Param("fromDate") LocalDateTime fromDate);
}