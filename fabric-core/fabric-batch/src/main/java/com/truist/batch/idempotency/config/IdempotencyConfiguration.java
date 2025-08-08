package com.truist.batch.idempotency.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truist.batch.idempotency.service.IdempotencyService;
import com.truist.batch.idempotency.tasklet.IdempotencyCheckTasklet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Boot configuration for the Fabric Platform Idempotency Framework.
 * Provides comprehensive configuration for idempotent batch job processing
 * with enterprise-grade features including caching, monitoring, and audit trails.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Configuration
@EnableBatchProcessing
@EnableCaching
@EnableConfigurationProperties(IdempotencyProperties.class)
@ComponentScan(basePackages = {
    "com.truist.batch.idempotency.service",
    "com.truist.batch.idempotency.repository",
    "com.truist.batch.idempotency.tasklet"
})
@ConditionalOnProperty(name = "fabric.idempotency.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class IdempotencyConfiguration {
    
    private final IdempotencyProperties idempotencyProperties;
    
    public IdempotencyConfiguration(IdempotencyProperties idempotencyProperties) {
        this.idempotencyProperties = idempotencyProperties;
        log.info("Initializing Fabric Platform Idempotency Framework with properties: {}", 
                idempotencyProperties);
    }
    
    /**
     * Enhanced job with integrated idempotency checking.
     * This job template can be extended by specific batch jobs.
     */
    @Bean
    public Job idempotentJobTemplate(JobRepository jobRepository,
                                   IdempotencyCheckTasklet idempotencyCheckTasklet,
                                   PlatformTransactionManager transactionManager) {
        
        log.info("Creating idempotent job template with idempotency framework integration");
        
        return new JobBuilder("idempotentJobTemplate", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(createIdempotentJobExecutionListener())
                .start(createIdempotencyCheckStep(jobRepository, idempotencyCheckTasklet, transactionManager))
                // Additional steps can be chained here by extending jobs
                .build();
    }
    
    /**
     * Idempotency check step that runs before main processing.
     */
    @Bean
    public Step idempotencyCheckStep(JobRepository jobRepository,
                                   IdempotencyCheckTasklet idempotencyCheckTasklet,
                                   PlatformTransactionManager transactionManager) {
        return createIdempotencyCheckStep(jobRepository, idempotencyCheckTasklet, transactionManager);
    }
    
    /**
     * Creates the idempotency check step.
     */
    private Step createIdempotencyCheckStep(JobRepository jobRepository,
                                          IdempotencyCheckTasklet idempotencyCheckTasklet,
                                          PlatformTransactionManager transactionManager) {
        
        return new StepBuilder("idempotencyCheckStep", jobRepository)
                .tasklet(idempotencyCheckTasklet, transactionManager)
                .listener(createIdempotentStepExecutionListener())
                .build();
    }
    
    /**
     * Job execution listener with idempotency support.
     */
    @Bean
    public JobExecutionListener idempotentJobExecutionListener(IdempotencyService idempotencyService) {
        return new IdempotentJobExecutionListener(idempotencyService, idempotencyProperties);
    }
    
    /**
     * Step execution listener with idempotency support.
     */
    @Bean
    public StepExecutionListener idempotentStepExecutionListener() {
        return new IdempotentStepExecutionListener(idempotencyProperties);
    }
    
    /**
     * ObjectMapper bean for JSON serialization/deserialization.
     * Configured with settings optimized for idempotency use cases.
     */
    @Bean
    public ObjectMapper idempotencyObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        
        // Configure for consistent serialization
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        log.debug("Configured ObjectMapper for idempotency framework");
        return mapper;
    }
    
    /**
     * Creates job execution listener for idempotency support.
     */
    private JobExecutionListener createIdempotentJobExecutionListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(org.springframework.batch.core.JobExecution jobExecution) {
                log.info("Starting idempotent job execution: {} (ID: {})",
                        jobExecution.getJobInstance().getJobName(),
                        jobExecution.getId());
                
                // Add idempotency metadata to job execution context
                jobExecution.getExecutionContext().putString("idempotencyFrameworkVersion", "1.0");
                jobExecution.getExecutionContext().putString("idempotencyEnabled", "true");
                jobExecution.getExecutionContext().putLong("jobStartTime", System.currentTimeMillis());
            }
            
            @Override
            public void afterJob(org.springframework.batch.core.JobExecution jobExecution) {
                long startTime = jobExecution.getExecutionContext().getLong("jobStartTime", 0L);
                long duration = startTime > 0 ? System.currentTimeMillis() - startTime : 0L;
                
                log.info("Completed idempotent job execution: {} (ID: {}, Status: {}, Duration: {}ms)",
                        jobExecution.getJobInstance().getJobName(),
                        jobExecution.getId(),
                        jobExecution.getStatus(),
                        duration);
                
                // Log idempotency information if available
                String idempotencyKey = jobExecution.getExecutionContext().getString(
                        IdempotencyCheckTasklet.CONTEXT_IDEMPOTENCY_KEY);
                String correlationId = jobExecution.getExecutionContext().getString(
                        IdempotencyCheckTasklet.CONTEXT_CORRELATION_ID);
                
                if (idempotencyKey != null) {
                    log.info("Job execution idempotency tracking - Key: {}, Correlation: {}", 
                            idempotencyKey, correlationId);
                }
                
                // Publish completion event if configured
                if (idempotencyProperties.getEvents().isPublishJobEvents()) {
                    publishJobCompletionEvent(jobExecution, duration);
                }
            }
        };
    }
    
    /**
     * Creates step execution listener for idempotency support.
     */
    private StepExecutionListener createIdempotentStepExecutionListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(org.springframework.batch.core.StepExecution stepExecution) {
                if ("idempotencyCheckStep".equals(stepExecution.getStepName())) {
                    log.debug("Executing idempotency check step for job: {}",
                            stepExecution.getJobExecution().getJobInstance().getJobName());
                }
            }
            
            @Override
            public org.springframework.batch.core.ExitStatus afterStep(org.springframework.batch.core.StepExecution stepExecution) {
                if ("idempotencyCheckStep".equals(stepExecution.getStepName())) {
                    String exitCode = stepExecution.getExitStatus().getExitCode();
                    log.info("Idempotency check step completed with exit code: {}", exitCode);
                    
                    // Handle different exit codes
                    switch (exitCode) {
                        case IdempotencyCheckTasklet.EXIT_STATUS_DUPLICATE:
                            log.info("Duplicate job detected - stopping execution");
                            return new org.springframework.batch.core.ExitStatus(
                                    IdempotencyCheckTasklet.EXIT_STATUS_DUPLICATE, 
                                    "Job already completed successfully");
                        
                        case IdempotencyCheckTasklet.EXIT_STATUS_IN_PROGRESS:
                            log.warn("Job already in progress - stopping execution");
                            return new org.springframework.batch.core.ExitStatus(
                                    IdempotencyCheckTasklet.EXIT_STATUS_IN_PROGRESS,
                                    "Job is currently being processed elsewhere");
                        
                        case IdempotencyCheckTasklet.EXIT_STATUS_MAX_RETRIES:
                            log.error("Maximum retries exceeded - stopping execution");
                            return new org.springframework.batch.core.ExitStatus(
                                    IdempotencyCheckTasklet.EXIT_STATUS_MAX_RETRIES,
                                    "Maximum retry attempts exceeded");
                        
                        case IdempotencyCheckTasklet.EXIT_STATUS_PROCEED:
                            log.info("Idempotency check passed - proceeding with job execution");
                            break;
                    }
                }
                
                return stepExecution.getExitStatus();
            }
        };
    }
    
    /**
     * Publishes job completion event for monitoring.
     */
    private void publishJobCompletionEvent(org.springframework.batch.core.JobExecution jobExecution, long duration) {
        try {
            // Create and publish event (implementation would depend on event system)
            log.debug("Publishing job completion event for: {} (Duration: {}ms)", 
                    jobExecution.getJobInstance().getJobName(), duration);
            
            // Event publishing logic would go here
            
        } catch (Exception e) {
            log.warn("Failed to publish job completion event", e);
        }
    }
    
    /**
     * Cache configuration for idempotency lookups.
     */
    @Configuration
    @ConditionalOnProperty(name = "fabric.idempotency.cache.enabled", havingValue = "true", matchIfMissing = true)
    static class IdempotencyCacheConfiguration {
        
        @Bean
        public org.springframework.cache.CacheManager idempotencyCacheManager() {
            org.springframework.cache.concurrent.ConcurrentMapCacheManager cacheManager = 
                    new org.springframework.cache.concurrent.ConcurrentMapCacheManager();
            
            // Add cache for idempotency configurations
            cacheManager.setCacheNames(java.util.Arrays.asList("idempotencyConfig"));
            cacheManager.setAllowNullValues(false);
            
            log.info("Configured cache manager for idempotency framework");
            return cacheManager;
        }
    }
    
    /**
     * Metrics and monitoring configuration.
     */
    @Configuration
    @ConditionalOnProperty(name = "fabric.idempotency.metrics.enabled", havingValue = "true", matchIfMissing = true)
    static class IdempotencyMetricsConfiguration {
        
        // Placeholder for metrics configuration
        // Could integrate with Micrometer, Prometheus, etc.
        
        @Bean
        public IdempotencyMetricsCollector idempotencyMetricsCollector(IdempotencyProperties properties) {
            return new IdempotencyMetricsCollector(properties);
        }
    }
    
    /**
     * Development and testing configuration.
     */
    @Configuration
    @ConditionalOnProperty(name = "fabric.idempotency.dev-mode", havingValue = "true")
    static class IdempotencyDevelopmentConfiguration {
        
        public IdempotencyDevelopmentConfiguration() {
            log.warn("Idempotency Framework running in DEVELOPMENT MODE - not suitable for production");
        }
        
        // Development-specific beans and configurations
    }
}