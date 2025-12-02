package com.fabric.batch.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fabric.batch.dto.*;
import com.fabric.batch.dto.SqlLoaderReports.*;

/**
 * Service interface for comprehensive SQL*Loader configuration management.
 * Extends the established ConfigurationService patterns to provide enterprise-grade
 * SQL*Loader configuration capabilities with security, compliance, and audit features.
 * 
 * This service provides:
 * - Configuration CRUD operations with validation
 * - Control file generation and management
 * - Security and compliance enforcement
 * - Performance optimization recommendations
 * - Comprehensive audit trail
 * - Integration with existing batch framework
 */
public interface SqlLoaderConfigService {

    // ==================== CONFIGURATION MANAGEMENT ====================
    
    /**
     * Create a new SQL*Loader configuration with comprehensive validation.
     * 
     * @param request Configuration creation request
     * @return Created configuration with generated control file template
     */
    SqlLoaderConfigResponse createConfiguration(SqlLoaderConfigRequest request);
    
    /**
     * Update existing SQL*Loader configuration with versioning and audit trail.
     * 
     * @param configId Configuration ID to update
     * @param request Updated configuration details
     * @return Updated configuration with new version
     */
    SqlLoaderConfigResponse updateConfiguration(String configId, SqlLoaderConfigRequest request);
    
    /**
     * Retrieve SQL*Loader configuration by ID with all related data.
     * 
     * @param configId Configuration ID
     * @return Complete configuration details
     */
    Optional<SqlLoaderConfigResponse> getConfiguration(String configId);
    
    /**
     * Retrieve configuration by source system and job name.
     * 
     * @param sourceSystem Source system identifier
     * @param jobName Job name
     * @return Configuration if found
     */
    Optional<SqlLoaderConfigResponse> getConfigurationBySourceAndJob(String sourceSystem, String jobName);
    
    /**
     * Get all configurations with pagination and filtering.
     * 
     * @param pageable Pagination parameters
     * @param filters Optional filters (sourceSystem, dataClassification, etc.)
     * @return Paginated configuration list
     */
    Page<SqlLoaderConfigResponse> getAllConfigurations(Pageable pageable, Map<String, Object> filters);
    
    /**
     * Get configurations by source system.
     * 
     * @param sourceSystem Source system identifier
     * @return List of configurations for the source system
     */
    List<SqlLoaderConfigResponse> getConfigurationsBySourceSystem(String sourceSystem);
    
    /**
     * Delete (disable) configuration with audit trail.
     * 
     * @param configId Configuration ID to delete
     * @param deletedBy User performing the deletion
     * @param reason Reason for deletion
     */
    void deleteConfiguration(String configId, String deletedBy, String reason);

    // ==================== CONTROL FILE GENERATION ====================
    
    /**
     * Generate SQL*Loader control file from configuration.
     * 
     * @param configId Configuration ID
     * @param dataFilePath Path to data file for loading
     * @return Generated control file content and metadata
     */
    ControlFileResponse generateControlFile(String configId, String dataFilePath);
    
    /**
     * Generate control file template for new configurations.
     * 
     * @param targetTable Target table name
     * @param fileType File type (DELIMITED, FIXED_WIDTH)
     * @return Control file template
     */
    String generateControlFileTemplate(String targetTable, String fileType);
    
    /**
     * Validate control file syntax and completeness.
     * 
     * @param controlFileContent Control file content to validate
     * @return Validation results with errors and warnings
     */
    ControlFileValidationResult validateControlFile(String controlFileContent);
    
    /**
     * Get control file generation history for a configuration.
     * 
     * @param configId Configuration ID
     * @return List of generated control files with metadata
     */
    List<ControlFileHistory> getControlFileHistory(String configId);

    // ==================== VALIDATION AND COMPLIANCE ====================
    
    /**
     * Validate configuration for completeness and compliance.
     * 
     * @param request Configuration to validate
     * @return Validation results with errors, warnings, and recommendations
     */
    ConfigurationValidationResult validateConfiguration(SqlLoaderConfigRequest request);
    
    /**
     * Get compliance status and recommendations for configuration.
     * 
     * @param configId Configuration ID
     * @return Compliance report with issues and recommendations
     */
    ComplianceReport getComplianceReport(String configId);
    
    /**
     * Get performance analysis and optimization recommendations.
     * 
     * @param configId Configuration ID
     * @return Performance report with optimization suggestions
     */
    PerformanceReport getPerformanceReport(String configId);
    
    /**
     * Check configuration security requirements and PII handling.
     * 
     * @param configId Configuration ID
     * @return Security assessment with risk level and recommendations
     */
    SecurityAssessment getSecurityAssessment(String configId);

    // ==================== EXECUTION MANAGEMENT ====================
    
    /**
     * Prepare configuration for SQL*Loader execution.
     * 
     * @param configId Configuration ID
     * @param executionParameters Runtime parameters for execution
     * @return Execution-ready configuration with correlation ID
     */
    ExecutionConfig prepareForExecution(String configId, Map<String, Object> executionParameters);
    
    /**
     * Get execution history for a configuration.
     * 
     * @param configId Configuration ID
     * @param pageable Pagination parameters
     * @return Paginated execution history
     */
    Page<ExecutionHistory> getExecutionHistory(String configId, Pageable pageable);
    
    /**
     * Get execution statistics and metrics.
     * 
     * @param configId Configuration ID
     * @param fromDate Start date for statistics
     * @param toDate End date for statistics
     * @return Execution statistics with performance metrics
     */
    ExecutionStatistics getExecutionStatistics(String configId, LocalDateTime fromDate, LocalDateTime toDate);

