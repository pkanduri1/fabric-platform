package com.fabric.batch.repository;

import com.fabric.batch.entity.FieldTemplateEntity;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TransformationConfig CRUD operations on FIELD_TEMPLATES.
 * <p>
 * All DB access uses JdbcTemplate — no JPA, no Spring Data.
 * The FIELD_TEMPLATES table stores field-level transformation rules for batch templates.
 */
public interface TransformationConfigRepository {

    /**
     * Return all enabled field templates ordered by field name.
     */
    List<FieldTemplateEntity> findAll();

    /**
     * Find a single field template by its synthetic ID.
     * Returns empty if no matching row is found.
     */
    Optional<FieldTemplateEntity> findById(String id);

    /**
     * Return all enabled field templates for the given file type (source system),
     * ordered by target position.
     */
    List<FieldTemplateEntity> findBySourceSystem(String fileType);

    /**
     * Insert a new field template record. Sets ENABLED = 'Y' and CREATED_DATE
     * on the entity before returning it.
     */
    FieldTemplateEntity save(FieldTemplateEntity entity);

    /**
     * Update all mutable fields of an existing field template record.
     */
    FieldTemplateEntity update(FieldTemplateEntity entity);

    /**
     * Soft-delete by setting ENABLED = 'N' and MODIFIED_DATE = now.
     */
    void softDelete(String id);
}
