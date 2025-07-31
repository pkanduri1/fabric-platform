package com.truist.batch.security.jwt;

import com.truist.batch.security.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter
 * 
 * Spring Security filter that processes JWT tokens from HTTP requests.
 * Validates tokens, extracts user information, and sets up the Spring Security
 * context for authenticated requests. Includes comprehensive security checks
 * including token blacklisting and correlation ID tracking.
 * 
 * Security Features:
 * - Bearer token extraction from Authorization header
 * - JWT token validation and claim extraction
 * - Token blacklist verification for logout security
 * - Correlation ID propagation for audit trails
 * - Role and permission-based authority mapping
 * - Request context enhancement with security details
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-01-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenService jwtTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String jwt = extractJwtFromRequest(request);
            
            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateWithJwt(jwt, request, response);
            }
            
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extracts JWT token from the HTTP request Authorization header
     * 
     * @param request HTTP request
     * @return JWT token string or null if not found
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
    
    /**
     * Authenticates user with JWT token and sets up Spring Security context
     * 
     * @param jwt JWT token
     * @param request HTTP request for additional context
     * @param response HTTP response for setting headers
     */
    private void authenticateWithJwt(String jwt, HttpServletRequest request, HttpServletResponse response) {
        try {
            // Check if token is blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
                log.warn("Attempted access with blacklisted token");
                return;
            }
            
            // Validate token and extract claims
            Claims claims = jwtTokenService.validateToken(jwt);
            
            // Validate token type (must be ACCESS token)
            String tokenType = claims.get(JwtTokenService.CLAIM_TOKEN_TYPE, String.class);
            if (!JwtTokenService.TOKEN_TYPE_ACCESS.equals(tokenType)) {
                log.warn("Invalid token type for authentication: {}", tokenType);
                return;
            }
            
            // Extract user information
            String username = claims.getSubject();
            String userId = claims.get(JwtTokenService.CLAIM_USER_ID, String.class);
            String correlationId = claims.get(JwtTokenService.CLAIM_CORRELATION_ID, String.class);
            String sessionId = claims.get(JwtTokenService.CLAIM_SESSION_ID, String.class);
            
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get(JwtTokenService.CLAIM_ROLES, List.class);
            
            @SuppressWarnings("unchecked")
            List<String> permissions = claims.get(JwtTokenService.CLAIM_PERMISSIONS, List.class);
            
            // Convert roles and permissions to Spring Security authorities
            List<SimpleGrantedAuthority> authorities = buildAuthorities(roles, permissions);
            
            // Create authentication token
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(username, null, authorities);
            
            // Set additional details
            FabricAuthenticationDetails details = FabricAuthenticationDetails.builder()
                .userId(userId)
                .username(username)
                .correlationId(correlationId)
                .sessionId(sessionId)
                .roles(roles)
                .permissions(permissions)
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .deviceFingerprint(claims.get(JwtTokenService.CLAIM_DEVICE_FINGERPRINT, String.class))
                .mfaVerified(claims.get(JwtTokenService.CLAIM_MFA_VERIFIED, Boolean.class))
                .build();
            
            authToken.setDetails(details);
            
            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authToken);
            
            // Set correlation ID in response header for tracing
            if (correlationId != null) {
                response.setHeader(CORRELATION_ID_HEADER, correlationId);
            }
            
            log.debug("Successfully authenticated user: {} with correlation ID: {}", username, correlationId);
            
        } catch (JwtTokenException e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during JWT authentication: {}", e.getMessage());
        }
    }
    
    /**
     * Builds Spring Security authorities from roles and permissions
     * 
     * @param roles User roles
     * @param permissions User permissions
     * @return List of granted authorities
     */
    private List<SimpleGrantedAuthority> buildAuthorities(List<String> roles, List<String> permissions) {
        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        
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
    
    /**
     * Extracts client IP address from request headers and connection
     * 
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Determines if this filter should be applied to the request
     * Skip authentication for public endpoints
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Skip authentication for public endpoints
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/actuator/health") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/favicon.ico");
    }
}