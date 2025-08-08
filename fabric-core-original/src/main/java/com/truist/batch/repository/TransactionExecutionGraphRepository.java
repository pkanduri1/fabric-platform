package com.truist.batch.repository;

import com.truist.batch.entity.TransactionExecutionGraphEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Epic 3: Transaction Execution Graph Repository
 * 
 * JPA repository for managing transaction execution graphs with advanced query capabilities
 * for graph algorithm support, performance optimization, and real-time monitoring.
 * 
 * Features:
 * - Graph traversal and analysis queries
 * - Performance-optimized queries with proper indexing
 * - Real-time status tracking and monitoring
 * - Batch operations for large execution graphs
 * - Audit trail support with correlation tracking
 * - Parallel processing coordination
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 3
 */
@Repository
public interface TransactionExecutionGraphRepository extends JpaRepository<TransactionExecutionGraphEntity, Long> {

    /**
     * Find all nodes for a specific execution ordered by topological order
     * Used for sequential processing and status monitoring
     * 
     * @param executionId execution identifier
     * @return list of graph nodes in topological order
     */
    List<TransactionExecutionGraphEntity> findByExecutionIdOrderByTopologicalOrder(String executionId);

    /**
     * Find specific transaction node in execution graph
     * Used for individual transaction status tracking
     * 
     * @param executionId execution identifier
     * @param transactionId transaction identifier
     * @return optional graph entity
     */
    Optional<TransactionExecutionGraphEntity> findByExecutionIdAndTransactionId(String executionId, String transactionId);

    /**
     * Find nodes by execution, in-degree, and processing status
     * Used for finding ready-to-process transactions (in-degree = 0, status = WHITE)
     * 
     * @param executionId execution identifier
     * @param inDegree in-degree value
     * @param processingStatus processing status
     * @return list of matching graph nodes
     */
    List<TransactionExecutionGraphEntity> findByExecutionIdAndInDegreeAndProcessingStatus(
            String executionId, 
            Integer inDegree, 
            TransactionExecutionGraphEntity.ProcessingStatus processingStatus);

    /**
     * Find nodes by execution and processing status
     * Used for monitoring and status tracking
     * 
     * @param executionId execution identifier
     * @param processingStatus processing status
     * @return list of nodes with specified status
     */
    List<TransactionExecutionGraphEntity> findByExecutionIdAndProcessingStatus(
            String executionId, 
            TransactionExecutionGraphEntity.ProcessingStatus processingStatus);

    /**
     * Find nodes by execution and graph level
     * Used for level-by-level processing coordination
     * 
     * @param executionId execution identifier
     * @param graphLevel graph level
     * @return list of nodes at specified level
     */
    List<TransactionExecutionGraphEntity> findByExecutionIdAndGraphLevel(String executionId, Integer graphLevel);

    /**
     * Find nodes ready for parallel processing at specific level
     * Used for optimizing parallel execution
     * 
     * @param executionId execution identifier
     * @param graphLevel graph level
     * @param processingStatus processing status (typically WHITE)
     * @param inDegree in-degree value (typically 0)
     * @return list of nodes ready for parallel processing
     */
    @Query("""
        SELECT teg FROM TransactionExecutionGraphEntity teg 
        WHERE teg.executionId = :executionId 
        AND teg.graphLevel = :graphLevel 
        AND teg.processingStatus = :processingStatus 
        AND teg.inDegree = :inDegree
        ORDER BY teg.topologicalOrder ASC
        """)
    List<TransactionExecutionGraphEntity> findReadyNodesAtLevel(
            @Param("executionId") String executionId,
            @Param("graphLevel") Integer graphLevel,
            @Param("processingStatus") TransactionExecutionGraphEntity.ProcessingStatus processingStatus,
            @Param("inDegree") Integer inDegree);

    /**
     * Find nodes by thread assignment
     * Used for thread-specific monitoring and coordination
     * 
     * @param executionId execution identifier
     * @param threadAssignment thread assignment identifier
     * @return list of nodes assigned to specific thread
     */
    List<TransactionExecutionGraphEntity> findByExecutionIdAndThreadAssignment(
            String executionId, String threadAssignment);

    /**
     * Find source nodes (in-degree = 0) for execution
     * Used for identifying starting points in execution graph
     * 
     * @param executionId execution identifier
     * @return list of source nodes
     */
    @Query("""
        SELECT teg FROM TransactionExecutionGraphEntity teg 
        WHERE teg.executionId = :executionId 
        AND teg.inDegree = 0
        ORDER BY teg.topologicalOrder ASC
        """)
    List<TransactionExecutionGraphEntity> findSourceNodes(@Param("executionId") String executionId);

