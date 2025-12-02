package com.fabric.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.fabric.batch.config.DatabaseConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication(exclude = {
        BatchAutoConfiguration.class
        // Exclude auto-configurations handled in application.properties
})
@ComponentScan(basePackages = {
        "com.fabric.batch.repository",
        "com.fabric.batch.dao",
        "com.fabric.batch.config",
        "com.fabric.batch.security",
        "com.fabric.batch.audit",
        "com.fabric.batch.service",
        "com.fabric.batch.controller",
        "com.fabric.batch.sqlloader",
        "com.fabric.batch.mapping",
        "com.fabric.batch.adapter",
        "com.fabric.batch.model",
        "com.fabric.batch.listener",
        "com.fabric.batch.tasklet",
        "com.fabric.batch.util",
        "com.fabric.batch.launcher"
}, includeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.truist\\.batch\\.controller\\.ManualJobConfigController"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.truist\\.batch\\.service\\.ManualJobConfigService"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.truist\\.batch\\.service\\.LocalManualJobExecutionService"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.truist\\.batch\\.service\\.LocalConfigurationService"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.truist\\.batch\\.controller\\.UIController"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.truist\\.batch\\.controller\\.SourceSystemController"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.truist\\.batch\\.controller\\.TemplateController")
}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = DatabaseConfig.class),
        /*
         * @ComponentScan.Filter(
         * type = FilterType.REGEX,
         * pattern =
         * "com\\.truist\\.batch\\.config\\.(GenericJobConfig|P327JobConfig).*"
         * ),
         */
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.truist\\.batch\\.service\\.AuditService"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.truist\\.batch\\.service\\.impl\\.SqlLoaderConfigServiceImpl"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.truist\\.batch\\.controller\\.SqlLoaderController")
})
@EnableTransactionManagement
public class InterfaceBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(InterfaceBatchApplication.class, args);
    }

}
