package com.truist.batch.idempotency.tasklet;

import com.truist.batch.idempotency.model.IdempotencyRequest;
import com.truist.batch.idempotency.model.IdempotencyResult;
import com.truist.batch.idempotency.model.IdempotencyStatus;
import com.truist.batch.idempotency.model.RequestContext;
import com.truist.batch.idempotency.service.IdempotencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Batch Tasklet for idempotency checking and registration.
 * This tasklet runs as the first step in idempotent batch jobs to ensure
 * that duplicate executions are detected and handled appropriately.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Component
@StepScope
@Slf4j
public class IdempotencyCheckTasklet implements Tasklet {
    
    private final IdempotencyService idempotencyService;
    
    // Exit status constants for different idempotency scenarios
    public static final String EXIT_STATUS_DUPLICATE = "DUPLICATE";
    public static final String EXIT_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String EXIT_STATUS_MAX_RETRIES = "MAX_RETRIES_EXCEEDED";
    public static final String EXIT_STATUS_PROCEED = "PROCEED";
    
    // Execution context keys for sharing idempotency information
    public static final String CONTEXT_IDEMPOTENCY_KEY = "idempotencyKey";
    public static final String CONTEXT_CORRELATION_ID = "correlationId";
    public static final String CONTEXT_IS_DUPLICATE = "isDuplicate";
    public static final String CONTEXT_IS_RETRY = "isRetry";
    public static final String CONTEXT_RETRY_COUNT = "retryCount";
    
    public IdempotencyCheckTasklet(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        
        StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
        JobParameters jobParameters = stepExecution.getJobExecution().getJobParameters();
        ExecutionContext executionContext = stepExecution.getExecutionContext();
        
        log.info("Starting idempotency check for job: {} with execution ID: {}", 
                stepExecution.getJobExecution().getJobInstance().getJobName(),
                stepExecution.getJobExecution().getId());
        
        try {
            // Extract job identification parameters
            IdempotencyRequest request = buildIdempotencyRequest(jobParameters, stepExecution);
            
            log.debug("Created idempotency request: {}", request.getSummary());
            
            // Perform idempotency check using a simple operation that returns Boolean
            IdempotencyResult<Boolean> result = idempotencyService.processWithIdempotencyForBatchJob(
                request,
                () -> {
                    log.info("Idempotency check passed - proceeding with new job execution");
                    return true;
                },
                Boolean.class
            );
            
            // Handle the idempotency result
            return handleIdempotencyResult(result, stepExecution, executionContext);
            
        } catch (Exception e) {
            log.error("Error during idempotency check for job: {} - {}", 
                    stepExecution.getJobExecution().getJobInstance().getJobName(), e.getMessage(), e);
            
            // Set error information in execution context for downstream steps
            executionContext.putString("idempotencyError", e.getMessage());
            executionContext.putString("idempotencyErrorType", e.getClass().getSimpleName());
            
            throw e;
        }
    }
    
    /**
     * Builds an idempotency request from job parameters and step execution context.
     */
    private IdempotencyRequest buildIdempotencyRequest(JobParameters jobParameters, StepExecution stepExecution) 
            throws Exception {
        
        // Extract core job parameters
        String sourceSystem = jobParameters.getString("sourceSystem");
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        String fileName = jobParameters.getString("fileName");
        String filePath = jobParameters.getString("filePath");
        String transactionId = jobParameters.getString("transactionId");
        
        // Use fileName as transactionId if transactionId is not provided
        if (transactionId == null && fileName != null) {
            transactionId = fileName;
        }
        
        // Create request builder
        IdempotencyRequest.IdempotencyRequestBuilder requestBuilder = IdempotencyRequest.builder()
                .sourceSystem(sourceSystem)
                .jobName(jobName)
                .transactionId(transactionId)
                .filePath(filePath);
        
        // Add file hash if file path is provided
        if (filePath != null && !filePath.trim().isEmpty()) {
            try {
                String fileHash = calculateFileHash(filePath);
                requestBuilder.fileHash(fileHash);
                log.debug("Calculated file hash: {} for file: {}", fileHash, filePath);
            } catch (Exception e) {
                log.warn("Failed to calculate file hash for: {} - {}", filePath, e.getMessage());
                // Continue without file hash - other parameters can provide uniqueness
            }
        }
        
        // Add job parameters as custom attributes
        Map<String, Object> jobParams = new HashMap<>();
        if (jobParameters.getParameters() != null) {
            jobParameters.getParameters().forEach((key, value) -> {
                if (value != null) {
                    jobParams.put(key, value.getValue());
                }
            });
        }
        requestBuilder.jobParameters(jobParams);
        
        // Create request context
        RequestContext requestContext = RequestContext.builder()
                .userId("BATCH_USER")
                .sessionId(String.valueOf(stepExecution.getJobExecution().getId()))
                .clientIp("localhost")
                .userAgent("Spring Batch Job Execution")
                .applicationName("fabric-batch")
                .businessContext("Batch Job Processing: " + jobName)
                .requestTimestamp(LocalDateTime.now())
                .hostName(getHostName())
                .processId(getProcessId())
                .threadId(Thread.currentThread().getId())
                .build();
        
        requestBuilder.requestContext(requestContext);
        
        return requestBuilder.build();
    }
    
