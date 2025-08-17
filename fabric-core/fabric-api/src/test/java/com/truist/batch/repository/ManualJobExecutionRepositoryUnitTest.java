package com.truist.batch.repository;

import com.truist.batch.entity.ManualJobExecutionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ManualJobExecutionRepository using mocks.
 * 
 * This tests the repository interface contract without requiring
 * database integration, making tests faster and more focused.
 * 
 * Tests cover all repository methods including:
 * - Basic CRUD operations
 * - Execution queries and filtering
 * - Performance analytics
 * - Time-based searches
 * - Status management
 * - Statistical operations
 * 
 * @author Senior Full Stack Developer Agent
 * @version 2.0
 * @since US001 Phase 2 - Job Execution Management
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ManualJobExecutionRepository Unit Tests")
class ManualJobExecutionRepositoryUnitTest {

    @Mock
    private ManualJobExecutionRepository repository;

    private ManualJobExecutionEntity completedExecution;
    private ManualJobExecutionEntity runningExecution;
    private ManualJobExecutionEntity failedExecution;
    private ManualJobExecutionEntity cancelledExecution;
    private LocalDateTime baseDateTime;

    @BeforeEach
    void setUp() {
        baseDateTime = LocalDateTime.of(2025, 8, 13, 10, 0, 0);
        
        completedExecution = ManualJobExecutionEntity.builder()
                .executionId("exec_hr_1691234567890_a1b2c3d4")
                .configId("cfg_hr_001")
                .jobName("HR Employee Load")
                .executionType("MANUAL")
                .triggerSource("USER_INTERFACE")
                .status("COMPLETED")
                .startTime(baseDateTime.minusHours(2))
                .endTime(baseDateTime.minusHours(1))
                .durationSeconds(BigDecimal.valueOf(3600.00))
                .recordsProcessed(1000L)
                .recordsSuccess(995L)
                .recordsError(5L)
                .errorPercentage(BigDecimal.valueOf(0.50))
                .errorMessage(null)
                .errorStackTrace(null)
                .retryCount(0)
                .executionParameters("{\"batchSize\":\"1000\",\"validateData\":\"true\"}")
                .executionLog("Execution completed successfully")
                .outputLocation("/data/output/hr/hr_employee_load_20250813.csv")
                .correlationId("corr-12345678-1234-1234-1234-123456789012")
                .monitoringAlertsSent('N')
                .executedBy("john.doe")
                .executionHost("batch-server-01")
                .executionEnvironment("PRODUCTION")
                .createdDate(baseDateTime.minusHours(2))
                .build();

        runningExecution = ManualJobExecutionEntity.builder()
                .executionId("exec_finance_1691234567891_b2c3d4e5")
                .configId("cfg_finance_002")
                .jobName("Finance Daily Reconciliation")
                .executionType("SCHEDULED")
                .triggerSource("SCHEDULER")
                .status("RUNNING")
                .startTime(baseDateTime.minusMinutes(30))
                .endTime(null)
                .durationSeconds(null)
                .recordsProcessed(500L)
                .recordsSuccess(500L)
                .recordsError(0L)
                .errorPercentage(BigDecimal.ZERO)
                .errorMessage(null)
                .errorStackTrace(null)
                .retryCount(0)
                .executionParameters("{\"reconciliationDate\":\"2025-08-13\"}")
                .executionLog("Processing finance records...")
                .outputLocation(null)
                .correlationId("corr-87654321-4321-4321-4321-210987654321")
                .monitoringAlertsSent('N')
                .executedBy("system")
                .executionHost("batch-server-02")
                .executionEnvironment("PRODUCTION")
                .createdDate(baseDateTime.minusMinutes(30))
                .build();

        failedExecution = ManualJobExecutionEntity.builder()
                .executionId("exec_risk_1691234567892_c3d4e5f6")
                .configId("cfg_risk_003")
                .jobName("Risk Data Validation")
                .executionType("MANUAL")
                .triggerSource("API")
                .status("FAILED")
                .startTime(baseDateTime.minusHours(3))
                .endTime(baseDateTime.minusHours(3).plusMinutes(15))
                .durationSeconds(BigDecimal.valueOf(900.00))
                .recordsProcessed(100L)
                .recordsSuccess(75L)
                .recordsError(25L)
                .errorPercentage(BigDecimal.valueOf(25.00))
                .errorMessage("Database connection timeout")
                .errorStackTrace("java.sql.SQLException: Connection timeout...")
                .retryCount(2)
                .executionParameters("{\"validationLevel\":\"STRICT\"}")
                .executionLog("Execution failed after 2 retries")
                .outputLocation(null)
                .correlationId("corr-11111111-2222-3333-4444-555555555555")
                .monitoringAlertsSent('Y')
                .executedBy("jane.smith")
                .executionHost("batch-server-03")
                .executionEnvironment("TEST")
                .createdDate(baseDateTime.minusHours(3))
                .build();

        cancelledExecution = ManualJobExecutionEntity.builder()
                .executionId("exec_compliance_1691234567893_d4e5f6g7")
                .configId("cfg_compliance_004")
                .jobName("Compliance Report Generation")
                .executionType("MANUAL")
                .triggerSource("USER_INTERFACE")
                .status("CANCELLED")
                .startTime(baseDateTime.minusHours(4))
                .endTime(baseDateTime.minusHours(4).plusMinutes(5))
                .durationSeconds(BigDecimal.valueOf(300.00))
                .recordsProcessed(50L)
                .recordsSuccess(50L)
                .recordsError(0L)
                .errorPercentage(BigDecimal.ZERO)
                .errorMessage("Execution cancelled: User requested cancellation")
                .errorStackTrace(null)
                .retryCount(0)
                .executionParameters("{\"reportType\":\"MONTHLY\"}")
                .executionLog("Execution cancelled by user")
                .outputLocation(null)
                .correlationId("corr-99999999-8888-7777-6666-555555555555")
                .monitoringAlertsSent('N')
                .executedBy("admin.user")
                .executionHost("batch-server-01")
                .executionEnvironment("DEVELOPMENT")
                .createdDate(baseDateTime.minusHours(4))
                .build();
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudOperations {

        @Test
        @DisplayName("Should find execution by ID successfully")
        void shouldFindExecutionByIdSuccessfully() {
            // Given
            when(repository.findById("exec_hr_1691234567890_a1b2c3d4")).thenReturn(Optional.of(completedExecution));

            // When
            Optional<ManualJobExecutionEntity> result = repository.findById("exec_hr_1691234567890_a1b2c3d4");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getExecutionId()).isEqualTo("exec_hr_1691234567890_a1b2c3d4");
            assertThat(result.get().getJobName()).isEqualTo("HR Employee Load");
            verify(repository).findById("exec_hr_1691234567890_a1b2c3d4");
        }

        @Test
        @DisplayName("Should return empty optional when execution not found")
        void shouldReturnEmptyOptionalWhenExecutionNotFound() {
            // Given
            when(repository.findById("nonexistent")).thenReturn(Optional.empty());

            // When
            Optional<ManualJobExecutionEntity> result = repository.findById("nonexistent");

            // Then
            assertThat(result).isEmpty();
            verify(repository).findById("nonexistent");
        }

        @Test
        @DisplayName("Should find all executions")
        void shouldFindAllExecutions() {
            // Given
            List<ManualJobExecutionEntity> allExecutions = Arrays.asList(
                    completedExecution, runningExecution, failedExecution, cancelledExecution);
            when(repository.findAll()).thenReturn(allExecutions);

            // When
            List<ManualJobExecutionEntity> result = repository.findAll();

            // Then
            assertThat(result).hasSize(4);
            assertThat(result).containsExactlyInAnyOrder(
                    completedExecution, runningExecution, failedExecution, cancelledExecution);
            verify(repository).findAll();
        }

        @Test
        @DisplayName("Should save execution successfully")
        void shouldSaveExecutionSuccessfully() {
            // Given
            ManualJobExecutionEntity newExecution = ManualJobExecutionEntity.builder()
                    .executionId("exec_new_001")
                    .configId("cfg_new_001")
                    .jobName("New Job")
                    .build();
            when(repository.save(newExecution)).thenReturn(newExecution);

            // When
            ManualJobExecutionEntity result = repository.save(newExecution);

            // Then
            assertThat(result).isEqualTo(newExecution);
            verify(repository).save(newExecution);
        }

        @Test
        @DisplayName("Should delete execution by ID")
        void shouldDeleteExecutionById() {
            // Given
            doNothing().when(repository).deleteById("exec_hr_1691234567890_a1b2c3d4");

            // When
            repository.deleteById("exec_hr_1691234567890_a1b2c3d4");

            // Then
            verify(repository).deleteById("exec_hr_1691234567890_a1b2c3d4");
        }

        @Test
        @DisplayName("Should check if execution exists by ID")
        void shouldCheckIfExecutionExistsById() {
            // Given
            when(repository.existsById("exec_hr_1691234567890_a1b2c3d4")).thenReturn(true);
            when(repository.existsById("nonexistent")).thenReturn(false);

            // When & Then
            assertThat(repository.existsById("exec_hr_1691234567890_a1b2c3d4")).isTrue();
            assertThat(repository.existsById("nonexistent")).isFalse();
            
            verify(repository).existsById("exec_hr_1691234567890_a1b2c3d4");
            verify(repository).existsById("nonexistent");
        }

        @Test
        @DisplayName("Should count all executions")
        void shouldCountAllExecutions() {
            // Given
            when(repository.count()).thenReturn(4L);

            // When
            long count = repository.count();

            // Then
            assertThat(count).isEqualTo(4L);
            verify(repository).count();
        }
    }

