package com.truist.batch.repository;

import com.truist.batch.entity.TemplateSourceMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TemplateSourceMappingEntity in US002 Template Configuration Enhancement
 * Maps to CM3INT.TEMPLATE_SOURCE_MAPPINGS table
 */
@Repository
public interface TemplateSourceMappingRepository extends JpaRepository<TemplateSourceMappingEntity, Long> {

    /**
     * Find all enabled mappings for a specific template-source combination
     * 
     * @param fileType Template file type
     * @param transactionType Template transaction type
     * @param sourceSystemId Source system ID
     * @param enabled Enabled flag ('Y' or 'N')
     * @return List of field mappings
     */
    List<TemplateSourceMappingEntity> findByFileTypeAndTransactionTypeAndSourceSystemIdAndEnabledOrderByTargetPosition(
            String fileType, String transactionType, String sourceSystemId, String enabled);

    /**
     * Find all enabled mappings for a specific template
     * 
     * @param fileType Template file type
     * @param transactionType Template transaction type
     * @param enabled Enabled flag ('Y' or 'N')
     * @return List of field mappings
     */
    List<TemplateSourceMappingEntity> findByFileTypeAndTransactionTypeAndEnabledOrderBySourceSystemIdAscTargetPositionAsc(
            String fileType, String transactionType, String enabled);

    /**
     * Find all enabled mappings for a specific source system
     * 
     * @param sourceSystemId Source system ID
     * @param enabled Enabled flag ('Y' or 'N')
     * @return List of field mappings
     */
    List<TemplateSourceMappingEntity> findBySourceSystemIdAndEnabledOrderByFileTypeAscTransactionTypeAscTargetPositionAsc(
            String sourceSystemId, String enabled);

    /**
     * Find all mappings for a specific job
     * 
     * @param jobName Job name
     * @param enabled Enabled flag ('Y' or 'N')
     * @return List of field mappings
     */
    List<TemplateSourceMappingEntity> findByJobNameAndEnabledOrderByTargetPosition(String jobName, String enabled);

    /**
     * Count mappings for a template-source combination
     * 
     * @param fileType Template file type
     * @param transactionType Template transaction type
     * @param sourceSystemId Source system ID
     * @param enabled Enabled flag ('Y' or 'N')
     * @return Count of mappings
     */
    long countByFileTypeAndTransactionTypeAndSourceSystemIdAndEnabled(
            String fileType, String transactionType, String sourceSystemId, String enabled);

    /**
     * Check if mapping exists for template-source combination
     * 
     * @param fileType Template file type
     * @param transactionType Template transaction type
     * @param sourceSystemId Source system ID
     * @param enabled Enabled flag ('Y' or 'N')
     * @return true if exists, false otherwise
     */
    boolean existsByFileTypeAndTransactionTypeAndSourceSystemIdAndEnabled(
            String fileType, String transactionType, String sourceSystemId, String enabled);

    /**
     * Delete mappings for a specific template-source-job combination
     * 
     * @param fileType Template file type
     * @param transactionType Template transaction type
     * @param sourceSystemId Source system ID
     * @param jobName Job name
     */
    @Modifying
    @Query("DELETE FROM TemplateSourceMappingEntity t WHERE t.fileType = :fileType AND t.transactionType = :transactionType AND t.sourceSystemId = :sourceSystemId AND t.jobName = :jobName")
    void deleteByFileTypeAndTransactionTypeAndSourceSystemIdAndJobName(
            @Param("fileType") String fileType, 
            @Param("transactionType") String transactionType, 
            @Param("sourceSystemId") String sourceSystemId, 
            @Param("jobName") String jobName);

    /**
     * Disable mappings for a template-source combination (soft delete)
     * 
     * @param fileType Template file type
     * @param transactionType Template transaction type
     * @param sourceSystemId Source system ID
     */
    @Modifying
    @Query("UPDATE TemplateSourceMappingEntity t SET t.enabled = 'N', t.modifiedDate = CURRENT_TIMESTAMP WHERE t.fileType = :fileType AND t.transactionType = :transactionType AND t.sourceSystemId = :sourceSystemId AND t.enabled = 'Y'")
    void disableByFileTypeAndTransactionTypeAndSourceSystemId(
            @Param("fileType") String fileType, 
            @Param("transactionType") String transactionType, 
            @Param("sourceSystemId") String sourceSystemId);

    /**
     * Get distinct source systems for a template
     * 
     * @param fileType Template file type
     * @param transactionType Template transaction type
     * @param enabled Enabled flag ('Y' or 'N')
     * @return List of distinct source system IDs
     */
    @Query("SELECT DISTINCT t.sourceSystemId FROM TemplateSourceMappingEntity t WHERE t.fileType = :fileType AND t.transactionType = :transactionType AND t.enabled = :enabled ORDER BY t.sourceSystemId")
    List<String> findDistinctSourceSystemIdsByFileTypeAndTransactionTypeAndEnabled(
            @Param("fileType") String fileType, 
            @Param("transactionType") String transactionType, 
            @Param("enabled") String enabled);

    /**
     * Get distinct templates for a source system
     * 
     * @param sourceSystemId Source system ID
     * @param enabled Enabled flag ('Y' or 'N')
     * @return List of distinct file type and transaction type combinations
     */
    @Query("SELECT DISTINCT CONCAT(t.fileType, '/', t.transactionType) FROM TemplateSourceMappingEntity t WHERE t.sourceSystemId = :sourceSystemId AND t.enabled = :enabled ORDER BY t.fileType, t.transactionType")
    List<String> findDistinctTemplatesBySourceSystemIdAndEnabled(
            @Param("sourceSystemId") String sourceSystemId, 
            @Param("enabled") String enabled);

    /**
     * Find mappings with missing source fields (for validation)
     * 
     * @param fileType Template file type
     * @param transactionType Template transaction type
     * @param sourceSystemId Source system ID
     * @param enabled Enabled flag ('Y' or 'N')
     * @return List of mappings with null or empty source fields
     */
    @Query("SELECT t FROM TemplateSourceMappingEntity t WHERE t.fileType = :fileType AND t.transactionType = :transactionType AND t.sourceSystemId = :sourceSystemId AND t.enabled = :enabled AND (t.sourceFieldName IS NULL OR t.sourceFieldName = '') ORDER BY t.targetPosition")
    List<TemplateSourceMappingEntity> findMappingsWithMissingSourceFields(
            @Param("fileType") String fileType, 
            @Param("transactionType") String transactionType, 
            @Param("sourceSystemId") String sourceSystemId, 
            @Param("enabled") String enabled);
}