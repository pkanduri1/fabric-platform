package com.fabric.batch.idempotency.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Configuration properties for the Fabric Platform Idempotency Framework.
 * Provides comprehensive, externalized configuration for all aspects of
 * idempotency behavior, performance tuning, and operational settings.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Data
@Validated
@ConfigurationProperties(prefix = "fabric.idempotency")
public class IdempotencyProperties {
    
    /**
     * Whether idempotency framework is enabled globally.
     */
    private boolean enabled = true;
    
    /**
     * Default TTL for idempotency keys in hours.
     */
    @Min(1)
    private int defaultTtlHours = 24;
    
    /**
     * Default maximum retries for failed operations.
     */
    @Min(0)
    private int defaultMaxRetries = 3;
    
    /**
     * Default key generation strategy.
     */
    @NotNull
    private String defaultKeyStrategy = "AUTO_GENERATED";
    
    /**
     * Whether to store request payloads by default.
     */
    private boolean storeRequestPayloadDefault = true;
    
    /**
     * Whether to store response payloads by default.
     */
    private boolean storeResponsePayloadDefault = true;
    
    /**
     * Database configuration settings.
     */
    @Valid
    private Database database = new Database();
    
    /**
     * Cache configuration settings.
     */
    @Valid
    private Cache cache = new Cache();
    
    /**
     * Cleanup configuration settings.
     */
    @Valid
    private Cleanup cleanup = new Cleanup();
    
    /**
     * Performance configuration settings.
     */
    @Valid
    private Performance performance = new Performance();
    
    /**
     * Security configuration settings.
     */
    @Valid
    private Security security = new Security();
    
    /**
     * Monitoring and events configuration.
     */
    @Valid
    private Events events = new Events();
    
    /**
     * Development and debugging settings.
     */
    @Valid
    private Development development = new Development();
    
    /**
     * Database configuration properties.
     */
    @Data
    public static class Database {
        
        /**
         * Schema name for idempotency tables.
         */
        private String schema = "CM3INT";
        
        /**
         * Whether to enable database auditing.
         */
        private boolean auditingEnabled = true;
        
        /**
         * Batch size for bulk operations.
         */
        @Min(1)
        private int batchSize = 1000;
        
        /**
         * Connection timeout in seconds.
         */
        @Min(1)
        private int connectionTimeoutSeconds = 30;
        
        /**
         * Query timeout in seconds.
         */
        @Min(1)
        private int queryTimeoutSeconds = 60;
        
        /**
         * Whether to enable optimistic locking.
         */
        private boolean optimisticLockingEnabled = true;
        
        /**
         * Maximum retries for optimistic locking failures.
         */
        @Min(1)
        private int maxOptimisticLockRetries = 3;
    }
    
    /**
     * Cache configuration properties.
     */
    @Data
    public static class Cache {
        
        /**
         * Whether caching is enabled.
         */
        private boolean enabled = true;
        
        /**
         * Cache type (e.g., "caffeine", "redis", "hazelcast").
         */
        private String type = "caffeine";
        
        /**
         * Maximum number of entries in cache.
         */
        @Min(1)
        private long maxSize = 10000;
        
        /**
         * Cache expiration time.
         */
        private Duration expireAfterWrite = Duration.ofHours(1);
        
        /**
         * Cache expiration time for access.
         */
        private Duration expireAfterAccess = Duration.ofMinutes(30);
        
        /**
         * Whether to enable cache statistics.
         */
        private boolean statisticsEnabled = true;
        
        /**
         * Cache refresh ahead time.
         */
        private Duration refreshAhead = Duration.ofMinutes(5);
    }
    
    /**
     * Cleanup configuration properties.
     */
    @Data
    public static class Cleanup {
        
        /**
         * Whether automatic cleanup is enabled.
         */
        private boolean enabled = true;
        
        /**
         * Cleanup schedule (cron expression).
         */
        private String schedule = "0 0 */4 * * *"; // Every 4 hours
        
        /**
         * Batch size for cleanup operations.
         */
        @Min(1)
        private int batchSize = 1000;
        
        /**
         * Audit retention period in days.
         */
        @Min(1)
        private int auditRetentionDays = 90;
        
        /**
         * Whether to enable cleanup metrics.
         */
        private boolean metricsEnabled = true;
        
        /**
         * Maximum execution time for cleanup in minutes.
         */
        @Min(1)
        private int maxExecutionTimeMinutes = 60;
    }
    
    /**
     * Performance configuration properties.
     */
    @Data
    public static class Performance {
        
        /**
         * Stale request timeout in minutes.
         */
        @Min(1)
        private int staleRequestTimeoutMinutes = 30;
        
        /**
         * Processing timeout in minutes.
         */
        @Min(1)
        private int processingTimeoutMinutes = 60;
        
        /**
         * Maximum payload size in bytes.
         */
        @Min(1)
        private long maxPayloadSizeBytes = 1024 * 1024; // 1MB
        
        /**
         * Thread pool size for async operations.
         */
        @Min(1)
        private int asyncThreadPoolSize = 10;
        
        /**
         * Whether to enable performance monitoring.
         */
        private boolean monitoringEnabled = true;
        
        /**
         * Performance threshold for warnings (in milliseconds).
         */
        @Min(1)
        private long warningThresholdMs = 5000; // 5 seconds
        
