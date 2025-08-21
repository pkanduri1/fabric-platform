package com.truist.batch.entity;

import java.time.LocalDateTime;

/**
 * =========================================================================
 * TEMPLATE MASTER QUERY MAPPING ENTITY - BANKING GRADE SECURITY
 * =========================================================================
 * 
 * Purpose: Entity for TEMPLATE_MASTER_QUERY_MAPPING table
 * - Links job configuration templates to master queries
 * - Provides secure parameterized query execution
 * - Implements banking-grade security and SOX compliance
 * 
 * Database Mapping:
 * - Table: CM3INT.TEMPLATE_MASTER_QUERY_MAPPING
 * - Primary Key: MAPPING_ID
 * - Foreign Keys: CONFIG_ID, MASTER_QUERY_ID
 * 
 * Security Features:
 * - Read-only query enforcement
 * - 30-second execution timeout
 * - Maximum 100 rows result limit
 * - Security classification tracking
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Phase 3 - Master Query Integration
 * =========================================================================
 */
public class TemplateMasterQueryMappingEntity {

    // =========================================================================
    // PRIMARY KEY AND IDENTIFIERS
    // =========================================================================
    
    /**
     * Unique identifier for the template-query mapping.
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.MAPPING_ID
     */
    private String mappingId;
    
    /**
     * Foreign key to job configuration.
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.CONFIG_ID
     * References: MANUAL_JOB_CONFIG.CONFIG_ID
     */
    private String configId;
    
    /**
     * Master query identifier.
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.MASTER_QUERY_ID
     */
    private String masterQueryId;

    // =========================================================================
    // QUERY DEFINITION
    // =========================================================================
    
    /**
     * Descriptive name for the query.
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.QUERY_NAME
     */
    private String queryName;
    
    /**
     * SQL query content with parameter placeholders.
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.QUERY_SQL
     * Security: Validated for read-only operations only
     */
    private String querySql;
    
    /**
     * Description of the query purpose.
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.QUERY_DESCRIPTION
     */
    private String queryDescription;

    // =========================================================================
    // QUERY CONFIGURATION
    // =========================================================================
    
    /**
     * Type of query operation (SELECT, WITH_SELECT).
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.QUERY_TYPE
     * Banking regulation: Only read-only operations allowed
     */
    private String queryType = "SELECT";
    
    /**
     * Maximum execution time in seconds (1-30).
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.MAX_EXECUTION_TIME_SECONDS
     */
    private Integer maxExecutionTimeSeconds = 30;
    
    /**
     * Maximum number of result rows (1-100).
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.MAX_RESULT_ROWS
     */
    private Integer maxResultRows = 100;

    // =========================================================================
    // PARAMETER CONFIGURATION
    // =========================================================================
    
    /**
     * JSON-formatted query parameters.
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.QUERY_PARAMETERS
     */
    private String queryParameters;
    
    /**
     * JSON-formatted parameter validation rules.
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.PARAMETER_VALIDATION_RULES
     */
    private String parameterValidationRules;

    // =========================================================================
    // STATUS AND CONTROL
    // =========================================================================
    
    /**
     * Status of the mapping (ACTIVE, INACTIVE, DEPRECATED).
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.STATUS
     */
    private String status = "ACTIVE";
    
    /**
     * Read-only enforcement flag.
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.IS_READ_ONLY
     */
    private String isReadOnly = "Y";

    // =========================================================================
    // SECURITY AND COMPLIANCE
    // =========================================================================
    
    /**
     * Security classification level.
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.SECURITY_CLASSIFICATION
     */
    private String securityClassification = "INTERNAL";
    
    /**
     * Approval requirement flag.
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.REQUIRES_APPROVAL
     */
    private String requiresApproval = "Y";

    // =========================================================================
    // AUDIT TRAIL
    // =========================================================================
    
    /**
     * User who created the mapping.
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.CREATED_BY
     */
    private String createdBy;
    
    /**
     * Timestamp when mapping was created.
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.CREATED_DATE
     */
    private LocalDateTime createdDate;
    
    /**
     * User who last updated the mapping.
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.UPDATED_BY
     */
    private String updatedBy;
    
