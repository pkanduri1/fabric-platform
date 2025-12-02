package com.fabric.batch.integration;

import com.fabric.batch.mapping.YamlMappingService;
import com.fabric.batch.model.FieldMapping;
import com.fabric.batch.model.YamlMapping;
import com.fabric.batch.model.FieldMappingConfig;
import com.fabric.batch.model.ValidationResult;
import com.fabric.batch.service.YamlGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that validates the complete YAML generation pipeline.
 * 
 * This test demonstrates the end-to-end flow:
 * 1. FieldMappingConfig → YamlGenerationService
 * 2. Generated YAML → File System 
 * 3. File System → YamlMappingService (existing component)
 * 4. YamlMapping → Field Transformation (GenericProcessor logic)
 * 
 * This ensures complete compatibility with your existing Spring Batch framework.
 */
@Slf4j
@SpringBootTest
class YamlGenerationIntegrationTest {

    @Autowired
    private YamlMappingService yamlMappingService;
    
    private final YamlGenerationService yamlGenerationService = new YamlGenerationService();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    private static final String TEMP_YAML_DIR = "generated-yamls";
    private Path tempDirPath;

    @BeforeEach
    void setUp() throws IOException {
        // Create a directory within the test resources folder
        tempDirPath = Path.of("target", "test-classes", TEMP_YAML_DIR);
        Files.createDirectories(tempDirPath);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up the directory
        Files.walk(tempDirPath)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    @Test
    @DisplayName("End-to-end: Config → YAML Generation → GenericProcessor Compatibility")
    void testCompleteYamlGenerationPipeline() throws Exception {
        log.info("🔄 Starting end-to-end YAML generation pipeline test");

        // ==================== STEP 1: Create Configuration ====================
        FieldMappingConfig config = createRealisticConfig();
        log.info("✅ Step 1: Created configuration with {} fields", 
                config.getFieldMappings().size());

        // ==================== STEP 2: Generate YAML Content ====================
        String generatedYaml = yamlGenerationService.generateYamlFromConfiguration(config);
        assertNotNull(generatedYaml);
        assertFalse(generatedYaml.trim().isEmpty());
        log.info("✅ Step 2: Generated YAML content ({} characters)", generatedYaml.length());
        log.debug("Generated YAML:\n{}", generatedYaml);

        // ==================== STEP 3: Save YAML to File ====================
        Path yamlFile = saveYamlToTempFile(generatedYaml, config);
        assertTrue(Files.exists(yamlFile));
        log.info("✅ Step 3: Saved YAML to file: {}", yamlFile.getFileName());

        // ==================== STEP 4: Load YAML with YamlMappingService ====================
        // This simulates how GenericProcessor loads YAML files
        String relativePath = getRelativePathForYamlMappingService(yamlFile);
        List<YamlMapping> loadedMappings = yamlMappingService.loadYamlMappings(relativePath);
        assertFalse(loadedMappings.isEmpty());
        log.info("✅ Step 4: Loaded {} YAML mapping documents", loadedMappings.size());

        // ==================== STEP 5: Validate YAML Structure Compatibility ====================
        YamlMapping mapping = loadedMappings.get(0);
        validateYamlMappingStructure(mapping, config);
        log.info("✅ Step 5: Validated YAML mapping structure compatibility");

        // ==================== STEP 6: Test Field Transformation ====================
        Map<String, Object> sampleInputRecord = createSampleInputRecord();
        Map<String, Object> transformedRecord = transformRecordUsingGeneratedYaml(
                mapping, sampleInputRecord);
        validateTransformedRecord(transformedRecord, config);
        log.info("✅ Step 6: Validated field transformation with {} output fields", 
                transformedRecord.size());

        // ==================== STEP 7: Performance Validation ====================
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            yamlGenerationService.generateYamlFromConfiguration(config);
        }
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration < 5000, "YAML generation should be fast (< 5 seconds for 100 iterations)");
        log.info("✅ Step 7: Performance validated - 100 generations in {}ms", duration);

