package com.fabric.batch.dto.jobexecution;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobStatusResponse {

    private String executionId;
    private String jobConfigId;
    private String sourceSystem;
    private String status;
    private Instant submittedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant failedAt;
    private Instant cancelledAt;
    private Long durationSeconds;
    private Long recordsProcessed;
    private String outputFilePath;
    private String auditTrailUrl;
    private String retryUrl;
    private ErrorDetails errorDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        private String errorCode;
        private String errorMessage;
        private String failedStep;
        private String stackTrace;
        private List<Long> affectedRecords;
    }
}
