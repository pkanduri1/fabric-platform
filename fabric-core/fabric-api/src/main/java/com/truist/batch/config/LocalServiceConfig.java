package com.truist.batch.config;

import com.truist.batch.service.SqlLoaderConfigurationManagementService;
import com.truist.batch.service.impl.SqlLoaderConfigurationManagementServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Local configuration to provide stub implementations for external services
 */
@Configuration
@Profile("local") 
public class LocalServiceConfig {

    @Bean("sqlLoaderConfigurationManagementService")
    @Primary
    public SqlLoaderConfigurationManagementService sqlLoaderConfigurationManagementService() {
        return new SqlLoaderConfigurationManagementServiceImpl();
    }
    
    @Bean("partitionColumn")
    public String partitionColumn() {
        return "transaction_type";
    }
    
    @Bean("partitionSize")
    @Primary
    public String partitionSize() {
        return "1000";
    }
}