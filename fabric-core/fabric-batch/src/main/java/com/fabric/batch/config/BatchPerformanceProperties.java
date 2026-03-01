package com.fabric.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Centralized performance tuning knobs for batch processing.
 * All defaults match current hardcoded values for zero-regression deployment.
 *
 * Configure via application.yml under 'batch.performance.*'
 */
@Data
@ConfigurationProperties(prefix = "batch.performance")
public class BatchPerformanceProperties {

    /** Records per transaction commit. Current default: 100. */
    private int chunkSize = 100;

    /** Number of parallel partitions. Current default: 4. */
    private int gridSize = 4;

    /** Thread pool configuration for partition execution. */
    private ThreadPool threadPool = new ThreadPool();

    /** SQL*Loader performance settings. */
    private Sqlldr sqlldr = new Sqlldr();

    /** JFR profiling settings for benchmark runs. */
    private Profiling profiling = new Profiling();

    @Data
    public static class ThreadPool {
        private int coreSize = 4;
        private int maxSize = 10;
        private int queueCapacity = 100;
        private String threadNamePrefix = "batch-";
        private int awaitTerminationSeconds = 60;
    }

    @Data
    public static class Sqlldr {
        private int parallelDegree = 1;
        private int bindSize = 256000;
        private int readSize = 1048576;
        private long timeoutMinutes = 60;
        private int maxRetries = 3;
        private int retryDelaySeconds = 10;
    }

    @Data
    public static class Profiling {
        private boolean jfrEnabled = false;
        private String jfrDuration = "300s";
        private String jfrOutputPath = "./data/benchmark/jfr/";
    }
}
