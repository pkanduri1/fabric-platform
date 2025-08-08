package com.truist.batch.repository;

import com.truist.batch.entity.TransactionDependencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Epic 3: Transaction Dependency Repository
 * 
 * JPA repository for managing transaction dependencies with advanced query capabilities
 * for graph algorithm support, dependency validation, and performance optimization.
 * 
 * Features:
 * - Complex dependency queries for graph algorithms
 * - Performance-optimized queries with proper indexing
 * - Dependency validation and conflict detection
 * - Audit trail support
 * - Batch operations for large dependency sets
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 3
 */
@Repository
public interface TransactionDependencyRepository extends JpaRepository<TransactionDependencyEntity, Long> {

    /**
     * Find all active dependencies for a set of transactions
     * Used by TransactionSequencer for building adjacency lists
     * 
     * @param transactionIds set of transaction IDs
     * @return list of active dependencies
     */
    @Query("""
        SELECT td FROM TransactionDependencyEntity td 
        WHERE td.activeFlag = 'Y' 
        AND (td.sourceTransactionId IN :transactionIds 
             OR td.targetTransactionId IN :transactionIds)
        ORDER BY td.priorityWeight DESC, td.createdDate ASC
        """)
    List<TransactionDependencyEntity> findActiveDependenciesForTransactions(
            @Param("transactionIds") Set<String> transactionIds);

    /**
     * Find dependencies where source transaction is in the given set
     * Used for outgoing dependency analysis
     * 
     * @param sourceTransactionIds set of source transaction IDs
     * @return list of dependencies
     */
    @Query("""
        SELECT td FROM TransactionDependencyEntity td 
        WHERE td.activeFlag = 'Y' 
        AND td.sourceTransactionId IN :sourceTransactionIds
        ORDER BY td.priorityWeight DESC
        """)
    List<TransactionDependencyEntity> findBySourceTransactionIds(
            @Param("sourceTransactionIds") Set<String> sourceTransactionIds);

    /**
     * Find dependencies where target transaction is in the given set
     * Used for incoming dependency analysis
     * 
     * @param targetTransactionIds set of target transaction IDs
     * @return list of dependencies
     */
    @Query("""
        SELECT td FROM TransactionDependencyEntity td 
        WHERE td.activeFlag = 'Y' 
        AND td.targetTransactionId IN :targetTransactionIds
        ORDER BY td.priorityWeight DESC
        """)
    List<TransactionDependencyEntity> findByTargetTransactionIds(
            @Param("targetTransactionIds") Set<String> targetTransactionIds);

    /**
     * Find specific dependency between two transactions
     * Used for dependency existence validation
     * 
     * @param sourceTransactionId source transaction ID
     * @param targetTransactionId target transaction ID
     * @return optional dependency entity
     */
    Optional<TransactionDependencyEntity> findBySourceTransactionIdAndTargetTransactionId(
            String sourceTransactionId, String targetTransactionId);

    /**
     * Find all dependencies by type
     * Used for analyzing specific dependency patterns
     * 
     * @param dependencyType dependency type
     * @return list of dependencies
     */
    @Query("""
        SELECT td FROM TransactionDependencyEntity td 
        WHERE td.activeFlag = 'Y' 
        AND td.dependencyType = :dependencyType
        ORDER BY td.priorityWeight DESC
        """)
    List<TransactionDependencyEntity> findByDependencyType(
            @Param("dependencyType") TransactionDependencyEntity.DependencyType dependencyType);

    /**
     * Find high priority dependencies (priority >= threshold)
     * Used for critical path analysis
     * 
     * @param priorityThreshold minimum priority threshold
     * @return list of high priority dependencies
     */
    @Query("""
        SELECT td FROM TransactionDependencyEntity td 
        WHERE td.activeFlag = 'Y' 
        AND td.priorityWeight >= :priorityThreshold
        ORDER BY td.priorityWeight DESC, td.createdDate ASC
        """)
    List<TransactionDependencyEntity> findHighPriorityDependencies(
            @Param("priorityThreshold") Integer priorityThreshold);

    /**
     * Find dependencies with conditional logic
     * Used for conditional dependency evaluation
     * 
     * @return list of conditional dependencies
     */
    @Query("""
        SELECT td FROM TransactionDependencyEntity td 
        WHERE td.activeFlag = 'Y' 
        AND td.dependencyType = 'CONDITIONAL'
        AND td.dependencyCondition IS NOT NULL
        ORDER BY td.priorityWeight DESC
        """)
    List<TransactionDependencyEntity> findConditionalDependencies();

    /**
     * Find potential cycles by looking for bidirectional dependencies
     * Used for cycle prevention during dependency creation
     * 
     * @param transactionId1 first transaction ID
     * @param transactionId2 second transaction ID
     * @return list of dependencies forming potential cycles
     */
    @Query("""
        SELECT td FROM TransactionDependencyEntity td 
        WHERE td.activeFlag = 'Y' 
        AND ((td.sourceTransactionId = :transactionId1 AND td.targetTransactionId = :transactionId2)
             OR (td.sourceTransactionId = :transactionId2 AND td.targetTransactionId = :transactionId1))
        """)
    List<TransactionDependencyEntity> findBidirectionalDependencies(
            @Param("transactionId1") String transactionId1,
            @Param("transactionId2") String transactionId2);

