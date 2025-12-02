package com.fabric.batch.repository;

import com.fabric.batch.entity.DataLoadAuditEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Bridge repository that provides a unified interface for audit operations.
 * This ensures compatibility between JPA and JdbcTemplate implementations.
 */
@Slf4j
@Component
public class DataLoadAuditRepositoryBridge {

    @Autowired(required = false)
    private DataLoadAuditRepository jpaRepository;

    @Autowired(required = false) 
    private DataLoadAuditRepositoryJdbc jdbcRepository;

    public DataLoadAuditEntity save(DataLoadAuditEntity auditEntity) {
        try {
            if (jdbcRepository != null) {
                log.debug("Using JdbcTemplate repository for save operation");
                return jdbcRepository.save(auditEntity);
            } else if (jpaRepository != null) {
                log.debug("Using JPA repository for save operation");
                return jpaRepository.save(auditEntity);
            } else {
                log.warn("No audit repository available - audit operation will be skipped");
                return auditEntity;
            }
        } catch (Exception e) {
            log.error("Error saving audit entry: {}", e.getMessage());
            return auditEntity;
        }
    }

    public void deleteAll(List<DataLoadAuditEntity> auditEntries) {
        try {
            if (jdbcRepository != null) {
                log.debug("Using JdbcTemplate repository for deleteAll operation");
                jdbcRepository.deleteAll(auditEntries);
            } else if (jpaRepository != null) {
                log.debug("Using JPA repository for deleteAll operation");
                jpaRepository.deleteAll(auditEntries);
            } else {
                log.warn("No audit repository available - deleteAll operation will be skipped");
            }
        } catch (Exception e) {
            log.error("Error deleting audit entries: {}", e.getMessage());
        }
    }

    public List<DataLoadAuditEntity> findByCorrelationIdOrderByAuditTimestamp(String correlationId) {
        try {
            if (jdbcRepository != null) {
                log.debug("Using JdbcTemplate repository for findByCorrelationId operation");
                return jdbcRepository.findByCorrelationIdOrderByAuditTimestamp(correlationId);
            } else if (jpaRepository != null) {
                log.debug("Using JPA repository for findByCorrelationId operation");
                return jpaRepository.findByCorrelationIdOrderByAuditTimestamp(correlationId);
            } else {
                log.warn("No audit repository available - returning empty list");
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error finding audit entries by correlation ID: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<DataLoadAuditEntity> findAuditEntriesForRetention(LocalDateTime retentionDate) {
        try {
            if (jdbcRepository != null) {
                log.debug("Using JdbcTemplate repository for findAuditEntriesForRetention operation");
                return jdbcRepository.findAuditEntriesForRetention(retentionDate);
            } else if (jpaRepository != null) {
                log.debug("Using JPA repository for findAuditEntriesForRetention operation");
                return jpaRepository.findAuditEntriesForRetention(retentionDate);
            } else {
                log.warn("No audit repository available - returning empty list");
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error finding audit entries for retention: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}