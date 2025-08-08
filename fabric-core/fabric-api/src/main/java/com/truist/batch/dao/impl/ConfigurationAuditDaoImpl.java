package com.truist.batch.dao.impl;

import com.truist.batch.dao.ConfigurationAuditDao;
import com.truist.batch.model.ConfigurationAudit;
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
    private static final String TABLE_NAME = SCHEMA + ".CONFIGURATION_AUDIT";
    
    private static final String INSERT_SQL = 
        "INSERT INTO " + TABLE_NAME + " (CONFIG_ID, ACTION, OLD_VALUE, NEW_VALUE, CHANGED_BY, CHANGE_DATE, CHANGE_REASON) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SELECT_BY_CONFIG_ID_SQL = 
        "SELECT * FROM " + TABLE_NAME + " WHERE CONFIG_ID = ? ORDER BY CHANGE_DATE DESC";
    
    private static final String SELECT_BY_CHANGED_BY_SQL = 
        "SELECT * FROM " + TABLE_NAME + " WHERE CHANGED_BY = ? ORDER BY CHANGE_DATE DESC";
    
    private static final String SELECT_RECENT_CHANGES_SQL = 
        "SELECT * FROM " + TABLE_NAME + " WHERE CHANGE_DATE >= ? ORDER BY CHANGE_DATE DESC";
    
    private static final String SELECT_BY_CONFIG_ID_AND_ACTION_SQL = 
        "SELECT * FROM " + TABLE_NAME + " WHERE CONFIG_ID = ? AND ACTION = ? ORDER BY CHANGE_DATE DESC";
    
    @Autowired
    public ConfigurationAuditDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public ConfigurationAudit save(ConfigurationAudit audit) {
        if (audit.getChangeDate() == null) {
            audit.setChangeDate(LocalDateTime.now());
        }
        
        // Debug logging to see exact values being inserted
        System.out.println("AUDIT INSERT VALUES: CONFIG_ID=" + audit.getConfigId() + 
                          ", ACTION=" + audit.getAction() + 
                          ", CHANGED_BY=" + audit.getChangedBy() + 
                          ", REASON=" + audit.getReason());
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, audit.getConfigId());
                ps.setString(2, audit.getAction());
                ps.setString(3, audit.getOldValue());
                ps.setString(4, audit.getNewValue());
                ps.setString(5, audit.getChangedBy());
                ps.setTimestamp(6, Timestamp.valueOf(audit.getChangeDate()));
                ps.setString(7, audit.getReason());
                return ps;
            }, keyHolder);
        } catch (Exception e) {
            System.out.println("AUDIT INSERT FAILED: " + e.getMessage());
            // For now, just log the error and continue without failing the main operation
            System.out.println("WARNING: Audit entry could not be created, but main operation succeeded");
            return audit; // Return without setting ID
        }
        
        if (keyHolder.getKey() != null) {
            audit.setId(keyHolder.getKey().longValue());
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
    
    private final RowMapper<ConfigurationAudit> configurationAuditRowMapper = new RowMapper<ConfigurationAudit>() {
        @Override
        public ConfigurationAudit mapRow(ResultSet rs, int rowNum) throws SQLException {
            ConfigurationAudit audit = new ConfigurationAudit();
            audit.setId(rs.getLong("AUDIT_ID"));
            audit.setConfigId(rs.getString("CONFIG_ID"));
            audit.setAction(rs.getString("ACTION"));
            audit.setOldValue(rs.getString("OLD_VALUE"));
            audit.setNewValue(rs.getString("NEW_VALUE"));
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