package com.truist.batch.service;

import com.truist.batch.dto.CreateSourceSystemRequest;
import com.truist.batch.dto.SourceSystemInfo;
import com.truist.batch.dto.SourceSystemWithUsage;

import java.util.List;

/**
 * Service interface for Source System management in US002 Template Configuration Enhancement
 */
public interface SourceSystemService {

    /**
     * Get all enabled source systems
     * 
     * @return List of enabled source systems
     */
    List<SourceSystemInfo> getAllEnabledSourceSystems();

    /**
     * Get all source systems (enabled and disabled)
     * 
     * @return List of all source systems
     */
    List<SourceSystemInfo> getAllSourceSystems();

    /**
     * Get source system by ID
     * 
     * @param id Source system ID
     * @return Source system info or null if not found
     */
    SourceSystemInfo getSourceSystemById(String id);

    /**
     * Check if source system exists by ID
     * 
     * @param id Source system ID
     * @return true if exists, false otherwise
     */
    boolean existsById(String id);

    /**
     * Create a new source system
     * 
     * @param request Source system creation request
     * @return Created source system info
     */
    SourceSystemInfo createSourceSystem(CreateSourceSystemRequest request);

    /**
     * Update an existing source system
     * 
     * @param id Source system ID
     * @param request Source system update request
     * @return Updated source system info
     */
    SourceSystemInfo updateSourceSystem(String id, CreateSourceSystemRequest request);

    /**
     * Disable a source system (soft delete)
     * 
     * @param id Source system ID
     */
    void disableSourceSystem(String id);

    /**
     * Enable a source system
     * 
     * @param id Source system ID
     */
    void enableSourceSystem(String id);

    /**
     * Get source systems with usage information for a specific template
     * 
     * @param fileType Template file type
     * @param transactionType Template transaction type
     * @return List of source systems with usage information
     */
    List<SourceSystemWithUsage> getSourceSystemsWithUsageForTemplate(String fileType, String transactionType);

    /**
     * Increment job count for a source system
     * 
     * @param sourceSystemId Source system ID
     */
    void incrementJobCount(String sourceSystemId);

    /**
     * Decrement job count for a source system
     * 
     * @param sourceSystemId Source system ID
     */
    void decrementJobCount(String sourceSystemId);
}