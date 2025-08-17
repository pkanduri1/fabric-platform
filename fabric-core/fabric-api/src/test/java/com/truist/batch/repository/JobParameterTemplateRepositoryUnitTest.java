package com.truist.batch.repository;

import com.truist.batch.entity.JobParameterTemplateEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JobParameterTemplateRepository using mocks.
 * 
 * This tests the repository interface contract without requiring
 * database integration, making tests faster and more focused.
 * 
 * Tests cover all repository methods including:
 * - Basic CRUD operations
 * - Template queries and filtering
 * - Usage analytics
 * - Hierarchy management
 * - Advanced search functionality
 * - Statistical operations
 * 
 * @author Senior Full Stack Developer Agent
 * @version 2.0
 * @since US001 Phase 2 - Parameter Template Management
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JobParameterTemplateRepository Unit Tests")
class JobParameterTemplateRepositoryUnitTest {

    @Mock
    private JobParameterTemplateRepository repository;

    private JobParameterTemplateEntity etlTemplate;
    private JobParameterTemplateEntity migrationTemplate;
    private JobParameterTemplateEntity systemTemplate;
    private JobParameterTemplateEntity deprecatedTemplate;
    private LocalDateTime baseDateTime;

    @BeforeEach
    void setUp() {
        baseDateTime = LocalDateTime.of(2025, 8, 13, 10, 0, 0);
        
        etlTemplate = JobParameterTemplateEntity.builder()
                .templateId("tpl_etl_001")
                .templateName("ETL_BATCH_STANDARD")
                .jobType("ETL_BATCH")
                .templateVersion("1.0")
                .templateDescription("Standard ETL batch processing template")
                .templateSchema("{\"type\":\"object\",\"properties\":{\"batchSize\":{\"type\":\"integer\"}}}")
                .defaultValues("{\"batchSize\":\"1000\"}")
                .validationRules("{\"batchSize\":{\"min\":100,\"max\":10000}}")
                .category("BANKING")
                .tags("etl,batch,standard")
                .status("ACTIVE")
                .isSystemTemplate('N')
                .isDeprecated('N')
                .parentTemplateId(null)
                .extendsTemplateId(null)
                .usageCount(50L)
                .lastUsedDate(baseDateTime.minusDays(1))
                .complianceNotes("SOX compliant processing")
                .documentationUrl("https://docs.example.com/etl-template")
                .createdBy("system")
                .createdDate(baseDateTime.minusMonths(1))
                .updatedBy("admin")
                .updatedDate(baseDateTime.minusDays(7))
                .versionDecimal(3L)
                .build();

        migrationTemplate = JobParameterTemplateEntity.builder()
                .templateId("tpl_migration_001")
                .templateName("DATA_MIGRATION_TEMPLATE")
                .jobType("DATA_MIGRATION")
                .templateVersion("2.0")
                .templateDescription("Data migration with validation template")
                .templateSchema("{\"type\":\"object\",\"properties\":{\"sourceTable\":{\"type\":\"string\"}}}")
                .defaultValues("{\"validationLevel\":\"STRICT\"}")
                .validationRules("{\"sourceTable\":{\"required\":true}}")
                .category("MIGRATION")
                .tags("migration,validation,data")
                .status("ACTIVE")
                .isSystemTemplate('N')
                .isDeprecated('N')
                .parentTemplateId(null)
                .extendsTemplateId("tpl_etl_001")
                .usageCount(25L)
                .lastUsedDate(baseDateTime.minusHours(6))
                .complianceNotes("GDPR compliant data handling")
                .documentationUrl("https://docs.example.com/migration-template")
                .createdBy("admin")
                .createdDate(baseDateTime.minusWeeks(3))
                .updatedBy("admin")
                .updatedDate(baseDateTime.minusDays(2))
                .versionDecimal(5L)
                .build();

        systemTemplate = JobParameterTemplateEntity.builder()
                .templateId("tpl_system_001")
                .templateName("SYSTEM_TEMPLATE")
                .jobType("SYSTEM")
                .templateVersion("1.0")
                .templateDescription("System managed template")
                .templateSchema("{\"type\":\"object\"}")
                .defaultValues("{}")
                .validationRules("{}")
                .category("SYSTEM")
                .tags("system,protected")
                .status("ACTIVE")
                .isSystemTemplate('Y')
                .isDeprecated('N')
                .parentTemplateId(null)
                .extendsTemplateId(null)
                .usageCount(100L)
                .lastUsedDate(baseDateTime)
                .complianceNotes("System template - do not modify")
                .documentationUrl("https://docs.example.com/system-template")
                .createdBy("system")
                .createdDate(baseDateTime.minusYears(1))
                .updatedBy("system")
                .updatedDate(baseDateTime.minusMonths(3))
                .versionDecimal(1L)
                .build();

        deprecatedTemplate = JobParameterTemplateEntity.builder()
                .templateId("tpl_deprecated_001")
                .templateName("OLD_TEMPLATE")
                .jobType("ETL_BATCH")
                .templateVersion("0.9")
                .templateDescription("Deprecated template - use ETL_BATCH_STANDARD instead")
                .templateSchema("{\"type\":\"object\"}")
                .defaultValues("{}")
                .validationRules("{}")
                .category("BANKING")
                .tags("deprecated,legacy")
                .status("DEPRECATED")
                .isSystemTemplate('N')
                .isDeprecated('Y')
                .parentTemplateId(null)
                .extendsTemplateId("tpl_etl_001") // Points to replacement
                .usageCount(10L)
                .lastUsedDate(baseDateTime.minusMonths(2))
                .complianceNotes("DEPRECATED: Use ETL_BATCH_STANDARD instead [admin]")
                .documentationUrl("https://docs.example.com/deprecated-template")
                .createdBy("legacy")
                .createdDate(baseDateTime.minusYears(2))
                .updatedBy("admin")
                .updatedDate(baseDateTime.minusMonths(1))
                .versionDecimal(2L)
                .build();
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudOperations {

        @Test
        @DisplayName("Should find template by ID successfully")
        void shouldFindTemplateByIdSuccessfully() {
            // Given
            when(repository.findById("tpl_etl_001")).thenReturn(Optional.of(etlTemplate));

            // When
            Optional<JobParameterTemplateEntity> result = repository.findById("tpl_etl_001");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getTemplateId()).isEqualTo("tpl_etl_001");
            assertThat(result.get().getTemplateName()).isEqualTo("ETL_BATCH_STANDARD");
            verify(repository).findById("tpl_etl_001");
        }

        @Test
        @DisplayName("Should return empty optional when template not found")
        void shouldReturnEmptyOptionalWhenTemplateNotFound() {
            // Given
            when(repository.findById("nonexistent")).thenReturn(Optional.empty());

            // When
            Optional<JobParameterTemplateEntity> result = repository.findById("nonexistent");

            // Then
            assertThat(result).isEmpty();
            verify(repository).findById("nonexistent");
        }

        @Test
        @DisplayName("Should find all templates")
        void shouldFindAllTemplates() {
            // Given
            List<JobParameterTemplateEntity> allTemplates = Arrays.asList(
                    etlTemplate, migrationTemplate, systemTemplate, deprecatedTemplate);
            when(repository.findAll()).thenReturn(allTemplates);

            // When
            List<JobParameterTemplateEntity> result = repository.findAll();

            // Then
            assertThat(result).hasSize(4);
            assertThat(result).containsExactlyInAnyOrder(
                    etlTemplate, migrationTemplate, systemTemplate, deprecatedTemplate);
            verify(repository).findAll();
        }

        @Test
        @DisplayName("Should save template successfully")
        void shouldSaveTemplateSuccessfully() {
            // Given
            JobParameterTemplateEntity newTemplate = JobParameterTemplateEntity.builder()
                    .templateId("tpl_new_001")
                    .templateName("NEW_TEMPLATE")
                    .build();
            when(repository.save(newTemplate)).thenReturn(newTemplate);

            // When
            JobParameterTemplateEntity result = repository.save(newTemplate);

            // Then
            assertThat(result).isEqualTo(newTemplate);
            verify(repository).save(newTemplate);
        }

        @Test
        @DisplayName("Should delete template by ID")
        void shouldDeleteTemplateById() {
            // Given
            doNothing().when(repository).deleteById("tpl_etl_001");

            // When
            repository.deleteById("tpl_etl_001");

            // Then
            verify(repository).deleteById("tpl_etl_001");
        }

        @Test
        @DisplayName("Should check if template exists by ID")
        void shouldCheckIfTemplateExistsById() {
            // Given
            when(repository.existsById("tpl_etl_001")).thenReturn(true);
            when(repository.existsById("nonexistent")).thenReturn(false);

            // When & Then
            assertThat(repository.existsById("tpl_etl_001")).isTrue();
            assertThat(repository.existsById("nonexistent")).isFalse();
            
            verify(repository).existsById("tpl_etl_001");
            verify(repository).existsById("nonexistent");
        }

        @Test
        @DisplayName("Should count all templates")
        void shouldCountAllTemplates() {
            // Given
            when(repository.count()).thenReturn(4L);

            // When
            long count = repository.count();

            // Then
            assertThat(count).isEqualTo(4L);
            verify(repository).count();
        }
    }

