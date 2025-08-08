package com.truist.batch.repository;

import com.truist.batch.entity.DashboardMetricsTimeseriesEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * US008: Dashboard Metrics Time-Series Repository
 * 
 * Repository interface for real-time monitoring metrics with:
 * - High-performance time-series queries optimized for partitioned tables
 * - Real-time dashboard data retrieval with efficient indexing
 * - Aggregation queries for business KPIs and system metrics
 * - Performance monitoring and alerting data access
 * - Time-based data filtering for optimal query performance
 * 
 * APPROVED FOR IMPLEMENTATION by Principal Enterprise Architect
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@Repository
public interface DashboardMetricsTimeseriesRepository extends JpaRepository<DashboardMetricsTimeseriesEntity, Long> {

    // ============================================================================
    // REAL-TIME DASHBOARD QUERIES
    // ============================================================================

    /**
     * Find recent metrics for dashboard display (last 5 minutes)
     */
    @Query("SELECT m FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.metricTimestamp >= :since " +
           "ORDER BY m.metricTimestamp DESC")
    List<DashboardMetricsTimeseriesEntity> findRecentMetrics(@Param("since") Instant since);

    /**
     * Find recent metrics by execution ID
     */
    List<DashboardMetricsTimeseriesEntity> findByExecutionIdAndMetricTimestampAfterOrderByMetricTimestampDesc(
            String executionId, Instant since);

    /**
     * Find recent metrics by execution ID with pagination
     */
    Page<DashboardMetricsTimeseriesEntity> findByExecutionIdAndMetricTimestampAfterOrderByMetricTimestampDesc(
            String executionId, Instant since, Pageable pageable);

    /**
     * Find latest metrics by execution ID and metric name
     */
    @Query("SELECT m FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.executionId = :executionId AND m.metricName = :metricName " +
           "ORDER BY m.metricTimestamp DESC")
    List<DashboardMetricsTimeseriesEntity> findLatestByExecutionIdAndMetricName(
            @Param("executionId") String executionId, 
            @Param("metricName") String metricName,
            Pageable pageable);

    // ============================================================================
    // METRIC TYPE QUERIES
    // ============================================================================

    /**
     * Find system performance metrics
     */
    @Query("SELECT m FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.metricType = 'SYSTEM_PERFORMANCE' " +
           "AND m.metricTimestamp >= :since " +
           "ORDER BY m.metricTimestamp DESC")
    List<DashboardMetricsTimeseriesEntity> findSystemMetrics(@Param("since") Instant since);

    /**
     * Find business KPI metrics
     */
    @Query("SELECT m FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.metricType = 'BUSINESS_KPI' " +
           "AND m.metricTimestamp >= :since " +
           "ORDER BY m.metricTimestamp DESC")
    List<DashboardMetricsTimeseriesEntity> findBusinessMetrics(@Param("since") Instant since);

    /**
     * Find security event metrics
     */
    @Query("SELECT m FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.metricType = 'SECURITY_EVENT' " +
           "AND m.metricTimestamp >= :since " +
           "ORDER BY m.metricTimestamp DESC")
    List<DashboardMetricsTimeseriesEntity> findSecurityMetrics(@Param("since") Instant since);

    /**
     * Find compliance metrics
     */
    @Query("SELECT m FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.metricType = 'COMPLIANCE_METRIC' " +
           "AND m.metricTimestamp >= :since " +
           "ORDER BY m.metricTimestamp DESC")
    List<DashboardMetricsTimeseriesEntity> findComplianceMetrics(@Param("since") Instant since);

    // ============================================================================
    // METRIC STATUS AND ALERTING QUERIES
    // ============================================================================

    /**
     * Find critical metrics requiring immediate attention
     */
    @Query("SELECT m FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.metricStatus = 'CRITICAL' " +
           "AND m.metricTimestamp >= :since " +
           "ORDER BY m.metricTimestamp DESC")
    List<DashboardMetricsTimeseriesEntity> findCriticalMetrics(@Param("since") Instant since);

    /**
     * Find warning metrics
     */
    @Query("SELECT m FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.metricStatus = 'WARNING' " +
           "AND m.metricTimestamp >= :since " +
           "ORDER BY m.metricTimestamp DESC")
    List<DashboardMetricsTimeseriesEntity> findWarningMetrics(@Param("since") Instant since);

    /**
     * Count metrics by status in time range
     */
    @Query("SELECT m.metricStatus, COUNT(m) FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.metricTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY m.metricStatus")
    List<Object[]> countMetricsByStatus(
            @Param("startTime") Instant startTime, 
            @Param("endTime") Instant endTime);

    /**
     * Find metrics exceeding performance thresholds
     */
    @Query("SELECT m FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE ((m.cpuUsagePercent IS NOT NULL AND m.cpuUsagePercent > :cpuThreshold) " +
           "OR (m.memoryUsageMb IS NOT NULL AND m.memoryUsageMb > :memoryThreshold) " +
           "OR (m.processingDurationMs IS NOT NULL AND m.processingDurationMs > :durationThreshold)) " +
           "AND m.metricTimestamp >= :since " +
           "ORDER BY m.metricTimestamp DESC")
    List<DashboardMetricsTimeseriesEntity> findMetricsExceedingThresholds(
            @Param("cpuThreshold") BigDecimal cpuThreshold,
            @Param("memoryThreshold") Long memoryThreshold,
            @Param("durationThreshold") Long durationThreshold,
            @Param("since") Instant since);

