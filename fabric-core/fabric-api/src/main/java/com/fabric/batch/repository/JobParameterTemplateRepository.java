package com.fabric.batch.repository;

import com.fabric.batch.entity.JobParameterTemplateEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Job Parameter Template management.
 * Uses JdbcTemplate implementation for direct database access.
 * 
 * Provides comprehensive data access operations for parameter templates with
 * enterprise-grade features including inheritance, search, analytics, and
 * compliance support for banking applications.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 2.0
 * @since US001 Phase 2 - Parameter Template Management
 */
public interface JobParameterTemplateRepository {

    // =========================================================================
    // BASIC CRUD OPERATIONS
    // =========================================================================

    /**
     * Find template by ID.
     * 
     * @param templateId the template ID
     * @return optional template if found
     */
    Optional<JobParameterTemplateEntity> findById(String templateId);

    /**
     * Find all templates.
     * 
     * @return list of all templates
     */
    List<JobParameterTemplateEntity> findAll();

    /**
     * Save template (insert or update).
     * 
     * @param entity the template entity
     * @return saved template
     */
    JobParameterTemplateEntity save(JobParameterTemplateEntity entity);

    /**
     * Delete template by ID.
     * 
     * @param templateId the template ID
     */
    void deleteById(String templateId);

    /**
     * Check if template exists by ID.
     * 
     * @param templateId the template ID
     * @return true if exists
     */
    boolean existsById(String templateId);

    /**
     * Count all templates.
     * 
     * @return total count
     */
    long count();

    // =========================================================================
    // BASIC TEMPLATE QUERIES
    // =========================================================================

    /**
     * Find template by name with exact match.
     * 
     * @param templateName the template name to search for
     * @return optional template if found
     */
    Optional<JobParameterTemplateEntity> findByTemplateName(String templateName);

    /**
     * Check if template name exists for uniqueness validation.
     * 
     * @param templateName the template name to check
     * @return true if template name exists
     */
    boolean existsByTemplateName(String templateName);

    /**
     * Check if template name exists excluding deprecated templates.
     * 
     * @param templateName the template name to check
     * @param isDeprecated deprecated flag ('N' for non-deprecated)
     * @return true if active template name exists
     */
    boolean existsByTemplateNameAndIsDeprecated(String templateName, Character isDeprecated);

    // =========================================================================
    // JOB TYPE AND CATEGORY QUERIES
    // =========================================================================

    /**
     * Find all templates for a specific job type ordered by usage count.
     * 
     * @param jobType the job type to filter by
     * @return list of templates ordered by usage (most used first)
     */
    List<JobParameterTemplateEntity> findByJobTypeOrderByUsageCountDesc(String jobType);

    /**
     * Find active templates for a specific job type.
     * 
     * @param jobType the job type to filter by
     * @param isDeprecated deprecated flag ('N' for non-deprecated)
     * @return list of active templates ordered by usage
     */
    List<JobParameterTemplateEntity> findByJobTypeAndIsDeprecatedOrderByUsageCountDesc(
            String jobType, Character isDeprecated);

    /**
     * Find templates by job type and category with deprecation filter.
     * 
     * @param jobType the job type to filter by
     * @param category the category to filter by
     * @param isDeprecated deprecated flag ('N' for non-deprecated)
     * @return list of matching templates ordered by usage
     */
    List<JobParameterTemplateEntity> findByJobTypeAndCategoryAndIsDeprecatedOrderByUsageCountDesc(
            String jobType, String category, Character isDeprecated);

    /**
     * Find templates by category.
     * 
     * @param category the category to filter by
     * @return list of templates in the specified category
     */
    List<JobParameterTemplateEntity> findByCategory(String category);

    // =========================================================================
    // STATUS AND SYSTEM TEMPLATE QUERIES
    // =========================================================================

    /**
     * Find templates by status.
     * 
     * @param status the template status to filter by
     * @return list of templates with specified status
     */
    List<JobParameterTemplateEntity> findByStatus(String status);

    /**
     * Find system templates for administrative operations.
     * 
     * @param isSystemTemplate system template flag ('Y' for system templates)
     * @return list of system-managed templates
     */
    List<JobParameterTemplateEntity> findByIsSystemTemplate(Character isSystemTemplate);

    /**
     * Find all active templates excluding deprecated ones.
     * 
     * @param status template status ('ACTIVE')
     * @param isDeprecated deprecated flag ('N' for non-deprecated)
     * @return list of active, non-deprecated templates
     */
    List<JobParameterTemplateEntity> findByStatusAndIsDeprecated(String status, Character isDeprecated);

