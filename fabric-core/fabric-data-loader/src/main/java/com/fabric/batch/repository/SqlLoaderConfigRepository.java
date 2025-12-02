package com.fabric.batch.repository;

import com.fabric.batch.entity.SqlLoaderConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SqlLoaderConfigEntity operations.
 * Provides comprehensive data access methods for SQL*Loader configurations
 * with focus on security, performance, and compliance requirements.
 */
@Repository
public interface SqlLoaderConfigRepository extends JpaRepository<SqlLoaderConfigEntity, String> {
    
    /**
     * Find configuration by source system and job name (enabled only).
     */
    Optional<SqlLoaderConfigEntity> findBySourceSystemAndJobNameAndEnabledOrderByVersionDesc(
            String sourceSystem, String jobName, String enabled);
    
    /**
     * Find all configurations for a specific source system.
     */
    List<SqlLoaderConfigEntity> findBySourceSystemAndEnabledOrderByJobName(String sourceSystem, String enabled);
    
    /**
     * Find configurations by target table.
     */
    List<SqlLoaderConfigEntity> findByTargetTableAndEnabledOrderByJobName(String targetTable, String enabled);
    
    /**
     * Find configurations by load method.
     */
    List<SqlLoaderConfigEntity> findByLoadMethodAndEnabledOrderByJobName(
            SqlLoaderConfigEntity.LoadMethod loadMethod, String enabled);
    
    /**
     * Find configurations with direct path enabled.
     */
    List<SqlLoaderConfigEntity> findByDirectPathAndEnabledOrderByJobName(String directPath, String enabled);
    
    /**
     * Find configurations with parallel processing.
     */
    @Query("SELECT s FROM SqlLoaderConfigEntity s WHERE s.parallelDegree > 1 AND s.enabled = :enabled ORDER BY s.parallelDegree DESC, s.jobName")
    List<SqlLoaderConfigEntity> findParallelConfigurationsAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Find configurations that require encryption.
     */
    List<SqlLoaderConfigEntity> findByEncryptionRequiredAndEnabledOrderByJobName(String encryptionRequired, String enabled);
    
    /**
     * Find configurations that contain PII data.
     */
    @Query("SELECT s FROM SqlLoaderConfigEntity s WHERE s.piiFields IS NOT NULL AND s.piiFields <> '' AND s.enabled = :enabled ORDER BY s.jobName")
    List<SqlLoaderConfigEntity> findConfigurationsWithPiiFieldsAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Find configurations by data classification.
     */
    List<SqlLoaderConfigEntity> findByDataClassificationAndEnabledOrderByJobName(
            SqlLoaderConfigEntity.DataClassification dataClassification, String enabled);
    
    /**
     * Find configurations by regulatory compliance requirements.
     */
    @Query("SELECT s FROM SqlLoaderConfigEntity s WHERE s.regulatoryCompliance LIKE %:compliance% AND s.enabled = :enabled ORDER BY s.jobName")
    List<SqlLoaderConfigEntity> findByRegulatoryComplianceContainingAndEnabled(
            @Param("compliance") String compliance, @Param("enabled") String enabled);
    
    /**
     * Find configurations created by specific user.
     */
    List<SqlLoaderConfigEntity> findByCreatedByAndEnabledOrderByCreatedDateDesc(String createdBy, String enabled);
    
    /**
     * Find configurations modified after specific date.
     */
    List<SqlLoaderConfigEntity> findByModifiedDateAfterAndEnabledOrderByModifiedDateDesc(
            LocalDateTime modifiedDate, String enabled);
    
    /**
     * Count configurations by source system.
     */
    @Query("SELECT COUNT(s) FROM SqlLoaderConfigEntity s WHERE s.sourceSystem = :sourceSystem AND s.enabled = :enabled")
    Long countBySourceSystemAndEnabled(@Param("sourceSystem") String sourceSystem, @Param("enabled") String enabled);
    
    /**
     * Find configurations with validation enabled.
     */
    List<SqlLoaderConfigEntity> findByValidationEnabledAndEnabledOrderByJobName(String validationEnabled, String enabled);
    
