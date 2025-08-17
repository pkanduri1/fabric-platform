package com.truist.batch.security.websocket;

import com.truist.batch.config.WebSocketMonitoringProperties;
import com.truist.batch.security.jwt.JwtTokenService;
import com.truist.batch.security.service.TokenBlacklistService;
import com.truist.batch.service.SecurityAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * US008: Comprehensive Unit Tests for SecurityHandshakeInterceptor
 * 
 * Tests banking-grade WebSocket security validation including:
 * - JWT authentication and authorization validation
 * - Origin validation and CSRF protection
 * - IP-based rate limiting and connection limits
 * - Security audit logging and SOX compliance
 * - Token blacklist checking and rotation handling
 * - Comprehensive error handling and edge cases
 * 
 * Target: 95%+ code coverage with comprehensive security testing
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityHandshakeInterceptor Unit Tests")
class SecurityHandshakeInterceptorTest {

    @Mock
    private JwtTokenService mockJwtTokenService;
    
    @Mock
    private TokenBlacklistService mockTokenBlacklistService;
    
    @Mock
    private SecurityAuditService mockSecurityAuditService;
    
    @Mock
    private WebSocketMonitoringProperties mockMonitoringProperties;
    
    @Mock
    private RedisTemplate<String, Object> mockRedisTemplate;
    
    @Mock
    private ServerHttpRequest mockRequest;
    
    @Mock
    private ServerHttpResponse mockResponse;
    
    @Mock
    private WebSocketHandler mockWebSocketHandler;
    
    @Mock
    private HttpHeaders mockHttpHeaders;
    
    @Mock
    private ValueOperations<String, Object> mockValueOperations;

    private SecurityHandshakeInterceptor handshakeInterceptor;
    private Map<String, Object> sessionAttributes;

