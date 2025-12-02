package com.fabric.batch.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Local Query Security Configuration for H2 Database Testing
 * 
 * This configuration provides H2-compatible read-only datasource 
 * for local development and testing scenarios.
 */
@Configuration
@Profile("local")
public class LocalQuerySecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(LocalQuerySecurityConfig.class);

    /**
     * Configure H2 read-only datasource for local testing.
     */
    @Bean("readOnlyDataSource")
    public DataSource readOnlyDataSource() {
        logger.info("Configuring H2 read-only datasource for local testing");
        
        HikariConfig config = new HikariConfig();
        
        // Oracle Database configuration for local development
        config.setJdbcUrl("jdbc:oracle:thin:@localhost:1521/ORCLPDB1");
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setUsername("cm3int");
        config.setPassword("MySecurePass123");
        
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
     * Configure JdbcTemplate for H2 read-only operations.
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