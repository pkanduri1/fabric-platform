package com.truist.batch.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for SqlLoaderConfigEntity.
 * Tests entity behavior, validation, utility methods, and business rules.
 */
@DisplayName("SQL*Loader Configuration Entity Tests")
class SqlLoaderConfigEntityTest {
    
    private SqlLoaderConfigEntity entity;
    
    @BeforeEach
    void setUp() {
        entity = SqlLoaderConfigEntity.builder()
                .jobName("test-job")
                .sourceSystem("TEST")
                .targetTable("CM3INT.TEST_TABLE")
                .controlFileTemplate("LOAD DATA INFILE...")
                .loadMethod(SqlLoaderConfigEntity.LoadMethod.INSERT)
                .directPath("Y")
                .parallelDegree(2)
                .maxErrors(1000)
                .fieldDelimiter("|")
                .encryptionRequired("N")
                .dataClassification(SqlLoaderConfigEntity.DataClassification.INTERNAL)
                .createdBy("test-user")
                .build();
    }
    
    @Nested
    @DisplayName("Entity Construction and Defaults")
    class EntityConstructionTests {
        
        @Test
        @DisplayName("Should create entity with builder pattern")
        void shouldCreateEntityWithBuilder() {
            SqlLoaderConfigEntity config = SqlLoaderConfigEntity.builder()
                    .jobName("sample-job")
                    .sourceSystem("HR")
                    .targetTable("CM3INT.HR_DATA")
                    .createdBy("admin")
                    .build();
            
            assertNotNull(config);
            assertEquals("sample-job", config.getJobName());
            assertEquals("HR", config.getSourceSystem());
            assertEquals("CM3INT.HR_DATA", config.getTargetTable());
            assertEquals("admin", config.getCreatedBy());
        }
        
        @Test
        @DisplayName("Should set default values correctly")
        void shouldSetDefaultValues() {
            SqlLoaderConfigEntity config = new SqlLoaderConfigEntity();
            
            assertEquals(SqlLoaderConfigEntity.LoadMethod.INSERT, config.getLoadMethod());
            assertEquals("Y", config.getDirectPath());
            assertEquals(Integer.valueOf(1), config.getParallelDegree());
            assertEquals(Long.valueOf(256000), config.getBindSize());
            assertEquals(Long.valueOf(1048576), config.getReadSize());
            assertEquals(Integer.valueOf(1000), config.getMaxErrors());
            assertEquals(Integer.valueOf(1), config.getSkipRows());
            assertEquals(Integer.valueOf(64), config.getRowsPerCommit());
            assertEquals("|", config.getFieldDelimiter());
            assertEquals("\n", config.getRecordDelimiter());
            assertEquals("\"", config.getStringDelimiter());
            assertEquals("UTF8", config.getCharacterSet());
            assertEquals("Y", config.getTrimWhitespace());
            assertEquals("N", config.getEncryptionRequired());
            assertEquals(SqlLoaderConfigEntity.DataClassification.INTERNAL, config.getDataClassification());
            assertEquals(Integer.valueOf(2555), config.getRetentionDays());
            assertEquals(Integer.valueOf(1), config.getVersion());
            assertEquals("Y", config.getEnabled());
        }
        
        @Test
        @DisplayName("Should handle JPA lifecycle callbacks")
        void shouldHandleJpaLifecycleCallbacks() {
            SqlLoaderConfigEntity config = SqlLoaderConfigEntity.builder()
                    .jobName("test")
                    .sourceSystem("TEST")
                    .targetTable("TEST_TABLE")
                    .createdBy("admin")
                    .build();
            
            // Simulate @PrePersist
            config.onCreate();
            
            assertNotNull(config.getCreatedDate());
            assertNotNull(config.getConfigId());
            assertTrue(config.getConfigId().contains("TEST-TEST-SQLLOADER-"));
            assertEquals(Integer.valueOf(1), config.getVersion());
        }
        
