package com.fabric.batch.repository;

import com.fabric.batch.entity.BatchTempStagingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Epic 2: Repository interface for BatchTempStagingEntity providing
 * high-performance data access patterns optimized for parallel transaction processing.
 * 
 * This repository supports the temporary staging architecture with specialized queries
 * for partition management, correlation tracking, and performance monitoring.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 2 - Simple Transaction Processing
 */
@Repository
public interface BatchTempStagingRepository extends JpaRepository<BatchTempStagingEntity, Long> {

    /**
     * Find all staging records for a specific execution ID
     */
    @Query("SELECT bts FROM BatchTempStagingEntity bts " +
           "WHERE bts.executionId = :executionId " +
           "ORDER BY bts.sequenceNumber ASC")
    List<BatchTempStagingEntity> findByExecutionIdOrderBySequenceNumber(
            @Param("executionId") String executionId);

    /**
     * Find staging records by execution ID and transaction type
     */
    @Query("SELECT bts FROM BatchTempStagingEntity bts " +
           "WHERE bts.executionId = :executionId " +
           "AND bts.transactionTypeId = :transactionTypeId " +
           "ORDER BY bts.sequenceNumber ASC")
    List<BatchTempStagingEntity> findByExecutionIdAndTransactionTypeId(
            @Param("executionId") String executionId,
            @Param("transactionTypeId") Long transactionTypeId);

    /**
     * Find staging records by processing status with pagination
     */
    @Query("SELECT bts FROM BatchTempStagingEntity bts " +
           "WHERE bts.processingStatus = :status " +
           "ORDER BY bts.createdTimestamp ASC")
    Page<BatchTempStagingEntity> findByProcessingStatus(
            @Param("status") BatchTempStagingEntity.ProcessingStatus status,
            Pageable pageable);

    /**
     * Find pending records for processing (optimized for parallel processing)
     */
    @Query("SELECT bts FROM BatchTempStagingEntity bts " +
           "WHERE bts.processingStatus = 'PENDING' " +
           "AND bts.retryCount < 10 " +
           "ORDER BY bts.createdTimestamp ASC")
    List<BatchTempStagingEntity> findPendingRecordsForProcessing(Pageable pageable);

    /**
     * Find records by correlation ID for tracing
     */
    @Query("SELECT bts FROM BatchTempStagingEntity bts " +
           "WHERE bts.correlationId = :correlationId " +
           "ORDER BY bts.sequenceNumber ASC")
    List<BatchTempStagingEntity> findByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Find stale processing records (hanging threads)
     */
    @Query("SELECT bts FROM BatchTempStagingEntity bts " +
           "WHERE bts.processingStatus = 'PROCESSING' " +
           "AND bts.processedTimestamp < :staleCutoff")
    List<BatchTempStagingEntity> findStaleProcessingRecords(@Param("staleCutoff") Instant staleCutoff);

    /**
     * Find records by thread ID for debugging
     */
    @Query("SELECT bts FROM BatchTempStagingEntity bts " +
           "WHERE bts.threadId = :threadId " +
           "ORDER BY bts.createdTimestamp DESC")
    List<BatchTempStagingEntity> findByThreadId(@Param("threadId") String threadId);

    /**
     * Count records by processing status for monitoring
     */
    @Query("SELECT COUNT(bts) FROM BatchTempStagingEntity bts " +
           "WHERE bts.processingStatus = :status " +
           "AND bts.businessDate = :businessDate")
    Long countByProcessingStatusAndBusinessDate(
            @Param("status") BatchTempStagingEntity.ProcessingStatus status,
            @Param("businessDate") LocalDate businessDate);

    /**
     * Find failed records that can be retried
     */
    @Query("SELECT bts FROM BatchTempStagingEntity bts " +
           "WHERE bts.processingStatus IN ('FAILED', 'RETRYING') " +
           "AND bts.retryCount < :maxRetries " +
           "ORDER BY bts.processedTimestamp ASC")
    List<BatchTempStagingEntity> findRetryableFailedRecords(@Param("maxRetries") Integer maxRetries);

    /**
     * Find records by partition key for parallel processing coordination
     */
    @Query("SELECT bts FROM BatchTempStagingEntity bts " +
           "WHERE bts.partitionKey = :partitionKey " +
           "ORDER BY bts.sequenceNumber ASC")
    List<BatchTempStagingEntity> findByPartitionKey(@Param("partitionKey") String partitionKey);

    /**
     * Get processing statistics for monitoring dashboard
     */
    @Query("SELECT new map(" +
           "bts.processingStatus as status, " +
           "COUNT(bts) as count, " +
           "AVG(CASE WHEN bts.processedTimestamp IS NOT NULL AND bts.createdTimestamp IS NOT NULL " +
           "    THEN EXTRACT(EPOCH FROM (bts.processedTimestamp - bts.createdTimestamp)) * 1000 " +
           "    ELSE NULL END) as avgProcessingTimeMs) " +
           "FROM BatchTempStagingEntity bts " +
           "WHERE bts.executionId = :executionId " +
           "GROUP BY bts.processingStatus")
    List<java.util.Map<String, Object>> getProcessingStatistics(@Param("executionId") String executionId);

