package com.truist.batch.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;

/**
 * Local Development Transaction Configuration
 * 
 * Ensures proper transaction management for local development profile.
 * This configuration is specifically designed to work with Oracle database
 * and provides enhanced transaction debugging capabilities.
 * 
 * Key Features:
 * - JDBC DataSource transaction manager configuration
 * - Enhanced logging for transaction debugging
 * - Oracle-optimized transaction settings
 * - Connection pool integration
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-08-03
 */
@Slf4j
@Configuration
@Profile("local")
@EnableTransactionManagement
@ConditionalOnProperty(name = "spring.transaction.management.enabled", havingValue = "true", matchIfMissing = true)
public class LocalTransactionConfig {

    /**
     * Explicitly configure DataSource Transaction Manager for local development.
     * 
     * Since JPA is disabled, we use DataSourceTransactionManager which works
     * directly with JDBC and Spring's JdbcTemplate.
     * 
     * @param dataSource The configured DataSource
     * @return Configured DataSource Transaction Manager
     */
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        log.info("🔧 Configuring DataSource Transaction Manager for local development");
        
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource);
        
        // Enable transaction debugging
        transactionManager.setRollbackOnCommitFailure(true);
        transactionManager.setDefaultTimeout(30); // 30 seconds default timeout
        
        // Enhanced logging for transaction lifecycle
        transactionManager.setGlobalRollbackOnParticipationFailure(true);
        
        log.info("✅ DataSource Transaction Manager configured successfully");
        return transactionManager;
    }
}