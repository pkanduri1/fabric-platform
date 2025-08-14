package com.truist.batch.service;

import com.truist.batch.entity.ManualJobConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ManualJobExecutionService
 * 
 * @author Senior Full Stack Developer Agent
 * @since Phase 3 - Batch Execution Enhancement
 */
@ExtendWith(MockitoExtension.class)
class ManualJobExecutionServiceTest {

    @Mock
    private ManualJobConfigService configService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ManualJobExecutionService executionService;

    private final String sampleJsonConfig = """
        {
          "sourceSystem": "ENCORE",
          "jobName": "atoctran_encore_200_job",
          "transactionType": "200",
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
            }
          ]
        }
        """;

    @BeforeEach
    void setUp() {
        executionService = new ManualJobExecutionService(configService, jdbcTemplate);
    }

    @Test
    void testExecuteJobWithJsonConfig_Success() {
        // Given
        String configId = "test-config-123";
        String executedBy = "test-user";
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put("batchDate", "2025-08-14");

        ManualJobConfigEntity mockConfig = new ManualJobConfigEntity();
        mockConfig.setConfigId(configId);
        mockConfig.setJobParameters(sampleJsonConfig);

        when(configService.getJobConfiguration(configId)).thenReturn(Optional.of(mockConfig));
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);
        when(jdbcTemplate.update(anyString(), any(), any(), any())).thenReturn(1);

        // When
        Map<String, Object> result = executionService.executeJobWithJsonConfig(configId, executedBy, additionalParams);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("executionId"));
        assertTrue(result.containsKey("correlationId"));
        assertEquals("SIMULATED", result.get("status")); // Since JobLauncher is not available in test
        assertEquals(configId, result.get("configId"));
        assertEquals("atoctran_encore_200_job", result.get("jobName"));

        // Verify database interactions
        verify(configService).getJobConfiguration(configId);
        verify(jdbcTemplate, times(2)).update(anyString(), any(Object[].class));
    }

    @Test
    void testExecuteJobWithJsonConfig_ConfigNotFound() {
        // Given
        String configId = "non-existent-config";
        String executedBy = "test-user";

        when(configService.getJobConfiguration(configId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            executionService.executeJobWithJsonConfig(configId, executedBy, null);
        });

        assertTrue(exception.getMessage().contains("Configuration not found"));
        verify(configService).getJobConfiguration(configId);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testExecuteJobWithJsonConfig_InvalidJson() {
        // Given
        String configId = "test-config-invalid";
        String executedBy = "test-user";

        ManualJobConfigEntity mockConfig = new ManualJobConfigEntity();
        mockConfig.setConfigId(configId);
        mockConfig.setJobParameters("{ invalid json }");

        when(configService.getJobConfiguration(configId)).thenReturn(Optional.of(mockConfig));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            executionService.executeJobWithJsonConfig(configId, executedBy, null);
        });

        assertTrue(exception.getMessage().contains("Job execution failed"));
        verify(configService).getJobConfiguration(configId);
    }

    @Test
    void testExecuteJobWithJsonConfig_WithDefaultBatchDate() {
        // Given
        String configId = "test-config-123";
        String executedBy = "test-user";

        ManualJobConfigEntity mockConfig = new ManualJobConfigEntity();
        mockConfig.setConfigId(configId);
        mockConfig.setJobParameters(sampleJsonConfig);

        when(configService.getJobConfiguration(configId)).thenReturn(Optional.of(mockConfig));
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);
        when(jdbcTemplate.update(anyString(), any(), any(), any())).thenReturn(1);

        // When (no additional params provided)
        Map<String, Object> result = executionService.executeJobWithJsonConfig(configId, executedBy, null);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("executionId"));
        verify(configService).getJobConfiguration(configId);
    }

    @Test
    void testGetExecutionStatus_Success() {
        // Given
        String executionId = "exec-123";
        Map<String, Object> mockStatus = new HashMap<>();
        mockStatus.put("EXECUTION_ID", executionId);
        mockStatus.put("STATUS", "SUCCESS");
        mockStatus.put("RECORDS_PROCESSED", 100);

        when(jdbcTemplate.queryForMap(anyString(), eq(executionId))).thenReturn(mockStatus);

        // When
        Map<String, Object> result = executionService.getExecutionStatus(executionId);

        // Then
        assertNotNull(result);
        assertEquals(executionId, result.get("EXECUTION_ID"));
        assertEquals("SUCCESS", result.get("STATUS"));
        assertEquals(100, result.get("RECORDS_PROCESSED"));
        verify(jdbcTemplate).queryForMap(anyString(), eq(executionId));
    }

    @Test
    void testGetMasterQuery_Success() {
        // Given
        String sourceSystem = "ENCORE";
        String jobName = "atoctran_encore_200_job";
        String expectedQuery = "SELECT * FROM ENCORE_TEST_DATA WHERE BATCH_DATE = :batchDate";

        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(sourceSystem), eq(jobName)))
            .thenReturn(expectedQuery);

        // When
        String result = executionService.getMasterQuery(sourceSystem, jobName);

        // Then
        assertEquals(expectedQuery, result);
        verify(jdbcTemplate).queryForObject(anyString(), eq(String.class), eq(sourceSystem), eq(jobName));
    }

    @Test
    void testGetMasterQuery_NotFound() {
        // Given
        String sourceSystem = "UNKNOWN";
        String jobName = "unknown_job";

        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(sourceSystem), eq(jobName)))
            .thenThrow(new RuntimeException("No result found"));

        // When
        String result = executionService.getMasterQuery(sourceSystem, jobName);

        // Then
        assertNull(result);
        verify(jdbcTemplate).queryForObject(anyString(), eq(String.class), eq(sourceSystem), eq(jobName));
    }
}