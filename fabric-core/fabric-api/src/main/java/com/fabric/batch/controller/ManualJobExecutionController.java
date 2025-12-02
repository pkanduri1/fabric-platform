package com.fabric.batch.controller;

import com.fabric.batch.dto.JobExecutionRequest;
import com.fabric.batch.service.JobExecutionService;
import com.fabric.batch.service.JobExecutionService.JobExecutionResult;
import com.fabric.batch.service.JobExecutionService.JobExecutionStatus;
import com.fabric.batch.service.JobExecutionService.JobCancellationResult;
import com.fabric.batch.service.JobExecutionService.ExecutionStatistics;
import com.fabric.batch.security.jwt.JwtTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * REST Controller for Manual Job Execution Management.
 *
 * Enterprise-grade REST API implementing comprehensive job execution operations
 * with JWT authentication, RBAC authorization, real-time monitoring, and
 * SOX-compliant audit logging for banking applications.
 *
 * Security Features:
 * - JWT token-based authentication
 * - Role-based access control (RBAC)
 * - Method-level security annotations
 * - Execution context validation
 * - Comprehensive audit logging
 *
 * API Features:
 * - RESTful endpoints with OpenAPI documentation
 * - Real-time execution status tracking
 * - Execution history and monitoring
 * - Retry and cancellation support
 * - Performance metrics and statistics
 *
 * Banking Compliance:
 * - SOX-compliant execution audit trails
 * - Regulatory approval validation
 * - Data lineage tracking
 * - Risk assessment integration
 *
 * RBAC Roles:
 * - JOB_EXECUTOR: Execute jobs and view execution status
 * - JOB_MODIFIER: Execute, retry, and cancel job executions
 * - JOB_VIEWER: Read-only access to execution history
 *
 * @author Senior Full Stack Developer Agent
 * @version 2.0
 * @since US001 Phase 2 - Job Execution REST API
 */
@RestController
@RequestMapping("/api/v2/manual-job-execution")
@RequiredArgsConstructor
@Slf4j
@Validated
@CrossOrigin(origins = {"http://localhost:3000", "https://localhost:3000"})
@Tag(name = "Manual Job Execution",
     description = "Enterprise API for manual job execution with banking-grade security and compliance")
@SecurityRequirement(name = "bearerAuth")
public class ManualJobExecutionController {

    private final JobExecutionService jobExecutionService;
    private final JwtTokenService jwtTokenService;

