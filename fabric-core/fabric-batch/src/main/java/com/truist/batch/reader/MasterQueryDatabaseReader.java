package com.truist.batch.reader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Database reader that uses master queries stored in MASTER_QUERY_CONFIG table.
 * Supports parameterized queries with batch date and other job parameters.
 * 
 * @author Senior Full Stack Developer Agent
 * @since Phase 3 - Batch Execution Enhancement
 */
@Component
@StepScope
@Slf4j
public class MasterQueryDatabaseReader extends JdbcCursorItemReader<Map<String, Object>> {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Value("#{jobParameters['sourceSystem']}")
    private String sourceSystem;
    
    @Value("#{jobParameters['jobName']}")
    private String jobName;
    
    @Value("#{jobParameters['batchDate']}")
    private String batchDate;
    
    @Value("#{jobParameters['configId']}")
    private String configId;
    
    /**
     * Initialize the reader with master query from database
     */
    @Autowired
    public void init(DataSource dataSource) {
        log.info("Initializing MasterQueryDatabaseReader for sourceSystem: {}, jobName: {}", 
                sourceSystem, jobName);
        
        this.setDataSource(dataSource);
        this.setRowMapper(new ColumnMapRowMapper());
        this.setVerifyCursorPosition(false);
        
        // Retrieve master query from database
        String masterQuery = getMasterQuery();
        
        if (masterQuery != null && !masterQuery.isEmpty()) {
            log.info("Using master query from database for {}/{}", sourceSystem, jobName);
            
            // Replace named parameters with positional parameters for JDBC
            String jdbcQuery = prepareJdbcQuery(masterQuery);
            this.setSql(jdbcQuery);
            
            // Set query parameters
            this.setPreparedStatementSetter(new PreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps) throws SQLException {
                    // Set batch date parameter if query contains it
                    if (jdbcQuery.contains("?")) {
                        LocalDate date = LocalDate.parse(batchDate, DateTimeFormatter.ISO_DATE);
                        ps.setDate(1, Date.valueOf(date));
                        log.debug("Set batch date parameter: {}", batchDate);
                    }
                }
            });
            
        } else {
            // Fallback to default test query if no master query found
            log.warn("No master query found, using default test query");
            String defaultQuery = "SELECT * FROM ENCORE_TEST_DATA WHERE BATCH_DATE = ?";
            this.setSql(defaultQuery);
            
            this.setPreparedStatementSetter(new PreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps) throws SQLException {
                    LocalDate date = LocalDate.parse(batchDate, DateTimeFormatter.ISO_DATE);
                    ps.setDate(1, Date.valueOf(date));
                }
            });
        }
        
        // Set fetch size for performance
        this.setFetchSize(100);
        this.setQueryTimeout(30);
        
        log.info("MasterQueryDatabaseReader initialized successfully");
    }
    
    /**
     * Retrieve master query from MASTER_QUERY_CONFIG table
     */
    private String getMasterQuery() {
        try {
            String sql = "SELECT QUERY_TEXT FROM MASTER_QUERY_CONFIG " +
                        "WHERE SOURCE_SYSTEM = ? AND JOB_NAME = ? AND IS_ACTIVE = 'Y' " +
                        "ORDER BY VERSION DESC";
            
            String query = jdbcTemplate.queryForObject(sql, String.class, sourceSystem, jobName);
            log.debug("Retrieved master query: {}", query);
            return query;
            
        } catch (Exception e) {
            log.error("Failed to retrieve master query for {}/{}", sourceSystem, jobName, e);
            return null;
        }
    }
    
    /**
     * Convert named parameters to positional parameters for JDBC
     */
    private String prepareJdbcQuery(String namedQuery) {
        // Replace :batchDate with ? for JDBC PreparedStatement
        String jdbcQuery = namedQuery.replaceAll(":batchDate", "?");
        
        log.debug("Converted query from named to positional parameters: {}", jdbcQuery);
        return jdbcQuery;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // Override to prevent premature initialization
        // Initialization is handled in init() method
    }
}