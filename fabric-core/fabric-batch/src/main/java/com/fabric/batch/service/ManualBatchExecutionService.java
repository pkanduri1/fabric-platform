package com.fabric.batch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fabric.batch.entity.BatchConfigurationEntity;
import com.fabric.batch.mapping.YamlMappingService;
import com.fabric.batch.model.FieldMapping;
import com.fabric.batch.repository.BatchConfigurationRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
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

    @Value("${batch.execution.retry.maxAttempts:3}")
    private int maxRetryAttempts;

    @Value("${batch.execution.retry.backoffMs:500}")
    private long retryBackoffMs;

    @Value("${batch.execution.checkpoint.interval:100}")
    private int checkpointInterval;

    @Value("${batch.execution.state.dir:/tmp/fabric-batch-state}")
    private String executionStateDir;

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

            // Idempotent rerun guard: if completion marker exists, return existing result
            if (isAlreadyCompleted(executionId)) {
                String existingOutput = getOutputPathFromCompletion(executionId);
                result.setStatus("COMPLETED");
                result.setOutputFilePath(existingOutput);
                result.setOutputFileSize(getFileSize(existingOutput));
                result.setEndTime(LocalDateTime.now());
                log.info("Execution {} already completed earlier; returning cached completion", executionId);
                return result;
            }

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

            // 3. Execute master query with retry policy
            List<Map<String, Object>> sourceData = withRetry(
                    () -> executeMasterQuery(masterQuerySql, executionParameters),
                    "master-query",
                    executionId
            );

            log.info("Master query returned {} records", sourceData.size());

            // 4. Apply row limit for safety
            if (sourceData.size() > MAX_RECORDS_PER_EXECUTION) {
                log.warn("Limiting execution to {} records (total: {})",
                        MAX_RECORDS_PER_EXECUTION, sourceData.size());
                sourceData = sourceData.subList(0, MAX_RECORDS_PER_EXECUTION);
            }

            // 5. Generate output file with retry + checkpoint/restart support
            final List<Map<String, Object>> executionData = sourceData;
            OutputGenerationResult output = withRetry(
                    () -> generateOutputFile(
                            executionData,
                            fieldMappings,
                            config.getJobName(),
                            executionId
                    ),
                    "output-generation",
                    executionId
            );

            // 6. Calculate metrics
            long fileSize = getFileSize(output.getOutputFilePath());

            result.setRecordsProcessed(output.getRecordsProcessed());
            result.setRecordsSuccess(output.getRecordsProcessed() - output.getRecordsError());
            result.setRecordsError(output.getRecordsError());
            result.setOutputFilePath(output.getOutputFilePath());
            result.setOutputFileSize(fileSize);
            result.setStatus("COMPLETED");
            result.setEndTime(LocalDateTime.now());

            markCompleted(executionId, output.getOutputFilePath());

            log.info("Batch execution completed successfully: {}", executionId);
            log.info("Output file: {} ({} bytes)", output.getOutputFilePath(), fileSize);

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
     * Generate fixed-width output file with checkpoint/restart support.
     */
    private OutputGenerationResult generateOutputFile(
            List<Map<String, Object>> sourceData,
            List<Map.Entry<String, FieldMapping>> fieldMappings,
            String jobName,
            String executionId) throws IOException {

        String fileName = String.format("%s_%s.txt", jobName, executionId);
        String outputFilePath = OUTPUT_DIRECTORY + fileName;

        int startIndex = readCheckpoint(executionId);
        boolean appendMode = startIndex > 0;

        log.info("Generating output file: {} (resumeStartIndex={})", outputFilePath, startIndex);

        int processedRecords = startIndex;
        int errorRecords = 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, appendMode))) {
            for (int i = startIndex; i < sourceData.size(); i++) {
                Map<String, Object> sourceRow = sourceData.get(i);
                try {
                    StringBuilder outputLine = new StringBuilder();

                    for (Map.Entry<String, FieldMapping> entry : fieldMappings) {
                        FieldMapping mapping = entry.getValue();
                        String transformedValue = yamlMappingService.transformField(sourceRow, mapping);
                        String paddedValue = applyPadding(transformedValue, mapping);
                        outputLine.append(paddedValue);
                    }

                    writer.write(outputLine.toString());
                    writer.newLine();
                    processedRecords++;

                    if (processedRecords % checkpointInterval == 0) {
                        writeCheckpoint(executionId, processedRecords);
                    }

                } catch (Exception e) {
                    log.warn("Error processing record {}: {}", i + 1, e.getMessage());
                    errorRecords++;
                }
            }
        }

        clearCheckpoint(executionId);

        log.info("Output file generated: {} records processed, {} errors", processedRecords, errorRecords);

        OutputGenerationResult result = new OutputGenerationResult();
        result.setOutputFilePath(outputFilePath);
        result.setRecordsProcessed(processedRecords);
        result.setRecordsError(errorRecords);
        return result;
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

    private <T> T withRetry(Retryable<T> operation, String operationName, String executionId) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                return operation.run();
            } catch (RuntimeException | IOException e) {
                last = new RuntimeException(e);
                if (attempt == maxRetryAttempts) {
                    break;
                }
                long backoff = retryBackoffMs * attempt;
                log.warn("{} failed (attempt {}/{}), backing off {} ms [execution:{}]: {}",
                        operationName, attempt, maxRetryAttempts, backoff, executionId, e.getMessage());
                sleep(backoff);
            }
        }
        throw new RuntimeException("Operation failed after retries: " + operationName, last);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private int readCheckpoint(String executionId) {
        try {
            Path path = checkpointPath(executionId);
            if (!Files.exists(path)) {
                return 0;
            }
            return Integer.parseInt(Files.readString(path).trim());
        } catch (Exception e) {
            log.warn("Failed reading checkpoint for {}: {}", executionId, e.getMessage());
            return 0;
        }
    }

    private void writeCheckpoint(String executionId, int recordCount) {
        try {
            Files.createDirectories(stateDir());
            Files.writeString(checkpointPath(executionId), String.valueOf(recordCount),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            log.warn("Failed writing checkpoint for {}: {}", executionId, e.getMessage());
        }
    }

    private void clearCheckpoint(String executionId) {
        try {
            Files.deleteIfExists(checkpointPath(executionId));
        } catch (Exception e) {
            log.warn("Failed clearing checkpoint for {}: {}", executionId, e.getMessage());
        }
    }

    private boolean isAlreadyCompleted(String executionId) {
        return Files.exists(completionPath(executionId));
    }

    private void markCompleted(String executionId, String outputPath) {
        try {
            Files.createDirectories(stateDir());
            Files.writeString(completionPath(executionId), outputPath,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            log.warn("Failed writing completion marker for {}: {}", executionId, e.getMessage());
        }
    }

    private String getOutputPathFromCompletion(String executionId) {
        try {
            return Files.readString(completionPath(executionId)).trim();
        } catch (Exception e) {
            return OUTPUT_DIRECTORY + executionId + ".txt";
        }
    }

    private Path checkpointPath(String executionId) {
        return stateDir().resolve(executionId + ".checkpoint");
    }

    private Path completionPath(String executionId) {
        return stateDir().resolve(executionId + ".done");
    }

    private Path stateDir() {
        return Paths.get(executionStateDir);
    }

    @FunctionalInterface
    private interface Retryable<T> {
        T run() throws IOException;
    }

    @Data
    private static class OutputGenerationResult {
        private String outputFilePath;
        private int recordsProcessed;
        private int recordsError;
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
