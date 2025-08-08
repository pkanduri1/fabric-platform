package com.truist.batch.repository;

import com.truist.batch.entity.ExecutionAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Epic 2: Repository interface for ExecutionAuditEntity providing
 * comprehensive audit trail management and compliance reporting capabilities.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 2 - Simple Transaction Processing
 */
@Repository
public interface ExecutionAuditRepository extends JpaRepository<ExecutionAuditEntity, Long> {

    /**
     * Find all audit events for a specific execution
     */
    @Query("SELECT ea FROM ExecutionAuditEntity ea " +
           "WHERE ea.executionId = :executionId " +
           "ORDER BY ea.eventTimestamp ASC")
    List<ExecutionAuditEntity> findByExecutionIdOrderByEventTimestamp(
            @Param("executionId") String executionId);

    /**
     * Find audit events by correlation ID for tracing
     */
    @Query("SELECT ea FROM ExecutionAuditEntity ea " +
           "WHERE ea.correlationId = :correlationId " +
           "ORDER BY ea.eventTimestamp ASC")
    List<ExecutionAuditEntity> findByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Find security events for compliance monitoring
     */
    @Query("SELECT ea FROM ExecutionAuditEntity ea " +
           "WHERE ea.eventType IN ('SECURITY_EVENT', 'USER_ACCESS', 'CONFIG_CHANGE') " +
           "AND ea.eventTimestamp >= :sinceDate " +
           "ORDER BY ea.eventTimestamp DESC")
    List<ExecutionAuditEntity> findSecurityEventsSince(@Param("sinceDate") Instant sinceDate);

    /**
     * Find failed events for error analysis
     */
    @Query("SELECT ea FROM ExecutionAuditEntity ea " +
           "WHERE ea.successFlag = 'N' " +
           "AND ea.eventTimestamp >= :sinceDate " +
           "ORDER BY ea.eventTimestamp DESC")
    List<ExecutionAuditEntity> findFailedEventsSince(@Param("sinceDate") Instant sinceDate);

    /**
     * Get compliance audit records for regulatory reporting
     */
    @Query("SELECT ea FROM ExecutionAuditEntity ea " +
           "WHERE ea.complianceFlags IS NOT NULL " +
           "AND ea.businessDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ea.eventTimestamp DESC")
    List<ExecutionAuditEntity> findComplianceEventsByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}