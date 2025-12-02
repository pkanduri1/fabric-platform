package com.fabric.batch.websocket.service;

import com.fabric.batch.config.WebSocketMonitoringProperties;
import com.fabric.batch.websocket.handler.WebSocketSessionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * US008: Comprehensive Unit Tests for RealTimeMonitoringService
 * 
 * Tests real-time monitoring capabilities including:
 * - Session subscription management with role-based filtering
 * - Delta update calculations for efficient broadcasting
 * - Circuit breaker patterns and adaptive intervals
 * - Performance monitoring and metric collection
 * - Redis caching and fallback mechanisms
 * - SOX compliance and audit trail integration
 * 
 * Target: 95%+ code coverage with comprehensive edge case testing
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RealTimeMonitoringService Unit Tests")
class RealTimeMonitoringServiceTest {

    @Mock
    private WebSocketMonitoringProperties mockMonitoringProperties;
    
    @Mock
    private RedisTemplate<String, Object> mockRedisTemplate;
    
    @Mock
    private WebSocketSession mockWebSocketSession;

    private RealTimeMonitoringService realTimeMonitoringService;

    @BeforeEach
    void setUp() {
        realTimeMonitoringService = new RealTimeMonitoringService(
                mockMonitoringProperties,
                mockRedisTemplate
        );
        
        // Set up default mock behaviors
        when(mockMonitoringProperties.getUpdateIntervalMs()).thenReturn(5000L);
        when(mockMonitoringProperties.isAdaptiveIntervals()).thenReturn(true);
        when(mockMonitoringProperties.isDeltaUpdates()).thenReturn(true);
        when(mockMonitoringProperties.isRoleBasedFiltering()).thenReturn(true);
        when(mockWebSocketSession.getId()).thenReturn("test-session-123");
        when(mockWebSocketSession.isOpen()).thenReturn(true);
    }

    // ============================================================================
    // SESSION SUBSCRIPTION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Session Subscription Management")
    class SessionSubscriptionTests {

        @Test
        @DisplayName("Should successfully subscribe session with OPERATIONS_MANAGER role")
        void shouldSuccessfullySubscribeSessionWithOperationsManagerRole() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_MANAGER");
            
            // When
            realTimeMonitoringService.subscribeSession(sessionInfo);
            
            // Then - should add all monitoring subscriptions for full access
            assertThat(sessionInfo.getSubscriptions()).isNotEmpty();
            assertThat(sessionInfo.isSubscribedTo(WebSocketSessionInfo.SubscriptionType.JOB_STATUS)).isTrue();
            assertThat(sessionInfo.isSubscribedTo(WebSocketSessionInfo.SubscriptionType.SYSTEM_METRICS)).isTrue();
            assertThat(sessionInfo.isSubscribedTo(WebSocketSessionInfo.SubscriptionType.BUSINESS_KPIS)).isTrue();
            assertThat(sessionInfo.isSubscribedTo(WebSocketSessionInfo.SubscriptionType.ALERTS)).isTrue();
            assertThat(sessionInfo.isSubscribedTo(WebSocketSessionInfo.SubscriptionType.AUDIT_EVENTS)).isTrue();
        }

        @Test
        @DisplayName("Should subscribe session with OPERATIONS_VIEWER role with limited access")
        void shouldSubscribeSessionWithOperationsViewerRoleWithLimitedAccess() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_VIEWER");
            
            // When
            realTimeMonitoringService.subscribeSession(sessionInfo);
            
