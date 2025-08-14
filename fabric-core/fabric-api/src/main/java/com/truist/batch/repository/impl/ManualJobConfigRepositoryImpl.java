package com.truist.batch.repository.impl;

import com.truist.batch.entity.ManualJobConfigEntity;
import com.truist.batch.repository.ManualJobConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * JdbcTemplate-based implementation of ManualJobConfigRepository.
 * Provides direct database access without JPA/Hibernate overhead.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ManualJobConfigRepositoryImpl implements ManualJobConfigRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<ManualJobConfigEntity> findById(String id) {
        String sql = "SELECT * FROM MANUAL_JOB_CONFIG WHERE CONFIG_ID = ?";
        try {
            ManualJobConfigEntity entity = jdbcTemplate.queryForObject(sql, new ManualJobConfigRowMapper(), id);
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            log.debug("No manual job config found with id: {}", id);
            return Optional.empty();
        }
    }

    @Override
    public List<ManualJobConfigEntity> findAll() {
        String sql = "SELECT * FROM MANUAL_JOB_CONFIG ORDER BY CREATED_DATE DESC";
        return jdbcTemplate.query(sql, new ManualJobConfigRowMapper());
    }

    @Override
    public ManualJobConfigEntity save(ManualJobConfigEntity entity) {
        if (entity.getConfigId() == null) {
            entity.setConfigId(UUID.randomUUID().toString());
        }
        
        if (existsById(entity.getConfigId())) {
            return update(entity);
        } else {
            return insert(entity);
        }
    }

    private ManualJobConfigEntity insert(ManualJobConfigEntity entity) {
        String sql = """
            INSERT INTO MANUAL_JOB_CONFIG (
                CONFIG_ID, JOB_NAME, JOB_TYPE, SOURCE_SYSTEM, TARGET_SYSTEM,
                JOB_PARAMETERS, STATUS, CREATED_BY, CREATED_DATE, VERSION_DECIMAL
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedDate(now);
        
        jdbcTemplate.update(sql,
            entity.getConfigId(),
            entity.getJobName(),
            entity.getJobType(),
            entity.getSourceSystem(),
            entity.getTargetSystem(),
            entity.getJobParameters(),
            entity.getStatus(),
            entity.getCreatedBy(),
            Timestamp.valueOf(now),
            entity.getVersionNumber()
        );
        
        return entity;
    }

    private ManualJobConfigEntity update(ManualJobConfigEntity entity) {
        String sql = """
            UPDATE MANUAL_JOB_CONFIG SET
                JOB_NAME = ?, JOB_TYPE = ?, SOURCE_SYSTEM = ?, TARGET_SYSTEM = ?,
                JOB_PARAMETERS = ?, STATUS = ?, UPDATED_BY = ?, UPDATED_DATE = ?,
                VERSION_DECIMAL = VERSION_DECIMAL + 1
            WHERE CONFIG_ID = ?
        """;
        
        LocalDateTime now = LocalDateTime.now();
        entity.setUpdatedDate(now);
        
        jdbcTemplate.update(sql,
            entity.getJobName(),
            entity.getJobType(),
            entity.getSourceSystem(),
            entity.getTargetSystem(),
            entity.getJobParameters(),
            entity.getStatus(),
            entity.getUpdatedBy(),
            Timestamp.valueOf(now),
            entity.getConfigId()
        );
        
        // Increment version number for optimistic locking
        entity.setVersionNumber(entity.getVersionNumber() + 1);
        
        return entity;
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM MANUAL_JOB_CONFIG WHERE CONFIG_ID = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT COUNT(*) FROM MANUAL_JOB_CONFIG WHERE CONFIG_ID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public List<ManualJobConfigEntity> findByJobName(String jobName) {
        String sql = "SELECT * FROM MANUAL_JOB_CONFIG WHERE JOB_NAME = ?";
        return jdbcTemplate.query(sql, new ManualJobConfigRowMapper(), jobName);
    }

    @Override
    public List<ManualJobConfigEntity> findByStatus(String status) {
        String sql = "SELECT * FROM MANUAL_JOB_CONFIG WHERE STATUS = ?";
        return jdbcTemplate.query(sql, new ManualJobConfigRowMapper(), status);
    }

    @Override
    public List<ManualJobConfigEntity> findBySourceSystem(String sourceSystem) {
        String sql = "SELECT * FROM MANUAL_JOB_CONFIG WHERE SOURCE_SYSTEM = ?";
        return jdbcTemplate.query(sql, new ManualJobConfigRowMapper(), sourceSystem);
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM MANUAL_JOB_CONFIG";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public List<ManualJobConfigEntity> findByJobType(String jobType) {
        String sql = "SELECT * FROM MANUAL_JOB_CONFIG WHERE JOB_TYPE = ?";
        return jdbcTemplate.query(sql, new ManualJobConfigRowMapper(), jobType);
    }

    @Override
    public List<ManualJobConfigEntity> findByJobTypeAndSourceSystemAndStatus(String jobType, String sourceSystem, String status) {
        String sql = "SELECT * FROM MANUAL_JOB_CONFIG WHERE JOB_TYPE = ? AND SOURCE_SYSTEM = ? AND STATUS = ?";
        return jdbcTemplate.query(sql, new ManualJobConfigRowMapper(), jobType, sourceSystem, status);
    }

    @Override
    public List<ManualJobConfigEntity> findByJobTypeAndStatus(String jobType, String status) {
        String sql = "SELECT * FROM MANUAL_JOB_CONFIG WHERE JOB_TYPE = ? AND STATUS = ?";
        return jdbcTemplate.query(sql, new ManualJobConfigRowMapper(), jobType, status);
    }

    @Override
    public List<ManualJobConfigEntity> findBySourceSystemAndStatus(String sourceSystem, String status) {
        String sql = "SELECT * FROM MANUAL_JOB_CONFIG WHERE SOURCE_SYSTEM = ? AND STATUS = ?";
        return jdbcTemplate.query(sql, new ManualJobConfigRowMapper(), sourceSystem, status);
    }

    @Override
    public boolean existsActiveConfigurationByJobName(String jobName) {
        String sql = "SELECT COUNT(*) FROM MANUAL_JOB_CONFIG WHERE JOB_NAME = ? AND STATUS = 'ACTIVE'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, jobName);
        return count != null && count > 0;
    }

    @Override
    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM MANUAL_JOB_CONFIG WHERE STATUS = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status);
        return count != null ? count : 0L;
    }

    @Override
    public long countConfigurationsCreatedToday() {
        String sql = "SELECT COUNT(*) FROM MANUAL_JOB_CONFIG WHERE DATE(CREATED_DATE) = DATE(?)";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, Timestamp.valueOf(LocalDateTime.now()));
        return count != null ? count : 0L;
    }

    @Override
    public long countConfigurationsModifiedThisWeek() {
        LocalDateTime weekStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(7);
        String sql = "SELECT COUNT(*) FROM MANUAL_JOB_CONFIG WHERE UPDATED_DATE >= ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, Timestamp.valueOf(weekStart));
        return count != null ? count : 0L;
    }

    private class ManualJobConfigRowMapper implements RowMapper<ManualJobConfigEntity> {
        @Override
        public ManualJobConfigEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            ManualJobConfigEntity entity = new ManualJobConfigEntity();
            entity.setConfigId(rs.getString("CONFIG_ID"));
            entity.setJobName(rs.getString("JOB_NAME"));
            entity.setJobType(rs.getString("JOB_TYPE"));
            entity.setSourceSystem(rs.getString("SOURCE_SYSTEM"));
            entity.setTargetSystem(rs.getString("TARGET_SYSTEM"));
            entity.setJobParameters(rs.getString("JOB_PARAMETERS"));
            entity.setStatus(rs.getString("STATUS"));
            entity.setCreatedBy(rs.getString("CREATED_BY"));
            
            Timestamp createdDate = rs.getTimestamp("CREATED_DATE");
            if (createdDate != null) {
                entity.setCreatedDate(createdDate.toLocalDateTime());
            }
            
            entity.setUpdatedBy(rs.getString("UPDATED_BY"));
            
            Timestamp updatedDate = rs.getTimestamp("UPDATED_DATE");
            if (updatedDate != null) {
                entity.setUpdatedDate(updatedDate.toLocalDateTime());
            }
            
            entity.setVersionNumber(rs.getLong("VERSION_DECIMAL"));
            return entity;
        }
    }
}