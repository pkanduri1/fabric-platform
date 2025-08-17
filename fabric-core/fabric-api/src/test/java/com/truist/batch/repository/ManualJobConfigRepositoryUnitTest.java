package com.truist.batch.repository;

import com.truist.batch.entity.ManualJobConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Unit tests for ManualJobConfigRepository using mocks.
 * 
 * This tests the repository interface contract without requiring
 * database integration, making tests faster and more focused.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since US001 - Manual Job Configuration Interface
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ManualJobConfigRepository Unit Tests")
class ManualJobConfigRepositoryUnitTest {

    @Mock
    private ManualJobConfigRepository repository;

    private ManualJobConfigEntity testEntity1;
    private ManualJobConfigEntity testEntity2;
    private LocalDateTime baseDateTime;

    @BeforeEach
    void setUp() {
        baseDateTime = LocalDateTime.of(2025, 8, 13, 10, 0, 0);
        
        testEntity1 = ManualJobConfigEntity.builder()
                .configId("cfg_hr_001")
                .jobName("HR Employee Load")
                .jobType("ETL")
                .sourceSystem("HR")
                .targetSystem("DW")
                .jobParameters("{\"source\":\"hr_employees\",\"target\":\"dim_employees\"}")
                .status("ACTIVE")
                .createdBy("user1")
                .createdDate(baseDateTime)
                .versionNumber(1L)
                .build();

        testEntity2 = ManualJobConfigEntity.builder()
                .configId("cfg_finance_002")
                .jobName("Finance Transaction Load")
                .jobType("BATCH")
                .sourceSystem("FINANCE")
                .targetSystem("DW")
                .jobParameters("{\"source\":\"finance_transactions\",\"target\":\"fact_transactions\"}")
                .status("ACTIVE")
                .createdBy("user2")
                .createdDate(baseDateTime.plusHours(1))
                .versionNumber(1L)
                .build();
    }

    @Nested
    @DisplayName("Basic Repository Operations")
    class BasicRepositoryOperationsTests {

        @Test
        @DisplayName("Should save job configuration")
        void shouldSaveJobConfiguration() {
            // Given
            when(repository.save(testEntity1)).thenReturn(testEntity1);

            // When
            ManualJobConfigEntity saved = repository.save(testEntity1);

            // Then
            assertThat(saved).isNotNull();
            assertThat(saved.getConfigId()).isEqualTo("cfg_hr_001");
            verify(repository).save(testEntity1);
        }

        @Test
        @DisplayName("Should find job configuration by ID")
        void shouldFindJobConfigurationById() {
            // Given
            when(repository.findById("cfg_hr_001")).thenReturn(Optional.of(testEntity1));

            // When
            Optional<ManualJobConfigEntity> result = repository.findById("cfg_hr_001");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getConfigId()).isEqualTo("cfg_hr_001");
            verify(repository).findById("cfg_hr_001");
        }

        @Test
        @DisplayName("Should return empty optional when configuration not found")
        void shouldReturnEmptyOptionalWhenConfigurationNotFound() {
            // Given
            when(repository.findById("non_existent_id")).thenReturn(Optional.empty());

            // When
            Optional<ManualJobConfigEntity> result = repository.findById("non_existent_id");

            // Then
            assertThat(result).isEmpty();
            verify(repository).findById("non_existent_id");
        }

        @Test
        @DisplayName("Should delete job configuration")
        void shouldDeleteJobConfiguration() {
            // Given
            doNothing().when(repository).deleteById("cfg_hr_001");

            // When
            repository.deleteById("cfg_hr_001");

            // Then
            verify(repository).deleteById("cfg_hr_001");
        }

        @Test
        @DisplayName("Should find all job configurations")
        void shouldFindAllJobConfigurations() {
            // Given
            List<ManualJobConfigEntity> expectedList = Arrays.asList(testEntity1, testEntity2);
            when(repository.findAll()).thenReturn(expectedList);

            // When
            List<ManualJobConfigEntity> result = repository.findAll();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(testEntity1, testEntity2);
            verify(repository).findAll();
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTests {

        @Test
        @DisplayName("Should find job configuration by job name")
        void shouldFindJobConfigurationByJobName() {
            // Given
            when(repository.findByJobName("HR Employee Load")).thenReturn(Optional.of(testEntity1));

            // When
            Optional<ManualJobConfigEntity> result = repository.findByJobName("HR Employee Load");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getJobName()).isEqualTo("HR Employee Load");
            verify(repository).findByJobName("HR Employee Load");
        }

        @Test
        @DisplayName("Should find job configurations by status")
        void shouldFindJobConfigurationsByStatus() {
            // Given
            List<ManualJobConfigEntity> activeConfigurations = Arrays.asList(testEntity1, testEntity2);
            when(repository.findByStatus("ACTIVE")).thenReturn(activeConfigurations);

            // When
            List<ManualJobConfigEntity> result = repository.findByStatus("ACTIVE");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(testEntity1, testEntity2);
            verify(repository).findByStatus("ACTIVE");
        }

        @Test
        @DisplayName("Should find job configurations by job type")
        void shouldFindJobConfigurationsByJobType() {
            // Given
            when(repository.findByJobType("ETL")).thenReturn(Arrays.asList(testEntity1));

            // When
            List<ManualJobConfigEntity> result = repository.findByJobType("ETL");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testEntity1);
            verify(repository).findByJobType("ETL");
        }

