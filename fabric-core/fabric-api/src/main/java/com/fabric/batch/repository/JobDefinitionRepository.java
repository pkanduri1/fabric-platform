package com.fabric.batch.repository;

import com.fabric.batch.entity.JobDefinitionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JdbcTemplate-based repository for job definitions
 */
@Repository
public class JobDefinitionRepository {

    private static final Logger logger = LoggerFactory.getLogger(JobDefinitionRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<JobDefinitionEntity> rowMapper = new RowMapper<JobDefinitionEntity>() {
        @Override
        public JobDefinitionEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            JobDefinitionEntity entity = new JobDefinitionEntity();
            entity.setId(rs.getString("ID"));
            entity.setSourceSystemId(rs.getString("SOURCE_SYSTEM_ID"));
            entity.setJobName(rs.getString("JOB_NAME"));
            entity.setDescription(rs.getString("DESCRIPTION"));
            entity.setInputPath(rs.getString("INPUT_PATH"));
            entity.setOutputPath(rs.getString("OUTPUT_PATH"));
            entity.setQuerySql(rs.getString("QUERY_SQL"));
            entity.setEnabled(rs.getString("ENABLED"));
            
            java.sql.Timestamp createdTimestamp = rs.getTimestamp("CREATED_DATE");
            if (createdTimestamp != null) {
                entity.setCreatedDate(createdTimestamp.toLocalDateTime());
            }
            
            entity.setTransactionTypes(rs.getString("TRANSACTION_TYPES"));
            return entity;
        }
    };

    /**
     * Find all job definitions for a source system
     */
    public List<JobDefinitionEntity> findBySourceSystemId(String sourceSystemId) {
        try {
            String sql = """
                SELECT ID, SOURCE_SYSTEM_ID, JOB_NAME, DESCRIPTION, INPUT_PATH, OUTPUT_PATH, 
                       QUERY_SQL, ENABLED, CREATED_DATE, TRANSACTION_TYPES
                FROM CM3INT.JOB_DEFINITIONS 
                WHERE SOURCE_SYSTEM_ID = ? 
                ORDER BY JOB_NAME
            """;
            return jdbcTemplate.query(sql, rowMapper, sourceSystemId);
        } catch (Exception e) {
            logger.error("Error finding job definitions for source system: {}", sourceSystemId, e);
            return List.of();
        }
    }

    /**
     * Find a specific job definition
     */
    public Optional<JobDefinitionEntity> findBySourceSystemIdAndJobName(String sourceSystemId, String jobName) {
        try {
            String sql = """
                SELECT ID, SOURCE_SYSTEM_ID, JOB_NAME, DESCRIPTION, INPUT_PATH, OUTPUT_PATH, 
                       QUERY_SQL, ENABLED, CREATED_DATE, TRANSACTION_TYPES
                FROM CM3INT.JOB_DEFINITIONS 
                WHERE SOURCE_SYSTEM_ID = ? AND JOB_NAME = ?
            """;
            List<JobDefinitionEntity> results = jdbcTemplate.query(sql, rowMapper, sourceSystemId, jobName);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            logger.error("Error finding job definition for {}/{}", sourceSystemId, jobName, e);
            return Optional.empty();
        }
    }

    /**
     * Save a job definition
     */
    public JobDefinitionEntity save(JobDefinitionEntity entity) {
        try {
            if (findBySourceSystemIdAndJobName(entity.getSourceSystemId(), entity.getJobName()).isPresent()) {
                // Update existing
                String updateSql = """
                    UPDATE CM3INT.JOB_DEFINITIONS 
                    SET DESCRIPTION = ?, INPUT_PATH = ?, OUTPUT_PATH = ?, QUERY_SQL = ?, 
                        ENABLED = ?, TRANSACTION_TYPES = ?
                    WHERE SOURCE_SYSTEM_ID = ? AND JOB_NAME = ?
                """;
                jdbcTemplate.update(updateSql,
                    entity.getDescription(),
                    entity.getInputPath(),
                    entity.getOutputPath(),
                    entity.getQuerySql(),
                    entity.getEnabled(),
                    entity.getTransactionTypes(),
                    entity.getSourceSystemId(),
                    entity.getJobName()
                );
            } else {
                // Insert new
                if (entity.getId() == null) {
                    entity.setId(entity.getSourceSystemId() + "-" + entity.getJobName());
                }
                if (entity.getCreatedDate() == null) {
                    entity.setCreatedDate(LocalDateTime.now());
                }

                String insertSql = """
                    INSERT INTO CM3INT.JOB_DEFINITIONS 
                    (ID, SOURCE_SYSTEM_ID, JOB_NAME, DESCRIPTION, INPUT_PATH, OUTPUT_PATH, 
                     QUERY_SQL, ENABLED, CREATED_DATE, TRANSACTION_TYPES)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
                jdbcTemplate.update(insertSql,
                    entity.getId(),
                    entity.getSourceSystemId(),
                    entity.getJobName(),
                    entity.getDescription(),
                    entity.getInputPath(),
                    entity.getOutputPath(),
                    entity.getQuerySql(),
                    entity.getEnabled(),
                    entity.getCreatedDate(),
                    entity.getTransactionTypes()
                );
            }

            logger.info("Successfully saved job definition: {}", entity.getId());
            return entity;
        } catch (Exception e) {
            logger.error("Error saving job definition: {}", entity.getId(), e);
            throw new RuntimeException("Failed to save job definition", e);
        }
    }

    /**
     * Delete a job definition
     */
    public void deleteBySourceSystemIdAndJobName(String sourceSystemId, String jobName) {
        try {
            String sql = "DELETE FROM CM3INT.JOB_DEFINITIONS WHERE SOURCE_SYSTEM_ID = ? AND JOB_NAME = ?";
            int rowsAffected = jdbcTemplate.update(sql, sourceSystemId, jobName);
            logger.info("Deleted {} job definition(s) for {}/{}", rowsAffected, sourceSystemId, jobName);
        } catch (Exception e) {
            logger.error("Error deleting job definition for {}/{}", sourceSystemId, jobName, e);
            throw new RuntimeException("Failed to delete job definition", e);
        }
    }

    /**
     * Count jobs for a source system
     */
    public int countBySourceSystemId(String sourceSystemId) {
        try {
            String sql = "SELECT COUNT(*) FROM CM3INT.JOB_DEFINITIONS WHERE SOURCE_SYSTEM_ID = ? AND ENABLED = 'Y'";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, sourceSystemId);
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Error counting job definitions for source system: {}", sourceSystemId, e);
            return 0;
        }
    }
}