package com.fabric.batch.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing transformation configuration for migrating 
 * Excel-based mappings to database-driven transformation rules.
 * 
 * This entity supports lending and risk management interfaces by providing
 * structured transformation configuration with audit capabilities.
 */
@Entity
@Table(name = "transformation_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TransformationConfigEntity {
    
    @Id
    @Column(name = "config_id", length = 100)
    private String configId;
    
    @Column(name = "source_system", nullable = false, length = 50)
    private String sourceSystem;
    
    @Column(name = "interface_name", nullable = false, length = 50)
    private String interfaceName;
    
    @Column(name = "transaction_type", length = 20)
    private String transactionType = "200";
    
    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Column(name = "excel_source_file", length = 500)
    private String excelSourceFile;
    
    @Column(name = "total_fields")
    private Integer totalFields = 0;
    
    @Column(name = "active_fields")
    private Integer activeFields = 0;
    
    @Column(name = "validation_enabled", length = 1)
    private String validationEnabled = "Y";
    
    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @Column(name = "modified_by", length = 50)
    private String modifiedBy;
    
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
    
    @Column(name = "version")
    private Integer version = 1;
    
    @Column(name = "enabled", length = 1)
    private String enabled = "Y";
    
    // Relationship to field transformation rules
    @OneToMany(mappedBy = "transformationConfig", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FieldTransformationRuleEntity> fieldTransformationRules;
    
    // Relationship to staging table configurations
    @OneToMany(mappedBy = "transformationConfig", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StagingTableConfigEntity> stagingTableConfigs;
    
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (configId == null) {
            configId = generateConfigId();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
        version++;
    }
    
    /**
     * Generates a unique configuration ID based on system, interface, and transaction type
     */
    private String generateConfigId() {
        return String.format("%s-%s-%s", 
            sourceSystem.toUpperCase(),
            interfaceName.toUpperCase(),
            transactionType
        );
    }
    
    /**
     * Checks if validation is enabled for this configuration
     */
    public boolean isValidationEnabled() {
        return "Y".equals(validationEnabled);
    }
    
    /**
     * Checks if this configuration is currently enabled
     */
    public boolean isEnabled() {
        return "Y".equals(enabled);
    }
    
    /**
     * Gets the configuration identifier for logging and audit purposes
     */
    public String getConfigurationIdentifier() {
        return String.format("%s.%s.%s", sourceSystem, interfaceName, transactionType);
    }
}