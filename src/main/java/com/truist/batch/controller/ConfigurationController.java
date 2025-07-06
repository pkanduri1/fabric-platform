package com.truist.batch.controller;

import com.truist.batch.service.ConfigurationService;
import com.truist.batch.service.YamlGenerationService;
import com.truist.batch.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced REST controller for batch configuration management with YAML generation.
 * 
 * This controller provides complete YAML generation capabilities, allowing
 * the frontend to generate working YAML files compatible with the existing
 * Spring Batch framework.
 */
@Slf4j
@RestController
@RequestMapping("/api/ui")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
@Tag(name = "Configuration Management", description = "APIs for managing batch configurations and YAML generation")
public class ConfigurationController {

    private final ConfigurationService configurationService;
    private final YamlGenerationService yamlGenerationService;

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
}