    /**
     * Find sink nodes (out-degree = 0) for execution
     * Used for identifying terminal nodes in execution graph
     * 
     * @param executionId execution identifier
     * @return list of sink nodes
     */
    @Query("""
        SELECT teg FROM TransactionExecutionGraphEntity teg 
        WHERE teg.executionId = :executionId 
        AND teg.outDegree = 0
        ORDER BY teg.topologicalOrder ASC
        """)
    List<TransactionExecutionGraphEntity> findSinkNodes(@Param("executionId") String executionId);

    /**
     * Get execution statistics for monitoring
     * Returns aggregated status counts for dashboard
     * 
     * @param executionId execution identifier
     * @return execution statistics
     */
    @Query("""
        SELECT 
            teg.processingStatus as status,
            COUNT(teg) as count,
            AVG(teg.graphLevel) as avgLevel,
            MIN(teg.topologicalOrder) as minOrder,
            MAX(teg.topologicalOrder) as maxOrder
        FROM TransactionExecutionGraphEntity teg 
        WHERE teg.executionId = :executionId
        GROUP BY teg.processingStatus
        ORDER BY COUNT(teg) DESC
        """)
    List<Object[]> getExecutionStatistics(@Param("executionId") String executionId);

    /**
     * Find nodes with specific correlation ID
     * Used for correlation tracking and audit trails
     * 
     * @param correlationId correlation identifier
     * @return list of nodes with matching correlation ID
     */
    List<TransactionExecutionGraphEntity> findByCorrelationIdOrderByTopologicalOrder(String correlationId);

    /**
     * Find nodes by business date
     * Used for date-based queries and archival
     * 
     * @param businessDate business date
     * @return list of nodes for specific business date
     */
    List<TransactionExecutionGraphEntity> findByBusinessDate(Date businessDate);

    /**
     * Find long-running transactions
     * Used for performance monitoring and bottleneck identification
     * 
     * @param executionId execution identifier
     * @param processingStatus processing status (typically GRAY for in-progress)
     * @return list of long-running transactions
     */
    @Query("""
        SELECT teg FROM TransactionExecutionGraphEntity teg 
        WHERE teg.executionId = :executionId 
        AND teg.processingStatus = :processingStatus 
        AND teg.startTime IS NOT NULL
        ORDER BY teg.startTime ASC
        """)
    List<TransactionExecutionGraphEntity> findLongRunningTransactions(
            @Param("executionId") String executionId,
            @Param("processingStatus") TransactionExecutionGraphEntity.ProcessingStatus processingStatus);

    /**
     * Find blocked transactions (positive in-degree)
     * Used for identifying bottlenecks and blocked dependencies
     * 
     * @param executionId execution identifier
     * @param minInDegree minimum in-degree threshold
     * @return list of blocked transactions
     */
    @Query("""
        SELECT teg FROM TransactionExecutionGraphEntity teg 
        WHERE teg.executionId = :executionId 
        AND teg.inDegree >= :minInDegree
        AND teg.processingStatus = 'WHITE'
        ORDER BY teg.inDegree DESC
        """)
    List<TransactionExecutionGraphEntity> findBlockedTransactions(
            @Param("executionId") String executionId,
            @Param("minInDegree") Integer minInDegree);

    /**
     * Get critical path analysis
     * Find transactions with highest out-degree (most dependencies)
     * 
     * @param executionId execution identifier
     * @param minOutDegree minimum out-degree threshold
     * @return list of critical path transactions
     */
    @Query("""
        SELECT teg FROM TransactionExecutionGraphEntity teg 
        WHERE teg.executionId = :executionId 
        AND teg.outDegree >= :minOutDegree
        ORDER BY teg.outDegree DESC, teg.topologicalOrder ASC
        """)
    List<TransactionExecutionGraphEntity> findCriticalPathTransactions(
            @Param("executionId") String executionId,
            @Param("minOutDegree") Integer minOutDegree);

    /**
     * Count nodes by status for execution
     * Used for progress tracking and monitoring dashboards
     * 
     * @param executionId execution identifier
     * @param processingStatus processing status
     * @return count of nodes with specified status
     */
    Long countByExecutionIdAndProcessingStatus(String executionId, 
            TransactionExecutionGraphEntity.ProcessingStatus processingStatus);

