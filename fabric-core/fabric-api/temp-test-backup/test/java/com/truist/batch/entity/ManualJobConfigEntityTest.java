package com.fabric.batch.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for ManualJobConfigEntity.
 * 
 * Tests cover:
 * - Entity validation logic
 * - Business logic methods
 * - Builder pattern functionality
 * - Status management
 * - Audit trail fields
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since US001 - Manual Job Configuration Interface
 */
@DisplayName("ManualJobConfigEntity Tests")
class ManualJobConfigEntityTest {

    private ManualJobConfigEntity.ManualJobConfigEntityBuilder baseBuilder;
    
    @BeforeEach
    void setUp() {
        baseBuilder = ManualJobConfigEntity.builder()
                .configId("cfg_hr_1692797600000_abc123")
                .jobName("Test ETL Job")
                .jobType("ETL")
                .sourceSystem("HR")
                .targetSystem("DW")
                .jobParameters("{\"sourceTable\":\"employees\",\"targetTable\":\"dim_employees\"}")
                .status("ACTIVE")
                .createdBy("test.user")
                .createdDate(LocalDateTime.now())
                .versionNumber(1L);
    }

    @Nested
    @DisplayName("Entity Construction Tests")
    class EntityConstructionTests {

        @Test
        @DisplayName("Should create entity with valid builder")
        void shouldCreateEntityWithValidBuilder() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            
            // When
            ManualJobConfigEntity entity = baseBuilder
                    .createdDate(now)
                    .build();
            
            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getConfigId()).isEqualTo("cfg_hr_1692797600000_abc123");
            assertThat(entity.getJobName()).isEqualTo("Test ETL Job");
            assertThat(entity.getJobType()).isEqualTo("ETL");
            assertThat(entity.getSourceSystem()).isEqualTo("HR");
            assertThat(entity.getTargetSystem()).isEqualTo("DW");
            assertThat(entity.getJobParameters()).isEqualTo("{\"sourceTable\":\"employees\",\"targetTable\":\"dim_employees\"}");
            assertThat(entity.getStatus()).isEqualTo("ACTIVE");
            assertThat(entity.getCreatedBy()).isEqualTo("test.user");
            assertThat(entity.getCreatedDate()).isEqualTo(now);
            assertThat(entity.getVersionNumber()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should set default status to ACTIVE")
        void shouldSetDefaultStatusToActive() {
            // Given & When
            ManualJobConfigEntity entity = ManualJobConfigEntity.builder()
                    .configId("test-id")
                    .jobName("test-job")
                    .jobType("ETL")
                    .sourceSystem("SRC")
                    .targetSystem("TGT")
                    .jobParameters("{}")
                    .createdBy("user")
                    .build();
            
            // Then
            assertThat(entity.getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("Should set default version number to 1")
        void shouldSetDefaultVersionNumberToOne() {
            // Given & When
            ManualJobConfigEntity entity = ManualJobConfigEntity.builder()
                    .configId("test-id")
                    .jobName("test-job")
                    .jobType("ETL")
                    .sourceSystem("SRC")
                    .targetSystem("TGT")
                    .jobParameters("{}")
                    .createdBy("user")
                    .build();
            
            // Then
            assertThat(entity.getVersionNumber()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Status Management Tests")
    class StatusManagementTests {

        @Test
        @DisplayName("Should return true for active configuration")
        void shouldReturnTrueForActiveConfiguration() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.status("ACTIVE").build();
            
            // When & Then
            assertThat(entity.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should return false for inactive configuration")
        void shouldReturnFalseForInactiveConfiguration() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.status("INACTIVE").build();
            
            // When & Then
            assertThat(entity.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should return false for deprecated configuration")
        void shouldReturnFalseForDeprecatedConfiguration() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.status("DEPRECATED").build();
            
            // When & Then
            assertThat(entity.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should activate configuration")
        void shouldActivateConfiguration() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.status("INACTIVE").build();
            
            // When
            entity.activate();
            
            // Then
            assertThat(entity.getStatus()).isEqualTo("ACTIVE");
            assertThat(entity.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should deactivate configuration")
        void shouldDeactivateConfiguration() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.status("ACTIVE").build();
            
            // When
            entity.deactivate();
            
            // Then
            assertThat(entity.getStatus()).isEqualTo("INACTIVE");
            assertThat(entity.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should deprecate configuration")
        void shouldDeprecateConfiguration() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.status("ACTIVE").build();
            
            // When
            entity.deprecate();
            
            // Then
            assertThat(entity.getStatus()).isEqualTo("DEPRECATED");
            assertThat(entity.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Display Name Tests")
    class DisplayNameTests {

        @Test
        @DisplayName("Should generate display name with source and target systems")
        void shouldGenerateDisplayNameWithSystems() {
            // Given
            ManualJobConfigEntity entity = baseBuilder
                    .jobName("Employee Data Load")
                    .sourceSystem("HR_SYSTEM")
                    .targetSystem("DATA_WAREHOUSE")
                    .build();
            
            // When
            String displayName = entity.getDisplayName();
            
            // Then
            assertThat(displayName).isEqualTo("Employee Data Load (HR_SYSTEM -> DATA_WAREHOUSE)");
        }

        @Test
        @DisplayName("Should handle empty or null values in display name")
        void shouldHandleEmptyValuesInDisplayName() {
            // Given
            ManualJobConfigEntity entity = baseBuilder
                    .jobName("")
                    .sourceSystem("")
                    .targetSystem("")
                    .build();
            
            // When
            String displayName = entity.getDisplayName();
            
            // Then
            assertThat(displayName).isEqualTo(" ( -> )");
        }
    }

    @Nested
    @DisplayName("Configuration Validation Tests")
    class ConfigurationValidationTests {

        @Test
        @DisplayName("Should validate complete configuration as valid")
        void shouldValidateCompleteConfigurationAsValid() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.build();
            
            // When & Then
            assertThat(entity.isValidConfiguration()).isTrue();
        }

        @Test
        @DisplayName("Should invalidate configuration with null configId")
        void shouldInvalidateConfigurationWithNullConfigId() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.configId(null).build();
            
            // When & Then
            assertThat(entity.isValidConfiguration()).isFalse();
        }

        @Test
        @DisplayName("Should invalidate configuration with empty configId")
        void shouldInvalidateConfigurationWithEmptyConfigId() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.configId("").build();
            
            // When & Then
            assertThat(entity.isValidConfiguration()).isFalse();
        }

        @Test
        @DisplayName("Should invalidate configuration with whitespace-only configId")
        void shouldInvalidateConfigurationWithWhitespaceOnlyConfigId() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.configId("   ").build();
            
            // When & Then
            assertThat(entity.isValidConfiguration()).isFalse();
        }

        @Test
        @DisplayName("Should invalidate configuration with null jobName")
        void shouldInvalidateConfigurationWithNullJobName() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.jobName(null).build();
            
            // When & Then
            assertThat(entity.isValidConfiguration()).isFalse();
        }

        @Test
        @DisplayName("Should invalidate configuration with empty jobName")
        void shouldInvalidateConfigurationWithEmptyJobName() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.jobName("").build();
            
            // When & Then
            assertThat(entity.isValidConfiguration()).isFalse();
        }

        @Test
        @DisplayName("Should invalidate configuration with null jobType")
        void shouldInvalidateConfigurationWithNullJobType() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.jobType(null).build();
            
            // When & Then
            assertThat(entity.isValidConfiguration()).isFalse();
        }

        @Test
        @DisplayName("Should invalidate configuration with empty jobType")
        void shouldInvalidateConfigurationWithEmptyJobType() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.jobType("").build();
            
            // When & Then
            assertThat(entity.isValidConfiguration()).isFalse();
        }

        @Test
        @DisplayName("Should invalidate configuration with null sourceSystem")
        void shouldInvalidateConfigurationWithNullSourceSystem() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.sourceSystem(null).build();
            
            // When & Then
            assertThat(entity.isValidConfiguration()).isFalse();
        }

        @Test
        @DisplayName("Should invalidate configuration with empty sourceSystem")
        void shouldInvalidateConfigurationWithEmptySourceSystem() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.sourceSystem("").build();
            
            // When & Then
            assertThat(entity.isValidConfiguration()).isFalse();
        }

        @Test
        @DisplayName("Should invalidate configuration with null targetSystem")
        void shouldInvalidateConfigurationWithNullTargetSystem() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.targetSystem(null).build();
            
            // When & Then
            assertThat(entity.isValidConfiguration()).isFalse();
        }

        @Test
        @DisplayName("Should invalidate configuration with empty targetSystem")
        void shouldInvalidateConfigurationWithEmptyTargetSystem() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.targetSystem("").build();
            
            // When & Then
            assertThat(entity.isValidConfiguration()).isFalse();
        }

        @Test
        @DisplayName("Should invalidate configuration with null jobParameters")
        void shouldInvalidateConfigurationWithNullJobParameters() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.jobParameters(null).build();
            
            // When & Then
            assertThat(entity.isValidConfiguration()).isFalse();
        }

