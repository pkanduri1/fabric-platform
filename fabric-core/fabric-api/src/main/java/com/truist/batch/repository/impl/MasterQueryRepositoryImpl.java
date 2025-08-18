package com.truist.batch.repository.impl;

import com.truist.batch.dto.MasterQueryResponse;
import com.truist.batch.repository.MasterQueryRepository;
import com.truist.batch.security.service.QuerySecurityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

/**
 * =========================================================================
 * MASTER QUERY REPOSITORY IMPLEMENTATION - BANKING GRADE SECURITY
 * =========================================================================
 * 
 * Purpose: JdbcTemplate-based implementation for Master Query operations
 * - Read-only query execution with banking-grade security
 * - Parameter validation and SQL injection protection
 * - SOX-compliant audit trail with correlation ID tracking
 * - Performance monitoring and timeout enforcement
 * 
 * Security Features:
 * - Isolated read-only connection pool
 * - Query validation before execution
 * - Parameter sanitization and injection protection
 * - 30-second timeout enforcement for all operations
 * - Result size limitation (100 rows maximum)
 * - Comprehensive audit logging
 * 
 * Enterprise Standards:
 * - Direct JDBC operations for optimal performance
 * - Banking compliance with regulatory requirements
 * - Error handling with secure error messages
 * - Correlation ID propagation for audit trails
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Phase 1 - Master Query Integration
 * =========================================================================
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class MasterQueryRepositoryImpl implements MasterQueryRepository {

    @Qualifier("readOnlyJdbcTemplate")
    private final JdbcTemplate readOnlyJdbcTemplate;
    
    private final QuerySecurityValidator querySecurityValidator;

    private static final int MAX_QUERY_TIMEOUT_SECONDS = 30;
    private static final int MAX_RESULT_ROWS = 100;
    private static final String AUDIT_LOGGER_NAME = "AUDIT.MasterQueryRepository";
    
    private final org.slf4j.Logger auditLogger = org.slf4j.LoggerFactory.getLogger(AUDIT_LOGGER_NAME);

    @Override
    public MasterQueryResponse executeQuery(String masterQueryId, String querySql, 
                                          Map<String, Object> parameters, String userRole, 
                                          String correlationId) {
        
        MDC.put("correlationId", correlationId);
        Instant startTime = Instant.now();
        
        try {
            log.info("Starting master query execution - ID: {}, User: {}, Correlation: {}", 
                    masterQueryId, userRole, correlationId);
            
            // 1. Security validation
            querySecurityValidator.validateQuery(querySql, parameters, userRole, correlationId);
            
            // 2. Prepare execution context
            NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(readOnlyJdbcTemplate);
            
            // 3. Execute query with timeout and result limits
            List<Map<String, Object>> results = new ArrayList<>();
            List<Map<String, Object>> columnMetadata = new ArrayList<>();
            
            try {
                // Set query timeout
                readOnlyJdbcTemplate.setQueryTimeout(MAX_QUERY_TIMEOUT_SECONDS);
                readOnlyJdbcTemplate.setMaxRows(MAX_RESULT_ROWS);
                
                // Execute query and capture metadata
                results = namedTemplate.query(querySql, parameters, (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    // Capture column metadata on first row
                    if (rowNum == 0) {
                        columnMetadata.addAll(extractColumnMetadata(metaData));
                    }
                    
                    // Extract row data
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    
                    return row;
                });
                
                Instant endTime = Instant.now();
                long executionTimeMs = java.time.Duration.between(startTime, endTime).toMillis();
                
                // 4. Build successful response
                MasterQueryResponse response = MasterQueryResponse.builder()
                    .masterQueryId(masterQueryId)
                    .executionStatus("SUCCESS")
                    .results(results)
                    .columnMetadata(columnMetadata)
                    .rowCount(results.size())
                    .executionTimeMs(executionTimeMs)
                    .executionStartTime(startTime)
                    .executionEndTime(endTime)
                    .executedBy(extractUsernameFromRole(userRole))
                    .userRole(userRole)
                    .correlationId(correlationId)
                    .queryParameters(parameters)
                    .securityClassification("INTERNAL")
                    .dataClassification("INTERNAL")
                    .executionContext("PRODUCTION")
                    .validationResults(buildValidationResults(true, null))
                    .performanceMetrics(buildPerformanceMetrics(executionTimeMs, results.size()))
                    .complianceInfo(buildComplianceInfo(correlationId))
                    .build();
                
                // Add warnings if results were truncated
                if (results.size() >= MAX_RESULT_ROWS) {
                    response.setWarnings(Arrays.asList(
                        String.format("Results limited to maximum %d rows - query may have additional data", MAX_RESULT_ROWS)
                    ));
                    response.setPaginationInfo(Map.of(
                        "totalRows", "UNKNOWN",
                        "returnedRows", results.size(),
                        "hasMoreResults", true,
                        "maxRowsLimit", MAX_RESULT_ROWS
                    ));
                }
                
                // Audit successful execution
                auditLogger.info("Query execution SUCCESS - ID: {}, Rows: {}, Time: {}ms, User: {}, Correlation: {}", 
                               masterQueryId, results.size(), executionTimeMs, userRole, correlationId);
                
                return response;
                
            } catch (QueryTimeoutException e) {
                throw new QueryExecutionException(
                    String.format("Query execution timeout after %d seconds", MAX_QUERY_TIMEOUT_SECONDS), 
                    correlationId, "TIMEOUT", e
                );
            } catch (DataAccessException e) {
                String errorCode = classifyDatabaseError(e);
                throw new QueryExecutionException(
                    "Query execution failed: " + getSafeErrorMessage(e), 
                    correlationId, errorCode, e
                );
            }
            
        } catch (QuerySecurityValidator.QuerySecurityException e) {
            auditLogger.error("Query security validation FAILED - ID: {}, User: {}, Correlation: {}, Error: {}", 
                            masterQueryId, userRole, correlationId, e.getMessage());
            throw new QuerySecurityException(e.getMessage(), correlationId);
            
        } catch (QueryExecutionException e) {
            Instant endTime = Instant.now();
            long executionTimeMs = java.time.Duration.between(startTime, endTime).toMillis();
            
            auditLogger.error("Query execution FAILED - ID: {}, User: {}, Correlation: {}, Error: {}, Time: {}ms", 
                            masterQueryId, userRole, correlationId, e.getErrorCode(), executionTimeMs);
            
            // Return error response instead of throwing
            return MasterQueryResponse.builder()
                .masterQueryId(masterQueryId)
                .executionStatus("FAILED")
                .executionStartTime(startTime)
                .executionEndTime(endTime)
                .executionTimeMs(executionTimeMs)
                .executedBy(extractUsernameFromRole(userRole))
                .userRole(userRole)
                .correlationId(correlationId)
                .queryParameters(parameters)
                .errorInfo(Map.of(
                    "errorCode", e.getErrorCode(),
                    "errorMessage", e.getMessage(),
                    "errorType", "EXECUTION_ERROR"
                ))
                .validationResults(buildValidationResults(false, e.getMessage()))
                .complianceInfo(buildComplianceInfo(correlationId))
                .build();
                
        } catch (Exception e) {
            Instant endTime = Instant.now();
            long executionTimeMs = java.time.Duration.between(startTime, endTime).toMillis();
            
            auditLogger.error("Query execution UNEXPECTED_ERROR - ID: {}, User: {}, Correlation: {}, Time: {}ms", 
                            masterQueryId, userRole, correlationId, executionTimeMs, e);
            
            throw new QueryExecutionException(
                "Unexpected error during query execution", 
                correlationId, "UNEXPECTED_ERROR", e
            );
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Override
    public Map<String, Object> validateQuery(String querySql, Map<String, Object> parameters, 
                                           String userRole, String correlationId) {
        
        MDC.put("correlationId", correlationId);
        
        try {
            log.debug("Validating query - User: {}, Correlation: {}", userRole, correlationId);
            
            // Use security validator for comprehensive validation
            QuerySecurityValidator.QueryValidationResult result = 
                querySecurityValidator.validateQuery(querySql, parameters, userRole, correlationId);
            
            Map<String, Object> validationResponse = new HashMap<>();
            validationResponse.put("valid", result.isValid());
            validationResponse.put("correlationId", result.getCorrelationId());
            validationResponse.put("message", result.getMessage());
            validationResponse.put("validatedAt", Instant.now());
            validationResponse.put("validatedBy", userRole);
            
            // Additional metadata validation
            try {
                NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(readOnlyJdbcTemplate);
                readOnlyJdbcTemplate.setQueryTimeout(5); // Short timeout for validation
                
                // Attempt to prepare statement to catch syntax errors
                namedTemplate.execute(querySql, parameters, (ps) -> {
                    validationResponse.put("syntaxValid", true);
                    validationResponse.put("parameterCount", ps.getParameterMetaData().getParameterCount());
                    return null;
                });
                
            } catch (Exception e) {
                validationResponse.put("syntaxValid", false);
                validationResponse.put("syntaxError", getSafeErrorMessage(e));
            }
            
            auditLogger.info("Query validation completed - Valid: {}, User: {}, Correlation: {}", 
                           result.isValid(), userRole, correlationId);
            
            return validationResponse;
            
        } catch (QuerySecurityValidator.QuerySecurityException e) {
            auditLogger.warn("Query validation FAILED - User: {}, Correlation: {}, Error: {}", 
                           userRole, correlationId, e.getMessage());
            
            return Map.of(
                "valid", false,
                "correlationId", correlationId,
                "message", e.getMessage(),
                "validatedAt", Instant.now(),
                "validatedBy", userRole,
                "securityViolation", true
            );
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Override
    public List<Map<String, Object>> getQueryColumnMetadata(String querySql, 
                                                           Map<String, Object> parameters, 
                                                           String correlationId) {
        
        MDC.put("correlationId", correlationId);
        
        try {
            log.debug("Getting column metadata for query - Correlation: {}", correlationId);
            
            NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(readOnlyJdbcTemplate);
            readOnlyJdbcTemplate.setQueryTimeout(10); // Short timeout for metadata
            readOnlyJdbcTemplate.setMaxRows(1); // Only need one row for metadata
            
            List<Map<String, Object>> columnMetadata = new ArrayList<>();
            
            namedTemplate.query(querySql, parameters, (rs) -> {
                columnMetadata.addAll(extractColumnMetadata(rs.getMetaData()));
            });
            
            return columnMetadata;
            
        } catch (Exception e) {
            log.error("Failed to get column metadata - Correlation: {}", correlationId, e);
            throw new QueryExecutionException(
                "Failed to retrieve column metadata: " + getSafeErrorMessage(e), 
                correlationId, "METADATA_ERROR", e
            );
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Override
    public Long estimateQueryRowCount(String querySql, Map<String, Object> parameters, 
                                    String correlationId) {
        
        MDC.put("correlationId", correlationId);
        
        try {
            log.debug("Estimating row count for query - Correlation: {}", correlationId);
            
            // Create COUNT wrapper query
            String countQuery = String.format("SELECT COUNT(*) as row_count FROM (%s) query_alias", querySql);
            
            NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(readOnlyJdbcTemplate);
            readOnlyJdbcTemplate.setQueryTimeout(15); // Moderate timeout for count
            
            Long count = namedTemplate.queryForObject(countQuery, parameters, Long.class);
            
            return count != null ? count : 0L;
            
        } catch (Exception e) {
            log.warn("Failed to estimate row count - Correlation: {}", correlationId, e);
            // Return null to indicate count estimation failed
            return null;
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Override
    public Map<String, Object> testConnectivity(String correlationId) {
        
        MDC.put("correlationId", correlationId);
        Instant startTime = Instant.now();
        
        try {
            log.debug("Testing read-only database connectivity - Correlation: {}", correlationId);
            
            readOnlyJdbcTemplate.setQueryTimeout(5);
            String result = readOnlyJdbcTemplate.queryForObject("SELECT 'HEALTHY' FROM DUAL", String.class);
            
            Instant endTime = Instant.now();
            long responseTimeMs = java.time.Duration.between(startTime, endTime).toMillis();
            
            boolean healthy = "HEALTHY".equals(result);
            
            Map<String, Object> connectivity = new HashMap<>();
            connectivity.put("healthy", healthy);
            connectivity.put("responseTimeMs", responseTimeMs);
            connectivity.put("testedAt", endTime);
            connectivity.put("correlationId", correlationId);
            connectivity.put("connectionType", "READ_ONLY");
            connectivity.put("maxTimeout", MAX_QUERY_TIMEOUT_SECONDS);
            connectivity.put("maxRows", MAX_RESULT_ROWS);
            
            return connectivity;
            
        } catch (Exception e) {
            Instant endTime = Instant.now();
            long responseTimeMs = java.time.Duration.between(startTime, endTime).toMillis();
            
            log.error("Database connectivity test failed - Correlation: {}", correlationId, e);
            
            return Map.of(
                "healthy", false,
                "responseTimeMs", responseTimeMs,
                "testedAt", endTime,
                "correlationId", correlationId,
                "error", getSafeErrorMessage(e),
                "connectionType", "READ_ONLY"
            );
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Override
    public Map<String, Object> getExecutionStatistics(Instant startTime, Instant endTime, 
                                                     String correlationId) {
        
        // This would typically query execution audit tables
        // For now, return basic statistics structure
        
        return Map.of(
            "statisticsPeriod", Map.of(
                "startTime", startTime,
                "endTime", endTime
            ),
            "totalExecutions", 0,
            "successfulExecutions", 0,
            "failedExecutions", 0,
            "averageExecutionTimeMs", 0.0,
            "maxExecutionTimeMs", 0,
            "totalRowsReturned", 0,
            "correlationId", correlationId,
            "note", "Execution statistics require audit table implementation"
        );
    }

    @Override
    public Map<String, Object> getAvailableSchemas(String userRole, String correlationId) {
        
        MDC.put("correlationId", correlationId);
        
        try {
            log.debug("Getting available schemas for user role: {} - Correlation: {}", userRole, correlationId);
            
            // This would typically query database metadata based on user permissions
            // For now, return a basic schema structure
            
            return Map.of(
                "schemas", Arrays.asList(
                    Map.of(
                        "schemaName", "CM3INT",
                        "description", "Core banking integration schema",
                        "accessLevel", "READ_ONLY",
                        "tables", Arrays.asList(
                            "MANUAL_JOB_CONFIG",
                            "MANUAL_JOB_EXECUTION", 
                            "TEMPLATE_MASTER_QUERY_MAPPING",
                            "MASTER_QUERY_COLUMNS"
                        )
                    )
                ),
                "userRole", userRole,
                "correlationId", correlationId,
                "retrievedAt", Instant.now(),
                "note", "Schema metadata requires database catalog implementation"
            );
            
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * Extract column metadata from ResultSetMetaData.
     */
    private List<Map<String, Object>> extractColumnMetadata(ResultSetMetaData metaData) throws SQLException {
        List<Map<String, Object>> columns = new ArrayList<>();
        int columnCount = metaData.getColumnCount();
        
        for (int i = 1; i <= columnCount; i++) {
            Map<String, Object> column = new HashMap<>();
            column.put("name", metaData.getColumnLabel(i));
            column.put("type", metaData.getColumnTypeName(i));
            column.put("sqlType", metaData.getColumnType(i));
            column.put("length", metaData.getColumnDisplaySize(i));
            column.put("precision", metaData.getPrecision(i));
            column.put("scale", metaData.getScale(i));
            column.put("nullable", metaData.isNullable(i) == ResultSetMetaData.columnNullable);
            column.put("order", i);
            columns.add(column);
        }
        
        return columns;
    }

    /**
     * Build validation results map.
     */
    private Map<String, Object> buildValidationResults(boolean passed, String errorMessage) {
        Map<String, Object> validation = new HashMap<>();
        validation.put("validationPassed", passed);
        validation.put("securityChecks", passed ? "PASSED" : "FAILED");
        validation.put("parameterValidation", passed ? "PASSED" : "FAILED");
        validation.put("validatedAt", Instant.now());
        
        if (!passed && errorMessage != null) {
            validation.put("validationError", errorMessage);
        }
        
        return validation;
    }

    /**
     * Build performance metrics map.
     */
    private Map<String, Object> buildPerformanceMetrics(long executionTimeMs, int resultCount) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("executionTimeMs", executionTimeMs);
        metrics.put("resultCount", resultCount);
        metrics.put("maxTimeoutSeconds", MAX_QUERY_TIMEOUT_SECONDS);
        metrics.put("maxResultRows", MAX_RESULT_ROWS);
        metrics.put("connectionType", "READ_ONLY");
        return metrics;
    }

    /**
     * Build compliance information map.
     */
    private Map<String, Object> buildComplianceInfo(String correlationId) {
        return Map.of(
            "auditTrailId", "audit_" + correlationId.substring(5), // Remove "corr_" prefix
            "complianceTags", Arrays.asList("SOX", "PCI_DSS"),
            "dataRetentionDays", 2555, // 7 years for banking compliance
            "auditLoggerName", AUDIT_LOGGER_NAME
        );
    }

    /**
     * Extract username from role information.
     */
    private String extractUsernameFromRole(String userRole) {
        // This would typically extract from security context
        // For now, return a placeholder
        return "system.user";
    }

    /**
     * Classify database errors for secure error reporting.
     */
    private String classifyDatabaseError(DataAccessException e) {
        String message = e.getMessage().toLowerCase();
        
        if (message.contains("timeout")) {
            return "TIMEOUT";
        } else if (message.contains("syntax")) {
            return "SYNTAX_ERROR";
        } else if (message.contains("permission") || message.contains("access")) {
            return "ACCESS_DENIED";
        } else if (message.contains("connection")) {
            return "CONNECTION_ERROR";
        } else {
            return "DATABASE_ERROR";
        }
    }

    /**
     * Get safe error message without exposing sensitive information.
     */
    private String getSafeErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return "Unknown database error occurred";
        }
        
        // Remove potentially sensitive information
        return message.replaceAll("password=\\w+", "password=***")
                     .replaceAll("user=\\w+", "user=***")
                     .substring(0, Math.min(message.length(), 200));
    }
}