    @Nested
    @DisplayName("Basic Execution Queries")
    class BasicExecutionQueries {

        @Test
        @DisplayName("Should find execution by correlation ID")
        void shouldFindExecutionByCorrelationId() {
            // Given
            when(repository.findByCorrelationId("corr-12345678-1234-1234-1234-123456789012"))
                    .thenReturn(Optional.of(completedExecution));

            // When
            Optional<ManualJobExecutionEntity> result = repository.findByCorrelationId("corr-12345678-1234-1234-1234-123456789012");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getCorrelationId()).isEqualTo("corr-12345678-1234-1234-1234-123456789012");
            verify(repository).findByCorrelationId("corr-12345678-1234-1234-1234-123456789012");
        }

        @Test
        @DisplayName("Should find executions by configuration ID")
        void shouldFindExecutionsByConfigurationId() {
            // Given
            List<ManualJobExecutionEntity> configExecutions = Arrays.asList(completedExecution);
            when(repository.findByConfigId("cfg_hr_001")).thenReturn(configExecutions);

            // When
            List<ManualJobExecutionEntity> result = repository.findByConfigId("cfg_hr_001");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(completedExecution);
            verify(repository).findByConfigId("cfg_hr_001");
        }

        @Test
        @DisplayName("Should find executions by config ID ordered by start time")
        void shouldFindExecutionsByConfigIdOrderedByStartTime() {
            // Given
            List<ManualJobExecutionEntity> recentExecutions = Arrays.asList(completedExecution);
            when(repository.findByConfigIdOrderByStartTimeDesc("cfg_hr_001", 10))
                    .thenReturn(recentExecutions);

            // When
            List<ManualJobExecutionEntity> result = repository.findByConfigIdOrderByStartTimeDesc("cfg_hr_001", 10);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(completedExecution);
            verify(repository).findByConfigIdOrderByStartTimeDesc("cfg_hr_001", 10);
        }

