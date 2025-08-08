package com.truist.batch.idempotency.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Model representing request context information for comprehensive
 * audit trail and security tracking of idempotent operations.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestContext {
    
    /**
     * User identifier who initiated the request
     */
    private String userId;
    
    /**
     * Session identifier for tracking user sessions
     */
    private String sessionId;
    
    /**
     * Client IP address for security tracking
     */
    private String clientIp;
    
    /**
     * User agent string for client identification
     */
    private String userAgent;
    
    /**
     * Device fingerprint for enhanced security
     */
    private String deviceFingerprint;
    
    /**
     * Business context description
     */
    private String businessContext;
    
    /**
     * Application name that initiated the request
     */
    private String applicationName;
    
    /**
     * Application version for version tracking
     */
    private String applicationVersion;
    
    /**
     * Environment identifier (DEV, TEST, PROD)
     */
    private String environment;
    
    /**
     * Host name where request originated
     */
    private String hostName;
    
    /**
     * Process ID for system tracking
     */
    private Long processId;
    
    /**
     * Thread ID for concurrency tracking
     */
    private Long threadId;
    
    /**
     * Request timestamp
     */
    private LocalDateTime requestTimestamp;
    
    /**
     * Trace ID for distributed tracing
     */
    private String traceId;
    
    /**
     * Span ID for distributed tracing
     */
    private String spanId;
    
    /**
     * Request headers for HTTP requests
     */
    private Map<String, String> requestHeaders;
    
    /**
     * Custom context attributes
     */
    private Map<String, Object> customAttributes;
    
    /**
     * Security context information
     */
    private SecurityContext securityContext;
    
    /**
     * Performance context information
     */
    private PerformanceContext performanceContext;
    
    // Utility methods
    
    /**
     * Creates a summary string for logging.
     */
    public String getSummary() {
        return String.format("RequestContext[user=%s, session=%s, ip=%s, app=%s]",
                userId, sessionId, clientIp, applicationName);
    }
    
    /**
     * Checks if security context is available.
     */
    public boolean hasSecurityContext() {
        return securityContext != null;
    }
    
    /**
     * Checks if performance context is available.
     */
    public boolean hasPerformanceContext() {
        return performanceContext != null;
    }
    
    /**
     * Checks if distributed tracing information is available.
     */
    public boolean hasTracingInfo() {
        return (traceId != null && !traceId.trim().isEmpty()) ||
               (spanId != null && !spanId.trim().isEmpty());
    }
    
    /**
     * Checks if request headers are available.
     */
    public boolean hasRequestHeaders() {
        return requestHeaders != null && !requestHeaders.isEmpty();
    }
    
    /**
     * Checks if custom attributes are available.
     */
    public boolean hasCustomAttributes() {
        return customAttributes != null && !customAttributes.isEmpty();
    }
    
    /**
     * Gets effective timestamp (current time if not set).
     */
    public LocalDateTime getEffectiveTimestamp() {
        return requestTimestamp != null ? requestTimestamp : LocalDateTime.now();
    }
    
    /**
     * Factory method for batch job context.
     */
    public static RequestContext forBatchJob(String userId, String sessionId) {
        return RequestContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .clientIp("localhost")
                .userAgent("Spring Batch Job Execution")
                .applicationName("fabric-batch")
                .businessContext("Batch Job Processing")
                .requestTimestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Factory method for API request context.
     */
    public static RequestContext forApiRequest(String userId, String sessionId, 
                                             String clientIp, String userAgent) {
        return RequestContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .applicationName("fabric-api")
                .businessContext("API Request Processing")
                .requestTimestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Factory method for system context.
     */
    public static RequestContext forSystemOperation(String operation) {
        return RequestContext.builder()
                .userId("SYSTEM")
                .sessionId("SYSTEM_SESSION")
                .clientIp("localhost")
                .userAgent("System Operation")
                .applicationName("fabric-platform")
                .businessContext(operation)
                .requestTimestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Security context nested class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityContext {
        private String authenticationMethod;
        private String[] roles;
        private String[] permissions;
        private String tenantId;
        private String organizationId;
        private boolean isAuthenticated;
        private boolean isAuthorized;
        private LocalDateTime authenticationTime;
        private String authenticationToken;
        
        public boolean hasRole(String role) {
            if (roles == null || role == null) return false;
            for (String r : roles) {
                if (role.equals(r)) return true;
            }
            return false;
        }
        
        public boolean hasPermission(String permission) {
            if (permissions == null || permission == null) return false;
            for (String p : permissions) {
                if (permission.equals(p)) return true;
            }
            return false;
        }
    }
    
    /**
     * Performance context nested class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceContext {
        private Long startTime;
        private Long expectedDurationMs;
        private Double memoryUsageMb;
        private Double cpuUsagePercent;
        private String performanceTier;
        private Map<String, Object> performanceMetrics;
        
        public Long getElapsedTime() {
            return startTime != null ? System.currentTimeMillis() - startTime : 0L;
        }
        
        public boolean hasPerformanceMetrics() {
            return performanceMetrics != null && !performanceMetrics.isEmpty();
        }
    }
}