package com.fabric.batch.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fabric.batch.entity.FieldTemplateEntity;
import com.fabric.batch.entity.FieldTemplateId;

@Repository
public interface FieldTemplateRepository extends JpaRepository<FieldTemplateEntity, FieldTemplateId> {
    
    List<FieldTemplateEntity> findByFileTypeAndTransactionTypeOrderByTargetPosition(String fileType, String transactionType);
    
    
    
    @Query("SELECT COUNT(f) FROM FieldTemplateEntity f WHERE f.fileType = :fileType AND f.transactionType = :transactionType AND f.enabled = 'Y'")
    long countEnabledFieldsByFileTypeAndTransactionType(@Param("fileType") String fileType, @Param("transactionType") String transactionType);
    
    @Query("SELECT MAX(f.targetPosition) FROM FieldTemplateEntity f WHERE f.fileType = :fileType AND f.transactionType = :transactionType")
    Integer getMaxTargetPositionByFileTypeAndTransactionType(@Param("fileType") String fileType, @Param("transactionType") String transactionType);
    
    @Query("SELECT SUM(f.length) FROM FieldTemplateEntity f WHERE f.fileType = :fileType AND f.transactionType = :transactionType AND f.enabled = 'Y'")
    Integer getTotalRecordLengthByFileTypeAndTransactionType(@Param("fileType") String fileType, @Param("transactionType") String transactionType);
    
    @Query("SELECT f FROM FieldTemplateEntity f WHERE f.fileType = :fileType AND f.transactionType = :transactionType AND f.required = 'Y' AND f.enabled = 'Y'")
    List<FieldTemplateEntity> findRequiredFieldsByFileTypeAndTransactionType(@Param("fileType") String fileType, @Param("transactionType") String transactionType);
    
    @Query("SELECT DISTINCT f.fileType FROM FieldTemplateEntity f WHERE f.enabled = 'Y'")
    List<String> findAllEnabledFileTypes();
    
    
    /**
     * Find by composite key
     */
    Optional<FieldTemplateEntity> findByFileTypeAndTransactionTypeAndFieldName(
        String fileType, String transactionType, String fieldName);
    
    /**
     * Find by position (to check for conflicts)
     */
    Optional<FieldTemplateEntity> findByFileTypeAndTransactionTypeAndTargetPosition(
        String fileType, String transactionType, Integer targetPosition);
    
    /**
     * Find all fields for a file type and transaction type (ordered by position)
     */
    List<FieldTemplateEntity> findByFileTypeAndTransactionTypeAndEnabledOrderByTargetPosition(
        String fileType, String transactionType, String enabled);
    
    /**
     * Find all transaction types for a file type
     */
    @Query("SELECT DISTINCT ft.transactionType FROM FieldTemplateEntity ft WHERE ft.fileType = :fileType AND ft.enabled = 'Y'")
    List<String> findTransactionTypesByFileType(@Param("fileType") String fileType);
    
    /**
     * Find all fields for a file type (across all transaction types)
     */
    List<FieldTemplateEntity> findByFileTypeAndEnabledOrderByTransactionTypeAscTargetPositionAsc(
        String fileType, String enabled);
    
    /**
     * Count fields for a file type and transaction type
     */
    Long countByFileTypeAndTransactionTypeAndEnabled(String fileType, String transactionType, String enabled);
    
    /**
     * Find fields by file type with pagination support
     */
    @Query("SELECT ft FROM FieldTemplateEntity ft WHERE ft.fileType = :fileType AND ft.enabled = :enabled ORDER BY ft.transactionType, ft.targetPosition")
    List<FieldTemplateEntity> findByFileTypeAndEnabledOrderByTransactionTypeAndPosition(
        @Param("fileType") String fileType, @Param("enabled") String enabled);
    
    /**
     * Delete all fields for a file type and transaction type (for cleanup)
     */
    void deleteByFileTypeAndTransactionType(String fileType, String transactionType);
    
    /**
     * Find maximum position for a file type and transaction type
     */
    @Query("SELECT COALESCE(MAX(ft.targetPosition), 0) FROM FieldTemplateEntity ft WHERE ft.fileType = :fileType AND ft.transactionType = :transactionType AND ft.enabled = 'Y'")
    Integer findMaxPositionByFileTypeAndTransactionType(
        @Param("fileType") String fileType, @Param("transactionType") String transactionType);

    // Consumer Default Template Enhancement Methods - COMMENTED OUT (fields don't exist in Oracle)
    
    // /**
    //  * Find templates by category
    //  */
    // @Query("SELECT ft FROM FieldTemplateEntity ft WHERE ft.templateCategory = :templateCategory AND ft.enabled = 'Y' ORDER BY ft.fileType, ft.transactionType, ft.targetPosition")
    // List<FieldTemplateEntity> findByTemplateCategory(@Param("templateCategory") String templateCategory);
    
    // /**
    //  * Find templates by category and file type
    //  */
    // @Query("SELECT ft FROM FieldTemplateEntity ft WHERE ft.templateCategory = :templateCategory AND ft.fileType = :fileType AND ft.enabled = 'Y' ORDER BY ft.transactionType, ft.targetPosition")
    // List<FieldTemplateEntity> findByTemplateCategoryAndFileType(
    //     @Param("templateCategory") String templateCategory, @Param("fileType") String fileType);
    
