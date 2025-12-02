package com.fabric.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * US008: Dashboard Metrics Time-Series POJO
 * 
 * Simplified POJO for storing real-time monitoring metrics to eliminate JPA dependencies.
 * Full implementation will be provided when dashboard metrics functionality is required.
 * 
 * Converted from JPA Entity to simple POJO to eliminate JPA dependencies
 * 
 * TODO: Implement full dashboard metrics fields when needed
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsTimeseriesEntity {

    // ============================================================================
    // PRIMARY KEY
    // ============================================================================
    
    private Long metricsId;

    // ============================================================================
    // METRIC IDENTIFICATION
    // ============================================================================
    
    private String executionId;
    private String metricName;
    private String metricType;
    private BigDecimal metricValue;
    private String metricUnit;
    private String metricStatus;

    // ============================================================================
    // TIMING
    // ============================================================================
    
    private Instant metricTimestamp;

    // ============================================================================
    // PERFORMANCE METRICS (Simplified)
    // ============================================================================
    
    private BigDecimal cpuUsagePercent;
    private Long memoryUsageMb;
    private Long processingDurationMs;
    private BigDecimal throughputPerSecond;
    private BigDecimal successRatePercent;

    // ============================================================================
    // TRACKING AND CORRELATION (Simplified)
    // ============================================================================
    
    private String correlationId;
    private String sourceSystem;
    private String userId;
    private String threadId;

    // ============================================================================
    // AUDIT AND COMPLIANCE (Simplified)
    // ============================================================================
    
    private String auditHash;
    private String complianceFlags;
    private String securityClassification;
    private BigDecimal dataQualityScore;

    // ============================================================================
    // METADATA
    // ============================================================================
    
    private Instant createdDate;
    private String partitionId;

    // TODO: Add remaining fields from original entity when full implementation is needed
    // This simplified version allows the application to start without JPA dependencies
}