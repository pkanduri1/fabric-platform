package com.truist.batch.launcher;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.truist.batch.context.SourceContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Standalone batch launcher for CAESP integration.
 * Executes batch jobs from command line with sourceSystem and jobName parameters.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StandaloneBatchLauncher implements ApplicationRunner {

    private final JobLauncher jobLauncher;
    private final Job genericJob;

    @Value("${sourceSystem:}")
    private String sourceSystem;

    @Value("${jobName:}")
    private String jobName;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (sourceSystem.isEmpty() || jobName.isEmpty()) {
            log.info("No sourceSystem or jobName provided. Skipping batch job execution.");
            return;
        }

        try {
            // Set the source context for database-driven property resolution
            SourceContext.setCurrentSource(sourceSystem);
            log.info("🎯 Source context set to: {}", sourceSystem);

            JobParameters jobParameters = new JobParametersBuilder()
                .addString("sourceSystem", sourceSystem)
                .addString("jobName", jobName)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

            log.info("🚀 STARTING BATCH JOB: sourceSystem={}, jobName={}", sourceSystem, jobName);
            log.info("Job parameters: {}", jobParameters);

            JobExecution jobExecution = jobLauncher.run(genericJob, jobParameters);

            log.info("✅ BATCH JOB COMPLETED: status={}", jobExecution.getStatus());

        } catch (Exception e) {
            log.error("❌ BATCH JOB FAILED", e);
            throw e;
        } finally {
            // Always clear the source context to prevent memory leaks
            SourceContext.clear();
            log.info("🧹 Source context cleared");
            log.info("🛑 Forcing application shutdown...");
            Thread.sleep(1000);
            System.exit(0);
        }
    }
}
