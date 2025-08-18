package com.truist.batch.controller;

import com.truist.batch.dto.MasterQueryRequest;
import com.truist.batch.dto.MasterQueryResponse;
import com.truist.batch.service.MasterQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * =========================================================================
 * MASTER QUERY CONTROLLER - BANKING GRADE SECURITY
 * =========================================================================
 * 
 * Purpose: REST API controller for Master Query operations with banking-grade security
 * - Role-based access control (RBAC) for all operations
 * - Comprehensive OpenAPI documentation with examples
 * - SOX-compliant audit trail integration
 * - Enterprise error handling and validation
 * 
 * Security Features:
 * - Method-level security with @PreAuthorize annotations
 * - JWT authentication and authorization
 * - Input validation and sanitization
 * - Secure error handling without sensitive data exposure
 * - Rate limiting and request throttling (configured externally)
 * 
 * Enterprise Standards:
 * - RESTful API design with proper HTTP status codes
 * - Comprehensive OpenAPI 3.0 documentation
 * - Banking compliance with regulatory requirements
 * - Correlation ID propagation for audit trails
 * 
 * Authorized Roles:
 * - JOB_VIEWER: Read-only query operations
 * - JOB_CREATOR: Create and validate queries
 * - JOB_MODIFIER: Modify existing queries
 * - JOB_EXECUTOR: Execute queries and view results
 * - ADMIN: All operations including statistics and management
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Phase 1 - Master Query Integration
 * =========================================================================
 */
@RestController
@RequestMapping("/api/v2/master-query")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Master Query Operations", 
     description = "Banking-grade master query operations with read-only execution, parameter validation, and SOX compliance")
@SecurityRequirement(name = "bearerAuth")
public class MasterQueryController {

    private final MasterQueryService masterQueryService;

