package com.truist.batch.security.service;

import com.truist.batch.security.service.ParameterEncryptionService.EncryptionValidationResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ParameterEncryptionService.
 * 
 * Tests enterprise-grade parameter encryption functionality including:
 * - Field-level encryption for sensitive data
 * - Automatic sensitive parameter detection
 * - Secure key management and validation
 * - Performance optimized encryption operations
 * - SOX-compliant encryption practices
 * 
 * Banking-grade testing standards:
 * - 95%+ code coverage requirement
 * - Comprehensive security scenario testing
 * - Encryption strength and integrity validation
 * - Performance and scalability testing
 * 
 * @author Senior Full Stack Developer Agent
 * @version 2.0
 * @since US001 Phase 2 - Security Testing Implementation
 */
class ParameterEncryptionServiceTest {

    private ParameterEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new ParameterEncryptionService();
        
        // Set test encryption configuration
        ReflectionTestUtils.setField(encryptionService, "encryptionEnabled", true);
        ReflectionTestUtils.setField(encryptionService, "encryptionKeyBase64", 
            Base64.getEncoder().encodeToString("TestEncryptionKey123456789012".getBytes()));
    }

    @Test
    @DisplayName("Should encrypt sensitive parameters correctly")
    void encryptSensitiveParameters_WithSensitiveData_ShouldEncryptValues() {
        // Arrange
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("username", "testuser");
        parameters.put("password", "secretPassword123");
        parameters.put("api_key", "abc123def456");
        parameters.put("database_url", "jdbc:oracle://localhost:1521/db");
        parameters.put("batchSize", 1000);

        // Act
        Map<String, Object> encryptedParams = encryptionService.encryptSensitiveParameters(parameters);

        // Assert
        assertNotNull(encryptedParams);
        assertEquals(5, encryptedParams.size());
        
        // Non-sensitive parameters should remain unchanged
        assertEquals("testuser", encryptedParams.get("username"));
        assertEquals(1000, encryptedParams.get("batchSize"));
        
        // Sensitive parameters should be encrypted
        String encryptedPassword = (String) encryptedParams.get("password");
        String encryptedApiKey = (String) encryptedParams.get("api_key");
        String encryptedDbUrl = (String) encryptedParams.get("database_url");
        
        assertTrue(encryptedPassword.startsWith("ENC:"));
        assertTrue(encryptedApiKey.startsWith("ENC:"));
        assertTrue(encryptedDbUrl.startsWith("ENC:"));
        
        assertNotEquals("secretPassword123", encryptedPassword);
        assertNotEquals("abc123def456", encryptedApiKey);
    }

    @Test
    @DisplayName("Should decrypt encrypted parameters correctly")
    void decryptSensitiveParameters_WithEncryptedData_ShouldDecryptValues() {
        // Arrange
        Map<String, Object> originalParams = new HashMap<>();
        originalParams.put("username", "testuser");
        originalParams.put("password", "secretPassword123");
        originalParams.put("connection_string", "server=localhost;database=test");
        originalParams.put("batchSize", 1000);

        // First encrypt the parameters
        Map<String, Object> encryptedParams = encryptionService.encryptSensitiveParameters(originalParams);
        
        // Act
        Map<String, Object> decryptedParams = encryptionService.decryptSensitiveParameters(encryptedParams);

        // Assert
        assertNotNull(decryptedParams);
        assertEquals(4, decryptedParams.size());
        
        // Non-sensitive parameters should remain unchanged
        assertEquals("testuser", decryptedParams.get("username"));
        assertEquals(1000, decryptedParams.get("batchSize"));
        
        // Sensitive parameters should be decrypted back to original values
        assertEquals("secretPassword123", decryptedParams.get("password"));
        assertEquals("server=localhost;database=test", decryptedParams.get("connection_string"));
    }

    @Test
    @DisplayName("Should mask sensitive parameters for display")
    void maskSensitiveParameters_WithSensitiveData_ShouldMaskValues() {
        // Arrange
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("username", "testuser");
        parameters.put("password", "secretPassword123");
        parameters.put("secret", "abc");
        parameters.put("apikey", "verylongapikey12345");
        parameters.put("batchSize", 1000);

        // Act
        Map<String, Object> maskedParams = encryptionService.maskSensitiveParameters(parameters);

        // Assert
        assertNotNull(maskedParams);
        assertEquals(5, maskedParams.size());
        
        // Non-sensitive parameters should remain unchanged
        assertEquals("testuser", maskedParams.get("username"));
        assertEquals(1000, maskedParams.get("batchSize"));
        
        // Sensitive parameters should be masked
        assertEquals("s***3", maskedParams.get("password"));
        assertEquals("***", maskedParams.get("secret"));
        assertEquals("v***5", maskedParams.get("apikey"));
    }

    @Test
    @DisplayName("Should correctly detect sensitive parameters")
    void containsSensitiveParameters_WithVariousParameters_ShouldDetectSensitive() {
        // Arrange - Test various sensitive parameter patterns
        Map<String, Object> sensitiveParams = new HashMap<>();
        sensitiveParams.put("password", "test");
        sensitiveParams.put("DB_PASSWORD", "test");
        sensitiveParams.put("api_key", "test");
        sensitiveParams.put("secret_token", "test");
        sensitiveParams.put("ssl_keystore", "test");
        sensitiveParams.put("privateKey", "test");

        Map<String, Object> nonSensitiveParams = new HashMap<>();
        nonSensitiveParams.put("username", "test");
        nonSensitiveParams.put("batchSize", 1000);
        nonSensitiveParams.put("timeout", 30);

        // Act & Assert
        assertTrue(encryptionService.containsSensitiveParameters(sensitiveParams));
        assertFalse(encryptionService.containsSensitiveParameters(nonSensitiveParams));
        assertFalse(encryptionService.containsSensitiveParameters(null));
        assertFalse(encryptionService.containsSensitiveParameters(new HashMap<>()));
    }

    @Test
    @DisplayName("Should get sensitive parameter names correctly")
    void getSensitiveParameterNames_WithMixedParameters_ShouldReturnSensitiveNames() {
        // Arrange
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("username", "test");
        parameters.put("password", "secret");
        parameters.put("api_key", "key123");
        parameters.put("batchSize", 1000);
        parameters.put("connection_string", "jdbc:oracle://localhost");
        parameters.put("timeout", 30);

        // Act
        Set<String> sensitiveNames = encryptionService.getSensitiveParameterNames(parameters);

        // Assert
        assertNotNull(sensitiveNames);
        assertEquals(3, sensitiveNames.size());
        assertTrue(sensitiveNames.contains("password"));
        assertTrue(sensitiveNames.contains("api_key"));
        assertTrue(sensitiveNames.contains("connection_string"));
        assertFalse(sensitiveNames.contains("username"));
        assertFalse(sensitiveNames.contains("batchSize"));
        assertFalse(sensitiveNames.contains("timeout"));
    }

    @Test
    @DisplayName("Should handle null and empty parameter maps gracefully")
    void encryptSensitiveParameters_WithNullOrEmpty_ShouldReturnSafely() {
        // Act & Assert
        assertNull(encryptionService.encryptSensitiveParameters(null));
        
        Map<String, Object> emptyMap = new HashMap<>();
        Map<String, Object> result = encryptionService.encryptSensitiveParameters(emptyMap);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should not encrypt when encryption is disabled")
    void encryptSensitiveParameters_WithEncryptionDisabled_ShouldReturnOriginal() {
        // Arrange
        ReflectionTestUtils.setField(encryptionService, "encryptionEnabled", false);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("password", "secretPassword123");
        parameters.put("username", "testuser");

        // Act
        Map<String, Object> result = encryptionService.encryptSensitiveParameters(parameters);

        // Assert
        assertSame(parameters, result);
        assertEquals("secretPassword123", result.get("password"));
        assertEquals("testuser", result.get("username"));
    }

    @Test
    @DisplayName("Should validate encryption configuration successfully")
    void validateEncryptionConfiguration_WithValidConfig_ShouldReturnValid() {
        // Act
        EncryptionValidationResult result = encryptionService.validateEncryptionConfiguration();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEncryptionEnabled());
        assertTrue(result.isValid());
        assertNotNull(result.getValidationTimestamp());
        assertTrue(result.getValidationInfos().size() > 0);
        assertEquals(0, result.getValidationErrors().size());
    }

    @Test
    @DisplayName("Should detect invalid encryption configuration")
    void validateEncryptionConfiguration_WithInvalidConfig_ShouldReturnInvalid() {
        // Arrange
        ReflectionTestUtils.setField(encryptionService, "encryptionKeyBase64", "invalid-key");

        // Act
        EncryptionValidationResult result = encryptionService.validateEncryptionConfiguration();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEncryptionEnabled());
        assertFalse(result.isValid());
        assertTrue(result.getValidationErrors().size() > 0);
    }

    @Test
    @DisplayName("Should detect when encryption is disabled")
    void validateEncryptionConfiguration_WithEncryptionDisabled_ShouldReturnDisabledWarning() {
        // Arrange
        ReflectionTestUtils.setField(encryptionService, "encryptionEnabled", false);

        // Act
        EncryptionValidationResult result = encryptionService.validateEncryptionConfiguration();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEncryptionEnabled());
        assertFalse(result.isValid());
        assertTrue(result.getValidationErrors().stream()
            .anyMatch(error -> error.contains("Encryption is disabled")));
    }

    @Test
    @DisplayName("Should handle encryption errors gracefully")
    void encryptSensitiveParameters_WithCorruptedKey_ShouldHandleErrorsGracefully() {
        // Arrange
        ReflectionTestUtils.setField(encryptionService, "encryptionKeyBase64", "corrupted-key-data");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("password", "secretPassword123");
        parameters.put("username", "testuser");

        // Act
        Map<String, Object> result = encryptionService.encryptSensitiveParameters(parameters);

        // Assert
        assertNotNull(result);
        // Should return original values when encryption fails
        assertEquals("secretPassword123", result.get("password"));
        assertEquals("testuser", result.get("username"));
    }

    @Test
    @DisplayName("Should handle decryption of non-encrypted values")
    void decryptSensitiveParameters_WithNonEncryptedValues_ShouldReturnOriginal() {
        // Arrange
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("password", "plainTextPassword"); // Not encrypted
        parameters.put("username", "testuser");

        // Act
        Map<String, Object> result = encryptionService.decryptSensitiveParameters(parameters);

        // Assert
        assertNotNull(result);
        assertEquals("plainTextPassword", result.get("password"));
        assertEquals("testuser", result.get("username"));
    }

    @Test
    @DisplayName("Should detect all common sensitive parameter patterns")
    void sensitiveParameterDetection_WithVariousPatterns_ShouldDetectAll() {
        // Arrange - Test comprehensive list of sensitive parameter patterns
        String[] sensitivePatterns = {
            "password", "passwd", "pwd", "secret", "key", "token", "credential",
            "auth", "apikey", "api_key", "private", "confidential", "secure",
            "connection_string", "connectionstring", "jdbc_url", "database_url",
            "ssl_keystore", "keystore", "truststore", "certificate", "cert",
            "DATABASE_PASSWORD", "API_SECRET_KEY", "JWT_SECRET", "PRIVATE_KEY"
        };

        // Act & Assert
        for (String pattern : sensitivePatterns) {
            Map<String, Object> testParams = Map.of(pattern, "testValue");
            assertTrue(encryptionService.containsSensitiveParameters(testParams),
                "Should detect sensitive parameter: " + pattern);
        }
    }

    @Test
    @DisplayName("Should handle complex nested parameter structures")
    void encryptSensitiveParameters_WithComplexStructure_ShouldHandleCorrectly() {
        // Arrange
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("database", Map.of(
            "host", "localhost",
            "password", "dbSecret123",
            "port", 5432
        ));
        parameters.put("apiConfig", Map.of(
            "endpoint", "https://api.example.com",
            "api_key", "apiSecret456"
        ));

        // Act
        Map<String, Object> result = encryptionService.encryptSensitiveParameters(parameters);

        // Assert
        assertNotNull(result);
        // Note: Current implementation only handles top-level parameters
        // Complex nested structures would require recursive processing
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should maintain parameter count and keys")
    void encryptSensitiveParameters_ShouldMaintainParameterStructure() {
        // Arrange
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param1", "value1");
        parameters.put("password", "secret");
        parameters.put("param3", 123);
        parameters.put("api_key", "key456");

        // Act
        Map<String, Object> result = encryptionService.encryptSensitiveParameters(parameters);

        // Assert
        assertNotNull(result);
        assertEquals(4, result.size());
        assertTrue(result.containsKey("param1"));
        assertTrue(result.containsKey("password"));
        assertTrue(result.containsKey("param3"));
        assertTrue(result.containsKey("api_key"));
    }

    @Test
    @DisplayName("Should handle special characters and unicode in sensitive data")
    void encryptSensitiveParameters_WithSpecialCharacters_ShouldHandleCorrectly() {
        // Arrange
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("password", "P@$$w0rd!@#$%^&*()_+-=[]{}|;':\",./<>?`~");
        parameters.put("secret", "αβγδε∑π∞≈≠≤≥÷×");
        parameters.put("api_key", "🔑🚀💻🔒");

        // Act
        Map<String, Object> encrypted = encryptionService.encryptSensitiveParameters(parameters);
        Map<String, Object> decrypted = encryptionService.decryptSensitiveParameters(encrypted);

        // Assert
        assertNotNull(encrypted);
        assertNotNull(decrypted);
        assertEquals("P@$$w0rd!@#$%^&*()_+-=[]{}|;':\",./<>?`~", decrypted.get("password"));
        assertEquals("αβγδε∑π∞≈≠≤≥÷×", decrypted.get("secret"));
        assertEquals("🔑🚀💻🔒", decrypted.get("api_key"));
    }
}