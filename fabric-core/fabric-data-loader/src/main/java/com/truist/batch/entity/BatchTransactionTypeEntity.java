package com.truist.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/**
 * Epic 2: Entity representing transaction type configurations for parallel processing.
 * This entity supports the banking-grade parallel transaction processing architecture
 * with proper validation, audit trails, and compliance features.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 2 - Simple Transaction Processing
 */
@Entity
@Table(name = "BATCH_TRANSACTION_TYPES", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_trans_type_job", 
                           columnNames = {"job_config_id", "transaction_type"})
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTransactionTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, 
                    generator = "seq_batch_transaction_types")
    @SequenceGenerator(name = "seq_batch_transaction_types", 
                       sequenceName = "seq_batch_transaction_types", 
                       allocationSize = 1)
    @Column(name = "transaction_type_id")
    private Long transactionTypeId;

    @NotNull(message = "Job configuration ID is required")
    @Column(name = "job_config_id", nullable = false)
    private Long jobConfigId;

    @NotBlank(message = "Transaction type is required")
    @Column(name = "transaction_type", nullable = false, length = 50)
    private String transactionType;

    @Min(value = 1, message = "Processing order must be at least 1")
    @Max(value = 999, message = "Processing order cannot exceed 999")
    @Column(name = "processing_order")
    @Builder.Default
    private Integer processingOrder = 1;

    @Min(value = 1, message = "Parallel threads must be at least 1")
    @Max(value = 50, message = "Parallel threads cannot exceed 50")
    @Column(name = "parallel_threads")
    @Builder.Default
    private Integer parallelThreads = 5;

    @Min(value = 100, message = "Chunk size must be at least 100")
    @Max(value = 100000, message = "Chunk size cannot exceed 100,000")
    @Column(name = "chunk_size")
    @Builder.Default
    private Integer chunkSize = 1000;

    @Column(name = "isolation_level", length = 20)
    @Builder.Default
    private String isolationLevel = "read_committed";

    @Column(name = "retry_policy", length = 100)
    private String retryPolicy;

    @Min(value = 30, message = "Timeout must be at least 30 seconds")
    @Max(value = 3600, message = "Timeout cannot exceed 3600 seconds")
    @Column(name = "timeout_seconds")
    @Builder.Default
    private Integer timeoutSeconds = 300;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    @NotBlank(message = "Created by is required")
    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;

    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    @Lob
    @Column(name = "encryption_fields")
    private String encryptionFields;

    @Column(name = "compliance_level", length = 20)
    @Builder.Default
    private String complianceLevel = "STANDARD";

    @Column(name = "active_flag", length = 1)
    @Builder.Default
    private String activeFlag = "Y";

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_config_id", 
                referencedColumnName = "config_id", 
                insertable = false, 
                updatable = false)
    private BatchConfigurationEntity batchConfiguration;

    @OneToMany(mappedBy = "transactionTypeId", 
               fetch = FetchType.LAZY, 
               cascade = CascadeType.ALL)
    private List<FieldMappingEntity> fieldMappings;

    /**
     * Business logic methods
     */
    
    public boolean isActive() {
        return "Y".equalsIgnoreCase(activeFlag);
    }

    public void activate() {
        this.activeFlag = "Y";
        this.lastModifiedDate = Instant.now();
    }

    public void deactivate() {
        this.activeFlag = "N";
        this.lastModifiedDate = Instant.now();
    }

    public boolean isHighComplianceLevel() {
        return "HIGH".equalsIgnoreCase(complianceLevel) || 
               "CRITICAL".equalsIgnoreCase(complianceLevel);
    }

    public boolean requiresEncryption() {
        return encryptionFields != null && !encryptionFields.trim().isEmpty();
    }

    /**
     * Get retry configuration as structured object
     */
    public RetryConfiguration getRetryConfiguration() {
        if (retryPolicy == null || retryPolicy.trim().isEmpty()) {
            return RetryConfiguration.defaultConfig();
        }
        
        try {
            // Parse retry policy format: "maxRetries:3,backoffMs:1000,exponential:true"
            String[] parts = retryPolicy.split(",");
            RetryConfiguration config = new RetryConfiguration();
            
            for (String part : parts) {
                String[] keyValue = part.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    
                    switch (key) {
                        case "maxRetries":
                            config.setMaxRetries(Integer.parseInt(value));
                            break;
                        case "backoffMs":
                            config.setBackoffMs(Long.parseLong(value));
                            break;
                        case "exponential":
                            config.setExponential(Boolean.parseBoolean(value));
                            break;
                    }
                }
            }
            
            return config;
        } catch (Exception e) {
            return RetryConfiguration.defaultConfig();
        }
    }

    /**
     * Audit tracking method
     */
    @PreUpdate
    public void onUpdate() {
        this.lastModifiedDate = Instant.now();
    }

    /**
     * Nested class for retry configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetryConfiguration {
        @Builder.Default
        private int maxRetries = 3;
        
        @Builder.Default
        private long backoffMs = 1000;
        
        @Builder.Default
        private boolean exponential = true;
        
        @Builder.Default
        private double multiplier = 2.0;

        public static RetryConfiguration defaultConfig() {
            return RetryConfiguration.builder()
                .maxRetries(3)
                .backoffMs(1000)
                .exponential(true)
                .multiplier(2.0)
                .build();
        }
    }

    /**
     * Performance optimization configuration
     */
    public boolean shouldUseParallelProcessing() {
        return parallelThreads > 1 && chunkSize >= 100;
    }

    public int getOptimalThreadCount(int recordCount) {
        if (recordCount < chunkSize) {
            return 1;
        }
        
        int calculatedThreads = Math.min(
            parallelThreads,
            (recordCount / chunkSize) + 1
        );
        
        return Math.max(1, calculatedThreads);
    }

    /**
     * String representation for logging and debugging
     */
    @Override
    public String toString() {
        return String.format("BatchTransactionType[id=%d, type=%s, threads=%d, chunkSize=%d, active=%s]",
                transactionTypeId, transactionType, parallelThreads, chunkSize, activeFlag);
    }
}