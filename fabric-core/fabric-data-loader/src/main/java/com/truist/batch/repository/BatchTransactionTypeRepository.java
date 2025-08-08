package com.truist.batch.repository;

import com.truist.batch.entity.BatchTransactionTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Epic 2: Repository interface for BatchTransactionTypeEntity providing
 * banking-grade data access patterns with proper query optimization and caching.
 * 
 * This repository supports the parallel transaction processing architecture
 * with specialized queries for performance monitoring, compliance tracking,
 * and configuration management.
 * 
 * @author Senior Full Stack Developer Agent  
 * @version 1.0
 * @since Epic 2 - Simple Transaction Processing
 */
@Repository
public interface BatchTransactionTypeRepository extends JpaRepository<BatchTransactionTypeEntity, Long> {

    /**
     * Find all active transaction types for a specific job configuration
     * ordered by processing order for deterministic execution sequence.
     */
    @Query("SELECT btt FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.jobConfigId = :jobConfigId " +
           "AND btt.activeFlag = :activeFlag " +
           "ORDER BY btt.processingOrder ASC")
    List<BatchTransactionTypeEntity> findByJobConfigIdAndActiveFlag(
            @Param("jobConfigId") Long jobConfigId, 
            @Param("activeFlag") String activeFlag);

    /**
     * Find transaction type by job configuration and transaction type name
     */
    @Query("SELECT btt FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.jobConfigId = :jobConfigId " +
           "AND btt.transactionType = :transactionType " +
           "AND btt.activeFlag = 'Y'")
    Optional<BatchTransactionTypeEntity> findByJobConfigIdAndTransactionType(
            @Param("jobConfigId") Long jobConfigId,
            @Param("transactionType") String transactionType);

    /**
     * Find all transaction types requiring high compliance levels
     */
    @Query("SELECT btt FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.complianceLevel IN ('HIGH', 'CRITICAL') " +
           "AND btt.activeFlag = 'Y' " +
           "ORDER BY btt.complianceLevel DESC, btt.processingOrder ASC")
    List<BatchTransactionTypeEntity> findHighComplianceTransactionTypes();

    /**
     * Find transaction types with parallel processing configuration
     */
    @Query("SELECT btt FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.parallelThreads > 1 " +
           "AND btt.activeFlag = 'Y' " +
           "ORDER BY btt.parallelThreads DESC")
    List<BatchTransactionTypeEntity> findParallelProcessingTransactionTypes();

    /**
     * Find transaction types requiring encryption
     */
    @Query("SELECT btt FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.encryptionFields IS NOT NULL " +
           "AND LENGTH(btt.encryptionFields) > 0 " +
           "AND btt.activeFlag = 'Y'")
    List<BatchTransactionTypeEntity> findEncryptionRequiredTransactionTypes();

    /**
     * Get total thread allocation for a job configuration
     */
    @Query("SELECT COALESCE(SUM(btt.parallelThreads), 0) FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.jobConfigId = :jobConfigId " +
           "AND btt.activeFlag = 'Y'")
    Integer getTotalThreadAllocationForJob(@Param("jobConfigId") Long jobConfigId);

    /**
     * Get transaction types by compliance level
     */
    @Query("SELECT btt FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.complianceLevel = :complianceLevel " +
           "AND btt.activeFlag = 'Y' " +
           "ORDER BY btt.processingOrder ASC")
    List<BatchTransactionTypeEntity> findByComplianceLevel(
            @Param("complianceLevel") String complianceLevel);

    /**
     * Find transaction types with timeout configuration above threshold
     */
    @Query("SELECT btt FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.timeoutSeconds > :timeoutThreshold " +
           "AND btt.activeFlag = 'Y' " +
           "ORDER BY btt.timeoutSeconds DESC")
    List<BatchTransactionTypeEntity> findWithTimeoutAbove(
            @Param("timeoutThreshold") Integer timeoutThreshold);

    /**
     * Find transaction types with chunk size in specific range
     */
    @Query("SELECT btt FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.chunkSize BETWEEN :minChunkSize AND :maxChunkSize " +
           "AND btt.activeFlag = 'Y' " +
           "ORDER BY btt.chunkSize ASC")
    List<BatchTransactionTypeEntity> findByChunkSizeRange(
            @Param("minChunkSize") Integer minChunkSize,
            @Param("maxChunkSize") Integer maxChunkSize);

