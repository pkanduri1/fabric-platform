package com.truist.batch.repository.impl;

import com.truist.batch.entity.DataLoadAuditEntity;
import com.truist.batch.repository.DataLoadAuditRepositoryJdbc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * JdbcTemplate implementation of DataLoadAuditRepository.
 * Provides high-performance database operations for audit trail management.
 */
@Slf4j
@Repository
public class DataLoadAuditRepositoryJdbcImpl implements DataLoadAuditRepositoryJdbc {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String INSERT_AUDIT_SQL = """
            INSERT INTO data_load_audit (
                config_id, job_execution_id, correlation_id, audit_type, event_name, event_description,
                source_system, target_table, file_name, file_path, record_count, processed_count, error_count,
                data_source, data_destination, transformation_applied, validation_rules_applied, business_rules_applied,
                data_quality_score, compliance_status, regulatory_requirement, retention_period_days, pii_fields,
                sensitive_data_hash, encryption_applied, masking_applied, access_control_applied, user_id, session_id,
                ip_address, user_agent, application_name, application_version, environment, host_name, process_id,
                thread_id, execution_time_ms, memory_usage_mb, cpu_usage_percent, error_code, error_message,
                stack_trace, additional_metadata, parent_audit_id, audit_timestamp, created_date
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            """;

    private static final String FIND_BY_CORRELATION_ID_SQL = """
            SELECT * FROM data_load_audit 
            WHERE correlation_id = ? 
            ORDER BY audit_timestamp
            """;

    private static final String DELETE_BY_IDS_SQL = """
            DELETE FROM data_load_audit WHERE audit_id = ?
            """;

    private static final String FIND_FOR_RETENTION_SQL = """
            SELECT * FROM data_load_audit 
            WHERE audit_timestamp <= ? 
            ORDER BY audit_timestamp
            """;

