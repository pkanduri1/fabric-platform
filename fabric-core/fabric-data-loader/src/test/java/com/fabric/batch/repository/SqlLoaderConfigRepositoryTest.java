package com.fabric.batch.repository;

import com.fabric.batch.entity.SqlLoaderConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for SqlLoaderConfigRepository.
 * Tests repository operations, custom queries, and database interactions.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SQL*Loader Configuration Repository Tests")
class SqlLoaderConfigRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private SqlLoaderConfigRepository repository;
    
    private SqlLoaderConfigEntity testConfig1;
    private SqlLoaderConfigEntity testConfig2;
    private SqlLoaderConfigEntity testConfig3;
    
    @BeforeEach
    void setUp() {
        // Create test configurations
        testConfig1 = SqlLoaderConfigEntity.builder()
                .configId("TEST-HR-001")
                .jobName("p327")
                .sourceSystem("HR")
                .targetTable("CM3INT.HR_P327_DATA")
                .loadMethod(SqlLoaderConfigEntity.LoadMethod.INSERT)
                .directPath("Y")
                .parallelDegree(2)
                .maxErrors(1000)
                .fieldDelimiter("|")
                .encryptionRequired("N")
                .dataClassification(SqlLoaderConfigEntity.DataClassification.INTERNAL)
                .validationEnabled("Y")
                .auditTrailRequired("Y")
                .retentionDays(2555)
                .createdBy("test-admin")
                .createdDate(LocalDateTime.now().minusDays(5))
                .version(1)
                .enabled("Y")
                .build();
        
        testConfig2 = SqlLoaderConfigEntity.builder()
                .configId("TEST-DDA-001")
                .jobName("accounts")
                .sourceSystem("DDA")
                .targetTable("CM3INT.DDA_ACCOUNTS")
                .loadMethod(SqlLoaderConfigEntity.LoadMethod.APPEND)
                .directPath("Y")
                .parallelDegree(4)
                .maxErrors(500)
                .fieldDelimiter(",")
                .encryptionRequired("Y")
                .piiFields("SSN,EMAIL,PHONE")
                .dataClassification(SqlLoaderConfigEntity.DataClassification.CONFIDENTIAL)
                .regulatoryCompliance("SOX,PCI-DSS")
                .validationEnabled("Y")
                .auditTrailRequired("Y")
                .retentionDays(2555)
                .createdBy("test-admin")
                .createdDate(LocalDateTime.now().minusDays(3))
                .version(1)
                .enabled("Y")
                .build();
        
        testConfig3 = SqlLoaderConfigEntity.builder()
                .configId("TEST-HR-002")
                .jobName("p329")
                .sourceSystem("HR")
                .targetTable("CM3INT.HR_P329_DATA")
                .loadMethod(SqlLoaderConfigEntity.LoadMethod.REPLACE)
                .directPath("N")
                .parallelDegree(1)
                .maxErrors(2000)
                .fieldDelimiter("|")
                .encryptionRequired("N")
                .dataClassification(SqlLoaderConfigEntity.DataClassification.INTERNAL)
                .validationEnabled("N")
                .auditTrailRequired("Y")
                .retentionDays(1825)
                .createdBy("test-user")
                .createdDate(LocalDateTime.now().minusDays(1))
                .version(1)
                .enabled("N") // Disabled config
                .build();
        
        // Persist test data
        entityManager.persistAndFlush(testConfig1);
        entityManager.persistAndFlush(testConfig2);
        entityManager.persistAndFlush(testConfig3);
        entityManager.clear();
    }
    
    @Nested
    @DisplayName("Basic Repository Operations")
    class BasicRepositoryOperationsTests {
        
        @Test
        @DisplayName("Should save and retrieve configuration")
        void shouldSaveAndRetrieveConfiguration() {
            SqlLoaderConfigEntity newConfig = SqlLoaderConfigEntity.builder()
                    .configId("TEST-NEW-001")
                    .jobName("new-job")
                    .sourceSystem("NEW")
                    .targetTable("CM3INT.NEW_TABLE")
                    .createdBy("admin")
                    .build();
            
            SqlLoaderConfigEntity saved = repository.save(newConfig);
            
            assertNotNull(saved);
            assertEquals("TEST-NEW-001", saved.getConfigId());
            
            Optional<SqlLoaderConfigEntity> retrieved = repository.findById("TEST-NEW-001");
            assertTrue(retrieved.isPresent());
            assertEquals("new-job", retrieved.get().getJobName());
        }
        
        @Test
        @DisplayName("Should count total configurations")
        void shouldCountTotalConfigurations() {
            long totalCount = repository.count();
            assertEquals(3, totalCount);
        }
        
        @Test
        @DisplayName("Should delete configuration")
        void shouldDeleteConfiguration() {
            repository.deleteById("TEST-HR-001");
            
            Optional<SqlLoaderConfigEntity> deleted = repository.findById("TEST-HR-001");
            assertFalse(deleted.isPresent());
        }
    }
    
    @Nested
    @DisplayName("Custom Query Methods Tests")
    class CustomQueryMethodsTests {
        
        @Test
        @DisplayName("Should find configuration by source system and job name")
        void shouldFindConfigurationBySourceSystemAndJobName() {
            Optional<SqlLoaderConfigEntity> found = repository
                    .findBySourceSystemAndJobNameAndEnabledOrderByVersionDesc("HR", "p327", "Y");
            
            assertTrue(found.isPresent());
            assertEquals("TEST-HR-001", found.get().getConfigId());
            assertEquals("p327", found.get().getJobName());
        }
        
        @Test
        @DisplayName("Should find configurations by source system")
        void shouldFindConfigurationsBySourceSystem() {
            List<SqlLoaderConfigEntity> hrConfigs = repository
                    .findBySourceSystemAndEnabledOrderByJobName("HR", "Y");
            
            assertEquals(1, hrConfigs.size()); // Only enabled HR config
            assertEquals("p327", hrConfigs.get(0).getJobName());
        }
        
        @Test
        @DisplayName("Should find configurations by target table")
        void shouldFindConfigurationsByTargetTable() {
            List<SqlLoaderConfigEntity> configs = repository
                    .findByTargetTableAndEnabledOrderByJobName("CM3INT.HR_P327_DATA", "Y");
            
            assertEquals(1, configs.size());
            assertEquals("TEST-HR-001", configs.get(0).getConfigId());
        }
        
        @Test
        @DisplayName("Should find configurations by load method")
        void shouldFindConfigurationsByLoadMethod() {
            List<SqlLoaderConfigEntity> insertConfigs = repository
                    .findByLoadMethodAndEnabledOrderByJobName(SqlLoaderConfigEntity.LoadMethod.INSERT, "Y");
            
            assertEquals(1, insertConfigs.size());
            assertEquals("TEST-HR-001", insertConfigs.get(0).getConfigId());
            
            List<SqlLoaderConfigEntity> appendConfigs = repository
                    .findByLoadMethodAndEnabledOrderByJobName(SqlLoaderConfigEntity.LoadMethod.APPEND, "Y");
            
            assertEquals(1, appendConfigs.size());
            assertEquals("TEST-DDA-001", appendConfigs.get(0).getConfigId());
        }
        
        @Test
        @DisplayName("Should find configurations with direct path enabled")
        void shouldFindConfigurationsWithDirectPathEnabled() {
            List<SqlLoaderConfigEntity> directPathConfigs = repository
                    .findByDirectPathAndEnabledOrderByJobName("Y", "Y");
            
            assertEquals(2, directPathConfigs.size()); // testConfig1 and testConfig2
        }
        
        @Test
        @DisplayName("Should find parallel configurations")
        void shouldFindParallelConfigurations() {
            List<SqlLoaderConfigEntity> parallelConfigs = repository
                    .findParallelConfigurationsAndEnabled("Y");
            
            assertEquals(2, parallelConfigs.size());
            // Should be ordered by parallel degree DESC
            assertEquals(Integer.valueOf(4), parallelConfigs.get(0).getParallelDegree());
            assertEquals(Integer.valueOf(2), parallelConfigs.get(1).getParallelDegree());
        }
    }
    
    @Nested
    @DisplayName("Security and Compliance Query Tests")
    class SecurityComplianceQueryTests {
        
        @Test
        @DisplayName("Should find configurations requiring encryption")
        void shouldFindConfigurationsRequiringEncryption() {
            List<SqlLoaderConfigEntity> encryptedConfigs = repository
                    .findByEncryptionRequiredAndEnabledOrderByJobName("Y", "Y");
            
            assertEquals(1, encryptedConfigs.size());
            assertEquals("TEST-DDA-001", encryptedConfigs.get(0).getConfigId());
        }
        
        @Test
        @DisplayName("Should find configurations with PII fields")
        void shouldFindConfigurationsWithPiiFields() {
            List<SqlLoaderConfigEntity> piiConfigs = repository
                    .findConfigurationsWithPiiFieldsAndEnabled("Y");
            
            assertEquals(1, piiConfigs.size());
            assertEquals("TEST-DDA-001", piiConfigs.get(0).getConfigId());
            assertTrue(piiConfigs.get(0).containsPiiData());
        }
        
        @Test
        @DisplayName("Should find configurations by data classification")
        void shouldFindConfigurationsByDataClassification() {
            List<SqlLoaderConfigEntity> internalConfigs = repository
                    .findByDataClassificationAndEnabledOrderByJobName(
                            SqlLoaderConfigEntity.DataClassification.INTERNAL, "Y");
            
            assertEquals(1, internalConfigs.size());
            assertEquals("TEST-HR-001", internalConfigs.get(0).getConfigId());
            
            List<SqlLoaderConfigEntity> confidentialConfigs = repository
                    .findByDataClassificationAndEnabledOrderByJobName(
                            SqlLoaderConfigEntity.DataClassification.CONFIDENTIAL, "Y");
            
            assertEquals(1, confidentialConfigs.size());
            assertEquals("TEST-DDA-001", confidentialConfigs.get(0).getConfigId());
        }
        
        @Test
        @DisplayName("Should find configurations by regulatory compliance")
        void shouldFindConfigurationsByRegulatoryCompliance() {
            List<SqlLoaderConfigEntity> pciConfigs = repository
                    .findByRegulatoryComplianceContainingAndEnabled("PCI", "Y");
            
            assertEquals(1, pciConfigs.size());
            assertEquals("TEST-DDA-001", pciConfigs.get(0).getConfigId());
        }
        
        @Test
        @DisplayName("Should find configurations requiring attention")
        void shouldFindConfigurationsRequiringAttention() {
            // This query should find configurations with potential security/compliance issues
            List<SqlLoaderConfigEntity> attentionConfigs = repository
                    .findConfigurationsRequiringAttention("Y");
            
            // Based on our test data, no configs should require attention as they are properly configured
            assertEquals(0, attentionConfigs.size());
        }
        
        @Test
        @DisplayName("Should get security compliance summary")
        void shouldGetSecurityComplianceSummary() {
            List<Object[]> summary = repository.getSecurityComplianceSummary("Y");
            
            assertNotNull(summary);
            assertEquals(2, summary.size()); // Two different classifications
            
            // Verify structure of results
            for (Object[] row : summary) {
                assertNotNull(row[0]); // Classification
                assertNotNull(row[1]); // Count
                assertNotNull(row[2]); // Encryption count
                assertNotNull(row[3]); // PII count
                assertNotNull(row[4]); // Audit trail count
            }
        }
    }
    
    @Nested
    @DisplayName("Performance and Metrics Query Tests")
    class PerformanceMetricsQueryTests {
        
        @Test
        @DisplayName("Should find configurations with high error thresholds")
        void shouldFindConfigurationsWithHighErrorThresholds() {
            List<SqlLoaderConfigEntity> highErrorConfigs = repository
                    .findByMaxErrorsGreaterThanAndEnabled(1500, "Y");
            
            assertEquals(0, highErrorConfigs.size()); // No configs have > 1500 errors threshold
            
            List<SqlLoaderConfigEntity> mediumErrorConfigs = repository
                    .findByMaxErrorsGreaterThanAndEnabled(750, "Y");
            
            assertEquals(1, mediumErrorConfigs.size());
            assertEquals("TEST-HR-001", mediumErrorConfigs.get(0).getConfigId());
        }
        
        @Test
        @DisplayName("Should find configurations by bind size range")
        void shouldFindConfigurationsByBindSizeRange() {
            List<SqlLoaderConfigEntity> configs = repository
                    .findByBindSizeBetweenAndEnabled(200000L, 300000L, "Y");
            
            assertEquals(2, configs.size()); // Both enabled configs have default bind size 256000
        }
        
        @Test
        @DisplayName("Should get performance configuration stats")
        void shouldGetPerformanceConfigurationStats() {
            List<Object[]> stats = repository.getPerformanceConfigurationStats("Y");
            
            assertNotNull(stats);
            assertEquals(2, stats.size()); // Two enabled configurations
            
            // Verify structure
            for (Object[] row : stats) {
                assertNotNull(row[0]); // Config ID
                assertNotNull(row[1]); // Job Name
                assertNotNull(row[2]); // Parallel Degree
                assertNotNull(row[3]); // Bind Size
                assertNotNull(row[4]); // Read Size
                assertNotNull(row[5]); // Direct Path
            }
        }
        
        @Test
        @DisplayName("Should get average configuration metrics")
        void shouldGetAverageConfigurationMetrics() {
            Object[] averages = repository.getAverageConfigurationMetrics("Y");
            
            assertNotNull(averages);
            assertEquals(5, averages.length);
            
            // Check that averages are reasonable
            Double avgMaxErrors = (Double) averages[0];
            Double avgParallelDegree = (Double) averages[1];
            Double avgBindSize = (Double) averages[2];
            Double avgReadSize = (Double) averages[3];
            Double avgRetentionDays = (Double) averages[4];
            
            assertNotNull(avgMaxErrors);
            assertNotNull(avgParallelDegree);
            assertNotNull(avgBindSize);
            assertNotNull(avgReadSize);
            assertNotNull(avgRetentionDays);
            
            assertTrue(avgMaxErrors > 0);
            assertTrue(avgParallelDegree > 0);
            assertTrue(avgBindSize > 0);
            assertTrue(avgReadSize > 0);
            assertTrue(avgRetentionDays > 0);
        }
    }
    
    @Nested
    @DisplayName("Statistical and Reporting Query Tests")
    class StatisticalReportingQueryTests {
        
        @Test
        @DisplayName("Should count configurations by source system")
        void shouldCountConfigurationsBySourceSystem() {
            Long hrCount = repository.countBySourceSystemAndEnabled("HR", "Y");
            assertEquals(1L, hrCount);
            
            Long ddaCount = repository.countBySourceSystemAndEnabled("DDA", "Y");
            assertEquals(1L, ddaCount);
            
            Long nonExistentCount = repository.countBySourceSystemAndEnabled("NONEXISTENT", "Y");
            assertEquals(0L, nonExistentCount);
        }
        
        @Test
        @DisplayName("Should get configuration stats by source system")
        void shouldGetConfigurationStatsBySourceSystem() {
            List<Object[]> stats = repository.getConfigurationStatsBySourceSystem("Y");
            
            assertNotNull(stats);
            assertEquals(2, stats.size()); // HR and DDA systems
            
            // Verify structure: sourceSystem, count, avgMaxErrors, avgParallelDegree, maxVersion
            for (Object[] row : stats) {
                assertNotNull(row[0]); // Source system
                assertNotNull(row[1]); // Count
                assertNotNull(row[2]); // Avg max errors
                assertNotNull(row[3]); // Max version
            }
        }
        
        @Test
        @DisplayName("Should get configuration count by classification")
        void shouldGetConfigurationCountByClassification() {
            List<Object[]> counts = repository.getConfigurationCountByClassification("Y");
            
            assertNotNull(counts);
            assertEquals(2, counts.size()); // INTERNAL and CONFIDENTIAL
            
            // Verify structure: classification, count
            for (Object[] row : counts) {
                assertNotNull(row[0]); // Classification
                assertNotNull(row[1]); // Count
                assertTrue((Long) row[1] > 0);
            }
        }
        
        @Test
        @DisplayName("Should find configurations created within date range")
        void shouldFindConfigurationsCreatedWithinDateRange() {
            LocalDateTime startDate = LocalDateTime.now().minusDays(4);
            LocalDateTime endDate = LocalDateTime.now();
            
            List<SqlLoaderConfigEntity> recentConfigs = repository
                    .findByCreatedDateBetweenAndEnabled(startDate, endDate, "Y");
            
            assertEquals(2, recentConfigs.size()); // testConfig1 (5 days ago) is outside range
            
            // Should be ordered by created date DESC
            assertTrue(recentConfigs.get(0).getCreatedDate()
                    .isAfter(recentConfigs.get(1).getCreatedDate()));
        }
        
        @Test
        @DisplayName("Should find configurations by created user")
        void shouldFindConfigurationsByCreatedUser() {
            List<SqlLoaderConfigEntity> adminConfigs = repository
                    .findByCreatedByAndEnabledOrderByCreatedDateDesc("test-admin", "Y");
            
            assertEquals(2, adminConfigs.size());
            
            List<SqlLoaderConfigEntity> userConfigs = repository
                    .findByCreatedByAndEnabledOrderByCreatedDateDesc("test-user", "Y");
            
            assertEquals(0, userConfigs.size()); // test-user config is disabled
        }
    }
    
    @Nested
    @DisplayName("Utility and Helper Query Tests")
    class UtilityHelperQueryTests {
        
        @Test
        @DisplayName("Should check configuration existence")
        void shouldCheckConfigurationExistence() {
            boolean exists = repository.existsBySourceSystemAndJobNameAndEnabled("HR", "p327", "Y");
            assertTrue(exists);
            
            boolean notExists = repository.existsBySourceSystemAndJobNameAndEnabled("HR", "nonexistent", "Y");
            assertFalse(notExists);
            
            boolean disabledExists = repository.existsBySourceSystemAndJobNameAndEnabled("HR", "p329", "Y");
            assertFalse(disabledExists); // This config is disabled
        }
        
        @Test
        @DisplayName("Should find configuration by config ID and enabled status")
        void shouldFindConfigurationByConfigIdAndEnabledStatus() {
            Optional<SqlLoaderConfigEntity> found = repository
                    .findByConfigIdAndEnabled("TEST-HR-001", "Y");
            
            assertTrue(found.isPresent());
            assertEquals("p327", found.get().getJobName());
            
            Optional<SqlLoaderConfigEntity> notFound = repository
                    .findByConfigIdAndEnabled("TEST-HR-002", "Y"); // This one is disabled
            
            assertFalse(notFound.isPresent());
        }
        
        @Test
        @DisplayName("Should find all enabled configurations")
        void shouldFindAllEnabledConfigurations() {
            List<SqlLoaderConfigEntity> enabledConfigs = repository
                    .findByEnabledOrderByJobName("Y");
            
            assertEquals(2, enabledConfigs.size());
            
            // Should be ordered by job name
            assertEquals("accounts", enabledConfigs.get(0).getJobName());
            assertEquals("p327", enabledConfigs.get(1).getJobName());
        }
        
        @Test
        @DisplayName("Should count enabled configurations")
        void shouldCountEnabledConfigurations() {
            long enabledCount = repository.countByEnabled("Y");
            assertEquals(2L, enabledCount);
            
            long disabledCount = repository.countByEnabled("N");
            assertEquals(1L, disabledCount);
        }
        
        @Test
        @DisplayName("Should find configurations with notification emails")
        void shouldFindConfigurationsWithNotificationEmails() {
            // First add notification emails to a config
            testConfig1.setNotificationEmails("admin@test.com,ops@test.com");
            entityManager.merge(testConfig1);
            entityManager.flush();
            
            List<SqlLoaderConfigEntity> configsWithEmails = repository
                    .findConfigurationsWithNotificationEmailsAndEnabled("Y");
            
            assertEquals(1, configsWithEmails.size());
            assertEquals("TEST-HR-001", configsWithEmails.get(0).getConfigId());
        }
    }
    
    @Nested
    @DisplayName("Advanced Feature Query Tests")
    class AdvancedFeatureQueryTests {
        
        @Test
        @DisplayName("Should find configurations with custom options")
        void shouldFindConfigurationsWithCustomOptions() {
            // Add custom options to a config
            testConfig2.setCustomOptions("STREAMSIZE=1000000 MULTITHREADING=TRUE");
            entityManager.merge(testConfig2);
            entityManager.flush();
            
            List<SqlLoaderConfigEntity> customConfigs = repository
                    .findConfigurationsWithCustomOptionsAndEnabled("Y");
            
            assertEquals(1, customConfigs.size());
            assertEquals("TEST-DDA-001", customConfigs.get(0).getConfigId());
        }
        
        @Test
        @DisplayName("Should find configurations with execution SQL")
        void shouldFindConfigurationsWithExecutionSql() {
            // Add pre-execution SQL to a config
            testConfig1.setPreExecutionSql("ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY-MM-DD'");
            entityManager.merge(testConfig1);
            entityManager.flush();
            
            List<SqlLoaderConfigEntity> sqlConfigs = repository
                    .findConfigurationsWithExecutionSqlAndEnabled("Y");
            
            assertEquals(1, sqlConfigs.size());
            assertEquals("TEST-HR-001", sqlConfigs.get(0).getConfigId());
        }
        
        @Test
        @DisplayName("Should find configurations by character set")
        void shouldFindConfigurationsByCharacterSet() {
            List<SqlLoaderConfigEntity> utf8Configs = repository
                    .findByCharacterSetAndEnabledOrderByJobName("UTF8", "Y");
            
            assertEquals(2, utf8Configs.size()); // All configs use UTF8 by default
        }
        
        @Test
        @DisplayName("Should find configurations by encryption algorithm")
        void shouldFindConfigurationsByEncryptionAlgorithm() {
            List<SqlLoaderConfigEntity> aes256Configs = repository
                    .findByEncryptionAlgorithmAndEnabledOrderByJobName("AES256", "Y");
            
            assertEquals(2, aes256Configs.size()); // All configs use AES256 by default
        }
        
        @Test
        @DisplayName("Should find configurations by field delimiter")
        void shouldFindConfigurationsByFieldDelimiter() {
            List<SqlLoaderConfigEntity> pipeConfigs = repository
                    .findByFieldDelimiterAndEnabledOrderByJobName("|", "Y");
            
            assertEquals(1, pipeConfigs.size());
            assertEquals("TEST-HR-001", pipeConfigs.get(0).getConfigId());
            
            List<SqlLoaderConfigEntity> commaConfigs = repository
                    .findByFieldDelimiterAndEnabledOrderByJobName(",", "Y");
            
            assertEquals(1, commaConfigs.size());
            assertEquals("TEST-DDA-001", commaConfigs.get(0).getConfigId());
        }
        
        @Test
        @DisplayName("Should find configurations by version")
        void shouldFindConfigurationsByVersion() {
            List<SqlLoaderConfigEntity> v1Configs = repository
                    .findByVersionAndEnabledOrderByModifiedDateDesc(1, "Y");
            
            assertEquals(2, v1Configs.size()); // Both enabled configs are version 1
        }
    }
}