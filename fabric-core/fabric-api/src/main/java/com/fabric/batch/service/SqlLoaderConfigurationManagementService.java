package com.fabric.batch.service;

import com.fabric.batch.dto.*;
import com.fabric.batch.dto.SqlLoaderReports.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stub interface for SqlLoaderConfigurationManagementService.
 * This would be implemented in the fabric-data-loader module.
 * For Phase 1.2 testing purposes, we provide stub methods.
 */
public interface SqlLoaderConfigurationManagementService {
    
    /**
     * Create a new SQL*Loader configuration entity.
     */
    Object createConfiguration(Object configEntity, List<Object> fieldEntities, String createdBy, String reason);
    
    /**
     * Update an existing SQL*Loader configuration entity.
     */
    Object updateConfiguration(String configId, Object configEntity, List<Object> fieldEntities, String modifiedBy, String reason);
    
    /**
     * Get configuration entity by ID.
     */
    Object getConfigurationById(String configId);
    
    /**
     * Get configuration by source system and job name.
     */
    Optional<Object> getConfigurationBySourceAndJob(String sourceSystem, String jobName);
    
    /**
     * Delete configuration (soft delete).
     */
    void deleteConfiguration(String configId, String deletedBy, String reason);
    
    /**
     * Convert configuration to execution config.
     */
    com.fabric.batch.sqlloader.SqlLoaderConfig convertToExecutionConfig(String configId);
    
    /**
     * Get compliance report for configuration.
     */
    ComplianceReport getComplianceReport(String configId);
    
    /**
     * Get performance report for configuration.
     */
    PerformanceReport getPerformanceReport(String configId);
}