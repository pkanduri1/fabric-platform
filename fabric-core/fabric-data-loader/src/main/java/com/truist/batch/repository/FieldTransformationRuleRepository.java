package com.truist.batch.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.truist.batch.entity.FieldTransformationRuleEntity;

/**
 * Repository for managing field transformation rules.
 * 
 * Provides data access methods for field-level transformation rules
 * migrated from Excel mappings to support database-driven ETL processing.
 */
@Repository
public interface FieldTransformationRuleRepository extends JpaRepository<FieldTransformationRuleEntity, Long> {
    
    /**
     * Find all transformation rules for a specific configuration ordered by execution order
     */
    List<FieldTransformationRuleEntity> findByTransformationConfigConfigIdAndEnabledOrderByExecutionOrderAsc(
        String configId, String enabled);
    
    /**
     * Find transformation rules by configuration and target position
     */
    List<FieldTransformationRuleEntity> findByTransformationConfigConfigIdAndEnabledOrderByTargetPositionAsc(
        String configId, String enabled);
    
    /**
     * Find a specific rule by configuration and target field name
     */
    Optional<FieldTransformationRuleEntity> findByTransformationConfigConfigIdAndTargetFieldNameAndEnabled(
        String configId, String targetFieldName, String enabled);
    
    /**
     * Find rules by transformation type
     */
    List<FieldTransformationRuleEntity> findByTransformationTypeAndEnabledOrderByTransformationConfigConfigIdAsc(
        String transformationType, String enabled);
    
    /**
     * Find required fields for a configuration
     */
    @Query("SELECT ftr FROM FieldTransformationRuleEntity ftr WHERE " +
           "ftr.transformationConfig.configId = :configId AND ftr.requiredField = 'Y' AND ftr.enabled = 'Y' " +
           "ORDER BY ftr.executionOrder ASC")
    List<FieldTransformationRuleEntity> findRequiredFieldsByConfigId(@Param("configId") String configId);
    
    /**
     * Find conditional transformation rules
     */
    @Query("SELECT ftr FROM FieldTransformationRuleEntity ftr WHERE " +
           "ftr.transformationConfig.configId = :configId AND ftr.transformationType = 'conditional' AND ftr.enabled = 'Y' " +
           "ORDER BY ftr.executionOrder ASC")
    List<FieldTransformationRuleEntity> findConditionalRulesByConfigId(@Param("configId") String configId);
    
    /**
     * Find lookup transformation rules
     */
    @Query("SELECT ftr FROM FieldTransformationRuleEntity ftr WHERE " +
           "ftr.transformationType = 'lookup' AND ftr.lookupTable IS NOT NULL AND ftr.enabled = 'Y'")
    List<FieldTransformationRuleEntity> findLookupTransformationRules();
    
    /**
     * Find rules that reference a specific source field
     */
    List<FieldTransformationRuleEntity> findBySourceFieldNameAndEnabledOrderByTransformationConfigConfigIdAsc(
        String sourceFieldName, String enabled);
    
    /**
     * Count rules by transformation type for a configuration
     */
    @Query("SELECT ftr.transformationType, COUNT(ftr) FROM FieldTransformationRuleEntity ftr WHERE " +
           "ftr.transformationConfig.configId = :configId AND ftr.enabled = 'Y' " +
           "GROUP BY ftr.transformationType")
    List<Object[]> countRulesByTransformationType(@Param("configId") String configId);
    
    /**
     * Find rules with validation requirements
     */
    @Query("SELECT ftr FROM FieldTransformationRuleEntity ftr WHERE " +
           "ftr.transformationConfig.configId = :configId AND ftr.validationRule IS NOT NULL AND ftr.enabled = 'Y' " +
           "ORDER BY ftr.executionOrder ASC")
    List<FieldTransformationRuleEntity> findRulesWithValidation(@Param("configId") String configId);
    
    /**
     * Find rules migrated from Excel with row references
     */
    @Query("SELECT ftr FROM FieldTransformationRuleEntity ftr WHERE " +
           "ftr.excelRowReference IS NOT NULL AND ftr.enabled = 'Y' " +
           "ORDER BY ftr.transformationConfig.configId ASC, ftr.excelRowReference ASC")
    List<FieldTransformationRuleEntity> findRulesWithExcelReference();
    
