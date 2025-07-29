package com.truist.batch.repository;

import com.truist.batch.entity.ValidationRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for ValidationRuleEntity operations.
 * Provides comprehensive data access methods for validation rule management.
 */
@Repository
public interface ValidationRuleRepository extends JpaRepository<ValidationRuleEntity, Long> {
    
    /**
     * Find all validation rules for a specific configuration.
     */
    List<ValidationRuleEntity> findByConfigIdAndEnabledOrderByExecutionOrder(String configId, String enabled);
    
    /**
     * Find validation rules by field name.
     */
    List<ValidationRuleEntity> findByConfigIdAndFieldNameAndEnabledOrderByExecutionOrder(
            String configId, String fieldName, String enabled);
    
    /**
     * Find validation rules by rule type.
     */
    List<ValidationRuleEntity> findByConfigIdAndRuleTypeAndEnabledOrderByExecutionOrder(
            String configId, ValidationRuleEntity.RuleType ruleType, String enabled);
    
    /**
     * Find validation rules by severity.
     */
    List<ValidationRuleEntity> findByConfigIdAndSeverityAndEnabledOrderByExecutionOrder(
            String configId, ValidationRuleEntity.Severity severity, String enabled);
    
    /**
     * Find critical validation rules (ERROR and CRITICAL severity).
     */
    @Query("SELECT r FROM ValidationRuleEntity r WHERE r.configId = :configId AND r.severity IN ('ERROR', 'CRITICAL') AND r.enabled = :enabled ORDER BY r.executionOrder")
    List<ValidationRuleEntity> findCriticalValidationRules(@Param("configId") String configId, @Param("enabled") String enabled);
    
    /**
     * Find required field validation rules.
     */
    @Query("SELECT r FROM ValidationRuleEntity r WHERE r.configId = :configId AND r.requiredField = :requiredField AND r.enabled = :enabled ORDER BY r.executionOrder")
    List<ValidationRuleEntity> findRequiredFieldValidationRules(@Param("configId") String configId, @Param("requiredField") String requiredField, @Param("enabled") String enabled);
    
    /**
     * Find validation rules by config ID and enabled true, ordered by execution order.
     */
    List<ValidationRuleEntity> findByConfigIdAndEnabledTrueOrderByExecutionOrder(String configId);
    
    /**
     * Find unique field validation rules.
     */
    @Query("SELECT r FROM ValidationRuleEntity r WHERE r.configId = :configId AND r.uniqueField = :uniqueField AND r.enabled = :enabled ORDER BY r.executionOrder")
    List<ValidationRuleEntity> findByConfigIdAndUniqueFieldAndEnabled(
            @Param("configId") String configId, @Param("uniqueField") String uniqueField, @Param("enabled") String enabled);
    
    /**
     * Find referential integrity validation rules.
     */
    @Query("SELECT r FROM ValidationRuleEntity r WHERE r.configId = :configId AND r.referenceTable IS NOT NULL AND r.enabled = :enabled ORDER BY r.executionOrder")
    List<ValidationRuleEntity> findReferentialIntegrityRules(@Param("configId") String configId, @Param("enabled") String enabled);
    
    /**
     * Find business rule validation rules.
     */
    @Query("SELECT r FROM ValidationRuleEntity r WHERE r.configId = :configId AND r.businessRuleClass IS NOT NULL AND r.enabled = :enabled ORDER BY r.executionOrder")
    List<ValidationRuleEntity> findBusinessRuleValidations(@Param("configId") String configId, @Param("enabled") String enabled);
    
    /**
     * Find validation rules with custom SQL.
     */
    @Query("SELECT r FROM ValidationRuleEntity r WHERE r.configId = :configId AND r.lookupSql IS NOT NULL AND r.enabled = :enabled ORDER BY r.executionOrder")
    List<ValidationRuleEntity> findCustomSqlValidationRules(@Param("configId") String configId, @Param("enabled") String enabled);
    
    /**
     * Find validation rules by data type.
     */
    List<ValidationRuleEntity> findByConfigIdAndDataTypeAndEnabledOrderByExecutionOrder(
            String configId, String dataType, String enabled);
    
    /**
     * Find validation rules with error thresholds.
     */
    @Query("SELECT r FROM ValidationRuleEntity r WHERE r.configId = :configId AND r.errorThreshold > 0 AND r.enabled = :enabled ORDER BY r.errorThreshold DESC")
    List<ValidationRuleEntity> findValidationRulesWithErrorThresholds(@Param("configId") String configId, @Param("enabled") String enabled);
    
    /**
     * Find validation rules with warning thresholds.
     */
    @Query("SELECT r FROM ValidationRuleEntity r WHERE r.configId = :configId AND r.warningThreshold > 0 AND r.enabled = :enabled ORDER BY r.warningThreshold DESC")
    List<ValidationRuleEntity> findValidationRulesWithWarningThresholds(@Param("configId") String configId, @Param("enabled") String enabled);
    
    /**
     * Count validation rules by type for a configuration.
     */
    @Query("SELECT r.ruleType, COUNT(r) FROM ValidationRuleEntity r WHERE r.configId = :configId AND r.enabled = :enabled GROUP BY r.ruleType")
    List<Object[]> countValidationRulesByType(@Param("configId") String configId, @Param("enabled") String enabled);
    
    /**
     * Count validation rules by severity for a configuration.
     */
    @Query("SELECT r.severity, COUNT(r) FROM ValidationRuleEntity r WHERE r.configId = :configId AND r.enabled = :enabled GROUP BY r.severity")
    List<Object[]> countValidationRulesBySeverity(@Param("configId") String configId, @Param("enabled") String enabled);
    
    /**
     * Find validation rules for specific fields.
     */
    @Query("SELECT r FROM ValidationRuleEntity r WHERE r.configId = :configId AND r.fieldName IN :fieldNames AND r.enabled = :enabled ORDER BY r.fieldName, r.executionOrder")
    List<ValidationRuleEntity> findValidationRulesForFields(
            @Param("configId") String configId, @Param("fieldNames") List<String> fieldNames, @Param("enabled") String enabled);
    
    /**
     * Find validation rules by reference table.
     */
    List<ValidationRuleEntity> findByReferenceTableAndEnabledOrderByConfigIdAscExecutionOrderAsc(
            String referenceTable, String enabled);
    
    /**
     * Check if validation rule exists for field and type.
     */
    boolean existsByConfigIdAndFieldNameAndRuleTypeAndEnabled(
            String configId, String fieldName, ValidationRuleEntity.RuleType ruleType, String enabled);
    
    /**
     * Get maximum execution order for a configuration.
     */
    @Query("SELECT MAX(r.executionOrder) FROM ValidationRuleEntity r WHERE r.configId = :configId")
    Integer getMaxExecutionOrder(@Param("configId") String configId);
    
    /**
     * Find validation rules created by specific user.
     */
    List<ValidationRuleEntity> findByCreatedByAndEnabledOrderByCreatedDateDesc(String createdBy, String enabled);
    
    /**
     * Get validation rule statistics by configuration.
     */
    @Query("SELECT r.configId, COUNT(r), COUNT(CASE WHEN r.severity IN ('ERROR', 'CRITICAL') THEN 1 END) as criticalCount FROM ValidationRuleEntity r WHERE r.enabled = :enabled GROUP BY r.configId")
    List<Object[]> getValidationRuleStatistics(@Param("enabled") String enabled);
}