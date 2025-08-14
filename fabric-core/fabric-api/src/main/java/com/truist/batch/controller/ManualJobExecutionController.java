package com.truist.batch.controller;

import com.truist.batch.service.ManualJobExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for manual batch job execution.
 * Provides endpoints to trigger and monitor batch jobs using saved configurations.
 * 
 * @author Senior Full Stack Developer Agent
 * @since Phase 3 - Batch Execution Enhancement
 */
@RestController
@RequestMapping("/api/v2/manual-job-execution")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Manual Job Execution", description = "APIs for manually triggering and monitoring batch job executions")
@SecurityRequirement(name = "bearerAuth")
public class ManualJobExecutionController {
    
    private final ManualJobExecutionService executionService;
    
    /**
     * Manually trigger a batch job execution using saved configuration
     */
    @PostMapping("/execute/{configId}")
    @PreAuthorize("hasAnyRole('JOB_EXECUTOR', 'JOB_MODIFIER', 'ADMIN')")
    @Operation(summary = "Execute batch job", description = "Manually trigger a batch job using saved configuration from database")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job execution started successfully"),
        @ApiResponse(responseCode = "404", description = "Configuration not found"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "500", description = "Internal server error during job execution")
    })
    public ResponseEntity<Map<String, Object>> executeJob(
            @PathVariable @Parameter(description = "Configuration ID from MANUAL_JOB_CONFIG table") String configId,
            @RequestBody(required = false) @Parameter(description = "Additional job parameters") Map<String, String> parameters,
            Authentication authentication) {
        
        try {
            log.info("Manual job execution requested for configId: {} by user: {}", 
                    configId, authentication.getName());
            
            // Execute the job
            Map<String, Object> result = executionService.executeJobWithJsonConfig(
                configId, 
                authentication.getName(), 
                parameters
            );
            
            log.info("Job execution initiated successfully: {}", result.get("executionId"));
            
            return ResponseEntity.ok(result);
            
        } catch (RuntimeException e) {
            log.error("Failed to execute job for configId: {}", configId, e);
            
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Job execution failed", "message", e.getMessage()));
        }
    }
    
    /**
     * Get execution status for a job
     */
    @GetMapping("/status/{executionId}")
    @PreAuthorize("hasAnyRole('JOB_VIEWER', 'JOB_EXECUTOR', 'JOB_MODIFIER', 'ADMIN')")
    @Operation(summary = "Get execution status", description = "Retrieve the current status of a batch job execution")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Execution not found"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> getExecutionStatus(
            @PathVariable @Parameter(description = "Execution ID") String executionId) {
        
        try {
            log.debug("Status request for execution: {}", executionId);
            
            Map<String, Object> status = executionService.getExecutionStatus(executionId);
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("Failed to get status for execution: {}", executionId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Execution not found", "executionId", executionId));
        }
    }
    
    /**
     * Get master query for a job configuration
     */
    @GetMapping("/query/{sourceSystem}/{jobName}")
    @PreAuthorize("hasAnyRole('JOB_VIEWER', 'JOB_MODIFIER', 'ADMIN')")
    @Operation(summary = "Get master query", description = "Retrieve the master SQL query for a job configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Query retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Query not found"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> getMasterQuery(
            @PathVariable @Parameter(description = "Source system name") String sourceSystem,
            @PathVariable @Parameter(description = "Job name") String jobName) {
        
        try {
            log.debug("Master query request for {}/{}", sourceSystem, jobName);
            
            String query = executionService.getMasterQuery(sourceSystem, jobName);
            
            if (query != null) {
                return ResponseEntity.ok(Map.of(
                    "sourceSystem", sourceSystem,
                    "jobName", jobName,
                    "query", query
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Master query not found"));
            }
            
        } catch (Exception e) {
            log.error("Failed to get master query for {}/{}", sourceSystem, jobName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve master query"));
        }
    }
    
    /**
     * Test endpoint to verify controller is working
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verify the manual job execution API is operational")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "ManualJobExecutionController",
            "timestamp", System.currentTimeMillis()
        ));
    }
}