package com.fabric.batch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.*;
import java.time.Duration;
import java.util.List;

/**
 * US008: WebSocket Monitoring Configuration Properties
 * 
 * Centralized configuration for WebSocket real-time monitoring with
 * banking-grade security, performance, and compliance settings.
 * 
 * APPROVED FOR IMPLEMENTATION by Principal Enterprise Architect
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@Data
@Validated
@ConfigurationProperties(prefix = "fabric.monitoring.websocket")
public class WebSocketMonitoringProperties {

    // ============================================================================
    // CONNECTION AND SECURITY SETTINGS
    // ============================================================================
    
    /**
     * Allowed origins for WebSocket connections (CORS security)
     */
    @NotNull
    private List<String> allowedOrigins = List.of(
        "https://*.truist.com",
        "https://fabric-platform.truist.com",
        "https://fabric-platform-dev.truist.com",
        "https://fabric-platform-test.truist.com",
        "https://fabric-platform-staging.truist.com"
    );
    
    /**
     * Allow localhost origins for development (security warning if enabled)
     */
    private boolean allowLocalhost = false;
    
    /**
     * Maximum concurrent WebSocket connections per server instance
     */
    @Min(1)
    @Max(1000)
    private int maxConnections = 100;
    
    /**
     * Maximum concurrent connections per user
     */
    @Min(1)
    @Max(10)
    private int maxConnectionsPerUser = 3;
    
    /**
     * Maximum concurrent connections per IP address
     */
    @Min(1)
    @Max(50)
    private int maxConnectionsPerIp = 10;
    
    /**
     * Connection timeout in milliseconds
     */
    @Min(5000)
    @Max(300000)
    private long connectionTimeoutMs = 30000L; // 30 seconds
    
    // ============================================================================
    // MESSAGE AND TRANSPORT SETTINGS
    // ============================================================================
    
    /**
     * Maximum message size in bytes (64KB default)
     */
    @Min(1024)
    @Max(1048576) // 1MB max
    private int maxMessageSize = 65536; // 64KB
    
    /**
     * Send buffer size in bytes (512KB default)
     */
    @Min(65536)
    @Max(5242880) // 5MB max
    private int sendBufferSize = 524288; // 512KB
    
    /**
     * Send timeout in milliseconds
     */
    @Min(1000)
    @Max(60000)
    private long sendTimeoutMs = 10000L; // 10 seconds
    
    /**
     * Time to first message in milliseconds
     */
    @Min(10000)
    @Max(300000)
    private long timeToFirstMessage = 60000L; // 60 seconds
    
    /**
     * Heartbeat interval in milliseconds
     */
    @Min(5000)
    @Max(60000)
    private long heartbeatIntervalMs = 25000L; // 25 seconds
    
    /**
     * Disconnect delay in milliseconds
     */
    @Min(1000)
    @Max(30000)
    private long disconnectDelayMs = 5000L; // 5 seconds
    
    /**
     * HTTP message cache size
     */
    @Min(100)
    @Max(10000)
    private int messageCacheSize = 1000;
    
    /**
     * Maximum stream bytes for SockJS
     */
    @Min(65536)
    @Max(1048576)
    private int maxStreamBytes = 524288; // 512KB
    
    // ============================================================================
    // THREAD POOL AND PERFORMANCE SETTINGS
    // ============================================================================
    
    /**
     * Core pool size for WebSocket channels
     */
    @Min(2)
    @Max(20)
    private int channelCorePoolSize = 4;
    
    /**
     * Maximum pool size for WebSocket channels
     */
    @Min(4)
    @Max(50)
    private int channelMaxPoolSize = 8;
    
    /**
     * Keep alive seconds for channel threads
     */
    @Min(30)
    @Max(300)
    private int channelKeepAliveSeconds = 60;
    
    /**
     * Task scheduler pool size
     */
    @Min(2)
    @Max(10)
    private int schedulerPoolSize = 4;
    
    // ============================================================================
    // REAL-TIME MONITORING SETTINGS
    // ============================================================================
    
    /**
     * Update interval for real-time monitoring in milliseconds
     */
    @Min(1000)
    @Max(30000)
    private long updateIntervalMs = 5000L; // 5 seconds
    
    /**
     * Minimum update interval (adaptive interval cannot go below this)
     */
    @Min(1000)
    @Max(10000)
    private long minUpdateIntervalMs = 2000L; // 2 seconds
    
    /**
     * Maximum update interval (adaptive interval cannot exceed this)
     */
    @Min(10000)
    @Max(60000)
    private long maxUpdateIntervalMs = 15000L; // 15 seconds
    
    /**
     * Enable adaptive update intervals based on system load
     */
    private boolean adaptiveIntervals = true;
    
    /**
     * Enable delta updates to reduce message size
     */
    private boolean deltaUpdates = true;
    
    /**
     * Enable message compression
     */
    private boolean messageCompression = true;
    
    /**
     * Maximum jobs to include in a single update message
     */
    @Min(10)
    @Max(1000)
    private int maxJobsPerUpdate = 100;
    
    /**
     * Enable role-based data filtering
     */
    private boolean roleBasedFiltering = true;
    
    // ============================================================================
    // SECURITY AND COMPLIANCE SETTINGS
    // ============================================================================
    
    /**
     * Enable comprehensive audit logging
     */
    private boolean auditLogging = true;
    
    /**
     * Enable security event logging
     */
    private boolean securityEventLogging = true;
    
    /**
     * JWT token rotation interval in milliseconds
     */
    @Min(300000) // 5 minutes minimum
    @Max(3600000) // 1 hour maximum
    private long tokenRotationIntervalMs = 900000L; // 15 minutes
    
    /**
     * Session validation interval in milliseconds
     */
    @Min(30000) // 30 seconds minimum
    @Max(300000) // 5 minutes maximum
    private long sessionValidationIntervalMs = 60000L; // 1 minute
    
    /**
     * Maximum failed authentication attempts before blocking
     */
    @Min(1)
    @Max(10)
    private int maxFailedAuthAttempts = 5;
    
    /**
     * Rate limit: requests per minute per IP
     */
    @Min(10)
    @Max(1000)
    private int rateLimitRequestsPerMinute = 100;
    
    /**
     * Rate limit: connections per minute per IP
     */
    @Min(1)
    @Max(100)
    private int rateLimitConnectionsPerMinute = 10;
    
    /**
     * Block duration for rate limit violations in minutes
     */
    @Min(1)
    @Max(1440) // 24 hours max
    private int rateLimitBlockDurationMinutes = 15;
    
    // ============================================================================
    // REDIS AND CACHING SETTINGS
    // ============================================================================
    
    /**
     * Redis key prefix for WebSocket session data
     */
    @NotBlank
    private String redisKeyPrefix = "fabric:websocket";
    
    /**
     * Redis session TTL in seconds
     */
    @Min(300) // 5 minutes minimum
    @Max(86400) // 24 hours maximum
    private long redisSessionTtlSeconds = 3600L; // 1 hour
    
    /**
     * Enable Redis session clustering
     */
    private boolean redisSessionClustering = true;
    
    /**
     * Cache update metrics for performance optimization
     */
    private boolean cacheUpdateMetrics = true;
    
    /**
     * Cache TTL for dashboard data in seconds
     */
    @Min(5)
    @Max(300)
    private long cacheTtlSeconds = 30L; // 30 seconds
    
    // ============================================================================
    // CIRCUIT BREAKER SETTINGS
    // ============================================================================
    
    /**
     * Enable circuit breaker for database operations
     */
    private boolean circuitBreakerEnabled = true;
    
    /**
     * Circuit breaker failure rate threshold (percentage)
     */
    @Min(10)
    @Max(90)
    private double circuitBreakerFailureRate = 50.0; // 50%
    
    /**
     * Circuit breaker wait duration in open state (milliseconds)
     */
    @Min(10000)
    @Max(300000)
    private long circuitBreakerWaitDurationMs = 30000L; // 30 seconds
    
    /**
     * Circuit breaker sliding window size
     */
    @Min(5)
    @Max(100)
    private int circuitBreakerSlidingWindowSize = 10;
    
    /**
     * Circuit breaker minimum calls before evaluation
     */
    @Min(3)
    @Max(50)
    private int circuitBreakerMinimumCalls = 5;
    
    // ============================================================================
    // MONITORING AND ALERTING SETTINGS
    // ============================================================================
    
    /**
     * Enable performance monitoring
     */
    private boolean performanceMonitoring = true;
    
    /**
     * Performance metrics collection interval in milliseconds
     */
    @Min(10000)
    @Max(300000)
    private long performanceMetricsIntervalMs = 30000L; // 30 seconds
    
    /**
     * Memory usage alert threshold (percentage)
     */
    @Min(50)
    @Max(95)
    private double memoryUsageAlertThreshold = 85.0; // 85%
    
    /**
     * CPU usage alert threshold (percentage)
     */
    @Min(50)
    @Max(99)
    private double cpuUsageAlertThreshold = 90.0; // 90%
    
    /**
     * Response time alert threshold in milliseconds
     */
    @Min(1000)
    @Max(30000)
    private long responseTimeAlertThresholdMs = 5000L; // 5 seconds
    
    /**
     * Error rate alert threshold (percentage)
     */
    @Min(1)
    @Max(50)
    private double errorRateAlertThreshold = 5.0; // 5%
    
    /**
     * Session stale timeout in milliseconds
     */
    @Min(300000) // 5 minutes minimum
    @Max(3600000) // 1 hour maximum
    private long sessionStaleTimeoutMs = 1800000L; // 30 minutes
    
    // ============================================================================
    // FEATURE FLAGS
    // ============================================================================
    
    /**
     * Enable development mode features (reduced security for testing)
     */
    private boolean developmentMode = false;
    
    /**
     * Enable debug logging
     */
    private boolean debugLogging = false;
    
    /**
     * Enable metrics export to Prometheus
     */
    private boolean metricsExportEnabled = true;
    
    /**
     * Enable health checks endpoint
     */
    private boolean healthCheckEnabled = true;
    
    /**
     * Enable graceful shutdown
     */
    private boolean gracefulShutdownEnabled = true;
    
    /**
     * Graceful shutdown timeout in seconds
     */
    @Min(5)
    @Max(300)
    private int gracefulShutdownTimeoutSeconds = 30;
    
    // ============================================================================
    // CONVENIENCE METHODS
    // ============================================================================
    
    /**
     * Get update interval as Duration
     */
    public Duration getUpdateInterval() {
        return Duration.ofMillis(updateIntervalMs);
    }
    
    /**
     * Get heartbeat interval as Duration
     */
    public Duration getHeartbeatInterval() {
        return Duration.ofMillis(heartbeatIntervalMs);
    }
    
    /**
     * Get connection timeout as Duration
     */
    public Duration getConnectionTimeout() {
        return Duration.ofMillis(connectionTimeoutMs);
    }
    
    /**
     * Get token rotation interval as Duration
     */
    public Duration getTokenRotationInterval() {
        return Duration.ofMillis(tokenRotationIntervalMs);
    }
    
    /**
     * Get session validation interval as Duration
     */
    public Duration getSessionValidationInterval() {
        return Duration.ofMillis(sessionValidationIntervalMs);
    }
    
    /**
     * Get Redis session TTL as Duration
     */
    public Duration getRedisSessionTtl() {
        return Duration.ofSeconds(redisSessionTtlSeconds);
    }
    
    /**
     * Get cache TTL as Duration
     */
    public Duration getCacheTtl() {
        return Duration.ofSeconds(cacheTtlSeconds);
    }
    
    /**
     * Check if security features are enabled (based on development mode)
     */
    public boolean isSecurityEnabled() {
        return !developmentMode;
    }
    
    /**
     * Check if comprehensive logging should be enabled
     */
    public boolean isComprehensiveLoggingEnabled() {
        return auditLogging && securityEventLogging;
    }
    
    /**
     * Get effective rate limit for connections
     */
    public int getEffectiveConnectionRateLimit() {
        return developmentMode ? rateLimitConnectionsPerMinute * 10 : rateLimitConnectionsPerMinute;
    }
    
    /**
     * Get effective rate limit for requests
     */
    public int getEffectiveRequestRateLimit() {
        return developmentMode ? rateLimitRequestsPerMinute * 10 : rateLimitRequestsPerMinute;
    }
    
    /**
     * Validation method to ensure configuration consistency
     */
    public void validateConfiguration() {
        if (channelMaxPoolSize < channelCorePoolSize) {
            throw new IllegalArgumentException("Channel max pool size must be >= core pool size");
        }
        
        if (maxUpdateIntervalMs <= minUpdateIntervalMs) {
            throw new IllegalArgumentException("Max update interval must be > min update interval");
        }
        
        if (maxConnectionsPerUser > maxConnections) {
            throw new IllegalArgumentException("Max connections per user cannot exceed max connections");
        }
        
        if (tokenRotationIntervalMs < 300000 && !developmentMode) {
            throw new IllegalArgumentException("Token rotation interval must be at least 5 minutes in production");
        }
    }
}