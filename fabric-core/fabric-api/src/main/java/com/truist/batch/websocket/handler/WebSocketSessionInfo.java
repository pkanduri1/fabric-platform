package com.truist.batch.websocket.handler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * US008: WebSocket Session Information Tracking
 * 
 * Comprehensive session tracking for real-time monitoring WebSocket connections
 * with security, performance, and compliance metadata.
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketSessionInfo {

    // ============================================================================
    // CORE SESSION INFORMATION
    // ============================================================================
    
    private String sessionId;
    private WebSocketSession session; // Transient - not serialized to Redis
    private String userId;
    private List<String> userRoles;
    private String correlationId;
    
    // ============================================================================
    // CONNECTION INFORMATION
    // ============================================================================
    
    private String clientIp;
    private String userAgent;
    private String originUrl;
    private Instant connectedAt;
    private Instant lastActivity;
    private Instant disconnectedAt;
    private ConnectionStatus connectionStatus;
    
    // ============================================================================
    // AUTHENTICATION AND SECURITY
    // ============================================================================
    
    private String jwtToken;
    private String csrfToken;
    private Instant lastTokenRotation;
    private Integer failedAuthAttempts;
    private Integer rateLimitViolations;
    private Map<String, Object> securityViolations;
    
    // ============================================================================
    // PERFORMANCE METRICS
    // ============================================================================
    
    @Builder.Default
    private AtomicInteger messagesSent = new AtomicInteger(0);
    
    @Builder.Default
    private AtomicInteger messagesReceived = new AtomicInteger(0);
    
    @Builder.Default
    private AtomicLong bytesSent = new AtomicLong(0);
    
    @Builder.Default
    private AtomicLong bytesReceived = new AtomicLong(0);
    
    @Builder.Default
    private AtomicInteger errorCount = new AtomicInteger(0);
    
    private Double connectionQualityScore;
    private Long averageResponseTime;
    
    // ============================================================================
    // MONITORING SUBSCRIPTIONS
    // ============================================================================
    
    @Builder.Default
    private Map<String, MonitoringSubscription> subscriptions = new ConcurrentHashMap<>();
    
    private Map<String, Object> monitoringPreferences;
    private Boolean deltaUpdatesEnabled;
    private Boolean compressionEnabled;
    
    // ============================================================================
    // COMPLIANCE AND AUDIT
    // ============================================================================
    
    private String auditHash;
    private List<String> complianceFlags;
    private Map<String, Object> auditMetadata;
    
    // ============================================================================
    // ENUMS
    // ============================================================================
    
    public enum ConnectionStatus {
        CONNECTING,
        ACTIVE,
        INACTIVE,
        DISCONNECTING,
        DISCONNECTED,
        TERMINATED,
        SUSPENDED
    }
    
    public enum SubscriptionType {
        JOB_STATUS,
        SYSTEM_METRICS,
        BUSINESS_KPIS,
        ALERTS,
        AUDIT_EVENTS
    }
    
    // ============================================================================
    // BUSINESS LOGIC METHODS
    // ============================================================================
    
    /**
     * Update last activity timestamp
     */
    public void updateLastActivity() {
        this.lastActivity = Instant.now();
    }
    
    /**
     * Increment messages sent counter
     */
    public void incrementMessagesSent() {
        this.messagesSent.incrementAndGet();
    }
    
    /**
     * Increment messages received counter
     */
    public void incrementMessagesReceived() {
        this.messagesReceived.incrementAndGet();
    }
    
    /**
     * Increment error counter
     */
    public void incrementErrorCount() {
        this.errorCount.incrementAndGet();
    }
    
    /**
     * Add bytes sent
     */
    public void addBytesSent(long bytes) {
        this.bytesSent.addAndGet(bytes);
    }
    
    /**
     * Add bytes received
     */
    public void addBytesReceived(long bytes) {
        this.bytesReceived.addAndGet(bytes);
    }
    
    /**
     * Check if session is active
     */
    public boolean isActive() {
        return connectionStatus == ConnectionStatus.ACTIVE;
    }
    
    /**
     * Check if session is connected (any connected state)
     */
    public boolean isConnected() {
        return connectionStatus == ConnectionStatus.CONNECTING ||
               connectionStatus == ConnectionStatus.ACTIVE ||
               connectionStatus == ConnectionStatus.INACTIVE;
    }
    
    /**
     * Check if session is terminated
     */
    public boolean isTerminated() {
        return connectionStatus == ConnectionStatus.DISCONNECTED ||
               connectionStatus == ConnectionStatus.TERMINATED;
    }
    
    /**
     * Get connection duration in seconds
     */
    public long getConnectionDurationSeconds() {
        if (connectedAt == null) {
            return 0;
        }
        
        Instant endTime = disconnectedAt != null ? disconnectedAt : Instant.now();
        return java.time.Duration.between(connectedAt, endTime).getSeconds();
    }
    
    /**
     * Get time since last activity in seconds
     */
    public long getTimeSinceLastActivitySeconds() {
        if (lastActivity == null) {
            return 0;
        }
        
        return java.time.Duration.between(lastActivity, Instant.now()).getSeconds();
    }
    
    /**
     * Check if session is stale (no activity for specified duration)
     */
    public boolean isStale(java.time.Duration staleThreshold) {
        if (lastActivity == null) {
            return true;
        }
        
        return java.time.Duration.between(lastActivity, Instant.now()).compareTo(staleThreshold) > 0;
    }
    
    /**
     * Check if user has specific role
     */
    public boolean hasRole(String role) {
        return userRoles != null && userRoles.contains(role);
    }
    
    /**
     * Check if user has any of the specified roles
     */
    public boolean hasAnyRole(List<String> roles) {
        if (userRoles == null || roles == null) {
            return false;
        }
        
        return userRoles.stream().anyMatch(roles::contains);
    }
    
    /**
     * Get subscription for specific type
     */
    public MonitoringSubscription getSubscription(SubscriptionType type) {
        return subscriptions.get(type.name());
    }
    
    /**
     * Add monitoring subscription
     */
    public void addSubscription(SubscriptionType type, MonitoringSubscription subscription) {
        subscriptions.put(type.name(), subscription);
        updateLastActivity();
    }
    
    /**
     * Remove monitoring subscription
     */
    public void removeSubscription(SubscriptionType type) {
        subscriptions.remove(type.name());
        updateLastActivity();
    }
    
    /**
     * Check if subscribed to specific monitoring type
     */
    public boolean isSubscribedTo(SubscriptionType type) {
        MonitoringSubscription subscription = subscriptions.get(type.name());
        return subscription != null && subscription.isActive();
    }
    
    /**
     * Get connection quality score (0-100)
     */
    public double getConnectionQuality() {
        if (connectionQualityScore != null) {
            return connectionQualityScore;
        }
        
        // Calculate based on error rate and response times
        int totalMessages = messagesSent.get() + messagesReceived.get();
        if (totalMessages == 0) {
            return 100.0;
        }
        
        double errorRate = (double) errorCount.get() / totalMessages;
        double qualityScore = Math.max(0.0, 100.0 - (errorRate * 100.0));
        
        // Adjust for response time if available
        if (averageResponseTime != null && averageResponseTime > 1000) {
            qualityScore *= 0.9; // Reduce quality for slow responses
        }
        
        return Math.min(100.0, qualityScore);
    }
    
    /**
     * Record security violation
     */
    public void recordSecurityViolation(String violationType, String description) {
        if (securityViolations == null) {
            securityViolations = new ConcurrentHashMap<>();
        }
        
        String key = violationType + "_" + Instant.now().getEpochSecond();
        securityViolations.put(key, Map.of(
            "type", violationType,
            "description", description,
            "timestamp", Instant.now().toString()
        ));
        
        updateLastActivity();
    }
    
    /**
     * Get security violation count
     */
    public int getSecurityViolationCount() {
        return securityViolations != null ? securityViolations.size() : 0;
    }
    
    /**
     * Check if session has security violations
     */
    public boolean hasSecurityViolations() {
        return getSecurityViolationCount() > 0;
    }
    
    /**
     * Convert to audit-friendly map
     */
    public Map<String, Object> toAuditMap() {
        return Map.of(
            "sessionId", sessionId,
            "userId", userId,
            "clientIp", clientIp,
            "connectedAt", connectedAt != null ? connectedAt.toString() : null,
            "lastActivity", lastActivity != null ? lastActivity.toString() : null,
            "connectionStatus", connectionStatus.toString(),
            "messagesSent", messagesSent.get(),
            "messagesReceived", messagesReceived.get(),
            "errorCount", errorCount.get(),
            "connectionDuration", getConnectionDurationSeconds(),
            "subscriptions", subscriptions.keySet()
        );
    }
    
    /**
     * Create summary for monitoring
     */
    public SessionSummary createSummary() {
        return SessionSummary.builder()
                .sessionId(sessionId)
                .userId(userId)
                .clientIp(clientIp)
                .status(connectionStatus)
                .connectedAt(connectedAt)
                .lastActivity(lastActivity)
                .connectionDurationSeconds(getConnectionDurationSeconds())
                .messagesSent(messagesSent.get())
                .messagesReceived(messagesReceived.get())
                .errorCount(errorCount.get())
                .connectionQuality(getConnectionQuality())
                .subscriptionCount(subscriptions.size())
                .hasSecurityViolations(hasSecurityViolations())
                .build();
    }
    
    // ============================================================================
    // NESTED CLASSES
    // ============================================================================
    
    /**
     * Monitoring subscription information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonitoringSubscription {
        private SubscriptionType type;
        private Instant subscribedAt;
        private Instant lastUpdate;
        private boolean active;
        private Map<String, Object> filters;
        private Map<String, Object> preferences;
        
        public boolean isActive() {
            return active;
        }
        
        public void updateLastUpdate() {
            this.lastUpdate = Instant.now();
        }
    }
    
    /**
     * Session summary for monitoring dashboards
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionSummary {
        private String sessionId;
        private String userId;
        private String clientIp;
        private ConnectionStatus status;
        private Instant connectedAt;
        private Instant lastActivity;
        private long connectionDurationSeconds;
        private int messagesSent;
        private int messagesReceived;
        private int errorCount;
        private double connectionQuality;
        private int subscriptionCount;
        private boolean hasSecurityViolations;
    }
}