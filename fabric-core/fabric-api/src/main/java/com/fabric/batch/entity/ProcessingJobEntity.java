package com.fabric.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * POJO for processing job to satisfy compilation requirements.
 * This is a temporary implementation until the fabric-data-loader dependency issue is resolved.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingJobEntity {

    private String id;
    
    private String jobName;
    private String description;
    private String sourceSystem;
    
    private JobStatus status;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String executedBy;
    private String parameters;
    private LocalDateTime createdDate;
    private String createdBy;
    
    public enum JobStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}