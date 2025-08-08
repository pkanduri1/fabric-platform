package com.truist.batch.sqlloader;

import com.truist.batch.entity.ProcessingJobEntity;
import com.truist.batch.entity.JobExecutionLogEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executor for SQL*Loader operations with comprehensive error handling and monitoring.
 * Provides secure execution of SQL*Loader with detailed logging and performance tracking.
 */
@Slf4j
@Component
public class SqlLoaderExecutor {
    
    @Value("${sqlloader.executable.path:sqlldr}")
    private String sqlLoaderExecutablePath;
    
    @Value("${sqlloader.timeout.minutes:60}")
    private long timeoutMinutes;
    
    @Value("${sqlloader.work.directory:${java.io.tmpdir}}")
    private String workDirectory;
    
    @Value("${sqlloader.max.retries:3}")
    private int maxRetries;
    
    @Value("${sqlloader.retry.delay.seconds:10}")
    private int retryDelaySeconds;
    
    private static final Pattern RECORD_COUNT_PATTERN = Pattern.compile("Total logical records skipped:\\s*(\\d+)");
    private static final Pattern ERROR_COUNT_PATTERN = Pattern.compile("Total logical records rejected:\\s*(\\d+)");
    private static final Pattern LOAD_COUNT_PATTERN = Pattern.compile("Total logical records read:\\s*(\\d+)");
    
    /**
     * Execute data loading with configuration, file name, and file path.
     * 
     * @param config Data loading configuration entity
     * @param fileName Name of the file to load
     * @param filePath Path to the file to load
     * @return SQL*Loader execution result
     */
    public SqlLoaderResult executeLoad(com.truist.batch.entity.DataLoadConfigEntity config, String fileName, String filePath) {
        log.info("Starting data load execution for config: {}, file: {}", config.getConfigId(), fileName);
        
        // Convert DataLoadConfigEntity to SqlLoaderConfig
        SqlLoaderConfig sqlConfig = convertToSqlLoaderConfig(config, fileName, filePath);
        
        // Create a temporary processing job for tracking
        ProcessingJobEntity processingJob = new ProcessingJobEntity();
        processingJob.setJobExecutionId(java.util.UUID.randomUUID().toString());
        processingJob.setConfigId(config.getConfigId());
        processingJob.setFileName(fileName);
        
        return executeSqlLoader(sqlConfig, processingJob);
    }
    
    /**
     * Convert DataLoadConfigEntity to SqlLoaderConfig.
     */
    private SqlLoaderConfig convertToSqlLoaderConfig(com.truist.batch.entity.DataLoadConfigEntity config, String fileName, String filePath) {
        return SqlLoaderConfig.builder()
                .jobName(config.getJobName())
                .correlationId(java.util.UUID.randomUUID().toString())
                .dataFileName(filePath)
                .targetTable(config.getTargetTable())
                .fieldDelimiter(config.getFieldDelimiter())
                .characterSet("UTF8")
                .build();
    }

    /**
     * Execute SQL*Loader with the provided configuration.
     * 
     * @param config SQL*Loader configuration
     * @param processingJob Processing job entity for tracking
     * @return SQL*Loader execution result
     */
    public SqlLoaderResult executeSqlLoader(SqlLoaderConfig config, ProcessingJobEntity processingJob) {
        log.info("Starting SQL*Loader execution for job: {}", config.getJobName());
        
        SqlLoaderResult result = new SqlLoaderResult();
        result.setJobExecutionId(processingJob.getJobExecutionId());
        result.setCorrelationId(config.getCorrelationId());
        result.setStartTime(LocalDateTime.now());
        
        try {
            // Generate control file
            ControlFileGenerator generator = new ControlFileGenerator();
            Path controlFile = generator.generateControlFile(config);
            result.setControlFilePath(controlFile.toString());
            
            // Execute SQL*Loader with retries
            int attempt = 0;
            boolean success = false;
            
            while (attempt < maxRetries && !success) {
                attempt++;
                log.info("SQL*Loader execution attempt {} of {} for job: {}", attempt, maxRetries, config.getJobName());
                
                try {
                    success = executeSqlLoaderCommand(config, controlFile, result, attempt);
                } catch (Exception e) {
                    log.error("SQL*Loader execution attempt {} failed for job: {}", attempt, config.getJobName(), e);
                    result.addError("Execution attempt " + attempt + " failed: " + e.getMessage());
                    
                    if (attempt < maxRetries) {
                        log.info("Retrying SQL*Loader execution in {} seconds...", retryDelaySeconds);
                        Thread.sleep(retryDelaySeconds * 1000L);
                    }
                }
            }
            
            result.setEndTime(LocalDateTime.now());
            result.setDurationMs(java.time.Duration.between(result.getStartTime(), result.getEndTime()).toMillis());
            result.setSuccessful(success);
            result.setRetryCount(attempt - 1);
            
            // Parse log file for statistics
            if (result.getLogFilePath() != null) {
                parseLogFile(result);
            }
            
            log.info("SQL*Loader execution completed for job: {} - Success: {}, Duration: {}ms", 
                    config.getJobName(), success, result.getDurationMs());
            
        } catch (Exception e) {
            log.error("Fatal error during SQL*Loader execution for job: {}", config.getJobName(), e);
            result.setSuccessful(false);
            result.setEndTime(LocalDateTime.now());
            result.addError("Fatal execution error: " + e.getMessage());
            result.setErrorDetails(getStackTrace(e));
        }
        
        return result;
    }
    
