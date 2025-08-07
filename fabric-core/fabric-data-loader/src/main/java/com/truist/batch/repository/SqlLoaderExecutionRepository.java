package com.truist.batch.repository;

import com.truist.batch.entity.SqlLoaderExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SqlLoaderExecutionEntity operations.
 * Provides comprehensive data access methods for SQL*Loader execution tracking,
 * performance metrics, and operational monitoring.
 */
@Repository
public interface SqlLoaderExecutionRepository extends JpaRepository<SqlLoaderExecutionEntity, String> {
    
    /**
     * Find execution by correlation ID.
     */
    Optional<SqlLoaderExecutionEntity> findByCorrelationId(String correlationId);
    
    /**
     * Find all executions for a specific configuration ordered by start date.
     */
    List<SqlLoaderExecutionEntity> findByConfigIdOrderByStartedDateDesc(String configId);
    
    /**
     * Find executions by status.
     */
    List<SqlLoaderExecutionEntity> findByExecutionStatusOrderByStartedDateDesc(
            SqlLoaderExecutionEntity.ExecutionStatus executionStatus);
    
    /**
     * Find executions by job execution ID.
     */
    List<SqlLoaderExecutionEntity> findByJobExecutionIdOrderByStartedDateDesc(String jobExecutionId);
    
