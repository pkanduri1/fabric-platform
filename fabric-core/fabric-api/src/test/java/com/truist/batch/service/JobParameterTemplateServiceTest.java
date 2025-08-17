package com.truist.batch.service;

import com.truist.batch.entity.JobParameterTemplateEntity;
import com.truist.batch.repository.JobParameterTemplateRepository;
import com.truist.batch.service.JobParameterTemplateService.JobParameterTemplate;
import com.truist.batch.service.JobParameterTemplateService.TemplateCreateRequest;
import com.truist.batch.service.JobParameterTemplateService.TemplateUpdateRequest;
import com.truist.batch.service.JobParameterTemplateService.TemplateDeprecationResult;
import com.truist.batch.service.JobParameterTemplateService.TemplateValidationResult;
import com.truist.batch.service.JobParameterTemplateService.TemplateUsageStatistics;
import com.truist.batch.service.JobParameterTemplateService.TemplateSearchCriteria;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for JobParameterTemplateService with repository integration.
 * 
 * Tests enterprise-grade template management functionality including:
 * - Template creation, update, and deprecation workflows
 * - Template validation and schema enforcement
 * - Usage analytics and performance monitoring
 * - Template search and filtering capabilities
 * - Template hierarchy and inheritance management
 * 
 * Banking-grade testing standards:
 * - 95%+ code coverage requirement
 * - Comprehensive error scenario testing
 * - Security validation and audit trail testing
 * - Performance and resource usage validation
 * 
 * @author Senior Full Stack Developer Agent
 * @version 2.0
 * @since US001 Phase 2 - Parameter Template Management
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JobParameterTemplateService Unit Tests")
class JobParameterTemplateServiceTest {

    @Mock
    private JobParameterTemplateRepository templateRepository;

    @InjectMocks
    private JobParameterTemplateService templateService;

    private TemplateCreateRequest validCreateRequest;
    private JobParameterTemplateEntity validTemplateEntity;
    private LocalDateTime baseDateTime;

    @BeforeEach
    void setUp() {
        baseDateTime = LocalDateTime.of(2025, 8, 13, 10, 0, 0);
        
        validCreateRequest = TemplateCreateRequest.builder()
                .templateName("ETL_BATCH_STANDARD")
                .jobType("ETL_BATCH")
                .templateVersion("1.0")
                .templateDescription("Standard ETL batch processing template")
                .templateSchema(createValidSchema())
                .defaultValues(createValidDefaults())
                .validationRules(createValidRules())
                .category("BANKING")
                .tags(Arrays.asList("etl", "batch", "standard"))
                .build();

        validTemplateEntity = JobParameterTemplateEntity.builder()
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
                .usageCount(50L)
                .lastUsedDate(baseDateTime.minusDays(1))
                .createdBy("admin")
                .createdDate(baseDateTime.minusMonths(1))
                .updatedBy("admin")
                .updatedDate(baseDateTime.minusDays(7))
                .versionDecimal(3L)
                .build();
    }

    @Nested
    @DisplayName("Template Creation")
    class TemplateCreation {

        @Test
        @DisplayName("Should create template successfully with valid request")
        void createTemplate_WithValidRequest_ShouldReturnCreatedTemplate() {
            // Given
            String createdBy = "admin.user";
            
            when(templateRepository.existsByTemplateNameAndIsDeprecated(
                    validCreateRequest.getTemplateName(), 'N')).thenReturn(false);
            when(templateRepository.save(any(JobParameterTemplateEntity.class)))
                    .thenReturn(validTemplateEntity);

            // When
            JobParameterTemplate result = templateService.createTemplate(validCreateRequest, createdBy);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTemplateName()).isEqualTo("ETL_BATCH_STANDARD");
            assertThat(result.getJobType()).isEqualTo("ETL_BATCH");
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            assertThat(result.getIsSystemTemplate()).isFalse();
            assertThat(result.getIsDeprecated()).isFalse();
            
