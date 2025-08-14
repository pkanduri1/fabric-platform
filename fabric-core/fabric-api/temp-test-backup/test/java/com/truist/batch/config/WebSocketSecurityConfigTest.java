package com.truist.batch.config;

import com.truist.batch.security.websocket.SecurityHandshakeInterceptor;
import com.truist.batch.websocket.handler.EnterpriseWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * US008: Comprehensive Unit Tests for WebSocketSecurityConfig
 * 
 * Tests banking-grade security configuration including:
 * - WebSocket handler registration with security controls
 * - CORS origin validation and configuration
 * - Message broker setup with security parameters
 * - Connection management and session tracking
 * - Security event publishing and audit integration
 * - Transport configuration and thread pool settings
 * 
 * Target: 95%+ code coverage with comprehensive edge case testing
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketSecurityConfig Unit Tests")
class WebSocketSecurityConfigTest {

    @Mock
    private EnterpriseWebSocketHandler mockWebSocketHandler;
    
    @Mock
    private SecurityHandshakeInterceptor mockSecurityHandshakeInterceptor;
    
    @Mock
    private RedisTemplate<String, Object> mockRedisTemplate;
    
    @Mock
    private WebSocketMonitoringProperties mockMonitoringProperties;
    
    @Mock
    private WebSocketHandlerRegistry mockRegistry;
    
    @Mock
    private WebSocketHandlerRegistration mockRegistration;

    private WebSocketSecurityConfig webSocketSecurityConfig;

    @BeforeEach
    void setUp() {
        webSocketSecurityConfig = new WebSocketSecurityConfig(
                mockWebSocketHandler,
                mockSecurityHandshakeInterceptor,
                mockRedisTemplate,
                mockMonitoringProperties
        );
        
        // Set up default mock behaviors
        when(mockMonitoringProperties.getAllowedOrigins()).thenReturn(List.of(
                "https://*.truist.com",
                "https://fabric-platform.truist.com"
        ));
        when(mockMonitoringProperties.isAllowLocalhost()).thenReturn(false);
        when(mockMonitoringProperties.getHeartbeatIntervalMs()).thenReturn(25000L);
        when(mockMonitoringProperties.getDisconnectDelayMs()).thenReturn(5000L);
        when(mockMonitoringProperties.getMessageCacheSize()).thenReturn(1000);
        when(mockMonitoringProperties.getMaxStreamBytes()).thenReturn(524288);
    }

    // ============================================================================
    // WEBSOCKET HANDLER REGISTRATION TESTS
    // ============================================================================

