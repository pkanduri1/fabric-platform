package com.fabric.batch.service;

import com.fabric.batch.dto.jobexecution.*;
import com.fabric.batch.entity.ManualJobConfigEntity;
import com.fabric.batch.entity.ManualJobExecutionEntity;
import com.fabric.batch.exception.JobExecutionApiException;
import com.fabric.batch.repository.ManualJobConfigRepository;
import com.fabric.batch.repository.ManualJobExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobExecutionApiService {

    private final JdbcTemplate jdbcTemplate;
    private final ManualJobExecutionRepository executionRepository;
    private final ManualJobConfigRepository configRepository;

    // States that allow CANCELLED transition
    private static final Set<String> CANCELLABLE = Set.of("STARTED", "RUNNING");
    // States that allow RETRY
    private static final Set<String> RETRYABLE = Set.of("FAILED");

    // ── Submit ────────────────────────────────────────────────────────────────

    @Transactional
    public JobExecutionResponse submitJob(JobExecutionRequest req, String actor) {
        validateJobConfigExists(req.getJobConfigId());

        String execId = generateExecutionId();
        ManualJobExecutionEntity entity = ManualJobExecutionEntity.builder()
                .executionId(execId)
                .configId(req.getJobConfigId())
                .jobName(req.getJobConfigId())
                .status("STARTED")
                .triggerSource(req.getSourceSystem())
                .executionType("TRIGGERED")
                .apiSource("API")
                .callbackUrl(req.getCallbackUrl())
                .callbackHeaders(serializeHeaders(req.getCallbackHeaders()))
                .callbackStatus(req.getCallbackUrl() != null ? "PENDING" : "SKIPPED")
                .correlationId(java.util.UUID.randomUUID().toString())
                .executedBy(actor)
                .build();

        executionRepository.save(entity);

        log.info("[{}] Job submitted via API — configId={}", execId, req.getJobConfigId());

        return JobExecutionResponse.builder()
                .executionId(execId)
                .jobConfigId(req.getJobConfigId())
                .sourceSystem(req.getSourceSystem())
                .status("STARTED")
                .submittedAt(Instant.now())
                .statusUrl("/api/v1/jobs/" + execId + "/status")
                .auditTrailUrl("/api/v1/jobs/" + execId + "/audit")
                .message("Job submitted successfully. Poll statusUrl or await callback.")
                .build();
    }

    // ── Status ────────────────────────────────────────────────────────────────

    public JobStatusResponse getStatus(String executionId) {
        ManualJobExecutionEntity e = findOrThrow(executionId);
        JobStatusResponse.JobStatusResponseBuilder b = JobStatusResponse.builder()
                .executionId(e.getExecutionId())
                .jobConfigId(e.getConfigId())
                .sourceSystem(e.getTriggerSource())
                .status(e.getStatus())
                .auditTrailUrl("/api/v1/jobs/" + executionId + "/audit");

        if ("FAILED".equals(e.getStatus())) {
            b.retryUrl("/api/v1/jobs/" + executionId + "/retry");
            if (e.getErrorMessage() != null) {
                b.errorDetails(JobStatusResponse.ErrorDetails.builder()
                        .errorCode("TRANSFORM_RULE_FAILURE")
                        .errorMessage(e.getErrorMessage())
                        .stackTrace(e.getErrorStackTrace())
                        .build());
            }
        }
        return b.build();
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Transactional
    public JobExecutionResponse cancelJob(String executionId) {
        ManualJobExecutionEntity e = findOrThrow(executionId);
        if (!CANCELLABLE.contains(e.getStatus())) {
            throw JobExecutionApiException.conflict("INVALID_STATE_TRANSITION",
                    "INVALID_STATE_TRANSITION: Cannot cancel job " + executionId + " — current status is " + e.getStatus());
        }
        executionRepository.updateStatus(executionId, "CANCELLED");
        return JobExecutionResponse.builder()
                .executionId(executionId)
                .status("CANCELLED")
                .auditTrailUrl("/api/v1/jobs/" + executionId + "/audit")
                .message("Job successfully cancelled.")
                .build();
    }

    // ── Retry ─────────────────────────────────────────────────────────────────

    @Transactional
    public JobExecutionResponse retryJob(String executionId) {
        ManualJobExecutionEntity original = findOrThrow(executionId);
        if (!RETRYABLE.contains(original.getStatus())) {
            throw JobExecutionApiException.conflict("INVALID_STATE_TRANSITION",
                    "INVALID_STATE_TRANSITION: Cannot retry job " + executionId + " — current status is " + original.getStatus());
        }
        String newId = generateExecutionId();
        ManualJobExecutionEntity clone = ManualJobExecutionEntity.builder()
                .executionId(newId)
                .configId(original.getConfigId())
                .jobName(original.getJobName())
                .status("STARTED")
                .triggerSource(original.getTriggerSource())
                .executionType("RETRY")
                .apiSource("API")
                .callbackUrl(original.getCallbackUrl())
                .callbackHeaders(original.getCallbackHeaders())
                .callbackStatus(original.getCallbackUrl() != null ? "PENDING" : "SKIPPED")
                .correlationId(java.util.UUID.randomUUID().toString())
                .executedBy(original.getExecutedBy())
                .build();
        executionRepository.save(clone);

        return JobExecutionResponse.builder()
                .originalExecutionId(executionId)
                .newExecutionId(newId)
                .executionId(newId)
                .status("STARTED")
                .submittedAt(Instant.now())
                .statusUrl("/api/v1/jobs/" + newId + "/status")
                .auditTrailUrl("/api/v1/jobs/" + newId + "/audit")
                .build();
    }

    // ── List recent ───────────────────────────────────────────────────────────

    public JobListResponse listRecent(String sourceSystem, String status,
                                     Instant fromDate, Instant toDate, int limit) {
        int effectiveLimit = Math.min(limit, 100);
        List<ManualJobExecutionEntity> rows = executionRepository
                .findRecentApiExecutions(sourceSystem, status, fromDate, toDate, effectiveLimit);
        List<JobListResponse.JobSummary> summaries = rows.stream()
                .map(e -> JobListResponse.JobSummary.builder()
                        .executionId(e.getExecutionId())
                        .jobConfigId(e.getConfigId())
                        .sourceSystem(e.getTriggerSource())
                        .status(e.getStatus())
                        .auditTrailUrl("/api/v1/jobs/" + e.getExecutionId() + "/audit")
                        .build())
                .collect(Collectors.toList());
        return JobListResponse.builder().total(summaries.size()).executions(summaries).build();
    }

    // ── Run All For Source ────────────────────────────────────────────────────

    @Transactional
    public RunAllJobsResponse runAllForSource(String sourceSystem, String actor) {
        List<ManualJobConfigEntity> activeConfigs =
                configRepository.findBySourceSystemAndStatus(sourceSystem, "ACTIVE");

        List<String> executionIds = new ArrayList<>();
        for (ManualJobConfigEntity config : activeConfigs) {
            JobExecutionRequest req = JobExecutionRequest.builder()
                    .jobConfigId(config.getConfigId())
                    .sourceSystem(sourceSystem)
                    .transformationRules(List.of("DEFAULT"))
                    .build();
            JobExecutionResponse resp = submitJob(req, actor);
            executionIds.add(resp.getExecutionId());
        }

        return RunAllJobsResponse.builder()
                .sourceSystem(sourceSystem)
                .submittedCount(executionIds.size())
                .executionIds(executionIds)
                .message(executionIds.size() + " job(s) submitted for source system: " + sourceSystem)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ManualJobExecutionEntity findOrThrow(String executionId) {
        return executionRepository.findById(executionId)
                .orElseThrow(() -> JobExecutionApiException.notFound(
                        "EXECUTION_NOT_FOUND",
                        "EXECUTION_NOT_FOUND: No execution found with id: " + executionId));
    }

    private void validateJobConfigExists(String jobConfigId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MANUAL_JOB_CONFIG WHERE CONFIG_ID = ?",
                Integer.class, jobConfigId);
        if (count == null || count == 0) {
            throw JobExecutionApiException.badRequest(
                    "JOB_CONFIG_NOT_FOUND",
                    "JOB_CONFIG_NOT_FOUND: Job config " + jobConfigId + " not found");
        }
    }

    private String generateExecutionId() {
        return "EXEC-" + String.format("%06d", System.currentTimeMillis() % 1_000_000);
    }

    private String serializeHeaders(java.util.Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(headers);
        } catch (Exception e) {
            return null;
        }
    }
}