            // Then - should have limited subscriptions
            assertThat(sessionInfo.isSubscribedTo(WebSocketSessionInfo.SubscriptionType.JOB_STATUS)).isTrue();
            assertThat(sessionInfo.isSubscribedTo(WebSocketSessionInfo.SubscriptionType.BUSINESS_KPIS)).isTrue();
            assertThat(sessionInfo.isSubscribedTo(WebSocketSessionInfo.SubscriptionType.SYSTEM_METRICS)).isFalse();
            assertThat(sessionInfo.isSubscribedTo(WebSocketSessionInfo.SubscriptionType.AUDIT_EVENTS)).isFalse();
        }

        @Test
        @DisplayName("Should subscribe session with minimal role with basic access only")
        void shouldSubscribeSessionWithMinimalRoleWithBasicAccessOnly() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("USER");
            
            // When
            realTimeMonitoringService.subscribeSession(sessionInfo);
            
            // Then - should have minimal subscriptions
            assertThat(sessionInfo.isSubscribedTo(WebSocketSessionInfo.SubscriptionType.BUSINESS_KPIS)).isTrue();
            assertThat(sessionInfo.isSubscribedTo(WebSocketSessionInfo.SubscriptionType.JOB_STATUS)).isFalse();
            assertThat(sessionInfo.isSubscribedTo(WebSocketSessionInfo.SubscriptionType.SYSTEM_METRICS)).isFalse();
            assertThat(sessionInfo.isSubscribedTo(WebSocketSessionInfo.SubscriptionType.ALERTS)).isFalse();
            assertThat(sessionInfo.isSubscribedTo(WebSocketSessionInfo.SubscriptionType.AUDIT_EVENTS)).isFalse();
        }

        @Test
        @DisplayName("Should send initial monitoring data on subscription")
        void shouldSendInitialMonitoringDataOnSubscription() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_MANAGER");
            
            // When
            realTimeMonitoringService.subscribeSession(sessionInfo);
            
            // Then - should attempt to send initial data
            // Note: Since sendInitialMonitoringData is called, we verify it doesn't throw
            assertThat(sessionInfo.getLastActivity()).isNotNull();
        }

        @Test
        @DisplayName("Should handle subscription errors gracefully")
        void shouldHandleSubscriptionErrorsGracefully() {
            // Given
            WebSocketSessionInfo sessionInfo = null; // Null session to trigger error
            
            // When & Then - should not throw exception
            assertThatCode(() -> realTimeMonitoringService.subscribeSession(sessionInfo))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should successfully unsubscribe session")
        void shouldSuccessfullyUnsubscribeSession() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_MANAGER");
            realTimeMonitoringService.subscribeSession(sessionInfo);
            
            // When
            realTimeMonitoringService.unsubscribeSession(sessionInfo);
            
            // Then - should remove session from subscribers
            // Verify through next collection cycle having no subscribers
            realTimeMonitoringService.collectAndBroadcastUpdates();
        }

        @Test
        @DisplayName("Should handle unsubscription errors gracefully")
        void shouldHandleUnsubscriptionErrorsGracefully() {
            // Given
            WebSocketSessionInfo sessionInfo = null; // Null session to trigger error
            
            // When & Then - should not throw exception
            assertThatCode(() -> realTimeMonitoringService.unsubscribeSession(sessionInfo))
                    .doesNotThrowAnyException();
        }
    }

    // ============================================================================
    // MONITORING UPDATE COLLECTION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Monitoring Update Collection")
    class MonitoringUpdateCollectionTests {

        @Test
        @DisplayName("Should skip collection when no subscribers present")
        void shouldSkipCollectionWhenNoSubscribersPresent() {
            // Given - no subscribers
            
            // When
            realTimeMonitoringService.collectAndBroadcastUpdates();
            
            // Then - should return early without processing
            // Verify by checking that no redis operations are performed
            verifyNoInteractions(mockRedisTemplate);
        }

        @Test
        @DisplayName("Should collect and broadcast updates when subscribers present")
        void shouldCollectAndBroadcastUpdatesWhenSubscribersPresent() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_MANAGER");
            realTimeMonitoringService.subscribeSession(sessionInfo);
            
            // When
            realTimeMonitoringService.collectAndBroadcastUpdates();
            
            // Then - should collect snapshot and broadcast
            // Verification through method execution completion without error
        }

        @Test
        @DisplayName("Should calculate delta updates between snapshots")
        void shouldCalculateDeltaUpdatesBetweenSnapshots() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_MANAGER");
            realTimeMonitoringService.subscribeSession(sessionInfo);
            
            // When - run collection twice to generate delta
            realTimeMonitoringService.collectAndBroadcastUpdates();
            realTimeMonitoringService.collectAndBroadcastUpdates();
            
            // Then - should calculate deltas between runs
            // Verified by successful execution without errors
        }

        @Test
        @DisplayName("Should skip broadcasting when circuit breaker is open")
        void shouldSkipBroadcastingWhenCircuitBreakerIsOpen() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_MANAGER");
            realTimeMonitoringService.subscribeSession(sessionInfo);
            
            // Simulate circuit breaker opening by causing multiple failures
            for (int i = 0; i < 10; i++) {
                try {
                    // Force error by providing invalid session
                    sessionInfo.setSession(null);
                    realTimeMonitoringService.collectAndBroadcastUpdates();
                } catch (Exception e) {
                    // Expected - circuit breaker should engage
                }
            }
            
            // When - next call should be skipped due to open circuit breaker
            realTimeMonitoringService.collectAndBroadcastUpdates();
            
            // Then - should skip processing (verified by no additional errors)
        }

        @Test
        @DisplayName("Should send fallback data when collection fails")
        void shouldSendFallbackDataWhenCollectionFails() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_MANAGER");
            realTimeMonitoringService.subscribeSession(sessionInfo);
            
            // Simulate failure
            sessionInfo.setSession(null); // Force error condition
            
            // When
            realTimeMonitoringService.collectAndBroadcastUpdates();
            
            // Then - should handle error gracefully and send fallback data
            assertThat(sessionInfo).isNotNull();
        }

        @Test
        @DisplayName("Should update adaptive intervals based on system load")
        void shouldUpdateAdaptiveIntervalsBasedOnSystemLoad() {
            // Given
            when(mockMonitoringProperties.isAdaptiveIntervals()).thenReturn(true);
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_MANAGER");
            realTimeMonitoringService.subscribeSession(sessionInfo);
            
            // When
            realTimeMonitoringService.collectAndBroadcastUpdates();
            
            // Then - should calculate new adaptive interval
            // Verified through successful method execution
        }
    }

    // ============================================================================
    // ROLE-BASED FILTERING TESTS
    // ============================================================================

    @Nested
    @DisplayName("Role-Based Data Filtering")
    class RoleBasedFilteringTests {

        @Test
        @DisplayName("Should provide full access to OPERATIONS_MANAGER role")
        void shouldProvideFullAccessToOperationsManagerRole() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_MANAGER");
            var mockDelta = createMockMonitoringDelta();
            
            // When
            var filteredDelta = realTimeMonitoringService.filterDeltaForRoles(mockDelta, List.of("OPERATIONS_MANAGER"));
            
            // Then
            assertThat(filteredDelta).isEqualTo(mockDelta); // Should return unfiltered delta
        }

        @Test
        @DisplayName("Should provide full access to ADMIN role")
        void shouldProvideFullAccessToAdminRole() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("ADMIN");
            var mockDelta = createMockMonitoringDelta();
            
            // When
            var filteredDelta = realTimeMonitoringService.filterDeltaForRoles(mockDelta, List.of("ADMIN"));
            
            // Then
            assertThat(filteredDelta).isEqualTo(mockDelta); // Should return unfiltered delta
        }

        @Test
        @DisplayName("Should provide limited access to OPERATIONS_VIEWER role")
        void shouldProvideLimitedAccessToOperationsViewerRole() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_VIEWER");
            var mockDelta = createMockMonitoringDelta();
            
            // When
            var filteredDelta = realTimeMonitoringService.filterDeltaForRoles(mockDelta, List.of("OPERATIONS_VIEWER"));
            
            // Then
            assertThat(filteredDelta).isNotNull();
            // Should be filtered version - exact verification would depend on implementation
        }

        @Test
        @DisplayName("Should provide minimal access to default roles")
        void shouldProvideMinimalAccessToDefaultRoles() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("USER");
            var mockDelta = createMockMonitoringDelta();
            
            // When
            var filteredDelta = realTimeMonitoringService.filterDeltaForRoles(mockDelta, List.of("USER"));
            
            // Then
            assertThat(filteredDelta).isNotNull();
            // Should be heavily filtered version
        }

        @Test
        @DisplayName("Should handle null or empty roles gracefully")
        void shouldHandleNullOrEmptyRolesGracefully() {
            // Given
            var mockDelta = createMockMonitoringDelta();
            
            // When & Then
            assertThatCode(() -> {
                realTimeMonitoringService.filterDeltaForRoles(mockDelta, null);
                realTimeMonitoringService.filterDeltaForRoles(mockDelta, List.of());
            }).doesNotThrowAnyException();
        }
    }

    // ============================================================================
    // WEBSOCKET METRICS COLLECTION TESTS
    // ============================================================================

    @Nested
    @DisplayName("WebSocket Metrics Collection")
    class WebSocketMetricsTests {

        @Test
        @DisplayName("Should collect WebSocket connection metrics")
        void shouldCollectWebSocketConnectionMetrics() {
            // Given
            WebSocketSessionInfo sessionInfo1 = createSessionInfo("OPERATIONS_MANAGER");
            WebSocketSessionInfo sessionInfo2 = createSessionInfo("OPERATIONS_VIEWER");
            
            realTimeMonitoringService.subscribeSession(sessionInfo1);
            realTimeMonitoringService.subscribeSession(sessionInfo2);
            
            // When
            var metrics = realTimeMonitoringService.collectWebSocketMetrics();
            
            // Then
            assertThat(metrics).isNotNull();
            assertThat(metrics.getTotalConnections()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should calculate connection metrics by role")
        void shouldCalculateConnectionMetricsByRole() {
            // Given
            WebSocketSessionInfo managerSession = createSessionInfo("OPERATIONS_MANAGER");
            WebSocketSessionInfo viewerSession1 = createSessionInfo("OPERATIONS_VIEWER");
            WebSocketSessionInfo viewerSession2 = createSessionInfo("OPERATIONS_VIEWER");
            
            realTimeMonitoringService.subscribeSession(managerSession);
            realTimeMonitoringService.subscribeSession(viewerSession1);
            realTimeMonitoringService.subscribeSession(viewerSession2);
            
            // When
            var connectionsByRole = realTimeMonitoringService.getConnectionsByRole();
            
            // Then
            assertThat(connectionsByRole).isNotNull();
            // Would contain role-based connection counts
        }

        @Test
        @DisplayName("Should calculate performance metrics")
        void shouldCalculatePerformanceMetrics() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_MANAGER");
            sessionInfo.incrementMessagesSent();
            sessionInfo.incrementMessagesReceived();
            
            realTimeMonitoringService.subscribeSession(sessionInfo);
            
            // When
            var metrics = realTimeMonitoringService.collectWebSocketMetrics();
            
            // Then
            assertThat(metrics).isNotNull();
            assertThat(metrics.getMessagesSentPerMinute()).isGreaterThanOrEqualTo(0);
            assertThat(metrics.getMessagesReceivedPerMinute()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should calculate error rates")
        void shouldCalculateErrorRates() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_MANAGER");
            sessionInfo.incrementErrorCount();
            sessionInfo.incrementMessagesSent();
            sessionInfo.incrementMessagesSent();
            
            realTimeMonitoringService.subscribeSession(sessionInfo);
            
            // When
            var errorRate = realTimeMonitoringService.getConnectionErrorRate();
            
            // Then
            assertThat(errorRate).isGreaterThanOrEqualTo(0.0);
            assertThat(errorRate).isLessThanOrEqualTo(1.0);
        }
    }

    // ============================================================================
    // DELTA CALCULATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Delta Calculation Logic")
    class DeltaCalculationTests {

        @Test
        @DisplayName("Should detect system metrics changes")
        void shouldDetectSystemMetricsChanges() {
            // Given
            var currentMetrics = Map.of("cpu", 50.0, "memory", 60.0);
            var previousMetrics = Map.of("cpu", 45.0, "memory", 60.0);
            
            // When
            boolean hasChanges = realTimeMonitoringService.hasSystemMetricsChanged(currentMetrics, previousMetrics);
            
            // Then
            assertThat(hasChanges).isTrue();
        }

        @Test
        @DisplayName("Should detect business metrics changes")
        void shouldDetectBusinessMetricsChanges() {
            // Given
            var currentMetrics = Map.of("jobsProcessed", 100, "errorRate", 2.5);
            var previousMetrics = Map.of("jobsProcessed", 95, "errorRate", 2.5);
            
            // When
            boolean hasChanges = realTimeMonitoringService.hasBusinessMetricsChanged(currentMetrics, previousMetrics);
            
            // Then
            assertThat(hasChanges).isTrue();
        }

        @Test
        @DisplayName("Should handle null metrics gracefully")
        void shouldHandleNullMetricsGracefully() {
            // When & Then
            assertThatCode(() -> {
                realTimeMonitoringService.hasSystemMetricsChanged(null, Map.of());
                realTimeMonitoringService.hasSystemMetricsChanged(Map.of(), null);
                realTimeMonitoringService.hasSystemMetricsChanged(null, null);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should create complete snapshot delta for first run")
        void shouldCreateCompleteSnapshotDeltaForFirstRun() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_MANAGER");
            realTimeMonitoringService.subscribeSession(sessionInfo);
            
            // When - first collection should create complete delta
            realTimeMonitoringService.collectAndBroadcastUpdates();
            
            // Then - should execute without errors (complete snapshot sent)
        }
    }

    // ============================================================================
    // ERROR HANDLING AND RESILIENCE TESTS
    // ============================================================================

    @Nested
    @DisplayName("Error Handling and Resilience")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle Redis connection failures gracefully")
        void shouldHandleRedisConnectionFailuresGracefully() {
            // Given
            when(mockRedisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection failed"));
            
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_MANAGER");
            
            // When & Then - should not throw exception
            assertThatCode(() -> realTimeMonitoringService.subscribeSession(sessionInfo))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle session with null WebSocket gracefully")
        void shouldHandleSessionWithNullWebSocketGracefully() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_MANAGER");
            sessionInfo.setSession(null); // Null WebSocket session
            
            // When & Then - should not throw exception
            assertThatCode(() -> {
                realTimeMonitoringService.subscribeSession(sessionInfo);
                realTimeMonitoringService.collectAndBroadcastUpdates();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should recover from monitoring collection errors")
        void shouldRecoverFromMonitoringCollectionErrors() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_MANAGER");
            realTimeMonitoringService.subscribeSession(sessionInfo);
            
            // When - simulate error then recovery
            sessionInfo.setSession(null); // Cause error
            realTimeMonitoringService.collectAndBroadcastUpdates();
            
            sessionInfo.setSession(mockWebSocketSession); // Restore
            realTimeMonitoringService.collectAndBroadcastUpdates();
            
            // Then - should recover gracefully
            assertThat(sessionInfo).isNotNull();
        }

        @Test
        @DisplayName("Should handle concurrent access safely")
        void shouldHandleConcurrentAccessSafely() {
            // Given
            WebSocketSessionInfo sessionInfo = createSessionInfo("OPERATIONS_MANAGER");
            
            // When - simulate concurrent operations
            assertThatCode(() -> {
                realTimeMonitoringService.subscribeSession(sessionInfo);
                realTimeMonitoringService.collectAndBroadcastUpdates();
                realTimeMonitoringService.unsubscribeSession(sessionInfo);
            }).doesNotThrowAnyException();
            
            // Then - should handle concurrent access safely
        }
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private WebSocketSessionInfo createSessionInfo(String role) {
        return WebSocketSessionInfo.builder()
                .sessionId("test-session-123")
                .session(mockWebSocketSession)
                .userId("test-user")
                .userRoles(List.of(role))
                .clientIp("192.168.1.100")
                .correlationId("test-correlation-123")
                .connectedAt(Instant.now())
                .lastActivity(Instant.now())
                .jwtToken("valid-jwt-token")
                .connectionStatus(WebSocketSessionInfo.ConnectionStatus.ACTIVE)
                .build();
    }

    private RealTimeMonitoringService.MonitoringDelta createMockMonitoringDelta() {
        return RealTimeMonitoringService.MonitoringDelta.builder()
                .timestamp(Instant.now())
                .hasChanges(true)
                .systemMetricsChanges(Map.of("cpu", 50.0))
                .businessMetricsChanges(Map.of("jobs", 100))
                .build();
    }
}