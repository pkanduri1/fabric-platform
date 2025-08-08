package com.truist.batch.idempotency.repository;

import com.truist.batch.idempotency.entity.FabricIdempotencyKeyEntity;
import com.truist.batch.idempotency.entity.FabricIdempotencyKeyEntity.ProcessingState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for FabricIdempotencyKeyEntity operations.
 * Provides comprehensive data access methods for idempotency key management,
 * state tracking, and performance optimization with enterprise-grade queries.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Repository
public interface FabricIdempotencyKeyRepository extends JpaRepository<FabricIdempotencyKeyEntity, String> {
    
    // ============================================================================
    // Core Idempotency Operations
    // ============================================================================
    
    /**
     * Find idempotency key by job identifiers for duplicate detection.
     */
    @Query("""
        SELECT i FROM FabricIdempotencyKeyEntity i 
        WHERE i.sourceSystem = :sourceSystem 
        AND i.jobName = :jobName 
        AND i.transactionId = :transactionId
        """)
    Optional<FabricIdempotencyKeyEntity> findByJobIdentifiers(
        @Param("sourceSystem") String sourceSystem,
        @Param("jobName") String jobName,
        @Param("transactionId") String transactionId
    );
    
    /**
     * Find idempotency keys by correlation ID for distributed tracing.
     */
    @Query("""
        SELECT i FROM FabricIdempotencyKeyEntity i 
        WHERE i.correlationId = :correlationId
        ORDER BY i.createdDate DESC
        """)
    List<FabricIdempotencyKeyEntity> findByCorrelationId(@Param("correlationId") String correlationId);
    
    /**
     * Find idempotency keys by processing state and source system.
     */
    @Query("""
        SELECT i FROM FabricIdempotencyKeyEntity i 
        WHERE i.processingState = :state 
        AND i.sourceSystem = :sourceSystem
        ORDER BY i.createdDate DESC
        """)
    List<FabricIdempotencyKeyEntity> findByStateAndSourceSystem(
        @Param("state") ProcessingState state,
        @Param("sourceSystem") String sourceSystem
    );
    
    /**
     * Find idempotency keys by file hash for file-based deduplication.
     */
    @Query("""
        SELECT i FROM FabricIdempotencyKeyEntity i 
        WHERE i.fileHash = :fileHash 
        AND i.sourceSystem = :sourceSystem
        ORDER BY i.createdDate DESC
        """)
    List<FabricIdempotencyKeyEntity> findByFileHashAndSourceSystem(
        @Param("fileHash") String fileHash,
        @Param("sourceSystem") String sourceSystem
    );
    
    // ============================================================================
    // State Management with Optimistic Locking
    // ============================================================================
    
