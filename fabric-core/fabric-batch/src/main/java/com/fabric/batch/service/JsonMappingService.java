package com.fabric.batch.service;

import com.fabric.batch.adapter.JsonMappingAdapter;
import com.fabric.batch.entity.BatchConfigurationEntity;
import com.fabric.batch.model.FieldMapping;
import com.fabric.batch.model.YamlMapping;
import com.fabric.batch.repository.BatchConfigurationRepository;
import com.fabric.batch.mapping.YamlMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing JSON-based field mappings stored in BATCH_CONFIGURATIONS table.
 * Provides caching and transformation capabilities using existing YamlMappingService logic.
 *
 * ARCHITECTURAL NOTE:
 * This service reads from BATCH_CONFIGURATIONS table (batch processing layer)
 * instead of MANUAL_JOB_CONFIG table (API/UI layer) to ensure proper module separation.
 *
 * @author Claude Code
 * @since Phase 2 - Batch Module Separation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JsonMappingService {

    private final BatchConfigurationRepository batchConfigRepository;
    private final JsonMappingAdapter adapter;
    private final YamlMappingService yamlMappingService;

    // In-memory cache for converted mappings
    private final Map<String, YamlMapping> mappingCache = new ConcurrentHashMap<>();

    /**
     * Load field mappings from JSON configuration stored in BATCH_CONFIGURATIONS table
     *
     * @param configId Configuration ID from BATCH_CONFIGURATIONS table
     * @return List of field mappings sorted by target position
     */
    public List<Map.Entry<String, FieldMapping>> loadFieldMappings(String configId) {
        YamlMapping mapping = getYamlMappingFromJson(configId, null);

        return mapping.getFields().entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().getTargetPosition()))
                .collect(Collectors.toList());
    }

    /**
     * Get YamlMapping object from JSON configuration with caching
     *
     * @param configId Configuration ID from BATCH_CONFIGURATIONS table
     * @param transactionType Optional transaction type override
     * @return YamlMapping object
     */
    @Cacheable(value = "jsonMappings", key = "#configId + '_' + #transactionType")
    public YamlMapping getYamlMappingFromJson(String configId, String transactionType) {
        String cacheKey = configId + "_" + (transactionType != null ? transactionType : "default");

        return mappingCache.computeIfAbsent(cacheKey, key -> {
            log.info("Loading batch configuration from BATCH_CONFIGURATIONS table for ID: {}", configId);

            Optional<BatchConfigurationEntity> configOpt = batchConfigRepository.findById(configId);

            if (configOpt.isEmpty()) {
                throw new RuntimeException("Batch configuration not found: " + configId);
            }

            BatchConfigurationEntity config = configOpt.get();

            if (!config.isEnabled()) {
                throw new RuntimeException("Batch configuration is disabled: " + configId);
            }

            String jsonConfig = config.getConfigurationJson();

            if (jsonConfig == null || jsonConfig.isEmpty()) {
                throw new RuntimeException("No configuration JSON found for: " + configId);
            }

            log.debug("Converting JSON configuration to YamlMapping for configId: {}", configId);
            return adapter.convertJsonToYamlMapping(jsonConfig, transactionType);
        });
    }
    
    /**
     * Transform a single field using existing YamlMappingService logic
     * 
     * @param row Source data row
     * @param mapping Field mapping configuration
     * @return Transformed field value
     */
    public String transformField(Map<String, Object> row, FieldMapping mapping) {
        return yamlMappingService.transformField(row, mapping);
    }
    
    /**
     * Clear cached mappings for a specific configuration
     * 
     * @param configId Configuration ID to clear from cache
     */
    public void clearCache(String configId) {
        mappingCache.entrySet().removeIf(entry -> entry.getKey().startsWith(configId + "_"));
        log.info("Cleared cache for configId: {}", configId);
    }
    
    /**
     * Clear all cached mappings
     */
    public void clearAllCache() {
        mappingCache.clear();
        log.info("Cleared all JSON mapping cache");
    }
    
    /**
     * Get cache statistics
     * 
     * @return Current cache size
     */
    public int getCacheSize() {
        return mappingCache.size();
    }
}