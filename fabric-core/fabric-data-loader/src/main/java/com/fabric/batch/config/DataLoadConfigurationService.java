package com.fabric.batch.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fabric.batch.entity.DataLoadConfigEntity;
import com.fabric.batch.entity.ValidationRuleEntity;
import com.fabric.batch.repository.DataLoadConfigRepository;
import com.fabric.batch.repository.ValidationRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing data loading configurations with database-first approach and JSON fallback.
 * Implements the user's requirement: "Lets use database with JSON fallback"
 */
@Slf4j
@Service
public class DataLoadConfigurationService {
    
    @Autowired
    private DataLoadConfigRepository configRepository;
    
    @Autowired
    private ValidationRuleRepository validationRuleRepository;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${data-loader.config.fallback.enabled:true}")
    private boolean fallbackEnabled;
    
    @Value("${data-loader.config.fallback.file:classpath:config/data-loader-configs.json}")
    private String fallbackConfigFile;
    
    // Cache for JSON fallback configurations
    private Map<String, DataLoadConfigEntity> fallbackConfigs = new HashMap<>();
    private Map<String, List<ValidationRuleEntity>> fallbackValidationRules = new HashMap<>();
    
    @PostConstruct
    public void initialize() {
        if (fallbackEnabled) {
            loadFallbackConfigurations();
        }
    }
    
    /**
     * Get data load configuration by ID.
     * First tries database, then falls back to JSON if not found.
     */
    public Optional<DataLoadConfigEntity> getConfiguration(String configId) {
        try {
            log.debug("Looking up configuration for ID: {}", configId);
            
            // Primary: Try database first
            Optional<DataLoadConfigEntity> dbConfig = configRepository.findByConfigIdAndEnabledTrue(configId);
            if (dbConfig.isPresent()) {
                log.debug("Found configuration in database: {}", configId);
                return dbConfig;
            }
            
            // Fallback: Try JSON configuration
            if (fallbackEnabled && fallbackConfigs.containsKey(configId)) {
                log.debug("Found configuration in JSON fallback: {}", configId);
                return Optional.of(fallbackConfigs.get(configId));
            }
            
            log.warn("Configuration not found in database or JSON fallback: {}", configId);
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Error retrieving configuration {}: {}", configId, e.getMessage());
            
            // On database error, try JSON fallback
            if (fallbackEnabled && fallbackConfigs.containsKey(configId)) {
                log.info("Database error - using JSON fallback for configuration: {}", configId);
                return Optional.of(fallbackConfigs.get(configId));
            }
            
            return Optional.empty();
        }
    }
    
    /**
     * Get validation rules for a configuration.
     * First tries database, then falls back to JSON if not found.
     */
    public List<ValidationRuleEntity> getValidationRules(String configId) {
        try {
            log.debug("Looking up validation rules for config: {}", configId);
            
            // Primary: Try database first
            List<ValidationRuleEntity> dbRules = validationRuleRepository.findByConfigIdAndEnabledTrueOrderByExecutionOrder(configId);
            if (!dbRules.isEmpty()) {
                log.debug("Found {} validation rules in database for config: {}", dbRules.size(), configId);
                return dbRules;
            }
            
            // Fallback: Try JSON configuration
            if (fallbackEnabled && fallbackValidationRules.containsKey(configId)) {
                List<ValidationRuleEntity> jsonRules = fallbackValidationRules.get(configId);
                log.debug("Found {} validation rules in JSON fallback for config: {}", jsonRules.size(), configId);
                return jsonRules;
            }
            
            log.warn("No validation rules found in database or JSON fallback for config: {}", configId);
            return List.of();
            
        } catch (Exception e) {
            log.error("Error retrieving validation rules for config {}: {}", configId, e.getMessage());
            
            // On database error, try JSON fallback
            if (fallbackEnabled && fallbackValidationRules.containsKey(configId)) {
                log.info("Database error - using JSON fallback for validation rules: {}", configId);
                return fallbackValidationRules.get(configId);
            }
            
            return List.of();
        }
    }
    
