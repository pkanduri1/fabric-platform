package com.truist.batch.idempotency.config;

import com.truist.batch.idempotency.tasklet.IdempotencyCheckTasklet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * Step execution listener that provides idempotency-aware step lifecycle management.
 * Handles special processing for idempotency check steps and provides detailed
 * logging and monitoring for idempotent step executions.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Slf4j
public class IdempotentStepExecutionListener implements StepExecutionListener {
    
    private final IdempotencyProperties idempotencyProperties;
    
    public IdempotentStepExecutionListener(IdempotencyProperties idempotencyProperties) {
        this.idempotencyProperties = idempotencyProperties;
    }
    
    @Override
    public void beforeStep(StepExecution stepExecution) {
        String stepName = stepExecution.getStepName();
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        
        log.debug("Starting step: {} in job: {}", stepName, jobName);
        
        // Add step start time for performance tracking
        stepExecution.getExecutionContext().putLong("stepStartTime", System.currentTimeMillis());
        
        // Special handling for idempotency check step
        if ("idempotencyCheckStep".equals(stepName)) {
            log.info("Executing idempotency check step for job: {}", jobName);
            
            // Add idempotency-specific context
            stepExecution.getExecutionContext().putString("stepType", "IDEMPOTENCY_CHECK");
            stepExecution.getExecutionContext().putString("frameworkVersion", "1.0");
            
            if (idempotencyProperties.getDevelopment().isVerboseLogging()) {
                log.debug("Idempotency properties: {}", idempotencyProperties.getConfigurationSummary());
            }
        } else {
            stepExecution.getExecutionContext().putString("stepType", "BUSINESS_LOGIC");
        }
    }
    
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String stepName = stepExecution.getStepName();
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        ExitStatus exitStatus = stepExecution.getExitStatus();
        
        // Calculate step duration
        long startTime = stepExecution.getExecutionContext().getLong("stepStartTime", 0L);
        long duration = startTime > 0 ? System.currentTimeMillis() - startTime : 0L;
        
        log.debug("Completed step: {} in job: {} (Status: {}, Duration: {}ms)", 
                stepName, jobName, exitStatus.getExitCode(), duration);
        
        // Special handling for idempotency check step
        if ("idempotencyCheckStep".equals(stepName)) {
            return handleIdempotencyCheckStepCompletion(stepExecution, exitStatus, duration);
        }
        
