package com.truist.batch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truist.batch.dto.*;
import com.truist.batch.service.SqlLoaderConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for SqlLoaderController REST API endpoints.
 * Tests all 15+ endpoints with proper security, validation, and error handling.
 */
@WebMvcTest(SqlLoaderController.class)
class SqlLoaderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SqlLoaderConfigService sqlLoaderConfigService;

    private SqlLoaderConfigRequest validRequest;
    private SqlLoaderConfigResponse mockResponse;
    private ControlFileResponse mockControlFileResponse;

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
                .reason("Integration test")
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

        // Set up mock response
        mockResponse = SqlLoaderConfigResponse.builder()
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
                .createdDate(LocalDateTime.now())
                .version(1)
                .enabled(true)
                .build();

        // Set up mock control file response
        mockControlFileResponse = ControlFileResponse.builder()
                .controlFileContent("LOAD DATA INFILE 'test.dat' INTO TABLE TEST_TABLE...")
                .controlFileName("test_config.ctl")
                .controlFilePath("/tmp/test_config.ctl")
                .configId("TEST-CONFIG-123")
                .jobName("TEST_JOB")
                .sourceSystem("TEST_SRC")
                .targetTable("TEST_TABLE")
                .dataFilePath("/data/test.dat")
                .generatedAt(LocalDateTime.now())
                .correlationId("CORR-123")
                .totalFields(1)
                .syntaxValid(true)
                .characterSet("UTF8")
                .fileFormat("DELIMITED")
                .loadMethod("INSERT")
                .readyForExecution(true)
                .build();
    }

    // ==================== CONFIGURATION MANAGEMENT ENDPOINT TESTS ====================

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testCreateConfiguration_Success() throws Exception {
        // Given
        when(sqlLoaderConfigService.createConfiguration(any(SqlLoaderConfigRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/sql-loader/configurations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("SQL*Loader configuration created successfully"))
                .andExpect(jsonPath("$.configuration.configId").value("TEST-CONFIG-123"))
                .andExpect(jsonPath("$.configuration.jobName").value("TEST_JOB"))
                .andExpect(jsonPath("$.configuration.sourceSystem").value("TEST_SRC"))
                .andExpect(jsonPath("$.configId").value("TEST-CONFIG-123"))
                .andExpected(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testCreateConfiguration_AccessDenied() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/sql-loader/configurations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testCreateConfiguration_ValidationError() throws Exception {
        // Given
        SqlLoaderConfigRequest invalidRequest = validRequest.toBuilder()
                .jobName("") // Invalid: empty job name
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/sql-loader/configurations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testCreateConfiguration_ServiceException() throws Exception {
        // Given
        when(sqlLoaderConfigService.createConfiguration(any(SqlLoaderConfigRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(post("/api/v1/sql-loader/configurations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to create configuration"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testGetConfiguration_Success() throws Exception {
        // Given
        when(sqlLoaderConfigService.getConfiguration("TEST-CONFIG-123"))
                .thenReturn(Optional.of(mockResponse));

        // When & Then
        mockMvc.perform(get("/api/v1/sql-loader/configurations/TEST-CONFIG-123"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.configuration.configId").value("TEST-CONFIG-123"))
                .andExpect(jsonPath("$.configuration.jobName").value("TEST_JOB"))
                .andExpect(jsonPath("$.retrievedAt").exists());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testGetConfiguration_NotFound() throws Exception {
        // Given
        when(sqlLoaderConfigService.getConfiguration("INVALID-CONFIG"))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/sql-loader/configurations/INVALID-CONFIG"))
                .andDo(print())
                .andExpected(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Configuration not found"));
    }

    @Test
    @WithMockUser(roles = {"CONFIG_MANAGER"})
    void testUpdateConfiguration_Success() throws Exception {
        // Given
        SqlLoaderConfigResponse updatedResponse = mockResponse.toBuilder()
                .version(2)
                .maxErrors(2000)
                .build();

        when(sqlLoaderConfigService.updateConfiguration(eq("TEST-CONFIG-123"), any(SqlLoaderConfigRequest.class)))
                .thenReturn(updatedResponse);

        SqlLoaderConfigRequest updateRequest = validRequest.toBuilder()
                .maxErrors(2000)
                .build();

        // When & Then
        mockMvc.perform(put("/api/v1/sql-loader/configurations/TEST-CONFIG-123")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.message").value("SQL*Loader configuration updated successfully"))
                .andExpect(jsonPath("$.configuration.version").value(2))
                .andExpected(jsonPath("$.newVersion").value(2));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testDeleteConfiguration_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/sql-loader/configurations/TEST-CONFIG-123")
                .with(csrf())
                .param("deletedBy", "TEST_USER")
                .param("reason", "Test deletion"))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.message").value("SQL*Loader configuration deleted successfully"))
                .andExpected(jsonPath("$.configId").value("TEST-CONFIG-123"))
                .andExpected(jsonPath("$.deletedBy").value("TEST_USER"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testGetAllConfigurations_Success() throws Exception {
        // Given
        List<SqlLoaderConfigResponse> configurations = Arrays.asList(mockResponse);
        when(sqlLoaderConfigService.getAllConfigurations(any(), any()))
                .thenReturn(new PageImpl<>(configurations, PageRequest.of(0, 10), 1));

        // When & Then
        mockMvc.perform(get("/api/v1/sql-loader/configurations")
                .param("sourceSystem", "TEST_SRC")
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.configurations").isArray())
                .andExpected(jsonPath("$.totalElements").value(1))
                .andExpected(jsonPath("$.totalPages").value(1));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testGetConfigurationsBySourceSystem_Success() throws Exception {
        // Given
        when(sqlLoaderConfigService.getConfigurationsBySourceSystem("TEST_SRC"))
                .thenReturn(Arrays.asList(mockResponse));

        // When & Then
        mockMvc.perform(get("/api/v1/sql-loader/configurations/source-system/TEST_SRC"))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.sourceSystem").value("TEST_SRC"))
                .andExpected(jsonPath("$.configurations").isArray())
                .andExpected(jsonPath("$.count").value(1));
    }

    // ==================== CONTROL FILE GENERATION ENDPOINT TESTS ====================

    @Test
    @WithMockUser(roles = {"USER"})
    void testGenerateControlFile_Success() throws Exception {
        // Given
        when(sqlLoaderConfigService.generateControlFile("TEST-CONFIG-123", "/data/test.dat"))
                .thenReturn(mockControlFileResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/sql-loader/configurations/TEST-CONFIG-123/control-file")
                .with(csrf())
                .param("dataFilePath", "/data/test.dat"))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Control file generated successfully"))
                .andExpected(jsonPath("$.controlFile.configId").value("TEST-CONFIG-123"))
                .andExpected(jsonPath("$.controlFile.syntaxValid").value(true))
                .andExpected(jsonPath("$.dataFilePath").value("/data/test.dat"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testDownloadControlFile_Success() throws Exception {
        // Given
        when(sqlLoaderConfigService.generateControlFile("TEST-CONFIG-123", "/data/test.dat"))
                .thenReturn(mockControlFileResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/sql-loader/configurations/TEST-CONFIG-123/control-file/download")
                .with(csrf())
                .param("dataFilePath", "/data/test.dat"))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(header().string("Content-Disposition", containsString("attachment")))
                .andExpected(content().string(containsString("LOAD DATA")));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testDownloadControlFile_ValidationErrors() throws Exception {
        // Given
        ControlFileResponse invalidControlFile = mockControlFileResponse.toBuilder()
                .readyForExecution(false)
                .validationErrors(Arrays.asList("Syntax error in control file"))
                .build();

        when(sqlLoaderConfigService.generateControlFile("TEST-CONFIG-123", "/data/test.dat"))
                .thenReturn(invalidControlFile);

        // When & Then
        mockMvc.perform(post("/api/v1/sql-loader/configurations/TEST-CONFIG-123/control-file/download")
                .with(csrf())
                .param("dataFilePath", "/data/test.dat"))
                .andDo(print())
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.success").value(false))
                .andExpected(jsonPath("$.message").value("Control file has validation issues"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testGenerateControlFileTemplate_Success() throws Exception {
        // Given
        when(sqlLoaderConfigService.generateControlFileTemplate("TEST_TABLE", "DELIMITED"))
                .thenReturn("-- Template control file for TEST_TABLE");

        // When & Then
        mockMvc.perform(get("/api/v1/sql-loader/control-file/template")
                .param("targetTable", "TEST_TABLE")
                .param("fileType", "DELIMITED"))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.template").value("-- Template control file for TEST_TABLE"))
                .andExpected(jsonPath("$.targetTable").value("TEST_TABLE"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testValidateControlFile_Success() throws Exception {
        // Given
        String controlFileContent = "LOAD DATA INFILE 'test.dat' INTO TABLE test_table";
        ControlFileValidationResult validationResult = ControlFileValidationResult.builder()
                .valid(true)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .recommendations(new ArrayList<>())
                .validationSummary("Control file is valid")
                .validatedAt(LocalDateTime.now())
                .build();

        when(sqlLoaderConfigService.validateControlFile(controlFileContent))
                .thenReturn(validationResult);

        // When & Then
        mockMvc.perform(post("/api/v1/sql-loader/control-file/validate")
                .with(csrf())
                .contentType(MediaType.TEXT_PLAIN)
                .content(controlFileContent))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.validation.valid").value(true))
                .andExpected(jsonPath("$.valid").value(true))
                .andExpected(jsonPath("$.errorCount").value(0));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testGetControlFileHistory_Success() throws Exception {
        // Given
        List<ControlFileHistory> history = Arrays.asList(
                ControlFileHistory.builder()
                        .configId("TEST-CONFIG-123")
                        .generatedAt(LocalDateTime.now())
                        .dataFilePath("/data/test1.dat")
                        .correlationId("CORR-1")
                        .build()
        );

        when(sqlLoaderConfigService.getControlFileHistory("TEST-CONFIG-123"))
                .thenReturn(history);

        // When & Then
        mockMvc.perform(get("/api/v1/sql-loader/configurations/TEST-CONFIG-123/control-file/history"))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.configId").value("TEST-CONFIG-123"))
                .andExpected(jsonPath("$.history").isArray())
                .andExpected(jsonPath("$.count").value(1));
    }

    // ==================== VALIDATION AND COMPLIANCE ENDPOINT TESTS ====================

    @Test
    @WithMockUser(roles = {"USER"})
    void testValidateConfiguration_Success() throws Exception {
        // Given
        ConfigurationValidationResult validationResult = ConfigurationValidationResult.builder()
                .valid(true)
                .jobName("TEST_JOB")
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .recommendations(Arrays.asList("Consider enabling monitoring"))
                .validatedAt(LocalDateTime.now())
                .validationSummary("Configuration is valid")
                .build();

        when(sqlLoaderConfigService.validateConfiguration(any(SqlLoaderConfigRequest.class)))
                .thenReturn(validationResult);

        // When & Then
        mockMvc.perform(post("/api/v1/sql-loader/configurations/validate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.validation.valid").value(true))
                .andExpected(jsonPath("$.valid").value(true))
                .andExpected(jsonPath("$.recommendationCount").value(1));
    }

    @Test
    @WithMockUser(roles = {"COMPLIANCE"})
    void testGetComplianceReport_Success() throws Exception {
        // Given
        com.truist.batch.dto.SqlLoaderReports.ComplianceReport complianceReport = 
                com.truist.batch.dto.SqlLoaderReports.ComplianceReport.builder()
                .configId("TEST-CONFIG-123")
                .jobName("TEST_JOB")
                .sourceSystem("TEST_SRC")
                .compliant(true)
                .dataClassification("INTERNAL")
                .regulatoryCompliance("SOX")
                .complianceIssues(new ArrayList<>())
                .complianceRecommendations(new ArrayList<>())
                .riskLevel("LOW")
                .assessedAt(LocalDateTime.now())
                .build();

        when(sqlLoaderConfigService.getComplianceReport("TEST-CONFIG-123"))
                .thenReturn(complianceReport);

        // When & Then
        mockMvc.perform(get("/api/v1/sql-loader/configurations/TEST-CONFIG-123/compliance"))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.report.configId").value("TEST-CONFIG-123"))
                .andExpected(jsonPath("$.compliant").value(true))
                .andExpected(jsonPath("$.riskLevel").value("LOW"));
    }

    @Test
    @WithMockUser(roles = {"PERFORMANCE"})
    void testGetPerformanceReport_Success() throws Exception {
        // Given
        com.truist.batch.dto.SqlLoaderReports.PerformanceReport performanceReport = 
                com.truist.batch.dto.SqlLoaderReports.PerformanceReport.builder()
                .configId("TEST-CONFIG-123")
                .jobName("TEST_JOB")
                .sourceSystem("TEST_SRC")
                .performanceProfile("OPTIMAL")
                .directPathEnabled(true)
                .parallelDegree(2)
                .bindSize(256000L)
                .readSize(1048576L)
                .performanceRecommendations(new ArrayList<>())
                .analyzedAt(LocalDateTime.now())
                .build();

        when(sqlLoaderConfigService.getPerformanceReport("TEST-CONFIG-123"))
                .thenReturn(performanceReport);

        // When & Then
        mockMvc.perform(get("/api/v1/sql-loader/configurations/TEST-CONFIG-123/performance"))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.report.configId").value("TEST-CONFIG-123"))
                .andExpected(jsonPath("$.performanceProfile").value("OPTIMAL"))
                .andExpected(jsonPath("$.needsOptimization").value(false));
    }

    @Test
    @WithMockUser(roles = {"SECURITY"})
    void testGetSecurityAssessment_Success() throws Exception {
        // Given
        com.truist.batch.dto.SqlLoaderReports.SecurityAssessment securityAssessment = 
                com.truist.batch.dto.SqlLoaderReports.SecurityAssessment.builder()
                .configId("TEST-CONFIG-123")
                .jobName("TEST_JOB")
                .sourceSystem("TEST_SRC")
                .riskLevel("LOW")
                .containsPiiData(false)
                .encryptionConfigured(false)
                .auditingEnabled(true)
                .complianceRequired(false)
                .securityWarnings(new ArrayList<>())
                .securityRecommendations(new ArrayList<>())
                .assessedAt(LocalDateTime.now())
                .build();

        when(sqlLoaderConfigService.getSecurityAssessment("TEST-CONFIG-123"))
                .thenReturn(securityAssessment);

        // When & Then
        mockMvc.perform(get("/api/v1/sql-loader/configurations/TEST-CONFIG-123/security"))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.assessment.configId").value("TEST-CONFIG-123"))
                .andExpected(jsonPath("$.riskLevel").value("LOW"))
                .andExpected(jsonPath("$.containsPii").value(false))
                .andExpected(jsonPath("$.encryptionConfigured").value(false));
    }

    // ==================== REPORTING AND ANALYTICS ENDPOINT TESTS ====================

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetUsageReport_Success() throws Exception {
        // Given
        com.truist.batch.dto.SqlLoaderReports.ConfigurationUsageReport usageReport = 
                com.truist.batch.dto.SqlLoaderReports.ConfigurationUsageReport.builder()
                .totalConfigurations(10)
                .activeConfigurations(8)
                .inactiveConfigurations(2)
                .configurationsWithPii(3)
                .encryptedConfigurations(2)
                .build();

        when(sqlLoaderConfigService.getUsageReport())
                .thenReturn(usageReport);

        // When & Then
        mockMvc.perform(get("/api/v1/sql-loader/reports/usage"))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.report.totalConfigurations").value(10));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testGetConfigurationsRequiringAttention_Success() throws Exception {
        // Given
        List<com.truist.batch.dto.SqlLoaderReports.ConfigurationAlert> alerts = Arrays.asList(
                com.truist.batch.dto.SqlLoaderReports.ConfigurationAlert.builder()
                        .configId("TEST-CONFIG-123")
                        .jobName("TEST_JOB")
                        .alertType("COMPLIANCE_WARNING")
                        .severity("MEDIUM")
                        .message("PII fields without encryption")
                        .recommendedAction("Enable encryption for sensitive fields")
                        .build()
        );

        when(sqlLoaderConfigService.getConfigurationsRequiringAttention())
                .thenReturn(alerts);

        // When & Then
        mockMvc.perform(get("/api/v1/sql-loader/configurations/attention"))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.alerts").isArray())
                .andExpected(jsonPath("$.alertCount").value(1));
    }

    @Test
    @WithMockUser(roles = {"COMPLIANCE"})
    void testGetDataClassificationReport_Success() throws Exception {
        // Given
        com.truist.batch.dto.SqlLoaderReports.DataClassificationReport classificationReport = 
                com.truist.batch.dto.SqlLoaderReports.DataClassificationReport.builder()
                .totalConfigurations(10)
                .totalPiiConfigurations(3)
                .encryptedPiiConfigurations(2)
                .piiComplianceRate(66.7)
                .build();

        when(sqlLoaderConfigService.getDataClassificationReport())
                .thenReturn(classificationReport);

        // When & Then
        mockMvc.perform(get("/api/v1/sql-loader/reports/data-classification"))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.report.totalPiiConfigurations").value(3))
                .andExpected(jsonPath("$.piiComplianceRate").value(66.7));
    }

    // ==================== ERROR HANDLING AND EDGE CASES ====================

    @Test
    @WithMockUser(roles = {"USER"})
    void testGenerateControlFile_ConfigNotFound() throws Exception {
        // Given
        when(sqlLoaderConfigService.generateControlFile("INVALID-CONFIG", "/data/test.dat"))
                .thenThrow(new IllegalArgumentException("Configuration not found"));

        // When & Then
        mockMvc.perform(post("/api/v1/sql-loader/configurations/INVALID-CONFIG/control-file")
                .with(csrf())
                .param("dataFilePath", "/data/test.dat"))
                .andDo(print())
                .andExpected(status().isNotFound())
                .andExpected(jsonPath("$.success").value(false))
                .andExpected(jsonPath("$.message").value("Configuration not found"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testGetConfiguration_ServiceException() throws Exception {
        // Given
        when(sqlLoaderConfigService.getConfiguration("TEST-CONFIG-123"))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(get("/api/v1/sql-loader/configurations/TEST-CONFIG-123"))
                .andDo(print())
                .andExpected(status().isInternalServerError())
                .andExpected(jsonPath("$.success").value(false))
                .andExpected(jsonPath("$.message").value("Failed to retrieve configuration"));
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/sql-loader/configurations/TEST-CONFIG-123"))
                .andDo(print())
                .andExpected(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testCreateConfiguration_MissingRequiredParameters() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/sql-loader/configurations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")) // Empty JSON
                .andDo(print())
                .andExpected(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testInvalidConfigId_PathVariable() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/sql-loader/configurations/ ")) // Empty/whitespace config ID
                .andDo(print())
                .andExpected(status().isBadRequest());
    }

    // ==================== PERFORMANCE AND LOAD TESTING ====================

    @Test
    @WithMockUser(roles = {"USER"})
    void testMultipleSimultaneousRequests() throws Exception {
        // Given
        when(sqlLoaderConfigService.getConfiguration(anyString()))
                .thenReturn(Optional.of(mockResponse));

        // Simulate multiple concurrent requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/sql-loader/configurations/TEST-CONFIG-" + i))
                    .andExpected(status().isOk())
                    .andExpected(jsonPath("$.success").value(true));
        }
    }

    // ==================== CORS AND SECURITY HEADERS ====================

    @Test
    @WithMockUser(roles = {"USER"})
    void testCorsHeaders() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/sql-loader/configurations/TEST-CONFIG-123")
                .header("Origin", "http://localhost:3000"))
                .andDo(print())
                .andExpected(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
}