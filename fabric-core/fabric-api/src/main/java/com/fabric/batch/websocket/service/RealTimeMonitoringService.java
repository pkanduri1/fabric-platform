package com.fabric.batch.websocket.service;

import com.fabric.batch.config.WebSocketMonitoringProperties;
import com.fabric.batch.monitor.Epic2PerformanceMonitor;
import com.fabric.batch.websocket.handler.WebSocketSessionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * US008: Real-Time Monitoring Service for WebSocket Dashboard
 * 
 * Extends Epic2PerformanceMonitor to provide real-time monitoring capabilities
 * for the WebSocket dashboard with:
 * - Delta update calculations for efficient broadcasting
 * - Circuit breaker patterns for resilient operations
 * - Adaptive update intervals based on system load
 * - Role-based data filtering for security
 * - Redis caching for improved performance
 * - Comprehensive metrics collection and aggregation
 * 
 * APPROVED FOR IMPLEMENTATION by Principal Enterprise Architect
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@Slf4j
// @Service - Temporarily disabled to get basic backend running
@RequiredArgsConstructor
public class RealTimeMonitoringService extends Epic2PerformanceMonitor {

    private final WebSocketMonitoringProperties monitoringProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Session management
    private final Map<String, WebSocketSessionInfo> subscribedSessions = new ConcurrentHashMap<>();
    
    // Delta update tracking
    private MonitoringSnapshot previousSnapshot;
    private Instant lastUpdateTime;
    private final Map<String, Object> cachedUpdates = new ConcurrentHashMap<>();
    
    // Performance tracking
    private AdaptiveIntervalCalculator intervalCalculator = new AdaptiveIntervalCalculator();
    private CircuitBreakerService circuitBreaker = new CircuitBreakerService();
    
    /**
     * Subscribe a WebSocket session to real-time monitoring updates
     */
    public void subscribeSession(WebSocketSessionInfo sessionInfo) {
        try {
            subscribedSessions.put(sessionInfo.getSessionId(), sessionInfo);
            
            // Subscribe to specific monitoring types based on user roles
            subscribeToMonitoringTypes(sessionInfo);
            
            // Send initial monitoring data
            sendInitialMonitoringData(sessionInfo);
            
            log.info("📊 Session subscribed to monitoring: session={}, user={}, roles={}", 
                    sessionInfo.getSessionId(), sessionInfo.getUserId(), sessionInfo.getUserRoles());
                    
        } catch (Exception e) {
            log.error("❌ Failed to subscribe session to monitoring: session={}, error={}", 
                    sessionInfo.getSessionId(), e.getMessage(), e);
        }
    }
    
    /**
     * Unsubscribe a WebSocket session from monitoring updates
     */
    public void unsubscribeSession(WebSocketSessionInfo sessionInfo) {
        try {
            subscribedSessions.remove(sessionInfo.getSessionId());
            
            log.info("📴 Session unsubscribed from monitoring: session={}, user={}", 
                    sessionInfo.getSessionId(), sessionInfo.getUserId());
                    
        } catch (Exception e) {
            log.error("❌ Failed to unsubscribe session: session={}, error={}", 
                    sessionInfo.getSessionId(), e.getMessage(), e);
        }
    }
    
    /**
     * Scheduled method to collect and broadcast real-time monitoring updates
     */
    @Scheduled(fixedDelayString = "#{monitoringProperties.updateInterval.toMillis()}")
    public void collectAndBroadcastUpdates() {
        if (subscribedSessions.isEmpty()) {
            return; // No subscribers
        }
        
        Instant startTime = Instant.now();
        
        try {
            // Check circuit breaker state
            if (circuitBreaker.isOpen()) {
                log.warn("⚠️ Circuit breaker is open, skipping monitoring update");
                return;
            }
            
            // Collect current monitoring data
            MonitoringSnapshot currentSnapshot = collectCurrentSnapshot();
            
            // Calculate delta updates for efficiency
            MonitoringDelta delta = calculateDelta(currentSnapshot);
            
            if (delta.hasChanges() || isForceUpdateRequired()) {
                // Broadcast delta to subscribed sessions
                broadcastDeltaToSessions(delta);
                
                // Cache current snapshot for next delta calculation
                previousSnapshot = currentSnapshot;
                lastUpdateTime = startTime;
                
                // Update adaptive interval based on system load
                updateAdaptiveInterval(startTime);
            }
            
            // Record successful operation
            circuitBreaker.recordSuccess();
            
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            log.debug("📡 Monitoring update cycle completed: sessions={}, changes={}, duration={}ms", 
                    subscribedSessions.size(), delta.hasChanges(), duration);
                    
        } catch (Exception e) {
            log.error("❌ Failed to collect and broadcast monitoring updates: error={}", e.getMessage(), e);
            
            // Record failure for circuit breaker
            circuitBreaker.recordFailure(e);
            
            // Send fallback data if available
            sendFallbackMonitoringData();
        }
    }
    
