package com.truist.batch.repository;

import com.truist.batch.entity.FileTypeTemplateEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository for file type template entities
 * JdbcTemplate implementation for Oracle database
 */
@Repository
public class FileTypeTemplateRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final RowMapper<FileTypeTemplateEntity> rowMapper = new RowMapper<FileTypeTemplateEntity>() {
        @Override
        public FileTypeTemplateEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            FileTypeTemplateEntity entity = new FileTypeTemplateEntity();
            entity.setFileType(rs.getString("FILE_TYPE"));
            entity.setDescription(rs.getString("DESCRIPTION"));
            entity.setTotalFields(rs.getInt("TOTAL_FIELDS"));
            entity.setRecordLength(rs.getInt("RECORD_LENGTH"));
            entity.setEnabled(rs.getString("ENABLED"));
            entity.setCreatedBy(rs.getString("CREATED_BY"));
            
            // Convert Timestamp to LocalDateTime
            java.sql.Timestamp createdTimestamp = rs.getTimestamp("CREATED_DATE");
            if (createdTimestamp != null) {
                entity.setCreatedDate(createdTimestamp.toLocalDateTime());
            }
            
            entity.setModifiedBy(rs.getString("MODIFIED_BY"));
            
            // Convert Timestamp to LocalDateTime
            java.sql.Timestamp modifiedTimestamp = rs.getTimestamp("MODIFIED_DATE");
            if (modifiedTimestamp != null) {
                entity.setModifiedDate(modifiedTimestamp.toLocalDateTime());
            }
            
            return entity;
        }
    };
    
    public Optional<FileTypeTemplateEntity> findById(String id) {
        String sql = "SELECT * FROM cm3int.file_type_templates WHERE FILE_TYPE = ?";
        List<FileTypeTemplateEntity> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public List<FileTypeTemplateEntity> findAll() {
        String sql = "SELECT * FROM cm3int.file_type_templates ORDER BY FILE_TYPE";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    public FileTypeTemplateEntity save(FileTypeTemplateEntity entity) {
        // Check if exists
        String checkSql = "SELECT COUNT(*) FROM cm3int.file_type_templates WHERE FILE_TYPE = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, entity.getFileType());
        
        if (count != null && count > 0) {
            // Update existing
            String updateSql = "UPDATE cm3int.file_type_templates SET DESCRIPTION = ?, TOTAL_FIELDS = ?, " +
                             "RECORD_LENGTH = ?, ENABLED = ?, MODIFIED_BY = ?, MODIFIED_DATE = SYSDATE " +
                             "WHERE FILE_TYPE = ?";
            jdbcTemplate.update(updateSql, entity.getDescription(), entity.getTotalFields(),
                              entity.getRecordLength(), entity.getEnabled(), entity.getModifiedBy(),
                              entity.getFileType());
        } else {
            // Insert new
            String insertSql = "INSERT INTO cm3int.file_type_templates (FILE_TYPE, DESCRIPTION, TOTAL_FIELDS, " +
                             "RECORD_LENGTH, ENABLED, CREATED_BY, CREATED_DATE) VALUES (?, ?, ?, ?, ?, ?, SYSDATE)";
            jdbcTemplate.update(insertSql, entity.getFileType(), entity.getDescription(), 
                              entity.getTotalFields(), entity.getRecordLength(), entity.getEnabled(),
                              entity.getCreatedBy());
        }
        return entity;
    }
    
    public void deleteById(String id) {
        String sql = "DELETE FROM cm3int.file_type_templates WHERE FILE_TYPE = ?";
        jdbcTemplate.update(sql, id);
    }
    
    public List<FileTypeTemplateEntity> findByFileType(String fileType) {
        String sql = "SELECT * FROM cm3int.file_type_templates WHERE FILE_TYPE = ?";
        return jdbcTemplate.query(sql, rowMapper, fileType);
    }
    
    public List<FileTypeTemplateEntity> findAllEnabled() {
        String sql = "SELECT * FROM cm3int.file_type_templates WHERE ENABLED = 'Y' ORDER BY FILE_TYPE";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    public Optional<FileTypeTemplateEntity> findByFileTypeAndEnabled(String fileType, String enabled) {
        String sql = "SELECT * FROM cm3int.file_type_templates WHERE FILE_TYPE = ? AND ENABLED = ?";
        List<FileTypeTemplateEntity> results = jdbcTemplate.query(sql, rowMapper, fileType, enabled);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public Optional<FileTypeTemplateEntity> findByFileTypeAndTransactionTypeAndEnabled(String fileType, String transactionType, String enabled) {
        // This table doesn't have transaction type, so just return by file type and enabled
        return findByFileTypeAndEnabled(fileType, enabled);
    }
}