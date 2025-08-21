package com.truist.batch.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.truist.batch.builders.TestDataBuilder;
import com.truist.batch.dto.ManualJobConfigRequest;
import com.truist.batch.dto.ManualJobConfigResponse;
import com.truist.batch.service.ConfigurationService;
import com.truist.batch.dao.BatchConfigurationDao;
import com.truist.batch.dao.ConfigurationAuditDao;
import com.truist.batch.testutils.BaseTestNGTest;

import lombok.extern.slf4j.Slf4j;

/**
 * Unit tests for ConfigurationService
 * Tests configuration CRUD operations, validation, and audit trail
 * 
 * @author Fabric Platform Testing Framework
 * @version 1.0.0
 */
@Slf4j
@Test(groups = {"unit", "service", "configuration"})
public class ConfigurationServiceTest extends BaseTestNGTest {

    @Mock
    private BatchConfigurationDao batchConfigurationDao;
    
    @Mock
    private ConfigurationAuditDao configurationAuditDao;
    
    @InjectMocks
    private ConfigurationService configurationService;
    
    private ManualJobConfigRequest validConfigRequest;
    private ManualJobConfigResponse sampleConfigResponse;
    private String testConfigId;
    private String testUserId;
    private String correlationId;
    
    @BeforeMethod
    @Override
    public void setUp() {
        super.setUp();
        initializeTestData();
        setupMockBehavior();
    }
    
    private void initializeTestData() {
        testConfigId = TestDataBuilder.generateConfigId();
        testUserId = "test-user-" + System.currentTimeMillis();
        correlationId = generateCorrelationId();
        
        validConfigRequest = TestDataBuilder.manualJobConfig()
            .withJobName("Test Configuration Job")
            .withDescription("Test job configuration for unit testing")
            .withSourceSystem("HR")
            .withTransactionCode("200")
            .withTemplateId("tpl_hr_200")
            .withMasterQueryId("qry_hr_employees")
            .withStatus("ACTIVE")
            .buildRequest();
            
        sampleConfigResponse = TestDataBuilder.manualJobConfig()
            .withConfigId(testConfigId)
            .withJobName(validConfigRequest.getJobName())
            .withDescription(validConfigRequest.getDescription())
            .withSourceSystem(validConfigRequest.getSourceSystem())
            .withTransactionCode(validConfigRequest.getTransactionCode())
            .withTemplateId(validConfigRequest.getTemplateId())
            .withMasterQueryId(validConfigRequest.getMasterQueryId())
            .withStatus(validConfigRequest.getStatus())
            .buildResponse();
    }
    
    private void setupMockBehavior() {
        // Default successful mock behavior
        when(batchConfigurationDao.createConfiguration(any(), anyString(), anyString()))
            .thenReturn(testConfigId);
            
        when(batchConfigurationDao.getConfigurationById(testConfigId))
            .thenReturn(Optional.of(sampleConfigResponse));
            
        when(batchConfigurationDao.getAllConfigurations())
            .thenReturn(Arrays.asList(sampleConfigResponse));
            
        when(batchConfigurationDao.updateConfiguration(anyString(), any(), anyString(), anyString()))
            .thenReturn(true);
            
        when(batchConfigurationDao.deleteConfiguration(anyString(), anyString(), anyString()))
            .thenReturn(true);
            
        doNothing().when(configurationAuditDao)
            .logConfigurationChange(anyString(), anyString(), anyString(), anyString(), anyString());
    }
    
    @Test(description = "Should create new configuration successfully")
    public void testCreateConfiguration_Success() {
        // Given
        log.info("Testing configuration creation with valid data");
        
        // When
        String createdConfigId = configurationService.createConfiguration(
            validConfigRequest, testUserId, correlationId);
        
        // Then
        assertThat(createdConfigId).isEqualTo(testConfigId);
        
        // Verify DAO interactions
        verify(batchConfigurationDao).createConfiguration(
            eq(validConfigRequest), eq(testUserId), eq(correlationId));
        verify(configurationAuditDao).logConfigurationChange(
            eq(testConfigId), eq("CREATE"), eq(testUserId), eq(correlationId), anyString());
        
        log.info("Configuration created successfully with ID: {}", createdConfigId);
    }
    
