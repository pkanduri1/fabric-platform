package com.truist.batch.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.truist.batch.model.FieldMappingConfig;
import com.truist.batch.model.YamlMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Adapter for JSON mapping and transformation operations.
 * 
 * Implements the Adapter pattern to provide clean separation of concerns between
 * JSON processing and business logic. This adapter handles all JSON-related
 * operations for the batch processing system following enterprise standards.
 * 
 * Key Responsibilities:
 * - JSON to internal model conversions
 * - Data transformation and validation
 * - JSON to YAML conversion operations
 * - Integration with existing mapping services
 * 
 * Enterprise Standards:
 * - Configuration-first approach
 * - Banking-grade error handling
 * - Comprehensive logging and audit trail
 * - Security-conscious data processing
 * 
 * Security Features:
 * - Input validation and sanitization
 * - Safe JSON parsing with proper exception handling
 * - No sensitive data exposure in logs
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Phase 3 - Manual Batch Execution Interface
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JsonMappingAdapter {

    private final ObjectMapper objectMapper;
    private final YAMLMapper yamlMapper;

    /**
     * Default constructor that initializes mappers with enterprise-grade configuration.
     */
    public JsonMappingAdapter() {
        this.objectMapper = new ObjectMapper();
        this.yamlMapper = new YAMLMapper();
        
        log.info("🔧 Initialized JsonMappingAdapter with enterprise configuration");
    }

    /**
     * Converts JSON string to FieldMappingConfig object.
     * 
     * @param jsonString JSON string containing field mapping configuration
     * @return FieldMappingConfig object
     * @throws JsonMappingException if JSON parsing fails
     */
    public FieldMappingConfig jsonToFieldMappingConfig(String jsonString) {
        log.debug("🔄 Converting JSON to FieldMappingConfig");
        
        try {
            if (jsonString == null || jsonString.trim().isEmpty()) {
                log.warn("⚠️ Empty JSON string provided for FieldMappingConfig conversion");
                throw new JsonMappingException("JSON string cannot be null or empty");
            }
            
            FieldMappingConfig config = objectMapper.readValue(jsonString, FieldMappingConfig.class);
            log.debug("✅ Successfully converted JSON to FieldMappingConfig");
            return config;
            
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to convert JSON to FieldMappingConfig: {}", e.getMessage());
            throw new JsonMappingException("Failed to parse JSON to FieldMappingConfig", e);
        }
    }

    /**
     * Converts FieldMappingConfig object to JSON string.
     * 
     * @param config FieldMappingConfig object to convert
     * @return JSON string representation
     * @throws JsonMappingException if serialization fails
     */
    public String fieldMappingConfigToJson(FieldMappingConfig config) {
        log.debug("🔄 Converting FieldMappingConfig to JSON");
        
        try {
            if (config == null) {
                log.warn("⚠️ Null FieldMappingConfig provided for JSON conversion");
                throw new JsonMappingException("FieldMappingConfig cannot be null");
            }
            
            String json = objectMapper.writeValueAsString(config);
            log.debug("✅ Successfully converted FieldMappingConfig to JSON");
            return json;
            
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to convert FieldMappingConfig to JSON: {}", e.getMessage());
            throw new JsonMappingException("Failed to serialize FieldMappingConfig to JSON", e);
        }
    }

    /**
     * Converts JSON string to YamlMapping object.
     * 
     * @param jsonString JSON string containing YAML mapping configuration
     * @return YamlMapping object
     * @throws JsonMappingException if JSON parsing fails
     */
    public YamlMapping jsonToYamlMapping(String jsonString) {
        log.debug("🔄 Converting JSON to YamlMapping");
        
        try {
            if (jsonString == null || jsonString.trim().isEmpty()) {
                log.warn("⚠️ Empty JSON string provided for YamlMapping conversion");
                throw new JsonMappingException("JSON string cannot be null or empty");
            }
            
            YamlMapping mapping = objectMapper.readValue(jsonString, YamlMapping.class);
            log.debug("✅ Successfully converted JSON to YamlMapping");
            return mapping;
            
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to convert JSON to YamlMapping: {}", e.getMessage());
            throw new JsonMappingException("Failed to parse JSON to YamlMapping", e);
        }
    }

    /**
     * Converts JSON string to YAML string format.
     * 
     * @param jsonString JSON string to convert
     * @return YAML string representation
     * @throws JsonMappingException if conversion fails
     */
    public String jsonToYaml(String jsonString) {
        log.debug("🔄 Converting JSON to YAML format");
        
        try {
            if (jsonString == null || jsonString.trim().isEmpty()) {
                log.warn("⚠️ Empty JSON string provided for YAML conversion");
                return "";
            }
            
            // Parse JSON first to validate structure
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            
            // Convert to YAML format
            String yaml = yamlMapper.writeValueAsString(jsonNode);
            log.debug("✅ Successfully converted JSON to YAML format");
            return yaml;
            
        } catch (IOException e) {
            log.error("❌ Failed to convert JSON to YAML: {}", e.getMessage());
            throw new JsonMappingException("Failed to convert JSON to YAML format", e);
        }
    }

    /**
     * Converts YAML string to JSON format.
     * 
     * @param yamlString YAML string to convert
     * @return JSON string representation
     * @throws JsonMappingException if conversion fails
     */
    public String yamlToJson(String yamlString) {
        log.debug("🔄 Converting YAML to JSON format");
        
        try {
            if (yamlString == null || yamlString.trim().isEmpty()) {
                log.warn("⚠️ Empty YAML string provided for JSON conversion");
                return "{}";
            }
            
            // Parse YAML first to validate structure
            JsonNode yamlNode = yamlMapper.readTree(yamlString);
            
            // Convert to JSON format
            String json = objectMapper.writeValueAsString(yamlNode);
            log.debug("✅ Successfully converted YAML to JSON format");
            return json;
            
        } catch (IOException e) {
            log.error("❌ Failed to convert YAML to JSON: {}", e.getMessage());
            throw new JsonMappingException("Failed to convert YAML to JSON format", e);
        }
    }

    /**
     * Validates JSON string structure and syntax.
     * 
     * @param jsonString JSON string to validate
     * @return true if valid JSON, false otherwise
     */
    public boolean isValidJson(String jsonString) {
        log.debug("🔄 Validating JSON structure");
        
        try {
            if (jsonString == null || jsonString.trim().isEmpty()) {
                return false;
            }
            
            objectMapper.readTree(jsonString);
            log.debug("✅ JSON validation successful");
            return true;
            
        } catch (JsonProcessingException e) {
            log.debug("❌ JSON validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Converts generic Map to JSON string with proper formatting.
     * 
     * @param dataMap Map containing data to convert
     * @return JSON string representation
     * @throws JsonMappingException if serialization fails
     */
    public String mapToJson(Map<String, Object> dataMap) {
        log.debug("🔄 Converting Map to JSON");
        
        try {
            if (dataMap == null) {
                log.warn("⚠️ Null Map provided for JSON conversion");
                return "{}";
            }
            
            String json = objectMapper.writeValueAsString(dataMap);
            log.debug("✅ Successfully converted Map to JSON");
            return json;
            
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to convert Map to JSON: {}", e.getMessage());
            throw new JsonMappingException("Failed to serialize Map to JSON", e);
        }
    }

    /**
     * Converts JSON string to generic Map for flexible data processing.
     * 
     * @param jsonString JSON string to convert
     * @return Map containing parsed JSON data
     * @throws JsonMappingException if parsing fails
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> jsonToMap(String jsonString) {
        log.debug("🔄 Converting JSON to Map");
        
        try {
            if (jsonString == null || jsonString.trim().isEmpty()) {
                log.warn("⚠️ Empty JSON string provided for Map conversion");
                return Map.of();
            }
            
            Map<String, Object> dataMap = objectMapper.readValue(jsonString, Map.class);
            log.debug("✅ Successfully converted JSON to Map");
            return dataMap;
            
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to convert JSON to Map: {}", e.getMessage());
            throw new JsonMappingException("Failed to parse JSON to Map", e);
        }
    }

    /**
     * Converts list of configurations to JSON array string.
     * 
     * @param configs List of FieldMappingConfig objects
     * @return JSON array string
     * @throws JsonMappingException if serialization fails
     */
    public String configListToJson(List<FieldMappingConfig> configs) {
        log.debug("🔄 Converting configuration list to JSON array");
        
        try {
            if (configs == null) {
                log.warn("⚠️ Null configuration list provided for JSON conversion");
                return "[]";
            }
            
            String json = objectMapper.writeValueAsString(configs);
            log.debug("✅ Successfully converted {} configurations to JSON array", configs.size());
            return json;
            
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to convert configuration list to JSON: {}", e.getMessage());
            throw new JsonMappingException("Failed to serialize configuration list to JSON", e);
        }
    }

    /**
     * Custom exception for JSON mapping operations.
     */
    public static class JsonMappingException extends RuntimeException {
        public JsonMappingException(String message) {
            super(message);
        }

        public JsonMappingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}