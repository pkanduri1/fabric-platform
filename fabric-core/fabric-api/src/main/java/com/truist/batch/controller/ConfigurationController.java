package com.truist.batch.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.FieldMappingConfig;
import com.truist.batch.model.JobConfig;
import com.truist.batch.model.SourceField;
import com.truist.batch.model.SourceSystem;
import com.truist.batch.model.TestResult;
import com.truist.batch.model.ValidationResult;
import com.truist.batch.service.ConfigurationService;
import com.truist.batch.service.YamlFileService;
import com.truist.batch.service.YamlGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Enhanced REST controller for batch configuration management with YAML generation.
 * 
 * This controller provides complete YAML generation capabilities, allowing
 * the frontend to generate working YAML files compatible with the existing
 * Spring Batch framework.
 */
@Slf4j
@RestController
@RequestMapping("/ui")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
@Tag(name = "Configuration Management", description = "APIs for managing batch configurations and YAML generation")
public class ConfigurationController {

    private final ConfigurationService configurationService;
    private final YamlGenerationService yamlGenerationService;
    @Autowired
    private YamlFileService yamlFileService;

    // ==================== SOURCE SYSTEMS ENDPOINTS ====================

    @GetMapping("/source-systems")
    @Operation(summary = "Get all source systems")
    public ResponseEntity<List<SourceSystem>> getSourceSystems() {
        log.info("📋 API Request: Get all source systems");
        try {
            List<SourceSystem> systems = configurationService.getAllSourceSystems();
            return ResponseEntity.ok(systems);
        } catch (Exception e) {
            log.error("❌ Failed to get source systems: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/source-systems/{id}/jobs")
    @Operation(summary = "Get jobs for source system")
    public ResponseEntity<List<JobConfig>> getJobsForSourceSystem(@PathVariable String id) {
        log.info("📋 API Request: Get jobs for source system: {}", id);
        try {
            List<JobConfig> jobs = configurationService.getJobsForSystem(id);
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            log.error("❌ Failed to get jobs for system {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/source-systems/{id}/fields")
    @Operation(summary = "Get source fields")
    public ResponseEntity<List<SourceField>> getSourceFields(
            @PathVariable String id,
            @RequestParam(required = false) String jobName) {
        log.info("📋 API Request: Get source fields for system: {} job: {}", id, jobName);
        try {
            List<SourceField> fields = configurationService.getSourceFields(id, jobName);
            return ResponseEntity.ok(fields);
        } catch (Exception e) {
            log.error("❌ Failed to get source fields for {}/{}: {}", id, jobName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== FIELD MAPPINGS ENDPOINTS ====================

    @GetMapping("/mappings/{system}/{job}")
    @Operation(summary = "Get field mappings")
    public ResponseEntity<FieldMappingConfig> getFieldMappings(
            @PathVariable String system, 
            @PathVariable String job) {
        log.info("📋 API Request: Get field mappings for {}/{}", system, job);
        try {
            FieldMappingConfig config = configurationService.getFieldMappings(system, job);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("❌ Failed to get field mappings for {}/{}: {}", system, job, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/mappings/{system}/{job}/{txnType}")
    @Operation(summary = "Get field mappings for transaction type")
    public ResponseEntity<FieldMappingConfig> getFieldMappingsForTransaction(
            @PathVariable String system, 
            @PathVariable String job,
            @PathVariable String txnType) {
        log.info("📋 API Request: Get field mappings for {}/{}/{}", system, job, txnType);
        try {
            FieldMappingConfig config = configurationService.getFieldMappings(system, job, txnType);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("❌ Failed to get field mappings for {}/{}/{}: {}", system, job, txnType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== YAML GENERATION ENDPOINTS ====================

    @PostMapping("/mappings/generate-yaml")
    @Operation(summary = "Generate YAML from configuration", 
               description = "Converts FieldMappingConfig to YAML format compatible with GenericProcessor")
    public ResponseEntity<?> generateYaml(@RequestBody FieldMappingConfig config) {
        log.info("🔄 API Request: Generate YAML for {}/{}", 
                config.getSourceSystem(), config.getJobName());

        try {
            // 1. Validate configuration first
            ValidationResult validation = configurationService.validateConfiguration(config);
            if (!validation.isValid()) {
                log.warn("⚠️ Configuration validation failed: {}", validation.getErrors());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Configuration validation failed",
                    "errors", validation.getErrors()
                ));
            }

            // 2. Generate YAML content
            String yamlContent = configurationService.generateYaml(config);

            // 3. Return YAML content with metadata
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("yamlContent", yamlContent);
            response.put("fileType", config.getJobName());
            response.put("transactionType", config.getTransactionType());
            response.put("fieldCount", config.getFieldMappings().size());
            response.put("generatedAt", java.time.LocalDateTime.now().toString());

            log.info("✅ YAML generated successfully: {} fields, {} characters", 
                    config.getFieldMappings().size(), yamlContent.length());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Failed to generate YAML: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to generate YAML: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/mappings/generate-yaml/download")
    @Operation(summary = "Download YAML file", 
               description = "Generates and downloads YAML file for the configuration")
    public ResponseEntity<String> downloadYaml(@RequestBody FieldMappingConfig config) {
        log.info("📥 API Request: Download YAML for {}/{}", 
                config.getSourceSystem(), config.getJobName());

        try {
            String yamlContent = configurationService.generateYaml(config);
            String fileName = config.getJobName() + ".yml";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);

            log.info("✅ YAML download prepared: {}", fileName);
            return ResponseEntity.ok()
                .headers(headers)
                .body(yamlContent);

        } catch (Exception e) {
            log.error("❌ Failed to prepare YAML download: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error generating YAML: " + e.getMessage());
        }
    }

    @PostMapping("/mappings/validate")
    @Operation(summary = "Validate configuration for YAML generation", 
               description = "Validates that the configuration can be converted to valid YAML")
    public ResponseEntity<?> validateConfiguration(@RequestBody FieldMappingConfig config) {
        log.info("🔍 API Request: Validate configuration for {}/{}", 
                config.getSourceSystem(), config.getJobName());

        try {
            ValidationResult validation = configurationService.validateConfiguration(config);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", validation.isValid());
            response.put("errors", validation.getErrors());
            response.put("warnings", validation.getWarnings());
            response.put("fieldCount", config.getFieldMappings() != null ? config.getFieldMappings().size() : 0);
            response.put("summary", validation.getSummary());

            log.info("✅ Validation complete: {} errors, {} warnings", 
                    validation.getErrors().size(), validation.getWarnings().size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Validation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "valid", false,
                "errors", Arrays.asList("Validation error: " + e.getMessage())
            ));
        }
    }

    // ==================== CONFIGURATION MANAGEMENT ENDPOINTS ====================

    @PostMapping("/mappings/save")
    @Operation(summary = "Save configuration with YAML generation", 
               description = "Saves configuration to database and generates YAML file")
    public ResponseEntity<?> saveConfiguration(@RequestBody FieldMappingConfig config) {
        log.info("💾 API Request: Save configuration for {}/{}", 
                config.getSourceSystem(), config.getJobName());
        
        // Debug: Log the complete input JSON for troubleshooting
        try {
            ObjectMapper debugMapper = new ObjectMapper();
            String configJson = debugMapper.writeValueAsString(config);
            log.info("📥 INPUT JSON: {}", configJson);
        } catch (Exception e) {
            log.error("Failed to serialize config for debugging: {}", e.getMessage());
        }
        
        // CRITICAL FIX: Auto-correct transformation types and transaction type
        fixConfigurationData(config);

        try {
            // Save configuration (includes YAML generation)
            String result = configurationService.saveConfiguration(config);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result);
            response.put("sourceSystem", config.getSourceSystem());
            response.put("jobName", config.getJobName());
            response.put("yamlGenerated", true);
            response.put("savedAt", java.time.LocalDateTime.now().toString());

            log.info("✅ Configuration saved successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Failed to save configuration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to save configuration: " + e.getMessage()
            ));
        }
    }

    // ==================== PREVIEW AND TESTING ENDPOINTS ====================

    @PostMapping("/mappings/preview")
    @Operation(summary = "Generate output preview", 
               description = "Generates sample output using the configuration and sample data")
    public ResponseEntity<?> generatePreview(@RequestBody Map<String, Object> request) {
        log.info("👁️ API Request: Generate preview");

        try {
            FieldMappingConfig config = objectMapper.convertValue(
                request.get("configuration"), FieldMappingConfig.class);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sampleData = (List<Map<String, Object>>) 
                request.getOrDefault("sampleData", createDefaultSampleData());

            List<String> preview = configurationService.generatePreview(config, sampleData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("preview", preview);
            response.put("recordCount", preview.size());
            response.put("sampleData", sampleData);

            log.info("✅ Preview generated: {} records", preview.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Failed to generate preview: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to generate preview: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/test/{sourceSystem}/{jobName}")
    @Operation(summary = "Test configuration", 
               description = "Tests the configuration end-to-end including YAML generation")
    public ResponseEntity<?> testConfiguration(
            @PathVariable String sourceSystem,
            @PathVariable String jobName) {
        
        log.info("🧪 API Request: Test configuration for {}/{}", sourceSystem, jobName);

        try {
            TestResult result = configurationService.testConfiguration(sourceSystem, jobName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("testOutput", result.getSampleOutput());
            response.put("executionTimeMs", result.getExecutionTimeMs());
            response.put("testedAt", java.time.LocalDateTime.now().toString());

            log.info("✅ Test completed: {}", result.isSuccess());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Test failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Test failed: " + e.getMessage()
            ));
        }
    }

    // ==================== UTILITY METHODS ====================

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = 
        new com.fasterxml.jackson.databind.ObjectMapper();

    private List<Map<String, Object>> createDefaultSampleData() {
        Map<String, Object> sampleRow = new HashMap<>();
        sampleRow.put("acct_num", "123456789012345");
        sampleRow.put("location_code", "ABC123");
        sampleRow.put("balance_amt", "1234.56");
        sampleRow.put("last_payment_date", "20250701");
        return Arrays.asList(sampleRow);
    }

    /**
     * CRITICAL FIX: Auto-correct common UI data structure issues
     * - Fix transformation types incorrectly marked as "constant"
     * - Fix transaction type format
     */
    private void fixConfigurationData(FieldMappingConfig config) {
        log.info("🔧 FIXING: Auto-correcting UI data structure issues...");
        
        // Fix transaction type format (remove template description)
        if (config.getTransactionType() != null && config.getTransactionType().contains("Generated from")) {
            String originalTxnType = config.getTransactionType();
            // Extract the number from "Generated from atoctran/200 template" -> "200"
            if (originalTxnType.contains("/") && originalTxnType.contains(" template")) {
                String[] parts = originalTxnType.split("/");
                if (parts.length > 1) {
                    String txnType = parts[1].replace(" template", "").trim();
                    config.setTransactionType(txnType);
                    log.info("🔧 FIXED: Transaction type '{}' -> '{}'", originalTxnType, txnType);
                }
            }
        }
        
        // Fix transformation types - auto-detect based on field content
        if (config.getFieldMappings() != null) {
            for (FieldMapping field : config.getFieldMappings()) {
                String originalType = field.getTransformationType();
                String correctedType = detectCorrectTransformationType(field);
                
                if (!originalType.equals(correctedType)) {
                    field.setTransformationType(correctedType);
                    log.info("🔧 FIXED: Field '{}' transformation type '{}' -> '{}'", 
                            field.getFieldName(), originalType, correctedType);
                }
            }
        }
    }
    
    /**
     * Smart detection of correct transformation type based on field content
     */
    private String detectCorrectTransformationType(FieldMapping field) {
        // If sourceField looks like a query column name (contains underscore or is a typical column name)
        if (field.getSourceField() != null && !field.getSourceField().trim().isEmpty()) {
            String sourceField = field.getSourceField().trim();
            
            // Check if sourceField looks like a database column (contains underscore, letters)
            if (sourceField.matches(".*[a-zA-Z_].*")) {
                // Likely a database column name like "act_num", "batch_date", "contact_id"
                return "source";
            }
            
            // Check if sourceField is purely numeric (likely a constant value)
            if (sourceField.matches("\\d+")) {
                // Set the value for constant transformation
                if (field.getValue() == null || field.getValue().trim().isEmpty()) {
                    field.setValue(sourceField);
                }
                return "constant";
            }
        }
        
        // If field has a value set, it's likely a constant
        if (field.getValue() != null && !field.getValue().trim().isEmpty()) {
            return "constant";
        }
        
        // Default to original type if unclear
        return field.getTransformationType();
    }
    
    /**
     * Global exception handler for this controller
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e, HttpServletRequest request) {
        log.error("❌ Unhandled exception in ConfigurationController: {} - {}", 
                request.getRequestURI(), e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "success", false,
            "message", "Internal server error: " + e.getMessage(),
            "timestamp", java.time.LocalDateTime.now().toString(),
            "path", request.getRequestURI()
        ));
    }
    
    
    @PostMapping("/mappings/save-files/{system}/{job}")
    @Operation(summary = "Save configuration to database AND generate YAML files", 
               description = "Hybrid approach: saves to database with audit trail and generates YAML files for GenericProcessor")
    public ResponseEntity<Map<String, Object>> saveConfigurationFiles(
            @PathVariable String system,
            @PathVariable String job,
            @RequestBody List<FieldMappingConfig> configs) {
        
        log.info("💾 API Request: Save configuration files for {}/{} with {} transaction types", 
                system, job, configs.size());
        
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            
            // ==================== STEP 1: Validate All Configurations ====================
            for (FieldMappingConfig config : configs) {
                ValidationResult validation = configurationService.validateConfiguration(config);
                if (!validation.isValid()) {
                    log.warn("⚠️ Configuration validation failed for {}: {}", 
                            config.getTransactionType(), validation.getErrors());
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Configuration validation failed",
                        "transactionType", config.getTransactionType(),
                        "errors", validation.getErrors()
                    ));
                }
            }
            
            // ==================== STEP 2: Save to Database with Audit Trail ====================
            List<String> savedConfigs = new ArrayList<>();
            for (FieldMappingConfig config : configs) {
                try {
                    configurationService.saveConfiguration(config);
                    savedConfigs.add(config.getTransactionType() != null ? config.getTransactionType() : "default");
                    log.debug("✅ Saved to database: {}/{}/{}", system, job, config.getTransactionType());
                } catch (Exception e) {
                    log.error("❌ Failed to save configuration to database: {}", e.getMessage(), e);
                    throw new RuntimeException("Database save failed for " + config.getTransactionType(), e);
                }
            }
            
            // ==================== STEP 3: Generate and Save YAML Files ====================
            Map<String, String> savedFiles;
            try {
                savedFiles = yamlFileService.saveConfigurationFiles(system, job, configs);
                log.info("✅ Generated YAML files: {}", savedFiles);
            } catch (Exception e) {
                log.error("❌ Failed to generate YAML files: {}", e.getMessage(), e);
                throw new RuntimeException("YAML file generation failed", e);
            }
            
            // ==================== STEP 4: Validate Generated Files ====================
            boolean filesValid = yamlFileService.validateGeneratedFiles(system, job);
            if (!filesValid) {
                log.error("❌ Generated files failed validation for {}/{}", system, job);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Generated files failed validation"
                ));
            }
            
            // ==================== STEP 5: Build Success Response ====================
            result.put("success", true);
            result.put("message", "Configuration saved successfully to database and files");
            result.put("sourceSystem", system);
            result.put("jobName", job);
            result.put("transactionTypes", savedConfigs);
            result.put("savedFiles", savedFiles);
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("filesValidated", true);
            
            log.info("🎉 Successfully saved configuration files for {}/{}: database + {} files", 
                    system, job, savedFiles.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to save configuration files for {}/{}: {}", system, job, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to save configuration files: " + e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }
    
    @GetMapping("/mappings/{system}/{job}/all-types")
    @Operation(summary = "Get all transaction types for a job")
    public ResponseEntity<List<FieldMappingConfig>> getAllTransactionTypes(
            @PathVariable String system,
            @PathVariable String job) {
        
        log.info("📋 API Request: Get all transaction types for {}/{}", system, job);
        
        try {
            // Get all configurations for this source system and job
            List<FieldMappingConfig> configs = configurationService.getAllTransactionTypesForJob(system, job);
            
            if (configs.isEmpty()) {
                log.warn("⚠️ No configurations found for {}/{}", system, job);
                return ResponseEntity.notFound().build();
            }
            
            log.info("✅ Found {} transaction types for {}/{}", configs.size(), system, job);
            return ResponseEntity.ok(configs);
            
        } catch (Exception e) {
            log.error("❌ Failed to get all transaction types for {}/{}: {}", system, job, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/mappings/validate-files/{system}/{job}")
    @Operation(summary = "Validate generated YAML files")
    public ResponseEntity<Map<String, Object>> validateGeneratedFiles(
            @PathVariable String system,
            @PathVariable String job) {
        
        log.info("🔍 API Request: Validate generated files for {}/{}", system, job);
        
        try {
            boolean isValid = yamlFileService.validateGeneratedFiles(system, job);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("valid", isValid);
            result.put("sourceSystem", system);
            result.put("jobName", job);
            result.put("timestamp", LocalDateTime.now().toString());
            
            if (isValid) {
                result.put("message", "Generated files are valid and ready for batch processing");
                log.info("✅ File validation passed for {}/{}", system, job);
            } else {
                result.put("message", "Generated files failed validation");
                log.warn("❌ File validation failed for {}/{}", system, job);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to validate files for {}/{}: {}", system, job, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "valid", false,
                "message", "Validation failed: " + e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }
    
    @GetMapping("/file-structure/{system}")
    @Operation(summary = "Get current file structure for source system")
    public ResponseEntity<Map<String, Object>> getFileStructure(@PathVariable String system) {
        log.info("📁 API Request: Get file structure for {}", system);
        
        try {
            Map<String, Object> structure = new LinkedHashMap<>();
            
            // Check batch-props file
            Path batchPropsPath = Paths.get("src/main/resources/batch-sources", system + "-batch-props.yml");
            structure.put("batchPropsFile", Map.of(
                "path", system + "-batch-props.yml",
                "exists", Files.exists(batchPropsPath),
                "lastModified", Files.exists(batchPropsPath) ? 
                    Files.getLastModifiedTime(batchPropsPath).toString() : null
            ));
            
            // Check mappings directory
            Path mappingsDir = Paths.get("src/main/resources/batch-sources/mappings");
            if (Files.exists(mappingsDir)) {
                List<Map<String, ? extends Object>> mappingFiles = Files.walk(mappingsDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().contains("/" + system + "/"))
                    .map(path -> {
                        try {
                            String relativePath = mappingsDir.relativize(path).toString();
                            return Map.of(
                                "path", relativePath,
                                "exists", true,
                                "lastModified", Files.getLastModifiedTime(path).toString()
                            );
                        } catch (IOException e) {
                            return Map.of("path", path.toString(), "error", e.getMessage());
                        }
                    })
                    .collect(Collectors.toList());
                
                structure.put("mappingFiles", mappingFiles);
            } else {
                structure.put("mappingFiles", List.of());
            }
            
            return ResponseEntity.ok(structure);
            
        } catch (Exception e) {
            log.error("❌ Failed to get file structure for {}: {}", system, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to get file structure: " + e.getMessage()
            ));
        }
    }
}