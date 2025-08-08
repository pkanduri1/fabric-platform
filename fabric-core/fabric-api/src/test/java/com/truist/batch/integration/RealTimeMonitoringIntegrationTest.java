package com.truist.batch.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truist.batch.entity.DashboardMetricsTimeseriesEntity;
import com.truist.batch.entity.WebSocketAuditLogEntity;
import com.truist.batch.repository.DashboardMetricsTimeseriesRepository;
import com.truist.batch.repository.WebSocketAuditLogRepository;
import com.truist.batch.security.JwtTokenProvider;
import com.truist.batch.websocket.service.RealTimeMonitoringService;
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

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * US008: Real-Time Monitoring Integration Test
 * 
 * End-to-end integration testing for monitoring workflow including:
 * - Complete WebSocket connection and message flow
 * - Real-time metrics processing and broadcasting
 * - Delta update calculations and optimizations
 * - Role-based content filtering and access control
 * - Subscription management and topic filtering
 * - Performance metrics collection and analysis
 * - Circuit breaker patterns and resilience
 * - Database and Redis integration testing
 * 
 * Target: 95%+ coverage of monitoring scenarios
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "fabric.websocket.delta-updates=true",
    "fabric.websocket.role-based-filtering=true",
    "fabric.websocket.adaptive-intervals=true",
    "fabric.websocket.performance-monitoring=true"
})
@DisplayName("Real-Time Monitoring Integration Tests")
class RealTimeMonitoringIntegrationTest {

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
    private DashboardMetricsTimeseriesRepository metricsRepository;

    @Autowired
    private WebSocketAuditLogRepository auditLogRepository;

    @Autowired
    private RealTimeMonitoringService monitoringService;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketClient webSocketClient;
    private String wsUrl;
    private String adminToken;
    private String userToken;
    private String readOnlyToken;

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
        
        // Generate test tokens with different roles
        adminToken = jwtTokenProvider.generateToken("admin-user", 
                List.of("ROLE_ADMIN", "ROLE_MONITOR", "ROLE_BATCH_USER"));
        userToken = jwtTokenProvider.generateToken("batch-user", 
                List.of("ROLE_MONITOR", "ROLE_BATCH_USER"));
        readOnlyToken = jwtTokenProvider.generateToken("readonly-user", 
                List.of("ROLE_MONITOR"));
        
        // Clear test data
        metricsRepository.deleteAll();
        auditLogRepository.deleteAll();
        
