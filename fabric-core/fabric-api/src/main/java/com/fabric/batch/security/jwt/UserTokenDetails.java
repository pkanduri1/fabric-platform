package com.fabric.batch.security.jwt;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * User Token Details DTO
 * 
 * Comprehensive user information container for JWT token generation.
 * Contains all necessary claims for access and refresh tokens including
 * security context, role-based permissions, and audit tracking data.
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */
@Data
@Builder
public class UserTokenDetails {
    
    // Core user identity
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    
    // Authorization context
    private List<String> roles;
    private List<String> permissions;
    
    // Session context
    private String sessionId;
    private String correlationId;
    
    // Security context
    private String deviceFingerprint;
    private String ipAddress;
    private String userAgent;
    private boolean mfaVerified;
    
    // Additional claims
    private String department;
    private String title;
    
    /**
     * Gets the full name of the user
     * 
     * @return Formatted full name
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return username;
    }
    
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
     * Checks if user has any of the specified roles
     * 
     * @param roleList List of roles to check
     * @return true if user has at least one of the roles
     */
    public boolean hasAnyRole(List<String> roleList) {
        if (roles == null || roleList == null) {
            return false;
        }
        return roles.stream().anyMatch(roleList::contains);
    }
    
    /**
     * Checks if user has any of the specified permissions
     * 
     * @param permissionList List of permissions to check
     * @return true if user has at least one of the permissions
     */
    public boolean hasAnyPermission(List<String> permissionList) {
        if (permissions == null || permissionList == null) {
            return false;
        }
        return permissions.stream().anyMatch(permissionList::contains);
    }
}