package com.fabric.batch.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fabric.batch.dto.MasterQueryCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

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
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=password",
    "spring.datasource.primary.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.primary.driver-class-name=org.h2.Driver",
    "spring.datasource.primary.username=sa",
    "spring.datasource.primary.password=password",
    "spring.datasource.readonly.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.readonly.driver-class-name=org.h2.Driver",
    "spring.datasource.readonly.username=sa",
    "spring.datasource.readonly.password=password",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.liquibase.enabled=false",
    "spring.sql.init.mode=never",
    "fabric.security.csrf.enabled=false"
})
class SourceSystemValidationIntegrationTest {

    /**
     * Test configuration providing H2 datasource beans to replace Oracle-dependent configs.
     * This overrides QuerySecurityConfig (which uses Oracle-specific DUAL table validation)
     * and provides the 'dataSource' bean required by Spring Batch DefaultBatchConfiguration.
     */
    @TestConfiguration
    static class H2TestDataSourceConfig {

        @Bean(name = "dataSource")
        @Primary
        public DataSource dataSource() {
            DataSource ds = DataSourceBuilder.create()
                    .driverClassName("org.h2.Driver")
                    .url("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                    .username("sa")
                    .password("password")
                    .build();
            return ds;
        }

        @Bean(name = "readOnlyDataSource")
        public DataSource readOnlyDataSource() {
            return DataSourceBuilder.create()
                    .driverClassName("org.h2.Driver")
                    .url("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                    .username("sa")
                    .password("password")
                    .build();
        }

        @Bean(name = "readOnlyJdbcTemplate")
        public JdbcTemplate readOnlyJdbcTemplate() {
            JdbcTemplate template = new JdbcTemplate(readOnlyDataSource());
            template.setQueryTimeout(30);
            template.setFetchSize(100);
            template.setMaxRows(100);
            return template;
        }

        @Bean(name = "jdbcTemplate")
        @Primary
        public JdbcTemplate jdbcTemplate() {
            return new JdbcTemplate(dataSource());
        }

        @Bean
        public DataSourceInitializer dataSourceInitializer() {
            DataSourceInitializer initializer = new DataSourceInitializer();
            initializer.setDataSource(dataSource());
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("schema-h2.sql"));
            populator.addScript(new ClassPathResource("data-h2.sql"));
            populator.setContinueOnError(false);
            initializer.setDatabasePopulator(populator);
            return initializer;
        }
    }

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
