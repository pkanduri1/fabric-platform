package com.fabric.batch.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fabric.batch.model.FieldMapping;
import com.fabric.batch.model.YamlMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter to convert JSON configuration to YamlMapping objects
 * for compatibility with existing transformation infrastructure.
 * 
 * @author Senior Full Stack Developer Agent
 * @since Phase 3 - Batch Execution Enhancement
 */
@Component
@Slf4j
public class JsonMappingAdapter {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Convert JSON configuration string to YamlMapping object
     * 
     * @param jsonConfig JSON configuration string from database
     * @param transactionType Optional transaction type override
     * @return YamlMapping object compatible with existing processors
     */
    public YamlMapping convertJsonToYamlMapping(String jsonConfig, String transactionType) {
        try {
            JsonNode configNode = objectMapper.readTree(jsonConfig);
            
            YamlMapping yamlMapping = new YamlMapping();
            
            // Set basic properties
            String configTransactionType = configNode.path("transactionType").asText();
            yamlMapping.setTransactionType(transactionType != null ? transactionType : configTransactionType);
            yamlMapping.setFileType("FIXED_WIDTH"); // Default for atoctran jobs
            
            // Convert fieldMappings array to Map<String, FieldMapping>
            Map<String, FieldMapping> fieldMappings = new LinkedHashMap<>();
            JsonNode fieldMappingsNode = configNode.path("fieldMappings");
            
            if (fieldMappingsNode.isArray()) {
                for (JsonNode fieldNode : fieldMappingsNode) {
                    FieldMapping fieldMapping = convertJsonToFieldMapping(fieldNode);
                    fieldMappings.put(fieldMapping.getFieldName(), fieldMapping);
                }
            }
            
            yamlMapping.setFields(fieldMappings);
            
            log.debug("Converted JSON config to YamlMapping with {} fields", fieldMappings.size());
            
            return yamlMapping;
            
        } catch (Exception e) {
            log.error("Failed to convert JSON configuration to YamlMapping", e);
            throw new RuntimeException("Invalid JSON configuration format", e);
        }
    }
    
    /**
     * Convert individual field JSON node to FieldMapping object
     */
    private FieldMapping convertJsonToFieldMapping(JsonNode fieldNode) {
        FieldMapping mapping = new FieldMapping();
        
        // Required fields
        mapping.setFieldName(fieldNode.path("fieldName").asText());
        mapping.setTransformationType(fieldNode.path("transformationType").asText());
        mapping.setLength(fieldNode.path("length").asInt(0));
        mapping.setTargetPosition(fieldNode.path("targetPosition").asInt(0));
        
        // Optional fields
        mapping.setValue(fieldNode.path("value").asText(null));
        mapping.setSourceField(fieldNode.path("sourceField").asText(null));
        mapping.setTargetField(fieldNode.path("targetField").asText(null));
        mapping.setFormat(fieldNode.path("format").asText(null));
        mapping.setDefaultValue(fieldNode.path("defaultValue").asText(""));
        
        // Padding configuration (defaults for fixed-width format)
        mapping.setPad(fieldNode.path("pad").asText("right"));
        mapping.setPadChar(fieldNode.path("padChar").asText(" "));
        
        // Additional configurations
        mapping.setDataType(fieldNode.path("dataType").asText("string"));
        mapping.setSourceFormat(fieldNode.path("sourceFormat").asText(null));
        mapping.setTargetFormat(fieldNode.path("targetFormat").asText(null));
        
        // Handle composite fields if present
        if (fieldNode.has("composite") && fieldNode.path("composite").asBoolean()) {
            mapping.setComposite(true);
            // Additional composite field handling can be added here
        }
        
        // Handle conditional transformations if present
        if (fieldNode.has("conditions")) {
            // Conditional logic handling can be extended here
        }
        
        log.trace("Converted field '{}' with transformation type '{}'", 
                 mapping.getFieldName(), mapping.getTransformationType());
        
        return mapping;
    }
}