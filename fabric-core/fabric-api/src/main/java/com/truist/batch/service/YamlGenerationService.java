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
import com.truist.batch.model.Condition;
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
    private Map<String, FieldMapping> convertFieldMappings(List<FieldMapping> frontendMappings) {
        log.debug("🔄 Converting {} field mappings from frontend to backend format", frontendMappings.size());
        
        Map<String, FieldMapping> backendFields = new LinkedHashMap<>();
        
        for (FieldMapping frontendMapping : frontendMappings) {
            try {
                String fieldName = getFieldNameFromMapping(frontendMapping);
                
                if (fieldName == null || fieldName.trim().isEmpty()) {
                    log.warn("⚠️ Skipping field mapping with null/empty field name: {}", frontendMapping);
                    continue;
                }
                
                // Create backend FieldMapping with proper field name
                FieldMapping backendMapping = createBackendFieldMapping(frontendMapping, fieldName);
                
                // Use lowercase field name as key (consistent with existing YAML files)
                String mapKey = fieldName.toLowerCase().replace("-", "_");
                backendFields.put(mapKey, backendMapping);
                
                log.debug("✅ Converted field mapping: {} -> {}", fieldName, mapKey);
                
            } catch (Exception e) {
                log.error("❌ Failed to convert field mapping: {}", frontendMapping, e);
                throw new RuntimeException("Failed to convert field mapping: " + frontendMapping, e);
            }
        }
        
        log.debug("✅ Successfully converted {} field mappings", backendFields.size());
        return backendFields;
    }
    
    /**
	 * Determines the field name to use for the backend mapping.
	 * Uses fieldName if available, otherwise falls back to targetField.
	 */
    private String getFieldNameFromMapping(FieldMapping mapping) {
        // Try fieldName first (backend property)
        if (mapping.getFieldName() != null && !mapping.getFieldName().trim().isEmpty()) {
            return mapping.getFieldName();
        }
        
        // Try targetField (frontend property)  
        if (mapping.getTargetField() != null && !mapping.getTargetField().trim().isEmpty()) {
            return mapping.getTargetField();
        }
        
        return null;
    }
    
    private FieldMapping createBackendFieldMapping(FieldMapping frontendMapping, String fieldName) {
        FieldMapping backendMapping = new FieldMapping();
        
        // Set field identifiers
        backendMapping.setFieldName(fieldName);
        backendMapping.setTargetField(fieldName); // Ensure both are set
        backendMapping.setSourceField(frontendMapping.getSourceField());
        backendMapping.setFrom(frontendMapping.getFrom());
        
        // Set position and formatting (NOTE: these are int, not Integer)
        backendMapping.setTargetPosition(frontendMapping.getTargetPosition());
        backendMapping.setLength(frontendMapping.getLength());
        backendMapping.setDataType(frontendMapping.getDataType());
        backendMapping.setFormat(frontendMapping.getFormat());
        backendMapping.setSourceFormat(frontendMapping.getSourceFormat());
        backendMapping.setTargetFormat(frontendMapping.getTargetFormat());
        
        // Set transformation
        backendMapping.setTransformationType(frontendMapping.getTransformationType());
        backendMapping.setValue(frontendMapping.getValue());
        backendMapping.setDefaultValue(frontendMapping.getDefaultValue());
        backendMapping.setTransform(frontendMapping.getTransform());
        
        // Set padding
        backendMapping.setPad(frontendMapping.getPad());
        backendMapping.setPadChar(frontendMapping.getPadChar());
        
        // Set conditional logic (NOTE: this is List<Condition>, not Object)
        backendMapping.setConditions(frontendMapping.getConditions());
        
        // Set composite properties (NOTE: these exist in your model)
        backendMapping.setComposite(frontendMapping.isComposite());
        backendMapping.setSources(frontendMapping.getSources());
        backendMapping.setDelimiter(frontendMapping.getDelimiter());
        
        // Set DSL expression
        backendMapping.setExpression(frontendMapping.getExpression());
        
        return backendMapping;
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
        	validateFieldMapping(config.getFieldMappings(), result);
        }
        
        log.debug("✅ Validation complete: {} errors, {} warnings", 
                result.getErrors().size(), result.getWarnings().size());
        
        return result;
    }
    

    /**
     * Validates individual field mappings for YAML generation compatibility.
     */
    private void validateFieldMapping(List<FieldMapping> mappings, ValidationResult result) {
        if (mappings == null || mappings.isEmpty()) {
            result.addError("Field mappings list is empty");
            return;
        }
        
        log.debug("🔍 Validating {} field mappings", mappings.size());
        
        // Track target positions to check for duplicates
        Set<Integer> usedPositions = new HashSet<>();
        
        for (int i = 0; i < mappings.size(); i++) {
            FieldMapping mapping = mappings.get(i);
            
            try {
                // Validate individual field mapping
                validateSingleFieldMapping(mapping, i, result);
                
                // Check for duplicate target positions
                if (mapping.getTargetPosition() != 0) {
                    if (usedPositions.contains(mapping.getTargetPosition())) {
                        result.addError("Duplicate target position " + mapping.getTargetPosition() + 
                                       " found in field mapping " + i);
                    } else {
                        usedPositions.add(mapping.getTargetPosition());
                    }
                }
                
            } catch (Exception e) {
                result.addError("Error validating field mapping " + i + ": " + e.getMessage());
                log.warn("⚠️ Error validating field mapping {}: {}", i, e.getMessage());
            }
        }
        
        log.debug("✅ Field mapping validation complete");
    }
    /**
	 * Validates a single field mapping for YAML generation compatibility.
	 */
    private void validateSingleFieldMapping(FieldMapping mapping, int index, ValidationResult result) {
        String fieldName = getFieldNameFromMapping(mapping);
        String fieldContext = fieldName != null ? fieldName : ("field " + index);
        
        // Validate field name
        if (fieldName == null || fieldName.trim().isEmpty()) {
            result.addError("Field name is required for field mapping " + index);
            return; // Can't validate further without field name
        }
        
        // Validate target position (NOTE: int, so check for <= 0, not null)
        if (mapping.getTargetPosition() <= 0) {
            result.addError("Target position must be greater than 0 for field: " + fieldContext);
        }
        
        // Validate length (NOTE: int, so check for <= 0, not null)
        if (mapping.getLength() <= 0) {
            result.addError("Length must be greater than 0 for field: " + fieldContext);
        }
        
        // Validate transformation type
        if (mapping.getTransformationType() == null || mapping.getTransformationType().trim().isEmpty()) {
            result.addError("Transformation type is required for field: " + fieldContext);
            return;
        }
        
        // Validate transformation-specific requirements
        switch (mapping.getTransformationType().toLowerCase()) {
            case "constant":
                if (mapping.getValue() == null || mapping.getValue().trim().isEmpty()) {
                    result.addError("Value is required for constant transformation: " + fieldContext);
                }
                break;
                
            case "source":
                if (mapping.getSourceField() == null || mapping.getSourceField().trim().isEmpty()) {
                    result.addError("Source field is required for source transformation: " + fieldContext);
                }
                break;
                
            case "conditional":
                // FIXED: Check List<Condition>, not Object
                if (mapping.getConditions() == null || mapping.getConditions().isEmpty()) {
                    result.addError("Conditions required for conditional field: " + fieldContext);
                } else {
                    validateConditions(mapping.getConditions(), fieldContext, result);
                }
                break;
                
            case "composite":
                // FIXED: Use actual composite properties
                if (!mapping.isComposite()) {
                    result.addWarning("Field marked as composite transformation but composite flag is false: " + fieldContext);
                }
                if (mapping.getSources() == null || mapping.getSources().isEmpty()) {
                    result.addError("Sources are required for composite transformation: " + fieldContext);
                }
                break;
                
            default:
                result.addWarning("Unknown transformation type: " + mapping.getTransformationType() + 
                                 " for field: " + fieldContext);
                break;
        }
        
        // Validate data type
        if (mapping.getDataType() != null) {
            validateDataType(mapping.getDataType(), fieldContext, result);
        }
        
        // Validate padding configuration
        if (mapping.getPad() != null && !mapping.getPad().matches("left|right")) {
            result.addError("Pad must be 'left' or 'right' for field: " + fieldContext);
        }
        
        // Validate pad character
        if (mapping.getPadChar() != null && mapping.getPadChar().length() != 1) {
            result.addError("Pad character must be exactly 1 character for field: " + fieldContext);
        }
        
        // Validate DSL expression if present
        if (mapping.hasDSLExpression()) {
            validateDSLExpression(mapping.getExpression(), fieldContext, result);
        }
    }
    
    /**	 * Validates conditions within a field mapping.
     * 	* This method checks that conditions are not null, have valid 'if' expressions,
     * @param conditions
     * @param fieldContext
     * @param result
     */	
    private void validateConditions(List<Condition> conditions, String fieldContext, ValidationResult result) {
        if (conditions == null || conditions.isEmpty()) {
            result.addError("Conditions list is empty for field: " + fieldContext);
            return;
        }
        
        for (int i = 0; i < conditions.size(); i++) {
            Condition condition = conditions.get(i);
            validateSingleCondition(condition, fieldContext, i, result);
        }
    }
    
    /**
	 * Validates a single condition within a field mapping.
	 * 
	 * @param condition The condition to validate
	 * @param fieldContext Contextual information about the field
	 * @param conditionIndex Index of the condition in the list
	 * @param result Validation result object to collect errors/warnings
	 */
    private void validateSingleCondition(Condition condition, String fieldContext, int conditionIndex, ValidationResult result) {
        if (condition == null) {
            result.addError("Condition " + conditionIndex + " is null for field: " + fieldContext);
            return;
        }
        
        String conditionContext = fieldContext + " condition " + conditionIndex;
        
        // Validate 'if' expression
        if (condition.getIfExpr() == null || condition.getIfExpr().trim().isEmpty()) {
            result.addError("'ifExpr' is required for " + conditionContext);
        }
        
        // Validate 'then' value
        if (condition.getThen() == null || condition.getThen().trim().isEmpty()) {
            result.addError("'then' value is required for " + conditionContext);
        }
        
        // Validate else-if conditions if present
        if (condition.getElseIfExprs() != null && !condition.getElseIfExprs().isEmpty()) {
            for (int i = 0; i < condition.getElseIfExprs().size(); i++) {
                Condition elseIfCondition = condition.getElseIfExprs().get(i);
                validateSingleCondition(elseIfCondition, fieldContext, i, result);
            }
        }
        
        log.debug("✅ Condition validation passed for: {}", conditionContext);
    }
    
    /**
	 * Validates DSL expressions for field mappings.
	 * 
	 * @param expression The DSL expression to validate
	 * @param fieldContext Contextual information about the field
	 * @param result Validation result object to collect errors/warnings
	 */
    private void validateDSLExpression(String expression, String fieldContext, ValidationResult result) {
        if (expression == null || expression.trim().isEmpty()) {
            result.addError("DSL expression is empty for field: " + fieldContext);
            return;
        }
        
        // Basic DSL validation - you can enhance this based on your DSL syntax
        if (expression.length() > 1000) {
            result.addWarning("DSL expression is very long for field: " + fieldContext);
        }
        
        log.debug("✅ DSL expression validation passed for field: {}", fieldContext);
    }
    

    /**
     * Validates data type values
     * * @param dataType The data type to validate
     */
    private void validateDataType(String dataType, String fieldContext, ValidationResult result) {
        if (dataType == null || dataType.trim().isEmpty()) {
            result.addError("Data type cannot be empty for field: " + fieldContext);
            return;
        }
        
        String[] validDataTypes = {"String", "Numeric", "Date", "Boolean"};
        boolean isValid = false;
        
        for (String validType : validDataTypes) {
            if (validType.equalsIgnoreCase(dataType.trim())) {
                isValid = true;
                break;
            }
        }
        
        if (!isValid) {
            result.addWarning("Unknown data type '" + dataType + "' for field: " + fieldContext + 
                             ". Valid types: " + String.join(", ", validDataTypes));
        }
    }
}