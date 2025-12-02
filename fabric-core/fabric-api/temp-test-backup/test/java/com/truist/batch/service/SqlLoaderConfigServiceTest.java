package com.fabric.batch.service;

import com.fabric.batch.dto.*;
import com.fabric.batch.service.impl.SqlLoaderConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for SqlLoaderConfigService implementation.
 * Tests all core service methods, validation, security, and compliance features.
 */
@ExtendWith(MockitoExtension.class)
class SqlLoaderConfigServiceTest {

    @Mock
    private com.truist.batch.service.SqlLoaderConfigurationManagementService configurationManagementService;

    @Mock
    private AuditService auditService;

    @Mock
    private EnhancedControlFileGenerator controlFileGenerator;

    @InjectMocks
    private SqlLoaderConfigServiceImpl sqlLoaderConfigService;

    private SqlLoaderConfigRequest validRequest;
    private com.truist.batch.entity.SqlLoaderConfigEntity mockEntity;
    private SqlLoaderConfigResponse expectedResponse;

    @BeforeEach
    void setUp() {
        // Set up valid test request
        validRequest = SqlLoaderConfigRequest.builder()
                .jobName("TEST_JOB")
                .sourceSystem("TEST_SRC")
                .targetTable("TEST_TABLE")
                .loadMethod("INSERT")
                .directPath(true)
                .parallelDegree(2)
                .bindSize(256000L)
                .readSize(1048576L)
                .maxErrors(1000)
                .skipRows(1)
                .rowsPerCommit(64)
                .fieldDelimiter("|")
                .recordDelimiter("\n")
                .stringDelimiter("\"")
                .characterSet("UTF8")
                .dataClassification("INTERNAL")
                .encryptionRequired(false)
                .auditTrailRequired(true)
                .createdBy("TEST_USER")
                .reason("Unit test")
                .fieldConfigurations(Arrays.asList(
                    FieldConfigRequest.builder()
                        .fieldName("FIELD1")
                        .columnName("COL1")
                        .sourceField("SOURCE_FIELD1")
                        .fieldOrder(1)
                        .dataType("VARCHAR")
                        .maxLength(50)
                        .nullable(true)
                        .encrypted(false)
                        .containsPii(false)
                        .build()
                ))
                .build();

        // Set up mock entity
        mockEntity = com.truist.batch.entity.SqlLoaderConfigEntity.builder()
                .configId("TEST-CONFIG-123")
                .jobName("TEST_JOB")
                .sourceSystem("TEST_SRC")
                .targetTable("TEST_TABLE")
                .loadMethod(com.truist.batch.entity.SqlLoaderConfigEntity.LoadMethod.INSERT)
                .directPath("Y")
                .parallelDegree(2)
                .bindSize(256000L)
                .readSize(1048576L)
                .maxErrors(1000)
                .skipRows(1)
                .rowsPerCommit(64)
                .fieldDelimiter("|")
                .recordDelimiter("\n")
                .stringDelimiter("\"")
                .characterSet("UTF8")
                .dataClassification(com.truist.batch.entity.SqlLoaderConfigEntity.DataClassification.INTERNAL)
                .encryptionRequired("N")
                .auditTrailRequired("Y")
                .createdBy("TEST_USER")
                .version(1)
                .enabled(true)
                .build();

        // Set up expected response
        expectedResponse = SqlLoaderConfigResponse.builder()
                .configId("TEST-CONFIG-123")
                .jobName("TEST_JOB")
                .sourceSystem("TEST_SRC")
                .targetTable("TEST_TABLE")
                .loadMethod("INSERT")
                .directPath(true)
                .parallelDegree(2)
                .bindSize(256000L)
                .readSize(1048576L)
                .maxErrors(1000)
                .skipRows(1)
                .rowsPerCommit(64)
                .fieldDelimiter("|")
                .recordDelimiter("\n")
                .stringDelimiter("\"")
                .characterSet("UTF8")
                .dataClassification("INTERNAL")
                .createdBy("TEST_USER")
                .version(1)
                .enabled(true)
                .build();
    }

