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
public class JobListResponse {
    private int total;
    private List<JobSummary> executions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JobSummary {
        private String executionId;
        private String jobConfigId;
        private String sourceSystem;
        private String status;
        private Instant submittedAt;
        private String auditTrailUrl;
    }
}