    // /**
    //  * Find templates by PII classification
    //  */
    // @Query("SELECT ft FROM FieldTemplateEntity ft WHERE ft.piiClassification = :piiClassification AND ft.enabled = 'Y' ORDER BY ft.fileType, ft.transactionType, ft.targetPosition")
    // List<FieldTemplateEntity> findByPiiClassification(@Param("piiClassification") String piiClassification);
    
    // /**
    //  * Find fields requiring encryption
    //  */
    // @Query("SELECT ft FROM FieldTemplateEntity ft WHERE ft.encryptionRequired = 'Y' AND ft.enabled = 'Y' ORDER BY ft.fileType, ft.transactionType, ft.targetPosition")
    // List<FieldTemplateEntity> findFieldsRequiringEncryption();
    
    // /**
    //  * Find consumer default templates by file type and transaction type
    //  */
    // @Query("SELECT ft FROM FieldTemplateEntity ft WHERE ft.fileType = :fileType AND ft.transactionType = :transactionType AND ft.templateCategory LIKE 'CONSUMER%' AND ft.enabled = 'Y' ORDER BY ft.targetPosition")
    // List<FieldTemplateEntity> findConsumerDefaultTemplates(
    //     @Param("fileType") String fileType, @Param("transactionType") String transactionType);
    
    // /**
    //  * Find high-risk PII fields for enhanced protection
    //  */
    // @Query("SELECT ft FROM FieldTemplateEntity ft WHERE ft.piiClassification IN ('HIGH', 'MEDIUM') AND ft.templateCategory LIKE 'CONSUMER%' AND ft.enabled = 'Y' ORDER BY ft.piiClassification DESC, ft.fileType, ft.targetPosition")
    // List<FieldTemplateEntity> findHighRiskPIIFields();
    
    // /**
    //  * Count templates by category
    //  */
    // @Query("SELECT COUNT(ft) FROM FieldTemplateEntity ft WHERE ft.templateCategory = :templateCategory AND ft.enabled = 'Y'")
    // Long countByTemplateCategory(@Param("templateCategory") String templateCategory);
    
    // /**
    //  * Count fields by PII classification
    //  */
    // @Query("SELECT COUNT(ft) FROM FieldTemplateEntity ft WHERE ft.piiClassification = :piiClassification AND ft.enabled = 'Y'")
    // Long countByPiiClassification(@Param("piiClassification") String piiClassification);
    
    // /**
    //  * Find templates with consumer default rules configured
    //  */
    // @Query("SELECT ft FROM FieldTemplateEntity ft WHERE ft.consumerDefaultRules IS NOT NULL AND ft.enabled = 'Y' ORDER BY ft.fileType, ft.transactionType, ft.targetPosition")
    // List<FieldTemplateEntity> findTemplatesWithConsumerRules();
    
    // /**
    //  * Find distinct template categories
    //  */
    // @Query("SELECT DISTINCT ft.templateCategory FROM FieldTemplateEntity ft WHERE ft.enabled = 'Y' ORDER BY ft.templateCategory")
    // List<String> findDistinctTemplateCategories();
    
    // /**
    //  * Find distinct PII classifications
    //  */
    // @Query("SELECT DISTINCT ft.piiClassification FROM FieldTemplateEntity ft WHERE ft.enabled = 'Y' ORDER BY ft.piiClassification")
    // List<String> findDistinctPiiClassifications();
    
    // /**
    //  * Find templates by risk level
    //  */
    // @Query("SELECT ft FROM FieldTemplateEntity ft WHERE ft.riskLevel = :riskLevel AND ft.enabled = 'Y' ORDER BY ft.fileType, ft.transactionType, ft.targetPosition")
    // List<FieldTemplateEntity> findByRiskLevel(@Param("riskLevel") String riskLevel);
    
    // /**
    //  * Find templates requiring audit logging (MEDIUM or HIGH PII)
    //  */
    // @Query("SELECT ft FROM FieldTemplateEntity ft WHERE ft.piiClassification IN ('MEDIUM', 'HIGH') AND ft.enabled = 'Y' ORDER BY ft.piiClassification DESC, ft.fileType, ft.targetPosition")
    // List<FieldTemplateEntity> findTemplatesRequiringAuditLogging();
    
    // /**
    //  * Template summary statistics by category
    //  */
    // @Query("SELECT ft.templateCategory, COUNT(ft), SUM(CASE WHEN ft.required = 'Y' THEN 1 ELSE 0 END), SUM(CASE WHEN ft.encryptionRequired = 'Y' THEN 1 ELSE 0 END) " +
    //        "FROM FieldTemplateEntity ft WHERE ft.enabled = 'Y' GROUP BY ft.templateCategory ORDER BY ft.templateCategory")
    // List<Object[]> getTemplateCategoryStatistics();
    
    // /**
    //  * Delete templates by category (for maintenance operations)
    //  */
    // @Query("UPDATE FieldTemplateEntity ft SET ft.enabled = 'N', ft.modifiedDate = CURRENT_TIMESTAMP WHERE ft.templateCategory = :templateCategory")
    // int softDeleteByTemplateCategory(@Param("templateCategory") String templateCategory);

}
