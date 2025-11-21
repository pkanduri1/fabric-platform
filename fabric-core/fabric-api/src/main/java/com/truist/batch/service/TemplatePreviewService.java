package com.truist.batch.service;

import com.truist.batch.dto.QueryPreviewResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for executing SQL query previews in Template Studio.
 * Provides read-only query execution with security and performance controls.
 */
@Service
@Slf4j
public class TemplatePreviewService {

    private final JdbcTemplate localReadOnlyJdbcTemplate;

    private static final List<String> FORBIDDEN_KEYWORDS = Arrays.asList(
        "INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "ALTER", "CREATE", "EXEC", "EXECUTE", "MERGE"
    );

    public TemplatePreviewService(@Qualifier("readOnlyJdbcTemplate") JdbcTemplate localReadOnlyJdbcTemplate) {
        this.localReadOnlyJdbcTemplate = localReadOnlyJdbcTemplate;
        // Set query timeout to 10 seconds
        this.localReadOnlyJdbcTemplate.setQueryTimeout(10);
    }

    /**
     * Execute a read-only SQL query and return preview results.
     *
     * @param sql SQL query to execute (must be SELECT statement)
     * @param maxRows Maximum number of rows to return (1-100)
     * @return QueryPreviewResponse with results or error information
     */
    public QueryPreviewResponse executePreviewQuery(String sql, int maxRows) {
        long startTime = System.currentTimeMillis();

        // Validate SQL
        String validationError = validateSql(sql);
        if (validationError != null) {
            return QueryPreviewResponse.builder()
                .success(false)
                .columns(new ArrayList<>())
                .rows(new ArrayList<>())
                .rowCount(0)
                .executionTimeMs(0)
                .message(validationError)
                .build();
        }

        // Add ROWNUM limit for Oracle
        String limitedSql = addRowLimit(sql, maxRows);

        try {
            List<String> columns = new ArrayList<>();
            List<List<String>> rows = new ArrayList<>();

            localReadOnlyJdbcTemplate.query(limitedSql, rs -> {
                // Extract column names on first row
                if (columns.isEmpty()) {
                    try {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        for (int i = 1; i <= columnCount; i++) {
                            columns.add(metaData.getColumnName(i));
                        }
                    } catch (SQLException e) {
                        log.error("Error extracting column metadata", e);
                    }
                }

                // Extract row data
                List<String> row = new ArrayList<>();
                try {
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        Object value = rs.getObject(i);
                        row.add(value != null ? value.toString() : "");
                    }
                } catch (SQLException e) {
                    log.error("Error extracting row data", e);
                }
                rows.add(row);
            });

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("Query preview executed successfully: {} rows in {}ms", rows.size(), executionTime);

            return QueryPreviewResponse.builder()
                .success(true)
                .columns(columns)
                .rows(rows)
                .rowCount(rows.size())
                .executionTimeMs(executionTime)
                .message("Query executed successfully")
                .build();

        } catch (DataAccessException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Query execution failed", e);

            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("ORA-")) {
                // Extract Oracle error message
                int oraIndex = errorMessage.indexOf("ORA-");
                if (oraIndex >= 0) {
                    errorMessage = errorMessage.substring(oraIndex);
                    if (errorMessage.length() > 200) {
                        errorMessage = errorMessage.substring(0, 200) + "...";
                    }
                }
            } else {
                errorMessage = "Query execution failed: " + (errorMessage != null ? errorMessage : "Unknown error");
            }

            return QueryPreviewResponse.builder()
                .success(false)
                .columns(new ArrayList<>())
                .rows(new ArrayList<>())
                .rowCount(0)
                .executionTimeMs(executionTime)
                .message(errorMessage)
                .build();
        }
    }

    /**
     * Validate SQL query for security and correctness.
     *
     * @param sql SQL query to validate
     * @return Error message if invalid, null if valid
     */
    private String validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "SQL query cannot be empty";
        }

        String trimmedSql = sql.trim().toUpperCase();

        // Check if it starts with SELECT
        if (!trimmedSql.startsWith("SELECT")) {
            return "Only SELECT statements are allowed";
        }

        // Check for forbidden keywords
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (trimmedSql.contains(keyword)) {
                return "SQL statement contains forbidden keyword: " + keyword;
            }
        }

        return null;
    }

    /**
     * Add row limit to SQL query for Oracle.
     *
     * @param sql Original SQL query
     * @param maxRows Maximum number of rows to return
     * @return Modified SQL with ROWNUM limit
     */
    private String addRowLimit(String sql, int maxRows) {
        // For Oracle, wrap query with ROWNUM limit
        return "SELECT * FROM (" + sql + ") WHERE ROWNUM <= " + maxRows;
    }
}
