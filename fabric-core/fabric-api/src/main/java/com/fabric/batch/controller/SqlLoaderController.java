package com.fabric.batch.controller;

import com.fabric.batch.dto.*;
import com.fabric.batch.dto.SqlLoaderReports.*;
import com.fabric.batch.service.SqlLoaderConfigService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Comprehensive REST controller for SQL*Loader configuration management.
 * Follows established patterns from ConfigurationController with enhanced
 * security, compliance, and audit capabilities.
 * 
 * Provides complete CRUD operations, control file generation, compliance
 * reporting, performance analysis, and comprehensive audit trail.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sql-loader")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
@Tag(name = "SQL*Loader Configuration Management", 
     description = "Comprehensive APIs for managing SQL*Loader configurations with enterprise security and compliance features")
public class SqlLoaderController {

    private final SqlLoaderConfigService sqlLoaderConfigService;

    // ==================== CONFIGURATION MANAGEMENT ENDPOINTS ====================

    @PostMapping("/configurations")
    @Operation(summary = "Create SQL*Loader configuration", 
               description = "Creates a new SQL*Loader configuration with comprehensive validation and audit trail")
    @ApiResponse(responseCode = "201", description = "Configuration created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid configuration data")
    @ApiResponse(responseCode = "409", description = "Configuration already exists")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CONFIG_MANAGER')")
    public ResponseEntity<?> createConfiguration(
            @Valid @RequestBody SqlLoaderConfigRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("🚀 API Request: Create SQL*Loader configuration for job: {}, source: {}", 
                request.getJobName(), request.getSourceSystem());
        
        try {
            // Create configuration
            SqlLoaderConfigResponse response = sqlLoaderConfigService.createConfiguration(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "SQL*Loader configuration created successfully");
            result.put("configuration", response);
            result.put("configId", response.getConfigId());
            result.put("timestamp", LocalDateTime.now().toString());
            
            log.info("✅ Successfully created SQL*Loader configuration: {}", response.getConfigId());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid configuration data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(
                    "Invalid configuration data", e.getMessage(), httpRequest.getRequestURI()));
                    
        } catch (Exception e) {
            log.error("❌ Failed to create SQL*Loader configuration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to create configuration", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @GetMapping("/configurations/{configId}")
    @Operation(summary = "Get SQL*Loader configuration", 
               description = "Retrieves complete configuration details with analysis and recommendations")
    @ApiResponse(responseCode = "200", description = "Configuration retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Configuration not found")
    @PreAuthorize("hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<?> getConfiguration(
            @Parameter(description = "Configuration ID") @PathVariable @NotBlank String configId,
            HttpServletRequest httpRequest) {
        
        log.info("📋 API Request: Get SQL*Loader configuration: {}", configId);
        
        try {
            Optional<SqlLoaderConfigResponse> configuration = sqlLoaderConfigService.getConfiguration(configId);
            
            if (configuration.isEmpty()) {
                log.warn("⚠️ Configuration not found: {}", configId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(
                        "Configuration not found", "Configuration ID: " + configId, httpRequest.getRequestURI()));
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("configuration", configuration.get());
            result.put("retrievedAt", LocalDateTime.now().toString());
            
            log.info("✅ Retrieved SQL*Loader configuration: {}", configId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to retrieve configuration {}: {}", configId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to retrieve configuration", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @PutMapping("/configurations/{configId}")
    @Operation(summary = "Update SQL*Loader configuration", 
               description = "Updates existing configuration with versioning and audit trail")
    @ApiResponse(responseCode = "200", description = "Configuration updated successfully")
    @ApiResponse(responseCode = "404", description = "Configuration not found")
    @ApiResponse(responseCode = "400", description = "Invalid configuration data")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CONFIG_MANAGER')")
    public ResponseEntity<?> updateConfiguration(
            @Parameter(description = "Configuration ID") @PathVariable @NotBlank String configId,
            @Valid @RequestBody SqlLoaderConfigRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("🔄 API Request: Update SQL*Loader configuration: {}", configId);
        
        try {
            SqlLoaderConfigResponse response = sqlLoaderConfigService.updateConfiguration(configId, request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "SQL*Loader configuration updated successfully");
            result.put("configuration", response);
            result.put("configId", configId);
            result.put("newVersion", response.getVersion());
            result.put("timestamp", LocalDateTime.now().toString());
            
            log.info("✅ Successfully updated SQL*Loader configuration: {} to version {}", 
                    configId, response.getVersion());
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Configuration not found or invalid data: {}", e.getMessage());
            
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(
                        "Configuration not found", e.getMessage(), httpRequest.getRequestURI()));
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse(
                        "Invalid configuration data", e.getMessage(), httpRequest.getRequestURI()));
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to update configuration {}: {}", configId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to update configuration", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @DeleteMapping("/configurations/{configId}")
    @Operation(summary = "Delete SQL*Loader configuration", 
               description = "Soft deletes configuration with audit trail")
    @ApiResponse(responseCode = "200", description = "Configuration deleted successfully")
    @ApiResponse(responseCode = "404", description = "Configuration not found")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteConfiguration(
            @Parameter(description = "Configuration ID") @PathVariable @NotBlank String configId,
            @RequestParam(required = false) String reason,
            @RequestParam String deletedBy,
            HttpServletRequest httpRequest) {
        
        log.info("🗑️ API Request: Delete SQL*Loader configuration: {}", configId);
        
        try {
            sqlLoaderConfigService.deleteConfiguration(configId, deletedBy, reason);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "SQL*Loader configuration deleted successfully");
            result.put("configId", configId);
            result.put("deletedBy", deletedBy);
            result.put("reason", reason);
            result.put("timestamp", LocalDateTime.now().toString());
            
            log.info("✅ Successfully deleted SQL*Loader configuration: {}", configId);
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Configuration not found: {}", configId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(
                    "Configuration not found", e.getMessage(), httpRequest.getRequestURI()));
                    
        } catch (Exception e) {
            log.error("❌ Failed to delete configuration {}: {}", configId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to delete configuration", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @GetMapping("/configurations")
    @Operation(summary = "Get all SQL*Loader configurations", 
               description = "Retrieves paginated list of configurations with filtering options")
    @PreAuthorize("hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<?> getAllConfigurations(
            Pageable pageable,
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(required = false) String dataClassification,
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {
        
        log.info("📋 API Request: Get all SQL*Loader configurations with filters");
        
        try {
            Map<String, Object> filters = new HashMap<>();
            if (sourceSystem != null) filters.put("sourceSystem", sourceSystem);
            if (dataClassification != null) filters.put("dataClassification", dataClassification);
            if (status != null) filters.put("status", status);
            
            Page<SqlLoaderConfigResponse> configurations = 
                    sqlLoaderConfigService.getAllConfigurations(pageable, filters);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("configurations", configurations.getContent());
            result.put("totalElements", configurations.getTotalElements());
            result.put("totalPages", configurations.getTotalPages());
            result.put("currentPage", configurations.getNumber());
            result.put("pageSize", configurations.getSize());
            result.put("filters", filters);
            result.put("retrievedAt", LocalDateTime.now().toString());
            
            log.info("✅ Retrieved {} SQL*Loader configurations", configurations.getNumberOfElements());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to retrieve configurations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to retrieve configurations", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @GetMapping("/configurations/source-system/{sourceSystem}")
    @Operation(summary = "Get configurations by source system", 
               description = "Retrieves all configurations for a specific source system")
    @PreAuthorize("hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<?> getConfigurationsBySourceSystem(
            @Parameter(description = "Source System ID") @PathVariable @NotBlank String sourceSystem,
            HttpServletRequest httpRequest) {
        
        log.info("📋 API Request: Get SQL*Loader configurations for source system: {}", sourceSystem);
        
        try {
            List<SqlLoaderConfigResponse> configurations = 
                    sqlLoaderConfigService.getConfigurationsBySourceSystem(sourceSystem);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("sourceSystem", sourceSystem);
            result.put("configurations", configurations);
            result.put("count", configurations.size());
            result.put("retrievedAt", LocalDateTime.now().toString());
            
            log.info("✅ Retrieved {} configurations for source system: {}", configurations.size(), sourceSystem);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to retrieve configurations for source system {}: {}", sourceSystem, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to retrieve configurations", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    // ==================== CONTROL FILE GENERATION ENDPOINTS ====================

    @PostMapping("/configurations/{configId}/control-file")
    @Operation(summary = "Generate SQL*Loader control file", 
               description = "Generates optimized control file from configuration for specified data file")
    @ApiResponse(responseCode = "200", description = "Control file generated successfully")
    @ApiResponse(responseCode = "404", description = "Configuration not found")
    @PreAuthorize("hasRole('USER') or hasRole('EXECUTOR')")
    public ResponseEntity<?> generateControlFile(
            @Parameter(description = "Configuration ID") @PathVariable @NotBlank String configId,
            @RequestParam @NotBlank String dataFilePath,
            HttpServletRequest httpRequest) {
        
        log.info("⚙️ API Request: Generate control file for configuration: {}", configId);
        
        try {
            ControlFileResponse controlFile = sqlLoaderConfigService.generateControlFile(configId, dataFilePath);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Control file generated successfully");
            result.put("controlFile", controlFile);
            result.put("configId", configId);
            result.put("dataFilePath", dataFilePath);
            result.put("generatedAt", LocalDateTime.now().toString());
            
            log.info("✅ Generated control file for configuration: {}", configId);
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Configuration not found: {}", configId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(
                    "Configuration not found", e.getMessage(), httpRequest.getRequestURI()));
                    
        } catch (Exception e) {
            log.error("❌ Failed to generate control file for {}: {}", configId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to generate control file", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @PostMapping("/configurations/{configId}/control-file/download")
    @Operation(summary = "Download SQL*Loader control file", 
               description = "Generates and downloads control file as attachment")
    @PreAuthorize("hasRole('USER') or hasRole('EXECUTOR')")
    public ResponseEntity<?> downloadControlFile(
            @Parameter(description = "Configuration ID") @PathVariable @NotBlank String configId,
            @RequestParam @NotBlank String dataFilePath,
            HttpServletRequest httpRequest) {
        
        log.info("📥 API Request: Download control file for configuration: {}", configId);
        
        try {
            ControlFileResponse controlFile = sqlLoaderConfigService.generateControlFile(configId, dataFilePath);
            
            if (!controlFile.isReadyForExecution()) {
                log.warn("⚠️ Control file not ready for execution due to validation issues");
                return ResponseEntity.badRequest().body(createErrorResponse(
                        "Control file has validation issues", 
                        "Errors: " + controlFile.getValidationErrors(), 
                        httpRequest.getRequestURI()));
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", controlFile.getControlFileName());
            
            log.info("✅ Control file download prepared: {}", controlFile.getControlFileName());
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(controlFile.getControlFileContent());
                    
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Configuration not found: {}", configId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(
                    "Configuration not found", e.getMessage(), httpRequest.getRequestURI()));
                    
        } catch (Exception e) {
            log.error("❌ Failed to download control file for {}: {}", configId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to download control file", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @GetMapping("/control-file/template")
    @Operation(summary = "Generate control file template", 
               description = "Generates a basic control file template for specified table and file type")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> generateControlFileTemplate(
            @RequestParam @NotBlank String targetTable,
            @RequestParam @NotBlank String fileType,
            HttpServletRequest httpRequest) {
        
        log.info("📋 API Request: Generate control file template for table: {}, type: {}", targetTable, fileType);
        
        try {
            String template = sqlLoaderConfigService.generateControlFileTemplate(targetTable, fileType);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("template", template);
            result.put("targetTable", targetTable);
            result.put("fileType", fileType);
            result.put("generatedAt", LocalDateTime.now().toString());
            
            log.info("✅ Generated control file template for table: {}", targetTable);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to generate control file template: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to generate template", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @PostMapping("/control-file/validate")
    @Operation(summary = "Validate control file syntax", 
               description = "Validates control file content for syntax errors and provides recommendations")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> validateControlFile(
            @RequestBody String controlFileContent,
            HttpServletRequest httpRequest) {
        
        log.info("🔍 API Request: Validate control file content");
        
        try {
            ControlFileValidationResult validation = sqlLoaderConfigService.validateControlFile(controlFileContent);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("validation", validation);
            result.put("valid", validation.getValid());
            result.put("errorCount", validation.getErrors() != null ? validation.getErrors().size() : 0);
            result.put("warningCount", validation.getWarnings() != null ? validation.getWarnings().size() : 0);
            result.put("validatedAt", LocalDateTime.now().toString());
            
            log.info("✅ Control file validation completed: {}", validation.getValid() ? "VALID" : "INVALID");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to validate control file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to validate control file", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @GetMapping("/configurations/{configId}/control-file/history")
    @Operation(summary = "Get control file generation history", 
               description = "Retrieves history of control file generations for configuration")
    @PreAuthorize("hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<?> getControlFileHistory(
            @Parameter(description = "Configuration ID") @PathVariable @NotBlank String configId,
            HttpServletRequest httpRequest) {
        
        log.info("📋 API Request: Get control file history for configuration: {}", configId);
        
        try {
            List<ControlFileHistory> history = sqlLoaderConfigService.getControlFileHistory(configId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("configId", configId);
            result.put("history", history);
            result.put("count", history.size());
            result.put("retrievedAt", LocalDateTime.now().toString());
            
            log.info("✅ Retrieved {} control file history entries for configuration: {}", history.size(), configId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to retrieve control file history for {}: {}", configId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to retrieve history", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    // ==================== VALIDATION AND COMPLIANCE ENDPOINTS ====================

    @PostMapping("/configurations/validate")
    @Operation(summary = "Validate configuration", 
               description = "Comprehensive validation of configuration for completeness, security, and compliance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> validateConfiguration(
            @Valid @RequestBody SqlLoaderConfigRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("🔍 API Request: Validate SQL*Loader configuration for job: {}", request.getJobName());
        
        try {
            ConfigurationValidationResult validation = sqlLoaderConfigService.validateConfiguration(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("validation", validation);
            result.put("valid", validation.getValid());
            result.put("errorCount", validation.getErrors() != null ? validation.getErrors().size() : 0);
            result.put("warningCount", validation.getWarnings() != null ? validation.getWarnings().size() : 0);
            result.put("recommendationCount", validation.getRecommendations() != null ? validation.getRecommendations().size() : 0);
            result.put("validatedAt", LocalDateTime.now().toString());
            
            log.info("✅ Configuration validation completed for job: {} - {}", 
                    request.getJobName(), validation.getValid() ? "VALID" : "INVALID");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to validate configuration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to validate configuration", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @GetMapping("/configurations/{configId}/compliance")
    @Operation(summary = "Get compliance report", 
               description = "Generates comprehensive compliance report with regulatory requirements assessment")
    @PreAuthorize("hasRole('USER') or hasRole('COMPLIANCE')")
    public ResponseEntity<?> getComplianceReport(
            @Parameter(description = "Configuration ID") @PathVariable @NotBlank String configId,
            HttpServletRequest httpRequest) {
        
        log.info("📊 API Request: Get compliance report for configuration: {}", configId);
        
        try {
            ComplianceReport report = sqlLoaderConfigService.getComplianceReport(configId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("report", report);
            result.put("configId", configId);
            result.put("compliant", report.getCompliant());
            result.put("riskLevel", report.getRiskLevel());
            result.put("issueCount", report.getComplianceIssues() != null ? report.getComplianceIssues().size() : 0);
            result.put("generatedAt", LocalDateTime.now().toString());
            
            log.info("✅ Generated compliance report for configuration: {} - Status: {}", 
                    configId, report.getCompliant() ? "COMPLIANT" : "NON-COMPLIANT");
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Configuration not found: {}", configId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(
                    "Configuration not found", e.getMessage(), httpRequest.getRequestURI()));
                    
        } catch (Exception e) {
            log.error("❌ Failed to generate compliance report for {}: {}", configId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to generate compliance report", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @GetMapping("/configurations/{configId}/performance")
    @Operation(summary = "Get performance report", 
               description = "Generates performance analysis with optimization recommendations")
    @PreAuthorize("hasRole('USER') or hasRole('PERFORMANCE')")
    public ResponseEntity<?> getPerformanceReport(
            @Parameter(description = "Configuration ID") @PathVariable @NotBlank String configId,
            HttpServletRequest httpRequest) {
        
        log.info("📊 API Request: Get performance report for configuration: {}", configId);
        
        try {
            PerformanceReport report = sqlLoaderConfigService.getPerformanceReport(configId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("report", report);
            result.put("configId", configId);
            result.put("performanceProfile", report.getPerformanceProfile());
            result.put("needsOptimization", report.needsOptimization());
            result.put("recommendationCount", report.getPerformanceRecommendations() != null ? 
                       report.getPerformanceRecommendations().size() : 0);
            result.put("generatedAt", LocalDateTime.now().toString());
            
            log.info("✅ Generated performance report for configuration: {} - Profile: {}", 
                    configId, report.getPerformanceProfile());
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Configuration not found: {}", configId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(
                    "Configuration not found", e.getMessage(), httpRequest.getRequestURI()));
                    
        } catch (Exception e) {
            log.error("❌ Failed to generate performance report for {}: {}", configId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to generate performance report", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @GetMapping("/configurations/{configId}/security")
    @Operation(summary = "Get security assessment", 
               description = "Generates security assessment with risk analysis and recommendations")
    @PreAuthorize("hasRole('USER') or hasRole('SECURITY')")
    public ResponseEntity<?> getSecurityAssessment(
            @Parameter(description = "Configuration ID") @PathVariable @NotBlank String configId,
            HttpServletRequest httpRequest) {
        
        log.info("🔒 API Request: Get security assessment for configuration: {}", configId);
        
        try {
            SecurityAssessment assessment = sqlLoaderConfigService.getSecurityAssessment(configId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("assessment", assessment);
            result.put("configId", configId);
            result.put("riskLevel", assessment.getRiskLevel());
            result.put("containsPii", assessment.getContainsPiiData());
            result.put("encryptionConfigured", assessment.getEncryptionConfigured());
            result.put("warningCount", assessment.getSecurityWarnings() != null ? 
                       assessment.getSecurityWarnings().size() : 0);
            result.put("assessedAt", LocalDateTime.now().toString());
            
            log.info("✅ Generated security assessment for configuration: {} - Risk Level: {}", 
                    configId, assessment.getRiskLevel());
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Configuration not found: {}", configId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(
                    "Configuration not found", e.getMessage(), httpRequest.getRequestURI()));
                    
        } catch (Exception e) {
            log.error("❌ Failed to generate security assessment for {}: {}", configId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to generate security assessment", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    // ==================== REPORTING AND ANALYTICS ENDPOINTS ====================

    @GetMapping("/reports/usage")
    @Operation(summary = "Get configuration usage report", 
               description = "Generates system-wide usage statistics and metrics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REPORTER')")
    public ResponseEntity<?> getUsageReport(HttpServletRequest httpRequest) {
        log.info("📊 API Request: Get SQL*Loader configuration usage report");
        
        try {
            ConfigurationUsageReport report = sqlLoaderConfigService.getUsageReport();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("report", report);
            result.put("generatedAt", LocalDateTime.now().toString());
            
            log.info("✅ Generated usage report - Total configurations: {}", report.getTotalConfigurations());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to generate usage report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to generate usage report", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @GetMapping("/configurations/attention")
    @Operation(summary = "Get configurations requiring attention", 
               description = "Identifies configurations with issues requiring immediate attention")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getConfigurationsRequiringAttention(HttpServletRequest httpRequest) {
        log.info("⚠️ API Request: Get configurations requiring attention");
        
        try {
            List<ConfigurationAlert> alerts = sqlLoaderConfigService.getConfigurationsRequiringAttention();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("alerts", alerts);
            result.put("alertCount", alerts.size());
            result.put("retrievedAt", LocalDateTime.now().toString());
            
            log.info("✅ Retrieved {} configuration alerts", alerts.size());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to retrieve configuration alerts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to retrieve alerts", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @GetMapping("/reports/data-classification")
    @Operation(summary = "Get data classification report", 
               description = "Generates report on data classification and PII compliance")
    @PreAuthorize("hasRole('USER') or hasRole('COMPLIANCE')")
    public ResponseEntity<?> getDataClassificationReport(HttpServletRequest httpRequest) {
        log.info("📊 API Request: Get data classification report");
        
        try {
            DataClassificationReport report = sqlLoaderConfigService.getDataClassificationReport();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("report", report);
            result.put("piiComplianceRate", report.getPiiComplianceRate());
            result.put("generatedAt", LocalDateTime.now().toString());
            
            log.info("✅ Generated data classification report - PII Compliance: {}%", 
                    report.getPiiComplianceRate());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to generate data classification report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Failed to generate data classification report", e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Create standardized error response following established patterns.
     */
    private Map<String, Object> createErrorResponse(String message, String details, String path) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        error.put("details", details);
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("path", path);
        return error;
    }

    /**
     * Global exception handler for this controller.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e, HttpServletRequest request) {
        log.error("❌ Unhandled exception in SqlLoaderController: {} - {}", 
                request.getRequestURI(), e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
            "Internal server error", e.getMessage(), request.getRequestURI()));
    }

    /**
     * Handle validation exceptions.
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<?> handleValidationException(
            jakarta.validation.ConstraintViolationException e, HttpServletRequest request) {
        
        log.warn("⚠️ Validation error in SqlLoaderController: {}", e.getMessage());
        
        return ResponseEntity.badRequest().body(createErrorResponse(
            "Validation error", e.getMessage(), request.getRequestURI()));
    }
}