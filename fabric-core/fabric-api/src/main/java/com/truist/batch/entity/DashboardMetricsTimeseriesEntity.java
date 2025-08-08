package com.truist.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * US008: Dashboard Metrics Time-Series Entity
 * 
 * Time-series entity for storing real-time monitoring metrics with:
 * - Automatic partitioning by timestamp for performance
 * - Comprehensive performance and business metrics
 * - Security and compliance tracking fields
 * - Tamper-evident audit hash validation
 * - Optimized indexing for real-time queries
 * 
 * APPROVED FOR IMPLEMENTATION by Principal Enterprise Architect
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@Entity
@Table(name = "DASHBOARD_METRICS_TIMESERIES",
       indexes = {
           @Index(name = "idx_metrics_ts_realtime", 
                  columnList = "metric_timestamp DESC, metric_type, execution_id"),
           @Index(name = "idx_metrics_execution_lookup", 
                  columnList = "execution_id, metric_timestamp DESC, metric_type"),
           @Index(name = "idx_metrics_dashboard_query", 
                  columnList = "metric_type, metric_status, metric_timestamp DESC"),
           @Index(name = "idx_metrics_correlation_tracking", 
                  columnList = "correlation_id, user_id, created_date DESC"),
           @Index(name = "idx_metrics_security_audit", 
                  columnList = "audit_hash, compliance_flags, security_classification")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsTimeseriesEntity {

    // ============================================================================
    // PRIMARY KEY AND SEQUENCE
    // ============================================================================
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_dashboard_metrics_timeseries")
    @SequenceGenerator(name = "seq_dashboard_metrics_timeseries", 
                       sequenceName = "SEQ_DASHBOARD_METRICS_TIMESERIES", 
                       allocationSize = 1)
    @Column(name = "metric_id")
    private Long metricId;

    // ============================================================================
    // TIME-SERIES CORE FIELDS
    // ============================================================================
    
    @NotNull(message = "Metric timestamp is required")
    @Column(name = "metric_timestamp", nullable = false)
    private Instant metricTimestamp;
    
    @NotBlank(message = "Execution ID is required")
    @Column(name = "execution_id", nullable = false, length = 100)
    private String executionId;
    
    @NotBlank(message = "Metric type is required")
    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType; // SYSTEM_PERFORMANCE, BUSINESS_KPI, SECURITY_EVENT, COMPLIANCE_METRIC
    
    @NotBlank(message = "Metric name is required")
    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    // ============================================================================
    // METRIC VALUES AND UNITS
    // ============================================================================
    
    @Column(name = "metric_value", precision = 15, scale = 4)
    private BigDecimal metricValue;
    
    @Column(name = "metric_unit", length = 20)
    private String metricUnit; // ms, MB, %, count, rate, etc.
    
    @Column(name = "metric_status", length = 20)
    private String metricStatus; // NORMAL, WARNING, CRITICAL, UNKNOWN

    // ============================================================================
    // SOURCE AND CONTEXT
    // ============================================================================
    
    @Column(name = "source_system", length = 50)
    private String sourceSystem;
    
    @Column(name = "thread_id", length = 50)
    private String threadId;
    
    @Column(name = "partition_id", length = 20)
    private String partitionId;
    
    @Column(name = "user_id", length = 100)
    private String userId;

    // ============================================================================
    // PERFORMANCE AND RESOURCE METRICS
    // ============================================================================
    
    @DecimalMin(value = "0.00", message = "CPU usage cannot be negative")
    @DecimalMax(value = "100.00", message = "CPU usage cannot exceed 100%")
    @Column(name = "cpu_usage_percent", precision = 5, scale = 2)
    private BigDecimal cpuUsagePercent;
    
    @Min(value = 0, message = "Memory usage cannot be negative")
    @Column(name = "memory_usage_mb")
    private Long memoryUsageMb;
    
    @Min(value = 0, message = "Processing duration cannot be negative")
    @Column(name = "processing_duration_ms")
    private Long processingDurationMs;
    
    @DecimalMin(value = "0.0000", message = "Throughput cannot be negative")
    @Column(name = "throughput_per_second", precision = 15, scale = 4)
    private BigDecimal throughputPerSecond;
    
    @DecimalMin(value = "0.00", message = "Success rate cannot be negative")
    @DecimalMax(value = "100.00", message = "Success rate cannot exceed 100%")
    @Column(name = "success_rate_percent", precision = 5, scale = 2)
    private BigDecimal successRatePercent;

    // ============================================================================
    // SECURITY AND COMPLIANCE
    // ============================================================================
    
    @NotBlank(message = "Audit hash is required for tamper detection")
    @Column(name = "audit_hash", nullable = false, length = 64)
    private String auditHash; // SHA-256 hash for tamper detection
    
    @Column(name = "compliance_flags", length = 200)
    private String complianceFlags; // Comma-separated flags
    
    @Column(name = "security_classification", length = 50)
    @Builder.Default
    private String securityClassification = "INTERNAL";
    
    @DecimalMin(value = "0.00", message = "Data quality score cannot be negative")
    @DecimalMax(value = "100.00", message = "Data quality score cannot exceed 100")
    @Column(name = "data_quality_score", precision = 5, scale = 2)
    private BigDecimal dataQualityScore;

    // ============================================================================
    // CORRELATION AND TRACKING
    // ============================================================================
    
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    // ============================================================================
    // STANDARD AUDIT FIELDS
    // ============================================================================
    
    @NotNull(message = "Created date is required")
    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private Instant createdDate = Instant.now();
    
    @NotBlank(message = "Created by is required")
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    
    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;
    
    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;
    
    @Column(name = "version_number")
    @Builder.Default
    private Long versionNumber = 1L;

    // ============================================================================
    // BUSINESS LOGIC METHODS
    // ============================================================================

    /**
     * Check if metric indicates a warning condition
     */
    public boolean isWarning() {
        return "WARNING".equals(metricStatus);
    }

    /**
     * Check if metric indicates a critical condition
     */
    public boolean isCritical() {
        return "CRITICAL".equals(metricStatus);
    }

    /**
     * Check if metric is in normal state
     */
    public boolean isNormal() {
        return "NORMAL".equals(metricStatus);
    }

    /**
     * Check if this is a system performance metric
     */
    public boolean isSystemMetric() {
        return "SYSTEM_PERFORMANCE".equals(metricType);
    }

    /**
     * Check if this is a business KPI metric
     */
    public boolean isBusinessMetric() {
        return "BUSINESS_KPI".equals(metricType);
    }

    /**
     * Check if this is a security event metric
     */
    public boolean isSecurityMetric() {
        return "SECURITY_EVENT".equals(metricType);
    }

    /**
     * Check if this is a compliance metric
     */
    public boolean isComplianceMetric() {
        return "COMPLIANCE_METRIC".equals(metricType);
    }

    /**
     * Get metric age in minutes
     */
    public long getMetricAgeMinutes() {
        if (metricTimestamp == null) {
            return 0;
        }
        return java.time.Duration.between(metricTimestamp, Instant.now()).toMinutes();
    }

    /**
     * Check if metric is recent (within last 5 minutes)
     */
    public boolean isRecentMetric() {
        return getMetricAgeMinutes() <= 5;
    }

    /**
     * Check if metric has good data quality
     */
    public boolean hasGoodDataQuality() {
        return dataQualityScore != null && 
               dataQualityScore.compareTo(BigDecimal.valueOf(85.0)) >= 0;
    }

    /**
     * Get formatted metric value with unit
     */
    public String getFormattedValue() {
        if (metricValue == null) {
            return "N/A";
        }
        
        String unit = metricUnit != null ? metricUnit : "";
        return String.format("%.2f %s", metricValue.doubleValue(), unit).trim();
    }

    /**
     * Check if performance is acceptable based on thresholds
     */
    public boolean isPerformanceAcceptable() {
        // CPU usage threshold
        if (cpuUsagePercent != null && cpuUsagePercent.compareTo(BigDecimal.valueOf(90.0)) > 0) {
            return false;
        }
        
        // Memory usage threshold (assuming 1GB max)
        if (memoryUsageMb != null && memoryUsageMb > 1000) {
            return false;
        }
        
        // Processing duration threshold (5 seconds)
        if (processingDurationMs != null && processingDurationMs > 5000) {
            return false;
        }
        
        // Success rate threshold
        if (successRatePercent != null && successRatePercent.compareTo(BigDecimal.valueOf(95.0)) < 0) {
            return false;
        }
        
        return true;
    }

    /**
     * Update metric status based on current values
     */
    public void updateMetricStatus() {
        if (!isPerformanceAcceptable()) {
            this.metricStatus = "CRITICAL";
        } else if (isAtWarningThreshold()) {
            this.metricStatus = "WARNING";
        } else {
            this.metricStatus = "NORMAL";
        }
    }

    private boolean isAtWarningThreshold() {
        // CPU warning at 80%
        if (cpuUsagePercent != null && cpuUsagePercent.compareTo(BigDecimal.valueOf(80.0)) > 0) {
            return true;
        }
        
        // Memory warning at 800MB
        if (memoryUsageMb != null && memoryUsageMb > 800) {
            return true;
        }
        
        // Processing duration warning at 3 seconds
        if (processingDurationMs != null && processingDurationMs > 3000) {
            return true;
        }
        
        // Success rate warning at 98%
        if (successRatePercent != null && successRatePercent.compareTo(BigDecimal.valueOf(98.0)) < 0) {
            return true;
        }
        
        return false;
    }

    /**
     * Add compliance flag
     */
    public void addComplianceFlag(String flag) {
        if (complianceFlags == null || complianceFlags.trim().isEmpty()) {
            this.complianceFlags = flag;
        } else if (!complianceFlags.contains(flag)) {
            this.complianceFlags = complianceFlags + "," + flag;
        }
    }

    /**
     * Check if has specific compliance flag
     */
    public boolean hasComplianceFlag(String flag) {
        return complianceFlags != null && complianceFlags.contains(flag);
    }

    /**
     * Validate audit hash integrity
     */
    public boolean validateHashIntegrity(String expectedHash) {
        return this.auditHash != null && this.auditHash.equals(expectedHash);
    }

    /**
     * Create metric summary for dashboard display
     */
    public MetricSummary createSummary() {
        return MetricSummary.builder()
                .metricId(metricId)
                .executionId(executionId)
                .metricType(metricType)
                .metricName(metricName)
                .metricValue(metricValue)
                .metricUnit(metricUnit)
                .metricStatus(metricStatus)
                .metricTimestamp(metricTimestamp)
                .formattedValue(getFormattedValue())
                .isRecent(isRecentMetric())
                .hasGoodQuality(hasGoodDataQuality())
                .performanceAcceptable(isPerformanceAcceptable())
                .build();
    }

    // ============================================================================
    // VALIDATION METHODS
    // ============================================================================

    @PrePersist
    protected void onCreate() {
        if (metricTimestamp == null) {
            metricTimestamp = Instant.now();
        }
        if (createdDate == null) {
            createdDate = Instant.now();
        }
        
        // Auto-update metric status
        updateMetricStatus();
        
        // Validate enum values
        validateMetricType();
        validateMetricStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedDate = Instant.now();
        this.versionNumber = this.versionNumber + 1;
        
        // Auto-update metric status
        updateMetricStatus();
        
        // Validate enum values
        validateMetricType();
        validateMetricStatus();
    }

    private void validateMetricType() {
        if (metricType != null && 
            !java.util.Arrays.asList("SYSTEM_PERFORMANCE", "BUSINESS_KPI", "SECURITY_EVENT", "COMPLIANCE_METRIC")
                    .contains(metricType)) {
            throw new IllegalArgumentException("Invalid metric type: " + metricType);
        }
    }

    private void validateMetricStatus() {
        if (metricStatus != null && 
            !java.util.Arrays.asList("NORMAL", "WARNING", "CRITICAL", "UNKNOWN").contains(metricStatus)) {
            throw new IllegalArgumentException("Invalid metric status: " + metricStatus);
        }
    }

    // ============================================================================
    // NESTED CLASSES
    // ============================================================================

    /**
     * Metric summary for dashboard display
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MetricSummary {
        private Long metricId;
        private String executionId;
        private String metricType;
        private String metricName;
        private BigDecimal metricValue;
        private String metricUnit;
        private String metricStatus;
        private Instant metricTimestamp;
        private String formattedValue;
        private boolean isRecent;
        private boolean hasGoodQuality;
        private boolean performanceAcceptable;
    }

    // ============================================================================
    // OBJECT METHODS
    // ============================================================================

    @Override
    public String toString() {
        return String.format("DashboardMetric[id=%d, type=%s, name=%s, value=%s, status=%s, timestamp=%s]",
                metricId, metricType, metricName, getFormattedValue(), metricStatus, metricTimestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DashboardMetricsTimeseriesEntity that = (DashboardMetricsTimeseriesEntity) obj;
        
        if (metricId != null) {
            return metricId.equals(that.metricId);
        }
        
        // For unsaved entities, use combination of execution_id, metric_name, and timestamp
        return java.util.Objects.equals(executionId, that.executionId) &&
               java.util.Objects.equals(metricName, that.metricName) &&
               java.util.Objects.equals(metricTimestamp, that.metricTimestamp);
    }

    @Override
    public int hashCode() {
        return metricId != null ? metricId.hashCode() : 
               java.util.Objects.hash(executionId, metricName, metricTimestamp);
    }
}