package com.fabric.batch.controller;

import com.fabric.batch.dto.MasterQueryRequest;
import com.fabric.batch.dto.MasterQueryResponse;
import com.fabric.batch.dto.MasterQueryConfigDTO;
import com.fabric.batch.dto.MasterQueryCreateRequest;
import com.fabric.batch.dto.MasterQueryUpdateRequest;
import com.fabric.batch.repository.MasterQueryRepository;
import com.fabric.batch.service.MasterQueryService;
import com.fabric.batch.service.MasterQueryValidationService;
import com.fabric.batch.service.SmartFieldMappingService;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private final SmartFieldMappingService smartFieldMappingService;

    /**
     * Get all available master query configurations from MASTER_QUERY_CONFIG table.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('JOB_VIEWER', 'JOB_CREATOR', 'JOB_MODIFIER', 'JOB_EXECUTOR', 'ADMIN')")
    @Operation(
        summary = "Get All Master Query Configurations",
        description = "Retrieve all available master query configurations from the MASTER_QUERY_CONFIG table. " +
                     "Returns active and inactive queries with comprehensive metadata for UI master query library. " +
                     "Results are filtered based on user permissions and data classification levels. " +
                     "Queries are sorted by business priority: active status, source system (ENCORE first), " +
                     "query name alphabetically, and version (latest first).",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Master query configurations retrieved successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MasterQueryConfigDTO.class),
                    examples = @ExampleObject(
                        name = "Master Query List Response",
                        description = "Example response showing master query configurations from database",
                        value = """
                            [
                              {
                                "id": 1,
                                "sourceSystem": "ENCORE",
                                "queryName": "atoctran_encore_200_job",
                                "queryType": "SELECT",
                                "querySql": "SELECT ACCT_NUM as acct_num, BATCH_DATE as batch_date, CCI as cci, CONTACT_ID as contact_id FROM ENCORE_TEST_DATA WHERE BATCH_DATE = TO_DATE(:batchDate, 'YYYY-MM-DD') ORDER BY ACCT_NUM",
                                "version": 1,
                                "isActive": "Y",
                                "createdBy": "system",
                                "createdDate": "2025-08-14T18:26:17.978",
                                "displayName": "atoctran_encore_200_job (v1) - ENCORE",
                                "dataClassification": "SENSITIVE",
                                "statusIndicator": "ACTIVE",
                                "complexityLevel": "LOW",
                                "parameterCount": 1,
                                "complianceRequirements": ["SOX", "FFIEC", "BASEL_III"]
                              }
                            ]
                            """
                    )
                )
            ),
            @ApiResponse(
                responseCode = "403", 
                description = "Insufficient permissions to view master query list",
                content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "Database error or internal server error",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Database Error Response",
                        value = """
                            {
                              "error": "DATABASE_ERROR",
                              "message": "Failed to retrieve master query list from database",
                              "correlationId": "corr_12345678-abcd-1234-5678-123456789012",
                              "timestamp": "2025-08-20T10:30:45.123Z"
                            }
                            """
                    )
                )
            )
        }
    )
    public ResponseEntity<List<MasterQueryConfigDTO>> getAllMasterQueries(
            @Parameter(description = "User role extracted from JWT token", hidden = true)
            @RequestHeader(value = "X-User-Role", defaultValue = "JOB_VIEWER") String userRole) {
        
        log.info("Received request to get all master query configurations - User: {}", userRole);
        
        try {
            List<MasterQueryConfigDTO> masterQueries = masterQueryService.getAllMasterQueries(userRole);
            
            log.info("Successfully retrieved {} master query configurations - User: {}", 
                    masterQueries.size(), userRole);
            
            return ResponseEntity.ok(masterQueries);
            
        } catch (MasterQueryRepository.QuerySecurityException e) {
            log.warn("Access denied for master query list - User: {}, Error: {}", userRole, e.getMessage());
            
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            
        } catch (MasterQueryRepository.QueryExecutionException e) {
            log.error("Database error retrieving master query list - User: {}, Error: {}, CorrelationId: {}", 
                     userRole, e.getErrorCode(), e.getCorrelationId());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for master query list - User: {}, Error: {}", userRole, e.getMessage());
            
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            log.error("Unexpected error retrieving master query list - User: {}", userRole, e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

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

    // =========================================================================
    // SMART FIELD MAPPING ENDPOINTS
    // =========================================================================

    /**
     * Generate smart field mappings for a master query with confidence scoring
     */
    @PostMapping("/{masterQueryId}/smart-mapping")
    @PreAuthorize("hasAnyRole('JOB_EXECUTOR', 'JOB_CREATOR', 'ADMIN')")
    @Operation(
        summary = "Generate Smart Field Mappings",
        description = "Generate intelligent field mappings based on column metadata analysis and banking domain patterns. " +
                     "Uses AI-powered pattern recognition with confidence scoring to suggest optimal field mappings " +
                     "for data transformation and integration tasks.",
        parameters = @Parameter(
            name = "masterQueryId",
            description = "Unique identifier of the master query to analyze",
            example = "mq_encore_account_summary"
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Smart field mappings generated successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Smart Mappings Response",
                    value = """
                        [
                          {
                            "sourceColumn": "account_id",
                            "targetField": "account-number",
                            "confidence": 0.95,
                            "detectedPattern": {
                              "fieldType": "ACCOUNT_NUMBER",
                              "description": "Bank account number field",
                              "maskingRequired": true
                            },
                            "suggestedTransformation": "mask",
                            "businessConcept": "Account Management",
                            "dataClassification": "SENSITIVE",
                            "complianceRequirements": ["PCI_DSS", "SOX", "GLBA"]
                          }
                        ]
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Master query not found"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<List<SmartFieldMappingService.SmartFieldMapping>> generateSmartFieldMappings(
            @PathVariable String masterQueryId,
            @RequestBody(required = false) Map<String, String> request,
            @Parameter(description = "User role extracted from JWT token", hidden = true)
            @RequestHeader(value = "X-User-Role", defaultValue = "JOB_EXECUTOR") String userRole) {
        
        log.info("Generating smart field mappings for master query: {} by user role: {}", masterQueryId, userRole);
        
        try {
            String targetSchema = request != null ? request.get("targetSchema") : null;
            List<SmartFieldMappingService.SmartFieldMapping> mappings = 
                smartFieldMappingService.generateSmartFieldMappings(masterQueryId, targetSchema);
            
            log.info("Generated {} smart field mappings for master query: {}", mappings.size(), masterQueryId);
            return ResponseEntity.ok(mappings);
            
        } catch (Exception e) {
            log.error("Failed to generate smart field mappings for master query: {}", masterQueryId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Validate and score field mappings
     */
    @PostMapping("/{masterQueryId}/validate-mappings")
    @PreAuthorize("hasAnyRole('JOB_EXECUTOR', 'JOB_CREATOR', 'ADMIN')")
    @Operation(
        summary = "Validate Field Mappings",
        description = "Validate and re-score field mappings with enhanced confidence analysis. " +
                     "Checks mapping consistency, data type compatibility, and compliance requirements.",
        parameters = @Parameter(
            name = "masterQueryId",
            description = "Unique identifier of the master query",
            example = "mq_encore_account_summary"
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Field mappings validated successfully",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(responseCode = "400", description = "Invalid mapping data"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<List<SmartFieldMappingService.SmartFieldMapping>> validateFieldMappings(
            @PathVariable String masterQueryId,
            @RequestBody Map<String, Object> request,
            @Parameter(description = "User role extracted from JWT token", hidden = true)
            @RequestHeader(value = "X-User-Role", defaultValue = "JOB_EXECUTOR") String userRole) {
        
        log.info("Validating field mappings for master query: {} by user role: {}", masterQueryId, userRole);
        
        try {
            @SuppressWarnings("unchecked")
            List<SmartFieldMappingService.SmartFieldMapping> mappings = 
                (List<SmartFieldMappingService.SmartFieldMapping>) request.get("mappings");
            
            if (mappings == null || mappings.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            List<SmartFieldMappingService.SmartFieldMapping> validatedMappings = 
                smartFieldMappingService.validateFieldMappings(masterQueryId, mappings);
            
            log.info("Validated {} field mappings for master query: {}", validatedMappings.size(), masterQueryId);
            return ResponseEntity.ok(validatedMappings);
            
        } catch (Exception e) {
            log.error("Failed to validate field mappings for master query: {}", masterQueryId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get banking field patterns for reference
     */
    @GetMapping("/patterns/banking")
    @PreAuthorize("hasAnyRole('JOB_VIEWER', 'JOB_EXECUTOR', 'JOB_CREATOR', 'ADMIN')")
    @Operation(
        summary = "Get Banking Field Patterns",
        description = "Retrieve available banking field patterns used for smart field mapping. " +
                     "Provides reference information for manual mapping configuration and validation."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Banking field patterns retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Banking Patterns Response",
                    value = """
                        {
                          "patterns": {
                            "ACCOUNT_NUMBER": {
                              "fieldType": "ACCOUNT_NUMBER",
                              "description": "Bank account number field",
                              "maskingRequired": true,
                              "baseConfidence": 0.95,
                              "complianceRequirements": ["PCI_DSS", "SOX", "GLBA"]
                            },
                            "TRANSACTION_ID": {
                              "fieldType": "TRANSACTION_ID", 
                              "description": "Transaction identifier field",
                              "maskingRequired": false,
                              "baseConfidence": 0.88,
                              "complianceRequirements": ["SOX", "FFIEC"]
                            }
                          },
                          "businessConcepts": [
                            "Account Management",
                            "Transaction Processing",
                            "Customer Information",
                            "Risk Assessment"
                          ]
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> getBankingFieldPatterns(
            @Parameter(description = "User role extracted from JWT token", hidden = true)
            @RequestHeader(value = "X-User-Role", defaultValue = "JOB_VIEWER") String userRole) {
        
        log.info("Retrieving banking field patterns for user role: {}", userRole);
        
        try {
            Map<String, Object> response = Map.of(
                "patterns", getBankingPatternsMap(),
                "businessConcepts", getBusinessConceptsList(),
                "dataClassifications", Arrays.asList("SENSITIVE", "INTERNAL", "PUBLIC"),
                "complianceStandards", Arrays.asList("PCI_DSS", "SOX", "GLBA", "BASEL_III", "GDPR", "CCPA")
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve banking field patterns", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper methods for banking patterns response
    private Map<String, Object> getBankingPatternsMap() {
        return Map.of(
            "ACCOUNT_NUMBER", Map.of(
                "fieldType", "ACCOUNT_NUMBER",
                "description", "Bank account number field",
                "maskingRequired", true,
                "baseConfidence", 0.95,
                "complianceRequirements", Arrays.asList("PCI_DSS", "SOX", "GLBA")
            ),
            "ROUTING_NUMBER", Map.of(
                "fieldType", "ROUTING_NUMBER",
                "description", "Bank routing number field",
                "maskingRequired", false,
                "baseConfidence", 0.90,
                "complianceRequirements", Arrays.asList("NACHA", "FED_REGULATIONS")
            ),
            "TRANSACTION_ID", Map.of(
                "fieldType", "TRANSACTION_ID",
                "description", "Transaction identifier field",
                "maskingRequired", false,
                "baseConfidence", 0.88,
                "complianceRequirements", Arrays.asList("SOX", "FFIEC")
            ),
            "AMOUNT", Map.of(
                "fieldType", "AMOUNT",
                "description", "Monetary amount field",
                "maskingRequired", true,
                "baseConfidence", 0.92,
                "complianceRequirements", Arrays.asList("SOX", "BASEL_III")
            ),
            "CUSTOMER_ID", Map.of(
                "fieldType", "CUSTOMER_ID",
                "description", "Customer identifier field",
                "maskingRequired", true,
                "baseConfidence", 0.93,
                "complianceRequirements", Arrays.asList("PII_PROTECTION", "GDPR", "CCPA")
            )
        );
    }

    private List<String> getBusinessConceptsList() {
        return Arrays.asList(
            "Account Management",
            "Transaction Processing", 
            "Customer Information",
            "Risk Assessment",
            "Regulatory Reporting",
            "Payment Processing",
            "Fraud Detection",
            "Credit Analysis",
            "Loan Management",
            "Investment Tracking"
        );
    }

    // =========================================================================
    // MASTER QUERY CRUD ENDPOINTS
    // =========================================================================

    /**
     * Create a new master query configuration.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('JOB_CREATOR', 'ADMIN')")
    @Operation(
        summary = "Create Master Query Configuration",
        description = "Create a new master query configuration with comprehensive validation and audit trail. " +
                     "Supports template-based creation and enforces banking compliance requirements. " +
                     "Requires business justification for SOX compliance and audit purposes.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Master query creation request with validation rules",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MasterQueryCreateRequest.class),
                examples = @ExampleObject(
                    name = "Account Summary Query Creation",
                    description = "Example request to create an account summary query",
                    value = """
                        {
                          "sourceSystem": "ENCORE",
                          "queryName": "customer_account_summary_v1",
                          "description": "Comprehensive customer account summary including balance and risk indicators",
                          "queryType": "SELECT",
                          "querySql": "SELECT account_id, customer_id, account_type, balance, status, risk_score FROM accounts a JOIN risk_assessment r ON a.customer_id = r.customer_id WHERE a.customer_id = :customerId AND a.status = 'ACTIVE'",
                          "dataClassification": "SENSITIVE",
                          "securityClassification": "INTERNAL",
                          "businessJustification": "Required for daily customer account reconciliation and regulatory reporting to meet SOX compliance requirements",
                          "complianceTags": ["SOX", "PCI_DSS", "BASEL_III"],
                          "expectedParameters": [
                            {
                              "name": "customerId",
                              "type": "STRING",
                              "required": true,
                              "description": "Unique customer identifier",
                              "validationPattern": "^[A-Z0-9]{6,12}$"
                            }
                          ],
                          "templateCategory": "Account Management",
                          "templateName": "Account Summary"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Master query created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MasterQueryConfigDTO.class),
                examples = @ExampleObject(
                    name = "Created Query Response",
                    value = """
                        {
                          "id": 15,
                          "sourceSystem": "ENCORE",
                          "queryName": "customer_account_summary_v1",
                          "queryType": "SELECT",
                          "querySql": "SELECT account_id, customer_id, account_type, balance, status, risk_score FROM accounts a JOIN risk_assessment r ON a.customer_id = r.customer_id WHERE a.customer_id = :customerId AND a.status = 'ACTIVE'",
                          "version": 1,
                          "isActive": "Y",
                          "createdBy": "john.creator",
                          "createdDate": "2025-08-22T14:30:45.123",
                          "displayName": "customer_account_summary_v1 (v1) - ENCORE",
                          "dataClassification": "SENSITIVE",
                          "statusIndicator": "ACTIVE",
                          "complexityLevel": "MEDIUM",
                          "parameterCount": 1,
                          "complianceRequirements": ["SOX", "FFIEC", "BASEL_III"]
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Validation error or invalid request data",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Validation Error Response",
                    value = """
                        {
                          "error": "VALIDATION_ERROR",
                          "message": "SQL validation failed: PROHIBITED_KEYWORD: Prohibited keyword detected: INSERT",
                          "correlationId": "corr_12345678-abcd-1234-5678-123456789012",
                          "timestamp": "2025-08-22T14:30:45.123Z",
                          "field": "querySql"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Insufficient permissions to create master queries",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Query name already exists in source system",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Duplicate Name Error",
                    value = """
                        {
                          "error": "DUPLICATE_NAME",
                          "message": "Query name 'customer_account_summary_v1' already exists in source system 'ENCORE'",
                          "correlationId": "corr_12345678-abcd-1234-5678-123456789012",
                          "timestamp": "2025-08-22T14:30:45.123Z"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<MasterQueryConfigDTO> createMasterQuery(
            @Valid @RequestBody MasterQueryCreateRequest request,
            @Parameter(description = "User role extracted from JWT token", hidden = true)
            @RequestHeader(value = "X-User-Role", defaultValue = "JOB_CREATOR") String userRole) {
        
        log.info("Received master query creation request - Name: {}, System: {}, User: {}", 
                request.getQueryName(), request.getSourceSystem(), userRole);
        
        try {
            MasterQueryConfigDTO created = masterQueryService.createMasterQuery(request, userRole);
            
            log.info("Master query created successfully - ID: {}, Name: {}", 
                    created.getId(), created.getQueryName());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
            
        } catch (IllegalArgumentException e) {
            log.warn("Master query creation validation failed - Name: {}, Error: {}", 
                    request.getQueryName(), e.getMessage());
            
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            log.error("Unexpected error during master query creation - Name: {}", 
                     request.getQueryName(), e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update an existing master query configuration.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('JOB_MODIFIER', 'JOB_CREATOR', 'ADMIN')")
    @Operation(
        summary = "Update Master Query Configuration",
        description = "Update an existing master query configuration with optimistic locking and version control. " +
                     "Supports partial updates and maintains complete audit trail for SOX compliance. " +
                     "Major changes (SQL, type) increment version number automatically.",
        parameters = @Parameter(
            name = "id",
            description = "Unique identifier of the master query to update",
            required = true,
            example = "15"
        ),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Master query update request with change justification",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MasterQueryUpdateRequest.class),
                examples = @ExampleObject(
                    name = "Query Enhancement Update",
                    description = "Example request to enhance an existing query with additional fields",
                    value = """
                        {
                          "id": 15,
                          "currentVersion": 1,
                          "querySql": "SELECT account_id, customer_id, account_type, balance, status, risk_score, credit_limit FROM accounts a JOIN risk_assessment r ON a.customer_id = r.customer_id LEFT JOIN credit_limits c ON a.account_id = c.account_id WHERE a.customer_id = :customerId AND a.status = 'ACTIVE'",
                          "description": "Enhanced customer account summary with credit limit information",
                          "dataClassification": "CONFIDENTIAL",
                          "changeJustification": "Enhanced query to include credit limit data for improved risk assessment and compliance with updated BASEL III requirements",
                          "changeSummary": "Added LEFT JOIN with credit_limits table and credit_limit column to output",
                          "complianceTags": ["SOX", "PCI_DSS", "BASEL_III", "GDPR"],
                          "createNewVersion": true,
                          "preserveOldVersion": true
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Master query updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MasterQueryConfigDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Validation error or invalid update data",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Master query not found",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Optimistic locking conflict - query was modified by another user",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Version Conflict Error",
                    value = """
                        {
                          "error": "VERSION_CONFLICT",
                          "message": "Optimistic locking failed - query was modified by another user. Expected version: 1, actual version: 2",
                          "correlationId": "corr_12345678-abcd-1234-5678-123456789012",
                          "timestamp": "2025-08-22T14:30:45.123Z",
                          "currentVersion": 2
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<MasterQueryConfigDTO> updateMasterQuery(
            @PathVariable Long id,
            @Valid @RequestBody MasterQueryUpdateRequest request,
            @Parameter(description = "User role extracted from JWT token", hidden = true)
            @RequestHeader(value = "X-User-Role", defaultValue = "JOB_MODIFIER") String userRole) {
        
        log.info("Received master query update request - ID: {}, Fields: {}, User: {}", 
                id, request.getUpdatedFields(), userRole);
        
        // Ensure ID consistency
        request.setId(id);
        
        try {
            MasterQueryConfigDTO updated = masterQueryService.updateMasterQuery(request, userRole);
            
            log.info("Master query updated successfully - ID: {}, New Version: {}", 
                    updated.getId(), updated.getVersion());
            
            return ResponseEntity.ok(updated);
            
        } catch (IllegalArgumentException e) {
            log.warn("Master query update validation failed - ID: {}, Error: {}", id, e.getMessage());
            
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("locking failed")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            log.error("Unexpected error during master query update - ID: {}", id, e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Soft delete a master query configuration.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete Master Query Configuration",
        description = "Soft delete a master query configuration with comprehensive audit trail. " +
                     "Only administrators can delete queries. Queries in use by active job configurations " +
                     "cannot be deleted. Requires business justification for SOX compliance.",
        parameters = {
            @Parameter(
                name = "id",
                description = "Unique identifier of the master query to delete",
                required = true,
                example = "15"
            ),
            @Parameter(
                name = "deleteJustification",
                description = "Business justification for deletion (minimum 20 characters)",
                required = true,
                example = "Query is obsolete due to system migration and no longer needed for regulatory reporting"
            )
        }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Master query deleted successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Deletion Success Response",
                    value = """
                        {
                          "deleted": true,
                          "id": 15,
                          "queryName": "customer_account_summary_v1",
                          "sourceSystem": "ENCORE",
                          "deletedBy": "admin.user",
                          "deletedAt": "2025-08-22T14:30:45.123",
                          "correlationId": "corr_12345678-abcd-1234-5678-123456789012",
                          "auditTrail": "audit_12345678-abcd-1234-5678"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid deletion request or missing justification",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Insufficient permissions - only administrators can delete",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Master query not found",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Cannot delete - query is referenced by active job configurations",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "In Use Error",
                    value = """
                        {
                          "error": "QUERY_IN_USE",
                          "message": "Cannot delete master query - it is currently referenced by active job configurations",
                          "correlationId": "corr_12345678-abcd-1234-5678-123456789012",
                          "timestamp": "2025-08-22T14:30:45.123Z",
                          "referencingJobs": 3
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> deleteMasterQuery(
            @PathVariable Long id,
            @RequestParam String deleteJustification,
            @Parameter(description = "User role extracted from JWT token", hidden = true)
            @RequestHeader(value = "X-User-Role", defaultValue = "ADMIN") String userRole) {
        
        log.info("Received master query deletion request - ID: {}, User: {}", id, userRole);
        
        try {
            Map<String, Object> result = masterQueryService.deleteMasterQuery(id, userRole, deleteJustification);
            
            log.info("Master query deleted successfully - ID: {}", id);
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("Master query deletion validation failed - ID: {}, Error: {}", id, e.getMessage());
            
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("currently referenced")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            log.error("Unexpected error during master query deletion - ID: {}", id, e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Validate SQL query without creating or updating.
     */
    @PostMapping("/validate-sql")
    @PreAuthorize("hasAnyRole('JOB_CREATOR', 'JOB_MODIFIER', 'ADMIN')")
    @Operation(
        summary = "Validate SQL Query",
        description = "Comprehensive SQL validation for master queries without creating or updating. " +
                     "Performs security scanning, complexity analysis, and banking compliance checks. " +
                     "Returns detailed validation results with suggestions for improvement.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "SQL validation request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "SQL Validation Request",
                    value = """
                        {
                          "querySql": "SELECT account_id, balance FROM accounts WHERE customer_id = :customerId AND balance > :minBalance",
                          "queryType": "SELECT",
                          "sourceSystem": "ENCORE"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "SQL validation completed",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Validation Success Response",
                    value = """
                        {
                          "valid": true,
                          "securityRisk": "LOW",
                          "complexityLevel": "LOW",
                          "parameterCount": 2,
                          "warnings": [
                            {
                              "type": "PERFORMANCE_HINT",
                              "message": "Consider adding index on customer_id for better performance"
                            }
                          ],
                          "checks": [
                            {
                              "type": "SQL_INJECTION",
                              "message": "SQL injection validation completed"
                            },
                            {
                              "type": "KEYWORD_VALIDATION", 
                              "message": "Keyword validation completed"
                            }
                          ],
                          "correlationId": "corr_12345678-abcd-1234-5678-123456789012",
                          "validatedAt": "2025-08-22T14:30:45.123Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "SQL validation failed",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Validation Failure Response",
                    value = """
                        {
                          "valid": false,
                          "securityRisk": "HIGH",
                          "errors": [
                            {
                              "type": "PROHIBITED_KEYWORD",
                              "message": "Prohibited keyword detected: DELETE"
                            },
                            {
                              "type": "SQL_INJECTION_DETECTED",
                              "message": "Potential SQL injection pattern detected"
                            }
                          ],
                          "correlationId": "corr_12345678-abcd-1234-5678-123456789012",
                          "validatedAt": "2025-08-22T14:30:45.123Z"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> validateSqlQuery(
            @RequestBody Map<String, String> request,
            @Parameter(description = "User role extracted from JWT token", hidden = true)
            @RequestHeader(value = "X-User-Role", defaultValue = "JOB_CREATOR") String userRole) {
        
        String sql = request.get("querySql");
        String correlationId = UUID.randomUUID().toString();
        
        log.info("Received SQL validation request - User: {}, Correlation: {}", userRole, correlationId);
        
        try {
            if (sql == null || sql.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "error", "SQL query is required",
                    "correlationId", correlationId
                ));
            }
            
            MasterQueryValidationService.ValidationResult result = 
                masterQueryService.getValidationService().validateSqlQuery(sql, userRole, correlationId);
            
            Map<String, Object> response = Map.of(
                "valid", result.isValid(),
                "securityRisk", result.getSecurityRisk(),
                "errors", result.getErrors(),
                "warnings", result.getWarnings(),
                "checks", result.getChecks(),
                "correlationId", correlationId,
                "validatedAt", result.getValidatedAt(),
                "validatedBy", userRole
            );
            
            log.info("SQL validation completed - Valid: {}, Risk: {}, Correlation: {}", 
                    result.isValid(), result.getSecurityRisk(), correlationId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during SQL validation - Correlation: {}", correlationId, e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "valid", false,
                "error", "Internal validation error",
                "correlationId", correlationId
            ));
        }
    }

    /**
     * Get available query templates.
     */
    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('JOB_VIEWER', 'JOB_CREATOR', 'JOB_MODIFIER', 'ADMIN')")
    @Operation(
        summary = "Get Query Templates",
        description = "Retrieve available query templates organized by category. " +
                     "Templates provide starting points for common banking queries and follow " +
                     "best practices for security and performance.",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Query templates retrieved successfully",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Templates Response",
                        value = """
                            {
                              "categories": [
                                {
                                  "name": "Account Management",
                                  "description": "Templates for account-related queries",
                                  "templates": [
                                    {
                                      "name": "Account Summary",
                                      "sql": "SELECT account_id, account_type, balance, status FROM accounts WHERE batch_date = :batchDate",
                                      "parameters": ["batchDate"],
                                      "description": "Basic account information summary"
                                    }
                                  ]
                                }
                              ],
                              "totalTemplates": 8,
                              "lastUpdated": "2025-08-22T14:30:45.123",
                              "version": "1.0"
                            }
                            """
                    )
                )
            )
        }
    )
    public ResponseEntity<Map<String, Object>> getQueryTemplates(
            @Parameter(description = "User role extracted from JWT token", hidden = true)
            @RequestHeader(value = "X-User-Role", defaultValue = "JOB_VIEWER") String userRole) {
        
        log.info("Received query templates request - User: {}", userRole);
        
        try {
            Map<String, Object> templates = masterQueryService.getQueryTemplates(userRole);
            
            log.info("Query templates retrieved successfully - Categories: {}", 
                    ((List<?>) templates.get("categories")).size());
            
            return ResponseEntity.ok(templates);
            
        } catch (Exception e) {
            log.error("Error retrieving query templates", e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}