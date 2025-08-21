package com.truist.batch.dao;

import com.truist.batch.entity.TemplateMasterQueryMappingEntity;

import java.util.List;
import java.util.Optional;

/**
 * =========================================================================
 * TEMPLATE MASTER QUERY MAPPING DAO INTERFACE
 * =========================================================================
 * 
 * Purpose: Data Access Object for TEMPLATE_MASTER_QUERY_MAPPING table operations
 * - Provides CRUD operations for template-query mappings
 * - Supports query-based lookups and validation
 * - Implements banking-grade data access patterns
 * 
 * Security: All operations use parameterized queries for SQL injection protection
 * Performance: Optimized with proper indexing on lookup columns
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Phase 3 - Master Query Integration
 * =========================================================================
 */
public interface TemplateMasterQueryMappingDao {

    // =========================================================================
    // CORE CRUD OPERATIONS
    // =========================================================================
    
    /**
     * Save template-master query mapping.
     * 
     * @param mapping The mapping entity to save
     * @return Saved mapping entity with generated ID
     */
    TemplateMasterQueryMappingEntity save(TemplateMasterQueryMappingEntity mapping);
    
    /**
     * Find mapping by mapping ID.
     * 
     * @param mappingId Unique mapping identifier
     * @return Optional mapping entity
     */
    Optional<TemplateMasterQueryMappingEntity> findByMappingId(String mappingId);
    
    /**
     * Find all mappings for a configuration.
     * 
     * @param configId Configuration identifier
     * @return List of mappings for the configuration
     */
    List<TemplateMasterQueryMappingEntity> findByConfigId(String configId);
    
    /**
     * Find mapping by configuration and master query.
     * 
     * @param configId Configuration identifier
     * @param masterQueryId Master query identifier
     * @return Optional mapping entity
     */
    Optional<TemplateMasterQueryMappingEntity> findByConfigIdAndMasterQueryId(String configId, String masterQueryId);
    
    /**
     * Update existing mapping.
     * 
     * @param mapping The mapping entity to update
     * @return Updated mapping entity
     */
    TemplateMasterQueryMappingEntity update(TemplateMasterQueryMappingEntity mapping);
    
    /**
     * Delete mapping by mapping ID.
     * 
     * @param mappingId Unique mapping identifier
     */
    void deleteByMappingId(String mappingId);
    
    /**
     * Delete all mappings for a configuration.
     * 
     * @param configId Configuration identifier
     */
    void deleteByConfigId(String configId);

    // =========================================================================
    // QUERY OPERATIONS
    // =========================================================================
    
    /**
     * Find all active mappings.
     * 
     * @return List of active mappings
     */
    List<TemplateMasterQueryMappingEntity> findActiveMapping();
    
    /**
     * Find mappings by status.
     * 
     * @param status Status filter (ACTIVE, INACTIVE, DEPRECATED)
     * @return List of mappings with specified status
     */
    List<TemplateMasterQueryMappingEntity> findByStatus(String status);
    
    /**
     * Find mappings by master query ID.
     * 
     * @param masterQueryId Master query identifier
     * @return List of mappings using the specified master query
     */
    List<TemplateMasterQueryMappingEntity> findByMasterQueryId(String masterQueryId);
    
    /**
     * Find mappings by query name.
     * 
     * @param queryName Query name to search for
     * @return List of mappings with specified query name
     */
    List<TemplateMasterQueryMappingEntity> findByQueryName(String queryName);

    // =========================================================================
    // VALIDATION AND UTILITY OPERATIONS
    // =========================================================================
    
    /**
     * Check if mapping exists for configuration.
     * 
     * @param configId Configuration identifier
     * @return True if mapping exists, false otherwise
     */
    boolean existsByConfigId(String configId);
    
    /**
     * Check if mapping exists for configuration and master query.
     * 
     * @param configId Configuration identifier
     * @param masterQueryId Master query identifier
     * @return True if mapping exists, false otherwise
     */
    boolean existsByConfigIdAndMasterQueryId(String configId, String masterQueryId);
    
    /**
     * Count total mappings.
     * 
     * @return Total count of mappings
     */
    long count();
    
    /**
     * Count active mappings.
     * 
     * @return Count of active mappings
     */
    long countActive();
    
    /**
     * Count mappings for a configuration.
     * 
     * @param configId Configuration identifier
     * @return Count of mappings for the configuration
     */
    long countByConfigId(String configId);

    // =========================================================================
    // SECURITY AND COMPLIANCE OPERATIONS
    // =========================================================================
    
    /**
     * Find mappings requiring approval.
     * 
     * @return List of mappings that require approval for execution
     */
    List<TemplateMasterQueryMappingEntity> findMappingsRequiringApproval();
    
    /**
     * Find mappings by security classification.
     * 
     * @param securityClassification Security level (PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED)
     * @return List of mappings with specified security classification
     */
    List<TemplateMasterQueryMappingEntity> findBySecurityClassification(String securityClassification);
    
    /**
     * Find read-only mappings.
     * 
     * @return List of read-only mappings
     */
    List<TemplateMasterQueryMappingEntity> findReadOnlyMappings();

    // =========================================================================
    // AUDIT AND MONITORING OPERATIONS
    // =========================================================================
    
    /**
     * Find mappings created by specific user.
     * 
     * @param createdBy User who created the mappings
     * @return List of mappings created by the user
     */
    List<TemplateMasterQueryMappingEntity> findByCreatedBy(String createdBy);
    
    /**
     * Find mappings with correlation ID.
     * 
     * @param correlationId Correlation identifier for audit trail
     * @return List of mappings with specified correlation ID
     */
    List<TemplateMasterQueryMappingEntity> findByCorrelationId(String correlationId);
    
    /**
     * Get all mappings for audit reporting.
     * 
     * @return List of all mappings for audit purposes
     */
    List<TemplateMasterQueryMappingEntity> findAllForAudit();
}