    /**
     * Find executions by file name pattern.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.fileName LIKE %:filePattern% ORDER BY e.startedDate DESC")
    List<SqlLoaderExecutionEntity> findByFileNameContainingOrderByStartedDateDesc(@Param("filePattern") String filePattern);
    
    /**
     * Find successful executions by config ID.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.configId = :configId AND e.executionStatus IN ('SUCCESS', 'SUCCESS_WITH_WARNINGS') ORDER BY e.startedDate DESC")
    List<SqlLoaderExecutionEntity> findSuccessfulExecutionsByConfigId(@Param("configId") String configId);
    
    /**
     * Find failed executions by config ID.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.configId = :configId AND e.executionStatus IN ('FAILED', 'CANCELLED', 'TIMEOUT') ORDER BY e.startedDate DESC")
    List<SqlLoaderExecutionEntity> findFailedExecutionsByConfigId(@Param("configId") String configId);
    
    /**
     * Find executions in progress.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.executionStatus IN ('SUBMITTED', 'GENERATING_CONTROL', 'EXECUTING') ORDER BY e.startedDate")
    List<SqlLoaderExecutionEntity> findExecutionsInProgress();
    
    /**
     * Find executions within date range.
     */
    List<SqlLoaderExecutionEntity> findByStartedDateBetweenOrderByStartedDateDesc(
            LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find completed executions within date range.
     */
    List<SqlLoaderExecutionEntity> findByCompletedDateBetweenOrderByCompletedDateDesc(
            LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find executions by SQL*Loader return code.
     */
    List<SqlLoaderExecutionEntity> findBySqlLoaderReturnCodeOrderByStartedDateDesc(Integer returnCode);
    
    /**
     * Find executions with errors (return code > 0).
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.sqlLoaderReturnCode > 0 ORDER BY e.startedDate DESC")
    List<SqlLoaderExecutionEntity> findExecutionsWithErrors();
    
    /**
     * Find executions with high error rates.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.rejectedRecords > 0 AND (e.rejectedRecords * 100.0 / NULLIF(e.totalRecords, 0)) > :errorRateThreshold ORDER BY (e.rejectedRecords * 100.0 / NULLIF(e.totalRecords, 0)) DESC")
    List<SqlLoaderExecutionEntity> findExecutionsWithHighErrorRate(@Param("errorRateThreshold") Double errorRateThreshold);
    
    /**
     * Find executions with warnings.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.warningRecords > 0 OR e.executionStatus = 'SUCCESS_WITH_WARNINGS' ORDER BY e.startedDate DESC")
    List<SqlLoaderExecutionEntity> findExecutionsWithWarnings();
    
    /**
     * Find long-running executions.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.durationMs > :thresholdMs ORDER BY e.durationMs DESC")
    List<SqlLoaderExecutionEntity> findLongRunningExecutions(@Param("thresholdMs") Long thresholdMs);
    
    /**
     * Find executions with low throughput.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.throughputRecordsPerSec < :thresholdThroughput ORDER BY e.throughputRecordsPerSec")
    List<SqlLoaderExecutionEntity> findLowThroughputExecutions(@Param("thresholdThroughput") Double thresholdThroughput);
    
    /**
     * Find executions by created user.
     */
    List<SqlLoaderExecutionEntity> findByCreatedByOrderByStartedDateDesc(String createdBy);
    
    /**
     * Find retryable executions.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.executionStatus IN ('FAILED', 'TIMEOUT') AND e.retryCount < e.maxRetries ORDER BY e.nextRetryDate")
    List<SqlLoaderExecutionEntity> findRetryableExecutions();
    
    /**
     * Find executions pending retry.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.nextRetryDate <= :currentTime AND e.retryCount < e.maxRetries AND e.executionStatus IN ('FAILED', 'TIMEOUT') ORDER BY e.nextRetryDate")
    List<SqlLoaderExecutionEntity> findExecutionsPendingRetry(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find executions that need cleanup.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.cleanupCompleted = 'N' AND e.executionStatus IN ('SUCCESS', 'SUCCESS_WITH_WARNINGS', 'FAILED', 'CANCELLED') ORDER BY e.completedDate")
    List<SqlLoaderExecutionEntity> findExecutionsNeedingCleanup();
    
    /**
     * Find executions that need archiving.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.archived = 'N' AND e.completedDate < :archiveThreshold AND e.cleanupCompleted = 'Y' ORDER BY e.completedDate")
    List<SqlLoaderExecutionEntity> findExecutionsNeedingArchival(@Param("archiveThreshold") LocalDateTime archiveThreshold);
    
    /**
     * Find executions that need notification.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.notificationSent = 'N' AND e.executionStatus IN ('SUCCESS', 'SUCCESS_WITH_WARNINGS', 'FAILED', 'CANCELLED', 'TIMEOUT') ORDER BY e.completedDate")
    List<SqlLoaderExecutionEntity> findExecutionsNeedingNotification();
    
    /**
     * Count executions by status.
     */
    @Query("SELECT COUNT(e) FROM SqlLoaderExecutionEntity e WHERE e.executionStatus = :status")
    Long countByExecutionStatus(@Param("status") SqlLoaderExecutionEntity.ExecutionStatus status);
    
    /**
     * Count executions by config ID and status.
     */
    Long countByConfigIdAndExecutionStatus(String configId, SqlLoaderExecutionEntity.ExecutionStatus status);
    
    /**
     * Count executions by config ID within date range.
     */
    @Query("SELECT COUNT(e) FROM SqlLoaderExecutionEntity e WHERE e.configId = :configId AND e.startedDate BETWEEN :startDate AND :endDate")
    Long countByConfigIdAndDateRange(@Param("configId") String configId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get execution statistics by config ID.
     */
    @Query("SELECT e.configId, COUNT(e), " +
           "SUM(CASE WHEN e.executionStatus IN ('SUCCESS', 'SUCCESS_WITH_WARNINGS') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN e.executionStatus IN ('FAILED', 'CANCELLED', 'TIMEOUT') THEN 1 ELSE 0 END), " +
           "AVG(e.durationMs), AVG(e.throughputRecordsPerSec), SUM(e.totalRecords), SUM(e.rejectedRecords) " +
           "FROM SqlLoaderExecutionEntity e GROUP BY e.configId")
    List<Object[]> getExecutionStatsByConfigId();
    
    /**
     * Get execution statistics for date range.
     */
    @Query("SELECT DATE(e.startedDate), COUNT(e), " +
           "SUM(CASE WHEN e.executionStatus IN ('SUCCESS', 'SUCCESS_WITH_WARNINGS') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN e.executionStatus IN ('FAILED', 'CANCELLED', 'TIMEOUT') THEN 1 ELSE 0 END), " +
           "AVG(e.durationMs), SUM(e.totalRecords) " +
           "FROM SqlLoaderExecutionEntity e WHERE e.startedDate BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(e.startedDate) ORDER BY DATE(e.startedDate) DESC")
    List<Object[]> getExecutionStatsByDate(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get performance metrics for executions.
     */
    @Query("SELECT AVG(e.durationMs), AVG(e.throughputRecordsPerSec), " +
           "PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY e.durationMs), " +
           "PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY e.durationMs), " +
           "MAX(e.durationMs), MIN(e.durationMs) " +
           "FROM SqlLoaderExecutionEntity e WHERE e.executionStatus IN ('SUCCESS', 'SUCCESS_WITH_WARNINGS') " +
           "AND e.startedDate BETWEEN :startDate AND :endDate")
    Object[] getPerformanceMetrics(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find top performing executions by throughput.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.throughputRecordsPerSec IS NOT NULL ORDER BY e.throughputRecordsPerSec DESC")
    List<SqlLoaderExecutionEntity> findTopPerformingExecutions();
    
    /**
     * Find executions with specific file size range.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.fileSize BETWEEN :minSize AND :maxSize ORDER BY e.fileSize DESC")
    List<SqlLoaderExecutionEntity> findByFileSizeBetween(@Param("minSize") Long minSize, @Param("maxSize") Long maxSize);
    
    /**
     * Find executions by execution environment.
     */
    List<SqlLoaderExecutionEntity> findByExecutionEnvironmentOrderByStartedDateDesc(String executionEnvironment);
    
    /**
     * Find recent executions for monitoring dashboard.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.startedDate >= :recentThreshold ORDER BY e.startedDate DESC")
    List<SqlLoaderExecutionEntity> findRecentExecutions(@Param("recentThreshold") LocalDateTime recentThreshold);
    
    /**
     * Get error summary for failed executions.
     */
    @Query("SELECT e.sqlLoaderReturnCode, COUNT(e), e.errorMessage FROM SqlLoaderExecutionEntity e " +
           "WHERE e.executionStatus IN ('FAILED', 'CANCELLED', 'TIMEOUT') AND e.startedDate BETWEEN :startDate AND :endDate " +
           "GROUP BY e.sqlLoaderReturnCode, e.errorMessage ORDER BY COUNT(e) DESC")
    List<Object[]> getErrorSummary(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find executions with memory usage above threshold.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.memoryUsageMb > :memoryThreshold ORDER BY e.memoryUsageMb DESC")
    List<SqlLoaderExecutionEntity> findExecutionsWithHighMemoryUsage(@Param("memoryThreshold") Double memoryThreshold);
    
    /**
     * Find executions with CPU usage above threshold.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.cpuUsagePercent > :cpuThreshold ORDER BY e.cpuUsagePercent DESC")
    List<SqlLoaderExecutionEntity> findExecutionsWithHighCpuUsage(@Param("cpuThreshold") Double cpuThreshold);
    
    /**
     * Get resource utilization summary.
     */
    @Query("SELECT AVG(e.memoryUsageMb), AVG(e.cpuUsagePercent), AVG(e.ioReadMb), AVG(e.ioWriteMb), " +
           "MAX(e.memoryUsageMb), MAX(e.cpuUsagePercent), MAX(e.ioReadMb), MAX(e.ioWriteMb) " +
           "FROM SqlLoaderExecutionEntity e WHERE e.startedDate BETWEEN :startDate AND :endDate " +
           "AND e.executionStatus IN ('SUCCESS', 'SUCCESS_WITH_WARNINGS')")
    Object[] getResourceUtilizationSummary(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find executions by correlation ID pattern for debugging.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.correlationId LIKE %:correlationPattern% ORDER BY e.startedDate DESC")
    List<SqlLoaderExecutionEntity> findByCorrelationIdContaining(@Param("correlationPattern") String correlationPattern);
    
    /**
     * Find executions that completed within specific duration range.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.durationMs BETWEEN :minDuration AND :maxDuration ORDER BY e.durationMs")
    List<SqlLoaderExecutionEntity> findByDurationBetween(@Param("minDuration") Long minDuration, @Param("maxDuration") Long maxDuration);
    
    /**
     * Get throughput distribution for performance analysis.
     */
    @Query("SELECT " +
           "CASE " +
           "   WHEN e.throughputRecordsPerSec < 1000 THEN '< 1K' " +
           "   WHEN e.throughputRecordsPerSec < 10000 THEN '1K-10K' " +
           "   WHEN e.throughputRecordsPerSec < 100000 THEN '10K-100K' " +
           "   WHEN e.throughputRecordsPerSec < 1000000 THEN '100K-1M' " +
           "   ELSE '> 1M' " +
           "END, COUNT(e) " +
           "FROM SqlLoaderExecutionEntity e WHERE e.throughputRecordsPerSec IS NOT NULL " +
           "AND e.startedDate BETWEEN :startDate AND :endDate " +
           "GROUP BY CASE " +
           "   WHEN e.throughputRecordsPerSec < 1000 THEN '< 1K' " +
           "   WHEN e.throughputRecordsPerSec < 10000 THEN '1K-10K' " +
           "   WHEN e.throughputRecordsPerSec < 100000 THEN '10K-100K' " +
           "   WHEN e.throughputRecordsPerSec < 1000000 THEN '100K-1M' " +
           "   ELSE '> 1M' " +
           "END ORDER BY MIN(e.throughputRecordsPerSec)")
    List<Object[]> getThroughputDistribution(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find oldest execution.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e ORDER BY e.startedDate LIMIT 1")
    Optional<SqlLoaderExecutionEntity> findOldestExecution();
    
    /**
     * Find latest execution by config ID.
     */
    @Query("SELECT e FROM SqlLoaderExecutionEntity e WHERE e.configId = :configId ORDER BY e.startedDate DESC LIMIT 1")
    Optional<SqlLoaderExecutionEntity> findLatestExecutionByConfigId(@Param("configId") String configId);
    
    /**
     * Count total records processed by config ID within date range.
     */
    @Query("SELECT SUM(e.totalRecords) FROM SqlLoaderExecutionEntity e WHERE e.configId = :configId AND e.startedDate BETWEEN :startDate AND :endDate")
    Long getTotalRecordsProcessedByConfigIdAndDateRange(@Param("configId") String configId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}