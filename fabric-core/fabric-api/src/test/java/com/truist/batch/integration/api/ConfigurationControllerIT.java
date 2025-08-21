package com.truist.batch.integration.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.truist.batch.builders.TestDataBuilder;
import com.truist.batch.dto.ManualJobConfigRequest;
import com.truist.batch.testutils.BaseTestNGTest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * Integration tests for Configuration API endpoints
 * Tests the complete API flow including authentication, validation, and database operations
 * 
 * @author Fabric Platform Testing Framework
 * @version 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@Test(groups = {"integration", "api", "configuration"})
@Sql(scripts = {"/sql/test-data-cleanup.sql", "/sql/test-configuration-data.sql"}, 
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Slf4j
public class ConfigurationControllerIT extends BaseTestNGTest {

    @LocalServerPort
    private int port;
    
    private String baseUrl;
    private String authToken;
    private String correlationId;
    private ManualJobConfigRequest validConfigRequest;
    private String testConfigId;
    
    // Test data constants
    private static final String API_BASE_PATH = "/api/v2/manual-job-config";
    private static final String AUTH_ENDPOINT = "/api/v1/auth/login";
    private static final String TEST_USERNAME = "test-user";
    private static final String TEST_PASSWORD = "test-password";
    
    @BeforeClass
    public void setupTestSuite() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        log.info("Setting up Configuration Controller Integration Test Suite");
    }
    
    @BeforeMethod
    @Override
    public void setUp() {
        super.setUp();
        setupRestAssured();
        authenticateTestUser();
        prepareTestData();
    }
    
    private void setupRestAssured() {
        baseUrl = "http://localhost:" + port;
        RestAssured.baseURI = baseUrl;
        correlationId = generateCorrelationId();
        log.debug("RestAssured configured with base URL: {}", baseUrl);
    }
    
    private void authenticateTestUser() {
        // Mock JWT token for testing - in real implementation, call auth endpoint
        authToken = "Bearer " + generateTestJwtToken();
        log.debug("Test user authenticated with token: {}", authToken.substring(0, 20) + "...");
    }
    
    private String generateTestJwtToken() {
        // For integration tests, generate a valid test JWT token
        // This would integrate with your JWT service in real implementation
        return "test-jwt-token-" + System.currentTimeMillis();
    }
    
    private void prepareTestData() {
        validConfigRequest = TestDataBuilder.manualJobConfig()
            .withJobName("Integration Test Configuration")
            .withDescription("Configuration created by integration test")
            .withSourceSystem("HR")
            .withTransactionCode("200")
            .withTemplateId("tpl_hr_test")
            .withMasterQueryId("qry_hr_test")
            .withStatus("ACTIVE")
            .buildRequest();
            
        log.debug("Test data prepared: {}", validConfigRequest.getJobName());
    }
    
    @Test(description = "POST /api/v2/manual-job-config - Create configuration successfully")
    public void testCreateConfiguration_Success() {
        log.info("Testing configuration creation via REST API");
        
        Response response = given()
            .header("Authorization", authToken)
            .header("X-Correlation-ID", correlationId)
            .contentType(ContentType.JSON)
            .body(validConfigRequest)
        .when()
            .post(API_BASE_PATH)
        .then()
            .statusCode(201)
            .header("X-Correlation-ID", equalTo(correlationId))
            .body("configId", notNullValue())
            .body("jobName", equalTo(validConfigRequest.getJobName()))
            .body("sourceSystem", equalTo(validConfigRequest.getSourceSystem()))
            .body("status", equalTo(validConfigRequest.getStatus()))
            .body("createdAt", notNullValue())
            .body("createdBy", equalTo(TEST_USERNAME))
        .extract()
            .response();
            
        testConfigId = response.path("configId");
        log.info("Configuration created successfully with ID: {}", testConfigId);
        
        // Verify response structure
        assertThat(response.path("configId")).isNotNull();
        assertThat(response.path("createdAt")).isNotNull();
    }
    
    @Test(description = "POST /api/v2/manual-job-config - Validation error for invalid request")
    public void testCreateConfiguration_ValidationError() {
        log.info("Testing configuration creation with validation errors");
        
        ManualJobConfigRequest invalidRequest = TestDataBuilder.manualJobConfig()
            .withJobName("")  // Invalid: empty job name
            .withSourceSystem(null)  // Invalid: null source system
            .buildRequest();
        
        given()
            .header("Authorization", authToken)
            .header("X-Correlation-ID", correlationId)
            .contentType(ContentType.JSON)
            .body(invalidRequest)
        .when()
            .post(API_BASE_PATH)
        .then()
            .statusCode(400)
            .header("X-Correlation-ID", equalTo(correlationId))
            .body("error", equalTo("VALIDATION_ERROR"))
            .body("message", containsString("Job name cannot be empty"))
            .body("validationErrors", hasSize(greaterThan(0)))
            .body("validationErrors[0].field", anyOf(equalTo("jobName"), equalTo("sourceSystem")))
            .body("timestamp", notNullValue());
            
        log.info("Validation error handling test completed successfully");
    }
    
    @Test(description = "POST /api/v2/manual-job-config - Unauthorized access")
    public void testCreateConfiguration_Unauthorized() {
        log.info("Testing configuration creation without authentication");
        
        given()
            .header("X-Correlation-ID", correlationId)
            .contentType(ContentType.JSON)
            .body(validConfigRequest)
        .when()
            .post(API_BASE_PATH)
        .then()
            .statusCode(401)
            .body("error", equalTo("UNAUTHORIZED"))
            .body("message", containsString("Authentication required"));
            
        log.info("Unauthorized access test completed successfully");
    }
    
    @Test(description = "GET /api/v2/manual-job-config/{id} - Retrieve configuration successfully", 
          dependsOnMethods = "testCreateConfiguration_Success")
    public void testGetConfiguration_Success() {
        log.info("Testing configuration retrieval by ID: {}", testConfigId);
        
        given()
            .header("Authorization", authToken)
            .header("X-Correlation-ID", correlationId)
        .when()
            .get(API_BASE_PATH + "/" + testConfigId)
        .then()
            .statusCode(200)
            .header("X-Correlation-ID", equalTo(correlationId))
            .body("configId", equalTo(testConfigId))
            .body("jobName", equalTo(validConfigRequest.getJobName()))
            .body("description", equalTo(validConfigRequest.getDescription()))
            .body("sourceSystem", equalTo(validConfigRequest.getSourceSystem()))
            .body("transactionCode", equalTo(validConfigRequest.getTransactionCode()))
            .body("templateId", equalTo(validConfigRequest.getTemplateId()))
            .body("masterQueryId", equalTo(validConfigRequest.getMasterQueryId()))
            .body("status", equalTo(validConfigRequest.getStatus()))
            .body("createdBy", notNullValue())
            .body("createdAt", notNullValue());
            
        log.info("Configuration retrieval test completed successfully");
    }
    
    @Test(description = "GET /api/v2/manual-job-config/{id} - Configuration not found")
    public void testGetConfiguration_NotFound() {
        String nonExistentId = "cfg_nonexistent_999";
        log.info("Testing configuration retrieval for non-existent ID: {}", nonExistentId);
        
        given()
            .header("Authorization", authToken)
            .header("X-Correlation-ID", correlationId)
        .when()
            .get(API_BASE_PATH + "/" + nonExistentId)
        .then()
            .statusCode(404)
            .header("X-Correlation-ID", equalTo(correlationId))
            .body("error", equalTo("NOT_FOUND"))
            .body("message", containsString("Configuration not found"))
            .body("resourceId", equalTo(nonExistentId));
            
        log.info("Configuration not found test completed successfully");
    }
    
    @Test(description = "GET /api/v2/manual-job-config - Retrieve all configurations")
    public void testGetAllConfigurations_Success() {
        log.info("Testing retrieval of all configurations");
        
        Response response = given()
            .header("Authorization", authToken)
            .header("X-Correlation-ID", correlationId)
        .when()
            .get(API_BASE_PATH)
        .then()
            .statusCode(200)
            .header("X-Correlation-ID", equalTo(correlationId))
            .body("$", hasSize(greaterThanOrEqualTo(0)))
            .body("$", everyItem(hasKey("configId")))
            .body("$", everyItem(hasKey("jobName")))
            .body("$", everyItem(hasKey("status")))
            .body("$", everyItem(hasKey("createdAt")))
        .extract()
            .response();
            
        List<Map<String, Object>> configurations = response.jsonPath().getList("$");
        log.info("Retrieved {} configurations successfully", configurations.size());
        
        // Verify response structure
        assertThat(configurations).isNotNull();
        configurations.forEach(config -> {
            assertThat(config.get("configId")).isNotNull();
            assertThat(config.get("jobName")).isNotNull();
            assertThat(config.get("status")).isNotNull();
        });
    }
    
    @Test(description = "PUT /api/v2/manual-job-config/{id} - Update configuration successfully",
          dependsOnMethods = "testCreateConfiguration_Success")
    public void testUpdateConfiguration_Success() {
        log.info("Testing configuration update for ID: {}", testConfigId);
        
        ManualJobConfigRequest updateRequest = TestDataBuilder.manualJobConfig()
            .withJobName("Updated Integration Test Configuration")
            .withDescription("Updated description for integration test")
            .withStatus("INACTIVE")
            .buildRequest();
        
        given()
            .header("Authorization", authToken)
            .header("X-Correlation-ID", correlationId)
            .contentType(ContentType.JSON)
            .body(updateRequest)
        .when()
            .put(API_BASE_PATH + "/" + testConfigId)
        .then()
            .statusCode(200)
            .header("X-Correlation-ID", equalTo(correlationId))
            .body("configId", equalTo(testConfigId))
            .body("jobName", equalTo(updateRequest.getJobName()))
            .body("description", equalTo(updateRequest.getDescription()))
            .body("status", equalTo(updateRequest.getStatus()))
            .body("lastModifiedAt", notNullValue())
            .body("lastModifiedBy", equalTo(TEST_USERNAME));
            
        log.info("Configuration update test completed successfully");
    }
    
    @Test(description = "PUT /api/v2/manual-job-config/{id} - Update non-existent configuration")
    public void testUpdateConfiguration_NotFound() {
        String nonExistentId = "cfg_nonexistent_999";
        log.info("Testing configuration update for non-existent ID: {}", nonExistentId);
        
        ManualJobConfigRequest updateRequest = TestDataBuilder.manualJobConfig()
            .withJobName("Updated Configuration")
            .buildRequest();
        
        given()
            .header("Authorization", authToken)
            .header("X-Correlation-ID", correlationId)
            .contentType(ContentType.JSON)
            .body(updateRequest)
        .when()
            .put(API_BASE_PATH + "/" + nonExistentId)
        .then()
            .statusCode(404)
            .header("X-Correlation-ID", equalTo(correlationId))
            .body("error", equalTo("NOT_FOUND"))
            .body("message", containsString("Configuration not found"));
            
        log.info("Update non-existent configuration test completed successfully");
    }
    
    @Test(description = "DELETE /api/v2/manual-job-config/{id} - Delete configuration successfully",
          dependsOnMethods = {"testGetConfiguration_Success", "testUpdateConfiguration_Success"})
    public void testDeleteConfiguration_Success() {
        log.info("Testing configuration deletion for ID: {}", testConfigId);
        
        given()
            .header("Authorization", authToken)
            .header("X-Correlation-ID", correlationId)
        .when()
            .delete(API_BASE_PATH + "/" + testConfigId)
        .then()
            .statusCode(204)
            .header("X-Correlation-ID", equalTo(correlationId));
            
        // Verify configuration is deleted by trying to retrieve it
        given()
            .header("Authorization", authToken)
            .header("X-Correlation-ID", correlationId)
        .when()
            .get(API_BASE_PATH + "/" + testConfigId)
        .then()
            .statusCode(404);
            
        log.info("Configuration deletion test completed successfully");
    }
    
    @Test(description = "POST /api/v2/manual-job-config/validate - Validate configuration request")
    public void testValidateConfiguration_Success() {
        log.info("Testing configuration validation endpoint");
        
        given()
            .header("Authorization", authToken)
            .header("X-Correlation-ID", correlationId)
            .contentType(ContentType.JSON)
            .body(validConfigRequest)
        .when()
            .post(API_BASE_PATH + "/validate")
        .then()
            .statusCode(200)
            .header("X-Correlation-ID", equalTo(correlationId))
            .body("valid", equalTo(true))
            .body("validationErrors", hasSize(0))
            .body("validationWarnings", hasSize(greaterThanOrEqualTo(0)));
            
        log.info("Configuration validation test completed successfully");
    }
    
    @Test(description = "POST /api/v2/manual-job-config/validate - Validation errors")
    public void testValidateConfiguration_WithErrors() {
        log.info("Testing configuration validation with errors");
        
        ManualJobConfigRequest invalidRequest = TestDataBuilder.manualJobConfig()
            .withJobName("")  // Invalid
            .withSourceSystem(null)  // Invalid
            .withTransactionCode("INVALID")  // Invalid format
            .buildRequest();
        
        given()
            .header("Authorization", authToken)
            .header("X-Correlation-ID", correlationId)
            .contentType(ContentType.JSON)
            .body(invalidRequest)
        .when()
            .post(API_BASE_PATH + "/validate")
        .then()
            .statusCode(200)
            .header("X-Correlation-ID", equalTo(correlationId))
            .body("valid", equalTo(false))
            .body("validationErrors", hasSize(greaterThan(0)))
            .body("validationErrors[0].field", notNullValue())
            .body("validationErrors[0].message", notNullValue());
            
        log.info("Configuration validation with errors test completed successfully");
    }
    
    @Test(description = "Test API rate limiting")
    public void testRateLimiting() {
        log.info("Testing API rate limiting");
        
        // Make multiple rapid requests to trigger rate limiting
        for (int i = 0; i < 10; i++) {
            Response response = given()
                .header("Authorization", authToken)
                .header("X-Correlation-ID", correlationId + "-" + i)
            .when()
                .get(API_BASE_PATH);
                
            if (i < 5) {
                // First few requests should succeed
                assertThat(response.getStatusCode()).isIn(200, 404);
            } else {
                // Later requests might hit rate limit
                if (response.getStatusCode() == 429) {
                    log.info("Rate limiting triggered at request {}", i + 1);
                    break;
                }
            }
        }
        
        log.info("Rate limiting test completed");
    }
    
    @Test(description = "Test API response time performance")
    public void testApiResponseTime() {
        log.info("Testing API response time performance");
        
        long startTime = System.currentTimeMillis();
        
        given()
            .header("Authorization", authToken)
            .header("X-Correlation-ID", correlationId)
        .when()
            .get(API_BASE_PATH)
        .then()
            .statusCode(200)
            .time(lessThan(2000L)); // API should respond within 2 seconds
            
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        log.info("API response time: {} ms", responseTime);
        assertThat(responseTime).isLessThan(2000); // 2 second threshold
    }
    
    @Override
    protected void cleanupTestData() {
        log.debug("Cleaning up integration test data");
        // Test data cleanup is handled by @Sql annotation
    }
}