package com.truist.batch.idempotency.service;

import com.truist.batch.idempotency.entity.FabricIdempotencyConfigEntity;
import com.truist.batch.idempotency.entity.FabricIdempotencyConfigEntity.ConfigType;
import com.truist.batch.idempotency.repository.FabricIdempotencyConfigRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IdempotencyConfigService.
 * Tests configuration lookup, caching, and management capabilities.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("IdempotencyConfigService Tests")
class IdempotencyConfigServiceTest {
    
    @Mock private FabricIdempotencyConfigRepository configRepository;
    
    @InjectMocks private IdempotencyConfigService configService;
    
    private FabricIdempotencyConfigEntity testBatchConfig;
    private FabricIdempotencyConfigEntity testApiConfig;
    
    @BeforeEach
    void setUp() {
        testBatchConfig = createTestBatchConfig();
        testApiConfig = createTestApiConfig();
    }
    
    // ============================================================================
    // Configuration Lookup Tests
    // ============================================================================
    
    @Test
    @Order(1)
    @DisplayName("Should find best matching batch job configuration")
    void shouldFindBestMatchingBatchJobConfiguration() {
        // Given
        String jobName = "TEST_JOB";
        when(configRepository.findBestMatchingConfiguration(ConfigType.BATCH_JOB, jobName))
                .thenReturn(Optional.of(testBatchConfig));
        
        // When
        FabricIdempotencyConfigEntity result = configService.getBatchJobConfig(jobName);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConfigType()).isEqualTo(ConfigType.BATCH_JOB);
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getTtlHours()).isEqualTo(24);
        
        verify(configRepository).findBestMatchingConfiguration(ConfigType.BATCH_JOB, jobName);
    }
    
    @Test
    @Order(2)
    @DisplayName("Should find best matching API endpoint configuration")
    void shouldFindBestMatchingApiEndpointConfiguration() {
        // Given
        String endpoint = "/api/test";
        when(configRepository.findBestMatchingConfiguration(ConfigType.API_ENDPOINT, endpoint))
                .thenReturn(Optional.of(testApiConfig));
        
        // When
        FabricIdempotencyConfigEntity result = configService.getApiEndpointConfig(endpoint);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConfigType()).isEqualTo(ConfigType.API_ENDPOINT);
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getTtlHours()).isEqualTo(1);
        
        verify(configRepository).findBestMatchingConfiguration(ConfigType.API_ENDPOINT, endpoint);
    }
    
    @Test
    @Order(3)
    @DisplayName("Should return default configuration when no specific match found")
    void shouldReturnDefaultConfiguration_WhenNoSpecificMatchFound() {
        // Given
        String jobName = "UNKNOWN_JOB";
        when(configRepository.findBestMatchingConfiguration(ConfigType.BATCH_JOB, jobName))
                .thenReturn(Optional.empty());
        when(configRepository.findById("DEFAULT_BATCH_JOB"))
                .thenReturn(Optional.of(createDefaultBatchConfig()));
        
        // When
        FabricIdempotencyConfigEntity result = configService.getBatchJobConfig(jobName);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConfigId()).isEqualTo("DEFAULT_BATCH_JOB");
        assertThat(result.getTargetPattern()).isEqualTo("*");
    }
    
    @Test
    @Order(4)
    @DisplayName("Should create fallback configuration when no default exists")
    void shouldCreateFallbackConfiguration_WhenNoDefaultExists() {
        // Given
        String jobName = "UNKNOWN_JOB";
        when(configRepository.findBestMatchingConfiguration(ConfigType.BATCH_JOB, jobName))
                .thenReturn(Optional.empty());
        when(configRepository.findById("DEFAULT_BATCH_JOB"))
                .thenReturn(Optional.empty());
        
        // When
        FabricIdempotencyConfigEntity result = configService.getBatchJobConfig(jobName);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConfigId()).isEqualTo("FALLBACK_BATCH_JOB");
        assertThat(result.getTargetPattern()).isEqualTo("*");
        assertThat(result.getTtlHours()).isEqualTo(24);
        assertThat(result.getMaxRetries()).isEqualTo(3);
        assertThat(result.isEnabled()).isTrue();
    }
    
    // ============================================================================
    // Configuration Property Tests
    // ============================================================================
    
    @Test
    @Order(5)
    @DisplayName("Should check if idempotency is enabled")
    void shouldCheckIfIdempotencyIsEnabled() {
        // Given
        String jobName = "TEST_JOB";
        when(configRepository.findBestMatchingConfiguration(ConfigType.BATCH_JOB, jobName))
                .thenReturn(Optional.of(testBatchConfig));
        
        // When
        boolean result = configService.isIdempotencyEnabled(ConfigType.BATCH_JOB, jobName);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @Order(6)
    @DisplayName("Should return TTL in seconds")
    void shouldReturnTtlInSeconds() {
        // Given
        String jobName = "TEST_JOB";
        when(configRepository.findBestMatchingConfiguration(ConfigType.BATCH_JOB, jobName))
                .thenReturn(Optional.of(testBatchConfig));
        
        // When
        int result = configService.getTtlSeconds(ConfigType.BATCH_JOB, jobName);
        
        // Then
        assertThat(result).isEqualTo(86400); // 24 hours * 3600 seconds
    }
    
    @Test
    @Order(7)
    @DisplayName("Should return max retries")
    void shouldReturnMaxRetries() {
        // Given
        String jobName = "TEST_JOB";
        when(configRepository.findBestMatchingConfiguration(ConfigType.BATCH_JOB, jobName))
                .thenReturn(Optional.of(testBatchConfig));
        
        // When
        int result = configService.getMaxRetries(ConfigType.BATCH_JOB, jobName);
        
        // Then
        assertThat(result).isEqualTo(3);
    }
    
    @Test
    @Order(8)
    @DisplayName("Should check payload storage settings")
    void shouldCheckPayloadStorageSettings() {
        // Given
        String jobName = "TEST_JOB";
        when(configRepository.findBestMatchingConfiguration(ConfigType.BATCH_JOB, jobName))
                .thenReturn(Optional.of(testBatchConfig));
        
        // When
        boolean storeRequest = configService.shouldStoreRequestPayload(ConfigType.BATCH_JOB, jobName);
        boolean storeResponse = configService.shouldStoreResponsePayload(ConfigType.BATCH_JOB, jobName);
        
        // Then
        assertThat(storeRequest).isTrue();
        assertThat(storeResponse).isTrue();
    }
    
    @Test
    @Order(9)
    @DisplayName("Should check encryption requirement")
    void shouldCheckEncryptionRequirement() {
        // Given
        String jobName = "TEST_JOB";
        FabricIdempotencyConfigEntity encryptedConfig = createTestBatchConfig();
        encryptedConfig.setEncryptionRequired("Y");
        
        when(configRepository.findBestMatchingConfiguration(ConfigType.BATCH_JOB, jobName))
                .thenReturn(Optional.of(encryptedConfig));
        
        // When
        boolean result = configService.isEncryptionRequired(ConfigType.BATCH_JOB, jobName);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @Order(10)
    @DisplayName("Should return key generation strategy")
    void shouldReturnKeyGenerationStrategy() {
        // Given
        String jobName = "TEST_JOB";
        when(configRepository.findBestMatchingConfiguration(ConfigType.BATCH_JOB, jobName))
                .thenReturn(Optional.of(testBatchConfig));
        
        // When
        String result = configService.getKeyGenerationStrategy(ConfigType.BATCH_JOB, jobName);
        
        // Then
        assertThat(result).isEqualTo("AUTO_GENERATED");
    }
    
    // ============================================================================
    // Configuration Management Tests
    // ============================================================================
    
    @Test
    @Order(11)
    @DisplayName("Should save configuration")
    void shouldSaveConfiguration() {
        // Given
        FabricIdempotencyConfigEntity newConfig = createTestBatchConfig();
        when(configRepository.save(newConfig)).thenReturn(newConfig);
        
        // When
        FabricIdempotencyConfigEntity result = configService.saveConfig(newConfig);
        
        // Then
        assertThat(result).isEqualTo(newConfig);
        verify(configRepository).save(newConfig);
    }
    
    @Test
    @Order(12)
    @DisplayName("Should enable configuration")
    void shouldEnableConfiguration() {
        // Given
        String configId = "TEST_CONFIG";
        when(configRepository.findById(configId)).thenReturn(Optional.of(testBatchConfig));
        when(configRepository.save(any(FabricIdempotencyConfigEntity.class)))
                .thenReturn(testBatchConfig);
        
        // When
        configService.setConfigEnabled(configId, true);
        
        // Then
        verify(configRepository).findById(configId);
        verify(configRepository).save(argThat(config -> "Y".equals(config.getEnabled())));
    }
    
    @Test
    @Order(13)
    @DisplayName("Should disable configuration")
    void shouldDisableConfiguration() {
        // Given
        String configId = "TEST_CONFIG";
        when(configRepository.findById(configId)).thenReturn(Optional.of(testBatchConfig));
        when(configRepository.save(any(FabricIdempotencyConfigEntity.class)))
                .thenReturn(testBatchConfig);
        
        // When
        configService.setConfigEnabled(configId, false);
        
        // Then
        verify(configRepository).findById(configId);
        verify(configRepository).save(argThat(config -> "N".equals(config.getEnabled())));
    }
    
    @Test
    @Order(14)
    @DisplayName("Should handle non-existent configuration gracefully when enabling/disabling")
    void shouldHandleNonExistentConfiguration_WhenEnablingDisabling() {
        // Given
        String configId = "NON_EXISTENT";
        when(configRepository.findById(configId)).thenReturn(Optional.empty());
        
        // When & Then - Should not throw exception
        assertThatCode(() -> configService.setConfigEnabled(configId, true))
                .doesNotThrowAnyException();
        
        verify(configRepository).findById(configId);
        verify(configRepository, never()).save(any());
    }
    
    // ============================================================================
    // Configuration Validation Tests
    // ============================================================================
    
    @Test
    @Order(15)
    @DisplayName("Should validate valid configuration")
    void shouldValidateValidConfiguration() {
        // Given
        FabricIdempotencyConfigEntity validConfig = createTestBatchConfig();
        
        // When
        boolean result = configService.validateConfig(validConfig);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @Order(16)
    @DisplayName("Should reject configuration with missing config ID")
    void shouldRejectConfiguration_WithMissingConfigId() {
        // Given
        FabricIdempotencyConfigEntity invalidConfig = createTestBatchConfig();
        invalidConfig.setConfigId(null);
        
        // When
        boolean result = configService.validateConfig(invalidConfig);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @Order(17)
    @DisplayName("Should reject configuration with missing config type")
    void shouldRejectConfiguration_WithMissingConfigType() {
        // Given
        FabricIdempotencyConfigEntity invalidConfig = createTestBatchConfig();
        invalidConfig.setConfigType(null);
        
        // When
        boolean result = configService.validateConfig(invalidConfig);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @Order(18)
    @DisplayName("Should reject configuration with invalid TTL")
    void shouldRejectConfiguration_WithInvalidTtl() {
        // Given
        FabricIdempotencyConfigEntity invalidConfig = createTestBatchConfig();
        invalidConfig.setTtlHours(-1);
        
        // When
        boolean result = configService.validateConfig(invalidConfig);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @Order(19)
    @DisplayName("Should reject configuration with invalid max retries")
    void shouldRejectConfiguration_WithInvalidMaxRetries() {
        // Given
        FabricIdempotencyConfigEntity invalidConfig = createTestBatchConfig();
        invalidConfig.setMaxRetries(-1);
        
        // When
        boolean result = configService.validateConfig(invalidConfig);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @Order(20)
    @DisplayName("Should reject null configuration")
    void shouldRejectNullConfiguration() {
        // When
        boolean result = configService.validateConfig(null);
        
        // Then
        assertThat(result).isFalse();
    }
    
    // ============================================================================
    // Configuration Summary Tests
    // ============================================================================
    
    @Test
    @Order(21)
    @DisplayName("Should generate configuration summary")
    void shouldGenerateConfigurationSummary() {
        // Given
        when(configRepository.count()).thenReturn(10L);
        when(configRepository.countByEnabled("Y")).thenReturn(8L);
        when(configRepository.countByConfigType(ConfigType.BATCH_JOB)).thenReturn(6L);
        when(configRepository.countByConfigType(ConfigType.API_ENDPOINT)).thenReturn(4L);
        
        // When
        IdempotencyConfigService.ConfigurationSummary summary = configService.getConfigurationSummary();
        
        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalConfigurations()).isEqualTo(10L);
        assertThat(summary.getEnabledConfigurations()).isEqualTo(8L);
        assertThat(summary.getDisabledConfigurations()).isEqualTo(2L);
        assertThat(summary.getBatchJobConfigurations()).isEqualTo(6L);
        assertThat(summary.getApiEndpointConfigurations()).isEqualTo(4L);
        assertThat(summary.getEnabledPercentage()).isEqualTo(80.0);
    }
    
    // ============================================================================
    // Default Configuration Handling Tests
    // ============================================================================
    
    @Test
    @Order(22)
    @DisplayName("Should handle default values for null properties")
    void shouldHandleDefaultValues_ForNullProperties() {
        // Given
        String jobName = "TEST_JOB";
        FabricIdempotencyConfigEntity configWithNulls = createTestBatchConfig();
        configWithNulls.setMaxRetries(null);
        configWithNulls.setKeyGenerationStrategy(null);
        
        when(configRepository.findBestMatchingConfiguration(ConfigType.BATCH_JOB, jobName))
                .thenReturn(Optional.of(configWithNulls));
        
        // When
        int maxRetries = configService.getMaxRetries(ConfigType.BATCH_JOB, jobName);
        String keyStrategy = configService.getKeyGenerationStrategy(ConfigType.BATCH_JOB, jobName);
        
        // Then
        assertThat(maxRetries).isEqualTo(3); // Default value
        assertThat(keyStrategy).isEqualTo("AUTO_GENERATED"); // Default value
    }
    
    // ============================================================================
    // Helper Methods
    // ============================================================================
    
    private FabricIdempotencyConfigEntity createTestBatchConfig() {
        return FabricIdempotencyConfigEntity.builder()
                .configId("TEST_BATCH_CONFIG")
                .configType(ConfigType.BATCH_JOB)
                .targetPattern("TEST_*")
                .enabled("Y")
                .ttlHours(24)
                .maxRetries(3)
                .keyGenerationStrategy("AUTO_GENERATED")
                .storeRequestPayload("Y")
                .storeResponsePayload("Y")
                .cleanupPolicy("TTL_BASED")
                .encryptionRequired("N")
                .description("Test batch job configuration")
                .createdBy("test_user")
                .createdDate(LocalDateTime.now())
                .build();
    }
    
    private FabricIdempotencyConfigEntity createTestApiConfig() {
        return FabricIdempotencyConfigEntity.builder()
                .configId("TEST_API_CONFIG")
                .configType(ConfigType.API_ENDPOINT)
                .targetPattern("/api/test/*")
                .enabled("Y")
                .ttlHours(1)
                .maxRetries(2)
                .keyGenerationStrategy("AUTO_GENERATED")
                .storeRequestPayload("Y")
                .storeResponsePayload("Y")
                .cleanupPolicy("TTL_BASED")
                .encryptionRequired("N")
                .description("Test API endpoint configuration")
                .createdBy("test_user")
                .createdDate(LocalDateTime.now())
                .build();
    }
    
    private FabricIdempotencyConfigEntity createDefaultBatchConfig() {
        return FabricIdempotencyConfigEntity.builder()
                .configId("DEFAULT_BATCH_JOB")
                .configType(ConfigType.BATCH_JOB)
                .targetPattern("*")
                .enabled("Y")
                .ttlHours(24)
                .maxRetries(3)
                .keyGenerationStrategy("AUTO_GENERATED")
                .storeRequestPayload("Y")
                .storeResponsePayload("Y")
                .cleanupPolicy("TTL_BASED")
                .encryptionRequired("N")
                .description("Default batch job configuration")
                .createdBy("SYSTEM")
                .createdDate(LocalDateTime.now())
                .build();
    }
}