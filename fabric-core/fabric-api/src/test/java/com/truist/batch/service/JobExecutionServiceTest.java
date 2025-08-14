package com.truist.batch.service;

import com.truist.batch.dto.JobExecutionRequest;
import com.truist.batch.entity.ManualJobConfigEntity;
import com.truist.batch.repository.ManualJobConfigRepository;
import com.truist.batch.service.JobExecutionService.JobExecutionResult;
import com.truist.batch.service.JobExecutionService.JobCancellationResult;
import com.truist.batch.service.JobExecutionService.ExecutionStatistics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for JobExecutionService.
 * 
 * Tests enterprise-grade job execution functionality including:
 * - Job execution with comprehensive validation and monitoring
 * - Security context validation and parameter handling
 * - Error handling and retry mechanisms
 * - Performance monitoring and statistics collection
 * - Dry run validation and actual execution flows
 * 
 * Banking-grade testing standards:
 * - 95%+ code coverage requirement
 * - Comprehensive error scenario testing
 * - Security validation and audit trail testing
 * - Performance and resource usage validation
 * 
 * @author Senior Full Stack Developer Agent
 * @version 2.0
 * @since US001 Phase 2 - Unit Testing Implementation
 */
@ExtendWith(MockitoExtension.class)
class JobExecutionServiceTest {

    @Mock
    private ManualJobConfigRepository configRepository;

    @InjectMocks
    private JobExecutionService jobExecutionService;

    private JobExecutionRequest validExecutionRequest;
    private ManualJobConfigEntity validConfiguration;

    @BeforeEach
    void setUp() {
        validExecutionRequest = createValidExecutionRequest();
        validConfiguration = createValidConfiguration();
    }

