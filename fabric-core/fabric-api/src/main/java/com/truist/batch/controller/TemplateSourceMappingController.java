package com.truist.batch.controller;

import com.truist.batch.dto.TemplateSourceMappingRequest;
import com.truist.batch.dto.TemplateSourceMappingResponse;
import com.truist.batch.service.TemplateSourceMappingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Template-Source System field mappings in US002 Template Configuration Enhancement
 * Provides endpoints for:
 * - Saving template-source field mappings
 * - Retrieving existing mappings
 * - Managing template-source associations
 */
@RestController
@RequestMapping("/api/admin/template-source-mappings")
@CrossOrigin(origins = {"http://localhost:3000", "https://localhost:3000"})
public class TemplateSourceMappingController {

    private static final Logger logger = LoggerFactory.getLogger(TemplateSourceMappingController.class);

    @Autowired
    private TemplateSourceMappingService templateSourceMappingService;

    /**
     * Save template-source field mappings
     * 
     * @param request The template-source mapping request
     * @return Success response with mapping count
     */
    @PostMapping
    public ResponseEntity<TemplateSourceMappingResponse> saveTemplateSourceMapping(
            @Valid @RequestBody TemplateSourceMappingRequest request) {
        try {
            logger.info("Saving template-source mappings for {}/{} -> {}", 
                    request.getFileType(), request.getTransactionType(), request.getSourceSystemId());
            
            // DEBUG: Log detailed field mapping data to trace transformation type persistence
            if (request.getFieldMappings() != null) {
                logger.info("DEBUG: Received {} field mappings from frontend:", request.getFieldMappings().size());
                for (int i = 0; i < request.getFieldMappings().size(); i++) {
                    var mapping = request.getFieldMappings().get(i);
                    logger.info("DEBUG: Field[{}]: target={}, source={}, transformationType={}, transformationConfig={}", 
                            i, mapping.getTargetFieldName(), mapping.getSourceFieldName(), 
                            mapping.getTransformationType(), mapping.getTransformationConfig());
                }
            }
            
            TemplateSourceMappingResponse response = templateSourceMappingService.saveTemplateSourceMapping(request);
            
            logger.info("Successfully saved {} field mappings for job: {}", 
                    response.getMappingCount(), request.getJobName());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for saving template-source mapping: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error saving template-source mapping", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get existing template-source mappings for a specific combination
     * 
     * @param fileType The template file type
     * @param transactionType The template transaction type  
     * @param sourceSystemId The source system ID
     * @return List of existing field mappings
     */
    @GetMapping("/{fileType}/{transactionType}/{sourceSystemId}")
    public ResponseEntity<TemplateSourceMappingResponse> getTemplateSourceMapping(
            @PathVariable String fileType,
            @PathVariable String transactionType,
            @PathVariable String sourceSystemId) {
        try {
            logger.info("Fetching template-source mappings for {}/{} -> {}", 
                    fileType, transactionType, sourceSystemId);
            
            TemplateSourceMappingResponse response = templateSourceMappingService
                    .getTemplateSourceMapping(fileType, transactionType, sourceSystemId);
            
            if (response != null && response.getFieldMappings() != null && !response.getFieldMappings().isEmpty()) {
                logger.info("Found {} existing field mappings", response.getFieldMappings().size());
                return ResponseEntity.ok(response);
            } else {
                logger.info("No existing mappings found for {}/{} -> {}", 
                        fileType, transactionType, sourceSystemId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error fetching template-source mapping for {}/{} -> {}", 
                    fileType, transactionType, sourceSystemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all template-source mappings for a specific template
     * 
     * @param fileType The template file type
     * @param transactionType The template transaction type
     * @return List of all source systems configured for this template
     */
    @GetMapping("/{fileType}/{transactionType}")
    public ResponseEntity<List<TemplateSourceMappingResponse>> getTemplateSourceMappings(
            @PathVariable String fileType,
            @PathVariable String transactionType) {
        try {
            logger.info("Fetching all template-source mappings for template: {}/{}", fileType, transactionType);
            
            List<TemplateSourceMappingResponse> responses = templateSourceMappingService
                    .getTemplateSourceMappings(fileType, transactionType);
            
            logger.info("Found {} source systems configured for template {}/{}", 
                    responses.size(), fileType, transactionType);
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            logger.error("Error fetching template-source mappings for template {}/{}", 
                    fileType, transactionType, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update existing template-source mappings
     * 
     * @param fileType The template file type
     * @param transactionType The template transaction type
     * @param sourceSystemId The source system ID
     * @param request The updated mapping request
     * @return Updated mapping response
     */
    @PutMapping("/{fileType}/{transactionType}/{sourceSystemId}")
    public ResponseEntity<TemplateSourceMappingResponse> updateTemplateSourceMapping(
            @PathVariable String fileType,
            @PathVariable String transactionType,
            @PathVariable String sourceSystemId,
            @Valid @RequestBody TemplateSourceMappingRequest request) {
        try {
            logger.info("Updating template-source mappings for {}/{} -> {}", 
                    fileType, transactionType, sourceSystemId);
            
            // Ensure path variables match request body
            if (!fileType.equals(request.getFileType()) || 
                !transactionType.equals(request.getTransactionType()) ||
                !sourceSystemId.equals(request.getSourceSystemId())) {
                logger.warn("Path variables don't match request body");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            TemplateSourceMappingResponse response = templateSourceMappingService
                    .updateTemplateSourceMapping(request);
            
            logger.info("Successfully updated {} field mappings", response.getMappingCount());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for updating template-source mapping: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error updating template-source mapping for {}/{} -> {}", 
                    fileType, transactionType, sourceSystemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete (disable) template-source mappings
     * 
     * @param fileType The template file type
     * @param transactionType The template transaction type
     * @param sourceSystemId The source system ID
     * @return Success response
     */
    @DeleteMapping("/{fileType}/{transactionType}/{sourceSystemId}")
    public ResponseEntity<Void> deleteTemplateSourceMapping(
            @PathVariable String fileType,
            @PathVariable String transactionType,
            @PathVariable String sourceSystemId) {
        try {
            logger.info("Deleting template-source mappings for {}/{} -> {}", 
                    fileType, transactionType, sourceSystemId);
            
            templateSourceMappingService.deleteTemplateSourceMapping(fileType, transactionType, sourceSystemId);
            
            logger.info("Successfully deleted template-source mappings for {}/{} -> {}", 
                    fileType, transactionType, sourceSystemId);
            
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            logger.error("Error deleting template-source mapping for {}/{} -> {}", 
                    fileType, transactionType, sourceSystemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get template usage statistics for a source system
     * 
     * @param sourceSystemId The source system ID
     * @return Usage statistics
     */
    @GetMapping("/usage/{sourceSystemId}")
    public ResponseEntity<List<TemplateSourceMappingResponse>> getUsageBySourceSystem(
            @PathVariable String sourceSystemId) {
        try {
            logger.info("Fetching template usage for source system: {}", sourceSystemId);
            
            List<TemplateSourceMappingResponse> responses = templateSourceMappingService
                    .getUsageBySourceSystem(sourceSystemId);
            
            logger.info("Found {} templates configured for source system {}", 
                    responses.size(), sourceSystemId);
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            logger.error("Error fetching usage for source system: {}", sourceSystemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Template Source Mapping Controller is healthy");
    }
}