package com.fabric.batch.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fabric.batch.model.FieldMapping;
import com.fabric.batch.model.FieldMappingConfig;
import com.fabric.batch.service.YamlFileService;
import com.fabric.batch.service.ConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Day 1: Complete YAML File Pipeline
 * 
 * Tests the hybrid approach: Database + YAML file generation
 */
@SpringBootTest
@TestPropertySource(properties = {
    "batch.yaml.output.directory=src/test/resources/test-batch-sources",
    "batch.yaml.backup.enabled=true"
})
class Day1IntegrationTest {

    @Autowired
    private YamlFileService yamlFileService;
    
    @Autowired
    private ConfigurationService configurationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @TempDir
    Path tempDir;
    
    private static final String TEST_SOURCE_SYSTEM = "hr";
    private static final String TEST_JOB_NAME = "p327";
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test files
        cleanupTestFiles();
    }
    
    @Test
    @DisplayName("Day 1: Complete YAML file generation pipeline")
    void testCompleteYamlFileGeneration() throws Exception {
        
        // ==================== STEP 1: Create Test Configurations ====================
        List<FieldMappingConfig> configs = createTestConfigurations();
        assertEquals(2, configs.size(), "Should have 2 transaction types");
        
        // ==================== STEP 2: Save Configurations to Database ====================
        for (FieldMappingConfig config : configs) {
            configurationService.saveConfiguration(config);
        }
        
        // Verify database save
        List<FieldMappingConfig> savedConfigs = configurationService.getAllTransactionTypesForJob(TEST_SOURCE_SYSTEM, TEST_JOB_NAME);
        assertEquals(2, savedConfigs.size(), "Should have saved 2 configurations to database");
        
        // ==================== STEP 3: Generate YAML Files ====================
        Map<String, String> savedFiles = yamlFileService.saveConfigurationFiles(TEST_SOURCE_SYSTEM, TEST_JOB_NAME, configs);
        
        assertNotNull(savedFiles, "Should return saved file paths");
        assertTrue(savedFiles.containsKey("fieldMapping"), "Should contain field mapping file path");
        assertTrue(savedFiles.containsKey("batchProps"), "Should contain batch props file path");
        
        // ==================== STEP 4: Verify File Structure ====================
        
        // Check field mapping file
        Path mappingFile = Paths.get("src/test/resources/test-batch-sources/mappings", TEST_JOB_NAME, TEST_SOURCE_SYSTEM, TEST_JOB_NAME + ".yml");
        assertTrue(Files.exists(mappingFile), "Field mapping YAML file should exist: " + mappingFile);
        
        // Check batch props file  
        Path batchPropsFile = Paths.get("src/test/resources/test-batch-sources", TEST_SOURCE_SYSTEM + "-batch-props.yml");
        assertTrue(Files.exists(batchPropsFile), "Batch props YAML file should exist: " + batchPropsFile);
        
        // ==================== STEP 5: Validate File Contents ====================
        
        // Validate field mapping YAML content
        String mappingContent = Files.readString(mappingFile);
        assertFalse(mappingContent.trim().isEmpty(), "Field mapping file should not be empty");
        assertTrue(mappingContent.contains("fileType: " + TEST_JOB_NAME), "Should contain correct fileType");
        assertTrue(mappingContent.contains("transactionType: \"200\""), "Should contain transaction type 200");
        assertTrue(mappingContent.contains("transactionType: \"900\""), "Should contain transaction type 900");
        assertTrue(mappingContent.contains("---"), "Should contain document separator for multi-document YAML");
        
        // Validate batch props YAML content
        String batchPropsContent = Files.readString(batchPropsFile);
        assertFalse(batchPropsContent.trim().isEmpty(), "Batch props file should not be empty");
        assertTrue(batchPropsContent.contains("batch:"), "Should contain batch section");
        assertTrue(batchPropsContent.contains("sources:"), "Should contain sources section");
        assertTrue(batchPropsContent.contains(TEST_SOURCE_SYSTEM + ":"), "Should contain source system");
        assertTrue(batchPropsContent.contains("jobs:"), "Should contain jobs section");
        assertTrue(batchPropsContent.contains(TEST_JOB_NAME + ":"), "Should contain job name");
        
        // ==================== STEP 6: Validate File Structure Compliance ====================
        boolean isValid = yamlFileService.validateGeneratedFiles(TEST_SOURCE_SYSTEM, TEST_JOB_NAME);
        assertTrue(isValid, "Generated files should pass validation");
        
        // ==================== STEP 7: Test File Backup ====================
        
        // Generate files again to test backup functionality
        Map<String, String> secondSave = yamlFileService.saveConfigurationFiles(TEST_SOURCE_SYSTEM, TEST_JOB_NAME, configs);
        assertNotNull(secondSave, "Second save should succeed");
        
        // Check that backup files were created
        Path mappingDir = mappingFile.getParent();
        long backupCount = Files.list(mappingDir)
            .filter(path -> path.getFileName().toString().contains("_backup_"))
            .count();
        assertTrue(backupCount > 0, "Should have created backup files");
        
        System.out.println("✅ Day 1 Integration Test Passed!");
        System.out.println("📁 Generated files:");
        System.out.println("   • " + savedFiles.get("fieldMapping"));
        System.out.println("   • " + savedFiles.get("batchProps"));
        System.out.println("🔍 Backup files created: " + backupCount);
    }
    
    @Test
    @DisplayName("Test batch-props.yml structure compliance")
    void testBatchPropsStructure() throws Exception {
        
        List<FieldMappingConfig> configs = createTestConfigurations();
        
        // Generate files
        yamlFileService.saveConfigurationFiles(TEST_SOURCE_SYSTEM, TEST_JOB_NAME, configs);
        
        // Read and validate batch-props structure
        Path batchPropsFile = Paths.get("src/test/resources/test-batch-sources", TEST_SOURCE_SYSTEM + "-batch-props.yml");
        String content = Files.readString(batchPropsFile);
        
        // Parse as Map to validate structure
        @SuppressWarnings("unchecked")
        Map<String, Object> batchProps = objectMapper.readValue(content, Map.class);
        
        // Validate top-level structure
        assertTrue(batchProps.containsKey("batch"), "Should have batch section");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> batch = (Map<String, Object>) batchProps.get("batch");
        assertTrue(batch.containsKey("sources"), "Should have sources section");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> sources = (Map<String, Object>) batch.get("sources");
        assertTrue(sources.containsKey(TEST_SOURCE_SYSTEM), "Should have source system section");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> sourceConfig = (Map<String, Object>) sources.get(TEST_SOURCE_SYSTEM);
        assertTrue(sourceConfig.containsKey("jobs"), "Should have jobs section");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> jobs = (Map<String, Object>) sourceConfig.get("jobs");
        assertTrue(jobs.containsKey(TEST_JOB_NAME), "Should have job configuration");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> jobConfig = (Map<String, Object>) jobs.get(TEST_JOB_NAME);
        assertTrue(jobConfig.containsKey("files"), "Should have files configuration");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) jobConfig.get("files");
        assertEquals(2, files.size(), "Should have 2 file configurations (one per transaction type)");
        
        System.out.println("✅ Batch props structure validation passed!");
    }
    
    /**
     * Creates test configurations with multiple transaction types.
     */
    private List<FieldMappingConfig> createTestConfigurations() {
        
        // Transaction type 200 configuration
        FieldMappingConfig config200 = new FieldMappingConfig();
        config200.setSourceSystem(TEST_SOURCE_SYSTEM);
        config200.setJobName(TEST_JOB_NAME);
        config200.setTransactionType("200");
        
        FieldMapping field1 = new FieldMapping();
        field1.setTargetField("LOCATION_CODE");
        field1.setTargetPosition(1);
        field1.setLength(6);
        field1.setDataType("String");
        field1.setTransformationType("constant");
        field1.setValue("100020");
        field1.setPad("right");
        field1.setPadChar(" ");
        
        FieldMapping field2 = new FieldMapping();
        field2.setTargetField("ACCT_NUM");
        field2.setSourceField("acct_num");
        field2.setTargetPosition(2);
        field2.setLength(18);
        field2.setDataType("String");
        field2.setTransformationType("source");
        field2.setPad("right");
        field2.setPadChar(" ");
        
        config200.setFieldMappings(Arrays.asList(field1, field2));
        
        // Transaction type 900 configuration
        FieldMappingConfig config900 = new FieldMappingConfig();
        config900.setSourceSystem(TEST_SOURCE_SYSTEM);
        config900.setJobName(TEST_JOB_NAME);
        config900.setTransactionType("900");
        
        FieldMapping field3 = new FieldMapping();
        field3.setTargetField("LOCATION_CODE");
        field3.setTargetPosition(1);
        field3.setLength(6);
        field3.setDataType("String");
        field3.setTransformationType("constant");
        field3.setValue("100020");
        field3.setPad("right");
        field3.setPadChar(" ");
        
        FieldMapping field4 = new FieldMapping();
        field4.setTargetField("NET_PAY");
        field4.setSourceField("net_pay");
        field4.setTargetPosition(2);
        field4.setLength(13);
        field4.setDataType("Numeric");
        field4.setTransformationType("source");
        field4.setFormat("9(12)");
        field4.setPad("left");
        field4.setPadChar("0");
        
        config900.setFieldMappings(Arrays.asList(field3, field4));
        
        return Arrays.asList(config200, config900);
    }
    
    /**
     * Cleans up test files.
     */
    private void cleanupTestFiles() {
        try {
            Path testDir = Paths.get("src/test/resources/test-batch-sources");
            if (Files.exists(testDir)) {
                Files.walk(testDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            // Ignore cleanup errors
                        }
                    });
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}