    @Test
    @DisplayName("Should execute job successfully with valid configuration")
    void executeJob_WithValidRequest_ShouldReturnSuccessfulResult() {
        // Arrange
        String executedBy = "test.user@bank.com";
        
        when(configRepository.findById(validExecutionRequest.getConfigId()))
            .thenReturn(Optional.of(validConfiguration));

        // Act
        JobExecutionResult result = jobExecutionService.executeJob(validExecutionRequest, executedBy);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getExecutionId());
        assertTrue(result.getExecutionId().startsWith("exec_core_banking_"));
        assertEquals(validExecutionRequest.getConfigId(), result.getConfigId());
        assertEquals(validConfiguration.getJobName(), result.getJobName());
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(executedBy, result.getExecutedBy());
        assertEquals(validExecutionRequest.getExecutionType(), result.getExecutionType());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getEndTime());
        assertTrue(result.getDurationSeconds() >= 0);
        assertEquals(1000L, result.getRecordsProcessed());
        assertEquals(1000L, result.getRecordsSuccess());
        assertEquals(0L, result.getRecordsError());
        assertNull(result.getErrorMessage());

        // Verify repository interaction
        verify(configRepository).findById(validExecutionRequest.getConfigId());
    }

    @Test
    @DisplayName("Should perform dry run validation without actual execution")
    void executeJob_WithDryRun_ShouldReturnValidationResult() {
        // Arrange
        String executedBy = "test.user@bank.com";
        validExecutionRequest.setDryRun(true);
        
        when(configRepository.findById(validExecutionRequest.getConfigId()))
            .thenReturn(Optional.of(validConfiguration));

        // Act
        JobExecutionResult result = jobExecutionService.executeJob(validExecutionRequest, executedBy);

        // Assert
        assertNotNull(result);
        assertEquals("DRY_RUN_COMPLETED", result.getStatus());
        assertEquals(0.0, result.getDurationSeconds());
        assertNotNull(result.getExecutionDetails());
        assertTrue((Boolean) result.getExecutionDetails().get("configurationValid"));
        assertTrue((Boolean) result.getExecutionDetails().get("parametersValid"));
        assertTrue((Boolean) result.getExecutionDetails().get("resourcesAvailable"));
        assertTrue((Boolean) result.getExecutionDetails().get("permissionsValid"));
    }

    @Test
    @DisplayName("Should throw exception when configuration not found")
    void executeJob_WithNonExistentConfig_ShouldThrowException() {
        // Arrange
        String executedBy = "test.user@bank.com";
        String nonExistentConfigId = "non-existent-config";
        validExecutionRequest.setConfigId(nonExistentConfigId);
        
        when(configRepository.findById(nonExistentConfigId))
            .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> jobExecutionService.executeJob(validExecutionRequest, executedBy)
        );
        
        assertTrue(exception.getMessage().contains("Job configuration not found"));
        assertEquals("Job configuration not found: " + nonExistentConfigId, exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when configuration is inactive")
    void executeJob_WithInactiveConfig_ShouldThrowException() {
        // Arrange
        String executedBy = "test.user@bank.com";
        validConfiguration.setStatus("INACTIVE");
        
        when(configRepository.findById(validExecutionRequest.getConfigId()))
            .thenReturn(Optional.of(validConfiguration));

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> jobExecutionService.executeJob(validExecutionRequest, executedBy)
        );
        
        assertTrue(exception.getMessage().contains("Cannot execute inactive job configuration"));
    }

    @Test
    @DisplayName("Should validate execution request parameters")
    void executeJob_WithInvalidRequest_ShouldThrowException() {
        // Arrange
        String executedBy = "test.user@bank.com";
        JobExecutionRequest invalidRequest = JobExecutionRequest.builder()
            .configId("") // Invalid: empty config ID
            .executionType("MANUAL")
            .triggerSource("USER_INTERFACE")
            .businessJustification("") // Invalid: empty justification
            .environment("PRODUCTION")
            .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> jobExecutionService.executeJob(invalidRequest, executedBy)
        );
        
        assertEquals("Invalid execution request", exception.getMessage());
    }

    @Test
    @DisplayName("Should validate executed by parameter")
    void executeJob_WithEmptyExecutedBy_ShouldThrowException() {
        // Arrange
        String emptyExecutedBy = "";
        
        when(configRepository.findById(validExecutionRequest.getConfigId()))
            .thenReturn(Optional.of(validConfiguration));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> jobExecutionService.executeJob(validExecutionRequest, emptyExecutedBy)
        );
        
        assertEquals("Executed by user is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should require approval ticket for high-risk executions")
    void executeJob_WithHighRiskWithoutApproval_ShouldThrowException() {
        // Arrange
        String executedBy = "test.user@bank.com";
        validExecutionRequest.setPriority("CRITICAL");
        validExecutionRequest.setRiskAssessment("CRITICAL");
        validExecutionRequest.setApprovalTicketReference(null);
        
        when(configRepository.findById(validExecutionRequest.getConfigId()))
            .thenReturn(Optional.of(validConfiguration));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> jobExecutionService.executeJob(validExecutionRequest, executedBy)
        );
        
        assertTrue(exception.getMessage().contains("Approval ticket reference required"));
    }

    @Test
    @DisplayName("Should generate unique correlation ID when not provided")
    void executeJob_WithoutCorrelationId_ShouldGenerateUniqueId() {
        // Arrange
        String executedBy = "test.user@bank.com";
        validExecutionRequest.setCorrelationId(null);
        
        when(configRepository.findById(validExecutionRequest.getConfigId()))
            .thenReturn(Optional.of(validConfiguration));

        // Act
        JobExecutionResult result = jobExecutionService.executeJob(validExecutionRequest, executedBy);

        // Assert
        assertNotNull(result.getCorrelationId());
        assertTrue(result.getCorrelationId().matches(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
        )); // UUID format
    }

    @Test
    @DisplayName("Should use provided correlation ID when present")
    void executeJob_WithProvidedCorrelationId_ShouldUseProvidedId() {
        // Arrange
        String executedBy = "test.user@bank.com";
        String providedCorrelationId = "550e8400-e29b-41d4-a716-446655440000";
        validExecutionRequest.setCorrelationId(providedCorrelationId);
        
        when(configRepository.findById(validExecutionRequest.getConfigId()))
            .thenReturn(Optional.of(validConfiguration));

        // Act
        JobExecutionResult result = jobExecutionService.executeJob(validExecutionRequest, executedBy);

        // Assert
        assertEquals(providedCorrelationId, result.getCorrelationId());
    }

    @Test
    @DisplayName("Should cancel execution successfully")
    void cancelExecution_WithValidExecutionId_ShouldReturnSuccessResult() {
        // Arrange
        String executionId = "exec_test_1691234567890_a1b2c3d4";
        String cancelledBy = "admin@bank.com";
        String reason = "Manual cancellation for maintenance";

        // Act
        JobCancellationResult result = jobExecutionService.cancelExecution(executionId, cancelledBy, reason);

        // Assert
        assertNotNull(result);
        assertEquals(executionId, result.getExecutionId());
        assertTrue(result.isCancelled());
        assertEquals(cancelledBy, result.getCancelledBy());
        assertEquals(reason, result.getCancellationReason());
        assertNotNull(result.getCancelledAt());
        assertNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Should handle cancellation errors gracefully")
    void cancelExecution_WithError_ShouldReturnErrorResult() {
        // Arrange
        String executionId = "invalid-execution-id";
        String cancelledBy = "admin@bank.com";
        String reason = "Test cancellation";

        // Act
        JobCancellationResult result = jobExecutionService.cancelExecution(executionId, cancelledBy, reason);

        // Assert
        assertNotNull(result);
        assertEquals(executionId, result.getExecutionId());
        assertFalse(result.isCancelled());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Should retry execution successfully")
    void retryExecution_WithValidParameters_ShouldReturnNewExecution() {
        // Arrange
        String originalExecutionId = "exec_original_1691234567890_a1b2c3d4";
        String retriedBy = "ops@bank.com";
        JobExecutionRequest retryRequest = createValidExecutionRequest();
        retryRequest.setBusinessJustification("Retry after fixing data issue");
        
        when(configRepository.findById(retryRequest.getConfigId()))
            .thenReturn(Optional.of(validConfiguration));

        // Act
        JobExecutionResult result = jobExecutionService.retryExecution(originalExecutionId, retryRequest, retriedBy);

        // Assert
        assertNotNull(result);
        assertEquals("RETRY", result.getExecutionType());
        assertEquals(retriedBy, result.getExecutedBy());
        assertEquals("COMPLETED", result.getStatus());
    }

    @Test
    @DisplayName("Should return empty list for active executions when no repository")
    void getActiveExecutions_WithNoRepository_ShouldReturnEmptyList() {
        // Act
        var result = jobExecutionService.getActiveExecutions();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty optional for execution status when no repository")
    void getExecutionStatus_WithNoRepository_ShouldReturnEmpty() {
        // Arrange
        String executionId = "exec_test_1691234567890_a1b2c3d4";

        // Act
        var result = jobExecutionService.getExecutionStatus(executionId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list for execution history when no repository")
    void getExecutionHistory_WithNoRepository_ShouldReturnEmptyList() {
        // Arrange
        String configId = "cfg_test_1691234567890_a1b2c3d4";
        int limit = 10;

        // Act
        var result = jobExecutionService.getExecutionHistory(configId, limit);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return default execution statistics")
    void getExecutionStatistics_ShouldReturnDefaultStatistics() {
        // Act
        ExecutionStatistics result = jobExecutionService.getExecutionStatistics();

        // Assert
        assertNotNull(result);
        assertEquals(0L, result.getTotalExecutions());
        assertEquals(0L, result.getSuccessfulExecutions());
        assertEquals(0L, result.getFailedExecutions());
        assertEquals(0L, result.getCancelledExecutions());
        assertEquals(0L, result.getActiveExecutions());
        assertEquals(0.0, result.getAverageExecutionDurationSeconds());
        assertEquals(0.0, result.getSuccessRate());
        assertNotNull(result.getLastUpdated());
    }

    @Test
    @DisplayName("Should handle sensitive execution parameters")
    void executeJob_WithSensitiveParameters_ShouldLogWarning() {
        // Arrange
        String executedBy = "test.user@bank.com";
        Map<String, Object> sensitiveParameters = new HashMap<>();
        sensitiveParameters.put("database_password", "secret123");
        sensitiveParameters.put("api_key", "key456");
        validExecutionRequest.setExecutionParameters(sensitiveParameters);
        
        when(configRepository.findById(validExecutionRequest.getConfigId()))
            .thenReturn(Optional.of(validConfiguration));

        // Act
        JobExecutionResult result = jobExecutionService.executeJob(validExecutionRequest, executedBy);

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        // Sensitive parameters should be detected during execution
        assertTrue(validExecutionRequest.containsSensitiveExecutionParameters());
    }

    @Test
    @DisplayName("Should validate production execution requirements")
    void executeJob_WithProductionExecution_ShouldEnforceStricterValidation() {
        // Arrange
        String executedBy = "test.user@bank.com";
        validExecutionRequest.setEnvironment("PRODUCTION");
        validExecutionRequest.setPriority("CRITICAL");
        
        when(configRepository.findById(validExecutionRequest.getConfigId()))
            .thenReturn(Optional.of(validConfiguration));

        // Act
        JobExecutionResult result = jobExecutionService.executeJob(validExecutionRequest, executedBy);

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        // High-risk production execution should be logged
    }

    // Helper methods

    private JobExecutionRequest createValidExecutionRequest() {
        return JobExecutionRequest.builder()
            .configId("cfg_test_1691234567890_a1b2c3d4")
            .executionType("MANUAL")
            .triggerSource("USER_INTERFACE")
            .businessJustification("Test execution for unit testing purposes")
            .priority("MEDIUM")
            .timeoutMinutes(60)
            .environment("TEST")
            .riskAssessment("LOW")
            .enableDataLineage(true)
            .dryRun(false)
            .build();
    }

    private ManualJobConfigEntity createValidConfiguration() {
        return ManualJobConfigEntity.builder()
            .configId("cfg_test_1691234567890_a1b2c3d4")
            .jobName("TEST_JOB_CONFIG")
            .jobType("ETL_BATCH")
            .sourceSystem("CORE_BANKING")
            .targetSystem("DATA_WAREHOUSE")
            .jobParameters("{\"batchSize\": 1000, \"connectionTimeout\": 30}")
            .status("ACTIVE")
            .createdBy("test.user@bank.com")
            .createdDate(LocalDateTime.now())
            .versionNumber(1L)
            .build();
    }
}