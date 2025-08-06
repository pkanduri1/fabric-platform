package com.truist.batch.controller;

import com.truist.batch.dto.CreateSourceSystemRequest;
import com.truist.batch.dto.SourceSystemInfo;
import com.truist.batch.dto.SourceSystemWithUsage;
import com.truist.batch.service.SourceSystemService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Source System management in US002 Template Configuration Enhancement
 * Provides endpoints for:
 * - Getting all source systems for dropdown
 * - Creating new source systems
 * - Getting source systems with usage information for templates
 */
@RestController
@RequestMapping("/api/admin/source-systems")
@CrossOrigin(origins = {"http://localhost:3000", "https://localhost:3000"})
public class SourceSystemController {

    private static final Logger logger = LoggerFactory.getLogger(SourceSystemController.class);

    @Autowired
    private SourceSystemService sourceSystemService;

    /**
     * Get all available source systems for dropdown in Template Configuration page
     * 
     * @return List of all enabled source systems
     */
    @GetMapping
    public ResponseEntity<List<SourceSystemInfo>> getAllSourceSystems() {
        try {
            logger.info("Fetching all source systems for dropdown");
            List<SourceSystemInfo> sourceSystems = sourceSystemService.getAllEnabledSourceSystems();
            logger.info("Found {} enabled source systems", sourceSystems.size());
            return ResponseEntity.ok(sourceSystems);
        } catch (Exception e) {
            logger.error("Error fetching source systems", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new source system
     * 
     * @param request The source system creation request
     * @return The created source system info
     */
    @PostMapping
    public ResponseEntity<SourceSystemInfo> createSourceSystem(@Valid @RequestBody CreateSourceSystemRequest request) {
        try {
            logger.info("Creating new source system with ID: {}", request.getId());
            
            // Check if source system already exists
            if (sourceSystemService.existsById(request.getId())) {
                logger.warn("Source system with ID {} already exists", request.getId());
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            SourceSystemInfo createdSystem = sourceSystemService.createSourceSystem(request);
            
            if (createdSystem == null) {
                logger.error("Service returned null when creating source system: {}", request.getId());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            logger.info("Successfully created source system: {}", createdSystem.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSystem);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for creating source system: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error creating source system", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get source systems with usage information for a specific template
     * 
     * @param fileType The template file type
     * @param transactionType The template transaction type
     * @return List of source systems with usage information
     */
    @GetMapping("/with-usage/{fileType}/{transactionType}")
    public ResponseEntity<List<SourceSystemWithUsage>> getSourceSystemsWithUsage(
            @PathVariable String fileType,
            @PathVariable String transactionType) {
        try {
            logger.info("Fetching source systems with usage for template: {}/{}", fileType, transactionType);
            List<SourceSystemWithUsage> systemsWithUsage = sourceSystemService.getSourceSystemsWithUsageForTemplate(fileType, transactionType);
            logger.info("Found {} source systems with usage information", systemsWithUsage.size());
            return ResponseEntity.ok(systemsWithUsage);
        } catch (Exception e) {
            logger.error("Error fetching source systems with usage for template {}/{}", fileType, transactionType, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a specific source system by ID
     * 
     * @param id The source system ID
     * @return The source system info
     */
    @GetMapping("/{id}")
    public ResponseEntity<SourceSystemInfo> getSourceSystemById(@PathVariable String id) {
        try {
            logger.info("Fetching source system by ID: {}", id);
            SourceSystemInfo sourceSystem = sourceSystemService.getSourceSystemById(id);
            if (sourceSystem != null) {
                return ResponseEntity.ok(sourceSystem);
            } else {
                logger.warn("Source system not found: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error fetching source system by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update a source system
     * 
     * @param id The source system ID
     * @param request The update request
     * @return The updated source system info
     */
    @PutMapping("/{id}")
    public ResponseEntity<SourceSystemInfo> updateSourceSystem(
            @PathVariable String id,
            @Valid @RequestBody CreateSourceSystemRequest request) {
        try {
            logger.info("Updating source system: {}", id);
            
            if (!sourceSystemService.existsById(id)) {
                logger.warn("Source system not found for update: {}", id);
                return ResponseEntity.notFound().build();
            }

            SourceSystemInfo updatedSystem = sourceSystemService.updateSourceSystem(id, request);
            logger.info("Successfully updated source system: {}", id);
            return ResponseEntity.ok(updatedSystem);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for updating source system {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error updating source system: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete (disable) a source system
     * 
     * @param id The source system ID
     * @return Success response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSourceSystem(@PathVariable String id) {
        try {
            logger.info("Disabling source system: {}", id);
            
            if (!sourceSystemService.existsById(id)) {
                logger.warn("Source system not found for deletion: {}", id);
                return ResponseEntity.notFound().build();
            }

            sourceSystemService.disableSourceSystem(id);
            logger.info("Successfully disabled source system: {}", id);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            logger.error("Error disabling source system: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Source System Controller is healthy");
    }
}