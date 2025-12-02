package com.fabric.batch.repository;

import com.fabric.batch.entity.BatchTransactionTypeEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository for batch transaction type entities
 * JdbcTemplate implementation for Oracle database
 */
@Repository
public class BatchTransactionTypeRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final RowMapper<BatchTransactionTypeEntity> rowMapper = new RowMapper<BatchTransactionTypeEntity>() {
        @Override
        public BatchTransactionTypeEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            BatchTransactionTypeEntity entity = new BatchTransactionTypeEntity();
            entity.setId(rs.getString("ID"));
            entity.setTransactionType(rs.getString("TRANSACTION_TYPE"));
            entity.setDescription(rs.getString("DESCRIPTION"));
            entity.setEnabled(rs.getString("ENABLED"));
            entity.setCreatedBy(rs.getString("CREATED_BY"));
            
            // Convert Timestamp to LocalDateTime
            java.sql.Timestamp createdTimestamp = rs.getTimestamp("CREATED_DATE");
            if (createdTimestamp != null) {
                entity.setCreatedDate(createdTimestamp.toLocalDateTime());
            }
            
            entity.setModifiedBy(rs.getString("MODIFIED_BY"));
            
            java.sql.Timestamp modifiedTimestamp = rs.getTimestamp("MODIFIED_DATE");
            if (modifiedTimestamp != null) {
                entity.setModifiedDate(modifiedTimestamp.toLocalDateTime());
            }
            
            return entity;
        }
    };
    
    public Optional<BatchTransactionTypeEntity> findById(String id) {
        String sql = "SELECT * FROM cm3int.transaction_types WHERE ID = ?";
        List<BatchTransactionTypeEntity> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public List<BatchTransactionTypeEntity> findAll() {
        String sql = "SELECT * FROM cm3int.transaction_types ORDER BY TRANSACTION_TYPE";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    public BatchTransactionTypeEntity save(BatchTransactionTypeEntity entity) {
        // Check if exists
        String checkSql = "SELECT COUNT(*) FROM cm3int.transaction_types WHERE TRANSACTION_TYPE = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, entity.getTransactionType());
        
        if (count != null && count > 0) {
            // Update existing
            String updateSql = "UPDATE cm3int.transaction_types SET DESCRIPTION = ?, ENABLED = ?, " +
                             "MODIFIED_BY = ?, MODIFIED_DATE = SYSDATE WHERE TRANSACTION_TYPE = ?";
            jdbcTemplate.update(updateSql, entity.getDescription(), entity.getEnabled(),
                              entity.getModifiedBy(), entity.getTransactionType());
        } else {
            // Insert new - generate ID if not provided
            if (entity.getId() == null) {
                entity.setId(generateNewId());
            }
            String insertSql = "INSERT INTO cm3int.transaction_types (ID, TRANSACTION_TYPE, DESCRIPTION, " +
                             "ENABLED, CREATED_BY, CREATED_DATE) VALUES (?, ?, ?, ?, ?, SYSDATE)";
            jdbcTemplate.update(insertSql, entity.getId(), entity.getTransactionType(), 
                              entity.getDescription(), entity.getEnabled(), entity.getCreatedBy());
        }
        return entity;
    }
    
    public void deleteById(String id) {
        String sql = "DELETE FROM cm3int.transaction_types WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }
    
    public List<BatchTransactionTypeEntity> findByTransactionType(String transactionType) {
        String sql = "SELECT * FROM cm3int.transaction_types WHERE TRANSACTION_TYPE = ?";
        return jdbcTemplate.query(sql, rowMapper, transactionType);
    }
    
    public List<BatchTransactionTypeEntity> findByEnabled(String enabled) {
        String sql = "SELECT * FROM cm3int.transaction_types WHERE ENABLED = ? ORDER BY TRANSACTION_TYPE";
        return jdbcTemplate.query(sql, rowMapper, enabled);
    }
    
    public Optional<BatchTransactionTypeEntity> findByTransactionTypeAndEnabled(String transactionType, String enabled) {
        String sql = "SELECT * FROM cm3int.transaction_types WHERE TRANSACTION_TYPE = ? AND ENABLED = ?";
        List<BatchTransactionTypeEntity> results = jdbcTemplate.query(sql, rowMapper, transactionType, enabled);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public long countByEnabled(String enabled) {
        String sql = "SELECT COUNT(*) FROM cm3int.transaction_types WHERE ENABLED = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, enabled);
    }
    
    public List<String> findDistinctTransactionTypesByEnabled(String enabled) {
        String sql = "SELECT DISTINCT TRANSACTION_TYPE FROM cm3int.transaction_types WHERE ENABLED = ? ORDER BY TRANSACTION_TYPE";
        return jdbcTemplate.queryForList(sql, String.class, enabled);
    }
    
    private String generateNewId() {
        String sql = "SELECT 'TXN_' || LPAD(NVL(MAX(TO_NUMBER(SUBSTR(ID, 5))), 0) + 1, 6, '0') FROM cm3int.transaction_types WHERE ID LIKE 'TXN_%'";
        return jdbcTemplate.queryForObject(sql, String.class);
    }
}