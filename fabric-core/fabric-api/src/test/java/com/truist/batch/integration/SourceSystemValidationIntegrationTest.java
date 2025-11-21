package com.truist.batch.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truist.batch.dto.MasterQueryCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

/**
 * =========================================================================
 * SOURCE SYSTEM VALIDATION INTEGRATION TESTS
 * =========================================================================
 * 
 * Purpose: Integration tests to verify that the @ValidSourceSystem annotation
 * correctly validates source systems against the actual database
 * 
 * Test Scenarios:
 * - Valid source systems from database (MTG, SHAW, ENCORE) should pass
 * - Invalid/non-existent source systems should fail with proper error message
 * - Hardcoded validation no longer blocks valid database systems
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Source System Validation Enhancement
 * =========================================================================
 */
@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=password",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml"
})
class SourceSystemValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "testuser", roles = {"JOB_CREATOR"})
    void testValidSourceSystem_MTG_ShouldPass() throws Exception {
        // Given
        MasterQueryCreateRequest request = createValidRequest("MTG");

        // When & Then
        mockMvc.perform(post("/api/v2/master-query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"JOB_CREATOR"})
    void testValidSourceSystem_SHAW_ShouldPass() throws Exception {
        // Given
        MasterQueryCreateRequest request = createValidRequest("SHAW");

        // When & Then
        mockMvc.perform(post("/api/v2/master-query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"JOB_CREATOR"})
    void testValidSourceSystem_ENCORE_ShouldPass() throws Exception {
        // Given
        MasterQueryCreateRequest request = createValidRequest("ENCORE");

        // When & Then
        mockMvc.perform(post("/api/v2/master-query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"JOB_CREATOR"})
    void testInvalidSourceSystem_ShouldFailWithValidationError() throws Exception {
        // Given
        MasterQueryCreateRequest request = createValidRequest("INVALID_SYSTEM");

        // When & Then
        mockMvc.perform(post("/api/v2/master-query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Source system 'INVALID_SYSTEM' does not exist in the database")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"JOB_CREATOR"})
    void testNullSourceSystem_ShouldFailWithValidationError() throws Exception {
        // Given
        MasterQueryCreateRequest request = createValidRequest(null);

        // When & Then
        mockMvc.perform(post("/api/v2/master-query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Source system is required")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"JOB_CREATOR"})
    void testEmptySourceSystem_ShouldFailWithValidationError() throws Exception {
        // Given
        MasterQueryCreateRequest request = createValidRequest("");

        // When & Then
        mockMvc.perform(post("/api/v2/master-query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Source system cannot be null or empty")));
    }

    /**
     * Helper method to create a valid request with specified source system.
     */
    private MasterQueryCreateRequest createValidRequest(String sourceSystem) {
        return MasterQueryCreateRequest.builder()
                .sourceSystem(sourceSystem)
                .queryName("test_query_" + System.currentTimeMillis())
                .description("Test query for validation")
                .queryType("SELECT") // Has default value now
                .querySql("SELECT * FROM test_table WHERE id = :id")
                .businessJustification("Testing database-driven source system validation for enhanced security compliance")
                .build();
    }
}