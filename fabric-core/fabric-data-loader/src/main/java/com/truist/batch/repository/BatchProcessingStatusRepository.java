package com.truist.batch.repository;

import com.truist.batch.entity.BatchProcessingStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Epic 2: Repository interface for BatchProcessingStatusEntity providing
 * real-time monitoring and status tracking capabilities for parallel processing.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 2 - Simple Transaction Processing
 */
@Repository
public interface BatchProcessingStatusRepository extends JpaRepository<BatchProcessingStatusEntity, Long> {

    /**
     * Find all status records for a specific execution
     */
    @Query("SELECT bps FROM BatchProcessingStatusEntity bps " +
           "WHERE bps.executionId = :executionId " +
           "ORDER BY bps.startTime ASC")
    List<BatchProcessingStatusEntity> findByExecutionIdOrderByStartTime(
            @Param("executionId") String executionId);

    /**
     * Find status records by processing status
     */
    @Query("SELECT bps FROM BatchProcessingStatusEntity bps " +
           "WHERE bps.processingStatus = :status " +
           "ORDER BY bps.lastHeartbeat DESC")
    List<BatchProcessingStatusEntity> findByProcessingStatus(
            @Param("status") BatchProcessingStatusEntity.ProcessingStatus status);

    /**
     * Find currently running processes (active monitoring)
     */
    @Query("SELECT bps FROM BatchProcessingStatusEntity bps " +
           "WHERE bps.processingStatus IN ('INITIALIZED', 'RUNNING') " +
           "ORDER BY bps.startTime ASC")
    List<BatchProcessingStatusEntity> findActiveProcesses();

    /**
     * Find stale processes (missed heartbeats)
     */
    @Query("SELECT bps FROM BatchProcessingStatusEntity bps " +
           "WHERE bps.processingStatus IN ('INITIALIZED', 'RUNNING') " +
           "AND bps.lastHeartbeat < :staleThreshold")
    List<BatchProcessingStatusEntity> findStaleProcesses(@Param("staleThreshold") Instant staleThreshold);

    /**
     * Get latest status for execution and transaction type
     */
    @Query("SELECT bps FROM BatchProcessingStatusEntity bps " +
           "WHERE bps.executionId = :executionId " +
           "AND bps.transactionTypeId = :transactionTypeId " +
           "ORDER BY bps.startTime DESC")
    Optional<BatchProcessingStatusEntity> findLatestByExecutionIdAndTransactionTypeId(
            @Param("executionId") String executionId,
            @Param("transactionTypeId") Long transactionTypeId);

    /**
     * Find processes by thread ID for debugging
     */
    @Query("SELECT bps FROM BatchProcessingStatusEntity bps " +
           "WHERE bps.threadId = :threadId " +
           "ORDER BY bps.startTime DESC")
    List<BatchProcessingStatusEntity> findByThreadId(@Param("threadId") String threadId);

    /**
     * Get processing statistics for monitoring dashboard
     */
    @Query("SELECT new map(" +
           "bps.processingStatus as status, " +
           "COUNT(bps) as count, " +
           "AVG(bps.recordsProcessed) as avgRecordsProcessed, " +
           "AVG(bps.recordsFailed) as avgRecordsFailed, " +
           "AVG(CASE WHEN bps.endTime IS NOT NULL AND bps.startTime IS NOT NULL " +
           "    THEN EXTRACT(EPOCH FROM (bps.endTime - bps.startTime)) * 1000 " +
           "    ELSE NULL END) as avgDurationMs) " +
           "FROM BatchProcessingStatusEntity bps " +
           "WHERE bps.executionId = :executionId " +
           "GROUP BY bps.processingStatus")
    List<java.util.Map<String, Object>> getProcessingStatistics(@Param("executionId") String executionId);

    /**
     * Count active processes by transaction type
     */
    @Query("SELECT COUNT(bps) FROM BatchProcessingStatusEntity bps " +
           "WHERE bps.transactionTypeId = :transactionTypeId " +
           "AND bps.processingStatus IN ('INITIALIZED', 'RUNNING')")
    Long countActiveProcessesByTransactionType(@Param("transactionTypeId") Long transactionTypeId);

    /**
     * Find processing status records by heartbeat time range
     */
    @Query("SELECT bps FROM BatchProcessingStatusEntity bps " +
           "WHERE bps.lastHeartbeat BETWEEN :startTime AND :endTime " +
           "ORDER BY bps.lastHeartbeat DESC")
    List<BatchProcessingStatusEntity> findByLastHeartbeatBetween(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);
}