    /**
     * Count dependencies for a specific transaction (both incoming and outgoing)
     * Used for complexity analysis
     * 
     * @param transactionId transaction ID
     * @return dependency count
     */
    @Query("""
        SELECT COUNT(td) FROM TransactionDependencyEntity td 
        WHERE td.activeFlag = 'Y' 
        AND (td.sourceTransactionId = :transactionId 
             OR td.targetTransactionId = :transactionId)
        """)
    Long countDependenciesForTransaction(@Param("transactionId") String transactionId);

    /**
     * Find dependencies created or modified after specified date
     * Used for audit and change tracking
     * 
     * @param fromDate date threshold
     * @return list of recent dependencies
     */
    @Query("""
        SELECT td FROM TransactionDependencyEntity td 
        WHERE td.createdDate >= :fromDate 
        OR td.lastModifiedDate >= :fromDate
        ORDER BY COALESCE(td.lastModifiedDate, td.createdDate) DESC
        """)
    List<TransactionDependencyEntity> findRecentDependencies(
            @Param("fromDate") LocalDateTime fromDate);

    /**
     * Find dependencies requiring specific compliance level
     * Used for compliance validation
     * 
     * @param complianceLevel compliance level requirement
     * @return list of compliance dependencies
     */
    @Query("""
        SELECT td FROM TransactionDependencyEntity td 
        WHERE td.activeFlag = 'Y' 
        AND td.complianceLevel = :complianceLevel
        ORDER BY td.priorityWeight DESC
        """)
    List<TransactionDependencyEntity> findByComplianceLevel(
            @Param("complianceLevel") String complianceLevel);

    /**
     * Find dependencies with long wait times (potential bottlenecks)
     * Used for performance optimization
     * 
     * @param maxWaitThreshold maximum wait time threshold in seconds
     * @return list of bottleneck dependencies
     */
    @Query("""
        SELECT td FROM TransactionDependencyEntity td 
        WHERE td.activeFlag = 'Y' 
        AND td.maxWaitTimeSeconds >= :maxWaitThreshold
        ORDER BY td.maxWaitTimeSeconds DESC
        """)
    List<TransactionDependencyEntity> findBottleneckDependencies(
            @Param("maxWaitThreshold") Integer maxWaitThreshold);

    /**
     * Find dependencies by retry policy
     * Used for resilience analysis
     * 
     * @param retryPolicy retry policy pattern
     * @return list of dependencies with specific retry policy
     */
    List<TransactionDependencyEntity> findByRetryPolicyContainingIgnoreCase(String retryPolicy);

    /**
     * Find dependencies created by specific user
     * Used for audit and ownership tracking
     * 
     * @param createdBy user identifier
     * @return list of dependencies created by user
     */
    List<TransactionDependencyEntity> findByCreatedByOrderByCreatedDateDesc(String createdBy);

    /**
     * Get dependency statistics for performance monitoring
     * Returns aggregated data about dependency distribution
     * 
     * @return dependency statistics
     */
    @Query("""
        SELECT 
            td.dependencyType as dependencyType,
            COUNT(td) as count,
            AVG(td.priorityWeight) as avgPriority,
            MAX(td.maxWaitTimeSeconds) as maxWaitTime
        FROM TransactionDependencyEntity td 
        WHERE td.activeFlag = 'Y'
        GROUP BY td.dependencyType
        ORDER BY COUNT(td) DESC
        """)
    List<Object[]> getDependencyStatistics();

    /**
     * Find orphaned dependencies (referencing non-existent transactions)
     * Used for data integrity validation
     * 
     * @param existingTransactionIds set of valid transaction IDs
     * @return list of orphaned dependencies
     */
    @Query("""
        SELECT td FROM TransactionDependencyEntity td 
        WHERE td.activeFlag = 'Y' 
        AND (td.sourceTransactionId NOT IN :existingTransactionIds
             OR td.targetTransactionId NOT IN :existingTransactionIds)
        """)
    List<TransactionDependencyEntity> findOrphanedDependencies(
            @Param("existingTransactionIds") Set<String> existingTransactionIds);

    /**
     * Find critical path dependencies (highest priority chain)
     * Used for critical path analysis in project management
     * 
     * @param minPriority minimum priority threshold
     * @return list of critical path dependencies
     */
    @Query("""
        SELECT td FROM TransactionDependencyEntity td 
        WHERE td.activeFlag = 'Y' 
        AND td.priorityWeight = (
            SELECT MAX(td2.priorityWeight) 
            FROM TransactionDependencyEntity td2 
            WHERE td2.activeFlag = 'Y'
            AND td2.priorityWeight >= :minPriority
        )
        ORDER BY td.createdDate ASC
        """)
    List<TransactionDependencyEntity> findCriticalPathDependencies(
            @Param("minPriority") Integer minPriority);

    /**
     * Bulk update active flag for multiple dependencies
     * Used for batch activation/deactivation
     * 
     * @param dependencyIds list of dependency IDs to update
     * @param activeFlag new active flag value
     * @param modifiedBy user making the change
     * @return number of updated records
     */
    @Query("""
        UPDATE TransactionDependencyEntity td 
        SET td.activeFlag = :activeFlag, 
            td.lastModifiedBy = :modifiedBy, 
            td.lastModifiedDate = CURRENT_TIMESTAMP
        WHERE td.dependencyId IN :dependencyIds
        """)
    int bulkUpdateActiveFlag(@Param("dependencyIds") List<Long> dependencyIds,
                            @Param("activeFlag") String activeFlag,
                            @Param("modifiedBy") String modifiedBy);
}