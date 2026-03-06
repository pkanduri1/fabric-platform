package com.fabric.batch.dto.jobexecution;

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
public class JobAuditResponse {
    private String executionId;
    private List<AuditEntry> auditEntries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditEntry {
        private Instant timestamp;
        private String action;
        private String actor;
        private String correlationId;
        private String details;
    }
}
