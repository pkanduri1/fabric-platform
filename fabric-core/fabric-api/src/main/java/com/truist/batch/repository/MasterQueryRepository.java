package com.truist.batch.repository;

import com.truist.batch.dto.MasterQueryResponse;
import com.truist.batch.dto.MasterQueryConfigDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * =========================================================================
 * MASTER QUERY REPOSITORY INTERFACE - BANKING GRADE SECURITY
 * =========================================================================
 * 
 * Purpose: Repository interface for Master Query operations with banking-grade security
 * - Read-only query execution with timeout enforcement
 * - Parameter validation and SQL injection protection
 * - Result pagination and size limitations
 * - SOX-compliant audit trail integration
 * 
 * Security Features:
 * - Read-only connection pool isolation
 * - Query timeout enforcement (30 seconds max)
 * - Result size limitations (100 rows max)
 * - Parameter injection attack prevention
 * - Correlation ID tracking for audit compliance
 * 
 * Enterprise Standards:
 * - JdbcTemplate-based implementation for performance
 * - Banking compliance with timeout and size limits
 * - Comprehensive error handling with secure messages
 * - Performance metrics and execution statistics
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Phase 1 - Master Query Integration
 * =========================================================================
 */
public interface MasterQueryRepository {

    /**
     * Execute a master query with banking-grade security validation.
     * 
     * @param masterQueryId The unique master query identifier
     * @param querySql The SQL query to execute (SELECT/WITH only)
     * @param parameters Query parameters for parameterized execution
     * @param userRole User role for authorization
     * @param correlationId Correlation ID for audit trail
     * @return Query execution results with metadata
     * @throws QuerySecurityException if query fails security validation
     * @throws QueryExecutionException if query execution fails
     */
    MasterQueryResponse executeQuery(String masterQueryId, String querySql, 
                                   Map<String, Object> parameters, String userRole, 
                                   String correlationId);

    /**
     * Validate query syntax and security without execution.
     * 
     * @param querySql The SQL query to validate
     * @param parameters Query parameters
     * @param userRole User role for authorization
     * @param correlationId Correlation ID for audit trail
     * @return Validation result with security assessment
     */
    Map<String, Object> validateQuery(String querySql, Map<String, Object> parameters, 
                                     String userRole, String correlationId);

    /**
     * Get column metadata for a query without executing it.
     * 
     * @param querySql The SQL query to analyze
     * @param parameters Query parameters
     * @param correlationId Correlation ID for audit trail
     * @return Column metadata information
     */
    List<Map<String, Object>> getQueryColumnMetadata(String querySql, 
                                                    Map<String, Object> parameters, 
                                                    String correlationId);

    /**
     * Execute query with result count estimation.
     * 
     * @param querySql The SQL query to count
     * @param parameters Query parameters
     * @param correlationId Correlation ID for audit trail
     * @return Estimated row count
     */
    Long estimateQueryRowCount(String querySql, Map<String, Object> parameters, 
                              String correlationId);

    /**
     * Test database connectivity for read-only operations.
     * 
     * @param correlationId Correlation ID for audit trail
     * @return Connectivity test results
     */
    Map<String, Object> testConnectivity(String correlationId);

    /**
     * Get query execution statistics for monitoring.
     * 
     * @param startTime Start time for statistics period
     * @param endTime End time for statistics period
     * @param correlationId Correlation ID for audit trail
     * @return Execution statistics
     */
    Map<String, Object> getExecutionStatistics(java.time.Instant startTime, 
                                              java.time.Instant endTime, 
                                              String correlationId);

    /**
     * Get available database schemas and tables for query building.
     * 
     * @param userRole User role for authorization
     * @param correlationId Correlation ID for audit trail
     * @return Available schemas and tables
     */
    Map<String, Object> getAvailableSchemas(String userRole, String correlationId);

    /**
     * Get all available master query configurations from MASTER_QUERY_CONFIG table.
     * 
     * @param userRole User role for authorization
     * @param correlationId Correlation ID for audit trail
     * @return List of master query configurations
     * @throws QuerySecurityException if user lacks permissions
     * @throws QueryExecutionException if database query fails
     */
    List<MasterQueryConfigDTO> getAllMasterQueries(String userRole, String correlationId);

    /**
     * Custom exceptions for master query operations.
     */
    class QuerySecurityException extends RuntimeException {
        private final String correlationId;
        
        public QuerySecurityException(String message, String correlationId) {
            super(message);
            this.correlationId = correlationId;
        }
        
        public String getCorrelationId() {
            return correlationId;
        }
    }

    class QueryExecutionException extends RuntimeException {
        private final String correlationId;
        private final String errorCode;
        
        public QueryExecutionException(String message, String correlationId, String errorCode) {
            super(message);
            this.correlationId = correlationId;
            this.errorCode = errorCode;
        }
        
        public QueryExecutionException(String message, String correlationId, String errorCode, Throwable cause) {
            super(message, cause);
            this.correlationId = correlationId;
            this.errorCode = errorCode;
        }
        
        public String getCorrelationId() {
            return correlationId;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
    }
}