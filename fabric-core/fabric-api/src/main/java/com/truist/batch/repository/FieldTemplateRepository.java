package com.truist.batch.repository;

import com.truist.batch.entity.FieldTemplateEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

/**
 * Repository for field template entities
 * JdbcTemplate implementation for Oracle database
 */
@Slf4j
@Repository
public class FieldTemplateRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<FieldTemplateEntity> rowMapper = new RowMapper<FieldTemplateEntity>() {
        @Override
        public FieldTemplateEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            FieldTemplateEntity entity = new FieldTemplateEntity();
            entity.setId(rs.getString("ID"));
            entity.setFileType(rs.getString("FILE_TYPE"));
            entity.setTransactionType(rs.getString("TRANSACTION_TYPE"));
            entity.setFieldName(rs.getString("FIELD_NAME"));

            // Handle potentially null integers safely
            Integer targetPosition = rs.getObject("TARGET_POSITION", Integer.class);
            entity.setTargetPosition(targetPosition != null ? targetPosition : 0);

            Integer length = rs.getObject("LENGTH", Integer.class);
            entity.setLength(length != null ? length : 0);

            entity.setDataType(rs.getString("DATA_TYPE"));
            entity.setFormat(rs.getString("FORMAT"));
            entity.setRequired(rs.getString("REQUIRED"));
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

            // Handle optional columns that may not exist in database schema
            try {
                entity.setDefaultValue(rs.getString("DEFAULT_VALUE"));
            } catch (SQLException e) {
                entity.setDefaultValue(null);
                log.debug("DEFAULT_VALUE column not found");
            }

            try {
                entity.setValidationRule(rs.getString("VALIDATION_RULE"));
            } catch (SQLException e) {
                entity.setValidationRule(null);
                log.debug("VALIDATION_RULE column not found");
            }

            try {
                Integer version = rs.getObject("VERSION", Integer.class);
                entity.setVersion(version != null ? version : 1);
            } catch (SQLException e) {
                entity.setVersion(1);
                log.debug("VERSION column not found, using default value 1");
            }

            try {
                entity.setTransformationType(rs.getString("TRANSFORMATION_TYPE"));
            } catch (SQLException e) {
                log.debug("TRANSFORMATION_TYPE column not found");
            }

            try {
                entity.setTransformationConfig(rs.getString("TRANSFORMATION_CONFIG"));
            } catch (SQLException e) {
                log.debug("TRANSFORMATION_CONFIG column not found");
            }

            try {
                entity.setValue(rs.getString("VALUE"));
            } catch (SQLException e) {
                log.debug("VALUE column not found");
            }

            try {
                entity.setSourceField(rs.getString("SOURCE_FIELD"));
            } catch (SQLException e) {
                log.debug("SOURCE_FIELD column not found");
            }