    @Test(description = "Should throw exception when creating configuration with null request")
    public void testCreateConfiguration_NullRequest() {
        // Given
        log.info("Testing configuration creation with null request");
        
        // When & Then
        assertThatThrownBy(() -> 
            configurationService.createConfiguration(null, testUserId, correlationId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Configuration request cannot be null");
        
        // Verify no DAO interactions occurred
        verify(batchConfigurationDao, never()).createConfiguration(any(), anyString(), anyString());
        verify(configurationAuditDao, never()).logConfigurationChange(anyString(), anyString(), anyString(), anyString(), anyString());
        
        log.info("Null request validation test completed");
    }
    
    @Test(description = "Should throw exception when creating configuration with empty job name")
    public void testCreateConfiguration_EmptyJobName() {
        // Given
        validConfigRequest.setJobName("");
        log.info("Testing configuration creation with empty job name");
        
        // When & Then
        assertThatThrownBy(() -> 
            configurationService.createConfiguration(validConfigRequest, testUserId, correlationId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Job name cannot be empty");
        
        log.info("Empty job name validation test completed");
    }
    
    @Test(description = "Should retrieve configuration by ID successfully")
    public void testGetConfigurationById_Success() {
        // Given
        log.info("Testing configuration retrieval by ID: {}", testConfigId);
        
        // When
        Optional<ManualJobConfigResponse> result = 
            configurationService.getConfigurationById(testConfigId);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getConfigId()).isEqualTo(testConfigId);
        assertThat(result.get().getJobName()).isEqualTo(validConfigRequest.getJobName());
        assertThat(result.get().getSourceSystem()).isEqualTo(validConfigRequest.getSourceSystem());
        
        // Verify DAO interaction
        verify(batchConfigurationDao).getConfigurationById(testConfigId);
        
        log.info("Configuration retrieved successfully: {}", result.get().getJobName());
    }
    
    @Test(description = "Should return empty optional for non-existent configuration")
    public void testGetConfigurationById_NotFound() {
        // Given
        String nonExistentId = "cfg_nonexistent_999";
        when(batchConfigurationDao.getConfigurationById(nonExistentId))
            .thenReturn(Optional.empty());
        log.info("Testing configuration retrieval for non-existent ID: {}", nonExistentId);
        
        // When
        Optional<ManualJobConfigResponse> result = 
            configurationService.getConfigurationById(nonExistentId);
        
        // Then
        assertThat(result).isEmpty();
        
        verify(batchConfigurationDao).getConfigurationById(nonExistentId);
        log.info("Non-existent configuration handling test completed");
    }
    
    @Test(description = "Should retrieve all configurations successfully")
    public void testGetAllConfigurations_Success() {
        // Given
        ManualJobConfigResponse secondConfig = TestDataBuilder.manualJobConfig()
            .withConfigId("cfg_test_002")
            .withJobName("Second Test Configuration")
            .buildResponse();
            
        when(batchConfigurationDao.getAllConfigurations())
            .thenReturn(Arrays.asList(sampleConfigResponse, secondConfig));
        
        log.info("Testing retrieval of all configurations");
        
        // When
        List<ManualJobConfigResponse> result = configurationService.getAllConfigurations();
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ManualJobConfigResponse::getConfigId)
            .containsExactly(testConfigId, "cfg_test_002");
        
        verify(batchConfigurationDao).getAllConfigurations();
        log.info("All configurations retrieved successfully: {} configs", result.size());
    }
    
    @Test(description = "Should update configuration successfully")
    public void testUpdateConfiguration_Success() {
        // Given
        ManualJobConfigRequest updateRequest = TestDataBuilder.manualJobConfig()
            .withJobName("Updated Configuration Job")
            .withDescription("Updated description for testing")
            .withStatus("INACTIVE")
            .buildRequest();
            
        log.info("Testing configuration update for ID: {}", testConfigId);
        
        // When
        boolean result = configurationService.updateConfiguration(
            testConfigId, updateRequest, testUserId, correlationId);
        
        // Then
        assertThat(result).isTrue();
        
        // Verify DAO interactions
        verify(batchConfigurationDao).updateConfiguration(
            eq(testConfigId), eq(updateRequest), eq(testUserId), eq(correlationId));
        verify(configurationAuditDao).logConfigurationChange(
            eq(testConfigId), eq("UPDATE"), eq(testUserId), eq(correlationId), anyString());
        
        log.info("Configuration updated successfully");
    }
    
    @Test(description = "Should handle update failure gracefully")
    public void testUpdateConfiguration_Failure() {
        // Given
        when(batchConfigurationDao.updateConfiguration(anyString(), any(), anyString(), anyString()))
            .thenReturn(false);
        
        ManualJobConfigRequest updateRequest = TestDataBuilder.manualJobConfig()
            .withJobName("Updated Configuration Job")
            .buildRequest();
        
        log.info("Testing configuration update failure handling");
        
        // When
        boolean result = configurationService.updateConfiguration(
            testConfigId, updateRequest, testUserId, correlationId);
        
        // Then
        assertThat(result).isFalse();
        
        // Verify audit log is still called for failed updates
        verify(configurationAuditDao).logConfigurationChange(
            eq(testConfigId), eq("UPDATE_FAILED"), eq(testUserId), eq(correlationId), anyString());
        
        log.info("Configuration update failure handling test completed");
    }
    
    @Test(description = "Should delete configuration successfully")
    public void testDeleteConfiguration_Success() {
        // Given
        log.info("Testing configuration deletion for ID: {}", testConfigId);
        
        // When
        boolean result = configurationService.deleteConfiguration(
            testConfigId, testUserId, correlationId);
        
        // Then
        assertThat(result).isTrue();
        
        // Verify DAO interactions
        verify(batchConfigurationDao).deleteConfiguration(
            eq(testConfigId), eq(testUserId), eq(correlationId));
        verify(configurationAuditDao).logConfigurationChange(
            eq(testConfigId), eq("DELETE"), eq(testUserId), eq(correlationId), anyString());
        
        log.info("Configuration deleted successfully");
    }
    
    @Test(description = "Should validate configuration request thoroughly")
    public void testValidateConfigurationRequest_Success() {
        // Given
        log.info("Testing configuration request validation");
        
        // When
        boolean isValid = configurationService.validateConfigurationRequest(validConfigRequest);
        
        // Then
        assertThat(isValid).isTrue();
        log.info("Configuration request validation passed");
    }
    
    @Test(description = "Should fail validation for invalid configuration request")
    public void testValidateConfigurationRequest_Failure() {
        // Given
        ManualJobConfigRequest invalidRequest = TestDataBuilder.manualJobConfig()
            .withJobName("")  // Empty job name
            .withSourceSystem(null)  // Null source system
            .buildRequest();
            
        log.info("Testing configuration request validation with invalid data");
        
        // When
        boolean isValid = configurationService.validateConfigurationRequest(invalidRequest);
        
        // Then
        assertThat(isValid).isFalse();
        log.info("Configuration request validation correctly failed for invalid data");
    }
    
    @Test(description = "Should handle database exceptions during configuration creation")
    public void testCreateConfiguration_DatabaseException() {
        // Given
        when(batchConfigurationDao.createConfiguration(any(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Database connection failed"));
            
        log.info("Testing configuration creation with database exception");
        
        // When & Then
        assertThatThrownBy(() -> 
            configurationService.createConfiguration(validConfigRequest, testUserId, correlationId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Database connection failed");
        
        // Verify error audit log
        verify(configurationAuditDao).logConfigurationChange(
            eq(null), eq("CREATE_ERROR"), eq(testUserId), eq(correlationId), anyString());
        
        log.info("Database exception handling test completed");
    }
    
    @Test(description = "Should enforce business rules for configuration status transitions")
    public void testStatusTransitionValidation() {
        // Given
        ManualJobConfigRequest transitionRequest = TestDataBuilder.manualJobConfig()
            .withStatus("ARCHIVED")  // Invalid status transition
            .buildRequest();
            
        log.info("Testing configuration status transition validation");
        
        // When & Then
        assertThatThrownBy(() -> 
            configurationService.updateConfiguration(testConfigId, transitionRequest, testUserId, correlationId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invalid status transition");
        
        log.info("Status transition validation test completed");
    }
    
    @Test(description = "Should track configuration changes with proper audit trail")
    public void testAuditTrailGeneration() {
        // Given
        log.info("Testing audit trail generation for configuration changes");
        
        // When
        configurationService.createConfiguration(validConfigRequest, testUserId, correlationId);
        configurationService.updateConfiguration(testConfigId, validConfigRequest, testUserId, correlationId);
        configurationService.deleteConfiguration(testConfigId, testUserId, correlationId);
        
        // Then
        verify(configurationAuditDao, times(3)).logConfigurationChange(
            anyString(), anyString(), eq(testUserId), eq(correlationId), anyString());
        
        // Verify specific audit log entries
        verify(configurationAuditDao).logConfigurationChange(
            eq(testConfigId), eq("CREATE"), eq(testUserId), eq(correlationId), anyString());
        verify(configurationAuditDao).logConfigurationChange(
            eq(testConfigId), eq("UPDATE"), eq(testUserId), eq(correlationId), anyString());
        verify(configurationAuditDao).logConfigurationChange(
            eq(testConfigId), eq("DELETE"), eq(testUserId), eq(correlationId), anyString());
        
        log.info("Audit trail generation test completed successfully");
    }
    
    @Override
    protected void cleanupTestData() {
        // Reset mocks
        reset(batchConfigurationDao, configurationAuditDao);
        log.debug("Test data cleanup completed for ConfigurationServiceTest");
    }
}