        /**
         * Whether to enable compression for large payloads.
         */
        private boolean compressionEnabled = false;
    }
    
    /**
     * Security configuration properties.
     */
    @Data
    public static class Security {
        
        /**
         * Whether encryption is required for sensitive payloads.
         */
        private boolean encryptionRequired = false;
        
        /**
         * Encryption algorithm to use.
         */
        private String encryptionAlgorithm = "AES-256-GCM";
        
        /**
         * Whether to mask sensitive data in logs.
         */
        private boolean maskSensitiveData = true;
        
        /**
         * List of fields to consider as sensitive.
         */
        private String[] sensitiveFields = {"password", "ssn", "creditCard", "token"};
        
        /**
         * Whether to enable security auditing.
         */
        private boolean auditingEnabled = true;
        
        /**
         * Maximum failed attempts before lockout.
         */
        @Min(1)
        private int maxFailedAttempts = 5;
        
        /**
         * Lockout duration in minutes.
         */
        @Min(1)
        private int lockoutDurationMinutes = 15;
    }
    
    /**
     * Events and monitoring configuration properties.
     */
    @Data
    public static class Events {
        
        /**
         * Whether to publish job events.
         */
        private boolean publishJobEvents = true;
        
        /**
         * Whether to publish state change events.
         */
        private boolean publishStateChangeEvents = true;
        
        /**
         * Whether to publish performance events.
         */
        private boolean publishPerformanceEvents = false;
        
        /**
         * Event publisher type (e.g., "kafka", "rabbitmq", "sqs").
         */
        private String publisherType = "local";
        
        /**
         * Event topic or queue name.
         */
        private String topicName = "fabric.idempotency.events";
        
        /**
         * Whether to enable metrics collection.
         */
        private boolean metricsEnabled = true;
        
        /**
         * Metrics collection interval in seconds.
         */
        @Min(1)
        private int metricsIntervalSeconds = 60;
        
        /**
         * Whether to export metrics to external systems.
         */
        private boolean exportMetrics = false;
        
        /**
         * Metrics export endpoint.
         */
        private String metricsEndpoint = "/actuator/prometheus";
    }
    
    /**
     * Development and debugging configuration properties.
     */
    @Data
    public static class Development {
        
        /**
         * Whether development mode is enabled.
         */
        private boolean enabled = false;
        
        /**
         * Log level for idempotency operations.
         */
        private String logLevel = "INFO";
        
        /**
         * Whether to enable verbose logging.
         */
        private boolean verboseLogging = false;
        
        /**
         * Whether to enable debug endpoints.
         */
        private boolean debugEndpointsEnabled = false;
        
        /**
         * Whether to disable TTL for testing.
         */
        private boolean disableTtl = false;
        
        /**
         * Test mode configurations.
         */
        private boolean testMode = false;
        
        /**
         * Mock external dependencies.
         */
        private boolean mockExternalDependencies = false;
        
        /**
         * Simulate failures for testing.
         */
        private boolean simulateFailures = false;
        
        /**
         * Failure simulation rate (0.0 to 1.0).
         */
        private double failureRate = 0.0;
    }
    
    // Utility methods
    
    /**
     * Validates that configuration is consistent and valid.
     */
    public void validate() {
        if (defaultTtlHours < 1) {
            throw new IllegalArgumentException("Default TTL hours must be at least 1");
        }
        
        if (defaultMaxRetries < 0) {
            throw new IllegalArgumentException("Default max retries cannot be negative");
        }
        
        if (performance.getMaxPayloadSizeBytes() <= 0) {
            throw new IllegalArgumentException("Max payload size must be positive");
        }
        
        if (cleanup.getAuditRetentionDays() < 1) {
            throw new IllegalArgumentException("Audit retention days must be at least 1");
        }
    }
    
    /**
     * Gets default TTL in seconds.
     */
    public long getDefaultTtlSeconds() {
        return defaultTtlHours * 3600L;
    }
    
    /**
     * Checks if development mode is active.
     */
    public boolean isDevelopmentMode() {
        return development.isEnabled();
    }
    
    /**
     * Gets effective cache size based on environment.
     */
    public long getEffectiveCacheSize() {
        if (isDevelopmentMode()) {
            return Math.min(cache.getMaxSize(), 1000L); // Smaller cache in dev
        }
        return cache.getMaxSize();
    }
    
    /**
     * Gets effective cleanup batch size.
     */
    public int getEffectiveCleanupBatchSize() {
        if (isDevelopmentMode()) {
            return Math.min(cleanup.getBatchSize(), 100); // Smaller batches in dev
        }
        return cleanup.getBatchSize();
    }
    
    /**
     * Checks if metrics should be collected.
     */
    public boolean shouldCollectMetrics() {
        return events.isMetricsEnabled() && !isDevelopmentMode();
    }
    
    /**
     * Gets configuration summary for logging.
     */
    public String getConfigurationSummary() {
        return String.format(
            "IdempotencyProperties[enabled=%s, ttl=%dh, retries=%d, cache=%s, cleanup=%s, dev=%s]",
            enabled, defaultTtlHours, defaultMaxRetries, 
            cache.isEnabled(), cleanup.isEnabled(), isDevelopmentMode()
        );
    }
    
    @Override
    public String toString() {
        return getConfigurationSummary();
    }
}