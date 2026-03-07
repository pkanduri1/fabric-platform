package com.fabric.batch.integration;

import com.fabric.batch.config.QuerySecurityConfig;
import com.fabric.batch.dto.jobexecution.JobExecutionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
class JobExecutionApiIntegrationTest {

    private static final String ORACLE_URL  = "jdbc:oracle:thin:@localhost:1522/FREEPDB1";
    private static final String ORACLE_USER = "cm3int";
    private static final String ORACLE_PASS = "MySecurePass123";

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
            return new JdbcTemplate(ds);
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

    private static final String KNOWN_CONFIG_ID = "TEST-JC-IT-001";

    @BeforeEach
    void seed() {
        jdbcTemplate.update("DELETE FROM MANUAL_JOB_EXECUTION WHERE CONFIG_ID = ?", KNOWN_CONFIG_ID);
        jdbcTemplate.update("DELETE FROM MANUAL_JOB_CONFIG WHERE CONFIG_ID = ?", KNOWN_CONFIG_ID);
        jdbcTemplate.update(
            "INSERT INTO MANUAL_JOB_CONFIG " +
            "(CONFIG_ID, JOB_NAME, JOB_TYPE, SOURCE_SYSTEM, TARGET_SYSTEM, JOB_PARAMETERS, CREATED_BY) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            KNOWN_CONFIG_ID, "IT Test Job", "BATCH", "TEST_SYS", "TEST_TARGET", "{}", "test-it");
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM MANUAL_JOB_EXECUTION WHERE CONFIG_ID LIKE 'TEST-%'");
        jdbcTemplate.update("DELETE FROM MANUAL_JOB_CONFIG WHERE CONFIG_ID LIKE 'TEST-%'");
    }

    // ── POST /execute ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "API_EXECUTOR")
    void execute_validRequest_returns202WithExecutionId() throws Exception {
        JobExecutionRequest req = JobExecutionRequest.builder()
                .jobConfigId(KNOWN_CONFIG_ID)
                .sourceSystem("TEST_SYS")
                .transformationRules(List.of("RULE_A"))
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/jobs/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.executionId").exists())
                .andExpect(jsonPath("$.status").value("STARTED"))
                .andExpect(jsonPath("$.statusUrl").exists())
                .andReturn();

        String executionId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("executionId").asText();
        assertThat(executionId).startsWith("EXEC-");
    }

    @Test
    @WithMockUser(roles = "API_EXECUTOR")
    void execute_missingJobConfigId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sourceSystem\":\"X\",\"transformationRules\":[\"R\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "API_EXECUTOR")
    void execute_unknownJobConfigId_returns400WithErrorCode() throws Exception {
        JobExecutionRequest req = JobExecutionRequest.builder()
                .jobConfigId("JC-DOES-NOT-EXIST")
                .sourceSystem("SYS")
                .transformationRules(List.of("R"))
                .build();

        mockMvc.perform(post("/api/v1/jobs/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("JOB_CONFIG_NOT_FOUND"));
    }

    @Test
    void execute_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "JOB_VIEWER")
    void execute_wrongRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ── GET /{id}/status ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "API_EXECUTOR")
    void status_unknownExecutionId_returns404WithErrorCode() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/EXEC-0000/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("EXECUTION_NOT_FOUND"));
    }

    // ── POST /{id}/cancel ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "API_EXECUTOR")
    void cancel_unknownId_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/EXEC-GONE/cancel"))
                .andExpect(status().isNotFound());
    }

    // ── POST /{id}/retry ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "API_EXECUTOR")
    void retry_unknownId_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/EXEC-GONE/retry"))
                .andExpect(status().isNotFound());
    }

    // ── GET /recent ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "API_EXECUTOR")
    void recent_noFilters_returns200WithEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executions").isArray());
    }

    @Test
    @WithMockUser(roles = "API_EXECUTOR")
    void recent_withStatusFilter_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/recent?status=FAILED"))
                .andExpect(status().isOk());
    }

    // ── GET /{id}/audit ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "API_EXECUTOR")
    void audit_afterSubmit_returnsNonEmptyAuditEntries() throws Exception {
        JobExecutionRequest req = JobExecutionRequest.builder()
                .jobConfigId(KNOWN_CONFIG_ID)
                .sourceSystem("TEST_SYS")
                .transformationRules(List.of("RULE_A"))
                .build();

        MvcResult submitResult = mockMvc.perform(post("/api/v1/jobs/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andReturn();

        String executionId = objectMapper.readTree(
                submitResult.getResponse().getContentAsString())
                .get("executionId").asText();

        mockMvc.perform(get("/api/v1/jobs/" + executionId + "/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(executionId))
                .andExpect(jsonPath("$.auditEntries").isArray())
                .andExpect(jsonPath("$.auditEntries.length()").value(greaterThan(0)));
    }

    @Test
    @WithMockUser(roles = "API_EXECUTOR")
    void audit_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/EXEC-AUDIT-GONE/audit"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("EXECUTION_NOT_FOUND"));
    }
}
