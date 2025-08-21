package com.truist.batch.testutils;

import org.testng.IExecutionListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import lombok.extern.slf4j.Slf4j;

/**
 * TestNG execution and test listener for comprehensive test monitoring
 * Provides detailed logging and metrics collection during test execution
 * 
 * @author Fabric Platform Testing Framework
 * @version 1.0.0
 */
@Slf4j
public class TestExecutionListener implements IExecutionListener, ITestListener {

    private long suiteStartTime;
    private long testStartTime;
    
    @Override
    public void onExecutionStart() {
        suiteStartTime = System.currentTimeMillis();
        log.info("=== Fabric Platform Test Suite Execution Started ===");
        log.info("Java Version: {}", System.getProperty("java.version"));
        log.info("Available Processors: {}", Runtime.getRuntime().availableProcessors());
        log.info("Max Memory: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
    }

    @Override
    public void onExecutionFinish() {
        long totalDuration = System.currentTimeMillis() - suiteStartTime;
        log.info("=== Fabric Platform Test Suite Execution Finished ===");
        log.info("Total Execution Time: {} ms", totalDuration);
        log.info("Memory Usage: {} MB", 
            (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);
    }

    @Override
    public void onTestStart(ITestResult result) {
        testStartTime = System.currentTimeMillis();
        String testName = getTestName(result);
        log.debug("Starting test: {} [{}]", testName, result.getMethod().getGroups());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        long duration = System.currentTimeMillis() - testStartTime;
        String testName = getTestName(result);
        log.debug("Test passed: {} in {} ms", testName, duration);
        
        if (duration > 1000) {
            log.warn("Slow test detected: {} took {} ms", testName, duration);
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        long duration = System.currentTimeMillis() - testStartTime;
        String testName = getTestName(result);
        Throwable throwable = result.getThrowable();
        
        log.error("Test failed: {} in {} ms", testName, duration, throwable);
        
        // Capture additional context for failure analysis
        logTestContext(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = getTestName(result);
        log.warn("Test skipped: {}", testName);
        
        if (result.getThrowable() != null) {
            log.warn("Skip reason: {}", result.getThrowable().getMessage());
        }
    }

    private String getTestName(ITestResult result) {
        return result.getTestClass().getName() + "." + result.getMethod().getMethodName();
    }
    
    private void logTestContext(ITestResult result) {
        try {
            log.error("Test Context for failure analysis:");
            log.error("  - Test Class: {}", result.getTestClass().getName());
            log.error("  - Test Method: {}", result.getMethod().getMethodName());
            log.error("  - Groups: {}", java.util.Arrays.toString(result.getMethod().getGroups()));
            log.error("  - Parameters: {}", java.util.Arrays.toString(result.getParameters()));
            log.error("  - Thread: {}", Thread.currentThread().getName());
            log.error("  - Memory: {} MB", 
                (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);
        } catch (Exception e) {
            log.warn("Failed to capture test context", e);
        }
    }
}