    // ==================== FIELD CONFIGURATION MANAGEMENT ====================
    
    /**
     * Add field configuration to existing SQL*Loader config.
     * 
     * @param configId Parent configuration ID
     * @param fieldConfig Field configuration to add
     * @return Added field configuration details
     */
    FieldConfigResponse addFieldConfiguration(String configId, FieldConfigRequest fieldConfig);
    
    /**
     * Update field configuration.
     * 
     * @param configId Parent configuration ID
     * @param fieldId Field configuration ID
     * @param fieldConfig Updated field configuration
     * @return Updated field configuration details
     */
    FieldConfigResponse updateFieldConfiguration(String configId, String fieldId, FieldConfigRequest fieldConfig);
    
    /**
     * Remove field configuration.
     * 
     * @param configId Parent configuration ID
     * @param fieldId Field configuration ID to remove
     */
    void removeFieldConfiguration(String configId, String fieldId);
    
    /**
     * Get all field configurations for a SQL*Loader config.
     * 
     * @param configId Parent configuration ID
     * @return List of field configurations ordered by field order
     */
    List<FieldConfigResponse> getFieldConfigurations(String configId);
    
    /**
     * Reorder field configurations.
     * 
     * @param configId Parent configuration ID
     * @param fieldOrders Map of field ID to new order position
     */
    void reorderFieldConfigurations(String configId, Map<String, Integer> fieldOrders);

    // ==================== REPORTING AND ANALYTICS ====================
    
    /**
     * Get configuration usage statistics across all source systems.
     * 
     * @return Configuration usage summary with metrics
     */
    ConfigurationUsageReport getUsageReport();
    
    /**
     * Get configurations requiring attention (compliance issues, performance problems).
     * 
     * @return List of configurations with attention flags and reasons
     */
    List<ConfigurationAlert> getConfigurationsRequiringAttention();
    
    /**
     * Generate configuration audit trail report.
     * 
     * @param configId Configuration ID (optional - null for all)
     * @param fromDate Start date for audit trail
     * @param toDate End date for audit trail
     * @return Audit trail with all configuration changes
     */
    AuditTrailReport getAuditTrailReport(String configId, LocalDateTime fromDate, LocalDateTime toDate);
    
    /**
     * Get data classification summary across all configurations.
     * 
     * @return Data classification metrics and compliance status
     */
    DataClassificationReport getDataClassificationReport();

    // ==================== CONFIGURATION TEMPLATES ====================
    
    /**
     * Create configuration template for reuse.
     * 
     * @param configId Source configuration ID
     * @param templateName Template name
     * @param description Template description
     * @return Created template details
     */
    ConfigurationTemplate createTemplate(String configId, String templateName, String description);
    
    /**
     * Create configuration from template.
     * 
     * @param templateId Template ID
     * @param sourceSystem Target source system
     * @param jobName Target job name
     * @param customizations Optional customizations to apply
     * @return Created configuration based on template
     */
    SqlLoaderConfigResponse createFromTemplate(String templateId, String sourceSystem, String jobName, 
                                             Map<String, Object> customizations);
    
    /**
     * Get available configuration templates.
     * 
     * @return List of available templates with metadata
     */
    List<ConfigurationTemplate> getAvailableTemplates();

    // ==================== BULK OPERATIONS ====================
    
    /**
     * Import configurations from external source (CSV, JSON, etc.).
     * 
     * @param importData Configuration data to import
     * @param importFormat Format of the import data
     * @param validationMode Validation mode (STRICT, LENIENT)
     * @return Import results with success/failure details
     */
    BulkImportResult importConfigurations(String importData, String importFormat, String validationMode);
    
    /**
     * Export configurations to specified format.
     * 
     * @param configIds List of configuration IDs to export (null for all)
     * @param exportFormat Export format (JSON, CSV, XML)
     * @param includeFieldConfigs Whether to include field configurations
     * @return Exported configuration data
     */
    String exportConfigurations(List<String> configIds, String exportFormat, boolean includeFieldConfigs);
    
    /**
     * Clone configuration with optional modifications.
     * 
     * @param sourceConfigId Source configuration ID to clone
     * @param newSourceSystem New source system for cloned config
     * @param newJobName New job name for cloned config
     * @param modifications Optional modifications to apply during cloning
     * @return Cloned configuration details
     */
    SqlLoaderConfigResponse cloneConfiguration(String sourceConfigId, String newSourceSystem, String newJobName,
                                             Map<String, Object> modifications);

    // ==================== INTEGRATION METHODS ====================
    
    /**
     * Test configuration connectivity and permissions.
     * 
     * @param configId Configuration ID to test
     * @return Test results with connection status and permission checks
     */
    ConfigurationTestResult testConfiguration(String configId);
    
    /**
     * Get compatible configurations for data migration scenarios.
     * 
     * @param sourceConfig Source configuration for compatibility check
     * @return List of compatible target configurations
     */
    List<CompatibilityResult> getCompatibleConfigurations(String sourceConfig);
    
    /**
     * Generate data mapping recommendations between configurations.
     * 
     * @param sourceConfigId Source configuration ID
     * @param targetConfigId Target configuration ID
     * @return Mapping recommendations with transformation suggestions
     */
    MappingRecommendations getDataMappingRecommendations(String sourceConfigId, String targetConfigId);
}