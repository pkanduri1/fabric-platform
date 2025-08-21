package com.truist.batch.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truist.batch.config.WebSocketSecurityConfig;
import com.truist.batch.entity.WebSocketAuditLogEntity;
import com.truist.batch.repository.WebSocketAuditLogRepository;
import com.truist.batch.security.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * US008: WebSocket Security Integration Test
 * 
 * Comprehensive end-to-end security testing including:
 * - JWT authentication and token validation flow
 * - Rate limiting and connection throttling
 * - CSRF protection and origin validation
 * - Session security and token rotation
 * - Security event auditing and compliance
 * - Connection lifecycle security controls
 * - Real Redis and Database integration
 * 
 * Target: 95%+ coverage of security scenarios
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "fabric.websocket.security-enabled=true",
    "fabric.websocket.rate-limit-enabled=true",
    "fabric.websocket.audit-logging=true",
    "fabric.websocket.development-mode=false"
})
@DisplayName("WebSocket Security Integration Tests")
class WebSocketSecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("fabric_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private WebSocketAuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketClient webSocketClient;
    private String validToken;
    private String expiredToken;
    private String invalidToken;
    private String wsUrl;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @BeforeEach
    void setUp() throws Exception {
        webSocketClient = new StandardWebSocketClient();
        wsUrl = "ws://localhost:" + port + "/fabric-websocket";
        
        // Generate test tokens
        validToken = jwtTokenProvider.generateToken("test-user-001", List.of("ROLE_MONITOR", "ROLE_BATCH_USER"));
        expiredToken = generateExpiredToken();
        invalidToken = "invalid.token.signature";
        
        // Clear audit logs
        auditLogRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // Clean up any remaining connections
        if (webSocketClient != null) {
            // WebSocket client cleanup handled by test framework
        }
    }

    // ============================================================================
    // JWT AUTHENTICATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("JWT Authentication Flow")
    class JwtAuthenticationTests {

        @Test
        @DisplayName("Should successfully connect with valid JWT token")
        void shouldSuccessfullyConnectWithValidJwtToken() throws Exception {
            // Given
            TestWebSocketHandler handler = new TestWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            // When
            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            // Then
            assertThat(session.isOpen()).isTrue();
            assertThat(handler.isConnected()).isTrue();
            
            // Verify audit log
            Thread.sleep(100); // Allow time for audit log
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(10));
            assertThat(auditLogs).isNotEmpty();
            assertThat(auditLogs.get(0).getEventType()).isEqualTo("WEBSOCKET_CONNECTION_ESTABLISHED");
            assertThat(auditLogs.get(0).getUserId()).isEqualTo("test-user-001");

            session.close();
        }

        @Test
        @DisplayName("Should reject connection with invalid JWT token")
        void shouldRejectConnectionWithInvalidJwtToken() throws Exception {
            // Given
            TestWebSocketHandler handler = new TestWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + invalidToken);

            // When & Then
            assertThatThrownBy(() -> 
                webSocketClient.doHandshake(handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS)
            ).hasCauseInstanceOf(Exception.class);

            // Verify security audit log
            Thread.sleep(100);
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(10));
            assertThat(auditLogs).isNotEmpty();
            assertThat(auditLogs.get(0).getEventType()).isEqualTo("WEBSOCKET_AUTH_FAILURE");
            assertThat(auditLogs.get(0).getSecurityEventFlag()).isEqualTo("Y");
            assertThat(auditLogs.get(0).getRiskLevel()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("Should reject connection with expired JWT token")
        void shouldRejectConnectionWithExpiredJwtToken() throws Exception {
            // Given
            TestWebSocketHandler handler = new TestWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + expiredToken);

            // When & Then
            assertThatThrownBy(() -> 
                webSocketClient.doHandshake(handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS)
            ).hasCauseInstanceOf(Exception.class);

            // Verify audit log shows token expiration
            Thread.sleep(100);
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(10));
            assertThat(auditLogs).isNotEmpty();
            assertThat(auditLogs.get(0).getEventType()).isEqualTo("WEBSOCKET_AUTH_FAILURE");
            assertThat(auditLogs.get(0).getEventDescription()).contains("expired");
        }

        @Test
        @DisplayName("Should reject connection without JWT token")
        void shouldRejectConnectionWithoutJwtToken() throws Exception {
            // Given
            TestWebSocketHandler handler = new TestWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            // No Authorization header

            // When & Then
            assertThatThrownBy(() -> 
                webSocketClient.doHandshake(handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS)
            ).hasCauseInstanceOf(Exception.class);

            // Verify security audit log
            Thread.sleep(100);
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(10));
            assertThat(auditLogs).isNotEmpty();
            assertThat(auditLogs.get(0).getEventType()).isEqualTo("WEBSOCKET_AUTH_FAILURE");
            assertThat(auditLogs.get(0).getEventDescription()).contains("missing");
        }
    }

    // ============================================================================
    // RATE LIMITING TESTS
    // ============================================================================

    @Nested
    @DisplayName("Rate Limiting and Throttling")
    class RateLimitingTests {

        @Test
        @DisplayName("Should enforce connection rate limits per user")
        void shouldEnforceConnectionRateLimitsPerUser() throws Exception {
            // Given - attempt multiple rapid connections
            TestWebSocketHandler handler1 = new TestWebSocketHandler();
            TestWebSocketHandler handler2 = new TestWebSocketHandler();
            TestWebSocketHandler handler3 = new TestWebSocketHandler();
            TestWebSocketHandler handler4 = new TestWebSocketHandler(); // Should be rejected
            
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            // When - establish multiple connections rapidly
            WebSocketSession session1 = webSocketClient.doHandshake(
                    handler1, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);
            WebSocketSession session2 = webSocketClient.doHandshake(
                    handler2, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);
            WebSocketSession session3 = webSocketClient.doHandshake(
                    handler3, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            // Fourth connection should be rate limited
            assertThatThrownBy(() -> 
                webSocketClient.doHandshake(handler4, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS)
            ).hasCauseInstanceOf(Exception.class);

            // Then
            assertThat(session1.isOpen()).isTrue();
            assertThat(session2.isOpen()).isTrue();
            assertThat(session3.isOpen()).isTrue();

            // Verify rate limiting audit log
            Thread.sleep(100);
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(10));
            boolean rateLimitFound = auditLogs.stream()
                    .anyMatch(log -> log.getEventType().contains("RATE_LIMIT"));
            assertThat(rateLimitFound).isTrue();

            // Cleanup
            session1.close();
            session2.close();
            session3.close();
        }

        @Test
        @DisplayName("Should enforce message rate limits per session")
        void shouldEnforceMessageRateLimitsPerSession() throws Exception {
            // Given
            TestWebSocketHandler handler = new TestWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            // When - send rapid messages exceeding rate limit
            String message = "{\"type\":\"subscribe\",\"topics\":[\"job-status\"]}";
            for (int i = 0; i < 150; i++) { // Exceed rate limit of 100/minute
                session.sendMessage(new TextMessage(message));
                Thread.sleep(10); // Small delay to simulate rapid sending
            }

            // Then - verify rate limiting occurred
            Thread.sleep(500); // Allow processing
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(30));
            
            boolean rateLimitViolation = auditLogs.stream()
                    .anyMatch(log -> log.getEventType().contains("RATE_LIMIT") 
                                  && log.getEventDescription().contains("message"));
            assertThat(rateLimitViolation).isTrue();

            session.close();
        }
    }

    // ============================================================================
    // CSRF AND ORIGIN VALIDATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("CSRF Protection and Origin Validation")
    class CsrfOriginValidationTests {

        @Test
        @DisplayName("Should accept connection from allowed origin")
        void shouldAcceptConnectionFromAllowedOrigin() throws Exception {
            // Given
            TestWebSocketHandler handler = new TestWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);
            headers.add("Origin", "https://truist.com");

            // When
            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            // Then
            assertThat(session.isOpen()).isTrue();
            session.close();
        }

        @Test
        @DisplayName("Should reject connection from disallowed origin")
        void shouldRejectConnectionFromDisallowedOrigin() throws Exception {
            // Given
            TestWebSocketHandler handler = new TestWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);
            headers.add("Origin", "https://malicious-site.com");

            // When & Then
            assertThatThrownBy(() -> 
                webSocketClient.doHandshake(handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS)
            ).hasCauseInstanceOf(Exception.class);

            // Verify security audit log
            Thread.sleep(100);
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(10));
            boolean originRejection = auditLogs.stream()
                    .anyMatch(log -> log.getEventType().contains("ORIGIN_REJECTED"));
            assertThat(originRejection).isTrue();
        }

        @Test
        @DisplayName("Should validate CSRF token when present")
        void shouldValidateCsrfTokenWhenPresent() throws Exception {
            // Given
            TestWebSocketHandler handler = new TestWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);
            headers.add("X-CSRF-TOKEN", "invalid-csrf-token");

            // When & Then
            assertThatThrownBy(() -> 
                webSocketClient.doHandshake(handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS)
            ).hasCauseInstanceOf(Exception.class);

            // Verify CSRF security audit
            Thread.sleep(100);
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(10));
            boolean csrfFailure = auditLogs.stream()
                    .anyMatch(log -> log.getEventDescription().contains("CSRF"));
            assertThat(csrfFailure).isTrue();
        }
    }

    // ============================================================================
    // SESSION SECURITY TESTS
    // ============================================================================

    @Nested
    @DisplayName("Session Security and Token Rotation")
    class SessionSecurityTests {

        @Test
        @DisplayName("Should handle token refresh during active session")
        void shouldHandleTokenRefreshDuringActiveSession() throws Exception {
            // Given
            TestWebSocketHandler handler = new TestWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            // When - simulate token refresh notification
            String refreshMessage = "{\"type\":\"token-refresh\",\"newToken\":\"" + 
                    jwtTokenProvider.generateToken("test-user-001", List.of("ROLE_MONITOR")) + "\"}";
            session.sendMessage(new TextMessage(refreshMessage));

            // Then - session should remain active with new token
            Thread.sleep(200);
            assertThat(session.isOpen()).isTrue();

            // Verify token refresh audit
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(10));
            boolean tokenRefresh = auditLogs.stream()
                    .anyMatch(log -> log.getEventType().contains("TOKEN_REFRESH"));
            assertThat(tokenRefresh).isTrue();

            session.close();
        }

        @Test
        @DisplayName("Should detect and prevent session hijacking attempts")
        void shouldDetectAndPreventSessionHijackingAttempts() throws Exception {
            // Given
            TestWebSocketHandler handler = new TestWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);
            headers.add("X-Forwarded-For", "192.168.1.100");

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            // When - simulate IP change during session (potential hijacking)
            String suspiciousMessage = "{\"type\":\"subscribe\",\"clientInfo\":{\"ip\":\"10.0.0.1\"}}";
            session.sendMessage(new TextMessage(suspiciousMessage));

            // Then - verify security monitoring
            Thread.sleep(200);
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(10));
            boolean securityAlert = auditLogs.stream()
                    .anyMatch(log -> log.getEventType().contains("SECURITY_ALERT"));
            assertThat(securityAlert).isTrue();

            session.close();
        }
    }

    // ============================================================================
    // AUDIT AND COMPLIANCE TESTS
    // ============================================================================

    @Nested
    @DisplayName("Security Audit and Compliance")
    class SecurityAuditComplianceTests {

        @Test
        @DisplayName("Should log all security events with proper audit trail")
        void shouldLogAllSecurityEventsWithProperAuditTrail() throws Exception {
            // Given
            TestWebSocketHandler handler = new TestWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            // When - perform complete connection lifecycle
            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);
            
            session.sendMessage(new TextMessage("{\"type\":\"subscribe\",\"topics\":[\"job-status\"]}"));
            Thread.sleep(100);
            session.close();
            Thread.sleep(100);

            // Then - verify comprehensive audit trail
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(10));
            
            assertThat(auditLogs).hasSizeGreaterThanOrEqualTo(3); // Connect, Subscribe, Disconnect
            
            // Verify audit trail integrity
            for (WebSocketAuditLogEntity log : auditLogs) {
                assertThat(log.getAuditHash()).isNotNull();
                assertThat(log.getCreatedBy()).isNotNull();
                assertThat(log.getSecurityClassification()).isEqualTo("INTERNAL");
                assertThat(log.getBusinessFunction()).isEqualTo("REAL_TIME_MONITORING");
            }
            
            // Verify audit chain integrity
            long brokenChains = auditLogRepository.countBrokenAuditChains();
            assertThat(brokenChains).isEqualTo(0);
        }

        @Test
        @DisplayName("Should generate compliance reports with security events")
        void shouldGenerateComplianceReportsWithSecurityEvents() throws Exception {
            // Given - generate various security events
            generateSecurityEvents();

            // When - query compliance events
            List<WebSocketAuditLogEntity> complianceEvents = auditLogRepository.findComplianceEventsForReporting(
                    Instant.now().minusMinutes(5), Instant.now());

            // Then - verify compliance reporting
            assertThat(complianceEvents).isNotEmpty();
            
            // Verify compliance event properties
            for (WebSocketAuditLogEntity event : complianceEvents) {
                assertThat(event.getComplianceEventFlag()).isEqualTo("Y");
                assertThat(event.getDigitalSignature()).isNotNull();
                assertThat(event.getEventTimestamp()).isNotNull();
            }

            // Verify security event summary
            List<Object[]> securitySummary = auditLogRepository.generateSecurityEventSummary(
                    Instant.now().minusMinutes(5), Instant.now());
            assertThat(securitySummary).isNotEmpty();
        }
    }

    // ============================================================================
    // PERFORMANCE AND RESILIENCE TESTS
    // ============================================================================

    @Nested
    @DisplayName("Security Performance and Resilience")
    class SecurityPerformanceResilienceTests {

        @Test
        @DisplayName("Should maintain security under high connection load")
        void shouldMaintainSecurityUnderHighConnectionLoad() throws Exception {
            // Given - multiple concurrent connections
            int connectionCount = 10;
            TestWebSocketHandler[] handlers = new TestWebSocketHandler[connectionCount];
            WebSocketSession[] sessions = new WebSocketSession[connectionCount];
            
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            // When - establish multiple concurrent connections
            for (int i = 0; i < connectionCount; i++) {
                handlers[i] = new TestWebSocketHandler();
                sessions[i] = webSocketClient.doHandshake(
                        handlers[i], headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);
            }

            // Then - all connections should be secure
            for (int i = 0; i < connectionCount; i++) {
                assertThat(sessions[i].isOpen()).isTrue();
                assertThat(handlers[i].isConnected()).isTrue();
            }

            // Verify security audit for all connections
            Thread.sleep(200);
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(30));
            
            long connectionEvents = auditLogs.stream()
                    .filter(log -> log.getEventType().equals("WEBSOCKET_CONNECTION_ESTABLISHED"))
                    .count();
            assertThat(connectionEvents).isEqualTo(connectionCount);

            // Cleanup
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.close();
                }
            }
        }

        @Test
        @DisplayName("Should handle security failures gracefully")
        void shouldHandleSecurityFailuresGracefully() throws Exception {
            // Given - various invalid authentication attempts
            String[] invalidTokens = {
                    "invalid.token.here",
                    "",
                    "Bearer malformed-token",
                    expiredToken,
                    null
            };

            // When - attempt connections with invalid tokens
            for (String token : invalidTokens) {
                TestWebSocketHandler handler = new TestWebSocketHandler();
                WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
                if (token != null) {
                    headers.add("Authorization", "Bearer " + token);
                }

                // Then - connection should fail gracefully
                assertThatThrownBy(() -> 
                    webSocketClient.doHandshake(handler, headers, URI.create(wsUrl)).get(2, TimeUnit.SECONDS)
                ).hasCauseInstanceOf(Exception.class);
            }

            // Verify all failures are properly audited
            Thread.sleep(200);
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(30));
            
            long failureEvents = auditLogs.stream()
                    .filter(log -> log.getEventType().equals("WEBSOCKET_AUTH_FAILURE"))
                    .count();
            assertThat(failureEvents).isGreaterThanOrEqualTo(invalidTokens.length);
        }
    }

    // ============================================================================
    // HELPER METHODS AND UTILITIES
    // ============================================================================

    private String generateExpiredToken() {
        // Generate a token that's already expired
        return jwtTokenProvider.generateTokenWithExpiry("test-user-expired", 
                List.of("ROLE_MONITOR"), Instant.now().minusSeconds(3600));
    }

    private void generateSecurityEvents() throws Exception {
        // Generate various types of security events for testing
        
        // Valid connection
        TestWebSocketHandler validHandler = new TestWebSocketHandler();
        WebSocketHttpHeaders validHeaders = new WebSocketHttpHeaders();
        validHeaders.add("Authorization", "Bearer " + validToken);
        WebSocketSession validSession = webSocketClient.doHandshake(
                validHandler, validHeaders, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);
        validSession.close();

        // Invalid token attempt
        try {
            TestWebSocketHandler invalidHandler = new TestWebSocketHandler();
            WebSocketHttpHeaders invalidHeaders = new WebSocketHttpHeaders();
            invalidHeaders.add("Authorization", "Bearer invalid-token");
            webSocketClient.doHandshake(invalidHandler, invalidHeaders, URI.create(wsUrl)).get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Expected failure
        }

        // Invalid origin attempt
        try {
            TestWebSocketHandler originHandler = new TestWebSocketHandler();
            WebSocketHttpHeaders originHeaders = new WebSocketHttpHeaders();
            originHeaders.add("Authorization", "Bearer " + validToken);
            originHeaders.add("Origin", "https://malicious.com");
            webSocketClient.doHandshake(originHandler, originHeaders, URI.create(wsUrl)).get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Expected failure
        }

        Thread.sleep(100); // Allow audit processing
    }

    /**
     * Test WebSocket handler for integration testing
     */
    private static class TestWebSocketHandler implements WebSocketHandler {
        private boolean connected = false;
        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private Throwable error;

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            connected = true;
        }

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
            messages.offer(message.getPayload().toString());
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            error = exception;
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
            connected = false;
        }

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }

        public boolean isConnected() {
            return connected;
        }

        public String getNextMessage(long timeout, TimeUnit unit) throws InterruptedException {
            return messages.poll(timeout, unit);
        }

        public Throwable getError() {
            return error;
        }
    }
}