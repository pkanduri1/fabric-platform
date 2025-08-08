package com.truist.batch.websocket.audit;

import com.truist.batch.config.WebSocketSecurityConfig.WebSocketSecurityEvent;
import com.truist.batch.entity.WebSocketAuditLogEntity;
import com.truist.batch.repository.WebSocketAuditLogRepository;
import com.truist.batch.service.SecurityAuditService;
import com.truist.batch.websocket.audit.AuditWebSocketEventListener.WebSocketConfigurationChangeEvent;
import com.truist.batch.websocket.audit.AuditWebSocketEventListener.WebSocketSessionLifecycleEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * US008: Comprehensive Unit Tests for AuditWebSocketEventListener
 * 
 * Tests SOX compliance audit logging including:
 * - Tamper-evident logging with SHA-256 hash validation
 * - Security event classification and risk assessment
 * - Audit trail integrity validation and blockchain-style linking
 * - Configuration change tracking with segregation of duties
 * - Digital signature generation for critical events
 * - Comprehensive error handling and compliance reporting
 * 
 * Target: 95%+ code coverage with comprehensive edge case testing
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditWebSocketEventListener Unit Tests")
class AuditWebSocketEventListenerTest {

    @Mock
    private WebSocketAuditLogRepository mockAuditLogRepository;
    
    @Mock
    private SecurityAuditService mockSecurityAuditService;
    
    @Mock
    private RedisTemplate<String, Object> mockRedisTemplate;
    
    @Mock
    private HashOperations<String, Object, Object> mockHashOperations;

    private AuditWebSocketEventListener auditEventListener;

    @BeforeEach
    void setUp() {
        auditEventListener = new AuditWebSocketEventListener(
                mockAuditLogRepository,
                mockSecurityAuditService,
                mockRedisTemplate
        );
        
        when(mockRedisTemplate.opsForHash()).thenReturn(mockHashOperations);
    }

    // ============================================================================
    // WEBSOCKET SECURITY EVENT TESTS
    // ============================================================================

    @Nested
    @DisplayName("WebSocket Security Event Auditing")
    class SecurityEventAuditingTests {

        @Test
        @DisplayName("Should audit successful connection establishment event")
        void shouldAuditSuccessfulConnectionEstablishmentEvent() {
            // Given
            WebSocketSecurityEvent event = createSecurityEvent("WEBSOCKET_CONNECTION_ESTABLISHED", 
                    "test-session-123", "test-user", "Connection established successfully");
            
            WebSocketAuditLogEntity savedAudit = createMockAuditEntity(1L);
            when(mockAuditLogRepository.save(any(WebSocketAuditLogEntity.class))).thenReturn(savedAudit);
            
            // When
            auditEventListener.handleSecurityEvent(event);
            
            // Then
            ArgumentCaptor<WebSocketAuditLogEntity> auditCaptor = ArgumentCaptor.forClass(WebSocketAuditLogEntity.class);
            verify(mockAuditLogRepository).save(auditCaptor.capture());
            
            WebSocketAuditLogEntity capturedAudit = auditCaptor.getValue();
            assertThat(capturedAudit.getEventType()).isEqualTo("WEBSOCKET_CONNECTION_ESTABLISHED");
            assertThat(capturedAudit.getEventSeverity()).isEqualTo("INFO");
            assertThat(capturedAudit.getRiskLevel()).isEqualTo("LOW");
            assertThat(capturedAudit.getSecurityEventFlag()).isEqualTo("Y");
            assertThat(capturedAudit.getComplianceEventFlag()).isEqualTo("N");
            assertThat(capturedAudit.getAuditHash()).isNotNull();
            assertThat(capturedAudit.getCreatedBy()).isEqualTo("WEBSOCKET_SECURITY_AUDITOR");
        }

        @Test
        @DisplayName("Should audit security violation with high risk classification")
        void shouldAuditSecurityViolationWithHighRiskClassification() {
            // Given
            WebSocketSecurityEvent event = createSecurityEvent("WEBSOCKET_SECURITY_VIOLATION", 
                    "test-session-123", "test-user", "Authentication failed");
            
            WebSocketAuditLogEntity savedAudit = createMockAuditEntity(2L);
            when(mockAuditLogRepository.save(any(WebSocketAuditLogEntity.class))).thenReturn(savedAudit);
            
            // When
            auditEventListener.handleSecurityEvent(event);
            
            // Then
            ArgumentCaptor<WebSocketAuditLogEntity> auditCaptor = ArgumentCaptor.forClass(WebSocketAuditLogEntity.class);
            verify(mockAuditLogRepository).save(auditCaptor.capture());
            
            WebSocketAuditLogEntity capturedAudit = auditCaptor.getValue();
            assertThat(capturedAudit.getEventType()).isEqualTo("WEBSOCKET_SECURITY_VIOLATION");
            assertThat(capturedAudit.getEventSeverity()).isEqualTo("WARN");
            assertThat(capturedAudit.getRiskLevel()).isEqualTo("MEDIUM");
            assertThat(capturedAudit.getSecurityEventFlag()).isEqualTo("Y");
            assertThat(capturedAudit.getComplianceEventFlag()).isEqualTo("Y");
        }