    /**
     * Bulk update processing status
     * Used for batch status updates during execution
     * 
     * @param executionId execution identifier
     * @param transactionIds list of transaction IDs to update
     * @param newStatus new processing status
     * @return number of updated records
     */
    @Modifying
    @Query("""
        UPDATE TransactionExecutionGraphEntity teg 
        SET teg.processingStatus = :newStatus
        WHERE teg.executionId = :executionId 
        AND teg.transactionId IN :transactionIds
        """)
    int bulkUpdateProcessingStatus(@Param("executionId") String executionId,
                                  @Param("transactionIds") List<String> transactionIds,
                                  @Param("newStatus") TransactionExecutionGraphEntity.ProcessingStatus newStatus);

    /**
     * Bulk update thread assignments
     * Used for parallel processing coordination
     * 
     * @param executionId execution identifier
     * @param transactionIds list of transaction IDs
     * @param threadAssignment thread assignment identifier
     * @return number of updated records
     */
    @Modifying
    @Query("""
        UPDATE TransactionExecutionGraphEntity teg 
        SET teg.threadAssignment = :threadAssignment
        WHERE teg.executionId = :executionId 
        AND teg.transactionId IN :transactionIds
        """)
    int bulkUpdateThreadAssignment(@Param("executionId") String executionId,
                                  @Param("transactionIds") List<String> transactionIds,
                                  @Param("threadAssignment") String threadAssignment);

    /**
     * Delete all nodes for specific execution
     * Used for cleanup after execution completion
     * 
     * @param executionId execution identifier
     * @return number of deleted records
     */
    @Modifying
    int deleteByExecutionId(String executionId);

    /**
     * Delete nodes older than specified business date
     * Used for data archival and cleanup
     * 
     * @param cutoffDate cutoff business date
     * @return number of deleted records
     */
    @Modifying
    @Query("""
        DELETE FROM TransactionExecutionGraphEntity teg 
        WHERE teg.businessDate < :cutoffDate
        """)
    int deleteByBusinessDateBefore(@Param("cutoffDate") Date cutoffDate);

    /**
     * Find executions with unresolved dependencies (debugging)
     * Used for identifying stuck executions
     * 
     * @param cutoffDate cutoff date for old executions
     * @return list of executions with potential issues
     */
    @Query("""
        SELECT DISTINCT teg.executionId 
        FROM TransactionExecutionGraphEntity teg 
        WHERE teg.businessDate < :cutoffDate
        AND teg.processingStatus IN ('WHITE', 'GRAY', 'BLOCKED')
        """)
    List<String> findStuckExecutions(@Param("cutoffDate") Date cutoffDate);

    /**
     * Get performance metrics for execution
     * Calculate processing times and throughput
     * 
     * @param executionId execution identifier
     * @return performance metrics
     */
    @Query("""
        SELECT 
            COUNT(teg) as totalNodes,
            SUM(CASE WHEN teg.processingStatus = 'BLACK' THEN 1 ELSE 0 END) as completedNodes,
            AVG(teg.graphLevel) as avgGraphLevel,
            MAX(teg.topologicalOrder) as maxTopologicalOrder,
            COUNT(DISTINCT teg.threadAssignment) as threadCount
        FROM TransactionExecutionGraphEntity teg 
        WHERE teg.executionId = :executionId
        """)
    List<Object[]> getPerformanceMetrics(@Param("executionId") String executionId);

    /**
     * Find nodes with resource locks
     * Used for resource contention analysis
     * 
     * @param executionId execution identifier
     * @return list of nodes with resource locks
     */
    @Query("""
        SELECT teg FROM TransactionExecutionGraphEntity teg 
        WHERE teg.executionId = :executionId 
        AND teg.resourceLocks IS NOT NULL
        ORDER BY teg.topologicalOrder ASC
        """)
    List<TransactionExecutionGraphEntity> findNodesWithResourceLocks(@Param("executionId") String executionId);

    /**
     * Get graph topology summary
     * Provides overview of graph structure for analysis
     * 
     * @param executionId execution identifier
     * @return topology summary data
     */
    @Query("""
        SELECT 
            MIN(teg.graphLevel) as minLevel,
            MAX(teg.graphLevel) as maxLevel,
            COUNT(DISTINCT teg.graphLevel) as levelCount,
            MAX(teg.inDegree) as maxInDegree,
            MAX(teg.outDegree) as maxOutDegree,
            AVG(CAST(teg.inDegree as DOUBLE)) as avgInDegree,
            AVG(CAST(teg.outDegree as DOUBLE)) as avgOutDegree
        FROM TransactionExecutionGraphEntity teg 
        WHERE teg.executionId = :executionId
        """)
    List<Object[]> getGraphTopologySummary(@Param("executionId") String executionId);
}