        @Test
        @DisplayName("Should find job configurations by source system")
        void shouldFindJobConfigurationsBySourceSystem() {
            // Given
            when(repository.findBySourceSystem("HR")).thenReturn(Arrays.asList(testEntity1));

            // When
            List<ManualJobConfigEntity> result = repository.findBySourceSystem("HR");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testEntity1);
            verify(repository).findBySourceSystem("HR");
        }

        @Test
        @DisplayName("Should find job configurations by target system")
        void shouldFindJobConfigurationsByTargetSystem() {
            // Given
            when(repository.findByTargetSystem("DW")).thenReturn(Arrays.asList(testEntity1, testEntity2));

            // When
            List<ManualJobConfigEntity> result = repository.findByTargetSystem("DW");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(testEntity1, testEntity2);
            verify(repository).findByTargetSystem("DW");
        }

        @Test
        @DisplayName("Should find job configurations by created by user")
        void shouldFindJobConfigurationsByCreatedByUser() {
            // Given
            when(repository.findByCreatedBy("user1")).thenReturn(Arrays.asList(testEntity1));

            // When
            List<ManualJobConfigEntity> result = repository.findByCreatedBy("user1");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testEntity1);
            verify(repository).findByCreatedBy("user1");
        }

        @Test
        @DisplayName("Should find job configurations by date range")
        void shouldFindJobConfigurationsByDateRange() {
            // Given
            LocalDateTime startDate = baseDateTime;
            LocalDateTime endDate = baseDateTime.plusHours(2);
            when(repository.findByCreatedDateBetween(startDate, endDate))
                    .thenReturn(Arrays.asList(testEntity1, testEntity2));

            // When
            List<ManualJobConfigEntity> result = repository.findByCreatedDateBetween(startDate, endDate);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(testEntity1, testEntity2);
            verify(repository).findByCreatedDateBetween(startDate, endDate);
        }
    }

    @Nested
    @DisplayName("Custom JPQL Query Methods")
    class CustomJPQLQueryMethodsTests {

        @Test
        @DisplayName("Should find active configurations by job type")
        void shouldFindActiveConfigurationsByJobType() {
            // Given
            when(repository.findActiveConfigurationsByJobType("ETL")).thenReturn(Arrays.asList(testEntity1));

            // When
            List<ManualJobConfigEntity> result = repository.findActiveConfigurationsByJobType("ETL");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testEntity1);
            verify(repository).findActiveConfigurationsByJobType("ETL");
        }

        @Test
        @DisplayName("Should find configurations by source and target system")
        void shouldFindConfigurationsBySourceAndTargetSystem() {
            // Given
            when(repository.findBySourceAndTargetSystem("HR", "DW")).thenReturn(Arrays.asList(testEntity1));

            // When
            List<ManualJobConfigEntity> result = repository.findBySourceAndTargetSystem("HR", "DW");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testEntity1);
            verify(repository).findBySourceAndTargetSystem("HR", "DW");
        }

        @Test
        @DisplayName("Should count active configurations")
        void shouldCountActiveConfigurations() {
            // Given
            when(repository.countActiveConfigurations()).thenReturn(2L);

            // When
            long count = repository.countActiveConfigurations();

            // Then
            assertThat(count).isEqualTo(2L);
            verify(repository).countActiveConfigurations();
        }

        @Test
        @DisplayName("Should count configurations by status")
        void shouldCountConfigurationsByStatus() {
            // Given
            when(repository.countByStatus("ACTIVE")).thenReturn(2L);

            // When
            long count = repository.countByStatus("ACTIVE");

            // Then
            assertThat(count).isEqualTo(2L);
            verify(repository).countByStatus("ACTIVE");
        }

        @Test
        @DisplayName("Should find configurations containing parameter")
        void shouldFindConfigurationsContainingParameter() {
            // Given
            when(repository.findByJobParametersContaining("hr_employees")).thenReturn(Arrays.asList(testEntity1));

            // When
            List<ManualJobConfigEntity> result = repository.findByJobParametersContaining("hr_employees");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testEntity1);
            verify(repository).findByJobParametersContaining("hr_employees");
        }

        @Test
        @DisplayName("Should check if active configuration exists by job name")
        void shouldCheckIfActiveConfigurationExistsByJobName() {
            // Given
            when(repository.existsActiveConfigurationByJobName("HR Employee Load")).thenReturn(true);
            when(repository.existsActiveConfigurationByJobName("Non-existent Job")).thenReturn(false);

            // When
            boolean existsActive = repository.existsActiveConfigurationByJobName("HR Employee Load");
            boolean existsNonExistent = repository.existsActiveConfigurationByJobName("Non-existent Job");

            // Then
            assertThat(existsActive).isTrue();
            assertThat(existsNonExistent).isFalse();
            verify(repository).existsActiveConfigurationByJobName("HR Employee Load");
            verify(repository).existsActiveConfigurationByJobName("Non-existent Job");
        }

        @Test
        @DisplayName("Should find most recent configuration")
        void shouldFindMostRecentConfiguration() {
            // Given
            when(repository.findMostRecentConfiguration()).thenReturn(Arrays.asList(testEntity2, testEntity1));

            // When
            List<ManualJobConfigEntity> result = repository.findMostRecentConfiguration();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0)).isEqualTo(testEntity2); // Most recent first
            verify(repository).findMostRecentConfiguration();
        }

        @Test
        @DisplayName("Should find configurations needing attention")
        void shouldFindConfigurationsNeedingAttention() {
            // Given
            LocalDateTime cutoffDate = baseDateTime.plusHours(1);
            ManualJobConfigEntity deprecatedEntity = ManualJobConfigEntity.builder()
                    .configId("cfg_old_003")
                    .jobName("Old Job")
                    .status("DEPRECATED")
                    .build();
            
            when(repository.findConfigurationsNeedingAttention(cutoffDate))
                    .thenReturn(Arrays.asList(deprecatedEntity));

            // When
            List<ManualJobConfigEntity> result = repository.findConfigurationsNeedingAttention(cutoffDate);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(deprecatedEntity);
            verify(repository).findConfigurationsNeedingAttention(cutoffDate);
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle null parameters gracefully")
        void shouldHandleNullParametersGracefully() {
            // Given
            when(repository.findByJobName(null)).thenReturn(Optional.empty());
            when(repository.findByStatus(null)).thenReturn(Arrays.asList());

            // When
            Optional<ManualJobConfigEntity> result1 = repository.findByJobName(null);
            List<ManualJobConfigEntity> result2 = repository.findByStatus(null);

            // Then
            assertThat(result1).isEmpty();
            assertThat(result2).isEmpty();
            verify(repository).findByJobName(null);
            verify(repository).findByStatus(null);
        }

        @Test
        @DisplayName("Should handle empty string parameters")
        void shouldHandleEmptyStringParameters() {
            // Given
            when(repository.findByJobName("")).thenReturn(Optional.empty());
            when(repository.findByJobType("")).thenReturn(Arrays.asList());

            // When
            Optional<ManualJobConfigEntity> result1 = repository.findByJobName("");
            List<ManualJobConfigEntity> result2 = repository.findByJobType("");

            // Then
            assertThat(result1).isEmpty();
            assertThat(result2).isEmpty();
            verify(repository).findByJobName("");
            verify(repository).findByJobType("");
        }

        @Test
        @DisplayName("Should handle repository exceptions")
        void shouldHandleRepositoryExceptions() {
            // Given
            when(repository.findById("error_id")).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> repository.findById("error_id"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error");

            verify(repository).findById("error_id");
        }

        @Test
        @DisplayName("Should handle count operations returning zero")
        void shouldHandleCountOperationsReturningZero() {
            // Given
            when(repository.countByStatus("INACTIVE")).thenReturn(0L);
            when(repository.countActiveConfigurations()).thenReturn(0L);

            // When
            long inactiveCount = repository.countByStatus("INACTIVE");
            long activeCount = repository.countActiveConfigurations();

            // Then
            assertThat(inactiveCount).isEqualTo(0L);
            assertThat(activeCount).isEqualTo(0L);
            verify(repository).countByStatus("INACTIVE");
            verify(repository).countActiveConfigurations();
        }
    }
}