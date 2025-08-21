package com.truist.batch.testutils;

import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * TestNG result listener for test reporting and analysis
 * Collects test results and generates summary reports
 * 
 * @author Fabric Platform Testing Framework  
 * @version 1.0.0
 */
@Slf4j
public class TestResultListener extends TestListenerAdapter {

    private int totalTests = 0;
    private int passedTests = 0;
    private int failedTests = 0;
    private int skippedTests = 0;
    
    @Override
    public void onTestStart(ITestResult result) {
        totalTests++;
        super.onTestStart(result);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        passedTests++;
        super.onTestSuccess(result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        failedTests++;
        super.onTestFailure(result);
        
        // Log detailed failure information
        logFailureDetails(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        skippedTests++;
        super.onTestSkipped(result);
    }

    @Override
    public void onFinish(org.testng.ITestContext context) {
        super.onFinish(context);
        
        // Generate test summary
        generateTestSummary(context);
    }
    
    private void logFailureDetails(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        String className = result.getTestClass().getName();
        Throwable exception = result.getThrowable();
        
        log.error("=== TEST FAILURE DETAILS ===");
        log.error("Test: {}.{}", className, methodName);
        log.error("Duration: {} ms", result.getEndMillis() - result.getStartMillis());
        
        if (exception != null) {
            log.error("Exception Type: {}", exception.getClass().getSimpleName());
            log.error("Exception Message: {}", exception.getMessage());
            
            // Log stack trace for debugging
            StackTraceElement[] stackTrace = exception.getStackTrace();
            for (int i = 0; i < Math.min(stackTrace.length, 5); i++) {
                log.error("  at {}", stackTrace[i]);
            }
        }
        log.error("=============================");
    }
    
    private void generateTestSummary(org.testng.ITestContext context) {
        log.info("");
        log.info("=== FABRIC PLATFORM TEST SUMMARY ===");
        log.info("Suite: {}", context.getSuite().getName());
        log.info("Test: {}", context.getName());
        log.info("Total Tests: {}", totalTests);
        log.info("Passed: {} ({}%)", passedTests, calculatePercentage(passedTests, totalTests));
        log.info("Failed: {} ({}%)", failedTests, calculatePercentage(failedTests, totalTests));
        log.info("Skipped: {} ({}%)", skippedTests, calculatePercentage(skippedTests, totalTests));
        
        long totalDuration = context.getEndDate().getTime() - context.getStartDate().getTime();
        log.info("Total Duration: {} ms", totalDuration);
        
        // Calculate success rate
        double successRate = calculatePercentage(passedTests, totalTests);
        if (successRate >= 95.0) {
            log.info("✅ Test Suite Status: EXCELLENT ({}% pass rate)", String.format("%.1f", successRate));
        } else if (successRate >= 80.0) {
            log.info("⚠️ Test Suite Status: GOOD ({}% pass rate)", String.format("%.1f", successRate));
        } else if (successRate >= 60.0) {
            log.info("❌ Test Suite Status: POOR ({}% pass rate)", String.format("%.1f", successRate));
        } else {
            log.info("🚫 Test Suite Status: CRITICAL ({}% pass rate)", String.format("%.1f", successRate));
        }
        
        log.info("=====================================");
    }
    
    private double calculatePercentage(int value, int total) {
        return total == 0 ? 0.0 : (double) value * 100 / total;
    }
}