    @BeforeEach
    void setUp() {
        handshakeInterceptor = new SecurityHandshakeInterceptor(
                mockJwtTokenService,
                mockTokenBlacklistService,
                mockSecurityAuditService,
                mockMonitoringProperties,
                mockRedisTemplate
        );
        
        sessionAttributes = new HashMap<>();
        
        // Set up default mock behaviors
        when(mockRequest.getHeaders()).thenReturn(mockHttpHeaders);
        when(mockRequest.getRemoteAddress()).thenReturn(new InetSocketAddress("192.168.1.100", 8080));
        when(mockHttpHeaders.getUpgrade()).thenReturn(List.of("websocket"));
        when(mockHttpHeaders.getConnection()).thenReturn(List.of("Upgrade"));
        when(mockHttpHeaders.getOrigin()).thenReturn("https://fabric-platform.truist.com");
        when(mockRequest.getMethod()).thenReturn(org.springframework.http.HttpMethod.GET);
        
        when(mockMonitoringProperties.isSecurityEnabled()).thenReturn(true);
        when(mockMonitoringProperties.getAllowedOrigins()).thenReturn(List.of(
                "https://*.truist.com",
                "https://fabric-platform.truist.com"
        ));
        when(mockMonitoringProperties.getRateLimitConnectionsPerMinute()).thenReturn(10);
        when(mockMonitoringProperties.getMaxConnectionsPerIp()).thenReturn(5);
        when(mockMonitoringProperties.getMaxConnectionsPerUser()).thenReturn(3);
        when(mockMonitoringProperties.getRedisKeyPrefix()).thenReturn("fabric:websocket");
        
        when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOperations);
    }

    // ============================================================================
    // SUCCESSFUL HANDSHAKE TESTS
    // ============================================================================

    @Nested
    @DisplayName("Successful Handshake Scenarios")
    class SuccessfulHandshakeTests {

        @Test
        @DisplayName("Should approve valid handshake with all security checks passed")
        void shouldApproveValidHandshakeWithAllSecurityChecksPassed() {
            // Given
            setupValidJwtToken();
            setupValidRateLimits();
            setupValidConnectionLimits();
            setupValidCSRFToken();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isTrue();
            assertThat(sessionAttributes)
                    .containsKeys("userId", "userRoles", "jwtToken", "clientIp", "correlationId", 
                               "handshakeTimestamp", "securityValidated");
            assertThat(sessionAttributes.get("userId")).isEqualTo("test-user");
            assertThat(sessionAttributes.get("securityValidated")).isEqualTo(true);
            
            // Verify audit logging
            verify(mockSecurityAuditService).logSecurityEvent(
                    eq("WEBSOCKET_HANDSHAKE_SUCCESS"),
                    eq("test-user"),
                    anyString(),
                    eq("WebSocket handshake completed successfully"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("Should set proper security headers in response")
        void shouldSetProperSecurityHeadersInResponse() {
            // Given
            setupValidJwtToken();
            setupValidRateLimits();
            setupValidConnectionLimits();
            setupValidCSRFToken();
            
            // When
            handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            verify(mockResponse.getHeaders()).add("X-Content-Type-Options", "nosniff");
            verify(mockResponse.getHeaders()).add("X-Frame-Options", "DENY");
            verify(mockResponse.getHeaders()).add("X-XSS-Protection", "1; mode=block");
            verify(mockResponse.getHeaders()).add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            verify(mockResponse.getHeaders()).add("Content-Security-Policy", "default-src 'self'");
            verify(mockResponse.getHeaders()).add("Referrer-Policy", "strict-origin-when-cross-origin");
        }

        @Test
        @DisplayName("Should handle development mode with reduced security")
        void shouldHandleDevelopmentModeWithReducedSecurity() {
            // Given
            when(mockMonitoringProperties.isSecurityEnabled()).thenReturn(false);
            when(mockHttpHeaders.getOrigin()).thenReturn(null); // Missing origin in dev mode
            setupValidJwtToken();
            setupValidRateLimits();
            setupValidConnectionLimits();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isTrue();
        }
    }

    // ============================================================================
    // BASIC REQUEST VALIDATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Basic Request Validation")
    class BasicRequestValidationTests {

        @Test
        @DisplayName("Should reject request with missing upgrade headers")
        void shouldRejectRequestWithMissingUpgradeHeaders() {
            // Given
            when(mockHttpHeaders.getUpgrade()).thenReturn(null);
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isFalse();
            verify(mockResponse).setStatusCode(HttpStatus.BAD_REQUEST);
            verify(mockSecurityAuditService).logSecurityEvent(
                    eq("WEBSOCKET_SECURITY_VIOLATION"),
                    eq("UNKNOWN"),
                    anyString(),
                    eq("WebSocket security violation: INVALID_UPGRADE_REQUEST"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("Should reject request with missing connection headers")
        void shouldRejectRequestWithMissingConnectionHeaders() {
            // Given
            when(mockHttpHeaders.getConnection()).thenReturn(null);
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isFalse();
            verify(mockResponse).setStatusCode(HttpStatus.BAD_REQUEST);
        }

        @ParameterizedTest
        @ValueSource(strings = {"POST", "PUT", "DELETE", "PATCH"})
        @DisplayName("Should reject non-GET HTTP methods")
        void shouldRejectNonGetHttpMethods(String httpMethod) {
            // Given
            when(mockRequest.getMethod()).thenReturn(org.springframework.http.HttpMethod.valueOf(httpMethod));
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isFalse();
            verify(mockResponse).setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
        }
    }

    // ============================================================================
    // ORIGIN VALIDATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Origin Validation Tests")
    class OriginValidationTests {

        @Test
        @DisplayName("Should reject request with missing origin header in production")
        void shouldRejectRequestWithMissingOriginHeaderInProduction() {
            // Given
            when(mockHttpHeaders.getOrigin()).thenReturn(null);
            when(mockMonitoringProperties.isSecurityEnabled()).thenReturn(true);
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isFalse();
            verify(mockResponse).setStatusCode(HttpStatus.FORBIDDEN);
            verify(mockSecurityAuditService).logSecurityEvent(
                    eq("WEBSOCKET_SECURITY_VIOLATION"),
                    eq("UNKNOWN"),
                    anyString(),
                    eq("WebSocket security violation: MISSING_ORIGIN"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("Should allow missing origin header in development mode")
        void shouldAllowMissingOriginHeaderInDevelopmentMode() {
            // Given
            when(mockHttpHeaders.getOrigin()).thenReturn(null);
            when(mockMonitoringProperties.isSecurityEnabled()).thenReturn(false);
            setupValidJwtToken();
            setupValidRateLimits();
            setupValidConnectionLimits();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject request from unauthorized origin")
        void shouldRejectRequestFromUnauthorizedOrigin() {
            // Given
            when(mockHttpHeaders.getOrigin()).thenReturn("https://malicious-site.com");
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isFalse();
            verify(mockResponse).setStatusCode(HttpStatus.FORBIDDEN);
            verify(mockSecurityAuditService).logSecurityEvent(
                    eq("WEBSOCKET_SECURITY_VIOLATION"),
                    eq("UNKNOWN"),
                    anyString(),
                    eq("WebSocket security violation: INVALID_ORIGIN"),
                    any(Map.class)
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "https://app.truist.com",
                "https://fabric-platform.truist.com",
                "https://subdomain.truist.com"
        })
        @DisplayName("Should accept requests from allowed origins")
        void shouldAcceptRequestsFromAllowedOrigins(String origin) {
            // Given
            when(mockHttpHeaders.getOrigin()).thenReturn(origin);
            setupValidJwtToken();
            setupValidRateLimits();
            setupValidConnectionLimits();
            setupValidCSRFToken();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isTrue();
        }
    }

    // ============================================================================
    // RATE LIMITING TESTS
    // ============================================================================

    @Nested
    @DisplayName("Rate Limiting Tests")
    class RateLimitingTests {

        @Test
        @DisplayName("Should reject request when IP rate limit exceeded")
        void shouldRejectRequestWhenIpRateLimitExceeded() {
            // Given
            when(mockValueOperations.increment(anyString())).thenReturn(15L); // Exceeds limit of 10
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isFalse();
            verify(mockResponse).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            verify(mockResponse.getHeaders()).add("Retry-After", "60");
            verify(mockSecurityAuditService).logSecurityEvent(
                    eq("WEBSOCKET_SECURITY_VIOLATION"),
                    eq("UNKNOWN"),
                    anyString(),
                    eq("WebSocket security violation: IP_RATE_LIMIT_EXCEEDED"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("Should allow request within rate limit")
        void shouldAllowRequestWithinRateLimit() {
            // Given
            when(mockValueOperations.increment(anyString())).thenReturn(5L); // Within limit of 10
            setupValidJwtToken();
            setupValidConnectionLimits();
            setupValidCSRFToken();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should fallback to local rate limiting when Redis fails")
        void shouldFallbackToLocalRateLimitingWhenRedisFails() {
            // Given
            when(mockRedisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection failed"));
            setupValidJwtToken();
            setupValidConnectionLimits();
            setupValidCSRFToken();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isTrue(); // Should fallback gracefully
        }
    }

    // ============================================================================
    // JWT AUTHENTICATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("JWT Authentication Tests")
    class JwtAuthenticationTests {

        @Test
        @DisplayName("Should reject request with missing JWT token")
        void shouldRejectRequestWithMissingJwtToken() {
            // Given - no Authorization header or token parameter
            setupValidRateLimits();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isFalse();
            verify(mockResponse).setStatusCode(HttpStatus.UNAUTHORIZED);
            verify(mockSecurityAuditService).logSecurityEvent(
                    eq("WEBSOCKET_SECURITY_VIOLATION"),
                    eq("UNKNOWN"),
                    anyString(),
                    eq("WebSocket security violation: MISSING_JWT_TOKEN"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("Should reject request with invalid JWT token")
        void shouldRejectRequestWithInvalidJwtToken() {
            // Given
            when(mockHttpHeaders.getFirst("Authorization")).thenReturn("Bearer invalid-token");
            when(mockJwtTokenService.validateToken("invalid-token")).thenReturn(false);
            setupValidRateLimits();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isFalse();
            verify(mockResponse).setStatusCode(HttpStatus.UNAUTHORIZED);
            verify(mockSecurityAuditService).logSecurityEvent(
                    eq("WEBSOCKET_SECURITY_VIOLATION"),
                    eq("UNKNOWN"),
                    anyString(),
                    eq("WebSocket security violation: INVALID_JWT_TOKEN"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("Should reject request with blacklisted JWT token")
        void shouldRejectRequestWithBlacklistedJwtToken() {
            // Given
            when(mockHttpHeaders.getFirst("Authorization")).thenReturn("Bearer valid-but-blacklisted-token");
            when(mockJwtTokenService.validateToken("valid-but-blacklisted-token")).thenReturn(true);
            when(mockTokenBlacklistService.isBlacklisted("valid-but-blacklisted-token")).thenReturn(true);
            setupValidRateLimits();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isFalse();
            verify(mockResponse).setStatusCode(HttpStatus.UNAUTHORIZED);
            verify(mockSecurityAuditService).logSecurityEvent(
                    eq("WEBSOCKET_SECURITY_VIOLATION"),
                    eq("UNKNOWN"),
                    anyString(),
                    eq("WebSocket security violation: BLACKLISTED_JWT_TOKEN"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("Should reject request with insufficient user permissions")
        void shouldRejectRequestWithInsufficientUserPermissions() {
            // Given
            setupValidJwtTokenWithInsufficientRoles();
            setupValidRateLimits();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isFalse();
            verify(mockResponse).setStatusCode(HttpStatus.FORBIDDEN);
            verify(mockSecurityAuditService).logSecurityEvent(
                    eq("WEBSOCKET_SECURITY_VIOLATION"),
                    eq("UNKNOWN"),
                    anyString(),
                    eq("WebSocket security violation: INSUFFICIENT_PERMISSIONS"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("Should extract JWT token from Authorization header")
        void shouldExtractJwtTokenFromAuthorizationHeader() {
            // Given
            when(mockHttpHeaders.getFirst("Authorization")).thenReturn("Bearer valid-jwt-token");
            setupValidJwtTokenBehavior("valid-jwt-token");
            setupValidRateLimits();
            setupValidConnectionLimits();
            setupValidCSRFToken();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isTrue();
            assertThat(sessionAttributes.get("jwtToken")).isEqualTo("valid-jwt-token");
        }

        @Test
        @DisplayName("Should extract JWT token from query parameter as fallback")
        void shouldExtractJwtTokenFromQueryParameterAsFallback() {
            // Given
            var mockServletRequest = mock(HttpServletRequest.class);
            var mockServletServerHttpRequest = mock(ServletServerHttpRequest.class);
            
            when(mockServletServerHttpRequest.getServletRequest()).thenReturn(mockServletRequest);
            when(mockServletRequest.getParameter("token")).thenReturn("valid-jwt-token");
            
            setupValidJwtTokenBehavior("valid-jwt-token");
            setupValidRateLimits();
            setupValidConnectionLimits();
            setupValidCSRFToken();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockServletServerHttpRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isTrue();
            assertThat(sessionAttributes.get("jwtToken")).isEqualTo("valid-jwt-token");
        }
    }

    // ============================================================================
    // CONNECTION LIMITS TESTS
    // ============================================================================

    @Nested
    @DisplayName("Connection Limits Tests")
    class ConnectionLimitsTests {

        @Test
        @DisplayName("Should reject request when IP connection limit exceeded")
        void shouldRejectRequestWhenIpConnectionLimitExceeded() {
            // Given
            setupValidJwtToken();
            setupValidRateLimits();
            when(mockValueOperations.get("fabric:websocket:connections:ip:192.168.1.100")).thenReturn(10); // Exceeds limit of 5
            setupValidCSRFToken();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isFalse();
            verify(mockResponse).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("Should reject request when user connection limit exceeded")
        void shouldRejectRequestWhenUserConnectionLimitExceeded() {
            // Given
            setupValidJwtToken();
            setupValidRateLimits();
            when(mockValueOperations.get("fabric:websocket:connections:ip:192.168.1.100")).thenReturn(2); // Within IP limit
            when(mockValueOperations.get("fabric:websocket:connections:user:test-user")).thenReturn(5); // Exceeds user limit of 3
            setupValidCSRFToken();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isFalse();
            verify(mockResponse).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    // ============================================================================
    // CSRF VALIDATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("CSRF Validation Tests")
    class CsrfValidationTests {

        @Test
        @DisplayName("Should reject request with missing CSRF token in production")
        void shouldRejectRequestWithMissingCsrfTokenInProduction() {
            // Given
            setupValidJwtToken();
            setupValidRateLimits();
            setupValidConnectionLimits();
            // No CSRF token header
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isFalse();
            verify(mockResponse).setStatusCode(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should reject request with invalid CSRF token format")
        void shouldRejectRequestWithInvalidCsrfTokenFormat() {
            // Given
            setupValidJwtToken();
            setupValidRateLimits();
            setupValidConnectionLimits();
            when(mockHttpHeaders.getFirst("X-CSRF-Token")).thenReturn("invalid-token"); // Too short
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isFalse();
            verify(mockResponse).setStatusCode(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should skip CSRF validation in development mode")
        void shouldSkipCsrfValidationInDevelopmentMode() {
            // Given
            when(mockMonitoringProperties.isSecurityEnabled()).thenReturn(false);
            setupValidJwtToken();
            setupValidRateLimits();
            setupValidConnectionLimits();
            // No CSRF token
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isTrue();
        }
    }

    // ============================================================================
    // AFTER HANDSHAKE TESTS
    // ============================================================================

    @Nested
    @DisplayName("After Handshake Tests")
    class AfterHandshakeTests {

        @Test
        @DisplayName("Should log successful handshake completion")
        void shouldLogSuccessfulHandshakeCompletion() {
            // When
            handshakeInterceptor.afterHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, null);
            
            // Then - should not throw exception and log debug message
            verify(mockRequest).getRemoteAddress(); // Verify IP extraction was called
        }

        @Test
        @DisplayName("Should log and audit handshake exception")
        void shouldLogAndAuditHandshakeException() {
            // Given
            Exception handshakeException = new RuntimeException("Handshake failed");
            
            // When
            handshakeInterceptor.afterHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, handshakeException);
            
            // Then
            verify(mockSecurityAuditService).logSecurityEvent(
                    eq("WEBSOCKET_SECURITY_VIOLATION"),
                    eq("UNKNOWN"),
                    anyString(),
                    eq("WebSocket security violation: HANDSHAKE_EXCEPTION"),
                    any(Map.class)
            );
        }
    }

    // ============================================================================
    // ERROR HANDLING AND EDGE CASES
    // ============================================================================

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle SecurityException during validation")
        void shouldHandleSecurityExceptionDuringValidation() {
            // Given
            when(mockJwtTokenService.validateToken(anyString()))
                    .thenThrow(new SecurityException("Token validation failed"));
            when(mockHttpHeaders.getFirst("Authorization")).thenReturn("Bearer some-token");
            setupValidRateLimits();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isFalse();
            verify(mockResponse).setStatusCode(HttpStatus.FORBIDDEN);
            verify(mockSecurityAuditService).logSecurityEvent(
                    eq("WEBSOCKET_SECURITY_VIOLATION"),
                    eq("UNKNOWN"),
                    anyString(),
                    eq("WebSocket security violation: SECURITY_EXCEPTION"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("Should handle general exception during validation")
        void shouldHandleGeneralExceptionDuringValidation() {
            // Given
            when(mockRequest.getHeaders()).thenThrow(new RuntimeException("Unexpected error"));
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isFalse();
            verify(mockResponse).setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            verify(mockSecurityAuditService).logSecurityEvent(
                    eq("WEBSOCKET_SECURITY_VIOLATION"),
                    eq("UNKNOWN"),
                    anyString(),
                    eq("WebSocket security violation: HANDSHAKE_ERROR"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("Should extract client IP from X-Forwarded-For header")
        void shouldExtractClientIpFromXForwardedForHeader() {
            // Given
            var mockServletRequest = mock(HttpServletRequest.class);
            var mockServletServerHttpRequest = mock(ServletServerHttpRequest.class);
            
            when(mockServletServerHttpRequest.getServletRequest()).thenReturn(mockServletRequest);
            when(mockServletServerHttpRequest.getHeaders()).thenReturn(mockHttpHeaders);
            when(mockServletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 198.51.100.1");
            
            setupValidJwtToken();
            setupValidRateLimits();
            setupValidConnectionLimits();
            setupValidCSRFToken();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockServletServerHttpRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isTrue();
            assertThat(sessionAttributes.get("clientIp")).isEqualTo("203.0.113.1");
        }

        @Test
        @DisplayName("Should extract client IP from X-Real-IP header as fallback")
        void shouldExtractClientIpFromXRealIpHeaderAsFallback() {
            // Given
            var mockServletRequest = mock(HttpServletRequest.class);
            var mockServletServerHttpRequest = mock(ServletServerHttpRequest.class);
            
            when(mockServletServerHttpRequest.getServletRequest()).thenReturn(mockServletRequest);
            when(mockServletServerHttpRequest.getHeaders()).thenReturn(mockHttpHeaders);
            when(mockServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(mockServletRequest.getHeader("X-Real-IP")).thenReturn("203.0.113.1");
            
            setupValidJwtToken();
            setupValidRateLimits();
            setupValidConnectionLimits();
            setupValidCSRFToken();
            
            // When
            boolean result = handshakeInterceptor.beforeHandshake(
                    mockServletServerHttpRequest, mockResponse, mockWebSocketHandler, sessionAttributes);
            
            // Then
            assertThat(result).isTrue();
            assertThat(sessionAttributes.get("clientIp")).isEqualTo("203.0.113.1");
        }
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private void setupValidJwtToken() {
        when(mockHttpHeaders.getFirst("Authorization")).thenReturn("Bearer valid-jwt-token");
        setupValidJwtTokenBehavior("valid-jwt-token");
    }

    private void setupValidJwtTokenBehavior(String token) {
        when(mockJwtTokenService.validateToken(token)).thenReturn(true);
        when(mockTokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(mockJwtTokenService.getUserIdFromToken(token)).thenReturn("test-user");
        when(mockJwtTokenService.getUserRolesFromToken(token)).thenReturn(List.of("OPERATIONS_MANAGER"));
    }

    private void setupValidJwtTokenWithInsufficientRoles() {
        when(mockHttpHeaders.getFirst("Authorization")).thenReturn("Bearer valid-jwt-token");
        when(mockJwtTokenService.validateToken("valid-jwt-token")).thenReturn(true);
        when(mockTokenBlacklistService.isBlacklisted("valid-jwt-token")).thenReturn(false);
        when(mockJwtTokenService.getUserIdFromToken("valid-jwt-token")).thenReturn("test-user");
        when(mockJwtTokenService.getUserRolesFromToken("valid-jwt-token")).thenReturn(List.of("USER")); // Insufficient role
    }

    private void setupValidRateLimits() {
        when(mockValueOperations.increment(anyString())).thenReturn(5L); // Within limits
        when(mockRedisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
    }

    private void setupValidConnectionLimits() {
        when(mockValueOperations.get("fabric:websocket:connections:ip:192.168.1.100")).thenReturn(2); // Within IP limit
        when(mockValueOperations.get("fabric:websocket:connections:user:test-user")).thenReturn(1); // Within user limit
    }

    private void setupValidCSRFToken() {
        when(mockHttpHeaders.getFirst("X-CSRF-Token")).thenReturn("abcdef1234567890abcdef1234567890abcdef12"); // Valid 40-char token
    }
}