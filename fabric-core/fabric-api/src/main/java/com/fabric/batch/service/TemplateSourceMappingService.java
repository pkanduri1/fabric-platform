package com.fabric.batch.service;

import com.fabric.batch.dto.TemplateSourceMappingRequest;
import com.fabric.batch.dto.TemplateSourceMappingResponse;

import java.util.List;

/**
 * Service interface for Template-Source System field mappings in US002 Template Configuration Enhancement
 */
public interface TemplateSourceMappingService {

    /**
     * Save template-source field mappings
     * 
     * @param request Template-source mapping request
     * @return Mapping response with success details
     */
    TemplateSourceMappingResponse saveTemplateSourceMapping(TemplateSourceMappingRequest request);

    /**
     * Get existing template-source mappings for a specific combination
     * 
     * @param fileType Template file type
     * @param transactionType Template transaction type
     * @param sourceSystemId Source system ID
     * @return Existing field mappings or null if none found
     */
    TemplateSourceMappingResponse getTemplateSourceMapping(String fileType, String transactionType, String sourceSystemId);

    /**
     * Get all template-source mappings for a specific template
     * 
     * @param fileType Template file type
     * @param transactionType Template transaction type
     * @return List of all source systems configured for this template
     */
    List<TemplateSourceMappingResponse> getTemplateSourceMappings(String fileType, String transactionType);

    /**
     * Update existing template-source mappings
     * 
     * @param request Updated mapping request
     * @return Updated mapping response
     */
    TemplateSourceMappingResponse updateTemplateSourceMapping(TemplateSourceMappingRequest request);

    /**
     * Delete (disable) template-source mappings
     * 
     * @param fileType Template file type
     * @param transactionType Template transaction type
     * @param sourceSystemId Source system ID
     */
    void deleteTemplateSourceMapping(String fileType, String transactionType, String sourceSystemId);

    /**
     * Get template usage statistics for a source system
     * 
     * @param sourceSystemId Source system ID
     * @return List of templates configured for this source system
     */
    List<TemplateSourceMappingResponse> getUsageBySourceSystem(String sourceSystemId);

    /**
     * Check if template-source mapping exists
     * 
     * @param fileType Template file type
     * @param transactionType Template transaction type
     * @param sourceSystemId Source system ID
     * @return true if mapping exists, false otherwise
     */
    boolean existsTemplateSourceMapping(String fileType, String transactionType, String sourceSystemId);

    /**
     * Get mapping count for a template-source combination
     * 
     * @param fileType Template file type
     * @param transactionType Template transaction type
     * @param sourceSystemId Source system ID
     * @return Number of field mappings
     */
    int getMappingCount(String fileType, String transactionType, String sourceSystemId);
}