package com.truist.batch.repository;

import com.truist.batch.entity.FieldMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Epic 2: Repository interface for FieldMappingEntity providing
 * database-driven field mapping configuration management with performance optimization.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 2 - Simple Transaction Processing
 */
@Repository
public interface FieldMappingRepository extends JpaRepository<FieldMappingEntity, Long> {

    /**
     * Find field mappings by transaction type ID and active flag
     */
    @Query("SELECT fm FROM FieldMappingEntity fm " +
           "WHERE fm.transactionTypeId = :transactionTypeId " +
           "AND fm.activeFlag = :activeFlag " +
           "ORDER BY fm.sequenceOrder ASC")
    List<FieldMappingEntity> findByTransactionTypeIdAndActiveFlag(
            @Param("transactionTypeId") Long transactionTypeId,
            @Param("activeFlag") String activeFlag);

    /**
     * Find field mapping by transaction type and target field
     */
    @Query("SELECT fm FROM FieldMappingEntity fm " +
           "WHERE fm.transactionTypeId = :transactionTypeId " +
           "AND fm.targetField = :targetField " +
           "AND fm.activeFlag = 'Y'")
    Optional<FieldMappingEntity> findByTransactionTypeIdAndTargetField(
            @Param("transactionTypeId") Long transactionTypeId,
            @Param("targetField") String targetField);

    /**
     * Find field mappings requiring encryption
     */
    @Query("SELECT fm FROM FieldMappingEntity fm " +
           "WHERE fm.encryptionLevel IS NOT NULL " +
           "AND fm.encryptionLevel != 'NONE' " +
           "AND fm.activeFlag = 'Y' " +
           "ORDER BY fm.encryptionLevel DESC, fm.sequenceOrder ASC")
    List<FieldMappingEntity> findEncryptionRequiredMappings();

    /**
     * Find field mappings containing PII data
     */
    @Query("SELECT fm FROM FieldMappingEntity fm " +
           "WHERE fm.piiClassification IS NOT NULL " +
           "AND fm.piiClassification != 'NONE' " +
           "AND fm.activeFlag = 'Y' " +
           "ORDER BY fm.piiClassification DESC, fm.sequenceOrder ASC")
    List<FieldMappingEntity> findPIIFieldMappings();

    /**
     * Find field mappings by compliance level
     */
    @Query("SELECT fm FROM FieldMappingEntity fm " +
           "WHERE fm.complianceLevel = :complianceLevel " +
           "AND fm.activeFlag = 'Y' " +
           "ORDER BY fm.sequenceOrder ASC")
    List<FieldMappingEntity> findByComplianceLevel(@Param("complianceLevel") String complianceLevel);

    /**
     * Count active field mappings
     */
    @Query("SELECT COUNT(fm) FROM FieldMappingEntity fm " +
           "WHERE fm.activeFlag = :activeFlag")
    Long countByActiveFlag(@Param("activeFlag") String activeFlag);

    /**
     * Find field mappings by transformation type
     */
    @Query("SELECT fm FROM FieldMappingEntity fm " +
           "WHERE fm.transformationType = :transformationType " +
           "AND fm.activeFlag = 'Y' " +
           "ORDER BY fm.sequenceOrder ASC")
    List<FieldMappingEntity> findByTransformationType(@Param("transformationType") String transformationType);

    /**
     * Find field mappings with business rules
     */
    @Query("SELECT fm FROM FieldMappingEntity fm " +
           "WHERE fm.businessRuleId IS NOT NULL " +
           "AND fm.activeFlag = 'Y' " +
           "ORDER BY fm.businessRuleId ASC, fm.sequenceOrder ASC")
    List<FieldMappingEntity> findMappingsWithBusinessRules();

    /**
     * Find field mappings modified after specific date
     */
    @Query("SELECT fm FROM FieldMappingEntity fm " +
           "WHERE fm.lastModifiedDate > :sinceDate " +
           "AND fm.activeFlag = 'Y' " +
           "ORDER BY fm.lastModifiedDate DESC")
    List<FieldMappingEntity> findModifiedSince(@Param("sinceDate") Instant sinceDate);