        @Test
        @DisplayName("Should generate resumable name automatically")
        void shouldGenerateResumableName() {
            SqlLoaderConfigEntity config = SqlLoaderConfigEntity.builder()
                    .jobName("test")
                    .sourceSystem("HR")
                    .targetTable("TEST_TABLE")
                    .resumable("Y")
                    .createdBy("admin")
                    .build();
            
            config.onCreate();
            
            assertNotNull(config.getResumableName());
            assertTrue(config.getResumableName().startsWith("FABRIC_SQLLOADER_"));
            assertTrue(config.getResumableName().contains(config.getConfigId()));
        }
    }
    
    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {
        
        @Test
        @DisplayName("Should correctly check boolean flags")
        void shouldCheckBooleanFlags() {
            entity.setDirectPath("Y");
            entity.setTrimWhitespace("Y");
            entity.setOptionalEnclosures("Y");
            entity.setResumable("Y");
            entity.setSilentMode("N");
            entity.setContinueLoad("N");
            entity.setEncryptionRequired("N");
            entity.setAuditTrailRequired("Y");
            entity.setValidationEnabled("Y");
            entity.setEnabled("Y");
            
            assertTrue(entity.isDirectPathEnabled());
            assertTrue(entity.isTrimWhitespaceEnabled());
            assertTrue(entity.isOptionalEnclosuresEnabled());
            assertTrue(entity.isResumableEnabled());
            assertFalse(entity.isSilentModeEnabled());
            assertFalse(entity.isContinueLoadEnabled());
            assertFalse(entity.isEncryptionRequired());
            assertTrue(entity.isAuditTrailRequired());
            assertTrue(entity.isValidationEnabled());
            assertTrue(entity.isEnabled());
        }
        
        @Test
        @DisplayName("Should check PII data presence")
        void shouldCheckPiiDataPresence() {
            entity.setPiiFields("SSN,EMAIL,PHONE");
            assertTrue(entity.containsPiiData());
            
            entity.setPiiFields("");
            assertFalse(entity.containsPiiData());
            
            entity.setPiiFields(null);
            assertFalse(entity.containsPiiData());
        }
        
        @Test
        @DisplayName("Should check parallel processing")
        void shouldCheckParallelProcessing() {
            entity.setParallelDegree(1);
            assertFalse(entity.hasParallelProcessing());
            
            entity.setParallelDegree(2);
            assertTrue(entity.hasParallelProcessing());
            
            entity.setParallelDegree(null);
            assertFalse(entity.hasParallelProcessing());
        }
        
        @Test
        @DisplayName("Should check SQL execution presence")
        void shouldCheckSqlExecutionPresence() {
            entity.setPreExecutionSql("SELECT 1 FROM DUAL");
            entity.setPostExecutionSql("");
            
            assertTrue(entity.hasPreExecutionSql());
            assertFalse(entity.hasPostExecutionSql());
            
            entity.setPostExecutionSql("COMMIT");
            assertTrue(entity.hasPostExecutionSql());
        }
    }
    
    @Nested
    @DisplayName("Command Line Options Generation Tests")
    class CommandLineOptionsTests {
        
        @Test
        @DisplayName("Should generate basic command line options")
        void shouldGenerateBasicCommandLineOptions() {
            entity.setDirectPath("Y");
            entity.setMaxErrors(500);
            entity.setSkipRows(2);
            entity.setBindSize(128000L);
            
            String options = entity.getFormattedCommandLineOptions();
            
            assertNotNull(options);
            assertTrue(options.contains("DIRECT=TRUE"));
            assertTrue(options.contains("ERRORS=500"));
            assertTrue(options.contains("SKIP=2"));
            assertTrue(options.contains("BINDSIZE=128000"));
        }
        
        @Test
        @DisplayName("Should generate parallel processing options")
        void shouldGenerateParallelProcessingOptions() {
            entity.setParallelDegree(4);
            
            String options = entity.getFormattedCommandLineOptions();
            
            assertTrue(options.contains("PARALLEL=TRUE"));
        }
        
        @Test
        @DisplayName("Should generate resumable options")
        void shouldGenerateResumableOptions() {
            entity.setResumable("Y");
            entity.setResumableTimeout(3600);
            entity.setResumableName("TEST_RESUMABLE");
            
            String options = entity.getFormattedCommandLineOptions();
            
            assertTrue(options.contains("RESUMABLE=TRUE"));
            assertTrue(options.contains("RESUMABLE_TIMEOUT=3600"));
            assertTrue(options.contains("RESUMABLE_NAME='TEST_RESUMABLE'"));
        }
        
        @Test
        @DisplayName("Should include custom options")
        void shouldIncludeCustomOptions() {
            entity.setCustomOptions("STREAMSIZE=1000000 MULTITHREADING=TRUE");
            
            String options = entity.getFormattedCommandLineOptions();
            
            assertTrue(options.contains("STREAMSIZE=1000000"));
            assertTrue(options.contains("MULTITHREADING=TRUE"));
        }
        