        log.info("🎉 End-to-end pipeline test completed successfully!");
    }

    @Test
    @DisplayName("Multi-transaction type YAML generation and processing")
    void testMultiTransactionTypeIntegration() throws Exception {
        log.info("🔄 Testing multi-transaction type YAML generation");

        // Create configurations for multiple transaction types
        List<FieldMappingConfig> configs = Arrays.asList(
            createConfigForTransactionType("default"),
            createConfigForTransactionType("credit"),
            createConfigForTransactionType("debit")
        );

        // Generate multi-document YAML
        String multiDocYaml = yamlGenerationService.generateMultiDocumentYaml(configs);
        assertNotNull(multiDocYaml);
        assertTrue(multiDocYaml.contains("---")); // Document separator
        log.info("✅ Generated multi-document YAML with {} transaction types", configs.size());

        // Save and load multi-document YAML
        Path yamlFile = saveMultiDocYamlToTempFile(multiDocYaml);
        String relativePath = getRelativePathForYamlMappingService(yamlFile);
        List<YamlMapping> mappings = yamlMappingService.loadYamlMappings(relativePath);
        
        assertEquals(3, mappings.size(), "Should load 3 mapping documents");
        
        // Verify each transaction type can be retrieved
        YamlMapping defaultMapping = yamlMappingService.getMapping(relativePath, "default");
        YamlMapping creditMapping = yamlMappingService.getMapping(relativePath, "credit");
        YamlMapping debitMapping = yamlMappingService.getMapping(relativePath, "debit");
        
        assertNotNull(defaultMapping);
        assertNotNull(creditMapping);
        assertNotNull(debitMapping);
        
        assertEquals("default", defaultMapping.getTransactionType());
        assertEquals("credit", creditMapping.getTransactionType());
        assertEquals("debit", debitMapping.getTransactionType());
        
        log.info("✅ Multi-transaction type integration test completed");
    }

    @Test
    @DisplayName("Error handling and validation integration")
    void testErrorHandlingIntegration() {
        log.info("🔄 Testing error handling integration");

        // Test with invalid configuration
        FieldMappingConfig invalidConfig = createInvalidConfig();
        
        // Validation should catch errors
        ValidationResult validation = yamlGenerationService.validateForYamlGeneration(invalidConfig);
        
        assertFalse(validation.isValid());
        assertFalse(validation.getErrors().isEmpty());
        log.info("✅ Validation correctly caught {} errors", validation.getErrors().size());

        // YAML generation should fail gracefully
        assertThrows(RuntimeException.class, () -> {
            yamlGenerationService.generateYamlFromConfiguration(invalidConfig);
        });
        log.info("✅ YAML generation failed gracefully for invalid config");
    }

    // ==================== HELPER METHODS ====================

    private FieldMappingConfig createRealisticConfig() {
        FieldMappingConfig config = new FieldMappingConfig();
        config.setSourceSystem("hr");
        config.setJobName("p327");
        config.setTransactionType("default");
        config.setDescription("Test configuration for HR P327 processing");
        config.setLastModified(LocalDateTime.now());
        config.setVersion(1);

        List<FieldMapping> fields = new ArrayList<>();

        // Location Code - Constant
        FieldMapping locationField = new FieldMapping();
        locationField.setFieldName("location-code");
        locationField.setTargetField("LOCATION-CODE");
        locationField.setTargetPosition(1);
        locationField.setLength(6);
        locationField.setDataType("String");
        locationField.setTransformationType("constant");
        locationField.setValue("100030");
        locationField.setPad("right");
        locationField.setPadChar(" ");
        fields.add(locationField);

        // Account Number - Source
        FieldMapping acctField = new FieldMapping();
        acctField.setFieldName("acct-num");
        acctField.setTargetField("ACCT-NUM");
        acctField.setTargetPosition(2);
        acctField.setLength(18);
        acctField.setDataType("String");
        acctField.setTransformationType("source");
        acctField.setSourceField("acct_num");
        acctField.setPad("right");
        acctField.setPadChar(" ");
        acctField.setDefaultValue("");
        fields.add(acctField);

        // Balance Amount - Source with formatting
        FieldMapping balanceField = new FieldMapping();
        balanceField.setFieldName("balance-amt");
        balanceField.setTargetField("BALANCE-AMT");
        balanceField.setTargetPosition(3);
        balanceField.setLength(19);
        balanceField.setDataType("Numeric");
        balanceField.setTransformationType("source");
        balanceField.setSourceField("balance_amt");
        balanceField.setPad("left");
        balanceField.setPadChar("0");
        balanceField.setFormat("+9(12)V9(6)");
        balanceField.setDefaultValue("0");
        fields.add(balanceField);

        config.setFieldMappings(fields);
        return config;
    }

    private FieldMappingConfig createConfigForTransactionType(String transactionType) {
        FieldMappingConfig config = createRealisticConfig();
        config.setTransactionType(transactionType);
        return config;
    }

    private FieldMappingConfig createInvalidConfig() {
        FieldMappingConfig config = new FieldMappingConfig();
        config.setSourceSystem(""); // Invalid - empty
        config.setJobName(null);   // Invalid - null
        config.setFieldMappings(null); // Invalid - null
        return config;
    }

    private Path saveYamlToTempFile(String yamlContent, FieldMappingConfig config) throws IOException {
        Path yamlFile = tempDirPath.resolve(config.getJobName() + ".yml");
        Files.write(yamlFile, yamlContent.getBytes());
        return yamlFile;
    }

    private Path saveMultiDocYamlToTempFile(String yamlContent) throws IOException {
        Path yamlFile = tempDirPath.resolve("multi-doc-test.yml");
        Files.write(yamlFile, yamlContent.getBytes());
        return yamlFile;
    }

    private String getRelativePathForYamlMappingService(Path yamlFile) {
        // YamlMappingService expects classpath-relative paths
        return TEMP_YAML_DIR + "/" + yamlFile.getFileName().toString();
    }

    private void validateYamlMappingStructure(YamlMapping mapping, FieldMappingConfig originalConfig) {
        assertNotNull(mapping);
        assertEquals(originalConfig.getJobName(), mapping.getFileType());
        assertEquals(originalConfig.getTransactionType(), mapping.getTransactionType());
        assertNotNull(mapping.getFields());
        assertEquals(originalConfig.getFieldMappings().size(), mapping.getFields().size());

        // Validate field structure
        for (FieldMapping originalField : originalConfig.getFieldMappings()) {
            String key = originalField.getFieldName().toLowerCase().replace("_", "-");
            FieldMapping backendField = mapping.getFields().get(key);
            
            assertNotNull(backendField, "Field should exist: " + key);
            assertEquals(originalField.getTargetPosition(), backendField.getTargetPosition());
            assertEquals(originalField.getLength(), backendField.getLength());
            assertEquals(originalField.getTransformationType(), backendField.getTransformationType());
        }
    }

    private Map<String, Object> createSampleInputRecord() {
        Map<String, Object> record = new HashMap<>();
        record.put("acct_num", "123456789012345678");
        record.put("balance_amt", "1234.56");
        record.put("location_id", "LOC001");
        record.put("status_code", "A");
        return record;
    }

    private Map<String, Object> transformRecordUsingGeneratedYaml(
            YamlMapping mapping, Map<String, Object> inputRecord) {
        
        // This simulates the GenericProcessor transformation logic
        Map<String, Object> outputRecord = new LinkedHashMap<>();
        
        // Sort fields by target position (same as GenericProcessor)
        List<FieldMapping> sortedFields = mapping.getFields().entrySet().stream()
            .map(Map.Entry::getValue)
            .sorted(Comparator.comparingInt(FieldMapping::getTargetPosition))
            .collect(java.util.stream.Collectors.toList());
        
        // Transform each field using YamlMappingService logic
        for (FieldMapping field : sortedFields) {
            String transformedValue = yamlMappingService.transformField(inputRecord, field);
            outputRecord.put(field.getTargetField(), transformedValue);
        }
        
        return outputRecord;
    }

    private void validateTransformedRecord(Map<String, Object> transformedRecord, 
                                         FieldMappingConfig originalConfig) {
        assertNotNull(transformedRecord);
        assertEquals(originalConfig.getFieldMappings().size(), transformedRecord.size());
        
        // Validate specific field transformations
        assertTrue(transformedRecord.containsKey("LOCATION-CODE"));
        assertTrue(transformedRecord.containsKey("ACCT-NUM"));
        assertTrue(transformedRecord.containsKey("BALANCE-AMT"));
        
        // Validate constant field value
        assertEquals("100030", transformedRecord.get("LOCATION-CODE"));
        
        // Validate source field mapping
        assertNotNull(transformedRecord.get("ACCT-NUM"));
        
        log.info("Transformed record: {}", transformedRecord);
    }
}