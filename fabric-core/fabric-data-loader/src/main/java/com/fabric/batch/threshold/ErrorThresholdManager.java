package com.fabric.batch.threshold;

import com.fabric.batch.entity.DataLoadConfigEntity;
import com.fabric.batch.validation.ValidationSummary;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages configurable error thresholds for data loading operations.
 * Implements user requirement: "Continue with configurable error threshold"
 */
@Slf4j
@Component
public class ErrorThresholdManager {
    
    // Threshold tracking per configuration
    private final Map<String, ThresholdTracker> configThresholds = new HashMap<>();
    
    /**
     * Check if error threshold is exceeded for a configuration.
     */
    public ThresholdCheckResult checkThreshold(String configId, DataLoadConfigEntity config, 
                                             ValidationSummary validationSummary) {
        ThresholdTracker tracker = getOrCreateTracker(configId, config);
        
        // Update tracker with current validation results
        tracker.addValidationResults(validationSummary);
        
        ThresholdCheckResult result = new ThresholdCheckResult();
        result.setConfigId(configId);
        result.setCurrentErrors(tracker.getCurrentErrors());
        result.setCurrentWarnings(tracker.getCurrentWarnings());
        result.setMaxAllowedErrors(tracker.getMaxErrors());
        result.setWarningThreshold(tracker.getWarningThreshold());
        result.setCheckTimestamp(LocalDateTime.now());
        
        // Check error threshold
        if (tracker.getMaxErrors() > 0 && tracker.getCurrentErrors() >= tracker.getMaxErrors()) {
            result.setThresholdExceeded(true);
            result.setThresholdType(ThresholdType.ERROR);
            result.setAction(ThresholdAction.STOP_PROCESSING);
            result.setMessage(String.format("Error threshold exceeded: %d errors >= %d maximum allowed", 
                tracker.getCurrentErrors(), tracker.getMaxErrors()));
            
            log.error("Error threshold exceeded for config {}: {} errors >= {} threshold", 
                configId, tracker.getCurrentErrors(), tracker.getMaxErrors());
        }
        // Check warning threshold
        else if (tracker.getWarningThreshold() > 0 && tracker.getCurrentWarnings() >= tracker.getWarningThreshold()) {
            result.setThresholdExceeded(true);
            result.setThresholdType(ThresholdType.WARNING);
            result.setAction(ThresholdAction.CONTINUE_WITH_ALERT);
            result.setMessage(String.format("Warning threshold exceeded: %d warnings >= %d threshold", 
                tracker.getCurrentWarnings(), tracker.getWarningThreshold()));
            
            log.warn("Warning threshold exceeded for config {}: {} warnings >= {} threshold", 
                configId, tracker.getCurrentWarnings(), tracker.getWarningThreshold());
        }
        else {
            result.setThresholdExceeded(false);
            result.setAction(ThresholdAction.CONTINUE);
            result.setMessage("Within acceptable thresholds");
        }
        
        return result;
    }
    
    /**
     * Check threshold for individual record validation.
     */
    public boolean shouldContinueProcessing(String configId, int currentErrors, int maxAllowedErrors) {
        if (maxAllowedErrors <= 0) {
            return true; // No threshold configured - continue processing
        }
        
        boolean shouldContinue = currentErrors < maxAllowedErrors;
        
        if (!shouldContinue) {
            log.error("Stopping processing for config {} - error threshold exceeded: {} >= {}", 
                configId, currentErrors, maxAllowedErrors);
        }
        
        return shouldContinue;
    }
    
    /**
     * Get threshold statistics for a configuration.
     */
    public ThresholdStatistics getThresholdStatistics(String configId) {
        ThresholdTracker tracker = configThresholds.get(configId);
        if (tracker == null) {
            return new ThresholdStatistics(configId);
        }
        
        ThresholdStatistics stats = new ThresholdStatistics(configId);
        stats.setMaxErrors(tracker.getMaxErrors());
        stats.setWarningThreshold(tracker.getWarningThreshold());
        stats.setCurrentErrors(tracker.getCurrentErrors());
        stats.setCurrentWarnings(tracker.getCurrentWarnings());
        stats.setTotalRecordsProcessed(tracker.getTotalRecords());
        stats.setLastCheckTime(tracker.getLastUpdateTime());
        stats.setErrorRate(tracker.getErrorRate());
        stats.setWarningRate(tracker.getWarningRate());
        
        return stats;
    }
    
    /**
     * Reset threshold counters for a configuration.
     */
    public void resetThresholds(String configId) {
        ThresholdTracker tracker = configThresholds.get(configId);
        if (tracker != null) {
            tracker.reset();
            log.info("Reset threshold counters for config: {}", configId);
        }
    }
    
    /**
     * Configure thresholds for a configuration.
     */
    public void configureThresholds(String configId, int maxErrors, int warningThreshold) {
        ThresholdTracker tracker = configThresholds.computeIfAbsent(configId, k -> new ThresholdTracker());
        tracker.setMaxErrors(maxErrors);
        tracker.setWarningThreshold(warningThreshold);
        tracker.setLastUpdateTime(LocalDateTime.now());
        
        log.info("Configured thresholds for config {}: maxErrors={}, warningThreshold={}", 
            configId, maxErrors, warningThreshold);
    }
    
