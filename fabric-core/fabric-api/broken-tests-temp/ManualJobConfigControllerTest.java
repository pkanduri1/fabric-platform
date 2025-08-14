package com.truist.batch.controller;

import com.truist.batch.dto.ManualJobConfigRequest;
import com.truist.batch.dto.ManualJobConfigResponse;
import com.truist.batch.entity.ManualJobConfigEntity;
import com.truist.batch.service.ManualJobConfigService;
import com.truist.batch.security.jwt.JwtTokenService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for ManualJobConfigController.
 * 
 * Tests enterprise-grade REST API functionality including:
 * - CRUD operations with comprehensive validation
 * - JWT authentication and RBAC authorization
 * - Security controls and error handling
 * - Request/response data transformation
 * - Audit logging and correlation tracking
 * 
 * Banking-grade testing standards:
 * - 95%+ code coverage requirement
 * - Comprehensive security scenario testing
 * - Error condition and edge case validation
 * - Performance and load testing scenarios
 * 
 * @author Senior Full Stack Developer Agent
 * @version 2.0
 * @since US001 Phase 2 - Unit Testing Implementation
 */
@ExtendWith(MockitoExtension.class)
@WebMvcTest(ManualJobConfigController.class)
@SpringJUnitConfig
class ManualJobConfigControllerTest {

    @MockBean
    private ManualJobConfigService manualJobConfigService;