    /**
     * Get all enabled configurations.
     */
    public List<DataLoadConfigEntity> getAllEnabledConfigurations() {
        try {
            // Primary: Try database first
            List<DataLoadConfigEntity> dbConfigs = configRepository.findByEnabledTrueOrderByJobName();
            
            if (!dbConfigs.isEmpty()) {
                log.debug("Found {} configurations in database", dbConfigs.size());
                return dbConfigs;
            }
            
            // Fallback: Return JSON configurations
            if (fallbackEnabled && !fallbackConfigs.isEmpty()) {
                List<DataLoadConfigEntity> jsonConfigs = fallbackConfigs.values().stream()
                    .filter(config -> "Y".equals(config.getEnabled()))
                    .toList();
                log.debug("Found {} configurations in JSON fallback", jsonConfigs.size());
                return jsonConfigs;
            }
            
            return List.of();
            
        } catch (Exception e) {
            log.error("Error retrieving all configurations: {}", e.getMessage());
            
            // On database error, return JSON fallback
            if (fallbackEnabled && !fallbackConfigs.isEmpty()) {
                log.info("Database error - using JSON fallback for all configurations");
                return fallbackConfigs.values().stream()
                    .filter(config -> "Y".equals(config.getEnabled()))
                    .toList();
            }
            
            return List.of();
        }
    }
    
    /**
     * Save or update configuration in database.
     */
    public DataLoadConfigEntity saveConfiguration(DataLoadConfigEntity config) {
        try {
            log.info("Saving configuration: {}", config.getConfigId());
            return configRepository.save(config);
        } catch (Exception e) {
            log.error("Error saving configuration {}: {}", config.getConfigId(), e.getMessage());
            throw new RuntimeException("Failed to save configuration", e);
        }
    }
    
    /**
     * Save or update validation rule in database.
     */
    public ValidationRuleEntity saveValidationRule(ValidationRuleEntity rule) {
        try {
            log.info("Saving validation rule: {} for config: {}", rule.getRuleId(), rule.getConfigId());
            return validationRuleRepository.save(rule);
        } catch (Exception e) {
            log.error("Error saving validation rule for config {}: {}", rule.getConfigId(), e.getMessage());
            throw new RuntimeException("Failed to save validation rule", e);
        }
    }
    
