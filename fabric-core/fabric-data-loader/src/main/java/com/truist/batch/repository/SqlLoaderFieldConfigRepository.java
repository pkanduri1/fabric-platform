package com.truist.batch.repository;

import com.truist.batch.entity.SqlLoaderFieldConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SqlLoaderFieldConfigEntity operations.
 * Provides comprehensive data access methods for SQL*Loader field configurations
 * with focus on field-level security, transformations, and validation rules.
 */
@Repository
public interface SqlLoaderFieldConfigRepository extends JpaRepository<SqlLoaderFieldConfigEntity, Long> {
    
    /**
     * Find all field configurations for a specific SQL*Loader config ordered by field order.
     */
    List<SqlLoaderFieldConfigEntity> findByConfigIdAndEnabledOrderByFieldOrder(String configId, String enabled);
    
    /**
     * Find field configuration by config ID and field name.
     */
    Optional<SqlLoaderFieldConfigEntity> findByConfigIdAndFieldNameAndEnabled(
            String configId, String fieldName, String enabled);
    
    /**
     * Find field configuration by config ID and column name.
     */
    Optional<SqlLoaderFieldConfigEntity> findByConfigIdAndColumnNameAndEnabled(
            String configId, String columnName, String enabled);
    
    /**
     * Find field configurations by data type.
     */
    List<SqlLoaderFieldConfigEntity> findByDataTypeAndEnabledOrderByConfigIdAscFieldOrderAsc(
            SqlLoaderFieldConfigEntity.SqlLoaderDataType dataType, String enabled);
    
    /**
     * Find encrypted field configurations.
     */
    List<SqlLoaderFieldConfigEntity> findByEncryptedAndEnabledOrderByConfigIdAscFieldOrderAsc(String encrypted, String enabled);
    
    /**
     * Find field configurations that are audit fields.
     */
    List<SqlLoaderFieldConfigEntity> findByAuditFieldAndEnabledOrderByConfigIdAscFieldOrderAsc(String auditField, String enabled);
    
    /**
     * Find field configurations with validation rules.
     */
    @Query("SELECT f FROM SqlLoaderFieldConfigEntity f WHERE f.validationRule IS NOT NULL AND f.validationRule <> '' AND f.enabled = :enabled ORDER BY f.configId, f.fieldOrder")
    List<SqlLoaderFieldConfigEntity> findFieldsWithValidationRulesAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Find field configurations with business rules.
     */
    @Query("SELECT f FROM SqlLoaderFieldConfigEntity f WHERE f.businessRuleClass IS NOT NULL AND f.businessRuleClass <> '' AND f.enabled = :enabled ORDER BY f.configId, f.fieldOrder")
    List<SqlLoaderFieldConfigEntity> findFieldsWithBusinessRulesAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Find field configurations with SQL expressions.
     */
    @Query("SELECT f FROM SqlLoaderFieldConfigEntity f WHERE f.sqlExpression IS NOT NULL AND f.sqlExpression <> '' AND f.enabled = :enabled ORDER BY f.configId, f.fieldOrder")
    List<SqlLoaderFieldConfigEntity> findFieldsWithSqlExpressionsAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Find field configurations with lookup references.
     */
    @Query("SELECT f FROM SqlLoaderFieldConfigEntity f WHERE f.lookupTable IS NOT NULL AND f.lookupTable <> '' AND f.enabled = :enabled ORDER BY f.configId, f.fieldOrder")
    List<SqlLoaderFieldConfigEntity> findFieldsWithLookupReferencesAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Find nullable field configurations.
     */
    List<SqlLoaderFieldConfigEntity> findByNullableAndEnabledOrderByConfigIdAscFieldOrderAsc(String nullable, String enabled);
    
    /**
     * Find field configurations with unique constraints.
     */
    List<SqlLoaderFieldConfigEntity> findByUniqueConstraintAndEnabledOrderByConfigIdAscFieldOrderAsc(String uniqueConstraint, String enabled);
    
    /**
     * Find primary key field configurations.
     */
    List<SqlLoaderFieldConfigEntity> findByPrimaryKeyAndEnabledOrderByConfigIdAscFieldOrderAsc(String primaryKey, String enabled);
    
    /**
     * Find field configurations with specific case sensitivity.
     */
    List<SqlLoaderFieldConfigEntity> findByCaseSensitiveAndEnabledOrderByConfigIdAscFieldOrderAsc(
            SqlLoaderFieldConfigEntity.CaseSensitive caseSensitive, String enabled);
    
    /**
     * Find field configurations by trim setting.
     */
    List<SqlLoaderFieldConfigEntity> findByTrimFieldAndEnabledOrderByConfigIdAscFieldOrderAsc(String trimField, String enabled);
    
    /**
     * Find field configurations with specific character set.
     */
    List<SqlLoaderFieldConfigEntity> findByCharacterSetAndEnabledOrderByConfigIdAscFieldOrderAsc(String characterSet, String enabled);
    
