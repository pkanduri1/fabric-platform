package com.fabric.batch.idempotency.repository;

import com.fabric.batch.idempotency.entity.FabricIdempotencyConfigEntity;
import com.fabric.batch.idempotency.entity.FabricIdempotencyConfigEntity.ConfigType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for FabricIdempotencyConfigEntity operations.
 * Provides comprehensive data access methods for idempotency configuration management,
 * pattern matching, and configuration analytics.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Repository
public interface FabricIdempotencyConfigRepository extends JpaRepository<FabricIdempotencyConfigEntity, String> {
    
    // ============================================================================
    // Core Configuration Operations
    // ============================================================================
    
    /**
     * Find configuration by config type for type-specific settings.
     */
    List<FabricIdempotencyConfigEntity> findByConfigTypeAndEnabledOrderByCreatedDateDesc(
        ConfigType configType, String enabled
    );
    
    /**
     * Find configuration by target pattern for pattern matching.
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE c.targetPattern = :targetPattern
        AND c.enabled = 'Y'
        ORDER BY c.createdDate DESC
        """)
    List<FabricIdempotencyConfigEntity> findByTargetPatternAndEnabled(@Param("targetPattern") String targetPattern);
    
    /**
     * Find all enabled configurations for active pattern matching.
     */
    List<FabricIdempotencyConfigEntity> findByEnabledOrderByCreatedDateDesc(String enabled);
    
    /**
     * Find configuration by config type and enabled status.
     */
    List<FabricIdempotencyConfigEntity> findByConfigTypeOrderByCreatedDateDesc(ConfigType configType);
    
    // ============================================================================
    // Pattern Matching Queries
    // ============================================================================
    
    /**
     * Find configurations that match specific target pattern (wildcard support).
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE c.enabled = 'Y'
        AND (c.targetPattern = '*' 
             OR c.targetPattern = :target
             OR :target LIKE REPLACE(c.targetPattern, '*', '%'))
        ORDER BY 
            CASE WHEN c.targetPattern = :target THEN 1
                 WHEN c.targetPattern LIKE '%*%' THEN 2
                 WHEN c.targetPattern = '*' THEN 3
                 ELSE 4 END,
            c.createdDate DESC
        """)
    List<FabricIdempotencyConfigEntity> findMatchingConfigurations(@Param("target") String target);
    
    /**
     * Find best matching configuration for specific target and type.
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE c.enabled = 'Y'
        AND c.configType = :configType
        AND (c.targetPattern = '*' 
             OR c.targetPattern = :target
             OR :target LIKE REPLACE(c.targetPattern, '*', '%'))
        ORDER BY 
            CASE WHEN c.targetPattern = :target THEN 1
                 WHEN c.targetPattern LIKE '%*%' THEN 2
                 WHEN c.targetPattern = '*' THEN 3
                 ELSE 4 END,
            c.createdDate DESC
        """)
    Optional<FabricIdempotencyConfigEntity> findBestMatchingConfiguration(
        @Param("configType") ConfigType configType,
        @Param("target") String target
    );
    
    /**
     * Find configurations with wildcard patterns.
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE c.targetPattern LIKE '%*%'
        AND c.enabled = 'Y'
        ORDER BY c.targetPattern, c.createdDate DESC
        """)
    List<FabricIdempotencyConfigEntity> findWildcardConfigurations();
    
    // ============================================================================
    // Configuration Analytics and Statistics
    // ============================================================================
    
    /**
     * Get configuration statistics by config type.
     */
    @Query("""
        SELECT c.configType, COUNT(c),
               COUNT(CASE WHEN c.enabled = 'Y' THEN 1 END) as enabled_count,
               AVG(c.ttlHours), AVG(c.maxRetries)
        FROM FabricIdempotencyConfigEntity c 
        GROUP BY c.configType
        """)
    List<Object[]> getConfigurationStatisticsByType();
    
