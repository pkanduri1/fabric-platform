package com.truist.batch.processor;

import com.truist.batch.mapping.YamlMappingService;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.FileConfig;
import com.truist.batch.model.YamlMapping;
import com.truist.batch.service.JsonMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enhanced Generic ItemProcessor that supports both YAML and JSON configurations.
 * Maps input records to fixed-width output format based on field mappings.
 * 
 * @author Senior Full Stack Developer Agent
 * @since Phase 3 - Batch Execution Enhancement
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class EnhancedGenericProcessor implements ItemProcessor<Map<String, Object>, Map<String, Object>> {

    private final FileConfig fileConfig;
    private final YamlMappingService yamlMappingService;
    private final JsonMappingService jsonMappingService;
    
    @Value("#{jobParameters['configId']}")
    private String configId;
    
    @Value("#{jobParameters['transactionType']}")
    private String transactionType;
    
    @Value("#{jobParameters['sourceSystem']}")
    private String sourceSystem;
    
    @Value("#{jobParameters['jobName']}")
    private String jobName;

    /**
     * Process input item using either JSON or YAML configuration
     */
    @Override
    public Map<String, Object> process(Map<String, Object> item) throws Exception {
        log.debug("Processing item with configId: {}, sourceSystem: {}, jobName: {}", 
                 configId, sourceSystem, jobName);
        log.debug("Input item: {}", item);
        
        YamlMapping mapping;
        boolean usingJsonConfig = false;
        
        // Determine configuration source
        if (configId != null && !configId.isEmpty() && !"null".equals(configId)) {
            // Use JSON configuration from database
            log.info("Using JSON configuration from database for configId: {}", configId);
            mapping = jsonMappingService.getYamlMappingFromJson(configId, transactionType);
            usingJsonConfig = true;
        } else if (fileConfig != null && fileConfig.getTemplate() != null) {
            // Use YAML configuration from classpath
            log.info("Using YAML configuration from classpath for template: {}", fileConfig.getTemplate());
            String txnType = transactionType != null ? transactionType : fileConfig.getTransactionType();
            mapping = yamlMappingService.getMapping(fileConfig.getTemplate(), txnType);
        } else {
            throw new IllegalStateException("No configuration available - neither configId nor template provided");
        }
        
        // Extract and sort field mappings by target position
        List<FieldMapping> fields = mapping.getFields().entrySet().stream()
            .map(Map.Entry::getValue)
            .sorted(Comparator.comparingInt(FieldMapping::getTargetPosition))
            .collect(Collectors.toList());
        
        log.debug("Processing {} fields from {} configuration", fields.size(), 
                 usingJsonConfig ? "JSON" : "YAML");
        
        // Process each field mapping
        Map<String, Object> output = new LinkedHashMap<>();
        for (FieldMapping fieldMapping : fields) {
            String value;
            
            if (usingJsonConfig) {
                // Use JSON mapping service for transformation
                value = jsonMappingService.transformField(item, fieldMapping);
            } else {
                // Use YAML mapping service for transformation
                value = yamlMappingService.transformField(item, fieldMapping);
            }
            
            // Log field transformation for debugging
            if (log.isTraceEnabled()) {
                log.trace("Field '{}': source='{}', value='{}', transformed='{}'", 
                         fieldMapping.getFieldName(),
                         fieldMapping.getSourceField(),
                         item.get(fieldMapping.getSourceField()),
                         value);
            }
            
            // Use field name as key and apply default if value is null
            String outputKey = fieldMapping.getFieldName() != null ? 
                              fieldMapping.getFieldName() : 
                              fieldMapping.getTargetField();
            
            output.put(outputKey, value != null ? value : fieldMapping.getDefaultValue());
        }
        
        log.debug("Processed output with {} fields", output.size());
        
        return output;
    }
}