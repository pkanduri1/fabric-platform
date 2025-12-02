package com.fabric.batch.repository;

import com.fabric.batch.entity.SqlLoaderSecurityAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SqlLoaderSecurityAuditEntity operations.
 * Provides comprehensive data access methods for SQL*Loader security audit tracking,
 * compliance monitoring, and regulatory reporting.
 */
@Repository
public interface SqlLoaderSecurityAuditRepository extends JpaRepository<SqlLoaderSecurityAuditEntity, Long> {
    
    /**
     * Find security audit events by correlation ID.
     */
    List<SqlLoaderSecurityAuditEntity> findByCorrelationIdOrderByAuditTimestampDesc(String correlationId);
    
    /**
     * Find security audit events by execution ID.
     */
    List<SqlLoaderSecurityAuditEntity> findByExecutionIdOrderByAuditTimestampDesc(String executionId);
    
    /**
     * Find security audit events by config ID.
     */
    List<SqlLoaderSecurityAuditEntity> findByConfigIdOrderByAuditTimestampDesc(String configId);
    
    /**
     * Find security audit events by event type.
     */
    List<SqlLoaderSecurityAuditEntity> findBySecurityEventTypeOrderByAuditTimestampDesc(
            SqlLoaderSecurityAuditEntity.SecurityEventType eventType);
    
    /**
     * Find security audit events by severity.
     */
    List<SqlLoaderSecurityAuditEntity> findBySeverityOrderByAuditTimestampDesc(
            SqlLoaderSecurityAuditEntity.Severity severity);
    
    /**
     * Find security audit events by user ID.
     */
    List<SqlLoaderSecurityAuditEntity> findByUserIdOrderByAuditTimestampDesc(String userId);
    
    /**
     * Find security audit events by compliance status.
     */
    List<SqlLoaderSecurityAuditEntity> findByComplianceStatusOrderByAuditTimestampDesc(
            SqlLoaderSecurityAuditEntity.ComplianceStatus complianceStatus);
    
    /**
     * Find high-severity security events.
     */
    @Query("SELECT s FROM SqlLoaderSecurityAuditEntity s WHERE s.severity IN ('HIGH', 'CRITICAL') ORDER BY s.auditTimestamp DESC")
    List<SqlLoaderSecurityAuditEntity> findHighSeverityEvents();
    
    /**
     * Find critical security events.
     */
    
    /**
     * Find security events with high risk scores.
     */
    @Query("SELECT s FROM SqlLoaderSecurityAuditEntity s WHERE s.riskScore >= :riskThreshold ORDER BY s.riskScore DESC, s.auditTimestamp DESC")
    List<SqlLoaderSecurityAuditEntity> findHighRiskEvents(@Param("riskThreshold") Integer riskThreshold);
    
    /**
     * Find security events requiring remediation.
     */
    List<SqlLoaderSecurityAuditEntity> findByRemediationRequiredOrderByAuditTimestampDesc(String remediationRequired);
    
    /**
     * Find security events that have been remediated.
     */
    @Query("SELECT s FROM SqlLoaderSecurityAuditEntity s WHERE s.complianceStatus = 'REMEDIATED' ORDER BY s.remediationDate DESC")
    List<SqlLoaderSecurityAuditEntity> findRemediatedEvents();
    
    /**
     * Find security events with PII exposure.
     */
    @Query("SELECT s FROM SqlLoaderSecurityAuditEntity s WHERE s.piiExposed IS NOT NULL AND s.piiExposed <> '' ORDER BY s.auditTimestamp DESC")
    List<SqlLoaderSecurityAuditEntity> findEventsWithPiiExposure();
    
    /**
     * Find security events with regulatory impact.
     */
    @Query("SELECT s FROM SqlLoaderSecurityAuditEntity s WHERE s.regulatoryImpact IS NOT NULL AND s.regulatoryImpact <> '' ORDER BY s.auditTimestamp DESC")
    List<SqlLoaderSecurityAuditEntity> findEventsWithRegulatoryImpact();
    
