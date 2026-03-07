package com.fabric.batch.integration;

import com.fabric.batch.config.QuerySecurityConfig;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TransformationConfigController endpoints.
 *
 * <p>Connects to the real Oracle database on localhost:1522/FREEPDB1.
 * The Oracle container (fabric-oracle-free) must be running before
 * executing these tests.
 *
 * <p>Test data is isolated with the "TEST_" prefix on FILE_TYPE so that
 * cleanup ({@code DELETE WHERE FILE_TYPE LIKE 'TEST_%'}) is safe and
 * never affects production rows.
 *
 * <p>Table structure of FIELD_TEMPLATES (live schema, composite PK + surrogate ID):
 * <ul>
 *   <li>ID               VARCHAR2 NOT NULL  (surrogate key, added by us051-001)</li>
 *   <li>FILE_TYPE         VARCHAR2 NOT NULL  (part of composite PK)</li>
 *   <li>TRANSACTION_TYPE  VARCHAR2 NOT NULL  (part of composite PK)</li>
 *   <li>FIELD_NAME        VARCHAR2 NOT NULL  (part of composite PK)</li>
 *   <li>TARGET_POSITION   NUMBER   NOT NULL</li>
 *   <li>CREATED_BY        VARCHAR2 NOT NULL</li>
 *   <li>All other columns are nullable</li>
 * </ul>
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
class TransformationConfigIntegrationTest {

    private static final String ORACLE_URL  = "jdbc:oracle:thin:@localhost:1522/FREEPDB1";
    private static final String ORACLE_USER = "cm3int";
    private static final String ORACLE_PASS = "MySecurePass123";

    // Test-data constants — FILE_TYPE uses TEST_ prefix for safe prefix-based cleanup
    private static final String TEST_FILE_TYPE  = "TEST_FT";
    private static final String TEST_TRANS_TYPE = "TEST_TX";
    private static final String TEST_FIELD_NAME = "TEST_FLD";

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

    // Seed row ID — fixed so tests can reference it without querying first
    private static final String TEST_ROW_ID = "TEST-ID-INTEGRATION-001";

    /**
     * Wipes any leftover TEST_ rows and seeds a single known test row.
     *
     * <p>Includes all NOT NULL columns plus the surrogate ID added by us051-001.
     * ENABLED is set to 'Y' so findAll() includes the row.
     */
    @BeforeEach
    void seed() {
        // Remove any leftover data from a previous run
        jdbcTemplate.update("DELETE FROM FIELD_TEMPLATES WHERE FILE_TYPE LIKE 'TEST_%'");

        // Insert a minimal valid row satisfying all NOT NULL constraints, including the ID column
        jdbcTemplate.update(
                "INSERT INTO FIELD_TEMPLATES " +
                "(ID, FILE_TYPE, TRANSACTION_TYPE, FIELD_NAME, TARGET_POSITION, CREATED_BY, ENABLED) " +
                "VALUES (?, ?, ?, ?, 1, 'test-it', 'Y')",
                TEST_ROW_ID, TEST_FILE_TYPE, TEST_TRANS_TYPE, TEST_FIELD_NAME);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM FIELD_TEMPLATES WHERE FILE_TYPE LIKE 'TEST_%'");
    }

    // ── GET /v1/transformation/configs ────────────────────────────────────────

    @Test
    @WithMockUser(roles = "USER")
    void getAll_authenticatedUser_returns200WithArray() throws Exception {
        mockMvc.perform(get("/v1/transformation/configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAll_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/v1/transformation/configs"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /v1/transformation/configs/by-system/{sourceSystem} ──────────────

    @Test
    @WithMockUser(roles = "USER")
    void getBySourceSystem_knownFileType_returns200WithNonEmptyArray() throws Exception {
        mockMvc.perform(get("/v1/transformation/configs/by-system/" + TEST_FILE_TYPE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getBySourceSystem_unknownFileType_returns200WithEmptyArray() throws Exception {
        mockMvc.perform(get("/v1/transformation/configs/by-system/DOES_NOT_EXIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getBySourceSystem_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/v1/transformation/configs/by-system/" + TEST_FILE_TYPE))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /v1/transformation/configs/{configId} ─────────────────────────────

    @Test
    void getById_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/v1/transformation/configs/ANY-ID"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getById_seededId_returns200() throws Exception {
        mockMvc.perform(get("/v1/transformation/configs/" + TEST_ROW_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileType").value(TEST_FILE_TYPE));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/v1/transformation/configs/DEFINITELY-DOES-NOT-EXIST"))
                .andExpect(status().isNotFound());
    }

    // ── POST /v1/transformation/configs ───────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_validEntity_returns201() throws Exception {
        String body = """
                {
                    "fileType": "TEST_FT2",
                    "transactionType": "TEST_TX2",
                    "fieldName": "TEST_FLD2",
                    "targetPosition": 2,
                    "createdBy": "test-create"
                }
                """;
        mockMvc.perform(post("/v1/transformation/configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }

    // ── DELETE /v1/transformation/configs/{configId} ──────────────────────────

    @Test
    void delete_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/v1/transformation/configs/ANY-ID"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_wrongRole_returns403() throws Exception {
        mockMvc.perform(delete("/v1/transformation/configs/ANY-ID"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_seededId_returns204() throws Exception {
        mockMvc.perform(delete("/v1/transformation/configs/" + TEST_ROW_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_unknownId_returns404() throws Exception {
        mockMvc.perform(delete("/v1/transformation/configs/DEFINITELY-DOES-NOT-EXIST"))
                .andExpect(status().isNotFound());
    }
}