    // ============================================================================
    // AGGREGATION QUERIES FOR ANALYTICS
    // ============================================================================

    /**
     * Calculate average performance metrics by execution ID
     */
    @Query("SELECT m.executionId, " +
           "AVG(m.cpuUsagePercent), AVG(m.memoryUsageMb), " +
           "AVG(m.processingDurationMs), AVG(m.throughputPerSecond), " +
           "AVG(m.successRatePercent), COUNT(m) " +
           "FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.metricTimestamp BETWEEN :startTime AND :endTime " +
           "AND m.metricType = 'SYSTEM_PERFORMANCE' " +
           "GROUP BY m.executionId " +
           "ORDER BY AVG(m.processingDurationMs) DESC")
    List<Object[]> calculateAveragePerformanceByExecution(
            @Param("startTime") Instant startTime, 
            @Param("endTime") Instant endTime);

    /**
     * Calculate business KPI summaries
     */
    @Query("SELECT m.metricName, " +
           "MIN(m.metricValue), MAX(m.metricValue), AVG(m.metricValue), " +
           "COUNT(m), m.metricUnit " +
           "FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.metricType = 'BUSINESS_KPI' " +
           "AND m.metricTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY m.metricName, m.metricUnit " +
           "ORDER BY m.metricName")
    List<Object[]> calculateBusinessKPISummary(
            @Param("startTime") Instant startTime, 
            @Param("endTime") Instant endTime);

    /**
     * Find top performing executions by throughput
     */
    @Query("SELECT m.executionId, AVG(m.throughputPerSecond), COUNT(m) " +
           "FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.throughputPerSecond IS NOT NULL " +
           "AND m.metricTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY m.executionId " +
           "ORDER BY AVG(m.throughputPerSecond) DESC")
    List<Object[]> findTopPerformingExecutions(
            @Param("startTime") Instant startTime, 
            @Param("endTime") Instant endTime,
            Pageable pageable);

    // ============================================================================
    // TIME-SERIES ANALYSIS QUERIES
    // ============================================================================

    /**
     * Find metrics for trending analysis (hourly aggregation)
     */
    @Query(value = "SELECT DATE_TRUNC('hour', metric_timestamp) as hour, " +
                   "metric_name, AVG(metric_value) as avg_value, " +
                   "MIN(metric_value) as min_value, MAX(metric_value) as max_value, " +
                   "COUNT(*) as sample_count " +
                   "FROM dashboard_metrics_timeseries " +
                   "WHERE metric_timestamp >= :since " +
                   "AND metric_type = :metricType " +
                   "GROUP BY DATE_TRUNC('hour', metric_timestamp), metric_name " +
                   "ORDER BY hour DESC, metric_name", 
           nativeQuery = true)
    List<Object[]> findHourlyTrends(
            @Param("since") Instant since, 
            @Param("metricType") String metricType);

    /**
     * Find daily metric summaries
     */
    @Query(value = "SELECT DATE_TRUNC('day', metric_timestamp) as day, " +
                   "execution_id, " +
                   "AVG(cpu_usage_percent) as avg_cpu, " +
                   "AVG(memory_usage_mb) as avg_memory, " +
                   "AVG(processing_duration_ms) as avg_duration, " +
                   "AVG(success_rate_percent) as avg_success_rate, " +
                   "COUNT(*) as metric_count " +
                   "FROM dashboard_metrics_timeseries " +
                   "WHERE metric_timestamp >= :since " +
                   "AND metric_type = 'SYSTEM_PERFORMANCE' " +
                   "GROUP BY DATE_TRUNC('day', metric_timestamp), execution_id " +
                   "ORDER BY day DESC, execution_id", 
           nativeQuery = true)
    List<Object[]> findDailyPerformanceSummary(@Param("since") Instant since);

    // ============================================================================
    // CORRELATION AND SOURCE QUERIES
    // ============================================================================

    /**
     * Find metrics by correlation ID
     */
    List<DashboardMetricsTimeseriesEntity> findByCorrelationIdOrderByMetricTimestampDesc(String correlationId);

    /**
     * Find metrics by source system
     */
    List<DashboardMetricsTimeseriesEntity> findBySourceSystemAndMetricTimestampAfterOrderByMetricTimestampDesc(
            String sourceSystem, Instant since);

    /**
     * Find metrics by user ID
     */
    List<DashboardMetricsTimeseriesEntity> findByUserIdAndMetricTimestampAfterOrderByMetricTimestampDesc(
            String userId, Instant since);

    /**
     * Find metrics by thread ID for parallel processing analysis
     */
    List<DashboardMetricsTimeseriesEntity> findByThreadIdAndMetricTimestampAfterOrderByMetricTimestampDesc(
            String threadId, Instant since);