    /**
     * Find security events within date range.
     */
    List<SqlLoaderSecurityAuditEntity> findByAuditTimestampBetweenOrderByAuditTimestampDesc(
            LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find security events reported to security team.
     */
    List<SqlLoaderSecurityAuditEntity> findByReportedToSecurityOrderByAuditTimestampDesc(String reportedToSecurity);
    
    /**
     * Find security events with incident numbers.
     */
    @Query("SELECT s FROM SqlLoaderSecurityAuditEntity s WHERE s.incidentNumber IS NOT NULL AND s.incidentNumber <> '' ORDER BY s.auditTimestamp DESC")
    List<SqlLoaderSecurityAuditEntity> findEventsWithIncidentNumbers();
    
    /**
     * Find security events by IP address.
     */
    List<SqlLoaderSecurityAuditEntity> findByIpAddressOrderByAuditTimestampDesc(String ipAddress);
    
    /**
     * Find security events by session ID.
     */
    List<SqlLoaderSecurityAuditEntity> findBySessionIdOrderByAuditTimestampDesc(String sessionId);
    
    /**
     * Find security events by host name.
     */
    List<SqlLoaderSecurityAuditEntity> findByHostNameOrderByAuditTimestampDesc(String hostName);
    
    /**
     * Find non-compliant security events.
     */
    
    /**
     * Find security events requiring immediate attention.
     */
    @Query("SELECT s FROM SqlLoaderSecurityAuditEntity s WHERE " +
           "(s.severity = 'CRITICAL' OR s.riskScore >= 75 OR " +
           "s.securityEventType IN ('PRIVILEGE_ESCALATION', 'DATA_EXPOSURE') OR " +
           "s.complianceStatus = 'NON_COMPLIANT') " +
           "ORDER BY s.severity DESC, s.riskScore DESC, s.auditTimestamp DESC")
    List<SqlLoaderSecurityAuditEntity> findEventsRequiringImmediateAttention();
    
    /**
     * Find recent security events for monitoring.
     */
    @Query("SELECT s FROM SqlLoaderSecurityAuditEntity s WHERE s.auditTimestamp >= :recentThreshold ORDER BY s.auditTimestamp DESC")
    List<SqlLoaderSecurityAuditEntity> findRecentSecurityEvents(@Param("recentThreshold") LocalDateTime recentThreshold);
    
    /**
     * Count security events by event type.
     */
    @Query("SELECT COUNT(s) FROM SqlLoaderSecurityAuditEntity s WHERE s.securityEventType = :eventType")
    Long countBySecurityEventType(@Param("eventType") SqlLoaderSecurityAuditEntity.SecurityEventType eventType);
    
    /**
     * Count security events by severity.
     */
    @Query("SELECT COUNT(s) FROM SqlLoaderSecurityAuditEntity s WHERE s.severity = :severity")
    Long countBySeverity(@Param("severity") SqlLoaderSecurityAuditEntity.Severity severity);
    
    /**
     * Count security events by compliance status.
     */
    @Query("SELECT COUNT(s) FROM SqlLoaderSecurityAuditEntity s WHERE s.complianceStatus = :complianceStatus")
    Long countByComplianceStatus(@Param("complianceStatus") SqlLoaderSecurityAuditEntity.ComplianceStatus complianceStatus);
    
    /**
     * Count security events by user within date range.
     */
    @Query("SELECT COUNT(s) FROM SqlLoaderSecurityAuditEntity s WHERE s.userId = :userId AND s.auditTimestamp BETWEEN :startDate AND :endDate")
    Long countByUserIdAndDateRange(@Param("userId") String userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get security event statistics by event type.
     */
    @Query("SELECT s.securityEventType, COUNT(s), AVG(s.riskScore), " +
           "SUM(CASE WHEN s.severity = 'CRITICAL' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN s.remediationRequired = 'Y' THEN 1 ELSE 0 END) " +
           "FROM SqlLoaderSecurityAuditEntity s WHERE s.auditTimestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY s.securityEventType ORDER BY COUNT(s) DESC")
    List<Object[]> getSecurityEventStatsByType(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get security event statistics by user.
     */
    @Query("SELECT s.userId, COUNT(s), AVG(s.riskScore), " +
           "SUM(CASE WHEN s.severity IN ('HIGH', 'CRITICAL') THEN 1 ELSE 0 END), " +
           "MAX(s.auditTimestamp) " +
           "FROM SqlLoaderSecurityAuditEntity s WHERE s.auditTimestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY s.userId ORDER BY COUNT(s) DESC")
    List<Object[]> getSecurityEventStatsByUser(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get daily security event summary.
     */
    @Query("SELECT DATE(s.auditTimestamp), COUNT(s), " +
           "SUM(CASE WHEN s.severity = 'CRITICAL' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN s.severity = 'HIGH' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN s.complianceStatus = 'NON_COMPLIANT' THEN 1 ELSE 0 END), " +
           "AVG(s.riskScore) " +
           "FROM SqlLoaderSecurityAuditEntity s WHERE s.auditTimestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(s.auditTimestamp) ORDER BY DATE(s.auditTimestamp) DESC")
    List<Object[]> getDailySecurityEventSummary(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get compliance status summary.
     */
    @Query("SELECT s.complianceStatus, COUNT(s), AVG(s.riskScore), " +
           "SUM(CASE WHEN s.remediationRequired = 'Y' THEN 1 ELSE 0 END) " +
           "FROM SqlLoaderSecurityAuditEntity s WHERE s.auditTimestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY s.complianceStatus ORDER BY COUNT(s) DESC")
    List<Object[]> getComplianceStatusSummary(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find security events by affected tables.
     */
    @Query("SELECT s FROM SqlLoaderSecurityAuditEntity s WHERE s.affectedTables LIKE %:tableName% ORDER BY s.auditTimestamp DESC")
    List<SqlLoaderSecurityAuditEntity> findByAffectedTablesContaining(@Param("tableName") String tableName);
    
    /**
     * Find security events by affected fields.
     */
    @Query("SELECT s FROM SqlLoaderSecurityAuditEntity s WHERE s.affectedFields LIKE %:fieldName% ORDER BY s.auditTimestamp DESC")
    List<SqlLoaderSecurityAuditEntity> findByAffectedFieldsContaining(@Param("fieldName") String fieldName);
    
    /**
     * Find top risk score events for analysis.
     */
    @Query("SELECT s FROM SqlLoaderSecurityAuditEntity s WHERE s.auditTimestamp BETWEEN :startDate AND :endDate ORDER BY s.riskScore DESC")
    List<SqlLoaderSecurityAuditEntity> findTopRiskScoreEvents(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find security events with specific risk score range.
     */
    @Query("SELECT s FROM SqlLoaderSecurityAuditEntity s WHERE s.riskScore BETWEEN :minRisk AND :maxRisk ORDER BY s.riskScore DESC, s.auditTimestamp DESC")
    List<SqlLoaderSecurityAuditEntity> findByRiskScoreBetween(@Param("minRisk") Integer minRisk, @Param("maxRisk") Integer maxRisk);
    
    /**
     * Get risk distribution for security events.
     */
    @Query("SELECT " +
           "CASE " +
           "   WHEN s.riskScore < 25 THEN 'LOW' " +
           "   WHEN s.riskScore < 50 THEN 'MEDIUM' " +
           "   WHEN s.riskScore < 75 THEN 'HIGH' " +
           "   ELSE 'CRITICAL' " +
           "END, COUNT(s) " +
           "FROM SqlLoaderSecurityAuditEntity s WHERE s.auditTimestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY CASE " +
           "   WHEN s.riskScore < 25 THEN 'LOW' " +
           "   WHEN s.riskScore < 50 THEN 'MEDIUM' " +
           "   WHEN s.riskScore < 75 THEN 'HIGH' " +
           "   ELSE 'CRITICAL' " +
           "END ORDER BY MIN(s.riskScore)")
    List<Object[]> getRiskDistribution(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find unresolved security events (not remediated and requiring attention).
     */
    @Query("SELECT s FROM SqlLoaderSecurityAuditEntity s WHERE " +
           "s.complianceStatus NOT IN ('REMEDIATED', 'EXCEPTION_APPROVED') " +
           "AND (s.severity IN ('HIGH', 'CRITICAL') OR s.riskScore >= 70 OR s.remediationRequired = 'Y') " +
           "ORDER BY s.severity DESC, s.riskScore DESC, s.auditTimestamp")
    List<SqlLoaderSecurityAuditEntity> findUnresolvedSecurityEvents();
    
    /**
     * Find security events by incident number.
     */
    Optional<SqlLoaderSecurityAuditEntity> findByIncidentNumber(String incidentNumber);
    
    /**
     * Find security events for specific user and IP combination.
     */
    List<SqlLoaderSecurityAuditEntity> findByUserIdAndIpAddressOrderByAuditTimestampDesc(String userId, String ipAddress);
    
    /**
     * Find suspicious activity patterns.
     */
    @Query("SELECT s.userId, s.ipAddress, COUNT(s), MAX(s.auditTimestamp) " +
           "FROM SqlLoaderSecurityAuditEntity s WHERE " +
           "s.securityEventType IN ('SUSPICIOUS_ACTIVITY', 'PRIVILEGE_ESCALATION', 'AUTHENTICATION_FAILURE', 'AUTHORIZATION_FAILURE') " +
           "AND s.auditTimestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY s.userId, s.ipAddress " +
           "HAVING COUNT(s) > :threshold " +
           "ORDER BY COUNT(s) DESC")
    List<Object[]> findSuspiciousActivityPatterns(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate, 
            @Param("threshold") Integer threshold);
    
    /**
     * Get security metrics for dashboard.
     */
    @Query("SELECT " +
           "COUNT(s), " +
           "SUM(CASE WHEN s.severity = 'CRITICAL' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN s.severity = 'HIGH' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN s.complianceStatus = 'NON_COMPLIANT' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN s.remediationRequired = 'Y' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN s.piiExposed IS NOT NULL AND s.piiExposed <> '' THEN 1 ELSE 0 END), " +
           "AVG(s.riskScore) " +
           "FROM SqlLoaderSecurityAuditEntity s WHERE s.auditTimestamp BETWEEN :startDate AND :endDate")
    Object[] getSecurityMetricsSummary(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find events requiring security team notification.
     */
    @Query("SELECT s FROM SqlLoaderSecurityAuditEntity s WHERE " +
           "s.securityTeamNotified = 'N' " +
           "AND (s.severity = 'CRITICAL' OR s.riskScore >= 80 OR " +
           "s.securityEventType IN ('PRIVILEGE_ESCALATION', 'DATA_EXPOSURE', 'COMPLIANCE_VIOLATION')) " +
           "ORDER BY s.severity DESC, s.riskScore DESC, s.auditTimestamp")
    List<SqlLoaderSecurityAuditEntity> findEventsRequiringSecurityNotification();
    
    /**
     * Find the latest security event by correlation ID.
     */
    @Query("SELECT s FROM SqlLoaderSecurityAuditEntity s WHERE s.correlationId = :correlationId ORDER BY s.auditTimestamp DESC LIMIT 1")
    Optional<SqlLoaderSecurityAuditEntity> findLatestByCorrelationId(@Param("correlationId") String correlationId);
    
    /**
     * Delete old security audit records (for data retention compliance).
     */
    @Query("DELETE FROM SqlLoaderSecurityAuditEntity s WHERE s.auditTimestamp < :retentionThreshold AND s.complianceStatus = 'REMEDIATED'")
    void deleteOldRemediatedAuditRecords(@Param("retentionThreshold") LocalDateTime retentionThreshold);
    
    /**
     * Count events by config ID and event type within date range.
     */
    @Query("SELECT COUNT(s) FROM SqlLoaderSecurityAuditEntity s WHERE s.configId = :configId AND s.securityEventType = :eventType AND s.auditTimestamp BETWEEN :startDate AND :endDate")
    Long countByConfigIdAndEventTypeAndDateRange(
            @Param("configId") String configId, 
            @Param("eventType") SqlLoaderSecurityAuditEntity.SecurityEventType eventType,
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);
}