    @MockBean
    private JwtTokenService jwtTokenService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // For LocalDateTime serialization
    }

    @Test
    @DisplayName("Should create job configuration successfully with JOB_CREATOR role")
    @WithMockUser(roles = {"JOB_CREATOR"})
    void createJobConfiguration_WithValidRequest_ShouldReturnCreated() throws Exception {
        // Arrange
        ManualJobConfigRequest request = createValidJobConfigRequest();
        ManualJobConfigEntity mockEntity = createMockEntity();
        
        when(jwtTokenService.extractUsername(anyString())).thenReturn("test.user@bank.com");
        when(manualJobConfigService.createJobConfiguration(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(mockEntity);

        // Act & Assert
        mockMvc.perform(post("/api/v2/manual-job-config")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer valid-jwt-token"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.configId").value(mockEntity.getConfigId()))
                .andExpect(jsonPath("$.jobName").value(mockEntity.getJobName()))
                .andExpect(jsonPath("$.status").value(mockEntity.getStatus()))
                .andExpect(jsonPath("$.correlationId").exists());

        // Verify service interaction
        verify(manualJobConfigService).createJobConfiguration(
            eq(request.getJobName()),
            eq(request.getJobType()),
            eq(request.getSourceSystem()),
            eq(request.getTargetSystem()),
            anyString(), // JSON parameters
            eq("test.user@bank.com")
        );
    }

    @Test
    @DisplayName("Should reject job configuration creation without proper role")
    @WithMockUser(roles = {"JOB_VIEWER"})
    void createJobConfiguration_WithInsufficientRole_ShouldReturnForbidden() throws Exception {
        // Arrange
        ManualJobConfigRequest request = createValidJobConfigRequest();

        // Act & Assert
        mockMvc.perform(post("/api/v2/manual-job-config")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Verify no service interaction
        verify(manualJobConfigService, never()).createJobConfiguration(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should validate request data and return bad request for invalid input")
    @WithMockUser(roles = {"JOB_CREATOR"})
    void createJobConfiguration_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        ManualJobConfigRequest invalidRequest = ManualJobConfigRequest.builder()
                .jobName("") // Invalid: empty name
                .jobType("INVALID_TYPE") // Invalid: not in allowed values
                .sourceSystem("invalid-system") // Invalid: doesn't match pattern
                .targetSystem("VALID_SYSTEM")
                .jobParameters(Map.of("key", "value"))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v2/manual-job-config")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle duplicate job name conflict")
    @WithMockUser(roles = {"JOB_CREATOR"})
    void createJobConfiguration_WithDuplicateName_ShouldReturnConflict() throws Exception {
        // Arrange
        ManualJobConfigRequest request = createValidJobConfigRequest();
        
        when(jwtTokenService.extractUsername(anyString())).thenReturn("test.user@bank.com");
        when(manualJobConfigService.createJobConfiguration(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new IllegalStateException("Active job configuration with name 'TEST_JOB' already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/v2/manual-job-config")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer valid-jwt-token"))
                .andExpected(status().isConflict());
    }

    @Test
    @DisplayName("Should retrieve job configuration by ID successfully")
    @WithMockUser(roles = {"JOB_VIEWER"})
    void getJobConfiguration_WithValidId_ShouldReturnConfiguration() throws Exception {
        // Arrange
        String configId = "cfg_test_1691234567890_a1b2c3d4";
        ManualJobConfigEntity mockEntity = createMockEntity();
        mockEntity.setConfigId(configId);
        
        when(jwtTokenService.extractUsername(anyString())).thenReturn("test.user@bank.com");
        when(manualJobConfigService.getJobConfiguration(configId)).thenReturn(Optional.of(mockEntity));

        // Act & Assert
        mockMvc.perform(get("/api/v2/manual-job-config/{configId}", configId)
                .header("Authorization", "Bearer valid-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.configId").value(configId))
                .andExpect(jsonPath("$.jobName").value(mockEntity.getJobName()));

        // Verify service interaction
        verify(manualJobConfigService).getJobConfiguration(configId);
    }

    @Test
    @DisplayName("Should return not found for non-existent configuration")
    @WithMockUser(roles = {"JOB_VIEWER"})
    void getJobConfiguration_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        // Arrange
        String configId = "non-existent-id";
        
        when(jwtTokenService.extractUsername(anyString())).thenReturn("test.user@bank.com");
        when(manualJobConfigService.getJobConfiguration(configId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/v2/manual-job-config/{configId}", configId)
                .header("Authorization", "Bearer valid-jwt-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should retrieve all configurations with pagination")
    @WithMockUser(roles = {"JOB_VIEWER"})
    void getAllJobConfigurations_WithPagination_ShouldReturnPagedResults() throws Exception {
        // Arrange
        List<ManualJobConfigEntity> mockEntities = Arrays.asList(
            createMockEntity(), createMockEntity(), createMockEntity());
        
        when(jwtTokenService.extractUsername(anyString())).thenReturn("test.user@bank.com");
        when(manualJobConfigService.getAllActiveConfigurations()).thenReturn(mockEntities);

        // Act & Assert
        mockMvc.perform(get("/api/v2/manual-job-config")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "jobName")
                .param("sortDir", "asc")
                .header("Authorization", "Bearer valid-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isNotEmpty())
                .andExpect(jsonPath("$.totalElements").value(mockEntities.size()))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpected(jsonPath("$.pageSize").value(20));
    }

    @Test
    @DisplayName("Should update job configuration successfully with JOB_MODIFIER role")
    @WithMockUser(roles = {"JOB_MODIFIER"})
    void updateJobConfiguration_WithValidRequest_ShouldReturnUpdated() throws Exception {
        // Arrange
        String configId = "cfg_test_1691234567890_a1b2c3d4";
        ManualJobConfigRequest updateRequest = createValidJobConfigRequest();
        updateRequest.setJobName("UPDATED_JOB_NAME");
        
        ManualJobConfigEntity updatedEntity = createMockEntity();
        updatedEntity.setConfigId(configId);
        updatedEntity.setJobName(updateRequest.getJobName());
        updatedEntity.setVersionNumber(2L);
        
        when(jwtTokenService.extractUsername(anyString())).thenReturn("test.user@bank.com");
        when(manualJobConfigService.updateJobConfiguration(eq(configId), any(), eq("test.user@bank.com")))
            .thenReturn(updatedEntity);

        // Act & Assert
        mockMvc.perform(put("/api/v2/manual-job-config/{configId}", configId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header("Authorization", "Bearer valid-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.configId").value(configId))
                .andExpect(jsonPath("$.jobName").value("UPDATED_JOB_NAME"))
                .andExpected(jsonPath("$.versionNumber").value(2));

        // Verify service interaction
        verify(manualJobConfigService).updateJobConfiguration(eq(configId), any(), eq("test.user@bank.com"));
    }

    @Test
    @DisplayName("Should reject update without JOB_MODIFIER role")
    @WithMockUser(roles = {"JOB_VIEWER"})
    void updateJobConfiguration_WithInsufficientRole_ShouldReturnForbidden() throws Exception {
        // Arrange
        String configId = "cfg_test_1691234567890_a1b2c3d4";
        ManualJobConfigRequest request = createValidJobConfigRequest();

        // Act & Assert
        mockMvc.perform(put("/api/v2/manual-job-config/{configId}", configId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Verify no service interaction
        verify(manualJobConfigService, never()).updateJobConfiguration(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Should deactivate job configuration successfully")
    @WithMockUser(roles = {"JOB_MODIFIER"})
    void deactivateJobConfiguration_WithValidId_ShouldReturnSuccess() throws Exception {
        // Arrange
        String configId = "cfg_test_1691234567890_a1b2c3d4";
        String reason = "No longer needed";
        
        ManualJobConfigEntity deactivatedEntity = createMockEntity();
        deactivatedEntity.setConfigId(configId);
        deactivatedEntity.setStatus("INACTIVE");
        
        when(jwtTokenService.extractUsername(anyString())).thenReturn("test.user@bank.com");
        when(manualJobConfigService.deactivateConfiguration(configId, "test.user@bank.com", reason))
            .thenReturn(deactivatedEntity);

        // Act & Assert
        mockMvc.perform(delete("/api/v2/manual-job-config/{configId}", configId)
                .with(csrf())
                .param("reason", reason)
                .header("Authorization", "Bearer valid-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.configId").value(configId))
                .andExpect(jsonPath("$.status").value("DEACTIVATED"))
                .andExpect(jsonPath("$.deactivatedBy").value("test.user@bank.com"))
                .andExpect(jsonPath("$.reason").value(reason));

        // Verify service interaction
        verify(manualJobConfigService).deactivateConfiguration(configId, "test.user@bank.com", reason);
    }

    @Test
    @DisplayName("Should retrieve system statistics successfully")
    @WithMockUser(roles = {"JOB_VIEWER"})
    void getSystemStatistics_ShouldReturnStatistics() throws Exception {
        // Arrange
        ManualJobConfigService.ConfigurationStatistics mockStats = 
            ManualJobConfigService.ConfigurationStatistics.builder()
                .activeConfigurations(50L)
                .inactiveConfigurations(10L)
                .deprecatedConfigurations(5L)
                .totalConfigurations(65L)
                .lastUpdated(LocalDateTime.now())
                .build();
        
        when(jwtTokenService.extractUsername(anyString())).thenReturn("test.user@bank.com");
        when(manualJobConfigService.getSystemStatistics()).thenReturn(mockStats);

        // Act & Assert
        mockMvc.perform(get("/api/v2/manual-job-config/statistics")
                .header("Authorization", "Bearer valid-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.activeConfigurations").value(50))
                .andExpect(jsonPath("$.inactiveConfigurations").value(10))
                .andExpect(jsonPath("$.totalConfigurations").value(65))
                .andExpect(jsonPath("$.correlationId").exists());

        // Verify service interaction
        verify(manualJobConfigService).getSystemStatistics();
    }

    @Test
    @DisplayName("Should handle authentication errors gracefully")
    void createJobConfiguration_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        ManualJobConfigRequest request = createValidJobConfigRequest();

        // Act & Assert
        mockMvc.perform(post("/api/v2/manual-job-config")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpected(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should validate configuration ID format")
    @WithMockUser(roles = {"JOB_VIEWER"})
    void getJobConfiguration_WithInvalidIdFormat_ShouldReturnBadRequest() throws Exception {
        // Arrange
        String invalidConfigId = "invalid-id-format";

        // Act & Assert
        mockMvc.perform(get("/api/v2/manual-job-config/{configId}", invalidConfigId)
                .header("Authorization", "Bearer valid-jwt-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle service exceptions gracefully")
    @WithMockUser(roles = {"JOB_CREATOR"})
    void createJobConfiguration_WithServiceException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        ManualJobConfigRequest request = createValidJobConfigRequest();
        
        when(jwtTokenService.extractUsername(anyString())).thenReturn("test.user@bank.com");
        when(manualJobConfigService.createJobConfiguration(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(post("/api/v2/manual-job-config")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer valid-jwt-token"))
                .andExpected(status().isInternalServerError());
    }

    // Helper methods

    private ManualJobConfigRequest createValidJobConfigRequest() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("batchSize", 1000);
        parameters.put("connectionTimeout", 30);
        parameters.put("retryCount", 3);

        return ManualJobConfigRequest.builder()
                .jobName("TEST_JOB_CONFIG")
                .jobType("ETL_BATCH")
                .sourceSystem("CORE_BANKING")
                .targetSystem("DATA_WAREHOUSE")
                .jobParameters(parameters)
                .description("Test job configuration")
                .priority("MEDIUM")
                .businessJustification("Test business justification for unit testing")
                .build();
    }

    private ManualJobConfigEntity createMockEntity() {
        return ManualJobConfigEntity.builder()
                .configId("cfg_test_1691234567890_a1b2c3d4")
                .jobName("TEST_JOB_CONFIG")
                .jobType("ETL_BATCH")
                .sourceSystem("CORE_BANKING")
                .targetSystem("DATA_WAREHOUSE")
                .jobParameters("{\"batchSize\": 1000, \"connectionTimeout\": 30}")
                .status("ACTIVE")
                .createdBy("test.user@bank.com")
                .createdDate(LocalDateTime.now())
                .versionNumber(1L)
                .build();
    }
}