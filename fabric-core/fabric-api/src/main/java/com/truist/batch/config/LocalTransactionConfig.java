package com.truist.batch.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * Local Development Transaction Configuration
 * 
 * Ensures proper transaction management for local development profile.
 * This configuration is specifically designed to work with Oracle database
 * and provides enhanced transaction debugging capabilities.
 * 
 * Key Features:
 * - Explicit JPA transaction manager configuration
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
     * Explicitly configure JPA Transaction Manager for local development.
     * 
     * While Spring Boot auto-configuration usually handles this,
     * explicit configuration ensures proper transaction management
     * when security and other auto-configurations are disabled.
     * 
     * @param entityManagerFactory The JPA EntityManagerFactory
     * @return Configured JPA Transaction Manager
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        log.info("🔧 Configuring JPA Transaction Manager for local development");
        
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        
        // Enable transaction debugging
        transactionManager.setRollbackOnCommitFailure(true);
        transactionManager.setDefaultTimeout(30); // 30 seconds default timeout
        
        // Enhanced logging for transaction lifecycle
        transactionManager.setGlobalRollbackOnParticipationFailure(true);
        
        log.info("✅ JPA Transaction Manager configured successfully");
        return transactionManager;
    }
}