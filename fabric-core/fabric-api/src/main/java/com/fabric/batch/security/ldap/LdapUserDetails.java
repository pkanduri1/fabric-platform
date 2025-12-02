package com.fabric.batch.security.ldap;

import lombok.Builder;
import lombok.Data;

/**
 * LDAP User Details
 * 
 * Data transfer object containing user information extracted from LDAP/Active Directory.
 * Represents the core user attributes retrieved during LDAP authentication process.
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */
@Data
@Builder
public class LdapUserDetails {
    
    // Core identity
    private String username;
    private String distinguishedName;
    private String commonName;
    
    // Contact information
    private String email;
    private String firstName;
    private String lastName;
    
    // Organizational information
    private String department;
    private String title;
    
    // Audit tracking
    private String correlationId;
    
    /**
     * Gets the full name of the user
     * 
     * @return Formatted full name
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (commonName != null) {
            return commonName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return username;
    }
}