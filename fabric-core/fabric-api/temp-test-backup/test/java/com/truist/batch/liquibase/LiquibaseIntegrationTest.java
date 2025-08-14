package com.truist.batch.liquibase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Liquibase database schema creation.
 * 
 * Tests US001 Manual Job Configuration database schema implementation:
 * - Validates Liquibase integration with Spring Boot
 * - Verifies MANUAL_JOB_CONFIG table creation
 * - Tests database connectivity and schema structure
 * 
 * Enterprise Standards:
 * - Banking-grade testing approach
 * - SOX-compliant audit verification
 * - Configuration-first testing methodology
 */
@SpringBootTest
@ActiveProfiles("test")
class LiquibaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testLiquibaseSchemaCreation() {
        // Verify that MANUAL_JOB_CONFIG table exists
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'MANUAL_JOB_CONFIG'";
        Integer tableCount = jdbcTemplate.queryForObject(sql, Integer.class);
        
        assertEquals(1, tableCount, "MANUAL_JOB_CONFIG table should exist");
    }

    @Test
    void testManualJobConfigTableStructure() {
        // Test that we can perform basic CRUD operations on the table
        String insertSql = """
            INSERT INTO MANUAL_JOB_CONFIG 
            (CONFIG_ID, JOB_NAME, JOB_TYPE, SOURCE_SYSTEM, TARGET_SYSTEM, 
             JOB_PARAMETERS, STATUS, CREATED_BY, VERSION_NUMBER) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        int rowsAffected = jdbcTemplate.update(insertSql,
            "test-config-001",
            "Test ETL Job", 
            "ETL",
            "HR_SYSTEM",
            "DATA_WAREHOUSE",
            "{'sourceTable': 'employees', 'targetTable': 'dim_employees'}",
            "ACTIVE",
            "system",
            1
        );
        
        assertEquals(1, rowsAffected, "Should insert one row successfully");
        
        // Verify the data was inserted correctly
        String selectSql = "SELECT COUNT(*) FROM MANUAL_JOB_CONFIG WHERE CONFIG_ID = ?";
        Integer count = jdbcTemplate.queryForObject(selectSql, Integer.class, "test-config-001");
        
        assertEquals(1, count, "Should find the inserted configuration");
        
        // Verify key fields
        String jobNameSql = "SELECT JOB_NAME FROM MANUAL_JOB_CONFIG WHERE CONFIG_ID = ?";
        String jobName = jdbcTemplate.queryForObject(jobNameSql, String.class, "test-config-001");
        
        assertEquals("Test ETL Job", jobName, "Job name should match inserted value");
    }

    @Test
    void testDatabaseChangelogTable() {
        // Verify that Liquibase changelog table exists
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'DATABASECHANGELOG'";
        Integer tableCount = jdbcTemplate.queryForObject(sql, Integer.class);
        
        assertEquals(1, tableCount, "DATABASECHANGELOG table should exist");
        
        // Verify our changeset was applied
        String changesetSql = """
            SELECT COUNT(*) FROM DATABASECHANGELOG 
            WHERE ID = 'us001-simple-manual-job-config-table' 
            AND AUTHOR = 'Senior Full Stack Developer Agent'
            """;
        Integer changesetCount = jdbcTemplate.queryForObject(changesetSql, Integer.class);
        
        assertEquals(1, changesetCount, "US001 changeset should be recorded in database changelog");
    }

    @Test
    void testDatabaseConnectivity() {
        // Basic connectivity test
        String sql = "SELECT 1";
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class);
        
        assertEquals(1, result, "Database connectivity should work");
    }
}