    // ==================== CONFIGURATION MANAGEMENT TESTS ====================

    @Test
    void testCreateConfiguration_Success() {
        // Given
        when(configurationManagementService.createConfiguration(any(), any(), anyString(), anyString()))
                .thenReturn(mockEntity);
        
        // When
        SqlLoaderConfigResponse result = sqlLoaderConfigService.createConfiguration(validRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getJobName()).isEqualTo("TEST_JOB");
        assertThat(result.getSourceSystem()).isEqualTo("TEST_SRC");
        assertThat(result.getTargetTable()).isEqualTo("TEST_TABLE");
        
        verify(configurationManagementService).createConfiguration(any(), any(), eq("TEST_USER"), eq("Unit test"));
        verify(auditService).logSecurityEvent(
                eq("SQL_LOADER_CONFIG_CREATED"), 
                anyString(), 
                eq("TEST_USER"), 
                any(Map.class)
        );
    }

    @Test
    void testCreateConfiguration_ValidationFailure() {
        // Given
        SqlLoaderConfigRequest invalidRequest = SqlLoaderConfigRequest.builder()
                .jobName("") // Invalid: empty job name
                .sourceSystem("TEST_SRC")
                .targetTable("TEST_TABLE")
                .createdBy("TEST_USER")
                .fieldConfigurations(Arrays.asList(
                    FieldConfigRequest.builder()
                        .fieldName("FIELD1")
                        .columnName("COL1")
                        .build()
                ))
                .build();
        
        // When & Then
        assertThatThrownBy(() -> sqlLoaderConfigService.createConfiguration(invalidRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create configuration");
    }

    @Test
    void testCreateConfiguration_DuplicateFieldNames() {
        // Given
        SqlLoaderConfigRequest requestWithDuplicates = validRequest.toBuilder()
                .fieldConfigurations(Arrays.asList(
                    FieldConfigRequest.builder()
                        .fieldName("FIELD1")
                        .columnName("COL1")
                        .dataType("VARCHAR")
                        .build(),
                    FieldConfigRequest.builder()
                        .fieldName("FIELD1") // Duplicate field name
                        .columnName("COL2")
                        .dataType("VARCHAR")
                        .build()
                ))
                .build();
        
        // When & Then
        assertThatThrownBy(() -> sqlLoaderConfigService.createConfiguration(requestWithDuplicates))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Duplicate field names are not allowed");
    }

    @Test
    void testGetConfiguration_Success() {
        // Given
        when(configurationManagementService.getConfigurationById("TEST-CONFIG-123"))
                .thenReturn(mockEntity);
        
        // When
        Optional<SqlLoaderConfigResponse> result = sqlLoaderConfigService.getConfiguration("TEST-CONFIG-123");
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getConfigId()).isEqualTo("TEST-CONFIG-123");
        assertThat(result.get().getJobName()).isEqualTo("TEST_JOB");
    }

    @Test
    void testGetConfiguration_NotFound() {
        // Given
        when(configurationManagementService.getConfigurationById("INVALID-CONFIG"))
                .thenThrow(new IllegalArgumentException("Configuration not found"));
        
        // When
        Optional<SqlLoaderConfigResponse> result = sqlLoaderConfigService.getConfiguration("INVALID-CONFIG");
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testUpdateConfiguration_Success() {
        // Given
        com.truist.batch.entity.SqlLoaderConfigEntity updatedEntity = mockEntity.toBuilder()
                .version(2)
                .maxErrors(2000)
                .build();
        
        when(configurationManagementService.updateConfiguration(anyString(), any(), any(), anyString(), anyString()))
                .thenReturn(updatedEntity);
        
        SqlLoaderConfigRequest updateRequest = validRequest.toBuilder()
                .maxErrors(2000)
                .reason("Updated for testing")
                .build();
        
        // When
        SqlLoaderConfigResponse result = sqlLoaderConfigService.updateConfiguration("TEST-CONFIG-123", updateRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVersion()).isEqualTo(2);
        
        verify(auditService).logSecurityEvent(
                eq("SQL_LOADER_CONFIG_UPDATED"), 
                anyString(), 
                eq("TEST_USER"), 
                any(Map.class)
        );
    }

    @Test
    void testDeleteConfiguration_Success() {
        // Given
        doNothing().when(configurationManagementService)
                .deleteConfiguration("TEST-CONFIG-123", "TEST_USER", "Test deletion");
        
        // When
        assertThatCode(() -> 
            sqlLoaderConfigService.deleteConfiguration("TEST-CONFIG-123", "TEST_USER", "Test deletion")
        ).doesNotThrowAnyException();
        
        // Then
        verify(configurationManagementService).deleteConfiguration("TEST-CONFIG-123", "TEST_USER", "Test deletion");
        verify(auditService).logSecurityEvent(
                eq("SQL_LOADER_CONFIG_DELETED"), 
                anyString(), 
                eq("TEST_USER"), 
                any(Map.class)
        );
    }

    @Test
    void testGetAllConfigurations_Success() {
        // Given
        Map<String, Object> filters = new HashMap<>();
        filters.put("sourceSystem", "TEST_SRC");
        PageRequest pageRequest = PageRequest.of(0, 10);
        
        // When
        Page<SqlLoaderConfigResponse> result = sqlLoaderConfigService.getAllConfigurations(pageRequest, filters);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(0); // Mock implementation returns empty
    }

    @Test
    void testGetConfigurationsBySourceSystem_Success() {
        // When
        List<SqlLoaderConfigResponse> result = sqlLoaderConfigService.getConfigurationsBySourceSystem("TEST_SRC");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty(); // Mock implementation returns empty
    }

    // ==================== CONTROL FILE GENERATION TESTS ====================

    @Test
    void testGenerateControlFile_Success() {
        // Given
        when(configurationManagementService.getConfigurationById("TEST-CONFIG-123"))
                .thenReturn(mockEntity);
        
        com.truist.batch.sqlloader.SqlLoaderConfig executionConfig = 
                com.truist.batch.sqlloader.SqlLoaderConfig.builder()
                .configId("TEST-CONFIG-123")
                .correlationId("CORR-123")
                .build();
        
        when(configurationManagementService.convertToExecutionConfig("TEST-CONFIG-123"))
                .thenReturn(executionConfig);
        
        when(controlFileGenerator.generateControlFile(any()))
                .thenReturn(java.nio.file.Paths.get("/tmp/test.ctl"));
        
        // When
        ControlFileResponse result = sqlLoaderConfigService.generateControlFile("TEST-CONFIG-123", "/data/test.dat");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConfigId()).isEqualTo("TEST-CONFIG-123");
        assertThat(result.getDataFilePath()).isEqualTo("/data/test.dat");
        assertThat(result.getControlFileName()).isEqualTo("test.ctl");
        
        verify(controlFileGenerator).generateControlFile(any());
    }

    @Test
    void testGenerateControlFileTemplate_Success() {
        // Given
        when(controlFileGenerator.generateTemplateControlFile("TEST_TABLE", Arrays.asList("FIELD1", "FIELD2", "FIELD3")))
                .thenReturn("-- Template control file for TEST_TABLE");
        
        // When
        String result = sqlLoaderConfigService.generateControlFileTemplate("TEST_TABLE", "DELIMITED");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("TEST_TABLE");
    }

    @Test
    void testValidateControlFile_ValidContent() {
        // Given
        String validControlFile = """
                LOAD DATA
                INFILE 'test.dat'
                INTO TABLE test_table
                FIELDS TERMINATED BY '|'
                (field1, field2, field3)
                """;
        
        // When
        ControlFileValidationResult result = sqlLoaderConfigService.validateControlFile(validControlFile);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void testValidateControlFile_InvalidContent() {
        // Given
        String invalidControlFile = "This is not a valid control file";
        
        // When
        ControlFileValidationResult result = sqlLoaderConfigService.validateControlFile(invalidControlFile);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getValid()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors()).contains("Missing LOAD DATA statement");
    }

    // ==================== VALIDATION AND COMPLIANCE TESTS ====================

    @Test
    void testValidateConfiguration_ValidRequest() {
        // When
        ConfigurationValidationResult result = sqlLoaderConfigService.validateConfiguration(validRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getValid()).isTrue();
        assertThat(result.getJobName()).isEqualTo("TEST_JOB");
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void testValidateConfiguration_PiiWithoutEncryption() {
        // Given
        SqlLoaderConfigRequest piiRequest = validRequest.toBuilder()
                .dataClassification("CONFIDENTIAL")
                .piiFields("ssn,social_security_number")
                .encryptionRequired(false)
                .build();
        
        // When
        ConfigurationValidationResult result = sqlLoaderConfigService.validateConfiguration(piiRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> 
            error.contains("PII data with high classification requires encryption"));
    }

    @Test
    void testValidateConfiguration_ParallelWithoutDirectPath() {
        // Given
        SqlLoaderConfigRequest invalidParallelRequest = validRequest.toBuilder()
                .parallelDegree(4)
                .directPath(false)
                .build();
        
        // When
        ConfigurationValidationResult result = sqlLoaderConfigService.validateConfiguration(invalidParallelRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> 
            error.contains("Parallel processing requires direct path loading"));
    }

    @Test
    void testGetComplianceReport_Success() {
        // Given
        com.truist.batch.dto.ComplianceReport mockReport = 
                com.truist.batch.dto.ComplianceReport.builder()
                .configId("TEST-CONFIG-123")
                .jobName("TEST_JOB")
                .sourceSystem("TEST_SRC")
                .compliant(true)
                .dataClassification("INTERNAL")
                .riskLevel("LOW")
                .build();
        
        when(configurationManagementService.getComplianceReport("TEST-CONFIG-123"))
                .thenReturn(mockReport);
        
        // When
        com.truist.batch.dto.SqlLoaderReports.ComplianceReport result = 
                sqlLoaderConfigService.getComplianceReport("TEST-CONFIG-123");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConfigId()).isEqualTo("TEST-CONFIG-123");
        assertThat(result.getCompliant()).isTrue();
        assertThat(result.getRiskLevel()).isEqualTo("LOW");
    }

    @Test
    void testGetPerformanceReport_Success() {
        // Given
        com.truist.batch.dto.PerformanceReport mockReport = 
                com.truist.batch.dto.PerformanceReport.builder()
                .configId("TEST-CONFIG-123")
                .jobName("TEST_JOB")
                .sourceSystem("TEST_SRC")
                .directPathEnabled(true)
                .parallelDegree(2)
                .bindSize(256000L)
                .readSize(1048576L)
                .optimizationRecommendations(Arrays.asList())
                .build();
        
        when(configurationManagementService.getPerformanceReport("TEST-CONFIG-123"))
                .thenReturn(mockReport);
        
        // When
        com.truist.batch.dto.SqlLoaderReports.PerformanceReport result = 
                sqlLoaderConfigService.getPerformanceReport("TEST-CONFIG-123");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConfigId()).isEqualTo("TEST-CONFIG-123");
        assertThat(result.getDirectPathEnabled()).isTrue();
        assertThat(result.getPerformanceProfile()).isEqualTo("OPTIMAL");
    }

