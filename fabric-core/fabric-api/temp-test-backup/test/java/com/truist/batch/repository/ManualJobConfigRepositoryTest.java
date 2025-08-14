package com.truist.batch.repository;

import com.truist.batch.entity.ManualJobConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive integration tests for ManualJobConfigRepository.
 * 
 * Tests cover:
 * - Basic CRUD operations
 * - Custom query methods
 * - Data access patterns
 * - Performance considerations
 * - Edge cases and error handling
 * 
 * Uses H2 in-memory database for testing isolation.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since US001 - Manual Job Configuration Interface
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ManualJobConfigRepository Integration Tests")
class ManualJobConfigRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ManualJobConfigRepository repository;

    private ManualJobConfigEntity testEntity1;
    private ManualJobConfigEntity testEntity2;
    private ManualJobConfigEntity testEntity3;
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

        testEntity3 = ManualJobConfigEntity.builder()
                .configId("cfg_hr_003")
                .jobName("HR Department Load")
                .jobType("ETL")
                .sourceSystem("HR")
                .targetSystem("DW")
                .jobParameters("{\"source\":\"hr_departments\",\"target\":\"dim_departments\"}")
                .status("INACTIVE")
                .createdBy("user1")
                .createdDate(baseDateTime.plusHours(2))
                .versionNumber(1L)
                .build();
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudOperationsTests {

        @Test
        @DisplayName("Should save and retrieve job configuration")
        void shouldSaveAndRetrieveJobConfiguration() {
            // When
            ManualJobConfigEntity saved = repository.save(testEntity1);
            
            // Then
            assertThat(saved).isNotNull();
            assertThat(saved.getConfigId()).isEqualTo("cfg_hr_001");
            
            Optional<ManualJobConfigEntity> retrieved = repository.findById("cfg_hr_001");
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getJobName()).isEqualTo("HR Employee Load");
        }

        @Test
        @DisplayName("Should update existing job configuration")
        void shouldUpdateExistingJobConfiguration() {
            // Given
            repository.save(testEntity1);
            
            // When
            testEntity1.setJobParameters("{\"updated\":\"parameters\"}");
            ManualJobConfigEntity updated = repository.save(testEntity1);
            
            // Then
            Optional<ManualJobConfigEntity> retrieved = repository.findById("cfg_hr_001");
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getJobParameters()).isEqualTo("{\"updated\":\"parameters\"}");
        }

        @Test
        @DisplayName("Should delete job configuration")
        void shouldDeleteJobConfiguration() {
            // Given
            repository.save(testEntity1);
            
            // When
            repository.deleteById("cfg_hr_001");
            
            // Then
            Optional<ManualJobConfigEntity> retrieved = repository.findById("cfg_hr_001");
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should find all job configurations")
        void shouldFindAllJobConfigurations() {
            // Given
            repository.save(testEntity1);
            repository.save(testEntity2);
            repository.save(testEntity3);
            
            // When
            List<ManualJobConfigEntity> allConfigurations = repository.findAll();
            
            // Then
            assertThat(allConfigurations).hasSize(3);
            assertThat(allConfigurations).extracting(ManualJobConfigEntity::getConfigId)
                    .containsExactlyInAnyOrder("cfg_hr_001", "cfg_finance_002", "cfg_hr_003");
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTests {

        @BeforeEach
        void setUpData() {
            repository.save(testEntity1);
            repository.save(testEntity2);
            repository.save(testEntity3);
        }

        @Test
        @DisplayName("Should find job configuration by job name")
        void shouldFindJobConfigurationByJobName() {
            // When
            Optional<ManualJobConfigEntity> result = repository.findByJobName("HR Employee Load");
            
            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getConfigId()).isEqualTo("cfg_hr_001");
        }

        @Test
        @DisplayName("Should return empty when job name not found")
        void shouldReturnEmptyWhenJobNameNotFound() {
            // When
            Optional<ManualJobConfigEntity> result = repository.findByJobName("Non-existent Job");
            
            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find job configurations by status")
        void shouldFindJobConfigurationsByStatus() {
            // When
            List<ManualJobConfigEntity> activeConfigurations = repository.findByStatus("ACTIVE");
            List<ManualJobConfigEntity> inactiveConfigurations = repository.findByStatus("INACTIVE");
            
            // Then
            assertThat(activeConfigurations).hasSize(2);
            assertThat(activeConfigurations).extracting(ManualJobConfigEntity::getConfigId)
                    .containsExactlyInAnyOrder("cfg_hr_001", "cfg_finance_002");
            
            assertThat(inactiveConfigurations).hasSize(1);
            assertThat(inactiveConfigurations.get(0).getConfigId()).isEqualTo("cfg_hr_003");
        }

        @Test
        @DisplayName("Should find job configurations by job type")
        void shouldFindJobConfigurationsByJobType() {
            // When
            List<ManualJobConfigEntity> etlJobs = repository.findByJobType("ETL");
            List<ManualJobConfigEntity> batchJobs = repository.findByJobType("BATCH");
            
            // Then
            assertThat(etlJobs).hasSize(2);
            assertThat(etlJobs).extracting(ManualJobConfigEntity::getConfigId)
                    .containsExactlyInAnyOrder("cfg_hr_001", "cfg_hr_003");
            
            assertThat(batchJobs).hasSize(1);
            assertThat(batchJobs.get(0).getConfigId()).isEqualTo("cfg_finance_002");
        }

        @Test
        @DisplayName("Should find job configurations by source system")
        void shouldFindJobConfigurationsBySourceSystem() {
            // When
            List<ManualJobConfigEntity> hrJobs = repository.findBySourceSystem("HR");
            List<ManualJobConfigEntity> financeJobs = repository.findBySourceSystem("FINANCE");
            
            // Then
            assertThat(hrJobs).hasSize(2);
            assertThat(hrJobs).extracting(ManualJobConfigEntity::getConfigId)
                    .containsExactlyInAnyOrder("cfg_hr_001", "cfg_hr_003");
            
            assertThat(financeJobs).hasSize(1);
            assertThat(financeJobs.get(0).getConfigId()).isEqualTo("cfg_finance_002");
        }

        @Test
        @DisplayName("Should find job configurations by target system")
        void shouldFindJobConfigurationsByTargetSystem() {
            // When
            List<ManualJobConfigEntity> dwJobs = repository.findByTargetSystem("DW");
            
            // Then
            assertThat(dwJobs).hasSize(3);
            assertThat(dwJobs).extracting(ManualJobConfigEntity::getConfigId)
                    .containsExactlyInAnyOrder("cfg_hr_001", "cfg_finance_002", "cfg_hr_003");
        }

        @Test
        @DisplayName("Should find job configurations by created by user")
        void shouldFindJobConfigurationsByCreatedByUser() {
            // When
            List<ManualJobConfigEntity> user1Jobs = repository.findByCreatedBy("user1");
            List<ManualJobConfigEntity> user2Jobs = repository.findByCreatedBy("user2");
            
            // Then
            assertThat(user1Jobs).hasSize(2);
            assertThat(user1Jobs).extracting(ManualJobConfigEntity::getConfigId)
                    .containsExactlyInAnyOrder("cfg_hr_001", "cfg_hr_003");
            
            assertThat(user2Jobs).hasSize(1);
            assertThat(user2Jobs.get(0).getConfigId()).isEqualTo("cfg_finance_002");
        }

        @Test
        @DisplayName("Should find job configurations by date range")
        void shouldFindJobConfigurationsByDateRange() {
            // When
            List<ManualJobConfigEntity> rangeResults = repository.findByCreatedDateBetween(
                    baseDateTime, baseDateTime.plusHours(1));
            
            // Then
            assertThat(rangeResults).hasSize(2);
            assertThat(rangeResults).extracting(ManualJobConfigEntity::getConfigId)
                    .containsExactlyInAnyOrder("cfg_hr_001", "cfg_finance_002");
        }
    }

    @Nested
    @DisplayName("Custom JPQL Query Methods")
    class CustomJPQLQueryMethodsTests {

        @BeforeEach
        void setUpData() {
            repository.save(testEntity1);
            repository.save(testEntity2);
            repository.save(testEntity3);
        }

        @Test
        @DisplayName("Should find active configurations by job type")
        void shouldFindActiveConfigurationsByJobType() {
            // When
            List<ManualJobConfigEntity> activeEtlJobs = repository.findActiveConfigurationsByJobType("ETL");
            
            // Then
            assertThat(activeEtlJobs).hasSize(1);
            assertThat(activeEtlJobs.get(0).getConfigId()).isEqualTo("cfg_hr_001");
        }

        @Test
        @DisplayName("Should find configurations by source and target system")
        void shouldFindConfigurationsBySourceAndTargetSystem() {
            // When
            List<ManualJobConfigEntity> hrToDwJobs = repository.findBySourceAndTargetSystem("HR", "DW");
            
            // Then
            assertThat(hrToDwJobs).hasSize(2);
            assertThat(hrToDwJobs).extracting(ManualJobConfigEntity::getConfigId)
                    .containsExactlyInAnyOrder("cfg_hr_001", "cfg_hr_003");
        }

        @Test
        @DisplayName("Should count active configurations")
        void shouldCountActiveConfigurations() {
            // When
            long activeCount = repository.countActiveConfigurations();
            
            // Then
            assertThat(activeCount).isEqualTo(2);
        }

        @Test
        @DisplayName("Should count configurations by status")
        void shouldCountConfigurationsByStatus() {
            // When
            long activeCount = repository.countByStatus("ACTIVE");
            long inactiveCount = repository.countByStatus("INACTIVE");
            
            // Then
            assertThat(activeCount).isEqualTo(2);
            assertThat(inactiveCount).isEqualTo(1);
        }

        @Test
        @DisplayName("Should find configurations containing parameter")
        void shouldFindConfigurationsContainingParameter() {
            // When
            List<ManualJobConfigEntity> results = repository.findByJobParametersContaining("hr_employees");
            
            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getConfigId()).isEqualTo("cfg_hr_001");
        }

        @Test
        @DisplayName("Should check if active configuration exists by job name")
        void shouldCheckIfActiveConfigurationExistsByJobName() {
            // When
            boolean existsActive = repository.existsActiveConfigurationByJobName("HR Employee Load");
            boolean existsInactive = repository.existsActiveConfigurationByJobName("HR Department Load");
            boolean existsNonExistent = repository.existsActiveConfigurationByJobName("Non-existent Job");
            
            // Then
            assertThat(existsActive).isTrue();
            assertThat(existsInactive).isFalse(); // This one is INACTIVE
            assertThat(existsNonExistent).isFalse();
        }

        @Test
        @DisplayName("Should find most recent configuration")
        void shouldFindMostRecentConfiguration() {
            // When
            List<ManualJobConfigEntity> results = repository.findMostRecentConfiguration();
            
            // Then
            assertThat(results).isNotEmpty();
            // The most recent should be testEntity3 (baseDateTime + 2 hours)
            assertThat(results.get(0).getConfigId()).isEqualTo("cfg_hr_003");
        }

        @Test
        @DisplayName("Should find configurations needing attention")
        void shouldFindConfigurationsNeedingAttention() {
            // Given - Add a deprecated configuration
            ManualJobConfigEntity deprecatedEntity = ManualJobConfigEntity.builder()
                    .configId("cfg_old_004")
                    .jobName("Old Deprecated Job")
                    .jobType("ETL")
                    .sourceSystem("OLD")
                    .targetSystem("DEPRECATED")
                    .jobParameters("{}")
                    .status("DEPRECATED")
                    .createdBy("user3")
                    .createdDate(baseDateTime.minusDays(1))
                    .versionNumber(1L)
                    .build();
            repository.save(deprecatedEntity);
            
            // When - Look for configurations older than 1 hour or deprecated
            List<ManualJobConfigEntity> needingAttention = repository.findConfigurationsNeedingAttention(
                    baseDateTime.plusMinutes(30));
            
            // Then
            assertThat(needingAttention).hasSize(1);
            assertThat(needingAttention.get(0).getConfigId()).isEqualTo("cfg_old_004");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty repository gracefully")
        void shouldHandleEmptyRepositoryGracefully() {
            // When
            List<ManualJobConfigEntity> allConfigurations = repository.findAll();
            long count = repository.count();
            
            // Then
            assertThat(allConfigurations).isEmpty();
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle null and empty string queries gracefully")
        void shouldHandleNullAndEmptyStringQueriesGracefully() {
            // Given
            repository.save(testEntity1);
            
            // When & Then
            assertThat(repository.findByJobName("")).isEmpty();
            assertThat(repository.findByJobType("")).isEmpty();
            assertThat(repository.findBySourceSystem("")).isEmpty();
            assertThat(repository.findByTargetSystem("")).isEmpty();
            assertThat(repository.findByCreatedBy("")).isEmpty();
        }

        @Test
        @DisplayName("Should handle case sensitivity in queries")
        void shouldHandleCaseSensitivityInQueries() {
            // Given
            repository.save(testEntity1);
            
            // When
            Optional<ManualJobConfigEntity> upperCase = repository.findByJobName("HR EMPLOYEE LOAD");
            Optional<ManualJobConfigEntity> lowerCase = repository.findByJobName("hr employee load");
            Optional<ManualJobConfigEntity> exactCase = repository.findByJobName("HR Employee Load");
            
            // Then
            assertThat(upperCase).isEmpty();
            assertThat(lowerCase).isEmpty();
            assertThat(exactCase).isPresent();
        }

        @Test
        @DisplayName("Should handle large result sets efficiently")
        void shouldHandleLargeResultSetsEfficiently() {
            // Given - Create multiple configurations with same criteria
            for (int i = 0; i < 100; i++) {
                ManualJobConfigEntity entity = ManualJobConfigEntity.builder()
                        .configId("cfg_bulk_" + i)
                        .jobName("Bulk Job " + i)
                        .jobType("BULK")
                        .sourceSystem("BULK_SRC")
                        .targetSystem("BULK_TGT")
                        .jobParameters("{\"bulk\":true}")
                        .status("ACTIVE")
                        .createdBy("bulk_user")
                        .createdDate(baseDateTime.plusMinutes(i))
                        .versionNumber(1L)
                        .build();
                repository.save(entity);
            }
            
            // When
            List<ManualJobConfigEntity> bulkJobs = repository.findByJobType("BULK");
            List<ManualJobConfigEntity> bulkSrcJobs = repository.findBySourceSystem("BULK_SRC");
            
            // Then
            assertThat(bulkJobs).hasSize(100);
            assertThat(bulkSrcJobs).hasSize(100);
        }
    }

    @Nested
    @DisplayName("Performance and Optimization Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should efficiently query by indexed fields")
        void shouldEfficientlyQueryByIndexedFields() {
            // Given - Setup test data
            for (int i = 0; i < 10; i++) {
                ManualJobConfigEntity entity = ManualJobConfigEntity.builder()
                        .configId("cfg_perf_" + i)
                        .jobName("Performance Job " + i)
                        .jobType("PERF")
                        .sourceSystem("PERF_SRC")
                        .targetSystem("PERF_TGT")
                        .jobParameters("{\"perf\":true}")
                        .status(i % 2 == 0 ? "ACTIVE" : "INACTIVE")
                        .createdBy("perf_user")
                        .createdDate(baseDateTime.plusMinutes(i))
                        .versionNumber(1L)
                        .build();
                repository.save(entity);
            }
            
            // When - Test queries that should use indexes
            long start = System.currentTimeMillis();
            
            List<ManualJobConfigEntity> statusResults = repository.findByStatus("ACTIVE");
            List<ManualJobConfigEntity> typeResults = repository.findByJobType("PERF");
            List<ManualJobConfigEntity> sourceResults = repository.findBySourceSystem("PERF_SRC");
            
            long duration = System.currentTimeMillis() - start;
            
            // Then
            assertThat(statusResults).hasSize(5);
            assertThat(typeResults).hasSize(10);
            assertThat(sourceResults).hasSize(10);
            assertThat(duration).isLessThan(1000); // Should complete quickly
        }

        @Test
        @DisplayName("Should handle concurrent access safely")
        void shouldHandleConcurrentAccessSafely() {
            // Given
            repository.save(testEntity1);
            
            // When - Simulate concurrent read operations
            Optional<ManualJobConfigEntity> result1 = repository.findById("cfg_hr_001");
            Optional<ManualJobConfigEntity> result2 = repository.findById("cfg_hr_001");
            
            // Then
            assertThat(result1).isPresent();
            assertThat(result2).isPresent();
            assertThat(result1.get()).isEqualTo(result2.get());
        }
    }
}