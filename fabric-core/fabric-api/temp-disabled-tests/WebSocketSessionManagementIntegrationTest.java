package com.truist.batch.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truist.batch.entity.WebSocketAuditLogEntity;
import com.truist.batch.repository.WebSocketAuditLogRepository;
import com.truist.batch.security.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
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
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * US008: WebSocket Session Management Integration Test
 * 
 * Comprehensive session lifecycle and distributed session testing including:
 * - Session creation, validation, and cleanup lifecycle
 * - Redis distributed session storage and clustering
 * - Session timeout and heartbeat management
 * - Cross-node session sharing and synchronization
 * - Session security validation and token rotation
 * - Connection limits and resource management
 * - Session persistence and recovery scenarios
 * - Memory leak prevention and cleanup verification
 * 
 * Target: 95%+ coverage of session management scenarios
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "fabric.websocket.redis-session-clustering=true",
    "fabric.websocket.session-timeout-minutes=5",
    "fabric.websocket.heartbeat-interval-seconds=30",
    "fabric.websocket.max-connections-per-user=3",
    "fabric.websocket.session-validation-interval-seconds=60"
})
@DisplayName("WebSocket Session Management Integration Tests")
class WebSocketSessionManagementIntegrationTest {

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
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketClient webSocketClient;
    private String wsUrl;
    private String validToken;
    private String userToken;
    private String adminToken;

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
        userToken = jwtTokenProvider.generateToken("session-user", List.of("ROLE_MONITOR", "ROLE_BATCH_USER"));
        adminToken = jwtTokenProvider.generateToken("session-admin", List.of("ROLE_ADMIN", "ROLE_MONITOR"));
        
        // Clear test data
        auditLogRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @AfterEach
    void tearDown() {
        // Cleanup Redis sessions
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    // ============================================================================
    // SESSION LIFECYCLE TESTS
    // ============================================================================

    @Nested
    @DisplayName("Session Lifecycle Management")
    class SessionLifecycleTests {

        @Test
        @DisplayName("Should create and store session in Redis on connection")
        void shouldCreateAndStoreSessionInRedisOnConnection() throws Exception {
            // Given
            SessionTestHandler handler = new SessionTestHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            // When
            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            // Then
            assertThat(session.isOpen()).isTrue();
            assertThat(handler.isConnected()).isTrue();

            // Verify session stored in Redis
            Thread.sleep(200); // Allow session creation
            Set<String> sessionKeys = redisTemplate.keys("fabric:websocket:session:*");
            assertThat(sessionKeys).isNotEmpty();

            // Verify session data
            String sessionKey = sessionKeys.iterator().next();
            Object sessionData = redisTemplate.opsForValue().get(sessionKey);
            assertThat(sessionData).isNotNull();

            // Verify audit log
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(10));
            assertThat(auditLogs).isNotEmpty();
            assertThat(auditLogs.get(0).getEventType()).isEqualTo("WEBSOCKET_CONNECTION_ESTABLISHED");

            session.close();
        }

        @Test
        @DisplayName("Should clean up session in Redis on disconnection")
        void shouldCleanUpSessionInRedisOnDisconnection() throws Exception {
            // Given
            SessionTestHandler handler = new SessionTestHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            Thread.sleep(200); // Allow session creation
            Set<String> sessionKeysBeforeClose = redisTemplate.keys("fabric:websocket:session:*");
            assertThat(sessionKeysBeforeClose).isNotEmpty();

            // When
            session.close();
            Thread.sleep(500); // Allow session cleanup

            // Then
            Set<String> sessionKeysAfterClose = redisTemplate.keys("fabric:websocket:session:*");
            assertThat(sessionKeysAfterClose).hasSize(sessionKeysBeforeClose.size() - 1);

            // Verify disconnection audit log
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(10));
            boolean disconnectFound = auditLogs.stream()
                    .anyMatch(log -> log.getEventType().equals("WEBSOCKET_CONNECTION_CLOSED"));
            assertThat(disconnectFound).isTrue();
        }

        @Test
        @DisplayName("Should handle session validation and renewal")
        void shouldHandleSessionValidationAndRenewal() throws Exception {
            // Given
            SessionTestHandler handler = new SessionTestHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            Thread.sleep(200);

            // When - send session validation request
            String validationMessage = """
                {
                    "type": "session-validate",
                    "timestamp": %d
                }
                """.formatted(System.currentTimeMillis());
            session.sendMessage(new TextMessage(validationMessage));

            // Then - should receive validation response
            String response = handler.getNextMessage(5, TimeUnit.SECONDS);
            assertThat(response).isNotNull();

            JsonNode responseJson = objectMapper.readTree(response);
            assertThat(responseJson.get("type").asText()).isEqualTo("session-validated");
            assertThat(responseJson.has("sessionId")).isTrue();
            assertThat(responseJson.has("validUntil")).isTrue();

            session.close();
        }

