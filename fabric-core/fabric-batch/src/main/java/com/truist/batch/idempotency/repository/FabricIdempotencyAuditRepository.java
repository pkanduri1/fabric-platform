package com.truist.batch.idempotency.repository;

import com.truist.batch.idempotency.entity.FabricIdempotencyAuditEntity;
import com.truist.batch.idempotency.entity.FabricIdempotencyKeyEntity.ProcessingState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for FabricIdempotencyAuditEntity operations.
 * Provides comprehensive data access methods for audit trail tracking,
 * compliance reporting, and operational monitoring of idempotency operations.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Repository
public interface FabricIdempotencyAuditRepository extends JpaRepository<FabricIdempotencyAuditEntity, Long> {
    
    // ============================================================================
    // Core Audit Trail Operations
    // ============================================================================
    
    /**
     * Find all audit entries for a specific idempotency key.
     */
    List<FabricIdempotencyAuditEntity> findByIdempotencyKeyOrderByChangeDateDesc(String idempotencyKey);
    
    /**
     * Find audit entries by state change for monitoring state transitions.
     */
    @Query("""
        SELECT a FROM FabricIdempotencyAuditEntity a 
        WHERE a.oldState = :oldState 
        AND a.newState = :newState
        ORDER BY a.changeDate DESC
        """)
    List<FabricIdempotencyAuditEntity> findByStateTransition(
        @Param("oldState") ProcessingState oldState,
        @Param("newState") ProcessingState newState
    );
    
    /**
     * Find audit entries by changed by user for user activity tracking.
     */
    List<FabricIdempotencyAuditEntity> findByChangedByOrderByChangeDateDesc(String changedBy);
    