    /**
     * Count active transaction types for a job
     */
    @Query("SELECT COUNT(btt) FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.jobConfigId = :jobConfigId " +
           "AND btt.activeFlag = 'Y'")
    Long countActiveTransactionTypesForJob(@Param("jobConfigId") Long jobConfigId);

    /**
     * Find transaction types modified after specific date
     */
    @Query("SELECT btt FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.lastModifiedDate > :sinceDate " +
           "AND btt.activeFlag = 'Y' " +
           "ORDER BY btt.lastModifiedDate DESC")
    List<BatchTransactionTypeEntity> findModifiedSince(
            @Param("sinceDate") java.time.Instant sinceDate);

    /**
     * Find transaction types created by specific user
     */
    @Query("SELECT btt FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.createdBy = :createdBy " +
           "ORDER BY btt.createdDate DESC")
    List<BatchTransactionTypeEntity> findByCreatedBy(@Param("createdBy") String createdBy);

    /**
     * Get configuration summary for monitoring dashboard
     */
    @Query("SELECT new map(" +
           "btt.transactionType as transactionType, " +
           "btt.parallelThreads as threads, " +
           "btt.chunkSize as chunkSize, " +
           "btt.complianceLevel as compliance, " +
           "btt.activeFlag as active) " +
           "FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.jobConfigId = :jobConfigId " +
           "ORDER BY btt.processingOrder ASC")
    List<java.util.Map<String, Object>> getConfigurationSummary(
            @Param("jobConfigId") Long jobConfigId);

    /**
     * Find transaction types with retry policy configured
     */
    @Query("SELECT btt FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.retryPolicy IS NOT NULL " +
           "AND LENGTH(btt.retryPolicy) > 0 " +
           "AND btt.activeFlag = 'Y'")
    List<BatchTransactionTypeEntity> findWithRetryPolicy();

    /**
     * Performance optimization query for batch processing statistics
     */
    @Query("SELECT new map(" +
           "COUNT(btt) as totalTypes, " +
           "SUM(btt.parallelThreads) as totalThreads, " +
           "AVG(btt.chunkSize) as avgChunkSize, " +
           "MAX(btt.timeoutSeconds) as maxTimeout) " +
           "FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.activeFlag = 'Y'")
    java.util.Map<String, Object> getPerformanceStatistics();

    /**
     * Compliance audit query for reporting
     */
    @Query("SELECT new map(" +
           "btt.complianceLevel as level, " +
           "COUNT(btt) as count, " +
           "SUM(CASE WHEN btt.encryptionFields IS NOT NULL THEN 1 ELSE 0 END) as encryptionRequired) " +
           "FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.activeFlag = 'Y' " +
           "GROUP BY btt.complianceLevel " +
           "ORDER BY btt.complianceLevel")
    List<java.util.Map<String, Object>> getComplianceStatistics();

    /**
     * Find duplicate transaction types (for data quality checks)
     */
    @Query("SELECT btt1 FROM BatchTransactionTypeEntity btt1 " +
           "WHERE EXISTS (" +
           "  SELECT btt2 FROM BatchTransactionTypeEntity btt2 " +
           "  WHERE btt2.jobConfigId = btt1.jobConfigId " +
           "  AND btt2.transactionType = btt1.transactionType " +
           "  AND btt2.transactionTypeId != btt1.transactionTypeId " +
           "  AND btt2.activeFlag = 'Y'" +
           ") " +
           "AND btt1.activeFlag = 'Y'")
    List<BatchTransactionTypeEntity> findDuplicateTransactionTypes();

    /**
     * Custom query for transaction type optimization recommendations
     */
    @Query("SELECT btt FROM BatchTransactionTypeEntity btt " +
           "WHERE (" +
           "  (btt.parallelThreads = 1 AND btt.chunkSize > 10000) OR " +
           "  (btt.parallelThreads > 10 AND btt.chunkSize < 500) OR " +
           "  (btt.timeoutSeconds > 1800)" +
           ") " +
           "AND btt.activeFlag = 'Y'")
    List<BatchTransactionTypeEntity> findOptimizationCandidates();

    /**
     * Find transaction types by transaction type name and active flag
     */
    @Query("SELECT btt FROM BatchTransactionTypeEntity btt " +
           "WHERE btt.transactionType = :transactionType " +
           "AND btt.activeFlag = :activeFlag " +
           "ORDER BY btt.processingOrder ASC")
    List<BatchTransactionTypeEntity> findByTransactionTypeAndActiveFlag(
            @Param("transactionType") String transactionType,
            @Param("activeFlag") String activeFlag);
}