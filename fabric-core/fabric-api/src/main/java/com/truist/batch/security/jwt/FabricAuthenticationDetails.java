package com.truist.batch.security.jwt;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Fabric Authentication Details
 * 
 * Extended authentication details for Spring Security context.
 * Contains comprehensive user session information, security context,
 * and audit tracking data for enhanced security and compliance.
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */
@Data
@Builder
public class FabricAuthenticationDetails {
    
    // Core user identity
    private String userId;
    private String username;
    
    // Session tracking
    private String correlationId;
    private String sessionId;
    
    // Authorization context
    private List<String> roles;
    private List<String> permissions;
    
    // Security context
    private String ipAddress;
    private String userAgent;
    private String deviceFingerprint;
    private Boolean mfaVerified;
    
    /**
     * Checks if user has a specific role
     * 
     * @param role Role to check
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
    
    /**
     * Checks if user has a specific permission
     * 
     * @param permission Permission to check
     * @return true if user has the permission
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
    
    /**
     * Checks if MFA has been verified for this session
     * 
     * @return true if MFA verified
     */
    public boolean isMfaVerified() {
        return Boolean.TRUE.equals(mfaVerified);
    }
}