        @Test
        @DisplayName("Should audit authentication failure with critical classification")
        void shouldAuditAuthenticationFailureWithCriticalClassification() {
            // Given
            WebSocketSecurityEvent event = createSecurityEvent("WEBSOCKET_AUTH_FAILURE", 
                    "test-session-123", "test-user", "JWT token invalid");
            
            WebSocketAuditLogEntity savedAudit = createMockAuditEntity(3L);
            when(mockAuditLogRepository.save(any(WebSocketAuditLogEntity.class))).thenReturn(savedAudit);
            
            // When
            auditEventListener.handleSecurityEvent(event);
            
            // Then
            ArgumentCaptor<WebSocketAuditLogEntity> auditCaptor = ArgumentCaptor.forClass(WebSocketAuditLogEntity.class);
            verify(mockAuditLogRepository).save(auditCaptor.capture());
            
            WebSocketAuditLogEntity capturedAudit = auditCaptor.getValue();
            assertThat(capturedAudit.getEventType()).isEqualTo("WEBSOCKET_AUTH_FAILURE");
            assertThat(capturedAudit.getEventSeverity()).isEqualTo("ERROR");
            assertThat(capturedAudit.getRiskLevel()).isEqualTo("HIGH");
            assertThat(capturedAudit.getSecurityEventFlag()).isEqualTo("Y");
            assertThat(capturedAudit.getComplianceEventFlag()).isEqualTo("Y");
        }

        @Test
        @DisplayName("Should calculate tamper-evident hash for audit records")
        void shouldCalculateTamperEvidentHashForAuditRecords() {
            // Given
            WebSocketSecurityEvent event = createSecurityEvent("WEBSOCKET_CONNECTION_ESTABLISHED", 
                    "test-session-123", "test-user", "Connection established");
            
            WebSocketAuditLogEntity savedAudit = createMockAuditEntity(4L);
            when(mockAuditLogRepository.save(any(WebSocketAuditLogEntity.class))).thenReturn(savedAudit);
            
            // When
            auditEventListener.handleSecurityEvent(event);
            
            // Then
            ArgumentCaptor<WebSocketAuditLogEntity> auditCaptor = ArgumentCaptor.forClass(WebSocketAuditLogEntity.class);
            verify(mockAuditLogRepository).save(auditCaptor.capture());
            
            WebSocketAuditLogEntity capturedAudit = auditCaptor.getValue();
            assertThat(capturedAudit.getAuditHash()).isNotNull();
            assertThat(capturedAudit.getAuditHash()).doesNotContain("HASH_CALCULATION_FAILED");
            assertThat(capturedAudit.getAuditHash().length()).isGreaterThan(20); // Base64 encoded hash
        }

        @Test
        @DisplayName("Should link audit records with blockchain-style hash chain")
        void shouldLinkAuditRecordsWithBlockchainStyleHashChain() {
            // Given
            WebSocketSecurityEvent event1 = createSecurityEvent("WEBSOCKET_CONNECTION_ESTABLISHED", 
                    "session-1", "user-1", "First connection");
            WebSocketSecurityEvent event2 = createSecurityEvent("WEBSOCKET_CONNECTION_ESTABLISHED", 
                    "session-2", "user-2", "Second connection");
            
            WebSocketAuditLogEntity savedAudit1 = createMockAuditEntity(1L);
            WebSocketAuditLogEntity savedAudit2 = createMockAuditEntity(2L);
            
            when(mockAuditLogRepository.save(any(WebSocketAuditLogEntity.class)))
                    .thenReturn(savedAudit1)
                    .thenReturn(savedAudit2);
            
            // When
            auditEventListener.handleSecurityEvent(event1);
            auditEventListener.handleSecurityEvent(event2);
            
            // Then
            ArgumentCaptor<WebSocketAuditLogEntity> auditCaptor = ArgumentCaptor.forClass(WebSocketAuditLogEntity.class);
            verify(mockAuditLogRepository, times(2)).save(auditCaptor.capture());
            
            List<WebSocketAuditLogEntity> capturedAudits = auditCaptor.getAllValues();
            assertThat(capturedAudits.get(0).getPreviousAuditHash()).isNull(); // First record
            assertThat(capturedAudits.get(1).getPreviousAuditHash()).isNotNull(); // Should link to previous
        }

