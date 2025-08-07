package com.truist.batch.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.truist.batch.entity.TransformationConfigEntity;

/**
 * Repository for managing transformation configurations.
 * 
 * Provides data access methods for database-driven transformation rules
 * supporting migration from Excel-based mappings to structured database configuration.
 */
@Repository
public interface TransformationConfigRepository extends JpaRepository<TransformationConfigEntity, String> {
    
    /**
     * Find transformation configurations by source system
     */
    List<TransformationConfigEntity> findBySourceSystemAndEnabledOrderByInterfaceNameAsc(
        String sourceSystem, String enabled);
    
    /**
     * Find transformation configuration by system, interface, and transaction type
     */
    Optional<TransformationConfigEntity> findBySourceSystemAndInterfaceNameAndTransactionTypeAndEnabled(
        String sourceSystem, String interfaceName, String transactionType, String enabled);
    
    /**
     * Find all enabled transformation configurations
     */
    List<TransformationConfigEntity> findByEnabledOrderBySourceSystemAscInterfaceNameAsc(String enabled);
    
    /**
     * Find configurations that need validation
     */
    @Query("SELECT tc FROM TransformationConfigEntity tc WHERE tc.validationEnabled = 'Y' AND tc.enabled = 'Y'")
    List<TransformationConfigEntity> findConfigurationsRequiringValidation();
    
    /**
     * Find configurations by interface name pattern
     */
    @Query("SELECT tc FROM TransformationConfigEntity tc WHERE tc.interfaceName LIKE :pattern AND tc.enabled = 'Y'")
    List<TransformationConfigEntity> findByInterfaceNamePattern(@Param("pattern") String pattern);
    
    /**
     * Find configurations created or modified after a specific date
     */
    @Query("SELECT tc FROM TransformationConfigEntity tc WHERE " +
           "(tc.createdDate >= :date OR tc.modifiedDate >= :date) AND tc.enabled = 'Y' " +
           "ORDER BY COALESCE(tc.modifiedDate, tc.createdDate) DESC")
    List<TransformationConfigEntity> findRecentlyModified(@Param("date") LocalDateTime date);
    
    /**
     * Find configurations with specific file types
     */
    List<TransformationConfigEntity> findByFileTypeAndEnabledOrderBySourceSystemAsc(
        String fileType, String enabled);
    
    /**
     * Count configurations by source system
     */
    @Query("SELECT tc.sourceSystem, COUNT(tc) FROM TransformationConfigEntity tc " +
           "WHERE tc.enabled = 'Y' GROUP BY tc.sourceSystem")
    List<Object[]> countConfigurationsBySourceSystem();
    
    /**
     * Find configurations with Excel source file reference
     */
    @Query("SELECT tc FROM TransformationConfigEntity tc WHERE tc.excelSourceFile IS NOT NULL AND tc.enabled = 'Y'")
    List<TransformationConfigEntity> findConfigurationsWithExcelSource();
    
    /**
     * Update active fields count for a configuration
     */
    @Modifying
    @Query("UPDATE TransformationConfigEntity tc SET tc.activeFields = :activeFields, " +
           "tc.modifiedDate = :modifiedDate, tc.modifiedBy = :modifiedBy " +
           "WHERE tc.configId = :configId")
    int updateActiveFieldsCount(@Param("configId") String configId, 
                               @Param("activeFields") Integer activeFields,
                               @Param("modifiedDate") LocalDateTime modifiedDate,
                               @Param("modifiedBy") String modifiedBy);
    
    /**
     * Enable or disable a configuration
     */
    @Modifying
    @Query("UPDATE TransformationConfigEntity tc SET tc.enabled = :enabled, " +
           "tc.modifiedDate = :modifiedDate, tc.modifiedBy = :modifiedBy " +
           "WHERE tc.configId = :configId")
    int updateEnabledStatus(@Param("configId") String configId, 
                           @Param("enabled") String enabled,
                           @Param("modifiedDate") LocalDateTime modifiedDate,
                           @Param("modifiedBy") String modifiedBy);
    
    /**
     * Find configurations that may need migration from Excel
     */
    @Query("SELECT tc FROM TransformationConfigEntity tc WHERE " +
           "tc.totalFields = 0 OR tc.activeFields = 0 OR tc.excelSourceFile IS NOT NULL")
    List<TransformationConfigEntity> findConfigurationsNeedingMigration();
    
    /**
     * Get configuration summary statistics
     */
    @Query("SELECT " +
           "COUNT(tc) as totalConfigs, " +
           "SUM(CASE WHEN tc.enabled = 'Y' THEN 1 ELSE 0 END) as enabledConfigs, " +
           "SUM(CASE WHEN tc.validationEnabled = 'Y' THEN 1 ELSE 0 END) as validationEnabledConfigs, " +
           "AVG(tc.totalFields) as avgFieldsPerConfig " +
           "FROM TransformationConfigEntity tc")
    Object[] getConfigurationStatistics();
    
    /**
     * Check if configuration exists for specific source system and interface
     */
    boolean existsBySourceSystemAndInterfaceNameAndEnabled(
        String sourceSystem, String interfaceName, String enabled);
}