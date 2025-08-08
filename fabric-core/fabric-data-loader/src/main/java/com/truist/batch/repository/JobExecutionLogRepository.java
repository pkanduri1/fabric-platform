package com.truist.batch.repository;

import com.truist.batch.entity.JobExecutionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for JobExecutionLogEntity operations.
 * Provides comprehensive data access methods for job execution monitoring and debugging.
 */
@Repository
public interface JobExecutionLogRepository extends JpaRepository<JobExecutionLogEntity, Long> {
    
    /**
     * Find all logs for a specific job execution.
     */
    List<JobExecutionLogEntity> findByJobExecutionIdOrderByStepStartTime(String jobExecutionId);
    
    /**
     * Find logs by correlation ID for complete execution tracing.
     */
    List<JobExecutionLogEntity> findByCorrelationIdOrderByStepStartTime(String correlationId);
    
    /**
     * Find logs by step name across all job executions.
     */
    List<JobExecutionLogEntity> findByStepNameOrderByStepStartTimeDesc(String stepName);
    
    /**
     * Find logs by step type.
     */
    List<JobExecutionLogEntity> findByStepTypeOrderByStepStartTimeDesc(JobExecutionLogEntity.StepType stepType);
    
    /**
     * Find logs by step status.
     */
    List<JobExecutionLogEntity> findByStepStatusOrderByStepStartTimeDesc(JobExecutionLogEntity.StepStatus stepStatus);
    
    /**
     * Find error logs (ERROR and FATAL log levels).
     */
    @Query("SELECT l FROM JobExecutionLogEntity l WHERE l.logLevel IN ('ERROR', 'FATAL') ORDER BY l.stepStartTime DESC")
    List<JobExecutionLogEntity> findErrorLogs();
    
    /**
     * Find warning logs.
     */
    List<JobExecutionLogEntity> findByLogLevelOrderByStepStartTimeDesc(JobExecutionLogEntity.LogLevel logLevel);
    
    /**
     * Find failed steps.
     */
    @Query("SELECT l FROM JobExecutionLogEntity l WHERE l.stepStatus = 'FAILED' ORDER BY l.stepStartTime DESC")
    List<JobExecutionLogEntity> findFailedStepsOrderByStepStartTimeDesc();
    
    /**
     * Find logs with errors for specific job execution.
     */
    @Query("SELECT l FROM JobExecutionLogEntity l WHERE l.jobExecutionId = :jobExecutionId AND l.recordsFailed > 0 ORDER BY l.stepStartTime")
    List<JobExecutionLogEntity> findErrorLogsForJob(@Param("jobExecutionId") String jobExecutionId);
    
    /**
     * Find logs with warnings for specific job execution.
     */
    @Query("SELECT l FROM JobExecutionLogEntity l WHERE l.jobExecutionId = :jobExecutionId AND l.recordsWarning > 0 ORDER BY l.stepStartTime")
    List<JobExecutionLogEntity> findWarningLogsForJob(@Param("jobExecutionId") String jobExecutionId);
    
    /**
     * Find long-running steps (duration above threshold).
     */
    @Query("SELECT l FROM JobExecutionLogEntity l WHERE l.stepDurationMs > :thresholdMs ORDER BY l.stepDurationMs DESC")
    List<JobExecutionLogEntity> findLongRunningSteps(@Param("thresholdMs") Long thresholdMs);
    
    /**
     * Find steps with high memory usage.
     */
    @Query("SELECT l FROM JobExecutionLogEntity l WHERE l.memoryUsageMb > :thresholdMb ORDER BY l.memoryUsageMb DESC")
    List<JobExecutionLogEntity> findHighMemoryUsageSteps(@Param("thresholdMb") Double thresholdMb);
    
    /**
     * Find steps with high CPU usage.
     */
    @Query("SELECT l FROM JobExecutionLogEntity l WHERE l.cpuUsagePercent > :thresholdPercent ORDER BY l.cpuUsagePercent DESC")
    List<JobExecutionLogEntity> findHighCpuUsageSteps(@Param("thresholdPercent") Double thresholdPercent);
    
    /**
     * Find steps with low throughput (records per second below threshold).
     */
    @Query("SELECT l FROM JobExecutionLogEntity l WHERE l.throughputRecordsPerSec < :thresholdThroughput AND l.throughputRecordsPerSec IS NOT NULL ORDER BY l.throughputRecordsPerSec")
    List<JobExecutionLogEntity> findLowThroughputSteps(@Param("thresholdThroughput") Double thresholdThroughput);
    