        // Handle other steps
        return handleRegularStepCompletion(stepExecution, exitStatus, duration);
    }
    
    /**
     * Handles completion of the idempotency check step.
     */
    private ExitStatus handleIdempotencyCheckStepCompletion(StepExecution stepExecution, 
                                                          ExitStatus exitStatus, long duration) {
        
        String exitCode = exitStatus.getExitCode();
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        
        log.info("Idempotency check step completed for job: {} with exit code: {} (Duration: {}ms)",
                jobName, exitCode, duration);
        
        // Extract idempotency information from execution context
        String idempotencyKey = stepExecution.getExecutionContext().getString(
                IdempotencyCheckTasklet.CONTEXT_IDEMPOTENCY_KEY);
        String correlationId = stepExecution.getExecutionContext().getString(
                IdempotencyCheckTasklet.CONTEXT_CORRELATION_ID);
        String isDuplicate = stepExecution.getExecutionContext().getString(
                IdempotencyCheckTasklet.CONTEXT_IS_DUPLICATE);
        String isRetry = stepExecution.getExecutionContext().getString(
                IdempotencyCheckTasklet.CONTEXT_IS_RETRY);
        
        // Log idempotency details
        if (idempotencyKey != null) {
            log.info("Idempotency details - Key: {}, Correlation: {}, Duplicate: {}, Retry: {}",
                    idempotencyKey, correlationId, isDuplicate, isRetry);
        }
        
        // Handle different exit codes with appropriate messaging and actions
        switch (exitCode) {
            case IdempotencyCheckTasklet.EXIT_STATUS_DUPLICATE:
                log.info("DUPLICATE JOB DETECTED: Job '{}' has already been completed successfully. " +
                        "Execution will be stopped to prevent duplicate processing.", jobName);
                
                // Mark the entire job execution with duplicate status
                stepExecution.getJobExecution().setExitStatus(new ExitStatus(
                        IdempotencyCheckTasklet.EXIT_STATUS_DUPLICATE,
                        "Job already completed - duplicate execution prevented"));
                
                return new ExitStatus(IdempotencyCheckTasklet.EXIT_STATUS_DUPLICATE,
                        "Duplicate job execution detected and prevented");
            
            case IdempotencyCheckTasklet.EXIT_STATUS_IN_PROGRESS:
                log.warn("CONCURRENT JOB DETECTED: Job '{}' is currently being processed elsewhere. " +
                        "This execution will be stopped to prevent concurrent processing.", jobName);
                
                stepExecution.getJobExecution().setExitStatus(new ExitStatus(
                        IdempotencyCheckTasklet.EXIT_STATUS_IN_PROGRESS,
                        "Job is currently being processed elsewhere"));
                
                return new ExitStatus(IdempotencyCheckTasklet.EXIT_STATUS_IN_PROGRESS,
                        "Concurrent job execution detected and prevented");
            
            case IdempotencyCheckTasklet.EXIT_STATUS_MAX_RETRIES:
                log.error("MAX RETRIES EXCEEDED: Job '{}' has exceeded the maximum number of retry attempts. " +
                        "Manual intervention may be required.", jobName);
                
                stepExecution.getJobExecution().setExitStatus(new ExitStatus(
                        IdempotencyCheckTasklet.EXIT_STATUS_MAX_RETRIES,
                        "Maximum retry attempts exceeded"));
                
                return new ExitStatus(IdempotencyCheckTasklet.EXIT_STATUS_MAX_RETRIES,
                        "Maximum retry attempts exceeded");
            
            case IdempotencyCheckTasklet.EXIT_STATUS_PROCEED:
                if ("true".equals(isRetry)) {
                    Integer retryCount = (Integer) stepExecution.getExecutionContext().get(
                            IdempotencyCheckTasklet.CONTEXT_RETRY_COUNT);
                    log.info("RETRY EXECUTION AUTHORIZED: Job '{}' is being retried (attempt {}).", 
                            jobName, retryCount != null ? retryCount : "unknown");
                } else {
                    log.info("NEW EXECUTION AUTHORIZED: Job '{}' is proceeding with new execution.", jobName);
                }
                
                return new ExitStatus(ExitStatus.COMPLETED.getExitCode(),
                        "Idempotency check passed - proceeding with job execution");
            
            case ExitStatus.FAILED.getExitCode():
                log.error("IDEMPOTENCY CHECK FAILED: Job '{}' failed during idempotency validation. " +
                        "Check logs for detailed error information.", jobName);
                
                return new ExitStatus(ExitStatus.FAILED.getExitCode(),
                        "Idempotency check failed - " + exitStatus.getExitDescription());
            
            default:
                log.warn("UNKNOWN EXIT CODE: Job '{}' completed idempotency check with unknown exit code: {}",
                        jobName, exitCode);
                
                return exitStatus;
        }
    }
    
    /**
     * Handles completion of regular (non-idempotency) steps.
     */
    private ExitStatus handleRegularStepCompletion(StepExecution stepExecution, 
                                                 ExitStatus exitStatus, long duration) {
        
        String stepName = stepExecution.getStepName();
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        
        // Check for performance issues
        long warningThreshold = idempotencyProperties.getPerformance().getWarningThresholdMs();
        if (duration > warningThreshold) {
            log.warn("PERFORMANCE WARNING: Step '{}' in job '{}' took {}ms (threshold: {}ms)",
                    stepName, jobName, duration, warningThreshold);
        }
        
        // Log step statistics if available
        if (stepExecution.getReadCount() > 0 || stepExecution.getWriteCount() > 0) {
            log.debug("Step statistics - Read: {}, Write: {}, Skip: {}, Duration: {}ms",
                    stepExecution.getReadCount(), stepExecution.getWriteCount(),
                    stepExecution.getSkipCount(), duration);
        }
        
        // Check if this is an idempotent job execution
        String idempotencyKey = stepExecution.getJobExecution().getExecutionContext().getString(
                IdempotencyCheckTasklet.CONTEXT_IDEMPOTENCY_KEY);
        
        if (idempotencyKey != null && idempotencyProperties.getDevelopment().isVerboseLogging()) {
            log.debug("Step '{}' completed in idempotent job execution (key: {})", stepName, idempotencyKey);
        }
        
        // Publish step completion events if configured
        if (idempotencyProperties.getEvents().isPublishJobEvents()) {
            publishStepCompletionEvent(stepExecution, duration);
        }
        
        return exitStatus;
    }
    
    /**
     * Publishes step completion event for monitoring.
     */
    private void publishStepCompletionEvent(StepExecution stepExecution, long duration) {
        try {
            // Create step completion event (placeholder for actual event system)
            log.debug("Publishing step completion event for: {} (Duration: {}ms)",
                    stepExecution.getStepName(), duration);
            
            // Actual event publishing would be implemented here
            
        } catch (Exception e) {
            log.warn("Failed to publish step completion event for step: {} - {}",
                    stepExecution.getStepName(), e.getMessage());
        }
    }
}