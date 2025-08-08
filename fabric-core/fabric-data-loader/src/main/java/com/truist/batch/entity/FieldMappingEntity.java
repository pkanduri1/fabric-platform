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
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Epic 2: Enhanced FieldMappingEntity for database-driven field mapping configuration.
 * This entity extends the existing field mapping capabilities with Epic 2 features
 * including encryption, PII classification, and business rule integration.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 2 - Simple Transaction Processing
 */
@Entity
@Table(name = "FIELD_MAPPINGS",
       indexes = {
           @Index(name = "idx_field_mappings_composite", 
                  columnList = "transaction_type_id, sequence_order, field_name"),
           @Index(name = "idx_field_mappings_target", 
                  columnList = "transaction_type_id, target_field"),
           @Index(name = "idx_field_mappings_config", 
                  columnList = "config_id, field_name")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, 
                    generator = "seq_field_mappings")
    @SequenceGenerator(name = "seq_field_mappings", 
                       sequenceName = "seq_field_mappings", 
                       allocationSize = 1)
    @Column(name = "mapping_id")
    private Long mappingId;

    // Existing Epic 1 fields
    @Column(name = "config_id")
    private Long configId;

    @NotBlank(message = "Field name is required")
    @Size(max = 100, message = "Field name cannot exceed 100 characters")
    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Size(max = 100, message = "Source field name cannot exceed 100 characters")
    @Column(name = "source_field", length = 100)
    private String sourceField;

    @NotBlank(message = "Target field is required")
    @Size(max = 100, message = "Target field name cannot exceed 100 characters")
    @Column(name = "target_field", nullable = false, length = 100)
    private String targetField;

    @Min(value = 0, message = "Target position cannot be negative")
    @Max(value = 9999, message = "Target position cannot exceed 9999")
    @Column(name = "target_position")
    @Builder.Default
    private Integer targetPosition = 0;

    @Min(value = 0, message = "Field length cannot be negative")
    @Max(value = 5000, message = "Field length cannot exceed 5000")
    @Column(name = "field_length")
    private Integer fieldLength;

    @Size(max = 50, message = "Data type cannot exceed 50 characters")
    @Column(name = "data_type", length = 50)
    private String dataType;

    @Size(max = 50, message = "Transformation type cannot exceed 50 characters")
    @Column(name = "transformation_type", length = 50)
    private String transformationType;

    @Lob
    @Column(name = "transformation_details")
    private String transformationDetails;

    @Size(max = 500, message = "Default value cannot exceed 500 characters")
    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "validation_required")
    @Builder.Default
    private Boolean validationRequired = false;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    @NotBlank(message = "Created by is required")
    @Size(max = 50, message = "Created by cannot exceed 50 characters")
    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;

    @Size(max = 50, message = "Last modified by cannot exceed 50 characters")
    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    // Epic 2 Enhanced Fields
    @Column(name = "transaction_type_id")
    private Long transactionTypeId;

    @Size(max = 20, message = "Encryption level cannot exceed 20 characters")
    @Column(name = "encryption_level", length = 20)
    @Builder.Default
    private String encryptionLevel = "NONE";

    @Size(max = 20, message = "PII classification cannot exceed 20 characters")
    @Column(name = "pii_classification", length = 20)
    @Builder.Default
    private String piiClassification = "NONE";

    @Column(name = "business_rule_id")
    private Long businessRuleId;

    @Min(value = 1, message = "Sequence order must be at least 1")
    @Max(value = 9999, message = "Sequence order cannot exceed 9999")
    @Column(name = "sequence_order")
    @Builder.Default
    private Integer sequenceOrder = 1;

    @Column(name = "active_flag", length = 1)
    @Builder.Default
    private String activeFlag = "Y";

    @Column(name = "compliance_level", length = 20)
    @Builder.Default
    private String complianceLevel = "STANDARD";

    @Lob
    @Column(name = "business_context")
    private String businessContext;

    @Size(max = 10, message = "Version cannot exceed 10 characters")
    @Column(name = "version", length = 10)
    @Builder.Default
    private String version = "1.0";

    /**
     * Relationship mappings
     */
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type_id", 
                referencedColumnName = "transaction_type_id", 
                insertable = false, 
                updatable = false)
    private BatchTransactionTypeEntity transactionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", 
                referencedColumnName = "config_id", 
                insertable = false, 
                updatable = false)
    private BatchConfigurationEntity batchConfiguration;

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

    public boolean requiresEncryption() {
        return encryptionLevel != null && 
               !"NONE".equalsIgnoreCase(encryptionLevel) && 
               !encryptionLevel.trim().isEmpty();
    }

    public boolean containsPII() {
        return piiClassification != null && 
               !"NONE".equalsIgnoreCase(piiClassification) && 
               !piiClassification.trim().isEmpty();
    }

    public boolean isHighCompliance() {
        return "HIGH".equalsIgnoreCase(complianceLevel) || 
               "CRITICAL".equalsIgnoreCase(complianceLevel);
    }

    /**
     * Epic 2 specific methods
     */
    
    public EncryptionLevel getEncryptionLevelEnum() {
        try {
            return EncryptionLevel.valueOf(encryptionLevel.toUpperCase());
        } catch (Exception e) {
            return EncryptionLevel.NONE;
        }
    }

    public PIIClassification getPIIClassificationEnum() {
        try {
            return PIIClassification.valueOf(piiClassification.toUpperCase());
        } catch (Exception e) {
            return PIIClassification.NONE;
        }
    }

    public ComplianceLevel getComplianceLevelEnum() {
        try {
            return ComplianceLevel.valueOf(complianceLevel.toUpperCase());
        } catch (Exception e) {
            return ComplianceLevel.STANDARD;
        }
    }

    /**
     * Data quality and validation methods
     */
    
    public boolean hasValidTransformation() {
        return transformationType != null && !transformationType.trim().isEmpty();
    }

    public boolean hasBusinessRule() {
        return businessRuleId != null && businessRuleId > 0;
    }

    public boolean isSourceField() {
        return "source".equalsIgnoreCase(transformationType);
    }

    public boolean isConstantField() {
        return "constant".equalsIgnoreCase(transformationType);
    }

    public boolean isComputedField() {
        return "computed".equalsIgnoreCase(transformationType) || 
               "composite".equalsIgnoreCase(transformationType);
    }

    /**
     * Performance optimization methods
     */
    
    public boolean shouldCacheTransformation() {
        return isConstantField() || 
               (transformationDetails != null && transformationDetails.length() > 100);
    }

    public int getComplexityScore() {
        int score = 0;
        
        if (requiresEncryption()) score += 3;
        if (containsPII()) score += 2;
        if (hasBusinessRule()) score += 2;
        if (isComputedField()) score += 1;
        if (validationRequired != null && validationRequired) score += 1;
        
        return score;
    }

    /**
     * Audit and compliance methods
     */
    
    public FieldMappingAudit getAuditInfo() {
        return FieldMappingAudit.builder()
                .mappingId(mappingId)
                .fieldName(fieldName)
                .targetField(targetField)
                .encryptionLevel(encryptionLevel)
                .piiClassification(piiClassification)
                .complianceLevel(complianceLevel)
                .activeFlag(activeFlag)
                .createdDate(createdDate)
                .lastModifiedDate(lastModifiedDate)
                .createdBy(createdBy)
                .lastModifiedBy(lastModifiedBy)
                .version(version)
                .build();
    }

    /**
     * Configuration export/import methods
     */
    
    public Map<String, Object> toConfigurationMap() {
        Map<String, Object> config = new HashMap<>();
        config.put("fieldName", fieldName);
        config.put("sourceField", sourceField);
        config.put("targetField", targetField);
        config.put("targetPosition", targetPosition);
        config.put("fieldLength", fieldLength);
        config.put("dataType", dataType);
        config.put("transformationType", transformationType);
        config.put("transformationDetails", transformationDetails);
        config.put("defaultValue", defaultValue);
        config.put("encryptionLevel", encryptionLevel);
        config.put("piiClassification", piiClassification);
        config.put("complianceLevel", complianceLevel);
        config.put("sequenceOrder", sequenceOrder);
        config.put("validationRequired", validationRequired);
        config.put("businessContext", businessContext);
        config.put("version", version);
        
        return config;
    }

    /**
     * Enumeration classes
     */
    
    public enum EncryptionLevel {
        NONE, STANDARD, HIGH, CRITICAL
    }

    public enum PIIClassification {
        NONE, PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED, CARDHOLDER_DATA
    }

    public enum ComplianceLevel {
        STANDARD, MEDIUM, HIGH, CRITICAL
    }

    /**
     * Nested classes
     */
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FieldMappingAudit {
        private Long mappingId;
        private String fieldName;
        private String targetField;
        private String encryptionLevel;
        private String piiClassification;
        private String complianceLevel;
        private String activeFlag;
        private Instant createdDate;
        private Instant lastModifiedDate;
        private String createdBy;
        private String lastModifiedBy;
        private String version;
    }

    /**
     * JPA lifecycle callbacks
     */
    
    @PreUpdate
    public void preUpdate() {
        this.lastModifiedDate = Instant.now();
    }

    /**
     * String representation for logging and debugging
     */
    @Override
    public String toString() {
        return String.format("FieldMapping[id=%d, field=%s->%s, pos=%d, type=%s, active=%s]",
                mappingId, sourceField, targetField, targetPosition, transformationType, activeFlag);
    }
}