    /**
     * Find rules by target position range
     */
    @Query("SELECT ftr FROM FieldTransformationRuleEntity ftr WHERE " +
           "ftr.transformationConfig.configId = :configId AND " +
           "ftr.targetPosition BETWEEN :startPosition AND :endPosition AND ftr.enabled = 'Y' " +
           "ORDER BY ftr.targetPosition ASC")
    List<FieldTransformationRuleEntity> findRulesByPositionRange(
        @Param("configId") String configId,
        @Param("startPosition") Integer startPosition,
        @Param("endPosition") Integer endPosition);
    
    /**
     * Update execution order for a rule
     */
    @Modifying
    @Query("UPDATE FieldTransformationRuleEntity ftr SET ftr.executionOrder = :executionOrder, " +
           "ftr.modifiedDate = :modifiedDate, ftr.modifiedBy = :modifiedBy " +
           "WHERE ftr.ruleId = :ruleId")
    int updateExecutionOrder(@Param("ruleId") Long ruleId,
                            @Param("executionOrder") Integer executionOrder,
                            @Param("modifiedDate") LocalDateTime modifiedDate,
                            @Param("modifiedBy") String modifiedBy);
    
    /**
     * Enable or disable a rule
     */
    @Modifying
    @Query("UPDATE FieldTransformationRuleEntity ftr SET ftr.enabled = :enabled, " +
           "ftr.modifiedDate = :modifiedDate, ftr.modifiedBy = :modifiedBy " +
           "WHERE ftr.ruleId = :ruleId")
    int updateEnabledStatus(@Param("ruleId") Long ruleId,
                           @Param("enabled") String enabled,
                           @Param("modifiedDate") LocalDateTime modifiedDate,
                           @Param("modifiedBy") String modifiedBy);
    
    /**
     * Update transformation expression for a rule
     */
    @Modifying
    @Query("UPDATE FieldTransformationRuleEntity ftr SET ftr.transformationExpression = :expression, " +
           "ftr.modifiedDate = :modifiedDate, ftr.modifiedBy = :modifiedBy " +
           "WHERE ftr.ruleId = :ruleId")
    int updateTransformationExpression(@Param("ruleId") Long ruleId,
                                      @Param("expression") String expression,
                                      @Param("modifiedDate") LocalDateTime modifiedDate,
                                      @Param("modifiedBy") String modifiedBy);
    
    /**
     * Get rule statistics for a configuration
     */
    @Query("SELECT " +
           "COUNT(ftr) as totalRules, " +
           "SUM(CASE WHEN ftr.enabled = 'Y' THEN 1 ELSE 0 END) as enabledRules, " +
           "SUM(CASE WHEN ftr.requiredField = 'Y' THEN 1 ELSE 0 END) as requiredFields, " +
           "SUM(CASE WHEN ftr.validationRule IS NOT NULL THEN 1 ELSE 0 END) as rulesWithValidation " +
           "FROM FieldTransformationRuleEntity ftr WHERE ftr.transformationConfig.configId = :configId")
    Object[] getRuleStatistics(@Param("configId") String configId);
    
    /**
     * Check for duplicate target positions within a configuration
     */
    @Query("SELECT ftr.targetPosition, COUNT(ftr) FROM FieldTransformationRuleEntity ftr WHERE " +
           "ftr.transformationConfig.configId = :configId AND ftr.enabled = 'Y' " +
           "GROUP BY ftr.targetPosition HAVING COUNT(ftr) > 1")
    List<Object[]> findDuplicateTargetPositions(@Param("configId") String configId);
    
    /**
     * Find gaps in target positions
     */
    @Query("SELECT MIN(ftr.targetPosition) as minPos, MAX(ftr.targetPosition) as maxPos, COUNT(ftr) as ruleCount " +
           "FROM FieldTransformationRuleEntity ftr WHERE " +
           "ftr.transformationConfig.configId = :configId AND ftr.enabled = 'Y'")
    Object[] getPositionRangeInfo(@Param("configId") String configId);
}