        @Test
        @DisplayName("Should find executions by job name")
        void shouldFindExecutionsByJobName() {
            // Given
            List<ManualJobExecutionEntity> jobExecutions = Arrays.asList(completedExecution);
            when(repository.findByJobName("HR Employee Load")).thenReturn(jobExecutions);

            // When
            List<ManualJobExecutionEntity> result = repository.findByJobName("HR Employee Load");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(completedExecution);
            verify(repository).findByJobName("HR Employee Load");
        }
    }

    @Nested
    @DisplayName("Status-Based Queries")
    class StatusBasedQueries {

        @Test
        @DisplayName("Should find executions by status")
        void shouldFindExecutionsByStatus() {
            // Given
            List<ManualJobExecutionEntity> completedExecutions = Arrays.asList(completedExecution);
            when(repository.findByStatus("COMPLETED")).thenReturn(completedExecutions);

            // When
            List<ManualJobExecutionEntity> result = repository.findByStatus("COMPLETED");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(completedExecution);
            verify(repository).findByStatus("COMPLETED");
        }

        @Test
        @DisplayName("Should find executions with multiple statuses")
        void shouldFindExecutionsWithMultipleStatuses() {
            // Given
            List<String> statuses = Arrays.asList("FAILED", "CANCELLED");
            List<ManualJobExecutionEntity> problemExecutions = Arrays.asList(failedExecution, cancelledExecution);
            when(repository.findByStatusIn(statuses)).thenReturn(problemExecutions);

            // When
            List<ManualJobExecutionEntity> result = repository.findByStatusIn(statuses);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(failedExecution, cancelledExecution);
            verify(repository).findByStatusIn(statuses);
        }

        @Test
        @DisplayName("Should find active executions")
        void shouldFindActiveExecutions() {
            // Given
            List<ManualJobExecutionEntity> activeExecutions = Arrays.asList(runningExecution);
            when(repository.findActiveExecutions()).thenReturn(activeExecutions);

            // When
            List<ManualJobExecutionEntity> result = repository.findActiveExecutions();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(runningExecution);
            verify(repository).findActiveExecutions();
        }

        @Test
        @DisplayName("Should find long-running executions")
        void shouldFindLongRunningExecutions() {
            // Given
            List<ManualJobExecutionEntity> longRunning = Arrays.asList(runningExecution);
            when(repository.findLongRunningExecutions(60)).thenReturn(longRunning);

            // When
            List<ManualJobExecutionEntity> result = repository.findLongRunningExecutions(60);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(runningExecution);
            verify(repository).findLongRunningExecutions(60);
        }

        @Test
        @DisplayName("Should find failed executions in time range")
        void shouldFindFailedExecutionsInTimeRange() {
            // Given
            LocalDateTime startTime = baseDateTime.minusHours(4);
            LocalDateTime endTime = baseDateTime;
            List<ManualJobExecutionEntity> failedInRange = Arrays.asList(failedExecution);
            when(repository.findFailedExecutionsBetween(startTime, endTime)).thenReturn(failedInRange);

            // When
            List<ManualJobExecutionEntity> result = repository.findFailedExecutionsBetween(startTime, endTime);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(failedExecution);
            verify(repository).findFailedExecutionsBetween(startTime, endTime);
        }
    }

    @Nested
    @DisplayName("Time-Based Queries")
    class TimeBasedQueries {

        @Test
        @DisplayName("Should find executions between time range")
        void shouldFindExecutionsBetweenTimeRange() {
            // Given
            LocalDateTime startTime = baseDateTime.minusHours(3);
            LocalDateTime endTime = baseDateTime.minusHours(1);
            List<ManualJobExecutionEntity> executionsInRange = Arrays.asList(completedExecution, failedExecution);
            when(repository.findExecutionsBetween(startTime, endTime)).thenReturn(executionsInRange);

            // When
            List<ManualJobExecutionEntity> result = repository.findExecutionsBetween(startTime, endTime);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(completedExecution, failedExecution);
            verify(repository).findExecutionsBetween(startTime, endTime);
        }

        @Test
        @DisplayName("Should find executions started after specific time")
        void shouldFindExecutionsStartedAfterSpecificTime() {
            // Given
            LocalDateTime since = baseDateTime.minusHours(1);
            List<ManualJobExecutionEntity> recentExecutions = Arrays.asList(runningExecution);
            when(repository.findByStartTimeAfter(since)).thenReturn(recentExecutions);

            // When
            List<ManualJobExecutionEntity> result = repository.findByStartTimeAfter(since);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(runningExecution);
            verify(repository).findByStartTimeAfter(since);
        }

        @Test
        @DisplayName("Should find completed executions in time range")
        void shouldFindCompletedExecutionsInTimeRange() {
            // Given
            LocalDateTime startTime = baseDateTime.minusHours(3);
            LocalDateTime endTime = baseDateTime;
            List<ManualJobExecutionEntity> completedInRange = Arrays.asList(completedExecution);
            when(repository.findCompletedExecutionsBetween(startTime, endTime)).thenReturn(completedInRange);

            // When
            List<ManualJobExecutionEntity> result = repository.findCompletedExecutionsBetween(startTime, endTime);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(completedExecution);
            verify(repository).findCompletedExecutionsBetween(startTime, endTime);
        }
    }

    @Nested
    @DisplayName("Performance and Analytics Queries")
    class PerformanceAndAnalyticsQueries {

        @Test
        @DisplayName("Should find high error rate executions")
        void shouldFindHighErrorRateExecutions() {
            // Given
            BigDecimal errorThreshold = BigDecimal.valueOf(10.0);
            LocalDateTime startTime = baseDateTime.minusHours(6);
            LocalDateTime endTime = baseDateTime;
            List<ManualJobExecutionEntity> highErrorRate = Arrays.asList(failedExecution);
            when(repository.findHighErrorRateExecutions(errorThreshold, startTime, endTime))
                    .thenReturn(highErrorRate);

            // When
            List<ManualJobExecutionEntity> result = repository.findHighErrorRateExecutions(errorThreshold, startTime, endTime);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(failedExecution);
            verify(repository).findHighErrorRateExecutions(errorThreshold, startTime, endTime);
        }

        @Test
        @DisplayName("Should find slowest executions")
        void shouldFindSlowestExecutions() {
            // Given
            LocalDateTime startTime = baseDateTime.minusHours(6);
            LocalDateTime endTime = baseDateTime;
            List<ManualJobExecutionEntity> slowest = Arrays.asList(completedExecution, failedExecution);
            when(repository.findSlowestExecutions(5, startTime, endTime)).thenReturn(slowest);

            // When
            List<ManualJobExecutionEntity> result = repository.findSlowestExecutions(5, startTime, endTime);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(completedExecution, failedExecution);
            verify(repository).findSlowestExecutions(5, startTime, endTime);
        }

        @Test
        @DisplayName("Should find highest throughput executions")
        void shouldFindHighestThroughputExecutions() {
            // Given
            LocalDateTime startTime = baseDateTime.minusHours(6);
            LocalDateTime endTime = baseDateTime;
            List<ManualJobExecutionEntity> highThroughput = Arrays.asList(completedExecution);
            when(repository.findHighestThroughputExecutions(3, startTime, endTime)).thenReturn(highThroughput);

            // When
            List<ManualJobExecutionEntity> result = repository.findHighestThroughputExecutions(3, startTime, endTime);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(completedExecution);
            verify(repository).findHighestThroughputExecutions(3, startTime, endTime);
        }
    }

    @Nested
    @DisplayName("Advanced Search Queries")
    class AdvancedSearchQueries {

        @Test
        @DisplayName("Should search by multiple criteria")
        void shouldSearchByMultipleCriteria() {
            // Given
            List<ManualJobExecutionEntity> searchResults = Arrays.asList(completedExecution);
            when(repository.findByMultipleCriteria("cfg_hr_001", "HR Employee Load", "COMPLETED", 
                    "john.doe", "PRODUCTION", baseDateTime.minusHours(3), baseDateTime))
                    .thenReturn(searchResults);

            // When
            List<ManualJobExecutionEntity> result = repository.findByMultipleCriteria("cfg_hr_001", "HR Employee Load", "COMPLETED", 
                    "john.doe", "PRODUCTION", baseDateTime.minusHours(3), baseDateTime);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(completedExecution);
            verify(repository).findByMultipleCriteria("cfg_hr_001", "HR Employee Load", "COMPLETED", 
                    "john.doe", "PRODUCTION", baseDateTime.minusHours(3), baseDateTime);
        }

        @Test
        @DisplayName("Should find executions by user in time range")
        void shouldFindExecutionsByUserInTimeRange() {
            // Given
            LocalDateTime startTime = baseDateTime.minusHours(6);
            LocalDateTime endTime = baseDateTime;
            List<ManualJobExecutionEntity> userExecutions = Arrays.asList(completedExecution);
            when(repository.findByExecutedByBetween("john.doe", startTime, endTime)).thenReturn(userExecutions);

            // When
            List<ManualJobExecutionEntity> result = repository.findByExecutedByBetween("john.doe", startTime, endTime);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(completedExecution);
            verify(repository).findByExecutedByBetween("john.doe", startTime, endTime);
        }

        @Test
        @DisplayName("Should find executions by environment")
        void shouldFindExecutionsByEnvironment() {
            // Given
            List<ManualJobExecutionEntity> prodExecutions = Arrays.asList(completedExecution, runningExecution);
            when(repository.findByExecutionEnvironment("PRODUCTION")).thenReturn(prodExecutions);

            // When
            List<ManualJobExecutionEntity> result = repository.findByExecutionEnvironment("PRODUCTION");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(completedExecution, runningExecution);
            verify(repository).findByExecutionEnvironment("PRODUCTION");
        }
    }

    @Nested
    @DisplayName("Bulk Update Operations")
    class BulkUpdateOperations {

        @Test
        @DisplayName("Should update execution status")
        void shouldUpdateExecutionStatus() {
            // Given
            when(repository.updateExecutionStatus("exec_hr_1691234567890_a1b2c3d4", "COMPLETED", baseDateTime))
                    .thenReturn(1);

            // When
            int updatedRows = repository.updateExecutionStatus("exec_hr_1691234567890_a1b2c3d4", "COMPLETED", baseDateTime);

            // Then
            assertThat(updatedRows).isEqualTo(1);
            verify(repository).updateExecutionStatus("exec_hr_1691234567890_a1b2c3d4", "COMPLETED", baseDateTime);
        }

        @Test
        @DisplayName("Should update execution progress")
        void shouldUpdateExecutionProgress() {
            // Given
            when(repository.updateExecutionProgress("exec_finance_1691234567891_b2c3d4e5", 750L, 750L, 0L))
                    .thenReturn(1);

            // When
            int updatedRows = repository.updateExecutionProgress("exec_finance_1691234567891_b2c3d4e5", 750L, 750L, 0L);

            // Then
            assertThat(updatedRows).isEqualTo(1);
            verify(repository).updateExecutionProgress("exec_finance_1691234567891_b2c3d4e5", 750L, 750L, 0L);
        }

        @Test
        @DisplayName("Should mark alerts as sent")
        void shouldMarkAlertsAsSent() {
            // Given
            List<String> executionIds = Arrays.asList("exec_risk_1691234567892_c3d4e5f6");
            when(repository.markAlertsSent(executionIds)).thenReturn(1);

            // When
            int updatedRows = repository.markAlertsSent(executionIds);

            // Then
            assertThat(updatedRows).isEqualTo(1);
            verify(repository).markAlertsSent(executionIds);
        }

        @Test
        @DisplayName("Should cancel active executions by config")
        void shouldCancelActiveExecutionsByConfig() {
            // Given
            when(repository.cancelActiveExecutionsByConfig("cfg_finance_002", "admin", "Emergency maintenance"))
                    .thenReturn(1);

            // When
            int cancelledRows = repository.cancelActiveExecutionsByConfig("cfg_finance_002", "admin", "Emergency maintenance");

            // Then
            assertThat(cancelledRows).isEqualTo(1);
            verify(repository).cancelActiveExecutionsByConfig("cfg_finance_002", "admin", "Emergency maintenance");
        }
    }

    @Nested
    @DisplayName("Statistical Queries")
    class StatisticalQueries {

        @Test
        @DisplayName("Should count executions by status")
        void shouldCountExecutionsByStatus() {
            // Given
            when(repository.countByStatus("COMPLETED")).thenReturn(1L);
            when(repository.countByStatus("RUNNING")).thenReturn(1L);
            when(repository.countByStatus("FAILED")).thenReturn(1L);
            when(repository.countByStatus("CANCELLED")).thenReturn(1L);

            // When & Then
            assertThat(repository.countByStatus("COMPLETED")).isEqualTo(1L);
            assertThat(repository.countByStatus("RUNNING")).isEqualTo(1L);
            assertThat(repository.countByStatus("FAILED")).isEqualTo(1L);
            assertThat(repository.countByStatus("CANCELLED")).isEqualTo(1L);
            
            verify(repository).countByStatus("COMPLETED");
            verify(repository).countByStatus("RUNNING");
            verify(repository).countByStatus("FAILED");
            verify(repository).countByStatus("CANCELLED");
        }

        @Test
        @DisplayName("Should count executions in time range")
        void shouldCountExecutionsInTimeRange() {
            // Given
            LocalDateTime startTime = baseDateTime.minusDays(1);
            LocalDateTime endTime = baseDateTime;
            when(repository.countExecutionsBetween(startTime, endTime)).thenReturn(4L);

            // When
            long count = repository.countExecutionsBetween(startTime, endTime);

            // Then
            assertThat(count).isEqualTo(4L);
            verify(repository).countExecutionsBetween(startTime, endTime);
        }

        @Test
        @DisplayName("Should count successful executions in time range")
        void shouldCountSuccessfulExecutionsInTimeRange() {
            // Given
            LocalDateTime startTime = baseDateTime.minusDays(1);
            LocalDateTime endTime = baseDateTime;
            when(repository.countSuccessfulExecutionsBetween(startTime, endTime)).thenReturn(1L);

            // When
            long successCount = repository.countSuccessfulExecutionsBetween(startTime, endTime);

            // Then
            assertThat(successCount).isEqualTo(1L);
            verify(repository).countSuccessfulExecutionsBetween(startTime, endTime);
        }

        @Test
        @DisplayName("Should get average execution duration")
        void shouldGetAverageExecutionDuration() {
            // Given
            LocalDateTime startTime = baseDateTime.minusDays(1);
            LocalDateTime endTime = baseDateTime;
            BigDecimal avgDuration = BigDecimal.valueOf(1800.00);
            when(repository.getAverageExecutionDuration(startTime, endTime)).thenReturn(avgDuration);

            // When
            BigDecimal result = repository.getAverageExecutionDuration(startTime, endTime);

            // Then
            assertThat(result).isEqualTo(avgDuration);
            verify(repository).getAverageExecutionDuration(startTime, endTime);
        }

        @Test
        @DisplayName("Should get total records processed")
        void shouldGetTotalRecordsProcessed() {
            // Given
            LocalDateTime startTime = baseDateTime.minusDays(1);
            LocalDateTime endTime = baseDateTime;
            when(repository.getTotalRecordsProcessed(startTime, endTime)).thenReturn(1650L);

            // When
            long totalRecords = repository.getTotalRecordsProcessed(startTime, endTime);

            // Then
            assertThat(totalRecords).isEqualTo(1650L);
            verify(repository).getTotalRecordsProcessed(startTime, endTime);
        }

        @Test
        @DisplayName("Should get execution counts by environment")
        void shouldGetExecutionCountsByEnvironment() {
            // Given
            List<Object[]> envCounts = Arrays.asList(
                    new Object[]{"PRODUCTION", 2L},
                    new Object[]{"TEST", 1L},
                    new Object[]{"DEVELOPMENT", 1L}
            );
            when(repository.getExecutionCountsByEnvironment()).thenReturn(envCounts);

            // When
            List<Object[]> result = repository.getExecutionCountsByEnvironment();

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).containsExactlyInAnyOrder(
                    new Object[]{"PRODUCTION", 2L},
                    new Object[]{"TEST", 1L},
                    new Object[]{"DEVELOPMENT", 1L}
            );
            verify(repository).getExecutionCountsByEnvironment();
        }

        @Test
        @DisplayName("Should get execution trends by day")
        void shouldGetExecutionTrendsByDay() {
            // Given
            LocalDateTime startTime = baseDateTime.minusDays(7);
            LocalDateTime endTime = baseDateTime;
            List<Object[]> dailyTrends = Arrays.asList(
                    new Object[]{"2025-08-13", 4L},
                    new Object[]{"2025-08-12", 3L},
                    new Object[]{"2025-08-11", 2L}
            );
            when(repository.getExecutionTrendsByDay(startTime, endTime)).thenReturn(dailyTrends);

            // When
            List<Object[]> result = repository.getExecutionTrendsByDay(startTime, endTime);

            // Then
            assertThat(result).hasSize(3);
            verify(repository).getExecutionTrendsByDay(startTime, endTime);
        }
    }

    @Nested
    @DisplayName("Data Retention and Cleanup")
    class DataRetentionAndCleanup {

        @Test
        @DisplayName("Should find old executions for cleanup")
        void shouldFindOldExecutionsForCleanup() {
            // Given
            LocalDateTime cutoffDate = baseDateTime.minusMonths(6);
            List<String> statuses = Arrays.asList("COMPLETED", "FAILED", "CANCELLED");
            List<String> oldExecutionIds = Arrays.asList("exec_old_001", "exec_old_002");
            when(repository.findOldExecutionsForCleanup(cutoffDate, statuses)).thenReturn(oldExecutionIds);

            // When
            List<String> result = repository.findOldExecutionsForCleanup(cutoffDate, statuses);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder("exec_old_001", "exec_old_002");
            verify(repository).findOldExecutionsForCleanup(cutoffDate, statuses);
        }

        @Test
        @DisplayName("Should archive execution details")
        void shouldArchiveExecutionDetails() {
            // Given
            List<String> executionIds = Arrays.asList("exec_old_001", "exec_old_002");
            when(repository.archiveExecutionDetails(executionIds)).thenReturn(2);

            // When
            int archivedCount = repository.archiveExecutionDetails(executionIds);

            // Then
            assertThat(archivedCount).isEqualTo(2);
            verify(repository).archiveExecutionDetails(executionIds);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("Should handle null parameters gracefully")
        void shouldHandleNullParametersGracefully() {
            // Given
            when(repository.findByJobName(null)).thenReturn(Arrays.asList());
            when(repository.findByExecutionEnvironment(null)).thenReturn(Arrays.asList());

            // When
            List<ManualJobExecutionEntity> result1 = repository.findByJobName(null);
            List<ManualJobExecutionEntity> result2 = repository.findByExecutionEnvironment(null);

            // Then
            assertThat(result1).isEmpty();
            assertThat(result2).isEmpty();
            
            verify(repository).findByJobName(null);
            verify(repository).findByExecutionEnvironment(null);
        }

        @Test
        @DisplayName("Should handle empty search results")
        void shouldHandleEmptySearchResults() {
            // Given
            when(repository.findByMultipleCriteria("nonexistent", "none", "invalid", 
                    "nobody", "nowhere", baseDateTime.minusDays(1), baseDateTime))
                    .thenReturn(Arrays.asList());

            // When
            List<ManualJobExecutionEntity> result = repository.findByMultipleCriteria("nonexistent", "none", "invalid", 
                    "nobody", "nowhere", baseDateTime.minusDays(1), baseDateTime);

            // Then
            assertThat(result).isEmpty();
            verify(repository).findByMultipleCriteria("nonexistent", "none", "invalid", 
                    "nobody", "nowhere", baseDateTime.minusDays(1), baseDateTime);
        }

        @Test
        @DisplayName("Should return zero for non-existent counts")
        void shouldReturnZeroForNonExistentCounts() {
            // Given
            when(repository.countByStatus("NONEXISTENT")).thenReturn(0L);

            // When
            long count = repository.countByStatus("NONEXISTENT");

            // Then
            assertThat(count).isEqualTo(0L);
            verify(repository).countByStatus("NONEXISTENT");
        }

        @Test
        @DisplayName("Should handle null dates in statistics queries")
        void shouldHandleNullDatesInStatisticsQueries() {
            // Given
            when(repository.getAverageExecutionDuration(null, null)).thenReturn(null);

            // When
            BigDecimal result = repository.getAverageExecutionDuration(null, null);

            // Then
            assertThat(result).isNull();
            verify(repository).getAverageExecutionDuration(null, null);
        }
    }
}