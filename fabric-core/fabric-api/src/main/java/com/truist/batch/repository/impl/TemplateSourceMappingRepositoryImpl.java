package com.truist.batch.repository.impl;

import com.truist.batch.entity.TemplateSourceMappingEntity;
import com.truist.batch.repository.TemplateSourceMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JdbcTemplate implementation of TemplateSourceMappingRepository
 * Replaces JPA-based repository to eliminate JPA dependencies
 * 
 * Maps to CM3INT.TEMPLATE_SOURCE_MAPPINGS table
 */
@Repository
public class TemplateSourceMappingRepositoryImpl implements TemplateSourceMappingRepository {

    private static final Logger logger = LoggerFactory.getLogger(TemplateSourceMappingRepositoryImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<TemplateSourceMappingEntity> rowMapper = new TemplateSourceMappingRowMapper();

    @Override
    public List<TemplateSourceMappingEntity> findByFileTypeAndTransactionTypeAndSourceSystemIdAndEnabledOrderByTargetPosition(
            String fileType, String transactionType, String sourceSystemId, String enabled) {
        String sql = """
            SELECT * FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS 
            WHERE FILE_TYPE = ? AND TRANSACTION_TYPE = ? AND SOURCE_SYSTEM_ID = ? AND ENABLED = ? 
            ORDER BY TARGET_POSITION
        """;
        return jdbcTemplate.query(sql, rowMapper, fileType, transactionType, sourceSystemId, enabled);
    }

    @Override
    public List<TemplateSourceMappingEntity> findByFileTypeAndTransactionTypeAndEnabledOrderBySourceSystemIdAscTargetPositionAsc(
            String fileType, String transactionType, String enabled) {
        String sql = """
            SELECT * FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS 
            WHERE FILE_TYPE = ? AND TRANSACTION_TYPE = ? AND ENABLED = ? 
            ORDER BY SOURCE_SYSTEM_ID ASC, TARGET_POSITION ASC
        """;
        return jdbcTemplate.query(sql, rowMapper, fileType, transactionType, enabled);
    }

    @Override
    public List<TemplateSourceMappingEntity> findBySourceSystemIdAndEnabledOrderByFileTypeAscTransactionTypeAscTargetPositionAsc(
            String sourceSystemId, String enabled) {
        String sql = """
            SELECT * FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS 
            WHERE SOURCE_SYSTEM_ID = ? AND ENABLED = ? 
            ORDER BY FILE_TYPE ASC, TRANSACTION_TYPE ASC, TARGET_POSITION ASC
        """;
        return jdbcTemplate.query(sql, rowMapper, sourceSystemId, enabled);
    }

    @Override
    public List<TemplateSourceMappingEntity> findByJobNameAndEnabledOrderByTargetPosition(String jobName, String enabled) {
        String sql = """
            SELECT * FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS 
            WHERE JOB_NAME = ? AND ENABLED = ? 
            ORDER BY TARGET_POSITION
        """;
        return jdbcTemplate.query(sql, rowMapper, jobName, enabled);
    }

    @Override
    public long countByFileTypeAndTransactionTypeAndSourceSystemIdAndEnabled(
            String fileType, String transactionType, String sourceSystemId, String enabled) {
        String sql = """
            SELECT COUNT(*) FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS 
            WHERE FILE_TYPE = ? AND TRANSACTION_TYPE = ? AND SOURCE_SYSTEM_ID = ? AND ENABLED = ?
        """;
        return jdbcTemplate.queryForObject(sql, Long.class, fileType, transactionType, sourceSystemId, enabled);
    }

    @Override
    public boolean existsByFileTypeAndTransactionTypeAndSourceSystemIdAndEnabled(
            String fileType, String transactionType, String sourceSystemId, String enabled) {
        long count = countByFileTypeAndTransactionTypeAndSourceSystemIdAndEnabled(
                fileType, transactionType, sourceSystemId, enabled);
        return count > 0;
    }

    @Override
    public void deleteByFileTypeAndTransactionTypeAndSourceSystemIdAndJobName(
            String fileType, String transactionType, String sourceSystemId, String jobName) {
        String sql = """
            DELETE FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS 
            WHERE FILE_TYPE = ? AND TRANSACTION_TYPE = ? AND SOURCE_SYSTEM_ID = ? AND JOB_NAME = ?
        """;
        int deletedRows = jdbcTemplate.update(sql, fileType, transactionType, sourceSystemId, jobName);
        logger.info("Deleted {} template source mappings for {}/{}/{}/{}", 
                deletedRows, fileType, transactionType, sourceSystemId, jobName);
    }

    @Override
    public void disableByFileTypeAndTransactionTypeAndSourceSystemId(
            String fileType, String transactionType, String sourceSystemId) {
        String sql = """
            UPDATE CM3INT.TEMPLATE_SOURCE_MAPPINGS 
            SET ENABLED = 'N', MODIFIED_DATE = CURRENT_TIMESTAMP 
            WHERE FILE_TYPE = ? AND TRANSACTION_TYPE = ? AND SOURCE_SYSTEM_ID = ? AND ENABLED = 'Y'
        """;
        int updatedRows = jdbcTemplate.update(sql, fileType, transactionType, sourceSystemId);
        logger.info("Disabled {} template source mappings for {}/{}/{}", 
                updatedRows, fileType, transactionType, sourceSystemId);
    }

    @Override
    public List<String> findDistinctSourceSystemIdsByFileTypeAndTransactionTypeAndEnabled(
            String fileType, String transactionType, String enabled) {
        String sql = """
            SELECT DISTINCT SOURCE_SYSTEM_ID FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS 
            WHERE FILE_TYPE = ? AND TRANSACTION_TYPE = ? AND ENABLED = ? 
            ORDER BY SOURCE_SYSTEM_ID
        """;
        return jdbcTemplate.queryForList(sql, String.class, fileType, transactionType, enabled);
    }

    @Override
    public List<String> findDistinctTemplatesBySourceSystemIdAndEnabled(String sourceSystemId, String enabled) {
        String sql = """
            SELECT DISTINCT CONCAT(FILE_TYPE, '/', TRANSACTION_TYPE) FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS 
            WHERE SOURCE_SYSTEM_ID = ? AND ENABLED = ? 
            ORDER BY FILE_TYPE, TRANSACTION_TYPE
        """;
        return jdbcTemplate.queryForList(sql, String.class, sourceSystemId, enabled);
    }

    @Override
    public List<TemplateSourceMappingEntity> findMappingsWithMissingSourceFields(
            String fileType, String transactionType, String sourceSystemId, String enabled) {
        String sql = """
            SELECT * FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS 
            WHERE FILE_TYPE = ? AND TRANSACTION_TYPE = ? AND SOURCE_SYSTEM_ID = ? AND ENABLED = ? 
            AND (SOURCE_FIELD_NAME IS NULL OR SOURCE_FIELD_NAME = '') 
            ORDER BY TARGET_POSITION
        """;
        return jdbcTemplate.query(sql, rowMapper, fileType, transactionType, sourceSystemId, enabled);
    }

    // Standard CRUD operations (replacing JpaRepository methods)

    @Override
    public <S extends TemplateSourceMappingEntity> S save(S entity) {
        if (entity.getId() != null && existsById(entity.getId())) {
            return update(entity);
        } else {
            return insert(entity);
        }
    }

    @Override
    public <S extends TemplateSourceMappingEntity> Iterable<S> saveAll(Iterable<S> entities) {
        for (S entity : entities) {
            save(entity);
        }
        return entities;
    }

    @Override
    public Optional<TemplateSourceMappingEntity> findById(Long id) {
        String sql = "SELECT * FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS WHERE ID = ?";
        try {
            TemplateSourceMappingEntity entity = jdbcTemplate.queryForObject(sql, rowMapper, id);
            return Optional.ofNullable(entity);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS WHERE ID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public List<TemplateSourceMappingEntity> findAll() {
        String sql = "SELECT * FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public Iterable<TemplateSourceMappingEntity> findAllById(Iterable<Long> ids) {
        // Convert iterable to list for SQL IN clause
        StringBuilder sql = new StringBuilder("SELECT * FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS WHERE ID IN (");
        StringBuilder placeholders = new StringBuilder();
        Object[] params = new Object[((List<Long>) ids).size()];
        int i = 0;
        for (Long id : ids) {
            if (i > 0) {
                placeholders.append(", ");
            }
            placeholders.append("?");
            params[i] = id;
            i++;
        }
        sql.append(placeholders).append(")");
        return jdbcTemplate.query(sql.toString(), rowMapper, params);
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public void delete(TemplateSourceMappingEntity entity) {
        deleteById(entity.getId());
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        for (Long id : ids) {
            deleteById(id);
        }
    }

    @Override
    public void deleteAll(Iterable<? extends TemplateSourceMappingEntity> entities) {
        for (TemplateSourceMappingEntity entity : entities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS";
        jdbcTemplate.update(sql);
    }

    // Helper methods for save operation
    private <S extends TemplateSourceMappingEntity> S insert(S entity) {
        String sql = """
            INSERT INTO CM3INT.TEMPLATE_SOURCE_MAPPINGS 
            (FILE_TYPE, TRANSACTION_TYPE, SOURCE_SYSTEM_ID, JOB_NAME, TARGET_FIELD_NAME, 
             SOURCE_FIELD_NAME, TRANSFORMATION_TYPE, TRANSFORMATION_CONFIG, TARGET_POSITION, 
             LENGTH, DATA_TYPE, CREATED_BY, CREATED_DATE, VERSION, ENABLED)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        if (entity.getCreatedDate() == null) {
            entity.setCreatedDate(LocalDateTime.now());
        }
        if (entity.getVersion() == null) {
            entity.setVersion(1);
        }
        if (entity.getEnabled() == null) {
            entity.setEnabled("Y");
        }
        if (entity.getTransformationType() == null) {
            entity.setTransformationType("source");
        }

        // Insert without using generated keys to avoid Oracle ROWID casting issues
        int rowsAffected = jdbcTemplate.update(sql,
            entity.getFileType(),
            entity.getTransactionType(),
            entity.getSourceSystemId(),
            entity.getJobName(),
            entity.getTargetFieldName(),
            entity.getSourceFieldName(),
            entity.getTransformationType(),
            entity.getTransformationConfig(),
            entity.getTargetPosition(),
            entity.getLength(),
            entity.getDataType(),
            entity.getCreatedBy(),
            entity.getCreatedDate(),
            entity.getVersion(),
            entity.getEnabled()
        );

        // Query back the inserted record to get the generated ID
        if (rowsAffected > 0) {
            String selectIdSql = """
                SELECT ID FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS 
                WHERE FILE_TYPE = ? AND TRANSACTION_TYPE = ? AND SOURCE_SYSTEM_ID = ? 
                AND JOB_NAME = ? AND TARGET_FIELD_NAME = ? AND CREATED_DATE = ?
                ORDER BY ID DESC FETCH FIRST 1 ROWS ONLY
            """;
            try {
                Long id = jdbcTemplate.queryForObject(selectIdSql, Long.class,
                    entity.getFileType(), entity.getTransactionType(), entity.getSourceSystemId(),
                    entity.getJobName(), entity.getTargetFieldName(), entity.getCreatedDate());
                entity.setId(id);
                logger.info("Inserted template source mapping with ID: {} (rows affected: {})", entity.getId(), rowsAffected);
            } catch (Exception ex) {
                logger.error("Failed to retrieve generated ID after insert: {}", ex.getMessage());
                // Set a placeholder ID to avoid null issues
                entity.setId(-1L);
            }
        }

        return entity;
    }

    private <S extends TemplateSourceMappingEntity> S update(S entity) {
        String sql = """
            UPDATE CM3INT.TEMPLATE_SOURCE_MAPPINGS 
            SET FILE_TYPE = ?, TRANSACTION_TYPE = ?, SOURCE_SYSTEM_ID = ?, JOB_NAME = ?, 
                TARGET_FIELD_NAME = ?, SOURCE_FIELD_NAME = ?, TRANSFORMATION_TYPE = ?, 
                TRANSFORMATION_CONFIG = ?, TARGET_POSITION = ?, LENGTH = ?, DATA_TYPE = ?, 
                MODIFIED_BY = ?, MODIFIED_DATE = ?, VERSION = ?, ENABLED = ?
            WHERE ID = ?
        """;

        entity.setModifiedDate(LocalDateTime.now());

        jdbcTemplate.update(sql,
                entity.getFileType(),
                entity.getTransactionType(),
                entity.getSourceSystemId(),
                entity.getJobName(),
                entity.getTargetFieldName(),
                entity.getSourceFieldName(),
                entity.getTransformationType(),
                entity.getTransformationConfig(),
                entity.getTargetPosition(),
                entity.getLength(),
                entity.getDataType(),
                entity.getModifiedBy(),
                entity.getModifiedDate(),
                entity.getVersion(),
                entity.getEnabled(),
                entity.getId()
        );

        logger.info("Updated template source mapping: {}", entity.getId());
        return entity;
    }

    /**
     * RowMapper for TemplateSourceMappingEntity
     */
    private static class TemplateSourceMappingRowMapper implements RowMapper<TemplateSourceMappingEntity> {
        @Override
        public TemplateSourceMappingEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            TemplateSourceMappingEntity entity = new TemplateSourceMappingEntity();
            entity.setId(rs.getLong("ID"));
            entity.setFileType(rs.getString("FILE_TYPE"));
            entity.setTransactionType(rs.getString("TRANSACTION_TYPE"));
            entity.setSourceSystemId(rs.getString("SOURCE_SYSTEM_ID"));
            entity.setJobName(rs.getString("JOB_NAME"));
            entity.setTargetFieldName(rs.getString("TARGET_FIELD_NAME"));
            entity.setSourceFieldName(rs.getString("SOURCE_FIELD_NAME"));
            entity.setTransformationType(rs.getString("TRANSFORMATION_TYPE"));
            entity.setTransformationConfig(rs.getString("TRANSFORMATION_CONFIG"));
            
            entity.setTargetPosition(rs.getInt("TARGET_POSITION"));
            if (rs.wasNull()) entity.setTargetPosition(null);
            
            entity.setLength(rs.getInt("LENGTH"));
            if (rs.wasNull()) entity.setLength(null);
            
            entity.setDataType(rs.getString("DATA_TYPE"));
            entity.setCreatedBy(rs.getString("CREATED_BY"));
            
            if (rs.getTimestamp("CREATED_DATE") != null) {
                entity.setCreatedDate(rs.getTimestamp("CREATED_DATE").toLocalDateTime());
            }
            
            entity.setModifiedBy(rs.getString("MODIFIED_BY"));
            
            if (rs.getTimestamp("MODIFIED_DATE") != null) {
                entity.setModifiedDate(rs.getTimestamp("MODIFIED_DATE").toLocalDateTime());
            }
            
            entity.setVersion(rs.getInt("VERSION"));
            if (rs.wasNull()) entity.setVersion(1);
            
            entity.setEnabled(rs.getString("ENABLED"));
            
            return entity;
        }
    }
}