        @Test
        @DisplayName("Should invalidate configuration with empty jobParameters")
        void shouldInvalidateConfigurationWithEmptyJobParameters() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.jobParameters("").build();
            
            // When & Then
            assertThat(entity.isValidConfiguration()).isFalse();
        }

        @Test
        @DisplayName("Should invalidate configuration with null createdBy")
        void shouldInvalidateConfigurationWithNullCreatedBy() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.createdBy(null).build();
            
            // When & Then
            assertThat(entity.isValidConfiguration()).isFalse();
        }

        @Test
        @DisplayName("Should invalidate configuration with empty createdBy")
        void shouldInvalidateConfigurationWithEmptyCreatedBy() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.createdBy("").build();
            
            // When & Then
            assertThat(entity.isValidConfiguration()).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null status gracefully in isActive method")
        void shouldHandleNullStatusGracefullyInIsActiveMethod() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.status(null).build();
            
            // When & Then
            assertThat(entity.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should handle case sensitivity in status comparison")
        void shouldHandleCaseSensitivityInStatusComparison() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.status("active").build();
            
            // When & Then
            assertThat(entity.isActive()).isFalse(); // Case sensitive comparison
        }

        @Test
        @DisplayName("Should handle status with whitespace")
        void shouldHandleStatusWithWhitespace() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.status(" ACTIVE ").build();
            
            // When & Then
            assertThat(entity.isActive()).isFalse(); // Exact match required
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when all fields are identical")
        void shouldBeEqualWhenAllFieldsAreIdentical() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            ManualJobConfigEntity entity1 = baseBuilder.createdDate(now).build();
            ManualJobConfigEntity entity2 = baseBuilder.createdDate(now).build();
            
            // When & Then
            assertThat(entity1).isEqualTo(entity2);
            assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when configId differs")
        void shouldNotBeEqualWhenConfigIdDiffers() {
            // Given
            ManualJobConfigEntity entity1 = baseBuilder.configId("id1").build();
            ManualJobConfigEntity entity2 = baseBuilder.configId("id2").build();
            
            // When & Then
            assertThat(entity1).isNotEqualTo(entity2);
        }

        @Test
        @DisplayName("Should not be equal when comparing to null")
        void shouldNotBeEqualWhenComparingToNull() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.build();
            
            // When & Then
            assertThat(entity).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal when comparing to different class")
        void shouldNotBeEqualWhenComparingToDifferentClass() {
            // Given
            ManualJobConfigEntity entity = baseBuilder.build();
            String differentObject = "not an entity";
            
            // When & Then
            assertThat(entity).isNotEqualTo(differentObject);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should generate meaningful toString representation")
        void shouldGenerateMeaningfulToStringRepresentation() {
            // Given
            ManualJobConfigEntity entity = baseBuilder
                    .configId("test-config-id")
                    .jobName("Test Job")
                    .build();
            
            // When
            String toString = entity.toString();
            
            // Then
            assertThat(toString).isNotNull();
            assertThat(toString).contains("ManualJobConfigEntity");
            assertThat(toString).contains("configId=test-config-id");
            assertThat(toString).contains("jobName=Test Job");
        }
    }
}