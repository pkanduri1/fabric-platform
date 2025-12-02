package com.fabric.batch.repository;

import com.fabric.batch.entity.WebSocketAuditLogEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * US008: Comprehensive Unit Tests for WebSocketAuditLogRepository
 * 
 * Tests SOX compliance audit repository including:
 * - CRUD operations with JPA validation
 * - Security event queries and filtering
 * - Compliance event reporting and analysis
 * - Time-based queries and pagination
 * - Audit trail integrity validation
 * - Performance and monitoring queries
 * 
 * Target: 95%+ code coverage with comprehensive query testing
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@DataJpaTest
@DisplayName("WebSocketAuditLogRepository Unit Tests")
class WebSocketAuditLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WebSocketAuditLogRepository repository;

    private WebSocketAuditLogEntity securityEvent;
    private WebSocketAuditLogEntity complianceEvent;
    private WebSocketAuditLogEntity normalEvent;
    private Instant testStartTime;
    private Instant testEndTime;

    @BeforeEach
    void setUp() {
        testStartTime = Instant.now().minusSeconds(3600); // 1 hour ago
        testEndTime = Instant.now();

        // Create test data
        securityEvent = createAuditEvent(
                "WEBSOCKET_AUTH_FAILURE",
                "Authentication failed",
                "ERROR",
                "HIGH",
                "Y", "Y", // security and compliance flags
                "test-user-1",
                "session-123",
                testStartTime.plusSeconds(600)
        );

        complianceEvent = createAuditEvent(
                "CONFIGURATION_CHANGE",
                "Security settings updated",
                "INFO",
                "MEDIUM",
                "Y", "Y",
                "admin-user",
                "session-456",
                testStartTime.plusSeconds(1200)
        );

        normalEvent = createAuditEvent(
                "WEBSOCKET_CONNECTION_ESTABLISHED",
                "Connection established successfully",
                "INFO",
                "LOW",
                "Y", "N", // security but not compliance
                "normal-user",
                "session-789",
                testStartTime.plusSeconds(1800)
        );

        // Persist test data
        entityManager.persist(securityEvent);
        entityManager.persist(complianceEvent);
        entityManager.persist(normalEvent);
        entityManager.flush();
    }

    // ============================================================================
    // RECENT EVENTS QUERIES TESTS
    // ============================================================================

    @Nested
    @DisplayName("Recent Events Queries")
    class RecentEventsQueriesTests {

        @Test
        @DisplayName("Should find recent audit logs ordered by timestamp")
        void shouldFindRecentAuditLogsOrderedByTimestamp() {
            // Given
            Instant since = testStartTime.plusSeconds(300);

            // When
            List<WebSocketAuditLogEntity> results = repository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(since);

            // Then
            assertThat(results).hasSize(3);
            assertThat(results).extracting(WebSocketAuditLogEntity::getEventTimestamp)
                    .isSortedAccordingTo((t1, t2) -> t2.compareTo(t1)); // DESC order
        }

        @Test
        @DisplayName("Should find recent audit logs with pagination")
        void shouldFindRecentAuditLogsWithPagination() {
            // Given
            Instant since = testStartTime;
            Pageable pageable = PageRequest.of(0, 2);

            // When
            Page<WebSocketAuditLogEntity> page = repository.findByEventTimestampAfterOrderByEventTimestampDesc(since, pageable);

            // Then
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getContent()).extracting(WebSocketAuditLogEntity::getEventTimestamp)
                    .isSortedAccordingTo((t1, t2) -> t2.compareTo(t1));
        }

        @Test
        @DisplayName("Should find recent audit logs by event type")
        void shouldFindRecentAuditLogsByEventType() {
            // Given
            String eventType = "WEBSOCKET_AUTH_FAILURE";
            Instant since = testStartTime;

            // When
            List<WebSocketAuditLogEntity> results = repository.findByEventTypeAndEventTimestampAfterOrderByEventTimestampDesc(eventType, since);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getEventType()).isEqualTo(eventType);
            assertThat(results.get(0).getEventSeverity()).isEqualTo("ERROR");
        }

        @Test
        @DisplayName("Should return empty list when no recent events found")
        void shouldReturnEmptyListWhenNoRecentEventsFound() {
            // Given
            Instant futureTime = Instant.now().plusSeconds(3600);

            // When
            List<WebSocketAuditLogEntity> results = repository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(futureTime);

            // Then
            assertThat(results).isEmpty();
        }
    }

    // ============================================================================
    // SECURITY EVENT QUERIES TESTS
    // ============================================================================

    @Nested
    @DisplayName("Security Event Queries")
    class SecurityEventQueriesTests {

        @Test
        @DisplayName("Should find security events between time range")
        void shouldFindSecurityEventsBetweenTimeRange() {
            // When
            List<WebSocketAuditLogEntity> results = repository.findSecurityEventsBetween(testStartTime, testEndTime);

            // Then
            assertThat(results).hasSize(3); // All test events are security events
            assertThat(results).allMatch(WebSocketAuditLogEntity::isSecurityEvent);
        }

        @Test
        @DisplayName("Should find security events by severity level")
        void shouldFindSecurityEventsBySeverityLevel() {
            // Given
            String severity = "ERROR";

            // When
            List<WebSocketAuditLogEntity> results = repository.findSecurityEventsBySeverity(severity, testStartTime);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getEventSeverity()).isEqualTo(severity);
            assertThat(results.get(0).getRiskLevel()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("Should find critical security events")
        void shouldFindCriticalSecurityEvents() {
            // When
            List<WebSocketAuditLogEntity> results = repository.findCriticalSecurityEvents(testStartTime);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getRiskLevel()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("Should return empty list when no security events match criteria")
        void shouldReturnEmptyListWhenNoSecurityEventsMatchCriteria() {
            // Given
            String nonExistentSeverity = "NONEXISTENT";

            // When
            List<WebSocketAuditLogEntity> results = repository.findSecurityEventsBySeverity(nonExistentSeverity, testStartTime);

            // Then
            assertThat(results).isEmpty();
        }
    }

    // ============================================================================
    // COMPLIANCE EVENT QUERIES TESTS
    // ============================================================================

    @Nested
    @DisplayName("Compliance Event Queries")
    class ComplianceEventQueriesTests {

        @Test
        @DisplayName("Should find compliance events for SOX reporting")
        void shouldFindComplianceEventsForSoxReporting() {
            // When
            List<WebSocketAuditLogEntity> results = repository.findComplianceEventsForReporting(testStartTime, testEndTime);

            // Then
            assertThat(results).hasSize(2); // securityEvent and complianceEvent
            assertThat(results).allMatch(WebSocketAuditLogEntity::isComplianceEvent);
        }

        @Test
        @DisplayName("Should find compliance events by business function")
        void shouldFindComplianceEventsByBusinessFunction() {
            // Given
            String businessFunction = "REAL_TIME_MONITORING";
            entityManager.persist(createAuditEventWithBusinessFunction(businessFunction));
            entityManager.flush();

            // When
            List<WebSocketAuditLogEntity> results = repository.findByComplianceEventFlagAndBusinessFunctionAndEventTimestampBetweenOrderByEventTimestampDesc(
                    "Y", businessFunction, testStartTime, testEndTime);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getBusinessFunction()).isEqualTo(businessFunction);
        }

        @Test
        @DisplayName("Should find configuration change events")
        void shouldFindConfigurationChangeEvents() {
            // When
            List<WebSocketAuditLogEntity> results = repository.findConfigurationChangeEvents(testStartTime, testEndTime);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getEventType()).isEqualTo("CONFIGURATION_CHANGE");
        }
    }

    // ============================================================================
    // USER AND SESSION QUERIES TESTS
    // ============================================================================

    @Nested
    @DisplayName("User and Session Queries")
    class UserAndSessionQueriesTests {

        @Test
        @DisplayName("Should find audit logs by user ID")
        void shouldFindAuditLogsByUserId() {
            // Given
            String userId = "test-user-1";

            // When
            List<WebSocketAuditLogEntity> results = repository.findByUserIdAndEventTimestampBetweenOrderByEventTimestampDesc(
                    userId, testStartTime, testEndTime);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should find audit logs by session ID")
        void shouldFindAuditLogsBySessionId() {
            // Given
            String sessionId = "session-123";

            // When
            List<WebSocketAuditLogEntity> results = repository.findBySessionIdOrderByEventTimestampDesc(sessionId);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getSessionId()).isEqualTo(sessionId);
        }

        @Test
        @DisplayName("Should generate user activity summary")
        void shouldGenerateUserActivitySummary() {
            // When
            List<Object[]> results = repository.findUserActivitySummary(testStartTime);

            // Then
            assertThat(results).hasSize(3); // Three different users
            assertThat(results.get(0)).hasSize(4); // userId, count, min, max timestamps
        }

        @Test
        @DisplayName("Should find session lifecycle events")
        void shouldFindSessionLifecycleEvents() {
            // Given - create connection and disconnection events
            String sessionId = "lifecycle-session";
            WebSocketAuditLogEntity connectionEvent = createAuditEvent(
                    "WEBSOCKET_CONNECTION_ESTABLISHED", "Connected", "INFO", "LOW", "Y", "N",
                    "test-user", sessionId, testStartTime.plusSeconds(100));
            WebSocketAuditLogEntity disconnectionEvent = createAuditEvent(
                    "WEBSOCKET_CONNECTION_CLOSED", "Disconnected", "INFO", "LOW", "Y", "N",
                    "test-user", sessionId, testStartTime.plusSeconds(200));

            entityManager.persist(connectionEvent);
            entityManager.persist(disconnectionEvent);
            entityManager.flush();

            // When
            List<WebSocketAuditLogEntity> results = repository.findSessionLifecycleEvents(sessionId);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(WebSocketAuditLogEntity::getEventType)
                    .containsExactly("WEBSOCKET_CONNECTION_ESTABLISHED", "WEBSOCKET_CONNECTION_CLOSED");
        }
    }

    // ============================================================================
    // CORRELATION AND TRACKING QUERIES TESTS
    // ============================================================================

    @Nested
    @DisplayName("Correlation and Tracking Queries")
    class CorrelationTrackingTests {

        @Test
        @DisplayName("Should find audit logs by correlation ID")
        void shouldFindAuditLogsByCorrelationId() {
            // Given
            String correlationId = "test-correlation-123";
            WebSocketAuditLogEntity correlatedEvent = createAuditEvent(
                    "TEST_EVENT", "Correlated event", "INFO", "LOW", "N", "N",
                    "test-user", "session-corr", testStartTime.plusSeconds(300));
            correlatedEvent.setCorrelationId(correlationId);
            entityManager.persist(correlatedEvent);
            entityManager.flush();

            // When
            List<WebSocketAuditLogEntity> results = repository.findByCorrelationIdOrderByEventTimestampDesc(correlationId);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getCorrelationId()).isEqualTo(correlationId);
        }

        @Test
        @DisplayName("Should find correlated events within time window")
        void shouldFindCorrelatedEventsWithinTimeWindow() {
            // Given
            String correlationId = "time-window-correlation";
            WebSocketAuditLogEntity event1 = createAuditEvent(
                    "EVENT_1", "First event", "INFO", "LOW", "N", "N",
                    "user1", "session1", testStartTime.plusSeconds(100));
            WebSocketAuditLogEntity event2 = createAuditEvent(
                    "EVENT_2", "Second event", "INFO", "LOW", "N", "N",
                    "user2", "session2", testStartTime.plusSeconds(200));

            event1.setCorrelationId(correlationId);
            event2.setCorrelationId(correlationId);

            entityManager.persist(event1);
            entityManager.persist(event2);
            entityManager.flush();

            // When
            List<WebSocketAuditLogEntity> results = repository.findCorrelatedEvents(
                    correlationId, testStartTime, testEndTime);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(WebSocketAuditLogEntity::getCorrelationId)
                    .allMatch(correlationId::equals);
        }
    }

    // ============================================================================
    // INTEGRITY VALIDATION QUERIES TESTS
    // ============================================================================

    @Nested
    @DisplayName("Integrity Validation Queries")
    class IntegrityValidationTests {

        @Test
        @DisplayName("Should find audit log by audit hash")
        void shouldFindAuditLogByAuditHash() {
            // Given
            String auditHash = "test-unique-hash-123";
            WebSocketAuditLogEntity hashedEvent = createAuditEvent(
                    "HASHED_EVENT", "Event with unique hash", "INFO", "LOW", "N", "N",
                    "test-user", "session-hash", testStartTime.plusSeconds(500));
            hashedEvent.setAuditHash(auditHash);
            entityManager.persist(hashedEvent);
            entityManager.flush();

            // When
            Optional<WebSocketAuditLogEntity> result = repository.findByAuditHash(auditHash);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getAuditHash()).isEqualTo(auditHash);
        }

        @Test
        @DisplayName("Should find audit logs by previous hash for chain validation")
        void shouldFindAuditLogsByPreviousHashForChainValidation() {
            // Given
            String previousHash = "previous-hash-123";
            WebSocketAuditLogEntity chainedEvent = createAuditEvent(
                    "CHAINED_EVENT", "Event in audit chain", "INFO", "LOW", "N", "N",
                    "test-user", "session-chain", testStartTime.plusSeconds(600));
            chainedEvent.setPreviousAuditHash(previousHash);
            entityManager.persist(chainedEvent);
            entityManager.flush();

            // When
            List<WebSocketAuditLogEntity> results = repository.findByPreviousAuditHash(previousHash);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getPreviousAuditHash()).isEqualTo(previousHash);
        }

        @Test
        @DisplayName("Should find audit logs for integrity validation")
        void shouldFindAuditLogsForIntegrityValidation() {
            // When
            List<WebSocketAuditLogEntity> results = repository.findForIntegrityValidation(testStartTime);

            // Then
            assertThat(results).hasSize(3);
            assertThat(results).extracting(WebSocketAuditLogEntity::getEventTimestamp)
                    .isSortedAccordingTo(Instant::compareTo); // ASC order for integrity validation
        }

        @Test
        @DisplayName("Should count broken audit chains")
        void shouldCountBrokenAuditChains() {
            // Given - create an event with a previous hash that doesn't exist
            WebSocketAuditLogEntity brokenChainEvent = createAuditEvent(
                    "BROKEN_CHAIN", "Event with broken chain", "INFO", "LOW", "N", "N",
                    "test-user", "session-broken", testStartTime.plusSeconds(700));
            brokenChainEvent.setPreviousAuditHash("non-existent-hash");
            entityManager.persist(brokenChainEvent);
            entityManager.flush();

            // When
            long brokenChainCount = repository.countBrokenAuditChains();

            // Then
            assertThat(brokenChainCount).isEqualTo(1);
        }
    }

    // ============================================================================
    // PERFORMANCE AND MONITORING QUERIES TESTS
    // ============================================================================

    @Nested
    @DisplayName("Performance and Monitoring Queries")
    class PerformanceMonitoringTests {

        @Test
        @DisplayName("Should count events by type in time range")
        void shouldCountEventsByTypeInTimeRange() {
            // When
            List<Object[]> results = repository.countEventsByType(testStartTime, testEndTime);

            // Then
            assertThat(results).hasSize(3); // Three different event types
            assertThat(results.get(0)).hasSize(2); // eventType, count
        }

        @Test
        @DisplayName("Should count events by severity in time range")
        void shouldCountEventsBySeverityInTimeRange() {
            // When
            List<Object[]> results = repository.countEventsBySeverity(testStartTime, testEndTime);

            // Then
            assertThat(results).isNotEmpty();
            assertThat(results.get(0)).hasSize(2); // severity, count
        }

        @Test
        @DisplayName("Should find high risk events")
        void shouldFindHighRiskEvents() {
            // When
            List<WebSocketAuditLogEntity> results = repository.findHighRiskEvents(testStartTime);

            // Then
            assertThat(results).hasSize(2); // HIGH and MEDIUM risk events
            assertThat(results).extracting(WebSocketAuditLogEntity::getRiskLevel)
                    .allMatch(level -> "HIGH".equals(level) || "MEDIUM".equals(level));
        }
    }

    // ============================================================================
    // REPORTING QUERIES TESTS
    // ============================================================================

    @Nested
    @DisplayName("Reporting Queries")
    class ReportingQueriesTests {

        @Test
        @DisplayName("Should generate security event summary")
        void shouldGenerateSecurityEventSummary() {
            // When
            List<Object[]> results = repository.generateSecurityEventSummary(testStartTime, testEndTime);

            // Then
            assertThat(results).isNotEmpty();
            assertThat(results.get(0)).hasSize(4); // eventType, severity, riskLevel, count
        }

        @Test
        @DisplayName("Should generate compliance event summary")
        void shouldGenerateComplianceEventSummary() {
            // When
            List<Object[]> results = repository.generateComplianceEventSummary(testStartTime, testEndTime);

            // Then
            assertThat(results).isNotEmpty();
            assertThat(results.get(0)).hasSize(5); // businessFunction, eventType, count, min, max timestamps
        }

        @Test
        @DisplayName("Should find events with digital signature")
        void shouldFindEventsWithDigitalSignature() {
            // Given
            WebSocketAuditLogEntity signedEvent = createAuditEvent(
                    "SIGNED_EVENT", "Event with digital signature", "INFO", "LOW", "N", "N",
                    "test-user", "session-signed", testStartTime.plusSeconds(800));
            signedEvent.setDigitalSignature("test-digital-signature");
            entityManager.persist(signedEvent);
            entityManager.flush();

            // When
            List<WebSocketAuditLogEntity> results = repository.findEventsWithDigitalSignature(testStartTime);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getDigitalSignature()).isNotNull();
        }
    }

    // ============================================================================
    // CLEANUP AND MAINTENANCE QUERIES TESTS
    // ============================================================================

    @Nested
    @DisplayName("Cleanup and Maintenance Queries")
    class CleanupMaintenanceTests {

        @Test
        @DisplayName("Should find old non-compliance events for cleanup")
        void shouldFindOldNonComplianceEventsForCleanup() {
            // Given
            Instant cutoffDate = testStartTime.minusSeconds(7200); // 2 hours ago
            WebSocketAuditLogEntity oldEvent = createAuditEvent(
                    "OLD_EVENT", "Old non-compliance event", "INFO", "LOW", "N", "N",
                    "test-user", "session-old", cutoffDate.minusSeconds(3600));
            entityManager.persist(oldEvent);
            entityManager.flush();

            // When
            List<WebSocketAuditLogEntity> results = repository.findOldNonComplianceEvents(cutoffDate);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getComplianceEventFlag()).isEqualTo("N");
        }

        @Test
        @DisplayName("Should count by security classification")
        void shouldCountBySecurityClassification() {
            // When
            List<Object[]> results = repository.countBySecurityClassification();

            // Then
            assertThat(results).isNotEmpty();
            assertThat(results.get(0)).hasSize(2); // classification, count
        }

        @Test
        @DisplayName("Should find events requiring attention")
        void shouldFindEventsRequiringAttention() {
            // When
            List<WebSocketAuditLogEntity> results = repository.findEventsRequiringAttention(testStartTime);

            // Then
            assertThat(results).hasSize(2); // ERROR severity and HIGH risk events
        }
    }

    // ============================================================================
    // DASHBOARD QUERIES TESTS
    // ============================================================================

    @Nested
    @DisplayName("Dashboard Queries")
    class DashboardQueriesTests {

        @Test
        @DisplayName("Should find recent events for dashboard")
        void shouldFindRecentEventsForDashboard() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<WebSocketAuditLogEntity> results = repository.findRecentEventsForDashboard(testStartTime, pageable);

            // Then
            assertThat(results.getContent()).hasSize(3);
            assertThat(results.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should count events by hour for trending")
        void shouldCountEventsByHourForTrending() {
            // When
            List<Object[]> results = repository.countEventsByHour(testStartTime);

            // Then
            assertThat(results).isNotEmpty();
            assertThat(results.get(0)).hasSize(2); // hour, count
        }
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private WebSocketAuditLogEntity createAuditEvent(String eventType, String description, String severity,
                                                   String riskLevel, String securityFlag, String complianceFlag,
                                                   String userId, String sessionId, Instant timestamp) {
        return WebSocketAuditLogEntity.builder()
                .eventType(eventType)
                .eventDescription(description)
                .eventSeverity(severity)
                .riskLevel(riskLevel)
                .securityEventFlag(securityFlag)
                .complianceEventFlag(complianceFlag)
                .userId(userId)
                .sessionId(sessionId)
                .eventTimestamp(timestamp)
                .auditHash("hash-" + System.currentTimeMillis())
                .createdBy("TEST_SYSTEM")
                .securityClassification("INTERNAL")
                .businessFunction("REAL_TIME_MONITORING")
                .build();
    }

    private WebSocketAuditLogEntity createAuditEventWithBusinessFunction(String businessFunction) {
        return WebSocketAuditLogEntity.builder()
                .eventType("BUSINESS_FUNCTION_EVENT")
                .eventDescription("Event for business function test")
                .eventSeverity("INFO")
                .riskLevel("LOW")
                .securityEventFlag("N")
                .complianceEventFlag("Y")
                .userId("business-user")
                .sessionId("business-session")
                .eventTimestamp(testStartTime.plusSeconds(900))
                .auditHash("business-hash-" + System.currentTimeMillis())
                .createdBy("BUSINESS_SYSTEM")
                .securityClassification("INTERNAL")
                .businessFunction(businessFunction)
                .build();
    }
}