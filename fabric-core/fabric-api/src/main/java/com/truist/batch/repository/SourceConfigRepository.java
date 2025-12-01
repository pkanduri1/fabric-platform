package com.truist.batch.repository;

import com.truist.batch.entity.SourceConfigEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for Source Configuration management.
 * Uses JdbcTemplate implementation for direct database access.
 *
 * This repository supports the Phase 2 database-driven configuration system
 * that enables dynamic property resolution based on source context.
 *
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Phase 2 - Database-Driven Source Configuration System
 */
public interface SourceConfigRepository {

    /**
     * Find a source configuration by its unique ID.
     *
     * @param configId the configuration ID
     * @return Optional containing the configuration if found, empty otherwise
     */
    Optional<SourceConfigEntity> findById(String configId);

    /**
     * Find all source configurations.
     *
     * @return list of all source configurations
     */
    List<SourceConfigEntity> findAll();

    /**
     * Find all active source configurations.
     *
     * @return list of all active source configurations
     */
    List<SourceConfigEntity> findAllActive();

    /**
     * Find all source configurations for a specific source system.
     *
     * @param sourceCode the source system code
     * @return list of source configurations for the specified source
     */
    List<SourceConfigEntity> findBySourceCode(String sourceCode);

    /**
     * Find all active source configurations for a specific source system.
     *
     * @param sourceCode the source system code
     * @return list of active source configurations for the specified source
     */
    List<SourceConfigEntity> findBySourceCodeActive(String sourceCode);

    /**
     * Find a specific configuration by source code and config key.
     *
     * @param sourceCode the source system code
     * @param configKey the configuration key
     * @return Optional containing the configuration if found, empty otherwise
     */
    Optional<SourceConfigEntity> findBySourceCodeAndConfigKey(String sourceCode, String configKey);

    /**
     * Get all active configurations for a source as a property map.
     * This method is optimized for Spring PropertySource integration.
     *
     * @param sourceCode the source system code
     * @return Map of config keys to config values for the specified source
     */
    Map<String, String> getActivePropertiesMapForSource(String sourceCode);

    /**
     * Get all active configurations as a property map.
     * This method loads all active configurations for all sources.
     * Note: If multiple sources have the same key, the last one loaded wins.
     *
     * @return Map of all active config keys to config values
     */
    Map<String, String> getAllActivePropertiesMap();

    /**
     * Save a source configuration (insert or update).
     *
     * @param entity the source configuration entity to save
     * @return the saved source configuration entity
     */
    SourceConfigEntity save(SourceConfigEntity entity);

    /**
     * Delete a source configuration by ID.
     *
     * @param configId the configuration ID
     */
    void deleteById(String configId);

    /**
     * Check if a source configuration exists by ID.
     *
     * @param configId the configuration ID
     * @return true if the configuration exists, false otherwise
     */
    boolean existsById(String configId);

    /**
     * Check if a configuration exists for a specific source and key.
     *
     * @param sourceCode the source system code
     * @param configKey the configuration key
     * @return true if the configuration exists, false otherwise
     */
    boolean existsBySourceCodeAndConfigKey(String sourceCode, String configKey);

    /**
     * Count total number of source configurations.
     *
     * @return total count of source configurations
     */
    long count();

    /**
     * Count active source configurations.
     *
     * @return count of active source configurations
     */
    long countActive();

    /**
     * Count source configurations for a specific source.
     *
     * @param sourceCode the source system code
     * @return count of configurations for the specified source
     */
    long countBySourceCode(String sourceCode);

    /**
     * Get list of distinct source codes.
     *
     * @return list of all distinct source codes
     */
    List<String> findDistinctSourceCodes();

    /**
     * Activate a configuration by ID.
     *
     * @param configId the configuration ID
     * @param modifiedBy the user activating the configuration
     * @return true if activation succeeded, false otherwise
     */
    boolean activate(String configId, String modifiedBy);

    /**
     * Deactivate a configuration by ID.
     *
     * @param configId the configuration ID
     * @param modifiedBy the user deactivating the configuration
     * @return true if deactivation succeeded, false otherwise
     */
    boolean deactivate(String configId, String modifiedBy);

    /**
     * Update configuration value.
     *
     * @param configId the configuration ID
     * @param newValue the new configuration value
     * @param modifiedBy the user making the change
     * @return true if update succeeded, false otherwise
     */
    boolean updateValue(String configId, String newValue, String modifiedBy);
}
