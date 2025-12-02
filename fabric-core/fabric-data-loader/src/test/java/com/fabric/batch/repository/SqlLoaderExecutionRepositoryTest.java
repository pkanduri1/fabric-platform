package com.fabric.batch.repository;

import com.fabric.batch.entity.SqlLoaderConfigEntity;
import com.fabric.batch.entity.SqlLoaderExecutionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for SqlLoaderExecutionRepository.
 * Tests execution tracking, performance metrics, and operational queries.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SQL*Loader Execution Repository Tests")
class SqlLoaderExecutionRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private SqlLoaderExecutionRepository repository;
    
    private SqlLoaderConfigEntity testConfig;
    private SqlLoaderExecutionEntity successfulExecution;
    private SqlLoaderExecutionEntity failedExecution;
    private SqlLoaderExecutionEntity inProgressExecution;
    
    @BeforeEach
    void setUp() {
        // Create test configuration
        testConfig = SqlLoaderConfigEntity.builder()
                .configId("TEST-CONFIG-001")
                .jobName("test-job")
                .sourceSystem("TEST")
                .targetTable("CM3INT.TEST_TABLE")
                .createdBy("test-admin")
                .build();
        
        entityManager.persistAndFlush(testConfig);
        
        // Create test executions
        successfulExecution = SqlLoaderExecutionEntity.builder()
                .executionId("EXEC-SUCCESS-001")
                .configId("TEST-CONFIG-001")
                .correlationId("CORR-SUCCESS-001")
                .fileName("test-file-1.dat")
                .filePath("/data/input/test-file-1.dat")
                .fileSize(1024L)
                .executionStatus(SqlLoaderExecutionEntity.ExecutionStatus.SUCCESS)
                .sqlLoaderReturnCode(0)
                .startedDate(LocalDateTime.now().minusHours(2))
                .completedDate(LocalDateTime.now().minusHours(1))
                .durationMs(3600000L) // 1 hour
                .totalRecords(10000L)
                .successfulRecords(9800L)
                .rejectedRecords(200L)
                .skippedRecords(0L)
                .warningRecords(50L)
                .throughputRecordsPerSec(2.78) // 10000 records / 3600 seconds
                .memoryUsageMb(512.0)
                .cpuUsagePercent(25.0)
                .ioReadMb(100.0)
                .ioWriteMb(80.0)
                .retryCount(0)
                .notificationSent("Y")
                .cleanupCompleted("Y")
                .archived("N")
                .createdBy("test-user")
                .createdDate(LocalDateTime.now().minusHours(3))
                .build();
        
        failedExecution = SqlLoaderExecutionEntity.builder()
                .executionId("EXEC-FAILED-001")
                .configId("TEST-CONFIG-001")
                .correlationId("CORR-FAILED-001")
                .fileName("test-file-2.dat")
                .filePath("/data/input/test-file-2.dat")
                .fileSize(2048L)
                .executionStatus(SqlLoaderExecutionEntity.ExecutionStatus.FAILED)
                .sqlLoaderReturnCode(3)
                .startedDate(LocalDateTime.now().minusMinutes(30))
                .completedDate(LocalDateTime.now().minusMinutes(20))
                .durationMs(600000L) // 10 minutes
                .totalRecords(5000L)
                .successfulRecords(0L)
                .rejectedRecords(5000L)
                .skippedRecords(0L)
                .warningRecords(0L)
                .errorMessage("Fatal error occurred during loading")
                .retryCount(1)
                .maxRetries(3)
                .nextRetryDate(LocalDateTime.now().plusMinutes(10))
                .notificationSent("N")
                .cleanupCompleted("N")
                .archived("N")
                .createdBy("test-user")
                .createdDate(LocalDateTime.now().minusMinutes(35))
                .build();
        
        inProgressExecution = SqlLoaderExecutionEntity.builder()
                .executionId("EXEC-PROGRESS-001")
                .configId("TEST-CONFIG-001")
                .correlationId("CORR-PROGRESS-001")
                .fileName("test-file-3.dat")
                .filePath("/data/input/test-file-3.dat")
                .fileSize(4096L)
                .executionStatus(SqlLoaderExecutionEntity.ExecutionStatus.EXECUTING)
                .startedDate(LocalDateTime.now().minusMinutes(5))
                .retryCount(0)
                .notificationSent("N")
                .cleanupCompleted("N")
                .archived("N")
                .createdBy("test-user")
                .createdDate(LocalDateTime.now().minusMinutes(10))
                .build();
        
        // Persist test data
        entityManager.persistAndFlush(successfulExecution);
        entityManager.persistAndFlush(failedExecution);
        entityManager.persistAndFlush(inProgressExecution);
        entityManager.clear();
    }
    
    @Nested
    @DisplayName("Basic Repository Operations")
    class BasicRepositoryOperationsTests {
        
        @Test
        @DisplayName("Should save and retrieve execution")
        void shouldSaveAndRetrieveExecution() {
            SqlLoaderExecutionEntity newExecution = SqlLoaderExecutionEntity.builder()
                    .executionId("EXEC-NEW-001")
                    .configId("TEST-CONFIG-001")
                    .correlationId("CORR-NEW-001")
                    .fileName("new-file.dat")
                    .filePath("/data/input/new-file.dat")
                    .executionStatus(SqlLoaderExecutionEntity.ExecutionStatus.SUBMITTED)
                    .createdBy("admin")
                    .build();
            
            SqlLoaderExecutionEntity saved = repository.save(newExecution);
            
            assertNotNull(saved);
            assertEquals("EXEC-NEW-001", saved.getExecutionId());
            
            Optional<SqlLoaderExecutionEntity> retrieved = repository.findById("EXEC-NEW-001");
            assertTrue(retrieved.isPresent());
            assertEquals("new-file.dat", retrieved.get().getFileName());
        }
        
        @Test
        @DisplayName("Should count total executions")
        void shouldCountTotalExecutions() {
            long totalCount = repository.count();
            assertEquals(3, totalCount);
        }
    }
    
    @Nested
    @DisplayName("Execution Status Query Tests")
    class ExecutionStatusQueryTests {
        
        @Test
        @DisplayName("Should find execution by correlation ID")
        void shouldFindExecutionByCorrelationId() {
            Optional<SqlLoaderExecutionEntity> found = repository
                    .findByCorrelationId("CORR-SUCCESS-001");
            
            assertTrue(found.isPresent());
            assertEquals("EXEC-SUCCESS-001", found.get().getExecutionId());
        }
        
        @Test
        @DisplayName("Should find executions by config ID")
        void shouldFindExecutionsByConfigId() {
            List<SqlLoaderExecutionEntity> executions = repository
                    .findByConfigIdOrderByStartedDateDesc("TEST-CONFIG-001");
            
            assertEquals(3, executions.size());
            // Should be ordered by started date DESC
            assertEquals("EXEC-PROGRESS-001", executions.get(0).getExecutionId());
        }
        
        @Test
        @DisplayName("Should find executions by status")
        void shouldFindExecutionsByStatus() {
            List<SqlLoaderExecutionEntity> successfulExecutions = repository
                    .findByExecutionStatusOrderByStartedDateDesc(SqlLoaderExecutionEntity.ExecutionStatus.SUCCESS);
            
            assertEquals(1, successfulExecutions.size());
            assertEquals("EXEC-SUCCESS-001", successfulExecutions.get(0).getExecutionId());
            
            List<SqlLoaderExecutionEntity> failedExecutions = repository
                    .findByExecutionStatusOrderByStartedDateDesc(SqlLoaderExecutionEntity.ExecutionStatus.FAILED);
            
            assertEquals(1, failedExecutions.size());
            assertEquals("EXEC-FAILED-001", failedExecutions.get(0).getExecutionId());
        }
        
        @Test
        @DisplayName("Should find successful executions by config ID")
        void shouldFindSuccessfulExecutionsByConfigId() {
            List<SqlLoaderExecutionEntity> successfulExecutions = repository
                    .findSuccessfulExecutionsByConfigId("TEST-CONFIG-001");
            
            assertEquals(1, successfulExecutions.size());
            assertEquals("EXEC-SUCCESS-001", successfulExecutions.get(0).getExecutionId());
        }
        
        @Test
        @DisplayName("Should find failed executions by config ID")
        void shouldFindFailedExecutionsByConfigId() {
            List<SqlLoaderExecutionEntity> failedExecutions = repository
                    .findFailedExecutionsByConfigId("TEST-CONFIG-001");
            
            assertEquals(1, failedExecutions.size());
            assertEquals("EXEC-FAILED-001", failedExecutions.get(0).getExecutionId());
        }
        
        @Test
        @DisplayName("Should find executions in progress")
        void shouldFindExecutionsInProgress() {
            List<SqlLoaderExecutionEntity> inProgressExecutions = repository
                    .findExecutionsInProgress();
            
            assertEquals(1, inProgressExecutions.size());
            assertEquals("EXEC-PROGRESS-001", inProgressExecutions.get(0).getExecutionId());
        }
    }
    
    @Nested
    @DisplayName("Performance and Error Analysis Tests")
    class PerformanceErrorAnalysisTests {
        
        @Test
        @DisplayName("Should find executions with errors")
        void shouldFindExecutionsWithErrors() {
            List<SqlLoaderExecutionEntity> errorExecutions = repository
                    .findExecutionsWithErrors();
            
            assertEquals(1, errorExecutions.size());
            assertEquals("EXEC-FAILED-001", errorExecutions.get(0).getExecutionId());
        }
        
        @Test
        @DisplayName("Should find executions by return code")
        void shouldFindExecutionsByReturnCode() {
            List<SqlLoaderExecutionEntity> successCode = repository
                    .findBySqlLoaderReturnCodeOrderByStartedDateDesc(0);
            
            assertEquals(1, successCode.size());
            assertEquals("EXEC-SUCCESS-001", successCode.get(0).getExecutionId());
            
            List<SqlLoaderExecutionEntity> errorCode = repository
                    .findBySqlLoaderReturnCodeOrderByStartedDateDesc(3);
            
            assertEquals(1, errorCode.size());
            assertEquals("EXEC-FAILED-001", errorCode.get(0).getExecutionId());
        }
        
        @Test
        @DisplayName("Should find executions with high error rates")
        void shouldFindExecutionsWithHighErrorRates() {
            // Failed execution has 100% error rate (5000/5000)
            List<SqlLoaderExecutionEntity> highErrorExecutions = repository
                    .findExecutionsWithHighErrorRate(50.0); // > 50% error rate
            
            assertEquals(1, highErrorExecutions.size());
            assertEquals("EXEC-FAILED-001", highErrorExecutions.get(0).getExecutionId());
        }
        
        @Test
        @DisplayName("Should find executions with warnings")
        void shouldFindExecutionsWithWarnings() {
            List<SqlLoaderExecutionEntity> warningExecutions = repository
                    .findExecutionsWithWarnings();
            
            assertEquals(1, warningExecutions.size());
            assertEquals("EXEC-SUCCESS-001", warningExecutions.get(0).getExecutionId());
        }
        
        @Test
        @DisplayName("Should find long-running executions")
        void shouldFindLongRunningExecutions() {
            // 30 minutes threshold
            List<SqlLoaderExecutionEntity> longRunningExecutions = repository
                    .findLongRunningExecutions(1800000L);
            
            assertEquals(1, longRunningExecutions.size());
            assertEquals("EXEC-SUCCESS-001", longRunningExecutions.get(0).getExecutionId());
        }
        
        @Test
        @DisplayName("Should find executions with low throughput")
        void shouldFindExecutionsWithLowThroughput() {
            // 5 records per second threshold
            List<SqlLoaderExecutionEntity> lowThroughputExecutions = repository
                    .findLowThroughputExecutions(5.0);
            
            assertEquals(1, lowThroughputExecutions.size());
            assertEquals("EXEC-SUCCESS-001", lowThroughputExecutions.get(0).getExecutionId());
        }
    }
    
    @Nested
    @DisplayName("Retry and Recovery Tests")
    class RetryRecoveryTests {
        
        @Test
        @DisplayName("Should find retryable executions")
        void shouldFindRetryableExecutions() {
            List<SqlLoaderExecutionEntity> retryableExecutions = repository
                    .findRetryableExecutions();
            
            assertEquals(1, retryableExecutions.size());
            assertEquals("EXEC-FAILED-001", retryableExecutions.get(0).getExecutionId());
        }
        
        @Test
        @DisplayName("Should find executions pending retry")
        void shouldFindExecutionsPendingRetry() {
            List<SqlLoaderExecutionEntity> pendingRetryExecutions = repository
                    .findExecutionsPendingRetry(LocalDateTime.now().plusMinutes(15));
            
            assertEquals(1, pendingRetryExecutions.size());
            assertEquals("EXEC-FAILED-001", pendingRetryExecutions.get(0).getExecutionId());
        }
    }
    
    @Nested
    @DisplayName("Operational Management Tests")
    class OperationalManagementTests {
        
        @Test
        @DisplayName("Should find executions needing cleanup")
        void shouldFindExecutionsNeedingCleanup() {
            List<SqlLoaderExecutionEntity> needingCleanup = repository
                    .findExecutionsNeedingCleanup();
            
            assertEquals(1, needingCleanup.size());
            assertEquals("EXEC-FAILED-001", needingCleanup.get(0).getExecutionId());
        }
        
        @Test
        @DisplayName("Should find executions needing archival")
        void shouldFindExecutionsNeedingArchival() {
            // Set archive threshold to 30 minutes ago
            LocalDateTime archiveThreshold = LocalDateTime.now().minusMinutes(30);
            
            List<SqlLoaderExecutionEntity> needingArchival = repository
                    .findExecutionsNeedingArchival(archiveThreshold);
            
            assertEquals(0, needingArchival.size()); // Successful execution is cleaned up but not old enough
        }
        
        @Test
        @DisplayName("Should find executions needing notification")
        void shouldFindExecutionsNeedingNotification() {
            List<SqlLoaderExecutionEntity> needingNotification = repository
                    .findExecutionsNeedingNotification();
            
            assertEquals(1, needingNotification.size());
            assertEquals("EXEC-FAILED-001", needingNotification.get(0).getExecutionId());
        }
    }
    
    @Nested
    @DisplayName("Statistical Analysis Tests")
    class StatisticalAnalysisTests {
        
        @Test
        @DisplayName("Should count executions by status")
        void shouldCountExecutionsByStatus() {
            Long successCount = repository.countByExecutionStatus(SqlLoaderExecutionEntity.ExecutionStatus.SUCCESS);
            assertEquals(1L, successCount);
            
            Long failedCount = repository.countByExecutionStatus(SqlLoaderExecutionEntity.ExecutionStatus.FAILED);
            assertEquals(1L, failedCount);
            
            Long executingCount = repository.countByExecutionStatus(SqlLoaderExecutionEntity.ExecutionStatus.EXECUTING);
            assertEquals(1L, executingCount);
        }
        
        @Test
        @DisplayName("Should count executions by config ID and status")
        void shouldCountExecutionsByConfigIdAndStatus() {
            Long configSuccessCount = repository
                    .countByConfigIdAndExecutionStatus("TEST-CONFIG-001", SqlLoaderExecutionEntity.ExecutionStatus.SUCCESS);
            assertEquals(1L, configSuccessCount);
            
            Long configFailedCount = repository
                    .countByConfigIdAndExecutionStatus("TEST-CONFIG-001", SqlLoaderExecutionEntity.ExecutionStatus.FAILED);
            assertEquals(1L, configFailedCount);
        }
        
        @Test
        @DisplayName("Should get execution statistics by config ID")
        void shouldGetExecutionStatsByConfigId() {
            List<Object[]> stats = repository.getExecutionStatsByConfigId();
            
            assertNotNull(stats);
            assertEquals(1, stats.size()); // One config ID
            
            Object[] row = stats.get(0);
            assertEquals("TEST-CONFIG-001", row[0]); // Config ID
            assertEquals(3L, row[1]); // Total count
            assertEquals(1L, row[2]); // Success count
            assertEquals(1L, row[3]); // Failed count
            assertNotNull(row[4]); // Avg duration
            assertNotNull(row[5]); // Avg throughput
            assertNotNull(row[6]); // Total records
            assertNotNull(row[7]); // Rejected records
        }
        
        @Test
        @DisplayName("Should get execution statistics by date")
        void shouldGetExecutionStatsByDate() {
            LocalDateTime startDate = LocalDateTime.now().minusDays(1);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);
            
            List<Object[]> stats = repository.getExecutionStatsByDate(startDate, endDate);
            
            assertNotNull(stats);
            assertTrue(stats.size() > 0);
            
            // Verify structure: date, count, success_count, failed_count, avg_duration, total_records
            for (Object[] row : stats) {
                assertNotNull(row[0]); // Date
                assertNotNull(row[1]); // Count
                assertNotNull(row[2]); // Success count
                assertNotNull(row[3]); // Failed count
                assertNotNull(row[5]); // Total records
            }
        }
        
        @Test
        @DisplayName("Should get performance metrics")
        void shouldGetPerformanceMetrics() {
            LocalDateTime startDate = LocalDateTime.now().minusDays(1);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);
            
            Object[] metrics = repository.getPerformanceMetrics(startDate, endDate);
            
            assertNotNull(metrics);
            assertEquals(6, metrics.length);
            
            // Only successful execution should be included in performance metrics
            assertNotNull(metrics[0]); // Avg duration
            assertNotNull(metrics[1]); // Avg throughput
            assertNotNull(metrics[4]); // Max duration
            assertNotNull(metrics[5]); // Min duration
        }
        
        @Test
        @DisplayName("Should find top performing executions")
        void shouldFindTopPerformingExecutions() {
            List<SqlLoaderExecutionEntity> topPerforming = repository.findTopPerformingExecutions();
            
            assertEquals(1, topPerforming.size()); // Only successful execution has throughput
            assertEquals("EXEC-SUCCESS-001", topPerforming.get(0).getExecutionId());
        }
    }
    
    @Nested
    @DisplayName("Resource Utilization Tests")
    class ResourceUtilizationTests {
        
        @Test
        @DisplayName("Should find executions with high memory usage")
        void shouldFindExecutionsWithHighMemoryUsage() {
            List<SqlLoaderExecutionEntity> highMemoryExecutions = repository
                    .findExecutionsWithHighMemoryUsage(400.0); // > 400MB
            
            assertEquals(1, highMemoryExecutions.size());
            assertEquals("EXEC-SUCCESS-001", highMemoryExecutions.get(0).getExecutionId());
        }
        
        @Test
        @DisplayName("Should find executions with high CPU usage")
        void shouldFindExecutionsWithHighCpuUsage() {
            List<SqlLoaderExecutionEntity> highCpuExecutions = repository
                    .findExecutionsWithHighCpuUsage(20.0); // > 20%
            
            assertEquals(1, highCpuExecutions.size());
            assertEquals("EXEC-SUCCESS-001", highCpuExecutions.get(0).getExecutionId());
        }
        
        @Test
        @DisplayName("Should get resource utilization summary")
        void shouldGetResourceUtilizationSummary() {
            LocalDateTime startDate = LocalDateTime.now().minusDays(1);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);
            
            Object[] summary = repository.getResourceUtilizationSummary(startDate, endDate);
            
            assertNotNull(summary);
            assertEquals(8, summary.length);
            
            // Verify averages and maximums are present
            assertNotNull(summary[0]); // Avg memory
            assertNotNull(summary[1]); // Avg CPU
            assertNotNull(summary[2]); // Avg IO read
            assertNotNull(summary[3]); // Avg IO write
            assertNotNull(summary[4]); // Max memory
            assertNotNull(summary[5]); // Max CPU
            assertNotNull(summary[6]); // Max IO read
            assertNotNull(summary[7]); // Max IO write
        }
    }
    
    @Nested
    @DisplayName("Date Range and File Size Tests")
    class DateRangeFileSizeTests {
        
        @Test
        @DisplayName("Should find executions within date range")
        void shouldFindExecutionsWithinDateRange() {
            LocalDateTime startDate = LocalDateTime.now().minusHours(1);
            LocalDateTime endDate = LocalDateTime.now().plusHours(1);
            
            List<SqlLoaderExecutionEntity> recentExecutions = repository
                    .findByStartedDateBetweenOrderByStartedDateDesc(startDate, endDate);
            
            assertEquals(2, recentExecutions.size()); // Failed and in-progress executions
        }
        
        @Test
        @DisplayName("Should find executions by file size range")
        void shouldFindExecutionsByFileSizeRange() {
            List<SqlLoaderExecutionEntity> mediumFiles = repository
                    .findByFileSizeBetween(1500L, 3000L);
            
            assertEquals(1, mediumFiles.size());
            assertEquals("EXEC-FAILED-001", mediumFiles.get(0).getExecutionId());
        }
        
        @Test
        @DisplayName("Should find executions by duration range")
        void shouldFindExecutionsByDurationRange() {
            List<SqlLoaderExecutionEntity> shortExecutions = repository
                    .findByDurationBetween(300000L, 1800000L); // 5-30 minutes
            
            assertEquals(1, shortExecutions.size());
            assertEquals("EXEC-FAILED-001", shortExecutions.get(0).getExecutionId());
        }
    }
    
    @Nested
    @DisplayName("Utility and Helper Tests")
    class UtilityHelperTests {
        
        @Test
        @DisplayName("Should find recent executions")
        void shouldFindRecentExecutions() {
            LocalDateTime recentThreshold = LocalDateTime.now().minusHours(1);
            
            List<SqlLoaderExecutionEntity> recentExecutions = repository
                    .findRecentExecutions(recentThreshold);
            
            assertEquals(2, recentExecutions.size()); // Failed and in-progress executions
        }
        
        @Test
        @DisplayName("Should get error summary")
        void shouldGetErrorSummary() {
            LocalDateTime startDate = LocalDateTime.now().minusDays(1);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);
            
            List<Object[]> errorSummary = repository.getErrorSummary(startDate, endDate);
            
            assertNotNull(errorSummary);
            assertEquals(1, errorSummary.size()); // One error type
            
            Object[] row = errorSummary.get(0);
            assertEquals(3, row[0]); // Return code
            assertEquals(1L, row[1]); // Count
            assertEquals("Fatal error occurred during loading", row[2]); // Error message
        }
        
        @Test
        @DisplayName("Should get throughput distribution")
        void shouldGetThroughputDistribution() {
            LocalDateTime startDate = LocalDateTime.now().minusDays(1);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);
            
            List<Object[]> distribution = repository.getThroughputDistribution(startDate, endDate);
            
            assertNotNull(distribution);
            assertEquals(1, distribution.size()); // One throughput category
            
            Object[] row = distribution.get(0);
            assertEquals("< 1K", row[0]); // Throughput category
            assertEquals(1L, row[1]); // Count
        }
        
        @Test
        @DisplayName("Should find oldest execution")
        void shouldFindOldestExecution() {
            Optional<SqlLoaderExecutionEntity> oldest = repository.findOldestExecution();
            
            assertTrue(oldest.isPresent());
            assertEquals("EXEC-SUCCESS-001", oldest.get().getExecutionId());
        }
        
        @Test
        @DisplayName("Should find latest execution by config ID")
        void shouldFindLatestExecutionByConfigId() {
            Optional<SqlLoaderExecutionEntity> latest = repository
                    .findLatestExecutionByConfigId("TEST-CONFIG-001");
            
            assertTrue(latest.isPresent());
            assertEquals("EXEC-PROGRESS-001", latest.get().getExecutionId());
        }
        
        @Test
        @DisplayName("Should get total records processed by config ID and date range")
        void shouldGetTotalRecordsProcessedByConfigIdAndDateRange() {
            LocalDateTime startDate = LocalDateTime.now().minusDays(1);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);
            
            Long totalRecords = repository
                    .getTotalRecordsProcessedByConfigIdAndDateRange("TEST-CONFIG-001", startDate, endDate);
            
            assertEquals(15000L, totalRecords); // 10000 + 5000 from our test executions
        }
    }
}