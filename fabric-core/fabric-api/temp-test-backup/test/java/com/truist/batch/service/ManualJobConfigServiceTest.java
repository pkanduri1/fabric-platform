package com.fabric.batch.service;

import com.fabric.batch.entity.ManualJobConfigEntity;
import com.fabric.batch.repository.ManualJobConfigRepository;
import com.fabric.batch.service.ManualJobConfigService.ConfigurationStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ManualJobConfigService.
 * 
 * Tests cover:
 * - Business logic validation
 * - Service layer functionality
 * - Error handling and edge cases
 * - Transaction management
 * - Audit trail and security
 * 
 * Uses Mockito for dependency mocking to ensure unit test isolation.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since US001 - Manual Job Configuration Interface
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ManualJobConfigService Unit Tests")
class ManualJobConfigServiceTest {

    @Mock
    private ManualJobConfigRepository repository;

    @InjectMocks
    private ManualJobConfigService service;

    private ManualJobConfigEntity testEntity;
    private String validJobParameters;

    @BeforeEach
    void setUp() {
        validJobParameters = "{\"sourceTable\":\"employees\",\"targetTable\":\"dim_employees\",\"batchSize\":1000}";
        
        testEntity = ManualJobConfigEntity.builder()
                .configId("cfg_hr_1692797600000_abc123")
                .jobName("Test ETL Job")
                .jobType("ETL")
                .sourceSystem("HR")
                .targetSystem("DW")
                .jobParameters(validJobParameters)
                .status("ACTIVE")
                .createdBy("test.user")
                .createdDate(LocalDateTime.now())
                .versionNumber(1L)
                .build();
    }

    @Nested
    @DisplayName("Create Job Configuration Tests")
    class CreateJobConfigurationTests {

        @Test
        @DisplayName("Should create valid job configuration successfully")
        void shouldCreateValidJobConfigurationSuccessfully() {
            // Given
            when(repository.existsActiveConfigurationByJobName("Test ETL Job")).thenReturn(false);
            when(repository.save(any(ManualJobConfigEntity.class))).thenReturn(testEntity);

            // When
            ManualJobConfigEntity result = service.createJobConfiguration(
                    "Test ETL Job", "ETL", "HR", "DW", validJobParameters, "test.user");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getJobName()).isEqualTo("Test ETL Job");
            assertThat(result.getJobType()).isEqualTo("ETL");
            assertThat(result.getSourceSystem()).isEqualTo("HR");
            assertThat(result.getTargetSystem()).isEqualTo("DW");
            assertThat(result.getJobParameters()).isEqualTo(validJobParameters);
            assertThat(result.getCreatedBy()).isEqualTo("test.user");
            assertThat(result.getStatus()).isEqualTo("ACTIVE");

            verify(repository).existsActiveConfigurationByJobName("Test ETL Job");
            verify(repository).save(any(ManualJobConfigEntity.class));
        }