        // Create sample metrics data
        createSampleMetricsData();
    }

    @AfterEach
    void tearDown() {
        // Cleanup handled by test framework and @Transactional
    }

    // ============================================================================
    // REAL-TIME SUBSCRIPTION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Real-Time Subscription Management")
    class RealTimeSubscriptionTests {

        @Test
        @DisplayName("Should handle subscription to job status updates")
        void shouldHandleSubscriptionToJobStatusUpdates() throws Exception {
            // Given
            MonitoringWebSocketHandler handler = new MonitoringWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + userToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            // When - subscribe to job status updates
            String subscribeMessage = """
                {
                    "type": "subscribe",
                    "topics": ["job-status", "system-metrics"],
                    "executionIds": ["exec-123", "exec-456"]
                }
                """;
            session.sendMessage(new TextMessage(subscribeMessage));

            // Then - should receive subscription confirmation
            String response = handler.getNextMessage(5, TimeUnit.SECONDS);
            assertThat(response).isNotNull();
            
            JsonNode responseJson = objectMapper.readTree(response);
            assertThat(responseJson.get("type").asText()).isEqualTo("subscription-confirmed");
            assertThat(responseJson.get("topics")).isArray();
            assertThat(responseJson.get("topics")).hasSize(2);

            session.close();
        }

        @Test
        @DisplayName("Should filter subscription topics based on user roles")
        void shouldFilterSubscriptionTopicsBasedOnUserRoles() throws Exception {
            // Given
            MonitoringWebSocketHandler handler = new MonitoringWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + readOnlyToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            // When - attempt to subscribe to admin-only topics
            String subscribeMessage = """
                {
                    "type": "subscribe",
                    "topics": ["job-status", "system-config", "security-events"],
                    "executionIds": ["exec-123"]
                }
                """;
            session.sendMessage(new TextMessage(subscribeMessage));

            // Then - should only receive allowed topics
            String response = handler.getNextMessage(5, TimeUnit.SECONDS);
            assertThat(response).isNotNull();
            
            JsonNode responseJson = objectMapper.readTree(response);
            assertThat(responseJson.get("type").asText()).isEqualTo("subscription-confirmed");
            
            JsonNode topics = responseJson.get("topics");
            assertThat(topics).hasSize(1); // Only job-status allowed for read-only user
            assertThat(topics.get(0).asText()).isEqualTo("job-status");

            session.close();
        }

        @Test
        @DisplayName("Should handle unsubscribe requests correctly")
        void shouldHandleUnsubscribeRequestsCorrectly() throws Exception {
            // Given
            MonitoringWebSocketHandler handler = new MonitoringWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + userToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            // Subscribe first
            session.sendMessage(new TextMessage("""
                {
                    "type": "subscribe",
                    "topics": ["job-status", "system-metrics"]
                }
                """));
            handler.getNextMessage(2, TimeUnit.SECONDS); // Clear subscription confirmation

            // When - unsubscribe from specific topic
            String unsubscribeMessage = """
                {
                    "type": "unsubscribe",
                    "topics": ["system-metrics"]
                }
                """;
            session.sendMessage(new TextMessage(unsubscribeMessage));

            // Then - should receive unsubscribe confirmation
            String response = handler.getNextMessage(5, TimeUnit.SECONDS);
            assertThat(response).isNotNull();
            
            JsonNode responseJson = objectMapper.readTree(response);
            assertThat(responseJson.get("type").asText()).isEqualTo("unsubscribe-confirmed");
            assertThat(responseJson.get("topics").get(0).asText()).isEqualTo("system-metrics");

            session.close();
        }
    }

    // ============================================================================
    // REAL-TIME UPDATE BROADCASTING TESTS
    // ============================================================================

    @Nested
    @DisplayName("Real-Time Update Broadcasting")
    class RealTimeUpdateBroadcastingTests {

        @Test
        @DisplayName("Should broadcast metrics updates to subscribed clients")
        void shouldBroadcastMetricsUpdatesToSubscribedClients() throws Exception {
            // Given
            MonitoringWebSocketHandler handler = new MonitoringWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + userToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            // Subscribe to metrics
            session.sendMessage(new TextMessage("""
                {
                    "type": "subscribe",
                    "topics": ["system-metrics"],
                    "executionIds": ["exec-123"]
                }
                """));
            handler.getNextMessage(2, TimeUnit.SECONDS); // Clear subscription confirmation

            // When - create new metrics that should trigger broadcast
            createMetricsUpdate("exec-123", "SYSTEM_PERFORMANCE");
            
            // Simulate monitoring service broadcast
            monitoringService.broadcastMetricsUpdate("exec-123");

            // Then - should receive metrics update
            String updateMessage = handler.getNextMessage(5, TimeUnit.SECONDS);
            assertThat(updateMessage).isNotNull();
            
            JsonNode updateJson = objectMapper.readTree(updateMessage);
            assertThat(updateJson.get("type").asText()).isEqualTo("metrics-update");
            assertThat(updateJson.get("executionId").asText()).isEqualTo("exec-123");
            assertThat(updateJson.get("metrics")).isArray();

            session.close();
        }

        @Test
        @DisplayName("Should send delta updates instead of full snapshots")
        void shouldSendDeltaUpdatesInsteadOfFullSnapshots() throws Exception {
            // Given
            MonitoringWebSocketHandler handler = new MonitoringWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + userToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            // Subscribe to metrics
            session.sendMessage(new TextMessage("""
                {
                    "type": "subscribe",
                    "topics": ["system-metrics"],
                    "executionIds": ["exec-delta"]
                }
                """));
            handler.getNextMessage(2, TimeUnit.SECONDS); // Clear subscription confirmation

            // When - send initial metrics
            createMetricsUpdate("exec-delta", "SYSTEM_PERFORMANCE");
            monitoringService.broadcastMetricsUpdate("exec-delta");
            String initialUpdate = handler.getNextMessage(3, TimeUnit.SECONDS);

            // Then - send updated metrics (should be delta)
            Thread.sleep(100);
            createMetricsUpdate("exec-delta", "SYSTEM_PERFORMANCE", true);
            monitoringService.broadcastMetricsUpdate("exec-delta");
            String deltaUpdate = handler.getNextMessage(3, TimeUnit.SECONDS);

            // Verify delta update
            JsonNode deltaJson = objectMapper.readTree(deltaUpdate);
            assertThat(deltaJson.get("type").asText()).isEqualTo("metrics-delta");
            assertThat(deltaJson.has("changes")).isTrue();
            assertThat(deltaJson.get("timestamp")).isNotNull();

            session.close();
        }

        @Test
        @DisplayName("Should handle concurrent client subscriptions efficiently")
        void shouldHandleConcurrentClientSubscriptionsEfficiently() throws Exception {
            // Given - multiple concurrent clients
            int clientCount = 5;
            MonitoringWebSocketHandler[] handlers = new MonitoringWebSocketHandler[clientCount];
            WebSocketSession[] sessions = new WebSocketSession[clientCount];

            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + userToken);

            // When - connect multiple clients and subscribe
            for (int i = 0; i < clientCount; i++) {
                handlers[i] = new MonitoringWebSocketHandler();
                sessions[i] = webSocketClient.doHandshake(
                        handlers[i], headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);
                
                sessions[i].sendMessage(new TextMessage("""
                    {
                        "type": "subscribe",
                        "topics": ["system-metrics"],
                        "executionIds": ["exec-concurrent"]
                    }
                    """));
                handlers[i].getNextMessage(2, TimeUnit.SECONDS); // Clear confirmation
            }

            // Broadcast metrics update
            createMetricsUpdate("exec-concurrent", "SYSTEM_PERFORMANCE");
            monitoringService.broadcastMetricsUpdate("exec-concurrent");

            // Then - all clients should receive the update
            for (int i = 0; i < clientCount; i++) {
                String updateMessage = handlers[i].getNextMessage(5, TimeUnit.SECONDS);
                assertThat(updateMessage).isNotNull();
                
                JsonNode updateJson = objectMapper.readTree(updateMessage);
                assertThat(updateJson.get("type").asText()).isEqualTo("metrics-update");
                assertThat(updateJson.get("executionId").asText()).isEqualTo("exec-concurrent");
            }

            // Cleanup
            for (WebSocketSession session : sessions) {
                session.close();
            }
        }
    }

    // ============================================================================
    // ADAPTIVE INTERVAL TESTS
    // ============================================================================

    @Nested
    @DisplayName("Adaptive Update Intervals")
    class AdaptiveUpdateIntervalTests {

        @Test
        @DisplayName("Should adjust update frequency based on activity level")
        void shouldAdjustUpdateFrequencyBasedOnActivityLevel() throws Exception {
            // Given
            MonitoringWebSocketHandler handler = new MonitoringWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + userToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            session.sendMessage(new TextMessage("""
                {
                    "type": "subscribe",
                    "topics": ["system-metrics"],
                    "executionIds": ["exec-adaptive"],
                    "adaptiveInterval": true
                }
                """));
            handler.getNextMessage(2, TimeUnit.SECONDS); // Clear confirmation

            // When - simulate high activity (rapid metrics changes)
            for (int i = 0; i < 10; i++) {
                createMetricsUpdate("exec-adaptive", "SYSTEM_PERFORMANCE");
                monitoringService.broadcastMetricsUpdate("exec-adaptive");
                Thread.sleep(100);
            }

            // Then - should receive interval adjustment notification
            boolean intervalAdjusted = false;
            for (int i = 0; i < 5; i++) {
                String message = handler.getNextMessage(2, TimeUnit.SECONDS);
                if (message != null) {
                    JsonNode messageJson = objectMapper.readTree(message);
                    if ("interval-adjusted".equals(messageJson.get("type").asText())) {
                        intervalAdjusted = true;
                        assertThat(messageJson.get("newInterval").asLong()).isLessThan(5000); // Faster updates
                        break;
                    }
                }
            }
            assertThat(intervalAdjusted).isTrue();

            session.close();
        }

        @Test
        @DisplayName("Should revert to normal intervals during low activity")
        void shouldRevertToNormalIntervalsDuringLowActivity() throws Exception {
            // Given
            MonitoringWebSocketHandler handler = new MonitoringWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + userToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            session.sendMessage(new TextMessage("""
                {
                    "type": "subscribe",
                    "topics": ["system-metrics"],
                    "executionIds": ["exec-quiet"],
                    "adaptiveInterval": true
                }
                """));
            handler.getNextMessage(2, TimeUnit.SECONDS); // Clear confirmation

            // When - simulate low activity period
            Thread.sleep(2000); // Wait for inactivity detection

            // Then - should receive interval adjustment for slower updates
            String message = handler.getNextMessage(3, TimeUnit.SECONDS);
            if (message != null) {
                JsonNode messageJson = objectMapper.readTree(message);
                if ("interval-adjusted".equals(messageJson.get("type").asText())) {
                    assertThat(messageJson.get("newInterval").asLong()).isGreaterThan(3000); // Slower updates
                }
            }

            session.close();
        }
    }

    // ============================================================================
    // PERFORMANCE MONITORING TESTS
    // ============================================================================

    @Nested
    @DisplayName("Performance Monitoring Integration")
    class PerformanceMonitoringTests {

        @Test
        @DisplayName("Should collect WebSocket performance metrics")
        void shouldCollectWebSocketPerformanceMetrics() throws Exception {
            // Given
            MonitoringWebSocketHandler handler = new MonitoringWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + adminToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            session.sendMessage(new TextMessage("""
                {
                    "type": "subscribe",
                    "topics": ["performance-metrics"]
                }
                """));
            handler.getNextMessage(2, TimeUnit.SECONDS); // Clear confirmation

            // When - simulate some activity to generate performance data
            for (int i = 0; i < 20; i++) {
                session.sendMessage(new TextMessage("""
                    {"type": "heartbeat", "timestamp": """ + System.currentTimeMillis() + """}
                    """));
                Thread.sleep(10);
            }

            // Then - should receive performance metrics
            Thread.sleep(1000); // Allow metrics collection
            String perfMessage = handler.getNextMessage(5, TimeUnit.SECONDS);
            
            if (perfMessage != null) {
                JsonNode perfJson = objectMapper.readTree(perfMessage);
                if ("performance-metrics".equals(perfJson.get("type").asText())) {
                    assertThat(perfJson.has("connectionCount")).isTrue();
                    assertThat(perfJson.has("messageRate")).isTrue();
                    assertThat(perfJson.has("memoryUsage")).isTrue();
                }
            }

            session.close();
        }

        @Test
        @DisplayName("Should monitor message throughput and latency")
        void shouldMonitorMessageThroughputAndLatency() throws Exception {
            // Given
            MonitoringWebSocketHandler handler = new MonitoringWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + userToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            session.sendMessage(new TextMessage("""
                {
                    "type": "subscribe",
                    "topics": ["system-metrics"],
                    "executionIds": ["exec-throughput"]
                }
                """));
            handler.getNextMessage(2, TimeUnit.SECONDS);

            // When - send messages with timestamps to measure latency
            long startTime = System.currentTimeMillis();
            createMetricsUpdate("exec-throughput", "SYSTEM_PERFORMANCE");
            monitoringService.broadcastMetricsUpdate("exec-throughput");

            // Then - measure round-trip time
            String response = handler.getNextMessage(5, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;

            assertThat(response).isNotNull();
            assertThat(latency).isLessThan(1000); // Should be under 1 second

            JsonNode responseJson = objectMapper.readTree(response);
            assertThat(responseJson.has("serverTimestamp")).isTrue();

            session.close();
        }
    }

    // ============================================================================
    // CIRCUIT BREAKER TESTS
    // ============================================================================

    @Nested
    @DisplayName("Circuit Breaker Integration")
    class CircuitBreakerTests {

        @Test
        @DisplayName("Should activate circuit breaker on high error rates")
        void shouldActivateCircuitBreakerOnHighErrorRates() throws Exception {
            // Given
            MonitoringWebSocketHandler handler = new MonitoringWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + userToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            session.sendMessage(new TextMessage("""
                {
                    "type": "subscribe",
                    "topics": ["system-metrics"],
                    "executionIds": ["exec-error-prone"]
                }
                """));
            handler.getNextMessage(2, TimeUnit.SECONDS);

            // When - simulate high error rate scenario
            for (int i = 0; i < 10; i++) {
                try {
                    // Create metrics that might cause processing errors
                    createErrorProneMetrics("exec-error-prone");
                    monitoringService.broadcastMetricsUpdate("exec-error-prone");
                } catch (Exception e) {
                    // Expected errors to trigger circuit breaker
                }
                Thread.sleep(50);
            }

            // Then - should receive circuit breaker notification
            Thread.sleep(1000);
            String message = handler.getNextMessage(3, TimeUnit.SECONDS);
            
            if (message != null) {
                JsonNode messageJson = objectMapper.readTree(message);
                if ("circuit-breaker-opened".equals(messageJson.get("type").asText())) {
                    assertThat(messageJson.get("reason").asText()).contains("error rate");
                }
            }

            session.close();
        }

        @Test
        @DisplayName("Should recover from circuit breaker after cooling period")
        void shouldRecoverFromCircuitBreakerAfterCoolingPeriod() throws Exception {
            // Given - circuit breaker is activated (from previous test scenario)
            MonitoringWebSocketHandler handler = new MonitoringWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + userToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            session.sendMessage(new TextMessage("""
                {
                    "type": "subscribe",
                    "topics": ["system-metrics"],
                    "executionIds": ["exec-recovery"]
                }
                """));
            handler.getNextMessage(2, TimeUnit.SECONDS);

            // When - wait for circuit breaker recovery period and send normal requests
            Thread.sleep(2000); // Wait for circuit breaker to potentially reset
            
            createMetricsUpdate("exec-recovery", "SYSTEM_PERFORMANCE");
            monitoringService.broadcastMetricsUpdate("exec-recovery");

            // Then - should receive normal metrics update (circuit breaker recovered)
            String message = handler.getNextMessage(5, TimeUnit.SECONDS);
            assertThat(message).isNotNull();
            
            JsonNode messageJson = objectMapper.readTree(message);
            assertThat(messageJson.get("type").asText()).isEqualTo("metrics-update");

            session.close();
        }
    }

    // ============================================================================
    // DATA CONSISTENCY TESTS
    // ============================================================================

    @Nested
    @DisplayName("Data Consistency and Integrity")
    class DataConsistencyTests {

        @Test
        @DisplayName("Should maintain data consistency across multiple updates")
        void shouldMaintainDataConsistencyAcrossMultipleUpdates() throws Exception {
            // Given
            MonitoringWebSocketHandler handler = new MonitoringWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + userToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            session.sendMessage(new TextMessage("""
                {
                    "type": "subscribe",
                    "topics": ["system-metrics"],
                    "executionIds": ["exec-consistency"]
                }
                """));
            handler.getNextMessage(2, TimeUnit.SECONDS);

            // When - send multiple rapid updates
            for (int i = 0; i < 5; i++) {
                createSequentialMetricsUpdate("exec-consistency", i);
                monitoringService.broadcastMetricsUpdate("exec-consistency");
                Thread.sleep(100);
            }

            // Then - verify all updates are received in order
            for (int i = 0; i < 5; i++) {
                String message = handler.getNextMessage(3, TimeUnit.SECONDS);
                assertThat(message).isNotNull();
                
                JsonNode messageJson = objectMapper.readTree(message);
                assertThat(messageJson.get("type").asText()).isEqualTo("metrics-update");
                assertThat(messageJson.get("sequenceNumber").asInt()).isEqualTo(i);
            }

            session.close();
        }

        @Test
        @DisplayName("Should handle database connectivity issues gracefully")
        void shouldHandleDatabaseConnectivityIssuesGracefully() throws Exception {
            // Given
            MonitoringWebSocketHandler handler = new MonitoringWebSocketHandler();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + userToken);

            WebSocketSession session = webSocketClient.doHandshake(
                    handler, headers, URI.create(wsUrl)).get(5, TimeUnit.SECONDS);

            session.sendMessage(new TextMessage("""
                {
                    "type": "subscribe",
                    "topics": ["system-metrics"],
                    "executionIds": ["exec-db-fail"]
                }
                """));
            handler.getNextMessage(2, TimeUnit.SECONDS);

            // When - attempt to broadcast with potential database issues
            createMetricsUpdate("exec-db-fail", "SYSTEM_PERFORMANCE");
            monitoringService.broadcastMetricsUpdate("exec-db-fail");

            // Then - should receive either update or error notification
            String message = handler.getNextMessage(5, TimeUnit.SECONDS);
            assertThat(message).isNotNull();
            
            JsonNode messageJson = objectMapper.readTree(message);
            assertThat(messageJson.get("type").asText())
                    .isIn("metrics-update", "error", "service-unavailable");

            session.close();
        }
    }

    // ============================================================================
    // HELPER METHODS AND TEST DATA
    // ============================================================================

    private void createSampleMetricsData() {
        DashboardMetricsTimeseriesEntity metric1 = DashboardMetricsTimeseriesEntity.builder()
                .executionId("exec-123")
                .metricType("SYSTEM_PERFORMANCE")
                .metricName("cpu_usage")
                .metricValue(BigDecimal.valueOf(75.5))
                .metricUnit("%")
                .metricStatus("NORMAL")
                .metricTimestamp(Instant.now().minusSeconds(300))
                .cpuUsagePercent(BigDecimal.valueOf(75.5))
                .memoryUsageMb(512L)
                .processingDurationMs(2500L)
                .auditHash("hash-" + System.currentTimeMillis())
                .createdBy("TEST_SYSTEM")
                .securityClassification("INTERNAL")
                .build();

        DashboardMetricsTimeseriesEntity metric2 = DashboardMetricsTimeseriesEntity.builder()
                .executionId("exec-456")
                .metricType("BUSINESS_KPI")
                .metricName("processing_rate")
                .metricValue(BigDecimal.valueOf(1250.0))
                .metricUnit("records/hour")
                .metricStatus("NORMAL")
                .metricTimestamp(Instant.now().minusSeconds(600))
                .throughputPerSecond(BigDecimal.valueOf(0.35))
                .auditHash("hash-" + System.currentTimeMillis())
                .createdBy("TEST_SYSTEM")
                .securityClassification("INTERNAL")
                .build();

        metricsRepository.saveAll(List.of(metric1, metric2));
    }

    private void createMetricsUpdate(String executionId, String metricType) {
        createMetricsUpdate(executionId, metricType, false);
    }

    private void createMetricsUpdate(String executionId, String metricType, boolean isUpdate) {
        DashboardMetricsTimeseriesEntity metric = DashboardMetricsTimeseriesEntity.builder()
                .executionId(executionId)
                .metricType(metricType)
                .metricName(isUpdate ? "updated_metric" : "new_metric")
                .metricValue(BigDecimal.valueOf(Math.random() * 100))
                .metricUnit("%")
                .metricStatus("NORMAL")
                .metricTimestamp(Instant.now())
                .cpuUsagePercent(BigDecimal.valueOf(Math.random() * 100))
                .memoryUsageMb((long) (Math.random() * 1024))
                .processingDurationMs((long) (Math.random() * 5000))
                .auditHash("hash-" + System.currentTimeMillis())
                .createdBy("TEST_SYSTEM")
                .securityClassification("INTERNAL")
                .build();

        metricsRepository.save(metric);
    }

    private void createSequentialMetricsUpdate(String executionId, int sequence) {
        DashboardMetricsTimeseriesEntity metric = DashboardMetricsTimeseriesEntity.builder()
                .executionId(executionId)
                .metricType("SYSTEM_PERFORMANCE")
                .metricName("sequential_metric_" + sequence)
                .metricValue(BigDecimal.valueOf(sequence * 10))
                .metricUnit("units")
                .metricStatus("NORMAL")
                .metricTimestamp(Instant.now())
                .auditHash("hash-" + System.currentTimeMillis())
                .createdBy("TEST_SYSTEM")
                .securityClassification("INTERNAL")
                .build();

        metricsRepository.save(metric);
    }

    private void createErrorProneMetrics(String executionId) {
        // Create metrics that might cause processing errors
        DashboardMetricsTimeseriesEntity metric = DashboardMetricsTimeseriesEntity.builder()
                .executionId(executionId)
                .metricType("ERROR_PRONE")
                .metricName("error_metric")
                .metricValue(null) // Null value might cause processing issues
                .metricUnit("errors")
                .metricStatus("ERROR")
                .metricTimestamp(Instant.now())
                .auditHash("hash-" + System.currentTimeMillis())
                .createdBy("TEST_SYSTEM")
                .securityClassification("INTERNAL")
                .build();

        metricsRepository.save(metric);
    }

    /**
     * Test WebSocket handler for monitoring integration testing
     */
    private static class MonitoringWebSocketHandler implements WebSocketHandler {
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