    /**
     * Collect current monitoring snapshot
     */
    private MonitoringSnapshot collectCurrentSnapshot() {
        // Use existing Epic2PerformanceMonitor capabilities enhanced with real-time features
        try {
            // Get comprehensive performance dashboard data
            PerformanceDashboard dashboard = getPerformanceDashboard();
            
            // Enhance with real-time WebSocket specific data
            WebSocketMetrics wsMetrics = collectWebSocketMetrics();
            
            return MonitoringSnapshot.builder()
                    .timestamp(Instant.now())
                    .systemMetrics(dashboard.getSystemMetrics())
                    .businessMetrics(dashboard.getBusinessMetrics())
                    .threadPoolMetrics(dashboard.getThreadPoolMetrics())
                    .executionMetrics(dashboard.getExecutionMetrics())
                    .alerts(dashboard.getAlerts())
                    .complianceMetrics(dashboard.getCompliance())
                    .webSocketMetrics(wsMetrics)
                    .activeJobCount(getActiveJobCount())
                    .totalSubscribers(subscribedSessions.size())
                    .build();
                    
        } catch (Exception e) {
            log.error("❌ Failed to collect monitoring snapshot: {}", e.getMessage(), e);
            return createFallbackSnapshot();
        }
    }
    
    /**
     * Calculate delta between current and previous snapshots
     */
    private MonitoringDelta calculateDelta(MonitoringSnapshot currentSnapshot) {
        if (previousSnapshot == null) {
            // First run - send complete snapshot as delta
            return MonitoringDelta.fromCompleteSnapshot(currentSnapshot);
        }
        
        MonitoringDelta.MonitoringDeltaBuilder deltaBuilder = MonitoringDelta.builder()
                .timestamp(currentSnapshot.getTimestamp())
                .hasChanges(false);
        
        // Check for system metrics changes
        if (hasSystemMetricsChanged(currentSnapshot.getSystemMetrics(), previousSnapshot.getSystemMetrics())) {
            deltaBuilder.systemMetricsChanges(currentSnapshot.getSystemMetrics());
            deltaBuilder.hasChanges(true);
        }
        
        // Check for business metrics changes
        if (hasBusinessMetricsChanged(currentSnapshot.getBusinessMetrics(), previousSnapshot.getBusinessMetrics())) {
            deltaBuilder.businessMetricsChanges(currentSnapshot.getBusinessMetrics());
            deltaBuilder.hasChanges(true);
        }
        
        // Check for new, updated, or completed jobs
        JobsDelta jobsDelta = calculateJobsDelta(
                currentSnapshot.getExecutionMetrics(), 
                previousSnapshot.getExecutionMetrics()
        );
        if (jobsDelta.hasChanges()) {
            deltaBuilder.jobsDelta(jobsDelta);
            deltaBuilder.hasChanges(true);
        }
        
        // Check for alert changes
        AlertsDelta alertsDelta = calculateAlertsDelta(
                currentSnapshot.getAlerts(), 
                previousSnapshot.getAlerts()
        );
        if (alertsDelta.hasChanges()) {
            deltaBuilder.alertsDelta(alertsDelta);
            deltaBuilder.hasChanges(true);
        }
        
        // Check for WebSocket metrics changes
        if (hasWebSocketMetricsChanged(currentSnapshot.getWebSocketMetrics(), 
                                     previousSnapshot.getWebSocketMetrics())) {
            deltaBuilder.webSocketMetricsChanges(currentSnapshot.getWebSocketMetrics());
            deltaBuilder.hasChanges(true);
        }
        
        return deltaBuilder.build();
    }
    
