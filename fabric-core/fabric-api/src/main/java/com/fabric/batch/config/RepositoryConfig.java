package com.fabric.batch.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for JdbcTemplate-based repositories
 * 
 * Ensures that JdbcTemplate repository implementations are properly scanned and registered
 */
@Configuration
@ComponentScan(basePackages = "com.fabric.batch.repository.impl")
public class RepositoryConfig {
    // Component scanning configuration for repository implementations
}