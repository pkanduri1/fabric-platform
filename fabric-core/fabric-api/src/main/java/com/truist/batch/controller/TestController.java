package com.truist.batch.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for database connectivity and master query data population
 */
@RestController
@RequestMapping("/test")
public class TestController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestController.class);
    
    @Autowired
    private DataSource primaryDataSource;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of(
            "status", "success",
            "message", "Hello from Fabric API!",
            "timestamp", String.valueOf(System.currentTimeMillis())
        );
    }
    
    @GetMapping("/db-health")
    public Map<String, Object> testDatabaseConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            // Test database connection
            String dbVersion = jdbcTemplate.queryForObject("SELECT BANNER FROM V$VERSION WHERE ROWNUM = 1", String.class);
            
            // Test schema access
            Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('TEMPLATE_MASTER_QUERY_MAPPING', 'MASTER_QUERY_COLUMNS')", 
                Integer.class
            );
            
            result.put("status", "success");
            result.put("database_version", dbVersion);
            result.put("master_query_tables_exist", tableCount == 2);
            result.put("connection_pool", primaryDataSource.getClass().getSimpleName());
            result.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
        }
        return result;
    }
    
    @PostMapping("/populate-master-queries")
    public Map<String, Object> populateMasterQueryData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("Starting master query data population...");
            
            // Read and execute the SQL script
            ClassPathResource resource = new ClassPathResource("data-master-query.sql");
            StringBuilder sqlScript = new StringBuilder();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sqlScript.append(line).append("\n");
                }
            }
            
            // Split script into individual statements and execute
            String[] statements = sqlScript.toString().split(";");
            int executedStatements = 0;
            
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && 
                    !trimmed.startsWith("--") && 
                    !trimmed.startsWith("/*") &&
                    !trimmed.toLowerCase().contains("commit")) {
                    try {
                        jdbcTemplate.execute(trimmed);
                        executedStatements++;
                    } catch (Exception e) {
                        logger.warn("Failed to execute statement: {}", trimmed.substring(0, Math.min(100, trimmed.length())));
                        logger.warn("Error: {}", e.getMessage());
                    }
                }
            }
            
            // Verify data was populated
            Integer mappingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM TEMPLATE_MASTER_QUERY_MAPPING WHERE MASTER_QUERY_ID LIKE 'mq_%'", 
                Integer.class
            );
            
            Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MASTER_QUERY_COLUMNS WHERE MASTER_QUERY_ID LIKE 'mq_%'", 
                Integer.class
            );
            
            result.put("status", "success");
            result.put("executed_statements", executedStatements);
            result.put("master_query_mappings_created", mappingCount);
            result.put("master_query_columns_created", columnCount);
            result.put("timestamp", System.currentTimeMillis());
            
            logger.info("Master query data population completed successfully. Mappings: {}, Columns: {}", 
                       mappingCount, columnCount);
            
        } catch (Exception e) {
            logger.error("Failed to populate master query data", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
        }
        
        return result;
    }
    
    @GetMapping("/master-query-summary")
    public Map<String, Object> getMasterQuerySummary() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get system breakdown
            String systemBreakdownQuery = """
                SELECT SUBSTR(MASTER_QUERY_ID, 1, 10) as system_prefix, COUNT(*) as query_count
                FROM TEMPLATE_MASTER_QUERY_MAPPING
                WHERE MASTER_QUERY_ID LIKE 'mq_%'
                GROUP BY SUBSTR(MASTER_QUERY_ID, 1, 10)
                ORDER BY system_prefix
                """;
            
            Map<String, Integer> systemBreakdown = new HashMap<>();
            jdbcTemplate.query(systemBreakdownQuery, rs -> {
                systemBreakdown.put(rs.getString("system_prefix"), rs.getInt("query_count"));
            });
            
            // Get total counts
            Integer totalMappings = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM TEMPLATE_MASTER_QUERY_MAPPING WHERE MASTER_QUERY_ID LIKE 'mq_%'", 
                Integer.class
            );
            
            Integer totalColumns = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MASTER_QUERY_COLUMNS WHERE MASTER_QUERY_ID LIKE 'mq_%'", 
                Integer.class
            );
            
            Integer sensitiveColumns = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MASTER_QUERY_COLUMNS WHERE IS_SENSITIVE_DATA = 'Y'", 
                Integer.class
            );
            
            result.put("status", "success");
            result.put("total_master_queries", totalMappings);
            result.put("total_columns", totalColumns);
            result.put("sensitive_columns", sensitiveColumns);
            result.put("system_breakdown", systemBreakdown);
            result.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            logger.error("Failed to get master query summary", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
        }
        
        return result;
    }
}