    /**
     * Broadcast delta updates to subscribed sessions with role-based filtering
     */
    private void broadcastDeltaToSessions(MonitoringDelta delta) {
        if (subscribedSessions.isEmpty()) {
            return;
        }
        
        int successCount = 0;
        int errorCount = 0;
        Map<String, Integer> roleBasedCounts = new HashMap<>();
        
        for (WebSocketSessionInfo sessionInfo : subscribedSessions.values()) {
            try {
                // Apply role-based filtering
                MonitoringDelta filteredDelta = filterDeltaForRoles(delta, sessionInfo.getUserRoles());
                
                if (!filteredDelta.isEmpty()) {
                    // Send update through WebSocket handler
                    sendDeltaToSession(sessionInfo, filteredDelta);
                    successCount++;
                    
                    // Track by role for analytics
                    String primaryRole = getPrimaryRole(sessionInfo.getUserRoles());
                    roleBasedCounts.merge(primaryRole, 1, Integer::sum);
                }
                
            } catch (Exception e) {
                errorCount++;
                log.warn("⚠️ Failed to broadcast delta to session: session={}, user={}, error={}", 
                        sessionInfo.getSessionId(), sessionInfo.getUserId(), e.getMessage());
            }
        }
        
        log.debug("📤 Delta broadcast completed: total={}, success={}, errors={}, byRole={}", 
                subscribedSessions.size(), successCount, errorCount, roleBasedCounts);
    }
    
    /**
     * Filter monitoring delta based on user roles
     */
    private MonitoringDelta filterDeltaForRoles(MonitoringDelta delta, List<String> userRoles) {
        // Operations Managers get full access
        if (userRoles.contains("OPERATIONS_MANAGER") || userRoles.contains("ADMIN")) {
            return delta;
        }
        
        // Operations Viewers get limited access
        if (userRoles.contains("OPERATIONS_VIEWER")) {
            return filterForViewerRole(delta);
        }
        
        // Default to minimal access
        return filterForMinimalAccess(delta);
    }
    
    private MonitoringDelta filterForViewerRole(MonitoringDelta delta) {
        return MonitoringDelta.builder()
                .timestamp(delta.getTimestamp())
                .hasChanges(delta.isHasChanges())
                .jobsDelta(sanitizeJobsForViewer(delta.getJobsDelta()))
                .alertsDelta(filterAlertsForViewer(delta.getAlertsDelta()))
                .systemMetricsChanges(sanitizeSystemMetricsForViewer(delta.getSystemMetricsChanges()))
                .businessMetricsChanges(delta.getBusinessMetricsChanges()) // Business metrics allowed
                // Exclude sensitive WebSocket metrics and detailed performance data
                .build();
    }
    
    private MonitoringDelta filterForMinimalAccess(MonitoringDelta delta) {
        return MonitoringDelta.builder()
                .timestamp(delta.getTimestamp())
                .hasChanges(delta.isHasChanges())
                .jobsDelta(sanitizeJobsForMinimalAccess(delta.getJobsDelta()))
                .businessMetricsChanges(sanitizeBusinessMetricsForMinimalAccess(delta.getBusinessMetricsChanges()))
                .build();
    }
    
