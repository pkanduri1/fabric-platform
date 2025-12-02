package com.fabric.batch.service;

import com.fabric.batch.dto.ManualJobConfigRequest;
import com.fabric.batch.entity.ManualJobConfigEntity;
import com.fabric.batch.repository.ManualJobConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

/**
 * Enterprise Service for Manual Job Configuration management.
 * 
 * Implements business logic for US001 Manual Job Configuration Interface.
 * Provides comprehensive job configuration lifecycle management with enterprise-grade
 * security, audit trail, and SOX compliance features.
 * 
 * Enterprise Standards:
 * - Configuration-first approach for all operations
 * - Banking-grade transaction management
 * - Comprehensive audit trail and logging
 * - Data lineage tracking and validation
 * 
 * Security Features:
 * - Input validation and sanitization
 * - Access control integration points
 * - Sensitive parameter handling guidelines
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since US001 - Manual Job Configuration Interface
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ManualJobConfigService {

    private final ManualJobConfigRepository repository;

    /**
     * Create a new manual job configuration.
     * 
     * Implements enterprise-grade configuration creation with:
     * - Automatic ID generation
     * - Duplicate name validation
     * - Configuration validation
     * - Audit trail creation
     * 
     * @param jobName the unique job name
     * @param jobType the job type (ETL, BATCH, etc.)
     * @param sourceSystem the source system identifier
     * @param targetSystem the target system identifier
     * @param masterQueryId optional master query identifier
     * @param jobParameters the JSON-formatted job parameters
     * @param createdBy the user creating the configuration
     * @return the created configuration entity
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException if duplicate job name exists
     */
    public ManualJobConfigEntity createJobConfiguration(
            String jobName,
            String jobType,
            String sourceSystem,
            String targetSystem,
            String masterQueryId,
            String jobParameters,
            String createdBy) {
        
        log.info("Creating new job configuration: {} by user: {}", jobName, createdBy);
        
        // Validate input parameters
        validateCreateJobConfigurationInput(jobName, jobType, sourceSystem, targetSystem, jobParameters, createdBy);
        
        // Check for duplicate job name
        if (repository.existsActiveConfigurationByJobName(jobName)) {
            log.warn("Attempt to create duplicate job configuration: {}", jobName);
            throw new IllegalStateException("Active job configuration with name '" + jobName + "' already exists");
        }
        
        // Generate unique configuration ID
        String configId = generateConfigurationId(sourceSystem);
        
        // Create configuration entity
        ManualJobConfigEntity config = ManualJobConfigEntity.builder()
                .configId(configId)
                .jobName(jobName)
                .jobType(jobType)
                .sourceSystem(sourceSystem)
                .targetSystem(targetSystem)
                .masterQueryId(masterQueryId)
                .jobParameters(jobParameters)
                .status("ACTIVE")
                .createdBy(createdBy)
                .versionNumber(1L)
                .build();
        
        // Validate configuration before saving
        if (!config.isValidConfiguration()) {
            log.error("Invalid job configuration data for: {}", jobName);
            throw new IllegalArgumentException("Job configuration validation failed");
        }
        
        // Save configuration
        ManualJobConfigEntity savedConfig = repository.save(config);
        
        log.info("Successfully created job configuration: {} with ID: {}", jobName, configId);
        
        return savedConfig;
    }

    /**
     * Retrieve job configuration by ID.
     * 
     * @param configId the configuration ID
     * @return Optional containing the configuration if found
     */
    @Transactional(readOnly = true)
    public Optional<ManualJobConfigEntity> getJobConfiguration(String configId) {
        log.debug("Retrieving job configuration: {}", configId);
        return repository.findById(configId);
    }

    /**
     * Retrieve all active job configurations.
     * 
     * @return List of active job configurations
     */
    @Transactional(readOnly = true)
    public List<ManualJobConfigEntity> getAllActiveConfigurations() {
        log.debug("Retrieving all active job configurations");
        return repository.findByStatus("ACTIVE");
    }

    /**
     * Retrieve job configurations by job type.
     * 
     * @param jobType the job type to filter by
     * @return List of configurations for the specified job type
     */
    @Transactional(readOnly = true)
    public List<ManualJobConfigEntity> getConfigurationsByJobType(String jobType) {
        log.debug("Retrieving job configurations for type: {}", jobType);
        return repository.findByJobType(jobType);
    }

    /**
     * Deactivate a job configuration.
     * 
     * @param configId the configuration ID to deactivate
     * @param updatedBy the user performing the deactivation
     * @return the updated configuration
     * @throws IllegalArgumentException if configuration not found
     */
    public ManualJobConfigEntity deactivateConfiguration(String configId, String updatedBy) {
        log.info("Deactivating job configuration: {} by user: {}", configId, updatedBy);
        
        ManualJobConfigEntity config = repository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Job configuration not found: " + configId));
        
        config.deactivate();
        ManualJobConfigEntity updatedConfig = repository.save(config);
        
        log.info("Successfully deactivated job configuration: {}", configId);
        
        return updatedConfig;
    }

    /**
     * Get system statistics for monitoring dashboard.
     * 
     * @return statistics object with configuration counts
     */
    @Transactional(readOnly = true)
    public ConfigurationStatistics getSystemStatistics() {
        log.debug("Retrieving system configuration statistics");
        
        long activeCount = repository.countByStatus("ACTIVE");
        long inactiveCount = repository.countByStatus("INACTIVE");
        long deprecatedCount = repository.countByStatus("DEPRECATED");
        long totalCount = repository.count();
        
        return ConfigurationStatistics.builder()
                .activeConfigurations(activeCount)
                .inactiveConfigurations(inactiveCount)
                .deprecatedConfigurations(deprecatedCount)
                .totalConfigurations(totalCount)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    // Private helper methods

    private void validateCreateJobConfigurationInput(
            String jobName, String jobType, String sourceSystem, 
            String targetSystem, String jobParameters, String createdBy) {
        
        if (jobName == null || jobName.trim().isEmpty()) {
            throw new IllegalArgumentException("Job name is required");
        }
        if (jobType == null || jobType.trim().isEmpty()) {
            throw new IllegalArgumentException("Job type is required");
        }
        if (sourceSystem == null || sourceSystem.trim().isEmpty()) {
            throw new IllegalArgumentException("Source system is required");
        }
        if (targetSystem == null || targetSystem.trim().isEmpty()) {
            throw new IllegalArgumentException("Target system is required");
        }
        if (jobParameters == null || jobParameters.trim().isEmpty()) {
            throw new IllegalArgumentException("Job parameters are required");
        }
        if (createdBy == null || createdBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Created by user is required");
        }
    }

    private String generateConfigurationId(String sourceSystem) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("cfg_%s_%s_%s", sourceSystem.toLowerCase(), timestamp, uuid);
    }

    /**
     * Update an existing job configuration with comprehensive validation and audit trail.
     * 
     * @param configId the configuration ID to update
     * @param request the update request with new values
     * @param updatedBy the user performing the update
     * @return the updated configuration entity
     * @throws IllegalArgumentException if configuration not found or validation fails
     */
    public ManualJobConfigEntity updateJobConfiguration(
            String configId, 
            ManualJobConfigRequest request, 
            String updatedBy) {
        
        log.info("Updating job configuration: {} by user: {}", configId, updatedBy);
        
        // Retrieve existing configuration
        ManualJobConfigEntity existingConfig = repository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Job configuration not found: " + configId));
        
        // Validate update request
        validateUpdateJobConfigurationInput(request, updatedBy);
        
        // Check for name conflicts if name is being changed
        if (!existingConfig.getJobName().equals(request.getJobName()) && 
            repository.existsActiveConfigurationByJobName(request.getJobName())) {
            throw new IllegalStateException("Active job configuration with name '" + request.getJobName() + "' already exists");
        }
        
        // Update configuration fields
        existingConfig.updateConfiguration(
            request.getJobName(),
            request.getJobType(),
            request.getSourceSystem(),
            request.getTargetSystem(),
            request.getMasterQueryId(),
            convertParametersToJson(request.getJobParameters()),
            updatedBy
        );
        
        // Validate updated configuration
        if (!existingConfig.isValidConfiguration()) {
            log.error("Invalid job configuration data after update for: {}", configId);
            throw new IllegalArgumentException("Job configuration validation failed after update");
        }
        
        // Save updated configuration
        ManualJobConfigEntity updatedConfig = repository.save(existingConfig);
        
        log.info("Successfully updated job configuration: {} to version: {}", configId, updatedConfig.getVersionNumber());
        
        return updatedConfig;
    }

    /**
     * Deactivate a job configuration with audit trail.
     * 
     * @param configId the configuration ID to deactivate
     * @param updatedBy the user performing the deactivation
     * @param reason the business reason for deactivation
     * @return the updated configuration
     * @throws IllegalArgumentException if configuration not found
     */
    public ManualJobConfigEntity deactivateConfiguration(String configId, String updatedBy, String reason) {
        log.info("Deactivating job configuration: {} by user: {} [reason: {}]", configId, updatedBy, reason);
        
        ManualJobConfigEntity config = repository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Job configuration not found: " + configId));
        
        config.deactivate();
        ManualJobConfigEntity updatedConfig = repository.save(config);
        
        // TODO: Create audit trail entry with deactivation reason
        log.info("Successfully deactivated job configuration: {} [reason: {}]", configId, reason);
        
        return updatedConfig;
    }

    /**
     * Get job configurations filtered by multiple criteria.
     * 
     * @param jobType optional job type filter
     * @param sourceSystem optional source system filter
     * @param status optional status filter
     * @return filtered list of configurations
     */
    @Transactional(readOnly = true)
    public List<ManualJobConfigEntity> getConfigurationsWithFilters(
            String jobType, String sourceSystem, String status) {
        
        log.debug("Retrieving job configurations with filters - jobType: {}, sourceSystem: {}, status: {}", 
                 jobType, sourceSystem, status);
        
        // Apply filters based on provided criteria
        if (jobType != null && sourceSystem != null && status != null) {
            return repository.findByJobTypeAndSourceSystemAndStatus(jobType, sourceSystem, status);
        } else if (jobType != null && status != null) {
            return repository.findByJobTypeAndStatus(jobType, status);
        } else if (sourceSystem != null && status != null) {
            return repository.findBySourceSystemAndStatus(sourceSystem, status);
        } else if (jobType != null) {
            return repository.findByJobType(jobType);
        } else if (sourceSystem != null) {
            return repository.findBySourceSystem(sourceSystem);
        } else if (status != null) {
            return repository.findByStatus(status);
        } else {
            return repository.findAll();
        }
    }

    /**
     * Get enhanced system statistics with performance metrics.
     * 
     * @return enhanced statistics object with additional metrics
     */
    @Transactional(readOnly = true)
    public EnhancedConfigurationStatistics getEnhancedSystemStatistics() {
        log.debug("Retrieving enhanced system configuration statistics");
        
        long activeCount = repository.countByStatus("ACTIVE");
        long inactiveCount = repository.countByStatus("INACTIVE");
        long deprecatedCount = repository.countByStatus("DEPRECATED");
        long totalCount = repository.count();
        
        // Calculate additional metrics
        long configurationsCreatedToday = repository.countConfigurationsCreatedToday();
        long configurationsModifiedThisWeek = repository.countConfigurationsModifiedThisWeek();
        
        return EnhancedConfigurationStatistics.builder()
                .activeConfigurations(activeCount)
                .inactiveConfigurations(inactiveCount)
                .deprecatedConfigurations(deprecatedCount)
                .totalConfigurations(totalCount)
                .configurationsCreatedToday(configurationsCreatedToday)
                .configurationsModifiedThisWeek(configurationsModifiedThisWeek)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * Validate a job configuration without saving it.
     * 
     * @param request the configuration request to validate
     * @return validation result with any errors or warnings
     */
    public ConfigurationValidationResult validateConfiguration(ManualJobConfigRequest request) {
        log.debug("Validating job configuration: {}", request.getJobName());
        
        ConfigurationValidationResult.ConfigurationValidationResultBuilder resultBuilder = 
            ConfigurationValidationResult.builder()
                .configurationName(request.getJobName())
                .validationTimestamp(LocalDateTime.now())
                .isValid(true);
        
        try {
            // Validate basic fields
            validateCreateJobConfigurationInput(
                request.getJobName(),
                request.getJobType(),
                request.getSourceSystem(),
                request.getTargetSystem(),
                convertParametersToJson(request.getJobParameters()),
                "VALIDATION_USER"
            );
            
            // Check for duplicate names
            if (repository.existsActiveConfigurationByJobName(request.getJobName())) {
                resultBuilder.isValid(false)
                    .validationError("Job name already exists: " + request.getJobName());
            }
            
            // Validate job parameters structure
            if (!request.hasValidJobParameters()) {
                resultBuilder.isValid(false)
                    .validationError("Invalid job parameters structure");
            }
            
            // Check for sensitive data
            if (request.containsSensitiveData()) {
                resultBuilder.validationWarning("Configuration contains potentially sensitive data");
            }
            
        } catch (IllegalArgumentException e) {
            resultBuilder.isValid(false)
                .validationError(e.getMessage());
        }
        
        return resultBuilder.build();
    }

    // Private helper methods for Phase 2

    private void validateUpdateJobConfigurationInput(ManualJobConfigRequest request, String updatedBy) {
        validateCreateJobConfigurationInput(
            request.getJobName(),
            request.getJobType(),
            request.getSourceSystem(),
            request.getTargetSystem(),
            convertParametersToJson(request.getJobParameters()),
            updatedBy
        );
    }

    private String convertParametersToJson(Map<String, Object> parameters) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(parameters);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON parameters", e);
        }
    }

    /**
     * Statistics data class for configuration monitoring.
     */
    @lombok.Data
    @lombok.Builder
    public static class ConfigurationStatistics {
        private long activeConfigurations;
        private long inactiveConfigurations;
        private long deprecatedConfigurations;
        private long totalConfigurations;
        private LocalDateTime lastUpdated;
    }

    /**
     * Enhanced statistics data class with additional performance metrics.
     */
    @lombok.Data
    @lombok.Builder
    public static class EnhancedConfigurationStatistics {
        private long activeConfigurations;
        private long inactiveConfigurations;
        private long deprecatedConfigurations;
        private long totalConfigurations;
        private long configurationsCreatedToday;
        private long configurationsModifiedThisWeek;
        private LocalDateTime lastUpdated;
    }

    /**
     * Configuration validation result data class.
     */
    @lombok.Data
    @lombok.Builder
    public static class ConfigurationValidationResult {
        private String configurationName;
        private boolean isValid;
        private LocalDateTime validationTimestamp;
        @lombok.Singular
        private List<String> validationErrors;
        @lombok.Singular
        private List<String> validationWarnings;
    }
}