    /**
     * Find logs for date range.
     */
    @Query("SELECT l FROM JobExecutionLogEntity l WHERE l.stepStartTime BETWEEN :startDate AND :endDate ORDER BY l.stepStartTime DESC")
    List<JobExecutionLogEntity> findByDateRange(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find logs with retry count greater than threshold.
     */
    @Query("SELECT l FROM JobExecutionLogEntity l WHERE l.retryCount > :threshold ORDER BY l.retryCount DESC, l.stepStartTime DESC")
    List<JobExecutionLogEntity> findStepsWithHighRetryCount(@Param("threshold") Integer threshold);
    
    /**
     * Get step performance statistics.
     */
    @Query("SELECT l.stepName, COUNT(l), AVG(l.stepDurationMs), MIN(l.stepDurationMs), MAX(l.stepDurationMs), AVG(l.throughputRecordsPerSec) FROM JobExecutionLogEntity l WHERE l.stepStatus = 'COMPLETED' AND l.stepStartTime >= :fromDate GROUP BY l.stepName")
    List<Object[]> getStepPerformanceStatistics(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get step error statistics.
     */
    @Query("SELECT l.stepName, COUNT(l), SUM(l.recordsFailed), AVG(l.recordsFailed * 100.0 / NULLIF(l.recordsProcessed, 0)) FROM JobExecutionLogEntity l WHERE l.stepStartTime >= :fromDate GROUP BY l.stepName")
    List<Object[]> getStepErrorStatistics(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get daily step execution volume.
     */
    @Query("SELECT DATE(l.stepStartTime), COUNT(l), SUM(l.recordsProcessed) FROM JobExecutionLogEntity l WHERE l.stepStartTime >= :fromDate GROUP BY DATE(l.stepStartTime) ORDER BY DATE(l.stepStartTime)")
    List<Object[]> getDailyStepExecutionVolume(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Find logs by step type and status.
     */
    List<JobExecutionLogEntity> findByStepTypeAndStepStatusOrderByStepStartTimeDesc(
            JobExecutionLogEntity.StepType stepType, JobExecutionLogEntity.StepStatus stepStatus);
    
    /**
     * Find the latest log entry for each step in a job execution.
     */
    @Query("SELECT l FROM JobExecutionLogEntity l WHERE l.jobExecutionId = :jobExecutionId AND l.stepStartTime = (SELECT MAX(l2.stepStartTime) FROM JobExecutionLogEntity l2 WHERE l2.jobExecutionId = l.jobExecutionId AND l2.stepName = l.stepName)")
    List<JobExecutionLogEntity> findLatestLogForEachStep(@Param("jobExecutionId") String jobExecutionId);
    
    /**
     * Count logs by log level for monitoring dashboard.
     */
    @Query("SELECT l.logLevel, COUNT(l) FROM JobExecutionLogEntity l WHERE l.stepStartTime >= :fromDate GROUP BY l.logLevel")
    List<Object[]> countLogsByLevel(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Count logs by step status for monitoring dashboard.
     */
    @Query("SELECT l.stepStatus, COUNT(l) FROM JobExecutionLogEntity l WHERE l.stepStartTime >= :fromDate GROUP BY l.stepStatus")
    List<Object[]> countLogsByStepStatus(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Find logs with specific error codes.
     */
    List<JobExecutionLogEntity> findByErrorCodeOrderByStepStartTimeDesc(String errorCode);
    
    /**
     * Find logs containing specific error messages.
     */
    @Query("SELECT l FROM JobExecutionLogEntity l WHERE l.errorMessage LIKE :errorMessagePattern ORDER BY l.stepStartTime DESC")
    List<JobExecutionLogEntity> findByErrorMessagePattern(@Param("errorMessagePattern") String errorMessagePattern);
    
    /**
     * Get resource usage statistics.
     */
    @Query("SELECT AVG(l.memoryUsageMb), MAX(l.memoryUsageMb), AVG(l.cpuUsagePercent), MAX(l.cpuUsagePercent), AVG(l.ioReadMb + l.ioWriteMb) FROM JobExecutionLogEntity l WHERE l.stepStartTime >= :fromDate")
    Object[] getResourceUsageStatistics(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Find logs with checkpoint data for recovery purposes.
     */
    @Query("SELECT l FROM JobExecutionLogEntity l WHERE l.checkpointData IS NOT NULL AND l.checkpointData != '' ORDER BY l.stepStartTime DESC")
    List<JobExecutionLogEntity> findLogsWithCheckpointData();
    
    /**
     * Find most recent completed log for a step type.
     */
    @Query("SELECT l FROM JobExecutionLogEntity l WHERE l.stepType = :stepType AND l.stepStatus = 'COMPLETED' ORDER BY l.stepStartTime DESC")
    List<JobExecutionLogEntity> findRecentCompletedLogsByStepType(@Param("stepType") JobExecutionLogEntity.StepType stepType);
    
    /**
     * Get throughput trends for specific step types.
     */
    @Query("SELECT DATE(l.stepStartTime), l.stepType, AVG(l.throughputRecordsPerSec), COUNT(l) FROM JobExecutionLogEntity l WHERE l.stepStartTime >= :fromDate AND l.throughputRecordsPerSec IS NOT NULL GROUP BY DATE(l.stepStartTime), l.stepType ORDER BY DATE(l.stepStartTime), l.stepType")
    List<Object[]> getThroughputTrends(@Param("fromDate") LocalDateTime fromDate);
}