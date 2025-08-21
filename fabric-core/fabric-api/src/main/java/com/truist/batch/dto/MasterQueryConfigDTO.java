package com.truist.batch.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * =========================================================================
 * MASTER QUERY CONFIG DTO - BANKING GRADE SECURITY
 * =========================================================================
 * 
 * Purpose: Data Transfer Object for Master Query Configuration data
 * - Maps to MASTER_QUERY_CONFIG Oracle table
 * - Provides secure data transport for UI master query library
 * - Implements banking-grade data classification and security
 * 
 * Database Mapping:
 * - Table: CM3INT.MASTER_QUERY_CONFIG
 * - Primary Key: ID
 * - Source System Integration: ENCORE, ATLAS, etc.
 * - Query Types: SELECT, WITH (read-only operations only)
 * 
 * Security Features:
 * - Data classification for sensitive query content
 * - Version control for change tracking
 * - Active status for query lifecycle management
 * - Creation audit trail with user attribution
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Phase 3 - Master Query List Integration
 * =========================================================================
 */
@Data
@Builder
public class MasterQueryConfigDTO {

    /**
     * Unique identifier for the master query configuration.
     * Maps to: MASTER_QUERY_CONFIG.ID
     */
    @JsonProperty("id")
    private Long id;

    /**
     * Source system identifier for data integration.
     * Maps to: MASTER_QUERY_CONFIG.SOURCE_SYSTEM
     * Examples: ENCORE, ATLAS, CORE_BANKING
     */
    @JsonProperty("sourceSystem")
    private String sourceSystem;

    /**
     * Descriptive name for the master query.
     * Maps to: MASTER_QUERY_CONFIG.QUERY_NAME
     * Example: atoctran_encore_200_job
     */
    @JsonProperty("queryName")
    private String queryName;

    /**
     * Type of query operation (SELECT, WITH).
     * Maps to: MASTER_QUERY_CONFIG.QUERY_TYPE
     * Banking regulation: Only read-only operations allowed
     */
    @JsonProperty("queryType")
    private String queryType;

    /**
     * SQL query content with parameter placeholders.
     * Maps to: MASTER_QUERY_CONFIG.QUERY_SQL
     * Security: Validated for SQL injection protection
     */
    @JsonProperty("querySql")
    private String querySql;

    /**
     * Version number for change tracking.
     * Maps to: MASTER_QUERY_CONFIG.VERSION
     * SOX Compliance: Required for audit trail
     */
    @JsonProperty("version")
    private Integer version;

    /**
     * Active status indicator (Y/N).
     * Maps to: MASTER_QUERY_CONFIG.IS_ACTIVE
     * Lifecycle: Controls query availability in UI
     */
    @JsonProperty("isActive")
    private String isActive;

    /**
     * User who created the query configuration.
     * Maps to: MASTER_QUERY_CONFIG.CREATED_BY
     * Audit: Required for SOX compliance
     */
    @JsonProperty("createdBy")
    private String createdBy;

    /**
     * Timestamp when configuration was created.
     * Maps to: MASTER_QUERY_CONFIG.CREATED_DATE
     * Format: ISO 8601 for consistent date handling
     */
    @JsonProperty("createdDate")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdDate;

    // =========================================================================
    // COMPUTED PROPERTIES FOR UI INTEGRATION
    // =========================================================================

    /**
     * Computed property: Is query currently active and available.
     */
    public boolean isCurrentlyActive() {
        return "Y".equalsIgnoreCase(this.isActive);
    }

    /**
     * Computed property: Display name for UI dropdown.
     */
    public String getDisplayName() {
        return String.format("%s (v%d) - %s", 
                            this.queryName, 
                            this.version, 
                            this.sourceSystem);
    }

    /**
     * Computed property: Short description for UI tooltips.
     */
    public String getShortDescription() {
        if (this.querySql != null && this.querySql.length() > 100) {
            return this.querySql.substring(0, 97) + "...";
        }
        return this.querySql;
    }

    /**
     * Computed property: Data classification for security labeling.
     */
    public String getDataClassification() {
        // Classify based on source system and query content
        if ("ENCORE".equalsIgnoreCase(this.sourceSystem)) {
            return "SENSITIVE"; // Transaction data
        } else if (this.querySql != null && 
                  (this.querySql.toLowerCase().contains("account") || 
                   this.querySql.toLowerCase().contains("customer"))) {
            return "CONFIDENTIAL"; // Customer/Account data
        }
        return "INTERNAL"; // Default classification
    }

    /**
     * Computed property: Security requirements based on data content.
     */
    public String[] getComplianceRequirements() {
        String dataClass = getDataClassification();
        switch (dataClass) {
            case "CONFIDENTIAL":
                return new String[]{"SOX", "PCI_DSS", "GLBA", "GDPR"};
            case "SENSITIVE":
                return new String[]{"SOX", "FFIEC", "BASEL_III"};
            default:
                return new String[]{"SOX"};
        }
    }

    /**
     * Computed property: UI status indicator with color coding.
     */
    public String getStatusIndicator() {
        if (isCurrentlyActive()) {
            return "ACTIVE"; // Green in UI
        } else {
            return "INACTIVE"; // Gray in UI
        }
    }

    /**
     * Computed property: Parameter count from SQL analysis.
     */
    public int getParameterCount() {
        if (this.querySql == null) {
            return 0;
        }
        // Count :parameterName patterns
        return (int) this.querySql.chars()
                                 .filter(ch -> ch == ':')
                                 .count();
    }

    /**
     * Computed property: Query complexity assessment.
     */
    public String getComplexityLevel() {
        if (this.querySql == null) {
            return "UNKNOWN";
        }
        
        String sql = this.querySql.toLowerCase();
        int complexity = 0;
        
        // Count complexity indicators
        if (sql.contains("join")) complexity += 2;
        if (sql.contains("subquery") || sql.contains("exists")) complexity += 3;
        if (sql.contains("union")) complexity += 2;
        if (sql.contains("group by")) complexity += 1;
        if (sql.contains("having")) complexity += 2;
        if (sql.contains("window")) complexity += 3;
        
        if (complexity >= 8) return "HIGH";
        if (complexity >= 4) return "MEDIUM";
        return "LOW";
    }

    // =========================================================================
    // VALIDATION METHODS
    // =========================================================================

    /**
     * Validate DTO data integrity.
     */
    public boolean isValid() {
        return this.id != null &&
               this.sourceSystem != null && !this.sourceSystem.trim().isEmpty() &&
               this.queryName != null && !this.queryName.trim().isEmpty() &&
               this.queryType != null && !this.queryType.trim().isEmpty() &&
               this.querySql != null && !this.querySql.trim().isEmpty() &&
               this.version != null && this.version > 0 &&
               this.isActive != null &&
               this.createdBy != null && !this.createdBy.trim().isEmpty() &&
               this.createdDate != null;
    }

    /**
     * Check if query type is allowed (read-only operations only).
     */
    public boolean hasValidQueryType() {
        return "SELECT".equalsIgnoreCase(this.queryType) || 
               "WITH".equalsIgnoreCase(this.queryType);
    }

    /**
     * Check if source system is recognized.
     */
    public boolean hasValidSourceSystem() {
        String[] validSystems = {"ENCORE", "ATLAS", "CORE_BANKING", "RISK_ENGINE"};
        for (String system : validSystems) {
            if (system.equalsIgnoreCase(this.sourceSystem)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("MasterQueryConfig{id=%d, name='%s', system='%s', version=%d, active=%s}", 
                           this.id, this.queryName, this.sourceSystem, this.version, this.isActive);
    }
}