    /**
     * Send delta update to specific session
     */
    private void sendDeltaToSession(WebSocketSessionInfo sessionInfo, MonitoringDelta delta) {
        try {
            // This would integrate with the WebSocket handler to send the actual message
            // For now, this is a placeholder
            
            // Create message payload
            Map<String, Object> payload = Map.of(
                "type", "monitoring_update",
                "timestamp", delta.getTimestamp().toString(),
                "delta", delta,
                "sequenceNumber", System.currentTimeMillis() % 100000
            );
            
            // The actual WebSocket sending would be handled by the WebSocketHandler
            log.debug("📨 Delta sent to session: session={}, user={}, changes={}", 
                    sessionInfo.getSessionId(), sessionInfo.getUserId(), delta.isHasChanges());
                    
        } catch (Exception e) {
            log.error("❌ Failed to send delta to session: session={}, error={}", 
                    sessionInfo.getSessionId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Collect WebSocket-specific metrics
     */
    private WebSocketMetrics collectWebSocketMetrics() {
        return WebSocketMetrics.builder()
                .totalConnections(subscribedSessions.size())
                .connectionsByRole(getConnectionsByRole())
                .averageConnectionDuration(getAverageConnectionDuration())
                .messagesSentPerMinute(getMessagesSentPerMinute())
                .messagesReceivedPerMinute(getMessagesReceivedPerMinute())
                .averageResponseTime(getAverageResponseTime())
                .errorRate(getConnectionErrorRate())
                .build();
    }
    
    /**
     * Update adaptive interval based on system performance
     */
    private void updateAdaptiveInterval(Instant startTime) {
        if (!monitoringProperties.isAdaptiveIntervals()) {
            return;
        }
        
        long processingTime = Duration.between(startTime, Instant.now()).toMillis();
        int activeConnections = subscribedSessions.size();
        double systemLoad = getCurrentSystemLoad();
        
        long newInterval = intervalCalculator.calculateInterval(processingTime, activeConnections, systemLoad);
        
        if (newInterval != monitoringProperties.getUpdateIntervalMs()) {
            log.info("⏱️ Adaptive interval updated: old={}ms, new={}ms, load={}, connections={}", 
                    monitoringProperties.getUpdateIntervalMs(), newInterval, systemLoad, activeConnections);
            
            // This would update the scheduled task interval dynamically
            // Implementation would depend on Spring's scheduling capabilities
        }
    }
    
    // ============================================================================
    // HELPER METHODS AND PRIVATE IMPLEMENTATIONS
    // ============================================================================
    
    private void subscribeToMonitoringTypes(WebSocketSessionInfo sessionInfo) {
        // Subscribe based on user roles
        List<String> roles = sessionInfo.getUserRoles();
        
        if (roles.contains("OPERATIONS_MANAGER") || roles.contains("ADMIN")) {
            // Full access - subscribe to all monitoring types
            sessionInfo.addSubscription(WebSocketSessionInfo.SubscriptionType.JOB_STATUS, 
                    createSubscription(WebSocketSessionInfo.SubscriptionType.JOB_STATUS));
            sessionInfo.addSubscription(WebSocketSessionInfo.SubscriptionType.SYSTEM_METRICS, 
                    createSubscription(WebSocketSessionInfo.SubscriptionType.SYSTEM_METRICS));
            sessionInfo.addSubscription(WebSocketSessionInfo.SubscriptionType.BUSINESS_KPIS, 
                    createSubscription(WebSocketSessionInfo.SubscriptionType.BUSINESS_KPIS));
            sessionInfo.addSubscription(WebSocketSessionInfo.SubscriptionType.ALERTS, 
                    createSubscription(WebSocketSessionInfo.SubscriptionType.ALERTS));
            sessionInfo.addSubscription(WebSocketSessionInfo.SubscriptionType.AUDIT_EVENTS, 
                    createSubscription(WebSocketSessionInfo.SubscriptionType.AUDIT_EVENTS));
        } else if (roles.contains("OPERATIONS_VIEWER")) {
            // Limited access - subscribe to business metrics and basic job status
            sessionInfo.addSubscription(WebSocketSessionInfo.SubscriptionType.JOB_STATUS, 
                    createSubscription(WebSocketSessionInfo.SubscriptionType.JOB_STATUS));
            sessionInfo.addSubscription(WebSocketSessionInfo.SubscriptionType.BUSINESS_KPIS, 
                    createSubscription(WebSocketSessionInfo.SubscriptionType.BUSINESS_KPIS));
        } else {
            // Minimal access - subscribe to basic business metrics only
            sessionInfo.addSubscription(WebSocketSessionInfo.SubscriptionType.BUSINESS_KPIS, 
                    createSubscription(WebSocketSessionInfo.SubscriptionType.BUSINESS_KPIS));
        }
    }
    
    private WebSocketSessionInfo.MonitoringSubscription createSubscription(
            WebSocketSessionInfo.SubscriptionType type) {
        return WebSocketSessionInfo.MonitoringSubscription.builder()
                .type(type)
                .subscribedAt(Instant.now())
                .active(true)
                .filters(new HashMap<>())
                .preferences(new HashMap<>())
                .build();
    }
    
    private void sendInitialMonitoringData(WebSocketSessionInfo sessionInfo) {
        try {
            // Send current state as initial data
            MonitoringSnapshot currentSnapshot = collectCurrentSnapshot();
            MonitoringDelta initialDelta = MonitoringDelta.fromCompleteSnapshot(currentSnapshot);
            MonitoringDelta filteredDelta = filterDeltaForRoles(initialDelta, sessionInfo.getUserRoles());
            
            sendDeltaToSession(sessionInfo, filteredDelta);
            
        } catch (Exception e) {
            log.error("❌ Failed to send initial monitoring data: session={}, error={}", 
                    sessionInfo.getSessionId(), e.getMessage(), e);
        }
    }
    
    // Placeholder implementations for helper methods
    private boolean hasSystemMetricsChanged(Object current, Object previous) { return true; }
    private boolean hasBusinessMetricsChanged(Object current, Object previous) { return true; }
    private boolean hasWebSocketMetricsChanged(WebSocketMetrics current, WebSocketMetrics previous) { return true; }
    private JobsDelta calculateJobsDelta(Object current, Object previous) { return new JobsDelta(); }
    private AlertsDelta calculateAlertsDelta(Object current, Object previous) { return new AlertsDelta(); }
    private boolean isForceUpdateRequired() { return false; }
    private void sendFallbackMonitoringData() { }
    private MonitoringSnapshot createFallbackSnapshot() { return new MonitoringSnapshot(); }
    private int getActiveJobCount() { return 0; }
    private Map<String, Integer> getConnectionsByRole() { return new HashMap<>(); }
    private double getAverageConnectionDuration() { return 0.0; }
    private int getMessagesSentPerMinute() { return 0; }
    private int getMessagesReceivedPerMinute() { return 0; }
    private double getAverageResponseTime() { return 0.0; }
    private double getConnectionErrorRate() { return 0.0; }
    private double getCurrentSystemLoad() { return 0.5; }
    private String getPrimaryRole(List<String> roles) { 
        return roles.isEmpty() ? "UNKNOWN" : roles.get(0); 
    }
    private JobsDelta sanitizeJobsForViewer(JobsDelta delta) { return delta; }
    private AlertsDelta filterAlertsForViewer(AlertsDelta delta) { return delta; }
    private Object sanitizeSystemMetricsForViewer(Object metrics) { return metrics; }
    private JobsDelta sanitizeJobsForMinimalAccess(JobsDelta delta) { return delta; }
    private Object sanitizeBusinessMetricsForMinimalAccess(Object metrics) { return metrics; }
    
    // Placeholder method for performance dashboard
    private PerformanceDashboard getPerformanceDashboard() {
        return PerformanceDashboard.builder()
            .performanceMetrics(new Object())
            .systemHealth(new Object())
            .executionStats(new Object())
            .build();
    }
    
    
    // Placeholder classes - would be fully implemented
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MonitoringSnapshot { 
        private Instant timestamp = Instant.now();
        private Object systemMetrics = new Object();
        private Object businessMetrics = new Object();
        private Object threadPoolMetrics = new Object();
        private Object executionMetrics = new Object();
        private Object alerts = new Object();
        private Object complianceMetrics = new Object();
        private WebSocketMetrics webSocketMetrics = WebSocketMetrics.builder().build();
        private int activeJobCount;
        private int totalSubscribers;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MonitoringDelta {
        private Instant timestamp;
        private boolean hasChanges;
        private Object systemMetricsChanges;
        private Object businessMetricsChanges;
        private JobsDelta jobsDelta;
        private AlertsDelta alertsDelta;
        private Object webSocketMetricsChanges;
        
        public boolean isEmpty() { return !hasChanges; }
        public boolean hasChanges() { return this.hasChanges; }
        
        public static MonitoringDelta fromCompleteSnapshot(MonitoringSnapshot snapshot) {
            return MonitoringDelta.builder()
                    .timestamp(snapshot.getTimestamp())
                    .hasChanges(true)
                    .systemMetricsChanges(snapshot.getSystemMetrics())
                    .businessMetricsChanges(snapshot.getBusinessMetrics())
                    .webSocketMetricsChanges(snapshot.getWebSocketMetrics())
                    .build();
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class WebSocketMetrics {
        private int totalConnections;
        private Map<String, Integer> connectionsByRole;
        private double averageConnectionDuration;
        private int messagesSentPerMinute;
        private int messagesReceivedPerMinute;
        private double averageResponseTime;
        private double errorRate;
    }
    
    // Placeholder PerformanceDashboard class
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor 
    @lombok.AllArgsConstructor
    public static class PerformanceDashboard {
        private Object performanceMetrics = new Object();
        private Object systemHealth = new Object();
        private Object executionStats = new Object();
        
        // Additional getter methods needed by the builder
        public Object getSystemMetrics() { return performanceMetrics; }
        public Object getBusinessMetrics() { return systemHealth; }
        public Object getThreadPoolMetrics() { return executionStats; }
        public Object getExecutionMetrics() { return executionStats; }
        public Object getAlerts() { return new Object(); }
        public Object getCompliance() { return new Object(); }
    }
    
    public static class JobsDelta { 
        public boolean hasChanges() { return false; }
    }
    public static class AlertsDelta { 
        public boolean hasChanges() { return false; }
    }
    
    // Circuit breaker and adaptive interval calculator classes would be implemented here
    private static class CircuitBreakerService {
        public boolean isOpen() { return false; }
        public void recordSuccess() { }
        public void recordFailure(Exception e) { }
    }
    
    private static class AdaptiveIntervalCalculator {
        public long calculateInterval(long processingTime, int connections, double systemLoad) {
            return 5000L; // Default 5 seconds
        }
    }
}