    /**
     * Handles the idempotency result and sets appropriate execution context and exit status.
     */
    private RepeatStatus handleIdempotencyResult(IdempotencyResult<Boolean> result, 
                                               StepExecution stepExecution, 
                                               ExecutionContext executionContext) {
        
        // Store idempotency information in execution context for downstream steps
        executionContext.putString(CONTEXT_IDEMPOTENCY_KEY, result.getIdempotencyKey());
        executionContext.putString(CONTEXT_CORRELATION_ID, result.getCorrelationId());
        
        log.info("Idempotency check result: {} for key: {}", 
                result.getStatus(), result.getIdempotencyKey());
        
        switch (result.getStatus()) {
            case SUCCESS:
                // New execution, continue processing
                executionContext.putString(CONTEXT_IS_DUPLICATE, "false");
                executionContext.putString(CONTEXT_IS_RETRY, "false");
                
                log.info("New idempotent job execution authorized: {} (correlation: {})", 
                        result.getIdempotencyKey(), result.getCorrelationId());
                
                stepExecution.setExitStatus(new ExitStatus(EXIT_STATUS_PROCEED));
                return RepeatStatus.FINISHED;
                
            case CACHED_RESULT:
                // Job already completed successfully, skip processing
                executionContext.putString(CONTEXT_IS_DUPLICATE, "true");
                executionContext.putString(CONTEXT_IS_RETRY, "false");
                
                log.info("Duplicate job execution detected - job already completed successfully: {}", 
                        result.getIdempotencyKey());
                
                // Mark the entire job as completed
                stepExecution.setExitStatus(new ExitStatus(EXIT_STATUS_DUPLICATE));
                stepExecution.getJobExecution().setExitStatus(new ExitStatus(EXIT_STATUS_DUPLICATE));
                
                return RepeatStatus.FINISHED;
                
            case IN_PROGRESS:
                // Job currently running elsewhere
                executionContext.putString(CONTEXT_IS_DUPLICATE, "true");
                executionContext.putString(CONTEXT_IS_RETRY, "false");
                
                log.warn("Job already in progress with idempotency key: {}", result.getIdempotencyKey());
                
                stepExecution.setExitStatus(new ExitStatus(EXIT_STATUS_IN_PROGRESS));
                stepExecution.getJobExecution().setExitStatus(new ExitStatus(EXIT_STATUS_IN_PROGRESS));
                
                // This should stop the job execution
                return RepeatStatus.FINISHED;
                
            case FAILED:
                // Previous execution failed, this is a retry
                executionContext.putString(CONTEXT_IS_DUPLICATE, "false");
                executionContext.putString(CONTEXT_IS_RETRY, "true");
                executionContext.put(CONTEXT_RETRY_COUNT, result.getRetryCount());
                
                log.info("Retry execution authorized for failed job: {} (retry count: {})", 
                        result.getIdempotencyKey(), result.getRetryCount());
                
                stepExecution.setExitStatus(new ExitStatus(EXIT_STATUS_PROCEED));
                return RepeatStatus.FINISHED;
                
            case MAX_RETRIES_EXCEEDED:
                // Too many retries
                executionContext.putString(CONTEXT_IS_DUPLICATE, "true");
                executionContext.putString(CONTEXT_IS_RETRY, "false");
                
                log.error("Maximum retries exceeded for idempotency key: {}", result.getIdempotencyKey());
                
                stepExecution.setExitStatus(new ExitStatus(EXIT_STATUS_MAX_RETRIES));
                stepExecution.getJobExecution().setExitStatus(new ExitStatus(EXIT_STATUS_MAX_RETRIES));
                
                return RepeatStatus.FINISHED;
                
            case EXPIRED:
                // Previous execution expired, treat as new
                executionContext.putString(CONTEXT_IS_DUPLICATE, "false");
                executionContext.putString(CONTEXT_IS_RETRY, "false");
                
                log.info("Expired idempotency record, proceeding with new execution: {}", 
                        result.getIdempotencyKey());
                
                stepExecution.setExitStatus(new ExitStatus(EXIT_STATUS_PROCEED));
                return RepeatStatus.FINISHED;
                
            default:
                log.error("Unknown idempotency result status: {} for key: {}", 
                        result.getStatus(), result.getIdempotencyKey());
                throw new IllegalStateException("Unknown idempotency result status: " + result.getStatus());
        }
    }
    
    /**
     * Calculates SHA-256 hash of a file.
     */
    private String calculateFileHash(String filePath) throws Exception {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.warn("File does not exist for hash calculation: {}", filePath);
                return null;
            }
            
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new Exception("File hash calculation failed", e);
        }
    }
    
    /**
     * Gets the hostname for tracking processing node.
     */
    private String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "UNKNOWN_HOST";
        }
    }
    
    /**
     * Gets the current process ID for tracking.
     */
    private Long getProcessId() {
        try {
            String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            return Long.parseLong(processName.split("@")[0]);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Utility method to check if the current execution is a duplicate.
     */
    public static boolean isDuplicateExecution(ExecutionContext executionContext) {
        String isDuplicate = executionContext.getString(CONTEXT_IS_DUPLICATE);
        return "true".equals(isDuplicate);
    }
    
    /**
     * Utility method to check if the current execution is a retry.
     */
    public static boolean isRetryExecution(ExecutionContext executionContext) {
        String isRetry = executionContext.getString(CONTEXT_IS_RETRY);
        return "true".equals(isRetry);
    }
    
    /**
     * Utility method to get the idempotency key from execution context.
     */
    public static String getIdempotencyKey(ExecutionContext executionContext) {
        return executionContext.getString(CONTEXT_IDEMPOTENCY_KEY);
    }
    
    /**
     * Utility method to get the correlation ID from execution context.
     */
    public static String getCorrelationId(ExecutionContext executionContext) {
        return executionContext.getString(CONTEXT_CORRELATION_ID);
    }
    
    /**
     * Utility method to get retry count from execution context.
     */
    public static Integer getRetryCount(ExecutionContext executionContext) {
        Object retryCount = executionContext.get(CONTEXT_RETRY_COUNT);
        return retryCount instanceof Integer ? (Integer) retryCount : 0;
    }
}