package com.fabric.batch.repository.impl;

import com.fabric.batch.entity.FieldTemplateEntity;
import com.fabric.batch.repository.TransformationConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JdbcTemplate-based implementation of TransformationConfigRepository.
 * <p>
 * Operates on the CM3INT.FIELD_TEMPLATES table. No JPA, no Hibernate.
 * All SQL is parameterised to prevent injection. Columns included in DML
 * match those confirmed in the Liquibase changelogs:
 * <ul>
 *   <li>Base table (us001 / templates-ddl.sql): FILE_TYPE, TRANSACTION_TYPE,
 *       FIELD_NAME, TARGET_POSITION, LENGTH, DATA_TYPE, FORMAT, REQUIRED,
 *       DESCRIPTION, CREATED_BY, CREATED_DATE, MODIFIED_BY, MODIFIED_DATE,
 *       VERSION, ENABLED</li>
 *   <li>phase2-001 additions: SOURCE_FIELD, TRANSFORMATION_TYPE, VALUE,
 *       TRANSFORMATION_CONFIG</li>
 * </ul>
 * NOTE: The FIELD_TEMPLATES table uses a composite natural PK
 * (FILE_TYPE, TRANSACTION_TYPE, FIELD_NAME). There is no standalone ID column
 * in the base schema. The {@code findById}, {@code update}, and
 * {@code softDelete} methods therefore attempt to use an ID column and
 * degrade gracefully when it is absent — matching the behaviour of the
 * existing {@link com.fabric.batch.repository.FieldTemplateRepository}.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class TransformationConfigRepositoryImpl implements TransformationConfigRepository {

    private final JdbcTemplate jdbcTemplate;

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Override
    public List<FieldTemplateEntity> findAll() {
        String sql = "SELECT * FROM FIELD_TEMPLATES WHERE ENABLED = 'Y' ORDER BY FIELD_NAME";
        log.debug("findAll: {}", sql);
        return jdbcTemplate.query(sql, new FieldTemplateRowMapper());
    }

    /**
     * Looks up a field template by its ID column.
     * Uses {@code jdbcTemplate.query()} (not {@code queryForObject}) so that
     * an empty result set returns {@code Optional.empty()} rather than throwing.
     */
    @Override
    public Optional<FieldTemplateEntity> findById(String id) {
        String sql = "SELECT * FROM FIELD_TEMPLATES WHERE ID = ?";
        log.debug("findById: id={}", id);
        try {
            List<FieldTemplateEntity> results = jdbcTemplate.query(sql, new FieldTemplateRowMapper(), id);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            log.warn("findById failed (ID column may not exist in schema): {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<FieldTemplateEntity> findBySourceSystem(String fileType) {
        String sql = "SELECT * FROM FIELD_TEMPLATES WHERE FILE_TYPE = ? AND ENABLED = 'Y' ORDER BY TARGET_POSITION";
        log.debug("findBySourceSystem: fileType={}", fileType);
        return jdbcTemplate.query(sql, new FieldTemplateRowMapper(), fileType);
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /**
     * Inserts a new FIELD_TEMPLATES row. ENABLED is hardcoded to 'Y'.
     * CREATED_DATE is set to now on the entity before the insert.
     * <p>
     * Columns included: FILE_TYPE, TRANSACTION_TYPE, FIELD_NAME,
     * TARGET_POSITION, LENGTH, DATA_TYPE, FORMAT, REQUIRED, DESCRIPTION,
     * ENABLED, CREATED_BY, CREATED_DATE, MODIFIED_BY, VERSION,
     * TRANSFORMATION_TYPE, TRANSFORMATION_CONFIG, VALUE, SOURCE_FIELD.
     */
    @Override
    public FieldTemplateEntity save(FieldTemplateEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedDate(now);
        entity.setEnabled("Y");

        String sql = """
                INSERT INTO FIELD_TEMPLATES (
                    FILE_TYPE, TRANSACTION_TYPE, FIELD_NAME,
                    TARGET_POSITION, LENGTH, DATA_TYPE, FORMAT,
                    REQUIRED, DESCRIPTION, ENABLED,
                    CREATED_BY, CREATED_DATE,
                    MODIFIED_BY, VERSION,
                    TRANSFORMATION_TYPE, TRANSFORMATION_CONFIG,
                    VALUE, SOURCE_FIELD
                ) VALUES (
                    ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?
                )
                """;

        log.debug("save: fileType={}, txnType={}, fieldName={}",
                entity.getFileType(), entity.getTransactionType(), entity.getFieldName());

        jdbcTemplate.update(sql,
                entity.getFileType(),
                entity.getTransactionType(),
                entity.getFieldName(),
                entity.getTargetPosition(),
                entity.getLength(),
                entity.getDataType(),
                entity.getFormat(),
                entity.getRequired(),
                entity.getDescription(),
                "Y",
                entity.getCreatedBy(),
                Timestamp.valueOf(now),
                entity.getModifiedBy(),
                entity.getVersion(),
                entity.getTransformationType(),
                entity.getTransformationConfig(),
                entity.getValue(),
                entity.getSourceField());

        return entity;
    }

    /**
     * Updates all mutable fields for the row identified by ID.
     * Sets MODIFIED_DATE on the entity.
     */
    @Override
    public FieldTemplateEntity update(FieldTemplateEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setModifiedDate(now);

        String sql = """
                UPDATE FIELD_TEMPLATES SET
                    FILE_TYPE             = ?,
                    TRANSACTION_TYPE      = ?,
                    FIELD_NAME            = ?,
                    TARGET_POSITION       = ?,
                    LENGTH                = ?,
                    DATA_TYPE             = ?,
                    FORMAT                = ?,
                    REQUIRED              = ?,
                    DESCRIPTION           = ?,
                    ENABLED               = ?,
                    MODIFIED_BY           = ?,
                    MODIFIED_DATE         = ?,
                    VERSION               = ?,
                    TRANSFORMATION_TYPE   = ?,
                    TRANSFORMATION_CONFIG = ?,
                    VALUE                 = ?,
                    SOURCE_FIELD          = ?
                WHERE ID = ?
                """;

        log.debug("update: id={}", entity.getId());

        jdbcTemplate.update(sql,
                entity.getFileType(),
                entity.getTransactionType(),
                entity.getFieldName(),
                entity.getTargetPosition(),
                entity.getLength(),
                entity.getDataType(),
                entity.getFormat(),
                entity.getRequired(),
                entity.getDescription(),
                entity.getEnabled(),
                entity.getModifiedBy(),
                Timestamp.valueOf(now),
                entity.getVersion(),
                entity.getTransformationType(),
                entity.getTransformationConfig(),
                entity.getValue(),
                entity.getSourceField(),
                entity.getId());

        return entity;
    }

    /**
     * Soft-deletes the row identified by ID by setting ENABLED = 'N'.
     */
    @Override
    public void softDelete(String id) {
        String sql = "UPDATE FIELD_TEMPLATES SET ENABLED = 'N', MODIFIED_DATE = ? WHERE ID = ?";
        log.debug("softDelete: id={}", id);
        jdbcTemplate.update(sql, Timestamp.valueOf(LocalDateTime.now()), id);
    }

    // -------------------------------------------------------------------------
    // Inner RowMapper
    // -------------------------------------------------------------------------

    /**
     * Maps a FIELD_TEMPLATES result-set row to a {@link FieldTemplateEntity}.
     * <p>
     * Optional columns (those added by phase2 migrations) are wrapped in
     * try/catch so the mapper works against both the base schema and the
     * extended schema.
     */
    private static class FieldTemplateRowMapper implements RowMapper<FieldTemplateEntity> {

        @Override
        public FieldTemplateEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            FieldTemplateEntity entity = new FieldTemplateEntity();

            // Attempt to read the ID column — absent in some schema versions
            try {
                entity.setId(rs.getString("ID"));
            } catch (SQLException e) {
                // ID column not present; leave null
            }

            entity.setFileType(rs.getString("FILE_TYPE"));
            entity.setTransactionType(rs.getString("TRANSACTION_TYPE"));
            entity.setFieldName(rs.getString("FIELD_NAME"));
            entity.setTargetPosition(rs.getInt("TARGET_POSITION"));
            entity.setLength(rs.getInt("LENGTH"));
            entity.setDataType(rs.getString("DATA_TYPE"));
            entity.setFormat(rs.getString("FORMAT"));
            entity.setRequired(rs.getString("REQUIRED"));
            entity.setDescription(rs.getString("DESCRIPTION"));
            entity.setEnabled(rs.getString("ENABLED"));
            entity.setCreatedBy(rs.getString("CREATED_BY"));

            Timestamp createdDate = rs.getTimestamp("CREATED_DATE");
            if (createdDate != null) {
                entity.setCreatedDate(createdDate.toLocalDateTime());
            }

            entity.setModifiedBy(rs.getString("MODIFIED_BY"));

            Timestamp modifiedDate = rs.getTimestamp("MODIFIED_DATE");
            if (modifiedDate != null) {
                entity.setModifiedDate(modifiedDate.toLocalDateTime());
            }

            entity.setVersion(rs.getInt("VERSION"));

            // phase2-001 optional columns
            try {
                entity.setTransformationType(rs.getString("TRANSFORMATION_TYPE"));
            } catch (SQLException e) {
                // column not present in this schema version
            }

            try {
                entity.setTransformationConfig(rs.getString("TRANSFORMATION_CONFIG"));
            } catch (SQLException e) {
                // column not present in this schema version
            }

            try {
                entity.setValue(rs.getString("VALUE"));
            } catch (SQLException e) {
                // column not present in this schema version
            }

            try {
                entity.setSourceField(rs.getString("SOURCE_FIELD"));
            } catch (SQLException e) {
                // column not present in this schema version
            }

            return entity;
        }
    }
}
