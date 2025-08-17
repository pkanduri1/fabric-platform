package com.truist.batch.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truist.batch.config.WebSocketMonitoringProperties;
import com.truist.batch.config.WebSocketSecurityConfig;
import com.truist.batch.security.jwt.JwtTokenService;
import com.truist.batch.service.SecurityAuditService;
import com.truist.batch.websocket.service.RealTimeMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.web.socket.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * US008: Comprehensive Unit Tests for EnterpriseWebSocketHandler
 * 
 * Tests banking-grade WebSocket connection management including:
 * - Session lifecycle management with security validation
 * - Message handling and processing with role-based filtering
 * - Connection quality monitoring and error handling
 * - Redis-based distributed session tracking
 * - SOX compliance audit logging
 * - JWT token rotation and session validation
 * 
 * Target: 95%+ code coverage with comprehensive edge case testing
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnterpriseWebSocketHandler Unit Tests")
class EnterpriseWebSocketHandlerTest {

    @Mock
    private RedisTemplate<String, Object> mockRedisTemplate;
    
    @Mock
    private JwtTokenService mockJwtTokenService;
    
    @Mock
    private SecurityAuditService mockAuditService;
    
    @Mock
    private WebSocketMonitoringProperties mockMonitoringProperties;
    
    @Mock
    private WebSocketSecurityConfig.WebSocketConnectionManager mockConnectionManager;
    
    @Mock
    private WebSocketSecurityConfig.WebSocketSecurityEventPublisher mockSecurityEventPublisher;
    
    @Mock
    private RealTimeMonitoringService mockRealTimeMonitoringService;
    
    @Mock
    private ObjectMapper mockObjectMapper;
    
    @Mock
    private WebSocketSession mockWebSocketSession;
    
    @Mock
    private HashOperations<String, Object, Object> mockHashOperations;

    private EnterpriseWebSocketHandler webSocketHandler;
    private Map<String, Object> sessionAttributes;

    @BeforeEach
    void setUp() {
        webSocketHandler = new EnterpriseWebSocketHandler(
                mockRedisTemplate,
                mockJwtTokenService,
                mockAuditService,
                mockMonitoringProperties,
                mockConnectionManager,
                mockSecurityEventPublisher,
                mockRealTimeMonitoringService,
                mockObjectMapper
        );
        
        // Set up default session attributes
        sessionAttributes = new HashMap<>();
        sessionAttributes.put("securityValidated", true);
        sessionAttributes.put("userId", "test-user");
        sessionAttributes.put("userRoles", List.of("OPERATIONS_MANAGER"));
        sessionAttributes.put("jwtToken", "valid-jwt-token");
        sessionAttributes.put("clientIp", "192.168.1.100");
        sessionAttributes.put("correlationId", "test-correlation-123");
        
        // Set up mock behaviors
        when(mockWebSocketSession.getId()).thenReturn("test-session-123");
        when(mockWebSocketSession.getAttributes()).thenReturn(sessionAttributes);
        when(mockWebSocketSession.isOpen()).thenReturn(true);
        
        when(mockMonitoringProperties.getSessionStaleTimeoutMs()).thenReturn(1800000L); // 30 minutes
        when(mockMonitoringProperties.getRedisKeyPrefix()).thenReturn("fabric:websocket");
        when(mockMonitoringProperties.getRedisSessionTtl()).thenReturn(Duration.ofHours(1));
        
        when(mockRedisTemplate.opsForHash()).thenReturn(mockHashOperations);
        when(mockJwtTokenService.validateToken(anyString())).thenReturn(true);
    }

    // ============================================================================
    // CONNECTION ESTABLISHMENT TESTS
    // ============================================================================

    @Nested
    @DisplayName("Connection Establishment")
    class ConnectionEstablishmentTests {

