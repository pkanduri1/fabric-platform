package com.truist.batch.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local data source configuration that overrides external DatabaseConfig
 * to properly configure Oracle for local development and testing
 */
@Configuration
@Profile("local")
public class LocalDataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(LocalDataSourceConfig.class);

    @Bean
    @Primary
    public DataSource dataSource() {
        logger.info("Creating PRIMARY Oracle DataSource for local profile");
        DataSource ds = DataSourceBuilder.create()
                .driverClassName("oracle.jdbc.OracleDriver")
                .url("jdbc:oracle:thin:@localhost:1521/ORCLPDB1")
                .username("cm3int")
                .password("MySecurePass123")
                .build();
        logger.info("PRIMARY Oracle DataSource created successfully");
        return ds;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        logger.info("Creating PRIMARY JdbcTemplate using Oracle DataSource for local profile");
        JdbcTemplate template = new JdbcTemplate(dataSource);
        logger.info("PRIMARY JdbcTemplate created successfully for Oracle");
        return template;
    }

}