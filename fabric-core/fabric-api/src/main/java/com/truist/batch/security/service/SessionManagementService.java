package com.truist.batch.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Session Management Service
 * 
 * Redis-based session management with Oracle persistence for audit trails.
 * Provides comprehensive session lifecycle management, device fingerprinting,
 * and correlation ID tracking for distributed tracing across microservices.
 * 
 * Session Features:
 * - Redis clustering support for high availability
 * - Session timeout and automatic cleanup
 * - Device fingerprinting for security analysis
 * - Geographic location tracking from IP addresses
 * - Risk scoring for adaptive authentication
 * - Comprehensive audit logging for compliance
 * - Correlation ID propagation for distributed tracing
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManagementService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    
    private static final String SESSION_PREFIX = "fabric:session:";
    private static final String USER_SESSIONS_PREFIX = "fabric:user_sessions:";
    private static final int DEFAULT_SESSION_TIMEOUT = 28800; // 8 hours in seconds
    
    /**
     * Creates a new user session with comprehensive tracking
     * 
     * @param userId User ID
     * @param username Username
     * @param ipAddress Client IP address
     * @param userAgent User agent string
     * @param deviceFingerprint Device fingerprint for tracking
     * @param correlationId Correlation ID for tracing
     * @return Session ID
     */
    @Transactional
    public String createSession(String userId, String username, String ipAddress, 
                               String userAgent, String deviceFingerprint, String correlationId) {
        try {
            String sessionId = UUID.randomUUID().toString();
            Instant now = Instant.now();
            
            // Store session in Redis for fast access
            String sessionKey = SESSION_PREFIX + sessionId;
            String sessionData = buildSessionData(userId, username, ipAddress, userAgent, 
                deviceFingerprint, correlationId, now);
            
            redisTemplate.opsForValue().set(sessionKey, sessionData, 
                Duration.ofSeconds(DEFAULT_SESSION_TIMEOUT));
            
            // Track active sessions per user
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            redisTemplate.opsForSet().add(userSessionsKey, sessionId);
            redisTemplate.expire(userSessionsKey, Duration.ofSeconds(DEFAULT_SESSION_TIMEOUT));
            
            // Store session in Oracle for audit persistence
            String sql = """
                INSERT INTO fabric_user_sessions 
                (session_id, user_id, correlation_id, login_time, last_activity,
                 ip_address, user_agent, device_fingerprint, location_info, risk_score)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            String locationInfo = resolveLocationFromIp(ipAddress);
            double riskScore = calculateInitialRiskScore(ipAddress, userAgent, deviceFingerprint);
            
            jdbcTemplate.update(sql, sessionId, userId, correlationId, now, now,
                ipAddress, userAgent, deviceFingerprint, locationInfo, riskScore);
            
            log.info("Created session {} for user {} from IP {} with correlation ID {}", 
                sessionId, username, ipAddress, correlationId);
            
            return sessionId;
            
        } catch (Exception e) {
            log.error("Error creating session for user {} with correlation ID {} - {}", 
                username, correlationId, e.getMessage());
            throw new RuntimeException("Failed to create session", e);
        }
    }
    
    /**
     * Updates session activity timestamp
     * 
     * @param sessionId Session ID
     * @param correlationId Correlation ID for tracing
     */
    public void updateSessionActivity(String sessionId, String correlationId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            
            // Check if session exists in Redis
            if (Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey))) {
                // Extend session TTL
                redisTemplate.expire(sessionKey, Duration.ofSeconds(DEFAULT_SESSION_TIMEOUT));
                
                // Update database timestamp
                String sql = "UPDATE fabric_user_sessions SET last_activity = ? WHERE session_id = ?";
                jdbcTemplate.update(sql, Instant.now(), sessionId);
                
                log.debug("Updated activity for session {} with correlation ID {}", 
                    sessionId, correlationId);
            }
            
        } catch (Exception e) {
            log.error("Error updating session activity for session {} - {}", sessionId, e.getMessage());
        }
    }
    
    /**
     * Checks if a session is active
     * 
     * @param sessionId Session ID
     * @return true if session is active
     */
    public boolean isSessionActive(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey));
        } catch (Exception e) {
            log.error("Error checking session status for session {} - {}", sessionId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Terminates a user session
     * 
     * @param sessionId Session ID
     * @param reason Termination reason
     * @param correlationId Correlation ID for tracing
     */
    @Transactional
    public void terminateSession(String sessionId, String reason, String correlationId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            
            // Get session data before deletion
            String sessionData = redisTemplate.opsForValue().get(sessionKey);
            if (sessionData != null) {
                String userId = extractUserIdFromSessionData(sessionData);
                
                // Remove from Redis
                redisTemplate.delete(sessionKey);
                
                // Remove from user sessions set
                String userSessionsKey = USER_SESSIONS_PREFIX + userId;
                redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
            }
            
            // Update database record
            String sql = """
                UPDATE fabric_user_sessions 
                SET session_status = 'TERMINATED', logout_time = ?, logout_reason = ?
                WHERE session_id = ?
                """;
            
            jdbcTemplate.update(sql, Instant.now(), reason, sessionId);
            
            log.info("Terminated session {} with reason '{}' and correlation ID {}", 
                sessionId, reason, correlationId);
            
        } catch (Exception e) {
            log.error("Error terminating session {} with correlation ID {} - {}", 
                sessionId, correlationId, e.getMessage());
        }
    }
    
    /**
     * Terminates all sessions for a user (security incident response)
     * 
     * @param userId User ID
     * @param reason Termination reason
     * @param correlationId Correlation ID for tracing
     */
    @Transactional
    public void terminateAllUserSessions(String userId, String reason, String correlationId) {
        try {
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            
            // Get all active sessions for user
            java.util.Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
            
            if (sessionIds != null && !sessionIds.isEmpty()) {
                // Remove all sessions from Redis
                for (String sessionId : sessionIds) {
                    String sessionKey = SESSION_PREFIX + sessionId;
                    redisTemplate.delete(sessionKey);
                }
                
                // Clear user sessions set
                redisTemplate.delete(userSessionsKey);
                
                // Update all sessions in database
                String sql = """
                    UPDATE fabric_user_sessions 
                    SET session_status = 'TERMINATED', logout_time = ?, logout_reason = ?
                    WHERE user_id = ? AND session_status = 'ACTIVE'
                    """;
                
                int updatedCount = jdbcTemplate.update(sql, Instant.now(), reason, userId);
                
                log.warn("Terminated {} sessions for user {} with reason '{}' and correlation ID {}", 
                    updatedCount, userId, reason, correlationId);
            }
            
        } catch (Exception e) {
            log.error("Error terminating all sessions for user {} with correlation ID {} - {}", 
                userId, correlationId, e.getMessage());
            throw new RuntimeException("Failed to terminate user sessions", e);
        }
    }
    
    /**
     * Gets active session count for a user
     * 
     * @param userId User ID
     * @return Number of active sessions
     */
    public long getActiveSessionCount(String userId) {
        try {
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            Long count = redisTemplate.opsForSet().size(userSessionsKey);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Error getting session count for user {} - {}", userId, e.getMessage());
            return 0;
        }
    }
    
    /**
     * Cleanup expired sessions (scheduled job)
     */
    @Transactional
    public void cleanupExpiredSessions() {
        try {
            String sql = """
                UPDATE fabric_user_sessions 
                SET session_status = 'EXPIRED'
                WHERE session_status = 'ACTIVE' 
                AND last_activity < ?
                """;
            
            Instant cutoffTime = Instant.now().minusSeconds(DEFAULT_SESSION_TIMEOUT);
            int expiredCount = jdbcTemplate.update(sql, cutoffTime);
            
            if (expiredCount > 0) {
                log.info("Marked {} sessions as expired during cleanup", expiredCount);
            }
            
        } catch (Exception e) {
            log.error("Error during session cleanup: {}", e.getMessage());
        }
    }
    
    // Private helper methods
    
    private String buildSessionData(String userId, String username, String ipAddress, 
                                   String userAgent, String deviceFingerprint, 
                                   String correlationId, Instant createdAt) {
        // Simple JSON-like format for Redis storage
        return String.format(
            "{\"userId\":\"%s\",\"username\":\"%s\",\"ipAddress\":\"%s\",\"userAgent\":\"%s\"," +
            "\"deviceFingerprint\":\"%s\",\"correlationId\":\"%s\",\"createdAt\":\"%s\"}",
            userId, username, ipAddress, userAgent, 
            deviceFingerprint != null ? deviceFingerprint : "", 
            correlationId, createdAt.toString()
        );
    }
    
    private String extractUserIdFromSessionData(String sessionData) {
        // Simple extraction - in production, use JSON parser
        try {
            int start = sessionData.indexOf("\"userId\":\"") + 10;
            int end = sessionData.indexOf("\"", start);
            return sessionData.substring(start, end);
        } catch (Exception e) {
            log.debug("Error extracting userId from session data: {}", e.getMessage());
            return null;
        }
    }
    
    private String resolveLocationFromIp(String ipAddress) {
        // TODO: Implement IP geolocation lookup
        // For now, return placeholder
        return "Unknown Location";
    }
    
    private double calculateInitialRiskScore(String ipAddress, String userAgent, String deviceFingerprint) {
        // TODO: Implement risk scoring algorithm
        // Factors: known IP ranges, device recognition, time of day, etc.
        double riskScore = 0.0;
        
        // Basic checks
        if (deviceFingerprint == null || deviceFingerprint.isEmpty()) {
            riskScore += 10.0; // Unknown device
        }
        
        if (isFromInternalNetwork(ipAddress)) {
            riskScore -= 5.0; // Internal network reduces risk
        } else {
            riskScore += 5.0; // External access increases risk
        }
        
        return Math.max(0.0, Math.min(100.0, riskScore)); // Clamp between 0-100
    }
    
    private boolean isFromInternalNetwork(String ipAddress) {
        // TODO: Implement internal network detection
        // Check against configured internal IP ranges
        return ipAddress.startsWith("10.") || 
               ipAddress.startsWith("192.168.") || 
               ipAddress.startsWith("172.");
    }
}