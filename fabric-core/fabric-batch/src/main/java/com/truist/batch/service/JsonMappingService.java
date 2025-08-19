package com.truist.batch.service;

import com.truist.batch.adapter.JsonMappingAdapter;
import com.truist.batch.entity.ManualJobConfigEntity;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.YamlMapping;
import com.truist.batch.repository.ManualJobConfigRepository;
import com.truist.batch.mapping.YamlMappingService;
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
 * Service for managing JSON-based field mappings stored in database.
 * Provides caching and transformation capabilities using existing YamlMappingService logic.
 * 
 * @author Senior Full Stack Developer Agent
 * @since Phase 3 - Batch Execution Enhancement
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JsonMappingService {
    
    private final ManualJobConfigRepository configRepository;
    private final JsonMappingAdapter adapter;
    private final YamlMappingService yamlMappingService;
    
    // In-memory cache for converted mappings
    private final Map<String, YamlMapping> mappingCache = new ConcurrentHashMap<>();
    
    /**
     * Load field mappings from JSON configuration stored in database
     * 
     * @param configId Configuration ID from MANUAL_JOB_CONFIG table
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
     * @param configId Configuration ID
     * @param transactionType Optional transaction type override
     * @return YamlMapping object
     */
    @Cacheable(value = "jsonMappings", key = "#configId + '_' + #transactionType")
    public YamlMapping getYamlMappingFromJson(String configId, String transactionType) {
        String cacheKey = configId + "_" + (transactionType != null ? transactionType : "default");
        
        return mappingCache.computeIfAbsent(cacheKey, key -> {
            log.info("Loading JSON configuration for configId: {}", configId);
            
            Optional<ManualJobConfigEntity> configOpt = configRepository.findByConfigId(configId);
            
            if (configOpt.isEmpty()) {
                throw new RuntimeException("Configuration not found: " + configId);
            }
            
            ManualJobConfigEntity config = configOpt.get();
            String jsonConfig = config.getJobParameters();
            
            if (jsonConfig == null || jsonConfig.isEmpty()) {
                throw new RuntimeException("No job parameters found for configuration: " + configId);
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