    /**
     * Find configurations with audit trail required.
     */
    List<SqlLoaderConfigEntity> findByAuditTrailRequiredAndEnabledOrderByJobName(String auditTrailRequired, String enabled);
    
    /**
     * Find configurations with specific retention period range.
     */
    @Query("SELECT s FROM SqlLoaderConfigEntity s WHERE s.retentionDays BETWEEN :minDays AND :maxDays AND s.enabled = :enabled ORDER BY s.retentionDays")
    List<SqlLoaderConfigEntity> findByRetentionDaysBetweenAndEnabled(
            @Param("minDays") Integer minDays, @Param("maxDays") Integer maxDays, @Param("enabled") String enabled);
    
    /**
     * Find configurations with high error thresholds.
     */
    @Query("SELECT s FROM SqlLoaderConfigEntity s WHERE s.maxErrors > :threshold AND s.enabled = :enabled ORDER BY s.maxErrors DESC")
    List<SqlLoaderConfigEntity> findByMaxErrorsGreaterThanAndEnabled(
            @Param("threshold") Integer threshold, @Param("enabled") String enabled);
    
    /**
     * Find configurations for compliance reporting by classification.
     */
    @Query("SELECT s FROM SqlLoaderConfigEntity s WHERE s.dataClassification IN :classifications AND s.enabled = :enabled ORDER BY s.sourceSystem, s.jobName")
    List<SqlLoaderConfigEntity> findByDataClassificationInAndEnabled(
            @Param("classifications") List<SqlLoaderConfigEntity.DataClassification> classifications, 
            @Param("enabled") String enabled);
    
    /**
     * Find configurations with resumable operations enabled.
     */
    List<SqlLoaderConfigEntity> findByResumableAndEnabledOrderByJobName(String resumable, String enabled);
    
    /**
     * Find configurations with custom options.
     */
    @Query("SELECT s FROM SqlLoaderConfigEntity s WHERE s.customOptions IS NOT NULL AND s.customOptions <> '' AND s.enabled = :enabled ORDER BY s.jobName")
    List<SqlLoaderConfigEntity> findConfigurationsWithCustomOptionsAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Find configurations with pre or post execution SQL.
     */
    @Query("SELECT s FROM SqlLoaderConfigEntity s WHERE (s.preExecutionSql IS NOT NULL OR s.postExecutionSql IS NOT NULL) AND s.enabled = :enabled ORDER BY s.jobName")
    List<SqlLoaderConfigEntity> findConfigurationsWithExecutionSqlAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Check if configuration exists by unique constraint.
     */
    boolean existsBySourceSystemAndJobNameAndEnabled(String sourceSystem, String jobName, String enabled);
    
    /**
     * Find configurations by character set.
     */
    List<SqlLoaderConfigEntity> findByCharacterSetAndEnabledOrderByJobName(String characterSet, String enabled);
    
    /**
     * Find configurations with specific bind size range for performance analysis.
     */
    @Query("SELECT s FROM SqlLoaderConfigEntity s WHERE s.bindSize BETWEEN :minSize AND :maxSize AND s.enabled = :enabled ORDER BY s.bindSize")
    List<SqlLoaderConfigEntity> findByBindSizeBetweenAndEnabled(
            @Param("minSize") Long minSize, @Param("maxSize") Long maxSize, @Param("enabled") String enabled);
    
    /**
     * Get configuration statistics by source system.
     */
    @Query("SELECT s.sourceSystem, COUNT(s), AVG(s.maxErrors), AVG(s.parallelDegree), MAX(s.version) FROM SqlLoaderConfigEntity s WHERE s.enabled = :enabled GROUP BY s.sourceSystem")
    List<Object[]> getConfigurationStatsBySourceSystem(@Param("enabled") String enabled);
    
    /**
     * Get performance statistics for configurations.
     */
    @Query("SELECT s.configId, s.jobName, s.parallelDegree, s.bindSize, s.readSize, s.directPath FROM SqlLoaderConfigEntity s WHERE s.enabled = :enabled ORDER BY s.parallelDegree DESC, s.bindSize DESC")
    List<Object[]> getPerformanceConfigurationStats(@Param("enabled") String enabled);
    
