package com.fabric.batch.repository;

import com.fabric.batch.entity.ProcessingJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ProcessingJobEntity operations.
 * Provides comprehensive data access methods for job execution tracking and monitoring.
 */
@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJobEntity, String> {
    
    /**
     * Find job by correlation ID.
     */
    Optional<ProcessingJobEntity> findByCorrelationId(String correlationId);
    
    /**
     * Find all jobs for a specific configuration.
     */
    List<ProcessingJobEntity> findByConfigIdOrderBySubmittedDateDesc(String configId);
    
    /**
     * Find jobs by status.
     */
    List<ProcessingJobEntity> findByJobStatusOrderBySubmittedDateDesc(ProcessingJobEntity.JobStatus jobStatus);
    
    /**
     * Find jobs by status and priority.
     */
    List<ProcessingJobEntity> findByJobStatusAndPriorityOrderBySubmittedDateDesc(
            ProcessingJobEntity.JobStatus jobStatus, ProcessingJobEntity.Priority priority);
    
    /**
     * Find failed jobs that can be retried.
     */
    @Query("SELECT j FROM ProcessingJobEntity j WHERE j.jobStatus = 'FAILED' AND j.retryCount < j.maxRetries AND (j.nextRetryDate IS NULL OR j.nextRetryDate <= :currentTime)")
    List<ProcessingJobEntity> findFailedJobsForRetry(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find jobs requiring rollback.
     */
    List<ProcessingJobEntity> findByRollbackRequiredAndRollbackCompletedOrderBySubmittedDateDesc(
            String rollbackRequired, String rollbackCompleted);
    
    /**
     * Find jobs by date range.
     */
    @Query("SELECT j FROM ProcessingJobEntity j WHERE j.submittedDate BETWEEN :startDate AND :endDate ORDER BY j.submittedDate DESC")
    List<ProcessingJobEntity> findBySubmittedDateBetween(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find jobs by file name pattern.
     */
    @Query("SELECT j FROM ProcessingJobEntity j WHERE j.fileName LIKE :fileNamePattern ORDER BY j.submittedDate DESC")
    List<ProcessingJobEntity> findByFileNameLike(@Param("fileNamePattern") String fileNamePattern);
    
    /**
     * Find long-running jobs (duration above threshold).
     */
    @Query("SELECT j FROM ProcessingJobEntity j WHERE j.durationMs > :thresholdMs AND j.jobStatus = 'COMPLETED' ORDER BY j.durationMs DESC")
    List<ProcessingJobEntity> findLongRunningJobs(@Param("thresholdMs") Long thresholdMs);
    
    /**
     * Find jobs with high error rates.
     */
    @Query("SELECT j FROM ProcessingJobEntity j WHERE j.totalRecords > 0 AND (j.failedRecords * 100.0 / j.totalRecords) > :errorThreshold ORDER BY (j.failedRecords * 100.0 / j.totalRecords) DESC")
    List<ProcessingJobEntity> findJobsWithHighErrorRate(@Param("errorThreshold") Double errorThreshold);
    
    /**
     * Find active jobs (not completed, failed, or cancelled).
     */
    @Query("SELECT j FROM ProcessingJobEntity j WHERE j.jobStatus NOT IN ('COMPLETED', 'FAILED', 'CANCELLED') ORDER BY j.submittedDate")
    List<ProcessingJobEntity> findActiveJobs();
    
    /**
     * Find jobs requiring notification.
     */
    @Query("SELECT j FROM ProcessingJobEntity j WHERE j.notificationSent = 'N' AND j.jobStatus IN ('COMPLETED', 'FAILED') ORDER BY j.completedDate")
    List<ProcessingJobEntity> findJobsRequiringNotification();
    
    /**
     * Get job statistics by status.
     */
    @Query("SELECT j.jobStatus, COUNT(j), AVG(j.durationMs), SUM(j.totalRecords), SUM(j.failedRecords) FROM ProcessingJobEntity j WHERE j.submittedDate >= :fromDate GROUP BY j.jobStatus")
    List<Object[]> getJobStatisticsByStatus(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get job statistics by configuration.
     */
    @Query("SELECT j.configId, COUNT(j), AVG(j.durationMs), AVG(j.failedRecords * 100.0 / NULLIF(j.totalRecords, 0)) FROM ProcessingJobEntity j WHERE j.submittedDate >= :fromDate GROUP BY j.configId")
    List<Object[]> getJobStatisticsByConfig(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get daily job volume.
     */
    @Query("SELECT DATE(j.submittedDate), COUNT(j), SUM(j.totalRecords) FROM ProcessingJobEntity j WHERE j.submittedDate >= :fromDate GROUP BY DATE(j.submittedDate) ORDER BY DATE(j.submittedDate)")
    List<Object[]> getDailyJobVolume(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Find jobs by batch ID.
     */
    List<ProcessingJobEntity> findByBatchIdOrderBySubmittedDateDesc(String batchId);
    
    /**
     * Find jobs by execution mode.
     */
    List<ProcessingJobEntity> findByExecutionModeOrderBySubmittedDateDesc(ProcessingJobEntity.ExecutionMode executionMode);
    
    /**
     * Count jobs by status for dashboard.
     */
    @Query("SELECT j.jobStatus, COUNT(j) FROM ProcessingJobEntity j WHERE j.submittedDate >= :fromDate GROUP BY j.jobStatus")
    List<Object[]> countJobsByStatus(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Find top error-prone configurations.
     */
    @Query("SELECT j.configId, COUNT(j), SUM(j.failedRecords), AVG(j.failedRecords * 100.0 / NULLIF(j.totalRecords, 0)) FROM ProcessingJobEntity j WHERE j.submittedDate >= :fromDate AND j.failedRecords > 0 GROUP BY j.configId ORDER BY AVG(j.failedRecords * 100.0 / NULLIF(j.totalRecords, 0)) DESC")
    List<Object[]> findTopErrorProneConfigurations(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Find jobs with specific SQL*Loader return codes.
     */
    List<ProcessingJobEntity> findBySqlLoaderReturnCodeOrderBySubmittedDateDesc(Integer sqlLoaderReturnCode);
    
    /**
     * Get performance metrics for monitoring.
     */
    @Query("SELECT AVG(j.durationMs), MIN(j.durationMs), MAX(j.durationMs), AVG(j.totalRecords), MAX(j.totalRecords) FROM ProcessingJobEntity j WHERE j.jobStatus = 'COMPLETED' AND j.submittedDate >= :fromDate")
    Object[] getPerformanceMetrics(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Find stale jobs (submitted but not started within threshold).
     */
    @Query("SELECT j FROM ProcessingJobEntity j WHERE j.jobStatus = 'SUBMITTED' AND j.submittedDate <= :thresholdTime")
    List<ProcessingJobEntity> findStaleJobs(@Param("thresholdTime") LocalDateTime thresholdTime);
}