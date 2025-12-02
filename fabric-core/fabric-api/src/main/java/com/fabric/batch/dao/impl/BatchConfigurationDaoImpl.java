package com.fabric.batch.dao.impl;

import com.fabric.batch.dao.BatchConfigurationDao;
import com.fabric.batch.model.BatchConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class BatchConfigurationDaoImpl implements BatchConfigurationDao {
    
    private final JdbcTemplate jdbcTemplate;
    
    private static final String SCHEMA = "CM3INT";
    private static final String TABLE_NAME = SCHEMA + ".BATCH_CONFIGURATIONS";
    
    private static final String INSERT_SQL = 
        "INSERT INTO " + TABLE_NAME + " (ID, SOURCE_SYSTEM, JOB_NAME, TRANSACTION_TYPE, CONFIGURATION_JSON, " +
        "ENABLED, CREATED_BY, CREATED_DATE, MODIFIED_BY, MODIFIED_DATE, VERSION, DESCRIPTION) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String UPDATE_SQL = 
        "UPDATE " + TABLE_NAME + " SET SOURCE_SYSTEM = ?, JOB_NAME = ?, TRANSACTION_TYPE = ?, " +
        "CONFIGURATION_JSON = ?, ENABLED = ?, MODIFIED_BY = ?, MODIFIED_DATE = ?, " +
        "VERSION = ?, DESCRIPTION = ? WHERE ID = ?";
    
    private static final String SELECT_BY_ID_SQL = 
        "SELECT * FROM " + TABLE_NAME + " WHERE ID = ?";
    
    private static final String SELECT_BY_SOURCE_SYSTEM_JOB_TRANSACTION_SQL = 
        "SELECT * FROM " + TABLE_NAME + " WHERE SOURCE_SYSTEM = ? AND JOB_NAME = ? AND TRANSACTION_TYPE = ?";
    
    private static final String SELECT_BY_SOURCE_SYSTEM_SQL = 
        "SELECT * FROM " + TABLE_NAME + " WHERE SOURCE_SYSTEM = ?";
    
    private static final String SELECT_ALL_ENABLED_SQL = 
        "SELECT * FROM " + TABLE_NAME + " WHERE ENABLED = 'Y'";
    
    private static final String SELECT_BY_SOURCE_SYSTEM_AND_JOB_SQL = 
        "SELECT * FROM " + TABLE_NAME + " WHERE SOURCE_SYSTEM = ? AND JOB_NAME = ? ORDER BY TRANSACTION_TYPE";
    
    private static final String COUNT_BY_SOURCE_SYSTEM_SQL = 
        "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE SOURCE_SYSTEM = ?";
    
    private static final String DELETE_BY_ID_SQL = 
        "DELETE FROM " + TABLE_NAME + " WHERE ID = ?";
    
    private static final String EXISTS_BY_ID_SQL = 
        "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE ID = ?";
    
    @Autowired
    public BatchConfigurationDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public BatchConfiguration save(BatchConfiguration configuration) {
        // Validate and set defaults for required fields
        validateAndSetDefaults(configuration);
        
        if (configuration.getId() == null) {
            configuration.setId(generateId(configuration));
        }
        
        // Check for duplicates before saving (only for new configurations)
        if (!existsById(configuration.getId())) {
            checkForDuplicateConfiguration(configuration);
        }
        
        if (existsById(configuration.getId())) {
            configuration.setLastModifiedDate(LocalDateTime.now());
            jdbcTemplate.update(UPDATE_SQL,
                configuration.getSourceSystem(),
                configuration.getJobName(),
                configuration.getTransactionType(),
                configuration.getConfigurationJson(),
                configuration.getEnabled(),
                configuration.getLastModifiedBy(),
                Timestamp.valueOf(configuration.getLastModifiedDate()),
                Integer.parseInt(configuration.getVersion()), // Convert to integer
                configuration.getDescription(),
                configuration.getId());
        } else {
            if (configuration.getCreatedDate() == null) {
                configuration.setCreatedDate(LocalDateTime.now());
            }
            
            // Debug logging to see exact values being inserted
            System.out.println("INSERT VALUES: ID=" + configuration.getId() + 
                              ", SOURCE_SYSTEM=" + configuration.getSourceSystem() + 
                              ", JOB_NAME=" + configuration.getJobName() + 
                              ", TRANSACTION_TYPE=" + configuration.getTransactionType() + 
                              ", ENABLED=" + configuration.getEnabled() + 
                              ", CREATED_BY=" + configuration.getCreatedBy() + 
                              ", VERSION=" + configuration.getVersion() + 
                              ", DESCRIPTION=" + configuration.getDescription());
            
            jdbcTemplate.update(INSERT_SQL,
                configuration.getId(),
                configuration.getSourceSystem(),
                configuration.getJobName(),
                configuration.getTransactionType(),
                configuration.getConfigurationJson(),
                configuration.getEnabled(),
                configuration.getCreatedBy(),
                Timestamp.valueOf(configuration.getCreatedDate()),
                configuration.getLastModifiedBy(),
                configuration.getLastModifiedDate() != null ? Timestamp.valueOf(configuration.getLastModifiedDate()) : null,
                Integer.parseInt(configuration.getVersion()), // Convert to integer
                configuration.getDescription());
        }
        
        return configuration;
    }
    
    @Override
    public Optional<BatchConfiguration> findById(String id) {
        try {
            BatchConfiguration config = jdbcTemplate.queryForObject(SELECT_BY_ID_SQL, batchConfigurationRowMapper, id);
            return Optional.ofNullable(config);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<BatchConfiguration> findBySourceSystemAndJobNameAndTransactionType(
            String sourceSystem, String jobName, String transactionType) {
        try {
            BatchConfiguration config = jdbcTemplate.queryForObject(
                SELECT_BY_SOURCE_SYSTEM_JOB_TRANSACTION_SQL, 
                batchConfigurationRowMapper, 
                sourceSystem, jobName, transactionType);
            return Optional.ofNullable(config);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    @Override
    public List<BatchConfiguration> findBySourceSystem(String sourceSystem) {
        return jdbcTemplate.query(SELECT_BY_SOURCE_SYSTEM_SQL, batchConfigurationRowMapper, sourceSystem);
    }
    
    @Override
    public List<BatchConfiguration> findAllEnabled() {
        return jdbcTemplate.query(SELECT_ALL_ENABLED_SQL, batchConfigurationRowMapper);
    }
    
    @Override
    public List<BatchConfiguration> findBySourceSystemAndJobName(String sourceSystem, String jobName) {
        return jdbcTemplate.query(SELECT_BY_SOURCE_SYSTEM_AND_JOB_SQL, batchConfigurationRowMapper, sourceSystem, jobName);
    }
    
    @Override
    public long countBySourceSystem(String sourceSystem) {
        return jdbcTemplate.queryForObject(COUNT_BY_SOURCE_SYSTEM_SQL, Long.class, sourceSystem);
    }
    
    @Override
    public void deleteById(String id) {
        jdbcTemplate.update(DELETE_BY_ID_SQL, id);
    }
    
    @Override
    public boolean existsById(String id) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_BY_ID_SQL, Integer.class, id);
        return count != null && count > 0;
    }
    
    private String generateId(BatchConfiguration config) {
        return config.getSourceSystem() + "-" + config.getJobName() + "-" + 
               config.getTransactionType() + "-" + System.currentTimeMillis();
    }
    
    /**
     * Check for duplicate configurations based on the unique constraint:
     * (SOURCE_SYSTEM, JOB_NAME, TRANSACTION_TYPE)
     */
    private void checkForDuplicateConfiguration(BatchConfiguration configuration) {
        Optional<BatchConfiguration> existing = findBySourceSystemAndJobNameAndTransactionType(
            configuration.getSourceSystem(), 
            configuration.getJobName(), 
            configuration.getTransactionType()
        );
        
        if (existing.isPresent()) {
            String message = String.format(
                "Configuration already exists for SOURCE_SYSTEM='%s', JOB_NAME='%s', TRANSACTION_TYPE='%s'. " +
                "Existing configuration ID: %s. Please use a different combination or update the existing configuration.",
                configuration.getSourceSystem(),
                configuration.getJobName(), 
                configuration.getTransactionType(),
                existing.get().getId()
            );
            System.out.println("DUPLICATE CHECK FAILED: " + message);
            throw new IllegalArgumentException(message);
        }
    }
    
    private void validateAndSetDefaults(BatchConfiguration config) {
        // Ensure all required fields have valid values
        if (config.getSourceSystem() == null || config.getSourceSystem().trim().isEmpty()) {
            throw new IllegalArgumentException("Source system cannot be null or empty");
        }
        if (config.getJobName() == null || config.getJobName().trim().isEmpty()) {
            throw new IllegalArgumentException("Job name cannot be null or empty");
        }
        if (config.getConfigurationJson() == null || config.getConfigurationJson().trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration JSON cannot be null or empty");
        }
        
        // Set defaults for optional fields if null
        if (config.getTransactionType() == null) {
            config.setTransactionType("200");
        }
        if (config.getEnabled() == null) {
            config.setEnabled("Y");
        }
        if (config.getVersion() == null) {
            config.setVersion("1");
        }
        if (config.getCreatedBy() == null) {
            config.setCreatedBy("system");
        }
        if (config.getDescription() == null) {
            config.setDescription("Configuration for " + config.getSourceSystem() + "/" + config.getJobName());
        }
        if (config.getCreatedDate() == null) {
            config.setCreatedDate(LocalDateTime.now());
        }
    }
    
    private final RowMapper<BatchConfiguration> batchConfigurationRowMapper = new RowMapper<BatchConfiguration>() {
        @Override
        public BatchConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
            BatchConfiguration config = new BatchConfiguration();
            config.setId(rs.getString("ID"));
            config.setSourceSystem(rs.getString("SOURCE_SYSTEM"));
            config.setJobName(rs.getString("JOB_NAME"));
            config.setTransactionType(rs.getString("TRANSACTION_TYPE"));
            config.setConfigurationJson(rs.getString("CONFIGURATION_JSON"));
            config.setEnabled(rs.getString("ENABLED"));
            config.setCreatedBy(rs.getString("CREATED_BY"));
            
            Timestamp createdDate = rs.getTimestamp("CREATED_DATE");
            if (createdDate != null) {
                config.setCreatedDate(createdDate.toLocalDateTime());
            }
            
            config.setLastModifiedBy(rs.getString("MODIFIED_BY"));
            
            Timestamp lastModifiedDate = rs.getTimestamp("MODIFIED_DATE");
            if (lastModifiedDate != null) {
                config.setLastModifiedDate(lastModifiedDate.toLocalDateTime());
            }
            
            // FILE_PATH column does not exist in actual table structure
            // config.setFilePath(rs.getString("FILE_PATH"));
            config.setVersion(String.valueOf(rs.getInt("VERSION")));
            config.setDescription(rs.getString("DESCRIPTION"));
            
            return config;
        }
    };
}