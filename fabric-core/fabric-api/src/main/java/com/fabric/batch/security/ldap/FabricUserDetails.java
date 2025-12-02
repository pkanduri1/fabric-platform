package com.fabric.batch.security.ldap;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fabric User Details
 * 
 * Spring Security UserDetails implementation for Fabric Platform users.
 * Combines LDAP authentication data with local role/permission information
 * to provide comprehensive user security context.
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */
@Data
@Builder
public class FabricUserDetails implements UserDetails {
    
    // Core identity
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    
    // LDAP attributes
    private String distinguishedName;
    private String department;
    private String title;
    
    // Authorization
    private List<String> roles;
    private List<String> permissions;
    
    // Account status
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean enabled;
    
    // Security context
    private String correlationId;
    private boolean mfaEnabled;
    private boolean mfaVerified;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        
        // Add role authorities with ROLE_ prefix
        if (roles != null) {
            authorities.addAll(roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList()));
        }
        
        // Add permission authorities
        if (permissions != null) {
            authorities.addAll(permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));
        }
        
        return authorities;
    }
    
    @Override
    public String getPassword() {
        // Password is not stored locally for LDAP users
        return null;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
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
}