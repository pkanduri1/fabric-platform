package com.fabric.batch.idempotency.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Model representing the result of an idempotency operation.
 * Contains the operation result, status, and comprehensive metadata
 * for tracking and monitoring idempotent operations.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyResult<T> {
    
    /**
     * The actual result data from the operation
     */
    private T data;
    
    /**
     * Status of the idempotency operation
     */
    private IdempotencyStatus status;
    
    /**
     * Unique idempotency key for this operation
     */
    private String idempotencyKey;
    
    /**
     * Correlation ID for distributed tracing
     */
    private String correlationId;
    
    /**
     * Error message if operation failed
     */
    private String errorMessage;
    
    /**
     * Detailed error information
     */
    private String errorDetails;
    
    /**
     * Timestamp when result was created
     */
    private LocalDateTime timestamp;
    
    /**
     * Processing duration in milliseconds
     */
    private Long processingDurationMs;
    
    /**
     * Number of retry attempts made
     */
    private Integer retryCount;
    
    /**
     * Maximum retries allowed
     */
    private Integer maxRetries;
    
    /**
     * Whether this result came from cache
     */
    private boolean fromCache;
    
    /**
     * Cache TTL remaining in seconds
     */
    private Long cacheTtlRemainingSeconds;
    
    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;
    
    /**
     * Performance metrics
     */
    private PerformanceMetrics performanceMetrics;
    
    /**
     * Warning messages (non-fatal issues)
     */
    private String[] warnings;
    
    // Utility methods
    
    /**
     * Checks if the operation was successful
     */
    public boolean isSuccess() {
        return status == IdempotencyStatus.SUCCESS || status == IdempotencyStatus.CACHED_RESULT;
    }
    
    /**
     * Checks if the operation failed
     */
    public boolean isFailure() {
        return status == IdempotencyStatus.FAILED || status == IdempotencyStatus.MAX_RETRIES_EXCEEDED;
    }
    
    /**
     * Checks if the operation is still in progress
     */
    public boolean isInProgress() {
        return status == IdempotencyStatus.IN_PROGRESS;
    }
    
    /**
     * Checks if the operation can be retried
     */
    public boolean canRetry() {
        return status == IdempotencyStatus.FAILED && 
               (retryCount == null || maxRetries == null || retryCount < maxRetries);
    }
    
    /**
     * Checks if there are warnings
     */
    public boolean hasWarnings() {
        return warnings != null && warnings.length > 0;
    }
    
    /**
     * Checks if there are performance metrics
     */
    public boolean hasPerformanceMetrics() {
        return performanceMetrics != null;
    }
    
    /**
     * Checks if there is metadata
     */
    public boolean hasMetadata() {
        return metadata != null && !metadata.isEmpty();
    }
    
    /**
     * Gets cache efficiency (for cached results)
     */
    public double getCacheEfficiency() {
        if (!fromCache || processingDurationMs == null) return 0.0;
        // Assume cache hit saves significant processing time
        return processingDurationMs < 100 ? 0.95 : 0.85;
    }
    
    /**
     * Creates a summary string for logging
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("IdempotencyResult[status=").append(status);
        sb.append(", key=").append(idempotencyKey);
        sb.append(", correlation=").append(correlationId);
        if (fromCache) sb.append(", cached");
        if (processingDurationMs != null) sb.append(", duration=").append(processingDurationMs).append("ms");
        if (retryCount != null && retryCount > 0) sb.append(", retries=").append(retryCount);
        sb.append("]");
        return sb.toString();
    }
    
    // Static factory methods
    
    /**
     * Creates a successful result
     */
    public static <T> IdempotencyResult<T> success(T data, String idempotencyKey, String correlationId) {
        return IdempotencyResult.<T>builder()
                .data(data)
                .status(IdempotencyStatus.SUCCESS)
                .idempotencyKey(idempotencyKey)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .fromCache(false)
                .build();
    }
    
    /**
     * Creates a cached result
     */
    public static <T> IdempotencyResult<T> fromCached(T data, String idempotencyKey, String correlationId) {
        return IdempotencyResult.<T>builder()
                .data(data)
                .status(IdempotencyStatus.CACHED_RESULT)
                .idempotencyKey(idempotencyKey)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .fromCache(true)
                .build();
    }
    
    /**
     * Creates an in-progress result
     */
    public static <T> IdempotencyResult<T> inProgress(String idempotencyKey, String correlationId) {
        return IdempotencyResult.<T>builder()
                .status(IdempotencyStatus.IN_PROGRESS)
                .idempotencyKey(idempotencyKey)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .fromCache(false)
                .build();
    }
    
    /**
     * Creates a failed result
     */
    public static <T> IdempotencyResult<T> failed(String errorMessage, String idempotencyKey, 
                                                  String correlationId, Integer retryCount) {
        return IdempotencyResult.<T>builder()
                .status(IdempotencyStatus.FAILED)
                .errorMessage(errorMessage)
                .idempotencyKey(idempotencyKey)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .retryCount(retryCount)
                .fromCache(false)
                .build();
    }
    
    /**
     * Creates a max retries exceeded result
     */
    public static <T> IdempotencyResult<T> maxRetriesExceeded(String errorDetails, 
                                                              String idempotencyKey, String correlationId) {
        return IdempotencyResult.<T>builder()
                .status(IdempotencyStatus.MAX_RETRIES_EXCEEDED)
                .errorMessage("Maximum retry attempts exceeded")
                .errorDetails(errorDetails)
                .idempotencyKey(idempotencyKey)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .fromCache(false)
                .build();
    }
    
    /**
     * Creates an expired result
     */
    public static <T> IdempotencyResult<T> expired(String idempotencyKey, String correlationId) {
        return IdempotencyResult.<T>builder()
                .status(IdempotencyStatus.EXPIRED)
                .errorMessage("Idempotency key has expired")
                .idempotencyKey(idempotencyKey)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .fromCache(false)
                .build();
    }
    
    /**
     * Performance metrics nested class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private Long executionTimeMs;
        private Double memoryUsageMb;
        private Double cpuUsagePercent;
        private Long networkLatencyMs;
        private Long databaseQueryTimeMs;
        private Integer databaseQueryCount;
        private Long cacheHitTimeMs;
        private Double cacheMissRatio;
        private String performanceTier;
        
        public boolean isHighPerformance() {
            return executionTimeMs != null && executionTimeMs < 1000; // Less than 1 second
        }
        
        public boolean isLowMemoryUsage() {
            return memoryUsageMb != null && memoryUsageMb < 100; // Less than 100MB
        }
        
        public double getOverallEfficiency() {
            double score = 1.0;
            if (executionTimeMs != null) {
                score *= Math.max(0.1, 1.0 - (executionTimeMs / 10000.0)); // Penalize long execution
            }
            if (memoryUsageMb != null) {
                score *= Math.max(0.1, 1.0 - (memoryUsageMb / 1000.0)); // Penalize high memory
            }
            if (cacheMissRatio != null) {
                score *= Math.max(0.1, 1.0 - cacheMissRatio); // Penalize cache misses
            }
            return score;
        }
    }
}