    /**
     * Find field mappings by creator
     */
    @Query("SELECT fm FROM FieldMappingEntity fm " +
           "WHERE fm.createdBy = :createdBy " +
           "ORDER BY fm.createdDate DESC")
    List<FieldMappingEntity> findByCreatedBy(@Param("createdBy") String createdBy);

    /**
     * Get field mapping statistics for monitoring
     */
    @Query("SELECT new map(" +
           "fm.transformationType as transformationType, " +
           "COUNT(fm) as count, " +
           "AVG(fm.fieldLength) as avgFieldLength) " +
           "FROM FieldMappingEntity fm " +
           "WHERE fm.transactionTypeId = :transactionTypeId " +
           "AND fm.activeFlag = 'Y' " +
           "GROUP BY fm.transformationType")
    List<java.util.Map<String, Object>> getFieldMappingStatistics(
            @Param("transactionTypeId") Long transactionTypeId);

    /**
     * Find field mappings by transaction type requiring validation
     */
    @Query("SELECT fm FROM FieldMappingEntity fm " +
           "WHERE fm.transactionTypeId = :transactionTypeId " +
           "AND fm.validationRequired = true " +
           "AND fm.activeFlag = 'Y' " +
           "ORDER BY fm.sequenceOrder ASC")
    List<FieldMappingEntity> findValidationRequiredByTransactionType(
            @Param("transactionTypeId") Long transactionTypeId);

    /**
     * Find duplicate field mappings for data quality checks
     */
    @Query("SELECT fm1 FROM FieldMappingEntity fm1 " +
           "WHERE EXISTS (" +
           "  SELECT fm2 FROM FieldMappingEntity fm2 " +
           "  WHERE fm2.transactionTypeId = fm1.transactionTypeId " +
           "  AND fm2.targetField = fm1.targetField " +
           "  AND fm2.mappingId != fm1.mappingId " +
           "  AND fm2.activeFlag = 'Y'" +
           ") " +
           "AND fm1.activeFlag = 'Y'")
    List<FieldMappingEntity> findDuplicateFieldMappings();

    /**
     * Find complex field mappings (high complexity score)
     */
    @Query("SELECT fm FROM FieldMappingEntity fm " +
           "WHERE fm.activeFlag = 'Y' " +
           "AND (" +
           "  fm.encryptionLevel IN ('HIGH', 'CRITICAL') OR " +
           "  fm.piiClassification IN ('CONFIDENTIAL', 'RESTRICTED', 'CARDHOLDER_DATA') OR " +
           "  fm.businessRuleId IS NOT NULL OR " +
           "  fm.transformationType = 'composite'" +
           ") " +
           "ORDER BY fm.complianceLevel DESC, fm.sequenceOrder ASC")
    List<FieldMappingEntity> findComplexFieldMappings();

    /**
     * Find field mappings by source field pattern
     */
    @Query("SELECT fm FROM FieldMappingEntity fm " +
           "WHERE fm.sourceField LIKE :pattern " +
           "AND fm.activeFlag = 'Y' " +
           "ORDER BY fm.sourceField ASC")
    List<FieldMappingEntity> findBySourceFieldPattern(@Param("pattern") String pattern);

    /**
     * Get compliance summary statistics
     */
    @Query("SELECT new map(" +
           "fm.complianceLevel as level, " +
           "COUNT(fm) as count, " +
           "SUM(CASE WHEN fm.encryptionLevel != 'NONE' THEN 1 ELSE 0 END) as encryptedCount, " +
           "SUM(CASE WHEN fm.piiClassification != 'NONE' THEN 1 ELSE 0 END) as piiCount) " +
           "FROM FieldMappingEntity fm " +
           "WHERE fm.activeFlag = 'Y' " +
           "GROUP BY fm.complianceLevel " +
           "ORDER BY fm.complianceLevel")
    List<java.util.Map<String, Object>> getComplianceSummary();
}