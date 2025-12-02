package com.fabric.batch.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing job definitions associated with source systems
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobDefinitionEntity {
    
    private String id;
    private String sourceSystemId;
    private String jobName;
    private String description;
    private String inputPath;
    private String outputPath;
    private String querySql;
    private String enabled = "Y";
    private LocalDateTime createdDate;
    private String transactionTypes; // Comma-separated: "200,300,900"
    
    /**
     * Constructor for creating new job definitions
     */
    public JobDefinitionEntity(String sourceSystemId, String jobName, String description, String transactionTypes) {
        this.sourceSystemId = sourceSystemId;
        this.jobName = jobName;
        this.description = description;
        this.transactionTypes = transactionTypes;
        this.id = sourceSystemId + "-" + jobName;
        this.createdDate = LocalDateTime.now();
        this.enabled = "Y";
    }
}