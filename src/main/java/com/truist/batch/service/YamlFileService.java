package com.truist.batch.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.truist.batch.model.FieldMappingConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for generating and saving YAML configuration files to the batch-sources directory structure.
 * 
 * This service handles:
 * 1. Field mapping YAML files (mappings/{jobName}/{sourceSystem}/{jobName}.yml)
 * 2. Batch properties YAML files ({sourceSystem}-batch-props.yml) 
 * 3. Directory structure creation and file backup
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YamlFileService {
    
    private final YamlGenerationService yamlGenerationService;
    private final YAMLMapper yamlMapper = new YAMLMapper();
    
    @Value("${batch.yaml.output.directory:src/main/resources/batch-sources}")
    private String outputDirectory;
    
    @Value("${batch.yaml.backup.enabled:true}")
    private boolean backupEnabled;

    /**
     * Saves complete configuration files for a source system and job.
     * 
     * @param sourceSystem Source system (e.g., "hr", "dda", "shaw")
     * @param jobName Job name (e.g., "p327", "atoctran")
     * @param configs List of configurations for different transaction types
     * @return Map containing saved file paths
     */
    public Map<String, String> saveConfigurationFiles(String sourceSystem, String jobName, 
                                                     List<FieldMappingConfig> configs) {
        log.info("🔄 Saving configuration files for {}/{} with {} transaction types", 
                sourceSystem, jobName, configs.size());
        
        try {
            Map<String, String> savedFiles = new LinkedHashMap<>();
            
            // 1. Save field mapping YAML file
            String mappingFilePath = saveFieldMappingYaml(sourceSystem, jobName, configs);
            savedFiles.put("fieldMapping", mappingFilePath);
            
            // 2. Update/create batch-props.yml file  
            String batchPropsFilePath = updateBatchPropsFile(sourceSystem, jobName, configs);
            savedFiles.put("batchProps", batchPropsFilePath);
            
            log.info("✅ Successfully saved configuration files for {}/{}", sourceSystem, jobName);
            return savedFiles;
            
        } catch (Exception e) {
            log.error("❌ Failed to save configuration files for {}/{}: {}", sourceSystem, jobName, e.getMessage(), e);
            throw new RuntimeException("Failed to save configuration files", e);
        }
    }
    
    /**
     * Saves field mapping YAML file in the correct directory structure.
     * Path: mappings/{jobName}/{sourceSystem}/{jobName}.yml
     */
    private String saveFieldMappingYaml(String sourceSystem, String jobName, 
                                       List<FieldMappingConfig> configs) throws IOException {
        
        // Generate multi-document YAML content
        String yamlContent = yamlGenerationService.generateMultiDocumentYaml(configs);
        
        // Build file path: mappings/{jobName}/{sourceSystem}/{jobName}.yml
        Path mappingPath = Paths.get(outputDirectory, "mappings", jobName, sourceSystem, jobName + ".yml");
        
        // Create directories if they don't exist
        Files.createDirectories(mappingPath.getParent());
        
        // Backup existing file if enabled
        if (backupEnabled && Files.exists(mappingPath)) {
            backupExistingFile(mappingPath);
        }
        
        // Write YAML content to file
        Files.write(mappingPath, yamlContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        String relativePath = "mappings/" + jobName + "/" + sourceSystem + "/" + jobName + ".yml";
        log.info("✅ Saved field mapping YAML: {}", relativePath);
        
        return relativePath;
    }
    
    /**
     * Updates or creates the batch-props.yml file for the source system.
     * Path: {sourceSystem}-batch-props.yml
     */
    private String updateBatchPropsFile(String sourceSystem, String jobName, 
                                       List<FieldMappingConfig> configs) throws IOException {
        
        Path batchPropsPath = Paths.get(outputDirectory, sourceSystem + "-batch-props.yml");
        
        // Load existing batch-props or create new structure
        Map<String, Object> batchProps = loadOrCreateBatchProps(batchPropsPath, sourceSystem);
        
        // Generate job configuration section
        Map<String, Object> jobConfig = generateJobConfig(jobName, configs);
        
        // Navigate to the jobs section with proper casting
        @SuppressWarnings("unchecked")
        Map<String, Object> batch = (Map<String, Object>) batchProps.get("batch");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> sources = (Map<String, Object>) batch.get("sources");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> sourceConfig = (Map<String, Object>) sources.get(sourceSystem);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> jobs = (Map<String, Object>) sourceConfig.computeIfAbsent("jobs", k -> new LinkedHashMap<>());
        
        jobs.put(jobName, jobConfig);
        
        // Backup existing file if enabled
        if (backupEnabled && Files.exists(batchPropsPath)) {
            backupExistingFile(batchPropsPath);
        }
        
        // Write updated batch-props.yml
        String yamlContent = yamlMapper.writeValueAsString(batchProps);
        Files.write(batchPropsPath, yamlContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        String relativePath = sourceSystem + "-batch-props.yml";
        log.info("✅ Updated batch-props YAML: {}", relativePath);
        
        return relativePath;
    }
    
    /**
     * Loads existing batch-props.yml or creates a new structure.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadOrCreateBatchProps(Path batchPropsPath, String sourceSystem) throws IOException {
        if (Files.exists(batchPropsPath)) {
            // Load existing file
            try {
                String content = Files.readString(batchPropsPath);
                return yamlMapper.readValue(content, Map.class);
            } catch (Exception e) {
                log.warn("⚠️ Failed to parse existing batch-props.yml, creating new structure: {}", e.getMessage());
            }
        }
        
        // Create new batch-props structure
        Map<String, Object> batchProps = new LinkedHashMap<>();
        Map<String, Object> batch = new LinkedHashMap<>();
        Map<String, Object> sources = new LinkedHashMap<>();
        Map<String, Object> sourceConfig = new LinkedHashMap<>();
        
        // Basic source system configuration
        sourceConfig.put("inputBasePath", "${batch.defaults.inputBasePath}/" + sourceSystem);
        sourceConfig.put("outputBasePath", "${batch.defaults.outputBasePath}/" + sourceSystem);
        sourceConfig.put("stagingSchema", "${batch.defaults.stagingSchema}");
        sourceConfig.put("destSchema", "${batch.defaults.destSchema}");
        sourceConfig.put("jobs", new LinkedHashMap<>());
        
        sources.put(sourceSystem, sourceConfig);
        batch.put("sources", sources);
        batchProps.put("batch", batch);
        
        return batchProps;
    }
    
    /**
     * Generates job configuration section for batch-props.yml.
     */
    private Map<String, Object> generateJobConfig(String jobName, List<FieldMappingConfig> configs) {
        Map<String, Object> jobConfig = new LinkedHashMap<>();
        
        // Create files array for different transaction types
        List<Map<String, Object>> files = configs.stream().map(config -> {
            Map<String, Object> fileConfig = new LinkedHashMap<>();
            
            // Add transaction type if not default
            if (config.getTransactionType() != null && !"default".equals(config.getTransactionType())) {
                fileConfig.put("transactionType", config.getTransactionType());
            }
            
            // Add template reference
            fileConfig.put("template", jobName);
            
            // Add target table
            fileConfig.put("target", "${batch.defaults.stagingSchema}." + config.getSourceSystem().toUpperCase() + "_MASTER");
            
            // Add parameters
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("format", "jdbc");
            params.put("batchDateParam", "${batch.defaults.batchDateParam}");
            params.put("batchDateValue", "${batch.defaults.batchDateValue}");
            
            // Generate output path
            String outputPath = "${batch.defaults.outputBasePath}/" + config.getSourceSystem() + "/" + jobName;
            if (config.getTransactionType() != null && !"default".equals(config.getTransactionType())) {
                outputPath += "_" + config.getTransactionType();
            }
            outputPath += "_${DATE}.txt";
            params.put("outputPath", outputPath);
            
            params.put("fetchSize", "500");
            params.put("pageSize", "1000");
            
            // Generate sample query
            String query = generateSampleQuery(config);
            params.put("query", query);
            
            fileConfig.put("params", params);
            
            return fileConfig;
        }).collect(Collectors.toList());
        
        jobConfig.put("files", files);
        return jobConfig;
    }
    
    /**
     * Generates a sample SQL query for the configuration.
     */
    private String generateSampleQuery(FieldMappingConfig config) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        
        // Add source fields that are used in mappings
        List<String> sourceFields = config.getFieldMappings().stream()
            .filter(mapping -> mapping.getSourceField() != null && !mapping.getSourceField().isEmpty())
            .map(mapping -> mapping.getSourceField())
            .distinct()
            .collect(Collectors.toList());
        
        if (sourceFields.isEmpty()) {
            query.append("*");
        } else {
            query.append(String.join(", ", sourceFields));
        }
        
        query.append(" FROM ${batch.defaults.stagingSchema}.").append(config.getSourceSystem().toUpperCase()).append("_MASTER t ");
        query.append("WHERE t.batch_date = (SELECT MAX(batch_date) FROM ${batch.defaults.stagingSchema}.batch_date_locator)");
        
        // Add transaction type filter if applicable
        if (config.getTransactionType() != null && !"default".equals(config.getTransactionType())) {
            query.append(" AND t.transaction_type = '").append(config.getTransactionType()).append("'");
        }
        
        return query.toString();
    }
    
    /**
     * Creates a backup of an existing file with timestamp.
     */
    private void backupExistingFile(Path filePath) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = filePath.getFileName().toString();
        String backupFileName = fileName.replaceFirst("\\.", "_backup_" + timestamp + ".");
        
        Path backupPath = filePath.getParent().resolve(backupFileName);
        Files.copy(filePath, backupPath);
        
        log.info("📁 Created backup: {}", backupPath.getFileName());
    }
    
    /**
     * Validates that the generated files can be read by the existing YamlMappingService.
     */
    public boolean validateGeneratedFiles(String sourceSystem, String jobName) {
        try {
            Path mappingPath = Paths.get(outputDirectory, "mappings", jobName, sourceSystem, jobName + ".yml");
            Path batchPropsPath = Paths.get(outputDirectory, sourceSystem + "-batch-props.yml");
            
            // Check if files exist
            if (!Files.exists(mappingPath) || !Files.exists(batchPropsPath)) {
                log.error("❌ Generated files not found: mapping={}, batchProps={}", 
                         Files.exists(mappingPath), Files.exists(batchPropsPath));
                return false;
            }
            
            // Validate YAML syntax
            String mappingContent = Files.readString(mappingPath);
            yamlMapper.readTree(mappingContent);
            
            String batchPropsContent = Files.readString(batchPropsPath);
            yamlMapper.readTree(batchPropsContent);
            
            log.info("✅ Validated generated files for {}/{}", sourceSystem, jobName);
            return true;
            
        } catch (Exception e) {
            log.error("❌ File validation failed for {}/{}: {}", sourceSystem, jobName, e.getMessage());
            return false;
        }
    }
}