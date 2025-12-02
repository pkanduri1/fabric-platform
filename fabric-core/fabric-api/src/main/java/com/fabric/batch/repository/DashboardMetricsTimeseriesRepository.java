package com.fabric.batch.repository;

import com.fabric.batch.entity.DashboardMetricsTimeseriesEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * US008: Dashboard Metrics Time-Series Repository
 * 
 * Simplified interface for real-time monitoring metrics.
 * Full implementation will be provided when dashboard metrics functionality is required.
 * 
 * Converted from JPA to JdbcTemplate-based repository to eliminate JPA dependencies
 * 
 * TODO: Implement full dashboard metrics functionality when needed
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@Repository
public interface DashboardMetricsTimeseriesRepository {

    // ============================================================================
    // REAL-TIME DASHBOARD QUERIES - METHOD SIGNATURES ONLY
    // ============================================================================

    List<DashboardMetricsTimeseriesEntity> findRecentMetrics(@Param("since") Instant since);
    List<DashboardMetricsTimeseriesEntity> findByExecutionIdAndMetricTimestampAfterOrderByMetricTimestampDesc(String executionId, Instant since);
    Page<DashboardMetricsTimeseriesEntity> findByExecutionIdAndMetricTimestampAfterOrderByMetricTimestampDesc(String executionId, Instant since, Pageable pageable);
    List<DashboardMetricsTimeseriesEntity> findLatestByExecutionIdAndMetricName(@Param("executionId") String executionId, @Param("metricName") String metricName, Pageable pageable);

    // ============================================================================
    // METRIC TYPE QUERIES - METHOD SIGNATURES ONLY
    // ============================================================================

    List<DashboardMetricsTimeseriesEntity> findSystemMetrics(@Param("since") Instant since);
    List<DashboardMetricsTimeseriesEntity> findBusinessMetrics(@Param("since") Instant since);
    List<DashboardMetricsTimeseriesEntity> findSecurityMetrics(@Param("since") Instant since);
    List<DashboardMetricsTimeseriesEntity> findComplianceMetrics(@Param("since") Instant since);

    // ============================================================================
    // METRIC STATUS AND ALERTING QUERIES - METHOD SIGNATURES ONLY
    // ============================================================================

    List<DashboardMetricsTimeseriesEntity> findCriticalMetrics(@Param("since") Instant since);
    List<DashboardMetricsTimeseriesEntity> findWarningMetrics(@Param("since") Instant since);
    List<Object[]> countMetricsByStatus(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    List<DashboardMetricsTimeseriesEntity> findMetricsExceedingThresholds(@Param("cpuThreshold") BigDecimal cpuThreshold, @Param("memoryThreshold") Long memoryThreshold, @Param("durationThreshold") Long durationThreshold, @Param("since") Instant since);

    // ============================================================================
    // AGGREGATION QUERIES FOR ANALYTICS - METHOD SIGNATURES ONLY
    // ============================================================================

    List<Object[]> calculateAveragePerformanceByExecution(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    List<Object[]> calculateBusinessKPISummary(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    List<Object[]> findTopPerformingExecutions(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime, Pageable pageable);

    // ============================================================================
    // TIME-SERIES ANALYSIS QUERIES - METHOD SIGNATURES ONLY
    // ============================================================================

    List<Object[]> findHourlyTrends(@Param("since") Instant since, @Param("metricType") String metricType);
    List<Object[]> findDailyPerformanceSummary(@Param("since") Instant since);

    // ============================================================================
    // CORRELATION AND SOURCE QUERIES - METHOD SIGNATURES ONLY
    // ============================================================================

    List<DashboardMetricsTimeseriesEntity> findByCorrelationIdOrderByMetricTimestampDesc(String correlationId);
    List<DashboardMetricsTimeseriesEntity> findBySourceSystemAndMetricTimestampAfterOrderByMetricTimestampDesc(String sourceSystem, Instant since);
    List<DashboardMetricsTimeseriesEntity> findByUserIdAndMetricTimestampAfterOrderByMetricTimestampDesc(String userId, Instant since);
    List<DashboardMetricsTimeseriesEntity> findByThreadIdAndMetricTimestampAfterOrderByMetricTimestampDesc(String threadId, Instant since);

    // ============================================================================
    // DATA QUALITY AND VALIDATION QUERIES - METHOD SIGNATURES ONLY
    // ============================================================================

    List<DashboardMetricsTimeseriesEntity> findMetricsWithGoodQuality(@Param("threshold") BigDecimal threshold, @Param("since") Instant since);
    List<DashboardMetricsTimeseriesEntity> findMetricsRequiringQualityAttention(@Param("threshold") BigDecimal threshold, @Param("since") Instant since);
    Optional<DashboardMetricsTimeseriesEntity> findByAuditHash(String auditHash);

    // ============================================================================
    // DASHBOARD OPTIMIZATION QUERIES - METHOD SIGNATURES ONLY
    // ============================================================================

    List<DashboardMetricsTimeseriesEntity> findLatestMetricsPerExecution(@Param("since") Instant since);
    List<DashboardMetricsTimeseriesEntity> findMetricsChangesSince(@Param("lastUpdate") Instant lastUpdate);
    Long countActiveExecutions(@Param("since") Instant since);

    // ============================================================================
    // COMPLIANCE AND REPORTING QUERIES - METHOD SIGNATURES ONLY
    // ============================================================================

    List<DashboardMetricsTimeseriesEntity> findMetricsWithComplianceFlag(@Param("flag") String flag, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    List<Object[]> generatePerformanceReport(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    // ============================================================================
    // CLEANUP AND MAINTENANCE QUERIES - METHOD SIGNATURES ONLY
    // ============================================================================

    List<DashboardMetricsTimeseriesEntity> findOldMetricsForCleanup(@Param("cutoffDate") Instant cutoffDate);
    List<Object[]> countMetricsByType();
    List<Object[]> findPartitionStatistics();

    // Standard CRUD operations to replace JpaRepository methods
    <S extends DashboardMetricsTimeseriesEntity> S save(S entity);
    <S extends DashboardMetricsTimeseriesEntity> Iterable<S> saveAll(Iterable<S> entities);
    Optional<DashboardMetricsTimeseriesEntity> findById(Long id);
    boolean existsById(Long id);
    List<DashboardMetricsTimeseriesEntity> findAll();
    Iterable<DashboardMetricsTimeseriesEntity> findAllById(Iterable<Long> ids);
    long count();
    void deleteById(Long id);
    void delete(DashboardMetricsTimeseriesEntity entity);
    void deleteAllById(Iterable<? extends Long> ids);
    void deleteAll(Iterable<? extends DashboardMetricsTimeseriesEntity> entities);
    void deleteAll();
}