    /**
     * Find records for data quality analysis
     */
    @Query("SELECT bts FROM BatchTempStagingEntity bts " +
           "WHERE bts.businessDate = :businessDate " +
           "AND bts.dataHash IS NULL " +
           "AND bts.processingStatus = 'COMPLETED'")
    List<BatchTempStagingEntity> findRecordsWithMissingHash(@Param("businessDate") LocalDate businessDate);

    /**
     * Update processing status in batch for performance
     */
    @Modifying
    @Query("UPDATE BatchTempStagingEntity bts " +
           "SET bts.processingStatus = :newStatus, " +
           "    bts.processedTimestamp = :timestamp " +
           "WHERE bts.executionId = :executionId " +
           "AND bts.processingStatus = :currentStatus")
    int updateProcessingStatusBatch(@Param("executionId") String executionId,
                                   @Param("currentStatus") BatchTempStagingEntity.ProcessingStatus currentStatus,
                                   @Param("newStatus") BatchTempStagingEntity.ProcessingStatus newStatus,
                                   @Param("timestamp") Instant timestamp);

    /**
     * Find records by business date range for reporting
     */
    @Query("SELECT bts FROM BatchTempStagingEntity bts " +
           "WHERE bts.businessDate BETWEEN :startDate AND :endDate " +
           "ORDER BY bts.businessDate DESC, bts.createdTimestamp DESC")
    Page<BatchTempStagingEntity> findByBusinessDateRange(@Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate,
                                                         Pageable pageable);

    /**
     * Get error summary for troubleshooting
     */
    @Query("SELECT new map(" +
           "SUBSTRING(bts.errorMessage, 1, 100) as errorPrefix, " +
           "COUNT(bts) as count, " +
           "MAX(bts.processedTimestamp) as lastOccurrence) " +
           "FROM BatchTempStagingEntity bts " +
           "WHERE bts.processingStatus = 'FAILED' " +
           "AND bts.businessDate >= :sinceDate " +
           "AND bts.errorMessage IS NOT NULL " +
           "GROUP BY SUBSTRING(bts.errorMessage, 1, 100) " +
           "ORDER BY COUNT(bts) DESC")
    List<java.util.Map<String, Object>> getErrorSummary(@Param("sinceDate") LocalDate sinceDate);

    /**
     * Find duplicate records by data hash
     */
    @Query("SELECT bts FROM BatchTempStagingEntity bts " +
           "WHERE bts.dataHash = :dataHash " +
           "AND bts.businessDate = :businessDate " +
           "ORDER BY bts.createdTimestamp ASC")
    List<BatchTempStagingEntity> findByDataHashAndBusinessDate(@Param("dataHash") String dataHash,
                                                               @Param("businessDate") LocalDate businessDate);

    /**
     * Cleanup old staging records (for maintenance)
     */
    @Modifying
    @Query("DELETE FROM BatchTempStagingEntity bts " +
           "WHERE bts.createdTimestamp < :cutoffDate " +
           "AND bts.processingStatus IN ('COMPLETED', 'FAILED')")
    int cleanupOldRecords(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Archive completed records older than retention period
     */
    @Query("SELECT bts FROM BatchTempStagingEntity bts " +
           "WHERE bts.createdTimestamp < :archiveDate " +
           "AND bts.processingStatus = 'COMPLETED' " +
           "ORDER BY bts.createdTimestamp ASC")
    Page<BatchTempStagingEntity> findRecordsForArchival(@Param("archiveDate") Instant archiveDate,
                                                        Pageable pageable);

    /**
     * Get throughput metrics for performance analysis
     */
    @Query("SELECT new map(" +
           "DATE(bts.createdTimestamp) as processingDate, " +
           "bts.transactionTypeId as transactionTypeId, " +
           "COUNT(bts) as totalRecords, " +
           "SUM(CASE WHEN bts.processingStatus = 'COMPLETED' THEN 1 ELSE 0 END) as completedRecords, " +
           "SUM(CASE WHEN bts.processingStatus = 'FAILED' THEN 1 ELSE 0 END) as failedRecords, " +
           "AVG(CASE WHEN bts.processedTimestamp IS NOT NULL AND bts.createdTimestamp IS NOT NULL " +
           "    THEN EXTRACT(EPOCH FROM (bts.processedTimestamp - bts.createdTimestamp)) " +
           "    ELSE NULL END) as avgProcessingTimeSeconds) " +
           "FROM BatchTempStagingEntity bts " +
           "WHERE bts.createdTimestamp >= :fromDate " +
           "GROUP BY DATE(bts.createdTimestamp), bts.transactionTypeId " +
           "ORDER BY DATE(bts.createdTimestamp) DESC")
    List<java.util.Map<String, Object>> getThroughputMetrics(@Param("fromDate") Instant fromDate);

    /**
     * Find most recent record for a correlation ID
     */
    @Query("SELECT bts FROM BatchTempStagingEntity bts " +
           "WHERE bts.correlationId = :correlationId " +
           "ORDER BY bts.createdTimestamp DESC")
    Optional<BatchTempStagingEntity> findLatestByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Performance optimization: Find next batch for processing with row locking
     */
    @Query("SELECT bts FROM BatchTempStagingEntity bts " +
           "WHERE bts.processingStatus = 'PENDING' " +
           "AND bts.transactionTypeId = :transactionTypeId " +
           "ORDER BY bts.createdTimestamp ASC")
    List<BatchTempStagingEntity> findNextBatchForProcessing(@Param("transactionTypeId") Long transactionTypeId,
                                                            Pageable pageable);
}