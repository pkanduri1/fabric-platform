package com.truist.batch.repository;

import com.truist.batch.entity.BatchConfigurationEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Batch Configuration management.
 * Reads field mapping configurations from BATCH_CONFIGURATIONS table.
 *
 * @author Claude Code
 * @since Phase 2 - Batch Module Separation
 */
public interface BatchConfigurationRepository {

    /**
     * Find a batch configuration by its ID
     *
     * @param id the configuration ID
     * @return Optional containing the configuration if found
     */
    Optional<BatchConfigurationEntity> findById(String id);

    /**
     * Find batch configuration by source system and job name
     *
     * @param sourceSystem the source system identifier
     * @param jobName the job name
     * @return Optional containing the configuration if found
     */
    Optional<BatchConfigurationEntity> findBySourceSystemAndJobName(String sourceSystem, String jobName);

    /**
     * Find all enabled batch configurations
     *
     * @return List of enabled configurations
     */
    List<BatchConfigurationEntity> findAllEnabled();

    /**
     * Find configurations by source system
     *
     * @param sourceSystem the source system identifier
     * @return List of configurations for the source system
     */
    List<BatchConfigurationEntity> findBySourceSystem(String sourceSystem);
}