    /**
     * Timestamp when mapping was last updated.
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.UPDATED_DATE
     */
    private LocalDateTime updatedDate;
    
    /**
     * Correlation ID for audit trail tracking.
     * Maps to: TEMPLATE_MASTER_QUERY_MAPPING.CORRELATION_ID
     */
    private String correlationId;

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================
    
    public TemplateMasterQueryMappingEntity() {
        this.createdDate = LocalDateTime.now();
    }
    
    public TemplateMasterQueryMappingEntity(String configId, String masterQueryId, 
                                          String queryName, String querySql, 
                                          String createdBy) {
        this();
        this.configId = configId;
        this.masterQueryId = masterQueryId;
        this.queryName = queryName;
        this.querySql = querySql;
        this.createdBy = createdBy;
        this.mappingId = generateMappingId(configId, masterQueryId);
    }

    // =========================================================================
    // BUSINESS METHODS
    // =========================================================================
    
    /**
     * Generate unique mapping ID.
     */
    private String generateMappingId(String configId, String masterQueryId) {
        return String.format("tmq_%s_%s_%d", 
                           configId != null ? configId.replace("_", "") : "unknown",
                           masterQueryId != null ? masterQueryId : "unknown",
                           System.currentTimeMillis() % 100000);
    }
    
    /**
     * Check if mapping is currently active.
     */
    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }
    
    /**
     * Check if query is read-only.
     */
    public boolean isReadOnlyQuery() {
        return "Y".equals(this.isReadOnly);
    }
    
    /**
     * Check if approval is required.
     */
    public boolean requiresApprovalForExecution() {
        return "Y".equals(this.requiresApproval);
    }
    
    /**
     * Update the mapping (sets updated timestamp).
     */
    public void preUpdate() {
        this.updatedDate = LocalDateTime.now();
    }

    // =========================================================================
    // GETTERS AND SETTERS
    // =========================================================================
    
    public String getMappingId() {
        return mappingId;
    }

    public void setMappingId(String mappingId) {
        this.mappingId = mappingId;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getMasterQueryId() {
        return masterQueryId;
    }

    public void setMasterQueryId(String masterQueryId) {
        this.masterQueryId = masterQueryId;
    }

    public String getQueryName() {
        return queryName;
    }

    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    public String getQuerySql() {
        return querySql;
    }

    public void setQuerySql(String querySql) {
        this.querySql = querySql;
    }

    public String getQueryDescription() {
        return queryDescription;
    }

    public void setQueryDescription(String queryDescription) {
        this.queryDescription = queryDescription;
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public Integer getMaxExecutionTimeSeconds() {
        return maxExecutionTimeSeconds;
    }

    public void setMaxExecutionTimeSeconds(Integer maxExecutionTimeSeconds) {
        this.maxExecutionTimeSeconds = maxExecutionTimeSeconds;
    }

    public Integer getMaxResultRows() {
        return maxResultRows;
    }

    public void setMaxResultRows(Integer maxResultRows) {
        this.maxResultRows = maxResultRows;
    }

    public String getQueryParameters() {
        return queryParameters;
    }

    public void setQueryParameters(String queryParameters) {
        this.queryParameters = queryParameters;
    }

    public String getParameterValidationRules() {
        return parameterValidationRules;
    }

    public void setParameterValidationRules(String parameterValidationRules) {
        this.parameterValidationRules = parameterValidationRules;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIsReadOnly() {
        return isReadOnly;
    }

    public void setIsReadOnly(String isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    public String getSecurityClassification() {
        return securityClassification;
    }

    public void setSecurityClassification(String securityClassification) {
        this.securityClassification = securityClassification;
    }

    public String getRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(String requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================
    
    @Override
    public String toString() {
        return String.format("TemplateMasterQueryMapping{mappingId='%s', configId='%s', queryName='%s', status='%s'}", 
                           mappingId, configId, queryName, status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemplateMasterQueryMappingEntity)) return false;
        TemplateMasterQueryMappingEntity that = (TemplateMasterQueryMappingEntity) o;
        return mappingId != null && mappingId.equals(that.mappingId);
    }

    @Override
    public int hashCode() {
        return mappingId != null ? mappingId.hashCode() : 0;
    }
}