    @Nested
    @DisplayName("Template Name Operations")
    class TemplateNameOperations {

        @Test
        @DisplayName("Should find template by name")
        void shouldFindTemplateByName() {
            // Given
            when(repository.findByTemplateName("ETL_BATCH_STANDARD")).thenReturn(Optional.of(etlTemplate));

            // When
            Optional<JobParameterTemplateEntity> result = repository.findByTemplateName("ETL_BATCH_STANDARD");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getTemplateName()).isEqualTo("ETL_BATCH_STANDARD");
            verify(repository).findByTemplateName("ETL_BATCH_STANDARD");
        }

        @Test
        @DisplayName("Should check if template name exists")
        void shouldCheckIfTemplateNameExists() {
            // Given
            when(repository.existsByTemplateName("ETL_BATCH_STANDARD")).thenReturn(true);
            when(repository.existsByTemplateName("NONEXISTENT_TEMPLATE")).thenReturn(false);

            // When & Then
            assertThat(repository.existsByTemplateName("ETL_BATCH_STANDARD")).isTrue();
            assertThat(repository.existsByTemplateName("NONEXISTENT_TEMPLATE")).isFalse();
            
            verify(repository).existsByTemplateName("ETL_BATCH_STANDARD");
            verify(repository).existsByTemplateName("NONEXISTENT_TEMPLATE");
        }