    /**
     * Get security and compliance summary.
     */
    @Query("SELECT s.dataClassification, COUNT(s), " +
           "SUM(CASE WHEN s.encryptionRequired = 'Y' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN s.piiFields IS NOT NULL AND s.piiFields <> '' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN s.auditTrailRequired = 'Y' THEN 1 ELSE 0 END) " +
           "FROM SqlLoaderConfigEntity s WHERE s.enabled = :enabled GROUP BY s.dataClassification")
    List<Object[]> getSecurityComplianceSummary(@Param("enabled") String enabled);
    
    /**
     * Find configurations that need attention (high risk or compliance issues).
     */
    @Query("SELECT s FROM SqlLoaderConfigEntity s WHERE " +
           "(s.dataClassification IN ('CONFIDENTIAL', 'RESTRICTED', 'TOP_SECRET') AND s.encryptionRequired = 'N') OR " +
           "(s.piiFields IS NOT NULL AND s.piiFields <> '' AND s.auditTrailRequired = 'N') OR " +
           "(s.regulatoryCompliance LIKE '%PCI%' AND s.auditTrailRequired = 'N') " +
           "AND s.enabled = :enabled ORDER BY s.dataClassification DESC, s.jobName")
    List<SqlLoaderConfigEntity> findConfigurationsRequiringAttention(@Param("enabled") String enabled);
    
    /**
     * Find configuration by config ID and enabled status.
     */
    Optional<SqlLoaderConfigEntity> findByConfigIdAndEnabled(String configId, String enabled);
    
    /**
     * Find all enabled configurations ordered by job name.
     */
    List<SqlLoaderConfigEntity> findByEnabledOrderByJobName(String enabled);
    
    /**
     * Count enabled configurations.
     */
    long countByEnabled(String enabled);
    
    /**
     * Find configurations with notification emails configured.
     */
    @Query("SELECT s FROM SqlLoaderConfigEntity s WHERE s.notificationEmails IS NOT NULL AND s.notificationEmails <> '' AND s.enabled = :enabled ORDER BY s.jobName")
    List<SqlLoaderConfigEntity> findConfigurationsWithNotificationEmailsAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Find configurations by encryption algorithm.
     */
    List<SqlLoaderConfigEntity> findByEncryptionAlgorithmAndEnabledOrderByJobName(String encryptionAlgorithm, String enabled);
    
    /**
     * Find configurations with specific field delimiter.
     */
    List<SqlLoaderConfigEntity> findByFieldDelimiterAndEnabledOrderByJobName(String fieldDelimiter, String enabled);
    
    /**
     * Get configuration count by data classification for reporting.
     */
    @Query("SELECT s.dataClassification, COUNT(s) FROM SqlLoaderConfigEntity s WHERE s.enabled = :enabled GROUP BY s.dataClassification ORDER BY COUNT(s) DESC")
    List<Object[]> getConfigurationCountByClassification(@Param("enabled") String enabled);
    
    /**
     * Find configurations created within date range.
     */
    @Query("SELECT s FROM SqlLoaderConfigEntity s WHERE s.createdDate BETWEEN :startDate AND :endDate AND s.enabled = :enabled ORDER BY s.createdDate DESC")
    List<SqlLoaderConfigEntity> findByCreatedDateBetweenAndEnabled(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate, 
            @Param("enabled") String enabled);
    
    /**
     * Find configurations with specific version for auditing.
     */
    List<SqlLoaderConfigEntity> findByVersionAndEnabledOrderByModifiedDateDesc(Integer version, String enabled);
    
    /**
     * Get average configuration metrics.
     */
    @Query("SELECT AVG(s.maxErrors), AVG(s.parallelDegree), AVG(s.bindSize), AVG(s.readSize), AVG(s.retentionDays) FROM SqlLoaderConfigEntity s WHERE s.enabled = :enabled")
    Object[] getAverageConfigurationMetrics(@Param("enabled") String enabled);
}