package com.truist.batch.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Security Audit Service
 * 
 * Provides security audit logging functionality for authentication,
 * authorization, and other security events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityAuditService {
    
    /**
     * Log security event
     */
    public void logSecurityEvent(String eventType, String userId, String sessionId, String details) {
        log.info("🔒 Security Event: type={}, user={}, session={}, details={}", 
                eventType, userId, sessionId, details);
    }
    
    /**
     * Log security event with additional data
     */
    public void logSecurityEvent(String eventType, String userId, String sessionId, String details, Map<String, Object> additionalData) {
        if (additionalData != null && !additionalData.isEmpty()) {
            log.info("🔒 Security Event: type={}, user={}, session={}, details={}, data={}", 
                    eventType, userId, sessionId, details, additionalData);
        } else {
            logSecurityEvent(eventType, userId, sessionId, details);
        }
    }
    
    /**
     * Log authentication event
     */
    public void logAuthenticationEvent(String userId, String eventType, boolean success) {
        log.info("🔐 Authentication Event: user={}, type={}, success={}", 
                userId, eventType, success);
    }
    
    /**
     * Log WebSocket security event
     */
    public void logWebSocketSecurityEvent(String eventType, String sessionId, String userId, String description) {
        log.info("🔒 WebSocket Security Event: type={}, session={}, user={}, description={}", 
                eventType, sessionId, userId, description);
    }
    
    /**
     * Audit successful login
     */
    public void auditLoginSuccess(String userId, String sessionId, String clientIp, String userAgent, String authenticationType, String ldapServer) {
        log.info("🔐 Login Success Audit: user={}, session={}, ip={}, userAgent={}, authType={}, ldapServer={}", 
                userId, sessionId, clientIp, userAgent, authenticationType, ldapServer);
    }
    
    /**
     * Audit failed login
     */
    public void auditLoginFailure(String userId, String sessionId, String clientIp, String userAgent, String reason, int attempts) {
        log.warn("🔒 Login Failure Audit: user={}, session={}, ip={}, userAgent={}, reason={}, attempts={}", 
                userId, sessionId, clientIp, userAgent, reason, attempts);
    }
    
    /**
     * Audit logout
     */
    public void auditLogout(String userId, String sessionId, String clientIp, String userAgent, String reason) {
        log.info("🚪 Logout Audit: user={}, session={}, ip={}, userAgent={}, reason={}", 
                userId, sessionId, clientIp, userAgent, reason);
    }
    
    /**
     * Audit role assignment
     */
    public void auditRoleAssignment(String userId, String assignedRole, String assignedBy, String reason) {
        log.info("👥 Role Assignment Audit: user={}, role={}, assignedBy={}, reason={}", 
                userId, assignedRole, assignedBy, reason);
    }
    
    /**
     * Audit role revocation
     */
    public void auditRoleRevocation(String userId, String revokedRole, String revokedBy, String reason, String sessionId) {
        log.info("🚫 Role Revocation Audit: user={}, role={}, revokedBy={}, reason={}, session={}", 
                userId, revokedRole, revokedBy, reason, sessionId);
    }
}