        @Test
        @DisplayName("Should handle silent mode")
        void shouldHandleSilentMode() {
            entity.setSilentMode("Y");
            
            String options = entity.getFormattedCommandLineOptions();
            
            assertTrue(options.contains("SILENT=ALL"));
        }
    }
    
    @Nested
    @DisplayName("Load Method Clause Generation Tests")
    class LoadMethodClauseTests {
        
        @Test
        @DisplayName("Should generate INSERT clause")
        void shouldGenerateInsertClause() {
            entity.setLoadMethod(SqlLoaderConfigEntity.LoadMethod.INSERT);
            
            String clause = entity.getLoadMethodClause();
            
            assertEquals("INSERT INTO TABLE " + entity.getTargetTable(), clause);
        }
        
        @Test
        @DisplayName("Should generate APPEND clause")
        void shouldGenerateAppendClause() {
            entity.setLoadMethod(SqlLoaderConfigEntity.LoadMethod.APPEND);
            
            String clause = entity.getLoadMethodClause();
            
            assertEquals("APPEND INTO TABLE " + entity.getTargetTable(), clause);
        }
        
        @Test
        @DisplayName("Should generate REPLACE clause")
        void shouldGenerateReplaceClause() {
            entity.setLoadMethod(SqlLoaderConfigEntity.LoadMethod.REPLACE);
            
            String clause = entity.getLoadMethodClause();
            
            assertEquals("REPLACE INTO TABLE " + entity.getTargetTable(), clause);
        }
        
        @Test
        @DisplayName("Should generate TRUNCATE clause")
        void shouldGenerateTruncateClause() {
            entity.setLoadMethod(SqlLoaderConfigEntity.LoadMethod.TRUNCATE);
            
            String clause = entity.getLoadMethodClause();
            
            assertEquals("TRUNCATE INTO TABLE " + entity.getTargetTable(), clause);
        }
        
        @Test
        @DisplayName("Should default to INSERT when null")
        void shouldDefaultToInsertWhenNull() {
            entity.setLoadMethod(null);
            
            String clause = entity.getLoadMethodClause();
            
            assertEquals("INSERT INTO TABLE " + entity.getTargetTable(), clause);
        }
    }
    
    @Nested
    @DisplayName("Configuration Validation Tests")
    class ValidationTests {
        
        @Test
        @DisplayName("Should pass validation for valid configuration")
        void shouldPassValidationForValidConfiguration() {
            assertDoesNotThrow(() -> entity.validateConfiguration());
        }
        
        @Test
        @DisplayName("Should fail validation for empty target table")
        void shouldFailValidationForEmptyTargetTable() {
            entity.setTargetTable("");
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateConfiguration()
            );
            