    /**
     * Execute a master query with banking-grade security validation.
     */
    @PostMapping("/execute")
    @PreAuthorize("hasAnyRole('JOB_EXECUTOR', 'JOB_VIEWER', 'ADMIN')")
    @Operation(
        summary = "Execute Master Query",
        description = "Execute a parameterized master query with banking-grade security validation. " +
                     "Only SELECT and WITH queries are allowed. Query execution is limited to 30 seconds " +
                     "and results are capped at 100 rows for banking compliance.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Master query execution request with SQL, parameters, and metadata",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MasterQueryRequest.class),
                examples = @ExampleObject(
                    name = "Transaction Summary Query",
                    description = "Example query to get transaction summary by account",
                    value = """
                        {
                          "masterQueryId": "mq_transaction_summary_20250818",
                          "queryName": "Transaction Summary Report",
                          "querySql": "SELECT account_id, SUM(amount) as total FROM transactions WHERE batch_date = :batchDate AND amount >= :minAmount GROUP BY account_id ORDER BY total DESC",
                          "queryDescription": "Summarizes transaction amounts by account for specified batch date",
                          "queryParameters": {
                            "batchDate": "2025-08-18",
                            "minAmount": 100.00
                          },
                          "maxExecutionTimeSeconds": 30,
                          "maxResultRows": 100,
                          "securityClassification": "INTERNAL",
                          "dataClassification": "SENSITIVE",
                          "businessJustification": "Required for daily transaction reconciliation and regulatory reporting",
                          "complianceTags": ["SOX", "PCI_DSS"]
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Query executed successfully",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = MasterQueryResponse.class),
                examples = @ExampleObject(
                    name = "Successful Query Response",
                    value = """
                        {
                          "masterQueryId": "mq_transaction_summary_20250818",
                          "executionStatus": "SUCCESS",
                          "queryName": "Transaction Summary Report",
                          "results": [
                            {"account_id": "ACC123", "total": 1500.00},
                            {"account_id": "ACC456", "total": 2300.50}
                          ],
                          "rowCount": 2,
                          "executionTimeMs": 1250,
                          "executedBy": "john.analyst",
                          "userRole": "JOB_EXECUTOR",
                          "correlationId": "corr_12345678-abcd-1234-5678-123456789012",
                          "securityClassification": "INTERNAL",
                          "dataClassification": "SENSITIVE"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid query request or validation failure",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Validation Error",
                    value = """
                        {
                          "error": "VALIDATION_ERROR",
                          "message": "Query validation failed: Only SELECT and WITH queries are allowed",
                          "correlationId": "corr_12345678-abcd-1234-5678-123456789012",
                          "timestamp": "2025-08-18T10:30:45.123Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Insufficient permissions for query execution",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "408", 
            description = "Query execution timeout",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error during query execution",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<MasterQueryResponse> executeQuery(
            @Valid @RequestBody MasterQueryRequest request,
            @Parameter(description = "User role extracted from JWT token", hidden = true) 
            @RequestHeader(value = "X-User-Role", defaultValue = "JOB_EXECUTOR") String userRole) {
        
        log.info("Received master query execution request - ID: {}, User: {}", 
                request.getMasterQueryId(), userRole);
        
        try {
            MasterQueryResponse response = masterQueryService.executeQuery(request, userRole);
            
            HttpStatus status = response.isSuccessful() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid query request - ID: {}, Error: {}", request.getMasterQueryId(), e.getMessage());
            
            MasterQueryResponse errorResponse = MasterQueryResponse.builder()
                .masterQueryId(request.getMasterQueryId())
                .executionStatus("VALIDATION_FAILED")
                .errorInfo(Map.of(
                    "errorCode", "VALIDATION_ERROR",
                    "errorMessage", e.getMessage()
                ))
                .build();
                
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            log.error("Unexpected error during query execution - ID: {}", request.getMasterQueryId(), e);
            
            MasterQueryResponse errorResponse = MasterQueryResponse.builder()
                .masterQueryId(request.getMasterQueryId())
                .executionStatus("FAILED")
                .errorInfo(Map.of(
                    "errorCode", "INTERNAL_ERROR",
                    "errorMessage", "An unexpected error occurred during query execution"
                ))
                .build();
                
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Validate a master query without executing it.
     */
    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('JOB_CREATOR', 'JOB_MODIFIER', 'JOB_EXECUTOR', 'ADMIN')")
    @Operation(
        summary = "Validate Master Query",
        description = "Validate a master query for syntax, security, and business rules without executing it. " +
                     "Performs comprehensive validation including SQL injection protection, parameter validation, " +
                     "and banking compliance checks.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Master query validation request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MasterQueryRequest.class)
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Query validation completed",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Validation Success",
                    value = """
                        {
                          "valid": true,
                          "correlationId": "corr_12345678-abcd-1234-5678-123456789012",
                          "message": "Query validation passed",
                          "validatedAt": "2025-08-18T10:30:45.123Z",
                          "validatedBy": "JOB_CREATOR",
                          "syntaxValid": true,
                          "businessRules": {
                            "complexityCheck": true,
                            "parameterConsistency": true,
                            "securityClassification": "INTERNAL"
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid validation request"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions for query validation")
    })
    public ResponseEntity<Map<String, Object>> validateQuery(
            @Valid @RequestBody MasterQueryRequest request,
            @Parameter(description = "User role extracted from JWT token", hidden = true)
            @RequestHeader(value = "X-User-Role", defaultValue = "JOB_CREATOR") String userRole) {
        
        log.info("Received master query validation request - ID: {}, User: {}", 
                request.getMasterQueryId(), userRole);
        
        try {
            Map<String, Object> validationResults = masterQueryService.validateQuery(request, userRole);
            return ResponseEntity.ok(validationResults);
            
        } catch (Exception e) {
            log.error("Error during query validation - ID: {}", request.getMasterQueryId(), e);
            
            Map<String, Object> errorResponse = Map.of(
                "valid", false,
                "error", e.getMessage(),
                "validatedAt", Instant.now(),
                "validatedBy", userRole
            );
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get column metadata for a query.
     */
    @PostMapping("/metadata")
    @PreAuthorize("hasAnyRole('JOB_VIEWER', 'JOB_CREATOR', 'JOB_MODIFIER', 'ADMIN')")
    @Operation(
        summary = "Get Query Column Metadata",
        description = "Retrieve column metadata for a master query without executing it. " +
                     "Returns column names, types, lengths, and other metadata information.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Master query for metadata extraction",
            required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MasterQueryRequest.class))
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Column metadata retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Column Metadata Response",
                    value = """
                        [
                          {
                            "name": "account_id",
                            "type": "VARCHAR2",
                            "length": 20,
                            "nullable": false,
                            "order": 1
                          },
                          {
                            "name": "total",
                            "type": "NUMBER",
                            "precision": 15,
                            "scale": 2,
                            "nullable": true,
                            "order": 2
                          }
                        ]
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid metadata request"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<List<Map<String, Object>>> getQueryColumnMetadata(
            @Valid @RequestBody MasterQueryRequest request,
            @Parameter(description = "User role extracted from JWT token", hidden = true)
            @RequestHeader(value = "X-User-Role", defaultValue = "JOB_VIEWER") String userRole) {
        
        log.info("Received column metadata request - ID: {}, User: {}", 
                request.getMasterQueryId(), userRole);
        
        try {
            List<Map<String, Object>> metadata = masterQueryService.getQueryColumnMetadata(request, userRole);
            return ResponseEntity.ok(metadata);
            
        } catch (Exception e) {
            log.error("Error retrieving column metadata - ID: {}", request.getMasterQueryId(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Test database connectivity.
     */
    @GetMapping("/connectivity/test")
    @PreAuthorize("hasAnyRole('JOB_VIEWER', 'ADMIN')")
    @Operation(
        summary = "Test Database Connectivity",
        description = "Test read-only database connectivity for master query operations. " +
                     "Returns connection health, response time, and configuration details.",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Connectivity test completed",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Connectivity Test Response",
                        value = """
                            {
                              "healthy": true,
                              "responseTimeMs": 125,
                              "testedAt": "2025-08-18T10:30:45.123Z",
                              "connectionType": "READ_ONLY",
                              "maxTimeout": 30,
                              "maxRows": 100
                            }
                            """
                    )
                )
            )
        }
    )
    public ResponseEntity<Map<String, Object>> testConnectivity(
            @Parameter(description = "User role extracted from JWT token", hidden = true)
            @RequestHeader(value = "X-User-Role", defaultValue = "JOB_VIEWER") String userRole) {
        
        log.info("Received connectivity test request - User: {}", userRole);
        
        try {
            Map<String, Object> connectivityResults = masterQueryService.testConnectivity(userRole);
            return ResponseEntity.ok(connectivityResults);
            
        } catch (Exception e) {
            log.error("Error during connectivity test", e);
            
            Map<String, Object> errorResponse = Map.of(
                "healthy", false,
                "error", e.getMessage(),
                "testedAt", Instant.now()
            );
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }

    /**
     * Get query execution statistics.
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get Query Execution Statistics",
        description = "Retrieve query execution statistics for monitoring and performance analysis. " +
                     "Available only to administrators. Statistics period cannot exceed 90 days.",
        parameters = {
            @Parameter(
                name = "startTime",
                description = "Statistics period start time (ISO 8601 format)",
                required = true,
                example = "2025-08-18T00:00:00Z"
            ),
            @Parameter(
                name = "endTime",
                description = "Statistics period end time (ISO 8601 format)",
                required = true,
                example = "2025-08-18T23:59:59Z"
            )
        }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Statistics retrieved successfully",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(responseCode = "400", description = "Invalid time period"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<Map<String, Object>> getExecutionStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @Parameter(description = "User role extracted from JWT token", hidden = true)
            @RequestHeader(value = "X-User-Role", defaultValue = "ADMIN") String userRole) {
        
        log.info("Received statistics request - Period: {} to {}, User: {}", startTime, endTime, userRole);
        
        try {
            Map<String, Object> statistics = masterQueryService.getExecutionStatistics(startTime, endTime, userRole);
            return ResponseEntity.ok(statistics);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid statistics request - Error: {}", e.getMessage());
            
            Map<String, Object> errorResponse = Map.of(
                "error", "INVALID_REQUEST",
                "message", e.getMessage(),
                "timestamp", Instant.now()
            );
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            log.error("Error retrieving statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get available database schemas and tables.
     */
    @GetMapping("/schemas")
    @PreAuthorize("hasAnyRole('JOB_CREATOR', 'JOB_MODIFIER', 'ADMIN')")
    @Operation(
        summary = "Get Available Database Schemas",
        description = "Retrieve available database schemas and tables for query building. " +
                     "Returns schema information based on user permissions and access levels.",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Schema information retrieved successfully",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Schema Information Response",
                        value = """
                            {
                              "schemas": [
                                {
                                  "schemaName": "CM3INT",
                                  "description": "Core banking integration schema",
                                  "accessLevel": "READ_ONLY",
                                  "tables": [
                                    "MANUAL_JOB_CONFIG",
                                    "MANUAL_JOB_EXECUTION",
                                    "TEMPLATE_MASTER_QUERY_MAPPING"
                                  ]
                                }
                              ],
                              "userRole": "JOB_CREATOR",
                              "retrievedAt": "2025-08-18T10:30:45.123Z"
                            }
                            """
                    )
                )
            )
        }
    )
    public ResponseEntity<Map<String, Object>> getAvailableSchemas(
            @Parameter(description = "User role extracted from JWT token", hidden = true)
            @RequestHeader(value = "X-User-Role", defaultValue = "JOB_CREATOR") String userRole) {
        
        log.info("Received schemas request - User: {}", userRole);
        
        try {
            Map<String, Object> schemas = masterQueryService.getAvailableSchemas(userRole);
            return ResponseEntity.ok(schemas);
            
        } catch (Exception e) {
            log.error("Error retrieving schemas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint for master query operations.
     */
    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('JOB_VIEWER', 'ADMIN')")
    @Operation(
        summary = "Master Query Service Health Check",
        description = "Health check endpoint for master query operations. " +
                     "Tests database connectivity and service availability.",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Service is healthy",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Health Check Response",
                        value = """
                            {
                              "status": "UP",
                              "service": "MasterQueryService",
                              "database": "HEALTHY",
                              "timestamp": "2025-08-18T10:30:45.123Z",
                              "version": "1.0"
                            }
                            """
                    )
                )
            ),
            @ApiResponse(responseCode = "503", description = "Service is unhealthy")
        }
    )
    public ResponseEntity<Map<String, Object>> healthCheck(
            @Parameter(description = "User role extracted from JWT token", hidden = true)
            @RequestHeader(value = "X-User-Role", defaultValue = "JOB_VIEWER") String userRole) {
        
        try {
            Map<String, Object> connectivity = masterQueryService.testConnectivity(userRole);
            boolean healthy = (Boolean) connectivity.getOrDefault("healthy", false);
            
            Map<String, Object> health = Map.of(
                "status", healthy ? "UP" : "DOWN",
                "service", "MasterQueryService",
                "database", healthy ? "HEALTHY" : "UNHEALTHY",
                "timestamp", Instant.now(),
                "version", "1.0"
            );
            
            HttpStatus status = healthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).body(health);
            
        } catch (Exception e) {
            log.error("Health check failed", e);
            
            Map<String, Object> health = Map.of(
                "status", "DOWN",
                "service", "MasterQueryService",
                "database", "ERROR",
                "timestamp", Instant.now(),
                "error", e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
}