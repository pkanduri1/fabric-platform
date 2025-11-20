package com.truist.batch.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.truist.batch.model.FieldMappingConfig;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.FieldTemplate;
import com.truist.batch.model.FileTypeTemplate;
import com.truist.batch.model.TemplateImportRequest;
import com.truist.batch.model.TemplateImportResult;
import com.truist.batch.model.TemplateMetadata;
import com.truist.batch.model.TemplateToConfigurationResult;
import com.truist.batch.model.ValidationResult;
import com.truist.batch.service.TemplateService;
import com.truist.batch.service.ConfigurationService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RestController
@RequestMapping("/admin/templates")
@CrossOrigin(origins = { "http://localhost:3000", "https://localhost:3000",
        "http://localhost:3001" }, allowedHeaders = "*", methods = {
                RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS })
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Get all available file types
     * Frontend expects: GET /api/admin/templates/file-types
     */
    @GetMapping("/file-types")
    public ResponseEntity<List<FileTypeTemplate>> getAllFileTypes() {
        try {
            List<FileTypeTemplate> fileTypes = templateService.getAllFileTypes();
            return ResponseEntity.ok(fileTypes);
        } catch (Exception e) {
            log.error("Error fetching file types", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get complete template for a file type
     * Frontend expects: GET /api/admin/templates/{fileType}
     */
    @GetMapping("/{fileType}")
    public ResponseEntity<FileTypeTemplate> getTemplate(@PathVariable String fileType) {
        try {
            Optional<FileTypeTemplate> template = templateService.getFileTypeTemplate(fileType);
            return template.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching template for fileType: {}", fileType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get fields for a specific file type and transaction type
     * Frontend expects: GET
     * /api/admin/templates/{fileType}/{transactionType}/fields
     */
    @GetMapping("/{fileType}/{transactionType}/fields")
    public ResponseEntity<List<FieldTemplate>> getTemplateFields(
            @PathVariable String fileType,
            @PathVariable String transactionType) {
        try {
            List<FieldTemplate> fields = templateService.getFieldTemplatesByFileTypeAndTransactionType(fileType,
                    transactionType);
            return ResponseEntity.ok(fields);
        } catch (Exception e) {
            log.error("Error fetching fields for fileType: {}, transactionType: {}", fileType, transactionType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get transaction types for a file type
     * Frontend expects: GET /api/admin/templates/{fileType}/transaction-types
     */
    @GetMapping("/{fileType}/transaction-types")
    public ResponseEntity<List<String>> getTransactionTypes(@PathVariable String fileType) {
        try {
            List<String> transactionTypes = templateService.getTransactionTypesByFileType(fileType);
            return ResponseEntity.ok(transactionTypes);
        } catch (Exception e) {
            log.error("Error fetching transaction types for fileType: {}", fileType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create configuration from template
     * Frontend expects: POST
     * /api/admin/templates/{fileType}/{transactionType}/create-config
     */
    @PostMapping("/{fileType}/{transactionType}/create-config")
    @Transactional
    public ResponseEntity<FieldMappingConfig> createConfigurationFromTemplate(
            @PathVariable String fileType,
            @PathVariable String transactionType,
            @RequestParam String sourceSystem,
            @RequestParam String jobName,
            @RequestParam(defaultValue = "system") String createdBy) {
        try {
            log.info(
                    "🔍 TRACE: Template configuration save called - Creating configuration from template: {}/{} for {}/{}",
                    fileType, transactionType, sourceSystem, jobName);
            log.info(
                    "🔍 TRACE: Request parameters - fileType={}, transactionType={}, sourceSystem={}, jobName={}, createdBy={}",
                    fileType, transactionType, sourceSystem, jobName, createdBy);

            // 1. Create configuration from template (this should now use saved mappings!)
            FieldMappingConfig config = templateService.createConfigurationFromTemplate(
                    fileType, transactionType, sourceSystem, jobName, createdBy);

            // Debug: Log the resulting configuration
            if (config != null && config.getFieldMappings() != null) {
                log.info("🔍 TRACE: Created configuration with {} field mappings", config.getFieldMappings().size());
                log.info("🔍 TRACE: Full field mapping details:");
                for (int i = 0; i < config.getFieldMappings().size(); i++) {
                    FieldMapping mapping = config.getFieldMappings().get(i);
                    log.info(
                            "🔍 TRACE: Field[{}]: name={}, transformationType={}, value={}, sourceField={}, defaultValue={}",
                            i, mapping.getFieldName(), mapping.getTransformationType(), mapping.getValue(),
                            mapping.getSourceField(), mapping.getDefaultValue());
                }
            } else {
                log.warn("🚨 TRACE: Configuration is null or has no field mappings!");
            }

            // 2. Save the configuration to batch_configurations table
            String saveResult = configurationService.saveConfiguration(config);
            log.info("DEBUG: Configuration saved to batch_configurations table: {}", saveResult);

            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("DEBUG: Error creating and saving configuration from template: {}/{}", fileType, transactionType,
                    e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Debug endpoint: Create configuration from template WITHOUT saving
     */
    @PostMapping("/{fileType}/{transactionType}/create-config-debug")
    public ResponseEntity<FieldMappingConfig> createConfigurationFromTemplateDebug(
            @PathVariable String fileType,
            @PathVariable String transactionType,
            @RequestParam String sourceSystem,
            @RequestParam String jobName,
            @RequestParam(defaultValue = "system") String createdBy) {
        try {
            log.info("DEBUG: Creating configuration from template: {}/{} for {}/{}",
                    fileType, transactionType, sourceSystem, jobName);

            // 1. Create configuration from template (WITHOUT SAVING)
            FieldMappingConfig config = templateService.createConfigurationFromTemplate(
                    fileType, transactionType, sourceSystem, jobName, createdBy);

            log.info("DEBUG: Configuration created with {} field mappings",
                    config.getFieldMappings() != null ? config.getFieldMappings().size() : 0);

            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("DEBUG: Error creating configuration from template: {}/{}", fileType, transactionType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Import template from Excel file
     * Frontend expects: POST /api/admin/templates/import/excel
     */
    @PostMapping("/import/excel")
    public ResponseEntity<TemplateImportResult> importFromExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam String fileType,
            @RequestParam(defaultValue = "system") String createdBy) {
        try {
            TemplateImportResult result = templateService.importFromExcel(file, fileType, createdBy);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error importing template from Excel", e);
            TemplateImportResult errorResult = new TemplateImportResult(
                    false, fileType, 0, 0,
                    List.of("Import failed: " + e.getMessage()),
                    List.of(), "Excel import failed");
            return ResponseEntity.ok(errorResult);
        }
    }

    /**
     * Import template from JSON
     * Frontend expects: POST /api/admin/templates/import/json
     */
    @PostMapping("/import/json")
    public ResponseEntity<TemplateImportResult> importFromJson(@RequestBody TemplateImportRequest request) {
        try {
            TemplateImportResult result = templateService.importFromJson(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error importing template from JSON", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Validate template
     * Frontend expects: POST /api/admin/templates/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validateTemplate(@RequestBody FileTypeTemplate template) {
        try {
            ValidationResult result = templateService.validateTemplate(template);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error validating template", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create new file type template
     * Frontend expects: POST /api/admin/templates/file-types
     */
    @PostMapping("/file-types")
    public ResponseEntity<Map<String, Object>> createFileTypeTemplate(
            @RequestBody FileTypeTemplate template,
            @RequestParam(defaultValue = "system") String createdBy) {
        try {
            FileTypeTemplate created = templateService.createFileTypeTemplate(template, createdBy);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileType", created.getFileType());
            response.put("message", "Template created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating file type template", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update file type template
     * Frontend expects: PUT /api/admin/templates/file-types/{fileType}
     */
    @PutMapping("/file-types/{fileType}")
    public ResponseEntity<Map<String, Object>> updateFileTypeTemplate(
            @PathVariable String fileType,
            @RequestBody FileTypeTemplate template,
            @RequestParam(defaultValue = "system") String modifiedBy) {
        try {
            template.setFileType(fileType);
            FileTypeTemplate updated = templateService.updateFileTypeTemplate(template, modifiedBy);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileType", updated.getFileType());
            response.put("message", "Template updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating file type template: {}", fileType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete file type template
     * Frontend expects: DELETE /api/admin/templates/file-types/{fileType}
     */
    @DeleteMapping("/file-types/{fileType}")
    public ResponseEntity<Map<String, Object>> deleteFileTypeTemplate(
            @PathVariable String fileType,
            @RequestParam(defaultValue = "system") String deletedBy) {
        try {
            templateService.deleteFileTypeTemplate(fileType, deletedBy);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Template deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting file type template: {}", fileType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create a new file type without fields - simplified version
     * Frontend expects: POST /api/admin/templates/file-types/simple
     */
    @PostMapping("/file-types/simple")
    public ResponseEntity<Map<String, Object>> createSimpleFileType(
            @RequestBody Map<String, Object> request,
            @RequestParam(defaultValue = "system") String createdBy) {
        try {
            String fileType = (String) request.get("fileType");
            String description = (String) request.get("description");

            if (fileType == null || fileType.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "File type is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Create a minimal FileTypeTemplate with just the basics
            FileTypeTemplate template = new FileTypeTemplate();
            template.setFileType(fileType.trim());
            template.setDescription(description);
            template.setTotalFields(0);
            template.setRecordLength(0);
            template.setEnabled("Y");

            FileTypeTemplate created = templateService.createFileTypeTemplate(template, createdBy);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileType", created.getFileType());
            response.put("description", created.getDescription());
            response.put("message", "File type created successfully. You can now add fields to this template.");

            log.info("Created simple file type: {} by {}", fileType, createdBy);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating simple file type", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to create file type: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // Add these methods to the existing TemplateController.java

    /**
     * Create individual field template
     * Frontend expects: POST /api/admin/templates/{fileType}/fields
     */
    @PostMapping("/{fileType}/fields")
    public ResponseEntity<FieldTemplate> createField(
            @PathVariable String fileType,
            @RequestBody Map<String, Object> fieldRequest,
            @RequestParam(defaultValue = "system") String createdBy) {
        try {
            // Convert the request to FieldTemplate
            FieldTemplate fieldTemplate = new FieldTemplate();
            fieldTemplate.setFileType(fileType);
            fieldTemplate.setTransactionType((String) fieldRequest.getOrDefault("transactionType", "default"));
            fieldTemplate.setFieldName((String) fieldRequest.get("fieldName"));
            fieldTemplate.setTargetPosition((Integer) fieldRequest.get("targetPosition"));
            fieldTemplate.setLength((Integer) fieldRequest.get("length"));
            fieldTemplate.setDataType((String) fieldRequest.get("dataType"));
            fieldTemplate.setFormat((String) fieldRequest.get("format"));
            fieldTemplate.setRequired((String) fieldRequest.getOrDefault("required", "N"));
            fieldTemplate.setDescription((String) fieldRequest.get("description"));
            fieldTemplate.setEnabled((String) fieldRequest.getOrDefault("enabled", "Y"));
            fieldTemplate.setCreatedBy(createdBy);

            FieldTemplate created = templateService.createFieldTemplate(fieldTemplate, createdBy);
            log.info("Created field template: {} for fileType: {}", fieldTemplate.getFieldName(), fileType);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error creating field template for fileType: {}, field: {}", fileType,
                    fieldRequest.get("fieldName"), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update individual field template
     * Frontend expects: PUT /api/admin/templates/{fileType}/fields/{fieldName}
     */
    @PutMapping("/{fileType}/fields/{fieldName}")
    public ResponseEntity<FieldTemplate> updateField(
            @PathVariable String fileType,
            @PathVariable String fieldName,
            @RequestBody FieldTemplate fieldTemplate,
            @RequestParam(defaultValue = "system") String modifiedBy) {
        try {
            // Ensure path parameters match body
            fieldTemplate.setFileType(fileType);
            fieldTemplate.setFieldName(fieldName);
            fieldTemplate.setModifiedBy(modifiedBy);

            FieldTemplate updated = templateService.updateFieldTemplate(fieldTemplate);
            log.info("Updated field template: {} for fileType: {}", fieldName, fileType);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating field template for fileType: {}, field: {}", fileType, fieldName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete individual field template
     * Frontend expects: DELETE /api/admin/templates/{fileType}/fields/{fieldName}
     */
    @DeleteMapping("/{fileType}/fields/{fieldName}")
    public ResponseEntity<Map<String, Object>> deleteField(
            @PathVariable String fileType,
            @PathVariable String fieldName,
            @RequestParam String transactionType,
            @RequestParam(defaultValue = "system") String deletedBy) {
        try {
            templateService.deleteFieldTemplate(fileType, transactionType, fieldName, deletedBy);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Field deleted successfully");
            response.put("fieldName", fieldName);

            log.info("Deleted field template: {} for fileType: {}, transactionType: {}", fieldName, fileType,
                    transactionType);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting field template for fileType: {}, transactionType: {}, field: {}", fileType,
                    transactionType, fieldName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Bulk update field templates
     * Frontend expects: PUT /api/admin/templates/{fileType}/fields/bulk
     */
    @PutMapping("/{fileType}/fields/bulk")
    public ResponseEntity<List<FieldTemplate>> bulkUpdateFields(
            @PathVariable String fileType,
            @RequestBody Map<String, Object> request,
            @RequestParam(defaultValue = "system") String modifiedBy) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fieldsData = (List<Map<String, Object>>) request.get("fields");

            if (fieldsData == null || fieldsData.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            List<FieldTemplate> fields = fieldsData.stream()
                    .map(this::mapToFieldTemplate)
                    .peek(field -> {
                        field.setFileType(fileType);
                        field.setModifiedBy(modifiedBy);
                    })
                    .toList();

            List<FieldTemplate> updated = templateService.bulkUpdateFieldTemplates(fileType, fields);
            log.info("Bulk updated {} field templates for fileType: {}", fields.size(), fileType);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error bulk updating field templates for fileType: {}", fileType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Duplicate field template
     * Frontend expects: POST
     * /api/admin/templates/{fileType}/fields/{fieldName}/duplicate
     */
    @PostMapping("/{fileType}/fields/{fieldName}/duplicate")
    public ResponseEntity<FieldTemplate> duplicateField(
            @PathVariable String fileType,
            @PathVariable String fieldName,
            @RequestBody Map<String, Object> request,
            @RequestParam(defaultValue = "system") String createdBy) {
        try {
            String newFieldName = (String) request.get("newFieldName");
            Integer newPosition = (Integer) request.get("newPosition");
            String transactionType = (String) request.getOrDefault("transactionType", "default");

            if (newFieldName == null || newPosition == null) {
                return ResponseEntity.badRequest().build();
            }

            FieldTemplate duplicated = templateService.duplicateFieldTemplate(
                    fileType, transactionType, fieldName, newFieldName, newPosition, createdBy);

            log.info("Duplicated field template: {} to {} for fileType: {}", fieldName, newFieldName, fileType);
            return ResponseEntity.ok(duplicated);
        } catch (Exception e) {
            log.error("Error duplicating field template for fileType: {}, field: {}", fileType, fieldName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Reorder field templates
     * Frontend expects: POST /api/admin/templates/{fileType}/fields/reorder
     */
    @PostMapping("/{fileType}/fields/reorder")
    public ResponseEntity<List<FieldTemplate>> reorderFields(
            @PathVariable String fileType,
            @RequestBody Map<String, Object> request,
            @RequestParam(defaultValue = "system") String modifiedBy) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fieldOrders = (List<Map<String, Object>>) request.get("fieldOrders");

            if (fieldOrders == null || fieldOrders.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            List<FieldTemplate> reordered = templateService.reorderFieldTemplates(fileType, fieldOrders, modifiedBy);
            log.info("Reordered {} field templates for fileType: {}", fieldOrders.size(), fileType);
            return ResponseEntity.ok(reordered);
        } catch (Exception e) {
            log.error("Error reordering field templates for fileType: {}", fileType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create configuration from template with metadata
     * Frontend expects: POST
     * /api/admin/templates/{fileType}/{transactionType}/create-config-with-metadata
     */
    @PostMapping("/{fileType}/{transactionType}/create-config-with-metadata")
    @Transactional
    public ResponseEntity<TemplateToConfigurationResult> createConfigurationFromTemplateWithMetadata(
            @PathVariable String fileType,
            @PathVariable String transactionType,
            @RequestParam String sourceSystem,
            @RequestParam String jobName,
            @RequestParam(defaultValue = "system") String createdBy) {
        try {
            log.info("Creating configuration with metadata from template: {}/{} for {}/{}",
                    fileType, transactionType, sourceSystem, jobName);

            // 1. Create the basic configuration
            FieldMappingConfig config = templateService.createConfigurationFromTemplate(
                    fileType, transactionType, sourceSystem, jobName, createdBy);

            // 2. Save the configuration to batch_configurations table
            String saveResult = configurationService.saveConfiguration(config);
            log.info("Configuration saved to batch_configurations table: {}", saveResult);

            // 3. Add template metadata
            TemplateMetadata metadata = new TemplateMetadata();
            metadata.setFileType(fileType);
            metadata.setTransactionType(transactionType);
            metadata.setTemplateVersion(1);
            metadata.setFieldsFromTemplate(config.getFieldMappings() != null ? config.getFieldMappings().size() : 0);
            metadata.setTotalFields(config.getFieldMappings() != null ? config.getFieldMappings().size() : 0);
            metadata.setGeneratedAt(java.time.LocalDateTime.now().toString());
            metadata.setGeneratedBy(createdBy);

            // 4. Create the result with metadata using the factory method
            TemplateToConfigurationResult result = TemplateToConfigurationResult.fromFieldMappingConfig(config,
                    metadata);

            log.info("Created configuration with metadata for template: {}/{}", fileType, transactionType);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error creating configuration with metadata from template: {}/{}", fileType, transactionType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Helper method to convert Map to FieldTemplate
     */
    private FieldTemplate mapToFieldTemplate(Map<String, Object> data) {
        FieldTemplate field = new FieldTemplate();
        field.setFieldName((String) data.get("fieldName"));
        field.setTransactionType((String) data.getOrDefault("transactionType", "default"));
        field.setTargetPosition((Integer) data.get("targetPosition"));
        field.setLength((Integer) data.get("length"));
        field.setDataType((String) data.get("dataType"));
        field.setFormat((String) data.get("format"));
        field.setRequired((String) data.getOrDefault("required", "N"));
        field.setDescription((String) data.get("description"));
        field.setEnabled((String) data.getOrDefault("enabled", "Y"));
        return field;
    }

    /**
     * Save complete template configuration (Job + Fields + Query)
     * Frontend expects: POST /api/admin/templates/save-config
     */
    @PostMapping("/save-config")
    public ResponseEntity<Map<String, Object>> saveTemplateConfiguration(
            @RequestBody com.truist.batch.model.TemplateConfigDto config) {
        try {
            String configId = templateService.saveTemplateConfiguration(config);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configId", configId);
            response.put("message", "Configuration saved successfully");

            log.info("Saved template configuration for job: {}, ID: {}", config.getJobName(), configId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error saving template configuration", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