    // ============================================================================
    // DATA QUALITY AND VALIDATION QUERIES
    // ============================================================================

    /**
     * Find metrics with good data quality
     */
    @Query("SELECT m FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.dataQualityScore >= :threshold " +
           "AND m.metricTimestamp >= :since " +
           "ORDER BY m.metricTimestamp DESC")
    List<DashboardMetricsTimeseriesEntity> findMetricsWithGoodQuality(
            @Param("threshold") BigDecimal threshold, 
            @Param("since") Instant since);

    /**
     * Find metrics requiring data quality attention
     */
    @Query("SELECT m FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE (m.dataQualityScore IS NULL OR m.dataQualityScore < :threshold) " +
           "AND m.metricTimestamp >= :since " +
           "ORDER BY m.metricTimestamp DESC")
    List<DashboardMetricsTimeseriesEntity> findMetricsRequiringQualityAttention(
            @Param("threshold") BigDecimal threshold, 
            @Param("since") Instant since);

    /**
     * Validate metric integrity by audit hash
     */
    Optional<DashboardMetricsTimeseriesEntity> findByAuditHash(String auditHash);

    // ============================================================================
    // DASHBOARD OPTIMIZATION QUERIES
    // ============================================================================

    /**
     * Find latest metrics for each execution (for dashboard display)
     */
    @Query("SELECT m FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.metricTimestamp = (" +
           "    SELECT MAX(m2.metricTimestamp) " +
           "    FROM DashboardMetricsTimeseriesEntity m2 " +
           "    WHERE m2.executionId = m.executionId " +
           "    AND m2.metricName = m.metricName" +
           ") " +
           "AND m.metricTimestamp >= :since " +
           "ORDER BY m.metricTimestamp DESC")
    List<DashboardMetricsTimeseriesEntity> findLatestMetricsPerExecution(@Param("since") Instant since);

    /**
     * Find metrics changes since last update (for delta broadcasting)
     */
    @Query("SELECT m FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.metricTimestamp > :lastUpdate " +
           "ORDER BY m.metricTimestamp ASC")
    List<DashboardMetricsTimeseriesEntity> findMetricsChangesSince(@Param("lastUpdate") Instant lastUpdate);

    /**
     * Count active executions
     */
    @Query("SELECT COUNT(DISTINCT m.executionId) FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.metricTimestamp >= :since")
    Long countActiveExecutions(@Param("since") Instant since);

    // ============================================================================
    // COMPLIANCE AND REPORTING QUERIES
    // ============================================================================

    /**
     * Find metrics with compliance flags
     */
    @Query("SELECT m FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.complianceFlags IS NOT NULL " +
           "AND m.complianceFlags LIKE %:flag% " +
           "AND m.metricTimestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY m.metricTimestamp DESC")
    List<DashboardMetricsTimeseriesEntity> findMetricsWithComplianceFlag(
            @Param("flag") String flag,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Generate performance report data
     */
    @Query("SELECT m.executionId, m.sourceSystem, " +
           "COUNT(m) as metric_count, " +
           "AVG(m.cpuUsagePercent) as avg_cpu, " +
           "AVG(m.memoryUsageMb) as avg_memory, " +
           "AVG(m.processingDurationMs) as avg_duration, " +
           "AVG(m.successRatePercent) as avg_success_rate, " +
           "MIN(m.metricTimestamp) as start_time, " +
           "MAX(m.metricTimestamp) as end_time " +
           "FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.metricType = 'SYSTEM_PERFORMANCE' " +
           "AND m.metricTimestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY m.executionId, m.sourceSystem " +
           "ORDER BY avg_duration DESC")
    List<Object[]> generatePerformanceReport(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    // ============================================================================
    // CLEANUP AND MAINTENANCE QUERIES
    // ============================================================================

    /**
     * Find old metrics for cleanup (beyond retention period)
     */
    @Query("SELECT m FROM DashboardMetricsTimeseriesEntity m " +
           "WHERE m.metricTimestamp < :cutoffDate " +
           "ORDER BY m.metricTimestamp ASC")
    List<DashboardMetricsTimeseriesEntity> findOldMetricsForCleanup(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Count metrics by type for storage analysis
     */
    @Query("SELECT m.metricType, COUNT(m) FROM DashboardMetricsTimeseriesEntity m " +
           "GROUP BY m.metricType " +
           "ORDER BY COUNT(m) DESC")
    List<Object[]> countMetricsByType();

    /**
     * Find partition information for maintenance
     */
    @Query(value = "SELECT partition_id, COUNT(*) as record_count, " +
                   "MIN(metric_timestamp) as oldest, MAX(metric_timestamp) as newest " +
                   "FROM dashboard_metrics_timeseries " +
                   "WHERE partition_id IS NOT NULL " +
                   "GROUP BY partition_id " +
                   "ORDER BY oldest DESC", 
           nativeQuery = true)
    List<Object[]> findPartitionStatistics();
}