            verify(templateRepository).existsByTemplateNameAndIsDeprecated(
                    validCreateRequest.getTemplateName(), 'N');
            verify(templateRepository).save(any(JobParameterTemplateEntity.class));
        }

        @Test
        @DisplayName("Should throw exception when template name already exists")
        void createTemplate_WithExistingName_ShouldThrowException() {
            // Given
            String createdBy = "admin.user";
            
            when(templateRepository.existsByTemplateNameAndIsDeprecated(
                    validCreateRequest.getTemplateName(), 'N')).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> templateService.createTemplate(validCreateRequest, createdBy))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Template with name")
                    .hasMessageContaining("already exists");
            
            verify(templateRepository).existsByTemplateNameAndIsDeprecated(
                    validCreateRequest.getTemplateName(), 'N');
            verify(templateRepository, never()).save(any(JobParameterTemplateEntity.class));
        }

        @Test
        @DisplayName("Should throw exception when template name is null")
        void createTemplate_WithNullName_ShouldThrowException() {
            // Given
            String createdBy = "admin.user";
            validCreateRequest.setTemplateName(null);

            // When & Then
            assertThatThrownBy(() -> templateService.createTemplate(validCreateRequest, createdBy))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Template name is required");
            
            verify(templateRepository, never()).save(any(JobParameterTemplateEntity.class));
        }

        @Test
        @DisplayName("Should throw exception when job type is null")
        void createTemplate_WithNullJobType_ShouldThrowException() {
            // Given
            String createdBy = "admin.user";
            validCreateRequest.setJobType(null);

            // When & Then
            assertThatThrownBy(() -> templateService.createTemplate(validCreateRequest, createdBy))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Job type is required");
            
            verify(templateRepository, never()).save(any(JobParameterTemplateEntity.class));
        }

        @Test
        @DisplayName("Should throw exception when template schema is null")
        void createTemplate_WithNullSchema_ShouldThrowException() {
            // Given
            String createdBy = "admin.user";
            validCreateRequest.setTemplateSchema(null);

            // When & Then
            assertThatThrownBy(() -> templateService.createTemplate(validCreateRequest, createdBy))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Template schema is required");
            
            verify(templateRepository, never()).save(any(JobParameterTemplateEntity.class));
        }
    }

    @Nested
    @DisplayName("Template Retrieval")
    class TemplateRetrieval {

        @Test
        @DisplayName("Should get template by ID and update usage statistics")
        void getTemplate_WithValidId_ShouldReturnTemplateAndUpdateUsage() {
            // Given
            String templateId = "tpl_etl_001";
            
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(validTemplateEntity));
            when(templateRepository.save(any(JobParameterTemplateEntity.class)))
                    .thenReturn(validTemplateEntity);

            // When
            Optional<JobParameterTemplate> result = templateService.getTemplate(templateId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getTemplateId()).isEqualTo(templateId);
            assertThat(result.get().getTemplateName()).isEqualTo("ETL_BATCH_STANDARD");
            
            verify(templateRepository).findById(templateId);
            verify(templateRepository).save(any(JobParameterTemplateEntity.class));
        }

        @Test
        @DisplayName("Should return empty optional when template not found")
        void getTemplate_WithInvalidId_ShouldReturnEmpty() {
            // Given
            String templateId = "nonexistent";
            
            when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

            // When
            Optional<JobParameterTemplate> result = templateService.getTemplate(templateId);

            // Then
            assertThat(result).isEmpty();
            
            verify(templateRepository).findById(templateId);
            verify(templateRepository, never()).save(any(JobParameterTemplateEntity.class));
        }

        @Test
        @DisplayName("Should get templates by job type with proper filtering")
        void getTemplatesByJobType_WithValidCriteria_ShouldReturnFilteredTemplates() {
            // Given
            String jobType = "ETL_BATCH";
            String category = "BANKING";
            boolean includeDeprecated = false;
            
            List<JobParameterTemplateEntity> entities = Arrays.asList(validTemplateEntity);
            when(templateRepository.findByJobTypeAndCategoryAndIsDeprecatedOrderByUsageCountDesc(
                    jobType, category, 'N')).thenReturn(entities);

            // When
            List<JobParameterTemplate> result = templateService.getTemplatesByJobType(jobType, category, includeDeprecated);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getJobType()).isEqualTo(jobType);
            assertThat(result.get(0).getCategory()).isEqualTo(category);
            
            verify(templateRepository).findByJobTypeAndCategoryAndIsDeprecatedOrderByUsageCountDesc(
                    jobType, category, 'N');
        }

        @Test
        @DisplayName("Should get templates by job type without category filter")
        void getTemplatesByJobType_WithoutCategory_ShouldReturnAllJobTypeTemplates() {
            // Given
            String jobType = "ETL_BATCH";
            boolean includeDeprecated = false;
            
            List<JobParameterTemplateEntity> entities = Arrays.asList(validTemplateEntity);
            when(templateRepository.findByJobTypeAndIsDeprecatedOrderByUsageCountDesc(
                    jobType, 'N')).thenReturn(entities);

            // When
            List<JobParameterTemplate> result = templateService.getTemplatesByJobType(jobType, null, includeDeprecated);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getJobType()).isEqualTo(jobType);
            
            verify(templateRepository).findByJobTypeAndIsDeprecatedOrderByUsageCountDesc(
                    jobType, 'N');
        }
    }

    @Nested
    @DisplayName("Template Updates")
    class TemplateUpdates {

        @Test
        @DisplayName("Should update template successfully with valid request")
        void updateTemplate_WithValidRequest_ShouldReturnUpdatedTemplate() {
            // Given
            String templateId = "tpl_etl_001";
            String updatedBy = "admin.user";
            
            TemplateUpdateRequest updateRequest = TemplateUpdateRequest.builder()
                    .templateDescription("Updated description")
                    .category("FINANCE")
                    .tags(Arrays.asList("etl", "finance", "updated"))
                    .build();
            
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(validTemplateEntity));
            when(templateRepository.save(any(JobParameterTemplateEntity.class)))
                    .thenReturn(validTemplateEntity);

            // When
            JobParameterTemplate result = templateService.updateTemplate(templateId, updateRequest, updatedBy);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTemplateId()).isEqualTo(templateId);
            
            verify(templateRepository).findById(templateId);
            verify(templateRepository).save(any(JobParameterTemplateEntity.class));
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent template")
        void updateTemplate_WithInvalidId_ShouldThrowException() {
            // Given
            String templateId = "nonexistent";
            String updatedBy = "admin.user";
            
            TemplateUpdateRequest updateRequest = TemplateUpdateRequest.builder()
                    .templateDescription("Updated description")
                    .build();
            
            when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> templateService.updateTemplate(templateId, updateRequest, updatedBy))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Template not found");
            
            verify(templateRepository).findById(templateId);
            verify(templateRepository, never()).save(any(JobParameterTemplateEntity.class));
        }
    }

    @Nested
    @DisplayName("Template Deprecation")
    class TemplateDeprecation {

        @Test
        @DisplayName("Should deprecate template successfully")
        void deprecateTemplate_WithValidRequest_ShouldReturnDeprecationResult() {
            // Given
            String templateId = "tpl_etl_001";
            String replacementTemplateId = "tpl_etl_002";
            String deprecatedBy = "admin.user";
            String reason = "Replaced by newer version";
            
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(validTemplateEntity));
            when(templateRepository.save(any(JobParameterTemplateEntity.class)))
                    .thenReturn(validTemplateEntity);

            // When
            TemplateDeprecationResult result = templateService.deprecateTemplate(
                    templateId, replacementTemplateId, deprecatedBy, reason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTemplateId()).isEqualTo(templateId);
            assertThat(result.isDeprecated()).isTrue();
            assertThat(result.getDeprecatedBy()).isEqualTo(deprecatedBy);
            assertThat(result.getReason()).isEqualTo(reason);
            assertThat(result.getReplacementTemplateId()).isEqualTo(replacementTemplateId);
            
            verify(templateRepository).findById(templateId);
            verify(templateRepository).save(any(JobParameterTemplateEntity.class));
        }

        @Test
        @DisplayName("Should return error result when template not found")
        void deprecateTemplate_WithInvalidId_ShouldReturnErrorResult() {
            // Given
            String templateId = "nonexistent";
            String deprecatedBy = "admin.user";
            String reason = "Test deprecation";
            
            when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

            // When
            TemplateDeprecationResult result = templateService.deprecateTemplate(
                    templateId, null, deprecatedBy, reason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTemplateId()).isEqualTo(templateId);
            assertThat(result.isDeprecated()).isFalse();
            assertThat(result.getErrorMessage()).contains("Template not found");
            
            verify(templateRepository).findById(templateId);
            verify(templateRepository, never()).save(any(JobParameterTemplateEntity.class));
        }
    }

    @Nested
    @DisplayName("Template Validation")
    class TemplateValidation {

        @Test
        @DisplayName("Should validate parameters successfully with valid template and parameters")
        void validateParameters_WithValidData_ShouldReturnValidResult() {
            // Given
            String templateId = "tpl_etl_001";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("batchSize", 1000);
            
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(validTemplateEntity));
            when(templateRepository.save(any(JobParameterTemplateEntity.class)))
                    .thenReturn(validTemplateEntity);

            // When
            TemplateValidationResult result = templateService.validateParameters(templateId, parameters);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTemplateId()).isEqualTo(templateId);
            assertThat(result.isValid()).isTrue();
            assertThat(result.getValidationErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should return invalid result when template not found")
        void validateParameters_WithInvalidTemplate_ShouldReturnInvalidResult() {
            // Given
            String templateId = "nonexistent";
            Map<String, Object> parameters = new HashMap<>();
            
            when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

            // When
            TemplateValidationResult result = templateService.validateParameters(templateId, parameters);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTemplateId()).isEqualTo(templateId);
            assertThat(result.isValid()).isFalse();
            assertThat(result.getValidationError()).contains("Template not found");
        }
    }

    @Nested
    @DisplayName("Template Defaults Application")
    class TemplateDefaultsApplication {

        @Test
        @DisplayName("Should apply template defaults to parameters")
        void applyTemplateDefaults_WithValidTemplate_ShouldReturnEnhancedParameters() {
            // Given
            String templateId = "tpl_etl_001";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("customParam", "customValue");
            
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(validTemplateEntity));
            when(templateRepository.save(any(JobParameterTemplateEntity.class)))
                    .thenReturn(validTemplateEntity);

            // When
            Map<String, Object> result = templateService.applyTemplateDefaults(templateId, parameters);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).containsKey("customParam");
            assertThat(result.get("customParam")).isEqualTo("customValue");
            assertThat(result).containsKey("batchSize"); // From defaults
        }

        @Test
        @DisplayName("Should return original parameters when template not found")
        void applyTemplateDefaults_WithInvalidTemplate_ShouldReturnOriginalParameters() {
            // Given
            String templateId = "nonexistent";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("customParam", "customValue");
            
            when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

            // When
            Map<String, Object> result = templateService.applyTemplateDefaults(templateId, parameters);

            // Then
            assertThat(result).isEqualTo(parameters);
        }
    }

    @Nested
    @DisplayName("Usage Statistics")
    class UsageStatistics {

        @Test
        @DisplayName("Should get comprehensive usage statistics")
        void getUsageStatistics_ShouldReturnComprehensiveStats() {
            // Given
            List<JobParameterTemplateEntity> activeTemplates = Arrays.asList(validTemplateEntity);
            List<JobParameterTemplateEntity> mostUsedTemplates = Arrays.asList(validTemplateEntity);
            
            when(templateRepository.count()).thenReturn(10L);
            when(templateRepository.findByStatusAndIsDeprecated("ACTIVE", 'N')).thenReturn(activeTemplates);
            when(templateRepository.countByIsDeprecated('Y')).thenReturn(2L);
            when(templateRepository.countByIsSystemTemplate('Y')).thenReturn(3L);
            when(templateRepository.findMostUsedTemplates(10)).thenReturn(mostUsedTemplates);

            // When
            TemplateUsageStatistics result = templateService.getUsageStatistics();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalTemplates()).isEqualTo(10L);
            assertThat(result.getActiveTemplates()).isEqualTo(1L);
            assertThat(result.getDeprecatedTemplates()).isEqualTo(2L);
            assertThat(result.getSystemTemplates()).isEqualTo(3L);
            assertThat(result.getMostUsedTemplates()).hasSize(1);
            assertThat(result.getMostUsedTemplates().get(0)).isEqualTo("ETL_BATCH_STANDARD");
        }
    }

    @Nested
    @DisplayName("Template Search")
    class TemplateSearch {

        @Test
        @DisplayName("Should search templates with text criteria")
        void searchTemplates_WithTextCriteria_ShouldReturnMatchingTemplates() {
            // Given
            TemplateSearchCriteria searchCriteria = TemplateSearchCriteria.builder()
                    .searchText("batch")
                    .jobType("ETL_BATCH")
                    .category("BANKING")
                    .status("ACTIVE")
                    .limit(10)
                    .build();
            
            List<JobParameterTemplateEntity> searchResults = Arrays.asList(validTemplateEntity);
            when(templateRepository.findByMultipleCriteria("ETL_BATCH", "BANKING", "ACTIVE", "batch"))
                    .thenReturn(searchResults);

            // When
            List<JobParameterTemplate> result = templateService.searchTemplates(searchCriteria);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTemplateName()).isEqualTo("ETL_BATCH_STANDARD");
            
            verify(templateRepository).findByMultipleCriteria("ETL_BATCH", "BANKING", "ACTIVE", "batch");
        }

        @Test
        @DisplayName("Should search templates without text criteria")
        void searchTemplates_WithoutTextCriteria_ShouldReturnMatchingTemplates() {
            // Given
            TemplateSearchCriteria searchCriteria = TemplateSearchCriteria.builder()
                    .jobType("ETL_BATCH")
                    .category("BANKING")
                    .status("ACTIVE")
                    .build();
            
            List<JobParameterTemplateEntity> searchResults = Arrays.asList(validTemplateEntity);
            when(templateRepository.findByMultipleCriteria("ETL_BATCH", "BANKING", "ACTIVE", null))
                    .thenReturn(searchResults);

            // When
            List<JobParameterTemplate> result = templateService.searchTemplates(searchCriteria);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTemplateName()).isEqualTo("ETL_BATCH_STANDARD");
            
            verify(templateRepository).findByMultipleCriteria("ETL_BATCH", "BANKING", "ACTIVE", null);
        }

        @Test
        @DisplayName("Should handle search errors gracefully")
        void searchTemplates_WithException_ShouldReturnEmptyList() {
            // Given
            TemplateSearchCriteria searchCriteria = TemplateSearchCriteria.builder()
                    .searchText("batch")
                    .build();
            
            when(templateRepository.findByMultipleCriteria(null, null, null, "batch"))
                    .thenThrow(new RuntimeException("Database error"));

            // When
            List<JobParameterTemplate> result = templateService.searchTemplates(searchCriteria);

            // Then
            assertThat(result).isEmpty();
        }
    }

    // Helper methods
    private Map<String, Object> createValidSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", "batchSize");
        schema.put("required", "batchSize");
        return schema;
    }

    private Map<String, Object> createValidDefaults() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("batchSize", "1000");
        defaults.put("validationLevel", "STANDARD");
        return defaults;
    }

    private Map<String, Object> createValidRules() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("batchSize", "min:100,max:10000");
        return rules;
    }
}