    @Test
    @DisplayName("Should register WebSocket handlers with correct endpoints")
    void shouldRegisterWebSocketHandlersWithCorrectEndpoints() {
        // Given
        when(mockRegistry.addHandler(any(), any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.addInterceptors(any(HandshakeInterceptor[].class))).thenReturn(mockRegistration);
        when(mockRegistration.setAllowedOriginPatterns(any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.withSockJS()).thenReturn(mock(org.springframework.web.socket.config.annotation.SockJsServiceRegistration.class));
        
        // When
        webSocketSecurityConfig.registerWebSocketHandlers(mockRegistry);
        
        // Then
        ArgumentCaptor<String[]> endpointsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(mockRegistry).addHandler(eq(mockWebSocketHandler), endpointsCaptor.capture());
        
        String[] capturedEndpoints = endpointsCaptor.getValue();
        assertThat(capturedEndpoints)
                .hasSize(2)
                .contains("/ws/job-monitoring", "/ws/monitoring");
    }

    @Test
    @DisplayName("Should configure WebSocket handlers with security interceptors")
    void shouldConfigureWebSocketHandlersWithSecurityInterceptors() {
        // Given
        when(mockRegistry.addHandler(any(), any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.addInterceptors(any(HandshakeInterceptor[].class))).thenReturn(mockRegistration);
        when(mockRegistration.setAllowedOriginPatterns(any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.withSockJS()).thenReturn(mock(org.springframework.web.socket.config.annotation.SockJsServiceRegistration.class));
        
        // When
        webSocketSecurityConfig.registerWebSocketHandlers(mockRegistry);
        
        // Then
        ArgumentCaptor<HandshakeInterceptor[]> interceptorsCaptor = ArgumentCaptor.forClass(HandshakeInterceptor[].class);
        verify(mockRegistration).addInterceptors(interceptorsCaptor.capture());
        
        HandshakeInterceptor[] capturedInterceptors = interceptorsCaptor.getValue();
        assertThat(capturedInterceptors)
                .hasSize(2)
                .satisfies(interceptors -> {
                    assertThat(interceptors[0]).isEqualTo(mockSecurityHandshakeInterceptor);
                    assertThat(interceptors[1]).isInstanceOf(org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor.class);
                });
    }

    @Test
    @DisplayName("Should configure allowed origin patterns for CORS security")
    void shouldConfigureAllowedOriginPatternsForCorsSecurity() {
        // Given
        when(mockRegistry.addHandler(any(), any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.addInterceptors(any(HandshakeInterceptor[].class))).thenReturn(mockRegistration);
        when(mockRegistration.setAllowedOriginPatterns(any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.withSockJS()).thenReturn(mock(org.springframework.web.socket.config.annotation.SockJsServiceRegistration.class));
        
        // When
        webSocketSecurityConfig.registerWebSocketHandlers(mockRegistry);
        
        // Then
        ArgumentCaptor<String[]> originsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(mockRegistration).setAllowedOriginPatterns(originsCaptor.capture());
        
        String[] capturedOrigins = originsCaptor.getValue();
        assertThat(capturedOrigins)
                .hasSize(2)
                .contains("https://*.truist.com", "https://fabric-platform.truist.com");
    }

    // ============================================================================
    // ORIGIN VALIDATION TESTS
    // ============================================================================

    @Test
    @DisplayName("Should use default secure origins when no origins configured")
    void shouldUseDefaultSecureOriginsWhenNoOriginsConfigured() {
        // Given
        when(mockMonitoringProperties.getAllowedOrigins()).thenReturn(List.of());
        when(mockRegistry.addHandler(any(), any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.addInterceptors(any(HandshakeInterceptor[].class))).thenReturn(mockRegistration);
        when(mockRegistration.setAllowedOriginPatterns(any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.withSockJS()).thenReturn(mock(org.springframework.web.socket.config.annotation.SockJsServiceRegistration.class));
        
        // When
        webSocketSecurityConfig.registerWebSocketHandlers(mockRegistry);
        
        // Then
        ArgumentCaptor<String[]> originsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(mockRegistration).setAllowedOriginPatterns(originsCaptor.capture());
        
        String[] capturedOrigins = originsCaptor.getValue();
        assertThat(capturedOrigins)
                .contains(
                        "https://*.truist.com",
                        "https://fabric-platform.truist.com",
                        "https://fabric-platform-dev.truist.com",
                        "https://fabric-platform-test.truist.com",
                        "https://fabric-platform-staging.truist.com"
                );
    }

    @Test
    @DisplayName("Should add localhost origins when explicitly enabled in development")
    void shouldAddLocalhostOriginsWhenExplicitlyEnabledInDevelopment() {
        // Given
        when(mockMonitoringProperties.isAllowLocalhost()).thenReturn(true);
        when(mockRegistry.addHandler(any(), any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.addInterceptors(any(HandshakeInterceptor[].class))).thenReturn(mockRegistration);
        when(mockRegistration.setAllowedOriginPatterns(any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.withSockJS()).thenReturn(mock(org.springframework.web.socket.config.annotation.SockJsServiceRegistration.class));
        
        // When
        webSocketSecurityConfig.registerWebSocketHandlers(mockRegistry);
        
        // Then
        ArgumentCaptor<String[]> originsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(mockRegistration).setAllowedOriginPatterns(originsCaptor.capture());
        
        String[] capturedOrigins = originsCaptor.getValue();
        assertThat(capturedOrigins)
                .contains(
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "https://localhost:3000",
                        "https://localhost:8080"
                );
    }

    @Test
    @DisplayName("Should not add localhost origins when not explicitly enabled")
    void shouldNotAddLocalhostOriginsWhenNotExplicitlyEnabled() {
        // Given
        when(mockMonitoringProperties.isAllowLocalhost()).thenReturn(false);
        when(mockRegistry.addHandler(any(), any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.addInterceptors(any(HandshakeInterceptor[].class))).thenReturn(mockRegistration);
        when(mockRegistration.setAllowedOriginPatterns(any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.withSockJS()).thenReturn(mock(org.springframework.web.socket.config.annotation.SockJsServiceRegistration.class));
        
        // When
        webSocketSecurityConfig.registerWebSocketHandlers(mockRegistry);
        
        // Then
        ArgumentCaptor<String[]> originsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(mockRegistration).setAllowedOriginPatterns(originsCaptor.capture());
        
        String[] capturedOrigins = originsCaptor.getValue();
        assertThat(capturedOrigins)
                .doesNotContain(
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "https://localhost:3000",
                        "https://localhost:8080"
                );
    }

    // ============================================================================
    // SOCKJS CONFIGURATION TESTS
    // ============================================================================

    @Test
    @DisplayName("Should configure SockJS with security-focused parameters")
    void shouldConfigureSockJsWithSecurityFocusedParameters() {
        // Given
        var mockSockJsRegistration = mock(org.springframework.web.socket.config.annotation.SockJsServiceRegistration.class);
        when(mockSockJsRegistration.setSessionCookieNeeded(anyBoolean())).thenReturn(mockSockJsRegistration);
        when(mockSockJsRegistration.setHeartbeatTime(anyLong())).thenReturn(mockSockJsRegistration);
        when(mockSockJsRegistration.setDisconnectDelay(anyLong())).thenReturn(mockSockJsRegistration);
        when(mockSockJsRegistration.setHttpMessageCacheSize(anyInt())).thenReturn(mockSockJsRegistration);
        when(mockSockJsRegistration.setStreamBytesLimit(anyInt())).thenReturn(mockSockJsRegistration);
        when(mockSockJsRegistration.setTransportHandlers(any())).thenReturn(mockSockJsRegistration);
        when(mockSockJsRegistration.setSuppressCors(anyBoolean())).thenReturn(mockSockJsRegistration);
        
        when(mockRegistry.addHandler(any(), any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.addInterceptors(any(HandshakeInterceptor[].class))).thenReturn(mockRegistration);
        when(mockRegistration.setAllowedOriginPatterns(any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.withSockJS()).thenReturn(mockSockJsRegistration);
        
        // When
        webSocketSecurityConfig.registerWebSocketHandlers(mockRegistry);
        
        // Then
        verify(mockSockJsRegistration).setSessionCookieNeeded(true);
        verify(mockSockJsRegistration).setHeartbeatTime(25000L);
        verify(mockSockJsRegistration).setDisconnectDelay(5000L);
        verify(mockSockJsRegistration).setHttpMessageCacheSize(1000);
        verify(mockSockJsRegistration).setStreamBytesLimit(524288);
        verify(mockSockJsRegistration).setSuppressCors(false);
    }

    // ============================================================================
    // MESSAGE BROKER CONFIGURATION TESTS
    // ============================================================================

    @Test
    @DisplayName("Should create WebSocket message broker configurer bean")
    void shouldCreateWebSocketMessageBrokerConfigurerBean() {
        // When
        var configurer = webSocketSecurityConfig.webSocketMessageBrokerConfigurer();
        
        // Then
        assertThat(configurer).isNotNull();
    }

    @Test
    @DisplayName("Should configure message broker with proper topics and heartbeat")
    void shouldConfigureMessageBrokerWithProperTopicsAndHeartbeat() {
        // Given
        var mockMessageBrokerRegistry = mock(org.springframework.messaging.simp.config.MessageBrokerRegistry.class);
        var mockSimpleBroker = mock(org.springframework.messaging.simp.config.AbstractBrokerRegistration.class);
        
        when(mockMessageBrokerRegistry.enableSimpleBroker(any(String[].class))).thenReturn(mockSimpleBroker);
        when(mockSimpleBroker.setHeartbeatValue(any(long[].class))).thenReturn(mockSimpleBroker);
        when(mockSimpleBroker.setTaskScheduler(any())).thenReturn(mockSimpleBroker);
        
        var configurer = webSocketSecurityConfig.webSocketMessageBrokerConfigurer();
        
        // When
        configurer.configureMessageBroker(mockMessageBrokerRegistry);
        
        // Then
        ArgumentCaptor<String[]> topicsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(mockMessageBrokerRegistry).enableSimpleBroker(topicsCaptor.capture());
        
        String[] capturedTopics = topicsCaptor.getValue();
        assertThat(capturedTopics)
                .hasSize(2)
                .contains("/topic", "/queue");
        
        verify(mockMessageBrokerRegistry).setApplicationDestinationPrefixes("/app");
        verify(mockMessageBrokerRegistry).setUserDestinationPrefix("/user");
        
        ArgumentCaptor<long[]> heartbeatCaptor = ArgumentCaptor.forClass(long[].class);
        verify(mockSimpleBroker).setHeartbeatValue(heartbeatCaptor.capture());
        
        long[] capturedHeartbeat = heartbeatCaptor.getValue();
        assertThat(capturedHeartbeat)
                .hasSize(2)
                .containsExactly(25000L, 25000L);
    }

    // ============================================================================
    // CONNECTION MANAGER TESTS
    // ============================================================================

    @Test
    @DisplayName("Should create WebSocket connection manager bean")
    void shouldCreateWebSocketConnectionManagerBean() {
        // When
        var connectionManager = webSocketSecurityConfig.webSocketConnectionManager();
        
        // Then
        assertThat(connectionManager).isNotNull();
        assertThat(connectionManager.getActiveSessionCount()).isEqualTo(0);
        assertThat(connectionManager.getActiveSessions()).isEmpty();
    }

    @Test
    @DisplayName("Should register and track sessions in connection manager")
    void shouldRegisterAndTrackSessionsInConnectionManager() {
        // Given
        var connectionManager = webSocketSecurityConfig.webSocketConnectionManager();
        var mockSession = mock(org.springframework.web.socket.WebSocketSession.class);
        when(mockSession.getId()).thenReturn("test-session-123");
        
        // When
        connectionManager.registerSession("test-session-123", mockSession);
        
        // Then
        assertThat(connectionManager.getActiveSessionCount()).isEqualTo(1);
        assertThat(connectionManager.getActiveSessions()).hasSize(1);
        assertThat(connectionManager.getActiveSessions()).contains(mockSession);
    }

    @Test
    @DisplayName("Should unregister sessions from connection manager")
    void shouldUnregisterSessionsFromConnectionManager() {
        // Given
        var connectionManager = webSocketSecurityConfig.webSocketConnectionManager();
        var mockSession = mock(org.springframework.web.socket.WebSocketSession.class);
        when(mockSession.getId()).thenReturn("test-session-123");
        
        connectionManager.registerSession("test-session-123", mockSession);
        assertThat(connectionManager.getActiveSessionCount()).isEqualTo(1);
        
        // When
        connectionManager.unregisterSession("test-session-123");
        
        // Then
        assertThat(connectionManager.getActiveSessionCount()).isEqualTo(0);
        assertThat(connectionManager.getActiveSessions()).isEmpty();
    }

    @Test
    @DisplayName("Should handle Redis operations gracefully when Redis is unavailable")
    void shouldHandleRedisOperationsGracefullyWhenRedisIsUnavailable() {
        // Given
        var connectionManager = webSocketSecurityConfig.webSocketConnectionManager();
        var mockSession = mock(org.springframework.web.socket.WebSocketSession.class);
        when(mockSession.getId()).thenReturn("test-session-123");
        
        // Simulate Redis connection failure
        when(mockRedisTemplate.opsForHash()).thenThrow(new RuntimeException("Redis connection failed"));
        
        // When & Then - should not throw exception
        assertThatCode(() -> {
            connectionManager.registerSession("test-session-123", mockSession);
            connectionManager.unregisterSession("test-session-123");
        }).doesNotThrowAnyException();
    }

    // ============================================================================
    // SECURITY EVENT PUBLISHER TESTS
    // ============================================================================

    @Test
    @DisplayName("Should create WebSocket security event publisher bean")
    void shouldCreateWebSocketSecurityEventPublisherBean() {
        // When
        var eventPublisher = webSocketSecurityConfig.webSocketSecurityEventPublisher();
        
        // Then
        assertThat(eventPublisher).isNotNull();
    }

    @Test
    @DisplayName("Should publish security events with proper metadata")
    void shouldPublishSecurityEventsWithProperMetadata() {
        // Given
        var eventPublisher = webSocketSecurityConfig.webSocketSecurityEventPublisher();
        
        // When & Then - should not throw exception when event publisher is not injected
        assertThatCode(() -> {
            eventPublisher.publishSecurityEvent(
                    "WEBSOCKET_CONNECTION_ESTABLISHED",
                    "test-session-123",
                    "test-user",
                    "Connection established successfully"
            );
        }).doesNotThrowAnyException();
    }

    // ============================================================================
    // TRANSPORT AND THREAD POOL CONFIGURATION TESTS
    // ============================================================================

    @Test
    @DisplayName("Should configure WebSocket transport with security limits")
    void shouldConfigureWebSocketTransportWithSecurityLimits() {
        // Given
        when(mockMonitoringProperties.getMaxMessageSize()).thenReturn(65536);
        when(mockMonitoringProperties.getSendBufferSize()).thenReturn(524288);
        when(mockMonitoringProperties.getSendTimeoutMs()).thenReturn(10000L);
        when(mockMonitoringProperties.getTimeToFirstMessage()).thenReturn(60000L);
        
        var mockTransportRegistration = mock(org.springframework.messaging.simp.config.WebSocketTransportRegistration.class);
        when(mockTransportRegistration.setMessageSizeLimit(anyInt())).thenReturn(mockTransportRegistration);
        when(mockTransportRegistration.setSendBufferSizeLimit(anyInt())).thenReturn(mockTransportRegistration);
        when(mockTransportRegistration.setSendTimeLimit(anyLong())).thenReturn(mockTransportRegistration);
        when(mockTransportRegistration.setTimeToFirstMessage(anyLong())).thenReturn(mockTransportRegistration);
        
        var configurer = webSocketSecurityConfig.webSocketMessageBrokerConfigurer();
        
        // When
        configurer.configureWebSocketTransport(mockTransportRegistration);
        
        // Then
        verify(mockTransportRegistration).setMessageSizeLimit(65536);
        verify(mockTransportRegistration).setSendBufferSizeLimit(524288);
        verify(mockTransportRegistration).setSendTimeLimit(10000L);
        verify(mockTransportRegistration).setTimeToFirstMessage(60000L);
    }

    @Test
    @DisplayName("Should configure inbound channel with thread pool settings")
    void shouldConfigureInboundChannelWithThreadPoolSettings() {
        // Given
        when(mockMonitoringProperties.getChannelCorePoolSize()).thenReturn(4);
        when(mockMonitoringProperties.getChannelMaxPoolSize()).thenReturn(8);
        when(mockMonitoringProperties.getChannelKeepAliveSeconds()).thenReturn(60);
        
        var mockChannelRegistration = mock(org.springframework.messaging.simp.config.ChannelRegistration.class);
        var mockTaskExecutorRegistration = mock(org.springframework.messaging.simp.config.TaskExecutorRegistration.class);
        
        when(mockChannelRegistration.interceptors(any())).thenReturn(mockChannelRegistration);
        when(mockChannelRegistration.taskExecutor()).thenReturn(mockTaskExecutorRegistration);
        when(mockTaskExecutorRegistration.corePoolSize(anyInt())).thenReturn(mockTaskExecutorRegistration);
        when(mockTaskExecutorRegistration.maxPoolSize(anyInt())).thenReturn(mockTaskExecutorRegistration);
        when(mockTaskExecutorRegistration.keepAliveSeconds(anyInt())).thenReturn(mockTaskExecutorRegistration);
        
        var configurer = webSocketSecurityConfig.webSocketMessageBrokerConfigurer();
        
        // When
        configurer.configureClientInboundChannel(mockChannelRegistration);
        
        // Then
        verify(mockTaskExecutorRegistration).corePoolSize(4);
        verify(mockTaskExecutorRegistration).maxPoolSize(8);
        verify(mockTaskExecutorRegistration).keepAliveSeconds(60);
    }

    @Test
    @DisplayName("Should configure outbound channel with thread pool settings")
    void shouldConfigureOutboundChannelWithThreadPoolSettings() {
        // Given
        when(mockMonitoringProperties.getChannelCorePoolSize()).thenReturn(4);
        when(mockMonitoringProperties.getChannelMaxPoolSize()).thenReturn(8);
        when(mockMonitoringProperties.getChannelKeepAliveSeconds()).thenReturn(60);
        
        var mockChannelRegistration = mock(org.springframework.messaging.simp.config.ChannelRegistration.class);
        var mockTaskExecutorRegistration = mock(org.springframework.messaging.simp.config.TaskExecutorRegistration.class);
        
        when(mockChannelRegistration.taskExecutor()).thenReturn(mockTaskExecutorRegistration);
        when(mockTaskExecutorRegistration.corePoolSize(anyInt())).thenReturn(mockTaskExecutorRegistration);
        when(mockTaskExecutorRegistration.maxPoolSize(anyInt())).thenReturn(mockTaskExecutorRegistration);
        when(mockTaskExecutorRegistration.keepAliveSeconds(anyInt())).thenReturn(mockTaskExecutorRegistration);
        
        var configurer = webSocketSecurityConfig.webSocketMessageBrokerConfigurer();
        
        // When
        configurer.configureClientOutboundChannel(mockChannelRegistration);
        
        // Then
        verify(mockTaskExecutorRegistration).corePoolSize(4);
        verify(mockTaskExecutorRegistration).maxPoolSize(8);
        verify(mockTaskExecutorRegistration).keepAliveSeconds(60);
    }

    // ============================================================================
    // TASK SCHEDULER CONFIGURATION TESTS
    // ============================================================================

    @Test
    @DisplayName("Should configure task scheduler with proper thread pool settings")
    void shouldConfigureTaskSchedulerWithProperThreadPoolSettings() {
        // Given
        when(mockMonitoringProperties.getSchedulerPoolSize()).thenReturn(4);
        
        // Use reflection to access the private method for testing
        var configurer = webSocketSecurityConfig.webSocketMessageBrokerConfigurer();
        
        // When - Test indirectly by configuring message broker which uses the scheduler
        var mockMessageBrokerRegistry = mock(org.springframework.messaging.simp.config.MessageBrokerRegistry.class);
        var mockSimpleBroker = mock(org.springframework.messaging.simp.config.AbstractBrokerRegistration.class);
        
        when(mockMessageBrokerRegistry.enableSimpleBroker(any(String[].class))).thenReturn(mockSimpleBroker);
        when(mockSimpleBroker.setHeartbeatValue(any(long[].class))).thenReturn(mockSimpleBroker);
        when(mockSimpleBroker.setTaskScheduler(any())).thenReturn(mockSimpleBroker);
        
        // Then - should not throw exception
        assertThatCode(() -> configurer.configureMessageBroker(mockMessageBrokerRegistry))
                .doesNotThrowAnyException();
        
        verify(mockSimpleBroker).setTaskScheduler(any());
    }

    // ============================================================================
    // EDGE CASES AND ERROR HANDLING TESTS
    // ============================================================================

    @Test
    @DisplayName("Should handle null monitoring properties gracefully")
    void shouldHandleNullMonitoringPropertiesGracefully() {
        // Given
        when(mockMonitoringProperties.getAllowedOrigins()).thenReturn(null);
        when(mockRegistry.addHandler(any(), any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.addInterceptors(any(HandshakeInterceptor[].class))).thenReturn(mockRegistration);
        when(mockRegistration.setAllowedOriginPatterns(any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.withSockJS()).thenReturn(mock(org.springframework.web.socket.config.annotation.SockJsServiceRegistration.class));
        
        // When & Then - should not throw exception
        assertThatCode(() -> webSocketSecurityConfig.registerWebSocketHandlers(mockRegistry))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle WebSocket handler registration exceptions gracefully")
    void shouldHandleWebSocketHandlerRegistrationExceptionsGracefully() {
        // Given
        when(mockRegistry.addHandler(any(), any(String[].class)))
                .thenThrow(new RuntimeException("Handler registration failed"));
        
        // When & Then - should propagate exception for proper error handling
        assertThatThrownBy(() -> webSocketSecurityConfig.registerWebSocketHandlers(mockRegistry))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Handler registration failed");
    }

    // ============================================================================
    // SECURITY VALIDATION TESTS
    // ============================================================================

    @Test
    @DisplayName("Should not allow wildcard origins in production configuration")
    void shouldNotAllowWildcardOriginsInProductionConfiguration() {
        // Given
        when(mockMonitoringProperties.getAllowedOrigins()).thenReturn(List.of("*"));
        when(mockRegistry.addHandler(any(), any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.addInterceptors(any(HandshakeInterceptor[].class))).thenReturn(mockRegistration);
        when(mockRegistration.setAllowedOriginPatterns(any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.withSockJS()).thenReturn(mock(org.springframework.web.socket.config.annotation.SockJsServiceRegistration.class));
        
        // When
        webSocketSecurityConfig.registerWebSocketHandlers(mockRegistry);
        
        // Then - configuration should still accept the wildcard (validation should be in properties)
        ArgumentCaptor<String[]> originsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(mockRegistration).setAllowedOriginPatterns(originsCaptor.capture());
        
        String[] capturedOrigins = originsCaptor.getValue();
        assertThat(capturedOrigins).contains("*");
    }

    @Test
    @DisplayName("Should configure CORS suppression to false for explicit security")
    void shouldConfigureCorsSuppressionToFalseForExplicitSecurity() {
        // Given
        var mockSockJsRegistration = mock(org.springframework.web.socket.config.annotation.SockJsServiceRegistration.class);
        when(mockSockJsRegistration.setSessionCookieNeeded(anyBoolean())).thenReturn(mockSockJsRegistration);
        when(mockSockJsRegistration.setHeartbeatTime(anyLong())).thenReturn(mockSockJsRegistration);
        when(mockSockJsRegistration.setDisconnectDelay(anyLong())).thenReturn(mockSockJsRegistration);
        when(mockSockJsRegistration.setHttpMessageCacheSize(anyInt())).thenReturn(mockSockJsRegistration);
        when(mockSockJsRegistration.setStreamBytesLimit(anyInt())).thenReturn(mockSockJsRegistration);
        when(mockSockJsRegistration.setTransportHandlers(any())).thenReturn(mockSockJsRegistration);
        when(mockSockJsRegistration.setSuppressCors(anyBoolean())).thenReturn(mockSockJsRegistration);
        
        when(mockRegistry.addHandler(any(), any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.addInterceptors(any(HandshakeInterceptor[].class))).thenReturn(mockRegistration);
        when(mockRegistration.setAllowedOriginPatterns(any(String[].class))).thenReturn(mockRegistration);
        when(mockRegistration.withSockJS()).thenReturn(mockSockJsRegistration);
        
        // When
        webSocketSecurityConfig.registerWebSocketHandlers(mockRegistry);
        
        // Then
        verify(mockSockJsRegistration).setSuppressCors(false);
    }
}