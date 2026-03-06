package com.fabric.batch.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fabric.batch.config.QuerySecurityConfig;
import com.fabric.batch.dto.MasterQueryCreateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration tests verifying that @ValidSourceSystem validates source systems
 * against the real Oracle database (not H2). Seed data uses TEST- prefix IDs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:oracle:thin:@localhost:1522/FREEPDB1",
    "spring.datasource.driver-class-name=oracle.jdbc.OracleDriver",
    "spring.datasource.username=cm3int",
    "spring.datasource.password=MySecurePass123",
    "spring.datasource.primary.url=jdbc:oracle:thin:@localhost:1522/FREEPDB1",
    "spring.datasource.primary.driver-class-name=oracle.jdbc.OracleDriver",
    "spring.datasource.primary.username=cm3int",
    "spring.datasource.primary.password=MySecurePass123",
    "spring.datasource.readonly.url=jdbc:oracle:thin:@localhost:1522/FREEPDB1",
    "spring.datasource.readonly.driver-class-name=oracle.jdbc.OracleDriver",
    "spring.datasource.readonly.username=cm3int",
    "spring.datasource.readonly.password=MySecurePass123",
    "spring.liquibase.enabled=false",
    "fabric.security.csrf.enabled=false",
    "fabric.security.ldap.enabled=false"
})
class SourceSystemValidationIntegrationTest {

    private static final String ORACLE_URL  = "jdbc:oracle:thin:@localhost:1522/FREEPDB1";
    private static final String ORACLE_USER = "cm3int";
    private static final String ORACLE_PASS = "MySecurePass123";

    // Test source system IDs — TEST- prefix allows safe @AfterEach cleanup
    private static final String SYS_MTG    = "TEST-MTG";
    private static final String SYS_SHAW   = "TEST-SHAW";
    private static final String SYS_ENCORE = "TEST-ENCORE";

    @TestConfiguration
    static class OracleTestDataSourceConfig {

        @Bean(name = "dataSource")
        @Primary
        public DataSource dataSource() {
            return DataSourceBuilder.create()
                    .driverClassName("oracle.jdbc.OracleDriver")
                    .url(ORACLE_URL)
                    .username(ORACLE_USER).password(ORACLE_PASS).build();
        }

        @Bean(name = "readOnlyDataSource")
        public DataSource readOnlyDataSource() {
            return DataSourceBuilder.create()
                    .driverClassName("oracle.jdbc.OracleDriver")
                    .url(ORACLE_URL)
                    .username(ORACLE_USER).password(ORACLE_PASS).build();
        }

        @Bean(name = "jdbcTemplate")
        @Primary
        public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource ds) {
            return new JdbcTemplate(ds);
        }

        @Bean(name = "readOnlyJdbcTemplate")
        public JdbcTemplate readOnlyJdbcTemplate(@Qualifier("readOnlyDataSource") DataSource ds) {
            JdbcTemplate template = new JdbcTemplate(ds);
            template.setQueryTimeout(30);
            template.setFetchSize(100);
            template.setMaxRows(100);
            return template;
        }

        @Bean(name = "readOnlyDataSourceHealthIndicator")
        public QuerySecurityConfig.ReadOnlyDataSourceHealthIndicator readOnlyDataSourceHealthIndicator(
                @Qualifier("readOnlyDataSource") DataSource ds) {
            return new QuerySecurityConfig.ReadOnlyDataSourceHealthIndicator(ds);
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired @Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seed() {
        jdbcTemplate.update("DELETE FROM SOURCE_SYSTEMS WHERE ID LIKE 'TEST-%'");
        jdbcTemplate.update(
            "INSERT INTO SOURCE_SYSTEMS (ID, NAME, TYPE, ENABLED) VALUES (?, ?, ?, ?)",
            SYS_MTG, "MTG Test System", "ORACLE", "Y");
        jdbcTemplate.update(
            "INSERT INTO SOURCE_SYSTEMS (ID, NAME, TYPE, ENABLED) VALUES (?, ?, ?, ?)",
            SYS_SHAW, "SHAW Test System", "ORACLE", "Y");
        jdbcTemplate.update(
            "INSERT INTO SOURCE_SYSTEMS (ID, NAME, TYPE, ENABLED) VALUES (?, ?, ?, ?)",
            SYS_ENCORE, "ENCORE Test System", "ORACLE", "Y");
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM SOURCE_SYSTEMS WHERE ID LIKE 'TEST-%'");
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"JOB_CREATOR"})
    void testValidSourceSystem_MTG_ShouldPass() throws Exception {
        mockMvc.perform(post("/api/v2/master-query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createValidRequest(SYS_MTG))))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"JOB_CREATOR"})
    void testValidSourceSystem_SHAW_ShouldPass() throws Exception {
        mockMvc.perform(post("/api/v2/master-query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createValidRequest(SYS_SHAW))))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"JOB_CREATOR"})
    void testValidSourceSystem_ENCORE_ShouldPass() throws Exception {
        mockMvc.perform(post("/api/v2/master-query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createValidRequest(SYS_ENCORE))))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"JOB_CREATOR"})
    void testInvalidSourceSystem_ShouldFailWithValidationError() throws Exception {
        mockMvc.perform(post("/api/v2/master-query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createValidRequest("INVALID_SYSTEM"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("does not exist in the database")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"JOB_CREATOR"})
    void testNullSourceSystem_ShouldFailWithValidationError() throws Exception {
        mockMvc.perform(post("/api/v2/master-query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createValidRequest(null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Source system is required")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"JOB_CREATOR"})
    void testEmptySourceSystem_ShouldFailWithValidationError() throws Exception {
        mockMvc.perform(post("/api/v2/master-query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createValidRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Source system cannot be null or empty")));
    }

    private MasterQueryCreateRequest createValidRequest(String sourceSystem) {
        return MasterQueryCreateRequest.builder()
                .sourceSystem(sourceSystem)
                .queryName("test_query_" + System.currentTimeMillis())
                .description("Test query for validation")
                .queryType("SELECT")
                .querySql("SELECT * FROM test_table WHERE id = :id")
                .businessJustification("Testing database-driven source system validation")
                .build();
    }
}
