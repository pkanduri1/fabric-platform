package com.truist.batch.idempotency.service;

import com.truist.batch.idempotency.model.IdempotencyRequest;
import com.truist.batch.idempotency.model.RequestContext;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for IdempotencyKeyGenerator.
 * Tests key generation strategies, uniqueness, and validation.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("IdempotencyKeyGenerator Tests")
class IdempotencyKeyGeneratorTest {
    
    private IdempotencyKeyGenerator keyGenerator;
    
    @BeforeEach
    void setUp() {
        keyGenerator = new IdempotencyKeyGenerator();
    }
    
    // ============================================================================
    // Key Generation Tests
    // ============================================================================
    
    @Test
    @Order(1)
    @DisplayName("Should generate consistent keys for identical requests")
    void shouldGenerateConsistentKeys_ForIdenticalRequests() {
        // Given
        IdempotencyRequest request = createTestRequest();
        
        // When
        String key1 = keyGenerator.generateKey(request);
        String key2 = keyGenerator.generateKey(request);
        
        // Then
        assertThat(key1).isEqualTo(key2);
        assertThat(key1).contains("TEST_SYSTEM");
        assertThat(key1).contains("TEST_JOB");
    }
    
    @Test
    @Order(2)
    @DisplayName("Should generate different keys for different source systems")
    void shouldGenerateDifferentKeys_ForDifferentSourceSystems() {
        // Given
        IdempotencyRequest request1 = createTestRequest();
        IdempotencyRequest request2 = createTestRequest();
        request2.setSourceSystem("DIFFERENT_SYSTEM");
        
        // When
        String key1 = keyGenerator.generateKey(request1);
        String key2 = keyGenerator.generateKey(request2);
        
        // Then
        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1).contains("TEST_SYSTEM");
        assertThat(key2).contains("DIFFERENT_SYSTEM");
    }
    
    @Test
    @Order(3)
    @DisplayName("Should generate different keys for different job names")
    void shouldGenerateDifferentKeys_ForDifferentJobNames() {
        // Given
        IdempotencyRequest request1 = createTestRequest();
        IdempotencyRequest request2 = createTestRequest();
        request2.setJobName("DIFFERENT_JOB");
        
        // When
        String key1 = keyGenerator.generateKey(request1);
        String key2 = keyGenerator.generateKey(request2);
        
        // Then
        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1).contains("TEST_JOB");
        assertThat(key2).contains("DIFFERENT_JOB");
    }
    
    @Test
    @Order(4)
    @DisplayName("Should use client-provided key when available")
    void shouldUseClientProvidedKey_WhenAvailable() {
        // Given
        String clientKey = "CLIENT_PROVIDED_KEY_123";
        IdempotencyRequest request = createTestRequest();
        request.setClientProvidedKey(clientKey);
        
        // When
        String result = keyGenerator.generateKey(request);
        
        // Then
        assertThat(result).isEqualTo("CLIENT_PROVIDED_KEY_123");
    }
    
    @Test
    @Order(5)
    @DisplayName("Should sanitize client-provided keys")
    void shouldSanitizeClientProvidedKeys() {
        // Given
        String dirtyKey = "client@key#with$special%chars!";
        IdempotencyRequest request = createTestRequest();
        request.setClientProvidedKey(dirtyKey);
        
        // When
        String result = keyGenerator.generateKey(request);
        
        // Then
        assertThat(result).doesNotContain("@", "#", "$", "%", "!");
        assertThat(result).matches("[A-Z0-9_:-]+");
    }
    
    @Test
    @Order(6)
    @DisplayName("Should include file hash in key when available")
    void shouldIncludeFileHash_InKeyWhenAvailable() {
        // Given
        IdempotencyRequest request1 = createTestRequest();
        request1.setFileHash("ABC123DEF456");
        
        IdempotencyRequest request2 = createTestRequest();
        request2.setFileHash("XYZ789UVW012");
        
        // When
        String key1 = keyGenerator.generateKey(request1);
        String key2 = keyGenerator.generateKey(request2);
        
        // Then
        assertThat(key1).isNotEqualTo(key2);
        // Keys should be different even with same other parameters due to different file hashes
    }
    
    @Test
    @Order(7)
    @DisplayName("Should include job parameters in key generation")
    void shouldIncludeJobParameters_InKeyGeneration() {
        // Given
        IdempotencyRequest request1 = createTestRequest();
        Map<String, Object> params1 = new HashMap<>();
        params1.put("param1", "value1");
        request1.setJobParameters(params1);
        
        IdempotencyRequest request2 = createTestRequest();
        Map<String, Object> params2 = new HashMap<>();
        params2.put("param1", "value2");
        request2.setJobParameters(params2);
        
        // When
        String key1 = keyGenerator.generateKey(request1);
        String key2 = keyGenerator.generateKey(request2);
        
        // Then
        assertThat(key1).isNotEqualTo(key2);
    }
    
    // ============================================================================
    // Correlation ID Tests
    // ============================================================================
    
    @Test
    @Order(8)
    @DisplayName("Should generate unique correlation IDs")
    void shouldGenerateUniqueCorrelationIds() {
        // When
        String corr1 = keyGenerator.generateCorrelationId();
        String corr2 = keyGenerator.generateCorrelationId();
        
        // Then
        assertThat(corr1).isNotEqualTo(corr2);
        assertThat(corr1).startsWith("IDEM_");
        assertThat(corr2).startsWith("IDEM_");
        assertThat(corr1).matches("IDEM_\\d{8}_\\d{6}_[A-Z0-9]{8}");
    }
    
    // ============================================================================
    // Key Validation Tests
    // ============================================================================
    
    @Test
    @Order(9)
    @DisplayName("Should validate well-formed keys")
    void shouldValidateWellFormedKeys() {
        // Given
        String validKey = "TEST_SYSTEM:TEST_JOB:20250807:ABCD1234";
        String invalidKey1 = null;
        String invalidKey2 = "";
        String invalidKey3 = "key@with#special$chars";
        String tooLongKey = "A".repeat(130);
        
        // When & Then
        assertThat(keyGenerator.isValidKey(validKey)).isTrue();
        assertThat(keyGenerator.isValidKey(invalidKey1)).isFalse();
        assertThat(keyGenerator.isValidKey(invalidKey2)).isFalse();
        assertThat(keyGenerator.isValidKey(invalidKey3)).isFalse();
        assertThat(keyGenerator.isValidKey(tooLongKey)).isFalse();
    }
    
    // ============================================================================
    // Key Component Extraction Tests
    // ============================================================================
    
    @Test
    @Order(10)
    @DisplayName("Should extract components from well-formed keys")
    void shouldExtractComponents_FromWellFormedKeys() {
        // Given
        String key = "TEST_SYSTEM:TEST_JOB:20250807:ABCD1234";
        
        // When
        IdempotencyKeyGenerator.KeyComponents components = keyGenerator.extractComponents(key);
        
        // Then
        assertThat(components).isNotNull();
        assertThat(components.getSourceSystem()).isEqualTo("TEST_SYSTEM");
        assertThat(components.getJobName()).isEqualTo("TEST_JOB");
        assertThat(components.getDateComponent()).isEqualTo("20250807");
        assertThat(components.getContentHash()).isEqualTo("ABCD1234");
        assertThat(components.getRawKey()).isEqualTo(key);
    }
    
    @Test
    @Order(11)
    @DisplayName("Should handle malformed keys gracefully")
    void shouldHandleMalformedKeys_Gracefully() {
        // Given
        String malformedKey = "MALFORMED_KEY";
        
        // When
        IdempotencyKeyGenerator.KeyComponents components = keyGenerator.extractComponents(malformedKey);
        
        // Then
        assertThat(components).isNotNull();
        assertThat(components.getRawKey()).isEqualTo(malformedKey);
        assertThat(components.getSourceSystem()).isNull();
    }
    
    // ============================================================================
    // Edge Cases and Error Handling
    // ============================================================================
    
    @Test
    @Order(12)
    @DisplayName("Should handle null and empty inputs gracefully")
    void shouldHandleNullAndEmptyInputs_Gracefully() {
        // Given
        IdempotencyRequest minimalRequest = IdempotencyRequest.builder()
                .sourceSystem("TEST")
                .jobName("JOB")
                .build();
        
        // When
        String key = keyGenerator.generateKey(minimalRequest);
        
        // Then
        assertThat(key).isNotNull();
        assertThat(key).contains("TEST");
        assertThat(key).contains("JOB");
    }
    
    @Test
    @Order(13)
    @DisplayName("Should handle special characters in input")
    void shouldHandleSpecialCharacters_InInput() {
        // Given
        IdempotencyRequest request = IdempotencyRequest.builder()
                .sourceSystem("test@system")
                .jobName("job#with$special%chars")
                .transactionId("transaction!with*chars")
                .build();
        
        // When
        String key = keyGenerator.generateKey(request);
        
        // Then
        assertThat(key).isNotNull();
        assertThat(key).matches("[A-Z0-9_:-]+"); // Only allowed characters
        assertThat(key).contains("TEST_SYSTEM"); // Sanitized source system
        assertThat(key).contains("JOB_WITH_SPECIAL_CHARS"); // Sanitized job name
    }
    
    @Test
    @Order(14)
    @DisplayName("Should generate fallback hash for empty content")
    void shouldGenerateFallbackHash_ForEmptyContent() {
        // Given
        IdempotencyRequest emptyRequest = IdempotencyRequest.builder()
                .sourceSystem("TEST")
                .jobName("JOB")
                // No transaction ID, file hash, or other content
                .build();
        
        // When
        String key1 = keyGenerator.generateKey(emptyRequest);
        String key2 = keyGenerator.generateKey(emptyRequest);
        
        // Then
        assertThat(key1).isNotNull();
        assertThat(key2).isNotNull();
        // Keys should be different due to timestamp/random components in fallback
        assertThat(key1).isNotEqualTo(key2);
    }
    
    @Test
    @Order(15)
    @DisplayName("Should truncate overly long keys")
    void shouldTruncateOverlyLongKeys() {
        // Given
        String veryLongKey = "A".repeat(200);
        IdempotencyRequest request = createTestRequest();
        request.setClientProvidedKey(veryLongKey);
        
        // When
        String result = keyGenerator.generateKey(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.length()).isLessThanOrEqualTo(128);
    }
    
    // ============================================================================
    // Performance Tests
    // ============================================================================
    
    @Test
    @Order(16)
    @DisplayName("Should generate keys efficiently")
    void shouldGenerateKeys_Efficiently() {
        // Given
        IdempotencyRequest request = createTestRequest();
        
        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            keyGenerator.generateKey(request);
        }
        long endTime = System.currentTimeMillis();
        
        // Then
        long totalTime = endTime - startTime;
        assertThat(totalTime).isLessThan(1000); // Should generate 1000 keys in less than 1 second
    }
    
    // ============================================================================
    // Helper Methods
    // ============================================================================
    
    private IdempotencyRequest createTestRequest() {
        return IdempotencyRequest.builder()
                .sourceSystem("TEST_SYSTEM")
                .jobName("TEST_JOB")
                .transactionId("test_transaction_123")
                .filePath("/test/data/file.csv")
                .requestContext(RequestContext.builder()
                        .userId("test_user")
                        .clientIp("127.0.0.1")
                        .build())
                .build();
    }
}