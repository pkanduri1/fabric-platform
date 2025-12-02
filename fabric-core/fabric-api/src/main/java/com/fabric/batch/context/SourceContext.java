package com.fabric.batch.context;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local context holder for source system identification.
 *
 * This utility class provides thread-safe storage and retrieval of the current
 * source system code. It is used in conjunction with DatabasePropertySource to
 * enable source-specific property resolution.
 *
 * Architecture:
 * - Uses ThreadLocal to ensure thread safety in multi-threaded environments
 * - Each thread maintains its own source context
 * - Context is propagated through the request/job execution lifecycle
 * - Must be explicitly cleared to prevent memory leaks
 *
 * Usage Pattern:
 * <pre>
 * try {
 *     // Set source context at the beginning of job execution
 *     SourceContext.setCurrentSource("HR");
 *
 *     // Execute job logic
 *     // All property resolutions will use HR-specific values
 *     jobExecutionService.execute(jobConfig);
 *
 * } finally {
 *     // Always clear context in finally block
 *     SourceContext.clear();
 * }
 * </pre>
 *
 * Integration with DatabasePropertySource:
 * When a property is requested (e.g., ${batch.defaults.outputBasePath}):
 * 1. DatabasePropertySource calls SourceContext.getCurrentSource()
 * 2. If source is set (e.g., "HR"), it retrieves HR-specific value
 * 3. If no source is set, it retrieves global default value
 *
 * Thread Safety:
 * This class is thread-safe through the use of ThreadLocal. Each thread
 * has its own independent source context that does not interfere with
 * other threads.
 *
 * Memory Management:
 * It is CRITICAL to call clear() when done with the context, especially
 * in thread pool environments. Failure to clear the context can lead to:
 * - Memory leaks in application servers
 * - Incorrect source context in subsequent requests
 * - Thread pool pollution
 *
 * Best Practices:
 * 1. Always use try-finally pattern
 * 2. Set context as early as possible in the execution flow
 * 3. Clear context in finally block
 * 4. Consider using AOP or interceptors for automatic context management
 *
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Phase 2 - Database-Driven Source Configuration System
 */
@Slf4j
public class SourceContext {

    /**
     * ThreadLocal storage for current source system code.
     * Each thread has its own independent copy of this variable.
     */
    private static final ThreadLocal<String> currentSource = new ThreadLocal<>();

    /**
     * ThreadLocal storage for source context metadata.
     * Stores additional context information like job ID, execution ID, etc.
     */
    private static final ThreadLocal<SourceContextMetadata> contextMetadata = new ThreadLocal<>();

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private SourceContext() {
        throw new UnsupportedOperationException("SourceContext is a utility class and cannot be instantiated");
    }

    /**
     * Set the current source system code for this thread.
     *
     * This method should be called at the beginning of job execution or
     * request processing to establish the source context.
     *
     * @param sourceCode the source system code (e.g., "HR", "ENCORE", "PAYROLL")
     * @throws IllegalArgumentException if sourceCode is null or empty
     */
    public static void setCurrentSource(String sourceCode) {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Source code cannot be null or empty");
        }

        String normalizedSourceCode = sourceCode.trim().toUpperCase();
        currentSource.set(normalizedSourceCode);

        log.debug("Source context set to: {} for thread: {}",
            normalizedSourceCode, Thread.currentThread().getName());
    }

    /**
     * Set the current source system code with additional metadata.
     *
     * This method allows setting both the source code and associated metadata
     * in a single call.
     *
     * @param sourceCode the source system code
     * @param metadata additional context metadata
     * @throws IllegalArgumentException if sourceCode is null or empty
     */
    public static void setCurrentSource(String sourceCode, SourceContextMetadata metadata) {
        setCurrentSource(sourceCode);
        contextMetadata.set(metadata);

        log.debug("Source context set to: {} with metadata for thread: {}",
            sourceCode, Thread.currentThread().getName());
    }

    /**
     * Get the current source system code for this thread.
     *
     * This method is called by DatabasePropertySource to determine which
     * source-specific configuration to use.
     *
     * @return the current source code, or null if not set
     */
    public static String getCurrentSource() {
        return currentSource.get();
    }

    /**
     * Get the current source context metadata for this thread.
     *
     * @return the current metadata, or null if not set
     */
    public static SourceContextMetadata getContextMetadata() {
        return contextMetadata.get();
    }

    /**
     * Check if a source context is currently set for this thread.
     *
     * @return true if source context is set, false otherwise
     */
    public static boolean isSourceContextSet() {
        return currentSource.get() != null;
    }

    /**
     * Clear the source context for this thread.
     *
     * This method MUST be called when done with the source context to prevent
     * memory leaks and context pollution in thread pools.
     *
     * Always call this in a finally block to ensure cleanup happens even
     * if exceptions occur.
     */
    public static void clear() {
        String previousSource = currentSource.get();
        currentSource.remove();
        contextMetadata.remove();

        if (previousSource != null) {
            log.debug("Source context cleared (was: {}) for thread: {}",
                previousSource, Thread.currentThread().getName());
        }
    }

    /**
     * Execute a runnable with a specific source context.
     *
     * This is a convenience method that automatically manages the source context
     * lifecycle using try-finally pattern.
     *
     * @param sourceCode the source system code
     * @param runnable the code to execute with the source context
     * @throws IllegalArgumentException if sourceCode is null or empty
     */
    public static void executeWithSource(String sourceCode, Runnable runnable) {
        try {
            setCurrentSource(sourceCode);
            runnable.run();
        } finally {
            clear();
        }
    }

    /**
     * Execute a runnable with a specific source context and metadata.
     *
     * This is a convenience method that automatically manages the source context
     * and metadata lifecycle using try-finally pattern.
     *
     * @param sourceCode the source system code
     * @param metadata additional context metadata
     * @param runnable the code to execute with the source context
     * @throws IllegalArgumentException if sourceCode is null or empty
     */
    public static void executeWithSource(String sourceCode, SourceContextMetadata metadata, Runnable runnable) {
        try {
            setCurrentSource(sourceCode, metadata);
            runnable.run();
        } finally {
            clear();
        }
    }

    /**
     * Get a formatted string representation of the current context.
     * Useful for logging and debugging.
     *
     * @return formatted context string
     */
    public static String getContextInfo() {
        String source = getCurrentSource();
        SourceContextMetadata metadata = getContextMetadata();

        if (source == null) {
            return "No source context set";
        }

        if (metadata != null) {
            return String.format("Source: %s, %s", source, metadata);
        }

        return String.format("Source: %s", source);
    }

    /**
     * Metadata holder for source context.
     * Stores additional information about the execution context.
     */
    public static class SourceContextMetadata {
        private final String jobId;
        private final String executionId;
        private final String batchDate;

        public SourceContextMetadata(String jobId, String executionId, String batchDate) {
            this.jobId = jobId;
            this.executionId = executionId;
            this.batchDate = batchDate;
        }

        public String getJobId() {
            return jobId;
        }

        public String getExecutionId() {
            return executionId;
        }

        public String getBatchDate() {
            return batchDate;
        }

        @Override
        public String toString() {
            return String.format("JobId: %s, ExecutionId: %s, BatchDate: %s",
                jobId, executionId, batchDate);
        }
    }
}
