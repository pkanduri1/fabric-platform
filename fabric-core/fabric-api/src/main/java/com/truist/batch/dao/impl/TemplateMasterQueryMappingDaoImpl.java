package com.truist.batch.dao.impl;

import com.truist.batch.dao.TemplateMasterQueryMappingDao;
import com.truist.batch.entity.TemplateMasterQueryMappingEntity;
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

/**
 * =========================================================================
 * TEMPLATE MASTER QUERY MAPPING DAO IMPLEMENTATION
 * =========================================================================
 * 
 * Purpose: JdbcTemplate-based implementation for TEMPLATE_MASTER_QUERY_MAPPING operations
 * - Provides efficient database operations with proper connection management
 * - Implements parameterized queries for SQL injection protection
 * - Supports banking-grade transaction management
 * 
 * Security: All SQL operations use parameterized queries
 * Performance: Optimized with batch operations where applicable
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Phase 3 - Master Query Integration
 * =========================================================================
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TemplateMasterQueryMappingDaoImpl implements TemplateMasterQueryMappingDao {

    private final JdbcTemplate jdbcTemplate;

    // =========================================================================
    // SQL CONSTANTS
    // =========================================================================
    
    private static final String INSERT_MAPPING = """
        INSERT INTO TEMPLATE_MASTER_QUERY_MAPPING (
            MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL, 
            QUERY_DESCRIPTION, QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
            QUERY_PARAMETERS, PARAMETER_VALIDATION_RULES, STATUS, IS_READ_ONLY,
            SECURITY_CLASSIFICATION, REQUIRES_APPROVAL, CREATED_BY, CREATED_DATE,
            CORRELATION_ID
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;
    
    private static final String UPDATE_MAPPING = """
        UPDATE TEMPLATE_MASTER_QUERY_MAPPING SET
            QUERY_NAME = ?, QUERY_SQL = ?, QUERY_DESCRIPTION = ?,
            QUERY_TYPE = ?, MAX_EXECUTION_TIME_SECONDS = ?, MAX_RESULT_ROWS = ?,
            QUERY_PARAMETERS = ?, PARAMETER_VALIDATION_RULES = ?,
            STATUS = ?, IS_READ_ONLY = ?, SECURITY_CLASSIFICATION = ?,
            REQUIRES_APPROVAL = ?, UPDATED_BY = ?, UPDATED_DATE = ?,
            CORRELATION_ID = ?
        WHERE MAPPING_ID = ?
    """;
    
    private static final String SELECT_BASE = """
        SELECT MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL,
               QUERY_DESCRIPTION, QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS,
               QUERY_PARAMETERS, PARAMETER_VALIDATION_RULES, STATUS, IS_READ_ONLY,
               SECURITY_CLASSIFICATION, REQUIRES_APPROVAL, CREATED_BY, CREATED_DATE,
               UPDATED_BY, UPDATED_DATE, CORRELATION_ID
        FROM TEMPLATE_MASTER_QUERY_MAPPING
    """;
    
    private static final String SELECT_BY_MAPPING_ID = SELECT_BASE + " WHERE MAPPING_ID = ?";
    private static final String SELECT_BY_CONFIG_ID = SELECT_BASE + " WHERE CONFIG_ID = ?";
    private static final String SELECT_BY_CONFIG_AND_QUERY = SELECT_BASE + " WHERE CONFIG_ID = ? AND MASTER_QUERY_ID = ?";
    private static final String SELECT_ACTIVE = SELECT_BASE + " WHERE STATUS = 'ACTIVE'";
    private static final String SELECT_BY_STATUS = SELECT_BASE + " WHERE STATUS = ?";
    private static final String SELECT_BY_MASTER_QUERY_ID = SELECT_BASE + " WHERE MASTER_QUERY_ID = ?";
    private static final String SELECT_BY_QUERY_NAME = SELECT_BASE + " WHERE QUERY_NAME = ?";
    private static final String SELECT_REQUIRING_APPROVAL = SELECT_BASE + " WHERE REQUIRES_APPROVAL = 'Y'";
    private static final String SELECT_BY_SECURITY_CLASS = SELECT_BASE + " WHERE SECURITY_CLASSIFICATION = ?";
    private static final String SELECT_READ_ONLY = SELECT_BASE + " WHERE IS_READ_ONLY = 'Y'";
    private static final String SELECT_BY_CREATED_BY = SELECT_BASE + " WHERE CREATED_BY = ?";
    private static final String SELECT_BY_CORRELATION_ID = SELECT_BASE + " WHERE CORRELATION_ID = ?";
    
    private static final String DELETE_BY_MAPPING_ID = "DELETE FROM TEMPLATE_MASTER_QUERY_MAPPING WHERE MAPPING_ID = ?";
    private static final String DELETE_BY_CONFIG_ID = "DELETE FROM TEMPLATE_MASTER_QUERY_MAPPING WHERE CONFIG_ID = ?";
    
    private static final String COUNT_ALL = "SELECT COUNT(*) FROM TEMPLATE_MASTER_QUERY_MAPPING";
    private static final String COUNT_ACTIVE = "SELECT COUNT(*) FROM TEMPLATE_MASTER_QUERY_MAPPING WHERE STATUS = 'ACTIVE'";
    private static final String COUNT_BY_CONFIG_ID = "SELECT COUNT(*) FROM TEMPLATE_MASTER_QUERY_MAPPING WHERE CONFIG_ID = ?";
    
    private static final String EXISTS_BY_CONFIG_ID = "SELECT COUNT(*) FROM TEMPLATE_MASTER_QUERY_MAPPING WHERE CONFIG_ID = ?";
    private static final String EXISTS_BY_CONFIG_AND_QUERY = "SELECT COUNT(*) FROM TEMPLATE_MASTER_QUERY_MAPPING WHERE CONFIG_ID = ? AND MASTER_QUERY_ID = ?";

    // =========================================================================
    // ROW MAPPER
    // =========================================================================
    
    private final RowMapper<TemplateMasterQueryMappingEntity> rowMapper = new RowMapper<TemplateMasterQueryMappingEntity>() {
        @Override
        public TemplateMasterQueryMappingEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            TemplateMasterQueryMappingEntity entity = new TemplateMasterQueryMappingEntity();
            
            entity.setMappingId(rs.getString("MAPPING_ID"));
            entity.setConfigId(rs.getString("CONFIG_ID"));
            entity.setMasterQueryId(rs.getString("MASTER_QUERY_ID"));
            entity.setQueryName(rs.getString("QUERY_NAME"));
            entity.setQuerySql(rs.getString("QUERY_SQL"));
            entity.setQueryDescription(rs.getString("QUERY_DESCRIPTION"));
            entity.setQueryType(rs.getString("QUERY_TYPE"));
            entity.setMaxExecutionTimeSeconds(rs.getInt("MAX_EXECUTION_TIME_SECONDS"));
            entity.setMaxResultRows(rs.getInt("MAX_RESULT_ROWS"));
            entity.setQueryParameters(rs.getString("QUERY_PARAMETERS"));
            entity.setParameterValidationRules(rs.getString("PARAMETER_VALIDATION_RULES"));
            entity.setStatus(rs.getString("STATUS"));
            entity.setIsReadOnly(rs.getString("IS_READ_ONLY"));
            entity.setSecurityClassification(rs.getString("SECURITY_CLASSIFICATION"));
            entity.setRequiresApproval(rs.getString("REQUIRES_APPROVAL"));
            entity.setCreatedBy(rs.getString("CREATED_BY"));
            
            Timestamp createdTimestamp = rs.getTimestamp("CREATED_DATE");
            if (createdTimestamp != null) {
                entity.setCreatedDate(createdTimestamp.toLocalDateTime());
            }
            
            entity.setUpdatedBy(rs.getString("UPDATED_BY"));
            
            Timestamp updatedTimestamp = rs.getTimestamp("UPDATED_DATE");
            if (updatedTimestamp != null) {
                entity.setUpdatedDate(updatedTimestamp.toLocalDateTime());
            }
            
            entity.setCorrelationId(rs.getString("CORRELATION_ID"));
            
            return entity;
        }
    };

    // =========================================================================
    // CORE CRUD OPERATIONS
    // =========================================================================
    
    @Override
    public TemplateMasterQueryMappingEntity save(TemplateMasterQueryMappingEntity mapping) {
        log.info("💾 Saving template master query mapping: {}", mapping.getMappingId());
        
        try {
            int rowsAffected = jdbcTemplate.update(INSERT_MAPPING,
                mapping.getMappingId(),
                mapping.getConfigId(),
                mapping.getMasterQueryId(),
                mapping.getQueryName(),
                mapping.getQuerySql(),
                mapping.getQueryDescription(),
                mapping.getQueryType(),
                mapping.getMaxExecutionTimeSeconds(),
                mapping.getMaxResultRows(),
                mapping.getQueryParameters(),
                mapping.getParameterValidationRules(),
                mapping.getStatus(),
                mapping.getIsReadOnly(),
                mapping.getSecurityClassification(),
                mapping.getRequiresApproval(),
                mapping.getCreatedBy(),
                Timestamp.valueOf(mapping.getCreatedDate()),
                mapping.getCorrelationId()
            );
            
            if (rowsAffected > 0) {
                log.info("✅ Template master query mapping saved successfully: {}", mapping.getMappingId());
                return mapping;
            } else {
                throw new RuntimeException("Failed to save template master query mapping");
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to save template master query mapping {}: {}", mapping.getMappingId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save template master query mapping", e);
        }
    }
    
    @Override
    public Optional<TemplateMasterQueryMappingEntity> findByMappingId(String mappingId) {
        log.debug("🔍 Finding template master query mapping by ID: {}", mappingId);
        
        try {
            List<TemplateMasterQueryMappingEntity> results = jdbcTemplate.query(SELECT_BY_MAPPING_ID, rowMapper, mappingId);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
            
        } catch (Exception e) {
            log.error("❌ Failed to find template master query mapping by ID {}: {}", mappingId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<TemplateMasterQueryMappingEntity> findByConfigId(String configId) {
        log.debug("🔍 Finding template master query mappings by config ID: {}", configId);
        
        try {
            return jdbcTemplate.query(SELECT_BY_CONFIG_ID, rowMapper, configId);
            
        } catch (Exception e) {
            log.error("❌ Failed to find template master query mappings by config ID {}: {}", configId, e.getMessage(), e);
            return List.of();
        }
    }
    
    @Override
    public Optional<TemplateMasterQueryMappingEntity> findByConfigIdAndMasterQueryId(String configId, String masterQueryId) {
        log.debug("🔍 Finding template master query mapping by config ID {} and master query ID {}", configId, masterQueryId);
        
        try {
            List<TemplateMasterQueryMappingEntity> results = jdbcTemplate.query(SELECT_BY_CONFIG_AND_QUERY, rowMapper, configId, masterQueryId);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
            
        } catch (Exception e) {
            log.error("❌ Failed to find template master query mapping by config ID {} and master query ID {}: {}", 
                     configId, masterQueryId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public TemplateMasterQueryMappingEntity update(TemplateMasterQueryMappingEntity mapping) {
        log.info("📝 Updating template master query mapping: {}", mapping.getMappingId());
        
        try {
            mapping.preUpdate(); // Set updated timestamp
            
            int rowsAffected = jdbcTemplate.update(UPDATE_MAPPING,
                mapping.getQueryName(),
                mapping.getQuerySql(),
                mapping.getQueryDescription(),
                mapping.getQueryType(),
                mapping.getMaxExecutionTimeSeconds(),
                mapping.getMaxResultRows(),
                mapping.getQueryParameters(),
                mapping.getParameterValidationRules(),
                mapping.getStatus(),
                mapping.getIsReadOnly(),
                mapping.getSecurityClassification(),
                mapping.getRequiresApproval(),
                mapping.getUpdatedBy(),
                Timestamp.valueOf(mapping.getUpdatedDate()),
                mapping.getCorrelationId(),
                mapping.getMappingId()
            );
            
            if (rowsAffected > 0) {
                log.info("✅ Template master query mapping updated successfully: {}", mapping.getMappingId());
                return mapping;
            } else {
                throw new RuntimeException("No mapping found to update with ID: " + mapping.getMappingId());
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to update template master query mapping {}: {}", mapping.getMappingId(), e.getMessage(), e);
            throw new RuntimeException("Failed to update template master query mapping", e);
        }
    }
    
    @Override
    public void deleteByMappingId(String mappingId) {
        log.info("🗑️ Deleting template master query mapping: {}", mappingId);
        
        try {
            int rowsAffected = jdbcTemplate.update(DELETE_BY_MAPPING_ID, mappingId);
            
            if (rowsAffected > 0) {
                log.info("✅ Template master query mapping deleted successfully: {}", mappingId);
            } else {
                log.warn("⚠️ No template master query mapping found to delete with ID: {}", mappingId);
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to delete template master query mapping {}: {}", mappingId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete template master query mapping", e);
        }
    }
    
    @Override
    public void deleteByConfigId(String configId) {
        log.info("🗑️ Deleting template master query mappings for config: {}", configId);
        
        try {
            int rowsAffected = jdbcTemplate.update(DELETE_BY_CONFIG_ID, configId);
            log.info("✅ Deleted {} template master query mappings for config: {}", rowsAffected, configId);
            
        } catch (Exception e) {
            log.error("❌ Failed to delete template master query mappings for config {}: {}", configId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete template master query mappings", e);
        }
    }

    // =========================================================================
    // QUERY OPERATIONS
    // =========================================================================
    
    @Override
    public List<TemplateMasterQueryMappingEntity> findActiveMapping() {
        try {
            return jdbcTemplate.query(SELECT_ACTIVE, rowMapper);
        } catch (Exception e) {
            log.error("❌ Failed to find active template master query mappings: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    @Override
    public List<TemplateMasterQueryMappingEntity> findByStatus(String status) {
        try {
            return jdbcTemplate.query(SELECT_BY_STATUS, rowMapper, status);
        } catch (Exception e) {
            log.error("❌ Failed to find template master query mappings by status {}: {}", status, e.getMessage(), e);
            return List.of();
        }
    }
    
    @Override
    public List<TemplateMasterQueryMappingEntity> findByMasterQueryId(String masterQueryId) {
        try {
            return jdbcTemplate.query(SELECT_BY_MASTER_QUERY_ID, rowMapper, masterQueryId);
        } catch (Exception e) {
            log.error("❌ Failed to find template master query mappings by master query ID {}: {}", masterQueryId, e.getMessage(), e);
            return List.of();
        }
    }
    
    @Override
    public List<TemplateMasterQueryMappingEntity> findByQueryName(String queryName) {
        try {
            return jdbcTemplate.query(SELECT_BY_QUERY_NAME, rowMapper, queryName);
        } catch (Exception e) {
            log.error("❌ Failed to find template master query mappings by query name {}: {}", queryName, e.getMessage(), e);
            return List.of();
        }
    }

    // =========================================================================
    // VALIDATION AND UTILITY OPERATIONS
    // =========================================================================
    
    @Override
    public boolean existsByConfigId(String configId) {
        try {
            Integer count = jdbcTemplate.queryForObject(EXISTS_BY_CONFIG_ID, Integer.class, configId);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("❌ Failed to check existence by config ID {}: {}", configId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean existsByConfigIdAndMasterQueryId(String configId, String masterQueryId) {
        try {
            Integer count = jdbcTemplate.queryForObject(EXISTS_BY_CONFIG_AND_QUERY, Integer.class, configId, masterQueryId);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("❌ Failed to check existence by config ID {} and master query ID {}: {}", 
                     configId, masterQueryId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public long count() {
        try {
            Long count = jdbcTemplate.queryForObject(COUNT_ALL, Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("❌ Failed to count template master query mappings: {}", e.getMessage(), e);
            return 0L;
        }
    }
    
    @Override
    public long countActive() {
        try {
            Long count = jdbcTemplate.queryForObject(COUNT_ACTIVE, Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("❌ Failed to count active template master query mappings: {}", e.getMessage(), e);
            return 0L;
        }
    }
    
    @Override
    public long countByConfigId(String configId) {
        try {
            Long count = jdbcTemplate.queryForObject(COUNT_BY_CONFIG_ID, Long.class, configId);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("❌ Failed to count template master query mappings by config ID {}: {}", configId, e.getMessage(), e);
            return 0L;
        }
    }

    // =========================================================================
    // SECURITY AND COMPLIANCE OPERATIONS
    // =========================================================================
    
    @Override
    public List<TemplateMasterQueryMappingEntity> findMappingsRequiringApproval() {
        try {
            return jdbcTemplate.query(SELECT_REQUIRING_APPROVAL, rowMapper);
        } catch (Exception e) {
            log.error("❌ Failed to find mappings requiring approval: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    @Override
    public List<TemplateMasterQueryMappingEntity> findBySecurityClassification(String securityClassification) {
        try {
            return jdbcTemplate.query(SELECT_BY_SECURITY_CLASS, rowMapper, securityClassification);
        } catch (Exception e) {
            log.error("❌ Failed to find mappings by security classification {}: {}", securityClassification, e.getMessage(), e);
            return List.of();
        }
    }
    
    @Override
    public List<TemplateMasterQueryMappingEntity> findReadOnlyMappings() {
        try {
            return jdbcTemplate.query(SELECT_READ_ONLY, rowMapper);
        } catch (Exception e) {
            log.error("❌ Failed to find read-only mappings: {}", e.getMessage(), e);
            return List.of();
        }
    }

    // =========================================================================
    // AUDIT AND MONITORING OPERATIONS
    // =========================================================================
    
    @Override
    public List<TemplateMasterQueryMappingEntity> findByCreatedBy(String createdBy) {
        try {
            return jdbcTemplate.query(SELECT_BY_CREATED_BY, rowMapper, createdBy);
        } catch (Exception e) {
            log.error("❌ Failed to find mappings by created by {}: {}", createdBy, e.getMessage(), e);
            return List.of();
        }
    }
    
    @Override
    public List<TemplateMasterQueryMappingEntity> findByCorrelationId(String correlationId) {
        try {
            return jdbcTemplate.query(SELECT_BY_CORRELATION_ID, rowMapper, correlationId);
        } catch (Exception e) {
            log.error("❌ Failed to find mappings by correlation ID {}: {}", correlationId, e.getMessage(), e);
            return List.of();
        }
    }
    
    @Override
    public List<TemplateMasterQueryMappingEntity> findAllForAudit() {
        try {
            return jdbcTemplate.query(SELECT_BASE + " ORDER BY CREATED_DATE DESC", rowMapper);
        } catch (Exception e) {
            log.error("❌ Failed to find all mappings for audit: {}", e.getMessage(), e);
            return List.of();
        }
    }
}