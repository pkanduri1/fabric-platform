package com.fabric.batch.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * US008: Comprehensive Unit Tests for DashboardMetricsTimeseriesEntity
 * 
 * Tests time-series metrics entity including:
 * - Bean validation and constraint checking
 * - Business logic methods and performance thresholds
 * - JPA lifecycle callbacks and auto-updates
 * - Metric status calculation and classification
 * - Data quality assessment and compliance tracking
 * - Performance threshold validation and alerting
 * 
 * Target: 95%+ code coverage with comprehensive edge case testing
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@DisplayName("DashboardMetricsTimeseriesEntity Unit Tests")
class DashboardMetricsTimeseriesEntityTest {

    private Validator validator;
    private DashboardMetricsTimeseriesEntity metricsEntity;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        metricsEntity = DashboardMetricsTimeseriesEntity.builder()
                .metricTimestamp(Instant.now())
                .executionId("exec-123")
                .metricType("SYSTEM_PERFORMANCE")
                .metricName("cpu_usage")
                .metricValue(BigDecimal.valueOf(45.5))
                .metricUnit("%")
                .metricStatus("NORMAL")
                .auditHash("test-hash-123")
                .createdBy("MONITORING_SYSTEM")
                .build();
    }

    // ============================================================================
    // VALIDATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Bean Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate valid metrics entity without errors")
        void shouldValidateValidMetricsEntityWithoutErrors() {
            // When
            Set<ConstraintViolation<DashboardMetricsTimeseriesEntity>> violations = validator.validate(metricsEntity);
            
            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should reject entity with missing required metric timestamp")
        void shouldRejectEntityWithMissingRequiredMetricTimestamp() {
            // Given
            metricsEntity.setMetricTimestamp(null);
            
            // When
            Set<ConstraintViolation<DashboardMetricsTimeseriesEntity>> violations = validator.validate(metricsEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Metric timestamp is required");
        }

        @Test
        @DisplayName("Should reject entity with missing required execution ID")
        void shouldRejectEntityWithMissingRequiredExecutionId() {
            // Given
            metricsEntity.setExecutionId(null);
            
            // When
            Set<ConstraintViolation<DashboardMetricsTimeseriesEntity>> violations = validator.validate(metricsEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Execution ID is required");
        }

        @Test
        @DisplayName("Should reject entity with blank execution ID")
        void shouldRejectEntityWithBlankExecutionId() {
            // Given
            metricsEntity.setExecutionId("   ");
            
            // When
            Set<ConstraintViolation<DashboardMetricsTimeseriesEntity>> violations = validator.validate(metricsEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Execution ID is required");
        }

        @Test
        @DisplayName("Should reject entity with missing required metric type")
        void shouldRejectEntityWithMissingRequiredMetricType() {
            // Given
            metricsEntity.setMetricType(null);
            
            // When
            Set<ConstraintViolation<DashboardMetricsTimeseriesEntity>> violations = validator.validate(metricsEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Metric type is required");
        }

        @Test
        @DisplayName("Should reject entity with missing required metric name")
        void shouldRejectEntityWithMissingRequiredMetricName() {
            // Given
            metricsEntity.setMetricName(null);
            
            // When
            Set<ConstraintViolation<DashboardMetricsTimeseriesEntity>> violations = validator.validate(metricsEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Metric name is required");
        }

        @Test
        @DisplayName("Should reject entity with missing required audit hash")
        void shouldRejectEntityWithMissingRequiredAuditHash() {
            // Given
            metricsEntity.setAuditHash(null);
            
            // When
            Set<ConstraintViolation<DashboardMetricsTimeseriesEntity>> violations = validator.validate(metricsEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Audit hash is required for tamper detection");
        }

        @Test
        @DisplayName("Should reject entity with negative CPU usage")
        void shouldRejectEntityWithNegativeCpuUsage() {
            // Given
            metricsEntity.setCpuUsagePercent(BigDecimal.valueOf(-5.0));
            
            // When
            Set<ConstraintViolation<DashboardMetricsTimeseriesEntity>> violations = validator.validate(metricsEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("CPU usage cannot be negative");
        }

        @Test
        @DisplayName("Should reject entity with CPU usage exceeding 100%")
        void shouldRejectEntityWithCpuUsageExceeding100Percent() {
            // Given
            metricsEntity.setCpuUsagePercent(BigDecimal.valueOf(105.0));
            
            // When
            Set<ConstraintViolation<DashboardMetricsTimeseriesEntity>> violations = validator.validate(metricsEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("CPU usage cannot exceed 100%");
        }

        @Test
        @DisplayName("Should reject entity with negative memory usage")
        void shouldRejectEntityWithNegativeMemoryUsage() {
            // Given
            metricsEntity.setMemoryUsageMb(-100L);
            
            // When
            Set<ConstraintViolation<DashboardMetricsTimeseriesEntity>> violations = validator.validate(metricsEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Memory usage cannot be negative");
        }

        @Test
        @DisplayName("Should reject entity with negative processing duration")
        void shouldRejectEntityWithNegativeProcessingDuration() {
            // Given
            metricsEntity.setProcessingDurationMs(-1000L);
            
            // When
            Set<ConstraintViolation<DashboardMetricsTimeseriesEntity>> violations = validator.validate(metricsEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Processing duration cannot be negative");
        }

        @Test
        @DisplayName("Should reject entity with negative throughput")
        void shouldRejectEntityWithNegativeThroughput() {
            // Given
            metricsEntity.setThroughputPerSecond(BigDecimal.valueOf(-10.0));
            
            // When
            Set<ConstraintViolation<DashboardMetricsTimeseriesEntity>> violations = validator.validate(metricsEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Throughput cannot be negative");
        }

        @Test
        @DisplayName("Should reject entity with success rate below 0%")
        void shouldRejectEntityWithSuccessRateBelow0Percent() {
            // Given
            metricsEntity.setSuccessRatePercent(BigDecimal.valueOf(-5.0));
            
            // When
            Set<ConstraintViolation<DashboardMetricsTimeseriesEntity>> violations = validator.validate(metricsEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Success rate cannot be negative");
        }

        @Test
        @DisplayName("Should reject entity with success rate above 100%")
        void shouldRejectEntityWithSuccessRateAbove100Percent() {
            // Given
            metricsEntity.setSuccessRatePercent(BigDecimal.valueOf(105.0));
            
            // When
            Set<ConstraintViolation<DashboardMetricsTimeseriesEntity>> violations = validator.validate(metricsEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Success rate cannot exceed 100%");
        }

        @Test
        @DisplayName("Should reject entity with negative data quality score")
        void shouldRejectEntityWithNegativeDataQualityScore() {
            // Given
            metricsEntity.setDataQualityScore(BigDecimal.valueOf(-10.0));
            
            // When
            Set<ConstraintViolation<DashboardMetricsTimeseriesEntity>> violations = validator.validate(metricsEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Data quality score cannot be negative");
        }

        @Test
        @DisplayName("Should reject entity with data quality score above 100")
        void shouldRejectEntityWithDataQualityScoreAbove100() {
            // Given
            metricsEntity.setDataQualityScore(BigDecimal.valueOf(150.0));
            
            // When
            Set<ConstraintViolation<DashboardMetricsTimeseriesEntity>> violations = validator.validate(metricsEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Data quality score cannot exceed 100");
        }
    }

    // ============================================================================
    // BUSINESS LOGIC TESTS
    // ============================================================================

    @Nested
    @DisplayName("Business Logic Methods")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should correctly identify warning status")
        void shouldCorrectlyIdentifyWarningStatus() {
            // Given
            metricsEntity.setMetricStatus("WARNING");
            
            // When & Then
            assertThat(metricsEntity.isWarning()).isTrue();
            assertThat(metricsEntity.isCritical()).isFalse();
            assertThat(metricsEntity.isNormal()).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify critical status")
        void shouldCorrectlyIdentifyCriticalStatus() {
            // Given
            metricsEntity.setMetricStatus("CRITICAL");
            
            // When & Then
            assertThat(metricsEntity.isCritical()).isTrue();
            assertThat(metricsEntity.isWarning()).isFalse();
            assertThat(metricsEntity.isNormal()).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify normal status")
        void shouldCorrectlyIdentifyNormalStatus() {
            // Given
            metricsEntity.setMetricStatus("NORMAL");
            
            // When & Then
            assertThat(metricsEntity.isNormal()).isTrue();
            assertThat(metricsEntity.isWarning()).isFalse();
            assertThat(metricsEntity.isCritical()).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify system performance metrics")
        void shouldCorrectlyIdentifySystemPerformanceMetrics() {
            // Given
            metricsEntity.setMetricType("SYSTEM_PERFORMANCE");
            
            // When & Then
            assertThat(metricsEntity.isSystemMetric()).isTrue();
            assertThat(metricsEntity.isBusinessMetric()).isFalse();
            assertThat(metricsEntity.isSecurityMetric()).isFalse();
            assertThat(metricsEntity.isComplianceMetric()).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify business KPI metrics")
        void shouldCorrectlyIdentifyBusinessKpiMetrics() {
            // Given
            metricsEntity.setMetricType("BUSINESS_KPI");
            
            // When & Then
            assertThat(metricsEntity.isBusinessMetric()).isTrue();
            assertThat(metricsEntity.isSystemMetric()).isFalse();
            assertThat(metricsEntity.isSecurityMetric()).isFalse();
            assertThat(metricsEntity.isComplianceMetric()).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify security event metrics")
        void shouldCorrectlyIdentifySecurityEventMetrics() {
            // Given
            metricsEntity.setMetricType("SECURITY_EVENT");
            
            // When & Then
            assertThat(metricsEntity.isSecurityMetric()).isTrue();
            assertThat(metricsEntity.isSystemMetric()).isFalse();
            assertThat(metricsEntity.isBusinessMetric()).isFalse();
            assertThat(metricsEntity.isComplianceMetric()).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify compliance metrics")
        void shouldCorrectlyIdentifyComplianceMetrics() {
            // Given
            metricsEntity.setMetricType("COMPLIANCE_METRIC");
            
            // When & Then
            assertThat(metricsEntity.isComplianceMetric()).isTrue();
            assertThat(metricsEntity.isSystemMetric()).isFalse();
            assertThat(metricsEntity.isBusinessMetric()).isFalse();
            assertThat(metricsEntity.isSecurityMetric()).isFalse();
        }

        @Test
        @DisplayName("Should calculate metric age in minutes")
        void shouldCalculateMetricAgeInMinutes() {
            // Given
            Instant pastTimestamp = Instant.now().minusSeconds(300); // 5 minutes ago
            metricsEntity.setMetricTimestamp(pastTimestamp);
            
            // When
            long ageMinutes = metricsEntity.getMetricAgeMinutes();
            
            // Then
            assertThat(ageMinutes).isBetween(4L, 6L); // Allow some variance
        }

        @Test
        @DisplayName("Should handle null metric timestamp gracefully")
        void shouldHandleNullMetricTimestampGracefully() {
            // Given
            metricsEntity.setMetricTimestamp(null);
            
            // When
            long ageMinutes = metricsEntity.getMetricAgeMinutes();
            
            // Then
            assertThat(ageMinutes).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should identify recent metrics correctly")
        void shouldIdentifyRecentMetricsCorrectly() {
            // Given - recent metric
            metricsEntity.setMetricTimestamp(Instant.now().minusSeconds(180)); // 3 minutes ago
            
            // When & Then
            assertThat(metricsEntity.isRecentMetric()).isTrue();
            
            // Given - old metric
            metricsEntity.setMetricTimestamp(Instant.now().minusSeconds(600)); // 10 minutes ago
            
            // When & Then
            assertThat(metricsEntity.isRecentMetric()).isFalse();
        }

        @Test
        @DisplayName("Should identify good data quality correctly")
        void shouldIdentifyGoodDataQualityCorrectly() {
            // Given - good quality
            metricsEntity.setDataQualityScore(BigDecimal.valueOf(90.0));
            
            // When & Then
            assertThat(metricsEntity.hasGoodDataQuality()).isTrue();
            
            // Given - poor quality
            metricsEntity.setDataQualityScore(BigDecimal.valueOf(70.0));
            
            // When & Then
            assertThat(metricsEntity.hasGoodDataQuality()).isFalse();
            
            // Given - null quality
            metricsEntity.setDataQualityScore(null);
            
            // When & Then
            assertThat(metricsEntity.hasGoodDataQuality()).isFalse();
        }

        @Test
        @DisplayName("Should format metric value with unit")
        void shouldFormatMetricValueWithUnit() {
            // Given
            metricsEntity.setMetricValue(BigDecimal.valueOf(75.5));
            metricsEntity.setMetricUnit("%");
            
            // When
            String formatted = metricsEntity.getFormattedValue();
            
            // Then
            assertThat(formatted).isEqualTo("75.50 %");
        }

        @Test
        @DisplayName("Should format metric value without unit")
        void shouldFormatMetricValueWithoutUnit() {
            // Given
            metricsEntity.setMetricValue(BigDecimal.valueOf(1000.0));
            metricsEntity.setMetricUnit(null);
            
            // When
            String formatted = metricsEntity.getFormattedValue();
            
            // Then
            assertThat(formatted).isEqualTo("1000.00");
        }

        @Test
        @DisplayName("Should handle null metric value in formatting")
        void shouldHandleNullMetricValueInFormatting() {
            // Given
            metricsEntity.setMetricValue(null);
            
            // When
            String formatted = metricsEntity.getFormattedValue();
            
            // Then
            assertThat(formatted).isEqualTo("N/A");
        }
    }

    // ============================================================================
    // PERFORMANCE THRESHOLD TESTS
    // ============================================================================

    @Nested
    @DisplayName("Performance Threshold Tests")
    class PerformanceThresholdTests {

        @Test
        @DisplayName("Should accept performance within acceptable thresholds")
        void shouldAcceptPerformanceWithinAcceptableThresholds() {
            // Given - all metrics within acceptable range
            metricsEntity.setCpuUsagePercent(BigDecimal.valueOf(70.0)); // < 90%
            metricsEntity.setMemoryUsageMb(800L); // < 1000MB
            metricsEntity.setProcessingDurationMs(3000L); // < 5000ms
            metricsEntity.setSuccessRatePercent(BigDecimal.valueOf(98.0)); // >= 95%
            
            // When
            boolean acceptable = metricsEntity.isPerformanceAcceptable();
            
            // Then
            assertThat(acceptable).isTrue();
        }

        @Test
        @DisplayName("Should reject performance with excessive CPU usage")
        void shouldRejectPerformanceWithExcessiveCpuUsage() {
            // Given
            metricsEntity.setCpuUsagePercent(BigDecimal.valueOf(95.0)); // > 90%
            metricsEntity.setMemoryUsageMb(500L);
            metricsEntity.setProcessingDurationMs(2000L);
            metricsEntity.setSuccessRatePercent(BigDecimal.valueOf(98.0));
            
            // When
            boolean acceptable = metricsEntity.isPerformanceAcceptable();
            
            // Then
            assertThat(acceptable).isFalse();
        }

        @Test
        @DisplayName("Should reject performance with excessive memory usage")
        void shouldRejectPerformanceWithExcessiveMemoryUsage() {
            // Given
            metricsEntity.setCpuUsagePercent(BigDecimal.valueOf(70.0));
            metricsEntity.setMemoryUsageMb(1200L); // > 1000MB
            metricsEntity.setProcessingDurationMs(2000L);
            metricsEntity.setSuccessRatePercent(BigDecimal.valueOf(98.0));
            
            // When
            boolean acceptable = metricsEntity.isPerformanceAcceptable();
            
            // Then
            assertThat(acceptable).isFalse();
        }

        @Test
        @DisplayName("Should reject performance with excessive processing duration")
        void shouldRejectPerformanceWithExcessiveProcessingDuration() {
            // Given
            metricsEntity.setCpuUsagePercent(BigDecimal.valueOf(70.0));
            metricsEntity.setMemoryUsageMb(500L);
            metricsEntity.setProcessingDurationMs(6000L); // > 5000ms
            metricsEntity.setSuccessRatePercent(BigDecimal.valueOf(98.0));
            
            // When
            boolean acceptable = metricsEntity.isPerformanceAcceptable();
            
            // Then
            assertThat(acceptable).isFalse();
        }

        @Test
        @DisplayName("Should reject performance with low success rate")
        void shouldRejectPerformanceWithLowSuccessRate() {
            // Given
            metricsEntity.setCpuUsagePercent(BigDecimal.valueOf(70.0));
            metricsEntity.setMemoryUsageMb(500L);
            metricsEntity.setProcessingDurationMs(2000L);
            metricsEntity.setSuccessRatePercent(BigDecimal.valueOf(90.0)); // < 95%
            
            // When
            boolean acceptable = metricsEntity.isPerformanceAcceptable();
            
            // Then
            assertThat(acceptable).isFalse();
        }

        @Test
        @DisplayName("Should update metric status based on performance")
        void shouldUpdateMetricStatusBasedOnPerformance() {
            // Given - critical performance
            metricsEntity.setCpuUsagePercent(BigDecimal.valueOf(95.0));
            
            // When
            metricsEntity.updateMetricStatus();
            
            // Then
            assertThat(metricsEntity.getMetricStatus()).isEqualTo("CRITICAL");
            
            // Given - warning performance
            metricsEntity.setCpuUsagePercent(BigDecimal.valueOf(85.0)); // Warning threshold
            metricsEntity.setMemoryUsageMb(500L);
            metricsEntity.setProcessingDurationMs(2000L);
            metricsEntity.setSuccessRatePercent(BigDecimal.valueOf(98.0));
            
            // When
            metricsEntity.updateMetricStatus();
            
            // Then
            assertThat(metricsEntity.getMetricStatus()).isEqualTo("WARNING");
            
            // Given - normal performance
            metricsEntity.setCpuUsagePercent(BigDecimal.valueOf(60.0));
            
            // When
            metricsEntity.updateMetricStatus();
            
            // Then
            assertThat(metricsEntity.getMetricStatus()).isEqualTo("NORMAL");
        }
    }

    // ============================================================================
    // COMPLIANCE FLAG TESTS
    // ============================================================================

    @Nested
    @DisplayName("Compliance Flag Management")
    class ComplianceFlagTests {

        @Test
        @DisplayName("Should add compliance flag to empty flags")
        void shouldAddComplianceFlagToEmptyFlags() {
            // Given
            metricsEntity.setComplianceFlags(null);
            
            // When
            metricsEntity.addComplianceFlag("SOX_REQUIRED");
            
            // Then
            assertThat(metricsEntity.getComplianceFlags()).isEqualTo("SOX_REQUIRED");
        }

        @Test
        @DisplayName("Should add compliance flag to existing flags")
        void shouldAddComplianceFlagToExistingFlags() {
            // Given
            metricsEntity.setComplianceFlags("PCI_DSS");
            
            // When
            metricsEntity.addComplianceFlag("SOX_REQUIRED");
            
            // Then
            assertThat(metricsEntity.getComplianceFlags()).isEqualTo("PCI_DSS,SOX_REQUIRED");
        }

        @Test
        @DisplayName("Should not duplicate existing compliance flags")
        void shouldNotDuplicateExistingComplianceFlags() {
            // Given
            metricsEntity.setComplianceFlags("SOX_REQUIRED,PCI_DSS");
            
            // When
            metricsEntity.addComplianceFlag("SOX_REQUIRED");
            
            // Then
            assertThat(metricsEntity.getComplianceFlags()).isEqualTo("SOX_REQUIRED,PCI_DSS");
        }

        @Test
        @DisplayName("Should check for specific compliance flags")
        void shouldCheckForSpecificComplianceFlags() {
            // Given
            metricsEntity.setComplianceFlags("SOX_REQUIRED,PCI_DSS,GDPR");
            
            // When & Then
            assertThat(metricsEntity.hasComplianceFlag("SOX_REQUIRED")).isTrue();
            assertThat(metricsEntity.hasComplianceFlag("PCI_DSS")).isTrue();
            assertThat(metricsEntity.hasComplianceFlag("GDPR")).isTrue();
            assertThat(metricsEntity.hasComplianceFlag("BASEL_III")).isFalse();
        }

        @Test
        @DisplayName("Should handle null compliance flags gracefully")
        void shouldHandleNullComplianceFlagsGracefully() {
            // Given
            metricsEntity.setComplianceFlags(null);
            
            // When & Then
            assertThat(metricsEntity.hasComplianceFlag("SOX_REQUIRED")).isFalse();
        }
    }

    // ============================================================================
    // HASH INTEGRITY AND AUDIT TESTS
    // ============================================================================

    @Nested
    @DisplayName("Hash Integrity and Audit Tests")
    class HashIntegrityTests {

        @Test
        @DisplayName("Should validate hash integrity correctly")
        void shouldValidateHashIntegrityCorrectly() {
            // Given
            String correctHash = "test-hash-123";
            String incorrectHash = "wrong-hash";
            
            // When & Then
            assertThat(metricsEntity.validateHashIntegrity(correctHash)).isTrue();
            assertThat(metricsEntity.validateHashIntegrity(incorrectHash)).isFalse();
            assertThat(metricsEntity.validateHashIntegrity(null)).isFalse();
        }

        @Test
        @DisplayName("Should create comprehensive metric summary")
        void shouldCreateComprehensiveMetricSummary() {
            // Given
            metricsEntity.setMetricId(123L);
            metricsEntity.setMetricValue(BigDecimal.valueOf(75.5));
            metricsEntity.setMetricUnit("%");
            metricsEntity.setDataQualityScore(BigDecimal.valueOf(95.0));
            
            // When
            DashboardMetricsTimeseriesEntity.MetricSummary summary = metricsEntity.createSummary();
            
            // Then
            assertThat(summary.getMetricId()).isEqualTo(123L);
            assertThat(summary.getExecutionId()).isEqualTo("exec-123");
            assertThat(summary.getMetricType()).isEqualTo("SYSTEM_PERFORMANCE");
            assertThat(summary.getMetricName()).isEqualTo("cpu_usage");
            assertThat(summary.getMetricValue()).isEqualTo(BigDecimal.valueOf(75.5));
            assertThat(summary.getMetricUnit()).isEqualTo("%");
            assertThat(summary.getFormattedValue()).isEqualTo("75.50 %");
            assertThat(summary.isHasGoodQuality()).isTrue();
        }
    }

    // ============================================================================
    // JPA LIFECYCLE TESTS
    // ============================================================================

    @Nested
    @DisplayName("JPA Lifecycle Events")
    class JpaLifecycleTests {

        @Test
        @DisplayName("Should set default values on create")
        void shouldSetDefaultValuesOnCreate() {
            // Given
            DashboardMetricsTimeseriesEntity newEntity = DashboardMetricsTimeseriesEntity.builder()
                    .executionId("exec-456")
                    .metricType("BUSINESS_KPI")
                    .metricName("processing_rate")
                    .auditHash("test-hash")
                    .createdBy("SYSTEM")
                    .build();
            
            // When
            newEntity.onCreate();
            
            // Then
            assertThat(newEntity.getMetricTimestamp()).isNotNull();
            assertThat(newEntity.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("Should auto-update metric status on create")
        void shouldAutoUpdateMetricStatusOnCreate() {
            // Given
            DashboardMetricsTimeseriesEntity newEntity = DashboardMetricsTimeseriesEntity.builder()
                    .executionId("exec-789")
                    .metricType("SYSTEM_PERFORMANCE")
                    .metricName("cpu_usage")
                    .cpuUsagePercent(BigDecimal.valueOf(95.0)) // Should trigger CRITICAL
                    .auditHash("test-hash")
                    .createdBy("SYSTEM")
                    .build();
            
            // When
            newEntity.onCreate();
            
            // Then
            assertThat(newEntity.getMetricStatus()).isEqualTo("CRITICAL");
        }

        @Test
        @DisplayName("Should update version and timestamp on update")
        void shouldUpdateVersionAndTimestampOnUpdate() {
            // Given
            metricsEntity.setVersionNumber(1L);
            metricsEntity.setLastModifiedDate(null);
            
            // When
            metricsEntity.onUpdate();
            
            // Then
            assertThat(metricsEntity.getVersionNumber()).isEqualTo(2L);
            assertThat(metricsEntity.getLastModifiedDate()).isNotNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"SYSTEM_PERFORMANCE", "BUSINESS_KPI", "SECURITY_EVENT", "COMPLIANCE_METRIC"})
        @DisplayName("Should validate valid metric types")
        void shouldValidateValidMetricTypes(String metricType) {
            // Given
            metricsEntity.setMetricType(metricType);
            
            // When & Then - should not throw exception
            assertThatCode(() -> metricsEntity.onCreate()).doesNotThrowAnyException();
            assertThatCode(() -> metricsEntity.onUpdate()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject invalid metric type")
        void shouldRejectInvalidMetricType() {
            // Given
            metricsEntity.setMetricType("INVALID_TYPE");
            
            // When & Then
            assertThatThrownBy(() -> metricsEntity.onCreate())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid metric type: INVALID_TYPE");
        }

        @ParameterizedTest
        @ValueSource(strings = {"NORMAL", "WARNING", "CRITICAL", "UNKNOWN"})
        @DisplayName("Should validate valid metric statuses")
        void shouldValidateValidMetricStatuses(String metricStatus) {
            // Given
            metricsEntity.setMetricStatus(metricStatus);
            
            // When & Then - should not throw exception
            assertThatCode(() -> metricsEntity.onCreate()).doesNotThrowAnyException();
            assertThatCode(() -> metricsEntity.onUpdate()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject invalid metric status")
        void shouldRejectInvalidMetricStatus() {
            // Given
            metricsEntity.setMetricStatus("INVALID_STATUS");
            
            // When & Then
            assertThatThrownBy(() -> metricsEntity.onCreate())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid metric status: INVALID_STATUS");
        }
    }

    // ============================================================================
    // EQUALITY AND HASH CODE TESTS
    // ============================================================================

    @Nested
    @DisplayName("Equality and Hash Code Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when metric IDs match")
        void shouldBeEqualWhenMetricIdsMatch() {
            // Given
            DashboardMetricsTimeseriesEntity entity1 = DashboardMetricsTimeseriesEntity.builder()
                    .metricId(123L)
                    .executionId("exec-1")
                    .metricName("cpu_usage")
                    .build();
            
            DashboardMetricsTimeseriesEntity entity2 = DashboardMetricsTimeseriesEntity.builder()
                    .metricId(123L)
                    .executionId("exec-2")
                    .metricName("memory_usage")
                    .build();
            
            // When & Then
            assertThat(entity1).isEqualTo(entity2);
            assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when metric IDs differ")
        void shouldNotBeEqualWhenMetricIdsDiffer() {
            // Given
            DashboardMetricsTimeseriesEntity entity1 = DashboardMetricsTimeseriesEntity.builder()
                    .metricId(123L)
                    .build();
            
            DashboardMetricsTimeseriesEntity entity2 = DashboardMetricsTimeseriesEntity.builder()
                    .metricId(456L)
                    .build();
            
            // When & Then
            assertThat(entity1).isNotEqualTo(entity2);
        }

        @Test
        @DisplayName("Should use composite key for equality when metric ID is null")
        void shouldUseCompositeKeyForEqualityWhenMetricIdIsNull() {
            // Given
            Instant timestamp = Instant.now();
            
            DashboardMetricsTimeseriesEntity entity1 = DashboardMetricsTimeseriesEntity.builder()
                    .executionId("exec-123")
                    .metricName("cpu_usage")
                    .metricTimestamp(timestamp)
                    .build();
            
            DashboardMetricsTimeseriesEntity entity2 = DashboardMetricsTimeseriesEntity.builder()
                    .executionId("exec-123")
                    .metricName("cpu_usage")
                    .metricTimestamp(timestamp)
                    .build();
            
            // When & Then
            assertThat(entity1).isEqualTo(entity2);
            assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
        }

        @Test
        @DisplayName("Should handle null comparisons gracefully")
        void shouldHandleNullComparisonsGracefully() {
            // When & Then
            assertThat(metricsEntity).isNotEqualTo(null);
            assertThat(metricsEntity).isNotEqualTo(new Object());
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            // When & Then
            assertThat(metricsEntity).isEqualTo(metricsEntity);
        }
    }

    // ============================================================================
    // STRING REPRESENTATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("String Representation Tests")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should generate proper toString representation")
        void shouldGenerateProperToStringRepresentation() {
            // Given
            metricsEntity.setMetricId(123L);
            metricsEntity.setMetricType("SYSTEM_PERFORMANCE");
            metricsEntity.setMetricName("cpu_usage");
            metricsEntity.setMetricValue(BigDecimal.valueOf(75.5));
            metricsEntity.setMetricUnit("%");
            metricsEntity.setMetricStatus("NORMAL");
            
            // When
            String toString = metricsEntity.toString();
            
            // Then
            assertThat(toString).contains("DashboardMetric[id=123");
            assertThat(toString).contains("type=SYSTEM_PERFORMANCE");
            assertThat(toString).contains("name=cpu_usage");
            assertThat(toString).contains("value=75.50 %");
            assertThat(toString).contains("status=NORMAL");
        }

        @Test
        @DisplayName("Should handle null values in toString")
        void shouldHandleNullValuesInToString() {
            // Given
            DashboardMetricsTimeseriesEntity entityWithNulls = DashboardMetricsTimeseriesEntity.builder().build();
            
            // When & Then - should not throw exception
            assertThatCode(() -> entityWithNulls.toString()).doesNotThrowAnyException();
        }
    }
}