    /**
     * Check if database is available.
     */
    public boolean isDatabaseAvailable() {
        try {
            configRepository.count();
            return true;
        } catch (Exception e) {
            log.warn("Database is not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get configuration source (DATABASE or JSON_FALLBACK).
     */
    public String getConfigurationSource(String configId) {
        try {
            if (configRepository.findByConfigIdAndEnabledTrue(configId).isPresent()) {
                return "DATABASE";
            } else if (fallbackEnabled && fallbackConfigs.containsKey(configId)) {
                return "JSON_FALLBACK";
            }
            return "NOT_FOUND";
        } catch (Exception e) {
            if (fallbackEnabled && fallbackConfigs.containsKey(configId)) {
                return "JSON_FALLBACK";
            }
            return "ERROR";
        }
    }
    
    /**
     * Reload JSON fallback configurations.
     */
    public void reloadFallbackConfigurations() {
        if (fallbackEnabled) {
            log.info("Reloading JSON fallback configurations");
            loadFallbackConfigurations();
        }
    }
    
    /**
     * Load configurations from JSON fallback file.
     */
    private void loadFallbackConfigurations() {
        try {
            log.info("Loading JSON fallback configurations from: {}", fallbackConfigFile);
            
            Resource resource = resourceLoader.getResource(fallbackConfigFile);
            if (!resource.exists()) {
                log.warn("JSON fallback configuration file not found: {}", fallbackConfigFile);
                return;
            }
            
            try (InputStream inputStream = resource.getInputStream()) {
                JsonNode rootNode = objectMapper.readTree(inputStream);
                
                // Parse configurations
                if (rootNode.has("configurations")) {
                    JsonNode configsNode = rootNode.get("configurations");
                    for (JsonNode configNode : configsNode) {
                        DataLoadConfigEntity config = parseConfigurationFromJson(configNode);
                        fallbackConfigs.put(config.getConfigId(), config);
                        log.debug("Loaded fallback configuration: {}", config.getConfigId());
                    }
                }
                
                // Parse validation rules
                if (rootNode.has("validationRules")) {
                    JsonNode rulesNode = rootNode.get("validationRules");
                    for (JsonNode ruleNode : rulesNode) {
                        ValidationRuleEntity rule = parseValidationRuleFromJson(ruleNode);
                        fallbackValidationRules.computeIfAbsent(rule.getConfigId(), k -> List.of()).add(rule);
                        log.debug("Loaded fallback validation rule for config: {}", rule.getConfigId());
                    }
                }
                
                log.info("Loaded {} configurations and {} validation rule sets from JSON fallback", 
                        fallbackConfigs.size(), fallbackValidationRules.size());
                
            }
        } catch (Exception e) {
            log.error("Error loading JSON fallback configurations: {}", e.getMessage());
            fallbackConfigs.clear();
            fallbackValidationRules.clear();
        }
    }
    
    /**
     * Parse configuration from JSON node.
     */
    private DataLoadConfigEntity parseConfigurationFromJson(JsonNode configNode) {
        DataLoadConfigEntity config = new DataLoadConfigEntity();
        
        config.setConfigId(configNode.get("configId").asText());
        config.setJobName(configNode.get("jobName").asText());
        config.setSourceSystem(configNode.get("sourceSystem").asText());
        config.setTargetTable(configNode.get("targetTable").asText());
        config.setFileType(DataLoadConfigEntity.FileType.valueOf(
            configNode.path("fileType").asText("PIPE_DELIMITED")));
        config.setFilePathPattern(configNode.path("filePathPattern").asText());
        config.setControlFileTemplate(configNode.path("controlFileTemplate").asText());
        config.setFieldDelimiter(configNode.path("fieldDelimiter").asText("|"));
        config.setMaxErrors(configNode.path("maxErrors").asInt(1000));
        config.setValidationEnabled(configNode.path("validationEnabled").asText("Y"));
        config.setPreLoadValidation(configNode.path("preLoadValidation").asText("Y"));
        config.setPostLoadValidation(configNode.path("postLoadValidation").asText("Y"));
        config.setEnabled(configNode.path("enabled").asText("Y"));
        config.setCreatedBy("JSON_FALLBACK");
        
        return config;
    }
    
    /**
     * Parse validation rule from JSON node.
     */
    private ValidationRuleEntity parseValidationRuleFromJson(JsonNode ruleNode) {
        ValidationRuleEntity rule = new ValidationRuleEntity();
        
        rule.setConfigId(ruleNode.get("configId").asText());
        rule.setFieldName(ruleNode.get("fieldName").asText());
        rule.setRuleType(ValidationRuleEntity.RuleType.valueOf(ruleNode.get("ruleType").asText()));
        rule.setDataType(ruleNode.path("dataType").asText());
        rule.setMaxLength(ruleNode.path("maxLength").asInt());
        rule.setMinLength(ruleNode.path("minLength").asInt());
        rule.setRequiredField(ruleNode.path("requiredField").asText("N"));
        rule.setErrorMessage(ruleNode.path("errorMessage").asText());
        rule.setExecutionOrder(ruleNode.path("executionOrder").asInt(1));
        rule.setEnabled(ruleNode.path("enabled").asText("Y"));
        rule.setCreatedBy("JSON_FALLBACK");
        
        return rule;
    }
    
    /**
     * Get configuration statistics.
     */
    public Map<String, Object> getConfigurationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            long dbConfigCount = configRepository.countByEnabledTrue();
            stats.put("databaseConfigurations", dbConfigCount);
        } catch (Exception e) {
            stats.put("databaseConfigurations", "ERROR");
        }
        
        stats.put("jsonFallbackConfigurations", fallbackConfigs.size());
        stats.put("fallbackEnabled", fallbackEnabled);
        stats.put("databaseAvailable", isDatabaseAvailable());
        stats.put("fallbackConfigFile", fallbackConfigFile);
        
        return stats;
    }
}