    /**
     * Find field configurations with default values.
     */
    @Query("SELECT f FROM SqlLoaderFieldConfigEntity f WHERE f.defaultValue IS NOT NULL AND f.defaultValue <> '' AND f.enabled = :enabled ORDER BY f.configId, f.fieldOrder")
    List<SqlLoaderFieldConfigEntity> findFieldsWithDefaultValuesAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Find field configurations with format masks.
     */
    @Query("SELECT f FROM SqlLoaderFieldConfigEntity f WHERE f.formatMask IS NOT NULL AND f.formatMask <> '' AND f.enabled = :enabled ORDER BY f.configId, f.fieldOrder")
    List<SqlLoaderFieldConfigEntity> findFieldsWithFormatMasksAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Find field configurations with check constraints.
     */
    @Query("SELECT f FROM SqlLoaderFieldConfigEntity f WHERE f.checkConstraint IS NOT NULL AND f.checkConstraint <> '' AND f.enabled = :enabled ORDER BY f.configId, f.fieldOrder")
    List<SqlLoaderFieldConfigEntity> findFieldsWithCheckConstraintsAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Find field configurations for fixed-width files.
     */
    @Query("SELECT f FROM SqlLoaderFieldConfigEntity f WHERE f.fieldPosition IS NOT NULL AND f.fieldLength IS NOT NULL AND f.fieldLength > 0 AND f.enabled = :enabled ORDER BY f.configId, f.fieldOrder")
    List<SqlLoaderFieldConfigEntity> findFixedWidthFieldsAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Find field configurations by specific config ID and data type.
     */
    List<SqlLoaderFieldConfigEntity> findByConfigIdAndDataTypeAndEnabledOrderByFieldOrder(
            String configId, SqlLoaderFieldConfigEntity.SqlLoaderDataType dataType, String enabled);
    
    /**
     * Count field configurations by config ID.
     */
    long countByConfigIdAndEnabled(String configId, String enabled);
    
    /**
     * Count encrypted fields by config ID.
     */
    @Query("SELECT COUNT(f) FROM SqlLoaderFieldConfigEntity f WHERE f.configId = :configId AND f.encrypted = 'Y' AND f.enabled = :enabled")
    Long countEncryptedFieldsByConfigIdAndEnabled(@Param("configId") String configId, @Param("enabled") String enabled);
    
    /**
     * Count audit fields by config ID.
     */
    @Query("SELECT COUNT(f) FROM SqlLoaderFieldConfigEntity f WHERE f.configId = :configId AND f.auditField = 'Y' AND f.enabled = :enabled")
    Long countAuditFieldsByConfigIdAndEnabled(@Param("configId") String configId, @Param("enabled") String enabled);
    
    /**
     * Find field configurations with length constraints.
     */
    @Query("SELECT f FROM SqlLoaderFieldConfigEntity f WHERE (f.maxLength IS NOT NULL OR f.minLength IS NOT NULL) AND f.enabled = :enabled ORDER BY f.configId, f.fieldOrder")
    List<SqlLoaderFieldConfigEntity> findFieldsWithLengthConstraintsAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Find field configurations with specific length range.
     */
    @Query("SELECT f FROM SqlLoaderFieldConfigEntity f WHERE f.maxLength BETWEEN :minLength AND :maxLength AND f.enabled = :enabled ORDER BY f.maxLength DESC")
    List<SqlLoaderFieldConfigEntity> findByMaxLengthBetweenAndEnabled(
            @Param("minLength") Integer minLength, @Param("maxLength") Integer maxLength, @Param("enabled") String enabled);
    
    /**
     * Get field statistics by data type.
     */
    @Query("SELECT f.dataType, COUNT(f), AVG(f.maxLength), SUM(CASE WHEN f.encrypted = 'Y' THEN 1 ELSE 0 END) FROM SqlLoaderFieldConfigEntity f WHERE f.enabled = :enabled GROUP BY f.dataType")
    List<Object[]> getFieldStatsByDataType(@Param("enabled") String enabled);
    
    /**
     * Get field statistics by config ID.
     */
    @Query("SELECT f.configId, COUNT(f), " +
           "SUM(CASE WHEN f.encrypted = 'Y' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN f.auditField = 'Y' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN f.nullable = 'N' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN f.uniqueConstraint = 'Y' THEN 1 ELSE 0 END) " +
           "FROM SqlLoaderFieldConfigEntity f WHERE f.enabled = :enabled GROUP BY f.configId")
    List<Object[]> getFieldStatsByConfigId(@Param("enabled") String enabled);
    
    /**
     * Find field configurations with encryption functions.
     */
    @Query("SELECT f FROM SqlLoaderFieldConfigEntity f WHERE f.encryptionFunction IS NOT NULL AND f.encryptionFunction <> '' AND f.enabled = :enabled ORDER BY f.configId, f.fieldOrder")
    List<SqlLoaderFieldConfigEntity> findFieldsWithEncryptionFunctionsAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Find field configurations by encryption function.
     */
    List<SqlLoaderFieldConfigEntity> findByEncryptionFunctionAndEnabledOrderByConfigIdAscFieldOrderAsc(String encryptionFunction, String enabled);
    