            assertEquals("Target table must be specified", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should fail validation for negative max errors")
        void shouldFailValidationForNegativeMaxErrors() {
            entity.setMaxErrors(-1);
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateConfiguration()
            );
            
            assertEquals("Max errors cannot be negative", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should fail validation for invalid parallel degree")
        void shouldFailValidationForInvalidParallelDegree() {
            entity.setParallelDegree(0);
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateConfiguration()
            );
            
            assertEquals("Parallel degree must be at least 1", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should fail validation for invalid bind size")
        void shouldFailValidationForInvalidBindSize() {
            entity.setBindSize(0L);
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateConfiguration()
            );
            
            assertEquals("Bind size must be positive", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should fail validation for invalid read size")
        void shouldFailValidationForInvalidReadSize() {
            entity.setReadSize(-1L);
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateConfiguration()
            );
            
            assertEquals("Read size must be positive", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should fail validation for invalid retention days")
        void shouldFailValidationForInvalidRetentionDays() {
            entity.setRetentionDays(0);
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateConfiguration()
            );
            
            assertEquals("Retention days must be positive", exception.getMessage());
        }
    }
    
    @Nested
    @DisplayName("Compliance and Security Validation Tests")
    class ComplianceValidationTests {
        
        @Test
        @DisplayName("Should fail validation for PII data without encryption in high classification")
        void shouldFailValidationForPiiWithoutEncryption() {
            entity.setPiiFields("SSN,EMAIL");
            entity.setEncryptionRequired("N");
            entity.setDataClassification(SqlLoaderConfigEntity.DataClassification.CONFIDENTIAL);
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateConfiguration()
            );
            
            assertEquals("PII data with high classification requires encryption", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should pass validation for PII data with encryption")
        void shouldPassValidationForPiiWithEncryption() {
            entity.setPiiFields("SSN,EMAIL");
            entity.setEncryptionRequired("Y");
            entity.setDataClassification(SqlLoaderConfigEntity.DataClassification.CONFIDENTIAL);
            
            assertDoesNotThrow(() -> entity.validateConfiguration());
        }
        
        @Test
        @DisplayName("Should fail validation for PCI compliance without audit trail")
        void shouldFailValidationForPciWithoutAuditTrail() {
            entity.setRegulatoryCompliance("PCI-DSS");
            entity.setAuditTrailRequired("N");
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateConfiguration()
            );
            
            assertEquals("PCI compliance requires audit trail", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should pass validation for PCI compliance with audit trail")
        void shouldPassValidationForPciWithAuditTrail() {
            entity.setRegulatoryCompliance("PCI-DSS");
            entity.setAuditTrailRequired("Y");
            
            assertDoesNotThrow(() -> entity.validateConfiguration());
        }
    }
    
    @Nested
    @DisplayName("Enum Handling Tests")
    class EnumHandlingTests {
        
        @Test
        @DisplayName("Should handle LoadMethod enum correctly")
        void shouldHandleLoadMethodEnum() {
            for (SqlLoaderConfigEntity.LoadMethod method : SqlLoaderConfigEntity.LoadMethod.values()) {
                entity.setLoadMethod(method);
                assertEquals(method, entity.getLoadMethod());
                
                String clause = entity.getLoadMethodClause();
                assertNotNull(clause);
                assertTrue(clause.contains("INTO TABLE " + entity.getTargetTable()));
            }
        }
        
        @Test
        @DisplayName("Should handle DataClassification enum correctly")
        void shouldHandleDataClassificationEnum() {
            for (SqlLoaderConfigEntity.DataClassification classification : SqlLoaderConfigEntity.DataClassification.values()) {
                entity.setDataClassification(classification);
                assertEquals(classification, entity.getDataClassification());
            }
        }
    }
    
    @Nested
    @DisplayName("PreUpdate Lifecycle Tests")
    class PreUpdateLifecycleTests {
        
        @Test
        @DisplayName("Should update modification fields on update")
        void shouldUpdateModificationFieldsOnUpdate() {
            entity.setVersion(2);
            LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
            
            entity.onUpdate();
            
            assertNotNull(entity.getModifiedDate());
            assertTrue(entity.getModifiedDate().isAfter(beforeUpdate));
            assertEquals(Integer.valueOf(3), entity.getVersion());
        }
        
        @Test
        @DisplayName("Should handle null version on update")
        void shouldHandleNullVersionOnUpdate() {
            entity.setVersion(null);
            
            entity.onUpdate();
            
            assertNotNull(entity.getModifiedDate());
            assertNull(entity.getVersion()); // Should remain null
        }
    }
    
    @Test
    @DisplayName("Should handle toString without circular references")
    void shouldHandleToStringWithoutCircularReferences() {
        entity.setFieldConfigs(Arrays.asList(/* mock field configs */));
        entity.setExecutions(Arrays.asList(/* mock executions */));
        entity.setSecurityAudits(Arrays.asList(/* mock security audits */));
        
        String toString = entity.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("SqlLoaderConfigEntity"));
        assertTrue(toString.contains("jobName=test-job"));
        // Should exclude lazy-loaded collections
        assertFalse(toString.contains("fieldConfigs"));
        assertFalse(toString.contains("executions"));
        assertFalse(toString.contains("securityAudits"));
    }
    
    @Test
    @DisplayName("Should handle equals and hashCode correctly")
    void shouldHandleEqualsAndHashCodeCorrectly() {
        SqlLoaderConfigEntity entity1 = SqlLoaderConfigEntity.builder()
                .configId("TEST-001")
                .jobName("test")
                .sourceSystem("TEST")
                .targetTable("TEST_TABLE")
                .createdBy("admin")
                .build();
        
        SqlLoaderConfigEntity entity2 = SqlLoaderConfigEntity.builder()
                .configId("TEST-001")
                .jobName("test")
                .sourceSystem("TEST")
                .targetTable("TEST_TABLE")
                .createdBy("admin")
                .build();
        
        SqlLoaderConfigEntity entity3 = SqlLoaderConfigEntity.builder()
                .configId("TEST-002")
                .jobName("test2")
                .sourceSystem("TEST")
                .targetTable("TEST_TABLE")
                .createdBy("admin")
                .build();
        
        assertEquals(entity1, entity2);
        assertNotEquals(entity1, entity3);
        assertEquals(entity1.hashCode(), entity2.hashCode());
        assertNotEquals(entity1.hashCode(), entity3.hashCode());
    }
}