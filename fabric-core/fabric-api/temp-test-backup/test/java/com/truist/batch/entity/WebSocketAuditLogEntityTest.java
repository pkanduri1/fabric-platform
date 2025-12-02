package com.fabric.batch.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * US008: Comprehensive Unit Tests for WebSocketAuditLogEntity
 * 
 * Tests SOX compliance entity including:
 * - Bean validation and constraint checking
 * - Business logic methods and state management
 * - JPA lifecycle callbacks and event handling
 * - Security classification and risk assessment
 * - Audit hash integrity and tamper detection
 * - Compliance flag management and tracking
 * 
 * Target: 95%+ code coverage with comprehensive edge case testing
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@DisplayName("WebSocketAuditLogEntity Unit Tests")
class WebSocketAuditLogEntityTest {

    private Validator validator;
    private WebSocketAuditLogEntity auditEntity;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        auditEntity = WebSocketAuditLogEntity.builder()
                .eventType("WEBSOCKET_CONNECTION_ESTABLISHED")
                .eventDescription("WebSocket connection established successfully")
                .sessionId("test-session-123")
                .userId("test-user")
                .clientIp("192.168.1.100")
                .eventTimestamp(Instant.now())
                .eventSeverity("INFO")
                .auditHash("test-audit-hash-123")
                .createdBy("SYSTEM")
                .build();
    }

    // ============================================================================
    // VALIDATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Bean Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate valid audit entity without errors")
        void shouldValidateValidAuditEntityWithoutErrors() {
            // When
            Set<ConstraintViolation<WebSocketAuditLogEntity>> violations = validator.validate(auditEntity);
            
            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should reject entity with missing required event type")
        void shouldRejectEntityWithMissingRequiredEventType() {
            // Given
            auditEntity.setEventType(null);
            
            // When
            Set<ConstraintViolation<WebSocketAuditLogEntity>> violations = validator.validate(auditEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Event type is required");
        }

        @Test
        @DisplayName("Should reject entity with blank event type")
        void shouldRejectEntityWithBlankEventType() {
            // Given
            auditEntity.setEventType("   ");
            
            // When
            Set<ConstraintViolation<WebSocketAuditLogEntity>> violations = validator.validate(auditEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Event type is required");
        }

        @Test
        @DisplayName("Should reject entity with missing required event timestamp")
        void shouldRejectEntityWithMissingRequiredEventTimestamp() {
            // Given
            auditEntity.setEventTimestamp(null);
            
            // When
            Set<ConstraintViolation<WebSocketAuditLogEntity>> violations = validator.validate(auditEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Event timestamp is required");
        }

        @Test
        @DisplayName("Should reject entity with missing required event severity")
        void shouldRejectEntityWithMissingRequiredEventSeverity() {
            // Given
            auditEntity.setEventSeverity(null);
            
            // When
            Set<ConstraintViolation<WebSocketAuditLogEntity>> violations = validator.validate(auditEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Event severity is required");
        }

        @Test
        @DisplayName("Should reject entity with missing required audit hash")
        void shouldRejectEntityWithMissingRequiredAuditHash() {
            // Given
            auditEntity.setAuditHash(null);
            
            // When
            Set<ConstraintViolation<WebSocketAuditLogEntity>> violations = validator.validate(auditEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Audit hash is required for tamper detection");
        }

        @Test
        @DisplayName("Should reject entity with missing required created by")
        void shouldRejectEntityWithMissingRequiredCreatedBy() {
            // Given
            auditEntity.setCreatedBy(null);
            
            // When
            Set<ConstraintViolation<WebSocketAuditLogEntity>> violations = validator.validate(auditEntity);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Created by is required");
        }
    }

    // ============================================================================
    // BUSINESS LOGIC TESTS
    // ============================================================================

    @Nested
    @DisplayName("Business Logic Methods")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should correctly identify security events")
        void shouldCorrectlyIdentifySecurityEvents() {
            // Given
            auditEntity.setSecurityEventFlag("Y");
            
            // When & Then
            assertThat(auditEntity.isSecurityEvent()).isTrue();
            
            // Given
            auditEntity.setSecurityEventFlag("N");
            
            // When & Then
            assertThat(auditEntity.isSecurityEvent()).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify compliance events")
        void shouldCorrectlyIdentifyComplianceEvents() {
            // Given
            auditEntity.setComplianceEventFlag("Y");
            
            // When & Then
            assertThat(auditEntity.isComplianceEvent()).isTrue();
            
            // Given
            auditEntity.setComplianceEventFlag("N");
            
            // When & Then
            assertThat(auditEntity.isComplianceEvent()).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify critical events by severity")
        void shouldCorrectlyIdentifyCriticalEventsBySeverity() {
            // Given
            auditEntity.setEventSeverity("CRITICAL");
            auditEntity.setRiskLevel("MEDIUM");
            
            // When & Then
            assertThat(auditEntity.isCriticalEvent()).isTrue();
        }

        @Test
        @DisplayName("Should correctly identify critical events by risk level")
        void shouldCorrectlyIdentifyCriticalEventsByRiskLevel() {
            // Given
            auditEntity.setEventSeverity("INFO");
            auditEntity.setRiskLevel("CRITICAL");
            
            // When & Then
            assertThat(auditEntity.isCriticalEvent()).isTrue();
        }

        @Test
        @DisplayName("Should identify events requiring digital signature")
        void shouldIdentifyEventsRequiringDigitalSignature() {
            // Given - critical event
            auditEntity.setEventSeverity("CRITICAL");
            
            // When & Then
            assertThat(auditEntity.requiresDigitalSignature()).isTrue();
            
            // Given - SOX compliance classification
            auditEntity.setEventSeverity("INFO");
            auditEntity.setSecurityClassification("SOX_COMPLIANCE_REQUIRED");
            
            // When & Then
            assertThat(auditEntity.requiresDigitalSignature()).isTrue();
            
            // Given - configuration change event
            auditEntity.setSecurityClassification("INTERNAL");
            auditEntity.setEventType("CONFIGURATION_CHANGE");
            
            // When & Then
            assertThat(auditEntity.requiresDigitalSignature()).isTrue();
        }

        @Test
        @DisplayName("Should mark events as security events with risk escalation")
        void shouldMarkEventsAsSecurityEventsWithRiskEscalation() {
            // Given
            auditEntity.setSecurityEventFlag("N");
            auditEntity.setRiskLevel("LOW");
            
            // When
            auditEntity.markAsSecurityEvent();
            
            // Then
            assertThat(auditEntity.getSecurityEventFlag()).isEqualTo("Y");
            assertThat(auditEntity.getRiskLevel()).isEqualTo("MEDIUM");
        }

        @Test
        @DisplayName("Should mark events as compliance events with classification")
        void shouldMarkEventsAsComplianceEventsWithClassification() {
            // Given
            auditEntity.setComplianceEventFlag("N");
            auditEntity.setSecurityClassification(null);
            
            // When
            auditEntity.markAsComplianceEvent();
            
            // Then
            assertThat(auditEntity.getComplianceEventFlag()).isEqualTo("Y");
            assertThat(auditEntity.getSecurityClassification()).isEqualTo("SOX_AUDIT_REQUIRED");
        }

        @Test
        @DisplayName("Should update performance metrics correctly")
        void shouldUpdatePerformanceMetricsCorrectly() {
            // Given
            long durationMs = 1500L;
            long memoryMb = 256L;
            double cpuPercent = 75.5;
            
            // When
            auditEntity.updatePerformanceMetrics(durationMs, memoryMb, cpuPercent);
            
            // Then
            assertThat(auditEntity.getProcessingDurationMs()).isEqualTo(1500L);
            assertThat(auditEntity.getMemoryUsageMb()).isEqualTo(256L);
            assertThat(auditEntity.getCpuUsagePercent()).isEqualTo(BigDecimal.valueOf(75.5));
        }

        @Test
        @DisplayName("Should escalate risk level with security flags")
        void shouldEscalateRiskLevelWithSecurityFlags() {
            // Given
            auditEntity.setRiskLevel("LOW");
            auditEntity.setSecurityEventFlag("N");
            auditEntity.setComplianceEventFlag("N");
            auditEntity.setSecurityClassification("INTERNAL");
            
            // When
            auditEntity.escalateRiskLevel();
            
            // Then
            assertThat(auditEntity.getRiskLevel()).isEqualTo("HIGH");
            assertThat(auditEntity.getSecurityEventFlag()).isEqualTo("Y");
            assertThat(auditEntity.getComplianceEventFlag()).isEqualTo("Y");
            assertThat(auditEntity.getSecurityClassification()).isEqualTo("SECURITY_INCIDENT");
        }

        @Test
        @DisplayName("Should validate hash integrity correctly")
        void shouldValidateHashIntegrityCorrectly() {
            // Given
            String correctHash = "test-audit-hash-123";
            String incorrectHash = "wrong-hash";
            
            // When & Then
            assertThat(auditEntity.validateHashIntegrity(correctHash)).isTrue();
            assertThat(auditEntity.validateHashIntegrity(incorrectHash)).isFalse();
            assertThat(auditEntity.validateHashIntegrity(null)).isFalse();
        }

        @Test
        @DisplayName("Should calculate event age in minutes")
        void shouldCalculateEventAgeInMinutes() {
            // Given
            Instant pastTimestamp = Instant.now().minusSeconds(300); // 5 minutes ago
            auditEntity.setEventTimestamp(pastTimestamp);
            
            // When
            long ageMinutes = auditEntity.getEventAgeMinutes();
            
            // Then
            assertThat(ageMinutes).isBetween(4L, 6L); // Allow some variance
        }

        @Test
        @DisplayName("Should handle null event timestamp gracefully")
        void shouldHandleNullEventTimestampGracefully() {
            // Given
            auditEntity.setEventTimestamp(null);
            
            // When
            long ageMinutes = auditEntity.getEventAgeMinutes();
            
            // Then
            assertThat(ageMinutes).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should identify recent events correctly")
        void shouldIdentifyRecentEventsCorrectly() {
            // Given - recent event
            auditEntity.setEventTimestamp(Instant.now().minusSeconds(1800)); // 30 minutes ago
            
            // When & Then
            assertThat(auditEntity.isRecentEvent()).isTrue();
            
            // Given - old event
            auditEntity.setEventTimestamp(Instant.now().minusSeconds(7200)); // 2 hours ago
            
            // When & Then
            assertThat(auditEntity.isRecentEvent()).isFalse();
        }

        @Test
        @DisplayName("Should generate proper event summary")
        void shouldGenerateProperEventSummary() {
            // Given
            auditEntity.setEventSeverity("ERROR");
            auditEntity.setEventType("WEBSOCKET_AUTH_FAILURE");
            auditEntity.setUserId("test-user-123");
            auditEntity.setSessionId("session-456789");
            auditEntity.setRiskLevel("HIGH");
            
            // When
            String summary = auditEntity.getEventSummary();
            
            // Then
            assertThat(summary).contains("ERROR");
            assertThat(summary).contains("WEBSOCKET_AUTH_FAILURE");
            assertThat(summary).contains("test-user-123");
            assertThat(summary).contains("session-4"); // First 8 chars of session ID
            assertThat(summary).contains("HIGH");
        }

        @Test
        @DisplayName("Should handle null values in event summary")
        void shouldHandleNullValuesInEventSummary() {
            // Given
            auditEntity.setUserId(null);
            auditEntity.setSessionId(null);
            
            // When
            String summary = auditEntity.getEventSummary();
            
            // Then
            assertThat(summary).contains("SYSTEM"); // Default for null userId
            assertThat(summary).contains("N/A"); // Default for null sessionId
        }

        @Test
        @DisplayName("Should create comprehensive audit metadata")
        void shouldCreateComprehensiveAuditMetadata() {
            // Given
            auditEntity.setAuditId(123L);
            auditEntity.setDigitalSignature("test-signature");
            
            // When
            Map<String, Object> metadata = auditEntity.getAuditMetadata();
            
            // Then
            assertThat(metadata).containsKeys(
                    "auditId", "eventType", "eventSeverity", "riskLevel",
                    "securityEvent", "complianceEvent", "userId", "sessionId",
                    "eventTimestamp", "businessFunction", "correlationId", "hasDigitalSignature"
            );
            assertThat(metadata.get("auditId")).isEqualTo(123L);
            assertThat(metadata.get("hasDigitalSignature")).isEqualTo(true);
        }
    }

    // ============================================================================
    // JPA LIFECYCLE TESTS
    // ============================================================================

    @Nested
    @DisplayName("JPA Lifecycle Events")
    class JpaLifecycleTests {

        @Test
        @DisplayName("Should set default values on create")
        void shouldSetDefaultValuesOnCreate() {
            // Given
            WebSocketAuditLogEntity newEntity = WebSocketAuditLogEntity.builder()
                    .eventType("TEST_EVENT")
                    .eventSeverity("INFO")
                    .auditHash("test-hash")
                    .createdBy("SYSTEM")
                    .build();
            
            // When
            newEntity.onCreate();
            
            // Then
            assertThat(newEntity.getEventTimestamp()).isNotNull();
            assertThat(newEntity.getCreatedDate()).isNotNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"DEBUG", "INFO", "WARN", "ERROR", "CRITICAL"})
        @DisplayName("Should validate valid event severities")
        void shouldValidateValidEventSeverities(String severity) {
            // Given
            auditEntity.setEventSeverity(severity);
            
            // When & Then - should not throw exception
            assertThatCode(() -> auditEntity.onCreate()).doesNotThrowAnyException();
            assertThatCode(() -> auditEntity.onUpdate()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject invalid event severity")
        void shouldRejectInvalidEventSeverity() {
            // Given
            auditEntity.setEventSeverity("INVALID_SEVERITY");
            
            // When & Then
            assertThatThrownBy(() -> auditEntity.onCreate())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid event severity: INVALID_SEVERITY");
        }

        @ParameterizedTest
        @ValueSource(strings = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
        @DisplayName("Should validate valid risk levels")
        void shouldValidateValidRiskLevels(String riskLevel) {
            // Given
            auditEntity.setRiskLevel(riskLevel);
            
            // When & Then - should not throw exception
            assertThatCode(() -> auditEntity.onCreate()).doesNotThrowAnyException();
            assertThatCode(() -> auditEntity.onUpdate()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject invalid risk level")
        void shouldRejectInvalidRiskLevel() {
            // Given
            auditEntity.setRiskLevel("INVALID_RISK");
            
            // When & Then
            assertThatThrownBy(() -> auditEntity.onCreate())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid risk level: INVALID_RISK");
        }

        @ParameterizedTest
        @ValueSource(strings = {"Y", "N"})
        @DisplayName("Should validate valid security event flags")
        void shouldValidateValidSecurityEventFlags(String flag) {
            // Given
            auditEntity.setSecurityEventFlag(flag);
            
            // When & Then - should not throw exception
            assertThatCode(() -> auditEntity.onCreate()).doesNotThrowAnyException();
            assertThatCode(() -> auditEntity.onUpdate()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject invalid security event flag")
        void shouldRejectInvalidSecurityEventFlag() {
            // Given
            auditEntity.setSecurityEventFlag("INVALID");
            
            // When & Then
            assertThatThrownBy(() -> auditEntity.onCreate())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid security event flag: INVALID");
        }

        @ParameterizedTest
        @ValueSource(strings = {"Y", "N"})
        @DisplayName("Should validate valid compliance event flags")
        void shouldValidateValidComplianceEventFlags(String flag) {
            // Given
            auditEntity.setComplianceEventFlag(flag);
            
            // When & Then - should not throw exception
            assertThatCode(() -> auditEntity.onCreate()).doesNotThrowAnyException();
            assertThatCode(() -> auditEntity.onUpdate()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject invalid compliance event flag")
        void shouldRejectInvalidComplianceEventFlag() {
            // Given
            auditEntity.setComplianceEventFlag("INVALID");
            
            // When & Then
            assertThatThrownBy(() -> auditEntity.onCreate())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid compliance event flag: INVALID");
        }
    }

    // ============================================================================
    // EQUALITY AND HASH CODE TESTS
    // ============================================================================

    @Nested
    @DisplayName("Equality and Hash Code Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when audit IDs match")
        void shouldBeEqualWhenAuditIdsMatch() {
            // Given
            WebSocketAuditLogEntity entity1 = WebSocketAuditLogEntity.builder()
                    .auditId(123L)
                    .eventType("EVENT_1")
                    .build();
            
            WebSocketAuditLogEntity entity2 = WebSocketAuditLogEntity.builder()
                    .auditId(123L)
                    .eventType("EVENT_2")
                    .build();
            
            // When & Then
            assertThat(entity1).isEqualTo(entity2);
            assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when audit IDs differ")
        void shouldNotBeEqualWhenAuditIdsDiffer() {
            // Given
            WebSocketAuditLogEntity entity1 = WebSocketAuditLogEntity.builder()
                    .auditId(123L)
                    .eventType("EVENT_1")
                    .build();
            
            WebSocketAuditLogEntity entity2 = WebSocketAuditLogEntity.builder()
                    .auditId(456L)
                    .eventType("EVENT_1")
                    .build();
            
            // When & Then
            assertThat(entity1).isNotEqualTo(entity2);
        }

        @Test
        @DisplayName("Should use audit hash for equality when audit ID is null")
        void shouldUseAuditHashForEqualityWhenAuditIdIsNull() {
            // Given
            WebSocketAuditLogEntity entity1 = WebSocketAuditLogEntity.builder()
                    .auditHash("same-hash")
                    .eventType("EVENT_1")
                    .build();
            
            WebSocketAuditLogEntity entity2 = WebSocketAuditLogEntity.builder()
                    .auditHash("same-hash")
                    .eventType("EVENT_2")
                    .build();
            
            // When & Then
            assertThat(entity1).isEqualTo(entity2);
            assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
        }

        @Test
        @DisplayName("Should handle null comparisons gracefully")
        void shouldHandleNullComparisonsGracefully() {
            // When & Then
            assertThat(auditEntity).isNotEqualTo(null);
            assertThat(auditEntity).isNotEqualTo(new Object());
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            // When & Then
            assertThat(auditEntity).isEqualTo(auditEntity);
        }
    }

    // ============================================================================
    // STRING REPRESENTATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("String Representation Tests")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should generate proper toString representation")
        void shouldGenerateProperToStringRepresentation() {
            // Given
            auditEntity.setAuditId(123L);
            auditEntity.setEventType("WEBSOCKET_CONNECTION");
            auditEntity.setUserId("test-user");
            auditEntity.setSessionId("session-456");
            auditEntity.setEventSeverity("INFO");
            auditEntity.setRiskLevel("LOW");
            
            // When
            String toString = auditEntity.toString();
            
            // Then
            assertThat(toString).contains("WebSocketAuditLog[id=123");
            assertThat(toString).contains("type=WEBSOCKET_CONNECTION");
            assertThat(toString).contains("user=test-user");
            assertThat(toString).contains("session=session-456");
            assertThat(toString).contains("severity=INFO");
            assertThat(toString).contains("risk=LOW");
        }

        @Test
        @DisplayName("Should handle null values in toString")
        void shouldHandleNullValuesInToString() {
            // Given
            WebSocketAuditLogEntity entityWithNulls = WebSocketAuditLogEntity.builder().build();
            
            // When & Then - should not throw exception
            assertThatCode(() -> entityWithNulls.toString()).doesNotThrowAnyException();
        }
    }
}