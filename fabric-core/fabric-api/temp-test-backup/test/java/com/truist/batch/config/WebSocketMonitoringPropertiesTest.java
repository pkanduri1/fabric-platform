package com.truist.batch.config;

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
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * US008: Comprehensive Unit Tests for WebSocketMonitoringProperties
 * 
 * Tests configuration properties validation including:
 * - Bean validation constraints and boundary testing
 * - Configuration consistency validation rules
 * - Duration conversion and convenience methods
 * - Security and compliance setting validation
 * - Performance and resource limit validation
 * - Business logic for development vs production modes
 * 
 * Target: 95%+ code coverage with comprehensive validation testing
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@DisplayName("WebSocketMonitoringProperties Unit Tests")
class WebSocketMonitoringPropertiesTest {

    private Validator validator;
    private WebSocketMonitoringProperties properties;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        properties = new WebSocketMonitoringProperties();
        // Set default valid values
        properties.setAllowedOrigins(List.of("https://*.truist.com"));
        properties.setMaxConnections(100);
        properties.setMaxConnectionsPerUser(3);
        properties.setMaxConnectionsPerIp(10);
        properties.setConnectionTimeoutMs(30000L);
        properties.setMaxMessageSize(65536);
        properties.setUpdateIntervalMs(5000L);
        properties.setMinUpdateIntervalMs(2000L);
        properties.setMaxUpdateIntervalMs(15000L);
        properties.setTokenRotationIntervalMs(900000L);
        properties.setSessionValidationIntervalMs(60000L);
        properties.setMaxFailedAuthAttempts(5);
        properties.setRateLimitRequestsPerMinute(100);
        properties.setRateLimitConnectionsPerMinute(10);
        properties.setRateLimitBlockDurationMinutes(15);
        properties.setRedisKeyPrefix("fabric:websocket");
        properties.setRedisSessionTtlSeconds(3600L);
        properties.setChannelCorePoolSize(4);
        properties.setChannelMaxPoolSize(8);
        properties.setChannelKeepAliveSeconds(60);
        properties.setSchedulerPoolSize(4);
        properties.setCircuitBreakerFailureRate(50.0);
        properties.setCircuitBreakerWaitDurationMs(30000L);
        properties.setCircuitBreakerSlidingWindowSize(10);
        properties.setCircuitBreakerMinimumCalls(5);
        properties.setPerformanceMetricsIntervalMs(30000L);
        properties.setMemoryUsageAlertThreshold(85.0);
        properties.setCpuUsageAlertThreshold(90.0);
        properties.setResponseTimeAlertThresholdMs(5000L);
        properties.setErrorRateAlertThreshold(5.0);
        properties.setSessionStaleTimeoutMs(1800000L);
        properties.setGracefulShutdownTimeoutSeconds(30);
        properties.setCacheTtlSeconds(30L);
    }

    // ============================================================================
    // VALIDATION CONSTRAINT TESTS
    // ============================================================================

    @Nested
    @DisplayName("Validation Constraint Tests")
    class ValidationConstraintTests {

        @Test
        @DisplayName("Should validate valid properties without errors")
        void shouldValidateValidPropertiesWithoutErrors() {
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should reject null allowed origins")
        void shouldRejectNullAllowedOrigins() {
            // Given
            properties.setAllowedOrigins(null);
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("must not be null");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, 1001})
        @DisplayName("Should reject invalid max connections values")
        void shouldRejectInvalidMaxConnectionsValues(int maxConnections) {
            // Given
            properties.setMaxConnections(maxConnections);
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).hasSize(1);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, 11})
        @DisplayName("Should reject invalid max connections per user values")
        void shouldRejectInvalidMaxConnectionsPerUserValues(int maxConnectionsPerUser) {
            // Given
            properties.setMaxConnectionsPerUser(maxConnectionsPerUser);
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).hasSize(1);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, 51})
        @DisplayName("Should reject invalid max connections per IP values")
        void shouldRejectInvalidMaxConnectionsPerIpValues(int maxConnectionsPerIp) {
            // Given
            properties.setMaxConnectionsPerIp(maxConnectionsPerIp);
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).hasSize(1);
        }

        @ParameterizedTest
        @ValueSource(longs = {4999L, 300001L})
        @DisplayName("Should reject invalid connection timeout values")
        void shouldRejectInvalidConnectionTimeoutValues(long connectionTimeoutMs) {
            // Given
            properties.setConnectionTimeoutMs(connectionTimeoutMs);
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).hasSize(1);
        }

        @ParameterizedTest
        @ValueSource(ints = {1023, 1048577})
        @DisplayName("Should reject invalid max message size values")
        void shouldRejectInvalidMaxMessageSizeValues(int maxMessageSize) {
            // Given
            properties.setMaxMessageSize(maxMessageSize);
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).hasSize(1);
        }

        @ParameterizedTest
        @ValueSource(longs = {999L, 30001L})
        @DisplayName("Should reject invalid update interval values")
        void shouldRejectInvalidUpdateIntervalValues(long updateIntervalMs) {
            // Given
            properties.setUpdateIntervalMs(updateIntervalMs);
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).hasSize(1);
        }

        @ParameterizedTest
        @ValueSource(longs = {299999L, 3600001L})
        @DisplayName("Should reject invalid token rotation interval values")
        void shouldRejectInvalidTokenRotationIntervalValues(long tokenRotationIntervalMs) {
            // Given
            properties.setTokenRotationIntervalMs(tokenRotationIntervalMs);
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).hasSize(1);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 6, 11})
        @DisplayName("Should reject invalid failed auth attempts values")
        void shouldRejectInvalidFailedAuthAttemptsValues(int maxFailedAuthAttempts) {
            // Given
            properties.setMaxFailedAuthAttempts(maxFailedAuthAttempts);
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).hasSize(1);
        }

        @ParameterizedTest
        @ValueSource(ints = {9, 1001})
        @DisplayName("Should reject invalid rate limit requests per minute values")
        void shouldRejectInvalidRateLimitRequestsPerMinuteValues(int rateLimitRequestsPerMinute) {
            // Given
            properties.setRateLimitRequestsPerMinute(rateLimitRequestsPerMinute);
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).hasSize(1);
        }

        @Test
        @DisplayName("Should reject blank Redis key prefix")
        void shouldRejectBlankRedisKeyPrefix() {
            // Given
            properties.setRedisKeyPrefix("   ");
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("must not be blank");
        }

        @ParameterizedTest
        @ValueSource(doubles = {9.0, 91.0})
        @DisplayName("Should reject invalid circuit breaker failure rate values")
        void shouldRejectInvalidCircuitBreakerFailureRateValues(double failureRate) {
            // Given
            properties.setCircuitBreakerFailureRate(failureRate);
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).hasSize(1);
        }

        @ParameterizedTest
        @ValueSource(doubles = {49.0, 96.0})
        @DisplayName("Should reject invalid memory usage alert threshold values")
        void shouldRejectInvalidMemoryUsageAlertThresholdValues(double threshold) {
            // Given
            properties.setMemoryUsageAlertThreshold(threshold);
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).hasSize(1);
        }
    }

    // ============================================================================
    // CONFIGURATION CONSISTENCY VALIDATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Configuration Consistency Validation")
    class ConfigurationConsistencyTests {

        @Test
        @DisplayName("Should validate consistent thread pool configuration")
        void shouldValidateConsistentThreadPoolConfiguration() {
            // Given - valid configuration
            properties.setChannelCorePoolSize(4);
            properties.setChannelMaxPoolSize(8);
            
            // When & Then - should not throw exception
            assertThatCode(() -> properties.validateConfiguration()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject max pool size less than core pool size")
        void shouldRejectMaxPoolSizeLessThanCorePoolSize() {
            // Given
            properties.setChannelCorePoolSize(8);
            properties.setChannelMaxPoolSize(4); // Less than core
            
            // When & Then
            assertThatThrownBy(() -> properties.validateConfiguration())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Channel max pool size must be >= core pool size");
        }

        @Test
        @DisplayName("Should validate consistent update interval configuration")
        void shouldValidateConsistentUpdateIntervalConfiguration() {
            // Given - valid configuration
            properties.setMinUpdateIntervalMs(2000L);
            properties.setMaxUpdateIntervalMs(15000L);
            
            // When & Then - should not throw exception
            assertThatCode(() -> properties.validateConfiguration()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject max update interval less than or equal to min update interval")
        void shouldRejectMaxUpdateIntervalLessThanOrEqualToMinUpdateInterval() {
            // Given
            properties.setMinUpdateIntervalMs(15000L);
            properties.setMaxUpdateIntervalMs(15000L); // Equal to min
            
            // When & Then
            assertThatThrownBy(() -> properties.validateConfiguration())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Max update interval must be > min update interval");
        }

        @Test
        @DisplayName("Should reject max connections per user exceeding max connections")
        void shouldRejectMaxConnectionsPerUserExceedingMaxConnections() {
            // Given
            properties.setMaxConnections(5);
            properties.setMaxConnectionsPerUser(10); // Exceeds total
            
            // When & Then
            assertThatThrownBy(() -> properties.validateConfiguration())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Max connections per user cannot exceed max connections");
        }

        @Test
        @DisplayName("Should validate token rotation interval in production mode")
        void shouldValidateTokenRotationIntervalInProductionMode() {
            // Given - production mode
            properties.setDevelopmentMode(false);
            properties.setTokenRotationIntervalMs(300000L); // 5 minutes - minimum for production
            
            // When & Then - should not throw exception
            assertThatCode(() -> properties.validateConfiguration()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject short token rotation interval in production mode")
        void shouldRejectShortTokenRotationIntervalInProductionMode() {
            // Given - production mode
            properties.setDevelopmentMode(false);
            properties.setTokenRotationIntervalMs(240000L); // 4 minutes - below minimum
            
            // When & Then
            assertThatThrownBy(() -> properties.validateConfiguration())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Token rotation interval must be at least 5 minutes in production");
        }

        @Test
        @DisplayName("Should allow short token rotation interval in development mode")
        void shouldAllowShortTokenRotationIntervalInDevelopmentMode() {
            // Given - development mode
            properties.setDevelopmentMode(true);
            properties.setTokenRotationIntervalMs(240000L); // 4 minutes - allowed in dev
            
            // When & Then - should not throw exception
            assertThatCode(() -> properties.validateConfiguration()).doesNotThrowAnyException();
        }
    }

    // ============================================================================
    // DURATION CONVERSION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Duration Conversion Tests")
    class DurationConversionTests {

        @Test
        @DisplayName("Should convert update interval to Duration")
        void shouldConvertUpdateIntervalToDuration() {
            // Given
            properties.setUpdateIntervalMs(5000L);
            
            // When
            Duration duration = properties.getUpdateInterval();
            
            // Then
            assertThat(duration).isEqualTo(Duration.ofMillis(5000L));
        }

        @Test
        @DisplayName("Should convert heartbeat interval to Duration")
        void shouldConvertHeartbeatIntervalToDuration() {
            // Given
            properties.setHeartbeatIntervalMs(25000L);
            
            // When
            Duration duration = properties.getHeartbeatInterval();
            
            // Then
            assertThat(duration).isEqualTo(Duration.ofMillis(25000L));
        }

        @Test
        @DisplayName("Should convert connection timeout to Duration")
        void shouldConvertConnectionTimeoutToDuration() {
            // Given
            properties.setConnectionTimeoutMs(30000L);
            
            // When
            Duration duration = properties.getConnectionTimeout();
            
            // Then
            assertThat(duration).isEqualTo(Duration.ofMillis(30000L));
        }

        @Test
        @DisplayName("Should convert token rotation interval to Duration")
        void shouldConvertTokenRotationIntervalToDuration() {
            // Given
            properties.setTokenRotationIntervalMs(900000L);
            
            // When
            Duration duration = properties.getTokenRotationInterval();
            
            // Then
            assertThat(duration).isEqualTo(Duration.ofMillis(900000L));
        }

        @Test
        @DisplayName("Should convert session validation interval to Duration")
        void shouldConvertSessionValidationIntervalToDuration() {
            // Given
            properties.setSessionValidationIntervalMs(60000L);
            
            // When
            Duration duration = properties.getSessionValidationInterval();
            
            // Then
            assertThat(duration).isEqualTo(Duration.ofMillis(60000L));
        }

        @Test
        @DisplayName("Should convert Redis session TTL to Duration")
        void shouldConvertRedisSessionTtlToDuration() {
            // Given
            properties.setRedisSessionTtlSeconds(3600L);
            
            // When
            Duration duration = properties.getRedisSessionTtl();
            
            // Then
            assertThat(duration).isEqualTo(Duration.ofSeconds(3600L));
        }

        @Test
        @DisplayName("Should convert cache TTL to Duration")
        void shouldConvertCacheTtlToDuration() {
            // Given
            properties.setCacheTtlSeconds(30L);
            
            // When
            Duration duration = properties.getCacheTtl();
            
            // Then
            assertThat(duration).isEqualTo(Duration.ofSeconds(30L));
        }
    }

    // ============================================================================
    // BUSINESS LOGIC METHODS TESTS
    // ============================================================================

    @Nested
    @DisplayName("Business Logic Methods")
    class BusinessLogicMethodsTests {

        @Test
        @DisplayName("Should identify security enabled in production mode")
        void shouldIdentifySecurityEnabledInProductionMode() {
            // Given
            properties.setDevelopmentMode(false);
            
            // When
            boolean securityEnabled = properties.isSecurityEnabled();
            
            // Then
            assertThat(securityEnabled).isTrue();
        }

        @Test
        @DisplayName("Should identify security disabled in development mode")
        void shouldIdentifySecurityDisabledInDevelopmentMode() {
            // Given
            properties.setDevelopmentMode(true);
            
            // When
            boolean securityEnabled = properties.isSecurityEnabled();
            
            // Then
            assertThat(securityEnabled).isFalse();
        }

        @Test
        @DisplayName("Should identify comprehensive logging when both audit and security logging enabled")
        void shouldIdentifyComprehensiveLoggingWhenBothAuditAndSecurityLoggingEnabled() {
            // Given
            properties.setAuditLogging(true);
            properties.setSecurityEventLogging(true);
            
            // When
            boolean comprehensiveLogging = properties.isComprehensiveLoggingEnabled();
            
            // Then
            assertThat(comprehensiveLogging).isTrue();
        }

        @Test
        @DisplayName("Should not identify comprehensive logging when audit logging disabled")
        void shouldNotIdentifyComprehensiveLoggingWhenAuditLoggingDisabled() {
            // Given
            properties.setAuditLogging(false);
            properties.setSecurityEventLogging(true);
            
            // When
            boolean comprehensiveLogging = properties.isComprehensiveLoggingEnabled();
            
            // Then
            assertThat(comprehensiveLogging).isFalse();
        }

        @Test
        @DisplayName("Should not identify comprehensive logging when security event logging disabled")
        void shouldNotIdentifyComprehensiveLoggingWhenSecurityEventLoggingDisabled() {
            // Given
            properties.setAuditLogging(true);
            properties.setSecurityEventLogging(false);
            
            // When
            boolean comprehensiveLogging = properties.isComprehensiveLoggingEnabled();
            
            // Then
            assertThat(comprehensiveLogging).isFalse();
        }

        @Test
        @DisplayName("Should apply development multiplier for connection rate limits")
        void shouldApplyDevelopmentMultiplierForConnectionRateLimits() {
            // Given
            properties.setDevelopmentMode(true);
            properties.setRateLimitConnectionsPerMinute(10);
            
            // When
            int effectiveLimit = properties.getEffectiveConnectionRateLimit();
            
            // Then
            assertThat(effectiveLimit).isEqualTo(100); // 10 * 10 multiplier
        }

        @Test
        @DisplayName("Should use standard rate limit for connections in production")
        void shouldUseStandardRateLimitForConnectionsInProduction() {
            // Given
            properties.setDevelopmentMode(false);
            properties.setRateLimitConnectionsPerMinute(10);
            
            // When
            int effectiveLimit = properties.getEffectiveConnectionRateLimit();
            
            // Then
            assertThat(effectiveLimit).isEqualTo(10); // No multiplier
        }

        @Test
        @DisplayName("Should apply development multiplier for request rate limits")
        void shouldApplyDevelopmentMultiplierForRequestRateLimits() {
            // Given
            properties.setDevelopmentMode(true);
            properties.setRateLimitRequestsPerMinute(100);
            
            // When
            int effectiveLimit = properties.getEffectiveRequestRateLimit();
            
            // Then
            assertThat(effectiveLimit).isEqualTo(1000); // 100 * 10 multiplier
        }

        @Test
        @DisplayName("Should use standard rate limit for requests in production")
        void shouldUseStandardRateLimitForRequestsInProduction() {
            // Given
            properties.setDevelopmentMode(false);
            properties.setRateLimitRequestsPerMinute(100);
            
            // When
            int effectiveLimit = properties.getEffectiveRequestRateLimit();
            
            // Then
            assertThat(effectiveLimit).isEqualTo(100); // No multiplier
        }
    }

    // ============================================================================
    // BOUNDARY VALUE TESTS
    // ============================================================================

    @Nested
    @DisplayName("Boundary Value Tests")
    class BoundaryValueTests {

        @Test
        @DisplayName("Should accept minimum valid connection timeout")
        void shouldAcceptMinimumValidConnectionTimeout() {
            // Given
            properties.setConnectionTimeoutMs(5000L); // Minimum
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept maximum valid connection timeout")
        void shouldAcceptMaximumValidConnectionTimeout() {
            // Given
            properties.setConnectionTimeoutMs(300000L); // Maximum
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept minimum valid max connections")
        void shouldAcceptMinimumValidMaxConnections() {
            // Given
            properties.setMaxConnections(1); // Minimum
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept maximum valid max connections")
        void shouldAcceptMaximumValidMaxConnections() {
            // Given
            properties.setMaxConnections(1000); // Maximum
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept minimum valid circuit breaker failure rate")
        void shouldAcceptMinimumValidCircuitBreakerFailureRate() {
            // Given
            properties.setCircuitBreakerFailureRate(10.0); // Minimum
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept maximum valid circuit breaker failure rate")
        void shouldAcceptMaximumValidCircuitBreakerFailureRate() {
            // Given
            properties.setCircuitBreakerFailureRate(90.0); // Maximum
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept minimum valid session stale timeout")
        void shouldAcceptMinimumValidSessionStaleTimeout() {
            // Given
            properties.setSessionStaleTimeoutMs(300000L); // 5 minutes minimum
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept maximum valid session stale timeout")
        void shouldAcceptMaximumValidSessionStaleTimeout() {
            // Given
            properties.setSessionStaleTimeoutMs(3600000L); // 1 hour maximum
            
            // When
            Set<ConstraintViolation<WebSocketMonitoringProperties>> violations = validator.validate(properties);
            
            // Then
            assertThat(violations).isEmpty();
        }
    }

    // ============================================================================
    // DEFAULT VALUES TESTS
    // ============================================================================

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have secure default values")
        void shouldHaveSecureDefaultValues() {
            // Given
            WebSocketMonitoringProperties defaultProperties = new WebSocketMonitoringProperties();
            
            // Then - verify secure defaults
            assertThat(defaultProperties.isAllowLocalhost()).isFalse();
            assertThat(defaultProperties.isDevelopmentMode()).isFalse();
            assertThat(defaultProperties.isSecurityEnabled()).isTrue();
            assertThat(defaultProperties.isAuditLogging()).isTrue();
            assertThat(defaultProperties.isSecurityEventLogging()).isTrue();
            assertThat(defaultProperties.isCircuitBreakerEnabled()).isTrue();
            assertThat(defaultProperties.isPerformanceMonitoring()).isTrue();
            assertThat(defaultProperties.isHealthCheckEnabled()).isTrue();
            assertThat(defaultProperties.isMetricsExportEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should have reasonable performance defaults")
        void shouldHaveReasonablePerformanceDefaults() {
            // Given
            WebSocketMonitoringProperties defaultProperties = new WebSocketMonitoringProperties();
            
            // Then - verify performance defaults
            assertThat(defaultProperties.getMaxConnections()).isEqualTo(100);
            assertThat(defaultProperties.getMaxConnectionsPerUser()).isEqualTo(3);
            assertThat(defaultProperties.getMaxConnectionsPerIp()).isEqualTo(10);
            assertThat(defaultProperties.getUpdateIntervalMs()).isEqualTo(5000L);
            assertThat(defaultProperties.getHeartbeatIntervalMs()).isEqualTo(25000L);
            assertThat(defaultProperties.getConnectionTimeoutMs()).isEqualTo(30000L);
        }

        @Test
        @DisplayName("Should have appropriate security defaults")
        void shouldHaveAppropriateSecurityDefaults() {
            // Given
            WebSocketMonitoringProperties defaultProperties = new WebSocketMonitoringProperties();
            
            // Then - verify security defaults
            assertThat(defaultProperties.getTokenRotationIntervalMs()).isEqualTo(900000L); // 15 minutes
            assertThat(defaultProperties.getMaxFailedAuthAttempts()).isEqualTo(5);
            assertThat(defaultProperties.getRateLimitRequestsPerMinute()).isEqualTo(100);
            assertThat(defaultProperties.getRateLimitConnectionsPerMinute()).isEqualTo(10);
            assertThat(defaultProperties.getRateLimitBlockDurationMinutes()).isEqualTo(15);
        }
    }

    // ============================================================================
    // FEATURE FLAG TESTS
    // ============================================================================

    @Nested
    @DisplayName("Feature Flag Tests")
    class FeatureFlagTests {

        @Test
        @DisplayName("Should enable all features by default except development mode")
        void shouldEnableAllFeaturesByDefaultExceptDevelopmentMode() {
            // Given
            WebSocketMonitoringProperties defaultProperties = new WebSocketMonitoringProperties();
            
            // Then
            assertThat(defaultProperties.isAdaptiveIntervals()).isTrue();
            assertThat(defaultProperties.isDeltaUpdates()).isTrue();
            assertThat(defaultProperties.isMessageCompression()).isTrue();
            assertThat(defaultProperties.isRoleBasedFiltering()).isTrue();
            assertThat(defaultProperties.isAuditLogging()).isTrue();
            assertThat(defaultProperties.isSecurityEventLogging()).isTrue();
            assertThat(defaultProperties.isRedisSessionClustering()).isTrue();
            assertThat(defaultProperties.isCacheUpdateMetrics()).isTrue();
            assertThat(defaultProperties.isCircuitBreakerEnabled()).isTrue();
            assertThat(defaultProperties.isPerformanceMonitoring()).isTrue();
            assertThat(defaultProperties.isDevelopmentMode()).isFalse(); // Should be false by default
            assertThat(defaultProperties.isDebugLogging()).isFalse(); // Should be false by default
            assertThat(defaultProperties.isMetricsExportEnabled()).isTrue();
            assertThat(defaultProperties.isHealthCheckEnabled()).isTrue();
            assertThat(defaultProperties.isGracefulShutdownEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should allow disabling individual features")
        void shouldAllowDisablingIndividualFeatures() {
            // Given & When
            properties.setAdaptiveIntervals(false);
            properties.setDeltaUpdates(false);
            properties.setMessageCompression(false);
            properties.setRoleBasedFiltering(false);
            properties.setAuditLogging(false);
            properties.setSecurityEventLogging(false);
            
            // Then
            assertThat(properties.isAdaptiveIntervals()).isFalse();
            assertThat(properties.isDeltaUpdates()).isFalse();
            assertThat(properties.isMessageCompression()).isFalse();
            assertThat(properties.isRoleBasedFiltering()).isFalse();
            assertThat(properties.isAuditLogging()).isFalse();
            assertThat(properties.isSecurityEventLogging()).isFalse();
            assertThat(properties.isComprehensiveLoggingEnabled()).isFalse(); // Should be false when both disabled
        }
    }
}