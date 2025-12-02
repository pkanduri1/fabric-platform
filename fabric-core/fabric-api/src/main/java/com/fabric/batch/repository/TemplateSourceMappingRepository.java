package com.fabric.batch.repository;

import com.fabric.batch.entity.TemplateSourceMappingEntity;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TemplateSourceMappingEntity in US002 Template Configuration Enhancement
 * Maps to CM3INT.TEMPLATE_SOURCE_MAPPINGS table
 * 
 * Converted from JPA to JdbcTemplate-based repository to eliminate JPA dependencies
 */
@Repository
public interface TemplateSourceMappingRepository {

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
    List<TemplateSourceMappingEntity> findMappingsWithMissingSourceFields(
            @Param("fileType") String fileType, 
            @Param("transactionType") String transactionType, 
            @Param("sourceSystemId") String sourceSystemId, 
            @Param("enabled") String enabled);

    // Standard CRUD operations to replace JpaRepository methods
    <S extends TemplateSourceMappingEntity> S save(S entity);
    <S extends TemplateSourceMappingEntity> Iterable<S> saveAll(Iterable<S> entities);
    Optional<TemplateSourceMappingEntity> findById(Long id);
    boolean existsById(Long id);
    List<TemplateSourceMappingEntity> findAll();
    Iterable<TemplateSourceMappingEntity> findAllById(Iterable<Long> ids);
    long count();
    void deleteById(Long id);
    void delete(TemplateSourceMappingEntity entity);
    void deleteAllById(Iterable<? extends Long> ids);
    void deleteAll(Iterable<? extends TemplateSourceMappingEntity> entities);
    void deleteAll();
}