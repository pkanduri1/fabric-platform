package com.truist.batch.repository;

import com.truist.batch.entity.ManualJobExecutionEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Manual Job Execution management.
 * Uses JdbcTemplate implementation for direct database access.
 * 
 * Provides comprehensive data access operations for job execution tracking with
 * enterprise-grade features including real-time monitoring, performance analytics,
 * and compliance reporting for banking applications.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 2.0
 * @since US001 Phase 2 - Job Execution Management
 */
public interface ManualJobExecutionRepository {

    // =========================================================================
    // BASIC CRUD OPERATIONS
    // =========================================================================

    /**
     * Find execution by ID.
     * 
     * @param executionId the execution ID
     * @return optional execution if found
     */
    Optional<ManualJobExecutionEntity> findById(String executionId);

    /**
     * Find all executions.
     * 
     * @return list of all executions
     */
    List<ManualJobExecutionEntity> findAll();

    /**
     * Save execution (insert or update).
     * 
     * @param entity the execution entity
     * @return saved execution
     */
    ManualJobExecutionEntity save(ManualJobExecutionEntity entity);

    /**
     * Delete execution by ID.
     * 
     * @param executionId the execution ID
     */
    void deleteById(String executionId);

    /**
     * Check if execution exists by ID.
     * 
     * @param executionId the execution ID
     * @return true if exists
     */
    boolean existsById(String executionId);

    /**
     * Count all executions.
     * 
     * @return total count
     */
    long count();

    // =========================================================================
    // BASIC EXECUTION QUERIES
    // =========================================================================

    /**
     * Find execution by correlation ID for distributed tracing.
     * 
     * @param correlationId the correlation ID to search for
     * @return optional execution if found
     */
    Optional<ManualJobExecutionEntity> findByCorrelationId(String correlationId);

    /**
     * Find executions by configuration ID.
     * 
     * @param configId the configuration ID
     * @return list of executions for the configuration
     */
    List<ManualJobExecutionEntity> findByConfigId(String configId);

    /**
     * Find executions by configuration ID ordered by start time (most recent first).
     * 
     * @param configId the configuration ID
     * @param limit maximum number of results
     * @return list of executions ordered by start time descending
     */
    List<ManualJobExecutionEntity> findByConfigIdOrderByStartTimeDesc(String configId, int limit);

    /**
     * Find executions by job name.
     * 
     * @param jobName the job name to filter by
     * @return list of executions for the specified job name
     */
    List<ManualJobExecutionEntity> findByJobName(String jobName);

    // =========================================================================
    // STATUS-BASED QUERIES
    // =========================================================================

    /**
     * Find executions by status.
     * 
     * @param status the execution status to filter by
     * @return list of executions with specified status
     */
    List<ManualJobExecutionEntity> findByStatus(String status);

    /**
     * Find executions with multiple statuses.
     * 
     * @param statuses list of statuses to filter by
     * @return list of executions matching any of the specified statuses
     */
    List<ManualJobExecutionEntity> findByStatusIn(List<String> statuses);

    /**
     * Find currently active executions for real-time monitoring.
     * 
     * @return list of active executions (STARTED, RUNNING)
     */
    List<ManualJobExecutionEntity> findActiveExecutions();

    /**
     * Find long-running executions for monitoring and alerting.
     * 
     * @param thresholdMinutes duration threshold in minutes
     * @return list of long-running executions
     */
    List<ManualJobExecutionEntity> findLongRunningExecutions(int thresholdMinutes);

    /**
     * Find failed executions within time range for error analysis.
     * 
     * @param startTime beginning of time range
     * @param endTime end of time range
     * @return list of failed executions in time range
     */
    List<ManualJobExecutionEntity> findFailedExecutionsBetween(LocalDateTime startTime, LocalDateTime endTime);

    // =========================================================================
    // TIME-BASED QUERIES
    // =========================================================================

    /**
     * Find executions within a specific time range.
     * 
     * @param startTime beginning of time range
     * @param endTime end of time range
     * @return list of executions in time range
     */
    List<ManualJobExecutionEntity> findExecutionsBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find executions started after a specific time.
     * 
     * @param since timestamp to filter from
     * @return list of executions started after the specified time
     */
    List<ManualJobExecutionEntity> findByStartTimeAfter(LocalDateTime since);

    /**
     * Find executions that completed within a time range.
     * 
     * @param startTime beginning of time range
     * @param endTime end of time range
     * @return list of completed executions
     */
    List<ManualJobExecutionEntity> findCompletedExecutionsBetween(LocalDateTime startTime, LocalDateTime endTime);

    // =========================================================================
    // PERFORMANCE AND ANALYTICS QUERIES
    // =========================================================================

