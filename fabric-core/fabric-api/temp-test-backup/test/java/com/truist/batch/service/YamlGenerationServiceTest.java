package com.fabric.batch.service;

import com.fabric.batch.model.FieldMapping;
import com.fabric.batch.model.YamlMapping;
import com.fabric.batch.model.FieldMappingConfig;
import com.fabric.batch.model.Condition;
import com.fabric.batch.model.CompositeSource;
import com.fabric.batch.model.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for YamlGenerationService.
 * 
 * This test validates that configurations are correctly converted
 * to YAML format compatible with the existing GenericProcessor.
 */
@Slf4j
@SpringBootTest
class YamlGenerationServiceTest {

    private YamlGenerationService yamlGenerationService;
    private ObjectMapper yamlMapper;

    @BeforeEach
    void setUp() {
        yamlGenerationService = new YamlGenerationService();
        yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    @Test
    @DisplayName("Should generate valid YAML for simple constant field mapping")
    void testGenerateYamlForConstantFields() {
        // Given: Configuration with constant fields
        FieldMappingConfig config = createSampleConstantConfig();

        // When: Generate YAML
        String yamlContent = yamlGenerationService.generateYamlFromConfiguration(config);

        // Then: Validate YAML structure
        assertNotNull(yamlContent);
        assertFalse(yamlContent.trim().isEmpty());
        
        log.info("Generated YAML for constant fields:\n{}", yamlContent);

        // Verify YAML can be parsed back to YamlMapping
        try {
            YamlMapping parsedMapping = yamlMapper.readValue(yamlContent, YamlMapping.class);
            assertNotNull(parsedMapping);
            assertEquals("p327", parsedMapping.getFileType());
            assertEquals("default", parsedMapping.getTransactionType());
            assertNotNull(parsedMapping.getFields());
            assertTrue(parsedMapping.getFields().size() > 0);
        } catch (Exception e) {
            fail("Generated YAML should be parseable: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should generate valid YAML for source field mappings")
    void testGenerateYamlForSourceFields() {
        // Given: Configuration with source fields
        FieldMappingConfig config = createSampleSourceConfig();

        // When: Generate YAML
        String yamlContent = yamlGenerationService.generateYamlFromConfiguration(config);

        // Then: Validate YAML content
        assertNotNull(yamlContent);
        assertTrue(yamlContent.contains("transformationType: source"));
        assertTrue(yamlContent.contains("sourceField:"));
        
        log.info("Generated YAML for source fields:\n{}", yamlContent);

        // Verify YAML structure
        try {
            YamlMapping parsedMapping = yamlMapper.readValue(yamlContent, YamlMapping.class);
            FieldMapping accountField = parsedMapping.getFields().get("acct-num");
            assertNotNull(accountField);
            assertEquals("source", accountField.getTransformationType());
            assertEquals("acct_num", accountField.getSourceField());
        } catch (Exception e) {
            fail("Generated YAML should be parseable: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should generate valid YAML for composite field mappings")
    void testGenerateYamlForCompositeFields() {
        // Given: Configuration with composite fields
        FieldMappingConfig config = createSampleCompositeConfig();

        // When: Generate YAML
        String yamlContent = yamlGenerationService.generateYamlFromConfiguration(config);

        // Then: Validate YAML content
        assertNotNull(yamlContent);
        assertTrue(yamlContent.contains("transformationType: composite"));
        assertTrue(yamlContent.contains("transform:"));
        
        log.info("Generated YAML for composite fields:\n{}", yamlContent);
    }

    @Test
    @DisplayName("Should generate valid YAML for conditional field mappings")
    void testGenerateYamlForConditionalFields() {
        // Given: Configuration with conditional fields
        FieldMappingConfig config = createSampleConditionalConfig();

        // When: Generate YAML
        String yamlContent = yamlGenerationService.generateYamlFromConfiguration(config);

        // Then: Validate YAML content
        assertNotNull(yamlContent);
        assertTrue(yamlContent.contains("transformationType: conditional"));
        assertTrue(yamlContent.contains("conditions:"));
        
        log.info("Generated YAML for conditional fields:\n{}", yamlContent);
    }

    @Test
    @DisplayName("Should generate multi-document YAML for multiple transaction types")
    void testGenerateMultiDocumentYaml() {
        // Given: Multiple configurations for different transaction types
        List<FieldMappingConfig> configs = Arrays.asList(
            createConfigForTransactionType("default"),
            createConfigForTransactionType("credit"),
            createConfigForTransactionType("debit")
        );

        // When: Generate multi-document YAML
        String yamlContent = yamlGenerationService.generateMultiDocumentYaml(configs);

        // Then: Validate multi-document structure
        assertNotNull(yamlContent);
        assertTrue(yamlContent.contains("---")); // Document separator
        assertTrue(yamlContent.contains("transactionType: default"));
        assertTrue(yamlContent.contains("transactionType: credit"));
        assertTrue(yamlContent.contains("transactionType: debit"));
        
        log.info("Generated multi-document YAML:\n{}", yamlContent);
    }

    @Test
    @DisplayName("Should validate configuration successfully for valid config")
    void testValidateValidConfiguration() {
        // Given: Valid configuration
        FieldMappingConfig config = createValidCompleteConfig();

        // When: Validate configuration
        ValidationResult result = yamlGenerationService.validateForYamlGeneration(config);

        // Then: Validation should pass
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Should detect validation errors for invalid config")
    void testValidateInvalidConfiguration() {
        // Given: Invalid configuration
        FieldMappingConfig config = createInvalidConfig();

        // When: Validate configuration
        ValidationResult result = yamlGenerationService.validateForYamlGeneration(config);

        // Then: Validation should fail with errors
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        
        log.info("Validation errors: {}", result.getErrors());
    }

    @Test
    @DisplayName("Should generate YAML compatible with existing GenericProcessor format")
    void testYamlCompatibilityWithGenericProcessor() {
        // Given: Configuration matching existing p327 structure
        FieldMappingConfig config = createP327CompatibleConfig();

        // When: Generate YAML
        String yamlContent = yamlGenerationService.generateYamlFromConfiguration(config);

        // Then: Validate against expected GenericProcessor format
        assertNotNull(yamlContent);
        
        // Check required YAML structure elements
        assertTrue(yamlContent.contains("fileType:"));
        assertTrue(yamlContent.contains("transactionType:"));
        assertTrue(yamlContent.contains("fields:"));
        
        // Verify field structure matches GenericProcessor expectations
        assertTrue(yamlContent.contains("targetPosition:"));
        assertTrue(yamlContent.contains("length:"));
        assertTrue(yamlContent.contains("dataType:"));
        assertTrue(yamlContent.contains("transformationType:"));
        
        log.info("GenericProcessor-compatible YAML:\n{}", yamlContent);

        // Test that it can be parsed as YamlMapping
        try {
            YamlMapping mapping = yamlMapper.readValue(yamlContent, YamlMapping.class);
            assertNotNull(mapping.getFields());
            
            // Verify field ordering by targetPosition
            List<FieldMapping> sortedFields = mapping.getFields().entrySet().stream()
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparingInt(FieldMapping::getTargetPosition))
                .collect(java.util.stream.Collectors.toList());
            
            assertEquals(3, sortedFields.size()); // Expected number of fields
            assertEquals(1, sortedFields.get(0).getTargetPosition());
            assertEquals(2, sortedFields.get(1).getTargetPosition());
            assertEquals(3, sortedFields.get(2).getTargetPosition());
            
        } catch (Exception e) {
            fail("Generated YAML should be compatible with YamlMapping: " + e.getMessage());
        }
    }

    // ==================== HELPER METHODS FOR TEST DATA ====================

    private FieldMappingConfig createSampleConstantConfig() {
        FieldMappingConfig config = new FieldMappingConfig();
        config.setSourceSystem("hr");
        config.setJobName("p327");
        config.setTransactionType("default");
        config.setLastModified(LocalDateTime.now());
        config.setVersion(1);

        List<FieldMapping> fields = new ArrayList<>();
        
        // Constant field
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
        config.setFieldMappings(fields);
        
        return config;
    }

    private FieldMappingConfig createSampleSourceConfig() {
        FieldMappingConfig config = new FieldMappingConfig();
        config.setSourceSystem("hr");
        config.setJobName("p327");
        config.setTransactionType("default");
        config.setLastModified(LocalDateTime.now());
        config.setVersion(1);

        List<FieldMapping> fields = new ArrayList<>();
        
        // Source field
        FieldMapping acctField = new FieldMapping();
        acctField.setFieldName("acct-num");
        acctField.setTargetField("ACCT-NUM");
        acctField.setTargetPosition(2);
        acctField.setLength(18);
        acctField.setDataType("String");
        acctField.setTransformationType("source");
        acctField.setSourceField("acct_num");
        acctField.setPad("right");
        
        fields.add(acctField);
        config.setFieldMappings(fields);
        
        return config;
    }

    private FieldMappingConfig createSampleCompositeConfig() {
        FieldMappingConfig config = new FieldMappingConfig();
        config.setSourceSystem("hr");
        config.setJobName("p327");
        config.setTransactionType("default");
        config.setLastModified(LocalDateTime.now());
        config.setVersion(1);

        List<FieldMapping> fields = new ArrayList<>();
        
        // Composite field
        FieldMapping compositeField = new FieldMapping();
        compositeField.setFieldName("full-name");
        compositeField.setTargetField("FULL-NAME");
        compositeField.setTargetPosition(3);
        compositeField.setLength(50);
        compositeField.setDataType("String");
        compositeField.setTransformationType("composite");
        compositeField.setTransform("concat");
        compositeField.setDelimiter(" ");
        compositeField.setComposite(true);
        
        // Add composite sources using actual model structure
        List<Map<String, String>> sources = new ArrayList<>();
        Map<String, String> firstNameSource = new HashMap<>();
        firstNameSource.put("field", "first_name");
        sources.add(firstNameSource);
        
        Map<String, String> lastNameSource = new HashMap<>();
        lastNameSource.put("field", "last_name");
        sources.add(lastNameSource);
        
        compositeField.setSources(sources);
        
        fields.add(compositeField);
        config.setFieldMappings(fields);
        
        return config;
    }

    private FieldMappingConfig createSampleConditionalConfig() {
        FieldMappingConfig config = new FieldMappingConfig();
        config.setSourceSystem("hr");
        config.setJobName("p327");
        config.setTransactionType("default");
        config.setLastModified(LocalDateTime.now());
        config.setVersion(1);

        List<FieldMapping> fields = new ArrayList<>();
        
        // Conditional field
        FieldMapping conditionalField = new FieldMapping();
        conditionalField.setFieldName("status-code");
        conditionalField.setTargetField("STATUS-CODE");
        conditionalField.setTargetPosition(4);
        conditionalField.setLength(2);
        conditionalField.setDataType("String");
        conditionalField.setTransformationType("conditional");
        
        // Add conditions
        List<Condition> conditions = new ArrayList<>();
        Condition condition = new Condition();
        condition.setIfExpr("balance_amt > 0");
        condition.setThen("A");
        condition.setElseExpr("I");
        conditions.add(condition);
        
        conditionalField.setConditions(conditions);
        
        fields.add(conditionalField);
        config.setFieldMappings(fields);
        
        return config;
    }

    private FieldMappingConfig createConfigForTransactionType(String transactionType) {
        FieldMappingConfig config = createSampleConstantConfig();
        config.setTransactionType(transactionType);
        return config;
    }

    private FieldMappingConfig createValidCompleteConfig() {
        FieldMappingConfig config = new FieldMappingConfig();
        config.setSourceSystem("hr");
        config.setJobName("p327");
        config.setTransactionType("default");
        config.setLastModified(LocalDateTime.now());
        config.setVersion(1);

        List<FieldMapping> fields = new ArrayList<>();
        
        // Add multiple valid fields
        fields.add(createValidField("location-code", 1, "constant", "100030"));
        fields.add(createValidField("acct-num", 2, "source", null));
        fields.add(createValidField("balance-amt", 3, "source", null));
        
        config.setFieldMappings(fields);
        return config;
    }

    private FieldMappingConfig createInvalidConfig() {
        FieldMappingConfig config = new FieldMappingConfig();
        // Missing required fields
        config.setSourceSystem("");  // Invalid - empty
        config.setJobName(null);     // Invalid - null
        config.setFieldMappings(null); // Invalid - null
        
        return config;
    }

    private FieldMappingConfig createP327CompatibleConfig() {
        FieldMappingConfig config = new FieldMappingConfig();
        config.setSourceSystem("hr");
        config.setJobName("p327");
        config.setTransactionType("default");
        config.setLastModified(LocalDateTime.now());
        config.setVersion(1);

        List<FieldMapping> fields = new ArrayList<>();
        
        // Create fields that match existing p327 structure
        fields.add(createP327Field("location-code", 1, "constant", "100030"));
        fields.add(createP327Field("acct-num", 2, "source", "acct_num"));
        fields.add(createP327Field("balance-amt", 3, "source", "balance_amt"));
        
        config.setFieldMappings(fields);
        return config;
    }

    private FieldMapping createValidField(String fieldName, int position, String transformationType, String value) {
        FieldMapping field = new FieldMapping();
        field.setFieldName(fieldName);
        field.setTargetField(fieldName.toUpperCase());
        field.setTargetPosition(position);
        field.setLength(20);
        field.setDataType("String");
        field.setTransformationType(transformationType);
        field.setPad("right");
        
        if ("constant".equals(transformationType)) {
            field.setValue(value);
        } else if ("source".equals(transformationType)) {
            field.setSourceField(fieldName.replace("-", "_"));
        }
        
        return field;
    }

    private FieldMapping createP327Field(String fieldName, int position, String transformationType, String sourceOrValue) {
        FieldMapping field = new FieldMapping();
        field.setFieldName(fieldName);
        field.setTargetField(fieldName.toUpperCase());
        field.setTargetPosition(position);
        field.setDataType("String");
        field.setTransformationType(transformationType);
        field.setPad("right");
        field.setPadChar(" ");
        
        // Set appropriate length based on field type
        switch (fieldName) {
            case "location-code":
                field.setLength(6);
                field.setValue(sourceOrValue);
                break;
            case "acct-num":
                field.setLength(18);
                field.setSourceField(sourceOrValue);
                break;
            case "balance-amt":
                field.setLength(19);
                field.setSourceField(sourceOrValue);
                field.setDataType("Numeric");
                field.setFormat("+9(12)V9(6)");
                field.setPad("left");
                break;
        }
        
        return field;
    }
}