    /**
     * Get configuration usage statistics.
     */
    @Query("""
        SELECT c.enabled, COUNT(c), AVG(c.ttlHours), AVG(c.maxRetries)
        FROM FabricIdempotencyConfigEntity c 
        GROUP BY c.enabled
        """)
    List<Object[]> getConfigurationUsageStatistics();
    
    /**
     * Count configurations by enabled status.
     */
    long countByEnabled(String enabled);
    
    /**
     * Count configurations by config type.
     */
    long countByConfigType(ConfigType configType);
    
    /**
     * Count configurations by config type and enabled status.
     */
    long countByConfigTypeAndEnabled(ConfigType configType, String enabled);
    
    // ============================================================================
    // Advanced Configuration Queries
    // ============================================================================
    
    /**
     * Find configurations with specific cleanup policy.
     */
    List<FabricIdempotencyConfigEntity> findByCleanupPolicyOrderByCreatedDateDesc(String cleanupPolicy);
    
    /**
     * Find configurations with specific key generation strategy.
     */
    List<FabricIdempotencyConfigEntity> findByKeyGenerationStrategyOrderByCreatedDateDesc(String keyGenerationStrategy);
    
    /**
     * Find configurations requiring encryption.
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE c.encryptionRequired = 'Y'
        AND c.enabled = 'Y'
        ORDER BY c.createdDate DESC
        """)
    List<FabricIdempotencyConfigEntity> findEncryptionRequiredConfigurations();
    
    /**
     * Find configurations with specific compliance flags.
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE c.complianceFlags LIKE :complianceFlag
        ORDER BY c.createdDate DESC
        """)
    List<FabricIdempotencyConfigEntity> findByComplianceFlag(@Param("complianceFlag") String complianceFlag);
    
    /**
     * Find configurations by TTL range for TTL analysis.
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE c.ttlHours BETWEEN :minTtl AND :maxTtl
        ORDER BY c.ttlHours, c.createdDate DESC
        """)
    List<FabricIdempotencyConfigEntity> findByTtlRange(
        @Param("minTtl") Integer minTtl,
        @Param("maxTtl") Integer maxTtl
    );
    
    /**
     * Find configurations by retry count range.
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE c.maxRetries BETWEEN :minRetries AND :maxRetries
        ORDER BY c.maxRetries, c.createdDate DESC
        """)
    List<FabricIdempotencyConfigEntity> findByRetryRange(
        @Param("minRetries") Integer minRetries,
        @Param("maxRetries") Integer maxRetries
    );
    
    // ============================================================================
    // User and Audit Tracking
    // ============================================================================
    
    /**
     * Find configurations created by specific user.
     */
    List<FabricIdempotencyConfigEntity> findByCreatedByOrderByCreatedDateDesc(String createdBy);
    
    /**
     * Find configurations modified by specific user.
     */
    List<FabricIdempotencyConfigEntity> findByModifiedByOrderByModifiedDateDesc(String modifiedBy);
    
    /**
     * Find configurations created within date range.
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE c.createdDate BETWEEN :startDate AND :endDate
        ORDER BY c.createdDate DESC
        """)
    List<FabricIdempotencyConfigEntity> findByCreatedDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find configurations modified within date range.
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE c.modifiedDate BETWEEN :startDate AND :endDate
        ORDER BY c.modifiedDate DESC
        """)
    List<FabricIdempotencyConfigEntity> findByModifiedDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find recently modified configurations for change tracking.
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE c.modifiedDate >= :fromDate
        ORDER BY c.modifiedDate DESC
        """)
    List<FabricIdempotencyConfigEntity> findRecentlyModified(@Param("fromDate") LocalDateTime fromDate);
    
    // ============================================================================
    // Version and Change Management
    // ============================================================================
    
    /**
     * Find configurations by version for version tracking.
     */
    List<FabricIdempotencyConfigEntity> findByVersionOrderByCreatedDateDesc(Integer version);
    
