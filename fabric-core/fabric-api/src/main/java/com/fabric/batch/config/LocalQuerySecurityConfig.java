package com.fabric.batch.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Local Query Security Configuration for Oracle Database
 *
 * This configuration provides Oracle read-only datasource
 * for local development and testing scenarios.
 */
@Configuration
@Profile("local")
public class LocalQuerySecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(LocalQuerySecurityConfig.class);

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    /**
     * Configure Oracle read-only datasource for local testing.
     */
    @Bean("readOnlyDataSource")
    public DataSource readOnlyDataSource() {
        logger.info("Configuring Oracle read-only datasource for local testing");

        HikariConfig config = new HikariConfig();

        // Oracle Database configuration - injected from application-local.properties
        config.setJdbcUrl(dbUrl);
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);

        // Basic pool configuration for local testing
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setPoolName("LocalReadOnlyQueryPool");

        // Oracle-specific validation
        config.setConnectionTestQuery("SELECT 1 FROM DUAL");
        config.setValidationTimeout(3000);

        logger.info("Local Oracle read-only datasource configured with pool size: {} connections",
                   config.getMaximumPoolSize());

        return new HikariDataSource(config);
    }

    /**
     * Configure JdbcTemplate for Oracle read-only operations.
     */
    @Bean("readOnlyJdbcTemplate")
    public JdbcTemplate readOnlyJdbcTemplate(@Qualifier("readOnlyDataSource") DataSource readOnlyDataSource) {
        logger.info("Configuring Oracle read-only JdbcTemplate for local testing");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(readOnlyDataSource);

        // Relaxed settings for local testing
        jdbcTemplate.setQueryTimeout(30);
        jdbcTemplate.setFetchSize(100);
        jdbcTemplate.setMaxRows(100);

        logger.info("Oracle read-only JdbcTemplate configured for local testing");

        return jdbcTemplate;
    }
}
