package com.truist.batch.service;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.FieldMappingConfig;
import com.truist.batch.model.ValidationResult;
import com.truist.batch.model.YamlMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for converting FieldMappingConfig objects to YAML format
 * compatible with the existing GenericProcessor and YamlMappingService.
 * 
 * This service ensures that generated YAML files have the exact same structure
 * as manually created YAML files, maintaining compatibility with the existing
 * Spring Batch framework.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YamlGenerationService {

    private final ObjectMapper yamlMapper;

    /**
     * Constructor that sets up the YAML ObjectMapper with proper configuration
     * to match the existing YAML format expectations.
     */
    public YamlGenerationService() {
        YAMLFactory yamlFactory = new YAMLFactory()
            .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
            .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
            .configure(YAMLGenerator.Feature.SPLIT_LINES, false);
        
        this.yamlMapper = new ObjectMapper(yamlFactory);
    }

    /**
     * Converts a FieldMappingConfig to YAML string format.
     * 
     * @param config The configuration object
     * @return YAML string compatible with GenericProcessor
     */
    public String generateYamlFromConfiguration(FieldMappingConfig config) {
        log.info("🔄 Converting FieldMappingConfig to YAML: {} / {}", 
                config.getSourceSystem(), config.getJobName());

        try {
            // Convert to YamlMapping structure
            YamlMapping yamlMapping = convertToYamlMapping(config);
            
            // Generate YAML string
            StringWriter writer = new StringWriter();
            yamlMapper.writeValue(writer, yamlMapping);
            String yamlContent = writer.toString();
            
            log.debug("📄 Generated YAML:\n{}", yamlContent);
            return yamlContent;
            
        } catch (Exception e) {
            log.error("❌ Failed to generate YAML from configuration: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate YAML from configuration", e);
        }
    }

    /**
     * Converts multiple FieldMappingConfig objects (different transaction types)
     * into a multi-document YAML string.
     * 
     * @param configs List of configurations for different transaction types
     * @return Multi-document YAML string
     */
    public String generateMultiDocumentYaml(List<FieldMappingConfig> configs) {
        log.info("🔄 Converting {} configurations to multi-document YAML", configs.size());

        if (configs.isEmpty()) {
            throw new IllegalArgumentException("Configuration list cannot be empty");
        }

        try {
            StringBuilder yamlBuilder = new StringBuilder();
            
            for (int i = 0; i < configs.size(); i++) {
                if (i > 0) {
                    yamlBuilder.append("---\n"); // Document separator
                }
                
                YamlMapping yamlMapping = convertToYamlMapping(configs.get(i));
                
                StringWriter writer = new StringWriter();
                yamlMapper.writeValue(writer, yamlMapping);
                yamlBuilder.append(writer.toString());
            }
            
            String result = yamlBuilder.toString();
            log.debug("📄 Generated multi-document YAML:\n{}", result);
            return result;
            
        } catch (Exception e) {
            log.error("❌ Failed to generate multi-document YAML: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate multi-document YAML", e);
        }
    }

    /**
     * Converts FieldMappingConfig to YamlMapping structure.
     * This is the critical mapping between frontend and backend data models.
     */
    private YamlMapping convertToYamlMapping(FieldMappingConfig config) {
        YamlMapping yamlMapping = new YamlMapping();
        
        // Set basic properties
        yamlMapping.setFileType(config.getJobName()); // jobName becomes fileType
        yamlMapping.setTransactionType(config.getTransactionType());
        
        // Convert field mappings from list to map structure
        Map<String, FieldMapping> backendFields = convertFieldMappings(config.getFieldMappings());
        yamlMapping.setFields(backendFields);
        
        log.debug("✅ Converted config: {} fields for {}/{}", 
                backendFields.size(), 
                yamlMapping.getFileType(), 
                yamlMapping.getTransactionType());
        
        return yamlMapping;
    }

    /**
     * Converts FieldMapping list to FieldMapping map.
     * The input FieldMappingConfig uses a List<FieldMapping>, but YamlMapping expects Map<String, FieldMapping>.
     */
    private Map<String, FieldMapping> convertFieldMappings(List<FieldMapping> fieldMappings) {
        Map<String, FieldMapping> backendFields = new LinkedHashMap<>();
        
        for (FieldMapping field : fieldMappings) {
            // Use lowercase fieldName as key to match existing YAML structure
            String key = field.getFieldName().toLowerCase().replace("_", "-");
            backendFields.put(key, field);
        }
        
        return backendFields;
    }

    /**
     * Validates that the configuration can be converted to valid YAML.
     * 
     * @param config Configuration to validate
     * @return Validation result with any errors or warnings
     */
    public ValidationResult validateForYamlGeneration(FieldMappingConfig config) {
        log.debug("🔍 Validating configuration for YAML generation");
        
        // Start with valid=true, will be set to false by addError() calls
        ValidationResult result = new ValidationResult(true);
        
        // Basic validation checks
        if (config.getFieldMappings() == null || config.getFieldMappings().isEmpty()) {
            result.addError("No field mappings defined");
        }
        
        if (config.getSourceSystem() == null || config.getSourceSystem().trim().isEmpty()) {
            result.addError("Source system is required");
        }
        
        if (config.getJobName() == null || config.getJobName().trim().isEmpty()) {
            result.addError("Job name is required");
        }
        
        // Field-level validation
        if (config.getFieldMappings() != null) {
            validateFieldMappings(config.getFieldMappings(), result);
        }
        
        log.debug("✅ Validation complete: {} errors, {} warnings", 
                result.getErrors().size(), result.getWarnings().size());
        
        return result;
    }

    /**
     * Validates individual field mappings for YAML generation compatibility.
     */
    private void validateFieldMappings(List<FieldMapping> fields, ValidationResult result) {
        Set<Integer> usedPositions = new HashSet<>();
        Set<String> usedTargetFields = new HashSet<>();
        
        for (FieldMapping field : fields) {
            // Check for duplicate positions
            if (usedPositions.contains(field.getTargetPosition())) {
                result.addError("Duplicate target position: " + field.getTargetPosition());
                result.getDuplicatePositions().add(String.valueOf(field.getTargetPosition()));
            }
            usedPositions.add(field.getTargetPosition());
            
            // Check for duplicate target fields
            if (usedTargetFields.contains(field.getTargetField())) {
                result.addError("Duplicate target field: " + field.getTargetField());
                result.getDuplicateFieldNames().add(field.getTargetField());
            }
            usedTargetFields.add(field.getTargetField());
            
            // Validate transformation-specific requirements
            validateTransformationType(field, result);
        }
    }

    /**
     * Validates transformation-specific requirements for each field.
     */
    private void validateTransformationType(FieldMapping field, ValidationResult result) {
        String fieldName = field.getFieldName();
        String transformationType = field.getTransformationType();
        
        if (transformationType == null) {
            result.addError("Transformation type is required for field: " + fieldName);
            result.getMissingRequiredFields().add(fieldName + " (transformationType)");
            return;
        }
        
        switch (transformationType) {
            case "source":
                if (field.getSourceField() == null || field.getSourceField().trim().isEmpty()) {
                    result.addError("Source field required for field: " + fieldName);
                    result.getMissingRequiredFields().add(fieldName + " (sourceField)");
                }
                break;
                
            case "constant":
                if (field.getValue() == null && field.getDefaultValue() == null) {
                    result.addWarning("No value specified for constant field: " + fieldName);
                }
                break;
                
            case "composite":
                if (field.getSources() == null || field.getSources().isEmpty()) {
                    result.addError("Sources required for composite field: " + fieldName);
                    result.getMissingRequiredFields().add(fieldName + " (sources)");
                }
                break;
                
            case "conditional":
                if (field.getConditions() == null || field.getConditions().isEmpty()) {
                    result.addError("Conditions required for conditional field: " + fieldName);
                    result.getMissingRequiredFields().add(fieldName + " (conditions)");
                }
                break;
        }
    }
}