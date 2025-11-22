package com.truist.batch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truist.batch.entity.BatchConfigurationEntity;
import com.truist.batch.mapping.YamlMappingService;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.repository.BatchConfigurationRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for executing manual batch jobs with Phase 2 field transformations.
 * This service is independent from the API module and handles actual batch processing logic.
 *
 * ARCHITECTURAL NOTE:
 * - Reads configuration from BATCH_CONFIGURATIONS table (not MANUAL_JOB_CONFIG)
 * - Uses YamlMappingService for transformations
 * - Generates fixed-width output files
 * - Provides execution metrics for tracking
 *
 * @author Claude Code
 * @since Phase 2 - Batch Module Separation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManualBatchExecutionService {

    private final BatchConfigurationRepository batchConfigRepository;
    private final JsonMappingService jsonMappingService;
    private final YamlMappingService yamlMappingService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private static final int MAX_RECORDS_PER_EXECUTION = 10000;
    private static final String OUTPUT_DIRECTORY = "/tmp/";

    /**
     * Execute a batch job with the given configuration and parameters
     *
     * @param batchConfigId ID from BATCH_CONFIGURATIONS table
     * @param masterQuerySql SQL query to fetch source data
     * @param executionParameters Runtime parameters (e.g., batchDate)
     * @param executionId Unique execution identifier
     * @return Execution result with metrics
     */
    public BatchExecutionResult executeBatchJob(
            String batchConfigId,
            String masterQuerySql,
            Map<String, Object> executionParameters,
            String executionId) {

        BatchExecutionResult result = new BatchExecutionResult();
        result.setExecutionId(executionId);
        result.setStartTime(LocalDateTime.now());
        result.setStatus("RUNNING");

        try {
            log.info("Starting batch execution: {}", executionId);

            // 1. Load batch configuration from BATCH_CONFIGURATIONS table
            BatchConfigurationEntity config = batchConfigRepository.findById(batchConfigId)
                    .orElseThrow(() -> new RuntimeException("Batch configuration not found: " + batchConfigId));

            if (!config.isEnabled()) {
                throw new RuntimeException("Batch configuration is disabled: " + batchConfigId);
            }

            result.setJobName(config.getJobName());
            result.setSourceSystem(config.getSourceSystem());

            // 2. Parse field mappings from BATCH_CONFIGURATIONS.CONFIGURATION_JSON
            List<Map.Entry<String, FieldMapping>> fieldMappings =
                    jsonMappingService.loadFieldMappings(batchConfigId);

            log.info("Loaded {} field mappings for config: {}", fieldMappings.size(), batchConfigId);

            // 3. Execute master query with parameters
            List<Map<String, Object>> sourceData = executeMasterQuery(masterQuerySql, executionParameters);

            log.info("Master query returned {} records", sourceData.size());

            // 4. Apply row limit for safety
            if (sourceData.size() > MAX_RECORDS_PER_EXECUTION) {
                log.warn("Limiting execution to {} records (total: {})",
                        MAX_RECORDS_PER_EXECUTION, sourceData.size());
                sourceData = sourceData.subList(0, MAX_RECORDS_PER_EXECUTION);
            }

            // 5. Generate output file with Phase 2 transformations
            String outputFilePath = generateOutputFile(
                    sourceData,
                    fieldMappings,
                    config.getJobName(),
                    executionId
            );

            // 6. Calculate metrics
            long fileSize = getFileSize(outputFilePath);

            result.setRecordsProcessed(sourceData.size());
            result.setRecordsSuccess(sourceData.size());
            result.setRecordsError(0);
            result.setOutputFilePath(outputFilePath);
            result.setOutputFileSize(fileSize);
            result.setStatus("COMPLETED");
            result.setEndTime(LocalDateTime.now());

            log.info("Batch execution completed successfully: {}", executionId);
            log.info("Output file: {} ({} bytes)", outputFilePath, fileSize);

            return result;

        } catch (Exception e) {
            log.error("Batch execution failed: " + executionId, e);
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage());
            result.setEndTime(LocalDateTime.now());
            return result;
        }
    }

    /**
     * Execute master query with parameterized values
     */
    private List<Map<String, Object>> executeMasterQuery(
            String sql,
            Map<String, Object> parameters) {

        try {
            // Replace parameters in SQL (basic implementation)
            String processedSql = sql;
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String placeholder = ":" + entry.getKey();
                String value = entry.getValue().toString();

                // Add quotes for date strings
                if (entry.getKey().toLowerCase().contains("date")) {
                    value = "TO_DATE('" + value + "', 'YYYY-MM-DD')";
                } else if (entry.getValue() instanceof String) {
                    value = "'" + value + "'";
                }

                processedSql = processedSql.replace(placeholder, value);
            }

            log.debug("Executing SQL: {}", processedSql);
            return jdbcTemplate.queryForList(processedSql);

        } catch (Exception e) {
            log.error("Failed to execute master query", e);
            throw new RuntimeException("Master query execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate fixed-width output file with Phase 2 transformations
     */
    private String generateOutputFile(
            List<Map<String, Object>> sourceData,
            List<Map.Entry<String, FieldMapping>> fieldMappings,
            String jobName,
            String executionId) throws IOException {

        // Generate output file name
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("%s_%s_%s.txt", jobName, executionId, timestamp);
        String outputFilePath = OUTPUT_DIRECTORY + fileName;

        log.info("Generating output file: {}", outputFilePath);

        int processedRecords = 0;
        int errorRecords = 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            for (Map<String, Object> sourceRow : sourceData) {
                try {
                    // Apply Phase 2 transformations to each field
                    StringBuilder outputLine = new StringBuilder();

                    for (Map.Entry<String, FieldMapping> entry : fieldMappings) {
                        FieldMapping mapping = entry.getValue();

                        // Transform field using YamlMappingService (supports all Phase 2 transformations)
                        String transformedValue = yamlMappingService.transformField(sourceRow, mapping);

                        // Apply padding for fixed-width format
                        String paddedValue = applyPadding(transformedValue, mapping);

                        outputLine.append(paddedValue);
                    }

                    writer.write(outputLine.toString());
                    writer.newLine();
                    processedRecords++;

                } catch (Exception e) {
                    log.warn("Error processing record {}: {}", processedRecords + 1, e.getMessage());
                    errorRecords++;
                    // Skip row and continue (as per user requirement)
                }
            }
        }

        log.info("Output file generated: {} records processed, {} errors",
                processedRecords, errorRecords);

        return outputFilePath;
    }

    /**
     * Apply padding to field value based on mapping configuration
     */
    private String applyPadding(String value, FieldMapping mapping) {
        if (value == null) {
            value = mapping.getDefaultValue() != null ? mapping.getDefaultValue() : "";
        }

        int targetLength = mapping.getLength() > 0 ? mapping.getLength() : value.length();

        // Truncate if too long
        if (value.length() > targetLength) {
            return value.substring(0, targetLength);
        }

        // Pad if too short
        if (value.length() < targetLength) {
            String paddingChar = mapping.getPadChar() != null ? mapping.getPadChar() : " ";
            String padding = paddingChar.repeat(targetLength - value.length());

            if ("left".equalsIgnoreCase(mapping.getPad())) {
                return padding + value;
            } else {
                return value + padding;
            }
        }

        return value;
    }

    /**
     * Get file size in bytes
     */
    private long getFileSize(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.size(path);
        } catch (IOException e) {
            log.warn("Could not determine file size: {}", filePath);
            return 0L;
        }
    }

    /**
     * Result object for batch execution
     */
    @Data
    public static class BatchExecutionResult {
        private String executionId;
        private String jobName;
        private String sourceSystem;
        private String status;
        private int recordsProcessed;
        private int recordsSuccess;
        private int recordsError;
        private String outputFilePath;
        private Long outputFileSize;
        private String errorMessage;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        public long getDurationSeconds() {
            if (startTime != null && endTime != null) {
                return java.time.Duration.between(startTime, endTime).getSeconds();
            }
            return 0;
        }
    }
}