    /**
     * Find configurations with version greater than specified.
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE c.version > :minVersion
        ORDER BY c.version DESC, c.modifiedDate DESC
        """)
    List<FabricIdempotencyConfigEntity> findByVersionGreaterThan(@Param("minVersion") Integer minVersion);
    
    /**
     * Get latest version configurations.
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE c.version = (
            SELECT MAX(c2.version) 
            FROM FabricIdempotencyConfigEntity c2 
            WHERE c2.configId = c.configId
        )
        ORDER BY c.modifiedDate DESC
        """)
    List<FabricIdempotencyConfigEntity> findLatestVersionConfigurations();
    
    // ============================================================================
    // Search and Filter Operations
    // ============================================================================
    
    /**
     * Find configurations by description pattern for content search.
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE c.description LIKE :descriptionPattern
        ORDER BY c.createdDate DESC
        """)
    List<FabricIdempotencyConfigEntity> findByDescriptionPattern(@Param("descriptionPattern") String descriptionPattern);
    
    /**
     * Find configurations with request payload storage enabled.
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE c.storeRequestPayload = 'Y'
        AND c.enabled = 'Y'
        ORDER BY c.createdDate DESC
        """)
    List<FabricIdempotencyConfigEntity> findWithRequestPayloadStorage();
    
    /**
     * Find configurations with response payload storage enabled.
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE c.storeResponsePayload = 'Y'
        AND c.enabled = 'Y'
        ORDER BY c.createdDate DESC
        """)
    List<FabricIdempotencyConfigEntity> findWithResponsePayloadStorage();
    
    /**
     * Find configurations by multiple criteria for advanced filtering.
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE (:configType IS NULL OR c.configType = :configType)
        AND (:enabled IS NULL OR c.enabled = :enabled)
        AND (:minTtl IS NULL OR c.ttlHours >= :minTtl)
        AND (:maxTtl IS NULL OR c.ttlHours <= :maxTtl)
        AND (:encryptionRequired IS NULL OR c.encryptionRequired = :encryptionRequired)
        ORDER BY c.createdDate DESC
        """)
    List<FabricIdempotencyConfigEntity> findByMultipleCriteria(
        @Param("configType") ConfigType configType,
        @Param("enabled") String enabled,
        @Param("minTtl") Integer minTtl,
        @Param("maxTtl") Integer maxTtl,
        @Param("encryptionRequired") String encryptionRequired
    );
    
    // ============================================================================
    // Maintenance and Health Check Operations
    // ============================================================================
    
    /**
     * Find configurations that might need review (old, high version, etc.).
     */
    @Query("""
        SELECT c FROM FabricIdempotencyConfigEntity c 
        WHERE (c.createdDate < :oldDate 
               AND c.modifiedDate IS NULL)
        OR c.version > :highVersionThreshold
        ORDER BY c.version DESC, c.createdDate ASC
        """)
    List<FabricIdempotencyConfigEntity> findConfigurationsNeedingReview(
        @Param("oldDate") LocalDateTime oldDate,
        @Param("highVersionThreshold") Integer highVersionThreshold
    );
    
    /**
     * Find duplicate target patterns for configuration validation.
     */
    @Query("""
        SELECT c.targetPattern, c.configType, COUNT(*)
        FROM FabricIdempotencyConfigEntity c 
        WHERE c.enabled = 'Y'
        GROUP BY c.targetPattern, c.configType
        HAVING COUNT(*) > 1
        """)
    List<Object[]> findDuplicateTargetPatterns();
    
    /**
     * Get configuration health metrics.
     */
    @Query("""
        SELECT 
            COUNT(*) as total_configs,
            COUNT(CASE WHEN c.enabled = 'Y' THEN 1 END) as enabled_configs,
            AVG(c.version) as avg_version,
            COUNT(CASE WHEN c.modifiedDate IS NOT NULL THEN 1 END) as modified_configs
        FROM FabricIdempotencyConfigEntity c
        """)
    Object[] getConfigurationHealthMetrics();
}