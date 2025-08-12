package com.truist.batch.repository;

import com.truist.batch.entity.DataLoadAuditEntity;
import java.time.LocalDateTime;
import java.util.List;

/**
 * JdbcTemplate-based repository interface for DataLoadAuditEntity operations.
 * Provides essential data access methods for audit trail and compliance reporting.
 * This interface contains only the methods actually used by AuditTrailManager.
 */
public interface DataLoadAuditRepositoryJdbc {
    
    /**
     * Save audit entry.
     */
    DataLoadAuditEntity save(DataLoadAuditEntity auditEntity);
    
    /**
     * Delete audit entries.
     */
    void deleteAll(List<DataLoadAuditEntity> auditEntries);
    
    /**
     * Find audit entries by correlation ID for complete data lineage tracking.
     */
    List<DataLoadAuditEntity> findByCorrelationIdOrderByAuditTimestamp(String correlationId);
    
    /**
     * Find audit entries for retention policy enforcement.
     */
    List<DataLoadAuditEntity> findAuditEntriesForRetention(LocalDateTime retentionDate);
}