    /**
     * Find field configurations with transformations applied.
     */
    @Query("SELECT f FROM SqlLoaderFieldConfigEntity f WHERE f.transformationApplied IS NOT NULL AND f.transformationApplied <> '' AND f.enabled = :enabled ORDER BY f.configId, f.fieldOrder")
    List<SqlLoaderFieldConfigEntity> findFieldsWithTransformationsAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Find field configurations with data lineage tracking.
     */
    @Query("SELECT f FROM SqlLoaderFieldConfigEntity f WHERE f.dataLineage IS NOT NULL AND f.dataLineage <> '' AND f.enabled = :enabled ORDER BY f.configId, f.fieldOrder")
    List<SqlLoaderFieldConfigEntity> findFieldsWithDataLineageAndEnabled(@Param("enabled") String enabled);
    
    /**
     * Check if field exists in config by field name.
     */
    boolean existsByConfigIdAndFieldNameAndEnabled(String configId, String fieldName, String enabled);
    
    /**
     * Check if column exists in config by column name.
     */
    boolean existsByConfigIdAndColumnNameAndEnabled(String configId, String columnName, String enabled);
    
    /**
     * Find field configurations by field order range for a config.
     */
    @Query("SELECT f FROM SqlLoaderFieldConfigEntity f WHERE f.configId = :configId AND f.fieldOrder BETWEEN :startOrder AND :endOrder AND f.enabled = :enabled ORDER BY f.fieldOrder")
    List<SqlLoaderFieldConfigEntity> findByConfigIdAndFieldOrderBetweenAndEnabled(
            @Param("configId") String configId, 
            @Param("startOrder") Integer startOrder, 
            @Param("endOrder") Integer endOrder, 
            @Param("enabled") String enabled);
    
    /**
     * Get maximum field order for a config.
     */
    @Query("SELECT MAX(f.fieldOrder) FROM SqlLoaderFieldConfigEntity f WHERE f.configId = :configId AND f.enabled = :enabled")
    Integer getMaxFieldOrderByConfigId(@Param("configId") String configId, @Param("enabled") String enabled);
    
    /**
     * Find field configurations created by specific user.
     */
    List<SqlLoaderFieldConfigEntity> findByCreatedByAndEnabledOrderByCreatedDateDesc(String createdBy, String enabled);
    
    /**
     * Find field configurations by lookup table.
     */
    List<SqlLoaderFieldConfigEntity> findByLookupTableAndEnabledOrderByConfigIdAscFieldOrderAsc(String lookupTable, String enabled);
    
    /**
     * Find field configurations with specific business rule class.
     */
    List<SqlLoaderFieldConfigEntity> findByBusinessRuleClassAndEnabledOrderByConfigIdAscFieldOrderAsc(String businessRuleClass, String enabled);
    
    /**
     * Get field configuration security summary.
     */
    @Query("SELECT " +
           "SUM(CASE WHEN f.encrypted = 'Y' THEN 1 ELSE 0 END) as encrypted_fields, " +
           "SUM(CASE WHEN f.auditField = 'Y' THEN 1 ELSE 0 END) as audit_fields, " +
           "SUM(CASE WHEN f.uniqueConstraint = 'Y' THEN 1 ELSE 0 END) as unique_fields, " +
           "SUM(CASE WHEN f.primaryKey = 'Y' THEN 1 ELSE 0 END) as primary_key_fields, " +
           "SUM(CASE WHEN f.validationRule IS NOT NULL AND f.validationRule <> '' THEN 1 ELSE 0 END) as validated_fields " +
           "FROM SqlLoaderFieldConfigEntity f WHERE f.configId = :configId AND f.enabled = :enabled")
    Object[] getFieldSecuritySummaryByConfigId(@Param("configId") String configId, @Param("enabled") String enabled);
    
    /**
     * Find fields that require special handling (encrypted, audit, or business rules).
     */
    @Query("SELECT f FROM SqlLoaderFieldConfigEntity f WHERE " +
           "(f.encrypted = 'Y' OR f.auditField = 'Y' OR " +
           "(f.businessRuleClass IS NOT NULL AND f.businessRuleClass <> '') OR " +
           "(f.sqlExpression IS NOT NULL AND f.sqlExpression <> '')) " +
           "AND f.configId = :configId AND f.enabled = :enabled ORDER BY f.fieldOrder")
    List<SqlLoaderFieldConfigEntity> findFieldsRequiringSpecialHandling(@Param("configId") String configId, @Param("enabled") String enabled);
    
    /**
     * Delete field configurations by config ID (for cascading delete operations).
     */
    void deleteByConfigIdAndEnabled(String configId, String enabled);
    
    /**
     * Count total field configurations.
     */
    long countByEnabled(String enabled);
}