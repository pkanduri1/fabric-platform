package com.truist.batch.idempotency.config;

import com.truist.batch.idempotency.service.IdempotencyService;
import com.truist.batch.idempotency.tasklet.IdempotencyCheckTasklet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * Job execution listener that provides idempotency-aware job lifecycle management.
 * Handles job completion status updates and correlation with idempotency records.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Slf4j
public class IdempotentJobExecutionListener implements JobExecutionListener {
    
    private final IdempotencyService idempotencyService;
    private final IdempotencyProperties idempotencyProperties;
    
    public IdempotentJobExecutionListener(IdempotencyService idempotencyService, 
                                        IdempotencyProperties idempotencyProperties) {
        this.idempotencyService = idempotencyService;
        this.idempotencyProperties = idempotencyProperties;
    }
    
    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Starting idempotent job execution: {} (ID: {})",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getId());
        
        // Add framework metadata to execution context
        jobExecution.getExecutionContext().putString("idempotencyFrameworkVersion", "1.0");
        jobExecution.getExecutionContext().putString("idempotencyEnabled", "true");
        jobExecution.getExecutionContext().putLong("jobStartTime", System.currentTimeMillis());
        
        if (idempotencyProperties.isDevelopmentMode()) {
            jobExecution.getExecutionContext().putString("developmentMode", "true");
            log.debug("Job execution running in development mode");
        }
    }
    
    @Override
    public void afterJob(JobExecution jobExecution) {
        long startTime = jobExecution.getExecutionContext().getLong("jobStartTime", 0L);
        long duration = startTime > 0 ? System.currentTimeMillis() - startTime : 0L;
        
        String jobName = jobExecution.getJobInstance().getJobName();
        String exitCode = jobExecution.getExitStatus().getExitCode();
        
        log.info("Completed idempotent job execution: {} (ID: {}, Status: {}, Duration: {}ms)",
                jobName, jobExecution.getId(), jobExecution.getStatus(), duration);
        
        // Extract idempotency information
        String idempotencyKey = jobExecution.getExecutionContext().getString(
                IdempotencyCheckTasklet.CONTEXT_IDEMPOTENCY_KEY);
        String correlationId = jobExecution.getExecutionContext().getString(
                IdempotencyCheckTasklet.CONTEXT_CORRELATION_ID);
        
        if (idempotencyKey != null) {
            log.info("Job execution idempotency tracking - Key: {}, Correlation: {}", 
                    idempotencyKey, correlationId);
            
            // Handle different completion scenarios
            handleJobCompletion(jobExecution, idempotencyKey, correlationId, duration);
        }
        
        // Publish completion events if configured
        if (idempotencyProperties.getEvents().isPublishJobEvents()) {
            publishJobCompletionEvent(jobExecution, duration, idempotencyKey, correlationId);
        }
        
        // Log performance warnings if applicable
        if (duration > idempotencyProperties.getPerformance().getWarningThresholdMs()) {
            log.warn("Job execution exceeded performance threshold: {} ({}ms > {}ms)",
                    jobName, duration, idempotencyProperties.getPerformance().getWarningThresholdMs());
        }
    }
    
    /**
     * Handles different job completion scenarios based on exit status.
     */
    private void handleJobCompletion(JobExecution jobExecution, String idempotencyKey, 
                                   String correlationId, long duration) {
        
        String exitCode = jobExecution.getExitStatus().getExitCode();
        
        switch (exitCode) {
            case "COMPLETED":
                log.debug("Job completed successfully - idempotency record will be marked as completed");
                // The IdempotencyService handles the completion status internally
                break;
                
            case "FAILED":
                log.warn("Job execution failed - idempotency record will track failure for retry logic");
                // The IdempotencyService handles the failure status internally
                break;
                
            case IdempotencyCheckTasklet.EXIT_STATUS_DUPLICATE:
                log.info("Job was identified as duplicate - no processing was performed");
                break;
                
            case IdempotencyCheckTasklet.EXIT_STATUS_IN_PROGRESS:
                log.warn("Job was already in progress elsewhere - execution was skipped");
                break;
                
            case IdempotencyCheckTasklet.EXIT_STATUS_MAX_RETRIES:
                log.error("Job execution exceeded maximum retry attempts");
                break;
                
            default:
                log.debug("Job completed with exit code: {}", exitCode);
                break;
        }
    }
    
    /**
     * Publishes job completion event for monitoring and alerting.
     */
    private void publishJobCompletionEvent(JobExecution jobExecution, long duration,
                                         String idempotencyKey, String correlationId) {
        try {
            // Create job completion event (placeholder for actual event system integration)
            JobCompletionEvent event = new JobCompletionEvent(
                    jobExecution.getJobInstance().getJobName(),
                    jobExecution.getId(),
                    jobExecution.getStatus().toString(),
                    jobExecution.getExitStatus().getExitCode(),
                    duration,
                    idempotencyKey,
                    correlationId,
                    System.currentTimeMillis()
            );
            
            log.debug("Publishing job completion event: {}", event);
            
            // Actual event publishing would be implemented here
            // Could integrate with Kafka, RabbitMQ, Spring Events, etc.
            
        } catch (Exception e) {
            log.warn("Failed to publish job completion event for job: {} - {}", 
                    jobExecution.getJobInstance().getJobName(), e.getMessage());
        }
    }
    
    /**
     * Job completion event data class.
     */
    private static class JobCompletionEvent {
        private final String jobName;
        private final Long jobExecutionId;
        private final String status;
        private final String exitCode;
        private final long durationMs;
        private final String idempotencyKey;
        private final String correlationId;
        private final long timestamp;
        
        public JobCompletionEvent(String jobName, Long jobExecutionId, String status, 
                                String exitCode, long durationMs, String idempotencyKey, 
                                String correlationId, long timestamp) {
            this.jobName = jobName;
            this.jobExecutionId = jobExecutionId;
            this.status = status;
            this.exitCode = exitCode;
            this.durationMs = durationMs;
            this.idempotencyKey = idempotencyKey;
            this.correlationId = correlationId;
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("JobCompletionEvent[job=%s, id=%d, status=%s, exit=%s, duration=%dms, key=%s]",
                    jobName, jobExecutionId, status, exitCode, durationMs, idempotencyKey);
        }
    }
}