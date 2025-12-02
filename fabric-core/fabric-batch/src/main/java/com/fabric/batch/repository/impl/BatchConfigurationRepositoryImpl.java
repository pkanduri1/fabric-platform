package com.fabric.batch.repository.impl;

import com.fabric.batch.entity.BatchConfigurationEntity;
import com.fabric.batch.repository.BatchConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * JdbcTemplate implementation of BatchConfigurationRepository.
 * Reads from CM3INT.BATCH_CONFIGURATIONS table.
 *
 * @author Claude Code
 * @since Phase 2 - Batch Module Separation
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BatchConfigurationRepositoryImpl implements BatchConfigurationRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String BASE_SELECT =
        "SELECT ID, SOURCE_SYSTEM, JOB_NAME, TRANSACTION_TYPE, DESCRIPTION, " +
        "CONFIGURATION_JSON, CREATED_BY, CREATED_DATE, MODIFIED_BY, MODIFIED_DATE, " +
        "VERSION, ENABLED " +
        "FROM CM3INT.BATCH_CONFIGURATIONS";

    private final RowMapper<BatchConfigurationEntity> rowMapper = this::mapRow;

    @Override
    public Optional<BatchConfigurationEntity> findById(String id) {
        String sql = BASE_SELECT + " WHERE ID = ?";

        try {
            BatchConfigurationEntity entity = jdbcTemplate.queryForObject(sql, rowMapper, id);
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            log.debug("Configuration not found for ID: {}", id);
            return Optional.empty();
        }
    }

    @Override
    public Optional<BatchConfigurationEntity> findBySourceSystemAndJobName(String sourceSystem, String jobName) {
        String sql = BASE_SELECT + " WHERE SOURCE_SYSTEM = ? AND JOB_NAME = ? AND ENABLED = 'Y'";

        try {
            BatchConfigurationEntity entity = jdbcTemplate.queryForObject(sql, rowMapper, sourceSystem, jobName);
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            log.debug("Configuration not found for source: {}, job: {}", sourceSystem, jobName);
            return Optional.empty();
        }
    }

    @Override
    public List<BatchConfigurationEntity> findAllEnabled() {
        String sql = BASE_SELECT + " WHERE ENABLED = 'Y' ORDER BY SOURCE_SYSTEM, JOB_NAME";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public List<BatchConfigurationEntity> findBySourceSystem(String sourceSystem) {
        String sql = BASE_SELECT + " WHERE SOURCE_SYSTEM = ? AND ENABLED = 'Y' ORDER BY JOB_NAME";
        return jdbcTemplate.query(sql, rowMapper, sourceSystem);
    }

    /**
     * Row mapper for BatchConfigurationEntity
     */
    private BatchConfigurationEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        BatchConfigurationEntity entity = new BatchConfigurationEntity();

        entity.setId(rs.getString("ID"));
        entity.setSourceSystem(rs.getString("SOURCE_SYSTEM"));
        entity.setJobName(rs.getString("JOB_NAME"));
        entity.setTransactionType(rs.getString("TRANSACTION_TYPE"));
        entity.setDescription(rs.getString("DESCRIPTION"));
        entity.setConfigurationJson(rs.getString("CONFIGURATION_JSON"));
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
        entity.setEnabled(rs.getString("ENABLED"));

        return entity;
    }
}
