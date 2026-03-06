package com.fabric.batch.integration;

import com.fabric.batch.config.QuerySecurityConfig;
import com.fabric.batch.dto.jobexecution.JobExecutionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:execdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=password",
    "spring.datasource.primary.url=jdbc:h2:mem:execdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.primary.driver-class-name=org.h2.Driver",
    "spring.datasource.primary.username=sa",
    "spring.datasource.primary.password=password",
    "spring.datasource.readonly.url=jdbc:h2:mem:execdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.readonly.driver-class-name=org.h2.Driver",
    "spring.datasource.readonly.username=sa",
    "spring.datasource.readonly.password=password",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.liquibase.enabled=false",
    "spring.sql.init.mode=never",
    "fabric.security.csrf.enabled=false"
})
class JobExecutionApiIntegrationTest {

    @TestConfiguration
    static class H2TestDataSourceConfig {

        @Bean(name = "dataSource")
        @Primary
        public DataSource dataSource() {
            return DataSourceBuilder.create()
                    .driverClassName("org.h2.Driver")
                    .url("jdbc:h2:mem:execdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                    .username("sa").password("password").build();
        }

        @Bean(name = "readOnlyDataSource")
        public DataSource readOnlyDataSource() {
            return DataSourceBuilder.create()
                    .driverClassName("org.h2.Driver")
                    .url("jdbc:h2:mem:execdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                    .username("sa").password("password").build();
        }

        @Bean(name = "jdbcTemplate")
        @Primary
        public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
            return new JdbcTemplate(dataSource);
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

        @Bean
        public DataSourceInitializer dataSourceInitializer(@Qualifier("dataSource") DataSource dataSource) {
            DataSourceInitializer initializer = new DataSourceInitializer();
            initializer.setDataSource(dataSource);
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("schema-h2.sql"));
            populator.addScript(new ClassPathResource("data-h2.sql"));
            populator.setContinueOnError(false);
            initializer.setDatabasePopulator(populator);
            return initializer;
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired @Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate;

    private static final String KNOWN_CONFIG_ID = "JC-IT-001";

    @BeforeEach
    void seed() {
        jdbcTemplate.update(
            "MERGE INTO MANUAL_JOB_CONFIG (CONFIG_ID, JOB_NAME, SOURCE_SYSTEM, STATUS) " +
            "KEY (CONFIG_ID) VALUES (?, ?, ?, ?)",
            KNOWN_CONFIG_ID, "IT Test Job", "TEST_SYS", "ACTIVE");
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
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
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
    void audit_anyId_returns200WithEmptyAuditEntries() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/EXEC-ANY/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditEntries").isArray());
    }
}