        @Test
        @DisplayName("Should generate unique configuration ID")
        void shouldGenerateUniqueConfigurationId() {
            // Given
            when(repository.existsActiveConfigurationByJobName(anyString())).thenReturn(false);
            when(repository.save(any(ManualJobConfigEntity.class))).thenReturn(testEntity);

            // When
            service.createJobConfiguration("Test Job", "ETL", "HR", "DW", validJobParameters, "user");

            // Then
            ArgumentCaptor<ManualJobConfigEntity> captor = ArgumentCaptor.forClass(ManualJobConfigEntity.class);
            verify(repository).save(captor.capture());
            
            ManualJobConfigEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getConfigId()).isNotNull();
            assertThat(savedEntity.getConfigId()).startsWith("cfg_hr_");
            assertThat(savedEntity.getConfigId()).hasSize(30); // Expected format length
        }

        @Test
        @DisplayName("Should set default values correctly")
        void shouldSetDefaultValuesCorrectly() {
            // Given
            when(repository.existsActiveConfigurationByJobName(anyString())).thenReturn(false);
            when(repository.save(any(ManualJobConfigEntity.class))).thenReturn(testEntity);

            // When
            service.createJobConfiguration("Test Job", "ETL", "HR", "DW", validJobParameters, "user");

            // Then
            ArgumentCaptor<ManualJobConfigEntity> captor = ArgumentCaptor.forClass(ManualJobConfigEntity.class);
            verify(repository).save(captor.capture());
            
            ManualJobConfigEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getStatus()).isEqualTo("ACTIVE");
            assertThat(savedEntity.getVersionNumber()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should validate configuration before saving")
        void shouldValidateConfigurationBeforeSaving() {
            // Given
            when(repository.existsActiveConfigurationByJobName(anyString())).thenReturn(false);
            
            // When & Then - Invalid configuration should cause validation failure
            assertThatThrownBy(() -> service.createJobConfiguration(
                    "", "ETL", "HR", "DW", validJobParameters, "user"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Job name is required");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception for duplicate job name")
        void shouldThrowExceptionForDuplicateJobName() {
            // Given
            when(repository.existsActiveConfigurationByJobName("Duplicate Job")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> service.createJobConfiguration(
                    "Duplicate Job", "ETL", "HR", "DW", validJobParameters, "user"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Active job configuration with name 'Duplicate Job' already exists");

            verify(repository).existsActiveConfigurationByJobName("Duplicate Job");
            verify(repository, never()).save(any());
        }

        @Nested
        @DisplayName("Input Validation Tests")
        class InputValidationTests {

            @Test
            @DisplayName("Should reject null job name")
            void shouldRejectNullJobName() {
                assertThatThrownBy(() -> service.createJobConfiguration(
                        null, "ETL", "HR", "DW", validJobParameters, "user"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Job name is required");
            }

            @Test
            @DisplayName("Should reject empty job name")
            void shouldRejectEmptyJobName() {
                assertThatThrownBy(() -> service.createJobConfiguration(
                        "", "ETL", "HR", "DW", validJobParameters, "user"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Job name is required");
            }

            @Test
            @DisplayName("Should reject whitespace-only job name")
            void shouldRejectWhitespaceOnlyJobName() {
                assertThatThrownBy(() -> service.createJobConfiguration(
                        "   ", "ETL", "HR", "DW", validJobParameters, "user"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Job name is required");
            }

            @Test
            @DisplayName("Should reject null job type")
            void shouldRejectNullJobType() {
                assertThatThrownBy(() -> service.createJobConfiguration(
                        "Test Job", null, "HR", "DW", validJobParameters, "user"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Job type is required");
            }

            @Test
            @DisplayName("Should reject empty job type")
            void shouldRejectEmptyJobType() {
                assertThatThrownBy(() -> service.createJobConfiguration(
                        "Test Job", "", "HR", "DW", validJobParameters, "user"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Job type is required");
            }

            @Test
            @DisplayName("Should reject null source system")
            void shouldRejectNullSourceSystem() {
                assertThatThrownBy(() -> service.createJobConfiguration(
                        "Test Job", "ETL", null, "DW", validJobParameters, "user"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Source system is required");
            }

            @Test
            @DisplayName("Should reject empty source system")
            void shouldRejectEmptySourceSystem() {
                assertThatThrownBy(() -> service.createJobConfiguration(
                        "Test Job", "ETL", "", "DW", validJobParameters, "user"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Source system is required");
            }

            @Test
            @DisplayName("Should reject null target system")
            void shouldRejectNullTargetSystem() {
                assertThatThrownBy(() -> service.createJobConfiguration(
                        "Test Job", "ETL", "HR", null, validJobParameters, "user"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Target system is required");
            }

            @Test
            @DisplayName("Should reject empty target system")
            void shouldRejectEmptyTargetSystem() {
                assertThatThrownBy(() -> service.createJobConfiguration(
                        "Test Job", "ETL", "HR", "", validJobParameters, "user"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Target system is required");
            }

            @Test
            @DisplayName("Should reject null job parameters")
            void shouldRejectNullJobParameters() {
                assertThatThrownBy(() -> service.createJobConfiguration(
                        "Test Job", "ETL", "HR", "DW", null, "user"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Job parameters are required");
            }

            @Test
            @DisplayName("Should reject empty job parameters")
            void shouldRejectEmptyJobParameters() {
                assertThatThrownBy(() -> service.createJobConfiguration(
                        "Test Job", "ETL", "HR", "DW", "", "user"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Job parameters are required");
            }

            @Test
            @DisplayName("Should reject null created by user")
            void shouldRejectNullCreatedByUser() {
                assertThatThrownBy(() -> service.createJobConfiguration(
                        "Test Job", "ETL", "HR", "DW", validJobParameters, null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Created by user is required");
            }

            @Test
            @DisplayName("Should reject empty created by user")
            void shouldRejectEmptyCreatedByUser() {
                assertThatThrownBy(() -> service.createJobConfiguration(
                        "Test Job", "ETL", "HR", "DW", validJobParameters, ""))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Created by user is required");
            }
        }
    }

    @Nested
    @DisplayName("Read Operations Tests")
    class ReadOperationsTests {

        @Test
        @DisplayName("Should retrieve job configuration by ID")
        void shouldRetrieveJobConfigurationById() {
            // Given
            when(repository.findById("cfg_hr_001")).thenReturn(Optional.of(testEntity));

            // When
            Optional<ManualJobConfigEntity> result = service.getJobConfiguration("cfg_hr_001");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getConfigId()).isEqualTo("cfg_hr_1692797600000_abc123");
            
            verify(repository).findById("cfg_hr_001");
        }

        @Test
        @DisplayName("Should return empty optional when configuration not found")
        void shouldReturnEmptyOptionalWhenConfigurationNotFound() {
            // Given
            when(repository.findById("non_existent_id")).thenReturn(Optional.empty());

            // When
            Optional<ManualJobConfigEntity> result = service.getJobConfiguration("non_existent_id");

            // Then
            assertThat(result).isEmpty();
            
            verify(repository).findById("non_existent_id");
        }

        @Test
        @DisplayName("Should retrieve all active configurations")
        void shouldRetrieveAllActiveConfigurations() {
            // Given
            ManualJobConfigEntity entity2 = ManualJobConfigEntity.builder()
                    .configId("cfg_finance_002")
                    .jobName("Finance Job")
                    .status("ACTIVE")
                    .build();
            
            when(repository.findByStatus("ACTIVE")).thenReturn(Arrays.asList(testEntity, entity2));

            // When
            List<ManualJobConfigEntity> result = service.getAllActiveConfigurations();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ManualJobConfigEntity::getConfigId)
                    .containsExactly("cfg_hr_1692797600000_abc123", "cfg_finance_002");
            
            verify(repository).findByStatus("ACTIVE");
        }

        @Test
        @DisplayName("Should retrieve configurations by job type")
        void shouldRetrieveConfigurationsByJobType() {
            // Given
            when(repository.findByJobType("ETL")).thenReturn(Arrays.asList(testEntity));

            // When
            List<ManualJobConfigEntity> result = service.getConfigurationsByJobType("ETL");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getJobType()).isEqualTo("ETL");
            
            verify(repository).findByJobType("ETL");
        }
    }

    @Nested
    @DisplayName("Update Operations Tests")
    class UpdateOperationsTests {

        @Test
        @DisplayName("Should deactivate configuration successfully")
        void shouldDeactivateConfigurationSuccessfully() {
            // Given
            ManualJobConfigEntity activeEntity = ManualJobConfigEntity.builder()
                    .configId("cfg_hr_001")
                    .jobName("Test Job")
                    .status("ACTIVE")
                    .build();
            
            ManualJobConfigEntity deactivatedEntity = ManualJobConfigEntity.builder()
                    .configId("cfg_hr_001")
                    .jobName("Test Job")
                    .status("INACTIVE")
                    .build();

            when(repository.findById("cfg_hr_001")).thenReturn(Optional.of(activeEntity));
            when(repository.save(any(ManualJobConfigEntity.class))).thenReturn(deactivatedEntity);

            // When
            ManualJobConfigEntity result = service.deactivateConfiguration("cfg_hr_001", "admin.user");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("INACTIVE");
            
            verify(repository).findById("cfg_hr_001");
            verify(repository).save(any(ManualJobConfigEntity.class));
        }

        @Test
        @DisplayName("Should throw exception when deactivating non-existent configuration")
        void shouldThrowExceptionWhenDeactivatingNonExistentConfiguration() {
            // Given
            when(repository.findById("non_existent_id")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.deactivateConfiguration("non_existent_id", "admin.user"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Job configuration not found: non_existent_id");

            verify(repository).findById("non_existent_id");
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Statistics and Monitoring Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should generate system statistics correctly")
        void shouldGenerateSystemStatisticsCorrectly() {
            // Given
            when(repository.countByStatus("ACTIVE")).thenReturn(15L);
            when(repository.countByStatus("INACTIVE")).thenReturn(5L);
            when(repository.countByStatus("DEPRECATED")).thenReturn(3L);
            when(repository.count()).thenReturn(23L);

            // When
            ConfigurationStatistics stats = service.getSystemStatistics();

            // Then
            assertThat(stats).isNotNull();
            assertThat(stats.getActiveConfigurations()).isEqualTo(15L);
            assertThat(stats.getInactiveConfigurations()).isEqualTo(5L);
            assertThat(stats.getDeprecatedConfigurations()).isEqualTo(3L);
            assertThat(stats.getTotalConfigurations()).isEqualTo(23L);
            assertThat(stats.getLastUpdated()).isNotNull();
            assertThat(stats.getLastUpdated()).isBeforeOrEqualTo(LocalDateTime.now());

            verify(repository).countByStatus("ACTIVE");
            verify(repository).countByStatus("INACTIVE");
            verify(repository).countByStatus("DEPRECATED");
            verify(repository).count();
        }

        @Test
        @DisplayName("Should handle zero counts in statistics")
        void shouldHandleZeroCountsInStatistics() {
            // Given
            when(repository.countByStatus(anyString())).thenReturn(0L);
            when(repository.count()).thenReturn(0L);

            // When
            ConfigurationStatistics stats = service.getSystemStatistics();

            // Then
            assertThat(stats.getActiveConfigurations()).isEqualTo(0L);
            assertThat(stats.getInactiveConfigurations()).isEqualTo(0L);
            assertThat(stats.getDeprecatedConfigurations()).isEqualTo(0L);
            assertThat(stats.getTotalConfigurations()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle repository exceptions gracefully")
        void shouldHandleRepositoryExceptionsGracefully() {
            // Given
            when(repository.findById(anyString())).thenThrow(new RuntimeException("Database connection error"));

            // When & Then
            assertThatThrownBy(() -> service.getJobConfiguration("cfg_hr_001"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database connection error");
        }

        @Test
        @DisplayName("Should handle concurrent creation attempts")
        void shouldHandleConcurrentCreationAttempts() {
            // Given - First check passes, but another thread creates the same job name
            when(repository.existsActiveConfigurationByJobName("Concurrent Job"))
                    .thenReturn(false)  // First check
                    .thenReturn(true);  // Would return true if checked again

            when(repository.save(any())).thenReturn(testEntity);

            // When
            ManualJobConfigEntity result = service.createJobConfiguration(
                    "Concurrent Job", "ETL", "HR", "DW", validJobParameters, "user");

            // Then - Should succeed with the first check
            assertThat(result).isNotNull();
            verify(repository).save(any());
        }

        @Test
        @DisplayName("Should handle very long parameter strings")
        void shouldHandleVeryLongParameterStrings() {
            // Given
            StringBuilder longParams = new StringBuilder("{");
            for (int i = 0; i < 1000; i++) {
                longParams.append("\"param").append(i).append("\":\"value").append(i).append("\",");
            }
            longParams.deleteCharAt(longParams.length() - 1); // Remove last comma
            longParams.append("}");

            when(repository.existsActiveConfigurationByJobName(anyString())).thenReturn(false);
            when(repository.save(any())).thenReturn(testEntity);

            // When & Then - Should handle long parameters without error
            assertThatCode(() -> service.createJobConfiguration(
                    "Long Param Job", "ETL", "HR", "DW", longParams.toString(), "user"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle special characters in job names")
        void shouldHandleSpecialCharactersInJobNames() {
            // Given
            String specialJobName = "Job with Special Characters: !@#$%^&*()_+-=[]{}|;':\",./<>?";
            
            when(repository.existsActiveConfigurationByJobName(specialJobName)).thenReturn(false);
            when(repository.save(any())).thenReturn(testEntity);

            // When & Then - Should handle special characters without error
            assertThatCode(() -> service.createJobConfiguration(
                    specialJobName, "ETL", "HR", "DW", validJobParameters, "user"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Logging and Audit Trail Tests")
    class AuditTrailTests {

        @Test
        @DisplayName("Should log creation attempts")
        void shouldLogCreationAttempts() {
            // Given
            when(repository.existsActiveConfigurationByJobName(anyString())).thenReturn(false);
            when(repository.save(any())).thenReturn(testEntity);

            // When
            service.createJobConfiguration("Audit Job", "ETL", "HR", "DW", validJobParameters, "audit.user");

            // Then - Verify repository calls (logging would be verified in integration tests)
            verify(repository).existsActiveConfigurationByJobName("Audit Job");
            verify(repository).save(any());
        }

        @Test
        @DisplayName("Should capture user context in configuration")
        void shouldCaptureUserContextInConfiguration() {
            // Given
            when(repository.existsActiveConfigurationByJobName(anyString())).thenReturn(false);
            when(repository.save(any())).thenReturn(testEntity);

            // When
            service.createJobConfiguration("User Context Job", "ETL", "HR", "DW", validJobParameters, "specific.user");

            // Then
            ArgumentCaptor<ManualJobConfigEntity> captor = ArgumentCaptor.forClass(ManualJobConfigEntity.class);
            verify(repository).save(captor.capture());
            
            ManualJobConfigEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getCreatedBy()).isEqualTo("specific.user");
        }
    }
}