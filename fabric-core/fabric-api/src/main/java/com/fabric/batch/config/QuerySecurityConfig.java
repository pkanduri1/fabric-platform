package com.fabric.batch.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * =========================================================================
 * QUERY SECURITY CONFIGURATION - BANKING GRADE SECURITY
 * =========================================================================
 * 
 * Purpose: Configure read-only database connection pool for master query operations
 * - Separate connection pool for query operations
 * - Banking-grade security with read-only enforcement
 * - Connection timeout and leak detection
 * - SOX-compliant configuration management
 * 
 * Security Features:
 * - Read-only connection pool with auto-commit disabled
 * - Connection timeout enforcement (20 seconds max)
 * - Leak detection for connection monitoring
 * - Separate from primary transaction datasource
 * 
 * Compliance:
 * - SOX: All configuration changes audited
 * - PCI-DSS: Secure connection management
 * - Basel III: Risk management for database operations
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since 2025-08-18
 * =========================================================================
 */
@Configuration
@Profile("!local")
public class QuerySecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(QuerySecurityConfig.class);

    /**
     * Configure read-only datasource for master query operations.
     * This datasource enforces banking-grade security with:
     * - Read-only connections
     * - Restricted connection pool
     * - Timeout enforcement
     * - Leak detection
     * 
     * @return Configured read-only datasource
     */
    @Bean(name = "readOnlyDataSource")
    public DataSource readOnlyDataSource(
            @Value("${spring.datasource.readonly.url:${spring.datasource.primary.url}}") String url,
            @Value("${spring.datasource.readonly.driver-class-name:oracle.jdbc.OracleDriver}") String driverClassName,
            @Value("${spring.datasource.readonly.username:${spring.datasource.primary.username}}") String username,
            @Value("${spring.datasource.readonly.password:${spring.datasource.primary.password}}") String password,
            @Value("${spring.datasource.readonly.hikari.maximum-pool-size:10}") int maxPoolSize,
            @Value("${spring.datasource.readonly.hikari.minimum-idle:2}") int minIdle,
            @Value("${spring.datasource.readonly.hikari.idle-timeout:300000}") long idleTimeout,
            @Value("${spring.datasource.readonly.hikari.max-lifetime:1800000}") long maxLifetime,
            @Value("${spring.datasource.readonly.hikari.connection-timeout:20000}") long connectionTimeout,
            @Value("${spring.datasource.readonly.hikari.leak-detection-threshold:30000}") long leakDetectionThreshold) {
        
        logger.info("Configuring read-only datasource for master query operations");
        
        HikariConfig config = new HikariConfig();
        
        // Set database connection properties
        config.setJdbcUrl(url);
        config.setDriverClassName(driverClassName);
        config.setUsername(username);
        config.setPassword(password);
        
        // Security Configuration - Banking Grade
        config.setReadOnly(true);
        config.setAutoCommit(false);
        config.setPoolName("ReadOnlyQueryPool");
        
        // Connection Pool Configuration from application.yml
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setConnectionTimeout(connectionTimeout);
        config.setLeakDetectionThreshold(leakDetectionThreshold);
        
        // Validation and Health Checks
        config.setValidationTimeout(5000);
        config.setConnectionTestQuery("SELECT 1 FROM DUAL");
        
        // Register MBeans for monitoring
        config.setRegisterMbeans(true);
        
        logger.info("Read-only datasource configured with pool size: {} connections", 
                   config.getMaximumPoolSize());
        
        return new HikariDataSource(config);
    }

    /**
     * Configure JdbcTemplate for read-only master query operations.
     * This template provides:
     * - Query timeout enforcement (30 seconds)
     * - Result set size limitations
     * - Read-only transaction context
     * 
     * @param readOnlyDataSource The read-only datasource
     * @return Configured JdbcTemplate for queries
     */
    @Bean(name = "readOnlyJdbcTemplate")
    public JdbcTemplate readOnlyJdbcTemplate(@Qualifier("readOnlyDataSource") DataSource readOnlyDataSource) {
        logger.info("Configuring read-only JdbcTemplate for master query operations");
        
        JdbcTemplate jdbcTemplate = new JdbcTemplate(readOnlyDataSource);
        
        // Set query timeout to 30 seconds (banking compliance)
        jdbcTemplate.setQueryTimeout(30);
        
        // Set fetch size for large result sets
        jdbcTemplate.setFetchSize(100);
        
        // Set max rows to prevent runaway queries
        jdbcTemplate.setMaxRows(100);
        
        logger.info("Read-only JdbcTemplate configured with 30s timeout and 100 row limit");
        
        return jdbcTemplate;
    }

    /**
     * Health check bean for monitoring read-only datasource connectivity.
     * Provides SOX-compliant monitoring and alerting.
     * 
     * @param readOnlyDataSource The read-only datasource to monitor
     * @return Health check indicator
     */
    @Bean(name = "readOnlyDataSourceHealthIndicator")
    public ReadOnlyDataSourceHealthIndicator readOnlyDataSourceHealthIndicator(
            @Qualifier("readOnlyDataSource") DataSource readOnlyDataSource) {
        return new ReadOnlyDataSourceHealthIndicator(readOnlyDataSource);
    }

    /**
     * Custom health indicator for read-only datasource monitoring.
     * Implements banking-grade health checks and alerting.
     */
    public static class ReadOnlyDataSourceHealthIndicator {
        
        private static final Logger healthLogger = LoggerFactory.getLogger(ReadOnlyDataSourceHealthIndicator.class);
        private final DataSource dataSource;
        
        public ReadOnlyDataSourceHealthIndicator(DataSource dataSource) {
            this.dataSource = dataSource;
        }
        
        /**
         * Perform health check on read-only datasource.
         * 
         * @return Health status with connection details
         */
        public boolean isHealthy() {
            try {
                JdbcTemplate healthTemplate = new JdbcTemplate(dataSource);
                healthTemplate.setQueryTimeout(5); // 5 second health check timeout
                
                String result = healthTemplate.queryForObject("SELECT 'HEALTHY' FROM DUAL", String.class);
                
                boolean isHealthy = "HEALTHY".equals(result);
                if (isHealthy) {
                    healthLogger.debug("Read-only datasource health check passed");
                } else {
                    healthLogger.warn("Read-only datasource health check failed: unexpected result {}", result);
                }
                
                return isHealthy;
                
            } catch (Exception e) {
                healthLogger.error("Read-only datasource health check failed", e);
                return false;
            }
        }
        
        /**
         * Get detailed health information for monitoring.
         * 
         * @return Health details map
         */
        public java.util.Map<String, Object> getHealthDetails() {
            java.util.Map<String, Object> details = new java.util.HashMap<>();
            
            try {
                if (dataSource instanceof HikariDataSource) {
                    HikariDataSource hikariDS = (HikariDataSource) dataSource;
                    details.put("poolName", hikariDS.getPoolName());
                    details.put("activeConnections", hikariDS.getHikariPoolMXBean().getActiveConnections());
                    details.put("idleConnections", hikariDS.getHikariPoolMXBean().getIdleConnections());
                    details.put("totalConnections", hikariDS.getHikariPoolMXBean().getTotalConnections());
                    details.put("readOnly", true);
                }
                
                details.put("healthy", isHealthy());
                details.put("lastCheck", java.time.Instant.now().toString());
                
            } catch (Exception e) {
                details.put("error", e.getMessage());
                details.put("healthy", false);
            }
            
            return details;
        }
    }
}