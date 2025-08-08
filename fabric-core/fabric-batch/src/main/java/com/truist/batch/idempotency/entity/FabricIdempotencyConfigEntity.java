package com.truist.batch.idempotency.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Entity representing idempotency configuration settings.
 * Provides flexible configuration management for different job types and API endpoints
 * enabling fine-grained control over idempotency behavior.
 * 
 * @author Fabric Platform Team
 * @version 1.0
 * @since 2025-08
 */
@Entity
@Table(name = "fabric_idempotency_config", schema = "CM3INT")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FabricIdempotencyConfigEntity {
    
    @Id
    @Column(name = "config_id", length = 100)
    private String configId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "config_type", length = 20, nullable = false)
    private ConfigType configType;
    
    @Column(name = "target_pattern", length = 500, nullable = false)
    private String targetPattern; // job name pattern or endpoint pattern
    
    @Column(name = "enabled", length = 1)
    private String enabled = "Y";
    
    @Column(name = "ttl_hours")
    private Integer ttlHours = 24;
    
    @Column(name = "max_retries")
    private Integer maxRetries = 3;
    
    @Column(name = "key_generation_strategy", length = 50)
    private String keyGenerationStrategy = "AUTO_GENERATED";
    
    @Column(name = "store_request_payload", length = 1)
    private String storeRequestPayload = "Y";
    
    @Column(name = "store_response_payload", length = 1)
    private String storeResponsePayload = "Y";
    
    @Column(name = "cleanup_policy", length = 20)
    private String cleanupPolicy = "TTL_BASED";
    
    @Column(name = "encryption_required", length = 1)
    private String encryptionRequired = "N";
    
    @Column(name = "compliance_flags", length = 100)
    private String complianceFlags; // SOX, PCI, GDPR flags
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Column(name = "created_by", length = 50, nullable = false)
    private String createdBy;
    
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
    
    @Column(name = "modified_by", length = 50)
    private String modifiedBy;
    
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
    
    @Column(name = "version")
    private Integer version = 1;
    
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (enabled == null) {
            enabled = "Y";
        }
        if (version == null) {
            version = 1;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
        version = version == null ? 1 : version + 1;
    }
    
    /**
     * Configuration types for idempotency
     */
    public enum ConfigType {
        BATCH_JOB("Configuration for Spring Batch jobs"),
        API_ENDPOINT("Configuration for REST API endpoints");
        
        private final String description;
        
        ConfigType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // Utility methods
    
    /**
     * Checks if idempotency is enabled for this configuration
     */
    public boolean isEnabled() {
        return "Y".equalsIgnoreCase(enabled);
    }
    
    /**
     * Checks if request payload storage is enabled
     */
    public boolean shouldStoreRequestPayload() {
        return "Y".equalsIgnoreCase(storeRequestPayload);
    }
    
    /**
     * Checks if response payload storage is enabled
     */
    public boolean shouldStoreResponsePayload() {
        return "Y".equalsIgnoreCase(storeResponsePayload);
    }
    
    /**
     * Checks if encryption is required for payloads
     */
    public boolean isEncryptionRequired() {
        return "Y".equalsIgnoreCase(encryptionRequired);
    }
    
    /**
     * Gets TTL in seconds
     */
    public Integer getTtlSeconds() {
        return ttlHours != null ? ttlHours * 3600 : 86400; // Default 24 hours
    }
    
    /**
     * Checks if the target pattern matches the given input
     */
    public boolean matches(String target) {
        if (targetPattern == null || target == null) {
            return false;
        }
        
        // Simple wildcard matching - can be enhanced with regex
        if ("*".equals(targetPattern)) {
            return true;
        }
        
        if (targetPattern.contains("*")) {
            String pattern = targetPattern.replace("*", ".*");
            return target.matches(pattern);
        }
        
        return targetPattern.equals(target);
    }
    
    /**
     * Checks if the configuration has compliance flags
     */
    public boolean hasComplianceFlags() {
        return complianceFlags != null && !complianceFlags.trim().isEmpty();
    }
    
    /**
     * Checks if a specific compliance flag is set
     */
    public boolean hasComplianceFlag(String flag) {
        if (!hasComplianceFlags() || flag == null) {
            return false;
        }
        String[] flags = complianceFlags.split("[,;]");
        for (String f : flags) {
            if (flag.equalsIgnoreCase(f.trim())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the cleanup policy as enum
     */
    public CleanupPolicy getCleanupPolicyEnum() {
        try {
            return CleanupPolicy.valueOf(cleanupPolicy);
        } catch (Exception e) {
            return CleanupPolicy.TTL_BASED;
        }
    }
    
    /**
     * Gets the key generation strategy as enum
     */
    public KeyGenerationStrategy getKeyGenerationStrategyEnum() {
        try {
            return KeyGenerationStrategy.valueOf(keyGenerationStrategy);
        } catch (Exception e) {
            return KeyGenerationStrategy.AUTO_GENERATED;
        }
    }
    
    /**
     * Creates a summary string for logging
     */
    public String getSummary() {
        return String.format("IdempotencyConfig[id=%s, type=%s, pattern=%s, enabled=%s, ttl=%dh]",
                configId, configType, targetPattern, enabled, ttlHours);
    }
    
    /**
     * Cleanup policy options
     */
    public enum CleanupPolicy {
        TTL_BASED,
        MANUAL,
        COUNT_BASED
    }
    
    /**
     * Key generation strategy options
     */
    public enum KeyGenerationStrategy {
        AUTO_GENERATED,
        CLIENT_PROVIDED,
        HASH_BASED
    }
    
    /**
     * Factory method to create default batch job configuration
     */
    public static FabricIdempotencyConfigEntity createDefaultBatchConfig(String createdBy) {
        return FabricIdempotencyConfigEntity.builder()
                .configId("DEFAULT_BATCH_JOB")
                .configType(ConfigType.BATCH_JOB)
                .targetPattern("*")
                .enabled("Y")
                .ttlHours(24)
                .maxRetries(3)
                .keyGenerationStrategy("AUTO_GENERATED")
                .storeRequestPayload("Y")
                .storeResponsePayload("Y")
                .cleanupPolicy("TTL_BASED")
                .encryptionRequired("N")
                .complianceFlags("SOX,AUDIT")
                .description("Default configuration for all batch jobs")
                .createdBy(createdBy)
                .createdDate(LocalDateTime.now())
                .version(1)
                .build();
    }
    
    /**
     * Factory method to create default API endpoint configuration
     */
    public static FabricIdempotencyConfigEntity createDefaultApiConfig(String createdBy) {
        return FabricIdempotencyConfigEntity.builder()
                .configId("DEFAULT_API_ENDPOINT")
                .configType(ConfigType.API_ENDPOINT)
                .targetPattern("/api/*")
                .enabled("Y")
                .ttlHours(1)
                .maxRetries(3)
                .keyGenerationStrategy("AUTO_GENERATED")
                .storeRequestPayload("Y")
                .storeResponsePayload("Y")
                .cleanupPolicy("TTL_BASED")
                .encryptionRequired("N")
                .complianceFlags("SOX,AUDIT")
                .description("Default configuration for all API endpoints")
                .createdBy(createdBy)
                .createdDate(LocalDateTime.now())
                .version(1)
                .build();
    }
}