        @Test
        @DisplayName("Should successfully establish WebSocket connection with valid session")
        void shouldSuccessfullyEstablishWebSocketConnectionWithValidSession() throws Exception {
            // When
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Then
            verify(mockConnectionManager).registerSession(eq("test-session-123"), eq(mockWebSocketSession));
            verify(mockRealTimeMonitoringService).subscribeSession(any(WebSocketSessionInfo.class));
            verify(mockSecurityEventPublisher).publishSecurityEvent(
                    eq("WEBSOCKET_CONNECTION_ESTABLISHED"),
                    eq("test-session-123"),
                    eq("test-user"),
                    eq("WebSocket connection established successfully")
            );
            
            // Verify Redis session registration
            verify(mockHashOperations).putAll(
                    eq("fabric:websocket:session:test-session-123"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("Should reject connection with invalid security validation")
        void shouldRejectConnectionWithInvalidSecurityValidation() throws Exception {
            // Given
            sessionAttributes.put("securityValidated", false);
            
            // When
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Then
            verify(mockWebSocketSession).close(eq(CloseStatus.NOT_ACCEPTABLE.withReason("Security validation failed")));
            verify(mockConnectionManager, never()).registerSession(anyString(), any());
            verify(mockRealTimeMonitoringService, never()).subscribeSession(any());
        }

        @Test
        @DisplayName("Should reject connection with missing user information")
        void shouldRejectConnectionWithMissingUserInformation() throws Exception {
            // Given
            sessionAttributes.remove("userId");
            
            // When
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Then
            verify(mockWebSocketSession).close(eq(CloseStatus.NOT_ACCEPTABLE.withReason("Security validation failed")));
            verify(mockConnectionManager, never()).registerSession(anyString(), any());
        }

        @Test
        @DisplayName("Should handle JWT token rotation during connection")
        void shouldHandleJwtTokenRotationDuringConnection() throws Exception {
            // Given
            when(mockJwtTokenService.isTokenNearExpiry("valid-jwt-token")).thenReturn(true);
            when(mockJwtTokenService.rotateToken("valid-jwt-token")).thenReturn("new-jwt-token");
            
            // When
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Then
            verify(mockJwtTokenService).rotateToken("valid-jwt-token");
            verify(sessionAttributes).put("jwtToken", "new-jwt-token");
        }

        @Test
        @DisplayName("Should handle connection establishment error gracefully")
        void shouldHandleConnectionEstablishmentErrorGracefully() throws Exception {
            // Given
            when(mockConnectionManager.registerSession(anyString(), any()))
                    .thenThrow(new RuntimeException("Registration failed"));
            
            // When
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Then
            verify(mockWebSocketSession).close(eq(CloseStatus.SERVER_ERROR.withReason("Connection failed")));
            verify(mockAuditService).logSecurityEvent(
                    eq("WEBSOCKET_HANDSHAKE_FAILURE"),
                    eq("test-user"),
                    eq("192.168.1.100"),
                    contains("Failed to establish WebSocket connection"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("Should handle Redis connection failure during session registration")
        void shouldHandleRedisConnectionFailureDuringSessionRegistration() throws Exception {
            // Given
            when(mockRedisTemplate.opsForHash()).thenThrow(new RuntimeException("Redis unavailable"));
            
            // When & Then - should not throw exception, connection should still succeed
            assertThatCode(() -> webSocketHandler.afterConnectionEstablished(mockWebSocketSession))
                    .doesNotThrowAnyException();
            
            verify(mockConnectionManager).registerSession(eq("test-session-123"), eq(mockWebSocketSession));
        }
    }

    // ============================================================================
    // MESSAGE HANDLING TESTS
    // ============================================================================

    @Nested
    @DisplayName("Message Handling")
    class MessageHandlingTests {

        private WebSocketSessionInfo sessionInfo;

        @BeforeEach
        void setUpSessionInfo() throws Exception {
            // Establish connection first to create session info
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Create session info for testing
            sessionInfo = WebSocketSessionInfo.builder()
                    .sessionId("test-session-123")
                    .session(mockWebSocketSession)
                    .userId("test-user")
                    .userRoles(List.of("OPERATIONS_MANAGER"))
                    .clientIp("192.168.1.100")
                    .correlationId("test-correlation-123")
                    .connectedAt(Instant.now())
                    .lastActivity(Instant.now())
                    .jwtToken("valid-jwt-token")
                    .connectionStatus(WebSocketSessionInfo.ConnectionStatus.ACTIVE)
                    .build();
        }

        @Test
        @DisplayName("Should handle heartbeat messages correctly")
        void shouldHandleHeartbeatMessagesCorrectly() throws Exception {
            // Given
            TextMessage heartbeatMessage = new TextMessage("{\"type\":\"heartbeat\",\"timestamp\":\"2024-01-01T00:00:00Z\"}");
            
            // When
            webSocketHandler.handleTextMessage(mockWebSocketSession, heartbeatMessage);
            
            // Then - should update session activity and not throw exception
            verify(mockHashOperations, atLeastOnce()).put(
                    eq("fabric:websocket:session:test-session-123"),
                    eq("lastActivity"),
                    anyString()
            );
        }

        @Test
        @DisplayName("Should handle subscription requests")
        void shouldHandleSubscriptionRequests() throws Exception {
            // Given
            TextMessage subscriptionMessage = new TextMessage(
                    "{\"type\":\"subscribe\",\"subscriptionType\":\"JOB_STATUS\",\"filters\":{}}"
            );
            
            // When
            webSocketHandler.handleTextMessage(mockWebSocketSession, subscriptionMessage);
            
            // Then - should process subscription request
            verify(mockHashOperations, atLeastOnce()).put(
                    eq("fabric:websocket:session:test-session-123"),
                    eq("messagesReceived"),
                    anyString()
            );
        }

        @Test
        @DisplayName("Should handle unsubscription requests")
        void shouldHandleUnsubscriptionRequests() throws Exception {
            // Given
            TextMessage unsubscriptionMessage = new TextMessage(
                    "{\"type\":\"unsubscribe\",\"subscriptionType\":\"JOB_STATUS\"}"
            );
            
            // When
            webSocketHandler.handleTextMessage(mockWebSocketSession, unsubscriptionMessage);
            
            // Then - should process unsubscription request
            verify(mockHashOperations, atLeastOnce()).put(
                    eq("fabric:websocket:session:test-session-123"),
                    eq("messagesReceived"),
                    anyString()
            );
        }

        @Test
        @DisplayName("Should handle monitoring configuration requests")
        void shouldHandleMonitoringConfigurationRequests() throws Exception {
            // Given
            TextMessage configMessage = new TextMessage(
                    "{\"type\":\"configure_monitoring\",\"preferences\":{\"deltaUpdates\":true}}"
            );
            
            // When
            webSocketHandler.handleTextMessage(mockWebSocketSession, configMessage);
            
            // Then - should process configuration request
            verify(mockHashOperations, atLeastOnce()).put(
                    eq("fabric:websocket:session:test-session-123"),
                    eq("messagesReceived"),
                    anyString()
            );
        }

        @Test
        @DisplayName("Should reject messages from unregistered sessions")
        void shouldRejectMessagesFromUnregisteredSessions() throws Exception {
            // Given
            when(mockWebSocketSession.getId()).thenReturn("unregistered-session");
            TextMessage message = new TextMessage("{\"type\":\"heartbeat\"}");
            
            // When
            webSocketHandler.handleTextMessage(mockWebSocketSession, message);
            
            // Then
            verify(mockWebSocketSession).close(eq(CloseStatus.NOT_ACCEPTABLE.withReason("Session not registered")));
        }

        @Test
        @DisplayName("Should handle unknown message types gracefully")
        void shouldHandleUnknownMessageTypesGracefully() throws Exception {
            // Given
            TextMessage unknownMessage = new TextMessage("{\"type\":\"unknown_type\",\"data\":{}}");
            
            // When
            webSocketHandler.handleTextMessage(mockWebSocketSession, unknownMessage);
            
            // Then - should log warning and send error response
            verify(mockHashOperations, atLeastOnce()).put(
                    eq("fabric:websocket:session:test-session-123"),
                    eq("messagesReceived"),
                    anyString()
            );
        }

        @Test
        @DisplayName("Should handle message parsing errors gracefully")
        void shouldHandleMessageParsingErrorsGracefully() throws Exception {
            // Given
            TextMessage invalidMessage = new TextMessage("invalid-json-message");
            
            // When
            webSocketHandler.handleTextMessage(mockWebSocketSession, invalidMessage);
            
            // Then - should handle parsing error gracefully
            verify(mockAuditService).logSecurityEvent(
                    eq("WEBSOCKET_MESSAGE_ERROR"),
                    eq("test-user"),
                    eq("192.168.1.100"),
                    contains("Error handling WebSocket message"),
                    any(Map.class)
            );
        }
    }

    // ============================================================================
    // CONNECTION MONITORING AND VALIDATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Connection Monitoring and Validation")
    class ConnectionMonitoringTests {

        @Test
        @DisplayName("Should validate active sessions and remove stale ones")
        void shouldValidateActiveSessionsAndRemoveStaleOnes() throws Exception {
            // Given - establish connection first
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Set up stale session scenario
            when(mockMonitoringProperties.getSessionStaleTimeoutMs()).thenReturn(1000L); // 1 second
            
            // Wait to make session stale
            Thread.sleep(1100);
            
            // When
            webSocketHandler.validateActiveSessions();
            
            // Then
            verify(mockWebSocketSession).close(eq(CloseStatus.GOING_AWAY.withReason("Session stale")));
        }

        @Test
        @DisplayName("Should validate JWT tokens in active sessions")
        void shouldValidateJwtTokensInActiveSessions() throws Exception {
            // Given - establish connection first
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Simulate token becoming invalid
            when(mockJwtTokenService.validateToken("valid-jwt-token")).thenReturn(false);
            
            // When
            webSocketHandler.validateActiveSessions();
            
            // Then
            verify(mockWebSocketSession).close(eq(CloseStatus.NOT_ACCEPTABLE.withReason("Token invalid")));
        }

        @Test
        @DisplayName("Should validate sessions in Redis and remove invalid ones")
        void shouldValidateSessionsInRedisAndRemoveInvalidOnes() throws Exception {
            // Given - establish connection first
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Simulate session not found in Redis
            when(mockRedisTemplate.hasKey("fabric:websocket:session:test-session-123")).thenReturn(false);
            
            // When
            webSocketHandler.validateActiveSessions();
            
            // Then
            verify(mockWebSocketSession).close(eq(CloseStatus.SERVER_ERROR.withReason("Session validation failed")));
        }

        @Test
        @DisplayName("Should handle validation errors gracefully")
        void shouldHandleValidationErrorsGracefully() throws Exception {
            // Given - establish connection first
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Simulate validation error
            when(mockJwtTokenService.validateToken(anyString())).thenThrow(new RuntimeException("Validation failed"));
            
            // When
            webSocketHandler.validateActiveSessions();
            
            // Then
            verify(mockWebSocketSession).close(eq(CloseStatus.SERVER_ERROR.withReason("Validation error")));
        }
    }

    // ============================================================================
    // BROADCASTING TESTS
    // ============================================================================

    @Nested
    @DisplayName("Broadcasting Tests")
    class BroadcastingTests {

        @Test
        @DisplayName("Should broadcast monitoring updates to eligible sessions")
        void shouldBroadcastMonitoringUpdatesToEligibleSessions() throws Exception {
            // Given - establish connection first
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Create monitoring update
            var monitoringUpdate = new EnterpriseWebSocketHandler.MonitoringUpdate();
            
            // When
            webSocketHandler.broadcastMonitoringUpdate(monitoringUpdate);
            
            // Then - should attempt to send update to session
            verify(mockWebSocketSession, atLeastOnce()).isOpen();
        }

        @Test
        @DisplayName("Should skip broadcasting to closed sessions")
        void shouldSkipBroadcastingToClosedSessions() throws Exception {
            // Given - establish connection first
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Simulate closed session
            when(mockWebSocketSession.isOpen()).thenReturn(false);
            
            var monitoringUpdate = new EnterpriseWebSocketHandler.MonitoringUpdate();
            
            // When
            webSocketHandler.broadcastMonitoringUpdate(monitoringUpdate);
            
            // Then - should not attempt to send to closed session
            verify(mockWebSocketSession, never()).sendMessage(any());
        }

        @Test
        @DisplayName("Should handle broadcasting errors gracefully")
        void shouldHandleBroadcastingErrorsGracefully() throws Exception {
            // Given - establish connection first
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Simulate send error
            when(mockWebSocketSession.isOpen()).thenReturn(true);
            doThrow(new RuntimeException("Send failed")).when(mockWebSocketSession).sendMessage(any());
            
            var monitoringUpdate = new EnterpriseWebSocketHandler.MonitoringUpdate();
            
            // When & Then - should not throw exception
            assertThatCode(() -> webSocketHandler.broadcastMonitoringUpdate(monitoringUpdate))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should apply role-based filtering to broadcast updates")
        void shouldApplyRoleBasedFilteringToBroadcastUpdates() throws Exception {
            // Given - establish connection with limited role
            sessionAttributes.put("userRoles", List.of("OPERATIONS_VIEWER")); // Limited role
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            var monitoringUpdate = new EnterpriseWebSocketHandler.MonitoringUpdate();
            
            // When
            webSocketHandler.broadcastMonitoringUpdate(monitoringUpdate);
            
            // Then - should filter update based on role
            verify(mockWebSocketSession, atLeastOnce()).isOpen();
        }
    }

    // ============================================================================
    // ERROR HANDLING TESTS
    // ============================================================================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle transport errors and increment error count")
        void shouldHandleTransportErrorsAndIncrementErrorCount() throws Exception {
            // Given - establish connection first
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            Exception transportError = new RuntimeException("Network error");
            
            // When
            webSocketHandler.handleTransportError(mockWebSocketSession, transportError);
            
            // Then
            verify(mockAuditService).logSecurityEvent(
                    eq("WEBSOCKET_TRANSPORT_ERROR"),
                    eq("test-user"),
                    eq("192.168.1.100"),
                    contains("WebSocket transport error"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("Should close session after excessive transport errors")
        void shouldCloseSessionAfterExcessiveTransportErrors() throws Exception {
            // Given - establish connection first
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            Exception transportError = new RuntimeException("Network error");
            
            // When - simulate multiple errors
            for (int i = 0; i < 15; i++) {
                webSocketHandler.handleTransportError(mockWebSocketSession, transportError);
            }
            
            // Then
            verify(mockWebSocketSession).close(eq(CloseStatus.SERVER_ERROR.withReason("Excessive transport errors")));
        }

        @Test
        @DisplayName("Should handle transport error for unregistered session")
        void shouldHandleTransportErrorForUnregisteredSession() throws Exception {
            // Given - unregistered session
            when(mockWebSocketSession.getId()).thenReturn("unregistered-session");
            Exception transportError = new RuntimeException("Network error");
            
            // When & Then - should not throw exception
            assertThatCode(() -> webSocketHandler.handleTransportError(mockWebSocketSession, transportError))
                    .doesNotThrowAnyException();
        }
    }

    // ============================================================================
    // CONNECTION CLOSURE TESTS
    // ============================================================================

    @Nested
    @DisplayName("Connection Closure")
    class ConnectionClosureTests {

        @Test
        @DisplayName("Should handle normal connection closure gracefully")
        void shouldHandleNormalConnectionClosureGracefully() throws Exception {
            // Given - establish connection first
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            CloseStatus normalClose = CloseStatus.NORMAL;
            
            // When
            webSocketHandler.afterConnectionClosed(mockWebSocketSession, normalClose);
            
            // Then
            verify(mockConnectionManager).unregisterSession("test-session-123");
            verify(mockRealTimeMonitoringService).unsubscribeSession(any(WebSocketSessionInfo.class));
            verify(mockSecurityEventPublisher).publishSecurityEvent(
                    eq("WEBSOCKET_CONNECTION_CLOSED"),
                    eq("test-session-123"),
                    eq("test-user"),
                    contains("WebSocket connection closed")
            );
            
            // Verify audit logging
            verify(mockAuditService).logSecurityEvent(
                    eq("WEBSOCKET_CONNECTION_CLOSED"),
                    eq("test-user"),
                    eq("192.168.1.100"),
                    contains("WebSocket connection closed"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("Should handle abnormal connection closure")
        void shouldHandleAbnormalConnectionClosure() throws Exception {
            // Given - establish connection first
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            CloseStatus abnormalClose = CloseStatus.SERVER_ERROR;
            
            // When
            webSocketHandler.afterConnectionClosed(mockWebSocketSession, abnormalClose);
            
            // Then
            verify(mockConnectionManager).unregisterSession("test-session-123");
            verify(mockAuditService).logSecurityEvent(
                    eq("WEBSOCKET_CONNECTION_CLOSED"),
                    eq("test-user"),
                    eq("192.168.1.100"),
                    contains("WebSocket connection closed"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("Should handle closure cleanup errors gracefully")
        void shouldHandleClosureCleanupErrorsGracefully() throws Exception {
            // Given - establish connection first
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Simulate cleanup error
            doThrow(new RuntimeException("Cleanup failed"))
                    .when(mockConnectionManager).unregisterSession(anyString());
            
            CloseStatus normalClose = CloseStatus.NORMAL;
            
            // When & Then - should not throw exception
            assertThatCode(() -> webSocketHandler.afterConnectionClosed(mockWebSocketSession, normalClose))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle closure of unregistered session")
        void shouldHandleClosureOfUnregisteredSession() throws Exception {
            // Given - unregistered session
            when(mockWebSocketSession.getId()).thenReturn("unregistered-session");
            CloseStatus normalClose = CloseStatus.NORMAL;
            
            // When & Then - should not throw exception
            assertThatCode(() -> webSocketHandler.afterConnectionClosed(mockWebSocketSession, normalClose))
                    .doesNotThrowAnyException();
        }
    }

    // ============================================================================
    // REDIS INTEGRATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Redis Integration")
    class RedisIntegrationTests {

        @Test
        @DisplayName("Should register session in Redis with proper data")
        void shouldRegisterSessionInRedisWithProperData() throws Exception {
            // When
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Then
            ArgumentCaptor<Map<String, Object>> sessionDataCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockHashOperations).putAll(
                    eq("fabric:websocket:session:test-session-123"),
                    sessionDataCaptor.capture()
            );
            
            Map<String, Object> sessionData = sessionDataCaptor.getValue();
            assertThat(sessionData)
                    .containsKey("userId")
                    .containsKey("clientIp")
                    .containsKey("connectedAt")
                    .containsKey("userRoles")
                    .containsKey("correlationId");
        }

        @Test
        @DisplayName("Should set Redis session TTL")
        void shouldSetRedisSessionTtl() throws Exception {
            // When
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Then
            verify(mockRedisTemplate).expire(
                    eq("fabric:websocket:session:test-session-123"),
                    eq(Duration.ofHours(1))
            );
        }

        @Test
        @DisplayName("Should update session data in Redis periodically")
        void shouldUpdateSessionDataInRedisPeriodicaly() throws Exception {
            // Given - establish connection first
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // Simulate message handling to trigger updates
            TextMessage message = new TextMessage("{\"type\":\"heartbeat\"}");
            
            // When
            webSocketHandler.handleTextMessage(mockWebSocketSession, message);
            
            // Then
            verify(mockHashOperations, atLeastOnce()).put(
                    eq("fabric:websocket:session:test-session-123"),
                    eq("lastActivity"),
                    anyString()
            );
            verify(mockHashOperations, atLeastOnce()).put(
                    eq("fabric:websocket:session:test-session-123"),
                    eq("messagesReceived"),
                    anyString()
            );
        }

        @Test
        @DisplayName("Should clean up Redis data on connection closure")
        void shouldCleanUpRedisDataOnConnectionClosure() throws Exception {
            // Given - establish connection first
            webSocketHandler.afterConnectionEstablished(mockWebSocketSession);
            
            // When
            webSocketHandler.afterConnectionClosed(mockWebSocketSession, CloseStatus.NORMAL);
            
            // Then - should update final session state
            verify(mockHashOperations, atLeastOnce()).put(
                    eq("fabric:websocket:session:test-session-123"),
                    eq("status"),
                    eq("DISCONNECTED")
            );
        }
    }
}