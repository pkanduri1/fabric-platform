package com.truist.batch.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truist.batch.entity.ManualJobConfigEntity;
import com.truist.batch.repository.ManualJobConfigRepository;
import com.truist.batch.service.ManualJobExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for complete manual batch execution workflow.
 * Tests the entire pipeline from JSON configuration to execution tracking.
 * 
 * @author Senior Full Stack Developer Agent
 * @since Phase 3 - Batch Execution Enhancement
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false",
    "batch.output.directory=/tmp/test/batch/output"
})
@Transactional
class ManualBatchExecutionIntegrationTest {

    @Autowired
    private ManualJobExecutionService executionService;

    @Autowired
    private ManualJobConfigRepository configRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ManualJobConfigEntity testConfig;

    @BeforeEach
    void setUp() {
        // Create test configuration
        testConfig = createTestConfiguration();
    }

    @Test
    void testCompleteExecutionWorkflow() {
        // Given
        String executedBy = "integration-test-user";
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put("batchDate", "2025-08-14");

        // When
        Map<String, Object> result = executionService.executeJobWithJsonConfig(
            testConfig.getConfigId(), 
            executedBy, 
            additionalParams
        );

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("executionId"));
        assertTrue(result.containsKey("correlationId"));
        assertEquals(testConfig.getConfigId(), result.get("configId"));
        assertEquals("atoctran_encore_200_job", result.get("jobName"));

        // Verify execution record was created
        String executionId = (String) result.get("executionId");
        Map<String, Object> executionStatus = executionService.getExecutionStatus(executionId);
        assertNotNull(executionStatus);
        assertEquals(executionId, executionStatus.get("EXECUTION_ID"));
    }

    @Test
    void testMasterQueryRetrieval() {
        // Given
        insertTestMasterQuery();

        // When
        String query = executionService.getMasterQuery("ENCORE", "atoctran_encore_200_job");

        // Then
        assertNotNull(query);
        assertTrue(query.contains("ENCORE_TEST_DATA"));
        assertTrue(query.contains("BATCH_DATE"));
    }

    @Test
    void testExecutionWithInvalidConfiguration() {
        // Given
        String invalidConfigId = "non-existent-config";
        String executedBy = "test-user";

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            executionService.executeJobWithJsonConfig(invalidConfigId, executedBy, null);
        });

        assertTrue(exception.getMessage().contains("Configuration not found"));
    }

    @Test
    void testExecutionStatusTracking() {
        // Given
        String configId = testConfig.getConfigId();
        String executedBy = "status-test-user";

        // When
        Map<String, Object> result = executionService.executeJobWithJsonConfig(configId, executedBy, null);
        String executionId = (String) result.get("executionId");

        // Then
        Map<String, Object> status = executionService.getExecutionStatus(executionId);
        assertNotNull(status);
        assertEquals(executionId, status.get("EXECUTION_ID"));
        assertEquals(configId, status.get("JOB_CONFIG_ID"));
        assertEquals("ENCORE", status.get("SOURCE_SYSTEM"));
        assertNotNull(status.get("CORRELATION_ID"));
    }

    @Test
    void testExecutionWithCustomParameters() {
        // Given
        String configId = testConfig.getConfigId();
        String executedBy = "param-test-user";
        Map<String, String> customParams = new HashMap<>();
        customParams.put("batchDate", "2025-08-15");
        customParams.put("customParam1", "value1");
        customParams.put("customParam2", "value2");

        // When
        Map<String, Object> result = executionService.executeJobWithJsonConfig(
            configId, 
            executedBy, 
            customParams
        );

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("executionId"));
        
        // Verify custom parameters are handled
        String executionId = (String) result.get("executionId");
        Map<String, Object> status = executionService.getExecutionStatus(executionId);
        assertNotNull(status);
    }

    private ManualJobConfigEntity createTestConfiguration() {
        String jsonConfig = """
            {
              "sourceSystem": "ENCORE",
              "jobName": "atoctran_encore_200_job",
              "transactionType": "200",
              "description": "Integration test configuration",
              "fieldMappings": [
                {
                  "fieldName": "location-code",
                  "value": "100030",
                  "transformationType": "constant",
                  "length": 6,
                  "targetPosition": 1
                },
                {
                  "fieldName": "acct-num",
                  "sourceField": "acct_num",
                  "transformationType": "source",
                  "length": 18,
                  "targetPosition": 2
                },
                {
                  "fieldName": "transaction-code",
                  "value": "200",
                  "transformationType": "constant",
                  "length": 3,
                  "targetPosition": 3
                },
                {
                  "fieldName": "transaction-date",
                  "sourceField": "batch_date",
                  "transformationType": "source",
                  "length": 8,
                  "format": "YYYYMMDD",
                  "targetPosition": 4
                }
              ]
            }
            """;

        ManualJobConfigEntity config = new ManualJobConfigEntity();
        config.setConfigId("test-config-" + UUID.randomUUID().toString().substring(0, 8));
        config.setJobName("atoctran_encore_200_job");
        config.setJobType("ETL_BATCH");
        config.setSourceSystem("ENCORE");
        config.setTargetSystem("FILE_OUTPUT");
        config.setJobParameters(jsonConfig);
        config.setStatus("ACTIVE");
        config.setCreatedBy("integration-test");
        config.setCreatedDate(LocalDateTime.now());
        config.setVersionNumber(1L);

        return configRepository.save(config);
    }

    private void insertTestMasterQuery() {
        String sql = """
            INSERT INTO MASTER_QUERY_CONFIG 
            (SOURCE_SYSTEM, JOB_NAME, QUERY_TEXT, VERSION, IS_ACTIVE, CREATED_BY) 
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        String query = """
            SELECT 
                ACCT_NUM as acct_num,
                BATCH_DATE as batch_date,
                CCI as cci,
                CONTACT_ID as contact_id
            FROM ENCORE_TEST_DATA
            WHERE BATCH_DATE = TO_DATE(:batchDate, 'YYYY-MM-DD')
            ORDER BY ACCT_NUM
            """;

        jdbcTemplate.update(sql, 
            "ENCORE", 
            "atoctran_encore_200_job", 
            query, 
            1, 
            "Y", 
            "integration-test"
        );
    }
}