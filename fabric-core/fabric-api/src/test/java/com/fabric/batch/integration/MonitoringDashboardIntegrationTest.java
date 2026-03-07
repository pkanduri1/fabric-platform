package com.fabric.batch.integration;

import com.fabric.batch.config.QuerySecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class MonitoringDashboardIntegrationTest {

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

    // No seed/cleanup needed — dashboard reads existing data, no test data mutations

    @Test
    @WithMockUser(roles = "OPERATIONS_MANAGER")
    void dashboard_withOpsManagerRole_returns200WithFullStructure() throws Exception {
        mockMvc.perform(get("/api/monitoring/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(jsonPath("$.data.activeJobs").isArray())
                .andExpect(jsonPath("$.data.recentCompletions").isArray())
                .andExpect(jsonPath("$.data.performanceMetrics").isMap())
                .andExpect(jsonPath("$.data.performanceMetrics.successRate").isNumber())
                .andExpect(jsonPath("$.data.performanceMetrics.memoryUsage").isNumber())
                .andExpect(jsonPath("$.data.performanceMetrics.cpuUsage").isNumber())
                .andExpect(jsonPath("$.data.systemHealth").isMap())
                .andExpect(jsonPath("$.data.systemHealth.overallScore").isNumber())
                .andExpect(jsonPath("$.data.systemHealth.database.status").isString())
                .andExpect(jsonPath("$.data.systemHealth.memory.status").isString())
                .andExpect(jsonPath("$.data.alerts").isArray())
                .andExpect(jsonPath("$.data.trends").isMap())
                .andExpect(jsonPath("$.data.trends.period").value("DAY"))
                .andExpect(jsonPath("$.data.lastUpdate").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboard_withAdminRole_returns200() throws Exception {
        mockMvc.perform(get("/api/monitoring/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "JOB_VIEWER")
    void dashboard_withJobViewerRole_returns200() throws Exception {
        mockMvc.perform(get("/api/monitoring/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void dashboard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/monitoring/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "JOB_CREATOR")
    void dashboard_withWrongRole_returns403() throws Exception {
        mockMvc.perform(get("/api/monitoring/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATIONS_MANAGER")
    void dashboard_systemHealth_databaseIsHealthy() throws Exception {
        mockMvc.perform(get("/api/monitoring/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.systemHealth.database.status").value("HEALTHY"))
                .andExpect(jsonPath("$.data.systemHealth.database.responseTime", greaterThanOrEqualTo(0.0)));
    }

    @Test
    @WithMockUser(roles = "OPERATIONS_MANAGER")
    void dashboard_performanceMetrics_hasValidValues() throws Exception {
        mockMvc.perform(get("/api/monitoring/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceMetrics.successRate",
                        allOf(greaterThanOrEqualTo(0.0), lessThanOrEqualTo(100.0))))
                .andExpect(jsonPath("$.data.performanceMetrics.errorRate",
                        allOf(greaterThanOrEqualTo(0.0), lessThanOrEqualTo(100.0))))
                .andExpect(jsonPath("$.data.performanceMetrics.memoryUsage",
                        greaterThanOrEqualTo(0.0)));
    }
}