    /**
     * Find recent audit entries within specified time window.
     */
    @Query("""
        SELECT a FROM FabricIdempotencyAuditEntity a 
        WHERE a.changeDate >= :fromDate
        ORDER BY a.changeDate DESC
        """)
    List<FabricIdempotencyAuditEntity> findRecentAuditEntries(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Find audit entries within date range for reporting.
     */
    @Query("""
        SELECT a FROM FabricIdempotencyAuditEntity a 
        WHERE a.changeDate BETWEEN :startDate AND :endDate
        ORDER BY a.changeDate DESC
        """)
    List<FabricIdempotencyAuditEntity> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // ============================================================================
    // State Transition Analysis
    // ============================================================================
    
    /**
     * Find failure transition audit entries for error analysis.
     */
    @Query("""
        SELECT a FROM FabricIdempotencyAuditEntity a 
        WHERE a.newState = 'FAILED'
        ORDER BY a.changeDate DESC
        """)
    List<FabricIdempotencyAuditEntity> findFailureTransitions();
    
    /**
     * Find completion transition audit entries for success tracking.
     */
    @Query("""
        SELECT a FROM FabricIdempotencyAuditEntity a 
        WHERE a.newState = 'COMPLETED'
        ORDER BY a.changeDate DESC
        """)
    List<FabricIdempotencyAuditEntity> findCompletionTransitions();
    
    /**
     * Find retry attempt audit entries for retry analysis.
     */
    @Query("""
        SELECT a FROM FabricIdempotencyAuditEntity a 
        WHERE a.oldState = 'FAILED' 
        AND a.newState = 'IN_PROGRESS'
        ORDER BY a.changeDate DESC
        """)
    List<FabricIdempotencyAuditEntity> findRetryAttempts();
    
    /**
     * Find audit entries for specific state transitions with reason pattern.
     */
    @Query("""
        SELECT a FROM FabricIdempotencyAuditEntity a 
        WHERE a.newState = :newState
        AND a.stateChangeReason LIKE :reasonPattern
        ORDER BY a.changeDate DESC
        """)
    List<FabricIdempotencyAuditEntity> findByStateAndReasonPattern(
        @Param("newState") ProcessingState newState,
        @Param("reasonPattern") String reasonPattern
    );
    
    // ============================================================================
    // Client and Session Tracking
    // ============================================================================
    
    /**
     * Find audit entries by client IP for security monitoring.
     */
    List<FabricIdempotencyAuditEntity> findByClientIpOrderByChangeDateDesc(String clientIp);
    
    /**
     * Find audit entries by session ID for session tracking.
     */
    List<FabricIdempotencyAuditEntity> findBySessionIdOrderByChangeDateDesc(String sessionId);
    
    /**
     * Find audit entries by device fingerprint for device tracking.
     */
    List<FabricIdempotencyAuditEntity> findByDeviceFingerprintOrderByChangeDateDesc(String deviceFingerprint);
    
    /**
     * Find audit entries by user agent pattern for client analysis.
     */
    @Query("""
        SELECT a FROM FabricIdempotencyAuditEntity a 
        WHERE a.userAgent LIKE :userAgentPattern
        ORDER BY a.changeDate DESC
        """)
    List<FabricIdempotencyAuditEntity> findByUserAgentPattern(@Param("userAgentPattern") String userAgentPattern);
    
    // ============================================================================
    // Business Context and Processing Analysis
    // ============================================================================
    
    /**
     * Find audit entries by business context for business analysis.
     */
    List<FabricIdempotencyAuditEntity> findByBusinessContextOrderByChangeDateDesc(String businessContext);
    
    /**
     * Find audit entries with processing context for technical analysis.
     */
    @Query("""
        SELECT a FROM FabricIdempotencyAuditEntity a 
        WHERE a.processingContext IS NOT NULL 
        AND a.processingContext != ''
        ORDER BY a.changeDate DESC
        """)
    List<FabricIdempotencyAuditEntity> findWithProcessingContext();
    
    /**
     * Find audit entries by processing context pattern.
     */
    @Query("""
        SELECT a FROM FabricIdempotencyAuditEntity a 
        WHERE a.processingContext LIKE :contextPattern
        ORDER BY a.changeDate DESC
        """)
    List<FabricIdempotencyAuditEntity> findByProcessingContextPattern(@Param("contextPattern") String contextPattern);
    
    // ============================================================================
    // Statistical and Reporting Queries
    // ============================================================================
    
    /**
     * Get audit statistics by state transitions.
     */
    @Query("""
        SELECT a.oldState, a.newState, COUNT(a), 
               MIN(a.changeDate), MAX(a.changeDate)
        FROM FabricIdempotencyAuditEntity a 
        WHERE a.changeDate >= :fromDate
        AND a.oldState IS NOT NULL 
        AND a.newState IS NOT NULL
        GROUP BY a.oldState, a.newState
        ORDER BY COUNT(a) DESC
        """)
    List<Object[]> getStateTransitionStatistics(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get audit statistics by user for user activity analysis.
     */
    @Query("""
        SELECT a.changedBy, COUNT(a),
               COUNT(CASE WHEN a.newState = 'COMPLETED' THEN 1 END),
               COUNT(CASE WHEN a.newState = 'FAILED' THEN 1 END),
               MIN(a.changeDate), MAX(a.changeDate)
        FROM FabricIdempotencyAuditEntity a 
        WHERE a.changeDate >= :fromDate
        GROUP BY a.changedBy
        ORDER BY COUNT(a) DESC
        """)
    List<Object[]> getUserActivityStatistics(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get hourly audit volume for monitoring.
     */
    @Query("""
        SELECT DATE_FORMAT(a.changeDate, '%Y-%m-%d %H:00:00'), COUNT(a)
        FROM FabricIdempotencyAuditEntity a 
        WHERE a.changeDate >= :fromDate
        GROUP BY DATE_FORMAT(a.changeDate, '%Y-%m-%d %H:00:00')
        ORDER BY DATE_FORMAT(a.changeDate, '%Y-%m-%d %H:00:00')
        """)
    List<Object[]> getHourlyAuditVolume(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get daily audit volume for reporting.
     */
    @Query("""
        SELECT DATE(a.changeDate), COUNT(a),
               COUNT(CASE WHEN a.newState = 'COMPLETED' THEN 1 END),
               COUNT(CASE WHEN a.newState = 'FAILED' THEN 1 END)
        FROM FabricIdempotencyAuditEntity a 
        WHERE a.changeDate >= :fromDate
        GROUP BY DATE(a.changeDate)
        ORDER BY DATE(a.changeDate)
        """)
    List<Object[]> getDailyAuditVolume(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get failure rate analysis.
     */
    @Query("""
        SELECT DATE(a.changeDate), 
               COUNT(CASE WHEN a.newState = 'FAILED' THEN 1 END) * 100.0 / COUNT(*) as failure_rate
        FROM FabricIdempotencyAuditEntity a 
        WHERE a.changeDate >= :fromDate
        AND a.newState IN ('COMPLETED', 'FAILED')
        GROUP BY DATE(a.changeDate)
        ORDER BY DATE(a.changeDate)
        """)
    List<Object[]> getFailureRateAnalysis(@Param("fromDate") LocalDateTime fromDate);
    
    // ============================================================================
    // Security and Compliance Queries
    // ============================================================================
    
    /**
     * Find suspicious activity based on multiple criteria.
     */
    @Query("""
        SELECT a FROM FabricIdempotencyAuditEntity a 
        WHERE a.changeDate >= :fromDate
        AND (a.clientIp IN :suspiciousIps 
             OR a.changedBy LIKE '%admin%' 
             OR a.stateChangeReason LIKE '%error%')
        ORDER BY a.changeDate DESC
        """)
    List<FabricIdempotencyAuditEntity> findSuspiciousActivity(
        @Param("fromDate") LocalDateTime fromDate,
        @Param("suspiciousIps") List<String> suspiciousIps
    );
    
    /**
     * Find audit entries by client IP pattern for security analysis.
     */
    @Query("""
        SELECT a FROM FabricIdempotencyAuditEntity a 
        WHERE a.clientIp LIKE :ipPattern
        ORDER BY a.changeDate DESC
        """)
    List<FabricIdempotencyAuditEntity> findByClientIpPattern(@Param("ipPattern") String ipPattern);
    
    /**
     * Count unique users activity for compliance reporting.
     */
    @Query("""
        SELECT COUNT(DISTINCT a.changedBy) 
        FROM FabricIdempotencyAuditEntity a 
        WHERE a.changeDate >= :fromDate
        """)
    long countUniqueUsers(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Count unique client IPs for security monitoring.
     */
    @Query("""
        SELECT COUNT(DISTINCT a.clientIp) 
        FROM FabricIdempotencyAuditEntity a 
        WHERE a.changeDate >= :fromDate
        AND a.clientIp IS NOT NULL
        """)
    long countUniqueClientIps(@Param("fromDate") LocalDateTime fromDate);
    
    // ============================================================================
    // Maintenance and Cleanup Operations
    // ============================================================================
    
    /**
     * Delete old audit records based on retention policy.
     */
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM FabricIdempotencyAuditEntity a 
        WHERE a.changeDate < :retentionDate
        """)
    int deleteOldAuditRecords(@Param("retentionDate") LocalDateTime retentionDate);
    
    /**
     * Count audit records older than retention period.
     */
    @Query("""
        SELECT COUNT(a) FROM FabricIdempotencyAuditEntity a 
        WHERE a.changeDate < :retentionDate
        """)
    long countOldAuditRecords(@Param("retentionDate") LocalDateTime retentionDate);
    
    /**
     * Find oldest audit records for cleanup prioritization.
     */
    @Query("""
        SELECT a FROM FabricIdempotencyAuditEntity a 
        ORDER BY a.changeDate ASC
        """)
    List<FabricIdempotencyAuditEntity> findOldestAuditRecords();
    
    // ============================================================================
    // Performance Monitoring Queries
    // ============================================================================
    
    /**
     * Count audit entries by change date for performance monitoring.
     */
    @Query("""
        SELECT COUNT(a) FROM FabricIdempotencyAuditEntity a 
        WHERE a.changeDate >= :fromDate
        """)
    long countAuditEntriesSince(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Count audit entries by new state for state distribution analysis.
     */
    long countByNewState(ProcessingState newState);
    
    /**
     * Count state transitions for transition frequency analysis.
     */
    @Query("""
        SELECT COUNT(a) FROM FabricIdempotencyAuditEntity a 
        WHERE a.oldState IS NOT NULL 
        AND a.newState IS NOT NULL
        AND a.changeDate >= :fromDate
        """)
    long countStateTransitions(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get average time between state changes for performance analysis.
     */
    @Query("""
        SELECT AVG(TIMESTAMPDIFF(SECOND, 
            LAG(a.changeDate) OVER (PARTITION BY a.idempotencyKey ORDER BY a.changeDate),
            a.changeDate))
        FROM FabricIdempotencyAuditEntity a 
        WHERE a.changeDate >= :fromDate
        """)
    Double getAverageTimeBetweenStateChanges(@Param("fromDate") LocalDateTime fromDate);
    
    // ============================================================================
    // Advanced Analytics Queries
    // ============================================================================
    
    /**
     * Find the most common state transition patterns.
     */
    @Query("""
        SELECT CONCAT(COALESCE(a.oldState, 'NULL'), ' -> ', COALESCE(a.newState, 'NULL')) as transition,
               COUNT(*) as count
        FROM FabricIdempotencyAuditEntity a 
        WHERE a.changeDate >= :fromDate
        GROUP BY CONCAT(COALESCE(a.oldState, 'NULL'), ' -> ', COALESCE(a.newState, 'NULL'))
        ORDER BY count DESC
        """)
    List<Object[]> getCommonTransitionPatterns(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Find audit trails for specific idempotency keys with full timeline.
     */
    @Query("""
        SELECT a FROM FabricIdempotencyAuditEntity a 
        WHERE a.idempotencyKey IN :idempotencyKeys
        ORDER BY a.idempotencyKey, a.changeDate ASC
        """)
    List<FabricIdempotencyAuditEntity> findAuditTrailForKeys(@Param("idempotencyKeys") List<String> idempotencyKeys);
    
    /**
     * Get processing efficiency metrics (successful vs failed operations).
     */
    @Query("""
        SELECT 
            COUNT(CASE WHEN a.newState = 'COMPLETED' THEN 1 END) * 100.0 / COUNT(*) as success_rate,
            COUNT(CASE WHEN a.newState = 'FAILED' THEN 1 END) * 100.0 / COUNT(*) as failure_rate,
            COUNT(*) as total_operations
        FROM FabricIdempotencyAuditEntity a 
        WHERE a.changeDate >= :fromDate
        AND a.newState IN ('COMPLETED', 'FAILED')
        """)
    Object[] getProcessingEfficiencyMetrics(@Param("fromDate") LocalDateTime fromDate);
}