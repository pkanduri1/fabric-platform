package com.fabric.batch.config;

import com.fabric.batch.repository.DataLoadAuditRepository;
import com.fabric.batch.repository.DataLoadAuditRepositoryJdbc;
import com.fabric.batch.repository.impl.DataLoadAuditRepositoryJdbcImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Configuration for JdbcTemplate-based repositories in fabric-data-loader.
 * This provides a fallback when JPA repositories are not available.
 */
@Configuration
@ComponentScan(basePackages = "com.fabric.batch", 
               excludeFilters = @ComponentScan.Filter(
                   type = FilterType.ASSIGNABLE_TYPE, 
                   classes = DataLoadAuditRepository.class))
public class DataLoaderJdbcConfig {

    @Bean("auditRepository")
    @Primary
    @ConditionalOnMissingBean(name = "dataLoadAuditRepository")
    public DataLoadAuditRepositoryJdbc auditRepository(JdbcTemplate jdbcTemplate) {
        DataLoadAuditRepositoryJdbcImpl repository = new DataLoadAuditRepositoryJdbcImpl();
        return repository;
    }
}