package com.truist.batch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truist.batch.entity.ManualJobConfigEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for executing batch jobs using JSON configurations from database.
 * Provides manual triggering capabilities for testing and operations.
 * 
 * @author Senior Full Stack Developer Agent
 * @since Phase 3 - Batch Execution Enhancement
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ManualJobExecutionService {
    
    private final ManualJobConfigService configService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired(required = false)
    private JobLauncher jobLauncher;
    
    @Autowired(required = false)
    private ApplicationContext applicationContext;
    
    @Value("${batch.output.directory:/tmp/batch/output}")
    private String outputDirectory;
    
    /**
     * Execute a batch job using JSON configuration from database
     * 
     * @param configId Configuration ID from MANUAL_JOB_CONFIG table
     * @param executedBy User executing the job
     * @param additionalParams Additional job parameters
     * @return Job execution result
     */
    @Transactional
    public Map<String, Object> executeJobWithJsonConfig(String configId, String executedBy, Map<String, String> additionalParams) {
        try {
            log.info("Starting batch job execution for configId: {} by user: {}", configId, executedBy);
            
            // Load configuration from database
            ManualJobConfigEntity config = configService.getJobConfiguration(configId)
                .orElseThrow(() -> new RuntimeException("Configuration not found: " + configId));
            
            // Parse JSON configuration
            JsonNode configNode = objectMapper.readTree(config.getJobParameters());
            
            String sourceSystem = configNode.path("sourceSystem").asText();
            String jobName = configNode.path("jobName").asText();
            String transactionType = configNode.path("transactionType").asText();
            
            // Generate execution tracking IDs
            String executionId = generateExecutionId(jobName);
            String correlationId = UUID.randomUUID().toString();
            
            // Create batch execution record
            createBatchExecutionRecord(configId, executionId, sourceSystem, correlationId);
            
            // Build job parameters
            JobParametersBuilder parametersBuilder = new JobParametersBuilder()
                .addString("configId", configId)
                .addString("sourceSystem", sourceSystem)
                .addString("jobName", jobName)
                .addString("transactionType", transactionType)
                .addString("executionId", executionId)
                .addString("correlationId", correlationId)
                .addString("executedBy", executedBy)
                .addString("outputDirectory", outputDirectory)
                .addLong("timestamp", System.currentTimeMillis());
            
            // Add batch date parameter (default to today if not provided)
            String batchDate = additionalParams != null ? 
                additionalParams.getOrDefault("batchDate", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE)) :
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);
            parametersBuilder.addString("batchDate", batchDate);
            
            // Add any additional parameters
            if (additionalParams != null) {
                additionalParams.forEach((key, value) -> {
                    if (!"batchDate".equals(key)) {
                        parametersBuilder.addString(key, value);
                    }
                });
            }
            
            JobParameters jobParameters = parametersBuilder.toJobParameters();
            
            // Execute job if JobLauncher is available
            JobExecution execution = null;
            if (jobLauncher != null) {
                try {
                    // Get the generic job bean
                    Job genericJob = applicationContext.getBean("genericJob", Job.class);
                    
                    log.info("Launching batch job with parameters: {}", jobParameters);
                    execution = jobLauncher.run(genericJob, jobParameters);
                    
                    log.info("Job execution completed with status: {}", execution.getStatus());
                    
                    // Update execution record with results
                    updateBatchExecutionRecord(executionId, execution.getStatus().toString(), null);
                    
                } catch (Exception e) {
                    log.error("Failed to execute batch job", e);
                    updateBatchExecutionRecord(executionId, "FAILED", e.getMessage());
                    throw new RuntimeException("Batch job execution failed", e);
                }
            } else {
                log.warn("JobLauncher not available - simulating execution for testing");
                updateBatchExecutionRecord(executionId, "SIMULATED", "JobLauncher not configured");
            }
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("executionId", executionId);
            response.put("correlationId", correlationId);
            response.put("status", execution != null ? execution.getStatus().toString() : "SIMULATED");
            response.put("startTime", LocalDateTime.now().toString());
            response.put("configId", configId);
            response.put("jobName", jobName);
            response.put("message", "Batch job execution initiated successfully");
            
            return response;
            
        } catch (Exception e) {
            log.error("Failed to execute job with config: {}", configId, e);
            throw new RuntimeException("Job execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get execution status for a given execution ID
     */
    public Map<String, Object> getExecutionStatus(String executionId) {
        String sql = "SELECT * FROM BATCH_EXECUTION_RESULTS WHERE EXECUTION_ID = ?";
        
        return jdbcTemplate.queryForMap(sql, executionId);
    }
    
    /**
     * Generate unique execution ID
     */
    private String generateExecutionId(String jobName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("exec_%s_%s_%s", jobName, timestamp, UUID.randomUUID().toString().substring(0, 8));
    }
    
    /**
     * Create batch execution record in database
     */
    private void createBatchExecutionRecord(String configId, String executionId, String sourceSystem, String correlationId) {
        String sql = "INSERT INTO BATCH_EXECUTION_RESULTS (JOB_CONFIG_ID, EXECUTION_ID, SOURCE_SYSTEM, " +
                    "STATUS, CORRELATION_ID) VALUES (?, ?, ?, ?, ?)";
        
        jdbcTemplate.update(sql, configId, executionId, sourceSystem, "RUNNING", correlationId);
        
        log.debug("Created batch execution record: {}", executionId);
    }
    
    /**
     * Update batch execution record with results
     */
    private void updateBatchExecutionRecord(String executionId, String status, String errorMessage) {
        String sql = "UPDATE BATCH_EXECUTION_RESULTS SET STATUS = ?, END_TIME = CURRENT_TIMESTAMP, " +
                    "ERROR_MESSAGE = ? WHERE EXECUTION_ID = ?";
        
        jdbcTemplate.update(sql, status, errorMessage, executionId);
        
        log.debug("Updated batch execution record: {} with status: {}", executionId, status);
    }
    
    /**
     * Get master query for a job configuration
     */
    public String getMasterQuery(String sourceSystem, String jobName) {
        String sql = "SELECT QUERY_TEXT FROM MASTER_QUERY_CONFIG WHERE SOURCE_SYSTEM = ? " +
                    "AND JOB_NAME = ? AND IS_ACTIVE = 'Y' ORDER BY VERSION DESC";
        
        try {
            return jdbcTemplate.queryForObject(sql, String.class, sourceSystem, jobName);
        } catch (Exception e) {
            log.error("Failed to retrieve master query for {} - {}", sourceSystem, jobName, e);
            return null;
        }
    }
}