            return entity;
        }
    };

    // Basic row mapper for essential columns only - avoids ORA-17006 errors
    private RowMapper<FieldTemplateEntity> createBasicRowMapper() {
        return new RowMapper<FieldTemplateEntity>() {
            @Override
            public FieldTemplateEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
                FieldTemplateEntity entity = new FieldTemplateEntity();

                // Map only the columns we explicitly selected
                entity.setFileType(rs.getString("FILE_TYPE"));
                entity.setTransactionType(rs.getString("TRANSACTION_TYPE"));
                entity.setFieldName(rs.getString("FIELD_NAME"));

                // Handle potentially null integers safely
                Integer targetPosition = rs.getObject("TARGET_POSITION", Integer.class);
                entity.setTargetPosition(targetPosition != null ? targetPosition : 0);

                Integer length = rs.getObject("LENGTH", Integer.class);
                entity.setLength(length != null ? length : 0);

                entity.setDataType(rs.getString("DATA_TYPE"));
                entity.setFormat(rs.getString("FORMAT"));
                entity.setRequired(rs.getString("REQUIRED"));
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

                // Set defaults for columns we don't have
                entity.setId("generated_" + System.currentTimeMillis()); // Generate a temporary ID
                entity.setDefaultValue(null);
                entity.setValidationRule(null);
                entity.setVersion(1);

                // Try to map new fields if they exist in the RS (depends on query)
                try {
                    entity.setTransformationType(rs.getString("TRANSFORMATION_TYPE"));
                } catch (SQLException e) {
                }
                try {
                    entity.setTransformationConfig(rs.getString("TRANSFORMATION_CONFIG"));
                } catch (SQLException e) {
                }
                try {
                    entity.setValue(rs.getString("VALUE"));
                } catch (SQLException e) {
                }
                try {
                    entity.setSourceField(rs.getString("SOURCE_FIELD"));
                } catch (SQLException e) {
                }

                return entity;
            }
        };
    }

    public Optional<FieldTemplateEntity> findById(String id) {
        // Note: ID column may not exist in the actual schema, this method may not work
        try {
            String sql = "SELECT * FROM cm3int.field_templates WHERE ID = ?";
            List<FieldTemplateEntity> results = jdbcTemplate.query(sql, rowMapper, id);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            log.warn("findById not supported by current schema: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public List<FieldTemplateEntity> findAll() {
        try {
            String sql = "SELECT FILE_TYPE, TRANSACTION_TYPE, FIELD_NAME, TARGET_POSITION, LENGTH, " +
                    "DATA_TYPE, FORMAT, REQUIRED, DESCRIPTION, ENABLED, CREATED_BY, CREATED_DATE, " +
                    "MODIFIED_BY, MODIFIED_DATE, TRANSFORMATION_TYPE, TRANSFORMATION_CONFIG, VALUE, SOURCE_FIELD " +
                    "FROM cm3int.field_templates " +
                    "ORDER BY FILE_TYPE, TRANSACTION_TYPE, TARGET_POSITION";
            return jdbcTemplate.query(sql, createBasicRowMapper());
        } catch (Exception e) {
            log.error("Error in findAll: {}", e.getMessage());
            return List.of();
        }
    }

    public FieldTemplateEntity save(FieldTemplateEntity entity) {
        // Check if exists
        String checkSql = "SELECT COUNT(*) FROM cm3int.field_templates WHERE FILE_TYPE = ? AND TRANSACTION_TYPE = ? AND FIELD_NAME = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class,
                entity.getFileType(), entity.getTransactionType(), entity.getFieldName());

        if (count != null && count > 0) {
            // Update existing
            String updateSql = "UPDATE cm3int.field_templates SET TARGET_POSITION = ?, LENGTH = ?, " +
                    "DATA_TYPE = ?, FORMAT = ?, REQUIRED = ?, DESCRIPTION = ?, ENABLED = ?, " +
                    "MODIFIED_BY = ?, MODIFIED_DATE = SYSDATE, DEFAULT_VALUE = ?, VALIDATION_RULE = ?, " +
                    "TRANSFORMATION_TYPE = ?, TRANSFORMATION_CONFIG = ?, VALUE = ?, SOURCE_FIELD = ? " +
                    "WHERE FILE_TYPE = ? AND TRANSACTION_TYPE = ? AND FIELD_NAME = ?";
            jdbcTemplate.update(updateSql,
                    entity.getTargetPosition(), entity.getLength(), entity.getDataType(), entity.getFormat(),
                    entity.getRequired(), entity.getDescription(), entity.getEnabled(), entity.getModifiedBy(),
                    entity.getDefaultValue(), entity.getValidationRule(),
                    entity.getTransformationType(), entity.getTransformationConfig(), entity.getValue(),
                    entity.getSourceField(),
                    entity.getFileType(), entity.getTransactionType(), entity.getFieldName());
        } else {
            // Insert new - generate ID if not provided
            if (entity.getId() == null) {
                entity.setId(generateNewId());
            }
            String insertSql = "INSERT INTO cm3int.field_templates (ID, FILE_TYPE, TRANSACTION_TYPE, " +
                    "FIELD_NAME, TARGET_POSITION, LENGTH, DATA_TYPE, FORMAT, REQUIRED, " +
                    "DESCRIPTION, ENABLED, CREATED_BY, CREATED_DATE, DEFAULT_VALUE, VALIDATION_RULE, VERSION, " +
                    "TRANSFORMATION_TYPE, TRANSFORMATION_CONFIG, VALUE, SOURCE_FIELD) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE, ?, ?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(insertSql,
                    entity.getId(), entity.getFileType(), entity.getTransactionType(), entity.getFieldName(),
                    entity.getTargetPosition(), entity.getLength(), entity.getDataType(), entity.getFormat(),
                    entity.getRequired(), entity.getDescription(), entity.getEnabled(), entity.getCreatedBy(),
                    entity.getDefaultValue(), entity.getValidationRule(), entity.getVersion(),
                    entity.getTransformationType(), entity.getTransformationConfig(), entity.getValue(),
                    entity.getSourceField());
        }
        return entity;
    }

    public void deleteById(String id) {
        String sql = "DELETE FROM cm3int.field_templates WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }

    public List<FieldTemplateEntity> findByTemplateId(String templateId) {
        // Assuming templateId format is "fileType_transactionType"
        String[] parts = templateId.split("_", 2);
        if (parts.length == 2) {
            return findByFileTypeAndTransactionTypeAndEnabledOrderByTargetPosition(parts[0], parts[1], "Y");
        }
        return List.of();
    }

    public List<FieldTemplateEntity> findByFileTypeAndTransactionTypeAndEnabledOrderByTargetPosition(String fileType,
            String transactionType, String enabled) {
        try {
            // First, let's test basic table access
            testTableAccess();

            // Use specific column list instead of SELECT * to avoid ORA-17006 errors
            String sql = "SELECT FILE_TYPE, TRANSACTION_TYPE, FIELD_NAME, TARGET_POSITION, LENGTH, " +
                    "DATA_TYPE, FORMAT, REQUIRED, DESCRIPTION, ENABLED, CREATED_BY, CREATED_DATE, " +
                    "MODIFIED_BY, MODIFIED_DATE " +
                    "FROM cm3int.field_templates " +
                    "WHERE FILE_TYPE = ? AND TRANSACTION_TYPE = ? AND (ENABLED = ? OR ENABLED IS NULL) " +
                    "ORDER BY TARGET_POSITION";

            List<FieldTemplateEntity> result = jdbcTemplate.query(sql, createBasicRowMapper(), fileType,
                    transactionType, enabled);
            log.info("✅ FIXED: Query executed successfully for {}/{}/{}, found {} results", fileType, transactionType,
                    enabled, result.size());

            // If no results found with enabled filter, try without filter to see what's
            // available
            if (result.isEmpty()) {
                String debugSql = "SELECT FILE_TYPE, TRANSACTION_TYPE, FIELD_NAME, TARGET_POSITION, LENGTH, " +
                        "DATA_TYPE, FORMAT, REQUIRED, DESCRIPTION, ENABLED, CREATED_BY, CREATED_DATE, " +
                        "MODIFIED_BY, MODIFIED_DATE " +
                        "FROM cm3int.field_templates " +
                        "WHERE FILE_TYPE = ? AND TRANSACTION_TYPE = ? " +
                        "ORDER BY TARGET_POSITION";

                List<FieldTemplateEntity> allResults = jdbcTemplate.query(debugSql, createBasicRowMapper(), fileType,
                        transactionType);
                log.info("🔍 DEBUG: Found {} total records for {}/{} (any enabled status)", allResults.size(), fileType,
                        transactionType);

                if (!allResults.isEmpty()) {
                    log.info("📋 Sample enabled values: {}",
                            allResults.stream().limit(3).map(FieldTemplateEntity::getEnabled).toList());
                }
            }

            return result;
        } catch (Exception e) {
            log.error("Error querying field_templates table for {}/{}/{}: {}", fileType, transactionType, enabled,
                    e.getMessage());
            log.info("Assuming field_templates table doesn't exist or has different schema, returning empty list");
            return List.of(); // Return empty list instead of throwing exception
        }
    }

    private void testTableAccess() {
        try {
            // Test 1: Simple count query
            String countSql = "SELECT COUNT(*) FROM cm3int.field_templates";
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
            log.info("✅ Table access test: field_templates table exists with {} records", count);

            // Test 2: Check if table has any data for our file type
            if (count > 0) {
                String distinctSql = "SELECT DISTINCT FILE_TYPE FROM cm3int.field_templates";
                List<String> fileTypes = jdbcTemplate.queryForList(distinctSql, String.class);
                log.info("📋 Available file types in field_templates: {}", fileTypes);

                // Test 3: Get actual column information
                String sampleSql = "SELECT * FROM cm3int.field_templates WHERE ROWNUM = 1";
                try {
                    jdbcTemplate.queryForMap(sampleSql);
                    log.info("🔍 Single row query succeeded - schema appears compatible");
                } catch (Exception schemaError) {
                    log.error("🚫 Single row query failed: {}", schemaError.getMessage());

                    // Try to discover available columns
                    String columnsSql = "SELECT column_name FROM user_tab_columns WHERE table_name = 'FIELD_TEMPLATES' ORDER BY column_id";
                    try {
                        List<String> columns = jdbcTemplate.queryForList(columnsSql, String.class);
                        log.info("📋 Available columns in field_templates: {}", columns);
                    } catch (Exception columnError) {
                        log.warn("⚠️ Could not retrieve column information: {}", columnError.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("🚫 Table access test failed: {}", e.getMessage());
        }
    }

    public List<String> findTransactionTypesByFileType(String fileType) {
        try {
            String sql = "SELECT DISTINCT TRANSACTION_TYPE FROM cm3int.field_templates WHERE FILE_TYPE = ? AND ENABLED = 'Y' ORDER BY TRANSACTION_TYPE";
            List<String> result = jdbcTemplate.queryForList(sql, String.class, fileType);
            log.debug("Found {} transaction types for fileType: {}", result.size(), fileType);
            return result;
        } catch (Exception e) {
            log.error("Error querying transaction types from field_templates for fileType: {}: {}", fileType,
                    e.getMessage());
            log.info("Field templates table may not exist, falling back to transaction_types table");

            // Fallback to the transaction_types table we know exists
            try {
                String fallbackSql = "SELECT DISTINCT TRANSACTION_TYPE FROM cm3int.transaction_types WHERE ENABLED = 'Y' ORDER BY TRANSACTION_TYPE";
                return jdbcTemplate.queryForList(fallbackSql, String.class);
            } catch (Exception fallbackError) {
                log.warn("Fallback query also failed, returning empty list: {}", fallbackError.getMessage());
                return List.of();
            }
        }
    }

    public Optional<FieldTemplateEntity> findByFileTypeAndTransactionTypeAndFieldName(String fileType,
            String transactionType, String fieldName) {
        try {
            String sql = "SELECT FILE_TYPE, TRANSACTION_TYPE, FIELD_NAME, TARGET_POSITION, LENGTH, " +
                    "DATA_TYPE, FORMAT, REQUIRED, DESCRIPTION, ENABLED, CREATED_BY, CREATED_DATE, " +
                    "MODIFIED_BY, MODIFIED_DATE " +
                    "FROM cm3int.field_templates " +
                    "WHERE FILE_TYPE = ? AND TRANSACTION_TYPE = ? AND FIELD_NAME = ?";
            List<FieldTemplateEntity> results = jdbcTemplate.query(sql, createBasicRowMapper(), fileType,
                    transactionType, fieldName);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            log.error("Error finding by fileType/transactionType/fieldName: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<FieldTemplateEntity> findByFileTypeAndTransactionTypeAndTargetPosition(String fileType,
            String transactionType, Integer targetPosition) {
        try {
            String sql = "SELECT FILE_TYPE, TRANSACTION_TYPE, FIELD_NAME, TARGET_POSITION, LENGTH, " +
                    "DATA_TYPE, FORMAT, REQUIRED, DESCRIPTION, ENABLED, CREATED_BY, CREATED_DATE, " +
                    "MODIFIED_BY, MODIFIED_DATE " +
                    "FROM cm3int.field_templates " +
                    "WHERE FILE_TYPE = ? AND TRANSACTION_TYPE = ? AND TARGET_POSITION = ?";
            List<FieldTemplateEntity> results = jdbcTemplate.query(sql, createBasicRowMapper(), fileType,
                    transactionType, targetPosition);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            log.error("Error finding by fileType/transactionType/targetPosition: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public List<FieldTemplateEntity> findByFileType(String fileType) {
        try {
            String sql = "SELECT FILE_TYPE, TRANSACTION_TYPE, FIELD_NAME, TARGET_POSITION, LENGTH, " +
                    "DATA_TYPE, FORMAT, REQUIRED, DESCRIPTION, ENABLED, CREATED_BY, CREATED_DATE, " +
                    "MODIFIED_BY, MODIFIED_DATE " +
                    "FROM cm3int.field_templates " +
                    "WHERE FILE_TYPE = ? AND ENABLED = 'Y' " +
                    "ORDER BY TRANSACTION_TYPE, TARGET_POSITION";
            return jdbcTemplate.query(sql, createBasicRowMapper(), fileType);
        } catch (Exception e) {
            log.error("Error finding by fileType: {}", e.getMessage());
            return List.of();
        }
    }

    public void deleteByFileTypeAndTransactionTypeAndFieldName(String fileType, String transactionType,
            String fieldName) {
        String sql = "DELETE FROM cm3int.field_templates WHERE FILE_TYPE = ? AND TRANSACTION_TYPE = ? AND FIELD_NAME = ?";
        jdbcTemplate.update(sql, fileType, transactionType, fieldName);
    }

    private String generateNewId() {
        String sql = "SELECT 'FLD_' || LPAD(NVL(MAX(TO_NUMBER(SUBSTR(ID, 5))), 0) + 1, 6, '0') FROM cm3int.field_templates WHERE ID LIKE 'FLD_%'";
        return jdbcTemplate.queryForObject(sql, String.class);
    }
}