    /**
     * Get or create threshold tracker for configuration.
     */
    private ThresholdTracker getOrCreateTracker(String configId, DataLoadConfigEntity config) {
        return configThresholds.computeIfAbsent(configId, k -> {
            ThresholdTracker tracker = new ThresholdTracker();
            
            // Set thresholds from configuration
            if (config.getMaxErrors() != null) {
                tracker.setMaxErrors(config.getMaxErrors());
            }
            
            // Set warning threshold based on percentage of max errors or from validation rules
            int warningThreshold = calculateWarningThreshold(config);
            tracker.setWarningThreshold(warningThreshold);
            
            tracker.setLastUpdateTime(LocalDateTime.now());
            
            log.debug("Created threshold tracker for config {}: maxErrors={}, warningThreshold={}", 
                configId, tracker.getMaxErrors(), tracker.getWarningThreshold());
            
            return tracker;
        });
    }
    
    /**
     * Calculate warning threshold based on configuration.
     */
    private int calculateWarningThreshold(DataLoadConfigEntity config) {
        Integer maxErrors = config.getMaxErrors();
        if (maxErrors == null || maxErrors <= 0) {
            return 0; // No warning threshold if no error threshold
        }
        
        // Default warning threshold to 75% of error threshold
        return (int) Math.ceil(maxErrors * 0.75);
    }
    
    /**
     * Get all threshold statistics.
     */
    public Map<String, ThresholdStatistics> getAllThresholdStatistics() {
        Map<String, ThresholdStatistics> allStats = new HashMap<>();
        
        for (String configId : configThresholds.keySet()) {
            allStats.put(configId, getThresholdStatistics(configId));
        }
        
        return allStats;
    }
    
    /**
     * Clean up old threshold trackers.
     */
    public void cleanupOldTrackers(int retentionHours) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(retentionHours);
        
        configThresholds.entrySet().removeIf(entry -> {
            boolean shouldRemove = entry.getValue().getLastUpdateTime().isBefore(cutoffTime);
            if (shouldRemove) {
                log.debug("Removed old threshold tracker for config: {}", entry.getKey());
            }
            return shouldRemove;
        });
    }
    
    /**
     * Internal threshold tracker class.
     */
    @Data
    private static class ThresholdTracker {
        private int maxErrors = 1000; // Default from common configurations
        private int warningThreshold = 750; // Default 75% of error threshold
        private final AtomicInteger currentErrors = new AtomicInteger(0);
        private final AtomicInteger currentWarnings = new AtomicInteger(0);
        private final AtomicLong totalRecords = new AtomicLong(0);
        private LocalDateTime lastUpdateTime = LocalDateTime.now();
        
        public void addValidationResults(ValidationSummary summary) {
            currentErrors.addAndGet(summary.getTotalErrors());
            currentWarnings.addAndGet(summary.getTotalWarnings());
            totalRecords.addAndGet(summary.getTotalFields());
            lastUpdateTime = LocalDateTime.now();
        }
        
        public void reset() {
            currentErrors.set(0);
            currentWarnings.set(0);
            totalRecords.set(0);
            lastUpdateTime = LocalDateTime.now();
        }
        
        public int getCurrentErrors() {
            return currentErrors.get();
        }
        
        public int getCurrentWarnings() {
            return currentWarnings.get();
        }
        
        public long getTotalRecords() {
            return totalRecords.get();
        }
        
        public double getErrorRate() {
            long total = getTotalRecords();
            return total > 0 ? (double) getCurrentErrors() / total * 100.0 : 0.0;
        }
        
        public double getWarningRate() {
            long total = getTotalRecords();
            return total > 0 ? (double) getCurrentWarnings() / total * 100.0 : 0.0;
        }
    }
    
    /**
     * Threshold check result.
     */
    @Data
    public static class ThresholdCheckResult {
        private String configId;
        private boolean thresholdExceeded;
        private ThresholdType thresholdType;
        private ThresholdAction action;
        private String message;
        private int currentErrors;
        private int currentWarnings;
        private int maxAllowedErrors;
        private int warningThreshold;
        private LocalDateTime checkTimestamp;
    }
    
    /**
     * Threshold statistics.
     */
    @Data
    public static class ThresholdStatistics {
        private final String configId;
        private int maxErrors;
        private int warningThreshold;
        private int currentErrors;
        private int currentWarnings;
        private long totalRecordsProcessed;
        private LocalDateTime lastCheckTime;
        private double errorRate;
        private double warningRate;
        
        public ThresholdStatistics(String configId) {
            this.configId = configId;
        }
        
        public boolean isHealthy() {
            return errorRate <= 5.0 && warningRate <= 15.0; // Configurable thresholds
        }
        
        public String getHealthStatus() {
            if (errorRate > 10.0) return "CRITICAL";
            if (errorRate > 5.0 || warningRate > 20.0) return "WARNING";
            return "HEALTHY";
        }
    }
    
    /**
     * Threshold types.
     */
    public enum ThresholdType {
        ERROR,
        WARNING,
        RATE_LIMIT
    }
    
    /**
     * Threshold actions.
     */
    public enum ThresholdAction {
        CONTINUE,
        CONTINUE_WITH_ALERT,
        STOP_PROCESSING,
        RETRY_AFTER_DELAY
    }
}