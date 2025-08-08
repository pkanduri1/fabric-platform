package com.truist.batch.idempotency.service;

import com.truist.batch.idempotency.entity.FabricIdempotencyConfigEntity;
import com.truist.batch.idempotency.entity.FabricIdempotencyConfigEntity.ConfigType;
import com.truist.batch.idempotency.repository.FabricIdempotencyConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing idempotency configuration settings.
 * Provides configuration lookup, caching, and management capabilities
 * for flexible idempotency behavior across different job types and endpoints.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class IdempotencyConfigService {
    
    private final FabricIdempotencyConfigRepository configRepository;
    
    // Default configuration values
    private static final int DEFAULT_TTL_HOURS = 24;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final String DEFAULT_KEY_STRATEGY = "AUTO_GENERATED";
    private static final boolean DEFAULT_STORE_PAYLOAD = true;
    private static final boolean DEFAULT_ENCRYPTION_REQUIRED = false;
    
    public IdempotencyConfigService(FabricIdempotencyConfigRepository configRepository) {
        this.configRepository = configRepository;
    }
    
    /**
     * Gets the best matching configuration for a batch job.
     */
    @Cacheable(value = "idempotencyConfig", key = "#jobName")
    public FabricIdempotencyConfigEntity getBatchJobConfig(String jobName) {
        return getConfigForTarget(ConfigType.BATCH_JOB, jobName);
    }
    
    /**
     * Gets the best matching configuration for an API endpoint.
     */
    @Cacheable(value = "idempotencyConfig", key = "#endpoint")
    public FabricIdempotencyConfigEntity getApiEndpointConfig(String endpoint) {
        return getConfigForTarget(ConfigType.API_ENDPOINT, endpoint);
    }
    
    /**
     * Gets the best matching configuration for a specific target and type.
     */
    public FabricIdempotencyConfigEntity getConfigForTarget(ConfigType configType, String target) {
        log.debug("Looking for {} configuration matching target: {}", configType, target);
        
        Optional<FabricIdempotencyConfigEntity> config = configRepository
                .findBestMatchingConfiguration(configType, target);
        
        if (config.isPresent()) {
            log.debug("Found configuration: {} for target: {}", 
                    config.get().getConfigId(), target);
            return config.get();
        }
        
        // Fallback to default configuration
        log.debug("No specific configuration found for target: {}, using default", target);
        return getDefaultConfig(configType);
    }
    
    /**
     * Gets default configuration for the specified type.
     */
    private FabricIdempotencyConfigEntity getDefaultConfig(ConfigType configType) {
        String defaultConfigId = configType == ConfigType.BATCH_JOB ? 
                "DEFAULT_BATCH_JOB" : "DEFAULT_API_ENDPOINT";
        
        Optional<FabricIdempotencyConfigEntity> defaultConfig = 
                configRepository.findById(defaultConfigId);
        
        if (defaultConfig.isPresent()) {
            return defaultConfig.get();
        }
        
        // Ultimate fallback - create in-memory default
        log.warn("No default configuration found for {}, creating fallback", configType);
        return createFallbackConfig(configType);
    }
    
    /**
     * Creates a fallback configuration when none exists in database.
     */
    private FabricIdempotencyConfigEntity createFallbackConfig(ConfigType configType) {
        return FabricIdempotencyConfigEntity.builder()
                .configId("FALLBACK_" + configType.name())
                .configType(configType)
                .targetPattern("*")
                .enabled("Y")
                .ttlHours(DEFAULT_TTL_HOURS)
                .maxRetries(DEFAULT_MAX_RETRIES)
                .keyGenerationStrategy(DEFAULT_KEY_STRATEGY)
                .storeRequestPayload(DEFAULT_STORE_PAYLOAD ? "Y" : "N")
                .storeResponsePayload(DEFAULT_STORE_PAYLOAD ? "Y" : "N")
                .cleanupPolicy("TTL_BASED")
                .encryptionRequired(DEFAULT_ENCRYPTION_REQUIRED ? "Y" : "N")
                .description("Fallback configuration created at runtime")
                .createdBy("SYSTEM")
                .build();
    }
    
    /**
     * Checks if idempotency is enabled for a specific target.
     */
    public boolean isIdempotencyEnabled(ConfigType configType, String target) {
        FabricIdempotencyConfigEntity config = getConfigForTarget(configType, target);
        return config.isEnabled();
    }
    
    /**
     * Gets TTL in seconds for a specific target.
     */
    public int getTtlSeconds(ConfigType configType, String target) {
        FabricIdempotencyConfigEntity config = getConfigForTarget(configType, target);
        return config.getTtlSeconds();
    }
    
    /**
     * Gets max retries for a specific target.
     */
    public int getMaxRetries(ConfigType configType, String target) {
        FabricIdempotencyConfigEntity config = getConfigForTarget(configType, target);
        return config.getMaxRetries() != null ? config.getMaxRetries() : DEFAULT_MAX_RETRIES;
    }
    
    /**
     * Checks if request payload should be stored.
     */
    public boolean shouldStoreRequestPayload(ConfigType configType, String target) {
        FabricIdempotencyConfigEntity config = getConfigForTarget(configType, target);
        return config.shouldStoreRequestPayload();
    }
    
    /**
     * Checks if response payload should be stored.
     */
    public boolean shouldStoreResponsePayload(ConfigType configType, String target) {
        FabricIdempotencyConfigEntity config = getConfigForTarget(configType, target);
        return config.shouldStoreResponsePayload();
    }
    
    /**
     * Checks if encryption is required for payloads.
     */
    public boolean isEncryptionRequired(ConfigType configType, String target) {
        FabricIdempotencyConfigEntity config = getConfigForTarget(configType, target);
        return config.isEncryptionRequired();
    }
    
    /**
     * Gets key generation strategy for a specific target.
     */
    public String getKeyGenerationStrategy(ConfigType configType, String target) {
        FabricIdempotencyConfigEntity config = getConfigForTarget(configType, target);
        return config.getKeyGenerationStrategy() != null ? 
                config.getKeyGenerationStrategy() : DEFAULT_KEY_STRATEGY;
    }
    
    /**
     * Creates or updates a configuration.
     */
    @Transactional
    public FabricIdempotencyConfigEntity saveConfig(FabricIdempotencyConfigEntity config) {
        log.info("Saving idempotency configuration: {}", config.getSummary());
        return configRepository.save(config);
    }
    
    /**
     * Enables or disables a configuration.
     */
    @Transactional
    public void setConfigEnabled(String configId, boolean enabled) {
        Optional<FabricIdempotencyConfigEntity> config = configRepository.findById(configId);
        if (config.isPresent()) {
            FabricIdempotencyConfigEntity entity = config.get();
            entity.setEnabled(enabled ? "Y" : "N");
            configRepository.save(entity);
            log.info("Configuration {} {} enabled", configId, enabled ? "is now" : "is no longer");
        } else {
            log.warn("Configuration {} not found for enable/disable operation", configId);
        }
    }
    
    /**
     * Validates that a configuration is properly set up.
     */
    public boolean validateConfig(FabricIdempotencyConfigEntity config) {
        if (config == null) return false;
        
        // Check required fields
        if (config.getConfigId() == null || config.getConfigId().trim().isEmpty()) {
            log.error("Configuration missing config ID");
            return false;
        }
        
        if (config.getConfigType() == null) {
            log.error("Configuration {} missing config type", config.getConfigId());
            return false;
        }
        
        if (config.getTargetPattern() == null || config.getTargetPattern().trim().isEmpty()) {
            log.error("Configuration {} missing target pattern", config.getConfigId());
            return false;
        }
        
        // Check value ranges
        if (config.getTtlHours() != null && config.getTtlHours() <= 0) {
            log.error("Configuration {} has invalid TTL hours: {}", 
                    config.getConfigId(), config.getTtlHours());
            return false;
        }
        
        if (config.getMaxRetries() != null && config.getMaxRetries() < 0) {
            log.error("Configuration {} has invalid max retries: {}", 
                    config.getConfigId(), config.getMaxRetries());
            return false;
        }
        
        log.debug("Configuration {} validation passed", config.getConfigId());
        return true;
    }
    
    /**
     * Gets configuration summary for monitoring.
     */
    public ConfigurationSummary getConfigurationSummary() {
        long totalConfigs = configRepository.count();
        long enabledConfigs = configRepository.countByEnabled("Y");
        long batchConfigs = configRepository.countByConfigType(ConfigType.BATCH_JOB);
        long apiConfigs = configRepository.countByConfigType(ConfigType.API_ENDPOINT);
        
        return ConfigurationSummary.builder()
                .totalConfigurations(totalConfigs)
                .enabledConfigurations(enabledConfigs)
                .batchJobConfigurations(batchConfigs)
                .apiEndpointConfigurations(apiConfigs)
                .disabledConfigurations(totalConfigs - enabledConfigs)
                .build();
    }
    
    /**
     * Configuration summary data class.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ConfigurationSummary {
        private long totalConfigurations;
        private long enabledConfigurations;
        private long disabledConfigurations;
        private long batchJobConfigurations;
        private long apiEndpointConfigurations;
        
        public double getEnabledPercentage() {
            return totalConfigurations > 0 ? 
                    (double) enabledConfigurations / totalConfigurations * 100.0 : 0.0;
        }
        
        public String getSummary() {
            return String.format("ConfigSummary[total=%d, enabled=%d (%.1f%%), batch=%d, api=%d]",
                    totalConfigurations, enabledConfigurations, getEnabledPercentage(),
                    batchJobConfigurations, apiEndpointConfigurations);
        }
    }
}