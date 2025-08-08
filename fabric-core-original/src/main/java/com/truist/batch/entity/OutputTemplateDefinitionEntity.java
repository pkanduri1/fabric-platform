package com.truist.batch.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Epic 3: Output Template Definition Entity
 * 
 * JPA entity for managing template definitions for header and footer generation
 * with support for multiple output formats, variable substitution, and versioning.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 3
 */
@Entity
@Table(name = "OUTPUT_TEMPLATE_DEFINITIONS", schema = "CM3INT",
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_template_name_version", 
                           columnNames = {"template_name", "version_number"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class OutputTemplateDefinitionEntity {

    /**
     * Template types
     */
    public enum TemplateType {
        HEADER,
        FOOTER,
        HEADER_FOOTER,
        CUSTOM
    }

    /**
     * Output formats
     */
    public enum OutputFormat {
        CSV,
        XML,
        JSON,
        FIXED_WIDTH,
        DELIMITED,
        EXCEL
    }

    @Id
    @Column(name = "template_id", nullable = false)
    @SequenceGenerator(name = "seq_output_template_definitions", 
                      sequenceName = "CM3INT.seq_output_template_definitions", 
                      allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, 
                   generator = "seq_output_template_definitions")
    private Long templateId;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 50)
    private TemplateType templateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_format", length = 20)
    private OutputFormat outputFormat = OutputFormat.CSV;

    @Lob
    @Column(name = "template_content", nullable = false)
    private String templateContent;

    @Lob
    @Column(name = "variable_definitions")
    private String variableDefinitions;

    @Lob
    @Column(name = "conditional_logic")
    private String conditionalLogic;

    @Lob
    @Column(name = "formatting_rules")
    private String formattingRules;

    @Lob
    @Column(name = "validation_rules")
    private String validationRules;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    @Column(name = "version_number")
    private Integer versionNumber = 1;

    @Column(name = "active_flag", length = 1)
    private String activeFlag = "Y";

    @Column(name = "compliance_flags", length = 200)
    private String complianceFlags;

    @Column(name = "business_owner", length = 50)
    private String businessOwner;

    @Column(name = "technical_contact", length = 50)
    private String technicalContact;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (versionNumber == null) {
            versionNumber = 1;
        }
        if (activeFlag == null) {
            activeFlag = "Y";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }

    public boolean isActive() {
        return "Y".equals(activeFlag);
    }
}