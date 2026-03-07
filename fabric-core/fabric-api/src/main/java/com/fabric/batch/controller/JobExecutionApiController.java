package com.fabric.batch.controller;

import com.fabric.batch.dto.jobexecution.*;
import com.fabric.batch.exception.JobExecutionApiException;
import com.fabric.batch.service.JobExecutionApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Job Execution API", description = "Programmatic job execution for external integrations")
public class JobExecutionApiController {

    private final JobExecutionApiService service;

    @Operation(summary = "Submit a job for execution")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Job submitted"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "409", description = "Job already running")
    })
    @PostMapping("/execute")
    @PreAuthorize("hasRole('API_EXECUTOR')")
    public ResponseEntity<JobExecutionResponse> execute(
            @Valid @RequestBody JobExecutionRequest request,
            Authentication auth) {
        String actor = auth != null ? auth.getName() : "unknown";
        log.info("Job execution request from actor={} configId={}", actor, request.getJobConfigId());
        JobExecutionResponse response = service.submitJob(request, actor);
        return ResponseEntity.accepted().body(response);
    }

    @Operation(summary = "Get job execution status")
    @GetMapping("/{executionId}/status")
    @PreAuthorize("hasRole('API_EXECUTOR')")
    public ResponseEntity<JobStatusResponse> status(@PathVariable String executionId) {
        return ResponseEntity.ok(service.getStatus(executionId));
    }

    @Operation(summary = "Cancel a submitted or running job")
    @PostMapping("/{executionId}/cancel")
    @PreAuthorize("hasRole('API_EXECUTOR')")
    public ResponseEntity<JobExecutionResponse> cancel(@PathVariable String executionId) {
        return ResponseEntity.ok(service.cancelJob(executionId));
    }

    @Operation(summary = "Retry a failed job")
    @PostMapping("/{executionId}/retry")
    @PreAuthorize("hasRole('API_EXECUTOR')")
    public ResponseEntity<JobExecutionResponse> retry(@PathVariable String executionId) {
        return ResponseEntity.accepted().body(service.retryJob(executionId));
    }

    @Operation(summary = "List recent job executions")
    @GetMapping("/recent")
    @PreAuthorize("hasRole('API_EXECUTOR')")
    public ResponseEntity<JobListResponse> recent(
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(service.listRecent(sourceSystem, status, fromDate, toDate, limit));
    }

    @Operation(summary = "Get the audit trail for a job execution")
    @GetMapping("/{executionId}/audit")
    @PreAuthorize("hasRole('API_EXECUTOR')")
    public ResponseEntity<JobAuditResponse> audit(@PathVariable String executionId) {
        return ResponseEntity.ok(JobAuditResponse.builder()
                .executionId(executionId)
                .auditEntries(java.util.List.of())
                .build());
    }

    @Operation(summary = "Run all active jobs for a source system")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Jobs submitted"),
        @ApiResponse(responseCode = "400", description = "sourceSystem is required")
    })
    @PostMapping("/run-all")
    @PreAuthorize("hasRole('API_EXECUTOR')")
    public ResponseEntity<RunAllJobsResponse> runAll(
            @RequestParam String sourceSystem,
            Authentication auth) {
        if (sourceSystem.isBlank()) {
            throw JobExecutionApiException.badRequest("MISSING_PARAM", "sourceSystem is required");
        }
        String actor = auth != null ? auth.getName() : "unknown";
        log.info("Run-all request from actor={} sourceSystem={}", actor, sourceSystem);
        return ResponseEntity.accepted().body(service.runAllForSource(sourceSystem, actor));
    }
}