    /**
     * Update processing state with optimistic locking for concurrency control.
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE FabricIdempotencyKeyEntity i 
        SET i.processingState = :newState, 
            i.completedDate = :completedDate,
            i.errorDetails = :errorDetails,
            i.lockVersion = i.lockVersion + 1,
            i.lastAccessed = CURRENT_TIMESTAMP
        WHERE i.idempotencyKey = :idempotencyKey 
        AND i.lockVersion = :expectedVersion
        """)
    int updateStateWithOptimisticLock(
        @Param("idempotencyKey") String idempotencyKey,
        @Param("newState") ProcessingState newState,
        @Param("completedDate") LocalDateTime completedDate,
        @Param("errorDetails") String errorDetails,
        @Param("expectedVersion") Integer expectedVersion
    );
    
    /**
     * Update response payload and mark as completed.
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE FabricIdempotencyKeyEntity i 
        SET i.processingState = 'COMPLETED',
            i.responsePayload = :responsePayload,
            i.completedDate = CURRENT_TIMESTAMP,
            i.errorDetails = NULL,
            i.lockVersion = i.lockVersion + 1,
            i.lastAccessed = CURRENT_TIMESTAMP
        WHERE i.idempotencyKey = :idempotencyKey
        AND i.lockVersion = :expectedVersion
        """)
    int markCompletedWithPayload(
        @Param("idempotencyKey") String idempotencyKey,
        @Param("responsePayload") String responsePayload,
        @Param("expectedVersion") Integer expectedVersion
    );
    
    /**
     * Increment retry count and update state.
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE FabricIdempotencyKeyEntity i 
        SET i.retryCount = i.retryCount + 1,
            i.processingState = CASE 
                WHEN i.retryCount + 1 >= i.maxRetries THEN 'FAILED'
                ELSE 'IN_PROGRESS'
            END,
            i.lockVersion = i.lockVersion + 1,
            i.lastAccessed = CURRENT_TIMESTAMP
        WHERE i.idempotencyKey = :idempotencyKey
        AND i.lockVersion = :expectedVersion
        """)
    int incrementRetryCount(
        @Param("idempotencyKey") String idempotencyKey,
        @Param("expectedVersion") Integer expectedVersion
    );
    
    // ============================================================================
    // Cleanup and Maintenance Operations
    // ============================================================================
    
    /**
     * Count expired idempotency records for cleanup monitoring.
     */
    @Query("""
        SELECT COUNT(i) FROM FabricIdempotencyKeyEntity i 
        WHERE i.expiresAt < :currentTime
        """)
    long countExpiredRecords(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Delete expired idempotency records for automated cleanup.
     */
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM FabricIdempotencyKeyEntity i 
        WHERE i.expiresAt < :currentTime
        """)
    int deleteExpiredRecords(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find stale in-progress records that may need intervention.
     */
    @Query("""
        SELECT i FROM FabricIdempotencyKeyEntity i 
        WHERE i.processingState = 'IN_PROGRESS' 
        AND i.createdDate < :staleThreshold
        ORDER BY i.createdDate ASC
        """)
    List<FabricIdempotencyKeyEntity> findStaleInProgressRecords(
        @Param("staleThreshold") LocalDateTime staleThreshold
    );
    
    /**
     * Update last accessed timestamp for record maintenance.
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE FabricIdempotencyKeyEntity i 
        SET i.lastAccessed = CURRENT_TIMESTAMP
        WHERE i.idempotencyKey = :idempotencyKey
        """)
    int updateLastAccessed(@Param("idempotencyKey") String idempotencyKey);
    
    // ============================================================================
    // Query and Reporting Operations
    // ============================================================================
    
    /**
     * Find idempotency keys by processing state for monitoring.
     */
    List<FabricIdempotencyKeyEntity> findByProcessingStateOrderByCreatedDateDesc(ProcessingState state);
    
    /**
     * Find idempotency keys by source system and job name.
     */
    List<FabricIdempotencyKeyEntity> findBySourceSystemAndJobNameOrderByCreatedDateDesc(
        String sourceSystem, String jobName
    );
    