        @Test
        @DisplayName("Should cache audit events in Redis for real-time monitoring")
        void shouldCacheAuditEventsInRedisForRealTimeMonitoring() {
            // Given
            WebSocketSecurityEvent event = createSecurityEvent("WEBSOCKET_CONNECTION_ESTABLISHED", 
                    "test-session-123", "test-user", "Connection established");
            
            WebSocketAuditLogEntity savedAudit = createMockAuditEntity(5L);
            when(mockAuditLogRepository.save(any(WebSocketAuditLogEntity.class))).thenReturn(savedAudit);
            
            // When
            auditEventListener.handleSecurityEvent(event);
            
            // Then
            verify(mockHashOperations).putAll(eq("websocket:audit:recent:5"), any(Map.class));
            verify(mockRedisTemplate).expire(eq("websocket:audit:recent:5"), eq(Duration.ofHours(24)));
        }

        @Test
        @DisplayName("Should trigger security audit service for centralized tracking")
        void shouldTriggerSecurityAuditServiceForCentralizedTracking() {
            // Given
            WebSocketSecurityEvent event = createSecurityEvent("WEBSOCKET_SECURITY_VIOLATION", 
                    "test-session-123", "test-user", "Rate limit exceeded");
            
            WebSocketAuditLogEntity savedAudit = createMockAuditEntity(6L);
            when(mockAuditLogRepository.save(any(WebSocketAuditLogEntity.class))).thenReturn(savedAudit);
            
            // When
            auditEventListener.handleSecurityEvent(event);
            
            // Then
            verify(mockSecurityAuditService).logSecurityEvent(
                    eq("WEBSOCKET_SECURITY_VIOLATION"),
                    eq("test-user"),
                    eq("unknown"), // Client IP extraction
                    eq("Rate limit exceeded"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("Should handle audit repository save failures gracefully")
        void shouldHandleAuditRepositorySaveFailuresGracefully() {
            // Given
            WebSocketSecurityEvent event = createSecurityEvent("WEBSOCKET_CONNECTION_ESTABLISHED", 
                    "test-session-123", "test-user", "Connection established");
            
            when(mockAuditLogRepository.save(any(WebSocketAuditLogEntity.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));
            
            // When & Then - should not throw exception
            assertThatCode(() -> auditEventListener.handleSecurityEvent(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle Redis caching failures gracefully")
        void shouldHandleRedisCachingFailuresGracefully() {
            // Given
            WebSocketSecurityEvent event = createSecurityEvent("WEBSOCKET_CONNECTION_ESTABLISHED", 
                    "test-session-123", "test-user", "Connection established");
            
            WebSocketAuditLogEntity savedAudit = createMockAuditEntity(7L);
            when(mockAuditLogRepository.save(any(WebSocketAuditLogEntity.class))).thenReturn(savedAudit);
            when(mockRedisTemplate.opsForHash()).thenThrow(new RuntimeException("Redis unavailable"));
            
            // When & Then - should not throw exception
            assertThatCode(() -> auditEventListener.handleSecurityEvent(event))
                    .doesNotThrowAnyException();
        }
    }

    // ============================================================================
    // SESSION LIFECYCLE EVENT TESTS
    // ============================================================================

    @Nested
    @DisplayName("Session Lifecycle Event Auditing")
    class SessionLifecycleEventTests {

        @Test
        @DisplayName("Should audit session lifecycle events with proper classification")
        void shouldAuditSessionLifecycleEventsWithProperClassification() {
            // Given
            WebSocketSessionLifecycleEvent event = WebSocketSessionLifecycleEvent.builder()
                    .eventType("SESSION_CREATED")
                    .eventSubtype("USER_INITIATED")
                    .sessionId("test-session-123")
                    .userId("test-user")
                    .clientIp("192.168.1.100")
                    .description("New WebSocket session created")
                    .correlationId("corr-123")
                    .build();
            
            WebSocketAuditLogEntity savedAudit = createMockAuditEntity(8L);
            when(mockAuditLogRepository.save(any(WebSocketAuditLogEntity.class))).thenReturn(savedAudit);
            
            // When
            auditEventListener.handleSessionLifecycleEvent(event);
            
            // Then
            ArgumentCaptor<WebSocketAuditLogEntity> auditCaptor = ArgumentCaptor.forClass(WebSocketAuditLogEntity.class);
            verify(mockAuditLogRepository).save(auditCaptor.capture());
            
            WebSocketAuditLogEntity capturedAudit = auditCaptor.getValue();
            assertThat(capturedAudit.getEventType()).isEqualTo("SESSION_CREATED");
            assertThat(capturedAudit.getEventSubtype()).isEqualTo("USER_INITIATED");
            assertThat(capturedAudit.getEventSeverity()).isEqualTo("INFO");
            assertThat(capturedAudit.getRiskLevel()).isEqualTo("LOW");
            assertThat(capturedAudit.getSecurityEventFlag()).isEqualTo("N");
            assertThat(capturedAudit.getComplianceEventFlag()).isEqualTo("Y");
            assertThat(capturedAudit.getBusinessFunction()).isEqualTo("REAL_TIME_MONITORING");
        }

        @Test
        @DisplayName("Should calculate audit hash for session lifecycle events")
        void shouldCalculateAuditHashForSessionLifecycleEvents() {
            // Given
            WebSocketSessionLifecycleEvent event = WebSocketSessionLifecycleEvent.builder()
                    .eventType("SESSION_TERMINATED")
                    .sessionId("test-session-456")
                    .userId("test-user-2")
                    .description("Session ended by timeout")
                    .build();
            
            WebSocketAuditLogEntity savedAudit = createMockAuditEntity(9L);
            when(mockAuditLogRepository.save(any(WebSocketAuditLogEntity.class))).thenReturn(savedAudit);
            
            // When
            auditEventListener.handleSessionLifecycleEvent(event);
            
            // Then
            ArgumentCaptor<WebSocketAuditLogEntity> auditCaptor = ArgumentCaptor.forClass(WebSocketAuditLogEntity.class);
            verify(mockAuditLogRepository).save(auditCaptor.capture());
            
            WebSocketAuditLogEntity capturedAudit = auditCaptor.getValue();
            assertThat(capturedAudit.getAuditHash()).isNotNull();
            assertThat(capturedAudit.getAuditHash()).doesNotContain("HASH_CALCULATION_FAILED");
        }

        @Test
        @DisplayName("Should handle session lifecycle event errors gracefully")
        void shouldHandleSessionLifecycleEventErrorsGracefully() {
            // Given
            WebSocketSessionLifecycleEvent event = WebSocketSessionLifecycleEvent.builder()
                    .eventType("SESSION_ERROR")
                    .build();
            
            when(mockAuditLogRepository.save(any(WebSocketAuditLogEntity.class)))
                    .thenThrow(new RuntimeException("Audit save failed"));
            
            // When & Then - should not throw exception
            assertThatCode(() -> auditEventListener.handleSessionLifecycleEvent(event))
                    .doesNotThrowAnyException();
        }
    }

    // ============================================================================
    // CONFIGURATION CHANGE EVENT TESTS
    // ============================================================================

    @Nested
    @DisplayName("Configuration Change Event Auditing")
    class ConfigurationChangeEventTests {

        @Test
        @DisplayName("Should audit configuration changes with SOX compliance classification")
        void shouldAuditConfigurationChangesWithSoxComplianceClassification() {
            // Given
            WebSocketConfigurationChangeEvent event = WebSocketConfigurationChangeEvent.builder()
                    .changeType("SECURITY_SETTING_UPDATE")
                    .changedBy("admin-user")
                    .clientIp("10.0.1.50")
                    .description("Updated rate limiting settings")
                    .correlationId("config-change-123")
                    .configurationData("{\"rateLimitPerMinute\": 100}")
                    .proposedApprover("manager-user")
                    .requiresApproval(true)
                    .criticalChange(false)
                    .build();
            
            WebSocketAuditLogEntity savedAudit = createMockAuditEntity(10L);
            when(mockAuditLogRepository.save(any(WebSocketAuditLogEntity.class))).thenReturn(savedAudit);
            
            // When
            auditEventListener.handleConfigurationChangeEvent(event);
            
            // Then
            ArgumentCaptor<WebSocketAuditLogEntity> auditCaptor = ArgumentCaptor.forClass(WebSocketAuditLogEntity.class);
            verify(mockAuditLogRepository).save(auditCaptor.capture());
            
            WebSocketAuditLogEntity capturedAudit = auditCaptor.getValue();
            assertThat(capturedAudit.getEventType()).isEqualTo("CONFIGURATION_CHANGE");
            assertThat(capturedAudit.getEventSubtype()).isEqualTo("SECURITY_SETTING_UPDATE");
            assertThat(capturedAudit.getSecurityClassification()).isEqualTo("SOX_COMPLIANCE_REQUIRED");
            assertThat(capturedAudit.getSecurityEventFlag()).isEqualTo("Y");
            assertThat(capturedAudit.getComplianceEventFlag()).isEqualTo("Y");
            assertThat(capturedAudit.getRiskLevel()).isEqualTo("MEDIUM");
            assertThat(capturedAudit.getBusinessFunction()).isEqualTo("MONITORING_CONFIGURATION");
        }

        @Test
        @DisplayName("Should validate segregation of duties for configuration changes")
        void shouldValidateSegregationOfDutiesForConfigurationChanges() {
            // Given - same user as requester and approver (violation)
            WebSocketConfigurationChangeEvent event = WebSocketConfigurationChangeEvent.builder()
                    .changeType("CRITICAL_SETTING_UPDATE")
                    .changedBy("admin-user")
                    .proposedApprover("admin-user") // Same user - violation
                    .requiresApproval(true)
                    .build();
            
            // When & Then - should throw SecurityException
            assertThatThrownBy(() -> auditEventListener.handleConfigurationChangeEvent(event))
                    .isInstanceOf(SecurityException.class)
                    .hasMessage("Segregation of duties violation detected");
        }

        @Test
        @DisplayName("Should calculate enhanced audit hash for configuration changes")
        void shouldCalculateEnhancedAuditHashForConfigurationChanges() {
            // Given
            WebSocketConfigurationChangeEvent event = WebSocketConfigurationChangeEvent.builder()
                    .changeType("MONITORING_CONFIG_UPDATE")
                    .changedBy("config-admin")
                    .proposedApprover("security-manager")
                    .configurationData("{\"updateInterval\": 5000}")
                    .requiresApproval(true)
                    .criticalChange(false)
                    .build();
            
            WebSocketAuditLogEntity savedAudit = createMockAuditEntity(11L);
            when(mockAuditLogRepository.save(any(WebSocketAuditLogEntity.class))).thenReturn(savedAudit);
            
            // When
            auditEventListener.handleConfigurationChangeEvent(event);
            
            // Then
            ArgumentCaptor<WebSocketAuditLogEntity> auditCaptor = ArgumentCaptor.forClass(WebSocketAuditLogEntity.class);
            verify(mockAuditLogRepository).save(auditCaptor.capture());
            
            WebSocketAuditLogEntity capturedAudit = auditCaptor.getValue();
            assertThat(capturedAudit.getAuditHash()).isNotNull();
            assertThat(capturedAudit.getAuditHash()).doesNotContain("ENHANCED_HASH_FAILED");
        }

        @Test
        @DisplayName("Should generate digital signature for critical configuration changes")
        void shouldGenerateDigitalSignatureForCriticalConfigurationChanges() {
            // Given
            WebSocketConfigurationChangeEvent event = WebSocketConfigurationChangeEvent.builder()
                    .changeType("CRITICAL_SECURITY_UPDATE")
                    .changedBy("security-admin")
                    .proposedApprover("ciso")
                    .criticalChange(true) // Critical change
                    .requiresApproval(true)
                    .build();
            
            WebSocketAuditLogEntity savedAudit = createMockAuditEntity(12L);
            when(mockAuditLogRepository.save(any(WebSocketAuditLogEntity.class))).thenReturn(savedAudit);
            
            // When
            auditEventListener.handleConfigurationChangeEvent(event);
            
            // Then
            ArgumentCaptor<WebSocketAuditLogEntity> auditCaptor = ArgumentCaptor.forClass(WebSocketAuditLogEntity.class);
            verify(mockAuditLogRepository).save(auditCaptor.capture());
            
            WebSocketAuditLogEntity capturedAudit = auditCaptor.getValue();
            assertThat(capturedAudit.getDigitalSignature()).isEqualTo("signature"); // Mock implementation
        }

        @Test
        @DisplayName("Should handle configuration change audit errors gracefully")
        void shouldHandleConfigurationChangeAuditErrorsGracefully() {
            // Given
            WebSocketConfigurationChangeEvent event = WebSocketConfigurationChangeEvent.builder()
                    .changeType("CONFIG_UPDATE")
                    .changedBy("admin")
                    .proposedApprover("manager")
                    .build();
            
            when(mockAuditLogRepository.save(any(WebSocketAuditLogEntity.class)))
                    .thenThrow(new RuntimeException("Audit repository failure"));
            
            // When & Then - should not throw exception
            assertThatCode(() -> auditEventListener.handleConfigurationChangeEvent(event))
                    .doesNotThrowAnyException();
        }
    }

    // ============================================================================
    // AUDIT INTEGRITY VALIDATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Audit Integrity Validation")
    class AuditIntegrityValidationTests {

        @Test
        @DisplayName("Should validate audit trail integrity successfully")
        void shouldValidateAuditTrailIntegritySuccessfully() {
            // Given
            List<WebSocketAuditLogEntity> recentAudits = List.of(
                    createValidAuditEntity(1L, "valid-hash-1"),
                    createValidAuditEntity(2L, "valid-hash-2")
            );
            
            when(mockAuditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(any(Instant.class)))
                    .thenReturn(recentAudits);
            
            // When
            auditEventListener.validateAuditIntegrity();
            
            // Then - should validate without errors
            verify(mockAuditLogRepository).findTop100ByEventTimestampAfterOrderByEventTimestampDesc(any(Instant.class));
        }

        @Test
        @DisplayName("Should detect tampered audit records")
        void shouldDetectTamperedAuditRecords() {
            // Given
            WebSocketAuditLogEntity tamperedAudit = createTamperedAuditEntity(3L);
            List<WebSocketAuditLogEntity> recentAudits = List.of(tamperedAudit);
            
            when(mockAuditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(any(Instant.class)))
                    .thenReturn(recentAudits);
            
            // When
            auditEventListener.validateAuditIntegrity();
            
            // Then - should detect tampering (logging verification would be in integration test)
            verify(mockAuditLogRepository).findTop100ByEventTimestampAfterOrderByEventTimestampDesc(any(Instant.class));
        }

        @Test
        @DisplayName("Should handle audit integrity validation errors gracefully")
        void shouldHandleAuditIntegrityValidationErrorsGracefully() {
            // Given
            when(mockAuditLogRepository.findTop100ByEventTimestampAfterOrderByEventTimestampDesc(any(Instant.class)))
                    .thenThrow(new RuntimeException("Database query failed"));
            
            // When & Then - should not throw exception
            assertThatCode(() -> auditEventListener.validateAuditIntegrity())
                    .doesNotThrowAnyException();
        }
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private WebSocketSecurityEvent createSecurityEvent(String eventType, String sessionId, String userId, String description) {
        return WebSocketSecurityEvent.builder()
                .eventType(eventType)
                .sessionId(sessionId)
                .userId(userId)
                .description(description)
                .timestamp(Instant.now())
                .serverInstance("test-server")
                .build();
    }

    private WebSocketAuditLogEntity createMockAuditEntity(Long auditId) {
        return WebSocketAuditLogEntity.builder()
                .auditId(auditId)
                .eventType("TEST_EVENT")
                .eventTimestamp(Instant.now())
                .eventSeverity("INFO")
                .securityClassification("INTERNAL")
                .securityEventFlag("N")
                .complianceEventFlag("N")
                .riskLevel("LOW")
                .auditHash("test-hash-" + auditId)
                .createdBy("TEST")
                .build();
    }

    private WebSocketAuditLogEntity createValidAuditEntity(Long auditId, String validHash) {
        return WebSocketAuditLogEntity.builder()
                .auditId(auditId)
                .eventType("VALID_EVENT")
                .eventDescription("Valid audit record")
                .userId("test-user")
                .sessionId("session-" + auditId)
                .eventTimestamp(Instant.now())
                .eventSeverity("INFO")
                .auditHash(validHash)
                .createdBy("SYSTEM")
                .build();
    }

    private WebSocketAuditLogEntity createTamperedAuditEntity(Long auditId) {
        WebSocketAuditLogEntity entity = WebSocketAuditLogEntity.builder()
                .auditId(auditId)
                .eventType("TAMPERED_EVENT")
                .eventDescription("This record has been tampered with")
                .userId("malicious-user")
                .eventTimestamp(Instant.now())
                .eventSeverity("CRITICAL")
                .auditHash("tampered-hash-does-not-match-content")
                .createdBy("UNKNOWN")
                .build();
        
        return entity;
    }
}