    /**
     * Row mapper for DataLoadAuditEntity.
     */
    private final RowMapper<DataLoadAuditEntity> auditEntityRowMapper = new RowMapper<DataLoadAuditEntity>() {
        @Override
        public DataLoadAuditEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            DataLoadAuditEntity entity = new DataLoadAuditEntity();
            
            entity.setAuditId(rs.getLong("audit_id"));
            entity.setConfigId(rs.getString("config_id"));
            entity.setJobExecutionId(rs.getString("job_execution_id"));
            entity.setCorrelationId(rs.getString("correlation_id"));
            
            String auditTypeStr = rs.getString("audit_type");
            if (auditTypeStr != null) {
                try {
                    entity.setAuditType(DataLoadAuditEntity.AuditType.valueOf(auditTypeStr));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid audit type: {}", auditTypeStr);
                    entity.setAuditType(DataLoadAuditEntity.AuditType.SYSTEM_EVENT);
                }
            }
            
            entity.setEventName(rs.getString("event_name"));
            entity.setEventDescription(rs.getString("event_description"));
            entity.setSourceSystem(rs.getString("source_system"));
            entity.setTargetTable(rs.getString("target_table"));
            entity.setFileName(rs.getString("file_name"));
            entity.setFilePath(rs.getString("file_path"));
            
            entity.setRecordCount(rs.getObject("record_count", Long.class));
            entity.setProcessedCount(rs.getObject("processed_count", Long.class));
            entity.setErrorCount(rs.getObject("error_count", Long.class));
            
            entity.setDataSource(rs.getString("data_source"));
            entity.setDataDestination(rs.getString("data_destination"));
            entity.setTransformationApplied(rs.getString("transformation_applied"));
            entity.setValidationRulesApplied(rs.getString("validation_rules_applied"));
            entity.setBusinessRulesApplied(rs.getString("business_rules_applied"));
            
            entity.setDataQualityScore(rs.getObject("data_quality_score", Double.class));
            
            String complianceStatusStr = rs.getString("compliance_status");
            if (complianceStatusStr != null) {
                try {
                    entity.setComplianceStatus(DataLoadAuditEntity.ComplianceStatus.valueOf(complianceStatusStr));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid compliance status: {}", complianceStatusStr);
                    entity.setComplianceStatus(DataLoadAuditEntity.ComplianceStatus.COMPLIANT);
                }
            }
            
            entity.setRegulatoryRequirement(rs.getString("regulatory_requirement"));
            entity.setRetentionPeriodDays(rs.getObject("retention_period_days", Integer.class));
            entity.setPiiFields(rs.getString("pii_fields"));
            entity.setSensitiveDataHash(rs.getString("sensitive_data_hash"));
            entity.setEncryptionApplied(rs.getString("encryption_applied"));
            entity.setMaskingApplied(rs.getString("masking_applied"));
            entity.setAccessControlApplied(rs.getString("access_control_applied"));
            
            entity.setUserId(rs.getString("user_id"));
            entity.setSessionId(rs.getString("session_id"));
            entity.setIpAddress(rs.getString("ip_address"));
            entity.setUserAgent(rs.getString("user_agent"));
            entity.setApplicationName(rs.getString("application_name"));
            entity.setApplicationVersion(rs.getString("application_version"));
            entity.setEnvironment(rs.getString("environment"));
            entity.setHostName(rs.getString("host_name"));
            
            entity.setProcessId(rs.getObject("process_id", Long.class));
            entity.setThreadId(rs.getObject("thread_id", Long.class));
            entity.setExecutionTimeMs(rs.getObject("execution_time_ms", Long.class));
            entity.setMemoryUsageMb(rs.getObject("memory_usage_mb", Double.class));
            entity.setCpuUsagePercent(rs.getObject("cpu_usage_percent", Double.class));
            
            entity.setErrorCode(rs.getString("error_code"));
            entity.setErrorMessage(rs.getString("error_message"));
            entity.setStackTrace(rs.getString("stack_trace"));
            entity.setAdditionalMetadata(rs.getString("additional_metadata"));
            entity.setParentAuditId(rs.getObject("parent_audit_id", Long.class));
            
            Timestamp auditTimestamp = rs.getTimestamp("audit_timestamp");
            if (auditTimestamp != null) {
                entity.setAuditTimestamp(auditTimestamp.toLocalDateTime());
            }
            
            Timestamp createdDate = rs.getTimestamp("created_date");
            if (createdDate != null) {
                entity.setCreatedDate(createdDate.toLocalDateTime());
            }
            
            return entity;
        }
    };

    @Override
    public DataLoadAuditEntity save(DataLoadAuditEntity auditEntity) {
        try {
            // Call onCreate to initialize timestamps if not set
            auditEntity.onCreate();
            
            KeyHolder keyHolder = new GeneratedKeyHolder();
            
            int updated = jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(INSERT_AUDIT_SQL, new String[]{"audit_id"});
                int paramIndex = 1;
                
                ps.setString(paramIndex++, auditEntity.getConfigId());
                ps.setString(paramIndex++, auditEntity.getJobExecutionId());
                ps.setString(paramIndex++, auditEntity.getCorrelationId());
                ps.setString(paramIndex++, auditEntity.getAuditType() != null ? auditEntity.getAuditType().name() : null);
                ps.setString(paramIndex++, auditEntity.getEventName());
                ps.setString(paramIndex++, auditEntity.getEventDescription());
                ps.setString(paramIndex++, auditEntity.getSourceSystem());
                ps.setString(paramIndex++, auditEntity.getTargetTable());
                ps.setString(paramIndex++, auditEntity.getFileName());
                ps.setString(paramIndex++, auditEntity.getFilePath());
                
                setLongOrNull(ps, paramIndex++, auditEntity.getRecordCount());
                setLongOrNull(ps, paramIndex++, auditEntity.getProcessedCount());
                setLongOrNull(ps, paramIndex++, auditEntity.getErrorCount());
                
                ps.setString(paramIndex++, auditEntity.getDataSource());
                ps.setString(paramIndex++, auditEntity.getDataDestination());
                ps.setString(paramIndex++, auditEntity.getTransformationApplied());
                ps.setString(paramIndex++, auditEntity.getValidationRulesApplied());
                ps.setString(paramIndex++, auditEntity.getBusinessRulesApplied());
                
                setDoubleOrNull(ps, paramIndex++, auditEntity.getDataQualityScore());
                
                ps.setString(paramIndex++, auditEntity.getComplianceStatus() != null ? auditEntity.getComplianceStatus().name() : null);
                ps.setString(paramIndex++, auditEntity.getRegulatoryRequirement());
                setIntegerOrNull(ps, paramIndex++, auditEntity.getRetentionPeriodDays());
                ps.setString(paramIndex++, auditEntity.getPiiFields());
                ps.setString(paramIndex++, auditEntity.getSensitiveDataHash());
                ps.setString(paramIndex++, auditEntity.getEncryptionApplied());
                ps.setString(paramIndex++, auditEntity.getMaskingApplied());
                ps.setString(paramIndex++, auditEntity.getAccessControlApplied());
                ps.setString(paramIndex++, auditEntity.getUserId());
                ps.setString(paramIndex++, auditEntity.getSessionId());
                ps.setString(paramIndex++, auditEntity.getIpAddress());
                ps.setString(paramIndex++, auditEntity.getUserAgent());
                ps.setString(paramIndex++, auditEntity.getApplicationName());
                ps.setString(paramIndex++, auditEntity.getApplicationVersion());
                ps.setString(paramIndex++, auditEntity.getEnvironment());
                ps.setString(paramIndex++, auditEntity.getHostName());
                
                setLongOrNull(ps, paramIndex++, auditEntity.getProcessId());
                setLongOrNull(ps, paramIndex++, auditEntity.getThreadId());
                setLongOrNull(ps, paramIndex++, auditEntity.getExecutionTimeMs());
                setDoubleOrNull(ps, paramIndex++, auditEntity.getMemoryUsageMb());
                setDoubleOrNull(ps, paramIndex++, auditEntity.getCpuUsagePercent());
                
                ps.setString(paramIndex++, auditEntity.getErrorCode());
                ps.setString(paramIndex++, auditEntity.getErrorMessage());
                ps.setString(paramIndex++, auditEntity.getStackTrace());
                ps.setString(paramIndex++, auditEntity.getAdditionalMetadata());
                setLongOrNull(ps, paramIndex++, auditEntity.getParentAuditId());
                
                setTimestampOrNull(ps, paramIndex++, auditEntity.getAuditTimestamp());
                setTimestampOrNull(ps, paramIndex++, auditEntity.getCreatedDate());
                
                return ps;
            }, keyHolder);

            if (updated > 0 && keyHolder.getKey() != null) {
                auditEntity.setAuditId(keyHolder.getKey().longValue());
            }
            
            log.debug("Saved audit entry with ID: {}", auditEntity.getAuditId());
            return auditEntity;
            
        } catch (DataAccessException e) {
            log.error("Error saving audit entry: {}", e.getMessage());
            throw new RuntimeException("Failed to save audit entry", e);
        }
    }

    @Override
    public void deleteAll(List<DataLoadAuditEntity> auditEntries) {
        if (auditEntries == null || auditEntries.isEmpty()) {
            return;
        }

        try {
            for (DataLoadAuditEntity entity : auditEntries) {
                if (entity.getAuditId() != null) {
                    jdbcTemplate.update(DELETE_BY_IDS_SQL, entity.getAuditId());
                }
            }
            log.info("Deleted {} audit entries", auditEntries.size());
        } catch (DataAccessException e) {
            log.error("Error deleting audit entries: {}", e.getMessage());
            throw new RuntimeException("Failed to delete audit entries", e);
        }
    }

    @Override
    public List<DataLoadAuditEntity> findByCorrelationIdOrderByAuditTimestamp(String correlationId) {
        try {
            List<DataLoadAuditEntity> results = jdbcTemplate.query(FIND_BY_CORRELATION_ID_SQL, auditEntityRowMapper, correlationId);
            log.debug("Found {} audit entries for correlation ID: {}", results.size(), correlationId);
            return results;
        } catch (DataAccessException e) {
            log.error("Error finding audit entries by correlation ID {}: {}", correlationId, e.getMessage());
            throw new RuntimeException("Failed to find audit entries by correlation ID", e);
        }
    }

    @Override
    public List<DataLoadAuditEntity> findAuditEntriesForRetention(LocalDateTime retentionDate) {
        try {
            List<DataLoadAuditEntity> results = jdbcTemplate.query(FIND_FOR_RETENTION_SQL, auditEntityRowMapper, Timestamp.valueOf(retentionDate));
            log.debug("Found {} audit entries for retention before: {}", results.size(), retentionDate);
            return results;
        } catch (DataAccessException e) {
            log.error("Error finding audit entries for retention: {}", e.getMessage());
            throw new RuntimeException("Failed to find audit entries for retention", e);
        }
    }

    // Helper methods for setting nullable parameters
    private void setLongOrNull(PreparedStatement ps, int paramIndex, Long value) throws SQLException {
        if (value != null) {
            ps.setLong(paramIndex, value);
        } else {
            ps.setNull(paramIndex, java.sql.Types.BIGINT);
        }
    }

    private void setDoubleOrNull(PreparedStatement ps, int paramIndex, Double value) throws SQLException {
        if (value != null) {
            ps.setDouble(paramIndex, value);
        } else {
            ps.setNull(paramIndex, java.sql.Types.DOUBLE);
        }
    }

    private void setIntegerOrNull(PreparedStatement ps, int paramIndex, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(paramIndex, value);
        } else {
            ps.setNull(paramIndex, java.sql.Types.INTEGER);
        }
    }

    private void setTimestampOrNull(PreparedStatement ps, int paramIndex, LocalDateTime value) throws SQLException {
        if (value != null) {
            ps.setTimestamp(paramIndex, Timestamp.valueOf(value));
        } else {
            ps.setNull(paramIndex, java.sql.Types.TIMESTAMP);
        }
    }
}