    /**
     * Find idempotency keys created within date range.
     */
    @Query("""
        SELECT i FROM FabricIdempotencyKeyEntity i 
        WHERE i.createdDate BETWEEN :startDate AND :endDate
        ORDER BY i.createdDate DESC
        """)
    List<FabricIdempotencyKeyEntity> findByDateRange(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find failed idempotency keys that can be retried.
     */
    @Query("""
        SELECT i FROM FabricIdempotencyKeyEntity i 
        WHERE i.processingState = 'FAILED'
        AND i.retryCount < i.maxRetries
        ORDER BY i.createdDate DESC
        """)
    List<FabricIdempotencyKeyEntity> findRetryableFailedRecords();
    
    /**
     * Find idempotency keys by processing node for distributed processing monitoring.
     */
    List<FabricIdempotencyKeyEntity> findByProcessingNodeOrderByCreatedDateDesc(String processingNode);
    
    /**
     * Find idempotency keys with specific retry count.
     */
    @Query("""
        SELECT i FROM FabricIdempotencyKeyEntity i 
        WHERE i.retryCount = :retryCount
        ORDER BY i.createdDate DESC
        """)
    List<FabricIdempotencyKeyEntity> findByRetryCount(@Param("retryCount") Integer retryCount);
    
    // ============================================================================
    // Performance and Statistics Queries
    // ============================================================================
    
    /**
     * Get idempotency statistics by processing state.
     */
    @Query("""
        SELECT i.processingState, COUNT(i), AVG(i.retryCount), 
               MIN(i.createdDate), MAX(i.createdDate)
        FROM FabricIdempotencyKeyEntity i 
        WHERE i.createdDate >= :fromDate
        GROUP BY i.processingState
        """)
    List<Object[]> getStatisticsByState(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get idempotency statistics by source system.
     */
    @Query("""
        SELECT i.sourceSystem, COUNT(i), 
               COUNT(CASE WHEN i.processingState = 'COMPLETED' THEN 1 END),
               COUNT(CASE WHEN i.processingState = 'FAILED' THEN 1 END),
               AVG(i.retryCount)
        FROM FabricIdempotencyKeyEntity i 
        WHERE i.createdDate >= :fromDate
        GROUP BY i.sourceSystem
        """)
    List<Object[]> getStatisticsBySourceSystem(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get daily idempotency volume.
     */
    @Query("""
        SELECT DATE(i.createdDate), COUNT(i),
               COUNT(CASE WHEN i.processingState = 'COMPLETED' THEN 1 END),
               COUNT(CASE WHEN i.processingState = 'FAILED' THEN 1 END)
        FROM FabricIdempotencyKeyEntity i 
        WHERE i.createdDate >= :fromDate
        GROUP BY DATE(i.createdDate)
        ORDER BY DATE(i.createdDate)
        """)
    List<Object[]> getDailyVolume(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get retry analysis data.
     */
    @Query("""
        SELECT i.retryCount, COUNT(i), i.processingState
        FROM FabricIdempotencyKeyEntity i 
        WHERE i.createdDate >= :fromDate
        GROUP BY i.retryCount, i.processingState
        ORDER BY i.retryCount, i.processingState
        """)
    List<Object[]> getRetryAnalysis(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Count records by state for dashboard metrics.
     */
    long countByProcessingState(ProcessingState state);
    
    /**
     * Count records by source system for monitoring.
     */
    long countBySourceSystem(String sourceSystem);
    
    /**
     * Count records created since specific date.
     */
    @Query("""
        SELECT COUNT(i) FROM FabricIdempotencyKeyEntity i 
        WHERE i.createdDate >= :fromDate
        """)
    long countCreatedSince(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get average retry count.
     */
    @Query("""
        SELECT AVG(i.retryCount) FROM FabricIdempotencyKeyEntity i 
        WHERE i.retryCount > 0 AND i.createdDate >= :fromDate
        """)
    Double getAverageRetryCount(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get processing time statistics (for completed records with completion date).
     */
    @Query("""
        SELECT AVG(TIMESTAMPDIFF(SECOND, i.createdDate, i.completedDate)),
               MIN(TIMESTAMPDIFF(SECOND, i.createdDate, i.completedDate)),
               MAX(TIMESTAMPDIFF(SECOND, i.createdDate, i.completedDate))
        FROM FabricIdempotencyKeyEntity i 
        WHERE i.processingState = 'COMPLETED'
        AND i.createdDate >= :fromDate
        AND i.completedDate IS NOT NULL
        """)
    Object[] getProcessingTimeStatistics(@Param("fromDate") LocalDateTime fromDate);
    
    // ============================================================================
    // Advanced Search and Filtering
    // ============================================================================
    
    /**
     * Find idempotency keys by job name pattern for pattern matching.
     */
    @Query("""
        SELECT i FROM FabricIdempotencyKeyEntity i 
        WHERE i.jobName LIKE :jobNamePattern
        ORDER BY i.createdDate DESC
        """)
    List<FabricIdempotencyKeyEntity> findByJobNamePattern(@Param("jobNamePattern") String jobNamePattern);
    
    /**
     * Find idempotency keys with error details for error analysis.
     */
    @Query("""
        SELECT i FROM FabricIdempotencyKeyEntity i 
        WHERE i.errorDetails IS NOT NULL 
        AND i.errorDetails != ''
        ORDER BY i.createdDate DESC
        """)
    List<FabricIdempotencyKeyEntity> findWithErrorDetails();
    
    /**
     * Find idempotency keys by created by user for user tracking.
     */
    List<FabricIdempotencyKeyEntity> findByCreatedByOrderByCreatedDateDesc(String createdBy);
    
    /**
     * Find duplicate requests by request hash.
     */
    @Query("""
        SELECT i FROM FabricIdempotencyKeyEntity i 
        WHERE i.requestHash = :requestHash
        ORDER BY i.createdDate DESC
        """)
    List<FabricIdempotencyKeyEntity> findByRequestHash(@Param("requestHash") String requestHash);
    
    /**
     * Count concurrent processing records (same source system and job).
     */
    @Query("""
        SELECT COUNT(i) FROM FabricIdempotencyKeyEntity i 
        WHERE i.sourceSystem = :sourceSystem
        AND i.jobName = :jobName
        AND i.processingState = 'IN_PROGRESS'
        """)
    long countConcurrentProcessing(
        @Param("sourceSystem") String sourceSystem,
        @Param("jobName") String jobName
    );
}