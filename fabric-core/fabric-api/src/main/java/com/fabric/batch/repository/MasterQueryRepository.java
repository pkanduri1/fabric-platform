package com.fabric.batch.repository;

import com.fabric.batch.dto.MasterQueryResponse;
import com.fabric.batch.dto.MasterQueryConfigDTO;

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

    // =========================================================================
    // MASTER QUERY CRUD OPERATIONS
    // =========================================================================

    /**
     * Create a new master query configuration.
     * 
     * @param sourceSystem Source system identifier
     * @param queryName Descriptive name for the query
     * @param description Human-readable description
     * @param queryType Type of query (SELECT, WITH)
     * @param querySql SQL query content
     * @param dataClassification Data classification level
     * @param securityClassification Security classification level
     * @param createdBy User creating the query
     * @param correlationId Correlation ID for audit trail
     * @return Created master query configuration
     * @throws QuerySecurityException if user lacks permissions
     * @throws QueryExecutionException if creation fails
     */
    MasterQueryConfigDTO createMasterQuery(String sourceSystem, String queryName, String description,
                                         String queryType, String querySql, String dataClassification,
                                         String securityClassification, String createdBy, String correlationId);

    /**
     * Update an existing master query configuration.
     * 
     * @param id Master query ID to update
     * @param currentVersion Expected current version for optimistic locking
     * @param sourceSystem Updated source system (null if not changing)
     * @param queryName Updated query name (null if not changing)
     * @param description Updated description (null if not changing)
     * @param queryType Updated query type (null if not changing)
     * @param querySql Updated SQL (null if not changing)
     * @param isActive Updated active status (null if not changing)
     * @param dataClassification Updated data classification (null if not changing)
     * @param securityClassification Updated security classification (null if not changing)
     * @param modifiedBy User modifying the query
     * @param isMajorChange Whether this is a major change requiring version increment
     * @param correlationId Correlation ID for audit trail
     * @return Updated master query configuration
     * @throws QuerySecurityException if user lacks permissions
     * @throws QueryExecutionException if update fails
     */
    MasterQueryConfigDTO updateMasterQuery(Long id, Integer currentVersion, String sourceSystem,
                                         String queryName, String description, String queryType,
                                         String querySql, String isActive, String dataClassification,
                                         String securityClassification, String modifiedBy,
                                         boolean isMajorChange, String correlationId);

    /**
     * Soft delete a master query configuration.
     * 
     * @param id Master query ID to delete
     * @param deletedBy User deleting the query
     * @param deleteJustification Business justification for deletion
     * @param correlationId Correlation ID for audit trail
     * @return True if deletion successful
     * @throws QuerySecurityException if user lacks permissions
     * @throws QueryExecutionException if deletion fails
     */
    boolean softDeleteMasterQuery(Long id, String deletedBy, String deleteJustification, String correlationId);

    /**
     * Get a specific master query by ID.
     * 
     * @param id Master query ID
     * @param correlationId Correlation ID for audit trail
     * @return Master query configuration or null if not found
     * @throws QueryExecutionException if retrieval fails
     */
    MasterQueryConfigDTO getMasterQueryById(Long id, String correlationId);

    /**
     * Check if a query name already exists in the source system.
     * 
     * @param sourceSystem Source system to check
     * @param queryName Query name to check
     * @param correlationId Correlation ID for audit trail
     * @return True if name exists
     * @throws QueryExecutionException if check fails
     */
    boolean queryNameExists(String sourceSystem, String queryName, String correlationId);

    /**
     * Check if a master query is currently in use by active job configurations.
     * 
     * @param id Master query ID to check
     * @param correlationId Correlation ID for audit trail
     * @return True if query is in use
     * @throws QueryExecutionException if check fails
     */
    boolean isMasterQueryInUse(Long id, String correlationId);

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