package com.fabric.batch.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableConfigurationProperties(BatchPerformanceProperties.class)
@Slf4j
public class TaskExecutorConfig {

    @Autowired
    private Environment environment;

    @Autowired
    private BatchPerformanceProperties perfProps;

    @Bean
    public TaskExecutor taskExecutor() {
        if (isDebugMode()) {
            log.info("DEBUG MODE: Using SyncTaskExecutor (truly single-threaded)");
            return new SyncTaskExecutor();
        } else {
            var tp = perfProps.getThreadPool();
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(tp.getCoreSize());
            executor.setMaxPoolSize(tp.getMaxSize());
            executor.setQueueCapacity(tp.getQueueCapacity());
            executor.setThreadNamePrefix(tp.getThreadNamePrefix());
            executor.setWaitForTasksToCompleteOnShutdown(true);
            executor.setAwaitTerminationSeconds(tp.getAwaitTerminationSeconds());
            executor.initialize();
            log.info("PRODUCTION MODE: ThreadPoolTaskExecutor core={} max={} queue={}",
                tp.getCoreSize(), tp.getMaxSize(), tp.getQueueCapacity());
            return executor;
        }
    }

    private boolean isDebugMode() {
        boolean debugJVM = java.lang.management.ManagementFactory.getRuntimeMXBean()
            .getInputArguments().toString().contains("-agentlib:jdwp");
        boolean debugProp = "true".equals(System.getProperty("debug.batch"));
        boolean debugEnv = "true".equals(System.getenv("DEBUG_BATCH"));
        boolean debugProfile = java.util.Arrays.asList(environment.getActiveProfiles()).contains("debug");
        boolean debugConfig = environment.getProperty("batch.debug.enabled", Boolean.class, false);
        boolean singleThreaded = perfProps.getGridSize() == 1;
        return debugJVM || debugProp || debugEnv || debugProfile || debugConfig || singleThreaded;
    }
}