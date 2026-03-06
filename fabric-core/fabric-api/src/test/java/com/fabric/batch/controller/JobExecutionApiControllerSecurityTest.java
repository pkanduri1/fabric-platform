package com.fabric.batch.controller;

import com.fabric.batch.config.QuerySecurityConfig;
import com.fabric.batch.service.JobExecutionApiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import javax.sql.DataSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:sectest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=password",
    "spring.datasource.primary.url=jdbc:h2:mem:sectest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.primary.driver-class-name=org.h2.Driver",
    "spring.datasource.primary.username=sa",
    "spring.datasource.primary.password=password",
    "spring.datasource.readonly.url=jdbc:h2:mem:sectest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.readonly.driver-class-name=org.h2.Driver",
    "spring.datasource.readonly.username=sa",
    "spring.datasource.readonly.password=password",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.liquibase.enabled=false",
    "spring.sql.init.mode=never",
    "fabric.security.csrf.enabled=false"
})
class JobExecutionApiControllerSecurityTest {

    @TestConfiguration
    static class H2TestDataSourceConfig {

        @Bean(name = "dataSource")
        @Primary
        public DataSource dataSource() {
            return DataSourceBuilder.create()
                    .driverClassName("org.h2.Driver")
                    .url("jdbc:h2:mem:sectest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                    .username("sa").password("password").build();
        }

        @Bean(name = "readOnlyDataSource")
        public DataSource readOnlyDataSource() {
            return DataSourceBuilder.create()
                    .driverClassName("org.h2.Driver")
                    .url("jdbc:h2:mem:sectest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
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
    @MockBean JobExecutionApiService service;

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

    @Test
    @WithMockUser(roles = "API_EXECUTOR")
    void status_correctRole_notRejectedBySecurity() throws Exception {
        // Mocked service returns null → 200. Just checking security passes (not 401/403).
        mockMvc.perform(get("/api/v1/jobs/EXEC-9999/status"))
                .andExpect(status().is2xxSuccessful());
    }
}