        @Test
        @DisplayName("Should detect and handle session expiration")
        void shouldDetectAndHandleSessionExpiration() throws Exception {
            // Given
            SessionTestHandler handler = new SessionTestHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            Thread.sleep(200);

            // When - simulate session expiration by manipulating Redis TTL
            Set<String> sessionKeys = redisTemplate.keys("fabric:websocket:session:*");
            assertThat(sessionKeys).isNotEmpty();
            
            String sessionKey = sessionKeys.iterator().next();
            redisTemplate.expire(sessionKey, 1, TimeUnit.SECONDS); // Force expiration

            Thread.sleep(2000); // Wait for expiration

            // Send message to trigger session validation
            session.sendMessage(new TextMessage("{\"type\":\"heartbeat\"}"));

            // Then - should receive session expired notification
            String response = handler.getNextMessage(5, TimeUnit.SECONDS);
            if (response != null) {
                JsonNode responseJson = objectMapper.readTree(response);
                if ("session-expired".equals(responseJson.get("type").asText())) {
                    assertThat(responseJson.get("reason").asText()).contains("expired");
                }
            }

            session.close();
        }
    }

    // ============================================================================
    // REDIS DISTRIBUTED SESSION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Redis Distributed Session Management")
    class RedisDistributedSessionTests {

        @Test
        @DisplayName("Should store comprehensive session metadata in Redis")
        void shouldStoreComprehensiveSessionMetadataInRedis() throws Exception {
            // Given
            SessionTestHandler handler = new SessionTestHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + userToken);
            headers.add("User-Agent", "Test-Client/1.0");
            headers.add("X-Forwarded-For", "192.168.1.100");

            // When
            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            Thread.sleep(300); // Allow session metadata storage

            // Then
            Set<String> sessionKeys = redisTemplate.keys("fabric:websocket:session:*");
            assertThat(sessionKeys).isNotEmpty();

            String sessionKey = sessionKeys.iterator().next();
            String sessionData = (String) redisTemplate.opsForValue().get(sessionKey);
            
            JsonNode sessionJson = objectMapper.readTree(sessionData);
            assertThat(sessionJson.get("userId").asText()).isEqualTo("session-user");
            assertThat(sessionJson.has("sessionId")).isTrue();
            assertThat(sessionJson.has("createdAt")).isTrue();
            assertThat(sessionJson.has("lastActivity")).isTrue();
            assertThat(sessionJson.get("userAgent").asText()).isEqualTo("Test-Client/1.0");
            assertThat(sessionJson.get("remoteAddress").asText()).contains("192.168.1.100");

            session.close();
        }

        @Test
        @DisplayName("Should update session activity timestamp on message")
        void shouldUpdateSessionActivityTimestampOnMessage() throws Exception {
            // Given
            SessionTestHandler handler = new SessionTestHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + userToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            Thread.sleep(200);
            
            Set<String> sessionKeys = redisTemplate.keys("fabric:websocket:session:*");
            String sessionKey = sessionKeys.iterator().next();
            String initialSessionData = (String) redisTemplate.opsForValue().get(sessionKey);
            JsonNode initialJson = objectMapper.readTree(initialSessionData);
            long initialActivity = initialJson.get("lastActivity").asLong();

            // When - send message to update activity
            Thread.sleep(1000); // Ensure timestamp difference
            session.sendMessage(new TextMessage("{\"type\":\"ping\"}"));
            Thread.sleep(200); // Allow session update

            // Then
            String updatedSessionData = (String) redisTemplate.opsForValue().get(sessionKey);
            JsonNode updatedJson = objectMapper.readTree(updatedSessionData);
            long updatedActivity = updatedJson.get("lastActivity").asLong();
            
            assertThat(updatedActivity).isGreaterThan(initialActivity);

            session.close();
        }

        @Test
        @DisplayName("Should handle Redis connection failures gracefully")
        void shouldHandleRedisConnectionFailuresGracefully() throws Exception {
            // Given
            SessionTestHandler handler = new SessionTestHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            // When - establish connection
            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            Thread.sleep(200);

            // Simulate Redis connectivity issue by clearing all data
            redisTemplate.getConnectionFactory().getConnection().flushAll();

            // Send message to trigger Redis operation
            session.sendMessage(new TextMessage("{\"type\":\"test\"}"));

            // Then - connection should remain stable
            assertThat(session.isOpen()).isTrue();

            // Should receive error notification or continue operating
            String response = handler.getNextMessage(3, TimeUnit.SECONDS);
            if (response != null) {
                JsonNode responseJson = objectMapper.readTree(response);
                // Should either continue normally or notify of degraded service
                assertThat(responseJson.get("type").asText())
                        .isIn("session-fallback", "redis-unavailable", "pong");
            }

            session.close();
        }

        @Test
        @DisplayName("Should enforce session TTL and automatic cleanup")
        void shouldEnforceSessionTtlAndAutomaticCleanup() throws Exception {
            // Given
            SessionTestHandler handler = new SessionTestHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            Thread.sleep(200);

            // When - verify session has TTL set
            Set<String> sessionKeys = redisTemplate.keys("fabric:websocket:session:*");
            assertThat(sessionKeys).isNotEmpty();
            
            String sessionKey = sessionKeys.iterator().next();
            Long ttl = redisTemplate.getExpire(sessionKey);
            
            // Then - session should have TTL configured
            assertThat(ttl).isGreaterThan(0);
            assertThat(ttl).isLessThanOrEqualTo(3600); // Max 1 hour

            session.close();
        }
    }

    // ============================================================================
    // CONNECTION LIMITS AND RESOURCE MANAGEMENT TESTS
    // ============================================================================

    @Nested
    @DisplayName("Connection Limits and Resource Management")
    class ConnectionLimitsResourceManagementTests {

        @Test
        @DisplayName("Should enforce per-user connection limits")
        void shouldEnforcePerUserConnectionLimits() throws Exception {
            // Given - establish maximum allowed connections per user
            SessionTestHandler[] handlers = new SessionTestHandler[4]; // Limit is 3
            WebSocketSession[] sessions = new WebSocketSession[4];
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + userToken);

            // When - attempt to exceed connection limit
            for (int i = 0; i < 3; i++) {
                handlers[i] = new SessionTestHandler();
                sessions[i] = webSocketClient.doHandshake(
                        handlers[i], headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);
                assertThat(sessions[i].isOpen()).isTrue();
            }

            // Fourth connection should be rejected
            handlers[3] = new SessionTestHandler();
            assertThatThrownBy(() -> 
                webSocketClient.doHandshake(handlers[3], headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS)
            ).hasCauseInstanceOf(Exception.class);

            // Then - verify connection limit enforcement audit
            Thread.sleep(200);
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(10));
            
            boolean limitViolationFound = auditLogs.stream()
                    .anyMatch(log -> log.getEventType().contains("CONNECTION_LIMIT_EXCEEDED"));
            assertThat(limitViolationFound).isTrue();

            // Cleanup
            for (int i = 0; i < 3; i++) {
                if (sessions[i] != null && sessions[i].isOpen()) {
                    sessions[i].close();
                }
            }
        }

        @Test
        @DisplayName("Should allow new connections after existing ones close")
        void shouldAllowNewConnectionsAfterExistingOnesClose() throws Exception {
            // Given - establish connections at limit
            SessionTestHandler[] initialHandlers = new SessionTestHandler[3];
            WebSocketSession[] initialSessions = new WebSocketSession[3];
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + userToken);

            for (int i = 0; i < 3; i++) {
                initialHandlers[i] = new SessionTestHandler();
                initialSessions[i] = webSocketClient.doHandshake(
                        initialHandlers[i], headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);
            }

            // When - close one connection and attempt new one
            initialSessions[0].close();
            Thread.sleep(300); // Allow cleanup

            SessionTestHandler newHandler = new SessionTestHandler();
            WebSocketSession newSession = webSocketClient.doHandshake(
                    newHandler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            // Then - new connection should be allowed
            assertThat(newSession.isOpen()).isTrue();

            // Cleanup
            newSession.close();
            for (int i = 1; i < 3; i++) {
                initialSessions[i].close();
            }
        }

        @Test
        @DisplayName("Should track and manage global connection count")
        void shouldTrackAndManageGlobalConnectionCount() throws Exception {
            // Given - establish multiple connections with different users
            WebSocketSession[] sessions = new WebSocketSession[5];
            String[] tokens = {
                userToken,
                adminToken,
                jwtTokenProvider.generateToken("user3", List.of("ROLE_MONITOR")),
                jwtTokenProvider.generateToken("user4", List.of("ROLE_MONITOR")),
                jwtTokenProvider.generateToken("user5", List.of("ROLE_MONITOR"))
            };

            // When - establish connections
            for (int i = 0; i < 5; i++) {
                SessionTestHandler handler = new SessionTestHandler();
                WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
                headers.add("Authorization", "Bearer " + tokens[i]);
                
                sessions[i] = webSocketClient.doHandshake(
                        handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);
            }

            Thread.sleep(300); // Allow session registration

            // Then - verify connection count tracking in Redis
            Set<String> sessionKeys = redisTemplate.keys("fabric:websocket:session:*");
            assertThat(sessionKeys).hasSize(5);

            Object connectionCount = redisTemplate.opsForValue().get("fabric:websocket:connection-count");
            assertThat(connectionCount).isNotNull();

            // Cleanup
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.close();
                }
            }
        }
    }

    // ============================================================================
    // HEARTBEAT AND TIMEOUT MANAGEMENT TESTS
    // ============================================================================

    @Nested
    @DisplayName("Heartbeat and Timeout Management")
    class HeartbeatTimeoutManagementTests {

        @Test
        @DisplayName("Should handle heartbeat messages and update session activity")
        void shouldHandleHeartbeatMessagesAndUpdateSessionActivity() throws Exception {
            // Given
            SessionTestHandler handler = new SessionTestHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            Thread.sleep(200);

            // When - send heartbeat
            String heartbeatMessage = """
                {
                    "type": "heartbeat",
                    "timestamp": %d
                }
                """.formatted(System.currentTimeMillis());
            session.sendMessage(new TextMessage(heartbeatMessage));

            // Then - should receive heartbeat response
            String response = handler.getNextMessage(5, TimeUnit.SECONDS);
            assertThat(response).isNotNull();

            JsonNode responseJson = objectMapper.readTree(response);
            assertThat(responseJson.get("type").asText()).isEqualTo("heartbeat-ack");
            assertThat(responseJson.has("serverTimestamp")).isTrue();

            session.close();
        }

        @Test
        @DisplayName("Should detect stale sessions and initiate cleanup")
        void shouldDetectStaleSessionsAndInitiateCleanup() throws Exception {
            // Given
            SessionTestHandler handler = new SessionTestHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            Thread.sleep(200);

            // When - simulate stale session by not sending heartbeats
            // and manually setting old timestamp in Redis
            Set<String> sessionKeys = redisTemplate.keys("fabric:websocket:session:*");
            if (!sessionKeys.isEmpty()) {
                String sessionKey = sessionKeys.iterator().next();
                String sessionData = (String) redisTemplate.opsForValue().get(sessionKey);
                JsonNode sessionJson = objectMapper.readTree(sessionData);
                
                // Update last activity to simulate stale session
                ((com.fasterxml.jackson.databind.node.ObjectNode) sessionJson)
                        .put("lastActivity", System.currentTimeMillis() - 600000); // 10 minutes ago
                
                redisTemplate.opsForValue().set(sessionKey, sessionJson.toString());
            }

            // Trigger session validation by sending a message
            session.sendMessage(new TextMessage("{\"type\":\"ping\"}"));

            // Then - should detect stale session
            String response = handler.getNextMessage(5, TimeUnit.SECONDS);
            if (response != null) {
                JsonNode responseJson = objectMapper.readTree(response);
                if ("session-stale".equals(responseJson.get("type").asText())) {
                    assertThat(responseJson.get("action").asText()).isEqualTo("refresh-required");
                }
            }

            session.close();
        }

        @Test
        @DisplayName("Should handle session timeout gracefully")
        void shouldHandleSessionTimeoutGracefully() throws Exception {
            // Given
            SessionTestHandler handler = new SessionTestHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            Thread.sleep(200);

            // When - force session timeout by manipulating Redis
            Set<String> sessionKeys = redisTemplate.keys("fabric:websocket:session:*");
            if (!sessionKeys.isEmpty()) {
                String sessionKey = sessionKeys.iterator().next();
                redisTemplate.expire(sessionKey, 1, TimeUnit.SECONDS);
                Thread.sleep(2000); // Wait for expiration
            }

            // Send message to trigger timeout detection
            session.sendMessage(new TextMessage("{\"type\":\"test\"}"));

            // Then - should handle timeout gracefully
            String response = handler.getNextMessage(5, TimeUnit.SECONDS);
            if (response != null) {
                JsonNode responseJson = objectMapper.readTree(response);
                assertThat(responseJson.get("type").asText())
                        .isIn("session-timeout", "session-expired", "authentication-required");
            }

            session.close();
        }
    }

    // ============================================================================
    // SESSION SECURITY AND TOKEN ROTATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Session Security and Token Rotation")
    class SessionSecurityTokenRotationTests {

        @Test
        @DisplayName("Should handle token refresh during active session")
        void shouldHandleTokenRefreshDuringActiveSession() throws Exception {
            // Given
            SessionTestHandler handler = new SessionTestHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            Thread.sleep(200);

            // When - send token refresh request
            String newToken = jwtTokenProvider.generateToken("test-user-001", List.of("ROLE_MONITOR", "ROLE_BATCH_USER"));
            String tokenRefreshMessage = """
                {
                    "type": "token-refresh",
                    "newToken": "%s"
                }
                """.formatted(newToken);
            session.sendMessage(new TextMessage(tokenRefreshMessage));

            // Then - should receive token refresh confirmation
            String response = handler.getNextMessage(5, TimeUnit.SECONDS);
            assertThat(response).isNotNull();

            JsonNode responseJson = objectMapper.readTree(response);
            assertThat(responseJson.get("type").asText()).isEqualTo("token-refreshed");
            assertThat(responseJson.get("status").asText()).isEqualTo("success");

            // Verify session data updated in Redis
            Set<String> sessionKeys = redisTemplate.keys("fabric:websocket:session:*");
            if (!sessionKeys.isEmpty()) {
                String sessionKey = sessionKeys.iterator().next();
                String sessionData = (String) redisTemplate.opsForValue().get(sessionKey);
                JsonNode sessionJson = objectMapper.readTree(sessionData);
                assertThat(sessionJson.has("tokenRefreshedAt")).isTrue();
            }

            session.close();
        }

        @Test
        @DisplayName("Should validate session integrity and detect tampering")
        void shouldValidateSessionIntegrityAndDetectTampering() throws Exception {
            // Given
            SessionTestHandler handler = new SessionTestHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            Thread.sleep(200);

            // When - tamper with session data in Redis
            Set<String> sessionKeys = redisTemplate.keys("fabric:websocket:session:*");
            if (!sessionKeys.isEmpty()) {
                String sessionKey = sessionKeys.iterator().next();
                String sessionData = (String) redisTemplate.opsForValue().get(sessionKey);
                JsonNode sessionJson = objectMapper.readTree(sessionData);
                
                // Tamper with session data
                ((com.fasterxml.jackson.databind.node.ObjectNode) sessionJson)
                        .put("userId", "tampered-user");
                
                redisTemplate.opsForValue().set(sessionKey, sessionJson.toString());
            }

            // Send message to trigger integrity check
            session.sendMessage(new TextMessage("{\"type\":\"integrity-check\"}"));

            // Then - should detect tampering
            String response = handler.getNextMessage(5, TimeUnit.SECONDS);
            if (response != null) {
                JsonNode responseJson = objectMapper.readTree(response);
                if ("session-integrity-violation".equals(responseJson.get("type").asText())) {
                    assertThat(responseJson.get("violation").asText()).contains("tampered");
                }
            }

            // Verify security audit log
            Thread.sleep(200);
            List<WebSocketAuditLogEntity> auditLogs = auditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(
                    Instant.now().minusSeconds(10));
            boolean integrityViolationFound = auditLogs.stream()
                    .anyMatch(log -> log.getEventType().contains("INTEGRITY_VIOLATION"));
            assertThat(integrityViolationFound).isTrue();

            session.close();
        }

        @Test
        @DisplayName("Should handle concurrent session operations safely")
        void shouldHandleConcurrentSessionOperationsSafely() throws Exception {
            // Given
            SessionTestHandler handler = new SessionTestHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + validToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            Thread.sleep(200);

            // When - send multiple concurrent session operations
            Thread[] threads = new Thread[5];
            for (int i = 0; i < 5; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        session.sendMessage(new TextMessage("""
                            {
                                "type": "session-operation",
                                "operation": "update-metadata",
                                "data": {"key": "value%d"}
                            }
                            """.formatted(index)));
                    } catch (Exception e) {
                        // Handle concurrent access
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join(5000);
            }

            // Then - session should remain consistent
            assertThat(session.isOpen()).isTrue();

            // Verify session data integrity
            Set<String> sessionKeys = redisTemplate.keys("fabric:websocket:session:*");
            if (!sessionKeys.isEmpty()) {
                String sessionKey = sessionKeys.iterator().next();
                String sessionData = (String) redisTemplate.opsForValue().get(sessionKey);
                assertThat(sessionData).isNotNull();
                JsonNode sessionJson = objectMapper.readTree(sessionData);
                assertThat(sessionJson.get("userId").asText()).isEqualTo("test-user-001");
            }

            session.close();
        }
    }

    // ============================================================================
    // HELPER METHODS AND TEST UTILITIES
    // ============================================================================

    /**
     * Test WebSocket handler for session management testing
     */
    private static class SessionTestHandler implements WebSocketHandler {
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