    @Test
    void testGetSecurityAssessment_Success() {
        // Given
        when(configurationManagementService.getConfigurationById("TEST-CONFIG-123"))
                .thenReturn(mockEntity);
        
        // When
        com.truist.batch.dto.SqlLoaderReports.SecurityAssessment result = 
                sqlLoaderConfigService.getSecurityAssessment("TEST-CONFIG-123");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConfigId()).isEqualTo("TEST-CONFIG-123");
        assertThat(result.getRiskLevel()).isEqualTo("LOW");
        assertThat(result.getContainsPiiData()).isFalse();
        assertThat(result.getEncryptionConfigured()).isFalse();
        assertThat(result.getAuditingEnabled()).isTrue();
    }

    @Test
    void testGetSecurityAssessment_HighRisk() {
        // Given
        com.truist.batch.entity.SqlLoaderConfigEntity highRiskEntity = mockEntity.toBuilder()
                .dataClassification(com.truist.batch.entity.SqlLoaderConfigEntity.DataClassification.CONFIDENTIAL)
                .piiFields("ssn,social_security")
                .encryptionRequired("N")
                .build();
        
        when(configurationManagementService.getConfigurationById("TEST-CONFIG-123"))
                .thenReturn(highRiskEntity);
        
        // When
        com.truist.batch.dto.SqlLoaderReports.SecurityAssessment result = 
                sqlLoaderConfigService.getSecurityAssessment("TEST-CONFIG-123");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRiskLevel()).isEqualTo("HIGH");
        assertThat(result.getContainsPiiData()).isTrue();
        assertThat(result.getSecurityWarnings()).isNotEmpty();
        assertThat(result.getSecurityRecommendations()).contains("Enable encryption for PII fields");
    }

    // ==================== EDGE CASE AND ERROR HANDLING TESTS ====================

    @Test
    void testCreateConfiguration_ServiceException() {
        // Given
        when(configurationManagementService.createConfiguration(any(), any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));
        
        // When & Then
        assertThatThrownBy(() -> sqlLoaderConfigService.createConfiguration(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create configuration");
    }

    @Test
    void testGenerateControlFile_ConfigNotFound() {
        // Given
        when(configurationManagementService.getConfigurationById("INVALID-CONFIG"))
                .thenThrow(new IllegalArgumentException("Configuration not found"));
        
        // When & Then
        assertThatThrownBy(() -> 
            sqlLoaderConfigService.generateControlFile("INVALID-CONFIG", "/data/test.dat"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate control file");
    }

    @Test
    void testValidateConfiguration_NullRequest() {
        // When & Then
        assertThatThrownBy(() -> sqlLoaderConfigService.validateConfiguration(null))
                .isInstanceOf(RuntimeException.class);
    }

    // ==================== PERFORMANCE AND MEMORY TESTS ====================

    @Test
    void testRequestMemoryCalculation() {
        // Given
        SqlLoaderConfigRequest largeRequest = validRequest.toBuilder()
                .bindSize(10485760L) // 10MB
                .readSize(20971520L) // 20MB
                .parallelDegree(4)
                .build();
        
        // When
        double memoryUsage = largeRequest.getEstimatedMemoryUsageMB();
        
        // Then
        assertThat(memoryUsage).isGreaterThan(100.0); // Should be significant memory usage
        
        // Test memory usage validation
        ConfigurationValidationResult result = sqlLoaderConfigService.validateConfiguration(largeRequest);
        assertThat(result.getRecommendations()).anyMatch(rec -> 
            rec.contains("Consider reducing memory usage"));
    }

    @Test
    void testRiskLevelCalculation() {
        // Test LOW risk
        assertThat(validRequest.getRiskLevel()).isEqualTo("LOW");
        
        // Test HIGH risk - PII without encryption
        SqlLoaderConfigRequest highRiskRequest = validRequest.toBuilder()
                .dataClassification("CONFIDENTIAL")
                .piiFields("ssn,social")
                .encryptionRequired(false)
                .build();
        assertThat(highRiskRequest.getRiskLevel()).isEqualTo("HIGH");
        
        // Test MEDIUM risk - PII without audit
        SqlLoaderConfigRequest mediumRiskRequest = validRequest.toBuilder()
                .piiFields("name,address")
                .auditTrailRequired(false)
                .build();
        assertThat(mediumRiskRequest.getRiskLevel()).isEqualTo("MEDIUM");
    }

    @Test
    void testConfigurationSummary() {
        // When
        String summary = validRequest.getConfigurationSummary();
        
        // Then
        assertThat(summary).contains("TEST_JOB");
        assertThat(summary).contains("TEST_SRC");
        assertThat(summary).contains("TEST_TABLE");
        assertThat(summary).contains("INTERNAL");
        assertThat(summary).contains("false"); // PII and encryption flags
    }
}