    /**
     * Find executions with error rate above threshold.
     * 
     * @param errorThreshold error percentage threshold
     * @param startTime beginning of time range
     * @param endTime end of time range
     * @return list of executions exceeding error threshold
     */
    List<ManualJobExecutionEntity> findHighErrorRateExecutions(
            BigDecimal errorThreshold, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find slowest executions for performance analysis.
     * 
     * @param limit maximum number of results
     * @param startTime beginning of time range
     * @param endTime end of time range
     * @return list of slowest executions
     */
    List<ManualJobExecutionEntity> findSlowestExecutions(int limit, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find highest throughput executions for capacity planning.
     * 
     * @param limit maximum number of results
     * @param startTime beginning of time range
     * @param endTime end of time range
     * @return list of highest throughput executions
     */
    List<ManualJobExecutionEntity> findHighestThroughputExecutions(
            int limit, LocalDateTime startTime, LocalDateTime endTime);

    // =========================================================================
    // ADVANCED SEARCH QUERIES
    // =========================================================================

    /**
     * Search executions by multiple criteria.
     * 
     * @param configId optional configuration ID filter
     * @param jobName optional job name filter
     * @param status optional status filter
     * @param executedBy optional executed by filter
     * @param executionEnvironment optional environment filter
     * @param startTime optional start time filter (after)
     * @param endTime optional end time filter (before)
     * @return list of executions matching all specified criteria
     */
    List<ManualJobExecutionEntity> findByMultipleCriteria(
            String configId, String jobName, String status, String executedBy,
            String executionEnvironment, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find executions by user with time range filter.
     * 
     * @param executedBy the user who executed jobs
     * @param startTime beginning of time range
     * @param endTime end of time range
     * @return list of user executions in time range
     */
    List<ManualJobExecutionEntity> findByExecutedByBetween(
            String executedBy, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find executions by environment for environment-specific analysis.
     * 
     * @param executionEnvironment the execution environment
     * @return list of executions in specified environment
     */
    List<ManualJobExecutionEntity> findByExecutionEnvironment(String executionEnvironment);

    // =========================================================================
    // BULK UPDATE OPERATIONS
    // =========================================================================

    /**
     * Update execution status atomically.
     * 
     * @param executionId the execution ID to update
     * @param status the new status
     * @param endTime optional end time for completion
     * @return number of rows updated
     */
    int updateExecutionStatus(String executionId, String status, LocalDateTime endTime);

    /**
     * Update execution progress metrics.
     * 
     * @param executionId the execution ID to update
     * @param recordsProcessed current records processed count
     * @param recordsSuccess current success count
     * @param recordsError current error count
     * @return number of rows updated
     */
    int updateExecutionProgress(String executionId, Long recordsProcessed, Long recordsSuccess, Long recordsError);

    /**
     * Mark alerts as sent for executions.
     * 
     * @param executionIds list of execution IDs to mark
     * @return number of executions updated
     */
    int markAlertsSent(List<String> executionIds);

    /**
     * Cancel active executions by configuration ID.
     * 
     * @param configId the configuration ID
     * @param cancelledBy user performing cancellation
     * @param cancellationReason reason for cancellation
     * @return number of executions cancelled
     */
    int cancelActiveExecutionsByConfig(String configId, String cancelledBy, String cancellationReason);

    // =========================================================================
    // STATISTICAL QUERIES FOR REPORTING
    // =========================================================================

    /**
     * Count executions by status for dashboard metrics.
     * 
     * @param status the status to count
     * @return count of executions with specified status
     */
    long countByStatus(String status);

    /**
     * Count executions in time range for activity metrics.
     * 
     * @param startTime beginning of time range
     * @param endTime end of time range
     * @return count of executions in time range
     */
    long countExecutionsBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Count successful executions in time range for success rate calculation.
     * 
     * @param startTime beginning of time range
     * @param endTime end of time range
     * @return count of successful executions
     */
    long countSuccessfulExecutionsBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Get average execution duration for performance benchmarking.
     * 
     * @param startTime beginning of time range
     * @param endTime end of time range
     * @return average duration in seconds
     */
    BigDecimal getAverageExecutionDuration(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Get total records processed for throughput analysis.
     * 
     * @param startTime beginning of time range
     * @param endTime end of time range
     * @return total records processed
     */
    long getTotalRecordsProcessed(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Get execution counts by environment for distribution analysis.
     * 
     * @return list of arrays containing [environment, count]
     */
    List<Object[]> getExecutionCountsByEnvironment();

    /**
     * Get execution trends by day for time-series analysis.
     * 
     * @param startTime beginning of time range
     * @param endTime end of time range
     * @return list of arrays containing [date, count]
     */
    List<Object[]> getExecutionTrendsByDay(LocalDateTime startTime, LocalDateTime endTime);

    // =========================================================================
    // DATA RETENTION AND CLEANUP
    // =========================================================================

    /**
     * Find old executions for data retention cleanup.
     * 
     * @param cutoffDate executions older than this date
     * @param statuses optional status filter for selective cleanup
     * @return list of execution IDs for cleanup
     */
    List<String> findOldExecutionsForCleanup(LocalDateTime cutoffDate, List<String> statuses);

    /**
     * Archive old execution logs by moving detailed data to archive storage.
     * 
     * @param executionIds list of execution IDs to archive
     * @return number of executions archived
     */
    int archiveExecutionDetails(List<String> executionIds);
}