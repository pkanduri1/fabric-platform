package com.truist.batch.testutils;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import lombok.extern.slf4j.Slf4j;

/**
 * Base class for all TestNG test classes in Fabric Platform
 * Provides common test setup, teardown, and Spring context configuration
 * 
 * @author Fabric Platform Testing Framework
 * @version 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Listeners({TestExecutionListener.class, TestResultListener.class})
@Slf4j
public abstract class BaseTestNGTest extends AbstractTestNGSpringContextTests {

    protected static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    protected static final String TEST_USER = "test-user";
    protected static final String TEST_ROLE = "JOB_CREATOR";
    
    /**
     * Setup method executed before each test method
     */
    @BeforeMethod
    public void setUp() {
        log.debug("Setting up test: {}", getCurrentTestName());
        setupTestData();
        setupSecurityContext();
    }
    
    /**
     * Teardown method executed after each test method
     */
    @AfterMethod
    public void tearDown() {
        log.debug("Tearing down test: {}", getCurrentTestName());
        cleanupTestData();
        clearSecurityContext();
    }
    
    /**
     * Override this method to set up test-specific data
     */
    protected void setupTestData() {
        // Default implementation - override in subclasses
    }
    
    /**
     * Override this method to clean up test-specific data
     */
    protected void cleanupTestData() {
        // Default implementation - override in subclasses
    }
    
    /**
     * Setup security context for testing
     */
    protected void setupSecurityContext() {
        // Will be implemented with actual security setup
    }
    
    /**
     * Clear security context after testing
     */
    protected void clearSecurityContext() {
        // Will be implemented with actual security cleanup
    }
    
    /**
     * Get current test method name
     * @return test method name
     */
    protected String getCurrentTestName() {
        return Thread.currentThread().getStackTrace()[2].getMethodName();
    }
    
    /**
     * Generate correlation ID for test
     * @return correlation ID
     */
    protected String generateCorrelationId() {
        return "test-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }
    
    /**
     * Wait for async operations to complete
     * @param timeoutMs timeout in milliseconds
     */
    protected void waitForAsync(long timeoutMs) {
        try {
            Thread.sleep(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test interrupted", e);
        }
    }
}