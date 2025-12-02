package com.fabric.batch.repository;

import com.fabric.batch.entity.ManualJobConfigEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Manual Job Configuration management in fabric-batch module.
 * 
 * This interface defines data access operations for batch job configurations.
 * It is specifically designed for the fabric-batch module to avoid dependencies 
 * on the fabric-api module while providing necessary data access methods.
 * 
 * Implementation Note:
 * This interface should be implemented using Spring's JdbcTemplate or similar
 * data access technology. The implementation will handle direct database access
 * for batch processing operations.
 * 
 * Security Considerations:
 * - All database operations should include proper authorization checks
 * - Sensitive configuration parameters should be handled appropriately
 * - Audit trail requirements must be met for SOX compliance
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since fabric-batch module
 */
public interface ManualJobConfigRepository {
    
    // Basic CRUD operations
    
    /**
     * Find a manual job configuration by its unique ID.
     * 
     * @param id the configuration ID
     * @return Optional containing the configuration if found, empty otherwise
     */
    Optional<ManualJobConfigEntity> findById(String id);
    
    /**
     * Find a manual job configuration by its config ID.
     * This is an alias for findById for better semantic clarity.
     * 
     * @param configId the configuration ID
     * @return Optional containing the configuration if found, empty otherwise
     */
    Optional<ManualJobConfigEntity> findByConfigId(String configId);
    
    /**
     * Retrieve all manual job configurations.
     * 
     * @return List of all configurations
     */
    List<ManualJobConfigEntity> findAll();
    
    /**
     * Save a manual job configuration entity.
     * Handles both insert and update operations.
     * 
     * @param entity the configuration entity to save
     * @return the saved entity with updated metadata
     */
    ManualJobConfigEntity save(ManualJobConfigEntity entity);
    
    /**
     * Delete a manual job configuration by its ID.
     * 
     * @param id the configuration ID to delete
     */
    void deleteById(String id);
    
    /**
     * Check if a configuration exists by its ID.
     * 
     * @param id the configuration ID
     * @return true if configuration exists, false otherwise
     */
    boolean existsById(String id);
    
    /**
     * Count total number of configurations.
     * 
     * @return total count of configurations
     */
    long count();
    
    // Query methods for filtering and searching
    
    /**
     * Find configurations by job name.
     * 
     * @param jobName the job name to search for
     * @return List of matching configurations
     */
    List<ManualJobConfigEntity> findByJobName(String jobName);
    
    /**
     * Find configurations by status.
     * 
     * @param status the status to filter by (ACTIVE, INACTIVE, DEPRECATED)
     * @return List of configurations with the specified status
     */
    List<ManualJobConfigEntity> findByStatus(String status);
    
    /**
     * Find configurations by source system.
     * 
     * @param sourceSystem the source system identifier
     * @return List of configurations for the specified source system
     */
    List<ManualJobConfigEntity> findBySourceSystem(String sourceSystem);
    
    /**
     * Find configurations by job type.
     * 
     * @param jobType the job type to filter by
     * @return List of configurations of the specified type
     */
    List<ManualJobConfigEntity> findByJobType(String jobType);
    
    /**
     * Find configurations by master query ID.
     * 
     * @param masterQueryId the master query ID
     * @return List of configurations linked to the specified master query
     */
    List<ManualJobConfigEntity> findByMasterQueryId(String masterQueryId);
    
    // Combined filter methods
    
    /**
     * Find configurations by job type, source system, and status.
     * 
     * @param jobType the job type
     * @param sourceSystem the source system
     * @param status the status
     * @return List of matching configurations
     */
    List<ManualJobConfigEntity> findByJobTypeAndSourceSystemAndStatus(String jobType, String sourceSystem, String status);
    
    /**
     * Find configurations by job type and status.
     * 
     * @param jobType the job type
     * @param status the status
     * @return List of matching configurations
     */
    List<ManualJobConfigEntity> findByJobTypeAndStatus(String jobType, String status);
    
    /**
     * Find configurations by source system and status.
     * 
     * @param sourceSystem the source system
     * @param status the status
     * @return List of matching configurations
     */
    List<ManualJobConfigEntity> findBySourceSystemAndStatus(String sourceSystem, String status);
    
    // Existence and count methods
    
    /**
     * Check if an active configuration exists with the specified job name.
     * 
     * @param jobName the job name to check
     * @return true if an active configuration exists, false otherwise
     */
    boolean existsActiveConfigurationByJobName(String jobName);
    
    /**
     * Count configurations by status.
     * 
     * @param status the status to count
     * @return number of configurations with the specified status
     */
    long countByStatus(String status);
    
    // Time-based query methods
    
    /**
     * Count configurations created today.
     * 
     * @return number of configurations created today
     */
    long countConfigurationsCreatedToday();
    
    /**
     * Count configurations modified this week.
     * 
     * @return number of configurations modified in the current week
     */
    long countConfigurationsModifiedThisWeek();
    
    // Batch-specific methods
    
    /**
     * Find all active configurations suitable for batch execution.
     * 
     * @return List of active configurations ready for batch processing
     */
    List<ManualJobConfigEntity> findActiveConfigurationsForBatch();
    
    /**
     * Find configurations by source system that are ready for execution.
     * 
     * @param sourceSystem the source system
     * @return List of executable configurations for the source system
     */
    List<ManualJobConfigEntity> findExecutableConfigurationsBySourceSystem(String sourceSystem);
}