    // =========================================================================
    // TEMPLATE HIERARCHY QUERIES
    // =========================================================================

    /**
     * Find child templates for hierarchy navigation.
     * 
     * @param parentTemplateId the parent template ID
     * @return list of child templates
     */
    List<JobParameterTemplateEntity> findByParentTemplateId(String parentTemplateId);

    /**
     * Find templates that extend a specific template.
     * 
     * @param extendsTemplateId the extended template ID
     * @return list of templates extending the specified template
     */
    List<JobParameterTemplateEntity> findByExtendsTemplateId(String extendsTemplateId);

    /**
     * Check if template has dependent children.
     * 
     * @param templateId the template ID to check
     * @return true if template has child dependencies
     */
    boolean existsByParentTemplateIdOrExtendsTemplateId(String templateId);

    // =========================================================================
    // ADVANCED SEARCH QUERIES
    // =========================================================================

    /**
     * Search templates by name pattern with case-insensitive matching.
     * 
     * @param namePattern the name pattern to search for (with wildcards)
     * @return list of templates matching the name pattern
     */
    List<JobParameterTemplateEntity> findByTemplateNameContainingIgnoreCase(String namePattern);

    /**
     * Full-text search across name and description fields.
     * 
     * @param searchText the text to search for
     * @return list of templates matching the search criteria
     */
    List<JobParameterTemplateEntity> searchByText(String searchText);

    /**
     * Advanced multi-criteria search with all filters.
     * 
     * @param jobType optional job type filter
     * @param category optional category filter
     * @param status optional status filter
     * @param searchText optional text search
     * @return list of templates matching all specified criteria
     */
    List<JobParameterTemplateEntity> findByMultipleCriteria(
            String jobType, String category, String status, String searchText);

    // =========================================================================
    // USAGE ANALYTICS QUERIES
    // =========================================================================

    /**
     * Find most used templates for analytics and reporting.
     * 
     * @param limit maximum number of templates to return
     * @return list of most used templates
     */
    List<JobParameterTemplateEntity> findMostUsedTemplates(int limit);

    /**
     * Find recently used templates for cache optimization.
     * 
     * @param since timestamp to filter recent usage
     * @return list of recently used templates
     */
    List<JobParameterTemplateEntity> findRecentlyUsed(LocalDateTime since);

    /**
     * Find unused templates for cleanup and optimization.
     * 
     * @param maxAge timestamp before which templates are considered unused
     * @return list of unused templates
     */
    List<JobParameterTemplateEntity> findUnusedTemplates(LocalDateTime maxAge);

    // =========================================================================
    // BULK OPERATIONS AND UPDATES
    // =========================================================================

    /**
     * Update template usage statistics atomically.
     * 
     * @param templateId the template ID to update
     * @param usageCount the new usage count
     * @param lastUsedDate the last used timestamp
     * @return number of rows updated
     */
    int updateUsageStatistics(String templateId, Long usageCount, LocalDateTime lastUsedDate);

    /**
     * Bulk deprecate templates by job type.
     * 
     * @param jobType the job type to deprecate
     * @param deprecatedBy user performing the deprecation
     * @param deprecationDate deprecation timestamp
     * @return number of templates deprecated
     */
    int deprecateTemplatesByJobType(String jobType, String deprecatedBy, LocalDateTime deprecationDate);

    /**
     * Bulk activate templates by category.
     * 
     * @param category the category to activate
     * @param activatedBy user performing the activation
     * @param activationDate activation timestamp
     * @return number of templates activated
     */
    int activateTemplatesByCategory(String category, String activatedBy, LocalDateTime activationDate);

    // =========================================================================
    // STATISTICAL QUERIES FOR REPORTING
    // =========================================================================

    /**
     * Count templates by status for dashboard reporting.
     * 
     * @param status the status to count
     * @return count of templates with specified status
     */
    long countByStatus(String status);

    /**
     * Count system templates for administrative reporting.
     * 
     * @param isSystemTemplate system template flag
     * @return count of system templates
     */
    long countByIsSystemTemplate(Character isSystemTemplate);

    /**
     * Count deprecated templates for cleanup planning.
     * 
     * @param isDeprecated deprecated flag
     * @return count of deprecated templates
     */
    long countByIsDeprecated(Character isDeprecated);

    /**
     * Get total usage count across all templates for analytics.
     * 
     * @return sum of all template usage counts
     */
    long getTotalUsageCount();

    /**
     * Count templates by job type for distribution analysis.
     * 
     * @param jobType the job type to count
     * @return count of templates for specified job type
     */
    long countByJobType(String jobType);
}