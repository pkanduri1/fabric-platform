package com.fabric.batch.repository;

import com.fabric.batch.entity.DataLoadConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DataLoadConfigEntity operations.
 * Provides comprehensive data access methods for data loading configurations.
 */
@Repository
public interface DataLoadConfigRepository extends JpaRepository<DataLoadConfigEntity, String> {
    
    /**
     * Find configuration by source system and job name.
     */
    Optional<DataLoadConfigEntity> findBySourceSystemAndJobNameAndEnabledOrderByVersionDesc(
            String sourceSystem, String jobName, String enabled);
    
    /**
     * Find all configurations for a specific source system.
     */
    List<DataLoadConfigEntity> findBySourceSystemAndEnabledOrderByJobName(String sourceSystem, String enabled);
    
    /**
     * Find configurations by target table.
     */
    List<DataLoadConfigEntity> findByTargetTableAndEnabledOrderByJobName(String targetTable, String enabled);
    
    /**
     * Find configurations by file type.
     */
    List<DataLoadConfigEntity> findByFileTypeAndEnabledOrderByJobName(
            DataLoadConfigEntity.FileType fileType, String enabled);
    
    /**
     * Find configurations that require encryption.
     */
    List<DataLoadConfigEntity> findByEncryptionRequiredAndEnabledOrderByJobName(String encryptionRequired, String enabled);
    
    /**
     * Find configurations that contain PII data.
     */
    List<DataLoadConfigEntity> findByPiiDataAndEnabledOrderByJobName(String piiData, String enabled);
    
    /**
     * Find configurations by data classification.
     */
    List<DataLoadConfigEntity> findByDataClassificationAndEnabledOrderByJobName(
            DataLoadConfigEntity.DataClassification dataClassification, String enabled);
    
    /**
     * Find configurations created by specific user.
     */
    List<DataLoadConfigEntity> findByCreatedByAndEnabledOrderByCreatedDateDesc(String createdBy, String enabled);
    
    /**
     * Count configurations by source system.
     */
    @Query("SELECT COUNT(c) FROM DataLoadConfigEntity c WHERE c.sourceSystem = :sourceSystem AND c.enabled = :enabled")
    Long countBySourceSystemAndEnabled(@Param("sourceSystem") String sourceSystem, @Param("enabled") String enabled);
    
    /**
     * Find configurations with validation enabled.
     */
    @Query("SELECT c FROM DataLoadConfigEntity c WHERE c.validationEnabled = :validationEnabled AND c.enabled = :enabled ORDER BY c.jobName")
    List<DataLoadConfigEntity> findByValidationEnabledAndEnabled(
            @Param("validationEnabled") String validationEnabled, @Param("enabled") String enabled);
    
    /**
     * Find configurations with specific retention period range.
     */
    @Query("SELECT c FROM DataLoadConfigEntity c WHERE c.retentionDays BETWEEN :minDays AND :maxDays AND c.enabled = :enabled ORDER BY c.retentionDays")
    List<DataLoadConfigEntity> findByRetentionDaysBetweenAndEnabled(
            @Param("minDays") Integer minDays, @Param("maxDays") Integer maxDays, @Param("enabled") String enabled);
    
    /**
     * Find configurations with high error thresholds.
     */
    @Query("SELECT c FROM DataLoadConfigEntity c WHERE c.maxErrors > :threshold AND c.enabled = :enabled ORDER BY c.maxErrors DESC")
    List<DataLoadConfigEntity> findByMaxErrorsGreaterThanAndEnabled(
            @Param("threshold") Integer threshold, @Param("enabled") String enabled);
    
    /**
     * Find configurations for compliance reporting.
     */
    @Query("SELECT c FROM DataLoadConfigEntity c WHERE c.dataClassification IN :classifications AND c.enabled = :enabled ORDER BY c.sourceSystem, c.jobName")
    List<DataLoadConfigEntity> findByDataClassificationInAndEnabled(
            @Param("classifications") List<DataLoadConfigEntity.DataClassification> classifications, 
            @Param("enabled") String enabled);
    
    /**
     * Find configurations requiring backup.
     */
    @Query("SELECT c FROM DataLoadConfigEntity c WHERE c.backupEnabled = :backupEnabled AND c.enabled = :enabled ORDER BY c.jobName")
    List<DataLoadConfigEntity> findByBackupEnabledAndEnabled(
            @Param("backupEnabled") String backupEnabled, @Param("enabled") String enabled);
    
    /**
     * Find configurations requiring archival.
     */
    @Query("SELECT c FROM DataLoadConfigEntity c WHERE c.archiveEnabled = :archiveEnabled AND c.enabled = :enabled ORDER BY c.jobName")
    List<DataLoadConfigEntity> findByArchiveEnabledAndEnabled(
            @Param("archiveEnabled") String archiveEnabled, @Param("enabled") String enabled);
    
    /**
     * Check if configuration exists by unique constraint.
     */
    boolean existsBySourceSystemAndJobNameAndEnabled(String sourceSystem, String jobName, String enabled);
    
    /**
     * Get configuration statistics by source system.
     */
    @Query("SELECT c.sourceSystem, COUNT(c), AVG(c.maxErrors), MAX(c.version) FROM DataLoadConfigEntity c WHERE c.enabled = :enabled GROUP BY c.sourceSystem")
    List<Object[]> getConfigurationStatsBySourceSystem(@Param("enabled") String enabled);
    
    /**
     * Find configuration by config ID and enabled status.
     */
    Optional<DataLoadConfigEntity> findByConfigIdAndEnabledTrue(String configId);
    
    /**
     * Find all enabled configurations ordered by job name.
     */
    List<DataLoadConfigEntity> findByEnabledTrueOrderByJobName();
    
    /**
     * Count enabled configurations.
     */
    long countByEnabledTrue();
}