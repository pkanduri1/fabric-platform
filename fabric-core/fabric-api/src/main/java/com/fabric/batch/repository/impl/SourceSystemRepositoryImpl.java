package com.fabric.batch.repository.impl;

import com.fabric.batch.entity.SourceSystemEntity;
import com.fabric.batch.repository.SourceSystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JdbcTemplate implementation of SourceSystemRepository
 * Replaces JPA-based repository to eliminate JPA dependencies
 * 
 * Maps to existing CM3INT.SOURCE_SYSTEMS table
 */
@Repository
public class SourceSystemRepositoryImpl implements SourceSystemRepository {

    private static final Logger logger = LoggerFactory.getLogger(SourceSystemRepositoryImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<SourceSystemEntity> rowMapper = new SourceSystemRowMapper();

    @Override
    public List<SourceSystemEntity> findByEnabledOrderByName(String enabled) {
        String sql = "SELECT * FROM CM3INT.SOURCE_SYSTEMS WHERE ENABLED = ? ORDER BY NAME";
        return jdbcTemplate.query(sql, rowMapper, enabled);
    }

    @Override
    public List<SourceSystemEntity> findAllByOrderByName() {
        String sql = "SELECT * FROM CM3INT.SOURCE_SYSTEMS ORDER BY NAME";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public List<SourceSystemEntity> findByTypeOrderByName(String type) {
        String sql = "SELECT * FROM CM3INT.SOURCE_SYSTEMS WHERE TYPE = ? ORDER BY NAME";
        return jdbcTemplate.query(sql, rowMapper, type);
    }

    @Override
    public List<SourceSystemEntity> findByTypeAndEnabledOrderByName(String type, String enabled) {
        String sql = "SELECT * FROM CM3INT.SOURCE_SYSTEMS WHERE TYPE = ? AND ENABLED = ? ORDER BY NAME";
        return jdbcTemplate.query(sql, rowMapper, type, enabled);
    }

    @Override
    public long countByEnabled(String enabled) {
        String sql = "SELECT COUNT(*) FROM CM3INT.SOURCE_SYSTEMS WHERE ENABLED = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, enabled);
    }

    @Override
    public List<SourceSystemEntity> findByJobCountGreaterThanOrderByJobCountDescNameAsc(Integer jobCount) {
        String sql = "SELECT * FROM CM3INT.SOURCE_SYSTEMS WHERE JOB_COUNT > ? ORDER BY JOB_COUNT DESC, NAME ASC";
        return jdbcTemplate.query(sql, rowMapper, jobCount);
    }

    @Override
    public List<SourceSystemEntity> findByNameContainingIgnoreCaseOrderByName(String name) {
        String sql = "SELECT * FROM CM3INT.SOURCE_SYSTEMS WHERE UPPER(NAME) LIKE UPPER(?) ORDER BY NAME";
        return jdbcTemplate.query(sql, rowMapper, "%" + name + "%");
    }

    @Override
    public boolean existsByIdIgnoreCase(String id) {
        String sql = "SELECT COUNT(*) FROM CM3INT.SOURCE_SYSTEMS WHERE UPPER(ID) = UPPER(?)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    // Standard CRUD operations (replacing JpaRepository methods)

    @Override
    public <S extends SourceSystemEntity> S save(S entity) {
        if (existsById(entity.getId())) {
            return update(entity);
        } else {
            return insert(entity);
        }
    }

    @Override
    public <S extends SourceSystemEntity> Iterable<S> saveAll(Iterable<S> entities) {
        for (S entity : entities) {
            save(entity);
        }
        return entities;
    }

    @Override
    public Optional<SourceSystemEntity> findById(String id) {
        String sql = "SELECT * FROM CM3INT.SOURCE_SYSTEMS WHERE ID = ?";
        try {
            SourceSystemEntity entity = jdbcTemplate.queryForObject(sql, rowMapper, id);
            return Optional.ofNullable(entity);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT COUNT(*) FROM CM3INT.SOURCE_SYSTEMS WHERE ID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public List<SourceSystemEntity> findAll() {
        String sql = "SELECT * FROM CM3INT.SOURCE_SYSTEMS";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public Iterable<SourceSystemEntity> findAllById(Iterable<String> ids) {
        // Convert iterable to list for SQL IN clause
        StringBuilder sql = new StringBuilder("SELECT * FROM CM3INT.SOURCE_SYSTEMS WHERE ID IN (");
        StringBuilder placeholders = new StringBuilder();
        Object[] params = new Object[((List<String>) ids).size()];
        int i = 0;
        for (String id : ids) {
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
        String sql = "SELECT COUNT(*) FROM CM3INT.SOURCE_SYSTEMS";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM CM3INT.SOURCE_SYSTEMS WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public void delete(SourceSystemEntity entity) {
        deleteById(entity.getId());
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        for (String id : ids) {
            deleteById(id);
        }
    }

    @Override
    public void deleteAll(Iterable<? extends SourceSystemEntity> entities) {
        for (SourceSystemEntity entity : entities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM CM3INT.SOURCE_SYSTEMS";
        jdbcTemplate.update(sql);
    }

    // Helper methods for save operation
    private <S extends SourceSystemEntity> S insert(S entity) {
        String sql = """
            INSERT INTO CM3INT.SOURCE_SYSTEMS 
            (ID, NAME, TYPE, DESCRIPTION, CONNECTION_STRING, ENABLED, CREATED_DATE, JOB_COUNT)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        if (entity.getCreatedDate() == null) {
            entity.setCreatedDate(LocalDateTime.now());
        }
        if (entity.getJobCount() == null) {
            entity.setJobCount(0);
        }

        jdbcTemplate.update(sql,
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getDescription(),
                entity.getConnectionString(),
                entity.getEnabled(),
                entity.getCreatedDate(),
                entity.getJobCount()
        );

        logger.info("Inserted source system: {}", entity.getId());
        return entity;
    }

    private <S extends SourceSystemEntity> S update(S entity) {
        String sql = """
            UPDATE CM3INT.SOURCE_SYSTEMS 
            SET NAME = ?, TYPE = ?, DESCRIPTION = ?, CONNECTION_STRING = ?, ENABLED = ?, JOB_COUNT = ?
            WHERE ID = ?
        """;

        jdbcTemplate.update(sql,
                entity.getName(),
                entity.getType(),
                entity.getDescription(),
                entity.getConnectionString(),
                entity.getEnabled(),
                entity.getJobCount(),
                entity.getId()
        );

        logger.info("Updated source system: {}", entity.getId());
        return entity;
    }

    /**
     * RowMapper for SourceSystemEntity
     */
    private static class SourceSystemRowMapper implements RowMapper<SourceSystemEntity> {
        @Override
        public SourceSystemEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            SourceSystemEntity entity = new SourceSystemEntity();
            entity.setId(rs.getString("ID"));
            entity.setName(rs.getString("NAME"));
            entity.setType(rs.getString("TYPE"));
            entity.setDescription(rs.getString("DESCRIPTION"));
            entity.setConnectionString(rs.getString("CONNECTION_STRING"));
            entity.setEnabled(rs.getString("ENABLED"));
            
            // Handle LocalDateTime conversion
            if (rs.getTimestamp("CREATED_DATE") != null) {
                entity.setCreatedDate(rs.getTimestamp("CREATED_DATE").toLocalDateTime());
            }
            
            entity.setJobCount(rs.getInt("JOB_COUNT"));
            if (rs.wasNull()) {
                entity.setJobCount(0);
            }
            
            return entity;
        }
    }
}