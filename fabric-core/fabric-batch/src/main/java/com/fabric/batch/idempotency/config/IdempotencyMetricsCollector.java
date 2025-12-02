package com.fabric.batch.idempotency.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Metrics collector for the Fabric Platform Idempotency Framework.
 * Provides comprehensive performance and operational metrics collection
 * for monitoring, alerting, and operational insights.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Component
@Slf4j
public class IdempotencyMetricsCollector {
    
    private final IdempotencyProperties properties;
    
    // Counters for different operation types
    private final LongAdder successfulOperations = new LongAdder();
    private final LongAdder cachedResults = new LongAdder();
    private final LongAdder failedOperations = new LongAdder();
    private final LongAdder duplicateOperations = new LongAdder();
    private final LongAdder inProgressOperations = new LongAdder();
    private final LongAdder maxRetriesExceeded = new LongAdder();
    
    // Performance metrics
    private final AtomicLong totalProcessingTime = new AtomicLong();
    private final AtomicLong maxProcessingTime = new AtomicLong();
    private final AtomicLong minProcessingTime = new AtomicLong(Long.MAX_VALUE);
    
    // System metrics
    private final ConcurrentHashMap<String, LongAdder> sourceSystemMetrics = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> jobTypeMetrics = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> errorTypeMetrics = new ConcurrentHashMap<>();
    
    public IdempotencyMetricsCollector(IdempotencyProperties properties) {
        this.properties = properties;
        log.info("Initialized idempotency metrics collector with properties: {}", properties.getConfigurationSummary());
    }
    
    /**
     * Records a successful idempotency operation.
     */
    public void recordSuccess(String sourceSystem, String jobName, long processingTimeMs) {
        if (!properties.shouldCollectMetrics()) return;
        
        successfulOperations.increment();
        updateProcessingTime(processingTimeMs);
        updateSourceSystemMetric(sourceSystem, "success");
        updateJobTypeMetric(jobName, "success");
        
        log.debug("Recorded successful operation: {}/{} ({}ms)", sourceSystem, jobName, processingTimeMs);
    }
    
    /**
     * Records a cached result return.
     */
    public void recordCachedResult(String sourceSystem, String jobName, long lookupTimeMs) {
        if (!properties.shouldCollectMetrics()) return;
        
        cachedResults.increment();
        updateProcessingTime(lookupTimeMs);
        updateSourceSystemMetric(sourceSystem, "cached");
        updateJobTypeMetric(jobName, "cached");
        
        log.debug("Recorded cached result: {}/{} ({}ms)", sourceSystem, jobName, lookupTimeMs);
    }
    
    /**
     * Records a failed idempotency operation.
     */
    public void recordFailure(String sourceSystem, String jobName, String errorType, long processingTimeMs) {
        if (!properties.shouldCollectMetrics()) return;
        
        failedOperations.increment();
        updateProcessingTime(processingTimeMs);
        updateSourceSystemMetric(sourceSystem, "failed");
        updateJobTypeMetric(jobName, "failed");
        updateErrorTypeMetric(errorType);
        
        log.debug("Recorded failed operation: {}/{} - {} ({}ms)", sourceSystem, jobName, errorType, processingTimeMs);
    }
    
    /**
     * Records a duplicate operation detection.
     */
    public void recordDuplicate(String sourceSystem, String jobName) {
        if (!properties.shouldCollectMetrics()) return;
        
        duplicateOperations.increment();
        updateSourceSystemMetric(sourceSystem, "duplicate");
        updateJobTypeMetric(jobName, "duplicate");
        
        log.debug("Recorded duplicate operation: {}/{}", sourceSystem, jobName);
    }
    
    /**
     * Records an in-progress operation detection.
     */
    public void recordInProgress(String sourceSystem, String jobName) {
        if (!properties.shouldCollectMetrics()) return;
        
        inProgressOperations.increment();
        updateSourceSystemMetric(sourceSystem, "in_progress");
        updateJobTypeMetric(jobName, "in_progress");
        
        log.debug("Recorded in-progress operation: {}/{}", sourceSystem, jobName);
    }
    
    /**
     * Records a max retries exceeded event.
     */
    public void recordMaxRetriesExceeded(String sourceSystem, String jobName) {
        if (!properties.shouldCollectMetrics()) return;
        
        maxRetriesExceeded.increment();
        updateSourceSystemMetric(sourceSystem, "max_retries");
        updateJobTypeMetric(jobName, "max_retries");
        
        log.debug("Recorded max retries exceeded: {}/{}", sourceSystem, jobName);
    }
    