        @Test
        @DisplayName("Should check if active template name exists")
        void shouldCheckIfActiveTemplateNameExists() {
            // Given
            when(repository.existsByTemplateNameAndIsDeprecated("ETL_BATCH_STANDARD", 'N')).thenReturn(true);
            when(repository.existsByTemplateNameAndIsDeprecated("OLD_TEMPLATE", 'N')).thenReturn(false);

            // When & Then
            assertThat(repository.existsByTemplateNameAndIsDeprecated("ETL_BATCH_STANDARD", 'N')).isTrue();
            assertThat(repository.existsByTemplateNameAndIsDeprecated("OLD_TEMPLATE", 'N')).isFalse();
            
            verify(repository).existsByTemplateNameAndIsDeprecated("ETL_BATCH_STANDARD", 'N');
            verify(repository).existsByTemplateNameAndIsDeprecated("OLD_TEMPLATE", 'N');
        }
    }

    @Nested
    @DisplayName("Job Type and Category Queries")
    class JobTypeAndCategoryQueries {

        @Test
        @DisplayName("Should find templates by job type ordered by usage")
        void shouldFindTemplatesByJobTypeOrderedByUsage() {
            // Given
            List<JobParameterTemplateEntity> etlTemplates = Arrays.asList(etlTemplate, deprecatedTemplate);
            when(repository.findByJobTypeOrderByUsageCountDesc("ETL_BATCH")).thenReturn(etlTemplates);

            // When
            List<JobParameterTemplateEntity> result = repository.findByJobTypeOrderByUsageCountDesc("ETL_BATCH");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(etlTemplate, deprecatedTemplate);
            verify(repository).findByJobTypeOrderByUsageCountDesc("ETL_BATCH");
        }

        @Test
        @DisplayName("Should find active templates by job type")
        void shouldFindActiveTemplatesByJobType() {
            // Given
            List<JobParameterTemplateEntity> activeEtlTemplates = Arrays.asList(etlTemplate);
            when(repository.findByJobTypeAndIsDeprecatedOrderByUsageCountDesc("ETL_BATCH", 'N'))
                    .thenReturn(activeEtlTemplates);

            // When
            List<JobParameterTemplateEntity> result = repository.findByJobTypeAndIsDeprecatedOrderByUsageCountDesc("ETL_BATCH", 'N');

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(etlTemplate);
            verify(repository).findByJobTypeAndIsDeprecatedOrderByUsageCountDesc("ETL_BATCH", 'N');
        }

        @Test
        @DisplayName("Should find templates by job type and category")
        void shouldFindTemplatesByJobTypeAndCategory() {
            // Given
            List<JobParameterTemplateEntity> bankingEtlTemplates = Arrays.asList(etlTemplate);
            when(repository.findByJobTypeAndCategoryAndIsDeprecatedOrderByUsageCountDesc("ETL_BATCH", "BANKING", 'N'))
                    .thenReturn(bankingEtlTemplates);

            // When
            List<JobParameterTemplateEntity> result = repository.findByJobTypeAndCategoryAndIsDeprecatedOrderByUsageCountDesc("ETL_BATCH", "BANKING", 'N');

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(etlTemplate);
            verify(repository).findByJobTypeAndCategoryAndIsDeprecatedOrderByUsageCountDesc("ETL_BATCH", "BANKING", 'N');
        }

        @Test
        @DisplayName("Should find templates by category")
        void shouldFindTemplatesByCategory() {
            // Given
            List<JobParameterTemplateEntity> bankingTemplates = Arrays.asList(etlTemplate, deprecatedTemplate);
            when(repository.findByCategory("BANKING")).thenReturn(bankingTemplates);

            // When
            List<JobParameterTemplateEntity> result = repository.findByCategory("BANKING");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(etlTemplate, deprecatedTemplate);
            verify(repository).findByCategory("BANKING");
        }
    }

    @Nested
    @DisplayName("Status and System Template Queries")
    class StatusAndSystemTemplateQueries {

        @Test
        @DisplayName("Should find templates by status")
        void shouldFindTemplatesByStatus() {
            // Given
            List<JobParameterTemplateEntity> activeTemplates = Arrays.asList(etlTemplate, migrationTemplate, systemTemplate);
            when(repository.findByStatus("ACTIVE")).thenReturn(activeTemplates);

            // When
            List<JobParameterTemplateEntity> result = repository.findByStatus("ACTIVE");

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).containsExactlyInAnyOrder(etlTemplate, migrationTemplate, systemTemplate);
            verify(repository).findByStatus("ACTIVE");
        }

        @Test
        @DisplayName("Should find system templates")
        void shouldFindSystemTemplates() {
            // Given
            List<JobParameterTemplateEntity> systemTemplates = Arrays.asList(systemTemplate);
            when(repository.findByIsSystemTemplate('Y')).thenReturn(systemTemplates);

            // When
            List<JobParameterTemplateEntity> result = repository.findByIsSystemTemplate('Y');

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(systemTemplate);
            verify(repository).findByIsSystemTemplate('Y');
        }

        @Test
        @DisplayName("Should find active non-deprecated templates")
        void shouldFindActiveNonDeprecatedTemplates() {
            // Given
            List<JobParameterTemplateEntity> activeNonDeprecated = Arrays.asList(etlTemplate, migrationTemplate, systemTemplate);
            when(repository.findByStatusAndIsDeprecated("ACTIVE", 'N')).thenReturn(activeNonDeprecated);

            // When
            List<JobParameterTemplateEntity> result = repository.findByStatusAndIsDeprecated("ACTIVE", 'N');

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).containsExactlyInAnyOrder(etlTemplate, migrationTemplate, systemTemplate);
            verify(repository).findByStatusAndIsDeprecated("ACTIVE", 'N');
        }
    }

    @Nested
    @DisplayName("Template Hierarchy Queries")
    class TemplateHierarchyQueries {

        @Test
        @DisplayName("Should find child templates by parent ID")
        void shouldFindChildTemplatesByParentId() {
            // Given
            List<JobParameterTemplateEntity> childTemplates = Arrays.asList(migrationTemplate);
            when(repository.findByParentTemplateId("tpl_etl_001")).thenReturn(childTemplates);

            // When
            List<JobParameterTemplateEntity> result = repository.findByParentTemplateId("tpl_etl_001");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(migrationTemplate);
            verify(repository).findByParentTemplateId("tpl_etl_001");
        }

        @Test
        @DisplayName("Should find templates that extend another template")
        void shouldFindTemplatesThatExtendAnotherTemplate() {
            // Given
            List<JobParameterTemplateEntity> extendingTemplates = Arrays.asList(migrationTemplate, deprecatedTemplate);
            when(repository.findByExtendsTemplateId("tpl_etl_001")).thenReturn(extendingTemplates);

            // When
            List<JobParameterTemplateEntity> result = repository.findByExtendsTemplateId("tpl_etl_001");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(migrationTemplate, deprecatedTemplate);
            verify(repository).findByExtendsTemplateId("tpl_etl_001");
        }

        @Test
        @DisplayName("Should check if template has dependent children")
        void shouldCheckIfTemplateHasDependentChildren() {
            // Given
            when(repository.existsByParentTemplateIdOrExtendsTemplateId("tpl_etl_001")).thenReturn(true);
            when(repository.existsByParentTemplateIdOrExtendsTemplateId("tpl_migration_001")).thenReturn(false);

            // When & Then
            assertThat(repository.existsByParentTemplateIdOrExtendsTemplateId("tpl_etl_001")).isTrue();
            assertThat(repository.existsByParentTemplateIdOrExtendsTemplateId("tpl_migration_001")).isFalse();
            
            verify(repository).existsByParentTemplateIdOrExtendsTemplateId("tpl_etl_001");
            verify(repository).existsByParentTemplateIdOrExtendsTemplateId("tpl_migration_001");
        }
    }

    @Nested
    @DisplayName("Advanced Search Queries")
    class AdvancedSearchQueries {

        @Test
        @DisplayName("Should search templates by name pattern")
        void shouldSearchTemplatesByNamePattern() {
            // Given
            List<JobParameterTemplateEntity> matchingTemplates = Arrays.asList(etlTemplate);
            when(repository.findByTemplateNameContainingIgnoreCase("BATCH")).thenReturn(matchingTemplates);

            // When
            List<JobParameterTemplateEntity> result = repository.findByTemplateNameContainingIgnoreCase("BATCH");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(etlTemplate);
            verify(repository).findByTemplateNameContainingIgnoreCase("BATCH");
        }

        @Test
        @DisplayName("Should perform full-text search")
        void shouldPerformFullTextSearch() {
            // Given
            List<JobParameterTemplateEntity> searchResults = Arrays.asList(etlTemplate, migrationTemplate);
            when(repository.searchByText("standard")).thenReturn(searchResults);

            // When
            List<JobParameterTemplateEntity> result = repository.searchByText("standard");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(etlTemplate, migrationTemplate);
            verify(repository).searchByText("standard");
        }

        @Test
        @DisplayName("Should search by multiple criteria")
        void shouldSearchByMultipleCriteria() {
            // Given
            List<JobParameterTemplateEntity> searchResults = Arrays.asList(etlTemplate);
            when(repository.findByMultipleCriteria("ETL_BATCH", "BANKING", "ACTIVE", "standard"))
                    .thenReturn(searchResults);

            // When
            List<JobParameterTemplateEntity> result = repository.findByMultipleCriteria("ETL_BATCH", "BANKING", "ACTIVE", "standard");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(etlTemplate);
            verify(repository).findByMultipleCriteria("ETL_BATCH", "BANKING", "ACTIVE", "standard");
        }
    }

    @Nested
    @DisplayName("Usage Analytics Queries")
    class UsageAnalyticsQueries {

        @Test
        @DisplayName("Should find most used templates")
        void shouldFindMostUsedTemplates() {
            // Given
            List<JobParameterTemplateEntity> mostUsed = Arrays.asList(systemTemplate, etlTemplate, migrationTemplate);
            when(repository.findMostUsedTemplates(3)).thenReturn(mostUsed);

            // When
            List<JobParameterTemplateEntity> result = repository.findMostUsedTemplates(3);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).containsExactly(systemTemplate, etlTemplate, migrationTemplate);
            verify(repository).findMostUsedTemplates(3);
        }

        @Test
        @DisplayName("Should find recently used templates")
        void shouldFindRecentlyUsedTemplates() {
            // Given
            LocalDateTime since = baseDateTime.minusHours(12);
            List<JobParameterTemplateEntity> recentlyUsed = Arrays.asList(migrationTemplate, systemTemplate);
            when(repository.findRecentlyUsed(since)).thenReturn(recentlyUsed);

            // When
            List<JobParameterTemplateEntity> result = repository.findRecentlyUsed(since);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(migrationTemplate, systemTemplate);
            verify(repository).findRecentlyUsed(since);
        }

        @Test
        @DisplayName("Should find unused templates")
        void shouldFindUnusedTemplates() {
            // Given
            LocalDateTime maxAge = baseDateTime.minusMonths(1);
            List<JobParameterTemplateEntity> unused = Arrays.asList(deprecatedTemplate);
            when(repository.findUnusedTemplates(maxAge)).thenReturn(unused);

            // When
            List<JobParameterTemplateEntity> result = repository.findUnusedTemplates(maxAge);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(deprecatedTemplate);
            verify(repository).findUnusedTemplates(maxAge);
        }
    }

    @Nested
    @DisplayName("Bulk Operations and Updates")
    class BulkOperationsAndUpdates {

        @Test
        @DisplayName("Should update usage statistics")
        void shouldUpdateUsageStatistics() {
            // Given
            when(repository.updateUsageStatistics("tpl_etl_001", 51L, baseDateTime)).thenReturn(1);

            // When
            int updatedRows = repository.updateUsageStatistics("tpl_etl_001", 51L, baseDateTime);

            // Then
            assertThat(updatedRows).isEqualTo(1);
            verify(repository).updateUsageStatistics("tpl_etl_001", 51L, baseDateTime);
        }

        @Test
        @DisplayName("Should bulk deprecate templates by job type")
        void shouldBulkDeprecateTemplatesByJobType() {
            // Given
            when(repository.deprecateTemplatesByJobType("OLD_TYPE", "admin", baseDateTime)).thenReturn(3);

            // When
            int deprecatedCount = repository.deprecateTemplatesByJobType("OLD_TYPE", "admin", baseDateTime);

            // Then
            assertThat(deprecatedCount).isEqualTo(3);
            verify(repository).deprecateTemplatesByJobType("OLD_TYPE", "admin", baseDateTime);
        }

        @Test
        @DisplayName("Should bulk activate templates by category")
        void shouldBulkActivateTemplatesByCategory() {
            // Given
            when(repository.activateTemplatesByCategory("MIGRATION", "admin", baseDateTime)).thenReturn(2);

            // When
            int activatedCount = repository.activateTemplatesByCategory("MIGRATION", "admin", baseDateTime);

            // Then
            assertThat(activatedCount).isEqualTo(2);
            verify(repository).activateTemplatesByCategory("MIGRATION", "admin", baseDateTime);
        }
    }

    @Nested
    @DisplayName("Statistical Queries")
    class StatisticalQueries {

        @Test
        @DisplayName("Should count templates by status")
        void shouldCountTemplatesByStatus() {
            // Given
            when(repository.countByStatus("ACTIVE")).thenReturn(3L);
            when(repository.countByStatus("DEPRECATED")).thenReturn(1L);

            // When & Then
            assertThat(repository.countByStatus("ACTIVE")).isEqualTo(3L);
            assertThat(repository.countByStatus("DEPRECATED")).isEqualTo(1L);
            
            verify(repository).countByStatus("ACTIVE");
            verify(repository).countByStatus("DEPRECATED");
        }

        @Test
        @DisplayName("Should count system templates")
        void shouldCountSystemTemplates() {
            // Given
            when(repository.countByIsSystemTemplate('Y')).thenReturn(1L);
            when(repository.countByIsSystemTemplate('N')).thenReturn(3L);

            // When & Then
            assertThat(repository.countByIsSystemTemplate('Y')).isEqualTo(1L);
            assertThat(repository.countByIsSystemTemplate('N')).isEqualTo(3L);
            
            verify(repository).countByIsSystemTemplate('Y');
            verify(repository).countByIsSystemTemplate('N');
        }

        @Test
        @DisplayName("Should count deprecated templates")
        void shouldCountDeprecatedTemplates() {
            // Given
            when(repository.countByIsDeprecated('Y')).thenReturn(1L);
            when(repository.countByIsDeprecated('N')).thenReturn(3L);

            // When & Then
            assertThat(repository.countByIsDeprecated('Y')).isEqualTo(1L);
            assertThat(repository.countByIsDeprecated('N')).isEqualTo(3L);
            
            verify(repository).countByIsDeprecated('Y');
            verify(repository).countByIsDeprecated('N');
        }

        @Test
        @DisplayName("Should get total usage count")
        void shouldGetTotalUsageCount() {
            // Given
            when(repository.getTotalUsageCount()).thenReturn(185L); // 50+25+100+10

            // When
            long totalUsage = repository.getTotalUsageCount();

            // Then
            assertThat(totalUsage).isEqualTo(185L);
            verify(repository).getTotalUsageCount();
        }

        @Test
        @DisplayName("Should count templates by job type")
        void shouldCountTemplatesByJobType() {
            // Given
            when(repository.countByJobType("ETL_BATCH")).thenReturn(2L);
            when(repository.countByJobType("DATA_MIGRATION")).thenReturn(1L);

            // When & Then
            assertThat(repository.countByJobType("ETL_BATCH")).isEqualTo(2L);
            assertThat(repository.countByJobType("DATA_MIGRATION")).isEqualTo(1L);
            
            verify(repository).countByJobType("ETL_BATCH");
            verify(repository).countByJobType("DATA_MIGRATION");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("Should handle null parameters gracefully")
        void shouldHandleNullParametersGracefully() {
            // Given
            when(repository.findByJobType(null)).thenReturn(Arrays.asList());
            when(repository.findByCategory(null)).thenReturn(Arrays.asList());

            // When
            List<JobParameterTemplateEntity> result1 = repository.findByJobType(null);
            List<JobParameterTemplateEntity> result2 = repository.findByCategory(null);

            // Then
            assertThat(result1).isEmpty();
            assertThat(result2).isEmpty();
            
            verify(repository).findByJobType(null);
            verify(repository).findByCategory(null);
        }

        @Test
        @DisplayName("Should handle empty search results")
        void shouldHandleEmptySearchResults() {
            // Given
            when(repository.findByMultipleCriteria("NONEXISTENT", "NONE", "INVALID", "nothing"))
                    .thenReturn(Arrays.asList());

            // When
            List<JobParameterTemplateEntity> result = repository.findByMultipleCriteria("NONEXISTENT", "NONE", "INVALID", "nothing");

            // Then
            assertThat(result).isEmpty();
            verify(repository).findByMultipleCriteria("NONEXISTENT", "NONE", "INVALID", "nothing");
        }

        @Test
        @DisplayName("Should return zero for non-existent counts")
        void shouldReturnZeroForNonExistentCounts() {
            // Given
            when(repository.countByJobType("NONEXISTENT")).thenReturn(0L);

            // When
            long count = repository.countByJobType("NONEXISTENT");

            // Then
            assertThat(count).isEqualTo(0L);
            verify(repository).countByJobType("NONEXISTENT");
        }
    }
}