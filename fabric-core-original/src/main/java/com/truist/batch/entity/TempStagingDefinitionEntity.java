package com.truist.batch.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Epic 3: Temporary Staging Table Definition Entity
 * 
 * JPA entity representing dynamic staging table definitions for complex transaction processing.
 * Supports automatic table creation, lifecycle management, performance monitoring,
 * and cleanup policies for temporary staging tables.
 * 
 * Features:
 * - Dynamic table schema definition and creation
 * - Automatic lifecycle management with TTL policies
 * - Performance monitoring and optimization
 * - Multiple partition strategies support
 * - Cleanup policies and archival options
 * - Encryption and compression support
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 3
 */
@Entity
@Table(name = "TEMP_STAGING_DEFINITIONS", schema = "CM3INT",
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_staging_table_name", 
                           columnNames = {"staging_table_name"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TempStagingDefinitionEntity {

    /**
     * Partition strategies for staging tables
     */
    public enum PartitionStrategy {
        NONE,           // No partitioning
        HASH,           // Hash partitioning
        RANGE_DATE,     // Range partitioning by date
        RANGE_NUMBER,   // Range partitioning by number
        LIST            // List partitioning
    }

    /**
     * Cleanup policies for staging tables
     */
    public enum CleanupPolicy {
        AUTO_DROP,          // Automatically drop after TTL
        MANUAL,             // Manual cleanup only
        ARCHIVE_THEN_DROP,  // Archive data then drop
        KEEP_METADATA       // Drop table but keep metadata
    }

    /**
     * Compression levels for staging tables
     */
    public enum CompressionLevel {
        NONE,
        BASIC,
        ADVANCED,
        MAXIMUM
    }

    @Id
    @Column(name = "staging_def_id", nullable = false)
    @SequenceGenerator(name = "seq_temp_staging_definitions", 
                      sequenceName = "CM3INT.seq_temp_staging_definitions", 
                      allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, 
                   generator = "seq_temp_staging_definitions")
    private Long stagingDefId;

    @Column(name = "execution_id", nullable = false, length = 100)
    private String executionId;

    @Column(name = "transaction_type_id")
    private Long transactionTypeId;

    @Column(name = "staging_table_name", nullable = false, length = 128)
    private String stagingTableName;

    @Lob
    @Column(name = "table_schema", nullable = false)
    private String tableSchema;

    @Enumerated(EnumType.STRING)
    @Column(name = "partition_strategy", length = 50)
    private PartitionStrategy partitionStrategy = PartitionStrategy.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "cleanup_policy", length = 50)
    private CleanupPolicy cleanupPolicy = CleanupPolicy.AUTO_DROP;

    @Column(name = "ttl_hours")
    private Integer ttlHours = 24;

    @Column(name = "created_timestamp", nullable = false)
    private LocalDateTime createdTimestamp = LocalDateTime.now();

    @Column(name = "dropped_timestamp")
    private LocalDateTime droppedTimestamp;

    @Column(name = "record_count")
    private Long recordCount = 0L;

    @Column(name = "table_size_mb")
    private Integer tableSizeMb = 0;

    @Column(name = "last_access_time")
    private LocalDateTime lastAccessTime;

    @Column(name = "optimization_applied", length = 1000)
    private String optimizationApplied;

    @Column(name = "monitoring_enabled", length = 1)
    private String monitoringEnabled = "Y";

    @Enumerated(EnumType.STRING)
    @Column(name = "compression_level", length = 20)
    private CompressionLevel compressionLevel = CompressionLevel.BASIC;

    @Column(name = "encryption_applied", length = 1)
    private String encryptionApplied = "N";

    @Column(name = "business_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private java.util.Date businessDate = new java.util.Date();

    /**
     * Pre-persist callback
     */
    @PrePersist
    protected void onCreate() {
        if (createdTimestamp == null) {
            createdTimestamp = LocalDateTime.now();
        }
        if (recordCount == null) {
            recordCount = 0L;
        }
        if (tableSizeMb == null) {
            tableSizeMb = 0;
        }
        if (ttlHours == null) {
            ttlHours = 24;
        }
        if (monitoringEnabled == null) {
            monitoringEnabled = "Y";
        }
        if (encryptionApplied == null) {
            encryptionApplied = "N";
        }
        if (businessDate == null) {
            businessDate = new java.util.Date();
        }
    }

    /**
     * Business logic helper methods
     */

    /**
     * Check if the staging table is active (not dropped)
     * @return true if active
     */
    public boolean isActive() {
        return droppedTimestamp == null;
    }

    /**
     * Check if the staging table has expired based on TTL
     * @return true if expired
     */
    public boolean hasExpired() {
        if (ttlHours == null || ttlHours <= 0) {
            return false; // No expiration
        }
        
        LocalDateTime expirationTime = createdTimestamp.plusHours(ttlHours);
        return LocalDateTime.now().isAfter(expirationTime);
    }

    /**
     * Get expiration timestamp
     * @return expiration time or null if no expiration
     */
    public LocalDateTime getExpirationTime() {
        if (ttlHours == null || ttlHours <= 0) {
            return null;
        }
        return createdTimestamp.plusHours(ttlHours);
    }

    /**
     * Check if monitoring is enabled
     * @return true if monitoring enabled
     */
    public boolean isMonitoringEnabled() {
        return "Y".equals(monitoringEnabled);
    }

    /**
     * Check if encryption is applied
     * @return true if encrypted
     */
    public boolean isEncrypted() {
        return "Y".equals(encryptionApplied);
    }

    /**
     * Check if table should be automatically cleaned up
     * @return true if auto cleanup
     */
    public boolean shouldAutoCleanup() {
        return cleanupPolicy == CleanupPolicy.AUTO_DROP || 
               cleanupPolicy == CleanupPolicy.ARCHIVE_THEN_DROP;
    }

    /**
     * Check if table is partitioned
     * @return true if partitioned
     */
    public boolean isPartitioned() {
        return partitionStrategy != PartitionStrategy.NONE;
    }

    /**
     * Check if table is ready for cleanup
     * @return true if ready for cleanup
     */
    public boolean isReadyForCleanup() {
        return shouldAutoCleanup() && hasExpired() && isActive();
    }

    /**
     * Mark table as dropped
     */
    public void markAsDropped() {
        this.droppedTimestamp = LocalDateTime.now();
    }

    /**
     * Update access time
     */
    public void updateAccessTime() {
        this.lastAccessTime = LocalDateTime.now();
    }

    /**
     * Update record count and table size
     * @param newRecordCount new record count
     * @param newTableSizeMb new table size in MB
     */
    public void updateStatistics(long newRecordCount, int newTableSizeMb) {
        this.recordCount = newRecordCount;
        this.tableSizeMb = newTableSizeMb;
        this.lastAccessTime = LocalDateTime.now();
    }

    /**
     * Get table age in hours
     * @return age in hours
     */
    public long getAgeInHours() {
        return java.time.Duration.between(createdTimestamp, LocalDateTime.now()).toHours();
    }

    /**
     * Get remaining TTL in hours
     * @return remaining hours or -1 if no TTL
     */
    public long getRemainingTtlHours() {
        if (ttlHours == null || ttlHours <= 0) {
            return -1;
        }
        
        long ageHours = getAgeInHours();
        return Math.max(0, ttlHours - ageHours);
    }

    /**
     * Check if table needs optimization
     * @return true if optimization needed
     */
    public boolean needsOptimization() {
        // Optimization needed if:
        // 1. Large table size with low utilization
        // 2. Many records but frequent access
        // 3. No recent optimization applied
        
        if (tableSizeMb > 1000 && recordCount < 10000) {
            return true; // Large size, few records
        }
        
        if (recordCount > 100000 && lastAccessTime != null && 
            lastAccessTime.isAfter(LocalDateTime.now().minusHours(1))) {
            return true; // High activity table
        }
        
        return false;
    }

    /**
     * Generate DDL for table creation
     * @return DDL statement
     */
    public String generateCreateDDL() {
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE ").append(stagingTableName).append(" (");
        
        // Add schema definition
        ddl.append(tableSchema);
        
        ddl.append(")");
        
        // Add partitioning if specified
        if (isPartitioned()) {
            ddl.append(" PARTITION BY ");
            switch (partitionStrategy) {
                case HASH:
                    ddl.append("HASH (partition_key) PARTITIONS 4");
                    break;
                case RANGE_DATE:
                    ddl.append("RANGE (business_date) (PARTITION p_current VALUES LESS THAN (SYSDATE + 1))");
                    break;
                case RANGE_NUMBER:
                    ddl.append("RANGE (sequence_id) (PARTITION p_default VALUES LESS THAN (MAXVALUE))");
                    break;
                case LIST:
                    ddl.append("LIST (status) (PARTITION p_pending VALUES ('PENDING'))");
                    break;
                default:
                    break;
            }
        }
        
        // Add compression if specified
        if (compressionLevel != CompressionLevel.NONE) {
            ddl.append(" COMPRESS FOR OLTP");
        }
        
        return ddl.toString();
    }

    /**
     * Generate DROP DDL for table cleanup
     * @return DROP statement
     */
    public String generateDropDDL() {
        return "DROP TABLE " + stagingTableName + " PURGE";
    }

    /**
     * Validate staging definition
     * @return true if valid
     */
    public boolean isValidDefinition() {
        if (executionId == null || executionId.trim().isEmpty()) {
            return false;
        }
        if (stagingTableName == null || stagingTableName.trim().isEmpty()) {
            return false;
        }
        if (tableSchema == null || tableSchema.trim().isEmpty()) {
            return false;
        }
        if (ttlHours != null && ttlHours < 0) {
            return false;
        }
        if (recordCount < 0) {
            return false;
        }
        if (tableSizeMb < 0) {
            return false;
        }
        
        return true;
    }

    /**
     * Get lifecycle status description
     * @return status description
     */
    public String getLifecycleStatus() {
        if (droppedTimestamp != null) {
            return "DROPPED";
        } else if (hasExpired() && shouldAutoCleanup()) {
            return "EXPIRED_READY_FOR_CLEANUP";
        } else if (hasExpired()) {
            return "EXPIRED";
        } else {
            return "ACTIVE";
        }
    }

    @Override
    public String toString() {
        return "TempStagingDefinitionEntity{" +
                "stagingDefId=" + stagingDefId +
                ", executionId='" + executionId + '\'' +
                ", stagingTableName='" + stagingTableName + '\'' +
                ", partitionStrategy=" + partitionStrategy +
                ", cleanupPolicy=" + cleanupPolicy +
                ", ttlHours=" + ttlHours +
                ", recordCount=" + recordCount +
                ", tableSizeMb=" + tableSizeMb +
                ", lifecycleStatus='" + getLifecycleStatus() + '\'' +
                '}';
    }
}