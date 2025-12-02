package com.fabric.batch.security.ldap;

import com.fabric.batch.security.service.UserSecurityService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import javax.naming.directory.DirContext;
import java.util.UUID;

/**
 * LDAP Authentication Provider
 * 
 * Enterprise LDAP authentication provider with circuit breaker pattern and fallback mechanisms.
 * Integrates with Active Directory/LDAP for primary authentication while maintaining
 * resilience through circuit breaker patterns and comprehensive error handling.
 * 
 * Security Features:
 * - Circuit breaker pattern for LDAP service resilience
 * - Secure credential validation against enterprise directory
 * - User attribute extraction and mapping
 * - Comprehensive audit logging for authentication events
 * - Fallback mechanisms for service degradation
 * - Connection pooling and timeout management
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!local")  // Exclude from local profile to avoid LDAP dependencies
@ConditionalOnProperty(name = "fabric.security.ldap.enabled", havingValue = "true", matchIfMissing = false)
public class LdapAuthenticationProvider implements AuthenticationProvider {
    
    private final LdapTemplate ldapTemplate;
    private final UserSecurityService userSecurityService;
    
    @Value("${fabric.security.ldap.user-search-base:ou=users,dc=truist,dc=com}")
    private String userSearchBase;
    
    @Value("${fabric.security.ldap.user-search-filter:uid={0}}")
    private String userSearchFilter;
    
    @Value("${fabric.security.ldap.group-search-base:ou=groups,dc=truist,dc=com}")
    private String groupSearchBase;
    
    @Value("${fabric.security.ldap.enabled:true}")
    private boolean ldapEnabled;
    
    private static final String CIRCUIT_BREAKER_NAME = "ldapAuthentication";
    private static final String RETRY_NAME = "ldapRetry";
    
    /**
     * Authenticates user credentials against LDAP/Active Directory
     * 
     * @param authentication Authentication request containing username and password
     * @return Authenticated authentication token with user details
     * @throws AuthenticationException if authentication fails
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackAuthentication")
    @Retry(name = RETRY_NAME)
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();
        String correlationId = UUID.randomUUID().toString();
        
        log.debug("Starting LDAP authentication for user: {} with correlation ID: {}", username, correlationId);
        
        if (!ldapEnabled) {
            log.warn("LDAP authentication is disabled, falling back to local authentication");
            return fallbackAuthentication(authentication, new RuntimeException("LDAP disabled"));
        }
        
        try {
            // Validate credentials against LDAP
            LdapUserDetails ldapUser = authenticateWithLdap(username, password, correlationId);
            
            // Enhance with local user data and permissions
            FabricUserDetails userDetails = userSecurityService.enhanceWithLocalUserData(ldapUser, correlationId);
            
            // Create successful authentication token
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
            
            log.info("LDAP authentication successful for user: {} with correlation ID: {}", username, correlationId);
            
            return authToken;
            
        } catch (Exception e) {
            log.error("LDAP authentication failed for user: {} with correlation ID: {} - {}", 
                username, correlationId, e.getMessage());
            throw new BadCredentialsException("Authentication failed", e);
        }
    }
    
    /**
     * Fallback authentication method when LDAP is unavailable
     * Used by circuit breaker pattern for service degradation
     */
    public Authentication fallbackAuthentication(Authentication authentication, Exception ex) {
        String username = authentication.getName();
        String correlationId = UUID.randomUUID().toString();
        
        log.warn("LDAP authentication circuit breaker activated for user: {} - falling back to local authentication", username);
        
        try {
            // Attempt local database authentication as fallback
            FabricUserDetails userDetails = userSecurityService.authenticateLocalUser(username, 
                (String) authentication.getCredentials(), correlationId);
            
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            
            log.info("Fallback authentication successful for user: {} with correlation ID: {}", username, correlationId);
            
            return authToken;
            
        } catch (Exception fallbackException) {
            log.error("Fallback authentication also failed for user: {} - {}", username, fallbackException.getMessage());
            throw new BadCredentialsException("Authentication failed - both LDAP and fallback unavailable");
        }
    }
    
    /**
     * Authenticates user credentials directly against LDAP server
     * 
     * @param username User identifier
     * @param password User password
     * @param correlationId Correlation ID for tracing
     * @return LDAP user details
     */
    private LdapUserDetails authenticateWithLdap(String username, String password, String correlationId) {
        try {
            // Build LDAP search filter
            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectClass", "person"));
            filter.and(new EqualsFilter("uid", username));
            
            // Perform LDAP bind authentication
            DirContext context = null;
            try {
                // Get user DN for bind authentication
                String userDn = findUserDn(username);
                
                if (userDn == null) {
                    throw new BadCredentialsException("User not found in LDAP directory: " + username);
                }
                
                // Attempt to bind with user credentials
                LdapContextSource contextSource = (LdapContextSource) ldapTemplate.getContextSource();
                context = contextSource.getContext(userDn, password);
                
                // If bind successful, extract user attributes
                return extractUserAttributes(username, userDn, correlationId);
                
            } finally {
                if (context != null) {
                    try {
                        context.close();
                    } catch (Exception e) {
                        log.debug("Error closing LDAP context: {}", e.getMessage());
                    }
                }
            }
            
        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("LDAP authentication error for user: {} with correlation ID: {} - {}", 
                username, correlationId, e.getMessage());
            throw new RuntimeException("LDAP authentication failed", e);
        }
    }
    
    /**
     * Finds user Distinguished Name (DN) in LDAP directory
     * 
     * @param username Username to search for
     * @return User DN or null if not found
     */
    private String findUserDn(String username) {
        try {
            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectClass", "person"));
            filter.and(new EqualsFilter("uid", username));
            
            return ldapTemplate.search(userSearchBase, filter.encode(), 
                (Object ctx) -> ((javax.naming.directory.DirContext) ctx).getNameInNamespace()).stream()
                .findFirst()
                .map(Object::toString)
                .orElse(null);
                
        } catch (Exception e) {
            log.error("Error finding user DN for username: {} - {}", username, e.getMessage());
            return null;
        }
    }
    
    /**
     * Extracts user attributes from LDAP directory
     * 
     * @param username Username
     * @param userDn User Distinguished Name
     * @param correlationId Correlation ID for tracing
     * @return LDAP user details
     */
    private LdapUserDetails extractUserAttributes(String username, String userDn, String correlationId) {
        try {
            return ldapTemplate.lookup(userDn, new String[]{"cn", "mail", "givenName", "sn", "department", "title"}, 
                (Object ctx) -> {
                    javax.naming.directory.Attributes attrs = (javax.naming.directory.Attributes) ctx;
                    
                    return LdapUserDetails.builder()
                        .username(username)
                        .distinguishedName(userDn)
                        .commonName(getAttributeValue(attrs, "cn"))
                        .email(getAttributeValue(attrs, "mail"))
                        .firstName(getAttributeValue(attrs, "givenName"))
                        .lastName(getAttributeValue(attrs, "sn"))
                        .department(getAttributeValue(attrs, "department"))
                        .title(getAttributeValue(attrs, "title"))
                        .correlationId(correlationId)
                        .build();
                });
                
        } catch (Exception e) {
            log.error("Error extracting LDAP attributes for user: {} - {}", username, e.getMessage());
            throw new RuntimeException("Failed to extract user attributes from LDAP", e);
        }
    }
    
    /**
     * Safely extracts attribute value from LDAP attributes
     * 
     * @param attrs LDAP attributes
     * @param attributeName Attribute name to extract
     * @return Attribute value or null if not found
     */
    private String getAttributeValue(javax.naming.directory.Attributes attrs, String attributeName) {
        try {
            javax.naming.directory.Attribute attr = attrs.get(attributeName);
            return attr != null ? (String) attr.get() : null;
        } catch (Exception e) {
            log.debug("Error getting LDAP attribute {}: {}", attributeName, e.getMessage());
            return null;
        }
    }
    
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}