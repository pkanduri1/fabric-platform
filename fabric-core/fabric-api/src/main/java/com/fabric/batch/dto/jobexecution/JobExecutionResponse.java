package com.fabric.batch.dto.jobexecution;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobExecutionResponse {
    private String executionId;
    private String jobConfigId;
    private String sourceSystem;
    private String status;
    private Instant submittedAt;
    private String statusUrl;
    private String auditTrailUrl;
    private String message;
    // for retry responses
    private String originalExecutionId;
    private String newExecutionId;
}
