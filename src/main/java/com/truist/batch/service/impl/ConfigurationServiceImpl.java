package com.truist.batch.service.impl;

import com.truist.batch.entity.BatchConfigurationEntity;
import com.truist.batch.entity.ConfigurationAuditEntity;
import com.truist.batch.entity.SourceSystemEntity;
import com.truist.batch.repository.BatchConfigurationRepository;
import com.truist.batch.repository.ConfigurationAuditRepository;
import com.truist.batch.repository.SourceSystemRepository;
import com.truist.batch.service.ConfigurationService;
import com.truist.batch.service.YamlGenerationService;
import com.truist.batch.model.*;
import com.truist.batch.mapping.YamlMappingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced ConfigurationService implementation with YAML generation capabilities.
 * 
 * This service integrates with YamlGenerationService to convert configurations
 * into working YAML files that are compatible with the existing
 * GenericProcessor and Spring Batch framework.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationServiceImpl implements ConfigurationService {

    private final BatchConfigurationRepository configRepository;
    private final ConfigurationAuditRepository auditRepository;
    private final SourceSystemRepository sourceSystemRepository;
    private final YamlGenerationService yamlGenerationService;
    private final YamlMappingService yamlMappingService;
    private final ObjectMapper objectMapper;

    @Value("${batch.yaml.output.basePath:src/main/resources}")
    private String yamlOutputBasePath;

    @Value("${batch.yaml.backup.enabled:true}")
    private boolean backupEnabled;

    // ==================== ConfigurationService Interface Implementation ====================

    @Override
    public List<SourceSystem> getAllSourceSystems() {
        try {
            // TODO: Replace with actual database query when entities are ready
            return Arrays.asList(
                new SourceSystem("hr", "HR System", "ORACLE", "Human Resources", true, 2, LocalDateTime.now(), null),
                new SourceSystem("dda", "DDA System", "ORACLE", "Demand Deposit Accounts", true, 3, LocalDateTime.now(), null),
                new SourceSystem("shaw", "Shaw System", "ORACLE", "Shaw Cable Data", true, 1, LocalDateTime.now(), null)
            );
        } catch (Exception e) {
            log.error("❌ Failed to load source systems: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load source systems", e);
        }
    }

    @Override
    public SourceSystem getSourceSystem(String systemId) {
        try {
            Optional<SourceSystemEntity> entity = sourceSystemRepository.findById(systemId);
            if (entity.isPresent()) {
                SourceSystemEntity e = entity.get();
                
                // Convert entity to model with proper field mapping
                return new SourceSystem(
                    e.getId(),
                    e.getName(),
                    e.getType() != null ? e.getType().name() : "UNKNOWN", // Convert enum to string
                    e.getDescription(),
                    "Y".equals(e.getEnabled()), // Convert Y/N string to boolean
                    e.getJobCount() != null ? e.getJobCount() : 0, // Handle null jobCount
                    e.getCreatedDate(), // Use createdDate as lastModified
                    createConnectionPropertiesMap(e.getConnectionString()) // Convert connection string to map
                );
            }
            return null;
        } catch (Exception e) {
            log.error("❌ Failed to get source system {}: {}", systemId, e.getMessage(), e);
            throw new RuntimeException("Failed to get source system", e);
        }
    }

    @Override
    public List<JobConfig> getJobsForSystem(String systemId) {
        try {
            // TODO: Replace with actual database query when entities are ready
            List<JobConfig> jobs = new ArrayList<>();
            switch (systemId) {
                case "hr":
                    jobs.add(new JobConfig("hr-p327", systemId, "p327", "P327 HR Processing", 
                        "/data/hr", "/output/hr", "SELECT * FROM hr_staging", true, 
                        LocalDateTime.now(), Arrays.asList("default", "credit", "debit")));
                    jobs.add(new JobConfig("hr-p425", systemId, "p425", "P425 HR Processing", 
                        "/data/hr", "/output/hr", "SELECT * FROM hr_staging_p425", true, 
                        LocalDateTime.now(), Arrays.asList("default")));
                    break;
                case "dda":
                    jobs.add(new JobConfig("dda-p327", systemId, "p327", "P327 DDA Processing", 
                        "/data/dda", "/output/dda", "SELECT * FROM dda_staging", true, 
                        LocalDateTime.now(), Arrays.asList("default", "savings", "checking")));
                    break;
                case "shaw":
                    jobs.add(new JobConfig("shaw-p327", systemId, "p327", "P327 Shaw Processing", 
                        "/data/shaw", "/output/shaw", "SELECT * FROM shaw_staging", true, 
                        LocalDateTime.now(), Arrays.asList("default")));
                    break;
            }
            return jobs;
        } catch (Exception e) {
            log.error("❌ Failed to load jobs for system {}: {}", systemId, e.getMessage(), e);
            throw new RuntimeException("Failed to load jobs for system", e);
        }
    }

    @Override
    public List<SourceField> getSourceFields(String systemId, String jobName) {
        try {
            // TODO: Replace with actual database query when entities are ready
            List<SourceField> fields = new ArrayList<>();
            
            // Fixed constructor calls to match SourceField(name, dataType, description, nullable, maxLength, sourceTable, sourceColumn)
            fields.add(new SourceField("acct_num", "STRING", "Account number", false, 18, "staging_table", "acct_num"));
            fields.add(new SourceField("location_code", "STRING", "Location code", true, 6, "staging_table", "location_code"));
            fields.add(new SourceField("balance_amt", "NUMBER", "Balance amount", true, 12, "staging_table", "balance_amt"));
            fields.add(new SourceField("last_payment_date", "DATE", "Last payment date", true, 8, "staging_table", "last_payment_date"));
            fields.add(new SourceField("status_code", "STRING", "Account status", true, 2, "staging_table", "status_code"));
            
            return fields;
        } catch (Exception e) {
            log.error("❌ Failed to load source fields for {}/{}: {}", systemId, jobName, e.getMessage(), e);
            throw new RuntimeException("Failed to load source fields", e);
        }
    }

    @Override
    public FieldMappingConfig getFieldMappings(String sourceSystem, String jobName) {
        return getFieldMappings(sourceSystem, jobName, "default");
    }

    @Override
    public FieldMappingConfig getFieldMappings(String sourceSystem, String jobName, String transactionType) {
        try {
            List<BatchConfigurationEntity> entities = 
                configRepository.findBySourceSystemAndJobName(sourceSystem, jobName);
            
            if (!entities.isEmpty()) {
                // Get the first matching entity (or could add additional filtering by transactionType)
                BatchConfigurationEntity entity = entities.get(0);
                return objectMapper.readValue(entity.getConfigurationJson(), FieldMappingConfig.class);
            }
            
            // Return empty configuration if not found
            FieldMappingConfig config = new FieldMappingConfig();
            config.setSourceSystem(sourceSystem);
            config.setJobName(jobName);
            config.setTransactionType(transactionType);
            config.setFieldMappings(new ArrayList<>());
            config.setLastModified(LocalDateTime.now());
            config.setVersion(1);
            
            return config;
            
        } catch (Exception e) {
            log.error("❌ Failed to load field mappings for {}/{}: {}", sourceSystem, jobName, e.getMessage(), e);
            throw new RuntimeException("Failed to load field mappings", e);
        }
    }

    @Override
    @Transactional
    public String saveConfiguration(FieldMappingConfig config) {
        log.info("💾 Saving configuration for {}/{}", config.getSourceSystem(), config.getJobName());

        try {
            // 1. Validate configuration before saving
            ValidationResult validation = validateConfiguration(config);
            if (!validation.isValid()) {
                throw new IllegalArgumentException("Configuration validation failed: " + 
                    String.join(", ", validation.getErrors()));
            }

            // 2. Save to database with audit trail
            BatchConfigurationEntity entity = saveToDatabase(config, "system");

            // 3. Generate and save YAML file
            String yamlContent = yamlGenerationService.generateYamlFromConfiguration(config);
            String yamlFilePath = saveYamlToFile(config, yamlContent);

            // 4. Create audit entry
            createAuditEntry(entity.getId(), "SAVE", null, 
                objectMapper.writeValueAsString(config), "system");

            log.info("✅ Configuration saved successfully: DB ID={}, YAML Path={}", 
                    entity.getId(), yamlFilePath);

            return "Configuration saved successfully";

        } catch (Exception e) {
            log.error("❌ Failed to save configuration: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save configuration", e);
        }
    }

    @Override
    public ValidationResult validateConfiguration(FieldMappingConfig config) {
        log.debug("🔍 Validating configuration for {}/{}", 
                config.getSourceSystem(), config.getJobName());

        // Use YamlGenerationService validation
        ValidationResult yamlValidation = yamlGenerationService.validateForYamlGeneration(config);

        // Additional business logic validation
        validateBusinessRules(config, yamlValidation);

        log.debug("✅ Validation complete: {} errors", yamlValidation.getErrors().size());
        return yamlValidation;
    }

    @Override
    public String generateYaml(FieldMappingConfig config) {
        log.info("📄 Generating YAML for {}/{}", config.getSourceSystem(), config.getJobName());

        try {
            String yamlContent = yamlGenerationService.generateYamlFromConfiguration(config);
            log.debug("✅ YAML generated successfully, {} characters", yamlContent.length());
            return yamlContent;

        } catch (Exception e) {
            log.error("❌ Failed to generate YAML: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate YAML", e);
        }
    }

    @Override
    public List<String> generatePreview(FieldMappingConfig config, List<Map<String, Object>> sampleData) {
        log.info("👁️ Generating preview for {}/{} with {} sample records", 
                config.getSourceSystem(), config.getJobName(), sampleData.size());

        try {
            List<String> previewLines = new ArrayList<>();
            
            for (Map<String, Object> sampleRow : sampleData) {
                StringBuilder line = new StringBuilder();
                
                // Sort fields by target position and build output line
                config.getFieldMappings().stream()
                    .sorted(Comparator.comparingInt(FieldMapping::getTargetPosition))
                    .forEach(field -> {
                        String value = transformFieldForPreview(sampleRow, field);
                        line.append(padValue(value, field.getLength(), field.getPad()));
                    });
                
                previewLines.add(line.toString());
            }
            
            log.info("✅ Preview generated: {} lines", previewLines.size());
            return previewLines;

        } catch (Exception e) {
            log.error("❌ Failed to generate preview: {}", e.getMessage(), e);
            return Arrays.asList("Error generating preview: " + e.getMessage());
        }
    }

    @Override
    public TestResult testConfiguration(String sourceSystem, String jobName) {
        log.info("🧪 Testing configuration for {}/{}", sourceSystem, jobName);

        try {
            // 1. Load configuration
            FieldMappingConfig config = getFieldMappings(sourceSystem, jobName);

            // 2. Validate configuration
            ValidationResult validation = validateConfiguration(config);
            if (!validation.isValid()) {
                return new TestResult(false, "Configuration validation failed: " + 
                    String.join(", ", validation.getErrors()), Arrays.asList(), 0);
            }

            // 3. Generate YAML and test file generation
            long startTime = System.currentTimeMillis();
            String yamlContent = generateYaml(config);
            long duration = System.currentTimeMillis() - startTime;

            // 4. Test with sample data
            List<String> testOutput = generateSampleOutput(config);

            return new TestResult(true, "Test completed successfully", testOutput, duration);

        } catch (Exception e) {
            log.error("❌ Configuration test failed: {}", e.getMessage(), e);
            return new TestResult(false, "Test failed: " + e.getMessage(), 
                Arrays.asList(), 0);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Convert connection string to properties map
     */
    private Map<String, String> createConnectionPropertiesMap(String connectionString) {
        Map<String, String> properties = new HashMap<>();
        if (connectionString != null && !connectionString.trim().isEmpty()) {
            properties.put("connectionString", connectionString);
        }
        return properties;
    }

    private BatchConfigurationEntity saveToDatabase(FieldMappingConfig config, String userId) throws Exception {
        BatchConfigurationEntity entity = new BatchConfigurationEntity();
        entity.setId(generateConfigId(config));
        entity.setSourceSystem(config.getSourceSystem());
        entity.setJobName(config.getJobName());
        entity.setConfigurationJson(objectMapper.writeValueAsString(config));
        entity.setCreatedBy(userId);
        entity.setCreatedDate(LocalDateTime.now());
        entity.setVersion(1);

        return configRepository.save(entity);
    }

    private String saveYamlToFile(FieldMappingConfig config, String yamlContent) throws IOException {
        // Create directory structure: {basePath}/{fileType}/{sourceSystem}/
        String fileType = config.getJobName(); // jobName becomes fileType in YAML
        Path yamlDir = Paths.get(yamlOutputBasePath, fileType, config.getSourceSystem());
        Files.createDirectories(yamlDir);

        // Generate file name: {fileType}.yml
        Path yamlFile = yamlDir.resolve(fileType + ".yml");

        // Write YAML content to file
        Files.write(yamlFile, yamlContent.getBytes(), 
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        log.info("📁 YAML file saved: {}", yamlFile.toAbsolutePath());
        return yamlFile.toAbsolutePath().toString();
    }

    private void createAuditEntry(String configId, String action, String oldValue, 
                                 String newValue, String userId) {
        ConfigurationAuditEntity audit = new ConfigurationAuditEntity();
        audit.setConfigId(configId);
        audit.setAction(action);
        audit.setOldValue(oldValue);
        audit.setNewValue(newValue);
        audit.setChangedBy(userId);
        audit.setChangeDate(LocalDateTime.now());

        auditRepository.save(audit);
    }

    private String generateConfigId(FieldMappingConfig config) {
        return config.getSourceSystem() + "_" + config.getJobName() + "_" + 
               (config.getTransactionType() != null ? config.getTransactionType() : "default");
    }

    private void validateBusinessRules(FieldMappingConfig config, ValidationResult result) {
        // Add business-specific validation rules here
        
        // Example: Validate total record length doesn't exceed limits
        int totalLength = config.getFieldMappings().stream()
            .mapToInt(FieldMapping::getLength)
            .sum();
        
        if (totalLength > 32000) { // Example limit
            result.addError("Total record length exceeds maximum: " + totalLength);
        }
    }

    private String transformFieldForPreview(Map<String, Object> row, FieldMapping field) {
        // Simplified transformation for preview
        switch (field.getTransformationType()) {
            case "constant":
                return field.getValue() != null ? field.getValue() : field.getDefaultValue();
                
            case "source":
                Object value = row.get(field.getSourceField());
                return value != null ? value.toString() : field.getDefaultValue();
                
            case "composite":
                // Simplified composite handling
                return field.getDefaultValue() != null ? field.getDefaultValue() : "";
                
            case "conditional":
                // Simplified conditional handling
                return field.getDefaultValue() != null ? field.getDefaultValue() : "";
                
            default:
                return field.getDefaultValue() != null ? field.getDefaultValue() : "";
        }
    }

    private String padValue(String value, int length, String pad) {
        if (value == null) value = "";
        
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        
        if ("left".equals(pad)) {
            return String.format("%" + length + "s", value);
        } else {
            return String.format("%-" + length + "s", value);
        }
    }

    private List<String> generateSampleOutput(FieldMappingConfig config) {
        // Generate sample output lines for testing
        Map<String, Object> sampleRow = createSampleDataRow(config);
        return generatePreview(config, Arrays.asList(sampleRow));
    }

    private Map<String, Object> createSampleDataRow(FieldMappingConfig config) {
        Map<String, Object> sampleRow = new HashMap<>();
        
        // Create sample data based on source fields used in the configuration
        config.getFieldMappings().stream()
            .filter(field -> "source".equals(field.getTransformationType()))
            .forEach(field -> {
                String sourceField = field.getSourceField();
                if (sourceField != null) {
                    // Generate sample data based on data type
                    switch (field.getDataType().toLowerCase()) {
                        case "numeric":
                        case "number":
                            sampleRow.put(sourceField, "12345");
                            break;
                        case "date":
                            sampleRow.put(sourceField, "20250701");
                            break;
                        default:
                            sampleRow.put(sourceField, "SAMPLE_" + sourceField.toUpperCase());
                            break;
                    }
                }
            });
        
        return sampleRow;
    }
    
    
    @Override
    public List<FieldMappingConfig> getAllTransactionTypesForJob(String sourceSystem, String jobName) {
        log.info("📋 Loading all transaction types for {}/{}", sourceSystem, jobName);
        
        try {
            // Query database for all configurations matching source system and job name
            List<BatchConfigurationEntity> entities = configRepository.findBySourceSystemAndJobName(sourceSystem, jobName);
            
            if (entities.isEmpty()) {
                log.warn("⚠️ No configurations found in database for {}/{}", sourceSystem, jobName);
                return List.of();
            }
            
            // Convert entities to FieldMappingConfig objects
            List<FieldMappingConfig> configs = entities.stream()
                .map(this::convertEntityToConfig)
                .collect(Collectors.toList());
            
            log.info("✅ Found {} transaction types for {}/{}: {}", 
                    configs.size(), sourceSystem, jobName,
                    configs.stream().map(FieldMappingConfig::getTransactionType).collect(Collectors.toList()));
            
            return configs;
            
        } catch (Exception e) {
            log.error("❌ Failed to load transaction types for {}/{}: {}", sourceSystem, jobName, e.getMessage(), e);
            throw new RuntimeException("Failed to load transaction types", e);
        }
    }

    /**
     * Converts BatchConfigurationEntity to FieldMappingConfig.
     */
    private FieldMappingConfig convertEntityToConfig(BatchConfigurationEntity entity) {
        try {
            // Parse JSON configuration from entity
            FieldMappingConfig config = objectMapper.readValue(entity.getConfigurationJson(), FieldMappingConfig.class);
            
            // Set metadata from entity
            config.setId(entity.getId());
            config.setSourceSystem(entity.getSourceSystem());
            config.setJobName(entity.getJobName());
            config.setTransactionType(entity.getTransactionType());
            config.setCreatedDate(entity.getCreatedDate());
            config.setLastModified(entity.getModifiedDate());
            
            return config;
            
        } catch (Exception e) {
            log.error("❌ Failed to convert entity to config for ID {}: {}", entity.getId(), e.getMessage());
            throw new RuntimeException("Failed to convert entity to configuration", e);
        }
    }
}