package com.fabric.batch.repository;

import com.fabric.batch.entity.DashboardMetricsTimeseriesEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * US008: Comprehensive Unit Tests for DashboardMetricsTimeseriesRepository
 * 
 * Tests time-series metrics repository including:
 * - Real-time dashboard queries with time-based filtering
 * - Performance metrics aggregation and analytics
 * - Metric status queries and alerting data access
 * - Time-series analysis and trending queries
 * - Data quality validation and compliance tracking
 * - Cleanup and maintenance operations
 * 
 * Target: 95%+ code coverage with comprehensive query testing
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@DataJpaTest
@DisplayName("DashboardMetricsTimeseriesRepository Unit Tests")
class DashboardMetricsTimeseriesRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DashboardMetricsTimeseriesRepository repository;

    private DashboardMetricsTimeseriesEntity systemMetric;
    private DashboardMetricsTimeseriesEntity businessMetric;
    private DashboardMetricsTimeseriesEntity securityMetric;
    private DashboardMetricsTimeseriesEntity complianceMetric;
    private Instant testStartTime;
    private Instant testEndTime;

    @BeforeEach
    void setUp() {
        testStartTime = Instant.now().minusSeconds(3600); // 1 hour ago
        testEndTime = Instant.now();

        // Create test data for different metric types
        systemMetric = createMetric(
                "exec-123",
                "SYSTEM_PERFORMANCE",
                "cpu_usage",
                BigDecimal.valueOf(75.5),
                "%",
                "WARNING",
                testStartTime.plusSeconds(600)
        );
        systemMetric.setCpuUsagePercent(BigDecimal.valueOf(85.0));
        systemMetric.setMemoryUsageMb(800L);
        systemMetric.setProcessingDurationMs(3500L);
        systemMetric.setSuccessRatePercent(BigDecimal.valueOf(97.5));

        businessMetric = createMetric(
                "exec-456",
                "BUSINESS_KPI",
                "processing_rate",
                BigDecimal.valueOf(1250.0),
                "records/hour",
                "NORMAL",
                testStartTime.plusSeconds(1200)
        );
        businessMetric.setThroughputPerSecond(BigDecimal.valueOf(0.35));

        securityMetric = createMetric(
                "exec-789",
                "SECURITY_EVENT",
                "auth_failures",
                BigDecimal.valueOf(3.0),
                "count",
                "CRITICAL",
                testStartTime.plusSeconds(1800)
        );

        complianceMetric = createMetric(
                "exec-101",
                "COMPLIANCE_METRIC",
                "data_quality",
                BigDecimal.valueOf(92.5),
                "%",
                "NORMAL",
                testStartTime.plusSeconds(2400)
        );
        complianceMetric.setDataQualityScore(BigDecimal.valueOf(92.5));
        complianceMetric.setComplianceFlags("SOX_REQUIRED,PCI_DSS");

        // Persist test data
        entityManager.persist(systemMetric);
        entityManager.persist(businessMetric);
        entityManager.persist(securityMetric);
        entityManager.persist(complianceMetric);
        entityManager.flush();
    }

    // ============================================================================
    // REAL-TIME DASHBOARD QUERIES TESTS
    // ============================================================================

    @Nested
    @DisplayName("Real-Time Dashboard Queries")
    class RealTimeDashboardTests {

        @Test
        @DisplayName("Should find recent metrics for dashboard display")
        void shouldFindRecentMetricsForDashboardDisplay() {
            // Given
            Instant since = testStartTime.plusSeconds(300);

            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findRecentMetrics(since);

            // Then
            assertThat(results).hasSize(4);
            assertThat(results).extracting(DashboardMetricsTimeseriesEntity::getMetricTimestamp)
                    .allMatch(timestamp -> timestamp.isAfter(since));
        }

        @Test
        @DisplayName("Should find recent metrics by execution ID")
        void shouldFindRecentMetricsByExecutionId() {
            // Given
            String executionId = "exec-123";
            Instant since = testStartTime;

            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findByExecutionIdAndMetricTimestampAfterOrderByMetricTimestampDesc(
                    executionId, since);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getExecutionId()).isEqualTo(executionId);
        }

        @Test
        @DisplayName("Should find recent metrics by execution ID with pagination")
        void shouldFindRecentMetricsByExecutionIdWithPagination() {
            // Given
            String executionId = "exec-123";
            Instant since = testStartTime;
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<DashboardMetricsTimeseriesEntity> page = repository.findByExecutionIdAndMetricTimestampAfterOrderByMetricTimestampDesc(
                    executionId, since, pageable);

            // Then
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent().get(0).getExecutionId()).isEqualTo(executionId);
        }

        @Test
        @DisplayName("Should find latest metrics by execution ID and metric name")
        void shouldFindLatestMetricsByExecutionIdAndMetricName() {
            // Given
            String executionId = "exec-123";
            String metricName = "cpu_usage";
            Pageable pageable = PageRequest.of(0, 1);

            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findLatestByExecutionIdAndMetricName(
                    executionId, metricName, pageable);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getExecutionId()).isEqualTo(executionId);
            assertThat(results.get(0).getMetricName()).isEqualTo(metricName);
        }
    }

    // ============================================================================
    // METRIC TYPE QUERIES TESTS
    // ============================================================================

    @Nested
    @DisplayName("Metric Type Queries")
    class MetricTypeQueriesTests {

        @Test
        @DisplayName("Should find system performance metrics")
        void shouldFindSystemPerformanceMetrics() {
            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findSystemMetrics(testStartTime);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMetricType()).isEqualTo("SYSTEM_PERFORMANCE");
            assertThat(results.get(0).isSystemMetric()).isTrue();
        }

        @Test
        @DisplayName("Should find business KPI metrics")
        void shouldFindBusinessKpiMetrics() {
            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findBusinessMetrics(testStartTime);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMetricType()).isEqualTo("BUSINESS_KPI");
            assertThat(results.get(0).isBusinessMetric()).isTrue();
        }

        @Test
        @DisplayName("Should find security event metrics")
        void shouldFindSecurityEventMetrics() {
            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findSecurityMetrics(testStartTime);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMetricType()).isEqualTo("SECURITY_EVENT");
            assertThat(results.get(0).isSecurityMetric()).isTrue();
        }

        @Test
        @DisplayName("Should find compliance metrics")
        void shouldFindComplianceMetrics() {
            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findComplianceMetrics(testStartTime);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMetricType()).isEqualTo("COMPLIANCE_METRIC");
            assertThat(results.get(0).isComplianceMetric()).isTrue();
        }
    }

    // ============================================================================
    // METRIC STATUS AND ALERTING TESTS
    // ============================================================================

    @Nested
    @DisplayName("Metric Status and Alerting Queries")
    class MetricStatusAlertingTests {

        @Test
        @DisplayName("Should find critical metrics requiring attention")
        void shouldFindCriticalMetricsRequiringAttention() {
            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findCriticalMetrics(testStartTime);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMetricStatus()).isEqualTo("CRITICAL");
            assertThat(results.get(0).isCritical()).isTrue();
        }

        @Test
        @DisplayName("Should find warning metrics")
        void shouldFindWarningMetrics() {
            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findWarningMetrics(testStartTime);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMetricStatus()).isEqualTo("WARNING");
            assertThat(results.get(0).isWarning()).isTrue();
        }

        @Test
        @DisplayName("Should count metrics by status in time range")
        void shouldCountMetricsByStatusInTimeRange() {
            // When
            List<Object[]> results = repository.countMetricsByStatus(testStartTime, testEndTime);

            // Then
            assertThat(results).hasSize(3); // NORMAL, WARNING, CRITICAL
            assertThat(results.get(0)).hasSize(2); // status, count
        }

        @Test
        @DisplayName("Should find metrics exceeding performance thresholds")
        void shouldFindMetricsExceedingPerformanceThresholds() {
            // Given
            BigDecimal cpuThreshold = BigDecimal.valueOf(80.0);
            Long memoryThreshold = 750L;
            Long durationThreshold = 3000L;

            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findMetricsExceedingThresholds(
                    cpuThreshold, memoryThreshold, durationThreshold, testStartTime);

            // Then
            assertThat(results).hasSize(1); // systemMetric exceeds thresholds
            assertThat(results.get(0).getCpuUsagePercent()).isGreaterThan(cpuThreshold);
            assertThat(results.get(0).getMemoryUsageMb()).isGreaterThan(memoryThreshold);
            assertThat(results.get(0).getProcessingDurationMs()).isGreaterThan(durationThreshold);
        }
    }

    // ============================================================================
    // AGGREGATION QUERIES FOR ANALYTICS TESTS
    // ============================================================================

    @Nested
    @DisplayName("Aggregation Queries for Analytics")
    class AggregationAnalyticsTests {

        @Test
        @DisplayName("Should calculate average performance by execution")
        void shouldCalculateAveragePerformanceByExecution() {
            // When
            List<Object[]> results = repository.calculateAveragePerformanceByExecution(testStartTime, testEndTime);

            // Then
            assertThat(results).hasSize(1); // Only one system performance metric
            assertThat(results.get(0)).hasSize(6); // executionId, avgCpu, avgMemory, avgDuration, avgThroughput, count
            assertThat(results.get(0)[0]).isEqualTo("exec-123"); // executionId
        }

        @Test
        @DisplayName("Should calculate business KPI summary")
        void shouldCalculateBusinessKpiSummary() {
            // When
            List<Object[]> results = repository.calculateBusinessKPISummary(testStartTime, testEndTime);

            // Then
            assertThat(results).hasSize(1); // One business KPI metric
            assertThat(results.get(0)).hasSize(6); // metricName, min, max, avg, count, unit
            assertThat(results.get(0)[0]).isEqualTo("processing_rate"); // metricName
        }

        @Test
        @DisplayName("Should find top performing executions by throughput")
        void shouldFindTopPerformingExecutionsByThroughput() {
            // Given
            Pageable pageable = PageRequest.of(0, 5);

            // When
            List<Object[]> results = repository.findTopPerformingExecutions(testStartTime, testEndTime, pageable);

            // Then
            assertThat(results).hasSize(1); // One execution with throughput data
            assertThat(results.get(0)).hasSize(3); // executionId, avgThroughput, count
            assertThat(results.get(0)[0]).isEqualTo("exec-456"); // executionId with throughput
        }
    }

    // ============================================================================
    // TIME-SERIES ANALYSIS TESTS
    // ============================================================================

    @Nested
    @DisplayName("Time-Series Analysis Queries")
    class TimeSeriesAnalysisTests {

        @Test
        @DisplayName("Should find hourly trends for metric type")
        void shouldFindHourlyTrendsForMetricType() {
            // Given
            String metricType = "SYSTEM_PERFORMANCE";

            // When
            List<Object[]> results = repository.findHourlyTrends(testStartTime, metricType);

            // Then
            assertThat(results).isNotEmpty();
            assertThat(results.get(0)).hasSize(6); // hour, metricName, avgValue, minValue, maxValue, sampleCount
        }

        @Test
        @DisplayName("Should find daily performance summary")
        void shouldFindDailyPerformanceSummary() {
            // When
            List<Object[]> results = repository.findDailyPerformanceSummary(testStartTime);

            // Then
            assertThat(results).isNotEmpty();
            assertThat(results.get(0)).hasSize(7); // day, executionId, avgCpu, avgMemory, avgDuration, avgSuccessRate, metricCount
        }
    }

    // ============================================================================
    // CORRELATION AND SOURCE QUERIES TESTS
    // ============================================================================

    @Nested
    @DisplayName("Correlation and Source Queries")
    class CorrelationSourceTests {

        @Test
        @DisplayName("Should find metrics by correlation ID")
        void shouldFindMetricsByCorrelationId() {
            // Given
            String correlationId = "test-correlation-123";
            DashboardMetricsTimeseriesEntity correlatedMetric = createMetric(
                    "exec-corr", "SYSTEM_PERFORMANCE", "correlated_metric",
                    BigDecimal.valueOf(50.0), "%", "NORMAL", testStartTime.plusSeconds(3000));
            correlatedMetric.setCorrelationId(correlationId);
            entityManager.persist(correlatedMetric);
            entityManager.flush();

            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findByCorrelationIdOrderByMetricTimestampDesc(correlationId);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getCorrelationId()).isEqualTo(correlationId);
        }

        @Test
        @DisplayName("Should find metrics by source system")
        void shouldFindMetricsBySourceSystem() {
            // Given
            String sourceSystem = "BATCH_PROCESSING";
            systemMetric.setSourceSystem(sourceSystem);
            entityManager.merge(systemMetric);
            entityManager.flush();

            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findBySourceSystemAndMetricTimestampAfterOrderByMetricTimestampDesc(
                    sourceSystem, testStartTime);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getSourceSystem()).isEqualTo(sourceSystem);
        }

        @Test
        @DisplayName("Should find metrics by user ID")
        void shouldFindMetricsByUserId() {
            // Given
            String userId = "test-user-123";
            businessMetric.setUserId(userId);
            entityManager.merge(businessMetric);
            entityManager.flush();

            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findByUserIdAndMetricTimestampAfterOrderByMetricTimestampDesc(
                    userId, testStartTime);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should find metrics by thread ID")
        void shouldFindMetricsByThreadId() {
            // Given
            String threadId = "thread-worker-001";
            securityMetric.setThreadId(threadId);
            entityManager.merge(securityMetric);
            entityManager.flush();

            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findByThreadIdAndMetricTimestampAfterOrderByMetricTimestampDesc(
                    threadId, testStartTime);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getThreadId()).isEqualTo(threadId);
        }
    }

    // ============================================================================
    // DATA QUALITY AND VALIDATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Data Quality and Validation Queries")
    class DataQualityValidationTests {

        @Test
        @DisplayName("Should find metrics with good data quality")
        void shouldFindMetricsWithGoodDataQuality() {
            // Given
            BigDecimal threshold = BigDecimal.valueOf(90.0);

            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findMetricsWithGoodQuality(threshold, testStartTime);

            // Then
            assertThat(results).hasSize(1); // Only complianceMetric has quality score >= 90
            assertThat(results.get(0).getDataQualityScore()).isGreaterThanOrEqualTo(threshold);
            assertThat(results.get(0).hasGoodDataQuality()).isTrue();
        }

        @Test
        @DisplayName("Should find metrics requiring quality attention")
        void shouldFindMetricsRequiringQualityAttention() {
            // Given
            BigDecimal threshold = BigDecimal.valueOf(95.0);

            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findMetricsRequiringQualityAttention(threshold, testStartTime);

            // Then
            assertThat(results).hasSize(4); // All metrics except those with quality >= 95
        }

        @Test
        @DisplayName("Should validate metric integrity by audit hash")
        void shouldValidateMetricIntegrityByAuditHash() {
            // Given
            String auditHash = "unique-audit-hash-123";
            systemMetric.setAuditHash(auditHash);
            entityManager.merge(systemMetric);
            entityManager.flush();

            // When
            Optional<DashboardMetricsTimeseriesEntity> result = repository.findByAuditHash(auditHash);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getAuditHash()).isEqualTo(auditHash);
        }
    }

    // ============================================================================
    // DASHBOARD OPTIMIZATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Dashboard Optimization Queries")
    class DashboardOptimizationTests {

        @Test
        @DisplayName("Should find latest metrics per execution")
        void shouldFindLatestMetricsPerExecution() {
            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findLatestMetricsPerExecution(testStartTime);

            // Then
            assertThat(results).hasSize(4); // Latest metric for each execution
        }

        @Test
        @DisplayName("Should find metrics changes since last update")
        void shouldFindMetricsChangesSinceLastUpdate() {
            // Given
            Instant lastUpdate = testStartTime.plusSeconds(1500);

            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findMetricsChangesSince(lastUpdate);

            // Then
            assertThat(results).hasSize(2); // securityMetric and complianceMetric
            assertThat(results).extracting(DashboardMetricsTimeseriesEntity::getMetricTimestamp)
                    .allMatch(timestamp -> timestamp.isAfter(lastUpdate));
        }

        @Test
        @DisplayName("Should count active executions")
        void shouldCountActiveExecutions() {
            // When
            Long count = repository.countActiveExecutions(testStartTime);

            // Then
            assertThat(count).isEqualTo(4L); // Four different execution IDs
        }
    }

    // ============================================================================
    // COMPLIANCE AND REPORTING TESTS
    // ============================================================================

    @Nested
    @DisplayName("Compliance and Reporting Queries")
    class ComplianceReportingTests {

        @Test
        @DisplayName("Should find metrics with compliance flag")
        void shouldFindMetricsWithComplianceFlag() {
            // Given
            String flag = "SOX_REQUIRED";

            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findMetricsWithComplianceFlag(
                    flag, testStartTime, testEndTime);

            // Then
            assertThat(results).hasSize(1); // complianceMetric has SOX_REQUIRED flag
            assertThat(results.get(0).hasComplianceFlag(flag)).isTrue();
        }

        @Test
        @DisplayName("Should generate performance report data")
        void shouldGeneratePerformanceReportData() {
            // When
            List<Object[]> results = repository.generatePerformanceReport(testStartTime, testEndTime);

            // Then
            assertThat(results).hasSize(1); // One system performance execution
            assertThat(results.get(0)).hasSize(9); // executionId, sourceSystem, count, avgCpu, avgMemory, avgDuration, avgSuccessRate, startTime, endTime
            assertThat(results.get(0)[0]).isEqualTo("exec-123"); // executionId
        }
    }

    // ============================================================================
    // CLEANUP AND MAINTENANCE TESTS
    // ============================================================================

    @Nested
    @DisplayName("Cleanup and Maintenance Queries")
    class CleanupMaintenanceTests {

        @Test
        @DisplayName("Should find old metrics for cleanup")
        void shouldFindOldMetricsForCleanup() {
            // Given
            Instant cutoffDate = testStartTime.minusSeconds(7200); // 2 hours ago
            DashboardMetricsTimeseriesEntity oldMetric = createMetric(
                    "exec-old", "SYSTEM_PERFORMANCE", "old_metric",
                    BigDecimal.valueOf(10.0), "%", "NORMAL", cutoffDate.minusSeconds(3600));
            entityManager.persist(oldMetric);
            entityManager.flush();

            // When
            List<DashboardMetricsTimeseriesEntity> results = repository.findOldMetricsForCleanup(cutoffDate);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMetricTimestamp()).isBefore(cutoffDate);
        }

        @Test
        @DisplayName("Should count metrics by type for storage analysis")
        void shouldCountMetricsByTypeForStorageAnalysis() {
            // When
            List<Object[]> results = repository.countMetricsByType();

            // Then
            assertThat(results).hasSize(4); // Four different metric types
            assertThat(results.get(0)).hasSize(2); // metricType, count
        }

        @Test
        @DisplayName("Should find partition statistics for maintenance")
        void shouldFindPartitionStatisticsForMaintenance() {
            // Given - add partition IDs to test data
            systemMetric.setPartitionId("partition-1");
            businessMetric.setPartitionId("partition-2");
            entityManager.merge(systemMetric);
            entityManager.merge(businessMetric);
            entityManager.flush();

            // When
            List<Object[]> results = repository.findPartitionStatistics();

            // Then
            assertThat(results).hasSize(2); // Two partitions
            assertThat(results.get(0)).hasSize(4); // partitionId, recordCount, oldest, newest
        }
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private DashboardMetricsTimeseriesEntity createMetric(String executionId, String metricType, String metricName,
                                                        BigDecimal metricValue, String metricUnit, String metricStatus,
                                                        Instant timestamp) {
        return DashboardMetricsTimeseriesEntity.builder()
                .executionId(executionId)
                .metricType(metricType)
                .metricName(metricName)
                .metricValue(metricValue)
                .metricUnit(metricUnit)
                .metricStatus(metricStatus)
                .metricTimestamp(timestamp)
                .auditHash("hash-" + System.currentTimeMillis())
                .createdBy("TEST_SYSTEM")
                .securityClassification("INTERNAL")
                .build();
    }
}