    /**
     * Gets current metrics snapshot.
     */
    public MetricsSnapshot getMetricsSnapshot() {
        long totalOps = getTotalOperations();
        long avgProcessingTime = totalOps > 0 ? 
                totalProcessingTime.get() / totalOps : 0;
        
        return MetricsSnapshot.builder()
                .successfulOperations(successfulOperations.sum())
                .cachedResults(cachedResults.sum())
                .failedOperations(failedOperations.sum())
                .duplicateOperations(duplicateOperations.sum())
                .inProgressOperations(inProgressOperations.sum())
                .maxRetriesExceeded(maxRetriesExceeded.sum())
                .totalOperations(totalOps)
                .averageProcessingTimeMs(avgProcessingTime)
                .maxProcessingTimeMs(maxProcessingTime.get())
                .minProcessingTimeMs(minProcessingTime.get() == Long.MAX_VALUE ? 0 : minProcessingTime.get())
                .cacheHitRatio(calculateCacheHitRatio())
                .successRate(calculateSuccessRate())
                .sourceSystemMetrics(new ConcurrentHashMap<>(sourceSystemMetrics))
                .jobTypeMetrics(new ConcurrentHashMap<>(jobTypeMetrics))
                .errorTypeMetrics(new ConcurrentHashMap<>(errorTypeMetrics))
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * Resets all metrics (useful for testing).
     */
    public void reset() {
        successfulOperations.reset();
        cachedResults.reset();
        failedOperations.reset();
        duplicateOperations.reset();
        inProgressOperations.reset();
        maxRetriesExceeded.reset();
        
        totalProcessingTime.set(0);
        maxProcessingTime.set(0);
        minProcessingTime.set(Long.MAX_VALUE);
        
        sourceSystemMetrics.clear();
        jobTypeMetrics.clear();
        errorTypeMetrics.clear();
        
        log.info("Reset all idempotency metrics");
    }
    
    // Private helper methods
    
    private void updateProcessingTime(long processingTimeMs) {
        totalProcessingTime.addAndGet(processingTimeMs);
        
        // Update max
        long currentMax = maxProcessingTime.get();
        while (processingTimeMs > currentMax) {
            if (maxProcessingTime.compareAndSet(currentMax, processingTimeMs)) {
                break;
            }
            currentMax = maxProcessingTime.get();
        }
        
        // Update min
        long currentMin = minProcessingTime.get();
        while (processingTimeMs < currentMin) {
            if (minProcessingTime.compareAndSet(currentMin, processingTimeMs)) {
                break;
            }
            currentMin = minProcessingTime.get();
        }
    }
    
    private void updateSourceSystemMetric(String sourceSystem, String operation) {
        String key = sourceSystem + "." + operation;
        sourceSystemMetrics.computeIfAbsent(key, k -> new LongAdder()).increment();
    }
    
    private void updateJobTypeMetric(String jobName, String operation) {
        String key = jobName + "." + operation;
        jobTypeMetrics.computeIfAbsent(key, k -> new LongAdder()).increment();
    }
    
    private void updateErrorTypeMetric(String errorType) {
        errorTypeMetrics.computeIfAbsent(errorType, k -> new LongAdder()).increment();
    }
    
    private long getTotalOperations() {
        return successfulOperations.sum() + cachedResults.sum() + failedOperations.sum() +
               duplicateOperations.sum() + inProgressOperations.sum() + maxRetriesExceeded.sum();
    }
    
    private double calculateCacheHitRatio() {
        long total = successfulOperations.sum() + cachedResults.sum();
        return total > 0 ? (double) cachedResults.sum() / total : 0.0;
    }
    
    private double calculateSuccessRate() {
        long total = getTotalOperations();
        long successful = successfulOperations.sum() + cachedResults.sum();
        return total > 0 ? (double) successful / total : 0.0;
    }
    
    /**
     * Metrics snapshot data class.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MetricsSnapshot {
        private long successfulOperations;
        private long cachedResults;
        private long failedOperations;
        private long duplicateOperations;
        private long inProgressOperations;
        private long maxRetriesExceeded;
        private long totalOperations;
        private long averageProcessingTimeMs;
        private long maxProcessingTimeMs;
        private long minProcessingTimeMs;
        private double cacheHitRatio;
        private double successRate;
        private ConcurrentHashMap<String, LongAdder> sourceSystemMetrics;
        private ConcurrentHashMap<String, LongAdder> jobTypeMetrics;
        private ConcurrentHashMap<String, LongAdder> errorTypeMetrics;
        private long timestamp;
        
        public String getSummary() {
            return String.format(
                "MetricsSnapshot[total=%d, success=%d, cached=%d, failed=%d, " +
                "duplicates=%d, successRate=%.2f%%, cacheHitRatio=%.2f%%, avgTime=%dms]",
                totalOperations, successfulOperations, cachedResults, failedOperations,
                duplicateOperations, successRate * 100, cacheHitRatio * 100, averageProcessingTimeMs
            );
        }
    }
}