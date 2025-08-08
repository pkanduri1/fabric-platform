package com.truist.batch.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Epic 3: Temporary Staging Performance Monitoring Entity
 * 
 * JPA entity for tracking performance metrics, optimization activities,
 * and operational analytics for temporary staging tables. Supports
 * banking-grade performance monitoring with detailed metrics collection
 * and historical trend analysis.
 * 
 * Features:
 * - Real-time performance metric tracking
 * - Optimization history and effectiveness analysis
 * - Resource utilization monitoring (CPU, memory, I/O)
 * - Performance bottleneck identification
 * - Banking compliance audit trails
 * - Historical trend analysis and reporting
 * 
 * Performance Specifications:
 * - Support for high-frequency metric updates (1000+ updates/sec)
 * - Efficient storage with compression for historical data
 * - Sub-second query response for dashboard analytics
 * - Automatic metric aggregation and rollup
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 3 - Complex Transaction Processing
 */
@Entity
@Table(name = "TEMP_STAGING_PERFORMANCE", schema = "CM3INT",
       indexes = {
           @Index(name = "idx_tsp_staging_def_id", columnList = "staging_def_id"),
           @Index(name = "idx_tsp_measurement_timestamp", columnList = "measurement_timestamp"),
           @Index(name = "idx_tsp_execution_id", columnList = "execution_id"),
           @Index(name = "idx_tsp_performance_type", columnList = "performance_measurement_type"),
           @Index(name = "idx_tsp_composite_lookup", columnList = "staging_def_id, measurement_timestamp, performance_measurement_type")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TempStagingPerformanceEntity {

    /**
     * Performance measurement types
     */
    public enum PerformanceMeasurementType {
        TABLE_CREATION,      // Table creation performance
        INDEX_CREATION,      // Index creation performance
        DATA_INSERTION,      // Data insertion performance
        QUERY_EXECUTION,     // Query execution performance
        OPTIMIZATION_APPLIED, // Optimization application
        CLEANUP_EXECUTION,   // Cleanup operation performance
        COMPRESSION_APPLIED, // Compression performance
        ENCRYPTION_APPLIED,  // Encryption performance
        PARTITION_ANALYSIS,  // Partition performance analysis
        MEMORY_UTILIZATION,  // Memory usage metrics
        IO_METRICS,          // I/O performance metrics
        CPU_UTILIZATION      // CPU usage metrics
    }

    /**
     * Optimization effectiveness levels
     */
    public enum OptimizationEffectiveness {
        HIGHLY_EFFECTIVE,    // > 50% performance improvement
        MODERATELY_EFFECTIVE, // 20-50% performance improvement
        SLIGHTLY_EFFECTIVE,   // 5-20% performance improvement
        NO_IMPROVEMENT,      // < 5% improvement
        DEGRADED            // Performance degraded
    }

    @Id
    @Column(name = "performance_id", nullable = false)
    @SequenceGenerator(name = "seq_temp_staging_performance", 
                      sequenceName = "CM3INT.seq_temp_staging_performance", 
                      allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, 
                   generator = "seq_temp_staging_performance")
    private Long performanceId;

    @Column(name = "staging_def_id", nullable = false)
    private Long stagingDefId;

    @Column(name = "execution_id", nullable = false, length = 100)
    private String executionId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "performance_measurement_type", nullable = false, length = 50)
    private PerformanceMeasurementType performanceMeasurementType;

    @Column(name = "measurement_timestamp", nullable = false)
    private LocalDateTime measurementTimestamp = LocalDateTime.now();

    // Performance metrics
    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "records_processed")
    private Long recordsProcessed;

    @Column(name = "throughput_records_per_sec", precision = 12, scale = 2)
    private BigDecimal throughputRecordsPerSec;

    @Column(name = "memory_used_mb")
    private Integer memoryUsedMb;

    @Column(name = "cpu_usage_percent", precision = 5, scale = 2)
    private BigDecimal cpuUsagePercent;

    @Column(name = "io_read_mb")
    private Integer ioReadMb;

    @Column(name = "io_write_mb")
    private Integer ioWriteMb;

    @Column(name = "table_size_before_mb")
    private Integer tableSizeBeforeMb;

    @Column(name = "table_size_after_mb")
    private Integer tableSizeAfterMb;

    @Column(name = "compression_ratio", precision = 5, scale = 2)
    private BigDecimal compressionRatio;

    // Optimization tracking
    @Column(name = "optimization_applied", length = 500)
    private String optimizationApplied;

    @Enumerated(EnumType.STRING)
    @Column(name = "optimization_effectiveness", length = 30)
    private OptimizationEffectiveness optimizationEffectiveness;

    @Column(name = "performance_improvement_percent", precision = 5, scale = 2)
    private BigDecimal performanceImprovementPercent;

    @Column(name = "bottleneck_identified", length = 500)
    private String bottleneckIdentified;

    @Column(name = "recommendation", length = 1000)
    private String recommendation;

    // Resource utilization details (JSON format)
    @Lob
    @Column(name = "detailed_metrics_json")
    private String detailedMetricsJson;

    // Error tracking
    @Column(name = "error_occurred", length = 1)
    private String errorOccurred = "N";

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    // Audit and compliance
    @Column(name = "business_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private java.util.Date businessDate = new java.util.Date();

    @Column(name = "created_timestamp", nullable = false)
    private LocalDateTime createdTimestamp = LocalDateTime.now();

    @Column(name = "monitoring_source", length = 50)
    private String monitoringSource = "TEMP_STAGING_MANAGER";

    /**
     * Pre-persist callback
     */
    @PrePersist
    protected void onCreate() {
        if (measurementTimestamp == null) {
            measurementTimestamp = LocalDateTime.now();
        }
        if (createdTimestamp == null) {
            createdTimestamp = LocalDateTime.now();
        }
        if (businessDate == null) {
            businessDate = new java.util.Date();
        }
        if (errorOccurred == null) {
            errorOccurred = "N";
        }
        if (monitoringSource == null) {
            monitoringSource = "TEMP_STAGING_MANAGER";
        }
    }

    /**
     * Business logic helper methods
     */

    /**
     * Check if this measurement represents an error condition
     * @return true if error occurred
     */
    public boolean hasError() {
        return "Y".equals(errorOccurred);
    }

    /**
     * Check if optimization was applied
     * @return true if optimization was applied
     */
    public boolean wasOptimizationApplied() {
        return optimizationApplied != null && !optimizationApplied.trim().isEmpty();
    }

    /**
     * Check if performance improved significantly
     * @return true if performance improved by more than 20%
     */
    public boolean hasSignificantPerformanceImprovement() {
        return performanceImprovementPercent != null && 
               performanceImprovementPercent.compareTo(BigDecimal.valueOf(20.0)) > 0;
    }

    /**
     * Calculate records per second if not already calculated
     * @return calculated throughput
     */
    public BigDecimal calculateThroughput() {
        if (throughputRecordsPerSec != null) {
            return throughputRecordsPerSec;
        }
        
        if (recordsProcessed != null && durationMs != null && durationMs > 0) {
            BigDecimal seconds = BigDecimal.valueOf(durationMs).divide(BigDecimal.valueOf(1000));
            return BigDecimal.valueOf(recordsProcessed).divide(seconds, 2, java.math.RoundingMode.HALF_UP);
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * Calculate compression effectiveness
     * @return compression percentage saved
     */
    public BigDecimal calculateCompressionSavings() {
        if (tableSizeBeforeMb != null && tableSizeAfterMb != null && tableSizeBeforeMb > 0) {
            BigDecimal before = BigDecimal.valueOf(tableSizeBeforeMb);
            BigDecimal after = BigDecimal.valueOf(tableSizeAfterMb);
            BigDecimal savings = before.subtract(after);
            
            return savings.divide(before, 4, java.math.RoundingMode.HALF_UP)
                         .multiply(BigDecimal.valueOf(100));
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * Determine performance category based on metrics
     * @return performance category
     */
    public String getPerformanceCategory() {
        if (hasError()) {
            return "ERROR";
        }
        
        if (performanceImprovementPercent != null) {
            if (performanceImprovementPercent.compareTo(BigDecimal.valueOf(50)) > 0) {
                return "EXCELLENT";
            } else if (performanceImprovementPercent.compareTo(BigDecimal.valueOf(20)) > 0) {
                return "GOOD";
            } else if (performanceImprovementPercent.compareTo(BigDecimal.valueOf(5)) > 0) {
                return "FAIR";
            } else if (performanceImprovementPercent.compareTo(BigDecimal.ZERO) < 0) {
                return "DEGRADED";
            }
        }
        
        // Performance category based on throughput for data operations
        if (throughputRecordsPerSec != null && recordsProcessed != null && recordsProcessed > 1000) {
            if (throughputRecordsPerSec.compareTo(BigDecimal.valueOf(10000)) > 0) {
                return "HIGH_PERFORMANCE";
            } else if (throughputRecordsPerSec.compareTo(BigDecimal.valueOf(1000)) > 0) {
                return "NORMAL_PERFORMANCE";
            } else {
                return "LOW_PERFORMANCE";
            }
        }
        
        return "BASELINE";
    }

    /**
     * Check if resource usage is high
     * @return true if high resource usage detected
     */
    public boolean hasHighResourceUsage() {
        if (memoryUsedMb != null && memoryUsedMb > 2048) { // > 2GB
            return true;
        }
        
        if (cpuUsagePercent != null && cpuUsagePercent.compareTo(BigDecimal.valueOf(80)) > 0) {
            return true;
        }
        
        if (ioReadMb != null && ioWriteMb != null && (ioReadMb + ioWriteMb) > 1024) { // > 1GB I/O
            return true;
        }
        
        return false;
    }

    /**
     * Generate performance summary
     * @return performance summary text
     */
    public String generatePerformanceSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Performance Measurement: ").append(performanceMeasurementType);
        
        if (durationMs != null) {
            summary.append(", Duration: ").append(durationMs).append("ms");
        }
        
        if (recordsProcessed != null) {
            summary.append(", Records: ").append(recordsProcessed);
        }
        
        if (throughputRecordsPerSec != null) {
            summary.append(", Throughput: ").append(throughputRecordsPerSec).append(" rec/sec");
        }
        
        if (performanceImprovementPercent != null) {
            summary.append(", Improvement: ").append(performanceImprovementPercent).append("%");
        }
        
        if (hasError()) {
            summary.append(", ERROR: ").append(errorMessage);
        }
        
        return summary.toString();
    }

    /**
     * Check if this is a recent measurement (within last hour)
     * @return true if recent
     */
    public boolean isRecentMeasurement() {
        return measurementTimestamp.isAfter(LocalDateTime.now().minusHours(1));
    }

    /**
     * Get age of measurement in minutes
     * @return age in minutes
     */
    public long getAgeInMinutes() {
        return java.time.Duration.between(measurementTimestamp, LocalDateTime.now()).toMinutes();
    }

    /**
     * Validate performance data
     * @return true if valid
     */
    public boolean isValidPerformanceData() {
        if (stagingDefId == null || executionId == null || executionId.trim().isEmpty()) {
            return false;
        }
        
        if (performanceMeasurementType == null) {
            return false;
        }
        
        if (durationMs != null && durationMs < 0) {
            return false;
        }
        
        if (recordsProcessed != null && recordsProcessed < 0) {
            return false;
        }
        
        if (memoryUsedMb != null && memoryUsedMb < 0) {
            return false;
        }
        
        return true;
    }

    /**
     * Mark as error condition
     * @param errorMessage error description
     * @param errorCode error code
     */
    public void markAsError(String errorMessage, String errorCode) {
        this.errorOccurred = "Y";
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    /**
     * Set optimization results
     * @param optimization optimization description
     * @param effectiveness effectiveness level
     * @param improvementPercent improvement percentage
     */
    public void setOptimizationResults(String optimization, OptimizationEffectiveness effectiveness, 
                                     BigDecimal improvementPercent) {
        this.optimizationApplied = optimization;
        this.optimizationEffectiveness = effectiveness;
        this.performanceImprovementPercent = improvementPercent;
    }

    /**
     * Update resource utilization metrics
     * @param memoryMb memory usage in MB
     * @param cpuPercent CPU usage percentage
     * @param ioReadMb I/O read in MB
     * @param ioWriteMb I/O write in MB
     */
    public void updateResourceMetrics(Integer memoryMb, BigDecimal cpuPercent, Integer ioReadMb, Integer ioWriteMb) {
        this.memoryUsedMb = memoryMb;
        this.cpuUsagePercent = cpuPercent;
        this.ioReadMb = ioReadMb;
        this.ioWriteMb = ioWriteMb;
    }

    @Override
    public String toString() {
        return "TempStagingPerformanceEntity{" +
                "performanceId=" + performanceId +
                ", stagingDefId=" + stagingDefId +
                ", executionId='" + executionId + '\'' +
                ", performanceMeasurementType=" + performanceMeasurementType +
                ", durationMs=" + durationMs +
                ", recordsProcessed=" + recordsProcessed +
                ", performanceCategory='" + getPerformanceCategory() + '\'' +
                ", hasError=" + hasError() +
                '}';
    }
}