    /**
     * Execute a manual job configuration.
     *
     * Requires JOB_EXECUTOR or JOB_MODIFIER role.
     * Implements comprehensive validation, execution tracking, and audit logging.
     */
    @PostMapping("/execute/{configId}")
    @PreAuthorize("hasRole('JOB_EXECUTOR') or hasRole('JOB_MODIFIER')")
    @Operation(
        summary = "Execute Manual Job Configuration",
        description = "Execute a manual job configuration with comprehensive monitoring and audit trail. Requires JOB_EXECUTOR or JOB_MODIFIER role.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Job execution started successfully",
                    content = @Content(schema = @Schema(implementation = JobExecutionResult.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Configuration not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> executeJob(
            @PathVariable
            @Parameter(description = "Configuration ID to execute", example = "cfg_encore_123")
            @NotBlank @Size(min = 10, max = 50) String configId,

            @Valid @RequestBody JobExecutionRequest request,
            HttpServletRequest httpRequest) {

        String correlationId = generateCorrelationId();
        String username = extractUsernameFromToken(httpRequest);

        log.info("Starting job execution - configId: {}, user: {}, correlationId: {}",
                configId, username, correlationId);

        try {
            // Set configId from path variable
            request.setConfigId(configId);

            // Set correlation ID if not provided
            if (request.getCorrelationId() == null) {
                request.setCorrelationId(correlationId);
            }

            // Set trigger source if not provided
            if (request.getTriggerSource() == null) {
                request.setTriggerSource("USER_INTERFACE");
            }

            // Execute the job
            JobExecutionResult result = jobExecutionService.executeJob(request, username);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("executionId", result.getExecutionId());
            response.put("configId", result.getConfigId());
            response.put("jobName", result.getJobName());
            response.put("status", result.getStatus());
            response.put("startTime", result.getStartTime());
            response.put("executedBy", result.getExecutedBy());
            response.put("correlationId", result.getCorrelationId());
            response.put("executionType", result.getExecutionType());
            response.put("message", "Job execution started successfully");

            log.info("Job execution started successfully - executionId: {}, configId: {}, correlationId: {}",
                    result.getExecutionId(), configId, correlationId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid execution request - configId: {}, error: {}, correlationId: {}",
                    configId, e.getMessage(), correlationId);

            Map<String, Object> errorResponse = createErrorResponse(
                "Invalid execution request: " + e.getMessage(), correlationId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (IllegalStateException e) {
            log.warn("Cannot execute job - configId: {}, error: {}, correlationId: {}",
                    configId, e.getMessage(), correlationId);

            Map<String, Object> errorResponse = createErrorResponse(
                "Cannot execute job: " + e.getMessage(), correlationId);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

        } catch (Exception e) {
            log.error("Job execution failed - configId: {}, error: {}, correlationId: {}",
                     configId, e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = createErrorResponse(
                "Job execution failed: " + e.getMessage(), correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get execution status for a specific execution.
     *
     * Requires JOB_EXECUTOR, JOB_MODIFIER, or JOB_VIEWER role.
     */
    @GetMapping("/status/{executionId}")
    @PreAuthorize("hasAnyRole('JOB_EXECUTOR', 'JOB_MODIFIER', 'JOB_VIEWER')")
    @Operation(
        summary = "Get Job Execution Status",
        description = "Retrieve real-time status of a job execution with progress tracking",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Execution status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = JobExecutionStatus.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Execution not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getExecutionStatus(
            @PathVariable
            @Parameter(description = "Execution ID", example = "exec_encore_1732191025000_12345678")
            @NotBlank @Size(min = 10, max = 50) String executionId,
            HttpServletRequest httpRequest) {

        String correlationId = generateCorrelationId();
        String username = extractUsernameFromToken(httpRequest);

        log.debug("Retrieving execution status - executionId: {}, user: {}, correlationId: {}",
                 executionId, username, correlationId);

        try {
            Optional<JobExecutionStatus> statusOpt = jobExecutionService.getExecutionStatus(executionId);

            if (!statusOpt.isPresent()) {
                log.warn("Execution not found - executionId: {}, correlationId: {}",
                        executionId, correlationId);

                Map<String, Object> errorResponse = createErrorResponse(
                    "Execution not found: " + executionId, correlationId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            JobExecutionStatus status = statusOpt.get();

            Map<String, Object> response = new HashMap<>();
            response.put("executionId", status.getExecutionId());
            response.put("configId", status.getConfigId());
            response.put("jobName", status.getJobName());
            response.put("status", status.getStatus());
            response.put("startTime", status.getStartTime());
            response.put("endTime", status.getEndTime());
            response.put("durationSeconds", status.getDurationSeconds());
            response.put("executedBy", status.getExecutedBy());
            response.put("progressPercentage", status.getProgressPercentage());
            response.put("currentStep", status.getCurrentStep());
            response.put("recordsProcessed", status.getRecordsProcessed());
            response.put("recordsSuccess", status.getRecordsSuccess());
            response.put("recordsError", status.getRecordsError());
            response.put("errorMessage", status.getErrorMessage());
            response.put("correlationId", correlationId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving execution status - executionId: {}, error: {}, correlationId: {}",
                     executionId, e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = createErrorResponse(
                "Failed to retrieve execution status: " + e.getMessage(), correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get execution history for a specific configuration.
     *
     * Requires JOB_EXECUTOR, JOB_MODIFIER, or JOB_VIEWER role.
     */
    @GetMapping("/history/{configId}")
    @PreAuthorize("hasAnyRole('JOB_EXECUTOR', 'JOB_MODIFIER', 'JOB_VIEWER')")
    @Operation(
        summary = "Get Job Execution History",
        description = "Retrieve execution history for a specific job configuration",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Execution history retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getExecutionHistory(
            @PathVariable
            @Parameter(description = "Configuration ID", example = "cfg_encore_123")
            @NotBlank @Size(min = 10, max = 50) String configId,

            @RequestParam(defaultValue = "10")
            @Parameter(description = "Maximum number of executions to return", example = "10")
            @Min(1) @Max(100) int limit,

            HttpServletRequest httpRequest) {

        String correlationId = generateCorrelationId();
        String username = extractUsernameFromToken(httpRequest);

        log.debug("Retrieving execution history - configId: {}, limit: {}, user: {}, correlationId: {}",
                 configId, limit, username, correlationId);

        try {
            List<JobExecutionStatus> history = jobExecutionService.getExecutionHistory(configId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("configId", configId);
            response.put("executions", history);
            response.put("totalReturned", history.size());
            response.put("limit", limit);
            response.put("correlationId", correlationId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving execution history - configId: {}, error: {}, correlationId: {}",
                     configId, e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = createErrorResponse(
                "Failed to retrieve execution history: " + e.getMessage(), correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Cancel a running job execution.
     *
     * Requires JOB_MODIFIER role.
     */
    @PostMapping("/cancel/{executionId}")
    @PreAuthorize("hasRole('JOB_MODIFIER')")
    @Operation(
        summary = "Cancel Job Execution",
        description = "Cancel a running job execution with audit trail. Requires JOB_MODIFIER role.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Job execution cancelled successfully",
                    content = @Content(schema = @Schema(implementation = JobCancellationResult.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or execution cannot be cancelled"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Execution not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> cancelExecution(
            @PathVariable
            @Parameter(description = "Execution ID to cancel", example = "exec_encore_1732191025000_12345678")
            @NotBlank @Size(min = 10, max = 50) String executionId,

            @RequestParam(required = false)
            @Parameter(description = "Cancellation reason", example = "User requested cancellation")
            @Size(max = 500) String reason,

            HttpServletRequest httpRequest) {

        String correlationId = generateCorrelationId();
        String username = extractUsernameFromToken(httpRequest);

        log.info("Cancelling job execution - executionId: {}, user: {}, reason: {}, correlationId: {}",
                executionId, username, reason, correlationId);

        try {
            JobCancellationResult result = jobExecutionService.cancelExecution(
                executionId, username, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("executionId", result.getExecutionId());
            response.put("cancelled", result.isCancelled());
            response.put("cancelledBy", result.getCancelledBy());
            response.put("cancelledAt", result.getCancelledAt());
            response.put("cancellationReason", result.getCancellationReason());
            response.put("correlationId", correlationId);

            if (result.isCancelled()) {
                log.info("Job execution cancelled successfully - executionId: {}, correlationId: {}",
                        executionId, correlationId);
                response.put("message", "Job execution cancelled successfully");
                return ResponseEntity.ok(response);
            } else {
                log.warn("Failed to cancel job execution - executionId: {}, error: {}, correlationId: {}",
                        executionId, result.getErrorMessage(), correlationId);
                response.put("message", "Failed to cancel job execution");
                response.put("errorMessage", result.getErrorMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (IllegalArgumentException e) {
            log.warn("Execution not found - executionId: {}, correlationId: {}",
                    executionId, correlationId);

            Map<String, Object> errorResponse = createErrorResponse(
                "Execution not found: " + executionId, correlationId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

        } catch (IllegalStateException e) {
            log.warn("Cannot cancel execution - executionId: {}, error: {}, correlationId: {}",
                    executionId, e.getMessage(), correlationId);

            Map<String, Object> errorResponse = createErrorResponse(
                "Cannot cancel execution: " + e.getMessage(), correlationId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            log.error("Error cancelling execution - executionId: {}, error: {}, correlationId: {}",
                     executionId, e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = createErrorResponse(
                "Failed to cancel execution: " + e.getMessage(), correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retry a failed job execution.
     *
     * Requires JOB_MODIFIER role.
     */
    @PostMapping("/retry/{executionId}")
    @PreAuthorize("hasRole('JOB_MODIFIER')")
    @Operation(
        summary = "Retry Failed Job Execution",
        description = "Retry a failed job execution with optional parameter modifications. Requires JOB_MODIFIER role.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Job retry started successfully",
                    content = @Content(schema = @Schema(implementation = JobExecutionResult.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or execution cannot be retried"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Execution not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> retryExecution(
            @PathVariable
            @Parameter(description = "Original execution ID to retry", example = "exec_encore_1732191025000_12345678")
            @NotBlank @Size(min = 10, max = 50) String executionId,

            @Valid @RequestBody(required = false) JobExecutionRequest retryRequest,
            HttpServletRequest httpRequest) {

        String correlationId = generateCorrelationId();
        String username = extractUsernameFromToken(httpRequest);

        log.info("Retrying job execution - originalExecutionId: {}, user: {}, correlationId: {}",
                executionId, username, correlationId);

        try {
            // Create retry request if not provided
            if (retryRequest == null) {
                retryRequest = new JobExecutionRequest();
            }

            JobExecutionResult result = jobExecutionService.retryExecution(
                executionId, retryRequest, username);

            Map<String, Object> response = new HashMap<>();
            response.put("newExecutionId", result.getExecutionId());
            response.put("originalExecutionId", executionId);
            response.put("configId", result.getConfigId());
            response.put("jobName", result.getJobName());
            response.put("status", result.getStatus());
            response.put("startTime", result.getStartTime());
            response.put("retriedBy", result.getExecutedBy());
            response.put("correlationId", result.getCorrelationId());
            response.put("message", "Job retry started successfully");

            log.info("Job retry started successfully - newExecutionId: {}, originalExecutionId: {}, correlationId: {}",
                    result.getExecutionId(), executionId, correlationId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Execution not found - executionId: {}, correlationId: {}",
                    executionId, correlationId);

            Map<String, Object> errorResponse = createErrorResponse(
                "Execution not found: " + executionId, correlationId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

        } catch (IllegalStateException e) {
            log.warn("Cannot retry execution - executionId: {}, error: {}, correlationId: {}",
                    executionId, e.getMessage(), correlationId);

            Map<String, Object> errorResponse = createErrorResponse(
                "Cannot retry execution: " + e.getMessage(), correlationId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            log.error("Error retrying execution - executionId: {}, error: {}, correlationId: {}",
                     executionId, e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = createErrorResponse(
                "Failed to retry execution: " + e.getMessage(), correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get active executions across all configurations.
     *
     * Requires JOB_EXECUTOR, JOB_MODIFIER, or JOB_VIEWER role.
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('JOB_EXECUTOR', 'JOB_MODIFIER', 'JOB_VIEWER')")
    @Operation(
        summary = "Get Active Job Executions",
        description = "Retrieve all currently active job executions for monitoring",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Active executions retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getActiveExecutions(HttpServletRequest httpRequest) {

        String correlationId = generateCorrelationId();
        String username = extractUsernameFromToken(httpRequest);

        log.debug("Retrieving active executions - user: {}, correlationId: {}", username, correlationId);

        try {
            List<JobExecutionStatus> activeExecutions = jobExecutionService.getActiveExecutions();

            Map<String, Object> response = new HashMap<>();
            response.put("activeExecutions", activeExecutions);
            response.put("totalActive", activeExecutions.size());
            response.put("retrievedAt", LocalDateTime.now());
            response.put("correlationId", correlationId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving active executions - error: {}, correlationId: {}",
                     e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = createErrorResponse(
                "Failed to retrieve active executions: " + e.getMessage(), correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get execution statistics and performance metrics.
     *
     * Requires JOB_EXECUTOR, JOB_MODIFIER, or JOB_VIEWER role.
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('JOB_EXECUTOR', 'JOB_MODIFIER', 'JOB_VIEWER')")
    @Operation(
        summary = "Get Execution Statistics",
        description = "Retrieve execution statistics and performance metrics for monitoring dashboard",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ExecutionStatistics.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getExecutionStatistics(HttpServletRequest httpRequest) {

        String correlationId = generateCorrelationId();
        String username = extractUsernameFromToken(httpRequest);

        log.debug("Retrieving execution statistics - user: {}, correlationId: {}", username, correlationId);

        try {
            ExecutionStatistics stats = jobExecutionService.getExecutionStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("totalExecutions", stats.getTotalExecutions());
            response.put("successfulExecutions", stats.getSuccessfulExecutions());
            response.put("failedExecutions", stats.getFailedExecutions());
            response.put("cancelledExecutions", stats.getCancelledExecutions());
            response.put("activeExecutions", stats.getActiveExecutions());
            response.put("averageExecutionDurationSeconds", stats.getAverageExecutionDurationSeconds());
            response.put("successRate", stats.getSuccessRate());
            response.put("lastUpdated", stats.getLastUpdated());
            response.put("correlationId", correlationId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving execution statistics - error: {}, correlationId: {}",
                     e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = createErrorResponse(
                "Failed to retrieve execution statistics: " + e.getMessage(), correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Private helper methods

    private String extractUsernameFromToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        return token != null ? jwtTokenService.extractUsername(token) : "anonymous";
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    private Map<String, Object> createErrorResponse(String message, String correlationId) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("correlationId", correlationId);
        errorResponse.put("timestamp", LocalDateTime.now());
        return errorResponse;
    }
}
