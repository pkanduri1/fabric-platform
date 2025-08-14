package com.truist.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.truist.batch.config.DatabaseConfig;

@SpringBootApplication(
    exclude = {
        // Exclude auto-configurations handled in application.properties
    }
)
@ComponentScan(
    basePackages = "com.truist.batch",
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE, 
            classes = DatabaseConfig.class
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX, 
            pattern = ".*\\.SqlLoaderConfigurationManagementService$"
        )
    }
)
@EnableTransactionManagement
public class InterfaceBatchApplication {
	public static void main(String[] args) {
		SpringApplication.run(InterfaceBatchApplication.class, args);
	}

}