    /**
     * Execute the actual SQL*Loader command.
     */
    private boolean executeSqlLoaderCommand(SqlLoaderConfig config, Path controlFile, SqlLoaderResult result, int attempt) 
            throws IOException, InterruptedException {
        
        // Build command
        List<String> command = buildSqlLoaderCommand(config, controlFile);
        log.debug("Executing SQL*Loader command: {}", String.join(" ", command));
        
        // Set up process builder
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(workDirectory));
        processBuilder.redirectErrorStream(true);
        
        // Set environment variables
        setupEnvironmentVariables(processBuilder, config);
        
        // Execute process
        Process process = processBuilder.start();
        
        // Capture output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("SQL*Loader output: {}", line);
            }
        }
        
        // Wait for completion with timeout
        boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
        
        if (!finished) {
            log.error("SQL*Loader execution timed out after {} minutes", timeoutMinutes);
            process.destroyForcibly();
            result.addError("Execution timed out after " + timeoutMinutes + " minutes");
            return false;
        }
        
        int exitCode = process.exitValue();
        result.setReturnCode(exitCode);
        result.setExecutionOutput(output.toString());
        
        // Set file paths based on control file location
        String baseFileName = controlFile.toString().replaceAll("\\.ctl$", "");
        result.setLogFilePath(baseFileName + ".log");
        result.setBadFilePath(baseFileName + ".bad");
        result.setDiscardFilePath(baseFileName + ".dsc");
        
        log.info("SQL*Loader completed with exit code: {} for job: {}", exitCode, config.getJobName());
        
        // Analyze exit code
        return analyzeSqlLoaderExitCode(exitCode, result);
    }
    
    /**
     * Build the SQL*Loader command with all parameters.
     */
    private List<String> buildSqlLoaderCommand(SqlLoaderConfig config, Path controlFile) {
        List<String> command = new ArrayList<>();
        
        command.add(sqlLoaderExecutablePath);
        
        // User credential (should be handled securely)
        if (config.getUserid() != null) {
            command.add(config.getUserid());
        }
        
        // Control file
        command.add("CONTROL=" + controlFile.toString());
        
        // Log file
        String logFile = controlFile.toString().replaceAll("\\.ctl$", ".log");
        command.add("LOG=" + logFile);
        
        // Bad file
        if (config.getBadFileName() != null) {
            command.add("BAD=" + config.getBadFileName());
        }
        
        // Discard file
        if (config.getDiscardFileName() != null) {
            command.add("DISCARD=" + config.getDiscardFileName());
        }
        
        // Add command line options
        String options = config.getCommandLineOptions();
        if (options != null && !options.trim().isEmpty()) {
            command.addAll(Arrays.asList(options.split("\\s+")));
        }
        
        // Parallel degree
        if (config.getParallelDegree() != null && config.getParallelDegree() > 1) {
            command.add("PARALLEL=TRUE");
            command.add("DEGREE=" + config.getParallelDegree());
        }
        
        return command;
    }
    
    /**
     * Setup environment variables for SQL*Loader execution.
     */
    private void setupEnvironmentVariables(ProcessBuilder processBuilder, SqlLoaderConfig config) {
        // Set Oracle environment variables if needed
        if (config.getExecutionEnvironment() != null) {
            processBuilder.environment().put("ORACLE_SID", config.getExecutionEnvironment());
        }
        
        // Set character set
        if (config.getCharacterSet() != null) {
            processBuilder.environment().put("NLS_LANG", "AMERICAN_AMERICA." + config.getCharacterSet());
        }
        
        // Set timezone if specified
        processBuilder.environment().put("TZ", "UTC");
        
        // Security: Clear potentially sensitive environment variables
        processBuilder.environment().remove("ORACLE_PASSWORD");
        processBuilder.environment().remove("TWO_TASK");
    }
    
    /**
     * Analyze SQL*Loader exit code and determine success/failure.
     */
    private boolean analyzeSqlLoaderExitCode(int exitCode, SqlLoaderResult result) {
        switch (exitCode) {
            case 0:
                result.setExecutionStatus("SUCCESS");
                return true;
            case 1:
                result.setExecutionStatus("SUCCESS_WITH_WARNINGS");
                result.addWarning("SQL*Loader completed with warnings");
                return true; // Consider warnings as success
            case 2:
                result.setExecutionStatus("FAILED");
                result.addError("All or some rows rejected");
                return false;
            case 3:
                result.setExecutionStatus("FAILED");
                result.addError("Fatal error occurred");
                return false;
            case 4:
                result.setExecutionStatus("FAILED");
                result.addError("Unable to allocate memory for data structures");
                return false;
            default:
                result.setExecutionStatus("FAILED");
                result.addError("Unknown exit code: " + exitCode);
                return false;
        }
    }
    
    /**
     * Parse SQL*Loader log file to extract statistics.
     */
    private void parseLogFile(SqlLoaderResult result) {
        try {
            Path logFile = Paths.get(result.getLogFilePath());
            if (!Files.exists(logFile)) {
                log.warn("SQL*Loader log file not found: {}", result.getLogFilePath());
                return;
            }
            
            List<String> lines = Files.readAllLines(logFile);
            
            for (String line : lines) {
                // Parse record counts
                Matcher matcher = LOAD_COUNT_PATTERN.matcher(line);
                if (matcher.find()) {
                    result.setTotalRecords(Long.parseLong(matcher.group(1)));
                    continue;
                }
                
                matcher = ERROR_COUNT_PATTERN.matcher(line);
                if (matcher.find()) {
                    result.setRejectedRecords(Long.parseLong(matcher.group(1)));
                    continue;
                }
                
                matcher = RECORD_COUNT_PATTERN.matcher(line);
                if (matcher.find()) {
                    result.setSkippedRecords(Long.parseLong(matcher.group(1)));
                    continue;
                }
                
                // Look for error messages
                if (line.contains("ORA-") || line.contains("SQL*Loader-")) {
                    result.addError(line.trim());
                }
                
                // Look for warnings
                if (line.contains("Warning:") || line.contains("WARNING:")) {
                    result.addWarning(line.trim());
                }
            }
            
            // Calculate successful records
            if (result.getTotalRecords() != null) {
                long successful = result.getTotalRecords();
                if (result.getRejectedRecords() != null) {
                    successful -= result.getRejectedRecords();
                }
                if (result.getSkippedRecords() != null) {
                    successful -= result.getSkippedRecords();
                }
                result.setSuccessfulRecords(Math.max(0, successful));
            }
            
            log.info("Parsed SQL*Loader statistics - Total: {}, Successful: {}, Rejected: {}, Skipped: {}", 
                    result.getTotalRecords(), result.getSuccessfulRecords(), 
                    result.getRejectedRecords(), result.getSkippedRecords());
            
        } catch (Exception e) {
            log.error("Error parsing SQL*Loader log file: {}", result.getLogFilePath(), e);
            result.addWarning("Could not parse log file: " + e.getMessage());
        }
    }
    
    /**
     * Validate SQL*Loader environment and configuration.
     */
    public boolean validateEnvironment() {
        try {
            // Check if SQL*Loader executable exists
            ProcessBuilder pb = new ProcessBuilder(sqlLoaderExecutablePath, "help=yes");
            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                log.error("SQL*Loader validation timed out");
                return false;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0 || exitCode == 1) { // 1 is also acceptable for help command
                log.info("SQL*Loader environment validation successful");
                return true;
            } else {
                log.error("SQL*Loader validation failed with exit code: {}", exitCode);
                return false;
            }
            
        } catch (Exception e) {
            log.error("SQL*Loader environment validation failed", e);
            return false;
        }
    }
    
    /**
     * Clean up temporary files after execution.
     */
    public void cleanupTempFiles(SqlLoaderResult result, boolean keepLogFiles) {
        try {
            // Always keep control files for audit
            
            // Optionally clean up bad and discard files if empty
            cleanupFileIfEmpty(result.getBadFilePath());
            cleanupFileIfEmpty(result.getDiscardFilePath());
            
            // Clean up log files only if requested and no errors
            if (!keepLogFiles && result.isSuccessful() && 
                (result.getRejectedRecords() == null || result.getRejectedRecords() == 0)) {
                cleanupFile(result.getLogFilePath());
            }
            
        } catch (Exception e) {
            log.warn("Error during cleanup of temporary files", e);
        }
    }
    
    private void cleanupFileIfEmpty(String filePath) {
        if (filePath == null) return;
        
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path) && Files.size(path) == 0) {
                Files.delete(path);
                log.debug("Cleaned up empty file: {}", filePath);
            }
        } catch (Exception e) {
            log.debug("Could not clean up file: {}", filePath, e);
        }
    }
    
    private void cleanupFile(String filePath) {
        if (filePath == null) return;
        
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.debug("Cleaned up file: {}", filePath);
            }
        } catch (Exception e) {
            log.debug("Could not clean up file: {}", filePath, e);
        }
    }
    
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}