package com.truist.batch.repository;

import com.truist.batch.entity.OutputTemplateDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Epic 3: Output Template Definition Repository
 * 
 * JPA repository for managing template definitions with versioning support
 * and advanced query capabilities for template management.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 3
 */
@Repository
public interface OutputTemplateDefinitionRepository extends JpaRepository<OutputTemplateDefinitionEntity, Long> {

    /**
     * Find template by name and active flag
     */
    Optional<OutputTemplateDefinitionEntity> findByTemplateNameAndActiveFlag(String templateName, String activeFlag);

    /**
     * Find templates by type and format
     */
    List<OutputTemplateDefinitionEntity> findByTemplateTypeAndOutputFormatAndActiveFlag(
            OutputTemplateDefinitionEntity.TemplateType templateType,
            OutputTemplateDefinitionEntity.OutputFormat outputFormat,
            String activeFlag);

    /**
     * Find latest version of template by name
     */
    @Query("""
        SELECT otd FROM OutputTemplateDefinitionEntity otd 
        WHERE otd.templateName = :templateName 
        AND otd.activeFlag = 'Y'
        ORDER BY otd.versionNumber DESC
        LIMIT 1
        """)
    Optional<OutputTemplateDefinitionEntity> findLatestVersionByName(@Param("templateName") String templateName);

    /**
     * Find templates by business owner
     */
    List<OutputTemplateDefinitionEntity> findByBusinessOwnerAndActiveFlagOrderByCreatedDateDesc(
            String businessOwner, String activeFlag);
}