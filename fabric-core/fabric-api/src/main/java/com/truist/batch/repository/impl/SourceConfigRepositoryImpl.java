package com.truist.batch.repository.impl;

import com.truist.batch.entity.SourceConfigEntity;
import com.truist.batch.repository.SourceConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JdbcTemplate-based implementation of SourceConfigRepository.
 * Provides direct database access without JPA/Hibernate overhead.
 *
 * This repository implementation supports the Phase 2 database-driven
 * configuration system with optimized methods for Spring PropertySource integration.
 *
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Phase 2 - Database-Driven Source Configuration System
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class SourceConfigRepositoryImpl implements SourceConfigRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * RowMapper for SOURCE_CONFIG table.
     * Maps database rows to SourceConfigEntity objects.
     */
    private static class SourceConfigRowMapper implements RowMapper<SourceConfigEntity> {
        @Override
        public SourceConfigEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            return SourceConfigEntity.builder()
                .configId(rs.getString("CONFIG_ID"))
                .sourceCode(rs.getString("SOURCE_CODE"))
                .configKey(rs.getString("CONFIG_KEY"))
                .configValue(rs.getString("CONFIG_VALUE"))
                .description(rs.getString("DESCRIPTION"))
                .createdDate(rs.getTimestamp("CREATED_DATE") != null
                    ? rs.getTimestamp("CREATED_DATE").toLocalDateTime()
                    : null)
                .modifiedDate(rs.getTimestamp("MODIFIED_DATE") != null
                    ? rs.getTimestamp("MODIFIED_DATE").toLocalDateTime()
                    : null)
                .modifiedBy(rs.getString("MODIFIED_BY"))
                .active(rs.getString("ACTIVE"))
                .build();
        }
    }

    @Override
    public Optional<SourceConfigEntity> findById(String configId) {
        String sql = "SELECT * FROM SOURCE_CONFIG WHERE CONFIG_ID = ?";
        try {
            SourceConfigEntity entity = jdbcTemplate.queryForObject(sql, new SourceConfigRowMapper(), configId);
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            log.debug("No source config found with id: {}", configId);
            return Optional.empty();
        }
    }

    @Override
    public List<SourceConfigEntity> findAll() {
        String sql = "SELECT * FROM SOURCE_CONFIG ORDER BY SOURCE_CODE, CONFIG_KEY";
        return jdbcTemplate.query(sql, new SourceConfigRowMapper());
    }

    @Override
    public List<SourceConfigEntity> findAllActive() {
        String sql = "SELECT * FROM SOURCE_CONFIG WHERE ACTIVE = 'Y' ORDER BY SOURCE_CODE, CONFIG_KEY";
        return jdbcTemplate.query(sql, new SourceConfigRowMapper());
    }

    @Override
    public List<SourceConfigEntity> findBySourceCode(String sourceCode) {
        String sql = "SELECT * FROM SOURCE_CONFIG WHERE SOURCE_CODE = ? ORDER BY CONFIG_KEY";
        return jdbcTemplate.query(sql, new SourceConfigRowMapper(), sourceCode);
    }

    @Override
    public List<SourceConfigEntity> findBySourceCodeActive(String sourceCode) {
        String sql = "SELECT * FROM SOURCE_CONFIG WHERE SOURCE_CODE = ? AND ACTIVE = 'Y' ORDER BY CONFIG_KEY";
        return jdbcTemplate.query(sql, new SourceConfigRowMapper(), sourceCode);
    }

    @Override
    public Optional<SourceConfigEntity> findBySourceCodeAndConfigKey(String sourceCode, String configKey) {
        String sql = "SELECT * FROM SOURCE_CONFIG WHERE SOURCE_CODE = ? AND CONFIG_KEY = ?";
        try {
            SourceConfigEntity entity = jdbcTemplate.queryForObject(sql, new SourceConfigRowMapper(), sourceCode, configKey);
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            log.debug("No source config found for source: {}, key: {}", sourceCode, configKey);
            return Optional.empty();
        }
    }

    @Override
    public Map<String, String> getActivePropertiesMapForSource(String sourceCode) {
        log.debug("Loading active properties for source: {}", sourceCode);
        List<SourceConfigEntity> configs = findBySourceCodeActive(sourceCode);

        Map<String, String> propertiesMap = configs.stream()
            .collect(Collectors.toMap(
                SourceConfigEntity::getConfigKey,
                SourceConfigEntity::getConfigValue,
                (existing, replacement) -> {
                    log.warn("Duplicate config key found for source {}: {}. Using latest value.", sourceCode, existing);
                    return replacement;
                }
            ));

        log.info("Loaded {} properties for source: {}", propertiesMap.size(), sourceCode);
        return propertiesMap;
    }

    @Override
    public Map<String, String> getAllActivePropertiesMap() {
        log.debug("Loading all active properties from database");
        List<SourceConfigEntity> configs = findAllActive();

        Map<String, String> propertiesMap = configs.stream()
            .collect(Collectors.toMap(
                SourceConfigEntity::getConfigKey,
                SourceConfigEntity::getConfigValue,
                (existing, replacement) -> {
                    log.warn("Duplicate config key found: {}. Using latest value.", existing);
                    return replacement;
                }
            ));

        log.info("Loaded {} total active properties from database", propertiesMap.size());
        return propertiesMap;
    }

    @Override
    public SourceConfigEntity save(SourceConfigEntity entity) {
        if (entity.getConfigId() == null || entity.getConfigId().trim().isEmpty()) {
            entity.setConfigId(generateConfigId(entity.getSourceCode()));
        }

        if (existsById(entity.getConfigId())) {
            return update(entity);
        } else {
            return insert(entity);
        }
    }

    /**
     * Insert a new source configuration.
     *
     * @param entity the source configuration entity to insert
     * @return the inserted entity
     */
    private SourceConfigEntity insert(SourceConfigEntity entity) {
        String sql = """
            INSERT INTO SOURCE_CONFIG (
                CONFIG_ID, SOURCE_CODE, CONFIG_KEY, CONFIG_VALUE, DESCRIPTION,
                CREATED_DATE, MODIFIED_DATE, MODIFIED_BY, ACTIVE
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedDate(now);
        entity.setModifiedDate(now);

        jdbcTemplate.update(sql,
            entity.getConfigId(),
            entity.getSourceCode(),
            entity.getConfigKey(),
            entity.getConfigValue(),
            entity.getDescription(),
            Timestamp.valueOf(now),
            Timestamp.valueOf(now),
            entity.getModifiedBy(),
            entity.getActive()
        );

        log.info("Inserted new source config: {} for source: {}", entity.getConfigKey(), entity.getSourceCode());
        return entity;
    }

    /**
     * Update an existing source configuration.
     *
     * @param entity the source configuration entity to update
     * @return the updated entity
     */
    private SourceConfigEntity update(SourceConfigEntity entity) {
        String sql = """
            UPDATE SOURCE_CONFIG SET
                SOURCE_CODE = ?, CONFIG_KEY = ?, CONFIG_VALUE = ?, DESCRIPTION = ?,
                MODIFIED_DATE = ?, MODIFIED_BY = ?, ACTIVE = ?
            WHERE CONFIG_ID = ?
        """;

        LocalDateTime now = LocalDateTime.now();
        entity.setModifiedDate(now);

        jdbcTemplate.update(sql,
            entity.getSourceCode(),
            entity.getConfigKey(),
            entity.getConfigValue(),
            entity.getDescription(),
            Timestamp.valueOf(now),
            entity.getModifiedBy(),
            entity.getActive(),
            entity.getConfigId()
        );

        log.info("Updated source config: {} for source: {}", entity.getConfigKey(), entity.getSourceCode());
        return entity;
    }

    @Override
    public void deleteById(String configId) {
        String sql = "DELETE FROM SOURCE_CONFIG WHERE CONFIG_ID = ?";
        int rowsAffected = jdbcTemplate.update(sql, configId);
        if (rowsAffected > 0) {
            log.info("Deleted source config: {}", configId);
        }
    }

    @Override
    public boolean existsById(String configId) {
        String sql = "SELECT COUNT(*) FROM SOURCE_CONFIG WHERE CONFIG_ID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, configId);
        return count != null && count > 0;
    }

    @Override
    public boolean existsBySourceCodeAndConfigKey(String sourceCode, String configKey) {
        String sql = "SELECT COUNT(*) FROM SOURCE_CONFIG WHERE SOURCE_CODE = ? AND CONFIG_KEY = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, sourceCode, configKey);
        return count != null && count > 0;
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM SOURCE_CONFIG";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public long countActive() {
        String sql = "SELECT COUNT(*) FROM SOURCE_CONFIG WHERE ACTIVE = 'Y'";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public long countBySourceCode(String sourceCode) {
        String sql = "SELECT COUNT(*) FROM SOURCE_CONFIG WHERE SOURCE_CODE = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, sourceCode);
        return count != null ? count : 0L;
    }

    @Override
    public List<String> findDistinctSourceCodes() {
        String sql = "SELECT DISTINCT SOURCE_CODE FROM SOURCE_CONFIG ORDER BY SOURCE_CODE";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    @Override
    public boolean activate(String configId, String modifiedBy) {
        String sql = """
            UPDATE SOURCE_CONFIG SET
                ACTIVE = 'Y', MODIFIED_DATE = ?, MODIFIED_BY = ?
            WHERE CONFIG_ID = ?
        """;

        LocalDateTime now = LocalDateTime.now();
        int rowsAffected = jdbcTemplate.update(sql, Timestamp.valueOf(now), modifiedBy, configId);

        if (rowsAffected > 0) {
            log.info("Activated source config: {} by user: {}", configId, modifiedBy);
            return true;
        }
        return false;
    }

    @Override
    public boolean deactivate(String configId, String modifiedBy) {
        String sql = """
            UPDATE SOURCE_CONFIG SET
                ACTIVE = 'N', MODIFIED_DATE = ?, MODIFIED_BY = ?
            WHERE CONFIG_ID = ?
        """;

        LocalDateTime now = LocalDateTime.now();
        int rowsAffected = jdbcTemplate.update(sql, Timestamp.valueOf(now), modifiedBy, configId);

        if (rowsAffected > 0) {
            log.info("Deactivated source config: {} by user: {}", configId, modifiedBy);
            return true;
        }
        return false;
    }

    @Override
    public boolean updateValue(String configId, String newValue, String modifiedBy) {
        String sql = """
            UPDATE SOURCE_CONFIG SET
                CONFIG_VALUE = ?, MODIFIED_DATE = ?, MODIFIED_BY = ?
            WHERE CONFIG_ID = ?
        """;

        LocalDateTime now = LocalDateTime.now();
        int rowsAffected = jdbcTemplate.update(sql, newValue, Timestamp.valueOf(now), modifiedBy, configId);

        if (rowsAffected > 0) {
            log.info("Updated source config value: {} by user: {}", configId, modifiedBy);
            return true;
        }
        return false;
    }

    /**
     * Generate a unique configuration ID for a source.
     *
     * @param sourceCode the source system code
     * @return generated configuration ID
     */
    private String generateConfigId(String sourceCode) {
        String prefix = "cfg_" + sourceCode.toLowerCase() + "_";
        String sql = "SELECT COUNT(*) FROM SOURCE_CONFIG WHERE SOURCE_CODE = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, sourceCode);
        long sequence = (count != null ? count : 0L) + 1;
        return prefix + String.format("%03d", sequence);
    }
}
