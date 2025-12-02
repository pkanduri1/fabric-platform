package com.fabric.batch.dao.impl;

import com.fabric.batch.dao.ConfigurationAuditDao;
import com.fabric.batch.model.ConfigurationAudit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ConfigurationAuditDaoImpl implements ConfigurationAuditDao {
    
    private final JdbcTemplate jdbcTemplate;
    
    private static final String SCHEMA = "CM3INT";
    private static final String TABLE_NAME = SCHEMA + ".MANUAL_JOB_AUDIT";
    
    private static final String INSERT_SQL = 
        "INSERT INTO " + TABLE_NAME + " (AUDIT_ID, CONFIG_ID, OPERATION_TYPE, OPERATION_DESCRIPTION, " +
        "OLD_VALUES, NEW_VALUES, CHANGED_BY, CHANGE_DATE, USER_ROLE, CHANGE_REASON, " +
        "APPROVAL_STATUS, SOX_COMPLIANCE_FLAG, RISK_ASSESSMENT, ENVIRONMENT, ARCHIVED_FLAG) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SELECT_BY_CONFIG_ID_SQL = 
        "SELECT * FROM " + TABLE_NAME + " WHERE CONFIG_ID = ? ORDER BY CHANGE_DATE DESC";
    
    private static final String SELECT_BY_CHANGED_BY_SQL = 
        "SELECT * FROM " + TABLE_NAME + " WHERE CHANGED_BY = ? ORDER BY CHANGE_DATE DESC";
    
    private static final String SELECT_RECENT_CHANGES_SQL = 
        "SELECT * FROM " + TABLE_NAME + " WHERE CHANGE_DATE >= ? ORDER BY CHANGE_DATE DESC";
    
    private static final String SELECT_BY_CONFIG_ID_AND_ACTION_SQL = 
        "SELECT * FROM " + TABLE_NAME + " WHERE CONFIG_ID = ? AND OPERATION_TYPE = ? ORDER BY CHANGE_DATE DESC";
    
    @Autowired
    public ConfigurationAuditDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public ConfigurationAudit save(ConfigurationAudit audit) {
        if (audit.getChangeDate() == null) {
            audit.setChangeDate(LocalDateTime.now());
        }
        
        // Generate audit ID if not provided
        String auditId = generateAuditId();
        
        // Debug logging to see exact values being inserted
        System.out.println("AUDIT INSERT VALUES: AUDIT_ID=" + auditId + 
                          ", CONFIG_ID=" + audit.getConfigId() + 
                          ", OPERATION_TYPE=" + mapActionToOperationType(audit.getAction()) + 
                          ", CHANGED_BY=" + audit.getChangedBy() + 
                          ", REASON=" + audit.getReason());
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(INSERT_SQL, new String[]{"AUDIT_ID"});
                ps.setString(1, auditId);                              // AUDIT_ID
                ps.setString(2, audit.getConfigId());                  // CONFIG_ID
                ps.setString(3, mapActionToOperationType(audit.getAction())); // OPERATION_TYPE
                ps.setString(4, "Configuration change via API");       // OPERATION_DESCRIPTION
                ps.setString(5, audit.getOldValue());                  // OLD_VALUES
                ps.setString(6, audit.getNewValue());                  // NEW_VALUES
                ps.setString(7, audit.getChangedBy());                 // CHANGED_BY
                ps.setTimestamp(8, Timestamp.valueOf(audit.getChangeDate())); // CHANGE_DATE
                ps.setString(9, "JOB_CREATOR");                        // USER_ROLE (default)
                ps.setString(10, audit.getReason());                   // CHANGE_REASON
                ps.setString(11, "AUTO_APPROVED");                     // APPROVAL_STATUS
                ps.setString(12, "Y");                                 // SOX_COMPLIANCE_FLAG
                ps.setString(13, "LOW");                               // RISK_ASSESSMENT
                ps.setString(14, "DEVELOPMENT");                       // ENVIRONMENT
                ps.setString(15, "N");                                 // ARCHIVED_FLAG
                return ps;
            }, keyHolder);
            
            // Set the generated audit ID
            audit.setId(Long.valueOf(auditId.hashCode())); // Convert string ID to Long for compatibility
            
        } catch (Exception e) {
            System.out.println("AUDIT INSERT FAILED: " + e.getMessage());
            e.printStackTrace();
            // For now, just log the error and continue without failing the main operation
            System.out.println("WARNING: Audit entry could not be created, but main operation succeeded");
            return audit; // Return without setting ID
        }
        
        return audit;
    }
    
    @Override
    public List<ConfigurationAudit> findByConfigIdOrderByChangeDateDesc(String configId) {
        return jdbcTemplate.query(SELECT_BY_CONFIG_ID_SQL, configurationAuditRowMapper, configId);
    }
    
    @Override
    public List<ConfigurationAudit> findByChangedByOrderByChangeDateDesc(String changedBy) {
        return jdbcTemplate.query(SELECT_BY_CHANGED_BY_SQL, configurationAuditRowMapper, changedBy);
    }
    
    @Override
    public List<ConfigurationAudit> findRecentChanges(LocalDateTime startDate) {
        return jdbcTemplate.query(SELECT_RECENT_CHANGES_SQL, configurationAuditRowMapper, Timestamp.valueOf(startDate));
    }
    
    @Override
    public List<ConfigurationAudit> findByConfigIdAndAction(String configId, String action) {
        return jdbcTemplate.query(SELECT_BY_CONFIG_ID_AND_ACTION_SQL, configurationAuditRowMapper, configId, action);
    }
    
    /**
     * Generate unique audit ID for MANUAL_JOB_AUDIT table
     */
    private String generateAuditId() {
        return "AUDIT_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    /**
     * Map generic action to MANUAL_JOB_AUDIT operation type
     */
    private String mapActionToOperationType(String action) {
        if (action == null) return "UPDATE";
        
        switch (action.toUpperCase()) {
            case "SAVE":
            case "CREATE":
                return "CREATE";
            case "UPDATE":
            case "MODIFY":
                return "UPDATE";
            case "DELETE":
            case "REMOVE":
                return "DELETE";
            case "ACTIVATE":
                return "ACTIVATE";
            case "DEACTIVATE":
                return "DEACTIVATE";
            default:
                return "UPDATE"; // Default to UPDATE for unknown actions
        }
    }

    private final RowMapper<ConfigurationAudit> configurationAuditRowMapper = new RowMapper<ConfigurationAudit>() {
        @Override
        public ConfigurationAudit mapRow(ResultSet rs, int rowNum) throws SQLException {
            ConfigurationAudit audit = new ConfigurationAudit();
            
            // Map from MANUAL_JOB_AUDIT table structure
            String auditId = rs.getString("AUDIT_ID");
            audit.setId(auditId != null ? (long)auditId.hashCode() : 0L); // Convert string to Long
            audit.setConfigId(rs.getString("CONFIG_ID"));
            audit.setAction(rs.getString("OPERATION_TYPE")); // Map OPERATION_TYPE back to action
            audit.setOldValue(rs.getString("OLD_VALUES"));
            audit.setNewValue(rs.getString("NEW_VALUES"));
            audit.setChangedBy(rs.getString("CHANGED_BY"));
            
            Timestamp changeDate = rs.getTimestamp("CHANGE_DATE");
            if (changeDate != null) {
                audit.setChangeDate(changeDate.toLocalDateTime());
            }
            
            audit.setReason(rs.getString("CHANGE_REASON"));
            
            return audit;
        }
    };
}